package org.apache.catalina;

/**
 * 
 * @author thewangzl
 *
 */
public interface Engine extends Container {

	
	public String getDefaultHost();
	
	public void setDefaultHost(String defaultHost);
	
	
	public String getJvmRoute();
	
	public void setJvmRoute(String jvmRoute);
	
	public Service getService();
	
	public void setService(Service service);
	
	
	public DefaultContext getDefaultContext();
	
	
	public void importDefaultContext(Context context);
	
	
	
	
	
	
	
	
	
	
}
