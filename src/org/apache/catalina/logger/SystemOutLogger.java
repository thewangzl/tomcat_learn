package org.apache.catalina.logger;

public class SystemOutLogger extends LoggerBase {

	protected static final String info = "org.apache.catalina.logger.SystemOutLogger/1.0";
	
	
	@Override
	public void log(String message) {
		System.out.println(message);
	}

}
