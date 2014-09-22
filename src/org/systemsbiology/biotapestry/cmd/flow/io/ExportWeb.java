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

package org.systemsbiology.biotapestry.cmd.flow.io;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.ViewExporter;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.DirectoryNamedOutputStreamSource;
import org.systemsbiology.biotapestry.util.NamedOutputStreamSource;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.WebPublisher;

/****************************************************************************
**
** Export a cheapo web version
*/

public class ExportWeb extends AbstractControlFlow  {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
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
  
  public ExportWeb(BTState appState) {
    super(appState);
    name = "command.Web";
    desc = "command.Web";
    mnem = "command.WebMnem";
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    return (true);
  }
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    StepState retval = new StepState(appState_, dacx);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      StepState ans;
      if (last == null) {
        ans = new StepState(appState_, cfh.getDataAccessContext());
        ans.cfh = cfh; 
      } else {
        ans = (StepState)last.currStateX;
        if (ans.cfh == null) {
          ans.cfh = cfh;
        }
      }
      if (ans.getNextStep().equals("oneStep")) {
        next = ans.oneStep();      
      } else {
        throw new IllegalStateException();
      }
      if (!next.state.keepLooping()) {
        return (next);
      }
      last = next;
    }
  }
  
  /***************************************************************************
  **
  ** Running State
  */
        
  public static class StepState implements DialogAndInProcessCmd.CmdState, BackgroundWorkerOwner {
     
    private ServerControlFlowHarness cfh;
    private String nextStep_;    
    private BTState appState_;
    
    private Boolean doFile_;
    private File targetDir_;
  
    private NamedOutputStreamSource nos_;
    private Map<WebPublisher.ModelScale, ViewExporter.BoundsMaps> intersectionMap_;
    private Set<WebPublisher.ModelScale> publishKeys_;
    
    private LoadSaveSupport myLsSup_;
    private boolean wantHtmlSkeleton_;
    private String holdKey_;
    private double zoom_;
    private UndoSupport support_;
    private DataAccessContext dacx_;
 
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "oneStep";
      myLsSup_ = appState_.getLSSupport();
      dacx_ = dacx.getContextForRoot();
    }
    
    /***************************************************************************
    **
    ** Next step...
    */ 
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Set the popup params
    */ 
        
    public void setParams(Object[] args) {
      doFile_ = (Boolean)args[0];
      if (doFile_.booleanValue()) {
        targetDir_ = new File((String)args[1]);
      } else {
        nos_ = (NamedOutputStreamSource)args[1];
        intersectionMap_ = (Map<WebPublisher.ModelScale, ViewExporter.BoundsMaps>)args[2];
        publishKeys_ = (Set<WebPublisher.ModelScale>)args[3];
      }
      return;
     }
    
    /***************************************************************************
    **
    ** Do it in one step
    */ 
       
    private DialogAndInProcessCmd oneStep() { 
      boolean skipRoot = false;
     
      if (!appState_.isHeadless()) {      
        //
        // See if user wants the root:
        //        
        ResourceManager rMan = appState_.getRMan();          
        int doRoot = 
          JOptionPane.showConfirmDialog(appState_.getTopFrame(), 
                                        rMan.getString("webPublish.wantRoot"), 
                                        rMan.getString("webPublish.wantRootTitle"),
                                        JOptionPane.YES_NO_CANCEL_OPTION);        
        if (doRoot == JOptionPane.CANCEL_OPTION) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
        }
        skipRoot = (doRoot == JOptionPane.NO_OPTION);
        
        targetDir_ = myLsSup_.getFprep().getExistingWritableDirectory("WebDirectory");
        if (targetDir_ == null) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
        }
        
        myLsSup_.getFprep().setPreference("WebDirectory", targetDir_.getAbsolutePath());
        nos_ = new DirectoryNamedOutputStreamSource(targetDir_);
        wantHtmlSkeleton_ = true;
        intersectionMap_ = null;
        publishKeys_ = null;
      } else {
        if (doFile_.booleanValue()) {
          if (!myLsSup_.getFprep().checkExistingWritableDirectory(targetDir_)) {          
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
          }
          nos_ = new DirectoryNamedOutputStreamSource(targetDir_);
          wantHtmlSkeleton_ = true;
          intersectionMap_ = null;
          publishKeys_ = null;
        } else {
          wantHtmlSkeleton_ = false;
        }
      }
      
      zoom_ = appState_.getZoomTarget().getZoomFactor();
      holdKey_ = appState_.getGenome();

      NavTree navTree = dacx_.getGenomeSource().getModelHierarchy();
      List<String> ordered = navTree.getPreorderListing(skipRoot);
      Iterator<String> oit = ordered.iterator();
      support_ = null;
      if (oit.hasNext()) {
        String gkey = oit.next();
        String layoutID = appState_.getLayoutMgr().getLayout(gkey);
        appState_.setGraphLayout(layoutID);
        // Catch selection clear to undo queue the first time through:        
        support_ = new UndoSupport(appState_, "undo.selection");
        appState_.setGenome(gkey, support_, dacx_);
        WebRunner runner = new WebRunner(appState_, nos_, skipRoot, gkey, wantHtmlSkeleton_, intersectionMap_, publishKeys_, dacx_);
        
        BackgroundWorkerClient bwc;     
        if (!appState_.isHeadless()) { // not headless, true background thread
          bwc = new BackgroundWorkerClient(appState_, this, runner, "webPublish.waitTitle", "webPublish.wait", support_, true);    
        } else { // headless; on this thread
          bwc = new BackgroundWorkerClient(appState_, this, runner, support_);
        }        
        runner.setClient(bwc);
        bwc.launchWorker();
      }
      // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((appState_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
                                                                                       : DialogAndInProcessCmd.Progress.DONE_ON_THREAD, this); // Done    
      return (daipc);  
    }
 
    public boolean handleRemoteException(Exception remoteEx) {
      if (remoteEx instanceof IOException) {
        myLsSup_.getFprep().displayFileOutputError();
        return (true);
      }
      return (false);
    }    
        
    public void cleanUpPreEnable(Object result) {
      String layoutID = appState_.getLayoutMgr().getLayout(holdKey_);
      appState_.setGraphLayout(layoutID);
      appState_.setGenomeForUndo(holdKey_, dacx_);
      appState_.getZoomTarget().setZoomFactor(zoom_);
      return;
    }
    
    public void handleCancellation() {
      String layoutID = appState_.getLayoutMgr().getLayout(holdKey_);
      appState_.setGraphLayout(layoutID);
      appState_.setGenomeForUndo(holdKey_, dacx_);
      if (support_ != null) {
        support_.finish();
      }
      appState_.getZoomTarget().setZoomFactor(zoom_);
      return;
    } 
    
    public void cleanUpPostRepaint(Object result) {
      return;
    }     
    
    protected boolean checkGuts(CheckGutsCache cache) {
      return (cache.moreThanOneModel());
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private static class WebRunner extends BackgroundWorker {
    
    private NamedOutputStreamSource streamSrc_;
    private boolean skipRoot_;
    private String topID_;
    private BTState myAppState_;
    private Map<WebPublisher.ModelScale, ViewExporter.BoundsMaps> mapRepository_;
    private boolean needHtmlSkeleton_;
    private List<WebPublisher.ModelScale> keyList_;
    private DataAccessContext myDacx_;
       
    public WebRunner(BTState appState, NamedOutputStreamSource streamSrc,
                     boolean skipRoot, String topID, 
                     boolean needHtmlSkeleton, Map<WebPublisher.ModelScale, ViewExporter.BoundsMaps> mapRepository, 
                     Set<WebPublisher.ModelScale> publishKeys, DataAccessContext dacx) {
      super(null);
      streamSrc_ = streamSrc;
      myAppState_ = appState;
      skipRoot_ = skipRoot;
      topID_ = topID; 
      needHtmlSkeleton_ = needHtmlSkeleton;
      mapRepository_ = mapRepository;
      myDacx_ = dacx;
      if (publishKeys == null) {
        keyList_ = buildFullKeyList();
      } else {
        keyList_ = new ArrayList<WebPublisher.ModelScale>(publishKeys);
      }
    }
    
    public Object runCore() throws AsynchExitRequestException {      
      SUPanel sup = myAppState_.getSUPanel();
      HashMap<WebPublisher.ModelScale, ViewExporter.BoundsMaps> boundsMap = new HashMap<WebPublisher.ModelScale, ViewExporter.BoundsMaps>();
      Map<Integer, Double> scaleMap = buildScaleMap();
      
      int total = keyList_.size();
      int done = 0;
      
      try {
        String lastKey = null;
        Iterator<WebPublisher.ModelScale> oit = keyList_.iterator();
        while (oit.hasNext()) {
          WebPublisher.ModelScale scaleKey = oit.next();
          String gkey = scaleKey.getModelID();
          if ((lastKey == null) || !gkey.equals(lastKey)) {
            String layoutID = myAppState_.getLayoutMgr().getLayout(gkey);
            myAppState_.setGraphLayout(layoutID);
            myAppState_.setGenomeForUndo(gkey, myDacx_);
          }
          done = runForScaleKey(sup, scaleKey, scaleMap, boundsMap, done, total);      
        }
        
        if (needHtmlSkeleton_) {
          // HTML just cares about note bounds:
          HashMap<WebPublisher.ModelScale, Map<String, Rectangle>> justNotes = new HashMap<WebPublisher.ModelScale, Map<String, Rectangle>>();
          Iterator<WebPublisher.ModelScale> bmit = boundsMap.keySet().iterator();
          while (bmit.hasNext()) {
            WebPublisher.ModelScale key = bmit.next();
            ViewExporter.BoundsMaps supMaps =  boundsMap.get(key);
            justNotes.put(key, supMaps.noteBounds);
          }
          WebPublisher pub = new WebPublisher(myDacx_);  
          pub.printHTML(streamSrc_, justNotes, skipRoot_, topID_);
        } else {
          mapRepository_.clear();
          mapRepository_.putAll(boundsMap);
        }
      } catch (IOException ex) {
        stashException(ex);
      }
      return (null);
    }
    
    private Map<Integer, Double> buildScaleMap() {          
      HashMap<Integer, Double> retval = new HashMap<Integer, Double>();
      retval.put(new Integer(WebPublisher.ModelScale.SMALL), new Double(0.38));
      retval.put(new Integer(WebPublisher.ModelScale.MEDIUM), new Double(0.5));
      retval.put(new Integer(WebPublisher.ModelScale.LARGE), new Double(0.62));
      retval.put(new Integer(WebPublisher.ModelScale.JUMBO), new Double(1.0));
      return (retval);
    }

    private int runForScaleKey(SUPanel sup, WebPublisher.ModelScale scaleKey, Map<Integer, Double> scaleMap,
                               Map<WebPublisher.ModelScale, ViewExporter.BoundsMaps> boundsMap, int done, int total) throws AsynchExitRequestException, IOException {          
      Double zoomFacObj = scaleMap.get(new Integer(scaleKey.getSize()));
      OutputStream namedStream = streamSrc_.getNamedStream(scaleKey.getFileName());
      ViewExporter.BoundsMaps smallBounds = sup.exportToStream(namedStream, true, "PNG", null, zoomFacObj.doubleValue(), null, myAppState_);
      boundsMap.put(scaleKey, smallBounds);
      double currProg = ((double)++done / (double)total);
      boolean keepGoing = updateProgress((int)(currProg * 100.0));
      if (!keepGoing) {
        throw new AsynchExitRequestException();
      }
      return (done);          
    }
    
    private List<WebPublisher.ModelScale> buildFullKeyList() {              
      ArrayList<WebPublisher.ModelScale> retval = new ArrayList<WebPublisher.ModelScale>();     
      NavTree navTree = myDacx_.getGenomeSource().getModelHierarchy();
      List<String> ordered = navTree.getPreorderListing(skipRoot_);
      Iterator<String> oit = ordered.iterator();
      while (oit.hasNext()) {
        String gkey = oit.next();
        retval.add(new WebPublisher.ModelScale(gkey, WebPublisher.ModelScale.SMALL));
        retval.add(new WebPublisher.ModelScale(gkey, WebPublisher.ModelScale.MEDIUM));      
        retval.add(new WebPublisher.ModelScale(gkey, WebPublisher.ModelScale.LARGE));                  
        retval.add(new WebPublisher.ModelScale(gkey, WebPublisher.ModelScale.JUMBO));
      }
      return (retval);
    }
    
    public Object postRunCore() {
      return (null);
    }
  }
}
