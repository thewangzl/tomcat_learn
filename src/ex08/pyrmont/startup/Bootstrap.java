package ex08.pyrmont.startup;

import java.util.Iterator;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Mapper;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.logger.FileLogger;
import org.apache.naming.resources.ProxyDirContext;

import ex08.pyrmont.core.SimpleContextConfig;
import ex08.pyrmont.core.SimpleWrapper;

public class Bootstrap {

	public static void main(String[] args) {

		System.setProperty("catalina.base",System.getProperty("user.dir"));

		Connector connector = new HttpConnector();
		
		Wrapper wrapper1 = new SimpleWrapper();
		wrapper1.setName("Primitive");
		wrapper1.setServletClass("PrimitiveServlet");
		Wrapper wrapper2 = new SimpleWrapper();
		wrapper2.setName("Modern");
		wrapper2.setServletClass("ModernServlet");
		
		
		Context context = new StandardContext();
		
		context.setPath("/myApp");
		context.setDocBase("myApp");
		
		context.addChild(wrapper1);
		context.addChild(wrapper2);
		
		context.addServletMapping("/Primitive", "Primitive");
		context.addServletMapping("/Modern", "Modern");
		
		LifecycleListener listener = new SimpleContextConfig();
		((Lifecycle)context).addLifecycleListener(listener);
		
		//here is our our loader
		Loader loader = new WebappLoader();
		//associate the loader with the Context
		context.setLoader(loader);
		
		//-----------add Logger ------------
		FileLogger logger = new FileLogger();
		logger.setPrefix("FileLog_");
		logger.setSuffix(".txt");
		logger.setTimestamp(true);
		logger.setDirectoty("webroot");
		context.setLogger(logger);
		
		//--------------------
		
		connector.setContainer(context);
		System.out.println(Bootstrap.class.getClassLoader());
		try {
			connector.initialize();
			
			((Lifecycle) connector).start();
			((Lifecycle)context).start();
			
			//now we want to know some details about WebappLoader
			WebappClassLoader classLoader = (WebappClassLoader) loader.getClassLoader();
			System.out.println("Resources's docBase: "+ ((ProxyDirContext) classLoader.getResources()).getDocBase());
			String[] repositories = classLoader.findRepositories();
			for (String repository : repositories) {
				System.out.println("repository: " + repository);
			}
			//
			System.in.read();
			
			((Lifecycle) context).stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	

	

	}

}
