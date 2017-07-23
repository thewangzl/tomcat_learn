package org.apache.catalina;

import java.security.Principal;

public interface Role extends Principal {

	// ------------------------------------------------------------- Properties


    /**
     * Return the description of this role.
     */
    public String getDescription();


    /**
     * Set the description of this role.
     *
     * @param description The new description
     */
    public void setDescription(String description);


    /**
     * Return the role name of this role, which must be unique
     * within the scope of a {@link UserDatabase}.
     */
    public String getRolename();


    /**
     * Set the role name of this role, which must be unique
     * within the scope of a {@link UserDatabase}.
     *
     * @param rolename The new role name
     */
    public void setRolename(String rolename);


    /**
     * Return the {@link UserDatabase} within which this Role is defined.
     */
    public UserDatabase getUserDatabase();
}
