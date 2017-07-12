package org.apache.catalina;

/**
 * A <b>Host</b> is a Container that represents a virtual host in the Catalina servlet engine. 
 * It is useful in the following types if scenarios:
 * <ul>
 * 	<li>You wish to use Interceptots that see every single request processd by this particular 
 * 		virtual host.
 * 	<li>You with to run Catalina in with a standalone HTTP connector, but still want support for
 * 		multiple virtual hosts.
 * </ul>
 * In general, you would not use a Host when deploying Catalina connected to w web server (such as 
 * Apache), because the Connector will have utilized the web server's facilities to determine which 
 * Context(or perhaps even which Wrapper) should be utilized to process this request.
 * <p> 
 * The parent Container attached to a Host is generally an Engine, but may be some other implementation, 
 * or may be omitted if it is not necessary.
 * <p>The child containers attached to a Host are generally implementations of Context(representing 
 * an individual servlet Context).
 * 
 * @author thewangzl
 *
 */
public interface Host extends Container {

	
	public static final String ADD_ALIAS_EVENT = "addAlias";
	
	public static final String REMOVE_ALIAS_EVENT = "removeAlias";
	
	/**
	 * Return the application root for this Host. This can be an absolute
	 * pathname, a relative pathname, or a URL.
	 * 
	 * @return
	 */
	public String getAppBase();
	
	public void setAppBase(String appBase);
	
	/**
	 * Return the valuee of the auto deploy flag. If true, ir indicates that this
	 * host's child webapps should be discoverd and automatically deployed.
	 */
	public boolean getAutoDeploy();
	
	public void setAutoDeploy(boolean autoDeploy);
	
	/**
	 * Set the defaultContext for new web applications
	 * 
	 * @param defaultContext
	 */
	public void addDefaultContext(DefaultContext defaultContext);

	public DefaultContext getDefaultContext();

	/**
	 * Import the DefaultContext config into a web appliation context.
	 * 
	 * @param context
	 */
	public void importDefaultContext(Context context);
	
	/**
	 * Return the Context that would be used to process the specified host-relative
	 * request URI, if any; otherwise return <code>null</code>.
	 * 
	 * @param uri
	 * @return
	 */
	public Context map(String uri);

	
	/**
	 * Add an alias name that should be mapped to this same Host.
	 * 
	 * @param alias
	 */
	public void addAlias(String alias);
	
	
	public String[] findAliases();
	
	
	public void removeAlias(String alias);
	
	
	
	
}
