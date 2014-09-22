package org.systemsbiology.biotapestry.ui.xplat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import flexjson.transformer.AbstractTransformer;

public class XPlatUIEvent {
	
	// The name of the even (eg. change, click, blur)
	private String event_;
	
	// The action to take, eg. MAIN_ADD_GENE. 
	private String cmdAction_;
	
	// Optional
	
	// If this action takes parameters, a map of them can be
	// provided
	private Map<String,Object> parameters_;
	
	// If special UI-specific events occur in addition to the cmdAction, list them
	// here
	private List<XPlatUIElementActionType> uiElementActions_;
	
	// If UI element properties should be recorded at the time this event executes,
	// this will detail them
	private List<XPlatUIElementProp> uiElementProps_;
	
	
	public XPlatUIEvent(String event,String action) {
		this(event,action,null);

	}
	
	public XPlatUIEvent(String event,String action,Map<String,Object> params) {
		if(event == null || action == null) {
			throw new IllegalArgumentException("An action and an event must be provided!");
		}
		this.event_ = event;
		this.cmdAction_ = action;
		this.parameters_ = params;
	}

	public String getEvent() {
		return this.event_;
	}

	public void setEvent(String event_) {
		this.event_ = event_;
	}

	public String getCmdAction() {
		return this.cmdAction_;
	}

	public void setCmdAction(String action_) {
		this.cmdAction_ = action_;
	}
	
	public List<XPlatUIElementActionType> getUiElementActions() {
		if(this.uiElementActions_ == null) {
			return null;
		}
		return Collections.unmodifiableList(this.uiElementActions_);
	}

	public void setUiElementActions(List<XPlatUIElementActionType> uiElemActions) {
		this.uiElementActions_ = uiElemActions;
	}
	
	public void addUiElementAction(XPlatUIElementActionType thisAction) {
		if(this.uiElementActions_ == null) {
			this.uiElementActions_ = new ArrayList<XPlatUIElementActionType>();
		}
		
		this.uiElementActions_.add(thisAction);
	}

	public void addUiElementProp(XPlatUIElementProp thisProp) {
		if(this.uiElementProps_ == null) {
			this.uiElementProps_ = new ArrayList<XPlatUIElementProp>();
		}
		
		this.uiElementProps_.add(thisProp);
	}	
	
	public List<XPlatUIElementProp> getUiElementProps() {
		if(this.uiElementProps_ == null) {
			return null;
		}
		return Collections.unmodifiableList(this.uiElementProps_);
	}

	public Map<String, Object> getParameters() {
		if(this.parameters_ == null) {
			return null;
		}
		return Collections.unmodifiableMap(this.parameters_);
	}

	public void setParameters(Map<String, Object> parameters_) {
		this.parameters_ = parameters_;
	}	
	
	public void addParameter(String name,Object value) {
		if(this.parameters_ == null) {
			this.parameters_ = new HashMap<String,Object>();
		}
		
		this.parameters_.put(name,value);
	}
	
	
	public enum XPlatUIElementActionType {
		DIALOG_CLOSE,
		BLANK_FORM,
		HIDE_ELEMENT,
		GET_ELEMENT_PROPS,
		FILTER_ELEMENT
	}
	
	public static class XPlatUIElementProp {
		public String elementId;
		public String propertyId;
		public String subPropertyId;
		public String storeAs;
		public String bundleAs;
		
		public XPlatUIElementProp(String elementId,String propertyId,String subPropertyId,String storeAs,String bundleAs) {
			this.elementId = elementId;
			this.propertyId = propertyId;
			this.subPropertyId = subPropertyId;
			this.storeAs = storeAs;
			this.bundleAs = bundleAs;
		}
		
	}
	
	public class ExcludeNullsTransformer extends AbstractTransformer {
	    @Override
	    public Boolean isInline() {
			return true;
	    }

	    public void transform(Object object) {
			// Do nothing, null objects are not serialized.
			return;
		}
	}	

}
