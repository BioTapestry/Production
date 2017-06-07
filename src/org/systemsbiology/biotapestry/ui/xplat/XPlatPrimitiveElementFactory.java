/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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
	
	//////////////////
	// nRandomChars
	//////////////////
	//
	// Generates a string of randomly chosen letters of
	// the specified length
	//
	public static String nRandomChars(int n) {
		if(n <= 0) {
			return null;
		}
		Random rnd = new Random();
	    StringBuilder strBuilder = new StringBuilder();

	    for(int i = 0; i < n; i++) {
	    	strBuilder.append((char) (rnd.nextInt(26) + 97));
	    }

	    return strBuilder.toString();
	}
	
	// Resource Manager for obtaining localized Strings
	private ResourceManager rMan_ = null;

	///////////////////
	// Constructors
	//////////////////
	//
	//
	public XPlatPrimitiveElementFactory(ResourceManager rMan){
		this.rMan_ = rMan;
	}

	public XPlatPrimitiveElementFactory(){}	
	
	
	////////////////
	// generateId
	///////////////
	//
	//
	public String generateId(String id, XPlatUIElementType type) {
		String dateTime = new Long(new Date().getTime()).toString();
		// IDs may be alphanumeric and contain underscores
		return id.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_]", "") + type.getIdSuffix() + "_" + dateTime.substring(dateTime.length()-5)+nRandomChars(4);
	}
	
	////////////////////////
	// setResourseManager
	///////////////////////
	//
	//
	public void setResourseManager(ResourceManager rman) {
		this.rMan_ = rman;
	}
	
	///////////////////////////
	// makeListMultiSelection
	///////////////////////////
	//
	//
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
	

	//////////////////
	// makeCheckbox
	//////////////////
	//
	// Makes a bundled Checkbox (for the value to be returned)
	// 
	public XPlatUIPrimitiveElement makeCheckbox(String label, String onChangeAction, String value, boolean isChecked,String bundleAs) {
		XPlatUIPrimitiveElement checkBox = new XPlatUIPrimitiveElement(XPlatUIElementType.CHECKBOX);
		checkBox.setParameter("label", label);
		if(isChecked) {
			checkBox.setParameter("checked", "true");	
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
	
	/////////////////
	// makeCheckbox
	////////////////
	//
	// Makes an unbundled Checkbox
	// 
	public XPlatUIPrimitiveElement makeCheckbox(String label, String onChangeAction, String value, boolean isChecked) {
		return makeCheckbox(label,onChangeAction,value,isChecked,null);
	}
	
	////////////////////////////// BUTTONS 	////////////////////////////// 
	
	////////////////
	// makeButton
	///////////////
	//
	//
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
	
	/////////////////////
	// makeBasicButton
	/////////////////////
	//
	//
	public XPlatUIPrimitiveElement makeBasicButton(
		String label,
		String id,
		String clickAction,
		XPlatUIElementLayout layout
	) {
		return makeButton(label,id,"click",clickAction,layout,true,false,false);
	}
	
	///////////////////////////
	// makeYesNoCancelButtons
	//////////////////////////
	//
	//
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
	
	/////////////////////
	// makeYesNoButtons
	/////////////////////
	//
	//
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
		yesBtn.getEvent("click").addParameter("buttonClicked", "Yes");
		noBtn.addUiElementEventAction("click", XPlatUIElementActionType.DIALOG_CLOSE);
		noBtn.addUiElementEventAction("click", XPlatUIElementActionType.BLANK_FORM);
		noBtn.getEvent("click").addParameter("buttonClicked", "No");
		
  		buttons.add(yesBtn);
  		buttons.add(noBtn);
  		
		return buttons;		
	}	
	
	/////////////////////////
	// makeOkCancelButtons
	////////////////////////
	//
	//
	public List<XPlatUIElement> makeOkCancelButtons(String okClickAction, String cancelClickAction) { 		
		return makeOkCancelButtons(okClickAction, cancelClickAction, true);		
	}

	///////////////////////////
	// makeOkCancelButtons
	//////////////////////////
	//
	//
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
			
	////////////////////////////
	// makeBasicClosingButton
	///////////////////////////
	//
	//
	public XPlatUIPrimitiveElement makeBasicClosingButton(
		String label,
		String id,
		String clickAction,
		XPlatUIElementLayout layout
	) {
		return makeButton(label,id,"click",clickAction,layout,true,true,true);
	}
	
	//////////////////////////
	// makeNonFloatingButton
	/////////////////////////
	//
	//
	public XPlatUIPrimitiveElement makeNonFloatingButton(
		String label,
		String id,
		String clickAction,
		XPlatUIElementLayout layout
	) {
		return makeButton(label,id,"click",clickAction,layout,false,false,false);
	}	
		
	//////////////////
	// makeOkButton
	//////////////////
	//
	//
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
		
		// OK buttons are never forApply
		okBtn.getEvent("click").addParameter("forApply", false);

  		return okBtn;
	}

	/////////////////////
	// makeCloseButton
	////////////////////
	//
	//
	public XPlatUIPrimitiveElement makeCloseButton(
		String clickAction,
		XPlatUIElementLayout layout
	) {
		
		XPlatUIPrimitiveElement closeBtn = makeBasicButton(
			this.rMan_ != null ? rMan_.getString("dialogs.close") : "Close",null,clickAction,layout
		);
		
		closeBtn.addUiElementEventAction("click", XPlatUIElementActionType.BLANK_FORM);
		closeBtn.addUiElementEventAction("click", XPlatUIElementActionType.DIALOG_CLOSE);

  		return closeBtn;
	}

	/////////////////////
	// makeApplyButton
	////////////////////
	//
	//
	public XPlatUIPrimitiveElement makeApplyButton(String clickAction,XPlatUIElementLayout layout) {
		
		XPlatUIPrimitiveElement applyBtn = makeBasicButton(
			this.rMan_ != null ? rMan_.getString("dialogs.apply") : "Apply",null,clickAction,layout
		);
		
		// Apply buttons are always forApply
		applyBtn.getEvent("click").addParameter("forApply", true);
		
  		return applyBtn;
	}
	
	////////////////////////////
	// makeApplyOkCloseButtons
	////////////////////////////
	//
	//
	public List<XPlatUIElement> makeApplyOkCloseButtons(
		String okClickAction,
		XPlatUIElementLayout okLayout,
		String applyClickAction,
		XPlatUIElementLayout applyLayout,
		String closeClickAction,
		XPlatUIElementLayout closeLayout
	) {		
		return this.makeApplyOkCloseButtons(okClickAction,okLayout,true,applyClickAction,applyLayout,closeClickAction,closeLayout);
	}
	
	////////////////////////////
	// makeApplyOkCloseButtons
	///////////////////////////
	//
	// Convenience method for no layout
	//
	public List<XPlatUIElement> makeApplyOkCloseButtons(
		String okClickAction,
		String applyClickAction,
		String closeClickAction
	) {
		return this.makeApplyOkCloseButtons(okClickAction,true,applyClickAction,closeClickAction);
	}	

	////////////////////////////
	// makeApplyOkCloseButtons
	///////////////////////////
	//
	// Convenience method for no layout
	//
	public List<XPlatUIElement> makeApplyOkCloseButtons(
		String okClickAction,
		boolean okCloses,
		String applyClickAction,
		String closeClickAction
	) {
		return this.makeApplyOkCloseButtons(okClickAction, null, okCloses, applyClickAction, null, closeClickAction, null);
	}	
	
	////////////////////////////
	// makeApplyOkCloseButtons
	////////////////////////////
	//
	//
	public List<XPlatUIElement> makeApplyOkCloseButtons(
		String okClickAction,
		XPlatUIElementLayout okLayout,
		boolean okCloses,
		String applyClickAction,
		XPlatUIElementLayout applyLayout,
		String closeClickAction,
		XPlatUIElementLayout closeLayout
	) {
		List<XPlatUIElement> buttons = new ArrayList<XPlatUIElement>();
		
		XPlatUIPrimitiveElement okBtn = this.makeOkButton(okClickAction, okLayout, okCloses);
		XPlatUIPrimitiveElement applyBtn = this.makeApplyButton(applyClickAction, applyLayout);
		applyBtn.setParameter("bundleAs", "forApply");
		
		XPlatUIPrimitiveElement closeBtn = this.makeCloseButton(closeClickAction, closeLayout);
				
  		buttons.add(applyBtn);
  		buttons.add(okBtn);
  		buttons.add(closeBtn);
  		
		return buttons;
	}
	
	/////////////////////////////
	// makeClientCancelButton
	////////////////////////////
	//
	//
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

	/////////////////////
	// makeCancelButton
	////////////////////
	//
	//
	public XPlatUIPrimitiveElement makeCancelButton(
		String clickAction,
		XPlatUIElementLayout layout,
		boolean withClose
	) {

		XPlatUIPrimitiveElement cancelBtn = makeBasicButton(
			this.rMan_ != null ? this.rMan_.getString("dialogs.cancel") : "Cancel",null,clickAction,layout
		);
					
		if(withClose) {
			cancelBtn.addUiElementEventAction("click", XPlatUIElementActionType.BLANK_FORM);
			cancelBtn.addUiElementEventAction("click", XPlatUIElementActionType.DIALOG_CLOSE);
		}
		cancelBtn.getEvent("click").addParameter("buttonClicked", "Cancel");
		
  		return cancelBtn;
	}	
	
	/////////////////////
	// makeCancelButton
	////////////////////
	//
	//
	public XPlatUIPrimitiveElement makeCancelButton(
		String clickAction,
		XPlatUIElementLayout layout
	) {
		return this.makeCancelButton(clickAction,layout,true);
	}
	
	/////////////////////
	// makeTextMessage
	////////////////////
	//
	//
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
	
	/////////////////
	// makeTextBox
	////////////////
	//
	//
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
	
	//////////////////////
	// makeTxtComboBox
	////////////////////
	//
	//
	public XPlatUIPrimitiveElement makeTxtComboBox(
		String id,
		String initialValue,
		XPlatUIElementLayout layout,
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
	
	//////////////////////////////
	// makeColorChooserComboBox
	/////////////////////////////
	//
	//
	public XPlatUIPrimitiveElement makeColorChooserComboBox(
		String id,
		String initialValue,
		XPlatUIElementLayout layout,
		String label,
		Map<String,Object> values
	) {
		XPlatUIPrimitiveElement comboBox = new XPlatUIPrimitiveElement(XPlatUIElementType.COMBO_BOX_COLOR);
		
		comboBox.setParameter("name", (id == null ? label.replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_]", "") : id));
		comboBox.setParameter("id", (id == null ? generateId(label,XPlatUIElementType.COMBO_BOX_TEXT) : generateId(id,XPlatUIElementType.COMBO_BOX_TEXT)));
				
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
		
		comboBox.setParameter("comboType", "COLOR_CHOOSER");
		
		return comboBox;
	}
	
	
	/////////////////////////
	// makeSelectionGroup
	///////////////////////
	//
	//
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
	
	//////////////////////
	// makeDrawingArea
	/////////////////////
	//
	//
	public XPlatUIPrimitiveElement makeDrawingArea(int height,int width) {
		XPlatUIPrimitiveElement drawingSpace = new XPlatUIPrimitiveElement(XPlatUIElementType.DRAWING_AREA);
		
		drawingSpace.setParameter("height",Integer.toString(height));
		drawingSpace.setParameter("width",Integer.toString(width));
		drawingSpace.setParameter("id", generateId("drawGeneInstance",XPlatUIElementType.DRAWING_AREA));
		
		return drawingSpace;
	}

		

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
