package org.apache.catalina.startup;

import java.lang.reflect.Method;

import org.apache.catalina.Container;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.xml.sax.Attributes;

public class CopyParentClassLoaderRule extends Rule {

	// ----------------------------------------------------------- Constructors

	/**
	 * Construct a new instance of this Rule.
	 *
	 * @param digester
	 *            Digester we are associated with
	 */
	public CopyParentClassLoaderRule(Digester digester) {

		super(digester);

	}

	// --------------------------------------------------------- Public Methods

	/**
	 * Handle the beginning of an XML element.
	 *
	 * @param attributes
	 *            The attributes of this element
	 *
	 * @exception Exception
	 *                if a processing error occurs
	 */
	public void begin(Attributes attributes) throws Exception {

		if (digester.getDebug() >= 1)
			digester.log("Copying parent class loader");
		Container child = (Container) digester.peek(0);
		Object parent = digester.peek(1);
		Method method = parent.getClass().getMethod("getParentClassLoader", new Class[0]);
		ClassLoader classLoader = (ClassLoader) method.invoke(parent, new Object[0]);
		child.setParentClassLoader(classLoader);

	}

}
