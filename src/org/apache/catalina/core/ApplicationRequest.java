package org.apache.catalina.core;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;

import org.apache.catalina.Globals;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.StringManager;

public class ApplicationRequest extends ServletRequestWrapper {

	/**
	 * The set of attribute names that are special for request dispatchers
	 */
	protected static final String[] specials = {
			Globals.REQUEST_URI_ATTR, Globals.CONTEXT_PATH_ATTR, Globals.SERVLET_PATH_ATTR,	//
			Globals.PATH_INFO_ATTR, Globals.QUERY_STRING_ATTR
	};
	
	
	protected Map<String,Object> attributes = new HashMap<>();
	
	protected static final StringManager sm = StringManager.getManager(Constants.Package);
	
	
	public ApplicationRequest(ServletRequest request) {
		super(request);
		// TODO Auto-generated constructor stub
	}
	
	// ---------------------------------- ServletRequest Methods
	
	@Override
	public Object getAttribute(String name) {
		synchronized (attributes) {
			return attributes.get(name);
		}
	}
	
	@Override
	public Enumeration<String> getAttributeNames() {
		synchronized (attributes) {
			return new Enumerator<>(attributes.keySet());
		}
	}
	
	@Override
	public void removeAttribute(String name) {
		synchronized (attributes) {
			attributes.remove(name);
			if(!isSpecial(name)){
				getRequest().removeAttribute(name);
			}
		}
	}
	
	@Override
	public void setAttribute(String name, Object value) {
		synchronized (attributes) {
			attributes.put(name, value);
			if(!isSpecial(name)){
				getRequest().setAttribute(name, value);
			}
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void setRequest(ServletRequest request) {
		
		super.setRequest(request);
		
		//Initialized the attributes for this request
		synchronized (attributes) {
			attributes.clear();
			Enumeration<String> names = request.getAttributeNames();
			while(names.hasMoreElements()){
				String name = names.nextElement();
				Object value = request.getAttribute(name);
				attributes.put(name, value);
			}
		}
	}

	// ------------------------------- Protected Methods
	

	/**
	 * Is this attribute name one of the special ones that is added for including servlets?
	 * 
	 * @param name
	 * @return
	 */
	protected boolean isSpecial(String name){
		for (int i = 0; i < specials.length; i++) {
			if(specials[i].equals(name)){
				return true;
			}
		}
		return false;
	}
}
