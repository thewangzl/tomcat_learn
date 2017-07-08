package ex02.pyrmont;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer1 {

	private static final String SHUTDOWN = "/SHUTDOWN";
	
	private boolean shutdown = false;
	
	public static void main(String[] args) {
		HttpServer1 server = new HttpServer1();
		server.await();
	}
	
	private void await(){
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(8080, 1, InetAddress.getByName("127.0.0.1"));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		while(!this.shutdown){
			try {
				Socket socket = serverSocket.accept();
				InputStream input = socket.getInputStream();
				OutputStream output = socket.getOutputStream();
				//create Request Object and parse
				Request request = new Request(input);
				request.parse();
				//create Response object
				Response response = new Response(output);
				response.setRequest(request);
				//check if  this is a request for servlet or a static resource
				//a request for a servlet begins with "/servlet/"
				if(request.getUri().startsWith("/servlet/")){
					ServletProcessor1 processor = new ServletProcessor1();
					processor.process(request, response);
				}else{
					StaticResourceProcessor processor = new StaticResourceProcessor();
					processor.process(request, response);
				}
				//close the socket
				socket.close();
				
				//check if the previous Uri is a shutdown command
				shutdown = SHUTDOWN.equals(request.getUri());
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
		}
	}

}
