package org.apache.catalina.session;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.apache.catalina.Container;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Engine;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.util.StringManager;

/**
 * Minimal implementation of the <b>Manager</b> interface that supports no
 * session persistence or distributable capbilities. This class mat be
 * subclassed to create more sophisticated Manager implementations.
 * 
 * @author thewangzl
 *
 */
public abstract class ManagerBase implements Manager {

	/**
	 * The default message digest algorithm to use if we cannot use the
	 * requested one
	 */
	protected static final String DEFAULT_ALGORITHM = "MD5";

	/**
	 * The number of random bytes to include when generating a session
	 * identifier.
	 */
	protected static final int SESSION_ID_BYTES = 16;

	protected String algorithm = DEFAULT_ALGORITHM;

	protected Container container;

	protected int debug = 0;

	protected DefaultContext defaultContext;

	/**
	 * Return the MessageDigest implementation to be used when creating session
	 * identifiers
	 */
	protected MessageDigest digest;

	/**
	 * The distributable flag for Sessions created by this Manager.If this flag
	 * is set to <code>true</code>, any user attributes added to a session
	 * controlled by this Manager must be Serializable.
	 */
	protected boolean distributable;

	/**
	 * A strng initialization parameter used to increase the entropy of the
	 * initialization of our random number generator.
	 */
	protected String entropy;

	protected int maxInactiveInterval = 60;

	/**
	 * A random number generator to be use when generating session identifiers.
	 */
	protected Random random;

	protected String randomClass = "java.security.SecureRandom";

	/**
	 * The set of previously recycled Sessions for this Manager.
	 */
	protected ArrayList<Session> recycled = new ArrayList<>();

	protected HashMap<String, Session> sessions = new HashMap<>();

	protected static String name = "ManagerBase";

	private static final String info = "ManagerBase/1.0";

	protected static final StringManager sm = StringManager.getManager(Constants.Package);

