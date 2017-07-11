package org.apache.catalina.loader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;

public class WebappClassLoader extends URLClassLoader implements Reloader,Lifecycle {

	/**
	 * Associated directory context giving access to the resources in this webapp
	 */
	protected DirContext resources;
	
	protected ArrayList<Extension> available = new ArrayList<>();
	
	/**
	 * The cache of ResourceEntry for classes and resources we have loaded, 
	 * keyed by resource name
	 */
	protected HashMap<String,ResourceEntry> resourceEntries = new HashMap<>();
	
	/**
	 * the list of not found resources.
	 */
	protected HashMap<String,String> notFoundResources = new HashMap<>();
	
	protected int debug = 0;
	
	/**
	 * Should this class loader delegate to the parent class loader <strong>before</strong> searching 
	 * its own repositories (i.e. the usual Java2 delegation model) ? If set to <code>false</code>,
	 * This class loader will search its own repositories first, and delegate to the parent only if 
	 * the class or resource is not found locally.
	 */
	protected boolean delegate;
	
	/**
	 * The list of local repositories, in the order they should be searched for locally loaded classes
	 * or resources.
	 */
	private String[] repositories = new String[0];
	
	/**
	 * Repositories translated as path in the work directory (for Jasper originally), but which is used 
	 * to generate fake ULS URLS should getURLS be called
	 */
	protected File[] files = new File[0];
	
	/**
	 * The list of JARs, in the order they should be searched for locally loaded classes or resources.
	 */
	protected JarFile[] jarFiles = new JarFile[0];
	
	/**
	 * This list of JARs, in the order they should be searched for locally loaded classes or resources.
	 */
	protected File[] jarRealFiles = new File[0];
	
	/**
	 * The path which will be monitored for added Jar files
	 */
	protected String jarPath;
	
	/**
	 * The list of JARs, in the order they should be searched for locally loaded classes or resources.
	 */
	protected String[] jarNames = new String[0];
	
	/**
	 * The list of JARs last modified dates, in the order they should be searched for locally loaded classes or resources.
	 */
	protected long[] lastModifiedDates = new long[0];
	
	/**
	 * This list of resources which should be cheched when checking for modifications.
	 */
	protected String[] paths = new String[0];
	
	/**
	 * The set of optional packages (formerly standard extensions) that are required 
	 * in the repositories associated with this class loader.Each object in this list
	 * is of type <code>org.apache.catalina.loader.Extension</code>
	 */
	protected ArrayList<Extension> required = new ArrayList<>();
	
	/**
	 * A list of read File and Jndi Permission's required if this loader is for a web 
	 * application context.
	 */
	private ArrayList<Permission> permissionList = new ArrayList<>();
	
	/**
	 * The PermissionCollection for each CodeSource for a web application context.
	 */
	private HashMap<String, PermissionCollection> loaderPC = new HashMap<>();
	
	/**
	 * Instance of the SecurityManager installed
	 */
	private SecurityManager securityManager;
	
	/**
	 * The parent class loader
	 */
	private ClassLoader parent;
	
	/**
	 * The system class loader
	 */
	private ClassLoader system;
	
	/**
	 * Has this component been started?
	 */
	protected boolean started;
	
	
	
	/**
	 * has external repositories
	 */
	protected boolean hasExternalRepositories;
	
	/**
	 * All permission
	 */
	private Permission allPermission = new AllPermission();
	
	/**
	 * The set of trigger classes that will cause a proposed repository not to be added if 
	 * this class is visible to the class loader that loaded this factory class. Typically,
	 * trigger classes will be listed for components that have been integrated into the JDK 
	 * for later versions, but where the correspponding JAR files are required to run on 
	 * earlier versions.
	 * 
	 */
	private static final String[] triggers = {
			"javax.servlet.Servlet"				//Servlet API
	};
	
	/**
	 * Set of package names which are not allowed to be loaded from a webapp
	 * class loader without delegating first.
	 */
	private static final String[] packageTriggers = {
		"javax",							//Java extensions
		"org.xml.sax",						//SAX 1 & 2
		"org.w3c.dom",						//DOM 1 & 2
		"org.apache.xerces",				//Xerces 1 & 2
		"org.apache.xalan"					//Xalan
	};
	
	public WebappClassLoader(){
		super(new URL[0]);
		this.parent = getParent();
		system = getSystemClassLoader();
		securityManager = System.getSecurityManager();
		
		if(securityManager != null){
			refreshPolicy();
		}
		
	}
	
	/**
	 * Construct a new classLoader with no defined repositories and no parent ClassLoader
	 * @param parent
	 */
	public WebappClassLoader(ClassLoader parent) {
		super(new URL[0], parent);
		this.parent = parent;
		system = getSystemClassLoader();
		securityManager = System.getSecurityManager();
		
		if(securityManager != null){
			refreshPolicy();
		}
	}
	
	//-----------------------------------------------ClassLoader Methods --------------------
	
