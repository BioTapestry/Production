/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.cmd.flow;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.TreeNode;

import org.systemsbiology.biotapestry.app.TabChange;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.xplat.XPlatStackPage;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** As we progress through an edit operation, the operation creates
** dialogs for user input, confirmation, or warning feedback, as well
** as a holder for the current state of the operation.
*/

public class DialogAndInProcessCmd {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
      
  public enum Progress {
    NORMAL(false),
    KEEP_PROCESSING(true),
    USER_CANCEL(false),
    HAVE_ERROR(false),
    DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK(false),
    INSTALL_MOUSE_MODE(false),
    MOUSE_MODE_RESULT(false),
    SIMPLE_USER_FEEDBACK(false),
    SIMPLE_USER_FEEDBACK_AND_MOUSE_RESULT(false),
    HAVE_STACK_PAGE(false),
    HAVE_FRAME_TO_LAUNCH_AND_MOUSE_RESULT(false),
    HAVE_DIALOG_TO_SHOW(false),
    HAVE_FRAME_TO_LAUNCH(false),
    HAVE_USER_INPUTS(false),
    DONE_WITH_SIMPLE_USER_FEEDBACK(false),
    DONE(false),
    DONE_ON_THREAD(false),
    APPLY_ON_THREAD(false),
    APPLY_PROCESSED(false);
    
    private boolean keepLooping_;
    
    Progress(boolean keepLooping) {
      this.keepLooping_ = keepLooping;  
    }
       
    public boolean keepLooping() {
      return (keepLooping_);      
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  public Progress state; 
  public SimpleUserFeedback suf;
  public XPlatStackPage stackPage;
  public ServerControlFlowHarness.Dialog dialog;
  public ServerControlFlowHarness.UserInputs cfhui; 
  public ServerControlFlowHarness.ClickResult pccr;
  
  public CmdState currStateX; 
  public PanelCommands.Mode suPanelMode;
  
  public Map<String,Object> commandResults;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult pccr, CmdState cmdState) {
    state = Progress.MOUSE_MODE_RESULT;
    this.pccr = pccr;
    currStateX = cmdState; // Needed when mouse mode result is a rejection and we have to keep going!
  }
  
  public DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult pccr, CmdState cmdState, ServerControlFlowHarness.Dialog dialog) {
    state = Progress.HAVE_FRAME_TO_LAUNCH_AND_MOUSE_RESULT;
    this.dialog = dialog;
    this.pccr = pccr;
    currStateX = cmdState;
  }
  
  public DialogAndInProcessCmd(Progress theState, CmdState cmdState) {
    state = theState;
    currStateX = cmdState;
  }
  
  public DialogAndInProcessCmd(SimpleUserFeedback suf, CmdState cmdState) {
    state = Progress.SIMPLE_USER_FEEDBACK;
    this.suf = suf;
    currStateX = cmdState;
  }
  
  // Use for DONE_WITH_SIMPLE_USER_FEEDBACK
  public DialogAndInProcessCmd(SimpleUserFeedback suf, CmdState cmdState, Progress theState) {
    state = theState;
    this.suf = suf;
    currStateX = cmdState;
  }
    
   public DialogAndInProcessCmd(SimpleUserFeedback suf, ServerControlFlowHarness.ClickResult pccr, CmdState cmdState) {
    state = Progress.SIMPLE_USER_FEEDBACK_AND_MOUSE_RESULT;
    this.suf = suf;
    this.pccr = pccr;
    currStateX = cmdState;
  }
 
  public DialogAndInProcessCmd(ServerControlFlowHarness.Dialog dialog, CmdState cmdState) {
    state = (dialog.dialogIsModal()) ? Progress.HAVE_DIALOG_TO_SHOW : Progress.HAVE_FRAME_TO_LAUNCH;
    this.dialog = dialog;
    currStateX = cmdState;
    cfhui = null;
  }
  
  public DialogAndInProcessCmd(ServerControlFlowHarness.UserInputs cfhui, CmdState cmdState) {
	  this(cfhui,cmdState,null);
  }

  public DialogAndInProcessCmd(ServerControlFlowHarness.UserInputs cfhui, CmdState cmdState, ServerControlFlowHarness.Dialog dialog) {
	    this.state = Progress.HAVE_USER_INPUTS;
	    this.cfhui = cfhui;
	    this.currStateX = cmdState;
	    this.dialog = dialog;
  }
  
  public DialogAndInProcessCmd(Progress theState, CmdState cmdState, Map<String, Object> cmdResults) {
	  state = theState;
	  currStateX = cmdState;
	  commandResults = cmdResults;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Command States
  */ 
  
  public interface CmdState {
    public String getNextStep();
  }
  
  /***************************************************************************
  **
  ** Command State for a button masker
  */ 
  
  public interface ButtonMaskerCmdState {
    public int getMask();
    public boolean pushDisplay();
  } 

  /***************************************************************************
  **
  ** Command State for mouse clicks
  */ 
  
  public interface MouseClickCmdState extends ButtonMaskerCmdState {
    
    public enum Floater{NO_FLOATER, LINES_FLOATER, RECT_FLOATER, OBJECT_FLOATER};
    
    public boolean noSubModels(); 
    public boolean noInstances(); 
    public boolean mustBeDynamic();
    public boolean cannotBeDynamic();
    public boolean noRootModel();
    public boolean showBubbles();
    public boolean hasTargetsAndOverlays();
    public Floater floaterMode();
    public Object getFloater(int x, int y);
    public Color getFloaterColor();
    public void setFloaterPropsInLayout(Layout flay);
  } 
  
  /***************************************************************************
  **
  ** Command State for Model Tree
  */ 
  
  public interface ModelTreeCmdState extends CmdState {
    public void setPreload(Genome popupTarget, Genome popupModelAncestor, TreeNode popupNode);
  } 
   
  /***************************************************************************
  **
  ** Command State for Popup
  */ 
  
  public interface PopupCmdState extends CmdState {
    public void setIntersection(Intersection intersect);
  }  
  
  /***************************************************************************
  **
  ** Command State for Tab Popup
  */ 
  
  public interface TabPopupCmdState extends CmdState {
    public void setTab(int tabNum, boolean viaUI, TabChange tc);
  }  
  
  /***************************************************************************
  **
  ** Command State for Popup with a point
  */ 
  
  public interface PopupPointCmdState extends PopupCmdState {
    public void setPopupPoint(Point2D point);
  }
  
  /***************************************************************************
  **
  ** Command State for Popup with a point and absolute point
  */ 
  
  public interface AbsolutePopupPointCmdState extends PopupPointCmdState {
    public void setAbsolutePoint(Point point);
  }
  
  /***************************************************************************
  **
  ** Command State for Popup handling multi-selection
  */ 
  
  public interface MultiSelectCmdState extends CmdState {
    public void setMultiSelections(Set<String> genes, Set<String> nodes, Set<String> links);
  }
  
  /***************************************************************************
  **
  ** Command State for path model generation
  */ 
  
  public interface SrcTrgCmdState extends CmdState {
    public void setSourceTargAndDepth(String src, String trg, int depth, boolean longerOK);
  }
 
}   
  
