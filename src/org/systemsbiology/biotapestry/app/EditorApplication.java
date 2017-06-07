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

import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.JWindow;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Map;
import javax.swing.SwingUtilities;

import org.systemsbiology.biotapestry.gaggle.DeadGoose;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.Metabase;
import org.systemsbiology.biotapestry.event.SelectionChangeEvent;
import org.systemsbiology.biotapestry.gaggle.GooseAppInterface;

/****************************************************************************
**
** The top-level Editor Application
*/

public class EditorApplication {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private JWindow splash_;
  private final BTState appState_;
  private final UIComponentSource uics_;
  private final CmdSource cSrc_;
  private final TabSource tSrc_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Main entry point
  */

  public static void main(String argv[]) {
    ArgParser ap = new ArgParser(); 
    final Map<String, Object> argMap = ap.parse(ArgParser.AppType.EDITOR, argv);
    if (argMap == null) {
      System.err.print(ap.getUsage((new BTState()).getUIComponentSource().getRMan(), ArgParser.AppType.EDITOR));
      System.exit(0);
    }

    BTState appState = new BTState("WJRL", argMap, false, false);
    UIComponentSource uics = appState.getUIComponentSource();
    CmdSource cSrc = appState.getCmdSource();
    TabSource tSrc = appState.getTabSource();
    final EditorApplication su = new EditorApplication(appState, uics, cSrc, tSrc); 
    
    //
    // Gotta wait to give splash screen a chance to get painted:
    //
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
          su.splash();
        }
      });
    } catch (Exception ex) {
    }
    
    boolean ok = uics.getPlugInMgr().loadDataDisplayPlugIns(argMap);   
    if (!ok) {
      System.err.println("Problems loading plugins");
    }
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {    
        su.launch();
      }
    });

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        su.unSplash();
      }
    });
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
        
  private EditorApplication(BTState appState, UIComponentSource uics, CmdSource cSrc, TabSource tSrc) {
    appState_ = appState;
    uics_ = uics;
    cSrc_ = cSrc;
    tSrc_ = tSrc;
    uics.setIsEditor(true);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
        
  private void launch() {
    Metabase mb = appState_.getMetabase();
    mb.newModelViaDACX();
    String tabID = tSrc_.getDbIdForIndex(tSrc_.getCurrentTabIndex());
    Database newDB = mb.getDB(tabID);
    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(mb.getAppState()); 
    newDB.newModelViaDACX(ddacx.getTabContext(tabID));
    EditorWindow ew = new EditorWindow(appState_, uics_, cSrc_, tSrc_);
         
    if (uics_.getDoGaggle()) {
      //
      // This keeps the verifier from trying to dig into this code when it is not present:
      //
      try {
        Class<?> gooseClass = Class.forName("org.systemsbiology.biotapestry.gaggle.BTGoose");
        GooseAppInterface liveGoose = (GooseAppInterface)gooseClass.newInstance();
        liveGoose.setParameters(ew, appState_, appState_.getGaggleSpecies());
        liveGoose.activate();
        uics_.getGooseMgr().setGoose(liveGoose);
        uics_.getGaggleControls().updateGaggleTargetActions();
      } catch (ClassNotFoundException cnfex) {
        System.err.println("BTGoose class not found");
      } catch (InstantiationException iex) {
        System.err.println("BTGoose class not instantiated");
      } catch (IllegalAccessException iex) {
        System.err.println("BTGoose class not instantiated");
      }
      //
      // Triggers button to turn on if connect was successful:
      //
      StaticDataAccessContext dacx = new StaticDataAccessContext(appState_);
      String genomeKey = dacx.getCurrentGenomeID();
      String layoutKey = dacx.getCurrentLayoutID();
      SelectionChangeEvent ev = new SelectionChangeEvent(genomeKey, layoutKey, SelectionChangeEvent.UNSPECIFIED_CHANGE);
      uics_.getEventMgr().sendSelectionChangeEvent(ev);
    } else {
      uics_.getGooseMgr().setGoose(new DeadGoose());
    }
    return;
  }
  
  private void splash() {
    splash_ = new JWindow();
    JPanel content = (JPanel)splash_.getContentPane();
    int width = 500;
    int height = 325;
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (screen.width - width) / 2;
    int y = (screen.height - height) / 2;
    splash_.setBounds(x, y, width, height);

    URL sugif = getClass().getResource(
      "/org/systemsbiology/biotapestry/images/BioTapestrySplash.gif");
    JLabel label = new JLabel(new ImageIcon(sugif));
    //JLabel copyright = new JLabel(rMan.getString("splash.copyright"), JLabel.CENTER);
    content.setBackground(Color.white);   
    content.add(label, BorderLayout.CENTER);
    //content.add(copyright, BorderLayout.SOUTH);
    content.setBorder(BorderFactory.createLineBorder(new Color(3, 107, 3), 10));
    splash_.setVisible(true);
    return;
  }
  
  private void unSplash() {
    splash_.setVisible(false);
    splash_.dispose();
    return;
  }  
}