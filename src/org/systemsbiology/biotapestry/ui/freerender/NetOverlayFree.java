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

package org.systemsbiology.biotapestry.ui.freerender;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.ui.IRenderer;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.ui.modelobjectcache.NetModuleCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.NetModuleLinkageCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.NetModuleLinkageExportForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.NetOverlayCacheGroup;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** This renders a net module
*/

public class NetOverlayFree {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Basic constructor
  */

  public NetOverlayFree() {
    super();
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Render the overlay
  */
  
  public void render(ModelObjectCache cache, NetworkOverlay netOvr, List<GroupFree.ColoredRect> renderedRects, 
                     boolean doUnderlay, Set<String> showComponents, StaticDataAccessContext rcx, IRenderer.Mode mode) {
  	
  	Integer minorLayer = 0;
  	Integer majorLayer = 0;
  	
    TaggedSet currentNetMods = rcx.getOSO().getCurrentNetModules();
    Set<String> currMods = (currentNetMods == null) ? null : currentNetMods.set;
    NetModuleFree.CurrentSettings settings = rcx.getOSO().getCurrentOverlaySettings();
    
    String ovrID = netOvr.getID();
    NetOverlayProperties nop = rcx.getCurrentLayout().getNetOverlayProperties(ovrID);
    NetOverlayProperties.OvrType nopType = nop.getType();
    if (((nopType == NetOverlayProperties.OvrType.UNDERLAY) && !doUnderlay) ||
        ((nopType != NetOverlayProperties.OvrType.UNDERLAY) && doUnderlay)) {
      return;
    }
    boolean isOpq = (nopType == NetOverlayProperties.OvrType.OPAQUE); 
    
    NetOverlayCacheGroup group = new NetOverlayCacheGroup(netOvr, nopType);
    
    //
    // If we have region rects, we draw those with SRC compositing (replaces dest regardless
    // of alpha channel):
    //

    if (isOpq && (renderedRects != null)) {
      // g2.setComposite(AlphaComposite.Src);
      ModelObjectCache.SetComposite sc = new ModelObjectCache.SetComposite(AlphaComposite.Src);
      group.addShape(sc, majorLayer, minorLayer);
      
      float[] coVals = new float[4];      
      Iterator<GroupFree.ColoredRect> rrit = renderedRects.iterator();
      while (rrit.hasNext()) {          
        GroupFree.ColoredRect cr = rrit.next();
        cr.drawColor.getComponents(coVals);
        Color aDrawCol = new Color(coVals[0], coVals[1], coVals[2], (float)settings.backgroundOverlayAlpha); 
        
        // TODO check stroke
        ModelObjectCache.Rectangle rect = new ModelObjectCache.Rectangle(cr.rect.getX(), cr.rect.getY(),
        		cr.rect.getWidth(), cr.rect.getHeight(), DrawMode.FILL, aDrawCol, new BasicStroke());
        
        group.addShape(rect, majorLayer, minorLayer);
      }
      
      // g2.setComposite(AlphaComposite.SrcOver);
      sc = new ModelObjectCache.SetComposite(AlphaComposite.SrcOver);
      group.addShape(sc, majorLayer, minorLayer);
    }
    
    //
    // Now draw the mods:
    //
    
    //
    // We use the rootInstance to drive rendering, so that non-included nodes are not
    // shown as part of the module (important for dynamic instances!)
    //
    
    Genome useGenome = Layout.determineLayoutTarget(rcx.getCurrentGenome());    
    NetOverlayOwner noo = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(rcx.getCurrentGenomeID());
    DynamicInstanceProxy dip = (noo.overlayModeForOwner() == NetworkOverlay.DYNAMIC_PROXY) ? (DynamicInstanceProxy)noo 
                                                                                           : null;    
    Set<String> useGroups = noo.getGroupsForOverlayRendering();
 
    TaggedSet revealed = rcx.getOSO().getRevealedModules();
    Iterator<NetModule> nmit = netOvr.getModuleIterator();
    while (nmit.hasNext()) {
      NetModule nm = nmit.next();
      String modID = nm.getID();
      if (!currMods.contains(modID) && !rcx.isForWeb()) {
        continue;
      }
      boolean showContents = ((revealed != null) && revealed.set.contains(modID));
      NetModuleProperties nmp = nop.getNetModuleProperties(modID);
      NetModuleFree nmf = nmp.getRenderer();
      boolean showingComponents = ((showComponents != null) && showComponents.contains(modID));
      StaticDataAccessContext rcxr = new StaticDataAccessContext(rcx, useGenome, rcx.getCurrentLayout());
      
      // In the desktop application, the shapes for rendering the NetModules can go directly to the NetOverlay cache group
      // and be therefore drawn by the ConcreteGraphicsCache.
      if (!rcx.isForWeb()) {
    	  nmf.render(group, dip, useGroups, ovrID, nm, settings, showingComponents, showContents, true, true, true, rcxr);
      }
      // In the web application, add each NetModule as a sub-group to the NetOverlay so that the hierarchy will be preserved
      // when the model is exported.
      else {
	      NetModuleCacheGroup nmf_group_filled = new NetModuleCacheGroup(nm);
	      nmf.render(nmf_group_filled, dip, useGroups, ovrID, nm, settings, showingComponents, showContents, false, true, false, rcxr);
	      
	      NetModuleCacheGroup nmf_group_edges = new NetModuleCacheGroup(nm);
	      nmf.render(nmf_group_edges, dip, useGroups, ovrID, nm, settings, showingComponents, showContents, true, false, false, rcxr);
	     
	      NetModuleCacheGroup nmf_group_label = new NetModuleCacheGroup(nm);
	      nmf.render(nmf_group_label, dip, useGroups, ovrID, nm, settings, showingComponents, showContents, false, false, true, rcxr);
	      
	      group.addNetModule(nmf_group_edges, nmf_group_filled, nmf_group_label, nmp);
	      
	      nmf.setGroupBounds(nmf_group_edges, dip, useGroups, nm, ovrID, rcxr);
	      nmf.setGroupBounds(nmf_group_filled, dip, useGroups, nm, ovrID, rcxr);
      }
    }
    
    //
    // Now draw the mod links TREEs:
    //
    
    Iterator<String> nlit = nop.getNetModuleLinkagePropertiesKeys();
    NetModuleLinkageFree nmlf = new NetModuleLinkageFree();
    
    if (!rcx.isForWeb()) {
	    while (nlit.hasNext()) {
	    	String treeID = nlit.next();
	    	nmlf.render(group, ovrID, treeID, rcx, mode);     
	    }
    }
    else {
	    while (nlit.hasNext()) {
	    	String treeID = nlit.next();  
	    	NetModuleLinkageExportForWeb lefw = new NetModuleLinkageExportForWeb();	        
	      NetModuleLinkageCacheGroup lcg = new NetModuleLinkageCacheGroup(treeID, treeID, "net_module_linkage", lefw, 0, 0, "null");
	    	nmlf.renderForWeb(lcg, ovrID, treeID, lefw, rcx, mode);
	    	group.addNetModuleLinkage(lcg);
	    }
	}
 
    cache.addGroup(group);
    
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
}
