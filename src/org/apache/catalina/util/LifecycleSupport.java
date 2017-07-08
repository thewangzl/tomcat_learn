package org.apache.catalina.util;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

public final class LifecycleSupport {

	private Lifecycle lifecycle;
	
	private LifecycleListener[] listeners = new LifecycleListener[0];

	public LifecycleSupport(Lifecycle lifecycle) {
		this.lifecycle = lifecycle;
	}
	
	public void fireLifecycleEvent(String type, Object data){
		LifecycleEvent event = new LifecycleEvent(lifecycle, type, data);
		LifecycleListener[] interested = null;
		synchronized (listeners) {
			interested = listeners.clone();
		}
		for (int i = 0; i < interested.length; i++) {
			interested[i].lifecycleEvent(event);
		}
	}

	public void addLifecycleListener(LifecycleListener listener){
		synchronized (listeners) {
			LifecycleListener[] results = new LifecycleListener[listeners.length + 1];
			for (int i = 0; i < listeners.length; i++) {
				results[i] = listeners[i];
			}
			results[listeners.length] = listener;
			listeners = results;
		}
	}
	
	public LifecycleListener[] findLifecycleListeners(){
		return this.listeners;
	}
	
	public void removeLifecycleListener(LifecycleListener listener){
		synchronized (listeners) {
			int n = -1;
			for (int i = 0; i < listeners.length; i++) {
				if(listeners[i] == listener){
					n = i;
					break;
				}
			}
			if(n < 0){
				return;
			}
			LifecycleListener[] results = new LifecycleListener[listeners.length - 1];
			int  j = 0;
			for (int i = 0; i < listeners.length; i++) {
				if(i != n){
					results[j++] = listeners[i];
				}
			}
			listeners = results;
		}
	}
	
	
}
