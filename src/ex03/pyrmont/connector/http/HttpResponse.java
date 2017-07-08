package ex03.pyrmont.connector.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.util.CookieTools;

import ex03.pyrmont.connector.ResponseStream;
import ex03.pyrmont.connector.ResponseWriter;

public class HttpResponse implements HttpServletResponse {

	// the default buffer size
	private static final int BUFFER_SIZE = 2048;

	private OutputStream out;
	private HttpRequest request;
	private PrintWriter writer;
	protected byte[] buffer = new byte[BUFFER_SIZE];
	protected int bufferCount = 0;

	protected boolean committed = false;

	protected int contentCount = 0;

	protected int contentLength = -1;

	protected String contentType;

	protected String encoding;

	protected ArrayList<Cookie> cookies = new ArrayList<>();

	protected HashMap headers = new HashMap<>();

	protected final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MM yyyy HH:mm:ss zzz", Locale.US);

	protected int status = HttpServletResponse.SC_OK;
	
	protected String message = getStatusMessage(status);
	
	public HttpResponse(OutputStream out) {
		this.out = out;
	}

	public void setRequest(HttpRequest request) {
		this.request = request;
	}


	public void sendStaticResource() throws IOException {
		//
		byte[] bytes = new byte[BUFFER_SIZE];
		FileInputStream fis = null;
		try{
			File file = new File(Constants.WEB_ROOT, request.getRequestURI());
			fis = new FileInputStream(file);
			/**
			 * 
			 */
			String header  ="HTTP/1.1 200 OK\r\n"
					+ "Content-Type: text/html\r\n"
					+ "\r\n";
			out.write(header.getBytes());
			int ch = fis.read(bytes, 0, BUFFER_SIZE);
			while(ch != -1){
				out.write(bytes, 0, ch);
				ch = fis.read(bytes, 0, BUFFER_SIZE);
			}
		}catch(FileNotFoundException e){
			String errorMessage = "HTTP/1.1 404 File Not Found\r\n"	//
					+ "Content-Type: text/html\r\n"	//
					+ "Content-Length: 23\r\n"	//
					+ "\r\n"					//
					+ "<h1>File Not Found</h1>";
			out.write(errorMessage.getBytes());
		}finally{
			if(fis != null){
				fis.close();
			}
		}
	}
	
	public void write(int b) throws IOException{
		if(bufferCount >= buffer.length){
			flushBuffer();
		}
		buffer[bufferCount++] = (byte) b;
		contentCount++;
	}
	
	public void write(byte[] b) throws IOException{
		this.write(b, 0, b.length);
	}
	
	public void write(byte[]b, int off, int len) throws IOException{
		//If the whole thing fits in the buffer, just put it there
		if(len == 0){
			return;
		}
		if(len <= (buffer.length - bufferCount)){
			System.arraycopy(b, off, buffer, bufferCount, len);
			bufferCount += len;
			contentCount += len;
			return;
		}
		//flush the buffer and start writing full-buffer-size chunks
		flushBuffer();
		int iterations = len / buffer.length;
		int leftoverStart = iterations * buffer.length;
		int leftoverLen = len - leftoverStart;
		for(int i = 0; i < iterations; i++){
			write(b, off + (i * buffer.length), buffer.length);
		}
		
		//write the remainder (guaranteed to fit in the buffer)'
		if(leftoverLen > 0){
			write(b, off + leftoverStart, leftoverLen);
		}
	}

	public void finishResponse() throws IOException {
		 sendHeaders();
		// Flush and close the appropriate output mechanism
		if (writer != null) {
			writer.flush();
			writer.close();
		}
	}

	public int getContentLength() {
		return contentLength;
	}

	public String getContentType() {
		return contentType;
	}

	public String getProtocol() {
		return request.getProtocol();
	}

