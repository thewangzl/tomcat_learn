package org.apache.catalina;

import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.naming.directory.DirContext;
import javax.servlet.ServletException;

public interface Container {

	public static final String ADD_CHILD_EVENT = "addChild";
	
	public static final String ADD_MAPPER_EVENT = "addMapper";
	
	public static final String ADD_VALVE_EVENT = "addValve";
	
	public static final String REMOVE_CHILD_EVENT = "removeChild";
	
	public static final String REMOVE_MAPPER_EVENT = "removeMapper";
	
	public static final String REMOVE_VALVE_EVENT = "removeValve";
	
	
	public String getInfo();
	
	
	public Loader getLoader();
	
	public void setLoader(Loader loader);
	
	public Logger getLogger();
	
	public void setLogger(Logger logger);
	
	public Manager getManager();
	
	public void setManager(Manager manager);
	
	public Cluster getCluster();
	
	public void setCluster(Cluster cluster);
	
	public String getName();
	
	public void setName(String name);
	
	public Container getParent();
	
	public void setParent(Container parent);
	
	public ClassLoader getParentClassLoader();
	
	public void setParentClassLoader(ClassLoader parentClassLoader);
	
	public Realm getRealm();
	
	public void setRealm(Realm realm);
	
	public DirContext getResources();
	
	public void setResources(DirContext resources);
	
	
	public void addChild(Container child);
	
	public void addContainerListener(ContainerListener listener);
	
	public void addMapper(Mapper mapper);
	
	public void addPropertyChangeListener(PropertyChangeListener listener);
	
	public Container findChild(String name);
	
	public Container[] findChildren();
	
	public ContainerListener[] findContainerListeners();
	
	public Mapper findMapper(String protocol);
	
	public Mapper[] findMappers();
	
	/**
	 * Process the specified Request, and generate the corresponding Response,
	 * according to the design of this particular Container
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ServletException
	 */
	public void invoke(Request request, Response response) throws IOException, ServletException;
	
	/**
	 * Return the child Container that should be used to process this Request,
	 * based upon its characteristics. If no such child Container can be identified, return null instead.
	 * @param request
	 * @param update
	 * @return
	 */
	public Container map(Request request, boolean update);
	
	public void removeChild(Container child);
	
	public void removeContainerListener(ContainerListener listener);
	
	public void removeMapper(Mapper mapper);
	
	public void removePropertyChangeListener(PropertyChangeListener listener);
	
}
