package org.apache.catalina.core;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.util.Enumerator;

public class ApplicationFilterConfig implements FilterConfig {

	private Context context;

	private Filter filter;

	private FilterDef filterDef;

	/**
	 * @param context
	 * @param filterDef
	 * @throws ServletException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public ApplicationFilterConfig(Context context, FilterDef filterDef) throws InstantiationException, IllegalAccessException, ClassNotFoundException, ServletException {
		super();
		this.context = context;
		this.setFilterDef(filterDef);
	}
	

	@Override
	public String getFilterName() {
		return filterDef.getFilterName();
	}

	@Override
	public String getInitParameter(String name) {
		Map<String, String> map = filterDef.getParameterMap();
		if (map != null) {
			return map.get(name);
		}
		return null;
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		Map<String, String> map = filterDef.getParameterMap();
		if (map != null) {
			return new Enumerator<String>(map.keySet());
		}
		return new Enumerator<String>(new ArrayList<>());
	}

	@Override
	public ServletContext getServletContext() {

		return this.context.getServletContext();
	}

	// --------------------------------- Package Method

	Filter getFilter() throws InstantiationException, IllegalAccessException, ClassNotFoundException, ServletException {

		// Return the existing filter instance, if any
		if (this.filter != null) {
			return filter;
		}

		// Identity the class loader we will be using
		String filterClass = filterDef.getFilterClass();
		ClassLoader classLoader = null;
		if (filterClass.startsWith("org.apache.catalina.")) {
			classLoader = this.getClass().getClassLoader();
		} else {
			classLoader = context.getLoader().getClassLoader();
		}


		// Instance a new instance of this filter and return it
		Class<?> clazz = classLoader.loadClass(filterClass);
		this.filter = (Filter) clazz.newInstance();
		filter.init(this);
		return filter;
	}

	FilterDef getFilterDef() {
		return filterDef;
	}

	void setFilterDef(FilterDef filterDef) throws InstantiationException, IllegalAccessException, ClassNotFoundException, ServletException {
		this.filterDef = filterDef;
		if (filterDef == null) {
			// Release any previously allocated filter instance
			if (this.filter != null) {
				this.filter.destroy();
			}
			this.filter = null;
		} else {

			// Allocate a new filter instance
			getFilter();
		}
	}
	
	void release(){
		if(this.filter != null){
			filter.destroy();
		}
		this.filter = null;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("ApplicationFilterConfig[");
		sb.append("name=").append(filterDef.getFilterName());
		sb.append(", filterClass=").append(filterDef.getFilterClass());
		sb.append("]");
		return sb.toString();
	}
}
