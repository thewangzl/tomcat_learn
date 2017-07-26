package org.apache.catalina.startup;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;

public class EngineRuleSet extends RuleSetBase {

	// ----------------------------------------------------- Instance Variables

	/**
	 * The matching pattern prefix to use for recognizing our elements.
	 */
	protected String prefix = null;

	// ------------------------------------------------------------ Constructor

	/**
	 * Construct an instance of this <code>RuleSet</code> with the default
	 * matching pattern prefix.
	 */
	public EngineRuleSet() {

		this("");

	}

	/**
	 * Construct an instance of this <code>RuleSet</code> with the specified
	 * matching pattern prefix.
	 *
	 * @param prefix
	 *            Prefix for matching pattern rules (including the trailing
	 *            slash character)
	 */
	public EngineRuleSet(String prefix) {

		super();
		this.namespaceURI = null;
		this.prefix = prefix;

	}

	// --------------------------------------------------------- Public Methods

	/**
	 * <p>
	 * Add the set of Rule instances defined in this RuleSet to the specified
	 * <code>Digester</code> instance, associating them with our namespace URI
	 * (if any). This method should only be called by a Digester instance.
	 * </p>
	 *
	 * @param digester
	 *            Digester instance to which the new Rule instances should be
	 *            added.
	 */
	public void addRuleInstances(Digester digester) {

		digester.addObjectCreate(prefix + "Engine", "org.apache.catalina.core.StandardEngine", "className");
		digester.addSetProperties(prefix + "Engine");
		digester.addRule(prefix + "Engine",
				new LifecycleListenerRule(digester, "org.apache.catalina.startup.EngineConfig", "engineConfigClass"));
		digester.addSetNext(prefix + "Engine", "setContainer", "org.apache.catalina.Container");

		digester.addObjectCreate(prefix + "Engine/Listener", null, // MUST be
																	// specified
																	// in the
																	// element
				"className");
		digester.addSetProperties(prefix + "Engine/Listener");
		digester.addSetNext(prefix + "Engine/Listener", "addLifecycleListener",
				"org.apache.catalina.LifecycleListener");

		digester.addObjectCreate(prefix + "Engine/Logger", null, // MUST be
																	// specified
																	// in the
																	// element
				"className");
		digester.addSetProperties(prefix + "Engine/Logger");
		digester.addSetNext(prefix + "Engine/Logger", "setLogger", "org.apache.catalina.Logger");

		digester.addObjectCreate(prefix + "Engine/Realm", null, // MUST be
																// specified in
																// the element
				"className");
		digester.addSetProperties(prefix + "Engine/Realm");
		digester.addSetNext(prefix + "Engine/Realm", "setRealm", "org.apache.catalina.Realm");

		digester.addObjectCreate(prefix + "Engine/Valve", null, // MUST be
																// specified in
																// the element
				"className");
		digester.addSetProperties(prefix + "Engine/Valve");
		digester.addSetNext(prefix + "Engine/Valve", "addValve", "org.apache.catalina.Valve");

	}
}
