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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.undo.FontChangeCmd;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.ui.FontChange;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing font properties
*/

public class FontDialog extends JDialog {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private JComboBox[] sizeCombo_;
  private JCheckBox[] useSerif_;
  private JCheckBox[] useBold_;
  private JCheckBox[] useItalic_;  
  private String layoutKey_;  
  private StaticDataAccessContext dacx_;
  private UIComponentSource uics_;
  private UndoFactory uFac_;
  
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
  
  public FontDialog(UIComponentSource uics, StaticDataAccessContext dacx, String layoutKey, UndoFactory uFac) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("fontDialog.title"), true);
    layoutKey_ = layoutKey;
    dacx_ = dacx;
    uics_ = uics;
    uFac_ = uFac;
    ResourceManager rMan = dacx_.getRMan();

    setSize(600, 500);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    

    //
    // Build a font setting for each variable font:
    //

    FontManager mgr = dacx_.getFontManager();
    int numFonts = mgr.getNumFonts();
    sizeCombo_ = new JComboBox[numFonts];
    useSerif_ = new JCheckBox[numFonts];
    useBold_ = new JCheckBox[numFonts]; 
    useItalic_ = new JCheckBox[numFonts];     
    
    int min = FontManager.MIN_SIZE;
    int max = FontManager.MAX_SIZE;
    Vector<Integer> choices = new Vector<Integer>();
    for (int i = min; i <= max; i++) {
      choices.add(new Integer(i));
    }
    String serifLabel = rMan.getString("fontDialog.serifLabel");
    String boldLabel = rMan.getString("fontDialog.boldLabel");
    String italicLabel = rMan.getString("fontDialog.italicLabel");    
    String sizeLabel = rMan.getString("fontDialog.sizeLabel");    
    
    for (int i = 0; i < numFonts; i++) {
      String fontKey = mgr.getFontTag(i);
      JLabel label = new JLabel(rMan.getString("fontDialog.fontLabel_" + fontKey) + ":");
      sizeCombo_[i] = new JComboBox(choices);
      useSerif_[i] = new JCheckBox(serifLabel);
      useBold_[i] = new JCheckBox(boldLabel); 
      useItalic_[i] = new JCheckBox(italicLabel);       
      UiUtil.gbcSet(gbc, 0, i, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
      cp.add(label, gbc);
      UiUtil.gbcSet(gbc, 1, i, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
      cp.add(new JLabel(sizeLabel), gbc);      
      UiUtil.gbcSet(gbc, 2, i, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      cp.add(sizeCombo_[i], gbc);
      UiUtil.gbcSet(gbc, 3, i, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 1.0);
      cp.add(useSerif_[i], gbc);
      UiUtil.gbcSet(gbc, 4, i, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 1.0);
      cp.add(useBold_[i], gbc);
      UiUtil.gbcSet(gbc, 5, i, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 1.0);
      cp.add(useItalic_[i], gbc);      
    }
    
    //
    // Build the button panel:
    //
    
    FixedJButton buttonR = new FixedJButton(rMan.getString("dialogs.reset"));
    buttonR.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          resetToDefaults();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });         
    
    FixedJButton buttonA = new FixedJButton(rMan.getString("dialogs.apply"));
    buttonA.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          applyProperties();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          applyProperties();
          FontDialog.this.setVisible(false);
          FontDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          FontDialog.this.setVisible(false);
          FontDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalStrut(10)); 
    buttonPanel.add(buttonR);
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonA);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);

    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, numFonts, 6, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(uics_.getTopFrame());
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
  ** Apply the font values to our UI components
  ** 
  */
  
  private void displayProperties() {
    FontManager mgr = dacx_.getFontManager();
    int numFonts = mgr.getNumFonts();   
    
    for (int i = 0; i < numFonts; i++) {      
      sizeCombo_[i].setSelectedItem(new Integer(mgr.getPointSize(i)));
      useSerif_[i].setSelected(!mgr.isFontSansSerif(i));
      useBold_[i].setSelected(mgr.isFontBold(i));  
      useItalic_[i].setSelected(mgr.isFontItalic(i));       
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Apply our UI values to the fonts
  ** 
  */
  
  private void resetToDefaults() {
    FontManager mgr = dacx_.getFontManager();
    int numFonts = mgr.getNumFonts();   
    
    for (int i = 0; i < numFonts; i++) {
      Font dFont = mgr.getDefaultFont(i);
      sizeCombo_[i].setSelectedItem(new Integer(dFont.getSize()));
      useSerif_[i].setSelected(!FontManager.isSansSerif(dFont));
      useBold_[i].setSelected(dFont.isBold());  
      useItalic_[i].setSelected(dFont.isItalic());       
    }    
    return;
  }    

  /***************************************************************************
  **
  ** Apply our UI values to the fonts
  ** 
  */
  
  private void applyProperties() {
    
    UiUtil.fixMePrintout("NO! This happens for ALL tabs, not just current!");
    Map<String, Layout.PadNeedsForLayout> globalPadNeeds = dacx_.getFGHO().getGlobalNetModuleLinkPadNeeds();
   
    //
    // Undo/Redo support
    //
    UndoSupport support = uFac_.provideUndoSupport("undo.fontDialog", dacx_);     

    FontManager mgr = dacx_.getFontManager();
    int numFonts = mgr.getNumFonts(); 
    ArrayList<FontManager.FontSpec> fontSpecs = new ArrayList<FontManager.FontSpec>();
    for (int i = 0; i < numFonts; i++) {      
      int size = ((Integer)sizeCombo_[i].getSelectedItem()).intValue();
      boolean isSans = !useSerif_[i].isSelected();
      boolean isBold = useBold_[i].isSelected();
      boolean isItalic = useItalic_[i].isSelected();      
      FontManager.FontSpec fs = new FontManager.FontSpec(i, size, isBold, isItalic, isSans);
      fontSpecs.add(fs);
    }
    FontChange chg = mgr.setFonts(fontSpecs);
    FontChangeCmd fcc = new FontChangeCmd(chg);
    support.addEdit(fcc);
    
    // Actually, ALL layouts get changed (FIX ME):
    UiUtil.fixMePrintout("ALL Layouts across ALL tabs!");
    UiUtil.fixMePrintout("Currently just firing a redraw in SUPanel");
    LayoutChangeEvent lcev = new LayoutChangeEvent(layoutKey_, LayoutChangeEvent.UNSPECIFIED_CHANGE);
    support.addEvent(lcev);        

    ModificationCommands.repairNetModuleLinkPadsGlobally(dacx_, globalPadNeeds, false, support);

    //
    // Finish undo support:
    //
    
    support.finish(); 
    return;
  }  
}
