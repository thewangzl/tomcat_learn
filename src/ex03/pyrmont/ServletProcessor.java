package ex03.pyrmont;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import ex03.pyrmont.connector.http.Constants;
import ex03.pyrmont.connector.http.HttpRequest;
import ex03.pyrmont.connector.http.HttpRequestFacade;
import ex03.pyrmont.connector.http.HttpResponse;
import ex03.pyrmont.connector.http.HttpResponseFacade;

public class ServletProcessor {

	public void process(HttpRequest request, HttpResponse response){

		String uri = request.getRequestURI();
		String servletName = uri.substring(uri.lastIndexOf("/") + 1);
		URLClassLoader loader = null;
		try {
			// create a URLClassLoader
			URL[] urls = new URL[1];
			URLStreamHandler streamHandler = null;
			File classPath = new File(Constants.WEB_ROOT);
			//createClassLoader method in 
			//org.apache.catalina.startup.ClassLoaderFactory
			String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator )).toString();
			//the code for forming the URL is taken from the addRepository method in
			//org.apache.catalina.loader.StandardClasLoader
			urls[0] = new URL(null, repository, streamHandler);
			loader = new URLClassLoader(urls);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Class myClass = null;
		try {
			myClass = loader.loadClass(servletName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		Servlet servlet = null;
		HttpRequestFacade requestFacade = new HttpRequestFacade(request);
		HttpResponseFacade responseFacade = new HttpResponseFacade(response);
		try {
			servlet = (Servlet) myClass.newInstance();
			servlet.service((ServletRequest)requestFacade, (ServletResponse)responseFacade);
			response.finishResponse();
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}
}
