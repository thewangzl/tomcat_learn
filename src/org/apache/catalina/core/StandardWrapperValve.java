package org.apache.catalina.core;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Logger;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.valves.ValveBase;

/**
 * 
 * @author thewangzl
 *
 */
public class StandardWrapperValve extends ValveBase {

	protected static final String info = "org.apache.catalina.core.StandardWrapperValve/1.0";
	
	protected static final StringManager sm = StringManager.getManager(Constants.Package);
	
	@Override
	public void invoke(Request request, Response response, ValveContext context) throws IOException, ServletException {

		//Initialize local variables we may need
		boolean unavailable = false;
		Throwable throwable = null;
		StandardWrapper wrapper = (StandardWrapper) getContainer();
		ServletRequest sreq = request.getRequest();
		ServletResponse sresp = response.getResponse();
		Servlet servlet = null;
//		HttpServletRequest hreq = null;
//		if(sreq instanceof HttpServletRequest){
//			hreq = (HttpServletRequest) sreq;
//		}
		HttpServletResponse hresp = null;
		if(sresp instanceof HttpServletResponse){
			hresp = (HttpServletResponse) sresp;
		}
		
		//Check for the application being marked unavailable
		if(!((Context) wrapper.getParent()).getAvailable()){
			hresp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,sm.getString("standardContext.unavailable"));
			unavailable = true;
		}
		
