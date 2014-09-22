/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.util;

import java.awt.geom.Point2D;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;

/****************************************************************************
**
** A class to analyze DOF options for link recovery
*/

public class RecoveryDOFAnalyzer {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private boolean haveOrthoPoints_;
  private boolean haveOrthoCorner_;
  private boolean gottaBeOrtho_; 
  private boolean cornerGottaBeOrtho_; 
  private List<Point2D> threePts_;
  private DOFOption firstPointOption_;
  private boolean returnFirstPoint_;
  private DOFOption secondPointOption_;
  private boolean returnSecondPoint_;    
  private LinkPlacementGrid.RecoveryDataForLink recoverForLink_; 
  private int currDepth_;
  private Point2D targ_;
  private LinkPlacementGrid.TravelTurn turn_;
  private String src_;
  private String trg_;
  private String linkID_;
  private HashSet<String> okGroups_; 
  private Set<String> recoveryExemptions_; 
  private boolean isStart_;
  private LinkPlacementGrid grid_;
  private List<DualCandidate> dualPtList_;
  private Point2D newPt_;
  private boolean modifyFirst_;
  private boolean pointIsFirst_;
  private int cornerDepth_;
  private ArrayList<DOFOptionPair> answerList_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  
  public RecoveryDOFAnalyzer(LinkPlacementGrid grid, boolean gottaBeOrtho, boolean cornerGottaBeOrtho, 
                             LinkPlacementGrid.RecoveryDataForLink recoverForLink, int currDepth, 
                             Point2D targ, LinkPlacementGrid.TravelTurn turn, String src, String trg, String linkID,
                             Set<String> okGroups, Set<String> recoveryExemptions, boolean isStart) {

    grid_ = grid;
    targ_ = targ; 
    turn_ = turn; 
    gottaBeOrtho_ = gottaBeOrtho; 
    cornerGottaBeOrtho_ = cornerGottaBeOrtho;
    recoverForLink_ = recoverForLink;
    currDepth_ = currDepth;
    src_ = src;
    trg_ = trg; 
    linkID_ = linkID;
    okGroups_ = (okGroups == null) ? null : new HashSet<String>(okGroups);
    recoveryExemptions_ = recoveryExemptions;
    isStart_ = isStart;

    threePts_ = grid_.buildRecoverPointList(turn.start, targ, recoverForLink_, currDepth_);
    haveOrthoPoints_ = (threePts_ == null) ? false : grid_.recoverPointListStartsOrtho(threePts_);
    haveOrthoCorner_ = (haveOrthoPoints_ && grid_.recoverPointListIsOrtho(threePts_));
    firstPointOption_ = null;
    returnFirstPoint_ = false;
    secondPointOption_ = null;
    returnSecondPoint_ = false;
    dualPtList_ = null;
    newPt_ = null;
    modifyFirst_ = false;
    pointIsFirst_ = false;
    cornerDepth_ = 0;
    answerList_ = new ArrayList<DOFOptionPair>();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** If we have some freedom to shift points to achieve orthogonality, generate a
  ** list of possible options
  */

  public List<DOFOptionPair> generateDOFOptions() {
                             
    ArrayList<DOFOptionPair> retval = new ArrayList<DOFOptionPair>();
    
    //
    // See if the first point can be wiggled:
    //
    
    if (!analyzeFirstPoint()) {
      return (retval);
    }
        
    //
    // If we do not need to do corner work, we are done:
    //
        
    if (!needCornerPointAnalysis()) {
      cleanUpForReturn(retval);
      return (retval);
    }  
      
    //
    // Try to make the corner ortho.  
    //
         
    if (!prepareCornerAnalysis()) {
      cleanUpForReturn(retval);
      return (retval);
    }    

    if (!doCornerAnalysis()) {
      cleanUpForReturn(retval);
      return (retval);
    }      
     
    cleanUpForReturn(retval);    
    return (retval);
   
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////     
  
  /***************************************************************************
  **
  ** Do DOF analysis for first point...
  */    
    
  private boolean analyzeFirstPoint() {
    if (gottaBeOrtho_ && !haveOrthoPoints_) {
      BusProperties.CornerDoF dof = grid_.getCornerDoF(recoverForLink_, currDepth_);
      if (dof != null) {
        Point2D newPt = generateDownstreamOrthoCandidate(dof, currDepth_);
        if (newPt != null) {
          LinkSegmentID lsid = recoverForLink_.getPointKey(currDepth_);
          if ((lsid != null) && recoverForLink_.revisedPointCanBePushed(lsid)) {
            recoverForLink_.pushRevisedPoint(lsid, newPt);
            firstPointOption_ = new DOFOption(lsid, newPt);
            threePts_ =  grid_.buildRecoverPointList(turn_.start, targ_, recoverForLink_, currDepth_);
            boolean nowHaveOrthoPoints = (threePts_ == null) ? false : grid_.recoverPointListStartsOrtho(threePts_);
            if (nowHaveOrthoPoints &&  grid_.haveDOFWillTravel(threePts_, false, src_, trg_, linkID_, okGroups_, 
                                                               recoveryExemptions_, isStart_ && (currDepth_ == 0))) {    
              returnFirstPoint_ = true;
              haveOrthoPoints_ = nowHaveOrthoPoints;
              haveOrthoCorner_ = (haveOrthoPoints_ &&  grid_.recoverPointListIsOrtho(threePts_));
              Point2D startPt = threePts_.get(0);
              Point2D endPt = threePts_.get(1);
              Vector2D departDir = (new Vector2D(startPt, endPt)).normalized();
              turn_ =  grid_.buildInitRecoveryTurn(departDir, threePts_);
            } else {
              recoverForLink_.popRevisedPoint(firstPointOption_.lsid);
              return (false);
            }
          } 
        }
      }
    }
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Clean up stuff and prepare to return
  */ 
  
  private void cleanUpForReturn(List<DOFOptionPair> retval) {
    if (firstPointOption_ != null) {
      recoverForLink_.popRevisedPoint(firstPointOption_.lsid);
    }
    if (secondPointOption_ != null) {
      recoverForLink_.popRevisedPoint(secondPointOption_.lsid);
    }    
    if (returnFirstPoint_ || returnSecondPoint_) {
      DOFOption firstRet = (returnFirstPoint_) ? firstPointOption_ : null;
      DOFOption secondRet = (returnSecondPoint_) ? secondPointOption_ : null;
      DOFOptionPair dop = new DOFOptionPair(firstRet, secondRet);
      retval.add(dop);
    } else {
      retval.addAll(answerList_);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Find out is corner point analysis is needed
  */
  
  private boolean needCornerPointAnalysis() {
    return (cornerGottaBeOrtho_ && !haveOrthoCorner_);
  }
    
    
  /***************************************************************************
  **
  ** Set up the corner point analysis
  */      
    
  private boolean prepareCornerAnalysis() {
    BusProperties.CornerDoF dof1 =  grid_.getCornerDoF(recoverForLink_, currDepth_);
    BusProperties.CornerDoF dof2 =  grid_.getCornerDoF(recoverForLink_, currDepth_ + 1);
    if ((dof1 != null) && (dof2 != null)) {
      dualPtList_ = generateDualOrthoCandidates(dof1, dof2, currDepth_); 
    } else if (dof1 != null) {
      newPt_ = generateUpstreamOrthoCandidate(dof1, currDepth_);
      cornerDepth_ = currDepth_;
      modifyFirst_ = (firstPointOption_ != null);
      pointIsFirst_ = true;
    } else if (dof2 != null) {
      newPt_ = generateDownstreamOrthoCandidate(dof2, currDepth_ + 1);       
      cornerDepth_ = currDepth_ + 1;
    }

    return ((dualPtList_ != null) || (newPt_ != null));    
  }
  
  /***************************************************************************
  **
  ** Do the corner point analysis 
  */
  
  private boolean doCornerAnalysis() {
    if (newPt_ != null) {
      return (doOnePointCornerAnalysis());
    } else if (dualPtList_ != null) {
      return (doTwoPointCornerAnalysis());
    } else {
      throw new IllegalStateException();
    }
  }  
 
  
  /***************************************************************************
  **
  ** Single point case.  Note that if we are working with the first point, if it was already pushed,
  ** we will need to modify that point!
  */  

  private boolean doOnePointCornerAnalysis() {    
    DOFOption stashedOption = null;
    LinkSegmentID lsid = recoverForLink_.getPointKey(cornerDepth_);
    if ((lsid != null) && (modifyFirst_ || recoverForLink_.revisedPointCanBePushed(lsid))) { 
      if (modifyFirst_) {
        recoverForLink_.popRevisedPoint(firstPointOption_.lsid);
        stashedOption = firstPointOption_;
      }
      recoverForLink_.pushRevisedPoint(lsid, newPt_);
      DOFOption pointOption = new DOFOption(lsid, newPt_);
      if (pointIsFirst_) {
        firstPointOption_ = pointOption;
      } else {
        secondPointOption_ = pointOption;
      }  
      threePts_ = grid_.buildRecoverPointList(turn_.start, targ_, recoverForLink_, currDepth_);
      boolean nowHaveOrthoCorner = (threePts_ == null) ? false : grid_.recoverPointListIsOrtho(threePts_);
      if (nowHaveOrthoCorner && grid_.haveDOFWillTravel(threePts_, true, src_, trg_, linkID_, okGroups_, recoveryExemptions_, false)) {
        if (pointIsFirst_) {
          returnFirstPoint_ = true;
        } else {
          returnSecondPoint_ = true;
        }
      } else {
        if (modifyFirst_) {
          recoverForLink_.popRevisedPoint(firstPointOption_.lsid);
          recoverForLink_.pushRevisedPoint(stashedOption.lsid, stashedOption.newPt);
          firstPointOption_ = stashedOption;
        }
        return (false);
      } 
    }
    return (true);
  }

  /***************************************************************************
  **
  ** Two point case
  */     
  
  private boolean doTwoPointCornerAnalysis() {
       
    int numDC = dualPtList_.size();
    modifyFirst_ = (firstPointOption_ != null);   
    DOFOption stashedOption = null;

    for (int i = 0; i < numDC; i++) {
      DualCandidate dc = (DualCandidate)dualPtList_.get(i);
      DOFOption startOption = null;
      DOFOption endOption = null;
      if (dc.startCandidate != null) {
        LinkSegmentID lsid = recoverForLink_.getPointKey(currDepth_);
        if ((lsid != null) && (modifyFirst_ || recoverForLink_.revisedPointCanBePushed(lsid))) { 
          if (modifyFirst_) {
            recoverForLink_.popRevisedPoint(firstPointOption_.lsid);
            stashedOption = firstPointOption_;
          }
          recoverForLink_.pushRevisedPoint(lsid, dc.startCandidate);
          startOption = new DOFOption(lsid, dc.startCandidate);
        }
      }
      if (dc.endCandidate != null) {
        LinkSegmentID lsid = recoverForLink_.getPointKey(currDepth_ + 1);
        if ((lsid != null) && recoverForLink_.revisedPointCanBePushed(lsid)) { 
          recoverForLink_.pushRevisedPoint(lsid, dc.endCandidate);
          endOption = new DOFOption(lsid, dc.endCandidate);
        }
      }
   
      threePts_ = grid_.buildRecoverPointList(turn_.start, targ_, recoverForLink_, currDepth_);
      boolean nowHaveOrthoCorner = (threePts_ == null) ? false : grid_.recoverPointListIsOrtho(threePts_);
      if (nowHaveOrthoCorner && grid_.haveDOFWillTravel(threePts_, true, src_, trg_, linkID_, okGroups_, recoveryExemptions_, false)) {
        DOFOptionPair dop = new DOFOptionPair(startOption, endOption); 
        answerList_.add(dop);
      }
      //
      // Get back to the original state:
      //
      
      if (startOption != null) {
        recoverForLink_.popRevisedPoint(startOption.lsid);
        if (modifyFirst_) {
          recoverForLink_.pushRevisedPoint(stashedOption.lsid, stashedOption.newPt);
          stashedOption = null;
        }
      }      
      if (endOption != null) {
        recoverForLink_.popRevisedPoint(endOption.lsid);
      }
    }
    return (answerList_.size() > 0);
  }

  /***************************************************************************
  **
  ** Generate an ortho candidate by adjusting the "downstream" point.  Used for
  ** first point generation via currDepth, or corner adjustment using currDepth + 1:
  */

  private Point2D generateDownstreamOrthoCandidate(BusProperties.CornerDoF dof, int useDepth) {
    
    List<Point2D> threePts = grid_.buildRecoverPointList(turn_.start, targ_, recoverForLink_, useDepth);
    if (threePts == null) {
      return (null);
    }
    if (!dof.inboundIsCanonical) {
      return (null);
    }    
    
    //
    // Get a downstream candidate that is canonical:
    //
    
    Point2D startPt = threePts.get(0);
    startPt = new Point2D.Double(startPt.getX() * 10.0, startPt.getY() * 10);
    Point2D nonOrthPt = threePts.get(1); 
    nonOrthPt = new Point2D.Double(nonOrthPt.getX() * 10.0, nonOrthPt.getY() * 10);
    Vector2D toNonOrth = new Vector2D(startPt, nonOrthPt);
    
    double dotVal = dof.runVector.dot(toNonOrth);
    Vector2D toOrth = dof.runVector.scaled(dotVal);
    Point2D candidate = toOrth.add(startPt);    
    UiUtil.forceToGrid(candidate, UiUtil.GRID_SIZE);
    
    Point2D replacement = withinDOFBounds(dof, nonOrthPt, candidate, dof.runVector);    
    return (replacement); // may be null
  }
  
  /***************************************************************************
  **
  ** Generate an ortho candidate for the corner case by wiggling the "upstream" point
  */

  private Point2D generateUpstreamOrthoCandidate(BusProperties.CornerDoF dof, int useDepth) {
    
    List<Point2D> threePts = grid_.buildRecoverPointList(turn_.start, targ_, recoverForLink_, useDepth);
    if (threePts == null) {
      return (null);
    }

    Point2D startPt = threePts.get(0);
    startPt = new Point2D.Double(startPt.getX() * 10.0, startPt.getY() * 10);
    Point2D cornerPt = threePts.get(1); 
    cornerPt = new Point2D.Double(cornerPt.getX() * 10.0, cornerPt.getY() * 10);
    Point2D endPt = threePts.get(2); 
    endPt = new Point2D.Double(endPt.getX() * 10.0, endPt.getY() * 10);    
       
    Vector2D toEnd = new Vector2D(cornerPt, endPt);
    double dotVal = dof.runVector.dot(toEnd);
    Vector2D toOrth = dof.runVector.scaled(dotVal);
    Point2D candidate = toOrth.add(cornerPt);    
    UiUtil.forceToGrid(candidate, UiUtil.GRID_SIZE);   
    Point2D replacement = withinDOFBounds(dof, cornerPt, candidate, dof.runVector); 
    
    return (replacement);  // may be null
  }    
 
  /***************************************************************************
  **
  ** Answer if the point is not allowed to shift because it is pinned by other
  ** downstream points that would become non-orthogonal in the process.
  ** FIX ME: IMPROVE THIS BY CHECKING FOR TRANSITIVE DOF OF THESE DOWNSTREAM POINTS!
  */

  private boolean isPinned(BusProperties.CornerDoF dof, Vector2D normShiftVec, 
                           Vector2D desiredCanonicalVec) {
 
    if (normShiftVec.equals(dof.antiNormVector) || normShiftVec.equals(dof.normVector)) {
      if (dof.runPoint != null) {
        return (true);
      }
      if (!dof.inboundIsCanonical && (dof.backupPoint != null)) {
        return (true);
      }
    } else if (normShiftVec.equals(dof.backupVector) || normShiftVec.equals(dof.runVector)) {
      if (desiredCanonicalVec.equals(dof.antiNormVector) && (dof.normPoint != null)) {
        return (true);
      }
      if (desiredCanonicalVec.equals(dof.normVector) && (dof.antiNormPoint != null)) {
        return (true);
      }      
    }
    
    return (false);
     
  }  

  /***************************************************************************
  **
  ** Get candidate within allowed bounds:
  */

  private Point2D withinDOFBounds(BusProperties.CornerDoF dof, Point2D orig, Point2D candidate, Vector2D desiredCanonicalVec) {
    Vector2D origToCand = new Vector2D(orig, candidate);
    Vector2D normOrigToCand = origToCand.normalized(); 
    
    if (isPinned(dof, normOrigToCand, desiredCanonicalVec)) {
      return (null);
    }
    
    Point2D modified = dotCompare(dof.backupPoint, dof.backupVector, origToCand, normOrigToCand, orig, candidate);
    if (modified == null) {
      return (null);
    // Special case when inbound is canonical: null backup means we cannot move:    
    } else if (normOrigToCand.equals(dof.backupVector) && (dof.inboundIsCanonical) && (dof.backupPoint == null)) {
      return (null);
    }     

    modified = dotCompare(dof.runPoint, dof.runVector, origToCand, normOrigToCand, orig, modified);
    if (modified == null) {
      return (null);
    }
     
    modified = dotCompare(dof.normPoint, dof.normVector, origToCand, normOrigToCand, orig, modified);
    if (modified == null) {
      return (null);
    }
    modified = dotCompare(dof.antiNormPoint, dof.antiNormVector, origToCand, normOrigToCand, orig, modified);
    return (modified);
  }  
  
  /***************************************************************************
  **
  ** Answer if the shifted point is within allowed variation:
  */

  private Point2D dotCompare(LinkSegmentID dofPoint, Vector2D dofVector, Vector2D origToCand, 
                             Vector2D normOrigToCand, Point2D orig, Point2D candidate) {
    
    if (dofPoint != null) {
     double runDot = dofVector.dot(origToCand);
      if (runDot <= 0.0) {
        return (candidate);
      }
      Point2D currRunPoint = recoverForLink_.getPoint(dofPoint);
      if (currRunPoint != null) {
        Vector2D origToCurr = new Vector2D(orig, currRunPoint);
        double origToCurrDot = dofVector.dot(origToCurr);
        if (origToCurrDot <= runDot) {
          
          // Don't want to overrun a point, *****but this makes us fall short.  What to do?
          origToCurrDot -= (2.0 * UiUtil.GRID_SIZE);
          if (origToCurrDot <= 0.0) {
            return (null);
          }
          Vector2D toReplace = dofVector.scaled(origToCurrDot);
          Point2D replacement = toReplace.add(orig);    
          UiUtil.forceToGrid(replacement, UiUtil.GRID_SIZE);
          return (replacement);
        }
      }
    }
    return (candidate);
  }
  

    
  /***************************************************************************
  **
  ** Generate a list of ortho candidates
  */

  private List<DualCandidate> generateDualOrthoCandidates(BusProperties.CornerDoF dofStart, 
                                                          BusProperties.CornerDoF dofEnd, int useDepth) {
    
    ArrayList<DualCandidate> retval = new ArrayList<DualCandidate>();
    List<Point2D> threePts = grid_.buildRecoverPointList(turn_.start, targ_, recoverForLink_, useDepth);
    if ((threePts == null) || (threePts.size() < 3)) {
      return (null);
    }   
    if (!dofEnd.inboundIsCanonical) {  // Wasn't ortho to begin with!
      return (null);
    }
    
    //
    // We want to shift the two points as needed:
    //
    
    Point2D startPt = threePts.get(1);
    startPt = new Point2D.Double(startPt.getX() * 10.0, startPt.getY() * 10);
    Point2D endPoint = threePts.get(2); 
    endPoint = new Point2D.Double(endPoint.getX() * 10.0, endPoint.getY() * 10);
    Point2D startCand = generateUpstreamOrthoCandidate(dofStart, useDepth);    
    Point2D endCand = generateDownstreamOrthoCandidate(dofEnd, useDepth + 1);
        
    //
    // Partial results give simple answers:
    //
    
    if ((startCand == null) && (endCand == null)) {
      return (null);
    } else if (startCand == null) {
      DualCandidate dc = new DualCandidate();
      dc.startCandidate = null;
      dc.endCandidate = endCand;
      retval.add(dc);
      return (retval);          
    } else if (endCand == null) {
      DualCandidate dc = new DualCandidate();
      dc.startCandidate = startCand;
      dc.endCandidate = null;
      retval.add(dc);
      return (retval);          
    }
 
    //
    // Generate all the canonical possibilities:
    //
 
    DualCandidate dc = new DualCandidate();
    dc.startCandidate = startCand;
    dc.endCandidate = null;
    retval.add(dc);
    
    dc = new DualCandidate();
    dc.startCandidate = null;
    dc.endCandidate = endCand;
    retval.add(dc);    
 
    return (retval);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////   
    
  public static class DOFOption {
    LinkSegmentID lsid;
    Point2D newPt;
 
    DOFOption(LinkSegmentID lsid, Point2D newPt) {
      this.lsid = lsid;
      this.newPt = newPt;
    }
    
    public String toString() {
      return ("DOFOption: lsid = " + lsid + " pt = " + newPt);
    }
    
  }
   
  public static class DOFOptionPair {
    DOFOption firstPoint;
    DOFOption cornerPoint;
 
    DOFOptionPair(DOFOption firstPoint, DOFOption cornerPoint) {
      this.firstPoint = firstPoint;
      this.cornerPoint = cornerPoint;
    }
    
    public String toString() {
      return ("DOFOptionPair: first = " + firstPoint + " corner = " + cornerPoint);
    } 
  }   

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  
  private static class DualCandidate {
    Point2D startCandidate;
    Point2D endCandidate;
    
    public String toString() {
      return ("DualCandidate: startCandidate = " + startCandidate + " endCandidate = " + endCandidate);
    }
    
    
  }    
}
