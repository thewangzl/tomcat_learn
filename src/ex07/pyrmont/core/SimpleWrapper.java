package ex07.pyrmont.core;

import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.naming.directory.DirContext;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Mapper;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.util.LifecycleSupport;

public class SimpleWrapper implements Wrapper, Pipeline, Lifecycle {

	private LifecycleSupport lifecycle = new LifecycleSupport(this);
	
	
	private Servlet instance;
	
	private String servletClass;
	
	private String name;
	
	/**
	 * Loader is used to load a servlet class 
	 */
	private Loader loader;
	
	private Container parent;
	
	private SimplePipeline pipeline = new SimplePipeline(this);
	
	private boolean started;
	
	public SimpleWrapper() {
		this.pipeline.setBasic(new SimpleWrapperValve());
	}
	
	@Override
	public Valve getBasic() {
		return this.pipeline.getBasic();
	}
	@Override
	public void setBasic(Valve basic) {
		this.pipeline.setBasic(basic);
	}
	@Override
	public synchronized void addValve(Valve valve) {
		this.pipeline.addValve(valve);
	}
	@Override
	public Valve[] getValves() {
		return this.pipeline.getValves();
	}
	@Override
	public void removeValve(Valve valve) {
		this.pipeline.removeValve(valve);
	}
	@Override
	public Servlet allocate() throws ServletException {
		if(instance == null){
			try{
				instance = this.loadServlet();
			}catch(ServletException e){
				throw e;
			}catch(Throwable t){
				throw new ServletException("Cannot allocate a servlet instance ",t);
			}
		}
		return instance;
	}
	
	private Servlet loadServlet() throws ServletException{
		if(instance != null	){
			return instance;
		}
		
		Servlet servlet = null;
		String actualClass = this.servletClass;
		if(actualClass == null){
			throw new ServletException("servlet class has not been specified");
		}
		Loader loader = getLoader();
		//
		if(loader == null){
			throw new ServletException("No Loader.");
		}
		ClassLoader classLoader = loader.getClassLoader();
		
		//Load the specified servlet class from the appropriate class loader.
		Class<?> clazz = null;
		try {
			if(classLoader !=null){
				clazz = classLoader.loadClass(actualClass);
			}
		} catch (ClassNotFoundException e) {
			throw new ServletException("Servlet class Not found:" + actualClass);
		}
		//Instantiate and initialize an instance of the servlet class itself
		try {
			servlet = (Servlet) clazz.newInstance();
		} catch (Throwable e) {
			throw new ServletException("Failed to instantiate servlet");
		} 
		try{
			servlet.init(null);
		}catch(Throwable t){
			throw new ServletException("Failed initialize servlet.");
		}
		return servlet;
	}

	@Override
	public void deallocated(Servlet servlet) throws ServletException {
		//TODO
	}

	@Override
	public void load() throws ServletException {
		instance = this.loadServlet();
	}

	/**
	 * Loader is used to load a servlet class 
	 * 
	 * @return
	 */
	public Loader getLoader() {
		if(loader != null)
			return loader;
		if(parent != null)
			return parent.getLoader();
		return null;
	}

	public void setLoader(Loader loader) {
		this.loader = loader;
	}

	public Container getParent() {
		return parent;
	}

	public void setParent(Container parent) {
		this.parent = parent;
	}
	@Override
	public String getServletClass() {
		return this.servletClass;
	}
	@Override
	public void setServletClass(String servletClass) {
		this.servletClass = servletClass;
	}
	@Override
	public String getInfo() {
		return null;
	}
	@Override
	public Logger getLogger() {
		return null;
	}
	@Override
	public void setLogger(Logger logger) {
		
	}
	@Override
	public Manager getManager() {
		return null;
	}
	@Override
	public void setManager(Manager manager) {
		
	}
	@Override
	public Cluster getCluster() {
		return null;
	}
	@Override
	public void setCluster(Cluster cluster) {
		
	}
	@Override
	public String getName() {
		return this.name;
	}
	@Override
	public void setName(String name) {
		this.name = name ;
	}
	@Override
	public ClassLoader getParentClassLoader() {
		return null;
	}
	@Override
	public void setParentClassLoader(ClassLoader parentClassLoader) {
		
	}
	@Override
	public Realm getRealm() {
		return null;
	}
	@Override
	public void setRealm(Realm realm) {
		
	}
	@Override
	public DirContext getResources() {
		return null;
	}
	@Override
	public void setResources(DirContext resources) {
		
	}
	@Override
	public void addChild(Container child) {
		
	}
	@Override
	public void addContainerListener(ContainerListener listener) {
		
	}
	@Override
	public void addMapper(Mapper mapper) {
		
	}
	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		
	}
	@Override
	public Container findChild(String name) {
		return null;
	}
	@Override
	public Container[] findChildren() {
		return null;
	}
	@Override
	public ContainerListener[] findContainerListeners() {
		return null;
	}
	@Override
	public Mapper findMapper(String protocol) {
		return null;
	}
	@Override
	public Mapper[] findMappers() {
		return null;
	}
	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {
		pipeline.invoke(request, response);
	}
	@Override
	public Container map(Request request, boolean update) {
		return null;
	}
	@Override
	public void removeChild(Container child) {
		
	}
	@Override
	public void removeContainerListener(ContainerListener listener) {
		
	}
	@Override
	public void removeMapper(Mapper mapper) {
		
	}
	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		
	}

	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		lifecycle.addLifecycleListener(listener);
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		return lifecycle.findLifecycleListeners();
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		lifecycle.removeLifecycleListener(listener);
	}

	@Override
	public void start() throws LifecycleException {
		System.out.println("Starting Wrapper " + name);
		if(started){
			throw new LifecycleException("Simple Wrapper has already started");
		}
		started = true;
		
		//Notify our intersted LifecycleListeners
		lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);
		
		started = true;
		
		//Start our subordinate components, if any
		if(loader != null && loader instanceof Lifecycle){
			((Lifecycle) loader).start();
		}
		
		//Start the Valves in our pipeline (including the basic), if any
		if(pipeline instanceof Lifecycle){
			((Lifecycle) pipeline).start();
		}
		
		//Nofify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(START_EVENT, null);
		
		lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
	}

	@Override
	public void stop() throws LifecycleException {
		System.out.println("Stoping wrapper " + name);
		
		//Shut down our servlet instance (if it has beeen initialized )
		try{
			instance.destroy();
		}catch(Throwable e){
			;
		}
		instance = null;
		
		if(!started){
			throw new LifecycleException("Simple Wrapper "+ name + " not started");
		}
		
		//Notify our insterested LifecycleListeners
		lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);
		
		lifecycle.fireLifecycleEvent(STOP_EVENT, null);
		
		started = false;
		
		//Stop the Valve in our pipeline (include the basic), if any
		if(pipeline instanceof Lifecycle){
			((Lifecycle) pipeline).stop();
		}
		
		//Stop our subordinate components, if any
		if(loader != null && loader instanceof Lifecycle){
			((Lifecycle) loader).stop();
		}
		
		// 
		lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
	}
	
	

}
