package org.apache.catalina.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.InstanceEvent;
import org.apache.catalina.Logger;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.catalina.util.InstanceSupport;
import org.apache.catalina.util.StringManager;

/**
 * Standard implementation of <code>RequestDispatcher</code> that allows a request to be forwarded to 
 * a different resource to create the ultimate response, or to include the output of another in the
 * response from this resource. This implementation allows application level servlets to wrap the 
 * request and/or response objects that are passed on the called resource, as long as the wrapping 
 * classes extend <code>javax.servlet.ServletRequestWrapper</code> and 
 * <code>javax.servlet.ServletResponseWrapper</code>.
 * 
 * @author thewangzl
 *
 */
public class ApplicationDispatcher implements RequestDispatcher {

	/**
	 * The request specified by the dispatching application
	 */
	@SuppressWarnings("unused")
	private ServletRequest appRequest;

	/**
	 * The response specified by the dispatching application
	 */
	@SuppressWarnings("unused")
	private ServletResponse appResponse;

	/**
	 * The Context this RequestDispatcher is associated with.
	 */
	private Context context;

	private int debug = 0;

	/**
	 * Are we performing an include() instead of a forward()?
	 */
	private boolean including;

	private static final String info = "org.apache.catalina.core.ApplicationDispatcher/1.0";

	/**
	 * The servlet name for a named dispatcher.
	 */
	private String name;

	/**
	 * The outermost request that will be passed on to the invoked servlet.
	 */
	private ServletRequest outerRequest;

	/**
	 * The outermost response that will be passed on to the invoked servlet.
	 */
	private ServletResponse outerResponse;

	/**
	 * The extra path informatation for this RequestDispatcher.
	 */
	private String pathInfo;

	/**
	 * The query string parameters for this RequestDispatcher.
	 */
	private String queryString;

	/**
	 * The servlet path for this RequestDispatcher.
	 */
	private String servletPath;

	private static final StringManager sm = StringManager.getManager(Constants.Package);

	/**
	 * The InstanceSupport intance associated with our Wrapper 
	 * (used to send "before dispatch" and "after dispatch" events)
	 */
	private InstanceSupport support;

	/**
	 * The Wrapper associated with the resource that will be forwarded to or included.
	 */
	private Wrapper wrapper;

	/**
	 * The request wrapper we have created and installed (if any)
	 */
	private ServletRequest wrapRequest;

	/**
	 * The response wrapper we have created and installed (if any)
	 */
	private ServletResponse wrapResponse;

	public ApplicationDispatcher(Wrapper wrapper, String servletPath, String pathInfo, String queryString,
			String name) {
		this.name = name;
		this.pathInfo = pathInfo;
		this.queryString = queryString;
		this.servletPath = servletPath;
		this.wrapper = wrapper;
		this.context = (Context) wrapper.getParent();
		if (wrapper instanceof StandardWrapper) {
			this.support = ((StandardWrapper) wrapper).getInstanceSupport();
		} else {
			this.support = new InstanceSupport(wrapper);
		}

		if (debug >= 1) {
			log("ServletPath=" + this.servletPath + ", pathInfo=" + this.pathInfo + ", queryString=" + queryString
					+ ", name=" + name);
		}
		
		//If this is a wrappe for a JSP page (<jsp-file>), tweak the request parameters appropriately
		String jspFile = wrapper.getJspFile();
		if(jspFile != null){
			if(debug >= 1){
				log(" -->servletPath=" + jspFile);
			}
			this.servletPath = jspFile;
		}
	}

	/**
	 * Forward this request and response to another resource for processing.
	 * Any runtime exception, IOException, or ServletException thrown by the 
	 * called servlet will be propogated to the called.
	 * 
	 * @param request
	 * @param response
	 * 
	 * @throws ServletException 
	 * @throws IOException
	 */
	@Override
	public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {

		if(System.getSecurityManager() != null){
			try {
				PrivilegedForward dp = new PrivilegedForward(request, response);
				AccessController.doPrivileged(dp);
			} catch (PrivilegedActionException pe) {
				Exception e = pe.getException();
				pe.printStackTrace();
				if(e instanceof ServletException){
					throw (ServletException)e;
				}
				throw (IOException) e;
			}
		}else{
			doForward(request, response);
		}

	}
	
