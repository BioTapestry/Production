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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

/****************************************************************************
**
** A class for creating unique abbreviations for a set of Strings
*/

public class Abbreviator {
  
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

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public Abbreviator() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Take a set of strings, return a mapping to abbreviations
  */

  public Map<String, String> abbreviate(Set<String> strings, int maxLen) { 
    return (abbreviateAppend(new HashMap<String, String>(), strings, maxLen));
  }
  
  /***************************************************************************
  ** 
  ** Take a set of strings, return a mapping to abbreviations.  Preserves the
  ** provided existing abbreviation mapping.
  */

  public Map<String, String> abbreviateAppend(Map<String, String> existing, Set<String> strings, int maxLen) {    
    HashMap<String, Set<String>> starter = new HashMap<String, Set<String>>();   
    HashMap<String, String> invExisting = new HashMap<String, String>();
    Iterator<String> eit = existing.keySet().iterator();
    while (eit.hasNext()) {
      String ex = (String)eit.next();    
      String oabb = (String)existing.get(ex);
      invExisting.put(oabb.toUpperCase(), ex.toUpperCase());
    }
    
    //
    // Bin strings by first character to get things started.  Everybody to uppercase:
    //
    
    HashSet<String> empties = new HashSet<String>();
    Iterator<String> sit = strings.iterator();
    while (sit.hasNext()) {
      String str = sit.next().toUpperCase();
      if (str.trim().equals("")) {
        empties.add(str);
        continue;
      }
      String pref = str.substring(0, 1);
      Set<String> minSet = starter.get(pref);
      if (minSet == null) {
        minSet = new HashSet<String>();
        starter.put(pref, minSet);
      }
      minSet.add(str);
    }
    
    //
    // Work on each set of different first characters.  Dig up useful differences.
    
    HashSet<String> disasters = new HashSet<String>(empties);
    Iterator<String> stit = starter.keySet().iterator();
    while (stit.hasNext()) {
      String pref = stit.next();
      Set<String> elemsForPref = starter.get(pref);
      HashMap<String, SortedSet<CharPos>> useful = new HashMap<String, SortedSet<CharPos>>();
      findUniqChars(1, elemsForPref, useful);
      List<String> order = orderByCandidateCount(useful);
      HashMap<String, Set<String>> results = new HashMap<String, Set<String>>();
      char check = pref.charAt(0);
      findUniqueAbbrevs(check, useful, order, results);
      forceUnique(results, invExisting, disasters);
    }
    
    //
    // Handle difficult cases (including blank and empty strings):
    //
    
    Iterator<String> dit = disasters.iterator();
    while (dit.hasNext()) {
      String disaster = dit.next();
      Set<String> faSet = invExisting.keySet();
      String utag = getUniqueTag(faSet, maxLen);
      if (utag == null) {
        throw new IllegalArgumentException();
      }
      invExisting.put(utag, disaster);
    } 
    
    //
    // invert for return:
    //
    
    HashMap<String, String> preRetval = new HashMap<String, String>();
    Iterator<String> fit = invExisting.keySet().iterator();
    while (fit.hasNext()) {
      String abb = fit.next();    
      String orig = (String)invExisting.get(abb);
      preRetval.put(orig, abb);
    }
    
    HashMap<String, String> retval = new HashMap<String, String>();
    HashSet<String> allStrs = new HashSet<String>(strings);
    allStrs.addAll(existing.keySet());
    Iterator<String> fit2 = allStrs.iterator();
    while (fit2.hasNext()) {
      String origStr = fit2.next();    
      String abb = preRetval.get(origStr.toUpperCase());
      retval.put(origStr, abb);
    }    
       
    return (retval);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Record useful differences between strings
  */

  private void findUniqChars(int checkPos, Set<String> elems, Map<String, SortedSet<CharPos>> results) {
    HashSet<String> survivingElems = new HashSet<String>();
    HashMap<String, Set<String>> diffs = new HashMap<String, Set<String>>();
    Iterator<String> eit = elems.iterator();
    while (eit.hasNext()) {
      String elem = eit.next();
      if (elem.length() <= checkPos) {
        SortedSet<CharPos> cpsForElem = results.get(elem);
        if (cpsForElem == null) {
          cpsForElem = new TreeSet<CharPos>();
          results.put(elem, cpsForElem);
        }
        cpsForElem.add(new CharPos("", checkPos));
        continue;
      } else {
        survivingElems.add(elem);
      }
      String diffStr = new String(elem.substring(checkPos, checkPos + 1));
      Set<String> elemsForChar = diffs.get(diffStr);
      if (elemsForChar == null) {
        elemsForChar = new HashSet<String>();
        diffs.put(diffStr, elemsForChar);
      }
      elemsForChar.add(elem);
    }
    
    Iterator<String> bit = diffs.keySet().iterator();
    while (bit.hasNext()) {
      String diffStr = bit.next();   
      Set<String> dbs = diffs.get(diffStr);    
      if (dbs.size() == 1) {
        String elem = dbs.iterator().next();
        SortedSet<CharPos> cpsForElem = results.get(elem);
        if (cpsForElem == null) {
          cpsForElem = new TreeSet<CharPos>();
          results.put(elem, cpsForElem);
        }
        cpsForElem.add(new CharPos(diffStr, checkPos));
      }
    }
    
    if (!survivingElems.isEmpty()) {
      findUniqChars(checkPos + 1, survivingElems, results);
    }
    
    return;
  }

  /***************************************************************************
  ** 
  ** Figure out order to assign 
  */

  private List<String> orderByCandidateCount(Map<String, SortedSet<CharPos>> cands) {
    TreeMap<Integer, ArrayList<String>> sortBins = new TreeMap<Integer, ArrayList<String>>();
    Iterator<String> kit = cands.keySet().iterator();
    while (kit.hasNext()) {
      String cand = kit.next();   
      SortedSet<CharPos> cps = cands.get(cand);
      Integer sizeObj = new Integer(cps.size());
      ArrayList<String> candsForSize = sortBins.get(sizeObj);
      if (candsForSize == null) {
        candsForSize = new ArrayList<String>();
        sortBins.put(sizeObj, candsForSize);
      }
      candsForSize.add(cand);    
    }
 
    ArrayList<String> retval = new ArrayList<String>();    
    Iterator<Integer> sbit = sortBins.keySet().iterator();
    while (sbit.hasNext()) {
      Integer sizeObj = sbit.next();
      ArrayList<String> sbg = sortBins.get(sizeObj);
      Collections.sort(sbg, new LengthComparator());
      retval.addAll(sbg);    
    }
    return (retval);
  }    
  
  /***************************************************************************
  ** 
  ** Take the element with the smallest number of possible differences
  ** and keep trying to create unique abbrevs.  If we need to give up, then
  ** do so.
  */

  private void findUniqueAbbrevs(char firstChar, Map<String, SortedSet<CharPos>> cands, List<String> checkOrder, Map<String, Set<String>> results) {
    int num = checkOrder.size();
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < num; i++) {
      String cand = (String)checkOrder.get(i);
      SortedSet<CharPos> diffs = cands.get(cand);
      Iterator<CharPos> dit = diffs.iterator();
      while (dit.hasNext()) {
        CharPos cp = dit.next();
        buf.setLength(0);
        buf.append(firstChar);
        buf.append(cp.character);
        String abbrev = buf.toString();
        Set<String> rset = results.get(abbrev);
        if (rset == null) {
          rset = new HashSet<String>();
          results.put(abbrev, rset);
          rset.add(cand);
          break;
        } else if (!dit.hasNext()) {
          rset.add(cand);
        }
      }
    }
  }  
  
