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

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.systemsbiology.biotapestry.analysis.Path;
import org.systemsbiology.biotapestry.analysis.PathRanker;
import org.systemsbiology.biotapestry.analysis.SignedTaggedLink;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DesktopControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.display.PathGenerator;
import org.systemsbiology.biotapestry.db.FreestandingSourceBundle;
import org.systemsbiology.biotapestry.db.LocalGenomeSource;
import org.systemsbiology.biotapestry.db.LocalLayoutSource;
import org.systemsbiology.biotapestry.db.LocalWorkspaceSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.nav.LocalGroupSettingSource;
import org.systemsbiology.biotapestry.ui.FreezeDriedOverlayOracle;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.LocalDispOptMgr;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
 
/****************************************************************************
**
** Panel for displaying a path
*/

public class PathDisplay extends JPanel implements ListSelectionListener, ModelViewPanel.TipGenerator {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private static final int PLUS_ICON_     = 0;  
  private static final int MINUS_ICON_    = 1;    
  private static final int QUESTION_ICON_ = 2;    
  private static final int AVOID_ICON_    = 3;      
  private static final int NUM_ICONS_     = 4;
  
  private static final int DEFAULT_DEPTH_ = 4;
  private static final int COMPRESSED_DEFAULT_DEPTH_ = 2;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private int defaultDepth_;
  private JComboBox depthCombo_;
  
  private String srcID_;
  private String targID_;
  private String phonyGenomeID_;
  
  private JList pathList_;
  private PathRenderer listRenderer_;
  private boolean showQPCR_;
  private boolean ignore_;
  private PathDisplayTracker pathTracker_;
  private PathRanker pathRanker_;
  private int maxDepth_;
  private BTState appState_;
  private FontRenderContext frci_;
  private ModelViewPanel msp_;
  private LocalWorkspaceSource lws_;
  private LocalLayoutSource lls_;
  private PathGenerator.StrungOutPath selected_;
  private JPanel hidingPanel_;
  private JLabel noPathLabel_;
  private CardLayout myCard_;
  private boolean neverAPath_;
  private int origDefaultDepth_;
  private DataAccessContext rcx_;
  private int crazyPath_;
  
  private List<PathGenerator.StrungOutPath> requestPaths_;
  private FreestandingSourceBundle requestFsb_;
  private PathGenerator.TipSource requestTips_;
  private String layoutID_;
  
  private LocalGenomeSource pSrc_;
  private String truID_;

  private static final long serialVersionUID = 1L;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public PathDisplay(BTState appState, String srcID, String targID, boolean showQPCR) {
    this(appState, false, DEFAULT_DEPTH_, showQPCR);
    srcID_ = srcID;
    targID_ = targID;
  }

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public PathDisplay(BTState appState) {
    this(appState, true, COMPRESSED_DEFAULT_DEPTH_, false);
    
  }

  /***************************************************************************
  **
  ** Constructor. 
  */ 
  
  public PathDisplay(BTState appState, boolean compressedLayout, int defaultDepth, boolean showQPCR) {
    appState_ = appState;
    showQPCR_ = showQPCR;
    URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/LinkPlus.gif");  
    ImageIcon[] icons = new ImageIcon[NUM_ICONS_];   
    icons[PLUS_ICON_] = new ImageIcon(ugif);
    ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/LinkMinus.gif");
    icons[MINUS_ICON_] = new ImageIcon(ugif);
    ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/LinkQuestion.gif");
    icons[QUESTION_ICON_] = new ImageIcon(ugif);
    ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/LinkAvoid.gif");
    icons[AVOID_ICON_] = new ImageIcon(ugif);    
        
    ResourceManager rMan = appState_.getRMan(); 

    defaultDepth_ = defaultDepth;
    ignore_ = false;
    origDefaultDepth_ = defaultDepth;
    srcID_ = null;
    targID_ = null;
    frci_ = new FontRenderContext(new AffineTransform(), true, true);
    selected_ = null;
    
    //
    // Build the path panel. Note that the view components have a reference to this RenderingContext. Change this, and they see it too.
    //
    
    LocalGenomeSource currGSrc = new LocalGenomeSource(new DBGenome(appState_, "foo", "bar"), new DBGenome(appState_, "foo", "bar"));
    lls_ = new LocalLayoutSource(new Layout(appState_,"__BOGUSKEY__", "bar"), currGSrc);
    lws_ = new LocalWorkspaceSource();
    rcx_ = new DataAccessContext(null, null, false, false, 0.0,
                                appState_.getFontMgr(), new LocalDispOptMgr(appState_.getDisplayOptMgr().getDisplayOptions().clone()), 
                                frci_, currGSrc, appState_.getDB(), false, 
                                new FullGenomeHierarchyOracle(currGSrc, lls_), appState_.getRMan(), 
                                new LocalGroupSettingSource(), lws_, lls_,
                                new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null), 
                                appState_.getDB(), appState_.getDB()
    );
    
