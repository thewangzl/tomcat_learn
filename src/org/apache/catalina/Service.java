package org.apache.catalina;

public interface Service {

	Container getContainer();
	
	void setContainer(Container container);
	
	String getInfo();
	
	String getName();
	
	void setName(String name);
	
	Server getServer();

	void setServer(Server server);
	
	void addConnector(Connector connector);

	Connector[] findConnectors();
	
	void removeConnector(Connector connector);

	void initialize() throws LifecycleException;

	

}
