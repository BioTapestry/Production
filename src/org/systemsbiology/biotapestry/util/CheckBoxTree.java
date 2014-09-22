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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeNode;


/****************************************************************************
**
** A JTree that has checkboxes to select
*/

public class CheckBoxTree extends JTree {
                                                          
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  private static final long serialVersionUID = 1L;  
  private HandlerAndManagerSource hams_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public CheckBoxTree(DefaultTreeModel dtm, HandlerAndManagerSource hams) {
    super(dtm);
    hams_ = hams;
    getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    addMouseListener(new MouseHandler());
    setCellRenderer(new CheckBoxCellRenderer());
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
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
        TreePath tp = CheckBoxTree.this.getPathForLocation(x, y);
        if (tp == null) {
          return;
        }
        TreeNode targetNode = (TreeNode)tp.getLastPathComponent();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)targetNode;
        CheckBoxTreeLeaf leafNode = (CheckBoxTreeLeaf)node.getUserObject();
        if (!leafNode.isLocked()) {
          leafNode.setSelected(!leafNode.isSelected());
        }
        CheckBoxTree.this.repaint();
      } catch (Exception ex) {
        hams_.getExceptionHandler().displayException(ex);
      }     
    }
  }
  
  /****************************************************************************
  **
  ** For rendering leaves
  */

  public class CheckBoxCellRenderer extends DefaultTreeCellRenderer {
    
    private static final long serialVersionUID = 1L;
    private CheckBoxLeaf myLeaf_;
    
    public CheckBoxCellRenderer() {
      super();
      myLeaf_ = new CheckBoxLeaf();
    }
    
    public Component getTreeCellRendererComponent(JTree tree, Object value, 
                                                  boolean selected, boolean expanded, 
                                                  boolean leaf, int row, boolean hasFocus) {
      try {
        if (leaf) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
          CheckBoxTreeLeaf leafNode = (CheckBoxTreeLeaf)node.getUserObject();
          if (!leafNode.isDisplayOnly()) {
            myLeaf_.setLeafName(leafNode.getName()); 
            myLeaf_.setColor(leafNode.getColor());
            myLeaf_.setChecked(leafNode.isSelected());
            myLeaf_.setLocked(leafNode.isLocked());
            return (myLeaf_); 
          }
        }
        return (super.getTreeCellRendererComponent(tree, value, selected, expanded, 
                                                   leaf, row, hasFocus));
      } catch (Exception ex) {
        hams_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }  
  }

  /****************************************************************************
  **
  ** Component returned for a leaf
  */  
  
  public class CheckBoxLeaf extends JPanel {

    private static final long serialVersionUID = 1L;
    private boolean isChecked_;
    private boolean isLocked_;
    private Color myColor_; 
    private String myName_;
    private JLabel myLabel_;
    private CheckBox myCheckbox_;
   
    public CheckBoxLeaf() {
      
      isChecked_ = false;
      isLocked_ = false;
      myColor_ = Color.white; 
      myName_ = "";
    
      setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
    
      myCheckbox_ = new CheckBox();
      myCheckbox_.setPreferredSize(new Dimension(10, 10));      
      myLabel_ = new JLabel(myName_);
 
      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 0, 5, 0, 5, UiUtil.CEN, 0.0, 0.0);       
      add(myCheckbox_, gbc);
    
      UiUtil.gbcSet(gbc, 1, 0, 5, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 5, UiUtil.CEN, 1.0, 0.0);
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
  }
  
  /****************************************************************************
  **
  ** Actual check box
  */  
  
  public class CheckBox extends JPanel {

    private static final long serialVersionUID = 1L;
    private boolean isChecked_; 
    private boolean isLocked_;     
   
    public CheckBox() {      
      isChecked_ = false;
      isLocked_ = false;
      setBackground(Color.white);
    }
    
    public void paintComponent(Graphics g) {
      try {
        super.paintComponent(g);
        Dimension size = getSize();
        Graphics2D g2 = (Graphics2D)g;   
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        Rectangle2D rect = new Rectangle2D.Double(0.0, 0.0, size.getWidth(), size.getHeight());   
        g2.setPaint((isLocked_) ? Color.gray : Color.black);
        g2.draw(rect);
        if (isChecked_) {
          GeneralPath currPath = new GeneralPath();  
          currPath.moveTo((float)0.0, (float)0.0);
          currPath.lineTo((float)size.getWidth(), (float)size.getHeight());
          currPath.moveTo((float)0.0, (float)size.getHeight());
          currPath.lineTo((float)size.getWidth(), (float)0.0);          
          g2.draw(currPath);
        }
      } catch (Exception ex) {
        hams_.getExceptionHandler().displayPaintException(ex);
      }
      return;
    }    

    public void setChecked(boolean isChecked) {
      isChecked_ = isChecked;
      return;      
    }  
    
    public void setLocked(boolean isLocked) {
      isLocked_ = isLocked;
      return;      
    }      
    
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

      DefaultMutableTreeNode root = new DefaultMutableTreeNode();
      root.setUserObject("I am rootie");
      DefaultTreeModel dfm = new DefaultTreeModel(root);
      DefaultMutableTreeNode n1 = new DefaultMutableTreeNode();
      n1.setUserObject("world");
      dfm.insertNodeInto(n1, root, 0);
      DefaultMutableTreeNode n2 = new DefaultMutableTreeNode();
      dfm.insertNodeInto(n2, root, 0);
      DefaultMutableTreeNode n3 = new DefaultMutableTreeNode();
      dfm.insertNodeInto(n3, n1, 0);
      DefaultMutableTreeNode n4 = new DefaultMutableTreeNode();
      dfm.insertNodeInto(n4, n1, 0);      
     
      CheckBoxTree cbt = new CheckBoxTree(dfm, null);
      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      cp.add(cbt, gbc);        
      testWindow.setVisible(true);
    } catch (Exception ioex) {
      System.err.println(ioex); 
    }
    return;
  }
}