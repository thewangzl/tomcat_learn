package org.apache.catalina.users;

import org.apache.catalina.Role;
import org.apache.catalina.UserDatabase;

public abstract class AbstractRole implements Role {

	 // ----------------------------------------------------- Instance Variables


    /**
     * The description of this Role.
     */
    protected String description = null;


    /**
     * The role name of this Role.
     */
    protected String rolename = null;


    // ------------------------------------------------------------- Properties


    /**
     * Return the description of this role.
     */
    public String getDescription() {

        return (this.description);

    }


    /**
     * Set the description of this role.
     *
     * @param description The new description
     */
    public void setDescription(String description) {

        this.description = description;

    }


    /**
     * Return the role name of this role, which must be unique
     * within the scope of a {@link UserDatabase}.
     */
    public String getRolename() {

        return (this.rolename);

    }


    /**
     * Set the role name of this role, which must be unique
     * within the scope of a {@link UserDatabase}.
     *
     * @param rolename The new role name
     */
    public void setRolename(String rolename) {

        this.rolename = rolename;

    }


    /**
     * Return the {@link UserDatabase} within which this Role is defined.
     */
    public abstract UserDatabase getUserDatabase();


    // --------------------------------------------------------- Public Methods


    // ------------------------------------------------------ Principal Methods


    /**
     * Make the principal name the same as the role name.
     */
    public String getName() {

        return (getRolename());

    }

}
