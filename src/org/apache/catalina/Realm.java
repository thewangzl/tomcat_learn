package org.apache.catalina;

public interface Realm {

	public Container getContainer();
	
	
	public void setContainer(Container container);
	
	
	public String getInfo();
}
