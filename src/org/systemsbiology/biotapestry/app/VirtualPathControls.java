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


package org.systemsbiology.biotapestry.app;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.userPath.PathStep;
import org.systemsbiology.biotapestry.ui.menu.XPlatComboBox;
import org.systemsbiology.biotapestry.ui.menu.XPlatComboOption;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenu;
import org.systemsbiology.biotapestry.ui.menu.XPlatToggleAction;
import org.systemsbiology.biotapestry.util.FixedJComboBox;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UndoFactory;

/****************************************************************************
**
** Handles User Path UI and XPlat handling
*/

public class VirtualPathControls {
   
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

  private DynamicDataAccessContext ddacx_;
  private UndoFactory uFac_;
  private UIComponentSource uics_;
  private CmdSource cmdSrc_;
 
  private XPlatComboBox pathXPlat_;
  private XPlatMenu pathXPlatMenu_;
  private HashMap<FlowMeister.FlowKey, Boolean> buttonStat_;
  
  private JMenu userPathsMenu_;
  private JComboBox userPathsCombo_;
  private boolean managingPathControls_;
  
  private boolean pathControlsVisible_;
  private int currentSelected_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public VirtualPathControls(UIComponentSource uics, DynamicDataAccessContext ddacx, UndoFactory uFac, CmdSource cmdSrc) {
    ResourceManager rMan = ddacx.getRMan();
    pathControlsVisible_ = false;
    ddacx_ = ddacx;
    uFac_ = uFac;
    uics_ = uics;
    cmdSrc_ = cmdSrc;
    
    if (!uics_.isHeadless()) {
      userPathsMenu_ = new JMenu(rMan.getString("command.userPaths"));
      userPathsMenu_.setMnemonic(rMan.getChar("command.userPathsMnem"));
      userPathsCombo_ = new FixedJComboBox(250);
  
      userPathsCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            if (managingPathControls_) {
              return;
            }
            ObjChoiceContent occ = (ObjChoiceContent)userPathsCombo_.getSelectedItem();
            uics_.getPathController().setCurrentPath((occ == null) ? null : occ.val, ddacx_, uFac_);
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
    }
    
    buttonStat_ = new HashMap<FlowMeister.FlowKey, Boolean>();
    updateUserPathActions();
    setCurrentUserPath(0);
  }  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Are controls visible?
  */ 
  
  public boolean areControlsVisible() {
    return (pathControlsVisible_);
  } 
  
  /***************************************************************************
  **
  ** Set controls visible/not
  */ 
  