		//Check for servlet being marked unvailable
		if(!unavailable && wrapper.isUnavailable()){
			log(sm.getString("standardWrapper.isUnavailable",wrapper.getName()));
			if(hresp == null){
				;		//Note - Not much we can do generically
			}else{
				long available = wrapper.getAvailable();
				if(available > 0L && available < Long.MAX_VALUE){
					hresp.setDateHeader("Retry-After", available);
				}
				hresp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,sm.getString("standardWrapper.unavailable",wrapper.getName()));
			}
			unavailable = true;
		}
		
		//Allocate a servlet instance to process this request
		
		try{
			if(!unavailable){
				servlet = wrapper.allocate();
			}
		}catch(ServletException e){
			log(sm.getString("standardWrapper.allocateException", wrapper.getName()), e);
			throwable = e;
			exception(request, response, e);
			servlet = null;
		}catch (Throwable e){
			log(sm.getString("standardWrapper.allocateException", wrapper.getName()), e);
			throwable = e;
			exception(request, response, e);
			servlet = null;
		}
		
		// Acknowlege the request
		try{
			response.sendAcknowledgment();
		}catch(IOException e){
			sreq.removeAttribute(Globals.JSP_FILE_ATTR);
			log(sm.getString("standardWrapper.acknowledgeException", wrapper.getName()), e);
			throwable = e;
			exception(request, response, e);
		}catch (Throwable e) {
			sreq.removeAttribute(Globals.JSP_FILE_ATTR);
			log(sm.getString("standardWrapper.acknowledgeException", wrapper.getName()), e);
			throwable = e;
			exception(request, response, e);
			servlet = null;
		}
		
		// Create the filter chain for this request
		ApplicationFilterChain filterChain = this.createFilterChain(request, servlet);
		
		// Call the filter chain for this request 
		//Note: This also calls the servlet's service()  method
		
		try{
			String jspFile = wrapper.getJspFile();
			if(jspFile == null){
				sreq.setAttribute(Globals.JSP_FILE_ATTR, jspFile);
			}else{
				sreq.removeAttribute(Globals.JSP_FILE_ATTR);
			}
			
			if(servlet != null && filterChain != null){
				filterChain.doFilter(sreq, sresp);
			}
			sreq.removeAttribute(Globals.JSP_FILE_ATTR);
		}catch(IOException e){
			sreq.removeAttribute(Globals.JSP_FILE_ATTR);
			log(sm.getString("standardWrrapper.serviceException", wrapper.getName()), e);
			throwable = e;
			exception(request, response, e);
		}catch (UnavailableException e) {
			sreq.removeAttribute(Globals.JSP_FILE_ATTR);
			log(sm.getString("standardWrrapper.serviceException", wrapper.getName()), e);
//			throwable = e;
//			exception(request, response, e);
			wrapper.unavailable(e);
			long available = wrapper.getAvailable();
			if(available > 0L && available < Long.MAX_VALUE){
				hresp.setDateHeader("Retry-After", available);
			}
			hresp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, sm.getString("standardWrapper.isUnavailable", wrapper.getAvailable()));
		}catch (ServletException e) {
			sreq.removeAttribute(Globals.JSP_FILE_ATTR);
			log(sm.getString("standardWrrapper.serviceException", wrapper.getName()), e);
			throwable = e;
			exception(request, response, e);
		}catch (Throwable e) {
			sreq.removeAttribute(Globals.JSP_FILE_ATTR);
			log(sm.getString("standardWrrapper.serviceException", wrapper.getName()), e);
			throwable = e;
			exception(request, response, e);
		}
		
		//Release the filter chain (if any) for this request 
		try{
			if(filterChain != null){
				filterChain.release();
			}
		}catch(Throwable e){
			log(sm.getString("standardWrrapper.releaseFilters", wrapper.getName()), e);
			if(throwable == null){
				throwable = e;
				exception(request, response, e);
			}
		}
		
		//Deallocate the allocated servlet instance
		try{
			if(servlet != null){
				wrapper.deallocated(servlet);
			}
		}catch(Throwable e){
			log(sm.getString("standardWrrapper.deallocateException", wrapper.getName()), e);
			if(throwable == null){
				throwable = e;
				exception(request, response, e);
			}
		}
		
		//If this servlet has been marked permanently unavailable,
		// unload it and release this instance
		try{
			if(servlet != null && wrapper.getAvailable() == Long.MAX_VALUE){
				wrapper.unload();
			}
		}catch(Throwable e){
			log(sm.getString("standardWrrapper.unloadException", wrapper.getName()), e);
			if(throwable == null){
				throwable = e;
				exception(request, response, e);
			}
		}
	}
	
	/**
	 * Construct and return a FilterChain implementation that will wrap  the execution of the specific servlet instance. 
	 * If we should not execute a filter chain at all, return <code>null</code>
	 * 
	 * <p>
	 * <strong>FIXME</strong> --Pool the chain instances !
	 * 
	 * @param request The servlet request we are processing 
	 * @param servlet The servlet instance of to be wrapped
	 * @return
	 */
	private ApplicationFilterChain createFilterChain(Request request, Servlet servlet){
		//
		if(servlet == null){
			return null;
		}
		
		// Create and initialze a filter chain object
		ApplicationFilterChain filterChain = new ApplicationFilterChain();
		filterChain.setServlet(servlet);
		StandardWrapper wrapper = (StandardWrapper) getContainer();
		filterChain.setSupport(wrapper.getInstanceSupport());
		
		// Acquire the filter mapping for this Context
		StandardContext context = (StandardContext) wrapper.getParent();
		FilterMap[] filterMaps = context.findFilterMaps();
		
		//If there are no filter mappings, wre done
		if(filterMaps == null || filterMaps.length == 0){
			return filterChain;
		}
		
		// Acquire 
		String requestPath = null;
		if(request instanceof HttpRequest){
			HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
			String contextPath = hreq.getContextPath();
			if(contextPath == null){
				contextPath = "";
			}
			//FIXME - decodedRequestURI may not found , fix it at last
			String requestURI = ((HttpRequest)request).getDecodedRequestURI();
			if(requestURI.length() >= contextPath.length()){
				requestPath = requestURI.substring(contextPath.length());
			}
		}
		
		String servletName = wrapper.getName();
		
		// Add the relevant path-mapped filters to this filter chain
		for (FilterMap filterMap : filterMaps) {
			if(!matchFiltersURL(filterMap, requestPath)){
				continue;
			}
			ApplicationFilterConfig filterConfig = (ApplicationFilterConfig) context.findFilterConfig(filterMap.getFilterName());
			if(filterConfig == null){
				continue;
			}
			filterChain.addFilter(filterConfig);
		}
		
		// Add filters that match on servlet name second
		for (FilterMap filterMap : filterMaps) {
			if(!matchFiltersServlet(filterMap, servletName)){
				continue;
			}
			ApplicationFilterConfig filterConfig = (ApplicationFilterConfig) context.findFilterConfig(filterMap.getFilterName());
			if(filterConfig == null){
				continue;
			}
			filterChain.addFilter(filterConfig);
		}
		return filterChain;
	}

	/**
	 * Return <code>true</code> if the context-relative request path match the requirements 
	 * of the specified filter mapping; otherwise return <code>false</code>
	 * 
	 * @param filterMap
	 * @param requestPath
	 * @return
	 */
	private boolean matchFiltersURL(FilterMap filterMap, String requestPath) {

		if(requestPath == null){
			return false;
		}
		
		//Match on context relative request path
		String testPath = filterMap.getUrlPattern();
		if(testPath == null	){
			return false;
		}
		
		// Case 1 - Exact match
		if(testPath.equals(requestPath)){
			return true;
		}
		
		// Case 2 - Match ("/../*")
		if(testPath.equals("/*")){
			return true;			// Optimize a common case
		}
		if(testPath.endsWith("/*")){
			String comparePath = requestPath;
			while(true){
				if(testPath.equals(comparePath + "/*")){
					return true;
				}
				int slash = comparePath.lastIndexOf('/');
				if(slash < 0){
					break;
				}
				comparePath = comparePath.substring(0, slash);
			}
			return false;
		}
		
		// Case 3 - Extension Match
		if(testPath.startsWith("*.")){
			int slash = requestPath.lastIndexOf('/');
			int period = requestPath.lastIndexOf('.');
			if(slash >= 0 && period > slash){
				return testPath.equals("*." + requestPath.substring(period + 1));
			}
		}
		
		// Case 4 - "default" Match 
		return false;		//Note - Not relavant for selecting filters
	}

	/**
	 * Return <code>true</code> if the specified servlet name matches the requirements 
	 * of the specified filter mapping; otherwise return <code>false</code>
	 * 
	 * @param filterMap
	 * @param servletName
	 * @return
	 */
	private boolean matchFiltersServlet(FilterMap filterMap, String servletName) {
		
		if(servletName == null){
			return false;
		}

		return servletName.equals(filterMap.getServletName());
	}
	
	/**
	 * 
	 * @param message
	 */
	private void log(String message){
		Logger logger = null;
        if (container != null)
            logger = container.getLogger();
        if (logger != null)
            logger.log("StandardWrapperValve[" + container.getName() + "]: "
                       + message);
        else {
            String containerName = null;
            if (container != null)
                containerName = container.getName();
            System.out.println("StandardWrapperValve[" + containerName
                               + "]: " + message);
        }
	}
	
	/**
	 * 
	 * @param message
	 * @param throwable
	 */
	private void log(String message, Throwable throwable){
		Logger logger = null;
        if (container != null)
            logger = container.getLogger();
        if (logger != null)
            logger.log("StandardWrapperValve[" + container.getName() + "]: "
                       + message, throwable);
        else {
            String containerName = null;
            if (container != null)
                containerName = container.getName();
            System.out.println("StandardWrapperValve[" + containerName
                               + "]: " + message);
            System.out.println("" + throwable);
            throwable.printStackTrace(System.out);
        }
	}
	
	/**
	 * Handle the specified ServletException encountered while processing the specified Request
	 * to produce the specified Response. And exceptions  that occur during generation of the 
	 * exception report are logged and swallowed.
	 * 
	 * @param request
	 * @param response
	 * @param e
	 */
	private void exception(Request request, Response response, Throwable exception){
		ServletRequest sreq = request.getRequest();
		sreq.setAttribute(Globals.EXCEPTION_ATTR, exception);
		
		ServletResponse sresp = response.getResponse();
		if(sresp instanceof HttpServletResponse){
			((HttpServletResponse) sresp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	@Override
	public String getInfo() {
		return info;
	}

}
