package org.apache.catalina.core;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class ApplicationContextFacade implements ServletContext {

	/**
	 * Wrapper application context.
	 */
	private ApplicationContext context;
	
	public ApplicationContextFacade(ApplicationContext context) {
		this.context = context;
	}

	// -------------------------------------- ServletContext Methods
	
	@Override
	public Object getAttribute(String name) {
		return context.getAttribute(name);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Enumeration getAttributeNames() {

		return context.getAttributeNames();
	}

	@Override
	public ServletContext getContext(String uripath) {
		ServletContext theContext = this.context.getContext(uripath);
		if(theContext != null && theContext instanceof ApplicationContext){
			theContext = ((ApplicationContext) theContext).getFacade();
		}
		return theContext;
	}

	@Override
	public String getInitParameter(String name) {
		
		return context.getInitParameter(name);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Enumeration getInitParameterNames() {
		return context.getInitParameterNames();
	}

	@Override
	public int getMajorVersion() {
		return context.getMajorVersion();
	}

	@Override
	public String getMimeType(String file) {
		return context.getMimeType(file);
	}

	@Override
	public int getMinorVersion() {

		return context.getMinorVersion();
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String name) {

		return context.getNamedDispatcher(name);
	}

	@Override
	public String getRealPath(String path) {

		return context.getRealPath(path);
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {

		return context.getRequestDispatcher(path);
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		return context.getResource(path);
	}

	@Override
	public InputStream getResourceAsStream(String path) {

		return context.getResourceAsStream(path);
	}

	@Override
	public Set<String> getResourcePaths(String path) {

		return context.getResourcePaths(path);
	}

	@Override
	public String getServerInfo() {

		return context.getServerInfo();
	}

	@SuppressWarnings("deprecation")
	@Override
	public Servlet getServlet(String name) throws ServletException {

		return context.getServlet(name);
	}

	@Override
	public String getServletContextName() {

		return context.getServletContextName();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Enumeration getServletNames() {

		return context.getServletNames();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Enumeration getServlets() {
		return context.getServlets();
	}

	@Override
	public void log(String message) {
		context.log(message);
	}

	@Override
	public void log(Exception exception, String message) {
		context.log(exception, message);
	}

	@Override
	public void log(String message, Throwable throwable) {
		context.log(message, throwable);
	}

	@Override
	public void removeAttribute(String name) {
		context.removeAttribute(name);
	}

	@Override
	public void setAttribute(String name, Object value) {
		context.setAttribute(name, value);
	}

}
