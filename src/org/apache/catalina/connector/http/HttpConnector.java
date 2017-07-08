package org.apache.catalina.connector.http;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessControlException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Stack;
import java.util.Vector;

import org.apache.catalina.Connector;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Service;
import org.apache.catalina.net.DefaultServerSocketFactory;
import org.apache.catalina.net.ServerSocketFactory;
import org.apache.catalina.util.LifecycleSupport;

public class HttpConnector implements Connector,Lifecycle, Runnable {

	/**
	 * The <code>Service</code> we are associated with (if any).
	 */
	private Service service;
	
	/**
	 * The accept count for this connector
	 */
	private int acceptCount = 10;
	
	/**
	 * The IP address on which to bind, if any. If <code>null</code>, all
	 * addresses on the server will be bound
	 */
	private String address;
	
	/**
	 * The input buffer size we should create on input stream.
	 */
	private int bufferSize = 2048;
	
	/**
	 * The Container used for processing requests received by this Connector.
	 */
	protected Container container;
	
	/**
	 * The set of processors that have ever been created.
	 */
	private Vector<HttpProcessor> created = new Vector<>();
	
	/**
	 * The current number of processors that have been created.
	 */
	private int curProcessors = 0;

	/**
	 * The "enable DNS lookups " flag for this Connector
	 */
	private boolean enableLookups;
	
	/**
	 * The server socket factory for this compoent.
	 */
	private ServerSocketFactory factory;

	/**
	 * Descriptive information about this Connector implementation.
	 */
	private static final String info = "org.apache.catalina.connector.http.HttpConnector";

	/**
	 * The lifecycle event support for this component.
	 */
	protected LifecycleSupport lifecycle = new LifecycleSupport(this);
	
	/**
	 * The minimum number of processors to start at initialization time.
	 */
	private int minProcessors = 5;
	
	/**
	 * The maximum number of processors allowed, or <0 for unlimited.
	 */
	private int maxProcessors = 20;
	
	/**
	 * Timeout value on the incoming connection.
	 * Note: a value of 0 means no timeout.
	 */
	private int connectionTimeout = Constants.DEFAULT_CONNECTION_TIMEOUT;
	
	/**
	 * The port number on which we listen for HTTP requests.
	 */
	private int port = 8080;
	
	/**
	 * The set of processors that have been created but are not currently
	 * being used to process a request.
	 */
	private Stack<HttpProcessor> processors = new Stack<>();

	/**
	 * The server name to which we should pretend requests to this Connector were directed.
	 * This is useful when operating Tomcat behind a proxy server, so that redirects get 
	 * constructed accurately. If not specified, the server name included in the <code>Host</code>
	 * header is used.
	 */
	private String proxyName;
	
	/**
	 * The server port to which we should pretend requests to this Connector were directed.
	 * This is useful when operating Tomcat behind a proxy server, so that redirects get 
	 * constructed accurately. If not specified, the port number included in the <code>port</code>
	 * property is used.
	 */
	private int proxyPort;
	
	/**
	 * The redirect port for non-SSL to SSL redirects.
	 */
	private int redirectePort = 443;
	
	/**
	 * The request scheme that will be set on all requests received through this connector.
	 */
	private String scheme = "http";

	/**
	 * The secure connection flag that will be set on all requests received through this connector.
	 */
	private boolean secure;
	
	/**
	 * The server socket through which we listen for incoming TCP connections.
	 */
	private ServerSocket serverSocket;
	
	/**
	 * Has this component been initialized yet? 
	 */
	private boolean initialized;
	
	/**
	 * Has this component been started yet?
	 */
	private boolean started;
	
	/**
	 * The shutdown signal to our background thread
	 */
	private boolean stopped;
	
	/**
	 * The name to register for the background thread
	 */
	private String threadName;
	
	/**
	 * The background thead.
	 */
	private Thread thread;
	
	/**
	 * The thread synchronization object.
	 */
	private Object threadSync = new Object();
	
	/**
	 * Is chunking allowed ?
	 */
	private boolean allowChunking = true;
	
	/**
	 * Use TCP no delay ?
	 */
	private boolean tcpNoDelay = true;
	
	/**
	 * The background thread that listens for incoming TCP/IP connections and 
	 * hands them off to an appropriate processor.
	 */
	@Override
	public void run() {
		//loop until we receive a shutdown  command
		while(!stopped){
			//Accept the next incoming connection from the server socket
			Socket socket = null;
			try {
				socket = serverSocket.accept();

				if(connectionTimeout > 0){
					socket.setSoTimeout(connectionTimeout);
				}
				socket.setTcpNoDelay(tcpNoDelay);
			}catch(AccessControlException e){
				log("socket accept security exception", e);
				continue;
			} catch (IOException e) {
				try {
					synchronized (threadSync) {
						if(started && !stopped){
							log("accept error:", e);
						}
						if(!started){
							serverSocket.close();
							
							serverSocket = open();
						}
					}
				} catch (IOException ioe) {
					log("socket reopen, io problem: ", ioe);
					break;
				} catch (UnrecoverableKeyException uke) {
					log("socket reopen, unrecoverable key problem: ", uke);
					break;
				} catch (KeyManagementException kme) {
					log("socket reopen, key management problem: ", kme);
					break;
				} catch (KeyStoreException kse) {
					log("socket reopen, keystore problem: ",kse);
					break;
				} catch (NoSuchAlgorithmException nsae) {
					log("socket reopen, keystore algorithm problem: ", nsae);
					break;
				} catch (CertificateException ce) {
					log("socket reopen, certificate  problem: ", ce);
					break;
				}
				continue;
			}
			
			//Hand this socket off to a appropriate processor
			HttpProcessor processor = createProcessor();
			if(processor == null){
				try {
					log("httpConnector.noProcessor");
					socket.close();
				} catch (IOException e) {
				;
				}
				continue;
			}
			processor.assign(socket);
		}
		
		//Notify the threadStop() method that we have shut ourselves down
		synchronized (threadSync) {
			threadSync.notifyAll();
		}
	}
	
