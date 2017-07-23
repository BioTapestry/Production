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


package org.systemsbiology.biotapestry.cmd.flow.simulate;


import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.plugin.SimulatorPlugIn;
import org.systemsbiology.biotapestry.simulation.ModelExpressionEntry;
import org.systemsbiology.biotapestry.simulation.ModelSource;
import org.systemsbiology.biotapestry.simulation.impl.ModelSourceImpl;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle some info providers.
*/

public class SimulationLaunch extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum ActionType {
    LAUNCH("command.LaunchSimulate", "command.LaunchSimulate", "FIXME24.gif", "command.LaunchSimulateMnem", null),
    LOAD_RESULTS("command.LoadResults", "command.LoadResults", "FIXME24.gif", "command.LoadResultsMnem", null),
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    ActionType(String name, String desc, String icon, String mnem, String accel) {
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
  
  private ActionType action_;
  private int engineIndex_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SimulationLaunch(BTState appState, ActionType action) {
    appState_ = appState;
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
  
  public SimulationLaunch(BTState appState, ActionType action, SimulationPluginArg args) {
    appState_ = appState;
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
    engineIndex_ = args.getEngineIndex();
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override
  public boolean isEnabled(CheckGutsCache cache) {
    switch (action_) {
      case LAUNCH:
      case LOAD_RESULTS:
        return (cache.genomeNotNull());
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
    
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
        ans = new StepState(appState_, action_, engineIndex_, cfh);
      } else {
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
      }
      if (ans.getNextStep().equals("stepToProcess")) {
        next = ans.stepToProcess();
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
  ** Running State
  */
        
  public static class StepState extends AbstractStepState {

    private ActionType myAction_;
    private int myEngineIndex_;
    private BTState appState_;
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, ActionType action, int engineIndex, StaticDataAccessContext dacx) {
      super(dacx);
      appState_ = appState;
      myAction_ = action;
      myEngineIndex_ = engineIndex;
      nextStep_ = "stepToProcess";
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, ActionType action, int engineIndex, ServerControlFlowHarness cfh) {
      super(cfh);
      appState_ = appState;
      myAction_ = action;
      myEngineIndex_ = engineIndex;
      nextStep_ = "stepToProcess";
    }
     
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {       
      switch (myAction_) {
        case LAUNCH:      
          showSimulator();
          break;          
        case LOAD_RESULTS:      
          doTimeCourseSimInstall();
          break;     
        default:
          throw new IllegalStateException();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Do counts
    */
    
    private void showSimulator() {
      SimulatorPlugIn stub = uics_.getPlugInMgr().getSimulatorPlugin(myEngineIndex_);
      ModelSource msrc = new ModelSourceImpl(dacx_, hBld_);
      appState_.setModelSource(msrc);
      stub.setModelSource(msrc);
      stub.launch();
      return;
    }
    
    /***************************************************************************
    **
    ** Command
    */ 
 
    private void doTimeCourseSimInstall() {
   
      SimulatorPlugIn sim = uics_.getPlugInMgr().getSimulatorPlugin(myEngineIndex_);
      Map<String, List<ModelExpressionEntry>> res = sim.provideResults();
      ModelSourceImpl mSrc = (ModelSourceImpl)appState_.getModelSource();
      
      DatabaseChange dc = null;
      UndoSupport support = uFac_.provideUndoSupport("undo.loadSim", dacx_);
      dc = dacx_.getExpDataSrc().startTimeCourseUndoTransaction();
      mSrc.handleResults(res);
      dc = dacx_.getExpDataSrc().finishTimeCourseUndoTransaction(dc);
      support.addEdit(new DatabaseChangeCmd(dacx_, dc));
     

      //
      // Unless we are building a full dynamic model from imported simulation data, we don't touch the
      // TIRD:
      //
      
      /*
      TemporalInputRangeData nowTird = dac_.getTemporalRangeSrc().getTemporalInputRangeData();
      TemporalInputRangeData tird;
      if (nowTird.haveMaps()) {
        tird = nowTird.extractOnlyMaps();    
      } else {
        tird = new TemporalInputRangeData(appState_);
        tird.buildMapsFromTCDMaps(dac_);
      } 
      dc = dac_.getTemporalRangeSrc().startTemporalInputUndoTransaction();
      appState_.getDB().setTemporalInputRangeData(tird);
      tird.buildFromTCD(dac_);   
      dc = dac_.getTemporalRangeSrc().finishTemporalInputUndoTransaction(dc);      
      support.addEdit(new DatabaseChangeCmd(appState_, dac_, dc));
      */   
      support.addEvent(new GeneralChangeEvent(dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
      support.finish();
      dacx_.getGenomeSource().clearAllDynamicProxyCaches();
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Control Flow Argument
  */  

  public static class SimulationPluginArg extends AbstractOptArgs {
    
    private static String[] keys_;
    private static Class<?>[] classes_;
    
    static {
      keys_ = new String[] {"engineName", "engineIndex"};  
      classes_ = new Class<?>[] {Integer.class};  
    }
    
    public String getEngineName() {
      return (getValue(0));
    }
       
    public int getEngineIndex() {
      return (Integer.parseInt(getValue(1)));
    }
  
    public SimulationPluginArg(Map<String, String> argMap) throws IOException {
      super(argMap);
    }
  
    protected String[] getKeys() {
      return (keys_);
    }
    
    @Override
    protected Class<?>[] getClasses() {
      return (classes_);
    }
    
    public SimulationPluginArg(String engineName, int engineIndex) {
      super();
      setValue(0, engineName);
      setValue(1, Integer.toString(engineIndex));
      bundle();
    }
  }  
}
