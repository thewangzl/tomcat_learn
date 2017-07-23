package org.apache.catalina;

import javax.servlet.ServletContext;

import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;

public interface Context extends Container{
	
	/**
	 * The lifecycleEvent type sent when a context is reloaded.
	 */
	public static final String RELOAD_EVENT = "reload";

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

	ErrorPage findErrorPage(String exceptionType);

	ErrorPage findErrorPage(int errorCode);

	Object[] getApplicationListeners();

	void setApplicationListeners(Object[] applicationListenersObjects);

	int getSessionTimeout();

	void setSessionTimeout(int timeout);

	SecurityConstraint[] findConstraints();

	LoginConfig getLoginConfig();

	void setLoginConfig(LoginConfig loginConfig);
	
	Realm getRealm();

	void addInstanceListener(String listener);

	void addFilterDef(FilterDef filterDef);

	void addMimeMapping(String extension, String mimeType);

	void addParameter(String name, String value);

	void addErrorPage(ErrorPage errorPage);

	void addConstraint(SecurityConstraint constraint);

	boolean getDistributable();

	void setDistributable(boolean distributable);

	Wrapper createWrapper();

	boolean getOverride();

	void setOverride(boolean override);

	boolean findSecurityRole(String string);

	void addRoleMapping(String role, String link);

	void addSecurityRole(String role);

	String findRoleMapping(String role);

	String[] findSecurityRoles();

	void addTaglib(String uri, String location);

	void addWelcomeFile(String name);

	String findTaglib(String uri);

	boolean findWelcomeFile(String name);

	int[] findStatusPages();

	String findStatusPage(String status);

	String[] findTaglibs();

	void removeTaglib(String uri);

	String[] findApplicationListeners();

	ApplicationParameter[] findApplicationParameters();

	void removeApplicationListener(String listener);

	void removeApplicationParameter(String name);

	void removeConstraint(SecurityConstraint constraint);

	ErrorPage[] findErrorPages();

	FilterDef findFilterDef(String name);

	FilterDef[] findFilterDefs();

	FilterMap[] findFilterMaps();

	String[] findInstanceListeners();

	String findMimeMapping(String extension);

	String[] findMimeMappings();

	String findParameter(String name);

	String[] findParameters();

	void removeErrorPage(ErrorPage errorPage);

	void removeFilterDef(FilterDef filterDef);

	void removeFilterMap(FilterMap filterMap);

	void removeInstanceListener(String listener);

	void removeParameter(String name);

	void removeServletMappings(String pattern);

	void removeMimeMapping(String extension);

	void removeRoleMapping(String role);

	void removeSecurityRole(String role);

	void removeServletMapping(String pattern);

	void removeWelcomeFile(String name);

	void removeWrapperLifecycle(String listener);

	void removeWrapperListener(String listener);

	String[] findWelcomeFiles();

	String[] findWrapperLifecycles();

	String[] findWrapperListeners();

	void addWrapperLifecycle(String listener);

	void addWrapperListener(String listener);
}
