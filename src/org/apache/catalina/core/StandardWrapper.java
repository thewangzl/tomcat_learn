package org.apache.catalina.core;


import java.io.PrintStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Stack;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Context;
import org.apache.catalina.InstanceEvent;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.HttpRequestBase;
import org.apache.catalina.connector.HttpResponseBase;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.InstanceSupport;
import org.apache.tomcat.util.log.SystemLogHandler;


/**
 * 
 * @author thewangzl
 *
 */
public class StandardWrapper extends ContainerBase implements Wrapper, ServletConfig {
	
	//------------------------------------------ INstance variables
	
	/**
	 * The date and time at which this servlet will become available (in milliseconds since th epoch),
	 * or zero if the servlet is avaliable. If this value equals Long.MAX_VALUE, the unavailability
	 * if this servlet is considered permanent.
	 */
	private long available = 0L;
	
	/**
	 * The count of allocations that are currently active (even if they are for 
	 * the same instance, as well be true on a non-STM servlet.
	 */
	private int countAllocated = 0;
	
	/**
	 * The facade associated with this wrapper
	 */
	private StandardWrapperFacade facade = new StandardWrapperFacade(this);
	
	/**
	 * 
	 */
	private HashMap<String,String> parameters;

	/**
	 * The (single ) initilized instance of this servlet
	 */
	private Servlet instance;
	
	/**
	 * The support object for our instance listeners.
	 */
	private InstanceSupport instanceSupport = new InstanceSupport(this);

	/**
	 * The context-relative URI of the JSP file for this servlet.
	 */
	private String jspFile;
	
	/**
	 * The load-on-startup order value (negative value means load on first call) for this servlet.
	 */
	private int loadOnStartup = -1;
	

	/**
	 * The security role references for this servlet, keyed by role name userd in the servlet. 
	 * The corrsponding value is the role name of the web application itself.
	 */
	private HashMap<String, String> references = new HashMap<>();
	
	/**
	 * The run-as identity for this servlet.
	 */
	private String runAs;
	
	/**
	 * The fully qualified servlet class name for this servlet.
	 */
	private String servletClass;
	
	
	/**
	 * Does this servlet implement the SingleThreadModel interface?
	 */
	private boolean singleThreadModel;
	
	/**
	 * Are we unloading our servlet intance at the moment?
	 */
	private boolean unloading;
	
	/**
	 * Maximum number of STM instances.
	 */
	private int maxInstances = 20;
	
	/**
	 * Number of instances currrently loaded for a STM servlet.
	 */
	private int nInstances = 0;
	
	/**
	 * Stack containing the STM instances.
	 */
	private Stack<Servlet> instancePool;
	
	private static final String info = "org.apache.catalina.core.StandardWrapper/1.0";
	
	
	public StandardWrapper() {
		super();
		pipeline.setBasic(new StandardWrapperValve());
	}
	

	@Override
	public long getAvailable() {
		
		return this.available;
	}
	/**
	 * 
	 * @param available
	 */
	@Override
	public void setAvailable(long available) {
		long oldAvailable = this.available;
		if(available > System.currentTimeMillis()){
			this.available = available;
		}else{
			this.available = 0L;
		}
		support.firePropertyChange("available", oldAvailable, this.available);
	}
	
	/**
	 * 
	 * @return
	 */
	public int getCountAllocated() {
		return countAllocated;
	}

	public InstanceSupport getInstanceSupport(){
		return this.instanceSupport; 
	}
	
	@Override
	public String getJspFile() {
		return this.jspFile;
	}
	
	@Override
	public void setJspFile(String jspFile) {
		String oldJspFile = this.jspFile;
		this.jspFile = jspFile;
		support.firePropertyChange("jspFile", oldJspFile, this.jspFile);
	}
	
	@Override
	public int getLoadOnStartup() {
		return loadOnStartup;
	}
	
	@Override
	public void setLoadOnStartup(int loadOnStartup) {
		int oldLoadOnStartup = this.loadOnStartup;
		this.loadOnStartup = loadOnStartup;
		support.firePropertyChange("loadOnStartup", oldLoadOnStartup, this.loadOnStartup);
	}
	
	public void setLoadOnStartupString(String value){
		try{
			setLoadOnStartup(Integer.parseInt(value));
		}catch(NumberFormatException e){
			setLoadOnStartup(0);
		}
	}
	
	public int getMaxInstances() {
		return maxInstances;
	}
	
