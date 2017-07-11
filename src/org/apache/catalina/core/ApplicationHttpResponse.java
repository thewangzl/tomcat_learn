
package org.apache.catalina.core;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.catalina.util.StringManager;

public class ApplicationHttpResponse extends HttpServletResponseWrapper {

	
	private boolean included;
	
	protected static final String info = "org.apache.catalina.core.ApplicationHttpResponse/1.0";
	
	protected static final StringManager sm = StringManager.getManager(Constants.Package);
	
	public ApplicationHttpResponse(HttpServletResponse response) {
		this(response, false);
	}

	public ApplicationHttpResponse(HttpServletResponse response, boolean included) {
		super(response);

	}
	
	// ---------------------------------------------------------- ServletResponse Methods
	
	
	@Override
	public void reset() {
		
		//If already committed, the wrapper response will throw ISE
		if(!included || getResponse().isCommitted()){
			getResponse().reset();
		}
	}
	
	@Override
	public void setContentLength(int len) {
		if(!included){
			getResponse().setContentLength(len);
		}
	}
	
	@Override
	public void setContentType(String type) {
		if(!included){
			getResponse().setContentType(type);
		}
	}
	
	@Override
	public void setLocale(Locale loc) {
		if(!included){
			getResponse().setLocale(loc);
		}
	}
	
	// ------------------------------------   HttpServletResponse Methods
	
	
	@Override
	public void addCookie(Cookie cookie) {
		if(!included){
			((HttpServletResponse)getResponse()).addCookie(cookie);
		}
	}
	
	@Override
	public void addDateHeader(String name, long date) {
		if(!included){
			((HttpServletResponse) getResponse()).addDateHeader(name, date);
		}
	}
	
	@Override
	public void addHeader(String name, String value) {
		if(!included){
			((HttpServletResponse) getResponse()).addHeader(name, value);
		}
	}
	
	@Override
	public void addIntHeader(String name, int value) {
		if(!included){
			((HttpServletResponse) getResponse()).addIntHeader(name, value);
		}
	}
	
	@Override
	public void sendError(int sc) throws IOException {
		if(!included){
			((HttpServletResponse) getResponse()).sendError(sc);
		}
	}
	
	@Override
	public void sendError(int sc, String msg) throws IOException {
		if(!included){
			((HttpServletResponse) getResponse()).sendError(sc, msg);
		}
	}
	
	@Override
	public void sendRedirect(String location) throws IOException {
		if(!included){
			((HttpServletResponse) getResponse()).sendRedirect(location);
		}
	}
	
	@Override
	public void setDateHeader(String name, long date) {
		if(!included){
			((HttpServletResponse) getResponse()).setDateHeader(name, date);
		}
	}
	
	@Override
	public void setHeader(String name, String value) {
		if(!included){
			((HttpServletResponse) getResponse()).setHeader(name, value);
		}
	}
	
	@Override
	public void setIntHeader(String name, int value) {
		if(!included){
			((HttpServletResponse) getResponse()).setIntHeader(name, value);
		}
	}
	
	@Override
	public void setStatus(int sc) {
		if(!included){
			((HttpServletResponse) getResponse()).setStatus(sc);
		}
	}
	
	@Override
	@SuppressWarnings("deprecation")
	public void setStatus(int sc, String sm) {
		if(!included){
			((HttpServletResponse)getResponse()).setStatus(sc, sm);
		}
	}
	
	public String getInfo() {
		return info;
	}
	
	public boolean isIncluded() {
		return included;
	}
	
	public void setIncluded(boolean included) {
		this.included = included;
	}
	
	@Override
	public void setResponse(ServletResponse response) {
		super.setResponse(response);
	}

}
