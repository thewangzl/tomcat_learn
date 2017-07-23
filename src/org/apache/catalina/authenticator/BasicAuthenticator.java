package org.apache.catalina.authenticator;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.util.Base64;

public class BasicAuthenticator extends AuthenticatorBase {

	protected static final Base64 base64Helper = new Base64();

	protected static final String info = "org.apache.catalina.authenticator.BasicAuthenticator/1.0";

	@Override
	protected boolean authenticate(HttpRequest request, HttpResponse response, LoginConfig config) {

		// Have we already authenticated someone?
		Principal principal = ((HttpServletRequest) request.getRequest()).getUserPrincipal();
		if (principal != null) {
			if (debug >= 1)
				log("Already authenticated '" + principal.getName() + "'");
			return (true);
		}

		// Validate any credentials already included with this request
		HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
		HttpServletResponse hres = (HttpServletResponse) response.getResponse();
		String authorization = request.getAuthorization();
		String username = parseUsername(authorization);
		String password = parsePassword(authorization);
		principal = context.getRealm().authenticate(username, password);
		if (principal != null) {
			register(request, response, principal, Constants.BASIC_METHOD, username, password);
			return true;
		}

		// Send an "unauthorized" response and an appropriate challenge
		String realmName = config.getRealmName();
		if (realmName == null) {
			realmName = hreq.getServerName() + ":" + hreq.getServerPort();
		}
		hres.setHeader("WWW-Authenticate", "Basic realm=\"" + realmName + "\"");
		hres.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		return false;
	}

	@SuppressWarnings("static-access")
	private String parseUsername(String authorization) {

		if (authorization == null) {
			return null;
		}
		if (!authorization.toLowerCase().startsWith("basic ")) {
			return null;
		}
		authorization = authorization.substring(6).trim();

		// Decode and parse the authorization credentials
		String unencoded = new String(base64Helper.decode(authorization.getBytes()));
		int colon = unencoded.indexOf(":");
		if (colon < 0) {
			return null;
		}
		String username = unencoded.substring(0, colon).trim();
		return username;
	}

	@SuppressWarnings("static-access")
	private String parsePassword(String authorization) {
		if (authorization == null) {
			return null;
		}
		if (!authorization.startsWith("Basic ")) {
			return null;
		}
		authorization = authorization.substring(6).trim();

		// Decode and parse the authorization credentials
		String unencoded = new String(base64Helper.decode(authorization.getBytes()));
		int colon = unencoded.indexOf(':');
        if (colon < 0)
            return (null);
        String password = unencoded.substring(colon + 1).trim();
		return password;
	}
}
