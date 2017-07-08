package ex05.pyrmont.core;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
		System.out.println("BasicValve invoke");
		System.out.println("--------------------------");
		SimpleWrapper wrapper = (SimpleWrapper) getContainer();
		ServletRequest sreq = request.getRequest();
		ServletResponse sresp = response.getResponse();
		
		Servlet servlet = null;
		HttpServletRequest hreq = null;
		if(sreq instanceof HttpServletRequest){
			hreq = (HttpServletRequest) sreq;
		}
		HttpServletResponse hresp = null;
		if(sresp instanceof HttpServletResponse){
			hresp = (HttpServletResponse) sresp;
		}
		
		//Allocate a servlet instance to process this request
		try{
			servlet = wrapper.allocate();
			if(hresp != null && hreq != null){
				servlet.service(hreq, hresp);
			}else{
				servlet.service(sreq, sresp);
			}
		}catch(ServletException e){
			e.printStackTrace();
		}
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
