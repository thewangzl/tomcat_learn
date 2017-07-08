package org.apache.catalina.logger;

public class SystemErrLogger extends LoggerBase {

	protected static final String info = "org.apache.catalina.logger.SystemErrLogger/1.0";
	
	
	@Override
	public void log(String message) {

		System.err.println(message);
	}

}
