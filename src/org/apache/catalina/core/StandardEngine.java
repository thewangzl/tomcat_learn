package org.apache.catalina.core;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Service;
import org.apache.catalina.util.ServerInfo;

public class StandardEngine extends ContainerBase implements Engine {

	private String defaultHost;
	
	private String mapperClass = "org.apache.catalina.core.StandardEngineMapper";
	
	private Service service;
	
	private DefaultContext defaultContext;
	
	private String jvmRouteId;
	
	private static final String info = "org.apache.catalina.core.StandardEngine/1.0";
	
	public StandardEngine() {
		super();
		this.pipeline.setBasic(new StandardEngineValve());
	}
	@Override
	public String getDefaultHost() {

		return this.defaultHost;
	}

	@Override
	public void setDefaultHost(String defaultHost) {
		String oldDefaultHost = this.defaultHost;
		this.defaultHost = defaultHost;
		support.firePropertyChange("defaultHost", oldDefaultHost, this.defaultHost);
	}

	@Override
	public String getJvmRoute() {
		return this.jvmRouteId;
	}

	@Override
	public void setJvmRoute(String jvmRoute) {
		log("setJvmRoute=" + jvmRoute);

		this.jvmRouteId = jvmRoute;
	}
	
	public void addDefaultContext(DefaultContext defaultContext){
		DefaultContext oldDefaultContext = this.defaultContext;
		this.defaultContext = defaultContext;
		support.firePropertyChange("", oldDefaultContext, this.defaultContext);
	}

	@Override
	public DefaultContext getDefaultContext() {
		return this.defaultContext;
	}
	
	public String getMapperClass() {
		return mapperClass;
	}
	
	public void setMapperClass(String mapperClass) {
		String oldMapperClass = this.mapperClass;
		this.mapperClass = mapperClass;
		support.firePropertyChange("mapperClass", oldMapperClass, this.mapperClass);
	}
	

	@Override
	public Service getService() {

		return this.service;
	}

	@Override
	public void setService(Service service) {

		this.service = service;
	}


	@Override
	public void importDefaultContext(Context context) {
		if(this.defaultContext != null){
			this.defaultContext.importDefaultContext(context);
		}
	}
	
	@Override
	public void addChild(Container child) {
		if(!(child instanceof Host)){
			throw new IllegalArgumentException(sm.getString("standardEngine.notHost"));
		}
		super.addChild(child);
	}
	
	@Override
	public void setParent(Container parent) {
		throw new IllegalArgumentException(sm.getString("standardEngine.notParent"));
	}
	
	@Override
	public synchronized void start() throws LifecycleException {
		System.out.println(ServerInfo.getServerInfo());
		super.start();
	}

	@Override
	public String getInfo() {
		return info;
	}
	
	 /**
     * Return a String representation of this component.
     */
    public String toString() {

        StringBuffer sb = new StringBuffer("StandardEngine[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());

    }

}
