package org.apache.catalina.deploy;

/**
 * Representation of a context initialization parameter that is configured in the server configuration file,
 * rather than the application deployment descriptor. This is convenient for establishing default values (
 * which may be configured to allow application overrides or not) without having to modify the application 
 * deployment descriptor itself.
 * 
 * @author thewangzl
 *
 */
public final class ApplicationParameter {

	/**
	 * The description of this environment entry.
	 */
	private String description;
	
	/**
	 * The name of this application parameter.
	 */
	private String name;
	
	/**
	 * Does this application parameter allow overrides by the application deployment descriptor?
	 */
	private boolean override = true;
	
	/**
	 * The value of this application parameter.
	 */
	private String value;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean getOverride() {
		return override;
	}

	public void setOverride(boolean override) {
		this.override = override;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("ApplicationParameter[");
		sb.append("name=").append(name);
		if(this.description != null){
			sb.append(", description=").append(this.description);
		}
		sb.append(", value=").append(value);
		sb.append(", override=").append(override);
		sb.append("]");
		return sb.toString();
	}
	
}
