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


import java.util.ArrayList;
import java.util.Iterator;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeNode;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NavTreeChange;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.ui.DataLocator;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.ui.dialogs.utils.TimeAxisHelper;

/****************************************************************************
**
** Dialog box for editing single model property data
*/

public class SingleInstanceModelPropDialog extends JDialog {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JTextField nameField_;
  private JTextField longNameField_;
  private JTextField descripField_;
  private JTextField minField_;
  private JTextField maxField_;  
  private JLabel minFieldLabel_;
  private JLabel maxFieldLabel_;  
  private JCheckBox timeBoundsBox_;
  private int minParentTime_;
  private int maxParentTime_;
  private int minChildTime_;
  private int maxChildTime_;
  private boolean lineageIsBounded_;
  private TimeAxisHelper timeAxisHelper_; ;
  private String targetID_;
  private StaticDataAccessContext dacx_;
  private UIComponentSource uics_;
  private NavTree nt_; 
  private TreeNode popupNode_;
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
  
  public SingleInstanceModelPropDialog(UIComponentSource uics, StaticDataAccessContext dacx, 
                                       String targetID, NavTree nt, TreeNode popupNode, UndoFactory uFac) {
    super(uics.getTopFrame(), dacx.getRMan().getString("simprop.title"), true);
    uics_ = uics;
    dacx_ = dacx;
    uFac_ = uFac;
    targetID_ = targetID;
    nt_ = nt; 
    popupNode_ = popupNode;
        
    ResourceManager rMan = dacx_.getRMan();    
    setSize(700, 300);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    

    //
    // Build the name panel:
    //
    int rowNum = 0;
    JLabel label = new JLabel(rMan.getString("simprop.name"));
    nameField_ = new JTextField();
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, rowNum++, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(nameField_, gbc);
    
    //
    // Build the long name panel:
    //

    label = new JLabel(rMan.getString("simprop.longName"));
    longNameField_ = new JTextField();
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, rowNum++, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(longNameField_, gbc);
    
    //
    // Build the description panel:
    //

    label = new JLabel(rMan.getString("simprop.descrip"));
    descripField_ = new JTextField();
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, rowNum++, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(descripField_, gbc);

    //
    // Time bounds box and times.  We cannot change our bounding state unless we are the root instance.
    //
    
    timeBoundsBox_ = new JCheckBox(rMan.getString("simprop.timeBounds"));
    if (!dacx_.currentGenomeIsRootInstance()) {
      timeBoundsBox_.setEnabled(false);
    }    

    UiUtil.gbcSet(gbc, 0, rowNum++, 6, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
    cp.add(timeBoundsBox_, gbc);
    timeBoundsBox_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          boolean enabled = timeBoundsBox_.isSelected();
          if (enabled && !timeAxisHelper_.establishTimeAxis()) {          
            timeBoundsBox_.setSelected(false);  // gonna cause a callback!
            return;
          }
          minField_.setEnabled(enabled);
          minFieldLabel_.setEnabled(enabled);
          maxField_.setEnabled(enabled);
          maxFieldLabel_.setEnabled(enabled);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });

    //
    // Build the minimum and maxiumum times:
    //

    minFieldLabel_ = new JLabel("");
    maxFieldLabel_ = new JLabel(""); 
    
    timeAxisHelper_ = new TimeAxisHelper(uics_, dacx_, this, minFieldLabel_, maxFieldLabel_, uFac_);    
    timeAxisHelper_.fixMinMaxLabels(false);

    minField_ = new JTextField();
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(minFieldLabel_, gbc);

