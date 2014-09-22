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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UniqueLabeller;

/****************************************************************************
**
** This is the collection of possible experiment conditions
**
*/

public class ConditionDictionary implements Cloneable {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private HashMap cond_;
  private UniqueLabeller labels_;
  private String defaultCond_;
  private HashMap controls_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public ConditionDictionary() {
    cond_ = new HashMap();
    controls_ = new HashMap();
    labels_ = new UniqueLabeller();
    defaultCond_ = createDefaultCondition(); 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
   /***************************************************************************
  **
  ** Get a label key:
  */
  
  public String getNextDataKey() {
    return (labels_.getNextLabel());
  }
    
  /***************************************************************************
  **
  ** Get the default condition
  */

  public String getStandardConditionKey() {
    return (defaultCond_);
  }
    
  /***************************************************************************
  **
  ** Answer if we have user stuff...
  */

  public boolean haveData() {
    return ((cond_.size() > 1) || !controls_.isEmpty());
  }
  
  /***************************************************************************
  **
  ** Clone
  */

  public Object clone() {
    try {
      ConditionDictionary newVal = (ConditionDictionary)super.clone();   
      newVal.cond_ = new HashMap();
      Iterator psit = this.cond_.keySet().iterator();
      while (psit.hasNext()) {
        String psKey = (String)psit.next();
        newVal.cond_.put(psKey, ((ExperimentConditions)this.cond_.get(psKey)).clone());
      }
      newVal.controls_ = new HashMap();
      Iterator conit = this.controls_.keySet().iterator();
      while (conit.hasNext()) {
        String contKey = (String)conit.next();
        newVal.controls_.put(contKey, ((ExperimentControl)this.controls_.get(contKey)).clone());
      }   
      newVal.labels_ = (UniqueLabeller)this.labels_.clone();
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }  
  
  /***************************************************************************
  **
  ** Fill in legacy default condition:
  */
  
  private String createDefaultCondition() {   
    String newID = labels_.getNextLabel();
    // Cannot be localizable, since it must remain unique
    ExperimentConditions stdC = new ExperimentConditions(newID, "Standard");
    cond_.put(newID, stdC);
    return (newID);
  }
   
  /***************************************************************************
  **
  ** Get all the condition names
  */
  
  public Set getConditionNameSet() {
    HashSet retval = new HashSet();
    Iterator ckit = cond_.values().iterator();
    while (ckit.hasNext()) {
      ExperimentConditions ec = (ExperimentConditions)ckit.next();
      retval.add(ec.getDescription());        
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Get an iterator over the keys
  */

  public Iterator getKeys() {
    TreeSet ordered = new TreeSet(cond_.keySet());
    Iterator oit = ordered.iterator();
    return (oit);
  }
  
  /***************************************************************************
  **
  ** Get the specified experimental condition
  */
  
  public ExperimentConditions getExprConditions(String key) {
    return ((ExperimentConditions)cond_.get(key));
  }
  
  /***************************************************************************
  **
  ** Add / replace an experimental condition:
  */
  
  public void setExprCondition(ExperimentConditions eCond) {
    cond_.put(eCond.getID(), eCond);
    return;
  }
  
  /***************************************************************************
  **
  ** Set an experimental condition for IO:
  */
  
  public void addExprConditionsForIO(ExperimentConditions eCond) {
    String id = eCond.getID();
    labels_.addExistingLabel(id);
    cond_.put(id, eCond);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop the condition with the given key:
  */
  
  public void dropExprConditions(String key) {
    cond_.remove(key);
    labels_.removeLabel(key);
    return;
  }
  
  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  public String getConditionKeyFromName(String name) {
    String normName = DataUtil.normKey(name);
    Iterator ckit = cond_.keySet().iterator();
    while (ckit.hasNext()) {
      String cKey = (String)ckit.next();
      ExperimentConditions cond = (ExperimentConditions)cond_.get(cKey);
      if (DataUtil.normKey(cond.getDescription()).equals(normName)) {
        return (cKey);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the control keys
  */

  public Iterator getControlKeys() {
    TreeSet ordered = new TreeSet(controls_.keySet());
    Iterator oit = ordered.iterator();
    return (oit);
  }
  
  /***************************************************************************
  **
  ** Get the number of experimental controls
  */
  
  public int getExprControlCount() {
    return (controls_.size());
  }
 
  /***************************************************************************
  **
  ** Get all the control names
  */
  
  public Set getControlNameSet() {
    HashSet retval = new HashSet();
    Iterator ckit = controls_.values().iterator();
    while (ckit.hasNext()) {
      ExperimentControl ec = (ExperimentControl)ckit.next();
      retval.add(ec.getDescription());        
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the specified experimental control
  */
  
  public ExperimentControl getExprControl(String key) {
    return ((ExperimentControl)controls_.get(key));
  }
  
  /***************************************************************************
  **
  ** Add / replace an experimental control:
  */
  
  public void setExprControl(ExperimentControl eCond) {
    controls_.put(eCond.getID(), eCond);
    return;
  }
  
  /***************************************************************************
  **
  ** Set an experimental control for IO:
  */
  
  public void addExprControlForIO(ExperimentControl eCond) {
    String id = eCond.getID();
    labels_.addExistingLabel(id);
    controls_.put(id, eCond);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop the control with the given key:
  */
  
  public void dropExprControl(String key) {
    controls_.remove(key);
    labels_.removeLabel(key);
    return;
  }
  
  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  public String getControlKeyFromName(String name) {
    if (name == null) {
      return (null);
    }
    String normName = DataUtil.normKey(name);
    Iterator ckit = controls_.keySet().iterator();
    while (ckit.hasNext()) {
      String cKey = (String)ckit.next();
      ExperimentControl ctrl = (ExperimentControl)controls_.get(cKey);
      if (DataUtil.normKey(ctrl.getDescription()).equals(normName)) {
        return (cKey);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Write the dictionary to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.println("<conditionDictionary>");     
    ind.up().indent();
    out.println("<conditions>");
    ind.up();
    TreeSet ordered = new TreeSet(cond_.keySet());
    Iterator oit = ordered.iterator();
    while (oit.hasNext()) {
      String key = (String)oit.next();
      ExperimentConditions pp = (ExperimentConditions)cond_.get(key);
      pp.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</conditions>");
    if (!controls_.isEmpty()) {
      ind.indent(); 
      out.println("<controls>"); 
      ind.up();
      ordered = new TreeSet(controls_.keySet());
      oit = ordered.iterator();
      while (oit.hasNext()) {
        String key = (String)oit.next();
        ExperimentControl pp = (ExperimentControl)controls_.get(key);
        pp.writeXML(out, ind);
      }
      ind.down().indent();
      out.println("</controls>");
    }
    ind.down().indent();
    out.println("</conditionDictionary>");
    return;
  }
  
  /***************************************************************************
  **
  ** Get the choices for conditions
  */
  
  public Vector getExprConditionsOptions() { 
    TreeSet sorted = new TreeSet();
    Iterator oit = cond_.keySet().iterator();
    while (oit.hasNext()) {
      String key = (String)oit.next();
      sorted.add(getExprConditionsChoice(key));
    }
    return (new Vector(sorted));
  }  
  
  /***************************************************************************
  **
  ** Get the choice for condition
  */
  
  public TrueObjChoiceContent getExprConditionsChoice(String key) {
    ExperimentConditions mp = (ExperimentConditions)cond_.get(key);
    return (new TrueObjChoiceContent(mp.getDisplayString(), key));
  }  

  /***************************************************************************
  **
  ** Get the choices for controls
  */
  
  public Vector getExprControlOptions() { 
    TreeSet sorted = new TreeSet();
    Iterator oit = controls_.keySet().iterator();
    while (oit.hasNext()) {
      String key = (String)oit.next();
      sorted.add(getExprControlChoice(key));
    }
    return (new Vector(sorted));
  }  
  
  /***************************************************************************
  **
  ** Get the choice for a control
  */
  
  public TrueObjChoiceContent getExprControlChoice(String key) {
    ExperimentControl mp = (ExperimentControl)controls_.get(key);
    return (new TrueObjChoiceContent(mp.getDisplayString(), key));
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** For XML I/O
  ** 
  */ 
      
  public static class ConditionDictionaryWorker extends AbstractFactoryClient {
    
    public ConditionDictionaryWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("conditionDictionary");
      installWorker(new ExperimentConditions.ExperimentCondWorker(whiteboard), new MyCondGlue());
      installWorker(new ExperimentControl.ExperimentControlWorker(whiteboard), new MyControlGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("conditionDictionary")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.cDict = buildFromXML(elemName, attrs);
        retval = board.cDict;
      }
      return (retval);     
    }  
        
    private ConditionDictionary buildFromXML(String elemName, Attributes attrs) throws IOException {
      ConditionDictionary cDict = new ConditionDictionary();
      return (cDict);
    } 
  }
  
  public static class MyCondGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.cDict.addExprConditionsForIO(board.experCond);
      return (null);
    }
  }
  
  public static class MyControlGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.cDict.addExprControlForIO(board.expControl);
      return (null);
    }
  }   
}
