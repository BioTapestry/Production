/*
**    Copyright (C) 2003-2009 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.ui;

import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** Gives us an OverlayStateOracle with fixed state
*/

public class FreezeDriedOverlayOracle implements OverlayStateOracle {
    
  private String co_;
  private TaggedSet ts_;
  private NetModuleFree.CurrentSettings cos_;
  private TaggedSet revealed_;

  public FreezeDriedOverlayOracle(String co, TaggedSet ts, NetModuleFree.CurrentSettings cos, TaggedSet revealed) {
    co_ = co;
    ts_ = ts;
    cos_ = cos;
    revealed_ = revealed;
  }
  
  public FreezeDriedOverlayOracle(String co, TaggedSet ts, int currentMaskType, TaggedSet revealed) {
    co_ = co;
    ts_ = ts;
    cos_ = new NetModuleFree.CurrentSettings(currentMaskType);
    revealed_ = revealed;
  }  

  public String getCurrentOverlay() {
    return (co_);
  }

  public TaggedSet getCurrentNetModules() {
    return (ts_);
  }
  
  public NetModuleFree.CurrentSettings getCurrentOverlaySettings() {
    return (cos_);
  }
  
  public TaggedSet getRevealedModules() {
    return (revealed_);
  } 
    
  public boolean showingModuleComponents() {
    return (false);
  }       
  
}  
