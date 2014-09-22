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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.GlobalChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GlobalChange;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NamedColor;
import org.systemsbiology.biotapestry.util.ColorDeletionListener;
import org.systemsbiology.biotapestry.util.ColorListRenderer;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing colors
*/

public class ColorEditorDialog extends JDialog implements ListSelectionListener  {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private JFrame parent_;
  private JList colorListing_;
  private HashSet<String> origKeys_;
  private ArrayList<ColorListRenderer.ColorSource> colorList_;  
  private ColorListRenderer renderer_;
  private JScrollPane jsp_;
  private JTextField textField_;
  private JColorChooser chooser_;
  private FixedJButton buttonD_;
  private NamedColor onDisplay_;
  private boolean updatingUI_;
  private List<ColorDeletionListener> cdls_;
  private BTState appState_;
  private DataAccessContext dacx_;
  
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
  
  public ColorEditorDialog(BTState appState, DataAccessContext dacx, List<ColorDeletionListener> colorDeletionListeners) {     
    super(appState.getTopFrame(), appState.getRMan().getString("colorDialog.title"), true);
    ResourceManager rMan = appState.getRMan();
    appState_ = appState;
    dacx_ = dacx;
    cdls_ = colorDeletionListeners;
    updatingUI_ = false;
    
    setSize(750, 500);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    

    displayProperties();
    
    colorListing_ = new JList(colorList_.toArray());
    colorListing_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    colorListing_.addListSelectionListener(this);
    renderer_ = new ColorListRenderer(colorList_, appState_);
    colorListing_.setCellRenderer(renderer_);
    jsp_ = new JScrollPane(colorListing_);
    jsp_.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jsp_.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);    
    
    //
    // Put the list on the left, text box top right, and a JColorChooser on the right
    //
    
