package org.apache.catalina.core;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import javax.naming.directory.DirContext;
import javax.servlet.ServletException;

import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
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
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.naming.resources.ProxyDirContext;

public abstract class ContainerBase implements Container, Lifecycle, Pipeline {

	protected HashMap<String, Container> children = new HashMap<>();
	
	protected int debug = 0;
	
	protected LifecycleSupport lifecycle = new LifecycleSupport(this);
	
	/**
	 * The container event listeners for this Container.
	 */
	protected ArrayList<ContainerListener>	listeners = new ArrayList<>();
	
	/**
	 * 
	 */
	protected Loader loader;
	
	protected Logger logger;
	
	/**
	 * The manager implementation with which which container is associated
	 */
	protected Manager manager;
	
	protected Cluster cluster;
	
	/**
	 * The one only Mapper associated with this Container, if any .
	 */
	protected Mapper mapper;
	
	/**
	 * The set of Mappers associated with this container, keyed by protocol
	 */
	protected HashMap<String, Mapper> mappers = new HashMap<>();
	
	/**
	 * The Java class name of the default Mapper class for this Container.
	 */
	protected String mappClass;
	
	/**
	 * The human-readable name of this Container
	 */
	protected String name;
	
	protected Container parent;
	
	protected ClassLoader parentClassLoader;
	
	protected Pipeline pipeline = new StandardPipeline(this);
	
	protected Realm realm = null;
	
	protected DirContext resources;
	
	protected static StringManager sm = StringManager.getManager(Constants.Package);
	
	/**
	 * Has this component been started ?
	 */
	protected boolean started;
	
	protected PropertyChangeSupport support = new PropertyChangeSupport(this);
	
	// ------------------------------------------------- 
	
	public int getDebug() {
		return debug;
	}
	
	public void setDebug(int debug) {
		int oldValue = this.debug;
		this.debug = debug;
		support.firePropertyChange("debug", oldValue, this.debug);
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
	public void addValve(Valve valve) {

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
	public synchronized void start() throws LifecycleException {

		if(started){
			throw new LifecycleException(sm.getString("containerBase.alreadyStarted",logName()));
		}
		
		//
		lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);
		
		this.addDefaultMapper(this.mappClass);
		started = true;
		
		//Start our subodinate components, if any
		if(loader != null && loader instanceof Lifecycle){
			((Lifecycle)loader).start();
		}
		if(logger != null && logger instanceof Lifecycle){
			((Lifecycle) logger).start();
		}
		if(manager != null && manager instanceof Lifecycle){
			((Lifecycle) manager).start();
		}
		if(cluster != null && cluster instanceof Lifecycle){
			((Lifecycle)cluster).start();
		}
		if(realm != null && realm instanceof Lifecycle){
			((Lifecycle) realm).start();
		}
		if(resources != null && resources instanceof Lifecycle){
			((Lifecycle)resources).start();
		}
		
		//Start our Mappers, if any
		Mapper[] mappers = this.findMappers();
		for (Mapper mapper : mappers) {
			if(mapper instanceof Lifecycle){
				((Lifecycle)mapper).start();
			}
		}
		
		//Start our child containers, if any
		Container[] children = findChildren();
		for (Container child : children) {
			((Lifecycle) child).start();
		}
		
		//Start the valves in our pipeline 
		if(pipeline instanceof Lifecycle){
			((Lifecycle) pipeline).start();
		}
		
		//
		lifecycle.fireLifecycleEvent(START_EVENT, null);
		
		lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
	}

	@Override
	public void stop() throws LifecycleException {
		if(!started){
			throw new LifecycleException(sm.getString("containerBase.notStarted",logName()));
		}

		lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);
		
		lifecycle.fireLifecycleEvent(STOP_EVENT, null);
		started = false;
		
		//Stop the valves in our pipeline 
		if(pipeline instanceof Lifecycle){
			((Lifecycle) pipeline).stop();
		}
		//Stop our Mappers, if any
		Mapper[] mappers = this.findMappers();
		for (Mapper mapper : mappers) {
			if(mapper instanceof Lifecycle){
				((Lifecycle)mapper).stop();
			}
		}
		
		//Stop our child containers, if any
		Container[] children = findChildren();
		for (Container child : children) {
			((Lifecycle) child).stop();
		}
				
		//Stop our subodinate components, if any
		if(resources != null && resources instanceof Lifecycle){
			((Lifecycle)resources).stop();
		}
		if(realm != null && realm instanceof Lifecycle){
			((Lifecycle) realm).stop();
		}
		if(cluster != null && cluster instanceof Lifecycle){
			((Lifecycle)cluster).stop();
		}
		if(manager != null && manager instanceof Lifecycle){
			((Lifecycle) manager).stop();
		}
		if(logger != null && logger instanceof Lifecycle){
			((Lifecycle) logger).stop();
		}
		if(loader != null && loader instanceof Lifecycle){
			((Lifecycle)loader).stop();
		}
		
		lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
	}

