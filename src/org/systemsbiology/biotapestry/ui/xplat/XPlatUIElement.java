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

import java.util.Map;

/**
 * An interface for defining the expected behavior of a cross platform UI 'element', 
 * which can either be a single primitive widget (eg. a button) or a more complex,
 * containing widget (eg. a tab control). 
 * 
 * The aspects of such a UI element are:
 * 
 * 1. It must be able to provide its 'type' as an XPlatUIElementType enum
 * 
 * 2. It must provide 'parameters' as key-value pairs of Strings in a Map, and allow them to be set.
 * 
 *
 */
public interface XPlatUIElement {
	
	// A method to retrieve the entire parameter set
	public Map<String,Object> getParameters();
	
	// A method to retrieve the entire parameter set
	public Object getParameter(String key);
	
	// A method to set parameters as key/value pairs
	public void setParameter(String key, Object value);
		
	// A method for viewing the implementation's XPlatUIElementType
	public XPlatUIElementType getElementType();
	
	
	/**
	   * Enum for specifying the 'type' of an element or widget
	   * to be used in a UI. 
	   *
	   */
	public enum XPlatUIElementType {
		// A block of text
		TEXT_MESSAGE,
		
		// Multi-line text box, sometimes called a text area
	  	TEXT_BOX_MULTI,
	  	  
	  	// Single-line text box
	  	TEXT_BOX_SINGLE,
	  	
	  	// Single-line text box w/validation
	  	TEXT_BOX_SINGLE_VALIDATED,
	  	  
	  	// A cluster of selectable elements (eg. radio buttons or checkboxes) 
	  	// which are mutually exclusive; the group's value is the value of the
	  	// currently selected element
	  	SELECTION_GROUP,
	  	  
	  	// Standard radio button (single)
	  	RADIO_BUTTON,
	  	  
	  	// Standard checkbox; mutually exclusive in a selection group
	  	CHECKBOX,
	  	  
	  	// Text dropdown with a list of options; restricted to the list
	  	SELECTION_BOX,
	  	  
	  	// Text dropdown with a list of options and ability to enter
	  	// a new option
	  	COMBO_BOX_TEXT,
	  	  
	  	// Button dropdown with a list of options
	  	COMBO_BOX_BUTTON,

	  	// Special combo box which shows a color (specified as an RGB int) and a name for it
	  	COMBO_BOX_COLOR,	  	
	  	
	  	// An area in which something is drawn (static image, SVG, canvas)
	  	DRAWING_AREA,
	  	  
		// Standard button
		BUTTON,
		  	  
		TABLE,
		
		// A list-select widget, eg. a set of values, several or only one of which can be selected or checked
		LISTSELECT,
		
		// A containing element which won't actually be instantiated
		ABSTRACT_CONTAINER,
		
		// A containing element used for layout
		LAYOUT_CONTAINER,
		
		// A simple pane which holds a set of elements in addition-order
		PANE,
		  	  
	  	// A menu, which may be one of several types (menu categories tend
	  	// to be platform-specific, so no sub-typing is done here)
	 	MENU,
		
		// A container for sets of elements which only shows one set of 
		// elements at a time (most other display control containers are
		// subtypes of this one)
		STACK_CONTAINER,
	 	
		// A container that conforms to the 'tabbed' UI element style
		TAB_CONTAINER,
		
		// A container for sets of elements which conforms to the 
		// accordian/expansion UI element style
		ACCORDIAN_CONTAINER;
		
		// Suffix creation for auto-generated IDs
		public String getIdSuffix() {
			String suffix = null;
			switch(this) {
				case LAYOUT_CONTAINER:
					suffix = "_layout";
					break;
				case BUTTON:
					suffix = "_btn";
					break;
				case TEXT_BOX_MULTI:
				case TEXT_BOX_SINGLE:
					suffix = "_txtbox";
					break;
				case MENU:
					suffix = "_menu";
					break;
				case TEXT_MESSAGE:
					suffix = "_txtmsg";
					break;
				case CHECKBOX:
					suffix = "_chkbox";
					break;
				case RADIO_BUTTON:
					suffix = "_radiobtn";
					break;
				case SELECTION_GROUP:
					suffix = "_selgrp";
					break;
				case SELECTION_BOX:
					suffix = "_selbox";
					break;
				case LISTSELECT:
					suffix = "_listselex";
					break;
				case COMBO_BOX_BUTTON:
				case COMBO_BOX_TEXT:
					suffix = "_combobox";
					break;
				case TABLE:
					suffix = "_table";
					break;
				case TAB_CONTAINER:
					suffix = "_tabctrl";
					break;
				case ACCORDIAN_CONTAINER:
					suffix = "_acrdcont";
					break;	
				case STACK_CONTAINER:
					suffix = "_stackcont";
					break;	
				case PANE:
					suffix = "_pane";
					break;	
				case DRAWING_AREA:
					suffix = "_draw";
					break;					
				default:
					throw new IllegalArgumentException("This type is not recognized in suffix creation!");
			}
			return suffix;
		}
	}		
	
}
