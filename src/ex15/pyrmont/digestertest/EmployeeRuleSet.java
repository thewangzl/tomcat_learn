package ex15.pyrmont.digestertest;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;

public class EmployeeRuleSet extends RuleSetBase {

	@Override
	public void addRuleInstances(Digester digester) {
		digester.addObjectCreate("employee", Employee.class);
		digester.addSetProperties("employee");
		
		digester.addObjectCreate("employee/office", Office.class);
		digester.addSetProperties("employee/office");
		digester.addSetNext("employee/office", "addOffice");
		
		digester.addObjectCreate("employee/office/address", Address.class);
		digester.addSetProperties("employee/office/address");
		digester.addSetNext("employee/office/address", "setAddress");
	}

}
