package org.apache.catalina.session;

public abstract class PersistentManager extends PersistentManagerBase {

	
	/**
     * The descriptive information about this implementation.
     */
    private static final String info = "PersistentManager/1.0";


    /**
     * The descriptive name of this Manager implementation (for logging).
     */
    protected static String name = "PersistentManager";


    @Override
    public String getInfo() {
    	return info;
    }
    
    @Override
    public String getName() {
    	return name;
    }
}
