package org.apache.catalina.core;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class StandardWrapperFacade implements ServletConfig {

	private ServletConfig config;
	
	public StandardWrapperFacade(ServletConfig config) {
		super();
		this.config = config;
	}

	@Override
	public String getInitParameter(String name) {
		return config.getInitParameter(name);
	}

	@Override
	public Enumeration<?> getInitParameterNames() {
		return config.getInitParameterNames();
	}

	@Override
	public ServletContext getServletContext() {
		return config.getServletContext();
	}

	@Override
	public String getServletName() {
		return config.getServletName();
	}

}
