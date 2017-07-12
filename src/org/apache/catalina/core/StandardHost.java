package org.apache.catalina.core;

import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Valve;
import org.apache.catalina.valves.ErrorDispatcherValve;

/**
 * Standard implementation of the <b>Host</b> interface. Each child container nust be a Context
 * implementation to process the requests directed to a particular web application.
 * 
 * @author thewangzl
 *
 */
public class StandardHost extends ContainerBase implements Host {

	private String[] aliases = new String[0];
	
	/**
	 * The application root for this root.
	 */
	private String appBase = ".";
	
	/**
	 * 
	 */
	private boolean autoDeploy;
	
	/**
	 * The Java class name of the default context configuration class for deployed 
	 * web applications.
	 */
	private String configClass = "org.apache.catalina.startup.ContextConfig";
	
	/**
	 * The Java class name of the default Context implementation class for deployed web applications.
	 */
	private String contextClass = "org.apache.catalina.core.StandardContext";
	
	/**
	 * The Java class name of the default error reporter implementation class for deployed web applicaition./
	 */
	private String errorReportValveClass = "org.apache.catalina.valves.ErrorReportValve";
	
	protected static final String info = "apache.catalina.core.StandardHost/1.0";
	
	/**
	 * The Java class name of the default Mapper class for this Context.
	 */
	protected String mapperClass = "org.apache.catalina.core.StandardHostMapper";
	
	/**
	 * Unpack WARs property
	 */
	private boolean unpackWARs = true;
	
	/**
	 * Work Directory base for applications.
	 */
	private String workDir;
	
	/**
	 * DefaultContext config
	 */
	private DefaultContext defaultContext;
	
	
	public StandardHost() {
		super();
		pipeline.setBasic(new StandardHostValve());
	}
	
	// -------------------------------------------------------------- Properties
	
	
	@Override
	public String getAppBase() {
		
		return this.appBase;
	}

	@Override
	public void setAppBase(String appBase) {
		String oldAppBase = this.appBase;
		this.appBase = appBase;
		support.firePropertyChange("appBase", oldAppBase, this.appBase);
	}

	@Override
	public boolean getAutoDeploy() {
		return this.autoDeploy;
	}

	@Override
	public void setAutoDeploy(boolean autoDeploy) {
		boolean oldAutoDeploy = this.autoDeploy;
		this.autoDeploy = autoDeploy;
		support.firePropertyChange("autoDeploy", oldAutoDeploy, this.autoDeploy);
	}
	
	public String getConfigClass() {
		return configClass;
	}
	 
	public void setConfigClass(String configClass) {
		String oldConfigClass = this.configClass;
		this.configClass = configClass;
		support.firePropertyChange("configClass", oldConfigClass, this.configClass);
	}

	@Override
	public void addDefaultContext(DefaultContext defaultContext) {
		DefaultContext oldDefaultContext = this.defaultContext;
		this.defaultContext = defaultContext;
		support.firePropertyChange("defaultConfig", oldDefaultContext, this.defaultContext);
		
	}

	@Override
	public DefaultContext getDefaultContext() {

		return this.defaultContext;
	}
	
	public String getContextClass() {
		return contextClass;
	}
	public void setContextClass(String contextClass) {
		String oldContextClass = this.contextClass;
		this.contextClass = contextClass;
		support.firePropertyChange("contextClass", oldContextClass, this.contextClass);
	}
	
	public String getMapperClass() {
		return mapperClass;
	}
	public void setMapperClass(String mapperClass) {
		String oldMapperClass = mapperClass;
		this.mapperClass = mapperClass;
		support.firePropertyChange("mapperClass", oldMapperClass, this.mapperClass);
	}
	
	public String getErrorReportValveClass() {
		return errorReportValveClass;
	}
	
	public void setErrorReportValveClass(String errorReportValveClass) {
		String oldErrorReportValveClass = this.errorReportValveClass;
		this.errorReportValveClass = errorReportValveClass;
		support.firePropertyChange("errorReportValveClass", oldErrorReportValveClass, this.errorReportValveClass);;
	}
	
