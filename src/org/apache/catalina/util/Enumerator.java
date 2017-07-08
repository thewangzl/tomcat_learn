package org.apache.catalina.util;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

public class Enumerator<T> implements Enumeration<T> {

	private Iterator<T> iter;
	
	public Enumerator(Collection<T> collection) {
		this(collection.iterator());
	}
	
	public Enumerator(Iterator<T> iter) {
		super();
		this.iter = iter;
	}
	
	public Enumerator(Map<?,T> map) {
		this(map.values().iterator());
	}
	
	@Override
	public boolean hasMoreElements() {

		return iter.hasNext();
	}

	@Override
	public T nextElement() {

		return iter.next();
	}

}
