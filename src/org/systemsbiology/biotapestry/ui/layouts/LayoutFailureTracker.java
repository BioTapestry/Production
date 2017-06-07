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

package org.systemsbiology.biotapestry.ui.layouts;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutRubberStamper;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** The failure of correct link tree construction when doing automatic layout is
** commonplace during development.  The places to put breakpoints and what to
** look for is obscure and easy to lose track of unless you are working on it
** all the time.  This class serves as a central resource for doing this stuff.
** Just looking for where these function calls exist in the code gives you a
** good idea of the control flow through the process! 
*/

public class LayoutFailureTracker {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE STATIC MEMBER
  //
  //////////////////////////////////////////////////////////////////////////// 


  public static final double GRID_SIZE = 10.0; 
  public static final int    GRID_SIZE_INT = 10;  
  
  private static HashMap<String, String> bordersExits_;
  private static HashMap<String, String> bordersExitsShifted_;
  private static HashMap<String, String> linkOrder_;
  private static HashMap<String, String> exitRoutes_;
  private static HashMap<String, String> mergeData_;
  private static HashMap<String, String> ptLists_;
  private static HashMap<String, String> trunkPoints_;
  private static HashMap<String, String> leafPoints_;
  private static HashMap<String, String> postLayoutForSource_;
  private static HashMap<String, String> recoveryPoints_;
  private static HashMap<String, String> recoveryTosses_;
  private static boolean enabled_;
  private static StringBuffer buf_;
  private static String glueDone_;
  private static ArrayList<String> failedLinks_;
  private static int passNum_;
  private static int subPassNum_;

  /***************************************************************************
  **
  ** In production, this is NEVER set to true!  Only for debug, since the
  ** data collection is intense.
  */
  
  static {
    UiUtil.fixMePrintout("Make sure this is disabled for production release!");
    enabled_ = false;
    if (enabled_) {
      buf_ = new StringBuffer();
      bordersExits_ = new HashMap<String, String>();
      bordersExitsShifted_ = new HashMap<String, String>();
      mergeData_ = new HashMap<String, String>();
      exitRoutes_ = new HashMap<String, String>();
      linkOrder_ = new HashMap<String, String>();
      trunkPoints_ = new HashMap<String, String>();
      leafPoints_ = new HashMap<String, String>();
      ptLists_ = new HashMap<String, String>();
      postLayoutForSource_ = new HashMap<String, String>();
      recoveryPoints_ = new HashMap<String, String>();
      recoveryTosses_ = new HashMap<String, String>();
      failedLinks_ = new ArrayList<String>();
      passNum_ = -1;
      subPassNum_ = -1;
    }
  }
 
  
  /***************************************************************************
  **
  ** Record recovery point toss
  */
    
   public static void recordRecoveryToss(String srcID, String linkID, LayoutRubberStamper.EdgeMove emov, Point2D point) {
     if (!enabled_) {
       return;
     }
     buf_.setLength(0);       
     String pre = recoveryPoints_.get(srcID);
     if (pre != null) {
       buf_.append(pre);   
     }
     buf_.append("Recovery Toss " + srcID + " " + linkID + " " + emov + " " + point);
     buf_.append("\n");
     recoveryTosses_.put(srcID, buf_.toString());
     return;
   }
 
  /***************************************************************************
  **
  ** Record recovery info
  */
    
   public static void recordRecoveryTrack(String srcID, String linkID, List<Point2D> points) {
     if (!enabled_) {
       return;
     }
     buf_.setLength(0);       
     String pre = recoveryPoints_.get(srcID);
     if (pre != null) {
       buf_.append(pre);   
     } else {
       buf_.append("Recover " + srcID + " " + linkID);
     }
     buf_.append(points.toString());
     buf_.append("\n");
     recoveryPoints_.put(srcID, buf_.toString());
     return;
   }
  
  /***************************************************************************
  **
  ** Stash ALL link layout data
  */
    
   public static void postLayoutReport(StaticDataAccessContext irx) {
     if (!enabled_) {
       return;
     }
     Iterator<Node> nit = irx.getCurrentGenome().getAllNodeIterator();
     while (nit.hasNext()) {
       Node aNode = nit.next();
       String srcID = aNode.getID();
       BusProperties bp = irx.getCurrentLayout().getLinkPropertiesForSource(srcID);
       if (bp == null) {
         continue;
       }     
       buf_.setLength(0);       
       String pre = postLayoutForSource_.get(srcID);
       if (pre != null) {
         buf_.append(pre);   
       } else {
         buf_.append("Full Post Layout for " + srcID + ":\n");
       }
       Set<Point2D> allPts = bp.getAllPointsToLeaves(LinkSegmentID.buildIDForStartDrop());
       if (!allPts.isEmpty()) {
         buf_.append(srcID + ": " + bp.getDepthFirstDebug(irx));
       }
       buf_.append("\n");
       postLayoutForSource_.put(srcID, buf_.toString());
     }
   }

