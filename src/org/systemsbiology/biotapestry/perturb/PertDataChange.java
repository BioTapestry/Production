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
import java.util.Map;

/***************************************************************************
**
** Info needed to undo a Perturbation Data change
**
*/
  
public class PertDataChange {
 
  public enum Mode {
                    UNDEFINED,
                    DATA_POINT,
                    EXPERIMENT,
                    DATA_POINT_SET,
                    EXPERIMENT_SET,
                    SOURCE_DEF,
                    USER_FIELD_VALS,
                    USER_FIELD_NAME,
                    REG_RESTRICT,
                    DATA_ANNOTS,
                    TARGET_ANNOTS,
                    SOURCE_NAME,
                    EXP_CONTROL,
                    INVEST,
                    DATA_ANNOTS_SET,
                    SOURCE_DEF_SET,
                    MEASURE_PROP,
                    EXP_COND,
                    MEAS_SCALE,
                    TARGET_NAME,
                    ANNOTATION,
                    PERT_PROP,
                    TARG_ANNOTS_SET,
                    ANNOT_MODULE,
                    NAME_MAPPER,
                    MEASURE_PROP_SET,
                   };
 
  public long serialNumberOrig;
  public long serialNumberNew;
  public Mode mode;

  public Map<String, List<String>> userDataSubsetOrig;
  public Map<String, List<String>> userDataSubsetNew;
  
  public Map<String, PertSource> srcDefsSubsetOrig;
  public Map<String, PertSource> srcDefsSubsetNew; 

  public Map<String, MeasureProps> mPropsSubsetOrig;
  public Map<String, MeasureProps> mPropsSubsetNew;
  
  public PertAnnotations pAnnotOrig;
  public PertAnnotations pAnnotNew;
  
  public int userFieldIndex;      
  public String userFieldNameOrig;
  public String userFieldNameNew;     
  
  public String userDataKey;
  public List<String> userDataOrig;
  public List<String> userDataNew;
  
  public Map<String, PerturbationData.RegionRestrict> dataPtRegResSubsetOrig;
  public Map<String, PerturbationData.RegionRestrict> dataPtRegResSubsetNew;
  
  public Map<String, List<String>> dataPtNotesSubsetOrig;
  public Map<String, List<String>> dataPtNotesSubsetNew;
  
  public Map<String, PertDataPoint> dataPtsSubsetOrig;
  public Map<String, PertDataPoint> dataPtsSubsetNew;
  
  public Map<String, List<String>> targetNotesSubsetOrig;
  public Map<String, List<String>> targetNotesSubsetNew;
    
  public Map<String, Experiment> expSubsetOrig;
  public Map<String, Experiment> expSubsetNew;
  
  public Experiment expOrig;
  public Experiment expNew;
  public double threshOrig;
  public double threshNew;
  public boolean threshChange;
  
  public PertSource srcDefOrig;
  public PertSource srcDefNew;
  
  public PertDataPoint pdpOrig;
  public PertDataPoint pdpNew;
  
  public MeasureProps mpOrig;
  public MeasureProps mpNew; 
  
  public MeasureScale mScaleOrig;
  public MeasureScale mScaleNew;

  public PertProperties ppOrig;
  public PertProperties ppNew;
  
  public ExperimentConditions ecOrig;
  public ExperimentConditions ecNew;
  
  public ExperimentControl ectrlOrig;
  public ExperimentControl ectrlNew;
 
  public String targetKey;
  public String targetOrig;
  public String targetNew;
  
  public String targetAnnotKey;
  public List<String> targetAnnotOrig;
  public List<String> targetAnnotNew; 
  
  public String dataAnnotsKey;
  public List<String> dataAnnotsOrig;
  public List<String> dataAnnotsNew;
  
  public String dataRegResKey;
  public PerturbationData.RegionRestrict dataRegResOrig;
  public PerturbationData.RegionRestrict dataRegResNew;

  public String srcNameKey;
  public String srcNameOrig;
  public String srcNameNew; 
    
  public String investKey;
  public String investOrig;
  public String investNew;
  
  public String annotKey;
  public String annotMsgOrig;
  public String annotMsgNew;
  public String annotTagOrig;
  public String annotTagNew;
  
  public String nameMapperMode;
  public String nameMapperKey;
  public List<String> nameMapperListOrig;
  public List<String> nameMapperListNew;
 
  public PertDataChange(long serNum) {
    this(serNum, Mode.UNDEFINED);  // FIX ME!!!!
  }
  
  public PertDataChange(long serNum, Mode mode) {
    this.mode = mode;
    serialNumberOrig = serNum;  
  }  
}
