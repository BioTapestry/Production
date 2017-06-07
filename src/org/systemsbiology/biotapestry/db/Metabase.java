/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.NewerVersionIOException;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.CopiesPerEmbryoData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NamedColor;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UniqueLabeller;

import org.xml.sax.Attributes;


/****************************************************************************
**
** Database of Databases
*/

public class Metabase implements ColorResolver {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private static final String DEFAULT_VERSION_       = "1.0"; 
  private static final String PREVIOUS_IO_VERSION_A_ = "2.0"; 
  private static final String PREVIOUS_IO_VERSION_B_ = "2.1"; // Actually for version 3.0 too....
  private static final String PREVIOUS_IO_VERSION_C_ = "3.1"; // Actually for version 4.0 too....
  private static final String PREVIOUS_IO_VERSION_D_ = "5.0"; // Used for 5, 6, 7.0
  private static final String PREVIOUS_IO_VERSION_E_ = "7.1"; // Used for 7.0.1
  
  private static final String CURRENT_IO_VERSION_    = "8.0";
  
  private static final String TAB_ID_PREFIX_ = "DB_";

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private TreeMap<String, Database> perTab_;
  private BTState appState_;
  private ColorGenerator colGen_;
  private UniqueLabeller labels_;
  private MetabaseSharedCore sharedStash_;
  private MetabaseSharedCore shared_;
  private String iOVersion_;
  private UIComponentSource uics_;
  private DynamicDataAccessContext ddacx_;
  private TabSource tSrc_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Not-null constructor
  */

