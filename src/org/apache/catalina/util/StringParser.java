package org.apache.catalina.util;

/**
 * Utility class for string parsing that is higher performance than StringParser 
 * for simple delimited text cases. Parsing is performed by setting the string, 
 * and then using the <code>findXXX()</code> and <code>skipXXX()</code> families 
 * methods to remember significant offsets, To retrieve the parsed substrings,
 * call the <code>extract()</code> method with the appropriate saved offset values.
 * 
 * @author thewangzl
 *
 */
public final class StringParser {

	
	/**
	 * The characters of the current string, Stored when the string is first
	 * specified to speed up access to characters being compared during parsing.
	 * 
	 */
	private char[] chars = null;
	
	/**
	 * The zero-relative index of the current point at which we are positioned within the string being parsed.
	 * <strong>NOTE</strong>:the value of this index can be one larger than the index the last character of 
	 * the string (i.e. equal to the string length) if you parse off the end of thw string. This value is 
	 * useful for extracting substrings that include the end of the string.
	 */
	private int index;
	
	/**
	 * The length of the String we are currently parsing. Stored when the string is first specified to 
	 * avoid repeated recalculations.
	 * 
	 */
	private int length;
	
	/**
	 * The string we are currently parsing
	 */
	private String string;
	
	public StringParser() {
		this(null);
	}
	
	public StringParser(String string) {
		setString(string);
	}
	
	
	public int getIndex() {
		return index;
	}
	
	public int getLength() {
		return length;
	}
	
	public String getString() {
		return string;
	}
	
	/**
	 * Set the String we are currently parsing. The parser state is als reset to begin 
	 * at the start of this string.
	 * 
	 * @param string
	 */
	public void setString(String string) {
		this.string = string;
		if(string != null){
			this.length = string.length();
			chars = this.string.toCharArray();
		}else{
			this.length = 0;
			this.chars = new char[0];
		}
		
		reset();
	}

	/**
	 * Advance the current parsing postition by one, if we are not already 
	 * past the end of the string.
	 */
	public void advance(){
		if(index < length){
			index++;
		}
	}
	
	/**
	 * Extract and return a substring that starts at the specified position,
	 * and extends to the end of the string being parsed. If this is not possible,
	 * a zero-length string is returned.
	 * 
	 * @param start Starting index, zero relative, inclusive.
	 * @return
	 */
	public String extract(int start){
		if(start < 0 || start >= length){
			return "";
		}
		return string.substring(start);
	}
	
	public String extract(int start, int end){
		if(start < 0 || start >= end || end > length){
			return "";
		}
		return string.substring(start, end);
	}
	
	/**
	 * Return the index of the next occurrence of the specified character, or the index of the character after 
	 * the last position of the string if no more occurrences of this character are found. The current parsing
	 * position is updated to the returned value.
	 * 
	 * @param ch
	 * @return
	 */
	public int findChar(char ch){
		while(index < length && ch != chars[index]){
			index++;
		}
		return index;
	}
	
	/**
	 * Return the index of the next occurrence of a non-whitespace character, or the index of the
	 * character after the last position of the string if no more non-whitespace characters are found.
	 * The current parsing postion is updated to the returned value.
	 * 
	 * @return
	 */
	public int findText(){
		while(index < length && isWhite(chars[index])){
			index++;
		}
		return index;
	}
	
	/**
	 * Return the index of the next occurrence of a whitespace character, or the index of the
	 * character after the last position of the string if no more whitespace characters are found.
	 * The current parsing postion is updated to the returned value.
	 * 
	 * @return
	 */
	public int findWhite(){
		while(index < length && !isWhite(chars[index])){
			index++;
		}
		return index;
	}
	
	/**
	 * Advance the current parsing position while it is pointing at the specified character,
	 * or until it moves past the end of the string. Return the final value.
	 * 
	 * @param ch
	 * @return
	 */
	public int skipChar(char ch){
		while(index < length && ch == chars[index]){
			index++;
		}
		return index;
	}
	
	/**
	 * Advance the current parsing position while it is pointing at a non-whitespace character,
	 * or until it moves past the end of the string. Return the final value.
	 * 
	 * @return
	 */
	public int skipText(){
		while(index < length && !isWhite(chars[index])){
			index++;
		}
		return index;
	}
	
	/**
	 * Advance the current parsing position while it is pointing at a whitespace character,
	 * or until it moves past the end of the string. Return the final value.
	 * 
	 * @return
	 */
	public int skipWhite(){
		while(index < length && isWhite(chars[index])){
			index++;
		}
		return index;
	}
	
	/**
	 * Is the specified character considered to be whitespace?
	 * 
	 * @param ch
	 * @return
	 */
	public boolean isWhite(char ch){
		if(ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n'){
			return true;
		}
		return false;
	}
	
	/**
	 * Reset the current state of the parser to the beginnng of 
	 * the current string being parsed.
	 * 
	 */
	public void reset(){
		index = 0;
	}
}
