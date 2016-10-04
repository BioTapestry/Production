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

package org.systemsbiology.biotapestry.perturb;

import java.io.IOException;
import java.util.List;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UniqueLabeller;

/****************************************************************************
**
** This holds the list of perturbation annotations
*/

public class PertAnnotations implements Cloneable {

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

  private UniqueLabeller labels_;
  private TreeMap<String, String> messages_;
  private HashMap<String, String> mapToKeys_;
  private Pattern pattern_;
  private Matcher matcher_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public PertAnnotations() {
    labels_ = new UniqueLabeller();
    messages_ = new TreeMap<String, String>();
    mapToKeys_ = new HashMap<String, String>();
    pattern_ = Pattern.compile("[0-9][0-9]*");
    matcher_ = pattern_.matcher("");
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Merge annotations.
  */
  
  public void mergeAnnotations(List<String> joinKeys, String commonKey, String tag, String message) {    
    editMessage(commonKey, tag, message);
    int numIDs = joinKeys.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = joinKeys.get(i);
      if (!keyID.equals(commonKey)) {
        deleteMessage(keyID);
      }
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public PertAnnotations clone() {
    try {
      PertAnnotations retval = (PertAnnotations)super.clone();
      retval.labels_ = this.labels_.clone();
      retval.messages_ = new TreeMap<String, String>(this.messages_);
      retval.mapToKeys_ = new HashMap<String, String>(this.mapToKeys_);
      retval.pattern_ = Pattern.compile("[0-9][0-9]*");
      retval.matcher_ = retval.pattern_.matcher("");
      return (retval);  
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }   
  
  /***************************************************************************
  **
  ** Return the message for the ID
  */
  
  public String getMessage(String id) {
    return (messages_.get(id));
  }
  
  /***************************************************************************
  **
  ** Return the key for the ID
  */
  
  public String getTag(String id) {
    return (mapToKeys_.get(id));
  } 
  
  /***************************************************************************
  **
  ** Get the annotation count
  */
  
  public int getAnnotationCount() {
    return (mapToKeys_.size());
  } 
    
  /***************************************************************************
  **
  ** Answer if the mesage exists
  */
  
  public String messageExists(String message) {
    String normMessage = DataUtil.normKey(message);
    Iterator<String> mit = messages_.keySet().iterator();
    while (mit.hasNext()) {
      String key = mit.next();
      String chkMsg = messages_.get(key); 
      if (normMessage.equals(DataUtil.normKey(chkMsg))) {
        return (key);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Add a message (if unique), return the ID
  */
  
  public String addMessage(String message) {
    Iterator<String> mit = messages_.keySet().iterator();
    while (mit.hasNext()) {
      String key = mit.next();
      String chkMsg = messages_.get(key);
      if (chkMsg.equals(message)) {
        return (key);
      }
    }
    String nextLabel = labels_.getNextLabel();
    messages_.put(nextLabel, message);
    
    //
    // Provide unique numbers by default:
    //   
 
    TreeSet<Integer> numsOnly = new TreeSet<Integer>();
    Iterator<String> vit = mapToKeys_.values().iterator();
    while (vit.hasNext()) {
      String val = vit.next();
      matcher_.reset(val);
      if (matcher_.matches()) {
        numsOnly.add(Integer.valueOf(val));
      }
    }

    Integer lastNum = (numsOnly.isEmpty()) ? Integer.valueOf(0) : numsOnly.last();
    lastNum = Integer.valueOf(lastNum.intValue() + 1);    
    mapToKeys_.put(nextLabel, lastNum.toString());
    return (nextLabel);
  } 
  
  /***************************************************************************
  **
  ** Add a message, return the ID. Caller must insure we have uniqueness first!
  */
  
  public String addMessage(String tag, String message) { 
    String nextLabel = labels_.getNextLabel();
    messages_.put(nextLabel, message);
    mapToKeys_.put(nextLabel, tag);
    return (nextLabel);
  }
  
  /***************************************************************************
  **
  ** Use for undo
  */
  
  public void addMessageExistingKey(String useKey, String tag, String message) { 
    labels_.addExistingLabel(useKey);
    messages_.put(useKey, message);
    mapToKeys_.put(useKey, tag);
    return;
  }
  
  /***************************************************************************
  **
  ** Edit an existing annotation. Caller must insure we have uniqueness first!
  */
  
  public void editMessage(String id, String tag, String message) {
    messages_.put(id, message);
    mapToKeys_.put(id, tag);
    return;
  }
  
  /***************************************************************************
  **
  ** Delete an existing annotation.
  */
  
  public void deleteMessage(String id) {
    messages_.remove(id);
    mapToKeys_.remove(id);
    labels_.removeLabel(id);
    return;
  }
 
  /***************************************************************************
  **
  ** Add legacy messages
  */
  
  public String addLegacyMessage(String legKey, String message) {
   
    Iterator<String> kit = mapToKeys_.keySet().iterator();
    while (kit.hasNext()) {
      String id = kit.next();
      String existing = mapToKeys_.get(id);
      if (existing.equals(legKey)) {
        String exMsg = messages_.get(id);
        if (!exMsg.equals(message)) {
          return (null);
        } else {
          return (id);
        }
      }
    }
    
    String nextLabel = labels_.getNextLabel();
    messages_.put(nextLabel, message);
    mapToKeys_.put(nextLabel, legKey);
    return (nextLabel);
  }
  
  /***************************************************************************
  **
  ** Get a sorted, comma separated list of footnote keys for the note IDs:
  */
  
  public String getFootnoteListAsString(List<String> noteIDs) {   
    List<String> flist = getFootnoteList(noteIDs); 
    return (convertFootnoteListToString(flist));
  }
  
  /***************************************************************************
  **
  ** Get a string of all key=messages
  */
  
  public String getFootnoteListAsNVString(List<String> noteIDs) {
    if (noteIDs.isEmpty()) {
      return ("");
    }    
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    List<String> flist = getFootnoteList(noteIDs); 
    Map<String, String> n2k = getFootTagToKeyMap();
    Iterator<String> flit = flist.iterator();
    while (flit.hasNext()) {
      String tag = flit.next();
      String key = n2k.get(tag);
      String message = getMessage(key);
      buf.append(tag);
      buf.append("=");
      buf.append(message);
      if (flit.hasNext()) {
        buf.append("; ");
      }
    }
    buf.append("]");
    return (buf.toString());
  } 
  
  /***************************************************************************
  **
  ** Get a sorted, list of footnote keys for the note IDs:
  */
  
  public List<String> getFootnoteList(List<String> noteIDs) {
    TreeSet<String> sortedKeys = new TreeSet<String>(new ReadOnlyTable.NumStrComparator());
    int nidNum = noteIDs.size();
    for (int i = 0; i < nidNum; i++) {
      String noteID = noteIDs.get(i);
      sortedKeys.add(mapToKeys_.get(noteID));
    }
    return (new ArrayList<String>(sortedKeys));
  }

  /***************************************************************************
  **
  ** Answer if we have anything
  */
  
  public boolean haveMessages() {
    return (!messages_.isEmpty());
  }
 
  /***************************************************************************
  **
  ** Get all the annotations, with a map from number to message:
  */
  
  public SortedMap<String, String> getFullMap() {
    TreeMap<String, String> retval = new TreeMap<String, String>(new ReadOnlyTable.NumStrComparator());
    Iterator<String> mtnkit = mapToKeys_.keySet().iterator();
    while (mtnkit.hasNext()) {
      String id = mtnkit.next();
      String key = mapToKeys_.get(id);
      String message = messages_.get(id);
      retval.put(key, message);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a map from footnoteID to key:
  */
  
  public SortedMap<String, String> getFootTagToKeyMap() {
    TreeMap<String, String> retval = new TreeMap<String, String>();
    Iterator<String> mtnkit = mapToKeys_.keySet().iterator();
    while (mtnkit.hasNext()) {
      String id = mtnkit.next();
      String tag = mapToKeys_.get(id);
      retval.put(tag, id);
    }
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Begin adding for IO:
  */
  
  public void beginAddAnnotForIO(Annot an) {
    labels_.addExistingLabel(an.id);
    mapToKeys_.put(an.id, an.tag);  
    messages_.put(an.id, "Waiting for Godot...");
  }
  
  /***************************************************************************
  **
  ** Finish adding for IO:
  */
  
  public void finishAddAnnotForIO(Annot an) { 
    messages_.put(an.id, an.message);
  }

  /***************************************************************************
  **
  ** Write the annotations to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.println("<pertAnnotations>"); 
    ind.up();
    Iterator<String> mkit = messages_.keySet().iterator();
    while (mkit.hasNext()) {
      String id = mkit.next();
      String message = messages_.get(id);
      String key = mapToKeys_.get(id);
      ind.indent();
      out.print("<pAnnot id=\"");
      out.print(id);
      out.print("\" key=\"");
      out.print(CharacterEntityMapper.mapEntities(key, false));
      out.print("\">");
      if (message != null) {
        out.print(CharacterEntityMapper.mapEntities(message, false));
      }      
      out.println("</pAnnot>");
    }
    ind.down().indent();
    out.println("</pertAnnotations>");   
    return;
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("PertAnnotations");
  }
 
  /***************************************************************************
  **
  ** Get the choices for annotations
  */
  
  public Vector getAnnotationOptions() {
    Vector retval = new Vector();
    StringBuffer buf = new StringBuffer();
    SortedMap<String, String> fullMap = getFootTagToKeyMap();
    Iterator<String> oit = fullMap.keySet().iterator();
    while (oit.hasNext()) {
      String tag = oit.next();
      String key = fullMap.get(tag);
      String message = getMessage(key);
      buf.setLength(0);
      buf.append(tag);
      buf.append("=");
      buf.append(message);
      retval.add(new TrueObjChoiceContent(buf.toString(), key));
    }
    return (retval);
  }
   
  /***************************************************************************
  **
  ** Get unused tag choices
  */
  
  public Vector getAvailableTagChoices() {
    UniqueLabeller ul = new UniqueLabeller();
    Iterator<String> kit = mapToKeys_.values().iterator();
    while (kit.hasNext()) {
      String tag = kit.next();
      ul.addExistingLabel(tag);
    }
    Vector retval = new Vector();
    for (int i = 0; i < 20; i++) {
      String newKey = ul.getNextLabel();
      retval.add(new TrueObjChoiceContent(newKey, newKey));   
    }  
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a temporary tag
  */
  
  public String getNextTempTag(Set<String> otherTempTags) {
    UniqueLabeller ul = new UniqueLabeller();
    Iterator<String> kit = mapToKeys_.values().iterator();
    while (kit.hasNext()) {
      String tag = kit.next();
      ul.addExistingLabel(tag);
    }
    Iterator<String> oit = otherTempTags.iterator();
    while (oit.hasNext()) {
      String tag = oit.next();
      ul.addExistingLabel(tag);
    }    
    return (ul.getNextLabel());
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
    
  /***************************************************************************
  **
  ** Get a sorted, comma separated list of footnote keys for the note IDs:
  */
  
  public static String convertFootnoteListToString(List<String> flist) {
    StringBuffer buf = new StringBuffer();
    Iterator<String> snit = flist.iterator();
    while (snit.hasNext()) {
      String noteNum = snit.next();
      buf.append(noteNum);
      if (snit.hasNext()) {
        buf.append(",");
      }
    }
    return (buf.toString());
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** For XML I/O
  ** 
  */ 
      
  public static class PertAnnotationsWorker extends AbstractFactoryClient {
    
    public PertAnnotationsWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("pertAnnotations");
      installWorker(new AnnotWorker(whiteboard), new MyAnnotGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("pertAnnotations")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.pa = buildFromXML(elemName, attrs);
        retval = board.pa;
      }
      return (retval);     
    }  
     
    @SuppressWarnings("unused")
    private PertAnnotations buildFromXML(String elemName, Attributes attrs) throws IOException {
      PertAnnotations pa = new PertAnnotations();
      return (pa);
    } 
  }
  
  public static class MyAnnotGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      PertAnnotations pa = board.pa;
      Annot annot = board.annot;
      pa.beginAddAnnotForIO(annot);
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
     
  public static class Annot {
    public String id;
    public String tag;
    public String message;
    
    public Annot(String id, String tag, String message) {
      this.id = id;
      this.tag = tag;
      this.message = message;
    }
  }

  public static class AnnotWorker extends AbstractFactoryClient {
    
    private StringBuffer buf_;
    private Annot currAnnot_;
 
    public AnnotWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      buf_ = new StringBuffer();
      myKeys_.add("pAnnot");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("pAnnot")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.annot = buildFromXML(elemName, attrs);
        retval = board.annot;
      }
      return (retval);     
    }
  
    protected void localFinishElement(String elemName) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
      currAnnot_.message = CharacterEntityMapper.unmapEntities(buf_.toString(), false);
      PertAnnotations pa = board.pa;
      pa.finishAddAnnotForIO(currAnnot_);    
      return;
    }
 
    protected void localProcessCharacters(char[] chars, int start, int length) {
      String nextString = new String(chars, start, length);
      buf_.append(nextString);
      return;
    }  
    
    private Annot buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "pAnnot", "id", true);     
      String key = AttributeExtractor.extractAttribute(elemName, attrs, "pAnnot", "key", true);
      key = CharacterEntityMapper.unmapEntities(key, false);
      buf_.setLength(0);
      currAnnot_ = new Annot(id, key, null);
      return (currAnnot_);
    }
  }
}
