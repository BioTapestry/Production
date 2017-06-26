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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.ui.NamedColor;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.util.ColorDeletionListener;
import org.systemsbiology.biotapestry.util.ColorListRenderer;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Factory to create dialogs for color changes
*/

public class ColorEditorDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ColorEditorDialogFactory(ServerControlFlowHarness cfh) {
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
   
    BuildArgs dniba = (BuildArgs)ba;
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.colors, dniba.cannotDeleteColors, dniba.cdl, dniba.cRes));
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
  
  public static class BuildArgs extends DialogBuildArgs { 
    
    List<NamedColor> colors;
    Set<String> cannotDeleteColors;
    List<ColorDeletionListener> cdl;
    ColorResolver cRes;
 
    public BuildArgs(List<NamedColor> colors, Set<String> cannotDeleteColors, 
                     List<ColorDeletionListener> cdl, ColorResolver cRes) {
      super(null);
      this.colors = colors;
      this.cannotDeleteColors = cannotDeleteColors;
      this.cdl = cdl;
      this.cRes = cRes;
    } 
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // DIALOG CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////      
  
  public static class DesktopDialog extends BTTransmitResultsDialog implements ListSelectionListener { 

    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
    
    private JList colorListing_;
    private HashSet<String> origKeys_;
    private ArrayList<NamedColor> colorList_;  
    private ColorListRenderer renderer_;
    private JScrollPane jsp_;
    private JTextField textField_;
    private JColorChooser chooser_;
    private FixedJButton buttonD_;
    private NamedColor onDisplay_;
    private boolean updatingUI_;
    private List<ColorDeletionListener> cdls_;
    private Set<String> cannotDeleteColors_;
    private ColorResolver cRes_;
 
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
    
    public DesktopDialog(ServerControlFlowHarness cfh, List<NamedColor> colors, 
                         Set<String> cannotDelete, 
                         List<ColorDeletionListener> colorDeletionListeners,
                         ColorResolver cRes) { 
      super(cfh, "colorDialog.title", new Dimension(1024, 500), 1, new ColorsRequest(), true);
      cdls_ = colorDeletionListeners;
      cRes_ = cRes;
      updatingUI_ = false;
      cannotDeleteColors_ = cannotDelete;
      GridBagConstraints gbc = new GridBagConstraints();
      
      buildUIFromColors(colors);
      
      colorListing_ = new JList(colorList_.toArray());
      colorListing_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      colorListing_.addListSelectionListener(this);
      renderer_ = new ColorListRenderer(colorList_, uics_.getHandlerAndManagerSource());
      colorListing_.setCellRenderer(renderer_);
      jsp_ = new JScrollPane(colorListing_);
      jsp_.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jsp_.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);    
      
      // Put the list on the left, text box top right, and a JColorChooser on the right
      //
      
      JPanel mashUp = new JPanel();
      mashUp.setLayout(new GridBagLayout());
      
      UiUtil.gbcSet(gbc, 0, 0, 3, 5, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
      mashUp.add(jsp_, gbc);
     
      JLabel label = new JLabel(rMan_.getString("colorDialog.selColor"));
      UiUtil.gbcSet(gbc, 3, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
      mashUp.add(label, gbc);    
      textField_ = new JTextField();
      UiUtil.gbcSet(gbc, 4, 0, 4, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);       
      mashUp.add(textField_, gbc);    
      
      chooser_ = new JColorChooser();
      AbstractColorChooserPanel[] vals = chooser_.getChooserPanels();
      chooser_.removeChooserPanel(vals[0]);  // Dump the swatches
      //chooser_.setPreviewPanel(new JPanel());
      UiUtil.gbcSet(gbc, 3, 1, 5, 4, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
      mashUp.add(chooser_, gbc);
      chooser_.setEnabled(false);
      textField_.setEnabled(false);
      
      FixedJButton buttonN = new FixedJButton(rMan_.getString("dialogs.addEntry"));
      buttonN.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            HashSet<String> names = buildNames();
            initInputPane(getUniqueColorName(names));
            showInputPane(true);    
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });
      
      buttonD_ = new FixedJButton(rMan_.getString("dialogs.deleteEntry"));
      buttonD_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            DesktopDialog.this.deleteAColor();
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });
      buttonD_.setEnabled(false);
      
      FixedJButton buttonS = new FixedJButton(rMan_.getString("colorDialog.sort"));
      buttonS.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            DesktopDialog.this.sort();
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });    
      
      //
      // Add the add/delete buttons
      //
      
      UiUtil.gbcSet(gbc, 0, 6, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      mashUp.add(buttonN, gbc);
      
      UiUtil.gbcSet(gbc, 1, 6, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      mashUp.add(buttonD_, gbc); 
      
      UiUtil.gbcSet(gbc, 2, 6, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      mashUp.add(buttonS, gbc);    
      
      addTableNoInset(mashUp, 8);

      finishConstructionForApply();
      
      buildInputPane("addColor.ChooseName", "addColor.ChooseTitle");
      
    }    
     
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
    
    public boolean dialogIsModal() {
      return (true);
    }
   
    /***************************************************************************
    **
    ** List selection stuff
    ** 
    */  

    public void valueChanged(ListSelectionEvent e) {
      try {
        if (updatingUI_) {
          return;
        }
        if (e.getValueIsAdjusting()) {
          return;
        }
        extractToList();
  
        NamedColor color = (NamedColor)colorListing_.getSelectedValue();
        if (color != null) {
          chooser_.setColor(color.color);     
          buttonD_.setEnabled(true);
          onDisplay_ = color;
          textField_.setText(color.name);
          chooser_.setEnabled(true);
          textField_.setEnabled(true);
        } else {
          chooser_.setColor(Color.black);       
          buttonD_.setEnabled(false);
          onDisplay_ = null;
          textField_.setText("");
          chooser_.setEnabled(false);
          textField_.setEnabled(false);      
        }
        chooser_.revalidate();
        textField_.revalidate();         
        colorListing_.revalidate();
        buttonD_.revalidate();
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;    
    }
  
    /***************************************************************************
    **
    ** Apply the color values to our UI components
    ** 
    */
    
    private void buildUIFromColors(List<NamedColor> colors) {
      
      Iterator<NamedColor> cit = colors.iterator();
      colorList_ = new ArrayList<NamedColor>();
      origKeys_ = new HashSet<String>();
      
      while (cit.hasNext()) {
        NamedColor col = cit.next();
        colorList_.add(new NamedColor(col));
        origKeys_.add(col.key);
      }
      Collections.sort(colorList_);
      
      return;
    }

    /***************************************************************************
    **
    ** Build set of names
    ** 
    */
    
    private HashSet<String> buildNames() {  
      //
      // Build set of existing names
      //
      Iterator<NamedColor> cit = colorList_.iterator();
      HashSet<String> names = new HashSet<String>();
      while (cit.hasNext()) {
        NamedColor color = cit.next();
        names.add(color.getDescription());
      }
      return (names);
    }
    
    /***************************************************************************
    **
    ** Process the input pane result
    ** 
    */    
    
    @Override
    public boolean processInputAnswer() {
      
      HashSet<String> names = buildNames();
      String newName = getInputAnswer();
      // Remember old eventing framework for control flow results: use that again for e.g. color deletion listeners?
      // What to do about a desktop harness launch in a dialog? This needs to be hidden behind current framework.
        
      if (newName.trim().equals("")) {
        String message = rMan_.getString("addColor.EmptyName");
        String title = rMan_.getString("addColor.EmptyNameTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      }
        
      if (names.contains(newName)) {
        String desc = MessageFormat.format(rMan_.getString("addColor.NameInUse"), 
                                           new Object[] {newName});
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, desc, rMan_.getString("addColor.CreationErrorTitle"));
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      }
      
      //
      // Now enter the color into the list with a neutral grey:
      //
      
      //
      // FIX ME! This is not going to work: it must be a qBomb! 
     
      String colKey = cRes_.getNextColorLabel();
      NamedColor newCol = new NamedColor(colKey, new Color(127, 127, 127), newName);
      colorList_.add(newCol);
      updatingUI_ = true;
      colorListing_.setListData(colorList_.toArray());
      updatingUI_ = false;
      colorListing_.setSelectedValue(newCol, true);
      return (true); 
    }
  
    /***************************************************************************
    **
    ** Get a new unique color name
    */
    
    private String getUniqueColorName(HashSet<String> names) {
      String rootName = rMan_.getString("addColor.NewNameRoot");    
      StringBuffer buf = new StringBuffer();
      String testName = rootName;
      int count = 1;
      while (true) {
        if (!names.contains(testName)) {
          return (testName);
        }
        buf.setLength(0);
        buf.append(rootName);
        buf.append("_");
        buf.append(count++);
        testName = buf.toString();
      }
    }

    /***************************************************************************
    **
    ** Delete a color
    */
    
    private void deleteAColor() {
  
      NamedColor color = (NamedColor)colorListing_.getSelectedValue();
      if (color != null) { 
        if (cannotDeleteColors_.contains(color.key)) {
          String message = rMan_.getString("deleteColor.CannotDelete");
          String title = rMan_.getString("deleteColor.CannotDeleteTitle");
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
          cfh_.showSimpleUserFeedback(suf);
          return;
        }
      }
    
      String message = rMan_.getString("deleteColor.ConfirmDelete");
      String title = rMan_.getString("deleteColor.ConfirmDeleteTitle");
      SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title);
      cfh_.showSimpleUserFeedback(suf);

      switch(suf.getIntegerResult()) {
        case SimpleUserFeedback.YES:
          break;   
        case SimpleUserFeedback.NO:
          return;
        default:
          throw new IllegalStateException();
      }   
  
      //
      // Take it out of the display list, update the UI widget, null out the display
      // flag:
      //
      
      updatingUI_ = true;
      color = (NamedColor)colorListing_.getSelectedValue();
      if (color != null) {
        chooser_.setColor(Color.black);       
        buttonD_.setEnabled(false);
        onDisplay_ = null;
      }    
      colorList_.remove(color);
      colorListing_.setSelectedIndex(-1);
      colorListing_.setListData(colorList_.toArray());
      updatingUI_ = false;
  
      UiUtil.fixMePrintout("ColorDeletionListeners not getting called!!");
      
      
      
      return;
    }   
  
    /***************************************************************************
    **
    ** Extract data and apply to list
    */  
  
    private void extractToList() {
      if (onDisplay_ != null) {
        String newName = textField_.getText().trim();
        if (!newName.equals(onDisplay_.name)) {
          HashSet<String> names = buildNames();
          if (names.contains(newName)) {
            String desc = MessageFormat.format(rMan_.getString("addColor.NameInUse"), 
                                               new Object[] {newName});
            SimpleUserFeedback suf = new SimpleUserFeedback(
              SimpleUserFeedback.JOP.ERROR, desc, rMan_.getString("addColor.CreationErrorTitle")
            );
            cfh_.showSimpleUserFeedback(suf);
          } else { 
            onDisplay_.name = newName;
          }
        }
        onDisplay_.color = chooser_.getColor();
      }  
      return;    
    }  
    
    /***************************************************************************
    **
    ** Do the bundle 
    */
     
    @Override
    protected boolean bundleForExit(boolean forApply) {
      
      
      //
      // Grab latest color, if appropriate:
      //
  
      extractToList();
      colorListing_.revalidate();
      colorListing_.repaint();
          
      ColorsRequest pcq = (ColorsRequest)request_;
      pcq.colors = new ArrayList<NamedColor>();
      Iterator<NamedColor> cit = colorList_.iterator();
      while (cit.hasNext()) {
        NamedColor col = cit.next();
        pcq.colors.add(new NamedColor(col));
      }
      pcq.isForApply_ = forApply; 
      pcq.haveResult = true;
      return (true);
    }
    
    /***************************************************************************
    **
    ** Resort the list
    ** 
    */
    
    private void sort() {
      extractToList();
      Collections.sort(colorList_);
      Object selected = colorListing_.getSelectedValue();
      updatingUI_ = true;
      colorListing_.setListData(colorList_.toArray());
      colorListing_.setSelectedValue(selected, true);
      updatingUI_ = false;    
      colorListing_.revalidate();
      colorListing_.repaint();
      return;
    }
  }
    
  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class ColorsRequest implements ServerControlFlowHarness.UserInputs {
    private boolean haveResult;     
    public List<NamedColor> colors;
    private boolean isForApply_;
    
    public void clearHaveResults() {
      haveResult = false;
      return;
    }   
    public boolean haveResults() {
      return (haveResult);
    }  
    public boolean isForApply() {
      return (isForApply_);
    }
	public void setHasResults() {
		this.haveResult = true;
		return;
	}
  }
}
