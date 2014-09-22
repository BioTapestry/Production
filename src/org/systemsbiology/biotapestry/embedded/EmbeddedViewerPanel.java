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


package org.systemsbiology.biotapestry.embedded;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.CommonView;
import org.systemsbiology.biotapestry.cmd.flow.BatchJobControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DesktopControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveOps;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.event.EventManager;
import org.systemsbiology.biotapestry.event.SelectionChangeEvent;
import org.systemsbiology.biotapestry.event.SelectionChangeListener;
import org.systemsbiology.biotapestry.gaggle.DeadGoose;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.util.ExceptionHandler;

/****************************************************************************
**
** A BioTapestry viewer panel that can be embedded in another Java application.
** This is a singleton; you only get one per application.
*/

public class EmbeddedViewerPanel extends JPanel implements EmbeddedBioTapestryViewer, 
                                                           SelectionChangeListener {
                                                                                                                       
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private ArrayList<ExternalSelectionChangeListener> selectionListeners_;
  private boolean amListening_;
  private JFrame topFrame_;
  private BTState appState_;
  
  private static final long serialVersionUID = 1L;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public EmbeddedViewerPanel(JFrame topFrame, BTState appState)  {
    appState_ = appState;
    appState_.getDB().newModelViaDACX(); // Bogus, but no DACX yet
    appState_.setIsEditor(false);
    appState_.setTopFrame(topFrame, this);
    appState_.setExceptionHandler(new ExceptionHandler(appState_, appState_.getRMan(), topFrame));
    
    topFrame_ = topFrame;
    amListening_ = false;
    selectionListeners_ = new ArrayList<ExternalSelectionChangeListener>();
    CommonView cview = new CommonView(appState_);
    cview.buildTheView();
    cview.embeddedPanelInit();
    
    HashMap<String, Object> args = new HashMap<String, Object>();
    boolean ok = appState_.getPlugInMgr().loadDataDisplayPlugIns(args);   
    if (!ok) {
      System.err.println("Problems loading plugins");
    }   
    appState_.getGooseMgr().setGoose(new DeadGoose());  
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS FOR EmbeddedBioTapestryViewer INTERFACE:
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Call to load a BioTapestry file
  */ 
  
  public void loadBtp(InputStream is) throws EmbeddedException {
    synchronized (this) {    
      try {
        Database db = appState_.getDB();
        db.newModelViaDACX(); // Bogus, but no DACX yet
        Object[] osArgs = new Object[2];
        osArgs[0] = new Boolean(false);
        osArgs[1] = is;
        BatchJobControlFlowHarness dcf0 = new BatchJobControlFlowHarness(appState_, null); 
        ControlFlow myFlow0 = appState_.getFloM().getControlFlow(FlowMeister.MainFlow.LOAD, null);
        DataAccessContext dacx = new DataAccessContext(appState_, (String)null, (Layout)null);
        LoadSaveOps.StepState pre0 = (LoadSaveOps.StepState)myFlow0.getEmptyStateForPreload(dacx);
        pre0.setParams(osArgs);     
        dcf0.initFlow(myFlow0, dacx);
        DialogAndInProcessCmd daipc0 = dcf0.stepTheFlow(pre0);          
        if (daipc0.state != DialogAndInProcessCmd.Progress.DONE) {
          throw new EmbeddedException("Embedded BioTapestry Input Exception");
        }
      } catch (ExceptionHandler.HeadlessException hex) {
        throw new EmbeddedException("Embedded BioTapestry Wrapped Exception", hex);
      }
    } 
    return;
  } 
  
  /***************************************************************************
  **
  ** Call to add a selection change listener.  Sorry, can only add at the
  ** moment, not remove:
  */ 
  
  public void addSelectionChangeListener(ExternalSelectionChangeListener escl) {
    synchronized (this) {
      selectionListeners_.add(escl);
      if (!amListening_) {
        EventManager em = appState_.getEventMgr();   
        em.addSelectionChangeListener(this);
        amListening_ = true;
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the inventory of all elements
  */ 
  
  public EmbeddedViewerInventory getElementInventory()  {
    synchronized (this) {
      return (new EmbeddedViewerInventory(appState_));
    }
  }
  
  /***************************************************************************
  **
  ** Go and select the links and nodes
  */ 
  
  public boolean goToModelAndSelect(String modelID, Set nodeIDs, Set linkIDs) {
    synchronized (this) {
      DesktopControlFlowHarness dcf = new DesktopControlFlowHarness(appState_, new DesktopDialogPlatform(topFrame_));
      ControlFlow myControlFlow = appState_.getFloM().getControlFlow(FlowMeister.OtherFlowKey.MODEL_AND_NODE_LINK_SELECTION, null);
      DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());
      SetCurrentModel.StepState agis = (SetCurrentModel.StepState)myControlFlow.getEmptyStateForPreload(dacx);
      agis.setPreload(modelID, nodeIDs, linkIDs);
      dcf.initFlow(myControlFlow, dacx);
      dcf.runFlow(agis);
      return (dcf.getOOBResult());
    }
  }

  /***************************************************************************
  **
  ** Call when the application is exiting
  */ 
  
  public void callForShutdown()  {
    // Don't want to make this synchronized, since it can get called during
    // app shutdown.  Nothing to do at the moment anyway.
    return;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS FOR IMPLEMENTATION ONLY
  //
  ////////////////////////////////////////////////////////////////////////////     
      
  /***************************************************************************
  **
  ** Notify listener of selection change
  */ 
  
  public void selectionHasChanged(SelectionChangeEvent scev) {
    int change = scev.getChangeType();
    SUPanel sup = appState_.getSUPanel();
    ExternalSelectionChangeEvent ce;
    if (change == SelectionChangeEvent.SELECTED_MODEL) {
      ce = new ExternalSelectionChangeEvent(scev.getGenomeKey(), new HashSet(), new HashSet()); 
      int numSl = selectionListeners_.size();
      for (int i = 0; i < numSl; i++) {
        ExternalSelectionChangeListener escl = selectionListeners_.get(i);
        escl.selectedModelHasChanged(ce);
      }        
    } else if (change == SelectionChangeEvent.SELECTED_ELEMENT) {
      if (!sup.hasASelection()) {
        ce = new ExternalSelectionChangeEvent(scev.getGenomeKey(), new HashSet(), new HashSet()); 
      } else {
        ce = sup.getExternalSelectionEvent();
      }
      int numSl = selectionListeners_.size();
      for (int i = 0; i < numSl; i++) {
        ExternalSelectionChangeListener escl = selectionListeners_.get(i);
        escl.selectionHasChanged(ce);
      }  
    }
    return;     
  }
}
