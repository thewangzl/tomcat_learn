package org.apache.catalina.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Deployer;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.startup.ContextRuleSet;
import org.apache.catalina.startup.NamingRuleSet;
import org.apache.catalina.util.StringManager;
import org.apache.commons.digester.Digester;

public class StandardHostDeployer implements Deployer {

	private Context context = null;

	private Digester digester = null;

	protected StandardHost host = null;

	private ContextRuleSet contextRuleSet = null;

	private NamingRuleSet namingRuleSet = null;

	private String overrideDocBase = null;

	protected static StringManager sm = StringManager.getManager(Constants.Package);

	public StandardHostDeployer(StandardHost host) {
		super();
		this.host = host;
	}

	@Override
	public String getName() {
		return host.getName();
	}

	@Override
	public synchronized void install(String contextPath, URL war) throws IOException {
		// Validate the format and state of our arguments
		if (contextPath == null)
			throw new IllegalArgumentException(sm.getString("standardHost.pathRequired"));
		if (!contextPath.equals("") && !contextPath.startsWith("/"))
			throw new IllegalArgumentException(sm.getString("standardHost.pathFormat", contextPath));
		if (findDeployedApp(contextPath) != null)
			throw new IllegalStateException(sm.getString("standardHost.pathUsed", contextPath));
		if (war == null)
			throw new IllegalArgumentException(sm.getString("standardHost.warRequired"));

		// Calculate the document base for the new web application
		host.log(sm.getString("standardHost.installing", contextPath, war.toString()));
		String url = war.toString();
		String docBase = null;
		if (url.startsWith("jar:")) {
			url = url.substring(4, url.length() - 2);
		}
		if (url.startsWith("file://"))
			docBase = url.substring(7);
		else if (url.startsWith("file:"))
			docBase = url.substring(5);
		else
			throw new IllegalArgumentException(sm.getString("standardHost.warURL", url));

		// Install the new web application
		try {
			Class<?> clazz = Class.forName(host.getContextClass());
			Context context = (Context) clazz.newInstance();
			context.setPath(contextPath);

			context.setDocBase(docBase);
			if (context instanceof Lifecycle) {
				clazz = Class.forName(host.getConfigClass());
				LifecycleListener listener = (LifecycleListener) clazz.newInstance();
				((Lifecycle) context).addLifecycleListener(listener);
			}
			host.fireContainerEvent(PRE_INSTALL_EVENT, context);
			host.addChild(context);
			host.fireContainerEvent(INSTALL_EVENT, context);
		} catch (Exception e) {
			host.log(sm.getString("standardHost.installError", contextPath), e);
			throw new IOException(e.toString());
		}
	}

