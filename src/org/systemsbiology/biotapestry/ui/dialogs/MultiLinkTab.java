/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.JPanel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.TitledBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.PerLinkDrawStyle;
import org.systemsbiology.biotapestry.ui.SuggestedDrawStyle;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ColorDeletionListener;
import org.systemsbiology.biotapestry.util.ColorSelectionWidget;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.ui.DisplayOptions;

/****************************************************************************
**
** A tab for editing multiple nodes at once
*/

public class MultiLinkTab implements ColorDeletionListener {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////    
     
  private SuggestedDrawStylePanel sdsPan_;
  private JComboBox extentCombo_;
  private NodeAndLinkPropertiesSupport nps_;
  private DataAccessContext dacx_;
  
  private JComboBox evidenceCombo_;
  private JComboBox signCombo_;
  private Set<String> links_;
  private List<ColorDeletionListener> colorListeners_;
  private SuggestedDrawStyle changedProps_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** Constructor
  ** 
  */
  
  public MultiLinkTab(BTState appState, DataAccessContext dacx, Set<String> links, 
                      List<ColorDeletionListener> cdls) {
    links_ = links;
    appState_ = appState;
    dacx_ = dacx;
    nps_ = new NodeAndLinkPropertiesSupport(appState_, dacx_);
    colorListeners_ = cdls;
    colorListeners_.add(this);
    changedProps_ = new SuggestedDrawStyle("red");
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public void colorReplacement(Map<String, String> oldToNew) {
    Iterator<String> otnit = oldToNew.keySet().iterator();
    while (otnit.hasNext()) {
      String oldColor = otnit.next();
      String newColor = oldToNew.get(oldColor);
      changedProps_.setColorName(newColor);
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Build a tab 
  ** 
  */

  public JPanel buildLinkTab(boolean haveStatInstance, boolean haveDynInstance,
                             ConsensusLinkProps gcp, ImageIcon warnIcon) {

    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();      
    ResourceManager rMan = appState_.getRMan();

    int row = 0;
    JPanel modelPanel = null;
    if (!haveDynInstance) {
      modelPanel = new JPanel();
      modelPanel.setLayout(new GridBagLayout());
      modelPanel.setBorder(new TitledBorder(rMan.getString("multiSelProps.model")));
      UiUtil.gbcSet(gbc, 0, row++, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      retval.add(modelPanel, gbc);
    }

    JPanel layoutPanel = new JPanel();
    layoutPanel.setLayout(new GridBagLayout());
    layoutPanel.setBorder(new TitledBorder(rMan.getString("multiSelProps.layout")));
    UiUtil.gbcSet(gbc, 0, row++, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(layoutPanel, gbc);

    int modelRownum = 0;
    int layoutRownum = 0;

    //
    // Evidence levels:
    //

    if (!haveDynInstance) {
      if ((gcp.evidenceCoverage != ConsensusLinkProps.UNDEFINED_OPTION_COVERAGE) && 
          (gcp.evidenceCoverage != ConsensusLinkProps.NO_OPTION_COVERAGE)) {   
        Vector<ChoiceContent> eviOpts = DBLinkage.getEvidenceChoices(appState_);
        eviOpts.add(0, new ChoiceContent(rMan.getString("multiSelProps.various"), LinkageInstance.LEVEL_VARIOUS));
        evidenceCombo_ = new JComboBox(eviOpts);    

        JLabel label = new JLabel(rMan.getString("multiSelProps.evidence"));
        UiUtil.gbcSet(gbc, 0, modelRownum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
        modelPanel.add(label, gbc);

        UiUtil.gbcSet(gbc, 1, modelRownum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
        modelPanel.add(evidenceCombo_, gbc);
      }

      //
      // Signs:
      //

      if ((gcp.signCoverage != ConsensusLinkProps.UNDEFINED_OPTION_COVERAGE) && 
          (gcp.signCoverage != ConsensusLinkProps.NO_OPTION_COVERAGE)) {   
        Vector<ChoiceContent> signOpts = DBLinkage.getSignChoices(appState_);
        signOpts.add(0, new ChoiceContent(rMan.getString("multiSelProps.various"), LinkageInstance.SIGN_VARIOUS));
        signCombo_ = new JComboBox(signOpts);    

        JLabel label = new JLabel(rMan.getString("multiSelProps.sign"));
        UiUtil.gbcSet(gbc, 0, modelRownum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
        modelPanel.add(label, gbc);

        UiUtil.gbcSet(gbc, 1, modelRownum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
        modelPanel.add(signCombo_, gbc);
      }

      //
      // Activity setting:
      //

      if (haveStatInstance) {
        JPanel activ = nps_.activityLevelUI(true, true);     
        if (activ != null) {
          UiUtil.gbcSet(gbc, 0, modelRownum++, 11, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 0.0);
          modelPanel.add(activ, gbc);
        }
      }
    }
   
    //
    // Draw Style:
    //
           
    sdsPan_ = new SuggestedDrawStylePanel(appState_, dacx_, true, false, true, colorListeners_);          
    UiUtil.gbcSet(gbc, 0, layoutRownum, 8, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    layoutPanel.add(sdsPan_, gbc);   
    layoutRownum += 8;
    
    JLabel comboLabel = new JLabel(rMan.getString("perLinkSpecial.extent"));
    Vector<ChoiceContent> extentChoices = PerLinkDrawStyle.getExtentChoices(appState_);
    extentChoices.add(0, new ChoiceContent(rMan.getString("multiSelProps.various"), PerLinkDrawStyle.EXTENT_VARIOUS));
    extentCombo_ = new JComboBox(extentChoices);
    
    UiUtil.gbcSet(gbc, 0, layoutRownum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    layoutPanel.add(comboLabel, gbc);
    UiUtil.gbcSet(gbc, 1, layoutRownum++, 7, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    layoutPanel.add(extentCombo_, gbc);

    //
    // Warning label, if needed:
  

  //  if ((gcp.hideLabelCoverage == ConsensusProps.PARTIAL_OPTION_COVERAGE)) {
 //     JLabel warnLabel = new JLabel(rMan.getString("multiSelProps.warnPartial"), warnIcon, JLabel.CENTER);
   //   UiUtil.gbcSet(gbc, 0, row, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
  //    retval.add(warnLabel, gbc); 
  //  }   
 
    return (retval);     
  }

  /***************************************************************************
  **
  ** Apply the current link property values to our UI components
  ** 
  */

  public void displayForTab(ConsensusLinkProps clp, boolean haveStatInstance, boolean haveDynamicInstance) {

    //
    // Now do the settings:
    //

    SuggestedDrawStyle sds = new SuggestedDrawStyle(clp.consensusStyle, clp.consensusColor, clp.consensusThickness);
    sdsPan_.displayMultiProperties(sds, clp);
  
    if (!haveDynamicInstance) {
      if (evidenceCombo_ != null) {
        if (clp.consensusEvidence == Linkage.LEVEL_VARIOUS) {
          evidenceCombo_.setSelectedIndex(0);
        } else {          
          evidenceCombo_.setSelectedItem(DBLinkage.evidenceTypeForCombo(appState_, clp.consensusEvidence));
        }
      }

      if (signCombo_ != null) {
        if (clp.consensusSign == Linkage.SIGN_VARIOUS) {
          signCombo_.setSelectedIndex(0);
        } else {          
          signCombo_.setSelectedItem(DBLinkage.signForCombo(appState_, clp.consensusSign));
        }
      }

      if (haveStatInstance) {
        nps_.setActivityLevelForMulti(clp.consensusActivity, true);
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Check for errors
  ** 
  */

  public boolean errorCheckForTab() { 
   
    SuggestedDrawStylePanel.QualifiedSDS qsds = sdsPan_.getChosenStyle();
    if (!qsds.isOK) {
      return (false);
    }
    
    //
    // Have to error check all activity changes before making the changes:
    // 
   
    boolean haveDynInstance = (dacx_.getGenome() instanceof DynamicGenomeInstance);
    boolean haveStatInstance = (dacx_.getGenome() instanceof GenomeInstance) && !haveDynInstance;
    
    if (haveStatInstance) {
      GenomeInstance gi = dacx_.getGenomeAsInstance();
      Iterator<String> nit = links_.iterator();
      while (nit.hasNext()) {
        String linkID = nit.next();     
        LinkageInstance li = (LinkageInstance)dacx_.getGenome().getLinkage(linkID);
        //
        // Same problem as Issue #167: VFNs can have null entries here:
        //
        if (li == null) {
          continue;
        }
        GenomeItemInstance.ActivityState newLiAs = nps_.getActivityLevel();
        if (newLiAs == null) {
          throw new IllegalStateException();
        }
        if (newLiAs.activityState != LinkageInstance.ACTIVITY_NOT_SET) {       
          int oldActivity = li.getActivity(gi);
          boolean levelChanged = false;
          if (newLiAs.activityState == LinkageInstance.VARIABLE) {
            if (oldActivity == LinkageInstance.VARIABLE) {
              double oldLevel = li.getActivity(gi);
              levelChanged = (oldLevel != newLiAs.activityLevel.doubleValue());
            }
          }
          if ((oldActivity != newLiAs.activityState) || levelChanged) {
            GenomeItemInstance.ActivityTracking tracking = li.calcActivityBounds(dacx_.getGenomeAsInstance());
            // FIX FOR BT-12-15-11:3
            double newLevel = (newLiAs.activityState == LinkageInstance.VARIABLE) ? newLiAs.activityLevel.doubleValue() : Double.NEGATIVE_INFINITY;            
            if (!nps_.checkActivityBounds(tracking, newLiAs.activityState, newLevel)) {
              return (false);
            }
          }
        }
      }
    }

    //
    // Before application, we need to see if any link with drawing style set is going to
    // be overridden by custom evidence levels
    //
    //
    // Issue 150: If there are links set to thick because of evidence, this test triggers, even if nothing changed in this regard.
    //

    DisplayOptions dOpt = appState_.getDisplayOptMgr().getDisplayOptions();
    Iterator<String> nit = links_.iterator();
    while (nit.hasNext()) {
      String linkID = nit.next();
      Linkage link = dacx_.getGenome().getLinkage(linkID);
        //
        // Same problem as Issue #167: VFNs can have null entries here:
        //
        if (link == null) {
          continue;
        }
      
      int evidence = link.getTargetLevel(); 
      if (evidenceCombo_ != null) {
        int newEvidence = (evidenceCombo_ != null) ? ((ChoiceContent)evidenceCombo_.getSelectedItem()).val : evidence;
        evidence = (newEvidence != Linkage.LEVEL_VARIOUS) ? newEvidence : evidence;
      }
      PerLinkDrawStyle plfe = dOpt.getEvidenceDrawChange(evidence);
      if (plfe != null) {
        ResourceManager rMan = appState_.getRMan();
        int ok = JOptionPane.showConfirmDialog(appState_.getTopFrame(), rMan.getString("multiLinkTab.customOverride"),
                                               rMan.getString("multiLinkTab.customOverrideTitle"),
                                               JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
          return (false);
        }
        break;
      }
    }
    return (true);
  }

  /***************************************************************************
  **
  ** Apply our UI values to all the link properties
  ** 
  */

  public boolean applyProperties(UndoSupport support) { 
        
    SuggestedDrawStylePanel.QualifiedSDS qsds = sdsPan_.getChosenStyle();
    if (!qsds.isOK) {
      throw new IllegalStateException();  // Already checked...
    }
        
    HashMap<String, DrawStyle> holdDS = new HashMap<String, DrawStyle>();
    boolean haveDynInstance = (dacx_.getGenome() instanceof DynamicGenomeInstance);
    boolean haveStatInstance = (dacx_.getGenome() instanceof GenomeInstance) && !haveDynInstance;   
    boolean instanceChange = false;
    boolean modelChange = false;
    boolean layoutChange = false;
    Iterator<String> nit = links_.iterator();
    while (nit.hasNext()) {
      String linkID = nit.next();
      Linkage link = dacx_.getGenome().getLinkage(linkID);
      //
      // Same problem as Issue #167: VFNs can have null entries here:
      //
      if (link == null) {
        continue;
      }
      //
      // Activity:
      //
      
      if (haveStatInstance) {
        GenomeInstance gi = dacx_.getGenomeAsInstance();
        LinkageInstance li = (LinkageInstance)link;
        GenomeItemInstance.ActivityState newLiAs = nps_.getActivityLevel();
        if (newLiAs == null) {
          throw new IllegalStateException();
        }
        if (newLiAs.activityState != LinkageInstance.ACTIVITY_NOT_SET) {       
          int oldActivity = li.getActivity(gi);
          boolean levelChanged = false;
          if (newLiAs.activityState == LinkageInstance.VARIABLE) {
            if (oldActivity == LinkageInstance.VARIABLE) {
              double oldLevel = li.getActivity(gi);
              levelChanged = (oldLevel != newLiAs.activityLevel.doubleValue());
            }
          }
          if ((oldActivity != newLiAs.activityState) || levelChanged) {
            double varLevel = (newLiAs.activityState == LinkageInstance.VARIABLE) ? newLiAs.activityLevel.doubleValue() : 0.0;   
            GenomeChange gc = gi.replaceLinkageInstanceActivity(linkID, newLiAs.activityState, varLevel);
            if (gc != null) {
              GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
              support.addEdit(gcc);
              instanceChange = true;
            }          
          }
        }
      }
 
      //
      // Evidence & sign (model props):
      //
      
      if (!haveDynInstance) {
        if ((evidenceCombo_ != null) || (signCombo_ != null)) {
          int oldEvidence = link.getTargetLevel();
          int newEvidence = (evidenceCombo_ != null) ? ((ChoiceContent)evidenceCombo_.getSelectedItem()).val : oldEvidence;
          newEvidence = (newEvidence != Linkage.LEVEL_VARIOUS) ? newEvidence : oldEvidence;
          int oldSign = link.getSign();
          int newSign = (signCombo_ != null) ? ((ChoiceContent)signCombo_.getSelectedItem()).val : oldSign;
          newSign = (newSign != Linkage.SIGN_VARIOUS) ? newSign : oldSign;

          if ((newEvidence != oldEvidence) || (newSign != oldSign)) {
            String oldName = link.getName();
            GenomeChange gc = dacx_.getGenome().replaceLinkageProperties(linkID, oldName, newSign, newEvidence);
            if (gc != null) {
              GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
              support.addEdit(gcc);
              modelChange = true;
            }
          }
        }
      }
   
      //
      // Layout (suggested draw style):
      //
      boolean didChange = collectLinkChanges(dacx_.getLayout(), linkID, qsds, holdDS);
      layoutChange = layoutChange || didChange; 
    }
        
    if (layoutChange) {
      boolean doSig = false;
      Map<String, ChangePlan> changePlan = generateLayoutChangePlan(dacx_.getLayout(), holdDS);
      Iterator<String> cpkit = changePlan.keySet().iterator();
      while (cpkit.hasNext()) {
        String linkID = cpkit.next();
        ChangePlan cp = changePlan.get(linkID);
        Layout.PropChange[] lpc = new Layout.PropChange[1];    
        lpc[0] = dacx_.getLayout().replaceLinkProperties(cp.oldProps, cp.newProps);
        if (lpc[0] != null) {
          PropChangeCmd mov = new PropChangeCmd(appState_, dacx_, lpc);
          support.addEdit(mov);
          doSig = true;
        }       
      }
      if (doSig) {
        LayoutChangeEvent ev = new LayoutChangeEvent(dacx_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(ev);
      }
    } 
    
    if (modelChange) {
      ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getDBGenome().getID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(mcev);
    }
    if (instanceChange) {
      ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(mcev);
    }
    
    return (true);
  }
 
 
  /***************************************************************************
  **
  ** Collect up link changes
  ** 
  */

  private boolean collectLinkChanges(Layout layout, String linkID, SuggestedDrawStylePanel.QualifiedSDS qsds, Map<String, DrawStyle> holdDS) { 

    LinkProperties lp = layout.getLinkProperties(linkID);
    DrawStyle workingStyle = drawStyleForLink(lp, linkID, true);
    holdDS.put(linkID, workingStyle);     
    SuggestedDrawStyle stylePart = workingStyle.getStylePart();

    int desiredStyle = qsds.sds.getLineStyle();
    if (desiredStyle != SuggestedDrawStyle.VARIOUS_STYLE) {
      if (stylePart.getLineStyle() != desiredStyle) {
        stylePart.setLineStyle(desiredStyle);
        workingStyle.modified = true;
      }
    }

    int desiredThickness = qsds.sds.getThickness();
    if (desiredThickness != SuggestedDrawStyle.VARIOUS_THICKNESS) {
      if (stylePart.getThickness() != desiredThickness) {
        stylePart.setThickness(desiredThickness);
        workingStyle.modified = true;
      }
    }

    String desiredColor = qsds.sds.getColorName();
    if (!desiredColor.equals(ColorSelectionWidget.VARIOUS_TAG)) {
      if (!stylePart.getColorName().equals(desiredColor)) {
        stylePart.setColorName(desiredColor);
        workingStyle.modified = true;
      }
    }

    if (workingStyle.plds != null) {        
      if (extentCombo_ != null) {
        int oldExtent = workingStyle.plds.getExtent();
        int newExtent = ((ChoiceContent)extentCombo_.getSelectedItem()).val;
        newExtent = (newExtent != PerLinkDrawStyle.EXTENT_VARIOUS) ? newExtent : oldExtent;        
        if (newExtent != oldExtent) {
          workingStyle.plds.setExtent(newExtent);
          workingStyle.modified = true;
        }
      }
    }
    return (workingStyle.modified);
  }

  /***************************************************************************
  **
  ** Generate a combined draw style for a link
  ** 
  */

  private DrawStyle drawStyleForLink(LinkProperties lp, String linkID, boolean selected) { 
    DrawStyle ds;
    SuggestedDrawStyle linkBase = lp.getDrawStyle();
    PerLinkDrawStyle plds = lp.getDrawStyleForLinkage(linkID);
    if (plds == null) {
      ds = new DrawStyle(linkBase.clone(), linkID, selected);       
    } else {
      SuggestedDrawStyle combined = ConsensusLinkProps.completeDrawStyleForLink(linkBase, plds);
      PerLinkDrawStyle pldsCo = new PerLinkDrawStyle(combined, plds.getExtent());
      ds = new DrawStyle(pldsCo, linkID, selected);
    } 
    return (ds);
  }
  
  /***************************************************************************
  **
  ** Figure out what we are changing for layout
  ** 
  */

  private Map<String, ChangePlan> generateLayoutChangePlan(Layout layout, Map<String, DrawStyle> holdDS) { 
  
    HashMap<String, ChangePlan> changePlan = new HashMap<String, ChangePlan>();
    
    //
    // Figure out how the changes map to the full tree settings or per link
    // settings by finding out the coverage of our current set of links:
    //
    
    HashSet<Set<String>> sharedItems = new HashSet<Set<String>>();    
    Iterator<String> nit = links_.iterator();
    while (nit.hasNext()) {
      String linkID = nit.next();
      Set<String> shared = layout.getSharedItems(linkID);
      sharedItems.add(shared);
    }
    
    //
    // Gather up all the styles that apply to each link tree,
    // which are identified by their share set:
    //
    
    HashMap<Set<String>, List<DrawStyle>> sharedItemsToAllStyles = new HashMap<Set<String>, List<DrawStyle>>();
    Iterator<Set<String>> siit = sharedItems.iterator();
    while (siit.hasNext()) {
      Set<String> shared = siit.next();
      Iterator<String> sit = shared.iterator();
      while (sit.hasNext()) {
        String linkID = sit.next();
        DrawStyle ds = holdDS.get(linkID);
        if (ds == null) {  // Not part of the original link selection!
          LinkProperties lp = layout.getLinkProperties(linkID); 
          ds = drawStyleForLink(lp, linkID, false);
        }
        List<DrawStyle> allSty = sharedItemsToAllStyles.get(shared);
        if (allSty == null) {
          allSty = new ArrayList<DrawStyle>();
          sharedItemsToAllStyles.put(shared, allSty);
        }
        allSty.add(ds);          
      }
    }
    
    //
    // Figure out the most numerous style, and make that the 
    // link style:
    //
        
    Map<Set<String>, DrawStyle> baseStyles = calcBaseLinkStyle(sharedItemsToAllStyles);    
    
    //
    // If everybody on the list shares the same suggested style, it
    // collapses into the single tree style.  If not, if the full
    // tree styles all match, we keep that as the full tree style.
    // Otherwise, things are going to get messy:
    //

    int useExtent = ((ChoiceContent)extentCombo_.getSelectedItem()).val;
    useExtent = (useExtent != PerLinkDrawStyle.EXTENT_VARIOUS) ? useExtent : PerLinkDrawStyle.SHARED_CONGRUENT; 
      
    Iterator<Set<String>> si2asit = sharedItemsToAllStyles.keySet().iterator();
    while (si2asit.hasNext()) {
      Set<String> shared = si2asit.next();
      String aLinkID = shared.iterator().next();
      BusProperties bp = layout.getLinkProperties(aLinkID);
      BusProperties newProps = bp.clone();
      changePlan.put(aLinkID, new ChangePlan(bp, newProps));
      List<DrawStyle> allSty = sharedItemsToAllStyles.get(shared);
      int nas = allSty.size();
      DrawStyle base = baseStyles.get(shared);
      SuggestedDrawStyle baseSds = base.getStylePart();
      newProps.setDrawStyle(baseSds);      
      for (int i = 0; i < nas; i++) {
        DrawStyle ds = allSty.get(i);
        if (!ds.getStylePart().equals(baseSds)) {
          if (ds.plds != null) {
            newProps.setDrawStyleForLinkage(ds.linkID, minimizePerLinkStyle(baseSds, ds.plds));
          } else {
            PerLinkDrawStyle pldsCo = minimizePerLinkStyle(baseSds, new PerLinkDrawStyle(ds.getStylePart(), useExtent));
            newProps.setDrawStyleForLinkage(ds.linkID, pldsCo);
          }
        } else {
          if (bp.getDrawStyleForLinkage(ds.linkID) != null) {
            newProps.setDrawStyleForLinkage(ds.linkID, null);
          }
        }
      }
    }
    
    return (changePlan);
  }
 
  /***************************************************************************
  **
  ** Minimize the per-link style to just what is needed:
  ** 
  */

  private PerLinkDrawStyle minimizePerLinkStyle(SuggestedDrawStyle sds, PerLinkDrawStyle plds) { 
    SuggestedDrawStyle pldsSty = plds.getDrawStyle();
    int style = (pldsSty.getLineStyle() == sds.getLineStyle()) ? SuggestedDrawStyle.NO_STYLE : pldsSty.getLineStyle();
    int thickness = (pldsSty.getThickness() == sds.getThickness()) ? SuggestedDrawStyle.NO_THICKNESS_SPECIFIED : pldsSty.getThickness();
    String color = (pldsSty.getColorName().equals(sds.getColorName())) ? null : pldsSty.getColorName();
    
    SuggestedDrawStyle minSds = new SuggestedDrawStyle(style, color, thickness);
    return (new PerLinkDrawStyle(minSds, plds.getExtent()));
  }
  
  
  /***************************************************************************
  **
  ** Figure out what to use as the base style for all the trees
  ** 
  */

  private Map<Set<String>, DrawStyle> calcBaseLinkStyle(Map<Set<String>, List<DrawStyle>> sharedItemsToAllStyles) { 
  
    HashMap<Set<String>, DrawStyle> retval = new HashMap<Set<String>, DrawStyle>();

    Iterator<Set<String>> si2asit = sharedItemsToAllStyles.keySet().iterator();
    while (si2asit.hasNext()) {
      Set<String> shared = si2asit.next();
      List<DrawStyle> allSty = sharedItemsToAllStyles.get(shared);
      DrawStyle base = calcBaseLinkStylePerTree(shared, allSty);
      retval.put(shared, base);
    }
    return (retval);    
  }
  
  /***************************************************************************
  **
  ** Figure out the base style for one tree
  ** 
  */

  private DrawStyle calcBaseLinkStylePerTree(Set<String> shared, List<DrawStyle> allSty) { 
  
    //
    // If we are just modifying a subset of all the links in a tree, then
    // we want to make those changes be link-only.  Exception is if the
    // changes converge to the base style, in which case we drop all the
    // relevant per-link changes.  Also, exception if that means nobody
    // uses the base style anymore.
    //
    // If a full link is being chosen, then all per-link styles are dropped.
    //
    // OK, what if only some things (e.g. linestyle) are synched, but others
    // are not??  What to do?
      
    int numLink = shared.size();
    int numSty = allSty.size();
    if ((numLink == 1) || (numSty == 1)) {
      return (allSty.get(0));
    }
    DrawStyle first = allSty.get(0);
    boolean allSame = true;
    
    for (int i = 0; i < numSty; i++) {
      DrawStyle chk = allSty.get(i);
      if (!chk.selected && (chk.plds == null)) {
        return (chk);
      }
      if (allSame) {
        allSame = first.getStylePart().equals(chk.getStylePart());
      }
    }
    
    if (allSame) {
      return (first);
    }
    
    Integer zero = new Integer(0);
    int maxCount = 0;
    int maxIndex = -1;
    HashMap<SuggestedDrawStyle, Integer> countPerSty = new HashMap<SuggestedDrawStyle, Integer>();
    for (int i = 0; i < numSty; i++) {
      DrawStyle chk = allSty.get(i);
      SuggestedDrawStyle sds = chk.getStylePart();
      Integer count = countPerSty.get(sds);
      if (count == null) {
        count = zero;
      }
      int newCount = count.intValue() + 1;
      countPerSty.put(sds, new Integer(newCount));
      if (newCount > maxCount) {
        maxCount = newCount;
        maxIndex = i;
      }
    }
    return (allSty.get(maxIndex));      
  }
  

   
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static class DrawStyle {    
 
    PerLinkDrawStyle plds;
    SuggestedDrawStyle sds;
    boolean modified;
    String linkID;
    boolean selected;
  
    DrawStyle(PerLinkDrawStyle plds, String linkID, boolean selected) {
      this.plds = plds;
      this.linkID = linkID;
      this.selected = selected;
    }
    
    DrawStyle(SuggestedDrawStyle sds, String linkID, boolean selected) {
      this.sds = sds;
      this.linkID = linkID;
      this.selected = selected;
    }
    
    SuggestedDrawStyle getStylePart() {
      return ((sds != null) ? sds : plds.getDrawStyle());
    }
  }
  
  private static class ChangePlan {    
    BusProperties oldProps;
    BusProperties newProps;
  
    ChangePlan(BusProperties oldProps, BusProperties newProps) {
      this.oldProps = oldProps;
      this.newProps = newProps;
    }
  }
}    
