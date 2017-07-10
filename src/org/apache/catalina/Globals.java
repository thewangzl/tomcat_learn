package org.apache.catalina;

public class Globals {

	/**
	 * The request attribute under which the original servlet path is stored
	 * on a included dispatcher request.
	 */
	public static final String SERVLET_PATH_ATTR = "javax.servlet.include.servlet_path";

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
}
