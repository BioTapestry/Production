/*
 **    Copyright (C) 2003-2015 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.systemsbiology.biotapestry.ui.xplat.XPlatLayoutFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatLayoutFactory.RegionType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatPrimitiveElementFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatResponse;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUICollectionElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement.XPlatUIElementType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElementLayout;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIPrimitiveElement;
import org.systemsbiology.biotapestry.ui.xplat.dialog.XPlatUIDialog;

/****************************************************************************
 **
 ** Cross-Platform Abstraction of the Swing JOptionDialog
 */

public class SimpleUserFeedback implements XPlatResponse {

	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC CONSTANTS
	//
	////////////////////////////////////////////////////////////////////////////  

	public enum JOP {NONE, WARNING, ERROR, YES_NO_OPTION, YES_NO_CANCEL_OPTION, QUESTION, PLAIN, OPTION_OPTION}; 

	//
	// Keep as ints to match JOptionPane interface:
	//

	public static final int YES = -3;
	public static final int NO = -2; 
	public static final int CANCEL = -1; 
	
	private final XPlatResponseType xplatResponseType_ = XPlatResponseType.SUF; 

	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC INSTANCE MEMBERS
	//
	////////////////////////////////////////////////////////////////////////////  

	public JOP optionPane;
	public String title;
	public String message;
	public Object[] jopOptions;
	public Object jopDefault;
	public boolean expectsResponse_;

	//
	// Various results
	//

	private int jopResult_;
	private Object jopQuestionResult_;

	// For XPlat SUFs, the events/actions associated with the buttons must be provided
	// when the dialog is exported via getXPlatDialog()
	private Map<String,String> clickActions_;

	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC CONSTRUCTORS
	//
	////////////////////////////////////////////////////////////////////////////  

	public SimpleUserFeedback(JOP op, String message, String title) {
		// If expectsReponse is not provided, we assume any error, none, plain, or warning SUF doesn't need an answer
		this(op,message,title,(op != JOP.ERROR && op != JOP.NONE && op != JOP.PLAIN && op != JOP.WARNING));
	}

	public SimpleUserFeedback(JOP op, String message, String title, boolean expectsResponse) {
		this(op,message,title,null,expectsResponse);
	}

	public SimpleUserFeedback(JOP op, String message, String title,Map<String,String> clickActions) {
		// If expectsReponse is not provided, we assume any error, none, plain, or warning SUF doesn't need an answer
		this(op,message,title,clickActions,(op != JOP.ERROR && op != JOP.NONE && op != JOP.PLAIN && op != JOP.WARNING));  
	}

	// For xplat SUFs, which require click actions to be provided prior to serialization
	public SimpleUserFeedback(JOP op, String message, String title,Map<String,String> clickActions,boolean expectsResponse) {
		this.optionPane = op;
		this.message = message;
		this.title = title;
		this.clickActions_ = clickActions;
		this.expectsResponse_ = expectsResponse;
	}


