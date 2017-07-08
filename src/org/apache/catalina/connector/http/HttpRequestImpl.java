package org.apache.catalina.connector.http;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.ServletInputStream;

import org.apache.catalina.connector.HttpRequestBase;
import org.apache.catalina.util.Enumerator;

/**
 * Implementation of <b>HttpRequest</b> specified to the HTTP connector.
 * 
 * @author thewangzl
 *
 */
public final class HttpRequestImpl extends HttpRequestBase {

	/**
	 * Initial pool size
	 */
	protected static final int INITIAL_POOL_SIZE = 10;
	
	/**
	 * Pool size increment
	 */
	protected static final int POOL_SIZE_INCREMENT = 5;
	
	/**
	 * The InetAddress of the remote client of this request.
	 */
	protected InetAddress inet;
	
	protected static final String info = "org.apache.catalina.connector.http.HttpRequestImpl/1l0";
	
	/**
	 * Headers pool.
	 */
	protected HttpHeader[] headerPool = new HttpHeader[INITIAL_POOL_SIZE];
	
	/**
	 * position of the next available header in the pool
	 */
	protected int nextHeader = 0;
	
	/**
	 * Connection header.
	 */
	protected HttpHeader connectionHeader;
	
	/**
	 * Transfer encoding header
	 */
	protected HttpHeader transferEncodingHeader;
	
	
	public InetAddress getInet() {
		return inet;
	}
	
	public void setInet(InetAddress inet) {
		this.inet = inet;
	}
	
	public String getInfo() {
		return info;
	}
	
	@Override
	public void recycle() {
		super.recycle();
		inet = null;
		nextHeader = 0;
		connectionHeader = null;
	}
	
	@Override
	public ServletInputStream createInputStream() throws IOException {
		return new HttpRequestStream(this,(HttpResponseImpl)response);
	}
	
	/**
	 * Allocate new header.
	 * 
	 * @return an HttpHeader buffer allocated from the pool
	 */
	HttpHeader allocateHeader(){
		if(nextHeader == headerPool.length){
			//Grow the pool
			HttpHeader[] newHeaderPool = new HttpHeader[headerPool.length + POOL_SIZE_INCREMENT];
			for(int i = 0; i < nextHeader; i++){
				newHeaderPool[i] = headerPool[i];
			}
			headerPool = newHeaderPool;
		}
		if(headerPool[nextHeader] == null){
			headerPool[nextHeader] = new HttpHeader();
		}
		return headerPool[nextHeader];
	}
	
	/**
	 * Go to the next header.
	 */
	void nextHeader(){
		nextHeader++;
	}
	
	/**
	 * Add a Header to the set of Headers associated with this request.
	 * 
	 * @param name
	 * @param value
	 * 
	 */
	@Override
	public void addHeader(String name, String value) {
		if(nextHeader == headerPool.length){
			//Grow the pool
			HttpHeader[] newHeaderPool = new HttpHeader[headerPool.length + POOL_SIZE_INCREMENT];
			for(int i = 0; i < nextHeader; i++){
				newHeaderPool[i] = headerPool[i];
			}
			headerPool = newHeaderPool;
		}
		headerPool[nextHeader++] = new HttpHeader(name, value);
	}
	/**
	 * Clear the collection of Headers associated with this Request
	 * 
	 */
	@Override
	public void clearHeaders() {
		nextHeader = 0;
	}
	
	/**
	 * Return the first value of the specified header, if any,otherwise, return <code>null</code>
	 * 
	 * @param header Header we want to retrieve
	 * @return
	 */
	public HttpHeader getHeader(HttpHeader header){
		for(int i = 0; i < nextHeader; i++){
			if(headerPool[i].equals(header)){
				return headerPool[i];
			}
		}
		return null;
	}
	
	public HttpHeader getHeader(char[] headerName){
		for(int i = 0; i < nextHeader; i++){
			if(headerPool[i].equals(headerName)){
				return headerPool[i];
			}
		}
		return null;
	}
	
	/**
	 * Perform whatever actions are required to flush and close the input stream
	 * or reader, in a single operation
	 * 
	 * @throws IOException
	 */
	@Override
	public void finishRequest() throws IOException {
		//If neither a reader or an is have been opened,
		//do it to consume request bytes, if any.
		if(reader == null && stream == null && getContentLength() != 0 
				&& getProtocol() != null && getProtocol().contentEquals("HTTP/1.1")){
			getInputStream();
		}
		
		super.finishRequest();
	}
	
	/**
	 * Return the Internet Protocol (IP) address of the client that sent this request
	 * 
	 */
	@Override
	public String getRemoteAddr() {
		return inet.getHostAddress();
	}
	
	/**
	 * Return the fully qualified name of the client that sent this request,
	 * or the IP address of the client if the name connot be determined.
	 */
	@Override
	public String getRemoteHost() {
		if(connector.geEnableLookups()){
			return inet.getHostName();
		}else{
			return getRemoteAddr();
		}
	}
	
	/**
	 * Return the first value of the specified header,if any;otherwise,
	 * return <code>null</code>
	 * 
	 * @param name
	 * @return 
	 */
	@Override
	public String getHeader(String name) {
		name = name.toLowerCase();
		for(int i = 0; i < nextHeader; i++){
			if(headerPool[i].equals(name)){
				return new String(headerPool[i].value, 0, headerPool[i].valueEnd);
			}
		}
		return null;
	}
	
	/**
	 * 
	 */
	@Override
	public Enumeration<String> getHeaders(String name) {
		name = name.toLowerCase();
		ArrayList<String> tempValues = new ArrayList<>();
		for (int i = 0; i < nextHeader; i++) {
			if(headerPool[i].equals(name)){
				tempValues.add(new String(headerPool[i].value, 0, headerPool[i].valueEnd));
			}
		}
		return new Enumerator<>(tempValues);
	}
	
	/**
	 * 
	 * @return
	 */
	@Override
	public Enumeration<String> getHeaderNames() {
		ArrayList<String> tempArray = new ArrayList<>();
		for (int i = 0; i < nextHeader; i++) {
			tempArray.add(new String(headerPool[i].name, 0, headerPool[i].nameEnd));
		}
		return new Enumerator<>(tempArray);
	}
}
