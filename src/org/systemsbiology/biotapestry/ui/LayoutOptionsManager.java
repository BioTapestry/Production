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

package org.systemsbiology.biotapestry.ui;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.layouts.HaloLayout;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParams;
import org.systemsbiology.biotapestry.ui.layouts.StackedBlockLayout;
import org.systemsbiology.biotapestry.ui.layouts.WorksheetLayout;

/****************************************************************************
**
** Layout Options Manager.
*/

public class LayoutOptionsManager {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private DataAccessContext dacx_;
  private LayoutOptions options_;
  private WorksheetLayout.WorksheetLayoutParams workSheetParams_;
  private WorksheetLayout.WorksheetLayoutParams workSheetParamsDiag_;
  private HaloLayout.HaloLayoutParams haloParams_;
  private StackedBlockLayout.StackedBlockLayoutParams stackedBlockParams_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** constructor
  */
    
  public LayoutOptionsManager(DataAccessContext dacx) {
    dacx_ = dacx;
    options_ = new LayoutOptions();
    workSheetParams_ = WorksheetLayout.getDefaultParams(false, false);
    workSheetParamsDiag_ = WorksheetLayout.getDefaultParams(false, true);
    haloParams_ = HaloLayout.getDefaultParams();
    stackedBlockParams_ = StackedBlockLayout.getDefaultParams(false);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get the incremental and link layout options
  */

  public LayoutOptions getLayoutOptions() {
    return (options_);
  }
    
  /***************************************************************************
  **
  ** Get options for type
  */   
  
  public SpecialtyLayoutEngineParams getLayoutParams(SpecialtyLayoutEngine.SpecialtyType st) {
    switch (st) {
      case STACKED:
        return (stackedBlockParams_);
      case DIAGONAL:
        WorksheetLayout.forceParamsAsNeeded(dacx_, workSheetParamsDiag_);
        return (workSheetParamsDiag_);
      case ALTERNATING:
        WorksheetLayout.forceParamsAsNeeded(dacx_, workSheetParams_);
        return (workSheetParams_);
      case HALO:
        return (haloParams_);
      default:
        throw new IllegalArgumentException();
    }
  } 
  
  /***************************************************************************
  ** 
  ** Get worksheet layout options
  */

  public WorksheetLayout.WorksheetLayoutParams getWorksheetLayoutParams() {
    WorksheetLayout.forceParamsAsNeeded(dacx_, workSheetParams_);
    return (workSheetParams_);
  }  
  
  /***************************************************************************
  ** 
  ** Get diagonal layout options
  */

  public WorksheetLayout.WorksheetLayoutParams getDiagLayoutParams() {
    WorksheetLayout.forceParamsAsNeeded(dacx_, workSheetParamsDiag_);
    return (workSheetParamsDiag_);
  }  
    
  /***************************************************************************
  ** 
  ** Get halo layout options
  */

  public HaloLayout.HaloLayoutParams getHaloLayoutParams() {
    return (haloParams_);
  }    
  
  /***************************************************************************
  ** 
  ** Get stackedBlock layout options
  */

  public StackedBlockLayout.StackedBlockLayoutParams getStackedBlockLayoutParams() {
    return (stackedBlockParams_);
  }    
  
 /***************************************************************************
  ** 
  ** Set the layout options
  */

  public void setLayoutOptions(LayoutOptions opts) {
    options_ = opts;
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Set worksheet layout options
  */

  public void setWorksheetLayoutParams(WorksheetLayout.WorksheetLayoutParams params) {
    workSheetParams_ = params;
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Set diagonal layout options
  */

  public void setDiagonalLayoutParams(WorksheetLayout.WorksheetLayoutParams params) {
    workSheetParamsDiag_ = params;
    return;
  }  
    
  /***************************************************************************
  ** 
  ** Set halo layout options
  */

  public void setHaloLayoutParams(HaloLayout.HaloLayoutParams params) {
    haloParams_ = params;
    return;
  }    
 
  /***************************************************************************
  ** 
  ** Set stacked block layout options
  */

  public void setStackedBlockLayoutParams(StackedBlockLayout.StackedBlockLayoutParams params) {
    stackedBlockParams_ = params;
    return;
  }    

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
}
