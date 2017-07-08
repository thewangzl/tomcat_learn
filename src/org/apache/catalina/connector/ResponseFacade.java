package org.apache.catalina.connector;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;

import org.apache.catalina.Response;

/**
 * Facade class that wraps a Catalina-internal <b>Response</b> object.
 * All methods are delegated to the wrapped response.
 * @author thewangzl
 *
 */
public class ResponseFacade implements ServletResponse {

	/**
	 * The wrapped response
	 */
	protected Response response;
	
	/**
	 * The wrapped response
	 */
	protected ServletResponse servletResponse;
	
	
	public ResponseFacade(Response response) {
		this.response = response;
		this.servletResponse = (ServletResponse) response;
	}
	
	/**
	 * clear facade
	 */
	public void clear(){
		this.response = null;
		this.servletResponse = null;
	}
	
	public void finish(){
		response.setSuspended(true);
	}
	
	public boolean isFinished(){
		return this.response.isSuspended();
	}

	@Override
	public void flushBuffer() throws IOException {
		
		if(this.isFinished()){
			return;
		}
		
		response.setAppCommitted(true);
		servletResponse.flushBuffer();
	}

	@Override
	public int getBufferSize() {
		return servletResponse.getBufferSize();
	}

	@Override
	public String getCharacterEncoding() {
		return this.servletResponse.getCharacterEncoding();
	}

	@Override
	public Locale getLocale() {

		return this.servletResponse.getLocale();
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {

		ServletOutputStream sos = this.servletResponse.getOutputStream();
		if(this.isFinished()){
			this.response.setSuspended(true);
		}
		return sos;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		PrintWriter writer = this.servletResponse.getWriter();
		if(this.isFinished()){
			this.response.setSuspended(true);
		}
		return writer;
	}

	@Override
	public boolean isCommitted() {

		return this.response.isAppCommitted();
	}

	@Override
	public void reset() {
		if(this.isCommitted()){
			throw new IllegalStateException("responseFacade.reset");
		}
		this.servletResponse.reset();
	}

	@Override
	public void resetBuffer() {
		if(this.isCommitted()){
			throw new IllegalStateException("responseFacade.resetBuffer");
		}
		this.servletResponse.resetBuffer();
	}

	@Override
	public void setBufferSize(int size) {
		if(this.isCommitted()){
			throw new IllegalStateException("responseFacade.setBufferSize");
		}
		this.servletResponse.setBufferSize(size);
	}

	@Override
	public void setContentLength(int length) {
		if(this.isCommitted()){
			throw new IllegalStateException("responseFacade.setContentLength");
		}
		this.servletResponse.setContentLength(length);
	}

	@Override
	public void setContentType(String type) {
		if(this.isCommitted()){
			throw new IllegalStateException("responseFacade.setContentType");
		}
		this.servletResponse.setContentType(type);
	}

	@Override
	public void setLocale(Locale locale) {
		this.servletResponse.setLocale(locale);
	}

}
