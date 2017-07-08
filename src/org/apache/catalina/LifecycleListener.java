package org.apache.catalina;

/**
 * 
 * @author lenovo
 *
 */
public interface LifecycleListener {

	/**
	 * Acknowledge the occurrence of the specified event.
	 * @param event LifecycleEvent that has occurred
	 */
	public void lifecycleEvent(LifecycleEvent event);
	
}
