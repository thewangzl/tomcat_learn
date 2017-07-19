package org.apache.catalina.connector;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivilegedExceptionAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUtils;

import org.apache.catalina.HttpResponse;

/**
 * Convenience base implementation of the <b>HttpResponse</b> interface, which can be used for the 
 * <code>Response</code> implementation required by by most <code>Connectors</code> that deal with
 * HTTP. Only the connector-specific methods need to be implemented.
 * 
 * @author thewangzl
 *
 */
@SuppressWarnings("deprecation")
public class HttpResponseBase extends ResponseBase implements HttpResponse, HttpServletResponse {

	
	protected ArrayList<Cookie> cookies = new ArrayList<>();
	
	protected final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",Locale.US);

	private static final String CRLF = "\r\n";
	
	protected HttpResponseFacade facade = new HttpResponseFacade(this);
	
	/**
	 * 
	 */
	protected HashMap<String,ArrayList<String>> headers = new HashMap<>();
	
	protected static final String info = "org.apache.catalina.connector.HttpResponseBase/1.0";
	
	/**
	 * The HTTP status code associated with this Response.
	 */
	protected int status = HttpServletResponse.SC_OK;
	
	protected String message = getStatusMessage(HttpServletResponse.SC_OK);
	
	protected static final TimeZone zone = TimeZone.getTimeZone("GMT");
	
	@Override
	public ServletResponse getResponse() {
		return facade;
	}
	
	protected class PrivilegedFlushBuffer implements PrivilegedExceptionAction<Object>{

		@Override
		public Object run() throws Exception {
			doFlushBuffer();
			return null;
		}
		
	}
	
	private void doFlushBuffer() throws IOException{
		if(!isCommitted()){
			this.sendHeaders();
		}
		super.flushBuffer();
	}
	