	@Override
	public abstract String getInfo();
	
	@Override
	public Loader getLoader() {
		if(loader != null){
			return loader;
		}
		if(parent != null){
			return parent.getLoader();
		}
		return null;
	}

	@Override
	public synchronized void setLoader(Loader loader) {

		Loader oldLoader = this.loader;
		if(loader == oldLoader){
			return;
		}
		this.loader = loader;
		
		//Stop the old component if necessary
		if(started && oldLoader != null && oldLoader instanceof Lifecycle){
			try {
				((Lifecycle) oldLoader).stop();
			} catch (LifecycleException e) {
				log("containerBase.setLoader: stop", e);
			}
		}
		
		//Start the new component if necessary
		if(loader != null){
			loader.setContainer(this);
		}
		if(started && loader != null && loader instanceof Lifecycle){
			try {
				((Lifecycle)loader).start();
			} catch (LifecycleException e) {
				log("containerBase.setLoader: start", e);
			}
		}
		
		//Report this property change to interested listeners
		support.firePropertyChange("loader", oldLoader, this.loader);
	}

	@Override
	public Logger getLogger() {
		if(logger != null){
			return logger;
		}
		if(parent != null){
			return parent.getLogger();
		}
		return null;
	}

	@Override
	public synchronized void setLogger(Logger logger) {

		Logger oldLogger = this.logger;
		if(oldLogger == logger){
			return;
		}
		this.logger = logger;
		
		//Stop the old component if necessary
		if(started && oldLogger != null && oldLogger instanceof Lifecycle){
			try {
				((Lifecycle)oldLogger).stop();
			} catch (LifecycleException e) {
				log("containerBase.setLogger: stop", e);
			}
		}
		
		//Start the new component if necessary
		if(logger != null){
			logger.setContainer(this);
		}
		if(started && logger != null && logger instanceof Lifecycle){
			try {
				((Lifecycle)logger).start();
			} catch (LifecycleException e) {
				log("containerBase.setLogger: start", e);
			}
		}
		//Report this property change to 
		support.firePropertyChange("logger", oldLogger, this.logger);
	}

	@Override
	public Manager getManager() {
		if(manager != null){
			return manager;
		}
		if(parent != null){
			return parent.getManager();
		}
		return null;
	}

	@Override
	public synchronized void setManager(Manager manager) {

		Manager oldManager = this.manager;
		if(oldManager == manager){
			return;
		}
		this.manager = manager;
		
		//Stop the old
		if(started && oldManager != null && oldManager instanceof Lifecycle){
			try {
				((Lifecycle) oldManager).stop();
			} catch (LifecycleException e) {
				log(sm.getString("containerBase.setManager.stop"), e);
			}
		}
		
		//Start the new
		if(manager != null){
			manager.setContainer(this);
		}
		if(started && manager != null && manager instanceof Lifecycle){
			try {
				((Lifecycle)manager).start();
			} catch (LifecycleException e) {
				log(sm.getString("containerBase.setManager.start"), e);
			}
		}
		
		//Report this property change to interested listeners
		support.firePropertyChange("manager", oldManager, this.manager);
	}

	@Override
	public Cluster getCluster() {
		if(cluster != null){
			return cluster;
		}
		if(parent != null){
			return parent.getCluster();
		}
		return null;
	}

	@Override
	public synchronized void setCluster(Cluster cluster) {

		Cluster oldCluster = this.cluster;
		if(oldCluster == cluster){
			return;
		}
		this.cluster = cluster;
		
		// Stop the new
		if(started && oldCluster != null && oldCluster instanceof Lifecycle){
			try {
				((Lifecycle) oldCluster).stop();
			} catch (LifecycleException e) {
				log(sm.getString("containerBase.setCluster.stop"), e);
			}
		}
		
		//Start the new
		if(cluster != null){
			cluster.setContainer(this);
		}
		if(started && cluster != null && cluster instanceof Lifecycle){
			try {
				((Lifecycle) cluster).start();
			} catch (LifecycleException e) {
				log(sm.getString("containerBase.setCluster.start"), e);
			}
		}
		
		//Report this property change to interested listeners
		support.firePropertyChange("cluster", oldCluster, this.cluster);
	}

