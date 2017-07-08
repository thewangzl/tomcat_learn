package ex03.pyrmont.connector;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

import ex03.pyrmont.connector.http.HttpResponse;

public class ResponseStream extends ServletOutputStream {

	private HttpResponse response;
	
	protected OutputStream stream;
	
	protected boolean closed;
	
	protected boolean commit;
	
	protected int count = 0;
	
	protected int length = -1;
	
	public ResponseStream(HttpResponse response) {
		super();
		this.response = response;
	}

	/**
	 * Write the specified byte to our output stream
	 */
	@Override
	public void write(int b) throws IOException {
		if(closed){
			throw new IOException("responseStream.write.close");
		}
		if(length > 0 && count >= length){
			throw new IOException("responseStream.write.count");
		}
		
		response.write(b);
		count++;
	}
	@Override
	public void write(byte[] b) throws IOException {
		this.write(b, 0, b.length);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(closed){
			throw new IOException("responseStream.write.closed");
		}
		int actual = len;
		if(length > 0 && count + len >= length){
			actual = length - count;
		}
		response.write(b, off, len);
		count += actual;
		if(actual < len){
			throw new IOException("responseStream.write.count");
		}
	}
	
	@Override
	public void close() throws IOException {
		if(closed){
			throw new IOException("responseStream.close.closed");
		}
		response.flushBuffer();
		closed = true;
	}
	
	@Override
	public void flush() throws IOException {
		if(closed){
			throw new IOException("responseStream.flush.closed");
		}
		if(commit){
			response.flushBuffer();
		}
	}

	public void setCommit(boolean commit) {
		this.commit = commit;
	}
	
	public boolean getCommit(){
		return this.commit;
	}

	public boolean closed(){
		return this.closed;
	}
	
	void reset(){
		count = 0;
	}
}
