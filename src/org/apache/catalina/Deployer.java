package org.apache.catalina;

import java.io.IOException;
import java.net.URL;

public interface Deployer {

	// ----------------------------------------------------- Manifest Constants

	/**
	 * The ContainerEvent event type sent when a new application is being
	 * installed by <code>install()</code>, before it has been started.
	 */
	public static final String PRE_INSTALL_EVENT = "pre-install";

	/**
	 * The ContainerEvent event type sent when a new application is installed by
	 * <code>install()</code>, after it has been started.
	 */
	public static final String INSTALL_EVENT = "install";

	/**
	 * The ContainerEvent event type sent when an existing application is
	 * removed by <code>remove()</code>.
	 */
	public static final String REMOVE_EVENT = "remove";

	public String getName();

	public void install(String contextPath, URL war) throws IOException;

	public void install(URL config, URL war) throws IOException;

	public Context findDeployedApp(String contextPath);

	public String[] findDeployedApps();

	public void remove(String contextPath) throws IOException;

	public void start(String contextPath) throws IOException;

	public void stop(String contextPath) throws IOException;

}
