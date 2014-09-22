/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import javax.swing.JList;
import javax.swing.DefaultListCellRenderer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Vector;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;

/****************************************************************************
**
** A JList that has checkboxes to select
*/

public class CheckBoxList extends JList {
                                                       
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final int BOX_WIDTH_        = 10;
  private static final int BOX_MARGIN_       = 5;
  private static final int INTER_BOX_MARGIN_ = 3;
  
  private static final int MAIN_BOX_MAX_ = BOX_MARGIN_ + BOX_WIDTH_;
  private static final int SUB_BOX_MIN_ = MAIN_BOX_MAX_ + INTER_BOX_MARGIN_ - 1;
  private static final int SUB_BOX_MAX_ = SUB_BOX_MIN_ + BOX_WIDTH_ + 2;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
      
  private boolean myEnabled_;
  private boolean showSubChecks_;
  private boolean enableSubChecks_;
  private CheckBoxListListener listen_;
  private CheckBoxCellRenderer renderer_;
  private MouseHandler mHandler_;
  private boolean doTips_;
  private String mainTip_;
  private String subTip_;
  private HandlerAndManagerSource ehs_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor allowing sub selection and toolTips.  IF YOU ALLOW TIPS,
  ** CALL UNREGISTER WHEN DISCARDING THE LIST
  */ 
  
  public CheckBoxList(CheckBoxListListener listen, boolean doSubSelection, boolean doTips, HandlerAndManagerSource ehs) {
    ehs_ = ehs;
    listen_ = listen;
    myEnabled_ = true;
    getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    mHandler_ = new MouseHandler(doSubSelection, doSubSelection);
    addMouseListener(mHandler_);
    showSubChecks_ = doSubSelection;
    enableSubChecks_ = doSubSelection;
    renderer_ = new CheckBoxCellRenderer(doSubSelection, doSubSelection);
    setCellRenderer(renderer_);
    doTips_ = doTips;
    if (doTips_) {
      ToolTipManager.sharedInstance().registerComponent(this); 
    }
  }  
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public CheckBoxList(CheckBoxListListener listen, boolean doSubSelection, HandlerAndManagerSource ehs) {
    listen_ = listen;
    myEnabled_ = true;
    getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    mHandler_ = new MouseHandler(doSubSelection, doSubSelection);
    addMouseListener(mHandler_);
    showSubChecks_ = doSubSelection;
    enableSubChecks_ = doSubSelection;
    renderer_ = new CheckBoxCellRenderer(doSubSelection, doSubSelection);
    setCellRenderer(renderer_);
    doTips_ = false;
  }
 
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public CheckBoxList(Vector items, HandlerAndManagerSource ehs) {
    super(items);
    myEnabled_ = true;
    showSubChecks_ = false;
    enableSubChecks_ = false;
    getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    mHandler_ = new MouseHandler(false, false);
    addMouseListener(mHandler_);
    renderer_ = new CheckBoxCellRenderer(false, false);
    setCellRenderer(renderer_);
    doTips_ = false;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Set tooltip text
  */
  
  public void setToolTipText(String mainTip, String subTip) {
    mainTip_ = mainTip;
    subTip_ = subTip;
    return;
  }   
 
  /***************************************************************************
  **
  ** Shutdown routine
  */
  
  public void unregister() {
    if (doTips_) {
      ToolTipManager.sharedInstance().unregisterComponent(this);
    }
    return;
  }  
   
  /***************************************************************************
  **
  ** Get the tool tip
  */
  
  public String getToolTipText(MouseEvent event) {
    try {        
      if (!doTips_) {
        return (null);
      }
      Point pt = event.getPoint();
      int clickIndex = locationToIndex(pt);
      if (clickIndex < 0) {
        return (null);
      }
      Rectangle rect = getCellBounds(clickIndex, clickIndex);
      if ((rect == null) || (rect.getMaxY() < pt.getY())) {
        return (null);
      }
      int x = (int)pt.getX();
      if (showSubChecks_) {
        if ((x <= MAIN_BOX_MAX_) || (x > SUB_BOX_MAX_)) {
          return (mainTip_);
        } else if ((x >= SUB_BOX_MIN_) && (x <= SUB_BOX_MAX_)) {
          return (subTip_);
        } else { // actually should be handled OK in first case...
          return (mainTip_);
        }
      } else {
        return (mainTip_);
      }
    } catch (Exception ex) {
      ehs_.getExceptionHandler().displayException(ex);
    } 
    return (null);
  }  
  
  /***************************************************************************
  **
  ** Disable/enable
  */ 
  
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    myEnabled_ = enabled;
    return;
  }  
 
