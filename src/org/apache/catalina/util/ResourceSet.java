package org.apache.catalina.util;

import java.util.Collection;
import java.util.HashSet;

@SuppressWarnings("serial")
public final class ResourceSet<T> extends HashSet<T> {

	/**
	 * The current lock state of this parameter map.
	 */
	private boolean locked;
	
	private static final StringManager sm = StringManager.getManager("org.apache.catalina.util");

	
	public ResourceSet(){
		super();
	}
	
	public ResourceSet(int initialCapacity){
		super(initialCapacity);
	}
	
	
	public ResourceSet(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public ResourceSet(Collection<? extends T> c) {
		super(c);
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
	
	@Override
	public boolean add(T e) {
		if(locked){
			throw new IllegalStateException(sm.getString("resourceSet.locked"));
		}
		return super.add(e);
	}
	
	@Override
	public void clear() {
		if(locked){
			throw new IllegalStateException(sm.getString("resourceSet.locked"));
		}
		super.clear();
	}
	
	@Override
	public boolean remove(Object o) {
		if(locked){
			throw new IllegalStateException(sm.getString("resourceSet.locked"));
		}
		return super.remove(o);
	}
	
	
	
	
}