  public Metabase(BTState appState, UIComponentSource uics, TabSource tSrc) {
    appState_ = appState;
    perTab_ = new TreeMap<String, Database>();
    labels_ = new UniqueLabeller();
    labels_.setFixedPrefix(TAB_ID_PREFIX_);
    colGen_ = new ColorGenerator();
    shared_ = new MetabaseSharedCore();
    iOVersion_ = CURRENT_IO_VERSION_;
    uics_ = uics;
    ddacx_ = null;
    tSrc_ = tSrc;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  ** 
  ** We have a circular reference....
  */

  public void setDDacx(DynamicDataAccessContext ddacx) {
    ddacx_ = ddacx;
    return;
  }
 
  /***************************************************************************
  ** 
  ** HACKETY HACK Used to stock a Dynamic DACX from a static DACX
  */

  public BTState getAppState() {
    return (appState_);
  }
  
  /***************************************************************************
  ** 
  ** Prepare Metabase to receive parallel shared data that must be merged
  */

  public void setForTabAppend() {
    sharedStash_ = shared_;
    shared_ = new MetabaseSharedCore();
    return;
  }
  
  /***************************************************************************
  ** 
  ** Merge shared data
  */

  public void doTabAppendPostMerge() {
    System.out.println("DANGER! Tossing unmerged data");
    /*
        if (forAppend_ && dataKeys_.contains(elemName)) {
      if (forAppend_ && (currTarg_ != null)) {
        UiUtil.fixMePrintout("Where is this coming from for append?");
        TimeCourseData existing = appState_.getDB().getTimeCourseData();        
        if (!existing.hasGeneTemplate() || existing.templatesMatch(currTarg_)) {
          Set<String> failMerges = existing.mergeData(currTarg_);
          if (failMerges != null) {
            if (sharedBoard_.mergeIssues == null) {
              sharedBoard_.mergeIssues = new ArrayList<String>();
            }
            String fmt = appState_.getRMan().getString("tabMerge.timeCourseGeneConflictFmt");
            for (String failMerge : failMerges) {
              String desc = MessageFormat.format(fmt, new Object[] {failMerge});
              sharedBoard_.mergeIssues.add(desc);
            }
          }
        } else {
          if (sharedBoard_.mergeIssues == null) {
            sharedBoard_.mergeIssues = new ArrayList<String>();
          }
          sharedBoard_.mergeIssues.add(appState_.getRMan().getString("tabMerge.fullTimeCourseTemplateFailure"));
        }
      }  
    
    */
    /*
        if (isForAppend_) {
      TimeAxisDefinition existing = appState_.getMetabase().getSharedTimeAxisDefinition();
      boolean existingInit = existing.isInitialized();
      boolean ctInit = ((currTarg_ != null) && currTarg_.isInitialized());
      if (existingInit) {
        if (ctInit && !existing.equals(currTarg_)) {
          FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
          if (board.mergeIssues == null) {
            board.mergeIssues = new ArrayList<String>();
          }
          board.mergeIssues.add(appState_.getRMan().getString("tabMerge.failedTimeAxisMatch"));
          board.failedTimeAxisMerge = true;
        }    
      } else if (ctInit) {
        appState_.getMetabase().setSharedTimeAxisDefinition(currTarg_);
      }
    }
    */
    
    shared_ = sharedStash_;
    return;
  }

  /***************************************************************************
  ** 
  ** Get Database ID
  */

  public String getNextDbID() {
    return (labels_.getNextLabel());
  }
  
  /***************************************************************************
  ** 
  ** Add a Database
  */

  public void addDB(int tabNum, String dbID) {
    perTab_.put(dbID, new Database(dbID, tabNum, uics_, this, ddacx_.getTabContext(dbID)));
    return;
  }
  
  /***************************************************************************
  ** 
  ** Add a Database for IO
  */

  public String loadDB(int tabNum, String dbID) {
    if (dbID == null) {
      dbID = getNextDbID();
    } else if (!labels_.addExistingLabel(dbID)) {
      throw new IllegalStateException();
    }
    perTab_.put(dbID, new Database(dbID, tabNum, uics_, this, ddacx_.getTabContext(dbID)));
    return (dbID);
  }
  
  /***************************************************************************
  ** 
  ** Add a Database for IO
  */

  public String loadDBExistingLabel(int tabNum, String dbID) {
    perTab_.put(dbID, new Database(dbID, tabNum, uics_, this, ddacx_.getTabContext(dbID)));
    return (dbID);
  }

  /***************************************************************************
  ** 
  ** Get DB for a legacy load with no ID.
  */

  public String legacyDBID() {
    return (perTab_.keySet().iterator().next());
  }
  
  /***************************************************************************
  ** 
  ** Reset DB info on load
  */

  public void resetZeroDB(int tabNum, String dbID) {
    if ((perTab_.size() != 1) || (tabNum != 0)) {
      throw new IllegalArgumentException(); 
    }
    String currDBID = perTab_.keySet().iterator().next();
    String defName = ddacx_.getRMan().getString("database.defaultModelName");
    TabNameData tabData = new TabNameData(defName, defName, defName);
    
    if (currDBID.equals(dbID)) {
      Database currDB = perTab_.get(currDBID);
      currDB.setTabNameData(tabData);
      return;
    }
    labels_.removeLabel(currDBID);
    Database currDB = perTab_.remove(currDBID);
    currDB.reset(dbID, tabNum);
    currDB.setTabNameData(tabData);
    if (!labels_.addExistingLabel(dbID)) {
      throw new IllegalStateException();
    }
    perTab_.put(dbID, currDB);
    tSrc_.clearCurrentTabID(dbID);
    return;
  }

  /***************************************************************************
  ** 
  ** Restore Database for undo
  */

  public void restoreDB(Database db) {
    if (!labels_.addExistingLabel(db.getID())) {
      throw new IllegalStateException();
    }
    perTab_.put(db.getID(), db);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Delete a Database
  */

  public Database removeDB(String dbID) {
    Database retval = perTab_.remove(dbID);
    labels_.removeLabel(dbID);
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Get Database count
  */

  public int getNumDB() {
    return (perTab_.size());
  }  

  /***************************************************************************
  ** 
  ** Get the database IDs
  */

  public Iterator<String> getDBIDs() {
    return (perTab_.keySet().iterator());
  }
  
  /***************************************************************************
  ** 
  ** Get a Database
  */

  public Database getDB(String id) {
    return (perTab_.get(id));
  }
 
  /***************************************************************************
  ** 
  ** Start a new model
  */

  public void newModelViaDACX() {
    dropViaDACX();
    //
    // Default behavior with one tab is that this data is not shared, but local to the database.
    // Note all these fields were nulled in the drop call anyway:
    //
    //pertData_ = new PerturbationData(appState_);
    //sharedTimeCourse_ = null; //new TimeCourseData(appState_);
    //copiesPerEmb_ = new CopiesPerEmbryoData(appState_);
    //timeAxis_ = new TimeAxisDefinition(appState_);
    
    //
    // We do NOT clear out display options, but we do need to drop
    // defs that depend on the time def, so the time axis definition 
    // is not frozen for a new model:
    //

    ddacx_.getDisplayOptsSource().getDisplayOptions().dropDataBasedOptions();
    iOVersion_ = CURRENT_IO_VERSION_;
    return;
  }  

  /***************************************************************************
  ** 
  ** Drop everything
  */

  public void dropViaDACX() {
    labels_ = new UniqueLabeller();
    labels_.setFixedPrefix(TAB_ID_PREFIX_);
    
    
    //
    // This little bit gets us the first existing database and turns it into the 
    // only existing database. Also calls appState to get it on the same page as
    // to that one database being the only tab:
    //
    
    Database db = perTab_.values().iterator().next();
    perTab_.clear();
    String newID = labels_.getNextLabel();
    db.setID(newID);
    db.setIndex(0);
    perTab_.put(newID, db);
    tSrc_.clearCurrentTabID(newID);
    
    //
    // Default behavior with one tab is that this data is not shared, but local to the database:
    //
    
    shared_.dropViaDACX();
  
    //
    // We do NOT clear out display options, but we do need to drop
    // defs that depend on the time def, so the time axis definition 
    // is not frozen for a new model:
    //

    ddacx_.getDisplayOptsSource().getDisplayOptions().dropDataBasedOptions();
    iOVersion_ = CURRENT_IO_VERSION_;
    
    colGen_.dropColors();
    ddacx_.getFontManager().resetToDefaults();
    uics_.getImageMgr().dropAllImages();

    return;
  }
  
  /***************************************************************************
  ** 
  ** This gets called following file input to do any needed legacy fixups.
  */

  public void legacyIOFixup(JFrame topWindow, int preTabCount, int postTabCount) {
    
    ddacx_.getFontManager().fixupLegacyIO();  // Need this starting version 3.1
    ResourceManager rMan = ddacx_.getRMan();
    String message = rMan.getString("legacyIOFixup.baseChange");
    String msgDiv;
    if (topWindow != null) {
      message = "<html><center>" + message;
      msgDiv = "<br><br>";
    } else {
      msgDiv = " ";
    }
    boolean doit = false;
    UiUtil.fixMePrintout("NOT working for multi-tabs (or has it been fixed??");    
    
    for (int i = preTabCount; i < postTabCount; i++) {
      String chosen = null;
      for (String ptk : perTab_.keySet()) {
        int which = tSrc_.getTabIndexFromId(ptk);
        if (which == i) {
          chosen = ptk;
          break;
        }
      }
      
      if (iOVersion_.equals(DEFAULT_VERSION_)) {
        Database legDb = perTab_.get(chosen);
        List<String> change = legDb.legacyIOFixupForHourBounds();
        if ((change != null) && !change.isEmpty()) {
          int size = change.size();
          String desc;
          if (size == 1) {
            String form = rMan.getString("legacyIOFixup.singleHourBoundsChange");                             
            desc = MessageFormat.format(form, new Object[] {change.get(0)});
          } else {
            String form = rMan.getString("legacyIOFixup.multiHourBoundsChange");
            desc = MessageFormat.format(form, new Object[] {change.get(0), new Integer(size - 1)});                    
          }
          message = message + msgDiv + desc;        
          doit = true;
        }
        
        List<String> nodeCh = legDb.legacyIOFixupForNodeActivities();
        if ((nodeCh != null) && !nodeCh.isEmpty()) {
          int size = nodeCh.size();
          String desc;
          if (size == 1) {
            String form = rMan.getString("legacyIOFixup.singleNodeActivityChange");                             
            desc = MessageFormat.format(form, new Object[] {nodeCh.get(0)});
          } else {
            String form = rMan.getString("legacyIOFixup.multiNodeActivityChange");
            desc = MessageFormat.format(form, new Object[] {nodeCh.get(0), new Integer(size - 1)});                    
          }
          message = message + msgDiv + desc;        
          doit = true;
        } 
      }
      
      if (iOVersion_.equals(DEFAULT_VERSION_) || iOVersion_.equals(PREVIOUS_IO_VERSION_A_)) {
        Database legDb = perTab_.get(chosen);
        legDb.legacyIOFixupForGroupOrdering();
      }
     
      //
      // Slash Node pads have been overhauled in Version 7.0.1. 
      //
      
      if (iOVersion_.equals(DEFAULT_VERSION_) || 
          iOVersion_.equals(PREVIOUS_IO_VERSION_A_) || 
          iOVersion_.equals(PREVIOUS_IO_VERSION_B_) || 
          iOVersion_.equals(PREVIOUS_IO_VERSION_C_) || 
          iOVersion_.equals(PREVIOUS_IO_VERSION_D_)) {
        Database legDb = perTab_.get(chosen);
        PadCalculatorToo pcalc = new PadCalculatorToo();
        pcalc.legacyIOFixupForSlashNodes(legDb, legDb);
        String newMessage = legDb.legacyIOFixupForGeneLengthAndModules(message, rMan, msgDiv, ddacx_);
        if (newMessage != null) {
          message = newMessage;
          doit = true;
        }
      }
         
      //
      // Turns out V3.0 had potential errors with pad assignments in root when
      // drawing bottom-up.  For kicks, let's _always_ look out for these problems:
      //
     
      Database legDbc = perTab_.get(chosen);
      PadCalculatorToo pcalc = new PadCalculatorToo();   
      List<PadCalculatorToo.IOFixup> padErrors = pcalc.checkForPadErrors(legDbc, legDbc);
      if (!padErrors.isEmpty()) {
        pcalc.fixIOPadErrors(legDbc, padErrors);
        message = message + msgDiv + rMan.getString("legacyIOFixup.padErrors");
        doit = true;
      }
      
      List<String> srcErrors = pcalc.checkForGeneSrcPadErrors(legDbc);
      if (!srcErrors.isEmpty()) {
        message = message + msgDiv + rMan.getString("legacyIOFixup.srcPadErrors");
        doit = true;
      }
      
      //
      // Note properties got orphaned in old versions:
      //
      
      if (iOVersion_.equals(DEFAULT_VERSION_) || iOVersion_.equals(PREVIOUS_IO_VERSION_A_) || iOVersion_.equals(PREVIOUS_IO_VERSION_B_)) {
        Database legDb = perTab_.get(chosen);
        List<String> noteOrphs = legDb.legacyIOFixupForOrphanedNotes();
        int numOrphs = noteOrphs.size();
        if (numOrphs > 0) {
          String form = rMan.getString("legacyIOFixup.orphanNoteFixup");                             
          message = message + msgDiv + form;        
          doit = true;
        }
      }    
  
      //
      // Prior to 3.1, need to fixup line breaks on nodes
      //
  
      if (iOVersion_.equals(DEFAULT_VERSION_) || iOVersion_.equals(PREVIOUS_IO_VERSION_A_) || iOVersion_.equals(PREVIOUS_IO_VERSION_B_)) {
        Database legDb = perTab_.get(chosen);
        List<String> lineBrks = legDb.legacyIOFixupForLineBreaks(ddacx_.getFontManager());
        if ((lineBrks != null) && !lineBrks.isEmpty()) {
          int size = lineBrks.size();
          String desc;
          if (size == 1) {
            String form = rMan.getString("legacyIOFixup.singleLineBreakChange");                             
            desc = MessageFormat.format(form, new Object[] {lineBrks.get(0)});
          } else {
            String form = rMan.getString("legacyIOFixup.multiLineBreakChange");
            desc = MessageFormat.format(form, new Object[] {lineBrks.get(0), new Integer(size - 1)});                    
          }
          message = message + msgDiv + desc;        
          doit = true;
        } 
      }     
      
      //
      // QPCR is no longer used for storage:
      //
      
      if (iOVersion_.equals(DEFAULT_VERSION_) || 
          iOVersion_.equals(PREVIOUS_IO_VERSION_A_) || 
          iOVersion_.equals(PREVIOUS_IO_VERSION_B_) || 
          iOVersion_.equals(PREVIOUS_IO_VERSION_C_)) {
        Database legDb = perTab_.get(chosen);
        legDb.getPertData().transferFromLegacy();
      }    
    }
 
    //
    // Finish assembling the message:
    //

    if (doit) {
      message = message + msgDiv + rMan.getString("legacyIOFixup.changeSuffix");
      if (topWindow != null) {
        message = message + "</center></html>";        
        JOptionPane.showMessageDialog(topWindow, message,
                                      rMan.getString("legacyIOFixup.dialogTitle"),
                                      JOptionPane.WARNING_MESSAGE); 
      } else {
        System.err.println(message);
      }
    }

    return;
  }

  /***************************************************************************
  ** 
  ** Drop all shared perturbation data
  */

  public DatabaseChange dropAllSharedData() {
    return (shared_.dropAllSharedData());
  }

  /***************************************************************************
  ** 
  ** Get the perturbation data
  */

  public PerturbationData getSharedPertData() {
    return (shared_.getSharedPertData());
  }  
  
  /***************************************************************************
  ** 
  ** Set the perturbation data
  */

  public DatabaseChange setSharedPertData(PerturbationData pertData) {
    return (shared_.setSharedPertData(pertData));
  }
 
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startSharedTimeCourseUndoTransaction() {
    return (shared_.startSharedTimeCourseUndoTransaction());
  }  
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishSharedTimeCourseUndoTransaction(DatabaseChange change) {
    return (shared_.finishSharedTimeCourseUndoTransaction(change));
  }
  
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startSharedCopiesPerEmbryoUndoTransaction() {
    return (shared_.startSharedCopiesPerEmbryoUndoTransaction());
  }  
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishSharedCopiesPerEmbryoUndoTransaction(DatabaseChange change) {
    return (shared_.finishSharedCopiesPerEmbryoUndoTransaction(change));
  }
  
  /***************************************************************************
  ** 
  ** Get the data sharing policy
  */

  public DataSharingPolicy getDataSharingPolicy() {
    return (shared_.getDataSharingPolicy());
  } 
  
  /***************************************************************************
  ** 
  ** Set the data sharing policy: For I/O (no undo support)
  */

  public void installDataSharing(DataSharingPolicy dsp) {
    shared_.installDataSharing(dsp);
    return;
  } 

  /***************************************************************************
  ** 
  ** Is data sharing in effect?
  */

  public boolean amSharingExperimentalData() {
    return (shared_.amSharingExperimentalData());
  } 
  
  /***************************************************************************
  ** 
  ** What dbs are sharing?
  */

  public Set<String> tabsSharingData() {
    HashSet<String> retval = new HashSet<String>();
    for (String dbKey : perTab_.keySet()) {
      Database db = perTab_.get(dbKey);
      if (db.amUsingSharedExperimentalData()) {
        retval.add(dbKey);
      }
    }
    if (getDataSharingPolicy().isSpecifyingSharing() != !retval.isEmpty()) {
      throw new IllegalStateException();
    }
    return (retval);
  } 
  
  /***************************************************************************
  ** 
  ** Get the Time course data
  */

  public TimeCourseData getSharedTimeCourseData() {
    return (shared_.getSharedTimeCourseData());
  }  
  
  /***************************************************************************
  ** 
  ** Set the Time Course data
  */

  public DatabaseChange setSharedTimeCourseData(TimeCourseData timeCourse) {
    return (shared_.setSharedTimeCourseData(timeCourse));
  }  
  
  /***************************************************************************
  ** 
  ** Get the shared copies data
  */

  public CopiesPerEmbryoData getSharedCopiesPerEmbryoData() {
    return (shared_.getSharedCopiesPerEmbryoData());
  }  
  
  /***************************************************************************
  ** 
  ** Set the copies per embryo data
  */

  public DatabaseChange setSharedCopiesPerEmbryoData(CopiesPerEmbryoData copies) {
    return (shared_.setSharedCopiesPerEmbryoData(copies));
  }    
    
  /***************************************************************************
  ** 
  ** Get the time axis definition
  */

  public TimeAxisDefinition getSharedTimeAxisDefinition() {
    return (shared_.getSharedTimeAxisDefinition());
  }  
  
  /***************************************************************************
  ** 
  ** Set the time axis definition
  */

  public DatabaseChange setSharedTimeAxisDefinition(TimeAxisDefinition timeAxis) {
    return (shared_.setSharedTimeAxisDefinition(timeAxis));
  }
  
  /***************************************************************************
  ** 
  ** Get the ColorGenerator
  */

  public ColorGenerator getColorGenerator() {
    return (colGen_);
  }
  
  /***************************************************************************
  ** 
  ** Get the color
  */

  public Color getColor(String colorKey) {
    return (colGen_.getColor(colorKey));
  }

  /***************************************************************************
  ** 
  ** Get the named color
  */

  public NamedColor getNamedColor(String colorKey) {
    return (colGen_.getNamedColor(colorKey));
  }  

  /***************************************************************************
  **
  ** Update the color set
  */
  
  public GlobalChange updateColors(Map<String, NamedColor> namedColors) {
    return (colGen_.updateColors(namedColors));
  }
  
  
  /***************************************************************************
  **
  ** Dump the database to the given file using XML
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {    
    ind.indent();
    out.print("<BioTapestry version=\"");
    out.print(CURRENT_IO_VERSION_);
    out.println("\" >");
    shared_.writeTimeAxisXML(out, ind);
    ddacx_.getFontManager().writeXML(out, ind.up());
    ind.down();
    ddacx_.getDisplayOptsSource().getDisplayOptions().writeXML(out, ind.up());
    ind.down();
    
    colGen_.writeXML(out, ind.up());
    ind.down();
    
    uics_.getImageMgr().writeXML(out, ind.up());
    ind.down();
      
    shared_.writeXML(out, ind);
 
    ind.up().indent();
    out.println("<Databases>");
    String currTab = tSrc_.getCurrentTab();
    // Output in tab order:
    Iterator<String> kit = perTab_.keySet().iterator();
    TreeMap<Integer, String> tabOrdered = new TreeMap<Integer, String>();
    while (kit.hasNext()) {
      Database db = perTab_.get(kit.next());
      tabOrdered.put(Integer.valueOf(db.getIndex()), db.getID());
    }
    Iterator<String> kit2 = tabOrdered.values().iterator();
    while (kit2.hasNext()) {
      Database db = perTab_.get(kit2.next());
      tSrc_.setCurrentTabIndex(db.getIndex());
      db.writeXML(out, ind.up());
      ind.down();
    }
    tSrc_.setCurrentTabID(currTab);
    ind.indent();
    out.println("</Databases>");
    
    ind.down().indent();
    out.println("</BioTapestry>");
    return;
  }  

  /***************************************************************************
  **
  ** Get the next color label
  */
  
  public String getNextColorLabel() {
    return (colGen_.getNextColorLabel()); 
  }  
  
  /***************************************************************************
  **
  ** Set the color for the given name
  */
  
  public void setColor(String itemId, NamedColor color) {
    colGen_.setColor(itemId, color);
    return;
  }
  
  /***************************************************************************
  **
  ** Return an iterator over all the color keys
  */
  
  public Iterator<String> getColorKeys() {
    return (colGen_.getColorKeys());
  }

  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(GlobalChange undo) {
    colGen_.changeUndo(undo);
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(GlobalChange undo) {
    colGen_.changeRedo(undo);
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void metabaseSharedChangeUndo(DatabaseChange undo) {
    shared_.metabaseSharedChangeUndo(undo);
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void metabaseSharedChangeRedo(DatabaseChange redo) {
    shared_.metabaseSharedChangeRedo(redo);
    return;
  }
  
  /***************************************************************************
  **
  ** Return the colors you cannot delete
  */
  
  public Set<String> cannotDeleteColors() {
    return (colGen_.cannotDeleteColors());
  }  

  /***************************************************************************
  **
  ** Return from a cycle of active colors
  */
  
  public String activeColorCycle(int i) {
    return (colGen_.activeColorCycle(i));
  }
  
  /***************************************************************************
  **
  ** A distinct active color
  */
  
  public String distinctActiveColor() { 
    return (colGen_.distinctActiveColor());
  }
  
  /***************************************************************************
  **
  ** Return a distinct inactive color
  */
  
  public String distinctInactiveColor() {  
    return (colGen_.distinctInactiveColor());
  }
   
  /***************************************************************************
  **
  ** Return from a cycle of inactive colors
  */
  
  public String inactiveColorCycle(int i) {
    return (colGen_.inactiveColorCycle(i));
  }
  
  /***************************************************************************
  **
  ** Return from a cycle of gene colors
  */
  
  public String getNextColor(GenomeSource gs, LayoutSource ls) {
    
    List<String> geneColors = colGen_.getGeneColorsAsList();
    HashMap<String, Integer> colorCounts = new HashMap<String, Integer>();
    int gcSize = geneColors.size();
    for (int i = 0; i < gcSize; i++) {
      String col = geneColors.get(i);
      colorCounts.put(col, new Integer(0));
    }
   
    //
    // Crank thru all the genes and get their colors.  Figure out the set of colors
    // with the minimum count.  Choose a color from that set.
    //
    
 
    Genome root = gs.getRootDBGenome();
    String loKey = ddacx_.getLayoutSource().mapGenomeKeyToLayoutKey(root.getID());
    Layout lo = ls.getLayout(loKey);    
    Iterator<Gene> git = root.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      NodeProperties np = lo.getNodeProperties(gene.getID());
      String col = np.getColorName();
      if (geneColors.contains(col)) {
        Integer count = colorCounts.get(col);
        colorCounts.put(col, new Integer(count.intValue() + 1));
      }
    }
    Iterator<Node> nit = root.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      if (NodeProperties.setWithLinkColor(node.getNodeType())) {
        NodeProperties srcProp = lo.getNodeProperties(node.getID());
        // FIX ME: Abstract this away!
        String col = "black";
        if (node.getNodeType() == Node.INTERCELL) {
          col = srcProp.getSecondColorName();
          if (col == null) {
            col = srcProp.getColorName();
          }
        } else {
          col = srcProp.getColorName();
        }
        if (geneColors.contains(col)) {
          Integer count = colorCounts.get(col);
          colorCounts.put(col, new Integer(count.intValue() + 1));
        }
      }
    }

    //
    // Minimum count colors:
    //
    
    int minVal = Integer.MAX_VALUE;
    HashSet<String> minSet = new HashSet<String>();
    Iterator<String> cckit = colorCounts.keySet().iterator();
    while (cckit.hasNext()) {
      String col = cckit.next();
      int count = colorCounts.get(col).intValue();
      if (count < minVal) {
        minSet.clear();
        minVal = count;
        minSet.add(col);
      } else if (count == minVal) {
        minSet.add(col);
      }
    }
 
    //
    // get a color from minimum set in the predefined order:
    //
    for (int i = 0; i < gcSize; i++) {
      String retval = geneColors.get(i);
      if (minSet.contains(retval)) {
        return (retval);
      }
    }
    
    return ("black");
  }
  
  /***************************************************************************
  **
  ** Return the least used colors from the list of colors
  */
  
  public void getRarestColors(List<String> geneColors, List<String> rareColors, Genome genome, Layout layout) {
    
    HashMap<String, Integer> colorCounts = new HashMap<String, Integer>();
    int gcSize = geneColors.size();
    for (int i = 0; i < gcSize; i++) {
      String col = geneColors.get(i);
      colorCounts.put(col, new Integer(0));
    }
   
    //
    // Crank thru all the genes and get their colors.  Figure out the set of colors
    // with the minimum count.  Choose a color from that set.
    //    
 
    Iterator<Gene> git = genome.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      NodeProperties np = layout.getNodeProperties(gene.getID());
      String col = np.getColorName();
      if (geneColors.contains(col)) {
        Integer count = colorCounts.get(col);
        colorCounts.put(col, new Integer(count.intValue() + 1));
      }
    }
    Iterator<Node> nit = genome.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      if (NodeProperties.setWithLinkColor(node.getNodeType())) {
        NodeProperties srcProp = layout.getNodeProperties(node.getID());
        // FIX ME: Abstract this away!
        String col = "black";
        if (node.getNodeType() == Node.INTERCELL) {
          col = srcProp.getSecondColorName();
          if (col == null) {
            col = srcProp.getColorName();
          }
        } else {
          col = srcProp.getColorName();
        }
        if (geneColors.contains(col)) {
          Integer count = colorCounts.get(col);
          colorCounts.put(col, new Integer(count.intValue() + 1));
        }
      }
    }

    //
    // Minimum count colors:
    //
    
    int minVal = Integer.MAX_VALUE;
    HashSet<String> minSet = new HashSet<String>();
    Iterator<String> cckit = colorCounts.keySet().iterator();
    while (cckit.hasNext()) {
      String col = cckit.next();
      int count = colorCounts.get(col).intValue();
      if (count < minVal) {
        minSet.clear();
        minVal = count;
        minSet.add(col);
      } else if (count == minVal) {
        minSet.add(col);
      }
    }
 
    //
    // get a color from minimum set in the predefined order:
    //
    
    rareColors.clear();
    for (int i = 0; i < gcSize; i++) {
      String retval = geneColors.get(i);
      if (minSet.contains(retval)) {
        rareColors.add(retval);
      }
    }
    
    return;
  }  
  
  /***************************************************************************
  **
  ** Get Ith available color
  */
  
  public String getGeneColor(int i) {
    return (colGen_.getGeneColor(i));
  }   
  
  /***************************************************************************
  **
  ** Get number of colors
  */
  
  public int getNumColors() {
    return (colGen_.getNumColors());
  }
  
  /***************************************************************************
  **
  ** Set the IO version
  **
  */
  
  public void setIOVersion(String version) throws IOException {
    iOVersion_ = version;
    // Issue 240 FIX: Must not be too new:
    boolean notTooNew = (iOVersion_.equals("1.0") || 
                         iOVersion_.equals(PREVIOUS_IO_VERSION_A_) || 
                         iOVersion_.equals(PREVIOUS_IO_VERSION_B_) || 
                         iOVersion_.equals(PREVIOUS_IO_VERSION_C_) || 
                         iOVersion_.equals(PREVIOUS_IO_VERSION_D_) ||
                         iOVersion_.equals(PREVIOUS_IO_VERSION_E_) ||
                         iOVersion_.equals(CURRENT_IO_VERSION_));
    if (!notTooNew) {
      throw new NewerVersionIOException(iOVersion_);
    }  
    return;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("BioTapestry");
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Parse out IO version
  **
  */  
  
  public static String versionFromXML(String elemName, Attributes attrs) throws IOException {
    String version = AttributeExtractor.extractAttribute(elemName, attrs, "BioTapestry", "version", false);   
    return ((version == null) ? DEFAULT_VERSION_ : version);
  }
    
  /***************************************************************************
  **
  ** Answer if pre-metabase
  **
  */  
  
  public static boolean preMetabaseVersion(String version) {
    return (version.equals(DEFAULT_VERSION_) || 
            version.equals(PREVIOUS_IO_VERSION_A_) ||
            version.equals(PREVIOUS_IO_VERSION_B_) ||
            version.equals(PREVIOUS_IO_VERSION_C_) ||
            version.equals(PREVIOUS_IO_VERSION_D_) ||
            version.equals(PREVIOUS_IO_VERSION_E_));
  }
  
  
  /***************************************************************************
  **
  ** Data sharing policy to follow for this database
  ** 
  */

  public static class DataSharingPolicy implements Cloneable {
    public boolean init;
    public boolean shareTimeUnits;  
    public boolean shareTimeCourses;    
    public boolean sharePerts; 
    public boolean sharePerEmbryoCounts;
    
    public DataSharingPolicy() {
      this.init = false;
    }

    public DataSharingPolicy(boolean shareTimeUnits, boolean shareTimeCourses, boolean sharePerts, boolean sharePerEmbryoCounts) {
      this.init = true;
      this.shareTimeUnits = shareTimeUnits;
      this.shareTimeCourses = shareTimeCourses;
      this.sharePerts = sharePerts;
      this.sharePerEmbryoCounts = sharePerEmbryoCounts;
    }
    
    public boolean isSpecifyingSharing() {
      return (shareTimeUnits || shareTimeCourses || sharePerts || sharePerEmbryoCounts);
    }
 
    @Override
    public DataSharingPolicy clone() {
      try {
        DataSharingPolicy retval = (DataSharingPolicy)super.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }
    
    public void writeXML(PrintWriter out, Indenter ind) {
      ind.indent();
      out.print("<dataSharingPolicy init=\"");
      out.print(init);
      if (!init) {
        out.println("\" />");
        return;
      }
      out.print("\" units=\"");
      out.print(shareTimeUnits);
      out.print("\" expr=\"");
      out.print(shareTimeCourses);
      out.print("\" pert=\"");
      out.print(sharePerts);
      out.print("\" emb=\"");
      out.print(sharePerEmbryoCounts);
      out.println("\" />");
      return;
    }
  }
    
  public static class PolicyWorker extends AbstractFactoryClient {
    
    private DataAccessContext dacx_;
    
    public PolicyWorker() {
      super(new FactoryWhiteboard());
      myKeys_.add("dataSharingPolicy");
    }
  
    public void setContext(DataAccessContext dacx) {
      dacx_ = dacx;
      return;
    }  

    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      DataSharingPolicy retval = null;
      if (elemName.equals("dataSharingPolicy")) {
        retval = buildFromXML(elemName, attrs);
        dacx_.getMetabase().installDataSharing(retval);
      }
      return (retval);     
    }  
    
    private DataSharingPolicy buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String isInit = AttributeExtractor.extractAttribute(elemName, attrs, "dataSharingPolicy", "init", true);
      String sharTU = AttributeExtractor.extractAttribute(elemName, attrs, "dataSharingPolicy", "units", true);
      String sharTC = AttributeExtractor.extractAttribute(elemName, attrs, "dataSharingPolicy", "expr", true);
      String sharPer = AttributeExtractor.extractAttribute(elemName, attrs, "dataSharingPolicy", "pert", true);
      String sharPEC = AttributeExtractor.extractAttribute(elemName, attrs, "dataSharingPolicy", "emb", true);

      if (!Boolean.valueOf(isInit).booleanValue()) {
        return (new DataSharingPolicy());
      } else {
      
        DataSharingPolicy retval = new DataSharingPolicy(Boolean.valueOf(sharTU).booleanValue(), Boolean.valueOf(sharTC).booleanValue(),
                                                         Boolean.valueOf(sharPer).booleanValue(), Boolean.valueOf(sharPEC).booleanValue());
        return (retval);
      }
    }
  }
}
