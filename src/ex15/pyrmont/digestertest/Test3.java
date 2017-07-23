package ex15.pyrmont.digestertest;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.digester.Digester;

public class Test3 {

	public static void main(String[] args) {

		String path = System.getProperty("user.dir") + File.separator + "etc";
		File file = new  File(path, "employee2.xml");
		
		Digester digester = new Digester();
		
		digester.addRuleSet(new EmployeeRuleSet());
		
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