  /***************************************************************************
  **
  ** When trying to recover existing tree segments to glue onto new specialty
  ** layout, the SpecialtyLayoutLinkData does the chopping and saving.
  */
   
  public static void recordTreeTrunk(SpecialtyLayoutLinkData sin, String linkID, SpecialtyLayoutLinkData.NoZeroList ptsPerLink) {
    if (!enabled_) {
      return;
    }
    buf_.setLength(0);
    String srcID = sin.getSrcID();
    String pre = trunkPoints_.get(srcID);
    if (pre != null) {
      buf_.append(pre);   
    } else {
      buf_.append("Tree Trunk Record for " + sin.getSrcID() + ":\n");
    }
    buf_.append(linkID);
    buf_.append(": ");
    buf_.append(ptsPerLink.toString());
    buf_.append("\n");
    trunkPoints_.put(srcID, buf_.toString());
    return;
  }
  
  /***************************************************************************
  **
  ** When trying to recover existing tree segments to glue onto new specialty
  ** layout, the SpecialtyLayoutLinkData does the chopping and saving.
  ** 
  */
   
  public static void recordTreeLeaf(SpecialtyLayoutLinkData sin, String linkID, SpecialtyLayoutLinkData.NoZeroList ptsPerLink) {
    if (!enabled_) {
      return;
    }
    buf_.setLength(0);
    String srcID = sin.getSrcID();
    String pre = leafPoints_.get(srcID);
    if (pre != null) {
      buf_.append(pre);   
    } else {
      buf_.append("Tree Leaf Record for " + sin.getSrcID() + ":\n");
    }
    buf_.append(linkID);
    buf_.append(": ");
    buf_.append(ptsPerLink.toString());
    buf_.append("\n");
    leafPoints_.put(srcID, buf_.toString());
    return;
  }
  

  /***************************************************************************
  **
  ** The rather poorly named SuperSrcRouterPointSource class generates
  ** exit points for the link sources in a cluster.  E.g. for stacked
  ** layouts, it is responsible to setting the border positions and exit 
  ** framework points for a source that is exiting the stack.
  ** 
  ** 
  */
  
  @SuppressWarnings("unused")
  public static void recordBordersAndExits(SuperSrcRouterPointSource ssr, 
                                           SpecialtyLayoutLinkData sin, 
                                           int debugPath) {
    if (!enabled_) {
      return;
    }
    buf_.setLength(0);
    buf_.append("Borders and exits for " + sin.getSrcID() + " set via branch " + debugPath + "\n"); 
    recordBordersAndExitGuts(sin, bordersExits_);
    return;
  }
    
  /***************************************************************************
  **
  ** Guts of borders and exits!
  ** 
  ** 
  */
    
  private static void recordBordersAndExitGuts(SpecialtyLayoutLinkData sin, Map<String, String> useMap) {
    for (int i = 0; i < SpecialtyLayoutLinkData.NUM_EXIT_FRAMEWORK; i++) {
      SpecialtyLayoutLinkData.PlacedPoint pp = sin.getExitFrameworkForDebug(i);
      buf_.append("Exit  " + i + " = " + pp + "\n");
    }
    for (int i = 0; i < SpecialtyLayoutLinkData.NUM_BORDERS; i++) {
      Point2D bp = sin.getBorderPosition(i);
      buf_.append("Border " + i + " = " + bp + "\n");
    }
    useMap.put(sin.getSrcID(), buf_.toString());
    return;
  }

  /***************************************************************************
  **
  ** Record layout pass
  ** 
  */
    
  public static void recordLayoutPass(int i, int j) {
    if (!enabled_) {
      return;
    }
    passNum_ = i;
    subPassNum_ = j;
    return;
  }
  
  /***************************************************************************
  **
  ** Clear layout pass
  ** 
  */
    
  public static void clearLayoutPass() {
    if (!enabled_) {
      return;
    }
    passNum_ = -1;
    subPassNum_ = -1;
    return;
  }
  
  /***************************************************************************
  **
  ** This stuff gets shifted as modules get moved around
  ** 
  */
    
  public static void recordShiftedBordersAndExits(SpecialtyLayoutLinkData sin, Vector2D shift) {
    if (!enabled_) {
      return;
    }
    buf_.setLength(0);
    buf_.append("Shifted Borders and exits for " + sin.getSrcID() + " after shift " + shift + "\n"); 
    recordBordersAndExitGuts(sin, bordersExitsShifted_);
    return;
  }
 
