package org.apache.catalina.loader;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.jar.JarFile;

import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Logger;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.DirContextURLStreamHandlerFactory;
import org.apache.naming.resources.Resource;

public class WebappLoader implements Loader,Lifecycle,PropertyChangeListener,Runnable {

	private PropertyChangeSupport support = new PropertyChangeSupport(this);
	
	private LifecycleSupport lifecycle = new LifecycleSupport(this);
	
	private WebappClassLoader classLoader;
	
	private String loaderClass = "org.apache.catalina.loader.WebappClasLoader";
	
	private ClassLoader parentClassLoader;
	
	
	private Container container;
	
	private boolean delegate;
	
	private boolean reloadable;
	
	private DefaultContext defaultContext;
	
//	private boolean modified;
	
	private String[] repositories = new String[0];
	
	private boolean started;
	
	/**
	 * The number of seconds between checks for modified classes, 
	 * if automatic reloading is enable
	 */
	private int checkInterval = 15;
	
	/**
	 * The background thread.
	 */
	private Thread thread;
	
	/**
	 * Name to register for the background thread.
	 */
	private String threadName = "WebappLoader";
	
	/**
	 * The background thread completion semaphore.
	 */
	private boolean threadDone;
	
	private int debug = 0;
	
	protected static final StringManager sm = StringManager.getManager(Constants.Package);
	
	private static final String info = "org.apache.catalina.logger.WebappLoader/1.0";
	
	public WebappLoader() {
		this(null);
	}
	
	public WebappLoader(ClassLoader parent) {
		super();
		this.parentClassLoader = parent;
	}
	
	
	private WebappClassLoader createClassLoader() throws Exception{
		
		
		Class<?> clazz = Class.forName(loaderClass);
		WebappClassLoader classLoader = null;
		if(this.parentClassLoader == null){
			//
			classLoader = (WebappClassLoader) clazz.newInstance();
			//In tomcat5, this if block is replaced by the following:
			//parentClassLoader = Thread.currentThread().getContextClassLoader();
		}else{
			Class<?>[] argsTypes = {ClassLoader.class};
			Object[] args = {parentClassLoader};
			Constructor<?> constr = clazz.getConstructor(argsTypes);
			classLoader = (WebappClassLoader)constr.newInstance(args);
		}
		return classLoader;
	}
	
	@Override
	public void run() {

		if(debug >= 1){
			log("background thread starting");
		}
		
		//Loop until the termination semaphore is set
		while(!threadDone){
			// wait for out check interval
			threadSleep();
			if(!started){
				break;
			}
			try{
				//Perform our modification check
				if(!classLoader.modified()){
					continue;
				}
			}catch(Exception e){
				log(sm.getString("webappLoader.failModifiedCheck"),e);
				continue;
			}
			
			//Handle a need for reloading
			notifyContext();
			break;
		}
		
		if(debug > 1){
			log("background thread stopping");
		}
	}
	/**
	 * Sleep for the duration specified by the <code>checkInterval</code> property.
	 */
	private void threadSleep(){
		try {
			Thread.sleep(checkInterval * 1000);
		} catch (InterruptedException e) {
			;
		}
	}
	
	/**
	 * start the background thread that will periodically check for session timeouts¡£
	 */
	private void threadStart(){
		if(thread != null){
			return;
		}
		
		//Validate our current state
		if(!reloadable){
			throw new IllegalStateException("webappLoader.notReloadable");
		}
		if(!(container instanceof Context)){
			throw new IllegalStateException("webappLoader.notContext");
		}
		
		//Start the background thread 
		if(debug >= 1){
			log("starting background thread");
		}
		threadDone = false;
		threadName = "WebappLoader[" + container.getName() + "]";
		thread = new Thread(this, threadName);
		thread.setDaemon(true);
		thread.start();
	}
	
	/**
	 * Stop the background thread that is periodically checking for modified classes.
	 */
	private void threadStop(){
		if(thread == null){
			return;
		}
		
		if(debug >= 1){
			log("Stopping background thread");
		}
		threadDone = true;
		thread.interrupt();
		try {
			thread.join();
		} catch (InterruptedException e) {
			;
		}
		thread = null;
	}
	
	/**
	 * Nofify our context that a reload is appropriate
	 */
	private void notifyContext() {
		WebappContextNotifier notifier = new WebappContextNotifier();
		(new Thread(notifier)).start();
	}
	
