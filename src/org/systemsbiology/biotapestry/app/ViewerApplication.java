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
import javax.swing.JWindow;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.BorderFactory;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Map;
import javax.swing.SwingUtilities;

import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.Metabase;
import org.systemsbiology.biotapestry.gaggle.DeadGoose;

/****************************************************************************
**
** The top-level BioTapestry Viewer Application
*/

public class ViewerApplication {

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
    final Map<String, Object> argMap = ap.parse(ArgParser.AppType.VIEWER, argv);
    if (argMap == null) {
      System.err.print(ap.getUsage((new BTState()).getUIComponentSource().getRMan(), ArgParser.AppType.VIEWER));
      System.exit(0);
    }
    
    BTState appState = new BTState("WJRL", argMap, false, false);
    UIComponentSource uics = appState.getUIComponentSource();
    CmdSource cSrc = appState.getCmdSource();
    TabSource tSrc = appState.getTabSource();
    final ViewerApplication va = new ViewerApplication(appState, uics, cSrc, tSrc);

    //
    // Gotta wait to give splash screen a chance to get painted:
    //
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
          va.splash();
        }
      });
    } catch (Exception ex) {
    }    
    
    //
    // Little extra time to advertise:
    //

    try {
      Thread.sleep(3000);
    } catch (InterruptedException iex) {
    }
    
    boolean ok = uics.getPlugInMgr().loadDataDisplayPlugIns(argMap);   
    if (!ok) {
      System.err.println("Problems loading plugins");
    }

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {    
        va.launch(argMap);
      }
    });  
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        va.unSplash();
      }
    });    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
        
  private ViewerApplication(BTState appState, UIComponentSource uics, CmdSource cSrc, TabSource tSrc) {
    appState_ = appState;
    uics_ = uics;
    cSrc_ = cSrc;
    tSrc_ = tSrc;
    uics_.setIsEditor(false);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private void launch(Map<String, Object> args) {
    Metabase mb = appState_.getMetabase();
    mb.newModelViaDACX();
    String tabID = tSrc_.getDbIdForIndex(tSrc_.getCurrentTabIndex());
    Database newDB = mb.getDB(tabID);
    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(mb.getAppState()); 
    newDB.newModelViaDACX(ddacx.getTabContext(tabID));
    new ViewerWindow(appState_, uics_, cSrc_, tSrc_);
    uics_.getGooseMgr().setGoose(new DeadGoose());
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