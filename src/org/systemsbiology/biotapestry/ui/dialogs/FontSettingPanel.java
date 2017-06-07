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
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import java.util.Vector;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Panel for handling settings of a font
*/

public class FontSettingPanel extends JPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private JComboBox sizeCombo_;
  private JCheckBox useSerif_;
  private JCheckBox useBold_;
  private JCheckBox useItalic_;
  private JLabel sizeLabel_;
  
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
  
  public FontSettingPanel(DataAccessContext dacx) {
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    ResourceManager rMan = dacx.getRMan();
  
    sizeCombo_ = new JComboBox();
    useSerif_ = new JCheckBox();
    useBold_ = new JCheckBox(); 
    useItalic_ = new JCheckBox();     
    
    int min = FontManager.MIN_SIZE;
    int max = FontManager.MAX_SIZE;
    Vector<Integer> choices = new Vector<Integer>();
    for (int i = min; i <= max; i++) {
      choices.add(new Integer(i));
    }
    String serifLabel = rMan.getString("fontDialog.serifLabel");
    String boldLabel = rMan.getString("fontDialog.boldLabel");
    String italicLabel = rMan.getString("fontDialog.italicLabel");    
    String sizeTag = rMan.getString("fontDialog.sizeLabel");
       
    sizeLabel_ = new JLabel(sizeTag);     
    sizeCombo_ = new JComboBox(choices);
    useSerif_ = new JCheckBox(serifLabel);
    useBold_ = new JCheckBox(boldLabel); 
    useItalic_ = new JCheckBox(italicLabel);       
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    add(sizeLabel_, gbc);      
    UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    add(sizeCombo_, gbc);
    UiUtil.gbcSet(gbc, 2, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 1.0);
    add(useSerif_, gbc);
    UiUtil.gbcSet(gbc, 3, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 1.0);
    add(useBold_, gbc);
    UiUtil.gbcSet(gbc, 4, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 1.0);
    add(useItalic_, gbc);
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
  
  public void displayProperties(Font currFont) { 
    sizeCombo_.setSelectedItem(new Integer(currFont.getSize()));
    useSerif_.setSelected(!FontManager.isSansSerif(currFont));
    useBold_.setSelected(currFont.isBold());  
    useItalic_.setSelected(currFont.isItalic());           
    return;
  }

  /***************************************************************************
  **
  ** Apply our UI values to get the result
  */
  
  public FontManager.FontOverride getFontResult() {
    int size = ((Integer)sizeCombo_.getSelectedItem()).intValue();
    boolean isSans = !useSerif_.isSelected();
    boolean isBold = useBold_.isSelected();
    boolean isItalic = useItalic_.isSelected();      
    return (new FontManager.FontOverride(size, isBold, isItalic, isSans));
  }
  
  /***************************************************************************
  **
  ** Set enabled state
  */
  
  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    sizeLabel_.setEnabled(enabled);
    sizeCombo_.setEnabled(enabled);
    useSerif_.setEnabled(enabled);
    useBold_.setEnabled(enabled);
    useItalic_.setEnabled(enabled);
    return;
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
}
