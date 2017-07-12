package org.apache.catalina.core;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Host;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.valves.ValveBase;

public class StandardEngineValve extends ValveBase {

	private static final StringManager sm = StringManager.getManager(Constants.Package);
	
	private static final String info = "org.apache.catalina.core.StandardEngineValve/1.0";

	@Override
	public void invoke(Request request, Response response, ValveContext valveContext)
			throws IOException, ServletException {

		// Validate the request and response object types
		if (!(request.getRequest() instanceof HttpServletRequest)
				|| !(response.getResponse() instanceof HttpServletResponse)) {
			return; // NOTE - Not much else we can do generically
		}

		// Validate that any HTTP/1.1 request included a host header
		HttpServletRequest hrequest = (HttpServletRequest) request;
		if ("HTTP/1.1".equals(hrequest.getProtocol()) && (hrequest.getServerName() == null)) {
			((HttpServletResponse) response.getResponse()).sendError(HttpServletResponse.SC_BAD_REQUEST,
					sm.getString("standardEngine.noHostHeader", request.getRequest().getServerName()));
			return;
		}

		//Select the Host to be used for this Request
		StandardEngine engine = (StandardEngine) getContainer();
		Host host = (Host) engine.map(request, true);
		if(host == null){
			((HttpServletResponse) response.getResponse()).sendError(HttpServletResponse.SC_BAD_REQUEST, sm.getString("standardEngine.noHost", request.getRequest().getServerName()));
			return;
		}
		
		//Ask this host to process this request
		host.invoke(request, response);
	}
	
	public String getInfo() {
		return info;
	}

}