	@Override
	public synchronized void install(URL config, URL war) throws IOException {
		// Validate the format and state of our arguments
		if (config == null)
			throw new IllegalArgumentException(sm.getString("standardHost.configRequired"));

		if (!host.isDeployXML())
			throw new IllegalArgumentException(sm.getString("standardHost.configNotAllowed"));

		// Calculate the document base for the new web application (if needed)
		String docBase = null; // Optional override for value in config file
		if (war != null) {
			String url = war.toString();
			host.log(sm.getString("standardHost.installingWAR", url));
			// Calculate the WAR file absolute pathname
			if (url.startsWith("jar:")) {
				url = url.substring(4, url.length() - 2);
			}
			if (url.startsWith("file://"))
				docBase = url.substring(7);
			else if (url.startsWith("file:"))
				docBase = url.substring(5);
			else
				throw new IllegalArgumentException(sm.getString("standardHost.warURL", url));

		}

		// Install the new web application
		this.context = null;
		this.overrideDocBase = docBase;
		InputStream stream = null;
		try {
			stream = config.openStream();
			Digester digester = createDigester();
			digester.setDebug(host.getDebug());
			digester.clear();
			digester.push(this);
			digester.parse(stream);
			stream.close();
			stream = null;
		} catch (Exception e) {
			host.log(sm.getString("standardHost.installError", docBase), e);
			throw new IOException(e.toString());
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (Throwable t) {
					;
				}
			}
		}
	}

	@Override
	public Context findDeployedApp(String contextPath) {
		return ((Context) host.findChild(contextPath));
	}

	@Override
	public String[] findDeployedApps() {
		Container children[] = host.findChildren();
		String results[] = new String[children.length];
		for (int i = 0; i < children.length; i++)
			results[i] = children[i].getName();
		return (results);
	}

	@Override
	public void remove(String contextPath) throws IOException {
		// Validate the format and state of our arguments
		if (contextPath == null)
			throw new IllegalArgumentException(sm.getString("standardHost.pathRequired"));
		if (!contextPath.equals("") && !contextPath.startsWith("/"))
			throw new IllegalArgumentException(sm.getString("standardHost.pathFormat", contextPath));

		// Locate the context and associated work directory
		Context context = findDeployedApp(contextPath);
		if (context == null)
			throw new IllegalArgumentException(sm.getString("standardHost.pathMissing", contextPath));

		// Remove this web application
		host.log(sm.getString("standardHost.removing", contextPath));
		try {
			host.removeChild(context);
			host.fireContainerEvent(REMOVE_EVENT, context);
		} catch (Exception e) {
			host.log(sm.getString("standardHost.removeError", contextPath), e);
			throw new IOException(e.toString());
		}
	}

	@Override
	public void start(String contextPath) throws IOException {
		// Validate the format and state of our arguments
		if (contextPath == null)
			throw new IllegalArgumentException(sm.getString("standardHost.pathRequired"));
		if (!contextPath.equals("") && !contextPath.startsWith("/"))
			throw new IllegalArgumentException(sm.getString("standardHost.pathFormat", contextPath));
		Context context = findDeployedApp(contextPath);
		if (context == null)
			throw new IllegalArgumentException(sm.getString("standardHost.pathMissing", contextPath));
		host.log("standardHost.start " + contextPath);
		try {
			((Lifecycle) context).start();
		} catch (LifecycleException e) {
			host.log("standardHost.start " + contextPath + ": ", e);
			throw new IllegalStateException("standardHost.start " + contextPath + ": " + e);
		}
	}

	@Override
	public void stop(String contextPath) throws IOException {
		// Validate the format and state of our arguments
		if (contextPath == null)
			throw new IllegalArgumentException(sm.getString("standardHost.pathRequired"));
		if (!contextPath.equals("") && !contextPath.startsWith("/"))
			throw new IllegalArgumentException(sm.getString("standardHost.pathFormat", contextPath));
		Context context = findDeployedApp(contextPath);
		if (context == null)
			throw new IllegalArgumentException(sm.getString("standardHost.pathMissing", contextPath));
		host.log("standardHost.stop " + contextPath);
		try {
			((Lifecycle) context).stop();
		} catch (LifecycleException e) {
			host.log("standardHost.stop " + contextPath + ": ", e);
			throw new IllegalStateException("standardHost.stop " + contextPath + ": " + e);
		}
	}

	// ------------------------------------------------------ Delegated Methods
	
	public void addChild(Container child) {

		context = (Context) child;
		String contextPath = context.getPath();
		if (contextPath == null)
			throw new IllegalArgumentException(sm.getString("standardHost.pathRequired"));
		else if (!contextPath.equals("") && !contextPath.startsWith("/"))
			throw new IllegalArgumentException(sm.getString("standardHost.pathFormat", contextPath));
		if (host.findChild(contextPath) != null)
			throw new IllegalStateException(sm.getString("standardHost.pathUsed", contextPath));
		if (this.overrideDocBase != null)
			context.setDocBase(this.overrideDocBase);
		host.fireContainerEvent(PRE_INSTALL_EVENT, context);
		host.addChild(child);
		host.fireContainerEvent(INSTALL_EVENT, context);

	}
	
	public ClassLoader getParentClassLoader() {

        return (host.getParentClassLoader());

    }
	
	// ------------------------------------------------------ Protected Methods

	protected Digester createDigester() {

		if (digester == null) {
			digester = new Digester();
			if (host.getDebug() > 0)
				digester.setDebug(3);
			digester.setValidating(false);
			contextRuleSet = new ContextRuleSet("");
			digester.addRuleSet(contextRuleSet);
			namingRuleSet = new NamingRuleSet("Context/");
			digester.addRuleSet(namingRuleSet);
		}
		return (digester);

	}

}
