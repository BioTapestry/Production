/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.util.BoundedDoubMinMax;
import org.systemsbiology.biotapestry.util.DataUtil;

/****************************************************************************
**
** This holds QPCR data records read in from CSV inputs
*/

public class CSVData {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList perturbs_;
  private String time_;
  private String date_;
  private String batchID_;
  private String condition_;
  private ArrayList investigators_;
  private HashMap measurements_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor for new standard CSV usage
  **    RepeatID PerturbationAgent MeasuredGene  Time  DeltaDeltaCt  ExpControl Significancy Comment
  */

  public CSVData(BTState appState, List expToks, String date, List investigators, String time, String condition, String key) {
    appState_ = appState;
    perturbs_ = new ArrayList();
    int numEt = expToks.size();
    for (int i = 0; i < numEt; i++) {
      ExperimentTokens etok = (ExperimentTokens)expToks.get(i);      
      perturbs_.add(etok);
    }  
    time_ = time;
    date_ = date;   
    ConditionDictionary cDict = appState_.getDB().getPertData().getConditionDictionary();
    String stdName = cDict.getExprConditions(cDict.getStandardConditionKey()).getDescription();   
    if ((condition == null) || condition.trim().equals("") || condition.equalsIgnoreCase(stdName)) {
      condition_ = stdName;
    } else {
      condition_ = condition;  
    }
    investigators_ = new ArrayList(investigators);
    batchID_ = key;
    measurements_ = new HashMap();    
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Answers if the measurement is valid
  **
  */
  
  public static boolean isValidMeasurement(String value, BoundedDoubMinMax illegal) {
    if (value == null) {
      return (false);
    } 
    String input = value.trim();
    if (input.equals("")) {
      return (false);
    }
    double val;
    try {
      val = Double.parseDouble(input);
    } catch (NumberFormatException nfe) {
      return (false); 
    }
    if (illegal == null) {
      return (true);
    }
    return (!illegal.contained(val));
  }

  /***************************************************************************
  **
  ** Add a data point
  */
  
  public void addDataPoint(DataPoint dp) {
    String target = DataUtil.normKey(dp.target);      
    ArrayList values = (ArrayList)measurements_.get(target);
    if (values == null) {
      values = new ArrayList();
      measurements_.put(target, values);
    }
    values.add(dp);
    return;
  }
   
  /***************************************************************************
  **
  ** Get the set of targets
  */
  
  public Set getTargets() {
    return (measurements_.keySet());
  }
  
  /***************************************************************************
  **
  ** Get the list of sources
  */
  
  public List getSources() {
    return (perturbs_);
  }
  
  /***************************************************************************
  **
  ** Get the measurement time
  */
  
  public String getTime() {
    return (time_);
  }
  
  /***************************************************************************
  **
  ** Get the measurement date
  */
  
  public String getDate() {
    return (date_);
  }
  
  /***************************************************************************
  **
  ** Get the list of investigators
  */
  
  public List getInvestigators() {
    return (investigators_);
  }
  
  /***************************************************************************
  **
  ** Get the batch key
  */
  
  public String getBatchID() {
    return (batchID_);
  }  
  
  /***************************************************************************
  **
  ** Get the condition
  */
  
  public String getCondition() {
    return (condition_);
  }  
  
  /***************************************************************************
  **
  ** Get measurements (List of DataPoint) for the target
  */
  
  public List getMeasurements(String target) {
    return ((List)measurements_.get(target));
  }
  
  /***************************************************************************
  **
  ** Get original target name
  */
  
  public String getOriginalTargetName(String targetKey) {
    List meas = (List)measurements_.get(targetKey);
    DataPoint dp = (DataPoint)meas.get(0);
    return (dp.target);
  }  
  

  /***************************************************************************
  **
  ** Build an access key
  */
  
  public static String buildRowKey(List etoks, String date, List invests, 
                                   String time, String condition, String fullBatchID) {
               
    StringBuffer buf = new StringBuffer();
    buf.append(date);
    buf.append("*$*");
    buf.append(time);
    buf.append("*$*");
    buf.append(condition);
    buf.append("*$*");
    buf.append(fullBatchID);
    buf.append("*$*");
    
    ArrayList sortInv = new ArrayList(invests);
    Collections.sort(sortInv);
    Iterator lit = invests.iterator();
    while (lit.hasNext()) {
      String invest = (String)lit.next();
      buf.append(invest);
      if (lit.hasNext()) {
        buf.append("*$*");
      }
    }
    
    StringBuffer buf2 = new StringBuffer();
    ArrayList otoks = new ArrayList();
    Iterator eit = etoks.iterator();
    while (eit.hasNext()) {
      ExperimentTokens tok = (ExperimentTokens)eit.next();
      buf2.setLength(0);
      buf2.append(tok.base);
      buf2.append(tok.expType);
      otoks.add(buf2.toString());
    }
    Collections.sort(otoks);
    Iterator oit = otoks.iterator();
    while (oit.hasNext()) {
      String tok = (String)oit.next();
      buf.append(tok);
      if (oit.hasNext()) {
        buf.append("*$*");
      }
    }
    return (buf.toString());      
  }
  
  /***************************************************************************
  **
  ** Build a batch key
  */
  
  public static String buildBatchKey(String date, List list, String repeatNum, String time, String condition, 
                                     boolean useDate, boolean useTime, boolean useBatch, boolean useInvest, 
                                     boolean useCondition) {
    StringBuffer buf = new StringBuffer();
    boolean isFirst = true;
    if (useDate) {
      buf.append(date);
      isFirst = false;
    }
    if (useInvest) {
      if (!isFirst) {
        buf.append("::");
      }
      isFirst = false;
      ArrayList sortInv = new ArrayList(list);
      Collections.sort(sortInv);
      Iterator lit = sortInv.iterator();
      while (lit.hasNext()) {
        String invest = (String)lit.next();
        buf.append(invest);
        if (lit.hasNext()) {
          buf.append("::");
        }
      }
    }
    if (useBatch) {
      if (!isFirst) {
        buf.append("::");
      }
      isFirst = false;
      buf.append(repeatNum);
    }
    if (useTime) {
      if (!isFirst) {
        buf.append("::");
      }
      isFirst = false;
      buf.append(time);
    }
    if (useCondition) {
      if (condition == null) {
        condition = "";
      }
      if (!isFirst) {
        buf.append("::");
      }
      isFirst = false;
      buf.append(condition);      
    }
    return (buf.toString());
  }  
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public static class DataPoint {    
    public String target;
    public String value;
    public String isValid;
    public String comment;
    public String control;
    
    public List annots;
    public String measurement;
    public HashMap userFields;
      
    public DataPoint(String target, String value, String control, String isValid, String comment) {
      this.target = target;
      this.value = value;
      this.isValid = isValid;
      this.comment = comment;
      this.control = control;
      this.userFields = new HashMap();
    }     
  }
  
  /***************************************************************************
  ** 
  ** Experiment tokens
  */  
  
  public static class ExperimentTokens {    
    public String base;
    public String expType;
    public String orig;

    public ExperimentTokens() {
    }     
    
    public ExperimentTokens(String base, String expType, String orig) {
      this.base = base;
      this.expType = expType;
      this.orig = orig;
    }
    
    public boolean haveAMatch(String value, String name, String abbrev) { 
      int expIndex = -1;
      boolean gottaMatch = false;
      String vuc = DataUtil.normKey(value);
      int chkIndex = vuc.indexOf(DataUtil.normKey(name));
      if ((chkIndex != -1) && (chkIndex == vuc.length() - name.length())) {
        expIndex = chkIndex;
        gottaMatch = true;
      } 
      
      if (!gottaMatch) {
        if (abbrev != null) {
          chkIndex = vuc.indexOf(DataUtil.normKey(abbrev));
          if ((chkIndex != -1) && (chkIndex == vuc.length() - abbrev.length())) {
            expIndex = chkIndex;
            gottaMatch = true;
          }
        }
      }
 
      if (gottaMatch) {
        expType = name;
        base = value.substring(0, expIndex);
        orig = value;
      }
      
      return (gottaMatch);
    }
  }
}