  public void setControlsVisible(boolean areViz) {
    pathControlsVisible_ = areViz;
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the menu
  */ 
  
  public JMenu getJMenu() {
    return (userPathsMenu_);
  } 
  
  /***************************************************************************
  **
  ** Get the Combo
  */ 
  
  public JComboBox getJCombo() {
    return (userPathsCombo_);
  }
  
  /***************************************************************************
  **
  ** Get the XPLAT menu
  */ 
  
  public XPlatMenu getXPlatMenu() {
    return (pathXPlatMenu_);
  } 
  
  /***************************************************************************
  **
  ** Get the XPLAT Combo
  */ 
 
  public XPlatComboBox getXPlatComboBox() {
    return (pathXPlat_);
  }
  
  /***************************************************************************
  **
  ** Get button enabled status
  */ 
 
  public Map<FlowMeister.FlowKey, Boolean> getButtonEnables() {
    return (buttonStat_);
  }
  
  /***************************************************************************
  **
  ** Set the enabled condition
  */ 
  
  public void setPathsEnabled(boolean enabled) {
    if (userPathsCombo_ != null) {
      userPathsCombo_.setEnabled(enabled);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Push a disabled condition
  */ 
  
  public void pushDisabled(boolean enabled) { 
    if (userPathsMenu_ != null) {
      int numPaths = userPathsMenu_.getItemCount();
      for (int i = 0; i < numPaths; i++) {
        JMenuItem item = userPathsMenu_.getItem(i);
        if (item != null) {
          item.setEnabled(enabled);
        }
      }    
    }
    
    if (userPathsCombo_ != null) {
      userPathsCombo_.setEnabled(enabled);
    }
    return;  
  }
  
  /***************************************************************************
  **
  ** Pop the disabled condition
  */ 
  
   public void popDisabled() {
 
    if (userPathsMenu_ != null) {
      int numPaths = userPathsMenu_.getItemCount();
      for (int i = 0; i < numPaths; i++) {
        JMenuItem item = userPathsMenu_.getItem(i);
        if (item != null) {
          item.setEnabled(true);
        }
      }
    }
    
    if (userPathsCombo_ != null) {    
      userPathsCombo_.setEnabled(true);
    }

    return;
  } 

  /***************************************************************************
  **
  ** Update the controls for user paths
  */ 
    
  public void updateUserPathActions() {
    managingPathControls_ = true;
    Vector<ObjChoiceContent> pathChoices = uics_.getPathMgr().getPathChoices();
    int numPC = pathChoices.size();
    ResourceManager rMan = ddacx_.getRMan(); 
    String noPath = rMan.getString("pathController.noPath");
    MainCommands mcmd = cmdSrc_.getMainCmds();
    
    boolean headless = uics_.isHeadless();
     
    //
    // Do the installation in the desktop mode:
    //
    
    if (!headless) {
      if (userPathsMenu_ != null) {
        userPathsMenu_.removeAll();
        PathStep.PathArg args = new PathStep.PathArg(noPath, null, true);
        MainCommands.ChecksForEnabled scupa = mcmd.getActionNoCache(FlowMeister.MainFlow.TREE_PATH_SET_CURRENT_USER_PATH, false, args);
        JCheckBoxMenuItem jcb = new JCheckBoxMenuItem(scupa);
        if (numPC == 0) {
          jcb.setSelected(true);
        }
        userPathsMenu_.add(jcb); 
      }
  
      if (userPathsCombo_ != null) {
        userPathsCombo_.removeAllItems();
        userPathsCombo_.addItem(new ObjChoiceContent(noPath, null));
      }  
      
      for (int i = 0; i < numPC; i++) {
        ObjChoiceContent occ = pathChoices.get(i); 
        if (userPathsMenu_ != null) {
          PathStep.PathArg args = new PathStep.PathArg(occ.name, occ.val, false);
          MainCommands.ChecksForEnabled scupa = mcmd.getActionNoCache(FlowMeister.MainFlow.TREE_PATH_SET_CURRENT_USER_PATH, false, args);
          JCheckBoxMenuItem jcb = new JCheckBoxMenuItem(scupa);
          userPathsMenu_.add(jcb);
        }
        if (userPathsCombo_ != null) {
          userPathsCombo_.addItem(occ);    
        }      
      }
         
      if (userPathsCombo_ != null) {
        userPathsCombo_.invalidate();
        userPathsCombo_.validate();
      } 
      
      uics_.getCommonView().makePathControlsVisible(numPC > 0);
      
      //
      // Do the condition for web servers and batch:
      //
      
    } else {
      FlowMeister flom = cmdSrc_.getFloM();
      pathXPlat_ = new XPlatComboBox("");
      pathXPlatMenu_ = new XPlatMenu(rMan.getString("command.userPaths"), rMan.getChar("command.userPathsMnem"));
      PathStep.PathArg args = new PathStep.PathArg(noPath, null, true);
      ControlFlow scupa = flom.getControlFlow(FlowMeister.MainFlow.TREE_PATH_SET_CURRENT_USER_PATH, args); 
  
      XPlatToggleAction xpta = new XPlatToggleAction(flom, rMan, FlowMeister.MainFlow.TREE_PATH_SET_CURRENT_USER_PATH, scupa.getName(), null, args, "userPathFam");
      pathXPlatMenu_.addItem(xpta);
      pathXPlat_.addItem(new XPlatComboOption(flom, rMan, FlowMeister.MainFlow.TREE_PATH_SET_CURRENT_USER_PATH, scupa.getName(), null, args, "userPathFam"));    
       
      for (int i = 0; i < numPC; i++) {
        ObjChoiceContent occ = pathChoices.get(i); 
        args = new PathStep.PathArg(occ.name, occ.val, false);
        scupa = flom.getControlFlow(FlowMeister.MainFlow.TREE_PATH_SET_CURRENT_USER_PATH, args);
        xpta = new XPlatToggleAction(flom, rMan, FlowMeister.MainFlow.TREE_PATH_SET_CURRENT_USER_PATH, scupa.getName(), null, args, "userPathFam");
        pathXPlatMenu_.addItem(xpta);
        pathXPlat_.addItem(new XPlatComboOption(flom, rMan, FlowMeister.MainFlow.TREE_PATH_SET_CURRENT_USER_PATH, scupa.getName(), null, args, "userPathFam"));
      }   
    }

    handlePathButtons();
 
    managingPathControls_ = false;
    return;
  }
  
  /***************************************************************************
  **
  ** Do path enabling and disabling
  */ 

  public void handlePathButtons() {  
    
    boolean headless = uics_.isHeadless();
    MainCommands mcmd = cmdSrc_.getMainCmds();
    
    //
    // Enable/disable path actions based on path limits:
    //
    
    if (!headless) {
      MainCommands.ChecksForEnabled baWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.TREE_PATH_BACK, true);
      MainCommands.ChecksForEnabled baNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.TREE_PATH_BACK, false);
      
      MainCommands.ChecksForEnabled faWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.TREE_PATH_FORWARD, true);
      MainCommands.ChecksForEnabled faNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.TREE_PATH_FORWARD, false);
          
      MainCommands.ChecksForEnabled daWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.TREE_PATH_DELETE, true);
      MainCommands.ChecksForEnabled daNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.TREE_PATH_DELETE, false);
      
      MainCommands.ChecksForEnabled asaWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.TREE_PATH_ADD_STOP, true);
      MainCommands.ChecksForEnabled asaNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.TREE_PATH_ADD_STOP, false);    
  
      MainCommands.ChecksForEnabled dsaWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.TREE_PATH_DELETE_STOP, true);
      MainCommands.ChecksForEnabled dsaNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.TREE_PATH_DELETE_STOP, false);    
    
      boolean goBack = uics_.getPathController().canGoBackward();
      if (baWI != null) baWI.setConditionalEnabled(goBack);
      if (baNI != null) baNI.setConditionalEnabled(goBack);
      
      boolean goForward = uics_.getPathController().canGoForward();
      if (faWI != null) faWI.setConditionalEnabled(goForward);
      if (faNI != null) faNI.setConditionalEnabled(goForward);
      
      boolean pathSelected = uics_.getPathController().pathIsSelected();
      if (daWI != null) daWI.setConditionalEnabled(pathSelected);
      if (daNI != null) daNI.setConditionalEnabled(pathSelected);
          
      boolean hasAPath = uics_.getPathController().hasAPath();    
      if (asaWI != null) asaWI.setConditionalEnabled(hasAPath);
      if (asaNI != null) asaNI.setConditionalEnabled(hasAPath);
      
      boolean hasAStop = uics_.getPathController().pathHasAStop();
      if (dsaWI != null) dsaWI.setConditionalEnabled(hasAStop);
      if (dsaNI != null) dsaNI.setConditionalEnabled(hasAStop);
    } else {     
      buttonStat_.clear();
       
      ControlFlow baFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.TREE_PATH_BACK);
      if (!baFlow.externallyEnabled()) {
        throw new IllegalStateException();
      }
      boolean goBack = uics_.getPathController().canGoBackward();
      buttonStat_.put(FlowMeister.MainFlow.TREE_PATH_BACK, new Boolean(goBack));
       
      ControlFlow faFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.TREE_PATH_FORWARD);
      if (!faFlow.externallyEnabled()) {
        throw new IllegalStateException();
      }
      boolean goForward = uics_.getPathController().canGoForward();
      buttonStat_.put(FlowMeister.MainFlow.TREE_PATH_FORWARD, new Boolean(goForward));       
       
      ControlFlow daFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.TREE_PATH_DELETE);
      if (!daFlow.externallyEnabled()) {
        throw new IllegalStateException();
      }
      boolean pathSelected = uics_.getPathController().pathIsSelected();
      buttonStat_.put(FlowMeister.MainFlow.TREE_PATH_DELETE, new Boolean(pathSelected));
       
      ControlFlow asaFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.TREE_PATH_ADD_STOP);
      if (!asaFlow.externallyEnabled()) {
        throw new IllegalStateException();
      }
      boolean hasAPath = uics_.getPathController().hasAPath(); 
      buttonStat_.put(FlowMeister.MainFlow.TREE_PATH_ADD_STOP, new Boolean(hasAPath));
       
      ControlFlow dsaFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.TREE_PATH_DELETE_STOP);
      if (!dsaFlow.externallyEnabled()) {
        throw new IllegalStateException();
      }
      boolean hasAStop = uics_.getPathController().pathHasAStop();
      buttonStat_.put(FlowMeister.MainFlow.TREE_PATH_DELETE_STOP, new Boolean(hasAStop));   
    }
    return;
  } 
 
  /***************************************************************************
  **
  ** Update the controls for user paths
  */ 
    
  public void setCurrentUserPath(int index) {
    currentSelected_ = index;
    managingPathControls_ = true;
    
    if (userPathsMenu_ != null) {
      int numUpm = userPathsMenu_.getItemCount();
      for (int i = 0; i < numUpm; i++) {
        JCheckBoxMenuItem jcbmi = (JCheckBoxMenuItem)userPathsMenu_.getItem(i);
        jcbmi.setSelected(i == index);
      }
      userPathsMenu_.invalidate();
      userPathsMenu_.validate();
    }
    
    if (userPathsCombo_ != null) {
      userPathsCombo_.setSelectedIndex(index); 
      userPathsCombo_.invalidate();
      userPathsCombo_.validate(); 
    }
    
    handlePathButtons();    
    
    managingPathControls_ = false;
    return;    
  }
}