    UiUtil.gbcSet(gbc, 0, 0, 3, 5, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
    cp.add(jsp_, gbc);
   
    JLabel label = new JLabel(rMan.getString("colorDialog.selColor"));
    UiUtil.gbcSet(gbc, 3, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
    cp.add(label, gbc);    
    textField_ = new JTextField();
    UiUtil.gbcSet(gbc, 4, 0, 4, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);       
    cp.add(textField_, gbc);    
    
    chooser_ = new JColorChooser();
    AbstractColorChooserPanel[] vals = chooser_.getChooserPanels();
    chooser_.removeChooserPanel(vals[0]);  // Dump the swatches
    //chooser_.setPreviewPanel(new JPanel());
    UiUtil.gbcSet(gbc, 3, 1, 5, 4, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(chooser_, gbc);
    chooser_.setEnabled(false);
    textField_.setEnabled(false);
    
    FixedJButton buttonN = new FixedJButton(rMan.getString("dialogs.addEntry"));
    buttonN.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          ColorEditorDialog.this.addAColor();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    buttonD_ = new FixedJButton(rMan.getString("dialogs.deleteEntry"));
    buttonD_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          ColorEditorDialog.this.deleteAColor();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    buttonD_.setEnabled(false);
    
    FixedJButton buttonS = new FixedJButton(rMan.getString("colorDialog.sort"));
    buttonS.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          ColorEditorDialog.this.sort();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });    
    
    //
    // Add the add/delete buttons
    //
    
    UiUtil.gbcSet(gbc, 0, 6, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(buttonN, gbc);
    
    UiUtil.gbcSet(gbc, 1, 6, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(buttonD_, gbc); 
    
    UiUtil.gbcSet(gbc, 2, 6, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(buttonS, gbc);    
    
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
            ColorEditorDialog.this.setVisible(false);
            ColorEditorDialog.this.dispose();
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
          ColorEditorDialog.this.setVisible(false);
          ColorEditorDialog.this.dispose();
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
    UiUtil.gbcSet(gbc, 0, 7, 8, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(appState_.getTopFrame());
    //displayProperties();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
    
  /***************************************************************************
  **
  ** List selection stuff
  ** 
  */  

  public void valueChanged(ListSelectionEvent e) {
    try {
      if (updatingUI_) {
        return;
      }
      if (e.getValueIsAdjusting()) {
        return;
      }
      extractToList();

      NamedColor color = (NamedColor)colorListing_.getSelectedValue();
      if (color != null) {
        chooser_.setColor(color.color);     
        buttonD_.setEnabled(true);
        onDisplay_ = color;
        textField_.setText(color.name);
        chooser_.setEnabled(true);
        textField_.setEnabled(true);
      } else {
        chooser_.setColor(Color.black);       
        buttonD_.setEnabled(false);
        onDisplay_ = null;
        textField_.setText("");
        chooser_.setEnabled(false);
        textField_.setEnabled(false);      
      }
      chooser_.revalidate();
      textField_.revalidate();         
      colorListing_.revalidate();
      buttonD_.revalidate();
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
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

  
  /***************************************************************************
  **
  ** Extract data and apply to list
  ** 
  */  

  private void extractToList() {
    if (onDisplay_ != null) {
      String newName = textField_.getText().trim();
      if (!newName.equals(onDisplay_.name)) {
        HashSet<String> names = buildNames();
        if (names.contains(newName)) {
          ResourceManager rMan = appState_.getRMan();
          String desc = MessageFormat.format(rMan.getString("addColor.NameInUse"), 
                                             new Object[] {newName});
          JOptionPane.showMessageDialog(parent_, desc, 
                                        rMan.getString("addColor.CreationErrorTitle"),
                                        JOptionPane.ERROR_MESSAGE);         
        } else { 
          onDisplay_.name = newName;
        }
      }
      onDisplay_.color = chooser_.getColor();
    }  
    return;    
  }  

  /***************************************************************************
  **
  ** Resort the list
  ** 
  */
  
  private void sort() {
    extractToList();
    Collections.sort(colorList_);
    Object selected = colorListing_.getSelectedValue();
    updatingUI_ = true;
    colorListing_.setListData(colorList_.toArray());
    colorListing_.setSelectedValue(selected, true);
    updatingUI_ = false;    
    colorListing_.revalidate();
    colorListing_.repaint();
    return;
  }  

  /***************************************************************************
  **
  ** Apply the color values to our UI components
  ** 
  */
  
  private void displayProperties() {
    
    Iterator<String> cit = dacx_.cRes.getColorKeys();
    colorList_ = new ArrayList<ColorListRenderer.ColorSource>();
    origKeys_ = new HashSet<String>();
    
    while (cit.hasNext()) {
      String colorKey = cit.next();
      NamedColor col = dacx_.cRes.getNamedColor(colorKey);
      colorList_.add(new NamedColor(col));
      origKeys_.add(colorKey);
    }
    Collections.sort(colorList_);
    
    return;
  }

  /***************************************************************************
  **
  ** Apply our UI values to the colors
  ** 
  */
  
  private boolean applyProperties() {
    
    //
    // Undo/Redo support
    //
    
    UndoSupport support = new UndoSupport(appState_, "undo.colorDialog");

    //
    // Grab latest color, if appropriate:
    //

    if (onDisplay_ != null) {
      String newName = textField_.getText().trim();
      if (!newName.equals(onDisplay_.name)) {
        HashSet<String> names = buildNames();
        if (names.contains(newName)) {
          String desc = MessageFormat.format(dacx_.rMan.getString("addColor.NameInUse"), 
                                             new Object[] {newName});
          JOptionPane.showMessageDialog(parent_, desc, 
                                        dacx_.rMan.getString("addColor.CreationErrorTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        } else { 
          onDisplay_.name = newName;
        }
      }
      onDisplay_.color = chooser_.getColor();
      colorListing_.revalidate();
      colorListing_.repaint();
    }
    
    //
    // Build new map, Figure out the deleted colors:
    //
    
    HashMap<String, NamedColor> newColors = new HashMap<String, NamedColor>();
    Iterator<ColorListRenderer.ColorSource> cit = colorList_.iterator();
    while (cit.hasNext()) {
      NamedColor color = (NamedColor)cit.next();
      newColors.put(color.key, color);
    }
   
    Set<String> newKeys = newColors.keySet();
    HashSet<String> intersect = new HashSet<String>(newKeys);
    intersect.retainAll(origKeys_);
    
    HashSet<String> deleted = new HashSet<String>(origKeys_);
    deleted.removeAll(intersect);
    
    //
    // Crank thru the deleted colors and change them to black:
    //
    
    Iterator<Layout> lit = dacx_.lSrc.getLayoutIterator();
    while (lit.hasNext()) {
      Layout lo = lit.next();
      Iterator<String> delit = deleted.iterator();
      while (delit.hasNext()) {
        String key = delit.next();
        //
        // Important!  Must not replace with a deletable color!
        //
        Layout.PropChange[] lpc = lo.replaceColor(key, "black");   
        if (lpc != null) {
          PropChangeCmd pcc = new PropChangeCmd(appState_, dacx_, lpc);
          support.addEdit(pcc);
        }
      }

      LayoutChangeEvent lcev = new LayoutChangeEvent(lo.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(lcev);      
    }
   
    //
    // Update the color map:
    //
    
    GlobalChange gc = dacx_.cRes.updateColors(newColors);   
    GlobalChangeCmd gcc = new GlobalChangeCmd(appState_, dacx_, gc);
    support.addEdit(gcc);
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.UNSPECIFIED_CHANGE));

    //
    // Inform listeners:
    //
    
    if ((cdls_ != null) && !cdls_.isEmpty()) {
      HashMap<String, String> colorMap = new HashMap<String, String>();
      Iterator<String> delit = deleted.iterator();
      while (delit.hasNext()) {
        String key = delit.next();
        colorMap.put(key, "black");
      }
      Iterator<ColorDeletionListener> cdlit = cdls_.iterator();
      while (cdlit.hasNext()) {
        ColorDeletionListener cdl = cdlit.next();
        cdl.colorReplacement(colorMap);
      } 
    }
    
    //
    // Finish undo support:
    //
    
    support.finish();
    return (true);
  }
  
  /***************************************************************************
  **
  ** Get a new unique color name
  */
  
  private String getUniqueColorName(HashSet<String> names) {
    String rootName = dacx_.rMan.getString("addColor.NewNameRoot");    
    StringBuffer buf = new StringBuffer();
    String testName = rootName;
    int count = 1;
    while (true) {
      if (!names.contains(testName)) {
        return (testName);
      }
      buf.setLength(0);
      buf.append(rootName);
      buf.append("_");
      buf.append(count++);
      testName = buf.toString();
    }
  }
  
  /***************************************************************************
  **
  ** Add a new color
  ** 
  */
  
  private HashSet<String> buildNames() {  
    //
    // Build set of existing names
    //
    Iterator<ColorListRenderer.ColorSource> cit = colorList_.iterator();
    HashSet<String> names = new HashSet<String>();
    while (cit.hasNext()) {
      ColorListRenderer.ColorSource color = cit.next();
      names.add(color.getDescription());
    }
    return (names);
  }
  
  /***************************************************************************
  **
  ** Add a new color
  ** 
  */
  
  private void addAColor() {
    
    //
    // Build set of existing names
    //

    HashSet<String> names = buildNames();
    
    //
    // Get a name for the color
    //
    
    String newName = null;
    while (true) {
      newName = (String)JOptionPane.showInputDialog(parent_, 
                                       dacx_.rMan.getString("addColor.ChooseName"), 
                                       dacx_.rMan.getString("addColor.ChooseTitle"),
                                       JOptionPane.QUESTION_MESSAGE, 
                                       null, null, getUniqueColorName(names)); 
      if (newName == null) {
        return;
      }
      
      if (names.contains(newName)) {
        String desc = MessageFormat.format(dacx_.rMan.getString("addColor.NameInUse"), 
                                           new Object[] {newName});
        JOptionPane.showMessageDialog(parent_, desc, 
                                      dacx_.rMan.getString("addColor.CreationErrorTitle"),
                                      JOptionPane.ERROR_MESSAGE);         
      } else {
        break;
      }
    }
    
    //
    // Now enter the color into the list with a neutral grey:
    //
    
    String colKey = dacx_.cRes.getNextColorLabel();
    NamedColor newCol = new NamedColor(colKey, new Color(127, 127, 127), newName);
    colorList_.add(newCol);
    updatingUI_ = true;
    colorListing_.setListData(colorList_.toArray());
    updatingUI_ = false;
    colorListing_.setSelectedValue(newCol, true);    
    
    return;
  } 
  
  /***************************************************************************
  **
  ** Delete a color
  ** 
  */
  
  private void deleteAColor() {

    NamedColor color = (NamedColor)colorListing_.getSelectedValue();
    if (color != null) {
      if (dacx_.cRes.cannotDeleteColors().contains(color.key)) {
        JOptionPane.showMessageDialog(parent_, dacx_.rMan.getString("deleteColor.CannotDelete"), 
                                      dacx_.rMan.getString("deleteColor.CannotDeleteTitle"),
                                      JOptionPane.ERROR_MESSAGE);         
        return;
      }
    }
   
    int ok = 
      JOptionPane.showConfirmDialog(parent_, 
                                    dacx_.rMan.getString("deleteColor.ConfirmDelete"), 
                                    dacx_.rMan.getString("deleteColor.ConfirmDeleteTitle"),
                                    JOptionPane.YES_NO_OPTION);
    if (ok != JOptionPane.YES_OPTION) {
      return;
    }
    //
    // Take it out of the display list, update the UI widget, null out the display
    // flag:
    //
    
    updatingUI_ = true;
    color = (NamedColor)colorListing_.getSelectedValue();
    if (color != null) {
      chooser_.setColor(Color.black);       
      buttonD_.setEnabled(false);
      onDisplay_ = null;
    }    
    colorList_.remove(color);
    colorListing_.setSelectedIndex(-1);
    colorListing_.setListData(colorList_.toArray());
    updatingUI_ = false;

    return;
  }   
}
