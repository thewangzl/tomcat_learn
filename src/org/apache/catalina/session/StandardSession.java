package org.apache.catalina.session;

import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.connector.http.Constants;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.StringManager;


@SuppressWarnings({ "serial", "deprecation" })
public class StandardSession implements HttpSession, Session, Serializable {

	
	private static final String NOT_SERIALIZED = "__NOT_SERIALIZABLE_EXCEPTION__";
	
	
	/**
	 * The collection of user data attributes associated with this Session.
	 */
	private HashMap<String, Object> attributes = new HashMap<>();
	
	/**
	 * The authentication type used to authenticate our cached Pricipal, if any.
	 */
	private transient String authType;
	
	/**
	 * The <code>java.lang.Method</code>for the <code>fireContainerEvent()</code> method
	 * of the <code>org.apache.catalina.core.StandardContext</code> method, if our Context 
	 * implementation dynamiclly the first time it is needed, or after a session reload
	 * (since it is declared trainsient).
	 * 
	 */
	private transient Method containerEventMethod;
	
	/**
	 * The method signature for the <code>fireContainerEvent</code> method.
	 */
	private static final Class<?> containerEventTypes[] = {String.class, Object.class};
	
	/**
	 * The time this session was created, in millseconds since midnight, January 1, 1970 GMT
	 */
	private long creationTime = 0;
	
	private transient int debug = 0;
	
	/**
	 * We are currently processing a session expiration, so bypass certain IllegalStateException tesets.
	 */
	private transient boolean expiring;
	
	/**
	 * The facade associated with this session.
	 */
	private transient StandardSessionFacade facade;
	
	/**
	 * The session identifier of this session.
	 */
	private String id;
	
	/**
	 * The last accessed time for this session.
	 */
	private long lastAccessedTime = creationTime;
	
	private transient ArrayList<SessionListener> listeners = new ArrayList<>();
	
	/**
	 * The manager with which this session is associated.
	 */
	private Manager manager;
	
	/**
	 * The maximum time interval, in seconds, between client requests before the servlet container
	 * may invalidate this session. A negative time indicates that the session should be never time out.
	 */
	private int maxInactiveInterval = -1;
	
	/**
	 * Flag indicating whether this session is new or not.
	 */
	private boolean isNew;
	
	/**
	 * Flag indicating whether this session is valid or not.
	 */
	private boolean isValid;
	
	private transient HashMap<String, Object> notes = new HashMap<>();
	
	private transient Principal principal;
	
	/**
	 * The HTTP session context associated with this session
	 */
	private static HttpSessionContext sessionContext;
	
	/**
	 * 
	 */
	private transient PropertyChangeSupport support = new PropertyChangeSupport(this);
	
	/**
	 * The current accessed time for this session
	 */
	private long thisAccessedTime = creationTime;
	
	
	private static StringManager sm = StringManager.getManager(Constants.Package);
	
	private static final String info = "org.apache.catalina.session.StandardSession/1.0";
	
	
	
	public StandardSession(Manager manager) {
		super();
		this.manager = manager;
		if(manager instanceof ManagerBase){
			this.debug = ((ManagerBase) manager).getDebug();
		}
	}

	@Override
	public String getAuthType() {

		return this.authType;
	}

	@Override
	public void setAuthType(String authType) {
		String oldAuthType = this.authType;
		this.authType = authType;
		support.firePropertyChange("authType", oldAuthType, this.authType);
	}

	@Override
	public void setCreationTime(long time) {
		this.creationTime = time;
		this.lastAccessedTime = time;
		this.thisAccessedTime = time;
		
	}
	
	@Override
	public String getId() {	
		
		return this.id;
	}

	@Override
	public void setId(String id) {
		if(this.id != null && manager != null){
			manager.remove(this);
		}
		this.id = id;
		if(manager != null){
			manager.add(this);
		}
		
		//Notify 
		fireSessionEvent(SESSION_CREATED_EVENT, null);
		
		//Notify
		Context context = (Context) manager.getContainer();
		Object[] listeners = context.getApplicationListeners();
		if(listeners != null){
			HttpSessionEvent event = new HttpSessionEvent(getSession());
			for (int i = 0; i < listeners.length; i++) {
				if(!(listeners[i] instanceof HttpSessionEvent)){
					continue;
				}
				HttpSessionListener listener = (HttpSessionListener) listeners[i];
				try {
					fireContainerEvent(context, "beforeSessionCreated", listener);
					listener.sessionCreated(event);
					fireContainerEvent(context, "afterSessionCreated", listener);
				} catch (Throwable t) {
					try{
						fireContainerEvent(context, "afterSessionCreated", listener);
					}catch(Exception e){
						;
					}
					// FIXME - should we do anything besides log these ?
					log(sm.getString("standardSession.sessionEvent"),t);
				}
				
			}
		}
	}

