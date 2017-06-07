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

package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.SuggestedDrawStyle;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ColorDeletionListener;
import org.systemsbiology.biotapestry.util.ColorSelectionWidget;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Panel for handling settings of SuggestedDrawingStyles
*/

public class SuggestedDrawStylePanel extends JPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private DataAccessContext dacx_;
  private UIComponentSource uics_;
  
  private JCheckBox setColorBox_;
  private ColorSelectionWidget colorWidget_;
  
  private JCheckBox setStyleBox_;
  private JComboBox styleCombo_;  
  
  private JCheckBox setThicknessBox_;
  private JComboBox thicknessCombo_;    
  private JTextField thicknessField_;    
  private JLabel customLabel_;
  
  private boolean optional_;
  private boolean doColor_;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor. Optional is used to allow the user to check or uncheck
  ** whether a feature is set; used for per-link override cases!
  */ 
  
  public SuggestedDrawStylePanel(UIComponentSource uics, DataAccessContext dacx, HarnessBuilder hBld, boolean optional) {   
    this(uics, dacx, hBld, false, optional, false, null);
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SuggestedDrawStylePanel(UIComponentSource uics, DataAccessContext dacx, HarnessBuilder hBld,
                                 boolean optional, List<ColorDeletionListener> colorListeners) { 
    this(uics, dacx, hBld, true, optional, false, colorListeners);
  }
 
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SuggestedDrawStylePanel(UIComponentSource uics, DataAccessContext dacx, HarnessBuilder hBld,
                                 boolean doColor, boolean optional, 
                                 boolean forMulti, List<ColorDeletionListener> colorListeners) {     
    uics_ = uics;
    dacx_ = dacx;
    optional_ = optional;
    doColor_ = doColor;
       
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    ResourceManager rMan = dacx_.getRMan();
    
    //
    // Build the color panel:
    //
    
    int rowNum = 0;
    int colNum = 0;
    
    if (doColor_) {
      if (optional) {
        setColorBox_ = new JCheckBox(rMan.getString("segSpecial.setColor"));
        setColorBox_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              enableColor(setColorBox_.isSelected());
            } catch (Exception ex) {
              uics_.getExceptionHandler().displayException(ex);
            }
            return;
          }
        });
      }

      colorWidget_ = new ColorSelectionWidget(uics_, dacx_, hBld, colorListeners, !optional, null, true, forMulti);

      if (optional) {
        UiUtil.gbcSet(gbc, colNum++, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
        add(setColorBox_, gbc);
      }     
      UiUtil.gbcSet(gbc, colNum, rowNum++, 9, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
      add(colorWidget_, gbc);
    }
    
    //
    // Build the linestyle selection:
    //
    
    colNum = 0;    
    if (optional) {
      setStyleBox_ = new JCheckBox(rMan.getString("segSpecial.lineStyle")); 
      setStyleBox_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            enableStyle(setStyleBox_.isSelected());
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
      UiUtil.gbcSet(gbc, colNum++, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
      add(setStyleBox_, gbc);    
    } else {
      UiUtil.gbcSet(gbc, colNum++, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
      add(new JLabel(rMan.getString("drawStyle.lineStyle")), gbc);            
    }
    
    Vector<ChoiceContent> styleChoices = SuggestedDrawStyle.getStyleChoices(dacx_.getRMan()); 
    if (forMulti) {
      styleChoices.add(0, new ChoiceContent(rMan.getString("multiSelProps.various"), SuggestedDrawStyle.VARIOUS_STYLE));
    }
    styleCombo_ = new JComboBox(styleChoices);
    
    UiUtil.gbcSet(gbc, colNum, rowNum++, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(styleCombo_, gbc);        
    
    //
    // Build the thickness selection:
    //
    
    colNum = 0;    
    if (optional) {
      setThicknessBox_ = new JCheckBox(rMan.getString("segSpecial.setThickness"));
      setThicknessBox_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            enableThick(setThicknessBox_.isSelected());
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
      UiUtil.gbcSet(gbc, colNum++, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
      add(setThicknessBox_, gbc);   
    } else {
      UiUtil.gbcSet(gbc, colNum++, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
      add(new JLabel(rMan.getString("drawStyle.setThickness")), gbc);            
    }
 
    Vector<ChoiceContent> thicknessChoices = SuggestedDrawStyle.getThicknessChoices(dacx_.getRMan());
    if (forMulti) {
      thicknessChoices.add(0, new ChoiceContent(rMan.getString("multiSelProps.various"), SuggestedDrawStyle.VARIOUS_THICKNESS));
    }
    thicknessCombo_ = new JComboBox(thicknessChoices);
    thicknessCombo_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          syncCustomThick();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
        return;
      }
    });

    customLabel_ = new JLabel(rMan.getString("segSpecial.customThickness"), JLabel.RIGHT);    
    thicknessField_ = new JTextField();    

    UiUtil.gbcSet(gbc, colNum++, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(thicknessCombo_, gbc);
 
    UiUtil.gbcSet(gbc, colNum++, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(customLabel_, gbc);
    
    UiUtil.gbcSet(gbc, colNum, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    add(thicknessField_, gbc);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  public void displayMultiProperties(SuggestedDrawStyle current, ConsensusLinkProps clp) {
 
    colorWidget_.setCurrentColor((clp.variousColor) ? ColorSelectionWidget.VARIOUS_TAG : clp.consensusColor);   
    
    if (clp.consensusStyle == SuggestedDrawStyle.VARIOUS_STYLE) {
      styleCombo_.setSelectedIndex(0);
    } else {          
      styleCombo_.setSelectedItem(SuggestedDrawStyle.lineStyleForCombo(dacx_.getRMan(), clp.consensusStyle));
    }
  
    if (clp.consensusThickness == SuggestedDrawStyle.VARIOUS_THICKNESS) {
      thicknessCombo_.setSelectedIndex(0);
    } else {          
      thicknessCombo_.setSelectedItem(SuggestedDrawStyle.thicknessForCombo(dacx_.getRMan(), clp.consensusThickness));
    }
    
    if (SuggestedDrawStyle.customThicknessRequest(clp.consensusThickness)) {
      thicknessField_.setText(Integer.toString(clp.consensusThickness));
    } else {
      thicknessField_.setText("");
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  public void displayProperties(SuggestedDrawStyle current, SuggestedDrawStyle defaults) {
 
    SuggestedDrawStyle sds = (current == null) ? defaults : current;
   
    if (doColor_) {
      String customColor = sds.getColorName();
      colorWidget_.setCurrentColor((customColor == null) ? defaults.getColorName() : customColor);
    }
    
    if (optional_) {
      if (doColor_) {
        boolean selectColor = (current != null) && (current.getColorName() != null);
        setColorBox_.setSelected(selectColor);
        enableColor(selectColor);
      }
   
      boolean selectStyle = (current != null) && (current.getLineStyle() != SuggestedDrawStyle.NO_STYLE);
      setStyleBox_.setSelected(selectStyle);
      enableStyle(selectStyle);
   
      boolean selectThick = (current != null) && 
                            (current.getThickness() != SuggestedDrawStyle.NO_THICKNESS_SPECIFIED);

      setThicknessBox_.setSelected(selectThick);
      enableThick(selectThick);
    }

    int showStyle = sds.getLineStyle();
    if (showStyle == SuggestedDrawStyle.NO_STYLE) {
      // FIX ME!!!!   Make this the tree value
      showStyle = SuggestedDrawStyle.SOLID_STYLE;
    }
    styleCombo_.setSelectedItem(SuggestedDrawStyle.lineStyleForCombo(dacx_.getRMan(), showStyle));

    int showThickness = sds.getThickness();
    if (showThickness == SuggestedDrawStyle.NO_THICKNESS_SPECIFIED) {
            // FIX ME!!!!   Make this the tree value
      showThickness = SuggestedDrawStyle.REGULAR_THICKNESS;
    }
    
    if (SuggestedDrawStyle.customThicknessRequest(showThickness)) {
      thicknessField_.setText(Integer.toString(showThickness));
    } else {
      thicknessField_.setText("");
    }
    thicknessCombo_.setSelectedItem(SuggestedDrawStyle.thicknessForCombo(dacx_.getRMan(), showThickness));
    return;
  }

  /***************************************************************************
  **
  ** Apply our UI values to the linkage properties
  */
  
  public QualifiedSDS getChosenStyle() {

    //
    // May bounce if the user specifies a non-valid integer thickness:
    //
    
    boolean buildADrawStyle = false;
    
    String colorChoice = null;
    if (doColor_) {
      if (!optional_ || setColorBox_.isSelected()) {
        buildADrawStyle = true;
        colorChoice = colorWidget_.getCurrentColor();
      }
    }
      
    int newStyle = SuggestedDrawStyle.NO_STYLE;
    if (!optional_ || setStyleBox_.isSelected()) {
      buildADrawStyle = true;
      newStyle =  ((ChoiceContent)styleCombo_.getSelectedItem()).val;      
    }

    int thickVal = SuggestedDrawStyle.NO_THICKNESS_SPECIFIED;    
    if (!optional_ || setThicknessBox_.isSelected()) {
      buildADrawStyle = true;
      thickVal =  ((ChoiceContent)thicknessCombo_.getSelectedItem()).val;
      if (SuggestedDrawStyle.customThicknessRequest(thickVal)) {
        String thickStr = thicknessField_.getText();
        boolean badVal = false;
        try {
          thickVal = Integer.parseInt(thickStr);
          if (thickVal <= 0) {
            badVal = true;
          }
        } catch (NumberFormatException ex) {
          badVal = true;
        }
        if (badVal) {
          ResourceManager rMan = dacx_.getRMan();
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        rMan.getString("segSpecial.badThick"), 
                                        rMan.getString("segSpecial.badThickTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (new QualifiedSDS(false, null));
        }
      }
    }
 
    SuggestedDrawStyle sds = (buildADrawStyle) ? new SuggestedDrawStyle(newStyle, colorChoice, thickVal) : null;
    return (new QualifiedSDS(true, sds));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static class QualifiedSDS {
    public boolean isOK;
    public SuggestedDrawStyle sds;
    
    QualifiedSDS(boolean isOK, SuggestedDrawStyle sds) {
      this.isOK = isOK;
      this.sds = sds;      
    }  
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** Enable color
  ** 
  */
  
  private void enableColor(boolean enabled) {
    colorWidget_.setEnabled(enabled);
    return;
  }  
  
  /***************************************************************************
  **
  ** Enable style
  ** 
  */
  
  private void enableStyle(boolean enabled) {
    styleCombo_.setEnabled(enabled);
    return;
  }  
  
  /***************************************************************************
  **
  ** Enable thickness
  ** 
  */
  
  private void enableThick(boolean enabled) {
    thicknessCombo_.setEnabled(enabled);
    if (enabled) {
      syncCustomThick();
    } else {
      thicknessField_.setEnabled(false);
      customLabel_.setEnabled(false);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Enable custom thickness
  ** 
  */
  
  private void syncCustomThick() {
    ChoiceContent thickChoice = (ChoiceContent)thicknessCombo_.getSelectedItem();
    boolean isCustom = SuggestedDrawStyle.customThicknessRequest(thickChoice.val);
    thicknessField_.setEnabled(isCustom);
    customLabel_.setEnabled(isCustom);
    return;
  }  
  

}
