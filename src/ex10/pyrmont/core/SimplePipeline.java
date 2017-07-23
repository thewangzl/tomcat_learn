package ex10.pyrmont.core;

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

public class SimplePipeline implements Pipeline, Lifecycle {

	protected Container container;

	protected Valve basic;

	protected Valve[] valves = new Valve[0];

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
		(new StandardPipelineValveContext()).invokeNext(request, response);

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

		((Contained) basic).setContainer(container);
	}

	@Override
	public void addValve(Valve valve) {
		if (valve instanceof Contained) {
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

		return new LifecycleListener[0];
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {

	}

	// this class is copied from org.apache.catalina.core.StandardPipeline
	// class's
	// StandardPipelineValveContext inner class.
	protected class StandardPipelineValveContext implements ValveContext {
		protected int stage = 0;

		public String getInfo() {
			return null;
		}

		public void invokeNext(Request request, Response response) throws IOException, ServletException {
			int subscript = stage;
			stage = stage + 1;
			// Invoke the requested Valve for the current request thread
			if (subscript < valves.length) {
				valves[subscript].invoke(request, response, this);
			} else if ((subscript == valves.length) && (basic != null)) {
				basic.invoke(request, response, this);
			} else {
				throw new ServletException("No valve");
			}
		}
	}
}
