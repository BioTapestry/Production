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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.move.RunningMove;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.LocalLayoutSource;
import org.systemsbiology.biotapestry.db.Workspace;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.XPlatDisplayText;
import org.systemsbiology.biotapestry.ui.freerender.DrawTree;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.ui.modelobjectcache.CacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.CommonCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ConcreteGraphicsCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.JSONGroupVisitor;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawLayer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.VisitableObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.transformer.FontExport;
import org.systemsbiology.biotapestry.ui.modelobjectcache.transformer.FontManagerExport;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.Vector2D;

/***************************************************************************
** 
** More refactoring of original SUPanel class. We want to be able to
** export valid models that are NOT the current genome! So this allows
** alternate RenderingContexts to be used.
*/

public class ViewExporter {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private GenomePresentation myGenomePre_;
  private ZoomTargetSupport zts_;
  private BufferedImage bim_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Constructor
  */
  
  public ViewExporter(GenomePresentation pre, ZoomTargetSupport zts) {
    myGenomePre_ = pre;
    zts_ = zts;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Support printing
  */  
  
  public int print(Graphics g, PageFormat pf, int pageIndex, StateForDraw sfd) {
    if (pageIndex != 0) {
      return (Printable.NO_SUCH_PAGE);
    }
    
    if (sfd.rcx.oso == null) {
      sfd.rcx.oso = new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null);
    }
    
    String currentOverlay = sfd.rcx.oso.getCurrentOverlay();
    TaggedSet currentNetMods = sfd.rcx.oso.getCurrentNetModules();
    
    double px = pf.getImageableX();
    double py = pf.getImageableY();
    double pw = pf.getImageableWidth();
    double ph = pf.getImageableHeight();
    boolean doModules = (currentOverlay != null) && !currentNetMods.set.isEmpty();
    Map<String, Layout.OverlayKeySet> allKeys = (doModules) ? sfd.rcx.fgho.fullModuleKeysPerLayout() : null;
    Rectangle origRect = myGenomePre_.getRequiredSize(sfd.rcx, true, true, doModules, doModules,  
                                                      currentOverlay, currentNetMods, allKeys);    
   
    Point2D oldCenter = zts_.getRawCenterPoint();
    zts_.fixCenterPoint(true, null, false);    
        
    //
    // Figure out how to maintain aspect ratio:
    //
    double wFrac = origRect.width / pw;
    double hFrac = origRect.height / ph;
    double frac = 1.0 / ((hFrac > wFrac) ? hFrac : wFrac);
    Vector2D preTrans = new Vector2D(px  + (pw / 2.0), py + (ph / 2.0));
    Point2D center = zts_.getRawCenterPoint();
    Vector2D postTrans = new Vector2D(-center.getX(), -center.getY());
    OverrideTransform otr = new OverrideTransform(preTrans, frac, postTrans, true);
    //
    // We set the opaque overlay rectangle to equal the printable area at 300 dpi. NOTE
    // we are adding to StateForDrawObject!
    //
    int orx = 0;
    int ory = 0;
    int orw = (int)((pw / 72.0) * 300.0);
    int orh = (int)((ph / 72.0) * 300.0);        
    sfd.imgView = new Rectangle(orx, ory, orw, orh); 

    drawingGuts(g, false, false, otr, false, false, sfd);
    zts_.setRawCenterPoint(oldCenter, null, false);
    return (Printable.PAGE_EXISTS);
  }

  /***************************************************************************
  **
  ** Support image export
  */  
  
  public BoundsMaps exportToFile(File saveFile, boolean calcMap, 
                                 String format, ImageExporter.ResolutionSettings res,
                                 double zoom, Dimension size, StateForDraw sfd) throws IOException {
    
    return (exportGuts(saveFile, calcMap, format, res, zoom, size, sfd));
  }  
  
  /***************************************************************************
  **
  ** Support image export
  */  
  
  public BoundsMaps exportToStream(OutputStream stream, boolean calcMap, 
                                   String format, ImageExporter.ResolutionSettings res,
                                   double zoom, Dimension size, StateForDraw sfd) throws IOException {
    
    return (exportGuts(stream, calcMap, format, res, zoom, size, sfd));
  }   

  /***************************************************************************
  **
  ** Support JSON export
  */  
  
