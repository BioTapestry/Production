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
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;

/****************************************************************************
**
** This describes the properties of an experiment type
**
*/

public class PertProperties implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public final static int UNSIGNED_LINK = 0;
  public final static int REPRESS_LINK  = 1;  
  public final static int PROMOTE_LINK  = 2;   
  public final static int NO_LINK       = 3; 
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String id_;
  private String expType_;
  private String expAbbrev_;
  private String legacyAltTag_;
  private PertDictionary.PertLinkRelation linkRelation_;
  private HashMap<String, String> nameValuePairs_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public PertProperties(String id, String type, String abbrev, PertDictionary.PertLinkRelation linkRelation) {
    id_ = id;
    expType_ = type;
    expAbbrev_ = abbrev;
    legacyAltTag_ = null;
    linkRelation_ = linkRelation;
    nameValuePairs_ = new HashMap<String, String>();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  public boolean semiEquals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof PertProperties)) {
      return (false);
    }
    PertProperties otherPP = (PertProperties)other;
  
    if (!this.expType_.equalsIgnoreCase(otherPP.expType_)) {
      return (false);
    }
  
    if (this.legacyAltTag_ == null) {
      if (otherPP.legacyAltTag_ != null) {
        return (false);
      }
    } else if (!this.legacyAltTag_.equalsIgnoreCase(otherPP.legacyAltTag_)) {
      return (false);
    }
    
    return (this.linkRelation_ == otherPP.linkRelation_);
  }  

  /***************************************************************************
  **
  ** Interpret sign in the presense of a signed proxy
  */
  
  public PertDictionary.PertLinkRelation getRelationWithProxy(String proxSign) {
    boolean reverse = proxSign.equals(PertSource.OPPOSITE_SIGN_PROXY);    
    switch (linkRelation_) {
      case PERT_POS_LINK_POS:     
        return ((reverse) ? PertDictionary.PertLinkRelation.PERT_NEG_LINK_POS : linkRelation_);
      case PERT_NEG_LINK_POS:
        return ((reverse) ? PertDictionary.PertLinkRelation.PERT_POS_LINK_POS : linkRelation_);
      case PERT_ALWAYS_NEGATIVE:
      case PERT_ALWAYS_POSITIVE:
      case PERT_LINK_NO_RELATION:
      case PERT_LINK_OTHER:
        return (linkRelation_);
      default:
        throw new IllegalArgumentException();
    }
  }  
  
  /***************************************************************************
  **
  ** Resolve sign given a perturbation value in the presense of a signed proxy
  */
  
  public int resolveWithProxy(String proxSign, double value, double unchanged) {
    return (resolve(getRelationWithProxy(proxSign), value, unchanged));
  }  
   
  /***************************************************************************
  **
  ** Resolve sign given a perturbation value
  */
  
  public int resolve(double value, double unchanged) {
    return (resolve(linkRelation_, value, unchanged));
  }
  
  /***************************************************************************
  **
  ** Get the relationship between the sign of the perturbation and the sign
  ** of the link it implies (e.g. neg pert -> promotes means opposite sign)
  */

  public PertDictionary.PertLinkRelation getLinkSignRelationship() {
    return (linkRelation_);
  } 
  
  /***************************************************************************
  **
  ** Set the relationship between the sign of the perturbation and the sign
  ** of the link it implies
  */

  public void setLinkSignRelationship(PertDictionary.PertLinkRelation lsRel) {
    linkRelation_ = lsRel;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the id
  */

  public String getID() {
    return (id_);
  } 
  
  /***************************************************************************
  **
  ** Get the legacy alt type (May be null)
  */

  public String getLegacyAlt() {
    return (legacyAltTag_);
  }
  
  /***************************************************************************
  **
  ** Set the legacy alt type (may be null)
  */

  public void setLegacyAlt(String altTag) {
    legacyAltTag_ = altTag;
    return;
  }
 
  /***************************************************************************
  **
  ** Get the type (the "name")
  */

  public String getType() {
    return (expType_);
  }
  
  /***************************************************************************
  **
  ** Get the type
  */

  public String getLegacyType() {
    return ((legacyAltTag_ == null) ? expType_ : legacyAltTag_);
  }
  
  /***************************************************************************
  **
  ** Get the abbrev (may be null)
  */

  public String getAbbrev() {
    return (expAbbrev_);
  }
  
  /***************************************************************************
  **
  ** Set the type
  */

  public void setType(String type) {
    expType_ = type;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the abbrev (may be null)
  */

  public void setAbbrev(String abbrev) {
    expAbbrev_ = abbrev;
    return;
  }

  /***************************************************************************
  **
  ** Get the value for the name
  */

  public String getValue(String key) {
    return ((String)nameValuePairs_.get(key));
  }
  
  /***************************************************************************
  **
  ** Get the boolean value for the name
  */

  public boolean getBooleanValue(String key) {
    String val = (String)nameValuePairs_.get(key);
    return ((val != null) && Boolean.valueOf(val).booleanValue());
  }
  
  /***************************************************************************
  **
  ** Set the value for the name
  */

  public void setNameValue(NameValuePair nvPair) {
    if (nameValuePairs_.get(nvPair.getName()) != null) {
      throw new IllegalArgumentException();
    }
    nameValuePairs_.put(nvPair.getName(), nvPair.getValue());
    return;
  }
  
  /***************************************************************************
  **
  ** Clear the name value pairs
  */

  public void clearNameValuePairs() {
    nameValuePairs_.clear();
    return;
  }

  /***************************************************************************
  **
  ** Get an iterator over the value keys
  */

  public Iterator<String> getNvpKeys() {
    TreeSet<String> ordered = new TreeSet<String>(nameValuePairs_.keySet());
    Iterator<String> oit = ordered.iterator();
    return (oit);
  }
  
  /***************************************************************************
  **
  ** Gets the valid experiment type entry
  */
  
  public TrueObjChoiceContent getExperimentTypeEntry() {  
    return (new TrueObjChoiceContent(expType_, id_));
  }
  
  /***************************************************************************
  **
  ** Clone
  */

  public PertProperties clone() {
    try {
      PertProperties newVal = (PertProperties)super.clone();
      newVal.nameValuePairs_ = new HashMap<String, String>();
      Iterator<String> nvpit = this.nameValuePairs_.keySet().iterator();
      while (nvpit.hasNext()) {
        String name = nvpit.next();
        String val = this.nameValuePairs_.get(name);
        newVal.nameValuePairs_.put(name, val);
      }
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
    out.print("<pertProp id=\"");
    out.print(id_);
    out.print("\" type=\"");
    out.print(CharacterEntityMapper.mapEntities(expType_, false));
    out.print("\"");
    out.print(" linkRel=\"");
    out.print(linkRelation_.getTag());
    out.print("\"");
    if (expAbbrev_ != null) {
      out.print(" abbrev=\"");
      out.print(CharacterEntityMapper.mapEntities(expAbbrev_, false));
      out.print("\"");
    }    
    if (legacyAltTag_ != null) {
      out.print(" altTag=\"");
      out.print(CharacterEntityMapper.mapEntities(legacyAltTag_, false));
      out.print("\"");
    }      
    if (!nameValuePairs_.isEmpty()) {
      out.println(">");
      ind.up();    
      TreeSet<String> ordered = new TreeSet<String>(nameValuePairs_.keySet());
      Iterator<String> oit = ordered.iterator();
      while (oit.hasNext()) {
        String key = oit.next();
        String val = nameValuePairs_.get(key);
        ind.indent();
        out.print("<nVPair name=\"");
        out.print(CharacterEntityMapper.mapEntities(key, false));
        out.print("\" val=\"");
        out.print(CharacterEntityMapper.mapEntities(val, false));
        out.println("\"/>");
      }
      ind.down().indent();
      out.println("</pertProp>");
    } else {
      out.println("/>");
    }
    return;
  }

  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("PertProperty: " + expType_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Map link sign to measurement sign
  */
  
  public static int mapLinkSignToMeasurementSign(int sign) {
    switch (sign) {
      case Linkage.NEGATIVE:
        return (REPRESS_LINK); 
      case Linkage.POSITIVE:
        return (PROMOTE_LINK);
      case Linkage.NONE:
        return (UNSIGNED_LINK);
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Resolve sign given a perturbation value
  */
  
  public static int resolve(PertDictionary.PertLinkRelation relation, double value, double unchanged) {
   
    switch (relation) {
      case PERT_POS_LINK_POS:
        return ((value >= unchanged) ? PROMOTE_LINK : REPRESS_LINK);
      case PERT_NEG_LINK_POS:
        return ((value >= unchanged) ? REPRESS_LINK : PROMOTE_LINK);
      case PERT_ALWAYS_NEGATIVE:
      case PERT_ALWAYS_POSITIVE:
      case PERT_LINK_NO_RELATION:
      case PERT_LINK_OTHER:
        return (UNSIGNED_LINK);
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Answer if link type and value are consistent
  */
  
  public static boolean consistent(PertDictionary.PertLinkRelation relation, int linkType, double value, Double unchangedObj) {
    switch (relation) {
      case PERT_POS_LINK_POS:
        if (unchangedObj == null) {
          return (false);
        }
        if (linkType == UNSIGNED_LINK) {
          return (true);
        }
        return ((linkType == PROMOTE_LINK) ? (value >= unchangedObj.doubleValue()) : (value <= unchangedObj.doubleValue()));
      case PERT_NEG_LINK_POS:
        if (unchangedObj == null) {
          return (false);
        }
        if (linkType == UNSIGNED_LINK) {
          return (true);
        }
        return ((linkType == REPRESS_LINK) ? (value >= unchangedObj.doubleValue()) : (value <= unchangedObj.doubleValue()));   
      case PERT_ALWAYS_NEGATIVE:
        if (unchangedObj == null) {
          return (false);
        }
        //
        // What effect does the link sign have on this determination?  If a particular type
        // of perturbation is supposed to always repress something, then it is inconsistent
        // if the target goes up, no matter what the link context is!
        //
        return (value <= unchangedObj.doubleValue());
      case PERT_ALWAYS_POSITIVE:
        if (unchangedObj == null) {
          return (false);
        }
        // See above case...
        return (value >= unchangedObj.doubleValue());
      case PERT_LINK_NO_RELATION:
      case PERT_LINK_OTHER:
        return (true);
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Answers if the sign of a perturbation, given the particular technology,
  ** implies an indirect linkage.  Classic case: obligate repressor construct
  ** causes target to go up:  must be indirect!
  */
  
  public static boolean signImpliesIndirect(PertDictionary.PertLinkRelation relation, double value, Double unchangedObj) {
    switch (relation) {
      case PERT_ALWAYS_NEGATIVE:
        if (unchangedObj == null) {
          return (false);
        }
        return (value >= unchangedObj.doubleValue());
      case PERT_ALWAYS_POSITIVE:
        if (unchangedObj == null) {
          return (false);
        }
        return (value <= unchangedObj.doubleValue());
      case PERT_POS_LINK_POS:
      case PERT_NEG_LINK_POS:   
      case PERT_LINK_NO_RELATION:
      case PERT_LINK_OTHER:
        return (false);
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
      
  public static class PertPropsWorker extends AbstractFactoryClient {
    
    public PertPropsWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("pertProp");
      installWorker(new NVPairWorker(whiteboard), new MyNVGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("pertProp")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.pertProps = buildFromXML(elemName, attrs);
        retval = board.pertProps;
      }
      return (retval);     
    }  
        
    private PertProperties buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "pertProp", "id", true);
      String typeStr = AttributeExtractor.extractAttribute(elemName, attrs, "pertProp", "type", true);
      String linkRel = AttributeExtractor.extractAttribute(elemName, attrs, "pertProp", "linkRel", true);
      String abbrev = AttributeExtractor.extractAttribute(elemName, attrs, "pertProp", "abbrev", false);
      String altTag = AttributeExtractor.extractAttribute(elemName, attrs, "pertProp", "altTag", false);
        
      typeStr = CharacterEntityMapper.unmapEntities(typeStr, false);
      if (abbrev != null) {
        abbrev = CharacterEntityMapper.unmapEntities(abbrev, false);
      }
      if (altTag != null) {
        altTag = CharacterEntityMapper.unmapEntities(altTag, false);
      }
     
      PertDictionary.PertLinkRelation linkRelVal;
      try {
        linkRelVal = PertDictionary.PertLinkRelation.fromTag(linkRel);
      } catch (IllegalArgumentException iaex) {
        throw new IOException();
      }
      PertProperties retval = new PertProperties(id, typeStr, abbrev, linkRelVal);    
      retval.setLegacyAlt(altTag);
      return (retval);
    } 
  }
  
  public static class MyNVGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      PertProperties pProps = board.pertProps;
      NameValuePair nvp = board.nvPair;
      pProps.setNameValue(nvp);
      return (null);
    }
  }
  
   public static class NVPairWorker extends AbstractFactoryClient {
    
    public NVPairWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("nVPair");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("nVPair")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.nvPair = buildFromXML(elemName, attrs);
        retval = board.nvPair;
      }
      return (retval);     
    }  
        
    private NameValuePair buildFromXML(String elemName, Attributes attrs) throws IOException {
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "nVPair", "name", true);
      String val = AttributeExtractor.extractAttribute(elemName, attrs, "nVPair", "val", true);      
      name = CharacterEntityMapper.unmapEntities(name, false);
      val = CharacterEntityMapper.unmapEntities(val, false);
      NameValuePair retval = new NameValuePair(name, val);
      return (retval);
    } 
  }
}
