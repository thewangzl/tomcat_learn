package org.apache.catalina;

import java.beans.PropertyChangeListener;
import java.io.IOException;

public interface Manager {

	Container getContainer();
	
	void setContainer(Container container);
	
	public DefaultContext getDefaultContext();

	
	public void setDefaultContext(DefaultContext defaultContext);

	public boolean getDistributable();
	
	public void setDistributable(boolean distributable);
	
	public int getMaxInactiveInterval();
	
	public void setMaxInactiveInterval(int interval);
	
	public void add(Session session);
	
	
	public void addPropertyChangeListener(PropertyChangeListener listener);

	public void removePropertyChangeListener(PropertyChangeListener listener);

	
	public Session createSession();
	

	Session findSession(String sessionId) throws IOException;
	
	
	public Session[] findSessions();
	
	
	public void remove(Session session);
	
	
	public void load() throws ClassNotFoundException, IOException;

	
	public void unload() throws IOException;

	
	public String getInfo();


}
