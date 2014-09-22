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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.text.MessageFormat;

import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;

/****************************************************************************
**
** Dialog box for choosing Time Course mapping
*/

public class TimeCourseMappingDialog extends BTStashResultsDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private TimeCourseMappingPanel tcmp_;
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
  
  public TimeCourseMappingDialog(BTState appState, String nodeName, String nodeID) {     
    super(appState, "", new Dimension(700, 500), 1);
    String format = rMan_.getString("tcmd.title");
    String desc = MessageFormat.format(format, new Object[]{nodeName});
    setTitle(desc); 
    
    //
    // Create a list of the target genes available:
    //
    
    TimeCourseData tcd = appState_.getDB().getTimeCourseData();
    List<TimeCourseData.TCMapping> mapped = tcd.getCustomTCMTimeCourseDataKeys(nodeID);
    mapped = (mapped == null) ? new ArrayList<TimeCourseData.TCMapping>() : TimeCourseData.TCMapping.cloneAList(mapped);
    tcmp_ = new TimeCourseMappingPanel(appState, nodeName, mapped, false, false);
    addTable(tcmp_, 6);
    finishConstruction();   
  }
  
  /***************************************************************************
  **
  ** Get the target list
  */
  
  public List<TimeCourseData.TCMapping> getEntryList() {
    // FIXME!  TimeCourseMappingPanel returns a list of Objects.  Need to cast....
    List<TimeCourseData.TCMapping> castRetval = new ArrayList<TimeCourseData.TCMapping>();
    List<Object> retval = tcmp_.getEntryList();
    Iterator<Object> oit = retval.iterator();
    while (oit.hasNext()) {
      TimeCourseData.TCMapping cval = (TimeCourseData.TCMapping)oit.next();
      castRetval.add(cval);
    }
    return (castRetval);
  }
  
    ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Does the processing of the result
  ** 
  */
  
  protected boolean stashForOK() {
    return (tcmp_.stashForOKSupport());
  }
}
