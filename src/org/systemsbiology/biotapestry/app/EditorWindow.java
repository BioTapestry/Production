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

import java.awt.Dimension;
import java.net.URL;
import java.net.MalformedURLException;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.event.WindowAdapter;
import javax.swing.WindowConstants;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;

import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveSupport;
import org.systemsbiology.biotapestry.util.FilePreparer;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.gaggle.GooseAppInterface;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** The top-level BioTapestry Window
*/

public class EditorWindow extends JFrame {
                                                                                                                      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private static final long serialVersionUID = 1L;
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
  
  public EditorWindow(BTState appState)  {     
    super(appState.getRMan().getString("window.editorTitle"));
    appState_ = appState.setTopFrame(this, (JComponent)this.getContentPane());
    Dimension dim = UiUtil.centerBigFrame(this, 1600, 1200, 1.0, 0);
    appState_.setIsEditor(true);
    CommonView cview = new CommonView(appState);
    cview.buildTheView();
    appState_.getContentPane().setSize(dim);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    appState_.setExceptionHandler(new ExceptionHandler(appState_, appState_.getRMan(), this));
    
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        shutdownEditor(true);
      }
    });
    
    URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/BioTapFab16White.gif");  
    setIconImage(new ImageIcon(ugif).getImage());
    setResizable(true);
    setVisible(true); // This has to be before load remote genome...    
    
    LoadSaveSupport lssup = appState_.getLSSupport();
    URL gurl = null;
    URL saltUrl = null;    
    try {   
      gurl = lssup.getURLFromArgs(appState_.getArgs());
      saltUrl = lssup.getSaltURLFromArgs(appState_.getArgs());      
    } catch (MalformedURLException mue) {
      // FIX ME: Should have dialog box
      System.err.println("malformed URL argument");
      return;
    }
    
    if (gurl != null) {
      while (true) {
        FilePreparer.FileInputResultClosure firc = lssup.loadRemoteGenome(gurl, saltUrl, false);
        if (!firc.wasSuccessful()) {
          firc.displayFileInputError();
        }
        if (firc.wasPasswordSuccess()) {
          break;
        }
      }
    } else {
      DynamicDataAccessContext dacx = new DynamicDataAccessContext(appState_);
      lssup.newModelTweaks(dacx);    
    }    

    appState_.getTree().requestTreeFocus(); // Keeps the "save" button from having focus
  }
  
  /***************************************************************************
  **
  ** Editor shutdown operations
  */  
  
  public void shutdownEditor(boolean doGaggle) {
    if (appState_.getCommonView().havePerturbationEditsInProgress()) {
      ResourceManager rMan = appState_.getRMan();
      int ok = JOptionPane.showConfirmDialog(EditorWindow.this, 
                                rMan.getString("closeApp.warningMessagePertEdits"), 
                                rMan.getString("closeApp.warningMessagePertEditsTitle"),
                                JOptionPane.YES_NO_OPTION);
      if (ok != JOptionPane.YES_OPTION) {
        return;
      }
    }
    if (appState_.hasAnUndoChange()) {
       ResourceManager rMan = appState_.getRMan();
       int ok = JOptionPane.showConfirmDialog(EditorWindow.this, 
                                rMan.getString("closeApp.warningMessage"), 
                                rMan.getString("closeApp.warningMessageTitle"),
                                JOptionPane.YES_NO_OPTION);
       if (ok != JOptionPane.YES_OPTION) {
         return;
       }
    }
    if (doGaggle) {
      GooseAppInterface goose = appState_.getGooseMgr().getGoose();
      if ((goose != null) && goose.isActivated()) {
        goose.closeDown();
      }        
    }
    dispose();
    System.exit(0);
  }
}