	@Override
	public void setName(String name) {
		if(name == null){
			throw new IllegalArgumentException("standardHost.nullName");
		}
		name = name.toLowerCase();		//Internally all names are lower case
		super.setName(name);
	}
	
	public boolean isUnpackWARs() {
		return unpackWARs;
	}
	
	public void setUnpackWARs(boolean unpackWARs) {
		this.unpackWARs = unpackWARs;
	}
	
	public String getWorkDir() {
		return workDir;
	}
	
	public void setWorkDir(String workDir) {
		this.workDir = workDir;
	}
	
	/**
	 * Import the DefaultContext config into a web application context.
	 * 
	 * @param context
	 */
	@Override
	public void importDefaultContext(Context context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAlias(String alias) {
		alias = alias.toLowerCase();
		
		//Skip deplicate aliases
		for (int i = 0; i < aliases.length; i++) {
			if(aliases[i].equals(alias)){
				return;
			}
		}
		
		//Add this alias to the list
		String[] results = new String[aliases.length + 1];
		for (int i = 0; i < aliases.length; i++) {
			results[i] = aliases[i];
		}
		results[aliases.length] = alias;
		aliases = results;
		
		//Inform interested listeners
		fireContainerEvent(ADD_ALIAS_EVENT, alias);
	}

	@Override
	public String[] findAliases() {

		return this.aliases;
	}

	@Override
	public void removeAlias(String alias) {
		alias = alias.toLowerCase();
		
		synchronized (aliases) {
			//Marke sure this alias is currently present
			int n = -1;
			for (int i = 0; i < aliases.length; i++) {
				if(aliases[i].equals(alias)){
					n = i;
					break;
				}
			}
			if(n < 0){
				return;
			}
			
			//Remove the specified alias
			int j = 0;
			String[] results = new String[aliases.length - 1];
			for (int i = 0; i < aliases.length; i++) {
				if(i != n){
					results[j++] = aliases[i];
				}
			}
			aliases = results;
		}
		
		//Inform interested listeners
		fireContainerEvent(REMOVE_ALIAS_EVENT, alias);
	}
	
	@Override
	public Context map(String uri) {
		if(debug > 0){
			log("Mapping request URI '" + uri + "'");
		}
		if(uri == null){
			return null;
		}
		
		//Match on the longest possible context path prefix
		if(debug > 1){
			log("Trying the longest context path prefix");
		}
		Context context = null;
		String mapuri = uri;
		while(true){
			context = ((Context) findChild(mapuri));
			if(context != null){
				break;
			}
			int slash = mapuri.lastIndexOf('/');
			if(slash < 0){
				break;
			}
			mapuri = mapuri.substring(0, slash);
		}
		
		//If no Context matches, select the default Context.
		if(context == null){
			if(debug > 1){
				log("Trying the default Context");
			}
			context = (Context) findChild("");
		}
		
		//Complain if no Context has been selected
		if(context == null){
			log(sm.getString("standardHost.mappingError", uri));
			return null;
		}
		
		// Return the mapped Context (if any)
		if(debug > 0){
			log("Mapped to context '" + context.getPath() + "'");
		}
		return context;
	}
	
	/**
	 * Start this host.
	 * 
	 * @throws LifecycleException
	 */
	@Override
	public synchronized void start() throws LifecycleException {
		
		//Set error report valve
		if(errorReportValveClass != null && !errorReportValveClass.equals("")){
			try{
				Valve valve = (Valve) Class.forName(errorReportValveClass).newInstance();
				addValve(valve);
			}catch(Throwable t){
				log(sm.getString("standardHost.invalidErrorReportValveClass",errorReportValveClass));
			}
		}

		// Set dispatcher valve
		addValve(new ErrorDispatcherValve());
		
		super.start();
	}
	

	
	@Override
	protected void addDefaultMapper(String mapperClass) {

		super.addDefaultMapper(this.mapperClass);
	}

	@Override
	public String getInfo() {
		return info;
	}

	
	@Override
	public String toString() {

        StringBuffer sb = new StringBuffer();
        if (getParent() != null) {
            sb.append(getParent().toString());
            sb.append(".");
        }
        sb.append("StandardHost[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());
	}

}