	public void setMaxInstances(int maxInstances) {
		int oldMaxInstances = this.maxInstances;
		this.maxInstances = maxInstances;
		support.firePropertyChange("maxInstances", oldMaxInstances, this.maxInstances);
	}
	
	@Override
	public void setParent(Container parent) {
		if(parent != null && !(parent instanceof Context)){
			throw new IllegalArgumentException("standardWrapper.notContext");
		}
		super.setParent(parent);
	}
	
	@Override
	public String getRunAs() {
		return runAs;
	}
	
	@Override
	public void setRunAs(String runAs) {
		String oldRunAs = this.runAs;
		this.runAs = runAs;
		support.firePropertyChange("runAs", oldRunAs, this.runAs);
	}
	
	@Override
	public String getServletClass() {
		return servletClass;
	}
	
	@Override
	public void setServletClass(String servletClass) {
		String oldServletClass = this.servletClass;
		this.servletClass = servletClass;
		support.firePropertyChange("servletClass", oldServletClass, this.servletClass);
		
	}
	
	public void setServletName(String name){
		setName(name);
	}

	/**
	 * Return <code>true</code> if the servlet class represented by this component implements 
	 * the <code>SingleThreadModel</code> interface.
	 * 
	 * @return
	 */
	public boolean isSingleThreadModel() {
		try{
			loadServlet();
		}catch(Throwable t){
			;
		}
		return singleThreadModel;
	}
	
	/**
	 * Is this servlet currently unavailable?
	 * 
	 * @return
	 */
	@Override
	public boolean isUnavailable() {
		if(available == 0L){
			return false;
		}else if (available <= System.currentTimeMillis()){
			available = 0L;
			return false;
		}
		return true;
	}
	
	
	@Override
	public void addChild(Container child) {
		throw new IllegalStateException(sm.getString("standardWrapper.notChild"));
	}
	
	@Override
	public void addInstanceListener(InstanceListener listener){
		this.instanceSupport.addInstanceListener(listener);
	}
	
	@Override
	public void removeInstanceListener(InstanceListener listener){
		this.instanceSupport.removeInstanceListener(listener);
	}
	
	/**
	 * Add a new security role reference record to the set of records for this servlet.
	 * 
	 * @param name Role name used within this servlet
	 * @param link Role name used within the web application
	 */
	@Override
	public void addSecurityReference(String name, String link){
		synchronized (references) {
			references.put(name, link);
		}
		fireContainerEvent("addSecurityReference", name);
	}
	
	public String findSecurityReference(String name){
		synchronized (references) {
			return this.references.get(name);
		}
	}
	
	public String[] findSecurityReferences(){
		synchronized (references) {
			return references.keySet().toArray(new String[0]);
		}
	}
	
	@Override
	public void removeSecurityReference(String name){
		synchronized (references) {
			references.remove(name);
		}
		fireContainerEvent("removeSecurityReference", name);
	}
	
	/**
	 * Allocate an initialized instance of this servlet that is ready to have its <code>service()</code> method called. 
	 * If the servlet class does not implement <code>SingleThreadMode</code>, the (only) initialized instance may be 
	 * returned immediately. If the servlet class implements <code>SinlgeThreadModel</code>, The Wrapper implementation
	 * must ensure that this instance is not allocated again until it is deallocated by a call to <code>dealloted()</code>
	 * 
	 * @return
	 * @throws ServletException
	 */
	@Override
	public Servlet allocate() throws ServletException {
		if(debug >= 1){
			log("Allocating an instance");
		}
		//If we are currently unloading this servlet, thrown an exception
		if(unloading){
			throw new ServletException(sm.getString("standardWrapper.unloading",getName()));
		}
		
		//if not SingleThreadModel, return the same instance every time
		if(!singleThreadModel){
			
			//Load and initialize our instance if necessary
			if(instance == null){
				synchronized (this) {
					if(instance == null){
						instance = loadServlet();
					}
				}
			}
			
			if(!singleThreadModel){
				if(debug >= 2){
					log(" Returning non-STM instance");
				}
				countAllocated++;
				return instance;
			}
		}
		
		synchronized (instancePool) {
			while(countAllocated >= nInstances){
				//Allocate a new instance if possible, or else wait
				if(nInstances < maxInstances){
					try{
						instancePool.push(this.loadServlet());
						nInstances++;
					}catch(ServletException e){
						throw e;
					}catch(Throwable e){
						throw new ServletException(sm.getString("stanardWrapper.allocate"),e);
					}
				}else{
					try {
						instancePool.wait();
					} catch (InterruptedException e) {
						;
					}
				}
			}
			
			if(debug >= 2){
				log(" Returning allocated STM instance");
			}
			countAllocated++;
			return instancePool.pop();
		}
		
	}
	
