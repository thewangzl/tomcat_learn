package ex03.pyrmont.connector.http;

public class HttpRequestLine {

	public static final int INITIAL_METHOD_SIZE = 8;
	public static final int INITIAL_URI_SIZE = 64;
	public static final int INITAL_PROTOCOL_SIZE = 8;
	public static final int MAX_METHOD_SIZE = 1024;
	public static final int MAX_URI_SIZE = 32768;
	public static final int MAX_PROTOCOL_SIZE = 1024;
	
	public HttpRequestLine() {
		this(new char[INITIAL_METHOD_SIZE], 0 , new char[INITIAL_URI_SIZE], 0, new char[INITAL_PROTOCOL_SIZE], 0);
	}
	
	public char[] method;
	public int methodEnd;
	public char[] uri;
	public int uriEnd;
	public char[] protocol;
	public int protocolEnd;
	
	public HttpRequestLine(char[] method, int methodEnd, char[] uri, int uriEnd, char[] protocol, int protocolEnd){
		this.method = method;
		this.methodEnd = methodEnd;
		this.uri = uri;
		this.uriEnd = uriEnd;
		this.protocol = protocol;
		this.protocolEnd = protocolEnd;
	}
	
	public void recycle(){
		this.method = null;
		this.uri = null;
		this.protocol = null;
	}
	
	public int indexOf(char[] buf){
		return this.indexOf(buf, buf.length);
	}
	
	public int indexOf(char[] buf,int end){
		char firstChar = buf[0];
		int pos = 0;
		while(pos < this.uriEnd){
			pos = indexOf(firstChar, pos);
			if(pos == -1){
				return -1;
			}
			if(uriEnd - pos < end){
				return -1;
			}
			for(int i = 0; i < end; i++){
				if(uri[i + pos] != buf[i]){
					break;
				}
				if(i == (end - 1)){
					return pos;
				}
			}
			pos++;
		}
		return -1;
	}

	public int indexOf(String str) {
		return this.indexOf(str.toCharArray());
	}
	
	public int indexOf(char ch, int start){
		for(int i = start; i < this.uriEnd; i++){
			if(uri[i] == ch){
				return i;
			}
		}
		return -1;
	}
	
	
}
