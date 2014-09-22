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


package org.systemsbiology.biotapestry.cmd.flow.add;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.GeneInstance;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.ui.dialogs.DrawGeneInstanceCreationDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.DrawNodeInstanceCreationDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.GeneCreationDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.NodeCreationDialogFactory;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Support for various node adding flows...
*/

public class AddNodeSupport {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private String currentOverlay;
  private DataAccessContext rcx_;
  private boolean doGene;
  private Node newNode;
  private NodeCandidate cand;
  private NodeCreationInfo nci;

  private BTState appState_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AddNodeSupport(BTState appState, boolean doGene, DataAccessContext dacx) {
    appState_ = appState;
    this.doGene = doGene;
    rcx_ = dacx;
    currentOverlay = appState_.getCurrentOverlay();
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get new node creation info
  */ 
      
  public NodeCreationInfo getNci() {
    return (nci);
  }
    
  /***************************************************************************
  **
  ** Get new node candidate
  */ 
      
  public NodeCandidate getCandidate() {
    return (cand);
  }
  
  /***************************************************************************
  **
  ** Clear new node candidate
  */ 
        
  public void clearCandidate() {
    cand = null;
    return;
  }
 
  /***************************************************************************
  **
  ** Get new node
  */ 
      
  public Node getNewNode() {
    return (newNode);
  }
  
  /***************************************************************************
  **
  ** Set new node
  */ 
      
  public void setNewNode(Node newNode) {
    this.newNode = newNode;
    return;
  }
  
  /***************************************************************************
  **
  ** Process a QueBomb
  */
  
  public RemoteRequest.Result queBombNameMatch(RemoteRequest qbom) {
    
    RemoteRequest.Result result = new RemoteRequest.Result(qbom);
    String nameResult = qbom.getStringArg("nameResult");

    if (doGene) {
      if (rcx_.fgho.matchesExistingGeneOrNodeName(nameResult)) {
        String message = rcx_.rMan.getString("geneProp.dupName");
        String title = rcx_.rMan.getString("geneProp.dupNameTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        result.setSimpleUserFeedback(suf);
        result.setDirection(RemoteRequest.Progress.STOP);
        return (result); 
      }
    } else {
      if (rcx_.fgho.matchesExistingGeneName(nameResult)) {
        String message = rcx_.rMan.getString("nodeProp.dupName");
        String title = rcx_.rMan.getString("nodeProp.dupNameTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        result.setSimpleUserFeedback(suf);
        result.setDirection(RemoteRequest.Progress.STOP);
        return (result);   
      }
    }
    return (result);   
  }
    
   /***************************************************************************
   **
   ** Process a QueBomb
   */
   
   public RemoteRequest.Result queBombNameMatchForGeneCreate(RemoteRequest qbom) {
     
     RemoteRequest.Result result = new RemoteRequest.Result(qbom);
     String nameResult = qbom.getStringArg("nameResult");
     ResourceManager rMan = appState_.getRMan();

     if (doGene) {
       if (rcx_.fgho.matchesExistingGeneOrNodeName(nameResult)) {
         String desc = MessageFormat.format(UiUtil.convertMessageToHtml(rMan.getString("addGene.NameInUse")), 
                                            new Object[] {nameResult});
         String title = rMan.getString("addGene.CreationErrorTitle");
         SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, desc, title);
         result.setSimpleUserFeedback(suf);
         result.setDirection(RemoteRequest.Progress.STOP);
         return (result); 
       }
     } else {
       throw new IllegalStateException();
     }
     return (result);   
   }
     
  /***************************************************************************
  **
  ** Get user info to create new root node
  */ 
  
  public DialogAndInProcessCmd getRootCreationDialog(ServerControlFlowHarness cfh, DBGenome genome, Set<String> modKeys, DialogAndInProcessCmd.CmdState cms) {    
    DialogAndInProcessCmd retval;
    if (doGene) {
      nci = new NodeCreationInfo(null, null, Node.GENE, false, null, false);
      GeneCreationDialogFactory.BuildArgs ba = 
          new GeneCreationDialogFactory.BuildArgs(genome.getUniqueGeneName(), currentOverlay, modKeys);
      GeneCreationDialogFactory ncd = new GeneCreationDialogFactory(cfh);
      ServerControlFlowHarness.Dialog cfhd = ncd.getDialog(ba);
      retval = new DialogAndInProcessCmd(cfhd, cms);
    } else {   
      nci = new NodeCreationInfo(null, null, Node.BUBBLE, false, null, false);
      NodeCreationDialogFactory.BuildArgs ba = 
          new NodeCreationDialogFactory.BuildArgs(nci.newType, genome.getUniqueNodeName(), currentOverlay, modKeys);
      NodeCreationDialogFactory ncd = new NodeCreationDialogFactory(cfh);
      ServerControlFlowHarness.Dialog cfhd = ncd.getDialog(ba);
      retval = new DialogAndInProcessCmd(cfhd, cms);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Extract new root node info from the creation dialog
  */ 
   
  public void extractNewRootNodeInfo(DialogAndInProcessCmd cmd) { 
    if (!cmd.cfhui.haveResults()) {
      nci = null;
      return;
    }      
    if (doGene) {
      GeneCreationDialogFactory.CreateRequest crq = (GeneCreationDialogFactory.CreateRequest)cmd.cfhui;
      nci.newName = crq.nameResult;
      nci.addToModule = crq.doModuleAdd;
    } else {
      NodeCreationDialogFactory.CreateRequest crq = (NodeCreationDialogFactory.CreateRequest)cmd.cfhui; 
      nci.newName = crq.nameResult;
      nci.addToModule = crq.doModuleAdd;
      nci.newType = crq.typeResult;
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** New node generation for root model case
  */  
  
  public void createNewNodeForRoot(boolean doGene) {  
    String nodeID = rcx_.getNextKey();
    if (nci.newType == Node.GENE) {
      newNode = new DBGene(appState_, nci.newName, nodeID);
    } else {
      newNode = new DBNode(appState_, nci.newType, nci.newName, nodeID);
    }
    cand = new NodeCandidate();
    cand.node = newNode;
    cand.addToModule = nci.addToModule; 
    return; 
  } 
  
  /***************************************************************************
  **
  ** Get the instance created, ready for the mouse handler mode change
  */  
 
  public void createNewNodeForInstance(GenomeInstance gi, String nodeID, Node rootNode, int rootType, GenomeSource altSrc) {
    //
    // Bug BT-12-03-08:2  Note if you get the next instance number in a subset model, you can
    // alias into another already existing instance in a parent model!  So we need to get this
    // for the root instance...
    GenomeInstance rootInstance = gi.getVfgParentRoot();
    rootInstance = (rootInstance == null) ? gi : rootInstance; 
    int instanceCount = rootInstance.getNextNodeInstanceNumber(nodeID);
    NodeInstance newInstance;
    if (doGene) {
      newInstance = new GeneInstance(appState_, (DBGene)rootNode, instanceCount, null, NodeInstance.ACTIVE);
    } else {
      newInstance = new NodeInstance(appState_, (DBNode)rootNode, rootType, instanceCount, null, NodeInstance.ACTIVE);
    }
    newNode = newInstance;
    if (altSrc != null) {
      newInstance.setGenomeSource(altSrc);
    }   
    cand = new NodeCandidate();
    cand.node = newNode;
    cand.addToModule = nci.addToModule;
    return; 
  } 
  
  /***************************************************************************
  **
  ** Get user info to create new instance node
  */   
  
  public DialogAndInProcessCmd getInstanceCreationDialog(ServerControlFlowHarness cfh, GenomeInstance gi, DBGenome genome, 
                                                          Set<String> modKeys, DialogAndInProcessCmd.CmdState cms) {     
    DialogAndInProcessCmd retval;
    if (doGene) {
      nci = new NodeCreationInfo(null, null, Node.GENE, false, null, false);
      DrawGeneInstanceCreationDialogFactory.BuildArgs ba = 
          new DrawGeneInstanceCreationDialogFactory.BuildArgs(genome, gi, genome.getUniqueGeneName(), currentOverlay, modKeys);
      DrawGeneInstanceCreationDialogFactory ncd = new DrawGeneInstanceCreationDialogFactory(cfh);
      ServerControlFlowHarness.Dialog cfhd = ncd.getDialog(ba);
      retval = new DialogAndInProcessCmd(cfhd, cms);
    } else {   
      nci = new NodeCreationInfo(null, null, Node.NO_NODE_TYPE, false, null, false);
      
      DrawNodeInstanceCreationDialogFactory.BuildArgs ba = 
        new DrawNodeInstanceCreationDialogFactory.BuildArgs(genome, gi, genome.getUniqueNodeName(), currentOverlay, modKeys);
      DrawNodeInstanceCreationDialogFactory ncd = new DrawNodeInstanceCreationDialogFactory(cfh);
      ServerControlFlowHarness.Dialog cfhd = ncd.getDialog(ba);
      retval = new DialogAndInProcessCmd(cfhd, cms);
    }
    return (retval);
  } 
    
  /***************************************************************************
  **
  ** Extract new instance node info from the creation dialog
  */   
    
  public void extractNewInstanceNodeInfo(DialogAndInProcessCmd cmd) { 
    if (!cmd.cfhui.haveResults()) {
      nci = null;
      return;
    }     
    if (doGene) {
      DrawGeneInstanceCreationDialogFactory.DrawRequest drq = (DrawGeneInstanceCreationDialogFactory.DrawRequest)cmd.cfhui;     
      if (drq.immediateAdd) {
        nci.immediateAdd = true;
      }    
      nci.addToModule = drq.doModuleAdd;
      nci.oldID = drq.idResult;
      if (nci.oldID == null) {
        nci.newName = drq.nameResult;
      }
    } else {
      DrawNodeInstanceCreationDialogFactory.DrawRequest drq = (DrawNodeInstanceCreationDialogFactory.DrawRequest)cmd.cfhui;           
      if (drq.immediateAdd) {
        nci.immediateAdd = true;
      }    
      nci.addToModule = drq.doModuleAdd;
      nci.oldID = drq.idResult;
      if (nci.oldID == null) {
        nci.newName = drq.nameResult;
        nci.newType = drq.typeResult;
        nci.oldIDOptions = drq.idOptions;
      }
    }
    return; 
  }   
 
  /***************************************************************************
  **
  ** Useful...
  */  
    
  public BlackHoleResults droppingIntoABlackHole(int x, int y) {    
    return (AddNodeSupport.droppingIntoABlackHole(appState_, rcx_, x, y, (cand != null) && cand.addToModule));
  } 
  
  /***************************************************************************
  **
  ** Answers if an add at the given point drops the item into a black hole
  ** (region obscured by an opaque overlay).  As a side benefit, we get
  ** back the intersected list used for pending module adds!
  */  
    
  public static BlackHoleResults droppingIntoABlackHole(BTState appState, DataAccessContext rcx, int x, int y, boolean addToModule) {
    
    BlackHoleResults retval = new BlackHoleResults();
    Set<String> nbh = appState.getGenomePresentation().nonBlackHoles(rcx);
    
    List<String> intersected = null;
    if (addToModule || (nbh != null)) {
      intersected = (new NetModuleIntersector(appState, rcx)).netModuleIntersections(x, y, 0.0);
    }
    retval.modCandidates = (addToModule) ? intersected : null;
    //
    // Note that if modules are stacked, with the one on the top being revealed, this test might be too
    // conservative; it will be visible.
    //
    retval.intoABlackHole = (nbh != null) ? (intersected.isEmpty() || !nbh.containsAll(intersected)) : false;
    return (retval);
  }
  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Results!
  */  
  
  public static class BlackHoleResults {  
    public boolean intoABlackHole;
    public List<String> modCandidates;
  }   
    
  /***************************************************************************
  **
  ** Handed out as a closure for calls that need to intersect net modules
  */  
       
  public static class NetModuleIntersector {
     
    private DataAccessContext rcx_;
    private BTState appState_;
   
    public NetModuleIntersector(BTState appState, DataAccessContext rcx) {
      appState_ = appState;
      rcx_ = rcx;
    }
     
    public List<String> netModuleIntersections(int x, int y, double pixDiam) {
      
      String currentOverlay = appState_.getCurrentOverlay();
      TaggedSet currentNetMods = appState_.getCurrentNetModules();
      List<String> intersected = null;
      if (currentOverlay == null) {
        throw new IllegalStateException();
      }
      intersected = appState_.getGenomePresentation().intersectNetModules(x, y, rcx_, currentOverlay, currentNetMods.set);
      return (intersected);
    }
  }
  
  /***************************************************************************
  **
  ** Represents a node that is being added
  */
  
  public static class NodeCandidate {
    public Node node;
    public boolean addToModule;
  }  
       
  /***************************************************************************
  **
  ** Used pass back node creation info
  */
   
  public static class NodeCreationInfo {    
     public String newName; 
     public String oldID;
     public int newType;
     public boolean immediateAdd;
     public Set<String> oldIDOptions;
     public boolean addToModule;
     
     NodeCreationInfo(String newName, String oldID, int newType, boolean immediateAdd, Set<String> oldIDOptions, boolean addToModule) {
       this.newName = newName;
       this.oldID = oldID;
       this.newType = newType;
       this.immediateAdd = immediateAdd;
       this.oldIDOptions = oldIDOptions;
       this.addToModule = addToModule;
     }    
  }   
}
