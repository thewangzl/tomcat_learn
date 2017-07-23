package org.apache.catalina.deploy;

import org.apache.catalina.util.RequestUtil;

public final class SecurityCollection {

	/**
	 * The name of this web resource collection.
	 */
	private String name;

	private String description;

	/**
	 * The HTTP methods coverted by this web resource collection
	 */
	private String[] methods = new String[0];

	/**
	 * The URL patterns protected by this security
	 */
	private String[] patterns = new String[0];

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void addMethod(String method) {
		if (method == null) {
			return;
		}
		String[] results = new String[methods.length + 1];
		for (int i = 0; i < methods.length; i++) {
			results[i] = methods[i];
		}
		results[methods.length] = method;
		methods = results;
	}

	public boolean findMethod(String method) {
		if (methods.length == 0) {
			return true;
		}
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].equals(method)) {
				return true;
			}
		}
		return false;
	}

	public String[] findMethods() {
		return methods;
	}

	public void addPattern(String pattern) {
		if (pattern == null) {
			return;
		}
		pattern = RequestUtil.URLDecode(pattern);
		String[] results = new String[patterns.length + 1];
		for (int i = 0; i < patterns.length; i++) {
			results[i] = patterns[i];
		}
		results[patterns.length] = pattern;
		patterns = results;
	}

	public boolean findPattern(String pattern) {
		for (int i = 0; i < patterns.length; i++) {
			if (patterns[i].equals(pattern)) {
				return true;
			}
		}
		return false;
	}

	public String[] findPatterns() {

		return patterns;
	}

	/**
	 * Remove the specified HTTP request method from those that are part of this
	 * web resource collection.
	 *
	 * @param method
	 *            Request method to be removed
	 */
	public void removeMethod(String method) {

		if (method == null)
			return;
		int n = -1;
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].equals(method)) {
				n = i;
				break;
			}
		}
		if (n >= 0) {
			int j = 0;
			String results[] = new String[methods.length - 1];
			for (int i = 0; i < methods.length; i++) {
				if (i != n)
					results[j++] = methods[i];
			}
			methods = results;
		}

	}

	/**
	 * Remove the specified URL pattern from those that are part of this web
	 * resource collection.
	 *
	 * @param pattern
	 *            Pattern to be removed
	 */
	public void removePattern(String pattern) {

		if (pattern == null)
			return;
		int n = -1;
		for (int i = 0; i < patterns.length; i++) {
			if (patterns[i].equals(pattern)) {
				n = i;
				break;
			}
		}
		if (n >= 0) {
			int j = 0;
			String results[] = new String[patterns.length - 1];
			for (int i = 0; i < patterns.length; i++) {
				if (i != n)
					results[j++] = patterns[i];
			}
			patterns = results;
		}
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("SecurityCollection[");
        sb.append(name);
        if (description != null) {
            sb.append(", ");
            sb.append(description);
        }
        sb.append("]");
        return (sb.toString());
	}

}
