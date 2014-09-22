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


package org.systemsbiology.biotapestry.nav;

import java.io.IOException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.OtherFlowKey;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.event.EventManager;
import org.systemsbiology.biotapestry.embedded.ExternalInventoryItem;
import org.systemsbiology.biotapestry.event.GeneralChangeListener;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeListener;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.event.SelectionChangeEvent;
import org.systemsbiology.biotapestry.event.SelectionChangeListener;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.TopOfTheHeap;
import org.systemsbiology.biotapestry.ui.dialogs.PathDisplayFrameFactory;
import org.systemsbiology.biotapestry.ui.dialogs.UsageFrameFactory;

/****************************************************************************
**
** Handles the display, updating, and shutdown of data popups.  Now handling the
** same functions for selection windows.
*/

public class DataPopupManager implements GeneralChangeListener, ModelChangeListener, SelectionChangeListener {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private HashMap<FrameKey, FrameInfo> popups_;
  private HashMap<FrameKey, FrameInfo> linkPopups_;
  private HashMap<String, FrameInfo> nodeSelections_;
  private HashMap<String, FrameInfo> linkSelections_;
  private FrameInfo pathDisplay_;
  private boolean clearing_ = false;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Notify listener of general change
  */ 
  
  public void generalChangeOccurred(GeneralChangeEvent gcev) {
    fullRefresh();
    return;
  }

  /***************************************************************************
  **
  ** Called when model has changed
  */   

  public void modelHasChanged(ModelChangeEvent event, int remaining) {
    modelHasChanged(event);
    return;
  }      

  /***************************************************************************
  **
  ** Notify listener of selection change. When the model is changed, we need to drop
  ** the path display:
  */ 
  
  public void selectionHasChanged(SelectionChangeEvent scev) {
    if (pathDisplay_ == null) {
      return;
    }
    if (scev.getChangeType() != SelectionChangeEvent.SELECTED_MODEL) {
      return;
    }
    if (!scev.getGenomeKey().equals(pathDisplay_.genomeKey)) {
      dropPathWindow();
    }
    return;
  }
  
  
  /***************************************************************************
  **
  ** Notify listener of model change
  */ 
  
