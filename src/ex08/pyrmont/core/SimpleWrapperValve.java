package ex08.pyrmont.core;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;

public class SimpleWrapperValve implements Valve, Contained {

	protected Container container;
	
	@Override
	public void invoke(Request request, Response response, ValveContext context) throws IOException, ServletException {


	}
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
		return null;
	}


}
