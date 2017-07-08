package org.apache.catalina.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public interface ServerSocketFactory {

	/**
	 * 
	 * @param port
	 * @return
	 * @throws IOException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 */
	public ServerSocket createSocket(int port) throws IOException, KeyStoreException, NoSuchAlgorithmException,
			CertificateException, UnrecoverableKeyException, KeyManagementException;

	/**
	 * 
	 * @param port
	 * @param backlog
	 * @return
	 * @throws IOException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 */
	public ServerSocket createSocket(int port, int backlog) throws IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException;

	/**
	 * 
	 * @param port
	 * @param backlog
	 * @param ifAddress
	 * @return
	 * @throws IOException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 */
	public ServerSocket createSocket(int port, int backlog, InetAddress ifAddress)
			throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
			UnrecoverableKeyException, KeyManagementException;

}
