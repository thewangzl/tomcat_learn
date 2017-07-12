package org.apache.catalina;

/**
 * Used to store the default configuration a Host will use when creating a Context.
 * A Context configured in server.xml can override these defaults by setting the 
 * Context attribute <code>override="true"</code>
 * 
 * @author thewangzl
 *
 */
public interface DefaultContext {

	void importDefaultContext(Context context);

	//TODO
	
}
