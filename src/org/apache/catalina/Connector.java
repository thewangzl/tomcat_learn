package org.apache.catalina;

import org.apache.catalina.net.ServerSocketFactory;

public interface Connector {

	/**
	 * Return the Container used for processing requests received by this Container
	 * @return
	 */
	public Container getContainer();
	
	
	public void setContainer(Container container);
	
	/**
	 * Return the "enable DNS lookups" flag
	 * @return
	 */
	public boolean geEnableLookups();
	
	public void setEnableLookups(boolean enableLookups);
	
	/**
	 * Return the server socket factory used by this Container
	 * @return
	 */
	public ServerSocketFactory getFactory();
	
	
	public void setFactory(ServerSocketFactory factory);
	
	/**
	 * Return desciptive information about this Connector implementation
	 * @return
	 */
	public String getInfo();
	
	/**
	 * Return the port number to which a request should be redirected if
	 * it comes in on a non-SSL port ad s subject to a security constraint
	 * with a transport guarantee that requires SSL
	 * @return
	 */
	public int getRedirectPort();
	
	public void setRedirectPort(int redirectPort);
	
	/**
	 * Return the scheme that will be assigned to request received through this connector.
	 * Default value is "http".
	 * @return
	 */
	public String getScheme();
	
	/**
	 * Set the scheme that will be assigned to requests received through this coneector
	 * @param scheme
	 */
	public void setScheme(String scheme);
	
	/**
	 * Return the secure connection flag that will be assigned to requests
	 * received through this connector. Default value is 'false'.
	 * @return
	 */
	public boolean getSecure();
	
	
	public void setSecure(boolean secure);
	
	/**
	 * Return the <code>Service</code> with which we are associated (if any)
	 * @return
	 */
	public Service getService();
	
	public void setService(Service service);
	
	/**
	 * Create (or allocate) and return a Request object suitable for 
	 * specifying the contents of a Request to the responsible Container
	 * @return
	 */
	public Request createRequest();
	
	/**
	 * Create (or allocate) and return a Response object suitable for
	 * receiving the contents of a Response from the responsible Container.
	 * @return
	 */
	public Response createResponse();
	
	/**
	 * Invoke a pre-startup initialization. This is used to allow connectors
	 * to bind restricted ports under Unix operating environments.
	 * 
	 * @throws LifecycleException
	 */
	public void initialize() throws LifecycleException;
}
