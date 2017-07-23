package org.apache.catalina.core;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.apache.catalina.Connector;
import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;

public class StandardService implements Service, Lifecycle {

	private Container container;

	private Connector[] connectors = new Connector[0];

	private int debug = 0;

	private String name;

	private boolean started;

	private boolean initialized;

	private Server server;

	private static final String info = "org.apache.catalina.core.StandardService/1.0";

	private LifecycleSupport lifecycle = new LifecycleSupport(this);

	private static final StringManager sm = StringManager.getManager(Constants.Package);

	protected PropertyChangeSupport support = new PropertyChangeSupport(this);

	@Override
	public Container getContainer() {

		return this.container;
	}

	@Override
	public void setContainer(Container container) {
		Container oldContainer = this.container;
		if ((oldContainer != null) && (oldContainer instanceof Engine))
			((Engine) oldContainer).setService(null);
		this.container = container;
		if ((this.container != null) && (this.container instanceof Engine))
			((Engine) this.container).setService(this);
		if (started && (this.container != null) && (this.container instanceof Lifecycle)) {
			try {
				((Lifecycle) this.container).start();
			} catch (LifecycleException e) {
				;
			}
		}
		synchronized (connectors) {
			for (int i = 0; i < connectors.length; i++)
				connectors[i].setContainer(this.container);
		}
		if (started && (oldContainer != null) && (oldContainer instanceof Lifecycle)) {
			try {
				((Lifecycle) oldContainer).stop();
			} catch (LifecycleException e) {
				;
			}
		}

		// Report this property change to interested listeners
		support.firePropertyChange("container", oldContainer, this.container);

	}

	public int getDebug() {
		return debug;
	}

	public void setDebug(int debug) {
		this.debug = debug;
	}

	@Override
	public String getInfo() {
		return info;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Server getServer() {
		return server;
	}

	@Override
	public void setServer(Server server) {

		this.server = server;
	}

	@Override
	public void addConnector(Connector connector) {
		synchronized (connectors) {
			connector.setContainer(this.container);
			connector.setService(this);
			Connector results[] = new Connector[connectors.length + 1];
			System.arraycopy(connectors, 0, results, 0, connectors.length);
			results[connectors.length] = connector;
			connectors = results;

			if (initialized) {
				try {
					connector.initialize();
				} catch (LifecycleException e) {
					e.printStackTrace(System.err);
				}
			}

			if (started && (connector instanceof Lifecycle)) {
				try {
					((Lifecycle) connector).start();
				} catch (LifecycleException e) {
					;
				}
			}

			// Report this property change to interested listeners
			support.firePropertyChange("connector", null, connector);
		}
	}

	@Override
	public Connector[] findConnectors() {
		return connectors;
	}

	@Override
	public void removeConnector(Connector connector) {
		synchronized (connectors) {
			int j = -1;
			for (int i = 0; i < connectors.length; i++) {
				if (connector == connectors[i]) {
					j = i;
					break;
				}
			}
			if (j < 0)
				return;
			if (started && (connectors[j] instanceof Lifecycle)) {
				try {
					((Lifecycle) connectors[j]).stop();
				} catch (LifecycleException e) {
					;
				}
			}
			connectors[j].setContainer(null);
			connector.setService(null);
			int k = 0;
			Connector results[] = new Connector[connectors.length - 1];
			for (int i = 0; i < connectors.length; i++) {
				if (i != j)
					results[k++] = connectors[i];
			}
			connectors = results;

			// Report this property change to interested listeners
			support.firePropertyChange("connector", connector, null);
		}

	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {

		support.addPropertyChangeListener(listener);

	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {

		support.removePropertyChangeListener(listener);

	}

	// ----------------------------------------------- Lifecycle Methods

	@Override
	public void initialize() throws LifecycleException {
		if (initialized)
			throw new LifecycleException(sm.getString("standardService.initialize.initialized"));
		initialized = true;

		//
		synchronized (connectors) {
			for (int i = 0; i < connectors.length; i++) {
				connectors[i].initialize();
			}
		}
	}

	@Override
	public void start() throws LifecycleException {
		// Validate and update our current component state
		if (started) {
			throw new LifecycleException(sm.getString("standardService.start.started"));
		}

		// Notify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

		System.out.println(sm.getString("standardService.start.name", this.name));
		lifecycle.fireLifecycleEvent(START_EVENT, null);
		started = true;

		// Start our defined Container first
		if (container != null) {
			synchronized (container) {
				if (container instanceof Lifecycle) {
					((Lifecycle) container).start();
				}
			}
		}

		// Start our defined connectors second
		synchronized (connectors) {
			for (int i = 0; i < connectors.length; i++) {
				if (connectors[i] instanceof Lifecycle) {
					((Lifecycle) connectors[i]).start();
				}
			}
		}
		// Notify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
	}

	@Override
	public void stop() throws LifecycleException {
		// Validate and update our current component state
		if (!started) {
			throw new LifecycleException(sm.getString("standardService.stop.notStarted"));
		}

		// Notify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

		lifecycle.fireLifecycleEvent(STOP_EVENT, null);

		System.out.println(sm.getString("standardService.stop.name", this.name));
		started = false;

		// Stop our defined connectors first
		synchronized (connectors) {
			for (int i = 0; i < connectors.length; i++) {
				if (connectors[i] instanceof Lifecycle) {
					((Lifecycle) connectors[i]).stop();
				}
			}
		}

		// Stop our defined Container second
		if (container != null) {
			synchronized (container) {
				if (container instanceof Lifecycle) {
					((Lifecycle) container).stop();
				}
			}
		}

		// Notify our interested LifecycleListeners
		lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
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
