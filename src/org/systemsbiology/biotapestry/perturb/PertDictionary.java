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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UniqueLabeller;

/****************************************************************************
**
** This is the collection of possible experiment perturbation types
**
*/

public class PertDictionary implements Cloneable {
  
   /***************************************************************************
   **
   ** Pert to Link sign relations
   */ 
   
   public enum PertLinkRelation {
     PERT_POS_LINK_POS("pertPosLinkPos", false),
     PERT_NEG_LINK_POS("pertNegLinkPos", false),
     PERT_ALWAYS_NEGATIVE("pertAlwaysNeg", true),
     PERT_ALWAYS_POSITIVE("pertAlwaysPos", true),
     PERT_LINK_NO_RELATION("pertLinkNoRelation", true),
     PERT_LINK_OTHER("pertLinkOther", true);
     
     private String tag_;
     private boolean undet_;
     
     PertLinkRelation(String tag, boolean signUndet) {
       this.tag_ = tag;  
       this.undet_ = signUndet;  
     }
     
     public static PertLinkRelation fromTag(String tag) {
       for (PertLinkRelation lr: values()) {
         if (tag.equals(lr.tag_)) {
           return (lr);
         }
       }
       throw new IllegalArgumentException();
     }
      
     public String getTag() {
       return (tag_);      
     }
     
     public String getDisplayTag(BTState appState) {
       return (appState.getRMan().getString("pertDict.relation" + tag_));      
     }
        
     public boolean linkSignUndetermined() {
       return (undet_);      
     }
         
     public EnumChoiceContent<PertLinkRelation> generateCombo(BTState appState) {
       return (new EnumChoiceContent<PertLinkRelation>(getDisplayTag(appState), this));
     }  
      
     public static Vector<EnumChoiceContent<PertLinkRelation>> getLinkRelationshipOptions(BTState appState) {
       Vector<EnumChoiceContent<PertLinkRelation>> retval = new Vector<EnumChoiceContent<PertLinkRelation>>();
       for (PertLinkRelation lr: values()) {
         retval.add(lr.generateCombo(appState));    
       }
       return (retval);
     }
   }
   
  // MUST DIE!!!
  
  public static final int MASSIVE_HACKO_LEGACY_MASO_TYPE = 0;
  public static final int MASSIVE_HACKO_LEGACY_MOE_TYPE = 1;
  public static final int MASSIVE_HACKO_LEGACY_ENGRAILED_TYPE = 2;
   
