package org.apache.catalina;

import java.beans.PropertyChangeListener;
import java.io.IOException;


/**
 * A <b>Store</b> is the abstraction of a Catalina component that provides persistent storage and loading
 * of Sessions and their associated user data. Implementations are free to save and load the Sessions to
 * any media they wish, but it is assumed that saved Sessions are persistent across server or context restarts.
 * 
 * @author thewangzl
 *
 */
public interface Store {

	/**
	 * Return the Manager istance associated with this Store.
	 * 
	 * @return
	 */
	public Manager getManager();
	
	
	public void setManager(Manager manager);
	
	/**
	 * Return the number of Sessions present in this Store.
	 * 
	 * @return
	 * @throws IOException
	 */
	public int getSize() throws IOException;
	
	/**
	 * Return an array containing the session identfiers of all Sessions currently saved in 
	 * this Store. If there are no such Sessions, a zero-length array is returned.
	 * 
	 * @return
	 * @throws IOException
	 */
	public String[] keys() throws IOException;
	
	/**
	 * Load and return the Session associated with the specified session identifier from
	 * this Store. without removing it. If there is no such stored Session, return
	 * <code>null</code>.
	 * 
	 * @param id
	 * @return
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public Session load(String id) throws ClassNotFoundException, IOException;
	
	
	/**
	 * Save the specified Session into this Store. Any previously saved information for 
	 * the associated session identifier is replaced.
	 * 
	 * @param session
	 * @throws IOException
	 */
	public void save(Session session) throws IOException;
	
	/**
	 * remove the session with the specified session identifier from this Store, if present.
	 * If no such Session is present, this method takes no action.
	 * 
	 * @param id
	 * @throws IOException
	 */
	public void remove(String id) throws IOException;
	
	/**
	 * Remove all Sessions from this Store.
	 * 
	 * @throws IOException
	 */
	public void clear() throws IOException;
	
	/**
	 * 
	 * @param listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener);
	
	/**
	 * 
	 * @param listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener);
	
	
	public String getInfo();
	
}
