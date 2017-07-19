package org.apache.catalina.session;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.ServletContext;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.util.CustomObjectInputStream;
import org.apache.catalina.util.LifecycleSupport;

public class StandardManager extends ManagerBase implements Lifecycle, PropertyChangeListener, Runnable {

	/**
	 * The interval (in seconds ) between checks for expired sessions.
	 */
	private int checkInterval = 60;

	/**
	 * The maxumum number of active Sessions allowed, or -1 for no limit.
	 */
	private int maxActiveSessions = -1;

	/**
	 * Path name of the disk file in which active sessions are saved when stop,
	 * and from which these sessions are loaded when we start.A
	 * <code>null</code> value indicates that no persistence is desired. If this
	 * pathname is relative, it will resolved against the temporary working
	 * directory provided by our context, availabl via the
	 * <code>javax.servlet.context.temdir</code> context attribute.
	 */
	private String pathname = "SESSIONS.ser";

	private boolean started = false;

	private Thread thread;

	/**
	 * The background thread completion semaphore.
	 */
	private boolean threadDone;

	private String threadName = "StandardManager";

	/**
	 * The descriptive name of this Manager implementation (for logging)
	 */
	protected static String name = "StandardManager";

	private static final String info = "StandardManager/1.0";

	protected LifecycleSupport lifecycle = new LifecycleSupport(this);

	public int getCheckInterval() {
		return checkInterval;
	}

	public void setCheckInterval(int checkInterval) {
		int oldCheckInterval = this.checkInterval;
		this.checkInterval = checkInterval;
		support.firePropertyChange("checkInterval", oldCheckInterval, this.checkInterval);
	}

	@Override
	public void setContainer(Container container) {
		// De-register from the old Container (if any)
		if ((this.container != null) && (this.container instanceof Context)) {
			((Context) this.container).removePropertyChangeListener(this);
		}
		//
		super.setContainer(container);
		
		//Register with the new Container (if any)
		if ((this.container != null) && (this.container instanceof Context)) {
			setMaxInactiveInterval(((Context)this.container).getSessionTimeout() * 60);
			((Context) this.container).addPropertyChangeListener(this);
		}
	}
	
	public int getMaxActiveSessions() {
		return maxActiveSessions;
	}
	