	@Override
	public String getName() {
		
		return this.name;
	}

	@Override
	public void setName(String name) {
		String oldName = this.name;
		this.name = name;
		support.firePropertyChange("name", oldName, this.name);

	}

	@Override
	public Container getParent() {
		return this.parent;
	}

	@Override
	public void setParent(Container parent) {
		Container oldParent = this.parent;
		this.parent = parent;
		support.firePropertyChange("parent", oldParent, this.parent);
	}

	@Override
	public ClassLoader getParentClassLoader() {
		if(this.parent != null){
			return this.parentClassLoader;
		}
		if(parent != null){
			return parent.getParentClassLoader();
		}
		return ClassLoader.getSystemClassLoader();
	}

	@Override
	public void setParentClassLoader(ClassLoader parentClassLoader) {
		ClassLoader oldParentClassLoader = this.parentClassLoader;
		this.parentClassLoader = parentClassLoader;
		support.firePropertyChange("parentClassLoader", oldParentClassLoader, this.parentClassLoader);
	}

	@Override
	public Realm getRealm() {
		if(realm != null){
			return realm;
		}
		if(parent != null){
			return parent.getRealm();
		}
		return null;
	}

	@Override
	public synchronized void setRealm(Realm realm) {
		Realm oldRealm = this.realm;
		if(oldRealm == realm){
			return;
		}
		this.realm = realm;
		
		//Stop the old 
		if(started && oldRealm != null && oldRealm instanceof Lifecycle){
			try {
				((Lifecycle) oldRealm).stop();
			} catch (LifecycleException e) {
				log(sm.getString("containerBase.setRealm: stop"),e);
			}
		}
		
		// Start the new
		if(realm != null){
			realm.setContainer(this);
		}
		if(started && realm != null && realm instanceof Lifecycle){
			try {
				((Lifecycle)realm).start();
			} catch (LifecycleException e) {
				log(sm.getString("containerBase.setRealm: start"),e);
			}
		}
		
		//Report the property change to interested listeners
		support.firePropertyChange("realm", oldRealm, this.realm);
	}

	@Override
	public DirContext getResources() {
		if(resources != null){
			return resources;
		}
		if(parent != null){
			return parent.getResources();
		}
		return null;
	}

	@Override
	public synchronized void setResources(DirContext resources) {

		DirContext oldResources = this.resources;
		if(oldResources == resources){
			return;
		}
		Hashtable<String, String> env = new Hashtable<>();
		if(getParent() != null){
			env.put(ProxyDirContext.HOST, getParent().getName());
		}
		env.put(ProxyDirContext.CONTEXT, getName());
		this.resources = new ProxyDirContext(env, resources);
	}

	@Override
	public void addChild(Container child) {
		if(System.getSecurityManager() != null){
			PrivilegedAddChild dp = new PrivilegedAddChild(child);
			AccessController.doPrivileged(dp);
		}else{
			addChildInternal(child);
		}

	}
	
	private void addChildInternal(Container child){
		synchronized (children) {
			if(children.get(child.getName()) != null){
				throw new IllegalArgumentException("addChild: Child name '" + child.getName() +"' is not unique");
			}
			child.setParent(this);
			if(started && child instanceof Lifecycle){
				try {
					((Lifecycle) child).start();
				} catch (LifecycleException e) {
					log(sm.getString("containerBase.addChild: start"),e);
					throw new IllegalStateException("containerBase.addChild: start");
				}
			}
			children.put(child.getName(), child);
			fireContainerEvent(ADD_CHILD_EVENT, child);
		}
	}

	@Override
	public void addContainerListener(ContainerListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	@Override
	public void addMapper(Mapper mapper) {

		synchronized (mappers) {
			if(mappers.get(mapper.getProtocol()) != null){
				throw new IllegalArgumentException("addMapper: Protocol '" + mapper.getProtocol()+"' is not unique");
			}
			mapper.setContainer(this);
			if(started && mapper instanceof Lifecycle){
				try {
					((Lifecycle) mapper).start();
				} catch (LifecycleException e) {
					log(sm.getString("containerBase.addMapper: start"),e);
					throw new IllegalStateException("containerBase.addMapper: start ", e);				}
			}
			mappers.put(mapper.getProtocol(), mapper);
			if(mappers.size() == 1){
				this.mapper = mapper;
			}else{
				this.mapper = null;
			}
			fireContainerEvent(ADD_MAPPER_EVENT, mapper);
		}
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);
	}

	@Override
	public Container findChild(String name) {
		if(name == null){
			return null;
		}
		synchronized (children) {
			return children.get(name);
		}
	}

