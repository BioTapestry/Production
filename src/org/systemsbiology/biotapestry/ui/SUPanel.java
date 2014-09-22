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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;
import javax.swing.undo.UndoManager;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.PopCommands;
import org.systemsbiology.biotapestry.cmd.flow.move.Mover;
import org.systemsbiology.biotapestry.cmd.flow.move.RunningMove;
import org.systemsbiology.biotapestry.cmd.undo.SelectionChangeCmd;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.embedded.ExternalSelectionChangeEvent;
import org.systemsbiology.biotapestry.event.EventManager;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.GeneralChangeListener;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.LayoutChangeListener;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeListener;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.Note;
import org.systemsbiology.biotapestry.nav.ZoomTarget;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenu;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/***************************************************************************
** 
** This is the network display panel wrapper. No longer a JPanel.
*/

public class SUPanel implements LayoutChangeListener,
                                ModelChangeListener,
                                GeneralChangeListener, 
                                Printable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private BTState appState_;
  
  //
  // Null when headless:
  //
  
  private MyPaintPanel myPanel_;
  private JScrollPane myScrollPane_;
  
  private Point lastPress_ = null;
  private Point lastView_ = null;
  private Point lastAbs_ = null;
  
  private boolean dragSelect_ = false;
  private boolean lastShifted_ = false;
  private boolean lastCtrl_ = false;
  
  private Layout dragLayout_ = null;
  private Layout multiMoveLayout_ = null;    
  private ArrayList<Point> dragFloater_;
  private RunningMove rmov_;
  private GenomePresentation myGenomePre_;

  private String menuDrivenShowComponentModule_ = null;
  private boolean pushedShowBubbles_ = false;
  private boolean showBubbles_ = false;

  private PopControl popCtrl_;
  private MouseMotionHandler motionHandle_;

  private JMenu currSelMenu_;
  private ViewExporter vexp_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Constructor
  */
  
  public SUPanel(BTState appState) {
    myPanel_ = (appState.isHeadless()) ? null : new MyPaintPanel();
    appState_ = appState.setSUPanel(this);
    myGenomePre_ = new GenomePresentation(appState_, true, .38, true, appState_.getRenderingContextForZTS());
    ZoomTargetSupport zts = new ZoomTargetSupport(appState_, myGenomePre_, myPanel_, appState_.getRenderingContextForZTS());
    appState_.setZoomTarget(zts);
    vexp_ = new ViewExporter(myGenomePre_, zts);
    
    
    // Saw freezeups with tooltips on Windows, but have not been
    // able to reproduce, so reenable for now. (BT-03-23-05:3)
    //String jVer = System.getProperty("java.version");
    //String jVend = System.getProperty("java.vendor");
    //String os = System.getProperty("os.name");
    //System.out.println("ver = " + jVer + " vend = " + jVend + " os = " + os);
    if (myPanel_ != null) {
      ToolTipManager.sharedInstance().registerComponent(myPanel_);
      ToolTipManager.sharedInstance().setDismissDelay(240000);
    }
    DataAccessContext dacx = new DataAccessContext(appState_);
    new PopCommands(appState_);
    // This gets built even when headless:
    popCtrl_ = new PopControl(appState_, dacx); 
    
    if (myPanel_ != null) {
      myPanel_.addMouseListener(new MouseHandler());
      motionHandle_ = new MouseMotionHandler();
      myPanel_.addMouseMotionListener(motionHandle_);
    }
    
    EventManager em = appState_.getEventMgr();
    em.addLayoutChangeListener(this);
    em.addGeneralChangeListener(this);  
    em.addModelChangeListener(this);
    
    if (myPanel_ != null) {
      myPanel_.setBackground(new Color(240, 240, 240));
    }
    new TextBoxManager(appState_);

    dragFloater_ = new ArrayList<Point>();
    new CursorManager(appState_);
    new PanelCommands(appState_);
    
  //  currentNetMods_ = new TaggedSet();
  //  revealed_ = new TaggedSet();
    
    ResourceManager rMan = appState_.getRMan();
    currSelMenu_ = new JMenu(rMan.getString("command.selectedItemMenu"));
    currSelMenu_.setMnemonic(rMan.getChar("command.selectedItemMenuMnem"));
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Model drawing
  */
   
  public void drawModel(boolean revalidate) {
    if (myPanel_ != null) {
      if (revalidate) {
        myPanel_.revalidate();
      }
      myPanel_.repaint();
    }
    return;
  }  
 
  /***************************************************************************
  **
  ** Request focus for model. No-op for headless
  */
   
  public void requestFocusForModel() {
    if (myPanel_ != null) {
      myPanel_.requestFocus();
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Request focus for model for window. No-op for headless
  */
   
  public void requestFocusForModelInWindow() {
    if (myPanel_ != null) {
      myPanel_.requestFocusInWindow();
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Show popup menu. No-op for headless
  */
   
  public void showPopup(JPopupMenu popMenu, int x, int y) {
    if (myPanel_ != null) {
      popMenu.show(myPanel_, x, y);
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Get panel inside a scroll pane. Null for headless
  */
   
  public JScrollPane getPanelInPane() {
    if (myPanel_ == null) {
      return (null);
    }
    myScrollPane_ = new JScrollPane(myPanel_);
    return (myScrollPane_);
  }  

  /***************************************************************************
  **
  ** Get panel. Null for headless. Use only for Cursor manager
  */
   
  public JPanel getPanel() {
    return (myPanel_);
  }  
  
  /***************************************************************************
  ** 
  ** Get the selections
  */

  public Map<String, Intersection> getSelections() { 
    return (myGenomePre_.getSelections());
  }
  
  /***************************************************************************
  ** 
  ** Get the selections divided into sets
  */

  public boolean getDividedSelections(Genome genome, Layout layout, Set<String> genes, Set<String> nodes, Set<String> links) { 
    Map<String, Intersection> intersects = myGenomePre_.getSelections();
    boolean retval = false;
    Iterator<String>  vit = intersects.keySet().iterator();
    while (vit.hasNext()) {
      String objID = vit.next();
      Gene gene = genome.getGene(objID);
      if (gene != null) {
        genes.add(objID);
        retval = true;
        continue;
      }
      Node node = genome.getNode(objID);
      if (node != null) {
        nodes.add(objID);
        retval = true;
        continue;
      }
     
      Linkage link = genome.getLinkage(objID);
      if (link != null) {
        Intersection lint = intersects.get(objID);
        Set<String> resolved = layout.resolveLinksFromIntersection(lint);  
        links.addAll(resolved); 
        retval = true;
        continue;
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Get Multi move layout
  */

  public Layout getMultiMoveLayout() { 
    return (multiMoveLayout_);
  }
  
  /***************************************************************************
  ** 
  ** Set Multi move layout
  */

  public void setMultiMoveLayout(Layout nuMulti) { 
    multiMoveLayout_ = nuMulti;
    return;
  }
   
  /***************************************************************************
  ** 
  ** Bump to next selection
  */

  public void incrementToNextSelection() {
    DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
    myGenomePre_.bumpNextSelection(rcx);
    return;
  }
   
  /***************************************************************************
  ** 
  ** Bump to previous selection
  */

  public void decrementToPreviousSelection() {
    DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
    myGenomePre_.bumpPreviousSelection(rcx);
    return;
  }  
  
  /***************************************************************************
  **
  ** Get menu for selected item
  */
  
  public JMenu getSelectedMenu() {
    return (currSelMenu_);
  }
  
  /***************************************************************************
  **
  ** Generate XPlatMenu for selected item
  */
  
  public XPlatMenu getSelectedXPlatMenu(DataAccessContext dacx) {
    
    if (hasASelection() && (dacx.getGenomeID() != null)) {
      Map<String, Intersection> selmap = myGenomePre_.getSelections();
      if (selmap.size() == 1) {
        Intersection intersect = selmap.values().iterator().next();
        return (popCtrl_.generateMenu(intersect, dacx));
      } else {
        return (popCtrl_.generateMultiSelMenu(dacx));
      }
    } else {
      return (popCtrl_.generateMenu(null, dacx));
    }
  }
   
  /***************************************************************************
  **
  ** Stock the current selection menu
  */
  
  public void stockSelectedMenu(DataAccessContext dacx) {
    if (hasASelection() && (dacx.getGenomeID() != null)) {
      Map<String, Intersection> selmap = myGenomePre_.getSelections();
      if (selmap.size() == 1) {
        Intersection intersect = selmap.values().iterator().next();
        boolean canUse = popCtrl_.stockMenu(currSelMenu_, intersect, dacx);
        currSelMenu_.setEnabled(canUse);
      } else {
        boolean canUse = popCtrl_.stockMultiSelMenu(currSelMenu_, selmap, dacx);
        currSelMenu_.setEnabled(canUse);
      }
    } else {
      currSelMenu_.setEnabled(false);
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Answer if links are not currently visible
  */
  
  public boolean linksAreHidden(DataAccessContext rcx) {
    return (myGenomePre_.linksAreHidden(rcx));
  }
    
  /***************************************************************************
  **
  ** Display error information
  */
  
  public void giveErrorFeedback() {
    appState_.getCursorMgr().signalError();
    Toolkit.getDefaultToolkit().beep();
    return;
  }
  
  /***************************************************************************
  **
  ** Get an external selection event filled in
  */
  
  public ExternalSelectionChangeEvent getExternalSelectionEvent() {
    DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
    return (myGenomePre_.selectionsForExternalEvents(rcx));
  }
  
  /***************************************************************************
  **
  ** Get the basic model bounds (may not be current keys!)
  */
  
  public Rectangle getBasicBounds(DataAccessContext rcx, boolean doComplete, boolean doBuffer, int moduleHandling) {  
    if (moduleHandling == ZoomTarget.VISIBLE_MODULES) {
      throw new IllegalArgumentException();
    }
    
    boolean doModules = false;
    Map<String, Layout.OverlayKeySet> allKeys = null;
    if (moduleHandling == ZoomTarget.ALL_MODULES) {
      doModules = true;
      allKeys = rcx.fgho.fullModuleKeysPerLayout();
    }
    
    return (myGenomePre_.getRequiredSize(rcx, doComplete, doBuffer, doModules, doModules, null, null, allKeys));  
  } 
  
  /***************************************************************************
  **
  ** Select everybody.  If we are currently only showing nodes (overlay
  ** hiding links), we only select nodes:
  */
  
  public void selectAll(UndoManager undom, DataAccessContext rcx) {

    String currentOverlay = rcx.oso.getCurrentOverlay();
    if (currentOverlay != null) {
      NetOverlayProperties nop = rcx.getLayout().getNetOverlayProperties(currentOverlay);
      if (nop.hideLinks()) {
        myGenomePre_.selectAllNodes(rcx, undom);
        return;
      }
    }
    myGenomePre_.selectAll(rcx, undom);
    return;
  }
  
  /***************************************************************************
  **
  ** Select nobody
  */
  
  public void selectNone(UndoManager undom, DataAccessContext rcx) {
    myGenomePre_.selectNone(rcx, undom);
    return;
  }  

  /***************************************************************************
  **
  ** Drop nodes from selections
  */
  
  public void dropNodeSelections(Integer type, UndoManager undom, DataAccessContext rcx) {
    myGenomePre_.dropNodeSelections(rcx, type, undom);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop links from selections
  */
  
  public SelectionChangeCmd.Bundle dropLinkSelections(boolean recordBundle, UndoManager undom, DataAccessContext rcx) {
    SelectionChangeCmd.Bundle bundle = (recordBundle) ? new SelectionChangeCmd.Bundle() : null;
    myGenomePre_.dropLinkSelections(rcx, bundle, undom);
    return (bundle);
  }  

  /***************************************************************************
  **
  ** Set the state to show link target bubbles.
  */
  
  public void toggleTargetBubbles() {
    showBubbles_ = !showBubbles_;
    if (appState_.getPanelCmds().isInShowBubbleMode()) {
      pushedShowBubbles_ = showBubbles_;
    }
    return;
  }   
 
  /***************************************************************************
  **
  ** Force to show module components
  */
  
  public void forceShowingModuleComponents(String moduleID) {  
    menuDrivenShowComponentModule_ = moduleID;
    drawModel(false);
    return;
  }         
    
  /***************************************************************************
  **
  ** Answer if we have a selection
  */
  
  public boolean hasASelection() {
    return (myGenomePre_.hasASelection());
  }  
  
  /***************************************************************************
  **
  ** Set the set of selected node IDs
  */
  
  public Set<String> getSelectedNodes() {
    Database db = appState_.getDB();
    Genome genome = db.getGenome(appState_.getGenome()); 
    Map<String, Intersection> selmap = myGenomePre_.getSelections();
 
    HashSet<String> retval = new HashSet<String>();   
    Iterator<String> selKeys = selmap.keySet().iterator();
    while (selKeys.hasNext()) {
      String key = selKeys.next();
      Node node = genome.getNode(key);
      if (node != null) {
        retval.add(key);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Show bubbles
  */
   
  public void pushBubblesAndShow() {
    pushedShowBubbles_ = showBubbles_;
    showBubbles_ = true;
    return;
  }   
  
  /***************************************************************************
  **
  ** Restore bubbles
  */
  
  public void popBubbles() {
    showBubbles_ = pushedShowBubbles_;
    return;
  } 
  
  /***************************************************************************
  **
  ** Support printing
  */  
  
  public int print(Graphics g, PageFormat pf, int pageIndex) {
    if (pageIndex != 0) {
      return (NO_SUCH_PAGE);
    }
    
    DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
    ViewExporter.StateForDraw sfd = new ViewExporter.StateForDraw(appState_, rcx, rmov_, 
                                                                  menuDrivenShowComponentModule_, 
                                                                  dragLayout_, multiMoveLayout_, 
                                                                  null, null, appState_.getFontMgr());

    return (vexp_.print(g, pf, pageIndex, sfd));
  }

  /***************************************************************************
  **
  ** Support image export
  */  
  
  public ViewExporter.BoundsMaps exportToFile(File saveFile, boolean calcMap, 
                                              String format, ImageExporter.ResolutionSettings res,
                                              double zoom, Dimension size, OverlayStateOracle oso) throws IOException {

    DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
    rcx.oso = oso;
    ViewExporter.StateForDraw sfd = new ViewExporter.StateForDraw(appState_, rcx, rmov_, 
                                                                  menuDrivenShowComponentModule_, 
                                                                  dragLayout_, multiMoveLayout_, 
                                                                  null, null, appState_.getFontMgr());
    
    
    
    return (vexp_.exportToFile(saveFile, calcMap, format, res, zoom, size, sfd));
  }  
  
  /***************************************************************************
  **
  ** Support image export
  */  
  
  public ViewExporter.BoundsMaps exportToStream(OutputStream stream, boolean calcMap, 
                                                String format, ImageExporter.ResolutionSettings res,
                                                double zoom, Dimension size, OverlayStateOracle oso) throws IOException {
    DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
    rcx.oso = oso;
    ViewExporter.StateForDraw sfd = new ViewExporter.StateForDraw(appState_, rcx, rmov_, 
                                                                  menuDrivenShowComponentModule_, 
                                                                  dragLayout_, multiMoveLayout_, 
                                                                  null, null, appState_.getFontMgr());
    return (vexp_.exportToStream(stream, calcMap, format, res, zoom, size, sfd));
  }   

  /***************************************************************************
  **
  ** Support JSON export
  */  
  
  public Map<String,Object> exportModelMap(boolean calcMap, double zoom, Dimension size, OverlayStateOracle oso) throws IOException {
    
    DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
    rcx.oso = oso;
    ViewExporter.StateForDraw sfd = new ViewExporter.StateForDraw(appState_, rcx, rmov_, 
                                                                  menuDrivenShowComponentModule_, 
                                                                  dragLayout_, multiMoveLayout_, 
                                                                  null, rcx.getGenomeSource().getTextFromGenomeKey(rcx.getGenomeID()), 
                                                                  appState_.getFontMgr());
    return (vexp_.exportMapGuts(calcMap, zoom, size, sfd));
  }    
  
  /***************************************************************************
  **
  ** Called when model has changed
  */   

  public void modelHasChanged(ModelChangeEvent event, int remaining) {
    modelHasChanged(event);
    return;
  }    

  /***************************************************************************
  **
  ** Called when model has changed
  */   

  public void modelHasChanged(ModelChangeEvent event) {
    appState_.getTextBoxMgr().checkForChanges(event);
    drawModel(false);
    return;
  }  
 
  /***************************************************************************
  **
  ** Called when layout has changed
  */   

  public void layoutHasChanged(LayoutChangeEvent event) {
    drawModel(false);
    return;
  }

  /***************************************************************************
  **
  ** Called when general model change has changed
  */   

  public void generalChangeOccurred(GeneralChangeEvent gcev) {
    if (gcev.getChangeType() == GeneralChangeEvent.MODEL_DATA_CHANGE) {
      drawModel(false);
    }
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Handles click events
  */  
  
  public class MouseHandler extends MouseAdapter {

    private final static int CLICK_SLOP_  = 2;
    
    public void mousePressed(MouseEvent me) {
      try {
        // IGNORE drags except with button 1 only
        int mods = me.getModifiers();
        int onmask = MouseEvent.BUTTON1_MASK;
        int offmask = MouseEvent.BUTTON2_MASK | MouseEvent.BUTTON3_MASK;
        boolean oneOnly = false;
        if ((mods & (onmask | offmask)) == onmask) {
          oneOnly = true;
        }
        //
        // On Linux, the popup trigger is a right mouse press.  On Windows, it is
        // a right mouse RELEASE!!!
        //  
        if (!me.isPopupTrigger()) {
          requestFocusForModelInWindow();
        }

        DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
        if (me.isPopupTrigger()) {
          Point pscreenLoc = me.getComponent().getLocationOnScreen();
          Point abs = new Point(me.getX() + pscreenLoc.x, me.getY() + pscreenLoc.y);
          triggerPopup(me.getX(), me.getY(), abs, rcx);
        } else if (oneOnly) {
          lastPress_ = new Point(me.getX(), me.getY());
          lastShifted_ = me.isShiftDown();        

          lastView_ = myScrollPane_.getViewport().getViewPosition(); 
          Point screenLoc = me.getComponent().getLocationOnScreen();
          lastAbs_ = new Point(me.getX() + screenLoc.x, me.getY() + screenLoc.y);
          lastCtrl_ = me.isControlDown();
          dragLayout_ = null;
          rmov_ = null;
        //  if (readOnly_) {
        //    return;
        //  }     
          Point pt = new Point();
          appState_.getZoomTarget().transformClick(me.getX(), me.getY(), pt);      
          dragSelect_ = nothingHit(pt, rcx);
          /*
          System.out.println("me = " + me);
          System.out.println("alt = " + me.isAltDown());
          System.out.println("crtl = " + me.isControlDown());
          System.out.println("meta = " + me.isMetaDown());
          System.out.println("shift = " + me.isShiftDown());
          */
        }
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
      return;
    }

    public void mouseClicked(MouseEvent me) {
      if (me.isPopupTrigger()) {
        Point pscreenLoc = me.getComponent().getLocationOnScreen();
        Point abs = new Point(me.getX() + pscreenLoc.x, me.getY() + pscreenLoc.y);
        DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
        triggerPopup(me.getX(), me.getY(), abs, rcx);
      }
      return;
    }    
    
    //
    // We want to catch the mouse position when overlaid dialog windows close
    // but the mouse is not moving:
    //
    public void mouseEntered(MouseEvent me) {
      try {
        motionHandle_.mouseMoved(me);
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }        
      return;
    }       
    
    public void mouseReleased(MouseEvent me) {
      //
      // On Linux, the popup trigger is a right mouse press.  On Windows, it is
      // a right mouse RELEASE!!!
      //       
      try {
        // This should handle lack of focus in network display:
        // Fix for bug BT-03-23-05:6
        // Yes, but this makes it impossible for popup menu to have key input!
        // So make it conditional and move it down to the bottom after the
        // popup can be fired    
        //  SUPanel.this.requestFocusInWindow();
        int mods = me.getModifiers();
        int onmask = MouseEvent.BUTTON1_MASK;
        int offmask = MouseEvent.BUTTON2_MASK | MouseEvent.BUTTON3_MASK;
        boolean oneOnly = false;
        if ((mods & (onmask | offmask)) == onmask) {
          oneOnly = true;
        }
        // Do this stuff NO MATTER WHAT!
        lastView_ = null;
        lastAbs_ = null;
        dragLayout_ = null;
        multiMoveLayout_ = null;
        DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
        if (me.isPopupTrigger()) {
          Point pscreenLoc = me.getComponent().getLocationOnScreen();
          Point abs = new Point(me.getX() + pscreenLoc.x, me.getY() + pscreenLoc.y);
          triggerPopup(me.getX(), me.getY(), abs, rcx);
          myGenomePre_.setFloater(null);
        } else if (oneOnly) {
          int currX = me.getX();
          int currY = me.getY();
          dragSelect_ = false;
          myGenomePre_.setNullRender(null);
          if (lastPress_ == null) {
            return;
          }
          int lastX = lastPress_.x;
          int lastY = lastPress_.y;
          int diffX = Math.abs(currX - lastX);
          int diffY = Math.abs(currY - lastY);      
          if ((diffX <= CLICK_SLOP_) && (diffY <= CLICK_SLOP_)) { 
            clickResult(lastX, lastY, lastShifted_, lastCtrl_, rcx);
          } else if ((diffX >= CLICK_SLOP_) || (diffY >= CLICK_SLOP_)) {
            dragResult(lastX, lastY, currX, currY, lastShifted_, lastCtrl_, rcx);
          } else {
            myGenomePre_.setFloater(null);
          }
        }
        // See above; moved down here:
        if (!popCtrl_.isVisible()) {
          requestFocusForModelInWindow();
        }        
        rmov_ = null;
        lastPress_ = null;  // DO THIS NO MATTER WHAT TOO
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
         
      return;
    }

    private boolean nothingHit(Point pt, DataAccessContext rcx) {    
      List<Intersection.AugmentedIntersection> augs = myGenomePre_.intersectItem(pt.x, pt.y, rcx, true, false);
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, rcx)).selectionRanker(augs);
      Intersection intersect = ((ai == null) || (ai.intersect == null)) ? null : ai.intersect;
      
      if (intersect == null) {
        if (myGenomePre_.noMiscHits(rcx, pt)) {
          return (true);
        }
      } else if (rcx.getGenome() instanceof GenomeInstance) {  // OK to hit group, but not group label...
        GenomeInstance gi = rcx.getGenomeAsInstance();
        if (gi.getGroup(intersect.getObjectID()) != null) {
          String groupID = Intersection.getLabelID(intersect);
          if (groupID == null) {
            if (myGenomePre_.noMiscHits(rcx, pt)) {
              return (true);
            }    
          }
        }
      }
      return (false);
    }

    //
    // Route the click:
    //    
    
    private void clickResult(int x, int y, boolean isShifted, boolean isCtrl, DataAccessContext rcx) {
      if (isCtrl) {
        myGenomePre_.setFloater(null);
        return;
      }
      Point pt = new Point();
      appState_.getZoomTarget().transformClick(x, y, pt);
      appState_.getPanelCmds().processMouseClick(pt, isShifted, rcx);    
      return;
    }
    
    private void dragResult(int sx, int sy, int ex, int ey, 
                            boolean isShifted, boolean isCtrl, DataAccessContext rcx) {
      if (isCtrl) {  // Keep floater alive!
        return;
      }
      // Actually rubber band box pulldowns are NOT currently supported!
      if (appState_.getPanelCmds().isModal() && appState_.getIsEditor()) {  // Keep floater alive!
        if (appState_.getPanelCmds().dragNotAllowedForMode()) {
          ResourceManager rMan = appState_.getRMan();
          JOptionPane.showMessageDialog(appState_.getTopFrame(),
                                        rMan.getString("addLink.noDragMessage"), 
                                        rMan.getString("addLink.placementErrorTitle"),
                                        JOptionPane.ERROR_MESSAGE);
        }
        return;
      }
      Point spt = new Point();
      appState_.getZoomTarget().transformClick(sx, sy, spt);
      Point ept = new Point();
      appState_.getZoomTarget().transformClick(ex, ey, ept);
      if (rcx.getGenome() != null) {
        //
        // Once a drag is done, we either move something (if editing), select stuff in a box, or do nothing
        //
        Point2D start = new Point2D.Double(spt.x, spt.y);
        Point2D end = new Point2D.Double(ept.x, ept.y);
        boolean didAMove = false;
        
        if (appState_.getIsEditor()) {
          if (rmov_ == null) {
            rmov_ = Mover.generateRMov(myGenomePre_, rcx, start);
          }
          if (rmov_ != null) {
            if (rmov_.notPermitted) {
              giveErrorFeedback();
              return;
            }
            didAMove = appState_.getPanelCmds().doAMove(rmov_, end);
            rmov_ = null;
          }
        }
        if (!didAMove) {
          Rectangle rect = buildRect(start, end);
          if (rect != null) {
            appState_.getPanelCmds().selectItems(rect, isShifted);
          }
        }
        myGenomePre_.setFloater(null);
        appState_.getTextBoxMgr().clearMessageSource(TextBoxManager.SELECTED_ITEM_MESSAGE);
        drawModel(false);
      }
    }

    private Rectangle buildRect(Point2D start, Point2D end) {
      int x = (int)start.getX();
      int y = (int)start.getY();
      int width = (int)end.getX() - x;
      int height = (int)end.getY() - y;
      int rx, ry, rw, rh;
      if ((width != 0) && (height != 0)) {
        if (width < 0) {
          rx = (int)end.getX();
          rw = -width;
        } else {
          rx = x;
          rw = width;
        }
        if (height < 0) {
          ry = (int)end.getY();
          rh = -height;
        } else {
          ry = y;
          rh = height;
        }
        Rectangle rect = new Rectangle(rx, ry, rw, rh);
        return (rect);
      }
      return (null);
    }

    private void triggerPopup(int x, int y, Point screenAbs, DataAccessContext rcx) {
      try {
        if (appState_.getPanelCmds().isModal()) {
          return;
        }
        if (rcx.getGenome() != null) {
          String currentOverlay = rcx.oso.getCurrentOverlay();
          Point pt = new Point();
          appState_.getZoomTarget().transformClick(x, y, pt);
          List<Intersection.AugmentedIntersection> itemList = myGenomePre_.intersectItem(pt.x, pt.y, rcx, true, (currentOverlay != null));
          Intersection.AugmentedIntersection aug = (new IntersectionChooser(false, rcx).selectionRanker(itemList));
          if ((aug == null) || (aug.intersect == null)) {
            if (currentOverlay == null) {
              return;
            } else {
              // We can "intersect" the overlay if one is active and nothing else is clicked, and ONLY for the
              // purpose of popup menu!  But NOT if we are inside a net module!
              if (myGenomePre_.intersectANetModuleElement(pt.x, pt.y, rcx,
                                                          GenomePresentation.NetModuleIntersect.NET_MODULE_INTERIOR) == null) {
                aug = new Intersection.AugmentedIntersection(null, Intersection.IS_OVERLAY);
              } else {
                return;
              }
            }
          }  
          //
          // Clear selected items
          //
          // WJRL 2/17/12: Going to start keeping selections around.  Any unexpected implications detected?
         // UndoSupport support = new UndoSupport(undom_, "undo.selectionClear");        
         // if (myGenomePre_.clearSelections(appState_.getGenome(), appState_.getLayoutKey(), support)) {
          //  repaint();
          //  support.finish();
         // }
          popCtrl_.showPopup(rcx, aug, x, y, screenAbs); 
        }
        return;
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
    }
  }

  /***************************************************************************
  **
  ** Handles mouse motion events
  */  
      
  public class MouseMotionHandler extends MouseMotionAdapter {
    @Override
    public void mouseDragged(MouseEvent me) {
      try {
        appState_.getTextBoxMgr().clearCurrentMouseOver();
        if ((lastView_ == null) || (lastAbs_ == null) || (lastPress_ == null)) {
          return;
        }
        // IGNORE drags except with button 1 only
        int mods = me.getModifiers();
        int onmask = MouseEvent.BUTTON1_MASK;
        int offmask = MouseEvent.BUTTON2_MASK | MouseEvent.BUTTON3_MASK;
        if (!((mods & (onmask | offmask)) == onmask)) {
          return;
        }
        
        if ((mods & MouseEvent.BUTTON2_DOWN_MASK) != 0x00) {
          return;
        }
        Point currPt = me.getPoint();
        if (me.isControlDown()) {
          Point compLoc = me.getComponent().getLocationOnScreen();
          Point currAbs = new Point(compLoc.x + currPt.x, compLoc.y + currPt.y);               

          JScrollBar hsb = myScrollPane_.getHorizontalScrollBar();
          int hMax = hsb.getMaximum() - hsb.getVisibleAmount();
          int hMin = hsb.getMinimum();
          int newX = lastView_.x - (currAbs.x - lastAbs_.x);
          if (newX > hMax) newX = hMax;
          if (newX < hMin) newX = hMin;


          JScrollBar vsb = myScrollPane_.getVerticalScrollBar();
          int vMax = vsb.getMaximum() - vsb.getVisibleAmount();
          int vMin = vsb.getMinimum();
          int newY = lastView_.y - (currAbs.y - lastAbs_.y);
          if (newY > vMax) newY = vMax;
          if (newY < vMin) newY = vMin;        

          myScrollPane_.getViewport().setViewPosition(new Point(newX, newY));
          myScrollPane_.getViewport().invalidate();      
          myScrollPane_.revalidate();
          return;
        }
        // Can't rubber band for any modal case except root instance pull down...
        // Actually rubber band box pulldowns are NOT currently supported!       
        if (appState_.getPanelCmds().isModal()) {
          return;
        }
        Point pt0 = new Point();
        appState_.getZoomTarget().transformClick(me.getX(), me.getY(), pt0);
        UiUtil.forcePtToGrid(pt0.x, pt0.y, pt0);
        Point pt2 = new Point();
        appState_.getZoomTarget().transformClick(lastPress_.getX(), lastPress_.getY(), pt2);
        UiUtil.forcePtToGrid(pt2.x, pt2.y, pt2); 
        if (dragSelect_) {
          dragFloater_.clear();
          dragFloater_.add(pt0);
          Point pt1 = new Point(pt2.x, pt0.y);
          dragFloater_.add(pt1);
          dragFloater_.add(pt2);
          Point pt3 = new Point(pt0.x, pt2.y);
          dragFloater_.add(pt3);
          myGenomePre_.setFloater(dragFloater_);        
        } else if (appState_.getIsEditor()) { 
          // How's this for inefficient???
          DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
          dragLayout_ = new Layout(rcx.getLayout());
          if (rmov_ == null) {
            rmov_ = Mover.generateRMov(myGenomePre_, rcx, pt2);
          }
          if (rmov_ != null) {
            if (rmov_.notPermitted) {
              giveErrorFeedback();
              return;
            }
            appState_.getPanelCmds().visualizeAMove(rmov_, pt0, dragLayout_);
          }
        }
        myGenomePre_.setFloaterPosition(pt0);
        drawModel(false);
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
    }
    
    @Override
    public void mouseMoved(MouseEvent me) {
      try {
        DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
        String mouseOverText = null;
        if (!appState_.getIsEditor()) {
          mouseOverText = getMouseOverText(me, rcx);
          if (mouseOverText != null) {
            appState_.getTextBoxMgr().setCurrentMouseOver(mouseOverText);
          } else {
            appState_.getTextBoxMgr().clearCurrentMouseOver();
          }
          return;
        }
        PanelCommands.ModeHandler handler = appState_.getPanelCmds().getCurrentHandler(!appState_.getIsEditor());
        if (myGenomePre_.isFloaterActive()) {
          Point pt = new Point();
          appState_.getZoomTarget().transformClick(me.getX(), me.getY(), pt);          
          if (handler.floaterIsRect() && appState_.getIsEditor()) {
            int x = UiUtil.forceToGridValueInt(pt.x, UiUtil.GRID_SIZE);
            int y = UiUtil.forceToGridValueInt(pt.y, UiUtil.GRID_SIZE);  
            myGenomePre_.setFloater(handler.getFloater(x, y), handler.getFloaterColor());        
          } else {
            if (handler.floaterIsLines() && appState_.getIsEditor()) {
              int x = UiUtil.forceToGridValueInt(pt.x, UiUtil.GRID_SIZE);
              int y = UiUtil.forceToGridValueInt(pt.y, UiUtil.GRID_SIZE);  
              List ptList = (List)handler.getFloater(x, y);                    
              if ((ptList != null) && me.isShiftDown()) {          
                Point2D lastPoint = (Point2D)ptList.get(ptList.size() - 1);
                Vector2D vec = (new Vector2D(lastPoint, new Point(x, y))).canonical();
                Point2D canonicalPt = vec.add(lastPoint);
                pt = new Point((int)canonicalPt.getX(), (int)canonicalPt.getY());
              }
            }
            myGenomePre_.setFloaterPosition(pt);
          }
          drawModel(false);
        } else if (handler.isMoveHandler() && appState_.getIsEditor()) {
          Point pt = new Point();
          appState_.getZoomTarget().transformClick(me.getX(), me.getY(), pt);
          appState_.getPanelCmds().processMouseMotion(pt, rcx);
        } else if (!appState_.getPanelCmds().isModal()) {
          mouseOverText = getMouseOverText(me, rcx);
        }
        if (mouseOverText != null) {
          appState_.getTextBoxMgr().setCurrentMouseOver(mouseOverText);
        } else {
          appState_.getTextBoxMgr().clearCurrentMouseOver();
        }
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
    }      
    
    private String getMouseOverText(MouseEvent me, DataAccessContext rcx) {
      if (rcx.getGenome() == null) {
        return (null);
      }
      Point pt = new Point();
      appState_.getZoomTarget().transformClick(me.getX(), me.getY(), pt);
      Point2D pt2d = new Point2D.Float(pt.x, pt.y);
      List<Intersection> itemList = myGenomePre_.intersectNotes(pt2d, rcx, true);
      Intersection intersect = (new IntersectionChooser(true, rcx).intersectionRanker(itemList));
      if (intersect != null) {
        String itemID = intersect.getObjectID();
        Note note = rcx.getGenome().getNote(itemID);
        if (note != null) {
          return (note.getTextWithBreaksReplaced());
        }
      }
      return (null);
    }          
  }
   
  /***************************************************************************
  **
  ** This is what paints
  */     
    
  private class MyPaintPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    
    @Override
    public void paintComponent(Graphics g) {
      try {
        super.paintComponent(g);
        vexp_.drawingGuts(g, false, false, null, showBubbles_, true, getSFD());
        appState_.getTextBoxMgr().refresh();
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayPaintException(ex);
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Get the preferred size
    */

    private ViewExporter.StateForDraw getSFD() {
      DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
      ViewExporter.StateForDraw sfd = new ViewExporter.StateForDraw(appState_, rcx, rmov_, 
                                                                    menuDrivenShowComponentModule_, 
                                                                    dragLayout_, multiMoveLayout_, 
                                                                    myScrollPane_.getViewport().getViewRect(), null, 
                                                                    appState_.getFontMgr());
      return (sfd);
    }
    
    /***************************************************************************
    **
    ** Get the preferred size
    */
    
    @Override
    public Dimension getPreferredSize() {
      return (appState_.getZoomTarget().getPreferredSize());
    }
    
    /***************************************************************************
    **
    ** Get the tool tip
    */
    
    @Override
    public String getToolTipText(MouseEvent event) {
      Point tipPoint = event.getPoint();
      appState_.getZoomTarget().transformPoint(tipPoint);
      DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
      List<Intersection.AugmentedIntersection> augs = myGenomePre_.intersectItem(tipPoint.x, tipPoint.y, rcx, false, false);    
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, rcx)).selectionRanker(augs);
      if ((ai == null) || (ai.intersect == null)) {
        return (null);
      }
      String objID = ai.intersect.getObjectID();
      
      Linkage link = rcx.getGenome().getLinkage(objID);  // This could be ANY link through a bus segment
      if (link == null) {
        return (null);
      }
      return (vexp_.buildTooltip(objID, getSFD()));
    }  
  }  
}
