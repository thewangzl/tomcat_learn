package org.apache.catalina.core;

import java.util.Locale;

import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;

import org.apache.catalina.util.StringManager;

public class ApplicationResponse extends ServletResponseWrapper {

	/**
	 * Is this wrapped response the subject of a <code>include()</code> call?
	 */
	protected boolean included = false;
	
	protected static final StringManager sm = StringManager.getManager(Constants.Package);
	
	public ApplicationResponse(ServletResponse response) {
		this(response, false);
	}

	public ApplicationResponse(ServletResponse response, boolean included) {
		super(response);
		setIncluded(included);
	}
	
	// -------------------------------------------------------  ServletResponse Methods
	
	@Override
	public void reset() {
		
		//If already committed, the wrapper response will throw ISE
		if(!included || getResponse().isCommitted()){
			getResponse().reset();
		}
	}
	
	@Override
	public void setContentLength(int len) {
		if(!included){
			getResponse().setContentLength(len);
		}
	}
	
	@Override
	public void setContentType(String type) {
		if(!included){
			getResponse().setContentType(type);
		}
	}
	
	@Override
	public void setLocale(Locale loc) {
		if(!included){
			getResponse().setLocale(loc);
		}
	}
	
	// -----------------------------------------------------------------  ServletResponseWrapper Methods
	
	
	@Override
	public void setResponse(ServletResponse response) {
		super.setResponse(response);
	}
	
	// ---------------------------------------------- Package Methods
	
	boolean isIncluded() {
		return included;
	}
	
	void setIncluded(boolean included) {
		this.included = included;
	}

}