	protected String getStatusMessage(int status) {
		switch (status) {
		case SC_OK:
			return "OK";
		case SC_ACCEPTED:
			return "Accepted";
		case SC_BAD_GATEWAY:
			return "Bad Gateway";
		case SC_BAD_REQUEST:
			return "Bad Request";
		case SC_CONFLICT:
			return "Conflict";
		case SC_CONTINUE:
			return "Continue";
		case SC_CREATED:
			return "Created";
		case SC_EXPECTATION_FAILED:
			return "Expectation Failed";
		case SC_FORBIDDEN:
			return "Forbidden";
		case SC_GATEWAY_TIMEOUT:
			return "Gateway Timeout";
		case SC_GONE:
			return ("Gone");
		case SC_HTTP_VERSION_NOT_SUPPORTED:
			return ("HTTP Version Not Supported");
		case SC_INTERNAL_SERVER_ERROR:
			return ("Internal Server Error");
		case SC_LENGTH_REQUIRED:
			return ("Length Required");
		case SC_METHOD_NOT_ALLOWED:
			return ("Method Not Allowed");
		case SC_MOVED_PERMANENTLY:
			return ("Moved Permanently");
		case SC_MOVED_TEMPORARILY:
			return ("Moved Temporarily");
		case SC_MULTIPLE_CHOICES:
			return ("Multiple Choices");
		case SC_NO_CONTENT:
			return ("No Content");
		case SC_NON_AUTHORITATIVE_INFORMATION:
			return ("Non-Authoritative Information");
		case SC_NOT_ACCEPTABLE:
			return ("Not Acceptable");
		case SC_NOT_FOUND:
			return ("Not Found");
		case SC_NOT_IMPLEMENTED:
			return ("Not Implemented");
		case SC_NOT_MODIFIED:
			return ("Not Modified");
		case SC_PARTIAL_CONTENT:
			return ("Partial Content");
		case SC_PAYMENT_REQUIRED:
			return ("Payment Required");
		case SC_PRECONDITION_FAILED:
			return ("Precondition Failed");
		case SC_PROXY_AUTHENTICATION_REQUIRED:
			return ("Proxy Authentication Required");
		case SC_REQUEST_ENTITY_TOO_LARGE:
			return ("Request Entity Too Large");
		case SC_REQUEST_TIMEOUT:
			return ("Request Timeout");
		case SC_REQUEST_URI_TOO_LONG:
			return ("Request URI Too Long");
		case SC_REQUESTED_RANGE_NOT_SATISFIABLE:
			return ("Requested Range Not Satisfiable");
		case SC_RESET_CONTENT:
			return ("Reset Content");
		case SC_SEE_OTHER:
			return ("See Other");
		case SC_SERVICE_UNAVAILABLE:
			return ("Service Unavailable");
		case SC_SWITCHING_PROTOCOLS:
			return ("Switching Protocols");
		case SC_UNAUTHORIZED:
			return ("Unauthorized");
		case SC_UNSUPPORTED_MEDIA_TYPE:
			return ("Unsupported Media Type");
		case SC_USE_PROXY:
			return ("Use Proxy");
		case 207: // WebDAV
			return ("Multi-Status");
		case 422: // WebDAV
			return ("Unprocessable Entity");
		case 423: // WebDAV
			return ("Locked");
		case 507: // WebDAV
			return ("Insufficient Storage");
		default:
			return ("HTTP Response Status " + status);
		}
	}
	
	public OutputStream getStream(){
		return this.out;
	}
	
	protected void sendHeaders() throws IOException{
		if(isCommitted()){
			return;
		}
		//Prepare a suitable output writer
		OutputStreamWriter osw = null;
		try{
			osw = new OutputStreamWriter(getStream(), getCharacterEncoding());
		}catch(UnsupportedEncodingException e){
			osw = new OutputStreamWriter(getStream());
		}
		final PrintWriter outputWriter = new PrintWriter(osw);
		//Send the "status:" header
		outputWriter.print(this.getProtocol());
		outputWriter.print(" ");
		outputWriter.print(status);
		if(message != null){
			outputWriter.print(" ");
			outputWriter.print(message);
		}
		outputWriter.print("\r\n");
		//Send the content-length and Content-Type headers (if any)
		if(this.getContentType() != null){
			outputWriter.print("Content-Type:" + this.getContentType() + "\r\n");
		}
		if(this.getContentLength() > 0){
			outputWriter.print("Content-Length:" + this.getContentLength() + "\r\n");
		}
		//Send all specified headers (if any)
		synchronized (headers) {
			Iterator names = headers.keySet().iterator();
			while(names.hasNext()){
				String name = (String) names.next();
				ArrayList values = (ArrayList) headers.get(name);
				Iterator items = values.iterator();
				while(items.hasNext()){
					String value = (String) items.next();
					outputWriter.print(name);
					outputWriter.print(": ");
					outputWriter.println(value);
					outputWriter.println("\r\n");
				}
			}
		}
		
		
		// Send all specified cookies (if any)
		synchronized (cookies) {
			Iterator items = cookies.iterator();
			while(items.hasNext()){
				Cookie cookie = (Cookie) items.next();
				outputWriter.print(CookieTools.getCookieHeaderName(cookie));
				outputWriter.print(": ");
				outputWriter.print(CookieTools.getCookieHeaderValue(cookie));
				outputWriter.print("\r\n");
			}
		}
		
		//Send a terminating blank line to mark the end of the headers
		outputWriter.print("\r\n");
		outputWriter.flush();
		committed = true;
	}

