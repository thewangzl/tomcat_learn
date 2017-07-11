package org.apache.catalina.util;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Date;

@SuppressWarnings("serial")
public class FastHttpDateFormat  extends DateFormat{

	DateFormat df;
	
	long lastSec = -1;
	
	StringBuffer sb = new StringBuffer();
	
	FieldPosition fp = new FieldPosition(DateFormat.MILLISECOND_FIELD);
	
	
	public FastHttpDateFormat(DateFormat df) {
		this.df = df;
	}

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
		long dt = date.getTime();
		long ds = dt / 1000;
		if(ds != lastSec){
			sb.setLength(0);
			df.format(date, sb, fp);
			lastSec = ds;
		}else{
			//Munge current msec into existing string
			int ms = (int) (dt % 1000);
			int pos = fp.getEndIndex();
			int begin = fp.getBeginIndex();
			if(pos > 0){
				if(pos > begin){
					sb.setCharAt(--pos, Character.forDigit(ms % 10, 10));
				}
				ms /= 10;
				if(pos > begin){
					sb.setCharAt(--pos, Character.forDigit(ms % 10, 10));
				}
				ms /= 10;
				if(pos > begin){
					sb.setCharAt(--pos, Character.forDigit(ms % 10, 10));
				}
			}
		}
		toAppendTo.append(sb.toString());
		return toAppendTo;
	}

	@Override
	public Date parse(String source, ParsePosition pos) {

		return df.parse(source, pos);
	}

}
