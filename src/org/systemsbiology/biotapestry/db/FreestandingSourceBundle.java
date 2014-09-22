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

package org.systemsbiology.biotapestry.db;

/****************************************************************************
**
** A bundle of independent sources that are detached from the database
*/
      
public class FreestandingSourceBundle  {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
    
  private WorkspaceSource wSrc_;
  private LayoutSource lSrc_;
  private GenomeSource gSrc_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  ** 
  ** Constructor
  */

  public FreestandingSourceBundle(GenomeSource gSrc, LayoutSource lSrc, WorkspaceSource wSrc) {
    gSrc_ = gSrc;
    lSrc_ = lSrc;
    wSrc_ = wSrc;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////      
  
  /***************************************************************************
  ** 
  ** Get the genome source
  */

  public GenomeSource getGenomeSource() {
    return (gSrc_);
  }

  /***************************************************************************
  ** 
  ** Get the workspace source
  */

  public LayoutSource getLayoutSource() {
    return (lSrc_);
  }

  /***************************************************************************
  ** 
  ** Get the workspace source
  */

  public WorkspaceSource getWorkspaceSource() {
    return (wSrc_);
  }
}