	public SimpleUserFeedback(JOP op, String message, String title, Object[] jopOptions, Object jopDefault) {
		// Any SUF that has an Object array of options must be a question or option pane
		if ((op != JOP.QUESTION) && (op != JOP.OPTION_OPTION)) {
			throw new IllegalArgumentException();
		}
		this.optionPane = op;
		this.message = message;
		this.title = title;
		this.jopOptions = jopOptions;
		this.jopDefault = jopDefault; 
		// If we're in a question or option SUF then a response is required
		this.expectsResponse_ = true;
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// METHODS
	//
	////////////////////////////////////////////////////////////////////////////  

	public int getIntegerResult() {
		return (jopResult_);
	}

	public Object getObjectResult() {
		return (jopQuestionResult_);
	}

	public void setIntegerResult(int res) {
		jopResult_ = res;
		return;
	}

	public void setObjectResult(Object res) {
		jopQuestionResult_ = res;
		return;
	} 
	
	public XPlatResponseType getXplatResponseType() {
		return this.xplatResponseType_;
	}

	////////////////////
	// getXPlatDialog
	///////////////////
	//
	// Produces an xplat serializable dialog of this SUF
	//
	public XPlatUIDialog getXPlatDialog() {
		XPlatPrimitiveElementFactory primElemFac_ = new XPlatPrimitiveElementFactory(null);
		XPlatUIDialog thisDialog = new XPlatUIDialog(this.title,200,400);

		thisDialog.setParameter("id",this.optionPane.toString());
		thisDialog.setDialogType(this.optionPane);

		XPlatUICollectionElement mainPane = thisDialog.createCollection("mainPane", XPlatUIElementType.LAYOUT_CONTAINER);
		mainPane.setParameter("style", "height: 175px; width: 375px;");
		mainPane.setParameter("gutters", "false");
		List<XPlatUIElement> centerElements = mainPane.createList("center");
		List<XPlatUIElement> bottomElements = mainPane.createList("bottom");

		centerElements.add(
			primElemFac_.makeTextMessage(
				"simple_user_message",
				this.message, 
				XPlatLayoutFactory.makeRegionalLayout(RegionType.CENTER, 0)
			)
		);

		List<XPlatUIElement> btns = null;

		// If a series of click actions pertaining to the
		// OK button are not provided then we assume there
		// is no action to be performed
		switch(this.optionPane) {
		case ERROR:
		case NONE:	
		case PLAIN:
		case WARNING:
			btns = new ArrayList<XPlatUIElement>();
			btns.add(primElemFac_.makeOkButton(
				(this.clickActions_ == null ? "DO_NOTHING" : this.clickActions_.get("ok")), 
				XPlatLayoutFactory.makeRegionalLayout(RegionType.BOTTOM, 0),true
			));
			bottomElements.addAll(btns);
			break;
			
		case YES_NO_OPTION:
			btns = primElemFac_.makeYesNoButtons(
				(this.clickActions_ == null ? "DO_NOTHING" : this.clickActions_.get("yes")),
				XPlatLayoutFactory.makeRegionalLayout(RegionType.BOTTOM, 0),
				(this.clickActions_ == null ? "DO_NOTHING" : this.clickActions_.get("no")),
				XPlatLayoutFactory.makeRegionalLayout(RegionType.BOTTOM, 1)
			);
			bottomElements.addAll(btns);
			break;
			
		case YES_NO_CANCEL_OPTION:
			btns = primElemFac_.makeYesNoCancelButtons(
				(this.clickActions_ == null ? null : this.clickActions_.get("yes")),
				XPlatLayoutFactory.makeRegionalLayout(RegionType.BOTTOM, 0),
				(this.clickActions_ == null ? null : this.clickActions_.get("no")),
				XPlatLayoutFactory.makeRegionalLayout(RegionType.BOTTOM, 1),
				(this.clickActions_ == null ? null : this.clickActions_.get("cancel")),
				XPlatLayoutFactory.makeRegionalLayout(RegionType.BOTTOM, 2)
			);
			bottomElements.addAll(btns);
			break;

			// TODO: Note QUESTIONs expect Objects returned; this needs to be tried out with
			// a bundleAs on these options, probably...
		case QUESTION:
		case OPTION_OPTION:
			btns = new ArrayList<XPlatUIElement>();  
			for (int i = 0; i < jopOptions.length; i++) {        
				XPlatUIElementLayout lay = XPlatLayoutFactory.makeRegionalLayout(RegionType.BOTTOM, i);
				XPlatUIPrimitiveElement but = primElemFac_.makeBasicButton(jopOptions[i].toString(), null, null, lay);
				btns.add(but);
			}      

			bottomElements.addAll(btns);
			break;

		default:
			throw new IllegalArgumentException();
		}

		for(XPlatUIElement thisBtn : btns) {
			((XPlatUIPrimitiveElement)thisBtn).getEvent("click").addParameter("dialogType", this.optionPane.toString());
		}

		return thisDialog;
	}
}   

