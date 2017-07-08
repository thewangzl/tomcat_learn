package org.apache.catalina.logger;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.CharArrayWriter;
import java.io.PrintWriter;

import javax.servlet.ServletException;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Logger;

public abstract class LoggerBase implements Logger {

	protected int verbosity = ERROR;
	
	protected Container container;
	
	protected PropertyChangeSupport support = new PropertyChangeSupport(this);
	
	
	protected static final String info = "org.apache.catalina.logger.LoggerBase/1.0";
	@Override
	public Container getContainer() {

		return this.container;
	}

	@Override
	public void setContainer(Container container) {
		this.container = container;
	}

	@Override
	public int getVerbosity() {

		return verbosity;
	}

	@Override
	public void setVerbosity(int verbosity) {
		
		this.verbosity = verbosity;
	}
	
	/**
	 * 
	 * @param verbosity
	 */
	public void setVerbosity(String verbosity){
		if("FATAL".equalsIgnoreCase(verbosity)){
			this.verbosity = FATAL;
		}else if("ERROR".equalsIgnoreCase(verbosity)){
			this.verbosity = ERROR;
		}else if("WARNING".equalsIgnoreCase(verbosity)){
			this.verbosity = WARNING;
		}else if("INFORMATION".equalsIgnoreCase(verbosity)){
			this.verbosity = INFORMATION;
		}else if("DEBUG".equalsIgnoreCase(verbosity)){
			this.verbosity = DEBUG;
		}
	}

	@Override
	public abstract void log(String message);

	@Override
	public void log(Exception e, String message) {
		log(message, e);
	}

	@Override
	public void log(String message, Throwable throwable) {

		CharArrayWriter buf = new CharArrayWriter();
		PrintWriter writer = new PrintWriter(buf);
		writer.println(message);
		throwable.printStackTrace(writer);
		Throwable rootCause = null;
		if (throwable instanceof LifecycleException	){
			rootCause = ((LifecycleException)throwable).getThrowable();
		}else if(throwable instanceof ServletException){
			rootCause = ((ServletException)throwable).getRootCause();
		}
		if(rootCause != null){
			writer.println("--------Root Cause --------------");
			rootCause.printStackTrace(writer);;		
		}
		log(buf.toString());
	}

	@Override
	public void log(String message, int verbosity) {
		
		if(this.verbosity >= verbosity){
			log(message);
		}
	}

	@Override
	public void log(String message, Throwable t, int verbosity) {
		if(this.verbosity >= verbosity){
			log(message, t);
		}
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		support.removePropertyChangeListener(listener);
	}

	@Override
	public String getInfo() {
		return info;
	}
}
