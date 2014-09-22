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

package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.freerender.GroupFree;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ConcreteGraphicsCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawLayer;
import org.systemsbiology.biotapestry.util.Bounds;
 
/****************************************************************************
**
** This draws an overview of the model
*/

public class OverviewPresentation {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private Rectangle2D floater_;
  private Rectangle cachedBounds_;
  private BufferedImage bi_;
  private int width_;
  private int height_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** constructor
  */

  public OverviewPresentation() {
    floater_ = null;
    cachedBounds_ = null;
    bi_ = null;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Present the given genome with the given layout
  */
  
  public void presentOverview(Graphics2D g2, DataAccessContext rcx) { //String genomeKey, String layoutKey) {

    Genome genome = rcx.getGenome();
    if (!(genome instanceof GenomeInstance)) {
      throw new IllegalArgumentException();
    }
    GenomeInstance gi = ((GenomeInstance)genome);
    GenomeInstance parentvfg = gi.getVfgParentRoot();
    if (parentvfg != null) {
      throw new IllegalArgumentException();
    }
    renderBackground(rcx);
    g2.drawImage(bi_, 0, 0, null);
    
    ConcreteGraphicsCache cgc = new ConcreteGraphicsCache();
    
    cgc.setDrawLayer(DrawLayer.FOREGROUND_REGIONS);
    renderGroups(cgc, rcx);
    cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.FOREGROUND_REGIONS);    
    
    renderFloater(g2);
    return;
  }

  /***************************************************************************
  **
  ** Return the required size of the layout
  */
  
  public Rectangle getRequiredSize(DataAccessContext rcx) {
                                   
    if (cachedBounds_ != null) {
      return (cachedBounds_);
    }
                                     
    //
    // Crank through the genes and nodes and have the renderer return the
    // bounds.
    //

    if (rcx == null) {
      return (new Rectangle(0, 0, 0, 0));
    }
    
    Rectangle retval = null;
    Genome genome = rcx.getGenome();
    genome = Layout.determineLayoutTarget(genome);
    boolean haveInstance = (genome instanceof GenomeInstance);
    boolean haveNode = false;

    Iterator<Gene> git = genome.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      NodeProperties np = rcx.getLayout().getNodeProperties(gene.getID());
      INodeRenderer rend = np.getRenderer();
      Rectangle bounds = rend.getBounds(gene, rcx, null);
      if (retval == null) {
        retval = bounds;
      } else {
        Bounds.tweakBounds(retval, bounds);
      }
      haveNode = true;
    }
    
    
    //
    // Crank through the nodes and draw them up
    //
    Iterator<Node> nit = genome.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      NodeProperties np = rcx.getLayout().getNodeProperties(node.getID());
      INodeRenderer rend = np.getRenderer();
      Rectangle bounds = rend.getBounds(node, rcx, null);
      if (retval == null) {
        retval = bounds;
      } else {
        Bounds.tweakBounds(retval, bounds);
      }      
      haveNode = true;
    }
    
    //
    // If there are no nodes or genes, use group lables
    //
    

    if ((!haveNode) && haveInstance) {
      Iterator<Group> grit = ((GenomeInstance)genome).getGroupIterator();
      while (grit.hasNext()) {
        Group gp = grit.next();
        GroupProperties grpr = rcx.getLayout().getGroupProperties(gp.getID());
        Point2D labelLoc = grpr.getLabelLocation();
        if (retval == null) {
          retval = new Rectangle((int)labelLoc.getX() - 50, (int)labelLoc.getY() - 50, 100, 100);
        } else {
          Bounds.tweakBoundsWithPoint(retval, labelLoc);
        }
      }
    }    
    
    //
    // Pad the outside
    
    Bounds.padBounds(retval, 100, 100);
    cachedBounds_ = retval;
    
    return (retval);
  }

  /***************************************************************************
  **
  ** Set the floater position
  */
  
  public void setFloater(Rectangle2D floater) {
    floater_ = (Rectangle2D)floater.clone();
    return;
  }
  
  /***************************************************************************
  **
  ** Set the floater position
  */
  
  public void setSize(int width, int height) {
    if ((width_ != width) || (height_ != height)) {
      bi_ = null;
      cachedBounds_ = null;
    }
    width_ = width;
    height_ = height;    
    return;
  }  
  
  /***************************************************************************
  **
  ** Set the zoom
  */
  
  public void setTransform(AffineTransform transform) {

    return;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  
   /***************************************************************************
  **
  ** Guts of presentation
  */
  
  private void renderBackground(DataAccessContext rcx) { //GenomeInstance genome, Layout layout) {
    if (bi_ == null) {
      bi_ = new BufferedImage(width_, height_, BufferedImage.TYPE_INT_RGB);
    }    
    Graphics2D g2 = bi_.createGraphics();
    g2.setColor(Color.white);
    g2.fillRect(0, 0, width_, height_);
    
    ConcreteGraphicsCache cgc = new ConcreteGraphicsCache();
    
    cgc.setDrawLayer(DrawLayer.BACKGROUND_REGIONS);
    renderGroups(cgc, rcx);
    cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.BACKGROUND_REGIONS);
    
    return;
  }  

  /***************************************************************************
  **
  ** Guts of presentation
  */
  
  private void renderGroups(ModelObjectCache moc, DataAccessContext rcx) { //GenomeInstance genome, Layout layout) { 
    
    //
    // Note how we are tied to database and the other usual suspects here:
    //
     
    List<String> order = rcx.getLayout().getGroupDrawingOrder();
    Iterator<Group> grit = ((GenomeInstance)rcx.getGenome()).getGroupIteratorFromList(order);

    
    while (grit.hasNext()) {
      Group group = grit.next();
      String groupRef = group.getID();
      GroupProperties gp = rcx.getLayout().getGroupProperties(groupRef);
      GroupFree renderer = gp.getRenderer();
      if (gp.getLayer() != 0) {
        continue;
      }
      renderer.render(moc, group, null, rcx, null);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Render the floater
  */
  
  private void renderFloater(Graphics2D g2) {
    if (floater_ != null) {
      g2.setPaint(Color.BLACK);
      g2.draw(floater_);
    }
    return;
  }  
}
