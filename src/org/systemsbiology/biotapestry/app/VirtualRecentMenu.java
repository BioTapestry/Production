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


package org.systemsbiology.biotapestry.app;

import java.util.Iterator;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveOps;
import org.systemsbiology.biotapestry.nav.RecentFilesManager;
import org.systemsbiology.biotapestry.ui.menu.XPlatAction;
import org.systemsbiology.biotapestry.ui.menu.XPlatMaskingStatus;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenu;
import org.systemsbiology.biotapestry.ui.menu.XPlatSeparator;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Handles the menu (real or headless) of recently opened files
*/

public class VirtualRecentMenu {
  
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
  private JMenu recentMenu_;
  private XPlatMenu recentXPlatMenu_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public VirtualRecentMenu(BTState appState) {   
    appState_ = appState;
    appState_.setRecentMenu(this);
    if (!appState_.getIsEditor()) {
      return;
    }   
    ResourceManager rMan = appState_.getRMan();
    if (!appState_.isHeadless()) {    
      recentMenu_ = new JMenu(rMan.getString("command.recentMenu"));
      recentMenu_.setMnemonic(rMan.getChar("command.recentMenuMnem"));
    } else {
      // Placeholder for non-nullness
      recentXPlatMenu_ = new XPlatMenu(rMan.getString("command.recentMenu"), rMan.getChar("command.recentMenuMnem")); 
    }
    updateRecentMenu();
  }  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** Push a disabled condition
  */ 
  
  public void pushDisabled(boolean enabled) {
    if (enabled) {
      return;
    }
    if (recentMenu_ != null) {
      int numRecent = recentMenu_.getItemCount();
      for (int i = 0; i < numRecent; i++) {
        JMenuItem item = recentMenu_.getItem(i);
        if (item != null) {
          item.setEnabled(false);
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Pop the disabled condition
  */ 
  
  public void popDisabled() {
    if (recentMenu_ != null) {
      int numRecent = recentMenu_.getItemCount();
      for (int i = 0; i < numRecent; i++) {
        JMenuItem item = recentMenu_.getItem(i);
        if (item != null) {
          item.setEnabled(true);
        }
      }
      MainCommands.ChecksForEnabled cra = appState_.getMainCmds().getCachedAction(FlowMeister.MainFlow.CLEAR_RECENT, false);
      cra.setEnabled((recentMenu_.getMenuComponentCount() > 2));
    }
    return;
  } 
 
  /***************************************************************************
  **
  ** Get the menu
  */ 
  
  public JMenu getJMenu() {
    return (recentMenu_);
  } 
  
  /***************************************************************************
  **
  ** Get the XPLAT menu
  */ 
  
  public XPlatMenu getXPlatMenu() {
    return (recentXPlatMenu_);
  } 
   
  /***************************************************************************
  **
  ** Answers if we have a recent menu
  */ 
    
  public boolean haveRecentMenu() {
    if (!appState_.isHeadless()) {
      return (recentMenu_ != null);
    } else {    
      return (recentXPlatMenu_ != null);
    }
  }  
  
  /***************************************************************************
  **
  ** Update the menu of recent files
  */ 
    
  public void updateRecentMenu() {
    
    RecentFilesManager rfm = appState_.getRecentFilesMgr();
    Iterator<String> rfit = rfm.getRecentFiles();
    MainCommands mcmd = appState_.getMainCmds();
    
    if (!appState_.isHeadless()) {
      if (recentMenu_ == null) {
        return;
      }      
      recentMenu_.removeAll();
      while (rfit.hasNext()) {
        String path = rfit.next();
        LoadSaveOps.FileArg args = new LoadSaveOps.FileArg(rfm.getRecentFileName(path), path);
        MainCommands.ChecksForEnabled lra = mcmd.getActionNoCache(FlowMeister.MainFlow.LOAD_RECENT, false, args);
        recentMenu_.add(lra);
      }
      MainCommands.ChecksForEnabled cra = mcmd.getCachedAction(FlowMeister.MainFlow.CLEAR_RECENT, false);
      cra.setEnabled((recentMenu_.getMenuComponentCount() != 0));
      recentMenu_.add(new JSeparator());
      recentMenu_.add(cra);
    } else {
      if (recentXPlatMenu_ == null) {
        return;
      }    
      FlowMeister flom = appState_.getFloM();
      ResourceManager rMan = appState_.getRMan();
      recentXPlatMenu_ = new XPlatMenu(rMan.getString("command.recentMenu"), rMan.getChar("command.recentMenuMnem"));
      boolean needClear = false;
      while (rfit.hasNext()) {
        String path = rfit.next();
        LoadSaveOps.FileArg args = new LoadSaveOps.FileArg(rfm.getRecentFileName(path), path);
        ControlFlow scupa = flom.getControlFlow(FlowMeister.MainFlow.LOAD_RECENT, args);
        XPlatAction xpa  = new XPlatAction(flom, rMan, FlowMeister.MainFlow.LOAD_RECENT, scupa.getName(), null, args);
        recentXPlatMenu_.addItem(xpa);
        needClear = true;
      }
      XPlatAction xpa  = new XPlatAction(flom, rMan, FlowMeister.MainFlow.CLEAR_RECENT);
      xpa.setEnabled(needClear);
      recentXPlatMenu_.addItem(new XPlatSeparator());
      recentXPlatMenu_.addItem(xpa);
    }
    return;
  }

  /***************************************************************************
  **
  ** Clear the menu of recent files
  */ 
    
  public void clearRecentMenu() {
    if (!appState_.isHeadless()) {
      if (recentMenu_ == null) {
        return;
      }
      recentMenu_.removeAll();    
      recentMenu_.add(new JSeparator());
      MainCommands.ChecksForEnabled cra = appState_.getMainCmds().getCachedAction(FlowMeister.MainFlow.CLEAR_RECENT, false);
      cra.setEnabled(false);
      recentMenu_.add(cra);
    } else {
      if (recentXPlatMenu_ == null) {
        return;
      }
      ResourceManager rMan = appState_.getRMan();
      FlowMeister flom = appState_.getFloM();
      recentXPlatMenu_ = new XPlatMenu(rMan.getString("command.recentMenu"), rMan.getChar("command.recentMenuMnem"));   
      XPlatAction xpa  = new XPlatAction(flom, rMan, FlowMeister.MainFlow.CLEAR_RECENT);
      xpa.setEnabled(false);
      recentXPlatMenu_.addItem(new XPlatSeparator());
      recentXPlatMenu_.addItem(xpa);
    }
    return;
  }
}
