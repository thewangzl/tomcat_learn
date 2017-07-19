package org.apache.catalina;

import java.security.Principal;
import java.util.Iterator;

import javax.servlet.http.HttpSession;

public interface Session {

	public static final String SESSION_CREATED_EVENT = "createSession";

	public static final String SESSION_DESTROY_EVENT = "destroySession";

	/**
	 * Return the authentication type used to authenticate our cached Principal, if any.
	 * 
	 * @return
	 */
	public String getAuthType();

	public void setAuthType(String authType);

	/**
	 * Return the creation tie for this session.
	 * @return
	 */
	public long getCreationTime();

	public void setCreationTime(long creationTime);

	/**
	 * Return the session identifier for this session.
	 * 
	 * @return
	 */
	public String getId();

	public void setId(String id);

	
	public String getInfo();

	/**
	 * Return the last time the client sent a request associated with this session, as the number of 
	 * milliseconds since midnight, January 1, 1970 GMT. Actions that your appication takes, such as 
	 * getting or setting a value associated with the session, do not affect the access time.
	 * 
	 * @return
	 */
	public long getLastAccessedTime();

	/**
	 * Return the manager within which this session is valid.
	 * 
	 * @return
	 */
	public Manager getManager();

	public void setManager(Manager manager);

	/**
	 * Return the maximum time interval, in seconds, between client requests before the servlet 
	 * container will invalidate the session. A negative time indicates that the session should 
	 * never time out.
	 * 
	 * @return
	 */
	public int getMaxInactiveInterval();

	public void setMaxInactiveInterval(int interval);

	/**
	 * Set the <code>isNew</code> flag for this session
	 * 
	 * @param isNew
	 */
	public void setNew(boolean isNew);

	/**
	 * 
	 * @return
	 */
	public Principal getPrincipal();

	public void setPrincipal(Principal principal);

	/**
	 * Return the <code>HttpSession</code> for which this object is the facade.
	 * @return
	 */
	public HttpSession getSession();

	/**
	 * Set the <code>isVaild</code> flag for this session.
	 * 
	 * @param valid
	 */
	public void setValid(boolean valid);

	public boolean isValid();

	/**
	 * Update the accessed time information for this session. This method should be 
	 * called by the context when a request comes in for a particular session,
	 * even if the application does not reference it.
	 */
	public void access();


	/**
	 * Perform the internal processing required to invalidate this session, without triggering
	 * an exception if the session has already expired.
	 */
	public void expire();

	public Object getNote(String name);

	/**
	 * Release all object references, and initialize instance variables, in preparation for 
	 * reuse of this object.
	 */
	public void recycle();

	/**
	 * 
	 * @param listener
	 */
	public void addSessionListener(SessionListener listener);
	
	/**
	 * 
	 * @param listener
	 */
	public void removeSessionListener(SessionListener listener);

	
	public Iterator<String> getNoteNames();

	public void removeNote(String name);

	public void setNote(String name, Object note);

}
