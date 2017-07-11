package org.apache.catalina.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Response;
import org.apache.catalina.Wrapper;

public class MappingRequest implements HttpRequest, HttpServletRequest {

	protected String contextPath;

	protected String decodeURI;

	protected String queryString;

	protected String pathInfo;

	protected String servletPath;

	protected Wrapper wrapper;

	public MappingRequest(String contextPath, String decodeURI, String queryString) {
		this.contextPath = contextPath;
		this.decodeURI = decodeURI;
		this.queryString = queryString;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
//		this.contextPath = contextPath;
	}

	public String getDecodedRequestURI() {
		return decodeURI;
	}

	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public String getPathInfo() {
		return pathInfo;
	}

	public void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}

	public String getServletPath() {
		return servletPath;
	}

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	public Wrapper getWrapper() {
		return wrapper;
	}

	public void setWrapper(Wrapper wrapper) {
		this.wrapper = wrapper;
	}

	
	@Override
	public String getAuthorization() {
		return null;
	}

	@Override
	public void setAuthorization(String authorization) {
	}

	@Override
	public Connector getConnector() {
		return null;
	}

	@Override
	public void setConnector(Connector connector) {

	}

	@Override
	public Context getContext() {
		return null;
	}

	@Override
	public void setContext(Context context) {
	}

	@Override
	public String getInfo() {
		return null;
	}

	@Override
	public ServletRequest getRequest() {
		return this;
	}

	@Override
	public Response getResponse() {
		return null;
	}

	@Override
	public void setResponse(Response response) {
	}

	@Override
	public Socket getSocket() {
		return null;
	}

	@Override
	public void setSocket(Socket socket) {

	}

	@Override
	public InputStream getStream() {
		return null;
	}

	@Override
	public void setStream(InputStream stream) {

	}

	@Override
	public ServletInputStream createInputStream() throws IOException {
		return null;
	}

	@Override
	public void finishRequest() throws IOException {

	}

	@Override
	public Object getNote(String name) {
		return null;
	}

	@Override
	public Iterator<String> getNoteNames() {
		return null;
	}

	@Override
	public void removeNote(String name) {

	}

	@Override
	public void setNote(String name, Object note) {

	}

	@Override
	public void recycle() {
	}

	@Override
	public void setContentLength(int length) {

	}

	@Override
	public void setContentType(String type) {
	}

	@Override
	public void setProtocol(String protocol) {

	}

	@Override
	public void setRemoteAddr(String remote) {

	}

	@Override
	public void setScheme(String scheme) {

	}

	@Override
	public void setSecure(boolean secure) {
	}

	@Override
	public void setServerName(String name) {

	}

	@Override
	public void setServerPort(int port) {

	}

	@Override
	public Object getAttribute(String arg0) {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getAttributeNames() {
		return null;
	}

	@Override
	public String getCharacterEncoding() {
		return null;
	}

	@Override
	public int getContentLength() {
		return -1;
	}

	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public Locale getLocale() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getLocales() {
		return null;
	}

	@Override
	public String getParameter(String arg0) {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map getParameterMap() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getParameterNames() {
		return null;
	}

	@Override
	public String[] getParameterValues(String arg0) {
		return null;
	}

	@Override
	public String getProtocol() {
		return null;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return null;
	}

	@Override
	public String getRealPath(String arg0) {
		return null;
	}

	@Override
	public String getRemoteAddr() {
		return null;
	}

	@Override
	public String getRemoteHost() {
		return null;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		return null;
	}

	@Override
	public String getScheme() {
		return null;
	}

	@Override
	public String getServerName() {
		return null;
	}

	@Override
	public int getServerPort() {
		return 0;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public void removeAttribute(String arg0) {
		

	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		

	}

	@Override
	public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
		

	}

	@Override
	public String getAuthType() {
		
		return null;
	}

	@Override
	public Cookie[] getCookies() {
		
		return null;
	}

	@Override
	public long getDateHeader(String arg0) {
		
		return 0;
	}

	@Override
	public String getHeader(String arg0) {
		
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getHeaderNames() {
		
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getHeaders(String arg0) {
		
		return null;
	}

	@Override
	public int getIntHeader(String arg0) {
		
		return 0;
	}

	@Override
	public String getMethod() {
		
		return null;
	}

	@Override
	public String getPathTranslated() {
		
		return null;
	}

	@Override
	public String getRemoteUser() {
		
		return null;
	}

	@Override
	public String getRequestURI() {
		
		return null;
	}

	@Override
	public StringBuffer getRequestURL() {
		
		return null;
	}

	@Override
	public String getRequestedSessionId() {
		
		return null;
	}

	@Override
	public HttpSession getSession() {
		
		return null;
	}

	@Override
	public HttpSession getSession(boolean arg0) {
		
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		
		return null;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		
		return false;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		
		return false;
	}

	@Override
	public boolean isUserInRole(String arg0) {
		
		return false;
	}

	@Override
	public void addCookie(Cookie cookie) {
		

	}

	@Override
	public void addHeader(String name, String value) {
		

	}

	@Override
	public void addLocale(Locale locale) {
		

	}

	@Override
	public void addParameter(String name, String[] values) {
		

	}

	@Override
	public void clearCookies() {
		

	}

	@Override
	public void clearHeaders() {
		

	}

	@Override
	public void clearLocales() {
		

	}

	@Override
	public void clearParameters() {
		

	}

	@Override
	public void setAuthType(String type) {
		

	}

	@Override
	public void setMethod(String method) {
		

	}

	@Override
	public void setRequestedSessionCookie(boolean flag) {
		

	}

	@Override
	public void setRequestedSessionId(String id) {
		

	}

	@Override
	public void setRequestedSessionURL(boolean flag) {
		

	}

	@Override
	public void setRequestURI(String uri) {
		

	}

	@Override
	public void setDecodedRequestURI(String uri) {
		

	}

	@Override
	public void setUserPrincipal(Principal principal) {
		

	}

}
