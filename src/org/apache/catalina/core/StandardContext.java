package org.apache.catalina.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.TreeMap;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Mapper;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Wrapper;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.util.CharsetMapper;
import org.apache.catalina.util.RequestUtil;
import org.apache.naming.ContextBindings;
import org.apache.naming.resources.BaseDirContext;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.FileDirContext;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.WARDirContext;
import org.apache.tomcat.util.log.SystemLogHandler;

/**
 * Standard implementation of the <b>Wrapper</b> interface that represents an individual servlet definition.
 * No child Containers are allowed, and the parent Container must be a Context.
 * 
 * @author thewangzl
 *
 */
public class StandardContext extends ContainerBase implements Context {

	//--------------------------------------------------- Instance variables
	
	/**
	 * The set of application listener class names configuired for this application,
	 * in the order they were encountered in the web.xml file.
	 */
	private String[] applicationListeners = new String[0];
	
	/**
	 * The set of instantiated application listener objects, in a one-to-one correspondence
	 * to the class names in <code>applicationListeners</code>.
	 */
	private Object[] applicationListenersObjects = new Object[0];
	
	/**
	 * The set of application parameters defined for this application
	 */
	private ApplicationParameter[] applicationParameters = new ApplicationParameter[0]; 
	
	/**
	 * The application available flag for this Context
	 */
	private boolean available;
	
	/**
	 * The Locale to character set mapper for this application.
	 */
	private CharsetMapper charsetMapper;
	
	/**
	 * The Java class name of this CharsetMapper class to be created.
	 */
	private String charsetMapperClass = "org.apache.catalina.util.CharsetMapper";
	
	/**
	 * The "corrently configured" flag for this context
	 */
	private boolean configured;
	
	/**
	 * The security constraints for this web application
	 */
	private SecurityConstraint[] constraints = new SecurityConstraint[0];
	
	/**
	 * The ServletContext implementation associated with this Context.
	 */
	private ApplicationContext context;
	
	/**
	 * Should we attempt to use cookies for session id communication?
	 */
	private boolean cookies;
	
	/**
	 * Should we allow the <code>ServletContext.getContext()</code> method to 
	 * access the context of other web applications in this server?
	 */
	private boolean crossContext;
	
	
	/**The display name of this web application
	 * 
	 */
	private String displayName;
	
	/**
	 * The document root for this web application.
	 */
	private String docBase;
	
	
	private static final String info = "org.apache.catalina.core.StandardContext/1.0";
	
	/**
	 * The set of classnames of InstanceListeners that will be added to each newly created
	 * Wrapper by <code>createWrapper()</code>.
	 */
	private String[] instanceListeners = new String[0];
	
	/**
	 * The naming context listener for this web application.
	 */
	private NamingContextListener namingContextListener = null;
	/**
	 * The Java class name of the default Mapper class for this Container.
	 */
	private String mapperClass = "org.apache.catalina.core.StandardContextMapper";
	
	/**
	 * The MIME mappings for this web application, keyed by extension.
	 */
	private HashMap<String, String>	mimeMappings = new HashMap<>();
	
	/**
	 * The context initialization parameters for this web application, keyed by name
	 */
	private HashMap<String, String> parameters = new HashMap<>();
	
	
	/**
	 * The request processing pause flag (while reloading occurs)
	 */
	private boolean paused;
	
	/**
	 * The public identifier of the DTD for the web application deployment descriptor
	 * version we are currently parsing. This is used support relaxed validation rules
	 * when processing version 2.2 web.xml files.
	 */
	private String publicId;
	
	/**
	 * The reloadable flag for this web application.
	 */
	private boolean reloadable;
	
	/**
	 * The privileged flag for this web application.
	 */
	private boolean privileged;
	
	/**
	 * Should the next call to <code>addWelcomeFile()</code> cause replacement of any existing welcome files?
	 * This will be set before processing the web application's deployment descriptor, so that application
	 * specified choices <strong>replace</strong>, rather then append to, those defined in the global descriptor.
	 */
	private boolean replaceWelcomeFiles;
	
	/**
	 * The servlet mappings for this web application, keyed by matching pattern
	 */
	private HashMap<String, String> servletMappings = new HashMap<>();
	
	/**
	 * The status code error pages for this web application, keyed by HTTP status code 9a an Integer)
	 */
	private HashMap<Integer, ErrorPage> statusPages = new HashMap<>();
	
	/**
	 * Set flag to true to cause the system.out and system.err to be redirected
	 * to the logger when executing a servlet.
	 */
	private boolean swallowOutput;
	
	/**
	 * The exception pages for this web application, keyed by fully qualified class name of the Java exception.
	 */
	private HashMap<String, ErrorPage> exceptionPages = new HashMap<>();
	
	/**
	 * The set of filter configurations ( and associated filter instances) we have initialized,
	 * keyed by filter name.
	 */
	private HashMap<String, ApplicationFilterConfig> filterConfigs = new HashMap<>();
	
	/**
	 * The set of filter definitions for this application, keyed by filter name.
	 */
	private HashMap<String,FilterDef> filterDefs = new HashMap<>();
	
	/**
	 * The set of filter mappings for this application , in the order they were defined 
	 * in the deployment descriptor.
	 */
	private FilterMap[] filterMaps = new FilterMap[0];
	
	/**
	 * The welcome files for this application.
	 */
	private String[] welcomeFiles = new String[0];
	
	/**
	 * The set of classnames of LifecycleListeners that will be added to each newly
	 * created Wrapper by <code>createWrapper()</code>.
	 */
	private String[] wrapperLifecycles = new String[0];
	
	/**
	 * The set of classnames of ContainerListeners that will be add to each newly 
	 * created Wrapper by <code>createWrapper()</code>
	 */
	private String[] wrapperListeners = new String[0];
	
	/**
	 * The pathname to work directory for this context (relative to the servlet's 
	 * home if not absolute).
	 */
	private String workDir;
	
	/**
	 * Java class name of the Wrapper class implementation we use.
	 */
	private String wrapperClass = "org.apache.catalina.core.StandardWrapper";
	
	/**
	 * JNDI use flag.
	 */
	private boolean useNaming = true;
	
	/**
	 * Filesystem based flag.
	 */
	private boolean filesystemBased;	
	/**
	 * Name of the associated naming context.
	 */
	private String namingContextName;
	
	/**
	 * Caching allowed flag.
	 */
	private boolean cachingAllowed = true;
	
	
	public StandardContext() {
		super();
		pipeline.setBasic(new StandardContextValve());
	}
	
	// -------------------------------------- Context Properties 
	
	public boolean isCachingAllowed() {
		return cachingAllowed;
	}
	
	public void setCachingAllowed(boolean cachingAllowed) {
		this.cachingAllowed = cachingAllowed;
	}
	
	public boolean isUseNaming() {
		return useNaming;
	}
	
	public void setUseNaming(boolean useNaming) {
		this.useNaming = useNaming;
	}
	
	public boolean isFilesystemBased() {
		return filesystemBased;
	}
	
	public Object[] getApplicationListeners(){
		return applicationListenersObjects;
	}
	
	public void setApplicationListeners(Object[] applicationListenersObjects) {
		this.applicationListenersObjects = applicationListenersObjects;
	}
	
	@Override
	public boolean getAvailable() {
		return available;
	}
	
	@Override
	public void setAvailable(boolean available) {
		boolean oldVailable = this.available;
		this.available = available;
		support.firePropertyChange("available", oldVailable, this.available);
	}
	
