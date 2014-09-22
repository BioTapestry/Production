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

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;


import java.util.SortedSet;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Splitter;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;

/****************************************************************************
**
** This holds multiple perturbation source data
**
*/

public class PertSources implements Comparable<PertSources>, Cloneable, PertFilterTarget {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  public final static int NO_FOOTS      = 0;
  public final static int SUPER_FOOTS   = 1;
  public final static int BRACKET_FOOTS = 2;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList<String> sources_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public PertSources(BTState appState) {
    appState_ = appState;
    sources_ = new ArrayList<String>();
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public PertSources(String sourceID) {
    sources_ = new ArrayList<String>();
    sources_.add(sourceID);
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public PertSources(List<String> sources) {
    sources_ = new ArrayList<String>(sources);
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

  public PertSources clone() {
    try {
      PertSources retval = (PertSources)super.clone();
      // List of immutable string IDs, shallow clone OK:
      retval.sources_ = new ArrayList<String>(this.sources_);
      return (retval);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  } 
  
  /***************************************************************************
  **
  ** Standard hashCode
  **
  */
  
  public int hashCode() {
    int code = 0;
    int numSrc = sources_.size();
    for (int i = 0; i < numSrc; i++) {
      String pskey = sources_.get(i);
      code += pskey.hashCode();
    }
    return (code);
  }
  
  /***************************************************************************
  **
  ** Standard equals:
  **
  */
  
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof PertSources)) {
      return (false);
    }
    PertSources otherPS = (PertSources)other;
    return (this.sources_.equals(otherPS.sources_));
  }
  
  /***************************************************************************
  **
  ** Standard compare
  */  
  
  public int compareTo(PertSources other) {
    PerturbationData pd = appState_.getDB().getPertData();
    String me = this.getDisplayString(pd, NO_FOOTS);
    String him = other.getDisplayString(pd, NO_FOOTS);
    return (me.compareTo(him));
  }

  /***************************************************************************
  **
  ** Answer if we meet the single filtering criteria
  */
  
  public boolean matchesFilter(PertFilter pf, SourceSrc ss) {
    int category = pf.getCategory();
    switch (category) {
      case PertFilter.SOURCE:
        String filterSrc = pf.getStringValue();
        int numSrc = sources_.size();
        for (int i = 0; i < numSrc; i++) {
          String pskey = sources_.get(i);
          if (filterSrc.equals(pskey)) {
            return (true);
          }
        }
        return (false);
      case PertFilter.SOURCE_NAME:
      case PertFilter.SOURCE_OR_PROXY_NAME:
      case PertFilter.ANNOTATION:    
        String filterStr = pf.getStringValue();
        int numSrcN = sources_.size();
        for (int i = 0; i < numSrcN; i++) {
          String pskey = sources_.get(i);
          PertSource ps = ss.getSourceDef(pskey);
          if (category == PertFilter.ANNOTATION) {
            List<String> annotIDs = ps.getAnnotationIDs();
            if ((annotIDs != null) && annotIDs.contains(filterStr)) {
              return (true);
            }
          } else {
            if (filterStr.equals(ps.getSourceNameKey())) {
              return (true);
            }
            if (pf.getCategory() == PertFilter.SOURCE_OR_PROXY_NAME) {
              if (ps.isAProxy() && filterStr.equals(ps.getProxiedSpeciesKey())) {
                return (true);
              }
            }
          }
        }
        return (false);
      case PertFilter.PERT:
        String filterPert= pf.getStringValue();
        numSrc = sources_.size();
        for (int i = 0; i < numSrc; i++) {
          String pskey = sources_.get(i);
          PertSource ps = ss.getSourceDef(pskey);
          if (filterPert.equals(ps.getExpTypeKey())) {
            return (true);
          }
        }
        return (false);
      case PertFilter.TARGET:
      case PertFilter.TIME:
      case PertFilter.INVEST:
      case PertFilter.INVEST_LIST:
      case PertFilter.VALUE:
      case PertFilter.EXP_CONTROL:
      case PertFilter.EXP_CONDITION:
      case PertFilter.MEASURE_SCALE:
        
      case PertFilter.MEASURE_TECH:
        // We do not care about this question:
        return (true);
      case PertFilter.EXPERIMENT:
        // Should not be asked this question
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Add to the set of source
  */
  
  public void addToSourceSet(Set<TrueObjChoiceContent> sources, SourceSrc ss) {   
    int numSrc = sources_.size();
    for (int i = 0; i < numSrc; i++) {
      String pskey = sources_.get(i);
      PertSource ps = ss.getSourceDef(pskey);
      String sName = ps.getDisplayValue(ss);
      sources.add(new TrueObjChoiceContent(sName, pskey));
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Add to the set of source NAMES
  */
  
  public void addToSourceNameSet(Set<TrueObjChoiceContent> sources, SourceSrc ss, boolean orProxy) {   
    int numSrc = sources_.size();
    for (int i = 0; i < numSrc; i++) {
      String pskey = sources_.get(i);
      PertSource ps = ss.getSourceDef(pskey);
      String sNameKey = ps.getSourceNameKey();
      String psName = ps.getSourceName(ss);
      if (orProxy && ps.isAProxy()) {
        sNameKey = ps.getProxiedSpeciesKey();
        psName = ps.getProxiedSpeciesName(ss);
      }
      sources.add(new TrueObjChoiceContent(psName, sNameKey));
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Add to the set of pert types
  */
  
  public void addToPertSet(Set<TrueObjChoiceContent> pertTypes, SourceSrc ss, PertDictionary pDict) {   
    int numSrc = sources_.size();
    for (int i = 0; i < numSrc; i++) {
      String pskey = sources_.get(i);
      PertSource ps = ss.getSourceDef(pskey);
      pertTypes.add(new TrueObjChoiceContent(ps.getExpType(pDict).getType(), ps.getExpTypeKey()));
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Add a source
  */
  
  public void addSource(PertSource ps) {
    sources_.add(ps.getID());
    return;
  }
  
  /***************************************************************************
  **
  ** Add a source ID
  */
  
  public void addSourceID(String psID) {
    sources_.add(psID);
    return;
  }
   
  /***************************************************************************
  **
  ** Delete a source
  */
  
  public void deleteSource(int i) {
    sources_.remove(i);
    return;
  }
  
  /***************************************************************************
  **
  ** Get source count
  */
  
  public int getNumSources() {
    return (sources_.size());
  }
   
  /***************************************************************************
  **
  ** Get a source
  */
  
  public PertSource getSource(int i, SourceSrc ss) {
    String srcID = (String)sources_.get(i);
    PertSource ps = ss.getSourceDef(srcID);
    return (ps);
  }
   
  /***************************************************************************
  **
  ** Return if we are a single perturbation 
  */
  
  public boolean isSinglePert() {
    return (sources_.size() == 1);
  }
  
  /***************************************************************************
  **
  ** Return the single perturbation 
  */
  
  public PertSource getSinglePert(SourceSrc ss) {
    if (sources_.size() != 1) {
      throw new IllegalStateException();
    }
    String srcID = (String)sources_.get(0);
    return (ss.getSourceDef(srcID));
  }
 
  /***************************************************************************
  **
  ** Return an iterator over sources
  */
  
  public Iterator<String> getSources() {
    return (sources_.iterator());
  }
  
  /***************************************************************************
  **
  ** Return if one of our sources matches one of those given
  */
  
  public boolean sourcesMatch(List<String> srcs) {
    int numSrc = sources_.size();
    for (int i = 0; i < numSrc; i++) {
      String srcID = sources_.get(i);
      if (srcs.contains(srcID)) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Add to the Set of annotation IDs used
  **
  */
  
  public void getAnnotationIDs(Set<String> usedIDs, SourceSrc ss) {
    int numSrc = sources_.size();
    for (int i = 0; i < numSrc; i++) {
      String srcID = (String)sources_.get(i);
      PertSource pSrc = ss.getSourceDef(srcID);
      usedIDs.addAll(pSrc.getAnnotationIDs());
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the (possibly mult-pert) display string
  **
  */
  
  public String getDisplayString(SourceSrc ss, int footnoteMode) {
    ArrayList<String> asNames = new ArrayList<String>();
    int numSrc = sources_.size();
    for (int i = 0; i < numSrc; i++) {
      String srcID = sources_.get(i);
      PertSource pSrc = ss.getSourceDef(srcID);
      if (footnoteMode != NO_FOOTS) {
        asNames.add(pSrc.getDisplayValueWithFootnotes(ss, (footnoteMode == SUPER_FOOTS)));
      } else {
        asNames.add(pSrc.getDisplayValue(ss));
      }
    }
    return (DataUtil.getMultiDisplayString(asNames));
  } 
  
  /***************************************************************************
  **
  ** Get the sorted set of perturbations
  **
  */
  
  public SortedSet<String> getPerturbsSortedSet(SourceSrc ss, boolean normalized) {
    TreeSet<String> retval = new TreeSet<String>();
    int numSrc = sources_.size();
    for (int i = 0; i < numSrc; i++) {
      String srcID = (String)sources_.get(i);
      PertSource pSrc = ss.getSourceDef(srcID);
      String disp = pSrc.getDisplayValue(ss);
      if (normalized) {
        disp = DataUtil.normKey(disp);
      }
      retval.add(disp);
    }
    return (retval);
  } 

  /***************************************************************************
  **
  ** Get the sources as a comma-delim list
  **
  */
  
  public String sourcesAsString() {
    return (Splitter.tokenJoin(sources_, ","));
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("PertSources: " + ((sources_.isEmpty()) ? "empty " : sources_.get(0).toString()));
  }
}
