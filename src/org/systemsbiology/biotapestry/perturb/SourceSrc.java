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

package org.systemsbiology.biotapestry.perturb;

import java.util.List;

/****************************************************************************
**
** Interface for source of pert sources
**
*/

public interface SourceSrc  {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////


  public Experiment getExperiment(String key); 
  public PertSource getSourceDef(String key);
  public String getInvestigator(String key);
  public List<String> getDataPointNotes(String dpKey);
  public List<String> getFootnotesForTarget(String targKey);
  public String getFootnoteListAsString(List<String> annots);
  public PertDictionary getPertDictionary();
  public MeasureDictionary getMeasureDictionary();
  public PertAnnotations getPertAnnotations();
  public ConditionDictionary getConditionDictionary();
  public String getTarget(String key);
  public String getSourceName(String key);
  public PerturbationData.RegionRestrict getRegionRestrictionForDataPoint(String key);
}
