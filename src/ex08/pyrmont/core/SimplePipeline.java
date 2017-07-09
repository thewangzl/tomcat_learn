package ex08.pyrmont.core;

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

public class SimplePipeline implements Pipeline, Lifecycle {

	
	protected Container container;
	
	protected Valve basic;
	
	protected Valve[] valves;
	
	public SimplePipeline(Container container) {
		setContainer(container);
	}
	@Override
	public void start() throws LifecycleException {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() throws LifecycleException {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {
		// TODO Auto-generated method stub

	}
	
	public Container getContainer() {
		return container;
	}
	
	public void setContainer(Container container) {
		this.container = container;
	}

	@Override
	public Valve getBasic() {

		return basic;
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
		synchronized (valves) {
			//
		}
	}

	
	@Override
	public void addLifecycleListener(LifecycleListener listener) {


	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {

		return new  LifecycleListener[0];
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {

	}

}
