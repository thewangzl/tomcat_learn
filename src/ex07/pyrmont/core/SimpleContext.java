package ex07.pyrmont.core;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashMap;

import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
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
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.util.LifecycleSupport;

public class SimpleContext implements Context, Pipeline, Lifecycle {
	
	private LifecycleSupport lifecycle = new LifecycleSupport(this);
	
	
	private SimplePipeline pipeline =  new SimplePipeline(this);
	
	private HashMap<String, Container> children = new HashMap<>();
	
	private Loader loader = null;
	
	private HashMap<String, String> servletMappings = new HashMap<>();
	
	private Mapper mapper;
	
	private HashMap<String,Mapper> mappers = new HashMap<>();
	
	private Container parent;
	
	private String name;
	
	private Logger logger;
	
	private boolean started;
	
	public SimpleContext() {
		this.pipeline.setBasic(new SimpleContextValve());
	}
	
	@Override
	public Container map(Request request, boolean update) {
		//this method is taken from the map method in org.apache.catalina.core.ContainerBase
		//this findMapper method always return the default mapper, if any, regardless the request's protocol
		Mapper mapper = this.findMapper(request.getRequest().getProtocol());
		if(mapper == null){
			return null;
		}
		//Use the Mapper to perform this mapping
		return mapper.map(request, update);
	}
	


	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {
		pipeline.invoke(request, response);
	}
	

	@Override
	public void addMapper(Mapper mapper) {
		mapper.setContainer(this);			//may throw IAE
		this.mapper = mapper;
		synchronized (mappers) {
			if(mappers.get(mapper.getProtocol()) != null){
				throw new IllegalArgumentException("addMapper: Protocol '" + mapper.getProtocol() + " is not unique" );
			}
			mapper.setContainer(this);//may throw IAE
			mappers.put(mapper.getProtocol(), mapper);
			if(mappers.size() == 1){
				this.mapper = mapper;
			}else{
				this.mapper = null;
			}
		}
	}
	
	@Override
	public Mapper findMapper(String protocol) {
		if(mapper != null){
			return this.mapper;
		}
		synchronized (mappers) {
			return mappers.get(protocol);
		}
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {

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

		return null;
	}


	@Override
	public Mapper[] findMappers() {
		return null;
	}
	
	@Override
	public void addServletMapping(String pattern, String name) {
		synchronized (servletMappings) {
			servletMappings.put(pattern, name);
		}
	}

	@Override
	public String findServletMapping(String pattern) {
		synchronized (servletMappings) {
			return servletMappings.get(pattern);
		}
	}

	@Override
	public String[] findServletMappings() {
		synchronized (servletMappings) {
			return servletMappings.values().toArray(new String[servletMappings.size()]);
		}
	}

	
	@Override
	public String getInfo() {
		return null;
	}

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
	public void setLoader(Loader loader) {
		this.loader = loader;
	}

	@Override
	public Logger getLogger() {

		return this.logger;
	}

	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
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
		this.name = name;
	}

	@Override
	public Container getParent() {

		return parent;
	}

	@Override
	public void setParent(Container parent) {

		this.parent = parent;
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
		child.setParent(this);
		this.children.put(child.getName(), child);
	}

	@Override
	public void addContainerListener(ContainerListener listener) {


	}

	@Override
	public void removeChild(Container child) {
		//TODO 

	}

	@Override
	public void removeContainerListener(ContainerListener listener) {


	}

	@Override
	public void removeMapper(Mapper mapper) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public ServletContext getServletContext() {

		return null;
	}

	@Override
	public Manager getManager() {

		return null;
	}

	@Override
	public boolean getCookies() {

		return false;
	}

	@Override
	public String getPath() {
		return null;
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
		log("Starting  context " + name);
		if(started){
			throw new LifecycleException("Simple Context has already started");
		}
		
		//Notify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);
		started = true;
		try{
			//Start our subordinate components, if any
			if(loader != null && loader instanceof Lifecycle){
				((Lifecycle) loader).start();
			}
			
			//Start our children containers, if any
			Container[] children = this.findChildren();
			for (int i = 0; i < children.length; i++) {
				if(children[i] instanceof Lifecycle){
					((Lifecycle) children[i]).start();
				}
			}
			
			//Start the valves in our pipeline (include the basic), if any
			if(pipeline instanceof Lifecycle){
				((Lifecycle)pipeline).start();
			}
			//Notify our insterested LifecycleListener
			lifecycle.fireLifecycleEvent(START_EVENT, null);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		//Notify our insterested LifecycleListener
		lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
		
		log("context started");
	}

	@Override
	public void stop() throws LifecycleException {

		log("Stopping context " + name);
		if(!started){
			throw new LifecycleException("Simple Context has not been started");
		}
		started = false;
		//Notify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);
		lifecycle.fireLifecycleEvent(STOP_EVENT, null);
		
		try{
			//Stop the Valve in our pipeline (including the basic), if any
			if( pipeline instanceof Lifecycle){
				((Lifecycle)pipeline).stop();
			}
			
			//Stop our child containers, if any
			Container[] children = this.findChildren();
			for (int i = 0; i < children.length; i++) {
				if(children[i] instanceof Lifecycle){
					((Lifecycle) children[i]).stop();
				}
			}
			
			//Stop our subordinate components, if any
			if(loader != null && loader instanceof Lifecycle){
				((Lifecycle)loader).stop();
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		//Notify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
		
		log("context stopped");
	}

	private void log(String message){
		Logger logger = this.getLogger();
		if(logger != null){
			logger.log(message);
		}
	}

	@Override
	public boolean getReloadable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setReloadable(boolean reloadable) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reload() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPath(String path) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getConfigured() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setConfigured(boolean configured) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getDocBase() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDocBase(String docBase) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getAvailable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setAvailable(boolean available) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getPrivileged() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setPrivileged(boolean privileged) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ErrorPage findErrorPage(String exceptionType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ErrorPage findErrorPage(int errorCode) {
		// TODO Auto-generated method stub
		return null;
	}

}
