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

package org.systemsbiology.biotapestry.timeCourse;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.app.BTState;

/****************************************************************************
**
** This handles Temporal Input Range construction from XML input
*/

public class TemporalInputRangeFormatFactory implements ParserClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private HashSet<String> allKeys_;  
  private Set<String> tirdKeys_;
  private Set<String> trKeys_;
  private Set<String> itKeys_;
  private Set<String> rarKeys_;
  @SuppressWarnings("unused")
  private Object container_;
  private TemporalInputRangeData currTarg_;
  private TemporalRange currTemporalRange_;
  private InputTimeRange currInputTimeRange_;
  private String trKey_; 
  private String useTrKey_;  
  private String gmKey_; 
  private String useGroupKey_;
  private String currTrMapKey_;
  private ArrayList<TemporalInputRangeData.TirMapResult> currTrMapList_;
  private String currGroupMapKey_;
  private ArrayList<GroupUsage> currGroupMapList_;
  private boolean mapsAreIllegal_;
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

  public TemporalInputRangeFormatFactory(BTState appState, boolean mapsAreIllegal) {
    appState_ = appState;
    mapsAreIllegal_ = mapsAreIllegal;
    tirdKeys_ = TemporalInputRangeData.keywordsOfInterest();
    trKeys_ = TemporalRange.keywordsOfInterest();    
    itKeys_ = InputTimeRange.keywordsOfInterest();
    rarKeys_ = RegionAndRange.keywordsOfInterest();
    trKey_ = TemporalInputRangeData.trMapKeyword();
    useTrKey_ = TemporalInputRangeData.useTrKeyword();
    gmKey_ = TemporalInputRangeData.groupMapKeyword();
    useGroupKey_ = TemporalInputRangeData.useGroupKeyword();    
    
    allKeys_ = new HashSet<String>();
    allKeys_.addAll(tirdKeys_);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get the TemporalInputRangeData
  */

  public TemporalInputRangeData getTemporalInputRangeData() {
    return (currTarg_);
  }  
  
  /***************************************************************************
  ** 
  ** Set the container
  */

  public void setContainer(Object container) {
    container_ = container;
    return;
  }
  
  /***************************************************************************
  **
  ** Callback for completion of the element
  **
  */
  
  public boolean finishElement(String elemName) {
    if (elemName.equals(trKey_)) {
      currTarg_.addCombinedTemporalInputRangeMaps(currTrMapKey_, currTrMapList_);
      currTrMapList_ = null;
      currTrMapKey_ = null;
    } else if (elemName.equals(gmKey_)) {
      currTarg_.setTemporalRangeGroupMap(currGroupMapKey_, currGroupMapList_);
      currGroupMapList_ = null;
      currGroupMapKey_ = null;
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

    if (tirdKeys_.contains(elemName)) {
      TemporalInputRangeData data = TemporalInputRangeData.buildFromXML(appState_, elemName, attrs);
      if (data != null) {
        appState_.getDB().setTemporalInputRangeData(data);
        currTarg_ = data;
      }
    } else if (trKeys_.contains(elemName)) {
      TemporalRange trange = TemporalRange.buildFromXML(elemName, attrs);
      if (trange != null) {
        currTarg_.addEntry(trange);
        currTemporalRange_ = trange;
      }
    } else if (itKeys_.contains(elemName)) {
      InputTimeRange itr = InputTimeRange.buildFromXML(elemName, attrs);
      if (itr != null) {
        currTemporalRange_.addTimeRange(itr);
        currInputTimeRange_ = itr;
      }
    } else if (rarKeys_.contains(elemName)) {
      RegionAndRange rar = RegionAndRange.buildFromXML(appState_, elemName, attrs);
      if (rar != null) {
        currInputTimeRange_.add(rar);
      }      
    } else if (elemName.equals(trKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currTrMapKey_ = TemporalInputRangeData.extractTrMapKey(elemName, attrs);
      currTrMapList_ = new ArrayList<TemporalInputRangeData.TirMapResult>();
    } else if (elemName.equals(useTrKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currTrMapList_.add(TemporalInputRangeData.extractUseTr(elemName, attrs)); 
    } else if (elemName.equals(gmKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currGroupMapKey_ = TemporalInputRangeData.extractGroupMapKey(elemName, attrs);
      currGroupMapList_ = new ArrayList<GroupUsage>();        
    } else if (elemName.equals(useGroupKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currGroupMapList_.add(TemporalInputRangeData.extractUseGroup(elemName, attrs)); 
    } 
    return (null);
  }
}
