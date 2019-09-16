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

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;

import org.xml.sax.Attributes;

/****************************************************************************
**
** This describes the properties of a measurement technology (e.g. QPCR,
** NanoString)
**
*/

public class MeasureProps implements Cloneable {

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

  private String id_;
  private String measureName_;
  private String scaleKey_;
  private Double negThresh_;
  private Double posThresh_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public MeasureProps(String id, String name, String scaleKey, Double negThresh, Double posThresh) {
    id_ = id;
    measureName_ = name;
    scaleKey_ = scaleKey;
    negThresh_ = negThresh;
    posThresh_ = posThresh;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the id
  */

  public String getID() {
    return (id_);
  }
  
  /***************************************************************************
  **
  ** Get the name
  */

  public String getName() {
    return (measureName_);
  }
  
  /***************************************************************************
  **
  ** Get the key of the scale being used
  */

  public String getScaleKey() {
    return (scaleKey_);
  }
  
  /***************************************************************************
  **
  ** Get the positive threshold (may be null)
  */

  public Double getPosThresh() {
    return (posThresh_);
  }
  
  /***************************************************************************
  **
  ** Get the negative threshold (may be null)
  */

  public Double getNegThresh() {
    return (negThresh_);
  }
  
  /***************************************************************************
  **
  ** Set the name
  */

  public void setName(String measureName) {
    measureName_ = measureName;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the scale key
  */

  public void setScaleKey(String scaleKey) {
    scaleKey_ = scaleKey;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the positive threshold (may be null)
  */

  public void setPosThresh(Double posThresh) {
    posThresh_ = posThresh;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the negative threshold (may be null)
  */

  public void setNegThresh(Double negThresh) {
    negThresh_ = negThresh;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the meaning
  */

  public String getDisplayString(MeasureDictionary md) {
    return (measureName_ + " " + md.getMeasureScale(scaleKey_).getDisplayString());
  }

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public MeasureProps clone() {
    try {
      MeasureProps newVal = (MeasureProps)super.clone();
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }  
  }
   
  /***************************************************************************
  **
  ** Write the properties to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<measureProp id=\"");
    out.print(id_);
    out.print("\" name=\"");
    out.print(CharacterEntityMapper.mapEntities(measureName_, false));
    out.print("\" scale=\"");
    out.print(scaleKey_);
    if (posThresh_ != null) {
      out.print("\" posThresh=\"");
      out.print(posThresh_);
    }
    if (negThresh_ != null) {
      out.print("\" negThresh=\"");
      out.print(negThresh_);
    }
    out.println("\"/>");
    return;
  }

  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  @Override
  public String toString() {
    return ("MeasureProps: " + id_ + " " + measureName_ + " " + scaleKey_);
  }
    
   /***************************************************************************
  **
  ** Answer if we meet the single filtering criteria
  */
  
  @SuppressWarnings("unused")
  public boolean matchesFilter(PertFilter pf, SourceSrc sources) {    
    switch (pf.getCategory()) {    
      case PertFilter.MEASURE_SCALE:
        return (scaleKey_.equals(pf.getStringValue()));
      case PertFilter.MEASURE_TECH:
        return (id_.equals(pf.getStringValue()));
      case PertFilter.SOURCE:
      case PertFilter.SOURCE_NAME:
      case PertFilter.SOURCE_OR_PROXY_NAME:
      case PertFilter.PERT:
      case PertFilter.TIME:
      case PertFilter.INVEST:
      case PertFilter.INVEST_LIST:
      case PertFilter.EXPERIMENT:   
      case PertFilter.TARGET:
      case PertFilter.VALUE:
      case PertFilter.EXP_CONTROL:
      case PertFilter.ANNOTATION:       
      default:
        throw new IllegalArgumentException();
    } 
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
      
  public static class MeasurePropsWorker extends AbstractFactoryClient {
    
    public MeasurePropsWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("measureProp");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("measureProp")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.measProps = buildFromXML(elemName, attrs);
        retval = board.measProps;
      }
      return (retval);     
    }  
        
    private MeasureProps buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "measureProp", "id", true);
      String nameStr = AttributeExtractor.extractAttribute(elemName, attrs, "measureProp", "name", true);
      String scale = AttributeExtractor.extractAttribute(elemName, attrs, "measureProp", "scale", true);
      String pThresh= AttributeExtractor.extractAttribute(elemName, attrs, "measureProp", "posThresh", false);
      String nThresh = AttributeExtractor.extractAttribute(elemName, attrs, "measureProp", "negThresh", false);

      nameStr = CharacterEntityMapper.unmapEntities(nameStr, false);
       
      Double negThresh = null; 
      Double posThresh = null;
      try {
        if (pThresh != null) {
          posThresh = new Double(pThresh);
        }
        if (nThresh != null) {
          negThresh = new Double(nThresh);
        }      
      } catch (NumberFormatException nex) {
        throw new IOException();   
      }
      return (new MeasureProps(id, nameStr, scale, negThresh, posThresh));
    } 
  }
}
