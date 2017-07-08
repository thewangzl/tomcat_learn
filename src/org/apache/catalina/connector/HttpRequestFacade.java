package org.apache.catalina.connector;

import java.security.Principal;
import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.catalina.HttpRequest;

public final class HttpRequestFacade extends RequestFacade implements HttpServletRequest {

	public HttpRequestFacade(HttpRequest request) {
		super(request);
	}

	@Override
	public String getAuthType() {

		return ((HttpServletRequest)request).getAuthType();
	}

	@Override
	public String getContextPath() {
		return ((HttpServletRequest)request).getContextPath();
	}

	@Override
	public Cookie[] getCookies() {
		return ((HttpServletRequest)request).getCookies();
	}

	@Override
	public long getDateHeader(String name) {
		return ((HttpServletRequest)request).getDateHeader(name);
	}

	@Override
	public String getHeader(String name) {
		return ((HttpServletRequest)request).getHeader(name);
	}

	@Override
	public Enumeration<?> getHeaderNames() {
		return ((HttpServletRequest)request).getHeaderNames();
	}

	@Override
	public Enumeration<?> getHeaders(String name) {
		return ((HttpServletRequest)request).getHeaders(name);
	}

	@Override
	public int getIntHeader(String name) {
		return ((HttpServletRequest)request).getIntHeader(name);
	}

	@Override
	public String getMethod() {
		return ((HttpServletRequest)request).getMethod();
	}

	@Override
	public String getPathInfo() {
		return ((HttpServletRequest)request).getPathInfo();
	}

	@Override
	public String getPathTranslated() {
		return ((HttpServletRequest)request).getPathTranslated();
	}

	@Override
	public String getQueryString() {
		return ((HttpServletRequest)request).getQueryString();
	}

	@Override
	public String getRemoteUser() {
		return ((HttpServletRequest)request).getRemoteUser();
	}

	@Override
	public String getRequestURI() {
		return ((HttpServletRequest)request).getRequestURI();
	}

	@Override
	public StringBuffer getRequestURL() {
		return ((HttpServletRequest)request).getRequestURL();
	}

	@Override
	public String getRequestedSessionId() {
		return ((HttpServletRequest)request).getRequestedSessionId();
	}

	@Override
	public String getServletPath() {
		return ((HttpServletRequest)request).getServletPath();
	}

	@Override
	public HttpSession getSession() {
		return ((HttpServletRequest)request).getSession();
	}

	@Override
	public HttpSession getSession(boolean create) {
		return ((HttpServletRequest)request).getSession(create);
	}

	@Override
	public Principal getUserPrincipal() {
		return ((HttpServletRequest)request).getUserPrincipal();
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return ((HttpServletRequest)request).isRequestedSessionIdFromCookie();
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return ((HttpServletRequest)request).isRequestedSessionIdFromURL();
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return ((HttpServletRequest)request).isRequestedSessionIdFromUrl();
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return ((HttpServletRequest)request).isRequestedSessionIdValid();
	}

	@Override
	public boolean isUserInRole(String role) {
		return ((HttpServletRequest)request).isUserInRole(role);
	}

}
