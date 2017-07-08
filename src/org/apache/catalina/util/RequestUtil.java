/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.http.Cookie;

/**
 * General purpose request parsing and encoding utility methods.
 *
 * @author Craig R. McClanahan
 * @author Tim Tye
 */
public final class RequestUtil {

    public static Cookie[] parseCookieHeader(String header){
    	if(header == null || header.length() == 0){
    		 return new Cookie[0];
    	}
    	
    	ArrayList<Cookie> cookies = new ArrayList<>();
    	while(header.length() > 0){
    		int semicolon = header.indexOf(";");
    		if (semicolon < 0){
    			semicolon = header.length();
    		}
			if(semicolon == 0){
				break;
			}
			String token = header.substring(0,semicolon);
			if(semicolon < header.length()){
				header = header.substring(semicolon+ 1);
			}else{
				header = "";
			}
			try{
				int equals = token.indexOf("=");
				if (equals > 0){
					String name = token.substring(0, equals).trim();
					String value = token.substring(equals + 1).trim();
					cookies.add(new Cookie(name, value));
				}
			}catch(Throwable t){
				;
			}
    	}
    	return cookies.toArray(new Cookie[cookies.size()]);
    }
    
    public static void parseParameters(Map map,String data, String encoding) throws UnsupportedEncodingException{
    	if(data != null && data.length() > 0){
    		int len = data.length();
    		byte[] buf = new byte[len];
    		buf = data.getBytes(encoding);
    		parseParameters(map, buf, encoding);
    	}
    }

	public static void parseParameters(Map map, byte[] data, String encoding) throws UnsupportedEncodingException {
		if(data != null && data.length > 0){
			int pos = 0;
			int ix = 0;
			int ox = 0;
			String key = null;
			String value = null;
			while(ix < data.length){
				byte c = data[ix++];
				switch((char) c){
				case '&':
					value = new String(data,0, ox, encoding);
					if(key != null){
						putMapEntity(map, key, value);
						key = null;
					}
					ox = 0;
					break;
				case '=':
					key = new String(data, 0, ox, encoding);
					ox = 0;
					break;
				case '+':
					data[ox++] = (byte) ' ';
					break;
				case '%':
					data[ox++] = (byte)((convertHexDigit(data[ix++]) << 4)+ convertHexDigit(data[ix++])) ;
					break;
				default:
					data[ox++] = c;	
				}
			}
			//The last value does not end in '&'.  So save it now.
			if(key != null){
				value = new String(data,0,ox, encoding);
				putMapEntity(map, key, value);
			}
		}
		
	}

	/**
	 * Convert a byte character value to hexidecimal digit value.
	 * @param b
	 * @return
	 */
	private static byte convertHexDigit(byte b) {
		if(b >= '0' && b <= '9' )
			return (byte) (b - '0');
		if(b >= 'a' && b <= 'f')
			return (byte)(b - 'a' + 10);
		if(b >= 'A' && b <= 'F')
			return (byte)(b - 'A' + 10);
		return 0;
	}

	/**
	 * Put name value pair in map.  When name already exist, add value
     * to array of values.
	 * @param map
	 * @param name
	 * @param value
	 */
	private static void putMapEntity(Map map, String name, String value) {

		String[] newValues = null;
		String[] oldValues = (String[]) map.get(name);
		if(oldValues == null){
			newValues = new String[1];
			newValues[0] = value;
		}else{
			newValues = new String[oldValues.length + 1];
			System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
			newValues[oldValues.length] = value;
		}
		map.put(name, newValues);
	}

	public static String parseCharacterEncoding(String contentType) {
		if(contentType == null){
			return null;
		}
		int start = contentType.indexOf("charset=");
		if (start < 0){
			return null;
		}
		String encoding = contentType.substring(start+8);
		int end = encoding.indexOf(';');
		if(end > 0){
			encoding = encoding.substring(0, end);
		}
		encoding = encoding.trim();
		if(encoding.length() > 2 && encoding.startsWith("\"") && encoding.endsWith("\"")){
			encoding = encoding.substring(1, encoding.length() - 1);
		}
		return encoding.trim();
	}
	/**
	 * Normalize a relative URI path that may have relative values ("/./", "/../", and so on) it it.
	 * <strong>WARINING</strong> - This method is  useful only for normalizing application-generated paths,
	 * It does not try to perform security checks for malicious input.
	 * 
	 * @param path
	 * @return
	 */
	public static String normalize(String path){
		if(path == null){
			return null;
		}
		
		//Create a place for the normalized path
		String normalized = path;
		if(normalized.equals("/.")){
			return "/";
		}
		
		//Add a leading "/" if necessaty
		if(!normalized.startsWith("/")){
			normalized = "/" + normalized;
		}
		
		//Resolve occurrences of "//" in the normalized path
		while(true){
			int index = normalized.indexOf("//");
			if(index < 0){
				break;
			}
			normalized = normalized.substring(0, index) + normalized.substring(index + 1);
		}
		
		//Resolve occurrences of "/./" in the normalized path.
		while(true){
			int index = normalized.indexOf("/./");
			if(index < 0){
				break;
			}
			normalized = normalized.substring(0, index) + normalized.substring(index + 2);
		}
		
		//Resolve occurrences of "/../" in the normalized path.
		while(true){
			int index = normalized.indexOf("/../");
			if(index < 0){
				break;
			}
			if(index == 0){
				return null;	//Try to go outside our context
			}
			int index2 = normalized.lastIndexOf("/", index - 1);
			normalized = normalized.substring(0, index2) + normalized.substring(index + 3);
		}
		
		return normalized;
	}
	
	
	
	
	
	
	
	
	
	
	
}
