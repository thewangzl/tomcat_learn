package org.apache.catalina.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.util.RequestUtil;

/**
 * Convenience base implementation of the <b>Response</b> interface, which can be used for
 * the Response implementation required by most Connectors. Only the connector-specific
 * methods need to be implemented.
 * 
 * @author thewangzl
 *
 */
public abstract class ResponseBase implements ServletResponse, Response {

	/**
	 * Ha this response been committed by the application yet?
	 */
	protected boolean appCommitted;
	
	/**
	 * The buffer through which all of our output bytes are passed.
	 */
	protected byte[] buffer = new byte[1024];
	
	/**
	 * The number of data bytes currently in the buffer.
	 */
	protected int bufferCount = 0;
	
	/**
	 * Has this response been committed yet?
	 */
	protected boolean committed;
	
	protected Connector connector;
	
	/**
	 * The actual number of bytes written to this Response
	 */
	protected int contentCount;
	
	/**
	 * The content length associated with this Response.
	 */
	protected int contentLength = -1;
	
	/**
	 * The content type associated with this Response.
	 */
	protected String contentType;
	
	/**
	 * The Context within which this Response is being producted.
	 */
	protected Context context;
	
	protected String encoding;
	
	/**
	 * The facade associated with this Response.
	 */
	protected ResponseFacade facade = new ResponseFacade(this);
	
	/**
	 * Are we currently processing inside a RequestDispatcher.include() ?
	 */
	protected boolean included;
	
	protected static final String info = "org.apache.catalina.connector.ResponseBase/1.0";
	
	protected Locale locale = Locale.getDefault();
	
	/**
	 * The output stream associated with this Response.
	 */
	protected OutputStream output;
	
	/**
	 * The Request with which this Response is associated.
	 */
	protected Request request;
	
	/**
	 * The ServletOutputStream that has been returned by <code>getOutputStream()</code>, if any.
	 */
	protected ServletOutputStream stream;
	
	/**
	 * Has this response output been suspended?
	 */
	protected boolean suspended;
	
	/**
	 * The PrintWriter that has been returned by <code>getWriter()</code>, kf any
	 */
	protected PrintWriter writer;
	
	/**
	 * Error flag, True if the response is an error report.
	 */
	protected boolean error;
	
	
	@Override
	public Connector getConnector() {

		return this.connector;
	}

	@Override
	public void setConnector(Connector connector) {
		this.connector = connector;
	}

	@Override
	public int getContentCount() {
		return this.contentCount;
	}

	@Override
	public Context getContext() {
		return this.context;
	}

	@Override
	public void setContext(Context context) {
		this.context = context;
	}

	@Override
	public void setAppCommitted(boolean appCommitted) {
		this.appCommitted = appCommitted;
	}

	@Override
	public boolean isAppCommitted() {
		return this.appCommitted || this.committed;
	}

	@Override
	public boolean getIncluded() {
		return this.included;
	}

	@Override
	public void setIncluded(boolean included) {
		this.included = included;

	}

	@Override
	public String getInfo() {
		return info;
	}

	@Override
	public Request getRequest() {
		return this.request;
	}

	@Override
	public void setRequest(Request request) {
		this.request = request;
	}

	@Override
	public ServletResponse getResponse() {
		return facade;
	}

	@Override
	public OutputStream getStream() {
		return this.output;
	}

	@Override
	public void setStream(OutputStream stream) {

		this.output = stream;
	}

