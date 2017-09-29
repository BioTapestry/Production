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

package org.systemsbiology.biotapestry.ui;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.MenuSource;
import org.systemsbiology.biotapestry.cmd.PopCommands;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.ui.menu.DesktopMenuFactory;
import org.systemsbiology.biotapestry.ui.menu.XPlatAction;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenu;
import org.systemsbiology.biotapestry.util.MenuItemTarget;
import org.systemsbiology.biotapestry.util.ResourceManager;

/***************************************************************************
** 
** Handles building and controlling popup menus
*/

public class PopControl {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  private BTState appState_;
  
//  private PopCommands popCmds_;
 // private boolean readOnly_;
  
  private NodePopup genePopupGuts_;
  private NodePopup geneMainGuts_;
   
  private NodePopup nodePopupGuts_;
  private NodePopup nodeMainGuts_;
  
  private LinkPopup linkPopupGuts_;
  private LinkPopup linkMainGuts_;
  
  private BasicPopup linkPointPop_;  
  private RegionPopup regionPop_;
  
  private NetModulePopup modulePop_;
  private LinkPopup moduleLinkPop_;
  private BasicPopup moduleLinkPointPop_;
  
//  private SUPanel suPanel_;
  
  private BasicPopup notPop_;
  private BasicPopup noPop_;
  
  private PopCommands.PopAction emsa_;
  private PopCommands.PopAction mergeNodes_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Constructor
  */
  
  public PopControl(BTState appState, DataAccessContext dacx) {
    appState_ = appState;
    boolean isHeadless = appState_.isHeadless();
    if (appState_.getIsEditor()) {
      if (!isHeadless) {
        emsa_ = appState_.getPopCmds().getAction(FlowMeister.PopFlow.EDIT_MULTI_SELECTIONS, null);
        mergeNodes_ =  appState_.getPopCmds().getAction(FlowMeister.PopFlow.MERGE_NODES, null);
      }
    }
    if (!isHeadless) {
      buildPopups(dacx);
    }
  }    
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** 
  */
  
  public boolean isVisible() {
    return (genePopupGuts_.isVisible() ||
            nodePopupGuts_.isVisible() ||
            linkPopupGuts_.isVisible() ||
            ((linkPointPop_ != null) && linkPointPop_.isVisible()) ||
            regionPop_.isVisible() ||
            ((notPop_ != null) && notPop_.isVisible()) ||
            modulePop_.isVisible() ||
            ((moduleLinkPop_ != null) && moduleLinkPop_.isVisible()) ||
            ((moduleLinkPointPop_ != null) && moduleLinkPointPop_.isVisible()) ||
            ((noPop_ != null) && noPop_.isVisible()));   
  }

  /***************************************************************************
  **
  ** Stock the given menu with single selection actions
  */
  
  public XPlatMenu generateMenu(Intersection intersect, DataAccessContext dacx) {
    if (intersect == null) {
      ResourceManager rMan = dacx.rMan;
      XPlatMenu xPlatEmptySel = new XPlatMenu(rMan.getString("command.selectedItemMenu"), rMan.getChar("command.selectedItemMenuMnem"));
      xPlatEmptySel.setEnabled(false);
      return (xPlatEmptySel);
    }
    GenerateOrStockResult gosr = generateOrStockMenu(null, intersect, dacx);
    return (gosr.generated);
  }
 
  /***************************************************************************
  **
  ** Stock the given menu with single selection actions
  */
  
  public boolean stockMenu(JMenu menu, Intersection intersect, DataAccessContext dacx) {
    GenerateOrStockResult gosr = generateOrStockMenu(menu, intersect, dacx);
    return (gosr.retval);
  }
   
  /***************************************************************************
  **
  ** Stock the given menu with single selection actions
  */
  
