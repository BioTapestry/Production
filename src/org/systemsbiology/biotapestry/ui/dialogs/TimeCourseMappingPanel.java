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
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.text.MessageFormat;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.JScrollPane;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.timeCourse.ExpressionEntry;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseGene;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BuildListUtil;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.util.CheckBoxList;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;

/****************************************************************************
 **
 ** Dialog box for setting up expression mapping.  Unlike previous QPCR
 ** mapping dialog, we don't need to be able to add entries, since we don't
 ** need to be able to create a target gene entry before editing it!
 */

public class TimeCourseMappingPanel extends JPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private CheckBoxList lw_;
  private BuildListUtil.BuildListResult targResult_; 
  private DialogSupport ds_;
  private BuildListUtil tblu_;
  private ArrayList<BuildListUtil.BuildListResult> allTabs_;
  private UIComponentSource uics_;
//  private DataAccessContext dacx_;
  
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
  
  public TimeCourseMappingPanel(UIComponentSource uics, TimeCourseData tcd, String nodeName, List<TimeCourseDataMaps.TCMapping> currEntries, boolean msgSeparate, boolean forceDrops) {
    uics_ = uics;
    ResourceManager rMan = uics_.getRMan();    
    GridBagConstraints gbc = new GridBagConstraints();
    setLayout(new GridBagLayout());
    ds_ = new DialogSupport(uics_, gbc);
    int rowNum = 0;
    int columns = 6;
     
    //
    // Create a list of the target genes available:
    //
    Iterator<TimeCourseGene> genes = tcd.getGenes();
    TreeSet<TrueObjChoiceContent> toccMapped = new TreeSet<TrueObjChoiceContent>();
    while (genes.hasNext()) {
      TimeCourseGene node = genes.next();
      //
      // Always provide non-channel option:
      //
      TrueObjChoiceContent tocc = new TrueObjChoiceContent(node.getName(), new TimeCourseDataMaps.TCMapping(node.getName()));
      toccMapped.add(tocc);
      
      //
      // If it has either, provide option for both, since we want a gene to be able to ignore maternal data:
      //
      Set<ExpressionEntry.Source> sourceOptions = node.getSourceOptions();
      if (ExpressionEntry.hasMaternalChannel(sourceOptions) || ExpressionEntry.hasZygoticChannel(sourceOptions)) {
        String matName = ExpressionEntry.buildMaternalDisplayName(uics_, node.getName());
        tocc = new TrueObjChoiceContent(matName, 
                                        new TimeCourseDataMaps.TCMapping(node.getName(), ExpressionEntry.Source.MATERNAL_SOURCE));
        toccMapped.add(tocc);
        String zygName = ExpressionEntry.buildZygoticDisplayName(uics_, node.getName());
        tocc = new TrueObjChoiceContent(zygName, new TimeCourseDataMaps.TCMapping(node.getName(), ExpressionEntry.Source.ZYGOTIC_SOURCE));
        toccMapped.add(tocc);
      } 
    }
    List<TimeCourseDataMaps.TCMapping> clonedCurr = TimeCourseDataMaps.TCMapping.cloneAList(currEntries);
    tblu_ = new BuildListUtil(uics_, uics_.getTopFrame(), nodeName, clonedCurr, new Vector<TrueObjChoiceContent>(toccMapped), forceDrops);
    targResult_ = tblu_.getBuildListResult();
        
    //
    // Build and install the UI components
    //

    String msg = rMan.getString("tcmd.sources");
    msg = MessageFormat.format(msg, new Object[]{nodeName});
    JLabel lab = new JLabel(msg);
    rowNum = ds_.addWidgetFullRow(this, lab, true, true, rowNum, columns);
 
    lw_ = tblu_.getBuiltList();
    JScrollPane jsp = new JScrollPane(lw_);
    rowNum = ds_.addTable(this, jsp, 5, rowNum, columns);
    if (targResult_.firstSel != -1) {
      lw_.ensureIndexIsVisible(targResult_.firstSel);
    }
      
    allTabs_ = new ArrayList<BuildListUtil.BuildListResult>();
    allTabs_.add(targResult_);
    
    if (!msgSeparate) {
      JPanel messagePanel = BuildListUtil.buildMessagePanel(uics_, allTabs_);
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
  
  public List<Object> getEntryList() {
    return (tblu_.getFinishedEntryList());
  }

  /***************************************************************************
  **
  ** Does the processing of the result
  ** 
  */
  
  public boolean stashForOKSupport() {
    tblu_.stashForOKSupport(uics_.getRMan().getString("tcdExprMap.forTargets"));
    return (true);
  }
}
