package org.apache.catalina.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Wrapper;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.RequestUtil;

/**
 * Convenience base implementation of the <b>Request</b> interface, which can be userd for 
 * Request implementation required by most Connectors.Only the container-specific methods
 * needs to be implemented.
 * @author lenovo
 *
 */
public abstract class RequestBase implements Request, ServletRequest {

	/**
	 * The attributes associated with this request, keyed by attribute name.
	 */
	protected HashMap<String,Object> attributes = new HashMap<>();
	
	protected String authorization;
	
	protected String characterEncoding;
	
	protected Connector connector;
	
	protected int contentLength = -1;
	
	protected String contentType;
	
	protected Context context;
	
	protected static Locale defaultLocale = Locale.getDefault();
	
	protected RequestFacade facade = new RequestFacade(this);
	
	protected static final String info = "org.apache.catalina.connector.RequestBase/1.0";
	
	protected InputStream input;
	
	protected ArrayList<Locale> locales = new ArrayList<>();
	
	private transient HashMap<String,Object> notes = new HashMap<>();
	
	protected String protocol;
	
	protected BufferedReader reader;
	
	protected String remoteAddr;
	
	protected String remoteHost;
	
	protected Response response;
	
	protected String scheme;
	
	protected boolean secure;
	
	protected String serverName;
	
	protected int serverPort = -1;
	
	protected Socket socket;
	
	protected ServletInputStream stream;
	
	protected Wrapper wrapper;
	
	@Override
	public String getAuthorization() {
		return this.authorization;
	}
	
	@Override
	public void setAuthorization(String authorization) {
		this.authorization = authorization;
	}
	
	@Override
	public Object getAttribute(String name) {
		synchronized (attributes) {
			return attributes.get(name);
		}
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		synchronized (attributes) {
			return new Enumerator<>(attributes.keySet());
		}
	}

	@Override
	public String getCharacterEncoding() {
		return this.characterEncoding;
	}

	@Override
	public int getContentLength() {

		return this.contentLength;
	}

