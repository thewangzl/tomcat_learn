package org.apache.catalina.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Simple utility module to make it easy to plug in the server identifer
 * when integrating Tomcat.
 * 
 * @author thewangzl
 *
 */
public class ServerInfo {

	private static String serverInfo = null;
	
	static {
		try {
			InputStream is = ServerInfo.class.getResourceAsStream("ServerInfo.properties");
			Properties props = new Properties();
			props.load(is);
			is.close();
			serverInfo = props.getProperty("server.info");
		} catch (IOException e) {
			;
		}
		if(serverInfo == null){
			serverInfo = "Apache Tomcat";
		}
	}
	
	public static String getServerInfo() {
		return serverInfo;
	}
}
