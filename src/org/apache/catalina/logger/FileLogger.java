package org.apache.catalina.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.util.LifecycleSupport;

public class FileLogger extends LoggerBase implements Lifecycle {

	/**
	 * The as-if date for currently open log file,or a zero-length string if
	 * there is no open log file.
	 */
	private String date = "";

	/**
	 * The directory in which log files are created.
	 */
	private String directoty = "logs";

	/**
	 * The prefix that is added to log file filenames.
	 */
	private String prefix = "catalina.";

	/**
	 * The suffix that is added to log file filenames.
	 */
	private String suffix = ".log";

	private LifecycleSupport lifecycle = new LifecycleSupport(this);

	/**
	 * has this component been started ?
	 */
	private boolean started;

	/**
	 * Should logged messages be date/time stamped ?
	 */
	private boolean timestamp;

	/**
	 * The PrintWriter to which we are currently logging, if any.
	 */
	private PrintWriter writer;

	@Override
	public void start() throws LifecycleException {

		// Validate and update our current component state
		if (started) {
			throw new LifecycleException("fileLogger.alreadyStarted");
		}

		lifecycle.fireLifecycleEvent(START_EVENT, null);
		started = true;

	}

	@Override
	public void stop() throws LifecycleException {

		// Validate and update our current component state
		if (!started) {
			throw new LifecycleException("fileLogger.notStarted");
		}
		lifecycle.fireLifecycleEvent(STOP_EVENT, null);
		started = false;

		this.close();
	}

	@Override
	public void log(String message) {
		// Construct the timestamp we will use, if reguested
		Timestamp ts = new Timestamp(System.currentTimeMillis());
		String tsString = ts.toString().substring(0, 19);
		String tsDate = tsString.substring(0, 10);

		// If the date has changed, switch log files
		if (!date.equals(tsDate)) {
			synchronized (this) {
				if (!date.equals(tsDate)) {
					close();
					date = tsDate;
					open();
				}
			}
		}
		
		//Log this message, timestamped if necessary
		if(writer != null){
			if(timestamp){
				writer.println(tsString + " " + message) ;
			}else{
				writer.println(message);
			}
		}
	}

	private void open() {
		//Create the directory if necessary
		File dir = new File(directoty);
		if(!dir.isAbsolute()){
			dir = new File(System.getProperty("catalina.base"), directoty);
		}
		dir.mkdirs();
		
		//Open the current log file
		try {
			String pathName = dir.getAbsolutePath() + File.separator + prefix + date + suffix;
			writer = new PrintWriter(new FileWriter(pathName, true), true);
		} catch (IOException e) {
			writer = null;
		}
	}
	
	private void close() {
		if(writer == null){
			return;
		}
		writer.flush();
		writer.close();
		writer = null;
		date = "";
	}
	
	public String getDirectoty() {
		return directoty;
	}
	
	public void setDirectoty(String directoty) {
		String oldDirectory = this.directoty;
		this.directoty = directoty;
		support.firePropertyChange("directory", oldDirectory, this.directoty);
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public void setPrefix(String prefix) {
		String oldPrefix = this.prefix;
		this.prefix = prefix;
		support.firePropertyChange("prefix", oldPrefix, this.prefix);
	}
	
	public String getSuffix() {
		return suffix;
	}
	
	public void setSuffix(String suffix) {
		String oldSuffix = this.suffix;
		this.suffix = suffix;
		support.firePropertyChange("suffix", oldSuffix, this.suffix);
	}

	public boolean isTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(boolean timestamp) {
		boolean oldTimestamp = this.timestamp;
		this.timestamp = timestamp;
		support.firePropertyChange("timestamp", oldTimestamp, this.timestamp);
	}

	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		lifecycle.addLifecycleListener(listener);
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {

		return lifecycle.findLifecycleListeners();
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		lifecycle.removeLifecycleListener(listener);
	}
}
