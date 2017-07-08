package org.apache.catalina.connector.http;

import java.io.IOException;

import org.apache.catalina.connector.RequestStream;

/**
 * 
 * @author thewangzl
 *
 */
public class HttpRequestStream extends RequestStream {

	
	/**
	 * Use chunk ?
	 */
	protected boolean chunk;
	
	/**
	 * True if final chunk was found
	 */
	protected boolean endChunk;
	
	/**
	 * Chunk buffer.
	 */
	protected byte[] chunkBuffer;
	
	/**
	 * Chunk length
	 */
	protected int chunkLength = 0;
	
	/**
	 * Chunk buffer postion.
	 */
	protected int chunkPos = 0;
	
	/**
	 * HTTP/1.1 flag
	 */
	protected boolean http11;
	
	public HttpRequestStream(HttpRequestImpl request,HttpResponseImpl response) {
		super(request);
		String transferEncoding = request.getHeader("Transfer-Encoding");
		chunk = (transferEncoding != null) && transferEncoding.indexOf("chunked") != -1;
		
		if(!chunk && length == -1){
			//Ask for connection close
			response.addHeader("Connection", "close");
		}
		
	}

	
	/**
	 * Close this input stream. No physical level I-O is performed, but any further attempt to read from 
	 * this stream will throw an IO IOException. If a content length has been set but not all of the bytes 
	 * have yet been consumed, the remaining bytes will be swallowed.
	 * 
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		if(closed){
			throw new IOException("requestStream.close.closed");
		}
		
		if(chunk){
			while(!endChunk){
				int b = read();
				if(b < 0){
					break;
				}
			}
		}else{
			if(http11 && length > 0){
				while(count < length){
					int b = read();
					if(b < 0){
						break;
					}
				}
			}
		}
		closed = true;
	}
	
	/**
	 * Read and return a single byte from this input stream, or -1 if end of file has been encountered.
	 * 
	 * @throws IOException
	 */
	@Override
	public int read() throws IOException {
		
		// Has this stream been closed?
		if(closed){
			throw new IOException("requestStream.read.closed");
		}
		
		if(chunk){
			if(endChunk){
				return -1;
			}
			if(chunkBuffer == null || chunkPos >= chunkLength){
				if(!fillChunkBuffer()){
					return -1;
				}
			}
			return chunkBuffer[chunkPos++] & 0xff;
		}
		return super.read();
	}
	
	/**
	 * Read up to <code>len</code> bytes of data from the input stream into an array of bytes.
	 * An attempt is made to read as many as <code>len</code> bytes, but a smaller number may 
	 * read, possibly zero. The number of bytes actually read is returned as an integer. This
	 * method blocks until input data is available, end of file is detected, or an exception is thrown.
	 * 
	 *  @param b the Buffer into which the data is read
	 *  @param off The start offset into array <code>b</code> at which the data is written
	 *  @param len the maxium number of bytes to read
	 *  @return
	 *  @throws IOException
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		// TODO Auto-generated method stub
		if(chunk){
			
			int avail = chunkLength - chunkPos;
			if(avail == 0){
				fillChunkBuffer();
			}
			avail = chunkLength - chunkPos;
			if(avail == 0){
				return -1;
			}
			
			int toCopy = avail;
			if(avail > len){
				toCopy = len;
			}
			System.arraycopy(chunkBuffer, chunkPos, b, off, toCopy);
			chunkPos += toCopy;
			return toCopy;
		}
		return super.read(b, off, len);
	}
	
	/**
	 * Fill the chunk buffer.
	 * 
	 * @return
	 * @throws IOException
	 */
	private synchronized boolean fillChunkBuffer() throws IOException{
		chunkPos = 0;
		try{
			String numberValue = readLineFromStream();
			if(numberValue != null){
				numberValue = numberValue.trim();
			}
			chunkLength = Integer.parseInt(numberValue, 16);
		}catch(NumberFormatException e){
			//Critical error, unable to parse the chunk length
			chunkLength = 0;
			chunk = false;
			close();
			return false;
		}
		
		if(chunkLength  == 0){
			//Skipping trailing headers, if any
			String trailingLine = readLineFromStream();
			while(!trailingLine.equals("")){
				trailingLine = readLineFromStream();
			}
			endChunk = true;
			return false;
			//TODO: should the stream be automatically closed ?
		}else{
			
			if(chunkBuffer == null && chunkLength > chunkBuffer.length){
				chunkBuffer = new byte[chunkLength];
			}
			
			//Now read the whole chunk into the buffer
			
			int nRead = 0;
			int currentRead = 0;
			
			while(nRead < chunkLength){
				try{
				currentRead = stream.read(chunkBuffer, nRead, chunkLength - nRead);
				}catch(Throwable t){
					t.printStackTrace();
					throw new IOException();
				}
				if(currentRead < 0){
					throw new IOException("requestStream.read.error");
				}
				nRead += currentRead;
			}
			//Skipping the CRLF
			String blank = readLineFromStream();
		}
		
		return true;
	}
	
	/**
	 * Reads the input stream, one line at a time, Reads bytes into an array, until it reads a certain 
	 * number of bytes or reaches a newline character, which it reads into the array as well.
	 *  
	 * @return
	 * @throws IOException
	 */
	private String readLineFromStream() throws IOException{
		StringBuffer sb = new StringBuffer();
		while(true){
			int ch = super.read();
			if(ch < 0){
				if(sb.length() == 0){
					return null;
				}else {
					break;
				}
			}else if(ch == '\r'){
				continue;
			}else if(ch == '\n'){
				break;
			}
			sb.append((char) ch);
		}
		return sb.toString();
	}
	
	
	
	
	
	
	
}
