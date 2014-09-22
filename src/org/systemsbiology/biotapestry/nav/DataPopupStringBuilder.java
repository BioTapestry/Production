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

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.embedded.ExternalInventoryItem;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.plugin.DataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.ExternalDataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.ExternalLinkDataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.ExternalNodeDataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.InternalDataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.InternalLinkDataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.InternalNodeDataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.PlugInManager;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorker;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorkerClient;

/****************************************************************************
**
** Handles the display, updating, and shutdown of data popups.  Now handling the
** same functions for selection windows.
*/

public class DataPopupStringBuilder implements PluginCallbackWorkerClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private TextTarget textTarg_;
  private ExternalInventoryItem.BuilderArgs args_;
  private ArrayList<DrawBlockData> drawBlocks_;
  private ArrayList<Thread> threadList_;
  private String desc_;
  private boolean bigScreen_;
  private boolean discarded_;
  private boolean isHeadless_;
  private BTState appState_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** constructor
  */

  public DataPopupStringBuilder(BTState appState, TextTarget textTarg, ExternalInventoryItem.BuilderArgs args) {
    appState_ = appState;
    isHeadless_ = appState_.isHeadless();
    textTarg_ = textTarg;
    args_ = args;
    drawBlocks_ = new ArrayList<DrawBlockData>();
    threadList_ = new ArrayList<Thread>();
    bigScreen_ = appState_.getDisplayOptMgr().isForBigScreen();  // legacy use only!
    discarded_ = false;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Discard the builder; disconnect it even if threads are pending
  */ 
    
  public void discardBuilder() {
    discarded_ = true;
    textTarg_ = null;
    return;
  }
 
  /***************************************************************************
  **
  ** Thread callback.  This is where we move onto the AWT thread!  BUT
  ** THIS IS CALLED ON ANY ARBITRARY BACKGROUND THREAD!
  */ 
    
  public void retrievedResult(String idKey, String result) {
    final String fid = idKey;
    final String fresult = result;
    if (!isHeadless_) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          DataPopupStringBuilder.this.processResult(fid, fresult);
        }
      });
    } else {
      processResult(fid, fresult);  
    }
    return;
  }
     
  /***************************************************************************
  **
  ** Prepare the data window
  */ 
  
  public void prepare() { 
    Genome genome = appState_.getDB().getGenome(args_.genomeKey);
    if (args_.isALink) {
      Linkage link = genome.getLinkage(args_.itemID);
      String title = link.getDisplayString(genome, false); 
      String useName = ((title == null) || (title.trim().equals(""))) ? "\" \"" : title;
      String format = appState_.getRMan().getString("dataWindow.dataForLinkFormat");
      desc_ = MessageFormat.format(format, new Object[] {useName}); 
      prepareBlocksAndThreadsForLink();    
    } else {
      Node node = genome.getNode(args_.itemID);
      String title = node.getDisplayString(genome, false); 
      String useName = ((title == null) || (title.trim().equals(""))) ? "\" \"" : title;
      String format = appState_.getRMan().getString("dataWindow.dataForNodeFormat");
      desc_ = MessageFormat.format(format, new Object[] {useName}); 
      prepareBlocksAndThreads();
    }
    
    StringBuffer contentsBuf = new StringBuffer();
    boolean pending = buildString(contentsBuf);
    textTarg_.setTextContents(contentsBuf.toString(), !pending);
    return;
  }
 
  /***************************************************************************
  **
  ** Kickoff background retrieval. Once threads are used, we toss them.
  */ 
  
  public void kickOff() {
    int numBlocks = threadList_.size();
    for (int i = 0; i < numBlocks; i++) {
      Thread nextThread = threadList_.get(i);
      nextThread.start();
    }
    threadList_.clear();
    return;
  } 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Thread callback.
  */ 
    
  private synchronized void processResult(String idKey, String result) {
    if (discarded_) {
      return;
    }
    int index;
    try {
      index = Integer.parseInt(idKey);
    } catch (NumberFormatException nfex) {
      throw new IllegalStateException();
    }
    DrawBlockData forBlock = drawBlocks_.get(index);
    forBlock.updateTextContents(result);
    StringBuffer contentsBuf = new StringBuffer();
    boolean pending = buildString(contentsBuf);
    textTarg_.setTextContents(contentsBuf.toString(), !pending);
    return;
  }
  
  /***************************************************************************
  **
  ** Build the contents string. Tell us if there are still pending items
  */ 
  
  private boolean buildString(StringBuffer buf) {

    boolean retval = false;
    buf.setLength(0);
    buf.append("<html>");
    buf.append("  <body>");
    buf.append("<p></p>");
    buf.append("<center><h1>");
    buf.append(desc_);
    buf.append("</h1></center>\n");
    buf.append("<p></p><hr width=\"50%\" align=\"center\">");
    
    int numBlocks = drawBlocks_.size();
    for (int i = 0; i < numBlocks; i++) {
      DrawBlockData dbd = drawBlocks_.get(i);
      String toDraw = dbd.getTextContents().trim();
      retval = retval || dbd.contentPending();
      buf.append(toDraw);
      if ((toDraw.length() > 0) && (i < (numBlocks - 1))) {
        buf.append("<p></p><hr width=\"50%\" align=\"center\">");
      }      
    }
    buf.append("<p></p><p></p>");
    buf.append("  </body>");      
    buf.append("</html>");

    return (retval);
  }

  /***************************************************************************
  **
  ** Build the contents string for a link
  */ 
  
  private void prepareBlocksAndThreadsForLink() {
   
    ExternalInventoryItem.ArgsForExternalLink afel = null;

    //
    // Stash all string blocks, get threads ready to launch:
    //
    
    int count = 0;
    PlugInManager pim = appState_.getPlugInMgr();
    Iterator<DataDisplayPlugIn> ddit = pim.getLinkDataDisplayPlugIns();
    while (ddit.hasNext()) {
      DataDisplayPlugIn ddpi = ddit.next();
      if (ddpi.isInternal()) {
        InternalLinkDataDisplayPlugIn ilddpi = (InternalLinkDataDisplayPlugIn)ddpi;
        boolean isPending = ilddpi.haveCallbackWorker();
        drawBlocks_.add(new DrawBlockData(ilddpi.getDataAsHTML(args_.genomeKey, args_.itemID), !isPending));
        if (isPending) {
          PluginCallbackWorker pcw = ilddpi.getCallbackWorker(args_.genomeKey, args_.itemID);
          pcw.setIDAndClient(Integer.toString(count), this);
          Thread pcwThread = new Thread(pcw);
          threadList_.add(pcwThread);
        }
      } else {
        if (afel == null) {
          afel = new ExternalInventoryItem.ArgsForExternalLink(args_);
        }
        ExternalLinkDataDisplayPlugIn elddpi = (ExternalLinkDataDisplayPlugIn)ddpi;
        boolean isPending = elddpi.haveCallbackWorker();
        drawBlocks_.add(new DrawBlockData(elddpi.getDataAsHTML(afel.modelNameChain, afel.srcNodeName, afel.srcRegionName, 
                                                               afel.trgNodeName, afel.trgRegionName), !isPending));
        if (isPending) {
          PluginCallbackWorker pcw = elddpi.getCallbackWorker(afel.modelNameChain, afel.srcNodeName, afel.srcRegionName, 
                                                                                   afel.trgNodeName, afel.trgRegionName);
          pcw.setIDAndClient(Integer.toString(count), this);
          Thread pcwThread = new Thread(pcw);
          threadList_.add(pcwThread);  
        }
      }
      count++;
    }
    return;
  }

  /***************************************************************************
  **
  ** Build the block list
  */ 
  
  private void prepareBlocksAndThreads() {
   
    ExternalInventoryItem.ArgsForExternalNode afen = null;
 
    //
    // Stash all string blocks, get threads ready to launch:
    //
    
    int count = 0;
    PlugInManager pim = appState_.getPlugInMgr();
    Iterator<DataDisplayPlugIn> ddit = pim.getDataDisplayPlugIns();
    while (ddit.hasNext()) {
      DataDisplayPlugIn ddpi = ddit.next();
      if (ddpi.isInternal()) {
        if (ddpi instanceof InternalDataDisplayPlugIn) { // Legacy interface!
          if (afen == null) {
            afen = new ExternalInventoryItem.ArgsForExternalNode(args_);
          }
          drawBlocks_.add(new DrawBlockData(((InternalDataDisplayPlugIn)ddpi).getDataAsHTML(args_.itemID, afen.nodeName, bigScreen_), true));
        } else {
          InternalNodeDataDisplayPlugIn inddpi = (InternalNodeDataDisplayPlugIn)ddpi;
          boolean isPending = inddpi.haveCallbackWorker();
          drawBlocks_.add(new DrawBlockData(inddpi.getDataAsHTML(args_.genomeKey, args_.itemID), !isPending));
          if (isPending) {
            PluginCallbackWorker pcw = inddpi.getCallbackWorker(args_.genomeKey, args_.itemID);
            pcw.setIDAndClient(Integer.toString(count), this);
            Thread pcwThread = new Thread(pcw);
            threadList_.add(pcwThread);  
          }
        }
      } else {
        if (afen == null) {
          afen = new ExternalInventoryItem.ArgsForExternalNode(args_);
        }
        if (ddpi instanceof ExternalDataDisplayPlugIn) { // Legacy interface!
          drawBlocks_.add(new DrawBlockData(((ExternalDataDisplayPlugIn)ddpi).getDataAsHTML(afen.nodeName), true));
        } else {
          ExternalNodeDataDisplayPlugIn enddpi = (ExternalNodeDataDisplayPlugIn)ddpi;
          boolean isPending = enddpi.haveCallbackWorker();
          drawBlocks_.add(new DrawBlockData(enddpi.getDataAsHTML(afen.modelNameChain, afen.nodeDisplay, afen.regionName), !isPending));
          if (isPending) {
            PluginCallbackWorker pcw = enddpi.getCallbackWorker(afen.modelNameChain, afen.nodeDisplay, afen.regionName);
            pcw.setIDAndClient(Integer.toString(count), this);
            Thread pcwThread = new Thread(pcw);
            threadList_.add(pcwThread);  
          }
        }
      }
      count++;
    }
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Answer if we need per-instance windows:
  */ 
  
  public static boolean needPerInstance(BTState appState, ExternalInventoryItem.BuilderArgs args) {
    if (args.isALink) {
      return (needPerInstanceForLink(appState, args));
    } else {
      return (needPerInstanceForNode(appState, args));
    }    
  }
   
  /***************************************************************************
  **
  ** Answer if we need per-instance windows:
  */ 
  
  public static boolean needPerInstanceForLink(BTState appState, ExternalInventoryItem.BuilderArgs args) {
    ExternalInventoryItem.ArgsForExternalLink afel = null;
    PlugInManager pim = appState.getPlugInMgr();
    Iterator<DataDisplayPlugIn> ddit = pim.getLinkDataDisplayPlugIns();
    while (ddit.hasNext()) {
      DataDisplayPlugIn ddpi = ddit.next();
      if (ddpi.isInternal()) {
        InternalLinkDataDisplayPlugIn ilddpi = (InternalLinkDataDisplayPlugIn)ddpi;
        if (ilddpi.requiresPerInstanceDisplay(args.genomeKey, args.itemID)) {
          return (true);
        }
      } else {
        if (afel == null) {
          afel = new ExternalInventoryItem.ArgsForExternalLink(args);
        }
        ExternalLinkDataDisplayPlugIn elddpi = (ExternalLinkDataDisplayPlugIn)ddpi;
        if (elddpi.requiresPerInstanceDisplay(afel.modelNameChain, afel.srcNodeName, afel.srcRegionName, 
                                                                   afel.trgNodeName, afel.trgRegionName)) {
          return (true);
        }
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Build the block list
  */ 
  
  public static boolean needPerInstanceForNode(BTState appState, ExternalInventoryItem.BuilderArgs args) {

    ExternalInventoryItem.ArgsForExternalNode afen = null;

    //
    // Stash all string blocks, get threads ready to launch:
    //
    
    PlugInManager pim = appState.getPlugInMgr();
    Iterator<DataDisplayPlugIn> ddit = pim.getDataDisplayPlugIns();
    while (ddit.hasNext()) {
      DataDisplayPlugIn ddpi = ddit.next();
      if (ddpi.isInternal()) {
        if (ddpi instanceof InternalDataDisplayPlugIn) { // Legacy interface!
          continue;
        } else {
          InternalNodeDataDisplayPlugIn inddpi = (InternalNodeDataDisplayPlugIn)ddpi;
          if (inddpi.requiresPerInstanceDisplay(args.genomeKey, args.itemID)) {
            return (true);
          }
        }
      } else {
        if (ddpi instanceof ExternalDataDisplayPlugIn) { // Legacy interface!
          continue;
        } else {
          if (afen == null) {
            afen = new ExternalInventoryItem.ArgsForExternalNode(args);
          }          
          ExternalNodeDataDisplayPlugIn enddpi = (ExternalNodeDataDisplayPlugIn)ddpi;
          if (enddpi.requiresPerInstanceDisplay(afen.modelNameChain, afen.nodeDisplay, afen.regionName)) { 
            return (true);
          }
        }
      }
    }
    return (false);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Generic interface allows us to operate headless
  */
  
  public interface TextTarget {       
    public void setTextContents(String text, boolean complete);
  }
  
  /***************************************************************************
  ** 
  ** Swing method of using interface
  */ 
  
  public static class JEPWrapper implements TextTarget {
     
    private JEditorPane jep_;
     
    public JEPWrapper(JEditorPane jep) {
      jep_ = jep;
    }
     
    public void setTextContents(String text, boolean complete) {
      jep_.setText(text);
      jep_.setCaretPosition(0);
      //
      // Gotta do this, or you get a ton of exceptions from down in the 
      // Swing layout code on repaint (Java 4 at least)
      //
      jep_.invalidate();
      jep_.validate();
      return;
    }    
  }
  
  /***************************************************************************
  ** 
  ** For batch applications
  */ 
   
  public static class StreamWrapper implements TextTarget {
      
    private PrintWriter out_;
      
    public StreamWrapper(PrintWriter out) {
      out_ = out;
    }
      
    public void setTextContents(String text, boolean complete) {
      out_.print(text);
      return;
    }
  }
    
  /***************************************************************************
  ** 
  ** For web server applications
  */ 
   
  public static class BufferWrapper implements TextTarget {
      
    private SyncedBuffer buf_;
      
    public BufferWrapper(SyncedBuffer buf) {
      buf_ = buf;
    }
      
    public void setTextContents(String text, boolean complete) {
      buf_.setTextContents(text, complete);
      return;
    }      
  }
  
  /***************************************************************************
  ** 
  ** For web server applications
  */ 
   
  public static class SyncedBuffer {
      
    private StringBuffer buf_;
    private boolean isComplete_;
      
    public SyncedBuffer() {
      buf_ = new StringBuffer();
      isComplete_ = false;
    }
      
    public synchronized void setTextContents(String text, boolean complete) {
      buf_.setLength(0);
      buf_.append(text);
      isComplete_ = complete;
      return;
    }
    
    public synchronized String getTextContents() {
      return (buf_.toString());
    }
    
    //
    // Yes, separate call may mean we get stale info, which may result in one extra callback:
    //
    public synchronized boolean getComplete() {
      return (isComplete_);
    } 
    
  } 
  
  /***************************************************************************
  ** 
  ** Draw Blocks need more than just strings now
  */ 
   
  private static class DrawBlockData {
      
    private String content_;
    private boolean completed_;
      
    public DrawBlockData(String content, boolean completed) {
      content_ = content;
      completed_ = completed;
    }
      
    public void updateTextContents(String text) {
      content_ = (text == null) ? "" : text.trim();
      completed_ = true;
      return;
    }
    
    public String getTextContents() {
      return (content_);
    } 
    
    public boolean contentPending() {
      return (!completed_);
    }   
  } 
}
