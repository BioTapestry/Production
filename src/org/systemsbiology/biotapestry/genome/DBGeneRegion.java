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

package org.systemsbiology.biotapestry.genome;

import java.io.PrintWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** This represents a region of a gene
*/

public class DBGeneRegion implements Cloneable, Comparable<DBGeneRegion> {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  //////////////////////////////////////////////////////////////////////////// 

  private String name_;
  private int startPad_;
  private int endPad_;
  private int evidenceLevel_;
  private boolean holder_;
  private boolean linkHolder_; // temporary use only
    
  private static String[] greek_ = {"α","β","γ","δ","ε","ζ","η","θ","ι","κ","λ","μ","ν","ξ","ο","π","ρ","σ","τ","υ","φ","χ","ψ","ω"};
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** For UI-based construction
  */

  public DBGeneRegion(String name, int startPad, int endPad, int evidenceLevel, boolean isHolder, boolean isLinkHolder) {
    if (name.trim().equals("")) {
      throw new IllegalArgumentException();
    }
    name_ = name;
    startPad_ = startPad;
    endPad_ = endPad;
    evidenceLevel_ = evidenceLevel;
    if (isLinkHolder && isHolder) {
      throw new IllegalArgumentException();
    }
    holder_ = isHolder;
    linkHolder_ = isLinkHolder;
  }

  /***************************************************************************
  **
  ** For UI-based construction
  */

  public DBGeneRegion(String name, int startPad, int endPad, int evidenceLevel, boolean isHolder) {
    this(name, startPad, endPad, evidenceLevel, isHolder, false);
  }
  

  
  
  /***************************************************************************
  **
  ** For XML-based construction
  */

