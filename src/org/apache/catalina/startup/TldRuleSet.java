package org.apache.catalina.startup;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;

public class TldRuleSet extends RuleSetBase {

	private String prefix;
	
	public TldRuleSet() {
		this("");
	}
	
	public TldRuleSet(String prefix) {
		super();
		this.namespaceURI = null;
		this.prefix = prefix;
		
	}

	@Override
	public void addRuleInstances(Digester digester) {
		digester.addCallMethod(prefix + "taglib/listener/listener-class", "addApplicationListener", 0);

	}


}