	private ServerSocket open() throws UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException{
		
		//Acquire the server socket factory for this connector 
		ServerSocketFactory factory = getFactory();
		
		//If no address is specified, open a connection on all addresses
		if(address == null){
			log("httpConnector.allAddress");
			try {
				return factory.createSocket(port, acceptCount);
			} catch (BindException e) {
				throw new BindException(e.getMessage() + ": "+ port);
			}
		}
		
		//Open a server socket on the specified address
		try{
			InetAddress is = InetAddress.getByName(address);
			try {
				return factory.createSocket(port, acceptCount, is);
			} catch (BindException e) {
				throw new BindException(e.getMessage() + ": " + address + ": "+ port);
			}
		}catch(Exception e){
			log("httpConnector.noAddress" +": " + address);
			try {
				return factory.createSocket(port, acceptCount);
			} catch (BindException be) {
				throw new BindException(be.getMessage() + ": "+ port);
			}
		}
	}

	/**
	 * Create (or allocate) and return an available processor for use in processing a specific HTTP request,
	 * if possible. if this maximum allowed processors have already been created and are in use, return 
	 * <code>null</code> insteed.
	 * 
	 * @return
	 */
	private HttpProcessor createProcessor() {
		synchronized (processors) {
			if(processors.size() > 0){
				return processors.pop();
			}
			if(maxProcessors > 0 && curProcessors < maxProcessors){
				return newProcessor();
			}else{
				if(maxProcessors < 0){
					return newProcessor();
				}else {
					return null;
				}
			}
		}
	}

	@Override
	public Container getContainer() {
		return this.container;
	}

	@Override
	public void setContainer(Container container) {
		this.container = container;
	}

	@Override
	public boolean geEnableLookups() {
		return this.enableLookups;
	}

	@Override
	public void setEnableLookups(boolean enableLookups) {
		this.enableLookups = enableLookups;
	}

	@Override
	public ServerSocketFactory getFactory() {
		if(this.factory == null){
			synchronized (this) {
				this.factory = new DefaultServerSocketFactory();
			}
		}
		return this.factory;
	}

	@Override
	public void setFactory(ServerSocketFactory factory) {
		this.factory = factory;
	}

	@Override
	public String getInfo() {
		return info;
	}

	@Override
	public int getRedirectPort() {
		return this.redirectePort;
	}

	@Override
	public void setRedirectPort(int redirectPort) {
		this.redirectePort = redirectPort;
	}

	@Override
	public String getScheme() {
		return this.scheme;
	}

	@Override
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	@Override
	public boolean getSecure() {
		return this.secure;
	}

	@Override
	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	@Override
	public Service getService() {
		return this.service;
	}

	@Override
	public void setService(Service service) {
		this.service = service;
	}

	@Override
	public Request createRequest() {
		HttpRequestImpl request = new HttpRequestImpl();
		request.setConnector(this);
		return request;
	}

	@Override
	public Response createResponse() {
		HttpResponseImpl response = new HttpResponseImpl();
		response.setConnector(this);
		return response;
	}

	@Override
	public void initialize() throws LifecycleException {
		//
		if(this.initialized){
			throw new LifecycleException("httpConnector.alreadyInitialized");
		}
		this.initialized = true;
		Exception eRethrow = null;
		
		try {
			serverSocket = this.open();
		} catch (IOException ioe) {
            log("httpConnector, io problem: ", ioe);
            eRethrow = ioe;
        } catch (KeyStoreException kse) {
            log("httpConnector, keystore problem: ", kse);
            eRethrow = kse;
        } catch (NoSuchAlgorithmException nsae) {
            log("httpConnector, keystore algorithm problem: ", nsae);
            eRethrow = nsae;
        } catch (CertificateException ce) {
            log("httpConnector, certificate problem: ", ce);
            eRethrow = ce;
        } catch (UnrecoverableKeyException uke) {
            log("httpConnector, unrecoverable key: ", uke);
            eRethrow = uke;
        } catch (KeyManagementException kme) {
            log("httpConnector, key management problem: ", kme);
            eRethrow = kme;
        }
		if(eRethrow != null){
			throw new LifecycleException(threadName + ".open", eRethrow);
		}
	}
	