  /***************************************************************************
  **
  ** Show/hide sub selection option
  */ 
  
  public void showAndEnableSubSelection(boolean show, boolean enable) {
    renderer_.allowSubChecks(show, enable);
    mHandler_.allowSubChecks(enable, show);
    showSubChecks_ = show;
    enableSubChecks_ = enable;
    return;
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

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
    
    private boolean allowSubChecks_;
    private boolean showSubChecks_;
    
    MouseHandler(boolean allowSubChecks, boolean showSubChecks) {
      allowSubChecks_ = allowSubChecks;
      showSubChecks_ = showSubChecks;
    }
    
    public void allowSubChecks(boolean allowSubChecks, boolean showSubChecks) {
      allowSubChecks_ = allowSubChecks;
      showSubChecks_ = showSubChecks;
      return;
    }    
    
    public void mousePressed(MouseEvent me) {
      return;
    }
    
    public void mouseClicked(MouseEvent me) {
      return;
    }    

    public void mouseReleased(MouseEvent me) {
      handleClick(me.getX(), me.getY());
      return;
    }
    
    private void handleClick(int x, int y) {
      try {
        if (!myEnabled_) {
          return;
        }
        int clickIndex = CheckBoxList.this.locationToIndex(new Point(x, y));
        if (clickIndex == -1) {
          return;
        }
        Rectangle rect = CheckBoxList.this.getCellBounds(clickIndex, clickIndex);
        if (rect.getMaxY() < (double)y) {
          return;
        }
        ListChoice choice = (ListChoice)CheckBoxList.this.getModel().getElementAt(clickIndex);
        if (choice.isLocked) {
          return;
        }
        if (showSubChecks_) {
          if ((x <= MAIN_BOX_MAX_) || (x > SUB_BOX_MAX_)) {
            choice.isSelected = !choice.isSelected;
          } else if ((x >= SUB_BOX_MIN_) && (x <= SUB_BOX_MAX_)) {
            if (allowSubChecks_ && choice.isSelected) {
              choice.isSubSelected = !choice.isSubSelected;
            }
          } else { // actually should be handled OK in first case...
            choice.isSelected = !choice.isSelected;
          }
        } else {
          choice.isSelected = !choice.isSelected;
        }
        CheckBoxList.this.repaint();
        if (listen_ != null) listen_.checkIsClicked();
      } catch (Exception ex) {
        ehs_.getExceptionHandler().displayException(ex);
      }     
    }
  }
  
  /****************************************************************************
  **
  ** For rendering leaves
  */

  public class CheckBoxCellRenderer extends DefaultListCellRenderer {
    
    private CheckBoxItem myItem_;
    private boolean showSubChecks_;
    private boolean enableSubChecks_;
    private static final long serialVersionUID = 1L;
    
    public CheckBoxCellRenderer(boolean showSubChecks, boolean enableSubChecks) {
      super();
      myItem_ = new CheckBoxItem(showSubChecks);
      showSubChecks_ = showSubChecks;
      enableSubChecks_ = enableSubChecks;
    }
    
    public void allowSubChecks(boolean showSubChecks, boolean enableSubChecks) {
      myItem_ = new CheckBoxItem(showSubChecks);
      showSubChecks_ = showSubChecks;
      enableSubChecks_ = enableSubChecks;
      return;
    }
        
    public Component getListCellRendererComponent(JList list, Object value, 
                                                  int index, boolean selected, boolean hasFocus) {
      try {
        ListChoice choice = (ListChoice)value;
        myItem_.setLeafName(choice.display); 
        myItem_.setColor(choice.myColor);
        myItem_.setChecked(choice.isSelected);
        myItem_.setLocked(choice.isLocked);
        Color mainColor = (!myEnabled_) ? Color.lightGray : Color.black;
        Color subColor = null;
        if (showSubChecks_) {
          myItem_.setSubChecked(choice.isSubSelected);
          if (enableSubChecks_) {
            subColor = (choice.isSelected) ? mainColor : Color.lightGray;
          } else {
            subColor = Color.lightGray;
          }
        }
        myItem_.setTextColor(mainColor, subColor);
      } catch (Exception ex) {
        ehs_.getExceptionHandler().displayException(ex);
      }
      return (myItem_);
    }
  }