	public CharsetMapper getCharsetMapper() {
		
		//Create a mapper the first time it is requested
		if(this.charsetMapper == null){
			try {
				Class<?> clazz = Class.forName(this.charsetMapperClass);
				this.charsetMapper = (CharsetMapper) clazz.newInstance();
			} catch (Throwable e) {
				this.charsetMapper = new CharsetMapper();
			}
		}
		return charsetMapper;
	}
	
	public void setCharsetMapper(CharsetMapper charsetMapper) {
		CharsetMapper oldCharsetMapper = this.charsetMapper;
		this.charsetMapper = charsetMapper;
		support.firePropertyChange("charsetMapper", oldCharsetMapper, this.charsetMapper);
	}
	
	@Override
	public boolean getConfigured() {
		return configured;
	}
	
	@Override
	public void setConfigured(boolean configured) {
		boolean oldConfigred = this.configured;
		this.configured = configured;
		support.firePropertyChange("configured", oldConfigred, this.configured);
	}
	
	/**
	 * Return the "use cookies for session id" flag
	 */
	public boolean getCookies() {
		return cookies;
	}
	
	public void setCookies(boolean cookies) {
		boolean oldCookies = this.cookies;
		this.cookies = cookies;
		support.firePropertyChange("cookies", oldCookies, this.cookies);
	}
	
	public boolean getCrossContext() {
		return crossContext;
	}
	
