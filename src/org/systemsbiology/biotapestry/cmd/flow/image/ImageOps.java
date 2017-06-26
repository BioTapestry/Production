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

package org.systemsbiology.biotapestry.cmd.flow.image;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveSupport;
import org.systemsbiology.biotapestry.cmd.undo.ImageChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.event.TreeNodeChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.nav.ImageChange;
import org.systemsbiology.biotapestry.nav.ImageManager;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.util.FileExtensionFilters;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Collection of Image add and drop actions
*/

public class ImageOps extends AbstractControlFlow {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum ImageAction {
    DROP("command.DropImageFromModel", "command.DropImageFromModel", "FIXME24.gif", "command.DropImageFromModelMnem", null),
    ADD("command.AssignImageToModel", "command.AssignImageToModel", "FIXME24.gif", "command.AssignImageToModelMnem", null),
    GROUPING_NODE_ADD("command.AssignImageToGroupingNode", "command.AssignImageToGroupingNode", "FIXME24.gif", "command.AssignImageToGroupingNodeMnem", null),
    GROUPING_NODE_DROP("command.DropImageFromGroupingNode", "command.DropImageFromGroupingNode", "FIXME24.gif", "command.DropImageFromGroupingNodeMnem", null),
    GROUPING_NODE_ADD_MAP("command.AssignMapImageToGroupingNode", "command.AssignMapImageToGroupingNode", "FIXME24.gif", "command.AssignMapImageToGroupingNodeMnem", null),
    GROUPING_NODE_DROP_MAP("command.DropMapImageFromGroupingNode", "command.DropMapImageFromGroupingNode", "FIXME24.gif", "command.DropMapImageFromGroupingNodeMnem", null),   
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    ImageAction(String name, String desc, String icon, String mnem, String accel) {
      this.name_ = name;  
      this.desc_ = desc;
      this.icon_ = icon;
      this.mnem_ = mnem;
      this.accel_ = accel;
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
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private ImageAction action_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ImageOps(ImageAction action) {
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** Answer if we are enabled for a model tree case
  ** 
  */
  
  @Override
  public boolean isTreeEnabled(XPlatModelNode.NodeKey key, DataAccessContext rcx, UIComponentSource uics) {
    NavTree nt = rcx.getGenomeSource().getModelHierarchy();
    TreeNode node = nt.resolveNode(key);
    if (!uics.getIsEditor() || (key == null)) {
      return (false);
    }
    switch (action_) {
      case DROP:
        if ((key.modType == XPlatModelNode.ModelType.GROUPING_ONLY) || (key.modType == XPlatModelNode.ModelType.SUPER_ROOT)) {
          return (false);
        }
        if (key.modType == XPlatModelNode.ModelType.DYNAMIC_PROXY) {
          if (!nodeIsSelectedDPNode(key, rcx, uics)) {
            return (false);
          }
          UiUtil.fixMePrintout("no, if no image for current slider setting, cannot drop");
          return (rcx.getGenomeSource().getDynamicProxy(key.id).hasGenomeImage());
        }
        return (rcx.getGenomeSource().getGenome(key.id).getGenomeImage() != null);
      case ADD:
        if (key.modType == XPlatModelNode.ModelType.DYNAMIC_PROXY) {         
          return (nodeIsSelectedDPNode(key, rcx, uics));
        }
        return ((key.modType != XPlatModelNode.ModelType.GROUPING_ONLY) && (key.modType != XPlatModelNode.ModelType.SUPER_ROOT));
      case GROUPING_NODE_ADD:
        return (key.modType == XPlatModelNode.ModelType.GROUPING_ONLY);  
      case GROUPING_NODE_ADD_MAP:
        if (key.modType != XPlatModelNode.ModelType.GROUPING_ONLY) {
          return (false);
        }
        return (nt.nodeHasGroupImage(node, false));
      case GROUPING_NODE_DROP:
        if (key.modType != XPlatModelNode.ModelType.GROUPING_ONLY) {
          return (false);
        }
        return (nt.nodeHasGroupImage(node, false) && !nt.nodeHasGroupImage(node, true));      
      case GROUPING_NODE_DROP_MAP:
        if (key.modType != XPlatModelNode.ModelType.GROUPING_ONLY) {
          return (false);
        }
        return (nt.nodeHasGroupImage(node, true));
      default:
        throw new IllegalStateException();
    }
  }
    
  /***************************************************************************
   **
   ** For programmatic preload
   ** 
   */ 
    
   @Override
   public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
     StepState retval = new StepState(action_, dacx);
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
        throw new IllegalStateException();
      } else {
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepToProcess")) {
          next = ans.stepToProcess();
        } else {
          throw new IllegalStateException();
        }
      }
      if (!next.state.keepLooping()) {
        return (next);
      }
      last = next;
    }
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** User can bring up popup menu and do things even if the node is not
  ** the currently seleted model. With slider models, this is more confusing
  ** than helpful, since slider state is important. We do not allow add or
  ** drops if selected node is not the source of the popup:
  ** 
  */ 
  