  /****************************************************************************
  **
  ** Component returned for a list element
  */  
  
  public class CheckBoxItem extends JPanel {

    private boolean isChecked_;
    private boolean isSubChecked_;
    private boolean isLocked_;
    private Color myColor_; 
    private String myName_;
    private JLabel myLabel_;
    private CheckBox myCheckbox_;
    private DotBox mySubCheckbox_;
    private static final long serialVersionUID = 1L;
   
    public CheckBoxItem(boolean doSubAlso) {
      
      isChecked_ = false;
      isSubChecked_ = false;
      myColor_ = Color.white; 
      myName_ = "";
    
      setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
    
      myCheckbox_ = new CheckBox(ehs_);
      myCheckbox_.setPreferredSize(new Dimension(BOX_WIDTH_, BOX_WIDTH_));
      if (doSubAlso) {
        mySubCheckbox_ = new DotBox(ehs_);
        mySubCheckbox_.setPreferredSize(new Dimension(BOX_WIDTH_, BOX_WIDTH_));    
      }
      myLabel_ = new JLabel(myName_);
 
      int colNum = 0;
      UiUtil.gbcSet(gbc, colNum++, 0, 1, 1, UiUtil.NONE, 0, 0, 0, BOX_MARGIN_, 0, 0, UiUtil.CEN, 0.0, 0.0);       
      add(myCheckbox_, gbc);
      
      if (doSubAlso) {
        UiUtil.gbcSet(gbc, colNum++, 0, 1, 1, UiUtil.NONE, 0, 0, 0, INTER_BOX_MARGIN_, 0, 0, UiUtil.CEN, 0.0, 0.0);       
        add(mySubCheckbox_, gbc);
      }
    
      UiUtil.gbcSet(gbc, colNum, 0, 5, 1, UiUtil.HOR, 0, 0, 0, BOX_MARGIN_, 0, 5, UiUtil.CEN, 1.0, 0.0);
      add(myLabel_, gbc); 
      setBackground(myColor_);
      
    }
    
    public void setLeafName(String name) {
      myName_ = name;
      myLabel_.setText(myName_);
      return;
    }
    
    public void setColor(Color color) {
      myColor_ = color;
      setBackground(myColor_);
      return;
    }
    
    public void setTextColor(Color color, Color subColor) {
      myLabel_.setForeground(color);
      myCheckbox_.setBoxColor(color);
      if (mySubCheckbox_ != null) {
        mySubCheckbox_.setBoxColor(subColor);
      }
      return;
    }    
   
    public void setChecked(boolean isChecked) {
      isChecked_ = isChecked;
      myCheckbox_.setChecked(isChecked);
      return;      
    }
    
    public void setLocked(boolean isLocked) {
      isLocked_ = isLocked;
      myCheckbox_.setLocked(isLocked);
      return;      
    }
    
    
    public void setSubChecked(boolean isSubChecked) {
      if (mySubCheckbox_ == null) {
        throw new IllegalStateException();
      }
      isSubChecked_ = isSubChecked;
      mySubCheckbox_.setChecked(isSubChecked);
      return;      
    }
  }
  
  /****************************************************************************
  **
  ** Component returned for a list element
  */  
  
  public static class ListChoice {
    public boolean isSelected;
    public boolean isSubSelected;
    public boolean isLocked;
    private Object myObj_;
    public String display;
    public Color myColor;
   
    public ListChoice(Object obj, String display, Color myColor, boolean isSelected) {     
      this(obj, display, myColor, isSelected, false);   
    }
    
    public ListChoice(Object obj, String display, Color myColor, boolean isSelected, boolean isSubSelected) {  
      this(obj, display, myColor, isSelected, isSubSelected, false);
    }
      public ListChoice(Object obj, String display, Color myColor, 
                        boolean isSelected, boolean isSubSelected, boolean isLocked) {     
      this.isSelected = isSelected;
      this.isSubSelected = isSubSelected;
      this.myObj_ = obj; 
      this.display = display;
      this.myColor = myColor;
      this.isLocked = isLocked;
    } 
      
    public Object getObject() {     
      return (myObj_);   
    }   
    
    public String getObjectAsString() {     
      return ((String)myObj_);   
    }   
      
    public String toString() {     
      return (display);   
    }    
  }  
    
  /****************************************************************************
  **
  ** Actual check box
  */  
  
  public static class CheckBox extends JPanel {

