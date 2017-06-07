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

package org.systemsbiology.biotapestry.plugin.examples;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.plugin.SimulatorPlugIn;
import org.systemsbiology.biotapestry.simulation.ModelExpressionEntry;
import org.systemsbiology.biotapestry.simulation.ModelLink;
import org.systemsbiology.biotapestry.simulation.ModelNode;
import org.systemsbiology.biotapestry.simulation.ModelRegion;
import org.systemsbiology.biotapestry.simulation.ModelRegionTopologyForTime;
import org.systemsbiology.biotapestry.simulation.ModelSource;
import org.systemsbiology.biotapestry.util.UiUtil;

public class SimulatorStubPlugIn extends JFrame implements SimulatorPlugIn {
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
	
  private static String menuName_ = "Simulator Example";     
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private ModelSource mSrc_;
  private JEditorPane descriptionPane_;
  private JPanel embed_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SimulatorStubPlugIn()  {
    super("Simulation Stub");
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setSize(1024, 1024);    

    Dimension frameSize = getSize();
    int x = (screenSize.width - frameSize.width) / 2;
    int y = (screenSize.height - frameSize.height) / 2;
    setLocation(x, y);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    

    //
    // Build the username panel:
    //
    embed_ = new JPanel();
    
    int rowNum = 0;
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 6, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    rowNum += 6;
    cp.add(embed_, gbc);
       
    String redirect = "Simulator Stub!"; 
    descriptionPane_ = new JEditorPane("text/plain", "");
    JScrollPane jsp = new JScrollPane(descriptionPane_);
    descriptionPane_.setEditable(false);
    descriptionPane_.setText(redirect);
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.3);
    rowNum += 2;
    cp.add(jsp, gbc);

    //
    // Build the button panel:
    //

