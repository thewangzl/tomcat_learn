package org.apache.catalina;

import java.beans.PropertyChangeListener;

/**
 * A <b>Loader</b> represents a Java ClassLoader implementation that can be used by a Container 
 * to load class files (with in a repository associated with the Loader) that are designed to
 * be reloaded upon request, as well as a mechanism to detect whether changes have occurred 
 * in the underlying repository.
 * <p>
 * In order for a <code>Loader</code> implementation to successfully operate with a <code>Context</code>
 * implementation that implements reloading, it must obey the following constraints:
 * <ul>
 * 	<li>Must implement <code>Lifecycle</code> so that the Context can indicate that a new class loader 
 * 		is required.
 * 	<li>The <code>start()</code> method must unconditionally create a new <code>ClassLoader</code> implementation.
 * 	<li>The <code>stop()</code> method must throw away its reference to the <code>ClassLoader</code> previously
 * 		utilized, so that the class loader, all classes loaded by it, and all objects of those classes, can be
 * 		garbage collected.
 * 	<li> allow a call to <code>stop()</code> to be followed by a call to <code>start()</code> on the same 
 * 		<code>Loader</code> instance.
 * 	<li>Based on a policy chosen by the implemetation, must call the <code>Context.reload()</code> method on
 * 		the owning <code>Context</code> when a change to one or more of the class files loaded by this class
 * 		loader is detected.
 * </ul>
 * 
 * @author thewangzl
 *
 */
public interface Loader {

	/**
	 * Return the Java class loader to be used by this Container.
	 * @return
	 */
	public ClassLoader getClassLoader();
	
	/**
	 * Return the container with which this Loader has been associated.
	 */
	public Container getContainer();
	
	/**
	 * Set the Container with which this Loader has been associated.
	 * 
	 * @param container
	 */
	public void setContainer(Container container);
	
	/**
	 * Return the DefaultContext with which whis Manager is associated.
	 * 
	 * @return
	 */
	public DefaultContext getDefaultContext();
	
	
	/**
	 * 
	 * @param defaultContext
	 */
	public void setDefaultContext(DefaultContext defaultContext);
	
	
	/**
	 * Return the "follow standard delegation model" flag used to configure our ClassLoader
	 * 
	 * @return
	 */
	public boolean getDelegate();
	
	
	public void setDelegate(boolean delegate);
	
	/**
	 * Return the reloadable flag for this Loader
	 * @return
	 */
	public boolean getReloadable();
	
	
	public void setReloadable(boolean reloadable);
	
	/**
	 * Add a new repository to the set of repositories for this class loader
	 * 
	 * @param repository
	 */
	public void addRepository(String repository);
	
	/**
	 * Return the set of repositories defined for this class loader.
	 * If none are defined, a zero-length array is returned.
	 * 
	 * @return
	 */
	public String[] findRepositories();
	
	/**
	 * Has the internal repository associated with this Loader been modified,
	 * such that the loaded classes should be reloaded?
	 * @return
	 */
	public boolean modified();
	
	public void addPropertyChangeListener(PropertyChangeListener listener);
	
	
	public void removePropertyChangeListener(PropertyChangeListener listener);
	
	
	
}
