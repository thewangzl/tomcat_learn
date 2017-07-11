package org.apache.catalina.core;


import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.valves.ValveBase;

/**
 * 
 * @author thewangzl
 *
 */
public class StandardContextValve extends ValveBase{

	@Override
	public void invoke(Request request, Response response, ValveContext context) throws IOException, ServletException {

		
	}

	
}