    UiUtil.gbcSet(gbc, 1, rowNum, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(minField_, gbc);
    
    maxField_ = new JTextField();     
    UiUtil.gbcSet(gbc, 3, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(maxFieldLabel_, gbc);
    
    UiUtil.gbcSet(gbc, 4, rowNum++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(maxField_, gbc);    

    //
    // Build the button panel:
    //
 
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {          
          if (applyProperties((GenomeInstance)dacx_.getGenomeSource().getGenome(targetID_))) {
            SingleInstanceModelPropDialog.this.setVisible(false);
            SingleInstanceModelPropDialog.this.dispose();
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          SingleInstanceModelPropDialog.this.setVisible(false);
          SingleInstanceModelPropDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);

    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, rowNum, 6, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(uics_.getTopFrame());
    displayProperties((GenomeInstance)dacx_.getGenomeSource().getGenome(targetID_));
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
  ** Figure out required hour ranges given the parents and children
  ** 
  */
  
  private void calcHourBounds(GenomeInstance targGI) {
    
    GenomeInstance parent = targGI.getVfgParent();
    ArrayList<GenomeInstance> children = new ArrayList<GenomeInstance>();
    ArrayList<DynamicInstanceProxy> kidProxies = new ArrayList<DynamicInstanceProxy>();
    
    // Static children:
    
    Iterator<GenomeInstance> git = dacx_.getGenomeSource().getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance gi = git.next();
      if (gi.getID().equals(targGI.getID())) {
        continue;
      }
      if (targGI.isAncestor(gi)) {
        children.add(gi);
      }
    }
    
    // dynamic children:
    
    Iterator<DynamicInstanceProxy> pit = dacx_.getGenomeSource().getDynamicProxyIterator();
    while (pit.hasNext()) {
      DynamicInstanceProxy dip = pit.next();
      if (dip.instanceIsAncestor(targGI)) {
        kidProxies.add(dip);
      }
    }
    
    //
    // No bigger than parent:
    //
    
    minParentTime_ = Integer.MIN_VALUE;
    maxParentTime_ = Integer.MAX_VALUE;
    lineageIsBounded_ = false;
    if (parent != null) {
      if (parent.hasTimeBounds()) {
        lineageIsBounded_ = true;
        minParentTime_ = parent.getMinTime();
        maxParentTime_ = parent.getMaxTime();
      }
    }
    
    //
    // No smaller than children:
    //

    int size = children.size();
    minChildTime_ = Integer.MAX_VALUE;
    maxChildTime_ = Integer.MIN_VALUE;
    for (int i = 0; i < size; i++) {
      GenomeInstance gi = children.get(i);
      if (gi.hasTimeBounds()) {
        int min = gi.getMinTime();
        if (min < minChildTime_) {
          minChildTime_ = min;
        }
        int max = gi.getMaxTime();
        if (max > maxChildTime_) {
          maxChildTime_ = max;
        }
      }
    }  
    
    size = kidProxies.size();
    for (int i = 0; i < size; i++) {
      DynamicInstanceProxy dip = kidProxies.get(i);
      int min = dip.getMinimumTime();
      if (min < minChildTime_) {
        minChildTime_ = min;
      }
      int max = dip.getMaximumTime();
      if (max > maxChildTime_) {
        maxChildTime_ = max;
      }      
    }
    
    return;
  }
  
  /***************************************************************************
  **
  ** Apply the current model property values to our UI components
  ** 
  */
  
  private void displayProperties(GenomeInstance theGenome) {
    if (nameField_ != null) {
      nameField_.setText(theGenome.getName());
    }
    longNameField_.setText(theGenome.getLongName());
    descripField_.setText(theGenome.getDescription());
    
    boolean hasTimeBounds = theGenome.hasTimeBounds();
    timeBoundsBox_.setSelected(hasTimeBounds);
    minFieldLabel_.setEnabled(hasTimeBounds);
    minField_.setEnabled(hasTimeBounds);
    String minText = (hasTimeBounds) ? timeAxisHelper_.timeValToDisplay(theGenome.getMinTime()) : "";
    minField_.setText(minText);
    maxFieldLabel_.setEnabled(hasTimeBounds);    
    maxField_.setEnabled(hasTimeBounds);
    String maxText = (hasTimeBounds) ? timeAxisHelper_.timeValToDisplay(theGenome.getMaxTime()) : "";
    maxField_.setText(maxText);   
    return;
  }

  /***************************************************************************
  **
  ** Apply our UI values to the model
  ** 
  */
  
  private boolean applyProperties(GenomeInstance targGI) {
    //
    // Only going to be enabled if we are a root instance, so we don't need to
    // check parent bounding condition.
    //
    boolean needBounds = timeBoundsBox_.isSelected();
    if (!needBounds && targGI.haveProxyDecendant()) {
      ResourceManager rMan = dacx_.getRMan();
      JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("simprop.timeBoundsRequiredByProxy"),
                                    rMan.getString("simprop.ErrorTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);            
    }       

    int minResult = TimeAxisDefinition.INVALID_STAGE_NAME;
    int maxResult = TimeAxisDefinition.INVALID_STAGE_NAME;
    if (needBounds) {
      minResult = timeAxisHelper_.timeDisplayToIndex(minField_.getText());
      if (minResult == TimeAxisDefinition.INVALID_STAGE_NAME) {
        return (false);
      }
      maxResult = timeAxisHelper_.timeDisplayToIndex(maxField_.getText());
      if (maxResult == TimeAxisDefinition.INVALID_STAGE_NAME) {
        return (false);
      }
      
      if ((minResult > maxResult) || (minResult < 0)) {
        ResourceManager rMan = dacx_.getRMan(); 
        JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("simprop.badBounds"),
                                      rMan.getString("simprop.ErrorTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return (false);      
      }

      calcHourBounds(targGI);
      
      //
      // We need to be within parent bounds:
      //
      
      if (lineageIsBounded_) {
        if ((minResult < minParentTime_) || (maxResult > maxParentTime_)) {
          ResourceManager rMan = dacx_.getRMan(); 
          JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("simprop.timeBoundsOutsideParent"),
                                        rMan.getString("simprop.ErrorTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          return (false);                                
        }
      }
      
      //
      // We cannot go smaller than child bounds:
      //
      
      if ((minChildTime_ < Integer.MAX_VALUE) || (maxChildTime_ > Integer.MIN_VALUE)) {
        if ((minResult > minChildTime_) || (maxResult < maxChildTime_)) {
          ResourceManager rMan = dacx_.getRMan(); 
          JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("simprop.timeBoundsInsideChild"),
                                        rMan.getString("simprop.ErrorTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          return (false);                                
        }                
      }
    }

    boolean hasTimeBoundsNow = targGI.hasTimeBounds();
    
    //
    // Undo/Redo support
    //       
    
    StaticDataAccessContext rcx = new StaticDataAccessContext(dacx_, targGI);
    UndoSupport support = uFac_.provideUndoSupport("undo.simprop", rcx);  
    String longName = longNameField_.getText().trim();
    
    String oldName = targGI.getName();  
    String newName = (nameField_ != null) ? nameField_.getText().trim() : targGI.getName();  
    
    GenomeChange gc = targGI.setProperties(newName, longName, descripField_.getText().trim(), needBounds, minResult, maxResult);
    GenomeChangeCmd cmd = new GenomeChangeCmd(gc);
    support.addEdit(cmd);
    
    if ((nameField_ != null) && !oldName.equals(newName)) {
      NavTreeChange ntc = nt_.setNodeName(popupNode_, newName);    
      support.addEdit(new NavTreeChangeCmd(rcx, ntc));  
    }
    
    if (!longName.trim().equals("")) {
      new DataLocator(uics_.getGenomePresentation(), rcx).setTitleLocation(support, targGI.getID(), longName);
    }
    
    //
    // If we have an actual time bounds change, we need to change all children to match.
    //
    
    ArrayList<String> eventList = new ArrayList<String>();
    eventList.add(targGI.getID());
    if (needBounds != hasTimeBoundsNow) {
      if (!targGI.isRootInstance()) {
        throw new IllegalStateException();
      }
      String rootID = targGI.getID();      
      Iterator<GenomeInstance> iit = dacx_.getGenomeSource().getInstanceIterator();    
      while (iit.hasNext()) {
        GenomeInstance gi = iit.next();
        GenomeInstance rootParent = gi.getVfgParentRoot();
        if (rootParent != null) {
          if (rootParent.getID().equals(rootID)) {
            if (needBounds) {
              gc = gi.setTimes(minResult, maxResult);              
            } else {
              gc = gi.dropTimes();
            }
            cmd = new GenomeChangeCmd(gc);
            support.addEdit(cmd);
            eventList.add(gi.getID());
          }
        }
      }
    }    

    int numEvent = eventList.size();
    for (int i = 0; i < numEvent; i++) {
      String id = eventList.get(i);
      ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeSource().getID(), id, ModelChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(mcev);    
      mcev = new ModelChangeEvent(dacx_.getGenomeSource().getID(), id, ModelChangeEvent.PROPERTY_CHANGE);
      support.addEvent(mcev);
    }
    
    support.finish();    
    return (true);
  } 
}