	/**
	 * Log a message on the Logger associated with our Container (if any).
	 * 
	 * @param message
	 */
	private void log(String message){
		Logger logger = container.getLogger();
		String localName = threadName;
		if(localName == null){
			localName = "HttpConnector";
		}
		if(logger != null){
			logger.log(localName + " " + message);
		}else{
			System.out.println(localName + " " + message);
		}
		
	}
	
	/**
	 * Log a message on the Logger associated with our Container (if any).
	 * 
	 * @param message
	 * @param t
	 */
	private void log(String message, Throwable t){
		Logger logger = container.getLogger();
		String localName = threadName;
		if(localName == null){
			localName = "HttpConnector";
		}
		if(logger != null){
			logger.log(localName + " " + message, t);
		}else{
			System.out.println(localName + " " + message);
			t.printStackTrace(System.err);
		}
	}
	

	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		lifecycle.addLifecycleListener(listener);
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		
		return lifecycle.findLifecycleListeners();
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		lifecycle.removeLifecycleListener(listener);
	}

	@Override
	public void start() throws LifecycleException {
		if(this.started){
			throw new LifecycleException("httpConnector.alreadyStarted");
		}
		this.threadName = "HttpConnector[" + port + "]";
		lifecycle.fireLifecycleEvent(START_EVENT, null);
		
		started = true;
		
		//start out background thread
		threadStart();
		
		//Create the specified minimum number of process
		while(curProcessors < minProcessors){
			if(maxProcessors > 0 && curProcessors >= maxProcessors){
				break;
			}
			HttpProcessor processor = newProcessor();
			recycle(processor);
		}
		
	}
	
	/**
	 * Start the background processing thread
	 */
	private void threadStart(){
		log("httpConnector.starting");
		
		thread = new Thread(this, threadName);
		thread.setDaemon(true);
		thread.start();
	}
	
	/**
	 * Stop the background processing thread.
	 */
	private void threadStop(){
		log("httpConnector.stopping");
		
		stopped = true;
		try {
			threadSync.wait(5000);
		} catch (InterruptedException e) {
			;
		}
		thread = null;
	}
	
	/**
	 * Create and return a new processor suitable for processoring HTTP
	 * requests and returning the corresponding responses.
	 * @return
	 */
	private HttpProcessor newProcessor(){
		HttpProcessor processor = new HttpProcessor(this, curProcessors++);
		if(processor instanceof Lifecycle){
			try {
				((Lifecycle) processor).start();
			} catch (LifecycleException e) {
				log("newProcessor", e);
				return null;
			}
		}
		created.addElement(processor);
		return processor;
	}
	
	void recycle(HttpProcessor processor){
		this.processors.push(processor);
	}

	/**
	 * Terminate processing requests via this Connector.
	 * 
	 * @throws LifecycleException
	 */
	@Override
	public void stop() throws LifecycleException {
		
		//Validate and update our current state
		if(!started){
			throw new LifecycleException("httpConnector.notStarted");
		}
		lifecycle.fireLifecycleEvent(STOP_EVENT, null);
		started = false;
		
		//Gracefully shut down all processors we have created
		for(int i = created.size() - 1; i >= 0; i--){
			HttpProcessor processor = (HttpProcessor) created.elementAt(i);
			if(processor instanceof Lifecycle){
				try{
					((Lifecycle)processor).stop();
				}catch(LifecycleException e){
					log("httpConnector.stop", e);
				}
			}
		}
		
		synchronized (threadSync) {
			//Close the server socket we were using
			if(serverSocket != null){
				try {
					serverSocket.close();
				} catch (IOException e) {
					;
				}
			}
			//Stop our background thread
			threadStop();
		}
		serverSocket = null;
	}

	
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	
	public int getCurProcessors() {
		return curProcessors;
	}
	
	public int getMinProcessors() {
		return minProcessors;
	}
	
	public void setMinProcessors(int minProcessors) {
		this.minProcessors = minProcessors;
	}
	
	public int getMaxProcessors() {
		return maxProcessors;
	}
	public void setMaxProcessors(int maxProcessors) {
		this.maxProcessors = maxProcessors;
	}

	public int getBufferSize() {
		return bufferSize;
	}
	
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}
	
	/**
	 * Get te allow chunking flag.
	 * @return
	 */
	public boolean isChunkingAllowed(){
		return this.allowChunking;
	}

	public void setAllowChunking(boolean allowChunking) {
		this.allowChunking = allowChunking;
	}
	
	public String getProxyName() {
		return proxyName;
	}
	
	public void setProxyName(String proxyName) {
		this.proxyName = proxyName;
	}
	
	public int getProxyPort() {
		return proxyPort;
	}
	
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}
	
	public int getAcceptCount() {
		return acceptCount;
	}
	
	public void setAcceptCount(int acceptCount) {
		this.acceptCount = acceptCount;
	}
	
	public int getConnectionTimeout() {
		return connectionTimeout;
	}
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}
	
	public int getRedirectePort() {
		return redirectePort;
	}
	
	public void setRedirectePort(int redirectePort) {
		this.redirectePort = redirectePort;
	}
	
	
	
}