  /***************************************************************************
  **
  ** Report link failures
  */  
  
  public static void reportFailedLinks(Set<String> failures) {
    if (!enabled_) {
      return;
    }
    failedLinks_.add(failures.toString());
    return;
  }

  /***************************************************************************
  **
  ** record results of merge
  */  
  
  public static void recordMergeOperation(SpecialtyLayoutLinkData sin, String funcName) {
    if (!enabled_) {
      return;
    }
    buf_.setLength(0);
    String srcID = sin.getSrcID();
    String pre = mergeData_.get(srcID);
    if (pre != null) {
      buf_.append(pre);   
    }
    buf_.append("POST MERGE SpecialtyLayoutLinkData " + srcID + " via " + funcName + ":\n");   
    linkDataGuts(sin);
    mergeData_.put(srcID, buf_.toString());
    return;
  }
  
  /***************************************************************************
  **
  ** Record inputs to merge operations
  */  
  
  public static void recordPreMergeOperation(SpecialtyLayoutLinkData sin, SpecialtyLayoutLinkData other, String funcName) {
    if (!enabled_) {
      return;
    }
    buf_.setLength(0);
    String srcID = sin.getSrcID();
    String pre = mergeData_.get(srcID);
    if (pre == null) {
      buf_.append("MERGE DATA FOR " + srcID + ": " + "\n");   
    } else {
      buf_.append(pre);   
    }
    buf_.append("PRIME MERGE SpecialtyLayoutLinkData " + srcID + " via " + funcName +  ":\n");   
    linkDataGuts(sin);
    buf_.append("OTHER MERGE SpecialtyLayoutLinkData " + other.getSrcID() + " via " + funcName +  "\n");   
    linkDataGuts(other);
    mergeData_.put(srcID, buf_.toString());
    return;
  }
  
  /***************************************************************************
  **
  ** Guts of link data output
  */  
  
  private static void linkDataGuts(SpecialtyLayoutLinkData sin) {
    int numLinks = sin.numLinks();
    for (int j = 0; j < numLinks; j++) {
      String linkID = sin.getLink(j);      
      buf_.append(linkID);
      buf_.append(": ");
      SpecialtyLayoutLinkData.NoZeroList posList = sin.getPositionList(linkID);
      int numPos = posList.size();
      for (int i = 0; i < numPos; i++) {
        Point2D pt = posList.get(i).getPoint();
        buf_.append(pt);
        if (i != (numPos - 1)) {
          buf_.append(", ");
        }
      }
      buf_.append("\n");
    }
    return;
  }

  /***************************************************************************
  **
  ** We have separate SpecialtyLayoutLinkData objects from each subset for
  ** each source.  Before we can go and build the link properties, we need to
  ** glue everybody together.  Record that here!
  */
  
  public static void reportGluingDone(List<SpecialtyLayoutLinkData> placeList) {
    if (!enabled_) {
      return;
    }
    buf_.setLength(0);
    buf_.append("Glue phase is complete\n"); 
    int numSin = placeList.size();
    for (int i = 0; i < numSin; i++) {
      SpecialtyLayoutLinkData sin = placeList.get(i);
      buf_.append((sin != null) ? sin.getSrcID() : "<no linkData>");
      if (i != (numSin - 1)) {
        buf_.append(", ");
      }
    }
    buf_.append("\n");
    glueDone_ = buf_.toString();
    return;    
  }
  
  /***************************************************************************
  **
  ** Reporting for final link assembly order:  IN THE OLD DAYS, the original
  ** link order in the SpecialtyLayoutLinkData was considered good!  NOT ANY
  ** MORE!  They are reordered so that things can be built out of order.
  */
  
  public static void reportReorder(SpecialtyLayoutLinkData sin) {
    if (!enabled_) {
      return;
    }
    buf_.setLength(0);
    buf_.append("Final link order for src " + sin.getSrcID() + ": ");   
    int numLinks = (sin == null) ? 0 : sin.numLinks();
    for (int i = 0; i < numLinks; i++) {
      String linkID = sin.getLink(i);
      buf_.append(linkID);
      if (i != (numLinks - 1)) {
        buf_.append(", ");
      }
    }
    buf_.append("\n");
    linkOrder_.put(sin.getSrcID(), buf_.toString());
    return;
  }
  
  /***************************************************************************
  **
  ** Report the construction of the final link tree:
  */