	private void doForward(ServletRequest request, ServletResponse response) throws ServletException, IOException{
		
		//Reset any output that has been buffed, but keep headers/cookies
		if(response.isCommitted()){
			if(debug >= 1){
				log(" Forward on committed response  --> ISE");
				throw new IllegalStateException(sm.getString("applicationDispatcher.forward.ise"));
			}
		}
		try {
			response.resetBuffer();
		} catch (IllegalStateException e) {
			if(debug >= 1){
				log(" Forward resetBuffer() returned ISE: " +  e);
				throw e;
			}
		}
		
		// Set up yo handle the specified request and response 
		setup(request, response, false);
		
		//Identify the HTTP-specific request and response objects (if any)
		HttpServletRequest hrequest = null;
		HttpServletResponse hresponse = null;
		if(request instanceof HttpServletRequest){
			hrequest = (HttpServletRequest) request;
		}
		if(response instanceof HttpServletResponse){
			hresponse = (HttpServletResponse) response;
		}
	
		// Handle a non-HTTP forward by passing the existing request/response
		if(hrequest == null || hresponse == null){
			if(debug >= 1){
				log("NON-HTTP Forward");
			}
			invoke(request, response);
		}
		// Handle an HTTP named dispatcher forward
		else if(servletPath == null && pathInfo == null ){
			if(debug>= 1){
				log(" Named Dispatcher Forwrad");
			}
			invoke(request, response);
		}
		// Handle an HTTP path-baed forward
		else{
			if(debug >= 1){
				log("Path Based Forward");
			}
			
			ApplicationHttpRequest wrequest = (ApplicationHttpRequest) wrapRequest();
			StringBuffer sb = new StringBuffer();
			String contextPath = context.getPath();
			if(contextPath != null){
				sb.append(contextPath);
			}
			if(servletPath != null){
				sb.append(servletPath);
			}
			if(pathInfo != null){
				sb.append(pathInfo);
			}
			wrequest.setContextPath(contextPath);
			wrequest.setRequestURI(sb.toString());
			wrequest.setServletPath(contextPath);
			wrequest.setPathInfo(pathInfo);
			if(queryString != null){
				wrequest.setQueryString(queryString);
				wrequest.mergeParameters(queryString);
			}
			invoke(outerRequest, response);
			unwrapRequest();
		}
		
		// Commit and close the response before we return;
		if(debug >= 1){
			log("Commiting and closing response");
		}
		
		if(response instanceof ResponseFacade){
			((ResponseFacade) response).finish();
		}else{
			//Close anyway
			try {
				response.flushBuffer();
			} catch (IllegalStateException e) {
				;
			}
			try {
				PrintWriter writer = response.getWriter();
				writer.flush();
				writer.close();
			} catch (IllegalStateException e) {
				try {
					ServletOutputStream stream = response.getOutputStream();
					stream.flush();
					stream.close();
				} catch (IllegalStateException ise) {
					;
				}catch (IOException ie) {
					;
				}
			} catch (IOException e) {
				;
			}
		}
	}


