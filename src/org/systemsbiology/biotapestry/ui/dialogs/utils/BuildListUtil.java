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

package org.systemsbiology.biotapestry.ui.dialogs.utils;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.URL;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.ListModel;
import javax.swing.border.EtchedBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.CheckBoxList;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
 **
 ** This is a utility for build lists (used for map creation)
 */

public class BuildListUtil {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private CheckBoxList lw_;
  private List newEntries_;
  private ArrayList<Object> entryList_;
  private BuildListResult targResult_;
  private boolean forceDrops_;
  private JFrame parent_; 
  private UIComponentSource uics_;
  private DataAccessContext dacx_;
  private static final ImageIcon greenIcon_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // STATIC INIT
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  static {
    URL ugif = BuildListUtil.class.getResource("/org/systemsbiology/biotapestry/images/GreenRect-16-64.gif");  
    greenIcon_ = new ImageIcon(ugif);   
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Constructor 
  */
  
  public BuildListUtil(UIComponentSource uics, DataAccessContext dacx, JFrame parent, String nodeName, List clonedCurrEntries, Vector targCand, boolean forceDrops) { 
    uics_ = uics;
    dacx_ = dacx;
    parent_ = parent; 
    if (clonedCurrEntries == null) {
      newEntries_ = new ArrayList();
    } else {
      newEntries_ = clonedCurrEntries;
    }
    forceDrops_ = forceDrops;
    targResult_ = buildCheckBoxList(newEntries_, targCand, nodeName);    
    lw_ = new CheckBoxList(targResult_.listElements, uics_.getHandlerAndManagerSource());
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get the build list
  */
  
  public CheckBoxList getBuiltList() {
    return (lw_);
  }
  
  /***************************************************************************
  **
  ** Get the build list
  */
  
  public BuildListResult getBuildListResult() {
    return (targResult_);
  }

  /***************************************************************************
  **
  ** Get the target list
  */
  
  public List getFinishedEntryList() {
    return (entryList_);
  }
  
  /***************************************************************************
  **
  ** Build the message panel
  ** 
  */

  public static JPanel buildMessagePanel(DataAccessContext dacx, List<BuildListResult> buildListResults) {
    
    ResourceManager rMan = dacx.getRMan();    
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel messagePanel = new JPanel();
    messagePanel.setBorder(new EtchedBorder());
    messagePanel.setLayout(new GridBagLayout());

    boolean defaultIsPresent = false;
    int numres = buildListResults.size();
    for (int i = 0; i < numres; i++) {
      BuildListResult blr = buildListResults.get(i);
      if (!blr.defaultEntries.isEmpty()) {
        defaultIsPresent = true;
        break;
      }  
    }
    int rowCount = 0;
    if (defaultIsPresent) { 
      String msg = UiUtil.convertMessageToHtml(rMan.getString("pertMapping.greenIsCool"));
      JLabel lab = new JLabel(msg, greenIcon_, JLabel.LEFT);
      UiUtil.gbcSet(gbc, 0, rowCount++, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
      messagePanel.add(lab, gbc); 
    } 
    return (messagePanel);
  }
    
  /***************************************************************************
  **
  ** Does the processing of the result
  ** 
  */
  
  public boolean stashForOKSupport(String whichList) {
    entryList_ = new ArrayList<Object>();
    ListModel myModel = lw_.getModel();
    processAList(myModel, entryList_, targResult_, whichList);
    return (true);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Build the guts of a check box list:
  ** 
  */

  private BuildListResult buildCheckBoxList(List entries, Vector targCand, String nodeName) {
   
    
    // entries: trg KEYs that we currently map to
    // targCand : TrueObjChoiceContent(targetName, targetKey);
    // nodeName: unnormalized node name
    
    
    BuildListResult retval = new BuildListResult();
    retval.danglingEntries = new HashSet(entries);
    retval.defaultEntries = new HashSet();
    retval.normName = DataUtil.normKey(nodeName);
  
    int count = 0;
    retval.firstSel = -1;
    int firstDefault = -1;
    ArrayList choices = new ArrayList();
    Iterator tcit = targCand.iterator();
    while (tcit.hasNext()) {
      TrueObjChoiceContent tocc = (TrueObjChoiceContent)tcit.next();
      String normTarg = DataUtil.normKey(tocc.name);
      boolean isDefault = retval.normName.equals(normTarg);
      if (isDefault) {
        retval.defaultEntries.add(tocc.val);
        if (firstDefault == -1) {
          firstDefault = count;
        }
      }
      String greekToLower = DataUtil.greekToLowerCase(tocc.name);
      boolean selected = entries.contains(tocc.val);
      Color useColor = (isDefault) ? Color.green : Color.white;
      CheckBoxList.ListChoice lc = new CheckBoxList.ListChoice(tocc.val, greekToLower, useColor, selected, false, false);
      choices.add(lc);
      if (selected) {
        retval.danglingEntries.remove(tocc.val);
        if (retval.firstSel == -1) {
          retval.firstSel = count;
        }
      }
      count++;            
    }
 
    //
    // Default to default, if needed:
    //
    
    if (retval.firstSel == -1) {
      retval.firstSel = firstDefault;
    }
      
    //
    // Show in context, if possible:
    //
    
    if (retval.firstSel == 1) {
      retval.firstSel = 0;
    } else if (retval.firstSel >= 2) {
      retval.firstSel -= 2;
    }

    retval.listElements = new Vector(choices);

    return (retval);
  }
  

  /***************************************************************************
  **
  ** Process a list into a result
  ** 
  */
  
  private void processAList(ListModel myModel, List<Object> results, BuildListResult blr, String whichList) {
    int numElem = myModel.getSize();
    //
    // Previous incarnation made it possible to have multiple map entries that
    // resolved to the same thing.  This should no longer be possible, and
    // will not be dealt with.
    //
    // Also, it should not be possible to have dangling map entries (maps to
    // non-existent targets).  These will be dropped silently if they exist
    // (will we crash instead trying to display it??)
    //
    // If the map is empty, the default key(s?) is provided for free.  If it
    // is checked, we can suggest they uncheck it if it the only thing on the
    // list.  If it is unchecked, and there are other entries, we can suggest
    // they check it!
    //
  
    HashSet<Object> retval = new HashSet<Object>();
    boolean defaultPresent = !blr.defaultEntries.isEmpty();
    HashSet<Object> defaultSelected = new HashSet<Object>();
    boolean nonDefaultSelected = false;
    for (int i = 0; i < numElem; i++) {
      CheckBoxList.ListChoice choice = (CheckBoxList.ListChoice)myModel.getElementAt(i);
      if (choice.isSelected) {
        Object choiceObj = choice.getObject();
        retval.add(choiceObj);
        if (blr.defaultEntries.contains(choiceObj)) {
          defaultSelected.add(choiceObj);
        } else {
          nonDefaultSelected = true;
        }
      }
    }
    ResourceManager rMan = dacx_.getRMan();
    //
    // FIX ME??  Consider modifying this to give clearer instructions when
    // a maternal or zygotic channel has been selected instead of the default!
    // (only applies to the time course case, not the perturbation mapping usage)
    //
    
    if (defaultPresent && !defaultSelected.isEmpty() && !nonDefaultSelected) {
      if (forceDrops_) {
        String format = rMan.getString("pertMapping.dontNeedDeleteCheckedWillDrop");
        String desc = MessageFormat.format(format, new Object[]{whichList});          
        JOptionPane.showMessageDialog(parent_, desc,
                                      rMan.getString("pertMapping.dontNeedDeleteCheckedTitle"),
                                      JOptionPane.WARNING_MESSAGE);
 
        retval.removeAll(defaultSelected);       
      } else {
        String format = rMan.getString("pertMapping.dontNeedDeleteCheckedAskToDrop");
        String desc = MessageFormat.format(format, new Object[]{whichList});      
        int ok = 
          JOptionPane.showConfirmDialog(parent_, desc,
                                        rMan.getString("pertMapping.dontNeedDeleteCheckedTitle"),
                                        JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
          retval.removeAll(defaultSelected);
        }
      }
    } else if (defaultPresent && !defaultSelected.equals(blr.defaultEntries) && nonDefaultSelected) {
      String format = rMan.getString("pertMapping.shouldHaveDeleteCheckedAskToAdd");
      String desc = MessageFormat.format(format, new Object[]{whichList});      
      int ok = 
        JOptionPane.showConfirmDialog(parent_, desc,
                                      rMan.getString("pertMapping.shouldHaveDeleteCheckedTitle"),
                                      JOptionPane.YES_NO_OPTION);
      if (ok == JOptionPane.YES_OPTION) {
        retval.addAll(blr.defaultEntries);
      } 
    }
    results.addAll(retval);
    return;
  }
  
  /***************************************************************************
  **
  ** Returns list properties
  */  
  
  public static class BuildListResult {
    public boolean haveDanglingKeys;
    public Vector listElements;
    public int firstSel;
    public HashSet<Object> defaultEntries;
    public HashSet danglingEntries;
    public String normName;
  }
}