	/**
	 * Return this previously allocated servlet to the pool of available instances.
	 * If this servlet class does not implement Si
	 * @param servlet
	 * @throws ServletException
	 */
	@Override
	public void deallocated(Servlet servlet) throws ServletException {
		
		//If not singleThreadModel, no action is required.
		if(!this.singleThreadModel){
			countAllocated--;
			return;
		}
		
		//Unlock and free this instance
		synchronized (instancePool) {
			countAllocated--;
			instancePool.push(servlet);
			instancePool.notify();
		}
	}
	
	/**
	 * Load and initialize an instance of this servlet, if there is not already at least one initialized instance.
	 * This can be used, for example, to load servlets that are marked in the deployment descriptor to be loaded
	 * at server startup time.
	 *  
	 * @throws ServletException
	 */
	@Override
	public void load() throws ServletException {
		
		instance = loadServlet();
	}
	
	/**
	 * Load and initialize an instance of this servlet, if there is not already at least one initialized instance.
	 * This can be used, for example, to load servlets that are marked in the deployment descriptor to be loaded
	 * at server startup time.
	 * @return 
	 * 
	 * @throws ServletException
	 */
	public synchronized Servlet loadServlet() throws ServletException{
		//Nothing to do if we already have an instance or an instance pool
		if(!singleThreadModel && instance != null){
			return instance;
		}
		
		PrintStream out = System.out;
		SystemLogHandler.startCapture();
		Servlet servlet = null;
		try{
			//If this "servlet" is readlly a JSP file, get the right class.
			//HOLD YOUR NOSE - this is a kludge that avoids having to do special
			//case Catalina-specific code in Jsper - it also requires that the 
			//servlet path be replaced by the <jsp-file> element content in order
			//to be completely effective.
			String actualClass = this.servletClass;
			if(actualClass == null && jspFile != null){
				Wrapper jspWrapper = (Wrapper)((Context) getParent()).findChild(Constants.JSP_SERVLET_NAME);
				if(jspWrapper != null){
					actualClass = jspWrapper.getServletClass();
				}
			}
			
			//Complain if no servlet class has been specified
			if(actualClass == null){
				unavailable(null);
				throw new ServletException(sm.getString("standardWrapper.notClass", getName()));
			}
			
			//Acquire an instance of the class loader to be used
			Loader loader = getLoader();
			if(loader == null){
				unavailable(null);
				throw new ServletException(sm.getString("standardWrapper.missingLoader", getName()));
			}
			
			ClassLoader classLoader = loader.getClassLoader();
			
			//special case class loader for a container provided servlet
			if(this.isContainerProvidedServlet(actualClass)){
				classLoader = this.getClass().getClassLoader();
				log(sm.getString("standardWrapper.containerServlet", getName()));
			}
			
			//Load the specified servlet class from the appropriate class loader
			Class<?> classClass = null;
			try{
				if(classLoader != null){
					log("Using classloader.loadClass");
					classClass = classLoader.loadClass(actualClass);
				}else{
					log("Using forName");
					classClass = Class.forName(actualClass);
				}
			}catch(ClassNotFoundException e){
				unavailable(null);
				throw new ServletException(sm.getString("standardWrapper.missingClass", actualClass),e);
			}
			if(classClass == null){
				unavailable(null);
				throw new ServletException(sm.getString("standardWrapper.missingClass", actualClass));
			}
			
			// Instantiate and initialize an instance of the servlet class itself
			try {
				servlet = (Servlet) classClass.newInstance();
			} catch (ClassCastException e) {
				unavailable(null);
				throw new ServletException(sm.getString("standardWrapper.notServlet", actualClass), e);
			} catch (Throwable e) {
				unavailable(null);
				throw new ServletException(sm.getString("standardWrapper.instantiate", actualClass), e);
			}
			
			//Check if loading the servlet in this web application should be allowed
			if(!isServletAllowed(classClass)){
				throw new ServletException(sm.getString("standardWrapper.privilegedServlet", actualClass));
			}
			
			//Special handing for ContainerServlet instance
			if(servlet instanceof ContainerServlet && isContainerProvidedServlet(actualClass)){
				log("calling setWrapper");
				((ContainerServlet) servlet).setWrapper(this);
				log("after calling setWrapper");
			}
			
			//Call the initialization method of this servlet
			try{
				instanceSupport.fireInstanceEvent(InstanceEvent.BEFORE_INIT_EVENT, servlet);
				servlet.init(facade);
				//Invoke jspInit on JSP pages
				if(loadOnStartup > 0 && jspFile != null	){
					//Invoking jspInit
					HttpRequestBase req = new HttpRequestBase();
					HttpResponseBase resp = new HttpResponseBase();
					req.setServletPath(jspFile);
					req.setQueryString("jsp_precompile=true");
					servlet.service(req, resp);
				}
				instanceSupport.fireInstanceEvent(InstanceEvent.AFTER_INIT_EVENT, servlet);
				
			}catch(UnavailableException e){
				instanceSupport.fireInstanceEvent(InstanceEvent.AFTER_INIT_EVENT, servlet, e);
				unavailable(e);
				throw e;
			}catch(ServletException e){
				instanceSupport.fireInstanceEvent(InstanceEvent.AFTER_INIT_EVENT, servlet, e);
				//If the servlet wanted to be unvailable it would have said so, 
				//so do not call unvailable(null).
				throw e;
			}catch(Throwable e){
				e.printStackTrace();
				instanceSupport.fireInstanceEvent(InstanceEvent.AFTER_INIT_EVENT, servlet, e);
				//If the servlet wanted to be unvailable it would have said so, 
				//so do not call unvailable(null).
				throw new ServletException(sm.getString("standardWrapper.initException", getName()));
			}
			
			//Register our newly initialized instance
			singleThreadModel = servlet instanceof SingleThreadModel;
			if(singleThreadModel){
				if(instancePool == null){
					instancePool = new Stack<>();
				}
			}
			
			fireContainerEvent("load", this);
		}finally{
			String log = SystemLogHandler.stopCapture();
			if(log != null && log.length() > 0){
				if(getServletContext() != null){
					getServletContext().log(log);
				}else{
					out.print(log);
				}
			}
		}
		return servlet;
	}

