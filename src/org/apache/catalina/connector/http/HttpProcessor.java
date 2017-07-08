package org.apache.catalina.connector.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Globals;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.ServerInfo;
import org.apache.catalina.util.StringParser;
import org.apache.tomcat.util.http.FastHttpDateFormat;



public final class HttpProcessor implements Lifecycle, Runnable {

	
	private static final String SERVER_INFO = ServerInfo.getServerInfo() + " (HTTP/1.1 Connector)";
	
	/**
	 * Is there a new socket available
	 */
	private boolean available;
	
	private HttpConnector connector;
	
	private int id;
	
	private HttpRequestImpl request;
	
	private HttpResponseImpl response;
	
	private int serverPort;
	
	private String threadName;
	
	private Socket socket;
	
	private boolean started;
	
	private boolean stopped;
	
	private Thread thread;
	
	private Object threadSync = new Object();
	
	private boolean keepAlive;
	
	private boolean http11 = true;
	
	private int proxyPort = 0;
	
	private String proxyName;
	
	private LifecycleSupport lifecycle = new LifecycleSupport(this);
	
	/**
	 * The match string for identifying a session ID parameter.
	 */
	private static final String match = ";" + Globals.SESSION_PARAMETER_NAME + "=";
	
	/**
	 * The match string for idenfying a session ID parameter.
	 */
	private static final char[] SESSION_ID = match.toCharArray();
	
	/**
	 * The string parser we will use for parsing request lines.
	 */
	private StringParser parser = new StringParser();
	
	/**
	 * 
	 */
	private boolean sendAck;
	
	private static final byte[] ack = (new String("HTTP/1.1 100 Continue\r\n\r\n")).getBytes();
	
	private static final byte[] CRLF = (new String("\r\n")).getBytes();
	
	/**
	 * Request line buffer
	 */
	private HttpRequestLine requestLine = new HttpRequestLine();
	
	
	private int status = Constants.PROCESSOR_IDLE;
	
