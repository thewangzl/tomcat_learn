package org.apache.catalina.authenticator;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Session;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.valves.ValveBase;

public abstract class AuthenticatorBase extends ValveBase implements Authenticator, Lifecycle {

	protected static final String DEFAULT_ALGORITHM = "MD5";

	protected static final int SESSION_ID_BYTES = 16;

	protected String algorithm = DEFAULT_ALGORITHM;

	protected boolean cache = true;

	protected Context context;

	protected int debug = 0;

	protected MessageDigest digest;

	protected String entropy;

	protected Random random;

	protected String randomClass = "java.security.SecureRandom";

	protected SingleSignOn sso;

	protected boolean started = false;

	protected LifecycleSupport lifecycle = new LifecycleSupport(this);

	protected static final String info = "org.apache.catalina.authenticator.AuthenticatorBase/1.0";

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public boolean getCache() {
		return cache;
	}

	public void setCache(boolean cache) {
		this.cache = cache;
	}

	@Override
	public Container getContainer() {
		return this.context;
	}

	@Override
	public void setContainer(Container container) {
		if (!(container instanceof Context)) {
			throw new IllegalArgumentException(sm.getString("authenticator.notContext"));
		}
		super.setContainer(container);
		this.context = (Context) container;
	}

	@Override
	public int getDebug() {
		return this.debug;
	}

	@Override
	public void setDebug(int debug) {
		this.debug = debug;
	}

	public String getEntropy() {
		return entropy;
	}

	public void setEntropy(String entropy) {
		this.entropy = entropy;
	}

	public String getInfo() {
		return info;
	}

	public String getRandomClass() {
		return randomClass;
	}

	public void setRandomClass(String randomClass) {
		this.randomClass = randomClass;
	}

	// ---------------------------------------------------- Public Methods

	@Override
	public void invoke(Request request, Response response, ValveContext valveContext)
			throws IOException, ServletException {
		// If this is not an HTTP request, do nothing
		if (!(request instanceof HttpRequest) || !(response instanceof HttpResponse)) {
			valveContext.invokeNext(request, response);
			return;
		}
		if (!(request.getRequest() instanceof HttpServletRequest)
				|| !(response.getResponse() instanceof HttpServletResponse)) {
			valveContext.invokeNext(request, response);
			return;
		}
		HttpRequest hrequest = (HttpRequest) request;
		HttpResponse hresponse = (HttpResponse) response;
		if (debug >= 1)
			log("Security checking request " + ((HttpServletRequest) request.getRequest()).getMethod() + " "
					+ ((HttpServletRequest) request.getRequest()).getRequestURI());
		LoginConfig config = this.context.getLoginConfig();

		// Have we got a cached authenticated Principal to record?
		if (cache) {
			Principal principal = ((HttpServletRequest) request.getRequest()).getUserPrincipal();
			if (principal == null) {
				Session session = getSession(hrequest);
				if (session != null) {
					principal = session.getPrincipal();
					if (principal != null) {
						if (debug >= 1) {
							log("We have cached auth type " + session.getAuthType() + " for principal "
									+ session.getPrincipal());
						}
						hrequest.setAuthType(session.getAuthType());
						hrequest.setUserPrincipal(principal);
					}
				}
			}
		}

		//
		String contextPath = this.context.getPath();
		String requestURI = ((HttpServletRequest) hrequest).getRequestURI();
		if (requestURI.startsWith(contextPath) && requestURI.endsWith(Constants.FORM_ACTION)) {
			if (!authenticate(hrequest, hresponse, config)) {
				if (debug >= 1)
					log(" Failed authenticate() test");
				return;
			}
		}

		//
		SecurityConstraint constraint = findConstraint(hrequest);
		if (constraint == null) {
			if (debug >= 1)
				log(" Not subject to any constraint");
			valveContext.invokeNext(request, response);
			return;
		}

		if ((debug >= 1) && (constraint != null))
			log(" Subject to constraint " + constraint);

		//
		if (!(((HttpServletRequest) hrequest.getRequest()).isSecure())) {
			HttpServletResponse sresponse = (HttpServletResponse) response.getResponse();
			sresponse.setHeader("Pragma", "No-cache");
			sresponse.setHeader("Cache-Control", "no-cache");
			sresponse.setDateHeader("Expires", 1);
		}

		// Enforce any user data constraint for this security constraint
		if (debug >= 1)
			log(" Calling checkUserData()");
		if (!checkUserData(hrequest, hresponse, constraint)) {
			if (debug >= 1) {
				log("Failed checkUserData()");
			}
			return;
		}
		// Authenticate based upon the specified login configuration
		if (constraint.getAuthConstraint()) {
			if (debug >= 1) {
				log(" Calling authenticate()");
			}
			if (!authenticate(hrequest, hresponse, config)) {
				if (debug >= 1)
					log(" Failed authenticate() test");
				// ASSERT: Authenticator already set the appropriate
				// HTTP status code, so we do not have to do anything special
				return;
			}
		}

		// Perform access control based on the specified role(s)
		if (constraint.getAuthConstraint()) {
			if (debug >= 1)
				log(" Calling accessControl()");
			if (!accessControl(hrequest, hresponse, constraint)) {
				if (debug >= 1)
					log(" Failed accessControl() test");
				// ASSERT: AccessControl method has already set the appropriate
				// HTTP status code, so we do not have to do anything special
				return;
			}
		}

		// Any and all specified constraints have been satisfied
		if (debug >= 1)
			log(" Successfully passed all security constraints");
		valveContext.invokeNext(request, response);

	}