  private GenerateOrStockResult generateOrStockMenu(JMenu menu, Intersection intersect, DataAccessContext dacx) {
    PopCommands popCmds = appState_.getPopCmds();
    
    popCmds.setIntersection(intersect);
    popCmds.setAbsScreenPoint(null);
    
    Genome genome = dacx.getGenome();
    String objID = intersect.getObjectID();
    boolean readOnly = !appState_.getIsEditor();
     
    NodePopup mainGuts = null;
    boolean doGene = false;
    Gene gene = genome.getGene(objID);
    if (gene != null) { 
      mainGuts = geneMainGuts_;
      doGene = true;
    } else {
      Node node = genome.getNode(objID);
      if (node != null) {
        mainGuts = nodeMainGuts_;
      }    
    }
      
    if (mainGuts != null) {           
      MenuSource.BuildInfo bifo = new MenuSource.BuildInfo();
      bifo.conditions.put("VIEWER", Boolean.valueOf(readOnly));
      bifo.conditions.put("EDITOR", Boolean.valueOf(!readOnly));
      String overlayKey = dacx.oso.getCurrentOverlay();
      MenuSource ms = new MenuSource(appState_.getFloM(), readOnly, appState_.getDoGaggle());
      XPlatMenu xpm = ms.defineNodeMenuPopup(genome, objID, doGene, overlayKey, true, dacx);
      
      if (!appState_.isHeadless()) {
        menu.removeAll();
        mainGuts.preparePopup(xpm, bifo, new MenuItemTarget(menu));
      } else {
        ms.activateXPlatPopup(xpm, bifo, objID, intersect.getSubID(), dacx);    
      }
      return (new GenerateOrStockResult(true, xpm));
    }
    
    XPlatMenu xpm = null;
    Linkage link = genome.getLinkage(objID);
    if (link != null) {    
      if (!appState_.isHeadless()) {  
        Layout layout = dacx.getLayout();
        DataAccessContext rcx = new DataAccessContext(appState_, genome, layout);
        popCmds.setupRemoteLinkSelection(rcx, link);
        menu.removeAll();
        linkMainGuts_.preparePopup(new MenuItemTarget(menu));
      } else {
        MenuSource ms = new MenuSource(appState_.getFloM(), readOnly, appState_.getDoGaggle());
        xpm = linkMainGuts_.getMenuDef();
        MenuSource.BuildInfo bifo = new MenuSource.BuildInfo();
        bifo.conditions.put("EDITOR", Boolean.valueOf(!readOnly));
        bifo.conditions.put("VIEWER", Boolean.valueOf(readOnly));
        ms.activateXPlatPopup(xpm, bifo, objID, null, dacx);       
      }
      return (new GenerateOrStockResult(true, xpm));
    }
    
    return (new GenerateOrStockResult(false, null));
  }
  
  
   /***************************************************************************
  **
  ** Stock the given menu for multi-selection option
  */
  
  public XPlatMenu generateMultiSelMenu(DataAccessContext dacx) {
    GenerateOrStockResult gosr = generateOrStockMultiSelMenu(null, null, dacx);
    return (gosr.generated);
  }
 
  /***************************************************************************
  **
  ** Stock the given menu for multi-selection option
  */
  
  public boolean stockMultiSelMenu(JMenu menu, Map<String, Intersection> intersects, DataAccessContext dacx) {
    GenerateOrStockResult gosr = generateOrStockMultiSelMenu(menu, intersects, dacx);
    return (gosr.retval);
  }
  
  /***************************************************************************
  **
  ** Stock the given menu for multi-selection option
  */
  
