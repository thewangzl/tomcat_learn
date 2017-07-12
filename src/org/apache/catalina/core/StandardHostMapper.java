package org.apache.catalina.core;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Mapper;
import org.apache.catalina.Request;
import org.apache.catalina.util.StringManager;

public class StandardHostMapper implements Mapper {

	private StandardHost host;
	
	private String protocol;
	
	private static final StringManager sm = StringManager.getManager(Constants.Package);
	
	
	
	@Override
	public Container getContainer() {
		return host;
	}

	@Override
	public void setContainer(Container container) {
		if(!(container instanceof StandardHost)){
			throw new IllegalArgumentException(sm.getString("httpHostMapper.container"));
		}
		this.host = (StandardHost) container;
	}

	@Override
	public String getProtocol() {

		return this.protocol;
	}

	@Override
	public void setProtocol(String protocol) {

		this.protocol = protocol;
	}

	/**
	 * Return the child container that should be used to process this request.
	 * based upon its characteristics. If no such child Container can be identified,
	 * return <code>null</code> instead.
	 * 
	 * @param request
	 * @param update
	 * @return
	 */
	@Override
	public Container map(Request request, boolean update) {
		//Has this request already been mapped?
		if(update && request.getContext() != null){
			return request.getContext();
		}
		//Perform mapping on our request URI
		HttpServletRequest  hreq = (HttpServletRequest) request.getRequest();
		Context context = host.map(hreq.getRequestURI());
		
		//Update the request (if requested) and return the selected Context.
		if(update){
			request.setContext(context);
			if(context != null){
				((HttpRequest) request).setContextPath(context.getPath());
			}else{
				((HttpRequest) request).setContextPath(null);
			}
		}
		return context;
	}

}
