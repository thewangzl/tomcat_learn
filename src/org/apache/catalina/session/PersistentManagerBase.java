package org.apache.catalina.session;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.catalina.util.LifecycleSupport;

public class PersistentManagerBase extends ManagerBase implements Lifecycle, PropertyChangeListener, Runnable {

	private int checkInterval = 60;

	private int maxActiveSessions = -1;

	protected static String name = "PersistentManagerBase";

	private boolean started = false;

	private Thread thread;

	private boolean threadDone;

	private String threadName = "PersistentManagerBase";

	private Store store;

	/**
	 * Whether to save and reload sessions when the Manager <code>unload</code>
	 * and <code>load</code> methods are called.
	 */
	private boolean saveOnRestart = true;

	/**
	 * How long a session must be idle before it should be backed up. -1 meaans
	 * sessions won't be backed up.
	 */
	private int maxIdleBackup = -1;

	/**
	 * Minumum time a session must be idle before it is swapped to disk. This
	 * overrides maxActiveSessions, to prevent thrashing if there are lots of
	 * active sessions. Setting to -1 means it's ignored.
	 */
	private int minIdleSwap = -1;

	/**
	 * The maximum time a session may be idle before it should be swapped to
	 * file just on general principle. Setting this to -1 means sessions should
	 * not be forced out.
	 */
	private int maxIdleSwap = -1;

	private static final String info = "PersistentManagerBase/1.0";

	protected LifecycleSupport lifecycle = new LifecycleSupport(this);

	// -------------------------------------------- Properties

	public int getCheckInterval() {
		return checkInterval;
	}

	public void setCheckInterval(int checkInterval) {
		int oldCheckInterval = this.checkInterval;
		this.checkInterval = checkInterval;
		support.firePropertyChange("checkInterval", new Integer(oldCheckInterval), new Integer(this.checkInterval));
	}

	public int getMaxIdleBackup() {
		return maxIdleBackup;
	}

	public void setMaxIdleBackup(int backup) {
		if (backup == this.maxIdleBackup)
			return;
		int oldBackup = this.maxIdleBackup;
		this.maxIdleBackup = backup;
		support.firePropertyChange("maxIdleBackup", new Integer(oldBackup), new Integer(this.maxIdleBackup));
	}

	public int getMaxIdleSwap() {
		return maxIdleSwap;
	}

	public void setMaxIdleSwap(int max) {
		if (max == this.maxIdleSwap)
			return;
		int oldMaxIdleSwap = this.maxIdleSwap;
		this.maxIdleSwap = max;
		support.firePropertyChange("maxIdleSwap", new Integer(oldMaxIdleSwap), new Integer(this.maxIdleSwap));
	}

	public int getMinIdleSwap() {
		return minIdleSwap;
	}

	public void setMinIdleSwap(int min) {
		if (this.minIdleSwap == min)
			return;
		int oldMinIdleSwap = this.minIdleSwap;
		this.minIdleSwap = min;
		support.firePropertyChange("minIdleSwap", new Integer(oldMinIdleSwap), new Integer(this.minIdleSwap));
	}

	public String getInfo() {
		return info;
	}

	public int getMaxActiveSessions() {
		return maxActiveSessions;
	}

	public void setMaxActiveSessions(int max) {
		int oldMaxActiveSessions = this.maxActiveSessions;
		this.maxActiveSessions = max;
		support.firePropertyChange("maxActiveSessions", new Integer(oldMaxActiveSessions),
				new Integer(this.maxActiveSessions));
	}

