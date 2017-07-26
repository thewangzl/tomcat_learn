package ex16.pyrmont.shutdownhook;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;

@SuppressWarnings("serial")
public class MySwingApp extends JFrame {

	JButton exitButton = new JButton();
	JTextArea jTextArea1 = new JTextArea();
	String dir = System.getProperty("user.dir");
	String filename = "temp.txt";

	public MySwingApp() {
		exitButton.setText("Exit");
		exitButton.setBounds(new Rectangle(304, 248, 76, 37));
		exitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exitButton_actionPerformed(e);
			}
		});
		this.getContentPane().setLayout(null);
		jTextArea1.setText("Click the Exit button to exit");
		jTextArea1.setBounds(new Rectangle(9, 7, 371, 235));
		this.getContentPane().add(exitButton, null);
		this.getContentPane().add(jTextArea1, null);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setBounds(0, 0, 400, 330);
		this.setVisible(true);
		initialize();
	}

	private void initialize() {
		File file = new File(dir, filename);
		System.out.println("Creating temporary file");
		try {
			file.createNewFile();
		} catch (IOException e) {
			System.out.println("Failed creating temporary file.");
		}
	}
	
	void exitButton_actionPerformed(ActionEvent e){
		shutdown();
		System.exit(0);
	}

	private void shutdown() {
		File file = new File(dir, filename);
		if (file.exists()) {
			System.out.println("Deleting temporary file.");
			file.delete();
		}
	}

	public static void main(String[] args) {
		MySwingApp app = new MySwingApp();
	}

}
