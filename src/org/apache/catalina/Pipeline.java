package org.apache.catalina;

import java.io.IOException;

import javax.servlet.ServletException;

public interface Pipeline {

	public Valve getBasic();
	
	public void setBasic(Valve basic);
	
	public void addValve(Valve valve);
	
	public Valve[] getValves();
	
	public void removeValve(Valve valve);
	
	/**
	 * Cause the specified request and response to be processed by the Valves associated with this pipline,
	 * until one of these valves causes the response to be created and returned. The implementation must
	 * ensure that multiple simultaneous requests ( on different threads ) can be processed through the
	 * same Pipeline without interfering with each other's control flow.
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ServletException
	 */
	public void invoke(Request request, Response response) throws IOException, ServletException;;
	
}
