package org.apache.catalina.valves;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;
import org.apache.catalina.util.StringManager;

/**
 * Convenience base class for implementations of the <b>Valve</b> Interface. A subclass 
 * <strong>MUST</strong> implement an <code>invoke()</code> method to provide the required 
 * funtionality, and <strong>MAY</strong> implement the <code>Lifecycle</code> interface 
 * to provide configuration management and lifecycle support.
 * 
 * @author thewangzl
 *
 */
public abstract class ValveBase implements Contained, Valve {

	protected Container container;
	
	protected int debug = 0;
	
	protected static final String info = "ora.apache.catalina.valves.ValveBase/1.0";
	
	protected static final StringManager sm = StringManager.getManager(Constants.Package);
	
	
	@Override
	public Container getContainer() {

		return this.container;
	}

	@Override
	public void setContainer(Container container) {

		this.container = container;
	}

	@Override
	public String getInfo() {
		return info;
	}

	public int getDebug() {
		return debug;
	}
	
	public void setDebug(int debug) {
		this.debug = debug;
	}
	
	/**
	 * 
	 */
	@Override
	public abstract void invoke(Request request, Response response, ValveContext context) throws IOException, ServletException;

}
