package org.apache.catalina;

/**
 * A <b>ContainerServlet</b> is a servlet that has access to Catalina internal funtionality,
 * and is loaded from the Catalina class loader instead of the web application class loader.
 * The property setter methods must be called by the container whenere a new instance of
 * this servlet is put into service.
 * 
 * @author thewangzl
 *
 */
public interface ContainerServlet {

	/**
	 * 
	 * @return
	 */
	public Wrapper getWrapper();
	
	public void setWrapper(Wrapper wrapper);
}
