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


package org.systemsbiology.biotapestry.cmd;

import java.awt.Color;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoManager;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.VirtualGaggleControls;
import org.systemsbiology.biotapestry.app.VirtualZoomControls;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DesktopControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveSupport;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.event.EventManager;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.GeneralChangeListener;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.LayoutChangeListener;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeListener;
import org.systemsbiology.biotapestry.event.OverlayDisplayChangeEvent;
import org.systemsbiology.biotapestry.event.OverlayDisplayChangeListener;
import org.systemsbiology.biotapestry.event.SelectionChangeEvent;
import org.systemsbiology.biotapestry.event.SelectionChangeListener;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.nav.UserTreePathController;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.menu.XPlatMaskingStatus;
import org.systemsbiology.biotapestry.util.FilePreparer;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Collection of primary commands for the application
** NEVER RENAME THIS CLASS from MainCommands! Due to legacy reasons, it is used as the 
** reference class for user preferences on the desktop.
*/

public class MainCommands implements SelectionChangeListener,
                                     LayoutChangeListener,
                                     ModelChangeListener,
                                     GeneralChangeListener,
                                     OverlayDisplayChangeListener {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int GENERAL_PUSH                 = 0x01;
  public static final int ALLOW_NAV_PUSH               = 0x02;
  public static final int SKIP_PULLDOWN_PUSH           = 0x04;
  public static final int SKIP_OVERLAY_VIZ_PUSH        = 0x08;  
                                  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////   

  ////////////////////////////////////////////////////////////////////////////
  //
  // MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private BTState appState_;
 
  private HashMap<FlowMeister.MainFlow, ControlFlow> flowCache_;
  private HashMap<FlowMeister.MainFlow, ChecksForEnabled> withIcons_;
  private HashMap<FlowMeister.MainFlow, ChecksForEnabled> noIcons_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public MainCommands(BTState appState) {
    appState_ = appState.setMainCmds(this);
    withIcons_ = new HashMap<FlowMeister.MainFlow, ChecksForEnabled>();
    noIcons_ = new HashMap<FlowMeister.MainFlow, ChecksForEnabled>();
    flowCache_ = new HashMap<FlowMeister.MainFlow, ControlFlow>();
    new VirtualZoomControls(appState_);
    new UserTreePathController(appState);
    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(appState_);
    new NetOverlayController(appState, ddacx);
    FilePreparer fprep = new FilePreparer(appState_, appState_.isHeadless(), appState_.getTopFrame(), this.getClass()); 
    LoadSaveSupport lsSup = new LoadSaveSupport(appState, fprep);
    appState_.setLSSupport(lsSup);
 
    EventManager em = appState_.getEventMgr();
    em.addLayoutChangeListener(this);
    em.addModelChangeListener(this);
    em.addSelectionChangeListener(this);
    em.addGeneralChangeListener(this);
    em.addOverlayDisplayChangeListener(this);
    
    appState_.getNetOverlayController().resetControllerState(ddacx);
  }  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get an action customized with args (not cached!);
  */ 
    
  public ChecksForEnabled getActionNoCache(FlowMeister.MainFlow actionKey, boolean withIcon, ControlFlow.OptArgs optionArgs) {
    FlowMeister flom = appState_.getFloM();
    switch (actionKey) {          
      case TREE_PATH_SET_CURRENT_USER_PATH:
      case REMOVE_SELECTIONS_FOR_NODE_TYPE:
      case SET_CURRENT_GAGGLE_TARGET:
      case LOAD_RECENT:
        return (new ChecksForEnabled(withIcon, flom.getControlFlow(actionKey, optionArgs)));
      default: 
        throw new IllegalArgumentException();
    }
  }
   
  /***************************************************************************
  **
  ** Get control flow FROM THE CACHE! Note lack of ControlFlow.OptArgs argument; 
  ** cached flow cannot have additional arguments!
  ** Note cached by FlowMeister key.
  */ 
  
  public ControlFlow getCachedFlow(FlowMeister.MainFlow actionKey) {
    ControlFlow retval = flowCache_.get(actionKey);
    if (retval == null) {
      retval = appState_.getFloM().getControlFlow(actionKey, null);
      flowCache_.put(actionKey, retval);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Get an action FROM THE CACHE only if it already exists!
  */ 
  
  public ChecksForEnabled getCachedActionIfPresent(FlowMeister.MainFlow actionKey, boolean withIcon) {
    HashMap<FlowMeister.MainFlow, ChecksForEnabled> useMap = (withIcon) ? withIcons_ : noIcons_;
    ChecksForEnabled retval = useMap.get(actionKey);
    return (retval);
  }

  /***************************************************************************
  **
  ** Get an action FROM THE CACHE! Note lack of ControlFlow.OptArgs argument; cached actions cannot have additional arguments!
  ** Note cached by FlowMeister key.
  */ 
  
  public ChecksForEnabled getCachedAction(FlowMeister.MainFlow actionKey, boolean withIcon) {
    HashMap<FlowMeister.MainFlow, ChecksForEnabled> useMap = (withIcon) ? withIcons_ : noIcons_;
    ChecksForEnabled retval = useMap.get(actionKey);
    if (retval != null) {
      return (retval);
    } else {
      ControlFlow flow = getCachedFlow(actionKey);
      switch (actionKey) {          
        case ZOOM_TO_CURRENT_SELECTED: 
          retval = new ChecksForEnabled(withIcon, flow);
          retval.setConditionalEnabled(false);
          break;       
        case GAGGLE_GOOSE_UPDATE:
          VirtualGaggleControls vgc = appState_.getGaggleControls();
          retval = new ChecksWithSpecialButton(withIcon, flow, "U24Selected.gif", vgc);   
          break;
        case GAGGLE_PROCESS_INBOUND:
          vgc = appState_.getGaggleControls();
          retval = new ChecksWithSpecialButton(withIcon, flow, "P24Selected.gif", vgc);
          break; 
        case PULLDOWN:
          retval = new ChecksWithToggle(withIcon, flow);
          break;
        // The following 4 require args and CANNOT be cached!
        case TREE_PATH_SET_CURRENT_USER_PATH:
        case REMOVE_SELECTIONS_FOR_NODE_TYPE:
        case SET_CURRENT_GAGGLE_TARGET:
        case LOAD_RECENT:
          throw new IllegalArgumentException();
        default: 
          retval = new ChecksForEnabled(withIcon, flow);
          break;
      }
      useMap.put(actionKey, retval);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Calculate the pushed disabled condition. Results into maskStat
  */ 
  
  public void calcPushDisabled(int pushCondition, XPlatMaskingStatus maskStat) {
    Iterator<FlowMeister.MainFlow> wiit = withIcons_.keySet().iterator();
    while (wiit.hasNext()) {
      FlowMeister.MainFlow mfk = wiit.next();
      ChecksForEnabled cfe = withIcons_.get(mfk);
      cfe.calcDisabled(pushCondition, maskStat, mfk);
    }
    Iterator<FlowMeister.MainFlow> niit = noIcons_.keySet().iterator();
    while (niit.hasNext()) {
      FlowMeister.MainFlow mfk = niit.next();
      ChecksForEnabled cfe = noIcons_.get(mfk);
      cfe.calcDisabled(pushCondition, maskStat, mfk);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Push a disabled condition
  */
  
  public void pushDisabled(XPlatMaskingStatus maskStat) {
    Iterator<FlowMeister.MainFlow> wiit = withIcons_.keySet().iterator();
    while (wiit.hasNext()) {
      FlowMeister.MainFlow mfk = wiit.next();
      ChecksForEnabled cfe = withIcons_.get(mfk);
      cfe.pushDisabled(maskStat, mfk);
    }
    Iterator<FlowMeister.MainFlow> niit = noIcons_.keySet().iterator();
    while (niit.hasNext()) {
      FlowMeister.MainFlow mfk = niit.next();
      ChecksForEnabled cfe = noIcons_.get(mfk);
      cfe.pushDisabled(maskStat, mfk);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Push a disabled condition
 
  
  public void pushDisabled(int pushCondition, XPlatMaskingStatus maskStat) {
    Iterator<FlowMeister.MainFlow> wiit = withIcons_.keySet().iterator();
    while (wiit.hasNext()) {
      FlowMeister.MainFlow mfk = wiit.next();
      ChecksForEnabled cfe = withIcons_.get(mfk);
      cfe.pushDisabled(pushCondition, maskStat, mfk);
    }
    Iterator<FlowMeister.MainFlow> niit = noIcons_.keySet().iterator();
    while (niit.hasNext()) {
      FlowMeister.MainFlow mfk = niit.next();
      ChecksForEnabled cfe = noIcons_.get(mfk);
      cfe.pushDisabled(pushCondition, maskStat, mfk);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Pop the disabled condition
  */ 
  
   public void popDisabled() {
    Iterator<ChecksForEnabled> wiit = withIcons_.values().iterator();
    while (wiit.hasNext()) {
      ChecksForEnabled cfe = wiit.next();
      cfe.popDisabled();
    }
    Iterator<ChecksForEnabled> niit = noIcons_.values().iterator();
    while (niit.hasNext()) {
      ChecksForEnabled cfe = niit.next();
      cfe.popDisabled();
    }
    return;
  } 
 
  /***************************************************************************
  **
  ** Notify listener of model change
  */ 
  
  public void modelHasChanged(ModelChangeEvent mcev) {
    DynamicDataAccessContext dacx = new DynamicDataAccessContext(appState_);
    checkForChanges(dacx.getGenomeID(), CheckGutsCache.MODEL, dacx);
    int change = mcev.getChangeType();
    if ((change == ModelChangeEvent.MODEL_DROPPED) && 
        (mcev.getGenomeKey().equals(dacx.getDBGenomeID()))) {
      appState_.getCommonView().handleModelDrop(); 
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Called when model has changed
  */   

  public void modelHasChanged(ModelChangeEvent event, int remaining) {
    // Since we don't care about the event internals , just use the last
    if (remaining == 0) {
      modelHasChanged(event);
    }
    return;
  }      
  
  /***************************************************************************
  **
  ** Notify listener of layout change
  */ 
  
  public void layoutHasChanged(LayoutChangeEvent lcev) {
    // currently not doing anything with this...
    return;
  }

  /***************************************************************************
  **
  ** Notify listener of general change
  */ 
  
  public void generalChangeOccurred(GeneralChangeEvent gcev) {
    DynamicDataAccessContext dacx = new DynamicDataAccessContext(appState_);
    appState_.getCommonView().perturbationsManagementWindowHasChanged();
    checkForChanges(dacx.getGenomeID(), CheckGutsCache.GENERAL, dacx);    
  }
  
  /***************************************************************************
  **
  ** Notify listener of selection change
  */ 
  
  public void selectionHasChanged(SelectionChangeEvent scev) {
    DynamicDataAccessContext dacx = new DynamicDataAccessContext(appState_);
    checkForChanges(dacx.getGenomeID(), CheckGutsCache.SELECT, dacx);
    return;
  }
  
  /***************************************************************************
  **
  ** Notify listener of overlay display change
  */ 
  
  public void overlayDisplayChangeOccurred(OverlayDisplayChangeEvent odcev) {
    DynamicDataAccessContext dacx = new DynamicDataAccessContext(appState_);
    checkForChanges(dacx.getGenomeID(), CheckGutsCache.OVERLAY, dacx);
    return;
  }  
  
  /***************************************************************************
  **
  ** Trigger the enabled checks
  */ 
  
  private void checkForChanges(String genomeID, int checkType, DataAccessContext dacx) {
    Genome genome = (genomeID == null) ? null : dacx.getGenomeSource().getGenome(genomeID);
    CheckGutsCache cache = new CheckGutsCache(appState_, genome, checkType);
    Iterator<ChecksForEnabled> wiit = withIcons_.values().iterator();
    while (wiit.hasNext()) {
      ChecksForEnabled cfe = wiit.next();
      cfe.checkIfEnabled(cache);
    }
    Iterator<ChecksForEnabled> niit = noIcons_.values().iterator();
    while (niit.hasNext()) {
      ChecksForEnabled cfe = niit.next();
      cfe.checkIfEnabled(cache);
    }
    appState_.getPathControls().handlePathButtons();
    appState_.getNetOverlayController().checkForChanges(dacx);
    return;
  }  
  
  /***************************************************************************
  **
  ** Batch/server mode: collect up enabled state
  */ 
  
  public Map<FlowMeister.FlowKey, Boolean> getFlowEnabledState() {
    Database db = appState_.getDB();   
    String genomeID = appState_.getGenome();
    Genome genome = (genomeID == null) ? null : db.getGenome(genomeID);
    CheckGutsCache cache = new CheckGutsCache(appState_, genome, CheckGutsCache.NONE);
    HashMap<FlowMeister.FlowKey, Boolean> retval = new HashMap<FlowMeister.FlowKey, Boolean>(); 
    
    Iterator<FlowMeister.MainFlow> fckit = flowCache_.keySet().iterator();
    while (fckit.hasNext()) {
      FlowMeister.MainFlow mfk = fckit.next();
      ControlFlow flow = flowCache_.get(mfk); 
      if (flow.externallyEnabled()) {
        continue;
      }    
      boolean enabled = flow.isEnabled(cache);
      retval.put(mfk, Boolean.valueOf(enabled));
    }
    Map<FlowMeister.FlowKey, Boolean> pathEna = appState_.getPathControls().getButtonEnables();
    retval.putAll(pathEna);
    Map<FlowMeister.FlowKey, Boolean> ovrEna = appState_.getNetOverlayController().getButtonEnables();
    retval.putAll(ovrEna);  
    Map<FlowMeister.FlowKey, Boolean> zoomEna = appState_.getVirtualZoom().getButtonEnables();
    retval.putAll(zoomEna); 
    
    UndoManager undom = appState_.getUndoManager();
    retval.put(FlowMeister.MainFlow.UNDO, undom.canUndo());
    retval.put(FlowMeister.MainFlow.REDO, undom.canRedo());
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

 /***************************************************************************
  **
  ** Interface for actions that toggle setting
  */
  
  public interface ToggleAction {      
    public void setToIgnore(boolean arg);
    public void setAsActive(boolean arg);
  }  
  
 /***************************************************************************
  **
  ** Checks if it is enabled or not
  */
  
  public class ChecksForEnabled extends AbstractAction {
    
    private static final long serialVersionUID = 1L;
    
    // Used so we can build cross-platform menus and toolbars:
    private String name_;
    private String desc_;      
    private String icon_;     
    private Character mNem_;
    private Character accel_;
 
    protected static final int IGNORE   = -1;
    protected static final int DISABLED =  0;
    protected static final int ENABLED  =  1;
    
    protected boolean enabled_ = true;
    protected boolean pushed_ = false;
    
    protected ControlFlow flow;
    protected DesktopControlFlowHarness dcf;
     
    protected ChecksForEnabled() {
      dcf = new DesktopControlFlowHarness(appState_, new DesktopDialogPlatform(appState_.getTopFrame())); 
    }
    
    protected ChecksForEnabled(boolean doIcon, ControlFlow flow) {
      dcf = new DesktopControlFlowHarness(appState_, new DesktopDialogPlatform(appState_.getTopFrame()));
      this.flow = flow;
      ResourceManager rMan = appState_.getRMan();
      name_ = rMan.getString(flow.getName());
      putValue(Action.NAME, name_);
      if (doIcon) {
        desc_ = rMan.getString(flow.getDesc());
        putValue(Action.SHORT_DESCRIPTION, desc_);
        if (flow.getIcon() != null) {
          URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/" + flow.getIcon());  
          putValue(Action.SMALL_ICON, new ImageIcon(ugif));
          icon_ = flow.getIcon();
        }
      } else {
        String fgm = flow.getMnem();
        if (fgm != null) {
          char mnemC = rMan.getChar(flow.getMnem());
          mNem_ = new Character(mnemC);
          putValue(Action.MNEMONIC_KEY, new Integer(mnemC));
        }
      }
      if (flow.getAccel() != null) {
        char accelC = rMan.getChar(flow.getAccel());
        accel_ = new Character(accelC);
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelC, Event.CTRL_MASK, false));
      }
    }
         
    protected ChecksForEnabled(boolean doIcon, String name, String sDesc, String icon, String mnem, String accel) {
      dcf = new DesktopControlFlowHarness(appState_, new DesktopDialogPlatform(appState_.getTopFrame()));
      ResourceManager rMan = appState_.getRMan();
      name_ = rMan.getString(name);
      putValue(Action.NAME, name_);
      if (doIcon) {
        desc_ = rMan.getString(sDesc);
        putValue(Action.SHORT_DESCRIPTION, desc_);
        if (icon != null) {
          URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/" + icon);  
          putValue(Action.SMALL_ICON, new ImageIcon(ugif));
          icon_ = icon;
        }
      } else {
        char mnemC = rMan.getChar(mnem);
        mNem_ = new Character(mnemC);
        putValue(Action.MNEMONIC_KEY, new Integer(mnemC)); 
      }
      if (accel != null) {
        char accelC = rMan.getChar(accel);
        accel_ = new Character(accelC);
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelC, Event.CTRL_MASK, false));
      }
    }
     
    protected ChecksForEnabled(boolean doIcon, String name, String sDesc, String icon, String mnem) {
      this(doIcon, name, sDesc, icon, mnem, null);
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
    public Character getMnem() {
      return (mNem_);
      
    }
    public Character getAccel() {
      return (accel_);      
    }
   
    public void actionPerformed(ActionEvent e) {
      try {
        if (flow == null) {
          throw new IllegalStateException();
        }
        dcf.initFlow(flow, new DataAccessContext(appState_, appState_.getGenome()));
        dcf.runFlow();
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      } catch (OutOfMemoryError oom) {
        appState_.getExceptionHandler().displayOutOfMemory(oom);
      }
      return;
    }

    public void checkIfEnabled(CheckGutsCache cache) {
      if ((flow != null) && flow.externallyEnabled()) {
        return;
      }    
      enabled_ = checkGuts(cache);
      if (!pushed_) {
        this.setEnabled(enabled_);
      }
      return;
    }
    
    public void calcDisabled(int pushCondition, XPlatMaskingStatus maskStat, FlowMeister.MainFlow mfk) {
      pushed_ = canPush(pushCondition);
      boolean reversed = reversePush(pushCondition);
      if (pushed_) {
        if (reversed) {
          maskStat.maskOn(mfk.toString());
        } else {
          maskStat.maskOff(mfk.toString());
        }
      } // We say nothing about guys who are not with the program.
      return;
    }

    public void pushDisabled(XPlatMaskingStatus maskStat, FlowMeister.MainFlow mfk) { //int pushCondition, XPlatMaskingStatus maskStat, FlowMeister.MainFlow mfk) {
      String key = mfk.toString();
      if (maskStat.getMaskedOff().contains(key)) {
        this.setEnabled(false);
      } else if (maskStat.getMaskedOn().contains(key)) {
        this.setEnabled(true);
      }
      return;
    /*
      pushed_ = canPush(pushCondition);
      boolean reversed = reversePush(pushCondition);
      if (pushed_) {
        this.setEnabled(reversed);
        if (reversed) {
          maskStat.maskOn(mfk.toString());
        } else {
          maskStat.maskOff(mfk.toString());
        }
      } else {
        maskStat.maskOn(mfk.toString());
      } 
      return;*/
    }
    
    public void setConditionalEnabled(boolean enabled) {
      //
      // If we are pushed, just stash the value.  If not
      // pushed, stash and apply.
      //
      enabled_ = enabled;
      if (!pushed_) {
        this.setEnabled(enabled_);
      }
      return;
    }    
    
    public boolean isPushed() {
      return (pushed_);
    }    
 
    public void popDisabled() {
      if (pushed_) {
        this.setEnabled(enabled_);
        pushed_ = false;
      }
      return;
    }
    
    // Default can always be enabled:
    protected boolean checkGuts(CheckGutsCache cache) {
      if (flow != null) {
        return (flow.isEnabled(cache));
      } else  {
        return (true);
      }
    }
 
    // Default can always be pushed:
    protected boolean canPush(int pushCondition) {
      if (flow != null) {
        return (flow.canPush(pushCondition));
      } else  {
        return (true);
      }
    }
    
    // Signals we are reverse pushed (enabled when others disabled)
    protected boolean reversePush(int pushCondition) {
      if (flow != null) {
        return (flow.reversePush(pushCondition));
      } else  {
        return (false);
      }
    }  
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  public class ChecksWithSpecialButton extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    protected ImageIcon standard_;
    protected ImageIcon inbound_;
    protected JButton targButton_;
    protected Color offColor_;
    protected boolean needSpecial_;
    private VirtualGaggleControls vgc_;
        
    protected ChecksWithSpecialButton(boolean doIcon, ControlFlow flow, String specialIcon, VirtualGaggleControls vgc) {
      super(doIcon, flow);
       // If this Action is on the menu, it still needs to update the separate button version!   
      URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/" + flow.getIcon());
      standard_ = new ImageIcon(ugif);
      if (needSpecial_) {
        ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/" + specialIcon);
        inbound_ = new ImageIcon(ugif);
      }
      targButton_ = null;
      vgc_ = vgc;      
    }  
    
    @Override
    public void actionPerformed(ActionEvent e) {  
      try {
        setButtonCondition(false);
        super.actionPerformed(e);
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      } catch (OutOfMemoryError oom) {
        appState_.getExceptionHandler().displayOutOfMemory(oom);
      }
      return;
    }

    public void setButtonCondition(boolean activate) {
      // These items not available when this object is created. Gotta be lazy:
      if (targButton_ == null) {
        offColor_ = vgc_.getButtonOffColor();
        needSpecial_  = vgc_.needSpecialButtonHandling();
        targButton_ = vgc_.getGaggleButton();
      }     
      if (needSpecial_) {
        putValue(Action.SMALL_ICON, (activate) ? inbound_ : standard_);
        targButton_.validate();
      } else {
        targButton_.setBackground((activate) ? Color.orange : offColor_);
      }
      return;
    }
  }  
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  public class ChecksWithToggle extends ChecksForEnabled implements ToggleAction {
    
    private static final long serialVersionUID = 1L;
    
    protected boolean ignore_;
 
    protected ChecksWithToggle(boolean doIcon, ControlFlow flow) {
      super(doIcon, flow);
      ignore_ = false;
    }
   
    public void setToIgnore(boolean arg) {
      ignore_ = arg;
      return;
    }
    
    public void setAsActive(boolean arg) {
      ((ControlFlow.FlowForMainToggle)flow).directCheck(arg);
      return;
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        if (ignore_) {
          return;
        }
        super.actionPerformed(e);
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      } catch (OutOfMemoryError oom) {
        appState_.getExceptionHandler().displayOutOfMemory(oom);
      }
      return;
    }
  }
}