	public void setCrossContext(boolean crossContext) {
		boolean oldCrossContext = this.crossContext;
		this.crossContext = crossContext;
		support.firePropertyChange("crossContext", oldCrossContext, this.crossContext);
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(String displayName) {
		String oldDisplayName = this.displayName;
		this.displayName = displayName;
		support.firePropertyChange("displayName", oldDisplayName, this.displayName);
	}
	
	@Override
	public String getDocBase() {
		return this.docBase;
	}
	
	@Override
	public void setDocBase(String docBase) {
		this.docBase = docBase;
	}
	
	@Override
	public String getInfo() {

		return info;
	}
	
	@Override
	public synchronized void setLoader(Loader loader) {
		
		super.setLoader(loader);
	}
	
	/**
	 * Return the context path for this Context
	 */
	@Override
	public String getPath() {
		return getName();
	} 
	
	/**
	 * Set the context path for this Context.
	 * <p>
	 * <b>IMPLEMENTATION NOTE</b>: The context path is used as the 
	 * "name" for a Context, because it must be unique.
	 * 
	 * @param path
	 */
	@Override
	public void setPath(String path) {
		setName(RequestUtil.URLDecode(path));
	}
	
	public String getPublicId() {
		return publicId;
	}
	public void setPublicId(String publicId) {
		if(debug >= 1){
			log("Setting deployment descriptor public ID to '" + this.publicId + "'");
		}
		String oldPublicId = this.publicId;
		this.publicId = publicId;
		support.firePropertyChange("publicId", oldPublicId, this.publicId);
	}
	
	public boolean getReloadable() {
		return reloadable;
	}
	
	@Override
	public boolean getPrivileged() {
		return this.privileged;
	}

	@Override
	public void setPrivileged(boolean privileged) {
		boolean oldPrivileged = this.privileged;
		this.privileged = privileged;
		support.firePropertyChange("privileged", oldPrivileged, this.privileged);
		
	}
	
	@Override
	public void setReloadable(boolean reloadable) {
		boolean oldReloadable = this.reloadable;
		this.reloadable = reloadable;
		support.firePropertyChange("reloadable", oldReloadable, this.reloadable);
	}
	
	public boolean isReplaceWelcomeFiles() {
		return replaceWelcomeFiles;
	}
	
	public void setReplaceWelcomeFiles(boolean replaceWelcomeFiles) {
		boolean oldReplaceWelcomeFiles = this.replaceWelcomeFiles;
		this.replaceWelcomeFiles = replaceWelcomeFiles;
		support.firePropertyChange("replaceWelcomeFiles", oldReplaceWelcomeFiles, this.replaceWelcomeFiles);
	}
	
	/**
	 * Return the servlet context for which this Context is a facade.
	 */
	@Override
	public ServletContext getServletContext() {
		if(context == null){
			context = new ApplicationContext(getBasePath(), this);
		}
		return context;
	}
	
	public boolean getSwallowOutput() {
		return swallowOutput;
	}
	
	public void setSwallowOutput(boolean swallowOutput) {
		boolean oldSwallowOutput = this.swallowOutput;
		this.swallowOutput = swallowOutput;
		support.firePropertyChange("swallowOutput", oldSwallowOutput, this.swallowOutput);
	}
	
	public String getWrapperClass() {
		return wrapperClass;
	}
	
	public void setWrapperClass(String wrapperClass) {
		this.wrapperClass = wrapperClass;
	}
	
	/**
	 * Set the resources DirContext object with which this Container is associated.
	 */
	@Override
	public synchronized void setResources(DirContext resources) {
		if(resources instanceof BaseDirContext){
			((BaseDirContext) resources).setCached(isCachingAllowed());
		}
		if(resources instanceof FileDirContext){
			filesystemBased = true;
		}
		super.setResources(resources);
		if(started){
			postResources();		// As a servlet contex attribute
		}
	}
	
	public String getCharsetMapperClass() {
		return charsetMapperClass;
	}
	
	public void setCharsetMapperClass(String charsetMapperClass) {
		String oldCharsetMapperClass = this.charsetMapperClass;
		this.charsetMapperClass = charsetMapperClass;
		support.firePropertyChange("charsetMapperClass", oldCharsetMapperClass, this.charsetMapperClass);
	}
	
	/**
	 * Return the default Mapper class name
	 * @return
	 */
	public String getMapperClass() {
		return mapperClass;
	}
	
	/**
	 * Set the default Mapper class name.
	 * 
	 * @param mapperClass The new default Mapper class name
	 */
	public void setMapperClass(String mapperClass) {
		String oldMapperClass = this.mapperClass;
		this.mapperClass = mapperClass;
		support.firePropertyChange("mapperClass", oldMapperClass, this.mapperClass);
	}
	
	public String getWorkDir() {
		return workDir;
	}
	
	public void setWorkDir(String workDir) {
		this.workDir = workDir;
		if(started){
			postWorkDirectory();
		}
	}
	
	// ------------------------------------------------  Context Methods
	
	/**
	 * Add a new Listener class name to the set of Listeners Configured for this application.
	 * 
	 * @param listener
	 */
	public void addApplicationListener(String listener){
		synchronized (applicationListeners) {
			String[] results = new String[this.applicationListeners.length + 1];
			for (int i = 0; i < applicationListeners.length; i++) {
				if(listener.equals(applicationListeners[i])){
					break;
				}
				results[i] = applicationListeners[i];
			}
			results[applicationListeners.length] = listener;
			applicationListeners = results;
		}
		fireContainerEvent("addApplicationListener", listener);
		
		// FIXME - add instance if already started?
	}

	/**
	 * Add a new application parameter for this application.
	 * 
	 * @param parameter The new  application parameter .
	 */
	public void addApplicationParameter(ApplicationParameter parameter){
		synchronized (applicationParameters) {
			String newName = parameter.getName();
			for (ApplicationParameter applicationParameter : applicationParameters) {
				if(newName.equals(applicationParameter.getName()) && !applicationParameter.getOverride()){
					return;
				}
			}
			ApplicationParameter[] results = new ApplicationParameter[applicationParameters.length + 1];
			System.arraycopy(applicationParameters, 0, results, 0, applicationParameters.length);
			results[applicationParameters.length] = parameter;
			applicationParameters = results;
		}
		fireContainerEvent("addApplicationParameter", parameter);
	}
	
	@Override
	public void addChild(Container child) {

		if(!(child instanceof Wrapper)){
			throw new IllegalArgumentException(sm.getString("standardContext.notWrapper"));
		}
		Wrapper wrapper = (Wrapper) child;
		String jspFile = wrapper.getJspFile();
		if(jspFile != null && !jspFile.startsWith("/")){
			if(isServlet22()){
				log(sm.getString("standardContext.wrapper.warning", jspFile));
				wrapper.setJspFile("/" + jspFile);
			}else{
				throw new IllegalArgumentException(sm.getString("standardContext.wrapper.error", jspFile));
			}
		}
		super.addChild(child);
	}
	
	/**
	 * 
	 * @param constraint
	 */
	public void addConstraint(SecurityConstraint constraint){
		//Validate the proposed constraint
		SecurityCollection[] collections = constraint.findCollections();
		for (int i = 0; i < collections.length; i++) {
			String[] patterns = collections[i].findPatterns();
			for (int j = 0; j < patterns.length; j++) {
				patterns[j] = adjustURLPattern(patterns[j]);
				if(!validateURLPattern(patterns[j])){
					throw new IllegalArgumentException(sm.getString("standardContext.securityConstraint.pattern", patterns[j]));
				}
			}
		}
		
		//Add this constraint to the set for our web application
		synchronized (constraints) {
			SecurityConstraint[] results = new SecurityConstraint[constraints.length + 1];
			for (int i = 0; i < constraints.length; i++) {
				results[i] = constraints[i];
			}
			results[constraints.length + 1] = constraint;
			constraints = results;
		}
	}

	/**
	 * Add an error page for the specified error or Java exception
	 * 
	 * @param errorPage The error page definition to be added
	 */
	public void addErrorPage(ErrorPage errorPage){
		
		//Validate the input parameters
		if(errorPage == null){
			throw new IllegalArgumentException(sm.getString("standardContext.errorPage.required"));
		}
		String location = errorPage.getLocation();
		if(location != null && !location.startsWith("/")){
			if(isServlet22()){
				log(sm.getString("standardContext.errorPage.warning", location));
				errorPage.setLocation("/" + location);
			}else{
				throw new IllegalArgumentException(sm.getString("standardContext.errorPage.error", location));
			}
		}
		
		//Add the specified error page to our internal collections
		String exceptionType = errorPage.getExceptionType();
		if(exceptionType != null){
			synchronized (exceptionPages) {
				exceptionPages.put(exceptionType, errorPage);
			}
		}else{
			synchronized (statusPages) {
				statusPages.put(errorPage.getErrorCode(), errorPage);
			}
		}
		fireContainerEvent("addErrorPage", errorPage);
	}
	
	/**
	 * Add a filter definition to this Context
	 * @param filterDef
	 */
	public void addFilterDef(FilterDef filterDef){
		synchronized (filterDefs) {
			filterDefs.put(filterDef.getFilterName(), filterDef);
		}
		fireContainerEvent("addFilterDef", filterDef);
	}
	
	/**
	 * Add the class name of an InstanceListener
	 * @param listener
	 */
	public void addInstanceListener(String listener){
		synchronized (instanceListeners) {
			String[] results = new String[instanceListeners.length + 1];
			for (int i = 0; i < instanceListeners.length; i++) {
				results[i] = instanceListeners[i];
			}
			results[instanceListeners.length] = listener;
			instanceListeners = results;
		}
		fireContainerEvent("addInstanceListener", listener);
	}
	
	/**
	 * 
	 * @param extension Filename extension being mapped
	 * @param mimeType Corresponding MIME type
	 */
	public void addMimeMapping(String extension, String mimeType){
		synchronized (mimeMappings) {
			mimeMappings.put(extension, mimeType);
		}
		fireContainerEvent("addMimeMapping", extension);
	}
	
	
	public void addParameter(String name, String value){
		//validate the proposed context initialization parameter
		if(name  == null || value == null){
			throw new IllegalArgumentException(sm.getString("standardContext.parameter.required"));
		}
		if(parameters.get(name) != null){
			throw new IllegalArgumentException(sm.getString("standardContext.parameter.duplicate", name));
		}
		
		//Add this parameter to our defined set
		synchronized (parameters) {
			parameters.put(name, value);
		}
		fireContainerEvent("addParameter", name);
	}
	

	/**
	 * Add a new servlet mapping, replacing any existing mapping for the specified pattern
	 * 
	 * @param pattern
	 * @param name
	 */
	@Override
	public void addServletMapping(String pattern, String name) {
		//Validate the proposed mapping
		if(findChild(name) == null){
			throw new IllegalArgumentException(sm.getString("standardContext.servletMap.name", name));
		}
		pattern = adjustURLPattern(RequestUtil.URLDecode(pattern));
		if(!validateURLPattern(pattern)){
			throw new IllegalArgumentException(sm.getString("standardContext.servletMap.pattern", pattern));
		}
		
		//Add this mapping to our registered set
		synchronized (servletMappings) {
			servletMappings.put(pattern, name);
		}
		fireContainerEvent("addServletMapping", pattern);
	}
	
	public void addWelcomeFile(String name){
		synchronized (welcomeFiles) {
			//Welcome files from the application deployment descriptor
			//completely replace those from the default conf/web.xml file
			if(replaceWelcomeFiles){
				welcomeFiles = new String[0];
				setReplaceWelcomeFiles(false);
			}
			String[] results = new String[welcomeFiles.length + 1];
			for (int i = 0; i < welcomeFiles.length; i++) {
				results[i] = welcomeFiles[i];
			}
			results[welcomeFiles.length] = name;
			welcomeFiles = results;
		}
		postWelcomeFiles();
		fireContainerEvent("addWelcomeFile", name);
	}
	
	public void addWrapperLifecycle(String listener){
		synchronized (wrapperLifecycles) {
			String[] results = new String[wrapperLifecycles.length + 1];
			for (int i = 0; i < wrapperLifecycles.length; i++) {
				results[i] = wrapperLifecycles[i];
			}
			results[wrapperLifecycles.length] = listener;
		}
		fireContainerEvent("addWrapperLifecycle", listener);
	}
	
	public void addWrapperListener(String listener){
		synchronized (wrapperListeners) {
			String[] results = new String[wrapperListeners.length + 1];
			for (int i = 0; i < wrapperListeners.length; i++) {
				results[i] = wrapperListeners[i];
			}
			results[wrapperListeners.length] = listener;
			wrapperListeners = results;
		}
		fireContainerEvent("addWrapperListener", listener);
	}
	
	/**
	 * Factory method to create and return a new Wrapper instance, of the Java implementation class 
	 * appropriate for this Context implementation. The constructor of the instantiated Wrapper will
	 * have been called, but no properties will have been set.
	 * 
	 * @return
	 */
	public Wrapper createWrapper(){
		Wrapper wrapper =  new StandardWrapper();
		
		synchronized (instanceListeners) {
			for (String instanceListener : instanceListeners) {
				try {
					Class<?> clazz = Class.forName(instanceListener);
					InstanceListener listener = (InstanceListener) clazz.newInstance();
					wrapper.addInstanceListener(listener);
				} catch (Throwable e) {
					log("createWrapper", e);
					return null;
				}
			}
		}
		
		synchronized (wrapperLifecycles) {
			for (String wrappeLifecycle : wrapperLifecycles) {
				try {
					Class<?> clazz = Class.forName(wrappeLifecycle);
					LifecycleListener listener = (LifecycleListener) clazz.newInstance();
					if(wrapper instanceof Lifecycle){
						((Lifecycle) wrapper).addLifecycleListener(listener);
					}
				} catch (Throwable e) {
					log("createWrapper", e);
					return null;
				}
			}
		}
		synchronized (wrapperListeners) {
			for (String wrapperListener : wrapperListeners) {
				try {
					Class<?> clazz = Class.forName(wrapperListener);
					ContainerListener listener = (ContainerListener) clazz.newInstance();
					wrapper.addContainerListener(listener);
				} catch (Throwable e) {
					log("createWrapper", e);
					return null;
				}
			}
		}
		return wrapper;
	}
	
	/**
	 * 
	 * @return
	 */
	public String[] findApplicationListeners(){
		return this.applicationListeners;
	}
	
	public ApplicationParameter[] findApplicationParameters(){
		return applicationParameters;
	}
	
	public SecurityConstraint[] findConstraints(){
		return constraints;
	}
	
	@Override
	public ErrorPage findErrorPage(int errorCode){
		return statusPages.get(errorCode);
	}
	
	@Override
	public ErrorPage findErrorPage(String exceptionType){
		synchronized (exceptionPages) {
			return exceptionPages.get(exceptionType);
		}
	}
	
	public ErrorPage[] findErrorPages(){
		synchronized (exceptionPages) {
			synchronized (statusPages) {
				ErrorPage[] results1 = exceptionPages.values().toArray(new ErrorPage[exceptionPages.size()]);
				ErrorPage[] results2 = statusPages.values().toArray(new ErrorPage[statusPages.size()]);
				ErrorPage[] results = new ErrorPage[results1.length + results2.length];
				for (int i = 0; i < results1.length; i++) {
					results[i] = results1[i];
				}
				for (int i = results1.length; i < results.length; i++) {
					results[i] = results2[i - results1.length];
				}
				return results;
			}
		}
	}
	
	public FilterDef findFilterDef(String name){
		synchronized (filterDefs) {
			return filterDefs.get(name);
		}
	}
	
	public FilterDef[] findFilterDefs(){
		synchronized (filterDefs) {
			return filterDefs.values().toArray(new FilterDef[filterDefs.size()]);
		}
	}
	
	public FilterMap[] findFilterMaps(){
		return filterMaps;
	}
	
	public String[] findInstanceListeners(){
		return this.instanceListeners;
	}
	
	public String findMimeMapping(String extension){
		synchronized (mimeMappings) {
			return mimeMappings.get(extension);
		}
	}
	
	public String[] findMimeMappings(){
		synchronized (mimeMappings) {
			return mimeMappings.keySet().toArray(new String[mimeMappings.size()]);
		}
	}
	
	public String findParameter(String name){
		synchronized (parameters) {
			return parameters.get(name);
		}
	}
	
	public String[] findParameters(){
		synchronized (parameters) {
			return parameters.keySet().toArray(new String[parameters.size()]);
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
			return servletMappings.keySet().toArray(new String[servletMappings.size()]);
		}
	}
	
	/**
	 * 
	 * @param status
	 * @return
	 */
	public String findStatusPage(String status){
		return statusPages.get(new Integer(status)).getLocation();
	}

	public int[] findStatusPages(){
		synchronized (statusPages) {
			int[] results = new int[statusPages.size()];
			int i = 0;
			for (Integer status : statusPages.keySet()) {
				results[i++] = status;
			}
			return results;
		}
	}
	
	public boolean findWelcomeFile(String name){
		synchronized (welcomeFiles) {
			for (int i = 0; i < welcomeFiles.length; i++) {
				if(name.equals(welcomeFiles[i])){
					return true;
				}
			}
		}
		return false;
	}
	
	public String[] findWelcomeFiles(){
		return welcomeFiles;
	}
	
	public String[] findWrapperLifecyclies(){
		return this.wrapperLifecycles;
	}
	
	public String[] findWrapperListeners(){
		return wrapperListeners;
	}
	
	/**
	 * Process the specified Request, and generate the corresponding Response. 
	 * according to the design of this particulat Container.
	 */
	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {

		//Wait if we are reloading
		while(getPaused()){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				;
			}
		}
		
		//Normal request processing
		if(swallowOutput){
			try {
				SystemLogHandler.startCapture();
				super.invoke(request, response);
			} finally {
				String log = SystemLogHandler.stopCapture();
				if(log != null && log.length() > 0){
					log(log);
				}
			}
		}else{
			super.invoke(request, response);
		}
	}