	protected PropertyChangeSupport support = new PropertyChangeSupport(this);

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		String oldAlgorithm = this.algorithm;
		this.algorithm = algorithm;
		support.firePropertyChange("algorithm", oldAlgorithm, this.algorithm);
	}

	@Override
	public Container getContainer() {
		return this.container;
	}

	@Override
	public void setContainer(Container container) {
		Container oldContainer = this.container;
		this.container = container;
		support.firePropertyChange("container", oldContainer, this.container);
	}

	@Override
	public DefaultContext getDefaultContext() {

		return this.defaultContext;
	}

	@Override
	public void setDefaultContext(DefaultContext defaultContext) {
		DefaultContext oldDefaultContext = this.defaultContext;
		this.defaultContext = defaultContext;
		support.firePropertyChange("defaultContext", oldDefaultContext, this.defaultContext);
	}

	public int getDebug() {
		return debug;
	}

	public void setDebug(int debug) {
		this.debug = debug;
	}

	public MessageDigest getDigest() {
		if (this.digest == null) {
			if (debug >= 1)
				log(sm.getString("managerBase.getting", algorithm));
			try {
				this.digest = MessageDigest.getInstance(algorithm);
			} catch (NoSuchAlgorithmException e) {
				log(sm.getString("managerBase.digest", algorithm), e);
				try {
					this.digest = MessageDigest.getInstance(DEFAULT_ALGORITHM);
				} catch (NoSuchAlgorithmException f) {
					log(sm.getString("managerBase.digest", DEFAULT_ALGORITHM), e);
					this.digest = null;
				}
			}
			if (debug >= 1)
				log(sm.getString("managerBase.gotten"));
		}

		return digest;
	}

	@Override
	public boolean getDistributable() {

		return this.distributable;
	}

	@Override
	public void setDistributable(boolean distributable) {

		boolean oldDistributable = this.distributable;
		this.distributable = distributable;
		support.firePropertyChange("distributable", new Boolean(oldDistributable), new Boolean(this.distributable));
	}

	public String getEntropy() {
		if (entropy == null) {
			setEntropy(this.toString());
		}
		return entropy;
	}

	public void setEntropy(String entropy) {
		String oldEntropy = entropy;
		this.entropy = entropy;
		support.firePropertyChange("entropy", oldEntropy, this.entropy);
	}

	@Override
	public int getMaxInactiveInterval() {
		return this.maxInactiveInterval;
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		int oldMaxInactiveInterval = this.maxInactiveInterval;
		this.maxInactiveInterval = interval;
		support.firePropertyChange("maxInactiveInterval", oldMaxInactiveInterval, this.maxInactiveInterval);
	}

	public String getName() {
		return name;
	}

	public synchronized Random getRandom() {
		if (this.random == null) {
			synchronized (this) {
				if (this.random == null) {
					// Calculate the new random number generator seed
					log(sm.getString("managerBase.seeding", randomClass));
					long seed = System.currentTimeMillis();
					char entropy[] = getEntropy().toCharArray();
					for (int i = 0; i < entropy.length; i++) {
						long update = ((byte) entropy[i]) << ((i % 8) * 8);
						seed ^= update;
					}
					try {
						// Construct and seed a new random number generator
						Class<?> clazz = Class.forName(randomClass);
						this.random = (Random) clazz.newInstance();
						this.random.setSeed(seed);
					} catch (Exception e) {
						// Fall back to the simple case
						log(sm.getString("managerBase.random", randomClass), e);
						this.random = new java.util.Random();
						this.random.setSeed(seed);
					}
					log(sm.getString("managerBase.complete", randomClass));
				}
			}
		}
		return random;
	}

	public String getRandomClass() {
		return randomClass;
	}

	public void setRandomClass(String randomClass) {
		String oldRandomClass = this.randomClass;
		this.randomClass = randomClass;
		support.firePropertyChange("randomClass", oldRandomClass, this.randomClass);
	}

	@Override
	public void add(Session session) {

		synchronized (sessions) {
			sessions.put(session.getId(), session);
		}

	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		support.removePropertyChangeListener(listener);
	}

	/**
	 * Construct and return a new session object, based on the default settings specified by 
	 * this Manager's properties. The session id will be assigned by this method, and available
	 * via the getId() method of the returned session. If a new session cannot be created for
	 * any reason, return <code>null</code>.
	 * 
	 * 
	 * @return
	 */
	@Override
	public Session createSession() {

		//Recycle or create a Session instance
		Session session = null;
		synchronized (recycled) {
			int size = recycled.size();
			if(size > 0){
				session = recycled.get(size - 1);
				recycled.remove(size - 1);
			}
		}
		if(session != null){
			session.setManager(this);
		}else{
			session = new StandardSession(this);
		}
		
		//Initialize the properties of the new session and return it
		session.setNew(true);
		session.setValid(true);
		session.setCreationTime(System.currentTimeMillis());
		session.setMaxInactiveInterval(this.maxInactiveInterval);
		String sessionId = generateSessionId();
		String jvmRoute = getJvmRoute();
		//@todo Move appending of jvmRoute generateSessionId()?
		if(jvmRoute != null){
			sessionId += "." + jvmRoute;
		}
		session.setId(sessionId);
		add(session);
		return session;
	}

	@Override
	public Session findSession(String sessionId) throws IOException{
		if(sessionId == null){
			return null;
		}
		synchronized (sessions) {
			return sessions.get(sessionId);
		}
	}

	@Override
	public Session[] findSessions() {
		synchronized (sessions) {
			return sessions.values().toArray(new Session[sessions.size()]);
		}
	}

	@Override
	public void remove(Session session) {
		synchronized (sessions) {
			sessions.remove(session.getId());
		}
	}
	
	/**
	 * Add this session to the recycle collection for this Manager.
	 * 
	 * @param sesison
	 */
	void recycle(Session sesison) {
		synchronized (recycled) {
			recycled.add(sesison);
		}

	}

	@Override
	public String getInfo() {
		
		return info;
	}

	/**
	 * Log a message on the Logger associated with our Container (if any).
	 *
	 * @param message
	 *            Message to be logged
	 */
	void log(String message) {

		Logger logger = null;
		if (container != null)
			logger = container.getLogger();
		if (logger != null)
			logger.log(getName() + "[" + container.getName() + "]: " + message);
		else {
			String containerName = null;
			if (container != null)
				containerName = container.getName();
			System.out.println(getName() + "[" + containerName + "]: " + message);
		}

	}

	/**
	 * Log a message on the Logger associated with our Container (if any).
	 *
	 * @param message
	 *            Message to be logged
	 * @param throwable
	 *            Associated exception
	 */
	void log(String message, Throwable throwable) {

		Logger logger = null;
		if (container != null)
			logger = container.getLogger();
		if (logger != null)
			logger.log(getName() + "[" + container.getName() + "] " + message, throwable);
		else {
			String containerName = null;
			if (container != null)
				containerName = container.getName();
			System.out.println(getName() + "[" + containerName + "]: " + message);
			throwable.printStackTrace(System.out);
		}

	}


	public String getJvmRoute(){
		Engine engine = getEngine();
		return engine != null ? engine.getJvmRoute() : null;
	}
	
	public Engine getEngine(){
		Engine engine = null;
		for(Container c = getContainer(); engine == null && c != null; c = c.getParent()){
			if(c != null && c instanceof Engine){
				engine = (Engine) c;
			}
		}
		return engine;
	}
	/**
	 * Generate and return a new session identifier
	 * @return
	 */
	protected synchronized String generateSessionId(){
		// Generate a byte array containing a session identifier
		byte[] bytes = new byte[SESSION_ID_BYTES];
		getRandom().nextBytes(bytes);
		bytes = getDigest().digest(bytes);
		
		//Render the result as a String ofhexadecimal digits
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			byte b1 = (byte) ((bytes[i] & 0xf0) >> 4);
			byte b2 = (byte) (bytes[i] & 0x0f);
			if(b1 < 10){
				result.append((char)('0' + b1));
			}else{
				result.append((char)('A' + (b1 -10)));
			}
			if(b2 < 10){
				result.append((char)('0' + b2));
			}else{
				result.append((char)('A' + (b2 -10)));
			}
		}
		return result.toString();
	}
}
