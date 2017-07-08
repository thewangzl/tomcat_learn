package org.apache.catalina.loader;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLStreamHandlerFactory;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Logger;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.DirContextURLStreamHandlerFactory;

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
	
	private boolean modified;
	
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
	
	private static final String info = "org.apache.catalina.logger.WebappLoader/1.0";
	
	public WebappLoader() {
		this(null);
	}
	
	public WebappLoader(ClassLoader parent) {
		super();
		this.parentClassLoader = parent;
	}
	
	
	private WebappClassLoader createClassLoader() throws Exception{
		
		
		Class clazz = Class.forName(loaderClass);
		WebappClassLoader classLoader = null;
		if(this.parentClassLoader == null){
			//
			classLoader = (WebappClassLoader) clazz.newInstance();
			//In tomcat5, this if block is replaced by the following:
			//parentClassLoader = Thread.currentThread().getContextClassLoader();
		}else{
			Class[] argsTypes = {ClassLoader.class};
			Object[] args = {parentClassLoader};
			Constructor constr = clazz.getConstructor(argsTypes);
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
				log("webappLoader.failModifiedCheck",e);
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
	
	private void notifyContext() {
		// TODO Auto-generated method stub
		
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
		
		//TODO
		ValidatePackages();
	}

	@Override
	public void stop() throws LifecycleException {
		// TODO Auto-generated method stub
		
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
	
}