	/**
	 * Reload this web application, if reloading is supported.
	 * 
	 */
	@Override
	public synchronized void reload() {

		if(!started){
			throw new IllegalStateException(sm.getString("containerBase.notStarted", logName()));
		}

		log(sm.getString("standardContext.reloadingStarted"));
		
		//Stop accepting requests temporarily
		setPaused(true);
		
		//Binding thread
		ClassLoader oldCCL = bindThread();
		
		//Shut down our session manager
		if(manager != null &&manager instanceof Lifecycle){
			try {
				((Lifecycle) manager).stop();
			} catch (LifecycleException e) {
				log(sm.getString("standardContext.stopContext.stoppingManager"), e);
			}
		}
		
		//Shut down the current version of all active servlets
		Container[] children = findChildren();
		for (Container child : children) {
			Wrapper wrapper = (Wrapper) child;
			if(wrapper instanceof Lifecycle){
				try {
					((Lifecycle) wrapper).stop();
				} catch (LifecycleException e) {
					log(sm.getString("standardContext.stoppingWrapper", wrapper.getName()),e );
				}
			}
		}
		
		// Shut down application event listeners
		listenerStop();
		
		//Clear all application-originated servlet
		if(context != null){
			context.clearAttributes();
		}
		
		// Shut down filters
		filterStop();
		
		if(isUseNaming()){
		 
			namingContextListener.lifecycleEvent(new LifecycleEvent(this, STOP_EVENT));
		}
		
		//
		unbindThread(oldCCL);
		
		//Shut down our application class loader
		if(loader != null && loader instanceof Lifecycle){
			try{
				((Lifecycle) loader).stop();
			}catch(LifecycleException e){
				log(sm.getString("standardContext.stoppingLoader"), e);
			}
		}
		
		//Binding thread
		oldCCL = bindThread();
		
		//Restart our application class loader
		if(loader != null && loader instanceof Lifecycle){
			try{
				((Lifecycle) loader).start();
			}catch(LifecycleException e){
				log(sm.getString("standardContext.startingLoader"), e);
			}
		}
		
		//
		unbindThread(oldCCL);
		
		//Create and register the associated naming context, if internal naming is used
		boolean ok = true;
		if(isUseNaming()){
			//Start
			namingContextListener.lifecycleEvent(new LifecycleEvent(this, START_EVENT));
		}
		
		//
		oldCCL = bindThread();
		
		//Restat our application event listeners and filters
		if(ok){
			if(!listenerStart()){
				log(sm.getString("standardContext.listenerStartFailed"));
				ok = false;
			}
		}
		if(ok){
			if(!filterStart()){
				log(sm.getString("standardContext.fiterStartFailed"));
				ok = false;
			}
		}
		
		// Restore the "Welcome Files" and "resources" context attributes
		postResources();
		postWelcomeFiles();
		
		//Restart our currently defined servlets
		for (Container child : children) {
			if(!ok){
				break;
			}
			Wrapper wrapper = (Wrapper) child;
			if(wrapper instanceof Lifecycle){
				try{
					((Lifecycle) wrapper).stop();
				}catch(LifecycleException e){
					log(sm.getString("standardContext.startingWrapper", wrapper.getName()));
					ok = false;
				}
			}
		}
		
		// Reintialize all load on startup servlets
		loadOnStartup(children);
		
		//Restart our session manager (AFTER naming context recreated/bound)
		if(manager != null && manager instanceof Lifecycle){
			try{
				((Lifecycle)manager).start();
			}catch(LifecycleException e){
				log(sm.getString("standardContext.startingManager"), e);
			}
		}
		
		//unbinding thread
		unbindThread(oldCCL);
		
		//Start accepting requests again
		if(ok){
			log(sm.getString("standardContext.reloadingCompleted"));
		}else{
			setAvailable(false);
			log(sm.getString("standardContext.reloadingFailed"));
		}
		setPaused(false);
		
		//Notify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(Context.RELOAD_EVENT, null);
		
	}

