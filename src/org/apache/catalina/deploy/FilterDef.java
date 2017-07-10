package org.apache.catalina.deploy;

import java.util.HashMap;
import java.util.Map;

/**
 * Pepresentation of a filter definition of a application, as represented in a
 * <code>filter</code> element in the deployment descript.
 * 
 * @author thewangzl
 *
 */
public final class FilterDef {

	/**
	 *  The description of this filter.
	 */
	private String description;
	
	
	/**
	 * The display name of this filter.
	 */
	private String displayName;
	
	/**
	 * The full qulified name of the JAVA class that implements this filter
	 */
	private String filterClass;
	
	/**
	 * The name of this filter, which must be unique among the filters defined 
	 * for a particular web application.
	 */
	private String filterName;
	
	/**
	 * The large icon associated with this filter
	 */
	private String largeIcon;
	
	/**
	 * The small icon associated with this filter.
	 */
	private String smallIcon;
	
	/**
	 * The set of initialization parameters for this filter, keyed by parameter name.
	 */
	private Map<String, String> parameters = new HashMap<>();

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getFilterClass() {
		return filterClass;
	}

	public void setFilterClass(String filterClass) {
		this.filterClass = filterClass;
	}

	public String getFilterName() {
		return filterName;
	}

	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}

	public String getLargeIcon() {
		return largeIcon;
	}

	public void setLargeIcon(String largeIcon) {
		this.largeIcon = largeIcon;
	}

	public String getSmallIcon() {
		return smallIcon;
	}

	public void setSmallIcon(String smallIcon) {
		this.smallIcon = smallIcon;
	}
	
	public Map<String, String> getParameterMap() {
		return parameters;
	}
	
	/**
	 * Add an initialization parameter to the set of parameters associated with this filter.
	 * 
	 * @param name
	 * @param value
	 */
	public void addInitParameter(String name, String value){

		this.parameters.put(name, value);
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("FilterDef[");
		sb.append("filterName=").append(this.filterName) //
			.append(", filterClass=").append(this.filterClass)//
			.append("]");
		return sb.toString();
	}
	
	
}
