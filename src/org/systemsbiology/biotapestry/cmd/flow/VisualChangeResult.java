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


package org.systemsbiology.biotapestry.cmd.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** Visual results for a command
*/

public class VisualChangeResult {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC ENUMS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public enum Viewports {NO_CHANGE, SELECTED, CENTERED};
  
  public enum ViewChange {NONE, NEW_SELECTIONS, CLEAR_SELECTIONS, OVERLAY_STATE, SHOW_MESSAGE};
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private Set<String> selectNodes_;
  private List<Intersection> selectLinks_;
  private boolean clearCurrent_;
  private ArrayList<ViewChange> chgs_;
  private SimpleUserFeedback suf_;
  private TaggedSet matchedModules_;   
  private Viewports vp_;
  private String undoString_;
  private boolean staleView_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  public VisualChangeResult(boolean stale) {
    chgs_ = new ArrayList<ViewChange>();
    staleView_ = stale;
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  public boolean isStale() {
    return (staleView_);
  }
   
  public void setUndoString(String undo) {
    undoString_ = undo;
    return;
  }
  
  public String getUndoString() {
    return (undoString_);
  }
   
  public void setModuleMatches(TaggedSet matches) {
    matchedModules_ = matches;
    return;
  }
 
  public TaggedSet getModuleMatches() {
    return (matchedModules_);
  }
  
  public void setViewport(Viewports vp) {
    vp_ = vp;
    return;
  }

  public void setSelections(Set<String> selectNodes, List<Intersection> selectLinks, boolean clearCurrent) {
    selectNodes_ = selectNodes;
    selectLinks_ = selectLinks;
    clearCurrent_ = clearCurrent;
    return;
  }
  
  public Set<String> getSelectedNodes() {
    return (selectNodes_);
  }
  
  public List<Intersection> getSelectedLinks() {
    return (selectLinks_);
  }
  
  public SimpleUserFeedback getFeedback() {
    return (suf_);
  }
  
  public void setFeedback(SimpleUserFeedback suf) {
    suf_ = suf;
    return;
  }
    
  public void addChange(ViewChange chg) {
    chgs_.add(chg);
    return;
  }
       
  public List<ViewChange> getOperations() {
    return (chgs_);   
  }
    
  public Viewports getViewport() {
    return (vp_);
  }
  
  public boolean doClearCurrent() {
    return (clearCurrent_);
  }
  
}