	public void removeApplicationListener(String listener){
		synchronized (applicationListeners) {
			
			//Make sure this welcome file is currently present
			int n = -1;
			for (int i = 0; i < applicationListeners.length; i++) {
				if(applicationListeners[i].equals(listener)){
					n = i;
					break;
				}
			}
			if(n < 0){
				return;
			}
			
			//Remove the specified listener
			int j = 0;
			String[] results = new String[applicationListeners.length -1];
			for (int i = 0; i < applicationListeners.length; i++) {
				if(i != n){
					results[j++] = applicationListeners[i];
				}
			}
			applicationListeners = results;
		}
		
		//Inform interested listeners
		fireContainerEvent("removeApplicationListener", listener);
		
		//FIXME - behavious is already started
	}
	
	public void removeApplicationParameter(String name){
		synchronized (applicationParameters) {
			
			//Make sure this welcome file is currently present
			int n = -1;
			for (int i = 0; i < applicationParameters.length; i++) {
				if(applicationParameters[i].getName().equals(name)){
					n = i;
					break;
				}
			}
			if(n < 0){
				return;
			}
			
			//Remove the specified parameter
			int j = 0;
			ApplicationParameter[] results = new ApplicationParameter[applicationParameters.length -1];
			for (int i = 0; i < applicationParameters.length; i++) {
				if(i != n){
					results[j++] = applicationParameters[i];
				}
			}
			applicationParameters = results;
		}
		
		//Inform interested listeners
		fireContainerEvent("removeApplicationParameter", name);
		
	}
	
	public void removeConstraint(SecurityConstraint constraint){
		synchronized (constraints) {
			
			//Make sure this welcome file is currently present
			int n = -1;
			for (int i = 0; i < constraints.length; i++) {
				if(constraints[i].equals(constraint)){
					n = i;
					break;
				}
			}
			if(n < 0){
				return;
			}
			
			//Remove the specified constraint
			int j = 0;
			SecurityConstraint[] results = new SecurityConstraint[constraints.length -1];
			for (int i = 0; i < applicationParameters.length; i++) {
				if(i != n){
					results[j++] = constraints[i];
				}
			}
			constraints = results;
		}
		
		//Inform interested listeners
		fireContainerEvent("removeConstraint", constraint);
		
	}
	
	public void removeErrorPage(ErrorPage errorPage){
		String exceptionType = errorPage.getExceptionType();
		if(exceptionType != null){
			synchronized (exceptionPages) {
				exceptionPages.remove(exceptionType);
			}
		}else{
			synchronized (statusPages) {
				statusPages.remove(errorPage.getErrorCode());
			}
		}
		fireContainerEvent("removeErrorPage", errorPage);
	}
	
	public void removeFilterDef(FilterDef filterDef){
		synchronized (filterDefs) {
			filterDefs.remove(filterDef.getFilterName());
		}
		fireContainerEvent("removeFilterDef", filterDef);
	}
	
	public void removeFilterMap(FilterMap filterMap){
		synchronized (filterMaps) {
			//
			int n = -1;
			for (int i = 0; i < filterMaps.length; i++) {
				if(filterMaps[i] == filterMap){
					n = i;
					break;
				}
			}
			if(n < 0){
				return;
			}
			
			FilterMap[] results = new FilterMap[filterMaps.length - 1];
			System.arraycopy(filterMaps, 0, results, 0, n);
			System.arraycopy(filterMaps, n + 1, results, n, (filterMaps.length -1 -n));
			filterMaps = results;
		}
		fireContainerEvent("removeFilterMap", filterMap);
	}
	
	public void removeInstanceListener(String listener){
		synchronized (instanceListeners) {
			
			int n = -1;
			for (int i = 0; i < instanceListeners.length; i++) {
				if(instanceListeners[i].equals(listener)){
					n = i;
					break;
				}
			}
			if(n < 0){
				return;
			}
			int j = 0;
			String[] results = new String[instanceListeners.length - 1];
			for (int i = 0; i < instanceListeners.length; i++) {
				if(i != n){
					results[j++] = instanceListeners[i];
				}
			}
			instanceListeners = results;
		}
		fireContainerEvent("removeInstanceListener", listener);
	}

	public void removeParameter(String name){
		synchronized (parameters) {
			parameters.remove(name);
		}
		fireContainerEvent("removeParameter", name);
	}
	
	public void removeServletMappings(String pattern){
		synchronized (servletMappings) {
			servletMappings.remove(pattern);
		}
		fireContainerEvent("removeServletMappings", pattern);
	}
	
	public void removeWelcomeFile(String name){
		synchronized (welcomeFiles) {
			int n = -1;
			for (int i = 0; i < welcomeFiles.length; i++) {
				if(welcomeFiles[i].equals(name)){
					n = i;
					break;
				}
			}
			if(n < 0){
				return;
			}
			
			int j = 0;
			String[] results = new String[welcomeFiles.length -1];
			for (int i = 0; i < welcomeFiles.length; i++) {
				if(i != n){
					results[j++] = welcomeFiles[i];
				}
			}
			welcomeFiles = results;
		}
		postWelcomeFiles();
		fireContainerEvent("removeWelcomeFile", name);
	}
	
	public void removeWrapperLifecycle(String listener){
		synchronized (wrapperLifecycles) {
			int n = -1;
			for (int i = 0; i < wrapperLifecycles.length; i++) {
				if(wrapperLifecycles[i].equals(listener)){
					n = i;
					break;
				}
			}
			if(n < 0){
				return;
			}
			int j = 0;
			String[] results = new String[wrapperLifecycles.length - 1];
			for (int i = 0; i < wrapperLifecycles.length; i++) {
				if(n != i){
					results[j++] = wrapperLifecycles[i];
				}
			}
			wrapperLifecycles = results;
		}
		fireContainerEvent("removeWrapperLifecycle", listener);
	}
	
