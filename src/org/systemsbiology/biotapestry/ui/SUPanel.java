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

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.PopCommands;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.move.Mover;
import org.systemsbiology.biotapestry.cmd.flow.move.RunningMove;
import org.systemsbiology.biotapestry.cmd.undo.SelectionChangeCmd;
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
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.Note;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenu;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
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

  private UIComponentSource uics_;
  private CmdSource cSrc_;
  private TabSource tSrc_; 
  private HarnessBuilder hBld_;
  private DynamicDataAccessContext ddacx_;
  
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
  
  public SUPanel(BTState appState, UIComponentSource uics, CmdSource cSrc, TabSource tSrc, HarnessBuilder hBld, DynamicDataAccessContext ddacx) {
    uics_ = uics;
    cSrc_ = cSrc;
    tSrc_ = tSrc;
    hBld_ = hBld;
    ddacx_ = ddacx;
    myPanel_ = (uics_.isHeadless()) ? null : new MyPaintPanel();
    uics_.setSUPanel(this);
    myGenomePre_ = new GenomePresentation(uics, true, .38, true, appState.getRenderingContextForZTS());
    uics_.setGenomePresentation(myGenomePre_);
    ZoomTargetSupport zts = new ZoomTargetSupport(myGenomePre_, myPanel_, appState.getRenderingContextForZTS());
    uics_.setZoomTarget(zts);
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
    StaticDataAccessContext dacx = new StaticDataAccessContext(ddacx_).getContextForRoot();
    cSrc_.setPopCmds(new PopCommands(uics_, cSrc_, ddacx, hBld_));
    // This gets built even when headless:
    popCtrl_ = new PopControl(dacx, uics_, cSrc_, tSrc_, hBld_); 
    
    if (myPanel_ != null) {
      myPanel_.addMouseListener(new MouseHandler());
      motionHandle_ = new MouseMotionHandler();
      myPanel_.addMouseMotionListener(motionHandle_);
    }
    
    EventManager em = uics_.getEventMgr();
    em.addLayoutChangeListener(this);
    em.addGeneralChangeListener(this);  
    em.addModelChangeListener(this);
    
    if (myPanel_ != null) {
      myPanel_.setBackground(new Color(240, 240, 240));
    }
    uics_.setTextBoxManager(new TextBoxManager(uics_, dacx));

    dragFloater_ = new ArrayList<Point>();
    uics_.setCursorManager(new CursorManager(myPanel_));
    cSrc_.setPanelCmds(new PanelCommands(uics_, ddacx, hBld_));
    
  //  currentNetMods_ = new TaggedSet();
  //  revealed_ = new TaggedSet();
    
    ResourceManager rMan = uics_.getRMan();
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

  public void incrementToNextSelection(StaticDataAccessContext rcx) {
    myGenomePre_.bumpNextSelection(rcx);
    return;
  }
   
  /***************************************************************************
  ** 
  ** Bump to previous selection
  */

  public void decrementToPreviousSelection(StaticDataAccessContext rcx) {
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
    
    if (hasASelection() && (dacx.getCurrentGenomeID() != null)) {
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
    if (hasASelection() && (dacx.getCurrentGenomeID() != null)) {
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
  ** Display error information
  */
  
  public void giveErrorFeedback() {
    uics_.getCursorMgr().signalError();
    Toolkit.getDefaultToolkit().beep();
    return;
  }
  
  /***************************************************************************
  **
  ** Get an external selection event filled in
  */
  
  public ExternalSelectionChangeEvent getExternalSelectionEvent() {
    StaticDataAccessContext rcx = new StaticDataAccessContext(ddacx_);
    return (myGenomePre_.selectionsForExternalEvents(rcx));
  }
  
  /***************************************************************************
  **
  ** Select everybody.  If we are currently only showing nodes (overlay
  ** hiding links), we only select nodes:
  */
  
  public void selectAll(UIComponentSource uics, UndoFactory uFac, StaticDataAccessContext rcx) {

    String currentOverlay = rcx.getOSO().getCurrentOverlay();
    if (currentOverlay != null) {
      NetOverlayProperties nop = rcx.getCurrentLayout().getNetOverlayProperties(currentOverlay);
      if (nop.hideLinks()) {
        myGenomePre_.selectAllNodes(uics, rcx, uFac);
        return;
      }
    }
    myGenomePre_.selectAll(uics, rcx, uFac);
    return;
  }
  
  /***************************************************************************
  **
  ** Select nobody
  */
  
  public void selectNone(UIComponentSource uics, UndoFactory uFac, StaticDataAccessContext rcx) {
    myGenomePre_.selectNone(uics, rcx, uFac);
    return;
  }  

  /***************************************************************************
  **
  ** Drop nodes from selections
  */
  
  public void dropNodeSelections(UIComponentSource uics, Integer type, UndoFactory uFac, StaticDataAccessContext rcx) {
    myGenomePre_.dropNodeSelections(uics, rcx, type, uFac);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop links from selections
  */
  
  public SelectionChangeCmd.Bundle dropLinkSelections(UIComponentSource uics, boolean recordBundle, UndoFactory uFac, StaticDataAccessContext rcx) {
    SelectionChangeCmd.Bundle bundle = (recordBundle) ? new SelectionChangeCmd.Bundle() : null;
    myGenomePre_.dropLinkSelections(uics, rcx, bundle, uFac);
    return (bundle);
  }  

  /***************************************************************************
  **
  ** Set the state to show link target bubbles.
  */
  
  public void toggleTargetBubbles() {
    showBubbles_ = !showBubbles_;
    if (cSrc_.getPanelCmds().isInShowBubbleMode()) {
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
  
  public Set<String> getSelectedNodes(DataAccessContext rcx) {
    Genome genome = rcx.getCurrentGenome();
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
    
    StaticDataAccessContext rcx = new StaticDataAccessContext(ddacx_);
    boolean showAsDiff = false;
    if (rcx.currentGenomeIsADynamicInstance()) {  
      showAsDiff = rcx.getGenomeSource().getDynamicProxy(((DynamicGenomeInstance)rcx.getCurrentGenome()).getProxyID()).showAsSimDiff();
    }   
    IRenderer.Mode mode = (!showAsDiff) ? IRenderer.Mode.NORMAL : IRenderer.Mode.DELTA;
    ViewExporter.StateForDraw sfd = new ViewExporter.StateForDraw(uics_, rcx, cSrc_, rmov_, 
                                                                  menuDrivenShowComponentModule_, 
                                                                  dragLayout_, multiMoveLayout_, 
                                                                  null, null, rcx.getFontManager(), mode);

    return (vexp_.print(g, pf, pageIndex, sfd));
  }

  /***************************************************************************
  **
  ** Support image export
  */  
  
  public ViewExporter.BoundsMaps exportToFile(UIComponentSource uics, DataAccessContext rcx, 
                                              File saveFile, boolean calcMap, 
                                              String format, ImageExporter.ResolutionSettings res,
                                              double zoom, Dimension size) throws IOException {

    boolean showAsDiff = false;
    if (rcx.currentGenomeIsADynamicInstance()) {  
      showAsDiff = rcx.getGenomeSource().getDynamicProxy(((DynamicGenomeInstance)rcx.getCurrentGenome()).getProxyID()).showAsSimDiff();
    }  
    IRenderer.Mode mode = (!showAsDiff) ? IRenderer.Mode.NORMAL : IRenderer.Mode.DELTA;
    ViewExporter.StateForDraw sfd = new ViewExporter.StateForDraw(uics, rcx, cSrc_, rmov_, 
                                                                  menuDrivenShowComponentModule_, 
                                                                  dragLayout_, multiMoveLayout_, 
                                                                  null, null, rcx.getFontManager(), mode);
    
    
    
    return (vexp_.exportToFile(saveFile, calcMap, format, res, zoom, size, sfd));
  }  
  
  /***************************************************************************
  **
  ** Support image export
  */  
  
  public ViewExporter.BoundsMaps exportToStream(UIComponentSource uics, DataAccessContext rcx,
                                                OutputStream stream, boolean calcMap, 
                                                String format, ImageExporter.ResolutionSettings res,
                                                double zoom, Dimension size) throws IOException {
    boolean showAsDiff = false;
    if (rcx.currentGenomeIsADynamicInstance()) {  
      showAsDiff = rcx.getGenomeSource().getDynamicProxy(((DynamicGenomeInstance)rcx.getCurrentGenome()).getProxyID()).showAsSimDiff();
    }  
    IRenderer.Mode mode = (!showAsDiff) ? IRenderer.Mode.NORMAL : IRenderer.Mode.DELTA;
    ViewExporter.StateForDraw sfd = new ViewExporter.StateForDraw(uics, rcx, cSrc_, rmov_, 
                                                                  menuDrivenShowComponentModule_, 
                                                                  dragLayout_, multiMoveLayout_, 
                                                                  null, null, rcx.getFontManager(), mode);
    return (vexp_.exportToStream(stream, calcMap, format, res, zoom, size, sfd));
  }   

  /***************************************************************************
  **
  ** Support JSON export
  */  
  
  public Map<String,Object> exportModelMap(UIComponentSource uics, StaticDataAccessContext rcx, boolean calcMap, 
                                           double zoom, Dimension size) throws IOException {
    
    boolean showAsDiff = false;
    if (rcx.currentGenomeIsADynamicInstance()) {  
      showAsDiff = rcx.getGenomeSource().getDynamicProxy(((DynamicGenomeInstance)rcx.getCurrentGenome()).getProxyID()).showAsSimDiff();
    }  
    IRenderer.Mode mode = (!showAsDiff) ? IRenderer.Mode.NORMAL : IRenderer.Mode.DELTA;
    ViewExporter.StateForDraw sfd = new ViewExporter.StateForDraw(uics, rcx, cSrc_, rmov_, 
                                                                  menuDrivenShowComponentModule_, 
                                                                  dragLayout_, multiMoveLayout_, 
                                                                  null, rcx.getGenomeSource().getTextFromGenomeKey(rcx.getCurrentGenomeID()), 
                                                                  rcx.getFontManager(), mode);
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
    uics_.getTextBoxMgr().checkForChanges(event);
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
    private final static double ACCEPT_DRAG_AS_CLICK_ = 60.0;
    
    @Override
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

        StaticDataAccessContext rcx = new StaticDataAccessContext(ddacx_);
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
          rcx.getZoomTarget().transformClick(me.getX(), me.getY(), pt);      
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
        uics_.getExceptionHandler().displayException(ex);
      }
      return;
    }

    @Override
    public void mouseClicked(MouseEvent me) {
      if (me.isPopupTrigger()) {
        Point pscreenLoc = me.getComponent().getLocationOnScreen();
        Point abs = new Point(me.getX() + pscreenLoc.x, me.getY() + pscreenLoc.y);
        StaticDataAccessContext rcx = new StaticDataAccessContext(ddacx_);
        triggerPopup(me.getX(), me.getY(), abs, rcx);
      }
      return;
    }    
    
    //
    // We want to catch the mouse position when overlaid dialog windows close
    // but the mouse is not moving:
    //
    @Override
    public void mouseEntered(MouseEvent me) {
      try {
        motionHandle_.mouseMoved(me);
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }        
      return;
    }       

    @Override
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
        StaticDataAccessContext rcx = new StaticDataAccessContext(ddacx_);
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
        uics_.getExceptionHandler().displayException(ex);
      }
         
      return;
    }

    private boolean nothingHit(Point pt, StaticDataAccessContext rcx) {    
      List<Intersection.AugmentedIntersection> augs = myGenomePre_.intersectItem(pt.x, pt.y, rcx, true, false);
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, rcx)).selectionRanker(augs);
      Intersection intersect = ((ai == null) || (ai.intersect == null)) ? null : ai.intersect;
      
      if (intersect == null) {
        if (myGenomePre_.noMiscHits(rcx, pt)) {
          return (true);
        }
      } else if (rcx.getCurrentGenome() instanceof GenomeInstance) {  // OK to hit group, but not group label...
        GenomeInstance gi = rcx.getCurrentGenomeAsInstance();
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
    
    private void clickResult(int x, int y, boolean isShifted, boolean isCtrl, StaticDataAccessContext rcx) {
      if (isCtrl) {
        myGenomePre_.setFloater(null);
        return;
      }
      Point pt = new Point();
      rcx.getZoomTarget().transformClick(x, y, pt);
      cSrc_.getPanelCmds().processMouseClick(pt, isShifted, rcx);    
      return;
    }
    
    private void dragResult(int sx, int sy, int ex, int ey, 
                            boolean isShifted, boolean isCtrl, StaticDataAccessContext rcx) {
      if (isCtrl) {  // Keep floater alive!
        return;
      }
      // Actually rubber band box pulldowns are NOT currently supported!
      if (cSrc_.getPanelCmds().isModal() && uics_.getIsEditor()) {  // Keep floater alive!
        if (cSrc_.getPanelCmds().dragNotAllowedForMode()) {
          Vector2D totalDrag = new Vector2D(ex - sx, ey - sy);
          if (totalDrag.length() < ACCEPT_DRAG_AS_CLICK_) {
            clickResult(sx, sy, isShifted, isCtrl, rcx);
            return;
          }
          ResourceManager rMan = rcx.getRMan();
          JOptionPane.showMessageDialog(uics_.getTopFrame(),
                                        rMan.getString("addLink.noDragMessage"), 
                                        rMan.getString("addLink.placementErrorTitle"),
                                        JOptionPane.ERROR_MESSAGE);
        }
        return;
      }
      Point spt = new Point();
      rcx.getZoomTarget().transformClick(sx, sy, spt);
      Point ept = new Point();
      rcx.getZoomTarget().transformClick(ex, ey, ept);
      if (rcx.getCurrentGenome() != null) {
        //
        // Once a drag is done, we either move something (if editing), select stuff in a box, or do nothing
        //
        Point2D start = new Point2D.Double(spt.x, spt.y);
        Point2D end = new Point2D.Double(ept.x, ept.y);
        boolean didAMove = false;
        
        if (uics_.getIsEditor()) {
          if (rmov_ == null) {
            rmov_ = Mover.generateRMov(myGenomePre_, rcx, start);
          }
          if (rmov_ != null) {
            if (rmov_.notPermitted) {
              giveErrorFeedback();
              return;
            }
            didAMove = cSrc_.getPanelCmds().doAMove(rmov_, end);
            rmov_ = null;
          }
        }
        if (!didAMove) {
          Rectangle rect = buildRect(start, end);
          if (rect != null) {
            cSrc_.getPanelCmds().selectItems(rect, isShifted);
          }
        }
        myGenomePre_.setFloater(null);
        uics_.getTextBoxMgr().clearMessageSource(TextBoxManager.SELECTED_ITEM_MESSAGE);
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

    private void triggerPopup(int x, int y, Point screenAbs, StaticDataAccessContext rcx) {
      try {
        if (cSrc_.getPanelCmds().isModal()) {
          return;
        }
        if (rcx.getCurrentGenome() != null) {
          String currentOverlay = rcx.getOSO().getCurrentOverlay();
          Point pt = new Point();
          rcx.getZoomTarget().transformClick(x, y, pt);
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
        uics_.getExceptionHandler().displayException(ex);
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
        StaticDataAccessContext rcx = new StaticDataAccessContext(ddacx_);
        uics_.getTextBoxMgr().clearCurrentMouseOver();
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
        if (cSrc_.getPanelCmds().isModal()) {
          return;
        }
        Point pt0 = new Point();
        rcx.getZoomTarget().transformClick(me.getX(), me.getY(), pt0);        
        Point pt2 = new Point();
        rcx.getZoomTarget().transformClick(lastPress_.getX(), lastPress_.getY(), pt2);       
        if (dragSelect_) {
          UiUtil.forcePtToGrid(pt0.x, pt0.y, pt0);
          UiUtil.forcePtToGrid(pt2.x, pt2.y, pt2); 
          dragFloater_.clear();
          dragFloater_.add(pt0);
          Point pt1 = new Point(pt2.x, pt0.y);
          dragFloater_.add(pt1);
          dragFloater_.add(pt2);
          Point pt3 = new Point(pt0.x, pt2.y);
          dragFloater_.add(pt3);
          myGenomePre_.setFloater(dragFloater_);        
        } else if (uics_.getIsEditor()) { 
          // How's this for inefficient???
          dragLayout_ = new Layout(rcx.getCurrentLayout());
          if (rmov_ == null) {
            //
            // ISSUE #215 RMOV generation now using the original lastPress_, not the gridded lastPress_;
            // the original one is what is used in the final move operation. Note that if no intersections
            // arise from this, the rmov_ continues to be null, and we witness no dragging
            // in progress. So if the original point intersects and the gridded does not, we got
            // issue #215 cropping up.
            // Note also that it is crucial we remain consistent with the nothingHit() call, which is what
            // sets the dragSelect_ == TRUE, and which uses non-gridded points to check for intersections.
            //
            rmov_ = Mover.generateRMov(myGenomePre_, rcx, pt2);
          }
          if (rmov_ != null) {
            if (rmov_.notPermitted) {
              giveErrorFeedback();
              return;
            }
            cSrc_.getPanelCmds().visualizeAMove(rmov_, pt0, dragLayout_);
          }
          // #215 FIX: Do gridding now, not before, to make the floater on-grid.
          UiUtil.forcePtToGrid(pt0.x, pt0.y, pt0);
        }
        myGenomePre_.setFloaterPosition(pt0);
        drawModel(false);
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
    }
    
    @Override
    public void mouseMoved(MouseEvent me) {
      try {
        StaticDataAccessContext rcx = new StaticDataAccessContext(ddacx_);
        String mouseOverText = null;
        if (!uics_.getIsEditor()) {
          mouseOverText = getMouseOverText(me, rcx);
          if (mouseOverText != null) {
            uics_.getTextBoxMgr().setCurrentMouseOver(mouseOverText);
          } else {
            uics_.getTextBoxMgr().clearCurrentMouseOver();
          }
          return;
        }
        PanelCommands.ModeHandler handler = cSrc_.getPanelCmds().getCurrentHandler(!uics_.getIsEditor());
        if (myGenomePre_.isFloaterActive()) {
          Point pt = new Point();
          rcx.getZoomTarget().transformClick(me.getX(), me.getY(), pt);          
          if (handler.floaterIsRect() && uics_.getIsEditor()) {
            int x = UiUtil.forceToGridValueInt(pt.x, UiUtil.GRID_SIZE);
            int y = UiUtil.forceToGridValueInt(pt.y, UiUtil.GRID_SIZE);  
            myGenomePre_.setFloater(handler.getFloater(x, y), handler.getFloaterColor());        
          } else {
            if (handler.floaterIsLines() && uics_.getIsEditor()) {
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
        } else if (handler.isMoveHandler() && uics_.getIsEditor()) {
          Point pt = new Point();
          rcx.getZoomTarget().transformClick(me.getX(), me.getY(), pt);
          cSrc_.getPanelCmds().processMouseMotion(pt, rcx);
        } else if (!cSrc_.getPanelCmds().isModal()) {
          mouseOverText = getMouseOverText(me, rcx);
        }
        if (mouseOverText != null) {
          uics_.getTextBoxMgr().setCurrentMouseOver(mouseOverText);
        } else {
          uics_.getTextBoxMgr().clearCurrentMouseOver();
        }
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
    }      
    
    private String getMouseOverText(MouseEvent me, StaticDataAccessContext rcx) {
      if (rcx.getCurrentGenome() == null) {
        return (null);
      }
      Point pt = new Point();
      rcx.getZoomTarget().transformClick(me.getX(), me.getY(), pt);
      Point2D pt2d = new Point2D.Float(pt.x, pt.y);
      List<Intersection> itemList = myGenomePre_.intersectNotes(pt2d, rcx, true);
      Intersection intersect = (new IntersectionChooser(true, rcx).intersectionRanker(itemList));
      if (intersect != null) {
        String itemID = intersect.getObjectID();
        Note note = rcx.getCurrentGenome().getNote(itemID);
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
        StaticDataAccessContext rcx = new StaticDataAccessContext(ddacx_);
        vexp_.drawingGuts(g, false, false, null, showBubbles_, true, getSFD(uics_, rcx));
        uics_.getTextBoxMgr().refresh();
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayPaintException(ex);
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Get the state for draw
    */

    private ViewExporter.StateForDraw getSFD(UIComponentSource uics, StaticDataAccessContext rcx) {
      boolean showAsDiff = false;
      if (rcx.currentGenomeIsADynamicInstance()) {  
        showAsDiff = rcx.getGenomeSource().getDynamicProxy(((DynamicGenomeInstance)rcx.getCurrentGenome()).getProxyID()).showAsSimDiff();
      }  
      IRenderer.Mode mode = (!showAsDiff) ? IRenderer.Mode.NORMAL : IRenderer.Mode.DELTA;
      ViewExporter.StateForDraw sfd = new ViewExporter.StateForDraw(uics, rcx, cSrc_, rmov_, 
                                                                    menuDrivenShowComponentModule_, 
                                                                    dragLayout_, multiMoveLayout_, 
                                                                    myScrollPane_.getViewport().getViewRect(), null, 
                                                                    rcx.getFontManager(), mode);
      return (sfd);
    }
    
    /***************************************************************************
    **
    ** Get the preferred size
    */
    
    @Override
    public Dimension getPreferredSize() {
      return (uics_.getZoomTarget().getPreferredSize());
    }
    
    /***************************************************************************
    **
    ** Get the tool tip
    */
    
    @Override
    public String getToolTipText(MouseEvent event) {
      Point tipPoint = event.getPoint();
      StaticDataAccessContext rcx = new StaticDataAccessContext(ddacx_);
      rcx.getZoomTarget().transformPoint(tipPoint);   
      List<Intersection.AugmentedIntersection> augs = myGenomePre_.intersectItem(tipPoint.x, tipPoint.y, rcx, false, false);    
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, rcx)).selectionRanker(augs);
      if ((ai == null) || (ai.intersect == null)) {
        return (null);
      }
      String objID = ai.intersect.getObjectID();
      
      Linkage link = rcx.getCurrentGenome().getLinkage(objID);  // This could be ANY link through a bus segment
      if (link == null) {
        return (null);
      }
      return (vexp_.buildTooltip(objID, getSFD(uics_, rcx)));
    }  
  }  
}
