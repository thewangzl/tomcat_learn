package org.apache.catalina.core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.deploy.NamingResources;
import org.apache.naming.NamingContext;

/**
 * Helper class used to initialize and populate the JNDI context associated with each Context and server.
 * 
 * @author thewangzl
 *
 */
public class NamingContextListener implements LifecycleListener, ContainerListener, PropertyChangeListener {

	protected String name = "/";
	
	protected Object container;
	
	
	protected int debug = 0;
	
	protected boolean initialized;
	
	protected NamingResources namingResources;
	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDebug() {
		return debug;
	}

	public void setDebug(int debug) {
		this.debug = debug;
	}

	public NamingContext getNamingContext() {
		return namingContext;
	}

	public void setNamingContext(NamingContext namingContext) {
		this.namingContext = namingContext;
	}

	/**
	 * 
	 */
	protected NamingContext namingContext;
	
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub

	}

	@Override
	public void containeEvent(ContainerEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		// TODO Auto-generated method stub

	}
	
	

}
