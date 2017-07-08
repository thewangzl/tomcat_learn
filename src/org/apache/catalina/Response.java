package org.apache.catalina;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;

/**
 * A <b>Response</b> is the Catalina-internal facade for a <code>ServletResponse</code> 
 * that is to be produced, based on the processing of a corresponding <code>Request</code>.
 * @author lenovo
 *
 */
public interface Response {

	/**
	 * Return the Connector through which this response is returned.
	 * @return
	 */
	public Connector getConnector();
	
	public void setConnector(Connector connector);
	
	/**
	 * Return the number of bytes actually written to the output stream.
	 * 
	 * @return
	 */
	public int getContentCount();
	
	/**
	 * Return the context with which this Response is associated.
	 * 
	 * @return
	 */
	public Context getContext();
	
	public void setContext(Context context);
	
	/**
	 * Set the application commit flag
	 * 
	 * @param appCommitted
	 */
	public void setAppCommitted(boolean appCommitted);

	public boolean isAppCommitted();
	
	/**
	 * Return the "processing inside an include" flag
	 * @return
	 */
	public boolean getIncluded();
	
	public void setIncluded(boolean included);
	
	public String getInfo();
	
	/**
	 * Return the request with which this Response is associated.
	 * @return
	 */
	public Request getRequest();
	
	
	public void setRequest(Request request);
	
	/**
	 * Return the <code>ServletResponse</code> for which this object is the facade.
	 * 
	 * @return
	 */
	public ServletResponse getResponse();
	
	/**
	 * Return the output stream associated with this response.
	 *   	
	 * @return
	 */
	public OutputStream getStream();
	
	public void setStream(OutputStream stream);
	
	/**
	 * Set the suspended flag
	 * @param suspended
	 */
	public 	void setSuspended(boolean suspended);
	
	public boolean isSuspended();
	
	/**
	 * Set the error flag
	 */
	public void setError();
	
	public boolean isError();
	
	/**
	 * Create and return a ServletOutputStream to write the content associated with with Response
	 * 
	 * @return
	 * @throws IOException
	 */
	public ServletOutputStream createOutputStream() throws IOException;
	
	/**
	 * Perform whatever actions are required to flush and close the output stream or writer,
	 * in a single operation.
	 * 
	 * @throws IOException
	 */
	public void finishResponse() throws IOException;
	
	/**
	 * Return the content length that was set or calculated for this Response.
	 * 
	 * @return
	 */
	public int getContentLength();
	
	/**
	 * Return the content type that was set or calculated for this Response,
	 * or <code>null</code> if no content type was set.
	 * @return
	 */
	public String getContentType();
	
	/**
	 * Return a PrintWriter that can be used to render errorMessages, 
	 * regardless of whether a stream or writer has already been acquired.
	 * 
	 * @return
	 */
	public PrintWriter getReporter();
	
	/*
	 * Release all object references, and initialize instance variables,
	 * in prepaation for reuse of this object.
	 */
	public void recycle();
	
	/**
	 * Reset the data buffer but not any status or header information.
	 */
	public void resetBuffer();
	
	/**
	 * Send an acknowledgment of a request.
	 * @throws IOException
	 */
	public void sendAcknowledgment() throws IOException;

	byte[] getBuffer();
	
}
