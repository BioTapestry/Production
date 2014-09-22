/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.HashSet;

import java.util.SortedMap;

import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Bounds;

/****************************************************************************
**
** Instructions to install a Specialty Layout
*/

public class SpecialtyInstructions {
  
  public ColorTypes colorStrategy;
  public boolean bubblesOn;
  public boolean checkColorOverlap;
  public Map<String, String> nodeColors;
  public Map<String, String> linkColors;
  public Map<String, Point2D> nodeLocations;
  public Map<String, PadCalculatorToo.PadResult> padChanges;
  public Map<String, Integer> lengthChanges; 
  public Map<String, Integer> orientChanges;     
  public Map<String, Integer> extraGrowthChanges;         
  private Map<String, SpecialtyLayoutLinkData> linkData_;
  private SortedMap<Integer, String> sourceOrder_;
  private Rectangle strictGrownBounds_;
  private Rectangle origNodeBounds_;
  private Rectangle origModuleBounds_;
  private Rectangle placedStackBounds_;
  private Rectangle placedStackNodeOnlyBounds_;
  private Rectangle placedFullBounds_;
  private Map<String, Rectangle> placedFullBoundsPerSrc_;
  public LayoutCompressionFramework lcf;
  public String moduleID;  // optional...

  public SpecialtyInstructions(Map<String, PadCalculatorToo.PadResult> padChanges) {
    colorStrategy = ColorTypes.KEEP_COLORS;
    bubblesOn = false;
    checkColorOverlap = false;   
    linkData_ = new HashMap<String, SpecialtyLayoutLinkData>();
    nodeColors = new HashMap<String, String>();
    linkColors = new HashMap<String, String>();
    nodeLocations = new HashMap<String, Point2D>();
    lengthChanges = new HashMap<String, Integer>();
    orientChanges = new HashMap<String, Integer>();      
    extraGrowthChanges = new HashMap<String, Integer>();            
    this.padChanges = padChanges;  // Pad changes need to be shared amoung multiple subsets!  
    origNodeBounds_ = null;
    placedStackBounds_ = null;
    placedStackNodeOnlyBounds_ = null;
    placedFullBounds_ = null;
    origModuleBounds_ = null;
    strictGrownBounds_ = null;
    placedFullBoundsPerSrc_ = new HashMap<String, Rectangle>();
    lcf = null;
    moduleID = null;
  }

  public void setSourceOrder(SortedMap<Integer, String> sourceOrder) {
    sourceOrder_ = sourceOrder;
    return;
  }
    
  public SortedMap<Integer, String> getSourceOrder() {
   return (sourceOrder_);
  }
   
  public void setOrigNodeBounds(Rectangle bounds) {
    origNodeBounds_ = (bounds == null) ? null : (Rectangle)bounds.clone();
    return;
  }
    
  public void setPlacedStackBounds(Rectangle bounds) {
   placedStackBounds_ = (bounds == null) ? null : (Rectangle)bounds.clone();
   return;
  }
  
  public void setPlacedStackNodeOnlyBounds(Rectangle bounds) {
   placedStackNodeOnlyBounds_ = (bounds == null) ? null : (Rectangle)bounds.clone();
   return;
  }
    
  public void setOrigModuleRect(Rectangle bounds) {
    origModuleBounds_ = (bounds == null) ? null : (Rectangle)bounds.clone();
    return;
  }
  
  public boolean hasLinkPointsForSrc(String srcID) {
    return (linkData_.containsKey(srcID));
  }
  
  public Iterator<String> getLinkSrcIterator() {
    return (linkData_.keySet().iterator());
  }  
  
  public SpecialtyLayoutLinkData getLinkPointsForSrc(String srcID) {
    return (linkData_.get(srcID));
  }

  public SpecialtyLayoutLinkData getOrStartLinkPointsForSrc(String srcID) {
    SpecialtyLayoutLinkData slld = linkData_.get(srcID);
    if (slld == null) {
      slld = new SpecialtyLayoutLinkData(srcID);
      linkData_.put(srcID, slld);     
    }
    return (slld);
  }  
  
