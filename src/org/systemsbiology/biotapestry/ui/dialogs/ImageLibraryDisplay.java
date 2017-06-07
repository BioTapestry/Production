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

import java.awt.Component;
import java.awt.GridLayout;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.ResourceManager;
 
/****************************************************************************
**
** Panel for images in the Image Manager
*/

public class ImageLibraryDisplay extends JDialog implements ListSelectionListener {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JList pathList_;
  private ImageRenderer listRenderer_;
  private UIComponentSource uics_;
  private boolean ignore_;
 
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
  
  public ImageLibraryDisplay(UIComponentSource uics, DataAccessContext dacx) {
    super(uics.getTopFrame(), dacx.getRMan().getString("Foobar"), true);
    uics_ = uics;
    setSize(300, 400);
    
    ResourceManager rMan = dacx.getRMan(); 

    URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/LinkPlus.gif");  
    ImageIcon[] icons = new ImageIcon[1];   
    icons[0] = new ImageIcon(ugif);
 
   List<String> imageNames = new ArrayList<String>();
    imageNames.add("foo");
    imageNames.add("bar");
    imageNames.add("fee");
    
    
    pathList_ = new JList(imageNames.toArray());
    pathList_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    pathList_.addListSelectionListener(this);

    listRenderer_ = new ImageRenderer(imageNames, icons);
    pathList_.setCellRenderer(listRenderer_);
    setLayout(new GridLayout(1,1));
    add(pathList_);
    setLocationRelativeTo(uics_.getTopFrame());
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
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
      pathList_.getSelectedValue();
    } catch (Exception ex) {
      uics_.getExceptionHandler().displayException(ex);
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
  ** Used for the data table renderer
  */
  
  class ImageRenderer extends JLabel implements ListCellRenderer {
    
    private List<String> values_;
    private ImageIcon[] icons_;
    private static final long serialVersionUID = 1L;
   
    ImageRenderer(List<String> values, ImageIcon[] icons) {
      super();
      values_ = values;
      icons_ = icons;
    }
    
    void setValues(List<String> values) {
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
        String currImgName = values_.get(index);
        setIcon(icons_[0]);
        setText(currImgName);
        System.out.println("set text " + currImgName);
        setOpaque(true);
        setBackground((isSelected) ? list.getSelectionBackground() : list.getBackground());       
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return (this);             
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
}