	@Override
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if(reader != null){
			throw new IOException("requestBase.getInputStream.ise");
		}
		if(stream == null){
			stream = this.createInputStream();
		}
		return stream;
	}

	@Override
	public Locale getLocale() {
		synchronized (locales) {
			if(locales.size() > 0){
				return (Locale) locales.get(0);
			}else{
				return defaultLocale;
			}
			
		}
	}

	@Override
	public Enumeration<Locale> getLocales() {
		synchronized (locales) {
			if(locales.size() > 0){
				return new Enumerator<>(locales);
			}
		}
		ArrayList<Locale> results = new ArrayList<>();
		results.add(defaultLocale);
		return new Enumerator<>(results);
	}

	/**
	 * Return the value of the specified request parameter, if any; otherwise, 
	 * return <code>null</code>. If there is more than one value defined, return only the first one.
	 */
	@Override
	public abstract String getParameter(String name);

	/**
	 * Return a <code>Map</code> of the parameters of this request. Request parameters are 
	 * extra information sent with the request. For HTTP servlets, parameters are containered 
	 * in the query string or posted from data.
	 * 
	 */
	@Override
	public abstract Map<String,String[]> getParameterMap();

	/**
	 * Return the names of all defined request parameters for this request.
	 */
	@Override
	public abstract Enumeration<String> getParameterNames();

	/**
	 * Return the defined values for the specified request parameter, if any;
	 * otherwise, return <code>null</code>
	 */
	@Override
	public abstract String[] getParameterValues(String name) ;

	@Override
	public String getProtocol() {
		return this.protocol;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if(stream != null){
			throw new IllegalStateException("requestBase.getReader.ise");
		}
		if(reader == null){
			String encoding = this.getCharacterEncoding();
			if(encoding == null){
				encoding = "UTF-8";
			}
			InputStreamReader isr = new InputStreamReader(createInputStream(),encoding);
			reader = new BufferedReader(isr);
		}
		return reader;
	}

	@Override
	public String getRealPath(String path) {
		if(context == null){
			return null;
		}
		ServletContext servletContext = context.getServletContext();
		if(servletContext == null){
			return null;
		}else{
			try{
				return servletContext.getRealPath(path);
			}catch(IllegalArgumentException e){
				return null;
			}
		}
	}

	@Override
	public String getRemoteAddr() {
		return this.remoteAddr;
	}

	@Override
	public String getRemoteHost() {
		return this.remoteHost;
	}

	/**
	 * Return a RequestDispatcher that wraps the resource at the path,
	 * which may be interpreted as relative to the current request path.
	 * 
	 * @param path Path of the resource to be wrapped
	 * @return
	 */
	@Override
	public abstract RequestDispatcher getRequestDispatcher(String path);

	@Override
	public String getScheme() {
		return this.scheme;
	}

	@Override
	public String getServerName() {
		return this.serverName;
	}

	@Override
	public int getServerPort() {
		return this.serverPort;
	}

	@Override
	public boolean isSecure() {
		return this.secure;
	}

	@Override
	public void removeAttribute(String name) {
		synchronized (attributes) {
			attributes.remove(name);
		}
	}

	@Override
	public void setAttribute(String name, Object value) {
		//
		if(name == null){
			throw new IllegalArgumentException("requestBase.setAttribute.namenull");
		}
		// null value is the same as removeAttribute()
		if(value == null){
			this.removeAttribute(name);
		}
		synchronized (attributes) {
			attributes.put(name, value);
		}
	}

	/**
	 * 
	 */
	@Override
	public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException {

		//ensure that the specified encoding is Valid
		byte[] buffer = new byte[1];
		buffer[0] = (byte)'a';
		@SuppressWarnings("unused")
		String dummy = new String(buffer, encoding);
		
		//Save the validated encoding
		this.characterEncoding = encoding;
	}

	@Override
	public Connector getConnector() {
		return this.connector;
	}

	@Override
	public void setConnector(Connector connector) {
		this.connector= connector;
	}

	@Override
	public String getInfo() {
		return info;
	}

	@Override
	public ServletRequest getRequest() {
		return this.facade;
	}

	@Override
	public Response getResponse() {
		return this.response;
	}

	@Override
	public void setResponse(Response response) {
		this.response = response;
	}
	
	@Override
	public Socket getSocket() {
		return this.socket;
	}
	@Override
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	@Override
	public InputStream getStream() {
		return this.input;
	}
	
	@Override
	public void setStream(InputStream input) {
		this.input = input;
	}
	
	@Override
	public Wrapper getWrapper() {
		return this.wrapper;
	}
	
	public void setWrapper(Wrapper wrapper) {
		this.wrapper = wrapper;
	}

	public Context getContext() {
		return context;
	}
	
	public void setContext(Context context) {
		this.context = context;
	}
	/**
	 * 
	 */
	@Override
	public ServletInputStream createInputStream() throws IOException {

		return new RequestStream(this);
	}
	
	/**
	 * 
	 */
	@Override
	public void finishRequest() throws IOException {
		if(reader != null){
			reader.close();
		}
		if(stream != null){
			stream.close();
		}
	}
	
	@Override
	public void setNote(String name, Object note) {
		synchronized (notes) {
			notes.put(name, note);
		}
	}
	
	@Override
	public Object getNote(String name) {
		synchronized (notes) {
			return notes.get(name);
		}
	}
	
	@Override
	public Iterator<String> getNoteNames() {
		synchronized (notes) {
			return notes.keySet().iterator();
		}
	}
	
	@Override
	public void removeNote(String name) {
		synchronized (notes) {
			notes.remove(name);
		}
	}
	
	@Override
	public void recycle() {
		attributes.clear();
		this.authorization = null;
		this.characterEncoding = null;
		//
		contentLength = -1;
		contentType = null;
		context = null;
		input = null;
		locales.clear();
		notes.clear();
		protocol = null;
		reader = null;
		remoteAddr = null;
		remoteHost = null;
		response = null;
		scheme = null;
		secure = false;
		serverName = null;
		serverPort = -1;
		socket = null;
		stream = null;
		wrapper = null;
	}
	
	@Override
	public void setContentLength(int length) {
		this.contentLength = length;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
		if(contentType.indexOf(';') >= 0){
			characterEncoding = RequestUtil.parseCharacterEncoding(contentType);
		}
	}
	
	@Override
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	@Override
	public void setRemoteAddr(String remote) {
		this.remoteAddr = remote;
	}
	
	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}
	
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}
	
	public void setSecure(boolean secure) {
		this.secure = secure;
	}
	
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
	
	public void addLocale(Locale locale){
		synchronized (locales) {
			locales.add(locale);
		}
	}
	
}
