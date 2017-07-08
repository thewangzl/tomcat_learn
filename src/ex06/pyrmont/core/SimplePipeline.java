package ex06.pyrmont.core;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;

public class SimplePipeline implements Pipeline,Lifecycle {

	protected Valve basic;
	
	protected Container container;
	
	protected Valve[] valves = new Valve[0];
	
	private boolean started;
	
	
	public SimplePipeline(Container container) {
		setContainer(container);
	}
	
	public void setContainer(Container container) {
		this.container = container;
	}

	@Override
	public Valve getBasic() {
		return this.basic;
	}

	@Override
	public void setBasic(Valve basic) {
		this.basic = basic;
		((Contained)basic).setContainer(container);
	}

	@Override
	public void addValve(Valve valve) {
		if(valve instanceof Contained){
			((Contained) valve).setContainer(container);
		}
		synchronized (valves) {
			Valve[] results = new Valve[valves.length + 1];
			System.arraycopy(valves, 0, results, 0, valves.length);
			results[valves.length] = valve;
			valves = results;
		}
	}

	@Override
	public Valve[] getValves() {

		return valves;
	}

	@Override
	public void removeValve(Valve valve) {

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
		System.out.println("Starting simple pipeline");
		
		if(started){
			throw new LifecycleException("pipeline has already started");
		}
		
		started = true;
		if(basic != null && basic instanceof Lifecycle){
			((Lifecycle) basic).start();
		}
		
		if(valves != null){
			for (int i = 0; i < valves.length; i++) {
				if(valves[i] instanceof Lifecycle){
					((Lifecycle) valves[i]).start();
				}
			}
		}
		
	}

	@Override
	public void stop() throws LifecycleException {
		System.out.println("Stoping simple pipeline");

		if(!started){
			throw new LifecycleException("pipeline not started");
		}
		
		started = false;
		
		if(basic != null && basic instanceof Lifecycle){
			((Lifecycle) basic).stop();
		}
		
		if(valves != null){
			for (int i = 0; i < valves.length; i++) {
				if(valves[i] instanceof Lifecycle){
					((Lifecycle) valves[i]).stop();
				}
			}
		}
	}

	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {

		(new SimplePipelineValveContext()).invokeNext(request, response);
	}
	
	
	protected class SimplePipelineValveContext implements ValveContext{

		protected int stage = 0;
		
		@Override
		public String getInfo() {

			return null;
		}

		@Override
		public void invokeNext(Request request, Response response) throws IOException, ServletException {
			
			int subscript = stage;
			stage += 1;
			
			//Invoke the request Valve for the current request thread
			if(subscript < valves.length){
				valves[subscript].invoke(request, response, this);
			}else if(subscript == valves.length && basic != null){
				basic.invoke(request, response, this);
			}else{
				throw new ServletException("No basic Valve");
			}
		}
	}


}
