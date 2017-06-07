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

package org.systemsbiology.biotapestry.db;

import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.CopiesPerEmbryoData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;

/****************************************************************************
**
** Source for experimental data
*/

public interface ExperimentalDataSource  {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  
  /***************************************************************************
  **
  ** Answers if we have data attached to our node by default (simple name match)
  ** 
  */
  
  public boolean hasDataAttachedByDefault(String nodeID);
 
  
  /***************************************************************************
  **
  ** Answers if we have region data attached to our region by default (simple name match)
  ** 
  */
  
  public boolean hasRegionAttachedByDefault(Group group);
 
  /***************************************************************************
  **
  ** Answers if other top-level genome instances have regions named the same
  ** as this one which are attached by default.
  ** 
  */
  
  public boolean hasOtherRegionsAttachedByDefault(GenomeInstance gi, Group group);
  

  /***************************************************************************
  ** 
  ** Get the perturbation data
  */

  public PerturbationData getPertData();
 
  /***************************************************************************
  ** 
  ** Set the perturbation data
  */

  public DatabaseChange setPertData(PerturbationData pd);
  
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startTimeCourseUndoTransaction();

  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishTimeCourseUndoTransaction(DatabaseChange change);
 
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startCopiesPerEmbryoUndoTransaction();

  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishCopiesPerEmbryoUndoTransaction(DatabaseChange change);
 
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
//  public DatabaseChange startTemporalInputUndoTransaction();
 
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
 // public DatabaseChange finishTemporalInputUndoTransaction(DatabaseChange change);
  
  /***************************************************************************
  **
  ** roll back an undo transaction
  */
  
  public void rollbackDataUndoTransaction(DatabaseChange change);
  
  /***************************************************************************
  ** 
  ** Get the Time course data
  */

  public TimeCourseData getTimeCourseData();
 
  /***************************************************************************
  ** 
  ** Set the Time Course data
  */

  public DatabaseChange setTimeCourseData(TimeCourseData timeCourse);

  /***************************************************************************
  ** 
  ** Get the copies per embryo data
  */

  public CopiesPerEmbryoData getCopiesPerEmbryoData();
  
  /***************************************************************************
  ** 
  ** Set the copies per embryo data
  */

  public DatabaseChange setCopiesPerEmbryoData(CopiesPerEmbryoData copies);
 
  /***************************************************************************
  ** 
  ** Get the Temporal Range Data
  */

//  public TemporalInputRangeData getTemporalInputRangeData();
 
  /***************************************************************************
  ** 
  ** Set the Temporal Range Data
  */

 // public void setTemporalInputRangeData(TemporalInputRangeData rangeData);
 
  /***************************************************************************
  ** 
  ** Get the time axis definition
  */

  public TimeAxisDefinition getTimeAxisDefinition();

  /***************************************************************************
  ** 
  ** Set the time axis definition
  */

  public DatabaseChange setTimeAxisDefinition(TimeAxisDefinition timeAxis);
 
  /***************************************************************************
  ** 
  ** Set the time axis definition
  */

  public void installLegacyTimeAxisDefinition(DataAccessContext dacx);
  
  /***************************************************************************
  ** 
  ** Set the time axis definition
  */

  public boolean amUsingSharedExperimentalData();

}
