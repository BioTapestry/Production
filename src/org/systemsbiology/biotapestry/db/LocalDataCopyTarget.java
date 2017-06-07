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

import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.CopiesPerEmbryoData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;

/****************************************************************************
**
** Interface for local copy installs during IO
*/

public interface LocalDataCopyTarget {

  /***************************************************************************
  **
  ** Install initialized data sharing policy
  */

  public void installDataSharing(Metabase.DataSharingPolicy dsp, TabPinnedDynamicDataAccessContext ddacx);

  /***************************************************************************
  ** 
  ** Set the local copies for IO
  */

  public void installLocalTimeCourseData(TimeCourseData timeCourseData);
 
  /***************************************************************************
  ** 
  ** Set the local copies for IO
  */

  public void installLocalPertData(PerturbationData pd);
 
  
  /***************************************************************************
  ** 
  ** Set the local copies for IO
  */

  public void installLocalCopiesPerEmbryoData(CopiesPerEmbryoData copies);
 
  /***************************************************************************
  ** 
  ** Set the time axis definition for IO
  */

  public void installLocalTimeAxisDefinition(TimeAxisDefinition timeAxis);
}
