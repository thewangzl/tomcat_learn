package org.apache.catalina.session;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;

import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Store;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;

/**
 * Abstract implementation of the Store interface to support most the
 * functionality required by a Store.
 * 
 * @author thewangzl
 *
 */
public abstract class StoreBase implements Lifecycle, Runnable, Store {

	protected static String info = "StoreBase/1.0";

	protected int checkInterval = 60;

	protected String threadName = "StoreBase";

	protected static String storeName = "StoreBase";

	protected Thread thread = null;

	protected boolean threadDone = false;

	protected int debug = 0;

	protected boolean started = false;

	protected LifecycleSupport lifecycle = new LifecycleSupport(this);

	protected PropertyChangeSupport support = new PropertyChangeSupport(this);

	protected StringManager sm = StringManager.getManager(Constants.Package);

	protected Manager manager;

	// ----------------------------------------------------Properties

	@Override
	public String getInfo() {
		return info;
	}

	public String getThreadName() {
		return threadName;
	}

	public static String getStoreName() {
		return storeName;
	}

	public int getDebug() {
		return debug;
	}

	public void setDebug(int debug) {
		this.debug = debug;
	}

	public int getCheckInterval() {
		return checkInterval;
	}

	public void setCheckInterval(int checkInterval) {
		int oldCheckInterval = this.checkInterval;
		this.checkInterval = checkInterval;
		support.firePropertyChange("checkInterval", new Integer(oldCheckInterval), new Integer(this.checkInterval));
	}

	@Override
	public Manager getManager() {
		return manager;
	}

	@Override
	public void setManager(Manager manager) {
		Manager oldManager = this.manager;
		this.manager = manager;
		support.firePropertyChange("manager", oldManager, this.manager);
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);

	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {

		support.removePropertyChangeListener(listener);
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

	// -------------------------------------------------------Protected Methods

	/**
	 * Log a message on the Logger associated with our Container (if any).
	 *
	 * @param message
	 *            Message to be logged
	 */
	protected void log(String message) {
		Logger logger = null;
		Container container = manager.getContainer();

		if (container != null)
			logger = container.getLogger();

		if (logger != null) {
			logger.log(getStoreName() + "[" + container.getName() + "]: " + message);
		} else {
			String containerName = null;
			if (container != null)
				containerName = container.getName();
			System.out.println(getStoreName() + "[" + containerName + "]: " + message);
		}
	}

	/**
	 * Called by our background reaper thread to check if Sessions saved in our
	 * store are subject of being expired. If so expire the Session and remove
	 * it from the Store.
	 */
	protected void processExpires() {
		long timeNow = System.currentTimeMillis();
		String[] keys = null;

		if (!started) {
			return;
		}
		try {
			keys = keys();
		} catch (IOException e) {
			log(e.toString());
			return;
		}
		for (int i = 0; i < keys.length; i++) {
			try {
				StandardSession session = (StandardSession) this.load(keys[i]);
				if (!session.isValid()) {
					continue;
				}
				int maxInactiveInterval = session.getMaxInactiveInterval();
				if (maxInactiveInterval < 0) {
					continue;
				}
				int timeIdle = (int) ((timeNow - session.getLastAccessedTime()) / 1000L);
				if (timeIdle >= maxInactiveInterval) {
					if (((PersistentManagerBase) manager).isLoaded(keys[i])) {
						// recycle old backup session
						session.recycle();
					} else {
						// expire swapped out session
						session.expire();
					}
					remove(session.getId());
				}

			} catch (ClassNotFoundException e) {
				log(e.toString());
				e.printStackTrace();
			} catch (IOException e) {
				log(e.toString());
				e.printStackTrace();
			}
		}
	}

	// ------------------------------------- Thread Methods

	@Override
	public void run() {
		// Loop until the termination semaphore is set
		while (!threadDone) {
			threadSleep();
			processExpires();
		}
	}

	@Override
	public void start() throws LifecycleException {
		//Validate and update our current component state
		if (started)
			throw new LifecycleException(sm.getString(getStoreName() + ".alreadyStarted"));
		lifecycle.fireLifecycleEvent(START_EVENT, null);
		started = true;

		// Start the background reaper thread
		threadStart();
	}

	@Override
	public void stop() throws LifecycleException {
		// Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString(getStoreName()+".notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop the background reaper thread
        threadStop();
	}

	protected void threadStart() {
		if (thread != null) {
			return;
		}

		threadDone = false;
		thread = new Thread(this, getThreadName());
		thread.setDaemon(true);
		thread.start();
	}

	protected void threadStop() {
		if (thread == null) {
			return;
		}

		threadDone = true;
		thread.interrupt();
		try {
			thread.join();
		} catch (InterruptedException e) {
			;
		}
		thread = null;
	}

	protected void threadSleep() {
		try {
			Thread.sleep(checkInterval * 1000L);
		} catch (InterruptedException e) {
			;
		}
	}
}
