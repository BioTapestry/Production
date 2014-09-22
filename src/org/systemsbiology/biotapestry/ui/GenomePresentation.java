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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.undo.UndoManager;

import org.systemsbiology.biotapestry.analysis.SignedLink;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.SelectionChangeCmd;
import org.systemsbiology.biotapestry.db.LocalGenomeSource;
import org.systemsbiology.biotapestry.db.ModelData;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.embedded.ExternalSelectionChangeEvent;
import org.systemsbiology.biotapestry.event.SelectionChangeEvent;
import org.systemsbiology.biotapestry.gaggle.GooseAppInterface;
import org.systemsbiology.biotapestry.gaggle.SelectionSupport;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleMember;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.Note;
import org.systemsbiology.biotapestry.nav.GroupSettings;
import org.systemsbiology.biotapestry.ui.freerender.GroupFree;
import org.systemsbiology.biotapestry.ui.freerender.LinkageFree;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleLinkageFree;
import org.systemsbiology.biotapestry.ui.freerender.NetOverlayFree;
import org.systemsbiology.biotapestry.ui.freerender.ProtoLinkFree;
import org.systemsbiology.biotapestry.ui.modelobjectcache.CommonCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForDesktop;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawLayer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** This handles the representation of a genome
*/

public class GenomePresentation implements ZoomPresentation {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  public static final int BOUNDS_PAD = 200;

  //
  // Different net module intersection targets:
  //
 
  public enum NetModuleIntersect {
    NET_MODULE_INTERIOR,
    NET_MODULE_LINK_PAD,
    NET_MODULE_BOUNDARY,
    NET_MODULE_NAME,
    NET_MODULE_DEFINITION_RECT;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String currSelZoomKey_;
  private HashMap<String, Intersection> selectionKeys_;
  private Object floater_;
  private Color floaterColor_;
  private Point2D floatPoint_;
  private Layout floaterLayout_;
  private Genome floaterGenome_;
  private HashSet<String> currentTargets_;  
  private HashSet<String> currentRootOverlays_;    
  private String nullRender_;
  private boolean processSelections_;  
  private HashMap<String, RenderObjectCache> viewerRocCache_;
  private HashMap<String, RenderObjectCache> ghostedViewerRocCache_;
  private HashMap<String, ModelObjectCache> viewerMocCache_;
  private HashMap<String, ModelObjectCache> ghostedViewerMocCache_;
  private DisplayOptionsManager dopmgr_;
  private boolean isMain_;

  LocalGenomeSource floaterSrc_;
  
  private BTState appState_;
    
  //private BufferedImage grid_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** constructor
  */

