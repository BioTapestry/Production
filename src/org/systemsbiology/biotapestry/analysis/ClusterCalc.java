/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Quick and dumb cluster calculator
*/

public class ClusterCalc {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private ArrayList<Cluster> clusters_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public ClusterCalc(List<Cluster> seedClusters) {   
    clusters_ = new ArrayList<Cluster>(seedClusters);  
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  /***************************************************************************
  ** 
  ** Get the given cluster
  */

  public Cluster getCluster(int i) {
    return (clusters_.get(i)); 
  }
    
  /***************************************************************************
  ** 
  ** report cluster count
  */

  public int numClusters() {
    return (clusters_.size()); 
  }
  
  /***************************************************************************
  ** 
  ** Answer the current smallest inter-cluster distance:
  */

  public double nextCombine() {
    
    if (clusters_.size() < 2) {
      throw new IllegalStateException();
    }
    
    TreeSet<TaggedDot> tdVals = new TreeSet<TaggedDot>();
    int numClust = clusters_.size();
    for (int i = 0; i < numClust; i++) {
      Cluster nextClust = clusters_.get(i);
      for (int j = i + 1; j < numClust; j++) {
        Cluster otherClust = clusters_.get(j);
        double dot = nextClust.dot(otherClust);
        TaggedDot noTag = new TaggedDot(dot, i, j);
        tdVals.add(noTag);
      }
    }
    // Biggest dot product is smallest distance!
    TaggedDot winner = tdVals.last();
    return (winner.dot);   
  }

  /***************************************************************************
  ** 
  ** Do a cluster iteration
  */

