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


package org.systemsbiology.biotapestry.ui.layouts;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import org.systemsbiology.biotapestry.analysis.CenteredGridElement;
import org.systemsbiology.biotapestry.analysis.GridElement;
import org.systemsbiology.biotapestry.analysis.GridGrower;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutRubberStamper;
import org.systemsbiology.biotapestry.cmd.flow.link.LinkSupport;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.db.LocalLayoutSource;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.LinkBusDrop;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NetModuleLinkageProperties;
import org.systemsbiology.biotapestry.ui.NetModuleShapeFixer;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.freerender.GroupFree;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;
import org.systemsbiology.biotapestry.util.HandlerAndManagerSource;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Handles specialty layout operations
*/

public class SpecialtyLayoutEngine {

  /***************************************************************************
  **
  ** Existing layout templates
  */ 
  
  public enum SpecialtyType {
    STACKED("stacked"),
    DIAGONAL("diagonal"),
    ALTERNATING("worksheet"),
    HALO("halo"),
    ;
  
    private String tag_;
    
    SpecialtyType(String tag) {
      this.tag_ = tag;  
    }
    
    public EnumChoiceContent<SpecialtyType> generateCombo(HandlerAndManagerSource hams) {
      return (new EnumChoiceContent<SpecialtyType>(hams.getRMan().getString("layoutParam." + tag_), this));
    }
  
