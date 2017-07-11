package org.apache.catalina;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Iterator;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;

/**
 * A <b>Request</b> is the Catalina-internal facade for a <code>ServletRequest</code> that is to be processed,
 * in order to produce the corresponding <code>Response</code>
 * @author lenovo
 *
 */
public interface Request {

	/**
	 * Return the authorization credentials sent with this request.
	 * @return
	 */
	public String getAuthorization();
	
	/**
	 * 
	 * @param authorization
	 */
	public void setAuthorization(String authorization);
	
	/**
	 * Return the Connector through which this Request was received
	 * @return
	 */
	public Connector getConnector();
	
	public void setConnector(Connector connector);
	
	/**
	 * Return the Context within which this Request is being processed.
	 * @return
	 */
	public Context getContext();
	
	public void setContext(Context context); 
	
	/**
	 * 
	 * @return
	 */
	public String getInfo();
	
	/**
	 * Return the <code>ServletRequest</code>  for which this object is the facade
	 * @return
	 */
	public ServletRequest getRequest();
	
	/**
	 * Return te Response with which this Request is associated. 
	 * @return
	 */
	public Response getResponse();
	
	public void setResponse(Response response);
	
	/**
	 * Set the socket (if any) through which this Request was received.
	 * @return
	 */
	public Socket getSocket();
	
	public void setSocket(Socket socket);
	
	/**
	 * Return the input stream associated with this Request.
	 * @return
	 */
	public InputStream getStream();
	
	public void setStream(InputStream stream);
	
	/**
	 * Return the Wrapper within which this Request is being processed.
	 * @return
	 */
	public Wrapper getWrapper();
	
	public void setWrapper(Wrapper wrapper);
	
	/**
	 * Create and return a ServletInputStream to read the content associated with this Request.
	 * 
	 * @return
	 * @throws IOException
	 */
	public ServletInputStream createInputStream() throws IOException;
	
	/**
	 * Perform whatever actions are required to flush and close the input stream or reader, in a single operation.
	 * 
	 * @throws IOException
	 */
	public void finishRequest() throws IOException;
	
	/**
	 * Return the object bound with the specified name to the internal notes for this request, or <code>null</code>
	 *  if no such binding exists.
	 * @param name
	 * @return
	 */
	public Object getNote(String name);
	
	public Iterator<String> getNoteNames();
	
	public void removeNote(String name);
	
	public void setNote(String name, Object note);
	
	/**
	 * Release all object references,  and initialize instance variables, in preparation for reuse of this object.
	 */
	public void recycle();
	
	/**
	 * Set the content length associated with this Request.
	 * 
	 * @param length
	 */
	public void setContentLength(int length);
	
	/**
	 * Set the content type (and optionally the character encoding) associated with this Request.
	 * For example, <code>text/html; charset=UTF-8</code>
	 * 
	 * @param type
	 */
	public void setContentType(String type);
	
	/**
	 * Set the protocol name and version associated with this Request.
	 * 
	 * @param protocol
	 */
	public void setProtocol(String protocol);
	
	/**
	 * Set the remote IP address associated with this request. NOTE: This value will be used to 
	 * resolve the value for <code>getRemoteHost()</code> if that method is called.
	 * @param remote
	 */
	public void setRemoteAddr(String remote);
	
	/**
	 * Set the name of the scheme associated with this request. Typical values are 
	 * <code>http</code>,<code>https</code>, <code>ftp</code>.
	 * 
	 * @param scheme
	 */
	public void setScheme(String scheme);
	
	/**
	 * Set the value to be returned by <code>isSecure()</code> for this request.
	 * @param secure
	 */
	public void setSecure(boolean secure);
	
	/**
	 * Set the name of the server (virtual host) to process this request.
	 * 
	 * @param name
	 */
	public void setServerName(String name);
	
	/**
	 * Set the port number of the server to process this request.
	 * @param port
	 */
	public void setServerPort(int port);
	
	
}