  public Map<String,Object> exportModelMap(boolean calcMap, double zoom, Dimension size, StateForDraw sfd) throws IOException {
    
    return exportMapGuts(calcMap,zoom, size, sfd);
  }   

  /***************************************************************************
  **
  ** Support image export.  Because of the way the image handler operates, it can take
  ** either an OutputStream or a File as the first argument:
  */  
  
  private BoundsMaps exportGuts(Object outObj, boolean calcMap, 
                                String format, ImageExporter.ResolutionSettings res,
                                double zoom, Dimension size, StateForDraw sfd) throws IOException {
    int width;
    int height;
    
    if (sfd.rcx.oso == null) {
      sfd.rcx.oso = new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null);
    }
    
    
    String currentOverlay = sfd.rcx.oso.getCurrentOverlay();
    TaggedSet currentNetMods = sfd.rcx.oso.getCurrentNetModules();
    if (size == null) {
      boolean doModules = (currentOverlay != null) && !currentNetMods.set.isEmpty();
      Map<String, Layout.OverlayKeySet> allKeys = (doModules) ? sfd.rcx.fgho.fullModuleKeysPerLayout() : null;
      Rectangle rect = myGenomePre_.getRequiredSize(sfd.rcx, true, true, doModules, doModules,
                                                    currentOverlay, currentNetMods, allKeys);                  
      width = (int)(rect.width * zoom); 
      height = (int)(rect.height * zoom);
    } else {
      width = size.width; 
      height = size.height;            
    }
    Point2D oldCenter = zts_.getRawCenterPoint();
    zts_.fixCenterPoint(true, null, false);
    
    BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = bi.createGraphics();
    g2.setColor(Color.white);
    g2.fillRect(0, 0, width, height);
    Vector2D preTrans = new Vector2D(width / 2.0, height / 2.0);
    Point2D center = zts_.getRawCenterPoint();
    Vector2D postTrans = new Vector2D(-center.getX(), -center.getY());
    OverrideTransform otr = new OverrideTransform(preTrans, zoom, postTrans, false);
    
    //
    // Here's the opaque overlay rectangle:
    // NOTE WE ARE ADDING TO THE StateForDraw object!
    
    sfd.imgView = new Rectangle(0, 0, width, height);  
    BoundsMaps retval = null;
    if (calcMap) {
      retval = drawingGuts(g2, true, true, otr, false, false, sfd);
      retval.convert(otr.buildTransform());
    } else {
      drawingGuts(g2, false, false, otr, false, false, sfd);
    }

    //
    // Fix for BT-10-27-09:1.  JPEG does not support RGBA.  Don't need to
    // export the A channel info anyway, so do a conversion first:
    //
    
    BufferedImage birgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2rgb = birgb.createGraphics();
    g2rgb.drawImage(bi, 0, 0, null);
        
    ImageExporter iex = new ImageExporter();
    iex.export(outObj, birgb, format, res);
    zts_.setRawCenterPoint(oldCenter, null, false);
    
