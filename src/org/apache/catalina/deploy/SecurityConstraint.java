package org.apache.catalina.deploy;

public final class SecurityConstraint {

	private boolean allRoles;

	private boolean authConstraint;

	private String[] authRoles = new String[0];

	private SecurityCollection[] collections = new SecurityCollection[0];

	private String displayName;

	private String userConstraint = "NONE";

	public boolean getAllRoles() {
		return allRoles;
	}

	public boolean getAuthConstraint() {
		return authConstraint;
	}

	public void setAuthConstraint(boolean authConstraint) {
		this.authConstraint = authConstraint;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getUserConstraint() {
		return userConstraint;
	}

	public void setUserConstraint(String userConstraint) {
		if (userConstraint != null) {
			this.userConstraint = userConstraint;
		}
	}

	// ---------------------------------------------------- Public Methods

	public void addAuthRole(String authRole) {
		if (authRole == null) {
			return;
		}
		if ("*".equals(authRole)) {
			allRoles = true;
			return;
		}
		String[] results = new String[authRoles.length + 1];
		for (int i = 0; i < authRoles.length; i++) {
			results[i] = authRoles[i];
		}
		results[authRoles.length] = authRole;
		authRoles = results;
		authConstraint = true;
	}

	public void addCollection(SecurityCollection collection) {
		if (collection == null) {
			return;
		}
		SecurityCollection[] results = new SecurityCollection[collections.length + 1];
		for (int i = 0; i < collections.length; i++) {
			results[i] = collections[i];
		}
		results[collections.length] = collection;
		collections = results;

	}

	public boolean findAuthRole(String role) {
		if (role == null) {
			return false;
		}
		for (int i = 0; i < authRoles.length; i++) {
			if (role.equals(authRoles[i])) {
				return true;
			}
		}
		return false;
	}

	public String[] findAuthRoles() {
		return authRoles;
	}

	public void removeAuthRole(String authRole) {
		if (authRole == null) {
			return;
		}
		int n = -1;
		for (int i = 0; i < authRoles.length; i++) {
			if (authRoles[i].equals(authRole)) {
				n = i;
				break;
			}
		}
		if (n >= 0) {
			int j = 0;
			String[] results = new String[authRoles.length - 1];
			for (int i = 0; i < authRoles.length; i++) {
				if (i != n) {
					results[j++] = authRoles[i];
				}
			}
			authRoles = results;
		}
	}

	public SecurityCollection findCollection(String name) {
		if (name == null) {
			return null;
		}
		for (int i = 0; i < collections.length; i++) {
			if (name.equals(collections[i].getName())) {
				return collections[i];
			}
		}
		return null;
	}

	public SecurityCollection[] findCollections() {
		return collections;
	}

	public void removeCollection(SecurityCollection collection) {
		if (collection == null) {
			return;
		}
		int n = -1;
		for (int i = 0; i < collections.length; i++) {
			if (collection.equals(collections[i])) {
				n = i;
				break;
			}
		}
		if (n >= 0) {
			int j = 0;
			SecurityCollection[] results = new SecurityCollection[collections.length - 1];
			for (int i = 0; i < collections.length; i++) {
				if (i != n) {
					results[j++] = collections[i];
				}
			}
			collections = results;
		}
	}
	
	/**
	 * Return <code>true</code> if the specified context-relative URI (and associated HTTP method)
	 * are protected by this security constraint.
	 * 
	 * @param uri
	 * @param method
	 * @return
	 */
	public boolean included(String uri, String method){
		
		// We cannot match without a valid request method]
		if(method == null){
			return false;
		}
		
		// Check all of the collections included in this constraint
		for (int i = 0; i < collections.length; i++) {
			if(!collections[i].findMethod(method)){
				continue;
			}
			String[] patterns = collections[i].findPatterns();
			for (int j = 0; j < patterns.length; j++) {
				if(matchPattern(uri, patterns[j])){
					return true;
				}
			}
		}
		
		// No collections in this constraint matches this request
		return false;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("SecurityConstraint[");
		for (int i = 0; i < collections.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(collections[i].getName());
		}
		sb.append("]");
		return sb.toString();
	}

	// --------------------------------------------- Private Methods

	/**
	 * 
	 * @param path
	 * @param pattern
	 * @return
	 */
	private boolean matchPattern(String path, String pattern) {
		// Normalize the argument strings
		if ((path == null) || (path.length() == 0))
			path = "/";
		if ((pattern == null) || (pattern.length() == 0))
			pattern = "/";

		// Check for exact match
		if (path.equals(pattern))
			return (true);

		// Check for path prefix matching
		if (pattern.startsWith("/") && pattern.endsWith("/*")) {
			pattern = pattern.substring(0, pattern.length() - 2);
			if (pattern.length() == 0)
				return (true); // "/*" is the same as "/"
			if (path.endsWith("/"))
				path = path.substring(0, path.length() - 1);
			while (true) {
				if (pattern.equals(path))
					return (true);
				int slash = path.lastIndexOf('/');
				if (slash <= 0)
					break;
				path = path.substring(0, slash);
			}
			return (false);
		}

		// Check for suffix matching
		if (pattern.startsWith("*.")) {
			int slash = path.lastIndexOf('/');
			int period = path.lastIndexOf('.');
			if ((slash >= 0) && (period > slash) && path.endsWith(pattern.substring(1))) {
				return (true);
			}
			return (false);
		}

		// Check for universal mapping
		if (pattern.equals("/"))
			return (true);

		return (false);
	}

}
