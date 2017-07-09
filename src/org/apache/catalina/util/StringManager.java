package org.apache.catalina.util;

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * An internationalization / localization helper class which reduce the bother of handling 
 * ResourceBundles and takes care of the common cases of message formating which otherwise
 * require the creation of object arrrays and such.
 * 
 * @author thewangzl
 *
 */
public class StringManager {

	private ResourceBundle bundle;
	
	private Locale locale;
	
	private  StringManager(String packageName) {
		String bundleName = packageName + ".LocalStrings";
		ResourceBundle tempBundle = null;
		try{
			
			tempBundle = ResourceBundle.getBundle(bundleName,Locale.getDefault());
			
		}catch(MissingResourceException e){
			//
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if(cl != null){
				try{
					tempBundle = ResourceBundle.getBundle(bundleName, Locale.getDefault(), cl);
				}catch(MissingResourceException mre){
					;			//Ignore
				}
			}
		}
		//Get a actual locale,which may be different from the requested one
		if(tempBundle != null){
			locale = tempBundle.getLocale();
		}else{
			locale = null;
		}
		bundle = tempBundle;
	}
	
	/**
	 * Get a string from the underlying resource bundle
	 * @param key
	 * @return
	 */
	public String getString(String key){
		if(key == null){
			String msg = "key  may not have a null value";
			throw new NullPointerException(msg);
		}
		String str = null;
		try{
			str = bundle.getString(key);
		}catch(MissingResourceException e){
			str = "";
		}
		return str;
	}
	
	/**
	 * Get a string from the underlying resource bundle and format it 
	 * with the given set of arguments.
	 * 
	 * @param key
	 * @param args
	 * @return
	 */
	public String getString(String key, Object... args){
		String value = getString(key);
		if(value == null){
			return value;
		}
		
		MessageFormat format = new MessageFormat(value);
		format.setLocale(locale);
		return format.format(args, new StringBuffer(), null).toString();
	}
	
	//
	
	private static final Hashtable<String, StringManager> managers = new Hashtable<>();
	
	/**
	 * Get the StringManager for the particular package.If a manager for a package already 
	 * exists, it will be reused, else a new StringManager will be created and returned.
	 * 
	 * @param packageName
	 * @return
	 */
	public static final synchronized StringManager getManager(String packageName){
		StringManager manager = managers.get(packageName);
		if(manager == null){
			manager = new StringManager(packageName);
			managers.put(packageName, manager);
		}
		return manager;
	}
	
	public static final StringManager getManager(Class<?> clazz){
		
		return getManager(clazz.getPackage().getName());
	}
	
	
	
	
	
	
	
	
	
}
