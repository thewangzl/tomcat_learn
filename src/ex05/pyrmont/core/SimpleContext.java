package ex05.pyrmont.core;

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
import org.apache.catalina.Loader;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Mapper;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;

public class SimpleContext implements Context, Pipeline {
	
	private SimplePipeline pipeline =  new SimplePipeline(this);
	
	private HashMap<String, Container> children = new HashMap<>();
	
	private Loader loader = null;
	
	private HashMap<String, String> servletMappings = new HashMap<>();
	
	private Mapper mapper;
	
	private HashMap<String,Mapper> mappers = new HashMap<>();
	
	private Container parent;
	
	private String name;
	
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
	public ContainerListener findContainerListeners() {

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

		return null;
	}

	@Override
	public void setLogger(Logger logger) {

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
	public boolean getReloadable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setReloadable(boolean reloadable) {
		// TODO Auto-generated method stub
		
	}


}
