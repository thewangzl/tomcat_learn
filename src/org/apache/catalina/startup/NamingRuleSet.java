package org.apache.catalina.startup;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;

public final class NamingRuleSet extends RuleSetBase {

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
	public NamingRuleSet() {

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
	public NamingRuleSet(String prefix) {

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

		digester.addObjectCreate(prefix + "Ejb", "org.apache.catalina.deploy.ContextEjb");
		digester.addSetProperties(prefix + "Ejb");
		digester.addSetNext(prefix + "Ejb", "addEjb", "org.apache.catalina.deploy.ContextEjb");

		digester.addObjectCreate(prefix + "Environment", "org.apache.catalina.deploy.ContextEnvironment");
		digester.addSetProperties(prefix + "Environment");
		digester.addSetNext(prefix + "Environment", "addEnvironment", "org.apache.catalina.deploy.ContextEnvironment");

		digester.addObjectCreate(prefix + "LocalEjb", "org.apache.catalina.deploy.ContextLocalEjb");
		digester.addSetProperties(prefix + "LocalEjb");
		digester.addSetNext(prefix + "LocalEjb", "addLocalEjb", "org.apache.catalina.deploy.ContextLocalEjb");

		digester.addObjectCreate(prefix + "Resource", "org.apache.catalina.deploy.ContextResource");
		digester.addSetProperties(prefix + "Resource");
		digester.addSetNext(prefix + "Resource", "addResource", "org.apache.catalina.deploy.ContextResource");

		digester.addCallMethod(prefix + "ResourceEnvRef", "addResourceEnvRef", 2);
		digester.addCallParam(prefix + "ResourceEnvRef/name", 0);
		digester.addCallParam(prefix + "ResourceEnvRef/type", 1);

		digester.addObjectCreate(prefix + "ResourceParams", "org.apache.catalina.deploy.ResourceParams");
		digester.addSetProperties(prefix + "ResourceParams");
		digester.addSetNext(prefix + "ResourceParams", "addResourceParams",
				"org.apache.catalina.deploy.ResourceParams");

		digester.addCallMethod(prefix + "ResourceParams/parameter", "addParameter", 2);
		digester.addCallParam(prefix + "ResourceParams/parameter/name", 0);
		digester.addCallParam(prefix + "ResourceParams/parameter/value", 1);

	}

}