  private GenerateOrStockResult generateOrStockMultiSelMenu(JMenu menu, Map<String, Intersection> intersects, DataAccessContext dacx) {
     
    if (!appState_.isHeadless()) {
      menu.removeAll();
      if (emsa_ != null) {
        menu.add(emsa_);
      }
      Genome genome = dacx.getGenome();
      Layout layout = dacx.getLayout();
      PopCommands popCmds = appState_.getPopCmds();
      HashSet<String> genes = new HashSet<String>();
      HashSet<String> nodes = new HashSet<String>();
      HashSet<String> links = new HashSet<String>();
      boolean retval = appState_.getSUPanel().getDividedSelections(genome, layout, genes, nodes, links);
      popCmds.setMultiSelections(genes, nodes, links);
      return (new GenerateOrStockResult(retval, null));
    } else {
      ResourceManager rMan = dacx.rMan;
      XPlatMenu xPlatMultiSel = new XPlatMenu(rMan.getString("command.selectedItemMenu"), rMan.getChar("command.selectedItemMenuMnem"));
      xPlatMultiSel.addItem(new XPlatAction(appState_.getFloM(), rMan, FlowMeister.PopFlow.EDIT_MULTI_SELECTIONS));     
      return (new GenerateOrStockResult(true, xPlatMultiSel));
    }
  }
  
  /***************************************************************************
  **
  ** Launch popup menu
  */
  
