/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.analysis.AllPathsResult;
import org.systemsbiology.biotapestry.genome.AbstractGenome;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.XPlatDisplayText;
import org.systemsbiology.biotapestry.nav.NavTree;

/****************************************************************************
**
** Serves as a genome source for non-database genomes
*/
  
      
public class LocalGenomeSource implements GenomeSource {

  private DBGenome rootGenome_;
  private HashMap<String, Genome> keyedGenomes_;

  public LocalGenomeSource() {
    keyedGenomes_ = new HashMap<String, Genome>();
  }
    
  public LocalGenomeSource(DBGenome genome, Genome keyedGenome) {
    this();
    ArrayList<Genome> myList = new ArrayList<Genome>();
    myList.add(keyedGenome);
    install(genome, myList);
  }
  
  public LocalGenomeSource(DBGenome genome, List<Genome> keyedGenomes) {
    this();
    ArrayList<Genome> myList = new ArrayList<Genome>(keyedGenomes);
    install(genome, myList);
  }
  
  private LocalGenomeSource(LocalGenomeSource other, Map<String, String> grpMap) {
    this.rootGenome_ = (other.rootGenome_ == null) ? null : new DBGenome(other.rootGenome_);
    String rgk = this.rootGenome_.getID();
    this.keyedGenomes_ = new HashMap<String, Genome>();
    Iterator<String> kit = other.keyedGenomes_.keySet().iterator();
    while (kit.hasNext()) {
      String okey = kit.next();
      if (okey.equals(rgk)) {
        this.keyedGenomes_.put(okey, this.rootGenome_);
      } else {
        Genome nextKeyed = other.keyedGenomes_.get(okey);
        this.keyedGenomes_.put(okey, ((GenomeInstance)nextKeyed).getBasicGenomeCopy(grpMap));
      }   
    }
    Iterator<Genome> vals = this.keyedGenomes_.values().iterator();
    while (vals.hasNext()) {
      AbstractGenome keyedCopy = (AbstractGenome)vals.next();
      keyedCopy.setGenomeSource(this);
    }       
  }
  
  public void dropAll() {
    rootGenome_ = null;
    keyedGenomes_.clear();
    return;
  }     
  
  public void install(DBGenome genome, List<Genome> keyedGenomes) {
    rootGenome_ = genome;
    keyedGenomes_ = new HashMap<String, Genome>();
    if (genome != null) {
      keyedGenomes_.put(genome.getID(), genome);
    }
    if (keyedGenomes != null) {
      int numkg = keyedGenomes.size();
      for (int i = 0; i < numkg; i++) {
        Genome keyedGenome = keyedGenomes.get(i);
        keyedGenomes_.put(keyedGenome.getID(), keyedGenome);
      }   
    }
    Iterator<Genome> vals = this.keyedGenomes_.values().iterator();
    while (vals.hasNext()) {
      AbstractGenome keyedCopy = (AbstractGenome)vals.next();
      keyedCopy.setGenomeSource(this);
    }  
    return;
  }
  
  public void install(LocalGenomeSource other) {
    this.rootGenome_ = (other.rootGenome_ == null) ? null : other.rootGenome_;
    String rgk = this.rootGenome_.getID();
    this.keyedGenomes_ = new HashMap<String, Genome>();
    Iterator<String> kit = other.keyedGenomes_.keySet().iterator();
    while (kit.hasNext()) {
      String okey = kit.next();
      if (okey.equals(rgk)) {
        this.keyedGenomes_.put(okey, this.rootGenome_);
      } else {
        Genome nextKeyed = other.keyedGenomes_.get(okey);        
        this.keyedGenomes_.put(okey, nextKeyed);
      }   
    }
    Iterator<Genome> vals = this.keyedGenomes_.values().iterator();
    while (vals.hasNext()) {
      AbstractGenome keyedCopy = (AbstractGenome)vals.next();
      keyedCopy.setGenomeSource(this);
    }  
    return;
  }
  
  public void setGenome(Genome genome) {
    rootGenome_ = (DBGenome)genome;
    keyedGenomes_.put(genome.getID(), genome);
    return;
  }    

  public void setGenome(String key, Genome genome) {
    if (!genome.getID().equals(key)) {
      throw new IllegalStateException();
    }
    keyedGenomes_.put(genome.getID(), genome);
    return;
  }  
  
  public void dropGenome(String key) {
    keyedGenomes_.remove(key);
    return;
  }    
  
  public Genome getGenome() {
    return (rootGenome_);
  }    

  public Genome getGenome(String key) {
    return (keyedGenomes_.get(key));
  }
  
  public Iterator<GenomeInstance> getInstanceIterator() {
    ArrayList<GenomeInstance> forRetval = new ArrayList<GenomeInstance>();
    Iterator<String> kit = keyedGenomes_.keySet().iterator();
    while (kit.hasNext()) {
      String okey = kit.next();
      Genome nextKeyed = keyedGenomes_.get(okey);
      if (nextKeyed instanceof GenomeInstance) {
        forRetval.add((GenomeInstance)nextKeyed);
      }   
    }
    return (forRetval.iterator());
  } 
  
