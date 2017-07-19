package org.apache.catalina;

import java.util.EventObject;

@SuppressWarnings("serial")
public final class SessionEvent extends EventObject {


	public SessionEvent(Session session, String type, Object data) {
		super(session);
	}

}
