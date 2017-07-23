package ex10.pyrmont.realm;

import java.beans.PropertyChangeListener;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.catalina.Container;
import org.apache.catalina.Realm;
import org.apache.catalina.realm.GenericPrincipal;

public class SimpleRealm implements Realm {

	
	private Container container;
	
	private ArrayList<User> users = new ArrayList<>();
	
	public SimpleRealm() {
		createUserDatabase();
	}
	
	@Override
	public Container getContainer() {

		return this.container;
	}

	@Override
	public void setContainer(Container container) {

		this.container = container;
	}

	@Override
	public Principal authenticate(String username, String credentials) {
		System.out.println("SimpleRealm.authenticate()");
		if(username == null || credentials == null){
			return null;
		}
		User user = getUser(username, credentials);
		if(user == null){
			return null;
		}
		return new GenericPrincipal(this, user.username, user.password, user.getRoles());
	}

	@Override
	public Principal authenticate(String username, byte[] credentials) {
		return null;
	}

	@Override
	public Principal authenticate(String username, String digest, String nonce, String nc, String cnonce, String qop,
			String realm, String md5a2) {
		return null;
	}

	@Override
	public Principal authenticate(X509Certificate[] certs) {
		return null;
	}

	@Override
	public boolean hasRole(Principal principal, String role) {
		if(principal == null || role == null || !(principal instanceof GenericPrincipal)){
			return false;
		}
		GenericPrincipal gp = (GenericPrincipal) principal;
		if(gp.getRealm() != this){
			return false;
		}
		boolean result = gp.hasRole(role);
		return result;
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {

	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {

	}

	@Override
	public String getInfo() {
		return "A simple Realm implementation";
	}
	
	private User getUser(String username, String password){
		Iterator<User> iterator = users.iterator();
		while(iterator.hasNext()){
			User user = iterator.next();
			if(user.username.equals(username) && user.password.equals(password)){
				return user;
			}
		}
		return null;
	}
	
	private void createUserDatabase(){
		User user1 = new User("ken", "blackcomb");
		user1.addRole("manager");
		user1.addRole("programmer");
		User user2 = new User("cindy", "bamboo");
		user2.addRole("programmer");
		
		users.add(user1);
		users.add(user2);
	}
	
	
	class User{
		public String username;
		
		public String password;
		
		public ArrayList<String> roles = new ArrayList<>();

		public User(String username, String password) {
			this.username = username;
			this.password = password;
		}
		
		public void addRole(String role){
			this.roles.add(role);
		}
		
		public ArrayList<String> getRoles() {
			return roles;
		}
		
	}

}
