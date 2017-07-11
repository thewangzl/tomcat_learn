package org.apache.catalina.connector;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.HttpResponse;

/**
 * Facade class that wraps a Catalina-internal <b>HttpResponse</b> object.
 * All method are delegated to the wrapped response.
 * 
 * @author thewangzl
 *
 */
public final class HttpResponseFacade extends ResponseFacade implements HttpServletResponse {

	public HttpResponseFacade(HttpResponse response) {
		super(response);
	}

	@Override
	public void addCookie(Cookie cookie) {
		if(isCommitted()){
			return;
		}
		
		((HttpServletResponse) servletResponse).addCookie(cookie);
	}

	@Override
	public void addDateHeader(String name, long date) {
		if(isCommitted()){
			return;
		}
		
		((HttpServletResponse) servletResponse).addDateHeader(name, date);
	}

	@Override
	public void addHeader(String name, String value) {
		if(isCommitted()){
			return;
		}
		
		((HttpServletResponse) servletResponse).addHeader(name, value);
	}

	@Override
	public void addIntHeader(String name, int value) {
		if(isCommitted()){
			return;
		}
		
		((HttpServletResponse) servletResponse).addIntHeader(name, value);
	}

	@Override
	public boolean containsHeader(String name) {
		return ((HttpServletResponse) servletResponse).containsHeader(name);
	}

	@Override
	public String encodeRedirectURL(String url) {
		return ((HttpServletResponse) servletResponse).encodeRedirectURL(url);
	}

	@Override
	@SuppressWarnings("deprecation")
	public String encodeRedirectUrl(String url) {
		return ((HttpServletResponse) servletResponse).encodeRedirectUrl(url);
	}

	@Override
	public String encodeURL(String url) {
		return ((HttpServletResponse) servletResponse).encodeURL(url);
	}

	@Override
	@SuppressWarnings("deprecation")
	public String encodeUrl(String url) {
		return ((HttpServletResponse) servletResponse).encodeUrl(url);
	}

	@Override
	public void sendError(int sc) throws IOException {
		if(isCommitted()){
			throw new IllegalStateException("");
		}
		response.setAppCommitted(true);
		
		((HttpServletResponse) servletResponse).sendError(sc);
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		if(isCommitted()){
			throw new IllegalStateException("");
		}
		response.setAppCommitted(true);
		
		((HttpServletResponse) servletResponse).sendError(sc, msg);
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		if(isCommitted()){
			throw new IllegalStateException("");
		}
		response.setAppCommitted(true);
		
		((HttpServletResponse) servletResponse).sendRedirect(location);
	}

	@Override
	public void setDateHeader(String name, long date) {
		if(isCommitted()){
			return;
		}
		
		((HttpServletResponse) servletResponse).setDateHeader(name, date);

	}

	@Override
	public void setHeader(String name, String value) {
		if(isCommitted()){
			return;
		}
		
		((HttpServletResponse) servletResponse).setHeader(name, value);
	}

	@Override
	public void setIntHeader(String name, int value) {
		if(isCommitted()){
			return;
		}
		
		((HttpServletResponse) servletResponse).setIntHeader(name, value);
	}

	@Override
	public void setStatus(int sc) {
		if(isCommitted()){
			return;
		}
		
		((HttpServletResponse) servletResponse).setStatus(sc);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setStatus(int sc, String msg) {
		if(isCommitted()){
			return;
		}
		
		((HttpServletResponse) servletResponse).setStatus(sc, msg);

	}

}
