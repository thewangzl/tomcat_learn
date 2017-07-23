package org.apache.catalina.authenticator;

public class Constants {

	public static final String Package = "org.apache.catalina.authenticator";

	public static final String BASIC_METHOD = "BASIC";
	public static final String FORM_METHOD = "FORM";

	public static final String NONE_TRANSPORT = "NONE";

	public static final String FORM_ACTION = "/j_security_check";
	
	public static final String SINGLE_SIGN_ON_COOKIE = "JSESSIONIDSSO";

	/**
	 * The notes key for the username used to authenticate this user.
	 */
	public static final String REQ_USERNAME_NOTE = "org.apache.catalina.request.USERNAME";

	/**
	 * The notes key for the password used to authenticate this user.
	 */
	public static final String REQ_PASSWORD_NOTE = "org.apache.catalina.request.PASSWORD";

	/**
	 * The notes key to track the single-sign-on identity with which this
	 * request is associated.
	 */
	public static final String REQ_SSOID_NOTE = "org.apache.catalina.request.SSOID";

	/**
	 * The notes key for the username used to authenticate this user.
	 */
	public static final String SESS_USERNAME_NOTE = "org.apache.catalina.session.USERNAME";

	/**
	 * The notes key for the password used to authenticate this user.
	 */
	public static final String SESS_PASSWORD_NOTE = "org.apache.catalina.session.PASSWORD";

	/**
	 * The previously authenticated principal (if caching is disabled).
	 */
	public static final String FORM_PRINCIPAL_NOTE = "org.apache.catalina.authenticator.PRINCIPAL";

	/**
	 * The original request information, to which the user will be redirected if
	 * authentication succeeds.
	 */
	public static final String FORM_REQUEST_NOTE = "org.apache.catalina.authenticator.REQUEST";
}