	/**
	 * Find the specified class in our local repositories, f possible.
	 * if not found, throw <code>ClassNotFoundException</code>
	 * 
	 * @param name Name of the class to be loaded
	 * @return
	 * @throws ClassNotFoundException
	 */
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {

		if(debug >= 3){
			log("findClass(" + name + ")");
		}
		
		//1.Permission to define this class when using a SecurityManager
		if(securityManager != null){
			int i = name.lastIndexOf(".");
			if(i >= 0){
				try{
					if(debug >= 4){
						log("securityManager.checkPackageDefinition");
					}
					securityManager.checkPackageDefinition(name.substring(0, i));
				}catch (Exception e){
					if(debug >= 4){
						log(" --> Extension -->ClassNotFoundException",e);
					}
					throw new ClassNotFoundException(name);
				}
			}
		}
		
		//Ask our superclass to locate this class, if possible
		//(throws ClassNotFoundException if it is not found)
		Class<?> clazz = null;
		try{
			if(debug >= 4){
				log("findClassInternal(" + name+")");
			}
			try{
				clazz = findClassInternal(name);
			}catch(ClassNotFoundException cnfe){
				if(!hasExternalRepositories){
					throw cnfe;
				}
			}catch (AccessControlException ace) {
				ace.printStackTrace();
				throw new ClassNotFoundException(name);
			}catch (RuntimeException  re) {
				if(debug >= 4){
					log(" --> RuntimeExeption Rethrow ",re);
				}
				throw re;
			}
			
			if(clazz == null && hasExternalRepositories){
				try{
					clazz = super.findClass(name);
				}catch(AccessControlException ace){
					throw new ClassNotFoundException(name);
				}catch (RuntimeException  re) {
					if(debug >= 4){
						log(" --> RuntimeExeption Rethrow ",re);
					}
					throw re;
				}
			}
			
			if(clazz == null){
				if(debug >= 3){
					log(" --> Returning ClassNotFoundException");
				}
				throw new ClassNotFoundException(name);
			}
		}catch(ClassNotFoundException e){
			if(debug >= 3){
				log(" --> Parsing on ClassNotFoundException", e);
			}
			throw e;
		}
		
		//Return the class we have located
		if(debug >= 4){
			log("  Returning class " + clazz);
		}
		if(debug >= 4 &&clazz != null){
			log("Loaded by" + clazz.getClassLoader());
		}
		
		return super.findClass(name);
	}
	
	/**
	 * Find the specified resource in our local repository, and return a <code>URL</code> refering to it,
	 * or <code>null</code> if this resource cannot be found.
	 * 
	 * @param name
	 * @return
	 */
	@Override
	public URL findResource(final String name) {
		if(debug >= 3){
			log("findResource(" + name + ")");
		}
		URL url = null;
		
		ResourceEntry entry = resourceEntries.get(name);
		if(entry == null){
			if(securityManager != null){
				PrivilegedAction<ResourceEntry> dp = new PrivilegedFindResource(name, name);
				entry = AccessController.doPrivileged(dp);
			}else{
				entry = findResourceInternal(name, name);
			}
		}
		if(entry != null){
			url = entry.source;
		}
		
		if(url == null && hasExternalRepositories){
			url = super.findResource(name);
		}
		if(debug >= 3){
			if(url != null){
				log("  --> Return '" + url.toString() + "'");
			}else{
				log("  --> Resource not found, returning null");
			}
		}
		return url;
	}
	
	/**
	 * return an enumeration of <code>URLs</code> representing all of the resource with the 
	 * given name . If no resources with this name are found, return an empty enumeration.
	 * 
	 * @param name
	 * @return
	 * @throws IOException
	 */
	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		if(debug >= 3){
			log("findResources(" + name + ")");
		}
		Vector<URL> result = new Vector<>();
		
		int jarFilesLength = jarFiles.length;
		int repositoriesLength = repositories.length;
		
		int i = 0;
		//Looking at the repositories
		for ( i = 0; i < repositoriesLength; i++) {
			try{
				String fullPath = repositories[i] + name;
				resources.lookup(fullPath);
				//Note: Not getting an exception here means the resources was found
				result.addElement(getURL(new File(files[i], name)));
			}catch(NamingException e){
				//Igonre
			}
		}
		
		//Looking at the JAR files
		for( i = 0; i < jarFilesLength; i++){
			JarEntry jarEntry = jarFiles[i].getJarEntry(name);
			if(jarEntry != null){
				try{
					String jarFakeUrl = getURL(jarRealFiles[i]).toString();
					jarFakeUrl = "jar:" + jarFakeUrl + "!/" + name;
					result.addElement(new URL(jarFakeUrl));
				}catch(MalformedURLException e){
					//Ignore
				}
			}
		}
		//Adding the results of a call to the superclass
		if(hasExternalRepositories){
			Enumeration<URL> otherResourcePaths = super.findResources(name);
			while(otherResourcePaths.hasMoreElements()){
				result.addElement(otherResourcePaths.nextElement());
			}
		}
		
