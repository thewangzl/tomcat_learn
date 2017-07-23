package org.apache.catalina.users;

import org.apache.catalina.UserDatabase;

public class MemoryRole extends AbstractRole {

	// ----------------------------------------------------------- Constructors


    /**
     * Package-private constructor used by the factory method in
     * {@link MemoryUserDatabase}.
     *
     * @param database The {@link MemoryUserDatabase} that owns this role
     * @param rolename Role name of this role
     * @param description Description of this role
     */
    MemoryRole(MemoryUserDatabase database,
               String rolename, String description) {

        super();
        this.database = database;
        setRolename(rolename);
        setDescription(description);

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The {@link MemoryUserDatabase} that owns this role.
     */
    protected MemoryUserDatabase database = null;


    // ------------------------------------------------------------- Properties


    /**
     * Return the {@link UserDatabase} within which this role is defined.
     */
    public UserDatabase getUserDatabase() {

        return (this.database);

    }


    // --------------------------------------------------------- Public Methods


    /**
     * <p>Return a String representation of this role in XML format.</p>
     */
    public String toString() {

        StringBuffer sb = new StringBuffer("<role rolename=\"");
        sb.append(rolename);
        sb.append("\"");
        if (description != null) {
            sb.append(" description=\"");
            sb.append(description);
            sb.append("\"");
        }
        sb.append("/>");
        return (sb.toString());

    }
}
