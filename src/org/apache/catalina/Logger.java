package org.apache.catalina;

import java.beans.PropertyChangeListener;

/**
 * A <b>Logger</b> is a generic interface for the message and exception logging methods 
 * of the ServletContext interface. Loggers can be attached at any Container level, but 
 * will typically only be attached to a Context, or higher level, Container.
 * 
 * @author thewangzl
 *
 */
public interface Logger {

	// ------------------------------------------------- Manifest Constants
	
	/**
	 * Verbosity level constants for log messages that may be filtered
	 * by the underlying logger.
	 */
	
	public static final int FATAL = Integer.MIN_VALUE;
	
	public static final int ERROR = 1;
	
	public static final int WARNING = 2;
	
	public static final int INFORMATION = 3;
	
	public static final int DEBUG = 4;
	
	// ------------------------------------------ Properties
	
	/**
	 * Return the Container with which this Logger has been associated.
	 * 
	 * @return
	 */
	public Container getContainer();
	
	/**
	 * Set  the Container with which this Logger has been associated.
	 * 
	 * @param container
	 */
	public void setContainer(Container container);
	
	/**
	 * 
	 * @return
	 */
	public String getInfo();
	
	/**
	 * Return the verbosity level of this logger. Messages logged with a
	 * higher verbosity than this level will be silently ignored.
	 * 
	 * @return
	 */
	public int getVerbosity();
	
	/**
	 * Set the verbosity level of this logger. Messages logged with a
	 * higher verbosity than this level will be silently ignored.
	 * 
	 * @param verbosity
	 */
	public void setVerbosity(int verbosity);
	
	/**
	 * Wites the specified message to a servlet log file, usually an event log.
	 * The name and type of the servlet log is specific to the servlet container.
	 * This message will be logged unconditionally.
	 * 
	 * @param message A <code>String</code> specifying the message to be written
	 * 			to the log file
	 */
	public void log(String message);
	
	/**
	 *  
	 * @param e
	 * @param message
	 */
	public void log(Exception e, String message);
	
	/**
	 * Writes an explainatory message and a stack trace for a given <code>Thowable</code> exception
	 * to the servlet log file. The name and type of the servlet log file is specific to the servlet
	 * container,usually an event log. This message will be logged unconditionally.
	 * 
	 * @param message
	 * @param throwable
	 */
	public void log(String message, Throwable throwable);
	
	/**
	 * Written the specified message to the servlet log file,usually an event log,
	 * If the logger is set to a verbosity level equal to or higher than the specified 
	 * value for this message.
	 * 
	 * @param message
	 * @param verbosity Verbosity level of this message
	 */
	public void log(String message, int verbosity);
	
	/**
	 * 
	 * @param message
	 * @param throwable
	 * @param verbosity
	 */
	public void log(String message, Throwable throwable, int verbosity);
	
	/**
	 * Add a property change listener to this component.
	 * 
	 * @param listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener);

	
	/**
	 * Remove a property change listener from this component.
	 * 
	 * @param listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener);
	
}
