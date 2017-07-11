package org.apache.catalina.core;


import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.Wrapper;
import org.apache.catalina.valves.ValveBase;

/**
 * 
 * @author thewangzl
 *
 */
public class StandardContextValve extends ValveBase{

	
	
	@Override
	public void invoke(Request request, Response response, ValveContext valveContext) throws IOException, ServletException {

		//Validate
		if(!(request.getRequest() instanceof HttpServletRequest || response.getResponse() instanceof HttpServletResponse)){
			return;
		}
		
		//Disallow any direct access to resources under WEB-INF or META-INF
		HttpServletRequest  hreq = (HttpServletRequest) request.getRequest();
		String contextPath = hreq.getContextPath();
//		String requestURI = ((HttpRequest)request).getDecodedRequestURI();
		String requestURI = hreq.getRequestURI();
		String relativeURI = requestURI.substring(contextPath.length()).toUpperCase();
		if(relativeURI.equals("/META-INF") || relativeURI.equals("/WEB-INF") || relativeURI.startsWith("/META-INF/") || relativeURI.startsWith("/WEB-INF/")){
			notFound(requestURI, (HttpServletResponse) response.getResponse());
			return;
		}
		
		Context context = (Context) getContainer();
		//Select the Wrapper to be used for this Request
		Wrapper wrapper = null;
		try{
			wrapper = (Wrapper) context.map(request, true);
		}catch(IllegalArgumentException e){
			badRequest(requestURI, (HttpServletResponse) response.getResponse());
			return;
		}
		
		if(wrapper == null){
			notFound(requestURI, (HttpServletResponse) response);
			return;
		}
		
		//Ask this Wrapper to process this Request
		response.setContext(context);
		
		wrapper.invoke(request, response);
		
	}

	private void badRequest(String requestURI, HttpServletResponse response) {
		try {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, requestURI);
        } catch (IllegalStateException e) {
            ;
        } catch (IOException e) {
            ;
        }
	}

	private void notFound(String requestURI, HttpServletResponse response) {
		try {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, requestURI);
        } catch (IllegalStateException e) {
            ;
        } catch (IOException e) {
            ;
        }
	}

	
}