	/**
	 * Unload all initialized instances of this sevlet, after calling the <code>destroy()</code> method for each instance.
	 * This can be used, for example, prior to shutting down the entire servlet engine, or prior to reloading all of the 
	 * classes from the Loader associated with our Loader's repository.
	 * @throws ServletException 
	 * 
	 */
	public synchronized void unload() throws ServletException {
		
		//Nothing to do if we have never loaded the instance
		if(!singleThreadModel && instance == null){
			return;
		}
		unloading = true;
		
		//Loaf a while if the current instance is allocated 
		//(possibly more than onece if non-STM)
		if(countAllocated > 0){
			int nRetries = 0;
			while(nRetries < 10){
				if(nRetries == 0){
					log("Waiting for " + this.countAllocated + " instance(s) to be deallocated");
				}
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					;
				}
				nRetries++;
			}
		}
		
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader classLoader = instance.getClass().getClassLoader();
		
		PrintStream out = System.out;
		SystemLogHandler.startCapture();
		
		// Call the servlet destroy() method
		try{
			instanceSupport.fireInstanceEvent(InstanceEvent.BEFORE_DESTROY_EVENT, instance);
			Thread.currentThread().setContextClassLoader(classLoader);
			instance.destroy();
			instanceSupport.fireInstanceEvent(InstanceEvent.AFTER_DESTROY_EVENT, instance);
		}catch(Throwable t){
			instanceSupport.fireInstanceEvent(InstanceEvent.AFTER_DESTROY_EVENT, instance, t);
			instance = null;
			instancePool = null;
			nInstances = 0;
			fireContainerEvent("unload", this);
			unloading = false;
			throw new ServletException(sm.getString("standardWrapper.destroyException", getName()));
		}finally{
			// restore the context ClassLoader
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
			//Write captured output
			String log = SystemLogHandler.stopCapture();
			if(log != null && log.length() > 0){
				if(getServletContext() != null){
					getServletContext().log(log);
				}else{
					out.println(log);
				}
			}
		}
		
		//Deregister the destroyed instance
		instance = null;
		
