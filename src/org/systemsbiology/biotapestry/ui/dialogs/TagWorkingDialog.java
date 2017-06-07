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

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.Box;
import javax.swing.border.EmptyBorder;
import java.util.Vector;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.FixedJButton;

/****************************************************************************
**
** Dialog box for creating tags 
*/

public class TagWorkingDialog extends JDialog {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JComboBox tagCombo_;
  private boolean haveResult_;
  private String tagResult_;
  private Set<String> usedTags_;
  private HashSet<String> forbiddenTags_;
  private UIComponentSource uics_;
  private StaticDataAccessContext dacx_;
  
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
  
  public TagWorkingDialog(UIComponentSource uics, StaticDataAccessContext dacx, String defaultTag, Set<String> allTags, Set<String> usedTags) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("tagWorking.title"), true);
    dacx_ = dacx;
    uics_ = uics;
    usedTags_ = usedTags;
    haveResult_ = false;
    ResourceManager rMan = dacx_.getRMan();    
    setSize(400, 200);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
   
    //
    // Build the choice combo with all existing tag values
    //
    
    forbiddenTags_ = buildForbiddenList(allTags, usedTags, defaultTag);
    JLabel label = new JLabel(rMan.getString("tagWorking.enterTag"));
    Vector<String> choices = buildTagsList(allTags, forbiddenTags_, defaultTag);
    tagCombo_ = new JComboBox(choices);
    if (defaultTag != null) {
      int numCh = choices.size();
      for (int i = 0; i < numCh; i++) {
        String choice = choices.get(i);
        if (choice.equals(defaultTag)) {
          tagCombo_.setSelectedIndex(i);
          break;
        }
      }
    }
    tagCombo_.setEditable(true);
    
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);
    
    UiUtil.gbcSet(gbc, 1, 0, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(tagCombo_, gbc);
    
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            TagWorkingDialog.this.setVisible(false);
            TagWorkingDialog.this.dispose();
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.cancel"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          stashResults(false);        
          TagWorkingDialog.this.setVisible(false);
          TagWorkingDialog.this.dispose();
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
    UiUtil.gbcSet(gbc, 0, 1, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(uics_.getTopFrame());
  }
  
  /***************************************************************************
  **
  ** Get the tag result.
  ** 
  */
  
  public String getTag() {
    return (tagResult_);
  }
  
  /***************************************************************************
  **
  ** Answer if we have a result
  ** 
  */
  
  public boolean haveResult() {
    return (haveResult_);
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
  ** Build set of tags that we _cannot_ use:
  */
   
  private HashSet<String> buildForbiddenList(Set<String> allTags, Set<String> usedTags, String currentTag) {
    HashSet<String> retval = new HashSet<String>();
    //
    // Forbidden list consists of all used tags _except_ the current one,
    // if it exists.
    //
    
    if (currentTag == null) {
      retval.addAll(usedTags);
      return (retval);
    }
    
    Iterator<String> utit = usedTags.iterator();
    while (utit.hasNext()) {
      String tag = utit.next();
      if (!DataUtil.keysEqual(tag, currentTag)) {
        retval.add(tag);
      }
    }
    return (retval);
  }  
    
  
  /***************************************************************************
  **
  ** Build list of tags we can use.  
  */
   
  private Vector<String> buildTagsList(Set<String> allTags, Set<String> forbiddenTags, String currentTag) {
    Vector<String> retval = new Vector<String>();
    TreeSet<String> sortedTags = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    sortedTags.addAll(allTags);
    if (currentTag == null) {
      retval.add("");
    } else {
      sortedTags.add(currentTag);
    }
    Iterator<String> sit = sortedTags.iterator();
    while (sit.hasNext()) {
      String tag = sit.next();
      if (!DataUtil.containsKey(forbiddenTags, tag)) {
        retval.add(tag);
      }
    }
    return (retval);      
  }  
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      String tagSelection = ((String)tagCombo_.getSelectedItem()).trim();
      
      // No blank tags:
      if (tagSelection.equals("")) {
        ResourceManager rMan = dacx_.getRMan();
        JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                      rMan.getString("tagWorking.emptyTag"), 
                                      rMan.getString("tagWorking.emptyTagTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }     
      
      // No tags from forbidden set:
      if (DataUtil.containsKey(forbiddenTags_, tagSelection)) {
        ResourceManager rMan = dacx_.getRMan();
        JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                      rMan.getString("tagWorking.dupTag"), 
                                      rMan.getString("tagWorking.dupTagTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }     
      
      
      tagResult_ = tagSelection;
      haveResult_ = true;
    } else {
      tagResult_ = null;
      haveResult_ = false;
    }
    return (true);
  }    
}