	@Override
	public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		if(System.getSecurityManager() != null){
			try {
				PrivilegedInclude dp = new PrivilegedInclude(request, response);
				AccessController.doPrivileged(dp);
			} catch (PrivilegedActionException pae) {
				Exception e = pae.getException();
				pae.printStackTrace();
				if(e instanceof ServletException){
					throw (ServletException)e;
				}
				throw (IOException)e;
			}
		}else{
			doInclude(request, response);
		}
	}
	
	@SuppressWarnings("unused")
	private void doInclude(ServletRequest request, ServletResponse response) throws ServletException, IOException{
		
		// set up to handle the specified request and response
		setup(request, response, true);
		
		//Create a wrapped response to use for this request
		//ServletResponse wresponse =null;
		ServletResponse wresponse = wrapResponse();
		
		//Handle a non-HTTP including
		if(!(request instanceof HttpServletRequest || response instanceof HttpServletResponse)){
			if(debug >=1){
				log("Non-HTTP Include");
			}
			invoke(request, outerResponse);
			unwrapResponse();
		}
		//Handle an HTTP named dispatcher include 
		else if(name != null){
			if(debug >= 1){
				log("Naming Dispatcher Include");
			}
			
			ApplicationHttpRequest wrequest = (ApplicationHttpRequest) wrapRequest();
			wrequest.setAttribute(Globals.NAMED_DISPATCHER_ATTR, name);
			if(servletPath != null){
				wrequest.setServletPath(servletPath);
			}
			invoke(outerRequest, outerResponse);
			unwrapRequest();
			unwrapResponse();
		}
		// Handle an HTTP path based include
		else{
			if(debug >= 1){
				log("Path Based Include");
			}
			
			ApplicationHttpRequest wrequest = (ApplicationHttpRequest) wrapRequest();
			StringBuffer sb = new StringBuffer();
			String contextPath = context.getPath();
			if(contextPath != null){
				sb.append(contextPath);
			}
			if(servletPath != null){
				sb.append(servletPath);
			}
			if(pathInfo != null){
				sb.append(pathInfo);
			}
			if(sb.length() >0){
				wrequest.setAttribute(Globals.REQUEST_URI_ATTR, sb.toString());
			}
			if(contextPath != null){
				wrequest.setAttribute(Globals.CONTEXT_PATH_ATTR, contextPath);
			}
			if(servletPath != null){
				wrequest.setAttribute(Globals.SERVLET_PATH_ATTR, servletPath);
			}
			if(pathInfo != null){
				wrequest.setAttribute(Globals.PATH_INFO_ATTR, pathInfo);
			}
			if(queryString != null){
				wrequest.setAttribute(Globals.QUERY_STRING_ATTR, queryString);
				wrequest.mergeParameters(queryString);
			}
			//invoke(
			invoke(outerRequest, outerResponse);
			unwrapRequest();
			unwrapResponse();
		}
	}
	
	// ------------------------------------------------------ Private Methods
	
	/**
	 * Ask the resource represented by this RequestDispatcher to process the associated request, and create
	 * (or append to) the associated response.
	 * <p>
	 * 
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ServletException
	 */
	private void invoke(ServletRequest request, ServletResponse response) throws IOException, ServletException{
		
		//Checking to see if the context classloader is the context classloader. If it's not, we're saving it,
		// and setting the context classloader to the Context classloader.
		ClassLoader oldCCL = Thread.currentThread().getContextClassLoader();
		ClassLoader contextClassLoader = context.getLoader().getClassLoader();
		
		if(oldCCL != contextClassLoader){
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}else{
			oldCCL = null;
		}
		
		//Initialize local variables we may need.
		HttpServletRequest hrequest = null;
		HttpServletResponse hresponse = null;
		if(request instanceof HttpServletRequest){
			hrequest = (HttpServletRequest) request;
		}
		if(response instanceof HttpServletResponse){
			hresponse = (HttpServletResponse) response;
		}
		
		Servlet servlet = null;
		IOException ioException = null;
		ServletException servletException = null;
		RuntimeException runtimeException = null;
		boolean unvailable = false;
		
		// Check for the servlet being marked unavailable
		if(wrapper.isUnavailable()){
			log(sm.getString("applicationDispatcher.isUnavailable", wrapper.getName()));
			if(hresponse == null){
				;
			}else{
				long available = wrapper.getAvailable();
				if(available > 0L && available < Long.MAX_VALUE){
					hresponse.setDateHeader("Retry-After", available);
				}
				hresponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, sm.getString("applicationDispatcher.isUnavailable", wrapper.getName()));
			}
			unvailable = true;
		}
		
		// Allocate a servlet instance to process this request
		try{
			if(!unvailable){
				servlet = wrapper.allocate();
			}
		}catch(ServletException e){
			log(sm.getString("applicationDispatcher.allocateException", wrapper.getName()), e);
			servletException = e;
			servlet = null;
		}catch(Throwable e){
			log(sm.getString("applicationDispatcher.allocateException", wrapper.getName()), e);
			servletException = new ServletException(sm.getString("applicationDispatcher.allocateException", wrapper.getName()), e);
			servlet = null;
		}
		
		//Call the service() method for the allocated servlet instance
		
		try {
			String jspFile = wrapper.getJspFile();
			if(jspFile != null){
				request.setAttribute(Globals.JSP_FILE_ATTR, jspFile);
			}else{
				request.removeAttribute(Globals.JSP_FILE_ATTR);
			}
			support.fireInstanceEvent(InstanceEvent.BEFORE_DISPATCH_EVENT, servlet, hrequest, hresponse);
			if(servlet != null){
				if(hrequest != null && hresponse != null){
					servlet.service((HttpServletRequest) request, (HttpServletResponse)response); 
				}else{
					servlet.service(request, response);
				}
			}
			request.removeAttribute(Globals.JSP_FILE_ATTR);
			support.fireInstanceEvent(InstanceEvent.AFTER_DISPATCH_EVENT, servlet, hrequest, hresponse);
		} catch (IOException e) {
			request.removeAttribute(Globals.JSP_FILE_ATTR);
			support.fireInstanceEvent(InstanceEvent.AFTER_DISPATCH_EVENT, servlet, hrequest, hresponse);
			log(sm.getString("applicationDispatcher.serviceException", wrapper.getName()), e);
			ioException = e;
		} catch(UnavailableException e){
			request.removeAttribute(Globals.JSP_FILE_ATTR);
			support.fireInstanceEvent(InstanceEvent.AFTER_DISPATCH_EVENT, servlet, hrequest, hresponse);
			log(sm.getString("applicationDispatcher.serviceException", wrapper.getName()), e);
			servletException = e;
			wrapper.unavailable(e);
		} catch(ServletException e){
			request.removeAttribute(Globals.JSP_FILE_ATTR);
			support.fireInstanceEvent(InstanceEvent.AFTER_DISPATCH_EVENT, servlet, hrequest, hresponse);
			log(sm.getString("applicationDispatcher.serviceException", wrapper.getName()), e);
			servletException = e;
		} catch(RuntimeException e){
			request.removeAttribute(Globals.JSP_FILE_ATTR);
			support.fireInstanceEvent(InstanceEvent.AFTER_DISPATCH_EVENT, servlet, hrequest, hresponse);
			log(sm.getString("applicationDispatcher.serviceException", wrapper.getName()), e);
			runtimeException = e;
		}
		
		// Deallocate the allocated servlet instance
		try{
			if(servlet != null){
				wrapper.deallocated(servlet);
			}
		}catch(ServletException e){
			log(sm.getString("applicationDispatcher.deallocateException", wrapper.getName()), e);
			servletException = e;
		}catch(Throwable e){
			log(sm.getString("applicationDispatcher.deallocateException", wrapper.getName()), e);
			servletException = new ServletException(sm.getString("applicationDispatcher.deallocateException", wrapper.getName()), e);
		}
		
		// Reset the old context class loader
		if(oldCCL != null){
			Thread.currentThread().setContextClassLoader(oldCCL);
		}
		
		// Rethrow an exception if one was thrown by the invoked servlet
		if(ioException != null){
			throw ioException;
		}
		if(servletException != null){
			throw servletException;
		}
		if(runtimeException != null){
			throw runtimeException;
		}
	}

	
	/**
	 * Set up handle the specified request and response
	 *  
	 * @param request
	 * @param response
	 * @param including
	 */
	private void setup(ServletRequest request, ServletResponse response, boolean including) {

		this.appRequest = request;
		this.appResponse = response;
		this.outerRequest = request;
		this.outerResponse = response;
		this.including = including;
	} 
	
	/**
	 * Create and return a request wrapper that has been inserted in the appropriate spot in the request chain.
	 * 
	 * @return
	 */
	private ServletRequest wrapRequest(){
		
		// Locate the request we should insert in front of
		ServletRequest previous = null;
		ServletRequest current = outerRequest;
		while(current != null){
			if("org.apache.catalina.servlets.InvokerHttpRequest".equals(current.getClass().getName())){
				break;			// KLUDGE - Make nested RD.forward() using invoker work
			}
			if(!(current instanceof ServletRequestWrapper)){
				break;
			}
			if(current instanceof ApplicationHttpRequest){
				break;
			}
			if(current instanceof ApplicationRequest){
				break;
			}
			if(current instanceof Request){
				break;
			}
			previous = current;
			current = ((ServletRequestWrapper) current).getRequest();
		}
		
		// Instantiate a new wrapper at this point and insert it in the chain
		ServletRequest wrapper = null;
		if(current instanceof ApplicationHttpRequest || current instanceof HttpRequest || current instanceof HttpServletRequest){
			wrapper = new ApplicationHttpRequest((HttpServletRequest)current);
		}else{
			wrapper = new ApplicationRequest(current);
		}
		if(previous == null){
			outerRequest = wrapper;
		}else{
			((ServletRequestWrapper)previous).setRequest(wrapper);
		}
		wrapRequest = wrapper;
		return wrapper;
	}
	
	/**
	 * unwrap the request if we have wrapped it.
	 */
	public void unwrapRequest(){
		
		if(wrapRequest == null){
			return;
		}
		
		ServletRequest previous = null;
		ServletRequest current = outerRequest;
		while(current != null){
			
			//If we run into the container request we are done.
			if(current instanceof Request || current instanceof RequestFacade){
				break;
			}
			
			//Remove the current request if it is our wrapper
			if(current == wrapRequest){
				ServletRequest next = ((ServletRequestWrapper) current).getRequest();
				if(previous == null){
					outerRequest = next;
				}else{
					((ServletRequestWrapper) previous).setRequest(next);
				}
				break;
			}
			
			// Advance to the next request in the chain
			previous = current;
			current = ((ServletRequestWrapper) current).getRequest();
		}
		
	}
	
	/**
	 * Create and return a response wrapper that has been inserted in the appropriate 
	 * spot in the response chain.
	 * 
	 * @return
	 */
	private ServletResponse wrapResponse(){
		
		//Locate the response we should insert in front of
		ServletResponse previous = null;
		ServletResponse current = outerResponse;
		while(current != null){
			if(!(current instanceof ServletResponseWrapper)){
				break;
			}
			if(!(current instanceof ApplicationHttpResponse)){
				break;
			}
			if(!(current instanceof ApplicationResponse)){
				break;
			}
			if(!(current instanceof Response)){
				break;
			}
			previous = current;
			current = ((ServletResponseWrapper) current).getResponse();
		}
		
		// Instantiate a new wrapper at this point and insert it in the chain
		ServletResponse wrapper = null;
		if(current instanceof ApplicationHttpResponse || current instanceof HttpResponse || current instanceof HttpServletResponse){
			wrapper = new ApplicationHttpResponse((HttpServletResponse) current,including);
		}else{
			wrapper = new ApplicationResponse(current, including);
		}
		if(previous == null){
			outerResponse = wrapper;
		}else{
			((ServletResponseWrapper)previous).setResponse(wrapper);
		}
		wrapResponse = wrapper;
		return wrapper;
	}
	
	/**
	 * Unwrap the response if we have wrapped it.
	 */
	@SuppressWarnings("unused")
	private void unwrapResponse(){
		if(wrapResponse == null){
			return;
		}
		
		ServletResponse previous = null;
		ServletResponse current = outerResponse;
		while(current != null){
			
			//If we run into the container response we are done.
			if(current instanceof Response || current instanceof ResponseFacade){
				break;
			}
			
			//Remove the current response if it is our wrapper
			if(current == wrapResponse){
				ServletResponse next = ((ServletResponseWrapper) previous).getResponse();
				if(previous == null){
					outerResponse = next;
				}else{
					((ServletResponseWrapper) previous).setResponse(next);
				}
				break;	
			}
			
			// Advance to the next response in the chain
			previous = current;
			current = ((ServletResponseWrapper) current).getResponse();
		}
	}
	
	/**
	 * Logger a message on the Logger associated
	 * 
	 * @param message
	 */
	private void log(String message) {
		Logger logger = context.getLogger();
		if (logger != null) {
			logger.log("ApplicationDispatcher[" + context.getPath() + "]: " + message);
		} else {
			System.out.println("ApplicationDispatcher[" + context.getPath() + "]: " + message);
		}
		
	}
	
	private void log(String message, Throwable throwable) {

        Logger logger = context.getLogger();
        if (logger != null)
            logger.log("ApplicationDispatcher[" + context.getPath() +
                       "] " + message, throwable);
        else {
            System.out.println("ApplicationDispatcher[" +
                               context.getPath() + "]: " + message);
            throwable.printStackTrace(System.out);
        }

    }
	
	public String getInfo() {
		return info;
	}
	
	protected class PrivilegedForward implements PrivilegedExceptionAction<Object>{

		private ServletRequest request;
		private ServletResponse response;
		
		public PrivilegedForward(ServletRequest request, ServletResponse response) {
			this.request = request;
			this.response = response;
		}

		@Override
		public Object run() throws ServletException, IOException {
			doForward(request, response);
			return null;
		}
	}
	
	protected class PrivilegedInclude implements PrivilegedExceptionAction<Object>{

		private ServletRequest request;
		private ServletResponse response;
		
		public PrivilegedInclude(ServletRequest request, ServletResponse response) {
			this.request = request;
			this.response = response;
		}

		@Override
		public Object run() throws ServletException, IOException {
			doInclude(request, response);
			return null;
		}
	}

}
