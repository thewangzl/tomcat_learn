package org.apache.catalina.authenticator;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletException;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.ValveContext;
import org.apache.catalina.valves.ValveBase;

public class SingleSignOn extends ValveBase implements Lifecycle, SessionListener {

	@Override
	public void sessionEvent(SessionEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void start() throws LifecycleException {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() throws LifecycleException {
		// TODO Auto-generated method stub

	}

	@Override
	public void invoke(Request request, Response response, ValveContext valveContext)
			throws IOException, ServletException {
		// TODO Auto-generated method stub

	}
	
	void register(String ssoId, Principal principal, String authType, String username, String password){
		//TODO
	}

}
