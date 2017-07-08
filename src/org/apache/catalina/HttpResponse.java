package org.apache.catalina;

import javax.servlet.http.Cookie;

/**
 * An <b>HttpResponse</b> is te Catalina-internal facade for an <code>HttpServletResponse</code>
 * that is to be produced, based on the processing of a corresponding <code>HttRequest</code>
 * @author thewangzl
 *
 */
public interface HttpResponse extends Response {

	public Cookie[] getCookies();
	
	public String getHeader(String name);
	
	public String[] getHeaderNames();
	
	public String[] getHeaderValues(String name);
	
	/**
	 * Return the error message that was set with <code>sendError()</code> for response.
	 * 
	 * @return
	 */
	public String getMessage();
	
	/**
	 * Return the HTTP status code associated with this Response.
	 * 
	 * @return
	 */
	public int getStatus();
	
	/**
	 * Reset this response, and specify the values for the HTTP statuscode and corresponding message.
	 * @param status
	 * @param messge
	 */
	public void reset(int status, String messge);
	
	
	
}
