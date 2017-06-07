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

package org.systemsbiology.biotapestry.ui.xplat.dialog;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.ui.xplat.XPlatPrimitiveElementFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUICollectionElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIPrimitiveElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement.XPlatUIElementType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIValidity;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

import flexjson.JSONSerializer;
import flexjson.transformer.AbstractTransformer;

public class XPlatUIDialog {

	private int height_;
	private int width_;
	
	private String title_;
	
	// The cancel event of a dialog is specific to that dialog
	// and is typically fired as a result of a user clicking on
	// a button in the dialog titlebar (as opposed to an explicit
	// 'Cancel' button, which must be added to the Dialog)
	private String cancel_;
	
	// The cancel event may have some specific parameters associated with
	// it
	private Map<String,Object> cancelParameters_;
	
	private Point openOffset_;
	
	private SimpleUserFeedback.JOP dialogType_;
	
	// The map of all the elements of our dialog, contained in DialogElementCollections
	//
	// In the simplest case, a DialogElementCollection can be a single PANE with
	// a single list of elements. (Some implementations of the XPlatDialog may not even
	// render the Pane itself.) In the most complex, a collection can contain 
	// other collections (eg. a TAB_CONTAINER collection might in turn contain a 
	// STACK_CONTAINER which has 5 stacks of elements).
	private Map<String,XPlatUICollectionElement> dialogElementCollections_;
	
	// Optional

	// Other, platform-specific parameters as key-value pairs
	private Map<String,Object> parameters_;
	
	// If some elements of this dialog are only enabled under certain conditions,
	// set the initial condition state
	private List<XPlatUIValidity> defaultConditionStates_;
	
	// If this dialog takes in user inputs, this is where they will be stored
	// upon return from the client
	private UserInputs ui_;
	
	// Main constructor
	public XPlatUIDialog(
		String title, 
		int height, 
		int width,
		Map<String,XPlatUICollectionElement> dialogElems,
		Map<String,Object> parameters,
		List<XPlatUIValidity> defaultConditionStates,
		SimpleUserFeedback.JOP dialogType,
		String cancel
	) {
		if(title == null) {
			throw new IllegalArgumentException("A dialog must have a title");
		}
		this.height_ = height;
		this.width_ = width;
		this.title_ = title;
		
		this.dialogElementCollections_ = (dialogElems == null ? new HashMap<String,XPlatUICollectionElement>() : dialogElems);
		
		this.defaultConditionStates_ = defaultConditionStates;
		
		this.parameters_ = (parameters == null ? new HashMap<String,Object>() : parameters);
		
		// All dialogs default to being modal unless previously specified
		if(this.parameters_.get("isModal") == null) {
			this.parameters_.put("isModal", true);
		}
		
		this.dialogType_ = dialogType;
		
		this.cancel_ = cancel;
	}
	
	// Convenience Constructors
	public XPlatUIDialog(String title, int height, int width) {
		this(title,height,width,null,null,null,SimpleUserFeedback.JOP.PLAIN,null);
	}
	public XPlatUIDialog(String title, int height, int width,String cancel) {
		this(title,height,width,null,null,null,SimpleUserFeedback.JOP.PLAIN,cancel);
	}	

	
	// Setters and Getters
	public String getCancel() {
		return cancel_;
	}

	public void setCancel(String cancel_) {
		this.cancel_ = cancel_;
	}
	
	
	public int getHeight() {
		return height_;
	}

	public void setHeight(int height) {
		this.height_ = height;
	}

	public int getWidth() {
		return width_;
	}

	public void setWidth(int width) {
		this.width_ = width;
	}

	public String getTitle() {
		return title_;
	}

	public void setTitle(String title) {
		this.title_ = title;
	}

	public Map<String,XPlatUICollectionElement> getDialogElementCollections() {
		return Collections.unmodifiableMap(dialogElementCollections_);
	}

	public XPlatUICollectionElement getDialogElementCollection(String key) {
		if(this.dialogElementCollections_ == null) {
			return null;
		}
		return this.dialogElementCollections_.get(key);
	}
	
