package org.apache.catalina.core;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Container;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Mapper;
import org.apache.catalina.Request;
import org.apache.catalina.Wrapper;
import org.apache.catalina.util.StringManager;

public final class StandardContextMapper implements Mapper {

	private StandardContext context;
	
	private String protocol;
	
	private static final StringManager sm = StringManager.getManager(Constants.Package);
	
	
	@Override
	public Container getContainer() {
		return context;
	}

	@Override
	public void setContainer(Container container) {
		if(!(container instanceof StandardContext)){
			throw new IllegalArgumentException(sm.getString("httpContextMaper.container"));
		}
		this.context = (StandardContext) container;
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

		int debug = context.getDebug();
		
		//Has this request already been mapped ?
		if(update && request.getWrapper() != null){
			return request.getWrapper();
		}
		
		//Identify the context-relative URI to be mapped
		
		String contextPath = ((HttpServletRequest) request.getRequest()).getContextPath();
		if(contextPath == null || contextPath.length() == 0	){
			contextPath = context.getPath();
		}
//		String requestURI = ((HttpRequest) request).getDecodedRequestURI();
		String requestURI =  ((HttpServletRequest) request.getRequest()).getRequestURI();
		String relativeURI = requestURI.substring(contextPath.length());
		
		//
		if(debug >= 1){
			context.log("Mapping contextPath='" + contextPath +
                    "' with requestURI='" + requestURI +
                    "' and relativeURI='" + relativeURI + "'");
		}
		
		//Apply the standard request URI mapping rules from the specification
		Wrapper wrapper = null;
		String servletPath = relativeURI;
		String pathInfo = null;
		String name = null;
		
		//Rule 1 -- Exact Match 
		if(wrapper == null){
			if(debug >= 2){
				context.log(" Trying exact match");
			}
			if(!(relativeURI.equals("/"))){
				name = context.findServletMapping(relativeURI);
			}
			if(name != null){
				wrapper = (Wrapper) context.findChild(name);
			}
			if(wrapper != null){
				servletPath = relativeURI;
				pathInfo = null;
			}
		}
		
		// rule 2 - prefix match
		if(wrapper == null){
			if(debug >= 2){
				context.log("Trying prefix match");
			}
			servletPath = relativeURI;
			while(true){
				name = context.findServletMapping(servletPath + "/");
				if(name != null){
					wrapper = (Wrapper) context.findChild(name);
				}
				if(wrapper != null){
					pathInfo = relativeURI.substring(servletPath.length());
					if(pathInfo.length() == 0){
						pathInfo = null;
					}
					break;
				}
				int slash = servletPath.lastIndexOf('/');
				if(slash < 0){
					break;
				}
				servletPath = servletPath.substring(0, slash);
			}
		}
		
		// Rule 3 - extension match 
		if(wrapper == null){
			if(debug >= 2){
				context.log("Trrying extension match");
			}
			int slash = relativeURI.lastIndexOf('/');
			if(slash >=0){
				String last = relativeURI.substring(slash);
				int period = last.lastIndexOf('.');
				if(period >= 0){
					String pattern = "*" + last.substring(period);
					name = context.findServletMapping(pattern);
					if(name != null){
						wrapper = (Wrapper) context.findChild(name);
					}
					if(wrapper != null){
						servletPath = relativeURI;
						pathInfo = null;
					}
				}
			}
		}
		
		// Rule 4 - Default Match
		if(wrapper == null){
			if(debug >= 2){
				context.log("Trying default match");
			}
			name = context.findServletMapping("/");
			if(name != null){
				wrapper = (Wrapper) context.findChild(name);
			}
			if(wrapper != null){
				servletPath = relativeURI;
				pathInfo = null;
			}
		}
		
		if(debug >= 1 && wrapper != null){
			context.log(" Mapped to servlet '" + wrapper.getName() +
                    "' with servlet path '" + servletPath +
                    "' and path info '" + pathInfo +
                    "' and update=" + update);
		}
		if(update){
			request.setWrapper(wrapper);
			((HttpRequest) request).setServletPath(servletPath);
			((HttpRequest) request).setPathInfo(pathInfo);
		}
		return wrapper;
	}

}
