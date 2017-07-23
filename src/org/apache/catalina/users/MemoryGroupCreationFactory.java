package org.apache.catalina.users;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

public class MemoryGroupCreationFactory implements ObjectFactory {

	@Override
	public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
			throws Exception {
		
		if(obj == null || !(obj instanceof Reference)){
			return null;
		}
		Reference ref = (Reference) obj;
		if (!"org.apache.catalina.UserDatabase".equals(ref.getClassName())) {
            return (null);
        }
		
		//Create and configure a MemoryUserDatabase instance based on the refAddr val
		
		
		return null;
	}

}
