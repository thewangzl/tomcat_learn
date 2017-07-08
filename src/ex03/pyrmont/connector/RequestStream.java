package ex03.pyrmont.connector;

import java.io.IOException;

import javax.servlet.ServletInputStream;

import ex03.pyrmont.connector.http.HttpRequest;

public class RequestStream extends ServletInputStream {

	private HttpRequest request;
	
	public RequestStream(HttpRequest request) {
		this.request = request;
	}

	@Override
	public int read() throws IOException {

		return 0;
	}

}
