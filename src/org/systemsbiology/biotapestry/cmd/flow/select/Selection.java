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

package org.systemsbiology.biotapestry.cmd.flow.select;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.VisualChangeResult;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.Note;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.TextBoxManager;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Handle various selection operations
*/

public class Selection extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum SelectAction {
    ALL("command.SelectAll", "command.SelectAll", "FIXME24.gif", "command.SelectAllMnem", "command.SelectAllAccel"),
    NONE("command.SelectNone", "command.SelectNone", "ClearSelected24.gif", "command.SelectNoneMnem", "command.SelectNoneAccel"),
    DROP_NODES("command.DropNodeSelections", "command.DropNodeSelections", "FIXME24.gif", "command.DropNodeSelectionsMnem", null),
    DROP_LINKS("command.DropLinkSelections", "command.DropLinkSelections", "FIXME24.gif", "command.DropLinkSelectionsMnem", null),
    SELECT_NON_ORTHO("command.SelectNonOrthoLinkSegments", "command.SelectNonOrthoLinkSegments", "FIXME24.gif", "command.SelectNonOrthoLinkSegmentsMnem", null),
    SELECT_FULL_LINK("linkPopup.SelectAll", "linkPopup.SelectAll", "FIXME24.gif", "linkPopup.SelectAllMnem", null),
    SELECT_ROOT_ONLY_NODE("command.SelectRootOnlyNode", "command.SelectRootOnlyNode", "FIXME24.gif", "command.SelectRootOnlyNodeMnem", null),
    SELECT_ROOT_ONLY_LINK("command.SelectRootOnlyLink", "command.SelectRootOnlyLink", "FIXME24.gif", "command.SelectRootOnlyLinkMnem", null),
    REMOTE_BATCH_SELECT(),
    LOCAL_SINGLE_CLICK_SELECT(),
    LOCAL_SELECTION_BY_RECT(),
    DROP_NODE_TYPE();
     
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    SelectAction(String name, String desc, String icon, String mnem, String accel) {
      this.name_ = name;  
      this.desc_ = desc;
      this.icon_ = icon;
      this.mnem_ = mnem;
      this.accel_ = accel;
    }  
     
    SelectAction() {
      this.name_ = null;  
      this.desc_ = null;
      this.icon_ = null;
      this.mnem_ = null;
      this.accel_ = null;
    }  
         
    public String getName() {
      return (name_);      
    }
     
    public String getDesc() {
      return (desc_);      
    }
     
    public String getIcon() {
      return (icon_);      
    }
     
    public String getMnem() {
      return (mnem_);      
    }
     
    public String getAccel() {
      return (accel_);      
    }     
 
