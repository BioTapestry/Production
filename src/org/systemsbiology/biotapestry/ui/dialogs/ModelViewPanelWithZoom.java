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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Wraps a model view panel with zoom buttons
*/

public class ModelViewPanelWithZoom extends JPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private JButton buttonIn_;  
  private JButton buttonOut_; 
  private JButton buttonZoom_; 
  private JButton buttonModel_;
  private ModelViewPanel msp_;
  private BTState appState_;
  
  private static final long serialVersionUID = 1L;
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ModelViewPanelWithZoom(BTState appState, ClickableClient optionalClient, DataAccessContext rcx) {      
    this(appState, optionalClient, true, rcx);
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ModelViewPanelWithZoom(BTState appState, ClickableClient optionalClient, boolean selectionZoom, DataAccessContext rcx) { 
    this(appState, optionalClient, selectionZoom, false, rcx);   
  }
   
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ModelViewPanelWithZoom(BTState appState, ClickableClient optionalClient, boolean selectionZoom, boolean centerZoom, DataAccessContext rcx) { 
    appState_ = appState;
    ResourceManager rMan = appState_.getRMan();
    setLayout(new GridBagLayout()); 
    GridBagConstraints gbc = new GridBagConstraints();

    msp_ = (optionalClient != null) ? new ClickableModelViewPanel(appState_, optionalClient, rcx) : new ModelViewPanel(appState_, rcx);
    msp_.setMinimumSize(new Dimension(200, 200));
    JScrollPane jspm = new JScrollPane(msp_);
    jspm.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jspm.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    msp_.registerScrollPane(jspm);
          
    UiUtil.gbcSet(gbc, 0, 1, 1, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    add(jspm, gbc);
    
    buttonOut_ = new FixedJButton(rMan.getString("mvpwz.zoomOut"));
    buttonOut_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          msp_.getZoomController().bumpZoomWrapper('-');
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    buttonIn_ = new FixedJButton(rMan.getString("mvpwz.zoomIn"));
    buttonIn_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          msp_.getZoomController().bumpZoomWrapper('+');
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    if (selectionZoom) {
      buttonZoom_ = new FixedJButton(rMan.getString("mvpwz.zoomToSelected"));
      buttonZoom_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            msp_.zoomToSelected();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
    }
    
   if (centerZoom) {
      buttonModel_ = new FixedJButton(rMan.getString("mvpwz.zoomToModel"));
      buttonModel_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            msp_.getZoomController().zoomToModel();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
    }
    
    Box dbuttonPanel = Box.createHorizontalBox();
    dbuttonPanel.add(buttonOut_);
    dbuttonPanel.add(Box.createHorizontalStrut(10));
    dbuttonPanel.add(buttonIn_);
    if (selectionZoom) {
      dbuttonPanel.add(Box.createHorizontalStrut(10));
      dbuttonPanel.add(buttonZoom_);
    }
    if (centerZoom) {
      dbuttonPanel.add(Box.createHorizontalStrut(10));
      dbuttonPanel.add(buttonModel_);
    }
    dbuttonPanel.add(Box.createHorizontalGlue());
 
    UiUtil.gbcSet(gbc, 0, 11, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);       
    add(dbuttonPanel, gbc); 
    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get the panel
  */ 
    
  public ModelViewPanel getModelView() {
    return (msp_);
  }
  
  /***************************************************************************
  ** 
  ** Override to track sizing changes
  */

  @Override
  public void setBounds(int x, int y, int width, int height) {
    super.setBounds(x, y, width, height);
    return;
  } 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE/PACKAGE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Update zoom state
  */ 
    
  void updateZoom(char sign) {
    try {
      msp_.getZoomController().bumpZoomWrapper(sign);
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return;
  }
}
