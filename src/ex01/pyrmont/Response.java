package ex01.pyrmont;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * HTTP Response = Status-line
 * (( general-header | response-header | entity-header))
 * CRLF
 * [message-body]
 * Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
 *
 */
public class Response {

	private static final int BUFFER_SIZE = 1024;
	private Request request;
	private OutputStream output;
	
	public Response(OutputStream output) {
		this.output = output;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	public void sendStaticResource() throws IOException {
		byte[] bytes = new byte[BUFFER_SIZE];
		FileInputStream fis = null;
		try {
			File file = new File(HttpServer.WEB_ROOT, request.getUri());
			if(file.exists()){
				fis = new FileInputStream(file);
				int ch = fis.read(bytes,0, BUFFER_SIZE);
				output.write("HTTP/1.1 200 OK\r\nContent-Type:text/html;charset=UTF-8\r\n\r\n".getBytes());
				while(ch != -1){
					output.write(bytes, 0, ch);
					ch = fis.read(bytes, 0, BUFFER_SIZE);
				}
			}else{
				//file not found
				String errorMessage = "HTTP/1.1 404 File Not Found\r\n"//
						+ "Content-Type:text/html\r\n"//
						+ "Content-Length:23\r\n"//
						+ "\r\n"//
						+ "<h1>File Not Found</h1>";
				output.write(errorMessage.getBytes());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(fis != null)
				fis.close();
		}
		
	}

}