  private boolean nodeIsSelectedDPNode(XPlatModelNode.NodeKey key, DataAccessContext rcx, UIComponentSource uics) {
    NavTree nt = rcx.getGenomeSource().getModelHierarchy();
    TreeNode node = nt.resolveNode(key); 
    String proxyID = nt.getDynamicProxyID(node);
    if (proxyID == null) {
      return (false);
    }
    // Note the "current genome" in the rcx is for the node, not the current selection...
    VirtualModelTree vmt = uics.getTree();
    TreePath tp = vmt.getTreeSelectionPath();
    String currDPI = nt.getDynamicProxyID(tp);
    if (currDPI == null) {
      return (false);
    }
    return (currDPI == proxyID);
  }
  
  
 
  /***************************************************************************
  **
  ** Running State
  */
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.ModelTreeCmdState {

    private ImageAction myAction_;
    private LoadSaveSupport myLsSup_;
    private Genome popupModel_;
    private TreeNode popupNode_;
    private ImageManager mgr_;
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ImageAction action, StaticDataAccessContext dacx) {
      super(dacx);
      myAction_ = action;
      nextStep_ = "stepToProcess";
      //myLsSup_ = uics_.getLSSupport(); must call stockCfhIfNeeded
      //mgr_ = uics_.getImageMgr(); must call stockCfhIfNeeded
    }
    
    /***************************************************************************
    **
    ** Add cfh in if StepState was pre-built
    */
     
    @Override
    public void stockCfhIfNeeded(ServerControlFlowHarness cfh) {
      if (cfh_ != null) {
        return;
      }
      super.stockCfhIfNeeded(cfh);
      myLsSup_ = uics_.getLSSupport();
      mgr_ = uics_.getImageMgr();
      return;
    }
 
    /***************************************************************************
    **
    ** for preload
    */ 
   
    public void setPreload(Genome popupModel, Genome popupModelAncestor, TreeNode popupNode) {
      popupModel_ = popupModel; // May be null if popup is a group node
      popupNode_ = popupNode;
      return;
    }

    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {          
      switch (myAction_) {
        case ADD: 
          return (addAction());
        case DROP: 
          return (dropAction());
        case GROUPING_NODE_ADD:
          return (addActionForGroup(false));
        case GROUPING_NODE_DROP:
          return (dropActionForGroup(false));
        case GROUPING_NODE_ADD_MAP:
          return (addActionForGroup(true));
        case GROUPING_NODE_DROP_MAP:      
          return (dropActionForGroup(true));          
        default:
          throw new IllegalStateException();
      }
    }

    /***************************************************************************
    **
    ** Command
    */ 

    private ImageLoad loadImage(int warnHeight, int warnWidth) {

      ResourceManager rMan = dacx_.getRMan();
      
      List<String> supported = mgr_.getSupportedFileSuffixes();
      FileExtensionFilters.MultiExtensionFilter filt = new FileExtensionFilters.MultiExtensionFilter(dacx_, supported, "filterName.img");
      File file = myLsSup_.getFprep(dacx_).getExistingImportFile("LoadImageDirectory", filt);
      if (file == null) {
        return (new ImageLoad(null, new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this)));
      }
     
