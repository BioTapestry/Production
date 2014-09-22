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

package org.systemsbiology.biotapestry.ui.freerender;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;

import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.SuggestedDrawStyle;
import org.systemsbiology.biotapestry.ui.ResolvedDrawStyle;
import org.systemsbiology.biotapestry.ui.PerLinkDrawStyle;

/***************************************************************************
**
** Link segment in a draw tree
*/

public class DrawTreeSegment {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
    
  private LinkSegmentID myID_;
  private LinkSegmentID parent_;
  private LinkSegment geomSeg_; 
  private SuggestedDrawStyle perSegProps_;  // May be null
  private ArrayList<LinkTaggedPerLinkProps> perLinkProps_;
  private HashMap<String, Double> perLinkModulation_;
  private DrawTreeCornerInfo footInfo_;
  private ResolvedDrawStyle resolved_;
  private boolean drawn_;
  private boolean selected_;
  private boolean isActive_;
  private int pathCount_;
  private String whoDraws_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////  

  DrawTreeSegment(LinkSegmentID myID, LinkSegment geomSeg, SuggestedDrawStyle props) {
    myID_ = myID;
    geomSeg_ = geomSeg; 
    perSegProps_ = props;
    perLinkProps_ = new ArrayList<LinkTaggedPerLinkProps>();
    perLinkModulation_ = new HashMap<String, Double>();
    footInfo_ = new DrawTreeCornerInfo();
    resolved_ = null;
    parent_ = null;
    drawn_ = false;
    selected_ = false;
    isActive_ = false;
    pathCount_ = 0;
    whoDraws_ = null;
  }

  void incrementPathCount() {
    pathCount_++;
    return;
  }    

  void setSelected() {
    selected_ = true;
    return;
  }

  boolean isSelected() {
    return (selected_);
  }        

  void setTag() {
    drawn_ = true;
    return;
  }

  void setWhoDraws(String linkID) {
    whoDraws_ = linkID;
    return;
  }  

  String getWhoDraws() {
    return (whoDraws_);
  }      

  void clearTag() {
    drawn_ = false;
    return;
  } 

  boolean isDrawn() {
    return (drawn_);
  } 

  boolean isActive() {
    return (isActive_);
  } 

  void setActive() {
    isActive_ = true;
    return;
  } 

  void setParent(LinkSegmentID parent) {
    parent_ = parent;
    return;
  }

  void setResolvedStyle(ResolvedDrawStyle style) {
    resolved_ = style;
    return;
  } 

  ResolvedDrawStyle getResolvedStyle() {
    return (resolved_);
  }     

  LinkSegmentID getParent() {
    return (parent_);
  }

  LinkSegmentID getID() {
    return (myID_);
  }    

  Vector2D getNormal() {
    return (geomSeg_.getNormal());
  }

  Vector2D getRun() {
    return (geomSeg_.getRun());
  }

  Point2D getStart() {
    return (geomSeg_.getStart());
  }

  double getLength() {
    return (geomSeg_.getLength());
  }    

  Point2D getEnd() {
    return (geomSeg_.getEnd());
  }  

  SuggestedDrawStyle getPerSegmentStyle() {
    return (perSegProps_);
  }

  int getPerLinkStyleCount() {
    return (perLinkProps_.size());
  }

  boolean needsBranchPoint() {
    return (footInfo_.activeKids >= 2);
  }

  void addActiveChild() {
    footInfo_.activeKids++;
    return;
  }    

  void addChild() {
    footInfo_.totalKids++;
    return;
  }

  @SuppressWarnings("unused")
  private boolean calcStraight(LinkSegment kidSeg, LinkSegment mySeg) {
    Vector2D kidRun = kidSeg.getRun();
    Vector2D myRun = mySeg.getRun();
    double dot = kidRun.dot(myRun);
    return (Math.abs(1.0 - Math.abs(dot)) < 1.0E-3);
  }

  void addPerLinkProps(PerLinkDrawStyle perLink, String linkID, 
                       PerLinkDrawStyle perLinkForEvidence, Double perLinkActivity) {
    
    //
    // Evidence setting always overrides the layout-driven option
    //
    
    if (perLinkForEvidence != null) {
       perLinkProps_.add(new LinkTaggedPerLinkProps(true, linkID, perLinkForEvidence));
    } else if (perLink != null) {
       perLinkProps_.add(new LinkTaggedPerLinkProps(false, linkID, perLink));
    }

    if (perLinkActivity != null) {
      perLinkModulation_.put(linkID, perLinkActivity);
    }
    return;
  }

  /***************************************************************************
  **
  ** Resolve the drawing style to use:
  */