    public String getDesc(BTState appState, String text) {
      ResourceManager rMan = appState.getRMan();
      String desc = MessageFormat.format(rMan.getString("command.DropNodeTypeSelections"), 
                                                         new Object[] {text});  
      return (desc);     
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private SelectAction action_;
  private Integer type_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public Selection(BTState appState, SelectAction action) {
    super(appState);
    if (action.equals(SelectAction.DROP_NODE_TYPE)) {
      throw new IllegalArgumentException();
    }
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
   
  public Selection(BTState appState, SelectAction action, TypeArg args) {
    super(appState);
    name =  action.getDesc(appState, args.getTag());
    desc =  action.getDesc(appState, args.getTag());
    icon =  null;
    mnem =  null;
    accel =  null;
    action_ = action;
    type_ = args.getType();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
     
   public boolean isEnabled(CheckGutsCache cache) {
     switch (action_) {
       case ALL:
         // FIX ME!!  Gotta check after a deletion from the model!
         return (cache.genomeNotEmpty());
       case NONE:
         return (cache.haveASelection());
       case DROP_NODES:
         // FIX ME!!  Gotta check after a selection change!
         return (cache.genomeNotEmpty());
       case DROP_LINKS:
         // FIX ME!!  Gotta check after a selection change!
         return (cache.genomeNotEmpty());
       case SELECT_NON_ORTHO:
         return (cache.genomeNotNull());
       case DROP_NODE_TYPE:
         return (true);
       case SELECT_ROOT_ONLY_NODE:
       case SELECT_ROOT_ONLY_LINK:
         if (!cache.genomeIsRoot()) {
           return (false);
         }
         if (!cache.genomeNotEmpty()) {
           return (false);
         }
         if (action_ == SelectAction.SELECT_ROOT_ONLY_LINK) {
           return (cache.canLayoutLinks()); // Not exactly right, but dynamic genome instances already culled above.
         }
         return (true);
       case SELECT_FULL_LINK:  // for popup only
       case REMOTE_BATCH_SELECT:
       case LOCAL_SINGLE_CLICK_SELECT:
       case LOCAL_SELECTION_BY_RECT:
       default:
         throw new IllegalStateException();
     }
   }
   
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
    
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) {
    if (!isSingleSeg) {
      return (false);
    }
    if (action_ == SelectAction.SELECT_FULL_LINK) {
      return (true);
    } else {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    StepState retval = new StepState(appState_, action_, null, dacx);
    return (retval);
  }
   
  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      StepState ans;
      if (last == null) {
        ans = new StepState(appState_, action_, type_, cfh.getDataAccessContext());
      } else { 
        ans = (StepState)last.currStateX;
      }
      if (ans.getNextStep().equals("stepSelect")) {
        next = ans.stepSelect();
      } else {
        throw new IllegalStateException();
      }
      if (!next.state.keepLooping()) {
        return (next);
      }
      last = next;
    }
  }
  
  /***************************************************************************
  **
  ** Final visualization step
  ** 
  */
  
  @Override
  public VisualChangeResult visualizeResults(DialogAndInProcessCmd last) {
    if (last.state != DialogAndInProcessCmd.Progress.DONE) {
      throw new IllegalStateException();
    }
    StepState ans = (StepState)last.currStateX;
    return (ans.generateVizResult());
  }
  
  /***************************************************************************
  **
  ** Running State
  */
        
  public static class StepState implements DialogAndInProcessCmd.PopupCmdState {
    
    private Intersection intersect_;
    private SelectAction myAction_;
    private Integer myType_;
    private String nextStep_;    
    private BTState appState_;
    private Point preloadClick_;
    private boolean preloadIsShifted_; 
    private List<Intersection> preloadIntersections_; 
    private Rectangle preloadRect_;
    private DataAccessContext rcxT_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, SelectAction action, Integer type, DataAccessContext dacx) {
      myType_ = type;
      appState_ = appState;
      myAction_ = action;
      nextStep_ = "stepSelect";
      rcxT_ = dacx;
    }
    
    /***************************************************************************
    **
    ** Set params
    */ 
        
    public void setPreload(Point point, double pixDiam, boolean isShifted) {
      preloadClick_ = point;
      rcxT_.pixDiam = pixDiam;
      preloadIsShifted_ = isShifted;
      return;
    }
    
    /***************************************************************************
    **
    ** Set params
    */ 
        
    public void setPreload(Rectangle rect, boolean isShifted) {
      preloadRect_ = rect;
      preloadIsShifted_ = isShifted;
      return;
    }
    
    /***************************************************************************
    **
    ** Set params
    */ 
        
    public void setIntersections(List<Intersection> inters) {
      preloadIntersections_ = inters;
      return;
    }  
    
    /***************************************************************************
    **
    ** Set params
    */ 
        
    public void setIntersection(Intersection intersect) {
      intersect_ = intersect;
      return;
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepSelect() {
      switch (myAction_) {
        case ALL:        
          appState_.getSUPanel().selectAll(appState_.getUndoManager(), rcxT_);
          break;
        case NONE:
          appState_.getSUPanel().selectNone(appState_.getUndoManager(), rcxT_);
          break;
        case DROP_NODES:
          appState_.getSUPanel().dropNodeSelections(null, appState_.getUndoManager(), rcxT_);
          break;
        case DROP_LINKS:
          appState_.getSUPanel().dropLinkSelections(false, appState_.getUndoManager(), rcxT_);
          break;
        case SELECT_NON_ORTHO:
          List<Intersection> nonOrtho = rcxT_.getLayout().getNonOrthoIntersections(rcxT_, null);
          appState_.getGenomePresentation().selectIntersectionList(nonOrtho, rcxT_, appState_.getUndoManager());
          appState_.getSUPanel().drawModel(false);
          appState_.getZoomCommandSupport().zoomToSelected();
          break;
        case DROP_NODE_TYPE:
          appState_.getSUPanel().dropNodeSelections(myType_, appState_.getUndoManager(), rcxT_);
          break;
        case REMOTE_BATCH_SELECT:
          appState_.getGenomePresentation().selectIntersectionList(preloadIntersections_, rcxT_, appState_.getUndoManager());
          appState_.getSUPanel().drawModel(false);
        case LOCAL_SINGLE_CLICK_SELECT:   
          singleClick();
          break;   
        case LOCAL_SELECTION_BY_RECT:  
          appState_.getGenomePresentation().selectItems(preloadRect_, rcxT_, preloadIsShifted_, appState_.getUndoManager());
          break;
        case SELECT_ROOT_ONLY_NODE: 
          Set<String> onlyRootNodes = rcxT_.fgho.findRootOnlyNodes();
          List<Intersection> onlyNodes = Intersection.nodeIDsToInters(onlyRootNodes);
          appState_.getGenomePresentation().selectIntersectionList(onlyNodes, rcxT_, appState_.getUndoManager());
          break;
        case SELECT_ROOT_ONLY_LINK:      
          Set<String> onlyRootLinks = rcxT_.fgho.findRootOnlyLinks();
          List<Intersection> onlyLinks = Intersection.linkIDsToInters(onlyRootLinks, rcxT_.getDBGenome().getID(), rcxT_);
          appState_.getGenomePresentation().selectIntersectionList(onlyLinks, rcxT_, appState_.getUndoManager());
          break;
        case SELECT_FULL_LINK:
          appState_.getGenomePresentation().selectFullItem(intersect_, rcxT_, appState_.getUndoManager());
          break;
        default:
          throw new IllegalStateException();
      }
      appState_.getSUPanel().drawModel(false); // Duplicated, but some (e.g. ALL) are not getting redrawn....
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
    
    /***************************************************************************
    **
    ** Code that used to be in the default click handler is now here
    */ 
       
    private void singleClick() {        
      if (rcxT_.getGenome() != null) {
        Intersection intersect = null;
        intersect = appState_.getGenomePresentation().selectItem(preloadClick_.x, preloadClick_.y, rcxT_,
                                                                 preloadIsShifted_, false, appState_.getUndoManager());
        String currNoteID = null;
        if (intersect != null) {
          String itemID = intersect.getObjectID();
          Note note = rcxT_.getGenome().getNote(itemID);
          if ((note != null) && note.isInteractive()) {
            currNoteID = itemID;
          }
        }
        appState_.getGenomePresentation().setFloater(null);
        if (currNoteID != null) {
          appState_.getTextBoxMgr().setMessageSource(currNoteID, TextBoxManager.SELECTED_ITEM_MESSAGE, false);
        } else {
          appState_.getTextBoxMgr().clearMessageSource(TextBoxManager.SELECTED_ITEM_MESSAGE);
        }
        appState_.getSUPanel().drawModel(false);
      }
      return;
    }
        
    /***************************************************************************
    **
    ** This is the way the selection results are SUPPOSED to be visualized; the selection list is
    ** returned to the control flow harness to paint. But the shift to this new technique is incomplete.
    ** The above steps all end up with the system handling the new presentation. But we should do it this way.
    ** Note that currently the WesServerApplication is always asking for the current selection state after the
    ** every command.
    */

    VisualChangeResult generateVizResult() {
      return (null); // < Change this when the above step results do not change the selection presentation state under the covers.
    }
  }
  
  /***************************************************************************
  **
  ** Control Flow Argument
  */  

  public static class TypeArg extends AbstractOptArgs {
      
    private static String[] keys_;
    private static Class<?>[] classes_;
    
    static {
      keys_ = new String[] {"type", "tag"};  
      classes_ = new Class<?>[] {Integer.class, String.class};  
    }
    
    public int getType() {
      return (Integer.parseInt(getValue(0)));
    }
    
    public String getTag() {
      return (getValue(1));
    }

    public TypeArg(Map<String, String> argMap) throws IOException {
      super(argMap);
    }
  
    protected String[] getKeys() {
      return (keys_);
    }
    
    @Override
    protected Class<?>[] getClasses() {
      return (classes_);
    }
    
    public TypeArg(Integer type, String tag) {
      super();
      setValue(0, Integer.toString(type));
      setValue(1, tag);
      bundle();
    }
  }  
}