  public void modelHasChanged(ModelChangeEvent mcev) { 
    int change = mcev.getChangeType();
    if (change == ModelChangeEvent.UNSPECIFIED_CHANGE) {
      fullRefresh();
    } else if (change == ModelChangeEvent.MODEL_DROPPED) {
      // This means the whole thing is gone:
      if (mcev.getGenomeKey().equals(appState_.getDB().getGenome().getID())) {
        fullClear();
      } else {
        fullRefresh();
      }
    } else if (change == ModelChangeEvent.PROPERTY_CHANGE) {
      fullRefresh();
    } else if (change == ModelChangeEvent.MODEL_ADDED) {
      fullRefresh();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle node data
  */ 
  
  public DataReturn popupData(String genomeID, String nodeID) {
 
    //
    // Have to work with the root instance:
    //
    
    TopOfTheHeap tooth = new TopOfTheHeap(appState_, genomeID);
    genomeID = tooth.getID();
    boolean isInstance = tooth.isInstance();
     
    //
    // If the window is already present, just raise it.  Otherwise, make a
    // new one.
    // 
    
    ExternalInventoryItem.BuilderArgs ba = new ExternalInventoryItem.BuilderArgs(appState_, genomeID, nodeID, false);
    boolean needPerInstance = DataPopupStringBuilder.needPerInstanceForNode(appState_, ba);     
    FrameKey fkey = new FrameKey(appState_, genomeID, nodeID, needPerInstance, isInstance);
    ba = new ExternalInventoryItem.BuilderArgs(appState_, fkey.genomeID, fkey.itemID, false);
    // This will be used for the web server results:
    DataPopupStringBuilder.SyncedBuffer sbuf = null;
    
    FrameInfo existing = popups_.get(fkey);
    if (existing != null) { 
      if (!appState_.isHeadless()) {
        existing.frame.setExtendedState(JFrame.NORMAL);
        existing.frame.toFront();
      } else {
        sbuf = existing.sbuf;     
      }
    } else {
      DataPopupStringBuilder.TextTarget putt;      
      JEditorPane pane = null;
      if (!appState_.isHeadless()) {
        pane = new JEditorPane("text/html", "");
        putt = new DataPopupStringBuilder.JEPWrapper(pane);
      } else {
        if (appState_.isWebApplication()) {
          sbuf = new DataPopupStringBuilder.SyncedBuffer(); 
          putt = new DataPopupStringBuilder.BufferWrapper(sbuf);
        } else {
          putt = new DataPopupStringBuilder.StreamWrapper(appState_.getCurrentPrintWriter());
        }
      }
      DataPopupStringBuilder builder = new DataPopupStringBuilder(appState_, putt, ba);
      builder.prepare();
      Genome keyGenome = appState_.getDB().getGenome(fkey.genomeID);
      Node node = keyGenome.getNode(fkey.itemID);
      String title = node.getDisplayString(keyGenome, false); 
      FrameInfo fi;
      JFrame frame = null;
      if (!appState_.isHeadless()) {
        frame = createDataWindow(fkey, title, pane, true);
        fi = new FrameInfo(fkey.genomeID, fkey.itemID, title, frame, null, builder);
      } else {
        fi = new FrameInfo(fkey.genomeID, fkey.itemID, title, sbuf, null, builder);
      }
      popups_.put(fkey, fi);
      if (!appState_.isHeadless()) {
        frame.setVisible(true);
      }
      builder.kickOff();
    }
    // Note the sbuf.getComplete() may be inconsistent, but only in a conservative fashion, giving us another later callback:
    return ((sbuf == null) ? null : new DataReturn(sbuf.getTextContents(), genomeID, nodeID, !sbuf.getComplete(), OtherFlowKey.TARDY_NODE_DATA));
  }
  
  /***************************************************************************
  **
  ** Handle link data
  */ 
  
  public Map<String, Object> popupLinkData(String genomeID, String linkID) {
    
	  String title = null;
	  
    //
    // Have to work with the root or root instance:
    //
    
    TopOfTheHeap tooth = new TopOfTheHeap(appState_, genomeID);
    genomeID = tooth.getID();
    boolean isInstance = tooth.isInstance();
  
    ExternalInventoryItem.BuilderArgs ba = new ExternalInventoryItem.BuilderArgs(appState_, genomeID, linkID, true);
    boolean needPerInstance = DataPopupStringBuilder.needPerInstanceForLink(appState_, ba);
    FrameKey fkey = new FrameKey(appState_, genomeID, linkID, needPerInstance, isInstance);
    ba = new ExternalInventoryItem.BuilderArgs(appState_, fkey.genomeID, fkey.itemID, true);
    DataPopupStringBuilder.SyncedBuffer sbuf = null;
    
    Genome keyGenome = appState_.getDB().getGenome(fkey.genomeID);
    Linkage link = keyGenome.getLinkage(fkey.itemID);
    title = link.getDisplayString(keyGenome, false);
    
    //
    // If the window is already present, just raise it.  Otherwise, make a
    // new one.
    //
  
    FrameInfo existing = linkPopups_.get(fkey);
    if (existing != null) {      
      if (!appState_.isHeadless()) {
        existing.frame.setExtendedState(JFrame.NORMAL);
        existing.frame.toFront();
      } else {
        sbuf = existing.sbuf;     
      }
    } else {
      DataPopupStringBuilder.TextTarget putt;
      JEditorPane pane = null;
      if (!appState_.isHeadless()) {
        pane = new JEditorPane("text/html", "");
        putt = new DataPopupStringBuilder.JEPWrapper(pane);
      } else {
        if (appState_.isWebApplication()) {
          sbuf = new DataPopupStringBuilder.SyncedBuffer(); 
          putt = new DataPopupStringBuilder.BufferWrapper(sbuf);
        } else {
          putt = new DataPopupStringBuilder.StreamWrapper(appState_.getCurrentPrintWriter());
        }       
      }
      DataPopupStringBuilder builder = new DataPopupStringBuilder(appState_, putt, ba);
      builder.prepare();
      FrameInfo fi;
      JFrame frame = null;
      if (!appState_.isHeadless()) {
        frame = createDataWindow(fkey, title, pane, true);
        fi = new FrameInfo(fkey.genomeID, fkey.itemID, title, frame, null, builder);
      } else {
        fi = new FrameInfo(fkey.genomeID, fkey.itemID, title, sbuf, null, builder);
      }
      linkPopups_.put(fkey, fi);
      if (!appState_.isHeadless()) {
        frame.setVisible(true);
      }
      builder.kickOff();
    }  
    
    Map<String, Object> results = new HashMap<String, Object>();
    results.put("title", title);
    
    if (sbuf != null) {
      // Note the sbuf.getComplete() may be inconsistent, but only in a conservative fashion, giving us another later callback:
      results.put("contents", new DataReturn(sbuf.getTextContents(), genomeID, linkID, !sbuf.getComplete(), OtherFlowKey.TARDY_LINK_DATA));
    }
    
    return results;
  } 
  
  /***************************************************************************
  **
  ** Manage a frame that has been created
  */  
 
  public void manageFrame(JFrame uijd) {
    if (appState_.isHeadless()) {
      return;
    }  
    if (uijd instanceof UsageFrameFactory.DesktopLinkUsageFrame) {
      displayLinkUsages((UsageFrameFactory.DesktopLinkUsageFrame)uijd);
    } else if (uijd instanceof UsageFrameFactory.DesktopNodeUsageFrame) {
      displayNodeUsages((UsageFrameFactory.DesktopNodeUsageFrame)uijd);
    } else if (uijd instanceof PathDisplayFrameFactory.DesktopPathFrame) {
      displayPathFrame((PathDisplayFrameFactory.DesktopPathFrame)uijd);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Pop up a window showing where the node is used
  */  
 
  private void displayNodeUsages(UsageFrameFactory.DesktopNodeUsageFrame nuf) {
    String baseID = nuf.getBaseNodeID();
    FrameInfo existing = nodeSelections_.get(baseID);
    if (existing != null) { 
      // Note we are just ignoring the frame we have just been handled. We already have one.
      nuf.dispose();
      existing.frame.setExtendedState(JFrame.NORMAL);
      existing.frame.toFront();
    } else {
      nodeSelections_.put(baseID, new FrameInfo(null, baseID, null, nuf, null, null));
      nuf.setVisible(true);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Pop up a window showing where the links are used
  */  
 
  private void displayLinkUsages(UsageFrameFactory.DesktopLinkUsageFrame luf) {
    Set<String> baseLinks = luf.getLinkSet();
    String baseSrcID = luf.getSrcID();
      
    FrameInfo existing = linkSelections_.get(baseSrcID);
    if (existing != null) {
      if (!existing.linkIDs.equals(baseLinks)) {
        ((UsageFrameFactory.DesktopLinkUsageFrame)existing.frame).updateLinkSet(baseLinks);
      }
      // Note we are just ignoring the frame we have just been handled. We already have one.
      luf.dispose();
      existing.frame.setExtendedState(JFrame.NORMAL);
      existing.frame.toFront();
    } else {
      linkSelections_.put(baseSrcID, new FrameInfo(null, baseSrcID, null, luf, baseLinks, null));
      luf.setVisible(true);
    }
    return;
  }   
   
  /***************************************************************************
  **
  ** Pop up a window for showing paths
  */  
 
  private void displayPathFrame(PathDisplayFrameFactory.DesktopPathFrame nuf) {
    String srcID = nuf.getCurrentSource();
    String trgID = nuf.getCurrentTarget();
    String genomeID = nuf.getCurrentModel();
    
    FrameInfo existing = pathDisplay_;
    if (existing != null) { 
      if (!existing.itemID2.equals(srcID) || !existing.itemID.equals(trgID) || !existing.genomeKey.equals(genomeID)) {
        ((PathDisplayFrameFactory.DesktopPathFrame)existing.frame).refresh(genomeID, srcID, trgID);
        existing.itemID2 = srcID;
        existing.itemID = trgID;
        existing.genomeKey = genomeID;       
      }    
      // Note we are just ignoring the frame we have just been handled. We already have one.
      nuf.dispose();
      existing.frame.setExtendedState(JFrame.NORMAL);
      existing.frame.toFront();
    } else {
      pathDisplay_ = new FrameInfo(nuf, srcID, trgID, genomeID);
      nuf.setVisible(true);
    }
    return;
  }    
  
  
  /***************************************************************************
  **
  ** Drop path window
  */ 
  
  public void dropPathWindow() {
    if (clearing_) {
      return;
    }
    if (pathDisplay_ != null) {
      if (!appState_.isHeadless()) {
        pathDisplay_.frame.dispose();
      }
      pathDisplay_ = null;
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Drop selection window
  */ 
  
  public void dropSelectionWindow(String geneID) {
    if (clearing_) {
      return;
    }
    FrameInfo existing = nodeSelections_.get(geneID);
    if (existing != null) {
      nodeSelections_.remove(geneID);
      if (!appState_.isHeadless()) {
        existing.frame.dispose();
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Drop selection window
  */ 
  
  public void dropLinkSelectionWindow(String srcID) {
    if (clearing_) {
      return;
    }
    FrameInfo existing = linkSelections_.get(srcID);
    if (existing != null) {
      linkSelections_.remove(srcID);
      if (!appState_.isHeadless()) {
        existing.frame.dispose();
      }
    }
    return;
  }    
  
  /***************************************************************************
  **
  ** Drop data window
  */ 
  
  public void dropDataWindow(FrameKey fiKey) {
    if (clearing_) {
      return;
    }
    FrameInfo existing = popups_.get(fiKey);
    if (existing != null) {
      popups_.remove(fiKey);
      existing.builder.discardBuilder();
      if (!appState_.isHeadless()) {
        existing.frame.dispose();
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Drop data window for a link:
  */ 
  
  public void dropLinkDataWindow(FrameKey fiKey) {
    if (clearing_) {
      return;
    }
    FrameInfo existing = linkPopups_.get(fiKey);
    if (existing != null) {
      linkPopups_.remove(fiKey);
      existing.builder.discardBuilder();
      if (!appState_.isHeadless()) {
        existing.frame.dispose();
      }
    }
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Basic constructor
  */

  public DataPopupManager(BTState appState) {
    appState_ = appState;
    popups_ = new HashMap<FrameKey, FrameInfo>();
    linkPopups_ = new HashMap<FrameKey, FrameInfo>();
    nodeSelections_ = new HashMap<String, FrameInfo>();
    linkSelections_ = new HashMap<String, FrameInfo>();
    clearing_ = false;
    EventManager em = appState.getEventMgr();
    em.addModelChangeListener(this);
    em.addGeneralChangeListener(this);
    em.addSelectionChangeListener(this);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Build the data window
  */ 
  
  private JFrame createDataWindow(FrameKey key, String title, JEditorPane pane, boolean forNode) {
    pane.setEditable(false);
 
    if (appState_.getDisplayOptMgr().isForBigScreen()) {
   	  HTMLDocument doc = (HTMLDocument)pane.getDocument();
   	  StyleSheet styles = doc.getStyleSheet();
      String rule1 = "h1 {font-size: 60pt;}";
      styles.addRule(rule1);
    }

    JFrame frame = new JFrame(((title == null) || (title.trim().equals(""))) ? "\" \"" : title);
    URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/BioTapFab16White.gif");  
    frame.setIconImage(new ImageIcon(ugif).getImage());    
    
    final JEditorPane fpane = pane;
    pane.addHyperlinkListener(new HyperlinkListener() {
      public void hyperlinkUpdate(HyperlinkEvent ev) {
        try {
          if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            fpane.setPage(ev.getURL());
          }
        } catch (IOException ex) {
        }
      }
    });
    
    final FrameKey closeID = key;
    if (forNode) {     
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          DataPopupManager.this.dropDataWindow(closeID);
        }
      });
    } else {   
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          DataPopupManager.this.dropLinkDataWindow(closeID);
        }
      });
    }
 
    frame.setSize(1024, 768);
    JPanel cp = (JPanel)frame.getContentPane();
    cp.add(new JScrollPane(pane));
  //  frame.setContentPane(new JScrollPane(pane));
    // Forces scrollbar back up to the top of the document:
    pane.setCaretPosition(0);
    return (frame);
  }


  /***************************************************************************
  **
  ** Do a full refresh
  */ 
  
  private void fullRefresh() {
    
    //
    // Crank thru all the frames and refresh them
    //
    Database db = appState_.getDB();
    
    Iterator<FrameKey> fit = new HashSet<FrameKey>(popups_.keySet()).iterator();
    while (fit.hasNext()) {
      FrameKey fiID = fit.next();
      FrameInfo info = popups_.get(fiID);   
      Genome nodeGenome = db.getGenome(info.genomeKey);
      Node node = (nodeGenome == null) ? null : nodeGenome.getNode(info.itemID);
      // name may have changed...
      if (node != null) {
        ExternalInventoryItem.BuilderArgs ba = new ExternalInventoryItem.BuilderArgs(appState_, info.genomeKey, info.itemID, false);
        boolean needPerInstance = DataPopupStringBuilder.needPerInstanceForNode(appState_, ba);
        boolean isInstance = !GenomeItemInstance.isBaseID(info.itemID);
        if (needPerInstance != isInstance) {
          dropDataWindow(fiID);
        } else {
          info.winTitle = node.getDisplayString(nodeGenome, false);
          fullRefreshCore(info, false);
        }
      } else {
        dropDataWindow(fiID);
      }
    }
    
    Iterator<FrameKey> flit = new HashSet<FrameKey>(linkPopups_.keySet()).iterator();
    while (flit.hasNext()) {
      FrameKey fiID = flit.next();
      FrameInfo info = linkPopups_.get(fiID);
      Genome linkGenome = db.getGenome(info.genomeKey);
      Linkage link = (linkGenome == null) ? null : linkGenome.getLinkage(info.itemID);
      if (link != null) {
        ExternalInventoryItem.BuilderArgs ba = new ExternalInventoryItem.BuilderArgs(appState_, info.genomeKey, info.itemID, true);
        boolean needPerInstance = DataPopupStringBuilder.needPerInstanceForLink(appState_, ba);
        boolean isInstance = !GenomeItemInstance.isBaseID(info.itemID);
        if (needPerInstance != isInstance) {
          dropLinkDataWindow(fiID);
        } else {
          info.winTitle = link.getDisplayString(linkGenome, false);
          fullRefreshCore(info, true);
        }
      } else {
        dropLinkDataWindow(fiID);
      }
    }
    
    Genome genome = db.getGenome();
    
    Iterator<FrameInfo> fiit = new HashSet<FrameInfo>(nodeSelections_.values()).iterator();
    while (fiit.hasNext()) {
      FrameInfo info = fiit.next();
      Node node = genome.getNode(info.itemID);
      if (node != null) {
        ((UsageFrameFactory.DesktopNodeUsageFrame)(info.frame)).refreshList();
      } else {
        dropSelectionWindow(info.itemID);
      }
    }   
    
    fiit = new HashSet<FrameInfo>(linkSelections_.values()).iterator();
    while (fiit.hasNext()) {
      FrameInfo info = fiit.next();
      Node node = genome.getNode(info.itemID);
      if (node == null) {
        dropLinkSelectionWindow(info.itemID);
      } else {
        HashSet<String> baseLinks = new HashSet<String>();
        Iterator<String> lit = info.linkIDs.iterator();
        while (lit.hasNext()) {
          String linkID = lit.next();
          String baseLinkID = GenomeItemInstance.getBaseID(linkID);
          Linkage baseLink = genome.getLinkage(baseLinkID);
          if (baseLink == null) {
            continue;
          }
          if (!baseLink.getSource().equals(info.itemID)) {
            continue;
          }
          baseLinks.add(baseLinkID);
        }
        if (baseLinks.size() == 0) {
          dropLinkSelectionWindow(info.itemID);
        } else if (!baseLinks.equals(info.linkIDs)) {
          ((UsageFrameFactory.DesktopLinkUsageFrame)(info.frame)).updateLinkSet(baseLinks);
          info.linkIDs = baseLinks;
        } else {
          ((UsageFrameFactory.DesktopLinkUsageFrame)(info.frame)).refreshList();
        }
      }
    } 
    
    if (pathDisplay_ != null) {
      boolean keepIt = true;
      Genome pdGenome = db.getGenome(pathDisplay_.genomeKey);
      if (pdGenome == null) {
        keepIt = false;
      } else { 
        Node srcNode = pdGenome.getNode(pathDisplay_.itemID2);
        if (srcNode == null) {
          keepIt = false;
        } else {
          Node trgNode = pdGenome.getNode(pathDisplay_.itemID);
          if (trgNode == null) {
            keepIt = false;
          }
        }
      }
      if (!keepIt) {
        dropPathWindow();
      } else {
        ((PathDisplayFrameFactory.DesktopPathFrame)pathDisplay_.frame).refresh(pathDisplay_.genomeKey, 
                                                                               pathDisplay_.itemID2, 
                                                                               pathDisplay_.itemID);  
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** full refresh core ops
  */ 
  
  private void fullRefreshCore(FrameInfo info, boolean forLink) {
    boolean isHeadless = appState_.isHeadless();
    info.builder.discardBuilder();
    if (!isHeadless) {
      info.frame.setTitle(info.winTitle);
    }
    ExternalInventoryItem.BuilderArgs ba = new ExternalInventoryItem.BuilderArgs(appState_, info.genomeKey, info.itemID, forLink);
    DataPopupStringBuilder.TextTarget putt;
    JEditorPane pane = null;
    if (!isHeadless) {
      pane = new JEditorPane("text/html", "");
      putt = new DataPopupStringBuilder.JEPWrapper(pane);
    } else {
      putt = new DataPopupStringBuilder.StreamWrapper(appState_.getCurrentPrintWriter());        
    }
    info.builder = new DataPopupStringBuilder(appState_, putt, ba);
    info.builder.prepare();
    info.builder.kickOff();
    return;
  }  

  /***************************************************************************
  **
  ** Do a full clear
  */ 
  
  private void fullClear() {
    //
    // Crank thru all the frames and eliminate them
    //
    clearing_ = true;
    Iterator<FrameInfo> fit = popups_.values().iterator();
    while (fit.hasNext()) {
      FrameInfo info = fit.next();
      info.builder.discardBuilder();
      if (!appState_.isHeadless()) {
        info.frame.dispose();
      }
    }
    popups_.clear();
    
    Iterator<FrameInfo> flit = linkPopups_.values().iterator();
    while (flit.hasNext()) {
      FrameInfo info = flit.next();
      info.builder.discardBuilder();
      if (!appState_.isHeadless()) {
        info.frame.dispose();
      }
    }
    linkPopups_.clear();

    fit = nodeSelections_.values().iterator();
    while (fit.hasNext()) {
      FrameInfo info = fit.next();
      if (!appState_.isHeadless()) {
        info.frame.dispose();
      }
    }
    nodeSelections_.clear();  
    
    fit = linkSelections_.values().iterator();
    while (fit.hasNext()) {
      FrameInfo info = fit.next();
      if (!appState_.isHeadless()) {
        info.frame.dispose();
      }
    }
    linkSelections_.clear();   
   
    if (pathDisplay_ != null) {
      if (!appState_.isHeadless()) {
        pathDisplay_.frame.dispose();
      }
    } 
    pathDisplay_ = null;

    clearing_ = false;
    return;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Holds frame info
  */  
  
  private static class FrameInfo {
    String genomeKey;
    String itemID;
    String itemID2;
    String winTitle;
    JFrame frame;
    DataPopupStringBuilder.SyncedBuffer sbuf;
    Set<String> linkIDs;
    DataPopupStringBuilder builder;
    
    FrameInfo(String genomeKey, String itemID, String winTitle, JFrame frame, Set<String> linkIDs, DataPopupStringBuilder builder) {
      this.genomeKey = genomeKey;
      this.itemID = itemID;
      this.winTitle = winTitle;
      this.frame = frame;
      this.sbuf = null;
      this.linkIDs = linkIDs;
      this.builder = builder;
    }
    
    FrameInfo(String genomeKey, String itemID, String winTitle, DataPopupStringBuilder.SyncedBuffer sbuf, Set<String> linkIDs, DataPopupStringBuilder builder) {
      this.genomeKey = genomeKey;
      this.itemID = itemID;
      this.winTitle = winTitle;
      this.frame = null;
      this.sbuf = sbuf;
      this.linkIDs = linkIDs;
      this.builder = builder;
    }
    
    FrameInfo(JFrame frame, String srcID, String trgID, String genomeID) {
      this.frame = frame;
      this.itemID2 = srcID;
      this.itemID = trgID;
      this.genomeKey = genomeID;
    }    
  }
  
  /***************************************************************************
  **
  ** FrameKeys
  */  
  
  public static class FrameKey {
    String genomeID;
    String itemID;
  
    FrameKey(BTState appState, String genomeID, String itemID, boolean needPerInstance, boolean isInstance) {
      if (needPerInstance) {
        this.genomeID = genomeID;
        this.itemID = itemID;
      } else {
        if (isInstance) {
          this.genomeID = appState.getDB().getGenome().getID();
          this.itemID = GenomeItemInstance.getBaseID(itemID);
        } else {
          this.genomeID = genomeID;
          this.itemID = itemID;
        }  
      } 
    } 

    @Override
    public int hashCode() {
      return (genomeID.hashCode() + itemID.hashCode());
    }
  
    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof FrameKey)) {
        return (false);
      }
      FrameKey otherFK = (FrameKey)other;
      return ((this.genomeID.equals(otherFK.genomeID)) && (this.itemID.equals(otherFK.itemID)));
    }  
  
    @Override
    public String toString() {
      return ("FrameKey genomeID: " + genomeID + " itemID: " + itemID);
    }
  }
  
  /***************************************************************************
  **
  ** Running State
  */
        
  public static class DataReturn {

    private String html_;
    private String genomeKey_;
    private String id_;
    private boolean incomplete_;
    private OtherFlowKey flowKey_;
    
    public DataReturn(String html, String genomeKey, String id, boolean incomplete, OtherFlowKey ofk) {
      html_ = html;
      genomeKey_ = genomeKey;
      id_ = id;
      incomplete_ = incomplete;
      flowKey_ = ofk;
    }
    
    public String getHTML() {
      return (html_);
    }
    
    public String getGenomeKey() {
      return (genomeKey_);
    }
    
    public String getID() {
      return (id_);
    }
    
    public void setIncomplete(boolean incomplete) {
    	this.incomplete_ = incomplete;
    }
    
    public boolean getIncomplete() {
    	return incomplete_;
    }
    
    public OtherFlowKey getFlowKey() {
      return (flowKey_);
    }  
  }
}