		return result.elements();
	}
	
	@Override
	public URL getResource(String name) {
		
		if(debug >= 2){
			log("getResource(" + name +")");
		}
		URL url = null;
		
		//1.Delegate to parent if requested
		if(delegate){
			if(debug >= 3){
				log("Delegate to parent classloader");
			}
			ClassLoader loader = parent;
			if(loader == null){
				loader = parent;
			}
			url = loader.getResource(name);
			if(url != null){
				if(debug >= 2){
					log(" --> Returning '" + url.toString() + "'");
				}
				return url;
			}
		}
		
		//2. Search local repositories
		if(debug >= 3){
			log("Searching local repositories");
		}
		url = findResource(name);
		if(url != null){
			if(debug >= 2){
				log(" --> Returning '" + url.toString() + "'");
			}
			return url;
		}
		
		//3.Delegate to parent unconditionally if not already attempted
		if(!delegate){
			ClassLoader loader = parent;
			if(loader == null){
				loader = system;
			}
			url = loader.getResource(name);
			if(url != null){
				if(debug >= 2){
					log("  --> Returning '" + url.toString() + "'");
				}
				return url;
			}
		}
		
		//4.Resoure was not found
		if(debug >= 2){
			log("  --> Resource not found, returning null");
		}
		return null;
	}
	
	@Override
	public InputStream getResourceAsStream(String name) {
		
		if(debug >= 2){
			log("getResourceAsStream(" + name +")");
		}
		InputStream stream = null;
		
		//0. Check for a cached copy of this resource
		stream = findLoadedResource(name);
		if(stream != null){
			if(debug >= 2){
				log("--> returing stream from cache");
			}
			return stream;
		}
		
		//1.Delegate to parent if requested
		if(delegate){
			if(debug >= 3){
				log("Delegating to parent classloader");
			}
			ClassLoader loader = parent;
			if(loader == null){
				loader = system;
			}
			stream = loader.getResourceAsStream(name);
			if(stream != null){
				//FIXME - cache??
				if(debug >= 2){
					log("--> Returning stream from parent");
				}
				return stream;
			}
		}
		
		//2.Search local repositories
		if(debug >= 3){
			log("Searching local repositories");
		}
		URL url = findResource(name);
		if(url != null){
			//FIXME -cache?
			if(debug >= 2){
				log(" --> Returning stream from local");
			}
			stream = findLoadedResource(name);
			try{
				if(hasExternalRepositories && stream == null){
					stream = url.openStream();
				}
			}catch(IOException e){
				//Ignore;
			}
			if(stream != null){
				return stream;
			}
		}
		
		//3. Delegate to parent unconditionally
		if(!delegate){
			if(debug >= 3){
				log(" Delegating to parent classloader");
			}
			ClassLoader loader = parent;
			if(loader == null){
				loader = system;
			}
			stream = loader.getResourceAsStream(name);
			if(stream != null){
				//FIXME -cache ?
				if(debug >= 2){
					log(" --> Returning stream from parent");
				}
				return stream;
			}
		}
		
		//4.Resource was not found
		if(debug >= 2){
			log(" --> Resource not found, returning null");
		}
		return null;
	}
	
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {

		return this.loadClass(name,false);
	}
	
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		
		if(debug >= 2){
			log("loadClass(" + name + "," + resolve + ")");
		}
		Class<?> clazz = null;
		
		//Don't load classes if class loader is stopped
		if(!started){
			log("Lifecycle error: CL stopped" );
			throw new ClassNotFoundException(name);
		}
		
		//0. Check our previously loaded local class cache
		clazz = findLoadedClass0(name);
		if(clazz != null){
			if(debug >= 3){
				log("Returning class from cache");
			}
			if(resolve){
				resolveClass(clazz);
			}
			return clazz;
		}
		
		//0.1 Check our previously loaded class cache
		clazz = findLoadedClass(name);
		if(clazz != null){
			if(debug >= 3){
				log("Returning class from cache");
			}
			if(resolve){
				resolveClass(clazz);
			}
			return clazz;
		}
		
		//0.2 Try loading the class with the system class loader, to prevent 
		// the webapp from overriding J2SE classes
		try{
			clazz = system.loadClass(name);
			if(clazz != null){
				if(resolve){
					resolveClass(clazz);
				}
				return clazz;
			}
		}catch(ClassNotFoundException e){
			//Ignore
		}
		
		// 0.5 Permission to access this class when using a SecurityManager
		if(securityManager != null){
			int i = name.lastIndexOf('.');
			if(i >= 0){
				try{
					securityManager.checkPackageAccess(name.substring(0, i));
				}catch(SecurityException e){
					String error = "Security Violation, attempt to use Restricted Class: " + name;
					log(error);
					e.printStackTrace();
					throw new ClassNotFoundException(name); 
				}
			}
		}
		
		boolean delegatedLoad = delegate || filter(name);
		
		// 1.Delegate to our parent if requested
		if(delegatedLoad){
			if(debug >= 3){
				log("Delegating to parent classloader");
			}
			ClassLoader loader = parent;
			if(loader == null){
				loader = system;
			}
			try{
				clazz = loader.loadClass(name);
				if(clazz != null){
					if(debug >= 3){
						log("Loading class from parent");
					}
					if(resolve){
						resolveClass(clazz);
					}
					return clazz;
				}
			}catch(ClassNotFoundException e){
				;
			}
		}
		
		// 2. Search local repositories
		if(debug >= 3){
			log("Searching local repositories");
		}
		try{
			clazz = findClass(name);
			if(clazz != null){
				if(debug >= 3){
					log("Loading clazz from local repository");
				}
				if(resolve){
					resolveClass(clazz);
				}
				return clazz;
			}
		}catch(ClassNotFoundException e){
			;
		}
		
		//3. Delegate to parent unconditionally
		if(!delegatedLoad){
			if(debug >= 3){
				log("Delegating to parent classloader");
			}
			ClassLoader loader = parent;
			if(loader == null){
				loader = system;
			}
			try{
				clazz = loader.loadClass(name);
				if(clazz != null){
					if(debug >= 3){
						log("Loading class from parent");
					}
					if(resolve){
						resolveClass(clazz);
					}
					return clazz;
				}
			}catch(ClassNotFoundException e){
				;
			}
		}
		
		//This class was not found
		throw new ClassNotFoundException(name);
	}
	
	@Override
	protected PermissionCollection getPermissions(CodeSource codesource) {
		String codeUrl = codesource.getLocation().toString();
		PermissionCollection pc;
		if((pc = loaderPC.get(codeUrl)) == null){
			pc = super.getPermissions(codesource);
			if(pc != null){
				Iterator<Permission> perms = permissionList.iterator();
				while(perms.hasNext()){
					Permission p = perms.next();
					pc.add(p);
				}
				loaderPC.put(codeUrl, pc);
			}
		}
		return pc;
	}
	
	@Override
	public URL[] getURLs() {

		URL[] external = super.getURLs();
		
		int filesLength = files.length;
		int jarFilesLength = jarRealFiles.length;
		int length = filesLength + jarFilesLength + external.length;
		
		URL[] urls = new URL[length];
		
		try {
			for (int i = 0; i < length; i++) {
				if(i < filesLength){
					urls[i] = getURL(files[i]);
				}else if(i < filesLength + jarFilesLength){
					urls[i] = getURL(jarRealFiles[i -filesLength]);
				}else{
					urls[i] = external[i - filesLength - jarFilesLength];
				}
			}
			return urls;
		} catch (MalformedURLException e) {
			return new URL[0];
		}
	}
	
	//---------------------------------------------------Other methods
	
	/**
	 * Finds the resource with the given name if it has previously been loaded and cached by this 
	 * class loader, and return and input stream to the resource data. If this resource has not 
	 * been cached, return <code>null</code>.
	 * 
	 * @param name
	 * @return
	 */
	protected InputStream findLoadedResource(String name){
		ResourceEntry entry  = resourceEntries.get(name);
		if(entry != null){
			if(entry.binaryContext != null){
				return new ByteArrayInputStream(entry.binaryContext);
			}
		}
		return null;				//FIXME - findLoadedResources()
	}
	
	/**
	 * Finds the class with the given name if has previously been loaded and cached by this class loader,
	 * and return the Class object. If this class has not been cached, return <code>null</code>.
	 *  	
	 * @param name
	 * @return
	 */
	protected Class<?> findLoadedClass0(String name){
		ResourceEntry entry = resourceEntries.get(name);
		if(entry != null){
			return entry.loadedClass;
		}
		return null;
	}
	
	
	
	/**
	 * Filter classes
	 * @param name
	 * @return
	 */
	protected boolean filter(String name){
		if(name == null){
			return false;
		}
		
		//Looking up the package
		String packageName = null;
		int pos = name.lastIndexOf('.');
		if(pos != -1){
			packageName = name.substring(0, pos);
		}else{
			return false;
		}
		
		for (int i = 0; i < packageTriggers.length; i++) {
			if(packageName.startsWith(packageTriggers[i])){
				return false;
			}
		}		
		return true;
	}
	
	/**
	 * Find specified class in local repositories.
	 * 
	 * @param name
	 * @return		the loaded class, or null if the class isn't found
	 * @throws ClassNotFoundException
	 */
	protected Class<?> findClassInternal(String name) throws ClassNotFoundException{
		
		if(!validate(name)){
			throw new ClassNotFoundException(name);
		}
		
		String tempPath = name.replace('.', '/');
		String classPath = tempPath + ".class";
		
		ResourceEntry entry = null;
		if(securityManager != null){
			PrivilegedAction<ResourceEntry> dp = new PrivilegedFindResource(name, classPath);
			entry = AccessController.doPrivileged(dp);
		}else{
			entry = findResourceInternal(name, classPath);
		}
		
		if(entry == null || entry.binaryContext == null){
			throw new ClassNotFoundException(name);
		}
		
		Class<?> clazz = entry.loadedClass;
		if(clazz != null){
			return clazz;
		}
		//lookup the package
		String packageName = null;
		int pos = name.lastIndexOf('.');
		if(pos != -1){
			packageName = name.substring(0, pos);
		}
		
		Package pkg = null;
		if(packageName != null){
			pkg = getPackage(packageName);
			
			//Define the package (if null)
			if(pkg == null){
				if(entry.manifest == null){
					definePackage(packageName, null, null, null, null, null, null, null);
				}else{
					definePackage(packageName, entry.manifest, entry.codeBase);
				}
			}
		}
		
		// Create the code source object
		CodeSource codeSource = new CodeSource(entry.codeBase, entry.certificates);
		
		if(securityManager != null){
			//Checking sealing
			if(pkg != null){
				boolean sealCheck = true;
				if(pkg.isSealed()){
					sealCheck = pkg.isSealed(entry.codeBase);
				}else{
					sealCheck = (entry.manifest == null) || !isPackageSealed(packageName, entry.manifest);
				}
				if(!sealCheck){
					throw new SecurityException("Sealing violation loading " + name + " : Package " + packageName + "is sealed.");
				}
			}
		}
		
		if(entry.loadedClass == null){
			synchronized (this) {
				if(entry.loadedClass == null){
					clazz = defineClass(name, entry.binaryContext, 0, entry.binaryContext.length, codeSource);
					entry.loadedClass = clazz;
				}else{
					clazz = entry.loadedClass;
				}
			}
		}else{
			clazz = entry.loadedClass;
		}
		return clazz;
	}
	

	
	/**
	 * Refresh the system policy file, to pick up eventual changes.
	 */
	protected void refreshPolicy() {
		try{
			// The policy file may have been modified to adjust permissions,
			// so we're reloading it when loading or reloading a Context
			Policy policy = Policy.getPolicy();
			policy.refresh();
		}catch(AccessControlException e){
			//
		}
	}

	/**
	 * Validate a classname, As per SRV.9.7.2,we must restict loading of classes from J2SE (java.*)
	 * and classes of the servlet API (javax.servlet.*). That should enhance robustness and prevent 
	 * a number of user error (where an older version of servlet.jar would be present in /WEB-INF/lib).
	 * 
	 * @param name class name
	 * @return	true if the name is valid
	 */
	private boolean validate(String name){
		if(name == null){
			return false;
		}
		if(name.startsWith("java.")){
			return false;
		}
		return true;
	}

	/**
	 * Have one or more classes or resources been modified so that a reload is appropridate ?
	 */
	@Override
	public boolean modified() {
		if(debug >= 2){
			log("modified()");
		}
		
		//Checking for modified loaded resources
		int length = paths.length;
		
		//A rare race condition can occur in the updates of the two arrays It's totally
		//ok if the lastest class added is not checked (it will be checked the next time)
		int length2 = lastModifiedDates.length;
		if(length > length2){
			length = length2;
		}
		
		for(int i = 0; i < length; i++){
			try {
				long lastModified = ((ResourceAttributes)resources.getAttributes(paths[i])).getLastModified();
				if(lastModified != lastModifiedDates[i]){
					log(" Resource '" + paths[i] + "' was modified; Date is now: "
							+ new Date(lastModified) + " was :" + new Date(lastModifiedDates[i]));
					return true;
				}
				
			} catch (NamingException e) {
				log("Resource '" + paths[i] + "' is missing");
				return true;
			}
		}
		
		length = jarNames.length;
		
		//Check if JARs have been added or removed
		if(getJarPath() != null){
			try {
				NamingEnumeration<NameClassPair> enums = resources.list(getJarPath());
				int i =0;
				while(enums.hasMoreElements() && i < length){
					NameClassPair ncPair = enums.nextElement();
					String name = ncPair.getName();
					//Ignore non JARs present in the lib folder
					if(!name.endsWith(".jar")){
						continue;
					}
					if(!name.equals(jarNames[i])){
						//Missing JAR
						log("Additional JARs have been added:'" + name + "'");
						return true;
					}
					i++;
				}
				if(enums.hasMoreElements()){
					while(enums.hasMoreElements()){
						NameClassPair ncPair = enums.nextElement();
						String name = ncPair.getName();
						//Additional non-JAR files are allowed 
						if(name.endsWith(".jar")){
							//There are more JARS
							log("Additional JARs have been added");
							return true;
						}
					}
				}else if (i <jarNames.length){
					//There was less JARs
					log("Additional JARs have been added");
					return true;
				}
			} catch (NamingException e) {
				if(debug >= 2){
					log("Failed tracking modifications of '" + getJarPath() + "' ");
				}
			}catch (ClassCastException e) {
				log("Failed tracking modifications of '" + getJarPath() + "' :" + e.getMessage());
			}
		}
		
		//No classes have been modified
		return false;
	}
	
	/**
	 * Start the class loader.
	 */
	@Override
	public void start() throws LifecycleException {
		started = true;
	}

	/**
	 * Stop the class loader
	 */
	@Override
	public void stop() throws LifecycleException {

		started = false;
		int length = jarFiles.length;
		for (int i = 0; i < length; i++) {
			try {
				jarFiles[i].close();
				jarFiles[i] = null;
			} catch (IOException e) {
				;//Ignore
			}
		}
		
		notFoundResources.clear();
		resourceEntries.clear();
		repositories = new String[0];
		files = new File[0];
		jarFiles = new JarFile[0];
		jarRealFiles = new File[0];
		jarPath = null;
		jarNames = new String[0];
		lastModifiedDates = new long[0];
		paths = new String[0];
		hasExternalRepositories = false;
		
		required.clear();
		permissionList.clear();
		loaderPC.clear();
	}

	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		return new LifecycleListener[0];
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		
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

	/**
	 * Add a new repository to the set of places this ClassLoader can look for classes to be loaded.
	 * 
	 * @param repository Name of a source of classes to be loaded, such as a directory pathname, 
	 * 					a JAR file pathname, or a ZIP file pathname
	 * @param file
	 */
	public synchronized void addRepository(String repository, File file) {
		//Note: There should be only one (of course), but I think we should 
		//keep this a bit generic
		if(repository == null){
			return ;
		}
		if(debug >= 1){
			log("addRepository(" + repository + ")");
		}
		
		int i;
		
		String[] result = new String[repositories.length + 1];
		for (i = 0; i < repositories.length; i++) {
			result[i] = repositories[i];
		}
		result[repositories.length] = repository;
		repositories = result;
		
		//Add the file to the list
		File[] result2 = new File[files.length + 1];
		for (i = 0; i < files.length; i++) {
			result2[i] = files[i];
		}
		result2[files.length] = file;
		files = result2;
		
	}

	public synchronized void addJar(String jar, JarFile jarFile, File file) throws IOException {

		if(jar == null || jarFile == null || file == null){
			return;
		}
		if(debug >= 1){
			log("addJar(" + jar+")");
		}
		int i ;
		if(jarPath != null && jar.startsWith(jarPath)){
			String jarName = jar.substring(jarPath.length());
			while(jarName.startsWith("/")){
				jarName = jarName.substring(1);
			}
			
			String[] result = new String[jarNames.length + 1];
			for (i = 0; i < jarNames.length; i++) {
				result[i] = jarNames[i];
			}
			result[jarNames.length] = jarName;
			jarNames = result;
		}
		
		//Register the JAR for tracking
		try {
			long lastModified = ((ResourceAttributes) resources.getAttributes(jar)).getLastModified();

			String[] result = new String[paths.length + 1];
			for (i = 0; i < paths.length; i++) {
				result[i] = paths[i];
			}
			result[paths.length] = jar;
			paths = result;
			
			long[] result2 = new long[lastModifiedDates.length + 1];
			for ( i = 0; i < lastModifiedDates.length; i++) {
				result2[i] = lastModifiedDates[i];
			}
			result2[lastModifiedDates.length] = lastModified;
			lastModifiedDates = result2;
			
		} catch (NamingException e) {
			//Ignore
		}
		
		//If the JAR currently contains invalid classes, don't actually use it for classloading
		if(!validateJarFile(file)){
			return;
		}
		
		JarFile[] result3 = new JarFile[jarFiles.length + 1];
		for ( i = 0; i < jarFiles.length; i++) {
			result3[i] = jarFiles[i];
		}
		result3[jarFiles.length] = jarFile;
		jarFiles = result3;
		
		//Add the file to the list
		File[] result4 = new File[files.length + 1];
		for (i = 0; i < files.length; i++) {
			result4[i] = files[i];
		}
		result4[files.length] = file;
		files = result4;
		
		//Load manifest
		Manifest manifest = jarFile.getManifest();
		if(manifest != null){
			Iterator<Extension> extendions = Extension.getAvailable(manifest).iterator();
			while(extendions.hasNext()){
				available.add(extendions.next());
			}
			extendions = Extension.getRequired(manifest).iterator();
			while(extendions.hasNext()){
				required.add(extendions.next());
			}
		}
		
	}

	/**
	 * Check the specified JAR file, and return <code>true</code> if it does not
	 * contain any of thie trigger classes.
	 * 
	 * @param jarFile
	 * @return
	 * @throws IOException 
	 */
	private boolean validateJarFile(File file) throws IOException {
		if(triggers == null){
			return true;
		}
		JarFile jarFile = new JarFile(file);
		for (int i = 0; i < triggers.length; i++) {
			Class<?> clazz = null;
			try{
				if(parent != null){
					clazz = parent.loadClass(triggers[i]);
				}else{
					clazz = Class.forName(triggers[i]);
				}
			}catch(Throwable t){
				clazz = null;
			}
			if(clazz == null){
				continue;
			}
			String name = triggers[i].replace('.', '/') + ".class";
			if(debug >= 2){
				log("Checking for" + name);
			}
			JarEntry entry = jarFile.getJarEntry(name);
			if(entry != null){
				log("validateJarFile(" + file + ") - jar not loaded. See Servlet Spec 2.3 section 9.7.2. Offending class:" + name);
				jarFile.close();
				return false;
			}
		}
		jarFile.close();
		return true;
	}

	public void addPermission(URL url) {
		this.addPermission(url.toString());
	}

	public void addPermission(Permission permission) {
		if(securityManager != null && permission != null){
			permissionList.add(permission);
		}
	}

	/**
	 * If there is a Java SecurityManager create a read FilePermission
	 * or JndiPermission for the file directory path.
	 * 
	 * @param path
	 */
	public void addPermission(String path) {
		if(securityManager != null){
			Permission permission = null;
			if(path.startsWith("jndi:") || path.startsWith("jar:jndi:")){
				//FIXME -- JndiPermission class not found
				;
			}else{
				permission = new FilePermission(path + "-", "read");
				addPermission(permission);
			}
		}
		
	}

	/**
	 * Return a list of "optional packages" (formerly "standard extensions") that have been 
	 * decleared to be available in the repositories associated with this class loader,
	 * plus any parent class loader implemented with the same class.
	 * 
	 * @return
	 */
	@SuppressWarnings("resource")
	public Extension[] findAvailable() {

		//Initialized the results with our local available extensions
		ArrayList<Extension> results = new ArrayList<>();
		Iterator<Extension> available = this.available.iterator();
		while(available.hasNext()){
			results.add(available.next());
		}
		
		//Trace our parentage tree and declared extensions when possible
		ClassLoader loader = this;
		while(true){
			loader = loader.getParent();
			if(loader == null){
				break;
			}
			if(!(loader instanceof WebappClassLoader)){
				continue;
			}
			Extension[] extensions = ((WebappClassLoader) loader).findAvailable();
			for (int i = 0; i < extensions.length; i++) {
				results.add(extensions[i]);
			}
		}
		//Return the results as an array
		return results.toArray(new Extension[results.size()]);
	}

	@SuppressWarnings("resource")
	public Extension[] findRequired() {
		//Initialized the results with our local available extensions
		ArrayList<Extension> results = new ArrayList<>();
		Iterator<Extension> required = this.required.iterator();
		while(required.hasNext()){
			results.add(required.next());
		}
		
		//Trace our parentage tree and declared extensions when possible
		ClassLoader loader = this;
		while(true){
			loader = loader.getParent();
			if(loader == null){
				break;
			}
			if(!(loader instanceof WebappClassLoader)){
				continue;
			}
			Extension[] extensions = ((WebappClassLoader) loader).findRequired();
			for (int i = 0; i < extensions.length; i++) {
				results.add(extensions[i]);
			}
		}
		//Return the results as an array
		return results.toArray(new Extension[results.size()]);
	}
	
	/**
	 * Returns true if the specified package name is sealed according to the given manifest
	 * 
	 * @param name
	 * @param manifest
	 * @return
	 */
	protected boolean isPackageSealed(String name, Manifest manifest){
		
		String path = name + "/";
		Attributes attr = manifest.getAttributes(path);
		String sealed = null;
		if(attr != null){
			sealed = attr.getValue(Name.SEALED);
		}
		if(sealed  == null){
			if((attr = manifest.getMainAttributes()) != null){
				sealed = attr.getValue(Name.SEALED);
			}
		}
		return "true".equalsIgnoreCase(sealed);
	}
	
	//-------------------------------------Getter and Setter ---------------------------
	
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
	
	@Override
	public String[] findRepositories() {
		return this.repositories;
	}
	public String getJarPath() {
		return jarPath;
	}
	
	public void setJarPath(String jarPath) {
		this.jarPath = jarPath;
	}
	
	// ----------------------------------------------------------------------------------
	/**
	 * Find specifieed resource in local repositories
	 * 
	 * @param name
	 * @param path
	 * @return 		the loaded resource, or null if the resource isn't found 
	 */
	protected ResourceEntry findResourceInternal(String name, String path){
		if(!started){
			log("Lifecycle error: CL stopped");
			return null;
		}
		
		if(name == null || path == null){
			return null;
		}
		
		ResourceEntry entry = resourceEntries.get(name);
		if(entry != null){
			return entry;
		}
		
		int contentLength = -1;
		InputStream binaryStream = null;
		
		int jarFilesLength = jarFiles.length;
		int repositoriesLength = repositories.length;
		
		int i;
		Resource resource = null;
		for (i = 0; (entry == null && i < repositoriesLength); i++) {
			try {
				String fullPath = repositories[i] + path;
				Object lookupResult  = resources.lookup(fullPath);
				if(lookupResult instanceof Resource){
					resource = (Resource) lookupResult;
				}
				
				//Note: not getting an exption has means the resource was found
				
				entry = new ResourceEntry();
				
				try {
					entry.source = getURL(new File(files[i], path));
					entry.codeBase = entry.source;
				} catch (MalformedURLException e) {
					return null;
				}
				ResourceAttributes attributes = (ResourceAttributes) resources.getAttributes(fullPath);
				contentLength = (int) attributes.getContentLength();
				entry.lastModified = attributes.getLastModified();
				
				if(resource != null){
					try {
						binaryStream = resource.streamContent();
					} catch (IOException e) {
						return null;
					}
					
					
					//Register the full path for modification checking 
					//Note: Only syncing on a 'constant' object is needed
					synchronized (allPermission) {
						
						int j;
						long[] result2 = new long[lastModifiedDates.length + 1];
						for (j = 0; j < lastModifiedDates.length; j++) {
							result2[j] = lastModifiedDates[j];
						}
						result2[lastModifiedDates.length] = entry.lastModified;
						lastModifiedDates = result2;
						
						String[] result	= new String[paths.length + 1];
						for (j = 0; j < paths.length; j++) {
							result[j] = paths[j];
						}
						result[paths.length] = fullPath;
						paths = result;
					}
				}
			} catch (NamingException e) {
				;
			}
		}
		
		if(entry == null && notFoundResources.containsKey(name)){
			return null;
		}
		
		JarEntry jarEntry = null;
		
		for (i = 0; i < jarFilesLength; i++) {
			jarEntry = jarFiles[i].getJarEntry(path);
			if(jarEntry != null){
				entry = new ResourceEntry();
				try {
					entry.codeBase = this.getURL(jarRealFiles[i]);
					String jarFakeUrl = entry.codeBase.toString();
					jarFakeUrl = "jar:" + jarFakeUrl + "!/" + path;
					entry.source = new URL(jarFakeUrl);
				} catch (MalformedURLException e) {
					return null;
				}
				contentLength = (int) jarEntry.getSize();
				
				try {
					entry.manifest = jarFiles[i].getManifest();
					binaryStream = jarFiles[i].getInputStream(jarEntry);
				} catch (IOException e) {
					return null;
				}
			}
		}
		
		if(entry == null){
			synchronized (notFoundResources) {
				notFoundResources.put(name, name);
			}
			return null;
		}
		if(binaryStream != null){
			byte[] binaryContent = new byte[contentLength];
			
			try {
				int pos = 0;
				while(true){
					int n = binaryStream.read(binaryContent, pos, binaryContent.length - pos);
					if(n <= 0){
						break;
					}
					pos += n;
				}
				binaryStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			
			entry.binaryContext = binaryContent;
			
			//The certificates are only available after the JarEntry 
			//associated input stream has been full read
			if(jarEntry != null){
				entry.certificates = jarEntry.getCertificates();
			}
		}
		
		//Add the entry in the local resource repository
		synchronized (resourceEntries) {
			//Ensures that all the threads which may be in a  race to load a particular
			//class all end up with the same ResourceEntry instance
			ResourceEntry entry2 = resourceEntries.get(name);
			if(entry2 == null){
				resourceEntries.put(name, entry2);
			}else{
				entry = entry2;
			}
		}
		return entry;
	}
	
	/**
	 * Get URL
	 * 
	 * @param file
	 * @return
	 * @throws MalformedURLException
	 */
	@SuppressWarnings("deprecation")
	private URL getURL(File file) throws MalformedURLException {
		File realFile = file;
		try {
			realFile = realFile.getCanonicalFile();
		} catch (IOException e) {
			;			//Ignore
		}
		//return new URL("file:" + realFile.getPath());
		return realFile.toURL();
	}

	protected class PrivilegedFindResource implements PrivilegedAction<ResourceEntry>{

		private String name;
		
		private String path;
		
		public PrivilegedFindResource(String name, String path) {
			this.name = name;
			this.path = path;
		}


		@Override
		public ResourceEntry run() {
			return findResourceInternal(name, path);
		}
		
	}
	
	/**
     * Render a String representation of this object.
     */
    public String toString() {

        StringBuffer sb = new StringBuffer("WebappClassLoader\r\n");
        sb.append("  available:\r\n");
        Iterator<Extension> available = this.available.iterator();
        while (available.hasNext()) {
            sb.append("    ");
            sb.append(available.next().toString());
            sb.append("\r\n");
        }
        sb.append("  delegate: ");
        sb.append(delegate);
        sb.append("\r\n");
        sb.append("  repositories:\r\n");
        for (int i = 0; i < repositories.length; i++) {
            sb.append("    ");
            sb.append(repositories[i]);
            sb.append("\r\n");
        }
        sb.append("  required:\r\n");
        Iterator<Extension> required = this.required.iterator();
        while (required.hasNext()) {
            sb.append("    ");
            sb.append(required.next().toString());
            sb.append("\r\n");
        }
        if (this.parent != null) {
            sb.append("----------> Parent Classloader:\r\n");
            sb.append(this.parent.toString());
            sb.append("\r\n");
        }
        return (sb.toString());

    }

	private void log(String message){
		System.out.println("WebappClassLoader: " + message);
	}
	
	private void log(String message, Throwable t){
		System.out.println("WebappClassLoader: " + message);
		t.printStackTrace(System.err);
	}
}