	public Point getOpenOffset() {
		return openOffset_;
	}

	public void setOpenOffset(Point openOffset) {
		this.openOffset_ = openOffset;
	}
	
	public void setParameter(String key, Object value) {
		this.parameters_.put(key, value);
	}
	
	public Map<String,Object> getParameters() {
		return Collections.unmodifiableMap(this.parameters_);
	}

	public Object getParameter(String key) {
		if(this.parameters_ == null) {
			return null;
		}
		return this.parameters_.get(key);
	}
	
	public SimpleUserFeedback.JOP getDialogType() {
		return dialogType_;
	}

	public void setDialogType(SimpleUserFeedback.JOP dialogType) {
		this.dialogType_ = dialogType;
	}

	public List<XPlatUIValidity> getDefaultConditionStates() {
		if(this.defaultConditionStates_ == null) {
			return null;
		}
		return Collections.unmodifiableList(this.defaultConditionStates_);
	}

	public void setDefaultConditionStates(List<XPlatUIValidity> defaultConditionStates) {
		this.defaultConditionStates_ = defaultConditionStates;
	}

	public void addDefaultState_(String condition, String value) {
		if(this.defaultConditionStates_ == null) {
			this.defaultConditionStates_ = new ArrayList<XPlatUIValidity>();
		}
		this.defaultConditionStates_.add(new XPlatUIValidity(condition,value));
	}
	
	public void setUserInputs(UserInputs ui) {
		this.ui_ = ui;
	}

	public UserInputs getUserInputs() {
		return this.ui_;
	}	
	
	
	public void setCancelParameters(Map<String,Object> cancelParameters) {
		if(this.cancelParameters_ == null) {
			this.cancelParameters_ = new HashMap<String,Object>();
		}
		this.cancelParameters_.putAll(cancelParameters);
	}
	
	public void addCancelParameter(String key, Object value) {
		if(this.cancelParameters_ == null) {
			this.cancelParameters_ = new HashMap<String,Object>();
		}
		this.cancelParameters_.put(key, value);
	}
	
	public Map<String,Object> getCancelParameters() {
		if(this.cancelParameters_ != null) {
			return Collections.unmodifiableMap(this.cancelParameters_);
		}
		return null;
	}
	
	
	////////////////////////////////
	// Main Dialog setup API
	////////////////////////////////

	// Add an already-instantiated collection
	public void addCollection(String collKey,XPlatUICollectionElement collection) {
		this.dialogElementCollections_.put(collKey,collection);
	}
	
	// Have the Dialog make the new collection and return a reference to it
	public XPlatUICollectionElement createCollection(String collKey, XPlatUIElementType collType) {
		XPlatUICollectionElement collection = new XPlatUICollectionElement(collType);
		this.addCollection(collKey,collection);
		return collection;
	}
	
	// Create a list for the elements in a specified collection via the underlying
	// collection API and return a reference to it
	public List<XPlatUIElement> createCollectionList(String collKey,String listKey) {
		XPlatUICollectionElement thisElementCollection = this.dialogElementCollections_.get(collKey);
		if(thisElementCollection == null) {
			throw new IllegalArgumentException(
				"Could not find the DialogElementCollection corresponding to the key " + collKey
			);
		}
		return thisElementCollection.createList(listKey);
	}
	
	// Add an instantiated element to a specified collection's specified list (eg. "Main Tab Collection"'s "Tab 1")
	public void addElementToCollection(String collKey, String listKey, XPlatUIElement element) {
		XPlatUICollectionElement thisElementCollection = this.dialogElementCollections_.get(collKey);
		if(thisElementCollection == null) {
			throw new IllegalArgumentException(
				"Could not find the DialogElementCollection corresponding to the key " + collKey
			);
		}
		thisElementCollection.addElement(listKey,element);
	}
	
