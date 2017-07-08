package org.apache.catalina.connector.http;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.HttpResponseBase;

/**
 * Implementation of <b>HttpResponse</b> specific to the HTTP connector.
 * 
 * @author thewangzl
 *
 */
public final class HttpResponseImpl extends HttpResponseBase {

	protected static final String info = "org.apache.catalina.connector.http.HttpResponseImpl/1.0";
	
	/**
	 * True if chunking is allowed.
	 */
	protected boolean allowChunking;
	
	/**
	 * Associated HTTP response stream.
	 */
	protected HttpResponseStream responseStream;
	
	
	/**
	 * Create and return a ServletOutputStream to write the content 
	 * associated with The Response.
	 */
	@Override
	public ServletOutputStream createOutputStream() throws IOException {
		responseStream = new HttpResponseStream(this);
		return responseStream;
	}
	
	/**
	 * Has stream been created ?
	 * 
	 * @return
	 */
	public boolean isStreamInitialized(){
		return responseStream != null;
	}
	
	@Override
	public void finishResponse() throws IOException {
		
		if(getStatus() < HttpServletResponse.SC_BAD_REQUEST){
			if(!isStreamInitialized() && getContentLength() == -1 			//
					&& getStatus() >= 200 && getStatus() != SC_NOT_MODIFIED //
					&& getStatus() != SC_NO_CONTENT){
				setContentLength(0);
			}
				
		}else{
			setHeader("Connection", "close");
		}
		super.finishResponse();
	}
	public  String getInfo() {
		return info;
	}
	
	
	public void setAllowChunking(boolean allowChunking) {
		this.allowChunking = allowChunking;
	}
	
	public boolean isChunkingAllowed() {
		return allowChunking;
	}
	
	public String getProtocol() {
		return "HTTP/1.1";
	}
	
	@Override
	public void recycle() {
		
		super.recycle();
		responseStream = null;
		allowChunking = false;
		
	}
	
	public void sendError(int status, String message) throws IOException{
		addHeader("Connection", "close");
		super.sendError(status, message);
	}
	
	/**
	 * Clear any content written to the buffer. In addition, all cookies 
	 * and headers are cleared, and the status is reset.
	 */
	@Override
	public void reset() {
		//Saving important HTTP/1.1 specific headers
		String connectionValue = (String) getHeader("Connection");
		String transferEncodingValue = (String) getHeader("Transfer-Encoding");
		super.reset();
		if(connectionValue != null){
			addHeader("Connection", connectionValue);
		}
		if(transferEncodingValue != null){
			addHeader("Transfer-Encoding", transferEncodingValue);
		}
	}
	
	
	/**
	 * Tests is the connection will be closed after the processing of the request.
	 * 
	 * @return
	 */
	public boolean isCloseConnection(){
		String connectionValue = (String) getHeader("Connection");
		return connectionValue != null && connectionValue.equals("close");
	}
	
	/**
	 * Remove the specified header
	 * 
	 * @param name
	 * @param value
	 */
	public void removeHeader(String name, String value){
		if(isCommitted()){
			return;
		}
		if(included){
			return;			//Ignore any call from an included servlet
		}
		
		synchronized (headers) {
			ArrayList<String> values =  headers.get(name);
			if(values != null && !values.isEmpty()){
				values.remove(value);
				if(values.isEmpty()){
					headers.remove(name);
				}
			}
		}
	}
	
	/**
	 * Set the HTTP status to be returned with this response.
	 * 
	 * @param status the new HTTP status
	 */
	@Override
	public void setStatus(int status) {
		super.setStatus(status);
		if(responseStream != null){
			responseStream.checkChunking(this);
		}
	}
	
	@Override
	public void setContentLength(int length) {
		if(isCommitted()){
			return;
		}
		if(included){
			return;
		}
		super.setContentLength(length);
		
		if(responseStream != null){
			responseStream.checkChunking(this);
		}
	}
	
}
