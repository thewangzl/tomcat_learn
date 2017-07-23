package ex15.pyrmont.digestertest;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.digester.Digester;

public class Test2 {

	public static void main(String[] args) {

		String path = System.getProperty("user.dir") + File.separator + "etc";
		File file = new  File(path, "employee2.xml");
		
		Digester digester = new Digester();
		
		digester.addObjectCreate("employee", Employee.class);
		digester.addSetProperties("employee");
		
		digester.addObjectCreate("employee/office", Office.class);
		digester.addSetProperties("employee/office");
		digester.addSetNext("employee/office", "addOffice");
		
		digester.addObjectCreate("employee/office/address", Address.class);
		digester.addSetProperties("employee/office/address");
		digester.addSetNext("employee/office/address", "setAddress");
		
		
		try {
			Employee employee = (Employee) digester.parse(file);
			ArrayList<Office> offices = employee.getOffices();
			System.out.println("--------------------------------");
			for (Office office : offices) {
				Address address = office.getAddress();
				System.out.println(office.getDescription());
				System.out.println("address:" + address);
			}
			System.out.println("-----------------------------------");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