	@Override
	public String getName() {
		return name;
	}

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}

	public void setStore(Store store) {
		this.store = store;
		store.setManager(this);
	}

	public Store getStore() {
		return store;
	}

	public boolean getSaveOnRestart() {
		return saveOnRestart;
	}

	public void setSaveOnRestart(boolean saveOnRestart) {
		if (saveOnRestart == this.saveOnRestart)
			return;

		boolean oldSaveOnRestart = this.saveOnRestart;
		this.saveOnRestart = saveOnRestart;
		support.firePropertyChange("saveOnRestart", new Boolean(oldSaveOnRestart), new Boolean(this.saveOnRestart));
	}

	@Override
	public void setContainer(Container container) {

		// De-register from the old Container (if any)
		if (this.container != null && this.container instanceof Context) {
			((Context) this.container).removePropertyChangeListener(this);
		}

		// Default processing provided by out superclass
		super.setContainer(container);

		// Register with the new Container(if any)
		if (this.container != null && this.container instanceof Context) {
			setMaxInactiveInterval(((Context) this.container).getSessionTimeout() * 60);
			((Context) this.container).addPropertyChangeListener(this);
		}
	}

	// ------------------------------------------------------ Public Methods

	@Override
	public void load() {

		// Initialize our internal data structures
		recycled.clear();
		sessions.clear();

		if (store == null) {
			return;
		}

		String[] ids = null;
		try {
			ids = store.keys();
		} catch (IOException e) {
			log("Can't load sessions from store, " + e.getMessage(), e);
			return;
		}

		int n = ids.length;
		if (n == 0) {
			return;
		}

		if (debug >= 1) {
			log(sm.getString("persistentManager.loading", String.valueOf(n)));
		}

		for (int i = 0; i < n; i++) {
			try {
				swapIn(ids[i]);
			} catch (IOException e) {
				log("Failed load session from store, " + e.getMessage(), e);
			}
		}
	}

	@Override
	public void unload() {
		if (store == null) {
			return;
		}

		Session[] sessions = findSessions();
		int n = sessions.length;
		if (n == 0) {
			return;
		}

		if (debug >= 1) {
			log(sm.getString("persistentManager.unloading", String.valueOf(n)));
		}

		for (int i = 0; i < n; i++) {
			try {
				swapOut(sessions[i]);
			} catch (IOException e) {
				;
			}
		}
	}

	/**
	 * Clear all sessions from the store
	 */
	public void clearStore() {
		if (store == null) {
			return;
		}
		try {
			store.clear();
		} catch (IOException e) {
			log("Exception clearing the Store: " + e);
			e.printStackTrace();
		}
	}

	/**
	 * Called by the background thread after active sessions have been checked for expiration,
	 * to allow sessions to be swapped out, backed up, etc.
	 */
	public void processPersistenceChecks() {
		processMaxIdleSwaps();
		processMaxActiveSwaps();
		processMaxIdleBackups();
	}
	
	@Override
	public Session createSession() {
		if(maxActiveSessions >= 0 && sessions.size() > maxActiveSessions){
			throw new IllegalStateException(sm.getString("standardManager.createSession.ise"));
		}
		return super.createSession();
	}
	
	/**
	 * Return true,if the session id is loaded in memory,otherwise false is returned
	 * 
	 * @param id
	 * @return
	 */
	public boolean isLoaded(String id){
		try {
			if(super.findSession(id)  != null){
				return true;
			}
		} catch (IOException e) {
			log("checking isLoaded for id, " + id + ", "+e.getMessage(), e);
		}
		return false;
	}
	
	/**
	 * Return the active session,associated with this Manager, with the specified session id (if any);
	 * otherwise return <code>null</code>.This method checks the persistence store if persistence is enabled,
	 * otherwise just used the functionality from ManagerBase.
	 * 
	 */
	public Session findSession(String id) throws IOException {
		
		Session session = super.findSession(id);
		if(session != null){
			return session;
		}
		
		//See if the Session is in the Store
		session = swapIn(id);
		return session;
	}

	// -------------------------------------------Lifecycle Methods

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

	@SuppressWarnings("unused")
	@Override
	public void start() throws LifecycleException {

		if (debug >= 1) {
			log("Stating");
		}
		// Validate and update our current component state
		if (started) {
			throw new LifecycleException(sm.getString("standardManager.alreadyStarted"));
		}
		lifecycle.fireLifecycleEvent(START_EVENT, null);
		setStarted(true);

		// Force initialization of the random number generator
		if (debug >= 1)
			log("Force random number initialization starting");
		String dummy = generateSessionId();
		if (debug >= 1)
			log("Force random number initialization completed");

		if (store == null) {
			log("No Store configured, persistence disabled");
		} else if (store instanceof Lifecycle) {
			((Lifecycle) store).start();
		}

		// Start the background reaper thread
		threadStart();
	}

	@Override
	public void stop() throws LifecycleException {
		if (debug >= 1)
			log("Stopping");

		// Validate and update our current component state
		if (!isStarted())
			throw new LifecycleException(sm.getString("standardManager.notStarted"));
		lifecycle.fireLifecycleEvent(STOP_EVENT, null);
		setStarted(false);

		// Stop the background reaper thread
		threadStop();

		if (getStore() != null && saveOnRestart) {
			unload();
		} else {
			// Expire all active sessions
			Session[] sessions = findSessions();
			for (int i = 0; i < sessions.length; i++) {
				StandardSession session = (StandardSession) sessions[i];
				if (!session.isValid()) {
					continue;
				}
				session.expire();
			}
		}
		if (getStore() != null && getStore() instanceof Lifecycle) {
			((Lifecycle) getStore()).stop();
		}
		//
		this.random = null;
	}

	// -------------------------------------- PropertyChangeListener Methods

	@Override
	public void propertyChange(PropertyChangeEvent event) {

		// Validate the source of this event
		if (!(event.getSource() instanceof Context)) {
			return;
		}

		// Process a relevant property change
		if (event.getPropertyName().equals("sessionTimeout")) {
			try {
				setMaxInactiveInterval(((Integer) event.getNewValue()).intValue() * 60);
			} catch (NumberFormatException e) {
				log(sm.getString("standardManager.sessionTimeout", event.getNewValue().toString()));
			}
		}
	}

	// -------------------------------------- Protected Methods

	/**
	 * Invalidate all sessions that have expired
	 */
	protected void processExpires() {
		if (!started) {
			return;
		}
		long timeNow = System.currentTimeMillis();
		Session[] sessions = findSessions();

		for (int i = 0; i < sessions.length; i++) {
			StandardSession session = (StandardSession) sessions[i];
			if (!session.isValid()) {
				continue;
			}
			if (isSessionStale(session, timeNow)) {
				session.expire();
			}
		}
	}

	/**
	 * Swap idle sessions out to Store if they are idle too long
	 */
	protected void processMaxIdleSwaps() {
		if (!isStarted() || maxIdleSwap < 0) {
			return;
		}

		long timeNow = System.currentTimeMillis();
		Session[] sessions = findSessions();

		// Swap out all sessions idle longer than maxIdleSwap
		// FIXME: What's preventing us from mangling a session during a request?
		if (maxIdleSwap >= 0) {
			for (int i = 0; i < sessions.length; i++) {
				StandardSession session = (StandardSession) sessions[i];
				if (!session.isValid()) {
					continue;
				}
				int timeIdle = (int) ((timeNow - session.getLastAccessedTime()) / 1000L);
				if (timeIdle > maxIdleSwap && timeIdle > minIdleSwap) {
					if (debug >= 1) {
						log(sm.getString("persistentManager.swapMaxIdle", session.getId(), new Integer(timeIdle)));
					}
					try {
						swapOut(session);
					} catch (IOException e) {
						;
					}
				}
			}
		}
	}

	/**
	 * Swap idle sessions out to Store if too many are active.
	 */
	protected void processMaxActiveSwaps() {
		if (!isStarted() || getMaxActiveSessions() < 0) {
			return;
		}
		Session[] sessions = findSessions();

		// FIXME : Smart algorithm (LRU)
		if (getMaxActiveSessions() >= sessions.length) {
			return;
		}
		if (debug > 0) {
			log(sm.getString("persistentManager.tooManyActive", new Integer(sessions.length)));
		}

		int toSwap = sessions.length - getMaxActiveSessions();
		long timeNow = System.currentTimeMillis();

		for (int i = 0; i < sessions.length && toSwap > 0; i++) {
			int timeIdle = (int) ((timeNow - sessions[i].getLastAccessedTime()) / 1000L);
			if (timeIdle > minIdleSwap) {
				if (debug > 1) {
					log(sm.getString("persistentManager.swapTooManyActive", sessions[i].getId(),
							new Integer(timeIdle)));
				}
				try {
					swapOut(sessions[i]);
				} catch (IOException e) {
					;
				}
				toSwap--;
			}
		}

	}

	/**
	 * Back up idle sessions.
	 */
	protected void processMaxIdleBackups() {
		if (!isStarted() || maxIdleBackup < 0) {
			return;
		}

		long timeNow = System.currentTimeMillis();
		Session[] sessions = findSessions();

		// Back up all sessions idle longer than maxIdleBackup
		if (maxIdleBackup >= 0) {
			for (int i = 0; i < sessions.length; i++) {
				StandardSession session = (StandardSession) sessions[i];
				if (!session.isValid()) {
					continue;
				}
				int timeIdle = (int) ((timeNow - session.getLastAccessedTime()) / 1000L);
				if (timeIdle > maxIdleBackup) {
					if (debug >= 1) {
						log(sm.getString("persistentManager.backupMaxIdle", session.getId(), new Integer(timeIdle)));
					}
					
					try {
						writeSession(session);
					} catch (IOException e) {
						;
					}
				}
			}
		}
	}

	/**
	 * 
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 */
	protected Session swapIn(String id) throws IOException {
		if (store == null) {
			return null;
		}
		Session session = null;
		try {
			session = store.load(id);
		} catch (ClassNotFoundException e) {
			log(sm.getString("persistentManager.deserializeError", id, e));
			throw new IllegalStateException(sm.getString("persistentManager.deserializeError", id, e));
		}

		if (session == null) {
			return null;
		}
		if (!session.isValid() || isSessionStale(session, System.currentTimeMillis())) {
			log("session swapped in is invaild or expired ");
			session.expire();
			store.remove(id);
			return null;
		}
		if (debug >= 2) {
			log(sm.getString("persistentManager.swapIn", id));
		}

		session.setManager(this);
		add(session);
		((StandardSession) session).activate();

		return session;
	}

	protected void swapOut(Session session) throws IOException {
		if (store == null || !session.isValid() || isSessionStale(session, System.currentTimeMillis())) {
			return;
		}

		((StandardSession) session).passivate();
		writeSession(session);
		super.remove(session);
		session.recycle();
	}

	/**
	 * Indicate whether the session has been idle for longer than its expiration
	 * data as of the supplied time.
	 * 
	 * @param session
	 * @param timeNow
	 * @return
	 */
	protected boolean isSessionStale(Session session, long timeNow) {
		int maxInactiveInterval = session.getMaxInactiveInterval();
		if (maxInactiveInterval >= 0) {
			int timeIdle = (int) ((timeNow - session.getLastAccessedTime()) / 1000L);
			if (timeIdle >= maxInactiveInterval) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Write the provided session to the Store without modifying the copy in
	 * memory or triggering passivation events.Does nothing if the session is
	 * invalid or past its expiration.
	 * 
	 * @param session
	 * @throws IOException
	 */
	protected void writeSession(Session session) throws IOException {
		if (store == null || !session.isValid() || isSessionStale(session, System.currentTimeMillis())) {
			return;
		}
		try {
			store.save(session);
		} catch (IOException e) {
			log(sm.getString("persistentManager.serializeError", session.getId(), e));
			throw e;
		}

	}

	protected void threadStart() {

		if (thread != null) {
			return;
		}

		threadDone = false;
		threadName = "StandardManager[" + container.getName() + "]";
		thread = new Thread(this, threadName);
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

	// -------------------------------------- Background Thread

	@Override
	public void run() {

		// Loop until the termination semaphore is set
		while(!threadDone){
			threadSleep();
			processExpires();
			processPersistenceChecks();
		}

	}

}