    private boolean isChecked_;
    private Color boxColor_;
    private boolean isLocked_;
    private static final long serialVersionUID = 1L;
    private HandlerAndManagerSource cbehs_;
   
    public CheckBox(HandlerAndManagerSource cbehs) {      
      isChecked_ = false;
      isLocked_ = false;
      boxColor_ = Color.black;
      setBackground(Color.white);
      cbehs_ = cbehs;
    }
    
    public void paintComponent(Graphics g) {
      try {
        super.paintComponent(g);
        Dimension size = getSize();
        Graphics2D g2 = (Graphics2D)g;   
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        Rectangle2D rect = new Rectangle2D.Double(0.0, 0.0, size.getWidth() - 1, size.getHeight() - 1);
        g2.setPaint((isLocked_) ? Color.gray : boxColor_);
        g2.draw(rect);
        if (isChecked_) {
          GeneralPath currPath = new GeneralPath();  
          currPath.moveTo((float)0.0, (float)0.0);
          currPath.lineTo((float)size.getWidth() - 1.0F, (float)size.getHeight() - 1.0F);
          currPath.moveTo((float)0.0, (float)size.getHeight() - 1.0F);
          currPath.lineTo((float)size.getWidth() - 1.0F, (float)0.0);          
          g2.draw(currPath);
        }
      } catch (Exception ex) {
        cbehs_.getExceptionHandler().displayPaintException(ex);
      }
      return;
    }    

    public void setChecked(boolean isChecked) {
      isChecked_ = isChecked;
      return;      
    } 
    
    public void setBoxColor(Color boxColor) {
      boxColor_ = boxColor;
      return;      
    } 
    
    public void setLocked(boolean isLocked) {
      isLocked_ = isLocked;
      return;      
    }    
    
  }  
  
  /****************************************************************************
  **
  ** Actual check box
  */  
  
  public static class DotBox extends JPanel {

    private boolean isChecked_;
    private Color boxColor_;
    private static final long serialVersionUID = 1L;
    private HandlerAndManagerSource cbehs_;
   
    public DotBox(HandlerAndManagerSource cbehs) {      
      isChecked_ = false;
      boxColor_ = Color.black;
      setBackground(Color.white);
      cbehs_ = cbehs;
    }
    
    public void paintComponent(Graphics g) {
      try {
        super.paintComponent(g);      
        Dimension size = getSize();
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setPaint(boxColor_);
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        Ellipse2D circ = new Ellipse2D.Double(1.0, 1.0, size.getWidth() - 2, size.getHeight() - 2);   
        g2.draw(circ);
        if (isChecked_) {
          Ellipse2D circ2 = new Ellipse2D.Double(3.0, 3.0, 5.0, 5.0);   
          g2.fill(circ2);
        }
      } catch (Exception ex) {
        cbehs_.getExceptionHandler().displayPaintException(ex);
      }
      return;
    }    

    public void setChecked(boolean isChecked) {
      isChecked_ = isChecked;
      return;      
    } 
    
    public void setBoxColor(Color boxColor) {
      boxColor_ = boxColor;
      return;      
    }    
    
  } 
  
  /****************************************************************************
  **
  ** For optional real-time listening
  */  
  
  public interface CheckBoxListListener {
    public void checkIsClicked(); 
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Test frame
  */

  public static void main(String[] argv) {
    try {
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      JFrame testWindow = new JFrame();
      testWindow.setSize(640, 480);    
      Dimension frameSize = testWindow.getSize();
      int x = (screenSize.width - frameSize.width) / 2;
      int y = (screenSize.height - frameSize.height) / 2;
      testWindow.setLocation(x, y);
      JPanel cp = (JPanel)testWindow.getContentPane();
      cp.setBorder(new EmptyBorder(20, 20, 20, 20));
      cp.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();  
           
      Vector<ListChoice> vec = new Vector<ListChoice>();
      vec.add(new ListChoice("1", "fie", Color.yellow, false));
      vec.add(new ListChoice("2", "fi", Color.orange, false));
      vec.add(new ListChoice("3", "fo", Color.blue, false));
      vec.add(new ListChoice("4", "fum", Color.green, false));

      CheckBoxList cbt = new CheckBoxList(vec, null);
      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      cp.add(cbt, gbc);        
      testWindow.setVisible(true);
    } catch (Exception ioex) {
      System.err.println(ioex); 
    }
    return;
  }
}