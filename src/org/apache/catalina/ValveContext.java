package org.apache.catalina;

import java.io.IOException;

import javax.servlet.ServletException;

public interface ValveContext {

	public String getInfo();
	
	/**
	 * Cause the code <code>invoke()</code> method of the next Valve that is part of the Pipeline
	 * currently being processed (if any) to be executed.passing on the specified request and response 
	 * objects plus this <code>ValueContext<code> instance. Exceptions thrown by a subsequently executed Valve 
	 * (or a Filter or Servlet at the application level) will be passed on to our caller.
	 * 
	 * If there are no more Valves to be executed, an appropriate ServletException will be thrown by
	 * this ValveContext.
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ServletException
	 */
	public void invokeNext(Request request, Response response) throws IOException, ServletException;
}