    return (retval);
  }

  /***************************************************************************
   * Export the drawing guts as a Map of Objects keyed to strings.
   *
   *
   */  
  
  public Map<String, Object> exportMapGuts(boolean calcMap,double zoom, Dimension size, StateForDraw sfd) throws IOException {
    int width;
    int height;
    
    if (sfd.rcx.oso == null) {
      sfd.rcx.oso = new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null);
    }
    
    String currentOverlay = sfd.rcx.oso.getCurrentOverlay();
    TaggedSet currentNetMods = sfd.rcx.oso.getCurrentNetModules();
    Rectangle rect = null;
    if (size == null) {
      boolean doModules = (currentOverlay != null) && !currentNetMods.set.isEmpty();
      Map<String, Layout.OverlayKeySet> allKeys = (doModules) ? sfd.rcx.fgho.fullModuleKeysPerLayout() : null;
      rect = myGenomePre_.getRequiredSize(sfd.rcx, true, false, doModules, doModules,
                                          currentOverlay, currentNetMods, allKeys);                  
      width = (int)(rect.width * zoom); 
      height = (int)(rect.height * zoom);
    } else {
      width = size.width; 
      height = size.height;            
    }
    
    //
    // Note we are NOT shifting the workspace in this case (unlike image and print cases),  since
    // workspace needs to stay immutable during exports to the browser!
    //
    
    Vector2D preTrans = new Vector2D(width / 2.0, height / 2.0);
    Point2D center = zts_.getRawCenterPoint();
    Vector2D postTrans = new Vector2D(-center.getX(), -center.getY());
    OverrideTransform otr = new OverrideTransform(preTrans, zoom, postTrans, false);
    
    //
    // Here's the opaque overlay rectangle:
    //
    
    sfd.imgView = new Rectangle(0, 0, width, height);

    VisitableObjectCache moc = new VisitableObjectCache();
    VisitableObjectCache overlayCache = new VisitableObjectCache();
    VisitableObjectCache floaterCache = new VisitableObjectCache();    

    if (calcMap) {
      drawingGutsMap(moc, overlayCache, floaterCache, true, true, otr, false, false, sfd);
    } else {
      drawingGutsMap(moc, overlayCache, floaterCache, false, false, otr, false, false, sfd);
    }
    
    JSONGroupVisitor overlayVisitor = new JSONGroupVisitor();
    overlayCache.accept(overlayVisitor);
    
    // Export the center point coordinates
    HashMap<String, Object> model_info = new HashMap<String, Object>();

    model_info.put("center_x", center.getX());
    model_info.put("center_y", center.getY());
    model_info.put("model_w",rect.width);
    model_info.put("model_h",rect.height);
    model_info.put("model_x",rect.x);
    model_info.put("model_y",rect.y);
    model_info.put("modelID", sfd.rcx.getGenomeID());
       
    HashMap<DrawLayer, ArrayList<String>> groups_per_layers = new HashMap<DrawLayer, ArrayList<String>>();
    Map<String, Object> id_to_meta = new HashMap<String, Object>();	    
    
    HashMap<DrawLayer, ArrayList<Map<String, Object>>> draw_layer_content = new HashMap<DrawLayer, ArrayList<Map<String, Object>>>();
    HashMap<String, Object> draw_layer_groups = new HashMap<String, Object>();
    
    
    for (DrawLayer layer : DrawLayer.values()) {
	    JSONGroupVisitor layerVisitor = new JSONGroupVisitor();
    	moc.accept(layer, layerVisitor);
    	
    	layerVisitor.addObjectGraphToMap(draw_layer_groups, layer.toString());
    }
        
    for (DrawLayer layer : DrawLayer.values()) {
    	exportGroupsInDrawLayer(moc, layer, groups_per_layers, id_to_meta);    	
    }
    
    model_info.put("draw_layer_groups", draw_layer_groups);
    
    exportGroupsInDrawLayer(overlayCache, DrawLayer.OVERLAY, groups_per_layers, id_to_meta);
    
    overlayVisitor.addObjectGraphToMap(model_info, "overlay_data");
    
    // Export workspace bounds
    HashMap<String, Object> workspace_info = new HashMap<String, Object>();
    Rectangle workspaceRect = sfd.rcx.wSrc.getWorkspace().getWorkspace();

    workspace_info.put("x", workspaceRect.getX());
    workspace_info.put("y", workspaceRect.getY());
    workspace_info.put("w", workspaceRect.getWidth());
    workspace_info.put("h", workspaceRect.getHeight());
    
    model_info.put("workspace", workspace_info);   
    model_info.put("displayText", sfd.displayText);
    
    // Export the font mapping tables
    model_info.put("fonts", new FontManagerExport(sfd.fMgr));
    
    // Export source tooltips
    model_info.put("tooltips", this.buildTooltips(sfd));
    
    // Export user-defined default fonts
    ArrayList<FontExport> model_fonts = new ArrayList<FontExport>();
    int num_fonts = sfd.fMgr.getDefaultFonts().size();
    for (int font_type = 0; font_type < num_fonts; font_type++) {
    	model_fonts.add(new FontExport(sfd.fMgr.getFont(font_type)));
    }
    
    model_info.put("fonts", model_fonts);

    return(model_info);
  }