	// Add an instantiated element to a specified collection's specified list (eg. "Main Tab Collection"'s "Tab 1")
	public void addElementsToCollection(String collKey, String listKey, List<XPlatUIElement> elements) {
		XPlatUICollectionElement thisElementCollection = this.dialogElementCollections_.get(collKey);
		if(thisElementCollection == null) {
			throw new IllegalArgumentException(
				"Could not find the DialogElementCollection corresponding to the key " + collKey
			);
		}
		thisElementCollection.addElements(listKey,elements);
	}

	// Replace the elements in the named collection and list with the provided list
	public void replaceElementsInCollection(String collKey, String listKey, List<XPlatUIElement> elements) {
		XPlatUICollectionElement thisElementCollection = this.dialogElementCollections_.get(collKey);
		if(thisElementCollection == null) {
			throw new IllegalArgumentException(
				"Could not find the DialogElementCollection corresponding to the key " + collKey
			);
		}
		thisElementCollection.addList(listKey,elements);
	}	
	
	
	/**************************
	 * MAIN
	 **************************
	 * 
	 * For testing
	 * 
	 * 
	 * @param args
	 * 
	 */
	public static void main(String[] args) {
		XPlatUIDialog thisDialog = new XPlatUIDialog("Test Dialog",500,500);
		
		thisDialog.setOpenOffset(new Point(-5,-10));
		
		XPlatPrimitiveElementFactory primElemFac = new XPlatPrimitiveElementFactory();
				
		/**
		 * Test for using container elements
		 * 
		 * 
		thisDialog.createCollection("tab container 1", XPlatUIElementType.TAB_CONTAINER);
		thisDialog.createCollectionList("tab container 1", "tab 1");
		
		XPlatUIPrimitiveElement okButton = new XPlatUIPrimitiveElement(XPlatUIElementType.BUTTON);
		okButton.setParameter("label", "OK");
		okButton.setParameter("action", "000");
		
		XPlatUIPrimitiveElement comboBox = new XPlatUIPrimitiveElement(XPlatUIElementType.COMBO_BOX_TEXT);
		comboBox.setParameter("label", "City");
		comboBox.addAvailVal("0","Fontana, CA");
		comboBox.addAvailVal("1","Chico, CA");
		comboBox.addAvailVal("2","Vancouver, BC");
		comboBox.addAvailVal("3","Quartzite, AZ");
		
		XPlatUICollectionElement stack1 = new XPlatUICollectionElement(XPlatUIElementType.STACK_CONTAINER);
		
		stack1.createList("page1");
		stack1.addElement("page1", comboBox);
				
		thisDialog.addElementToCollection("tab container 1", "tab 1", okButton);
		
		thisDialog.addElementToCollection("tab container 1", "tab 1", stack1);
		*
		*/
		
		List<XPlatUIElement> buttons = primElemFac.makeOkCancelButtons("bob", "lol");
		
		XPlatUIPrimitiveElement comboBox = new XPlatUIPrimitiveElement(XPlatUIElementType.COMBO_BOX_TEXT);
		comboBox.setParameter("label", "City");
		comboBox.addAvailVal("0","Fontana, CA");
		comboBox.addAvailVal("1","Chico, CA");
		comboBox.addAvailVal("2","Vancouver, BC");
		comboBox.addAvailVal("3","Quartzite, AZ");
		
		thisDialog.createCollection("main", XPlatUIElementType.PANE);
		List<XPlatUIElement> dialogList = thisDialog.createCollectionList("main", "all");
		
		thisDialog.addElementToCollection("main", "all", comboBox);
		
		thisDialog.addElementsToCollection("main", "all", buttons);
		
		thisDialog.addElementToCollection(
			"main", 
			"all",
			primElemFac.makeTextBox("lol", false, "lol?",null,true, "HI MOM",false)
		);

		thisDialog.addElementToCollection(
			"main", 
			"all",
			primElemFac.makeBasicButton("Bob's House of Pancakes!!", null, "6", null)
		);		
		
		JSONSerializer serializer = new JSONSerializer(); 
		
		serializer.transform(thisDialog.new ExcludeNullsTransformer(), void.class);
		
		System.out.println(serializer.deepSerialize(thisDialog));
		
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
