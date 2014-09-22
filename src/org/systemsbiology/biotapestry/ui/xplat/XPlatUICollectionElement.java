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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import flexjson.JSONSerializer;
import flexjson.transformer.AbstractTransformer;


/**
 * A class to describe a UI element which collects other UI elements or other
 * UI element collections. This can be used to describe a complex display
 * element such as a tab controller.
 * 
 *
 */
public class XPlatUICollectionElement implements XPlatUIElement {
		
	private XPlatUIElementType elementType_;

  	// The parameters of this element, eg. its name,
  	// any label it might need, default value, action, etc.
	private Map<String,Object> parameters_;
	
	// A Map of the elements this collection holds
	private Map<String,List<XPlatUIElement>> collectionElements_;
	
	// Optional 
	
	// Event specific to this collecting element
	private Map<String,Map<String,String>> events_;
	
	// Parameters specific to the collection's element groups
	private Map<String,Map<String,Object>> groupParams_;
	
	// Ordering ordinal for groups of elements
	private Map<String,Integer> groupOrder_;
	
	// If a specific group should be 'selected' on load, name it here
	private String selected_;
	
	// Layout information
	private XPlatUIElementLayout layout_;
	
	// Should this containing element float
	private boolean float_;
		
	public XPlatUICollectionElement(
		XPlatUIElementType elementType,
		Map<String,List<XPlatUIElement>> collectionElements,
		Map<String,Object> parameters,
		Map<String,Map<String,String>> events,
		XPlatUIElementLayout layout,
		boolean shouldFloat
	) {
		if(elementType == null) {
			throw new IllegalArgumentException(
				"A Dialog Element Collection must have a specified type"
			);
		}
		this.elementType_ = elementType;
		
		this.collectionElements_ = (collectionElements == null ? new HashMap<String,List<XPlatUIElement>>() : collectionElements);
		
		// At the very least, you must always specify a 'name' for the element,
		// so parameters will never be null; however, you can put it in later
		this.parameters_ = (parameters == null ? new HashMap<String,Object>() : parameters);
		
		this.events_ = events;
		this.layout_ = layout;
		this.float_ = shouldFloat;
	}
	
	public XPlatUICollectionElement(XPlatUIElementType elementType) {
		this(elementType,null,null,null,null,false);
	}
	
	public Map<String, Object> getParameters() {
		return Collections.unmodifiableMap(this.parameters_);
	}
	
	public void setParameter(String key, Object value) {
		this.parameters_.put(key,value);
	}
	
	public Object getParameter(String key) {
		if(this.parameters_ != null) {
			return this.parameters_.get(key);
		}
		return null;
	}
	
	public void setEvent(String eventKey, String eventParamKey, String eventParamValue) {
		if(this.events_ == null) {
			this.events_ = new HashMap<String,Map<String,String>>();
		}
		Map<String,String> thisEvent = this.events_.get(eventKey);
		if(thisEvent == null) {
			thisEvent = new HashMap<String,String>();
			this.events_.put(eventKey,thisEvent);
		}
		thisEvent.put(eventParamKey,eventParamValue);
	}	
	
	public Map<String,Map<String,String>> getEvents() {
		if(this.events_ == null) {
			return null;
		}
		return Collections.unmodifiableMap(this.events_);
	}		
	
	public XPlatUIElementType getElementType() {
		return elementType_;
	}

	public XPlatUIElementLayout getLayout() {
		return layout_;
	}

	public void setLayout(XPlatUIElementLayout layout_) {
		this.layout_ = layout_;
	}
	
	public boolean getFloat() {
		return this.float_;
	}

	public void setFloat(boolean shouldFloat) {
		this.float_ = shouldFloat;
	}

	public Map<String, List<XPlatUIElement>> getCollectionElements() {
		return Collections.unmodifiableMap(collectionElements_);
	}
	
	public void addElement(String listKey,XPlatUIElement element) {
		List<XPlatUIElement> elementCollection = this.collectionElements_.get(listKey);
		if(elementCollection == null) {
			throw new IllegalArgumentException(
				"Could not find the List of CollectionElements corresponding to the key " + listKey
			);			
		}
		elementCollection.add(element);
	}
	
	public void addElements(String listKey,List<XPlatUIElement> elements) {
		List<XPlatUIElement> elementCollection = this.collectionElements_.get(listKey);
		if(elementCollection == null) {
			throw new IllegalArgumentException(
				"Could not find the List of CollectionElements corresponding to the key " + listKey
			);			
		}
		elementCollection.addAll(elements);
	}
	
	public void setSelected(String selected) {
		this.selected_ = selected;
	}
	
	public String getSelected() {
		return this.selected_;
	}
	
	public Map<String,Map<String,Object>> getElementGroupParameters() {
		if(this.groupParams_ == null) {
			return null;
		}
		return Collections.unmodifiableMap(this.groupParams_);
	}
	
	public void addElementGroupParam(String group,String param,Object value) {
		if(this.groupParams_ == null) {
			this.groupParams_ = new HashMap<String,Map<String,Object>>();
		}
		Map<String,Object> thisGroup = this.groupParams_.get(group);
		if(thisGroup == null) {
			thisGroup = new HashMap<String,Object>();
			this.groupParams_.put(group,thisGroup);
		}
		thisGroup.put(param, value);
	}
	
	public Map<String,Integer> getElementGroupOrder() {
		if(this.groupOrder_ == null) {
			return null;
		}
		return Collections.unmodifiableMap(this.groupOrder_);
	}
	
	public void addGroupOrder(String group,Integer value) {
		if(this.groupOrder_ == null) {
			this.groupOrder_ = new HashMap<String,Integer>();
		}
		this.groupOrder_.put(group, value);
	}
	
	public void addList(String listKey,List<XPlatUIElement> elementList) {
		this.collectionElements_.put(listKey, elementList);
	}	

	public List<XPlatUIElement> createList(String listKey) {
		List<XPlatUIElement> elementList = new ArrayList<XPlatUIElement>();
		this.addList(listKey,elementList);
		return elementList;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			XPlatUICollectionElement tabController = new XPlatUICollectionElement(XPlatUIElementType.TAB_CONTAINER);
			
			tabController.createList("tab1");
			
			XPlatUIPrimitiveElement okBtn = new XPlatUIPrimitiveElement(XPlatUIElementType.BUTTON);
			okBtn.setParameter("label", "OK");
	
			XPlatUIPrimitiveElement cancelBtn = new XPlatUIPrimitiveElement(XPlatUIElementType.BUTTON);
			cancelBtn.setParameter("label", "Cancel");		
			
			tabController.addElement("tab1", okBtn);
			tabController.addElement("tab1", cancelBtn);
			
			JSONSerializer serializer = new JSONSerializer(); 
			serializer.transform(tabController.new ExcludeNullsTransformer(), void.class);
			System.out.println(serializer.deepSerialize(tabController));
			
		} catch(Exception e) {
			System.err.println(e);
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
