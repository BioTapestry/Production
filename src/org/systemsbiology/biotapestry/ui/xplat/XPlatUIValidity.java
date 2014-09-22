package org.systemsbiology.biotapestry.ui.xplat;

public class XPlatUIValidity {
	private String conditionName_;
	private String conditionValue_;
	
	public XPlatUIValidity(String name,String value) {
		this.conditionName_ = name;
		this.conditionValue_ = value;
	}
	
	public String getConditionName() {
		return this.conditionName_;
	}
	
	public String getConditionValue() {
		return this.conditionValue_;
	}
	
	public void setConditionName(String name) {
		this.conditionName_ = name;
	}

	public void setConditionValue(String value) {
		this.conditionValue_ = value;
	}
}
