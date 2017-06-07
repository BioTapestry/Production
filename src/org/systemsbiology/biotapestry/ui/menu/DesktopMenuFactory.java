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


package org.systemsbiology.biotapestry.ui.menu;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JSeparator;
import javax.swing.JToolBar;

import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.MenuSource;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.util.BTToggleButton;
import org.systemsbiology.biotapestry.util.MenuItemTarget;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Builds Desktop menus
*/

public class DesktopMenuFactory { 
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private CmdSource cSrc_;
  private ResourceManager rMan_;
  private UIComponentSource uics_;
  private HarnessBuilder hBld_;
  
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DesktopMenuFactory(CmdSource cSrc, ResourceManager rMan, UIComponentSource uics, HarnessBuilder hBld) { 
    cSrc_ = cSrc;
    rMan_ = rMan;
    uics_ = uics; 
    hBld_ = hBld;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** Menubar!
  */ 
  
  public JMenuBar buildDesktopMenuBar(XPlatMenuBar xpMenuBar, ActionSource aSrc, MenuSource.BuildInfo bifo) {
    
    JMenuBar menuBar = new JMenuBar();
    Iterator<XPlatMenu> mit = xpMenuBar.getMenus();
    while (mit.hasNext()) {
      XPlatMenu xpm = mit.next();
      String condition = xpm.getCondition();
      if (condition != null) {
        Boolean doit = bifo.conditions.get(condition);
        // Do not build items tagged for a false condition!
        if ((doit != null) && !doit.booleanValue()) {
          continue;
        }
      }
      JMenu subMenu = buildMenu(xpm, aSrc, bifo);
      menuBar.add(subMenu);
      String tag = xpm.getTag();
      if (tag != null) {
        bifo.collected.put(tag, subMenu);
      }  
    }
    return (menuBar);  
  }
  
  /***************************************************************************
  **
  ** Menu!
  */ 
  
  private JMenu buildMenu(XPlatMenu xpMenu, ActionSource aSrc, MenuSource.BuildInfo bifo) {   
    JMenu jmenu = new JMenu(xpMenu.getName());
    char mnem = xpMenu.getMnem();
    if (mnem != '\0') {
      jmenu.setMnemonic(mnem);
    }
    MenuItemTarget mit = new MenuItemTarget(jmenu);
    buildMenuGeneral(xpMenu, aSrc, bifo, mit);
    Boolean enabled = xpMenu.getEnabled();
    if (enabled != null) {
      jmenu.setEnabled(enabled.booleanValue());
    }
    return (jmenu); 
  }
  
  /***************************************************************************
  **
  ** Menu!
  */ 
  
  public void buildMenuGeneral(XPlatMenu xpMenu, ActionSource aSrc, MenuSource.BuildInfo bifo, MenuItemTarget mit) {
    
    Iterator<XPlatMenuItem> iit = xpMenu.getItems();
    while (iit.hasNext()) {
      XPlatMenuItem xpmi = iit.next();
      // Do not build items tagged for a false condition!
      String condition = xpmi.getCondition();
      if (condition != null) {
        Boolean doit = bifo.conditions.get(condition);
        // Do not build items tagged for a false condition!
        if ((doit != null) && !doit.booleanValue()) {
          continue;
        }
      }
      // Remember items that are tagged!
      String tag = xpmi.getTag();
      switch (xpmi.getType()) {
        case MENU:
          JMenu subMenu = buildMenu((XPlatMenu)xpmi, aSrc, bifo);
          mit.add(subMenu);
          if (tag != null) {
            bifo.collected.put(tag, subMenu);
          }  
          break;
        case SEPARATOR:
          mit.add(new JSeparator());
          break;
        case ACTION:
          XPlatAction xpa = (XPlatAction)xpmi; 
          AbstractAction aa = aSrc.getAction(xpa.getKey(), xpa.getActionArg(), rMan_, uics_, cSrc_, hBld_);
          JComponent jc = mit.add(aa);
          if (tag != null) {
            bifo.taggedActions.put(tag, aa);
            bifo.components.put(tag, jc);
            int currPos = mit.getComponentCount() - 1;
            bifo.compPosition.put(tag, new Integer(currPos));
          }
          bifo.allActions.add(aa);
          break;           
        case CHECKBOX_ACTION:
          XPlatToggleAction xpta = (XPlatToggleAction)xpmi;
          AbstractAction pda = aSrc.getAction(xpta.getKey(), xpta.getActionArg(), rMan_, uics_, cSrc_, hBld_);
          JCheckBoxMenuItem jcb = new JCheckBoxMenuItem(pda);
          mit.add(jcb);
          if (tag != null) {
            bifo.taggedActions.put(tag, pda);
            bifo.components.put(tag, jcb);
            int currPos = mit.getComponentCount() - 1;
            bifo.compPosition.put(tag, new Integer(currPos));
          }
          bifo.allActions.add(pda);
          break;
        case MENU_PLACEHOLDER:
          if (tag == null) {
            throw new IllegalStateException();
          }
          JMenu pjm = bifo.collected.get(tag);
          mit.add(pjm);
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Stock a tool bar from an action map
  */ 
  
  public void stockToolBar(JToolBar jbar, XPlatToolBar toob, Map<FlowMeister.FlowKey, MainCommands.ChecksForEnabled> actionMap, MenuSource.BuildInfo bifo) {
     
    jbar.removeAll();
    Iterator<XPlatToolBar.BarMember> iit = toob.getItems();
    while (iit.hasNext()) {
      XPlatToolBar.BarMember barm = iit.next();
      String condition = barm.getCondition();
      if (condition != null) {
        Boolean doit = bifo.conditions.get(condition);
        // Do not build items tagged for a false condition!
        if ((doit != null) && !doit.booleanValue()) {
          continue;
        }
      }        
      // Remember items that are tagged!
      String tag = barm.getTag();
      switch (barm.getType()) {
        case ACTION:
        case CHECKBOX_ACTION:
          XPlatMenuItem.XPType theType = barm.getType();
          XPlatToolBar.BarAction barba = (XPlatToolBar.BarAction)barm; 
          MainCommands.ChecksForEnabled aa = actionMap.get(barba.getKey());
          if (tag != null) {
            JComponent pcm = bifo.components.get(tag);
            if (pcm == null) {
              bifo.taggedActions.put(tag, aa);
              if (theType == XPlatMenuItem.XPType.CHECKBOX_ACTION) {
                pcm = new BTToggleButton(aa);
                jbar.add(pcm);
                bifo.components.put(tag, pcm);
                int currPos = jbar.getComponentCount() - 1;
                bifo.compPosition.put(tag, new Integer(currPos));
              } else {
                JComponent addedComponent = jbar.add(aa);
                bifo.components.put(tag, addedComponent);
                int currPos = jbar.getComponentCount() - 1;
                bifo.compPosition.put(tag, new Integer(currPos));
              }               
            } else {
              jbar.add(pcm);
            }
          } else {
            jbar.add(aa);            
          }
          bifo.allActions.add(aa);
          break;
        case SEPARATOR:
          jbar.addSeparator();
          break;
        case MENU_PLACEHOLDER:
          if (tag == null) {
            throw new IllegalStateException();
          }
          JComponent pcmp = bifo.components.get(tag);
          jbar.add(pcmp);
          break;
        case MENU:
        default:
          throw new IllegalArgumentException();
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Stock a toolbar action map
  */ 
  
  public Map<FlowMeister.FlowKey, MainCommands.ChecksForEnabled> stockActionMap(XPlatToolBar toob, MenuSource.BuildInfo bifo) {
     
    MainCommands mcmd = cSrc_.getMainCmds();
    HashMap<FlowMeister.FlowKey, MainCommands.ChecksForEnabled> retval = new HashMap<FlowMeister.FlowKey, MainCommands.ChecksForEnabled>();
    
    Iterator<XPlatToolBar.BarMember> iit = toob.getItems();
    while (iit.hasNext()) {
      XPlatToolBar.BarMember barm = iit.next();
      if (!(barm instanceof XPlatToolBar.BarAction)) {
        continue;
      }

      String condition = barm.getCondition();
      if (condition != null) {
        Boolean doit = bifo.conditions.get(condition);
        // Do not build items tagged for a false condition!
        if ((doit != null) && !doit.booleanValue()) {
          continue;
        }
      }
      XPlatToolBar.BarAction barba = (XPlatToolBar.BarAction)barm; 
      
      retval.put(barba.getKey(), mcmd.getCachedAction((FlowMeister.MainFlow)barba.getKey(), true));
    }

    return (retval);
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
   
  //
  // Class for getting abstract actions to use for populating menus:
  // 
  
  public static class ActionSource {  
    
    private CmdSource cSrc_;
    private Map<FlowMeister.FlowKey, ControlFlow> flowMap_;
  
    public ActionSource(CmdSource cSrc) {    
      cSrc_ = cSrc;
    }
    
    public ActionSource(CmdSource cSrc, Map<FlowMeister.FlowKey, ControlFlow> flowMap) {
      cSrc_ = cSrc;
      flowMap_ = flowMap;
    }
        
    public AbstractAction getAction(FlowMeister.FlowKey key, ControlFlow.OptArgs actionArgs, ResourceManager rMan,
                                    UIComponentSource uics, CmdSource cSrc, HarnessBuilder hBld) {    
      FlowMeister.FlowKey.FlowType type = key.getFlowType();
      switch (type) {
        case MAIN:
          if (flowMap_ != null) {
            throw new IllegalArgumentException();
          }
          if (actionArgs == null) {
            return (cSrc_.getMainCmds().getCachedAction((FlowMeister.MainFlow)key, false));
          } else {
            return (cSrc_.getMainCmds().getActionNoCache((FlowMeister.MainFlow)key, false, actionArgs)); 
          }
        case MODEL_TREE:
          // These can be self-contained, and this gets called while VMT under construction
          if (flowMap_ == null) {
            return (VirtualModelTree.getAction(rMan, uics, cSrc, hBld, (FlowMeister.TreeFlow)key));
          } else {
            return (VirtualModelTree.getAction(rMan, uics, cSrc, hBld, flowMap_.get(key)));
          }
        case POP:
          if (flowMap_ != null) {
            throw new IllegalArgumentException();
          }
          return (cSrc_.getPopCmds().getAction((FlowMeister.PopFlow)key, actionArgs));     
        default:
          throw new IllegalArgumentException();
      }
    }
  }
}