  /***************************************************************************
  ** 
  ** Get display Text
  */
  
  public XPlatDisplayText getTextFromGenomeKey(String key) {
    return (null);
  }
 
  /***************************************************************************
  **
  ** Reduce the genome to hold just our results
  */
    
  public LocalGenomeSource reduceToResult(AllPathsResult apr, String redID, Map<String, String> grpIDMap) {
    // Transfer of genome source is occurring here:
    LocalGenomeSource retval = new LocalGenomeSource(this, grpIDMap);
    Genome reduced = retval.getGenome(redID);
    Genome sourceGenome = getGenome(redID);
    Set<String> lst = apr.getLinkSet();
    Iterator<Linkage> tlit = sourceGenome.getLinkageIterator();
    while (tlit.hasNext()) {
      Linkage link = tlit.next();
      String linkID = link.getID();
      if (!lst.contains(linkID)) {
        reduced.removeLinkage(linkID);
      }
    }
    Set<String> nst = apr.getNodeSet();
    Iterator<Node> nlit = sourceGenome.getAllNodeIterator();
    while (nlit.hasNext()) {
      Node node = nlit.next();
      String nodeID = node.getID();
      if (!nst.contains(nodeID)) {
        reduced.removeNode(nodeID);
      }
    }
    return (retval);
  }
  
  
  public NetOverlayOwner getOverlayOwnerFromGenomeKey(String key) {
    if (DynamicInstanceProxy.isDynamicInstance(key)) {
      //
      // FIXME: This still needs to work OK with dynamic stuff
      //
      throw new IllegalArgumentException();
    } else {
      NetOverlayOwner genOwn = getGenome(key);
      if (genOwn == null) {
        throw new IllegalArgumentException();
      }     
      return (genOwn);
    }
  } 
 
  /***************************************************************************
  **
  ** Get an overlay owner
  */ 
   
  public NetOverlayOwner getOverlayOwnerWithOwnerKey(String key) {
    throw new UnsupportedOperationException();
  }   
 
  /***************************************************************************
  **
  ** Get an iterator over the dynamic proxies
  */ 
   
  public Iterator<DynamicInstanceProxy> getDynamicProxyIterator() {
    return (new ArrayList<DynamicInstanceProxy>().iterator());
  } 
  
  /***************************************************************************
  ** 
  ** Get the given dynamic proxy
  */

  public DynamicInstanceProxy getDynamicProxy(String key) {
    throw new UnsupportedOperationException();  
  }
  
  /***************************************************************************
  ** 
  ** Add the given genome instance
  */

  public DatabaseChange addGenomeInstanceExistingLabel(String key, GenomeInstance genome){
    throw new UnsupportedOperationException();  
  }
 

  /***************************************************************************
  ** 
  ** Add the given genome instance
  */

  public DatabaseChange addGenomeInstance(String key, GenomeInstance genome) {
    throw new UnsupportedOperationException();  
  }

  /***************************************************************************
  ** 
  ** Add the given dynamic proxy
  */

  public DatabaseChange addDynamicProxy(String key, DynamicInstanceProxy dip) {
    throw new UnsupportedOperationException();  
  }
  
  /***************************************************************************
  ** 
  ** Add the given dynamic proxy
  */

  public DatabaseChange addDynamicProxyExistingLabel(String key, DynamicInstanceProxy dip) {
    throw new UnsupportedOperationException();  
  }
  
  
  /***************************************************************************
  ** 
  ** Remove a dynamic proxy
  */

  public DatabaseChange[] removeDynamicProxy(String key) {
    throw new UnsupportedOperationException();  
  }

  /***************************************************************************
  ** 
  ** Clear out all dynamic proxies
  */

  public void clearAllDynamicProxyCaches() {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  ** 
  ** Get the startupView, with first view overlay preference installed
  */

  public StartupView getStartupView() {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  ** 
  ** Get the startupView
  */

  public DatabaseChange setStartupView(StartupView startupView) {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  ** 
  ** Get the genome count
  */

  public int getGenomeCount() {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  ** 
  ** Answer if any model has time bounds set
  */

  public boolean modelsHaveTimeBounds() {
    throw new UnsupportedOperationException();
  }
 
    /***************************************************************************
  ** 
  ** Get the next key
  */

  public String getNextKey(){
    throw new UnsupportedOperationException();
  }

  /***************************************************************************
  ** 
  ** Drop only the guts of the root network
  */
  
  public DatabaseChange dropRootNetworkOnly(){
    throw new UnsupportedOperationException();
  }
    
  /***************************************************************************
  ** 
  ** Drop only the guts of an instance network
  */

  public DatabaseChange dropInstanceNetworkOnly(String gid){
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  ** 
  ** Get a unique model name
  */

  public String getUniqueModelName(){
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  ** 
  ** Get model tree
  */
   
  public NavTree getModelHierarchy(){
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  ** 
  ** Remove an instance
  */

  public DatabaseChange[] removeInstance(String key){
    throw new UnsupportedOperationException();
  }

}