    public static Vector<EnumChoiceContent<SpecialtyType>> getChoices(HandlerAndManagerSource hams, boolean skipHalo) {
      Vector<EnumChoiceContent<SpecialtyType>> retval = new Vector<EnumChoiceContent<SpecialtyType>>();
      for (SpecialtyType st: values()) {
        if (skipHalo && st.equals(HALO)) {
          continue;
        }
        retval.add(st.generateCombo(hams)); 
      }
      return (retval);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private static final int MODULE_LINK_PAD_ = 8;
  private static final int MODULE_EXPAND_LIMIT_ = 50;
  private static final int MODULE_NOPROGRESS_LIMIT_ = 10;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  //
  // Seeing cases where doing a layout while at very wide zooms causes
  // text boxes to be collapsed.
  
  private FontRenderContext fixedFrc_;
  
  private List<GenomeSubset> subsetListIn_; // List of genome subsets   
  private StaticDataAccessContext rcx_;
  private StaticDataAccessContext workingRcx_;
  private SpecialtyLayout specL_;
  private NetModuleLinkExtractor.SubsetAnalysis sa_;
  private Point2D fullGenomeCenter_;                                                 
  private SpecialtyLayoutEngineParams params_;
  private boolean fromScratch_;
  private Layout.PadNeedsForLayout padNeedsForLayout_;
  private Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery_;
  private boolean hideNames_;
  
  private List<GenomeSubset> topoSortedSubsetList_;
  private List<SpecialtyInstructions> propList_;
  private Map<String, Map<Integer, LinkBundleSplicer.SpliceSolution>> splicePlans_;
  private Map<String, BusProperties.RememberProps> rememberProps_; 
  private HashMap<Point2D, Point2D> shiftedIMP_;                                                                
  private Vector2D shiftedAmount_;
  private int overlayOption_;
  private HashSet<String> newLinks_;
  private ArrayList<Map<String, SpecialtyLayoutLinkData>> forRecoveriesOut_;
  private HashMap<String, SpecialtyLayoutLinkData> alienSources_ ;
  private HashMap<String, SpecialtyLayoutLinkData> srcToBetween_;
  private Map<Integer, LinkBundleSplicer.SpliceSolution> nonOverlaySolutions_;
  private Rectangle nonOverlaySpliceBorders_;
  private HashSet<String> ultimateFailures_;
  
  private Layout.SupplementalDataCoords sdc_;
  private GlobalSLEState gss_;
 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////


  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VISIBLE MEMBERS
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
  
  
  public SpecialtyLayoutEngine(List<GenomeSubset> subsetListIn,
                               StaticDataAccessContext rcx, SpecialtyLayout specL,
                               NetModuleLinkExtractor.SubsetAnalysis sa,
                               Point2D fullGenomeCenter,
                               SpecialtyLayoutEngineParams params,                               
                               boolean fromScratch,
                               boolean hideNames) {
    
    fixedFrc_ = new FontRenderContext(null, true, true);
    shiftedIMP_ = new HashMap<Point2D, Point2D>();
    shiftedAmount_ = new Vector2D(0.0, 0.0);
    subsetListIn_ = subsetListIn;                                   
    rcx_ = rcx;
    specL_ = specL;
    sa_ = sa;
    fullGenomeCenter_ = fullGenomeCenter;                                          
    params_ = params;
    fromScratch_ = fromScratch;
    hideNames_ = hideNames;
    newLinks_ = new HashSet<String>();
    forRecoveriesOut_ = new ArrayList<Map<String, SpecialtyLayoutLinkData>>();
    alienSources_ = new HashMap<String, SpecialtyLayoutLinkData>();
    srcToBetween_ = new HashMap<String, SpecialtyLayoutLinkData>();
    workingRcx_ = null;
 
    ultimateFailures_ = new HashSet<String>();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get layout for type
  */   
  
  public static SpecialtyLayout getLayout(SpecialtyType st, StaticDataAccessContext rcx) {
    switch (st) {
      case STACKED:
        return (new StackedBlockLayout(rcx));
      case DIAGONAL:
        return (new WorksheetLayout(true, rcx));
      case ALTERNATING:
        return (new WorksheetLayout(false, rcx));
      case HALO:
        return (new HaloLayout(rcx));
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Methods for getting prepared and passthrough data
  */  
  
  public List<Map<String, SpecialtyLayoutLinkData>> getForRecoveriesOut() {
    return (forRecoveriesOut_);
  }
  
  public Layout getWorkingLayout() {
    return (workingRcx_.getCurrentLayout());
  }
  
  public Set<String> getUltimateFailures() {
    return (ultimateFailures_);
  }
  
  public Map<String, SpecialtyLayoutLinkData> getAlienSources() {
    return (alienSources_);
  }
    
   public Map<String, SpecialtyLayoutLinkData> getSrcToBetween() {
    return (srcToBetween_);
  }
  
  public Set<String> getNewLinks() {
    return (newLinks_);
  }
   
  public List<GenomeSubset> getSortedGenomeSubsets() {
    return (topoSortedSubsetList_);
  }
  
  public List<SpecialtyInstructions> getInstructions() {
    return (propList_);
  }
 
  public Layout.PadNeedsForLayout getPadNeedsForLayout() {
    return (padNeedsForLayout_);
  }

  public Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> getModuleShapeRecovery() {
    return (moduleShapeRecovery_);
  }
  
  public int getOverlayOption() {
    return (overlayOption_);
  }
  
  public Layout getLayout() {
    return (rcx_.getCurrentLayout());
  }
  
  public StaticDataAccessContext getRcx() {
    return (rcx_);
  }
  
  public StaticDataAccessContext getWorkingRcx() {
    return (workingRcx_);
  }
  
  public FontRenderContext getFontRenderContext() {
    return (fixedFrc_);
  } 
        
  public Map<String, BusProperties.RememberProps> getRememberProps() {
    return (rememberProps_);
  }
      
  public Map<String, Map<Integer, LinkBundleSplicer.SpliceSolution>> getSplicePlans() {
    return (splicePlans_);
  }
  
  public Map<Integer, LinkBundleSplicer.SpliceSolution> getNonOverlaySplicePlans() {
    return (nonOverlaySolutions_);
  }
    
  public Vector2D getShiftedAmount() {
    return (shiftedAmount_);
  }
  
  public Map<Point2D, Point2D> getModLinkCornerMoves() {
    return (shiftedIMP_);
  }
  
  public Layout.SupplementalDataCoords getSupplementalDataCoords() {
    return (sdc_);
  }
   
  public Point2D getFullGenomeCenter() {
    return (fullGenomeCenter_);
  }
  
  public NetModuleLinkExtractor.SubsetAnalysis getSubsetAnalysis() {
    return (sa_);
  }
  
  public boolean doFromScratch() {
    return (fromScratch_);
  }
  
  public boolean doHideNames() {
    return (hideNames_);
  }
  
  public GlobalSLEState getGSS() {
    return (gss_);
  }
  
  /***************************************************************************
  **
  ** Setup data
  */  
  
  
  public void setModuleRecoveryData(Layout.PadNeedsForLayout padNeedsForLayout,
                                    Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery) {
      
    padNeedsForLayout_ = padNeedsForLayout;
    moduleShapeRecovery_ = moduleShapeRecovery;
    return;
  }

  /***************************************************************************
  **
  ** Do autolayout
  */  
 
  public LinkRouter.RoutingResult specialtyLayout(UndoSupport support, BTProgressMonitor monitor, 
                                                  double startFrac, double maxFrac)
                                                  throws AsynchExitRequestException {

    //
    // Recover geometry and primary/other subset data for each source:
    //

    Layout.OverlayKeySet loModKeys = null;
    if (padNeedsForLayout_ != null) {    
      loModKeys = padNeedsForLayout_.getFullModuleKeys();
    }
    
    int numSub = subsetListIn_.size();   
    if (numSub == 0) {
      return (new LinkRouter.RoutingResult());
    }
    
   
    //
    // Work with the list of subsets in an order that allows the source
    // modules to set the source link order first.  Note that this is essential
    // even if there is only one subset, as there are other initializations occurring.
    //
    
    topoSortedSubsetList_ = sa_.topoReorder(subsetListIn_);
       
    //
    // Only need to do this stuff once:
    //
    
    GenomeSubset firstSubset = subsetListIn_.get(0);
    String overlayKey = firstSubset.getOverlayID();
      
    //
    // We work exclusively with a copy that is modified and installed by the add commands
    //
       
    workingRcx_ = new StaticDataAccessContext(rcx_);
    workingRcx_.setLayout(new Layout(rcx_.getCurrentLayout()));
    workingRcx_.setLayoutSource(new LocalLayoutSource(workingRcx_.getCurrentLayout(), workingRcx_.getGenomeSource()));
    workingRcx_.setFrc(fixedFrc_);
    
    sdc_ = workingRcx_.getCurrentLayout().getSupplementalCoords(workingRcx_, loModKeys);    
    rememberProps_ = workingRcx_.getCurrentLayout().buildRememberProps(workingRcx_);
   
    
    //
    // Dealing with Issue 211. Need to get core geometry up to speed if it is
    // inconsistent with visible geometry:
    //
    
    Set<String> nexp = sa_.needExpansion();
    if ((nexp != null) && !nexp.isEmpty()) {
      for (String netModKey : nexp) {
        workingRcx_.getCurrentLayout().sizeCoreToRegionBounds(workingRcx_.getOSO().getCurrentOverlay(), netModKey, workingRcx_);
      }
    }
  
    //
    // Handle overlay module geometry recovery:
    //

    overlayOption_ = params_.getOverlayRelayoutOption();    
    if (moduleShapeRecovery_ == null) {
      moduleShapeRecovery_ = workingRcx_.getCurrentLayout().getModuleShapeParams(workingRcx_, loModKeys, fullGenomeCenter_);
    }

    if (overlayOption_ == NetOverlayProperties.RELAYOUT_TO_MEMBER_ONLY) {     
      workingRcx_.getCurrentLayout().convertAllModulesToMemberOnly(loModKeys, workingRcx_);
    }
   
  
    //
    // Lots of layout operations want to know the sources and targets of nodes,
    // or the counts of links in and out of nodes. Given the way the info is stored,
    // those are expensive questions to do on a one-off basis for each node. Do the
    // calcs once, hold on to the info, and pass it around.
    //
  
    InvertedSrcTrg ist = new InvertedSrcTrg(rcx_.getCurrentGenome());   
    
    //
    // Now crank through each subset to fork and layout the nodes:
    //
   
    TreeMap<Integer, String> existingOrder = new TreeMap<Integer, String>();
    HashMap<String, PadCalculatorToo.PadResult> globalPadChanges = new HashMap<String, PadCalculatorToo.PadResult>();
    gss_ = new GlobalSLEState(globalPadChanges, ist);

    ArrayList<SpecialtyLayout> forkList = new ArrayList<SpecialtyLayout>(); 
    ArrayList<String> proc = new ArrayList<String>();
    for (int i = 0; i < numSub; i++) {
      GenomeSubset subset = topoSortedSubsetList_.get(i);
      proc.add(subset.getModuleID());
      SortedMap<Integer, String> customOrder = customOrderForTarget(existingOrder, subset);    
      Set<String> nodeSet = DataUtil.setFromIterator(subset.getNodeIterator());   
      
      //
      // Most cases have tons of pure target nodes. Links to those nodes 
      // cannot create cycles, so knowing these nodes helps improve my crappy
      // cycle-finding step.
      //
    
      HashSet<String> pureTargets = new HashSet<String>(nodeSet);
      Iterator<String> lit = subset.getLinkageIterator();
      while (lit.hasNext()) {
        String linkID = lit.next();
        Linkage link = rcx_.getCurrentGenome().getLinkage(linkID);
        String src = link.getSource();
        pureTargets.remove(src);
      }
        
      
      SpecialtyLayoutData sld = new SpecialtyLayoutData(subset, workingRcx_, params_, gss_, customOrder, pureTargets, nodeSet);     
      gss_.addData(sld);
            
      SpecialtyLayout forked = specL_.forkForSubset(sld);
      forkList.add(forked);
      
      if (!nodeSet.isEmpty()) {
        forked.layoutNodes(monitor);
      } else {
        return (new LinkRouter.RoutingResult(LinkRouter.LAYOUT_COULD_NOT_PROCEED)); 
      } 
 
      //
      // Merge together the source orders as they get built:
      //
      
      SortedMap<Integer, String> outOrder = sld.results.getSourceOrder();
      if (outOrder != null) {
        int keyBase = 0;
        if (!existingOrder.isEmpty()) {
          Integer last = existingOrder.lastKey();
          keyBase = last.intValue() + 1;
        }
        Iterator<Integer> ooit = outOrder.keySet().iterator();
        while (ooit.hasNext()) {
          Integer key = ooit.next();
          String srcID = outOrder.get(key);
          if (!existingOrder.containsValue(srcID)) {
            existingOrder.put(new Integer(key.intValue() + keyBase), srcID);
          }
        }
      }
      
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
    }
    
    //
    // Second pass routes (but does not build) the links:
    //

    for (int i = 0; i < numSub; i++) {
      SpecialtyLayout forked = forkList.get(i);
      forked.routeLinks(gss_, monitor);  
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
    }
    
    //
    // Third pass to assign colors. NOTE! Actual color operations are actually occurring in 
    // LayoutLinkSupport.doColorOperations() inside LayoutLinkSupport.runSpecialtyLinkPlacement()
    // at the very end. This is just transferring some parameters.
    //

    for (int i = 0; i < numSub; i++) {
      SpecialtyLayout forked = forkList.get(i);
      forked.assignColors(monitor);  
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
    }
  
    //
    // Forth pass to get results:
    //

    propList_ = new ArrayList<SpecialtyInstructions>();
    Iterator<SpecialtyLayoutData> dit = gss_.dataIterator();
    while (dit.hasNext()) {
      SpecialtyLayoutData sld = dit.next();
      propList_.add(sld.results);
      // Yes, it adds nulls to the propList; keeps it in sync with subset list!
      if (sld.results == null) {
        continue;
      }   
    }
               
    //
    // Fifth pass to pass source order into interModPaths results
    //
    
    int pSize = propList_.size();
    for (int i = 0; i < pSize; i++) {
      SpecialtyInstructions results = propList_.get(i);
      if (results == null) {
        continue;
      }    
      String moduleID = results.getModuleID();
      if (moduleID != null) {
        NetModuleLinkExtractor.ExtractResultForSource er4s = sa_.getExtractResultForSource(moduleID);
        if (er4s != null) {
          er4s.setSourceOrder(results.getSourceOrder());
        }
      }
    }  
    
               
    //
    // Sixth pass to generate boundaries:
    //

    for (int i = 0; i < pSize; i++) {
      SpecialtyInstructions results = propList_.get(i);
      if (results == null) {
        continue;
      }    
      //
      // Links have been routed (but not placed) by now.  We have generated boundary link
      // points for each subset. Figure out the bounds of that result so we can size up the
      // result, shift modules around, and chop up the existing link trees where we need 
      // to:
      // 
      results.setBoundsForLinks(sa_);
    }      

    //
    // If the layout was generated by an overlay, we want to resize the modules to contain the
    // intra-module links.  We also want to shift the modules around to handle size increases
    // resulting from the layout.  In the subset case, we do the relayout pretty much in-situ, 
    // growing the layout as needed (and we check below to see if the space is there to do this
    // without overlapping into existing layout).
    // With region-driven layouts, we don't want to do anything yet to shift stuff..
    //
    
    String grpID = firstSubset.getGroupID();     
    if (!firstSubset.isCompleteGenome() && (overlayKey != null)) {
      splicePlans_ = shiftTheModules(gss_, overlayKey, workingRcx_, monitor);
    } else if (grpID == null) { // Not region driven...
      ArrayList<Vector2D> shiftVecs = new ArrayList<Vector2D>();
      ArrayList<Map<String, SpecialtyLayoutLinkData>> shiftedCopies = new ArrayList<Map<String, SpecialtyLayoutLinkData>>();
      buildShiftedVecsAndCopies(gss_, shiftVecs, shiftedCopies, new HashMap<Object, GridElement>(), new HashMap<String, Rectangle>(), null);
      installShift(gss_, shiftVecs, new HashMap<Object, GridElement>(), false);
    }

    //
    // If working with a region layout, we may need to grow the region.  If so, we need to
    // expand the whole layout to accomodate the growth.  
    //   
  
    double currStart = startFrac;
    
    
    //
    // NOTE THE CONDITIONAL!  Following block is ONLY for region-only layouts!
    //
  
    Rectangle paddedBounds = null;
    if (!firstSubset.isCompleteGenome() && (grpID != null)) {
      GenomeInstance bgi = (GenomeInstance)rcx_.getCurrentGenome();
      LayoutRubberStamper lrs = new LayoutRubberStamper();
      HashSet<String> groups = new HashSet<String>();
      groups.add(grpID);
      double expandMax = currStart + ((maxFrac - currStart) * 0.20);
      LayoutOptions options = new LayoutOptions(rcx_.getLayoutOptMgr().getLayoutOptions());
      options.overlayCpexOption = overlayOption_;
      
      // Original region bounds:
      GroupFree renderer = new GroupFree();
      //
      // FIXME: Appears that having a region with weirdo paddings makes this whole thing work pretty funky.
      // Should look into the effect of visual bounds versus node bounds for region expansion stuff!
      //

      Rectangle origRegionBounds = renderer.getBounds(bgi.getGroup(grpID), workingRcx_, null, false);
      Rectangle labelBounds = renderer.getLabelBounds(bgi.getGroup(grpID), workingRcx_);
      HashMap<String, Map<String, LayoutRubberStamper.EdgeMove>> chopDeparts = new HashMap<String, Map<String, LayoutRubberStamper.EdgeMove>>();
      HashMap<String, Map<String, LayoutRubberStamper.EdgeMove>> chopArrives = new HashMap<String, Map<String, LayoutRubberStamper.EdgeMove>>();
      
      chopForRegionLayout(firstSubset, workingRcx_, origRegionBounds, chopDeparts, chopArrives);
      
      // Correct height and width of newly laid-out region, with generous padding:
      SpecialtyInstructions props = propList_.get(0);
      Rectangle baseline = props.getPlacedFullBounds();
      // Seeing null when laying out a region with only one gene in it.  Best to suppress layout for only one gene!
      if (baseline == null) {
        baseline = (Rectangle)origRegionBounds.clone();
      }
      paddedBounds = UiUtil.rectFromRect2D(UiUtil.padTheRect(baseline, 4.0 * UiUtil.GRID_SIZE));

      //
      // We tweak the region to provide space at the top for the repositioned label:
      //
      if (labelBounds != null) {
        paddedBounds.setLocation(paddedBounds.x, (int)UiUtil.forceToGridValueMin(paddedBounds.getY() - labelBounds.getHeight(), UiUtil.GRID_SIZE));
        paddedBounds.setSize(paddedBounds.width, (int)UiUtil.forceToGridValueMax(paddedBounds.getHeight() + labelBounds.getHeight(), UiUtil.GRID_SIZE));
      }
      // Locate it at the original position:
      Rectangle regx = (Rectangle)paddedBounds.clone();
      regx.setLocation(origRegionBounds.x, origRegionBounds.y);
      Vector2D shift = new Vector2D(UiUtil.forceToGridValue(origRegionBounds.x - paddedBounds.x, UiUtil.GRID_SIZE),
                                    UiUtil.forceToGridValue(origRegionBounds.y - paddedBounds.y, UiUtil.GRID_SIZE));
      //
      // These shifted node locations will be installed in the actual layout during the 
      // generalizedExpandRootInstanceLayout call to follow:
      //
      
      props.shiftNodesOnly(shift);
 
      //
      // The bounds need to be shifted too so they end up in a consistent position:
      //
      
      paddedBounds.x += shift.getX();
      paddedBounds.y += shift.getY();
      
      workingRcx_.pushFrc(fixedFrc_);
      // Do the region expansion layout step.  We get back the centering shift that is being applied to the modified
      // layout as a whole; we need to add that bit to our own shift of the new stuff:
      LayoutRubberStamper.RegionExpander rexp = 
        new RegionLayoutExpander(grpID, origRegionBounds, regx, support, firstSubset, paddedBounds, chopDeparts, chopArrives);
       
      LayoutRubberStamper.ERILData eril = new LayoutRubberStamper.ERILData(workingRcx_, groups, rexp, ultimateFailures_,
                                                                           padNeedsForLayout_, loModKeys, moduleShapeRecovery_, 
                                                                           options, monitor, currStart, expandMax);     
      lrs.setERIL(eril);
      Vector2D centeringShift = lrs.generalizedExpandRootInstanceLayout(true);
      
      currStart = expandMax;
        
      // Add the shift to the global centering, get it into a list, and go off and do the shift:
      shift = shift.add(centeringShift);
      shift = shift.add(rexp.getDelta(grpID));
      ArrayList<Vector2D> shiftVecs = new ArrayList<Vector2D>();
      shiftVecs.add(shift);
      installShift(gss_, shiftVecs, new HashMap<Object, GridElement>(), true); // Skip nodes done above; just for everything else
      workingRcx_.popFrc();
    }
    // DONE with block that is ONLY for region-only layouts!
    
    //
    // Need to generate the pieces of links that are not going to be tossed away in a single-subset
    // layout case.  But note that this also creates stuff that is used in the overlay case as well:
    //

    if (!firstSubset.isCompleteGenome()) {   
      linkChopping(workingRcx_, (grpID != null));
    }
    
    //
    // For single-subset (not complete!) layouts, we need to make sure that the region the subset
    // is going to occupy in the layout is empty of everything not bound for the region:
    //
     
    if (!firstSubset.isCompleteGenome() && (overlayKey == null) && (grpID == null)) {
      SpecialtyInstructions props = propList_.get(0);
      GenomeSubset sub = topoSortedSubsetList_.get(0);     
      Set<String> ignoreLinks = DataUtil.setFromIterator(sub.getLinkageSuperSetIterator());
      Set<String> ignoreNodes = DataUtil.setFromIterator(sub.getNodeIterator());
      HashSet<String> keepNodes = new HashSet<String>();
      Iterator<Node> anit = rcx_.getCurrentGenome().getAllNodeIterator();
      while (anit.hasNext()) {
        Node nextNode = anit.next();
        keepNodes.add(nextNode.getID());       
      }
      keepNodes.removeAll(ignoreNodes);     
      LinkRouter router = new LinkRouter();   
      LinkPlacementGrid lps = router.initLimitedGrid(workingRcx_, keepNodes, ignoreLinks, INodeRenderer.LAX_FOR_ORTHO);          
      Rectangle chopRect = props.getPlacedFullBounds();
      if (nonOverlaySpliceBorders_ != null) {
        chopRect = chopRect.union(nonOverlaySpliceBorders_);
      }    
      chopRect = UiUtil.rectFromRect2D(UiUtil.padTheRect(chopRect, UiUtil.GRID_SIZE));
      if (!lps.isEmpty(chopRect)) {
        return (new LinkRouter.RoutingResult(LinkRouter.LAYOUT_OVERLAID_SUBSET));  
      }
    }
     
    //
    // Seventh pass to apply node properties, including placing the nodes down.  Skip if we 
    // are doing a region layout; it was installed previously
    //
      
    if (grpID == null) { 
      dit = gss_.dataIterator();
      int count = 0;
      while (dit.hasNext()) {
        SpecialtyLayoutData sld = dit.next();
        SpecialtyInstructions results = propList_.get(count++);
        if (results == null) {
          continue;
        } 
        installNodeProps(results, workingRcx_, support, sld.subset, monitor, false, ist);
        if ((monitor != null) && !monitor.keepGoing()) {
          throw new AsynchExitRequestException();
        }
      }
    }

    //
    // Original plan was to try and shift newly laid out subset to a clean region if there
    // was overlap with the existing layout, or to try and expand the layout to make room.
    // This will have to wait until a future version; too ambitious for Version 6 release.
    //

    //
    // Now run the link routing FOR ALL THE SUBSETS AS A UNIT:
    //
         
    LinkRouter.RoutingResult retval = LayoutLinkSupport.runSpecialtyLinkPlacement(this, support, monitor, currStart, maxFrac); 
    return (retval);       
  }
 
 
  /***************************************************************************
  **
  ** In the case of region-driven layout, update the region bounds to match the
  ** new layout.  Significantly, we use label position to get all the space we
  ** need for links on the left instead of region pad settings
  */
  
  private void updateRegionBounds(Rectangle needBounds, String grpID, StaticDataAccessContext rcx) { 
   
    GenomeInstance bgi = (GenomeInstance)rcx.getCurrentGenome();
    GroupProperties gp = rcx.getCurrentLayout().getGroupProperties(Group.getBaseID(grpID));     
    Point2D oldPos = gp.getLabelLocation();
    
    GroupFree renderer = new GroupFree();
    Rectangle labelBounds = renderer.getLabelBounds(bgi.getGroup(grpID), rcx);

    double lbw = (labelBounds == null) ? 0.0 : labelBounds.getWidth() / 2.0;
    double lbh = (labelBounds == null) ? 0.0 : labelBounds.getHeight();
    
    double newX = UiUtil.forceToGridValueMax(needBounds.getX() + lbw, UiUtil.GRID_SIZE);
    double newY = UiUtil.forceToGridValueMax(needBounds.getY() + lbh, UiUtil.GRID_SIZE);
    
    rcx.getCurrentLayout().moveGroup(grpID, newX - oldPos.getX(), newY - oldPos.getY());    
    Rectangle bounds = renderer.getBounds(bgi.getGroup(grpID),rcx, null, true);

    GroupProperties changedProps = new GroupProperties(gp);

    int pad = changedProps.getPadding(GroupProperties.TOP);
    int delt = bounds.y - needBounds.y;
    changedProps.setPadding(GroupProperties.TOP, (int)UiUtil.forceToGridValueMax(delt + pad, UiUtil.GRID_SIZE));

    int bpad = changedProps.getPadding(GroupProperties.BOTTOM);
    delt = (needBounds.y + needBounds.height) - (bounds.y + bounds.height); 
    changedProps.setPadding(GroupProperties.BOTTOM, (int)UiUtil.forceToGridValueMax(delt + bpad, UiUtil.GRID_SIZE));

    int lpad = changedProps.getPadding(GroupProperties.LEFT);
    delt = bounds.x - needBounds.x;
    changedProps.setPadding(GroupProperties.LEFT, (int)UiUtil.forceToGridValueMax(delt + lpad, UiUtil.GRID_SIZE));

    int rpad = changedProps.getPadding(GroupProperties.RIGHT);
    delt = (needBounds.x + needBounds.width) - (bounds.x + bounds.width); 
    changedProps.setPadding(GroupProperties.RIGHT, (int)UiUtil.forceToGridValueMax(delt + rpad, UiUtil.GRID_SIZE));

    rcx.getCurrentLayout().replaceGroupProperties(gp, changedProps); 
    return;
  }
  

  /***************************************************************************
  **
  ** Slice and dice existing links in support of subset layouts.  Moved here from
  ** AddCommands class!
  */
  
  private void linkChopping(StaticDataAccessContext rcx, boolean choppedRegion) {
  
    //
    // Since we are now doing subset layout, we need to be able to glue
    // new layouts of a portion of the network into the existing layout.
    // This means we will need to chop up the existing link geometry in the
    // context of shifted source/target nodes.   Save the geometry: 
    //
    
    Genome baseGenome = rcx.getCurrentGenome();
    Layout workingCopyLo = new Layout(rcx.getCurrentLayout());
    StaticDataAccessContext rcxCpy = new StaticDataAccessContext(rcx);
    rcxCpy.setLayout(workingCopyLo);
     
 
    HashMap<String, Map<LinkSegmentID, LinkSegment>> savedTreeGeom = new HashMap<String, Map<LinkSegmentID, LinkSegment>>();
    Iterator<Linkage> lit = baseGenome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String srcID = link.getSource();
      if (savedTreeGeom.get(srcID) == null) {
        BusProperties bp = workingCopyLo.getBusForSource(srcID);
        Map<LinkSegmentID, LinkSegment> geoms = bp.getAllSegmentGeometries(rcxCpy, true);
        savedTreeGeom.put(srcID, geoms);
      }
    }
    
    //
    // For subsets, we do NOT want to completely drop links heading out of a subset
    // unless they are heading INTO another subset that will be handling the layout of
    // those links.  For links heading out to nodes that are untouched, we are going to
    // want to save those for reattachment!  For links heading IN from nodes that are untouched,
    // we are going to want to save those as well!
    //

    HashSet<String> intoSets = new HashSet<String>();
    HashSet<String> outOfSets = new HashSet<String>();
    HashSet<String> betweenSets = new HashSet<String>();
    HashSet<String> insideSets = new HashSet<String>();
    
    int numSub = topoSortedSubsetList_.size();
    
    for (int i = 0; i < numSub; i++) {
      GenomeSubset subset = topoSortedSubsetList_.get(i);
      Set<String> internalLinksForSet = DataUtil.setFromIterator(subset.getLinkageIterator());
      intoSets.addAll(DataUtil.setFromIterator(subset.getLinksInIterator()));
      outOfSets.addAll(DataUtil.setFromIterator(subset.getLinksOutIterator()));
      insideSets.addAll(internalLinksForSet);
    }

    betweenSets.addAll(intoSets);
    betweenSets.retainAll(outOfSets);
    outOfSets.removeAll(betweenSets);
    intoSets.removeAll(betweenSets);
    HashMap<String, BusProperties.RememberProps> fakeRememberProps = new HashMap<String, BusProperties.RememberProps>();

  
    Iterator<String> nlit = insideSets.iterator();
    while (nlit.hasNext()) {
      String lid = nlit.next();
      workingCopyLo.removeLinkPropertiesAndRemember(lid, fakeRememberProps, rcxCpy);
    }
    newLinks_.addAll(insideSets);  

    HashSet<String> allAlienSources = new HashSet<String>();
    Iterator<String> ilit = intoSets.iterator();
    while (ilit.hasNext()) {
      String lid = ilit.next();
      allAlienSources.add(baseGenome.getLinkage(lid).getSource());
    }

    HashMap<String, Set<LinkSegmentID>> seenSegsPerSrc = new HashMap<String, Set<LinkSegmentID>>();
    for (int i = 0; i < numSub; i++) {
      SpecialtyInstructions props = propList_.get(i);
      GenomeSubset subset = topoSortedSubsetList_.get(i);
      Rectangle chopRect = props.getPlacedFullBounds();
      if (chopRect != null) {
        chopRect = UiUtil.rectFromRect2D(UiUtil.padTheRect(chopRect, UiUtil.GRID_SIZE));
      }
      //
      // Recover geometry _to_ nodes outside our subsets.  Segments outside our source rectangle are
      // retained all the way to the target nodes.
      //

      forRecoveriesOut_.add(recoverSpecialtyInputsOut(subset.getSourcesHeadedOut(), workingCopyLo, chopRect, betweenSets, savedTreeGeom, choppedRegion));

      //
      // Recover geometry _from_ nodes outside our subsets.  More complex (lots of different rectangle chops):
      //

      HashSet<String> aliens = new HashSet<String>(allAlienSources);
      aliens.retainAll(subset.getSourcesHeadedIn());
      Set<String> allLinksIntoSubset = DataUtil.setFromIterator(subset.getLinksInIterator());
      HashMap<String, Set<String>> linksFromAlienSrcToSubset = new HashMap<String, Set<String>>();
      Iterator<String> ailit = allLinksIntoSubset.iterator();
      while (ailit.hasNext()) {
        String lid = ailit.next();
        String linkSrc = baseGenome.getLinkage(lid).getSource();
        if (allAlienSources.contains(linkSrc)) {
          Set<String> linksForSource = linksFromAlienSrcToSubset.get(linkSrc);
          if (linksForSource == null) {
            linksForSource = new HashSet<String>();
            linksFromAlienSrcToSubset.put(linkSrc, linksForSource);
          }
          linksForSource.add(lid);
        }
      }
      //
      // Note that the following is not called during an overlay layout.  We also are not
      // supporting multiple subset layout in Version 6!  
      // ******>>>>>!!!!! Originally, the plan was to support overlay layout WITHOUT MODULE LINKS!
      // That would have introduced multiple subsets that each would chop up a link tree.  But we
      // now require module links for overlay, and subset layout treats all the selections as a
      // single subset.  The chopRect is then the bounds of the subset nodes after the stack
      // layout is completed!
      //
      if (!linksFromAlienSrcToSubset.isEmpty()) {
        recoverSpecialtyInputsIn(aliens, workingCopyLo, linksFromAlienSrcToSubset, chopRect, savedTreeGeom, alienSources_, seenSegsPerSrc);
      }
      //
      // Recover geometry for links between our subsets.
      //
      srcToBetween_.putAll(recoverSpecialtyInputsOut(subset.getSourcesHeadedOut(), workingCopyLo, chopRect, intoSets, savedTreeGeom, choppedRegion)); 
    }


    newLinks_.addAll(outOfSets);
    newLinks_.addAll(intoSets);
    newLinks_.addAll(betweenSets);        

    LinkModuleBoundaryBuilder lmbb = new LinkModuleBoundaryBuilder();
    SpecialtyInstructions spSrc = propList_.get(0);
    nonOverlaySolutions_ = lmbb.nonOverlaySplicePrep(spSrc, null, alienSources_, forRecoveriesOut_);
    nonOverlaySpliceBorders_ = lmbb.spliceBoundsForAllBorders(nonOverlaySolutions_);
    if (nonOverlaySpliceBorders_ != null) {
      nonOverlaySpliceBorders_ = UiUtil.growTheRect(nonOverlaySpliceBorders_, UiUtil.GRID_SIZE_INT);
    }
    return;       
  }
  
  /***************************************************************************
  **
  ** Get the info needed to rebuild a link by way of the Specialty Layout Engine/Link Router
  */
  
  private Map<String, SpecialtyLayoutLinkData> recoverSpecialtyInputsOut(Set<String> srcs, Layout lo, Rectangle chopRect, 
                                                                         Set<String> ignoreLinks, 
                                                                         Map<String, Map<LinkSegmentID, LinkSegment>> savedTreeGeom, 
                                                                         boolean choppedRegion) {
    HashMap<String, SpecialtyLayoutLinkData> retval = new HashMap<String, SpecialtyLayoutLinkData>();
    Iterator<String> sit = srcs.iterator();
    while (sit.hasNext()) {
      String srcID = sit.next();
      Map<LinkSegmentID, LinkSegment> geoms = savedTreeGeom.get(srcID);
      SpecialtyLayoutLinkData sin = new SpecialtyLayoutLinkData(srcID);
      BusProperties bp = lo.getBusForSource(srcID);
      sin.extractTreeLeafRemains(bp, chopRect, geoms, ignoreLinks, choppedRegion);
      retval.put(srcID, sin);
    }  
    return (retval);
  } 
 
  /***************************************************************************
  **
  ** We need to pre-chop the existing links at the region boundary when doing a
  ** region layout
  */
  
  private void chopForRegionLayout(GenomeSubset regionSubset, StaticDataAccessContext rcxi, Rectangle chopRect, 
                                   Map<String, Map<String, LayoutRubberStamper.EdgeMove>> chopDepart, 
                                   Map<String, Map<String, LayoutRubberStamper.EdgeMove>> chopArrive) {

    Genome baseGenome = regionSubset.getBaseGenome();
    StaticDataAccessContext rcx = new StaticDataAccessContext(rcxi, baseGenome, rcxi.getCurrentLayout());
    // GROW IT!
    chopRect = UiUtil.rectFromRect2D(UiUtil.padTheRect(chopRect, 2.0 * UiUtil.GRID_SIZE));
   
    Iterator<String> liit = regionSubset.getLinksInIterator();
    chopAndClean(regionSubset, liit, rcx, chopArrive, chopRect, false);
    
    Iterator<String> loit = regionSubset.getLinksOutIterator();
    chopAndClean(regionSubset, loit, rcx, chopDepart, chopRect, true);
    
    //
    // Just toss all the region-internal links.  They are going to be replaced, and the less
    // junk hanging around, the better.
    //
    
    Iterator<String> xrit = regionSubset.getLinkageIterator();
    HashMap<String, BusProperties.RememberProps> fakeRememberProps = new HashMap<String, BusProperties.RememberProps>();
    while (xrit.hasNext()) {
      String linkID = xrit.next();
      rcx.getCurrentLayout().removeLinkPropertiesAndRemember(linkID, fakeRememberProps, rcx);
      rcx.pushFrc(fixedFrc_);
      LayoutLinkSupport.autoAddCrudeLinkProperties(rcx, linkID, null, fakeRememberProps);
      rcx.popFrc();
    }
    return;
  }
   
  
  /***************************************************************************
  **
  ** For links crossing in/out of a region, chop the link trees at the region
  ** boundary and toss away the internal structure
  */
  
  private void chopAndClean(GenomeSubset subset, Iterator<String> lit, StaticDataAccessContext rcxi,
                            Map<String, Map<String, LayoutRubberStamper.EdgeMove>> edgeMovesForSrc, 
                            Rectangle chopRect, boolean headingOut) {
    
    Genome baseGenome = subset.getBaseGenome();   
    StaticDataAccessContext rcx = new StaticDataAccessContext(rcxi, baseGenome, rcxi.getCurrentLayout());
    String regionID = subset.getGroupID();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage link = baseGenome.getLinkage(linkID);
      String srcID = link.getSource();
      // Handle each source just once:
      Map<String, LayoutRubberStamper.EdgeMove> moves = edgeMovesForSrc.get(srcID);
      if (moves == null) {
        moves = new HashMap<String, LayoutRubberStamper.EdgeMove>();
        edgeMovesForSrc.put(srcID, moves);
        BusProperties bp = rcx.getCurrentLayout().getBusForSource(srcID);
        // We keep recalculating segment geometries as long as we are "working"
        boolean working = true;
        // 11/30/12: Safety valve for the imminent V6 release, for when our logic proves to not be airtight!
        // Like today! We have one link inbound, and another from the same source just skirting the outside
        // of the region and going somewhere else.  We were getting double rectangle intersections, causing
        // infinite oscillation where the two links kept swapping between alternating intersections.
        int count = 0;
        while (working) { // Better get set to false sometime!
          // Crank through each segment geometry:
          if (count++ >= 500) {
            System.err.println("chopAndClean failing to converge for: " + srcID);
            break;
          }
          Map<LinkSegmentID, LinkSegment> geoms = bp.getAllSegmentGeometries(rcx, true);  
          
          Iterator<LinkSegmentID>  sit = geoms.keySet().iterator();
          while (sit.hasNext()) {
            LinkSegmentID lsid = sit.next();    
            Set<String> linksThru = bp.resolveLinkagesThroughSegment(lsid);
            LinkSegment fakeSeg = geoms.get(lsid);
            Point2D start = fakeSeg.getStart();
            Point2D end = fakeSeg.getEnd();
            // Try to intersect with region bounds:
            UiUtil.PointAndSide[] pasIntersect = UiUtil.rectIntersections(chopRect, start, end);
            boolean hadAMove = false;
            // Have an intersection:
            if (pasIntersect != null) {
              Point2D forcedSplit = new Point2D.Double();
              // Go through each intersection:
              // NO! Only use the first/last intersection, otherwise we can get oscillations!
              int useIndex = (headingOut) ? 0 : pasIntersect.length - 1;
              UiUtil.forceToGrid(pasIntersect[useIndex].point.getX(), pasIntersect[useIndex].point.getY(), forcedSplit, UiUtil.GRID_SIZE);
              // Forced split is an endpoint:
              if (forcedSplit.equals(start) || forcedSplit.equals(end)) {
                // not doing this stuff for a drop....
                if (!lsid.isForDrop()) {
                  if ((forcedSplit.equals(start) && headingOut) || 
                      (forcedSplit.equals(end) && !headingOut)) {
                    LayoutRubberStamper.EdgeMove em = new LayoutRubberStamper.EdgeMove(new UiUtil.PointAndSide(pasIntersect[useIndex], forcedSplit), srcID, regionID);
                    Iterator<String> ltit = linksThru.iterator();
                    boolean repeated = false;
                    while (ltit.hasNext()) {
                      String nextLinkID = ltit.next();
                      LayoutRubberStamper.EdgeMove emchk = moves.get(nextLinkID);
                      if ((emchk != null) && emchk.pas.equals(pasIntersect[useIndex])) {
                        repeated = true;
                      } else {
                        moves.put(nextLinkID, em);
                      }
                    }
                    if (!repeated) {
                      // For links leaving the region:
                      if (headingOut) {              
                        LinkSegment ls = bp.getSegment(lsid);
                        String parID = ls.getParent();                       
                        if (parID != null) {
                          LinkSegmentID attachID = bp.getRootSegmentID().clone();                          
                          LinkSegmentID reattachID = LinkSegmentID.buildIDForSegment(parID);
                          attachID.tagIDWithEndpoint(LinkSegmentID.START);
                          reattachID.tagIDWithEndpoint(LinkSegmentID.START);
                          bp.moveSegmentOnTree(reattachID, attachID);
                          hadAMove = true;
                          break;
                        }
                      } else { // For links coming into the region:
                        LinkSegmentID attachID = lsid.clone();

                        LinkSegmentID reattachID = LinkSegmentID.buildIDForEndDrop(linkID);
                        LinkBusDrop drop = bp.getDrop(reattachID);
                        if (!drop.getConnectionTag().equals(bp.getSegment(lsid).getID())) {
                          attachID.tagIDWithEndpoint(LinkSegmentID.END);
                          reattachID.tagIDWithEndpoint(LinkSegmentID.START);
                          bp.moveSegmentOnTree(reattachID, attachID);
                          hadAMove = true;
                          break;
                        }
                      }
                    }
                  }
                }
              } else {  // !(forcedSplit.equals(start) || forcedSplit.equals(end)) 
                // Forced split NOT an endpoint, so split the segment:
                LinkSegmentID[] splits = bp.linkSplitSupport(lsid, forcedSplit);
                if (splits != null) {
                  LayoutRubberStamper.EdgeMove em = new LayoutRubberStamper.EdgeMove(new UiUtil.PointAndSide(pasIntersect[useIndex], forcedSplit), srcID, regionID);
                  // Have a split in the segment?  Record the edgeMove for all the links through the segment!
                  Iterator<String> ltit = linksThru.iterator();
                  while (ltit.hasNext()) {
                    String nextLinkID = ltit.next();
                    moves.put(nextLinkID, em);
                  }
                  if (headingOut) {
                    // Heading out?  Attach the split to the root segment.  Remember we are
                    // tossing out geometry internal to the region!
                    LinkSegmentID attachID = bp.getRootSegmentID().clone();
                    attachID.tagIDWithEndpoint(LinkSegmentID.START);
                    LinkSegmentID reattachID = splits[0].clone();
                    reattachID.tagIDWithEndpoint(LinkSegmentID.START);
                    bp.moveSegmentOnTree(reattachID, attachID);
                    hadAMove = true;
                    break;
                  } else {
                    // Heading in?  Attach just an end drop to the split.  Remember we are
                    // tossing out geometry internal to the region!
                    LinkSegmentID attachID = splits[0].clone();
                    attachID.tagIDWithEndpoint(LinkSegmentID.END);
                    LinkSegmentID reattachID = LinkSegmentID.buildIDForEndDrop(linkID);
                    reattachID.tagIDWithEndpoint(LinkSegmentID.START);
                    bp.moveSegmentOnTree(reattachID, attachID);
                    hadAMove = true;
                    break;
                  }
                } // end if (splits != null)
              }  // end else...
            }  // End of having detected an intersection. The break takes us here....
            if (hadAMove) {
              break; // Stop looking at segments, but keep working
            }
            // No more segments, we are done:
            if (!sit.hasNext()) {
              working = false;
            }   
          }  // End of loop over all segments
        }  // End of working...
      }  // End of filling out moves...
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the info needed to rebuild a link by way of the Specialty Layout Engine/Link Router for inbound
  ** link fragment recovery.  Note that in this case, many subsets may be interested in the external
  ** source.  We need to be dealing with only a single copy of the sin for the source, however.
  */
  
  private void recoverSpecialtyInputsIn(Set<String> srcs, Layout lo,  
                                        Map<String, Set<String>> linksFromAlienSrcsToSubset,
                                        Rectangle chopRect, 
                                        Map<String, Map<LinkSegmentID, LinkSegment>> savedTreeGeom, 
                                        Map<String, SpecialtyLayoutLinkData> alienSources, 
                                        Map<String, Set<LinkSegmentID>> seenSegsPerSrc) {
    Iterator<String> sit = srcs.iterator();
    while (sit.hasNext()) {
      String srcID = sit.next();
      Map<LinkSegmentID, LinkSegment> geoms = savedTreeGeom.get(srcID);
      SpecialtyLayoutLinkData originalSin = alienSources.get(srcID);
      SpecialtyLayoutLinkData sin = (originalSin != null) ? originalSin : new SpecialtyLayoutLinkData(srcID);
      if (originalSin == null) {
        alienSources.put(srcID, sin);
      }
      Set<LinkSegmentID> seenSegs = seenSegsPerSrc.get(srcID);
      if (seenSegs == null) {
        seenSegs = new HashSet<LinkSegmentID>();
        seenSegsPerSrc.put(srcID, seenSegs);
      }
      BusProperties bp = lo.getBusForSource(srcID);
      Set<String> linksForSource = linksFromAlienSrcsToSubset.get(srcID);
      sin.extractTreeTrunkRemains(bp, linksForSource, chopRect, geoms, seenSegs);
    }  
    return;
  } 
  
  /***************************************************************************
  **
  ** Actual order for a target module depends on the order that inter-module links
  ** connect on the boundary.  Tweak the global order as needed to present this 
  */  
 
  private SortedMap<Integer, String> customOrderForTarget(TreeMap<Integer, String> existingOrder, GenomeSubset subset) {
  
    
    String trgModule = subset.getModuleID();
    if (trgModule == null) {
      return (new TreeMap<Integer, String>(existingOrder));
    }
    List<String> clockwise = sa_.getBorderOrderedSrcModules(trgModule);
     
    HashMap<String, SortedMap<Integer, String>> orderPerSrcMod = new HashMap<String, SortedMap<Integer, String>>();
    Iterator<Integer> eoit = existingOrder.keySet().iterator();
    while (eoit.hasNext()) {
      Integer keyVal = eoit.next();
      String srcID = existingOrder.get(keyVal);
      String srcMod = sa_.getSourceModuleIDForSource(srcID);
      SortedMap<Integer, String> perMod = orderPerSrcMod.get(srcMod);
      if (perMod == null) {
        perMod = new TreeMap<Integer, String>();
        orderPerSrcMod.put(srcMod, perMod);
      }
      perMod.put(keyVal, srcID);    
    }   
    
    TreeMap<Integer, String> retval = new TreeMap<Integer, String>();
    int offset = 0;
    Iterator<String> cwit = clockwise.iterator();
    while (cwit.hasNext()) {
      String srcModID = cwit.next();
      SortedMap<Integer, String> ops = orderPerSrcMod.get(srcModID);
      if (ops == null) {  // No existing order has been established yet, so skip it....
        continue;
      }
      Iterator<Integer> okit = ops.keySet().iterator();
      int highKey = 0;
      while (okit.hasNext()) {
        Integer keyVal = okit.next();
        highKey = keyVal.intValue();
        String srcID = ops.get(keyVal);
        retval.put(new Integer(highKey + offset), srcID);
      }
      offset += highKey + 1;
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build a shift vec
  */  
 
  private Vector2D buildAShiftVec(SpecialtyLayoutData sld, GridElement grown, Rectangle padded, Rectangle renderedRegion) {
    Vector2D shift = null;      
    if (grown != null) {
      Rectangle orig = sld.results.getOrigModuleRect(); 
           
      double deltaX = (orig.getX() - padded.x) + (grown.rect.getX() - orig.getX());     
      double deltaY = (orig.getY() - padded.y) + (grown.rect.getY() - orig.getY());
       
      //
      // This little bit of code proceeds to adjust the position of the stacked layout so that it
      // is centered inside the shifted module outline:
      //
      
      Rectangle placed = sld.results.getPlacedFullBounds();
      if (placed == null) {
        placed = orig;
      }
      Rectangle shiftedLoc = new Rectangle((int)(placed.getX() + deltaX), (int)(placed.getY() + deltaY), placed.width, placed.height);
      double deltaXtoCenter = grown.rect.getCenterX() - shiftedLoc.getCenterX();    
      double deltaYtoCenter = grown.rect.getCenterY() - shiftedLoc.getCenterY();    
      deltaX += deltaXtoCenter;
      deltaY += deltaYtoCenter;    
         
      deltaX = UiUtil.forceToGridValue(deltaX, UiUtil.GRID_SIZE);      
      deltaY = UiUtil.forceToGridValue(deltaY, UiUtil.GRID_SIZE);
      
      shift = new Vector2D(deltaX, deltaY); 
      
    } else if (renderedRegion != null) {
      //
      // This handles the special region-driven case.  In the case below, we grow the new layout centered
      // on the old layout.  Here, we want the upper left corner of the region to remain fixed so that
      // the region expansion code gets what it expects in terms of a GridGrower element!
      //
      System.err.println("Implement me!");
      throw new IllegalArgumentException();
      /*
   //   Rectangle placedBounds = sld.results.getPlacedStackNodeOnlyBounds();
      Rectangle placedBounds = sld.results.getPlacedStackBounds();
      if (placedBounds != null) {
    //    double origX = sld.subset.getPreferredCenter().getX();
    //    double origY = sld.subset.getPreferredCenter().getY();
              
        placedBounds = UiUtil.rectFromRect2D(UiUtil.padTheRect(placedBounds, UiUtil.GRID_SIZE));
        
        double origPBx = renderedRegion.getX();
        double origPBy = renderedRegion.getY();
                
        double newCenterX = placedBounds.getCenterX();
        double newCenterY = placedBounds.getCenterY();
        
        double newHalfWidth = placedBounds.getWidth() / 2.0;
        double newHalfHeight = placedBounds.getHeight() / 2.0;
               
        shift = new Vector2D(UiUtil.forceToGridValue(origPBx - (newCenterX - newHalfWidth), UiUtil.GRID_SIZE),
                             UiUtil.forceToGridValue(origPBy - (newCenterY - newHalfHeight), UiUtil.GRID_SIZE));
      } */
    } else {  // non-overlay case handled here!
      Rectangle placedBounds = sld.results.getPlacedStackNodeOnlyBounds();
      if ((sld.results.getOrigNodeBounds() != null) && (placedBounds != null)) {
        double origX = sld.subset.getPreferredCenter().getX();
        double origY = sld.subset.getPreferredCenter().getY();
        double newCenterX = placedBounds.getCenterX();
        double newCenterY = placedBounds.getCenterY();
        shift = new Vector2D(UiUtil.forceToGridValue(origX - newCenterX, UiUtil.GRID_SIZE),
                             UiUtil.forceToGridValue(origY - newCenterY, UiUtil.GRID_SIZE));
      }
    }
    return (shift);
  }
  
  /***************************************************************************
  **
  ** Do module shifting steps
  */  
 
  private Map<String, Map<Integer, LinkBundleSplicer.SpliceSolution>> shiftTheModules(GlobalSLEState gss, String overlayKey, 
                                                                                      StaticDataAccessContext rcx,
                                                                                      BTProgressMonitor monitor) throws AsynchExitRequestException {

    //
    // If the layout was generated by an overlay, we want to resize the modules to contain the
    // intra-module links.  We also want to shift the modules around to handle size increases
    // resulting from the layout.
    //

    //
    // NOTE: If we offered the per-stack-row compression scheme for subset networks, this
    // would need to occur BEFORE we do this step.  Currently, it is only offered for full-network
    // layout, which occurs only after everything is placed into the final layout!
    //
    
    //
    // We now implement an outer loop that insures that boundary splice geometry is contained within
    // the module bounds.  Note that the final splice geometry depends on exactly how the final module 
    // link routing positions interact with the interior layout of each module network.  It would seem
    // that a couple iterations at most should resolve things, but I have no proof that the solution
    // would converge on single solution state.  So the loop is bounded...
    //
     
    Map<String, Map<Integer, LinkBundleSplicer.SpliceSolution>> splicePlans = null;
    HashMap<Object, GridElement> grownResults = new HashMap<Object, GridElement>();
    ArrayList<Vector2D> shiftVecs = null;
    HashMap<String, List<GridElement>> spliceNeeds = new HashMap<String, List<GridElement>>();
    HashMap<Point2D, Point2D> myShiftedIMP = new HashMap<Point2D, Point2D>();
       
    for (int i = 0; i < 20; i++) {
      
      //
      // Start with a fresh copy of inter-module paths for each pass:
      //
      
      Map<String, NetModuleLinkExtractor.ExtractResultForSource> impCopy = copyInterModPaths();  
         
      grownResults.clear();
      HashMap<String, Rectangle> paddedResults = new HashMap<String, Rectangle>();     
      HashMap<String, Map<Point2D, Map<String, Point>>> multiLinkData = new HashMap<String, Map<Point2D, Map<String, Point>>>();
      myShiftedIMP.clear();

      int count = 0;
      int nonProgCount = 0;
      Map<String, Map<Point2D, Dimension[]>> lastPlan = null;
      Map<String, Map<Point2D, Dimension[]>> recoveryPlan = null;
      while (true) {
        recoveryPlan = shiftGrownSubsets(impCopy, grownResults, paddedResults, myShiftedIMP, 
                                         multiLinkData, overlayKey, recoveryPlan, spliceNeeds, rcx);
        //
        // Pay attention!  Even if the recovery plan is null and we are done here, if the module rectangles
        // are not big enough, we are coming right back here again until that outer loop up there gives up!
        //
        if (recoveryPlan == null) {
          break;
        }
        //
        // In theory, the above loop exit should work...eventually the shifts will cause the
        // need fo recovery plans to disappear.  But recovery plan glitches could cause the
        // system to get stuck, returning the same plan over and over.  In the case that
        // revealed this problem, links that were inside a module but backtracking on themselves
        // caused badCells (even though those cases will be fixed when link geometry is repaired).
        // I've tried to fix that bad cell case, but to protect against infinite loops, we handle 
        // non-progress in recovery plans as well:
        //
        if (recoveryPlan.equals(lastPlan)) {
          nonProgCount++;
        } else {
          nonProgCount = 0;
        }
        //
        // Fire the explosive bolts!  If all else fails, let's cap the loop count as
        // well, even though it is big!
        //
        count++;
        if ((count >= MODULE_EXPAND_LIMIT_) || (nonProgCount == MODULE_NOPROGRESS_LIMIT_)) {
          break;
        }
        lastPlan = recoveryPlan;
      }
      
      //
      // Shift everybody around in the working copy using the above shift results
      //

      if (sa_.hasInterModulePaths()) {
        shiftWorkingInterModPaths(impCopy, myShiftedIMP);
      } 

      //
      // Get copies for splice planning and shift vecs for installation 
      // if splice planning is successful:
      //

      shiftVecs = new ArrayList<Vector2D>();
      ArrayList<Map<String, SpecialtyLayoutLinkData>> shiftedCopies = new ArrayList<Map<String, SpecialtyLayoutLinkData>>();

      buildShiftedVecsAndCopies(gss, shiftVecs, shiftedCopies, grownResults, paddedResults, null);

      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }

      //
      // Having done the shift, now do splice plans:
      //

      LinkModuleBoundaryBuilder lmbb = new LinkModuleBoundaryBuilder();
      splicePlans = lmbb.splicePrep(sa_, impCopy, shiftedCopies);

      //
      // Check if the splice plans can be accomodated in the current bounds.  If
      // not, we need to keep iterating:
      //
      
      boolean done = true;
      Map<String, Rectangle> needForSplice = lmbb.spliceBoundsPerModule(splicePlans);
      int plNum = propList_.size();
      for (int j = 0; j < plNum; j++) {
        String strKey = Integer.toString(j);
        GridElement grown = grownResults.get(strKey);
        Rectangle grownRect = UiUtil.rectFromRect2D(grown.rect);
        SpecialtyInstructions results = propList_.get(j);
        if (results == null) {
          continue;
        }
        
        //
        // See if the splice bounds exceed the grown rectangle size.  If they have, we need to 
        // create a set of GridElements that can be used to get the boundary bigger in strips
        // around the edges.
        //
        
        Rectangle spliceBounds = needForSplice.get(results.moduleID);
        if (spliceBounds != null) {
          Rectangle speW = UiUtil.growTheRect(spliceBounds, UiUtil.GRID_SIZE_INT);
          // Previously, we had the outermost link tracks right underneath the
          // overlay boundaries.  This is to push the boundaries out to reveal
          // the underlying links.
          speW = UiUtil.rectFromRect2D(UiUtil.padTheRect(speW, UiUtil.GRID_SIZE));
          Rectangle reallyNeeded = grownRect.union(speW);
          if (!reallyNeeded.equals(grownRect)) { // mismatch!
            List<GridElement> needElems = spliceNeeds.get(results.moduleID);
            if (needElems == null) {
              needElems = new ArrayList<GridElement>();
              spliceNeeds.put(results.moduleID, needElems);
            }      
            buildBoundaryGridElements(results, grownRect, speW, needElems, i); 
            done = false;
            // no break --> accumulate all splice needs...
          }
        }
      }      
      if (done) {  // exit since the splice plan can be contained....
        break;
      }      
    }
   
    //
    // Handle the installation:
    //  
  
    if (sa_.hasInterModulePaths()) {
      installShiftedInterModPaths(myShiftedIMP);
    } 

    shiftedIMP_.putAll(myShiftedIMP);

    //
    // Now do the actual installation of the shift:
    //
    
    installShift(gss, shiftVecs, grownResults, false);
  
    return (splicePlans);       
  }
  
  
  /***************************************************************************
  **
  ** Install calculated shifts
  */  
  
  private void installShift(GlobalSLEState gss, List<Vector2D> shiftVecs, Map<Object, GridElement> grownResults, boolean skipNodes) {    
   
    Iterator<SpecialtyLayoutData> dit = gss.dataIterator();
    int count = 0;
    while (dit.hasNext()) {
      SpecialtyLayoutData sld = dit.next();
      SpecialtyInstructions results = propList_.get(count);
      Vector2D shift = shiftVecs.get(count);
      GridElement grown = grownResults.get(Integer.toString(count++));      
      if ((results == null) || (shift == null)) {
        continue;
      }
      sld.results.shift(shift, skipNodes);
      if (grown != null) {
        Rectangle forcedR = UiUtil.rectFromRect2D(grown.rect);
        UiUtil.forceToGrid(forcedR, UiUtil.GRID_SIZE);
        sld.results.setStrictGrownBounds(forcedR);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Generate the boundary grid elements we will need
  */  
  
  @SuppressWarnings("unused")
  private void buildBoundaryGridElements(SpecialtyInstructions results, Rectangle grownRect, Rectangle speW, 
                                         List<GridElement> needElems, int pass) {    
    
    Rectangle oldBounds = results.getOrigModuleRect();
    
    double topNeed = grownRect.getMinY() - speW.getMinY();
    double botNeed = speW.getMaxY() - grownRect.getMaxY();
    double rightNeed = speW.getMaxX() - grownRect.getMaxX();
    double leftNeed = grownRect.getMinX() - speW.getMinX();
       
    HashMap<Object, GridElement> gotEm = new HashMap<Object, GridElement>();
    int numGot = needElems.size();
    for (int i = 0; i < numGot; i++) {
      GridElement ge = needElems.get(i);
      gotEm.put(ge.id, ge);
    }
    needElems.clear();
    
    // HACK!!!!
    double padHack = 20.0;
    
    String key = hacktasticKeyGen(SpecialtyLayoutLinkData.TOP_BORDER, results.moduleID); 
    GridElement ge = gotEm.get(key);
    if (topNeed > 0.0) {
      if (ge != null) {        
        ge.deltaY += topNeed;
      } else {
        ge = new GridElement(new Rectangle(oldBounds.x, oldBounds.y, 10, 10), key, 0.0, topNeed + padHack);
      }
    } 
    if (ge != null) {
      needElems.add(ge);     
    }
    
    key = hacktasticKeyGen(SpecialtyLayoutLinkData.BOTTOM_BORDER, results.moduleID); 
    ge = gotEm.get(key);
    if (botNeed > 0.0) {
      if (ge != null) {        
        ge.deltaY += botNeed;
      } else {
        ge = new GridElement(new Rectangle(oldBounds.x, oldBounds.y + oldBounds.height - 10, 10, 10), key, 0, botNeed + padHack);
      }
    } 
    if (ge != null) {
      needElems.add(ge);     
    }
    
    key = hacktasticKeyGen(SpecialtyLayoutLinkData.RIGHT_BORDER, results.moduleID); 
    ge = gotEm.get(key);
    if (rightNeed > 0.0) {
      if (ge != null) {        
        ge.deltaX += rightNeed;
      } else {
        ge = new GridElement(new Rectangle(oldBounds.x + oldBounds.width - 10, oldBounds.y, 10, 10), key, rightNeed + padHack, 0);
      }
    } 
    if (ge != null) {
      needElems.add(ge);     
    } 
    
    key = hacktasticKeyGen(SpecialtyLayoutLinkData.LEFT_BORDER, results.moduleID); 
    ge = gotEm.get(key);
    if (leftNeed > 0.0) {
      if (ge != null) {        
        ge.deltaX += leftNeed;
      } else {
        ge = new GridElement(new Rectangle(oldBounds.x, oldBounds.y, 10, 10), key, leftNeed + padHack, 0);
      }
    } 
    if (ge != null) {
      needElems.add(ge);     
    }
    return;
  }
  
  /***************************************************************************
  **
  ** HACKTASTIC!
  */  
  
  private String hacktasticKeyGen(int border, String moduleID) {  
    return ("WJRL_HACKASTIC_" + border + "_KEY_" + moduleID); 
  }
  
  /***************************************************************************
  **
  ** Process shift results
  */  
  
  private void buildShiftedVecsAndCopies(GlobalSLEState gss, 
                                         ArrayList<Vector2D> shiftVecs, ArrayList<Map<String, SpecialtyLayoutLinkData>> shiftedCopies, 
                                         HashMap<Object, GridElement> grownResults, HashMap<String, Rectangle> paddedResults, Rectangle renderedRegion) { 
    Iterator<SpecialtyLayoutData> dit = gss.dataIterator();
    int count = 0;
    while (dit.hasNext()) {
      SpecialtyLayoutData sld = dit.next();
      String strKey = Integer.toString(count);
      GridElement grown = grownResults.get(strKey);
      Rectangle padded = paddedResults.get(strKey);

      SpecialtyInstructions results = propList_.get(count++);
      if (results == null) {
        shiftVecs.add(null);
        shiftedCopies.add(null);
        continue;
      }
      Vector2D shift = buildAShiftVec(sld, grown, padded, renderedRegion);
      shiftVecs.add(shift);
      if (shift != null) {
        Map<String, SpecialtyLayoutLinkData> shiftedCopy = sld.results.getShiftedLinkDataCopy(shift);
        shiftedCopies.add(shiftedCopy);
      } else {
        shiftedCopies.add(null);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Collect up the grid elements to grow the modules and links
  */  
 
  private List<GridGrower.GeneralizedGridElement> collectGridElements(Map<String, Rectangle> paddedResults, 
                                                                      Set<Point2D> cpIDs, Map<String, Map<Point2D, Dimension[]>> linkExpansions, 
                                                                      Map<String, NetModuleLinkExtractor.ExtractResultForSource> impCopy, 
                                                                      Map<String, List<GridElement>> spliceNeeds) {
  
    ArrayList<GridGrower.GeneralizedGridElement> gridElements = new ArrayList<GridGrower.GeneralizedGridElement>();
    int numSub = propList_.size();
    for (int i = 0; i < numSub; i++) {
      String strKey = Integer.toString(i);
      SpecialtyInstructions sp = propList_.get(i);
   
      if (sp.moduleID != null) {
        Rectangle oldBounds = sp.getOrigModuleRect();
        Rectangle newBounds = sp.getPlacedFullBounds();  // May be null if no links in module...
        Rectangle padded = new Rectangle((newBounds == null) ? oldBounds : newBounds);
  
        //
        // If the boundary splice operations require that extra padding be added,
        // we do that by adding tiny rectangles to around the boundaries that
        // represent the bounds of the splice blocks that are grown to the
        // needed size.
        // Note that it is not enough to just grow the module rectangle.  This
        // is because strips along select edges need to be grown, possibly
        // shifting growth elements representing other module links that are
        // being negotiated with to set the boundary buffer.  If we just
        // grow the overall module rectangle alon, we might have to expand
        // it hugely overall just to get the needed boundary slice.
        //
        
        int extraW = 0;
        int extraH = 0;
        List<GridElement> spliceExtras = spliceNeeds.get(sp.moduleID);
        if (spliceExtras != null) {
          gridElements.addAll(spliceExtras);
          int numExtra = spliceExtras.size();
          for (int j = 0; j < numExtra; j++) {
            GridElement ge = spliceExtras.get(j);
            extraW += ge.deltaX;
            extraH += ge.deltaY;
          }
        }
    
        paddedResults.put(strKey, padded);
        int hDiff = padded.height - oldBounds.height;
        int wDiff = padded.width - oldBounds.width;
        if (hDiff < 0) hDiff = 0;
        if (wDiff < 0) wDiff = 0;
        GridElement ge = new GridElement(oldBounds, strKey, wDiff + (extraW * 2), hDiff + (extraH * 2));
        gridElements.add(ge);
      }
    }
    
    //
    // Add in elements representing the inter-module corner points that need to be moved:
    //
     
    for (int i = 0; i < numSub; i++) {
      SpecialtyInstructions sp = propList_.get(i);
      if (sp.moduleID != null) {
        NetModuleLinkExtractor.ExtractResultForSource res = impCopy.get(sp.moduleID);
        if (res != null) {
          res.collectGridElements(cpIDs, gridElements, linkExpansions);
        }      
      }
    }
    
    return (gridElements);
  }
 
  /***************************************************************************
  **
  ** Shift grown module subsets around.  Note this only works on private copies;
  ** nothing is committed!
  */  
 
  private Map<String, Map<Point2D, Dimension[]>> shiftGrownSubsets(Map<String, NetModuleLinkExtractor.ExtractResultForSource> impCopy, 
                                                                   Map<Object, GridElement> grownResults, 
                                                                   Map<String, Rectangle> paddedResults, 
                                                                   Map<Point2D, Point2D> shiftedIMP, 
                                                                   Map<String, Map<Point2D, Map<String, Point>>> multiLinkData,
                                                                   String overlayKey, 
                                                                   Map<String, Map<Point2D, Dimension[]>> linkExpansions, 
                                                                   Map<String, List<GridElement>> spliceNeeds, 
                                                                   StaticDataAccessContext rcx) {
 
    grownResults.clear();
    paddedResults.clear();
    shiftedIMP.clear();
    multiLinkData.clear();
       
    HashSet<Point2D> cpIDs = new HashSet<Point2D>();
    List<GridGrower.GeneralizedGridElement> gridElements = collectGridElements(paddedResults, cpIDs, linkExpansions, impCopy, spliceNeeds);
     
    GridGrower grower = new GridGrower();
    
    //
    // We need to go with FULL_GROWTH or MIN_GROWTH_PINNED_ORIGIN to insure the links grown in this fashion 
    // remain orthogonal!  And actually, _only_ FULL_GROWTH is truly valid.  While MIN_GROWTH_PINNED_ORIGIN keeps
    // modules from gaining lots of ugly empty space on the bottom, any links that are leaving/entering in that
    // region lose orthogonality without FULL_GROWTH.  This is because MIN_GROWTH_PINNED_ORIGIN is ignorant of 
    // incoming links that must be kept orthogonal, and outgoing links are considered "independent" grid elements 
    // grow for their own needs only.  Future enhancement could allow MIN_GROWTH_PINNED_ORIGIN if the difference
    // does not adversely affect any links.
    //
    
    List<GridGrower.GeneralizedGridElement> grown = grower.growGrid(gridElements, GridGrower.GrowthTypes.FULL_GROWTH); //MIN_GROWTH_PINNED_ORIGIN);
 
    //
    // Shifted points in one map, rectangles in another:
    //
    
    HashMap<Point2D, Point2D> inverse = new HashMap<Point2D, Point2D>();
    HashMap<String, Rectangle2D> forGrid = new HashMap<String, Rectangle2D>();
    int numGrown = grown.size();
    for (int i = 0; i < numGrown; i++) {
      GridGrower.GeneralizedGridElement ele = grown.get(i);
      if (ele instanceof CenteredGridElement) {
        CenteredGridElement cge = (CenteredGridElement)ele;
        Point2D id = cge.getID();
        if (!cpIDs.contains(cge.getID())) {  //i.e. in the point set...
          throw new IllegalStateException();
        }
        Point2D use = (Point2D)cge.getPoint().clone();
        UiUtil.forceToGrid(use, UiUtil.GRID_SIZE);
        shiftedIMP.put(id, use);
        inverse.put(use, id);
      } else {
        GridElement ge = (GridElement)ele;
        grownResults.put(ge.id, ge);
        forGrid.put((String)ge.id, ge.rect);
      }   
    }
    
    //
    // Create local copies of the link geometry that has been shifted to match the grown
    // grid:
    //
    
    HashMap<String, NetModuleLinkageProperties> moduleLinkTrees = new HashMap<String, NetModuleLinkageProperties>();
    NetOverlayProperties nop = rcx.getCurrentLayout().getNetOverlayProperties(overlayKey);
    Iterator<String> tkit = nop.getNetModuleLinkagePropertiesKeys();
    while (tkit.hasNext()) {
      String tKey = tkit.next();
      NetModuleLinkageProperties lp = rcx.getCurrentLayout().getNetModuleLinkagePropertiesFromTreeID(tKey, overlayKey);
      NetModuleLinkageProperties myCopy = lp.clone();
      myCopy.shiftPerMap(shiftedIMP, true);
      moduleLinkTrees.put(tKey, myCopy);     
    }
    
    //
    // Create our own copy **of the global copy** and shift.  Actually only care about the multiLinkData here!
    //
   
    Iterator<String> samit = impCopy.keySet().iterator();
    while (samit.hasNext()) {
      String modID = samit.next();
      NetModuleLinkExtractor.ExtractResultForSource res = impCopy.get(modID);
      NetModuleLinkExtractor.ExtractResultForSource localCopy = res.clone();
      localCopy.shiftPaths(shiftedIMP);
      localCopy.buildMultiDataMap(multiLinkData);
    }   
  
    //
    // See what problems we have with the grid:
    //
    
    LinkRouter router = new LinkRouter();    
    LinkPlacementGrid initialGrid = router.initGridForModuleGuidedLinks(forGrid, multiLinkData, moduleLinkTrees, rcx);
       
    // initialGrid.debugPatternGrid(false);
    Map<Point, Map<String, LinkPlacementGrid.DecoInfo>>  badCells = initialGrid.findBadModuleCells(true);
    Map<String, Map<Point2D, Dimension[]>> retval = null;
    if ((badCells != null) && !badCells.isEmpty()) {
      // First vertical, then horizontal (see last arg there?)
      Map<String, Map<Point2D, Dimension[]>> currMap = buildRecoveryPlan(badCells, inverse, linkExpansions, true);
      retval = buildRecoveryPlan(badCells, inverse, (currMap == null) ? linkExpansions : currMap, false);
      // If we have remaining bad cells, and start to repeat the creation of the same recovery plan due to
      // underlying error, we will still exit from the loop calling us after a certain number of identical
      // recovery plans are created!
    }
    //
    // If we have to keep going to fix things, we install where we are now ON THE WORKING COPY!!!!
    //
    
    if (retval == null) {
      if (sa_.hasInterModulePaths()) {
        shiftWorkingInterModPaths(impCopy, shiftedIMP);
      }
    }
    return (retval);
  }
    
  /***************************************************************************
  **
  ** 
  */  
 
  private Map<LinkPlacementGrid.DecoInfoKey, int[]> decoSort(Point point, List<LinkPlacementGrid.DecoInfo> decos, boolean isVert) { 
    HashMap<LinkPlacementGrid.DecoInfoKey, int[]> retval = new HashMap<LinkPlacementGrid.DecoInfoKey, int[]>();
    if ((decos == null) || decos.isEmpty()) {
      return (null);
    }
    int numDecos = decos.size();
    //
    // Sort the overlaps by the first coord, and figure out if there is a module
    // overlap in the mix.
    //
    
    boolean haveModuleOverlap = false;
    TreeMap<Double, LinkPlacementGrid.DecoInfo> forSort = new TreeMap<Double, LinkPlacementGrid.DecoInfo>();
    for (int i = 0; i < numDecos; i++) {
      LinkPlacementGrid.DecoInfo deco = decos.get(i);
      if (deco.isModule) {
        haveModuleOverlap = true;
      } else {
        double firstCoord = (isVert) ? deco.firstPt.getX() : deco.firstPt.getY();
        forSort.put(new Double(firstCoord), deco);
      }
    }
    
    ArrayList<LinkPlacementGrid.DecoInfo> sortedList = new ArrayList<LinkPlacementGrid.DecoInfo>(forSort.values());
    int numSort = sortedList.size();
  
    //
    // Calculate the pre-existing separation between the link traces and the location of
    // the overlap point wrt the link trace centerline
    //
     
    LinkPlacementGrid.DecoInfoKey firstDeco = null;
    LinkPlacementGrid.DecoInfoKey lastDeco = null;
    double overCoord = (isVert) ? point.getX() : point.getY();
    HashMap<Integer, Double> currentGaps = new HashMap<Integer, Double>();
    HashMap<Integer, Double> neededGaps = new HashMap<Integer, Double>();
  //  ArrayList isLessThan = new ArrayList();  // i.e. overlap point is left of / above link centerline
    for (int i = 0; i < numSort; i++) {     
      LinkPlacementGrid.DecoInfo deco = sortedList.get(i);
      if (i == 0) {
        firstDeco = deco.getDIK();
      }
      if (i == numSort - 1) {
        lastDeco = deco.getDIK();
      }      
      double decoCoord = ((isVert) ? deco.firstPt.getX() : deco.firstPt.getY()) / 10.0;
      boolean ptLT = (decoCoord >= overCoord);
    //  isLessThan.add(new Boolean(ptLT));
      
      int overOffset = (isVert) ? deco.offsetForSrc.x : deco.offsetForSrc.y;
      if ((overOffset < 0) && !ptLT) {
        // 12/04/12: Am seeing this, but stuff works!  Is this REALLY unexpected?
        //System.err.println("Unexpected combo: " + ptLT + " " + overCoord + " " + decoCoord + " " + overOffset + " " + isVert + " " + point + " " + deco);
      }
     // int overlap = (overOffset < 0) ? (-deco.traceRange.min - (-overOffset)) : (deco.traceRange.max - overOffset);
      int overlap = (overOffset < 0) ? -deco.traceRange.min : deco.traceRange.max;
      Integer intIndex = new Integer((ptLT) ? i - 1 : i);
      Double need = neededGaps.get(intIndex);
      if (need == null) {
        need = new Double(overlap);
        neededGaps.put(intIndex, need);
      } else {
        neededGaps.put(intIndex, new Double(overlap + need.doubleValue()));
      }
      if (i > 0) {
        Integer cintIndex = new Integer(i - 1);
        Double existing = currentGaps.get(cintIndex);
        if (existing == null) {
          throw new IllegalStateException();
        }
        currentGaps.put(cintIndex, new Double(decoCoord - existing.doubleValue()));
      }
      Integer cintIndex = new Integer(i);
      Double existing = new Double(decoCoord);
      currentGaps.put(cintIndex, existing);
    }
    
    for (int i = 0; i < (numSort - 1); i++) {     
      LinkPlacementGrid.DecoInfo deco = sortedList.get(i);
      Integer intIndex = new Integer(i);
      Double needObj = neededGaps.get(intIndex);
      double need = (needObj == null) ? 0 : needObj.doubleValue();
      Double existingObj = currentGaps.get(intIndex);
      double existing = existingObj.doubleValue();
      double expand = need - existing;
      int[] forL = new int[2];
      forL[0] = 0;
      forL[1] = (expand >= 0.0) ? (int)expand + MODULE_LINK_PAD_ : 0;
      retval.put(deco.getDIK(), forL);
    } 
    if (haveModuleOverlap) {
      Double needObj = neededGaps.get(new Integer(-1));
      if (needObj != null) {
        int[] forL = retval.get(firstDeco);
        if (forL == null) {
          forL = new int[2];
          forL[1] = 0;
          retval.put(firstDeco, forL);
          
        }
        forL[0] = needObj.intValue() + + MODULE_LINK_PAD_;     
      }
      needObj = neededGaps.get(new Integer(numSort - 1));
      if (needObj != null) {
        int[] forL = new int[2];
        forL[0] = 0;
        forL[1] = needObj.intValue() + MODULE_LINK_PAD_;
        retval.put(lastDeco, forL);
      }
    }
    return (retval); 
  }
  
  /***************************************************************************
  **
  ** Build a failure recovery plan, i.e. after we detect that a module expansion
  ** is not enough to eliminate link bundle overlaps, we come up with a plan
  ** to remove that problem.
  */  
 
  private Map<String, Map<Point2D, Dimension[]>> buildRecoveryPlan(Map<Point, Map<String, LinkPlacementGrid.DecoInfo>> badCells, 
                                                                   Map<Point2D, Point2D> inverse, 
                                                                   Map<String, Map<Point2D, Dimension[]>> currMap, boolean forVert) {  
    Map<String, Map<Point2D, Dimension[]>> retval = (currMap == null) ? new HashMap<String, Map<Point2D, Dimension[]>>() : currMap;
    boolean upgraded = false;
    Iterator<Point> bcit = badCells.keySet().iterator();
    while (bcit.hasNext()) {
      Point ptKey = bcit.next();
      boolean pending = false;
      boolean modified = false;
      boolean treePending = false;
      Map<String, LinkPlacementGrid.DecoInfo> forPt = badCells.get(ptKey);
      ArrayList<LinkPlacementGrid.DecoInfo> split = new ArrayList<LinkPlacementGrid.DecoInfo>();
      HashMap<LinkPlacementGrid.DecoInfoKey, Point2D> firstPtForTreeID = new HashMap<LinkPlacementGrid.DecoInfoKey, Point2D>();
  
      boolean modFound = false;
      Iterator<String> fpit = forPt.keySet().iterator();
      while (fpit.hasNext()) {
        String srcKey = fpit.next();
        LinkPlacementGrid.DecoInfo deco = forPt.get(srcKey);
        if (deco.isModule) {
          split.add(deco);
          modFound = true;
          //
          // Previous disaster.  We used treeID for key into firstPtForTreeID.  But if there were
          // two branches of the SAME tree that were too close together, this went all to hell
          // with an overwrite of the firstPt.  Updated to use DecoInfoKeys, which also include
          // LinkSegmentID!
          //
        } else if ((deco.normCanon == Vector2D.EAST) || (deco.normCanon == Vector2D.WEST)) {
          if (forVert) {
            split.add(deco);
            firstPtForTreeID.put(deco.getDIK(), deco.firstPt);
          }
        } else {
          if (!forVert) {
            split.add(deco);
            firstPtForTreeID.put(deco.getDIK(), deco.firstPt);
          }
        }
      }
      //
      // Less than two means just skip it.  If we are dealing with modules, this can happen
      // when the module overlap is with e.g. vertical trace and we are collecting horizontal...
      //
      if (split.size() < 2) {
        // FIXME We are actually seeing a good number of reported badCells that appear to not have
        // mutiple decoration!  Track this down, but for now there actually is enough redundancy that
        // this does not appear to cause any problems.
        if (!((split.size() == 1) && modFound)) { 
          //System.err.println("Unexpected split size " + split.size() + " " + ((split.size() > 0) ? split.get(0) + "" : ""));
        }
        split = null;
      }
      
      Map<LinkPlacementGrid.DecoInfoKey, int[]> minForPt = decoSort(ptKey, split, forVert);
      if (minForPt != null) {
        Iterator<LinkPlacementGrid.DecoInfoKey> mfpit = minForPt.keySet().iterator();
        while (mfpit.hasNext()) {
          LinkPlacementGrid.DecoInfoKey dik = mfpit.next();
          int[] overlap = minForPt.get(dik);
          Map<Point2D, Dimension[]> forTree = retval.get(dik.treeID);      
          if (forTree == null) {
            treePending = true;
            forTree = new HashMap<Point2D, Dimension[]>();
          }
          Point2D first = firstPtForTreeID.get(dik);
          Point2D orig = inverse.get(first);
          Dimension[] forPoint = forTree.get(orig);

          if (forPoint == null) {
            forPoint = new Dimension[2];
            forPoint[0] = new Dimension(0, 0);
            forPoint[1] = new Dimension(0, 0);
            pending = true;
          }
          if (overlap[0] > ((forVert) ? forPoint[0].width : forPoint[0].height)) {
            if (forVert) {
              forPoint[0].width = overlap[0];
            } else {
              forPoint[0].height = overlap[0];
            }
            modified = true;
            upgraded = true;
          }
          if (overlap[1] > ((forVert) ? forPoint[1].width : forPoint[1].height)) {
            if (forVert) {
              forPoint[1].width = overlap[1];
            } else {
              forPoint[1].height = overlap[1];
            }
            modified = true;
            upgraded = true;
          }
          if (pending && modified) {
            if (treePending) {
              retval.put(dik.treeID, forTree);
            }
            forTree.put(orig, forPoint);
          }
        }      
      }
    }   
    return ((!upgraded && (currMap == null)) ? null : retval);
  }
  
  /***************************************************************************
  **
  ** Shift Intermodule paths ****on a working copy*****
  */  
 
  private void shiftWorkingInterModPaths(Map<String, NetModuleLinkExtractor.ExtractResultForSource> workingCopy, Map<Point2D, Point2D> movedPts) {
    Iterator<String> samit = sa_.getInterModPathKeys();
    while (samit.hasNext()) {
      String modID = samit.next();
      NetModuleLinkExtractor.ExtractResultForSource res = workingCopy.get(modID);
      res.shiftPaths(movedPts);
    } 
    return;
  }
  
 /***************************************************************************
  **
  ** Shift Intermodule paths on the actual final result
  */  
 
  private void installShiftedInterModPaths(Map<Point2D, Point2D> movedPts) {
    Iterator<String> samit = sa_.getInterModPathKeys();
    while (samit.hasNext()) {
      String modID = samit.next();
      NetModuleLinkExtractor.ExtractResultForSource res = sa_.getExtractResultForSource(modID);
      res.shiftPaths(movedPts);
    } 
    return;
  }
  
  /***************************************************************************
  **
  ** Create a local copy of the InterModPaths that we are going to play with:
  */  
 
  private Map<String, NetModuleLinkExtractor.ExtractResultForSource> copyInterModPaths() {
    HashMap<String, NetModuleLinkExtractor.ExtractResultForSource> localCopy = new HashMap<String, NetModuleLinkExtractor.ExtractResultForSource>();
    Iterator<String> samit = sa_.getInterModPathKeys();
    while (samit.hasNext()) {
      String modID = samit.next();
      NetModuleLinkExtractor.ExtractResultForSource res = sa_.getExtractResultForSource(modID);
      NetModuleLinkExtractor.ExtractResultForSource myCopy = res.clone();
      localCopy.put(modID, myCopy);
    } 
    return (localCopy);
  }

  /***************************************************************************
  **
  ** Install the node properties on the working layout copy, BUT ALSO ACTUALLY
  ** INSTALLS MODEL CHANGES on the GENOME!
  */  
 
  private void installNodeProps(SpecialtyInstructions props, StaticDataAccessContext rcx, UndoSupport support,
                                GenomeSubset subset, BTProgressMonitor monitor, boolean skipPads, InvertedSrcTrg ist)
                                 throws AsynchExitRequestException {
 
    Layout working = rcx.getCurrentLayout();
    
    //
    // Get the points down:
    //
  
    Map<String, Point2D> ptPos = props.nodeLocations;
    Iterator<String> pkit = ptPos.keySet().iterator();
    while (pkit.hasNext()) {
      String ptID = pkit.next();
      if (subset != null) {
        if (!subset.nodeInSubset(ptID)) {
          continue;
        }
      }
      Point2D loc = ptPos.get(ptID);
      NodeProperties npo = working.getNodeProperties(ptID);
      Point2D currLoc = npo.getLocation();
      double dx = UiUtil.forceToGridValue(loc.getX() - currLoc.getX(), UiUtil.GRID_SIZE);
      double dy = UiUtil.forceToGridValue(loc.getY() - currLoc.getY(), UiUtil.GRID_SIZE);
      working.moveNode(ptID, dx, dy, (skipPads) ? null: padNeedsForLayout_, rcx);
    }
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }

    //
    // Change the orientation of the node, if needed:
    //

    Map<String, Integer> nodeOrient = props.orientChanges;
    Iterator<String> noit = nodeOrient.keySet().iterator();
    while (noit.hasNext()) {
      String nodeID = noit.next();
      if (subset != null) {
        if (!subset.nodeInSubset(nodeID)) {
          continue;
        }
      }
      Integer orient = nodeOrient.get(nodeID);
      int newOrient = orient.intValue();
      NodeProperties npo = working.getNodeProperties(nodeID);
      int currOrient = npo.getOrientation();
      if (currOrient != newOrient) {
        NodeProperties changedProps = npo.clone();
        changedProps.setOrientation(newOrient); 
        working.replaceNodeProperties(npo, changedProps); 
      }
    }
    
    //
    // Change the extraPadGrowth of the node, if needed:
    //

    Map<String, Integer> nodeExtraGrowth = props.extraGrowthChanges;
    Iterator<String> negit = nodeExtraGrowth.keySet().iterator();
    while (negit.hasNext()) {
      String nodeID = negit.next();
      if (subset != null) {
        if (!subset.nodeInSubset(nodeID)) {
          continue;
        }
      }
      Integer extraGrowth = nodeExtraGrowth.get(nodeID);
      int newExtra = extraGrowth.intValue();
      NodeProperties npo = working.getNodeProperties(nodeID);
      int currExtra = npo.getExtraGrowthDirection();
      if (currExtra != newExtra) {
        NodeProperties changedProps = npo.clone(); 
        changedProps.setExtraGrowthDirection(newExtra); 
        working.replaceNodeProperties(npo, changedProps); 
      }
    }

    LinkSupport.specialtyPadChanges(props.padChanges, rcx, support, ist);
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
    
    //
    // Lengthen genes if needed (we will NOT shorten nodes - this could
    // result in gene region definition problems:
    //

    Iterator<String> pit = props.lengthChanges.keySet().iterator();
    while (pit.hasNext()) {
      String nodeID = pit.next();
      if (subset != null) {
        if (!subset.nodeInSubset(nodeID)) {
          continue;
        }
      }
      
      Node node = rcx.getCurrentGenome().getNode(nodeID);
      int currPads = node.getPadCount();
      Integer wantPads = props.lengthChanges.get(nodeID);
      int newPads = wantPads.intValue();
      if (newPads <= currPads) {
        continue;
      }
      GenomeChange gc;
      if (node.getNodeType() == Node.GENE) {        
         gc = rcx.getCurrentGenome().changeGeneSize(nodeID, newPads);
      } else {
         gc = rcx.getCurrentGenome().changeNodeSize(nodeID, newPads);
      }
      if ((gc != null) && (support != null)) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
        support.addEdit(gcc);
        // FIXME! issue a model change command
      }
    }
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public static class GlobalSLEState  {

    private ArrayList<SpecialtyLayoutData> sldData_;
    private Map<String, PadCalculatorToo.PadResult> padChanges_;
    private InvertedSrcTrg ist_;
    
    public GlobalSLEState(Map<String, PadCalculatorToo.PadResult> padChanges, InvertedSrcTrg ist) {
      sldData_ = new ArrayList<SpecialtyLayoutData>();
      padChanges_ = padChanges;
      ist_ = ist;
    } 
    
    public void addData(SpecialtyLayoutData sld) {
      sldData_.add(sld);
      return;
    }
  
    public Map<String, PadCalculatorToo.PadResult> getGlobalPadChanges() {
      return (padChanges_);
    } 
    
    public InvertedSrcTrg getInvertedSrcTrg() {
      return (ist_);
    } 
    
    public Iterator<SpecialtyLayoutData> dataIterator() {
      return (sldData_.iterator());
    }  
  }
  
  public static class NodePlaceSupport {

    private Genome genomex;
    private Layout lox;
    private Map<String, Point2D> placement;
    private Map<String, PadCalculatorToo.PadResult> finalPadChanges; 
    private InvertedSrcTrg ist;
    public boolean pureCoreNetwork;
     
    
    public NodePlaceSupport(Genome genome) {   
      this.genomex = genome;
      this.lox = null;
      this.placement = null;
      this.finalPadChanges = null;
      this.ist = null;
      pureCoreNetwork = false;
    }
    
    public NodePlaceSupport(Genome genome, Layout lo) {   
      this.genomex = genome;
      this.lox = lo;
      this.placement = null;
      this.finalPadChanges = null;
      this.ist = null;
      pureCoreNetwork = false;
    }
       
    public NodePlaceSupport(Genome genome, Layout lo,
                            Map<String, Point2D> placement, 
                            Map<String, PadCalculatorToo.PadResult> padChanges, 
                            InvertedSrcTrg ist) {   
      this.genomex = genome;
      this.lox = lo;
      this.placement = placement;
      this.finalPadChanges = padChanges;
      this.ist = ist;
      pureCoreNetwork = false;
    }
    
    public Map<String, PadCalculatorToo.PadResult> getPadChanges() {
      return (finalPadChanges);  
    } 
    
    public Point2D getPosition(String id) {
      return (placement.get(id));  
    } 

    public void setPosition(String id, Point2D pt) {
      placement.put(id, pt);
      return;
    } 
    
    public String getMatchingGene(String geneName) {
      return (ist.getMatchingGene(geneName)); 
    } 
    
    public Set<String> getMatchingNodes(String nodeName) {
      return (ist.getMatchingNodes(nodeName)); 
    }
    
    public Set<String> outboundLinkIDs(String srcID) {
      return (ist.outboundLinkIDs(srcID)); 
    }
    
    public Set<String> inboundLinkIDs(String trgID) {
      return (ist.inboundLinkIDs(trgID)); 
    }
 
    public int outboundLinkCount(String srcID) {
      return (ist.outboundLinkCount(srcID)); 
    }
    
    public Set<String> getTargets(String srcID) {
      return (ist.getTargets(srcID)); 
    }
    
    public int inboundLinkCount(String trgID) {
      return (ist.inboundLinkCount(trgID)); 
    }
    
    public Set<String> getSources(String trgID) {
      return (ist.getSources(trgID)); 
    }

    public Set<String> getLinksBetween(String srcID, String trgID) {
      return (ist.getLinksBetween(srcID, trgID)); 
    }
      
      
    public Iterator<Node> getAllNodeIterator() {
      return (genomex.getAllNodeIterator());  
    } 
    
    public Iterator<Linkage> getLinkageIterator() {
      return (genomex.getLinkageIterator());  
    }
        
    public NodeProperties getNodeProperties(String itemId) {
      return (lox.getNodeProperties(itemId));  
    }
    
    public Node getNode(String itemId) {
      return (genomex.getNode(itemId));  
    }
    
    public Set<String> getNodeSources(String itemId) {
      return (genomex.getNodeSources(itemId));  
    }
    
    public Linkage getLinkage(String itemId) {
      return (genomex.getLinkage(itemId));  
    }
       
    public String getGenomeID() {
      return (genomex.getID());  
    }
    
    public boolean hasLayout() {
      return (lox != null);  
    } 
    
    public NodePlaceSupport(NodePlaceSupport other) { 
      //
      // Note we are NOT cloning the genome or layout, and are restricting the info the user can get from them
      this.genomex = other.genomex;
      this.lox = other.lox;
      this.ist = other.ist;
      
      //
      // These guys do get modified as the layout process proceeds, so we MUST copy them.
      //
      if (other.placement != null) {
        this.placement = new HashMap<String, Point2D>();
        for (String key : other.placement.keySet()) {
          this.placement.put(key, (Point2D)other.placement.get(key).clone());
        }  
      }
      if (other.finalPadChanges != null) {
        this.finalPadChanges = new HashMap<String, PadCalculatorToo.PadResult>();
        for (String key : other.finalPadChanges.keySet()) {
          this.finalPadChanges.put(key, other.finalPadChanges.get(key).clone());
        }  
      }
      this.pureCoreNetwork = other.pureCoreNetwork;   
    }  
  }
 
  //
  // Used to expand out regions when a region layout is chosen:
  //
  
  
  public class RegionLayoutExpander extends LayoutRubberStamper.RegionExpander {
  
    private String regionID_;
    private Rectangle oldRect_;
    private Rectangle newRect_;
    private UndoSupport support_;
    private GenomeSubset sub_;
    private Rectangle padded_;
    
    public RegionLayoutExpander(String regionID, Rectangle oldRect, Rectangle newRect, 
                                UndoSupport support, GenomeSubset sub, Rectangle padded, 
                                Map<String, Map<String, LayoutRubberStamper.EdgeMove>> chopDeparts, 
                                Map<String, Map<String, LayoutRubberStamper.EdgeMove>>chopArrives) {
      super(chopDeparts, chopArrives);
      regionID_ = regionID;
      oldRect_ = (Rectangle)oldRect.clone();
      newRect_ = (Rectangle)newRect.clone();
      support_ = support;
      sub_ = sub;
      padded_ = padded;
    }
   
    protected void customExpandTheRegion(String grpID, double startFrac, double endFrac) throws AsynchExitRequestException {
      if (!regionID_.equals(grpID)) {
        throw new IllegalArgumentException();
      }
      int extraWidth = newRect_.width - oldRect_.width;
      int extraHeight = newRect_.height - oldRect_.height;
      if (extraWidth < 0) {
        extraWidth = 0;
      }
      if (extraHeight < 0) {
        extraHeight = 0;
      }
      SpecialtyInstructions results = propList_.get(0);
      InvertedSrcTrg ist = new InvertedSrcTrg(rcx.getCurrentGenome());
      StaticDataAccessContext rcxL = new StaticDataAccessContext(rcx);
      rcxL.setLayout(grpLo);
      installNodeProps(results, rcxL, support_, sub_, monitor, true, ist);   
      updateRegionBounds(padded_, grpID, rcxL);
      
      //
      // Useless to do this here; the group layouts only have link data for links wholly contained.
      // Cross-region links are held out in the recovery data structure and handled separately!
      /*
      Iterator bmit = boundaryMoves_.iterator();
      while (bmit.hasNext()) {
        EdgeMove em = (EdgeMove)bmit.next();
        BusProperties bp = grpLo.getLinkPropertiesForSource(em.srcID);
        if (bp == null) {
          System.out.println("Bad bp news for " + em.point + " on " + em.srcID);
          continue;
        }
        LinkSegmentID lsid = bp.simpleIntersect(em.point);
        if (lsid == null) {
          System.out.println("unexpected null LSID " + em.srcID + " " + em.point);
          continue;
        }
        LinkSegmentID[] args = new LinkSegmentID[] {lsid};
        System.out.println("gonna move with " + em.point + " for " + em.srcID);
        bp.moveBusLinkSegments(args, em.point, 0.0, 1000.0);
      }
      */
  
      // A mult here causes the region name to be pushed way out!
      int mult = 1; 
      if (mult > 0) {
    //    insertRows.add(new Integer(oldRect_.y / UiUtil.GRID_SIZE_INT));
    //    insertCols.add(new Integer(oldRect_.x / UiUtil.GRID_SIZE_INT));
    //    grpLo.pseudoExpand(gi, insertRows, insertCols, mult, frc, oldToNewShapesPerGroup, moduleLinkFragShifts, grpID);
      }
      return;
    }
    
    public Rectangle getGroupBounds(String grpID) {
      // This is the call that is used to figure out how the region rectangles are going to be
      // grown -> returns the desired new bounds of the group for use by the GridGrower.  Note that
      // (currently) GridGrower is only interested in the delta width and height, not absolute position of new bounds,
      // to calculate growth, BUT it does use it to figure out how to shift the grown bounds when doing reassembly.
      if (regionID_.equals(grpID)) {
        return (newRect_);
      } else {
        Group grp = ((GenomeInstance)rcx_.getCurrentGenome()).getGroup(grpID);
        grpLo = layouts.get(grpID);
        StaticDataAccessContext rcxL = new StaticDataAccessContext(rcx_);
        rcxL.setLayout(grpLo);
        return (grpLo.getLayoutBoundsForGroup(grp, rcxL, true, true));
      }
    }
  }
}