  public static final int LEGACY_PERTURB_TYPES = 3;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static final String MASO_TYPE_        = "MASO";
  private static final String MOE_TYPE_         = "MOE";
  private static final String ENGRAILED_TYPE_   = "Engrailed";
  private static final String ENGRAILED_ABBREV_ = "-En"; 
  private static final String ENGRAILED_ALT_    = "EN"; 
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private HashMap<String, PertProperties> perts_;
  private UniqueLabeller labels_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public PertDictionary() {
    perts_ = new HashMap<String, PertProperties>();
    labels_ = new UniqueLabeller();
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
  ** Clone
  */

  public PertDictionary clone() {
    try {
      PertDictionary newVal = (PertDictionary)super.clone();
      
      newVal.perts_ = new HashMap<String, PertProperties>();
      Iterator<String> psit = this.perts_.keySet().iterator();
      while (psit.hasNext()) {
        String psKey = psit.next();
        newVal.perts_.put(psKey, (this.perts_.get(psKey)).clone());
      }
      newVal.labels_ = this.labels_.clone();
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }  
  
  /***************************************************************************
  **
  ** Fill in ALL legacy perturb props:
  */
  
  public void createAllLegacyPerturbProps() {  
    createLegacyPerturbProp(MASO_TYPE_);
    createLegacyPerturbProp(MOE_TYPE_);
    createLegacyPerturbProp(ENGRAILED_TYPE_);
    return;
  }
   
  /***************************************************************************
  **
  ** Get an iterator over the keys
  */

  public Iterator<String> getKeys() {
    TreeSet<String> ordered = new TreeSet<String>(perts_.keySet());
    Iterator<String> oit = ordered.iterator();
    return (oit);
  }

  /***************************************************************************
  **
  ** Get the count of perturb properties:
  */
  
  public int getPerturbPropsCount() {
    return (perts_.size());
  }  
  
  /***************************************************************************
  **
  ** Get the specified perturb properties:
  */
  
  public PertProperties getPerturbProps(String key) {
    return (perts_.get(key));
  }
  
  /***************************************************************************
  **
  ** Get all the perturb prop names
  */
  
  public Set<String> getPerturbPropNameSet() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<PertProperties> ckit = perts_.values().iterator();
    while (ckit.hasNext()) {
      PertProperties ec = ckit.next();
      retval.add(ec.getType());        
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Set a perturb props:
  */
  
  public void setPerturbProp(PertProperties pProps) {
    perts_.put(pProps.getID(), pProps);
    return;
  }
  
  /***************************************************************************
  **
  ** Set a perturb props:
  */
  
  public void addPerturbPropForIO(PertProperties pProps) {
    String id = pProps.getID();
    labels_.addExistingLabel(id);
    perts_.put(id, pProps);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop the prop with the given key:
  */
  
  public void dropPerturbProp(String key) {
    perts_.remove(key);
    labels_.removeLabel(key);
    return;
  }
  
  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  public String getPerturbPropsFromName(String legacyStr) {
    Iterator<String> pit = perts_.keySet().iterator();
    while (pit.hasNext()) {
      String key = pit.next();
      PertProperties pp = perts_.get(key);
      if (DataUtil.keysEqual(pp.getType(), legacyStr)) {
        return (key);
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
    out.println("<pertDictionary>");     
    ind.up();    
    TreeSet<String> ordered = new TreeSet<String>(perts_.keySet());
    Iterator<String> oit = ordered.iterator();
    while (oit.hasNext()) {
      String key = oit.next();
      PertProperties pp = perts_.get(key);
      pp.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</pertDictionary>");
    return;
  }
  
  /***************************************************************************
  **
  ** Fill in legacy perturb props:
  */

  public String createLegacyPerturbProp(String expType) {   
    PertProperties legPP;
    String newID = labels_.getNextLabel();
    if (expType.equalsIgnoreCase(MASO_TYPE_)) {
      legPP = new PertProperties(newID, MASO_TYPE_, null, PertLinkRelation.PERT_NEG_LINK_POS);
    } else if (expType.equalsIgnoreCase(MOE_TYPE_)) {
      legPP = new PertProperties(newID, MOE_TYPE_, null, PertLinkRelation.PERT_POS_LINK_POS);
    } else if (expType.equalsIgnoreCase(ENGRAILED_TYPE_)) {
      legPP = new PertProperties(newID, ENGRAILED_TYPE_, ENGRAILED_ABBREV_, PertLinkRelation.PERT_ALWAYS_NEGATIVE);
      legPP.setLegacyAlt(ENGRAILED_ALT_);
    } else {
      throw new IllegalArgumentException();
    } 
    perts_.put(newID, legPP);
    return (newID);
  }
 
  /***************************************************************************
  **
  ** Get rid of this ASAP:
  */
  
  public String getLegacyKeyMassiveHacko(int legacyType) {
    String lookFor;
    if (legacyType == MASSIVE_HACKO_LEGACY_MASO_TYPE) {
       lookFor = MASO_TYPE_;
    } else if (legacyType == MASSIVE_HACKO_LEGACY_MOE_TYPE) {
      lookFor = MOE_TYPE_; 
    } else if (legacyType == MASSIVE_HACKO_LEGACY_ENGRAILED_TYPE) {
      lookFor = ENGRAILED_TYPE_;
    } else {
      throw new IllegalArgumentException();
    }
    Iterator<String> pkit = perts_.keySet().iterator();
    while (pkit.hasNext()) {
      String key = pkit.next();
      PertProperties pp = perts_.get(key);
      if (pp.getType().equals(lookFor)) {
        return (key);
      }
    }
    throw new IllegalStateException();
  }  

  /***************************************************************************
  **
  ** Get all nv pair info:
  */    
  
  public void getNVPairs(Map<String, Set<String>> nvPairs, Set<String> allNames, Set<String> allVals) {
    
    Iterator<String> pkit = perts_.keySet().iterator();
    while (pkit.hasNext()) {
      String key = pkit.next();
      PertProperties pp = perts_.get(key);
      Iterator<String> kit = pp.getNvpKeys();
      while (kit.hasNext()) {
        String name = kit.next();
        String value = pp.getValue(name);
        String normName = DataUtil.normKey(name);
        Set<String> valsForName = nvPairs.get(normName);
        if (valsForName == null) {
          valsForName = new HashSet<String>();
          nvPairs.put(normName, valsForName);
        }
        allNames.add(name);
        allVals.add(value);
        valsForName.add(value);
      }
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Gets the valid experiment types in a vector
  */
  
  public Vector<TrueObjChoiceContent> getExperimentTypes() { 
    TreeSet<TrueObjChoiceContent> sorted = new TreeSet<TrueObjChoiceContent>();
    Iterator<String> oit = perts_.keySet().iterator();
    while (oit.hasNext()) {
      String key = oit.next();
      sorted.add(getExperimentTypeChoice(key));
    }
    return (new Vector<TrueObjChoiceContent>(sorted));
  } 
   
  /***************************************************************************
  **
  ** Gets the choice for a pert key
  */
  
  public TrueObjChoiceContent getExperimentTypeChoice(String pertKey) { 
    PertProperties pp = perts_.get(pertKey);
    return (new TrueObjChoiceContent(pp.getType(), pertKey));
  } 
 
  /***************************************************************************
  **
  ** For XML I/O
  ** 
  */ 
      
  public static class PertDictionaryWorker extends AbstractFactoryClient {
    
    public PertDictionaryWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("pertDictionary");
      installWorker(new PertProperties.PertPropsWorker(whiteboard), new MyPropsGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("pertDictionary")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.pDict = buildFromXML(elemName, attrs);
        retval = board.pDict;
      }
      return (retval);     
    }  
        
    private PertDictionary buildFromXML(String elemName, Attributes attrs) throws IOException {
      PertDictionary pDict = new PertDictionary();
      return (pDict);
    } 
  }
  
  public static class MyPropsGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      PertDictionary pDict = board.pDict;
      PertProperties pProps = board.pertProps;
      pDict.addPerturbPropForIO(pProps);
      return (null);
    }
  } 
}
