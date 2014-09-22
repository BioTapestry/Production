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

import java.awt.Dimension;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;

/****************************************************************************
**
** Factory for dialog boxes for editing properties of a network overlay
*/

public class NetOverlayPropertiesDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public NetOverlayPropertiesDialogFactory(ServerControlFlowHarness cfh) {
    super(cfh);
  }  
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // FACTORY METHOD
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Get the appropriate dialog
  */  
  
  public ServerControlFlowHarness.Dialog getDialog(DialogBuildArgs ba) { 
   
    NetOverlayPropsArgs dniba = (NetOverlayPropsArgs)ba;
  
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.name, dniba.desc, dniba.hideLinks, dniba.currType, 
                                dniba.existingNames, dniba.typeChoices));
    } else if (platform.getPlatform() == DialogPlatform.Plat.WEB) {
      throw new IllegalStateException();
    }
    throw new IllegalArgumentException();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // COMMON HELPERS
  //
  ////////////////////////////////////////////////////////////////////////////      

  ////////////////////////////////////////////////////////////////////////////
  //
  // BUILD ARGUMENT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class NetOverlayPropsArgs extends DialogBuildArgs { 
    
    Set<String> existingNames;
    Vector<TrueObjChoiceContent> typeChoices;
    String name;
    String desc;
    NetOverlayProperties.OvrType currType;
    boolean hideLinks;
    
          
    public NetOverlayPropsArgs(BTState appState) {
      super(null);      
      Database db = appState.getDB();
      Layout layout = db.getLayout(appState.getLayoutKey());
      NetOverlayOwner owner = db.getOverlayOwnerFromGenomeKey(appState.getGenome());
      NetworkOverlay novr = owner.getNetworkOverlay(appState.getCurrentOverlay());
      NetOverlayProperties nop = layout.getNetOverlayProperties(appState.getCurrentOverlay());
      
      existingNames = new HashSet<String>();
      Iterator<NetworkOverlay> oit = owner.getNetworkOverlayIterator();
      while (oit.hasNext()) {
        NetworkOverlay no = oit.next();
        if (no != novr) {
          existingNames.add(no.getName());
        }
      }
 
      typeChoices = NetOverlayProperties.getOverlayTypes(appState);
      name = novr.getName(); 
      desc = novr.getDescription();  
      hideLinks = nop.hideLinks();    
      currType = nop.getType();  
    } 
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // DIALOG CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class DesktopDialog extends BTTransmitResultsDialog { 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JTextField nameField_;
  private JComboBox typeCombo_;
  private JCheckBox hideLinksBox_;
  private JTextField descField_;
  private Set<String> existingNames_;
  private String oldName_;
  private String oldDesc_;
  private NetOverlayProperties.OvrType origType_;
  private boolean origHide_;
  
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
  
  public DesktopDialog(ServerControlFlowHarness cfh, String name, String desc, boolean hideLinks, 
                       NetOverlayProperties.OvrType currType, 
                       Set<String> existingNames, Vector<TrueObjChoiceContent> typeChoices) {      
    super(cfh, "ovrprop.title", new Dimension(900, 300), 10, new NetOverlayPropsRequest(), false);
    existingNames_ = existingNames;
    oldName_ = name;
    oldDesc_ = desc;
    origType_ = currType;
    origHide_ = hideLinks;
 
    //
    // Build the name panel:
    //

    JLabel label = new JLabel(rMan_.getString("ovrprop.name"));
    nameField_ = new JTextField();
    if (name != null) {
      nameField_.setText(name);
    }
    addLabeledWidget(label, nameField_, false, false);
 
    //
    // Add the presentation type:
    //
    
    JLabel tlabel = new JLabel(rMan_.getString("ovrprop.typeLabel"));
    typeCombo_ = new JComboBox(typeChoices);
    typeCombo_.setSelectedItem(NetOverlayProperties.typeForCombo(appState_, currType));
    addLabeledWidget(tlabel, typeCombo_, false, false);
  
    //
    // Add the hide links option:
    //
  
    hideLinksBox_ = new JCheckBox(rMan_.getString("ovrprop.hideLinks"));
    hideLinksBox_.setSelected(hideLinks);
    addWidgetFullRow(hideLinksBox_, false, true);
     
    //
    // Add the overlay description:
    //
      
    label = new JLabel(rMan_.getString("ovrprop.desc"));
    descField_ = new JTextField();
    if (desc != null) {
      descField_.setText(desc);
    }
    addLabeledWidget(label, descField_, false, false);
  
    finishConstruction();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Do the bundle 
  ** 
  */
    
   @Override   
   protected boolean bundleForExit(boolean forApply) {
     NetOverlayPropsRequest nocrq = (NetOverlayPropsRequest)request_;           
   
     nocrq.submitModel = false;
     String newName = nameField_.getText().trim();
     nocrq.nameResult = oldName_;
     if (!newName.equals(oldName_)) {
       if (nocrq.nameResult.equals("")) {
         String message = rMan_.getString("noverlay.emptyName"); 
         String title = rMan_.getString("noverlay.emptyNameTitle");
         SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
         cfh_.showSimpleUserFeedback(suf);
         return (false);
       }      
          
       if (DataUtil.containsKey(existingNames_, nocrq.nameResult)) {
         String message = rMan_.getString("noverlay.dupName"); 
         String title = rMan_.getString("noverlay.dupNameTitle");
         SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
         cfh_.showSimpleUserFeedback(suf);
         return (false);
       }             
       nocrq.submitModel = true;       
       nocrq.nameResult = newName;
     }
     
     String newDesc = descField_.getText().trim();
     nocrq.descResult = oldDesc_;
     if (!newDesc.equals(oldDesc_)) {
       nocrq.submitModel = true;     
       nocrq.descResult = newDesc;
     }
     
     nocrq.submitLayout = false;
     nocrq.typeResult = origType_;
     nocrq.hideLinksResult = origHide_;
     TrueObjChoiceContent typeSelection = (TrueObjChoiceContent)typeCombo_.getSelectedItem();
     NetOverlayProperties.OvrType typeResult = (NetOverlayProperties.OvrType)typeSelection.val;
     boolean hideResult = hideLinksBox_.isSelected();
     if ((typeResult != origType_) || (hideResult != origHide_)) {
       nocrq.typeResult = typeResult;
       nocrq.hideLinksResult = hideLinksBox_.isSelected();
       nocrq.submitLayout = true;
     }
     nocrq.startHiding = ((nocrq.hideLinksResult != origHide_) && nocrq.hideLinksResult);
         
     nocrq.haveResult = nocrq.submitModel || nocrq.submitLayout;
     return (true);
   }
 }
 
 /***************************************************************************
 **
 ** Return Results
 ** 
 */

 public static class NetOverlayPropsRequest implements ServerControlFlowHarness.UserInputs {   
   public boolean submitModel;
   public boolean submitLayout;
   private boolean haveResult;
   public String nameResult;
   public String descResult;
   public NetOverlayProperties.OvrType typeResult;
   public boolean hideLinksResult;
   public boolean startHiding;

   public void clearHaveResults() {
     haveResult = false;
     return;
   }   
   public boolean haveResults() {
     return (haveResult);
   }  
   public boolean isForApply() {
     return (false);
   }
 }
}