	/**
	 * Start this component, initializing our associated class loader.
	 * 
	 * @throws LifecycleException
	 */
	@Override
	public void start() throws LifecycleException {

		//Validate and update our current component state
		if(started){
			throw new LifecycleException("webappLoader.alreadyStarted");
		}
		if(debug >= 1){
			log("webappLoader.starting");
		}
		lifecycle.fireLifecycleEvent(START_EVENT, null);
		started = true;
		
		if(container.getResources() == null){
			return;
		}
		
		//Register a stream handler factory for the JNDI protocol
		URLStreamHandlerFactory factory = new DirContextURLStreamHandlerFactory();
		try{
			URL.setURLStreamHandlerFactory(factory);
		}catch(Throwable e){
			;				//Ignore the error here.
		}
		
		//Construct a class loader based on our current repositories list
		try{
			classLoader = createClassLoader();
			classLoader.setResources(container.getResources());
			classLoader.setDelegate(this.delegate);
			classLoader.setDebug(this.debug);
			
			for (int i = 0; i < repositories.length; i++) {
				classLoader.addRepository(repositories[i]);
			}
			
			//Configura out repositories
			setRepositories();
			setClassPath();
			
			setPermissions();
			
			if(classLoader instanceof Lifecycle){
				((Lifecycle) classLoader).start();
			}
			
			//Binding the Webapp class loader to the directory context
			DirContextURLStreamHandler.bind(classLoader,this.container.getResources());
			
		}catch(Throwable t){
			throw new LifecycleException("start: ",t);
		}
		
		//FIXME -- has been removed in laster tomcat
		validatePackages();
		
		//Starting our background thread if we are reloadable;
		if(reloadable){
			log(sm.getString("webappLoader.reloading"));
			threadStart();
		}
	}
	

	/**
	 * Stop this component, finilizing our associated class loader.
	 * 
	 * @throws LifecycleException
	 */
	@Override
	public void stop() throws LifecycleException {
		
		//Validate and update current component state
		if(!started){
			throw new LifecycleException(sm.getString("webappLoader.noStarted"));
		}
		if(debug >= 1){
			log(sm.getString("webappLoader.stopping"));
		}
		lifecycle.fireLifecycleEvent(STOP_EVENT, null);
		started = false;
		
		
		//Stop our background thread if we are reloadable
		if(reloadable){
			this.threadStop();
		}
		
		//Remove context attributes as approperate
		if(container instanceof Context){
			ServletContext servletContext = ((Context)container).getServletContext();
			servletContext.removeAttribute(Globals.CLASS_PATH_ATTR);
		}
		
		//Throw away our current class loader.
		if(classLoader instanceof Lifecycle){
			((Lifecycle) classLoader).stop();
		}
		DirContextURLStreamHandler.unbind(classLoader);
		classLoader = null;
	}
	
