package org.apache.catalina.core;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;

public class StandardPipeline implements Pipeline, Contained, Lifecycle {

	protected Container container;
	
	protected Valve basic;
	
	protected Valve[] valves = new Valve[0];
	
	protected LifecycleSupport lifecycle = new LifecycleSupport(this);
	
	protected static final String info = "org.apache.catalina.core.StandardPipeline/1.0";
	
	protected static final StringManager sm = StringManager.getManager(Constants.Package);
	
	protected boolean started;
	
	public StandardPipeline() {
		this(null);
	}
	
	public StandardPipeline(Container container) {
		setContainer(container);
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
	public Valve getBasic() {

		return basic;
	}

	@Override
	public void setBasic(Valve valve) {
		
		Valve oldValve = this.basic;
		if(oldValve == valve){
			return;
		}
		if(oldValve != null){
			if(started && oldValve instanceof Lifecycle){
				try {
					((Lifecycle) oldValve).stop();
				} catch (LifecycleException e) {
					log("standardPipeline.setBasic: stop",e);
				}
			}
			if(oldValve instanceof Contained){
				((Contained) oldValve).setContainer(null);
			}
		}
		
		//Start the new component if necessary
		if(valve == null){
			return;
		}
		if(valve instanceof Contained){
			((Contained)valve).setContainer(container);
		}
		if(started && valve instanceof Lifecycle){
			try {
				((Lifecycle) valve).start();
			} catch (LifecycleException e) {
				log("standardPipeline.setBasic: start",e);
			}
		}
		this.basic = valve;
	}

	@Override
	public void addValve(Valve valve) {
		//
		if(valve instanceof Contained){
			((Contained) valve).setContainer(container);
		}
		
		//
		if(started && valve instanceof Lifecycle){
			try {
				((Lifecycle) valve).start();
			} catch (LifecycleException e) {
				log("standardPipeline.addValve: start",e);

			}
		}
		
		//
		synchronized (valves) {
			Valve[] results = new Valve[valves.length + 1];
			System.arraycopy(valves, 0, results, 0, valves.length);
			results[valves.length] = valve;
			valves = results;
		}
	}

	@Override
	public Valve[] getValves() {
		if(basic == null){
			return valves;
		}
		synchronized (valves) {
			Valve[] results = new Valve[valves.length + 1];
			System.arraycopy(valves, 0, results, 0, valves.length);
			results[valves.length] = basic;
			return results;
		}
	}

	@Override
	public void removeValve(Valve valve) {
		synchronized (valves) {
			
			//Locate this valve in our list
			int j = -1;
			for (int i = 0; i < valves.length; i++) {
				if(valve == valves[i]){
					j = i;
					break;
				}
			}
			if (j < 0){
				return ;
			}
			
			//
			Valve[] results = new Valve[valves.length - 1];
			int n = 0;
			for (int i = 0; i < valves.length; i++) {
				if(j ==i){
					continue;
				}
				results[n++] = valves[i];
			}
			valves = results;
			
			try{
				if(valve instanceof Contained){
					((Contained) valve).setContainer(null);
				}
			} catch(Throwable e){
				;
			}
		}
		// Stop this valve if necessary
		if(started && valve instanceof Lifecycle){
			try {
				((Lifecycle) valve).stop();
			} catch (LifecycleException e) {
				log("standardPipeline.removeValve: stop",e);
			}
		}
	}

	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {

		(new StandardPipelineValveContext()).invokeNext(request, response);
	}

	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		lifecycle.addLifecycleListener(listener);
		
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {

		return lifecycle.findLifecycleListeners();
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {

		lifecycle.removeLifecycleListener(listener);
	}

	@Override
	public synchronized void start() throws LifecycleException {

		if(started){
			throw new LifecycleException(sm.getString("standardPipeline.alreadyStarted"));
		}
		//
		lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);
		
		started = true;
		
		// Start the Valves in our pipleline (including the basic).
		for (Valve valve : valves) {
			if(valve instanceof Lifecycle){
				((Lifecycle) valve).start();
			}
		}
		if(basic != null && basic instanceof Lifecycle) {
			((Lifecycle) basic).start();
		}
		
		//Notify
		lifecycle.fireLifecycleEvent(START_EVENT, null);
		lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
		
		
	}

	@Override
	public synchronized void stop() throws LifecycleException {
		if(!started){
			throw new LifecycleException(sm.getString("standardPipeline.notStarted"));
		}
		//
		lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);
		
		lifecycle.fireLifecycleEvent(STOP_EVENT, null);
		
		started = false;

        // Stop the Valves in our pipeline (including the basic), if any
        if ((basic != null) && (basic instanceof Lifecycle))
            ((Lifecycle) basic).stop();
        for (int i = 0; i < valves.length; i++) {
            if (valves[i] instanceof Lifecycle)
                ((Lifecycle) valves[i]).stop();
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
		
	}
	
	private void log(String message, Throwable t){
		Logger logger = null;
		if(container != null){
			logger = container.getLogger();
		}
		if(logger != null){
			logger.log("StandardPipeline[" + container.getName() + "]:" + message, t);
		}else{
			System.err.println("StandardPipeline[" + container.getName() + "]:" + message);
			t.printStackTrace(System.err);
		}
	}
	
	// ---------------------------------- StandardPipelineValveContext inner class
	
	protected class StandardPipelineValveContext implements ValveContext{

		protected int stage = 0;
		
		@Override
		public String getInfo() {
			return info;
		}

		@Override
		public void invokeNext(Request request, Response response) throws IOException, ServletException {

			int subscript = stage;
			stage = stage + 1;
			if(subscript < valves.length){
				valves[subscript].invoke(request, response, this);
			}else if(subscript == valves.length && basic != null){
				basic.invoke(request, response, this);
			}else{
				throw new ServletException(sm.getString("standardPipeline.noValve"));
			}
			
		}
		
	}
}
