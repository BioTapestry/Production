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

//
// Ported to Gaggle 2007-4 by Dan Tenenbaum <dtenenbaum@systemsbiology.org>
// in September 2007
//
/* START HIDE GAGGLE CODE */  /*
package org.systemsbiology.biotapestry.gaggle;

import java.util.HashMap;
import java.util.ArrayList;
import java.awt.Frame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.ConnectException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.systemsbiology.gaggle.geese.common.GooseShutdownHook;
import org.systemsbiology.gaggle.core.Boss;
import org.systemsbiology.gaggle.core.Goose;
import org.systemsbiology.gaggle.core.datatypes.Cluster;
import org.systemsbiology.gaggle.core.datatypes.DataMatrix;
import org.systemsbiology.gaggle.core.datatypes.GaggleTuple;
import org.systemsbiology.gaggle.core.datatypes.Interaction;
import org.systemsbiology.gaggle.core.datatypes.Namelist;
import org.systemsbiology.gaggle.core.datatypes.Network;
import org.systemsbiology.gaggle.geese.common.RmiGaggleConnector;
import org.systemsbiology.gaggle.util.MiscUtil;

import org.systemsbiology.biotapestry.analysis.SignedLink;
import org.systemsbiology.biotapestry.app.EditorWindow;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.GeneralBuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.LoneNodeBuildInstruction;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.DBNode;

///****************************************************************************
//**
//** Used for gaggle communication.
//

public class BTGoose implements Goose, GooseAppInterface {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static final String NODE_TYPE_TAG_ = "NodeType";    
  private static final String LINK_SIGN_TAG_ = "LinkSign";  

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
  
  private EditorWindow topWindow_;  
  private BTState appState_;
  private String name_;
  private Boss gaggleBoss_;
  private SelectionSupport selSupport_;
  private String targetName_;
  private boolean activated_;
  private RmiGaggleConnector connector_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  ///***************************************************************************
  //**
  //** Null Constructor
  //

  public BTGoose() {
    activated_ = false;    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  //***************************************************************************
  //**
  //** Set goose parameters - called on AWT thread
  //

  public void setParameters(EditorWindow topWindow, BTState appState, String species) {
    if (activated_) {
      throw new IllegalStateException();
    }
    appState_ = appState;
    selSupport_ = new SelectionSupport(appState_, species);
    topWindow_ = topWindow;
    name_ = "BioTapestry";
    targetName_ = "boss";
  }  

  ///***************************************************************************
  //**
  //** Activate the goose - called on AWT thread
 // 

  public void activate() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException();
    }
    activated_ = false;
    connector_ = new RmiGaggleConnector(this);
    new GooseShutdownHook(connector_);
    connectToGaggle();  // sets activated_ on success
    return;
  }  

  ///***************************************************************************
  //**
  //** A function to find out if goose is active - called on AWT thread
  //
  
  public boolean isActivated() {
    return (activated_);
  }  
  
  ///***************************************************************************
  //**
  //** A function to get selection support - called on AWT thread
  //
  
  public SelectionSupport getSelectionSupport() {
    return (selSupport_);
  }

  ///***************************************************************************
  //**
  //** A function set the current target - called on AWT thread
  //
  
  public void setCurrentGaggleTarget(String gooseName) {
    targetName_ = gooseName;
    return;
  }  
  
  ///***************************************************************************
  //**
 // ** A function to transmit selections to the gaggle boss. - called on AWT thread
  //
  
  public void transmitSelections() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException();
    }
    try {
      SelectionSupport.SelectionsForSpecies sel = selSupport_.getSelections();
      ArrayList list = new ArrayList(sel.selections);
      Namelist namelist = new Namelist();
      namelist.setSpecies(sel.species);
      namelist.setNames((String[])list.toArray(new String[list.size()]));
      gaggleBoss_.broadcastNamelist(name_, targetName_, namelist);
    } catch (RemoteException rex) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(topWindow_, 
                                    rMan.getString("gaggleSupport.failedToContactBoss"), 
                                    rMan.getString("gaggleSupport.commFailureTitle"),
                                    JOptionPane.WARNING_MESSAGE);
    }
    return;
  } 

  ///***************************************************************************
 // **
  //** A function to transmit a network - called on AWT thread
 // 
  
  public void transmitNetwork(SelectionSupport.NetworkForSpecies net, boolean doOptional) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException();
    }
    try {
      HashSet seenNodes = new HashSet();
      HashSet seenEdgeNames= new HashSet();      
      StringBuffer buf = new StringBuffer();
      Network gaggleNet = new Network();

      String species = net.getSpecies();
      gaggleNet.setSpecies(species);
      Map nodeTypes = net.getNodes();
      List links = (doOptional) ? net.getLinksWithOptionalLinks() : net.getLinks();
      Iterator ltit = links.iterator();
      while (ltit.hasNext()) {
        SignedLink link = (SignedLink)ltit.next();
        String src = link.getSrc();
        String trg = link.getTrg();
        
        String typeStr = (nodeTypes.get(trg).equals(DBNode.GENE_TAG)) ? "pd" : "pp";
        int linkSign = link.getSign();
        String linkTag = DBLinkage.mapToSignTag(linkSign);
        buf.setLength(0);
        buf.append(typeStr);
        buf.append("-");
        buf.append(linkTag);
        typeStr = buf.toString();
        Interaction interact = new Interaction(src, trg, typeStr, true);
        gaggleNet.add(interact);
        
        String edgeName = interact.toString();
        if (!seenEdgeNames.contains(edgeName)) {
          gaggleNet.addEdgeAttribute(edgeName, LINK_SIGN_TAG_, linkTag);
          seenEdgeNames.add(edgeName); 
        }

        if (!seenNodes.contains(src)) {
          String nodeType = (String)nodeTypes.get(src);
          gaggleNet.addNodeAttribute(src, NODE_TYPE_TAG_, nodeType);
          seenNodes.add(src);
        }
        if (!seenNodes.contains(trg)) {
          String nodeType = (String)nodeTypes.get(trg);
          gaggleNet.addNodeAttribute(trg, NODE_TYPE_TAG_, nodeType);
          seenNodes.add(trg);
        }
      }
      
      Iterator ntit = nodeTypes.keySet().iterator();
      while (ntit.hasNext()) {
        String nodeName = (String)ntit.next();
        if (!seenNodes.contains(nodeName)) {
          String nodeType = (String)nodeTypes.get(nodeName);
          gaggleNet.addNodeAttribute(nodeName, NODE_TYPE_TAG_, nodeType);
          gaggleNet.add(nodeName);
          seenNodes.add(nodeName);
        }
      }
	    gaggleNet.setSpecies(species);
      gaggleBoss_.broadcastNetwork(name_, targetName_, gaggleNet);
    } catch (RemoteException rex) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(topWindow_, 
                                    rMan.getString("gaggleSupport.failedToContactBoss"), 
                                    rMan.getString("gaggleSupport.commFailureTitle"),
                                    JOptionPane.WARNING_MESSAGE);
    }
    return;
  } 
  
  ///***************************************************************************
 // **
 // ** A function to show the current target - called on AWT thread
 // 
  
  public void raiseCurrentTarget() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException();
    }
    try { 
      gaggleBoss_.show(targetName_);
    } catch (RemoteException rex) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(topWindow_, 
                                    rMan.getString("gaggleSupport.failedToContactBoss"), 
                                    rMan.getString("gaggleSupport.commFailureTitle"),
                                    JOptionPane.WARNING_MESSAGE);
    }    
  }
  
  ///***************************************************************************
  //**
  //** A function to hide the current target - called on AWT thread
  //
  
  public void hideCurrentTarget() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException();
    }
    try { 
      gaggleBoss_.hide(targetName_);
    } catch (RemoteException rex) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(topWindow_, 
                                    rMan.getString("gaggleSupport.failedToContactBoss"), 
                                    rMan.getString("gaggleSupport.commFailureTitle"),
                                    JOptionPane.WARNING_MESSAGE);
    }        
    return;
  }

  ///***************************************************************************
 // **
  //** A function to shut down the gaggle infrastructure - called on AWT thread
  //
  
  public void closeDown() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException();
    }
    try {
      connector_.disconnectFromGaggle(true);
      activated_ = false;
    } catch (Exception ex) {
      ex.printStackTrace();
      // We are going down, so don't display in UI
    }

    return;
  }

  ///***************************************************************************
  //**
  //** Connect to the boss (called on AWT thread)
  // dt.  Geese can now connect and disconnect independently. You may want to add
  // some UI elements to handle this. You can use connector_.isConnected() to find out
  // if you are connected, or to be notified when connection status changes, implement
  // GaggleConnectionListener, which defines a method setConnected(boolean connected, Boss boss).
  // See org.systemsbiology.gaggle.geese.sample.SampleGoose for an example.
  
  public void connectToGaggle() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException();
    }
    try {
	   //
	   //dt By default, a goose will look to see if there is a boss and if not, will start 
	   //an invisible boss using java web start. I am guessing you don't want this functionality so 
	   //I am adding the line below:
	   //
	  connector_.setAutoStartBoss(false);
	  connector_.connectToGaggle();
	  gaggleBoss_ = connector_.getBoss();
      activated_ = true;
    } catch (ConnectException ce) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(topWindow_, 
                                    rMan.getString("gaggleSupport.failedToConnectToBoss"), 
                                    rMan.getString("gaggleSupport.commFailureTitle"),
                                    JOptionPane.WARNING_MESSAGE);      
    } catch (NotBoundException nbe) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(topWindow_, 
                                    rMan.getString("gaggleSupport.failedToLookupBoss"), 
                                    rMan.getString("gaggleSupport.commFailureTitle"),
                                    JOptionPane.WARNING_MESSAGE);      
    } catch (RemoteException re) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(topWindow_, 
                                    rMan.getString("gaggleSupport.failedToContactBoss"), 
                                    rMan.getString("gaggleSupport.commFailureTitle"),
                                    JOptionPane.WARNING_MESSAGE);      
    } catch (MalformedURLException nbe) {
      throw new IllegalStateException();
    } catch (Exception ex) { // required by 2007-4 interface
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(topWindow_, 
                                    rMan.getString("gaggleSupport.failedToConnectToBoss"), 
                                    rMan.getString("gaggleSupport.commFailureTitle"),
                                    JOptionPane.WARNING_MESSAGE);
    }
    return;
  }

  ///***************************************************************************
  //**
  //** Get the goose name (Goose req'd - called on RMI thread)
  //
  
  public String getName() {
    return (name_);
  }

  ///***************************************************************************
  //**
 // ** Set the goose name (Goose req'd - called on RMI thread)
  //
  
  public void setName(String newName) {
    this.name_ = newName;
    return;
  }

  ///***************************************************************************
 // **
 // ** Hide the application (Goose req'd - called on RMI thread)
 // 
  
  public void doHide() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          topWindow_.hide();
        } catch (Exception ex) {   
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    return;
  }

  ///***************************************************************************
 // **
 // ** Show the application (Goose req'd - called on RMI thread)
 // 
  
  public void doShow() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          if (topWindow_.getExtendedState() != Frame.NORMAL) {
            topWindow_.setExtendedState (Frame.NORMAL);
          }
          MiscUtil.setJFrameAlwaysOnTop(topWindow_, true);
          topWindow_.setVisible(true);
          MiscUtil.setJFrameAlwaysOnTop(topWindow_, false);
        } catch (Exception ex) {   
           appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    return;
  }

  ///***************************************************************************
 // **
 // ** Inbound request to do a broadcast (Goose req'd - called on RMI thread)
 // 
 //
 //   * @deprecated //dt this is no longer the right way to do this but so many geese still
 //                   use it, so we were lazy and left it in the interface but marked it deprecated.
 // 
  public void doBroadcastList() {
    try {
      SelectionSupport.SelectionsForSpecies sel = selSupport_.getSelections();
      ArrayList list = new ArrayList(sel.selections);
      Namelist namelist = new Namelist();
      namelist.setSpecies(sel.species);
      namelist.setNames((String[])list.toArray(new String[list.size()]));
      gaggleBoss_.broadcastNamelist(name_, BOSS_NAME, namelist);
    } catch (Exception ex) {    
      final Exception exF = ex;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
           appState_.getExceptionHandler().displayException(exF);
        }
      });    
    }
    return;
  }

 // /***************************************************************************
 // **
 // ** Request for program to exit (Goose req'd - called on RMI thread)
 // 
 
  public void doExit() {
    try {
      connector_.disconnectFromGaggle(true);
      activated_ = false;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          try {
            topWindow_.shutdownEditor(false);
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  ///***************************************************************************
  //**
  //** Inbound request to handle a name list (Goose req'd - called on RMI thread)
 // 
  
  public void handleNameList(String source, Namelist namelist) {
    try {    
      selSupport_.addSelectionCommand(namelist.getSpecies(), namelist.getNames());
    } catch (Exception ex) {
      final Exception exF = ex;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
           appState_.getExceptionHandler().displayException(exF);
        }
      });
    }
    return;
  }

  ///***************************************************************************
  //**
  //** Inbound request to handle a matrix (Goose req'd - called on RMI thread)
  //

  public void handleMatrix(String source, DataMatrix matrix) {
    try {    
      selSupport_.addUnsupportedCommand();
    } catch (Exception ex) {
      final Exception exF = ex;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
           appState_.getExceptionHandler().displayException(exF);
        }
      });
    }
  }
  
  ///***************************************************************************
  //**
  //** Inbound request to handle a map (Goose req'd - called on RMI thread)
  //

  public void handleTuple(String source, GaggleTuple gaggleTuple) {
    try {    
      selSupport_.addUnsupportedCommand();
    } catch (Exception ex) {
      final Exception exF = ex;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
           appState_.getExceptionHandler().displayException(exF);
        }
      });
    }
  }

  ///***************************************************************************
  //**
  //** Inbound request to handle a cluster (Goose req'd - called on RMI thread)
  //

  public void handleCluster(String source, Cluster cluster) {
    try {    
      selSupport_.addUnsupportedCommand();
    } catch (Exception ex) {
      final Exception exF = ex;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
           appState_.getExceptionHandler().displayException(exF);
        }
      });
    }
  }

  ///***************************************************************************
  //**
  //** Inbound request to handle a network (Goose req'd - called on RMI thread)
 // 

  public void handleNetwork(String source, Network network) {
    try {
      HashMap typeMap = network.getNodeAttributes(NODE_TYPE_TAG_);
      HashMap signMap = network.getEdgeAttributes(LINK_SIGN_TAG_);      
      ArrayList instructionList = new ArrayList();
      Interaction[] interact = network.getInteractions();
      int numInter = interact.length;      
      for (int i = 0; i < numInter; i++) {
        String src = interact[i].getSource();
        String trg = interact[i].getTarget();
        String srcTag = null;
        String trgTag = null;
        if (typeMap != null) {
          srcTag = (String)typeMap.get(src);
          trgTag = (String)typeMap.get(trg);
        }
        if (srcTag == null) srcTag = DBNode.GENE_TAG;
        if (trgTag == null) trgTag = DBNode.GENE_TAG;
        int srcType = DBNode.mapFromTag(srcTag);
        int trgType = DBNode.mapFromTag(trgTag);
        
        String signType = null;
        if (signMap != null) {
          signType = (String)signMap.get(interact[i].toString());
        }
        if (signType == null) signType = GeneralBuildInstruction.POSITIVE_STR;
        int signVal = GeneralBuildInstruction.POSITIVE;
        try {
          signVal = GeneralBuildInstruction.mapFromSignTag(signType);
        } catch (IllegalArgumentException iaex) {
          // Do nothing...
        }
        
        BuildInstruction bi = new GeneralBuildInstruction(null, srcType, src, signVal, trgType, trg);
        instructionList.add(bi);
      }
      String[] singletons = network.getOrphanNodes();
      int numSingles = singletons.length;      
      for (int i = 0; i < numSingles; i++) {
        String src = singletons[i];
        String srcTag = null;
        if (typeMap != null) {
          srcTag = (String)typeMap.get(src);
        }
        if (srcTag == null) srcTag = DBNode.GENE_TAG;
        int srcType = DBNode.mapFromTag(srcTag);
        BuildInstruction bi = new LoneNodeBuildInstruction(null, srcType, src);
        instructionList.add(bi);
      }
      selSupport_.createNewNetwork(network.getSpecies(), instructionList);
    } catch (Exception ex) {
      final Exception exF = ex;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
           appState_.getExceptionHandler().displayException(exF);
        }
      });
    }    

  }  

  
  ///***************************************************************************
  //**
  //** Inbound notification of current geese (Goose req'd - called on RMI thread)
  //
  
  public void update(String[] updatedGeese) {     
    List gooseList = new ArrayList(Arrays.asList(updatedGeese));
    gooseList.remove(name_);
    Collections.sort(gooseList);
    selSupport_.registerNewGooseList(gooseList);
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
}
*/ // FINISH HIDE GAGGLE CODE