	@Override
	public String getInfo() {

		return info;
	}

	@Override
	public Manager getManager() {

		return this.manager;
	}

	@Override
	public void setManager(Manager manager) {
		this.manager = manager;
	}

	@Override
	public void setNew(boolean isNew) {

		this.isNew = isNew;
	}

	@Override
	public Principal getPrincipal() {

		return this.principal;
	}

	@Override
	public void setPrincipal(Principal principal) {
		Principal oldPrincipal = this.principal;
		this.principal = principal;
		support.firePropertyChange("principal", oldPrincipal, this.principal);
	}

	@Override
	public HttpSession getSession() {
		if(facade == null){
			facade = new StandardSessionFacade(this); 
		}
		return facade;
	}

	@Override
	public void setValid(boolean valid) {
		this.isValid = valid;
	}

	@Override
	public boolean isValid() {
		return this.isValid;
	}

	@Override
	public void access() {
		this.isNew = false;
		this.lastAccessedTime = this.thisAccessedTime;
		this.thisAccessedTime = System.currentTimeMillis();

	}

	@Override
	public void expire() {
		this.expire(true);
	}
	
	public void expire(boolean notify){
		//Mark this session as "being expired" if need
		if(expiring){
			return;
		}
		expiring = true;
		setValid(false);
		
		//remove this session from our manager's active sessions
		if(manager != null){
			manager.remove(this);
		}
		
		//Unbind any objects associated with this session
		String[] keys = keys();
		for (int i = 0; i < keys.length; i++) {
			removeAttribute(keys[i], notify);
		}
		//Notify interested session event listeners
		if(notify){
			fireSessionEvent(Session.SESSION_DESTROY_EVENT, null);
		}
		//
		Context context = (Context) manager.getContainer();
		Object[] listeners = context.getApplicationListeners();
		if(notify && listeners != null){
			HttpSessionEvent event = new HttpSessionEvent(getSession());
			
			for (int i = listeners.length; i >= 0; i--) {
				if(!(listeners[i] instanceof HttpSessionListener)){
					continue;
				}
				HttpSessionListener listener = (HttpSessionListener) listeners[i];
				try {
					fireContainerEvent(context, "beforeSessionDestroyed", listener);
					listener.sessionDestroyed(event);
					fireContainerEvent(context, "afterSessionDestroyed", listener);
				} catch (Throwable t) {
					try{
						fireContainerEvent(context, "afterSessionDestroyed", listener);
					}catch(Exception e){
						;
					}
					//FIXME  should we do anything besides log these?
					log(sm.getString("standardSession.sessionEvent"), t);
				}
			}
		}
		
		//We have completed expire of this session
		expiring = false;
		if(manager != null && manager instanceof ManagerBase){
			recycle();
		}
	}

	

	@Override
	public void recycle() {
		//Reset the instance variables associated with this session
		attributes.clear();
		setAuthType(null);
		creationTime = 0L;
		expiring = false;
		id = null;
		lastAccessedTime = 0L;
		maxInactiveInterval = -1;
		notes.clear();
		setPrincipal(null);
		isNew = false;
		isValid = false;
		Manager saveManager = manager;
		manager = null;
		
		//Tell our Manager that this session has been recycled.
		if(saveManager != null && saveManager instanceof ManagerBase){
			((ManagerBase) saveManager).recycle(this);
		}

	}

