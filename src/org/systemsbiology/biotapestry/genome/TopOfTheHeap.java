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

package org.systemsbiology.biotapestry.genome;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;

/****************************************************************************
**
** If the genomeID we are given is for the full (root) genome, we return that.  
** Else we return the top level instance!
*/

public class TopOfTheHeap {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  protected String genomeID_;  
  protected boolean isInstance_;
  protected DataAccessContext dacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public TopOfTheHeap(DataAccessContext dacx, String genomeID) {
    dacx_ = dacx;
    GenomeSource gs = dacx_.getGenomeSource();
    Genome genome = gs.getGenome(genomeID);
    if (genome instanceof GenomeInstance) {
      GenomeInstance thisGI = (GenomeInstance)genome;
      GenomeInstance rootGI = thisGI.getVfgParentRoot();
      genome = (rootGI == null) ? thisGI : rootGI;
      genomeID_ = genome.getID();
      isInstance_ = true;
    } else {
      genomeID_ = genomeID;
      isInstance_ = false;
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the ID
  */

  public String getID() {
    return (genomeID_);
  }  

  /***************************************************************************
  **
  ** Get if we are an instance
  */

  public boolean isInstance() {
    return (isInstance_);
  }  
  
  /***************************************************************************
  **
  ** Get the actual genome
  */

  public Genome getGenome() {
    return (dacx_.getGenomeSource().getGenome(genomeID_));
  }  
}
