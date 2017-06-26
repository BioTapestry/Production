/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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
import java.util.ArrayList;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.qpcr.QPCRData;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.MinMax;

/****************************************************************************
**
** This handles QPCR formatting
*/

public class QpcrXmlFormatFactory implements ParserClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  private static final int NO_TARGET_     = 0;
  private static final int NAME_TARGET_   = 1; 
  private static final int SOURCE_TARGET_ = 2;
  private static final int FOOT_TARGET_   = 3;
  private static final int PERT_TARGET_   = 4;
  private static final int COLUMN_TARGET_ = 5;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private HashSet<String> allKeys_;  
  private Set<String> qpcrKeys_;
  private Set<String> targKeys_;
  private Set<String> nullPertKeys_;
  private Set<String> nullTargKeys_;
  private Set<String> nullSpanKeys_;  
  private Set<String> footKeys_;
  private Set<String> pertKeys_;
  private Set<String> batchKeys_;
  private Set<String> measKeys_;
  private Set<String> spanKeys_;
  private String regionRestrictKey_;
  private Set<String> sourceKeys_;
  private Set<String> nullSourceKeys_;
  private Set<String> defaultNullSpanKeys_;
  @SuppressWarnings("unused")
  private Object container_;
  private TargetGene currTarg_;
  private Perturbation currPerturb_;
  private Batch currBatch_; 
  private Source currSource_; 
  private TimeSpan currSpan_;
  private Footnote currFoot_;
  private NullPerturb currNPert_;
  private NullTarget currNullTarg_;  
  private String currMapKey_;
  private ArrayList<QPCRData.QpcrMapResult> currMapList_;  
  private String nameKey_;
  private String colKey_; 
  private String colRangeKey_;   
  private String dmKey_; 
  private String useKey_;   
  private int charTarg_;
  
  private QPCRData qpcr_;
  
  private StringBuffer buf_;
  private boolean mapsAreIllegal_;
  private boolean serialNumberIsIllegal_;
  private NullTimeSpan pendingDefaultNullTimeSpan_;
  private long origSerialNumber_;
  
  private DynamicDataAccessContext ddacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

   public QpcrXmlFormatFactory(boolean mapsAreIllegal, boolean serialNumberIsIllegal) {
    
    mapsAreIllegal_ = mapsAreIllegal;
    serialNumberIsIllegal_ = serialNumberIsIllegal;
    allKeys_ = new HashSet<String>();
    
    qpcrKeys_ = QPCRData.keywordsOfInterest();
    targKeys_ = TargetGene.keywordsOfInterest();
    nullPertKeys_ = NullPerturb.keywordsOfInterest();
    footKeys_ = Footnote.keywordsOfInterest();
    pertKeys_ = Perturbation.keywordsOfInterest();
    batchKeys_ = Batch.keywordsOfInterest();
    measKeys_ = Measurement.keywordsOfInterest();
    spanKeys_ = TimeSpan.keywordsOfInterest();
    regionRestrictKey_ = TimeSpan.regionRestrictKeyword();    
    sourceKeys_ = Source.keywordsOfInterest(false);
    nullSourceKeys_ = Source.keywordsOfInterest(true);
    nameKey_ = Perturbation.nameKeyword();
    colKey_ = QPCRData.columnKeyword();  // legacy use only
    colRangeKey_ = QPCRData.columnRangeKeyword();    
    dmKey_ = QPCRData.datamapKeyword();
    useKey_ = QPCRData.useqpcrKeyword();
    nullTargKeys_ = NullTarget.keywordsOfInterest();
    nullSpanKeys_ = NullTimeSpan.keywordsOfInterest(false);    
    defaultNullSpanKeys_ = NullTimeSpan.keywordsOfInterest(true);
    allKeys_ = new HashSet<String>();
    allKeys_.addAll(qpcrKeys_);
  
    buf_ = new StringBuffer();
    charTarg_ = NO_TARGET_;
    pendingDefaultNullTimeSpan_ = null;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Set the data access context
  */
 
  public void setContext(DynamicDataAccessContext ddacx) {
    ddacx_ = ddacx;
    return;
  }

  /***************************************************************************
  ** 
  ** Get the QPCR
  */

   QPCRData getQPCR() {
    return (qpcr_);
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
  
   public boolean finishElement(String elemName) throws IOException {
    
    if ((qpcr_ != null)  && (qpcr_.getSerialNumber() != origSerialNumber_)) {
      throw new IOException();
    }
    
    if (elemName.equals("i")) {
      buf_.append("</i>");
    } else if (elemName.equals("sup")) {
      buf_.append("</sup>");      
    } else if (elemName.equals("sub")) {
      buf_.append("</sub>"); 
    } else if (elemName.equals(dmKey_)) {
      qpcr_.addCombinedDataMaps(currMapKey_, currMapList_);
      currMapList_ = null;
    } else {
      switch (charTarg_) {
        case NAME_TARGET_:
          currPerturb_.addInvestigator(CharacterEntityMapper.unmapEntities(buf_.toString(), false));
          charTarg_ = NO_TARGET_;
          break;
        case SOURCE_TARGET_:
          currSource_.setBaseValue(CharacterEntityMapper.unmapEntities(buf_.toString(), false));
          charTarg_ = NO_TARGET_;          
          break;
        case FOOT_TARGET_:
          currFoot_.setNote(buf_.toString());
          //currFoot_.setNote(CharacterEntityMapper.unmapEntitiesNotTags(buf_.toString(), false));
          charTarg_ = NO_TARGET_;          
          break;
        case PERT_TARGET_:
          currNPert_.setNote(buf_.toString(), qpcr_, ddacx_.getExpDataSrc().getTimeAxisDefinition());
          //currNPert_.setNote(CharacterEntityMapper.unmapEntitiesNotTags(buf_.toString(), false));
          charTarg_ = NO_TARGET_;          
          break;
        case COLUMN_TARGET_: // LEGACY USE ONLY!!!
          String colStr = CharacterEntityMapper.unmapEntities(buf_.toString(), false);
          int min = QPCRData.getMinimum(colStr);
          int max = QPCRData.getMaximum(colStr);          
          qpcr_.addColumn(new MinMax(min, max));
          charTarg_ = NO_TARGET_;          
          break;          
        case NO_TARGET_:
        default:
          break;
      }
    }    
    return (allKeys_.contains(elemName));
  }
  
  /***************************************************************************
  ** 
  ** Handle incoming characters
  */

   public void processCharacters(char[] chars, int start, int length) {
    String nextString = new String(chars, start, length);
    buf_.append(nextString);
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
    
    if ((qpcr_ != null)  && (qpcr_.getSerialNumber() != origSerialNumber_)) {
      throw new IOException();
    }
    
    if (qpcrKeys_.contains(elemName)) {
      QPCRData qpcr = QPCRData.buildFromXML(elemName, attrs, serialNumberIsIllegal_);
      if (qpcr != null) {
        ddacx_.getExpDataSrc().getPertData().setLegacyQPCR(new QpcrLegacyPublicExposed(qpcr));
        qpcr_ = qpcr;
        origSerialNumber_ = qpcr_.getSerialNumber();
      }
    } else if (targKeys_.contains(elemName)) {
      TargetGene tgene = TargetGene.buildFromXML(elemName, attrs);
      if (tgene != null) {
        if (!qpcr_.hasColumns()) {
          throw new IOException();
        }
        qpcr_.addGene(tgene, false);
        currTarg_ = tgene;
      }
    } else if (nullPertKeys_.contains(elemName)) {
      NullPerturb np = NullPerturb.buildFromXML(ddacx_.getExpDataSrc().getTimeAxisDefinition(), elemName, attrs);
      if (np != null) {
        if (pendingDefaultNullTimeSpan_ != null) {
          qpcr_.setNullPerturbationsDefaultTimeSpan(pendingDefaultNullTimeSpan_);
          pendingDefaultNullTimeSpan_ = null;          
        }
        qpcr_.addNullPerturbation(np);
        currNPert_ = np;
        charTarg_ = PERT_TARGET_; 
        buf_.setLength(0);
      }
    } else if (footKeys_.contains(elemName)) {
      Footnote fn = Footnote.buildFromXML(elemName, attrs);
      if (fn != null) {
        qpcr_.addFootnote(fn);
        currFoot_ = fn;
        charTarg_ = FOOT_TARGET_;
        buf_.setLength(0);
      }
    } else if (pertKeys_.contains(elemName)) {
      Perturbation pert = Perturbation.buildFromXML(elemName, attrs);
      if (pert != null) {
        currTarg_.addPerturbation(pert);
        currPerturb_ = pert; 
      } 
    } else if (sourceKeys_.contains(elemName)) {
      Source src = Source.buildFromXML(elemName, attrs);
      if (src != null) {
        currPerturb_.addSource(src);
        currSource_ = src;
        charTarg_ = SOURCE_TARGET_;
        buf_.setLength(0);
      }
    } else if (nullSourceKeys_.contains(elemName)) {
      Source src = Source.buildFromXML(elemName, attrs);
      if (src != null) {
        currNPert_.addSource(src);
        currSource_ = src;
        charTarg_ = SOURCE_TARGET_;
        buf_.setLength(0);
      }    
    } else if (nullTargKeys_.contains(elemName)) {
      NullTarget nt = NullTarget.buildFromXML(elemName, attrs);
      if (nt != null) {
        currNPert_.addTarget(nt);
        currNullTarg_ = nt;
      } 
    } else if (nullSpanKeys_.contains(elemName)) {
      NullTimeSpan nts = NullTimeSpan.buildFromXML(ddacx_.getExpDataSrc().getTimeAxisDefinition(), elemName, attrs);
      if (nts != null) {
        currNullTarg_.addTimeSpan(nts);
      }
    } else if (defaultNullSpanKeys_.contains(elemName)) {
      NullTimeSpan nts = NullTimeSpan.buildFromXML(ddacx_.getExpDataSrc().getTimeAxisDefinition(), elemName, attrs);
      //
      // Here is the rub.  If the default null time span is defined, we cannot change
      // the time axis definition.  But legacy loads will show up with the old default
      // definition no matter what.  Only load it if it is not the default, or if we have
      // actual null perturbs to load.
      //
      if (nts != null) {        
         MinMax mm = TimeAxisDefinition.getLegacyDefaultTimeSpan();
         NullTimeSpan legNts = new NullTimeSpan(mm.min, mm.max);
        if (!legNts.equals(nts)) {
          qpcr_.setNullPerturbationsDefaultTimeSpan(nts);
        } else {
          pendingDefaultNullTimeSpan_ = nts;
        }
      } 
    } else if (spanKeys_.contains(elemName)) {
      TimeSpan span = TimeSpan.buildFromXML(ddacx_.getExpDataSrc().getTimeAxisDefinition(), elemName, attrs);
      if (span != null) {
        currPerturb_.addTime(span);
        currSpan_ = span;
      }  
    } else if (elemName.equals(regionRestrictKey_)) {
      String region = TimeSpan.getRegionRestrictFromXML(elemName, attrs);
      if (region != null) {
        currSpan_.addRegionRestriction(region);
      }        
    } else if (batchKeys_.contains(elemName)) {
      Batch bat = Batch.buildFromXML(elemName, attrs);
      if (bat != null) {
        if (Batch.isForNull(elemName)) {
          currNullTarg_.addBatch(bat);
        } else {
          currSpan_.addBatch(bat);
        }
        currBatch_ = bat;
      }      
    } else if (measKeys_.contains(elemName)) {
      Measurement meas = Measurement.buildFromXML(ddacx_.getExpDataSrc().getTimeAxisDefinition(), elemName, attrs, qpcr_.getThresholdValue());
      if (meas != null) {
        currBatch_.addMeasurement(meas);
      }      
    } else if (elemName.equals(nameKey_)) {
      charTarg_ = NAME_TARGET_;
      buf_.setLength(0);
    } else if (elemName.equals(colKey_)) {  // Legacy column types
      charTarg_ = COLUMN_TARGET_;
      buf_.setLength(0); 
    } else if (elemName.equals(colRangeKey_)) {
      MinMax colRange = QPCRData.extractColumnRange(elemName, attrs);
      qpcr_.addColumn(colRange);
    } else if (elemName.equals(dmKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currMapKey_ = QPCRData.extractMapKey(elemName, attrs);
      currMapList_ = new ArrayList<QPCRData.QpcrMapResult>();        
    } else if (elemName.equals(useKey_)) {
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      currMapList_.add(QPCRData.extractUseQPCR(elemName, attrs));       
    } else if (elemName.equals("i")) {
      buf_.append("<i>");
    } else if (elemName.equals("sup")) {
      buf_.append("<sup>");      
    } else if (elemName.equals("sub")) {
      buf_.append("<sub>");      
    } 
    return (null);
  }
}