	public HttpProcessor(HttpConnector connector, int id) {
		this.connector = connector;
		this.id = id;
		this.proxyName = connector.getProxyName();
		this.proxyPort = connector.getProxyPort();
		this.request = (HttpRequestImpl) connector.createRequest();
		this.response = (HttpResponseImpl) connector.createResponse();
		this.serverPort = connector.getPort();
		this.threadName = "HttpProcessor[" + connector.getPort() + "][" + id + "]";
	}
	
	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		this.lifecycle.addLifecycleListener(listener);
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		return this.lifecycle.findLifecycleListeners();
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		this.lifecycle.removeLifecycleListener(listener);
	}

	@Override
	public void start() throws LifecycleException {
		if(started){
			throw new LifecycleException("httpProcessor.alreadyStarted");
		}
		lifecycle.fireLifecycleEvent(START_EVENT, null);
		started = true;
		
		threadStart();
	}

	@Override
	public void stop() throws LifecycleException {
		if(!started){
			throw new LifecycleException("httpProcessor.notStarted");
		}
		lifecycle.fireLifecycleEvent(STOP_EVENT, null);
		started = false;
		
		this.threadStop();
	}

	@Override
	public void run() {
		//Process requests until we receive a shutdown
		while(!stopped){
			
			//Wait for the next socket to be assigned
			Socket socket = await();
			if(socket == null){
				continue;
			}
			
			//process the request from this socket
			try{
				process(socket);
			}catch(Throwable t){
				log("process.invoke",t);
			}
			
			//Finish up this request
			connector.recycle(this);
		}
		
		//tell threadStop() we have shut ourselves down successfully
		synchronized (threadSync) {
			threadSync.notifyAll();
		}
	}
	
	private void threadStart(){
		log(SERVER_INFO +":httpProcessor.starting");
		
		thread = new Thread(this, threadName);
		thread.setDaemon(true);
		thread.start();
	}
	
	private void threadStop(){
		log("httpProcessor.stopping");
		stopped = true;
		assign(null);
		
		if(status == Constants.PROCESSOR_IDLE){
			//Only wait if the processor is actually processing a command
			synchronized (threadSync) {
				try {
					threadSync.wait(5000);
				} catch (InterruptedException e) {
					;
				}
			}
		}
		thread = null;
	}
	
	/**
	 * Process an incoming HTTP request on the Socket that has been assigned to this Processor.
	 * Any exceptions that occur during processing must be swallowed and dealt with.
	 * 
	 * @param socket
	 */
	private void process(Socket socket){
		boolean ok = true;
		boolean finishResponse = true;
		SocketInputStream input = null;
		OutputStream output = null;
		
		//Construct and initialize the objects we will need
		try {
			input = new SocketInputStream(socket.getInputStream(), connector.getBufferSize());
		} catch (IOException e) {
			log("process.create",e);
			ok = false;
		}
		
		keepAlive = true;
		while(!stopped && ok && keepAlive){
			finishResponse = true;
			try{
				request.setStream(input);
				request.setResponse(response);
				output = socket.getOutputStream();
				response.setStream(output);
				response.setRequest(request);
				((HttpServletResponse) response.getResponse()).setHeader("Server", SERVER_INFO);;
			}catch(Exception e){
				log("process.create",e);
				ok = false;
			}
			
			//Parse the incoming requet
			try{
				if(ok){
					parseConnection(socket);
					parseRequest(input, output);
					if(!request.getRequest().getProtocol().startsWith("HTTP/1.0")){
						parseHeaders(input);
					}
					if(http11){
						//Sending a request acknowledge back to the client of requested
						ackRequest(output);
						//If the protocol is HTTP/1.1, chunking is allowed.
						if(connector.isChunkingAllowed()){
							response.setAllowChunking(true);
						}
					}
				}
				
			}catch(EOFException e){
				// It's vert likely to be a socket disconnect on either the client or the server
				ok = false;
				finishResponse = false;
			}catch (ServletException e) {
				ok = false;
				try {
					((HttpServletResponse) response.getResponse()).sendError(HttpServletResponse.SC_BAD_REQUEST);
				} catch (IOException e1) {
					;
				}
			}catch (InterruptedIOException e) {
				log("process.parse",e);
				try {
					((HttpServletResponse) response.getResponse()).sendError(HttpServletResponse.SC_BAD_REQUEST);
				} catch (IOException e1) {
					;
				}
				ok = false;
			}
			catch(Exception e){
				log("process.parse",e);
				try {
					((HttpServletResponse) response.getResponse()).sendError(HttpServletResponse.SC_BAD_REQUEST);
				} catch (IOException e1) {
					;
				}
				ok = false;
			}
			try{
				//Ask our Container to proess this request
				((HttpServletResponse) response).setHeader("Date", FastHttpDateFormat.getCurrentDate());
				if(ok){
					connector.getContainer().invoke(request, response);
				}
			}catch(InterruptedIOException e){
				ok = false;
			}
			catch(ServletException e){
				log("process.invoke", e);
				try {
					((HttpServletResponse) response.getResponse()).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				} catch (IOException e1) {
					;
				}
				ok = false;
			} catch (IOException e) {
				log("process.invoke", e);
				try {
					((HttpServletResponse) response.getResponse()).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				} catch (IOException e1) {
					;
				}
				ok = false;
			}catch(Throwable t){
				log("process.invoke", t);
				try {
					((HttpServletResponse) response.getResponse()).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				} catch (IOException e1) {
					;
				}
				ok = false;
			}
			
			//Finish up the handling of the request
			if(finishResponse){
				try {
					response.finishResponse();
				} catch (IOException e) {
					ok = false;
				}catch(Throwable e){
					log("process.invoke", e);
					ok = false;
				}
				
				try {
					request.finishRequest();
				} catch (IOException e) {
					ok = false;
				}catch(Throwable e){
					log("process.invoke", e);
					ok = false;
				}
				try{
					if(output != null){
						output.flush();
					}
				}catch(IOException e){
					ok = false;
				}
			}
			
			//We have to check if the connection closure has been requested by the application 
			//or the response stream (in case of HTTP/1.0 and keep-alive).
			if("close".equals(response.getHeader("Connection"))){
				keepAlive = false;
			}
			
			//End of request processing
			status = Constants.PROCESSOR_IDLE;

			//Recycling the request and the response objects 
			request.recycle();
			response.recycle();
		}
		
		try{
			shutdownInput(input);
			socket.close();
		}catch(IOException e){
			;
		}
	}
	
	protected void shutdownInput(InputStream input) {
		try{
			int available = input.available();
			//skip any unread (bogus) bytes
			if(available > 0){
				input.skip(available);
			}
		}catch(Throwable t){
			;
		}
	}
	
	/**
	 * Parse the incoming HTTP request headers, and set te appropriate request headers.
	 * 
	 * @param input The input stream connected to our socket
	 */
	private void parseHeaders(SocketInputStream input) throws IOException, ServletException{

		while(true){
			HttpHeader header = request.allocateHeader();
			
			//Read the next header
			input.readHeader(header);
			if(header.nameEnd == 0){
				if(header.valueEnd == 0){
					return;
				}else{
					throw new ServletException("httpProcessor.parseHeaders.colon");
				}
			}
			
			String value = new String(header.value, 0, header.valueEnd);
			
			//Set the corresponding request headers
			if(header.equals(DefaultHeaders.AUTHORIZATION_NAME)){
				request.setAuthorization(value);
			}else if(header.equals(DefaultHeaders.ACCEPT_LANGUAGE_NAME)){
				parseAcceptLanguage(value);
			}else if(header.equals(DefaultHeaders.COOKIE_NAME)){
				Cookie[] cookies = RequestUtil.parseCookieHeader(value);
				for(int i = 0; i < cookies.length; i++){
					if(cookies[i].getName().equals(Globals.SESSION_COOKIE_NAME)){
						//Override anything rquested in the URL
						if(!request.isRequestedSessionIdFromCookie()){
							//Accept only the first session id cookie
							request.setRequestedSessionId(cookies[i].getValue());
							request.setRequestedSessionCookie(true);
							request.setRequestedSessionURL(false);
							
						}
					}
					request.addCookie(cookies[i]);
				}
			}else if(header.equals(DefaultHeaders.CONTENT_LENGTH_NAME)){
				int n = -1;
				try{
					n = Integer.parseInt(value);
				}catch(Exception e){
					throw new ServletException("httpProcessor.parseHeaders.contentLength");
				}
				request.setContentLength(n);
			}else if(header.equals(DefaultHeaders.CONTENT_TYPE_NAME)){
				request.setContentType(value);
			}else if(header.equals(DefaultHeaders.HOST_MAME)){
				int n = value.indexOf(':');
				if(n < 0){
					if(connector.getScheme().equals("http")){
						request.setServerPort(80);
					}else if(connector.getScheme().equals("https")){
						request.setServerPort(443);
					}
					if(proxyName != null){
						request.setServerName(proxyName);
					}else{
						request.setServerName(value);
					}
				}else{
					if(proxyName != null){
						request.setServerName(proxyName);
					}else{
						request.setServerName(value.substring(0, n).trim());
					}
					if(proxyPort != 0){
						request.setServerPort(proxyPort);
					}else{
						int port = 80;
						try{
							port = Integer.parseInt(value.substring(n + 1).trim());
						}catch(Exception e){
							throw new ServletException("httpProcessor.parseHeaders.portNumber");
						}
						request.setServerPort(port);
					}
				}
			}else if(header.equals(DefaultHeaders.CONNECTION_NAME)){
				if(header.valueEquals(DefaultHeaders.CONNECTION_CLOSE_VALUE)){
					keepAlive = false;
					response.setHeader("Connection", "close");
				}
			}else if(header.equals(DefaultHeaders.EXPECT_NAME)){
				if(header.valueEquals(DefaultHeaders.EXPECT_100_VALUE)){
					sendAck = true;
				}else{
					throw new ServletException("httpProcessor.parseHeaders.unknownExpectation");
				}
			}else if(header.equals(DefaultHeaders.TRANSFER_ENCODING_NAME)){
				//
				
			}
			request.nextHeader();
		}
	}
	
	/**
	 * Send a confirmation that a request has been processed when pipelining.
	 * HTTP/1.1 100 Continue is sent back to the client.
	 * 
	 * @param output Socket output stream
	 * @throws IOException
	 */
	private void ackRequest(OutputStream output) throws IOException{
		if(sendAck){
			output.write(ack);
		}
	}

	/**
	 * Parse the incoming HTTP request and set the corresponding HTTP request properties.
	 * 
	 * @param input The input stream attached to our socket
	 * @param output The output stream of the socket
	 * @throws IOException 
	 * @throws ServletException 
	 */
	private void parseRequest(SocketInputStream input, OutputStream output) throws IOException, ServletException {
		
		//Parse the incoming request line
		input.readRequestLine(requestLine);
		
		//When the previous method returns, we're actually processing a request
		status = Constants.PROCESSOR_ACTIVE;
		
		String method = new String(requestLine.method, 0, requestLine.methodEnd);
		String uri = null;
		String protocol = new String(requestLine.protocol, 0, requestLine.protocolEnd);
		
		if(protocol.length() == 0){
			protocol = "HTTP/0.9";
		}
		
		//Now check if the connection should be kept alive after parsing the request
		if(protocol.equals("HTTP/1.1")){
			http11 = true;
			sendAck = false;
		}else{
			http11 = false;
			sendAck = false;
			
			//For HTTP/1.0 , connection are nor persistent by default,
			//unless specified with a Connection:Keep-Alive header.
			keepAlive = false;
		}
		
		//Validate the incoming request line
		if(method.length() < 1){
			throw new ServletException("httpProcessor.parseRequest.method");
		}else if(requestLine.uriEnd < 1){
			throw new ServletException("httpProcessor.parseRequest.uri");
		}
		
		//Parse any query parameters out of the request URI
		int question = requestLine.indexOf("?");
		if(question >= 0){
			request.setQueryString(new String(requestLine.uri,question+ 1, requestLine.uriEnd - question - 1));
			uri = new String(requestLine.uri, 0, question);
		}else{
			request.setQueryString(null);
			uri = new String(requestLine.uri, 0, requestLine.uriEnd);
		}
		
		//Checking for an absolute URI (with the HTTP protocol)
		if(!uri.startsWith("/")){
			int pos = uri.indexOf("://");
			//Parsing out protocol and host
			if(pos >= 0){
				pos = uri.indexOf("/", pos + 3);
				if(pos == -1){
					uri = "";
				}else{
					uri = uri.substring(pos);
				}
			}
		}
		
		//Parse any requested session ID out of the request URI
		int semicolon = uri.indexOf(match);
		if(semicolon >= 0){
			String rest = uri.substring(semicolon + match.length());
			int semicolon2 = rest.indexOf(";");
			if(semicolon2 >= 0){
				request.setRequestedSessionId(rest.substring(0, semicolon2));
				rest = rest.substring(semicolon2);
			}else{
				request.setRequestedSessionId(rest);
				rest = "";
			}
			request.setRequestedSessionURL(true);
			uri = uri.substring(0, semicolon) + rest;
			log(" Requested URL session id is " + ((HttpServletRequest)request.getRequest()).getRequestedSessionId());
		}else{
			request.setRequestedSessionId(null);
			request.setRequestedSessionURL(false);
		}
		
		// Normalize URI (using String operations at the moment)
		String normalizedUri = normalize(uri);
		
		//Set the corresponding request properties
		((HttpRequest) request).setMethod(method);
		request.setProtocol(protocol);
		if(normalizedUri != null){
			((HttpRequest) request).setRequestURI(normalizedUri);
		}else{
			((HttpRequest)request).setRequestURI(uri);
		}
		request.setSecure(connector.getSecure());
		request.setScheme(connector.getScheme());
		
		//
		if(normalizedUri == null){
			log(" Invalid request URI: '" + uri + "'");
			throw new ServletException("Invalid URI: '" + uri + "'");
		}
	}

	/**
	 * Parse and record the connection parameters related to this request.
	 * 
	 * @param socket
	 * @throws IOException
	 * @throws ServletException
	 */
	private void parseConnection(Socket socket) throws IOException, ServletException{
		
		log("parse Connection: address=" + socket.getInetAddress()+ ", port="+connector.getPort());
		
		((HttpRequestImpl) request).setInet(socket.getInetAddress());
		if(proxyPort != 0){
			request.setServerPort(proxyPort);
		}else{
			request.setServerPort(serverPort);
		}
		request.setSocket(socket);
	}

	public synchronized void assign(Socket socket) {
		//Wait for the Processor to get the previous  Socket
		while(this.available){
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//Store the newly available Socket and notify our thread
		this.socket = socket;
		this.available = true;
		notifyAll();
		
		if(socket != null){
			log("An incoming request is being assigned");
		}
	}
	
	
	private synchronized Socket await(){
		//Wait for the Connector to provide a new Socket 
		while(!available){
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		//Notify the Connector that we have received this socket
		Socket socket = this.socket;
		available = false;
		notifyAll();
		
		//
		log("The incoming request has been awaited");
		return socket;
	}
	
	/**
	 * Return a context-relative path, beginning with a "/", that represents the cononical version of 
	 * the specified path after ".." and "." elements are resolved out. If the specified path attempts 
	 * to go outside the boundaries of te current context (i.e. too many ".." path elements are present),
	 * return <code>null</code> instead.
	 * 
	 * @param url
	 * @return
	 */
	private String normalize(String path){

		if(path == null){
			return null;
		}
		
		//Create a place for the normalized path
		String normalized = path;
		
		//Normalize "/%7E" and "/%7e" at the beginning to "/~"
		if(normalized.startsWith("/%7E") || normalized.startsWith("/%7e")){
			normalized = "/~" + normalized.substring(4);
		}
		
		//Prevent encoding '%', '/', '.', and '\', which are special reserved characters.
		if(normalized.indexOf("%25") >= 0 			//
				|| normalized.indexOf("%2F") >= 0	//
				|| normalized.indexOf("%2E") >= 0	//
				|| normalized.indexOf("%5C") >= 0	//
				|| normalized.indexOf("2f") >= 0	//
				|| normalized.indexOf("2e") >= 0	//
				|| normalized.indexOf("2c") >= 0){
			return null;
		}
		if(normalized.equals("/.")){
			return "/";
		}
		
		//Normalize the slashes and add leading slash if necessary
		if(normalized.indexOf('\\') >= 0){
			normalized = normalized.replace('\\', '/');
		}
		if(!normalized.startsWith("/")){
			normalized = "/" + normalized;
		}
		
		//Resolve occurrences of "//" in the normalized path
		while(true){
			int index = normalized.indexOf("//");
			if(index < 0){
				break;
			}
			normalized = normalized.substring(0, index) + normalized.substring(index + 1);
		}
		
		//Resolve occurrences of "/./" in the normalized path
		while(true){
			int index = normalized.indexOf("/./");
			if(index < 0){
				break;
			}
			normalized = normalized.substring(0, index) + normalized.substring(index + 2);
		}
		
		//Resolve occurrences of "/../" in the normalized path
		while(true){
			int index = normalized.indexOf("/../");
			if(index < 0){
				break;
			}
			if(index == 0){
				return null;			//Trying to go outside our context 
			}
			int index2 = normalized.lastIndexOf('/', index - 1);
			normalized = normalized.substring(0, index2) + normalized.substring(index + 3);
		}
		
		//Declare occurrences of "/..." (three or more dots) to be invalid 
		//(on some windows platforms this walks the directory tree!!!)
		if(normalized.indexOf("/...") >= 0){
			return null;
		}
		return normalized;
	}
	
	/**
	 * Parse the value of an <code>Accept-Language</code> header, and add the 
	 * corresponding Locales to the current requet.
	 * 
	 * @param value
	 * 
	 */
	private void parseAcceptLanguage(String value){
		
		//Store the acceumulated languages that have been requested in a local collection,
		//sorted by the quality value (s we can add Locales in descending order). The values 
		//will be ArrayLists containing the corresponding Locales to be added.
		TreeMap<Double,ArrayList<Locale>> locales = new TreeMap<>();
		
		//Preprocess the value to remove all whitespace
		int white = value.indexOf(' ');
		if(white < 0){
			white = value.indexOf('\t');
		}
		if(white >= 0){
			StringBuffer sb = new StringBuffer();
			int len = value.length();
			for(int i = 0; i < len; i++){
				char ch = value.charAt(i);
				if(ch != ' ' && ch != '\t'){
					sb.append(ch);
				}
			}
			value = sb.toString();
		}
		
		
		//Process each comma-delimited language specification 
		parser.setString(value);
		int length = parser.getLength();
		while(true){
			
			//Extract the next comma-delimited entry
			int start = parser.getIndex();
			if(start >= length){
				break;
			}
			int end = parser.findChar(',');
			String entry = parser.extract(start, end).trim();
			parser.advance();	//For the following entry
			
			//Extract the quality factor for this entry
			double quality = 1.0;
			int semi = entry.indexOf(";q=");
			if(semi >= 0){
				try{
					quality = Double.parseDouble(entry.substring(semi + 3));
				}catch(NumberFormatException e){
					quality = 0.0;
				}
				entry = entry.substring(0, semi);
			}
			
			//Skip entity we are not going to keep tract of
			if(quality < 0.00005){
				continue;			//Zero (or effectively zero ) quality factors.
			}
			if("*".equals(entry)){
				continue;			//FIXME - "*" entities are not handled
			}
			
			//Extract the language and country for this entry
			String language = null;
			String country = null;
			String variant = null;
			int dash = entry.indexOf('-');
			if(dash < 0){
				language = entry;
				country = "";
				variant = "";
			}else{
				language = entry.substring(0, dash);
				country = entry.substring(dash + 1);
				int vDash = country.indexOf('-');
				if(vDash > 0){
					String cTemp = country.substring(0, vDash);
					variant = country.substring(vDash + 1);
					country = cTemp;
				}else{
					variant = "";
				}
			}
			
			//Add a new Locale to the list of Locales for this quality level
			Locale locale = new Locale(language, country, variant);
			Double key = new Double(-quality);		//Reverse the order
			ArrayList<Locale> values = locales.get(key);
			if(values == null){
				values = new ArrayList<Locale>();
				locales.put(key, values);
			}
			values.add(locale);
		}
		
		//Process the quality values in highest -> lowest order (due to 
		//negating the Double value when creating the key)
		Iterator<Double> keys = locales.keySet().iterator();
		while(keys.hasNext()){
			Double key = (Double) keys.next();
			ArrayList<Locale> list = locales.get(key);
			Iterator<Locale> values = list.iterator();
			while(values.hasNext()){
				Locale locale =  values.next();
				request.addLocale(locale);
			}
		}
		
		
	}

	private void log(String message){
		Logger logger = connector.getContainer().getLogger();
		if(logger != null){
			logger.log(this.threadName + " " + message);
		}else{
			System.out.println(this.threadName + " " + message);
		}
	}
	
	private void log(String message, Throwable t){
		Logger logger = connector.getContainer().getLogger();
		if(logger != null){
			logger.log(this.threadName + " " + message, t);
		}else{
			System.out.println(this.threadName + " " + message);
			t.printStackTrace(System.err);
		}
	}
	
	
	@Override
	public String toString() {
		return this.threadName;
	}
	
}
