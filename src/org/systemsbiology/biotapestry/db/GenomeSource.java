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

package org.systemsbiology.biotapestry.db;

import java.util.Iterator;

import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.XPlatDisplayText;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Interface for genome sources
*/

public interface GenomeSource {

  /***************************************************************************
  ** 
  ** Get the database/tab ID
  */

  public String getID();   
  
  
  /***************************************************************************
  ** 
  ** Get the root genome.
  */

  public DBGenome getRootDBGenome();   
  
  /***************************************************************************
  ** 
  ** Get the given genome.
  */

  public Genome getGenome(String key);
  
  /***************************************************************************
  ** 
  ** Get an instance iterator.
  */

  public Iterator<GenomeInstance> getInstanceIterator();  
  
  
  /***************************************************************************
  **
  ** Get the overlay owner from a genome key
  */ 
    
  public NetOverlayOwner getOverlayOwnerFromGenomeKey(String key);
  
  /***************************************************************************
  **
  ** Get the overlay owner from an owner key
  */ 
    
  public NetOverlayOwner getOverlayOwnerWithOwnerKey(String ownkey);
  
  /***************************************************************************
  **
  ** Get an iterator over the dynamic proxies
  */ 
   
  Iterator<DynamicInstanceProxy> getDynamicProxyIterator();
  
  /***************************************************************************
  ** 
  ** Get the given dynamic proxy
  */

  public DynamicInstanceProxy getDynamicProxy(String key);
  
  
  /***************************************************************************
  ** 
  ** Get display Text
  */
  
  public XPlatDisplayText getTextFromGenomeKey(String key);
  
  /***************************************************************************
  ** 
  ** Add the given genome instance
  */

  public DatabaseChange addGenomeInstanceExistingLabel(String key, GenomeInstance genome);
 

  /***************************************************************************
  ** 
  ** Add the given genome instance
  */

  public DatabaseChange addGenomeInstance(String key, GenomeInstance genome);
  

  /***************************************************************************
  ** 
  ** Add the given dynamic proxy
  */

  public DatabaseChange addDynamicProxy(String key, DynamicInstanceProxy dip);
  
  /***************************************************************************
  ** 
  ** Add the given dynamic proxy
  */

  public DatabaseChange addDynamicProxyExistingLabel(String key, DynamicInstanceProxy dip);
  
  
  /***************************************************************************
  ** 
  ** Remove a dynamic proxy
  */

  public DatabaseChange[] removeDynamicProxy(String key);
 
  /***************************************************************************
  ** 
  ** Clear out all dynamic proxies
  */

  public void clearAllDynamicProxyCaches();  
  
  /***************************************************************************
  ** 
  ** Get the next key for a >> MODEL OR A LAYOUT<<
  */

  public String getNextKey();

  /***************************************************************************
  ** 
  ** Drop only the guts of the root network
  */
  
  public DatabaseChange dropRootNetworkOnly();
    
  /***************************************************************************
  ** 
  ** Drop only the guts of an instance network
  */

  public DatabaseChange dropInstanceNetworkOnly(String gid);
  
  /***************************************************************************
  ** 
  ** Get a unique model name
  */

  public String getUniqueModelName(ResourceManager rMan);
  
  /***************************************************************************
  ** 
  ** Get model tree
  */
   
  public NavTree getModelHierarchy();
  
  /***************************************************************************
  ** 
  ** Remove an instance
  */

  public DatabaseChange[] removeInstance(String key);
  
  /***************************************************************************
  ** 
  ** Get the TabNameData
  */

  public TabNameData getTabNameData();
  
  /***************************************************************************
  ** 
  ** Set the TabNameData
  */

  public DatabaseChange setTabNameData(TabNameData tnData);
  
  /***************************************************************************
  ** 
  ** Get the startupView, with first view overlay preference installed
  */

  public StartupView getStartupView();
  
  /***************************************************************************
  ** 
  ** Set the startupView
  */

  public DatabaseChange setStartupView(StartupView startupView);
   
  /***************************************************************************
  ** 
  ** Get the genome count
  */

  public int getGenomeCount(); 
  
  /***************************************************************************
  ** 
  ** Answer if any model has time bounds set
  */

  public boolean modelsHaveTimeBounds();

  /***************************************************************************
  ** 
  ** Set the core genome
  */

  public void setGenome(DBGenome genome);
  
}