	@Override
	public void addSessionListener(SessionListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	@Override
	public void removeSessionListener(SessionListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}



	@Override
	public long getCreationTime() {
		if(!isValid){
			throw new IllegalStateException(sm.getString("standardSession.getCreationTime.ise"));
		}
		return this.creationTime;
	}


	@Override
	public long getLastAccessedTime() {
		return this.lastAccessedTime;
	}

	@Override
	public int getMaxInactiveInterval() {
		return this.maxInactiveInterval;
	}

	@Override
	public ServletContext getServletContext() {
		if(manager == null){
			return null;
		}
		Context context = (Context) manager.getContainer();
		if(context == null){
			return null;
		}
		return context.getServletContext();
	}

	@Override
	public HttpSessionContext getSessionContext() {
		if(sessionContext == null){
			sessionContext = new StandardSessionContext();
		}
		return sessionContext;
	}

	@Override
	public Object getValue(String name) {

		return getAttribute(name);
	}

	@Override
	public String[] getValueNames() {
		if(!isValid){
			throw new IllegalStateException(sm.getString("standardSession.getAttributeNames.ise"));
		}
		return keys();
	}

	@Override
	public void invalidate() {
		if(!isValid){
			throw new IllegalStateException(sm.getString("standardSession.invalidate.ise"));
		}
		// Cause this session to expire
		expire();
	}

	@Override
	public boolean isNew() {
		if(!isValid){
			throw new IllegalStateException(sm.getString("standardSession.isNew.ise"));
		}
		return this.isNew;
	}

	@Override
	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}
	
	@Override
	public void removeValue(String name) {
		removeAttribute(name);
	}
	
	@Override
	public Object getAttribute(String name) {
		if(!isValid){
			throw new IllegalStateException(sm.getString("standardSession.getAttribute.ise"));
		}
		synchronized (attributes) {
			return attributes.get(name);
		}
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		if(!isValid){
			throw new IllegalStateException(sm.getString("standardSession.getAttributeNames.ise"));
		}
		synchronized (attributes) {
			return new Enumerator<>(attributes.keySet());
		}
	}

	@Override
	public void removeAttribute(String name) {

		this.removeAttribute(name, true);
	}
	
	
	public void removeAttribute(String name, boolean notify) {

		if(!expiring && !isValid){
			throw new IllegalStateException(sm.getString("standardSession.removeAttribute.ise"));
		}
		
		//Remove this attribute from our collection
		Object value = null;
		boolean found = false;
		synchronized (attributes) {
			found = attributes.containsKey(name);
			if(found){
				value = attributes.get(name);
				attributes.remove(name);
			}else{
				return;
			}
		}
		
		if(!notify){
			return;
		}
		
		// Call the valuebound() method if necessary
		HttpSessionBindingEvent event = new HttpSessionBindingEvent(getSession(), name, value);
		if(value != null && value instanceof HttpSessionBindingListener){
			((HttpSessionBindingListener) value).valueBound(event);
		}
		
		//
		Context context = (Context) manager.getContainer();
		Object[] listeners = context.getApplicationListeners();
		if(listeners == null){
			return;
		}
		for (int i = 0; i < listeners.length; i++) {
			if(!(listeners[i] instanceof HttpSessionAttributeListener)){
				continue;
			}
			HttpSessionAttributeListener listener = (HttpSessionAttributeListener) listeners[i];
			try {
				fireContainerEvent(context, "beforeSessionAttributeRemoved", listener);
				listener.attributeRemoved(event);
				fireContainerEvent(context, "afterSessionAttributeRemoved", listener);
			} catch (Throwable t) {
				try{
					fireContainerEvent(context, "afterSessionAttributeRemoved", listener);
				}catch(Exception e){
					;
				}
				// FIXME - should we do anything besides log these ?
				log(sm.getString("standardSession.attributeEvent"), t);
			}
		}
	}


	@Override
	public void setAttribute(String name, Object value) {
		if(name == null){
			throw new IllegalArgumentException(sm.getString("standardSession.setAttribute.namenull"));
		}
		
		// null value is the same as removeAttribute()
		if(value == null){
			removeAttribute(name);
			return;
		}
		
		//Validate our current state
		if(!isValid){
			throw new IllegalStateException(sm.getString("standardSession.setAttribute.ise"));
		}
		if(manager != null && manager.getDistributable() && !(value instanceof Serializable)){
			throw new IllegalArgumentException(sm.getString("standardSession.setAttribute.iae"));
		}
		
		//Replace or add this attribute
		Object unbound = null;
		synchronized (attributes) {
			unbound = attributes.get(name);
			attributes.put(name, value);
		}
		
		// Call the valueUnbound() method if necessary
		if(unbound != null && unbound instanceof HttpSessionBindingListener){
			((HttpSessionBindingListener) unbound).valueUnbound(new HttpSessionBindingEvent(this, name));
		}
		
		// Call the valueBound() method if necessary
		HttpSessionBindingEvent event = null;
		if(unbound != null){
			event = new HttpSessionBindingEvent(this, name, unbound);
		}else{
			event = new HttpSessionBindingEvent(this, name, value);
		}
		if(value instanceof HttpSessionBindingListener){
			((HttpSessionBindingListener) value).valueBound(event);
		}
		
		//Notify interested application event listeners
		Context context = (Context) manager.getContainer();
		Object[] listeners = context.getApplicationListeners();
		if(listeners == null){
			return;
		}
		for (int i = 0; i < listeners.length; i++) {
			if(!(listeners[i] instanceof HttpSessionAttributeListener)){
				continue;
			}
			HttpSessionAttributeListener listener = (HttpSessionAttributeListener) listeners[i];
			try {
				if(unbound != null){
					fireContainerEvent(context, "beforeSessionAttributeReplaced", listener);
					listener.attributeReplaced(event);
					fireContainerEvent(context, "afterSessionAttributeReplaced", listener);
				}else{
					fireContainerEvent(context, "beforeSessionAttributeAdded", listener);
					listener.attributeRemoved(event);
					fireContainerEvent(context, "afterSessionAttributeAdded", listener);
				}
			} catch (Throwable t) {
				try{
					if(unbound != null){
						fireContainerEvent(context, "afterSessionAttributeReplaced", listener);
					}else{
						fireContainerEvent(context, "afterSessionAttributeAdded", listener);
					}
				}catch(Exception e){
					;
				}
				// FIXME - should we do anything besides log  these?
				log(sm.getString("standardSession.attributeEvent"), t);
			}
		}
		
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		
		this.maxInactiveInterval = interval;
	}
	