  public DBGeneRegion(String name, String startPad, String endPad, String evidence, String isHolder) throws IOException {
    name_ = name;
    startPad_ = 0;
    if (startPad != null) {
      try {
        startPad_ = Integer.parseInt(startPad);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
    endPad_ = 0;
    if (endPad != null) {
      try {
        endPad_ = Integer.parseInt(endPad);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
    evidenceLevel_ = Gene.LEVEL_NONE;
    if (evidence != null) {
      try {
        evidenceLevel_ = Integer.parseInt(evidence);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
    holder_ = false;
    if (isHolder != null) {
      holder_ = Boolean.valueOf(isHolder);    
    }
    linkHolder_ = false;
  }

  /***************************************************************************
  **
  ** Copy constructor
  */

  public DBGeneRegion(DBGeneRegion other) {
    this.name_ = other.name_;
    this.startPad_ = other.startPad_;
    this.endPad_ = other.endPad_;
    this.evidenceLevel_ = other.evidenceLevel_;
    this.holder_ = other.holder_;
    this.linkHolder_ = other.linkHolder_;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Get the key
  */

  public DBGeneRegion.DBRegKey getKey() {
    return (new DBGeneRegion.DBRegKey(this));
  }

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public DBGeneRegion clone() {
    try {
      DBGeneRegion retval = (DBGeneRegion)super.clone();        
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Compare:
  */
  
  public int compareTo(DBGeneRegion otherDbgr) {
    
    if (otherDbgr.startPad_ != this.startPad_) {
      return (this.startPad_ - otherDbgr.startPad_);
    }
    if (otherDbgr.endPad_ != this.endPad_) {
      return (this.endPad_ - otherDbgr.endPad_);
    }
    if (otherDbgr.holder_ != this.holder_) {
      return ((this.holder_) ? -1 : 1);
    }
    if (otherDbgr.linkHolder_ != this.linkHolder_) {
      return ((this.linkHolder_) ? -1 : 1);
    }
    if (otherDbgr.evidenceLevel_ != this.evidenceLevel_) {
      return (otherDbgr.evidenceLevel_ - this.evidenceLevel_);
    }    
    String useTest = (otherDbgr.name_ == null) ? "" : otherDbgr.name_;
    return (useTest.compareTo((this.name_ == null) ? "" : this.name_));
  }
 
  
  /***************************************************************************
  **
  ** Get existing holder keys:
  */
  
  public static Set<Integer> getExistingHolderKeys(List<DBGeneRegion> dbgrList) {
    TreeSet<Integer> used = new TreeSet<Integer>();
    for (int i = 0; i < dbgrList.size(); i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      if (dbgr.isHolder() || dbgr.isLinkHolder()) {
        String in = dbgr.getInternalName();
        try {
          int myVal = Integer.parseInt(in);
          used.add(Integer.valueOf(myVal));
        } catch (NumberFormatException nfe) {
          throw new IllegalStateException();
        } 
      }
    }
    return (used);
  }
  
  /***************************************************************************
  **
  ** Get an unused holder key:
  */
  
  public static String getNextHolderKey(List<DBGeneRegion> dbgrList, Set<Integer> avoid) {
    TreeSet<Integer> used = new TreeSet<Integer>(avoid);
    for (int i = 0; i < dbgrList.size(); i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      if (dbgr.isHolder() || dbgr.isLinkHolder()) {
        String in = dbgr.getInternalName();
        try {
          int myVal = Integer.parseInt(in);
          used.add(Integer.valueOf(myVal));
        } catch (NumberFormatException nfe) {
          throw new IllegalStateException();
        } 
      }
    }
    if (used.isEmpty()) {
      return ("0");
    } else {
      return ((Integer.valueOf(used.last().intValue() + 1)).toString());
    }
  }

  /***************************************************************************
  **
  ** Check the list is valid and in left to right order. With holders, must cover all pads:
  */
  
  public static boolean validOrder(List<DBGeneRegion> dbgrList, int minPad, int maxPad) {
    HashSet<Integer> usedPads = new HashSet<Integer>();
    TreeSet<Integer> unusedPads = new TreeSet<Integer>();
    unusedPads.add(Integer.valueOf(minPad));
    unusedPads.add(Integer.valueOf(maxPad));   
    DataUtil.fillOutHourly(unusedPads);
    
    HashSet<String> usedNames = new HashSet<String>();
    Integer max = null;
    for (int i = 0; i < dbgrList.size(); i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      int startPad = dbgr.getStartPad();
      int endPad = dbgr.getEndPad();
      if ((startPad < minPad) || (endPad < minPad)) {
        return (false);
      }
      if ((startPad > maxPad) || (endPad > maxPad)) {
        return (false);
      }
      if (startPad > endPad) { // Can be equal if size == 1
        return (false);
      }
      // Check for overlap: none allowed
      for (int j = startPad; j <= endPad; j++) {
        if (!usedPads.add(Integer.valueOf(j))) {
          return (false);
        }
        unusedPads.remove(Integer.valueOf(j));
      }
      if (max != null) {
        if (startPad <= max.intValue()) {
          return (false);  
        }    
      }
      max = Integer.valueOf(endPad);

      //
      // If it a holder, name does not matter.
      //
      
      if (dbgr.isHolder() || dbgr.isLinkHolder()) {
        continue;
      }
      
      // duplicate names not allowed,blanks not allowed
      String name = dbgr.getName();
      if (name == null) {
        return (false);
      }
      name = name.trim();
      if (name.equals("")) {
        return (false);
      }
      String nkwg = DataUtil.normKeyWithGreek(name);
      if (DataUtil.containsKey(usedNames, nkwg)) {
        return (false);
      }
      usedNames.add(nkwg);
    }
    if (!unusedPads.isEmpty()) {
      return (false);
    }
    return (true);
  } 

  /***************************************************************************
  **
  ** Get region
  */  
  
  public static DBGeneRegion getRegion(List<DBGeneRegion> dbgrList, DBRegKey key) { 
    return (dbgrList.get(regionIndex(dbgrList, key)));
  }

  /***************************************************************************
  **
  ** Get region position
  */  
  
  public static int regionIndex(List<DBGeneRegion> dbgrList, DBRegKey key) { 
    boolean lookingForHolder = key.isHolder();
    for (int i = 0; i < dbgrList.size(); i++) {
      DBGeneRegion reg = dbgrList.get(i);
      if (lookingForHolder && (reg.isHolder() || reg.isLinkHolder())) {
        if (reg.getInternalName().equals(key.getInternalName())) {
          return (i);
        }
      } else if (!lookingForHolder && !(reg.isHolder() || reg.isLinkHolder())) {
        if (reg.getName().equals(key.getName())) {
          return (i);
        }
      }
    }
    throw new IllegalStateException();
  }

  /***************************************************************************
  **
  ** On the left, we can pull something if we are not the leftmost non-holder.
  */
  
  public static boolean canPullSomething(List<DBGeneRegion> dbgrList, int index) {
    int fnhi = firstNonHolderIndex(dbgrList);
    int lnhi = lastNonHolderIndex(dbgrList);
    return ((index > fnhi) || (index < lnhi));
  }
  
  /***************************************************************************
  **
  ** Get the leftmost non-holder
  */
  
  public static DBGeneRegion firstNonHolder(List<DBGeneRegion> dbgrList) { 
    int fnhi = firstNonHolderIndex(dbgrList);
    if (fnhi != -1) {
      return (dbgrList.get(fnhi));
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Get the leftmost non-holder
  */
  
  public static int firstNonHolderIndex(List<DBGeneRegion> dbgrList) {  
    int num = dbgrList.size();
    for (int i = 0; i < num; i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      if (!dbgr.isHolder() && !dbgr.isLinkHolder()) {
        return (i);
      }
    }
    return (-1);
  }

  /***************************************************************************
  **
  ** Get the rightmost non-holder
  */
  
  public static DBGeneRegion lastNonHolder(List<DBGeneRegion> dbgrList) {  
    int lnhi = lastNonHolderIndex(dbgrList);
    if (lnhi != -1) {
      return (dbgrList.get(lnhi));
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Get the rightmost non-holder
  */
  
  public static int lastNonHolderIndex(List<DBGeneRegion> dbgrList) {  
    int num = dbgrList.size();
    for (int i = num - 1; i >= 0; i--) {
      DBGeneRegion dbgr = dbgrList.get(i);
      if (!dbgr.isHolder() && !dbgr.isLinkHolder()) {
        return (i);
      }
    }
    return (-1);
  }

  /***************************************************************************
  **
  ** Initialize a list with a single holder region
  */
  
  public static List<DBGeneRegion> listFromGene(Gene gene) {  

    Iterator<DBGeneRegion> grit = gene.regionIterator();
    ArrayList<DBGeneRegion> glist = new ArrayList<DBGeneRegion>();
    while (grit.hasNext()) {
      DBGeneRegion reg = grit.next();
      glist.add(reg.clone());
    }
    return (glist);
  }

  /***************************************************************************
  **
  ** Initialize a list with true contents or a single holder region
  */
  
  public static List<DBGeneRegion> initTheList(Gene gene) {
    int pads = gene.getPadCount();
    int firstPad = DBGene.DEFAULT_PAD_COUNT - pads;
    int lastPad = DBGene.DEFAULT_PAD_COUNT - 1;
    List<DBGeneRegion> dbgrList = listFromGene(gene);
    if (!dbgrList.isEmpty()) {
      return (dbgrList);
    }
    DBGeneRegion dbreg = new DBGeneRegion(DBGeneRegion.getNextHolderKey(dbgrList, new HashSet<Integer>()), firstPad, lastPad, Gene.LEVEL_NONE, true);
    dbgrList.add(dbreg);
    return (dbgrList);
  }

  /***************************************************************************
  **
  ** How much space does a region with links in it require. Enough so all used pads get retained.
  */
  
  public static Map<DBRegKey, SortedSet<Integer>> linkPadRequirement(Map<String, Map<String, LinkAnalysis>> gla, String baseGeneID) {
    
    Map<DBRegKey, SortedSet<Integer>> needs = new HashMap<DBRegKey, SortedSet<Integer>>();
    
    for (String genomeID : gla.keySet()) {
      Map<String, DBGeneRegion.LinkAnalysis> lsMap = gla.get(genomeID);
      for (String geneID : lsMap.keySet()) {
        if (GenomeItemInstance.getBaseID(geneID).equals(baseGeneID)) {
          DBGeneRegion.LinkAnalysis ls = lsMap.get(geneID);
          for (String linkID : ls.status.keySet()) {
            DBGeneRegion.PadOffset po = ls.offsets.get(linkID);
            SortedSet<Integer> needed = needs.get(po.regKey);
            if (needed == null) {
              needed = new TreeSet<Integer>();
              needs.put(po.regKey, needed);
            }
            needed.add(Integer.valueOf(po.offset));
          }
        }
      }
    }
    return (needs);
  }
    
  /***************************************************************************
  **
  ** Current width of regions without any holder paddings. Has enough to keep all modules at same size, non-module parts with
  ** links from having to overlap links. Embedded holders without links can be tossed out.
  */
  
  public static int compressedRegionsWidth(String geneID, List<DBGeneRegion> dbgrList, Map<String, Map<String, LinkAnalysis>> gla, Set<DBRegKey> linkHolders) {
    
    Map<DBRegKey, SortedSet<Integer>> lhr = linkPadRequirement(gla, geneID);
    int width = 0;
    int num = dbgrList.size();
    for (int i = 0; i < num; i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      DBRegKey dbrk = dbgr.getKey();
      if (!dbgr.isHolder()) {
        width += dbgr.getWidth();
      } else if (linkHolders.contains(dbrk)){
        SortedSet<Integer> pads = lhr.get(dbrk);
        width += pads.size();
      }
    }
    return (width);
  } 
 
  /***************************************************************************
  **
  ** Current width of just the regions, including all holders. 
  */
  
  private static int currRegionsWidth(List<DBGeneRegion> dbgrList) {
    int num = dbgrList.size();
    if (num == 0) {
      return (0);
    }
    DBGeneRegion first = dbgrList.get(0);
    DBGeneRegion last = dbgrList.get(num - 1);
    return (last.getEndPad() - first.getStartPad() + 1);
  } 
  
  /***************************************************************************
  **
  ** Current width of regions with *minimum* shoulder padding requirements. Embedded non-regions are not compressed.
  */
  
  private static int fullRegionsWidth(String geneID, List<DBGeneRegion> dbgrList, Map<String, Map<String, LinkAnalysis>> gla, Set<DBRegKey> linkHolders) {
    Map<DBRegKey, SortedSet<Integer>> lhr = linkPadRequirement(gla, geneID);
    int fnhi = firstNonHolderIndex(dbgrList);
    int lnhi = lastNonHolderIndex(dbgrList);
    if ((fnhi == -1) || (lnhi == -1)) {
      throw new IllegalStateException();
    }
    DBGeneRegion first = dbgrList.get(fnhi);
    DBGeneRegion last = dbgrList.get(lnhi);
    int nonHolderWidth = last.getEndPad() - first.getStartPad() + 1;
    int num = dbgrList.size();
    
    int width = nonHolderWidth;
    for (int i = lnhi + 1; i < num; i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      DBRegKey dbrk = dbgr.getKey();
      if (linkHolders.contains(dbrk)){
        SortedSet<Integer> pads = lhr.get(dbrk);
        width += pads.size();
      }
    }

    return (width);
  }

  /***************************************************************************
  **
  ** After some operations, we may end up with a run of contiguous holders. Always
  ** merge these into a single holder
  */
  
  public static List<DBGeneRegion> mergeHolders(List<DBGeneRegion> dbgrList, Map<DBGeneRegion.DBRegKey, DBGeneRegion.DBRegKey> oldToNew) {
    ArrayList<DBGeneRegion> retval = new ArrayList<DBGeneRegion>();
    int num = dbgrList.size();
    DBGeneRegion lastHolder = null;
    for (int i = 0; i < num; i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      if (!dbgr.isHolder()) {
        retval.add(dbgr);
        lastHolder = null;
      } else {
        if (lastHolder != null) {
          DBGeneRegion lastReg = retval.remove(retval.size() - 1);
          DBGeneRegion.DBRegKey lastKey = lastReg.getKey();
          DBGeneRegion dbreg = new DBGeneRegion(lastKey.getInternalName(), lastHolder.getStartPad(), dbgr.getEndPad(), Gene.LEVEL_NONE, true);
          DBGeneRegion.DBRegKey nextKey = dbgr.getKey();
          if (!nextKey.equals(lastKey)) {
            oldToNew.put(nextKey, lastKey);
          }
          retval.add(dbreg);
          lastHolder = dbreg;
        } else {
          retval.add(dbgr);
          lastHolder = dbgr;
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** After some operations, we may end up with empty gaps that need to be
  ** replaced with holders.
  */
  
  public static List<DBGeneRegion> fillGapsWithHolders(List<DBGeneRegion> dbgrList, Gene gene) {
     
    int pads = gene.getPadCount();
    int firstPad = DBGene.DEFAULT_PAD_COUNT - pads;
    int lastPad = DBGene.DEFAULT_PAD_COUNT - 1;
    Set<Integer> avoid = getExistingHolderKeys(dbgrList);
    
    ArrayList<DBGeneRegion> retval = new ArrayList<DBGeneRegion>();
    int num = dbgrList.size();
    int prevPad = firstPad - 1;
    for (int i = 0; i < num; i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      if (dbgr.getStartPad() > (prevPad + 1)) {
        DBGeneRegion dbreg = new DBGeneRegion(DBGeneRegion.getNextHolderKey(retval, avoid), prevPad + 1, dbgr.getStartPad() - 1, Gene.LEVEL_NONE, true);
        retval.add(dbreg);
      }
      retval.add(dbgr);
      prevPad = dbgr.getEndPad();
    }
    if (prevPad < lastPad) {
      DBGeneRegion dbreg = new DBGeneRegion(DBGeneRegion.getNextHolderKey(retval, avoid), prevPad + 1, lastPad, Gene.LEVEL_NONE, true);
      retval.add(dbreg);
    }  
    return (retval);
  }

  
  /***************************************************************************
  **
  ** Gene is being expanded. Fix the list. Just means adding an empty holder or expanding existing
  ** holder in position 0.
  */
  
  public static List<DBGeneRegion> stretchTheList(List<DBGeneRegion> dbgrList, int numPads) {
    int currWidth = currRegionsWidth(dbgrList);
    DBGeneRegion first = dbgrList.get(0);
    int extend = numPads - currWidth;
    if (extend <= 0) {
      throw new IllegalArgumentException();
    }
     Set<Integer> avoid = getExistingHolderKeys(dbgrList);
    ArrayList<DBGeneRegion> retval = new ArrayList<DBGeneRegion>();
    int start;
    DBGeneRegion dbreg;
    if (first.isHolder()) {
      // Extend existing holder
      dbreg = new DBGeneRegion(first.getStringID(), first.getStartPad() - extend, first.getEndPad(), Gene.LEVEL_NONE, first.isHolder());
      start = 1;
    } else {
      // add new holder
      dbreg = new DBGeneRegion(DBGeneRegion.getNextHolderKey(retval, avoid), first.getStartPad() - extend, first.getStartPad() - 1, Gene.LEVEL_NONE, true);
      start = 0;
    }
    retval.add(dbreg);
    
    int num = dbgrList.size();
    for (int i = start; i < num; i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      retval.add(dbgr);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Gene is being shrunk. Trim the shoulders. If no links in shoulders, this means taking all
  ** the space from there.
  */
  
  private static List<DBGeneRegion> trimShoulders(List<DBGeneRegion> dbgrList, int shrink, int newGeneWidth, String geneID, 
                                                  Map<String, Map<String, LinkAnalysis>> gla, Set<DBRegKey> linkHolders) { 
    Map<DBRegKey, SortedSet<Integer>> lhr = linkPadRequirement(gla, geneID);
    ArrayList<DBGeneRegion> retval = new ArrayList<DBGeneRegion>();
    int num = dbgrList.size();
    DBGeneRegion dbgrF = dbgrList.get(0);
    int start = 0;
    if (dbgrF.isHolder()) {
      start = 1;
      int available = dbgrF.getWidth();
      DBRegKey dbrk = dbgrF.getKey();
      boolean needHolder = linkHolders.contains(dbrk);
      if (needHolder) {
        SortedSet<Integer> pads = lhr.get(dbrk);
        available -= pads.size();
      }
      if (shrink < available) {
        DBGeneRegion dbreg = new DBGeneRegion(dbgrF.getStringID(), dbgrF.getStartPad() + shrink, dbgrF.getEndPad(), Gene.LEVEL_NONE, true);
        retval.add(dbreg);
        shrink = 0;
      } else if (needHolder) { // Equal or greater, we drop the holder unless it holds links
        DBGeneRegion dbreg = new DBGeneRegion(dbgrF.getStringID(), dbgrF.getStartPad() + available, dbgrF.getEndPad(), Gene.LEVEL_NONE, true);
        retval.add(dbreg);
        shrink -= available;
      } else { // Must drop the holder entirely
        shrink -= available;
      }
    }
    for (int i = start; i < num - 1; i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      retval.add(dbgr);
    }
    DBGeneRegion dbgrL = dbgrList.get(num - 1);
    if (dbgrL.isHolder()) {
      int available = dbgrL.getWidth();
      DBRegKey dbrk = dbgrL.getKey();
      boolean needHolder = linkHolders.contains(dbrk);
      if (needHolder) {
        SortedSet<Integer> pads = lhr.get(dbrk);
        available -= pads.size();
      }
      if (shrink < available) {
        DBGeneRegion dbreg = new DBGeneRegion(dbgrL.getStringID(), dbgrL.getStartPad(), dbgrL.getEndPad() - shrink, Gene.LEVEL_NONE, true);
        retval.add(dbreg);
      } else if (needHolder) { // Equal or greater, we drop the holder unless it holds links
        DBGeneRegion dbreg = new DBGeneRegion(dbgrL.getStringID(), dbgrL.getStartPad(), dbgrF.getEndPad() - available, Gene.LEVEL_NONE, true);
        retval.add(dbreg);
      } // Else Must drop the holder entirely
    } else {
      retval.add(dbgrL);
    }
    
    //
    // Shove everybody over if needed:
    //
    
    int firstPad = DBGene.DEFAULT_PAD_COUNT - newGeneWidth;
    int currStart = retval.get(0).getStartPad();
    int rnum = retval.size();
    if (currStart < firstPad) {
      ArrayList<DBGeneRegion> nretval = new ArrayList<DBGeneRegion>();
      int shove = firstPad - currStart;
      for (int i = 0; i < rnum; i++) {
        DBGeneRegion dbgr = retval.get(i);
        DBGeneRegion dbreg = new DBGeneRegion(dbgr.getStringID(), dbgr.getStartPad() + shove, dbgr.getEndPad() + shove, 
                                              dbgr.getEvidenceLevel(), dbgr.isHolder());
        nretval.add(dbreg);
      }
      retval = nretval;
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Gene is being shrunk. Ditch and reduce inner holders. We have no holders on the
  ** shoulders.
  */
  
  private static List<DBGeneRegion> squeezeOutSpace(List<DBGeneRegion> dbgrList, int reduce, int newGeneWidth) {  
    ArrayList<DBGeneRegion> retval = new ArrayList<DBGeneRegion>();
    int num = dbgrList.size();
    int remains = reduce;
    int extracted = 0;
    Set<Integer> avoid = getExistingHolderKeys(dbgrList);
    
    for (int i = 0; i < num; i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      if (!dbgr.isHolder()) {
        DBGeneRegion dbreg = new DBGeneRegion(dbgr.getStringID(), dbgr.getStartPad() - extracted, 
                                              dbgr.getEndPad() - extracted, 
                                              dbgr.getEvidenceLevel(), dbgr.isHolder());
        retval.add(dbreg);
      } else {
        if ((i == 0) || (i == (num - 1))) {
          throw new IllegalStateException();
        }
        int width = dbgr.getWidth();
        if (width <= remains) {
          remains -= width;
          extracted += width;;
        } else {
          DBGeneRegion dbreg = new DBGeneRegion(dbgr.getStringID(), dbgr.getStartPad() - extracted, 
                                                dbgr.getEndPad() - extracted - remains, Gene.LEVEL_NONE, true);
          retval.add(dbreg);
          extracted += remains;
          remains = 0;
        }
      }
    }
    
    //
    // Shove everybody over if needed:
    //
    
    int firstPad = DBGene.DEFAULT_PAD_COUNT - newGeneWidth;
    int currStart = retval.get(0).getStartPad();
    if (currStart < firstPad) {
      ArrayList<DBGeneRegion> nretval = new ArrayList<DBGeneRegion>();
      int shove = firstPad - currStart;
      for (int i = 0; i < num; i++) {
        DBGeneRegion dbgr = dbgrList.get(i);
        DBGeneRegion dbreg = new DBGeneRegion(dbgr.getStringID(), dbgr.getStartPad() + shove, dbgr.getEndPad() + shove, 
                                              dbgr.getEvidenceLevel(), dbgr.isHolder());
        nretval.add(dbreg);
      }
      retval = nretval;
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Gene is being shrunk. Fix the list. 
  */
  
  public static List<DBGeneRegion> compressTheList(String geneID, List<DBGeneRegion> dbgrList, int currGeneWidth, int newGeneWidth, 
                                                   Map<String, Map<String, LinkAnalysis>> gla, Set<DBRegKey> linkHolders) {

    // Current width of regions without any holder paddings. Has enough to keep all modules at same size, non-module parts with
    // links from having to overlap links. Embedded holders without links can be tossed out:
    int compress = compressedRegionsWidth(geneID, dbgrList, gla, linkHolders);
    
    // current width of regions with *minimum* shoulder padding requirements. Embedded non-regions are not compressed.
    int full = fullRegionsWidth(geneID, dbgrList, gla, linkHolders);
    
    // Current width of just the regions, including all holders. 
    int currModuleWidth = currRegionsWidth(dbgrList);
    if (currModuleWidth != currGeneWidth) {
      throw new IllegalStateException();
    }
    
    // This is the bit we can cut off the shoulders, leaving them with enough space to handle non-module links:
    int shave = currGeneWidth - full;
    
    // This is he part we have to squish out:   
    int available = full - compress;
    
    // This is how far we have to go:
    int reduce = currGeneWidth - newGeneWidth;
     
    List<DBGeneRegion> retval;
    //
    // First case: We have enough space in the bounding "shoulders" to handle all the
    // reduction we need
    //
    if (reduce <= shave) {
      retval = trimShoulders(dbgrList, reduce, newGeneWidth, geneID, gla, linkHolders);  
    //
    // Second case: We have to squeeze some space out of the internal paddings. First, get rid of
    // shoulders.
    //  
    } else if (reduce <= available) {
      retval = trimShoulders(dbgrList, shave, newGeneWidth, geneID, gla, linkHolders);  
      retval = squeezeOutSpace(retval, reduce - shave, newGeneWidth);
      // compress
    } else {
      throw new IllegalArgumentException();
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Does the user have the option of drawing a new region?
  */
  
  public static boolean canAddARegion(List<DBGeneRegion> dbgrList) {
    if (dbgrList.isEmpty()) {
      throw new IllegalArgumentException();
    }
    //
    // Must be the case there is a holder
    //
    
    int num = dbgrList.size();
    for (int i = 0; i < num; i++) {
      if (dbgrList.get(i).isHolder()) { // Gotta have a holder!
        return (true);
      }
    }
    return (false);
  }
 
  /***************************************************************************
  **
  ** Does the user have the option of providing only a left pad?
  */
  
  public static boolean canGiveOnlyLeft(List<DBGeneRegion> dbgrList) {
    if (dbgrList.isEmpty()) {
      throw new IllegalArgumentException();
    }
    //
    // Must be the case there is a holder and a non-holder
    // right above it. Will allow a holder to be start == end.
    //
    
    int num = dbgrList.size();
    for (int i = 0; i < num - 1; i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      if (dbgr.isHolder()) { // Gotta have a holder!
        if (!dbgrList.get(i + 1).isHolder()) {
          return (true);
        }
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Does the user have the option of providing only a left pad?
  */
  
  public static boolean canGiveOnlyRight(List<DBGeneRegion> dbgrList) {
    if (dbgrList.isEmpty()) {
      throw new IllegalArgumentException();
    }
    //
    // Must be the case there is a holder and a non-holder
    // right above it. Will allow a holder to be start == end.
    //
    
    int num = dbgrList.size();
    for (int i = 1; i < num; i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      if (dbgr.isHolder()) { // Gotta have a holder!
        if (!dbgrList.get(i - 1).isHolder()) {
          return (true);
        }
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Are we a legitimate start pad?
  */
  
  public static boolean legalStart(List<DBGeneRegion> dbgrList, int startPadI) {
    if (dbgrList.isEmpty()) {
      throw new IllegalArgumentException();
    }
    int num = dbgrList.size();
    for (int i = 0; i < num; i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      if (!dbgr.isHolder()) { // Holders can be broken up
        int startGap = dbgr.getEndPad();
        int endGap = dbgr.getStartPad();
        if ((startGap <= startPadI) && (endGap >= startPadI)) {
          return (false);
        }
      }
    }
    return (true);
  }

  /***************************************************************************
  **
  ** Are we a legitimate left-only bounding pad?
  */
  
  public static DBGeneRegion legalLeftStart(List<DBGeneRegion> dbgrList, int startPadI) {
    if (dbgrList.isEmpty()) {
      throw new IllegalArgumentException();
    }
    int indx = whichRegionIndex(dbgrList, startPadI);

    int num = dbgrList.size();
    if (indx == (num - 1)) {
      return (null);
    }
    DBGeneRegion reg = dbgrList.get(indx);
    if (!reg.isHolder()) {
      return (null);
    }   
    reg = dbgrList.get(indx + 1);
    return ((!reg.isHolder()) ? reg : null);
  }
  
  /***************************************************************************
  **
  ** Insert new region
  */
  
  public static List<DBGeneRegion> insertInHolder(List<DBGeneRegion> dbgrList, DBGeneRegion newRegion) {
    if (dbgrList.isEmpty()) {
      throw new IllegalArgumentException();
    }
    int splitSlot = findTheSlot(dbgrList, newRegion.getStartPad(), newRegion.getEndPad());
    if (splitSlot == -1) {
      throw new IllegalArgumentException();
    }
    Set<Integer> avoid = getExistingHolderKeys(dbgrList);
    ArrayList<DBGeneRegion> retval = new ArrayList<DBGeneRegion>();
    int num = dbgrList.size();
    for (int i = 0; i < num; i++) {
      if (i != splitSlot) {
        retval.add(dbgrList.get(i));
        continue;
      }
      int newStart = newRegion.getStartPad();
      int newEnd = newRegion.getEndPad();
      DBGeneRegion splitHolder = dbgrList.get(i);
      if (newStart > splitHolder.getStartPad()) {
        DBGeneRegion newRegL = new DBGeneRegion(DBGeneRegion.getNextHolderKey(retval, avoid), splitHolder.getStartPad(), newStart - 1, Gene.LEVEL_NONE, true);
        retval.add(newRegL);
      }
      retval.add(newRegion);
      if (newEnd < splitHolder.getEndPad()) {
        DBGeneRegion newRegL = new DBGeneRegion(DBGeneRegion.getNextHolderKey(retval, avoid), newEnd + 1, splitHolder.getEndPad(), Gene.LEVEL_NONE, true);
        retval.add(newRegL);
      }  
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Are we a legitimate right-only bounding pad?
  */
  
  public static DBGeneRegion legalRightStart(List<DBGeneRegion> dbgrList, int startPadI) {
    if (dbgrList.isEmpty()) {
      throw new IllegalArgumentException();
    }
    int indx = whichRegionIndex(dbgrList, startPadI);
    if (indx == 0) {
      return (null);
    }
    DBGeneRegion reg = dbgrList.get(indx);
    if (!reg.isHolder()) {
      return (null);
    }   
    reg = dbgrList.get(indx - 1);
    return ((!reg.isHolder()) ? reg : null);
  }
  
  /***************************************************************************
  **
  ** Size of non-holders
  */
  
  public static int nonHolderSize(List<DBGeneRegion> dbgrList) {
    if (dbgrList.isEmpty()) {
      return (0);
    }
    int num = dbgrList.size();
    int count = 0;
    for (int i = 0; i < num; i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      if (!dbgr.isHolder()) {
        count++;
      }
    }
    return (count);
  }
  
  /***************************************************************************
  **
  ** Amount region can be slid
  */
  
  public static int slideSpace(List<DBGeneRegion> dbgrList, int currSlot, boolean toLeft, Map<DBRegKey, SortedSet<Integer>> padUse) {
    if (dbgrList.isEmpty()) {
      throw new IllegalArgumentException();
    }
    int num = dbgrList.size();
    int count = 0;
    
    if (toLeft) {
      // Holder on far left can be chucked:
      DBGeneRegion dbgL = dbgrList.get(0);
      if (dbgL.isHolder()) {
        count += dbgL.getWidth();
      } else if (dbgL.isLinkHolder()) {
        SortedSet<Integer> pads = padUse.get(dbgL.getKey());
        count += (dbgL.getWidth() - pads.size());
      }
      // Holder right next to us can be chucked:
      if (currSlot > 1) {
        dbgL = dbgrList.get(currSlot - 1);
        if (dbgL.isHolder()) {
          count += dbgL.getWidth();
        } else if (dbgL.isLinkHolder()) {
          SortedSet<Integer> pads = padUse.get(dbgL.getKey());
          count += (dbgL.getWidth() - pads.size());
        }
      }
    } else {
      DBGeneRegion dbgR = dbgrList.get(num - 1);
      if (dbgR.isHolder()) {
        count += dbgR.getWidth();
      } else if (dbgR.isLinkHolder()) {
        SortedSet<Integer> pads = padUse.get(dbgR.getKey());
        count += (dbgR.getWidth() - pads.size());
      }
      if (currSlot < (num - 2)) {
        dbgR = dbgrList.get(currSlot + 1);
        if (dbgR.isHolder()) {
          count += dbgR.getWidth();
        } else if (dbgR.isLinkHolder()) {
          SortedSet<Integer> pads = padUse.get(dbgR.getKey());
          count += (dbgR.getWidth() - pads.size());
        }
      }
    }
    return (count);
  }

  /***************************************************************************
  **
  ** Find the correct holder to split:
  */
  
  public static int findTheSlot(List<DBGeneRegion> dbgrList, int startPadI, int endPadI) {
    if (dbgrList.isEmpty()) {
      throw new IllegalArgumentException();
    }
    int whichStart = whichRegionIndex(dbgrList, startPadI);
    int whichEnd = whichRegionIndex(dbgrList, endPadI);
    if (whichStart == whichEnd) {
      return (whichStart);
    }
    return (-1); // NO correct placement!
  }
  
  /***************************************************************************
  **
  ** Given a pad, what is the list index of the region it is in? Assumed the
  ** list is in "valid order":
  */
  
  public static int whichRegionIndex(List<DBGeneRegion> dbgrList, int padNum) {
    if (dbgrList.isEmpty()) {
      throw new IllegalArgumentException();
    }
    int num = dbgrList.size();
    for (int i = 0; i < num; i++) {
      DBGeneRegion dbgr = dbgrList.get(i);
      if ((padNum >= dbgr.getStartPad()) && (padNum <= dbgr.getEndPad())) {
        return (i);
      }
    }
    throw new IllegalArgumentException();
  }
  
 
  /***************************************************************************
  **
  ** Answers if equal:
  */
  
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (!(other instanceof DBGeneRegion)) {
      return (false);
    }
    DBGeneRegion otherDbgr = (DBGeneRegion)other;
    
    if (otherDbgr.startPad_ != this.startPad_) {
      return (false);
    }
    if (otherDbgr.endPad_ != this.endPad_) {
      return (false);
    }
    if (otherDbgr.holder_ != this.holder_) {
      return (false);
    }
    if (otherDbgr.linkHolder_ != this.linkHolder_) {
      return (false);
    } 
    if (otherDbgr.evidenceLevel_ != this.evidenceLevel_) {
      return (false);
    }    
    if (otherDbgr.name_ == null) {
      return (this.name_ == null);
    }    
    if (!otherDbgr.name_.equals(this.name_)) {
      return (false);
    }
    return (true);
  }  
  
   /***************************************************************************
  **
  ** Get the Name:
  */
  
  public String getStringID() {
    return (name_);
  } 
  
  /***************************************************************************
  **
  ** Get the Name:
  */
  
  public String getName() {
    return ((holder_ || linkHolder_) ? " " : name_);
  } 
  
  /***************************************************************************
  **
  ** Get the Name:
  */
  
  public String getInternalName() {
    return ((holder_ || linkHolder_) ? name_ : " ");
  } 
  
  /***************************************************************************
  **
  ** Get the start pad
  */
  
  public int getStartPad() {
    return (startPad_);
  }
  
  /***************************************************************************
  **
  ** Get the end pad
  */
  
  public int getEndPad() {
    return (endPad_);
  }
  
  /***************************************************************************
  **
  ** Get the evidence level
  */
  
  public int getEvidenceLevel() {
    return (evidenceLevel_);
  }  
  
  /***************************************************************************
  **
  ** Get the holder status 
  */
  
  public boolean isHolder() {
    return (holder_);
  } 
  
  /***************************************************************************
  **
  ** Get the temporary linkholder status 
  */
  
  public boolean isLinkHolder() {
    return (linkHolder_);
  } 
  
  /***************************************************************************
  **
  ** Get the width
  */
  
  public int getWidth() {
    return ((endPad_ - startPad_) + 1);
  } 
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
 
  @Override
  public String toString() {
    return ("DBGeneRegion: name = " + name_ + " startPad = " + startPad_ 
            + " endPad = " + endPad_ + " evidence = " + evidenceLevel_ + " holder = " + holder_ +  " linkHolder = " + linkHolder_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<region");
    out.print(" name=\"");
    out.print(CharacterEntityMapper.mapEntities(name_, false));
    out.print("\" startPad=\"");
    out.print(startPad_);
    out.print("\" endPad=\"");
    out.print(endPad_);
    if (evidenceLevel_ != Gene.LEVEL_NONE) {
      out.print("\" evidence=\"");
      out.print(evidenceLevel_);    
    } 
    if (holder_) {
      out.print("\" holder=\"");
      out.print(holder_);    
    } 
    if (linkHolder_) {
      throw new IllegalStateException(); 
    } 
   
    out.println("\" />");        
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Handle region creation
  **
  */
  
  public static DBGeneRegion buildFromXML(Genome genome,
                                          Attributes attrs) throws IOException {
    String name = null;
    String startPad = null;
    String endPad = null;
    String evidence = null;
    String holder = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("name")) {
          name = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("startPad")) {
          startPad = val;
        } else if (key.equals("endPad")) {
          endPad = val;
        } else if (key.equals("evidence")) {
          evidence = val;
        } else if (key.equals("holder")) {
          holder = val;
        }
      }
    }
    return (new DBGeneRegion(name, startPad, endPad, evidence, holder));
  }  

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("region");
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor.  Do not use
  */

  protected DBGeneRegion() {
  }

  
  /***************************************************************************
  **
  ** Unique keys.
  */
        
  public static class DBRegKey implements Cloneable {
    
    private String name_;
    private boolean isHolder_;
    
    public DBRegKey(DBGeneRegion reg) {
      isHolder_ = reg.isHolder() || reg.isLinkHolder();
      name_ = (isHolder_) ? reg.getInternalName() : reg.getName();
    }
  
    public String getName() {
      if (isHolder_) {
        throw new IllegalStateException();
      }
      return (name_);
    }
    
    public String getInternalName() {
      if (!isHolder_) {
        throw new IllegalStateException();
      }
      return (name_);
    }
    
    public boolean isHolder() {
      return (isHolder_);
    }
  
    @Override
    public String toString() {
      return ("DBRegKey: name = " + name_ + " isHolder = " + isHolder_);
    }
    
    @Override
    public int hashCode() {
      return (name_.hashCode() + ((isHolder_) ? 1 : 0)); 
    }
       
    @Override
    public DBRegKey clone() {
      try {
        DBRegKey retval = (DBRegKey)super.clone();        
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return (true);
      }
      if (!(other instanceof DBRegKey)) {
        return (false);
      }
      
      DBRegKey otherDbgr = (DBRegKey)other;  
      if (this.isHolder_ != otherDbgr.isHolder_) {
        return (false);
      }
        
      String otherTest = (otherDbgr.name_ == null) ? "" : otherDbgr.name_;
      String myTest = (this.name_ == null) ? "" : this.name_;
  
      return (otherTest.equalsIgnoreCase(myTest));
    }  
  }  
  
  /***************************************************************************
  **
  ** Describes how linkage landing pads correspond to module definition
  */
  
  public enum LinkModStatus {CONSISTENT, ORPHANED, NON_MODULE, TRESSPASS}; 
  
  /***************************************************************************
  **
  ** Describes how linkage landing pads correspond to module definition
  */
  
  public static class LinkAnalysis {   
    public String geneID;
    public Map<String, LinkModStatus> status;
    public Map<String, PadOffset> offsets;
    public Map<Integer, DBRegKey> padToReg;
    public Map<DBRegKey, DBGeneRegion> keyToReg;
  
    public LinkAnalysis(String geneID) {
      this.geneID = geneID;
      this.status = new HashMap<String, LinkModStatus>();
      this.offsets = new HashMap<String, PadOffset>();
      this.padToReg = new HashMap<Integer, DBRegKey>();
      this.keyToReg = new HashMap<DBRegKey, DBGeneRegion>();
    }
    
    public LinkAnalysis(String geneID, Map<DBRegKey, DBGeneRegion> k2r, Map<Integer, DBRegKey> p2r) {
      this.geneID = geneID;
      this.status = new HashMap<String, LinkModStatus>();
      this.offsets = new HashMap<String, PadOffset>();
      this.padToReg = new HashMap<Integer, DBRegKey>(p2r);
      this.keyToReg = new HashMap<DBRegKey, DBGeneRegion>(k2r);
    }   
  }
   
  /***************************************************************************
  **
  ** Represents a landing pad location wrt region start
  */
  
  public static class PadOffset {   
    public DBRegKey regKey;
    public int offset;
    
    public PadOffset(DBRegKey regKey, int offset) {
      this.regKey = regKey;
      this.offset = offset;
    }
    
    @Override
    public String toString() {
      return ("PadOffset: regKey = " + regKey + " offset = " + offset);
    }
  }
  
  
  /***************************************************************************
  ** 
  ** Get greek characters
  */

  public static String[] getGreek() {
    return (greek_);
  }

  /***************************************************************************
  ** 
  ** We need to fix module definitions from before 7.0.1
  */

  public static List<String> legacyIOFixup(GenomeSource gSrc, ResourceManager rMan, Map<String, Map<String, Set<Integer>>> padsForModGenes) {
 
    //
    // Previous versions did not slash the leftmost and rightmost bounds, and only one line was drawn between regions, even
    // if they were not contiguous: the line was at the average between the two end pads. This meant that a divider could have 
    // been in the middle of an occupied pad. 
    //  
    // Thus, we extend the bounds of the leftmost and rightmost mods out to the end. Note that this means that if there
    // was an inbound link that was not actually in the formal module definition, it LOOKED like it was due to lack of
    // end-bounds (see hox11/13B into blimp). For regions with interstitial gaps, move bounds out to the average. 
    // If average is directly under a pad, we move it down. UNLESS there is a link right onto it. Then we see how many
    // submodel kid link placement on either side to tie break. We issue warning to check our decision.
    //
        
    TreeSet<String> errors = new TreeSet<String>();
    DBGenome genome = (DBGenome)gSrc.getGenome();
    Iterator<Gene> git = genome.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      if (gene.getNumRegions() == 0) {
        continue;
      }
      List<DBGeneRegion> glist = listFromGene(gene);
      int num = glist.size();
      
      boolean repairName = false;
      HashSet<String> used = new HashSet<String>();
      
      for (int i = 0; i < num; i++) {
        DBGeneRegion regL = glist.get(i);
        if (regL.getName().trim().equals("")) {
          repairName = true;
        } else {
          used.add(DataUtil.normKeyWithGreek(regL.getName().trim()));
        }
      }
      boolean needReplace = false;
      
      if (repairName) {
        int backupNum = 0;
        TreeSet<String> canUse = new TreeSet<String>(Arrays.asList(getGreek()));
        canUse.removeAll(used);
        ArrayList<DBGeneRegion> revlist1 = new ArrayList<DBGeneRegion>();
        for (int i = 0; i < num; i++) {
          DBGeneRegion regL = glist.get(i);
          if (regL.getName().trim().equals("")) {
            String toUse = canUse.pollFirst();
            if (toUse == null) {
              toUse = Integer.valueOf(backupNum++).toString();
            }      
            String msg = MessageFormat.format(rMan.getString("geneRegIOFixup.blankNameFix"), new Object[] {gene.getName(), toUse});
            errors.add(msg);
            regL = new DBGeneRegion(toUse, regL.getStartPad(), regL.getEndPad(), regL.getEvidenceLevel(), false);
          }
          revlist1.add(regL);
        }
        glist = revlist1;
        needReplace = true;
      }
 
      Map<String, Set<Integer>> padsForGene = padsForModGenes.get(gene.getID());
      if (padsForGene == null) {
        padsForGene = new HashMap<String, Set<Integer>>();       
      }

      int pads = gene.getPadCount();
      int firstPad = DBGene.DEFAULT_PAD_COUNT - pads;
      int lastPad = DBGene.DEFAULT_PAD_COUNT - 1;
  
      
      Integer lastStart = null;
      ArrayList<DBGeneRegion> revlist = new ArrayList<DBGeneRegion>();
      boolean onlyOne = (num == 1);
      for (int i = 0; i < num; i++) {
        DBGeneRegion regL = glist.get(i);
   
        //
        // Does the first region extend to the leftmost pad? If not, make it.
        //
        if (i == 0) {
          if (regL.getStartPad() > firstPad) {
            regL = new DBGeneRegion(regL.getName(), firstPad, regL.getEndPad(), regL.getEvidenceLevel(), false);
            String msg = MessageFormat.format(rMan.getString("geneRegIOFixup.leftFix"), new Object[] {gene.getName()});
            errors.add(msg);
            needReplace = true;
          }  
        }
        
        DBGeneRegion regR;
        boolean rightMost = false;
        if (onlyOne) {
          regR = regL;
          rightMost = true;
        } else if (i < (num - 1)) {
          regR = glist.get(i + 1);
          rightMost = (i == (num - 2));
        } else {
          break;
        }
        
        //
        // Repair last time
        //
        
        if (lastStart != null) {
          regL = new DBGeneRegion(regL.getName(), lastStart.intValue(), regL.getEndPad(), regL.getEvidenceLevel(), false);
          lastStart = null;
        }  

        if (regL.getEndPad() != (regR.getStartPad() - 1)) {
          double avg = ((regR.getStartPad() + regL.getEndPad())) / 2.0;
          if (Math.floor(avg) == avg) { // under a pad DANGER!
            //
            // If the split is directly under a pad, and some link actually lands
            // on the pad, we have a problem: it is ambiguous. Default is to put
            // the new split below the shared pad, but 
            int average = (int)avg;
            int newEnd = average - 1;
            for (String linkID : padsForGene.keySet()) {
              Set<Integer> padsForLink = padsForGene.get(linkID);
              String msg = MessageFormat.format(rMan.getString("geneRegIOFixup.boundaryUnderPad"), new Object[] {gene.getName()});  
              errors.add(msg);
              boolean tieBreak = padsForLink.contains(Integer.valueOf(average));              
              if (tieBreak) {
                msg = MessageFormat.format(rMan.getString("geneRegIOFixup.linkIntoBoundary"), new Object[] {gene.getName()});
                errors.add(msg);
                int above = 0;
                int below = 0;
                int equals = 0;
                for (Integer pad : padsForLink) {
                  if (pad.intValue() == average) {
                    equals++;
                  } else if (pad.intValue() > average) {
                    above++;
                  } else if (pad.intValue() < average) {
                    below++;
                  }
                }
                if (above < below) {
                  newEnd++;
                }
              }
              lastStart = Integer.valueOf(newEnd + 1);
              regL = new DBGeneRegion(regL.getName(), regL.getStartPad(), newEnd, regL.getEvidenceLevel(), false);  
            }
          } else { // between pads
            int newEnd = (int)Math.floor(avg);
            lastStart = Integer.valueOf((int)Math.floor(avg) + 1);
            regL = new DBGeneRegion(regL.getName(), regL.getStartPad(), newEnd, regL.getEvidenceLevel(), false);
            String msg = MessageFormat.format(rMan.getString("geneRegIOFixup.gapped"), new Object[] {gene.getName()});
            errors.add(msg);
          }  
        }
        
        //
        // Does the last region extend to the rightmost pad? If not, make it.
        //
        
        if (rightMost) {
          if ((regR.getEndPad() < lastPad) || (lastStart != null)) {
            int startPad = (lastStart != null) ? lastStart.intValue() : regR.getStartPad();  
            regR = new DBGeneRegion(regR.getName(), startPad, lastPad, regR.getEvidenceLevel(), false);
            String msg = MessageFormat.format(rMan.getString("geneRegIOFixup.rightFix"), new Object[] {gene.getName()});
            errors.add(msg);
            needReplace = true;
            lastStart = null;
          }
        }
        
        if (!onlyOne) {
          revlist.add(regL);
        }
        if (rightMost) {
          revlist.add(regR); 
        }
      }
      
      if (needReplace) {     
        if (!validOrder(revlist, firstPad, lastPad)) {
          throw new IllegalStateException();
        }      
        genome.changeGeneRegions(gene.getID(), revlist);
      }
    }
    
    return (new ArrayList<String>(errors));  
  }
}
