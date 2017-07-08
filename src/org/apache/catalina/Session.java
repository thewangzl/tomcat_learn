package org.apache.catalina;

import javax.servlet.http.HttpSession;

public interface Session {

	
	public void setValid(boolean valid);
	
	public boolean isValid();

	public HttpSession getSession();
}
