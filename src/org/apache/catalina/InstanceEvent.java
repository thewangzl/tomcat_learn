package org.apache.catalina;

import java.util.EventObject;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * General event for notifying listeners of significant events related to a
 * specific instance of a Servlet, or a specific instance of a Filter, as
 * opposed to the Wrapper component that manages it.
 * 
 * @author thewangzl
 *
 */
@SuppressWarnings("serial")
public final class InstanceEvent extends EventObject {

	// ---------------------------------------Manifest Constants

	public static final String BEFORE_INIT_EVENT = "beforeInit";

	public static final String AFTER_INIT_EVENT = "afterInit";

	public static final String BEFORE_SERVICE_EVENT = "beforeService";

	public static final String AFTER_SERVICE_EVENT = "afterService";

	public static final String BEFORE_DESTROY_EVENT = "beforeDestroy";

	public static final String AFTER_DESTROY_EVENT = "afterDestroy";

	/**
	 * The event indicating that the <code>service()</code> method of a servlet
	 * accessed via a request dispatcher is about to be called. The
	 * <code>servlet</code> property contains a reference to the dispatched-to
	 * servlet instance, and the <code>request</code> and <code>response</code>
	 * properties contain the current request and response being processed. The
	 * <code>wrapper</code> property will contain a reference to the
	 * dispatched-to Wrapper.
	 */
	public static final String BEFORE_DISPATCH_EVENT = "beforeDispatch";

	public static final String AFTER_DISPATCH_EVENT = "afterDispatch";

	/**
	 * 
	 */
	public static final String BEFORE_FILTER_EVENT = "beforeFilter";

	public static final String AFTER_FILTER_EVENT = "afterFilter";

	// ---------------------------------------------------- Instance Variables

	private Throwable exception;

	private Filter filter;

	private ServletRequest request;

	private ServletResponse response;

	private Servlet servlet;

	private String type;

	private Wrapper wrapper;

	// -----------------------------------------------Constructors
	
	
	/**
	 * 
	 * @param wrapper Wrapper managing this servlet instance
	 * @param filter Filter instance for which this event occurred
	 * @param type Event type (required)
	 */
	public InstanceEvent(Wrapper wrapper, Filter filter, String type) {
		super(wrapper);
		this.wrapper = wrapper;
		this.filter = filter;
		this.type = type;
	}
	
	/**
	 * 
	 * @param wrapper Wrapper managing this servlet instance
	 * @param filter Filter instance for which this event occurred
	 * @param type Event type (required)
	 * @param exception Exception that occurred
	 */
	public InstanceEvent(Wrapper wrapper, Filter filter, String type, Throwable exception) {
		super(wrapper);
		this.wrapper = wrapper;
		this.filter = filter;
		this.type = type;
		this.exception = exception;
	}
	/**
	 * 
	 * @param wrapper Wrapper managing this servlet instance
	 * @param filter Filter instance for which this event occurred
	 * @param type Event type (required)
	 * @param request Servlet request we are processing
	 * @param response Servlet response we are processing
	 */
	public InstanceEvent(Wrapper wrapper, Filter filter, String type, ServletRequest request, ServletResponse response) {
		super(wrapper);
		this.wrapper = wrapper;
		this.filter = filter;
		this.type = type;
		this.request = request;
		this.response = response;
	}
	
	/**
	 * 
	 * @param wrapper Wrapper managing this servlet instance
	 * @param filter Filter instance for which this event occurred
	 * @param type Event type (required)
	 * @param request Servlet request we are processing
	 * @param response Servlet response we are processing
	 * @param exception
	 */
	public InstanceEvent(Wrapper wrapper, Filter filter, String type, ServletRequest request, ServletResponse response, Throwable exception) {
		super(wrapper);
		this.wrapper = wrapper;
		this.filter = filter;
		this.type = type;
		this.request = request;
		this.response = response;
		this.exception = exception;
	}
	
	/**
	 * 
	 * @param wrapper Wrapper managing this servlet instance
	 * @param filter Filter instance for which this event occurred
	 * @param type Event type (required)
	 */
	public InstanceEvent(Wrapper wrapper, Servlet servlet, String type) {
		super(wrapper);
		this.wrapper = wrapper;
		this.servlet = servlet;
		this.type = type;
	}
	
	/**
	 * 
	 * @param wrapper Wrapper managing this servlet instance
	 * @param filter Filter instance for which this event occurred
	 * @param type Event type (required)
	 * @param exception
	 */
	public InstanceEvent(Wrapper wrapper, Servlet servlet, String type, Throwable exception) {
		super(wrapper);
		this.wrapper = wrapper;
		this.servlet = servlet;
		this.type = type;
		this.exception = exception;
	}
	
	/**
	 * 
	 * @param wrapper Wrapper managing this servlet instance
	 * @param filter Filter instance for which this event occurred
	 * @param type Event type (required)
	 * @param request
	 * @param response
	 */
	public InstanceEvent(Wrapper wrapper, Servlet servlet, String type, ServletRequest request, ServletResponse response) {
		super(wrapper);
		this.wrapper = wrapper;
		this.servlet = servlet;
		this.type = type;
		this.request = request;
		this.response = response;
	}
	
	/**
	 * 
	 * @param wrapper Wrapper managing this servlet instance
	 * @param filter Filter instance for which this event occurred
	 * @param type Event type (required)
	 * @param request
	 * @param response
	 * @param exception
	 */
	public InstanceEvent(Wrapper wrapper, Servlet servlet, String type, ServletRequest request, ServletResponse response, Throwable exception) {
		super(wrapper);
		this.wrapper = wrapper;
		this.servlet = servlet;
		this.type = type;
		this.request = request;
		this.response = response;
		this.exception = exception;
	}
	
	
	// -------------------------------------- Properties

	public Throwable getException() {
		return exception;
	}

	public Filter getFilter() {
		return filter;
	}

	public ServletRequest getRequest() {
		return request;
	}

	public ServletResponse getResponse() {
		return response;
	}

	public Servlet getServlet() {
		return servlet;
	}

	public String getType() {
		return type;
	}

	public Wrapper getWrapper() {
		return wrapper;
	}

}