  ResolvedDrawStyle resolveDrawStyle(LinkProperties lp, boolean isGhosted, int activityDrawChange, boolean forModules, ColorResolver cRes) { 

    //
    // Start with the values for the whole tree, then add on top of it:
    //

    SuggestedDrawStyle currStyle = lp.getDrawStyle();
    ResolvedDrawStyle retval = resolvePerLinkProps(currStyle, isActive_, isGhosted, activityDrawChange, forModules, cRes);

    SuggestedDrawStyle perSeg = getPerSegmentStyle();
    if (perSeg != null) {
      retval.masterUpdate(perSeg, isActive_, isGhosted, forModules, cRes);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Figure out per-link winners.  If none, this does the Suggested->Resolved 
  ** conversion anyway
  */

  ResolvedDrawStyle resolvePerLinkProps(SuggestedDrawStyle currStyle, 
                                        boolean isActive, boolean isGhosted, 
                                        int activityDrawChange, boolean forModules, ColorResolver cRes) {
    int num = perLinkProps_.size();
    boolean colorMatters = (isActive && !isGhosted);

    //
    // Easy case: Just convert
    //
    if ((num == 0) && perLinkModulation_.isEmpty()) {
      currStyle = currStyle.clone();
      currStyle.fillWithDefaults();
      return (new ResolvedDrawStyle(currStyle, colorMatters, forModules, cRes));    
    } 

    //
    // Figure out if all the shared congruent styles here are equal:
    //

    boolean sharedCongruentsEqual = true;
    boolean sharedCongruentsOnly = true;
    HashSet<String> styledLinks = new HashSet<String>();
    SuggestedDrawStyle shcgrSty = null;
    for (int i = 0; i < num; i++) {
      LinkTaggedPerLinkProps ltplp = perLinkProps_.get(i);
      PerLinkDrawStyle plds = ltplp.style;
      styledLinks.add(ltplp.linkID);
      if (plds.getExtent() != PerLinkDrawStyle.SHARED_CONGRUENT) {
        sharedCongruentsOnly = false;
        continue;
      }
      if (shcgrSty == null) {
        shcgrSty = plds.getDrawStyle();
      } else {
        if (!shcgrSty.equals(plds.getDrawStyle())) {
          sharedCongruentsEqual = false;
        }
      }
    }
    if (styledLinks.size() < pathCount_) {
      sharedCongruentsOnly = false;        
    }

    //
    // Figure out how to mix the styles:
    // Differences in thickness: the thickest wins
    // Differences in style: The most solid wins
    // Differences in color: Average the colors
    // NOTE: We start with unspecified values, then merge with tree defaults
    // after.  (e.g. don't want thick tree default to override thin per-link!)
    //

    int maxThick = SuggestedDrawStyle.NO_THICKNESS_SPECIFIED;
    int maxStyle = SuggestedDrawStyle.NO_STYLE;
    int[] colorSums = (colorMatters) ? new int[3] : null;
    int numCol = 0;

    for (int i = 0; i < num; i++) {
      LinkTaggedPerLinkProps ltplp = perLinkProps_.get(i);
      PerLinkDrawStyle plds = ltplp.style;      

      //
      // In the presence of multiple link styles, those that are only
      // for unique segments are thrown away:
      //
      if ((pathCount_ > 1) && (plds.getExtent() == PerLinkDrawStyle.UNIQUE)) {
        continue;
      }
      //
      // We ditch shared congruents if they don't match. Note that back to source
      // extents will survive this.
      //

      if ((plds.getExtent() == PerLinkDrawStyle.SHARED_CONGRUENT) && 
          (!sharedCongruentsEqual || !sharedCongruentsOnly)) {
        continue;
      }

      SuggestedDrawStyle curr = plds.getDrawStyle();
      maxThick = curr.returnWinningThick(maxThick, forModules);
      maxStyle = curr.returnWinningStyle(maxStyle);
      if (colorMatters) {
        numCol = curr.sumColors(cRes, colorSums, numCol);
      }
    }

    Color newCol = null;
    if (colorMatters) {
      if (numCol != 0) {
        for (int i = 0; i < 3; i++) {
          colorSums[i] = colorSums[i] / numCol;
        }
        newCol = new Color(colorSums[0], colorSums[1], colorSums[2]);
      }
    }
    
    //
    // Resolve the drawing style, then modulate it based upon activity values:
    //
    

    ResolvedDrawStyle rds = new ResolvedDrawStyle(currStyle, colorMatters, maxThick, maxStyle, newCol, forModules, cRes);
    if (perLinkModulation_.isEmpty()) {
      return (rds);
    }
 
    //
    // Choose the activity value for a segment based on the maxium activity:
    //
    
    double maxLevel = 0.0;
    int numLevels = 0;
    Iterator<Double> plmit = perLinkModulation_.values().iterator();
    while (plmit.hasNext()) {
      Double perLinkActivity = plmit.next();
      double plaVal = perLinkActivity.doubleValue();
      if (plaVal > maxLevel) {
        maxLevel = plaVal;
      }
      numLevels++;
    }
    if (pathCount_ > numLevels) {
      maxLevel = 1.0;
    }

    if (maxLevel != 1.0) {
      if ((activityDrawChange == DisplayOptions.LINK_ACTIVITY_COLOR) ||
          (activityDrawChange == DisplayOptions.LINK_ACTIVITY_BOTH)) {
        rds.modulateColor(maxLevel);
      }
      if ((activityDrawChange == DisplayOptions.LINK_ACTIVITY_THICK) ||
          (activityDrawChange == DisplayOptions.LINK_ACTIVITY_BOTH)) {
        rds.modulateThickness(maxLevel);
      }
    }

    return (rds);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Corner info for the foot of each segment
  */

  static class DrawTreeCornerInfo {
    int totalKids;
    int activeKids;
  }

  /***************************************************************************
  **
  ** For holding per-link props
  */

  static class LinkTaggedPerLinkProps {
    boolean fromEvidence;
    String linkID;
    PerLinkDrawStyle style;
    
    LinkTaggedPerLinkProps(boolean fromEvidence, String linkID, PerLinkDrawStyle style) {
      this.fromEvidence = fromEvidence; 
      this.linkID = linkID;
      this.style = style;
    }
  }
}