	public void setMaxActiveSessions(int max) {
		int oldMaxActiveSessions = this.maxActiveSessions;
		this.maxActiveSessions = max;
		support.firePropertyChange("maxActiveSessions", oldMaxActiveSessions, this.maxActiveSessions);
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public String getPathname() {
		return pathname;
	}
	
	public void setPathname(String pathname) {
		String oldPathname = this.pathname;
        this.pathname = pathname;
        support.firePropertyChange("pathname", oldPathname, this.pathname);
	}
	
	@Override
	public Session createSession() {
		if(maxActiveSessions >= 0 && sessions.size() >= maxActiveSessions){
			throw new IllegalStateException(sm.getString("standardManager.createSession.ise"));
		}
		return super.createSession();
	}

	/**
	 * Load any currently active sessions that were previously unloaded to the appropriate persistence 
	 * mechanism, if any. If persistence is not supported, this method returns without doing anything.
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	@Override
	public void load() throws ClassNotFoundException, IOException {
		if(debug >= 1){
			log("Start: Loading persisted sessions");
		}
		//Initialize our internak data structures
		recycled.clear();
		sessions.clear();
		
		//Open an input stream to the specified pathname, if any
		File file = file();
		if(file == null){
			return;
		}
		if(debug >= 1){
			log(sm.getString("standardManager.loading", pathname));
		}
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		Loader loader = null;
		ClassLoader classLoader = null;
		try{
			fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			if(container != null){
				loader = container.getLoader();
			}
			if(loader != null){
				classLoader = loader.getClassLoader();
			}
			if(classLoader != null){
				if(debug >= 1){
					log("Creating custom object input stream for class loader " + classLoader);
				}
				ois = new CustomObjectInputStream(bis, classLoader);
			}else{
				if(debug >= 1){
					log("Creating standard object input stream");
				}
				ois = new ObjectInputStream(bis);
			}
			
		}catch(FileNotFoundException e){
			if (debug >= 1){
                log("No persisted data file found");
			}
			return;
		}catch (IOException e) {
			log(sm.getString("standardManager.loading.ioe", e), e);
			if(ois != null){
				try{
					ois.close();
				}catch(IOException ie){
					;
				}
				ois = null;
			}
			throw e;
		}
		
		//Load the previously unloaded active sessions
		synchronized (sessions) {
			try {
				Integer count = (Integer)ois.readObject();
				int n = count.intValue();
				if(debug >= 1){
					log("Loading " + n + " persisted sessions");
				}
				for (int i = 0; i < n; i++) {
					StandardSession session = new StandardSession(this);
					session.readObjectData(ois);
//					session.setManager(this);
					sessions.put(session.getId(), session);
					((StandardSession) session).activate();
				}
			} catch (ClassNotFoundException e) {
				log(sm.getString("standardManager.loading.cnfe", e), e);
				if(ois != null){
					try{
						ois.close();
					}catch(IOException ie){
						;
					}
					ois = null;
				}
				throw e;
			}catch (IOException e) {
				log(sm.getString("standardManager.loading.ioe", e), e);
				if(ois != null){
					try{
						ois.close();
					}catch(IOException ie){
						;
					}
					ois = null;
				}
				throw e;
			}finally{
				if(ois != null){
					try{
						ois.close();
					}catch(IOException ie){
						;
					}
					ois = null;
				}
				
				//Delete the persistent storage file
				if(file != null && file.exists()){
					file.delete();
				}
			}
		}
		
		if(debug >= 1){
			log("Finish: Loading presisted sessions");
		}
	}

	/**
	 * Save any currently active sessions in the appropriate persistence mechanim, if any.
	 */
	@Override
	public void unload() throws IOException {
		
		if(debug >= 1){
			log("Unloading persisted sessions");
		}
		
		//
		File file = file();
		if(file == null){
			return;
		}
		if(debug >= 1){
			log(sm.getString("standardManager.unloading", pathname));
		}
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = new FileOutputStream(file.getAbsolutePath());
			oos = new ObjectOutputStream(new BufferedOutputStream(fos));
		} catch (IOException e) {
			log(sm.getString("standardManager.unloading.ioe", e), e);
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException f) {
                    ;
                }
                oos = null;
            }
            throw e;
		}

		//Write the number of active sessions, followed by the details
		ArrayList<StandardSession> list = new ArrayList<>();
		synchronized (sessions) {
			if(debug >= 1){
				log("Unloading " + sessions.size() + " sessions");
			}
			try {
				oos.writeObject(new Integer(sessions.size()));
				Iterator<Session> elements = sessions.values().iterator();
				while(elements.hasNext()){
					StandardSession session = (StandardSession) elements.next();
					list.add(session);
					session.passivate();
					session.writeObjectData(oos);
				}
			} catch (IOException e) {
				log(sm.getString("standardManager.unloading.ioe", e), e);
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (IOException f) {
                        ;
                    }
                    oos = null;
                }
                throw e;
			}
		}
		
		// Flush and close the outoput stream
		try {
			oos.flush();
			oos.close();
		} catch (IOException e) {
			if (oos != null) {
                try {
                    oos.close();
                } catch (IOException f) {
                    ;
                }
                oos = null;
            }
            throw e;
		}
		
		//Expire all the sessions we just wrote
		if(debug >= 1){
			log("Expiring " + list.size() + " persisted sessions");
		}
		Iterator<StandardSession> expires = list.iterator();
		while(expires.hasNext()){
			StandardSession session = expires.next();
			try {
				session.expire(false);
			} catch (Throwable e) {
				;
			}
		}
		
		if(debug >= 1){
			log("Unloading complete");
		}
	}

	/**
	 * The background thread that checks for session timeouts and shutdown
	 */
	@Override
	public void run() {

		//Loop until the termination semaphore is set
		while(!threadDone){
			threadSleep();
			processExpires();
		}

	}

	/**
	 * Process property change events from our associated Context.
	 * 
	 * @param event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {

		//Validate the source of this event
		if(!(event.getSource() instanceof Context)){
			return;
		}
//		Context context = (Context) event.getSource();
		
		//Process a relevant property change
		if(event.getPropertyName().equals("sessionTimeout")){
			 try {
				setMaxActiveSessions(((Integer) event.getNewValue()).intValue() * 60);
			} catch (NumberFormatException e) {
				 log(sm.getString("standardManager.sessionTimeout", event.getNewValue().toString()));
			}
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

	/**
	 * Prepare for the beginning of active use of the public methods of this component.
	 * This method should be called after <code>configure()</code>, and before any of 
	 * the public methods of the component are utilized.
	 * 
	 * @throws LifecycleException
	 */
	@Override
	public void start() throws LifecycleException {

		if(debug >= 1){
			log("Starting");
		}
		
		//Validate and update our current component state
		if(started){
			throw new LifecycleException(sm.getString("stanardManager.alreadyStarted"));
		}
		lifecycle.fireLifecycleEvent(START_EVENT, null);
		started = true;
		
		//Force initialization of the random number generator
		if(debug >= 1){
			log("Force random number initialization starting");
		}
		String dummy = generateSessionId();
		if(debug >= 1){
			log("Force random number initialization completed") ;
		}
		
		//Load unloaded sessions, if any.
		try {
			load();
		} catch (Throwable e) {
			log(sm.getString("standardManager.managerLoad"), e);
		}
		
		//Start the background reaper thread
		threadStart();
	}

	@Override
	public void stop() throws LifecycleException {
		if(debug >= 1){
			log("Stopping");
		}
		
		//Validate and update our current component state
		if(!started){
			throw new LifecycleException(sm.getString("standardManager.notStarted"));
		}
		
		//Stop the background reaper thread
		threadStop();
		
		//Write out sessions
		try {
			unload();
		} catch (IOException e) {
			log(sm.getString("standardManager.managerUnload"), e);
		}
		
		//Expire all active sessions
		Session[] sessions = findSessions();
		for (int i = 0; i < sessions.length; i++) {
			StandardSession session = (StandardSession) sessions[i];
			if(!session.isValid()){
				continue;
			}
			try {
				session.expire();
			} catch (Throwable e) {
				;
			}
		}
		
		// Require a new random number generator if we are restarted
		this.random = null;
	}
	
	@Override
	public String getInfo() {
		return info;
	}

	/**
	 * Return a File object representing the pathname to our presistence, if any
	 * 
	 * @return
	 */
	private File file(){
		if(pathname == null){
			return null;
		}
		File file = new File(pathname);
		if(!file.isAbsolute()){
			if(container instanceof Context){
				ServletContext servletContext = ((Context) container).getServletContext();
				File tempdir = (File) servletContext.getAttribute(Globals.WORK_DIR_ATTR);
				if(tempdir != null){
					file = new File(tempdir, pathname);
				}
			}
		}
		return file;
	}
	
	/**
	 * Start the background thread that will periodically check for session timeouts.
	 */
	private void threadStart(){
		if(thread != null){
			return;
		}
		
		threadDone = false;
		threadName = "StandardManager[" + container.getName() + "]";
		thread = new Thread(this, threadName);
		thread.setDaemon(true);
		thread.setContextClassLoader(container.getLoader().getClassLoader());
		thread.start();
	}
	
	/**
	 * Stop the background thread that is periodically checking for session timeouts
	 */
	private void threadStop(){
		
		if(thread == null){
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
	
	/**
	 * Sleep for the duration specfied by the <code>checkInterval</code>
	 */
	private void threadSleep(){
		try {
			Thread.sleep(checkInterval * 1000L) ;
		} catch (InterruptedException e) {
			;
		}
	}
	
	/**
	 * Invalidate all sessions that have expired.
	 */
	private void processExpires(){
		
		long timeNow = System.currentTimeMillis();
		Session[] sessions = findSessions();
		for (int i = 0; i < sessions.length; i++) {
			StandardSession session = (StandardSession) sessions[i];
			if(!session.isValid()){
				continue;
			}
			int maxInactiveInterval = session.getMaxInactiveInterval();
			if(maxInactiveInterval < 0){
				continue;
			}
			int timeIdle = (int)((timeNow - session.getLastAccessedTime()) / 1000L);
			if(timeIdle >= maxInactiveInterval){
				try {
					session.expire();
				} catch (Throwable e) {
					log(sm.getString("standardManager.expireException"), e);
				}
			}
		}
		
		
	}
}
