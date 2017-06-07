/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

import java.util.HashMap;
import java.util.Iterator;

import org.systemsbiology.biotapestry.genome.DBInternalLogic;
import org.systemsbiology.biotapestry.genome.Node;

/****************************************************************************
**
** Refactored from Database. Simulation parameter source
*/

public class SimParamSource {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private HashMap<String, String> simDefaultsNode_;
  private HashMap<String, String> simDefaultsGeneAnd_;
  private HashMap<String, String> simDefaultsGeneOr_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Not-null constructor
  */

  public SimParamSource() { 
    simDefaultsNode_ = new HashMap<String, String>();
    simDefaultsNode_.put("initVal", "200.0");
    simDefaultsNode_.put("KD", "0.005");
    simDefaultsNode_.put("Kmult", "1.0");  // This parameter is for non-gene nodes
                                           // with one or more inputs; the Kmult value
                                           // sets the scale for the output of the node,
                                           // relative to the input values.  
    simDefaultsGeneAnd_ = new HashMap<String, String>();
    simDefaultsGeneAnd_.put("DN", "160000000.0");
    simDefaultsGeneAnd_.put("IM", "5.45");
    simDefaultsGeneAnd_.put("initVal", "0.0");
    simDefaultsGeneAnd_.put("KR", "100000.0");
    simDefaultsGeneAnd_.put("KQ", "10.0");
    simDefaultsGeneAnd_.put("KD", "0.005");
    simDefaultsGeneOr_ = new HashMap<String, String>();
    simDefaultsGeneOr_.put("DN", "160000000.0");
    simDefaultsGeneOr_.put("IM", "5.45");
    simDefaultsGeneOr_.put("initVal", "0.0");
    simDefaultsGeneOr_.put("KR", "100000.0");
    simDefaultsGeneOr_.put("KQ", "1.0");
    simDefaultsGeneOr_.put("KD", "0.005");
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 

  /***************************************************************************
  ** 
  ** Get the simulation defaults
  */

  public Iterator<String> getSimulationDefaults(int nodeType, int logicType) {
    if (nodeType != Node.GENE) {
      return (simDefaultsNode_.keySet().iterator());
    } else if (logicType == DBInternalLogic.AND_FUNCTION) {
      return (simDefaultsGeneAnd_.keySet().iterator());
    } else {
      return (simDefaultsGeneOr_.keySet().iterator());
    }
  }
  
  /***************************************************************************
  ** 
  ** Get a simulation default
  */

  public String getSimulationDefaultValue(String key, int nodeType, int logicType) {
    if (nodeType != Node.GENE) {
      return (simDefaultsNode_.get(key));
    } else if (logicType == DBInternalLogic.AND_FUNCTION) {
      return (simDefaultsGeneAnd_.get(key));
    } else {
      return (simDefaultsGeneOr_.get(key));
    }
  }
}
