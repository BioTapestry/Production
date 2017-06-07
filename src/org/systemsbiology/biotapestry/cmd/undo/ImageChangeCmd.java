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


package org.systemsbiology.biotapestry.cmd.undo;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.nav.ImageChange;
import org.systemsbiology.biotapestry.nav.NavTree;

/****************************************************************************
**
** Handles undos of image changes
*/

public class ImageChangeCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private ImageChange restore_;
  private static final long serialVersionUID = 1L;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build the command
  */ 
  
  public ImageChangeCmd(DataAccessContext dacx, ImageChange restore) {
    super(dacx);
    restore_ = restore;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Name to show
  */ 
  
  @Override
  public String getPresentationName() {
    return ("Image Change");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
  
  @Override
  public void undo() {
    super.undo();
    if (restore_.genomeKey != null) {
      Genome imgGenome = dacx_.getGenomeSource().getGenome(restore_.genomeKey);
      imgGenome.imageChangeUndo(uics_.getImageMgr(), restore_);           
    } else if (restore_.proxyKey != null) {
      DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(restore_.proxyKey);
      dip.imageChangeUndo(uics_.getImageMgr(), restore_);
    } else if (restore_.groupNodeKey != null) {
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
      nt.undoImageChange(restore_);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo the operation
  */ 
  
  @Override
  public void redo() {
    super.redo();
    if (restore_.genomeKey != null) {
      Genome imgGenome = dacx_.getGenomeSource().getGenome(restore_.genomeKey);
      imgGenome.imageChangeRedo(uics_.getImageMgr(), restore_);           
    } else if (restore_.proxyKey != null) {
      DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(restore_.proxyKey);
      dip.imageChangeRedo(uics_.getImageMgr(), restore_);
    } else if (restore_.groupNodeKey != null) {
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
      nt.redoImageChange(restore_);
    }
    return;
  }
}
