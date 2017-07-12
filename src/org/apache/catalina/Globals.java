package org.apache.catalina;

public class Globals {


	public static final String SESSION_COOKIE_NAME = "JSESSIONID";

	public static final String SESSION_PARAMETER_NAME = "jsessionid";
	
	
	/**
	 * The servlet context attribute under which we store a temporary working directory ( as an object
	 * of type File) for use by serlvets within this web application.
	 * 
	 */
	public static final String WORK_DIR_ATTR = "javax.servlet.context.tempdir";

	/**
	 * The servlet context attribute under which we store the class path for our application class loader
	 * (as an object of type String),delimited with the appropriate path delimiter for this platform.
	 */
	public static final String CLASS_PATH_ATTR = "org.apache.catalina.jsp_classpath";

	
	public static final String JSP_FILE_ATTR = "org.apache.catalina.jsp_file";

	/**
	 * The request attribute under which we forward a Java exception
	 * (as an object of type Throwable) to an error page.
	 */
	public static final String EXCEPTION_ATTR = "javax.servlet.error.exception";

	/**
	 * The JNDI directory contex which is associted with the context.
	 * This context can be used to manipulate static files.
	 */
	public static final String RESOURCES_ATTR = "org.apache.catalina.resources";
	
	
	/**
	 * The request attribute under which the original request URI is stored on an included dispatcher request.
	 */
	public static final String REQUEST_URI_ATTR = "javax.servlet.include.request_uri";
	
	/**
	 * The request attribute under which the original context path is stored on an 
	 * included dispatcher request.
	 */
	public static final String CONTEXT_PATH_ATTR = "javax.servlet.include.context_path";
	
	/**
	 * The request attribute under which the original servlet path is stored
	 * on a included dispatcher request.
	 */
	public static final String SERVLET_PATH_ATTR = "javax.servlet.include.servlet_path";
	
	/**
	 * The request attribute under which the original path info is stored
	 * on a included dispatcher request.
	 */
	public static final String PATH_INFO_ATTR = "javax.servlet.include.path_info";
	
	/**
	 * The request attribute under which the original query string is stored
	 * on a included dispatcher request.
	 */
	public static final String QUERY_STRING_ATTR = "javax.servlet.include.query_string";

	/**
	 * The request attribute under which we store the servlet name on named dispatcher request
	 */
	public static final String NAMED_DISPATCHER_ATTR = "org.apache.catalina.NAMED";

	public static final String STATUS_CODE_ATTR = null;

	public static final String ERROR_MESSAGE_ATTR = null;

	public static final String SERVLET_NAME_ATTR = null;

	public static final String EXCEPTION_PAGE_ATTR = null;
	
	
	
	
	
	
	
	
	
	
}
