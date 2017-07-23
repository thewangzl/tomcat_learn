package org.apache.catalina;

import java.beans.PropertyChangeListener;
import java.security.Principal;
import java.security.cert.X509Certificate;

public interface Realm {

	public Container getContainer();
	
	
	public void setContainer(Container container);
	
	/**
	 * Return the Principal associated with the specified username and credentials, 
	 * if where is one; otherwise return <code>null</code>.
	 * 
	 * @param username
	 * @param credentials
	 * @return
	 */
	public Principal authenticate(String username, String credentials);
	
	
	public Principal authenticate(String username, byte[] credentials);
	
	/**
	 * 
	 * @param username
	 * @param digest
	 * @param nonce
	 * @param nc
	 * @param cnonce
	 * @param qop
	 * @param realm
	 * @param md5a2
	 * @return
	 */
	public Principal authenticate(String username, String digest, String nonce, String nc, String cnonce, String qop, String realm, String md5a2);
	
	
	public Principal authenticate(X509Certificate[] certs);
	
	
	public boolean hasRole(Principal principal, String role);
	
	
	public void addPropertyChangeListener(PropertyChangeListener listener);
	
	public void removePropertyChangeListener(PropertyChangeListener listener);
	
	public String getInfo();
}
