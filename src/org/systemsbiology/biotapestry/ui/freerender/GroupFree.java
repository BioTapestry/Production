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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.db.ColorGenerator;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.nav.GroupSettings;
import org.systemsbiology.biotapestry.ui.AnnotatedFont;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.IRenderer;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.modelobjectcache.BoundShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.GroupFreeCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForDesktop;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** This renders a group. 11/20/13: No longer extends ItemRenderBase, since it
** requires extra render arguments
*/

public class GroupFree {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final double TEXT_PAD_ = 10.0;
  // Font bounds gives fixed height that is ~ 33% taller than upper case,
  // so hack it to get it centered.
  private static final double HEIGHT_HACK_ = 0.75;
  private static final int SUBREGION_BORDER_ = 3;  
  private static final int EMPTY_REGION_PAD_ = 100;    
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor.
  */

  public GroupFree() {
    super();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Render the group
  */
  
  public void render(ModelObjectCache cache, GenomeItem item, Intersection selected, 
                     StaticDataAccessContext rcx, IRenderer.Mode mode, Object miscInfo) {
  	
    String groupRef = item.getID();
    
    GroupProperties gp = rcx.getCurrentLayout().getGroupProperties(Group.getBaseID(groupRef));
    if (gp.doNotRender()) {
      return;
    }
    
    AugmentedExtraInfo augExtra = (AugmentedExtraInfo)miscInfo;
    
    // The childId is needed only in the web application, and is read from the cache group and
    // packaged for export in JSONGroupVisitor. It is not used in the desktop editor or viewer,
    // so it can be passed in as null.
    String childId;
    if (augExtra == null) {
    	childId = null;
    }
    else {
    	childId = augExtra.childId_;
    }
    
    GroupFreeCacheGroup group = new GroupFreeCacheGroup(item, gp.getOrder(), gp.getLayer(), childId);    
    
  	if (rcx.isForWeb()) {
  			renderGuts(group, item, selected, rcx, miscInfo, mode, false);
  			renderGuts(group, item, selected, rcx, miscInfo, mode, true);  			
  	}
  	else {
  			renderGuts(group, item, selected, rcx, miscInfo, mode, false);
  	}
  	
    cache.addGroup(group);  	
  }
  
  public void renderGuts(GroupFreeCacheGroup group, GenomeItem item, Intersection selected, 
                         StaticDataAccessContext rcx, Object miscInfo, IRenderer.Mode mode, Boolean drawLightOverride) {
  	ModalTextShapeFactory textFactory = null;
  	
  	if (rcx.isForWeb()) {
  		textFactory = new ModalTextShapeFactoryForWeb(rcx.getFrc());
  	}
  	else {
  		textFactory = new ModalTextShapeFactoryForDesktop();
  	}
  	
  	Integer minorLayer = 0;
  	Integer majorLayer = 0;
    Genome genome = rcx.getCurrentGenome();  	
  	
    //
    // Currently, while doing path display, we are not rendering the groups!
    //
    
    String groupRef = item.getID();
    GroupProperties gp = rcx.getCurrentLayout().getGroupProperties(Group.getBaseID(groupRef));
    if (gp.doNotRender()) {
      return;
    }
      
    GroupSettings.Setting groupViz = GroupSettings.Setting.NONE;
    boolean genomeIsRoot = true;
    AugmentedExtraInfo augEx = null;
    ExtraInfo extra = null;
    GroupSettings.Setting setting = null;
    List<ColoredRect> passOut = null;
    if (miscInfo != null) {
      augEx = (AugmentedExtraInfo)miscInfo;
      passOut = augEx.passOut;
      extra = augEx.extra;
      if (extra != null) {
        setting = extra.vizVal;
        genomeIsRoot = extra.genomeIsRoot;
      }
    }
    if (setting != null) {
      groupViz = setting;
    } else {      
      groupViz = rcx.getGSM().getGroupVisibility(genome.getID(), groupRef, rcx);
    }
   
    int style = gp.getStyle();
    int layer = gp.getLayer();
    int tpad = gp.getPadding(GroupProperties.TOP);
    int bpad = gp.getPadding(GroupProperties.BOTTOM);
    int lpad = gp.getPadding(GroupProperties.LEFT);
    int rpad = gp.getPadding(GroupProperties.RIGHT);    
    boolean doBold = (layer == 0);

    Color col = gp.getColor(groupViz == GroupSettings.Setting.ACTIVE, rcx.getColorResolver());
    Color vlg = new Color(240, 240, 240);
    if ((extra != null) && (extra.replacementColor != null)) {
      col = (groupViz != GroupSettings.Setting.ACTIVE) ? extra.replacementColor : vlg;
    }

    boolean isGhosted = rcx.isGhosted();
    Color paintColor = (isGhosted) ? vlg : col;
    
    if (mode != IRenderer.Mode.NORMAL) {
      paintColor = (new ColorGenerator()).modulateColorSaturation(0.25, paintColor);
    }
     
    boolean drawLight = ((groupViz != GroupSettings.Setting.ACTIVE) || isGhosted) || drawLightOverride;
    
    // GroupFreeCacheGroup group = new GroupFreeCacheGroup(item, gp.getOrder(), gp.getLayer());    

    //
    // Figure out what name to use:
    //

    NameAndBoldAndEmpty nab = getName(genome, item, extra, doBold, rcx.getRMan());
    doBold = nab.doBold;
    String name = nab.name;
    
    //
    // Figure out name text rectangle:
    //
    
    TextRect textRectInfo = getTextRect(gp, doBold, name, rcx.getFrc(), rcx.getFontManager());

    if (style == GroupProperties.AUTOBOUND) {
      //
      // Get the bounds around all the nodes in the group, and draw up that
      // rectangle.
      //
      
      List<Rectangle> drawRects = getRenderRects(item, layer, textRectInfo, rcx, tpad, bpad, lpad, rpad);
      int drNum = drawRects.size();
      for (int i = 0; i < drNum; i++) {
        Rectangle drawRect = drawRects.get(i);
        // g2.fillRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
        
        ModelObjectCache.Rectangle rect = new ModelObjectCache.Rectangle(drawRect.x, drawRect.y, drawRect.width, drawRect.height,
        		DrawMode.FILL, paintColor, new BasicStroke());
        group.addShapeWithToggle(rect, majorLayer, minorLayer, drawLightOverride);
        
        if ((passOut != null) && layer == 0) {
          passOut.add(new ColoredRect(drawRect, paintColor));
        }
      }
      
      //
      // If the group is collapsed, center the label:
      //

      if ((name != null) && (!name.trim().equals("")) && drawLight) {
        double width = textRectInfo.tbounds.getWidth();
        double height = textRectInfo.tbounds.getHeight() * HEIGHT_HACK_;
        textRectInfo.nameX = textRectInfo.extent.x + (textRectInfo.extent.width / 2) - width / 2.0;
        textRectInfo.nameY = textRectInfo.extent.y + (textRectInfo.extent.height / 2) + height / 2.0;        
        double ty = textRectInfo.extent.y + (textRectInfo.extent.height / 2) - height / 2.0;
    
        textRectInfo.rect = new Rectangle2D.Double(textRectInfo.nameX - TEXT_PAD_, 
                                                   ty - TEXT_PAD_,
                                                   width + (TEXT_PAD_ * 2.0),
                                                   height + (TEXT_PAD_ * 2.0));        
      } 
      DisplayOptions dop = rcx.getDisplayOptsSource().getDisplayOptions();
      
      if ((name != null) && !name.trim().equals("") && (!calcHideLabel(genomeIsRoot, gp) || nab.isEmpty)) {
        Color blackCol = (drawLight) ? dop.getInactiveGray() : Color.black;
        Color drawCol = (drawLight) ? vlg : col;
        
        if (!doBold) {
          ModelObjectCache.Rectangle borderRect = new ModelObjectCache.Rectangle(
          		textRectInfo.rect.getX() - SUBREGION_BORDER_,
              textRectInfo.rect.getY() - SUBREGION_BORDER_,
              textRectInfo.rect.getWidth() + (2 * SUBREGION_BORDER_),
              textRectInfo.rect.getHeight() + (2 * SUBREGION_BORDER_),
          		DrawMode.FILL, blackCol, new BasicStroke());
          
          group.addShape(borderRect, majorLayer, minorLayer);
        }
        
        // g2.setPaint((doBold) ? blackCol : drawCol);                 
        // g2.fill(textRectInfo.rect);    
        // g2.setPaint((doBold) ? drawCol : blackCol);
        // g2.setFont((doBold) ? textRectInfo.bFont : textRectInfo.mFont);
        // g2.drawString(name, (float)textRectInfo.nameX, (float)textRectInfo.nameY);
        
        Color textRectCol = (doBold) ? blackCol : drawCol;
        // TODO fix stroke
        ModelObjectCache.Rectangle textRect = new ModelObjectCache.Rectangle(
        		textRectInfo.rect.getX() - SUBREGION_BORDER_,
            textRectInfo.rect.getY() - SUBREGION_BORDER_,
            textRectInfo.rect.getWidth() + (2 * SUBREGION_BORDER_),
            textRectInfo.rect.getHeight() + (2 * SUBREGION_BORDER_),
        		DrawMode.FILL, textRectCol, new BasicStroke());
        
        group.addShapeWithToggle(textRect, majorLayer, minorLayer, drawLightOverride);
                
        Color textCol = (doBold) ? drawCol : blackCol;
        Font font = (doBold) ? textRectInfo.bFont : textRectInfo.mFont;
        
        ModalShape ts = textFactory.buildTextShape(name, new AnnotatedFont(font, false), textCol, (float)textRectInfo.nameX, (float)textRectInfo.nameY, new AffineTransform());
        group.addShapeWithToggle(ts, majorLayer, minorLayer, drawLightOverride);
      }
    }
    
    setGroupBounds(group, item, rcx, miscInfo);
    
    return;  	
  }

  /***************************************************************************
  **
  ** Get the text rectangle
  */
  
  public Rectangle getLabelBounds(GenomeItem item, StaticDataAccessContext irx) { 
    //
    // Figure out what name to use:
    //
    String groupRef = item.getID();
    GroupProperties gp = irx.getCurrentLayout().getGroupProperties(Group.getBaseID(groupRef)); 
    int layer = gp.getLayer();
    boolean doBold = (layer == 0);
    
    NameAndBoldAndEmpty nab = getName(irx.getCurrentGenome(), item, null, doBold, irx.getRMan());
    doBold = nab.doBold;
    
    TextRect tr = getTextRect(gp, doBold, nab.name, irx.getFrc(), irx.getFontManager()); 
    return (tr.textRect);
  }
  
  /***************************************************************************
  **
  ** Get the text rectangle
  */
  
  private TextRect getTextRect(GroupProperties gp, boolean doBold, String name, 
                                FontRenderContext frc, FontManager fmgr) {
    
    TextRect retval = new TextRect();
    retval.mFont = fmgr.getFont(FontManager.MED_LARGE);
    retval.bFont = fmgr.getFont(FontManager.LARGE);     
    
    retval.tbounds = null;
    retval.rect = null;
    retval.textRect = null; 
    retval.extent = null;

    retval.nameX = 0.0;
    retval.nameY = 0.0;
 
    if ((name != null) && (!name.trim().equals(""))) {
      retval.tbounds = ((doBold)? retval.bFont : retval.mFont).getStringBounds(name, frc);
      double width = retval.tbounds.getWidth();
      double height = retval.tbounds.getHeight() * HEIGHT_HACK_;
      Point2D origin = gp.getLabelLocation();    
      retval.nameX = origin.getX() - width / 2.0;
      retval.nameY = origin.getY() + height / 2.0;
      double ty = origin.getY() - height / 2.0;
    
      retval.rect = new Rectangle2D.Double(retval.nameX - TEXT_PAD_, 
                                           ty - TEXT_PAD_,
                                           width + (TEXT_PAD_ * 2.0),
                                           height + (TEXT_PAD_ * 2.0));
      
      retval.textRect = new Rectangle((int)retval.rect.getX(), (int)retval.rect.getY(), 
                                      (int)retval.rect.getWidth(), (int)retval.rect.getHeight());

    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the name
  */
  
  private NameAndBoldAndEmpty getName(Genome genome, GenomeItem item, ExtraInfo extra, boolean doBold, ResourceManager rmgr) {
 
    //
    // Figure out what name to use:
    //
    
    String name = ((Group)item).getInheritedDisplayName((GenomeInstance)genome);
    // Toss out unnamed groups
    if ((name != null) && 
        name.equals(rmgr.getString("groupName.noName"))) {
      name = null; 
    }
    boolean isEmpty = (((Group)item).getMemberCount() == 0);
    // Gotta have something to click on to delete these guys even with no name!
    if (isEmpty) {
      if ((name == null) || (name.trim().equals(""))) {
        name = rmgr.getString("groupName.empty");
      } 
    }    
       
    // Note that these days, active subgroups are sent down as the primary group to
    // render, so the extra.replacementName is not displayed through the former method.
    
    if (extra != null) {
      if (extra.dropName) {
        doBold = true;
      } else if (extra.replacementName != null) {
        name = null;  //(drawLight) ? extra.replacementName : null;
      }
    }  
    
    return (new NameAndBoldAndEmpty(name, doBold, isEmpty));
  }
    
  /***************************************************************************
  **
  ** Get rectangles to render
  */
  
  private List<Rectangle> getRenderRects(GenomeItem item, int layer,                               
                                         TextRect textRectInfo, DataAccessContext rcx,
                                         int tpad, int bpad, int lpad, int rpad) {   
     
    ArrayList<Rectangle> retval = new ArrayList<Rectangle>();                               
    textRectInfo.extent = textRectInfo.textRect;
    boolean haveMember = false;
    Iterator<GroupMember> mit = ((Group)item).getMemberIterator();
    while (mit.hasNext()) {
      GroupMember member = mit.next();
      String nodeID = member.getID();
      Node node = rcx.getCurrentGenome().getNode(nodeID);
      // Useful for partial layouts:
      NodeProperties np = rcx.getCurrentLayout().getNodeProperties(nodeID);
      if (np == null) {
        continue;
      }
      haveMember = true;
      INodeRenderer rend = np.getRenderer();
      Rectangle bounds = rend.getBounds(node, rcx, null);
      if (((layer >= 1) && (layer <= GroupProperties.MAX_SUBGROUP_LAYER)) && (node.getNodeType() == Node.GENE)) {
        Bounds.padBounds(bounds, tpad, bpad, lpad, rpad);
        int fillX = bounds.x;
        int fillW = bounds.width;
        String nodeName = node.getName();
        int nnLen = nodeName.length();
        if (nnLen > 12) {
          int overrun = nnLen - 12;
          Rectangle2D subWidth = textRectInfo.mFont.getStringBounds(nodeName.substring(0, overrun), rcx.getFrc());
          Rectangle2D allWidth = textRectInfo.mFont.getStringBounds(nodeName, rcx.getFrc());
          double delta = allWidth.getWidth() - subWidth.getWidth();
          fillX = fillX + (int)delta;
          fillW = fillW - (int)delta;
        }
        retval.add(new Rectangle(fillX, bounds.y, fillW, bounds.height));
      } else {
        if (textRectInfo.extent == null) {
          textRectInfo.extent = bounds;
        } else {
          Bounds.tweakBounds(textRectInfo.extent, bounds);
        }
      }
    }
    
    //
    // Handle contained modules:
    //
    
    Rectangle modBounds = moduleBoxBounds(item, rcx);
    if (modBounds != null) {
      if (textRectInfo.extent == null) {
        textRectInfo.extent = modBounds;
      } else {
        Bounds.tweakBounds(textRectInfo.extent, modBounds);
      } 
    } 
    
    if (layer == 0) {
      //
      // May have nothing to draw if we are a ghosted parent group with an 
      // active subset, and there are no members
      //
      if (textRectInfo.extent != null) {
        subLabelBoxBounds(item, rcx, textRectInfo.extent);        
        Bounds.padBounds(textRectInfo.extent, tpad, bpad, lpad, rpad);
        if (!haveMember) {
          Bounds.padBounds(textRectInfo.extent, EMPTY_REGION_PAD_, 0, 0, 0);
        }
        retval.add(new Rectangle(textRectInfo.extent.x, textRectInfo.extent.y, 
                                 textRectInfo.extent.width, textRectInfo.extent.height));
      }
    }
    return (retval);
  }  

 /***************************************************************************
  **
  ** Figure out if we are hiding the label
  */
  
  private boolean calcHideLabel(boolean genomeIsRoot, GroupProperties gp) {
    int labelMode = gp.getHideLabelMode();
    boolean hideLabel = ((labelMode == GroupProperties.HIDE_LABEL_ALL_LEVELS) || 
                         ((labelMode == GroupProperties.HIDE_LABEL_TOP_LEVEL_ONLY) && genomeIsRoot)); 
    return (hideLabel);
  }  
  
  
  /***************************************************************************
  **
  ** Render the group to a placement grid
  */
  
  public void renderToPlacementGrid(GenomeItem item, DataAccessContext rcx, LinkPlacementGrid grid) {                                    
    String groupRef = item.getID();                                      
    GroupProperties gp = rcx.getCurrentLayout().getGroupProperties(Group.getBaseID(groupRef));
    if (gp.doNotRender()) {
      return;
    }   
    int layer = gp.getLayer();
    int style = gp.getStyle();
    int tpad = gp.getPadding(GroupProperties.TOP);
    int bpad = gp.getPadding(GroupProperties.BOTTOM);
    int lpad = gp.getPadding(GroupProperties.LEFT);
    int rpad = gp.getPadding(GroupProperties.RIGHT);    
    boolean doBold = (layer == 0);
    
    //
    // Figure out what name to use:
    //

    NameAndBoldAndEmpty nab = getName(rcx.getCurrentGenome(), item, null, doBold, rcx.getRMan());
    String name = nab.name;
    
    //
    // Figure out name text rectangle:
    //
    
    TextRect textRectInfo = getTextRect(gp, doBold, name, rcx.getFrc(), rcx.getFontManager());

    if (style == GroupProperties.AUTOBOUND) {
      //
      // Get the bounds around all the nodes in the group, and draw up that
      // rectangle.
      //
      
      List<Rectangle> drawRects = getRenderRects(item, layer, textRectInfo, rcx, tpad, bpad, lpad, rpad);
      int drNum = drawRects.size();
      for (int i = 0; i < drNum; i++) {
        Rectangle drawRect = drawRects.get(i);
        grid.addGroup(drawRect, groupRef);
      }                                      
                                      
    }
    return;
  }  

  /***************************************************************************
  **
  ** Answer if we intersect the group
  */
  
  public Intersection intersects(GenomeItem item, Point2D pt, StaticDataAccessContext itx, Object miscInfo) {
                       
    String groupRef = item.getID();
    GroupProperties gp = itx.getCurrentLayout().getGroupProperties(Group.getBaseID(groupRef));
    int style = gp.getStyle();
    int x = (int)pt.getX();  // FIX ME: Round, not trunc
    int y = (int)pt.getY();
    Genome genome = itx.getCurrentGenome();    
    
    ExtraInfo exi = (miscInfo != null) ? ((AugmentedExtraInfo)miscInfo).extra : null;
    
    if (style == GroupProperties.AUTOBOUND) {
      //
      // Get the bounds around all the nodes in the group, and figure out if it
      // intersects
      //
           
      HashSet<Rectangle> bounds = boxBounds(item, itx, false, false);
      Iterator<Rectangle> bit = bounds.iterator();
      while (bit.hasNext()) {
        Rectangle rect = bit.next();
        if (Bounds.intersects(rect, x, y)) {
          boolean hasLabel = intersectsLabel(item, itx, pt, exi);
          if (hasLabel) {
            return (new Intersection(item.getID(), new Intersection.LabelSubID(item.getID()), 0.0));
          }
          Set<String> subsets = ((Group)item).getSubsets((GenomeInstance)genome);
          Iterator<String> sidit = subsets.iterator();
          while (sidit.hasNext()) {
            String subid = sidit.next();
            Group subgroup = ((GenomeInstance)genome).getGroup(subid);
            hasLabel = intersectsLabel(subgroup, itx, pt, exi);
            if (hasLabel) {
              return (new Intersection(item.getID(), new Intersection.LabelSubID(subid), 0.0));
            }
          }
          return (new Intersection(item.getID(), new Intersection.LabelSubID(null), 0.0));
        }
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Check for intersection of label:
  */
  
  private boolean intersectsLabel(GenomeItem item, StaticDataAccessContext itx, Point2D pt, ExtraInfo extra) {

    String groupRef = item.getID();
    GroupProperties gp = itx.getCurrentLayout().getGroupProperties(Group.getBaseID(groupRef));
    String name = item.getName();
    boolean hideName = calcHideLabel(extra.genomeIsRoot, gp);
    
    if ((name == null) || (name.trim().equals("")) || hideName) {
      return (false);
    }

    // Duplicate logic used in rendering:
    boolean doBold = false;
    if (extra != null) {
      if (extra.dropName) {
        doBold = true;
      } else if (extra.replacementName != null) {
        return (false);
      }
    }
    
    TextRect textRect = getTextRect(gp, doBold, name, itx.getFrc(), itx.getFontManager());    
    Rectangle2D useMe = textRect.rect;
    return (Bounds.intersects(useMe.getMinX(), useMe.getMinY(), 
                              useMe.getMaxX(), useMe.getMaxY(), pt.getX(), pt.getY()));
    
  }
  
  /***************************************************************************
  **
  ** Check for intersection of rectangle: FIX ME!
  */

  public Intersection intersects(Genome genome, GenomeItem item, 
                                 Layout layout, FontRenderContext frc, Rectangle rect, boolean countPartial, Object miscInfo) {
    return (null);                                
  }
  
  /***************************************************************************
  **
  ** Return the Rectangle used by this group
  */
  
  public Rectangle getBounds(GenomeItem item, StaticDataAccessContext itx, Object miscInfo, boolean skipModules) {
                               
    HashSet<Rectangle> bounds = boxBounds(item, itx, true, skipModules);
    if (bounds.isEmpty()) {
      return (null);
    }
    Iterator<Rectangle> bit = bounds.iterator();
    return (bit.next());
  }

  /***************************************************************************
  **
  ** Sets the bound shapes for the CacheGroup, to be exported in the
  ** web application.
  */
  public void setGroupBounds(BoundShapeContainer group, GenomeItem item, StaticDataAccessContext rcx, Object miscInfo) {
    ArrayList<ModelObjectCache.ModalShape> bounds = new ArrayList<ModelObjectCache.ModalShape>();

    fillBoundsArray(bounds, item, rcx, miscInfo);
    for (ModelObjectCache.ModalShape ms : bounds) {
      group.addBoundsShape(ms);
    }
  }
  
  /***************************************************************************
  **
  ** Fills an array with bound shapes that will be exported in the model map
  ** in the web application.
  */
  void fillBoundsArray(ArrayList<ModelObjectCache.ModalShape> targetArray, GenomeItem item, 
                       StaticDataAccessContext rcx, Object miscInfo) {

	  Rectangle bounds = getBounds(item, rcx, miscInfo, false);
    targetArray.add(ModalShapeFactory.buildBoundRectangleForGlyph(bounds));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Figure out bounding rectangles
  */
  
  private HashSet<Rectangle> boxBounds(GenomeItem item, StaticDataAccessContext itx, boolean onlyOne, boolean skipModules) {

    HashSet<Rectangle> retval = new HashSet<Rectangle>();
    String groupRef = item.getID();
    GroupProperties gp = itx.getCurrentLayout().getGroupProperties(Group.getBaseID(groupRef));
    int style = gp.getStyle();
    int layer = gp.getLayer();
    int tpad = gp.getPadding(GroupProperties.TOP);
    int bpad = gp.getPadding(GroupProperties.BOTTOM);
    int lpad = gp.getPadding(GroupProperties.LEFT);
    int rpad = gp.getPadding(GroupProperties.RIGHT);
    
    String name = item.getName();
    Rectangle extent = null;
    TextRect tRect = null;
    
    if (gp.getHideLabelMode() == GroupProperties.HIDE_LABEL_ALL_LEVELS) {
      name = null;
    }
    
    // Gotta have something to click on to delete these guys!
    if ((((Group)item).getMemberCount() == 0) && ((name == null) || (name.trim().equals("")))) {
      name = itx.getRMan().getString("groupName.empty");
    }
    
    if ((name != null) && (!name.trim().equals(""))) {
      tRect = getTextRect(gp, true, name, itx.getFrc(), itx.getFontManager());    
    }    
    
    if (style == GroupProperties.AUTOBOUND) {
      //
      // Get the bounds around all the nodes in the group, and figure out if it
      // intersects
      //                       
      if (tRect != null) {
        extent = tRect.textRect;
      }
      boolean haveMember = false;
      Iterator<GroupMember> mit = ((Group)item).getMemberIterator();
      while (mit.hasNext()) {
        GroupMember member = mit.next();
        String nodeID = member.getID();
        Node node = itx.getCurrentGenome().getNode(nodeID);
        NodeProperties np = itx.getCurrentLayout().getNodeProperties(nodeID);
        if (np == null) { // useful for partial layouts...
          continue;
        }
        haveMember = true;
        INodeRenderer rend = np.getRenderer(); 
        Rectangle bounds = rend.getBounds(node, itx, null);
        if (((layer >= 1) && (layer <= GroupProperties.MAX_SUBGROUP_LAYER)) && (!onlyOne)) {
          Bounds.padBounds(bounds, tpad, bpad, lpad, rpad);
          retval.add(bounds);
        } else {
          if (extent == null) {
            extent = bounds;
          } else {
            Bounds.tweakBounds(extent, bounds);
          }
        }
      }
      
      //
      // Handle contained modules. 8/7/14: Added inability to skip modules, since we frequently need to calculate group bounds
      // while module geometry is in a state of flux, and modules attached to the group are undefined.
      //
      
      if (!skipModules) {
        Rectangle modBounds = moduleBoxBounds(item, itx);
        if (modBounds != null) {
          if (extent == null) {
            extent = modBounds;
          } else {
            Bounds.tweakBounds(extent, modBounds);
          }        
        }
      }
      
      //
      // Make sure we bound the subset labels too!
      //
      subLabelBoxBounds(item, itx, extent);
 
      if (((layer == 0) || onlyOne) && (extent != null)) {
        Bounds.padBounds(extent, tpad, bpad, lpad, rpad);
        if (!haveMember) {
          Bounds.padBounds(extent, EMPTY_REGION_PAD_, 0, 0, 0);
        }
        retval.add(extent);
      }  
    }
    return (retval);
  }

  
  /***************************************************************************
  **
  ** Tweak bounds to account for attached modules
  */
  
  private Rectangle moduleBoxBounds(GenomeItem item, DataAccessContext itx) {   
  
 
    Rectangle extent = null;
    // FIX ME Pretty expensive to do here.  Better once for each paint!
    HashSet<NetModule.FullModuleKey> attached = new HashSet<NetModule.FullModuleKey>();
    itx.getFGHO().getModulesAttachedToGroup(((GenomeInstance)itx.getCurrentGenome()), item.getID(), attached); 
    
    Iterator<NetModule.FullModuleKey> ait = attached.iterator();
    while (ait.hasNext()) {
      NetModule.FullModuleKey fmk = ait.next();
      NetOverlayProperties nop = itx.getCurrentLayout().getNetOverlayProperties(fmk.ovrKey);
      NetModuleProperties nmp = nop.getNetModuleProperties(fmk.modKey);
      Rectangle sob = nmp.shapeOnlyBounds();
      if (sob == null) {
        continue;
      }
      if (extent == null) {
        extent = sob;
      } else {
        Bounds.tweakBounds(extent, sob);
      }
    }
    return (extent);
  }

  /***************************************************************************
  **
  ** Tweak bounds to account for sub labels
  */
  
  private void subLabelBoxBounds(GenomeItem item, DataAccessContext itx, Rectangle currBounds) { 
    //
    // Make sure we bound the subset labels too!
    //
    Group group = (Group)item;
    GenomeInstance gi = (GenomeInstance)itx.getCurrentGenome();
    Set<String> subsets = group.getSubsets(gi);
    Iterator<String> sidit = subsets.iterator();
    while (sidit.hasNext()) {
      String subid = sidit.next();
      GroupProperties gps = itx.getCurrentLayout().getGroupProperties(Group.getBaseID(subid));
      if (gps == null) {
        //FIX ME!  Why would subLabelBoxBounds be null?
        return;
      }
      Group sub = gi.getGroup(subid);
      String name = sub.getName();
      boolean hideLabel = (gps.getHideLabelMode() == GroupProperties.HIDE_LABEL_ALL_LEVELS);
      if ((name != null) && !hideLabel) {
        TextRect tRect = getTextRect(gps, true, name, itx.getFrc(), itx.getFontManager());
        Bounds.tweakBounds(currBounds, tRect.textRect);
      }
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
  ** Used to hold name and boldness
  */
  
  private class NameAndBoldAndEmpty {
    String name;
    boolean doBold;
    boolean isEmpty;
    
    NameAndBoldAndEmpty(String name, boolean doBold, boolean isEmpty) {
      this.name = name;
      this.doBold = doBold;
      this.isEmpty = isEmpty;
    }
  } 
  
  /***************************************************************************
  **
  ** Used to hold text rectangle results
  */
  
  private class TextRect {
    double nameX;
    double nameY;
    Font mFont;
    Font bFont;  
    Rectangle2D tbounds;
    Rectangle2D rect;
    Rectangle textRect;
    Rectangle extent;
  } 
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Used to pass in additional rendering info AND pass out rendered info
  */
  
  public static class AugmentedExtraInfo {
    public ExtraInfo extra;
    public List<ColoredRect> passOut;
    public String childId_;
    
    public AugmentedExtraInfo() { 
    }
  }  
 
  /***************************************************************************
  **
  ** Used to pass in additional rendering info
  */
  
  public static class ExtraInfo {
    public boolean genomeIsRoot;
    public GroupSettings.Setting vizVal;
    public boolean dropName;
    public String replacementName;
    public Color replacementColor;

    public ExtraInfo() { 
    }
  }
  
  /***************************************************************************
  **
  ** Used to pass out additional rendering info
  */
  
  public static class ColoredRect {
    public Rectangle rect;
    public Color drawColor;
    
    public ColoredRect(Rectangle rect, Color drawColor) {
      this.rect = rect;
      this.drawColor = drawColor;
    }
  }  
}
