package org.apache.catalina;

/**
 * 
 * @author thewangzl
 *
 */
public interface InstanceListener {

	
	/**
	 * Acknowledge the occurrence of the specified event.
	 * 
	 * @param event InstanceEvent that has occurred
	 */
	public void instanceEvent(InstanceEvent event);
}
