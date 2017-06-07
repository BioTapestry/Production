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


package org.systemsbiology.biotapestry.cmd.flow.add;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.link.LinkSupport;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBGeneRegion;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.IntersectionChooser;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.dialogs.CisModuleCreationDialogFactory;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;


/****************************************************************************
**
** Handle adding cis-reg modules to genes
*/

public class AddCisRegModule extends AbstractControlFlow {

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
  
  public AddCisRegModule() { 
    name = "genePopup.DrawCisRegModule";
    desc = "genePopup.DrawCisRegModule";
    icon = "FIXME24.gif";
    mnem = "genePopup.DrawCisRegModuleMnem";
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
    
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    if (!rcx.currentGenomeIsRootDBGenome()) {
      return (false);
    } 
    String geneID = inter.getObjectID();
    Gene gene = rcx.getCurrentGenome().getGene(geneID);
    if (gene == null) {
      return (false); // Should not happen
    }
    if (gene.getNumRegions() == 0) {
      return (true);
    }
    Iterator<DBGeneRegion> grit = gene.regionIterator();
    ArrayList<DBGeneRegion> glist = new ArrayList<DBGeneRegion>();
    while (grit.hasNext()) {
      DBGeneRegion reg = grit.next();
      glist.add(reg);
    }      
    return (DBGeneRegion.canAddARegion(glist));
  }
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
  @Override    
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(dacx, "stepBiWarning");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        StepState ans = new StepState(cfh, "stepBiWarning");
        next = ans.stepBiWarning();
      } else {
        StepState ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepBiWarning")) {
          next = ans.stepBiWarning();
        } else if (ans.getNextStep().equals("stepStart")) {
          next = ans.stepStart();
        } else if (ans.getNextStep().equals("stepSetToMode")) {
          next = ans.stepSetToMode();
        } else if (ans.getNextStep().equals("stepDataExtract")) { 
          next = ans.stepDataExtract(last);
        } else if (ans.getNextStep().equals("beginDrawCisRegModule")) {
          next = ans.beginDrawCisRegModule();  
        } else if (ans.getNextStep().equals("finishDrawCisRegModule")) {
          next = ans.finishDrawCisRegModule(); 
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
  
  /***************************************************************************
  **
  ** Called as new X, Y info comes in to choose pads.
  */
  
  @Override    
  public DialogAndInProcessCmd processClick(Point theClick, boolean isShifted, double pixDiam, DialogAndInProcessCmd.CmdState cmds) {
    StepState ans = (StepState)cmds;
    ans.x = UiUtil.forceToGridValueInt(theClick.x, UiUtil.GRID_SIZE);
    ans.y = UiUtil.forceToGridValueInt(theClick.y, UiUtil.GRID_SIZE); 
    if ((ans.firstPad_ == null) && (ans.buildType_ == CisModuleCreationDialogFactory.CisModRequest.BuildExtent.BOTH_PADS)) {
      ans.setNextStep("beginDrawCisRegModule");
    } else {
      ans.setNextStep("finishDrawCisRegModule");
    }
    return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans));
  }
   
  /***************************************************************************
  **
  ** Running State: Kinda needs cleanup!
  */
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState, DialogAndInProcessCmd.MouseClickCmdState {

    private String inter_;
    private int x;
    private int y; 
    private Integer firstPad_;
    private int secondPad_;
    private CisModuleCreationDialogFactory.CisModRequest.BuildExtent buildType_;
    private String cisRegName_;
    private List<DBGeneRegion> regList_;
    private DBGene gene_;
   
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
      return (true);
    }
    
    public boolean noRootModel() {
      return (false);
    }
       
    public boolean mustBeDynamic() {
      return (false);
    }
    
    public boolean cannotBeDynamic() {
      return (true);
    }
    
    public boolean hasTargetsAndOverlays() {nextStep_ = "stepAddCisRegModule";
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
    ** Constructor
    */
       
    StepState(ServerControlFlowHarness cfh, String firstStep) {
      super(cfh);
      nextStep_ = firstStep; 
      firstPad_ = null;
      buildType_ = null;
      cisRegName_ = null;
      regList_ = new ArrayList<DBGeneRegion>();
      gene_ = null;     
    }

    /***************************************************************************
    **
    ** Constructor
    */
       
    public StepState(StaticDataAccessContext dacx, String firstStep) {
      super(dacx);
      nextStep_ = firstStep; 
      firstPad_ = null;
      buildType_ = null;
      cisRegName_ = null;
      regList_ = new ArrayList<DBGeneRegion>();
      gene_ = null;     
    }
    
    /***************************************************************************
    **
    ** For ongoing adds
    */ 
       
    public void setIntersection(Intersection intersect) {
      inter_ = intersect.getObjectID();
      return;
    }
    
    /***************************************************************************
    **
    ** Warn of build instructions
    */
      
    private DialogAndInProcessCmd stepBiWarning() {
      DialogAndInProcessCmd daipc;
      if (dacx_.getInstructSrc().haveBuildInstructions()) {
        ResourceManager rMan = dacx_.getRMan();
        String message = rMan.getString("instructWarning.modMessage");
        message = UiUtil.convertMessageToHtml(message);
        String title = rMan.getString("instructWarning.title");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);     
        daipc = new DialogAndInProcessCmd(suf, this);      
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      }    
      nextStep_ = "stepStart";
      return (daipc);     
    }
    
    /***************************************************************************
    **
    ** Kick things off
    */
      
    private DialogAndInProcessCmd stepStart() { 
      gene_ = (DBGene)dacx_.getCurrentGenome().getGene(inter_);
    
      List<DBGeneRegion> glist = DBGeneRegion.initTheList(gene_);
      regList_ = DBGeneRegion.initTheList(gene_);

      boolean canLeft = DBGeneRegion.canGiveOnlyLeft(regList_);
      boolean canRight = DBGeneRegion.canGiveOnlyRight(regList_);

      CisModuleCreationDialogFactory.CisModBuildArgs ba = 
        new CisModuleCreationDialogFactory.CisModBuildArgs(glist, canLeft, canRight);
      CisModuleCreationDialogFactory dgcdf = new CisModuleCreationDialogFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = dgcdf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);       
      nextStep_ = "stepDataExtract";
      return (retval); 
    }
    
    /***************************************************************************
    **
    ** Handle the control flow for drawing into subset instance
    */
         
    private DialogAndInProcessCmd stepDataExtract(DialogAndInProcessCmd cmd) {
      CisModuleCreationDialogFactory.CisModRequest crq = (CisModuleCreationDialogFactory.CisModRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      } 
      cisRegName_ = crq.nameResult;
      buildType_ = crq.buildType;
      nextStep_ = "stepSetToMode";
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      return (retval);
    } 

    /***************************************************************************
    **
    ** Install a mouse handler
    */
    
    private DialogAndInProcessCmd stepSetToMode() {
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      retval.suPanelMode = PanelCommands.Mode.DEFINE_CIS_REG_MODULE;
      return (retval);
    } 
      
    /***************************************************************************
    **
    ** Start to add a new net module
    */  
   
    private DialogAndInProcessCmd beginDrawCisRegModule() {
          
      List<Intersection.AugmentedIntersection> augs = 
        cfh_.getUI().getGenomePresentation().intersectItem(x, y, dacx_, true, false);
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, dacx_)).selectionRanker(augs);
      Intersection inter = ((ai == null) || (ai.intersect == null)) ? null : ai.intersect;
      
      if (inter == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      String id = inter.getObjectID();
      if (!id.equals(inter_)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      Object sub = inter.getSubID();
      if (sub == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      if (!(sub instanceof Intersection.PadVal)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      
      firstPad_ = Integer.valueOf(((Intersection.PadVal)sub).padNum);
      if (!DBGeneRegion.legalStart(regList_, firstPad_.intValue())) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this)); // Keep going
    } 
    
    /***************************************************************************
    **
    ** Start to add a new net module
    */  
   
    private DialogAndInProcessCmd finishDrawCisRegModule() {
         
      List<Intersection.AugmentedIntersection> augs = 
        cfh_.getUI().getGenomePresentation().intersectItem(x, y, dacx_, true, false);
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, dacx_)).selectionRanker(augs);
      Intersection inter = ((ai == null) || (ai.intersect == null)) ? null : ai.intersect;
      
      if (inter == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      String id = inter.getObjectID();
      if (!id.equals(inter_)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      Object sub = inter.getSubID();
      if (sub == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      if (!(sub instanceof Intersection.PadVal)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
     
      if (firstPad_ == null) {
        firstPad_ = Integer.valueOf(((Intersection.PadVal)sub).padNum);
      } else {
        secondPad_ = ((Intersection.PadVal)sub).padNum;
      }
        
      if (!sanityCheck()) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this)); 
      }
   
      addCisRegModule();
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this);
      return (retval); // Keep going
    }  
  
    /***************************************************************************
    **
    ** Decide if the pad is legitimate:
    */  
   
    private boolean sanityCheck() { 
      //
      // A left pad must be in a holder region, with a non-holder to its right. Opposite for
      // right pad
      //        
      if (buildType_ == CisModuleCreationDialogFactory.CisModRequest.BuildExtent.PAD_LEFT) {
        return (DBGeneRegion.legalLeftStart(regList_, firstPad_.intValue()) != null);
      } else if (buildType_ == CisModuleCreationDialogFactory.CisModRequest.BuildExtent.PAD_RIGHT) {
        return (DBGeneRegion.legalRightStart(regList_, firstPad_.intValue()) != null);
      }

      int padLeft = Math.min(firstPad_.intValue(), secondPad_);
      int padRight = Math.max(firstPad_.intValue(), secondPad_);        
      int index = DBGeneRegion.findTheSlot(regList_, padLeft, padRight);
      return (index != -1);
    }

    /***************************************************************************
    **
    ** Do the add operation
    */  
   
    private void addCisRegModule() { 

      //
      // Undo/Redo support
      //
      
      int newPadLeft;
      int newPadRight;
   
      if (buildType_ == CisModuleCreationDialogFactory.CisModRequest.BuildExtent.PAD_LEFT) {
        newPadLeft = firstPad_.intValue();
        newPadRight = DBGeneRegion.legalLeftStart(regList_, newPadLeft).getStartPad() - 1;   
      } else if (buildType_ == CisModuleCreationDialogFactory.CisModRequest.BuildExtent.PAD_RIGHT) {
        newPadRight = firstPad_.intValue();
        newPadLeft = DBGeneRegion.legalRightStart(regList_, newPadRight).getEndPad() + 1;   
      } else {
        newPadLeft = Math.min(firstPad_.intValue(), secondPad_);
        newPadRight = Math.max(firstPad_.intValue(), secondPad_);      
      }  
      
      UndoSupport support = uFac_.provideUndoSupport("undo.addCisReg", dacx_);         
      
      DBGeneRegion newReg = new DBGeneRegion(cisRegName_, newPadLeft, newPadRight, Gene.LEVEL_NONE, false);
      
      regList_ = DBGeneRegion.insertInHolder(regList_, newReg);      
  
      // Sanity check:
      int num = regList_.size();
      DBGeneRegion dbgrF = regList_.get(0);
      DBGeneRegion dbgrL = regList_.get(num - 1);
      if (!DBGeneRegion.validOrder(regList_, dbgrF.getStartPad(), dbgrL.getEndPad())) {
        throw new IllegalStateException();
      }
      
      GenomeChange gc = dacx_.getDBGenome().changeGeneRegions(gene_.getID(), regList_);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(dacx_, gc);
        support.addEdit(gcc);
        ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getDBGenome().getID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(mcev);
      }      
      //
      // The top-level canonical regions have changed. When we draw a new module, the links that go into 
      // that module need to be globally targeted to that module:
      //     
      Map<String, Map<String, DBGeneRegion.LinkAnalysis>> gla = dacx_.getFGHO().analyzeLinksIntoModules(gene_.getID());
    
      if (FullGenomeHierarchyOracle.hasModuleProblems(gla)) {
        LinkSupport.fixCisModLinks(support, gla, dacx_, gene_.getID(), false);
      }
      support.finish();
      return;
    }
  }
}
