package org.apache.catalina;

@SuppressWarnings("serial")
public class LifecycleException extends Exception {

	/**
	 * The error message passed to our constructor (if any)
	 */
	protected String message;
	
	/**
	 * The underly exception or error passed to our constructor (if any)
	 */
	protected Throwable throwable;
	
	public LifecycleException() {
		this(null,null);
	}
	public LifecycleException(String message) {
		this(message,null);
	}
	
	public LifecycleException(Throwable throwable){
		this(null, throwable);
	}

	public LifecycleException(String message, Throwable throwable) {
		super();
		this.message = message;
		this.throwable = throwable;
	}
	
	@Override
	public String getMessage() {
		return this.message;
	}
	
	public Throwable getThrowable() {
		return throwable;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("LifecycleException: ");
		if(message != null){
			sb.append(message);
			if(throwable != null){
				sb.append(": ");
			}
		}
		if(throwable != null){
			sb.append(throwable.toString());
		}
		return sb.toString();
	}
	

}
