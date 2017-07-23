package org.apache.catalina.users;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.catalina.Group;
import org.apache.catalina.Role;
import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.catalina.util.StringManager;

public class MemoryUserDatabase implements UserDatabase {

	protected String id;

	protected HashMap<String, Group> groups = new HashMap<>();

	protected HashMap<String, Role> roles = new HashMap<>();

	protected HashMap<String, User> users = new HashMap<>();

	protected String pathname = "conf/tomcat-users.xml";

	protected String pathnameOld = pathname + ".old";

	protected String pathnameNew = pathname + ".new";

	protected static final StringManager sm = StringManager.getManager(Constants.Package);

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public Iterator<Group> getGroups() {
		synchronized (groups) {
			return groups.values().iterator();
		}
	}

	@Override
	public Iterator<Role> getRoles() {
		synchronized (roles) {
			return roles.values().iterator();
		}
	}

	@Override
	public Iterator<User> getUsers() {
		synchronized (users) {
			return users.values().iterator();
		}
	}

	public String getPathname() {
		return pathname;
	}

	public void setPathname(String pathname) {
		this.pathname = pathname;
		this.pathnameOld = pathname + ".old";
		this.pathnameNew = pathname + ".new";
	}

	@Override
	public void open() throws Exception {
		synchronized (groups) {
			synchronized (users) {
				//
				users.clear();
				groups.clear();
				roles.clear();

				//
				File file = new File(pathname);
				if (!file.isAbsolute()) {
					file = new File(System.getProperty("catalina.base"), pathname);
				}
				if (!file.exists()) {
                    return;
                }
                FileInputStream fis = new FileInputStream(file);

			}
		}

	}

	@Override
	public void close() throws Exception {

		save();
		synchronized (groups) {
			synchronized (users) {
				users.clear();
				groups.clear();
			}
		}

	}

	@Override
	public void save() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public Group createGroup(String groupname, String description) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Role createRole(String rolename, String description) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User createUser(String username, String password, String fullName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Group findGroup(String groupname) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Role findRole(String rolename) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User findUser(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeGroup(Group group) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeRole(Role role) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeUser(User user) {
		// TODO Auto-generated method stub

	}

}