    JButton buttonConvert = new JButton("Convert model");
    buttonConvert.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          
        } catch (Exception ex) {
          System.err.println("Caught exception");
           ex.printStackTrace();
        }
      }
    });
    
    UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    cp.add(buttonConvert, gbc);    
    
    JButton buttonN = new JButton("List Nodes");
    buttonN.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          listNodes();
        } catch (Exception ex) {
          System.err.println("Caught exception");
        }
      }
    });
    
    UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    cp.add(buttonN, gbc);        
    
    JButton buttonL = new JButton("List Links");
    buttonL.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          listLinks();
        } catch (Exception ex) {
          System.err.println("Caught exception");
          ex.printStackTrace();
        }
      }
    });
    
    UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    cp.add(buttonL, gbc);    
    
    JButton buttonT = new JButton("List Topology");
    buttonT.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          listTopology();
        } catch (Exception ex) {
          System.err.println("Caught exception");
           ex.printStackTrace();
        }
      }
    });
    
    UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    cp.add(buttonT, gbc); 
    
    JButton buttonR = new JButton("List Regions");
    buttonR.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          listLineage();
        } catch (Exception ex) {
          System.err.println("Caught exception");
           ex.printStackTrace();
        }
      }
    });
    
    UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    cp.add(buttonR, gbc);    
    
    JButton buttonX = new JButton("List Expressions");
    buttonX.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          listExpression();
        } catch (Exception ex) {
          System.err.println("Caught exception");
           ex.printStackTrace();
        }
      }
    });
    
    UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    cp.add(buttonX, gbc);    
    
    JButton buttonO = new JButton("Close this Window");
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          SimulatorStubPlugIn.this.setVisible(false);
          SimulatorStubPlugIn.this.dispose();
        } catch (Exception ex) {
          System.err.println("Caught exception");
        }
      }
    });     

    UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    cp.add(buttonO, gbc);  
    
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {

        dispose();
      }
    });
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Launch the engine
  */
  
  public void launch() {
    // important to call this whenever embedding a PApplet.
    // It ensures that the animation thread is started and
    // that other internal variables are properly set.
       
	setVisible(true);
  }
  
  /***************************************************************************
  **
  ** Set the model source
  */  
   
  public void setModelSource(ModelSource mSrc) {
    mSrc_ = mSrc;
    return;
  }
  
  /***************************************************************************
  **
  ** Provide results
  */
  
  public Map<String, List<ModelExpressionEntry>> provideResults() {
    HashMap<String, List<ModelExpressionEntry>> results = new HashMap<String, List<ModelExpressionEntry>>();
    SortedSet<String> mrl = new TreeSet<String>(mSrc_.getExpressionGenes());    
    SortedSet<Integer> times = mSrc_.getExpressionTimes();
    List<ModelRegion> mrs = mSrc_.getRegions();
    ArrayList<String> mrn = new ArrayList<String>();
    for (ModelRegion mr : mrs) {
      mrn.add(mr.getRegionName());
    }
    for (String gene : mrl) {
      ArrayList<ModelExpressionEntry> meelist = new ArrayList<ModelExpressionEntry>();
      results.put(gene, meelist);
      for (String reg : mrn) {
        for (Integer time : times) {
          ModelExpressionEntry mre = mSrc_.getExpressionEntry(gene, reg, time.intValue());
          Double var = (mre.getLevel().equals(ModelExpressionEntry.Level.VARIABLE)) ? mre.getVariable() : null;
          ModelExpressionEntry.Level myLev = (mre.getLevel().equals(ModelExpressionEntry.Level.EXPRESSED)) ? ModelExpressionEntry.Level.WEAK_EXPRESSION : mre.getLevel();
          meelist.add(new ModelExpressionEntry(reg, time.intValue(), myLev, mre.getSource(), var));
        }
      }
    }
    return (results);    
  }
  
  /***************************************************************************
  **
  ** Returns the menu name
  */  
  public String getMenuName() {
	  return menuName_;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** List the nodes
  */  
   
  private void listNodes() {
    StringBuffer buf = new StringBuffer();
    Iterator<ModelNode> nit = mSrc_.getRootModelNodes();
    while (nit.hasNext()) {
      ModelNode node = nit.next();
      String name = node.getName();
      String id = node.getUniqueInternalID();
      ModelNode.Type type = node.getType();
      buf.append("\"");
      buf.append(name);
      buf.append("\" ");
      buf.append(id);
      buf.append(" ");
      buf.append(type);
      buf.append("\n");
    }
    descriptionPane_.setText(buf.toString());
    return;
  }
  
  /***************************************************************************
  **
  ** List the nodes
  */  
   
  private void listLinks() {
    StringBuffer buf = new StringBuffer();
    Iterator<ModelLink> lit = mSrc_.getRootModelLinks();
    while (lit.hasNext()) {
      ModelLink link = lit.next();
      String src = link.getSrc();
      String trg = link.getTrg();
      String id = link.getUniqueInternalID();
      ModelLink.Sign sign = link.getSign();
      buf.append("\"");
      buf.append(mSrc_.getNode(src).getName());
      buf.append("\" \"");
      buf.append(mSrc_.getNode(trg).getName());
      buf.append("\" ");
      buf.append(id);
      buf.append(" ");
      buf.append(sign);
      buf.append("\n");
    }
    descriptionPane_.setText(buf.toString());
    return;
  }
  
  /***************************************************************************
  **
  ** List the nodes
  */  
   
  private void listTopology() {
    StringBuffer buf = new StringBuffer();
    Iterator<ModelRegionTopologyForTime> lit = mSrc_.getRegionTopology();
    while (lit.hasNext()) {
      ModelRegionTopologyForTime topo = lit.next();
      int minTime = topo.getMinTime();
      int maxTime = topo.getMaxTime();
      buf.append("--------------------------TIME: min = ");
      buf.append(minTime);
      buf.append(" max = ");
      buf.append(maxTime);
      buf.append("\n"); 
      buf.append("REGIONS:\n"); 
      Iterator<String> rit = topo.getRegions();
      while (rit.hasNext()) {
        buf.append("Region: ");
        buf.append(rit.next());
        buf.append("\n"); 
      }
      buf.append("LINKS:\n"); 
      Iterator<ModelRegionTopologyForTime.TopoLink> tlit = topo.getLinks();
      while (tlit.hasNext()) {
        ModelRegionTopologyForTime.TopoLink tlink = tlit.next();
        buf.append("Link: ");
        buf.append(tlink.getRegion1());
        buf.append(" to: ");
        buf.append(tlink.getRegion2());
        buf.append("\n"); 
      }
    }
    descriptionPane_.setText(buf.toString());
    return;
  }
  
  /***************************************************************************
  **
  ** List the nodes
  */  
   
  private void listLineage() {
    StringBuffer buf = new StringBuffer(); 
    List<ModelRegion> mrl = mSrc_.getRegions();
    for (ModelRegion mr : mrl) {
      dumpAReg(mr, buf);
    }
    descriptionPane_.setText(buf.toString());
    return;
  }
  
  /***************************************************************************
  **
  ** List the nodes
  */  	
   
  private void dumpAReg(ModelRegion mr, StringBuffer buf) {
    buf.append(mr.getRegionName());
    buf.append(": minTime = ");
    buf.append(mr.getRegionStart());
    buf.append(" maxTime = ");
    buf.append(mr.getRegionEnd());
    buf.append(" lineage:");
    List<String> lin = mr.getLineage();
    for (String lr : lin) {
      buf.append(" \"");
      buf.append(lr);
      buf.append("\"");
    }
    buf.append("\n");
    return;
  }

  /***************************************************************************
  **
  ** List Expression. 
  ** Note:
  ** GeNeTool codes: 0 = No data, 1 = No expression, 2 = Weak Expression , 3 = Expression, 4 = Maternal
  */  
   
  private void listExpression() {
    StringBuffer buf = new StringBuffer(); 
    SortedSet<String> mrl = new TreeSet<String>(mSrc_.getExpressionGenes());    
    SortedSet<Integer> times = mSrc_.getExpressionTimes();
    List<ModelRegion> mrs = mSrc_.getRegions();
    ArrayList<String> mrn = new ArrayList<String>();
    for (ModelRegion mr : mrs) {
      mrn.add(mr.getRegionName());
    }
    for (String gene : mrl) {
      buf.append(gene);
      buf.append(":\n");
      for (String reg : mrn) {
        buf.append("  ");
        buf.append(reg);
        buf.append(":");
        for (Integer time : times) {
          ModelExpressionEntry mre = mSrc_.getExpressionEntry(gene, reg, time.intValue());
          if (mre != null) {
            buf.append(" ");
            buf.append(mre.getTime());
            buf.append(":");
            buf.append(mre.getLevel());
          }
        }
        buf.append("\n");
      }
    }
    descriptionPane_.setText(buf.toString());
    return;
  }
}