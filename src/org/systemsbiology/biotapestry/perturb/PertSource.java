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

import java.util.List;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.HashSet;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.Splitter;

/****************************************************************************
**
** This holds perturbation source data
**
*/

public class PertSource implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  public static final int NO_PROXY_INDEX         = 0;
  public static final String NO_PROXY            = "noProxy";
  public static final String SAME_SIGN_PROXY     = "sameSign";
  public static final String OPPOSITE_SIGN_PROXY = "oppositeSign";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String id_;
  private String srcNameKey_;
  private String proxyForKey_;
  private String proxySign_;
  private String typeKey_; 
  private ArrayList<String> annotations_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PertSource(String id, String srcNameKey, String typeKey, 
                    String proxyForKey, String proxySign) throws IOException {
    id_ = id;
    srcNameKey_ = srcNameKey;
    annotations_ = new ArrayList<String>();
    if (proxySign == null) {
      proxySign_ = NO_PROXY;
    } else if (!(proxySign.equals(NO_PROXY) || 
                 proxySign.equals(SAME_SIGN_PROXY) ||
                 proxySign.equals(OPPOSITE_SIGN_PROXY))) {
      throw new IOException();
    } else {
      proxySign_ = proxySign;
    }
    
    if (typeKey == null)  {
      throw new IOException();
    } 
    typeKey_ = typeKey;
    
    if ((proxyForKey != null) && proxySign_.equals(NO_PROXY)) { 
      throw new IOException();
    }
    proxyForKey_ = proxyForKey;
  }  
  
  /***************************************************************************
  **
  ** Constructor
  */

  public PertSource(String id, String srcNameKey, String typeKey, List<String> annotations) {
    id_ = id;
    srcNameKey_ = srcNameKey;
    typeKey_ = typeKey;
    proxyForKey_ = null;
    proxySign_ = NO_PROXY;
    annotations_ = new ArrayList<String>(annotations);
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

  public PertSource clone() {
    try {
      PertSource newVal = (PertSource)super.clone();
      newVal.annotations_ = new ArrayList<String>(this.annotations_);
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }  
  }
  
  /***************************************************************************
  **
  ** Get the sign relationship of the source (e.g. SAME_SIGN, OPPOSITE_SIGN)
  */
  
  public PertDictionary.PertLinkRelation getSign(SourceSrc ss) {
    PertDictionary pDict = ss.getPertDictionary();
    PertProperties pprops = getExpType(pDict);
    if (isAProxy()) {
      return (pprops.getRelationWithProxy(getProxySign()));
    } else {
      return (pprops.getLinkSignRelationship());
    }
  }
  
  /***************************************************************************
  **
  ** Compare just source and type
  */
 
  public int compareSrcAndType(PertSource other) {
    int diff = srcNameKey_.compareTo(other.srcNameKey_);
    if (diff != 0) {
      return (diff);
    }
    diff = typeKey_.compareTo(other.typeKey_);
    return (diff);
  }   
  
  /***************************************************************************
  **
  ** Standard hashCode
  **
  */
  
  public int hashCode() {
    int retval = id_.hashCode() + srcNameKey_.hashCode() + typeKey_.hashCode() + proxySign_.hashCode() + annotations_.hashCode();
    retval += (proxyForKey_ == null) ? 0 : proxyForKey_.hashCode();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Standard equals:
  **
  */
  
  public boolean equals(Object other) {
    if (!equalsMinusID(other)) {
      return (false);
    }
    PertSource otherPS = (PertSource)other;
    return (this.id_.equals(otherPS.id_));
  }
  
  /***************************************************************************
  **
  ** Standard equals modulo ID
  **
  */
  
  public boolean equalsMinusID(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof PertSource)) {
      return (false);
    }
    PertSource otherPS = (PertSource)other;
    
 
    if (!this.srcNameKey_.equals(otherPS.srcNameKey_)) {
      return (false);
    }
    
    if (!this.typeKey_.equals(otherPS.typeKey_)) {
      return (false);
    }
    
    if (!this.proxySign_.equals(otherPS.proxySign_)) {    
      return (false);
    }
  
    if (!this.proxySign_.equals(NO_PROXY)) {
      if (!this.proxyForKey_.equals(otherPS.proxyForKey_)) {    
        return (false);
      }
    }
  
    // Make equivalence independent of list ordering:
    
    return (new HashSet<String>(this.annotations_).equals(new HashSet<String>(otherPS.annotations_)));    
  }
  
  /***************************************************************************
  **
  ** set the ID
  */
  
  public void setID(String id) {
    id_ = id;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the ID
  */
  
  public String getID() {
    return (id_);
  }  

  /***************************************************************************
  **
  ** Get the list of annotation IDs used
  **
  */
  
  public List<String> getAnnotationIDs() {
    return (annotations_);
  } 
  
  /***************************************************************************
  **
  ** set the list of annotation IDs used
  **
  */
  
  public void setAnnotationIDs(List<String> annots) {
    annotations_.clear();
    annotations_.addAll(annots);
    return;
  } 
  
  /***************************************************************************
  **
  ** Return experiment type 
  */
  
  public PertProperties getExpType(PertDictionary pd) {
    return (pd.getPerturbProps(typeKey_));
  }
  
  /***************************************************************************
  **
  ** Get experiment type key
  */
  
  public String getExpTypeKey() {
    return (typeKey_);
  }  
 
  /***************************************************************************
  **
  ** Set experiment type key
  */
  
  public void setExpType(String key) {
    typeKey_ = key;
    return;
  }  
   
  /***************************************************************************
  **
  ** Get the source Name
  */
  
  public String getSourceName(SourceSrc ss) {
    return (ss.getSourceName(srcNameKey_));
  } 
 
  /***************************************************************************
  **
  ** Get the source Key
  */
  
  public String getSourceNameKey() {
    return (srcNameKey_);
  } 
  
  /***************************************************************************
  **
  ** Set the source
  */
  
  public void setSourceNameKey(String srcNameKey) {
    srcNameKey_ = srcNameKey;
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the value
  */
  
  public String getDisplayValue(SourceSrc ss) {
    StringBuffer buf = new StringBuffer();
    PertDictionary pd = ss.getPertDictionary();
    PertProperties pp = pd.getPerturbProps(typeKey_);
    String name = ss.getSourceName(srcNameKey_);
    buf.append(name);
    String useMe = pp.getAbbrev();
    if (useMe == null) {
      buf.append(" ");
      buf.append(pp.getType());
    } else {
      buf.append(useMe);
    }  
    return (buf.toString());
  }
  
  /***************************************************************************
  **
  ** Get the value with a notes as bracket or superscript
  */
  
  public String getDisplayValueWithFootnotes(SourceSrc ss, boolean asSuper) {
    PertAnnotations pa = ss.getPertAnnotations(); 
    StringBuffer buf = new StringBuffer();
    buf.append(getDisplayValue(ss));    
    if (!annotations_.isEmpty()) {
      buf.append((asSuper) ? "<sup>" : " [");
      buf.append(pa.getFootnoteListAsString(annotations_));
      buf.append((asSuper) ? "</sup>" : "]");
    } 
    return (buf.toString());
  }  
  
  /***************************************************************************
  **
  ** Set the notes
  */
  
  public void setNotes(List<String> notes) {
    annotations_.clear();
    annotations_.addAll(notes);
    return;
  }  
  
  /***************************************************************************
  **
  ** add a note
  */
  
  public void addNote(String note) {
    annotations_.add(note);
    return;
  }  
  
  /***************************************************************************
  **
  ** Delete a note
  */
  
  public void deleteNote(String noteID) {
    annotations_.remove(noteID);
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the proxied species key
  */
  
  public String getProxiedSpeciesKey() {
    return (proxyForKey_);
  }
  
  /***************************************************************************
  **
  ** Get the proxied Name
  */
  
  public String getProxiedSpeciesName(SourceSrc ss) {
    return (ss.getSourceName(proxyForKey_));
  } 

  /***************************************************************************
  **
  ** Set the proxied species
  */
  
  public void setProxiedSpeciesKey(String speciesKey) {
    proxyForKey_ = speciesKey;
    return;
  } 
  
  /***************************************************************************
  **
  ** Answer if we are a proxy
  */
  
  public boolean isAProxy() {
    return (!proxySign_.equals(PertSource.NO_PROXY));
  } 
  
  /***************************************************************************
  **
  ** Get the proxy sign
  */
  
  public String getProxySign() {
    return (proxySign_);
  } 
  
  /***************************************************************************
  **
  ** Set the proxy sign
  */
  
  public void setProxySign(String proxySign) {
    proxySign_ = proxySign;
    return;
  }   
 
  /***************************************************************************
  **
  ** Write the source to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<pertSrc id=\"");
    out.print(id_);
    out.print("\" src=\"");
    out.print(srcNameKey_);
    out.print("\" type=\"");
    out.print(typeKey_);
    out.print("\""); 
    if (!proxySign_.equals(NO_PROXY)) {
      out.print(" proxy=\"");
      out.print(proxyForKey_);
      out.print("\"");
      out.print(" proxySign=\"");
      out.print(proxySign_);
      out.print("\"");      
    }
    if (!annotations_.isEmpty()) {
      String notesStr = Splitter.tokenJoin(annotations_, ",");
      out.print(" notes=\"");
      out.print(notesStr);
      out.print("\"");    
    }
    out.println("/>");
    return;
  }

  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("PertSource: id = " + id_ + " name = " +  srcNameKey_ + " typekey = " + typeKey_);
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Gets the valid measurement types in a list
  */
  
  public static List<ObjChoiceContent> getProxySignValues(BTState appState) { 
    ArrayList<ObjChoiceContent> retval = new ArrayList<ObjChoiceContent>();
    retval.add(getProxySignValue(appState, NO_PROXY));
    retval.add(getProxySignValue(appState, SAME_SIGN_PROXY));
    retval.add(getProxySignValue(appState, OPPOSITE_SIGN_PROXY));
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Gets a valid measurement types
  */
  
  public static ObjChoiceContent getProxySignValue(BTState appState, String proxVal) { 
    return (new ObjChoiceContent(appState.getRMan().getString("proxyTypes." + proxVal), proxVal));
  }

  /***************************************************************************
  **
  ** Gets the valid proxy sign values
  */
  
  public static String mapProxySignIndex(int index) {
    switch (index) {
      case 0:
        return (NO_PROXY);
      case 1:
        return (SAME_SIGN_PROXY);
      case 2:
        return (OPPOSITE_SIGN_PROXY);
      default:
        throw new IllegalArgumentException();
    }
  }

  /***************************************************************************
  **
  ** Legacy map is actually the identity:
  */
  
  public static String mapLegacyProxySign(String legacy) {
    return (legacy);
  }          
          
  /***************************************************************************
  **
  ** For XML I/O
  ** 
  */ 
      
  public static class PertSourceWorker extends AbstractFactoryClient {
    
    public PertSourceWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("pertSrc");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("pertSrc")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.pertSrc = buildFromXML(elemName, attrs);
        retval = board.pertSrc;
      }
      return (retval);     
    }  
        
    private PertSource buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "pertSrc", "id", true);
      
      String src = AttributeExtractor.extractAttribute(elemName, attrs, "pertSrc", "src", true);   
    
      String type = AttributeExtractor.extractAttribute(elemName, attrs, "pertSrc", "type", true);
      type = CharacterEntityMapper.unmapEntities(type, false);
      
      String proxyKey = AttributeExtractor.extractAttribute(elemName, attrs, "pertSrc", "proxy", false);
     
      String proxySign = AttributeExtractor.extractAttribute(elemName, attrs, "pertSrc", "proxySign", false);
      if ((proxySign == null) && (proxyKey != null)) {
        throw new IOException();
      }
      
      String notes = AttributeExtractor.extractAttribute(elemName, attrs, "pertSrc", "notes", false);
    
      PertSource nps = new PertSource(id, src, type, proxyKey, proxySign);
      if (notes != null) {
        nps.setNotes(Splitter.stringBreak(notes, ",", 0, false));
      }
  
      return (nps);
    } 
  }
}
