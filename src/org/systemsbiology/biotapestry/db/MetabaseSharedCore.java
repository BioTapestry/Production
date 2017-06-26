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

package org.systemsbiology.biotapestry.db;

import java.io.PrintWriter;

import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.CopiesPerEmbryoData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.Indenter;


/****************************************************************************
**
** Shared data between databases
*/

public class MetabaseSharedCore {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private PerturbationData pertData_;
  private TimeCourseData sharedTimeCourse_;
  private CopiesPerEmbryoData copiesPerEmb_;
  private TimeAxisDefinition timeAxis_;
  private Metabase.DataSharingPolicy dsp_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** constructor
  */

  public MetabaseSharedCore() {
    dsp_ = new Metabase.DataSharingPolicy(false, false, false, false);
    timeAxis_ = null;
    pertData_ = null;
    sharedTimeCourse_ = null;
    copiesPerEmb_ = null;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  ** 
  ** Start a new model
  */

  public void newModelViaDACX() {
    dropViaDACX();
    return;
  }  

  /***************************************************************************
  ** 
  ** Drop everything
  */

  public void dropViaDACX() {
    //
    // Default behavior with one tab is that this data is not shared, but local to the database:
    //
    dsp_ = new Metabase.DataSharingPolicy(false, false, false, false);
    pertData_ = null;
    sharedTimeCourse_ = null;
    copiesPerEmb_ = null;
    timeAxis_ = null;
    return;
  }
  
  /***************************************************************************
  ** 
  ** Drop all shared perturbation data
  */

  public DatabaseChange dropAllSharedData() {
    DatabaseChange dc = new DatabaseChange();
    
    if (timeAxis_ != null) {
      dc.sharedTimeAxisChange = true;
      dc.oldTimeAxis = timeAxis_.clone();
      timeAxis_ = null;
      dc.newTimeAxis = null;
    }
    
    if (sharedTimeCourse_ != null) {
      dc.sharedTcdChange = true;
      dc.oldTcd = sharedTimeCourse_.clone();
      sharedTimeCourse_ = null;
      dc.newTcd = null;
    }
     
    if (pertData_ != null) {
      dc.sharedPertDataChange = true;
      dc.oldSharedPertData = pertData_.clone();
      pertData_ = null;
      dc.newSharedPertData = null;
    }
    
    if (copiesPerEmb_ != null) {
      dc.sharedCPEChange = true;
      dc.oldCpe = copiesPerEmb_.clone();
      copiesPerEmb_ = null;
      dc.newCpe = null;
    }
    
    Metabase.DataSharingPolicy dsp = new Metabase.DataSharingPolicy(false, false, false, false);
    dc.dataSharingPolicyChange = true;
    dc.oldDsp = dsp_.clone();
    dsp_ = dsp;
    dc.newDsp = dsp_.clone();
  
    return (dc);
  }

  /***************************************************************************
  ** 
  ** Get the perturbation data
  */

  public PerturbationData getSharedPertData() {
    if (!dsp_.sharePerts) {
      throw new IllegalStateException();
    }
    return (pertData_);
  }  
  
  /***************************************************************************
  ** 
  ** Set the perturbation data
  */

  public DatabaseChange setSharedPertData(PerturbationData pertData) {
    DatabaseChange dc = new DatabaseChange();
    dc.sharedPertDataChange = true;
    dc.dataSharingPolicyChange = true;
    dc.oldDsp = dsp_.clone();
    dsp_.sharePerts = (pertData != null);
    dc.newDsp = dsp_.clone();
    dc.oldSharedPertData = (pertData_ == null) ? null : pertData_.clone();
    pertData_ = pertData;
    dc.newSharedPertData = (pertData_ == null) ? null : pertData_.clone();
    return (dc);
  }
 
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startSharedTimeCourseUndoTransaction() {
    DatabaseChange dc = new DatabaseChange();
    dc.oldTcd = (sharedTimeCourse_ == null) ? null : sharedTimeCourse_.clone();
    dc.sharedTcdChange = true;
    return (dc);
  }  
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishSharedTimeCourseUndoTransaction(DatabaseChange change) {
    change.newTcd = (sharedTimeCourse_ == null) ? null : sharedTimeCourse_.clone();
    if (!change.sharedTcdChange) {
      throw new IllegalStateException();
    }
    return (change);
  }
  
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startSharedCopiesPerEmbryoUndoTransaction() {
    DatabaseChange dc = new DatabaseChange();
    dc.oldCpe = (copiesPerEmb_ == null) ? null : copiesPerEmb_.clone();
    dc.sharedCPEChange = true;
    return (dc);
  }  
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishSharedCopiesPerEmbryoUndoTransaction(DatabaseChange change) {
    change.newCpe = (copiesPerEmb_ == null) ? null : copiesPerEmb_.clone();
    if (!change.sharedCPEChange) {
      throw new IllegalStateException();
    }
    return (change);
  }
  
  /***************************************************************************
  ** 
  ** Get the data sharing policy
  */

  public Metabase.DataSharingPolicy getDataSharingPolicy() {
    return (dsp_);
  } 
  
  /***************************************************************************
  ** 
  ** Set the data sharing policy: For I/O (no undo support)
  */

  public void installDataSharing(Metabase.DataSharingPolicy dsp) {
    dsp_ = dsp;
    return;
  } 

  /***************************************************************************
  ** 
  ** Is data sharing in effect?
  */

  public boolean amSharingExperimentalData() {
    return (dsp_.isSpecifyingSharing());
  } 
  
  /***************************************************************************
  ** 
  ** Get the Time course data
  */

  public TimeCourseData getSharedTimeCourseData() {
    if (!dsp_.shareTimeCourses) {
      throw new IllegalStateException();
    }
    return (sharedTimeCourse_);
  }  
  
  /***************************************************************************
  ** 
  ** Set the Time Course data
  */

  public DatabaseChange setSharedTimeCourseData(TimeCourseData timeCourse) {
    DatabaseChange dc = new DatabaseChange();
    dc.sharedTcdChange = true;
    dc.dataSharingPolicyChange = true;
    dc.oldDsp = dsp_.clone();
    dsp_.shareTimeCourses = (timeCourse != null);
    dc.newDsp = dsp_.clone();
    dc.oldTcd = (sharedTimeCourse_ == null) ? null : sharedTimeCourse_.clone();
    sharedTimeCourse_ = timeCourse;
    dc.newTcd = (sharedTimeCourse_ == null) ? null : sharedTimeCourse_.clone();
    return (dc);
  }  
  
  /***************************************************************************
  ** 
  ** Get the shared copies data
  */

  public CopiesPerEmbryoData getSharedCopiesPerEmbryoData() {
    if (!dsp_.sharePerEmbryoCounts) {
      throw new IllegalStateException();
    }
    return (copiesPerEmb_);
  }  
  
  /***************************************************************************
  ** 
  ** Set the copies per embryo data
  */

  public DatabaseChange setSharedCopiesPerEmbryoData(CopiesPerEmbryoData copies) {
    DatabaseChange dc = new DatabaseChange();
    dc.sharedCPEChange = true;
    dc.dataSharingPolicyChange = true;
    dc.oldDsp = dsp_.clone();
    dsp_.sharePerEmbryoCounts = (copies != null);
    dc.newDsp = dsp_.clone();
    
    dc.oldCpe = (copiesPerEmb_ == null) ? null : copiesPerEmb_.clone();
    copiesPerEmb_ = copies;
    dc.newCpe = (copiesPerEmb_ == null) ? null : copiesPerEmb_.clone();
    return (dc);
  }    
    
  /***************************************************************************
  ** 
  ** Get the time axis definition
  */

  public TimeAxisDefinition getSharedTimeAxisDefinition() {
    if (!dsp_.shareTimeUnits) {
      throw new IllegalStateException();
    }
    return (timeAxis_);
  }  
  
  /***************************************************************************
  ** 
  ** Set the time axis definition
  */

  public DatabaseChange setSharedTimeAxisDefinition(TimeAxisDefinition timeAxis) {
    DatabaseChange dc = new DatabaseChange();
    dc.sharedTimeAxisChange = true;
    dc.dataSharingPolicyChange = true;
    dc.oldDsp = dsp_.clone();
    dsp_.shareTimeUnits = (timeAxis != null);
    dc.newDsp = dsp_.clone();

    dc.oldTimeAxis = (timeAxis_ == null) ? null : timeAxis_.clone();
    timeAxis_ = timeAxis;
    dc.newTimeAxis = (timeAxis_ == null) ? null : timeAxis_.clone();
    return (dc);
  }
  
  /***************************************************************************
  **
  ** Dump the shared data to the given file using XML
  */
  
  public void writeTimeAxisXML(PrintWriter out, Indenter ind) {    
    if ((timeAxis_ != null) && timeAxis_.isInitialized()) {
      timeAxis_.writeXML(out, ind.up());
      ind.down();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Dump the shared data to the given file using XML
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {    
    dsp_.writeXML(out, ind.up());
    ind.down();
    
    if (sharedTimeCourse_ != null) {
      sharedTimeCourse_.writeXML(out, ind.up());
      ind.down();
    }
    
    if (pertData_ != null) {
      pertData_.writeXML(out, ind.up());
      ind.down();
    }

    if ((copiesPerEmb_ != null) && copiesPerEmb_.haveData()) {
      copiesPerEmb_.writeXML(out, ind.up());
      ind.down();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void metabaseSharedChangeUndo(DatabaseChange undo) {
    
    if (undo.dataSharingPolicyChange) {
      dsp_ = undo.oldDsp;     
    }
    
    if (undo.sharedTimeAxisChange) {
      timeAxis_ = undo.oldTimeAxis;
    }
    if (undo.sharedTcdChange) {
      sharedTimeCourse_ = undo.oldTcd;
    }
    if (undo.sharedPertDataChange) {
      pertData_ = undo.oldSharedPertData;
    }
    if (undo.sharedCPEChange) {
      copiesPerEmb_ = undo.oldCpe;
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void metabaseSharedChangeRedo(DatabaseChange redo) {
 
    if (redo.dataSharingPolicyChange) {
      dsp_ = redo.newDsp;     
    }  
    if (redo.sharedTimeAxisChange) {
      timeAxis_ = redo.newTimeAxis;
    }
    if (redo.sharedTcdChange) {
      sharedTimeCourse_ = redo.newTcd;
    }
    if (redo.sharedPertDataChange) {
      pertData_ = redo.newSharedPertData;
    }
    if (redo.sharedCPEChange) {
      copiesPerEmb_ = redo.newCpe;
    }
    return;
  }
}
