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

package org.systemsbiology.biotapestry.cmd;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.move.Mover;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/***************************************************************************
** 
** Commands accessed by network popup menus
*/

public class PopCommands {
  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  private Point2D popupPoint_;  
  private Intersection intersected_;
  private Point absScreen_;
  private Set<String> genes_;
  private Set<String> nodes_;
  private Set<String> links_;
  private UIComponentSource uics_;
  private DynamicDataAccessContext ddacx_;
  private FlowMeister flom_;
  private HarnessBuilder hBld_;
  private CmdSource cSrc_;

  private boolean singleSeg_;
  private boolean canSplit_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Constructor
  */
 
  public PopCommands(UIComponentSource uics, CmdSource cSrc, DynamicDataAccessContext ddacx, HarnessBuilder hBld) {
    cSrc_ = cSrc;
    flom_ = cSrc_.getFloM();
    uics_ = uics;
    ddacx_ = ddacx;
    hBld_ = hBld;   
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Set the popup point
  */
  
  public void setPopup(Point2D popupPoint) {
    popupPoint_ = popupPoint;
    return;
  }
  
  /***************************************************************************
  **
  ** Stash this for cursor warping
  */
  
  public void setAbsScreenPoint(Point absScreen) {
    absScreen_ = absScreen;
    return;
  }   
  
  /***************************************************************************
  **
  ** Set the intersection.  1/09: Start to use this instead of reintersecting
  ** the popup point!
   */
  
  public void setIntersection(Intersection intersect) {
    singleSeg_ = false;
    canSplit_ = false;
    intersected_ = intersect;
    return;
  }  
  
  /***************************************************************************
  **
  ** Do setup for popup selection of a link!
   */
  
  public void setupNormalLinkSelection() {
    singleSeg_ = true;
    canSplit_ = true;
    return;
  }
  
  /***************************************************************************
  **
  ** Do setup for main-menu selection of a link!
   */
  
  public void setupRemoteLinkSelection(StaticDataAccessContext rcx, Linkage link) {    
    String objID = link.getID();
    BusProperties linkProps = rcx.getCurrentLayout().getLinkProperties(objID);
    LinkSegmentID[] segIDs = intersected_.segmentIDsFromIntersect();
    singleSeg_ = ((segIDs != null) && (segIDs.length == 1));
    Point viewPoint = null;
    if (singleSeg_) {
      LinkSegmentID segID = segIDs[0];
      LinkSegment geomOnly = linkProps.getSegmentGeometryForID(segID, rcx, false);
      if (geomOnly.getLength() >= (UiUtil.GRID_SIZE * 3.0)) {
        Point2D segSplit  = geomOnly.pointAtFraction(0.5);
        viewPoint = rcx.getZoomTarget().pointToViewport(new Point((int)segSplit.getX(), (int)segSplit.getY()));        
        setPopup(viewPoint);
      }
    }
    canSplit_ = (viewPoint != null);
    return;
  }
  
  /***************************************************************************
  **
  ** for multi-selections
  */ 
      
  public void setMultiSelections(Set<String> genes, Set<String> nodes, Set<String> links) {
    genes_ = genes;
    nodes_ = nodes;
    links_ = links;
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////                           
   
  /***************************************************************************
  **
  ** Action Wrappers around the flows
  */ 
  
  public PopAction getAction(FlowMeister.PopFlow key, ControlFlow.OptArgs args) {  
    PopAction retval; 
    switch (key) {  
      case SELECT_LINKS_TOGGLE:
      case SELECT_QUERY_NODE_TOGGLE:
      case APPEND_TO_CURRENT_SELECTION_TOGGLE:
        retval = new ToggleFlowPopAction((ControlFlow.FlowForPopToggle)flom_.getControlFlow(key, args));
        break;
      case MOVE_NET_MODULE_REGION:
        retval = new PopAction(flom_.getControlFlow(key, args));
        retval.setEnabled(((Mover.MoveNetModuleRegionArgs)args).getIsEnabled());
        break; 
      default:
        retval = new PopAction(flom_.getControlFlow(key, args));
        break;
    }
    return (retval);
  }
  

    
  ////////////////////////////////////////////////////////////////////////////
  //
  // INTERFACES
  //
  ////////////////////////////////////////////////////////////////////////////   

  /***************************************************************************
  **
  ** Interface for commands that support toggles:
  */
    
  public interface ToggleSupport  {
    public void setToUpdate(boolean sfu);
    public boolean shouldCheck();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC ABSTRACT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////   

  /***************************************************************************
   **
   ** Base class for these commands
   */
     
  public class PopAction extends AbstractAction {     
    private static final long serialVersionUID = 1L;
  
    protected ControlFlow myControlFlow;
    private String name_;
    private Character mNem_;

    public PopAction(ControlFlow theFlow) {   
      ResourceManager rMan = ddacx_.getRMan();
      name_ = rMan.getString(theFlow.getName());
      putValue(Action.NAME, name_);
      String mnem = theFlow.getMnem();
      if (mnem != null) {
        char mnemC = rMan.getChar(mnem);
        mNem_ = new Character(mnemC);
        putValue(Action.MNEMONIC_KEY, new Integer(mnemC)); 
      }
      myControlFlow = theFlow;
    }
       
    public String getName() {
      return (name_);
    }
    
    public Character getMnem() {
      return (mNem_);        
    } 

    protected boolean preCheck() {
      return (true);       
    }
     
    public void actionPerformed(ActionEvent e) {
      try {
        if (!preCheck()) {
          return;
        }
        HarnessBuilder.PreHarness pH = hBld_.buildHarness(myControlFlow);
        DialogAndInProcessCmd.CmdState pre = pH.getCmdState();
        if (pre instanceof DialogAndInProcessCmd.PopupCmdState) {
          DialogAndInProcessCmd.PopupCmdState pcs = (DialogAndInProcessCmd.PopupCmdState)pre;
          pcs.setIntersection(intersected_);
        }
        if (pre instanceof DialogAndInProcessCmd.PopupPointCmdState) {
          DialogAndInProcessCmd.PopupPointCmdState ppcs = (DialogAndInProcessCmd.PopupPointCmdState)pre;
          ppcs.setPopupPoint(popupPoint_);
        }   
        if (pre instanceof DialogAndInProcessCmd.AbsolutePopupPointCmdState) {
          DialogAndInProcessCmd.AbsolutePopupPointCmdState appcs = (DialogAndInProcessCmd.AbsolutePopupPointCmdState)pre;
          appcs.setAbsolutePoint(absScreen_);
        }
        if (pre instanceof DialogAndInProcessCmd.MultiSelectCmdState) {
          DialogAndInProcessCmd.MultiSelectCmdState mscs = (DialogAndInProcessCmd.MultiSelectCmdState)pre;
          mscs.setMultiSelections(genes_, nodes_, links_);
        }
        
        hBld_.runHarness(pH);     
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;
    }
     
    public boolean isValid() {
      StaticDataAccessContext rcx = new StaticDataAccessContext(ddacx_);
      return (myControlFlow.isValid(intersected_, singleSeg_, canSplit_, rcx, uics_));       
    }   
   }
   
  /***************************************************************************
  **
  ** Support for flows that toggle state
  */
  
  class ToggleFlowPopAction extends PopAction implements ToggleSupport {
    private static final long serialVersionUID = 1L;
    
    private boolean ignore_;
    
    public ToggleFlowPopAction(ControlFlow.FlowForPopToggle theFlow) {      
      super((ControlFlow)theFlow);
      ignore_ = false;
    }

    @Override
    protected boolean preCheck() {
      return (!ignore_);       
    }      

    public boolean shouldCheck() {
      return (((ControlFlow.FlowForPopToggle)myControlFlow).shouldCheck(cSrc_));
    }
    
    public void setToUpdate(boolean ignore) {
      ignore_ = ignore;
      return;
    }
  }
}