  public void setLinkPointsForSrc(String srcID, SpecialtyLayoutLinkData slld) {
    linkData_.put(srcID, slld);
    return; 
  }
   
  public Rectangle getOrigModuleRect() {
   return (origModuleBounds_);
  }

  public void setModuleID(String moduleID) {
    this.moduleID = moduleID; 
    return; 
  } 
  // may be null...
  public String getModuleID() {
    return (moduleID);
  }

  public Rectangle getPlacedFullBounds() {
    // Sometimes (e.g. right edge), nodes go out past link bounds.  Gotta include both:
    // Placed full bounds is for links.  Stack bounds include nodes (but not JUST nodes)
    if ((placedStackBounds_ != null) && (placedFullBounds_ != null)) {
      return (placedFullBounds_.union(placedStackBounds_));
    } else {
      return (placedFullBounds_);
    }
  }
  
  public Rectangle getStrictGrownBounds() {
    return (strictGrownBounds_);
  }
   
  public void setStrictGrownBounds(Rectangle strictGrown) {
    strictGrownBounds_ = (strictGrown == null) ? null : (Rectangle)strictGrown.clone();
    return;
  } 

  public Rectangle getPlacedStackBounds() {
    return (placedStackBounds_);
  }
  
  public Rectangle getPlacedStackNodeOnlyBounds() {
    return (placedStackNodeOnlyBounds_);
  }  
  
  public Rectangle getOrigNodeBounds() {
    return (origNodeBounds_);
  }
    
  public Rectangle getLinkBoundsForSrc(String srcID) {
    return (placedFullBoundsPerSrc_.get(srcID));
  }
    
  public void setBoundsForLinks(NetModuleLinkExtractor.SubsetAnalysis sa) {
    HashMap<String, Set<Vector2D>> allDepartures = null;
    HashMap<String, Set<Vector2D>> allArrivals = null;    
    if (moduleID != null) {
      allDepartures = new HashMap<String, Set<Vector2D>>();
      allArrivals = new HashMap<String, Set<Vector2D>>();
      departsAndArrives(sa, allDepartures, allArrivals);
    }
    
    Iterator<String> siit = linkData_.keySet().iterator();        
    while (siit.hasNext()) {
      String srcID = siit.next();
      SpecialtyLayoutLinkData si = linkData_.get(srcID);      
      Rectangle sib = UiUtil.rectFromRect2D(si.getLinkBounds(allArrivals, allDepartures));
      if (sib != null) {
        if (placedFullBounds_ == null) {
          placedFullBounds_ = (Rectangle)sib.clone();
        } else {
          Bounds.tweakBounds(placedFullBounds_, sib);
        }
        placedFullBoundsPerSrc_.put(srcID, sib);
      }
    }
    // See placedFullBounds_ == null if module has no links...
    if ((origModuleBounds_ != null) && (placedFullBounds_ != null)) {  // DO NOT ALLOW MODULE SHRINKAGE!!!!
      double oH = origModuleBounds_.getHeight();
      double pH = placedFullBounds_.getHeight();
      double oW = origModuleBounds_.getWidth();
      double pW = placedFullBounds_.getWidth();        
            
      double diffH = (oH > pH) ? oH - pH : 0.0;
      double diffW = (oW > pW) ? oW - pW : 0.0;
      diffH = UiUtil.forceToGridValueMax(diffH / 2.0, UiUtil.GRID_SIZE);
      diffW = UiUtil.forceToGridValueMax(diffW / 2.0, UiUtil.GRID_SIZE);

      if ((diffH > 0.0) || (diffW > 0.0)) {
        placedFullBounds_ = UiUtil.rectFromRect2D(UiUtil.padTheRect(placedFullBounds_, diffH, diffW));        
      }
    }
  
    return;
  }
  
