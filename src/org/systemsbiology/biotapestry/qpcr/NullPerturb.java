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

package org.systemsbiology.biotapestry.qpcr;

import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.util.ResourceManager;


/****************************************************************************
**
** This holds QPCR Null perturbations
*/

class NullPerturb implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private BTState appState_;
  private ArrayList sources_;
  private ArrayList targets_;
  
  private TreeSet investigators_;  // Added after QPCR was made obsolete to
                                   // support perturb HTML display
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

   NullPerturb(BTState appState) {
    appState_ = appState;
    this.sources_ = new ArrayList();
    this.targets_ = new ArrayList();
    this.investigators_ = new TreeSet();
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

   NullPerturb(BTState appState, List sources) {
    appState_ = appState;
    this.sources_ = new ArrayList();
    int num = sources.size();
    for (int i = 0; i < num; i++) {
      this.sources_.add(((Source)sources.get(i)).clone());
    }
    this.targets_ = new ArrayList();
    this.investigators_ = new TreeSet();
  }  
   
  /***************************************************************************
  **
  ** Copy Constructor
  */

   NullPerturb(NullPerturb other) {
    mergeInNewValues(other);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Answer if our set of sources contains one of the given sources, or TRUE
  ** if otherSources is null!
  */
  
  boolean sourcesContainOneOrMore(List otherSources) { 
    if (otherSources != null) {
      Iterator srcIt = sources_.iterator();
      while (srcIt.hasNext()) {
        Source src = (Source)srcIt.next();
        if (DataUtil.containsKey(otherSources, src.getBaseType())) {
          return (true);
        }
      }
      return (false);
    } else {
      return (true);
    }   
  }
   
  /***************************************************************************
  **
  ** Answer if our set of sources matches the given set
  */
  
   boolean sourcesMatch(List otherSources) {
    if (otherSources.size() != sources_.size()) {
      return (false);
    }
    QPCRData.SourceComparator srcCmp = new QPCRData.SourceComparator();   
    TreeSet mySorted = new TreeSet(srcCmp);
    mySorted.addAll(sources_);
    TreeSet otherSorted = new TreeSet(srcCmp);
    otherSorted.addAll(otherSources);
    Iterator msit = mySorted.iterator();
    Iterator osit = otherSorted.iterator();    
    while (msit.hasNext()) {
      Source ms = (Source)msit.next();
      Source os = (Source)osit.next();
      if (srcCmp.compare(ms, os) != 0) {
        return (false);
      }
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Clone
  */

   public Object clone() {
    try {
      NullPerturb newVal = (NullPerturb)super.clone();
      newVal.sources_ = new ArrayList();
      Iterator osit = this.sources_.iterator();
      while (osit.hasNext()) {
        newVal.sources_.add(((Source)osit.next()).clone());
      }
      newVal.targets_ = new ArrayList();
      Iterator otit = this.targets_.iterator();
      while (otit.hasNext()) {
        newVal.targets_.add(((NullTarget)otit.next()).clone());
      }     
      newVal.investigators_ = new TreeSet(this.investigators_);     
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }

  /***************************************************************************
  **
  ** Update given data range.
  */
  
   void updateDataRange(NullTimeSpan defaultSpan, int[] range) {
    int size = targets_.size();
    for (int i = 0; i < size; i++) {
      NullTarget targ = (NullTarget)targets_.get(i);
      targ.updateDataRange(defaultSpan, range);
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Merge in new values
  */
  
   void mergeInNewValues(NullPerturb other) {
    this.appState_ = other.appState_;
    this.sources_ = new ArrayList();
    Iterator osit = other.sources_.iterator();
    while (osit.hasNext()) {
      this.sources_.add(((Source)osit.next()).clone());
    }
    this.targets_ = new ArrayList();
    Iterator otit = other.targets_.iterator();
    while (otit.hasNext()) {
      this.targets_.add(((NullTarget)otit.next()).clone());
    }
    this.investigators_ = new TreeSet(other.investigators_); 
    return;
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the sources
  */
  
   Iterator getSources() {
    return (sources_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the targets
  */
  
   Iterator getTargets() {
    return (targets_.iterator());
  }  

  /***************************************************************************
  **
  ** Add a source
  **
  */
  
   void addSource(Source source) {
    sources_.add(source);
    return;
  } 
  
  /***************************************************************************
  **
  ** Delete a source
  **
  */
  
   void deleteSource(int index) {
    sources_.remove(index);
    return;
  }  

  /***************************************************************************
  **
  ** Get the given source
  **
  */
  
   Source getSource(int index) {
    return ((Source)sources_.get(index));
  }  
  
  /***************************************************************************
  **
  ** Get the source count
  **
  */
  
   int getSourceCount() {
    return (sources_.size());
  }
  
  /***************************************************************************
  **
  ** Replace the given source
  **
  */
  
   void replaceSource(int index, Source source) {
    sources_.set(index, source);
    return;
  }  
  
  /***************************************************************************
  **
  ** Add a target
  **
  */
  
   void addTarget(NullTarget target) {
    targets_.add(target);
    return;
  }
  
  /***************************************************************************
  **
  ** Delete a target
  **
  */
  
   void deleteTarget(int index) {
    targets_.remove(index);
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the given target
  **
  */
  
   NullTarget getTarget(int index) {
    return ((NullTarget)targets_.get(index));
  } 
  
  /***************************************************************************
  **
  ** Get the set of footnote numbers used by this null perturb
  */
  
   Set getFootnoteNumbers() {
    HashSet retval = new HashSet();
    Iterator pers = getSources();
    while (pers.hasNext()) {
      Source src = (Source)pers.next();
      List notes = src.getFootnoteNumbers();
      retval.addAll(notes);
    }
    Iterator pert = getTargets();
    while (pert.hasNext()) {
      NullTarget nt = (NullTarget)pert.next();
      List notes = nt.getFootnoteNumbers();
      retval.addAll(notes);
    } 
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Set the note.  Used for legacy input
  */
  
   void setNote(String note, QPCRData qpcr) throws IOException {
    TimeAxisDefinition tad = appState_.getDB().getTimeAxisDefinition();
    if (tad.getUnits() != TimeAxisDefinition.LEGACY_UNITS) {
      throw new IOException();
    }
    convertNoteToTargets(note, targets_, qpcr.getLegacyNullPerturbationsDefaultTimeSpan());
    return;
  }

  /***************************************************************************
  **
  ** Answer if this perturbation lists one or more of the given target genes
  **
  */
  
   boolean appliesToTargets(List targetGeneNames) {
    return (appliesToTargetsGetTarget(targetGeneNames) != null);
  }  
  
  /***************************************************************************
  **
  ** Answer if this perturbation lists one or more of the given target genes
  **
  */
  
   NullTarget appliesToTargetsGetTarget(List targetGeneNames) {
    Iterator tgit = targetGeneNames.iterator();
    while (tgit.hasNext()) {
      String targetName = (String)tgit.next();
      Iterator trgit = getTargets();
      while (trgit.hasNext()) {
        NullTarget nt = (NullTarget)trgit.next();
        String nextTarg = nt.getTarget();
        if (DataUtil.keysEqual(targetName, nextTarg)) {
          return (nt);
        }
      }      
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Get the target indices that match the name, if any
  **
  */
  
   List appliesToTargetGetTargetIndices(String targetName) {
    ArrayList retval = new ArrayList();
    int numTarg = targets_.size();
    for (int i = 0; i < numTarg; i++) {
      NullTarget nt = getTarget(i);
      String nextTarg = nt.getTarget();
      if (DataUtil.keysEqual(targetName, nextTarg)) {
        retval.add(new Integer(i));
      }
    }      
    return (retval);
  } 

  /***************************************************************************
  **
  ** Get the target count
  **
  */
  
   int getTargetCount() {
    return (targets_.size());
  }  
  
  /***************************************************************************
  **
  ** Delete the given target indices
  **
  */
  
   void deleteTargetIndices(List indices) {
    ArrayList newTargs = new ArrayList();
    int numTarg = targets_.size();
    for (int i = 0; i < numTarg; i++) {
      if (!indices.contains(new Integer(i))) {
        newTargs.add(targets_.get(i));
      }
    }
    targets_ = newTargs;
    return;
  }  

  /***************************************************************************
  **
  ** Returns a display string for the targets
  **
  */
  
   String buildTargetDisplayString(NullTimeSpan defaultSpan) {

    //
    // Group targets into common span sets:
    //
    
    TimeAxisDefinition tad = appState_.getDB().getTimeAxisDefinition();
    String units = tad.unitDisplayAbbrev();
    boolean isSuffix = tad.unitsAreASuffix();
       
    Map spanLists = groupInSpanSets(defaultSpan);
    
    //
    // Build alphabetic lists of default and single-target spans:
    //
    
    HashSet defaultSet = new HashSet();
    defaultSet.add(defaultSpan);
    HashMap multiTargs = new HashMap();
    TreeMap defAndSingles = new TreeMap();
    Iterator slit = spanLists.keySet().iterator();
    while (slit.hasNext()) {
      Set spanSet = (Set)slit.next();
      SortedMap targMap = (SortedMap)spanLists.get(spanSet);
      if (spanSet.equals(defaultSet) || (targMap.size() == 1)) {
        mergeMaps(defAndSingles, targMap);
      } else {
        multiTargs.put(spanSet, targMap);
      }
    }
        
    StringBuffer buf = new StringBuffer();
    boolean isFirst = true;
    
    Iterator dsit = defAndSingles.keySet().iterator();
    while (dsit.hasNext()) {
      String dKey = (String)dsit.next();
      List targForTString = (List)defAndSingles.get(dKey);
      Iterator tftit = targForTString.iterator();
      while (tftit.hasNext()) {
        NullTarget nt = (NullTarget)tftit.next();
        if (isFirst) {
         isFirst = false;
        } else {
          buf.append(", ");
        }
        Set spanSet = nt.getSpansInSet();
        if (spanSet.isEmpty()) {
          spanSet.add(new NullTimeSpan(defaultSpan));
        }
        boolean showSpans = (!spanSet.equals(defaultSet));
        buf.append(nt.displayString(showSpans, units, isSuffix));
      }
    }
    
    Iterator mtit = multiTargs.keySet().iterator();
    while (mtit.hasNext()) {
      Set spanSet = (Set)mtit.next();
      SortedMap targMap = (SortedMap)spanLists.get(spanSet);
      Iterator tmit = targMap.keySet().iterator();
      boolean isFirstForBracket = true;
      while (tmit.hasNext()) {
        String dKey = (String)tmit.next();
        List targForTString = (List)targMap.get(dKey);
        Iterator tftit = targForTString.iterator();
        while (tftit.hasNext()) {
          NullTarget nt = (NullTarget)tftit.next();
          if (isFirstForBracket) {
            isFirstForBracket = false;
            if (isFirst) {
              isFirst = false;
              buf.append("[");
            } else {
              buf.append(", [");
            }
          } else {          
            if (isFirst) {
              isFirst = false;
            } else {
              buf.append(", ");
            }
          } 
          boolean showSpans = !tmit.hasNext() && !tftit.hasNext();
          buf.append(nt.displayString(showSpans, units, isSuffix));
          if (showSpans) {
            buf.append("]");
          }
        }
      } 
    }

    return (buf.toString());
  }
  
  /***************************************************************************
  **
  ** Add investigators
  **
  */
  
   void addInvestigators(Set invest) {
    investigators_.addAll(invest);
    return;
  }
    
  /***************************************************************************
  **
  ** Returns a display string for the Investigators
  **
  */
  
   String buildInvestDisplayString() {
     
    ResourceManager rMan = appState_.getRMan();
    String etAl = rMan.getString("qpcrData.andOthers");
    int numInv = investigators_.size();
    if (numInv == 1) {  
      String only = (String)investigators_.iterator().next();
      if (only.equals(etAl)) {
        return (null);
      }
    }
     
    int count = 0;
    boolean etAlPending = investigators_.contains(etAl);
    
    StringBuffer buf = new StringBuffer();
    Iterator iit = investigators_.iterator();
    while (iit.hasNext()) {
      String inv = (String)iit.next();
      if (inv.equals(etAl)) {
        continue;
      }
      buf.append(inv);
      if (count < (numInv - 2)) { 
        buf.append(", ");
      } else if (count < (numInv - 1)) {
        if (etAlPending) {
          buf.append(", ");
        } else {
          buf.append(" &amp ");
        } 
      }
      count++;
    }
    if (etAlPending) {
      buf.append(etAl);
    }
    return (buf.toString());
  }

 /**********************************************************************
  **
  ** Merge target maps
  **
  */
  
  private void mergeMaps(SortedMap destMap, SortedMap srcMap) {
    Iterator smit = srcMap.keySet().iterator();
    while (smit.hasNext()) {
      String smkey = (String)smit.next();
      List smList = (List)srcMap.get(smkey);
      List dstList = (List)destMap.get(smkey);
      if (dstList == null) {
        destMap.put(smkey, new ArrayList(smList));
      } else {
        dstList.addAll(smList);
      }
    }
    return;
  }    

  /***************************************************************************
  **
  ** Group targets into common span sets
  **
  */
  
  private Map groupInSpanSets(NullTimeSpan defaultSpan) {
    //
    // Group targets into common span sets:
    //
    Iterator trgs = getTargets();
    HashMap spanMaps = new HashMap();
    while (trgs.hasNext()) {
      NullTarget trg = (NullTarget)trgs.next();
      Set spanSet = trg.getSpansInSet();
      if (spanSet.isEmpty()) {
        spanSet.add(new NullTimeSpan(defaultSpan));
      }
      SortedMap currMap = (SortedMap)spanMaps.get(spanSet);
      ArrayList targsForTarg;
      if (currMap == null) {
        currMap = new TreeMap();
        spanMaps.put(spanSet, currMap);
        targsForTarg = new ArrayList();
        currMap.put(trg.getTarget(), targsForTarg);
      } else {
        targsForTarg = (ArrayList)currMap.get(trg.getTarget());
        if (targsForTarg == null) {
          targsForTarg = new ArrayList();
          currMap.put(trg.getTarget(), targsForTarg);          
        } 
      }
      targsForTarg.add(trg);
    }
    return (spanMaps);
  }  

  /***************************************************************************
  **
  ** Write the display string
  **
  */
  
   String getSourceDisplayString(boolean footnotes) {
    StringBuffer buf = new StringBuffer();
    Iterator sit = getSources();
    while (sit.hasNext()) {
      Source src = (Source)sit.next();
      buf.append(src.getDisplayValue());
      if (footnotes) {
        String notes = src.getNotes();
        if (notes != null) {
          buf.append(" [");
          buf.append(notes);
          buf.append("]");
        }
      }
      if (sit.hasNext()) {
        buf.append(" + ");
      }
    }
    return (buf.toString());
  }  
  
  /***************************************************************************
  **
  ** Write the perturb to HTML
  **
  */
  
   void writeHTML(PrintWriter out, Indenter ind, QpcrTablePublisher qtp, NullTimeSpan defaultSpan) {
    ind.indent();    
    out.println("<tr valign=\"top\" >");    
    ind.up().indent();    
    out.println("<td>");
    ind.up().indent();
    Iterator srcs = getSources();   
    while (srcs.hasNext()) {
      Source src = (Source)srcs.next();
      src.writeHTML(out, ind, srcs.hasNext(), qtp, true);
    }
    ind.down().indent();    
    out.println("</td>");
    ind.indent();  
    out.println("<td>");
    ind.up().indent();
    qtp.paragraph(false);
    out.print("<i>");
    out.print(buildTargetDisplayString(defaultSpan));    
    out.println("</i>");
    String inv = buildInvestDisplayString();
    if (inv != null) {
      ResourceManager rMan = appState_.getRMan();
      out.print(" (");
      out.print(rMan.getString("qpcrData.dataOf"));
      out.print(" ");
      out.print(inv);
      out.print(")");
    }
    out.println("</p>");   
    ind.down().indent();   
    out.println("</td>");
    ind.down().indent();     
    out.println("</tr>");
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
   static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("nullPerturbation");
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
   public String toString() {
    return ("NullPerturb: sources = " + sources_ + " targets = " + targets_);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
   static NullPerturb buildFromXML(BTState appState, String elemName, 
                                   Attributes attrs) throws IOException {
    if (!elemName.equals("nullPerturbation")) {
      return (null);
    }
    
    //
    // This handles legacy operations:
    //
    
    String p = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("perturbation")) {
          p = CharacterEntityMapper.unmapEntities(val, false);
        }
      }
    }
    
    NullPerturb retval = new NullPerturb(appState);
    if (p != null) {
      retval.convertLegacySource(p);
    }
    
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Set the targets from a string.  Used for legacy input
  */
  
  private void convertNoteToTargets(String note, List targets, NullTimeSpan defaultNull) {
    
    //
    // Chop up the note string by commas, but keep the time span
    // lists (with commas) glued together:
    //
    ArrayList tokens = new ArrayList();
    Pattern plus = Pattern.compile(",");
    Pattern spanStart = Pattern.compile(".*\\([0-9]+$");
    Pattern spanEnd = Pattern.compile(".*[0-9]+ *h\\)]?$");
    Matcher m = plus.matcher(note);
    int pstart = 0;
    ArrayList currGlue = new ArrayList();
    while (m.find()) {
      String next = note.substring(pstart, m.start());
      addOrGlue(next, spanStart, spanEnd, currGlue, tokens);
      pstart = m.end();
    }
    String last = note.substring(pstart, note.length());
    addOrGlue(last, spanStart, spanEnd, currGlue, tokens);

    //
    // Get any leading default time string:
    //
    
    String def = extractDefault(tokens);
    int minTime = defaultNull.getMin();
       
    //
    // Group by brackets:
    //
    
    TimeTaggedList defaultToks = new TimeTaggedList();
    if (def == null) {
      defaultToks.isDefault = true;
    } else {
      defaultToks.taggedToken = def;
    }
    ArrayList bracketedToks = new ArrayList();
    groupByTimeTags(defaultToks, bracketedToks, tokens);

    //
    // Create Null targets from the tagged lists:
    //
   
    targets.addAll(convertTimeTaggedList(defaultToks, minTime));
    int brakNum = bracketedToks.size();    
    for (int i = 0; i < brakNum; i++) {
      TimeTaggedList bt = (TimeTaggedList)bracketedToks.get(i);
      targets.addAll(convertTimeTaggedList(bt, minTime));
    }    
    return;
  }

 /***************************************************************************
  **
  ** Convert time tagged list to NullTargets
  */
 
  private List convertTimeTaggedList(TimeTaggedList ttl, int minTime) {
    ArrayList retval = new ArrayList();
    List spans = parseTimeTag(ttl.isDefault, ttl.taggedToken, minTime);
    int size = ttl.tokens.size();
    for (int i = 0; i < size; i++) {
      String token = (String)ttl.tokens.get(i);
      String[] chopped = chopOutTimeSpan(token);
      List useSpans;
      if (chopped.length == 2) {
        useSpans = parseTimeTag(false, chopped[1], minTime);
      } else {
        useSpans = spans;
      }
      String[] choppedToo = chopOutRegion(chopped[0].trim());
      
      NullTarget newNull = (choppedToo.length == 2) ? new NullTarget(choppedToo[0].trim(), choppedToo[1].trim())
                                                    : new NullTarget(choppedToo[0].trim());
      if (useSpans != null) {
        int spSize = useSpans.size();
        for (int j = 0; j < spSize; j++) {
          NullTimeSpan span = (NullTimeSpan)useSpans.get(j);
          newNull.addTimeSpan(span);
        }
      }
      retval.add(newNull);
    }
    return (retval);
  }    

 /***************************************************************************
  **
  ** Chop or glue
  */
  
  private void addOrGlue(String next, Pattern spanStart, Pattern spanEnd, List glued, List tokens) {
    next = next.trim();
    Matcher mSS = spanStart.matcher(next);
    if (mSS.matches()) {
      glued.add(next);
    } else {
      if (!glued.isEmpty()) {
        Matcher mSE = spanEnd.matcher(next);
        glued.add(next);   
        if (mSE.matches()) {      
          tokens.add(flushGlueToString(glued));
          glued.clear();
        }
      } else {
        tokens.add(next);
      }
    }
    return;
  }  

  /***************************************************************************
  **
  ** Flush a glue list to a String
  */

  private String flushGlueToString(List glued) {
    StringBuffer buf = new StringBuffer();
    int size = glued.size();
    for (int i = 0; i < size; i++) {
      if (i != 0) {
        buf.append(", ");
      }
      buf.append((String)glued.get(i));
    }
    return (buf.toString());
  }

  /***************************************************************************
  **
  ** Handle bracketing
  */

  private void groupByTimeTags(TimeTaggedList defaultToks, List specialToks, List tokens) {
    Pattern brackStart = Pattern.compile("^\\[.*");
    Pattern brackEnd = Pattern.compile(".*]$");
    int tokNum = tokens.size();
    TimeTaggedList tokTarg = defaultToks;
    for (int i = 0; i < tokNum; i++) {
      String tok = (String)tokens.get(i);
      Matcher mBS = brackStart.matcher(tok);
      if (mBS.matches()) {
        tok = tok.replaceFirst("\\[", "");
        tokTarg = new TimeTaggedList();
      }
      Matcher mBE = brackEnd.matcher(tok);
      if (mBE.matches()) {
        tok = tok.substring(0, tok.length() - 1);
        String[] chopped = chopOutTimeSpan(tok);
        tokTarg.tokens.add(chopped[0]);
        if (chopped.length == 2) {
          tokTarg.taggedToken = chopped[1];
          specialToks.add(tokTarg);
        } else {
          defaultToks.tokens.addAll(tokTarg.tokens);
        }
        tokTarg = defaultToks;
      } else {
        tokTarg.tokens.add(tok);
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Handle default assignment
  */

  private String extractDefault(List tokens) {
    if (tokens.size() == 0) {
      return (null);
    }
    String tok = (String)tokens.get(0);
    Pattern defSpan = Pattern.compile("^(\\([0-9, -&]+h *\\):)");
    Matcher dSM = defSpan.matcher(tok);
    if (dSM.find()) {
      String grp = dSM.group(1);
      dSM.reset();
      tok = dSM.replaceFirst("");
      tokens.set(0, tok.trim());
      return (grp);
    }    
    return (null);
  }
  
  /***************************************************************************
  **
  ** Parse time tag
  */

  private List parseTimeTag(boolean isDefault, String tag, int minTime) {
    if (isDefault) {
      return (null);
    }
    boolean isList = (tag.indexOf(",") != -1);
    boolean isRange = (tag.indexOf("-") != -1);
    boolean isPair = (tag.indexOf("&") != -1);
    boolean toUpper = (tag.indexOf("(to") != -1);    
    boolean isSingle = !isList && !isRange && !isPair && !toUpper;
    
    ArrayList retval = new ArrayList();
    Pattern number = Pattern.compile("([0-9]+)");
    Matcher m = number.matcher(tag);
    int pstart = 0;
    int first = Integer.MIN_VALUE;
    while (m.find()) {
      String next = m.group(1);
      int val;
      try {
        val = Integer.parseInt(next);
      } catch (NumberFormatException nfex) {
        System.err.println("Could not parse " + next);
        throw new IllegalStateException();
      }
      if (isList || isSingle) {
        retval.add(new NullTimeSpan(appState_, val));
      } else if (toUpper) {
        retval.add(new NullTimeSpan(appState_, minTime, val));
      } else if (isPair) {
        retval.add(new NullTimeSpan(appState_, val));
      } else if (first != Integer.MIN_VALUE) {
        retval.add(new NullTimeSpan(appState_, first, val));
        first = Integer.MIN_VALUE;
      } else {
        first = val;
      }
      pstart = m.end();
    }
    return (retval);        
  }  

  /***************************************************************************
  **
  ** Get the time span out of a token
  */
  
  private String[] chopOutTimeSpan(String token) {
    String[] retval;
    Pattern parenSpan = Pattern.compile("(\\(.*[0-9] *h *\\))$");
    Matcher cP = parenSpan.matcher(token);
    if (cP.find()) {
      String grp = cP.group(1);
      cP.reset();
      token = cP.replaceFirst("");
      retval = new String[2];
      retval[0] = token.trim();
      retval[1] = grp;
    } else {
      retval = new String[1];
      retval[0] = token;
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the region out of a token
  */
  
  private String[] chopOutRegion(String token) {
    String[] retval;
    Pattern parenSpan = Pattern.compile("(\\(in *[0-9a-zA-Z']+ *\\))$");
    Matcher cP = parenSpan.matcher(token);
    if (cP.find()) {
      String grp = cP.group(1);
      cP.reset();
      token = cP.replaceFirst("");
      retval = new String[2];
      retval[0] = token.trim();
      Pattern regionPart = Pattern.compile("\\(in *([0-9a-zA-Z']+) *\\)");
      Matcher rP = regionPart.matcher(grp);
      if (rP.find()) {
        retval[1] = rP.group(1);      
      } else {
        throw new IllegalStateException();
      }
    } else {
      retval = new String[1];
      retval[0] = token;
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Set the source from a string.  Used for legacy input
  */
  
  private void convertLegacySource(String legacy) throws IOException {
    TimeAxisDefinition tad = appState_.getDB().getTimeAxisDefinition();
    if (tad.getUnits() != TimeAxisDefinition.LEGACY_UNITS) {
      throw new IOException();
    }
    Source src = new Source();
    src.setDisplayValue(legacy);
    addSource(src);
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Holds tagged tokens
  */  
      
  private class TimeTaggedList {
    
    boolean isDefault;
    String taggedToken;
    ArrayList tokens;
  
    TimeTaggedList() {
      isDefault = false;
      this.tokens = new ArrayList();
    }
    
     public String toString() {
      return (((isDefault) ? "Default: " : taggedToken + " : ") + tokens);
    }
  }   
}
