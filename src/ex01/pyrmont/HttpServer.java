package ex01.pyrmont;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class HttpServer {

	/**
	 * WEB_ROOT is the directory where our HTML and other files reside
	 */
	public static final String WEB_ROOT = System.getProperty("user.dir") + File.separator + "webroot";
	
	//shutdown command
	private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";
	
	private boolean shutdown = false;
	
	public static void main(String[] args) {
		HttpServer server = new HttpServer();
		server.await();
	}
	
	
	public void await(){
		ServerSocket serverSocket = null;
		int port = 8080;
		try {
			serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		//look awaiting for a request
		while(!shutdown){
			Socket socket = null;
			InputStream in = null;
			OutputStream out = null;
			try {
				socket = serverSocket.accept();
				in = socket.getInputStream();
				out = socket.getOutputStream();
				//create Request object and parse
				Request request = new Request(in);
				request.parse();
				//create Response object
				Response response = new Response(out);
				response.setRequest(request);
				response.sendStaticResource();
				//close the socket 
				socket.close();
				//check if the previous URI is a shutdown command
				shutdown = SHUTDOWN_COMMAND.equals(request.getUri());
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
		}
	}
}