    ModelViewPanelWithZoom mvpwz = new ModelViewPanelWithZoom(appState_, null, false, true, rcx_);
    hidingPanel_ = new JPanel();
    myCard_ = new CardLayout();
    hidingPanel_.setLayout(myCard_);
    hidingPanel_.add(mvpwz, "Display");
    JPanel blankPanel = new JPanel();
    blankPanel.setBackground(Color.white);
    blankPanel.setLayout(new GridLayout(1,1));
    noPathLabel_ = new JLabel("", JLabel.CENTER);
    blankPanel.add(noPathLabel_);
    hidingPanel_.add(blankPanel, "Hiding");
       
    msp_ = mvpwz.getModelView();
    msp_.setToolTipGenerator(this);
    
    if (showQPCR_) {
      ToolTipManager.sharedInstance().registerComponent(msp_);      
    }  
 
    //
    // Depth selector:
    //
    
    JPanel bottomHalf = new JPanel();
    bottomHalf.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
        
    JLabel label = new JLabel(rMan.getString("showPath.depth"));

    depthCombo_ = new JComboBox();
    depthCombo_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (ignore_ || (srcID_ == null)) {
            return;
          }
          int newDepth = depthCombo_.getSelectedIndex() + 1;
          refreshPath(newDepth, true, false);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        } catch (OutOfMemoryError oom) {
          appState_.getExceptionHandler().displayOutOfMemory(oom);
        }
      }
    });
    
   
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    bottomHalf.add(label, gbc);
    UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
    bottomHalf.add(depthCombo_, gbc);
    
    //
    // Path listing:
    //

    pathList_ = new JList();
    pathList_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    pathList_.addListSelectionListener(this);
    listRenderer_ = new PathRenderer(new ArrayList<PathGenerator.StrungOutPath>(), icons);
    pathList_.setCellRenderer(listRenderer_);

    label = new JLabel(rMan.getString("showPath.paths"));
     
    if (compressedLayout) {
      UiUtil.gbcSet(gbc, 2, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    } else {
      UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    }
    bottomHalf.add(label, gbc);

    JScrollPane jsp = new JScrollPane(pathList_);
    if (compressedLayout) {
      UiUtil.gbcSet(gbc, 3, 0, 7, 6, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    } else {
      UiUtil.gbcSet(gbc, 1, 1, 9, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    }
    bottomHalf.add(jsp, gbc);
    
    bottomHalf.setMinimumSize(new Dimension(250, 150));
    hidingPanel_.setMinimumSize(new Dimension(250, (compressedLayout) ? 150 : 250));    
    JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, hidingPanel_, bottomHalf);
    setLayout(new GridLayout(1,1));
    add(sp);
    if (compressedLayout) {
      setMinimumSize(new Dimension(300, 300));
      setPreferredSize(new Dimension(300, 400));
      // FIXME?? "Move somewhere else"
      maxDepth_ = Math.max(10, defaultDepth_ + 4);
      ignore_ = true;     
      DefaultComboBoxModel cbm = (DefaultComboBoxModel)depthCombo_.getModel();
      cbm.removeAllElements();
      for (int i = 1; i <= maxDepth_; i++) {
        cbm.addElement(new Integer(i));
      }
      depthCombo_.setSelectedIndex(defaultDepth_ - 1);
      ignore_ = false;    
    } 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Handle network installation after the dialog is up and displayed
  */
  
  public void updateNetworkDisplay() {
    refreshPath(defaultDepth_, true, true);
    return;
  }  
  
  /***************************************************************************
  **
  ** Handle path change stuff
  */
  
  private void refreshPath(int depth, boolean doSelect, boolean longerOK) {

    sendRequest(depth, longerOK);
    labelNoPath(neverAPath_, crazyPath_);
    
    if (!neverAPath_) {
      if (longerOK) {
        maxDepth_ = Math.max(10, defaultDepth_ + 4);
        ignore_ = true;     
        DefaultComboBoxModel cbm = (DefaultComboBoxModel)depthCombo_.getModel();
        cbm.removeAllElements();
        for (int i = 1; i <= maxDepth_; i++) {
          cbm.addElement(new Integer(i));
        }
        depthCombo_.setSelectedIndex(defaultDepth_ - 1);
        ignore_ = false;
      }
      
      if (!requestPaths_.isEmpty()) {
        Layout requestedLo = requestFsb_.getLayoutSource().getLayout(layoutID_);
        lls_.setRootLayout(requestedLo);
        rcx_.setGenomeSource(requestFsb_.getGenomeSource());    
        rcx_.setGenome(rcx_.getGenomeSource().getGenome(phonyGenomeID_));        
        rcx_.setLayout(requestedLo);
        lws_.simpleSetWorkspace(requestFsb_.getWorkspaceSource().getWorkspace());
        requestedLo.recenterLayout(msp_.getRawCenterPoint(), rcx_, false, false, false, null, null, null, null, null);    
        msp_.getZoomController().zoomToModel();
      }
    }
    
    if (pathRanker_ != null) {
      pathRanker_.rankPaths(requestPaths_);
    }
    Collections.sort(requestPaths_);
    
    PathGenerator.StrungOutPath oldSel = selected_;
    ignore_ = true;
    listRenderer_.setValues(requestPaths_); 
    pathList_.setListData(requestPaths_.toArray());
    pathList_.clearSelection();
    ignore_ = false;
    myCard_.show(hidingPanel_, requestPaths_.isEmpty() ? "Hiding" : "Display");   
    if (doSelect) {
      selectTheExistingPath((oldSel != null) ? oldSel : null, requestPaths_);
    } 
    return;
  }  
  
  /***************************************************************************
  **
  ** Disable the controls
  */ 
  
  public void disableControls() { 
    depthCombo_.setEnabled(false);
    msp_.setEnabled(false);
    pathList_.setEnabled(false);
    return;
  }  
  
  /***************************************************************************
  **
  ** Reenable the controls
  */ 
  
  public void enableControls() {
    depthCombo_.setEnabled(true);
    msp_.setEnabled(true);
    pathList_.setEnabled(true);
    return;
  }  
   
  /***************************************************************************
  **
  ** Shutdown routine
  */
  
  public void unregister() {
    if (showQPCR_) {
      ToolTipManager.sharedInstance().unregisterComponent(msp_);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handles new data
  */   

  public void installNewData(LocalGenomeSource pSrc, String trueGenomeID, String srcID, String targID, Path matchPath) {
   
    srcID_ = srcID;
    targID_ = targID; 
    if (pSrc != null) {
      pSrc_ = pSrc; 
      truID_ = trueGenomeID;
    }
 
    ignore_ = true; 
    int currDepth = origDefaultDepth_;
    if (matchPath != null) {
      int depth = matchPath.getDepth();
      if (depth <= maxDepth_) {
        currDepth = depth;
      }
    }  
    depthCombo_.setSelectedIndex(currDepth - 1);
    ignore_ = false;
    
    refreshPath(currDepth, true, true);
    return;
  }
  
  /***************************************************************************
  **
  ** Select the given path
  */
  
  private void selectTheExistingPath(PathGenerator.StrungOutPath matchPath, List<PathGenerator.StrungOutPath> paths) {
    if (pathList_.getModel().getSize() > 0) {
      boolean wasSet = false;
      if (matchPath != null) {
        int numPa = paths.size();
        for (int i = 0; i < numPa; i++) {
          PathGenerator.StrungOutPath chkPath = paths.get(i);
          if (matchPath.equals(chkPath)) {
            pathList_.setSelectedIndex(i);
            wasSet = true;
            break;
          }
        }
      }
      if (!wasSet) {
        pathList_.setSelectedIndex(0);
      }
    } else {
      // FIXME?? Do something in this case?
    }
    return;
  }
 

  /***************************************************************************
  **
  ** Tweak the no path message
  */
  
  private void labelNoPath(boolean neverAPath, int crazyDepth) {
    ResourceManager rMan = appState_.getRMan();
    String noPath;
    if (neverAPath) {
      String form = rMan.getString("showPath.noPath");
      noPath = MessageFormat.format(form, new Object[] {new Integer(crazyDepth)});
    } else {
      noPath = rMan.getString("showPath.noPathAtDepth");      
    }    
    noPathLabel_.setText(noPath);
    noPathLabel_.invalidate();
    hidingPanel_.validate();
    return;
  }
  
  /***************************************************************************
  **
  ** Clear the display
  */   

  public void clear() {
    
    srcID_ = null;
    targID_ = null;
    ignore_ = true;
    rcx_.setGenome(null);
    msp_.repaint();

    int currDepth = origDefaultDepth_; 
    depthCombo_.setSelectedIndex(currDepth - 1);

    requestPaths_ = null;
    requestFsb_ =  null;
    requestTips_ = null;
    phonyGenomeID_ = null;
    layoutID_ = null;
 
    listRenderer_.setValues(new ArrayList<PathGenerator.StrungOutPath>());    
    pathList_.setListData(new Object[0]);
    pathList_.clearSelection();
  
    if (pathTracker_ != null) {
      pathTracker_.newPathSelected(new ArrayList<String>());
    }
       
    ignore_ = false;
    return;
  }

  /***************************************************************************
  **
  ** Center the path view
  */   

  public void centerView() {
    msp_.getZoomController().zoomToModel();
    return;
  }  
   
  /***************************************************************************
  **
  ** Handles selection changes
  */    
  
  public void valueChanged(ListSelectionEvent e) {
    try {
      if (ignore_) {
        return;
      }
      if (e.getValueIsAdjusting()) {
        return;
      }
      ArrayList<Intersection> selections = new ArrayList<Intersection>();
      selected_ = (PathGenerator.StrungOutPath)pathList_.getSelectedValue();
      ArrayList<String> currentPathList = new ArrayList<String>();
      if (selected_ != null) {
        Iterator<SignedTaggedLink> pit = selected_.pathIterator();
        while (pit.hasNext()) {
          SignedTaggedLink slink = pit.next();
          String linkID = slink.getTag();
          String source = slink.getSrc();          
          currentPathList.add(source);
          Set<LinkSegmentID> forLink = selected_.allSegs.get(linkID);
          Intersection linkInt = Intersection.intersectionForSegmentIDs(linkID, forLink);
          selections.add(linkInt);
        }
        String target = selected_.getEnd();
        currentPathList.add(target);
      }
      HashSet<String> currentPath = new HashSet<String>(currentPathList);
      msp_.selectNodesAndLinks(currentPath, selections);
      msp_.repaint();
      if (pathTracker_ != null) {
        pathTracker_.newPathSelected(currentPathList);
      }
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Register display tracker
  */    
  
  public void setPathTracker(PathDisplayTracker tracker) {
    pathTracker_ = tracker;
    return;
  }
  
  /***************************************************************************
  **
  ** Register path ranker
  */    
  
  public void setPathRanker(PathRanker ranker) {
    pathRanker_ = ranker;
    return;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Used for the data table renderer
  */
  
  class PathRenderer extends JLabel implements ListCellRenderer {
    
    private List<PathGenerator.StrungOutPath> values_;
    private ImageIcon[] icons_;
    private static final long serialVersionUID = 1L;
   
    PathRenderer(List<PathGenerator.StrungOutPath> values, ImageIcon[] icons) {
      super();
      values_ = values;
      icons_ = icons;
    }
    
    void setValues(List<PathGenerator.StrungOutPath> values) {
      values_ = values;
    }
    
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index,
                                                  boolean isSelected, boolean hasFocus) {
      try {
        if (value == null) {
          return (this);
        }
        //
        // Java Swing book says this may be needed for combo boxes (not lists):
        //
        if (index == -1) {
          index = list.getSelectedIndex();
          if (index == -1) {
            return (this);
          }
        }
        PathGenerator.StrungOutPath currPath = values_.get(index);
        int iconIndex;
        boolean avoid = (pathRanker_ != null) && (currPath.getRanking() > 1);
        
        if (avoid) {
          iconIndex = PathDisplay.AVOID_ICON_;
        } else {
          switch (currPath.getSign()) {
            case Linkage.NEGATIVE:
              iconIndex = PathDisplay.MINUS_ICON_;
              break;
            case Linkage.POSITIVE:
              iconIndex = PathDisplay.PLUS_ICON_;
              break;          
            case Linkage.NONE:
              iconIndex = PathDisplay.QUESTION_ICON_;
              break;          
            default:
              throw new IllegalArgumentException();
          }
        }
        setIcon(icons_[iconIndex]);
        setText(values_.get(index).display);
        setOpaque(true);
        setBackground((isSelected) ? list.getSelectionBackground() : list.getBackground());
        setForeground((avoid) ? Color.lightGray : Color.black);
        
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
      return (this);             
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
   /***************************************************************************
   **
   ** This is an independent frame, so it is sending off a control flow request (there is no
   ** currently running control flow):
   */
  
  public void sendRequest(int depth, boolean longerOK) {

    DesktopControlFlowHarness dcf = new DesktopControlFlowHarness(appState_, new DesktopDialogPlatform(appState_.getTopFrame()));
    ControlFlow myControlFlow = appState_.getFloM().getControlFlow(FlowMeister.OtherFlowKey.PATH_MODEL_GENERATION, null);
    DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());
    PathGenerator.StepState agis = (PathGenerator.StepState)myControlFlow.getEmptyStateForPreload(dacx);    
    if (pSrc_ != null) {
      // Legacy worksheet application
      agis.setLegacyData(pSrc_, truID_, srcID_, targID_, depth);
    } else {   
      agis.setSourceTargAndDepth(srcID_, targID_, depth, longerOK);
    }
    dcf.initFlow(myControlFlow, dacx);
    DialogAndInProcessCmd daipc = dcf.runFlow(agis);
    Map<String, Object> res = daipc.commandResults;
    
    
    boolean havePath = ((Boolean)res.get("havePath")).booleanValue();
    Integer maxPath = (Integer)res.get("maxPath");
    if (!havePath) {
      neverAPath_ = longerOK;
      crazyPath_ = maxPath;
      requestPaths_ = new ArrayList<PathGenerator.StrungOutPath>();
      return;
    } else {
      neverAPath_ = false;
    }

    requestPaths_ = (List<PathGenerator.StrungOutPath>)res.get("pathList");
    requestFsb_ = (FreestandingSourceBundle)res.get("pathModel");
    // May be null:
    requestTips_ = (PathGenerator.TipSource)res.get("pathTips");
    phonyGenomeID_ = (String)res.get("pathModelID");
    layoutID_ = (String)res.get("pathLayoutID");
    defaultDepth_ = maxPath;
    
    maxDepth_ = Math.max(10, defaultDepth_ + 4);
    return;
  }

  /***************************************************************************
  **
  ** Get the tool tip
  */
   
  public String generateTip(Intersection inter) {
    if (!showQPCR_) {
      return (null);
    }  
    //
    // If node, we are done:
    //
    
    String objID = inter.getObjectID();
    String nodeTip = requestTips_.nodeTips.get(objID);
    if (nodeTip != null) {
      return (nodeTip);
    }
    String linkTip = requestTips_.linkTips.get(objID);
    if (linkTip == null) {
      return (null);
    }
    String srcID = requestTips_.linkToSrc.get(objID);
    Map<LinkSegmentID, Set<String>> mToLinks = requestTips_.linkSegMaps.get(srcID);
    LinkSegmentID lsid = inter.segmentIDFromIntersect();
    Set<String> linksThruSeg = mToLinks.get(lsid);
    int numLinks = linksThruSeg.size();
    if (numLinks == 1) {
      return (requestTips_.linkTips.get(linksThruSeg.iterator().next()));
    }
    HashSet<String> intersect = new HashSet<String>();
    if (selected_ != null) {
      Iterator<SignedTaggedLink> pit = selected_.pathIterator();
      HashSet<String> selLinks = new HashSet<String>();
      while (pit.hasNext()) {
        SignedTaggedLink plink = pit.next();
        selLinks.add(plink.getTag());
      } 
      intersect.addAll(linksThruSeg);
      intersect.retainAll(selLinks);
    }
    if (intersect.size() == 1) {
      return (requestTips_.linkTips.get(intersect.iterator().next())); 
    }
    return (requestTips_.ambiguousTips.get(srcID));
  } 
  
  /***************************************************************************
  **
  ** Get the tool tip. Note this is the required interfase, but we no longer 
  ** use the genome or layout for this detached client component.
  */
   
  public String generateTip(Genome genome, Layout layout, Intersection inter) {
    return (generateTip(inter));
  } 
}
