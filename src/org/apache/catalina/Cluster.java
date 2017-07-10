package org.apache.catalina;

public interface Cluster {

	Container getContainer();
	
	void setContainer(Container container);

}