	@Override
	public Container[] findChildren() {
		synchronized (children) {
			return children.values().toArray(new Container[children.size()]);
		}
	}

	@Override
	public ContainerListener[] findContainerListeners() {
		synchronized (listeners) {
			return listeners.toArray(new ContainerListener[listeners.size()]);
		}
	}

	@Override
	public Mapper findMapper(String protocol) {
		if(mapper != null){
			return mapper;
		}
		synchronized (mappers) {
			return mappers.get(protocol);
		}
	}

	@Override
	public Mapper[] findMappers() {
		synchronized (mappers) {
			return mappers.values().toArray(new Mapper[mappers.size()]);
		}
	}

	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {

		this.pipeline.invoke(request, response);
	}

	@Override
	public Container map(Request request, boolean update) {
		// Select the Mapper we will use
		Mapper mapper = findMapper(request.getRequest().getProtocol());
		if(mapper == null){
			return null;
		}
		//User this mapper to perform this mapping
		return mapper.map(request, update);
	}

	@Override
	public void removeChild(Container child) {
		synchronized (children) {
			if(children.get(child.getName()) == null){
				return;
			}
			children.remove(child.getName());
		}
		if(started && child instanceof Lifecycle){
			try {
				((Lifecycle) child).stop();
			} catch (LifecycleException e) {
				log("containerBase.removeChild: stop", e);
			}
		}
		
		fireContainerEvent(REMOVE_CHILD_EVENT, child);
		child.setParent(null);
	}

	@Override
	public void removeContainerListener(ContainerListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	@Override
	public void removeMapper(Mapper mapper) {
		synchronized (mappers) {
			if(mappers.get(mapper.getProtocol()) == null){
				return;
			}
			mappers.remove(mapper);
			if(started && mapper instanceof Lifecycle){
				try {
					((Lifecycle) mapper).stop();
				} catch (LifecycleException e) {
					log("containerBase.removeMapper: stop", e);
					throw new IllegalStateException("containerBase.removeMapper: stop", e);
				}
			}
			if(mappers.size() != 1){
				this.mapper = null;
			}else{
				Iterator<Mapper> values = mappers.values().iterator();
				this.mapper = values.next();
			}
			fireContainerEvent(REMOVE_MAPPER_EVENT, mapper);
		}
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {

		support.removePropertyChangeListener(listener);
	}
	
	protected void addDefaultMapper(String mapperClass){
		//Do we read a default Mapper?
		if(mapperClass == null){
			return;
		}
		if(mappers.size() >= 1){
			return;
		}
		
		// Instantiate and add a default Mapper
		try {
			Class<?> clazz = Class.forName(mapperClass);
			Mapper mapper = (Mapper) clazz.newInstance();
			mapper.setProtocol("http");
			addMapper(mapper);
		} catch (Exception e) {

			log(sm.getString("containerBase.addDefaultMapper", mapperClass), e);
		}
	}
	
	public void fireContainerEvent(String type, Object data){
		if(listeners.size() < 1){
			return;
		}
		ContainerEvent event = new ContainerEvent(this, type, data);
		ContainerListener[] list = new ContainerListener[0];
		synchronized (listeners) {
			list = listeners.toArray(list);
		}
		for (ContainerListener listener : list) {
			listener.containeEvent(event);
		}
	}
	
	protected void log(String message) {

		Logger logger = getLogger();
		if(logger != null){
			logger.log(logName() + ": " + message);
		}else{
			System.out.println(logName() + ": "+ message);
		}
		
	}
	
	protected void log(String message, Throwable throwable) {

		Logger logger = getLogger();
		if(logger != null){
			logger.log(logName() + ": " + message,throwable);
		}else{
			System.err.println(logName() + ": "+ message);
			throwable.printStackTrace(System.err);
		}
		
	}

	/**
	 * Return the abbreviated name of this container for logging messages
	 * 
	 * @return
	 */
	protected String logName(){
		String className = this.getClass().getName();
		int period = className.lastIndexOf('.');
		if(period >= 0){
			className = className.substring(period + 1);
		}
		return className + "[" + getName() + "]";
	}

	
	/**
	 * Perform addChild with the permissions of this class.
	 * 
	 * @author thewangzl
	 *
	 */
	protected class PrivilegedAddChild implements PrivilegedAction<Object>{
		
		private Container child;
		
		public PrivilegedAddChild(Container child) {
			this.child = child;
		}

		@Override
		public Object run() {
			addChildInternal(child);
			return null;
		}
	}
}
