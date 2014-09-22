/*
**    Copyright (C) 2003-2011 Institute for Systems Biology 
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
  
  private ArrayList clusters_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public ClusterCalc(List seedClusters) {   
    clusters_ = new ArrayList(seedClusters);  
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
    return ((Cluster)clusters_.get(i)); 
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
    
    TreeSet tdVals = new TreeSet();
    int numClust = clusters_.size();
    for (int i = 0; i < numClust; i++) {
      Cluster nextClust = (Cluster)clusters_.get(i);
      for (int j = i + 1; j < numClust; j++) {
        Cluster otherClust = (Cluster)clusters_.get(j);
        double dot = nextClust.dot(otherClust);
        TaggedDot noTag = new TaggedDot(dot, i, j);
        tdVals.add(noTag);
      }
    }
    // Biggest dot product is smallest distance!
    TaggedDot winner = (TaggedDot)tdVals.last();
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
    
    TreeSet tdVals = new TreeSet();
    int numClust = clusters_.size();
    for (int i = 0; i < numClust; i++) {
      Cluster nextClust = (Cluster)clusters_.get(i);
      for (int j = i + 1; j < numClust; j++) {
        Cluster otherClust = (Cluster)clusters_.get(j);
        double dot = nextClust.dot(otherClust);
        TaggedDot noTag = new TaggedDot(dot, i, j);
        tdVals.add(noTag);
      }
    }
    // Biggest dot product is smallest distance!
    TaggedDot winner = (TaggedDot)tdVals.last();
    
    ArrayList newList = new ArrayList();
    for (int i = 0; i < numClust; i++) {
      if (i == winner.one) {
        Cluster nextClust = (Cluster)clusters_.get(winner.one);
        Cluster otherClust = (Cluster)clusters_.get(winner.two);
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
    private Comparable tag_;
    private ArrayList vals_;
 
    public ClusterVec(Comparable tag, List vals) {
      tag_ = tag;
      vals_ = new ArrayList(vals);
    }
    
    public ClusterVec(Comparable tag, int dim) {
      tag_ = tag;
      vals_ = new ArrayList();     
      for (int i = 0; i < dim; i++) {
        vals_.add(new Double(0.0));
      }
    }
    
    public int vecSize() {
      return (vals_.size());
    }
      
    public Double getVal(int i) {
      return ((Double)vals_.get(i));
    }
      
    /***************************************************************************
    **
    ** Get the value string
    */

    public String valueString() {
      StringBuffer buf = new StringBuffer();
      buf.append("[");
      Iterator vit = vals_.iterator();
      while (vit.hasNext()) {
        Double val = (Double)vit.next();
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

    public void setTag(Comparable tag) {
      tag_ = tag;
      return;     
    }
    
    /***************************************************************************
    **
    ** Get the tag
    */

    public Comparable getTag() {
      return (tag_);      
    }   
    
    /***************************************************************************
    **
    ** Clone
    */

    public Object clone() {
      try {
        ClusterVec newVal = (ClusterVec)super.clone();
        // Warning!  Object tag not cloned!
        newVal.vals_ = new ArrayList(this.vals_);
        return (newVal);            
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }    
 
    /***************************************************************************
    **
    ** Std. toString
    */
     
    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append(tag_.toString());
      buf.append(valueString());
      return (buf.toString());
    }
    
    /***************************************************************************
    **
    ** Calculate the dot product.  Missing vissing values treated as zero.
    */

    public double dot(ClusterVec other) {
      int numVal = vals_.size();
      if (other.vals_.size() != numVal) {
        throw new IllegalArgumentException();
      }   
      
      double sum = 0.0;
      for (int i = 0; i < numVal; i++) {
        Double val = (Double)vals_.get(i);
        double dVal = (val == null) ? 0.0 : val.doubleValue();
        Double otherVal = (Double)other.vals_.get(i);
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
        Double val = (Double)vals_.get(i);
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
        Double val = (Double)vals_.get(i);
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

    public ClusterVec weightedAverage(Comparable newTag, double myWeight, ClusterVec other, double otherWeight) {

      int numVal = vals_.size();
      if (other.vals_.size() != numVal) {
        throw new IllegalArgumentException();
      }       
      ArrayList myWeightedSum = new ArrayList();
      for (int i = 0; i < numVal; i++) {
        Double val = (Double)vals_.get(i);
        Double newVal = (val == null) ? null : new Double(val.doubleValue() * myWeight);
        myWeightedSum.add(newVal);
      }      
      ArrayList otherWeightedSum = new ArrayList();
      for (int i = 0; i < numVal; i++) {
        Double val = (Double)other.vals_.get(i);
        Double newVal = (val == null) ? null :new Double(val.doubleValue() * otherWeight);
        otherWeightedSum.add(newVal);
      }      
      double weightSum = myWeight + otherWeight;
      ArrayList retval = new ArrayList();
      for (int i = 0; i < numVal; i++) {
        Double myVal = (Double)myWeightedSum.get(i);
        Double otherVal = (Double)otherWeightedSum.get(i);
        Double newVal = ((myVal == null) || (otherVal == null)) ? null : new Double((myVal.doubleValue() + otherVal.doubleValue()) / weightSum);
        retval.add(newVal);
      }
      return (new ClusterVec(newTag, retval));
    }

    /***************************************************************************
    **
    ** Supply missing values (filled with mean of existing values)
    */

    public static void supplyMissingValues(List clusterVecs) {

      HashMap existingValSums = new HashMap();
      HashMap existingCounts = new HashMap();
      int vecSize = -1;
      
      //
      // Sum up the existing values:
      //
      
      int numClust = clusterVecs.size();
      for (int i = 0; i < numClust; i++) {
        ClusterVec cVec = (ClusterVec)clusterVecs.get(i);
        int numVal = cVec.vals_.size();
        if (i == 0) {
          vecSize = numVal;
        } else if (vecSize != numVal) {
          throw new IllegalArgumentException();
        } 
        for (int j = 0; j < vecSize; j++) {
          Double val = (Double)cVec.vals_.get(j);
          if (val != null) {
            Integer indexObj = new Integer(j);
            Double existing = (Double)existingValSums.get(indexObj);
            if (existing == null) {
              existingValSums.put(indexObj, val);
            } else {
              Double newVal = new Double(existing.doubleValue() + val.doubleValue());
              existingValSums.put(indexObj, newVal);
            }
            Integer count = (Integer)existingCounts.get(indexObj);
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
      
      HashMap averages = new HashMap();
      
      Iterator kit = existingValSums.keySet().iterator();
      while (kit.hasNext()) {
        Integer index = (Integer)kit.next();
        Double existingSum = (Double)existingValSums.get(index);
        Integer existingCount = (Integer)existingCounts.get(index);
        Double avg = new Double(existingSum.doubleValue() / existingCount.doubleValue());
        averages.put(index, avg);
      }
      
      //
      // Go back through clusters and add missing values:
      //
          
      for (int i = 0; i < numClust; i++) {
        ClusterVec cVec = (ClusterVec)clusterVecs.get(i);
        for (int j = 0; j < vecSize; j++) {
          Double val = (Double)cVec.vals_.get(j);
          if (val == null) {            
            Integer indexObj = new Integer(j);
            Double avg = (Double)averages.get(indexObj);
            cVec.vals_.set(j, avg);
          }
        }
      }
      return;
    } 
  }       

  public static class Cluster implements Cloneable {
    private TreeMap components_;
    private ClusterVec consensus_;

    public Cluster(ClusterVec seed) {
      components_ = new TreeMap();
      components_.put(seed.getTag(), seed);
      consensus_ = (ClusterVec)seed.clone();
      consensus_.setTag(components_.keySet().toString());
    }
    
    public Cluster(Cluster clust1, Cluster clust2) { 
      components_ = new TreeMap();
      components_.putAll(clust1.components_);
      components_.putAll(clust2.components_);
      ClusterVec vec1 = clust1.getClusterVec();
      ClusterVec vec2 = clust2.getClusterVec();
      consensus_ = vec1.weightedAverage(components_.keySet().toString(), (double)clust1.getClusterWeight(), vec2, (double)clust2.getClusterWeight());     
      consensus_.normalize();  // shouldn't need, right? 
    }
    
    /***************************************************************************
    **
    ** Collapse (drop all components to a single vector with a new tag)
    */

    public void collapse(Comparable seedTag) {
      components_.clear();
      ClusterVec newCons = (ClusterVec)consensus_.clone();
      HashSet newKeySet = new HashSet();
      newKeySet.add(seedTag);
      newCons.setTag(newKeySet.toString());
      components_.put(seedTag, newCons);
      consensus_ = newCons;
      return;
    }
 
    /***************************************************************************
    **
    ** Return original seed tags
    */

    public Set getSeedTags() {
      return (new HashSet(components_.keySet()));
    }    
       
    /***************************************************************************
    **
    ** Clone
    */

    public Object clone() {
      try {
        Cluster newVal = (Cluster)super.clone();
        // Warning!  Object tag not cloned!
        newVal.components_ = new TreeMap();
        Iterator tcit = this.components_.keySet().iterator();
        while (tcit.hasNext()) {
          Comparable key = (Comparable)tcit.next();
          ClusterVec nextThis = (ClusterVec)this.components_.get(key);
          newVal.components_.put(key, nextThis.clone());
        }
        newVal.consensus_ = (ClusterVec)this.consensus_.clone();        
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
      Iterator vit = components_.keySet().iterator();
      while (vit.hasNext()) {
        Object tag = vit.next();
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
  
  public static class TaggedDot implements Comparable {
    public double dot;
    public int one;
    public int two;

    public TaggedDot(double dot, int one, int two) {
      this.dot = dot;
      this.one = one;
      this.two = two;
    }
    
    public int compareTo(Object o) {
      if (this == o) {
        return (0);
      }
      TaggedDot other = (TaggedDot)o;
      double diff = this.dot - other.dot;
      if (diff != 0.0) {
        return ((diff < 0.0) ? (int)Math.floor(diff) : (int)Math.ceil(diff));
      }
      return (one - two);
    }
    
    public String toString() {
      return (dot + " " + one + " " + two);
    }    
  }   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
