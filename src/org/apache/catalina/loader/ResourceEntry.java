package org.apache.catalina.loader;

import java.net.URL;
import java.security.cert.Certificate;
import java.util.jar.Manifest;

/**
 * Resource entry
 * 
 * @author thewangzl
 *
 */
public class ResourceEntry {

	/**
	 * The "last modified" time of the origin file at the time this class was loaded,
	 * in millseconds since the epoch
	 */
	public long lastModified = -1;
	
	/**
	 * Binary content of the resource
	 */
	public byte[] binaryContext;
	
	/**
	 * Loaded class
	 */
	public Class<?> loadedClass;
	
	/**
	 * URL source from where the object was loaded.
	 */
	public URL source;
	
	/**
	 * URL of the codebase from where the object was loaded.
	 */
	public URL codeBase;
	
	/**
	 * Manifest (if the resource was loaded from a JAR)
	 */
	public Manifest manifest;
	
	/**
	 * Certificate (if the resource was loaded from a JAR )
	 */
	public Certificate[] certificates;
	
}
