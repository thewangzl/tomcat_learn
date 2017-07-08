package org.apache.catalina;

/**
 * Interface defining methods that a parent Container may implement to select a subordinate Container 
 * to process a particular Request, optionally modifying the properties of the Request to reflect the 
 * selections made.
 * <p>
 * A typical Container may be associated with a single Mapper that processes all requets to that
 * Container, or a Mapper per request protocol that allows the same Container to support multiple
 * protocols at once.
 * 
 * @author thewangzl
 *
 */
public interface Mapper {

	/**
	 * Return the Container with which this Mapper is associated.
	 * 
	 * @return
	 */
	public Container getContainer();
	
	public void setContainer(Container container);
	
	/**
	 * Return the protocol for which this Mapper is responsible.
	 * 
	 * @return
	 */
	public String getProtocol();
	
	public void setProtocol(String protocol);
	
	/**
	 * Return the child Container that should be used to process this Request, based upon its characteristics. 
	 * If no such child Container can be identified, return <code>null</code> instead.
	 * 
	 * @param request Request being processed.
	 * @param update Update the Request to reflect the mapping selection?
	 * @return
	 */
	public Container map(Request request, boolean update);
}
