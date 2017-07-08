package org.apache.catalina;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

public interface Wrapper extends Container {

	/**
	 * Return the fully qualified servlet class name for this servlet
	 * @return
	 */
	public String getServletClass();
	
	public void setServletClass(String servletClass);

	/**
	 * Allocate an initialized instance of this Servlet that is ready to have its <code>service() </code> method called.
	 * If the servlet class does not implement <code> SingleThreadModel </code>, the (only) 
	 * initialized instance may be returned immediately. If the servlet clas implements <code>
	 * SingleThreadModel </code>, the Wrapper implementation must ensure that this instance
	 * is not allocated again until it is deallocated by a call to <code> deallocate() </code>.
	 * @return
	 * @throws ServletException
	 */
	public Servlet allocate() throws ServletException;
	
	
	public void deallocated(Servlet servlet) throws ServletException;
	
	/**
	 * Load and initialize an instance of this servlet, if there is not already at least one
	 * initialized instance. This can be used, for example, to load servlets that are marked 
	 * in the deployment descriptor to be loaded at server startup time.
	 * @throws ServletException
	 */
	public void load() throws ServletException;
	
	
}
