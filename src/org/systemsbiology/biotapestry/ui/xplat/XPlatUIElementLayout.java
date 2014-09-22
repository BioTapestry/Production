package org.systemsbiology.biotapestry.ui.xplat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class XPlatUIElementLayout {
	
	private UILayoutType type_;
	
	private Map<String,String> layoutParameters_;
	
	public XPlatUIElementLayout(UILayoutType type,Map<String,String> layoutParameters) {
		if(type == null) {
			throw new IllegalArgumentException("You must supply a valid UILayoutType for this layout");
		}
		this.type_ = type;
		this.layoutParameters_ = (layoutParameters == null ? new HashMap<String,String>() : layoutParameters);
	}

	public XPlatUIElementLayout(UILayoutType type) {
		this(type,null);
	}
	
	public void setLayoutParameter(String key, String value) {
		this.layoutParameters_.put(key,value);
	}
	
	public Map<String,String> getLayoutParameters() {
		return Collections.unmodifiableMap(this.layoutParameters_);
	}
	
	public UILayoutType getLayoutType() {
		return this.type_;
	}
	
	
	public enum UILayoutType {
		REGIONAL,
		GRID;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		XPlatUIElementLayout myRegionalLayout = new XPlatUIElementLayout(XPlatUIElementLayout.UILayoutType.REGIONAL);
		
		myRegionalLayout.setLayoutParameter("region", "top");
		myRegionalLayout.setLayoutParameter("ordinal", "0");
		
		XPlatUIElementLayout myGridLayout = new XPlatUIElementLayout(XPlatUIElementLayout.UILayoutType.GRID);
		
		myGridLayout.setLayoutParameter("row", "2");
		myGridLayout.setLayoutParameter("colstart", "0");
		myGridLayout.setLayoutParameter("colspan", "3");
	}
}
