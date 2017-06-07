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
import org.systemsbiology.biotapestry.util.BoundedDoubMinMax;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UniqueLabeller;

/****************************************************************************
**
** This is the collection of possible measurement technology types
**
*/

public class MeasureDictionary implements Cloneable {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  public static final int POS_FOLD_INDEX    = 0;
  public static final int SIGNED_FOLD_INDEX = 1;
  public static final int DDCT_INDEX        = 2;
  
  public static final int DEFAULT_INDEX = POS_FOLD_INDEX;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private HashMap<String, MeasureProps> meas_;
  private UniqueLabeller labels_;
  private String[] defaultScales_;
  private HashMap<String, MeasureScale> scales_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public MeasureDictionary() {
    meas_ = new HashMap<String, MeasureProps>();
    scales_ = new HashMap<String, MeasureScale>();
    labels_ = new UniqueLabeller();
    defaultScales_ = createDefaultScale();  
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Answer if we have user stuff...
  */

  public boolean haveData() {
    if (scales_.size() > defaultScales_.length) {
      return (true);
    }
    if (meas_.isEmpty()) {
      return (false);
    }
    if (meas_.size() == 1) {
      MeasureProps legMp = meas_.values().iterator().next();
      if (legMp.getName().equals("QPCR")) {
        return (false);
      }      
    }
    return (true);
  }
    
  /***************************************************************************
  **
  ** Get a label key:
  */
  
  public String getNextDataKey() {
    return (labels_.getNextLabel());
  }  
  
  /***************************************************************************
  **
  ** Get the default scale
  */

  public String[] getStandardScaleKeys() {
    return (defaultScales_);
  } 
  
  /***************************************************************************
  **
  ** Fill in default SCALEs:
  */
  
  private String[] createDefaultScale() {
    String[] retval = new String[3];
    retval[SIGNED_FOLD_INDEX] = labels_.getNextLabel();
    // Cannot be localizable, since it must remain unique
    MeasureScale.Conversion conv = new MeasureScale.Conversion(MeasureScale.Conversion.NEGATIVE_RECIPROCAL_IF_LT_ONE, null);
    MeasureScale stdScale = new MeasureScale(retval[SIGNED_FOLD_INDEX], "Signed Fold Change", 
                                             conv, new BoundedDoubMinMax(-1.0, 1.0, true, false), new Double(1.0));
    scales_.put(retval[SIGNED_FOLD_INDEX], stdScale);
    retval[POS_FOLD_INDEX] = labels_.getNextLabel();
    // Cannot be localizable, since it must remain unique
    conv = new MeasureScale.Conversion(MeasureScale.Conversion.NO_CONVERSION, null);
    MeasureScale stdScale2 = new MeasureScale(retval[POS_FOLD_INDEX], "Fold Change", 
                                              conv, new BoundedDoubMinMax(Double.NEGATIVE_INFINITY, 0.0, true, true), new Double (1.0));
    scales_.put(retval[POS_FOLD_INDEX], stdScale2);
    retval[DDCT_INDEX] = labels_.getNextLabel();
    // Cannot be localizable, since it must remain unique
    conv = new MeasureScale.Conversion(MeasureScale.Conversion.FACTOR_IS_EXPONENT_BASE, new Double(1.94));
    MeasureScale stdScale3 = new MeasureScale(retval[DDCT_INDEX], "DeltaDeltaCT", conv, null, new Double(0.0));
    scales_.put(retval[DDCT_INDEX], stdScale3);
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public MeasureDictionary clone() {
    try {
      MeasureDictionary newVal = (MeasureDictionary)super.clone();   
      newVal.meas_ = new HashMap<String, MeasureProps>();
      Iterator<String> psit = this.meas_.keySet().iterator();
      while (psit.hasNext()) {
        String psKey = psit.next();
        newVal.meas_.put(psKey, this.meas_.get(psKey).clone());
      }
      newVal.scales_ = new HashMap<String, MeasureScale>();
      Iterator<String> scit = this.scales_.keySet().iterator();
      while (scit.hasNext()) {
        String psKey = scit.next();
        newVal.scales_.put(psKey, this.scales_.get(psKey).clone());
      }     
      newVal.labels_ = this.labels_.clone();
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }  
  
  /***************************************************************************
  **
  ** Fill in legacy measurement props:
  */
  
  public String createLegacyMeasureProps(double thresh) {
    String legKey = defaultScales_[DDCT_INDEX];
    String newID = labels_.getNextLabel();
    MeasureProps legMp = new MeasureProps(newID, "QPCR", legKey, 
                                          new Double(-Math.abs(thresh)), new Double(Math.abs(thresh)));
    meas_.put(newID, legMp);
    return (newID);
  }
 
  /***************************************************************************
  **
  ** Get an iterator over the keys
  */

  public Iterator<String> getKeys() {
    TreeSet<String> ordered = new TreeSet<String>(meas_.keySet());
    Iterator<String> oit = ordered.iterator();
    return (oit);
  }
  
  /***************************************************************************
  **
  ** Get the number of measurement properties:
  */
  
  public int getMeasurePropsCount() {
    return (meas_.size());
  }
  
  /***************************************************************************
  **
  ** Get all the measurement prop names
  */
  
  public Set<String> getMeasurePropsNameSet() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<MeasureProps> ckit = meas_.values().iterator();
    while (ckit.hasNext()) {
      MeasureProps ec = ckit.next();
      retval.add(ec.getName());        
    }
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Get the specified measurement properties:
  */
  
  public MeasureProps getMeasureProps(String key) {
    return (meas_.get(key));
  }
  
  /***************************************************************************
  **
  ** Add / replace a measurement prop:
  */
  
  public void setMeasureProp(MeasureProps mProps) {
    meas_.put(mProps.getID(), mProps);
    return;
  }
  
  /***************************************************************************
  **
  ** Set a measure props:
  */
  
  public void addMeasurePropForIO(MeasureProps mProps) {
    String id = mProps.getID();
    labels_.addExistingLabel(id);
    meas_.put(id, mProps);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop the prop with the given key:
  */
  
  public void dropMeasureProp(String key) {
    meas_.remove(key);
    labels_.removeLabel(key);
    return;
  }
  
   /***************************************************************************
  **
  ** Get an iterator over the scale keys
  */

  public Iterator<String> getScaleKeys() {
    TreeSet<String> ordered = new TreeSet<String>(scales_.keySet());
    Iterator<String> oit = ordered.iterator();
    return (oit);
  }
  
  /***************************************************************************
  **
  ** Get the specified measurement scale:
  */
  
  public int getMeasureScaleCount() {
    return (scales_.size());
  }

  /***************************************************************************
  **
  ** Get the specified measurement scale:
  */
  
  public MeasureScale getMeasureScale(String key) {
    return (scales_.get(key));
  }
  
  /***************************************************************************
  **
  ** Get all the measurement scale names
  */
  
  public Set<String> getMeasureScaleNameSet() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<MeasureScale> ckit = scales_.values().iterator();
    while (ckit.hasNext()) {
      MeasureScale ec = ckit.next();
      retval.add(ec.getName());        
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Add / replace a measurement scale:
  */
  
  public void setMeasureScale(MeasureScale mScale) {
    scales_.put(mScale.getID(), mScale);
    return;
  }
  
  /***************************************************************************
  **
  ** Set a measure scale:
  */
  
  public void addMeasureScaleForIO(MeasureScale mScale) {
    String id = mScale.getID();
    labels_.addExistingLabel(id);
    scales_.put(id, mScale);
    return;
  } 
  
  /***************************************************************************
  **
  ** Drop the scale with the given key:
  */
  
  public void dropMeasureScale(String key) {
    scales_.remove(key);
    labels_.removeLabel(key);
    return;
  }
  
  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  public String getMeasureScaleFromName(String name) {
    if (name == null) {
      return (null);
    }
    String normName = DataUtil.normKey(name);
    Iterator<String> skit = scales_.keySet().iterator();
    while (skit.hasNext()) {
      String sKey = skit.next();
      MeasureScale scale = scales_.get(sKey);
      if (DataUtil.normKey(scale.getName()).equals(normName)) {
        return (sKey);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  public String getMeasurePropsFromName(String name) {
    if (name == null) {
      return (null);
    }
    String normName = DataUtil.normKey(name);
    Iterator<String> skit = meas_.keySet().iterator();
    while (skit.hasNext()) {
      String sKey = skit.next();
      MeasureProps mProps = meas_.get(sKey);
      if (DataUtil.normKey(mProps.getName()).equals(normName)) {
        return (sKey);
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
    out.println("<measureDictionary>");
    ind.up().indent();
    out.println("<scales>");
    ind.up();   
    TreeSet<String> ordered = new TreeSet<String>(scales_.keySet());
    Iterator<String> oit = ordered.iterator();
    while (oit.hasNext()) {
      String key = oit.next();
      MeasureScale pp = scales_.get(key);
      pp.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</scales>");
    if (!meas_.isEmpty()) {
      ind.indent(); 
      out.println("<props>"); 
      ind.up();
      ordered = new TreeSet<String>(meas_.keySet());
      oit = ordered.iterator();
      while (oit.hasNext()) {
        String key = oit.next();
        MeasureProps pp = meas_.get(key);
        pp.writeXML(out, ind);
      }
      ind.down().indent();
      out.println("</props>");
    }
    ind.down().indent();
    out.println("</measureDictionary>");
    return;
  }
  
  /***************************************************************************
  **
  ** Get the choices for measurement
  */
  
  public Vector<TrueObjChoiceContent> getMeasurementOptions() { 
    TreeSet<TrueObjChoiceContent> sorted = new TreeSet<TrueObjChoiceContent>();
    Iterator<String> oit = meas_.keySet().iterator();
    while (oit.hasNext()) {
      String key = oit.next();
      sorted.add(getMeasurementChoice(key));
    }
    return (new Vector<TrueObjChoiceContent>(sorted));
  }  
  
  /***************************************************************************
  **
  ** Get the choice for measurement
  */
  
  public TrueObjChoiceContent getMeasurementChoice(String key) {
    MeasureProps mp = meas_.get(key);
    return (new TrueObjChoiceContent(mp.getDisplayString(this), key));
  } 
  
  /***************************************************************************
  **
  ** Get the choices for scales that can be interchanged
  */
  
  public Vector<TrueObjChoiceContent> getConvertibleScaleOptions() { 
    //
    // It is ALL or none!
    //
    TreeSet<TrueObjChoiceContent> sorted = new TreeSet<TrueObjChoiceContent>();
    Iterator<String> oit = scales_.keySet().iterator();
    while (oit.hasNext()) {
      String key = oit.next();
      MeasureScale ms = scales_.get(key);
      if (ms.getConvToFold() != null) {
        sorted.add(getScaleChoice(key));
      } else {
        return (new Vector<TrueObjChoiceContent>());
      }
    }
    return (new Vector<TrueObjChoiceContent>(sorted));
  }  
    
  /***************************************************************************
  **
  ** Get the choices for scales
  */
  
  public Vector<TrueObjChoiceContent> getScaleOptions() { 
    TreeSet<TrueObjChoiceContent> sorted = new TreeSet<TrueObjChoiceContent>();
    Iterator<String> oit = scales_.keySet().iterator();
    while (oit.hasNext()) {
      String key = oit.next();
      sorted.add(getScaleChoice(key));
    }
    return (new Vector<TrueObjChoiceContent>(sorted));
  }  
  
  /***************************************************************************
  **
  ** Get the choice for scale
  */
  
  public TrueObjChoiceContent getScaleChoice(String key) {
    MeasureScale ms = scales_.get(key);
    return (new TrueObjChoiceContent(ms.getDisplayString(), key));
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
      
  public static class MeasureDictionaryWorker extends AbstractFactoryClient {
    
    public MeasureDictionaryWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("measureDictionary");
      installWorker(new MeasureProps.MeasurePropsWorker(whiteboard), new MyPropsGlue());
      installWorker(new MeasureScale.MeasureScaleWorker(whiteboard), new MyScaleGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("measureDictionary")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.mDict = buildFromXML(elemName, attrs);
        retval = board.mDict;
      }
      return (retval);     
    }
    
    @SuppressWarnings("unused")
    private MeasureDictionary buildFromXML(String elemName, Attributes attrs) throws IOException {
      MeasureDictionary mDict = new MeasureDictionary();
      return (mDict);
    } 
  }
  
  public static class MyPropsGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      MeasureDictionary mDict = board.mDict;
      MeasureProps mProps = board.measProps;
      mDict.addMeasurePropForIO(mProps);
      return (null);
    }
  }   
  
  public static class MyScaleGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      MeasureDictionary mDict = board.mDict;
      MeasureScale mScale = board.measScale;
      mDict.addMeasureScaleForIO(mScale);
      return (null);
    }
  }   
}
