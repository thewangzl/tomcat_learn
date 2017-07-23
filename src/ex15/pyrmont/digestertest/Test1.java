package ex15.pyrmont.digestertest;

import java.io.File;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class Test1 {

	public static void main(String[] args) {

		String path = System.getProperty("user.dir") + File.separator + "etc";
		File file = new  File(path, "employee1.xml");
		
		Digester digester = new Digester();
		digester.addObjectCreate("employee", Employee.class);
		digester.addSetProperties("employee");
		digester.addCallMethod("employee", "printName");
		
		try {
			Employee employee = (Employee) digester.parse(file);
			employee.printName();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

