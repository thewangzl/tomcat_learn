package org.apache.catalina;

/**
 * 
 * @author thewangzl
 *
 */
public interface Host extends Container {

	
	public String getAppBase();
	
	
	public void setAppBase(String appBase);


	public Context map(String uri);
}
