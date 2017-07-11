package org.apache.catalina.core;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.naming.Binding;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Logger;
import org.apache.catalina.Wrapper;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.ResourceSet;
import org.apache.catalina.util.ServerInfo;
import org.apache.catalina.util.StringManager;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.Resource;

/**
 * Standard implementation of <code>ServletContext</code> that represents  a web application's execution environment.
 * An instance of this class is associated with each instance of <code>StandardContext</code>.
 * 
 * @author thewangzl
 *
 */
public class ApplicationContext implements ServletContext {

	
	/**
	 * The context atributes for this context.
	 */
	private HashMap<String,Object> attributes = new HashMap<>();
	
	/**
	 * List of read only attributes for this context.
	 */
	private HashMap<String, String> readOnlyAttributes = new HashMap<>();
	
	
	/**
	 * The Context instance with which we are associated.
	 */
	private StandardContext context ;
	
	private static final ArrayList<Object> empty = new ArrayList<>();
	
	/**
	 * The facade arround this object.
	 */
	private ServletContext facade = new ApplicationContextFacade(this);
	
	/**
	 * The merged context initialization parameters for this Context.
	 */
	private HashMap<String, String> parameters;
	
	private static final StringManager sm = StringManager.getManager(Constants.Package);
	
	/**
	 * Base path.
	 */
	private String basePath;
	
	
	public ApplicationContext( String basePath, StandardContext context) {
		super();
		this.context = context;
		this.basePath = basePath;
	}


	// -------------------------------- Public Methods
	
	/**
	 * Clear all application-created attributes
	 */
	public void clearAttributes() {
		// Create list of attributes to be removed
		ArrayList<String> list = new ArrayList<>();
		synchronized (attributes) {
			Iterator<String> iter = attributes.keySet().iterator();
			while(iter.hasNext()){
				list.add(iter.next());
			}
		}
		
		// Remove application orginated attributes 
		//(read only attributes will be left in place)
		Iterator<String> keys = list.iterator();
		while(keys.hasNext()){
			String key = keys.next();
			this.removeAttribute(key);
		}
	}
	
	/**
	 * Return the resources object that is mapped to a specified path.
	 * The path must begin with a "/" and is interpreted as relative to 
	 * the current context path.
	 * @return
	 */
	public DirContext getResources(){
		return context.getResources();
	}

	/**
	 * Set an attribute as read only
	 * @param workDirAttr
	 */
	public void setAttributeReadOnly(String name) {
		synchronized (attributes) {
			if(attributes.containsKey(name)){
				readOnlyAttributes.put(name, name);
			}
		}
	}
	
	
	// ---------------------------- ServletContext Methods
	
	@Override
	public Object getAttribute(String name) {
		synchronized (attributes) {
			return attributes.get(name);
		}
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		synchronized (attributes) {
			return new Enumerator<>(attributes.keySet());
		}
	}

	@Override
	public ServletContext getContext(String uri) {

		if(uri == null || !uri.startsWith("/")){
			return null;
		}
		
		//Return the current context if request
		String contextPath = context.getPath();
		if(!contextPath.endsWith("/")){
			contextPath = contextPath + "/";
		}
		if(contextPath.length() >0 && uri.startsWith(contextPath)){
			return this;
		}
		
		//Return other contexts only if allowed
		if(!context.getCrossContext()){
			return null;
		}
		try {
			if(context.getParent() != null){
				Host host = (Host) context.getParent();
				Context child = host.map(uri);
				if(child != null){
					return child.getServletContext();
				}
			}
			return null;
		} catch (Throwable e) {
			return null;
		}
	}

	@Override
	public String getInitParameter(String name) {
		mergeParameters();
		synchronized (parameters) {
			return parameters.get(name);
		}
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		mergeParameters();
		synchronized (parameters) {
			return new Enumerator<>(parameters.keySet());
		}
	}

	/**
	 * Return the major version of the Java Servlet API that we implement.
	 * 
	 * @return
	 */
	@Override
	public int getMajorVersion() {
		
		return Constants.MAJOR_VERSION;
	}

