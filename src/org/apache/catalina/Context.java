package org.apache.catalina;

import javax.servlet.ServletContext;

public interface Context extends Container{

	ServletContext getServletContext();

	Manager getManager();

	boolean getCookies();

	String getPath();
	
	void setPath(String path);
	
	/**
	 * Add a new servlet mapping, relacing any existing mapping for the specified pattern.
	 * 
	 * @param pattern
	 * @param name
	 */
	public void addServletMapping(String pattern, String name);

	/**
	 * Return the servlet name mapped by the specified pattern (if any);
	 * otherwise return <code>null</code>
	 *  
	 * @param pattern
	 * @return
	 */
	String findServletMapping(String pattern);

	/**
	 * Return the patterns of all defined servlet mappings for this Context.
	 * If no mappings are defined, a zero-length array is returned.
	 * 
	 * @return
	 */
	public String[] findServletMappings();
	
	/**
	 * Return the reloadable flag for this web appliation
	 * @return
	 */
	public boolean getReloadable();
	
	
	public void setReloadable(boolean reloadable);

	/**
	 * Reload this web application, if reloading is supported.
	 * 
	 */
	void reload();
	
	/**
	 * Return the "correctly configured" flag for this Context
	 * 
	 * @return
	 */
	boolean getConfigured();

	/**
	 * Set the "correctly configured" flag for this Context. This can be set to false  by startup
	 * listeners that detect a fatal configuration error to avoid application from being made available.
	 * 
	 * @param configured
	 */
	void setConfigured(boolean configured);
	
	
	String getDocBase();
	
	void setDocBase(String docBase);
	
	boolean getAvailable();
	
	void setAvailable(boolean available);
	
	/**
	 * Return the privileged flag for this web application
	 * 
	 * @return
	 */
	boolean getPrivileged();
	
	
	void setPrivileged(boolean privileged);
	
}
