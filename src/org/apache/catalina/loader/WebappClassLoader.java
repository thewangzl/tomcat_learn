package org.apache.catalina.loader;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import javax.naming.directory.DirContext;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;

public class WebappClassLoader extends URLClassLoader implements Reloader,Lifecycle {

	/**
	 * Associated directory context giving access to the resources in this webapp
	 */
	protected DirContext resources;
	
	protected int debug = 0;
	
	protected boolean delegate;
	
	private String[] repositories = new String[0];
	
	
	protected ArrayList available = new ArrayList<>();
	
	/**
	 * has external repositories
	 */
	private boolean hasExternalRepositories;
	
	public DirContext getResources() {
		return resources;
	}
	
	public void setResources(DirContext resources) {
		this.resources = resources;
	}
	
	public int getDebug() {
		return debug;
	}
	
	
	public void setDebug(int debug) {
		this.debug = debug;
	}
	
	public boolean getDelegate() {
		return delegate;
	}
	public void setDelegate(boolean delegate) {
		this.delegate = delegate;
	}
	
	
	public WebappClassLoader(URL[] urls) {
		super(urls);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Add a new repository to the set of places this ClassLoader 
	 * can look for classes to be loaded.
	 * 
	 * @param repository Name of a resource of a classes to be loaded, such a a directory
	 * 					pathname, a JAR file pathname, or a ZIP file pathname.
	 */
	@Override
	public void addRepository(String repository) {

		//Ignoe any of the standard repositories, as they are set up using
		//either addJar or addRepository
		if(repository.startsWith("/WEB-INF/lib") || repository.startsWith("/WEB-INF/classes")){
			return;
		}
		
		//Add this repository to our underlying class loader
		try {
			URL url = new URL(repository);
			super.addURL(url);
			hasExternalRepositories = true;
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e.toString());
		}
	}

	@Override
	public String[] findRepositories() {
		return this.repositories;
	}

	@Override
	public boolean modified() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() throws LifecycleException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() throws LifecycleException {
		// TODO Auto-generated method stub
		
	}

}
