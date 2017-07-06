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

package org.systemsbiology.biotapestry.perturb;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.NameValuePair;

import org.xml.sax.Attributes;

/****************************************************************************
**
** A class to specify display options for perturbation data
*/

public class PertDisplayOptions implements Cloneable {
  
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
  
  private ArrayList<MinMax> timeSpanCols_;
  private MinMax nullPertDefaultSpan_;
  private HashMap<String, String> pertMeasureColors_;
  private String currentScaleTag_;
  private boolean breakOutInvestigators_;
  private PerturbationData pd_; 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PertDisplayOptions(PerturbationData pd) {
    pd_ = pd;
    timeSpanCols_ = new ArrayList<MinMax>();
    breakOutInvestigators_ = false;
    nullPertDefaultSpan_ = null;
    MeasureDictionary md = pd.getMeasureDictionary();
    currentScaleTag_ = md.getStandardScaleKeys()[MeasureDictionary.DEFAULT_INDEX];
    pertMeasureColors_ = new HashMap<String, String>();
  }

  /***************************************************************************
  **
  ** Constructor for XML
  */

  public PertDisplayOptions(PerturbationData pd, String pertDefMin, String pertDefMax, 
                            String scaleTag, String breakOutStr) throws IOException {
    
    pd_ = pd;
    timeSpanCols_ = new ArrayList<MinMax>();
    pertMeasureColors_ = new HashMap<String, String>();
    
    nullPertDefaultSpan_ = null;
    if ((pertDefMin != null) && (pertDefMax != null)) {
      try {
        int minTime = Integer.parseInt(pertDefMin);
        int maxTime = Integer.parseInt(pertDefMax);
        nullPertDefaultSpan_ = new MinMax(minTime, maxTime);
      } catch (NumberFormatException ex) {
        throw new IOException();
      }
    }
    
    breakOutInvestigators_ = (breakOutStr != null) ? Boolean.valueOf(breakOutStr).booleanValue() : false;
    currentScaleTag_ = scaleTag; 
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Late binding for legacy I/O
  */
  
  public void setPerturbData(PerturbationData pd) {    
    pd_ = pd;
    return;
  }

  /***************************************************************************
  **
  ** Break out investigators in perturbation display
  */
  
  public boolean breakOutInvestigators() {    
    return (breakOutInvestigators_);
  }
  
  /***************************************************************************
  **
  ** Set whether to break out investigators in perturbation display
  */
  
  public void setBreakOutInvestigatorMode(boolean investMode) {
    breakOutInvestigators_ = investMode;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the measurement display colors
  */
  
  public Map<String, String> getMeasurementDisplayColors() {
    if (pertMeasureColors_.isEmpty()) {
      HashMap<String, String> defMap = new HashMap<String, String>();
      MeasureDictionary md = pd_.getMeasureDictionary();
      Iterator<String> mkit = md.getKeys();
      while (mkit.hasNext()) {
        String mkey = mkit.next();
        defMap.put(mkey, "black"); 
      }
      return (defMap);
    } else {
      return (pertMeasureColors_);
    }
  }
  
  /***************************************************************************
  **
  ** Answer if we are stale following a perturbation data change
  */
  
  public Map<String, String> haveInconsistentMeasurementDisplayColors() {
    if (pertMeasureColors_.isEmpty()) {
      return (null);
    }
    MeasureDictionary md = pd_.getMeasureDictionary();
    HashSet<String> currKeys = new HashSet<String>();
    Iterator<String> mkit = md.getKeys();
    while (mkit.hasNext()) {
      currKeys.add(mkit.next()); 
    }
    if (currKeys.equals(pertMeasureColors_.keySet())) {
      return (null);
    }
    //
    // We are out of whack!  Return the fix!
    //
    
    HashMap<String, String> retval = new HashMap<String, String>();
    mkit = md.getKeys();
    while (mkit.hasNext()) {
      String key = mkit.next();
      String color = pertMeasureColors_.get(key);
      if (color == null) {
        color = "black";
      }
      retval.put(key, color);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answer if we are stale following a perturbation data change
  */
  
  public String haveInconsistentScaleTag() {
    if (currentScaleTag_ == null) {
      return (null);
    }
    MeasureDictionary md = pd_.getMeasureDictionary();
    Iterator<String> mkit = md.getScaleKeys();
    while (mkit.hasNext()) {
      String key = mkit.next();
      if (key.equals(currentScaleTag_)) {
        return (null);
      }
    }
    return (md.getStandardScaleKeys()[MeasureDictionary.DEFAULT_INDEX]);
  }
  
  /***************************************************************************
  **
  ** Set the measurement display colors
  */
  
  public void setMeasurementDisplayColors(Map<String, String> colors) {
    pertMeasureColors_.clear();
    pertMeasureColors_.putAll(colors);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a measurement display color (for IO)
  */
  
  public void addMeasurementDisplayColor(NameValuePair colorPair) {
    pertMeasureColors_.put(colorPair.getName(), colorPair.getValue());
    return;
  }
  
  /***************************************************************************
  **
  ** Get the perturbation data measurement display scale key
  */
  
  public String getPerturbDataDisplayScaleKey() {
    if (currentScaleTag_ == null) {
      MeasureDictionary md = pd_.getMeasureDictionary();
      return (md.getStandardScaleKeys()[MeasureDictionary.DEFAULT_INDEX]);
    } else {
      return (currentScaleTag_);
    }
  }
  
  /***************************************************************************
  **
  ** Set the perturbation data measurement display scale key
  */
  
  public void setPerturbDataDisplayScaleKey(String scaleKey) {
    currentScaleTag_ = scaleKey;
    return;
  }
 
  /***************************************************************************
  **
  ** Get the null perturbation default span.
  */
  
  public MinMax getNullPertDefaultSpan(TimeAxisDefinition tad) {
    if (nullPertDefaultSpan_ == null) {
      return ((tad.isInitialized()) ? tad.getDefaultTimeSpan() : null);
    } else {
      return (nullPertDefaultSpan_);
    }
  }
 
  /***************************************************************************
  **
  ** Set the null perturbation default span.
  */
  
  public void setNullDefaultSpan(MinMax defaultSpan) {
    nullPertDefaultSpan_ = (defaultSpan == null) ? null : (MinMax)defaultSpan.clone();
    return;
  }
 
  /***************************************************************************
  **
  ** Drop column definitions and other data-specific options
  **
  */
  
  public void dropDataBasedOptions() {
    pertMeasureColors_.clear();
    timeSpanCols_.clear();
    nullPertDefaultSpan_ = null;
    currentScaleTag_ = null;
    return;
  }
 
  /***************************************************************************
  **
  ** Set the column headings
  */
  
  public void setColumns(ArrayList<MinMax> columns) {
    timeSpanCols_.clear();
    if (columns != null) {
      timeSpanCols_.addAll(columns);
    }
    return;
  }

  /***************************************************************************
  **
  ** Set a column heading
  */
  
  public void addColumn(MinMax col) {
    timeSpanCols_.add(col);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the column headings iterator
  */
  
  public Iterator<MinMax> getColumnIterator() {
    return (timeSpanCols_.iterator());
  }  
  
  /***************************************************************************
  **
  ** Answer if we have column headings
  */
  
  public boolean hasColumns() {
    return (timeSpanCols_.size() > 0);
  } 
  
  /***************************************************************************
  **
  ** Answer if we have a default time span
  */
  
  public boolean hasDefaultTimeSpan() {
    return (nullPertDefaultSpan_ != null);
  } 
 
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public PertDisplayOptions clone() {
    try {
      PertDisplayOptions retval = (PertDisplayOptions)super.clone();
      
      retval.timeSpanCols_ = new ArrayList<MinMax>();
      Iterator<MinMax> cit = this.timeSpanCols_.iterator();
      while (cit.hasNext()) {
        MinMax mm = cit.next();
        retval.timeSpanCols_.add(mm.clone());
      } 
      
      retval.nullPertDefaultSpan_ = (this.nullPertDefaultSpan_ == null) ? null : (MinMax)this.nullPertDefaultSpan_.clone();  
      retval.pertMeasureColors_ = new HashMap<String, String>(this.pertMeasureColors_);
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<pertDisplayOptions ");
       
    if (breakOutInvestigators_) {
      out.print("breakInvest=\"true\" ");
    }
    
    if (currentScaleTag_ != null) {
      out.print("scale=\"");
      out.print(currentScaleTag_);
      out.print("\" ");
    }
    
    if (nullPertDefaultSpan_ != null) {
      out.print("pdMin=\"");
      out.print(nullPertDefaultSpan_.min);
      out.print("\" pdMax=\"");
      out.print(nullPertDefaultSpan_.max);
      out.print("\" ");
    }
    
    if (hasColumns() || !pertMeasureColors_.isEmpty()) {
      out.println(">");
      ind.up();    
      if (hasColumns()) {
        ind.indent();
        out.println("<colRanges>");
        Iterator<MinMax> cit = timeSpanCols_.iterator();
        ind.up();    
        while (cit.hasNext()) {
          MinMax tc = cit.next();
          ind.indent();
          out.print("<colRange min=\"");
          out.print(tc.min);
          if (tc.min != tc.max) {
            out.print("\" max=\"");
            out.print(tc.max);
          }
          out.println("\" />");
        }
        ind.down().indent(); 
        out.println("</colRanges>");
      }
      if (!pertMeasureColors_.isEmpty()) {
        ind.indent();
        out.println("<pertColors>");
        TreeSet<String> sorted = new TreeSet<String>(pertMeasureColors_.keySet());
        Iterator<String> pcit = sorted.iterator();
        ind.up();    
        while (pcit.hasNext()) {
          String key = pcit.next();
          ind.indent();
          out.print("<pertColor key=\"");
          out.print(key);
          out.print("\" color=\"");
          out.print(pertMeasureColors_.get(key));
          out.println("\" />");
        }
        ind.down().indent(); 
        out.println("</pertColors>");
      }
      ind.down().indent();
      out.println("</pertDisplayOptions>");
    } else {
      out.println("/>");
    }
   
    return;
  }   

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////s
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class PertDisplayOptionsWorker extends AbstractFactoryClient {
 
    public PertDisplayOptionsWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("pertDisplayOptions");
      installWorker(new ColumnWorker(whiteboard), new MyColumnGlue());
      installWorker(new ColorWorker(whiteboard), new MyColorGlue());
    }
       
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {

      Object retval = null;
      if (elemName.equals("pertDisplayOptions")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.pertDisplayOptions = buildFromXML(board.pertData, elemName, attrs);
        retval = board.pertDisplayOptions;
      }
      return (retval);     
    }
    
    private PertDisplayOptions buildFromXML(PerturbationData pd, String elemName, Attributes attrs) throws IOException {  
      String pertDefMin = AttributeExtractor.extractAttribute(elemName, attrs, "pertDisplayOptions", "pdMin", false);
      String pertDefMax = AttributeExtractor.extractAttribute(elemName, attrs, "pertDisplayOptions", "pdMax", false);
      String scaleTag = AttributeExtractor.extractAttribute(elemName, attrs, "pertDisplayOptions", "scale", false);
      String breakOutStr = AttributeExtractor.extractAttribute(elemName, attrs, "pertDisplayOptions", "breakInvest", false);
      return (new PertDisplayOptions(pd, pertDefMin, pertDefMax, scaleTag, breakOutStr));
    }
  }
  
  public static class MyColumnGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      if (board.pertDisplayOptions == null) {
        return (null);
      }
      board.pertDisplayOptions.addColumn(board.column);
      return (null);
    }
  }
  
  public static class MyColorGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      if (board.pertDisplayOptions == null) {
        return (null);
      }
      board.pertDisplayOptions.addMeasurementDisplayColor(board.pertColorPair);
      return (null);
    }
  }
    
  public static class ColumnWorker extends AbstractFactoryClient {
 
    public ColumnWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("colRange");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("colRange")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        if (board.pertDisplayOptions == null) {
          return (null);
        }
        
        
        board.column = buildFromXML(elemName, attrs);
        retval = board.column;
      }
      return (retval);     
    }  
    
    private MinMax buildFromXML(String elemName, Attributes attrs) throws IOException {
      String minStr = AttributeExtractor.extractAttribute(elemName, attrs, "colRange", "min", true);     
      String maxStr = AttributeExtractor.extractAttribute(elemName, attrs, "colRange", "max", false);
      try {
        int minVal = Integer.parseInt(minStr);
        int maxVal = minVal;
        if (maxStr != null) {
          maxVal = Integer.parseInt(maxStr);
        }
        return (new MinMax(minVal, maxVal));
      } catch (NumberFormatException ex) {
        throw new IOException();
      } 
    }
  }  
  
   public static class ColorWorker extends AbstractFactoryClient {
 
    public ColorWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("pertColor");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("pertColor")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        
        if (board.pertDisplayOptions == null) {
          return (null);
        }
        
        board.pertColorPair = buildFromXML(elemName, attrs);
        retval = board.pertColorPair;
      }
      return (retval);     
    }  
    
    private NameValuePair buildFromXML(String elemName, Attributes attrs) throws IOException {
      String keyStr = AttributeExtractor.extractAttribute(elemName, attrs, "pertColor", "key", true);     
      String colorStr = AttributeExtractor.extractAttribute(elemName, attrs, "pertColor", "color", true);
      return (new NameValuePair(keyStr, colorStr));
    }
  }
}
