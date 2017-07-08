package org.apache.catalina.connector;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

import org.apache.catalina.Response;

/**
 * Convenience implementation of <b>ServletOutputStream</b> that works with the standard ResponseBase 
 * implementation of <b>Response</b>. If the content length has been set on out associated Response, 
 * this implementation will enforce not writing more than that many bytes on the underlying stream.
 * @author thewangzl
 *
 */
public class ResponseStream extends ServletOutputStream {

	protected Response response;
	
	protected boolean closed;
	
	protected boolean commit;
	
	/**
	 * The number of bytes which has already been written to this stream.
	 */
	protected int count;
	
	/**
	 * The content length pass which we will not write, or -1 if there is 
	 * no defined content length.
	 */
	protected int length = -1;
	
	protected OutputStream stream;
	
	protected boolean suspended;
	
	public ResponseStream(Response response) {
		super();
		
		this.response = response;
		
		this.stream = response.getStream();
		this.suspended = response.isSuspended();
	}



	@Override
	public void write(int b) throws IOException {
		if(this.suspended){
			return;
		}
		if(closed){
			throw new IOException("responseStream.write.closed");
		}
		if(length > 0 && count > length){
			throw new IOException("responseStream.write.count");
		}
		((ResponseBase)response).write(b);
		count++;
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		//
		if(suspended){
			return;
		}
		write(b, 0, b.length);
	}
	
	/**
	 * Write <code>len</code> bytes from the specified byte array,
	 * starting at the specified offset, to our outpt stream.
	 * 
	 * @param b the byte array containing the bytes to be written
	 * @param off  Zero-relative starting offset of the bytes to be written
	 * @param len
	 * @throws IOException
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(suspended){
			return;
		}
		if(closed){
			throw new IOException("responseStream.write.closed");
		}
		int actual = len;
		if(length > 0 && (count + len) >= length){
			actual = length - count;
		}
		((ResponseBase) response).write(b, off, actual);
		count += actual;
		if(actual < len){
			throw new IOException("responseStream.write.count");
		}
	}

	/**
	 * Close this output stream, causing any buffered data to be flushed and
	 * any further output data to throw an IOException.
	 * 
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		if(suspended){
			throw new IOException("responseStream.suspended");
		}
		if(closed){
			throw new IOException("responseStream.close.closed");
		}
		response.getResponse().flushBuffer();
		closed = true;
	}


	public boolean isCommit() {
		return commit;
	}



	public void setCommit(boolean commit) {
		this.commit = commit;
	}

	public boolean isSuspended() {
		return suspended;
	}

	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClose(boolean closed) {
		this.closed = closed;
	}


	/**
	 * Reset the count of bytes written to this stream to zero.
	 */
	public void reset() {
		count = 0;
	}
	

}