	// ------------------------------------ Protected Methods

	protected abstract boolean authenticate(HttpRequest request, HttpResponse response, LoginConfig config);

	/**
	 * 
	 * @param request
	 * @param response
	 * @param principal
	 * @param authType
	 * @param username
	 * @param password
	 */
	protected void register(HttpRequest request, HttpResponse response, Principal principal, String authType,
			String username, String password) {
		if (debug >= 1)
			log("Authenticated '" + principal.getName() + "' with type '" + authType + "'");

		// Cache the authentication information in our request
		request.setAuthType(authType);
		request.setUserPrincipal(principal);

		// Cache the authentication information in our session, if any.
		if (cache) {
			Session session = getSession(request, false);
			if (session != null) {
				session.setAuthType(authType);
				session.setPrincipal(principal);
				if (username != null) {
					session.setNote(Constants.SESS_USERNAME_NOTE, username);
				} else {
					session.removeNote(Constants.SESS_USERNAME_NOTE);
				}
				if (password != null) {
					session.setNote(Constants.SESS_PASSWORD_NOTE, password);
				} else {
					session.removeNote(Constants.SESS_PASSWORD_NOTE);
				}
			}
		}

		// Construct a cookie to be returned to be client
		if (sso == null) {
			return;
		}

		// HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
		HttpServletResponse hres = (HttpServletResponse) response.getResponse();
		String value = generateSessionId();

		Cookie cookie = new Cookie(Constants.SINGLE_SIGN_ON_COOKIE, value);
		cookie.setMaxAge(-1);
		cookie.setPath("/");
		hres.addCookie(cookie);

		// Register this principal with our SSO value
		sso.register(value, principal, authType, username, password);
		request.setNote(Constants.REQ_SSOID_NOTE, value);
	}

	protected Session getSession(HttpRequest request) {
		return getSession(request, false);
	}

	protected Session getSession(HttpRequest request, boolean create) {
		HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
		HttpSession hses = hreq.getSession(create);
		if (hses == null) {
			return null;
		}
		Manager manager = context.getManager();
		if (manager == null) {
			return null;
		} else {
			try {
				return manager.findSession(hses.getId());
			} catch (IOException e) {
				return null;
			}
		}
	}

	protected boolean accessControl(HttpRequest request, HttpResponse response, SecurityConstraint constraint)
			throws IOException {

		if (constraint == null)
			return (true);

		// Specifically allow access to the form login and form error pages
		// and the "j_security_check" action
		LoginConfig config = context.getLoginConfig();
		if ((config != null) && (Constants.FORM_METHOD.equals(config.getAuthMethod()))) {
			String requestURI = ((HttpServletRequest) request).getRequestURI();
			String loginPage = context.getPath() + config.getLoginPage();
			if (loginPage.equals(requestURI)) {
				if (debug >= 1)
					log(" Allow access to login page " + loginPage);
				return (true);
			}
			String errorPage = context.getPath() + config.getErrorPage();
			if (errorPage.equals(requestURI)) {
				if (debug >= 1)
					log(" Allow access to error page " + errorPage);
				return (true);
			}
			if (requestURI.endsWith(Constants.FORM_ACTION)) {
				if (debug >= 1)
					log(" Allow access to username/password submission");
				return (true);
			}
		}

		// Which user principal have we already authenticated?
		Principal principal = ((HttpServletRequest) request.getRequest()).getUserPrincipal();
		if (principal == null) {
			if (debug >= 2)
				log("  No user authenticated, cannot grant access");
			((HttpServletResponse) response.getResponse()).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					sm.getString("authenticator.notAuthenticated"));
			return (false);
		}

