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

package org.systemsbiology.biotapestry.cmd.flow.display;

import java.awt.Rectangle;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.analysis.AllPathsResult;
import org.systemsbiology.biotapestry.analysis.Path;
import org.systemsbiology.biotapestry.analysis.PathAnalyzer;
import org.systemsbiology.biotapestry.analysis.SignedTaggedLink;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.FreestandingSourceBundle;
import org.systemsbiology.biotapestry.db.LocalGenomeSource;
import org.systemsbiology.biotapestry.db.LocalLayoutSource;
import org.systemsbiology.biotapestry.db.LocalWorkspaceSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Workspace;
import org.systemsbiology.biotapestry.genome.AbstractGenome;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.nav.LocalGroupSettingSource;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.AllPathsLayoutBuilder;
import org.systemsbiology.biotapestry.ui.FreezeDriedOverlayOracle;
import org.systemsbiology.biotapestry.ui.GenomePresentation;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.LocalDispOptMgr;
import org.systemsbiology.biotapestry.ui.ViewExporter;
import org.systemsbiology.biotapestry.ui.ZoomTargetSupport;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
 
/****************************************************************************
**
** Creates path genome models
*/

public class PathGenerator extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private static final int CRAZY_DEPTH_ = 30;
  
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
  
  public PathGenerator(BTState appState) {
    super(appState);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
  
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) { 
    return (false);
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
      if (last == null) {
        throw new IllegalStateException();
      } else {
        StepState ans = (StepState)last.currStateX;
        if (ans.getNextStep().equals("stepGeneratePathModel")) {
          next = ans.stepGeneratePathModel();
        } else {
          throw new IllegalStateException();
        }
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
        
  public static class StepState implements DialogAndInProcessCmd.SrcTrgCmdState {
     
    private Genome targetGenome_;
    private String nextStep_;    
    private String srcID_;
    private String targID_;
    private int depth_;
    private String trueGenomeID_;
    private BTState appState_;
    private boolean showQPCR_;
    private boolean longerOK_;
    private LocalGenomeSource pSrc_;
    private DataAccessContext rcx_;
    
    private int maxDepth_ = -1;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, DataAccessContext dacx) {
      
      appState_ = appState;
      trueGenomeID_ = dacx.getGenomeID();
      targetGenome_ = dacx.getGenome();
      
      showQPCR_ = appState_.getIsEditor() && appState_.getDB().getPertData().haveData();
      nextStep_ = "stepGeneratePathModel";
    }
  
    /***************************************************************************
    **
    ** Preload init
    */ 
      
    public void setSourceTargAndDepth(String src, String trg, int depth, boolean longerOK) {
      srcID_ = src;
      targID_ = trg;
      depth_ = depth;
      longerOK_ = longerOK;
      return;
    } 
    
    /***************************************************************************
    **
    ** Used for legacy worksheet application. Needs to be ported! BOGUS!
    */ 
      
    public void setLegacyData(LocalGenomeSource pSrc, String genomeID, String src, String trg, int depth) { 
      pSrc_ = pSrc;
      trueGenomeID_ = genomeID;
      targetGenome_ = pSrc_.getGenome(trueGenomeID_);
      srcID_ = src;
      targID_ = trg;
      depth_ = depth;
      longerOK_ = false;
      showQPCR_ = false;
      return;
    } 
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////  

    /***************************************************************************
    **
    ** Core operations for path model generation
    */  
   
    private DialogAndInProcessCmd stepGeneratePathModel() {
      
      //
      // Build a local genome source that can support our operations:
      //
      
      HashMap<String, String> groupIDMap = new HashMap<String, String>();
      LocalGenomeSource origGSrc = (pSrc_ == null) ? genLGS(trueGenomeID_, groupIDMap, true) : pSrc_; 
      String phonyGenomeID = (pSrc_ == null) ? mapGITarget(trueGenomeID_) : trueGenomeID_;
  
      //
      // Generate the genome and return the result:
      //
      
      LocalGenomeSource currGSrc = new LocalGenomeSource(new DBGenome(appState_, "foo", "bar"), new DBGenome(appState_, "foo", "bar"));
      LocalLayoutSource lls = new LocalLayoutSource(new Layout(appState_,"__BOGUSKEY__", "bar"), currGSrc);
      LocalWorkspaceSource lws = new LocalWorkspaceSource();
      LocalDispOptMgr llbs = new LocalDispOptMgr(appState_.getDisplayOptMgr().getDisplayOptions().clone());
            
      rcx_ = new DataAccessContext(null, null, false, false, 0.0,
                                  appState_.getFontMgr(), llbs, 
                                  appState_.getFontRenderContext(), currGSrc, appState_.getDB(), appState_.isWebApplication(), 
                                  new FullGenomeHierarchyOracle(currGSrc, lls), appState_.getRMan(), 
                                  new LocalGroupSettingSource(), lws, lls, 
                                  new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null),
                                  appState_.getDB(), appState_.getDB()                                  
      );
      GenomePresentation myGenomePre = new GenomePresentation(appState_, false, 1.0, false, rcx_);
      ZoomTargetSupport zts = new ZoomTargetSupport(appState_, myGenomePre, null, rcx_);
  
      Results res = generateGenome(trueGenomeID_, phonyGenomeID, srcID_, targID_, depth_, groupIDMap, origGSrc, currGSrc, longerOK_,appState_.isWebApplication(), llbs);
      
      Map<String, Object> modelMap = new HashMap<String, Object>();
      if (!res.haveAPath) {
        modelMap.put("havePath", Boolean.valueOf(false));
        modelMap.put("maxPath", new Integer((longerOK_) ? CRAZY_DEPTH_ : res.depth));
        modelMap.put("neverAPath", longerOK_);
        modelMap.put(
    		"pathMsg", 
    		!longerOK_ ? appState_.getRMan().getString("showPath.noPathAtDepth") : 
    		appState_.getRMan().getString("showPath.noPath").replaceAll("\\{0\\}",Integer.toString(CRAZY_DEPTH_))
		);
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this, modelMap));
      }  

      lls.setRootLayout(res.aprLayout);
      rcx_.setLayout(res.aprLayout);
      rcx_.setGenome(rcx_.getGenomeSource().getGenome(phonyGenomeID));
      Rectangle modelSize = zts.getCurrentBasicBounds(true, false, ZoomTargetSupport.NO_MODULES);    
      Workspace boundWs = Workspace.setToFreeModelBounds(modelSize);
      lws.simpleSetWorkspace(boundWs);
  
      //
      // Build the StrungOutPaths, which include a string descriptor and a Map of all 
      // link segment IDs for the path:
      //
      
      ArrayList<StrungOutPath> sopl = new ArrayList<StrungOutPath>();
      Iterator<Path> pit = res.paths.iterator();
      while (pit.hasNext()) {
        Path path = pit.next();
        String display = path.toString();
        HashMap<String, Set<LinkSegmentID>> allPathSegs = new HashMap<String, Set<LinkSegmentID>>();
        for (SignedTaggedLink stl : path.getLinks()) {
          String linkID = stl.getTag();
          LinkProperties lp = res.aprLayout.getLinkProperties(linkID);
          allPathSegs.put(linkID, new HashSet<LinkSegmentID>(lp.getBusLinkSegmentsForOneLink(res.rcx, linkID)));
        }
        sopl.add(new StrungOutPath(path, display, allPathSegs));
      }

      modelMap.put("pathList", sopl);
      modelMap.put("selectedPath", 0);
      
      
      if (showQPCR_) {
        TipSource tips = generateTips(res.paths, res.aprLayout);
        modelMap.put("pathTips", tips);
      }
      
      modelMap.put("havePath", Boolean.valueOf(res.haveAPath));
            
      if (res.haveAPath) {
    	  modelMap.put("maxPath", res.depth);
    	  if(maxDepth_ < 0 && longerOK_) {
    		  maxDepth_ = Math.max(10, res.depth + 4);
    	  }
    	  modelMap.put("maxDepth", maxDepth_);
      } else {
    	  modelMap.put("maxDepth", new Integer((longerOK_) ? CRAZY_DEPTH_ : res.depth));
    	  modelMap.put("maxPath", new Integer((longerOK_) ? CRAZY_DEPTH_ : res.depth));
      }
      modelMap.put("longerOK", longerOK_);
      modelMap.put("neverAPath",false);
      
      if (!rcx_.forWeb) {
        FreestandingSourceBundle fsb = new FreestandingSourceBundle(currGSrc, lls, lws);    
        modelMap.put("pathModel", fsb);
        modelMap.put("pathModelID", phonyGenomeID);
        modelMap.put("pathLayoutID", res.aprLayout.getID());
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this, modelMap));
      } else {
        try {
          ViewExporter vexp = new ViewExporter(myGenomePre, zts);
          ViewExporter.StateForDraw sfd = new ViewExporter.StateForDraw(appState_, rcx_, null, null,
                                                                        null, null, null, null, appState_.getFontMgr()); 
          Map<String, Object> emg = vexp.exportMapGuts(true, 1.0, null, sfd);
          modelMap.put("pathRenderingMap", emg);
        } catch (IOException ioe) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
        }
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this, modelMap));      
      }
    }

    /***************************************************************************
    **
    ** Run the path analysis
    */    
    
    private Results generateGenome(String trueGenomeID, String phonyGenomeID, String sourceID, String targetID, 
                                  int depth, Map<String, String> groupIDMap, 
                                  LocalGenomeSource gSrc, LocalGenomeSource fillIn, boolean longerOK,boolean isWebApp, LocalDispOptMgr llbs) {
          	
      if ((sourceID == null) || (targetID == null)) {
        return (new Results(null, null, Integer.MAX_VALUE, false, null));       
      }  
      //
      // Look for a path at the specified length. If caller has specified, we will keep looking
      // at longer paths if the specified length produces no results:
      //
      
      AllPathsResult result = null;
      int workingDepth = Integer.MAX_VALUE;
      for (int i = depth; i < CRAZY_DEPTH_; i++) {
        result = new PathAnalyzer().getAllPaths(trueGenomeID, sourceID, targetID, i,  (pSrc_ == null) ? appState_.getDB() : pSrc_);
        List<Path> startList = result.getPaths();
        if (!startList.isEmpty()) {
          workingDepth = i;        
          break;
        }
        if (!longerOK) {
          break;
        }
      }
      if (workingDepth == Integer.MAX_VALUE) {
        return (new Results(null, null, Integer.MAX_VALUE, false, null));       
      }

      LocalGenomeSource reduced = gSrc.reduceToResult(result, phonyGenomeID, groupIDMap);
      fillIn.dropAll();
      fillIn.install(reduced);
      List<Path> paths = result.getPaths();
      Collections.sort(paths);

      Genome useGenome = Layout.determineLayoutTarget(fillIn, phonyGenomeID);
      Layout retval = new Layout(appState_, "__BOGUSKEY__", useGenome.getID());
      LocalLayoutSource lls = new LocalLayoutSource(retval, reduced);
      Layout colorRef = appState_.getLayoutForGenomeKey(trueGenomeID_);
      DataAccessContext rcx = new DataAccessContext(useGenome, retval, false, false, 0.0,
                                  appState_.getFontMgr(), llbs, 
                                  appState_.getFontRenderContext(), fillIn, appState_.getDB(), isWebApp, 
                                  new FullGenomeHierarchyOracle(fillIn, lls), appState_.getRMan(), 
                                  new LocalGroupSettingSource(), appState_.getDB(), lls, 
                                  new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null), 
                                  appState_.getDB(), appState_.getDB()
      );

      new AllPathsLayoutBuilder(appState_).buildNeighborLayout(result, rcx, colorRef);     
      return (new Results(paths, rcx.getLayout(), workingDepth, true, rcx));    
    }
    
    /***************************************************************************
    **
    ** Create a local genome source that will be able to support the desired exported path model
    */  
    
    private LocalGenomeSource genLGS(String giKey, Map<String, String> groupIDMap, boolean reduced) {  
      Database db = appState_.getDB();
      Genome keyed = db.getGenome(giKey);
      List<Genome> keyedCopies = new ArrayList<Genome>();
      DBGenome baseCopy;
      if (keyed instanceof DBGenome) {
        Genome keyedCopy = ((DBGenome)keyed).getBasicGenomeCopy();
        baseCopy = (DBGenome)keyedCopy;
        keyedCopies.add(keyedCopy);
      } else {
        GenomeInstance kgi = (GenomeInstance)keyed;
        GenomeInstance kgirt = kgi.getVfgParentRoot();
        GenomeInstance keyedCopyGI;
        if (kgirt == null) {
          keyedCopyGI = kgi.getBasicGenomeCopy(groupIDMap);
        } else {
          HashSet<String> keepLinks = new HashSet<String>();
          Iterator<Linkage> kgilit = kgi.getLinkageIterator();
          while (kgilit.hasNext()) {
            keepLinks.add(kgilit.next().getID());  
          }
          
          // FIX ME NOW  
          UiUtil.fixMePrintout("This is where we lose VFN inactivity level. Issue 78");
          keyedCopyGI = kgirt.getBasicGenomeCopy(groupIDMap, keepLinks); 
        }    
        baseCopy = ((DBGenome)db.getGenome()).getBasicGenomeCopy();
  
        keyedCopies.add(baseCopy);
        keyedCopies.add(keyedCopyGI);
      }
      LocalGenomeSource retval = new LocalGenomeSource(baseCopy, keyedCopies);
      int numKeyed = keyedCopies.size();
      for (int i = 0; i < numKeyed; i++) {
        AbstractGenome keyedCopy = (AbstractGenome)keyedCopies.get(i);
        keyedCopy.setGenomeSource(retval);
      }     
      return (retval);
    }
    
    /***************************************************************************
    **
    ** bundle up LGS creation:
    */  
    
    private String mapGITarget(String giKey) {  
      Database db = appState_.getDB();
      Genome keyed = db.getGenome(giKey);
      if (keyed instanceof DBGenome) {
        return (giKey);
      } else {
        GenomeInstance kgi = (GenomeInstance)keyed;
        GenomeInstance kgirt = kgi.getVfgParentRoot();
        if (kgirt == null) {
          return (giKey);
        } else {
          return (kgirt.getID());
        }
      }
    }

    /***************************************************************************
    **
    ** Get the tool tips
    */
     
    private TipSource generateTips(List<Path> paths, Layout layout) {
      if (!showQPCR_) {
        throw new IllegalStateException();
      }
      
      Map<String, String> nodeTips = new HashMap<String, String>();
      Map<String, String> linkTips = new HashMap<String, String>(); 
      Map<String, String> ambiguousTips = new HashMap<String, String>();
      Map<String, Map<LinkSegmentID, Set<String>>> linkSegMaps = new HashMap<String, Map<LinkSegmentID, Set<String>>>();
      Map<String, String> linkToSrc = new HashMap<String, String>();

      int numPath = paths.size();
      for (int i = 0; i < numPath; i++) {
        Path path = paths.get(i);
        Iterator<SignedTaggedLink> lit = path.pathIterator();
        while (lit.hasNext()) {
          SignedTaggedLink link = lit.next();
          String id = link.getTag();
          linkTips.put(id, generateLinkTip(id));
          String src = link.getSrc();
          linkToSrc.put(id, src);
          if (nodeTips.get(src) == null) {
            nodeTips.put(src, generateNodeTip(src));
            LinkProperties forSrc = layout.getLinkProperties(id);       
            Map<LinkSegmentID, Set<String>> stl = forSrc.getFullSegmentToLinkMap();
            HashSet<String> allLinks = new HashSet<String>();
            Iterator<Set<String>> alit = stl.values().iterator();
            while (alit.hasNext()) {
              allLinks.addAll(alit.next());
            }
            LinkSegmentID lsid = LinkSegmentID.buildIDForStartDrop();
            stl.put(lsid, allLinks);
            linkSegMaps.put(src, stl);
          }
          if (ambiguousTips.get(src) == null) {
            ambiguousTips.put(src, generateNonSpecificTip(src));
          }
          if (!lit.hasNext()) {
            String trg = link.getTrg();
            if (nodeTips.get(trg) == null) {
              nodeTips.put(trg, generateNodeTip(trg));
            }
          }
        }
      }
      return (new TipSource(nodeTips, linkTips, ambiguousTips, linkSegMaps, linkToSrc));
    }
    
    /***************************************************************************
    **
    ** Get a tool tip
    */
     
    private String generateLinkTip(String linkID) {
      ResourceManager rMan = appState_.getRMan();
      Linkage link = targetGenome_.getLinkage(linkID);  // This could be ANY link through a bus segment
      PerturbationData pd = appState_.getDB().getPertData();
      String tip = pd.getToolTip(GenomeItemInstance.getBaseID(link.getSource()), GenomeItemInstance.getBaseID(link.getTarget()), null);
      if ((tip == null) || tip.trim().equals("")) {
        return (rMan.getString("pathpanel.noqpcr"));
      }
      return (tip);
    } 
    
    /***************************************************************************
    **
    ** Get a tool tip
    */
     
    private String generateNodeTip(String nodeID) {
      Node node = targetGenome_.getNode(nodeID);
      if (node != null) {
        return (node.getName());
      }
      return (null);
    } 
    
    /***************************************************************************
    **
    ** Get a tool tip
    */
     
    private String generateNonSpecificTip(String srcID) {
      Node src = targetGenome_.getNode(srcID);
      String format = appState_.getRMan().getString("pathpanel.source");
      String desc = MessageFormat.format(format, new Object[] {src.getName()}); 
      return (desc);
    } 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Holds onto results
  */  
  
  private static class Results {
    List<Path> paths;  
    Layout aprLayout;
    int depth;
    boolean haveAPath;
    DataAccessContext rcx;
    
    Results(List<Path> paths, Layout aprLayout, int depth, boolean haveAPath, DataAccessContext rcx) {
      this.paths = paths;
      this.aprLayout = aprLayout;
      this.depth = depth;     
      this.haveAPath = haveAPath;
      this.rcx = rcx;
    }
  }

  /***************************************************************************
  **
  ** Holds onto tooltips
  */  
  
  public static class TipSource {
    public Map<String, String> nodeTips;
    public Map<String, String> linkTips;
    public Map<String, String> ambiguousTips;
    public Map<String, Map<LinkSegmentID, Set<String>>> linkSegMaps;
    public Map<String, String> linkToSrc;
    
    public TipSource(Map<String, String> nodeTips, Map<String, String> linkTips, 
                     Map<String, String> ambiguousTips, Map<String, 
                     Map<LinkSegmentID, Set<String>>> linkSegMaps, Map<String, String> linkToSrc) {
      this.nodeTips = nodeTips;
      this.linkTips = linkTips;
      this.ambiguousTips = ambiguousTips; 
      this.linkSegMaps = linkSegMaps;
      this.linkToSrc = linkToSrc;
    }
  }
  
  /***************************************************************************
  **
  ** Bundles the display string along with the path
  */  
  
  public static class StrungOutPath extends Path {
    public String display;
    public Map<String, Set<LinkSegmentID>> allSegs;
    
    public StrungOutPath(Path other, String display, Map<String, Set<LinkSegmentID>> allSegs) {
      super(other);
      this.display = display;
      this.allSegs = allSegs;
    }
  }
}