	/**
	 * Return the MIME type of the specified file, or <code>null</code> if the MIME type cannot be determined.
	 * 
	 * @param file
	 * @return
	 */
	@Override
	public String getMimeType(String file) {
		if(file == null){
			return null;
		}
		int period = file.lastIndexOf('.');
		if(period < 0){
			return null;
		}
		String extension = file.substring(period + 1);
		if(extension.length()< 1){
			return null;
		}

		return context.findMimeMapping(extension);
	}

	@Override
	public int getMinorVersion() {
		return Constants.MINOR_VERSION;
	}

	/**
	 * Return a <code>RequestDisatcher</code> object that acts as a Wrapper for the named servlets
	 * 
	 * @param name
	 * @return
	 */
	@Override
	public RequestDispatcher getNamedDispatcher(String name) {

		if(name == null){
			return null;
		}
		
		//Create and return a corresponding request dispatcher
		Wrapper wrapper = (Wrapper) context.findChild(name);
		if(wrapper == null){
			return null;
		}
		ApplicationDispatcher dispatcher = new ApplicationDispatcher(wrapper, null, null, null, name);
		
		return dispatcher;
	}

	/**
	 * Return the real path for a given virtual path, if possible;
	 * otherwise, return <code> null</code>.
	 */
	@Override
	public String getRealPath(String path) {

		if(!context.isFilesystemBased()){
			return null;
		}
		
		File file = new File(basePath, path);
		return file.getAbsolutePath();
	}

	/**
	 * Return a <code>RequestDispatcher</code> instance that acts as a wrapper for the resource at the 
	 * given path. The path must begin with a "/" and is interpreted as relative to the current context root.
	 */
	@Override
	public RequestDispatcher getRequestDispatcher(String path) {

		//Validate the path argument
		if(path == null){
			return null;
		}
		
		if(!path.startsWith("/")){
			throw new IllegalArgumentException(sm.getString("applicationContext.requestDispatcher.iae", path));
		}
		if(normalize(path) == null){
			return null;
		}
		
		// Construct a "fake" request to be mapped by our Context
		String contextPath = context.getPath();
		if(contextPath == null){
			contextPath = "";
		}
		String relativeURI = path;
		String queryString = null;
		int question = path.indexOf('?');
		if(question >= 0){
			relativeURI = path.substring(0, question);
			queryString = path.substring(question + 1);
		}
		if(System.getSecurityManager() != null){
			PrivilegedGetRequestDespatcher dp = new PrivilegedGetRequestDespatcher(contextPath, relativeURI, queryString);
			return AccessController.doPrivileged(dp);
		}
		
		//The remaining code is duplicated in PrivilegedGetRequestDispatcher,
		//we need to make sure they stay in sync.
		HttpRequest request = new MappingRequest(context.getPath(), contextPath + relativeURI, queryString);
		Wrapper wrapper = (Wrapper) context.map(request, true);
		if(wrapper == null){
			return null;
		}
		
		//Construct a RequestDispatcher to process this request
		HttpServletRequest hrequest = (HttpServletRequest)request.getRequest();
		return new ApplicationDispatcher(wrapper, hrequest.getServletPath(), hrequest.getPathInfo(), hrequest.getQueryString(), null);
	}


	/**
	 * Return the URL to the resource that is mapped to a specified path. The path must 
	 * begin with a "/" and is interpreted as a relative to the current context root.
	 * 
	 * @param path
	 * @return
	 * @throws MalformedURLException
	 */
	@Override
	public URL getResource(String path) throws MalformedURLException {
		
		DirContext resources = context.getResources();
		if(resources != null){
			String fullPath = context.getName() + path;
			
			//This is the problem. Host must not be null
			String hostName = context.getParent().getName();
			
			try {
				resources.lookup(path);
				if(System.getSecurityManager() != null){
					try{
						PrivilegedGetResource dp = new PrivilegedGetResource(hostName, fullPath, resources);
						return AccessController.doPrivileged(dp);
					}catch(PrivilegedActionException pae){
						throw pae.getException();
					}
				}else{
					return new URL("jndi", null, 0, getJNDIUri(hostName, fullPath), new DirContextURLStreamHandler(resources));
				}
			} catch (Exception e) {
				;
			}
		}
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		DirContext resources = context.getResources();
		if(resources != null){
			try {
				Object resource = resources.lookup(path);
				return ((Resource) resource).streamContent();
			} catch (Exception e) {
				
			}
		}
		return null;
	}

