package org.apache.catalina;

public interface Manager {

	Container getContainer();
	
	void setContainer(Container container);

	Session findSession(String sessionId);

}
