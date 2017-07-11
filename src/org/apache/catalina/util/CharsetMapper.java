package org.apache.catalina.util;

import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

public class CharsetMapper {

	
	/**
	 * Default properties resource name.
	 */
	public static final String DEFAULT_RESOURCE = "/org/apache/catalina/util/CharsetMapperDefault.properties";
	
	/**
	 * The mapping properties that have been initialized from the specified or default properties resource 
	 */
	private Properties map = new Properties();
	
	public CharsetMapper() {
		this(DEFAULT_RESOURCE);
	}
	
	/**
	 * 
	 * @param name
	 */
	public CharsetMapper(String name) {
		try {
			InputStream stream = this.getClass().getResourceAsStream(name);
			map.load(stream);
			stream.close();
		} catch (Throwable e) {
			throw new IllegalArgumentException(e.toString());
		}
	}
	
	public String getCharset(Locale locale){
		String charset = null;
		
		//First, try a full name match (language and country)
		charset = map.getProperty(locale.toString());
		if(charset != null){
			return charset;
		}
		//Second, try to match just the language
		charset = map.getProperty(locale.getLanguage());
		return charset;
	}
}
