package org.apache.catalina.core;

import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.Mapper;
import org.apache.catalina.Request;
import org.apache.catalina.util.StringManager;

public class StandardEngineMapper implements Mapper {

	private StandardEngine engine;
	
	private String protocol;
	
	private static final StringManager sm = StringManager.getManager(Constants.Package);
	
	@Override
	public Container getContainer() {
		return engine;
	}

	@Override
	public void setContainer(Container container) {
		if(!(container instanceof StandardEngine)){
			throw new IllegalArgumentException(sm.getString("httpEngineMapper.container"));
		}
		this.engine = (StandardEngine) container;
	}

	@Override
	public String getProtocol() {

		return this.protocol;
	}

	@Override
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	@Override
	public Container map(Request request, boolean update) {

		int debug = engine.getDebug();
		
		//Extract the requested server name
		String server = request.getRequest().getServerName();
		if(server == null){
			server = engine.getDefaultHost();
			if(update){
				request.setServerName(server);
			}
		}
		if(server == null){
			return null;
		}
		
		server = server.toLowerCase();
		if(debug >= 1){
			engine.log("Mapping server name '" + server + "' "); 
		}
		// Find the matching child Host directly
		if(debug >= 2){
			engine.log("Trying a direct match");
		}
		Host host = (Host) engine.findChild(server);
		
		//Find the matching Host by alias. FIXME - optimize this !
		if(host == null){
			if(debug >= 2){
				engine.log(" Trying an alias match");
			}
			Container[] children = engine.findChildren();
			for (int i = 0; i < children.length; i++) {
				String[] aliases = ((Host)children[i]).findAliases();
				for (int j = 0; j < aliases.length; j++) {
					if(server.equals(aliases[i])){
						host = (Host) children[i];
						break;
					}
				}
				if(host != null){
					break;
				}
			}
		}
		
		//Trying the "default" host if any
		if(host == null){
			if(debug >= 2){
				engine.log("Trying the default host");
			}
			host = (Host) engine.findChild(engine.getDefaultHost());
		}
		
		return host;
	}

}