private void exportGroupsInDrawLayer(VisitableObjectCache sourceCache,
		DrawLayer layer, HashMap<DrawLayer, ArrayList<String>> targetMap,
		Map<String, Object> groupMetaDataPerLayer) {
	ArrayList<String> group_ids = new ArrayList<String>();
	
	Iterator<CacheGroup> layerGroupIter = sourceCache.getDrawLayerIterator(layer);
	while (layerGroupIter.hasNext()) {
		CacheGroup group = layerGroupIter.next();
		group_ids.add(group.getId());
		
		Map<String, Object> meta = new HashMap<String, Object>();
		meta.put("name", group.getName());
	
		groupMetaDataPerLayer.put(group.getId(), meta);
	}
	
	targetMap.put(layer, group_ids);
}

  /***************************************************************************
  **
  ** Drawing guts
  */
  
  public BoundsMaps drawingGuts(Graphics g, boolean getNoteBounds, boolean getNodeBounds, 
                                OverrideTransform ovrTra, boolean showBubbles, 
                                boolean doRect, StateForDraw sfd) {
    
    BoundsMaps retval = null;
    if (sfd.rcx.getGenome() != null) {
      Graphics2D g2 = (Graphics2D)g;   
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      
      //
      // Going to the screen, useTrans is null and we just composite standard zoomer settings.  Else
      // we use a transform particularly build for e.g. image export:
      //
      // Save transform for displays includes the viewport offset as a translation, but no zoom.
      // Save transform for images is the identity.
      // Save transform for printing includes the rotations and translations that the printer requires.
      // The zoomer sticks the zoom scaling on. 
      //
      AffineTransform saveTrans = g2.getTransform();
      AffineTransform useTrans = (ovrTra == null) ? null : ovrTra.buildTransform();     
      zts_.installTransform(g2, useTrans);
            
      if (doRect) {
        Workspace ws = sfd.rcx.wSrc.getWorkspace();
        Point2D origin = ws.getOrigin();
        Rectangle2D rect = new Rectangle2D.Double(origin.getX(), origin.getY(),
                                                  ws.getWidth(), ws.getHeight());
        g2.setPaint(Color.white);
        g2.fill(rect);      
        g2.setPaint(Color.black);
        g2.draw(rect);
      }
          
      Layout useLayout;
      if (sfd.dragLayout != null) {
        useLayout = sfd.dragLayout;
      } else if (sfd.multiMoveLayout != null) {
        useLayout = sfd.multiMoveLayout;
      } else {
        useLayout = sfd.rcx.getLayout();
      }

      Rectangle viewRect = null;
      GenomePresentation.OpaqueOverlayInfo ooi = null;
      Graphics2D ig2 = null;
      String currentOverlay = sfd.rcx.oso.getCurrentOverlay();
      if (currentOverlay != null) {
        NetOverlayProperties nop = useLayout.getNetOverlayProperties(currentOverlay);
        if (nop.getType() == NetOverlayProperties.OvrType.OPAQUE) {
          //
          // We render opaque overlays by superimposing an image with alpha over the standard
          // rendering.  When going to screen, the image is just the viewport.  When going to
          // an export, we are provided a rect that covers the whole model. 11/18/13: This 
          // sourcing decision is now done in the StateForDraw creation!
          // 
          viewRect = sfd.imgView;
          if ((bim_ == null) || (bim_.getHeight() != viewRect.height) || (bim_.getWidth() != viewRect.width)) {
            bim_ = new BufferedImage(viewRect.width, viewRect.height, BufferedImage.TYPE_INT_ARGB);
          }
          ig2 = bim_.createGraphics();
          ig2.setTransform(new AffineTransform());
          Color drawCol = new Color(1.0f, 1.0f, 1.0f, (float)sfd.rcx.oso.getCurrentOverlaySettings().backgroundOverlayAlpha); 
          ig2.setBackground(drawCol);
          ig2.clearRect(0, 0, viewRect.width, viewRect.height);
          ig2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          ig2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
          
          AffineTransform overTrans;
          if (ovrTra == null) {
            AffineTransform otp = new AffineTransform();
            // The zoomer transform is a combination of the zoom, plus needed offset when we
            // have zoomed out and no longer have scrollbars:
            overTrans = new AffineTransform(zts_.getTransform());
            // Need to offset the origin to handle view rectangle offset:
            otp.translate(-viewRect.getX(), -viewRect.getY());
            // This needs to be pre-concatenated to have it "done first"
            overTrans.preConcatenate(otp);
          } else {
            // With exports and printing, nothing the zoomer has is useful.  Since the overlay is the
            // same size as the image, with the same origin, we can just use the same transform!
            overTrans = ovrTra.buildTransform();
          }          
          ig2.setTransform(overTrans);
          
          // Laying the image into the standard g2 just involves offset by the viewport transform.  For
          // image exports, we lay it down directly (identity).  For print export, the saveTrans is
          // essential because that says how everything gets rotated and shifted to get it to the
          // printer correctly.
          AffineTransform ooit;
          if (ovrTra != null) {
            if (ovrTra.forPrinter) {
              ooit = saveTrans;
            } else {
              ooit = new AffineTransform();
            }
          } else {
            ooit = saveTrans;
          }
          //
          // The rendering context for the bim_ (ig2) has been set with a transform so rendering into the
          // image is done correctly (i.e. zoomed and translated so that the origin of the image matches
          // the origin of the viewport).  The ooit transform says how the image is to be laid into
          // the "standard" g2.
          //
          // Note that _printing_ has the complexity that the g2 we are handed to render to the print
          // job frequently has a rotation (to get it to landscape).  So the standard assumption of
          // no rotation that works for displays is NOT the case for printing!
          //         
          ooi = new GenomePresentation.OpaqueOverlayInfo(viewRect, bim_, ig2, ooit);
        }
      }
  
      Set<String> showModuleComponents = null;
      if (sfd.rcx.oso.showingModuleComponents()) {
        showModuleComponents = sfd.rcx.oso.getCurrentNetModules().set;
      } else {
        if (sfd.showModuleComps) {
          PanelCommands.MotionHandler handler = sfd.moHandler;
          RunningMove[] movs = handler.getMultiMov();
          for (int i = 0; i < movs.length; i++) {
            if (movs[i].type == RunningMove.MoveType.NET_MODULE_WHOLE) {    
              showModuleComponents = new HashSet<String>();
              showModuleComponents.add(movs[i].modIntersect.getObjectID());
              break;                     
            }
          }
        } else if ((sfd.rmov != null) && (sfd.rmov.type == RunningMove.MoveType.NET_MODULE_EDGE)) {
          showModuleComponents = new HashSet<String>();
          showModuleComponents.add(sfd.rmov.modIntersect.getObjectID());
        } else if (sfd.menuDrivenShowComponentModule != null) {
          showModuleComponents = new HashSet<String>();
          showModuleComponents.add(sfd.menuDrivenShowComponentModule);
        }
      }
          
      ConcreteGraphicsCache cgc = new ConcreteGraphicsCache();
      ConcreteGraphicsCache overlayCache = new ConcreteGraphicsCache();
      ConcreteGraphicsCache floaterCache = new ConcreteGraphicsCache();
      
      DataAccessContext rcxP = new DataAccessContext(sfd.rcx);
      rcxP.setLayout(useLayout);
      rcxP.lSrc = new LocalLayoutSource(useLayout, rcxP.getGenomeSource());
      rcxP.pixDiam = zts_.currentPixelDiameter();
      rcxP.showBubbles = showBubbles;
      myGenomePre_.presentGenomeWithOverlay(cgc, overlayCache, floaterCache, g2, ooi, rcxP,
          																	sfd.showRoot, showModuleComponents);
      
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.BACKGROUND_REGIONS);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.UNDERLAY);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.VFN_GHOSTED);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.VFN_UNUSED_REGIONS);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.MODEL_NODEGROUPS);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.FOREGROUND_REGIONS);
      			
			if (ooi == null) {
				// TODO fix this branch
	      overlayCache.renderAllGroupsInDrawLayer(g2, DrawLayer.OVERLAY);
				//cgc.renderAllGroupsInMajorLayer(g2, DrawLayer.MODEL_NODEGROUPS, DrawTree.MAJOR_SELECTED_LAYER_);
				g2.setComposite(AlphaComposite.SrcOver);
			}
			else {
	      overlayCache.renderAllGroupsInDrawLayer(ooi.overlayLayerG2, DrawLayer.OVERLAY);

	      // This is the 'burn through' part for the alpha compositing.
	      // overlayRender puts the relevant stuff to the MODEL_NODEGROUPS drawLayer.
	      ooi.overlayLayerG2.setComposite(AlphaComposite.Clear);
	      overlayCache.renderAllGroupsInDrawLayer(ooi.overlayLayerG2, DrawLayer.MODEL_NODEGROUPS);
	      // Render linkage
				cgc.renderAllGroupsInMajorLayer(ooi.overlayLayerG2, DrawLayer.MODEL_NODEGROUPS, DrawTree.MAJOR_SELECTED_LAYER_);
				
				ooi.overlayLayerG2.setComposite(AlphaComposite.SrcOver);
				// end burn through
				
	      ooi.renderImage(g2);
			}
			
			cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.MODELDATA);
      
      floaterCache.renderAllGroupsByLayers(g2);
      
      //
      // If we request intersection bounds, here they are:
      //      
     
      if ((getNoteBounds || getNodeBounds)) {
        DataAccessContext rcxQ = new DataAccessContext(sfd.rcx);
        rcxQ.setLayout(useLayout);
        retval = new BoundsMaps();
        if (getNoteBounds) {        
          retval.noteBounds = myGenomePre_.getNoteBounds(g2, rcxQ);
        }
        if (getNodeBounds) {
          retval.nodeBounds = myGenomePre_.getNodeBounds(g2, rcxQ);             
        }
      }
      
      g2.setTransform(saveTrans);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a tooltip for a link
  */
  
  public String buildTooltip(String linkID, StateForDraw sfd) {
  
    Genome genome = sfd.rcx.getGenome();
    Linkage link = genome.getLinkage(linkID);  // This could be ANY link through a bus segment
    if (link == null) {
      return (null);
    }
    Node srcNode = genome.getNode(link.getSource());
    String src = srcNode.getName();
    if ((src == null) || (src.trim().equals(""))) {
      src = sfd.rcx.rMan.getString("tip.noname");
    }
    if (genome instanceof GenomeInstance) {
      GenomeInstance gi = (GenomeInstance)genome;
      Group grp = gi.getGroupForNode(srcNode.getID(), GenomeInstance.ALWAYS_MAIN_GROUP);
      if (grp != null) {
        String grpName = grp.getInheritedTrueName(gi);
        src = src + " [" + grpName + "]";
      }  
    }
    String format = sfd.rcx.rMan.getString("tip.source");
    String desc = MessageFormat.format(format, new Object[] {src});
    return (desc);
  }

  /***************************************************************************
  **
  ** Get a tooltip for a link
  */
  
  public Map<String, String> buildTooltips(StateForDraw sfd) {
  
    HashMap<String, String> retval = new HashMap<String, String>();
    Genome genome = sfd.rcx.getGenome();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String srcID = link.getSource();
      if (retval.containsKey(srcID)) {
        continue;
      }
      retval.put(srcID, buildTooltip(link.getID(), sfd));
    }
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  /**
   * 
   * 
   * 
   * 
   * 
   * @param moc
   * @param getNoteBounds
   * @param getNodeBounds
   * @param ovrTra
   * @param showBubbles
   * @param doRect
   * @param imgView
   */
  
  private void drawingGutsMap(ModelObjectCache moc, ModelObjectCache overlayCache, ModelObjectCache floaterCache,
          boolean getNoteBounds, boolean getNodeBounds, OverrideTransform ovrTra,
          boolean showBubbles, boolean doRect, StateForDraw sfd) {

    Layout useLayout;
    if (sfd.dragLayout != null) {
      useLayout = sfd.dragLayout;
    } else if (sfd.multiMoveLayout != null) {
      useLayout = sfd.multiMoveLayout;
    } else {
      useLayout = sfd.rcx.getLayout();
    }  	
 
    GenomePresentation.OpaqueOverlayInfo ooi = null;
    
    Set<String> showModuleComponents = null;
    if (sfd.rcx.oso.showingModuleComponents()) {
      showModuleComponents = sfd.rcx.oso.getCurrentNetModules().set;
    } else {
      if (sfd.showModuleComps) {
        PanelCommands.MotionHandler handler = sfd.moHandler;
        RunningMove[] movs = handler.getMultiMov();
        for (int i = 0; i < movs.length; i++) {
          if (movs[i].type == RunningMove.MoveType.NET_MODULE_WHOLE) {    
            showModuleComponents = new HashSet<String>();
            showModuleComponents.add(movs[i].modIntersect.getObjectID());
            break;                     
          }
        }
      } else if ((sfd.rmov != null) && (sfd.rmov.type == RunningMove.MoveType.NET_MODULE_EDGE)) {
        showModuleComponents = new HashSet<String>();
        showModuleComponents.add(sfd.rmov.modIntersect.getObjectID());
      } else if (sfd.menuDrivenShowComponentModule != null) {
        showModuleComponents = new HashSet<String>();
        showModuleComponents.add(sfd.menuDrivenShowComponentModule);
      }
    }
   
    DataAccessContext rcxQ = new DataAccessContext(sfd.rcx);
    rcxQ.setLayout(useLayout);
    rcxQ.pixDiam = zts_.currentPixelDiameter();
    rcxQ.showBubbles = showBubbles;
    myGenomePre_.presentGenomeWithOverlay(moc, overlayCache, floaterCache, null, ooi, rcxQ, 
                                          sfd.showRoot, showModuleComponents);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  /***************************************************************************
  **
  ** Used to provide state for a drawing operation
  */     
    
  public static class StateForDraw {
    
    DataAccessContext rcx;
    boolean showRoot;
    boolean showModuleComps;
    PanelCommands.MotionHandler moHandler;
    RunningMove rmov;
    Layout dragLayout;
    Layout multiMoveLayout;
    Rectangle imgView;
    String menuDrivenShowComponentModule;
    XPlatDisplayText displayText;
    FontManager fMgr;
   
    public StateForDraw(BTState appState, DataAccessContext rcx, RunningMove rmov,
                        String menuDrivenShowComponentModule,  
                        Layout dragLayout, Layout multiMoveLayout, Rectangle imgView, 
                        XPlatDisplayText displayText, FontManager fMgr) {
 
      this.rcx = rcx;
      showRoot = (appState != null) ? !appState.getPanelCmds().getCurrentHandler(!appState.getIsEditor()).isPullDownHandler() : false;    
      showModuleComps  = (appState != null) ? appState.getPanelCmds().showModuleComponentsForMode() : false;
      moHandler = (showModuleComps && (appState != null)) ? (PanelCommands.MotionHandler)appState.getPanelCmds().getCurrentHandler(false) : null; 
      this.rmov = rmov;
      this.dragLayout = dragLayout;
      this.multiMoveLayout = multiMoveLayout;
      this.menuDrivenShowComponentModule = menuDrivenShowComponentModule;
      this.displayText = displayText; 
      this.fMgr = fMgr;
      this.imgView = imgView;
    }
  }
   
  /***************************************************************************
  **
  ** Used to return intersection bounds for image exports
  */     
    
  public static class BoundsMaps {
    public Map<String, Rectangle> noteBounds;
    public Map<String, Rectangle> nodeBounds;
       
    void convert(AffineTransform expTrans) {
      if (noteBounds != null) convertMap(noteBounds, expTrans);
      if (nodeBounds != null) convertMap(nodeBounds, expTrans);
      return;
    }
 
    private void convertMap(Map<String, Rectangle> boundsMap, AffineTransform expTrans) {
      Iterator<Rectangle> rit = boundsMap.values().iterator();
      while (rit.hasNext()) {
        Rectangle brect = rit.next();
        Point2D originPoint = new Point2D.Double(brect.x, brect.y);
        expTrans.transform(originPoint, originPoint);
        Point2D extentPoint = new Point2D.Double(brect.x + brect.width, brect.y + brect.height);
        expTrans.transform(extentPoint, extentPoint);
        brect.width = (int)(Math.round(extentPoint.getX() - originPoint.getX()));
        brect.height = (int)(Math.round(extentPoint.getY() - originPoint.getY()));
        brect.x = (int)(Math.round(originPoint.getX()));
        brect.y = (int)(Math.round(originPoint.getY()));
      }
      return;
    }
  }   
  
  /***************************************************************************
  **
  ** Used to override display transform for drawing
  */     
    
  public static class OverrideTransform {
    Vector2D preTrans;
    double zoom;
    Vector2D postTrans;
    boolean forPrinter;
       
    public OverrideTransform(Vector2D preTrans, double zoom, Vector2D postTrans, boolean forPrinter) {
      this.preTrans = preTrans;
      this.zoom = zoom;
      this.postTrans = postTrans;
      this.forPrinter = forPrinter;
    }
 
    public AffineTransform buildTransform() {
      AffineTransform retval = new AffineTransform();
      retval.translate(preTrans.getX(), preTrans.getY());
      retval.scale(zoom, zoom);
      retval.translate(postTrans.getX(), postTrans.getY());
      return (retval);
    }
  }  
}
