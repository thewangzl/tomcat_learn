package ex06.pyrmont.valves;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;

public class ClientIPLoggerValve implements Valve,Contained,Lifecycle {

	private Container container;

	@Override
	public void invoke(Request request, Response response, ValveContext context) throws IOException, ServletException {

		context.invokeNext(request, response);
		
		System.out.println("Client IP Logger Valve");
		
		System.out.println(request.getRequest().getRemoteHost());

		System.out.println("-----------------------");
	}

	@Override
	public void addLifecycleListener(LifecycleListener listener) {

		
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {

		return null;
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		
	}

	@Override
	public void start() throws LifecycleException {
		System.out.println("Starting ClientIPLoggerValve");
	}

	@Override
	public void stop() throws LifecycleException {

		System.out.println("Stopping ClientIPLoggerValve");
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
