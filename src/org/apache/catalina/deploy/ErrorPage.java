package org.apache.catalina.deploy;

public final class ErrorPage {

	/**
	 * The error (status) code for which this error page is active.
	 */
	private int errorCode = 0;
	
	/**
	 * The exception type for which this error page is active
	 */
	private String exceptionType;
	
	/**
	 * The context-relative location to handle this error or exception.
	 */
	private String location;

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	
	public void setErrorCode(String errorCode){
		try{
			this.errorCode = Integer.parseInt(errorCode);
		}catch(Throwable t){
			this.errorCode = 0;
		}
	}

	public String getExceptionType() {
		return exceptionType;
	}

	public void setExceptionType(String exceptionType) {
		this.exceptionType = exceptionType;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("ErrorPage[");
		if(exceptionType == null){
			sb.append("errorCode=").append(errorCode);
		}else{
			sb.append("exceptionType=").append(exceptionType);
		}
		sb.append(", location=").append(location);
		sb.append("]");
		return sb.toString();
	}
	
}