	public void removeWrapperListener(String listener){
		synchronized (wrapperListeners) {
			int n = -1;
			for (int i = 0; i < wrapperListeners.length; i++) {
				if(wrapperListeners[i].equals(listener)){
					n = i;
					break;
				}
			}
			if(n < 0 ){
				return;
			}
			int j = 0;
			String[] results = new String[wrapperListeners.length - 1];
			for (int i = 0; i < wrapperListeners.length; i++) {
				if(n != i){
					results[j++] = wrapperListeners[i];
				}
			}
			wrapperListeners = results;
		}
		fireContainerEvent("removeWrapperListener", listener);
	}
	
	/**
	 * Configure and initialize the set of filter for this Context. Return <code>true</code>
	 * if all filter initialization completed successfully, or <code>false</code> otherwise.
	 * 
	 * @return
	 */
	public boolean filterStart(){
		if(debug >= 1){
			log("Starting filters");
		}
		
		//Instantiate and record a FilterConfig for each defined filter.
		boolean ok = true;
		synchronized (filterConfigs) {
			filterConfigs.clear();
			Iterator<String> names = filterDefs.keySet().iterator();
			while(names.hasNext()){
				String name = names.next();
				if(debug >= 1){
					log("Starting filter '" + name + "'");
				}
				ApplicationFilterConfig filterConfig = null;
				try {
					filterConfig = new ApplicationFilterConfig(this, filterDefs.get(name));
					filterConfigs.put(name, filterConfig);
				} catch (Throwable e) {
					log(sm.getString("standardContext.filterStart"), e);
					ok = false;
				} 
			}
		}
		return ok;
	}
	
	/**
	 * Finilized and release the set of filters for this Context. Return <code>true</code> if all 
	 * filter finalization completed successfully, or <code>false</code> otherwise.
	 * 
	 * @return
	 */
	public boolean filterStop(){
		
		if(debug >= 1){
			log("Stopping filtres");
		}
		
		synchronized (filterConfigs) {
			Iterator<String> names = filterConfigs.keySet().iterator();
			while(names.hasNext()){
				String name = names.next();
				if(debug >= 1){
					log("Stopping filter '" + name + "'");
				}
				ApplicationFilterConfig filterConfig = filterConfigs.get(name);
				filterConfig.release();
			}
			filterConfigs.clear();
		}
		return true;
	}

	public ApplicationFilterConfig findFilterConfig(String filterName) {
		synchronized (filterConfigs) {
			return filterConfigs.get(name);
		}
	}
	
	/**
	 * Configure the set of instantizated application event listeners for this Context.
	 * Return <code>true</code> if all listeners are initialized successfully, or 
	 * <code>false</code> otherwise.
	 * 
	 * @return
	 */
	public boolean listenerStart(){
		if(debug >= 1){
			log("Configuring application event listeners");
		}
		
		//Instantiate the required listeners
		ClassLoader loader = getLoader().getClassLoader();
		String[] listeners = findApplicationListeners();
		Object[] results = new Object[listeners.length];
		boolean ok = true;
		for (int i = 0; i < results.length; i++) {
			if(debug >= 2){
				log("Configuring event listener class '" + listeners[i] + "'");
			}
			try {
				Class<?> clazz = loader.loadClass(listeners[i]);
				results[i] = clazz.newInstance();
			} catch (Throwable e) {
				log(sm.getString("standardContext.applicationListener", listeners[i]), e);
				ok = false;
			}
		}
		
		if(!ok){
			log(sm.getString("standardContext.applicationSkipped"));
			return false;
		}
		
		if(debug >= 1){
			log("Sending application start events");
		}
		
		setApplicationListeners(results);
		Object[] instances = getApplicationListeners();
		if(instances == null){
			return ok;
		}
		ServletContextEvent event = new ServletContextEvent(getServletContext());
		for (int i = 0; i < instances.length; i++) {
			if(instances[i] == null){
				continue;
			}
			if(!(instances[i] instanceof ServletContextListener)){
				continue;
			}
			ServletContextListener listener = (ServletContextListener) instances[i];
			try {
				fireContainerEvent("beforeContextInitialized", listener);
				listener.contextInitialized(event);
				fireContainerEvent("afterContextInitialized", listener);
			} catch (Throwable e) {
				fireContainerEvent("afterContextInitialized", listener);
				log(sm.getString("standardContext.listenerStart", instances[i].getClass().getName()), e);
				ok = false;
			}
		}
		return ok;
	}
	
	/**
	 * Send an application stop event to all interested listeners. Return <code>true</code> if all events 
	 * were sent successfully, or <code>false</code> otherwise.
	 * 
	 * @return
	 */
	public boolean listenerStop(){
		if(debug >= 1){
			log("Sending application stop events");
		}

		boolean ok = true;
		Object[] listeners = getApplicationListeners();
		if(listeners == null){
			return ok;
		}
		ServletContextEvent event = new ServletContextEvent(getServletContext());
		for (int i = 0; i < listeners.length; i++) {
			int j = listeners.length - 1 - i;
			if(listeners[j] == null){
				continue;
			}
			if(!(listeners[j] instanceof ServletContextListener)){
				continue;
			}
			ServletContextListener listener = (ServletContextListener) listeners[j];
			try{
				fireContainerEvent("beforeContextDestroyed", listener);
				listener.contextDestroyed(event);
				fireContainerEvent("afterContextDestroyed", listener);
				
			}catch(Throwable e){
				fireContainerEvent("afterContextDestroyed", listener);
				ok = false;
			}
		}
		setApplicationListeners(null);
		return ok;
	}
	
	/**
	 * Load and initialze all servlets marks "load on startup" in the wep application deployment descriptor.
	 * 
	 * @param children Arrays of wrappers for all currently defined servlets
	 */
	public void loadOnStartup(Container[] children){
		
		//Collect "load on startup" servlets that need to be initialized
		TreeMap<Integer, ArrayList<Wrapper>> map = new TreeMap<>();
		for (int i = 0; i < children.length; i++) {
			Wrapper wrapper = (Wrapper) children[i];
			int loadOnSartup = wrapper.getLoadOnStartup();
			if(loadOnSartup < 0){
				continue;
			}
			if(loadOnSartup == 0){		//Arbitrarily put them last
				loadOnSartup = Integer.MAX_VALUE;
			}
			Integer key = new Integer(loadOnSartup);
			ArrayList<Wrapper> list = map.get(key);
			if(list == null){
				list = new ArrayList<>();
				map.put(key, list);
			}
			list.add(wrapper);
		}
		
		//Load the collected "load on startup" servlets
		Iterator<Integer> keys = map.keySet().iterator();
		while(keys.hasNext()){
			Integer key = keys.next();
			ArrayList<Wrapper> list = map.get(key);
			Iterator<Wrapper> wrappers = list.iterator();
			while(wrappers.hasNext()){
				Wrapper wrapper = wrappers.next();
				try {
					wrapper.load();
				} catch (ServletException e) {
					log(sm.getString("standardWrapper.loadException", getName()), e);
				}
			}
		}
	}

