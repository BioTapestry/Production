/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.timeCourse;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.app.BTState;

/****************************************************************************
**
** This handles loading in time course from XML
*/

public class TimeCourseFormatFactory implements ParserClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private HashSet<String> allKeys_;  
  private Set<String> dataKeys_;
  private String geneKey_;
  private Set<String> exprKeys_;
  private Set<String> pertGeneKeys_;
  private TimeCourseData currTarg_;
  private TimeCourseGene currGene_;
  private List<GeneTemplateEntry> mustMatch_;
  private PerturbedTimeCourseGene currPertGene_;
  private boolean currGeneIsTemplate_;
  private TimeCourseData.RegionTopology currRegTopo_;
  private TimeCourseData.TopoTimeRange currLocRange_;
  private String tcKey_; 
  private String useTcKey_;  
  private String gmKey_; 
  private String useGroupKey_;
  private String currTcMapKey_;
  private String rootRegionKey_;
  private String regionParentKey_;  
  private String regionHierarchyKey_;
  private String regionTopologyKey_;      
  private String topoRegionKey_;     
  private String topoLinkKey_;     
  private String topoLocationsForRangeKey_;    
  private String topoRegionLocationKey_;
  private String ctrlExpKey_;
  private boolean currExpIsCtrl_;
  
  private HashSet<String> currRootRegionSet_;  
  private HashMap<String, String> currRegionParentMap_;
  private ArrayList<TimeCourseData.TCMapping> currTcMapList_;
  private String currGroupMapKey_;
  private ArrayList<GroupUsage> currGroupMapList_;
  private boolean mapsAreIllegal_;
  private boolean serialNumberIsIllegal_;
  private long origSerialNumber_;
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

  public TimeCourseFormatFactory(BTState appState, boolean mapsAreIllegal, boolean serialNumberIsIllegal) {
    
    appState_ = appState;
    mapsAreIllegal_ = mapsAreIllegal;
    serialNumberIsIllegal_ = serialNumberIsIllegal;
            
    dataKeys_ = TimeCourseData.keywordsOfInterest();
    geneKey_ = TimeCourseGene.keywordOfInterest();
    exprKeys_ = ExpressionEntry.keywordsOfInterest();
    pertGeneKeys_ = PerturbedTimeCourseGene.keywordsOfInterest();
    ctrlExpKey_ = PerturbedTimeCourseGene.keyForControlCollection();
 
    tcKey_ = TimeCourseData.tcMapKeyword();
    useTcKey_ = TimeCourseData.useTcKeyword();
    gmKey_ = TimeCourseData.groupMapKeyword();
    useGroupKey_ = TimeCourseData.useGroupKeyword();
    regionHierarchyKey_ = TimeCourseData.regionHierarchyKeyword();    
    rootRegionKey_ = TimeCourseData.rootRegionKeyword();
    regionParentKey_ = TimeCourseData.regionParentKeyword();
    
    regionTopologyKey_ = TimeCourseData.regionTopologyKeyword();      
    topoRegionKey_ = TimeCourseData.topoRegionKeyword();     
    topoLinkKey_ = TimeCourseData.topoLinkKeyword();     
    topoLocationsForRangeKey_ = TimeCourseData.topoLocationsForRangeKeyword();    
    topoRegionLocationKey_ = TimeCourseData.topoRegionLocationKeyword();  

    allKeys_ = new HashSet<String>();
    allKeys_.addAll(dataKeys_);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get the TimeCourseData
  */

  public TimeCourseData getTimeCourseData() {
    return (currTarg_);
  }  
  
  /***************************************************************************
  ** 
  ** Set the container
  */

  public void setContainer(Object container) {
    return;
  }
  
  /***************************************************************************
  **
  ** Callback for completion of the element
  **
  */
  
  public boolean finishElement(String elemName) throws IOException {
    if (elemName.equals(tcKey_)) {
      currTarg_.addTimeCourseTCMMap(currTcMapKey_, currTcMapList_, false);
      currTcMapList_ = null;
      currTcMapKey_ = null;
    } else if (elemName.equals(gmKey_)) {
      currTarg_.setTimeCourseGroupMap(currGroupMapKey_, currGroupMapList_, false);
      currGroupMapList_ = null;
      currGroupMapKey_ = null;
    } else if (elemName.equals(regionHierarchyKey_)) {
      currTarg_.setRegionHierarchy(currRegionParentMap_, currRootRegionSet_, false);  
      currRootRegionSet_ = null;  
      currRegionParentMap_ = null;    
    } else if (elemName.equals(topoLocationsForRangeKey_)) {
      currLocRange_ = null;
    } else if (elemName.equals(ctrlExpKey_)) {
      currExpIsCtrl_ = false;
    } else if (geneKey_.equals(elemName)) {
      //
      // Fix for Issue #169
      //
      List<GeneTemplateEntry> template;
      if (currGeneIsTemplate_) {
        template = new ArrayList<GeneTemplateEntry>();
        Iterator<GeneTemplateEntry> gtei = currTarg_.getGeneTemplate();
        while (gtei.hasNext()) {
          template.add(gtei.next());
        }
      } else {
        template = currGene_.toTemplate();
      }
      if (template.isEmpty()) {
        throw new IOException(appState_.getRMan().getString("timeCourseImport.emptyEntry"));
      } else if (mustMatch_ == null) {
        mustMatch_ = template;
      } else {
        if (!mustMatch_.equals(template)) {
          throw new IOException(appState_.getRMan().getString("timeCourseImport.entryMismatch"));
        }
      }
    }
    return (allKeys_.contains(elemName));
  }
  
  /***************************************************************************
  ** 
  ** Handle incoming characters
  */

  public void processCharacters(char[] chars, int start, int length) {
    return;
  }
    
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public Set<String> keywordsOfInterest() {
    return (allKeys_);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public Object processElement(String elemName, Attributes attrs) throws IOException {

    if ((attrs == null) || (elemName == null)) {
      return (null);
    }
    
    if ((currTarg_ != null)  && (currTarg_.getSerialNumber() != origSerialNumber_)) {
      throw new IOException();
    }
    
    if (dataKeys_.contains(elemName)) {
      TimeCourseData data = TimeCourseData.buildFromXML(appState_, elemName, attrs, serialNumberIsIllegal_);
      if (data != null) {
        appState_.getDB().setTimeCourseData(data);
        currTarg_ = data;
        origSerialNumber_ = data.getSerialNumber();
      }
    } else if (geneKey_.equals(elemName)) {
      currPertGene_ = null;
      TimeCourseGene tgene = TimeCourseGene.buildFromXML(appState_, elemName, attrs);
      if (tgene != null) {
        if (tgene.isTemplate()) {
          currGeneIsTemplate_ = true;
        } else {
          currGeneIsTemplate_ = false;
          currTarg_.addGene(tgene);
          currGene_ = tgene;
        }    
      }
    } else if (pertGeneKeys_.contains(elemName)) {
      PerturbedTimeCourseGene ptcg = PerturbedTimeCourseGene.buildFromXML(appState_, elemName, attrs);
      if (ptcg != null) {
        if (currGeneIsTemplate_) {
          throw new IOException();
        } else {
          currPertGene_ = ptcg;
          currGene_.setPerturbedState(ptcg.getPertSources(), ptcg);
        }       
      }
    } else if (exprKeys_.contains(elemName)) {
      ExpressionEntry ee = ExpressionEntry.buildFromXML(appState_, elemName, attrs);
      if (ee != null) {
        if (currGeneIsTemplate_) {
          GeneTemplateEntry gte = new GeneTemplateEntry(ee.getTime(), ee.getRegion());
          try {
            currTarg_.addTemplateEntry(gte);
          } catch (IllegalArgumentException iaex) {
            throw new IOException();
          }
        } else if (currPertGene_ != null) {
          if (currExpIsCtrl_) {
            currPertGene_.addCtrlExpression(ee);
          } else {
            currPertGene_.addExpression(ee, null);
          }
        } else {
          currGene_.addExpression(ee);
        }
      }
    } else if (elemName.equals(tcKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currTcMapKey_ = TimeCourseData.extractTcMapKey(elemName, attrs);
      currTcMapList_ = new ArrayList<TimeCourseData.TCMapping>();        
    } else if (elemName.equals(useTcKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currTcMapList_.add(TimeCourseData.extractTCMapping(elemName, attrs));        
    } else if (elemName.equals(gmKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currGroupMapKey_ = TimeCourseData.extractGroupMapKey(elemName, attrs);
      currGroupMapList_ = new ArrayList<GroupUsage>();
    } else if (elemName.equals(useGroupKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currGroupMapList_.add(TimeCourseData.extractUseGroup(elemName, attrs));
    } else if (elemName.equals(regionHierarchyKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currRootRegionSet_ = new HashSet<String>();  
      currRegionParentMap_ = new HashMap<String, String>();
    } else if (elemName.equals(rootRegionKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currRootRegionSet_.add(TimeCourseData.extractRootRegion(elemName, attrs));
    } else if (elemName.equals(regionParentKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      TimeCourseData.extractRegionParent(elemName, attrs, currRegionParentMap_);
    } else if (elemName.equals(regionTopologyKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      TimeCourseData.TopoTimeRange newRange = TimeCourseData.TopoTimeRange.buildFromXML(appState_, elemName, attrs);
      currRegTopo_ = new TimeCourseData.RegionTopology(newRange);
      currTarg_.setRegionTopology(newRange, currRegTopo_);
    } else if (elemName.equals(topoRegionKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      String regName = TimeCourseData.RegionTopology.buildRegionFromXML(elemName, attrs);
      currRegTopo_.addRegion(regName);
    } else if (elemName.equals(topoLinkKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      TimeCourseData.TopoLink newLink = TimeCourseData.TopoLink.buildFromXML(elemName, attrs);
      currRegTopo_.addLink(newLink);
    } else if (elemName.equals(topoLocationsForRangeKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currTarg_.prepareRegionTopologyLocatorForInput();
      currLocRange_ = TimeCourseData.TopoTimeRange.buildFromXML(appState_, elemName, attrs); 
    } else if (elemName.equals(topoRegionLocationKey_)) {
      if (currLocRange_ == null) {
        throw new IOException();
      }
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      TimeCourseData.TopoRegionLoc newLoc = TimeCourseData.TopoRegionLoc.buildFromXML(elemName, attrs);
      currTarg_.getRegionTopologyLocator().setRegionTopologyLocation(currLocRange_, newLoc);
    } else if (elemName.equals(ctrlExpKey_)) {
      currPertGene_.setForDistinctControlExpr();
      currExpIsCtrl_ = true;
    }
    return (null);
  }
}