      try {
        ImageManager.TypedImage ti = mgr_.loadImageFromFileStart(file);
        if ((ti.getHeight() < 1) || (ti.getWidth() < 1)) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        rMan.getString("assignImage.zeroImageMessage"), 
                                        rMan.getString("assignImage.errorMessageTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (new ImageLoad(null, new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this)));
        }
        if ((ti.getHeight() > warnHeight) || (ti.getWidth() > warnWidth)) {
          String format = rMan.getString("assignImage.bigImageMessageFormat");
          String formMsg = MessageFormat.format(format, new Object[] {Integer.valueOf(ti.getWidth()),
                                                                      Integer.valueOf(ti.getHeight()),
                                                                      Integer.valueOf(warnWidth),                    
                                                                      Integer.valueOf(warnHeight)});
          formMsg = "<html><center>" + formMsg + "</center></html>";
          formMsg = formMsg.replaceAll("\n", "<br>");
          int ok = JOptionPane.showConfirmDialog(uics_.getTopFrame(), formMsg,
                                                 rMan.getString("assignImage.warningMessageTitle"),
                                                 JOptionPane.YES_NO_OPTION);
          if (ok != JOptionPane.YES_OPTION) {
            return (new ImageLoad(null, new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this)));
          }
        }          

        ImageManager.NewImageInfo nii = mgr_.loadImageFromFileFinish(ti);
        myLsSup_.getFprep(dacx_).setPreference("LoadImageDirectory", file.getAbsoluteFile().getParent());
        return (new ImageLoad(nii, null)); 
      } catch (IOException ioex) {
        myLsSup_.getFprep(dacx_).getFileInputError(ioex).displayFileInputError();
        return (new ImageLoad(null, new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this)));
      }  
    }

    /***************************************************************************
    **
    ** Command
    */ 

    private DialogAndInProcessCmd addAction() {
    
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
      
      int warnHeight = ImageManager.WARN_DIM_Y;
      int warnWidth = ImageManager.WARN_DIM_X;
      
      ImageLoad il = loadImage(warnHeight, warnWidth);
      if (il.daipc != null) {
        return (il.daipc);  
      }
      ImageManager.NewImageInfo nii = il.nii;
          
      if (nii.key != null) {
        String genomeID = nt.getGenomeID(popupNode_);
        String proxyID = nt.getDynamicProxyID(popupNode_);
            
        if (proxyID != null) { 
          if (genomeID != null) {
            throw new IllegalStateException();
          }
          DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID);
          VirtualModelTree vmt = uics_.getTree();
          TreePath tp = vmt.getTreeSelectionPath();
          String currDPI = nt.getDynamicProxyID(tp);
          int time;
          if ((currDPI != null) && (currDPI == proxyID)) {
            String timeStr = DynamicInstanceProxy.extractTime(popupModel_.getID());
            try {
              time = Integer.valueOf(timeStr);
            } catch (NumberFormatException nfex) {
              throw new IllegalStateException();
            }
          } else { // We are NOT looking at the model we are adding image to. Not legal (should be disabled)
            throw new IllegalStateException();
          }
          genomeID = dip.getKeyForTime(time, true);
        }
        
        DataAccessContext dacx4u = new StaticDataAccessContext(dacx_, genomeID);    
        UndoSupport support = uFac_.provideUndoSupport("undo.setImage", dacx4u);
        UiUtil.fixMePrintout("Undo of image assignment not closing model image panel if group node has image");
        UiUtil.fixMePrintout("Because proxy still has image key");
        UiUtil.fixMePrintout("Gotta set change.proxyKey to drop image from proxy? No, that does not work");


        // This was causing a crash:
        UiUtil.fixMePrintout("But how do we get image manager to e.g. drop fresh image??");
  //      nii.change.genomeKey = genomeID; // All undo processing goes through genome
  //      support.addEdit(new ImageChangeCmd(dacx4u, nii.change));
  
        // Note that this handles proxies as well, since the genomeID an represent a dynamic instance:
        
        ImageChange[] ics = dacx_.getGenomeSource().getGenome(genomeID).setGenomeImage(mgr_, nii.key);
        
        if (ics != null) {
          int numic = ics.length;
          for (int i = 0; i < numic; i++) {
            support.addEdit(new ImageChangeCmd(dacx4u, ics[i]));
          }
        }    
        support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), genomeID, ModelChangeEvent.PROPERTY_CHANGE));   
        support.finish();
      }  
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
     **
     ** Build time string
     */
     
     private String timeValToString(int val) { 
       TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
       boolean namedStages = tad.haveNamedStages();
       String displayUnits = tad.unitDisplayString();       
       String formatKey = (tad.unitsAreASuffix()) ? "grpColMatEdit.nameFormat" : "grpColMatEdit.nameFormatPrefix";
       String format = dacx_.getRMan().getString(formatKey);
       String stageName = (namedStages) ? tad.getNamedStageForIndex(val).name : Integer.toString(val);
       String dispName = MessageFormat.format(format, new Object[] {stageName, displayUnits});
       return (dispName);
     } 
 
    /***************************************************************************
     **
     ** Build Times
     */
     
     private Object[] buildTimeChoices(DynamicInstanceProxy dip) { 
       int min = dip.getMinimumTime();
       int max = dip.getMaximumTime();
       int count = 0;
       Object[] retval = new Object[max - min + 1]; 
       for (int i = min; i <= max; i++) {
         String dispName = timeValToString(i);
         retval[count++] = new TrueObjChoiceContent(dispName, Integer.valueOf(i));
       }
       return (retval);
     }

    /***************************************************************************
    **
    ** Command
    */ 

    private DialogAndInProcessCmd addActionForGroup(boolean forMap) {

      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();    
      int warnHeight = ImageManager.GROUP_WARN_DIM_Y;
      int warnWidth = ImageManager.GROUP_WARN_DIM_X;

      ImageLoad il = loadImage(warnHeight, warnWidth);
      if (il.daipc != null) {
        return (il.daipc);  
      }
      ImageManager.NewImageInfo nii = il.nii;
          
      if (nii.key != null) {
        String whichOp = (forMap) ? "undo.setGroupNodeMapImage": "undo.setGroupNodeImage";
        UndoSupport support = uFac_.provideUndoSupport(whichOp, dacx_);
        String groupNodeID = nt.getGroupNodeID(popupNode_);
        nii.change.groupNodeKey = groupNodeID;
        nii.change.groupNodeForMap = forMap;  
        support.addEdit(new ImageChangeCmd(dacx_, nii.change));
        ImageChange[] ics = dacx_.getGenomeSource().getModelHierarchy().setGroupImage(groupNodeID, nii.key, forMap);

        if (ics != null) {
          int numic = ics.length;
          for (int i = 0; i < numic; i++) {
            support.addEdit(new ImageChangeCmd(dacx_, ics[i]));
          }
        }
        NavTree.NodeID nodeKey = new NavTree.NodeID(groupNodeID);
        support.addEvent(new TreeNodeChangeEvent(nodeKey, TreeNodeChangeEvent.Change.GROUP_NODE_CHANGE));
        support.finish();
      }
    
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Command
    */ 

    private DialogAndInProcessCmd dropAction() {
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();    
     
      String genomeID = nt.getGenomeID(popupNode_);
      if (genomeID == null) {
        UiUtil.fixMePrintout("NO! NEED PROXY IF SLIDER");
        String currDPI = nt.getDynamicProxyID(popupNode_);
        throw new IllegalStateException();
      }
      DataAccessContext dacx4u = new StaticDataAccessContext(dacx_, genomeID);
      UndoSupport support = uFac_.provideUndoSupport("undo.dropImage", dacx4u);
      ImageChange ic = dacx_.getCurrentGenome().dropGenomeImage(mgr_);
      if (ic != null) {
        support.addEdit(new ImageChangeCmd(dacx_, ic));
      }
      support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), ModelChangeEvent.PROPERTY_CHANGE));
      support.finish();    
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Command
    */ 

    private DialogAndInProcessCmd dropActionForGroup(boolean forMap) {
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
      String groupNodeID = nt.getGroupNodeID(popupNode_);
      String whichOp = (forMap) ? "undo.dropGroupNodeMapImage": "undo.dropGroupNodeImage";
      UndoSupport support = uFac_.provideUndoSupport(whichOp, dacx_);
      ImageChange ic = nt.dropGroupImage(groupNodeID, forMap);
      if (ic != null) {
        support.addEdit(new ImageChangeCmd(dacx_, ic));
      }
      NavTree.NodeID nodeKey = new NavTree.NodeID(groupNodeID);
      support.addEvent(new TreeNodeChangeEvent(nodeKey, TreeNodeChangeEvent.Change.GROUP_NODE_CHANGE));
      support.finish();    
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }

    /***************************************************************************
    **
    ** Utility return class
    */    
    
    
    private static class ImageLoad {
       ImageManager.NewImageInfo nii;
       DialogAndInProcessCmd daipc;
       
       ImageLoad(ImageManager.NewImageInfo nii, DialogAndInProcessCmd daipc) {
         this.nii = nii;
         this.daipc = daipc; 
       }  
    }
  }
}
