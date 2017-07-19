package org.apache.catalina.session;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.servlet.ServletContext;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.catalina.util.CustomObjectInputStream;

public final class FileStore extends StoreBase implements Store {

	private static final String FILE_EXT = ".session";

	private String directory = ".";

	private File directoryFile;

	private static final String info = "FileStore/1.0";

	private static final String storeName = "fileStore";

	private static final String threadName = "FileStore";

	// ----------------------------------------Properties

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String path) {
		String oldDirectory = this.directory;
		this.directory = path;
		this.directoryFile = null;
		support.firePropertyChange("directory", oldDirectory, this.directory);
	}

	@Override
	public String getInfo() {
		return info;
	}

	public String getThreadName() {
		return threadName;
	}

	public static String getStorename() {
		return storeName;
	}

	// -------------------------------------- Public Methods

	@Override
	public int getSize() throws IOException {

		// Acquire the list of files in our storage directory
		File file = directory();
		if (file == null) {
			return 0;
		}
		String[] files = file.list();

		// Figure out which files are sessions
		int keycount = 0;
		for (int i = 0; i < files.length; i++) {
			if (files[i].endsWith(FILE_EXT)) {
				keycount++;
			}
		}
		return keycount;
	}

	@Override
	public void clear() throws IOException {
		String[] keys = keys();
		for (int i = 0; i < keys.length; i++) {
			remove(keys[i]);
		}
	}

	@Override
	public String[] keys() throws IOException {
		// Acquire the list of files in our storage directory
		File file = directory();
		if (file == null) {
			return new String[0];
		}
		String[] files = file.list();

		// Build and return the list of session identifiers
		ArrayList<String> list = new ArrayList<>();
		int n = FILE_EXT.length();
		for (int i = 0; i < files.length; i++) {
			if (files[i].endsWith(FILE_EXT)) {
				list.add(files[i].substring(0, files[i].length() - n));
			}
		}
		return list.toArray(new String[list.size()]);
	}

	@Override
	public Session load(String id) throws ClassNotFoundException, IOException {
		File file = file(id);
		if (file == null) {
			return null;
		}
		if (debug >= 1) {
			log(sm.getString(getStoreName() + ".loading", id, file.getAbsolutePath()));
		}

		FileInputStream fis = null;
		ObjectInputStream ois = null;
		Loader loader = null;
		ClassLoader classLoader = null;
		try {
			fis = new FileInputStream(file.getAbsolutePath());
			BufferedInputStream bis = new BufferedInputStream(fis);
			Container container = manager.getContainer();
			if (container != null) {
				loader = container.getLoader();
			}
			if (loader != null) {
				classLoader = loader.getClassLoader();
			}
			if (classLoader != null) {
				ois = new CustomObjectInputStream(bis, classLoader);
			} else {
				ois = new ObjectInputStream(bis);
			}
		} catch (FileNotFoundException e) {
			if (debug >= 1)
				log("No persisted data file found");
			return (null);
		} catch (IOException e) {
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException f) {
					;
				}
				ois = null;
			}
			throw e;
		}

		try {
			StandardSession session = (StandardSession) manager.createSession();
			session.readObjectData(ois);
			session.setManager(manager);
			return session;
		} finally {
			// Close the input stream
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException f) {
					;
				}
			}
		}
	}

	@Override
	public void save(Session session) throws IOException {
		File file = file(session.getId());
		if (file == null) {
			return;
		}
		if (debug >= 1) {
			log(sm.getString(getStoreName() + ".saving", session.getId(), file.getAbsolutePath()));
		}
		
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = new FileOutputStream(file.getAbsolutePath());
			oos = new ObjectOutputStream(new BufferedOutputStream(fos));
		} catch (IOException e) {
			if (oos != null) {
                try {
                    oos.close();
                } catch (IOException f) {
                    ;
                }
            }
            throw e;
		}
		
		try {
			((StandardSession) session).writeObjectData(oos);
		} finally {
			oos.close();
		}
	}

	@Override
	public void remove(String id) throws IOException {

		File file = file(id);
		if (file == null) {
			return;
		}
		if (debug >= 1) {
			log(sm.getString(getStoreName() + ".removing", id, file.getAbsolutePath()));
		}
		file.delete();
	}

	// ---------------------------------- Private Methods

	private File directory() {
		if (this.directory == null) {
			return null;
		}
		if (this.directoryFile != null) {
			return this.directoryFile;
		}
		File file = new File(this.directory);
		if (!file.isAbsolute()) {
			Container container = manager.getContainer();
			if (container instanceof Context) {
				ServletContext servletContext = ((Context) container).getServletContext();
				File work = (File) servletContext.getAttribute(Globals.WORK_DIR_ATTR);
				file = new File(work, this.directory);
			} else {
				throw new IllegalArgumentException("Parent Container is not a Context");
			}
		}
		if (!file.exists() || !file.isDirectory()) {
			file.delete();
			file.mkdirs();
		}
		this.directoryFile = file;
		return file;
	}

	private File file(String id) {
		if (this.directory == null) {
			return null;
		}
		String filename = id + FILE_EXT;
		File file = new File(directory(), filename);
		return file;
	}

}