	private void setRepositories(){
		
		if(!(container instanceof Context)){
			return;
		}
		ServletContext servletContext = ((Context)this.container).getServletContext();
		if(servletContext == null){
			return;
		}
		
		//Loading the work directory
		File workDir = (File) servletContext.getAttribute(Globals.WORK_DIR_ATTR);
		if(workDir == null){
			return;
		}
		log(sm.getString("webappLoader.deploy", workDir.getAbsolutePath()));
		
		DirContext resources = container.getResources();
		
		//Setting up the class repository (WEB-INF/class) ,if it exists
		String classPath = "/WEB-INF/classes";
		DirContext classes = null;
		
		try {
			Object object = resources.lookup(classPath);
			if(object instanceof DirContext){
				classes = (DirContext) object;
			}
		} catch (NamingException e) {
			//silent catch: It's valid that no /WEB-INF/clsses collection exists
		}
		
		if(classes != null){
			
			File classRepository = null;
			
			String absoluteClassPath = servletContext.getRealPath(classPath);
			if(absoluteClassPath != null){
				classRepository = new File(absoluteClassPath);
			}else{
				classRepository = new File(workDir, classPath);
				classRepository.mkdir();
				copyDir(classes, classRepository);
			}
			
			log(sm.getString("webappLoader.classDeploy", classPath, classRepository.getAbsoluteFile()));
			
			//Adding the repository to the class loader
			classLoader.addRepository(classPath + "/", classRepository) ;
		}
		
		//setring up the JAR repository (/WEB-INF/lib), if it exists
		String libPath = "/WEB-INF/lib";
		
		classLoader.setJarPath(libPath);
		
		DirContext libDir = null;
		//Looking up directory /WEB-INF/lib in the context
		try {
			Object object = resources.lookup(libPath);
			if(object instanceof DirContext){
				libDir = (DirContext) object;
			}
		} catch (NamingException e) {
			;
		}
		
		if(libDir != null){
			
			boolean copyJars = false;
			String absolteLibPath = servletContext.getRealPath(libPath);
			
			File destDir = null;
			if(absolteLibPath != null){
				destDir = new File(absolteLibPath);
			}else{
				copyJars = true;
				destDir = new File(workDir, libPath);
			}
			
			// Looking up directory /WEB-INF/lib in the context
			
			try {
				NamingEnumeration<Binding> enums = resources.listBindings(libPath);
				while(enums.hasMoreElements()){
					Binding binding = enums.next();
					String fileName = libPath + "/" + binding.getName();
					if(!fileName.endsWith(".jar")){
						continue;
					}
					
					//Copy JAR in the work directory, always (the JAR file would get locked otherwise,
					//which would make it impossible to update it or remove it at runtime)
					File destFile = new File(destDir, binding.getName());
					
					log(sm.getString("webappLoader.jarDeploy", fileName, destFile.getAbsolutePath()));
					
					Resource jarResources = (Resource) binding.getObject();
					if(copyJars){
						if(!copy(jarResources.streamContent(), new FileOutputStream(destFile))){
							continue;
						}
					}
					
					JarFile jarFile = new JarFile(destFile);
					classLoader.addJar(fileName, jarFile,destFile);
				}
			
			} catch (NamingException e) {
				;		//Silent catch : it's valid that no /WEB-INF/lib directory exists
			} catch (FileNotFoundException e) {	
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Set the approciate context attribute for our class path.
	 * This is required only bacause Jasper depends on it.
	 */
	private void setClassPath(){
		
		//Validate our current state information
		if(!(container instanceof Context)){
			return;
		}
		ServletContext servletContext = ((Context)container).getServletContext();
		if(servletContext == null){
			return;
		}
		
		StringBuffer classpath = new StringBuffer();
		
		//Assemble the class path information from our class loader chain
		ClassLoader loader = getClassLoader();
		int layers = 0;
		int n = 0;
		while(layers < 3 && loader != null){
			if(!(loader instanceof URLClassLoader)){
				break;
			}
			URL[] repositories = ((URLClassLoader) loader).getURLs();
			for (int i = 0; i < repositories.length; i++) {
				String repository = repositories[i].toString();
				if(repository.startsWith("file://")){
					repository = repository.substring(7);
				}else if(repository.startsWith("file:")){
					repository = repository.substring(5);
				}else if(repository.startsWith("jndi:")){
					repository = servletContext.getRealPath(repository.substring(5));
				}else{
					continue;
				}
				if(repository == null){
					continue;
				}
				if(n > 0){
					classpath.append(File.pathSeparator);
				}
				classpath.append(repository);
				n++;
			}
			loader = loader.getParent();
			layers++;
		}
		
		//Store the asembled class path a a servlet context attribute
		servletContext.setAttribute(Globals.CLASS_PATH_ATTR, classpath.toString());
	}
	
	/**
	 * Configure associated class loader permissions.
	 */
	private void setPermissions(){
		if(System.getSecurityManager() == null){
			return;
		}
		if(!(container instanceof Context)){
			return;
		}
		
		//Tell the class loader the root of the context
		ServletContext servletContext = ((Context) container).getServletContext();
		
		//Assigning permissions for the work directory
		File workDir = (File) servletContext.getAttribute(Globals.WORK_DIR_ATTR);
		if(workDir != null){
			try{
				String workDirPath = workDir.getCanonicalPath();
				classLoader.addPermission(new FilePermission(workDirPath, "read,write"));
				classLoader.addPermission(new FilePermission(workDirPath + File.separator + "-", "read,write,delete"));
			}catch(IOException e){
				;		//Ignore
			}
		}
		
		try {
			URL rootURL = servletContext.getResource("/");
			classLoader.addPermission(rootURL);
			
			String contextRoot = servletContext.getRealPath("/");
			if(contextRoot != null){
				try{
					contextRoot = (new File(contextRoot).getCanonicalPath()) + File.separator;
					classLoader.addPermission(contextRoot);
				}catch (IOException e) {
					;		//Ignore
				}
			}
			
			URL classesURL = servletContext.getResource("WEB-INF/classes/");
			if(classesURL != null){
				classLoader.addPermission(classesURL);
			}
			
			URL libURL = servletContext.getResource("/WEB-INF/lib/");
			if(libURL != null){
				classLoader.addPermission(libURL);
			}
			
			if(contextRoot != null){
				if(libURL != null){
					File rootDir = new File(contextRoot);
					File libDir = new File(rootDir, "WEB-INF/lib/");
					String path = null;
					try {
						path = libDir.getCanonicalPath() + File.separator;
					} catch (IOException e) {
						;
					}
					if(path != null){
						classLoader.addPermission(path);
					}
				}
			}else{
				if(workDir != null){
					if(libURL != null){
						File libDir = new File(workDir, "WEB-INF/lib/");
						String path = null;
						try {
							path = libDir.getCanonicalPath() + File.separator;
						} catch (IOException e) {
							;
						}
						classLoader.addPermission(path);
					}
					
					if(classesURL != null){
						File classesFile = new File(workDir, "WEB-INF/classes/");
						String path = null;
						try {
							path = classesFile.getCanonicalPath() + File.separator;
						} catch (IOException e) {
							;
						}
						classLoader.addPermission(path);
					}
				}
			}
		} catch (MalformedURLException e) {
			;
		} 
	}
	
	/**
	 * Validate that the required optional packages for the application are actually present.
	 * @throws LifecycleException
	 */
	@Deprecated
	private void validatePackages() throws LifecycleException{
		ClassLoader classLoader = getClassLoader();
		if(classLoader instanceof WebappClassLoader){
			
			Extension[] available = ((WebappClassLoader) classLoader).findAvailable();
			Extension[] required = ((WebappClassLoader) classLoader).findRequired();
			
			if(debug >= 1){
				log("Optional Packages: available=" + available.length + ", required=" + required.length);
			}
			
			for (int i = 0; i < required.length; i++) {
				if(debug >= 1){
					log("checking for required package " + required[i]);
				}
				boolean found = false;
				for (int j = 0; j < available.length; j++) {
					if(available[j].isCompatibleWith(required[i])){
						found = true;
						break;
					}
				}
				if(!found){
					throw new LifecycleException("Missing optional package " + required[i]);
				}
			}
		}
		
		
	}

	/**
	 * Copy a file to the specified temp directory. This is required only because
	 * Jasper depends on it
	 * @param is
	 * @param os
	 * @return
	 */
	private boolean copy(InputStream is, OutputStream os) {
		try{
			byte[] buf = new byte[4096];
			while(true){
				int len = is.read(buf);
				if(len < 0){
					break;
				}
				os.write(buf, 0, len);
			}
			is.close();
			os.close();
		}catch(IOException e){
			return false;
		}
		return true;
	}

	/**
	 * Copy directory
	 * 
	 * @param classes
	 * @param classRepository
	 */
	@SuppressWarnings("resource")
	private boolean copyDir(DirContext srcDir, File destDir) {
		try {
			NamingEnumeration<NameClassPair> enums = srcDir.list("");
			while(enums.hasMoreElements()){
				NameClassPair ncPair = enums.nextElement();
				String name = ncPair.getName();
				Object object = srcDir.lookup(name);
				File currentFile = new File(destDir, name);
				if(object instanceof Resource){
					InputStream is = ((Resource) object).streamContent();
					OutputStream os = new FileOutputStream(currentFile);
					if(!copy(is, os)){
						return false;
					}
				}else if(object instanceof InputStream){
					OutputStream os = new FileOutputStream(currentFile);
					if(!copy((InputStream) object, os)){
						return false;
					}
				}else if(object instanceof DirContext){
					currentFile.mkdir();
					copyDir((DirContext) object, currentFile);
				}
				
			}
		} catch (NamingException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public int getCheckInterval() {
		return checkInterval;
	}
	
	public void setCheckInterval(int checkInterval) {
		int oldCheckInterval = this.checkInterval;
		this.checkInterval = checkInterval;
		support.firePropertyChange("checkInterval", oldCheckInterval, this.checkInterval);
	}
	@Override
	public ClassLoader getClassLoader() {

		return this.classLoader;
	}

	@Override
	public Container getContainer() {
		return this.container;
	}

	@Override
	public void setContainer(Container container) {
		
		//Deregiter from the old Container (if any)
		if(this.container != null && this.container instanceof Context ){
			((Context)this.container).removePropertyChangeListener(this);
		}
		
		//Process this property change
		Container oldContainer = this.container;
		this.container = container;
		support.firePropertyChange("container", oldContainer, this.container);
		
		//Register with the new Container (if any)
		if(this.container != null && this.container instanceof Context){
			setReloadable(((Context)this.container).getReloadable());
			((Context) this.container).addPropertyChangeListener(this);
		}
	}

	@Override
	public DefaultContext getDefaultContext() {

		return this.defaultContext;
	}

	@Override
	public void setDefaultContext(DefaultContext defaultContext) {
		DefaultContext oldDefaultContext = this.defaultContext;
		this.defaultContext = defaultContext;
		support.firePropertyChange("defaultContext", oldDefaultContext, this.defaultContext);
	}

	@Override
	public boolean getDelegate() {
		return this.delegate;
	}

	@Override
	public void setDelegate(boolean delegate) {
		boolean old = this.delegate;
		this.delegate = delegate;
		support.firePropertyChange("delegate", old, this.delegate);
	}

	@Override
	public boolean getReloadable() {
		return this.reloadable;
	}

	@Override
	public void setReloadable(boolean reloadable) {
		boolean old = this.reloadable;
		this.reloadable = reloadable;
		support.firePropertyChange("reloadable", old, this.reloadable);
	}
	
	public int getDebug() {
		return debug;
	}
	
	public void setDebug(int debug) {
		int old = this.debug;
		this.debug = debug;
		support.firePropertyChange("debug", old, this.debug);
	}
	
	public void setClassLoader(WebappClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	public String getLoaderClass() {
		return loaderClass;
	}
	
	public void setLoaderClass(String loaderClass) {
		this.loaderClass = loaderClass;
	}

	@Override
	public void addRepository(String repository) {
		
		if(debug >= 1){
			log("webappLoader.addRepository" + repository);
		}
		for (int i = 0; i < repositories.length; i++) {
			if(repository.equals(repositories[i])){
				return;
			}
		}
		String[] results = new String[repositories.length + 1];
		for (int i = 0; i < repositories.length; i++) {
			results[i] = repositories[i];
		}
		results[repositories.length] = repository;
		repositories = results;
		
		if(started && classLoader != null){
			classLoader.addRepository(repository);
		}
	}

	@Override
	public String[] findRepositories() {
		return this.repositories;
	}

	@Override
	public boolean modified() {

		return classLoader.modified();
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		support.removePropertyChangeListener(listener);
	}

	

	/**
	 * Process property change events from our associated Context.
	 * @param event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		
		//Validate this source of this event
		if(!(event.getSource() instanceof Context)){
			return;
		}
		@SuppressWarnings("unused")
		Context context = (Context) event.getSource();
		
		//Process a relevant property change
		if(event.getPropertyName().equals("reloadable")){
			try{
				this.setReloadable(((Boolean)event.getNewValue()).booleanValue());
			}catch(NumberFormatException e){
				log("webappLoader.reloadable" + event.getNewValue().toString());
			}
		}
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
	
	
	private void log(String message){
		Logger logger = null;
		if(container != null){
			logger = container.getLogger();
		}
		if(logger != null){
			logger.log("WebappLoader[" + container.getName() + "]:" + message);
		}else{
			String containerName = null;
			if(container != null){
				containerName = container.getName();
			}
			System.out.println("WebappLoader[" + containerName + "]:" + message);
		}
	}
	
	
	private void log(String message,Throwable throwable){
		Logger logger = null;
		if(container != null){
			logger = container.getLogger();
		}
		if(logger != null){
			logger.log("WebappLoader[" + container.getName() + "]:" + message,throwable);
		}else{
			String containerName = null;
			if(container != null){
				containerName = container.getName();
			}
			System.err.println("WebappLoader[" + containerName + "]:" + message);
			System.err.println("" + throwable);
			throwable.printStackTrace(System.err);
		}
	}
	
	public static String getInfo() {
		return info;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("WebappLoader[");
		if(container != null){
			sb.append(container.getName());
		}
		sb.append("]");
		return sb.toString();
	}
	
	
	
	// -----------------------------------WebappContextNotifier Inner Class ------------------
	
	
	/**
	 * Private thread class to notify our associated Context that we are have recognized the need for a reload.
	 * 
	 * @author thewangzl
	 *
	 */
	protected class WebappContextNotifier implements Runnable{

		/**
		 * Perfrom the requested notification
		 */
		@Override
		public void run() {

			((Context)container).reload();
		}
		
	}
}