	private void fireContainerEvent(Context context, String type, Object data) throws Exception{
		if(!"org.apache.catalina.core.StandardContext".equals(context.getClass().getName())){
			return;					// Container events are not supported
		}
		//NOTE: race condition is harmless, so do not synchronize
		if(containerEventMethod == null){
			containerEventMethod = context.getClass().getMethod("fireContainerEvent", containerEventTypes);
		}
		Object[] containerEventParams = new Object[2];
		containerEventParams[0] = type;
		containerEventParams[1] = data;
		containerEventMethod.invoke(context, containerEventParams);
		
	}
	
	public void fireSessionEvent(String type, Object data){
		if(listeners.size() == 0){
			return;
		}
		SessionEvent event = new SessionEvent(this, type, data);
		SessionListener[] list = new SessionListener[0];;
		synchronized (listeners) {
			list = listeners.toArray(list);
		}
		for (int i = 0; i < list.length; i++) {
			list[i].sessionEvent(event);
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
	public void setNote(String name, Object note) {
		synchronized (notes) {
			notes.put(name, note);
		}

	}
	
	/**
	 * Perform the internal processing required to passivate this session
	 */
	public void passivate(){
		//Notify ActivationListeners
		HttpSessionEvent event = null;
		String[] keys = keys();
		for (int i = 0; i < keys.length; i++) {
			Object attribute = getAttribute(keys[i]);
			if(attribute instanceof HttpSessionActivationListener){
				if(event == null){
					event = new HttpSessionEvent(this);
				}
				//FIXME - Should we catch throwables?
				((HttpSessionActivationListener) attribute).sessionWillPassivate(event);
			}
		}
	}
	
	/**
	 * Perform internal processing required to activate this session.
	 */
	public void activate(){
		//Notify ActivationListeners
		HttpSessionEvent event = null;
		String[] keys = keys();
		for (int i = 0; i < keys.length; i++) {
			Object attribute = getAttribute(keys[i]);
			if(attribute instanceof HttpSessionActivationListener){
				if(event == null){
					event = new HttpSessionEvent(this);
				}
				//FIXME - Should we catch throwables?
				((HttpSessionActivationListener) attribute).sessionDidActivate(event);
			}
		}
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("StandardSession[").append(id).append("]");
		return sb.toString();
	}
	
	
	// ------------------------------------------ Package Methods
	
	/**
	 * Read a serialized version of the contexts of this session object from the specified object 
	 * input stream, without requiring that the StandardSession itself have been serialized.
	 * 
	 * @param stream
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	void readObjectData(ObjectInputStream stream) throws ClassNotFoundException, IOException{
		readObject(stream);
	}
	
	/**
	 * Write a serialized version of the contents of this session object to the specified object 
	 * output stream, without requiring that the StandardSession itself have been serialized.
	 * 
	 * @param stream
	 * @throws IOException
	 */
	void writeObjectData(ObjectOutputStream stream) throws IOException{
		writeObject(stream);
	}
	
	
	// ---------------------------------------- Private Methods
	
	private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException{
		
		//Deserialize the scalar instance variables (except Manager)
		authType = null;	// Transient only
		creationTime = ((Long) stream.readObject()).longValue();
		lastAccessedTime = ((Long) stream.readObject()).longValue();
		maxInactiveInterval = ((Integer) stream.readObject()).intValue();
		isNew = ((Boolean) stream.readObject()).booleanValue();
		isValid = ((Boolean) stream.readObject()).booleanValue();
		thisAccessedTime = ((Long) stream.readObject()).longValue();
		principal = null;
		
		id = (String) stream.readObject();
		if(debug >= 2){
			log("readObject() loading session " + id);
		}
		
		//Deserialize the attribute count and attribute values
		if(attributes == null){
			attributes = new HashMap<>();
		}
		int n = ((Integer) stream.readObject()).intValue();
		boolean isVaildSave = isValid;
		isValid= true;
		for(int i = 0; i < n;i++){
			String name = (String) stream.readObject();
			Object value = (Object) stream.readObject();
			if(value instanceof String && value.equals(NOT_SERIALIZED)){
				continue;
			}
			if(debug >= 2){
				log(" Loading attribute ;" + name + "' with value '" + value);
			}
			synchronized (attributes) {
				attributes.put(name, value);
			}
		}
		isValid = isVaildSave;
	}
	
	
	private void writeObject(ObjectOutputStream stream) throws IOException {
		//
		stream.writeObject(new Long(creationTime));
		stream.writeObject(new Long(lastAccessedTime));
		stream.writeObject(new Integer(maxInactiveInterval));
		stream.writeObject(new Boolean(isNew));
		stream.writeObject(new Boolean(isValid));
		stream.writeObject(new Long(thisAccessedTime));
		stream.writeObject(id);
		if (debug >= 2)
			log("writeObject() storing session " + id);

		// Accumulate the names of serializable and non-serializable attributes
		String keys[] = keys();
		ArrayList<String> saveNames = new ArrayList<>();
		ArrayList<Object> saveValues = new ArrayList<>();
		for (int i = 0; i < keys.length; i++) {
			Object value = null;
			synchronized (attributes) {
				value = attributes.get(keys[i]);
			}
			if (value == null)
				continue;
			else if (value instanceof Serializable) {
				saveNames.add(keys[i]);
				saveValues.add(value);
			}
		}

		// Serialize the attribute count and the Serializable attributes
		int n = saveNames.size();
		stream.writeObject(new Integer(n));
		for (int i = 0; i < n; i++) {
			stream.writeObject((String) saveNames.get(i));
			try {
				stream.writeObject(saveValues.get(i));
				if (debug >= 2)
					log("  storing attribute '" + saveNames.get(i) + "' with value '" + saveValues.get(i) + "'");
			} catch (NotSerializableException e) {
				log(sm.getString("standardSession.notSerializable", saveNames.get(i), id), e);
				stream.writeObject(NOT_SERIALIZED);
				if (debug >= 2)
					log("  storing attribute '" + saveNames.get(i) + "' with value NOT_SERIALIZED");
			}
		}

	}
	/**
	 * 
	 * @return
	 */
	private String[] keys() {
		synchronized (attributes) {
			return attributes.keySet().toArray(new String[0]);
		}
	}
	
	 /**
     * Log a message on the Logger associated with our Manager (if any).
     *
     * @param message Message to be logged
     */
	private void log(String message) {

        if ((manager != null) && (manager instanceof ManagerBase)) {
            ((ManagerBase) manager).log(message);
        } else {
            System.out.println("StandardSession: " + message);
        }

    }


    /**
     * Log a message on the Logger associated with our Manager (if any).
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    private void log(String message, Throwable throwable) {

        if ((manager != null) && (manager instanceof ManagerBase)) {
            ((ManagerBase) manager).log(message, throwable);
        } else {
            System.out.println("StandardSession: " + message);
            throwable.printStackTrace(System.out);
        }

    }

}

/**
 * This class is a dummy implementation of the <code>HttpSessionContext</code> interface, to comfirm to
 * the requirement that such an object be returned when <code>HttpSession.getSessionContxt()</code> is called
 * .
 * @author thewangzl
 *
 */
@SuppressWarnings("deprecation")
final class StandardSessionContext implements HttpSessionContext{
	
	@SuppressWarnings("rawtypes")
	private HashMap dummy = new HashMap<>();
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Enumeration getIds() {
		return new Enumerator<>(dummy);
	}

	@Override
	public HttpSession getSession(String id) {
		return null;
	}
	
}
