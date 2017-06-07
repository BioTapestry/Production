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


package org.systemsbiology.biotapestry.cmd.flow.userPath;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.ui.OverlayStateOracle;

/****************************************************************************
**
** Handle Stepping in user tree path
*/

public class PathStep extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private boolean doForward_;
  private String pathKey_;
  private boolean isForNoPath_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public PathStep(boolean doForward) {
    name =  (doForward) ? "command.TreePathForward" : "command.TreePathBackward";
    desc = (doForward) ? "command.TreePathForward" : "command.TreePathBackward";
    icon = (doForward) ? "Forward24.gif" : "Back24.gif";
    mnem =  (doForward) ? "command.TreePathForwardMnem" : "command.TreePathBackwardMnem";
    doForward_ = doForward;
    pathKey_ = null;
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public PathStep(PathArg args) {
    pathKey_ = (args.getForNoPath()) ? null : args.getPathKey();
    isForNoPath_ = args.getForNoPath();
    name = args.getPathName();
    desc = null;
    icon = null;
    mnem = null;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if enabling is handled externally, or through the "isEnabled" route:
  ** 
  */
 
  @Override
  public boolean externallyEnabled() {
    return (true);
  }
  
  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        StepState ans = new StepState(doForward_, pathKey_, cfh, isForNoPath_);
        next = ans.stepInPath();    
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
    
    private boolean doForward;
    private String myPathKey_;
    private boolean isForNoPath_;
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean doForward, String pathKey, StaticDataAccessContext dacx, boolean isForNoPath) {
      super(dacx); 
      this.doForward = doForward;
      myPathKey_ = pathKey;
      nextStep_ = "stepInPath";
      isForNoPath_ = isForNoPath;
    }
      
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean doForward, String pathKey, ServerControlFlowHarness cfh, boolean isForNoPath) {
      super(cfh); 
      this.doForward = doForward;
      myPathKey_ = pathKey;
      nextStep_ = "stepInPath";
      isForNoPath_ = isForNoPath;
    }

    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepInPath() {
      if (myPathKey_ != null || isForNoPath_) {
        uics_.getPathController().setCurrentPath(myPathKey_, dacx_, uFac_);
      } else {
        if (doForward) {
          uics_.getPathController().pathForward(dacx_, uFac_);
        } else {
          uics_.getPathController().pathBackward(dacx_, uFac_);
        }
      }
      uics_.getPathControls().handlePathButtons();
      Map<String,Object> result = new HashMap<String,Object>();
      
      OverlayStateOracle oso = dacx_.getOSO();
      result.put("currModel",dacx_.getCurrentGenomeID());
      result.put("overlay", oso.getCurrentOverlay());
      result.put("currModules", oso.getCurrentNetModules().set);
      result.put("revModules", oso.getRevealedModules().set);
      
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this, result));
    } 
  }

  /***************************************************************************
  **
  ** Arguments
  */
  
  public static class PathArg extends AbstractOptArgs {
    
    private static String[] keys_;
    private static Class<?>[] classes_;
    
    static {
      keys_ = new String[] {"pathName", "pathKey", "forNoPath"}; 
      classes_ = new Class<?>[] {String.class, String.class, Boolean.class};  
    }
    
    public String getPathName() {
      return (getValue(0));
    }
     
    public String getPathKey() {
      return (getValue(1));
    }
    
    public boolean getForNoPath() {
      return (Boolean.parseBoolean(getValue(2)));
    }

    public PathArg(Map<String, String> argMap) throws IOException {
      super(argMap);
    }
  
    protected String[] getKeys() {
      return (keys_);
    }
    
    @Override
    protected Class<?>[] getClasses() {
      return (classes_);
    }

    public PathArg(String pathName, String pathKey, boolean forNoPath) {
      super();
      if (forNoPath) {
        if (pathKey != null) {
          throw new IllegalArgumentException();
        }
        pathKey = "DONT_CARE";
      }    
      setValue(0, pathName);
      setValue(1, pathKey);
      setValue(2, Boolean.toString(forNoPath));
      bundle();
    }
  }
}
