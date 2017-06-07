/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
**                            Seattle, Washington, USA. 
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.systemsbiology.biotapestry.ui.xplat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.systemsbiology.biotapestry.ui.xplat.XPlatUIEvent.XPlatUIElementActionType;

import flexjson.JSONSerializer;
import flexjson.transformer.AbstractTransformer;

/**
 * An class to describe a UI element for display. This type of display
 * element does not contain other UI elements. It may optionally
 * specify a list of values (eg. if it is a dropdown list of some kind)
 * 
 *
 */

public class XPlatUIPrimitiveElement implements XPlatUIElement {
	// The type of element this is
  	private XPlatUIElementType elementType_;

  	// The parameters of this element, eg. its ID,
  	// any label it might need, default value, action, etc.
	private Map<String,Object> parameters_;
  
	// Optional members
	
	// If this is a selection or combo element, there will
	// be some values it lists initially
	// This might really need to be its own Object type
	// to handle how a key might be paired to a variety
	// of information (eg. <String,SelectionObject>)
	private Map<String,Object> availableValues_;
	
	// Events specific to this element
	private Map<String,XPlatUIEvent> events_;
	
	// If this element is intended to have a specific
	// position in a layout, this private member can
	// indicate that
	private XPlatUIElementLayout layout_;

	// If this element does not have a specific layout position,
	// this boolean indicates if the element should be allowed to
	// float with other elements in a layout.
	private boolean float_;
	
	// If this element's enabled state is linked to a condition,
	// this specifies the condition
	private XPlatUIValidity validity_;
	
	  
	public XPlatUIPrimitiveElement(
		XPlatUIElementType type,
		Map<String,Object> parameters,
		Map<String,Object> availVals,
		XPlatUIElementLayout layout,
		boolean shouldFloat
	) {
		if(type == null) {
			throw new IllegalArgumentException("You must specify an ElementType for this DialogElement");
		}
		this.elementType_ = type;
		  
		// At the very least, you must always specify an 'ID' for the element,
		// so parameters will never be null; however, you can put it in later
		this.parameters_ = (parameters == null ? new HashMap<String,Object>() : parameters);
		  
		this.availableValues_ = availVals;
		
		this.layout_ = layout;
		
		this.float_ = shouldFloat;
	}

	public XPlatUIPrimitiveElement(XPlatUIElementType type) {
		this(type,null,null,null,false);
	}

	public XPlatUIPrimitiveElement(XPlatUIElementType type,boolean shouldFloat) {
		this(type,null,null,null,shouldFloat);
	}
	
	
	public XPlatUIElementType getElementType() {
		return this.elementType_;
	}
	
	public Map<String, Object> getAvailableValues() {
		if(this.availableValues_ == null) {
			return null;
		}
		return Collections.unmodifiableMap(this.availableValues_);
	}
	
	public void addAvailVal(String key,Object value) {
		if(this.availableValues_ == null) {
			this.availableValues_ = new HashMap<String,Object>();
		}
		this.availableValues_.put(key,value);
	}
	  
	public void setParameter(String key, Object value) {
		if(this.parameters_ == null) 
			this.parameters_ = new HashMap<String,Object>();
		this.parameters_.put(key,value);
	}	
	
	public Map<String,Object> getParameters() {
		return Collections.unmodifiableMap(this.parameters_);
	}

	public Object getParameter(String key) {
		if(this.parameters_ != null)
			return this.parameters_.get(key);
		return null;
	}
	
	public void setEvent(String eventKey, XPlatUIEvent thisEvent) {
		if(this.events_ == null) {
			this.events_ = new HashMap<String,XPlatUIEvent>();
		}
		this.events_.put(eventKey,thisEvent);
	}
	
	public void addUiElementEventAction(String eventKey,XPlatUIElementActionType thisAction) {
		XPlatUIEvent thisEvent = this.events_.get(eventKey);
		if(thisEvent != null) {
			thisEvent.addUiElementAction(thisAction);
		}
	}
	
	public Map<String,XPlatUIEvent> getEvents() {
		if(this.events_ == null) {
			return null;
		}
		return Collections.unmodifiableMap(this.events_);
	}
	
	public XPlatUIEvent getEvent(String eventKey) {
		if(this.events_ == null) {
			return null;
		}
		return this.events_.get(eventKey);
	}

	public XPlatUIElementLayout getLayout() {
		return this.layout_;
	}
	
	public void setLayout(XPlatUIElementLayout layout) {
		this.layout_ = layout;
	}
	
	public boolean getFloat() {
		return this.float_;
	}
	
	public void setFloat(boolean shouldFloat) {
		this.float_ = shouldFloat;
	}
	
	public XPlatUIValidity getValidity() {
		return validity_;
	}

	public void setValidity(XPlatUIValidity validity_) {
		this.validity_ = validity_;
	}

	public void setValidity(String condition,String value) {
		this.validity_ = new XPlatUIValidity(condition,value);
	}	
	
	
	public static void main(String[] args) {
		try {
			XPlatUIPrimitiveElement textBox = new XPlatUIPrimitiveElement(XPlatUIElementType.TEXT_BOX_SINGLE);
			textBox.setParameter("label", "Name");
			textBox.setParameter("initialValue", "Please supply a gene name");
			XPlatUIElementLayout regLayoutTop = new XPlatUIElementLayout(XPlatUIElementLayout.UILayoutType.REGIONAL);
			regLayoutTop.setLayoutParameter("region", "top");
			textBox.setLayout(regLayoutTop);
			
			JSONSerializer serializer = new JSONSerializer(); 
			
			serializer.transform(textBox.new ExcludeNullsTransformer(), void.class);
			
			System.out.println(serializer.deepSerialize(textBox));
		} catch(Exception e) {
			
			System.err.println("[ERROR] In DialogElement: " + e);
			e.printStackTrace();
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