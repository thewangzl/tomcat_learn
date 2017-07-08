package org.apache.catalina.connector;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

import org.apache.catalina.Request;

/**
 * Convenience implementation of <b>ServletInputStream</b> that works with the standard implementations of
 * <b>Request</b>. if the content length has been set on our associated Request, this implementation will
 * enforce not reading more than that many bytes on the underlying stream.
 * 
 * @author thewangzl
 *
 */
public class RequestStream extends ServletInputStream {

	protected boolean closed;
	
	/**
	 * The number of bytes which have already been returned by this stream.
	 */
	protected int count;
	
	/**
	 * The content length past which we will not read, or -1 if where 
	 * is no defined content length.
	 */
	protected int length = -1;
	
	/**
	 * The underlying input stream from which we should read data.
	 */
	protected InputStream stream;
	
	
	public RequestStream(Request request) {
		super();
		length = request.getRequest().getContentLength();
		stream = request.getStream();
		
	}
	
	/**
	 * Close this input stream. No physical level I-O is performed, but any further attempt to read
	 * from this stream will throw an IOException. If a content length has been set but not all of
	 * the bytes have yet been consumed, the remaining bytes will be swallowed.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		if(closed){
			throw new IOException("requestStream.close.closed");
		}
		if(length > 0){
			while(count < length){
				int b = read();
				if(b < 0){
					break;
				}
			}
		}
		closed = true;
	}

	/**
	 * Read and return a single byte from this input stream, or -1 if end of 
	 * file has been encountered.
	 * 
	 * @exception IOException
	 */
	@Override
	public int read() throws IOException {
		if(closed){
			throw new IOException("requestStream.close.closed");
		}
		
		//Have we read the specified content length already?
		if(length >= 0 && count >= length){
			return -1;
		}
		
		//Read and count the next byte, then return it
		int b = stream.read();
		if(b >= 0){
			count++;
		}
		return b;
	}
	
	/**
	 * 
	 * 
	 * @param b
	 * 
	 * @exception IOException
	 */
	@Override
	public int read(byte[] b) throws IOException {
		
		return this.read(b, 0, b.length);
	}
	
	
	/**
	 * Read up to <code>len</code> bytes of data from the input stream into an array of bytes.
	 * An attempt is made to read as many as <code>len</code> bytes, but a smaller number may read,
	 * possibly zero. The number of bytes actually read is returned as an integer. This method 
	 * blocks until input data is available, end of file is detected, or an exception is thrown.
	 * 
	 * @param b
	 * @param off
	 * @param len
	 * 
	 * @exception IOException
	 */
	public int read(byte[] b, int off, int len) throws IOException{
		
		int toRead = len;
		if(length > 0){
			if(count >= length){
				return -1;
			}
			if(count + len > length){
				toRead = length - count;
			}
		}
		int actuallyRead = super.read(b, off, toRead);
		return actuallyRead;
	}

}
