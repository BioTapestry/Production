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

package org.systemsbiology.biotapestry.qpcr;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.Splitter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;

/****************************************************************************
**
** This holds QPCR perturbation source data
**
*/

class Source implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

   static final int NO_PROXY_INDEX         = 0;
   static final String NO_PROXY            = "noProxy";
   static final String SAME_SIGN_PROXY     = "sameSign";
   static final String OPPOSITE_SIGN_PROXY = "oppositeSign";
  
   static final String MASO_TYPE      = "MASO";
   static final String MOE_TYPE       = "MOE";
   static final String ENGRAILED_TYPE = "Engrailed";
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String value_;
  private String notes_;
  private String proxyFor_;
  private String proxySign_;
  private String expType_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

   Source(String notes, String type, String proxyFor, String proxySign) throws IOException {
    notes_ = notes;
    if (proxySign == null) {
      proxySign_ = NO_PROXY;
    } else if (!(proxySign.equals(NO_PROXY) || 
                 proxySign.equals(SAME_SIGN_PROXY) ||
                 proxySign.equals(OPPOSITE_SIGN_PROXY))) {
      throw new IOException();
    } else {
      proxySign_ = proxySign;
    }
    
    if (type == null) {
      expType_ = null;
    } else if (!(type.equals(MASO_TYPE) || 
                 type.equals(MOE_TYPE) ||
                 type.equals(ENGRAILED_TYPE))) {
      throw new IOException();
    } else {
      expType_ = type;
    }    
    
    if ((proxyFor != null) && proxySign_.equals(NO_PROXY)) { 
      throw new IOException();
    }
    proxyFor_ = proxyFor;
  }  
  
  /***************************************************************************
  **
  ** Constructor
  */

   Source(String notes) {
    notes_ = notes;
    proxyFor_ = null;
    proxySign_ = NO_PROXY;
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

   Source(String base, String experiment) {
    base = base.replaceAll(" ", "");
    experiment = experiment.replaceAll(" ", "");
    value_ = base;
    expType_ = (experiment.toUpperCase().equals("EN")) ? ENGRAILED_TYPE : experiment.toUpperCase();
    proxyFor_ = null;
    proxySign_ = NO_PROXY;    
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

   Source(Source other) {
    this.value_ = other.value_; 
    this.notes_ = other.notes_;
    this.proxyFor_ = other.proxyFor_;
    this.proxySign_ = other.proxySign_;
    this.expType_ = other.expType_;
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

   Source() {
    proxyFor_ = null;
    proxySign_ = NO_PROXY;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Clone
  */

   public Object clone() {
    try {
      return (super.clone());
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }
  
  /***************************************************************************
  **
  ** Return experiment type
  */
  
   String getExpType() {
    return (expType_);
  }

  /***************************************************************************
  **
  ** Get the source
  */
  
   String getBaseType() {
    return (value_);
  } 
  
  /***************************************************************************
  **
  ** Get the value
  */
  
   String getDisplayValue() {
    if (expType_.equals(ENGRAILED_TYPE)) {
      return (value_ + "-En");
    } else {
      return (value_ + " " + expType_);
    }
  }
  
  /***************************************************************************
  **
  ** Get the value with a superscript
  */
  
   String getDisplayValueWithSuperScript() {
    StringBuffer buf = new StringBuffer();
    buf.append(getDisplayValue());    
    if (notes_ != null) {
      buf.append("<sup>");
      buf.append(notes_);
      buf.append("</sup>");
    } 
    return (buf.toString());
  }  
  
  /***************************************************************************
  **
  ** Get the notes
  */
  
   String getNotes() {
    return (notes_);
  }
  
  /***************************************************************************
  **
  ** Set the display value (Legacy loads only)
  */
  
   void setDisplayValue(String value) throws IOException {
    //
    // Lame experiment parse:
    //
    if (value.indexOf("-En") != -1) {
      expType_ = ENGRAILED_TYPE;
    } else if (value.indexOf("MASO") != -1) {
      expType_ = MASO_TYPE;
    } else if (value.indexOf("MOE") != -1) {  
      expType_ = MOE_TYPE;
    } else {
      throw new IOException();
    }
    //
    // Lame base value parse:
    //
    int index = value.lastIndexOf(" ");    
    if (index != -1) {
      value_ = value.substring(0, index);
      return;
    } else {
      index = value.indexOf("-");
      if (index != -1) {
        value_ = value.substring(0, index);
        return;
      }
    }
    throw new IOException();
  }     
  
  /***************************************************************************
  **
  ** Set the base value
  */
  
   void setBaseValue(String baseValue) {
    value_ = baseValue.replaceAll(" ", "");
    return;
  }  

  /***************************************************************************
  **
  ** Set the value
  */
  
   void setValue(String value, String exp) {
    value_ = value;
    expType_ = exp;
    return;
  } 

  /***************************************************************************
  **
  ** Set the notes
  */
  
   void setNotes(String notes) {
    notes_ = notes;
    return;
  }  

  /***************************************************************************
  **
  ** Get the list of footnote numbers used 
  */
  
   List<String> getFootnoteNumbers() {
    if (notes_ != null) {
      return (Splitter.stringBreak(notes_, ",", 0, true));
    } else {
      return (new ArrayList<String>());
    }
  }
  
  /***************************************************************************
  **
  ** Get the proxied species
  */
  
   String getProxiedSpecies() {
    return (proxyFor_);
  }
  
  /***************************************************************************
  **
  ** Set the proxied species
  */
  
   void setProxiedSpecies(String species) {
    proxyFor_ = species;
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the proxy sign
  */
  
   String getProxySign() {
    return (proxySign_);
  } 
  
  /***************************************************************************
  **
  ** Set the proxy sign
  */
  
   void setProxySign(String proxySign) {
    proxySign_ = proxySign;
    return;
  }   

  /***************************************************************************
  **
  ** Write the source to HTML
  */
  
   void writeHTML(PrintWriter out, Indenter ind, boolean hasNext, QpcrTablePublisher qtp, boolean doBold) {
    ind.indent();    
    qtp.paragraph(false);
    if (doBold) {
      out.print("<b>");
    }
    out.print(getDisplayValueWithSuperScript());
    if (hasNext) {
      out.print(" +");
    }
    if (doBold) {
      out.print("</b>");
    }
    out.println("</p>");
    return;
  }  
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
   public String toString() {
    return ("Source: value = " + value_ + " notes = " + notes_);
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
   static Set<String> keywordsOfInterest(boolean forNull) {
    HashSet<String> retval = new HashSet<String>();
    retval.add((forNull) ? "nullSource" : "source");    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
   static Source buildFromXML(String elemName, 
                                    Attributes attrs) throws IOException {
    if (!elemName.equals("source") && !elemName.equals("nullSource")) {
      return (null);
    }
    
    String notes = null;
    String proxySpecies = null;
    String proxySign = null;
    String type = null;
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("notes")) {
          notes = val;
        } else if (key.equals("proxySign")) {
          proxySign = val;
        } else if (key.equals("proxy")) {
          proxySpecies = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("type")) {
          type = val;
        }         
      }
    }
    return (new Source(notes, type, proxySpecies, proxySign));
  }
}
