package org.apache.catalina.startup;

import java.lang.reflect.Method;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.RuleSetBase;
import org.xml.sax.Attributes;

public class WebRuleSet extends RuleSetBase {

	private String prefix;

	public WebRuleSet() {
		this("");
	}

	public WebRuleSet(String prefix) {
		super();
		this.namespaceURI = null;
		this.prefix = prefix;

	}

	@Override
	public void addRuleInstances(Digester digester) {
		digester.addRule(prefix + "web-app", new SetPublicIdRule(digester, "setPublicId"));
		digester.addCallMethod(prefix + "web-app/context-param", "addParameter", 2);
		digester.addCallParam(prefix + "web-app/context-param/param-name", 0);
		digester.addCallParam(prefix + "web-app/context-param/param-value", 1);

		digester.addCallMethod(prefix + "web-app/display-name", "setDisplayName", 0);

		digester.addRule(prefix + "web-app/distributable", new SetDistributableRule(digester));

		digester.addObjectCreate(prefix + "web-app/ejb-local-ref", "org.apache.catalina.deploy.ContextLocalEjb");
		digester.addSetNext(prefix + "web-app/ejb-local-ref", "addLocalEjb",
				"org.apache.catalina.deploy.ContextLocalEjb");

		digester.addCallMethod(prefix + "web-app/ejb-local-ref/description", "setDescription", 0);
		digester.addCallMethod(prefix + "web-app/ejb-local-ref/ejb-link", "setLink", 0);
		digester.addCallMethod(prefix + "web-app/ejb-local-ref/ejb-ref-name", "setName", 0);
		digester.addCallMethod(prefix + "web-app/ejb-local-ref/ejb-ref-type", "setType", 0);
		digester.addCallMethod(prefix + "web-app/ejb-local-ref/local", "setLocal", 0);
		digester.addCallMethod(prefix + "web-app/ejb-local-ref/local-home", "setHome", 0);

		digester.addObjectCreate(prefix + "web-app/ejb-ref", "org.apache.catalina.deploy.ContextEjb");
		digester.addSetNext(prefix + "web-app/ejb-ref", "addEjb", "org.apache.catalina.deploy.ContextEjb");

		digester.addCallMethod(prefix + "web-app/ejb-ref/description", "setDescription", 0);
		digester.addCallMethod(prefix + "web-app/ejb-ref/ejb-link", "setLink", 0);
		digester.addCallMethod(prefix + "web-app/ejb-ref/ejb-ref-name", "setName", 0);
		digester.addCallMethod(prefix + "web-app/ejb-ref/ejb-ref-type", "setType", 0);
		digester.addCallMethod(prefix + "web-app/ejb-ref/home", "setHome", 0);
		digester.addCallMethod(prefix + "web-app/ejb-ref/remote", "setRemote", 0);

		digester.addObjectCreate(prefix + "web-app/env-entry", "org.apache.catalina.deploy.ContextEnvironment");
		digester.addSetNext(prefix + "web-app/env-entry", "addEnvironment",
				"org.apache.catalina.deploy.ContextEnvironment");

		digester.addCallMethod(prefix + "web-app/env-entry/description", "setDescription", 0);
		digester.addCallMethod(prefix + "web-app/env-entry/env-entry-name", "setName", 0);
		digester.addCallMethod(prefix + "web-app/env-entry/env-entry-type", "setType", 0);
		digester.addCallMethod(prefix + "web-app/env-entry/env-entry-value", "setValue", 0);

		digester.addObjectCreate(prefix + "web-app/error-page", "org.apache.catalina.deploy.ErrorPage");
		digester.addSetNext(prefix + "web-app/error-page", "addErrorPage", "org.apache.catalina.deploy.ErrorPage");

		digester.addCallMethod(prefix + "web-app/error-page/error-code", "setErrorCode", 0);
		digester.addCallMethod(prefix + "web-app/error-page/exception-type", "setExceptionType", 0);
		digester.addCallMethod(prefix + "web-app/error-page/location", "setLocation", 0);

		digester.addObjectCreate(prefix + "web-app/filter", "org.apache.catalina.deploy.FilterDef");
		digester.addSetNext(prefix + "web-app/filter", "addFilterDef", "org.apache.catalina.deploy.FilterDef");
		digester.addCallMethod(prefix + "web-app/filter/description", "setDescription", 0);
		digester.addCallMethod(prefix + "web-app/filter/display-name", "setDisplayName", 0);
		digester.addCallMethod(prefix + "web-app/filter/filter-class", "setFilterClass", 0);
		digester.addCallMethod(prefix + "web-app/filter/filter-name", "setFilterName", 0);
		digester.addCallMethod(prefix + "web-app/filter/large-icon", "setLargeIcon", 0);
		digester.addCallMethod(prefix + "web-app/filter/small-icon", "setSmallIcon", 0);

		digester.addCallMethod(prefix + "web-app/filter/init-param", "addInitParameter", 2);
		digester.addCallParam(prefix + "web-app/filter/init-param/param-name", 0);
		digester.addCallParam(prefix + "web-app/filter/init-param/param-value", 1);

		digester.addObjectCreate(prefix + "web-app/filter-mapping", "org.apache.catalina.deploy.FilterMap");
		digester.addSetNext(prefix + "web-app/filter-mapping", "addFilterMap", "org.apache.catalina.deploy.FilterMap");

		digester.addCallMethod(prefix + "web-app/filter-mapping/filter-name", "setFilterName", 0);
		digester.addCallMethod(prefix + "web-app/filter-mapping/servlet-name", "setServletName", 0);
		digester.addCallMethod(prefix + "web-app/filter-mapping/url-pattern", "setURLPattern", 0);

		digester.addCallMethod(prefix + "web-app/listener/listener-class", "addApplicationListener", 0);

		digester.addObjectCreate(prefix + "web-app/login-config", "org.apache.catalina.deploy.LoginConfig");
		digester.addSetNext(prefix + "web-app/login-config", "setLoginConfig",
				"org.apache.catalina.deploy.LoginConfig");

		digester.addCallMethod(prefix + "web-app/login-config/auth-method", "setAuthMethod", 0);
		digester.addCallMethod(prefix + "web-app/login-config/realm-name", "setRealmName", 0);
		digester.addCallMethod(prefix + "web-app/login-config/form-login-config/form-error-page", "setErrorPage", 0);
		digester.addCallMethod(prefix + "web-app/login-config/form-login-config/form-login-page", "setLoginPage", 0);

		digester.addCallMethod(prefix + "web-app/mime-mapping", "addMimeMapping", 2);
		digester.addCallParam(prefix + "web-app/mime-mapping/extension", 0);
		digester.addCallParam(prefix + "web-app/mime-mapping/mime-type", 1);

		digester.addCallMethod(prefix + "web-app/resource-env-ref", "addResourceEnvRef", 2);
		digester.addCallParam(prefix + "web-app/resource-env-ref/resource-env-ref-name", 0);
		digester.addCallParam(prefix + "web-app/resource-env-ref/resource-env-ref-type", 1);

		digester.addObjectCreate(prefix + "web-app/resource-ref", "org.apache.catalina.deploy.ContextResource");
		digester.addSetNext(prefix + "web-app/resource-ref", "addResource",
				"org.apache.catalina.deploy.ContextResource");

		digester.addCallMethod(prefix + "web-app/resource-ref/description", "setDescription", 0);
		digester.addCallMethod(prefix + "web-app/resource-ref/res-auth", "setAuth", 0);
		digester.addCallMethod(prefix + "web-app/resource-ref/res-ref-name", "setName", 0);
		digester.addCallMethod(prefix + "web-app/resource-ref/res-sharing-scope", "setScope", 0);
		digester.addCallMethod(prefix + "web-app/resource-ref/res-type", "setType", 0);

		digester.addObjectCreate(prefix + "web-app/security-constraint",
				"org.apache.catalina.deploy.SecurityConstraint");
		digester.addSetNext(prefix + "web-app/security-constraint", "addConstraint",
				"org.apache.catalina.deploy.SecurityConstraint");

		digester.addRule(prefix + "web-app/security-constraint/auth-constraint", new SetAuthConstraintRule(digester));
		digester.addCallMethod(prefix + "web-app/security-constraint/auth-constraint/role-name", "addAuthRole", 0);
		digester.addCallMethod(prefix + "web-app/security-constraint/display-name", "setDisplayName", 0);
		digester.addCallMethod(prefix + "web-app/security-constraint/user-data-constraint/transport-guarantee",
				"setUserConstraint", 0);

		digester.addObjectCreate(prefix + "web-app/security-constraint/web-resource-collection",
				"org.apache.catalina.deploy.SecurityCollection");
		digester.addSetNext(prefix + "web-app/security-constraint/web-resource-collection", "addCollection",
				"org.apache.catalina.deploy.SecurityCollection");
		digester.addCallMethod(prefix + "web-app/security-constraint/web-resource-collection/http-method", "addMethod",
				0);
		digester.addCallMethod(prefix + "web-app/security-constraint/web-resource-collection/url-pattern", "addPattern",
				0);
		digester.addCallMethod(prefix + "web-app/security-constraint/web-resource-collection/web-resource-name",
				"setName", 0);

		digester.addCallMethod(prefix + "web-app/security-role/role-name", "addSecurityRole", 0);

		digester.addRule(prefix + "web-app/servlet", new WrapperCreateRule(digester));
		digester.addSetNext(prefix + "web-app/servlet", "addChild", "org.apache.catalina.Container");

		digester.addCallMethod(prefix + "web-app/servlet/init-param", "addInitParameter", 2);
		digester.addCallParam(prefix + "web-app/servlet/init-param/param-name", 0);
		digester.addCallParam(prefix + "web-app/servlet/init-param/param-value", 1);

		digester.addCallMethod(prefix + "web-app/servlet/jsp-file", "setJspFile", 0);
		digester.addCallMethod(prefix + "web-app/servlet/load-on-startup", "setLoadOnStartupString", 0);
		digester.addCallMethod(prefix + "web-app/servlet/run-as/role-name", "setRunAs", 0);

		digester.addCallMethod(prefix + "web-app/servlet/security-role-ref", "addSecurityReference", 2);
		digester.addCallParam(prefix + "web-app/servlet/security-role-ref/role-link", 1);
		digester.addCallParam(prefix + "web-app/servlet/security-role-ref/role-name", 0);

		digester.addCallMethod(prefix + "web-app/servlet/servlet-class", "setServletClass", 0);
		digester.addCallMethod(prefix + "web-app/servlet/servlet-name", "setName", 0);

		digester.addCallMethod(prefix + "web-app/servlet-mapping", "addServletMapping", 2);
		digester.addCallParam(prefix + "web-app/servlet-mapping/servlet-name", 1);
		digester.addCallParam(prefix + "web-app/servlet-mapping/url-pattern", 0);

		digester.addCallMethod(prefix + "web-app/session-config/session-timeout", "setSessionTimeout", 1,
				new Class[] { Integer.TYPE });
		digester.addCallParam(prefix + "web-app/session-config/session-timeout", 0);

		digester.addCallMethod(prefix + "web-app/taglib", "addTaglib", 2);
		digester.addCallParam(prefix + "web-app/taglib/taglib-location", 1);
		digester.addCallParam(prefix + "web-app/taglib/taglib-uri", 0);

		digester.addCallMethod(prefix + "web-app/welcome-file-list/welcome-file", "addWelcomeFile", 0);
	}
}

