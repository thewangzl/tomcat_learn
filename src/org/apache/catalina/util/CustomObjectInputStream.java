package org.apache.catalina.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class CustomObjectInputStream extends ObjectInputStream {

	
	private ClassLoader classLoader;
	
	
	public CustomObjectInputStream(InputStream stream, ClassLoader classLoader) throws IOException {
		super(stream);
		this.classLoader = classLoader;
	}
	
	/**
	 * 
	 */
	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		return classLoader.loadClass(desc.getName());
	}

}