  public void showPopup(DataAccessContext rcx, Intersection.AugmentedIntersection aug, int x, int y, Point screenAbs) {
    PopCommands popCmds = appState_.getPopCmds();
    boolean readOnly = !appState_.getIsEditor();
    FlowMeister flom = appState_.getFloM();
    popCmds.setPopup(new Point2D.Float(x, y));
    popCmds.setIntersection(aug.intersect);
    popCmds.setAbsScreenPoint(screenAbs);
    HashSet<String> genes = new HashSet<String>();
    HashSet<String> nodes = new HashSet<String>();
    HashSet<String> links = new HashSet<String>();
    Genome genome = rcx.getGenome();
    Layout layout = rcx.getLayout();
    appState_.getSUPanel().getDividedSelections(genome, layout, genes, nodes, links); 
    popCmds.setMultiSelections(genes, nodes, links);

    //
    // Double-checking that we have something is probably overkill, but that's how it
    // was done before we went to the switch method:
    //
    switch (aug.type) {
      case Intersection.IS_GENE:
        Gene gene = rcx.getGenome().getGene(aug.intersect.getObjectID());
        if (gene != null) {
          genePopupGuts_.showPopup(new MenuSource(flom, readOnly, appState_.getDoGaggle()), rcx.getGenome(), aug.intersect, x, y, true, rcx);       
        }
        break;
      case Intersection.IS_NODE:
        Node node = rcx.getGenome().getNode(aug.intersect.getObjectID());
        if (node != null) {
          nodePopupGuts_.showPopup(new MenuSource(flom, readOnly, appState_.getDoGaggle()), rcx.getGenome(), aug.intersect, x, y, false, rcx);
        }
        break;        
      case Intersection.IS_LINK: 
        if (rcx.getGenome().getLinkage(aug.intersect.getObjectID()) != null) {
          LinkSegmentID segID = aug.intersect.segmentIDFromIntersect();
          if (!readOnly && (segID != null) && (segID.isTaggedWithEndpoint())) {
            linkPointPop_.showPopup(x, y);           
          } else {
            linkPopupGuts_.showPopup(x, y);
          }
        }
        break;        
      case Intersection.IS_NOTE:
        if (!readOnly && (rcx.getGenome().getNote(aug.intersect.getObjectID()) != null)) {
          notPop_.showPopup(x, y);
        }
        break;        
      case Intersection.IS_GROUP:
        if ((rcx.getGenome() instanceof GenomeInstance) &&
               (rcx.getGenomeAsInstance().getGroup(aug.intersect.getObjectID()) != null)) {
          regionPop_.showPopup(new MenuSource(flom, readOnly, appState_.getDoGaggle()), rcx.getGenome(), aug.intersect, x, y, rcx);
        }
        break;
      case Intersection.IS_MODULE:
        modulePop_.showPopup(new MenuSource(flom, readOnly, appState_.getDoGaggle()), rcx.getLayout(), aug.intersect, x, y, rcx);
        break;
      case Intersection.IS_MODULE_LINK_TREE:
        if (!readOnly) {
          LinkSegmentID segID = aug.intersect.segmentIDFromIntersect();
          if ((segID != null) && (segID.isTaggedWithEndpoint())) {
            moduleLinkPointPop_.showPopup(x, y);           
          } else {
            moduleLinkPop_.showPopup(x, y);
          }
        }
        break;    
      case Intersection.IS_OVERLAY:
        if (!readOnly) {
          noPop_.showPopup(x, y);
        }
        break;
      case Intersection.IS_NONE:
      default:
        throw new IllegalArgumentException();       
    }
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
        
  /***************************************************************************
  **
  ** Build popup menus
  */
  

  private void buildPopups(DataAccessContext dacx) {
 
    MenuSource ms = new MenuSource(appState_.getFloM(), !appState_.getIsEditor(), appState_.getDoGaggle());
    MenuSource.SupportedMenus supp = ms.getSupportedMenuClasses();
    
    //
    // These are built just-in-time right before display
    //
    
    genePopupGuts_ = new NodePopup();  
    geneMainGuts_ = new NodePopup();
    nodePopupGuts_ = new NodePopup();
    nodeMainGuts_ = new NodePopup();
    modulePop_ = new NetModulePopup();
    regionPop_ = new RegionPopup();
 
    try {
      linkPopupGuts_ = new LinkPopup(ms.defineLinkMenuPopup(false, dacx));  
      linkMainGuts_ = new LinkPopup(ms.defineLinkMenuPopup(true, dacx));
      moduleLinkPop_ = (supp.popupTypes.contains(MenuSource.PopType.MODULE_LINK)) ? new LinkPopup(ms.defineModLinkMenuPopup(dacx)) : null;   
      moduleLinkPointPop_ = (supp.popupTypes.contains(MenuSource.PopType.MODULE_LINK_POINT)) ? new BasicPopup(ms.defineModuleLinkPointPopup(dacx)) : null;   
      noPop_ = (supp.popupTypes.contains(MenuSource.PopType.OVERLAY)) ? new BasicPopup(ms.defineOverlayPopup(dacx)) : null;
      linkPointPop_ = (supp.popupTypes.contains(MenuSource.PopType.LINK_POINT)) ? new BasicPopup(ms.defineLinkPointPopup(dacx)) : null;
      notPop_ = (supp.popupTypes.contains(MenuSource.PopType.NOTE)) ? new BasicPopup(ms.defineNotePopup(dacx)) : null;
    } catch (MenuSource.InvalidMenuRequestException imrex) {
      throw new IllegalStateException();
    }
    return;
  } 
  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Listens for popup events
  */  
  
  private class PopupHandler implements PopupMenuListener {
    
    public void popupMenuCanceled(PopupMenuEvent e) {
      appState_.getSUPanel().forceShowingModuleComponents(null);
    }
    
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      appState_.getSUPanel().forceShowingModuleComponents(null);
      appState_.getSUPanel().requestFocusForModelInWindow();
    }
    
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    }
  }
  
  
  /***************************************************************************
  **
  ** Basic popup class
  */
   
  private class BasicPopup {
     
    protected XPlatMenu menuDef_;
    protected JPopupMenu popMenu_;
          
    /***************************************************************************
    **
    ** Build popup menu
    */
   
    BasicPopup(XPlatMenu menuDef) {
      popMenu_ = new JPopupMenu();
      popMenu_.addPopupMenuListener(new PopupHandler());
      menuDef_ = menuDef;
      return;
    }
    
    /***************************************************************************
    **
    ** Get the menu def
    */
   
    XPlatMenu getMenuDef() {
      return (menuDef_);
    }
 
    /***************************************************************************
    **
    ** Answer if visible
    */
  
    boolean isVisible() {
      return (popMenu_.isVisible());
    } 
      
    /***************************************************************************
    **
    ** Show popup menu
    */
 
    void showPopup(int x, int y) { 
      popMenu_.removeAll();  
      preparePopup(new MenuItemTarget(popMenu_));
      appState_.getSUPanel().showPopup(popMenu_, x, y);
      popMenu_.requestFocusInWindow();
      return;
    }
   
    /***************************************************************************
    **
    ** prepare the menu
    */
 
    void preparePopup(MenuItemTarget useMenu) { 
      DesktopMenuFactory dmf = new DesktopMenuFactory(appState_);
      DesktopMenuFactory.ActionSource asrc = new DesktopMenuFactory.ActionSource(appState_);
      MenuSource.BuildInfo bifo = new MenuSource.BuildInfo();
      boolean readOnly = !appState_.getIsEditor();
      bifo.conditions.put("EDITOR", Boolean.valueOf(!readOnly));
      bifo.conditions.put("VIEWER", Boolean.valueOf(readOnly));    
      dmf.buildMenuGeneral(menuDef_, asrc, bifo, useMenu);      
      Iterator<AbstractAction> cavit = bifo.allActions.iterator();
      while (cavit.hasNext()) {
        PopCommands.PopAction pa = (PopCommands.PopAction)cavit.next();
        pa.setEnabled(pa.isValid());
      }
      return;
    }   
  }  
       
  /***************************************************************************
  **
  ** Used for gene/node popup 
  */
  
  private class NodePopup {
    private JPopupMenu popMenu_;
 
    /***************************************************************************
    **
    ** Build node popup menu
    */
  
    NodePopup() {
      popMenu_ = new JPopupMenu();
      popMenu_.addPopupMenuListener(new PopupHandler());
    }
       
    /***************************************************************************
    **
    ** Answer if visible
    */
  
    boolean isVisible() {
      return (popMenu_.isVisible());
    } 

    /***************************************************************************
    **
    ** prepare popup menu
    */
  
    void preparePopup(XPlatMenu xpm, MenuSource.BuildInfo bifo, MenuItemTarget mit) {
      DesktopMenuFactory dmf = new DesktopMenuFactory(appState_);
      dmf.buildMenuGeneral(xpm, new DesktopMenuFactory.ActionSource(appState_), bifo, mit);
      Iterator<AbstractAction> cavit = bifo.allActions.iterator();
      while (cavit.hasNext()) {
        PopCommands.PopAction pa = (PopCommands.PopAction)cavit.next();
        pa.setEnabled(pa.isValid());
      }
      PopCommands.ToggleSupport pcts = (PopCommands.ToggleSupport)bifo.taggedActions.get("sltoga_");
      JCheckBoxMenuItem jcm = (JCheckBoxMenuItem)bifo.components.get("sltoga_");
      pcts.setToUpdate(true);
      jcm.setSelected(pcts.shouldCheck());
      pcts.setToUpdate(false);
   
      pcts = (PopCommands.ToggleSupport)bifo.taggedActions.get("sqntoga_");
      jcm = (JCheckBoxMenuItem)bifo.components.get("sqntoga_");
      pcts.setToUpdate(true);
      jcm.setSelected(pcts.shouldCheck());
      pcts.setToUpdate(false);        

      pcts = (PopCommands.ToggleSupport)bifo.taggedActions.get("sa2ctoga_");
      jcm = (JCheckBoxMenuItem)bifo.components.get("sa2ctoga_");
      pcts.setToUpdate(true);
      jcm.setSelected(pcts.shouldCheck());
      pcts.setToUpdate(false);
      return;
    }
    
    /***************************************************************************
    **
    ** Show popup menu
    */
  
    void showPopup(MenuSource ms, Genome genome, Intersection selected, int x, int y, boolean doGene, DataAccessContext dacx) {
      popMenu_.removeAll();    
      boolean readOnly = !appState_.getIsEditor();
      MenuSource.BuildInfo bifo = new MenuSource.BuildInfo();
      bifo.conditions.put("VIEWER", Boolean.valueOf(readOnly));
      bifo.conditions.put("EDITOR", Boolean.valueOf(!readOnly));
      String overlayKey = appState_.getCurrentOverlay();
      XPlatMenu xpm = ms.defineNodeMenuPopup(genome, selected.getObjectID(), doGene, overlayKey, false, dacx); 
      MenuItemTarget mit = new MenuItemTarget(popMenu_);
      preparePopup(xpm, bifo, mit);
      appState_.getSUPanel().showPopup(popMenu_, x, y);
      popMenu_.requestFocusInWindow();
      return;
    } 
  }
      
  /***************************************************************************
  **
  ** Used for link popup 
  */
  
  private class LinkPopup extends BasicPopup {
    
    LinkPopup(XPlatMenu menuDef) {
      super(menuDef);
    }
    
    /***************************************************************************
    **
    ** Show link popup menu
    */
  
    @Override
    void showPopup(int x, int y) {
      appState_.getPopCmds().setupNormalLinkSelection();
      super.showPopup(x, y);
      return;
    }
  }
 
  /***************************************************************************
  **
  ** Used for region popup 
  */
  
  private class RegionPopup {
    private JPopupMenu popup_;
 
    /***************************************************************************
    **
    ** Answer if visible
    */
  
    boolean isVisible() {
      return (popup_.isVisible());
    } 
       
    /***************************************************************************
    **
    ** Build region popup menu
    */
  
    RegionPopup() {
      popup_ = new JPopupMenu();
      // So much is dependent on group state, we DO NOT prebuild!
      popup_.addPopupMenuListener(new PopupHandler());
      return;
    }
    
    /***************************************************************************
    **
    ** Show region popup menu.  We build it when we are showing it!
    */
  
    void showPopup(MenuSource ms, Genome genome, Intersection selected, int x, int y, DataAccessContext dacx) {
      popup_.removeAll();
      
      GenomeInstance gi = (GenomeInstance)genome;
      MenuSource.BuildInfo bifo = new MenuSource.BuildInfo();
      boolean readOnly = !appState_.getIsEditor();
      bifo.conditions.put("VIEWER", Boolean.valueOf(readOnly));
      bifo.conditions.put("EDITOR", Boolean.valueOf(!readOnly));
      XPlatMenu xpm = ms.defineRegionPopup(gi, selected.getObjectID(), dacx);
      DesktopMenuFactory dmf = new DesktopMenuFactory(appState_);
      dmf.buildMenuGeneral(xpm, new DesktopMenuFactory.ActionSource(appState_), bifo, new MenuItemTarget(popup_));
      Iterator<AbstractAction> cavit = bifo.allActions.iterator();
      while (cavit.hasNext()) {
        PopCommands.PopAction pa = (PopCommands.PopAction)cavit.next();
        pa.setEnabled(pa.isValid());
      }
      appState_.getSUPanel().showPopup(popup_, x, y);
      popup_.requestFocusInWindow();
      return;
    }        
  }
  
  
  
  
  
  /***************************************************************************
  **
  ** Used for net module popup
  */
  
  private class NetModulePopup implements MenuListener {
    private JPopupMenu popup_;

    private String showCompsForModule_;
    private int componentPos1_;
    private int componentPos2_;
    private int componentPos3_;    
 
    /***************************************************************************
    **
    ** Build popup
    */
    
    NetModulePopup() {
      popup_ = new JPopupMenu();
      // So much is dependent on module state, we DO NOT prebuild!
      popup_.addPopupMenuListener(new PopupHandler());
    }
 
    /***************************************************************************
    **
    ** Answer if visible
    */
  
    boolean isVisible() {
      return (popup_.isVisible());
    } 
     
    /***************************************************************************
    **
    ** Handle move menu selection
    */
    
    public void menuSelected(MenuEvent e) { 
      appState_.getSUPanel().forceShowingModuleComponents(showCompsForModule_);
    }

    public void menuDeselected(MenuEvent e) {
      appState_.getSUPanel().forceShowingModuleComponents(null);
    }

    public void menuCanceled(MenuEvent e) {
      appState_.getSUPanel().forceShowingModuleComponents(null);
    }   
    
    
    /***************************************************************************
    **
    ** Show module popup menu.  We build it when we are showing it!
    */
  
    void showPopup(MenuSource ms, Layout layout, Intersection selected, int x, int y, DataAccessContext dacx) {
      popup_.removeAll();
      String overlayKey = appState_.getCurrentOverlay();
      boolean readOnly = !appState_.getIsEditor();
      MenuSource.BuildInfo bifo = new MenuSource.BuildInfo();
      bifo.conditions.put("VIEWER", Boolean.valueOf(readOnly));
      bifo.conditions.put("EDITOR", Boolean.valueOf(!readOnly));      
      NetModuleFree.IntersectionExtraInfo ei = (NetModuleFree.IntersectionExtraInfo)selected.getSubID();    
      XPlatMenu xpm = ms.defineNetModulePopup(layout, overlayKey, selected.getObjectID(), ei, dacx);
      DesktopMenuFactory dmf = new DesktopMenuFactory(appState_);
      dmf.buildMenuGeneral(xpm, new DesktopMenuFactory.ActionSource(appState_), bifo, new MenuItemTarget(popup_));
      Iterator<AbstractAction> cavit = bifo.allActions.iterator();
      while (cavit.hasNext()) {
        PopCommands.PopAction pa = (PopCommands.PopAction)cavit.next();
        pa.setEnabled(pa.isValid());
      }
      
      JMenu moveMenu = bifo.collected.get("moveMenu");
      if (moveMenu != null) {
        moveMenu.addMenuListener(this);
        
        componentPos1_ = bifo.compPosition.get("sctrb_").intValue();     
        JMenuItem dnmi = (JMenuItem)popup_.getComponent(componentPos1_);
        dnmi.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            boolean armed = ((JMenuItem)popup_.getComponent(componentPos1_)).isArmed();     
            appState_.getSUPanel().forceShowingModuleComponents((armed) ? showCompsForModule_ : null);
          }
        });     
       
        componentPos2_ = bifo.compPosition.get("dnmr_").intValue();
        JMenuItem dnmi2 = (JMenuItem)popup_.getComponent(componentPos2_);
        dnmi2.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            boolean armed = ((JMenuItem)popup_.getComponent(componentPos2_)).isArmed();     
            appState_.getSUPanel().forceShowingModuleComponents((armed) ? showCompsForModule_ : null);
          }
        });     
             
        componentPos3_ = bifo.compPosition.get("simpleMove_").intValue();
        JMenuItem dnmi3 = (JMenuItem)popup_.getComponent(componentPos3_);
        dnmi3.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            boolean armed = ((JMenuItem)popup_.getComponent(componentPos3_)).isArmed();     
            appState_.getSUPanel().forceShowingModuleComponents((armed) ? showCompsForModule_ : null);
          }
        });
        
        String moduleID = selected.getObjectID();
        NetOverlayProperties nop = layout.getNetOverlayProperties(overlayKey);
        NetModuleProperties nmp = nop.getNetModuleProperties(moduleID);
        showCompsForModule_ = (nmp.getType() != NetModuleProperties.MEMBERS_ONLY) ? moduleID : null;
      }
      
      appState_.getSUPanel().showPopup(popup_, x, y);
      popup_.requestFocusInWindow();
      return;
    }     
  }
  
  /***************************************************************************
  **
  ** Used for menu generation result
  */
  
  private static class GenerateOrStockResult  {
    boolean retval;
    XPlatMenu generated;

    GenerateOrStockResult(boolean retval, XPlatMenu generated) {
     this.retval = retval;
     this.generated = generated;
    }
  }
}
