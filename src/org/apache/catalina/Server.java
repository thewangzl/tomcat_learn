package org.apache.catalina;

import org.apache.catalina.deploy.NamingResources;

public interface Server {

	// ----------------------------------------------------- Properties
	
	public String getInfo();
	
	public NamingResources getGlobalNamingResources();
	
	public void setGlobalNamingResources(NamingResources globalNamingResources);
	
	public int getPort();
	
	public void setPort(int port);
	
	public String getShutdown();
	
	public void setShutdown(String shutdown);
	
	// ------------------------------------------ Public Methods
	
	/**
	 * Wait until a proper shutdown command is received, then return.
	 */
	public void await();
	
	public void addService(Service service);

	public Service findService(String name);
	
	public Service[] findServices();
	
	public void removeService(Service service);
	
	/**
	 * Invoke pre-startup initialization. This is used to allow connectors to 
	 * bind to restricted ports under Unix operating environments.
	 * 
	 * @throws LifecycleException
	 */
	public void initialize() throws LifecycleException;
	
	
}
