package org.apache.catalina.core;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Session;
import org.apache.catalina.ValveContext;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.valves.ValveBase;

public class StandardHostValve extends ValveBase {

	
	private static final StringManager sm = StringManager.getManager(Constants.Package);
	
	private static final String info = "org.apache.catalina.core.StandardHostValve";
	
	
	/**
	 * 
	 */
	@Override
	public void invoke(Request request, Response response, ValveContext valveContext) throws IOException, ServletException {

		//Validate the request and resonse object types
		if(!(request.getRequest() instanceof HttpServletRequest) || !(response.getResponse() instanceof HttpServletResponse)){
			return;
		}

		//Select the Context to be used for this Request
		StandardHost host = (StandardHost)getContainer();
		Context context = (Context) host.map(request, true);
		if(context == null){
			((HttpServletResponse) response.getResponse()).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, sm.getString("standardHost.noContext"));
			return;
		}
		
		//Bind the context CL to the current thread
		Thread.currentThread().setContextClassLoader(context.getLoader().getClassLoader());
		
		//Update the session last access time for our session (if any)
		HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
		String sessionId = hreq.getRequestedSessionId();
		if(sessionId != null){
			Manager manager = context.getManager();
			if(manager != null){
				Session session = manager.findSession(sessionId);
				if(sessionId != null && session.isValid()){
					session.access();
				}
			}
			
		}
		//Ask this Context to process this request
		context.invoke(request, response);
		
		
		
	}
	
	public String getInfo() {
		return info;
	}

}