  private void departsAndArrives(NetModuleLinkExtractor.SubsetAnalysis sa, 
                                 Map<String, Set<Vector2D>> allDepartures, 
                                 Map<String, Set<Vector2D>> allArrivals) {
    Iterator<String> impit = sa.getInterModPathKeys();
    while (impit.hasNext()) {
      String modID = impit.next();
      NetModuleLinkExtractor.ExtractResultForSource er4s = sa.getExtractResultForSource(modID);
   
      if (modID.equals(moduleID)) {
        Map<String, Set<Vector2D>> departs = er4s.getAllDepartures();
        if (departs != null) {
          allDepartures.putAll(departs);
        }
      } else {
        Map<String, Vector2D> ad = er4s.getArrivalDirs(moduleID);
        if (ad != null) {
          Iterator<String> kit = ad.keySet().iterator();
          while (kit.hasNext()) {
            String src = kit.next();
            Set<Vector2D> ad4s = allArrivals.get(src);
            if (ad4s == null) {
              ad4s = new HashSet<Vector2D>();
              allArrivals.put(src, ad4s);
            }
            Vector2D myArr = ad.get(src);
            ad4s.add(myArr);
          }  
        }
      }
    }
    return;
  }
  
  public Map<String, SpecialtyLayoutLinkData> getShiftedLinkDataCopy(Vector2D shiftVec) {
    HashMap<String, SpecialtyLayoutLinkData> retval = new HashMap<String, SpecialtyLayoutLinkData>();
    Iterator<String> siit = linkData_.keySet().iterator();
    while (siit.hasNext()) {
      String srcID = siit.next();      
      SpecialtyLayoutLinkData si = linkData_.get(srcID);
      SpecialtyLayoutLinkData siCopy = si.clone();
      siCopy.shift(shiftVec);
      retval.put(srcID, siCopy);
    }
    return (retval);
  }
  
  public void shiftNodesOnly(Vector2D shiftVec) {
    Iterator<Point2D> locit = nodeLocations.values().iterator();
    while (locit.hasNext()) {
      Point2D loc = locit.next();
      loc.setLocation(shiftVec.add(loc));        
    }
    return;
  }
 
  public void shift(Vector2D shiftVec, boolean skipNodes) {
    Iterator<SpecialtyLayoutLinkData> siit = linkData_.values().iterator();        
    while (siit.hasNext()) {
      SpecialtyLayoutLinkData si = siit.next();
      si.shift(shiftVec);
    }

    if (!skipNodes) {
      Iterator<Point2D>  locit = nodeLocations.values().iterator();
      while (locit.hasNext()) {
        Point2D loc = locit.next();
        loc.setLocation(shiftVec.add(loc));        
      }
    }

    if (placedStackBounds_ != null) {
      Point2D loc = placedStackBounds_.getLocation();
      loc = shiftVec.add(loc);
      placedStackBounds_.setLocation((int)loc.getX(), (int)loc.getY());
    } 
    
    if (placedStackNodeOnlyBounds_ != null) {
      Point2D loc = placedStackNodeOnlyBounds_.getLocation();
      loc = shiftVec.add(loc);
      placedStackNodeOnlyBounds_.setLocation((int)loc.getX(), (int)loc.getY());
    } 
      
    if (placedFullBounds_ != null) {
      Point2D loc = placedFullBounds_.getLocation();
      loc = shiftVec.add(loc);
      placedFullBounds_.setLocation((int)loc.getX(), (int)loc.getY());
    }
    Iterator<Rectangle> pfbit = placedFullBoundsPerSrc_.values().iterator();        
    while (pfbit.hasNext()) {
      Rectangle rec = pfbit.next();
      Point2D loc = rec.getLocation();
      loc = shiftVec.add(loc);
      rec.setLocation((int)loc.getX(), (int)loc.getY());
    }

    if (lcf != null) {
      Iterator<Rectangle> bit = lcf.getBounds();
      while (bit.hasNext()) {
        Rectangle brect = bit.next();
        Point2D loc = brect.getLocation();
        loc = shiftVec.add(loc);
        brect.setLocation((int)loc.getX(), (int)loc.getY());
      }
    }
    return;
  }
}
