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


package org.systemsbiology.biotapestry.cmd.flow.gaggle;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.gaggle.GooseAppInterface;
import org.systemsbiology.biotapestry.gaggle.InboundGaggleOp;
import org.systemsbiology.biotapestry.gaggle.SelectionSupport;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Handle gaggle ops
*/

public class GaggleOps extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum GaggleAction {
    SEND_NET("command.GaggleSendNetwork", "command.GaggleSendNetwork", "N24.gif", "command.GaggleSendNetworkMnem", null),
    SEND_NAMES("command.GaggleSendNameList", "command.GaggleSendNameList", "B24.gif", "command.GaggleSendNameListMnem", null),
    RAISE("command.GaggleRaiseGoose", "command.GaggleRaiseGoose", "S24.gif", "command.GaggleRaiseGooseMnem", null),
    LOWER("command.GaggleLowerGoose", "command.GaggleLowerGoose", "H24.gif", "command.GaggleLowerGooseMnem", null),
    GOOSE_UPDATE("command.GaggleUpdateGeese", "command.GaggleUpdateGeese", "U24.gif", "command.GaggleUpdateGeeseMnem", null),
    PROCESS_INBOUND("command.GaggleProcessInbound", "command.GaggleProcessInbound", "P24.gif", "command.GaggleProcessInboundMnem", null),
    SET_CURRENT(),  
    ;
 
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    GaggleAction(String name, String desc, String icon, String mnem, String accel) {
      this.name_ = name;  
      this.desc_ = desc;
      this.icon_ = icon;
      this.mnem_ = mnem;
      this.accel_ = accel;
    }

    GaggleAction() {
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
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private GaggleAction action_;
  private int gooseIndex_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public GaggleOps(GaggleAction action) {
    if (action.equals(GaggleAction.SET_CURRENT)) {
      throw new IllegalArgumentException();
    }
    name = action.getName();
    desc = action.getDesc();
    icon = action.getIcon();
    mnem = action.getMnem();
    accel = action.getAccel();
    action_ = action;
  }
    
  /***************************************************************************
  **
  ** Constructor 
  */ 
   
  public GaggleOps(GaggleAction action, GaggleArg args) {
    if (!action.equals(GaggleAction.SET_CURRENT)) {
      throw new IllegalArgumentException();
    }
    name = args.getGooseName();
    desc = null;
    icon =  null;
    mnem =  null;
    accel =  null;
    action_ = action;
    gooseIndex_ = args.getGooseIndex();
  }

  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    switch (action_) {
      case RAISE:
      case LOWER:
      case SEND_NAMES:
      case SET_CURRENT:
      case GOOSE_UPDATE:
      case PROCESS_INBOUND:      
        return (cache.gooseIsActive());
      case SEND_NET:
        if (!cache.gooseIsActive()) {
          return (false);
        }
        return (cache.genomeIsRoot()); 
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
        ans = new StepState(cfh, action_, name, gooseIndex_);
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

    private GaggleAction myAction_;
    private String myGooseName_;
    private int myGooseIndex_;
  
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh, GaggleAction action, String gooseName, int gooseIndex) {
      super(cfh);
      myAction_ = action;
      nextStep_ = "stepToProcess";
      myGooseName_ = gooseName;
      myGooseIndex_ = gooseIndex;
    }
     
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {
      
      GooseAppInterface goose = uics_.getGooseMgr().getGoose();
      if ((goose != null) && goose.isActivated()) {
        switch (myAction_) {
          case SEND_NET:
            if (!sendNet(goose)) {
              return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
            }
            break;   
          case RAISE:
            goose.raiseCurrentTarget();
            break;
          case LOWER:
            goose.hideCurrentTarget();
            break;
          case SEND_NAMES:
            goose.transmitSelections();
            break;
          case GOOSE_UPDATE:
            uics_.getGaggleControls().updateGaggleTargetActions();
            break;
          case PROCESS_INBOUND:
            // Will be on background thread for awhile; don't lose incoming commands
            SelectionSupport ss = goose.getSelectionSupport();
            List<InboundGaggleOp> pending = ss.getPendingCommands();
            Iterator<InboundGaggleOp> pit = pending.iterator();
            while (pit.hasNext()) {
              InboundGaggleOp op = pit.next();
              op.executeOp(uics_, uFac_, dacx_);
            }
            break;
          case SET_CURRENT:
            goose.setCurrentGaggleTarget(myGooseName_);
            uics_.getGaggleControls().setCurrentGaggleTarget(myGooseIndex_);
            break;                  
          default:
            throw new IllegalStateException();
        }     
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Handle network transmit
    */ 
       
    private boolean sendNet(GooseAppInterface goose) {
      SelectionSupport ss = goose.getSelectionSupport();
      SelectionSupport.NetworkForSpecies net = ss.getOutboundNetwork();
      if ((net == null) || net.getNodes().isEmpty()) {
        return (true);
      }
      int choice = JOptionPane.NO_OPTION;          
      if (net.haveDupNames()) {
        ResourceManager rMan = dacx_.getRMan(); 
        choice = JOptionPane.showConfirmDialog(uics_.getTopFrame(), 
                                               rMan.getString("gaggle.dupNames"), 
                                               rMan.getString("gaggle.dupNamesTitle"),
                                               JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.NO_OPTION) {
          return (false);
        }
      }                   
      choice = JOptionPane.NO_OPTION;          
      if (net.haveOptionalLinks()) {
        ResourceManager rMan = dacx_.getRMan(); 
        choice = JOptionPane.showConfirmDialog(uics_.getTopFrame(), 
                                               rMan.getString("gaggle.networkChoice"), 
                                               rMan.getString("gaggle.networkChoiceTitle"),
                                               JOptionPane.YES_NO_CANCEL_OPTION);
        if (choice == JOptionPane.CANCEL_OPTION) {
          return (false);
        }
      }          
      goose.transmitNetwork(net, (choice == JOptionPane.YES_OPTION));
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** Control Flow Argument
  */  

  public static class GaggleArg extends AbstractOptArgs {
    
    private static String[] keys_;
    private static Class<?>[] classes_;
    
    static {
      keys_ = new String[] {"gooseName", "gooseIndex"};  
      classes_ = new Class<?>[] {String.class, Integer.class};  
    }
    
    public String getGooseName() {
      return (getValue(0));
    }
     
    public int getGooseIndex() {
      return (Integer.parseInt(getValue(1)));
    }
  
    public GaggleArg(Map<String, String> argMap) throws IOException {
      super(argMap);
    }
  
    protected String[] getKeys() {
      return (keys_);
    }
    
    @Override
    protected Class<?>[] getClasses() {
      return (classes_);
    }
    
    public GaggleArg(String gooseName, int gooseIndex) {
      super();
      setValue(0, gooseName);
      setValue(1, Integer.toString(gooseIndex));
      bundle();
    }
  }  
}
