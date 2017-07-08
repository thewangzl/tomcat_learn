package ex04.pyrmont.startup;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.http.HttpConnector;

import ex04.pyrmont.core.SimpleContainer;

public class Bootstrap {

	public static void main(String[] args) {

		HttpConnector connector = new HttpConnector();
		SimpleContainer container = new SimpleContainer();
		connector.setContainer(container);
		try {
			connector.initialize();
			connector.start();
		} catch (LifecycleException e) {
			e.printStackTrace();
		}
	}

}