final class SetPublicIdRule extends Rule {

	private String method;

	@SuppressWarnings("deprecation")
	public SetPublicIdRule(Digester digester, String method) {
		super(digester);
		this.method = method;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void begin(Attributes attributes) throws Exception {
		// Context context = (Context) digester.peek(digester.getCount() - 1);
		Object top = digester.peek();
		Class<?> paramClasses[] = new Class[1];
		paramClasses[0] = "String".getClass();
		String paramValues[] = new String[1];
		paramValues[0] = digester.getPublicId();

		Method m = null;
		try {
			m = top.getClass().getMethod(method, paramClasses);
		} catch (NoSuchMethodException e) {
			digester.log("Can't find method " + method + " in " + top + " CLASS " + top.getClass());
			return;
		}

		m.invoke(top, paramValues);
		if (digester.getDebug() >= 1)
			digester.log("" + top.getClass().getName() + "." + method + "(" + paramValues[0] + ")");
	}
}

final class SetDistributableRule extends Rule {

	public SetDistributableRule(Digester digester) {
		super(digester);
	}

	@Override
	public void begin(Attributes attributes) throws Exception {

		Context context = (Context) digester.peek();
		context.setDistributable(true);
		if (digester.getDebug() > 0)
			digester.log(context.getClass().getName() + ".setDistributable( true)");
	}
}

final class SetAuthConstraintRule extends Rule {

	public SetAuthConstraintRule(Digester digester) {
		super(digester);
	}

	public void begin(Attributes attributes) throws Exception {
		SecurityConstraint securityConstraint = (SecurityConstraint) digester.peek();
		securityConstraint.setAuthConstraint(true);
		if (digester.getDebug() > 0)
			digester.log("Calling SecurityConstraint.setAuthConstraint(true)");
	}
}

final class WrapperCreateRule extends Rule {

	public WrapperCreateRule(Digester digester) {
		super(digester);
	}

	@Override
	public void begin(Attributes attributes) throws Exception {
		Context context = (Context) digester.peek(digester.getCount() - 1);
		Wrapper wrapper = context.createWrapper();
		digester.push(wrapper);
		if (digester.getDebug() > 0)
			digester.log("new " + wrapper.getClass().getName());
	}

	@Override
	public void end() throws Exception {
		Wrapper wrapper = (Wrapper) digester.pop();
		if (digester.getDebug() > 0)
			digester.log("pop " + wrapper.getClass().getName());
	}
}