/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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
import java.text.MessageFormat;

import java.util.Vector;
import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.BoundedDoubMinMax;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** This describes a measurement scale
**
*/

public class MeasureScale implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public static final int INFINITY          = 0;
  public static final int VALUE_EXCLUSIVE   = 1;
  public static final int VALUE_INCLUSIVE   = 2;
  private static final int NUM_VAL_OPTIONS_ = 3;

  private static final String INFINITY_STR_          = "infinity";
  private static final String VALUE_EXCLUSIVE_STR_   = "boundExclude";
  private static final String VALUE_INCLUSIVE_STR_   = "boundInclude";

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String id_;
  private String scaleName_;
  private Conversion convToFold_;
  private BoundedDoubMinMax illegalRange_;
  private Double unchanged_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public MeasureScale(String id, String name, Conversion convToFold, BoundedDoubMinMax illegal, Double unchanged) {
    id_ = id;
    scaleName_ = name;
    convToFold_ = convToFold;
    illegalRange_ = illegal;
    unchanged_ = unchanged;
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
    return (scaleName_);
  }  
  
  /***************************************************************************
  **
  ** Get the unchanged value
  */

  public Double getUnchanged() {
    return (unchanged_);
  }  
    
  /***************************************************************************
  **
  ** Set the unchanged value
  */

  public void setUnchanged(Double unchanged) {
    unchanged_ = unchanged;
    return;
  }    
  
  /***************************************************************************
  **
  ** Get the conversion to fold change (may be null)
  */

  public Conversion getConvToFold() {
    return (convToFold_);
  }
  
  /***************************************************************************
  **
  ** Get the illegal range (may be null)
  */

  public BoundedDoubMinMax getIllegalRange() {
    return (illegalRange_);
  }
  
  /***************************************************************************
  **
  ** Set the name
  */

  public void setName(String name) {
    scaleName_ = name;
    return;
  }
 
  /***************************************************************************
  **
  ** Set the conversion to fold change
  */

  public void setConvToFold(Conversion convToFold) {
    convToFold_ = convToFold;
    return;
  } 

  /***************************************************************************
  **
  ** Set the illegal range (may be null)
  */

  public void setIllegalRange(BoundedDoubMinMax illegal) {
    illegalRange_ = illegal;
    return;
  } 

  /***************************************************************************
  **
  ** Get the display string
  */

  public String getDisplayString() {
    return (scaleName_);
  }

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public MeasureScale clone() {
    try {
      MeasureScale newVal = (MeasureScale)super.clone();
      newVal.illegalRange_ = (this.illegalRange_ == null) ? null : (BoundedDoubMinMax)this.illegalRange_.clone();
      newVal.convToFold_ = (this.convToFold_ == null) ? null : (Conversion)this.convToFold_.clone();
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
    out.print("<measureScale id=\"");
    out.print(id_);
    out.print("\" name=\"");
    out.print(CharacterEntityMapper.mapEntities(scaleName_, false));
    if (unchanged_ != null) {
      out.print("\" unchanged=\"");
      out.print(unchanged_);
    }    
    if (convToFold_ != null) {
      out.print("\" foldConvType=\"");
      out.print(Conversion.mapTypeToTag(convToFold_.type));
      if (convToFold_.factor != null) {
        out.print("\" foldConvFac=\"");
        out.print(convToFold_.factor);
      }
    }
    if (illegalRange_ != null) {
      out.print("\" illegalMin=\"");
      out.print(illegalRange_.min);
      out.print("\" incMin=\"");
      out.print(illegalRange_.includeMin);
      out.print("\" illegalMax=\"");
      out.print(illegalRange_.max);
      out.print("\" incMax=\"");
      out.print(illegalRange_.includeMax);
    }
    out.println("\"/>");
    return;
  }
  
  /***************************************************************************
  **
  ** Standard equals
  **
  */  
  
  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof MeasureScale)) {
      return (false);
    }
    MeasureScale otherMS = (MeasureScale)other;
    if (!this.id_.equals(otherMS.id_)) {
      return (false);
    }
    if (!this.scaleName_.equals(otherMS.scaleName_)) {
      return (false);
    }
    if (this.unchanged_ == null) {
      if (otherMS.unchanged_ != null) {
        return (false);
      }
    } else if (!this.unchanged_.equals(otherMS.unchanged_)) {
      return (false);
    }
    
    if (this.convToFold_ == null) {
      if (otherMS.convToFold_ != null) {
        return (false);
      }
    } else if (!this.convToFold_.equals(otherMS.convToFold_)) {
      return (false);
    }
    
    if (this.illegalRange_ == null) {
      return (otherMS.illegalRange_ == null);
    }
      
    return (this.illegalRange_.equals(otherMS.illegalRange_));
       
  }  
 
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  @Override
  public String toString() {
    return ("MeasureScale: " + id_ + " " + scaleName_ + " " + convToFold_ + " " + illegalRange_ + " " + unchanged_);
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
      
  public static class MeasureScaleWorker extends AbstractFactoryClient {
    
    public MeasureScaleWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("measureScale");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("measureScale")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.measScale = buildFromXML(elemName, attrs);
        retval = board.measScale;
      }
      return (retval);     
    }  
        
    private MeasureScale buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "measureScale", "id", true);
      String nameStr = AttributeExtractor.extractAttribute(elemName, attrs, "measureScale", "name", true);
      String foldConvTypeStr = AttributeExtractor.extractAttribute(elemName, attrs, "measureScale", "foldConvType", false);
      String foldConvFacStr = AttributeExtractor.extractAttribute(elemName, attrs, "measureScale", "foldConvFac", false);
      String illegalMin = AttributeExtractor.extractAttribute(elemName, attrs, "measureScale", "illegalMin", false);
      String illegalMax = AttributeExtractor.extractAttribute(elemName, attrs, "measureScale", "illegalMax", false);
      String illegalMinInc = AttributeExtractor.extractAttribute(elemName, attrs, "measureScale", "incMin", false);
      String illegalMaxInc = AttributeExtractor.extractAttribute(elemName, attrs, "measureScale", "incMax", false);
      String unchangedStr = AttributeExtractor.extractAttribute(elemName, attrs, "measureScale", "unchanged", false);

      nameStr = CharacterEntityMapper.unmapEntities(nameStr, false);
         
      Conversion conv = null;      
      if (foldConvTypeStr != null) {
        Double foldConvFac = null; 
        try {
          if (foldConvFacStr != null) {
            foldConvFac = new Double(foldConvFacStr);
          }
        } catch (NumberFormatException nex) {
          throw new IOException();   
        }
        try {
          int foldConvType = Conversion.mapTagToType(foldConvTypeStr);
          conv = new Conversion(foldConvType, foldConvFac);
        } catch (IllegalArgumentException iaex) {
          throw new IOException();
        }
      }
             
      if (((illegalMinInc != null) || (illegalMaxInc != null)) && 
          ((illegalMin == null) || (illegalMax == null))) {
        throw new IOException();
      }
      
      boolean incMin = (illegalMinInc != null) ? Boolean.valueOf(illegalMinInc).booleanValue() : true;
      boolean incMax = (illegalMaxInc != null) ? Boolean.valueOf(illegalMaxInc).booleanValue() : true;
      
      BoundedDoubMinMax illegal = null; 
      try {
        Double min = null;
        Double max = null;
        if (illegalMin != null) {
          if (illegalMax == null) {
            throw new IOException();
          }
          min = new Double(illegalMin);
        }
        if (illegalMax != null) {
          if (illegalMin == null) {
            throw new IOException();
          }
          max = new Double(illegalMax);
        }
        if ((min != null) && (max != null)) {
          if (min.doubleValue() > max.doubleValue()) {
            throw new IOException();
          }
          illegal = new BoundedDoubMinMax(min.doubleValue(), max.doubleValue(), incMin, incMax);
        }
      } catch (NumberFormatException nex) {
        throw new IOException();   
      }
      
      Double unchanged = null; 
      try {
        if (unchangedStr != null) {
          unchanged = new Double(unchangedStr);
        }
        if ((illegal != null) && illegal.contained(unchanged.doubleValue())) {
          throw new IOException();
        }
      } catch (NumberFormatException nex) {
        throw new IOException();   
      }
      return (new MeasureScale(id, nameStr, conv, illegal, unchanged));
    } 
  }
  
  public static class Conversion implements Cloneable {  
    
    public static final int NO_CONVERSION                 = 0;
    public static final int FACTOR_IS_EXPONENT_BASE       = 1;
    public static final int NEGATIVE_RECIPROCAL_IF_LT_ONE = 2;
    private static final int NUM_CONVERT_OPTIONS_         = 3;
    
    private static final String NO_CONVERSION_STR_                 = "none";
    private static final String FACTOR_IS_EXPONENT_BASE_STR_       = "expBase";
    private static final String NEGATIVE_RECIPROCAL_IF_LT_ONE_STR_ = "negRecip";
 
    public int type;
    public Double factor;
    
    public Conversion(int type, Double factor) {
      this.type = type;
      this.factor = factor;
    }
          
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof Conversion)) {
        return (false);
      }    
      Conversion otherC = (Conversion)other;     
      if (this.type != otherC.type) {
        return (false);
      }
      if (this.factor == null) {
        return (otherC.factor == null);
      }
      return (this.factor.equals(otherC.factor));
    }  
    
    public int hashCode() {
      return (type + ((factor == null) ? 0 : (int)Math.round(factor.doubleValue() * 1000.0)));
    }
    
    public Object clone() {
      try {
        Conversion newVal = (Conversion)super.clone();
        return (newVal);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }  
    }
  
    public double toFold(double value) {
      double asFold;
      switch (type) {
        case MeasureScale.Conversion.NO_CONVERSION:
          asFold = value;
          break;
        case MeasureScale.Conversion.FACTOR_IS_EXPONENT_BASE:
          asFold = Math.pow(factor.doubleValue(), value);
          break;
        case MeasureScale.Conversion.NEGATIVE_RECIPROCAL_IF_LT_ONE:
          if (value < 0.0) {
            asFold = 1.0 / -value;
          } else {
            asFold = value;
          }
          break;
        default:
          throw new IllegalArgumentException();
      }
      return (asFold);
    }
    
    public double fromFold(double asFold) {
      double fromFold;
      switch (type) {
        case MeasureScale.Conversion.NO_CONVERSION:
          fromFold = asFold;
          break;
        case MeasureScale.Conversion.FACTOR_IS_EXPONENT_BASE:
          fromFold = Math.log(asFold) / Math.log(factor.doubleValue());
          break;
        case MeasureScale.Conversion.NEGATIVE_RECIPROCAL_IF_LT_ONE:
          if (asFold < 1.0) {
            fromFold = -1.0 / asFold;
          } else {
            fromFold = asFold;
          }
          break;
        default:
          throw new IllegalArgumentException();
      }
      return (fromFold);
    }

    public String getDisplayString(BTState appState) {
      ResourceManager rMan = appState.getRMan();
      switch (type) {
        case NO_CONVERSION:
          return (rMan.getString("scaleConvert." + NO_CONVERSION_STR_)); 
        case FACTOR_IS_EXPONENT_BASE:
          String format = rMan.getString("scaleConvert." + FACTOR_IS_EXPONENT_BASE_STR_);
          return (MessageFormat.format(format, new Object[] {factor})); 
        case NEGATIVE_RECIPROCAL_IF_LT_ONE:
          return (rMan.getString("scaleConvert." + NEGATIVE_RECIPROCAL_IF_LT_ONE_STR_));
        default:
          throw new IllegalArgumentException();
      }
    }

    public static Vector<ChoiceContent> getConvertChoices(BTState appState) {
      Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
      for (int i = 0; i < NUM_CONVERT_OPTIONS_; i++) {
        retval.add(convertTypeForCombo(appState, i));    
      }
      return (retval);
    }
  
    public static ChoiceContent convertTypeForCombo(BTState appState, int type) {
      return (new ChoiceContent(appState.getRMan().getString("scaleConvertOpt." + mapTypeToTag(type)), type));
    }
 
    public static String mapTypeToTag(int value) {
      switch (value) {
        case NO_CONVERSION:
          return (NO_CONVERSION_STR_); 
        case FACTOR_IS_EXPONENT_BASE:
          return (FACTOR_IS_EXPONENT_BASE_STR_);
        case NEGATIVE_RECIPROCAL_IF_LT_ONE:
          return (NEGATIVE_RECIPROCAL_IF_LT_ONE_STR_);
        default:
          throw new IllegalArgumentException();
      }
    }

    public static int mapTagToType(String tag) {
      if (tag.equals(NO_CONVERSION_STR_)) {
        return (NO_CONVERSION);
      } else if (tag.equals(FACTOR_IS_EXPONENT_BASE_STR_)) {
        return (FACTOR_IS_EXPONENT_BASE);
      } else if (tag.equals(NEGATIVE_RECIPROCAL_IF_LT_ONE_STR_)) {
        return (NEGATIVE_RECIPROCAL_IF_LT_ONE);
      } else {
        throw new IllegalArgumentException();
      }
    }
  }
  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    

  public static Vector getIllegalChoices(BTState appState, boolean forNeg) {
    Vector retval = new Vector();
    for (int i = 0; i < NUM_VAL_OPTIONS_; i++) {
      retval.add(convertTypeForCombo(appState, i, forNeg));    
    }
    return (retval);
  }
  
  public static int rangeToType(BoundedDoubMinMax bdmm, boolean forNeg) {
    if (forNeg) {
      if (bdmm.min == Double.NEGATIVE_INFINITY) {
        return (INFINITY);
      } else {
        if (bdmm.includeMin) {
          return (VALUE_INCLUSIVE);
        } else {
          return (VALUE_EXCLUSIVE);
        }
      } 
    } else {
      if (bdmm.max == Double.POSITIVE_INFINITY) {
        return (INFINITY);
      } else {
        if (bdmm.includeMax) {
          return (VALUE_INCLUSIVE);
        } else {
          return (VALUE_EXCLUSIVE);
        }
      } 
    }
  }

  public static ChoiceContent convertTypeForCombo(BTState appState, int type, boolean forNeg) {
    String rs = "illegalBoundOpt." + mapTypeToTag(type);
    String suf = "";
    if (type == INFINITY) {
      suf = (forNeg) ? "neg" : "pos";
    }
    return (new ChoiceContent(appState.getRMan().getString(rs + suf), type));
  }

  public static String mapTypeToTag(int value) {
    switch (value) {
      case INFINITY:
        return (INFINITY_STR_); 
      case VALUE_EXCLUSIVE:
        return (VALUE_EXCLUSIVE_STR_);
      case VALUE_INCLUSIVE:
        return (VALUE_INCLUSIVE_STR_);
      default:
        throw new IllegalArgumentException();
    }
  }

  public static int mapTagToType(String tag) {
    if (tag.equals(INFINITY_STR_)) {
      return (INFINITY);
    } else if (tag.equals(VALUE_EXCLUSIVE_STR_)) {
      return (VALUE_EXCLUSIVE);
    } else if (tag.equals(VALUE_INCLUSIVE_STR_)) {
      return (VALUE_INCLUSIVE);
    } else {
      throw new IllegalArgumentException();
    }
  }
}
