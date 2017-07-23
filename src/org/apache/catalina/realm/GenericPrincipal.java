package org.apache.catalina.realm;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import org.apache.catalina.Realm;

public class GenericPrincipal implements Principal {

	protected String name;
	
	protected String password;
	
	protected Realm realm;
	
	protected String[] roles = new String[0];
	
	public GenericPrincipal(Realm realm, String name, String password) {
		this(realm, name, password, null);
	}
	public GenericPrincipal(Realm realm, String name, String password, List<String> roles) {
		super();
		this.realm = realm;
		this.name = name;
		this.password = password;
		if(roles != null){
			this.roles = roles.toArray(new String[roles.size()]);
			Arrays.sort(this.roles);
		}
	}


	@Override
	public String getName() {
		return this.name;
	}
	
	public String getPassword() {
		return password;
	}
	
	public Realm getRealm() {
		return realm;
	}
	
	public String[] getRoles() {
		return roles;
	}
	
	public boolean hasRole(String role){
		if("*".equals(role)){
			return true;
		}
		if(role == null){
			return false;
		}
		return Arrays.binarySearch(roles, role) >= 0;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("GenericPrincipal[");
		sb.append(this.name).append("]");
		return sb.toString();
	}
}
