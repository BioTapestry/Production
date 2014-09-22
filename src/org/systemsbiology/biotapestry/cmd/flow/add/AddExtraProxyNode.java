/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.cmd.flow.add;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.ProxyChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.genome.ProxyChange;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.IntersectionChooser;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle adding extra proxy node
*/

public class AddExtraProxyNode extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AddExtraProxyNode(BTState appState) {
    super(appState);
    name = "command.AddExtraProxyNode";
    desc = "command.AddExtraProxyNode";
    mnem = "command.AddExtraProxyNodeMnem";
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
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.isDynamicInstance());
  }
    
  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
 
    DialogAndInProcessCmd next;
    while (true) {
      StepState ans;
      if (last == null) {
        ans = new StepState(appState_, cfh.getDataAccessContext()); 
      } else {
        ans = (StepState)last.currStateX;
      }
      
      if (ans.getNextStep().equals("stepSetToMode")) {
        next = ans.stepSetToMode();      
      } else if (ans.getNextStep().equals("stepDoExtraProxyNodeAdd")) {   
        next = ans.stepDoExtraProxyNodeAdd();  
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
  ** Called as new X, Y info comes in to place the node.
  */
  
  @Override
  public DialogAndInProcessCmd processClick(Point theClick, boolean isShifted, double pixDiam, DialogAndInProcessCmd.CmdState cmds) {
    StepState ans = (StepState)cmds;
    ans.x = UiUtil.forceToGridValueInt(theClick.x, UiUtil.GRID_SIZE);
    ans.y = UiUtil.forceToGridValueInt(theClick.y, UiUtil.GRID_SIZE);
    ans.rcxT_.pixDiam = pixDiam;
    DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans);
    ans.nextStep_ = "stepDoExtraProxyNodeAdd"; 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Running State:
  */
        
  public static class StepState implements DialogAndInProcessCmd.CmdState, DialogAndInProcessCmd.MouseClickCmdState {
     
    private DataAccessContext rcxT_;
    private int x;
    private int y;
    private String nextStep_;   
    private BTState appState_;

    /***************************************************************************
    **
    ** Constructor
    */
     
    private StepState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      rcxT_ = dacx;
      nextStep_ = "stepSetToMode";
    }
    
    /***************************************************************************
    **
    ** Next step...
    */ 
      
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** mouse masking
    */
    
    public int getMask() {
      return (MainCommands.ALLOW_NAV_PUSH);
    }
    
    public boolean pushDisplay() {
      return (false);
    }
    
    public boolean noSubModels() {
      return (false);
    }
        
    public boolean noInstances() {
      return (false);
    }
            
    public boolean showBubbles() {
      return (false);
    }
    
    public boolean noRootModel() {
      return (true);
    }
    
    public boolean mustBeDynamic() {
      return (true);
    }
     
    public boolean cannotBeDynamic() {
      return (false);
    }
    
    public boolean hasTargetsAndOverlays() {
      return (false);
    }
    
    public Floater floaterMode() {
      return (DialogAndInProcessCmd.MouseClickCmdState.Floater.NO_FLOATER);
    } 
    
    public void setFloaterPropsInLayout(Layout flay) {
      throw new IllegalStateException();
    }
  
    public Object getFloater(int x, int y) {
      throw new IllegalStateException();
    }
    
    public Color getFloaterColor() {
      throw new IllegalStateException();
    }
  
    /***************************************************************************
    **
    ** Install a mouse handler
    */
     
    private DialogAndInProcessCmd stepSetToMode() {       
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      retval.suPanelMode = PanelCommands.Mode.ADD_EXTRA_PROXY_NODE;
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Do the selection
    */
          
    private DialogAndInProcessCmd stepDoExtraProxyNodeAdd() {     
      //
      // FIX ME!  Clicking on signal nodes to add them as extra proxy nodes tends to
      // hit the links instead->get an intersection failure.  Make the intersection test
      // smarter!  07/27/09
      //
      
      if (rcxT_.getGenome() instanceof DynamicGenomeInstance) {
        GenomeInstance parent = rcxT_.getGenomeAsInstance().getVfgParent();
        if (parent != null) {
          List<Intersection> itemList = appState_.getGenomePresentation().selectFromParent(x, y, rcxT_);
          Intersection inter = (new IntersectionChooser(true, rcxT_).intersectionRanker(itemList));
          if (inter == null) {
            return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.UNSELECTED, this));
          }
          String startID = inter.getObjectID();
          NodeInstance subsetNode = (NodeInstance)parent.getNode(startID);
          if (subsetNode == null) {
            return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.UNSELECTED, this));
          }
          //
          // We do not allow nodes that belong to groups included in the instance
          // to be force added.  They are already being driven by data tables.
          // We also require children of dynamic parents to only include nodes
          // that are themselves extra nodes. (FIX ME? - Too restrictive)  
          //
          if (parent instanceof DynamicGenomeInstance) {
            String parentProxID = ((DynamicGenomeInstance)parent).getProxyID();
            DynamicInstanceProxy parProx = rcxT_.getGenomeSource().getDynamicProxy(parentProxID);
            if (parProx.getGroupForExtraNode(startID) == null) {
              return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.UNSELECTED, this));
            }
          } else {
            Group nodeGroup = parent.getGroupForNode(startID, GenomeInstance.LEGACY_MODE);
            if (nodeGroup != null) {  // if null->included by way of extra proxy node, i.e. OK 
              int generation = parent.getGeneration();
              String myID = Group.buildInheritedID(nodeGroup.getID(), generation + 1);
              Group myGroup = rcxT_.getGenomeAsInstance().getGroup(myID);         
              if (myGroup != null) {
                String activeID = myGroup.getActiveSubset();
                if (activeID == null) {  // actual group is in instance, so cannot include.
                  return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.UNSELECTED, this));
                } else { // child is active.  Cannot include if we are part of child group in parent instance
                  String mysubID = Group.buildInheritedID(activeID, generation);
                  if (parent.getGroup(mysubID).isInGroup(startID, parent)) {
                    return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.UNSELECTED, this));
                  }
                }
              }
            }
          }

          String proxID = ((DynamicGenomeInstance)rcxT_.getGenome()).getProxyID();
          DynamicInstanceProxy prox = rcxT_.getGenomeSource().getDynamicProxy(proxID);
          if (prox.hasAddedNode(startID)) {  // Already present
            return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ERROR, this));
          }

          //
          // Get the group choice.  Depends on what our parent is:
          //
          String useGroupID = null;
          if (parent instanceof DynamicGenomeInstance) {
            String parentProxID = ((DynamicGenomeInstance)parent).getProxyID();
            DynamicInstanceProxy parProx = rcxT_.getGenomeSource().getDynamicProxy(parentProxID);
            useGroupID = parProx.getGroupForExtraNode(startID);
          } else {
            // 3-25-08 Could be returned as null, though MAIN_GROUP_AS_FALLBACK arg should reduce that.
            Group parGroup = parent.getGroupForNode(startID, GenomeInstance.MAIN_GROUP_AS_FALLBACK);
            if (parGroup != null) {
              useGroupID = parGroup.getID(); 
            }
            //
            // If our parent is a static group, make the group choice combo:
            //

            Object defObj = null;
            Iterator<Group> grit = parent.getGroupIterator();
            ArrayList<ObjChoiceContent> choices = new ArrayList<ObjChoiceContent>();
            while (grit.hasNext()) {
              Group group = grit.next();
              String groupID = group.getID();
              ObjChoiceContent ccont = new ObjChoiceContent(group.getDisplayName(), Group.getBaseID(groupID));
              choices.add(ccont);
              if (groupID.equals(useGroupID)) {
                defObj = ccont;
              } 
            }

            ResourceManager rMan = appState_.getRMan();
            Object piggyGroup = JOptionPane.showInputDialog(appState_.getTopFrame(), 
                                                 rMan.getString("addExtraProxyNode.PiggyBackGroup"), 
                                                 rMan.getString("addExtraProxyNode.Title"),
                                                 JOptionPane.QUESTION_MESSAGE, null,
                                                 choices.toArray(),
                                                 (defObj == null) ? choices.get(0) : defObj);
            if (piggyGroup == null) {
              return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.CANCELLED, this));
            }
            useGroupID = ((ObjChoiceContent)piggyGroup).val;
          }
          addExtraProxyNode(subsetNode, useGroupID);
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
        }
      }
      throw new IllegalArgumentException(); 
    }
   
    /***************************************************************************
    **
    ** Add an extra proxy node.
    */  
   
    private boolean addExtraProxyNode(NodeInstance useNode, String groupToUse) {
                                        
      Genome genome = rcxT_.getGenome();
      if (!(genome instanceof DynamicGenomeInstance)) {   
        throw new IllegalArgumentException();    
      }
      DynamicGenomeInstance dgi = (DynamicGenomeInstance)genome;
      DynamicInstanceProxy dip = rcxT_.getGenomeSource().getDynamicProxy(dgi.getProxyID());
   
      DynamicInstanceProxy.AddedNode added = new DynamicInstanceProxy.AddedNode(useNode.getID(), groupToUse);
      ProxyChange pc = dip.addExtraNode(added);
      if (pc != null) {
        ProxyChangeCmd pcc = new ProxyChangeCmd(appState_, rcxT_, pc);
        UndoSupport support = new UndoSupport(appState_, "undo.addExtraProxyNode");        
        support.addEdit(pcc);
        support.addEvent(new ModelChangeEvent(genome.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
        support.finish();
      } 
      return (true); 
    }   
  }
}
