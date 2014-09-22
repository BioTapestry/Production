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
** This handles loading in copies per embryo from XML
*/

public class CopiesPerEmbryoFormatFactory implements ParserClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private HashSet<String> allKeys_;  
  private Set<String> dataKeys_;
  private Set<String> geneKeys_;
  private Object container_;
  private CopiesPerEmbryoData currTarg_;
  private CopiesPerEmbryoGene currGene_;

  private String cpeKey_; 
  private String useCpeKey_;  
  private String cpeTimeKey_; 
  private String cAtTimeKey_;

  private String currCpeMapKey_;
  private ArrayList currCpeMapList_;
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

  public CopiesPerEmbryoFormatFactory(BTState appState, boolean mapsAreIllegal) {
    appState_ = appState;
    
    mapsAreIllegal_ = mapsAreIllegal;    
    dataKeys_ = CopiesPerEmbryoData.keywordsOfInterest();
    geneKeys_ = CopiesPerEmbryoGene.keywordsOfInterest();
    cpeKey_ = CopiesPerEmbryoData.cpeMapKeyword();    
    useCpeKey_ = CopiesPerEmbryoData.useCpeKeyword();
    cpeTimeKey_ = CopiesPerEmbryoData.cpeTimeKeyword();
    cAtTimeKey_ = CopiesPerEmbryoGene.cAtTimeKeyword();
     
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
  ** Get the CopiesPerEmbryoData
  */

  public CopiesPerEmbryoData getCopiesPerEmbryoData() {
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
    if (elemName.equals(cpeKey_)) {
      currTarg_.addCpeMap(currCpeMapKey_, currCpeMapList_);
      currCpeMapKey_ = null;
      currCpeMapList_ = null;
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
  
  public Set keywordsOfInterest() {
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
    
    if (dataKeys_.contains(elemName)) {
      CopiesPerEmbryoData data = CopiesPerEmbryoData.buildFromXML(appState_, elemName, attrs);
      if (data != null) {
        appState_.getDB().setCopiesPerEmbryoData(data);
        currTarg_ = data;
      }
    } else if (geneKeys_.contains(elemName)) {
      CopiesPerEmbryoGene cpeGene = CopiesPerEmbryoGene.buildFromXML(elemName, attrs);
      if (cpeGene != null) {
        currTarg_.addGene(cpeGene);
        currGene_ = cpeGene;
      }
    } else if (elemName.equals(cpeKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currCpeMapKey_ = CopiesPerEmbryoData.extractCpeMapKey(elemName, attrs);
      currCpeMapList_ = new ArrayList();
      
    } else if (elemName.equals(useCpeKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currCpeMapList_.add(CopiesPerEmbryoData.extractUseCpe(elemName, attrs)); 
      
    } else if (elemName.equals(cpeTimeKey_)) {
      int nextTime = CopiesPerEmbryoData.extractTime(elemName, attrs);
      currTarg_.addDefaultTime(nextTime);

    } else if (elemName.equals(cAtTimeKey_)) {
      int time = CopiesPerEmbryoGene.extractCpeTime(elemName, attrs); 
      double count = CopiesPerEmbryoGene.extractCpeCount(elemName, attrs);
      currGene_.addCount(new Integer(time), count);
    }
    return (null);
  }
}
