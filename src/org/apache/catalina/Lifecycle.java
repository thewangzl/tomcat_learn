package org.apache.catalina;

public interface Lifecycle {

	/**
	 * The LifecycleEvent type for the "component start" event.
	 */
	public static final String START_EVENT = "start";
	
	public static final String BEFORE_START_EVENT = "before_start";
	
	public static final String AFTER_START_EVENT = "after_start";
	
	public static final String STOP_EVENT = "stop";
	
	public static final String BEFORE_STOP_EVENT = "before_stop";
	
	public static final String AFTER_STOP_EVENT = "after_stop";
	
	
	public void addLifecycleListener(LifecycleListener listener);
	
	public LifecycleListener[] findLifecycleListeners();
	
	public void removeLifecycleListener(LifecycleListener listener);
	
	/**
	 * Prepare for the beginning of active use of the public methods of this component.
	 * This method should be called before any of the public methods of this component are utilized.
	 * It should also send a LifecycleEvent of type START_EVENT to any registered listeners.
	 * @throws LifecycleException
	 */
	public void start() throws LifecycleException;
	
	/**
	 * Gracefully terminate the active use of the public methods of this component.
	 * This method should be the last on called on a given instance of this component.
	 * It should also send a LifecycleEvent of type STOP_EVENT to any registered listeners.
	 * @throws LifecycleException
	 */
	public void stop() throws LifecycleException;
	
}