	@Override
	public void flushBuffer() throws IOException {
		if(bufferCount > 0){
			try{
				out.write(buffer, 0, bufferCount);
			}finally{
				bufferCount =0;
			}
		}
	}

	@Override
	public int getBufferSize() {
		return 0;
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
		return null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return null;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		ResponseStream newStream = new ResponseStream(this);
		newStream.setCommit(false);
		OutputStreamWriter osw = new OutputStreamWriter(newStream, this.getCharacterEncoding());
		writer = new ResponseWriter(osw);
		return writer;
	}

	@Override
	public boolean isCommitted() {
		return this.committed;
	}

	@Override
	public void reset() {

	}

	@Override
	public void resetBuffer() {


	}

	@Override
	public void setBufferSize(int size) {

	}

	@Override
	public void setContentLength(int length) {
		if(this.isCommitted()){
			return;
		}
		this.contentLength = length;
	}

	@Override
	public void setContentType(String type) {
		if(this.isCommitted()){
			return;
		}
		this.contentType = type;
	}

	@Override
	public void setLocale(Locale locale) {
		if(this.isCommitted()){
			return;
		}
		String language = locale.getLanguage();
		if(language != null && language.length() > 0){
			String country = locale.getCountry();
			StringBuffer value = new StringBuffer();
			if(country != null && country.length() > 0){
				value.append("-");
				value.append(country);
			}
			setHeader("Content-Language", value.toString());
		}
	}

	@Override
	public void addCookie(Cookie cookie) {
		if(this.isCommitted()){
			return;
		}
		synchronized (cookies) {
			cookies.add(cookie);
		}

	}

	@Override
	public void addDateHeader(String name, long value) {
		if(this.isCommitted()){
			return;
		}
		this.addHeader(name, format.format(new Date(value)));
	}

	@Override
	public void addHeader(String name, String value) {
		if(this.isCommitted()){
			return;
		}
		synchronized (headers) {
			ArrayList values = (ArrayList) headers.get(name);
			if(values == null){
				values = new ArrayList<>();
				headers.put(name, values);
			}
			values.add(value);
		}
	}

	@Override
	public void addIntHeader(String name, int value) {
		if(this.isCommitted()){
			return;
		}
		this.addHeader(name, value + "");
	}

	@Override
	public boolean containsHeader(String name) {
		synchronized (headers) {
			return headers.get(name) != null;
		}
	}

	@Override
	public String encodeRedirectURL(String url) {
		return encodeRedirectURL(url);
	}

	@Override
	public String encodeRedirectUrl(String url) {
		return encodeRedirectURL(url);
	}

	@Override
	public String encodeURL(String name) {

		return encodeURL(name);
	}

	@Override
	public String encodeUrl(String name) {
		return encodeURL(name);
	}

	@Override
	public void sendError(int sc) throws IOException {

	}

	@Override
	public void sendError(int sc, String message) throws IOException {

	}

	@Override
	public void sendRedirect(String location) throws IOException {

	}

	@Override
	public void setDateHeader(String name, long value) {
		this.setHeader(name, format.format(new Date(value)));
	}

	@Override
	public void setIntHeader(String name, int value) {
		this.setHeader(name, "" + value);
	}
	

	public void setHeader(String name, String value) {
		if(this.isCommitted()){
			return;
		}
		ArrayList values = new ArrayList<>();
		values.add(value);
		synchronized (headers) {
			headers.put(name, values);
		}
		//
		String match = name.toLowerCase();
		if(match.equals("content-length")){
			int contentLength = -1;
			try{
				contentLength = Integer.parseInt(value);
			}catch(NumberFormatException e){
				;
			}
			if(contentLength >= 0){
				this.setContentLength(contentLength);
			}
		}else if(match.equals("content-type")){
			this.setContentType(value);
		}
	}

	@Override
	public void setStatus(int arg0) {

	}

	@Override
	public void setStatus(int arg0, String arg1) {

	}

}
