package ex03.pyrmont.startup;

import ex03.pyrmont.connector.http.HttpConnector;

public class Bootstrap {

	public static void main(String[] args) {

		HttpConnector connector = new HttpConnector();
		connector.start();
		System.out.println("server started");
	}

}