	/**
	 * Send the HTTP response headers, if this has not already occurred
	 * 
	 * @throws IOException
	 */
	protected void sendHeaders() throws IOException{
		if(isCommitted()){
			return;
		}
		
		//Check if the request was an HTTP/0.9 request
		if("HTTP/0.9".equals(request.getRequest().getProtocol())){
			committed = true;
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
		
		//Send the "Status:" header
		outputWriter.print(this.getProtocol());
		outputWriter.print(" ");
		outputWriter.print(status);
		if(message != null){
			outputWriter.print(" ");
			outputWriter.print(message);
		}
		outputWriter.print(CRLF);
		if(getContentType() != null){
			outputWriter.print("Content-Type: " + getContentType() + CRLF); 
		}
		if(getContentLength() >= 0){
			outputWriter.print("Content-Length: " + getContentLength() + CRLF);
		}
		
		//Send all specified headers (if any)
		synchronized (headers) {
			Iterator<String> names = headers.keySet().iterator();
			while(names.hasNext()){
				String name = names.next();
				ArrayList<String> values =  headers.get(name);
				Iterator<String> items = values.iterator();
				while(items.hasNext()){
					String value = items.next() ;
					outputWriter.print(name + ": " + value + CRLF);
				}
			}
		}
		
		//Add the session ID cookie if necessary
		//TODO
		/*HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
		HttpSession session = hreq.getSession(false);
		if(session != null && session.isNew() && getContext() != null && getContext().getCookies()){
			
		}*/
		
		//Send all specified cookies (if any)
		//TODO
		
		outputWriter.print(CRLF);
//		outputWriter.print(CRLF);
		outputWriter.flush();
		
		committed = true;
	}
	
	/**
	 * Preform whatever actions are required to flush and close the output stream or writer,
	 * in a single operation.
	 * 
	 * @throws IOException
	 */
	@Override
	public void finishResponse() throws IOException {

		//If an HTTP error >= 400 has been created with no content,
		//attempt to create a simple error message
		if(!isCommitted() && stream == null && writer == null && status >= HttpServletResponse.SC_BAD_REQUEST
				&& contentType == null && contentCount == 0){
			try{
				setContentType("text/html");
				PrintWriter writer = getWriter();
				writer.println("<html>");
				writer.println("<head>");
				writer.println("<title>Tomcat Error Report</title>");
				writer.println("</head>");
				writer.println("<br><br>");
				writer.println("<body>");
				writer.println("<h1>HTTP Status ");
				writer.print(status);
				writer.print(" - ");
				if(message != null){
					writer.print(message);
				}else{
					writer.print(getStatusMessage(status));
				}
				writer.println("</h1>");
				writer.println("</body>");
				writer.println("</html>");
			}catch(IOException e){
				throw e;
			}
		}
		
		//Flush the headers and finish this response
		sendHeaders();
		super.finishResponse();
	}
	
	protected String getProtocol(){
		return request.getRequest().getProtocol();
	}
	
	/**
	 * Returns a default status message for the specified HTTP status code.
	 * 
	 * @param status
	 * @return
	 */
	protected String getStatusMessage(int status){
		switch (status) {
        case SC_OK:
            return ("OK");
        case SC_ACCEPTED:
            return ("Accepted");
        case SC_BAD_GATEWAY:
            return ("Bad Gateway");
        case SC_BAD_REQUEST:
            return ("Bad Request");
        case SC_CONFLICT:
            return ("Conflict");
        case SC_CONTINUE:
            return ("Continue");
        case SC_CREATED:
            return ("Created");
        case SC_EXPECTATION_FAILED:
            return ("Expectation Failed");
        case SC_FORBIDDEN:
            return ("Forbidden");
        case SC_GATEWAY_TIMEOUT:
            return ("Gateway Timeout");
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
        case 207:       // WebDAV
            return ("Multi-Status");
        case 422:       // WebDAV
            return ("Unprocessable Entity");
        case 423:       // WebDAV
            return ("Locked");
        case 507:       // WebDAV
            return ("Insufficient Storage");
        default:
            return ("HTTP Response Status " + status);
        }
	}
	@Override
	public void sendAcknowledgment() throws IOException {

	}

	@Override
	public void addCookie(Cookie cookie) {
		if(isCommitted()){
			return;
		}
		if(included){
			return;			//Ignore any call 
		}
		synchronized (cookies) {
			cookies.add(cookie);
		}
	}

	@Override
	public void addDateHeader(String name, long date) {
		if(isCommitted()){
			return;
		}
		if(included){
			return;			//Ignore any call 
		}
		addHeader(name, format.format(new Date(date)));

	}

	@Override
	public void addHeader(String name, String value) {
		if(isCommitted()){
			return;
		}
		if(included){
			return;			//Ignore any call 
		}
		synchronized (headers) {
			ArrayList<String> values =  headers.get(name);
			if(values == null){
				values = new ArrayList<>();
				headers.put(name, values);
			}
			values.add(value);
		}
	}

	@Override
	public void addIntHeader(String name, int value) {
		if(isCommitted()){
			return;
		}
		if(included){
			return;			//Ignore any call 
		}
		this.addHeader(name, "" + value);
	}

	@Override
	public boolean containsHeader(String name) {
		synchronized (headers) {
			return headers.get(name) != null;
		}
	}

	/**
	 * Encode the session identifier associated with this response into 
	 * the specified redirect URL, if necessary.
	 * @param url
	 * @return
	 */
	@Override
	public String encodeRedirectURL(String url) {
		if(this.isEncodeable(this.toAbsolute(url))){
			HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
			return this.toEncoded(url, hreq.getSession().getId());
		}
		return url;
	}

	@Override
	public String encodeRedirectUrl(String url) {
		return this.encodeRedirectURL(url);
	}

	@Override
	public String encodeURL(String url) {
		if(this.isEncodeable(this.toAbsolute(url))){
			HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
			return this.toEncoded(url, hreq.getSession().getId());
		}
		return url;
	}

	@Override
	public String encodeUrl(String url) {
		return this.encodeURL(url);
	}

	/**
	 * Send an error response with the specified status and a default message.
	 * 
	 * @param status HTTP status code to send
	 * @throws IOException
	 */
	@Override
	public void sendError(int status) throws IOException {
		this.sendError(status, this.getStatusMessage(status));
	}

	/**
	 * Send an error response with the specified status and message
	 * 
	 * @param status HTTP status code to send
	 * @param message Corresponding message to send
	 * @throws IOException
	 */
	@Override
	public void sendError(int status, String message) throws IOException {
		
		if(isCommitted()){
			throw new IllegalStateException("httpResponseBase.sendError.ise");
		}
		if(included){
			return;		//Ignore any call from an included servlet
		}
		setError();
		
		//Clear the status code and message.
		this.status = status;
		this.message = message;
		
		//Clear any data content that has been buffered
		resetBuffer();
		
		//Cause the response to be finished (from the application perspective)
		setSuspended(true);
	}

	/**
	 * Send a temporary redirect to be the specified redirect location URL.
	 * 
	 * @param location Location URL to redirect to
	 * @throws IOException
	 */
	@Override
	public void sendRedirect(String location) throws IOException {
		if(isCommitted()){
			throw new IllegalStateException("httpResonseBase.sendRedirect.ise");
		}
		if(included){
			return;			// Ignore any call from an included servlet
		}
		
		//Clear any data content that has been buffed
		resetBuffer();
		
		//Generate a temporary redirect to the specified location 
		try{
			String absolute = toAbsolute(location);
			setStatus(SC_MOVED_TEMPORARILY);
			setHeader("Location", absolute);
		}catch(IllegalArgumentException e){
			setStatus(SC_NOT_FOUND);
		}
		
		//Cause the response to be finished (from the application prespective)
		setSuspended(true);
	}

	@Override
	public void setDateHeader(String name, long value) {
		if(isCommitted()){
			return;
		}
		if(included){
			return;
		}
		
		setHeader(name, format.format(new Date(value)));
	}

	@Override
	public void setHeader(String name, String value) {
		if(isCommitted()){
			return;
		}
		if(included){
			return;
		}
		
		ArrayList<String> values = new ArrayList<>();
		values.add(value);
		synchronized (headers) {
			headers.put(name, values);
		}
		String match = name.toLowerCase();
		if(match.equals("content-length")){
			int contentLength = -1;
			try{
				contentLength = Integer.parseInt(value);
			}catch(NumberFormatException e){
				;
			}
			if(contentLength >= 0){
				setContentLength(contentLength);
			}
		}else if(match.equals("content-type")){
			setContentType(value);
		}
	}

	@Override
	public void setIntHeader(String name, int value) {
		if(isCommitted()){
			return;
		}
		if(included){
			return;
		}
		
		setHeader(name, "" + value);
	}

	/**
	 * Set the HTTP status to be returned with this response.
	 * 
	 * @param status
	 */
	@Override
	public void setStatus(int status) {
		this.setStatus(status, getStatusMessage(status));
	}

	/**
	 * Set the HTTP status and message to be returned with this response.
	 * 
	 * @param status
	 * @param message
	 */
	@Override
	public void setStatus(int status, String message) {

		if(included){
			return;
		}
		this.status = status;
		this.message = message;
	}

	@Override
	public Cookie[] getCookies() {
		synchronized (cookies) {
			return cookies.toArray(new Cookie[0]);
		}
	}

	@Override
	public String getHeader(String name) {
		ArrayList<String> values = null;
		synchronized (headers) {
			values =  headers.get(name);
		}
		if(values != null){
			return values.get(0);
		}
		return null;
	}

	@Override
	public String[] getHeaderNames() {
		synchronized (headers) {
			return (String[]) (headers.keySet().toArray(new String[headers.size()]));
		}
	}

	@Override
	public String[] getHeaderValues(String name) {
		ArrayList<String> values = null;
		synchronized (headers) {
			values =  headers.get(name);
		}
		if(values == null){
			return new String[0];
		}
		return  values.toArray(new String[values.size()]);
	}

	@Override
	public String getMessage() {
		return this.message;
	}

	@Override
	public int getStatus() {
		return this.status;
	}

	@Override
	public void reset(int status, String messge) {
		reset();
		setStatus(status, message);
	}
	
	private String toEncoded(String url, String sessionId){
		if(url == null || sessionId == null){
			return url;
		}
		
		String path = url;
		String query = "";
		String anchor = "";
		int question = url.indexOf("?");
		if(question >= 0){
			path = url.substring(0, question);
			query = url.substring(question);
		}
		int pound = path.indexOf("#");
		if(pound >= 0){
			anchor = path.substring(pound);
			path = path.substring(0, pound);
		}
		StringBuffer sb = new StringBuffer(path);
		if(sb.length() > 0){
			sb.append(";jsession=");
			sb.append(sessionId);
		}
		sb.append(anchor);
		sb.append(query);
		
		return sb.toString();
	}
	
	/**
	 * Convert (if necessary) and return the absolute URL that reporesents the resource referenced by 
	 * this possibly relative URL. If this URL is already absolute, return it unchanged.
	 * 
	 * @param location URL to be (possibly) converted and then returned
	 * @return
	 */
	private String toAbsolute(String location){
		
		if(location == null){
			return location;
		}
		
		//Construct a new absolute URL if possible (cribbed from the DefaultErrorPage servlet)
		URL url = null;
		try {
			url = new URL(location);
		} catch (MalformedURLException e) {
			HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
			String requrl = HttpUtils.getRequestURL(hreq).toString();
			try {
				url = new URL(new URL(requrl), location);
			} catch (MalformedURLException e1) {
				throw new IllegalArgumentException(location);
			}
		}
		return url.toExternalForm();
	}

	/**
	 * Return <code>true</code> if the specified URL should be encoded with a session identifier.
	 * This will be true if all of the following conditions are met:
	 * <ul>
	 * 	<li>The request we are responding to asked for a valid session
	 * 	<li>The request session ID was not received via a cookie
	 * 	<li>The specified URL points back to somewhere within the 
	 * 		web application that is responding to this request
	 * </ul>
	 * 
	 * @param location Absolute URL to be validated
	 * @return
	 */
	private boolean isEncodeable(String location){
		if(location == null){
			return false;
		}
		
		//Is this an intra-document reference ?
		if(location.startsWith("#")){
			return false;
		}
		
		//Are we in a valid session that is not using cookie ?
		HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
		HttpSession session = hreq.getSession(false);
		if(session == null){
			return false;
		}
		if(hreq.isRequestedSessionIdFromCookie()){
			return false;
		}
		
		//Is this a valid absolute URL?
		URL url = null;
		try {
			url = new URL(location);
		} catch (MalformedURLException e) {
			return false;
		}
		
		//Does this URL match down to (and including) the context path?
		if(!hreq.getScheme().equalsIgnoreCase(url.getProtocol())){
			return false;
		}
		if(!hreq.getServerName().equalsIgnoreCase(url.getHost())){
			return false;
		}
		int serverPort = hreq.getServerPort();
		if(serverPort == -1){
			if("https".equals(hreq.getScheme())){
				serverPort = 443;
			}else{
				serverPort = 80;
			}
		}
		int urlPort = url.getPort();
		if(urlPort == -1){
			if("https".equals(url.getProtocol())){
				urlPort = 443;
			}else{
				urlPort = 80;
			}
		}
		if(serverPort != urlPort){
			return false;
		}
		
		String contextPath = getContext().getPath();
		if(contextPath != null && contextPath.length() > 0){
			String file = url.getFile();
			if(file == null || !file.startsWith(contextPath)){
				return false;
			}
			if(file.indexOf(";jsessionid=" + session.getId()) >= 0){
				return false;
			}
		}
		
		//This URL belongs to our web application, so it is encodeable
		return true;
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
	}
	
	@Override
	public void setContentType(String type) {
		if(isCommitted()){
			return;
		}
		if(included){
			return;
		}
		
		super.setContentType(type);
	}
	
	@Override
	public void setLocale(Locale locale) {
		if(isCommitted()){
			return;
		}
		if(included){
			return;
		}
		
		super.setLocale(locale);
		String language = locale.getLanguage();
		if(language != null && language.length() > 0){
			String country = locale.getCountry();
			StringBuffer value = new StringBuffer(language);
			if(country != null && country.length() > 0){
				value.append("-");
				value.append(country);
			}
			setHeader("Content-Language", value.toString());
		}
	}
	
	/**
     * Release all object references, and initialize instance variables, in
     * preparation for reuse of this object.
     */
    public void recycle() {

        super.recycle();
        cookies.clear();
        headers.clear();
        message = getStatusMessage(HttpServletResponse.SC_OK);
        status = HttpServletResponse.SC_OK;

    }
	
	/**
	 * Clear any content written to the buffer. In addition, all cookies and headers
	 * are cleared, and the status is reset.
	 * 
	 */
	public void reset() {
		if(included){
			return;
		}
		super.reset();
		cookies.clear();
		headers.clear();
		message = getStatusMessage(SC_OK);
		status = HttpServletResponse.SC_OK;
	};
	
}