	@Override
	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
		if(stream != null){
			((ResponseStream)stream).setSuspended(suspended);
		}

	}

	@Override
	public boolean isSuspended() {
		return this.suspended;
	}

	@Override
	public void setError() {
		this.error = true;
	}

	@Override
	public boolean isError() {
		return this.error;
	}

	@Override
	public ServletOutputStream createOutputStream() throws IOException {

		return new ResponseStream(this);
	}

	@Override
	public void finishResponse() throws IOException {
		//If no stream has been requested yet, get one so we can flush the necessary headers.
		if(this.stream == null){
			ServletOutputStream sos = this.getOutputStream();
			sos.flush();
			sos.close();
			return;
		}
		//if our stream is closed, no action is necessary
		if(((ResponseStream) stream).isClosed()){
			return;
		}
		//Flush and close the appropriate output mechanism
		if(writer != null){
			writer.flush();
			writer.close();
		}else{
			stream.flush();
			stream.close();
		}
		
		//
	}

	@Override
	public int getContentLength() {
		return this.contentLength;
	}

	@Override
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * Return a PrintWriter that can be used to render error messages,
	 * regardless of whether a stream or writer has already been acquired.
	 * 
	 * @return
	 */
	@Override
	public PrintWriter getReporter() {
		if(isError()){
			try{
				if(this.stream == null){
					this.stream = createOutputStream();
				}
			}catch(IOException e){
				return null;
			}
			return new PrintWriter(this.stream);
		}else{
			if(this.stream != null){
				return null;
			}else{
				try {
					return new PrintWriter(getOutputStream());
				} catch (IOException e) {
					return null;
				}
			}
		}
	}

	/**
	 * Release all object references, and initialize instance variables,
	 * in preparation for reuse of this object.
	 */
	@Override
	public void recycle() {

		//buffer is NOT reset when recycling
		bufferCount = 0;
		committed = false;
		appCommitted = false;
		suspended = false;
		//connector is NOT reset when recycling
		contentCount = 0;
		contentLength = -1;
		contentType = null;
		context = null;
		encoding = null;
		included = false;
		locale = Locale.getDefault();
		output = null;
		request = null;
		stream = null;
		writer = null;
		error = false;
	}

	/**
	 * 
	 * @param b
	 * @throws IOException
	 */
	public void write(int b) throws IOException{
		if(this.suspended){
			throw new IOException("responseBase.write.suspended");
		}
		
		if(bufferCount >= buffer.length){
			flushBuffer();
		}
		buffer[bufferCount++] = (byte)b;
		contentCount++;
	}
	
	/**
	 * 
	 * @param b
	 * @throws IOException
	 */
	public void write(byte[] b) throws IOException{
		if(suspended){
			throw new IOException("responseBase.write.suspended");
		}
		write(b, 0, b.length);
	}
	
	/**
	 * Write <code>len</code> bytes from the specified bytes array,starting at the specified offset,
	 * to our output stream. Flush the output stream as necessary.
	 * @param b
	 * @param off
	 * @param len
	 * @throws IOException
	 */
	public void write(byte[] b, int off, int len) throws IOException{
		if(suspended){
			throw new IOException("responseBase.write.suspended");
		}
		
		//if the whole thing fits in the buffer, just put it there
		if(len == 0){
			return;
		}
		if(len < (buffer.length - bufferCount)){
			System.arraycopy(b, off, buffer, bufferCount, len);
			bufferCount += len;
			contentCount += len;
			return;
		}
		
		//Flush the buffer and start writing full-buffer-size chunks
		flushBuffer();
		int iterations = len / buffer.length;
		int leftoverStart = iterations * buffer.length;
		int leftoverLen = len - leftoverStart;
		for (int i = 0; i < iterations; i++) {
			write(b, off + (i * buffer.length),buffer.length);
		}
		
		//Write the remainder ( guaranteed to fit in the buffer)
		if(leftoverLen > 0){
			write(b, off + leftoverStart, leftoverLen);
		}
	}

	/**
	 * Flush the buffer and commit this resppnse. 
	 * 
	 * @throws IOException
	 */
	@Override
	public void flushBuffer() throws IOException {
		committed = true;
		if(bufferCount > 0){
			try{
				output.write(buffer, 0, bufferCount);
			}finally{
				bufferCount = 0;
			}
		}
	}

	@Override
	public int getBufferSize() {
		return buffer.length;
	}

	@Override
	public String getCharacterEncoding() {
		if(encoding == null){
			return "UTF-8";
		}
		return encoding;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if(writer != null){
			throw new IllegalStateException("responseBase.getOutputStream.ise");
		}
		if(stream == null){
			stream = createOutputStream();
		}
		((ResponseStream)stream).setCommit(true);
		return stream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if(writer != null){
			return writer;
		}
		if(stream != null){
			throw new IllegalStateException("responseBase.getWriter.ise");
		}
		ResponseStream newStream = (ResponseStream) this.createOutputStream();
		newStream.setCommit(false);
		OutputStreamWriter osw = new OutputStreamWriter(newStream, this.getCharacterEncoding());
		writer = new PrintWriter(osw);
		stream = newStream;
		return writer;
	}

	@Override
	public boolean isCommitted() {

		return this.committed;
	}

	/**
	 * clear any content written to the buffer.
	 * 
	 */
	@Override
	public void reset() {
		if(this.committed){
			throw new IllegalStateException("responseBase.reset.ise");
		}
		
		if(this.included){
			return;			//Ignore any call from an included servlet
		}
		if(stream != null){
			((ResponseStream)stream).reset();
		}
		bufferCount = 0;
		contentLength = -1;
		contentType = null;

	}

	/**
	 * Reset the data buffer but not any status or header information
	 * 
	 */
	@Override
	public void resetBuffer() {
		if(this.committed){
			throw new IllegalStateException("responseBase.resetBuffer.ise");
		}
		bufferCount = 0;
	}

	/**
	 * Set the buffer size to be used for this Response
	 * 
	 * @param size
	 */
	@Override
	public void setBufferSize(int size) {
		if(committed || bufferCount > 0){
			throw new IllegalStateException("responseBase.setBufferSize.ise");
	
		}
		if(buffer.length >= size){
			return;
		}
		buffer = new byte[size];
	}

	/**
	 * Set the content length (in bytes) for this Response
	 * @param length
	 */
	@Override
	public void setContentLength(int length) {
		if(this.isCommitted()){
			return;
		}
		if(included){
			return;
		}
		this.contentLength = length;
	}

	@Override
	public void setContentType(String type) {
		if(this.isCommitted()){
			return;
		}
		if(this.included){
			return;		//Ignore any call from an included servlet
		}
		
		this.contentType = type;
		if(type.indexOf(";") >= 0){
			encoding = RequestUtil.parseCharacterEncoding(type);
			if(encoding == null){
				encoding = "UTF-8";
			}
		}else {
			if(encoding != null){
				this.contentType = type + ";charset=" + encoding;
			}
		}
	}

	@Override
	public void setLocale(Locale locale) {
		if(this.isCommitted()){
			return;
		}
		if(this.included){
			return;		//Ignore any call from an included servlet
		}
		
		this.locale = locale;
		if(this.context != null){
			//TODO
			//TODO
//			CharsetMapper mapper = context.getc
		}

	}
	
	@Override
	public byte[] getBuffer() {
		return buffer;
	}
	
}
