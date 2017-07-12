package org.apache.catalina.valves;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Globals;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.ServerInfo;
import org.apache.catalina.util.StringManager;

public class ErrorReportValve extends ValveBase {

	private static final String info = "org.apache.catalina.valves.ErrorReportValve/1.0";
	
	private static final StringManager sm = StringManager.getManager(Constants.Package);
	
	
	@Override
	public void invoke(Request request, Response response, ValveContext valveContext) throws IOException, ServletException {

		//Perform the request
		valveContext.invokeNext(request, response);
		
		ServletRequest sreq = (ServletRequest) request;
		Throwable throwable = (Throwable) sreq.getAttribute(Globals.EXCEPTION_ATTR);
		
		ServletResponse sresp = (ServletResponse)response;

		if(sresp.isCommitted()){
			return;
		}
		if(throwable != null){
			
			response.setError();
			
			try {
				sresp.reset();
			} catch (IllegalArgumentException e) {
				;
			}
			
			ServletResponse sresponse = (ServletResponse) response;
			if(sresponse instanceof HttpServletResponse){
				((HttpServletResponse) sresponse).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
		
		response.setSuspended(false);
		
	}
	
	/**
	 * Prints out an error report.
	 * 
	 * @param request
	 * @param response
	 * @param throwable
	 * @throws IOException
	 */
	protected void report(Request request, Response response, Throwable throwable) throws IOException{
		
		//
		if(!(response instanceof HttpResponse)){
			return;
		}
		HttpResponse hresponse = (HttpResponse) response;
		if(!(hresponse instanceof HttpServletResponse)){
			return;
		}
		HttpServletResponse hresp = (HttpServletResponse) response;
		int statusCode = hresponse.getStatus();
		String message = RequestUtil.filter(hresponse.getMessage());
		if(message == null){
			message = "";
		}
		
		//DO nothing on a 1XX and 2XX status
		if(statusCode < 300){
			return;
		}
		
		//Do nothing on NOT MDIFIED status
		if(statusCode == HttpServletResponse.SC_NOT_MODIFIED){
			return;
		}
		
		// FIXME - Reset part of the request
		
		Throwable rootCause = null;
		if(throwable != null){
			if(throwable instanceof ServletException){
				rootCause = ((ServletException) throwable).getRootCause();
			}
		}
		
		//Do nothing if there is no report for the specified status code
		String report = null;
		try{
			report = sm.getString("http." + statusCode , message);
		}catch(Throwable t){
			;
		}
		if(report == null){
			return;
		}
		StringBuffer sb = new StringBuffer();
		sb.append("<html><head><title>");
		sb.append(ServerInfo.getServerInfo()).append(" - ");
		sb.append(sm.getString("errorReportValve.errorReport"));
		sb.append("</title>");
		sb.append("<STYLE><!--");
		sb.append("H1{font-family : sans-serif,Arial,Tahoma;color : white;background-color : #0086b2;} ");
        sb.append("H3{font-family : sans-serif,Arial,Tahoma;color : white;background-color : #0086b2;} ");
        sb.append("BODY{font-family : sans-serif,Arial,Tahoma;color : black;background-color : white;} ");
        sb.append("B{color : white;background-color : #0086b2;} ");
        sb.append("HR{color : #0086b2;} ");
        sb.append("--></STYLE> ");
        sb.append("</head><body>");
        sb.append("<h1>");
        sb.append(sm.getString("errorReportValve.statusHeader",
                               "" + statusCode, message)).append("</h1>");
        sb.append("<HR size=\"1\" noshade>");
        sb.append("<p><b>type</b> ");
        if (throwable != null) {
            sb.append(sm.getString("errorReportValve.exceptionReport"));
        } else {
            sb.append(sm.getString("errorReportValve.statusReport"));
        }
        sb.append("</p>");
        sb.append("<p><b>");
        sb.append(sm.getString("errorReportValve.message"));
        sb.append("</b> <u>");
        sb.append(message).append("</u></p>");
        sb.append("<p><b>");
        sb.append(sm.getString("errorReportValve.description"));
        sb.append("</b> <u>");
        sb.append(report);
        sb.append("</u></p>");
        
        if(throwable != null){
        	StringWriter stackTrace = new StringWriter();
        	throwable.printStackTrace(new PrintWriter(stackTrace));
        	sb.append("<p><b>");
            sb.append(sm.getString("errorReportValve.exception"));
            sb.append("</b> <pre>");
            sb.append(stackTrace.toString());
            sb.append("</pre></p>");
            if (rootCause != null) {
                stackTrace = new StringWriter();
                rootCause.printStackTrace(new PrintWriter(stackTrace));
                sb.append("<p><b>");
                sb.append(sm.getString("errorReportValve.rootCause"));
                sb.append("</b> <pre>");
                sb.append(stackTrace.toString());
                sb.append("</pre></p>");
            }
        }
		
        sb.append("<HR size=\"1\" noshade>");
        sb.append("<h3>").append(ServerInfo.getServerInfo()).append("</h3>");
        sb.append("</body></html>");
		
		try {
			
			Writer writer = response.getReporter();
			
			if(writer != null){
				
				Locale locale = Locale.getDefault();
				
				try {
					hresp.setContentType("text/html");
					hresp.setLocale(locale);
				} catch (Throwable e) {
					if(debug >= 1){
						log("status.setContextType", e);
					}
				}
			}
			
			writer.write(sb.toString());
			writer.flush();
			
		} catch (IOException e) {
			;
		}catch (IllegalStateException e) {
			;
		}
	}
	
	@Override
	public String getInfo() {
		return info;
	}

}
