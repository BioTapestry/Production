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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Note;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.NoteProperties;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.ui.xplat.dialog.XPlatUIDialog;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ColorSelectionWidget;
import org.systemsbiology.biotapestry.util.MultiLineRenderSupport;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Factory for dialog boxes for creating and editing notes
*/

public class NotePropertiesDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public NotePropertiesDialogFactory(ServerControlFlowHarness cfh) {
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
   
    NotePropBuildArgs dniba = (NotePropBuildArgs)ba;
  
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.noteToEdit, dniba.nprops));
    } else if (platform.getPlatform() == DialogPlatform.Plat.WEB) {
      return (new SerializableDialog(cfh, dniba.noteToEdit, dniba.nprops));   
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
  
  public static class NotePropBuildArgs extends DialogBuildArgs { 
    
    Note noteToEdit;
    NoteProperties nprops; 
          
    public NotePropBuildArgs(Note noteToEdit, NoteProperties nprops) {
      super(null);
      this.noteToEdit = noteToEdit;
      this.nprops = nprops;
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
   
    private JTextArea nameField_;
    private JLabel textLabel_;
    private JTextArea textField_;  
    private JCheckBox doInteractiveBox_;
    private JCheckBox doOverFontBox_;
    private JComboBox justCombo_;
    private FontSettingPanel fsp_;
    private ColorSelectionWidget colorWidget_;
    private DataAccessContext dacx_;
    
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
    
    DesktopDialog(ServerControlFlowHarness cfh, Note noteToEdit, NoteProperties props) {
      super(cfh, (noteToEdit == null) ? "ntprops.newTitle" : "ntprops.modTitle", new Dimension(600, 500), 1, new NoteRequest(), false);
      dacx_ = cfh.getDataAccessContext();
      ResourceManager rMan = appState_.getRMan();
      
      String defaultName;
      String defaultText;
      boolean defaultInteractive;
      if (noteToEdit == null) {
        defaultName = rMan.getString("addNote.defaultName");
        defaultText = "";
        defaultInteractive = false;
      } else {
        defaultName = noteToEdit.getName();
        defaultText = noteToEdit.getText();
        defaultInteractive = noteToEdit.isInteractive();
      }   
     
      //
      // Build the tabs.
      //

      JTabbedPane tabPane = new JTabbedPane();
      tabPane.addTab(rMan.getString("ntprops.modelProp"), buildModelTab(defaultName, defaultText, defaultInteractive));
      tabPane.addTab(rMan.getString("ntprops.layoutProp"), buildLayoutTab(props));
      addTable(tabPane, 5);
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
    */
    
    @Override
    protected boolean bundleForExit(boolean forApply) { 
      NoteRequest crq = (NoteRequest)request_; 
      crq.nameResult = nameField_.getText().trim();
      if (crq.nameResult.equals("")) {
        ResourceManager rMan = appState_.getRMan();
        String message = rMan.getString("ntprops.badName");
        String title = rMan.getString("ntprops.badNameTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      }      
      crq.colorResult = colorWidget_.getCurrentColor(); 
      crq.fontOverrideResult = (doOverFontBox_.isSelected()) ? fsp_.getFontResult() : null;
      crq.interactiveResult = doInteractiveBox_.isSelected();
      crq.textResult = (crq.interactiveResult) ? textField_.getText().trim() : "";
      ChoiceContent justSelection = (ChoiceContent)justCombo_.getSelectedItem();
      crq.justResult = justSelection.val;          
      crq.haveResult = true;
      return (true);
    }
  
    /***************************************************************************
    **
    ** Build a tab for model properties
    ** 
    */
     
    private JPanel buildModelTab(String defaultName, String defaultMessage, boolean defaultInteractive) {
  
      JPanel retval = new JPanel();
      retval.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints(); 
      ResourceManager rMan = appState_.getRMan();
      //
      // Build the name panel:
      //
  
      JLabel label = new JLabel(rMan.getString("ntprops.name"));
      nameField_ = new JTextArea(defaultName);
      nameField_.setEditable(true);
      JScrollPane jsp = new JScrollPane(nameField_);  
       
      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
      retval.add(label, gbc);
        
      UiUtil.gbcSet(gbc, 0, 1, 1, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      retval.add(jsp, gbc); 
       
      doInteractiveBox_ = new JCheckBox(rMan.getString("ntprops.interactive"));
      doInteractiveBox_.setSelected(defaultInteractive);    
      doInteractiveBox_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            textField_.setEnabled(doInteractiveBox_.isSelected());
            textLabel_.setEnabled(doInteractiveBox_.isSelected());
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
      UiUtil.gbcSet(gbc, 0, 6, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);       
      retval.add(doInteractiveBox_, gbc);        
  
      //
      // Build the text panel:
      //
  
      textLabel_ = new JLabel(rMan.getString("ntprops.text"));
      textLabel_.setEnabled(doInteractiveBox_.isSelected());
      textField_ = new JTextArea(defaultMessage);
      textField_.setEditable(true);
      textField_.setEnabled(doInteractiveBox_.isSelected());
      jsp = new JScrollPane(textField_);
       
      UiUtil.gbcSet(gbc, 0, 7, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
      retval.add(textLabel_, gbc);
  
      UiUtil.gbcSet(gbc, 0, 8, 1, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      retval.add(jsp, gbc);        
       
      return (retval);
    }  
  
    /***************************************************************************
    **
    ** Build a tab for layout properties
    ** 
    */
     
    private JPanel buildLayoutTab(NoteProperties nProps) {
  
      JPanel retval = new JPanel();
      retval.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints(); 
      ResourceManager rMan = appState_.getRMan();
    
      //
      // Build the color panel.
      //
  
      colorWidget_ = new ColorSelectionWidget(appState_, dacx_, null, true, "ntprops.color", true, false);
      UiUtil.gbcSet(gbc, 0, 0, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
      retval.add(colorWidget_, gbc);     
        
      //
      // Justification:
      //
       
      JLabel label = new JLabel(rMan.getString("ntprops.justType"));
      Vector<ChoiceContent> choices = MultiLineRenderSupport.getJustifyTypes(appState_.getRMan());
      justCombo_ = new JComboBox(choices);
      int currJust = (nProps == null) ? MultiLineRenderSupport.DEFAULT_JUST : nProps.getJustification();
      justCombo_.setSelectedItem(MultiLineRenderSupport.justForCombo(appState_.getRMan(), currJust));        
       
      UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
      retval.add(label, gbc);
       
      UiUtil.gbcSet(gbc, 1, 1, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      retval.add(justCombo_, gbc);        
       
      //
      // Build font support
      //
       
      FontManager.FontOverride fo = (nProps == null) ? null : nProps.getFontOverride();
      doOverFontBox_ = new JCheckBox(rMan.getString("ntprops.overFont"));
      doOverFontBox_.setSelected(fo != null);
          
      doOverFontBox_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            enableOverFont(doOverFontBox_.isSelected());
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
      UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
      retval.add(doOverFontBox_, gbc);    
    
      fsp_ = new FontSettingPanel(appState_);
      fsp_.setEnabled(fo != null);
  
      UiUtil.gbcSet(gbc, 1, 2, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
      retval.add(fsp_, gbc);
       
      colorWidget_.setCurrentColor((nProps == null) ? NoteProperties.DEFAULT_COLOR : nProps.getColorName());
       
      FontManager fmgr = appState_.getFontMgr();
      Font bFont = fmgr.getOverrideFont(FontManager.NOTES, fo).getFont();
      fsp_.displayProperties(bFont);
  
      return (retval);
    }
     
    /***************************************************************************
    **
    ** Enable font
    ** 
    */
     
    private void enableOverFont(boolean enabled) {
      fsp_.setEnabled(enabled);
      return;
    } 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // WEB DIALOG CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
   
  public static class SerializableDialog implements SerializableDialogPlatform.Dialog { 
 
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
     private String defaultName_;
     private String defaultText_;
     private boolean defaultInteractive_;
     private ServerControlFlowHarness scfh_;
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC CONSTRUCTORS
    //
    ////////////////////////////////////////////////////////////////////////////    
  
    /***************************************************************************
    **
    ** Constructor 
    */ 
    
    public SerializableDialog(ServerControlFlowHarness cfh, Note noteToEdit, NoteProperties props) { 
      scfh_ = cfh;
      if (noteToEdit == null) {
        ResourceManager rMan = scfh_.getBTState().getRMan();
        defaultName_ = rMan.getString("addNote.defaultName");
        defaultText_ = "";
        defaultInteractive_ = false;
      } else {
        defaultName_ = noteToEdit.getName();
        defaultText_ = noteToEdit.getText();
        defaultInteractive_ = noteToEdit.isInteractive();     
      }
    }
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
     
    
    //TODO: Implement XPlatDialogs
    public XPlatUIDialog getDialog() {
    	return null;
    }
    
    public XPlatUIDialog getDialog(FlowKey keyVal) {
    	return null;
    }
    
    
    public boolean isModal() {
      return (true);
    }
    
    /***************************************************************************
    **
    ** Get Html
    */
    
    public String getHTML(String hiddenForm) {
      StringBuffer buf = new StringBuffer();
      buf.append("<html>\n");
      buf.append("<body>\n");
      buf.append("<center>\n");
      buf.append("  <form method=\"get\" action=\"");
      String servPath = ((SerializableDialogPlatform)scfh_.getDialogPlatform()).getServletPath();
      buf.append(servPath);
      buf.append("\">\n");
      buf.append("    <p>Note Text: <input type=\"text\" name=\"note\" value=\"" + defaultName_ + "\"></p>");
      buf.append("    <p>FIX ME LOTS OF FIELDS MISSING</p>");
      buf.append("    <p><input type=\"submit\" name=\"formButton\" value=\"OK\"><input type=\"submit\" name=\"formButton\" value=\"Cancel\"></p>");
      buf.append(hiddenForm);
      buf.append("  </form>");
      buf.append("</center>\n");
      buf.append("</body>");
      buf.append("</html>");
      return (buf.toString());
    }   
    
    /***************************************************************************
    **
    ** Return the parameters we are interested in:
    */
    
    public Set<String> getRequiredParameters() {
      HashSet<String> retval = new HashSet<String>();
      retval.add("note");
      retval.add("action");
      return (retval);
    } 

    /***************************************************************************
    **
    ** Talk to the expert!
    ** 
    */
      
    public SimpleUserFeedback checkForErrors(Map<String, String> params) {
      String val = params.get("note");
      if ((val == null) || val.trim().equals("")) {
        ResourceManager rMan = scfh_.getBTState().getRMan();
        String message = rMan.getString("ntprops.badName");
        String title = rMan.getString("ntprops.badNameTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        return (suf);
      }
      return (null);
    }
    
    public SimpleUserFeedback checkForErrors(UserInputs ui) { 	
    	return null;
    }
      
    /***************************************************************************
    **
    ** Do the bundle 
    */
    
    public ServerControlFlowHarness.UserInputs bundleForExit(Map<String, String> params) {
      String val = params.get("note");  
      NoteRequest crq = new NoteRequest();
      crq.nameResult = val;
    //  crq.colorResult = colorWidget_.getCurrentColor(); 
    //  crq.fontOverrideResult = (doOverFontBox_.isSelected()) ? fsp_.getFontResult() : null;
    //  crq.interactiveResult = doInteractiveBox_.isSelected();
    //  crq.textResult = (crq.interactiveResult) ? textField_.getText().trim() : "";
    //  ChoiceContent justSelection = (ChoiceContent)justCombo_.getSelectedItem();
    //  crq.justResult = justSelection.val;          
   //   crq.haveResult = true;
      return (crq);
    }
  }
  
  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class NoteRequest implements ServerControlFlowHarness.UserInputs {
    private boolean haveResult;
    public String textResult;
    public String nameResult;
    public int justResult;
    public String colorResult;
    public boolean interactiveResult;
    public FontManager.FontOverride fontOverrideResult;
    
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
