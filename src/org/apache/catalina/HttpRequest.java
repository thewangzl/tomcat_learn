package org.apache.catalina;

import java.security.Principal;
import java.util.Locale;

import javax.servlet.http.Cookie;

/**
 * An <b>HttpRequest</b> is the Catalina-internal facade for an <code>HttpServletRequest</code> 
 * that is to be processed, in order to product the corresponding <code>HttpResponse</code>
 * 
 * @author lenovo
 *
 */
public interface HttpRequest extends Request {

	
	public void addCookie(Cookie cookie);
	
	public void addHeader(String name, String value);
	
	public void addLocale(Locale locale);
	
	public void addParameter(String name, String[] values);
	
	public void clearCookies();
	
	public void clearHeaders();
	
	public void clearLocales();
	
	public void clearParameters();
	
	/**
	 * Set the authentication type used for this request, if any; otherwise set the type to
	 * <code>null</code>. Typical values are "BASIC", "DIGEST", or "SSL".
	 * 
	 * @param type
	 */
	public void setAuthType(String type);
	
	/**
	 * Set the context path for this Request. This will normally be called when 
	 * the associated Context is mapping the Request to a particular Wrapper.
	 * 
	 * @param path
	 */
	public void setContextPath(String path);
	
	/**
	 * Set the HTTP request method used for this Request.
	 * 
	 * @param method
	 */
	public void setMethod(String method);
	
	/**
	 * Set the query string for this Request. This normally be called by the HTTP Connector, 
	 * when it parses the request headers.
	 * 
	 * @param queryString
	 */
	public void setQueryString(String query);
	
	/**
	 * Set the path information for this Request. This will normally be called when the 
	 * associated Context is mapping the Requet to a particular Wrapper.
	 * 
	 * @param path
	 */
	public void setPathInfo(String path);
	
	/**
	 * Set a flag indicating whether or not the requested session ID for this request
	 * came in through a cookie. This is normally called by the HTTP Connector, 
	 * when it parses the request headers.
	 * 
	 * @param flag
	 */
	public void setRequestedSessionCookie(boolean flag);
	
	/**
	 * Set the requested session ID for this request. This is normally called by the 
	 * HTTP Connector, when it parses the request headers.
	 * 
	 * @param id
	 */
	public void setRequestedSessionId(String id);
	
	/**
	 * Set a flag indicating whether or not the requested session ID for this request
	 * came in through a URL. This is normally called by the HTTP Connector, 
	 * when it parses the request headers.
	 * 
	 * @param flag
	 */
	public void setRequestedSessionURL(boolean flag);
	
	/**
	 * Set the unparsed request URI for this request. This will normally be called by
	 * the HTTP Connector, when it parses the request headers.
	 * 
	 * @param uri
	 */
	public void setRequestURI(String uri);
	
	/**
	 * Set the decoded request URI
	 * 
	 * @param uri
	 */
	public void setDecodedRequestURI(String uri);
	
	public String getDecodedRequestURI();
	
	/**
	 * Set the servlet path for this request. This will normally be called when 
	 * the associated Context is mapping the Request to a particular Wrapper.
	 * 
	 * @param path
	 */
	public void setServletPath(String path);
	
	/**
	 * Set the principal who has been authenticated for this Request. This value is also 
	 * used to calculate the value to be returned by the <code>getRemoteUser()</code> method.
	 * @param principal
	 */
	public void setUserPrincipal(Principal principal);
	
}
