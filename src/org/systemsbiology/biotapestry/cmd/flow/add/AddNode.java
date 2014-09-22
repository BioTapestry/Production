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


package org.systemsbiology.biotapestry.cmd.flow.add;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.AddCommands;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GroupChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.LocalGenomeSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupChange;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutDataSource;
import org.systemsbiology.biotapestry.ui.LayoutDerivation;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.dialogs.DrawNodeInstanceExistingOptionsDialogFactory;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleAlphaBuilder;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Adding nodes
*/

public class AddNode extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private boolean doGene_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AddNode(BTState appState, boolean doGene) {
    super(appState);
    doGene_ = doGene;
    name = (doGene) ? "command.Add" : "command.AddNode";
    desc = (doGene) ? "command.Add" : "command.AddNode";
    icon = (doGene) ? "NewGene24.gif" : "NewNodes24.gif";
    mnem = (doGene) ? "command.AddMnem" : "command.AddNodeMnem"; 
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
    return (cache.canAdd());
  }
  
  /***************************************************************************
  **
  ** Handle out-of-band question/response
  ** 
  */
  
  @Override     
  public RemoteRequest.Result processRemoteRequest(RemoteRequest qbom, DialogAndInProcessCmd.CmdState cms) {
    AddNodeState ans = (AddNodeState)cms;
    if (qbom.getLabel().equals("queBombCheckInstanceToSubset")) {
      return (ans.queBombCheckInstanceToSubset(qbom));
    } else if (qbom.getLabel().equals("queBombCheckTargetGroups")) {
      return (ans.queBombCheckTargetGroups(qbom));
    } else if (qbom.getLabel().equals("topOnSubset")) {
      return (ans.topOnSubset(qbom));
    } else if (qbom.getLabel().equals("queBombNameMatch")) {
      return (ans.queBombNameMatch(qbom));
    } else if (qbom.getLabel().equals("queBombNameMatchForGeneCreate")) {
      return (ans.queBombNameMatchForGeneCreate(qbom));
    } else {
      throw new IllegalArgumentException();
    }
  }
 
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
  
  @Override      
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    AddNodeState retval = new AddNodeState(appState_, doGene_, dacx);
    return (retval);  
  }
  
  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    DialogAndInProcessCmd next;
    // Last test is to insure this only executes once:
    if ((last != null) && (((AddNodeState)last.currStateX).rcxT_ == null)) {
      AddNodeState ans = (AddNodeState)last.currStateX;
      ans.doGene = doGene_;
      ans.currentOverlay = appState_.getCurrentOverlay();
      ans.rcxT_ = cfh.getDataAccessContext();
      ans.rcxR_ = ans.rcxT_.getContextForRoot();
      ans.cfh = cfh;
      next = ans.stepBiWarning(cfh);
    }
  
    while (true) {
      if (last == null) {
        AddNodeState ans = new AddNodeState(appState_, doGene_, cfh.getDataAccessContext());
        ans.doGene = doGene_;
        ans.currentOverlay = appState_.getCurrentOverlay();
        ans.cfh = cfh;       
        next = ans.stepBiWarning(cfh);
      } else {
        AddNodeState ans = (AddNodeState)last.currStateX;
        if (ans.getNextStep().equals("stepGenNodeInfoDialog")) {
          next = ans.stepGenNodeInfoDialog(cfh);
        } else if (ans.getNextStep().equals("stepBuildNodeCreationInfo")) {
          next = ans.stepBuildNodeCreationInfo(last);
        } else if (ans.getNextStep().equals("stepSetToMode")) {
          next = ans.stepSetToMode();
        } else if (ans.getNextStep().equals("stepProcessBlackHoleAnswer")) {
          next = ans.stepProcessBlackHoleAnswer(last);
        } else if (ans.getNextStep().equals("stepCreateNewInstanceNode")) {
          next = ans.stepCreateNewInstanceNode();
        } else if (ans.getNextStep().equals("stepDoTheInstanceInstall")) {         
          next = ans.stepDoTheInstanceInstall();
        } else if (ans.getNextStep().equals("stepNodeAddForDBGenome")) { 
          next = ans.stepNodeAddForDBGenome();
        } else if (ans.getNextStep().equals("stepCheckForVfNNodeReuseProcessAnswer")) {   
          next = ans.stepCheckForVfNNodeReuseProcessAnswer(last);
        } else if (ans.getNextStep().equals("stepCheckForNodeReuseProcessAnswer")) {      
          next = ans.stepCheckForNodeReuseProcessAnswer(last);
        } else if (ans.getNextStep().equals("stepAfterWeSetReUse")) {   
          next = ans.stepAfterWeSetReUse();          
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
  ** Called as new X, Y info comes in to place the node.
  */
  
  @Override    
  public DialogAndInProcessCmd processClick(Point theClick, boolean isShifted, double pixDiam, DialogAndInProcessCmd.CmdState cmds) {
    AddNodeState ans = (AddNodeState)cmds;
    ans.x = UiUtil.forceToGridValueInt(theClick.x, UiUtil.GRID_SIZE);
    ans.y = UiUtil.forceToGridValueInt(theClick.y, UiUtil.GRID_SIZE);
    ans.rcxT_.pixDiam = pixDiam;
    ans.changeViz = false;
    DialogAndInProcessCmd retval;
    if (ans.rcxT_.getGenome() instanceof GenomeInstance) {
      retval = ans.nodeAddForGenomeInstance(ans.cfh);
    } else {
      AddNodeSupport.BlackHoleResults bhr = ans.droppingIntoABlackHole();
      ans.modCandidates = bhr.modCandidates;
      if (bhr.intoABlackHole) {
        ResourceManager rMan = appState_.getRMan();
        String message = UiUtil.convertMessageToHtml(rMan.getString("intoBlackHole.resetViz"));
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_CANCEL_OPTION, message, rMan.getString("intoBlackHole.resetVizTitle"));
        retval = new DialogAndInProcessCmd(suf, ans);      
        ans.nextStep_ = "stepProcessBlackHoleAnswer";
        ans.nextNextStep = "stepNodeAddForDBGenome";
      } else {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans);
        ans.nextStep_ = "stepNodeAddForDBGenome"; 
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Running State: Kinda needs cleanup!
  */
        
  public static class AddNodeState implements DialogAndInProcessCmd.CmdState, DialogAndInProcessCmd.MouseClickCmdState {
     
  //  private Genome genome; 
    private String currentOverlay;
 //  private String genomeKey;
  //  private String layoutKey;
    private boolean doGene;
    private ServerControlFlowHarness cfh;
    private AddNodeSupport ansu_; 
    private DataAccessContext rcxT_;
    private DataAccessContext rcxR_;
     //--------------------
     
     
    private String nextStep_;
    private String nextNextStep;

    private Node newRootNode;
    private Node existingRootNode;
    private Set<String> existingRootNodeOptions;
    private boolean changeViz;
     
    private String nodeID;
    private Node rootNode;
    private int rootType;
     
    private Layout.PadNeedsForLayout rootFixups;
    private List<String> modCandidates;

    private AddLevel mode;
    private int x;
    private int y;
    private Group rootGroup;
    private String inGrpID;
    private NodeInstance rootNodeInstance;
    private Layout.PadNeedsForLayout padFixups;
    
    private LocalGenomeSource lgs;

    private NodeReuse reuse;

    private Group intersectGroup;
     
    private BTState appState_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Constructor
    */
       
    public AddNodeState(BTState appState, boolean doGene, DataAccessContext dacx) {
      appState_ = appState;
      ansu_ = new AddNodeSupport(appState, doGene, dacx);
      rcxT_ = dacx;
      rcxR_ = rcxT_.getContextForRoot();
    }
     
    /***************************************************************************
    **
    ** mouse masking
    */
    
    public int getMask() {
      return (MainCommands.ALLOW_NAV_PUSH);
    }
    
    public boolean pushDisplay() {
      return (false);
    }
    
    public boolean noSubModels() {
      return (false);
    }
        
    public boolean noInstances() {
      return (false);
    }
            
    public boolean showBubbles() {
      return (false);
    }
    
    public boolean noRootModel() {
      return (false);
    }  
    
    public boolean mustBeDynamic() {
      return (false);
    }
     
    public boolean cannotBeDynamic() {
      return (true);
    }
    
    public boolean hasTargetsAndOverlays() {
      return (false);
    }
    
    public Floater floaterMode() {
      return (DialogAndInProcessCmd.MouseClickCmdState.Floater.OBJECT_FLOATER);
    }
    
    public void setFloaterPropsInLayout(Layout flay) {
      NodeProperties nprop = new NodeProperties(rcxT_.cRes, flay, ansu_.getNewNode().getNodeType(), ansu_.getNewNode().getID(), 0.0, 0.0, false);
      flay.setNodeProperties(ansu_.getNewNode().getID(), nprop); 
      return;
    }
  
    public Object getFloater(int x, int y) {
      return (ansu_.getNewNode());
    }
    
    public Color getFloaterColor() {
      return (null); // This handler uses setFloaterProps, not this
    }
       
    /***************************************************************************
    **
    ** Process a QueBomb
    */
    
    private RemoteRequest.Result queBombNameMatch(RemoteRequest qbom) {
      return (ansu_.queBombNameMatch(qbom));
    }
    
    /***************************************************************************
     **
     ** Process a QueBomb
     */
     
     private RemoteRequest.Result queBombNameMatchForGeneCreate(RemoteRequest qbom) {
       return (ansu_.queBombNameMatchForGeneCreate(qbom));
     }
   
    /***************************************************************************
    **
    ** Process a QueBomb
    */
    
    private RemoteRequest.Result queBombCheckInstanceToSubset(RemoteRequest qbom) {
      
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);
      String idResult = qbom.getStringArg("idResult");
      result.setBooleanAnswer("haveResult", false);
      result.setBooleanAnswer("immediateAdd", false);
  
      //
      // Further analysis of existing gene:
      //
      
      GenomeInstance rgi = rcxT_.getGenomeAsInstance().getVfgParentRoot();
      boolean doingSubsetModel = (rgi != null);
      String prefix = (doGene) ? "drawNewGene" : "drawNewNode";
  
      // When installing an instance, it is immediate if the region is present, and a
      // balk if it is not or if the gene is already there:
      
      if (!GenomeItemInstance.isBaseID(idResult)) {
        if (!doingSubsetModel) {
          throw new IllegalStateException();
        }
        if (!haveRequiredGroup(idResult, rcxT_.getGenomeAsInstance(), rgi) || (rcxT_.getGenome().getNode(idResult) != null)) { 
          String message = UiUtil.convertMessageToHtml(rcxT_.rMan.getString(prefix + ".noTargsSubsetModel"));
          String title = rcxT_.rMan.getString(prefix + ".noTargsTitle");
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);
          result.setBooleanAnswer("haveResult", false);
          result.setSimpleUserFeedback(suf);
          result.setDirection(RemoteRequest.Progress.STOP);
          return (result);   
        } else {
          String message = rcxT_.rMan.getString(prefix + ".immediateAdd");
          String title = rcxT_.rMan.getString(prefix + ".immediateAddTitle");
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.PLAIN, message, title);
          result.setBooleanAnswer("haveResult", true);
          result.setBooleanAnswer("immediateAdd", true);
          result.setSimpleUserFeedback(suf);
          result.setDirection(RemoteRequest.Progress.DONE);
          return (result);
        }
      }
      return (result);
    }
     
    /***************************************************************************
    **
    ** Process a QueBomb
    */
       
    private RemoteRequest.Result queBombCheckTargetGroups(RemoteRequest qbom) {   
       
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);
      String idResult = qbom.getStringArg("idResult");
        
      //
      // We are working with a top model ID.  There may be no place to put it if
      // an instance in present in every group (top instance only).  Even if there is
      // only one place, we are not in "immediate" mode, since the user needs to be
      // able to draw where they want it:
      //
       
      GenomeInstance rgi = rcxT_.getGenomeAsInstance().getVfgParentRoot();
      boolean doingSubsetModel = (rgi != null);
      String prefix = (doGene) ? "drawNewGene" : "drawNewNode";
  
      if (!doingSubsetModel) {
        if (getTargetGroups(idResult, rcxT_.getGenomeAsInstance(), doGene).size() == 0) {
          String message = UiUtil.convertMessageToHtml(rcxT_.rMan.getString(prefix + ".noTargs"));
          String title = rcxT_.rMan.getString(prefix + ".noTargsTitle");
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title); 
          result.setBooleanAnswer("haveResult", false);
          result.setSimpleUserFeedback(suf);
          result.setDirection(RemoteRequest.Progress.STOP);
          return (result);
        }
        result.setBooleanAnswer("haveResult", true);
        result.setDirection(RemoteRequest.Progress.DONE);
        return (result);
      }
      return (result);
    }
    
      
    /***************************************************************************
    **
    ** Process a QueBomb
    ** 
    */
       
    private RemoteRequest.Result topOnSubset(RemoteRequest qbom) {   
      
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);
      String idResult = qbom.getStringArg("idResult");
         
      //
      // Top model ID on subset model.  There must be a group without an instance of the gene if we
      // are to continue.  If there is only one group, we can be immediate if the top instance has
      // a copy already.  If not, the user still needs to draw where they want the gene to go:
      //
  
      ResourceManager rMan = appState_.getRMan();
      GenomeInstance rgi = rcxT_.getGenomeAsInstance().getVfgParentRoot();
      String prefix = (doGene) ? "drawNewGene" : "drawNewNode";
        
      Set<String> targGroups = getTargetGroups(idResult, rcxT_.getGenomeAsInstance(), doGene);
      int numTarg = targGroups.size();
      if (numTarg == 0) {
        String message = UiUtil.convertMessageToHtml(rMan.getString(prefix + ".noTargs"));
        String title = rMan.getString(prefix + ".noTargsTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title); 
        result.setBooleanAnswer("haveResult", false);
        result.setSimpleUserFeedback(suf);
        result.setDirection(RemoteRequest.Progress.STOP);
        return (result);
      } else if (numTarg == 1) {
        String groupID = targGroups.iterator().next();
        String baseGrpID = Group.getBaseID(groupID);   
        int genCount = rgi.getGeneration();        
        String inherit = Group.buildInheritedID(baseGrpID, genCount);
        int instNum = rgi.getInstanceForNodeInGroup(idResult, inherit);
        if (instNum != -1) { // i.e. VfA needs a copy of it
          String message = rMan.getString(prefix + ".immediateAdd");
          String title = rMan.getString(prefix + ".immediateAddTitle");
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.PLAIN, message, title);        
          result.setBooleanAnswer("immediateAdd", true);
          result.setSimpleUserFeedback(suf);   
          result.setStringAnswer("idResult", GenomeItemInstance.getCombinedID(idResult, Integer.toString(instNum)));
        }
        result.setBooleanAnswer("haveResult", true);
        result.setDirection(RemoteRequest.Progress.DONE); 
        return (result);
      }
      result.setBooleanAnswer("haveResult", true);
      result.setDirection(RemoteRequest.Progress.DONE);
      return (result);
    }
  
    /***************************************************************************
    **
    ** In a subset model, if we are trying to place an instance, it must be
    ** the case that the group holding the instance must be present in the
    ** subset model.
    */  
   
    private boolean haveRequiredGroup(String nodeInstanceID, GenomeInstance tgi, GenomeInstance rgi) {    
      Group needGroup = rgi.getGroupForNode(nodeInstanceID, GenomeInstance.ALWAYS_MAIN_GROUP);
      String baseGrpID = Group.getBaseID(needGroup.getID());   
      int genCount = tgi.getGeneration();        
      String inherit = Group.buildInheritedID(baseGrpID, genCount);
      return (tgi.getGroup(inherit) != null);
    }   
   
    /***************************************************************************
    **
    ** Find out if there is a place to put the top-level gene, i.e.
    ** is there some group that the gene in not in already.  Good for any
    ** level instance model.  This return the set of the groups
    ** it can go
    */  
   
    private Set<String> getTargetGroups(String baseNodeID, GenomeInstance tgi, boolean isGene) {
      if (!GenomeItemInstance.isBaseID(baseNodeID)) {
        throw new IllegalArgumentException();
      }
      HashSet<String> retval = new HashSet<String>();
      Iterator<Group> git = tgi.getGroupIterator();
      while (git.hasNext()) {
        Group group = git.next();
        if (group.isASubset(tgi)) {
          continue;
        }
        boolean notInTheGroup = true;
        Iterator<? extends Node> nit = (isGene) ? tgi.getGeneIterator() : tgi.getNodeIterator();
        while (nit.hasNext()) {
          Node node = nit.next();
          String nodeID = node.getID();
          if (!GenomeItemInstance.getBaseID(nodeID).equals(baseNodeID)) {
            continue;
          }
          if (group.isInGroup(nodeID, tgi)) {
            notInTheGroup = false;
            break;
          }
        }
        if (notInTheGroup) {
          retval.add(group.getID());
        }      
      }
      return (retval);
    }
     
    /***************************************************************************
    **
    ** Warn of build instructions
    */
      
    private DialogAndInProcessCmd stepBiWarning(ServerControlFlowHarness cfh) {
      DialogAndInProcessCmd daipc;
      if (appState_.getDB().haveBuildInstructions()) {
        ResourceManager rMan = appState_.getRMan();
        String message = rMan.getString("instructWarning.message");
        String title = rMan.getString("instructWarning.title");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);     
        daipc = new DialogAndInProcessCmd(suf, this);      
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      }    
      nextStep_ = "stepGenNodeInfoDialog";
      return (daipc);     
    }
   
    /***************************************************************************
    **
    ** Generate dialog to get node info from user
    */
         
    private DialogAndInProcessCmd stepGenNodeInfoDialog(ServerControlFlowHarness cfh) {
    
      TaggedSet currentNetMods = appState_.getCurrentNetModules();  
 
      DialogAndInProcessCmd daipc;
      if (rcxT_.getGenome() instanceof GenomeInstance) {
        daipc = ansu_.getInstanceCreationDialog(cfh, rcxT_.getGenomeAsInstance(), rcxT_.getDBGenome(), currentNetMods.set, this);     
      } else {    
        daipc = ansu_.getRootCreationDialog(cfh, (DBGenome)rcxT_.getGenome(), currentNetMods.set, this);      
      }
      nextStep_ = "stepBuildNodeCreationInfo";
      return (daipc);
    }
   
    /***************************************************************************
    **
    ** Step 2: Build node creation info 
    */
    
    private DialogAndInProcessCmd stepBuildNodeCreationInfo(DialogAndInProcessCmd lastDaipc) {

      DialogAndInProcessCmd retval;
         
      if (rcxT_.getGenome() instanceof GenomeInstance) {
        AddNodeSupport.NetModuleIntersector nmi = new AddNodeSupport.NetModuleIntersector(appState_, rcxT_);
        ansu_.extractNewInstanceNodeInfo(lastDaipc);         
        if (ansu_.getNci() == null) {
          ansu_.clearCandidate();
          retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going 
          nextStep_ = "stepSetToMode";
        } else {
          retval = drawNewNodeToInstanceStart(nmi, rcxT_, rcxR_);
          // Heads us off on a more roundabout route.... 
        }
      } else {  // We are NOT an instance!  
        ansu_.extractNewRootNodeInfo(lastDaipc);
        if (ansu_.getNci() == null) {
          ansu_.clearCandidate();
        } else {
          ansu_.createNewNodeForRoot(doGene);
        }
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
        nextStep_ = "stepSetToMode";
      }
      
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Install a mouse handler
    */
    
    private DialogAndInProcessCmd stepSetToMode() {
  
      ansu_.setNewNode((ansu_.getCandidate() == null) ? null : ansu_.getCandidate().node);
       
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      retval.suPanelMode = (doGene) ? PanelCommands.Mode.ADD_GENE : PanelCommands.Mode.ADD_NODE;
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Add a gene to an instance model via drawing
    */  
   
    private DialogAndInProcessCmd drawNewNodeToInstanceStart(AddNodeSupport.NetModuleIntersector intersector, DataAccessContext rcxT, DataAccessContext rcxR) {
   
      //
      // Add it
      //
  
      if (ansu_.getNci().oldID == null) {
        if (ansu_.getNci().oldIDOptions != null) {
          existingRootNodeOptions = ansu_.getNci().oldIDOptions;
        }
        nodeID = rcxT.getNextKey();
        if (doGene) {
          newRootNode = new DBGene(appState_, ansu_.getNci().newName, nodeID);
        } else {
          newRootNode = new DBNode(appState_, ansu_.getNci().newType, ansu_.getNci().newName, nodeID);
        }      
        existingRootNode = null;
        DBGenome holdingGenome = new DBGenome(appState_, "fake", "_BT_FAKE_HOLDING_GENOME_");
        lgs = new LocalGenomeSource(holdingGenome, holdingGenome);
        if (doGene) {
          holdingGenome.addGene((Gene)newRootNode);
        } else {
          holdingGenome.addNode(newRootNode);
        }
        rootNode = newRootNode;
        rootType = ansu_.getNci().newType;
      } else {
        nodeID = ansu_.getNci().oldID;
        newRootNode = null;
        existingRootNode = rcxR.getGenome().getNode(GenomeItemInstance.getBaseID(ansu_.getNci().oldID));
        rootNode = existingRootNode;
        rootType = existingRootNode.getNodeType();
      }
      
      DialogAndInProcessCmd retval;
      if (ansu_.getNci().immediateAdd) {
        ansu_.setNewNode(rcxT.getGenomeAsInstance().getVfgParentRoot().getNode(nodeID));  // can work with original; only need ID
        modCandidates = null;
        if (ansu_.getNci().addToModule) {
          NodeProperties np = rcxT.getLayout().getNodeProperties(nodeID);
          Point2D nodeLoc = np.getLocation();
          modCandidates = intersector.netModuleIntersections((int)nodeLoc.getX(), (int)nodeLoc.getY(), 0.0);
        }
        // Immediate adds mean these values don't matter
        x = 0;
        y = 0;
        // This will not warn of black holes; we do not need to handle viz changes:
        retval = calculateLevelAdds(null, false, rcxT, rcxR);      
      } else {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
        nextStep_ = "stepCreateNewInstanceNode";
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Get the instance created, ready for the mouse handler mode change
    */  
   
    private DialogAndInProcessCmd stepCreateNewInstanceNode() {
      ansu_.createNewNodeForInstance(rcxT_.getGenomeAsInstance(), nodeID, rootNode, rootType, lgs);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      nextStep_ = "stepSetToMode";   
      return (retval); 
    } 
  
    /***************************************************************************
    **
    ** Locate a new node in an instance, and get it into the
    ** root too if needed.
    */  
   
    private enum AddLevel {ADDING_ONLY_TO_ROOT_, ADD_ALL_LEVELS_, ADD_BELOW_ROOT_};
  
    private DialogAndInProcessCmd calculateLevelAdds(Group group, boolean intoBlackHole, DataAccessContext rcxT, DataAccessContext rcxR) {   
      //
      // If we are working at the root instance level, then we need to 
      // check and see if we are adding the node to a group that does
      // not have a copy already.  Otherwise, we fail.  If we are working
      // at a subset level, there are three possibilities:
      // 1) Root vfa does not have an instance in the group
      //     Add to group at root level, add to this instance
      // 2) Root vfa does have an instance in the group, but this subset does not
      //    Add to this instance as an inclusion of the root version
      // 3) Root vfa does have an instance in the group, and so does this subset
      //    This is a failure case.      
    
      inGrpID = null;
      rootNodeInstance = null;
      
      if (rcxT.getGenomeAsInstance().getVfgParent() == null) {
        // instanceIsInGroup gotta be called for VfAroots only!
        if (group.instanceIsInGroup(GenomeItemInstance.getBaseID(ansu_.getNewNode().getID()))) {
          DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this);
          retval.pccr = ServerControlFlowHarness.ClickResult.ERROR;
          return (retval);
        }
        mode = AddLevel.ADDING_ONLY_TO_ROOT_;
        rootGroup = group;
      } else if (group != null) {
        GenomeInstance rootVfg = rcxT.getGenomeAsInstance().getVfgParentRoot();
        String baseGrpID = Group.getBaseID(group.getID());   
        int genCount = rootVfg.getGeneration();
        String inherit = Group.buildInheritedID(baseGrpID, genCount);
        rootGroup = rootVfg.getGroup(inherit);
        String baseID = GenomeItemInstance.getBaseID(ansu_.getNewNode().getID());
        rootNodeInstance = (NodeInstance)rootGroup.getInstanceInGroup(baseID, rootVfg);
        if (rootNodeInstance == null) {
          // case 1
          mode = AddLevel.ADD_ALL_LEVELS_;
        } else {
          inGrpID = rootNodeInstance.getID();
          Node inSub = rcxT.getGenome().getNode(inGrpID);
          if (inSub == null) {
            // case 2
            mode = AddLevel.ADD_BELOW_ROOT_;
          } else {
            // case 3
            DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this);
            retval.pccr = ServerControlFlowHarness.ClickResult.ERROR;
            return (retval);
          }        
        }  
      } else { //immediate mode (this method called before user draws)
        mode = AddLevel.ADD_BELOW_ROOT_; 
        rootGroup = null; // don't need it
        GenomeInstance rootVfg = rcxT.getGenomeAsInstance().getVfgParentRoot();
        rootNodeInstance = (NodeInstance)rootVfg.getNode(ansu_.getNewNode().getID());
      }
   
      //
      // Adding in a node may mess up existing net module linkages.  Record existing
      // state before changing anything:
      //      
      rootFixups = (newRootNode != null) ? rcxR_.getLayout().findAllNetModuleLinkPadRequirements(rcxR_) : null;
      padFixups = rcxT_.getLayout().findAllNetModuleLinkPadRequirements(rcxT_);
  
      //
      // Give user the chance to change the viz if they are falling into a black hole:
  
      DialogAndInProcessCmd retval;
      changeViz = false;
      if (intoBlackHole) {
        ResourceManager rMan = appState_.getRMan();
        String message = UiUtil.convertMessageToHtml(rMan.getString("intoBlackHole.resetViz")); 
        String title = rMan.getString("intoBlackHole.resetVizTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_CANCEL_OPTION, message, title);     
        retval = new DialogAndInProcessCmd(suf, this);
        nextStep_ = "stepProcessBlackHoleAnswer";
        nextNextStep = "stepDoTheInstanceInstall";
      } else {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
        nextStep_ = "stepDoTheInstanceInstall"; 
      }
      return (retval);    
    } 
    
    /***************************************************************************
    **
    ** Collect up the black hole answer, and change viz if user indicates
    */   
  
    private DialogAndInProcessCmd stepProcessBlackHoleAnswer(DialogAndInProcessCmd daipc) {   
      DialogAndInProcessCmd retval;
      int changeVizVal = daipc.suf.getIntegerResult();
      if (changeVizVal == SimpleUserFeedback.CANCEL) {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this);
      } else {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
        changeViz = (changeVizVal == SimpleUserFeedback.YES);
        nextStep_ = nextNextStep;
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Locate a new node in an instance, and get it into all other needed levels as well.
    */    
  
    private DialogAndInProcessCmd stepDoTheInstanceInstall() {   
     
      UndoSupport support = new UndoSupport(appState_, (doGene) ? "undo.addGeneToInstance" : "undo.addNodeToInstance");
      
      ArrayList<GenomeInstance> ancestry = new ArrayList<GenomeInstance>();
      ancestry.add(rcxT_.getGenomeAsInstance());
      GenomeInstance parent = rcxT_.getGenomeAsInstance().getVfgParent();
      while (parent != null) {
        ancestry.add(parent);
        parent = parent.getVfgParent();
      }
      Collections.reverse(ancestry);
       
      NodeInstance pInst = null;
      int aNum = ancestry.size();
      for (int i = 0; i < aNum; i++) {
        GenomeInstance gi = ancestry.get(i);
        DataAccessContext rcxA = new DataAccessContext(rcxT_, gi);
        if (i == 0) {
          if (mode != AddLevel.ADD_BELOW_ROOT_) {
            drawNewNodeAddForRootInstance(rootGroup, gi, support, rcxT_, rcxR_);
            if (mode == AddLevel.ADDING_ONLY_TO_ROOT_) {
              AddCommands.drawIntoNetModuleSupport(appState_, ansu_.getNewNode(), modCandidates, rcxA, currentOverlay, support);
              AddCommands.finishNetModPadFixups(appState_, rcxR_, rootFixups, rcxA, padFixups, support);
              support.finish();
              if (changeViz) {
                NetOverlayController noc = appState_.getNetOverlayController();
                noc.setSliderValue(NetModuleAlphaBuilder.MINIMAL_ALL_MEMBER_VIZ);
              }
              return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));         
            }
            pInst = (NodeInstance)ansu_.getNewNode();
            inGrpID = ansu_.getNewNode().getID();
          } else {
            pInst = rootNodeInstance;
            inGrpID = pInst.getID();
          }
        } else {
          Node inSub = gi.getNode(inGrpID);
          if (inSub == null) {
            PropagateSupport.addNewNodeToSubsetInstance(appState_, rcxA, pInst, support);
            inSub = gi.getNode(inGrpID);
            if (i == (aNum - 1)) {
              AddCommands.drawIntoNetModuleSupport(appState_, inSub, modCandidates, rcxA, currentOverlay, support);
            }
          }
          pInst = (NodeInstance)inSub;
        }
      }
       
      //
      // Repair net module pads, if needed:
      //
       
      AddCommands.finishNetModPadFixups(appState_, rcxR_, rootFixups, rcxT_, padFixups, support); 
      support.finish(); 
   
      if (changeViz) {
        NetOverlayController noc = appState_.getNetOverlayController();
        noc.setSliderValue(NetModuleAlphaBuilder.MINIMAL_ALL_MEMBER_VIZ);
      }
      
      //
      // DONE!
      //
      
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
    }   
    
    /***************************************************************************
    **
    ** Final placement of node in DBGenome
    */  
   
    private DialogAndInProcessCmd stepNodeAddForDBGenome() {
  
      //
      // Adding in a node may mess up existing net module linkages.  Record existing
      // state before changing anything:
      //
      // NOTE: Was getting a crash here with null targetLayout on root gene add on web client. Is this still
      // the case??
      
      Layout.PadNeedsForLayout padFixups = rcxT_.getLayout().findAllNetModuleLinkPadRequirements(rcxT_);
   
      //
      // Undo/Redo support
      //
      
      UndoSupport support = new UndoSupport(appState_, doGene ? "undo.addGene" : "undo.addNode");     
      
      GenomeChange gc;
      if (doGene) {
        gc = ((DBGenome)rcxT_.getGenome()).addGeneWithExistingLabel((Gene)ansu_.getNewNode());
      } else {
        gc = ((DBGenome)rcxT_.getGenome()).addNodeWithExistingLabel(ansu_.getNewNode());
      }    
      
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, rcxT_, gc);
        support.addEdit(gcc);
      }
      
      //
      // If instructed to add to current net module, do so here:
      //
      
      AddCommands.drawIntoNetModuleSupport(appState_, ansu_.getNewNode(), modCandidates, rcxT_, currentOverlay, support);
      
      //
      // Install node properties 
      //
      
      NodeProperties np = new NodeProperties(rcxT_.cRes, rcxT_.getLayout(), ansu_.getNewNode().getNodeType(), 
                                             ansu_.getNewNode().getID(), x, y, false);
      Layout.PropChange[] lpc = new Layout.PropChange[1]; 
      lpc[0] = rcxT_.getLayout().setNodeProperties(ansu_.getNewNode().getID(), np);
      if (lpc != null) {
        PropChangeCmd pcc = new PropChangeCmd(appState_, rcxT_, lpc);
        support.addEdit(pcc);
      }     
       
      //
      // Module link pad fixups:
      //
      
      AddCommands.finishNetModPadFixups(appState_, null, null, rcxT_, padFixups, support);
      
      support.addEvent(new ModelChangeEvent(rcxT_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));    
      support.finish();
      
      if (changeViz) {
        NetOverlayController noc = appState_.getNetOverlayController();
        noc.setSliderValue(NetModuleAlphaBuilder.MINIMAL_ALL_MEMBER_VIZ);
      }
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this)); 
    }
    
    /***************************************************************************
    **
    ** Locate a new node in an instance, and get it into the root too if needed.
    */  
      
    private boolean drawNewNodeAddForRootInstance(Group group, GenomeInstance rootVfg, UndoSupport support, DataAccessContext rcxT, DataAccessContext rcxR) {
      //
      // If the gene does not exist already at the top, put it there:
      //     
      if (newRootNode != null) {
        GenomeChange gc;
        if (doGene) {
          gc = ((DBGenome)rcxR.getGenome()).addGeneWithExistingLabel((Gene)newRootNode);
        } else {
          gc = ((DBGenome)rcxR.getGenome()).addNodeWithExistingLabel(newRootNode);
        }
        
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, rcxT_, gc);
          support.addEdit(gcc);
        }
        support.addEvent(new ModelChangeEvent(rcxR.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));    
        //    appState_.getDB().clearHoldingGenome(); NO More! Do this instead:
        ((NodeInstance)ansu_.getNewNode()).setGenomeSource(null);
        lgs = null;
      
        //
        // Propagate node properties to all interested layouts
        //
       
        NodeProperties np = new NodeProperties(rcxR.cRes, rcxR.getLayout(), newRootNode.getNodeType(), 
                                               newRootNode.getID(), x, y, false);
        Layout.PropChange[] lpc = new Layout.PropChange[1]; 
        lpc[0] = rcxR.getLayout().setNodeProperties(newRootNode.getID(), np);
        if (lpc != null) {
          PropChangeCmd pcc = new PropChangeCmd(appState_, rcxT_, lpc);
          support.addEdit(pcc);
        }   
        handleLayoutDerivation(appState_, rcxT_, rootVfg, group, support); 
      }
       
      //
      // Propagate node to top-level instances:
      //
        
      GenomeChange gc;
      if (doGene) {
        gc = rootVfg.addGene((Gene)ansu_.getNewNode());
      } else {
        gc = rootVfg.addNode(ansu_.getNewNode());
      }    
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, rcxT_, gc);
        support.addEdit(gcc);
      }
  
      GroupChange grc = group.addMember(new GroupMember(ansu_.getNewNode().getID()), rootVfg.getID());
      if (grc != null) {
        GroupChangeCmd grcc = new GroupChangeCmd(appState_, rcxT_, grc);
        support.addEdit(grcc);
      }
      support.addEvent(new ModelChangeEvent(rootVfg.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
       
      NodeProperties newProp = new NodeProperties(rcxT.cRes, rcxT.getLayout(), ansu_.getNewNode().getNodeType(), 
                                                  ansu_.getNewNode().getID(), x, y, false);
      Layout.PropChange[] lpc = new Layout.PropChange[1]; 
      lpc[0] = rcxT.getLayout().setNodeProperties(ansu_.getNewNode().getID(), newProp);
      if (lpc != null) {
        PropChangeCmd pcc = new PropChangeCmd(appState_, rcxT_, lpc);
        support.addEdit(pcc);
      }   
      return (true); 
    } 
  
    /***************************************************************************
    **
    ** Add node step
    */  
    
    public DialogAndInProcessCmd nodeAddForGenomeInstance(ServerControlFlowHarness cfh) {
      GenomeInstance parent = rcxT_.getGenomeAsInstance().getVfgParent();
      Intersection inter;
      if (parent != null) {    
        inter = appState_.getGenomePresentation().selectGroupFromParent(x, y, rcxT_);
      } else {
        inter = appState_.getGenomePresentation().selectGroupForRootInstance(x, y, rcxT_);
      }
      if (inter == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.UNSELECTED, this)); 
      }
      String groupID = inter.getObjectID();
      String baseGrpID = Group.getBaseID(groupID);   
      int genCount = rcxT_.getGenomeAsInstance().getGeneration();        
      String inherit = Group.buildInheritedID(baseGrpID, genCount);
      intersectGroup = rcxT_.getGenomeAsInstance().getGroup(inherit);
      if (intersectGroup == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ERROR, this));
      }
  
      //
      // If we are in the VfA, and all the existing node options already have been added
      // to the specified group, we must create a new one.  Otherwise, prompt the
      // user to reuse one or create a new one.
      //
      DialogAndInProcessCmd retval;
      GenomeInstance tgi = rcxT_.getGenomeAsInstance();
      if (existingRootNodeOptions == null) {
        reuse = NodeReuse.DO_REGULAR_MODE;
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
        nextStep_ = "stepAfterWeSetReUse"; 
      } else if (tgi.getVfgParent() == null) {
        retval = setNodeReuseInGroupForVfA(cfh); 
      } else {        
        retval = setNodeReuseInGroupForVfN(cfh); 
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** After we figure out the reuse plans, process them
    */
     
    private DialogAndInProcessCmd stepAfterWeSetReUse() {       
      if (reuse == NodeReuse.CANCEL_REQUEST) {
        // Not really selected, but it cancels the add mode!
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
      } else if (reuse == NodeReuse.DO_IMMEDIATE_MODE) {
        intersectGroup = null;
      }
        
      AddNodeSupport.BlackHoleResults bhr = ansu_.droppingIntoABlackHole(x, y);
      modCandidates = bhr.modCandidates;
      return (calculateLevelAdds(intersectGroup, bhr.intoABlackHole, rcxT_, rcxR_));
    }
    
    /***************************************************************************
    **
    ** Make needed changes to a node add if user chooses a group that allows reuse:
    ** In the case of nodes, not genes, the ability to have the same name means that
    ** if the user is drawing a node with a previously used name, they may either
    ** create a new node, _or_ reuse an existing one.
    */  
  
    private enum NodeReuse {CANCEL_REQUEST, DO_IMMEDIATE_MODE, DO_REGULAR_MODE};
    
    private DialogAndInProcessCmd setNodeReuseInGroupForVfA(ServerControlFlowHarness cfh) {     
      //
      // If we are in the VfA, and all the existing node options already have been added
      // to the specified group, we must create a new one.  Otherwise, prompt the
      // user to reuse one or create a new one.
      //
      GenomeInstance tgi = rcxT_.getGenomeAsInstance();
      HashSet<String> remainingOptions = new HashSet<String>();
      Iterator<String> ernoit = existingRootNodeOptions.iterator();
      while (ernoit.hasNext()) {
        String existingID = ernoit.next(); 
        // instanceIsInGroup gotta be called for VfAroots only!
        if (!intersectGroup.instanceIsInGroup(existingID)) {
          remainingOptions.add(existingID);
        }      
      }
      DialogAndInProcessCmd retval;
      if (remainingOptions.size() > 0) { 
        DrawNodeInstanceExistingOptionsDialogFactory.BuildArgs ba = 
            new DrawNodeInstanceExistingOptionsDialogFactory.BuildArgs(rcxT_.getDBGenome(), tgi, remainingOptions, new HashSet<String>());
        DrawNodeInstanceExistingOptionsDialogFactory ncd = new DrawNodeInstanceExistingOptionsDialogFactory(cfh);
        ServerControlFlowHarness.Dialog cfhd = ncd.getDialog(ba);
        retval = new DialogAndInProcessCmd(cfhd, this);
        nextStep_ = "stepCheckForNodeReuseProcessAnswer";
      } else {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
        reuse = NodeReuse.DO_REGULAR_MODE;
        nextStep_ = "stepAfterWeSetReUse"; 
      }
      return (retval);
    }
        
    /***************************************************************************
    **
    ** Figure out what user wants for reuse 
    */  
    
    private DialogAndInProcessCmd stepCheckForNodeReuseProcessAnswer(DialogAndInProcessCmd daipc) {
      DialogAndInProcessCmd retval;    
      DrawNodeInstanceExistingOptionsDialogFactory.ExistingDrawRequest drq = 
        (DrawNodeInstanceExistingOptionsDialogFactory.ExistingDrawRequest)daipc.cfhui;
   
      if (!drq.haveResults()) {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
        reuse = NodeReuse.CANCEL_REQUEST;
        nextStep_ = "stepAfterWeSetReUse"; 
      } else {
        if (!drq.doDraw) {
          newRootNode = null;
          existingRootNode = rcxT_.getDBGenome().getNode(drq.idResult);
          int instanceCount = rcxT_.getGenomeAsInstance().getNextNodeInstanceNumber(drq.idResult);
          ansu_.setNewNode(new NodeInstance(appState_, (DBNode)existingRootNode, existingRootNode.getNodeType(), instanceCount, null, NodeInstance.ACTIVE));
        }
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
        reuse = NodeReuse.DO_REGULAR_MODE;
        nextStep_ = "stepAfterWeSetReUse";
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Node reuse for VfN
    */  
   
    private DialogAndInProcessCmd setNodeReuseInGroupForVfN(ServerControlFlowHarness cfh) {     

      //
      // If in a VFN, we may have options already added to a parent group but not pulled
      // down, or options not added at all.  Prompt the user to pull, reuse, or create.
      //
      HashSet<String> remainingOptions = new HashSet<String>();
      HashSet<String> remainingInstanceOptions = new HashSet<String>();
      
      GenomeInstance rootVfg = rcxT_.getGenomeAsInstance().getVfgParentRoot();
      if (rootVfg == null) {
        throw new IllegalStateException();
      }
      String baseGrpID = Group.getBaseID(intersectGroup.getID());   
      int genCount = rootVfg.getGeneration();        
      String inherit = Group.buildInheritedID(baseGrpID, genCount);
      Group rootGroup = rootVfg.getGroup(inherit);
      Iterator<String> ernoit = existingRootNodeOptions.iterator();
      while (ernoit.hasNext()) {
        String existingID = ernoit.next(); 
        NodeInstance checkNodeInstance = (NodeInstance)rootGroup.getInstanceInGroup(existingID, rootVfg);
        if (checkNodeInstance == null) {
          remainingOptions.add(existingID);
        } else {
          String inGrpID = checkNodeInstance.getID();
          Node inSub = rcxT_.getGenome().getNode(inGrpID);
          if (inSub == null) {
            remainingInstanceOptions.add(inGrpID);
          }     
        } 
      }
      DialogAndInProcessCmd retval;
      if ((remainingOptions.size() > 0) || (remainingInstanceOptions.size() > 0)) {  
        DrawNodeInstanceExistingOptionsDialogFactory.BuildArgs ba = 
            new DrawNodeInstanceExistingOptionsDialogFactory.BuildArgs(rcxT_.getDBGenome(), rcxT_.getGenomeAsInstance(), remainingOptions, remainingInstanceOptions);
        DrawNodeInstanceExistingOptionsDialogFactory ncd = new DrawNodeInstanceExistingOptionsDialogFactory(cfh);
        ServerControlFlowHarness.Dialog cfhd = ncd.getDialog(ba);
        retval = new DialogAndInProcessCmd(cfhd, this);
        nextStep_ = "stepCheckForVfNNodeReuseProcessAnswer";
      } else {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
        reuse = NodeReuse.DO_REGULAR_MODE;
        nextStep_ = "stepAfterWeSetReUse"; 
      }
      return (retval);
    }
           
    /***************************************************************************
    **
    ** Figure out what user wants for reuse 
    */  
    
    private DialogAndInProcessCmd stepCheckForVfNNodeReuseProcessAnswer(DialogAndInProcessCmd daipc) {     
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      nextStep_ = "stepAfterWeSetReUse";
      DrawNodeInstanceExistingOptionsDialogFactory.ExistingDrawRequest drq = 
        (DrawNodeInstanceExistingOptionsDialogFactory.ExistingDrawRequest)daipc.cfhui;
      if (!drq.haveResults()) {
        reuse = NodeReuse.CANCEL_REQUEST;
      } else {
        reuse = NodeReuse.DO_REGULAR_MODE;
        if (!drq.doDraw) {
          newRootNode = null;
          existingRootNode = rcxT_.getDBGenome().getNode(drq.idResult);
          // First case: we are reusing a node in a parent group.  Just use the
          // immediate add path from here on out
          if (!GenomeItemInstance.getBaseID(drq.idResult).equals(drq.idResult)) {
            ansu_.setNewNode(rcxT_.getGenomeAsInstance().getVfgParentRoot().getNode(drq.idResult));  // can work with original; only need ID
            reuse = NodeReuse.DO_IMMEDIATE_MODE;  // NOTE OVERRIDE REGULAR!
          //
          // Second case: we are reusing a root node.  Previous new instance
          // referenced a now defunct new root node.
          } else {
            int instanceCount = rcxT_.getGenomeAsInstance().getVfgParentRoot().getNextNodeInstanceNumber(drq.idResult);
            ansu_.setNewNode(new NodeInstance(appState_, (DBNode)existingRootNode, existingRootNode.getNodeType(), instanceCount, null, NodeInstance.ACTIVE));
          }
        }
      }
      return (retval);
    } 
   
    
    /***************************************************************************
    **
    ** Useful...
    */  
      
    private AddNodeSupport.BlackHoleResults droppingIntoABlackHole() {    
      return (ansu_.droppingIntoABlackHole(x, y));
    } 
  }
   
  /***************************************************************************
  **
  ** Update the root layout derivation following a node add
  **
  */  
 
  public static void handleLayoutDerivation(BTState appState, DataAccessContext dacx, GenomeInstance gi, Group group, UndoSupport support) { 
    
    GenomeInstance tgi;
    Group rootGroup;
    GenomeInstance parentGi = gi.getVfgParent();
    
    if (parentGi == null) {
      tgi = gi;
      rootGroup = group;
    } else { 
      tgi = parentGi;
      String baseGrpID = Group.getBaseID(group.getID());   
      int genCount = tgi.getGeneration();        
      String inherit = Group.buildInheritedID(baseGrpID, genCount);
      rootGroup = tgi.getGroup(inherit);
    }

    Layout rootLo = dacx.getRootLayout();
    LayoutDerivation ld = rootLo.getDerivation();
    if (ld == null) {
      ld = new LayoutDerivation();
    }
    LayoutDataSource lds = new LayoutDataSource(tgi.getID(), rootGroup.getID());
    if (!ld.containsDirectiveModuloTransforms(lds)) {
      ld.addDirective(lds);
      Layout.PropChange lpc = rootLo.setDerivation(ld);
      PropChangeCmd pcc = new PropChangeCmd(appState, dacx, lpc);
      support.addEdit(pcc);   
    }
    return;
  }
}