  public static void reportAssembly(String srcID, String linkID, SpecialtyLayoutLinkData.NoZeroList ptList) {
    if (!enabled_) {
      return;
    }
    buf_.setLength(0);
    String pre = ptLists_.get(srcID);
    if (pre == null) {
      buf_.append("Placing links for src " + srcID + ": " + "\n");   
    } else {
      buf_.append(pre);   
    }
    buf_.append("  Point list for link " + linkID + ": ");   
    int numPts = ptList.size();
    for (int i = 0; i < numPts; i++) {
      Point2D pt = ptList.get(i).getPoint();
      buf_.append(pt);
      if (i != (numPts - 1)) {
        buf_.append(", ");
      }
    }
    buf_.append("\n");
    ptLists_.put(srcID, buf_.toString());
    return;
  }
     
  /***************************************************************************
  **
  ** Error trigger for report on failure:
  */
   
  public static void considerFailureReport(String linkID, Point2D reloc, BusProperties bp) {
    if (!enabled_) {
      return;
    }
    if (!bp.getAllTreePoints().contains(reloc)) {
      System.err.println("EMERGENCY fallback placement for linkID: " + linkID + " no attachment for" + reloc);
      System.err.println("pass: " + passNum_ + " subPass: " + subPassNum_);
      failureReport(bp);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Force report on failure:
  ** Look for commented out calls to forceFailureReport in the code for recommended placement
  */
   
  public static void forceFailureReport(String linkID, String forceID, BusProperties bp) {
    if (!enabled_) {
      return;
    }
    if (linkID.equals(forceID)) {
      System.err.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
      System.err.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
      System.err.println("Forced failure report for linkID: " + linkID);
      System.err.println("pass: " + passNum_ + " subPass: " + subPassNum_);
      failureReport(bp);
    }
    return;
  }

   /***************************************************************************
  **
  ** Error trigger for report on failure:
  */
   
  private static void failureReport(BusProperties bp) {
    System.err.println("****************************************************************");
    System.err.println("Link Layout Failures:");
    for (int i = 0; i < failedLinks_.size(); i++) {
      String failed = failedLinks_.get(i);
      System.err.println(failed);
    }
    System.err.println("****************************************************************");
    String rct = recoveryTosses_.get(bp.getSourceTag());
    System.err.print(rct);
    System.err.println("****************************************************************");
    String rco = recoveryPoints_.get(bp.getSourceTag());
    System.err.print(rco);
    System.err.println("****************************************************************");
    String ploTxt = postLayoutForSource_.get(bp.getSourceTag());
    System.err.print(ploTxt);
    System.err.println("****************************************************************");
    System.err.println("Current BP: " + bp.getAllPointsToLeaves(LinkSegmentID.buildIDForStartDrop()));
    System.err.println("****************************************************************");
    String trpTxt = trunkPoints_.get(bp.getSourceTag());
    System.err.print(trpTxt);
    System.err.println("****************************************************************");
    String lepTxt = leafPoints_.get(bp.getSourceTag());
    System.err.print(lepTxt);
    System.err.println("****************************************************************");
    String beTxt = bordersExits_.get(bp.getSourceTag());
    System.err.print(beTxt);
    System.err.println("****************************************************************");
    String besTxt = bordersExitsShifted_.get(bp.getSourceTag());
    System.err.print("bs " + besTxt);
    System.err.println("****************************************************************");
    String erTxt = exitRoutes_.get(bp.getSourceTag());
    System.err.print("er " + erTxt);
    System.err.println("****************************************************************");
    String mrgTxt = mergeData_.get(bp.getSourceTag());
    System.err.print("md " + mrgTxt);      
    System.err.println("****************************************************************");
    System.err.print(glueDone_);
    System.err.println("****************************************************************");
    String loTxt = linkOrder_.get(bp.getSourceTag());
    System.err.print(loTxt);
    System.err.println("****************************************************************");
    String plTxt = ptLists_.get(bp.getSourceTag());
    System.err.print(plTxt);
    System.err.println("****************************************************************");
    return;
  }  
  
  /***************************************************************************
  **
  ** Track route needs
  */
  
  public static void trackRouteNeeds(SpecialtyLayoutLinkData sin, SpecialtyLayoutLinkData.RouteNeeds rn) {
    if (!enabled_) {
      return;
    } 
    buf_.setLength(0);
    String srcID = sin.getSrcID();
    String pre = exitRoutes_.get(srcID);
    if (pre == null) {
      buf_.append("Exit route built for " + sin.getSrcID() + ": " + "\n");   
    } else {
      buf_.append(pre);   
    }
    buf_.append(rn.toString());
    buf_.append("\n");
    exitRoutes_.put(srcID, buf_.toString());
    return;
  }

  /***************************************************************************
  **
  ** Marker function
  */
  
  @SuppressWarnings("unused")
  public static void tracePaths(String msg) {
    if (!enabled_) {
      return;
    }     
    return;
  } 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor.  Do not use
  */

  private LayoutFailureTracker() {
  }
}
