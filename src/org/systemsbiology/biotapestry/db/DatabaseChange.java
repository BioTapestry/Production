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

import java.util.ArrayList;
import java.util.HashMap;

import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.modelBuild.ModelBuilder;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSet;
import org.systemsbiology.biotapestry.timeCourse.CopiesPerEmbryoData;

/***************************************************************************
**
** Info needed to undo a database change (e.g. genome instance, layout);
*/
  
public class DatabaseChange {
  public GenomeInstance oldInstance;
  public GenomeInstance newInstance;
  public DynamicInstanceProxy oldProxy;
  public DynamicInstanceProxy newProxy;  
  public Layout oldLayout;
  public Layout newLayout;
  
  public boolean dataSharingPolicyChange;
  public Metabase.DataSharingPolicy oldDsp;
  public Metabase.DataSharingPolicy newDsp;
  
  public boolean sharedTimeAxisChange;
  public boolean localTimeAxisChange;  
  public TimeAxisDefinition oldTimeAxis;
  public TimeAxisDefinition newTimeAxis;
  
  public boolean sharedPertDataChange;
  public boolean localPertDataChange;
  public PerturbationData oldSharedPertData;
  public PerturbationData newSharedPertData;  

  public boolean sharedTcdChange;
  public boolean localTcdChange;
  public TimeCourseData oldTcd;
  public TimeCourseData newTcd;

  public boolean sharedCPEChange;
  public boolean localCPEChange;  
  public CopiesPerEmbryoData oldCpe;
  public CopiesPerEmbryoData newCpe;
  
  public TemporalInputRangeData oldTir;
  public TemporalInputRangeData newTir;  
  public ArrayList<BuildInstruction> oldBuildInst;
  public ArrayList<BuildInstruction> newBuildInst;
  public InstanceInstructionSet oldInstructSet;
  public String instructSetKey;
  public InstanceInstructionSet newInstructSet; 
  public DBGenome oldGenome;
  public DBGenome newGenome;
  public HashMap<String, Layout> oldLayouts;
  public HashMap<String, Layout> newLayouts;

  public Workspace oldWorkspace;
  public Workspace newWorkspace;  
  public StartupView oldStartupView;
  public StartupView newStartupView;
  public TabNameData oldTabData;
  public TabNameData newTabData;
  public ModelBuilder.Undo oldModelBuilderData;
  public ModelBuilder.Undo newModelBuilderData;   
}
