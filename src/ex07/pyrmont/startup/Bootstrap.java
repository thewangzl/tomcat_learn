package ex07.pyrmont.startup;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Mapper;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.logger.FileLogger;

import ex06.pyrmont.core.SimpleContextLifecycleListener;
import ex07.pyrmont.core.SimpleContext;
import ex07.pyrmont.core.SimpleContextMapper;
import ex07.pyrmont.core.SimpleLoader;
import ex07.pyrmont.core.SimpleWrapper;

public class Bootstrap {

	public static void main(String[] args) {


		Connector connector = new HttpConnector();
		
		Wrapper wrapper1 = new SimpleWrapper();
		wrapper1.setName("Primitive");
		wrapper1.setServletClass("PrimitiveServlet");
		Wrapper wrapper2 = new SimpleWrapper();
		wrapper2.setName("Modern");
		wrapper2.setServletClass("ModernServlet");
		
		Mapper mapper = new SimpleContextMapper();
		mapper.setProtocol("protocol");
		
		Context context = new SimpleContext();
		
		LifecycleListener listener = new SimpleContextLifecycleListener();
		((Lifecycle)context).addLifecycleListener(listener);
		
		Loader loader = new SimpleLoader();
		context.setLoader(loader);
		
		context.addChild(wrapper1);
		context.addChild(wrapper2);
		
		context.addMapper(mapper);
		context.addServletMapping("/Primitive", "Primitive");
		context.addServletMapping("/Modern", "Modern");
		
		//-----------add Logger ------------
		System.setProperty("catalina.base",System.getProperty("user.dir"));
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
			
			System.in.read();
			
			((Lifecycle) context).stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	

	}

}
