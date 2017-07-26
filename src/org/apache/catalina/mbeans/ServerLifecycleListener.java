package org.apache.catalina.mbeans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

public class ServerLifecycleListener implements ContainerListener, LifecycleListener,PropertyChangeListener {

	public ServerLifecycleListener() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void containeEvent(ContainerEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		// TODO Auto-generated method stub
		
	}

}