  public GenomePresentation(BTState appState, boolean isMain, double zoom, boolean processSelections, DataAccessContext rcx) {
    appState_ = (isMain) ? appState.setGenomePresentation(this) : appState;
    isMain_ = isMain;
    selectionKeys_ = new HashMap<String, Intersection>();
    currSelZoomKey_ = null;
    floater_ = null;
    floaterColor_ = null;
    floaterGenome_ = new DBGenome(appState_, "floater", "floater");
    floaterLayout_ = new Layout(appState_, "floater", "floater");
    currentTargets_ = new HashSet<String>(); 
    currentRootOverlays_ = new HashSet<String>();     
    viewerRocCache_ = new HashMap<String, RenderObjectCache>();
    ghostedViewerRocCache_ = new HashMap<String, RenderObjectCache>();
    viewerMocCache_ = new HashMap<String, ModelObjectCache>();
    ghostedViewerMocCache_ = new HashMap<String, ModelObjectCache>();
    processSelections_ = processSelections;
    dopmgr_ = appState_.getDisplayOptMgr();
        
    /*
    grid_ = new BufferedImage(2000, 1600, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = grid_.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  
                        RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(Color.white);
    g2.fillRect(0, 0, 2000, 1600);    
    g2.setPaint(Color.GRAY);
    for (int i = 0; i < 2000; i += 10) {
      for (int j = 0; j < 1600; j += 10) {
        Line2D line = new Line2D.Float(i, j, i, j);
        g2.draw(line);
      }
    }
    */
    setPresentationZoomFactor(zoom, rcx);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Answer if nothing misc has been intersected. NOTE we should be handed an OSO instead; this
  ** is tied to appState_'s current overlay state!
  */
  
  public boolean noMiscHits(DataAccessContext rcx, Point pt) {
    String currentOverlay = rcx.oso.getCurrentOverlay();
    TaggedSet currentNetMods = rcx.oso.getCurrentNetModules();
    NetModuleFree.CurrentSettings currOvrSettings = rcx.oso.getCurrentOverlaySettings();

    return ((intersectModelData(rcx, pt, currentOverlay, currentNetMods.set, currOvrSettings.intersectionMask) == null) &&
          (!intersectLinkageLabels(rcx, pt)) &&
          (intersectANetModuleElement(pt.x, pt.y, rcx, NetModuleIntersect.NET_MODULE_BOUNDARY) == null) &&
          (!appState_.showingModuleComponents() || (intersectANetModuleElement(pt.x, pt.y, rcx, NetModuleIntersect.NET_MODULE_DEFINITION_RECT) == null)) &&
          (intersectNetModuleLinks(pt.x, pt.y, rcx) == null) &&
          (intersectANetModuleElement(pt.x, pt.y, rcx, NetModuleIntersect.NET_MODULE_NAME) == null));
  }

  /***************************************************************************
  **
  ** Move commands need raw keys. Otherwise, do not use!
  */
  
  public Map<String, Intersection> getRawSelectionKeys() {
    return (selectionKeys_);
  }
  
  /***************************************************************************
  **
  ** Answer if links are not currently visible
  */
  
  public boolean linksAreHidden(DataAccessContext rcx) {
    boolean hideLinks = false;
    if ((rcx.oso != null) && (rcx.oso.getCurrentOverlay() != null)) {
      NetOverlayProperties nop = rcx.getLayout().getNetOverlayProperties(rcx.oso.getCurrentOverlay());
      hideLinks = nop.hideLinks();  
    }
    return (hideLinks);
  }
  
  /***************************************************************************
  **
  ** Present the given genome with the given layout
  */
  
	public void presentGenome(ModelObjectCache moc, DataAccessContext rcx, boolean showRoot) { // String genomeKey,Layout layout, boolean showBubbles, boolean showRoot,OverlayStateOracle oso, double pixDiam) {
		presentGenomeWithOverlay(moc, null, null, null, null, rcx, showRoot, null);
		return;
	}
  
  /***************************************************************************
  **
  ** Present the given genome with the given layout
  */
  
  public void presentGenomeWithOverlay(ModelObjectCache moc, ModelObjectCache overMoc, ModelObjectCache floaterCache, 
      																 Graphics2D g2, OpaqueOverlayInfo ooi, DataAccessContext rcxI,
      																 boolean showRoot, Set<String> showComponents) {
		//
		// This is done in up to two passes
		//

		// overMoc.setDrawLayer(DrawLayer.OVERLAY);

		DataAccessContext rcx = new DataAccessContext(rcxI);
			
		List<GroupFree.ColoredRect> renderedRects = null;
		
		boolean hideLinks = false;
		if (rcx.getGenome() instanceof GenomeInstance) { // yuk! gag!
			GenomeInstance child = rcx.getGenomeAsInstance();
			GenomeInstance rootvfg = child.getVfgParentRoot();
			//
			// When in pull-down mode, we only want to show the immediate parent
			// to see available elements, not the vfg root with all elements:
			//
			GenomeInstance ancestorVfg = (showRoot) ? rootvfg : child.getVfgParent();
			// Shows background for regions actually in the model:
			if ((rcx.oso != null) && (rcx.oso.getCurrentOverlay() != null)) {
				renderedRects = new ArrayList<GroupFree.ColoredRect>();
			}
			else if (rcx.oso != null && rcx.forWeb) {
				renderedRects = new ArrayList<GroupFree.ColoredRect>();
			}

			moc.setDrawLayer(DrawLayer.BACKGROUND_REGIONS);
			renderGroupsInBackground(moc, rcx, rootvfg, child, renderedRects);

			moc.setDrawLayer(DrawLayer.UNDERLAY);
			hideLinks = underlayRender(moc, null, null, showComponents, rcx);

			if (rootvfg != null) {
				// I.E. if not a top-level instance, we draw inactive parent under the
				// model:
				moc.setDrawLayer(DrawLayer.VFN_GHOSTED);
				DataAccessContext rcx2 = new DataAccessContext(rcx, ancestorVfg);
				rcx2.pushGhosted(true);
				presentationGuts(moc, null, false, hideLinks, rcx2);
				rcx2.popGhosted();

				// And then ghosted groups not in the model:
				moc.setDrawLayer(DrawLayer.VFN_UNUSED_REGIONS);
			  DataAccessContext rcxR = new DataAccessContext(rcxI, rootvfg);
				presentUnusedGroups(moc, rcxR, ancestorVfg, child);
			}

			// Draw the actual contents of the model:
			moc.setDrawLayer(DrawLayer.MODEL_NODEGROUPS);

			// linkRoc = presentationGuts(moc, null, genome, layout, false,
			// showBubbles, false, hideLinks, clipRect, pixDiam);
			presentationGuts(moc, null, false, hideLinks, rcx);

			// used to obscure toggled groups:
			moc.setDrawLayer(DrawLayer.FOREGROUND_REGIONS);
			renderGroupsInForeground(moc, rcx, rootvfg, child);
		} else {
			// TODO check that this is the correct layer
			moc.setDrawLayer(DrawLayer.UNDERLAY);
			hideLinks = overlayRender(moc,  null, null, true, showComponents, rcx);

			// TODO check that this is the correct layer
			moc.setDrawLayer(DrawLayer.MODEL_NODEGROUPS);

			// linkRoc = presentationGuts(moc, null, layout, false, showBubbles,
			// false, hideLinks, clipRect, pixDiam);
			presentationGuts(moc, null, false, hideLinks, rcx);
		}

		//
		// Why is the semi-opaque overlay being rendered to an image that is then
		// composited?
		// (I should have written this comment a few months ago!) If I recollect,
		// this was
		// a _lot_ faster for display with alpha < 1.0:
		//

		/*
		 * overlayRender((ooi != null) ? ooi.overlayLayerG2 : g2, genome, layout,
		 * renderedRects, linkRoc, oso, false, showComponents, showBubbles,
		 * clipRect, pixDiam); if (ooi != null) { ooi.renderImage(g2); }
		 */

		if (overMoc != null) {
			// TODO remove null ROC parameter
			// overlayRender sets the drawLayer for the cache
			overlayRender(overMoc, renderedRects, null, false, showComponents, rcx);
		}
		
		moc.setDrawLayer(DrawLayer.MODELDATA);
		rcx.pushGhosted(false);
		renderModelData(moc, rcx);
		rcx.popGhosted();
		
		if (floaterCache != null) {
			floaterCache.setDrawLayer(DrawLayer.MODEL_NODEGROUPS);
			DataAccessContext rcxF = new DataAccessContext(rcxI);
			rcxF.setLayout(floaterLayout_);
			rcxF.setGenome(floaterGenome_);
			rcxF.setGenomeSource((floaterSrc_ != null) ? floaterSrc_ : new LocalGenomeSource((DBGenome)floaterGenome_, new ArrayList<Genome>()));
			renderFloater(floaterCache, rcxF);
		}
		
		return;
	}

  
  /***************************************************************************
  **
  ** Handle overlay rendering:
  */
  
  private boolean underlayRender(ModelObjectCache moc,
                                 List<GroupFree.ColoredRect> renderedRects, 
                                 RenderObjectCache roc,
                                 Set<String> showComponents, 
                                 DataAccessContext rcx) {
  	
    boolean hideLinks = false;
    
    if ((rcx.oso != null) && (rcx.oso.getCurrentOverlay() != null)) {
      String genomeKey = rcx.getGenomeID();
      NetOverlayOwner owner = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(genomeKey);
      NetworkOverlay no = owner.getNetworkOverlay(rcx.oso.getCurrentOverlay());
      NetOverlayProperties nop = rcx.getLayout().getNetOverlayProperties(rcx.oso.getCurrentOverlay());
      hideLinks = nop.hideLinks();
      NetOverlayFree nof = nop.getRenderer();
      nof.render(moc, no, renderedRects, true, showComponents, rcx);
    }
    
    return (hideLinks);
  }
  
  /***************************************************************************
  **
  ** Handle overlay rendering:
  */
  private boolean overlayRender(ModelObjectCache moc,
      List<GroupFree.ColoredRect> renderedRects, RenderObjectCache roc,
      boolean doUnderlay, 
      Set<String> showComponents, DataAccessContext rcx) {
  	
  	// TODO correct layers
  	Integer minorLayer = 0;
  	Integer majorLayer = 0;
  	
  	Layout layout = rcx.getLayout();
  	Genome genome = rcx.getGenome();
  	
    boolean hideLinks = false;
    String genomeKey = genome.getID();
    NetOverlayOwner owner = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(genomeKey);
    
    // In the web application, loop through every overlay and render it, so that all overlays
    // can be packaged and exported at the same time.
    if (appState_.isWebApplication()) {
    	for (Iterator<NetworkOverlay> noi = owner.getNetworkOverlayIterator(); noi.hasNext();) {
    		NetworkOverlay no = noi.next();
    		
    		if (no != null) {
        		// Create a DataAccessContext with a OverlayStateOracle configured such that
        		// the overlay being rendered is enabled in the DataAccessContext.
        		// The NetModuleLinkages in each overlay are rendered only if the overlay is enabled.
    			
    			// Enable all overlay modules for rendering    			
    			TaggedSet enabledModules = new TaggedSet();
    			for(Iterator<NetModule> nmit = no.getModuleIterator(); nmit.hasNext();){
    				NetModule module = nmit.next();
    				enabledModules.set.add(module.getID());
    			}
    			
    			FreezeDriedOverlayOracle enabledOverlayOracle = new FreezeDriedOverlayOracle(no.getID(), enabledModules, NetModuleFree.CurrentSettings.NOTHING_MASKED, null);
    			
    			DataAccessContext overlayAccessContext = new DataAccessContext(rcx);
    			overlayAccessContext.oso = enabledOverlayOracle;
    			
        		NetOverlayProperties nop = layout.getNetOverlayProperties(no.getID());
        		// For underlays to be rendered, the type of the overlay has to be OvrType.UNDERLAY
        		// and the doUnderlay parameter to renderNetworkOverlay has to be true.
        		if (nop.getType() == NetOverlayProperties.OvrType.UNDERLAY) {
        			renderNetworkOverlay(moc, no, renderedRects, layout, true, showComponents, overlayAccessContext);
        		}
        		else {
        			renderNetworkOverlay(moc, no, renderedRects, layout, doUnderlay, showComponents, overlayAccessContext);
        		}
    			
    		}
    	}
    }
    else if ((rcx.oso != null) && (rcx.oso.getCurrentOverlay() != null)) {
      NetworkOverlay no = owner.getNetworkOverlay(rcx.oso.getCurrentOverlay());
      hideLinks = renderNetworkOverlay(moc, no, renderedRects, layout,
			doUnderlay, showComponents, rcx);
              
      //
      // Drill holes for selection:
      //
      
      if (doUnderlay || !overlayIsOpaque(layout, rcx.oso.getCurrentOverlay()) || selectionKeys_.isEmpty()) { 
        return (hideLinks);
      }
      
      moc.setDrawLayer(DrawLayer.MODEL_NODEGROUPS);
      Iterator<Node> nit = genome.getAllNodeIterator();
      while (nit.hasNext()) {
        Node node = nit.next();
        if (node.getID().equals(nullRender_)) {
          continue;
        }
        Intersection selected = isSelected(selectionKeys_, node);
        if (selected == null) {
          continue;
        }
        IRenderer render = layout.getNodeProperties(node.getID()).getRenderer(); 
        // TODO correct cache
        rcx.pushGhosted(false);
        boolean oldShow = rcx.showBubbles;
        rcx.showBubbles = false;
        render.render(moc, node, selected, rcx, null);
        rcx.showBubbles = oldShow;
        rcx.popGhosted();
      }
      if (roc != null) {
        //roc.renderALayer(g2, DrawTree.getSelectionLayerKey());
      }
      // g2.setComposite(AlphaComposite.SrcOver);
      
      // TODO this needs to happen after roc.renderALayer(g2, DrawTree.getSelectionLayerKey());
      // sc = new ModelObjectCache.SetComposite(AlphaComposite.SrcOver);
      // group.addShape(sc, majorLayer, minorLayer);
      
       //moc.addGroup(group);
    }
    
    return (hideLinks);
  }

	private boolean renderNetworkOverlay(ModelObjectCache moc,
			NetworkOverlay no, List<GroupFree.ColoredRect> renderedRects,
			Layout layout, boolean doUnderlay, Set<String> showComponents,
			DataAccessContext rcx) {
		boolean hideLinks;
		// NetOverlayProperties nop = layout.getNetOverlayProperties(rcx.oso.getCurrentOverlay());
		
		NetOverlayProperties nop = layout.getNetOverlayProperties(no.getID());
		hideLinks = nop.hideLinks();
		NetOverlayFree nof = nop.getRenderer();

		moc.setDrawLayer(DrawLayer.OVERLAY);

		nof.render(moc, no, renderedRects, doUnderlay, showComponents, rcx);
		return hideLinks;
	}
  
  /***************************************************************************
  **
  ** Present the given genome with the given layout
  */
  
  public void presentRootGenome(ModelObjectCache moc, DataAccessContext rcx) { //String genomeKey, Layout layout, boolean isGhosted, double pixDiam) {
		if (rcx.getGenome() instanceof GenomeInstance) {
			throw new IllegalArgumentException();
		} else {
			presentationGuts(moc, null, false, false, rcx);
		}
		return;
	}
  
  /***************************************************************************
  **
  ** Get the bounds of all notes
  */
  
  public Map<String, Rectangle> getNoteBounds(Graphics2D g2, DataAccessContext rcx) {

    HashMap<String, Rectangle> retval = new HashMap<String, Rectangle>();
    //
    // Crank through notes and get the bounds for each
    //
    
    rcx.pushFrc(g2.getFontRenderContext());
    Iterator<Note> noit = rcx.getGenome().getNoteIterator();    
    while (noit.hasNext()) {
      Note note = noit.next();
      IRenderer render = rcx.getLayout().getNoteProperties(note.getID()).getRenderer();
      Rectangle bounds = render.getBounds(note, rcx, null);
      retval.put(note.getID(), bounds);
    }
    rcx.popFrc();
    
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Get the bounds of all nodes
  */
  
  public Map<String, Rectangle> getNodeBounds(Graphics2D g2, DataAccessContext rcx) {

    HashMap<String, Rectangle> retval = new HashMap<String, Rectangle>();
    //
    // Crank through nodes and get the bounds for each
    //
    
    rcx.pushFrc(g2.getFontRenderContext());
    Iterator<Node> noit = rcx.getGenome().getAllNodeIterator();    
    while (noit.hasNext()) {
      Node node = noit.next();
      IRenderer render = rcx.getLayout().getNodeProperties(node.getID()).getRenderer();
      Rectangle bounds = render.getBounds(node, rcx, null);
      retval.put(node.getID(), bounds);
    }
    rcx.popFrc();
    
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Bump to the next selection for zoom
  */
  
  public void bumpPreviousSelection(DataAccessContext rcx) {
    bumpGuts(false, rcx);
    return;    
  } 
    
  /***************************************************************************
  **
  ** Bump to the next selection for zoom
  */
  
  public void bumpNextSelection(DataAccessContext rcx) {
    bumpGuts(true, rcx);
    return;
  } 
  
  /***************************************************************************
  **
  ** Bump selection for zoom
  */
  
  private void bumpGuts(boolean up, DataAccessContext rcx) {
    currSelZoomKey_ = calcSelectionKey(up, currSelZoomKey_, rcx);
    return;    
  } 
  
  /***************************************************************************
  **
  ** Bump selection for zoom
  */
  
  private String calcSelectionKey(boolean up, String baseKey, DataAccessContext rcx) {
    if (selectionKeys_.isEmpty()) {
      return (null);
    }
    Set<String> justNodes = justNodeKeys(rcx, selectionKeys_.keySet());
    
    TreeSet<String> order = new TreeSet<String>(justNodes);
    if (baseKey == null) {
      baseKey = (up) ? order.first() : order.last();
      return (baseKey);
    }
    Iterator<String> oit = order.iterator();
    String last = null;
    while (oit.hasNext()) {
      String nextO = oit.next();
      if (baseKey.equals(nextO)) {
        if (up) {
          if (oit.hasNext()) {
            baseKey = oit.next();
          } else {
            baseKey = order.first();
          }
          return (baseKey);
        } else {
          if (last == null) {
            baseKey = order.last();
          } else {
            baseKey = last;
          }
          return (baseKey);
        }
      }
      last = nextO;
    }
    return (null);    
  } 
  
  
  /***************************************************************************
  **
  ** When iterating through a _mixture_ of link and node selections, we JUST
  ** do nodes!
  */
  
  private boolean selectionIsMixed(DataAccessContext rcx, Set<String> selectionKeys) {
    if (selectionKeys.isEmpty()) {
      return (false);
    } 
    Genome genome = rcx.getGenome();
    Iterator<String> ksit = selectionKeys.iterator();
    boolean haveNode = false;
    boolean haveLink = false;
    while (ksit.hasNext()) {
      String objID = ksit.next();
      if (genome.getLinkage(objID) != null) {
        haveLink = true;
      } else if (genome.getNode(objID) != null) {
        haveNode = true;
      }
      if (haveLink && haveNode) {
        return (true);
      }
    }
    return (false);    
  } 
  
  /***************************************************************************
  **
  ** When iterating through a _mixture_ of link and node selections, we JUST
  ** do nodes!
  */
  
  private Set<String> justNodeKeys(DataAccessContext rcx, Set<String> selectionKeys) {
    if (!selectionIsMixed(rcx, selectionKeys)) {
      return (selectionKeys);
    }
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> ksit = selectionKeys.iterator();
    while (ksit.hasNext()) {
      String objID = ksit.next(); 
      if (rcx.getGenome().getNode(objID) != null) {
        retval.add(objID);
      }
    }
    return (retval);    
  }

  /***************************************************************************
  **
  ** Answer if we have a selection
  */
  
  public boolean hasASelection() {
    return (!selectionKeys_.isEmpty());
  }  
  
  /***************************************************************************
  **
  ** Set the zoom
  */
  
  public void setPresentationZoomFactor(double zoom, DataAccessContext rcx) {
    AffineTransform trans = new AffineTransform();
    trans.scale(zoom, zoom);
    rcx.setFrc(new FontRenderContext(trans, true, true));
    if (isMain_) {
      appState_.setFontRenderContext(rcx.getFrc());
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if we have a current selection
  */
  
  public boolean haveCurrentSelection() {  
    return (currSelZoomKey_ != null);
  }
  
  /***************************************************************************
  **
  ** Answers if we multiple selections (i.e. can cycle through selections) 
  */
  
  public boolean haveMultipleSelections() {  
    return (selectionKeys_.size() > 1);
  }
  
  /***************************************************************************
  **
  ** Clear the selection
  */
  
  public boolean clearSelections(DataAccessContext rcx, UndoSupport support) {
    if (selectionKeys_.isEmpty()) {
      return (false);
    }
    SelectionChange sc = preSelect();
    
    selectionKeys_.clear();
    currSelZoomKey_ = null;

    if (support == null) {
      throw new IllegalArgumentException();
    }
    postSelect(rcx, sc, support, null, false, null);
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Drop node selections from current selection set
  */
  
  public void dropNodeSelections(DataAccessContext rcx, Integer nodeType, UndoManager undom) {
    if (selectionKeys_.isEmpty()) {
      return;
    }
    SelectionChange sc = preSelect();
    
    HashSet<String> keySet = new HashSet<String>(selectionKeys_.keySet());
    Iterator<Node> git = rcx.getGenome().getAllNodeIterator();    
    while (git.hasNext()) {
      Node node = git.next();
      if ((nodeType == null) || (node.getNodeType() == nodeType.intValue())) {
        String nodeID = node.getID();
        if (keySet.contains(nodeID)) {
          selectionKeys_.remove(nodeID);
        }
      }
    }
    currSelZoomKey_ = null;
    
    postSelect(rcx, sc, null, null, false, undom);
    return; 
  }
  
  /***************************************************************************
  **
  ** Drop link selections from current selection set
  */
  
  public void dropLinkSelections(DataAccessContext rcx, SelectionChangeCmd.Bundle bundle, UndoManager undom) {
    if (selectionKeys_.isEmpty()) {
      return;
    }
    SelectionChange sc = preSelect();
    
    HashSet<String> keySet = new HashSet<String>(selectionKeys_.keySet());       
    Iterator<Linkage> git = rcx.getGenome().getLinkageIterator();    
    while (git.hasNext()) {
      Linkage link = git.next();
      String linkID = link.getID();
      if (keySet.contains(linkID)) {
        selectionKeys_.remove(linkID);
      }
    }
    currSelZoomKey_ = null;
    
    postSelect(rcx, sc, null, bundle, false, undom);
    return; 
  }  
  
  /***************************************************************************
  **
  ** Return the required size of the layout
  */
  
  public Rectangle getRequiredSize(DataAccessContext rcx, boolean doModules, String ovrKey, 
                                   TaggedSet modSet, Map<String, Layout.OverlayKeySet> allKeys) {    
    //
    // We traditionally return the "quick" version
    //
    return (getRequiredSize(rcx, false, false, doModules, doModules, ovrKey, modSet, allKeys));    
  }
  
  /***************************************************************************
  **
  ** Return the required size of the layout
  */
  
  public Rectangle getRequiredSize(DataAccessContext rcx, 
                                   boolean doComplete, boolean useBuffer, 
                                   boolean doModules, boolean doModuleLinks, 
                                   String ovrKey, TaggedSet modSet, Map<String, Layout.OverlayKeySet> allKeys) {
    //
    // Crank through the genes and nodes and have the renderer return the
    // bounds.
    //

    Rectangle backupRetval = new Rectangle(1950, 1450, 100, 100);
    Rectangle retval = null;
    
    Genome genome = rcx.getGenome();
    
    if ((genome == null) || (rcx.getLayout() == null)) {
      return (backupRetval);
    }

    String ovrOwnerKey = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(rcx.getGenomeID()).getID();
    // Layout KNOWS who it is for these days!
   // if (genome instanceof GenomeInstance) { //yuk!  gag!
   //   GenomeInstance parentvfg = ((GenomeInstance)genome).getVfgParentRoot();      
   //   if (parentvfg != null) {
   //     genome = parentvfg;
   //   }
   // }  
    
    Layout.OverlayKeySet myKeys = null;
    if (allKeys != null) {
      myKeys = allKeys.get(rcx.getLayoutID());
    }
    
    retval = rcx.getLayout().getLayoutBounds(rcx, doComplete, doModules, doModuleLinks, true, true, ovrOwnerKey, ovrKey, modSet, myKeys);
    
    //
    // Make sure we return something
    //
    
    if (retval == null) {
      retval = backupRetval;
    } 
    
    if (doComplete) {
      Rectangle modRect = getModelDataBounds(rcx);
      if (modRect != null) {
        Bounds.tweakBounds(retval, modRect);
      }
    }

    //
    // Pad the outside
    //
  
    if (useBuffer) {
      Bounds.padBounds(retval, BOUNDS_PAD, BOUNDS_PAD);
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Return the required size of the selected items.  Null if nothing is
  ** selected.
  */
  
  public Rectangle getSelectionSize(DataAccessContext rcx) {   
    return (getSelectionSizeGuts(rcx, null));
  }   
  
  /***************************************************************************
  **
  ** Return the required size of the next zoom selection item.  Null if nothing is
  ** selected.
  */
  
  public Rectangle getCurrentSelectionSize(DataAccessContext rcx) {
    return (getSelectionSizeGuts(rcx, currSelZoomKey_));
  }  
  
  /***************************************************************************
  **
  ** Return the required size of the selected items.  Null if nothing is
  ** selected.
  */
  
  private Rectangle getSelectionSizeGuts(DataAccessContext rcx, String oneKey) {

    Rectangle retval = null;
    
    if (rcx.getGenome() == null) {
      return (retval);
    } 
    if (rcx.getGenome() instanceof GenomeInstance) { //yuk!  gag!
      rcx = new DataAccessContext(rcx, rcx.getRootInstance());
    }    
    
    HashSet<String> selectedNodes = new HashSet<String>();
    HashSet<String> selectedLinks = new HashSet<String>();
    
    Set<String> keySet = selectionKeys_.keySet();
    
    Iterator<Node> git = rcx.getGenome().getAllNodeIterator();    
    while (git.hasNext()) {
      Node node = git.next();
      String nodeID = node.getID();
      if (keySet.contains(nodeID)) {
        if ((oneKey == null) || oneKey.equals(nodeID)) {
          selectedNodes.add(nodeID);
        }
      }
    }
    
    Map subLinks = new HashMap();
    Iterator<Linkage> lit = rcx.getGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      if (keySet.contains(linkID)) {
        if ((oneKey == null) || oneKey.equals(linkID)) {
          Intersection linkSel = selectionKeys_.get(linkID);
          if (linkSel.getSubID() != null) {
            subLinks.put(linkID, linkSel.getSubID());
          }
          selectedLinks.add(linkID);
        }
      }
    }    

    retval = rcx.getLayout().getPartialBounds(selectedNodes, true, true, selectedLinks, subLinks, true, rcx);  
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Intersect an item
  */
  
  public List<Intersection.AugmentedIntersection> intersectItem(int x, int y, DataAccessContext rcx, boolean nodesFirst, 
                                                                boolean doOverlay) {
    return (selectionGuts(x, y, rcx, nodesFirst, false, doOverlay));
  }
  
  /***************************************************************************
  **
  ** Intersect items with a box
  */
  
  public List<Intersection> intersectItems(Rectangle rect, DataAccessContext rcx) {
    return (selectionGuts(rect, rcx, true));
  } 
  
  /***************************************************************************
  **
  ** Answer if we intersect any net modules
  */
  
  public List<String> intersectNetModules(int x, int y, DataAccessContext rcxM, String ovrKey, Set<String> modKeys) {

    ArrayList<String> retval = new ArrayList<String>();
    if (ovrKey == null) {
      return (retval);
    }
    Genome genome = rcxM.getGenome();
    //
    // We use the rootInstance to drive rendering, so that non-included nodes are not
    // shown as part of the module (important for dynamic instances!)
    //
    Genome useGenome = genome;
    DynamicInstanceProxy dip = null;
    if (genome instanceof GenomeInstance) {
      useGenome = rcxM.getRootInstance();
      if (genome instanceof DynamicGenomeInstance) {
        dip = rcxM.getGenomeSource().getDynamicProxy(((DynamicGenomeInstance)genome).getProxyID());
      }
    }

    Point2D pt = new Point2D.Float(x, y);
    NetOverlayOwner owner = rcxM.getGenomeSource().getOverlayOwnerFromGenomeKey(genome.getID());
    Set<String> useGroups = owner.getGroupsForOverlayRendering();
    NetworkOverlay novr = owner.getNetworkOverlay(ovrKey);
    NetOverlayProperties nop = rcxM.getLayout().getNetOverlayProperties(ovrKey);
    Iterator<NetModule> mit = novr.getModuleIterator();
    DataAccessContext rcx = new DataAccessContext(rcxM, useGenome, rcxM.getLayout());
    while (mit.hasNext()) {
      NetModule mod = mit.next();
      String id = mod.getID();
      if (modKeys.contains(id)) {
        NetModuleProperties nmp = nop.getNetModuleProperties(id);      
        if (nmp.getRenderer().intersects(dip, useGroups, mod, ovrKey, pt, rcx)) {
          retval.add(id);
        }
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Return an Intersection for first intersected net module:
  */
  
  public Intersection intersectANetModuleElement(int x, int y, DataAccessContext rcxN, NetModuleIntersect type) {
 
    String ovrKey = rcxN.oso.getCurrentOverlay();
    if (ovrKey == null) {
      return (null);
    }
    TaggedSet currentNetMods = rcxN.oso.getCurrentNetModules();
    Set<String> modKeys = (currentNetMods == null) ? null : currentNetMods.set;
    NetModuleFree.CurrentSettings settings = rcxN.oso.getCurrentOverlaySettings();  

    Genome genome = rcxN.getGenome();
    //
    // We use the rootInstance to drive rendering, so that non-included nodes are not
    // shown as part of the module (important for dynamic instances!)
    //
    Genome useGenome = genome;
    DynamicInstanceProxy dip = null;
    if (genome instanceof GenomeInstance) {
      useGenome = rcxN.getRootInstance();
      if (genome instanceof DynamicGenomeInstance) {
        dip = rcxN.getGenomeSource().getDynamicProxy(((DynamicGenomeInstance)genome).getProxyID());
      }
    } 
    Point2D pt = new Point2D.Float(x, y);
    NetOverlayOwner owner = rcxN.getGenomeSource().getOverlayOwnerFromGenomeKey(rcxN.getGenomeID());
    Set<String> useGroups = owner.getGroupsForOverlayRendering();
    NetworkOverlay novr = owner.getNetworkOverlay(ovrKey);
    NetOverlayProperties nop = rcxN.getLayout().getNetOverlayProperties(ovrKey);
    boolean isOpq = (nop.getType() == NetOverlayProperties.OvrType.OPAQUE);
    Iterator<NetModule> mit = novr.getModuleIterator();
    
    DataAccessContext rcxU = new DataAccessContext(rcxN, useGenome, rcxN.getLayout());
    
    while (mit.hasNext()) {
      NetModule mod = mit.next();
      String id = mod.getID();
      NetModuleProperties nmp = nop.getNetModuleProperties(id);   
      Intersection retval = null;
      if (modKeys.contains(id)) {
        NetModuleFree render = nmp.getRenderer();
        switch (type) {
          case NET_MODULE_INTERIOR:
            retval = render.intersectsInterior(dip, useGroups, mod,  ovrKey, rcxU, pt);
            break;
          case NET_MODULE_LINK_PAD:
            retval = render.intersectsLinkPad(dip, useGroups, mod, ovrKey, rcxU, pt);
            break;
          case NET_MODULE_BOUNDARY:
            retval = render.intersectsBoundary(dip, useGroups, mod, ovrKey, rcxU, pt);
            break;
          case NET_MODULE_NAME:
            // Name fading is tricky.  Handle all the special cases here.
            boolean isRevealed = rcxU.oso.getRevealedModules().set.contains(id);
            boolean quickFade = (nmp.getNameFadeMode() == NetModuleProperties.FADE_QUICKLY);
            boolean nameHiding = (isRevealed && isOpq && quickFade);
            boolean labelViz = !nameHiding && ((quickFade) ? settings.fastDecayLabelVisible : true);            
            if (labelViz) {
              retval = render.intersectsName(mod, ovrKey, rcxU, pt);
            }
            break;
          case NET_MODULE_DEFINITION_RECT:  
            retval = render.intersectsDefinitionBoundary(mod, ovrKey, rcxU, pt); 
            break;   
          default:
            throw new IllegalArgumentException();
        }
        if (retval != null) {
          return (retval);
        }
      }
    }
    return (null);
  }    
  
  /***************************************************************************
  **
  ** Answer if we intersect any net module links
  */
  
  public Intersection intersectNetModuleLinks(int x, int y, DataAccessContext rcxN) {
    String ovrKey = rcxN.oso.getCurrentOverlay();
    
    if (ovrKey == null) {
      return (null);
    }
    // Though use the rootInstance to drive rendering for modules, genome key is being used
    // in link rendering to derive overlay owner, so don't fool with it!
    Point2D pt = new Point2D.Float(x, y);
    NetOverlayProperties nop = rcxN.getLayout().getNetOverlayProperties(ovrKey);
    
    Iterator<String> nlit = nop.getNetModuleLinkagePropertiesKeys();   
    while (nlit.hasNext()) {
      String treeID = nlit.next();
      NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(treeID);
      NetModuleLinkageFree nmlf = (NetModuleLinkageFree)nmlp.getRenderer();
      Intersection retval = nmlf.intersects(treeID, ovrKey, rcxN, pt);
      if (retval != null) {
        return (retval);
      }
    }    
    return (null);
  }    
 
  /***************************************************************************
  **
  ** Select a full link
  */
  
  public void selectFullItem(Intersection intersect, DataAccessContext rcx, UndoManager undom) {
    SelectionChange sc = preSelect(); 

    String itemID = intersect.getObjectID();
    Intersection exists = selectionKeys_.get(itemID);
    if (exists == null) {
      exists = intersect;  // FIX ME: does this do what I want?
    }
    if (exists.getSubID() != null) {
      Intersection fullInt = new Intersection(exists.getObjectID(), null, exists.getDistance(), true);
      selectionKeys_.put(itemID, fullInt);
      currSelZoomKey_ = null;
    }
    
    postSelect(rcx, sc, null, null, false, undom);
    return;
  }
  
  /***************************************************************************
  **
  ** unselect a full link
  */
  
  public void unselectFullLink(Intersection intersect, DataAccessContext rcx, UndoManager undom) {

    String itemID = intersect.getObjectID();
    Intersection exists = selectionKeys_.get(itemID);
    if (exists != null) {
      SelectionChange sc = preSelect();
      
      selectionKeys_.remove(itemID);
      currSelZoomKey_ = null;

      postSelect(rcx, sc, null, null, false, undom);
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Select the given nodes/genes
  */
  
  public void appendToSelectNodes(Set<String> nodeIDs, DataAccessContext rcx, UndoManager undom) {
    
    SelectionChange sc = preSelect();
    
    Iterator<String> nit = nodeIDs.iterator();    
    while (nit.hasNext()) {
      String nid = nit.next();
      if (selectionKeys_.get(nid) == null) {
        Intersection fullInt = new Intersection(nid, null, 0.0);
        selectionKeys_.put(nid, fullInt);
        currSelZoomKey_ = null;
      }
    }
    
    postSelect(rcx, sc, null, null, false, undom);
    return;
  }
  
  /***************************************************************************
  **
  ** Throw away all selections
  */
  
  public void selectNone(DataAccessContext rcx, UndoManager undom) {
    SelectionChange sc = preSelect();
    
    selectionKeys_.clear();
    currSelZoomKey_ = null;
    
    postSelect(rcx, sc, null, null, false, undom);
    return;
  }    
  
  /***************************************************************************
  **
  ** If a single node/gene is selected, return the ID.  Else return null.
  */
  
  public String getSingleNodeSelection(DataAccessContext rcx) {    
    if (selectionKeys_.size() != 1) {
      return (null);
    }    
    String key = selectionKeys_.keySet().iterator().next();
    Node node = rcx.getGenome().getNode(key);
    return ((node == null) ? null : key);
  }

  /***************************************************************************
  **
  ** Select everybody
  */
  
  public void selectAll(DataAccessContext rcx, UndoManager undom) {
    
    SelectionChange sc = preSelect(); 
  
    selectionKeys_.clear();
    currSelZoomKey_ = null;
    
    Iterator<Node> nit = rcx.getGenome().getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      Intersection fullInt = new Intersection(node.getID(), null, 0.0);
      selectionKeys_.put(fullInt.getObjectID(), fullInt); 
    }

    Iterator<Linkage> lit = rcx.getGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      Intersection fullInt = new Intersection(link.getID(), null, 0.0);
      selectionKeys_.put(fullInt.getObjectID(), fullInt); 
    }    

    //
    // No groups or notes at the moment
    //
    
    postSelect(rcx, sc, null, null, false, undom);
    return;
  }  

  /***************************************************************************
  **
  ** Select all nodes
  */
  
  public void selectAllNodes(DataAccessContext rcx, UndoManager undom) {
    
    SelectionChange sc = preSelect(); 
  
    selectionKeys_.clear();
    currSelZoomKey_ = null;
    
    Iterator<Node> nit = rcx.getGenome().getAllNodeIterator();    
    while (nit.hasNext()) {
      Node node = nit.next();
      Intersection fullInt = new Intersection(node.getID(), null, 0.0);
      selectionKeys_.put(fullInt.getObjectID(), fullInt);       
    }
    
    //
    // No groups or notes at the moment
    //
    
    postSelect(rcx, sc, null, null, false, undom);
    return;
  }   
  
  /***************************************************************************
  **
  ** Selection preprocessing
  */
  
  private SelectionChange preSelect() {
    SelectionChange sc = null;
    if (processSelections_) {
      sc = new SelectionChange();
      sc.oldMap = duplicateSelections();
    }
    return (sc);
  }


  /***************************************************************************
  **
  ** Selection postprocessing
  */
  
  private boolean postSelect(DataAccessContext rcxPS, SelectionChange sc, UndoSupport support, 
                             SelectionChangeCmd.Bundle bundle, boolean shortCircuit, UndoManager undom) {
    
    viewerRocCache_.clear();
    ghostedViewerRocCache_.clear();
    
    if (processSelections_) {
      sc.newMap = duplicateSelections();
      
      //
      // Don't want to submit undo if there is no change.  For now, just
      // handle most common no-change case (empty->empty):
      //
      if (shortCircuit && sc.oldMap.isEmpty() && sc.newMap.isEmpty()) {
        return (false);
      }
      GooseAppInterface goose = (isMain_) ? appState_.getGooseMgr().getGoose() : null;
      if ((goose != null) && goose.isActivated()) {
        gaggleSelectionSupport(goose, rcxPS);
      }
      selectionUndoSupport(sc, rcxPS.getGenomeID(), rcxPS.getLayoutID(), support, bundle, rcxPS, undom);
    }
    return (true);
  }  

  /***************************************************************************
  **
  ** Select items
  */
  
  public void selectItems(Rectangle rect, DataAccessContext rcxSI, boolean isShifted, UndoManager undom) {
    
    SelectionChange sc = preSelect();
    
    List<Intersection> newSelections = selectionGuts(rect, rcxSI, true);
    boolean isFirst = true;
    Iterator<Intersection> nsit = newSelections.iterator();    
    while (nsit.hasNext()) {
      Intersection newSelection = nsit.next();
      boolean doShifted = (isFirst) ? isShifted : true;
      isFirst = false;
      selectionDecision(newSelection, rcxSI, doShifted);
    }

    postSelect(rcxSI, sc, null, null, false, undom);
    return;
  } 

  /***************************************************************************
  **
  ** Select the given nodes/genes, with links thrown in
  */
  
  public void selectNodesAndLinks(Set<String> nodeIDs, DataAccessContext rcx, 
                                  List<Intersection> linkIntersections, boolean clearCurrent, UndoManager undom) {
    SelectionChange sc = preSelect();
    
    if (clearCurrent) { 
      selectionKeys_.clear();
    }
    currSelZoomKey_ = null;
    
    Iterator<String> nit = nodeIDs.iterator();    
    while (nit.hasNext()) {
      String nid = nit.next();
      Intersection fullInt = new Intersection(nid, null, 0.0);
      selectionKeys_.put(nid, fullInt);       
    }
    
    if (linkIntersections != null) {
      int numLI = linkIntersections.size();    
      for (int i = 0; i < numLI; i++) {
        Intersection li = linkIntersections.get(i);
        selectionKeys_.put(li.getObjectID(), li);
      }    
    }
    
    postSelect(rcx, sc, null, null, false, undom);
    return;
  }  

  /***************************************************************************
  **
  ** Select the given intersection list
  */
  
  public void selectIntersectionList(List<Intersection> intersections, DataAccessContext rcx, UndoManager undom) {
    SelectionChange sc = preSelect();
             
    selectionKeys_.clear();
    currSelZoomKey_ = null;
    
    Iterator<Intersection> iit = intersections.iterator();
    while (iit.hasNext()) {
      Intersection fullInt = iit.next();
      selectionKeys_.put(fullInt.getObjectID(), fullInt);       
    }

    //
    // No groups or notes at the moment
    //
    
    postSelect(rcx, sc, null, null, false, undom);
    return;
  }
  
  /***************************************************************************
  **
  ** Select an item
  */
  
  public Intersection selectItem(int x, int y, DataAccessContext rcxSI,
                                 boolean isShifted, boolean doOverlay, UndoManager undom) {
    
    SelectionChange sc = preSelect();                
    List<Intersection.AugmentedIntersection> augs = selectionGuts(x, y, rcxSI, true, true, doOverlay);
    Intersection.AugmentedIntersection aug = (new IntersectionChooser(false, rcxSI)).selectionRanker(augs);
    Intersection newSelection = (aug == null) ? null : aug.intersect;
    Intersection retval = selectionDecision(newSelection, rcxSI, isShifted);   
    postSelect(rcxSI, sc, null, null, true, undom);
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Handle selection semantics
  */
  
  private Intersection selectionDecision(Intersection newSelection, DataAccessContext rcxSD, boolean isShifted) {
   
    //
    // Clicking on a note short circuits the selection process:
    //
                                           
    if (newSelection != null) {
      String itemID = newSelection.getObjectID();
      if (rcxSD.getGenome().getNote(itemID) != null) {
        return (newSelection);
      }
    }
   
    //
    // Cases:
    // 
    // No.   Set          item      shifted      result
    //
    // 1     empty        null        y          nothing
    // 2     empty        matches     y          cannot occur
    // 3     empty        new         y          add item to set
    // 4    !empty        null        y          nothing
    // 5    !empty        matches     y          remove item
    // 6    !empty        new         y          add to set
    // 7     empty        null        n          nothing
    // 8     empty        matches     n          cannot occur
    // 9     empty        new         n          add to set
    // 10   !empty        null        n          clear set
    // 11   !empty        matches     n          nothing
    // 12   !empty        new         n          clear set, add to set

    boolean emptySet = selectionKeys_.isEmpty();
    boolean isNull = (newSelection == null);

    //
    // Figure out if selection is already in the set:
    //
    
    boolean matches = false;
    String newID = null;
    boolean sameParent = false;
    boolean intersects = false;
    Intersection exists = null;
    if (!isNull && !emptySet) { 
      newID = newSelection.getObjectID();
      exists = selectionKeys_.get(newID);
      if (exists != null) {  // Match at top level
        sameParent = true;
        //
        // Handle mergeable selections (linkages)
        //
        if (newSelection.canMerge() && exists.canMerge()) {
          Mergeable exSub = (Mergeable)exists.getSubID();
          Mergeable newSub = (Mergeable)newSelection.getSubID();
          // break full intersections into segments if needed
          if ((exSub == null) && (newSub == null)) {
            matches = true;
          } else if ((exSub == null) || (newSub == null)) {
            Linkage newLink = rcxSD.getGenome().getLinkage(newID);
            if (newLink == null) {
              throw new IllegalStateException();
            }
            if (exSub == null) {
              exSub = (Mergeable)Intersection.fullIntersection(newLink, rcxSD, false).getSubID();
              exists = new Intersection(exists, exSub);
            }
            if (newSub == null) {
              newSub = (Mergeable)Intersection.fullIntersection(newLink, rcxSD, false).getSubID();
              newSelection = new Intersection(newSelection, newSub);
            }
          }
          if (!matches) {
            if (exSub.equals(newSub)) {       
              matches = true;
            } else if (exSub.intersects(newSub)) {
              intersects = true;
            }
          }
        } else {  // non-mergeable subIDs: we don't care about them
          matches = true;
        }
      }
    }
    boolean isNew = (!isNull && !matches && !intersects);
    
    if (isShifted) {
      if (isNew) {
        addOrMerge(selectionKeys_, exists, newSelection, sameParent); // cases 3, 6
      } else if (matches) {
        selectionKeys_.remove(newID);  // case 5
      } else if (intersects) {
        intersect(selectionKeys_, exists, newSelection); 
      }
    } else {  // !isShifted
      if (emptySet) {
        if (isNew) {
          selectionKeys_.put(newSelection.getObjectID(), newSelection); // case 9
        }
      } else { // !empty
        if (isNull) {
          selectionKeys_.clear();  // case 10
        } else if (isNew) {
          selectionKeys_.clear();
          selectionKeys_.put(newSelection.getObjectID(), newSelection); // case 12
        }
      }
    }
    currSelZoomKey_ = null;
    return (newSelection); 
  }  

 /***************************************************************************
  **
  ** Set the floater
  */
  
  public void setFloater(Object floater) {
    setFloater(floater, null);
    return;
  }
  
  /***************************************************************************
  **
  ** Set the floater and floater color
  */
  
  public void setFloater(Object floater, Color floaterColor) {

    floaterLayout_ = new Layout(appState_, "floater", "floater");
    floaterSrc_ = null;
    if (floater == null) {
      floaterGenome_ = new DBGenome(appState_, "floater", "floater");
      floater_ = null;
      floatPoint_ = null;
      floaterColor_ = null;
      return;
    }
    if (floater instanceof Gene) {
      floaterGenome_ = new DBGenome(appState_, "floater", "floater");
      floaterGenome_.addGene((Gene)floater);
    } else if (floater instanceof Node) {
      floaterGenome_ = new DBGenome(appState_, "floater", "floater");
      floaterGenome_.addNode((Node)floater);
    } else if (floater instanceof Linkage) {
      floaterGenome_ = new DBGenome(appState_, "floater", "floater");
      floaterGenome_.addLinkage((Linkage)floater);
    } else if (floater instanceof Group) {
      floaterGenome_ = new GenomeInstance(appState_, "floater", "floater", null);
      DBGenome bogoGenome = new DBGenome(appState_, "floaterDB", "floaterDB");
      List<Genome> bogoList = new ArrayList<Genome>();
      bogoList.add(bogoGenome);
      bogoList.add(floaterGenome_);
      floaterSrc_ = new LocalGenomeSource(bogoGenome, bogoList);
      ((GenomeInstance)floaterGenome_).setGenomeSource(floaterSrc_);   
      ((GenomeInstance)floaterGenome_).addGroupWithExistingLabel((Group)floater);
    } else if (floater instanceof Note) {
      floaterGenome_ = new DBGenome(appState_, "floater", "floater");
      floaterGenome_.addNote((Note)floater);
    } else {
      // do nothing
    }
    floaterColor_ = floaterColor;
    floater_ = floater;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the targets
  */
  
  public void setTargets(Set<String> targetSet) {
    currentTargets_.clear();
    currentTargets_.addAll(targetSet);
    return;
  }  
  
  /***************************************************************************
  **
  ** Clear the targets
  */
  
  public void clearTargets() {
    currentTargets_.clear();
    return;
  }   
  
  /***************************************************************************
  **
  ** Set the root overlays
  */
  
  public void setRootOverlays(Set<String> overlaySet) {
    currentRootOverlays_.clear();
    currentRootOverlays_.addAll(overlaySet);
    return;
  }  
  
  /***************************************************************************
  **
  ** Clear the root overlays
  */
  
  public void clearRootOverlays() {
    currentRootOverlays_.clear();
    return;
  }  
  
  /***************************************************************************
  **
  ** Set the items not to be rendered
  */
  
  public void setNullRender(String nullID) {
    nullRender_ = nullID;
    return;
  }  

  /***************************************************************************
  **
  ** Set the current floater position
  */
  
  public void setFloaterPosition(Point2D position) {
    if (floater_ == null) {
      return;
    }
    forceToGrid(position);
    if (floater_ instanceof ArrayList) {
      floatPoint_ = position;
    } else if (floater_ instanceof Node) {
      floaterLayout_.getNodeProperties(((Node)floater_).getID()).setLocation(position);
    } else if (floater_ instanceof Linkage) {
      // do something with current position! (is this for link/target pad changes?)
    } else if (floater_ instanceof Group) {
      floaterLayout_.getGroupProperties(((Group)floater_).getID()).setLabelLocation(position);
    } else if (floater_ instanceof Note) {
      floaterLayout_.getNoteProperties(((Note)floater_).getID()).setLocation(position);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Answer if the floater is active
  */
  
  public boolean isFloaterActive() {
    return (floater_ != null);
  }

  /***************************************************************************
  **
  ** Get the set of selections
  */
  
  public Map<String, Intersection> getSelections() {
    return (selectionKeys_);
  }  
  
  /***************************************************************************
  **
  ** Convert selection to array as needed
  */  
  
  public Intersection[] selectionToArray(Intersection selection) {
    Intersection[] retval;
    if (selectionKeys_.get(selection.getObjectID()) != null) {
      retval = new Intersection[selectionKeys_.size()];
      selectionKeys_.values().toArray(retval);
    } else {
      retval = new Intersection[] {selection};
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Reduce selections
  */
  
  public Intersection[] selectionReduction(Intersection[] inters, DataAccessContext rcx) {
    
    //
    // Two different intersected linkages can resolve to one tree, and
    // we only want to deal with the tree once.  Handle this reduction.
    //
    
    Genome genome = rcx.getGenome();
    
    int size = inters.length;
    ArrayList<Intersection> reducedList = new ArrayList<Intersection>();
    HashSet<String> seenProps = new HashSet<String>();
    
    for (int i = 0; i < size; i++) {
      String interID = inters[i].getObjectID();
      Linkage link = genome.getLinkage(interID);
      if (link == null) {  // i.e. not a link
        reducedList.add(inters[i]);
        continue;
      }
      LinkProperties lp = rcx.getLayout().getLinkProperties(interID);
      if (lp == null) {
        continue;
      }
      String lpid = lp.getSourceTag();
      if (!seenProps.contains(lpid)) {
        reducedList.add(inters[i]);
        seenProps.add(lpid);
      }
    }
      
    return (reducedList.toArray(new Intersection[reducedList.size()]));
  }
  
  /***************************************************************************
  **
  ** Select a group.  Note how this version will intersect using the root parent,
  ** so group ID should be for the root!
  */
  
  public Intersection selectGroupFromParent(int x, int y, DataAccessContext rcx) {
    
    Genome genome = rcx.getGenome();
    if (!(genome instanceof GenomeInstance)) {
      throw new IllegalArgumentException();
    }
    GenomeInstance gi = (GenomeInstance)genome;
    GenomeInstance parent = gi.getVfgParent();
    if (parent == null) {
      throw new IllegalArgumentException();
    }
    
    Point2D pt = new Point2D.Float(x, y);
 
    GenomeInstance rootvfg = gi.getVfgParentRoot();
    DataAccessContext rcxV = new DataAccessContext(rcx, rootvfg);
    Intersection groupCheck = intersectGroups(rcxV, null, rootvfg, pt, false);
    return (groupCheck);
  }
  
  /***************************************************************************
  **
  ** Select a group
  */
  
  public Intersection selectGroupForRootInstance(int x, int y, DataAccessContext rcxT) {
    
    Genome genome = rcxT.getGenome();
    if (!(genome instanceof GenomeInstance)) {
      throw new IllegalArgumentException();
    }
    GenomeInstance gi = (GenomeInstance)genome;
    GenomeInstance parent = gi.getVfgParent();
    if (parent != null) {
      throw new IllegalArgumentException();
    }
    
    Point2D pt = new Point2D.Float(x, y);
    Intersection groupCheck = intersectGroups(rcxT, null, gi, pt, false);
    return (groupCheck);
  }  
  
  /***************************************************************************
  **
  ** Select a group.  Note how this returns group ID that reflect level in the
  ** model.
  */
  
  public Intersection selectGroup(int x, int y, DataAccessContext rcxT) {
 
    if (!(rcxT.getGenome() instanceof GenomeInstance)) {
      throw new IllegalArgumentException();
    }
    GenomeInstance gi = rcxT.getGenomeAsInstance();
    GenomeInstance rootvfg = gi.getVfgParentRoot(); // may be null...
    Point2D pt = new Point2D.Float(x, y);
    Intersection groupCheck = intersectGroups(rcxT, rootvfg, gi, pt, false);
    return (groupCheck);
  }   
  
  /***************************************************************************
  **
  ** Select from a limited set of linkages present in the root parent
  */
  
  public List<Intersection> selectLinkFromRootParent(int x, int y, DataAccessContext rcxP, Set<String> okLinks) {

    //
    // Crank through the items and ask the renderers for an intersection
    //
    
    if (!(rcxP.getGenome() instanceof GenomeInstance)) {
      throw new IllegalArgumentException();
    }
    GenomeInstance gi = rcxP.getGenomeAsInstance();
    GenomeInstance rootvfg = gi.getVfgParentRoot();
    if (rootvfg == null) {
      throw new IllegalArgumentException();
    }
    
    Point2D pt = new Point2D.Float(x, y);
 
    //
    // First intersect collapsed groups, which mask their contents.
    //
    
    DataAccessContext rcxRo = new DataAccessContext(rcxP, rootvfg);
    
    Intersection groupCheck = intersectGroups(rcxRo, rootvfg, gi, pt, true);
    if (groupCheck != null) {
      return (null);
    }
    
    ArrayList<Intersection> retval = new ArrayList<Intersection>();
     
    Iterator<String> lit = okLinks.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage link = rootvfg.getLinkage(linkID);
      LinkageFree render = (LinkageFree)rcxRo.getLayout().getLinkProperties(linkID).getRenderer();      
      Intersection inter = render.intersects(link, pt, rcxRo, null);
      if (inter != null) {
        retval.add(inter);
      }
    }
    return ((retval.isEmpty()) ? null : retval);
  }  

  /***************************************************************************
  **
  ** Select an item
  */
  
  public List<Intersection> selectFromParent(int x, int y, DataAccessContext rcxO) {
       
    //
    // Crank through the items and ask the renderers for an intersection
    //
    
    if (!(rcxO.getGenome() instanceof GenomeInstance)) {
      throw new IllegalArgumentException();
    }
    GenomeInstance gi = rcxO.getGenomeAsInstance();
    GenomeInstance parent = gi.getVfgParent();
    if (parent == null) {
      throw new IllegalArgumentException();
    }
    
    Point2D pt = new Point2D.Float(x, y);
 
    //
    // First intersect collapsed groups, which mask their contents.
    //
    
   
    GenomeInstance rootvfg = gi.getVfgParentRoot();
    DataAccessContext rcxR = new DataAccessContext(rcxO, rootvfg);
    
    Intersection groupCheck = intersectGroups(rcxR, rootvfg, gi, pt, true);
    if (groupCheck != null) {
      return (null);
    }
    
    DataAccessContext rcxP = new DataAccessContext(rcxO, parent);
    
    ArrayList<Intersection> retval = new ArrayList<Intersection>();
    
    Iterator<Linkage> lit = parent.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      LinkageFree render = (LinkageFree)rcxP.getLayout().getLinkProperties(link.getID()).getRenderer();      
      Intersection inter = render.intersects(link, pt, rcxP, null);
      if (inter != null) {
        retval.add(inter);
      }
    }
    
    Iterator<Gene> git = parent.getGeneIterator();    
    while (git.hasNext()) {
      Gene gene = git.next();
      IRenderer render = rcxP.getLayout().getNodeProperties(gene.getID()).getRenderer();
      Intersection inter = render.intersects(gene, pt, rcxP, null);      
      if (inter != null) {
        retval.add(inter);
      }
    }
    
    Iterator<Node> nit = parent.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      IRenderer render = rcxP.getLayout().getNodeProperties(node.getID()).getRenderer();      
      Intersection inter = render.intersects(node, pt, rcxP, null);
      if (inter != null) {
        retval.add(inter);
      }      
    } 
    return ((retval.isEmpty()) ? null : retval);
  }
  
  /***************************************************************************
  **
  ** Forces the coordinates to grid points
  */  
  
  void forceToGrid(Point2D pt) {
    pt.setLocation((Math.round(pt.getX() / 10.0)) * 10.0, (Math.round(pt.getY() / 10.0)) * 10.0);    
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(SelectionChange undo) {
    if (undo.oldMap != null) {
      selectionKeys_ = duplicateSelections(undo.oldMap);
    }
    GooseAppInterface goose = (isMain_) ? appState_.getGooseMgr().getGoose() : null;
    if ((goose != null) && goose.isActivated()) {
      DataAccessContext rcx = new DataAccessContext(appState_);
      rcx.setGenome(rcx.getGenomeSource().getGenome(undo.genomeKey));
      rcx.setLayout(rcx.lSrc.getLayout(undo.layoutKey));
      gaggleSelectionSupport(goose, rcx);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(SelectionChange undo) {
    if (undo.newMap != null) {
      selectionKeys_ = duplicateSelections(undo.newMap);
    }
    GooseAppInterface goose = (isMain_) ? appState_.getGooseMgr().getGoose() : null;
    if ((goose != null) && goose.isActivated()) {
      DataAccessContext rcx = new DataAccessContext(appState_);
      rcx.setGenome(rcx.getGenomeSource().getGenome(undo.genomeKey));
      rcx.setLayout(rcx.lSrc.getLayout(undo.layoutKey));
      gaggleSelectionSupport(goose, rcx);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** When placing things like nodes, they will "evaporate" when the user
  ** clicks over a non-revealed region with an opaque overlay.  Send back
  ** the set of non-black holes (module IDs).  A null return means there are 
  ** no restrictions.
  */
  
  public Set<String> nonBlackHoles(DataAccessContext rcxB) {
    
    String currentOverlay = rcxB.oso.getCurrentOverlay();
    TaggedSet modKeys = rcxB.oso.getCurrentNetModules();
    int intersectMask = rcxB.oso.getCurrentOverlaySettings().intersectionMask;
    Set<String> currentNetModSet = (modKeys == null) ? null : modKeys.set;
    TaggedSet revKeys = rcxB.oso.getRevealedModules();
    Set<String> currentRevealed = (revKeys == null) ? null : revKeys.set;         

    //
    // Quick kills:
    //
     
    if (currentOverlay == null) {
      return (null);
    }  
    if (intersectMask == NetModuleFree.CurrentSettings.NOTHING_MASKED) {
      return (null);
    } 
    NetOverlayProperties nop = rcxB.getLayout().getNetOverlayProperties(currentOverlay);
    if (nop.getType() != NetOverlayProperties.OvrType.OPAQUE) {
      return (null);
    }   
    
    Set<String> useMods = currentNetModSet;
    if (intersectMask == NetModuleFree.CurrentSettings.ALL_MASKED) {
      HashSet<String> normalized = new HashSet<String>(currentRevealed); 
      normalized.retainAll(currentNetModSet);
      if (normalized.isEmpty()) {
        return (new HashSet<String>());
      }
      useMods = normalized;
    } else if (intersectMask != NetModuleFree.CurrentSettings.NON_MEMBERS_MASKED) {
      throw new IllegalArgumentException();
    }   
  
    return (new HashSet<String>(useMods));
  }  
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Hold info on current state of overlay presentation 
  */
  
  public static class OpaqueOverlayInfo {

    public Rectangle viewRect;
    public BufferedImage bim;
    public Graphics2D overlayLayerG2;
    public AffineTransform drawImageTrans;
 
    public OpaqueOverlayInfo(Rectangle viewRect, BufferedImage bim, 
                             Graphics2D overlayLayerG2, AffineTransform drawImageTrans) {
      this.viewRect = viewRect;
      this.bim = bim;
      this.overlayLayerG2 = overlayLayerG2;
      this.drawImageTrans = drawImageTrans;
    }
    
    public void renderImage(Graphics2D g2) {
      AffineTransform saveTransform = g2.getTransform();
      g2.setTransform(drawImageTrans);
      g2.drawImage(bim, viewRect.x, viewRect.y, viewRect.width, viewRect.height, null);
      g2.setTransform(saveTransform);
      return;
    }  
  }   
 
  /***************************************************************************
  **
  ** Intersection masking
  */
  
  
  private static class MaskData {      
    // Modules not in this list have an opaque interior.
    Set<String> nonBlackHoles;
    // per module, can we see the label?
    HashMap<String, Boolean> labelViz;
    // what candidates can we intersect?
    HashSet<String> candidates;
   
    MaskData() {
      labelViz = new HashMap<String, Boolean>();
    }    
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Support selection undo operations
  */
  
  private void selectionUndoSupport(SelectionChange sc, String genomeKey, 
                                    String layoutKey, UndoSupport support, 
                                    SelectionChangeCmd.Bundle bundle, DataAccessContext dacx, UndoManager undom) {
    sc.gp = this;
    sc.genomeKey = genomeKey;
    sc.layoutKey = layoutKey;

    boolean localUndo = false;
    if ((support == null) && (bundle == null)) {
      if (undom != null) {
        localUndo = true;
        support = new UndoSupport(appState_, "undo.selection");
      }
    }    

    if (support != null) {
      support.addEdit(new SelectionChangeCmd(appState_, dacx, sc));
      support.addEvent(new SelectionChangeEvent(genomeKey, layoutKey, SelectionChangeEvent.SELECTED_ELEMENT));
    } else {
      bundle.cmd = new SelectionChangeCmd(appState_, dacx, sc);
      bundle.event = new SelectionChangeEvent(genomeKey, layoutKey, SelectionChangeEvent.SELECTED_ELEMENT);
    }
    if (localUndo) {support.finish();}
    return;
  } 
  
 
  
  /***************************************************************************
  **
  ** Figure out if the item is selected
  */
  
  private Intersection isSelected(HashMap<String, Intersection> keys, GenomeItem item) {
    String id = item.getID();
    return (keys.get(id));
  }
  
 /***************************************************************************
  **
  ** BOGUS.  The linkID inside an intersection can be for any link passing
  ** through the tree, not just for the key in the selection table
  */
  
  private Intersection linkIsSelected(Set<String> justKeys, HashMap<String, Intersection> keys, Layout layout, GenomeItem link) {
    if (keys.isEmpty()) {
      return (null);
    }
    String id = link.getID();
    Set<String> shared = layout.getSharedItems(id);  // All the links in the tree for the link arg
    HashSet<String> intersection = new HashSet<String>(justKeys);  // All selected keys
    intersection.retainAll(shared);  // the one link key that is used for this link arg (or none)
    if (intersection.isEmpty()) {  // not selected
      return (null);
    }
    return (keys.get(intersection.iterator().next()));  // the one left...
  }  

 
  
  /***************************************************************************
  **
  ** Guts of presentation
  */
  
	private void presentationGuts(ModelObjectCache moc, ModelObjectCache linkMoc,
		                           	boolean doReset, boolean hideLinks, DataAccessContext rcx) {

		Object misc = null;

		if (doReset) {
			// g2.drawImage(grid_, 0, 0, 2000, 1600, null);
		}
    Genome genome = rcx.getGenome();
    Layout layout = rcx.getLayout();
		
		//
		// Crank through the genes and nodes and draw them up
		//
		Iterator<Node> nit = genome.getAllNodeIterator();
		while (nit.hasNext()) {
			Node node = nit.next();
			if (node.getID().equals(nullRender_)) {
				continue;
			}
			Intersection selected = isSelected(selectionKeys_, node);
			IRenderer render = layout.getNodeProperties(node.getID()).getRenderer();
			boolean localGhost = rcx.isGhosted();
			if (!rcx.isGhosted() && !currentTargets_.isEmpty()) {
				localGhost = !currentTargets_.contains(node.getID());
			}
			rcx.pushGhosted(localGhost);
			render.render(moc, node, selected, rcx, misc);
			rcx.popGhosted();
		}

		//
		// Crank through the links and draw them up (unless the overlay says no!)
		//

		DisplayOptions ddopt = dopmgr_.getDisplayOptions();
		LinkageFree.AugmentedDisplayOptions adopt = new LinkageFree.AugmentedDisplayOptions(ddopt, null);
		HashSet<String> skipDrops = new HashSet<String>();
		LinkageFree.AugmentedDisplayOptions adoptf = new LinkageFree.AugmentedDisplayOptions(ddopt, skipDrops);

		Genome rootParent = null;
		if (genome instanceof GenomeInstance) {
			//
			// 12/05/12: Trying to draw links in a second-level VfN causes crashes if
			// the links
			// are not in the parent VfN. Need to go all the way to the rootInstance!
			//
			// rootParent = ((GenomeInstance)genome).getVfgParent();
			rootParent = ((GenomeInstance) genome).getVfgParentRoot();
		}

		HashSet<LinkProperties> renderedBuses = new HashSet<LinkProperties>();
		Iterator<Linkage> lit = genome.getLinkageIterator();
		//
		// For now, we just use the object cache for the viewer, since the view
		// never
		// changes:
		//
		HashMap<String, RenderObjectCache> cache = (rcx.isGhosted()) ? ghostedViewerRocCache_ : viewerRocCache_;
		HashMap<String, ModelObjectCache> mcache = (rcx.isGhosted()) ? ghostedViewerMocCache_ : viewerMocCache_;
		
		// RenderObjectCache roc = cache.get(genome.getID());
		ModelObjectCache linkagemoc = mcache.get(genome.getID());
		boolean viewerOnly = !appState_.getIsEditor();
		
		// TODO
		// In the viewer, cache the rendered linkage and do not render it again.
		if (!viewerOnly || true) {
			/*
			 * TODO
			 * Use a cached ModelObjectCache in viewer to avoid redrawing the linkage.
			 *  
			 * roc = new RenderObjectCache();
			 * if (viewerOnly) {
			 * 	cache.put(genome.getID(), roc);
			 * }
		 	*/
			if (!hideLinks) {
				renderLinkage(moc, rcx, adopt, skipDrops, adoptf, rootParent, renderedBuses, lit);

				/*
				 * if (linkMoc != null) { renderLinkage(linkMoc, genome, layout,
				 * isGhosted, showBubbles, clipRect, pixDiam, adopt, skipDrops, adoptf,
				 * rootParent, renderedBuses, lit); }
				 */
			}
		}

		//
		// Crank through notes and draw them up
		//

		if (!rcx.isGhosted()) {
			Iterator<Note> noit = genome.getNoteIterator();
			while (noit.hasNext()) {
				Note note = noit.next();
				IRenderer render = layout.getNoteProperties(note.getID()).getRenderer();
				Intersection selected = isSelected(selectionKeys_, note);
				render.render(moc, note, selected, rcx, misc);
			}
		}

		// return (roc);
	}

	private void renderLinkage(ModelObjectCache moc, DataAccessContext rcx, 
			LinkageFree.AugmentedDisplayOptions adopt, HashSet<String> skipDrops,
			LinkageFree.AugmentedDisplayOptions adoptf, Genome rootParent,
			HashSet<LinkProperties> renderedBuses, Iterator<Linkage> lit) {
	  
		Set<String> justKeys = selectionKeys_.keySet();
		while (lit.hasNext()) {
		  Linkage link = lit.next();
		  if (link.getID().equals(nullRender_)) {
		    continue;
		  }      
		  LinkProperties lp = rcx.getLayout().getLinkProperties(link.getID());
		  if (lp == null) {
		    System.err.println("No linkProperties for " + link.getID());
		    throw new IllegalStateException();
		  }
		  if (renderedBuses.contains(lp)) {
		    continue;
		  } else {
		    renderedBuses.add(lp);
		  }
		  Intersection selected = linkIsSelected(justKeys, selectionKeys_, rcx.getLayout(), link);
		  LinkageFree render = (LinkageFree)lp.getRenderer();
		  //
		  // If we are drawing into an instance, sometimes we ghost everything but
		  // allowed link sources and targets.  Note this method is crude, and really only
		  // works to blank out all link rendering or no link rendering.  In the latter case,
		  // all links ID in the tree have to be targeted, since we are working with a random
		  // ID from the set:
		  //
		  boolean isGhosted = rcx.isGhosted();
		  boolean localGhost = isGhosted;
		  if (!isGhosted && !currentTargets_.isEmpty()) {
		    localGhost = !currentTargets_.contains(link.getID());
		  }
		  rcx.pushGhosted(localGhost);

		  //
		  // If we are drawing into a subset instance, sometimes we want to draw the full
		  // link tree from the root to give the user the full range of existing linkages to
		  // start drawing.  Replace anybody who would get drawn in child context if parent
		  // context is needed:
		  //        

		  if (!currentRootOverlays_.isEmpty() && 
		      (rootParent != null) &&
		      currentRootOverlays_.contains(link.getID())) {
		    skipDrops.clear();
		    skipDrops.addAll(currentRootOverlays_);
		    Set<String> actuallyPresent = rcx.getGenome().getOutboundLinks(link.getSource());
		    skipDrops.removeAll(actuallyPresent);
		    link = rootParent.getLinkage(link.getID());
		    DataAccessContext rcxP = new DataAccessContext(rcx);
		    rcxP.setGenome(rootParent);
		    render.render(moc, link, selected, rcxP, adoptf);
		  } else {
		    render.render(moc, link, selected, rcx, adopt);
		  }
		  rcx.popGhosted();
		}
		//
		// The above handling of root overlays replaces child-based rendering with root-based
		// rendering if there is a child link to be rendered.  But we also need to render root-based
		// links even if the child has no representatives:

		if (!currentRootOverlays_.isEmpty() && (rootParent != null)) {
		  Iterator<String> croit = currentRootOverlays_.iterator();
		  skipDrops.clear();
		  skipDrops.addAll(currentRootOverlays_);
		  while (croit.hasNext()) {
		    String linkID = croit.next();
		    LinkProperties lp = rcx.getLayout().getLinkProperties(linkID);
		    if (renderedBuses.contains(lp)) {
		      continue;
		    } else {
		      renderedBuses.add(lp);
		    }
		    LinkageFree render = (LinkageFree)lp.getRenderer();
		    Linkage link = rootParent.getLinkage(linkID);
		    DataAccessContext rcxP = new DataAccessContext(rcx);
        rcxP.setGenome(rootParent);
		    rcxP.pushGhosted(false);
        render.render(moc, link, null, rcxP, adoptf);
        rcx.popGhosted();
		  }
		}
	}
	
  /***************************************************************************
  **
  ** Render the model data
  */
  
	private void renderModelData(ModelObjectCache moc, DataAccessContext rcx) {
	  	ModalTextShapeFactory textFactory = null;
	  	
	  	if (rcx.forWeb) {
	  		textFactory = new ModalTextShapeFactoryForWeb(rcx.getFrc());
	  	}
	  	else {
	  		textFactory = new ModalTextShapeFactoryForDesktop();
	  	}
	  	
		// TODO replace by constants
		Integer majorLayer = 0;
		Integer minorLayer = 0;
		CommonCacheGroup group = new CommonCacheGroup("_modeldata", "modeldata", "modeldata");

		ModelData md = appState_.getDB().getModelData();
		String longName = rcx.getGenome().getLongName();
		if (((md != null) || (longName != null)) && !rcx.isGhosted()) {
			Font mFont = appState_.getFontMgr().getFont(FontManager.DATE);
			Font bFont = appState_.getFontMgr().getFont(FontManager.TITLE);

			String date = (md == null) ? null : md.getDate();
			if ((date != null) && (!date.trim().equals(""))) {
				Point2D loc = rcx.getLayout().getDataLocation(Layout.DATE);
				if (loc != null) { // Should not ever be null, but...
					Rectangle2D bounds = mFont.getStringBounds(date, rcx.getFrc());
					double width = bounds.getWidth();
					double height = bounds.getHeight();

          // Flag the AnnotatedFont as an override so that the font will be
          // exported as part of the TextShape in the web app.

					ModalShape ts = textFactory.buildTextShape(date,
							new AnnotatedFont(mFont, true), Color.black, (float) loc.getX() - ((float) width / 2.0F),
							(float) loc.getY() + ((float) height / 2.0F),
							new AffineTransform());
					group.addShape(ts, majorLayer, minorLayer);
				}
			}
			String attrib = (md == null) ? null : md.getAttribution();
			if ((attrib != null) && (!attrib.trim().equals(""))) {
				Point2D loc = rcx.getLayout().getDataLocation(Layout.ATTRIB);
				if (loc != null) { // Should not ever be null, but...
					Rectangle2D bounds = mFont.getStringBounds(attrib, rcx.getFrc());
					double width = bounds.getWidth();
					double height = bounds.getHeight();

          // Flag the AnnotatedFont as an override so that the font will be
          // exported as part of the TextShape in the web app.
					ModalShape ts = textFactory.buildTextShape(
							attrib, new AnnotatedFont(mFont, true), Color.black, (float) loc.getX()
									- ((float) width / 2.0F), (float) loc.getY()
									+ ((float) height / 2.0F), new AffineTransform());
					group.addShape(ts, majorLayer, minorLayer);
				}
			}

			int keySize = (md == null) ? 0 : md.getKeySize();
			if (keySize > 0) {
				DataLocator dl = new DataLocator(appState_, rcx);
				Point2D loc = rcx.getLayout().getDataLocation(Layout.KEY);
				if (loc != null) { // Should not ever be null, but...

					ArrayList<String> keyList = new ArrayList<String>();
					for (int i = 0; i < keySize; i++) {
						keyList.add(md.getKey(i));
					}
					Dimension dim = dl.calcKeyBounds(keyList, rcx.getLayout(), mFont, rcx.getFrc());
					double width = dim.getWidth();
					double height = dim.getHeight();
					double keyX = loc.getX() - (width / 2.0);
					double currY = loc.getY() - (height / 2.0);
					for (int i = 0; i < keySize; i++) {
						String entry = md.getKey(i);
						LineMetrics lm = mFont.getLineMetrics(entry, rcx.getFrc());
						currY += lm.getHeight();

            // Flag the AnnotatedFont as an override so that the font will be
            // exported as part of the TextShape in the web app.
						ModalShape ts = textFactory.buildTextShape(
								entry, new AnnotatedFont(mFont, true), Color.black, (float) keyX, (float) currY,
								new AffineTransform());
						group.addShape(ts, majorLayer, minorLayer);
					}
				}
			}

			if ((longName != null) && (!longName.trim().equals(""))) {
				Point2D loc = rcx.getLayout().getDataLocation(Layout.TITLE);
				if (loc != null) { // Should not ever be null, but...
					Rectangle2D bounds = mFont.getStringBounds(longName, rcx.getFrc());
					double width = bounds.getWidth();
					double height = bounds.getHeight();

          // Flag the AnnotatedFont as an override so that the font will be
          // exported as part of the TextShape in the web app.
					ModalShape ts = textFactory.buildTextShape(
							longName, new AnnotatedFont(bFont, true), Color.black, (float) loc.getX()
									- ((float) width / 2.0F), (float) loc.getY()
									+ ((float) height / 2.0F), new AffineTransform());
					group.addShape(ts, majorLayer, minorLayer);
				}
			}
		}

		moc.addGroup(group);

		return;
	}

 /***************************************************************************
  **
  ** Intersect linkage labels
  */
  
  public boolean intersectLinkageLabels(DataAccessContext rcx2, Point2D pt) {
    String currentOverlay = rcx2.oso.getCurrentOverlay();
    TaggedSet modKeys = rcx2.oso.getCurrentNetModules();
    int intersectMask = rcx2.oso.getCurrentOverlaySettings().intersectionMask;
    Set<String> currentNetModSet = (modKeys == null) ? null : modKeys.set;
    TaggedSet revKeys = rcx2.oso.getRevealedModules();
    Set<String> revMods = (revKeys == null) ? null : revKeys.set;     
    // Intersect linkage labels:
    HashSet<String> seenSrcs = new HashSet<String>();
    
    Iterator<Linkage> lit = rcx2.getGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String lSrc = link.getSource();
      if (seenSrcs.contains(lSrc)) {
        continue;
      }
      seenSrcs.add(lSrc);
      LinkProperties lp = rcx2.getLayout().getLinkProperties(link.getID());
      LinkageFree render = (LinkageFree)lp.getRenderer();    
      if (render.intersectsLabel(link, lp, rcx2, pt)) {
        Intersection dummy = new Intersection(null, null, 0.0);
        dummy = filterForModuleMask(rcx2, pt, dummy, currentOverlay, currentNetModSet, revMods, intersectMask);
        if (dummy != null) {
          return (true);
        }
      }
    }
    return (false);
  }

  /***************************************************************************
  **
  ** Filter intersection for module mask
  */
  
  public Intersection filterForModuleMask(DataAccessContext rcx, Point2D pt, Intersection inter,
                                          String currentOverlay, Set<String> currentNetModSet, Set<String> currentRevealed, 
                                          int intersectMask) {
    if (inter != null) {
      int x = (int)pt.getX();
      int y = (int)pt.getY();
      int maskNeed = needMaskingReduction(rcx.getLayout(), currentOverlay, intersectMask);
      switch (maskNeed) {
        case NetModuleFree.CurrentSettings.NOTHING_MASKED:
          return (inter);
        case NetModuleFree.CurrentSettings.ALL_MASKED:
          HashSet<String> normalized = new HashSet<String>(currentRevealed); 
          normalized.retainAll(currentNetModSet);
          if (normalized.isEmpty()) {
            return (null);
          }
          List<String> nmintAM = intersectNetModules(x, y, rcx, currentOverlay, normalized);
          if (nmintAM.size() > 0) {
            return (inter);
          }
          break;
        case NetModuleFree.CurrentSettings.NON_MEMBERS_MASKED: 
          List<String> nmint = intersectNetModules(x, y, rcx, currentOverlay, currentNetModSet);
          if (nmint.size() > 0) {
            return (inter);
          }
          break;
        default:
          throw new IllegalStateException();
      }
    }
    return (null);
  } 

  /***************************************************************************
  **
  ** Filter rectangle-derived link intersections for module mask
  */  
  
  private List<Intersection> filterLinksForModuleMask(DataAccessContext rcx, List<Intersection> rawMatches,
                                                      String currentOverlay, Set<String> currentNetModSet, Set<String> currentRevealed, int intersectMask) {  

    Set<String> useForIntersect = currentNetModSet;
    int maskNeed = needMaskingReduction(rcx.getLayout(), currentOverlay, intersectMask);
    switch (maskNeed) {
      case NetModuleFree.CurrentSettings.NOTHING_MASKED:
        return (rawMatches);
      case NetModuleFree.CurrentSettings.ALL_MASKED:
        HashSet<String> normalized = new HashSet<String>(currentRevealed); 
        normalized.retainAll(currentNetModSet);
        if (normalized.isEmpty()) {
          return (new ArrayList<Intersection>());
        }
        useForIntersect = normalized;
        // Yes, we are falling thru!
      case NetModuleFree.CurrentSettings.NON_MEMBERS_MASKED:
        Map<String, Rectangle2D> outerRectMap = new HashMap<String, Rectangle2D>(); 
        Map<String, List<Shape>> shapeMap = new HashMap<String, List<Shape>>();
        
        buildShapeMapForNetModules(rcx, currentOverlay, useForIntersect, outerRectMap, shapeMap);
        return (intersectCandidatesWithShapes(rcx, rawMatches, outerRectMap, shapeMap));
      default:
        throw new IllegalStateException();
    }
  } 
 
  /***************************************************************************
  **
  ** Build up shape maps
  */
  
  public void buildShapeMapForNetModules(DataAccessContext rcx,
                                          String currentOverlay, 
                                          Set<String> useModsForIntersect,
                                          Map<String, Rectangle2D> outerRectMap, 
                                          Map<String, List<Shape>> shapeMap) {

    Genome inputGenome = rcx.getGenome();
    NetOverlayOwner owner = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(inputGenome.getID());
    Set<String> useGroups = owner.getGroupsForOverlayRendering();
    NetworkOverlay novr = owner.getNetworkOverlay(currentOverlay);
    NetOverlayProperties nop = rcx.getLayout().getNetOverlayProperties(currentOverlay);
    //
    // We use the rootInstance to drive rendering, so that non-included nodes are not
    // shown as part of the module (important for dynamic instances!)
    //
    
    Genome useGenome = inputGenome;
    if (inputGenome instanceof GenomeInstance) {
      GenomeInstance gi = (GenomeInstance)inputGenome;
      GenomeInstance rootGI = gi.getVfgParentRoot();
      useGenome = (rootGI == null) ? gi : rootGI;
    }
    DataAccessContext rcxU = new DataAccessContext(rcx);
    rcxU.setGenome(useGenome);

    Iterator<NetModule> mit = novr.getModuleIterator();
    while (mit.hasNext()) {
      NetModule mod = mit.next();
      String id = mod.getID();
      if (useModsForIntersect.contains(id)) {
        Rectangle2D outerRect = new Rectangle2D.Double();
        NetModuleProperties nmp = nop.getNetModuleProperties(id);
        List<Shape> modShapes = nmp.getRenderer().getModuleShapes(inputGenome, useGroups, currentOverlay, id, outerRect, rcxU); 
        shapeMap.put(id, modShapes);
        outerRectMap.put(id, outerRect);
      }
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Reduce the intersection list by eliminating segments not in the unmasked modules
  */
  
  public List<Intersection> intersectCandidatesWithShapes(DataAccessContext rcx,
                                                          List<Intersection> rawMatches,
                                                          Map<String, Rectangle2D> outerRectMap, 
                                                          Map<String, List<Shape>> shapeMap) { 
    
    ArrayList<Intersection> retval = new ArrayList<Intersection>();
    
    int numRaw = rawMatches.size();
    for (int i = 0; i < numRaw; i++) {
      Intersection inter = rawMatches.get(i);
      String linkID = inter.getObjectID();
      BusProperties bp = rcx.getLayout().getLinkProperties(linkID);
      LinkSegmentID[] lsids = inter.segmentIDsFromIntersect();
      HashSet<LinkSegmentID> rawLSIDs = new HashSet<LinkSegmentID>(Arrays.asList(lsids));
      Iterator<String> skit = shapeMap.keySet().iterator();
      while (skit.hasNext()) {
        String modKey = skit.next();
        // First pass is cheap (outer bounding rectangle):
        Rectangle2D outerRect = outerRectMap.get(modKey);
        List<LinkSegmentID> firstCut = bp.intersectBusSegmentsWithRect(rcx, null, outerRect, true);
        // If the intersection is empty, no point in continuing:
        HashSet<LinkSegmentID> intersect = new HashSet<LinkSegmentID>(firstCut);
        intersect.retainAll(rawLSIDs);
        if (!intersect.isEmpty()) {
          List<Shape> modShapes = shapeMap.get(modKey);
          int numShape = modShapes.size();
          HashSet<LinkSegmentID> allSegIDs = new HashSet<LinkSegmentID>();
          for (int j = 0; j < numShape; j++) {
            Shape nextShape = modShapes.get(j);
            List<LinkSegmentID> perShape = bp.intersectBusSegmentsWithShape(rcx, null, nextShape, true);
            allSegIDs.addAll(perShape);
          }
          HashSet<LinkSegmentID> intersectFinal = new HashSet<LinkSegmentID>(allSegIDs);
          intersectFinal.retainAll(rawLSIDs);
          if (!intersectFinal.isEmpty()) {
            Intersection maskedInter = Intersection.intersectionForSegmentIDs(linkID, intersectFinal);
            retval.add(maskedInter);
          }
        }
      }
    }
           
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Intersect notes
  */
  
  public List<Intersection> intersectNotes(Point2D pt, DataAccessContext rcx, boolean interactiveOnly) {

    String currentOverlay = rcx.oso.getCurrentOverlay();
    TaggedSet modKeys = rcx.oso.getCurrentNetModules();
    int intersectMask = rcx.oso.getCurrentOverlaySettings().intersectionMask;
    Set<String> currentNetModSet = (modKeys == null) ? null : modKeys.set;
    TaggedSet revKeys = rcx.oso.getRevealedModules();
    Set<String> revMods = (revKeys == null) ? null : revKeys.set;    
    ArrayList<Intersection> retval = new ArrayList<Intersection>();
   
    Iterator<Note> ntit = rcx.getGenome().getNoteIterator();
    while (ntit.hasNext()) {
      Note note = ntit.next();
      if (!interactiveOnly || note.isInteractive()) {
        IRenderer render = rcx.getLayout().getNoteProperties(note.getID()).getRenderer();      
        Intersection inter = render.intersects(note, pt, rcx, null);
        inter = filterForModuleMask(rcx, pt, inter, currentOverlay, currentNetModSet, revMods, intersectMask);
        if (inter != null) {
          retval.add(inter);
        }
      }
    }      
    return ((retval.isEmpty()) ? null : retval);
  }   
  
  /***************************************************************************
  **
  ** Intersect the model data
  */
  
  public String intersectModelData(DataAccessContext rcxD,
                                   Point2D pt,                                     
                                   String currentOverlay, 
                                   Set<String> currentNetModSet, int intersectMask) { 

    String[] keys = new String[] {Layout.DATE, Layout.ATTRIB, Layout.TITLE, Layout.KEY};
    int[] fonts = new int[] {FontManager.DATE, FontManager.DATE, FontManager.TITLE, FontManager.DATE};
    boolean[] isSingle = new boolean[] {true, true, true, false};    
    ModelData md = appState_.getDB().getModelData();
    String longName = rcxD.getGenome().getLongName();
    if ((md == null) && ((longName == null) || longName.trim().equals(""))) {
      return (null);
    }
    Object[] data = new Object[4];
    data[0] = (md == null) ? null : md.getDate();
    data[1] = (md == null) ? null : md.getAttribution();
    data[2] = longName;
    if (md != null) {
      List<String> keyList = new ArrayList<String>();
      int numKey = md.getKeySize();
      for (int i = 0; i < numKey; i++) {
        keyList.add(md.getKey(i));
      }      
      data[3] = keyList;
    } else {
      data[3] = null;
    }

    DataLocator dl = new DataLocator(appState_, rcxD);
    for (int i = 0; i < keys.length; i++) {
      if (data[i] != null) {        
        Point2D loc = rcxD.getLayout().getDataLocation(keys[i]);
        if (loc != null) {  // 7-15-04 I'm getting a null value here!
          Font mFont = appState_.getFontMgr().getFont(fonts[i]);
          double width;
          double height;
          if (isSingle[i]) {
            Rectangle2D bounds = mFont.getStringBounds((String)data[i], rcxD.getFrc());
            width = bounds.getWidth();
            height = bounds.getHeight();
          } else {
            Dimension dim = dl.calcKeyBounds((List<String>)data[i], rcxD.getLayout(), mFont, rcxD.getFrc());
            width = dim.getWidth();
            height = dim.getHeight();                        
          }  
          double minX = loc.getX() - (width / 2.0);
          double maxX = loc.getX() + (width / 2.0);    
          double maxY = loc.getY() + (height / 2.0); 
          double minY = loc.getY() - (height / 2.0);
          if (Bounds.intersects(minX, minY, maxX, maxY, pt.getX(), pt.getY())) {
            return (keys[i]);
          }
        }
      }
    } 
    return (null);
  }
  
  /***************************************************************************
  **
  ** Get the bounds of the model data
  */
  
  public Rectangle getModelDataBounds(DataAccessContext rcx) { 
    
    Rectangle retval = null;

    String[] keys = new String[] {Layout.DATE, Layout.ATTRIB, Layout.TITLE, Layout.KEY};
    int[] fonts = new int[] {FontManager.DATE, FontManager.DATE, FontManager.TITLE, FontManager.DATE};
    boolean[] isSingle = new boolean[] {true, true, true, false};    
    ModelData md = appState_.getDB().getModelData();
    String longName = rcx.getGenome().getLongName();
    if ((md == null) && ((longName == null) || longName.trim().equals(""))) {
      return (null);
    }
    Object[] data = new Object[4];
    data[0] = (md == null) ? null : md.getDate();
    data[1] = (md == null) ? null : md.getAttribution();
    data[2] = longName;
    if (md != null) {
      List<String> keyList = new ArrayList<String>();
      int numKey = md.getKeySize();
      for (int i = 0; i < numKey; i++) {
        keyList.add(md.getKey(i));
      }      
      data[3] = keyList;
    } else {
      data[3] = null;
    }

    DataLocator dl = new DataLocator(appState_, rcx);
    for (int i = 0; i < keys.length; i++) {
      if (data[i] != null) {        
        Point2D loc = rcx.getLayout().getDataLocation(keys[i]);
        if (loc != null) {  // 7-15-04 I'm getting a null value here!
          Font mFont = appState_.getFontMgr().getFont(fonts[i]);
          int width;
          int height;
          if (isSingle[i]) {
            Rectangle2D bounds = mFont.getStringBounds((String)data[i], rcx.getFrc());
            width = (int)bounds.getWidth();
            height = (int)bounds.getHeight();
          } else {
            Dimension dim = dl.calcKeyBounds((List<String>)data[i], rcx.getLayout(), mFont, rcx.getFrc());
            width = (int)dim.getWidth();
            height = (int)dim.getHeight();                        
          }  
          int minX = (int)loc.getX() - (width / 2);
          int minY = (int)loc.getY() - (height / 2);
          Rectangle newRect = new Rectangle(minX, minY, width, height);
          UiUtil.forceToGrid(newRect, UiUtil.GRID_SIZE);
          if (retval == null) {
            retval = newRect;
          } else {
            Bounds.tweakBounds(retval, newRect);
          }
        }
      }
    } 
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Render groups in the background.  Used to draw group backgrounds for
  ** groups actually present in the given genome.
  */
  
	private void renderGroupsInBackground(ModelObjectCache moc, DataAccessContext rcx,
			GenomeInstance root, GenomeInstance genome, List<GroupFree.ColoredRect> collectRects) {

		//
		// If we don't have a parent instance, we just render the groups. If we
		// do, we use the parent to render the groups that are subsetted in the
		// child.
		//
		List<String> order = rcx.getLayout().getGroupDrawingOrder();

		GroupFree renderer;
		for (int i = 0; i <= GroupProperties.MAX_SUBGROUP_LAYER; i++) {
			Iterator<Group> grit = genome.getGroupIteratorFromList(order);
			while (grit.hasNext()) {
				Group group = grit.next();
				String childId = group.getID();
				GenomeInstance useToRender = null;
				String groupRef = null;
				GroupFree.AugmentedExtraInfo augExtra = new GroupFree.AugmentedExtraInfo();
				augExtra.extra = null;
				augExtra.passOut = collectRects;
				GroupProperties gp;
				if (root == null) {
					useToRender = genome;
					groupRef = group.getID();
					gp = rcx.getLayout().getGroupProperties(Group.getBaseID(groupRef));
					if (gp.getLayer() != i) {
						continue;
					}
					renderer = gp.getRenderer();
				} else {
					augExtra.extra = new GroupFree.ExtraInfo();
					useToRender = root;
					groupRef = group.getReference();
					gp = rcx.getLayout().getGroupProperties(Group.getBaseID(groupRef));
					if (gp.getLayer() != i) {
						continue;
					}
					renderer = gp.getRenderer();
					augExtra.extra.vizVal = rcx.gsm.getGroupVisibility(genome.getID(), group.getID(), rcx);
					augExtra.extra.dropName = group.isUsingParent();
					augExtra.extra.genomeIsRoot = false;
					String activeSubsetID = group.getActiveSubset();
					if (activeSubsetID != null) {
						String inherit = Group.buildInheritedID(activeSubsetID,
								root.getGeneration());
						Group activeSubset = root.getGroup(inherit);
						if (activeSubset == null) {
							Iterator<Group> pit = root.getGroupIterator();
							while (pit.hasNext()) {
								Group gr = pit.next();
								System.err.println(gr.getID());
								throw new IllegalStateException();
							}
						}
						augExtra.extra.replacementName = activeSubset
								.getInheritedDisplayName(genome);
						GroupProperties asgp = rcx.getLayout().getGroupProperties(Group
								.getBaseID(activeSubsetID));
						augExtra.extra.replacementColor = asgp.getColor(false, rcx.cRes);
					}
					String inherit = Group.buildInheritedID(groupRef,
							root.getGeneration());
					group = root.getGroup(inherit);
				}
				// The renderer has to know the group's ID in the genome so that
				// it can be exported in the web application.
				augExtra.childId_ = childId;
			  DataAccessContext rcxU = new DataAccessContext(rcx);
        rcxU.setGenome(useToRender);
				rcxU.pushGhosted(false);				
				renderer.render(moc, group, null, rcxU, augExtra);
				rcxU.popGhosted();
			}
		}
		return;
	}
  
  /***************************************************************************
  **
  ** Show ghosted groups not used in child.  During pulldown, we only show
  ** groups present in immediate parent, sized by root.
  */
  
	private void presentUnusedGroups(ModelObjectCache moc, DataAccessContext rcx,
			GenomeInstance parent, GenomeInstance genome) {
	  
		//
		// Ghost groups not included in the child.
		//

		List<String> order = rcx.getLayout().getGroupDrawingOrder();
		Iterator<Group> grit = parent.getGroupIteratorFromList(order);
		while (grit.hasNext()) {
			Group group = grit.next();
			String childId = group.getID();
			String groupRef = group.getID();
			GroupProperties gp = rcx.getLayout().getGroupProperties(Group.getBaseID(groupRef));
			if (gp.getLayer() != 0) {
				continue;
			}
			Iterator<Group> cgrit = genome.getGroupIteratorFromList(order);
			boolean needToGhost = true;
			while (cgrit.hasNext()) {
				Group cgroup = cgrit.next();
				if (Group.getBaseID(cgroup.getReference()).equals(
						Group.getBaseID(groupRef))) {
					needToGhost = false;
					break;
				}
			}
			GroupFree renderer = gp.getRenderer();
			if (needToGhost) { // not in child - ghost it:
				group = ((GenomeInstance)rcx.getGenome()).getGroup(Group.getBaseID(groupRef));
				rcx.pushGhosted(true);
				GroupFree.AugmentedExtraInfo augExtra = new GroupFree.AugmentedExtraInfo();
				augExtra.childId_ = childId;
				renderer.render(moc, group, null, rcx, augExtra);
				rcx.popGhosted();
			}
		}
		return;
	}   
  
  /***************************************************************************
  **
  ** Render groups in foregrond.  Used to obscure toggled groups.
  */
  
	private void renderGroupsInForeground(ModelObjectCache moc, DataAccessContext rcx,
																				GenomeInstance parent, GenomeInstance genome) {

		//
		// If we have no parent, we crank through collapsed groups to cover them up.
		// If we have a parent, we do the same, using the parent to do the
		// rendering.
		//

		GenomeInstance iterateOver = (parent == null) ? genome : parent;
		List<String> order = rcx.getLayout().getGroupDrawingOrder();
		Iterator<Group> grit = iterateOver.getGroupIteratorFromList(order);
		
		while (grit.hasNext()) {
			Group group = grit.next();
			if (parent == null) {
				String groupRef = group.getID();
				GroupProperties gp = rcx.getLayout().getGroupProperties(groupRef);
				GroupSettings.Setting groupViz = rcx.gsm.getGroupVisibility(genome.getID(), groupRef, rcx);
				int layer = gp.getLayer();
				if ((layer == 0) && (groupViz != GroupSettings.Setting.ACTIVE)) {
					GroupFree renderer = gp.getRenderer();
					DataAccessContext rcxU = new DataAccessContext(rcx);
          rcxU.setGenome(genome);
					rcxU.pushGhosted(false);
					renderer.render(moc, group, null, rcxU, null);
					rcxU.popGhosted();
				}
			} else {
				String groupRef = group.getID();
				GroupProperties gp = rcx.getLayout().getGroupProperties(groupRef);
				if (gp.getLayer() != 0) {
					continue;
				}
				Iterator<Group> cgrit = genome.getGroupIteratorFromList(order);
				while (cgrit.hasNext()) {
					Group cgroup = cgrit.next();
					if (Group.getBaseID(cgroup.getReference()).equals(groupRef)) {
						GroupSettings.Setting groupViz = rcx.gsm.getGroupVisibility(genome.getID(), cgroup.getID(), rcx);
						if (groupViz != GroupSettings.Setting.ACTIVE) {
							GroupFree.AugmentedExtraInfo augExtra = new GroupFree.AugmentedExtraInfo();
							augExtra.extra = new GroupFree.ExtraInfo();
							augExtra.extra.vizVal = groupViz;
							augExtra.extra.dropName = cgroup.isUsingParent();
							augExtra.extra.genomeIsRoot = false;
							String activeSubsetName = cgroup.getActiveSubset();
							if (activeSubsetName != null) {
								activeSubsetName = Group.buildInheritedID(activeSubsetName, parent.getGeneration());
								Group activeSubset = parent.getGroup(activeSubsetName);
								GroupProperties asgp = rcx.getLayout().getGroupProperties(activeSubsetName);
								augExtra.extra.replacementName = activeSubset.getName();
								augExtra.extra.replacementColor = asgp.getColor(false, rcx.cRes);
							}
							GroupFree renderer = gp.getRenderer();
              DataAccessContext rcxU = new DataAccessContext(rcx);
              rcxU.setGenome(parent);
              rcxU.pushGhosted(false);
              renderer.render(moc, group, null, rcxU, augExtra);
              rcxU.popGhosted();
						}
						break;
					}
				}
			}
		}
		return;
	}
 
  /***************************************************************************
  **
  ** Do group intersection test
  */
  
  private Intersection intersectGroups(DataAccessContext rcx, 
                                       GenomeInstance parent, GenomeInstance genome, Point2D pt,
                                       boolean inactiveOnly) {

    //
    // If we don't have a parent, just do intersection test.  If we
    // do, we use the parent to check out intersections.  We can be instructed
    // to either intersect only inactive groups, or all groups
    //
    
    List<String> order = rcx.getLayout().getGroupIntersectionOrder();
    Iterator<Group> grit = genome.getGroupIteratorFromList(order);
    while (grit.hasNext()) {
      Group group = grit.next();
      GenomeInstance useToRender = null;
      String groupRef = null;
      String origID = group.getID();
      if (parent == null) {
        useToRender = genome;        
        groupRef = origID;
      } else { 
        useToRender = parent;        
        groupRef = group.getReference();
        group = parent.getGroup(Group.getBaseID(groupRef));
      }  
      GroupProperties gp = rcx.getLayout().getGroupProperties(Group.getBaseID(groupRef));
      GroupSettings.Setting groupViz = rcx.gsm.getGroupVisibility(genome.getID(), origID, rcx);
      boolean checkForIntersect = ((gp.getLayer() == 0) && 
                                   ((inactiveOnly && (groupViz != GroupSettings.Setting.ACTIVE)) ||
                                    !inactiveOnly));
      if (checkForIntersect) {
        GroupFree.AugmentedExtraInfo augExtra = new GroupFree.AugmentedExtraInfo();
        augExtra.extra = new GroupFree.ExtraInfo();
        augExtra.extra.genomeIsRoot = (parent == null);
        GroupFree renderer = gp.getRenderer();
        DataAccessContext rcxU = new DataAccessContext(rcx);
        rcxU.setGenome(useToRender);
        Intersection inter = renderer.intersects(group, pt, rcxU, augExtra);
        if (inter != null) {
          return (new Intersection(origID, inter.getSubID(), 0.0));
        }
      }
    }
    return (null);
  }

  /***************************************************************************
  **
  ** Answer if we need to do fancy opaque stuff
  */
  
  private boolean overlayIsOpaque(Layout layout, String currentOverlay) {    

    if (currentOverlay == null) {
      return (false);
    }  
    NetOverlayProperties nop = layout.getNetOverlayProperties(currentOverlay);
    return (nop.getType() == NetOverlayProperties.OvrType.OPAQUE);
  }
  
  /***************************************************************************
  **
  ** Just answer what masking restriction exists
  */
  
  private int needMaskingReduction(Layout layout, String currentOverlay, int intersectMask) {    
    //
    // Quick kills:
    //
     
    if (currentOverlay == null) {
      return (NetModuleFree.CurrentSettings.NOTHING_MASKED);
    }  
    if (intersectMask == NetModuleFree.CurrentSettings.NOTHING_MASKED) {
      return (NetModuleFree.CurrentSettings.NOTHING_MASKED);
    } 
    NetOverlayProperties nop = layout.getNetOverlayProperties(currentOverlay);
    if (nop.getType() != NetOverlayProperties.OvrType.OPAQUE) {
      return (NetModuleFree.CurrentSettings.NOTHING_MASKED);
    }
    if (intersectMask == NetModuleFree.CurrentSettings.ALL_MASKED) {
      return (NetModuleFree.CurrentSettings.ALL_MASKED);
    }
    
    if (intersectMask != NetModuleFree.CurrentSettings.NON_MEMBERS_MASKED) {
      throw new IllegalArgumentException();
    }
    
    return (NetModuleFree.CurrentSettings.NON_MEMBERS_MASKED);

  }  
  
  /***************************************************************************
  **
  ** Figure out what we can click on due overlay masking.
  **
  */
  
  private MaskData maskingReduction(DataAccessContext rcxM) {
 
    MaskData retval = new MaskData();
    retval.nonBlackHoles = nonBlackHoles(rcxM);
    if (retval.nonBlackHoles != null) {
      retval.candidates = new HashSet<String>();
    }
     
    String ovrID = rcxM.oso.getCurrentOverlay();
    if (ovrID == null) {
      return (retval);
    }    
   
    NetOverlayOwner owner = rcxM.getGenomeSource().getOverlayOwnerFromGenomeKey(rcxM.getGenomeID());
    NetworkOverlay novr = owner.getNetworkOverlay(ovrID);
    NetOverlayProperties nop = rcxM.getLayout().getNetOverlayProperties(ovrID);
    boolean isOpq = (nop.getType() == NetOverlayProperties.OvrType.OPAQUE);
    NetModuleFree.CurrentSettings settings = rcxM.oso.getCurrentOverlaySettings();
 
    Iterator<NetModule> modit = novr.getModuleIterator();
    while (modit.hasNext()) {
      NetModule nmod = modit.next();
      String modID = nmod.getID();
      NetModuleProperties nmp = nop.getNetModuleProperties(modID);
      boolean isRevealed = rcxM.oso.getRevealedModules().set.contains(modID);
      boolean quickFade = (nmp.getNameFadeMode() == NetModuleProperties.FADE_QUICKLY);
      boolean nameHiding = (isRevealed && isOpq && quickFade);
      boolean labelViz = !nameHiding && ((quickFade) ? settings.fastDecayLabelVisible : true);
      Boolean labelVizObj = new Boolean(labelViz);
      retval.labelViz.put(modID, labelVizObj);
      if ((retval.candidates != null) && retval.nonBlackHoles.contains(modID)) {
        Iterator<NetModuleMember> memit = nmod.getMemberIterator();
        while (memit.hasNext()) {
          NetModuleMember nmm = memit.next();
          retval.candidates.add(nmm.getID());
        }
      }
    }
      
    return (retval);
  }
  
 /***************************************************************************
  **
  ** Answers if we are intersecting an already selected link segment
  */
  
  private boolean intersectsSelectedLinkSegment(Linkage link, Intersection inter, 
                                                Layout layout) {  
  
    Set<String> justKeys = selectionKeys_.keySet();
    Intersection selected = linkIsSelected(justKeys, selectionKeys_, layout, link);
    if (selected == null) {
      return (false);
    }
    LinkSegmentID[] selsegs = selected.segmentIDsFromIntersect();
    if (selsegs == null) {  // whole link is selected...
      return (true);
    }
        
    LinkSegmentID[] intersegs = inter.segmentIDsFromIntersect();
    if (intersegs == null) {  // is this even possible??
      return (true);
    }
    for (int i = 0; i < selsegs.length; i++) {
      for (int j = 0; j < intersegs.length; j++) {
        if (selsegs[i].equals(intersegs[j])) {
          return (true);
        }
      }
    }
    return (false); 
  }        
  
  /***************************************************************************
  **
  ** Select an item
  */
  
  public List<Intersection.AugmentedIntersection> selectionGuts(int x, int y, DataAccessContext rcxS, boolean nodesFirst, 
                                                                boolean omitGroups, boolean doOverlay) {
 
    String currentOverlay = rcxS.oso.getCurrentOverlay();
    TaggedSet currentNetMods = rcxS.oso.getCurrentNetModules();
    int intersectMask = rcxS.oso.getCurrentOverlaySettings().intersectionMask;
    Set<String> currentNetModSet = (currentNetMods == null) ? null : currentNetMods.set;
    TaggedSet revKeys = rcxS.oso.getRevealedModules();
    Set<String> revMods = (revKeys == null) ? null : revKeys.set; 

    //
    // Crank through the items and ask the renderers for an intersection
    //

    Genome genome = rcxS.getGenome();
    if (genome == null) { // not showing anything
      return (null);
    }
    
    Point2D pt = new Point2D.Float(x, y);
    ArrayList<Intersection.AugmentedIntersection> retval = new ArrayList<Intersection.AugmentedIntersection>();
    
    MaskData overMask = maskingReduction(rcxS);   
        
    //
    // Even if nothing survives masking, we still want to be able to intersect modules
    // if asked to do so.  If everything is opaque, intersections in the interior of
    // a module that are not being revealed are OK.  If there are selections, they
    // can be showing up through the opaque overlay, and we will need to look for them:
    // overMask.candidates == null means no masking; empty candidates means no further selection needed.
    
    if ((overMask.candidates != null) && overMask.candidates.isEmpty() && selectionKeys_.isEmpty()) {
      if (doOverlay) {

        Intersection inter = intersectANetModuleElement(x, y, rcxS, NetModuleIntersect.NET_MODULE_BOUNDARY);
        if (inter != null) {
          Intersection.AugmentedIntersection ait =
            new Intersection.AugmentedIntersection(inter, Intersection.IS_MODULE);
          retval.add(ait);
        }
        
        inter = intersectANetModuleElement(x, y, rcxS, NetModuleIntersect.NET_MODULE_NAME);
        if (inter != null) {
          Intersection.AugmentedIntersection ait =
            new Intersection.AugmentedIntersection(inter, Intersection.IS_MODULE);
          retval.add(ait);
        }
       
        inter = intersectANetModuleElement(x, y, rcxS, NetModuleIntersect.NET_MODULE_INTERIOR);
        if (inter != null) {
          if (!overMask.nonBlackHoles.contains(inter.getObjectID())) {     
            Intersection.AugmentedIntersection ait =
              new Intersection.AugmentedIntersection(inter, Intersection.IS_MODULE);
            retval.add(ait);
          }
        }
        
        inter = intersectNetModuleLinks(x, y, rcxS);
        if (inter != null) {
          Intersection.AugmentedIntersection ait =
            new Intersection.AugmentedIntersection(inter, Intersection.IS_MODULE_LINK_TREE);
          retval.add(ait);
        }   
      } 
      return ((retval.isEmpty()) ? null : retval);
    }
    
    //
    // First intersect collapsed groups, which mask their contents.  When we "omit groups",
    // we intersect them, but don't return the intersection.  This is because groups
    // can be intersected, but not selected.
    //
    
    if (genome instanceof GenomeInstance) {
      GenomeInstance child = ((GenomeInstance)genome);
      GenomeInstance parentvfg = child.getVfgParentRoot();  
      Intersection groupCheck = intersectGroups(rcxS, parentvfg, child, pt, true);
      groupCheck = filterForModuleMask(rcxS, pt, groupCheck, currentOverlay, currentNetModSet, revMods, intersectMask);
      if (groupCheck != null) {
        if (omitGroups) {
          return (null);
        } else {
          retval.add(new Intersection.AugmentedIntersection(groupCheck, Intersection.IS_GROUP));
          return (retval);
        }
      }
    }
    
    // Sometimes we want to intersect nodes first
    if (nodesFirst) {
      Iterator<Gene> git = genome.getGeneIterator();    
      while (git.hasNext()) {
        Gene gene = git.next();
        String gid = gene.getID();
        IRenderer render = rcxS.getLayout().getNodeProperties(gene.getID()).getRenderer();      
        Intersection inter = render.intersects(gene, pt, rcxS, null);
        if ((inter != null) && 
            ((overMask.candidates == null) || 
              overMask.candidates.contains(inter.getObjectID()) ||
              selectionKeys_.containsKey(gid))) {
          retval.add(new Intersection.AugmentedIntersection(inter, Intersection.IS_GENE));
        }
      }
      Iterator<Node> nit = genome.getNodeIterator();
      while (nit.hasNext()) {
        Node node = nit.next();
        String nid = node.getID();
        IRenderer render = rcxS.getLayout().getNodeProperties(node.getID()).getRenderer();      
        Intersection inter = render.intersects(node, pt, rcxS, null);
        if ((inter != null) && 
            ((overMask.candidates == null) || 
              overMask.candidates.contains(inter.getObjectID()) ||
              selectionKeys_.containsKey(nid))) {
           retval.add(new Intersection.AugmentedIntersection(inter, Intersection.IS_NODE));
        }      
      }
    }
      
    if (doOverlay) {
      Intersection inter = intersectANetModuleElement(x, y, rcxS, NetModuleIntersect.NET_MODULE_BOUNDARY);
      if (inter != null) {
        Intersection.AugmentedIntersection ait =
          new Intersection.AugmentedIntersection(inter, Intersection.IS_MODULE);
        retval.add(ait);
      }
         
      inter = intersectANetModuleElement(x, y, rcxS, NetModuleIntersect.NET_MODULE_NAME);
      if (inter != null) {
        Intersection.AugmentedIntersection ait =
          new Intersection.AugmentedIntersection(inter, Intersection.IS_MODULE);
        retval.add(ait);
      }
     
      NetModuleFree.CurrentSettings settings = rcxS.oso.getCurrentOverlaySettings();
      if (settings.intersectionMask == NetModuleFree.CurrentSettings.ALL_MASKED) {
        inter = intersectANetModuleElement(x, y, rcxS, NetModuleIntersect.NET_MODULE_INTERIOR);
        if (inter != null) {
          if ((overMask.nonBlackHoles != null) && !overMask.nonBlackHoles.contains(inter.getObjectID())) {     
            Intersection.AugmentedIntersection ait =
              new Intersection.AugmentedIntersection(inter, Intersection.IS_MODULE);
             retval.add(ait);
          }
        }
      }
      
      inter = intersectNetModuleLinks(x, y, rcxS);
      if (inter != null) {
        Intersection.AugmentedIntersection ait =
          new Intersection.AugmentedIntersection(inter, Intersection.IS_MODULE_LINK_TREE);
         retval.add(ait);
      }
    } 

    if (!linksAreHidden(rcxS)) {
      HashSet<String> seenSrc = new HashSet<String>();
      Iterator<Linkage> lit = genome.getLinkageIterator();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        String srcID = link.getSource();
        if (seenSrc.contains(srcID)) {
          continue;
        }
        seenSrc.add(srcID);
        LinkageFree render = (LinkageFree)rcxS.getLayout().getLinkProperties(link.getID()).getRenderer();      
        Intersection inter = render.intersects(link, pt, rcxS, null);
        // already selected gets a free pass
        if ((inter != null) && intersectsSelectedLinkSegment(link, inter, rcxS.getLayout())) {  
          retval.add(new Intersection.AugmentedIntersection(inter, Intersection.IS_LINK));
        }   
        inter = filterForModuleMask(rcxS, pt, inter, currentOverlay, currentNetModSet, revMods, intersectMask);
        if (inter != null) {    
          retval.add(new Intersection.AugmentedIntersection(inter, Intersection.IS_LINK));
        }
      }
    }
    
    if (!nodesFirst) {
      Iterator<Gene> git = genome.getGeneIterator();    
      while (git.hasNext()) {
        Gene gene = git.next();
        String gid = gene.getID();
        IRenderer render = rcxS.getLayout().getNodeProperties(gene.getID()).getRenderer();      
        Intersection inter = render.intersects(gene, pt, rcxS, null);
        if ((inter != null) && 
            ((overMask.candidates == null) || 
              overMask.candidates.contains(inter.getObjectID()) ||
              selectionKeys_.containsKey(gid))) {
          retval.add(new Intersection.AugmentedIntersection(inter, Intersection.IS_NODE));
        }
      }
 
      Iterator<Node> nit = genome.getNodeIterator();
      while (nit.hasNext()) {
        Node node = nit.next();
        String nid = node.getID();
        IRenderer render = rcxS.getLayout().getNodeProperties(node.getID()).getRenderer();      
        Intersection inter = render.intersects(node, pt, rcxS, null);   
        if ((inter != null) && 
            ((overMask.candidates == null) || 
              overMask.candidates.contains(inter.getObjectID()) ||
              selectionKeys_.containsKey(nid))) {
           retval.add(new Intersection.AugmentedIntersection(inter, Intersection.IS_GENE));
        }      
      }
    }
 
    Iterator<Note> ntit = genome.getNoteIterator();
    while (ntit.hasNext()) {
      Note note = ntit.next();
      IRenderer render = rcxS.getLayout().getNoteProperties(note.getID()).getRenderer();      
      Intersection inter = render.intersects(note, pt, rcxS, null);
      inter = filterForModuleMask(rcxS, pt, inter, currentOverlay, currentNetModSet, revMods, intersectMask);
      if (inter != null) {
         retval.add(new Intersection.AugmentedIntersection(inter, Intersection.IS_NOTE));
      }      
    }
    
    if (retval.isEmpty()) {
      if (genome instanceof GenomeInstance) {     
        GenomeInstance child = ((GenomeInstance)genome);
        GenomeInstance parentvfg = child.getVfgParentRoot();
        Intersection groupCheck = intersectGroups(rcxS, parentvfg, child, pt, false);
        groupCheck = filterForModuleMask(rcxS, pt, groupCheck, currentOverlay, currentNetModSet, revMods, intersectMask);
        if (groupCheck != null) {
          if (omitGroups) {
            return (null);
          } else {
            retval.add(new Intersection.AugmentedIntersection(groupCheck, Intersection.IS_GROUP));
            return (retval);
          }
        }
      }
    }
    return ((retval.isEmpty()) ? null : retval);
  }

  /***************************************************************************
  **
  ** Intersect a bunch of things, return a list of intersections
  */
  
  private List<Intersection> selectionGuts(Rectangle rect, DataAccessContext rcxG, boolean nodesFirst) {

    ArrayList<Intersection> retval = new ArrayList<Intersection>();
    String currentOverlay = rcxG.oso.getCurrentOverlay();
    TaggedSet currentNetMods = rcxG.oso.getCurrentNetModules();
    int intersectMask = rcxG.oso.getCurrentOverlaySettings().intersectionMask;
    Set<String> currentNetModSet = (currentNetMods == null) ? null : currentNetMods.set;
    TaggedSet revKeys = rcxG.oso.getRevealedModules();
    Set<String> revMods = (revKeys == null) ? null : revKeys.set;    
       
    //
    // Crank through the items and ask the renderers for an intersection
    //
 
    MaskData overMask = maskingReduction(rcxG);
    
    if ((overMask.candidates != null) && overMask.candidates.isEmpty()) {
      return (retval);
    }    
       
    //
    // First intersect collapsed groups, which mask their contents.
    //
    // FIX ME: box intersect groups
    /*
    if (genome instanceof GenomeInstance) {
      GenomeInstance child = ((GenomeInstance)genome);
      GenomeInstance parentvfg = child.getVfgParentRoot();            
      Intersection groupCheck = intersectGroups(parentvfg, child, pt, layout, true);
      if (groupCheck != null) {
        return (groupCheck);
      }
    }
    */
    
    // Sometimes we want to intersect nodes first
    if (nodesFirst) {
      Iterator<Gene> git = rcxG.getGenome().getGeneIterator();    
      while (git.hasNext()) {
        Gene gene = git.next();
        IRenderer render = rcxG.getLayout().getNodeProperties(gene.getID()).getRenderer();      
        Intersection inter = render.intersects(gene, rect, false, rcxG, null);
        if ((inter != null) && ((overMask.candidates == null) || overMask.candidates.contains(inter.getObjectID()))) {
          retval.add(inter);
        }
      }
      Iterator<Node> nit = rcxG.getGenome().getNodeIterator();
      while (nit.hasNext()) {
        Node node = nit.next();
        IRenderer render = rcxG.getLayout().getNodeProperties(node.getID()).getRenderer();      
        Intersection inter = render.intersects(node, rect, false, rcxG, null);
        if ((inter != null) && ((overMask.candidates == null) || overMask.candidates.contains(inter.getObjectID()))) {
          retval.add(inter);
        }      
      }
    }

    if (!linksAreHidden(rcxG)) {
      ArrayList<Intersection> linkOnlyRetval = new ArrayList<Intersection>();
      HashSet<String> seenSrc = new HashSet<String>();
      Iterator<Linkage> lit = rcxG.getGenome().getLinkageIterator();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        // 1/30/09: NOTE that every link through a segment is getting intersected, not just one
        // Kinda sucks!  But the selection list keyed by link iD, so which one would
        // you use?  FIX THIS?
        String srcID = link.getSource();
        if (seenSrc.contains(srcID)) {
          continue;
        }
        seenSrc.add(srcID);
        LinkageFree render = (LinkageFree)rcxG.getLayout().getLinkProperties(link.getID()).getRenderer(); 
        Intersection inter = render.intersects(link, rect, false, rcxG, null);
        if (inter != null) {
          linkOnlyRetval.add(inter);
        }
      }

      if (!linkOnlyRetval.isEmpty()) {
        List<Intersection> filtered = filterLinksForModuleMask(rcxG, linkOnlyRetval,
                                                               currentOverlay, currentNetModSet, revMods, intersectMask);
        retval.addAll(filtered);
      }
    }
 
    if (!nodesFirst) {
      Iterator<Gene> git = rcxG.getGenome().getGeneIterator();    
      while (git.hasNext()) {
        Gene gene = git.next();
        IRenderer render = rcxG.getLayout().getNodeProperties(gene.getID()).getRenderer();      
        Intersection inter = render.intersects(gene, rect, false, rcxG, null);
        if ((inter != null) && ((overMask.candidates == null) || overMask.candidates.contains(inter.getObjectID()))) {
          retval.add(inter);
        }
      }
 
      Iterator<Node> nit = rcxG.getGenome().getNodeIterator();
      while (nit.hasNext()) {
        Node node = nit.next();
        IRenderer render = rcxG.getLayout().getNodeProperties(node.getID()).getRenderer();      
        Intersection inter = render.intersects(node, rect, false, rcxG, null);
        if ((inter != null) && ((overMask.candidates == null) || overMask.candidates.contains(inter.getObjectID()))) {
          retval.add(inter);
        }      
      }
    }
    
    // FIX ME: box intersect groups
    /* 
    if (genome instanceof GenomeInstance) {     
      Iterator ntit = ((GenomeInstance)genome).getNoteIterator();
      while (ntit.hasNext()) {
        Note note = (Note)ntit.next();
        IRenderer render = layout.getNoteProperties(note.getID()).getRenderer();      
        Intersection inter = render.intersects(genome, note, layout, frc_, pt);
        if (inter != null) {
          return (inter);
        }      
      }      

      GenomeInstance child = ((GenomeInstance)genome);
      GenomeInstance parentvfg = child.getVfgParentRoot();            
      Intersection groupCheck = intersectGroups(parentvfg, child, pt, layout, false);
      if (groupCheck != null) {
        return (groupCheck);
      }
    }
     */
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Render the floater
  */
  
  private void renderFloater(ModelObjectCache floaterCache, DataAccessContext rcxF) { //boolean showBubbles, double pixDiam) {
    if (floater_ == null) {
      return;
    } else if (floater_ instanceof ArrayList) {
      if (floatPoint_ != null) {
        ProtoLinkFree renderer = new ProtoLinkFree();
        ArrayList augment = new ArrayList((ArrayList)floater_);
        augment.add(floatPoint_);
        renderer.render(floaterCache, augment);
      }
    } else if (floater_ instanceof Rectangle2D) {
    	Rectangle2D frect = (Rectangle2D)floater_;
    	
    	// TODO check getX() or getMinX()
    	double x = frect.getX();
    	double y = frect.getY();
    	double w = frect.getWidth();
    	double h = frect.getHeight();

      Color drawColor = (floaterColor_ == null) ? Color.black : floaterColor_;
      
      // TODO correct stroke
      BasicStroke stroke = new BasicStroke();
      ModelObjectCache.Rectangle rect = new ModelObjectCache.Rectangle(x, y, w, h, DrawMode.DRAW, drawColor, stroke);
      
      // TODO fix layers
      CommonCacheGroup group = new CommonCacheGroup("_float", "floater", "rect");
      group.addShape(rect, new Integer(0), new Integer(0));
      floaterCache.addGroup(group);
      
    } else if (floater_ instanceof GenomeItem) {
      GenomeItem git = (GenomeItem)floater_;
      IRenderer render = null;
      Object optArg = null;
      if (git instanceof Node) {
        render = floaterLayout_.getNodeProperties(git.getID()).getRenderer();
      } else if (git instanceof Linkage) {
        render = (LinkageFree)floaterLayout_.getLinkProperties(git.getID()).getRenderer();
      } else if (git instanceof Group) {
        GroupFree gFRender = new GroupFree();
        GroupFree.AugmentedExtraInfo augExtra = new GroupFree.AugmentedExtraInfo();
        augExtra.extra = new GroupFree.ExtraInfo();
        augExtra.extra.vizVal = GroupSettings.Setting.ACTIVE;
        optArg = augExtra;
        if (floaterCache != null) {
          gFRender.render(floaterCache, git, null, rcxF, optArg);
        }
        return;
      } else if (git instanceof Note) {
        render = floaterLayout_.getNoteProperties(git.getID()).getRenderer();
      }
      
      if (floaterCache != null) {
	      render.render(floaterCache, git, null, rcxF, optArg);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the floater layout
  */
  
  public Layout getFloaterLayout() {
    return (floaterLayout_);
  }

  /***************************************************************************
  **
  ** Add or merge two intersections
  */
  
  private void addOrMerge(HashMap<String, Intersection> map, Intersection oldInt, 
                          Intersection newInt, boolean sameParent) {
    //
    // If the two intersections have different parents, we just add the new
    // one.  Otherwise, we merge the two into one.
    //
    
    if (!sameParent) {
      map.put(newInt.getObjectID(), newInt);
      return;
    }
    //
    // Linkage segments can be merged; other intersections with subIDs
    // (e.g. pads on genes) are not relevant here.  This is kinda icky!
    //
    
    if (oldInt.canMerge() && newInt.canMerge()) {
      Intersection mergeInt = Intersection.mergeIntersect(oldInt, newInt);       
      map.put(mergeInt.getObjectID(), mergeInt);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Add or merge two intersections
  */
  
  private void intersect(HashMap<String, Intersection> map, Intersection oldInt, Intersection newInt) {
    
    Intersection mergeInt = Intersection.intersectComplement(oldInt, newInt);       
    map.put(mergeInt.getObjectID(), mergeInt);
    return;
  } 
  
  /***************************************************************************
  **
  ** Duplicate the current selections list
  */
  
  private HashMap<String, Intersection> duplicateSelections() {
    return (duplicateSelections(selectionKeys_));
  }

  /***************************************************************************
  **
  ** Duplicate the given selections list
  */
  
  private HashMap<String, Intersection> duplicateSelections(HashMap<String, Intersection> oldMap) {
    HashMap<String, Intersection> retval = new HashMap<String, Intersection>(); 
    Iterator<String> sit = oldMap.keySet().iterator();
    while (sit.hasNext()) {
      String key = sit.next();
      Intersection inter = oldMap.get(key);
      // FIX ME!  AACCK! Does this need refactoring, OR WHAT???
      if (inter.canCopy()) {
        retval.put(key, new Intersection(inter));
      } else if (Intersection.isLinkageSubID(inter)) {
        retval.put(key, Intersection.copyIntersection(inter));
      } else if (Intersection.isGroupSubID(inter)) {
        retval.put(key, Intersection.copyGroupIntersection(inter));        
      } else {
        throw new IllegalStateException();
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Common gaggle selection operations
  */
  
  private void gaggleSelectionSupport(GooseAppInterface goose, DataAccessContext rcx) {
    SelectionSupport ss = goose.getSelectionSupport();
    if ((rcx.getGenome() == null) || selectionKeys_.isEmpty()) {
      ss.setSelections(new ArrayList<String>());
      ss.setOutboundNetwork(new SelectionSupport.NetworkForSpecies());
      return;
    }
    Genome gen = rcx.getGenome();
    Map<String, String> uniques = rcx.getDBGenome().genUnique();
    Set<String> selections = cullNodeSelections(gen, uniques, false);

    ss.setSelections(new ArrayList<String>(selections));
    SelectionSupport.NetworkForSpecies nfs = selectionsToGaggleNets(gen, uniques, rcx.getLayout());
    ss.setOutboundNetwork(nfs);
    return;
  }   
  
  
  /***************************************************************************
  **
  ** Cull out node selections from the current set of selection keys
  */
  
  private Set<String> cullNodeSelections(Genome genome, Map<String, String> uniques, boolean idOnly) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> sit = selectionKeys_.keySet().iterator();
    while (sit.hasNext()) {
      String key = sit.next();
      Intersection inter = selectionKeys_.get(key);
      String itemID = inter.getObjectID();
      Iterator<Node> git = genome.getAllNodeIterator();    
      while (git.hasNext()) {
        Node node = git.next();
        String nodeID = node.getID();
        if (itemID.equals(nodeID)) {
          if (idOnly) {
            retval.add(nodeID);
          } else if (uniques != null) {
            retval.add(uniques.get(GenomeItemInstance.getBaseID(nodeID)));
          } else {
            retval.add(node.getName());
          }
          break;
        }
      }
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Generate gaggle-transmitted current set of selection keys
  */
  
  private SelectionSupport.NetworkForSpecies selectionsToGaggleNets(Genome gen, Map<String, String> uniques, Layout layout) {
    if (!(gen instanceof DBGenome)) {
      return (null);
    }
    DBGenome genome = (DBGenome)gen;
    
    //
    // Get links through selected segments:
    //
    
    HashSet<String> selectedLinks = new HashSet<String>();
    Iterator<String> sit = selectionKeys_.keySet().iterator();
    while (sit.hasNext()) {
      String key = sit.next();
      Intersection inter = selectionKeys_.get(key);
      String itemID = inter.getObjectID();
      Linkage lnk = genome.getLinkage(itemID);
      if (lnk == null) {
        continue;
      }
      LinkProperties lp = layout.getLinkProperties(itemID);
      LinkSegmentID[] segIDs = inter.segmentIDsFromIntersect();      
      if (segIDs == null) {
        List<String> allLinks = ((BusProperties)lp).getLinkageList();
        selectedLinks.addAll(allLinks);
      } else {      
        for (int i = 0; i < segIDs.length; i++) {
          Set<String> resolved = ((BusProperties)lp).resolveLinkagesThroughSegment(segIDs[i]);
          selectedLinks.addAll(resolved);
        }
      }
    }
    
    //
    // Selected Nodes:
    
    Set<String> selectedNodes = cullNodeSelections(genome, null, true);
        
    //
    // Get unselected endpoint nodes to support the selected links:
    //
    
    Iterator<String> slit = selectedLinks.iterator();
    while (slit.hasNext()) {
      String linkID = slit.next();
      Linkage link = genome.getLinkage(linkID);
      String src = link.getSource();
      String trg = link.getTarget();
      if (!selectedNodes.contains(src)) {
        selectedNodes.add(src);
      }
      if (!selectedNodes.contains(trg)) {
        selectedNodes.add(trg);
      }       
    }
        
    //
    // Get (optional) links to connect selected nodes:
    //
    
    HashSet<String> optLinks = new HashSet<String>();
    Iterator<Linkage> lit = genome.getLinkageIterator();    
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      if (selectedLinks.contains(linkID)) {
        continue;
      }
      String src = link.getSource();
      String trg = link.getTarget();
      if (selectedNodes.contains(src) && selectedNodes.contains(trg)) {
        optLinks.add(linkID);
      }
    }

    //
    // Bundle it up:
    //
    
    ArrayList<SignedLink> gagLinks = new ArrayList<SignedLink>();
    slit = selectedLinks.iterator();
    while (slit.hasNext()) {
      String linkID = slit.next();
      Linkage link = genome.getLinkage(linkID);
      String src = link.getSource();
      String srcName = (uniques == null) ? genome.getNode(src).getName() : (String)uniques.get(src);
      String trg = link.getTarget();
      String trgName = (uniques == null) ? genome.getNode(trg).getName() : (String)uniques.get(trg);
      int sign = link.getSign();
      SignedLink slink = new SignedLink(srcName, trgName, sign);
      gagLinks.add(slink);
    }
    
    HashMap<String, String> nodeTypes = new HashMap<String, String>();
    Iterator<String> snit = selectedNodes.iterator();
    while (snit.hasNext()) {
      String nodeID = snit.next();
      String nodeName = (uniques == null) ? genome.getNode(nodeID).getName() : uniques.get(nodeID);
      Node node = genome.getNode(nodeID);
      String nodeType = DBNode.mapToTag(node.getNodeType());
      nodeTypes.put(nodeName, nodeType);
    }
    
    ArrayList<SignedLink> optGagLinks = new ArrayList<SignedLink>();
    Iterator<String> olit = optLinks.iterator();
    while (olit.hasNext()) {
      String linkID = olit.next();
      Linkage link = genome.getLinkage(linkID);
      String src = link.getSource();
      String srcName = (uniques == null) ? genome.getNode(src).getName() : uniques.get(src);
      String trg = link.getTarget();
      String trgName = (uniques == null) ? genome.getNode(trg).getName() : uniques.get(trg);
      int sign = link.getSign();
      SignedLink slink = new SignedLink(srcName, trgName, sign);
      optGagLinks.add(slink);
    }    

    SelectionSupport.NetworkForSpecies nfs = 
      new SelectionSupport.NetworkForSpecies((uniques != null), nodeTypes, gagLinks, optGagLinks); 
 
    return (nfs);
  }
  
  /***************************************************************************
  **
  ** Generate external selection event info
  */
  
  public ExternalSelectionChangeEvent selectionsForExternalEvents(DataAccessContext rcx) {
    
    //
    // Get links through selected segments:
    //

    HashSet<String> selectedLinks = new HashSet<String>();
    Iterator<String> sit = selectionKeys_.keySet().iterator();
    while (sit.hasNext()) {
      String key = sit.next();
      Intersection inter = selectionKeys_.get(key);
      String itemID = inter.getObjectID();
      Linkage lnk = rcx.getGenome().getLinkage(itemID);
      if (lnk == null) {
        continue;
      }
      LinkProperties lp = rcx.getLayout().getLinkProperties(itemID);
      LinkSegmentID[] segIDs = inter.segmentIDsFromIntersect();      
      if (segIDs == null) {
        List<String> allLinks = ((BusProperties)lp).getLinkageList();
        selectedLinks.addAll(allLinks);
      } else {      
        for (int i = 0; i < segIDs.length; i++) {
          Set<String> resolved = ((BusProperties)lp).resolveLinkagesThroughSegment(segIDs[i]);
          selectedLinks.addAll(resolved);
        }
      }
    }
    
    //
    // Selected nodes:
    //
    
    HashSet<String> selectedNodes = new HashSet<String>();
    sit = selectionKeys_.keySet().iterator();
    while (sit.hasNext()) {
      String key = sit.next();
      Intersection inter = selectionKeys_.get(key);
      String itemID = inter.getObjectID();
      Iterator<Node> git = rcx.getGenome().getAllNodeIterator();    
      while (git.hasNext()) {
        Node node = git.next();
        String nodeID = node.getID();
        if (itemID.equals(nodeID)) {
          selectedNodes.add(nodeID);
          break;
        }
      }
    }    

    //
    // Links too, but trim if not there:
    //
      
    HashSet<String> linkSelections = new HashSet<String>();
    Iterator<String> slit = selectedLinks.iterator();
    while (slit.hasNext()) {
      String linkID = slit.next();
      Linkage link = rcx.getGenome().getLinkage(linkID);
      if (link == null) {
        continue;
      }
      linkSelections.add(linkID);    
    }
 
    ExternalSelectionChangeEvent ce = new ExternalSelectionChangeEvent(rcx.getGenomeID(), selectedNodes, linkSelections);
    return (ce);
  }
}
