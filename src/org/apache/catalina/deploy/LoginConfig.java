package org.apache.catalina.deploy;

public final class LoginConfig {

	private String authMethod;
	
	private String errorPage;
	
	private String loginPage;
	
	private String realmName;
	
	public LoginConfig() {
		 super();
	}

	public LoginConfig(String authMethod, String realmName, String loginPage, String errorPage) {
		super();
		this.authMethod = authMethod;
		this.realmName = realmName;
		this.loginPage = loginPage;
		this.errorPage = errorPage;
	}

	public String getAuthMethod() {
		return authMethod;
	}

	public void setAuthMethod(String authMethod) {
		this.authMethod = authMethod;
	}

	public String getErrorPage() {
		return errorPage;
	}

	public void setErrorPage(String errorPage) {
		this.errorPage = errorPage;
	}

	public String getLoginPage() {
		return loginPage;
	}

	public void setLoginPage(String loginPage) {
		this.loginPage = loginPage;
	}

	public String getRealmName() {
		return realmName;
	}

	public void setRealmName(String realmName) {
		this.realmName = realmName;
	}
	
	public String toString() {

        StringBuffer sb = new StringBuffer("LoginConfig[");
        sb.append("authMethod=");
        sb.append(authMethod);
        if (realmName != null) {
            sb.append(", realmName=");
            sb.append(realmName);
        }
        if (loginPage != null) {
            sb.append(", loginPage=");
            sb.append(loginPage);
        }
        if (errorPage != null) {
            sb.append(", errorPage=");
            sb.append(errorPage);
        }
        sb.append("]");
        return (sb.toString());

    }
	
	
}