	/**
	 * Return a Set containing the resource paths of resources member of the specified collection.
	 * Each path will be a String starting with a "/" character. The returned set is immutable.
	 * 
	 * @param path
	 */
	@Override
	public Set<String> getResourcePaths(String path) {
		DirContext resources = context.getResources();
		if(resources != null){
			if(System.getSecurityManager() != null){
				PrivilegedAction<Set<String>> dp = new PrivilegedGetResourcePaths(resources, path);
				return AccessController.doPrivileged(dp);	
			}else{
				getResourcePathsInternal(resources, path);
			}
		}

		return null;
	}
	
	private Set<String> getResourcePathsInternal(DirContext resources, String path){
		ResourceSet<String> set = new ResourceSet<>();
		try {
			listCollectionPaths(set, resources, path);
		} catch (NamingException e) {
			return null;
		}
		set.setLocked(true);
		return set;
	}

	@Override
	public String getServerInfo() {
		return ServerInfo.getServerInfo();
	}

	@Deprecated //As of Java Servlet API 2.1, with no direct replacement
	@Override
	public Servlet getServlet(String name) throws ServletException {

		return null;
	}

	/**
	 * Return the display name of this web application
	 */
	@Override
	public String getServletContextName() {
		return context.getDisplayName();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getServletNames() {
		return new Enumerator<>(empty);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getServlets() {
		return new Enumerator<>(empty);
	}

	@Override
	public void log(String message) {
		if(System.getSecurityManager() != null){
			PrivilegedAction<Object> dp = new PrivilegedLogMessage(message);
			AccessController.doPrivileged(dp);
		}else{
			internalLog(message);
		}
	}
	
	private void internalLog(String message){
		Logger logger = context.getLogger();
		if(logger != null){
			logger.log(message);
		}
	}

	@Override
	public void log(Exception exception, String message) {
		if(System.getSecurityManager() != null){
			PrivilegedLogException dp = new PrivilegedLogException(exception, message);
			AccessController.doPrivileged(dp);
		}else{
			internalLog(exception, message);
		}
	}
	
	private void internalLog(Exception exception, String message){
		Logger logger = context.getLogger();
		if(logger != null){
			logger.log(exception, message);
		}
	}

	@Override
	public void log(String message, Throwable throwable) {
		if(System.getSecurityManager() != null){
			PrivilegedLogThrowable dp = new PrivilegedLogThrowable(message, throwable);
			AccessController.doPrivileged(dp);
		}else{
			internalLog(message, throwable);
		}
	}
	
	private void internalLog( String message, Throwable throwable){
		Logger logger = context.getLogger();
		if(logger != null){
			logger.log(message, throwable);
		}
	}

	@Override
	public void removeAttribute(String name) {
		Object value = null;
		boolean found = false;
		
		//Remove the specified attribute
		synchronized (attributes) {
			//Check for read only attribute
			if(readOnlyAttributes.containsKey(name)){
				return;
			}
			found = attributes.containsKey(name);
			if(found){
				value = attributes.get(name);
			}else{
				return;
			}
		}
		
		//Notify interested application event listeners
		Object[] listeners = context.getApplicationListeners();
		if(listeners == null || listeners.length == 0){
			return;
		}
		ServletContextAttributeEvent event = new ServletContextAttributeEvent(context.getServletContext(), name, value);
		for (Object object : listeners) {
			if(!(object instanceof ServletContextAttributeListener)){
				continue;
			}
			ServletContextAttributeListener listener = (ServletContextAttributeListener) object;
			
			try{
				context.fireContainerEvent("beforeContextAttributeRemoved", listener);
				listener.attributeRemoved(event);
				context.fireContainerEvent("afterContextAttributeRemoved", listener);
			}catch(Throwable t){
				context.fireContainerEvent("afterContextAttributeRemoved", listener);
				//FIXME - should we do anything besides log these?
				log(sm.getString("applicationContext.attributeEvent"), t);
			}
		}
	}

	@Override
	public void setAttribute(String name, Object value) {

		if(name == null){
			throw new IllegalArgumentException(sm.getString("applicationContext.setAttribute.namenull"));
		}
		
		// null value is the same as removeAttribute()
		if(value == null){
			removeAttribute(name);
			return;
		}
		
		Object oldValue = null;
		boolean replaced = false;

		// Add or replace the specified attribute
		synchronized (attributes) {
			//Check for read only attribute
			if(readOnlyAttributes.containsKey(name)){
				return;
			}
			oldValue = attributes.get(name);
			if(oldValue != null){
				replaced = true;
			}
			attributes.put(name,value);
		}
		
		// Notify interested application event listeners
		Object[] listeners = context.getApplicationListeners();
		if(listeners == null || listeners.length == 0){
			return;
		}
		ServletContextAttributeEvent event;
		if(replaced){
			event = new ServletContextAttributeEvent(context.getServletContext(), name, oldValue);
		}else{
			event = new ServletContextAttributeEvent(context.getServletContext(), name, value);
		}
		for (Object object : listeners) {
			if(!(object instanceof ServletContextAttributeListener)){
				return;
			}
			ServletContextAttributeListener listener = (ServletContextAttributeListener) object;
			try {
				if(replaced){
					context.fireContainerEvent("beforeContextAttributeReplaced", listener);
					listener.attributeReplaced(event);
					context.fireContainerEvent("afterContextAttributeReplaced", listener);
				}else{
					context.fireContainerEvent("beforeContextAttributeRemoved", listener);
					listener.attributeRemoved(event);
					context.fireContainerEvent("afterContextAttributeRemoved", listener);
				}
			} catch (Throwable e) {
				if(replaced){
					context.fireContainerEvent("afterContextAttributeReplaced", listener);
				}else{
					context.fireContainerEvent("afterContextAttributeRemoved", listener);	
				}
				//FIXME - should we do anything besides log these?
				log(sm.getString("applicationContext.attributeEvent"), e);
			}
		}
	}
	
	/**
	 * Return the facade associated with this ApplicationContext.
	 * @return
	 */
	ServletContext getFacade() {
		return facade;
	}
	
	/**
	 * Merge the context initialization parameters specified in the application deployment
	 * descriptor with the application parameters described in the server configuration,
	 * respecting the <code>override</code> property of the appliation parameters appropriately.
	 */
	private void mergeParameters(){
		if(parameters != null){
			return;
		}
		HashMap<String, String> results = new HashMap<>();
		String[] names = context.findParameters();
		for (int i = 0; i < names.length; i++) {
			results.put(names[i], context.findParameter(names[i]));
		}
		ApplicationParameter[] params = context.findApplicationParameters();
		for (int i = 0; i < params.length; i++) {
			if(params[i].getOverride()){
				if(results.get(params[i].getName()) == null){
					results.put(params[i].getName(), params[i].getValue());
				}
			}else{
				results.put(params[i].getName(), params[i].getValue());
			}
		}
		parameters = results;
	}
	
	private Object normalize(String path) {
		String normalized = path;

		// Normalize the slashes and add leading slash if necessary
		if (normalized.indexOf('\\') >= 0)
		    normalized = normalized.replace('\\', '/');

		// Resolve occurrences of "/../" in the normalized path
		while (true) {
		    int index = normalized.indexOf("/../");
		    if (index < 0)
			break;
		    if (index == 0)
			return (null);	// Trying to go outside our context
		    int index2 = normalized.lastIndexOf('/', index - 1);
		    normalized = normalized.substring(0, index2) +
			normalized.substring(index + 3);
		}

		// Return the normalized path that we have completed
		return (normalized);
	}
	
	/**
	 * List resource paths (recursively), and store all of them in the given Set.
	 * 
	 * @param set
	 * @param resources
	 * @param path
	 * 
	 * @throws NamingException
	 */
	private static void listCollectionPaths(Set<String> set, DirContext resources, String path) throws NamingException{
		
		Enumeration<Binding> childPaths = resources.listBindings(path);
		while(childPaths.hasMoreElements()){
			Binding binding = childPaths.nextElement();
			String name = binding.getName();
			StringBuffer childPath = new StringBuffer(path);
			if(!"/".equals(path) && !path.endsWith("/")){
				childPath.append("/");
			}
			childPath.append(name);
			Object object = binding.getObject();
			if(object instanceof DirContext){
				childPath.append("/");
			}
			set.add(childPath.toString());
		}
	}
	
	/**
	 * Get full path, based on the host name and the context path.
	 * 
	 * @param hostName
	 * @param path
	 * @return
	 */
	public static String getJNDIUri(String hostName, String path){
		if(!path.startsWith("/")){
			return "/" + hostName + "/" + path;
		}
		return "/" + hostName + path;
	}

	// --------------------------------------------------- Inner class 
	
	protected class PrivilegedGetRequestDespatcher implements PrivilegedAction<ApplicationDispatcher>{

		private String contextPath;
		
		private String relativeURI;
		
		private String queryString;
		
		public PrivilegedGetRequestDespatcher(String contextPath, String relativeURI, String queryString) {
			this.contextPath = contextPath;
			this.relativeURI = relativeURI;
			this.queryString = queryString;
		}
		
		@Override
		public ApplicationDispatcher run() {
			
			HttpRequest request =  new MappingRequest(context.getPath(), contextPath + relativeURI, queryString);
			
			Wrapper wrapper = (Wrapper) context.map(request, true);
			if(wrapper == null){
				return null;
			}
			
			HttpServletRequest hrequest = (HttpServletRequest) request.getRequest();
			return new ApplicationDispatcher(wrapper, hrequest.getServletPath(), hrequest.getPathInfo(), hrequest.getQueryString(), null);
		}
	}
	
	protected class PrivilegedGetResource implements PrivilegedExceptionAction<URL>{

		private String host;
		
		private String path;
		
		private DirContext resources;
		
		public PrivilegedGetResource(String host, String path, DirContext resources) {
			this.host = host;
			this.path = path;
			this.resources = resources;
		}

		@Override
		public URL run() throws Exception {

			return new URL("jndi", null, 0, getJNDIUri(host, path), new DirContextURLStreamHandler(resources));
		}
	}
	
	protected class PrivilegedGetResourcePaths implements PrivilegedAction<Set<String>>{

		private DirContext resources;
		private String path;
		
		public PrivilegedGetResourcePaths(DirContext resources, String path) {
			this.resources = resources;
			this.path = path;
		}

		@Override
		public Set<String> run() {
			return getResourcePathsInternal(resources, path);
		}
	}
	
	protected class PrivilegedLogMessage implements PrivilegedAction<Object>{

		private String message;
		
		public PrivilegedLogMessage(String message) {
			this.message = message;
		}

		@Override
		public Object run() {
			internalLog(message);
			return null;
		}
	}
	
	protected class PrivilegedLogException implements PrivilegedAction<Object>{

		private String message;
		private Exception exception;
		
		public PrivilegedLogException(Exception exception, String message) {
			this.exception = exception;
			this.message = message;
		}

		@Override
		public Object run() {
			internalLog(exception, message);
			return null;
		}
	}
	
	protected class PrivilegedLogThrowable implements PrivilegedAction<Object>{

		private String message;
		private Throwable throwable;
		
		public PrivilegedLogThrowable(String message, java.lang.Throwable throwable) {
			super();
			this.message = message;
			this.throwable = throwable;
		}

		@Override
		public Object run() {
			log(message, throwable);
			return null;
		}
		
	}
	
}
