package ex07.pyrmont.core;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Container;
import org.apache.catalina.Mapper;
import org.apache.catalina.Request;
import org.apache.catalina.Wrapper;

public class SimpleContextMapper implements Mapper {

	private SimpleContext context;
	
	private String protocol;
	
	@Override
	public Container getContainer() {

		return this.context;
	}

	@Override
	public void setContainer(Container container) {
		if(!(container instanceof SimpleContext)){
			throw new IllegalArgumentException("Illegal type of container");
		}
		this.context = (SimpleContext) container;
	}

	@Override
	public String getProtocol() {

		return this.protocol;
	}

	@Override
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	@Override
	public Container map(Request request, boolean update) {

		//Identify the context-relative URI to be mapped
//		String contextPath = ((HttpServletRequest)request.getRequest()).getContextPath();
//		String requestURI = ((HttpRequest)request).getDecodedRequestURI();
//		String relativeURI = requestURI.substring(contextPath.length());
		String relativeURI = ((HttpServletRequest)request.getRequest()).getRequestURI();
		
		//Apply the standard request URI mapping rules from the specification
		Wrapper wrapper = null;
		String servletPath = null;
		String pathInfo = null;
		String name = context.findServletMapping(relativeURI);
		
		if(name != null){
			wrapper =  (Wrapper)context.findChild(name);
		}
		return wrapper;
	}

}
