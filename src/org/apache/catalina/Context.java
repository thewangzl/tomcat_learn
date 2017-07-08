package org.apache.catalina;

import javax.servlet.ServletContext;

public interface Context extends Container{

	ServletContext getServletContext();

	Manager getManager();

	boolean getCookies();

	String getPath();
	
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
}
