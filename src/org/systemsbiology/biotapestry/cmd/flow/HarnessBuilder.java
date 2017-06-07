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

import javax.swing.JFrame;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.PathAndFileSource;
import org.systemsbiology.biotapestry.app.RememberSource;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.db.DataAccessContext;

/****************************************************************************
**
** Class to build a control flow harness
*/

public class HarnessBuilder {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private BTState appState_;
  private UIComponentSource uics_;
  private TabSource tSrc_;
  private CmdSource cSrc_;
  private RememberSource rSrc_;
  private PathAndFileSource pafs_;
  private UndoFactory uFac_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public HarnessBuilder(BTState appState, UIComponentSource uics, 
                        CmdSource cSrc, TabSource tSrc, RememberSource rSrc,
                        PathAndFileSource pafs, UndoFactory uFac) {
    appState_ = appState;
    uics_ = uics;
    cSrc_ = cSrc;
    tSrc_ = tSrc;
    rSrc_ = rSrc;
    pafs_ = pafs;
    uFac_ = uFac;
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Assemble all the pieces:
  */
    
  public DesktopControlFlowHarness genDesktopHarness(JFrame topFrame) {  
    return (genHarness(topFrame));
  }

  /***************************************************************************
  **
  ** Assemble all the pieces:
  */
    
  public BatchJobControlFlowHarness genBatchHarness() {   
    BatchJobControlFlowHarness retval = new BatchJobControlFlowHarness(this,  uics_, rSrc_, uFac_, tSrc_, pafs_, cSrc_, null); 
    return (retval);
  }

  /***************************************************************************
  **
  ** Assemble all the pieces:
  */
    
  public WebServerControlFlowHarness genWebHarness(SerializableDialogPlatform dPlat) {  
    return (new WebServerControlFlowHarness(this,  uics_, rSrc_,  uFac_, tSrc_, pafs_, cSrc_, dPlat)); 
  }

  /***************************************************************************
  **
  ** Assemble all the pieces:
  */
    
  private DesktopControlFlowHarness genHarness(JFrame topFrame) {   
    DesktopControlFlowHarness retval = new DesktopControlFlowHarness(this,  uics_, rSrc_, 
                                                                     uFac_, tSrc_, pafs_, 
                                                                     cSrc_, new DesktopDialogPlatform(topFrame)); 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the ability to launch another harness:
  */
    
  public PreHarness buildHarness(FlowMeister.FlowKey flok) { 
    StaticDataAccessContext dacx = new StaticDataAccessContext(appState_);
    DesktopControlFlowHarness dcf = genHarness(uics_.getTopFrame());
    ControlFlow myControlFlow = cSrc_.getFloM().getControlFlow(flok, null);
    dcf.initFlow(myControlFlow, dacx);
    DialogAndInProcessCmd.CmdState cst = myControlFlow.getEmptyStateForPreload(dacx);
    return (new PreHarness(cst, dcf));
  }
  
  /***************************************************************************
  **
  ** Get the ability to launch another harness:
  */
    
  public PreHarness buildHarness(FlowMeister.FlowKey flok, StaticDataAccessContext dacx) { 
    DesktopControlFlowHarness dcf = genHarness(uics_.getTopFrame());
    ControlFlow myControlFlow = cSrc_.getFloM().getControlFlow(flok, null);
    dcf.initFlow(myControlFlow, dacx);
    DialogAndInProcessCmd.CmdState cst = myControlFlow.getEmptyStateForPreload(dacx);
    return (new PreHarness(cst, dcf));
  }
  
  /***************************************************************************
  **
  ** Get the ability to launch another harness:
  */
    
  public PreHarness buildHarness(FlowMeister.FlowKey flok, JFrame topFrame) { 
    StaticDataAccessContext dacx = new StaticDataAccessContext(appState_);
    DesktopControlFlowHarness dcf = genHarness(topFrame);
    ControlFlow myControlFlow = cSrc_.getFloM().getControlFlow(flok, null);
    dcf.initFlow(myControlFlow, dacx);
    DialogAndInProcessCmd.CmdState cst = myControlFlow.getEmptyStateForPreload(dacx);
    return (new PreHarness(cst, dcf));
  }
  
  /***************************************************************************
  **
  ** Get the ability to launch another harness:
  */
    
  public PreHarness buildHarness(ControlFlow controlFlow) { 
    StaticDataAccessContext dacx = new StaticDataAccessContext(appState_);
    DesktopControlFlowHarness dcf = genHarness(uics_.getTopFrame());
    dcf.initFlow(controlFlow, dacx);
    DialogAndInProcessCmd.CmdState cst = controlFlow.getEmptyStateForPreload(dacx);
    return (new PreHarness(cst, dcf));
  }
  
  /***************************************************************************
  **
  ** Get the ability to launch another harness:
  */
    
  public PreHarness buildHarness(ControlFlow controlFlow, DynamicDataAccessContext ddacx, UIComponentSource uics) { 
    StaticDataAccessContext dacx = new StaticDataAccessContext(ddacx);
    DesktopControlFlowHarness dcf = genHarness(uics.getTopFrame());
    dcf.initFlow(controlFlow, dacx);
    DialogAndInProcessCmd.CmdState cst = controlFlow.getEmptyStateForPreload(dacx);
    return (new PreHarness(cst, dcf));
  }

  /***************************************************************************
  **
  ** Hand us a DACX and run it now.
  */
    
  public void buildAndRunHarness(FlowMeister.FlowKey flok, StaticDataAccessContext dacx) { 
    DesktopControlFlowHarness dcf = genHarness(uics_.getTopFrame());
    ControlFlow myControlFlow = cSrc_.getFloM().getControlFlow(flok, null);
    dcf.initFlow(myControlFlow, dacx);
    dcf.runFlow();
    return;
  }
  
  /***************************************************************************
  **
  ** Hand us a DACX and run it now.
  */
    
  public void buildAndRunHarness(ControlFlow myControlFlow, DynamicDataAccessContext dacx, UIComponentSource uics) { 
    DesktopControlFlowHarness dcf = genHarness(uics.getTopFrame()); 
    dcf.initFlow(myControlFlow, new StaticDataAccessContext(dacx));
    dcf.runFlow();
    return;
  }
  /***************************************************************************
  **
  ** Hackish workaround
  */
    
  public DialogAndInProcessCmd.CmdState getNewCmdState(FlowMeister.FlowKey flok, DataAccessContext dacx) { 
  
    ControlFlow cFlow = cSrc_.getFloM().getControlFlow(flok, null);
    DialogAndInProcessCmd.CmdState cst = cFlow.getEmptyStateForPreload(new StaticDataAccessContext(dacx));
    if (cst instanceof SetCurrentModel.StepState) {
      ((SetCurrentModel.StepState)cst).setAppState(appState_, dacx);
    }
    return (cst);
  }

  /***************************************************************************
  **
  ** Batch job
  */
    
  public PreHarness buildBatchHarness(FlowMeister.FlowKey flok, StaticDataAccessContext dacx) {
    BatchJobControlFlowHarness bcf = genBatchHarness();
    ControlFlow myFlow0 = cSrc_.getFloM().getControlFlow(flok, null);
    bcf.initFlow(myFlow0, dacx);
    DialogAndInProcessCmd.CmdState cst = myFlow0.getEmptyStateForPreload(dacx);
    return (new PreHarness(cst, bcf));
  }
  
  /***************************************************************************
  **
  ** Batch job
  */
    
  public DialogAndInProcessCmd stepBatchFlow(PreHarness preH) {
    return (preH.bcf.stepTheFlow(preH.cst));
  }  
  
  /***************************************************************************
  **
  ** launch the harness:
  */
    
  public boolean runHarnessForOOB(PreHarness preH) {
    preH.dcf.runFlow(preH.cst);
    return (preH.dcf.getOOBResult());   
  }

  /***************************************************************************
  **
  ** launch the harness:
  */
    
  public DialogAndInProcessCmd runHarness(PreHarness preH) {
    return (preH.dcf.runFlow(preH.cst));  
  }
  
  /***************************************************************************
  ** 
  ** Get the Web Server Control Flow Harness
  */
  
  public WebServerControlFlowHarness getHarness() {
    return (appState_.getHarness());  
  }   
  
  /***************************************************************************
  ** 
  ** Set the Web Server Control Flow Harness
  */
  
  public void setHarness(WebServerControlFlowHarness cfh) {
    appState_.setHarness(cfh); 
    return;
  } 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  public static class PreHarness {
    DialogAndInProcessCmd.CmdState cst;
    DesktopControlFlowHarness dcf;
    BatchJobControlFlowHarness bcf;
    
    PreHarness(DialogAndInProcessCmd.CmdState cst, DesktopControlFlowHarness dcf) {
      this.cst = cst;
      this.dcf = dcf;     
    }
    
    PreHarness(DialogAndInProcessCmd.CmdState cst, BatchJobControlFlowHarness bcf) {
      this.cst = cst;
      this.bcf = bcf;     
    }
    
    public DialogAndInProcessCmd.CmdState getCmdState() {
      return (cst);
    } 
  } 
}
