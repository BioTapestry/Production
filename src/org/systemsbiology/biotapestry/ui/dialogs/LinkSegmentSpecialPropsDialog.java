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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.SuggestedDrawStyle;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing special drawing properties for link segments (or drops)
*/

public class LinkSegmentSpecialPropsDialog extends JDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private BusProperties props_;
  private BTState appState_;
  private DataAccessContext dacx_;

  private LinkSegmentID segID_;  
  private SuggestedDrawStyle sds_;
   
  private SuggestedDrawStylePanel sdsPan_;
  
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
  
  public LinkSegmentSpecialPropsDialog(BTState appState, DataAccessContext dacx, String propKey, LinkSegmentID segID) {     
    super(appState.getTopFrame(), appState.getRMan().getString("segSpecial.setSpecial"), true);
    appState_ = appState;
    dacx_ = dacx;
    props_ = dacx_.getLayout().getLinkProperties(propKey);
    sds_ = props_.getDrawStyleForID(segID);
    segID_ = segID;
    
    ResourceManager rMan = appState_.getRMan();    
    setSize(800, 300);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    sdsPan_ = new SuggestedDrawStylePanel(appState_, dacx_, true, null);    
    UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(sdsPan_, gbc);
    
    JPanel panel = new JPanel();
    panel.setBorder(new EtchedBorder(Color.red, Color.darkGray));
    panel.setLayout(new GridLayout(1, 1));
    panel.setBackground(Color.pink);
    
    JLabel label = new JLabel(rMan.getString("segSpecial.warning"), JLabel.CENTER);
    panel.add(label);
    
    UiUtil.gbcSet(gbc, 0, 8, 10, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(panel, gbc);        
    
    //
    // Build the button panel:
    //

    FixedJButton buttonA = new FixedJButton(rMan.getString("dialogs.apply"));
    buttonA.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          applyProperties();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (applyProperties()) {
            LinkSegmentSpecialPropsDialog.this.setVisible(false);
            LinkSegmentSpecialPropsDialog.this.dispose();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          LinkSegmentSpecialPropsDialog.this.setVisible(false);
          LinkSegmentSpecialPropsDialog.this.dispose();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonA);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);

    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, 9, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(appState_.getTopFrame());
    displayProperties();
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  private void displayProperties() {
    //
    // Want to set these settings to current default values applied to whole
    // tree (or maybe even to link-specific)?
        
    String treeColor = props_.getColorName();           
    sdsPan_.displayProperties(sds_, new SuggestedDrawStyle(treeColor));
    return;
  }

  /***************************************************************************
  **
  ** Apply our UI values to the linkage properties
  */
  
  private boolean applyProperties() {

    //
    // Undo/Redo support
    //

    UndoSupport support = new UndoSupport(appState_, "undo.segSpecial"); 

    //
    // May bounce if the user specifies a non-valid integer thickness:
    //
    
    SuggestedDrawStylePanel.QualifiedSDS qsds = sdsPan_.getChosenStyle();
    if (!qsds.isOK) {
      return (false);
    }

    //
    // Submit the changes:
    //
       
    Layout.PropChange[] lpc = new Layout.PropChange[1];
    lpc[0] = dacx_.getLayout().setSpecialPropsForSegment(props_, segID_, qsds.sds, null, dacx_);     
    
    if (lpc[0] != null) {
      PropChangeCmd pcc = new PropChangeCmd(appState_, dacx_, lpc);
      support.addEdit(pcc);
      LayoutChangeEvent ev = new LayoutChangeEvent(dacx_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(ev);
    }
    
    support.finish();
    return (true);
  }
}
