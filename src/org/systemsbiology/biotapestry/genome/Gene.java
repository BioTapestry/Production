/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

import java.util.Iterator;

/****************************************************************************
**
** This represents a gene
*/

public interface Gene extends Node, Cloneable {
  
  /*
  ** Experimental Evidence
  */

  public static final int LEVEL_UNDEFINED = -2;
  public static final int LEVEL_VARIOUS   = -1;
  
  public static final int LEVEL_NONE = 0;
  public static final int LEVEL_1    = 1;
  public static final int NUM_EVIDENCE_LEVELS = 2;
  
  public static final String LEV_NONE_STR = "none";  
  public static final String LEV_1_STR    = "isolated";  
  
  /***************************************************************************
  **
  ** Get a region iterator
  */
  
  public Iterator<DBGeneRegion> regionIterator();

  /***************************************************************************
  **
  ** Get a region count
  */
  
  public int getNumRegions(); 
  
  /***************************************************************************
  **
  ** Get the region holding a pad. May be null:
  */
  
  public DBGeneRegion getRegionForPad(int padNum);
  
  /***************************************************************************
  **
  ** Get the experimental verification level
  */
  
  public int getEvidenceLevel();

  /***************************************************************************
  **
  ** Supports cloning
  */
  
  public Gene clone();  
}
