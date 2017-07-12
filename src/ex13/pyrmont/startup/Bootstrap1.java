package ex13.pyrmont.startup;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Logger;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.logger.SystemOutLogger;

import ex13.pyrmont.core.SimpleContextConfig;

public class Bootstrap1 {

	public static void main(String[] args) {

		System.setProperty("catalina.base", System.getProperty("user.dir"));

		Connector connector = new HttpConnector();

		Wrapper wrapper1 = new StandardWrapper();
		wrapper1.setName("Primitive");
		wrapper1.setServletClass("PrimitiveServlet");
		Wrapper wrapper2 = new StandardWrapper();
		wrapper2.setName("Modern");
		wrapper2.setServletClass("ModernServlet");

		Context context = new StandardContext();
		context.setDocBase("app1");
		context.setPath("/app1");
		LifecycleListener listener = new SimpleContextConfig();
		((Lifecycle) context).addLifecycleListener(listener);

		context.addChild(wrapper1);
		context.addChild(wrapper2);
		
		context.addServletMapping("/Primitive", "Primitive");
		context.addServletMapping("/Modern", "Modern");
		
		Host host = new StandardHost();
		host.addChild(context);
		host.setName("localhost");
		host.setAppBase("webapps");

		Loader loader = new WebappLoader();
		host.setLoader(loader);
		

		Logger logger = new SystemOutLogger();
		logger.setVerbosity(3);
		host.setLogger(logger);

		((ContainerBase) host).setDebug(4);
		((ContainerBase) context).setDebug(4);
		((ContainerBase) wrapper1).setDebug(4);
		((ContainerBase) wrapper2).setDebug(4);

		connector.setContainer(host);

		try {
			connector.initialize();
			((Lifecycle) connector).start();
			((Lifecycle) host).start();

			System.in.read();
			((Lifecycle) host).stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