  /***************************************************************************
  ** 
  ** Take the element with the smallest number of possible differences
  ** and keep trying to create unique abbrevs.  If we need to give up, then
  ** do so.
  */

  private void forceUnique(Map<String, Set<String>> results, Map<String, String> fullAbbrev, Set<String> disasters) {
    Iterator<String> kit = results.keySet().iterator();
    while (kit.hasNext()) {
      String abbrev = kit.next();
      Set<String> rset = results.get(abbrev);
      if (rset.size() == 0) {
        throw new IllegalStateException();
      }
      if (rset.size() == 1) {
        String forAbbrev = (String)fullAbbrev.get(abbrev);
        if (forAbbrev != null) {
          addThirdForUnique(abbrev, rset, fullAbbrev, disasters);
        } else {
          fullAbbrev.put(abbrev, rset.iterator().next());
        }
        continue;
      }
      //
      // Try adding third random character to make unique:
      //
      addThirdForUnique(abbrev, rset, fullAbbrev, disasters);
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Take the element with the smallest number of possible differences
  ** and keep trying to create unique abbrevs.  If we need to give up, then
  ** do so.
  */

  private void addThirdForUnique(String abbrev, Set<String> rset, Map<String, String> fullAbbrev, Set<String> disasters) {
    StringBuffer buf = new StringBuffer();
    Iterator<String> rsit = rset.iterator();
    while (rsit.hasNext()) {
      String cand = rsit.next();
      Character nextChar = new Character(getFirstCharacter());
      while (nextChar != null) {
        buf.setLength(0);
        buf.append(abbrev);
        buf.append(nextChar);
        String testAbbrev = (String)fullAbbrev.get(buf.toString());
        if (testAbbrev == null) {
          fullAbbrev.put(buf.toString(), cand);
          break;
        }
        nextChar = getNextCharacter(nextChar.charValue());
      }
      if (nextChar == null) {
        disasters.add(cand);
      }
    }
  }  
  
  /***************************************************************************
  ** 
  ** Bin by prefixes:
  */

  private void dumpset(Map<String, Set<String>> bins) {
    Iterator<String> rit = bins.keySet().iterator();
    while (rit.hasNext()) {
      String key = (String)rit.next();
      Set<String> abbrev = bins.get(key);
      System.out.println(key + " -> " + abbrev);
    }
  } 

  /***************************************************************************
  ** 
  ** Emergency fallback.  Get a unique tag of given length or less.  May still
  ** fail (and return null) for huge sets.  New string not added to set.
  */

  private String getUniqueTag(Set<String> existing, int maxLen) {
    if (maxLen < 1) {
      throw new IllegalArgumentException();
    }
    StringBuffer buf = new StringBuffer();
    char first = getFirstCharacter();
    buf.append(first);
    while (existing.contains(buf.toString())) {
      int lastPos = buf.length() - 1;
      char lastChar = buf.charAt(lastPos);
      buf.deleteCharAt(lastPos);
      Character next = getNextCharacter(lastChar);
      if (next == null) {
        if (lastPos == (maxLen - 1)) {
          return (null);
        }
        buf.append(lastChar);
        buf.append(getFirstCharacter());
      } else {
        buf.append(next);
      }
    }
    return (buf.toString());
  }
   
  /***************************************************************************
  ** 
  ** Get first character
  */

  private char getFirstCharacter() {
    return ('0');
  }
  
  /***************************************************************************
  ** 
  ** Get next character.  Null if we are at the end of the line.
  */

  private Character getNextCharacter(char lastChar) {
    if (lastChar == '9') {
      return (new Character('_'));
    } else if (lastChar == '_') {
      return (new Character('A'));
    } else if (lastChar == 'Z') {
      return (null); 
    } else {
      return (new Character((char)((int)lastChar + 1))); 
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Sorts by string length
  **
  */
  
  private static class LengthComparator implements Comparator<Object> {
    public int compare(Object o1, Object o2) {
      String s1 = (String)o1;
      String s2 = (String)o2;
      return (s1.length() - s2.length());
    }
  }     
  
  /***************************************************************************
  **
  ** Used to record character differences
  */ 
    
  private static class CharPos implements Comparable<Object> {

    String character;
    int pos;
    
    CharPos(String character, int pos) {
      this.character = character;
      this.pos = pos;
    }
    
    public int hashCode() {
      return (pos + character.hashCode());
    }

    public String toString() {
      return (character + ": " + pos);
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof CharPos)) {
        return (false);
      }
      return (this.compareTo(other) == 0);
    } 

    public int compareTo(Object o) {
      CharPos other = (CharPos)o;
      int posDiff = this.pos - other.pos;
      if (posDiff != 0) {
        return (posDiff);
      }
      return (this.character.compareTo(other.character));
    } 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // TEST FRAMEWORK
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Test frame
  */
  
  public static void main(String[] argv) {
    
    Abbreviator abv = new Abbreviator();
   
    HashSet<String> inputs = new HashSet<String>();
    inputs.add("aaaa");
    inputs.add("aaab");
    inputs.add("aaaac");
    inputs.add("aaaaaaaad");    
    inputs.add("cdefg");
    inputs.add("cdefga");
    inputs.add("ee1");
    inputs.add("eee");   
    inputs.add("x");       
    inputs.add("eee1");
    inputs.add("g12345");
    inputs.add("0");
    inputs.add("1");
    inputs.add("2");
    inputs.add("");
    inputs.add("    ");
    inputs.add("     ");
    
    Map<String, String> results = abv.abbreviate(inputs, 3);
    Iterator<String> rit = results.keySet().iterator();
    while (rit.hasNext()) {
      String str = rit.next();
      String abbrev = results.get(str);
      System.out.println(str + " ->(" + abbrev + ")");
    }
   
    HashSet<String> testSet = new HashSet<String>();
    testSet.add("EC");
    testSet.add("P");
    testSet.add("EM");    
    results = abv.abbreviateAppend(new HashMap<String, String>(), testSet, 3);
    rit = results.keySet().iterator();
    while (rit.hasNext()) {
      String str = (String)rit.next();
      String abbrev = (String)results.get(str);
      System.out.println(str + " -> " + abbrev);
    }    
    return;
  }  
}
