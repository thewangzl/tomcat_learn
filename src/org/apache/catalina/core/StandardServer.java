package org.apache.catalina.core;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;

public class StandardServer implements Server, Lifecycle {

	// ------------------------------------------------- Instance Variables

	private int debug = 0;

	private NamingResources globalNamingResources;

	private LifecycleSupport lifecycle = new LifecycleSupport(this);

	private NamingContextListener namingContextListener;

	private int port = 8005;

	private Random random;

	private Service[] services = new Service[0];

	private String shutdown = "SHUTDOWN";

	private boolean started;

	private boolean initialized;

	private static final String info = "org.apache.catalina.core.StandardServer/1.0";

	private static final StringManager sm = StringManager.getManager(Constants.Package);

	private PropertyChangeSupport support = new PropertyChangeSupport(this);

	// --------------------------------- Properties

	@Override
	public String getInfo() {
		return info;
	}

	@Override
	public NamingResources getGlobalNamingResources() {
		return this.globalNamingResources;
	}

	@Override
	public void setGlobalNamingResources(NamingResources globalNamingResources) {
		NamingResources oldGlobalNamingResources = this.globalNamingResources;
		this.globalNamingResources = globalNamingResources;
		this.globalNamingResources.setContainer(this);
		support.firePropertyChange("globalNamingResources", oldGlobalNamingResources, this.globalNamingResources);
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String getShutdown() {

		return this.shutdown;
	}

	@Override
	public void setShutdown(String shutdown) {

		this.shutdown = shutdown;
	}

	// ------------------------------------- Server Methods

	@Override
	public void await() {
		
		// Set up a server socket to wait on
		ServerSocket serverSocket = null;
		
		try {
			serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
			
		} catch (IOException e) {
			System.err.println("StandardServer.await:create[" + port + "]:" + e);
			e.printStackTrace();
			System.exit(1);
		}
			
		// Loop waiting for a connection and a valid command 
		while(true){
			Socket socket = null;
			InputStream stream = null;
			try {
				socket = serverSocket.accept();
				socket.setSoTimeout(10 * 1000); 	// Ten seconds
				stream = socket.getInputStream();
			} catch (IOException e) {
				System.err.println("StandardServer.await:accept:" + e);
				e.printStackTrace();
				System.exit(1);
			}
			
			// Read a set of characters from the socket
			StringBuffer command = new StringBuffer();
			int expected = 1024;
			while(expected < shutdown.length()){
				if(random == null){
					random = new Random(System.currentTimeMillis());
				}
				expected += (random.nextInt() % 1024);
			}
			
			while(expected > 0){
				int ch = -1;
				try {
					ch = stream.read();
				} catch (IOException e) {
					System.err.println("StandardServer.await:read:" + e);
					e.printStackTrace();
					ch = -1;
				}
				if(ch < 32){		// Control character or EOF terminates loop
					break;
				}
				command.append((char)ch);
				expected--;
			}
			
			// Close the socket now that we are done with it
			try {
				socket.close();
			} catch (IOException e) {
				;
			}
			
			// Match against our command string
			boolean match = command.toString().equals(shutdown);
			if(match){
				break;
			}else{
				System.err.println("StandardServer.await:Invalid command'" + command.toString() +"' received" );
			}
		}
		
		// Close the server socket and return;
		try {
			serverSocket.close();
		} catch (IOException e) {
			;
		}
				

	}
	
	@Override
	public void addService(Service service) {
		service.setServer(this);
		synchronized (services) {
			Service results[] = new Service[services.length + 1];
			System.arraycopy(services, 0, results, 0, services.length);
			results[services.length] = service;
			services = results;

			if (initialized) {
				try {
					service.initialize();
				} catch (LifecycleException e) {
					e.printStackTrace(System.err);
				}
			}

			if (started && (service instanceof Lifecycle)) {
				try {
					((Lifecycle) service).start();
				} catch (LifecycleException e) {
					;
				}
			}

			// Report this property change to interested listeners
			support.firePropertyChange("service", null, service);
		}
	}

	@Override
	public Service findService(String name) {
		if (name == null) {
			return (null);
		}
		synchronized (services) {
			for (int i = 0; i < services.length; i++) {
				if (name.equals(services[i].getName())) {
					return (services[i]);
				}
			}
		}
		return null;
	}

	@Override
	public Service[] findServices() {
		return this.services;
	}

	@Override
	public void removeService(Service service) {
		synchronized (services) {
			int j = -1;
			for (int i = 0; i < services.length; i++) {
				if (service == services[i]) {
					j = i;
					break;
				}
			}
			if (j < 0)
				return;
			if (services[j] instanceof Lifecycle) {
				try {
					((Lifecycle) services[j]).stop();
				} catch (LifecycleException e) {
					;
				}
			}
			int k = 0;
			Service results[] = new Service[services.length - 1];
			for (int i = 0; i < services.length; i++) {
				if (i != j)
					results[k++] = services[i];
			}
			services = results;

			// Report this property change to interested listeners
			support.firePropertyChange("service", service, null);
		}

	}


	// ----------------------------- Public Methods

	/**
	 * Add a property change listener to this component.
	 *
	 * @param listener
	 *            The listener to add
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {

		support.addPropertyChangeListener(listener);

	}

	/**
	 * Remove a property change listener from this component.
	 *
	 * @param listener
	 *            The listener to remove
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {

		support.removePropertyChangeListener(listener);

	}

	// ----------------------------------------------------- Lifecycle Methods

	@Override
	public void start() throws LifecycleException {
		// Validate and update our current component state
		if (started)
			throw new LifecycleException(sm.getString("standardServer.start.started"));
		// Notify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

		lifecycle.fireLifecycleEvent(START_EVENT, null);
		started = true;

		// Start our defined Services
		synchronized (services) {
			for (int i = 0; i < services.length; i++) {
				if (services[i] instanceof Lifecycle) {
					((Lifecycle) services[i]).start();
				}
			}
		}

		//
		lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
	}

	@Override
	public void stop() throws LifecycleException {
		// Validate and update our current component state
		if (!started)
			throw new LifecycleException(sm.getString("standardServer.stop.notStarted"));

		// Notify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

		lifecycle.fireLifecycleEvent(STOP_EVENT, null);
		started = false;

		// Stop our defined services
		for (int i = 0; i < services.length; i++) {
			if (services[i] instanceof Lifecycle) {
				((Lifecycle) services[i]).stop();
			}
		}

		// Notify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
	}

	@Override
	public void initialize() throws LifecycleException {
		if (initialized) {
			throw new LifecycleException(sm.getString("standardServer.initialize.initialized"));
		}
		initialized = true;

		// Initialize our defined Services
		for (int i = 0; i < services.length; i++) {
			services[i].initialize();
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

}
