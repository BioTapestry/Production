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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement.XPlatUIElementType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIEvent.XPlatUIElementActionType;
import org.systemsbiology.biotapestry.util.ResourceManager;

/**
 * A collection of convenience methods for use in making very common UI elements
 * (eg. OK/Cancel buttons or text messages)
 * 
 * 
 * 
 *
 */
public class XPlatPrimitiveElementFactory {
	
	public static String nRandomChars(int n) {
		Random rnd = new Random();
	    StringBuilder strBuilder = new StringBuilder();

	    for(int i = 0; i < n; i++) {
	    	strBuilder.append((char) (rnd.nextInt(26) + 97));
	    }

	    return strBuilder.toString();
	}
	
	private ResourceManager rMan_ = null;

	public XPlatPrimitiveElementFactory(ResourceManager rMan){
		this.rMan_ = rMan;
	}

	public XPlatPrimitiveElementFactory(){}	
	
	public void setResourseManager(ResourceManager rman) {
		this.rMan_ = rman;
	}
	
	
	public XPlatUIPrimitiveElement makeListMultiSelection(
		String label,
		String id,
		Map<String,Object> listActions, 
		Object columns,
		List<Map<String,Object>> listValues,
		List<String> initialSelections,
		boolean showHeader,
		boolean showGridlines,
		boolean singleSelex
	) {
		XPlatUIPrimitiveElement listMultiSelect = new XPlatUIPrimitiveElement(XPlatUIElementType.LISTSELECT);
		
		listMultiSelect.setParameter("class", "ListGrid");
		
		if(columns != null) {
			listMultiSelect.setParameter("columns", columns);
		}
		if(label != null) {
			listMultiSelect.setParameter("label", label);
		}
		if(initialSelections != null) {
			listMultiSelect.setParameter("selected", initialSelections);
		}
		if(listActions != null) {
			for(Map.Entry<String, Object> listAction : listActions.entrySet()) {
				listMultiSelect.setEvent(listAction.getKey(), (XPlatUIEvent) listAction.getValue());	
			}
		}
		if(listValues != null) {
			listMultiSelect.addAvailVal("list", listValues);
		}
		
		if(!showHeader) {
			listMultiSelect.setParameter("showHeader", false);
			listMultiSelect.setParameter("class", (listMultiSelect.getParameter("class") == null ? "" : listMultiSelect.getParameter("class")) + " NoHeader");
		}
		if(!showGridlines) {
			listMultiSelect.setParameter("showGridlines", false);
			listMultiSelect.setParameter("class", (listMultiSelect.getParameter("class") == null ? "" : listMultiSelect.getParameter("class")) + " NoGridlines AutoScroller");
		}
		
		if(singleSelex) {
			listMultiSelect.setParameter("selectionMode", "single");
		}
	
		String idBase = (id == null ? (label == null ? "list_multiselect" : label) : id); 
		
		listMultiSelect.setParameter("id", generateId(idBase.replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_]", ""),XPlatUIElementType.LISTSELECT));
		
		return listMultiSelect;
	}
	

	public XPlatUIPrimitiveElement makeCheckbox(String label, String onChangeAction, String value, boolean isChecked,String bundleAs) {
		XPlatUIPrimitiveElement checkBox = new XPlatUIPrimitiveElement(XPlatUIElementType.CHECKBOX);
		checkBox.setParameter("label", label);
		if(isChecked) {
			checkBox.setParameter("isChecked", "true");	
		}
		checkBox.setParameter("name", value.replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_]", ""));
		checkBox.setParameter("value", value);
		checkBox.setParameter("id", generateId(value,XPlatUIElementType.CHECKBOX));
  		if(onChangeAction != null) {
  			checkBox.setEvent("change", new XPlatUIEvent("change",onChangeAction));
  		}
  		
  		if(bundleAs != null) {
  			checkBox.setParameter("bundleAs", bundleAs);
  		}
  		
  		return checkBox;
	}
	
	public XPlatUIPrimitiveElement makeCheckbox(String label, String onChangeAction, String value, boolean isChecked) {
		return makeCheckbox(label,onChangeAction,value,isChecked,null);
	}
	
	public List<XPlatUIElement> makeYesNoCancelButtons(
		String yesClickAction,
		XPlatUIElementLayout yesLayout,
		String noClickAction,
		XPlatUIElementLayout noLayout,
		String cancelEvent,
		XPlatUIElementLayout cancelLayout
	) {
		List<XPlatUIElement> buttons = this.makeYesNoButtons(yesClickAction, yesLayout, noClickAction, noLayout);	
  		buttons.add(makeCancelButton(cancelEvent,cancelLayout));
  		
		return buttons;
	}
	

	public List<XPlatUIElement> makeYesNoButtons(
		String yesClickAction, 
		XPlatUIElementLayout yesLayout,
		String noClickAction,
		XPlatUIElementLayout noLayout
	) {
		List<XPlatUIElement> buttons = new ArrayList<XPlatUIElement>();
		
		XPlatUIPrimitiveElement yesBtn = makeBasicButton(this.rMan_ != null ? rMan_.getString("dialogs.yes") : "Yes",null,yesClickAction,yesLayout);
		XPlatUIPrimitiveElement noBtn = makeBasicButton(this.rMan_ != null ? rMan_.getString("dialogs.no") : "No",null,noClickAction,noLayout);
		
		yesBtn.addUiElementEventAction("click", XPlatUIElementActionType.DIALOG_CLOSE);
		yesBtn.addUiElementEventAction("click", XPlatUIElementActionType.BLANK_FORM);
		noBtn.addUiElementEventAction("click", XPlatUIElementActionType.DIALOG_CLOSE);
		noBtn.addUiElementEventAction("click", XPlatUIElementActionType.BLANK_FORM);
		
  		buttons.add(yesBtn);
  		buttons.add(noBtn);
  		
		return buttons;		
	}	
	
	public List<XPlatUIElement> makeOkCancelButtons(String okClickAction, String cancelClickAction) { 		
		return makeOkCancelButtons(okClickAction, cancelClickAction, true);		
	}

	public List<XPlatUIElement> makeOkCancelButtons(
		String okClickAction, 
		String cancelClickAction,
		boolean okShouldClose
	) {
		List<XPlatUIElement> buttons = new ArrayList<XPlatUIElement>();
		
		XPlatUIElement okBtn = makeOkButton(okClickAction,null,okShouldClose);
		XPlatUIElement cancelBtn = makeCancelButton(cancelClickAction,null);
		
  		buttons.add(okBtn);
  		buttons.add(cancelBtn);
  		
		return buttons;		
	}	
	
	public XPlatUIPrimitiveElement makeTextMessage(
		String id,
		String message,
		XPlatUIElementLayout layout
	) {
		if(id == null) {
			throw new IllegalArgumentException("You must provide an ID for this kind of element (Text Message)");
		}		
		XPlatUIPrimitiveElement txtMsg = new XPlatUIPrimitiveElement(XPlatUIElementType.TEXT_MESSAGE);
		txtMsg.setParameter("content", message);
		txtMsg.setParameter("name", id);
		txtMsg.setParameter("id", id.replaceAll("\\s+", "_") + "_txtmsg");
		if(layout != null) {
			txtMsg.setLayout(layout);
		}
		return txtMsg;
	}
	
	public XPlatUIPrimitiveElement makeTextBox(
		String id,
		boolean multi,
		String initialValue,
		XPlatUIElementLayout layout,
		boolean needsLabel,
		String label,
		boolean isValidated
	) {
		if(id == null) {
			throw new IllegalArgumentException("You must provide an ID for this kind of element (Text Box)");
		}
		XPlatUIPrimitiveElement txtBox = new XPlatUIPrimitiveElement(
			(multi ? XPlatUIElementType.TEXT_BOX_MULTI : (isValidated ? XPlatUIElementType.TEXT_BOX_SINGLE_VALIDATED : XPlatUIElementType.TEXT_BOX_SINGLE))
		);
		txtBox.setParameter("name", id);
		
		// This parameter is only required when an element does not normally expect a label (eg., a checkbox)
		if(needsLabel) {
			if(label == null) {
				throw new IllegalArgumentException("If a label is required, the label parameter cannot be null.");
			}
			txtBox.setParameter("needsLabel", "true");
		}
		if(label != null) {
			txtBox.setParameter("label", label);
		}
		
		if(initialValue != null) {
			txtBox.setParameter("value", initialValue);
		}
		if(layout != null) {
			txtBox.setLayout(layout);
		}
		// It turns out the naming convention for Text Boxes is the same, so we don't
		// care what the actual type is in this case
		txtBox.setParameter("id", generateId(id,XPlatUIElementType.TEXT_BOX_MULTI));	
		
		return txtBox;
	}
	
	public XPlatUIPrimitiveElement makeTxtComboBox(
			String id,
			String initialValue,
			XPlatUIElementLayout layout,
			boolean needsLabel,
			String label,
			Map<String,Object> values
		) {
			if(id == null && label == null) {
				throw new IllegalArgumentException(
					"This XPlatUIPrimitiveElement type (Text ComboBox) requires either an id or a label."
				);
			}
			
			XPlatUIPrimitiveElement comboBox = new XPlatUIPrimitiveElement(XPlatUIElementType.COMBO_BOX_TEXT);
						
			comboBox.setParameter("name", (id == null ? label.replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_]", "") : id));
			comboBox.setParameter("id", (id == null ? generateId(label,XPlatUIElementType.COMBO_BOX_TEXT) : generateId(id,XPlatUIElementType.COMBO_BOX_TEXT)));
			
			if(needsLabel) {
				if(label == null) {
					throw new IllegalArgumentException("If a label is required, the label parameter cannot be null.");
				}
				comboBox.setParameter("needsLabel", "true");
			}
			
			if(label != null) {
				comboBox.setParameter("label", label);
			}
			
			if(initialValue != null) {
				comboBox.setParameter("selValue", initialValue);
			}
									
			if(layout != null) {
				comboBox.setLayout(layout);
			}
			
			if(values != null) {
				for(Map.Entry<String, Object> entry : values.entrySet()) {
					comboBox.addAvailVal(entry.getKey(), entry.getValue());
				}
			}
			
			return comboBox;
		}		
	
	

	public XPlatUIPrimitiveElement makeSelectionGroup(
			String id,
			String initialValue,
			XPlatUIElementLayout layout,
			boolean needsLabel,
			String label,
			Map<String,String> values
		) {
			if(id == null) {
				throw new IllegalArgumentException("You must provide an ID for this kind of element (Selection Group)");
			}
			XPlatUIPrimitiveElement selexGrp = new XPlatUIPrimitiveElement(XPlatUIElementType.SELECTION_GROUP);
			
			selexGrp.setParameter("name", id);
			if(needsLabel) {
				if(label == null) {
					throw new IllegalArgumentException("If a label is required, the label parameter cannot be null.");
				}
				selexGrp.setParameter("needsLabel", "true");
			}
			
			if(label != null) {
				selexGrp.setParameter("label", label);
			}
			
			if(initialValue != null) {
				selexGrp.setParameter("selValue", initialValue);
			}
									
			if(layout != null) {
				selexGrp.setLayout(layout);
			}
			
			if(values != null) {
				for(Map.Entry<String, String> entry : values.entrySet()) {
					selexGrp.addAvailVal(entry.getKey(), entry.getValue());
				}
			}

			selexGrp.setParameter("id", generateId(id,XPlatUIElementType.SELECTION_GROUP));
			
			return selexGrp;
		}	
	
	
	private XPlatUIPrimitiveElement makeButton(
		String label,
		String id,
		String event,
		String eventAction,
		XPlatUIElementLayout layout,
		boolean shouldFloat,
		boolean shouldClearForm,
		boolean shouldCloseDialog
	) {
  		XPlatUIPrimitiveElement btn = new XPlatUIPrimitiveElement(XPlatUIElementType.BUTTON,shouldFloat);
  		btn.setParameter("label", label);
  		btn.setParameter("id", generateId((id==null?label:id),XPlatUIElementType.BUTTON));
  		if(event != null && eventAction != null) {
  			btn.setEvent(event, new XPlatUIEvent(event,eventAction));
  			if(shouldClearForm)
  				btn.getEvent(event).addUiElementAction(XPlatUIElementActionType.BLANK_FORM);
  			if(shouldCloseDialog)
  				btn.getEvent(event).addUiElementAction(XPlatUIElementActionType.DIALOG_CLOSE);
  		}
  		if(layout != null) {
  	  		btn.setLayout(layout);	
  		}
  		  		
  		return btn;		
	}
	
	public XPlatUIPrimitiveElement makeBasicButton(
		String label,
		String id,
		String clickAction,
		XPlatUIElementLayout layout
	) {
		return makeButton(label,id,"click",clickAction,layout,true,false,false);
	}
	
	public XPlatUIPrimitiveElement makeBasicClosingButton(
			String label,
			String id,
			String clickAction,
			XPlatUIElementLayout layout
		) {
			return makeButton(label,id,"click",clickAction,layout,true,true,true);
		}
	
	public XPlatUIPrimitiveElement makeNonFloatingButton(
		String label,
		String id,
		String clickAction,
		XPlatUIElementLayout layout
	) {
		return makeButton(label,id,"click",clickAction,layout,false,false,false);
	}	
	
	public String generateId(String id, XPlatUIElementType type) {
		// Restrict IDs to only numbers and letters
		return id.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_]", "") + type.getIdSuffix() + "_" + new Date().getTime();
	}
	
	public XPlatUIPrimitiveElement makeOkButton(
		String clickAction,
		XPlatUIElementLayout layout,
		boolean shouldClose
	) {
		
		XPlatUIPrimitiveElement okBtn = makeBasicButton(
			this.rMan_ != null ? rMan_.getString("dialogs.ok") : "OK",null,clickAction,layout
		);
		
		if(shouldClose) {
			okBtn.addUiElementEventAction("click", XPlatUIElementActionType.BLANK_FORM);
			okBtn.addUiElementEventAction("click", XPlatUIElementActionType.DIALOG_CLOSE);
		}

  		return okBtn;
	}
	
	
	public XPlatUIPrimitiveElement makeClientCancelButton(XPlatUIElementLayout layout, FlowKey keyVal) {
			XPlatUIPrimitiveElement cancelBtn = makeBasicButton(
				this.rMan_ != null ? rMan_.getString("dialogs.cancel") : "Cancel",null,"CLIENT_CANCEL_COMMAND",layout
			);
						
			cancelBtn.getEvent("click").addParameter("cmdClass", keyVal.toString().split("_")[0]);
			cancelBtn.getEvent("click").addParameter("cmdKey", keyVal.toString().substring(keyVal.toString().indexOf("_")+1));
			cancelBtn.getEvent("click").addParameter("action", keyVal.toString());
			
			cancelBtn.addUiElementEventAction("click", XPlatUIElementActionType.BLANK_FORM);
			cancelBtn.addUiElementEventAction("click", XPlatUIElementActionType.DIALOG_CLOSE);
			
	  		return cancelBtn;
		}	
	
	public XPlatUIPrimitiveElement makeCancelButton(
		String clickAction,
		XPlatUIElementLayout layout
	) {
		XPlatUIPrimitiveElement cancelBtn = makeBasicButton(
			this.rMan_ != null ? rMan_.getString("dialogs.cancel") : "Cancel",null,clickAction,layout
		);
					
		cancelBtn.addUiElementEventAction("click", XPlatUIElementActionType.BLANK_FORM);
		cancelBtn.addUiElementEventAction("click", XPlatUIElementActionType.DIALOG_CLOSE);
		
  		return cancelBtn;
	}
	
	
	public XPlatUIPrimitiveElement makeDrawingArea(int height,int width) {
		XPlatUIPrimitiveElement drawingSpace = new XPlatUIPrimitiveElement(XPlatUIElementType.DRAWING_AREA);
		
		drawingSpace.setParameter("height",Integer.toString(height));
		drawingSpace.setParameter("width",Integer.toString(width));
		drawingSpace.setParameter("id", generateId("drawGeneInstance",XPlatUIElementType.DRAWING_AREA));
		
		return drawingSpace;
	}

		
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