	/**
	 * Start this context component.
	 * 
	 * @throws LifecycleException
	 */
	@Override
	public synchronized void start() throws LifecycleException {
		if(started){
			throw new LifecycleException(sm.getString("containerBase.alreadyStarted", logName()));
		}
		if(debug >= 1){
			log("Stating");
		}
		
		//Nofify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, name);
		
		if(debug >= 1){
			log("Processing start(), current available=" + getAvailable());
		}
		setAvailable(false);
		setConfigured(false);
		boolean ok = true;
		
		//Add missing components as necessary
		if(getResources() == null){					//1.Required by loader
			if(debug >= 1){
				log("Configuring default Resources");
			}
			try{
				if(docBase != null && docBase.endsWith(".war")){
					setResources(new WARDirContext());
				}else{
					setResources(new FileDirContext());
				}
			}catch(IllegalArgumentException e){
				log("Error initializing resources:" + e.getMessage());
				ok = false;
			}
		}
		if(ok && resources instanceof ProxyDirContext){
			DirContext dirContext = ((ProxyDirContext) resources).getDirContext();
			if(dirContext != null && dirContext instanceof BaseDirContext){
				((BaseDirContext)dirContext).setDocBase(getBasePath());
				((BaseDirContext) dirContext).allocate();
			}
		}
		
		if(getLoader() == null){					// 2. Required by Manager
			if(getPrivileged()){
				if(debug >= 1){
					log("Configuring privileged default Loader");
				}
				setLoader(new WebappLoader(this.getClass().getClassLoader()));
			}else{
				if(debug >= 1){
					log("Configuring non-privileged default Loader");
				}
				setLoader(new WebappLoader(getParentClassLoader()));
			}
		}
		if(getManager() == null){					// 3. After prerequistes
			if(debug >= 1){
				log("Configuring default Manager");
			}
			setManager(new StandardManager());
		}
		
		//Initialize character set mapper
		getCharsetMapper();
		
		//Post work directory
		postWorkDirectory();
		
		//Reading the "catalina.useNaming" environment variable
		String useNamingProperty = System.getProperty("catalina.useNaming");
		if(useNamingProperty != null && useNamingProperty.equals("false")){
			useNaming = false;
		}
		
		if(ok && isUseNaming()){
			if(namingContextListener == null){
				namingContextListener = new NamingContextListener();
				namingContextListener.setDebug(getDebug());
				namingContextListener.setName(getNamingContextName());
				addLifecycleListener(namingContextListener);
			}
		}
		
		// Binding thread
		ClassLoader oldCCL = bindThread();
		
		//Standard container startup
		if(debug >= 1){
			log("Processing standard container startup");
		}
		
		if(ok){
			try{
				
				addDefaultMapper(mapperClass);
				started = true;
				
				// Start our subordinate components, if any
				if(loader != null && loader instanceof Lifecycle){
					((Lifecycle) loader).start();
				}
				if(logger != null && logger instanceof Lifecycle){
					((Lifecycle) logger).start();
				}
				
				//unbinding Thread
				unbindThread(oldCCL);
				
				//Binding thread
				oldCCL = bindThread();
				
				if(cluster != null && cluster instanceof Lifecycle){
					((Lifecycle)cluster).start();
				}
				if(realm != null && realm instanceof Lifecycle){
					((Lifecycle) realm).start();
				}
				if(resources != null && resources instanceof Lifecycle){
					((Lifecycle) resources).stop();
				}
				
				//Start our mappers, if any
				Mapper[] mappers = findMappers();
				for (Mapper mapper : mappers) {
					if(mapper instanceof Lifecycle){
						((Lifecycle) mapper).start();
					}
				}
				
				//Start our child containers, if any
				Container[] children = findChildren();
				for (Container child : children) {
					if(child instanceof Lifecycle){
						((Lifecycle) child).start();
					}
				}
				//Start the valves in our pipeline (including the basic), if any
				if(pipeline instanceof Lifecycle){
					((Lifecycle) pipeline).start();
				}
				
				//Notify our intersted LifecycleListeners
				lifecycle.fireLifecycleEvent(START_EVENT, null);
				
				if(manager != null && manager instanceof Lifecycle){
					((Lifecycle) manager).start();
				}
			}catch(Exception e){
            	e.printStackTrace();
            }finally{
				//Unbinding thrad
				unbindThread(oldCCL);
			}
		}
		
		if(!getConfigured()){
			ok = false;
		}
		
		//We put the resources into the servlet context
		if(ok){
			postResources();
//			getServletContext().setAttribute(Globals.RESOURCES_ATTR, getResources());
		}
		
		//Binding thread
		oldCCL = bindThread();
		
		//Create context attributes that will be required
		if(ok){
			if(debug >= 1){
				log("Posting standard context attributes");
			}
			postWelcomeFiles();
		}
		
		//Cnfiguere and call application event listeners and filters
		if(ok){
			if(!listenerStart()){
				ok = false;
			}
		}
		if(ok){
			if(!filterStart()){
				ok = false;
			}
		}
		
		// Load and initialize all "load on startup" servlets
		if(ok){
			loadOnStartup(findChildren());
		}
		
		//Unbinding thread
		unbindThread(oldCCL);
		
		// Set available status depending upon startup sucess
		if(ok){
			if(debug >= 1){
				log("Starting completed");
			}
			setAvailable(true);
		}else{
			log(sm.getString("standardContext.startFailed"));
			try{
				stop();
			}catch(Throwable t){
				log(sm.getString("standardContext.startCleanup"), t);
			}
			setAvailable(false);
		}
		
		// Notify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
	}
	
	/**
	 * Stop this Context component
	 * 
	 * @throws LifecycleException
	 */
	@Override
	public void stop() throws LifecycleException {
		
		//Validate and update our current component state
		if(!started){
			throw new LifecycleException(sm.getString("containerBase.notStarted", logName()));
		}
		if(debug >= 1){
			log("Stopping");
		}
		
		//Notify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);
		
		//Mark this application as unavailable while we shut down 
		setAvailable(false);
		
		//Binding thread
		ClassLoader oldCCL = bindThread();
		
		//Stop our filters
		filterStop();
		
		//Finilize our character set mapper
		setCharsetMapper(null);
		
		if(manager != null && manager instanceof Lifecycle){
			((Lifecycle) manager).stop();
		}
		
		//Normal container shutdown processing
		if(debug >= 1){
			log("Processing standard container shutdown");
		}
		
		lifecycle.fireLifecycleEvent(STOP_EVENT, null);
		started =  false;
		
		try {
			
			//Stop the valves in our pipeline
			if(pipeline instanceof Lifecycle){
				((Lifecycle) pipeline).stop();
			}
			
			//Stop our child containers, if any
			Container[] children = findChildren();
			for (int i = 0; i < children.length; i++) {
				if(children[i] instanceof Lifecycle){
					((Lifecycle) children[i]).stop();
				}
			}
				
			//Stop our Mappers, if any
			Mapper[] mappers = findMappers();
			for (int i = mappers.length; i >=0; i--) {
				if(mappers[i] instanceof Lifecycle){
					((Lifecycle) mappers[i]).stop();
				}
			}
			
			//Stop our application listeners
			listenerStop();
			
			if(resources != null){
				if(resources instanceof Lifecycle){
					((Lifecycle) resources).stop();
				}else if(resources instanceof ProxyDirContext){
					DirContext dirContext = ((ProxyDirContext) resources).getDirContext();
					if(dirContext != null){
						if(debug >= 1){
							log("Releaseing document base " + docBase);
						}
						if(dirContext instanceof BaseDirContext){
							((BaseDirContext) dirContext).release();
							if(dirContext instanceof WARDirContext || dirContext instanceof FileDirContext){
								resources = null;
							}
						}else{
							log("Cannot release " + resources);
						}
					}
				}
			}
			
			if(realm != null && realm instanceof Lifecycle){
				((Lifecycle) realm).stop();
			}
			if(cluster != null && cluster instanceof Lifecycle){
				((Lifecycle) cluster).stop();
			}
			if(logger != null && logger instanceof Lifecycle){
				((Lifecycle) logger).stop();
			}
			if(loader != null && loader instanceof Lifecycle){
				((Lifecycle) loader).stop();
			}
			
			
		} finally {
			unbindThread(oldCCL);
		}
		
		//Reset application context
		
		//
		lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
		
		if(debug >= 1){
			log("Stopping complete");
		}
		
		
	}
	
