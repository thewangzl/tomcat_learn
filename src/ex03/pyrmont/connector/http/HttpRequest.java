package ex03.pyrmont.connector.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.ParameterMap;
import org.apache.catalina.util.RequestUtil;

import ex03.pyrmont.connector.RequestStream;

public class HttpRequest implements HttpServletRequest {

	private String contentType;
	private int contentLength;
	private String method;
	private String protocol;
	private String queryString;
	private String requestURI;
	private boolean requestedSessionCookie;
	private String requestedSessionId;
	private boolean requestedSessionURL;

	private SocketInputStream input;

	private HashMap headers = new HashMap<>();
	private ArrayList<Cookie> cookies = new ArrayList<>();
	private ParameterMap parameters = null;

	// have the parameters for this request bean parsed yet?
	protected boolean parsed = false;
	private String pathInfo;

	private HashMap attributes = new HashMap<>();

	private String authorization;

	private String contextPath = "";

	protected BufferedReader reader = null;

	protected ServletInputStream stream;

	/**
	 * An empty collection to use for returning empty Enumerations. Do not add
	 * any elements to this collection!
	 */
	protected static ArrayList empty = new ArrayList();

	/**
	 * The set of SimpleDateFormat formats to use in getDateHeader().
	 */
	protected SimpleDateFormat formats[] = { new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
			new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
			new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US) };

	public HttpRequest(SocketInputStream input) {
		this.input = input;
	}

	public void setRequestURI(String uri) {
		this.requestURI = uri;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public void setRequestedSessionId(String requestedSessionId) {
		this.requestedSessionId = requestedSessionId;
	}

	public void setRequestedSessionURL(boolean requestedSessionURL) {
		this.requestedSessionURL = requestedSessionURL;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void addHeader(String name, String value) {
		name = name.toLowerCase();
		synchronized (headers) {
			ArrayList values = (ArrayList) this.headers.get(name);
			if (values == null) {
				values = new ArrayList<>();
				headers.put(name, values);
			}
			values.add(value);
		}
	}

	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setRequestedSessionCookie(boolean requestedSessionCookie) {
		this.requestedSessionCookie = requestedSessionCookie;
	}

	public void addCookie(Cookie cookie) {
		synchronized (cookie) {
			this.cookies.add(cookie);
		}
	}

	protected void parseParameters() {
		if (parsed) {
			return;
		}
		ParameterMap results = parameters;
		if (results == null) {
			results = new ParameterMap<>();
		}
		results.setLocked(false);
		String encoding = this.getCharacterEncoding();
		if (encoding == null) {
			encoding = "UTF-8";
		}
		// Parse any parameters specified in the query string
		String queryString = this.getQueryString();
		try {
			RequestUtil.parseParameters(results, queryString, encoding);
		} catch (UnsupportedEncodingException e) {
			;
		}
		// Parse any parameters specified in the input stream
		String contentType = this.getContentType();
		if (contentType == null) {
			contentType = "";
		}
		int semicolon = contentType.indexOf(";");
		if (semicolon >= 0) {
			contentType = contentType.substring(0, semicolon).trim();
		} else {
			contentType = contentType.trim();
		}
		//
		if ("POST".equals(this.getMethod()) && this.getContentLength() > 0
				&& "application/x-www-form-urlencoded".equals(contentType)) {
			try {
				int max = this.getContentLength();
				int len = 0;
				byte[] buf = new byte[max];
				ServletInputStream is = this.getInputStream();
				while (len < max) {
					int next = is.read(buf, len, max - len);
					if (next < 0) {
						break;
					}
					len += next;
				}
				is.close();
				if (len < max) {
					throw new RuntimeException("Content length mismatch");
				}
				RequestUtil.parseParameters(results, buf, encoding);
			} catch (UnsupportedEncodingException e) {
				;
			} catch (IOException e) {
				throw new RuntimeException("Content read fail");
			}
		}
		results.setLocked(true);
		parsed = true;
		parameters = results;
	}

	public ServletInputStream createInputStream() {
		return new RequestStream(this);
	}

	public InputStream getStream() {
		return this.input;
	}

	@Override
	public Object getAttribute(String name) {
		synchronized (attributes) {
			return attributes.get(name);
		}
	}

	@Override
	public Enumeration getAttributeNames() {
		synchronized (attributes) {
			return new Enumerator<>(this.attributes);
		}
	}

	@Override
	public String getCharacterEncoding() {
		return null;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (reader != null) {
			throw new IllegalStateException("getInputStream has beeen called");
		}
		if (stream == null) {
			stream = createInputStream();
		}
		return stream;
	}

	@Override
	public Locale getLocale() {

		return null;
	}

	@Override
	public Enumeration getLocales() {
		return null;
	}

	@Override
	public String getParameter(String name) {
		this.parseParameters();
		String[] values = (String[]) this.parameters.get(name);
		if (values != null) {
			return values[0];
		}
		return null;
	}

	@Override
	public Map getParameterMap() {
		parseParameters();
		return this.parameters;
	}

	@Override
	public Enumeration getParameterNames() {
		parseParameters();
		return new Enumerator<>(this.parameters.keySet());
	}

	@Override
	public String[] getParameterValues(String name) {
		this.parseParameters();
		return (String[]) this.parameters.get(name);
	}

	@Override
	public String getProtocol() {
		return this.protocol;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (this.stream != null) {
			throw new IllegalStateException("getInputStream has been called");
		}
		if (this.reader == null) {
			String encoding = this.getCharacterEncoding();
			if (encoding == null) {
				encoding = "UTF-8";
			}
			InputStreamReader isr = new InputStreamReader(createInputStream(), encoding);
			this.reader = new BufferedReader(isr);
		}
		return this.reader;
	}

	@Override
	public String getRealPath(String arg0) {
		return null;
	}

	@Override
	public String getRemoteAddr() {
		return null;
	}

	@Override
	public String getRemoteHost() {
		return null;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		return null;
	}

	@Override
	public String getScheme() {
		return null;
	}

	@Override
	public String getServerName() {
		return null;
	}

	@Override
	public int getServerPort() {
		return 0;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public void removeAttribute(String arg0) {

	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
	}

	@Override
	public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {

	}

	@Override
	public String getAuthType() {
		return null;
	}

	@Override
	public String getContextPath() {
		return this.contextPath;
	}

	@Override
	public Cookie[] getCookies() {
		synchronized (cookies) {
			if (cookies.size() < 1) {
				return null;
			}
			return cookies.toArray(new Cookie[cookies.size()]);
		}
	}

	@Override
	public long getDateHeader(String name) {
		String value = this.getHeader(name);
		if (value == null) {
			return -1L;
		}
		for (int i = 0; i < formats.length; i++) {
			try{
				Date date = formats[i].parse(value);
				return date.getTime();
			}catch(ParseException e){
				;
			}
		}
		throw new IllegalArgumentException(value);
	}

	@Override
	public String getHeader(String name) {
		name = name.toLowerCase();
		synchronized (headers) {
			ArrayList values = (ArrayList) headers.get(name);
			if (values != null) {
				return (String) values.get(0);
			}
			return null;
		}
	}

	@Override
	public Enumeration getHeaderNames() {
		synchronized (headers) {
			return new Enumerator<>(headers.keySet());
		}
	}

	@Override
	public Enumeration getHeaders(String name) {
		name = name.toLowerCase();
		synchronized (headers) {
			ArrayList values = (ArrayList) headers.get(name);
			if (values != null) {
				return new Enumerator<>(values);
			}
			return new Enumerator<>(empty);
		}
	}

	@Override
	public int getIntHeader(String name) {
		String value = this.getHeader(name);
		if (value != null) {
			return Integer.parseInt(value);
		}
		return -1;
	}

	@Override
	public String getMethod() {
		return this.method;
	}

	@Override
	public String getPathInfo() {
		return this.pathInfo;
	}

	@Override
	public String getPathTranslated() {

		return null;
	}

	@Override
	public String getQueryString() {
		return this.queryString;
	}

	@Override
	public String getRemoteUser() {

		return null;
	}

	@Override
	public String getRequestURI() {
		return this.requestURI;
	}

	@Override
	public StringBuffer getRequestURL() {
		return null;
	}

	@Override
	public String getRequestedSessionId() {
		return this.requestedSessionId;
	}

	@Override
	public String getServletPath() {

		return null;
	}

	@Override
	public HttpSession getSession() {
		return null;
	}

	@Override
	public HttpSession getSession(boolean create) {
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		return null;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return this.isRequestedSessionIdFromUrl();
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return false;
	}

	@Override
	public boolean isUserInRole(String arg0) {
		return false;
	}

	@Override
	public int getContentLength() {
		return this.contentLength;
	}

	@Override
	public String getContentType() {
		return this.contentType;
	}

}
