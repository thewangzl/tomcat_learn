package org.apache.catalina.loader;

/**
 * Internal interface that <code>ClassLoader</code> implementations  may optionally implement 
 * to support the auto-reload funtionality of the classloader associated with the context.
 * 
 * @author thewangzl
 *
 */
public interface Reloader {

	
	public void addRepository(String repository);
	
	
	public String[] findRepositories();
	
	/**
	 * 
	 * @return
	 */
	public boolean modified();
	
	
}