		if(singleThreadModel && instancePool != null){
			try{
				Thread.currentThread().setContextClassLoader(classLoader);
				while(!instancePool.isEmpty()){
					instancePool.pop().destroy();
				}
			}catch(Throwable t){
				instancePool = null;
				nInstances = 0;
				unloading = false;
				fireContainerEvent("unload", this);
				throw new ServletException(sm.getString("standardWraper.destroyException", getName()), t);
			}finally{
				//
				Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
			}
			instancePool = null;
			nInstances = 0;
		}
		unloading = false;
		fireContainerEvent("unload", this);
	}

	/**
	 * Process an UnavailableException, marking this servlet as unavailable for the specified amount of time.
	 * 
	 * @param unavailable The exception that occurred, or <code> null</code> to mark this servlet as 
	 * 			permanently unavailable.
	 */
	@Override
	public void unavailable(UnavailableException unavailable) {
		log(sm.getString("standardWrapper.unvailable", getName()));
		if(unavailable == null){
			setAvailable(Long.MAX_VALUE);
		}else if(unavailable.isPermanent()){
			setAvailable(Long.MAX_VALUE);
		}else{
			int unavailableSeconds = unavailable.getUnavailableSeconds();
			if(unavailableSeconds <= 0){
				unavailableSeconds = 60;			// Arbitrary default
			}
			setAvailable(System.currentTimeMillis() + (unavailableSeconds * 1000L));
		}
	}
	
	@Override
	public void addInitParameter(String name, String value){
		synchronized (parameters) {
			parameters.put(name, value);	
		}
		fireContainerEvent("addInitParameter", name);
	}

	@Override
	public String getInitParameter(String name) {
		
		return this.findInitParameter(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		synchronized (parameters) {
			return new Enumerator<>(parameters.keySet());
		}
	}
	
	@Override
	public void removeInitParameter(String name){
		synchronized (parameters) {
			parameters.remove(name);
		}
		fireContainerEvent("removeInitParameter", name);
	}

	@Override
	public ServletContext getServletContext() {
		if(parent == null){
			return null;
		}
		if(!(parent instanceof Context)){
			return null;
		}
		return ((Context)parent).getServletContext();
	}

	@Override
	public String getServletName() {
		return getName();
	}
	
	@Override
	public String findInitParameter(String name){
		synchronized (parameters) {
			return parameters.get(name);
		}
	}
	
	@Override
	public String[] findInitParameters(){
		synchronized (parameters) {
			return parameters.keySet().toArray(new String[parameters.size()]);
		}
	}
	
	@Override
	public String getInfo() {
		return info;
	}
	
	@Override
	protected void addDefaultMapper(String mapperClass) {
		;		//No need for a default Mapper on a Wrapper
	}
	
	/**
	 * Return the <code>true</code> if the specified class name represents a container 
	 * provided servlet class that should be loaded by the server class loader.
	 * 
	 * @param className
	 * @return
	 */
	private boolean isContainerProvidedServlet(String className){
		if(className.startsWith("org.apache.catalina.")){
			return true;
		}
		try {
			Class<?> clazz = this.getClass().getClassLoader().loadClass(className);
			return ContainerServlet.class.isAssignableFrom(clazz);
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	/**
	 * Return <code>true</code> if loading this servlet is allowed.
	 * 
	 * @param servlet
	 * @return
	 */
	private boolean isServletAllowed(Object servlet){
		if(servlet instanceof ContainerServlet){
			if(((Context)getParent()).getPrivileged() || servlet.getClass().getName().equals("org.apache.catalina.servlets.InvokerServlet")){
				return true;
			}else{
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected String logName() {
		StringBuffer sb = new StringBuffer("StandardWrapper[");
		if(getParent() != null){
			sb.append(getParent().getName());
		}else{
			sb.append("null");
		}
		sb.append(":").append(getName()).append("]");
		return sb.toString();
	}
	
	 /**
     * Return a String representation of this component.
     */
	@Override
    public String toString() {

        StringBuffer sb = new StringBuffer();
        if (getParent() != null) {
            sb.append(getParent().toString());
            sb.append(".");
        }
        sb.append("StandardWrapper[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());

    }
	
	@Override
	public synchronized void start() throws LifecycleException {
		
		//Start up this component
		super.start();
	}
	
	@Override
	public void stop() throws LifecycleException {
		//Shut down our servlet instance (if it has been initialzed)
		try {
			this.unload();
		} catch (ServletException e) {
			log(sm.getString("standardWrapper.unloadException", getName()), e);
		}
		//Shut down this component
		super.stop();
	}
}
