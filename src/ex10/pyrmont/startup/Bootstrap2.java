package ex10.pyrmont.startup;


import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Realm;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.logger.SystemOutLogger;
import org.apache.naming.resources.ProxyDirContext;

import ex10.pyrmont.core.SimpleContextConfig;
import ex10.pyrmont.core.SimpleWrapper;
import ex10.pyrmont.realm.SimpleUserDatabaseRealm;

public class Bootstrap2 {

	public static void main(String[] args) {

		System.setProperty("catalina.base",System.getProperty("user.dir"));

		Connector connector = new HttpConnector();
		
//		Wrapper wrapper1 = new SimpleWrapper();
//		wrapper1.setName("Primitive");
//		wrapper1.setServletClass("PrimitiveServlet");
		Wrapper wrapper2 = new SimpleWrapper();
		wrapper2.setName("Modern");
		wrapper2.setServletClass("ModernServlet");
		
		
		Context context = new StandardContext();
		
		context.setPath("/myApp");
		context.setDocBase("myApp");
		
//		context.addChild(wrapper1);
		context.addChild(wrapper2);
		
		
//		context.addServletMapping("/Primitive", "Primitive");
		context.addServletMapping("/Modern", "Modern");
		
		LifecycleListener listener = new SimpleContextConfig();
		((Lifecycle)context).addLifecycleListener(listener);
		
		//here is our our loader
		Loader loader = new WebappLoader();
		//associate the loader with the Context
		context.setLoader(loader);
		
		// add constraint
		SecurityCollection securityCollection = new SecurityCollection();
		securityCollection.addPattern("/");
		securityCollection.addMethod("GET");
		
		SecurityConstraint constraint = new SecurityConstraint();
		constraint.addCollection(securityCollection);
		constraint.addAuthRole("manager");
		LoginConfig loginConfig = new LoginConfig();
		loginConfig.setRealmName("Simple User Database Realm");
		
//		//
		Realm realm = new SimpleUserDatabaseRealm();
	    ((SimpleUserDatabaseRealm) realm).createDatabase("conf/tomcat-users.xml");
		context.setRealm(realm);
		context.addConstraint(constraint);
		context.setLoginConfig(loginConfig);
		
		//-----------add Logger ------------
		SystemOutLogger logger = new SystemOutLogger();
		context.setLogger(logger);
		
		//--------------------
		
		connector.setContainer(context);
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