  public void combine() {
    
    if (clusters_.size() < 2) {
      throw new IllegalStateException();
    }
    
    TreeSet<TaggedDot> tdVals = new TreeSet<TaggedDot>();
    int numClust = clusters_.size();
    for (int i = 0; i < numClust; i++) {
      Cluster nextClust = clusters_.get(i);
      for (int j = i + 1; j < numClust; j++) {
        Cluster otherClust = clusters_.get(j);
        double dot = nextClust.dot(otherClust);
        TaggedDot noTag = new TaggedDot(dot, i, j);
        tdVals.add(noTag);
      }
    }
    // Biggest dot product is smallest distance!
    TaggedDot winner = tdVals.last();
    
    ArrayList<Cluster> newList = new ArrayList<Cluster>();
    for (int i = 0; i < numClust; i++) {
      if (i == winner.one) {
        Cluster nextClust = clusters_.get(winner.one);
        Cluster otherClust = clusters_.get(winner.two);
        newList.add(new Cluster(nextClust, otherClust));
      } else if (i == winner.two) {
        continue;
      } else {
        newList.add(clusters_.get(i));
      }
    }
    
    clusters_.clear();
    clusters_.addAll(newList);
    return;
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  public static class ClusterVec implements Cloneable {
    private ClusterKey tag_;
    private ArrayList<Double> vals_;
 
    public ClusterVec(ClusterKey tag, List<Double> vals) {
      tag_ = tag;
      vals_ = new ArrayList<Double>(vals);
    }
    
    public ClusterVec(ClusterKey tag, int dim) {
      tag_ = tag;
      vals_ = new ArrayList<Double>();     
      for (int i = 0; i < dim; i++) {
        vals_.add(new Double(0.0));
      }
    }
    
    public int vecSize() {
      return (vals_.size());
    }
      
    public Double getVal(int i) {
      return (vals_.get(i));
    }
      
    /***************************************************************************
    **
    ** Get the value string
    */

    public String valueString() {
      StringBuffer buf = new StringBuffer();
      buf.append("[");
      Iterator<Double> vit = vals_.iterator();
      while (vit.hasNext()) {
        Double val = vit.next();
        String out = (val == null) ? "(NoVal)" : UiUtil.doubleFormat(val.doubleValue(), false);
        buf.append(out);
        if (vit.hasNext()) {
          buf.append(" ");
        }
      }
      buf.append("]");
      return (buf.toString());     
    }
        
    /***************************************************************************
    **
    ** Set the tag
    */

    public void setTag(ClusterKey tag) {
      tag_ = tag;
      return;     
    }
    
    /***************************************************************************
    **
    ** Get the tag
    */

    public ClusterKey getTag() {
      return (tag_);      
    }   
    
    /***************************************************************************
    **
    ** Clone
    */

    @Override
    public ClusterVec clone() {
      try {
        ClusterVec newVal = (ClusterVec)super.clone();
        // Warning!  Object tag not cloned!
        newVal.vals_ = new ArrayList<Double>(this.vals_);
        return (newVal);            
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }    
 
    /***************************************************************************
    **
    ** Std. toString
    */
    
    @Override     
    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append(tag_.toString());
      buf.append(valueString());
      return (buf.toString());
    }
    
    /***************************************************************************
    **
    ** Calculate the dot product.  Missing missing values treated as zero.
    */

    public double dot(ClusterVec other) {
      int numVal = vals_.size();
      if (other.vals_.size() != numVal) {
        throw new IllegalArgumentException();
      }   
      
      double sum = 0.0;
      for (int i = 0; i < numVal; i++) {
        Double val = vals_.get(i);
        double dVal = (val == null) ? 0.0 : val.doubleValue();
        Double otherVal = other.vals_.get(i);
        double odVal = (otherVal == null) ? 0.0 : otherVal.doubleValue();
        sum += (dVal * odVal);
      }    
      return (sum);
    }
    
    /***************************************************************************
    **
    ** Get the missing count
    */

    public int numMissing() {
      int count = 0;
      int numVal = vals_.size();
      for (int i = 0; i < numVal; i++) {
        Double val = vals_.get(i);
        if (val == null) {
          count++;
        }
      }          
      return (count);
    } 
        
    /***************************************************************************
    **
    ** Calculate the length
    */

    public double length() {
      return Math.sqrt(dot(this));
    } 

    /***************************************************************************
    **
    ** Normalize the ClusterVec.  Treats missing values as zero, keeps them null
    */

    public void normalize() {
      double len = length();
      if (len == 0.0) {
        throw new IllegalStateException();
      }
      int numVal = vals_.size();
      for (int i = 0; i < numVal; i++) {
        Double val = vals_.get(i);
        if (val == null) {
          continue;
        } else {
          Double newVal = new Double(val.doubleValue() / len);
          vals_.set(i, newVal);
        }
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Get a new weighted average ClusterVec
    */

    public ClusterVec weightedAverage(ClusterKey newTag, double myWeight, ClusterVec other, double otherWeight) {

      int numVal = vals_.size();
      if (other.vals_.size() != numVal) {
        throw new IllegalArgumentException();
      }       
      ArrayList<Double> myWeightedSum = new ArrayList<Double>();
      for (int i = 0; i < numVal; i++) {
        Double val = vals_.get(i);
        Double newVal = (val == null) ? null : new Double(val.doubleValue() * myWeight);
        myWeightedSum.add(newVal);
      }      
      ArrayList<Double> otherWeightedSum = new ArrayList<Double>();
      for (int i = 0; i < numVal; i++) {
        Double val = other.vals_.get(i);
        Double newVal = (val == null) ? null :new Double(val.doubleValue() * otherWeight);
        otherWeightedSum.add(newVal);
      }      
      double weightSum = myWeight + otherWeight;
      ArrayList<Double> retval = new ArrayList<Double>();
      for (int i = 0; i < numVal; i++) {
        Double myVal = myWeightedSum.get(i);
        Double otherVal = otherWeightedSum.get(i);
        Double newVal = ((myVal == null) || (otherVal == null)) ? null : new Double((myVal.doubleValue() + otherVal.doubleValue()) / weightSum);
        retval.add(newVal);
      }
      return (new ClusterVec(newTag, retval));
    }

    /***************************************************************************
    **
    ** Supply missing values (filled with mean of existing values)
    */

    public static void supplyMissingValues(List<ClusterVec> clusterVecs) {

      HashMap<Integer, Double> existingValSums = new HashMap<Integer, Double>();
      HashMap<Integer, Integer> existingCounts = new HashMap<Integer, Integer>();
      int vecSize = -1;
      
      //
      // Sum up the existing values:
      //
      
      int numClust = clusterVecs.size();
      for (int i = 0; i < numClust; i++) {
        ClusterVec cVec = clusterVecs.get(i);
        int numVal = cVec.vals_.size();
        if (i == 0) {
          vecSize = numVal;
        } else if (vecSize != numVal) {
          throw new IllegalArgumentException();
        } 
        for (int j = 0; j < vecSize; j++) {
          Double val = cVec.vals_.get(j);
          if (val != null) {
            Integer indexObj = Integer.valueOf(j);
            Double existing = existingValSums.get(indexObj);
            if (existing == null) {
              existingValSums.put(indexObj, val);
            } else {
              Double newVal = new Double(existing.doubleValue() + val.doubleValue());
              existingValSums.put(indexObj, newVal);
            }
            Integer count = existingCounts.get(indexObj);
            if (count == null) {
              existingCounts.put(indexObj, new Integer(1));              
            } else {
              existingCounts.put(indexObj, new Integer(count.intValue() + 1));                           
            }            
          }
        }   
      }      
        
      //
      // Average:
      //
      
      HashMap<Integer, Double> averages = new HashMap<Integer, Double>();
      
      Iterator<Integer> kit = existingValSums.keySet().iterator();
      while (kit.hasNext()) {
        Integer index = kit.next();
        Double existingSum = existingValSums.get(index);
        Integer existingCount = existingCounts.get(index);
        Double avg = new Double(existingSum.doubleValue() / existingCount.doubleValue());
        averages.put(index, avg);
      }
      
      //
      // Go back through clusters and add missing values:
      //
          
      for (int i = 0; i < numClust; i++) {
        ClusterVec cVec = clusterVecs.get(i);
        for (int j = 0; j < vecSize; j++) {
          Double val = cVec.vals_.get(j);
          if (val == null) {            
            Integer indexObj = new Integer(j);
            Double avg = averages.get(indexObj);
            cVec.vals_.set(j, avg);
          }
        }
      }
      return;
    } 
  }       

  public static class Cluster implements Cloneable {
    private TreeMap<ClusterKey, ClusterVec> components_;
    private ClusterVec consensus_;

    public Cluster(ClusterVec seed) {
      components_ = new TreeMap<ClusterKey, ClusterVec>();
      components_.put(seed.getTag(), seed);
      consensus_ = seed.clone();
      consensus_.setTag(new ClusterKey(components_.keySet().toString()));
    }
    
    public Cluster(Cluster clust1, Cluster clust2) { 
      components_ = new TreeMap<ClusterKey, ClusterVec>();
      components_.putAll(clust1.components_);
      components_.putAll(clust2.components_);
      ClusterVec vec1 = clust1.getClusterVec();
      ClusterVec vec2 = clust2.getClusterVec();
      consensus_ = vec1.weightedAverage(new ClusterKey(components_.keySet()), clust1.getClusterWeight(), vec2, clust2.getClusterWeight());     
      consensus_.normalize();  // shouldn't need, right? 
    }
    
    /***************************************************************************
    **
    ** Collapse (drop all components to a single vector with a new tag)
    */

    public void collapse(ClusterKey seedTag) {
      components_.clear();
      ClusterVec newCons = consensus_.clone();
    //  HashSet<ClusterKey> newKeySet = new HashSet<ClusterKey>();
    //  newKeySet.add(seedTag);
    //  newCons.setTag(new ClusterKey(newKeySet));
      newCons.setTag(seedTag);
      components_.put(seedTag, newCons);
      consensus_ = newCons;
      return;
    }
 
    /***************************************************************************
    **
    ** Return original seed tags
    */

    public Set<ClusterKey> getSeedTags() {
      return (new HashSet<ClusterKey>(components_.keySet()));
    }    
       
    /***************************************************************************
    **
    ** Clone
    */

    @Override
    public Cluster clone() {
      try {
        Cluster newVal = (Cluster)super.clone();
        // Warning!  Object tag not cloned!
        newVal.components_ = new TreeMap<ClusterKey, ClusterVec>();
        Iterator<ClusterKey> tcit = this.components_.keySet().iterator();
        while (tcit.hasNext()) {
          ClusterKey key = tcit.next();
          ClusterVec nextThis = this.components_.get(key);
          newVal.components_.put(key, nextThis.clone());
        }
        newVal.consensus_ = this.consensus_.clone();        
        return (newVal);            
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }    
     
    public ClusterVec getClusterVec() {
      return (consensus_);
    }
    
    public int getClusterWeight() {
      return (components_.size());
    }
    
    public double dot(Cluster otherClust) {
      return (this.consensus_.dot(otherClust.consensus_));
    }
   
    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append(" [");
      Iterator<ClusterKey> vit = components_.keySet().iterator();
      while (vit.hasNext()) {
        ClusterKey tag = vit.next();
        buf.append(tag.toString());
        if (vit.hasNext()) {
          buf.append(" ");
        }
      }
      buf.append("]: ");
      buf.append(consensus_);
      return (buf.toString());
    }
  }   
  
  public static class TaggedDot implements Comparable<TaggedDot> {
    public double dot;
    public int one;
    public int two;

    public TaggedDot(double dot, int one, int two) {
      this.dot = dot;
      this.one = one;
      this.two = two;
    }
    
    public int compareTo(TaggedDot other) {
      if (this == other) {
        return (0);
      }
      double diff = this.dot - other.dot;
      if (diff != 0.0) {
        return ((diff < 0.0) ? (int)Math.floor(diff) : (int)Math.ceil(diff));
      }
      return (one - two);
    }
    
    @Override
    public String toString() {
      return (dot + " " + one + " " + two);
    }
    
    @Override
    public int hashCode() {
      return ((int)Math.round(100.0 * dot) + one + two);
    }
    
    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof TaggedDot)) {
        return (false);
      }
      TaggedDot otherDot = (TaggedDot)other;
      if (otherDot.dot != this.dot) {
        return (false);
      }
      if (otherDot.one != this.one) {
        return (false);
      }
      if (otherDot.two != this.two) {
        return (false);
      }
      return (true);
    }
  } 
  
