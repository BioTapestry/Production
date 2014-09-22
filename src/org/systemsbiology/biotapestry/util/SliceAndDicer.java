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

package org.systemsbiology.biotapestry.util;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.EnumMap;

/****************************************************************************
**
** Handles slicing up a rectangle into pieces to isolate ownership to
** other rectangles
*/

public class SliceAndDicer  {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  public enum Claim {OWNED_BY_GROUP, UNCLAIMED, MULTI_CLAIMED, BOUNDING_A_GROUP};

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  

  public enum SliceResult {NO_INTERSECTION_, CHUNKS_CONTAINED_IN_SHAPE_, SHAPE_CONTAINED_IN_CHUNKS_, CHUNK_AND_SHAPE_SLICE_};
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  public SliceAndDicer() {

  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  /***************************************************************************
  **
  ** Slice a single rectangle by the given bounds
  */
  
  public List<OneSliceResult> sliceARectByAllBounds(Map<String, Rectangle> boundsForGroups, Map<String, Integer> groupZOrder, Rectangle2D oldShape, boolean isName) {
    
    //
    // If the shape is sliced by one or more regions, it
    // must be broken apart until each region owns a unique
    // piece. (What do we do about overlapping regions?)
    // We attempt to slice into a minimum number, but the
    // approach is stupid and far from optimal
    // (would that be NP??).
    //
    
    Map<GroupAndChunk, Rectangle2D> chunkedBounds = sliceBFG(boundsForGroups, groupZOrder);
        
    PreSliceResult psr = doEverythingButSlice(chunkedBounds, oldShape, isName);
    if (psr.osr != null) {
      ArrayList<OneSliceResult> retval = new ArrayList<OneSliceResult>();
      if (psr.osr.status == Claim.BOUNDING_A_GROUP) {             
        retval.addAll(sliceOffBoundaries(chunkedBounds, oldShape, psr, isName));
      } else {
        retval.add(psr.osr);       
      }
      return (retval);
    }

    return (sliceTheShape(chunkedBounds, oldShape, psr, isName));
  }    

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  public static class OneSliceResult {
    public Claim status;
    public HashSet<String> groups;
    public Rectangle2D shape;
    public boolean isName;
    
    OneSliceResult(Rectangle2D oldShape, boolean isName) {
      status = Claim.UNCLAIMED;
      shape = (Rectangle2D)oldShape.clone();
      this.isName = isName;
    }
    
    OneSliceResult(String grpID, Rectangle2D oldShape, boolean withBounds, boolean isName) {
      status = (withBounds) ? Claim.BOUNDING_A_GROUP : Claim.OWNED_BY_GROUP;
      groups = new HashSet<String>();
      groups.add(grpID);
      shape = (Rectangle2D)oldShape.clone();
      this.isName = isName;
    }    
      
    OneSliceResult(Set<String> groups, Rectangle2D oldShape, boolean isName) {
      status = Claim.MULTI_CLAIMED;
      this.groups = new HashSet<String>(groups);
      this.shape = (Rectangle2D)oldShape.clone();
      this.isName = isName;
    }
   
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////     

  /***************************************************************************
  **
  ** Slice a single rectangle by the given bounds
  */
  
  private PreSliceResult doEverythingButSlice(Map<GroupAndChunk, Rectangle2D> chunkedBounds, Rectangle2D oldShape, boolean isName) {
    
    //
    // If the shape is sliced by one or more regions, it
    // must be broken apart until each region owns a unique
    // piece. (What do we do about overlapping regions?)
    // We attempt to slice into a minimum number, but the
    // approach is stupid and far from optimal
    // (would that be NP??).
    //
    
    EnumMap<SliceResult, Set<GroupAndChunk>> chunksPerRes = new EnumMap<SliceResult, Set<GroupAndChunk>>(SliceResult.class);
    for (SliceResult sr: SliceResult.values()) {
      chunksPerRes.put(sr, new HashSet<GroupAndChunk>());
    }
    HashMap<GroupAndChunk, Rectangle2D> interRects = new HashMap<GroupAndChunk, Rectangle2D>();
 
    PreSliceResult retval = new PreSliceResult(chunksPerRes, interRects);       

    if (chunkedBounds.isEmpty()) {
      return (retval.setOsr(new OneSliceResult(oldShape, isName)));
    }    
      
    buildIntersectionInfo(chunkedBounds, oldShape, chunksPerRes, interRects);
    int numChunks = chunkedBounds.size();
 
    //
    // Nobody inside shape, and shape contains nobody, we are done!
    //
 
    retval.unclaimed = (chunksPerRes.get(SliceResult.NO_INTERSECTION_).size() == numChunks);
    retval.chunksFullyInShape = chunksPerRes.get(SliceResult.CHUNKS_CONTAINED_IN_SHAPE_).size();
    retval.shapeFullyInChunks = chunksPerRes.get(SliceResult.SHAPE_CONTAINED_IN_CHUNKS_).size();
    retval.numToSlice = chunksPerRes.get(SliceResult.CHUNK_AND_SHAPE_SLICE_).size();
      
    if (retval.unclaimed) {
      return (retval.setOsr(new OneSliceResult(oldShape, isName)));
    }

    //
    // No slicing?  Great!  Will just be assigned as needed:
    //

    if (retval.numToSlice == 0) {
      //
      // If a shape in contained completely inside one and only
      // one region, it is owned by that region.
      //
      if (retval.shapeFullyInChunks == 1) {
        if (retval.chunksFullyInShape != 0) {
          throw new IllegalStateException();
        }
        GroupAndChunk gac = chunksPerRes.get(SliceResult.SHAPE_CONTAINED_IN_CHUNKS_).iterator().next();
        return (retval.setOsr(new OneSliceResult(gac.grp, oldShape, false, isName)));
      } else if (retval.shapeFullyInChunks > 1) {
        throw new IllegalStateException();
      } // so by now, shapeFullyInChunks == 0;     
      //
      // If a shape contains one and only one chunk, it is owned by that chunk's region.  But we are
      // going to slice off the areas around it into unclaimed regions.
      //
      if (retval.chunksFullyInShape == 1) {
        GroupAndChunk gac = (GroupAndChunk)chunksPerRes.get(SliceResult.CHUNKS_CONTAINED_IN_SHAPE_).iterator().next();
        return (retval.setOsr(new OneSliceResult(gac.grp, oldShape, true, isName)));
      } else if (retval.chunksFullyInShape > 1) {
        return (retval);
      } // so by now, chunksFullyInShape == 0;
      throw new IllegalStateException();
    } else { // gotta slice...
      return (retval);   
    }
  }

  /***************************************************************************
  **
  ** Break the groups bounds up into chunks to eliminate nesting.
  */
  
  private Map<GroupAndChunk, Rectangle2D> sliceBFG(Map<String, Rectangle> boundsForGroups, Map<String, Integer> groupZOrder) {
    ChunkTracker ct = new ChunkTracker();
    Map<GroupAndChunk, Rectangle2D> chunked = basicSliceBFG(boundsForGroups, ct);
  
    int lastChunked = -1;
    int chunkedSize = chunked.size();
    while (lastChunked != chunkedSize) {
      Iterator<GroupAndChunk> cit1 = chunked.keySet().iterator();
      boolean broken = false;
      //
      // Bogus double pass.  Should use only upper or lower
      // triangle and halve the effort:
      //
      while (cit1.hasNext() && !broken) {
        GroupAndChunk gac1 = cit1.next();
        Rectangle2D rect1 = (Rectangle2D)chunked.get(gac1);
        Iterator<GroupAndChunk> cit2 = chunked.keySet().iterator();      
        while (cit2.hasNext()) {
          GroupAndChunk gac2 = cit2.next();
          if (gac2.equals(gac1)) {
            continue;
          }
          Rectangle2D rect2 = (Rectangle2D)chunked.get(gac2);
          Rectangle2D interRect = rect2.createIntersection(rect1);
          // Intersects in a non-zero area:
          if ((interRect.getHeight() > 0.0) && (interRect.getWidth() > 0.0)) {
            breakToChunks(chunked, gac1, gac2, interRect, groupZOrder, ct);
            broken = true;
            break;
          }
        }
      }
      lastChunked = chunkedSize;
      chunkedSize = chunked.size();
    }
    
    return (chunked);
  } 
  
  /***************************************************************************
  **
  ** Break the groups bounds up into chunks to eliminate nesting.  This is
  ** the basic first step.
  */
  
  private Map<GroupAndChunk, Rectangle2D> basicSliceBFG(Map<String, Rectangle> boundsForGroups, ChunkTracker ct) {
    HashMap<GroupAndChunk, Rectangle2D> retval = new HashMap<GroupAndChunk, Rectangle2D>();
    Iterator<String> bfgit = boundsForGroups.keySet().iterator();
    while (bfgit.hasNext()) {
      String grpID = bfgit.next();
      Rectangle rect = boundsForGroups.get(grpID);
      int next = ct.getNextChunkNum(grpID);
      GroupAndChunk gac = new GroupAndChunk(grpID, next);
      retval.put(gac, rect);
    }
    return (retval);
  }
 
 /***************************************************************************
  **
  ** Break chunks into smaller chunks
  */
  
  private void fragment(Map<GroupAndChunk, Rectangle2D> chunked, GroupAndChunk gac, Rectangle2D fragRect, Rectangle2D interRect, ChunkTracker ct) {  
    SlicedRectangle sl = new SlicedRectangle(fragRect, interRect);
    List<SlicedRectangle.MinMaxOpt> sliceOpts = sl.getFirstSliceOptions();
    if (sliceOpts.size() == 0) {
      return;
    }
    SlicedRectangle.MinMaxOpt opt = sliceOpts.get(0);
    SlicedRectangle.SlicedRectangleSplit srs = sl.slice(opt);
    chunked.put(gac, srs.first);
    int next = ct.getNextChunkNum(gac.grp);
    GroupAndChunk gac2 = new GroupAndChunk(gac.grp, next);
    chunked.put(gac2, srs.second);
    return;
  }
  
  /***************************************************************************
  **
  ** Break chunks into smaller chunks
  */
  
  private void breakToChunks(Map<GroupAndChunk, Rectangle2D> chunked, GroupAndChunk gac1, GroupAndChunk gac2, 
                             Rectangle2D interRect, Map<String, Integer> groupZOrder, ChunkTracker ct) {
 
    Rectangle2D rect1 = chunked.get(gac1);  
    Rectangle2D rect2 = chunked.get(gac2);
    int z1 = groupZOrder.get(gac1.grp).intValue();
    int z2 = groupZOrder.get(gac2.grp).intValue();
    boolean z1InFront = (z1 > z2);
    
    if (interRect.equals(rect1)) {
      if (z1InFront) {
        if (interRect.equals(rect2)) { // rect2 == but obscured
          chunked.remove(gac2);
        } else {
          fragment(chunked, gac2, rect2, interRect, ct);
        }
      } else { // z1 is hidden
        chunked.remove(gac1);
      }
    } else if (interRect.equals(rect2)) {
      if (z1InFront) {
        fragment(chunked, gac2, rect2, interRect, ct);
      } else { // z1 is hidden
        chunked.remove(gac1);
      }
    } else if (z1InFront) {
      fragment(chunked, gac2, rect2, interRect, ct);      
    } else {
      fragment(chunked, gac1, rect1, interRect, ct);
    }
    return;
  }   
  
 
  /***************************************************************************
  **
  ** Break the rectangle up
  */
  
  private List<OneSliceResult> sliceTheShape(Map<GroupAndChunk, Rectangle2D> chunkedBounds, Rectangle2D oldShape, PreSliceResult psr, boolean isName) {
    //
    // Chop up the shape into up to subrectangles:
    //         
    
    ArrayList<SliceChoice> choices = new ArrayList<SliceChoice>();
    
    Iterator<GroupAndChunk> git;
    if (psr.chunksPerRes.get(SliceResult.CHUNK_AND_SHAPE_SLICE_).isEmpty()) {
      git = psr.chunksPerRes.get(SliceResult.CHUNKS_CONTAINED_IN_SHAPE_).iterator();
    } else {
      git = psr.chunksPerRes.get(SliceResult.CHUNK_AND_SHAPE_SLICE_).iterator();
    }
    
    while (git.hasNext()) {
      GroupAndChunk gac = git.next();
      Rectangle2D interRect = psr.interRects.get(gac);    
      SlicedRectangle sl = new SlicedRectangle(oldShape, interRect);
      List<SlicedRectangle.MinMaxOpt> sliceOpts = sl.getFirstSliceOptions();
      int numOpts = sliceOpts.size();
      for (int i = 0; i < numOpts; i++) {
        SlicedRectangle.MinMaxOpt opt = sliceOpts.get(i);
        SlicedRectangle.SlicedRectangleSplit srs = sl.slice(opt);
        PreSliceResult psr1 = doEverythingButSlice(chunkedBounds, srs.first, isName);
        PreSliceResult psr2 = doEverythingButSlice(chunkedBounds, srs.second, isName);
        choices.add(new SliceChoice(opt, psr1, psr2, srs.first, srs.second));
      }
    }
    
    //
    // Rate the choices!
    //
    
    int numChoices = choices.size();
    SliceChoice choice = null;
    SliceChoice lastDitch = null;
    SliceChoice finalSlice = null;
    int minSlices = Integer.MAX_VALUE;
    for (int i = 0; i < numChoices; i++) {
      SliceChoice nextChoice = (SliceChoice)choices.get(i);

      //
      // If we need to slice MORE, we are heading in the wrong direction:
      //
    
      int val = nextChoice.psr1.numToSlice + nextChoice.psr2.numToSlice;
      if (val > psr.numToSlice) {
        continue;
      }
    
      //
      // If slicing makes no change, prefer somebody else:
      //
      
      if (((nextChoice.psr1.numToSlice == psr.numToSlice) && (nextChoice.psr2.numToSlice == 0)) ||
          ((nextChoice.psr2.numToSlice == psr.numToSlice) && (nextChoice.psr1.numToSlice == 0))) {
        
        //
        // same number of slices as before, but at least the groups are being successfully chopped apart.
        // So it is a valid choice:
        //
        
        if ((nextChoice.psr1.chunksFullyInShape < psr.chunksFullyInShape) && (nextChoice.psr2.chunksFullyInShape < psr.chunksFullyInShape)) {       
          lastDitch = nextChoice;
        }
        
        if (((nextChoice.psr1.numToSlice == 1) && (nextChoice.psr2.numToSlice == 0)) ||
           ((nextChoice.psr2.numToSlice == 1) && (nextChoice.psr1.numToSlice == 0))) { 
          finalSlice = nextChoice;
        }
        
        
        continue;
      }
 
      //
      // Go for the choice with the smallest number of slices:
      //
      
      if (val < minSlices) {
        minSlices = val;
        choice = nextChoice;
      }
    }
    
    if (choice == null) {
      choice = lastDitch;
    }     
    if (choice == null) {
      choice = finalSlice;
    }     
    if (choice == null) {
      choice = (SliceChoice)choices.get(0);
    }     

    ArrayList<OneSliceResult> retval = new ArrayList<OneSliceResult>();
    if (choice.psr1.osr == null) {
      retval.addAll(sliceTheShape(chunkedBounds, choice.shape1, choice.psr1, isName));
    } else if (choice.psr1.osr.status == Claim.BOUNDING_A_GROUP) { 
      retval.addAll(sliceOffBoundaries(chunkedBounds, choice.shape1, choice.psr1, isName));
    } else {
      retval.add(choice.psr1.osr);
    }
    if (choice.psr2.osr == null) {
      retval.addAll(sliceTheShape(chunkedBounds, choice.shape2, choice.psr2, isName));
    } else if (choice.psr2.osr.status == Claim.BOUNDING_A_GROUP) { 
      retval.addAll(sliceOffBoundaries(chunkedBounds, choice.shape2, choice.psr2, isName));
    } else {
      retval.add(choice.psr2.osr);
    }
    return (retval);    
  }
  
  
  /***************************************************************************
  **
  ** Once we have a group completely contained in a region, chop off the
  ** boundaries to make them unclaimed
  */
  
  private List<OneSliceResult> sliceOffBoundaries(Map<GroupAndChunk, Rectangle2D> chunkedBounds, Rectangle2D oldShape, PreSliceResult psr, boolean isName) {
    //
    // Chop up the shape into up to subrectangles:
    //         

    ArrayList<OneSliceResult> retval = new ArrayList<OneSliceResult>();
    GroupAndChunk gac = psr.chunksPerRes.get(SliceResult.CHUNKS_CONTAINED_IN_SHAPE_).iterator().next();
    Rectangle2D interRect = (Rectangle2D)psr.interRects.get(gac);    
    SlicedRectangle sl = new SlicedRectangle(oldShape, interRect);
    List<SlicedRectangle.MinMaxOpt> options = sl.getFirstSliceOptions();
    if (options.isEmpty()) {
      retval.add(new OneSliceResult(gac.grp, oldShape, false, isName));
      return (retval);
    }
    SlicedRectangle.MinMaxOpt opt = sl.getFirstSliceOptions().get(0);
    
    SlicedRectangle.SlicedRectangleSplit srs = sl.slice(opt);
    PreSliceResult psr1 = doEverythingButSlice(chunkedBounds, srs.first, isName);
    PreSliceResult psr2 = doEverythingButSlice(chunkedBounds, srs.second, isName);   
   
    if (psr1.osr == null) {
      retval.addAll(sliceTheShape(chunkedBounds, srs.first, psr1, isName));
    } else if (psr1.osr.status == Claim.BOUNDING_A_GROUP) { 
      retval.addAll(sliceOffBoundaries(chunkedBounds, srs.first, psr1, isName));
    } else {
      retval.add(psr1.osr);
    }
    if (psr2.osr == null) {
      retval.addAll(sliceTheShape(chunkedBounds, srs.second, psr2, isName)); 
    } else if (psr2.osr.status == Claim.BOUNDING_A_GROUP) { 
      retval.addAll(sliceOffBoundaries(chunkedBounds, srs.second, psr2, isName));
    } else {
      retval.add(psr2.osr);
    }
    return (retval);    
  }   

 /***************************************************************************
  **
  ** Slice a single rectangle by the given bounds
  */
  
  private void buildIntersectionInfo(Map<GroupAndChunk, Rectangle2D> chunkedBounds, Rectangle2D oldShape, 
                                     EnumMap<SliceResult, Set<GroupAndChunk>> chunksPerRes, HashMap<GroupAndChunk, Rectangle2D> interRects) {    
    Iterator<GroupAndChunk> bfgkit = chunkedBounds.keySet().iterator();
    while (bfgkit.hasNext()) {
      GroupAndChunk gac = bfgkit.next();
      Rectangle2D regBounds = chunkedBounds.get(gac);
      IBResult result = intersectBounds(regBounds, oldShape);
      chunksPerRes.get(result).add(gac);
      if (result.interRect != null) {
        interRects.put(gac, result.interRect);
      }
    }
    return;
  }
      
  /***************************************************************************
  **
  ** Figure out how two rectangles interact
  */
 
  private IBResult intersectBounds(Rectangle2D groupBounds, Rectangle2D shape) {
    Rectangle2D interRect = groupBounds.createIntersection(shape);
    // Intersects in a non-zero area:
    if ((interRect.getHeight() > 0.0) && (interRect.getWidth() > 0.0)) {
      if (interRect.equals(groupBounds)) {
        return (new IBResult(SliceResult.CHUNKS_CONTAINED_IN_SHAPE_, interRect));
      } else if (interRect.equals(shape)) {
        return (new IBResult(SliceResult.SHAPE_CONTAINED_IN_CHUNKS_, interRect));
      } else {
        return (new IBResult(SliceResult.CHUNK_AND_SHAPE_SLICE_, interRect));
      }
    } else {
      return (new IBResult(SliceResult.NO_INTERSECTION_, null));
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  private static class SliceChoice {
    @SuppressWarnings("unused")
    SlicedRectangle.MinMaxOpt opt;
    PreSliceResult psr1;
    PreSliceResult psr2;
    Rectangle2D shape1; 
    Rectangle2D shape2;
    
    SliceChoice(SlicedRectangle.MinMaxOpt opt, PreSliceResult psr1, PreSliceResult psr2, Rectangle2D shape1, Rectangle2D shape2) {
      this.opt = opt;
      this.psr1 = psr1;
      this.psr2 = psr2;
      this.shape1 = shape1;
      this.shape2 = shape2;
    }
  } 
  
 
  private static class IBResult {
    @SuppressWarnings("unused")
    SliceResult result;
    Rectangle2D interRect;
    
    IBResult(SliceResult result, Rectangle2D interRect) {
      this.result = result;
      this.interRect = interRect;
    }
  } 

  private static class PreSliceResult {
    OneSliceResult osr;
    boolean unclaimed;
    int chunksFullyInShape;
    int shapeFullyInChunks;
    int numToSlice;
    EnumMap<SliceResult, Set<GroupAndChunk>> chunksPerRes;
    HashMap<GroupAndChunk, Rectangle2D> interRects;
  
    PreSliceResult(EnumMap<SliceResult, Set<GroupAndChunk>> chunksPerRes, HashMap<GroupAndChunk, Rectangle2D> interRects) {
      this.unclaimed = true;
      this.chunksPerRes = chunksPerRes;
      this.interRects = interRects;
    }
    
    PreSliceResult setOsr(OneSliceResult osr) {
      this.osr = osr;
      return (this);
    }
    
    @SuppressWarnings("unused")
    int rateIt() {
      return (numToSlice);
    }
  }
 
  public static class ChunkTracker {
  
    private HashMap<String, Integer> currMaxChunk_;
    
    ChunkTracker() {
      currMaxChunk_ = new HashMap<String, Integer>();
    }
    
    int getNextChunkNum(String grp) {
      int retval;
      Integer cmc = (Integer)currMaxChunk_.get(grp);
      if (cmc == null) {
        retval = 0;
      } else {
        retval = cmc.intValue() + 1;        
      }
      cmc = new Integer(retval);
      currMaxChunk_.put(grp, cmc);
      return (retval);
    }
  }
 
  public static class GroupAndChunk implements Cloneable {
    
    public String grp;
    public int chunkNum;
    
    public GroupAndChunk(String groupID, int chunkNum) {
      this.grp = groupID;
      this.chunkNum = chunkNum;
    }
    
    public GroupAndChunk clone() {
      try {
        GroupAndChunk newVal = (GroupAndChunk)super.clone();
        return (newVal);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }    
    }

    public int hashCode() {
      return (grp.hashCode() + chunkNum);
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof GroupAndChunk)) {
        return (false);
      }
      GroupAndChunk otherTR = (GroupAndChunk)other;
      
      if (!this.grp.equals(otherTR.grp)) {
        return (false);
      }
      
      return (this.chunkNum == otherTR.chunkNum);
    }  
  }
 
}
