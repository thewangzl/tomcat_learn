package org.apache.catalina.connector;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Globals;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.ParameterMap;
import org.apache.catalina.util.RequestUtil;

/**
 * 
 * @author thewangzl
 *
 */
public class HttpRequestBase extends RequestBase implements HttpRequest, HttpServletRequest {

	/**
	 * The authentication type used for this request
	 */
	protected String authType;

	/**
	 * The context path for this request 
	 */
	protected String contextPath = "";

	/**
	 * 
	 */
	protected ArrayList<Cookie> cookies = new ArrayList<>();

	protected static ArrayList<String> empty = new ArrayList<>();

	protected SimpleDateFormat[] formats = { new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
			new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
			new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US) };

	/**
	 * The facade associated with this requet
	 */
	protected HttpRequestFacade facade = new HttpRequestFacade(this);

	
	protected HashMap<String,ArrayList<String>> headers = new HashMap<>();

	protected static final String info = "org.apache.catalina.connector.HttpRequestBase/1.0";

	protected String method;

	protected ParameterMap<String, String[]> parameters;

	/**
	 * Have the parameters for this request been parsed yet?
	 */
	protected boolean parsed;

	protected String pathInfo;

	protected String queryString;

	protected boolean requestedSessionCookie;

	protected String requestedSessionId;

	protected boolean requestedSessionURL;

	protected String requestURI;

	protected String decodedRequestURI;

	protected boolean secure;

	protected String servletPath;

	/**
	 * The currently active session for this request
	 */
	protected Session session;

	protected Principal userPrincipal;
	
	@Override
	public ServletRequest getRequest() {
		return facade;
	}

	private HttpSession doGetSession(boolean create) {
		// There cannot be a session if no context has been assigned yet
		if (context == null) {
			return null;
		}
		// Return the current session if it exists and is valid
		if (session != null && !session.isValid()) {
			return null;
		}
		if (session != null) {
			return session.getSession();
		}

		// Return the requested session if it exists and its valid
		// TODO
		return session.getSession();
	}

	@Override
	public String getAuthType() {
		return this.authType;
	}

	@Override
	public String getContextPath() {
		return this.contextPath;
	}

	@Override
	public Cookie[] getCookies() {
		return this.cookies.toArray(new Cookie[0]);
	}

	@Override
	public long getDateHeader(String name) {
		String value = this.getHeader(name);
		if(value == null){
			return -1;
		}
		value += " ";
		for (int i = 0; i < formats.length; i++) {
			try {
				Date date = formats[i].parse(value);
				return date.getTime();
			} catch (ParseException e) {
				;
			}
		}
		throw new IllegalArgumentException(value);
	}

	@Override
	public String getHeader(String name) {
		name = name.toLowerCase();
		synchronized (headers) {
			ArrayList<String> values =  headers.get(name);
			if(values != null){
				return  values.get(0);
			}
			return null;
		}
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		synchronized (headers) {
			return new Enumerator<>(headers.keySet());
		}
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		name = name.toLowerCase();
		synchronized (headers) {
			ArrayList<String> values =  headers.get(name);
			if(values != null){
				return new Enumerator<String>(values);
			}else {
				return new Enumerator<String>(empty);
			}
		}
	}

	@Override
	public int getIntHeader(String name) {
		String value = this.getHeader(name);
		if(value == null){
			return -1;
		}
		return Integer.parseInt(value);
	}

	@Override
	public String getMethod() {
		return this.method;
	}

	@Override
	public String getPathInfo() {
		return this.pathInfo;
	}

	@Override
	public String getPathTranslated() {
		if(context == null){
			return null;
		}
		if(pathInfo == null){
			return null;
		}
		return context.getServletContext().getRealPath(pathInfo);
	}

	@Override
	public String getQueryString() {
		return this.queryString;
	}

	@Override
	public String getRemoteUser() {
		if(userPrincipal != null){
			return userPrincipal.getName();
		}
		return null;
	}

	@Override
	public String getRequestURI() {
		return this.requestURI;
	}

	/**
	 * Reconstructs the URL the client used to make the request. The returned URL contains 
	 * a prorocol, server name,port number, and server path, but it does not include query 
	 * string parameters.
	 * 
	 * @return
	 */
	@Override
	public StringBuffer getRequestURL() {

		StringBuffer url = new StringBuffer();
		String scheme = this.getScheme();
		int port = this.getServerPort();
		if(port < 0){
			port = 80;	// Work around java.net.URL bug
		}
		url.append(scheme);
		url.append("://");
		url.append(getServerName());
		if((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443) ){
			url.append(":");
			url.append(port);
		}
		url.append(getRequestURI());
		
		return url;
	}

	@Override
	public String getRequestedSessionId() {
		return this.requestedSessionId;
	}

	@Override
	public String getServletPath() {
		return this.servletPath;
	}

	@Override
	public HttpSession getSession() {
		return getSession(true);
	}

	@Override
	public HttpSession getSession(boolean create) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		return this.userPrincipal;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return this.requestedSessionCookie;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		if(requestedSessionId != null){
			return requestedSessionURL;
		}
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		
		return isRequestedSessionIdFromURL();
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		if(this.requestedSessionId == null){
			return false;
		}
		if(context == null){
			return false;
		}
		Manager manager = context.getManager();
		if(manager == null){
			return false;
		}
		// TODO
		return false;
	}

	@Override
	public boolean isUserInRole(String role) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addCookie(Cookie cookie) {
		synchronized (cookies) {
			cookies.add(cookie);
		}
	}

	/**
	 * Add a Header to the set of Headers associated with this Request.
	 * 
	 * @param name
	 * @param value
	 */
	@Override
	public void addHeader(String name, String value) {
		name = name.toLowerCase();
		synchronized (headers) {
			ArrayList<String> values = headers.get(name);
			if(values == null){
				values = new ArrayList<>();
				headers.put(name, values);
			}
			values.add(value);
		}

	}

	
	@Override
	public void addParameter(String name, String[] values) {
		synchronized (parameters) {
			parameters.put(name, values);
		}
	}

	@Override
	public void clearCookies() {
		synchronized (cookies) {
			cookies.clear();
		}

	}

	@Override
	public void clearHeaders() {

		headers.clear();
	}

	@Override
	public void clearLocales() {

		locales.clear();
	}

	@Override
	public void clearParameters() {
		if(parameters != null){
			parameters.setLocked(false);
			parameters.clear();
		}else{
			parameters = new ParameterMap<>();
		}

	}

	@Override
	public void setAuthType(String type) {
		this.authType = type;
	}

	@Override
	public void setContextPath(String path) {
		this.contextPath = path;
	}

	@Override
	public void setMethod(String method) {
		this.method = method;
	}

	@Override
	public void setQueryString(String query) {
		this.queryString = query;
	}

	@Override
	public void setPathInfo(String path) {
		this.pathInfo = path;
	}

	@Override
	public void setRequestedSessionCookie(boolean flag) {
		this.requestedSessionCookie = flag;
	}

	@Override
	public void setRequestedSessionId(String id) {
		this.requestedSessionId = id;
	}

	@Override
	public void setRequestedSessionURL(boolean flag) {
		this.requestedSessionURL = flag;
	}

	@Override
	public void setRequestURI(String uri) {
		this.requestURI = uri;
	}

	@Override
	public void setDecodedRequestURI(String uri) {
		this.decodedRequestURI = uri;
	}

	@Override
	public String getDecodedRequestURI() {
		return this.decodedRequestURI;
	}

	@Override
	public void setServletPath(String path) {
		this.servletPath = path;
	}

	@Override
	public void setUserPrincipal(Principal principal) {
		this.userPrincipal = principal;
	}
	
	/**
	 * Parse the parameters of this request, if it has not already occured. If parameters are 
	 * present in both the query string and the request
	 * 
	 */
	protected void parseParameters(){
		if(parsed){
			return;
		}
		ParameterMap<String, String[]> results = parameters;
		if(results == null){
			results = new ParameterMap<>();
		}
		results.setLocked(false);
		
		String encoding = this.getCharacterEncoding();
		if(encoding == null){
			encoding = "UTF-8";
		}
		
		// Parse any parameters specified in the query string
		String queryString = this.getQueryString();
		try {
			RequestUtil.parseParameters(results, queryString, encoding);
		} catch (UnsupportedEncodingException e) {
			;
		}
		
		//Parse any parameters specified in the input stream 
		String contentType = this.getContentType();
		if(contentType == null){
			contentType = "";
		}
		int semicolon = contentType.indexOf(';');
		if(semicolon >= 0){
			contentType = contentType.substring(0, semicolon).trim();
		}else{
			contentType = contentType.trim();
		}
		if("POST".equals(this.getMethod()) && this.getContentLength() > 0 && this.stream == null && "application/x-www-form-urlencoded".equals(contentType)){
			try {
				int max = this.getContentLength();
				int len = 0;
				byte[] buf = new byte[max];
				ServletInputStream is = getInputStream();
				while(len < max){
					int next = is.read(buf, len, max - len);
					if(next < 0){
						break;
					}
					len += next;
				}
				is.close();
				if(len < max){
					//
					throw new RuntimeException("httpRequestBase.contentLengthMistach");
				}
				RequestUtil.parseParameters(results, buf, encoding);;
			} catch (IOException e) {
				throw new RuntimeException("httpRequestBase.contentReadFail");
			}
		}
		results.setLocked(true);
		parsed = true;
		parameters = results;
	}

	@Override
	public String getParameter(String name) {
		parseParameters();
		String[] values = (String[]) parameters.get(name);
		if(values != null){
			return values[0];
		}
		return null;
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		parseParameters();
		return this.parameters;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		parseParameters();
		return new Enumerator<>(this.parameters.keySet());
	}

	@Override
	public String[] getParameterValues(String name) {
		parseParameters();
		String[] values =  parameters.get(name);
		if(values != null){
			return values;
		}
		return null;
	}

	/**
	 * Return a RequestDispatcher that wraps the resource at the specified path,
	 * which may be interpreted as relative to the current request path;
	 * 
	 * @param path
	 * @return
	 */
	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		if(context == null){
			return null;
		}
		//If the path is already context-relative,just pass it through
		if(path == null){
			return null;
		}else if(path.startsWith("/")){
			return context.getServletContext().getRequestDispatcher(path);
		}
		
		//Convert a request-relative path to a context-relative one
		String servletPath = (String) this.getAttribute(Globals.SERVLET_PATH_ATTR);
		if(servletPath == null){
			servletPath = this.getServletPath();
		}
		int pos = servletPath.lastIndexOf("/");
		String relative = null;
		if(pos > 0){
			relative = RequestUtil.normalize(servletPath.substring(0, pos + 1) + path);
		}else{
			relative = RequestUtil.normalize(servletPath + path);
		}
		return context.getServletContext().getRequestDispatcher(relative);
	}

	protected class PrivilegedGetSession implements PrivilegedAction<HttpSession> {

		private boolean create;

		public PrivilegedGetSession(boolean create) {
			this.create = create;
		}

		@Override
		public HttpSession run() {
			return doGetSession(create);
		}
	}
}
