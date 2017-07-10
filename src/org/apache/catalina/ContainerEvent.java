package org.apache.catalina;

import java.util.EventObject;

@SuppressWarnings("serial")
public class ContainerEvent extends EventObject {

	private Container container;

	private Object data;

	private String type;

	public ContainerEvent(Container container, String type, Object data) {
		super(container);
		this.container = container;
		this.type = type;
		this.data = data;
	}

	public Container getContainer() {
		return container;
	}

	public Object getData() {
		return data;
	}
	
	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return "ContainerEvent['" + getContainer() + "','" + getType() + "','"+ getData() + "']";
	}
}
