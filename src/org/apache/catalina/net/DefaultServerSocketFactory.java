package org.apache.catalina.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * Default server socket factory, which returns unadorned server sockets.
 * @author thewangzl
 *
 */
public class DefaultServerSocketFactory implements ServerSocketFactory {

	@Override
	public ServerSocket createSocket(int port) throws IOException, KeyStoreException, NoSuchAlgorithmException,
			CertificateException, UnrecoverableKeyException, KeyManagementException {
	
		return new ServerSocket(port);
	}

	@Override
	public ServerSocket createSocket(int port, int backlog) throws IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException {

		return new ServerSocket(port, backlog);
	}

	@Override
	public ServerSocket createSocket(int port, int backlog, InetAddress ifAddress)
			throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
			UnrecoverableKeyException, KeyManagementException {
	
		return new ServerSocket(port, backlog, ifAddress);
	}

}
