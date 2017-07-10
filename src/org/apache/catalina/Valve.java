package org.apache.catalina;

import java.io.IOException;

import javax.servlet.ServletException;

public interface Valve {

	
	/**
	 * <p> Perform request processing as required by this valve</p>
	 *TODO
	 * 
	 * @param request
	 * @param response
	 * @param context the Valve context used to invoke the next valve in the current processing pipeline
	 * @throws IOException
	 * @throws ServletException
	 */
	public void invoke(Request request, Response response,ValveContext context) throws IOException, ServletException;
	
	
	/**
	 * Return descriptive information about this Valve implementation
	 * @return
	 */
	public String getInfo();
}
