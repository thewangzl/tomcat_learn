package org.apache.catalina.core;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.InstanceEvent;
import org.apache.catalina.util.InstanceSupport;
import org.apache.catalina.util.StringManager;

/**
 * Implementation of <code>javax.servlet.FilterChain</code> usered to manage the
 * execution of a set of filters for a particular request. When the set of
 * defined filters has all been executed, the next call to
 * <code>doFilter()</code> will execute the servlet's <code>service()</code>
 * method itself.
 * 
 * @author thewangzl
 *
 */
public final class ApplicationFilterChain implements FilterChain {

	/**
	 * The set of filters that will be executed on the chain.
	 */
	private ArrayList<ApplicationFilterConfig> filters = new ArrayList<>();

	/**
	 * The iterator that is used to manitain the current position in the filter
	 * chain. The iterator is called the first time that <code>doFilter()</code>
	 * is called.
	 */
	private Iterator<ApplicationFilterConfig> iterator = null;

	/**
	 * The servlet instance to be executed by this chain.
	 */
	private Servlet servlet;

	private InstanceSupport support;

	private static final StringManager sm = StringManager.getManager(Constants.Package);

	/**
	 * @param request
	 * @param response
	 * 
	 * @throws IOException
	 * @throws ServletException
	 * 
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {

		if (System.getSecurityManager() != null) {
			final ServletRequest req = request;
			final ServletResponse resp = response;
			AccessController.doPrivileged(new PrivilegedAction<Object>() {

				@Override
				public Object run(){
					try {
						// FIXME -- now exception cannot be throwed 
						internalDoFilter(req, resp);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ServletException e) {
						e.printStackTrace();
					}
					return null;
				}
			});
		} else {
			internalDoFilter(request, response);
		}

	}

	private void internalDoFilter(ServletRequest request, ServletResponse response)
			throws IOException, ServletException {

		// Construct an iterator the first time this method is called
		if (this.iterator == null) {
			this.iterator = filters.iterator();
		}

		// Call the next filter if there is one
		if (this.iterator.hasNext()) {
			ApplicationFilterConfig filterConfig = iterator.next();
			Filter filter = null;

			try {
				filter = filterConfig.getFilter();
				support.fireInstanceEvent(InstanceEvent.BEFORE_FILTER_EVENT, filter, request, response);
				filter.doFilter(request, response, this);
				support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT, filter, request, response);
			} catch (IOException e) {
				if (filter != null) {
					support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT, filter, request, response, e);
				}
				throw e;
			}  catch (ServletException e) {
				if (filter != null) {
					support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT, filter, request, response, e);
				}
				throw e;
			} catch (RuntimeException e) {
				if (filter != null) {
					support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT, filter, request, response, e);
				}
				throw e;
			} catch (Throwable e) {
				if (filter != null) {
					support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT, filter, request, response, e);
				}
				throw new ServletException(sm.getString("filterChain.filter"), e);
			} 
			return;
		}
		
		//We fell off the end of the chain -- call the servlet instance
		
		try {
			support.fireInstanceEvent(InstanceEvent.BEFORE_SERVICE_EVENT, servlet, request, response);
			if(request instanceof HttpServletRequest && response instanceof HttpServletResponse ){
				servlet.service((HttpServletRequest) request, (HttpServletResponse)response);
			}else{
				servlet.service(request, response);
			}
			support.fireInstanceEvent(InstanceEvent. AFTER_SERVICE_EVENT, servlet, request, response);

		} catch (IOException e) {
			support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT, servlet, request, response, e);
			throw e;
		}  catch (ServletException e) {
			support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT, servlet, request, response, e);
			throw e;
		} catch (RuntimeException e) {
			support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT, servlet, request, response, e);
			throw e;
		} catch (Throwable e) {
			support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT, servlet, request, response, e);
			throw new ServletException(sm.getString("filterChain.servlet"), e);
		} 
	
	}

	// -------------------------------------------- Package Methods

	void addFilter(ApplicationFilterConfig filterConfig) {
		this.filters.add(filterConfig);
	}

	void setServlet(Servlet servlet) {
		this.servlet = servlet;
	}

	void setSupport(InstanceSupport support) {
		this.support = support;
	}

	void release() {
		this.filters.clear();
		this.iterator = null;
		this.servlet = null;
	}
}