  //
  // Designed to be extensible to anything else the user wants to use as a key
  //
  
  public static class ClusterKey implements Comparable<ClusterKey> {
    protected final String myString_;
     
    protected ClusterKey() {
      myString_ = null;
    }
    
    public ClusterKey(String tag) {
      myString_ = tag;
    }
    
    public ClusterKey(Collection<ClusterKey> keys) {
      myString_ = keys.toString();
    }

    protected Integer hashCodeVote() {
      return (null);
    }

    protected String toStringVote() {
      return (null);
    }
    
    @SuppressWarnings("unused")
    protected Boolean equalsVote(ClusterKey otherKey) {
      return (null);
    }
    
    @SuppressWarnings("unused")
    protected Integer compareToVote(ClusterKey otherKey) {
      return (null);
    }
      
    @Override
    public int hashCode() {
      Integer hcv = hashCodeVote();
      if (hcv == null) {
        return (myString_.hashCode());
      } else {
        return (hcv.intValue());
      }
    }

    @Override
    public String toString() {
      String tsv = toStringVote();
      if (tsv == null) {
        return (myString_.toString());
      } else {
        return (tsv);
      }
    }
    
    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof ClusterKey)) {
        return (false);
      }
      ClusterKey otherKey = (ClusterKey)other;
      Boolean ev = equalsVote(otherKey);
      if (ev != null) {
        return (ev.booleanValue());
      }
      if ((otherKey.myString_ != null) && (this.myString_ != null)) {
        return (otherKey.myString_.equals(this.myString_));  
      } else {
        return (false);
      }
    }
    
    public int compareTo(ClusterKey otherKey) {
      if (this == otherKey) {
        return (0);
      }
      Integer ctv = compareToVote(otherKey);
      if (ctv != null) {
        return (ctv.intValue());
      }
      if ((otherKey.myString_ != null) && (this.myString_ != null)) {
        return (otherKey.myString_.compareTo(this.myString_));  
      } else if (this.myString_ != null) {
        return (-1);
      } else if (otherKey.myString_ != null) {
        return (1);
      } else {
        throw new IllegalStateException();
      }
    } 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
