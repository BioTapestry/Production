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


package org.systemsbiology.biotapestry.util;

import java.util.Vector;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.DefaultComboBoxModel;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.ListCellRenderer;
import javax.swing.JList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.awt.Component;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import org.systemsbiology.biotapestry.ui.NamedColor;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.dialogs.ColorEditorDialog;

/****************************************************************************
**
** Widget for selecting colors
*/

public class ColorSelectionWidget extends JPanel implements ColorDeletionListener {

  
  public final static String VARIOUS_TAG = "__BT_WJRL_UNCHANGED_VARIOUS__";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private JComboBox colorCombo_;
  private BTState appState_;
  private DataAccessContext dacx_;
  private HashMap<String, String> changedColors_;
  private ColorListRenderer renderer_;
  private JButton editLaunch_;
  private List<ColorDeletionListener> colorDeletionListeners_;
  private boolean addVarious_;
  private NamedColor variousChoice_;
  
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
  
  public ColorSelectionWidget(BTState appState,
                              DataAccessContext dacx,
                              List<ColorDeletionListener> colorDeletionListeners, 
                              boolean showLabel, String altTag, 
                              boolean showButton, boolean addVarious) {     
   
    appState_ = appState;
    dacx_= dacx;
    setLayout(new GridBagLayout());    
    GridBagConstraints gbc = new GridBagConstraints(); 
    ResourceManager rMan = appState_.getRMan();
    addVarious_ = addVarious;
    if (addVarious) {
      variousChoice_ = new NamedColor(VARIOUS_TAG, new Color(225, 225, 225), rMan.getString("colorSelector.unchanged"));
    }
    
    changedColors_ = new HashMap<String, String>();
    colorDeletionListeners_ = (colorDeletionListeners == null) ? new ArrayList<ColorDeletionListener>() 
                                                               : colorDeletionListeners; // Need a global copy
    colorDeletionListeners_.add(this);
    
    //
    // Build the color panel:
    //
    
    JLabel label = null;
    if (showLabel) {
      String tag = (altTag == null) ? "colorSelector.color" : altTag;
      label = new JLabel(rMan.getString(tag));
    }
    Vector<ObjChoiceContent> choices = new Vector<ObjChoiceContent>();
    ArrayList<NamedColor> colorList = new ArrayList<NamedColor>();
    stockColorLists(choices, colorList);    
    
    colorCombo_ = new JComboBox(choices);
    renderer_ = new ColorListRenderer(colorList, colorCombo_);
    colorCombo_.setRenderer(renderer_);
    colorCombo_.addActionListener(renderer_);
       
    JLabel dummyLabel = null;
    if (showButton) {
      editLaunch_ = new JButton(rMan.getString("colorSelector.colorNew")); 
      editLaunch_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            changedColors_.clear();
            ColorEditorDialog ced = new ColorEditorDialog(appState_, dacx_, colorDeletionListeners_);
            ced.setVisible(true);
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });    
    } else {
      // Very poor and cruddy quick hack:
      String replacement = rMan.getString("colorSelector.colorNew");
      int dummyLength = replacement.length() + 20; 
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < dummyLength; i++) {
        buf.append(' ');
      }
      dummyLabel = new JLabel(buf.toString());
    }

    int rowNum = 0;
    int colNum = 0;
    if (label != null) {
      UiUtil.gbcSet(gbc, colNum++, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
      add(label, gbc);
    }
    
    UiUtil.gbcSet(gbc, colNum, rowNum, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.5, 1.0);
    add(colorCombo_, gbc);
    colNum += 2;

    UiUtil.gbcSet(gbc, colNum, rowNum, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 1.0);    
    if (editLaunch_ != null) {
      add(editLaunch_, gbc);
    } else {
      add(dummyLabel, gbc);
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Used to track color deletions
  ** 
  */
  
  public void colorReplacement(Map<String, String> oldToNew) {
    changedColors_ = new HashMap<String, String>(oldToNew);
    installColorChange();
    repaint();
    return;
  }
  
  /***************************************************************************
  **
  ** Set the current color
  ** 
  */
  
  public void setCurrentColor(String colorKey) {
    NamedColor nc;
    if (addVarious_ && colorKey.equals(VARIOUS_TAG)) {
      nc= variousChoice_;
    } else {
      nc = dacx_.cRes.getNamedColor(colorKey);
    }
    colorCombo_.setSelectedItem(new ObjChoiceContent(nc.name, nc.key));
    return;
  }

  /***************************************************************************
  **
  ** Get the current color
  */
  
  public String getCurrentColor() { 
    ObjChoiceContent colName = (ObjChoiceContent)colorCombo_.getSelectedItem();
    return (colName.val);
  }
  
  /***************************************************************************
  **
  ** Set enabled state
  */
  
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    colorCombo_.setEnabled(enabled);
    if (editLaunch_ != null) {
      editLaunch_.setEnabled(enabled);
    }
    renderer_.setGhosted(!enabled);
    return;
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  
  /***************************************************************************
  **
  ** Handle color changes 
  */ 
  
  private void installColorChange() {
    Vector<ObjChoiceContent> choices = new Vector<ObjChoiceContent>();
    ArrayList<NamedColor> colorList = new ArrayList<NamedColor>();
    stockColorLists(choices, colorList); 
    renderer_.setValues(colorList);          
    //
    // Current selection may have been deleted by the editor:
    //        
    ObjChoiceContent selected = (ObjChoiceContent)colorCombo_.getSelectedItem();
    colorCombo_.setModel(new DefaultComboBoxModel(choices));
    if (choices.contains(selected)) {
      colorCombo_.setSelectedItem(selected);
    } else {
      String newColor = (String)changedColors_.get(selected.val); 
      NamedColor nc = appState_.getDB().getNamedColor(newColor);
      colorCombo_.setSelectedItem(new ObjChoiceContent(nc.name, nc.key));
    }
    return;
  }  
 
  /***************************************************************************
  **
  ** Stock the color choices
  ** 
  */
  
  private void stockColorLists(Vector<ObjChoiceContent> comboChoices, List<NamedColor> renderList) {
    Iterator<String> ckit = dacx_.cRes.getColorKeys();
    while (ckit.hasNext()) {
      String colorKey = ckit.next();
      NamedColor nc = dacx_.cRes.getNamedColor(colorKey);
      renderList.add(new NamedColor(nc));
    }
    
    Collections.sort(renderList);  
    if (addVarious_) {
      renderList.add(0, variousChoice_);
    }
     
    Iterator<NamedColor> rlit = renderList.iterator();
    while (rlit.hasNext()) {
      NamedColor nc = rlit.next();
      comboChoices.add(new ObjChoiceContent(nc.name, nc.key));
    }    
    return;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used for the data table renderer
  */
  
  class ColorListRenderer extends ColorLabelToo implements ListCellRenderer, ActionListener {
    
    private List<NamedColor> values_;
    private int selIndex_;
    private boolean ghosted_;    
    private JComboBox localColorCombo_;
    private static final long serialVersionUID = 1L;
           
    ColorListRenderer(List<NamedColor> values, JComboBox colorCombo) {
      super(Color.white, "");
      values_ = values;
      selIndex_ = -1;
      localColorCombo_ = colorCombo;
      ghosted_ = false;
    }
    
    void setValues(List<NamedColor> values) {
      values_ = values;
      return;
    }
    
    void setGhosted(boolean ghosted) {
      ghosted_ = ghosted;
      return;
    }    
    
    public void actionPerformed(ActionEvent ev) {
      try {
        selIndex_ = localColorCombo_.getSelectedIndex();
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
      return;
    }            
       
    public Component getListCellRendererComponent(JList list, 
                                                  Object value,
                                                  int index,
                                                  boolean isSelected, 
                                                  boolean hasFocus) {
      try {        
        if (value == null) {
          return (this);
        }
        //
        // Java Swing book says this may be needed for combo boxes (not lists):
        //
        if (index == -1) {
          //index = list.getSelectedIndex();  // This can return the current highlight, but
          // not the current selection!      
          index = selIndex_;
          if (index == -1) {
            return (this); // prevent crash, but should not happen...
          }
        }
        NamedColor currCol = values_.get(index);
        Color showColor = (ghosted_) ? currCol.color.darker() : currCol.color;
        setColorValues(showColor, currCol.name);
        setEnabled(!ghosted_);
        setBackground((isSelected) ? list.getSelectionBackground() : list.getBackground());
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }      
      return (this);             
    }
  }  
}
