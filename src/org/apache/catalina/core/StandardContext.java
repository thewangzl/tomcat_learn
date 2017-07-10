package org.apache.catalina.core;

import javax.servlet.ServletContext;

import org.apache.catalina.Context;
import org.apache.catalina.deploy.FilterMap;

/**
 * Standard implementation of the <b>Wrapper</b> interface that represents an individual servlet definition.
 * No child Containers are allowed, and the parent Container must be a Context.
 * 
 * @author thewangzl
 *
 */
public class StandardContext extends ContainerBase implements Context {

	@Override
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getCookies() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPath(String path) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addServletMapping(String pattern, String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String findServletMapping(String pattern) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] findServletMappings() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getReloadable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setReloadable(boolean reloadable) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reload() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getConfigured() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setConfigured(boolean configured) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getDocBase() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDocBase(String docBase) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getAvailable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setAvailable(boolean available) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getPrivileged() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setPrivileged(boolean privileged) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	public FilterMap[] findFilterMaps() {
		// TODO Auto-generated method stub
		return null;
	}

	public ApplicationFilterConfig findFilterConfig(String filterName) {
		// TODO Auto-generated method stub
		return null;
	}

	

}
