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

package org.systemsbiology.biotapestry.ui.dialogs.pertManage;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.List;
import java.text.MessageFormat;
import java.util.Vector;
import javax.swing.JScrollPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BuildListUtil;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.util.CheckBoxList;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;

/****************************************************************************
 **
 ** Dialog box for setting up perturbation mapping.  Unlike previous QPCR
 ** mapping dialog, we don't need to be able to add entries, since we don't
 ** need to be able to create a target gene entry before editing it!
 */

public class PertMappingPanel extends JPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private CheckBoxList lwnt_;
  private CheckBoxList lw_;
  private PerturbationData pd_;
  private BuildListUtil.BuildListResult targResult_; 
  private BuildListUtil.BuildListResult srcResult_;
  private DialogSupport ds_;
  private BuildListUtil tblu_;
  private BuildListUtil sblu_;
  private ArrayList allTabs_;
  private BTState appState_;
  
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
  
  public PertMappingPanel(BTState appState, String nodeName, List currEntries, List currSources, boolean msgSeparate, boolean forceDrop) {
    appState_ = appState;
    ResourceManager rMan = appState_.getRMan();    
    GridBagConstraints gbc = new GridBagConstraints();
    setLayout(new GridBagLayout());
    ds_ = new DialogSupport(appState_, gbc);
    int rowNum = 0;
    int columns = 6;
        
    pd_ = appState.getDB().getPertData();
    
    //
    // Create a list of the target genes available:
    //

    Vector targCand = pd_.getTargetOptions(false);    
    tblu_ = new BuildListUtil(appState_, appState_.getTopFrame(), nodeName, (currEntries == null) ?  new ArrayList() : new ArrayList(currEntries), targCand, forceDrop);
    targResult_ = tblu_.getBuildListResult();
        
    //
    // Build and install the UI components
    //

    String msg = rMan.getString("pertMapping.targets");
    msg = MessageFormat.format(msg, new Object[]{nodeName});
    JLabel lab = new JLabel(msg);
    rowNum = ds_.addWidgetFullRow(this, lab, true, true, rowNum, columns);
 
    lw_ = tblu_.getBuiltList();
    JScrollPane jsp = new JScrollPane(lw_);
    rowNum = ds_.addTable(this, jsp, 5, rowNum, columns);
    if (targResult_.firstSel != -1) {
      lw_.ensureIndexIsVisible(targResult_.firstSel);
    }
    
    //
    // Create a list of the source genes available:
    //

    Vector<TrueObjChoiceContent> srcCand = pd_.getSourceNameOptions();
    sblu_ = new BuildListUtil(appState_, appState_.getTopFrame(), nodeName, (currSources == null) ?  new ArrayList() : new ArrayList(currSources), srcCand, false);
    srcResult_ = sblu_.getBuildListResult();

   
    //
    // Add the UI components:
    //

    msg = rMan.getString("pertMapping.sources");
    msg = MessageFormat.format(msg, new Object[]{nodeName});
    lab = new JLabel(msg);
    rowNum = ds_.addWidgetFullRow(this, lab, true, true, rowNum, columns);
 
    lwnt_ = sblu_.getBuiltList();
    jsp = new JScrollPane(lwnt_);
    rowNum = ds_.addTable(this, jsp, 5, rowNum, columns);
       
    if (srcResult_.firstSel != -1) {
      lwnt_.ensureIndexIsVisible(srcResult_.firstSel);
    }
    
    allTabs_ = new ArrayList();
    allTabs_.add(targResult_);
    allTabs_.add(srcResult_);
    
    if (!msgSeparate) {
      JPanel messagePanel = BuildListUtil.buildMessagePanel(appState_, allTabs_);
      rowNum = ds_.addTable(this, messagePanel, 5, rowNum, columns);
    }
  }
  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get all the tables
  */
  
  public List getAllTabs() {
    return (allTabs_);
  }
  
  /***************************************************************************
  **
  ** Get the target list
  */
  
  public List getEntryList() {
    return (tblu_.getFinishedEntryList());
  }

  /***************************************************************************
  **
  ** Get the source list
  */
  
  public List getSourceList() {
    return (sblu_.getFinishedEntryList());
  }
 
  /***************************************************************************
  **
  ** Does the processing of the result
  ** 
  */
  
  public boolean stashForOKSupport() {
    ResourceManager rMan = appState_.getRMan();
    tblu_.stashForOKSupport(rMan.getString("pertMapping.forTargets"));
    sblu_.stashForOKSupport(rMan.getString("pertMapping.forSources"));
    return (true);
  }
}