		// Check each role included in this constraint
		Realm realm = context.getRealm();
		String roles[] = constraint.findAuthRoles();
		if (roles == null)
			roles = new String[0];

		if (constraint.getAllRoles())
			return (true);
		if ((roles.length == 0) && (constraint.getAuthConstraint())) {
			((HttpServletResponse) response.getResponse()).sendError(HttpServletResponse.SC_FORBIDDEN,
					sm.getString("authenticator.forbidden"));
			return (false); // No listed roles means no access at all
		}
		for (int i = 0; i < roles.length; i++) {
			if (realm.hasRole(principal, roles[i]))
				return (true);
		}

		// Return a "Forbidden" message denying access to this resource
		((HttpServletResponse) response.getResponse()).sendError(HttpServletResponse.SC_FORBIDDEN,
				sm.getString("authenticator.forbidden"));
		return (false);

	}

	protected boolean checkUserData(HttpRequest request, HttpResponse response, SecurityConstraint constraint)
			throws IOException {
		// Is there a relevant user data constraint?
		if (constraint == null) {
			if (debug >= 2)
				log("  No applicable security constraint defined");
			return (true);
		}
		String userConstraint = constraint.getUserConstraint();
		if (userConstraint == null) {
			if (debug >= 2)
				log("  No applicable user data constraint defined");
			return (true);
		}
		if (userConstraint.equals(Constants.NONE_TRANSPORT)) {
			if (debug >= 2)
				log("  User data constraint has no restrictions");
			return (true);
		}

		// Validate the request against the user data constraint
		if (request.getRequest().isSecure()) {
			if (debug >= 2)
				log("  User data constraint already satisfied");
			return (true);
		}

		// Initialize variables we need to determine the appropriate action
		HttpServletRequest hrequest = (HttpServletRequest) request.getRequest();
		HttpServletResponse hresponse = (HttpServletResponse) response.getResponse();
		int redirectPort = request.getConnector().getRedirectPort();

		// Is redirecting disabled?
		if (redirectPort <= 0) {
			if (debug >= 2)
				log("  SSL redirect is disabled");
			hresponse.sendError(HttpServletResponse.SC_FORBIDDEN, hrequest.getRequestURI());
			return (false);
		}

		// Redirect to the corresponding SSL port
		String protocol = "https";
		String host = hrequest.getServerName();
		StringBuffer file = new StringBuffer(hrequest.getRequestURI());
		String requestedSessionId = hrequest.getRequestedSessionId();
		if ((requestedSessionId != null) && hrequest.isRequestedSessionIdFromURL()) {
			file.append(";jsessionid=");
			file.append(requestedSessionId);
		}
		String queryString = hrequest.getQueryString();
		if (queryString != null) {
			file.append('?');
			file.append(queryString);
		}
		URL url = null;
		try {
			url = new URL(protocol, host, redirectPort, file.toString());
			if (debug >= 2)
				log("  Redirecting to " + url.toString());
			hresponse.sendRedirect(url.toString());
			return (false);
		} catch (MalformedURLException e) {
			if (debug >= 2)
				log("  Cannot create new URL", e);
			hresponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, hrequest.getRequestURI());
			return (false);
		}
	}

	/**
	 * 
	 * @param request
	 * @return
	 */
	protected SecurityConstraint findConstraint(HttpRequest request) {

		SecurityConstraint[] constraints = context.findConstraints();
		if (constraints == null || constraints.length == 0) {
			if (debug >= 2) {
				log(" No applicable constraints defined");
			}
			return null;
		}

		// Check each defined security constraint
		HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
		String uri = hreq.getRequestURI();
		String contextPath = hreq.getContextPath();
		if (contextPath.length() > 0) {
			uri = uri.substring(contextPath.length());
		}
		String method = hreq.getMethod();
		for (int i = 0; i < constraints.length; i++) {
			if (debug >= 2) {
				log("  Checking constraint '" + constraints[i] + "' against " + method + " " + uri + " --> "
						+ constraints[i].included(uri, method));
			}
			if (constraints[i].included(uri, method)) {
				return constraints[i];
			}
		}
		if (debug >= 2) {
			log("  No applicable constraint located");
		}
		return null;
	}

	protected synchronized String generateSessionId() {

		// Generate a byte array containing a session identifier
		byte bytes[] = new byte[SESSION_ID_BYTES];
		getRandom().nextBytes(bytes);
		bytes = getDigest().digest(bytes);

		// Render the result as a String of hexadecimal digits
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			byte b1 = (byte) ((bytes[i] & 0xf0) >> 4);
			byte b2 = (byte) (bytes[i] & 0x0f);
			if (b1 < 10)
				result.append((char) ('0' + b1));
			else
				result.append((char) ('A' + (b1 - 10)));
			if (b2 < 10)
				result.append((char) ('0' + b2));
			else
				result.append((char) ('A' + (b2 - 10)));
		}
		return (result.toString());
	}

	/**
	 * Return the MessageDigest object to be used for calculating session
	 * identifiers. If none has been created yet, initialize one the first time
	 * this method is called.
	 */
	protected synchronized MessageDigest getDigest() {

		if (this.digest == null) {
			try {
				this.digest = MessageDigest.getInstance(algorithm);
			} catch (NoSuchAlgorithmException e) {
				try {
					this.digest = MessageDigest.getInstance(DEFAULT_ALGORITHM);
				} catch (NoSuchAlgorithmException f) {
					this.digest = null;
				}
			}
		}

		return (this.digest);

	}

	/**
	 * Return the random number generator instance we should use for generating
	 * session identifiers. If there is no such generator currently defined,
	 * construct and seed a new one.
	 */
	protected synchronized Random getRandom() {

		if (this.random == null) {
			try {
				Class<?> clazz = Class.forName(randomClass);
				this.random = (Random) clazz.newInstance();
				long seed = System.currentTimeMillis();
				char entropy[] = getEntropy().toCharArray();
				for (int i = 0; i < entropy.length; i++) {
					long update = ((byte) entropy[i]) << ((i % 8) * 8);
					seed ^= update;
				}
				this.random.setSeed(seed);
			} catch (Exception e) {
				this.random = new java.util.Random();
			}
		}

		return (this.random);

	}

	/**
	 * Log a message on the Logger associated with our Container (if any).
	 *
	 * @param message
	 *            Message to be logged
	 */
	protected void log(String message) {

		Logger logger = context.getLogger();
		if (logger != null)
			logger.log("Authenticator[" + context.getPath() + "]: " + message);
		else
			System.out.println("Authenticator[" + context.getPath() + "]: " + message);

	}

	/**
	 * Log a message on the Logger associated with our Container (if any).
	 *
	 * @param message
	 *            Message to be logged
	 * @param throwable
	 *            Associated exception
	 */
	protected void log(String message, Throwable throwable) {

		Logger logger = context.getLogger();
		if (logger != null)
			logger.log("Authenticator[" + context.getPath() + "]: " + message, throwable);
		else {
			System.out.println("Authenticator[" + context.getPath() + "]: " + message);
			throwable.printStackTrace(System.out);
		}

	}

	// ------------------------------------ Lifecycle Methods

	@Override
	public void addLifecycleListener(LifecycleListener listener) {

		lifecycle.addLifecycleListener(listener);

	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		return lifecycle.findLifecycleListeners();
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {

		lifecycle.removeLifecycleListener(listener);
	}

	@Override
	public void start() throws LifecycleException {
		// Validate and update our current component state
		if (started)
			throw new LifecycleException(sm.getString("authenticator.alreadyStarted"));
		lifecycle.fireLifecycleEvent(START_EVENT, null);
		if ("org.apache.catalina.core.StandardContext".equals(context.getClass().getName())) {
			try {
				Class<?>[] paramTypes = new Class[0];
				Object[] paramValues = new Object[0];
				Method method = context.getClass().getMethod("getDebug", paramTypes);
				Integer result = (Integer) method.invoke(context, paramValues);
				setDebug(result.intValue());
			} catch (Exception e) {
				log("Exception getting debug value", e);
			}
		}
		started = true;

		// Look up the SinggleSignOn implementation in our request processing
		// path, if there is one
		Container parent = context.getParent();
		while (sso == null && parent != null) {
			if (!(parent instanceof Pipeline)) {
				parent = parent.getParent();
				continue;
			}
			Valve[] valves = ((Pipeline) parent).getValves();
			for (int i = 0; i < valves.length; i++) {
				if (valves[i] instanceof SingleSignOn) {
					sso = (SingleSignOn) valves[i];
					break;
				}
			}
			if (sso == null) {
				parent = parent.getParent();
			}
		}
		if (debug >= 1) {
			if (sso != null)
				log("Found SingleSignOn Valve at " + sso);
			else
				log("No SingleSignOn Valve is present");
		}
	}

	@Override
	public void stop() throws LifecycleException {
		// Validate and update our current component state
		if (!started)
			throw new LifecycleException(sm.getString("authenticator.notStarted"));
		lifecycle.fireLifecycleEvent(STOP_EVENT, null);
		started = false;

		sso = null;
	}

}
