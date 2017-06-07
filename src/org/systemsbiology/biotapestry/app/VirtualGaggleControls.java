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


package org.systemsbiology.biotapestry.app;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JMenu;

import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.MenuSource;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.gaggle.GaggleOps;
import org.systemsbiology.biotapestry.gaggle.GooseAppInterface;
import org.systemsbiology.biotapestry.gaggle.SelectionSupport;
import org.systemsbiology.biotapestry.ui.menu.XPlatComboBox;
import org.systemsbiology.biotapestry.ui.menu.XPlatComboOption;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenu;
import org.systemsbiology.biotapestry.ui.menu.XPlatToggleAction;
import org.systemsbiology.biotapestry.util.FixedJComboBox;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Handles gaggle controls for both desktop and headless mode
*/

public class VirtualGaggleControls {
  
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

  private UIComponentSource uics_;
  private CmdSource cSrc_;
  private ResourceManager rMan_;
 
  private JButton gaggleInstallButton_;
  private JButton gaggleUpdateGooseButton_;
  private Color gaggleButtonOffColor_;
  private boolean isAMac_;
  private JMenu gaggleGooseChooseMenu_;
  private JComboBox gaggleGooseCombo_;
  private boolean managingGaggleControls_;
  private XPlatComboBox gaggleXPlat_;
  private XPlatMenu gaggleXPlatMenu_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public VirtualGaggleControls(UIComponentSource uics, CmdSource cSrc, ResourceManager rMan) {
    uics_ = uics;
    cSrc_ = cSrc;
    rMan_ = rMan;
    boolean doGaggle = uics_.getDoGaggle();
    if (doGaggle) {
      gaggleGooseChooseMenu_ = new JMenu(rMan_.getString("command.gooseChoose"));
      gaggleGooseChooseMenu_.setMnemonic(rMan_.getChar("command.gooseChooseMnem"));
      gaggleGooseCombo_ = new FixedJComboBox(250);
      gaggleGooseCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            if (managingGaggleControls_) {
              return;
            }
            GooseAppInterface goose = uics_.getGooseMgr().getGoose();
            if ((goose != null) && goose.isActivated()) {
              ObjChoiceContent occ = (ObjChoiceContent)gaggleGooseCombo_.getSelectedItem();
              goose.setCurrentGaggleTarget((occ == null) ? null : occ.val);
              setCurrentGaggleTarget(gaggleGooseCombo_.getSelectedIndex());
            }
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });        
    } 
    isAMac_ = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
  }  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Tool bar stocking
  */ 
  
  public void stockToolBarPre(MenuSource.BuildInfo bifot) {
    boolean doGaggle = uics_.getDoGaggle();
    if (doGaggle) {
      if (gaggleUpdateGooseButton_ != null) {
        bifot.components.put("GOOSE_UP", gaggleUpdateGooseButton_);
      }
      bifot.components.put("GOOSE_COMBO", gaggleGooseCombo_);
      if (gaggleInstallButton_ != null) {
        bifot.components.put("GOOSE_PROC", gaggleInstallButton_);
      } 
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Tool bar stocking
  */ 
  
  public void stockToolBarPost(MenuSource.BuildInfo bifot) {
    boolean doGaggle = uics_.getDoGaggle();
   
    if (doGaggle) {
      JButton gagup = (JButton)bifot.components.get("GOOSE_UP");
      if ((gagup != null) && (gaggleUpdateGooseButton_ == null)) {
        gaggleUpdateGooseButton_ = gagup;
      }
      JButton gagProc = (JButton)bifot.components.get("GOOSE_PROC");
      if ((gagProc != null) && (gaggleInstallButton_ == null)) {
        gaggleInstallButton_ = gagProc;
      }
      gaggleButtonOffColor_ =  gaggleInstallButton_.getBackground();    
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Get the menu
  */ 
  
  public JMenu getJMenu() {
    return (gaggleGooseChooseMenu_);
  } 
  
  /***************************************************************************
  **
  ** Get the Combo
  */ 
  
  public JComboBox getJCombo() {
    return (gaggleGooseCombo_);
  }
  
  /***************************************************************************
  **
  ** Get the XPLAT menu
  */ 
  
  public XPlatMenu getXPlatMenu() {
    return (gaggleXPlatMenu_);
  } 
  
  /***************************************************************************
  **
  ** Get the XPLAT Combo
  */ 
 
  public XPlatComboBox getXPlatComboBox() {
    return (gaggleXPlat_);
  }
  
  /***************************************************************************
  **
  ** get Gaggle Button
  */ 
  
  public JButton getGaggleButton() {
    return (gaggleInstallButton_);
  }
 
  /***************************************************************************
  **
  ** Get Gaggle Update Goose Button
  */ 
  
  public JButton getGaggleUpdateGooseButton() {
    return (gaggleUpdateGooseButton_);
  }
  
  /***************************************************************************
  **
  ** Get the button off color
  */ 
  
  public Color getButtonOffColor() {
    return (gaggleButtonOffColor_);
  }
  
  /***************************************************************************
  **
  ** Do we need special handling?
  */ 
  
  public boolean needSpecialButtonHandling() {
    return (isAMac_);
  }
  
  /***************************************************************************
  **
  ** Set the enabled condition
  */ 
  
  public void setGooseEnabled(boolean enabled) {
    if (gaggleGooseCombo_ != null) {
      gaggleGooseCombo_.setEnabled(enabled);
    }  
    return;
  }
  
  /***************************************************************************
  **
  ** Call to let us know gaggle buttons need work.
  */    
  
  public void triggerGaggleState(FlowMeister.MainFlow whichAction, boolean activate) {
    MainCommands mcmd = cSrc_.getMainCmds();
    if (whichAction == FlowMeister.MainFlow.GAGGLE_PROCESS_INBOUND) {
      MainCommands.ChecksWithSpecialButton gpi = (MainCommands.ChecksWithSpecialButton)mcmd.getCachedAction(FlowMeister.MainFlow.GAGGLE_PROCESS_INBOUND, true);
      gpi.setButtonCondition(activate);
    } else if (whichAction == FlowMeister.MainFlow.GAGGLE_GOOSE_UPDATE) {
      MainCommands.ChecksWithSpecialButton gug = (MainCommands.ChecksWithSpecialButton)mcmd.getCachedAction(FlowMeister.MainFlow.GAGGLE_GOOSE_UPDATE, true);
      gug.setButtonCondition(activate);
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Update the controls for gaggle 
  */ 
    
  public void updateGaggleTargetActions() {
    MainCommands mcmd = cSrc_.getMainCmds();
    GooseAppInterface goose = uics_.getGooseMgr().getGoose();
    if ((goose != null) && goose.isActivated()) {
      if (!uics_.isHeadless()) {
        managingGaggleControls_ = true;
        SelectionSupport ss = goose.getSelectionSupport();
        List<String> targets = ss.getGooseList();
        int numTarg = targets.size();
        if (gaggleGooseChooseMenu_ != null) {
          gaggleGooseChooseMenu_.removeAll();     
          GaggleOps.GaggleArg args = new GaggleOps.GaggleArg(GooseAppInterface.BOSS_NAME, Integer.valueOf(0));
           MainCommands.ChecksForEnabled scupa = mcmd.getActionNoCache(FlowMeister.MainFlow.SET_CURRENT_GAGGLE_TARGET, false, args);
          JCheckBoxMenuItem jcb = new JCheckBoxMenuItem(scupa);
          gaggleGooseChooseMenu_.add(jcb);   
        }
  
        if (gaggleGooseCombo_ != null) {
          gaggleGooseCombo_.removeAllItems();
          gaggleGooseCombo_.addItem(new ObjChoiceContent(GooseAppInterface.BOSS_NAME, GooseAppInterface.BOSS_NAME)); 
        }
  
        for (int i = 0; i < numTarg; i++) {
          String gooseName = targets.get(i);
          ObjChoiceContent occ = new ObjChoiceContent(gooseName, gooseName);
          if (gaggleGooseChooseMenu_ != null) {
            GaggleOps.GaggleArg args = new GaggleOps.GaggleArg(occ.val, Integer.valueOf(i + 1));
            MainCommands.ChecksForEnabled scupa = mcmd.getActionNoCache(FlowMeister.MainFlow.SET_CURRENT_GAGGLE_TARGET, false, args);
            JCheckBoxMenuItem jcb = new JCheckBoxMenuItem(scupa);
            gaggleGooseChooseMenu_.add(jcb);
          }
          if (gaggleGooseCombo_ != null) {
            gaggleGooseCombo_.addItem(occ);
          }      
        }
        
        if (gaggleGooseChooseMenu_ != null) {        
          JCheckBoxMenuItem jcbmi = (JCheckBoxMenuItem)gaggleGooseChooseMenu_.getItem(0);
          jcbmi.setSelected(true);
        }
  
        if (gaggleGooseCombo_ != null) {
          gaggleGooseCombo_.setSelectedIndex(0); 
          gaggleGooseCombo_.invalidate();
          gaggleGooseCombo_.validate(); 
        }
        
        managingGaggleControls_ = false;      
      } else {
        FlowMeister flom = cSrc_.getFloM();
        SelectionSupport ss = goose.getSelectionSupport();
        List<String> targets = ss.getGooseList();
        int numTarg = targets.size();
        
        gaggleXPlatMenu_ = new XPlatMenu(rMan_.getString("command.gooseChoose"), rMan_.getChar("command.gooseChooseMnem"));       
        GaggleOps.GaggleArg args = new GaggleOps.GaggleArg(GooseAppInterface.BOSS_NAME, Integer.valueOf(0));
        ControlFlow scupa = flom.getControlFlow(FlowMeister.MainFlow.SET_CURRENT_GAGGLE_TARGET, args);
        XPlatToggleAction xpta = new XPlatToggleAction(flom, rMan_, FlowMeister.MainFlow.SET_CURRENT_GAGGLE_TARGET, scupa.getName(), null, args, "gaggleFam");
        gaggleXPlatMenu_.addItem(xpta);
        
        gaggleXPlat_ = new XPlatComboBox("");
        gaggleXPlat_.addItem(new XPlatComboOption(flom, rMan_, FlowMeister.MainFlow.SET_CURRENT_GAGGLE_TARGET, scupa.getName(), null, args, "gaggleFam"));
        
         
        for (int i = 0; i < numTarg; i++) {
          String gooseName = targets.get(i);
          args = new GaggleOps.GaggleArg(gooseName, Integer.valueOf(i + 1));
          scupa = flom.getControlFlow(FlowMeister.MainFlow.SET_CURRENT_GAGGLE_TARGET, args);
          xpta = new XPlatToggleAction(flom, rMan_, FlowMeister.MainFlow.SET_CURRENT_GAGGLE_TARGET, scupa.getName(), null, args, "gaggleFam");
          gaggleXPlatMenu_.addItem(xpta);
          gaggleXPlat_.addItem(new XPlatComboOption(flom, rMan_, FlowMeister.MainFlow.SET_CURRENT_GAGGLE_TARGET, scupa.getName(), null, args, "gaggleFam"));          
        } 
      }
    }
    return;
  }  

  /***************************************************************************
  **
  ** Update the controls for user paths
  */ 
    
  public void setCurrentGaggleTarget(int index) {
    managingGaggleControls_ = true;
    
    if (gaggleGooseChooseMenu_ != null) {
      int numUpm = gaggleGooseChooseMenu_.getItemCount();
      for (int i = 0; i < numUpm; i++) {
        JCheckBoxMenuItem jcbmi = (JCheckBoxMenuItem)gaggleGooseChooseMenu_.getItem(i);
        jcbmi.setSelected(i == index);
      }
    }
    if (gaggleGooseCombo_ != null) {
      gaggleGooseCombo_.setSelectedIndex(index); 
      gaggleGooseCombo_.invalidate();
      gaggleGooseCombo_.validate(); 
    }
       
    managingGaggleControls_ = false;
    return;    
  }    

  /***************************************************************************
  **
  ** Call to let us know new gaggle commands are available
  */    
  
  public void haveInboundGaggleCommands() {
    triggerGaggleState(FlowMeister.MainFlow.GAGGLE_PROCESS_INBOUND, true);
    return;
  }
  
  /***************************************************************************
  **
  ** Call to let us know gaggle geese have changed
  */    
  
  public void haveGaggleGooseChange() {
    triggerGaggleState(FlowMeister.MainFlow.GAGGLE_GOOSE_UPDATE, true);
    return;
  }  
}
