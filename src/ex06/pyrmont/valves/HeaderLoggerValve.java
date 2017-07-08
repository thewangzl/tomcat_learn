package ex06.pyrmont.valves;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;

public class HeaderLoggerValve implements Valve, Contained, Lifecycle {

	private Container container;
	

	@Override
	public void start() throws LifecycleException {
		System.out.println("Starting HeaderLoggerValve ");
	}

	@Override
	public void stop() throws LifecycleException {
		System.out.println("Stopping HeaderLoggerValve");
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

	@Override
	public void invoke(Request request, Response response, ValveContext context) throws IOException, ServletException {

		context.invokeNext(request, response);
		
		System.out.println("Header Logger Pipeline");
		
		if(request.getRequest() instanceof HttpServletRequest){
			HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
			Enumeration headerNames = hreq.getHeaderNames();
			while(headerNames.hasMoreElements()){
				String headerName = headerNames.nextElement().toString();
				String headerValue = hreq.getHeader(headerName);
				System.out.println(headerName + ": " + headerValue);
			}
		}else{
			System.err.println("Not a HTTP request");
		}
		
		System.out.println("----------------------------------");
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

}