	/**
	 * Get base path
	 * 
	 * @return
	 */
	private String getBasePath(){
		String docBase = null;
		Container container = this;
		while(container != null){
			if(container instanceof Host){
				break;
			}
			container = container.getParent();
		}
		if(container == null){
			docBase = (new File(engineBase(), getDocBase())).getPath();
		}else{
			File file = new File(getDocBase());
			if(!file.isAbsolute()){
				//Use the "appBase" property of this container
				String appBase = ((Host)container).getAppBase();
				file = new File(appBase);
				if(!file.isAbsolute()){
					file = new File(engineBase(), appBase);
				}
				docBase = file.getPath();
			}
		}
		return docBase;
	}
	
	/**
	 * Get naming context full name.
	 * @return
	 */
	private String getNamingContextName(){
		if(namingContextName == null){
			Container parent = getParent();
			if(parent == null){
				namingContextName = getName();
			}else{
				Stack<String> stack = new Stack<>();
				StringBuffer sb = new StringBuffer();
				while(parent != null){
					stack.push(parent.getName());
					parent = parent.getParent();
				}
				while(!stack.isEmpty()){
					sb.append("/" + stack.pop());
				}
				sb.append(getName());
				namingContextName = sb.toString();
			}
		}
		return namingContextName;
	}
	
	private boolean getPaused(){
		return paused;
	}
	
	private void postResources(){
		getServletContext().setAttribute(Globals.RESOURCES_ATTR, getResources());
	}
	
	
	/**
	 * Post a copy of our current list of welcome files as a servlet context attribute,
	 * so that the default servlet can find them.
	 */
	private void postWelcomeFiles(){
		getServletContext().setAttribute("org.apache.catalina.WELCOME_FILES", welcomeFiles);
	}
	
	
	/**
	 * Set the appropriate context attribute for our work directory.
	 */
	private void postWorkDirectory(){
		
		//Acquire (or calculate) the work directory path
		String workDir = getWorkDir();
		if(workDir == null){
			
			//Retrieve our parent (normally a host) name
			String hostName = null;
			String engineName = null;
			String hostWorkDir = null;
			Container parentHost = getParent();
			if(parentHost != null){
				hostName = parentHost.getName();
				if(parentHost instanceof StandardHost){
					hostWorkDir = ((StandardHost) parentHost).getWorkDir();
				}
				Container parentEngine = parentHost.getParent();
				if(parentEngine != null){
					engineName = parentEngine.getName();
				}
			}
			if(hostName == null || hostName.length() < 1){
				hostName = "_";
			}
			if(engineName == null || engineName.length() < 1){
				engineName = "_";
			}
			
			String temp = getPath();
			if(temp.startsWith("/")){
				temp = temp.substring(1);
			}
			temp = temp.replace('/', '_');
			temp = temp.replace('\\', '_');
			if(temp.length() < 1){
				temp = "_";
			}
			if(hostWorkDir != null){
				workDir = hostWorkDir + File.separator + temp;
			}else{
				workDir = "work" + File.separator + engineName + File.separator + hostName + File.separator + temp;
			}
			setWorkDir(workDir);
		}
		
		// Create this directory is neccessary
		File dir = new File(workDir);
		if(!dir.isAbsolute()){
			File catalinaHome = new File(System.getProperty("catalina.base"));
			String catalinaHostPath = null;
			try{
				catalinaHostPath = catalinaHome.getCanonicalPath();
				dir = new File(catalinaHome, catalinaHostPath);
			}catch(IOException e){
				;
			}
		}
		dir.mkdirs();
		
		//Set the appropriate servlet context attribute 
		getServletContext().setAttribute(Globals.WORK_DIR_ATTR, dir);
		if(getServletContext() instanceof ApplicationContext){
			((ApplicationContext) getServletContext()).setAttributeReadOnly(Globals.WORK_DIR_ATTR);
		}
	}
	
	
	/**
	 * Return a File object representing the base directory for the entire servlet container
	 * (i.e. the Engine container if present)
	 * @return
	 */
	protected File engineBase() {
		
		return (new File(System.getProperty("catalina.base")));
	}
	
	/**
	 * Bind current thread, both for CL purposes and for JNDI ENC support during:
	 * startup, shutdown and reloading for the context
	 * 
	 * @return
	 */
	private ClassLoader bindThread(){
		
		ClassLoader oldContextThreadLoader = Thread.currentThread().getContextClassLoader();
		
		if(getResources() == null){
			return oldContextThreadLoader;
		}
		
		Thread.currentThread().setContextClassLoader(getLoader().getClassLoader());
		
		DirContextURLStreamHandler.bind(getResources());
		
		if(isUseNaming()){
			
			try {
				ContextBindings.bindThread(this, this);
			} catch (NamingException e) {
				// silent catch, as this is a normal case during case during the early startup stages
			}
		}
		return oldContextThreadLoader;
	}
	
	/**
	 * Unbind thread
	 * 
	 * @param oldContextClassLoader
	 */
	private void unbindThread(ClassLoader oldContextClassLoader){
		
		Thread.currentThread().setContextClassLoader(oldContextClassLoader);
		
		oldContextClassLoader = null;
		if(isUseNaming()){
			
			ContextBindings.unbindThread(this, this);
		}
		
		DirContextURLStreamHandler.unbind();
	}
	
	/**
	 * Are we processing a version 2.2 deployment descriptor?
	 * @return
	 */
	protected boolean isServlet22(){
		if(this.publicId == null){
			return false;
		}
		if(this.publicId.equals(org.apache.catalina.startup.Constants.WebDtdPublicId_22)){
			return true;
		}
		return false;
	}
	
	/**
	 * Adjust the URL pattern to begin with a leading slash, if appropriate (i.e. we are running 
	 * a serv,et 2.2 application). Otherwise, return the specified URL pattern unchanged
	 * @param urlPattern
	 * @return
	 */
	protected String adjustURLPattern(String urlPattern){
		if(urlPattern == null){
			return urlPattern;
		}
		if(urlPattern.startsWith("/") || urlPattern.startsWith("*.")){
			return urlPattern;
		}
		if(!isServlet22()){
			return urlPattern;
		}
		log(sm.getString("standardContext.urlPattern.patternWarning", urlPattern));
		return "/" + urlPattern;
	}
	
	private void setPaused(boolean paused) {
		this.paused = paused;
	}
	
	/**
	 */
	private boolean validateURLPattern(String urlPattern){
		if(urlPattern == null){
			return false;
		}
		if(urlPattern.startsWith("*.")){
			if(urlPattern.indexOf('/') < 0){
				return true;
			}else{
				return false;
			}
		}
		if(urlPattern.startsWith("/")){
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if(getParent() != null){
			sb.append(getParent().toString()).append(".");
		}
		sb.append("StandardContext[").append(getName()).append("]");
		return sb.toString();
	}
}
