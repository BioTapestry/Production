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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Note;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.FontManager.FontOverride;
import org.systemsbiology.biotapestry.ui.NamedColor;
import org.systemsbiology.biotapestry.ui.NoteProperties;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.ui.xplat.XPlatLayoutFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatPrimitiveElementFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUICollectionElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatLayoutFactory.RegionType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement.XPlatUIElementType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIPrimitiveElement;
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
    private StaticDataAccessContext dacx_;
    private UIComponentSource uics_;
    private HarnessBuilder hBld_;
    
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
      uics_ = cfh.getUI();
      hBld_ = cfh.getHarnessBuilder();
      ResourceManager rMan = dacx_.getRMan();
      
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
  
    public boolean dialogIsModal() {
      return (true);
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
        ResourceManager rMan = dacx_.getRMan();
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
      crq.setHasResults();
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
      ResourceManager rMan = dacx_.getRMan();
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
            uics_.getExceptionHandler().displayException(ex);
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
      ResourceManager rMan = dacx_.getRMan();
    
      //
      // Build the color panel.
      //
  
      colorWidget_ = new ColorSelectionWidget(uics_, dacx_.getColorResolver(), hBld_, null, true, "ntprops.color", true, false);
      UiUtil.gbcSet(gbc, 0, 0, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
      retval.add(colorWidget_, gbc);     
        
      //
      // Justification:
      //
       
      JLabel label = new JLabel(rMan.getString("ntprops.justType"));
      Vector<ChoiceContent> choices = MultiLineRenderSupport.getJustifyTypes(dacx_.getRMan());
      justCombo_ = new JComboBox(choices);
      int currJust = (nProps == null) ? MultiLineRenderSupport.DEFAULT_JUST : nProps.getJustification();
      justCombo_.setSelectedItem(MultiLineRenderSupport.justForCombo(dacx_.getRMan(), currJust));        
       
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
            uics_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
      UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
      retval.add(doOverFontBox_, gbc);    
    
      fsp_ = new FontSettingPanel(dacx_);
      fsp_.setEnabled(fo != null);
  
      UiUtil.gbcSet(gbc, 1, 2, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
      retval.add(fsp_, gbc);
       
      colorWidget_.setCurrentColor((nProps == null) ? NoteProperties.DEFAULT_COLOR : nProps.getColorName());
       
      FontManager fmgr = dacx_.getFontManager();
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
	  
	  // The dialog box
	 private XPlatUIDialog xplatDialog_;

	 private ServerControlFlowHarness scfh_;
	 private ResourceManager rMan_; 
	 private XPlatPrimitiveElementFactory primElemFac_;
	 private Note noteToEdit_;
	 private NoteProperties nprops_;
    
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
    	this.scfh_ = cfh;
        this.rMan_ = scfh_.getDataAccessContext().getRMan();
        this.primElemFac_ = new XPlatPrimitiveElementFactory(this.rMan_);
        this.noteToEdit_ = noteToEdit;
        this.nprops_ = props;
        
        buildDialog((noteToEdit == null ? "ntprops.newTitle" : "ntprops.modTitle" ), 400, 650);
    }
    
    private void buildDialog(String title, int height, int width) {
    	
    	this.xplatDialog_ = new XPlatUIDialog(title,height,width);
    	
    	String noteID = this.nprops_ != null ? this.nprops_.getReference() : "newNote";
    	
    	this.xplatDialog_.setParameter("id", noteID.replaceAll("\\s+", "_").toLowerCase());
    	
		XPlatUICollectionElement layoutCollection = this.xplatDialog_.createCollection("main", XPlatUIElementType.LAYOUT_CONTAINER);
		layoutCollection.setParameter("style", "height: " + height + "px; width: " + width + "px;");
		layoutCollection.setParameter("gutters", "false");
		  
		this.xplatDialog_.createCollectionList("main", "center");
		this.xplatDialog_.createCollectionList("main", "bottom");
		  
		// Set up the Tab container
	
		XPlatUICollectionElement tabContainer = new XPlatUICollectionElement(XPlatUIElementType.TAB_CONTAINER);
		tabContainer.setParameter("style", "height: " + (height - 30)+ "px; width: " + (width - 10) + "px;");
		tabContainer.setParameter(
			"id", this.primElemFac_.generateId("noteEditorTabCtrl", XPlatUIElementType.TAB_CONTAINER)
		);
		tabContainer.setParameter("gutters", "false");
		tabContainer.setLayout(XPlatLayoutFactory.makeRegionalLayout(RegionType.CENTER, 0));
		
		this.xplatDialog_.addElementToCollection("main", "center", tabContainer);

		String modelProps = this.rMan_.getString("ntprops.modelProp");
		String layoutProps = this.rMan_.getString("ntprops.layoutProp");

		tabContainer.createList(modelProps);
		tabContainer.createList(layoutProps);

		tabContainer.addGroupOrder(modelProps, new Integer(0));
		tabContainer.addGroupOrder(layoutProps, new Integer(1));

		tabContainer.addElementGroupParam(modelProps, "index", "0");
		tabContainer.addElementGroupParam(layoutProps, "index", "1");

		tabContainer.setSelected(modelProps);
		
		XPlatUICollectionElement modelPropsLayout = new XPlatUICollectionElement(XPlatUIElementType.LAYOUT_CONTAINER);
		modelPropsLayout.createList("center");
		tabContainer.addElement(modelProps, modelPropsLayout);

		buildModelPropsTab(modelPropsLayout);

		XPlatUICollectionElement layoutPropsLayout = new XPlatUICollectionElement(XPlatUIElementType.LAYOUT_CONTAINER);
		layoutPropsLayout.createList("center");
		tabContainer.addElement(layoutProps, layoutPropsLayout);

		buildLayoutPropsTab(layoutPropsLayout);
		this.xplatDialog_.setUserInputs(new NoteRequest(true));
    }
    
    /////////////////////////
    // buildModelPropsTab
    /////////////////////////
    //
    //
    private void buildModelPropsTab(XPlatUICollectionElement container) {
    	String currName = (this.noteToEdit_ != null ? this.noteToEdit_.getName() : this.rMan_.getString("addNote.defaultName"));
    	String currTxt = (this.noteToEdit_ != null ? this.noteToEdit_.getText() : "      ");
    	
    	XPlatUIElement nameTxt = this.primElemFac_.makeTextBox("nameTxt", true, currName, null, true, this.rMan_.getString("ntprops.name"), false);
    	nameTxt.setParameter("style", "width: 95%; min-height: 7em;");
    	nameTxt.setParameter("justifiedLabel", true);
    	nameTxt.setParameter("bundleAs", "nameResult");
    	
    	container.addElement("center",nameTxt);
    	
    	XPlatUIPrimitiveElement chkBx = this.primElemFac_.makeCheckbox(
			this.rMan_.getString("ntprops.interactive"), "CLIENT_SET_ELEMENT_CONDITION", "isInteractive", (this.noteToEdit_ != null ? this.noteToEdit_.isInteractive() : false)
		);
    	chkBx.setParameter("bundleAs", "interactiveResult");
    	
		Map<String,Object> onChangeParams = new HashMap<String,Object>();
		onChangeParams.put("conditionValueLoc", "ELEMENT_VALUES");
		  
		chkBx.getEvent("change").setParameters(onChangeParams);
		    	
    	container.addElement("center",chkBx);
    	
    	XPlatUIPrimitiveElement displayTxt = this.primElemFac_.makeTextBox("displayTxt", true, currTxt, null, true, this.rMan_.getString("ntprops.text"), false);
    	container.addElement("center",displayTxt);
    	
    	displayTxt.setParameter("disabled", (this.noteToEdit_ != null ? !(this.noteToEdit_.isInteractive()) : true));
    	displayTxt.setParameter("style", "width: 95%; min-height: 7em;");
    	displayTxt.setParameter("justifiedLabel", true);
    	displayTxt.setValidity("isInteractive", "true");
    	displayTxt.setParameter("bundleAs", "textResult");
    	
    	this.xplatDialog_.addDefaultState_("isInteractive", (this.noteToEdit_ != null ? new Boolean(this.noteToEdit_.isInteractive()).toString() : "false"));
    	
    }
    
    
    //////////////////////////
    // buildLayoutPropsTab
    //////////////////////////
    //
    //
    private void buildLayoutPropsTab(XPlatUICollectionElement container) {
    	List<NamedColor> ncList = new ArrayList<NamedColor>();
    	DataAccessContext dacx = scfh_.getDataAccessContext();
        Iterator<String> ckit = dacx.getColorResolver().getColorKeys();
                
        while (ckit.hasNext()) {
        	String colorKey = ckit.next();
        	NamedColor nc = dacx.getColorResolver().getNamedColor(colorKey);
        	ncList.add(new NamedColor(nc));
        }
        
        Collections.sort(ncList); 
        
        Map<String,Object> colorVals = new HashMap<String,Object>();
        int count = 0; 
        
        for(NamedColor nc : ncList) {
        	Map<String,String> colorMap = new HashMap<String,String>();
        	colorMap.put("id", nc.key);
        	colorMap.put("label", nc.name);
        	colorMap.put("index",Integer.toString(count));
        	colorMap.put("color", "#"+Integer.toHexString(nc.color.getRGB()).substring(2));
        	colorVals.put(nc.key,colorMap);
        	count++;
        }
        
        XPlatUIPrimitiveElement colorCombo = this.primElemFac_.makeColorChooserComboBox(
    		"note_color", (this.nprops_ == null) ? NoteProperties.DEFAULT_COLOR : this.nprops_.getColorName(), 
			null, this.rMan_.getString("ntprops.color"), colorVals
		);
        colorCombo.setFloat(true);
        colorCombo.setParameter("bundleAs", "colorResult");
        
        XPlatUIPrimitiveElement colorEdBtn = this.primElemFac_.makeBasicButton(this.rMan_.getString("colorSelector.colorNew"), "launch_color_ed", "CLIENT_LAUNCH_COLOR_EDITOR", null);
        colorEdBtn.setFloat(true);
        colorEdBtn.setParameter("colorChoices", ncList);
        
        Map<String,Object> txtJustVals = new HashMap<String,Object>();
        Vector<ChoiceContent> choices = MultiLineRenderSupport.getJustifyTypes(this.rMan_);
        
        for(ChoiceContent tj : choices) {
        	txtJustVals.put(new Integer(tj.val).toString(),tj.name);
        }
        
        XPlatUIPrimitiveElement txtJust = this.primElemFac_.makeTxtComboBox(
        	"txt_just"
    		, Integer.toString((this.nprops_ == null) ? MultiLineRenderSupport.DEFAULT_JUST : this.nprops_.getJustification())
    		, null, this.rMan_.getString("ntprops.justType"), txtJustVals
		);
        txtJust.setParameter("bundleAs", "justResult");
        
        FontOverride fontOverride = (this.nprops_ != null ? this.nprops_.getFontOverride() : null);
        
        XPlatUIPrimitiveElement fontOvr = this.primElemFac_.makeCheckbox(
    		this.rMan_.getString("ntprops.overFont"), "CLIENT_SET_ELEMENT_CONDITION", "fontOverride",fontOverride != null
		);
		Map<String,Object> onChangeParams = new HashMap<String,Object>();
		onChangeParams.put("conditionValueLoc", "ELEMENT_VALUES");
		  
		fontOvr.getEvent("change").setParameters(onChangeParams);
        fontOvr.setFloat(true);
        // Special CSS layout
        fontOvr.setParameter("class","FontOverride");

        int min = FontManager.MIN_SIZE;
        int max = FontManager.MAX_SIZE;
        Map<String,Object> fontSizes = new HashMap<String,Object>();
        for (int i = min; i <= max; i++) {
          fontSizes.put(new Integer(i).toString(),new Integer(i).toString() + " pt");
        }
        
        Font bFont = this.scfh_.getDataAccessContext().getFontManager().getOverrideFont(FontManager.NOTES, fontOverride).getFont();
        
        XPlatUIPrimitiveElement fontSize = this.primElemFac_.makeTxtComboBox(
    		"note_font_size", new Integer(bFont.getSize()).toString(), null, this.rMan_.getString("fontDialog.sizeLabel"), fontSizes
		);
        fontSize.setFloat(true);
        // Special CSS class because this is a small combo box
        fontSize.setParameter("class","FontOverrideSize");
        fontSize.setParameter("bundleAs", "size");
        fontSize.setParameter("bundleIn", "fontOverrideResult");
        
        XPlatUIPrimitiveElement fontSerif = this.primElemFac_.makeCheckbox(
    		this.rMan_.getString("fontDialog.serifLabel"), null, "note_font_serif"
    		,(fontOverride != null ? (!fontOverride.makeSansSerif) : false)
		);
        fontSerif.setFloat(true);
        fontSerif.setParameter("bundleAs", "makeSansSerif");
        fontSerif.setParameter("bundleIn", "fontOverrideResult");
        
        XPlatUIPrimitiveElement fontBold = this.primElemFac_.makeCheckbox(
    		this.rMan_.getString("fontDialog.boldLabel"), null, "note_font_bold"
    		,(fontOverride != null ? (fontOverride.makeBold) : false)
		);
        fontBold.setFloat(true);
        fontBold.setParameter("bundleAs", "makeBold");
        fontBold.setParameter("bundleIn", "fontOverrideResult");
        
        XPlatUIPrimitiveElement fontItal = this.primElemFac_.makeCheckbox(
    		this.rMan_.getString("fontDialog.italicLabel"), null, "note_font_italics"
    		,(fontOverride != null ? (fontOverride.makeItalic) : false)
		);
        fontItal.setFloat(true);
        fontItal.setParameter("bundleAs", "makeItalic");
        fontItal.setParameter("bundleIn", "fontOverrideResult");
        
        XPlatUICollectionElement topCenter = new XPlatUICollectionElement(XPlatUIElementType.PANE);
        topCenter.createList("main");
        container.addElement("center", topCenter);
        XPlatUICollectionElement midCenter = new XPlatUICollectionElement(XPlatUIElementType.PANE);
        midCenter.createList("main");
        container.addElement("center", midCenter);
        XPlatUICollectionElement btmCenter = new XPlatUICollectionElement(XPlatUIElementType.PANE);
        btmCenter.createList("main");
        container.addElement("center", btmCenter);
        
        topCenter.addElement("main", colorCombo);
        topCenter.addElement("main", colorEdBtn);
            	
    	midCenter.addElement("main", txtJust);
    	
    	btmCenter.addElement("main", fontOvr);
    	btmCenter.addElement("main", fontSize);
    	btmCenter.addElement("main", fontSerif);
    	btmCenter.addElement("main", fontBold);
    	btmCenter.addElement("main", fontItal);
    	
    	fontSize.setValidity("fontOverride", "true");
    	fontSerif.setValidity("fontOverride", "true");
    	fontBold.setValidity("fontOverride", "true");
    	fontItal.setValidity("fontOverride", "true");
    	
    	this.xplatDialog_.addDefaultState_("fontOverride", (this.noteToEdit_ != null ? new Boolean(fontOverride != null).toString() : "false"));
    }
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
     
    
	  public XPlatUIDialog getDialog() {
		  
		  Map<String,String> clickActions = new HashMap<String,String>();
		  clickActions.put("cancel", "CLIENT_CANCEL_COMMAND");
		  clickActions.put("ok", "DO_NOTHING");
		  
		  List<XPlatUIElement> btns = this.primElemFac_.makeOkCancelButtons(clickActions.get("ok"),clickActions.get("cancel"),false); 
		  
		  this.xplatDialog_.setCancel(clickActions.get("cancel"));
		  
		  this.xplatDialog_.replaceElementsInCollection("main","bottom",btns);
		  
		  return xplatDialog_;
	  }	  
	  
	  
	  public XPlatUIDialog getDialog(FlowKey okKeyVal) {
		  
		  Map<String,String> clickActions = new HashMap<String,String>();
		  clickActions.put("cancel", "CLIENT_CANCEL_COMMAND");
		  clickActions.put("ok", okKeyVal.toString());
		  
		  List<XPlatUIElement> btns = this.primElemFac_.makeOkCancelButtons(clickActions.get("ok"),clickActions.get("cancel"),false); 
		  
		  this.xplatDialog_.setCancel(clickActions.get("cancel"));
		  
		  this.xplatDialog_.replaceElementsInCollection("main","bottom",btns);
		  
		  return xplatDialog_;
	  }
    
    
	  public boolean dialogIsModal() {
      return (true);
    }
    
	  // Stub to satisfy interface
	  public String getHTML(String hiddenForm) {
		  return null;
	  }   
        
	  // We require the 'name' field to not be null/empty
	  // All other values can be blank
	  public SimpleUserFeedback checkForErrors(UserInputs ui) { 
	      NoteRequest crq = (NoteRequest)ui; 

	      if (crq.nameResult.equals("")) {
	        String message = this.rMan_.getString("ntprops.badName");
	        String title = this.rMan_.getString("ntprops.badNameTitle");
	        crq.clearHaveResults();
	        return (new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title));
	      } 
	      // A -1 size means there is no font override, therefor we need to
	      // null this out
	      if(crq.fontOverrideResult != null && crq.fontOverrideResult.size <= 0) {
	    	  crq.fontOverrideResult = null;
	      }
	      crq.setHasResults();
		  return null;
	  }
      
	  // Stub to satisfy interface
	  public DialogAndInProcessCmd handleSufResponse(DialogAndInProcessCmd daipc) {
		  // TODO Auto-generated method stub
		  return null;
	  }
  }
  
  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class NoteRequest implements ServerControlFlowHarness.UserInputs {
    
    public String textResult;
    public String nameResult;
    public int justResult;
    public String colorResult;
    public boolean interactiveResult;
    public FontManager.FontOverride fontOverrideResult;
    
    private boolean haveResult;
    
    public NoteRequest() {

    }
    
    // For serialization when null fields are excluded
    public NoteRequest(boolean forTransit) {
    	haveResult = true;
    	textResult = "";
    	nameResult = "";
    	justResult = -1;
    	colorResult = "";
    	interactiveResult = false;
    	fontOverrideResult = new FontManager.FontOverride(-1,false,false,false);	
    }
    
    public void clearHaveResults() {
      haveResult = false;
      return;
    }
    
    public void setHasResults() {
    	haveResult = true;
    	return;
    }
    
    public boolean haveResults() {
        return (haveResult);
    }  
      
    public boolean isForApply() {
        return (false);
    }
    
    // setter/getter required for serialization
    public void setHaveResult(boolean result) {
    	this.haveResult = result;
    	return;
    }
    public boolean getHaveResult() {
    	return this.haveResult;
    }
  }
}
