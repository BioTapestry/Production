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

package org.systemsbiology.biotapestry.gaggle;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;

import org.systemsbiology.biotapestry.analysis.SignedLink;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;

/****************************************************************************
**
** Used for gaggle selection support
*/

public class SelectionSupport {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private Set<String> pendingSelections_;
  private NetworkForSpecies outboundNetwork_;
  private String species_;
  private ArrayList<InboundGaggleOp> commands_;
  private ArrayList<String> gooseList_;
  private boolean initGooseList_;
  private UIComponentSource uics_;
  
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
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public SelectionSupport(UIComponentSource uics, String species) {
    uics_ = uics;
    species_ = species;
    commands_ = new ArrayList<InboundGaggleOp>();
    gooseList_ = new ArrayList<String>();    
    pendingSelections_ = new HashSet<String>();
    initGooseList_ = true;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** Set the species
  */
  
  public synchronized void setSpecies(String species) {
    species_ = species;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the species
  */
  
  public synchronized String getSpecies() {
    return (species_);
  }  
 
  /***************************************************************************
  **
  ** Get the pending command list
  */
  
  public synchronized List<InboundGaggleOp> getPendingCommands() {
    ArrayList<InboundGaggleOp> pending = commands_;
    commands_ = new ArrayList<InboundGaggleOp>();
    return (pending);
  }   

  /***************************************************************************
  **
  ** Let us know what the current goose list is
  */
  
  public synchronized void registerNewGooseList(List<String> gooseList) {
    gooseList_.clear();
    gooseList_.addAll(gooseList);
    if (initGooseList_) {
      initGooseList_ = false;
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {    
          uics_.getGaggleControls().haveGaggleGooseChange();
        }
      });
    }
    
    return;
  }
  
  /***************************************************************************
  **
  ** Get the current goose list
  */
  
  public synchronized List<String> getGooseList() {
    return (new ArrayList<String>(gooseList_));
  }  
  
  /***************************************************************************
  **
  ** Create a new network
  */
  
  public synchronized void createNewNetwork(String species, List<BuildInstruction> instruct) {
    InboundNetworkOp op = new InboundNetworkOp(species, instruct);
    commands_.add(op);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {    
        uics_.getGaggleControls().haveInboundGaggleCommands();
      }
    });
    
    return;
  } 
  
  /***************************************************************************
  **
  ** Register an unsupported command
  */
  
  public synchronized void addUnsupportedCommand() {
    InboundGaggleOp op = new InboundGaggleOp();
    commands_.add(op);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {    
        uics_.getGaggleControls().haveInboundGaggleCommands();
      }
    });    
    return;
  }   
  
  /***************************************************************************
  **
  ** Register a selection command
  */
  
  public synchronized void addSelectionCommand(String species, String[] selections) {
    HashSet<String> newSelections = new HashSet<String>(Arrays.asList(selections));
    SelectionSupport.SelectionsForSpecies sfs = new SelectionsForSpecies(species, newSelections);
    InboundSelectionOp op = new InboundSelectionOp(sfs);
    commands_.add(op);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {    
        uics_.getGaggleControls().haveInboundGaggleCommands();
      }
    });    
    return;
  }     
  
  
  /***************************************************************************
  **
  ** Clear the selections
  */
  
  public synchronized void clearSelections() { 
    SelectionSupport.SelectionsForSpecies sfs = new SelectionsForSpecies(species_, new HashSet<String>());
    InboundSelectionOp op = new InboundSelectionOp(sfs);
    commands_.add(op);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {    
        uics_.getGaggleControls().haveInboundGaggleCommands();
      }
    });
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the selections
  */
  
  public synchronized SelectionsForSpecies getSelections() {
    return (new SelectionsForSpecies(species_, new HashSet<String>(pendingSelections_)));
  }

  
  /***************************************************************************
  **
  ** Set outbound network
  */
  
  public synchronized void setOutboundNetwork(NetworkForSpecies network) {
    outboundNetwork_ = (network == null) ? null : (NetworkForSpecies)network.clone();
    if (outboundNetwork_ != null) {
      outboundNetwork_.setSpecies(species_);
    }
    return;
  }    

  /***************************************************************************
  **
  ** Get the network
  */
  
  public synchronized NetworkForSpecies getOutboundNetwork() {
    return ((outboundNetwork_ == null) ? null : (NetworkForSpecies)outboundNetwork_.clone());
  }
  
  /***************************************************************************
  **
  ** Set the selections
  */
  
  public synchronized void setSelections(List<String> selections) { 
    pendingSelections_.clear();
    pendingSelections_.addAll(selections);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the selection count
  */
  
  public synchronized int getSelectionCount() {
    return (pendingSelections_.size());
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

  /***************************************************************************
  **
  ** returns info all at once for concurrency protection
  */  
  
  public static class SelectionsForSpecies {
    public String species;
    public Set<String> selections;
    
    SelectionsForSpecies(String species, Set<String> selections) {
      this.species = species;
      this.selections = selections;
    }
  }
  
  /***************************************************************************
  **
  ** Returns a defined network
  */  
  
  public static class NetworkForSpecies implements Cloneable {
    private String species_;
    private Map<String, String> nodeTypes_;
    private List<SignedLink> links_;
    private List<SignedLink> optLinks_;
    private boolean dupNames_;
    
    public NetworkForSpecies(boolean dupNames, String species, Map<String, String> nodeTypes, List<SignedLink> reqLinks, List<SignedLink> optLinks) {
      species_ = species;
      nodeTypes_ = nodeTypes;
      links_ = reqLinks;     
      optLinks_ = optLinks;
      dupNames_ = dupNames;
    }
    
    public NetworkForSpecies() {
      this(false, null, new HashMap<String, String>(), new ArrayList<SignedLink>(), new ArrayList<SignedLink>());
    }      
        
    public NetworkForSpecies(boolean dupNames, Map<String, String> nodeTypes, List<SignedLink> reqLinks, List<SignedLink> optLinks) {
      this(dupNames, null, nodeTypes, reqLinks, optLinks);
    }
    
    public NetworkForSpecies clone() {
      try {
        NetworkForSpecies retval = (NetworkForSpecies)super.clone();
        retval.nodeTypes_ = new HashMap<String, String>(this.nodeTypes_);
        retval.links_ = new ArrayList<SignedLink>();
        int numLinks = this.links_.size();
        for (int i = 0; i < numLinks; i++) {
          retval.links_.add(this.links_.get(i).clone());
        }
        numLinks = this.optLinks_.size();
        for (int i = 0; i < numLinks; i++) {
          retval.optLinks_.add(this.optLinks_.get(i).clone());
        }        
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
      
    }   
    
    void setSpecies(String species) { 
      species_ = species;
      return;
    }   
    
    public String getSpecies() { 
      return (species_);
    }  
    
    public boolean haveDupNames() { 
      return (dupNames_);
    }       
    
    public boolean haveOptionalLinks() { 
      return ((optLinks_ != null) && !optLinks_.isEmpty());
    }        
        
    public Map<String, String> getNodes() { 
      return (nodeTypes_);
    }
    
    public List<SignedLink> getLinks() { 
      return (links_);
    }
    
    public List<SignedLink> getLinksWithOptionalLinks() {
      ArrayList<SignedLink> retval = new ArrayList<SignedLink>(links_);
      retval.addAll(optLinks_);
      return (retval);
    }      
  }  
  
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
