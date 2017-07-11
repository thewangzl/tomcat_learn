package org.apache.catalina.core;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.catalina.Globals;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.StringManager;

public class ApplicationHttpRequest extends HttpServletRequestWrapper {

	
	/**
	 * The set of attribute names that are special for request dispatchers
	 */
	protected static final String[] specials = {
			Globals.REQUEST_URI_ATTR, Globals.CONTEXT_PATH_ATTR, Globals.SERVLET_PATH_ATTR,	//
			Globals.PATH_INFO_ATTR, Globals.QUERY_STRING_ATTR
	};
	
	protected HashMap<String,Object> attributes = new HashMap<>();
	
	protected String contextPath;
	
	@SuppressWarnings("rawtypes")
	protected Map parameters = new HashMap<>();
	
	protected String pathInfo;
	
	protected String queryString;
	
	protected String requestURI;
	
	protected String servletPath;
	
	protected static final StringManager sm = StringManager.getManager(Constants.Package);
	
	protected static final String info = "org.apache.catalina.core.ApplicationHttpRequest/1.0";
	
	
	
	public ApplicationHttpRequest(HttpServletRequest request) {
		super(request);
		setRequest(request);
	}
	
	// -------------------------------------------- ServletRequest Methods

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
	public void removeAttribute(String name) {
		synchronized (attributes) {
			attributes.remove(name);
			if(!isSpecial(name)){
				getRequest().removeAttribute(name);
			}
		}
	}
	
	@Override
	public void setAttribute(String name, Object value) {
		synchronized (attributes) {
			attributes.put(name, value);
			if(!isSpecial(name)){
				getRequest().setAttribute(name, value);
			}
		}
	}
	
	// --------------------------------------------  HttpServletRequest Methods
	
	@Override
	public String getContextPath() {
		return contextPath;
	}
	
	@Override
	public String getParameter(String name) {
		synchronized (parameters) {
			Object value = parameters.get(name);
			if(value == null){
				return null;
			}else if(value instanceof String[]){
				return ((String[]) value)[0];
			}else if(value instanceof String){
				return (String) value;
			}else{
				return value.toString();
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Map getParameterMap() {

		return parameters;
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Enumeration getParameterNames() {
		synchronized (parameters) {
			return new Enumerator<>(parameters.keySet());
		}
	}
	
	@Override
	public String[] getParameterValues(String name) {
		synchronized (attributes) {
			Object value = parameters.get(name);
			if(value == null){
				return (String[]) null;
			}else if(value instanceof String[]){
				return (String[]) value;
			}else if(value instanceof String){
				return new String[]{(String) value};
			}else{
				return new String[]{value.toString()};
			}
		}
	}
	
	@Override
	public String getPathInfo() {

		return pathInfo;
	}
	
	@Override
	public String getQueryString() {

		return this.queryString;
	}
	
	@Override
	public String getRequestURI() {
		return requestURI;
	}
	
	@Override
	public String getServletPath() {
		return servletPath;
	}
	
	// ------------------------------------------------ Package Methods
	
	
	public String getInfo() {
		return info;
	}
	
	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}
	
	public void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}
	
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}
	
	@SuppressWarnings("unchecked")
	public void setRequest(HttpServletRequest request) {
		super.setRequest(request);
		
		//Initialize the attribute for this request
		synchronized (attributes) {
			attributes.clear();
			Enumeration<String> names = request.getAttributeNames();
			while(names.hasMoreElements()){
				String name = names.nextElement();
				if(!(Globals.REQUEST_URI_ATTR.equals(name) || Globals.SERVLET_PATH_ATTR.equals(name))){
					Object value = request.getAttribute(name);
					attributes.put(name, value);
				}
			}
		}
		
		//Initialize the parameters for this request
		synchronized (parameters) {
			parameters = copyMap(request.getParameterMap());
		}
		
		// Initialize the path elements for this requet
		contextPath = request.getContextPath();
		pathInfo = request.getPathInfo();
		queryString = request.getQueryString();
		requestURI = request.getRequestURI();
		servletPath = request.getServletPath();
	}
	
	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}
	
	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}
	
	// ----------------------------------- Package Methods
	
	/**
	 * Perform a shallow copy of the specified Map, and return the result.
	 *  
	 * @param orig
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	Map copyMap(Map orig) {

		if(orig == null	){
			return new HashMap<>();
		}
		HashMap dest = new HashMap<>();
		synchronized (orig) {
			Iterator keys = orig.keySet().iterator();
			while(keys.hasNext()){
				String key = (String) keys.next();
				dest.put(key, orig.get(key));
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param queryString
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void mergeParameters(String queryString){
		if(queryString == null || queryString.length() == 0){
			return;
		}
		HashMap queryParameters = new HashMap<>();
		String encoding = getCharacterEncoding();
		if(encoding == null){
			encoding = "UTF-8";
		}
		try {
			RequestUtil.parseParameters(queryParameters, queryString, encoding);
		} catch (Exception e) {
			;
		}
		synchronized (parameters) {
			Iterator keys = parameters.keySet().iterator();
			while(keys.hasNext()){
				String key = (String) keys.next();
				Object value = queryParameters.get(key);
				if(value == null){
					queryParameters.put(key, parameters.get(key));
					continue;
				}
				queryParameters.put(key, mergeValues(value, parameters.get(key)));
			}
			parameters = queryParameters;
		}
	}
	
	/**
	 * Merge the two sets of parameter values into a single string array.
	 * 
	 * @param values1
	 * @param values2
	 * @return
	 */
	protected String[] mergeValues(Object values1, Object values2){
		ArrayList<String> results = new ArrayList<>();
		if(values1 == null){
			;
		}else if(values1 instanceof String){
			results.add((String)values1);
		}else if(values1 instanceof String[]){
			String[] values = (String[]) values1;
			for (int i = 0; i < values.length; i++) {
				results.add(values[i]);
			}
		}else{
			results.add(values1.toString());
		}
		if(values2 == null){
			;
		}else if(values2 instanceof String){
			results.add((String)values2);
		}else if(values2 instanceof String[]){
			String[] values = (String[]) values2;
			for (int i = 0; i < values.length; i++) {
				results.add(values[i]);
			}
		}else{
			results.add(values2.toString());
		}
		return results.toArray(new String[results.size()]);
	}

	/**
	 * Is this attribute name one of the special ones that is added for including servlets?
	 * 
	 * @param name
	 * @return
	 */
	protected boolean isSpecial(String name){
		for (int i = 0; i < specials.length; i++) {
			if(specials[i].equals(name)){
				return true;
			}
		}
		return false;
	}
	
}
