package org.apache.catalina.util;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.catalina.InstanceEvent;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.Wrapper;

/**
 * Support class to assist in firing InstanceEvent notifications to regstered
 * InstanceListeners
 * 
 * @author thewangzl
 *
 */
public final class InstanceSupport {

	private Wrapper wrapper;

	private InstanceListener[] listeners = new InstanceListener[0];

	/**
	 * @param wrapper
	 */
	public InstanceSupport(Wrapper wrapper) {
		super();
		this.wrapper = wrapper;
	}
	
	//---------------------------------------Public Methods
	
	/**
	 * Nofify all lifecycle event listeners that a particular event has occurred for this Container.
	 * The default implementation performs this notification synchronously using the calling thread.
	 * 
	 * @param type Event type
	 * @param filter The relavant Filter for this event
	 */
	public void fireInstanceEvent(String type, Filter filter){
		if(listeners.length == 0){
			return;
		}
		InstanceEvent event = new InstanceEvent(wrapper, filter, type);
		InstanceListener[] interested = null;
		synchronized (listeners) {
			interested = listeners.clone();
		}
		for (InstanceListener listener : interested) {
			listener.instanceEvent(event);
		}
	}
	
	/**
	 * Nofify all lifecycle event listeners that a particular event has occurred for this Container.
	 * The default implementation performs this notification synchronously using the calling thread.
	 * 
	 * @param type Event type
	 * @param filter The relavant Filter for this event
	 * @param exception
	 */
	public void fireInstanceEvent(String type, Filter filter, Throwable exception){
		if(listeners.length == 0){
			return;
		}
		InstanceEvent event = new InstanceEvent(wrapper, filter, type, exception);
		InstanceListener[] interested = null;
		synchronized (listeners) {
			interested = listeners.clone();
		}
		for (InstanceListener listener : interested) {
			listener.instanceEvent(event);
		}
	}
	
	/**
	 * Nofify all lifecycle event listeners that a particular event has occurred for this Container.
	 * The default implementation performs this notification synchronously using the calling thread.
	 * 
	 * @param type Event type
	 * @param filter The relavant Filter for this event
	 * @param request
	 * @param response
	 */
	public void fireInstanceEvent(String type, Filter filter, ServletRequest request, ServletResponse response){
		if(listeners.length == 0){
			return;
		}
		InstanceEvent event = new InstanceEvent(wrapper, filter, type, request, response);
		InstanceListener[] interested = null;
		synchronized (listeners) {
			interested = listeners.clone();
		}
		for (InstanceListener listener : interested) {
			listener.instanceEvent(event);
		}
	}
	
	/**
	 * Nofify all lifecycle event listeners that a particular event has occurred for this Container.
	 * The default implementation performs this notification synchronously using the calling thread.
	 * 
	 * @param type Event type
	 * @param filter The relavant Filter for this event
	 * @param request
	 * @param response
	 * @param exception
	 */
	public void fireInstanceEvent(String type, Filter filter, ServletRequest request, ServletResponse response, Throwable exception){
		if(listeners.length == 0){
			return;
		}
		InstanceEvent event = new InstanceEvent(wrapper, filter, type, request, response,exception);
		InstanceListener[] interested = null;
		synchronized (listeners) {
			interested = listeners.clone();
		}
		for (InstanceListener listener : interested) {
			listener.instanceEvent(event);
		}
	}
	
	/**
	 * Nofify all lifecycle event listeners that a particular event has occurred for this Container.
	 * The default implementation performs this notification synchronously using the calling thread.
	 * 
	 * @param type Event type
	 * @param filter The relavant Filter for this event
	 */
	public void fireInstanceEvent(String type, Servlet servlet){
		if(listeners.length == 0){
			return;
		}
		InstanceEvent event = new InstanceEvent(wrapper, servlet, type);
		InstanceListener[] interested = null;
		synchronized (listeners) {
			interested = listeners.clone();
		}
		for (InstanceListener listener : interested) {
			listener.instanceEvent(event);
		}
	}
	
	/**
	 * Nofify all lifecycle event listeners that a particular event has occurred for this Container.
	 * The default implementation performs this notification synchronously using the calling thread.
	 * 
	 * @param type Event type
	 * @param servlet The relavant servlet for this event
	 * @param exception
	 */
	public void fireInstanceEvent(String type,Servlet servlet, Throwable exception){
		if(listeners.length == 0){
			return;
		}
		InstanceEvent event = new InstanceEvent(wrapper, servlet, type, exception);
		InstanceListener[] interested = null;
		synchronized (listeners) {
			interested = listeners.clone();
		}
		for (InstanceListener listener : interested) {
			listener.instanceEvent(event);
		}
	}
	
	/**
	 * Nofify all lifecycle event listeners that a particular event has occurred for this Container.
	 * The default implementation performs this notification synchronously using the calling thread.
	 * 
	 * @param type Event type
	 * @param servlet The relavant servlet for this event
	 * @param request
	 * @param response
	 */
	public void fireInstanceEvent(String type,Servlet servlet, ServletRequest request, ServletResponse response){
		if(listeners.length == 0){
			return;
		}
		InstanceEvent event = new InstanceEvent(wrapper, servlet, type, request, response);
		InstanceListener[] interested = null;
		synchronized (listeners) {
			interested = listeners.clone();
		}
		for (InstanceListener listener : interested) {
			listener.instanceEvent(event);
		}
	}
	
	/**
	 * Nofify all lifecycle event listeners that a particular event has occurred for this Container.
	 * The default implementation performs this notification synchronously using the calling thread.
	 * 
	 * @param type Event type
	 * @param servlet The relavant servlet for this event
	 * @param request
	 * @param response
	 * @param exception
	 */
	public void fireInstanceEvent(String type,Servlet servlet, ServletRequest request, ServletResponse response, Throwable exception){
		if(listeners.length == 0){
			return;
		}
		InstanceEvent event = new InstanceEvent(wrapper, servlet, type, request, response,exception);
		InstanceListener[] interested = null;
		synchronized (listeners) {
			interested = listeners.clone();
		}
		for (InstanceListener listener : interested) {
			listener.instanceEvent(event);
		}
	}
	
	
	public void addInstanceListener(InstanceListener listener){
		synchronized (listeners) {
			InstanceListener[] results = new InstanceListener[listeners.length + 1];
			for (int i = 0; i < listeners.length; i++) {
				results[i] = listeners[i];
			}
			results[listeners.length] = listener;
			listeners = results;
		}
	}
	
	public void removeInstanceListener(InstanceListener listener){
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
			InstanceListener[] results = new InstanceListener[listeners.length - 1];
			int j = 0;
			for (int i = 0; i < listeners.length; i++) {
				if(i != n){
					results[j++] = listeners[i];
				}
				
			}
			listeners = results;
		}
	}
	

	//---------------------------------------------------  Properties
	
	public Wrapper getWrapper() {
		return wrapper;
	}

}
