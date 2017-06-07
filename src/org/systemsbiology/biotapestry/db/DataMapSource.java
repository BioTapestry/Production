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

import org.systemsbiology.biotapestry.perturb.PerturbationDataMaps;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;

/****************************************************************************
**
** Source for data maps
*/

public interface DataMapSource  {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  
  /***************************************************************************
  ** 
  ** Now just hold onto data maps, not the underlying data:
  */
  
  
  public TimeCourseDataMaps getTimeCourseDataMaps();
  
  /***************************************************************************
  ** 
  ** Now just hold onto data maps, not the underlying data:
  */
  
  
  public void setTimeCourseDataMaps(TimeCourseDataMaps tcdm);

  
  /***************************************************************************
  ** 
  ** Now just hold onto data maps, not the underlying data:
  */
  
  public PerturbationDataMaps getPerturbationDataMaps();
 
  /***************************************************************************
  ** 
  ** Now just hold onto data maps, not the underlying data:
  */
  
  public void setPerturbationDataMaps(PerturbationDataMaps pdms);
  
  
}
