package org.apache.catalina.deploy;

/**
 * Representation of a filter mapping for a web application, as represented in a 
 * <code>filter-mapping</code> element in the deployment descriptor. Each filter 
 * mapping  must contain a filter name plus either a URL pattern or a servlet name.
 * 
 * @author thewangzl
 *
 */
public final class FilterMap {

	/**
	 * The name of this filter to be executed when this mapping matches a particular request.
	 */
	private String filterName;
	
	/**
	 * The name of this mapping matches.
	 */
	private String servletName;
	
	/**
	 * The URL pattern this mapping matches.
	 */
	private String urlPattern;

	public String getFilterName() {
		return filterName;
	}

	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}

	public String getServletName() {
		return servletName;
	}

	public void setServletName(String servletName) {
		this.servletName = servletName;
	}

	public String getUrlPattern() {
		return urlPattern;
	}

	public void setUrlPattern(String urlPattern) {
		this.urlPattern = urlPattern;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("FilterMap[");
		sb.append("filterName=").append(this.filterName);
		if(this.servletName != null){
			sb.append(", servletName=").append(this.servletName);
		}
		if(this.urlPattern != null){
			sb.append(", urlPattern=").append(this.urlPattern);
		}
		sb.append("]");
		return sb.toString();
	}
	
	
	
}
