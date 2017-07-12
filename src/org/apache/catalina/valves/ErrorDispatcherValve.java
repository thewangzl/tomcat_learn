package org.apache.catalina.valves;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.Wrapper;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.util.RequestUtil;

public class ErrorDispatcherValve extends ValveBase {

	@Override
	public void invoke(Request request, Response response, ValveContext valveContext) throws IOException, ServletException {

		// Perform the request
		valveContext.invokeNext(request, response);

        response.setSuspended(false);
		//
		if(!(response instanceof HttpResponse)){
			return;
		}
		HttpResponse hresponse = (HttpResponse) response;
		if(!(hresponse.getResponse() instanceof HttpServletResponse)){
			return;
		}
		
		int statusCode = hresponse.getStatus();
		String message = RequestUtil.filter(hresponse.getMessage());
		if(message == null){
			message = "";
		}
		
		// Handle a custom error Page for this status code
		Context context = request.getContext();
		if(context == null){
			return;
		}
		
		ErrorPage errorPage = context.findErrorPage(statusCode);
		
		if(errorPage != null){
			response.setAppCommitted(false);
			ServletRequest sreq = request.getRequest();
			ServletResponse sresp = response.getResponse();
			sreq.setAttribute(Globals.STATUS_CODE_ATTR, statusCode);
			sreq.setAttribute(Globals.ERROR_MESSAGE_ATTR, message);
			
			Wrapper wrapper = request.getWrapper();
			if(wrapper != null){
				sreq.setAttribute(Globals.SERVLET_NAME_ATTR, wrapper.getName());
			}
			
			if(sreq instanceof HttpServletRequest){
				sreq.setAttribute(Globals.EXCEPTION_PAGE_ATTR, ((HttpServletRequest)sreq).getRequestURI());
			}
			
			if(custom(request, response, errorPage)){
				try {
					sresp.flushBuffer();
				} catch (IOException e) {
					log("Exception Processing  " + errorPage, e);
				}
			}
			
		}
	}

	/**
	 *  Handle an HTTP status code or Java exception by forwarding control
     * to the location included in the specified errorPage object.  It is
     * assumed that the caller has already recorded any request attributes
     * that are to be forwarded to this page.  Return <code>true</code> if
     * we successfully utilized the specified error page location, or
     * <code>false</code> if the default error report should be rendered.
     * 
	 * @param request
	 * @param response
	 * @param errorPage
	 * @return
	 */
	private boolean custom(Request request, Response response, ErrorPage errorPage) {
		if (debug >= 1)
            log("Processing " + errorPage);

        // Validate our current environment
        if (!(request instanceof HttpRequest)) {
            if (debug >= 1)
                log(" Not processing an HTTP request --> default handling");
            return (false);     // NOTE - Nothing we can do generically
        }
        HttpServletRequest hreq =
            (HttpServletRequest) request.getRequest();
        if (!(response instanceof HttpResponse)) {
            if (debug >= 1)
                log("Not processing an HTTP response --> default handling");
            return (false);     // NOTE - Nothing we can do generically
        }
        HttpServletResponse hres =  (HttpServletResponse) response.getResponse();
        try {
        	
        	// Reset the response if possible (else IllegalStateException)
        	hres.reset();
        	
        	//Forward control to the specified location
        	ServletContext servletContext = request.getContext().getServletContext();
        	RequestDispatcher rd = servletContext.getRequestDispatcher(errorPage.getLocation());
        	//
        	rd.forward(hreq, hres);
        	
        	//If we forward, the response is suspended again
        	response.setSuspended(false);
			
        	//Indicate that we have successfully processed this custom page
        	return true;
		} catch (Throwable t) {
			 // Report our failure to process this custom page
            log("Exception Processing " + errorPage, t);
            return (false);
		}
	}

}
