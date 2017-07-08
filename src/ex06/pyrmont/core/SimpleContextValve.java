package ex06.pyrmont.core;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;
import org.apache.catalina.Wrapper;

/**
 * 
 * @author thewangzl
 *
 */
public class SimpleContextValve implements Contained, Valve, Lifecycle {

	protected Container container;
	
	@Override
	public void invoke(Request request, Response response, ValveContext valveContext) throws IOException, ServletException {
		
		System.out.println("SimpleContextValve invoke");
		System.out.println("-----------------------");
		//Validate the request and response object types
		if(!(request.getRequest() instanceof HttpServletRequest) || !(response.getResponse() instanceof HttpServletResponse))
		{
			return;		//Note - Not much else we can do generically
		}
		
		//Disallow any direct access resources under WEB-INF or META-INF
		HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
		String requestURI = hreq.getRequestURI();
//		String contextPath = hreq.getContextPath();
//		String requestURI = ((HttpRequest)request).getDecodedRequestURI();
//		String relativeURI = requestURI.substring(contextPath.length()).trim();
		
		Context context = (Context) getContainer();
		//Select the wrapper to be used for this request
		Wrapper wrapper = null;
		try{
			wrapper = (Wrapper) context.map(request, true);
		}catch(IllegalArgumentException e){
			this.badRequest(requestURI,(HttpServletResponse) response.getResponse());
			return;
		}
		if(wrapper == null){
			this.notFound(requestURI, (HttpServletResponse)response.getResponse());
			return;
		}
		
		//Ask this Wrapper to process this Request
		response.setContext(context);
		wrapper.invoke(request, response);
	}
	
	private void badRequest(String requestURI, HttpServletResponse response){
		try {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, requestURI);
		}catch(IllegalStateException e){
			;
		} catch (IOException e) {
			;
		}
	}
	
	private void notFound(String requestURI, HttpServletResponse response){
		try {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, requestURI);
		}catch(IllegalStateException e){
			;
		} catch (IOException e) {
			;
		}
	}

	@Override
	public Container getContainer() {

		return container;
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
	public void addLifecycleListener(LifecycleListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() throws LifecycleException {

		System.out.println("Starting SimpleContextValve ");
		
	}

	@Override
	public void stop() throws LifecycleException {

		System.out.println("Stopping SimpleContextValve ");
	}


}
