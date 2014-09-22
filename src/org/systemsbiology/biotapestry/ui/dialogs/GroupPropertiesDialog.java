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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ColorDeletionListener;
import org.systemsbiology.biotapestry.util.ColorSelectionWidget;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing group properties
*/

public class GroupPropertiesDialog extends JDialog {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JTextField nameField_;
  private ColorSelectionWidget colorWidget1_;
  private ColorSelectionWidget colorWidget2_;  
  private GroupProperties props_;
  private String layoutKey_;
  private JComboBox layerCombo_;
  private JComboBox tpadCombo_;
  private JComboBox bpadCombo_;
  private JComboBox lpadCombo_;
  private JComboBox rpadCombo_;
  private BTState appState_; 
  private DataAccessContext dacx_;
  private JFrame parent_;
  private JComboBox hideCombo_;
  
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
  
  public GroupPropertiesDialog(BTState appState, DataAccessContext dacx,
                               GroupProperties props, boolean forSubGroup) {     
    super(appState.getTopFrame(), appState.getRMan().getString("groupprop.title"), true);
    dacx_ = dacx;
    props_ = props;
    layoutKey_ = dacx_.getLayoutID();
    appState_ = appState;
    
    ResourceManager rMan = appState_.getRMan();    
    setSize(900, 400);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    

    //
    // Build the name panel:
    //

    JLabel label = new JLabel(rMan.getString("groupprop.name"));
    nameField_ = new JTextField();
    int rowNum = 0;
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, rowNum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(nameField_, gbc);
    
    colorWidget1_ = new ColorSelectionWidget(appState_, dacx_, null, true, "groupprop.activecolor", false, false);
    ArrayList<ColorDeletionListener> colorDeletionListeners = new ArrayList<ColorDeletionListener>();
    colorDeletionListeners.add(colorWidget1_);
    colorWidget2_ = new ColorSelectionWidget(appState_, dacx_, colorDeletionListeners, true, "groupprop.inactivecolor", true, false);    
    
    UiUtil.gbcSet(gbc, 0, rowNum++, 11, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(colorWidget1_, gbc); 
    
    UiUtil.gbcSet(gbc, 0, rowNum++, 11, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(colorWidget2_, gbc);     
    
    //
    // Add the layer and pad choices:
    //
    
    if (forSubGroup) {
      label = new JLabel(rMan.getString("groupprop.layer"));
      Vector<Integer> layers = new Vector<Integer>();
      for (int i = 1; i <= GroupProperties.MAX_SUBGROUP_LAYER; i++) {
        layers.add(new Integer(i));
      }
      layerCombo_ = new JComboBox(layers);      
          
      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
      cp.add(label, gbc);
    
      UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
      cp.add(layerCombo_, gbc);
    } else {
      layerCombo_ = null;
    }

    JLabel tlabel = new JLabel(rMan.getString("groupprop.tpad"));
    JLabel blabel = new JLabel(rMan.getString("groupprop.bpad"));
    JLabel rlabel = new JLabel(rMan.getString("groupprop.rpad"));
    JLabel llabel = new JLabel(rMan.getString("groupprop.lpad"));    
    Vector<Integer> pads = new Vector<Integer>();
    for (int i = -40; i <= 400; i++) {
      pads.add(new Integer(i));
    }   
    tpadCombo_ = new JComboBox(pads);
    bpadCombo_ = new JComboBox(pads);
    lpadCombo_ = new JComboBox(pads);
    rpadCombo_ = new JComboBox(pads);    
      
    UiUtil.gbcSet(gbc, 2, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(tlabel, gbc);
    
    UiUtil.gbcSet(gbc, 3, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    cp.add(tpadCombo_, gbc); 
   
    UiUtil.gbcSet(gbc, 4, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(blabel, gbc);
    
    UiUtil.gbcSet(gbc, 5, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    cp.add(bpadCombo_, gbc);    
    
    UiUtil.gbcSet(gbc, 6, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(llabel, gbc);
    
    UiUtil.gbcSet(gbc, 7, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    cp.add(lpadCombo_, gbc);
    
    UiUtil.gbcSet(gbc, 8, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(rlabel, gbc);
    
    UiUtil.gbcSet(gbc, 9, rowNum++, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    cp.add(rpadCombo_, gbc); 
    
    //
    // Build the hide node name
    //        
    
    
    JLabel hlabel = new JLabel(rMan.getString("groupprop.hideLabel"));
    Vector<ChoiceContent> hideChoices = GroupProperties.getLabelHidingChoices(appState_);
    hideCombo_ = new JComboBox(hideChoices);
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(hlabel, gbc);         
    UiUtil.gbcSet(gbc, 1, rowNum++, 10, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    cp.add(hideCombo_, gbc);           
      
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
            GroupPropertiesDialog.this.setVisible(false);
            GroupPropertiesDialog.this.dispose();
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
          GroupPropertiesDialog.this.setVisible(false);
          GroupPropertiesDialog.this.dispose();
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
    UiUtil.gbcSet(gbc, 0, rowNum, 11, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
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
  ** Apply the current group property values to our UI components
  ** 
  */
  
  private void displayProperties() {
    String ref = props_.getReference();
    Layout layout = dacx_.lSrc.getLayout(layoutKey_);
    String genomeKey = layout.getTarget();
    GenomeInstance genomeInstance = (GenomeInstance)dacx_.getGenomeSource().getGenome(genomeKey);
    Group group = genomeInstance.getGroup(ref);
 
    nameField_.setText(group.getName());
    colorWidget1_.setCurrentColor(props_.getColorTag(true));    
    colorWidget2_.setCurrentColor(props_.getColorTag(false));
    
    if (layerCombo_ != null) {
      layerCombo_.setSelectedItem(new Integer(props_.getLayer()));
    }
    tpadCombo_.setSelectedItem(new Integer(props_.getPadding(GroupProperties.TOP)));
    bpadCombo_.setSelectedItem(new Integer(props_.getPadding(GroupProperties.BOTTOM)));
    lpadCombo_.setSelectedItem(new Integer(props_.getPadding(GroupProperties.LEFT)));
    rpadCombo_.setSelectedItem(new Integer(props_.getPadding(GroupProperties.RIGHT)));
    
    int hideMode = props_.getHideLabelMode();
    hideCombo_.setSelectedItem(GroupProperties.labelHidingForCombo(appState_, hideMode));
    
    return;
  }

  /***************************************************************************
  **
  ** Apply our UI values to the group properties
  ** 
  */
  
  private boolean applyProperties() {
    String ref = props_.getReference();
    Layout layout = dacx_.lSrc.getLayout(layoutKey_);
    String genomeKey = layout.getTarget();
    GenomeInstance genomeInstance = (GenomeInstance)dacx_.getGenomeSource().getGenome(genomeKey);
    Group group = genomeInstance.getGroup(ref);

    UndoSupport support = new UndoSupport(appState_, "undo.groupprop");
    boolean submit = false;
      
    // FIX ME!!!! Name must be unique (though it can match existing name of course!)
    String gname = nameField_.getText();
    gname = gname.trim();
    String oldName = group.getName();
    
    if (!gname.equals(oldName)) {
      if (genomeInstance.groupNameInUse(gname, ref)) {
        ResourceManager rMan = appState_.getRMan();
        String desc = MessageFormat.format(rMan.getString("groupprops.NameInUse"), 
                                           new Object[] {gname});
        JOptionPane.showMessageDialog(parent_, desc, 
                                      rMan.getString("groupprops.nameError"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
      
      //
      // If name change disconnects from underlying data, we let user choose how
      // to handle it:
      //
    
      if ((oldName != null) && dacx_.getExpDataSrc().hasRegionAttachedByDefault(group)) {    
        RegionNameChangeChoicesDialog rnccd = 
          new RegionNameChangeChoicesDialog(appState_, dacx_, ref, oldName, gname, genomeInstance, group, support);  
        rnccd.setVisible(true);
        if (rnccd.userCancelled()) {
          return (false);
        }
      }

      if (gname.trim().equals("")) {
        gname = null;
      }
      
      GenomeChange chng = new GenomeChange();
      chng.grOrig = new Group(group);
      group.setName(gname);
      chng.grNew = new Group(group);
      chng.genomeKey = genomeKey;    
      support.addEdit(new GenomeChangeCmd(appState_, dacx_, chng));
      // Actually, all child model groups change too. FIX ME?
      support.addEvent(new ModelChangeEvent(genomeKey, ModelChangeEvent.UNSPECIFIED_CHANGE));
      submit = true;
    }
    
    //
    // Though we hold layout's property directly, make change by changing
    // a clone and submitting it:
    //
    
    GroupProperties changedProps = new GroupProperties(props_); 
    changedProps.setColor(true, colorWidget1_.getCurrentColor());    
    changedProps.setColor(false, colorWidget2_.getCurrentColor());
        
    if (layerCombo_ != null) {
      Integer layer = (Integer)layerCombo_.getSelectedItem();
      changedProps.setLayer(layer.intValue());
    }
    Integer pad = (Integer)tpadCombo_.getSelectedItem();
    changedProps.setPadding(GroupProperties.TOP, pad.intValue());
    pad = (Integer)bpadCombo_.getSelectedItem();
    changedProps.setPadding(GroupProperties.BOTTOM, pad.intValue());
    pad = (Integer)lpadCombo_.getSelectedItem();
    changedProps.setPadding(GroupProperties.LEFT, pad.intValue());
    pad = (Integer)rpadCombo_.getSelectedItem();
    changedProps.setPadding(GroupProperties.RIGHT, pad.intValue());
       
    int hideMode =  ((ChoiceContent)hideCombo_.getSelectedItem()).val;
    changedProps.setHideLabel(hideMode);
   
    Layout.PropChange[] lpc = new Layout.PropChange[1];    
    lpc[0] = layout.replaceGroupProperties(props_, changedProps); 
        
    if (lpc[0] != null) {
      PropChangeCmd mov = new PropChangeCmd(appState_, dacx_, lpc);
      support.addEdit(mov);
      props_ = changedProps;
      support.addEvent(new LayoutChangeEvent(layoutKey_, LayoutChangeEvent.UNSPECIFIED_CHANGE));
      submit = true;
    }
    
    if (submit) {
      support.finish();
    }
    return (true);
  }  
}
