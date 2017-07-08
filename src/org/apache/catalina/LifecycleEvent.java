package org.apache.catalina;

import java.util.EventObject;

@SuppressWarnings("serial")
public final class LifecycleEvent extends EventObject {

	/**
	 * The Lifecycle on which this event occurred.
	 */
	private Lifecycle lifecycle;
	
	/**
	 * The event type this instance represents.
	 */
	private String type;
	
	/**
	 * The event data associated with this event.
	 */
	private Object data;
	
	public LifecycleEvent(Lifecycle lifecycle, String type) {
		this(lifecycle, type, null);
	}
	
	public LifecycleEvent(Lifecycle lifecycle, String type, Object data) {
		super(lifecycle);
		this.lifecycle = lifecycle;
		this.type = type;
		this.data = data;
	}

	public Lifecycle getLifecycle() {
		return lifecycle;
	}

	public String getType() {
		return type;
	}

	public Object getData() {
		return data;
	}


	
}
