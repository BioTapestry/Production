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

package org.systemsbiology.biotapestry.plugin;

import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Map;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.parser.SUParser;
import org.systemsbiology.biotapestry.app.ArgParser;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;

/****************************************************************************
**
** Plugin Manager.
*/

public class PlugInManager {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList<DataDisplayPlugIn> dataDisplay_;
  private ArrayList<DataDisplayPlugIn> linkDataDisplay_;
  private ArrayList<SimulatorPlugIn> engines_;
  private ArrayList<ModelBuilderPlugIn> builders_;
  private int maxCount_;
  private int maxLinkCount_;
  private int maxEngineCount_;
  private int maxBuilderCount_;
  private TreeSet<AbstractPlugInDirective> directives_;
  private TreeSet<LinkPlugInDirective> linkDirectives_;
  private TreeSet<SimulatorPlugInDirective> simDirectives_;
  private TreeSet<ModelBuilderPlugInDirective> mbDirectives_;
  private DynamicDataAccessContext ddacx_;
  private UIComponentSource uics_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Basic Constructor
  */

  public PlugInManager(DynamicDataAccessContext ddacx, UIComponentSource uics) {
    uics_ = uics;
    ddacx_ = ddacx;
    dataDisplay_ = new ArrayList<DataDisplayPlugIn>();
    linkDataDisplay_ = new ArrayList<DataDisplayPlugIn>();
    linkDirectives_ = new TreeSet<LinkPlugInDirective>();
    directives_ = new TreeSet<AbstractPlugInDirective>();
    simDirectives_ = new TreeSet<SimulatorPlugInDirective>();
    mbDirectives_ = new TreeSet<ModelBuilderPlugInDirective>();
    maxCount_ = Integer.MIN_VALUE;
    maxLinkCount_ = Integer.MIN_VALUE;
    engines_ = new ArrayList<SimulatorPlugIn>();
    builders_ = new ArrayList<ModelBuilderPlugIn>();
    maxEngineCount_ = Integer.MIN_VALUE;
    maxBuilderCount_ = Integer.MIN_VALUE;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Add a directive for nodes (can be either legacy or modern)
  */

  public void addDirective(AbstractPlugInDirective dir) {
    directives_.add(dir);
    int order = dir.getOrder();
    if (maxCount_ < order) {
      maxCount_ = order;
    }
    return;
  }   
  
  /***************************************************************************
  ** 
  ** Add a directive
  */

  public void addLinkDirective(LinkPlugInDirective dir) {
    linkDirectives_.add(dir);
    int order = dir.getOrder();
    if (maxLinkCount_ < order) {
      maxLinkCount_ = order;
    }
    return;
  }   
  
  /***************************************************************************
  ** 
  ** Add a directive
  */

  public void addSimDirective(SimulatorPlugInDirective dir) {
    simDirectives_.add(dir);
    int order = dir.getOrder();
    if (maxEngineCount_ < order) {
      maxEngineCount_ = order;
    }
    return;
  }   

  /***************************************************************************
  ** 
  ** Add a directive
  */

  public void addModelBuilderDirective(ModelBuilderPlugInDirective dir) {
    mbDirectives_.add(dir);
    int order = dir.getOrder();
    if (maxBuilderCount_ < order) {
      maxBuilderCount_ = order;
    }
    return;
  } 
   
  /***************************************************************************
  ** 
  ** Get an iterator over live node data display plugins (class DataDisplayPlugIn)
  */

  public Iterator<DataDisplayPlugIn> getDataDisplayPlugIns() {
    return (dataDisplay_.iterator());
  }  
  
  /***************************************************************************
  ** 
  ** Get an iterator over live link data display plugins (class DataDisplayPlugIn)
  */

  public Iterator<DataDisplayPlugIn> getLinkDataDisplayPlugIns() {
    return (linkDataDisplay_.iterator());
  } 
  
  /***************************************************************************
  ** 
  ** Load data display plugins
  */

  public boolean loadDataDisplayPlugIns(Map<String, Object> args) {
    
    //
    // Load in the plugins specified in the resource file first:
    //

    if (!readPlugInListing()) {
      return (false);
    }
    
    //
    // Now load from jar file, if specified in command line argument:
    //
        
    String plugDirStr = (String)args.get(ArgParser.PLUG_IN_DIR);
    if (plugDirStr != null) {
      File plugDirectory = new File(plugDirStr);
      if (!plugDirectory.exists() || !plugDirectory.isDirectory() || !plugDirectory.canRead()) {
        return (false);
      }
      if (!readJarFiles(plugDirectory, maxCount_ + 1, maxLinkCount_ + 1, maxEngineCount_ + 1, maxBuilderCount_ + 1)) {
         return (false);
      }
    }
    
    Iterator<AbstractPlugInDirective> drit = directives_.iterator();
    while (drit.hasNext()) {
      // May be either legacy type or modern type:
      AbstractPlugInDirective pid = drit.next();
      DataDisplayPlugIn pi = (DataDisplayPlugIn)pid.buildPlugIn();
      if (pi != null) {
        if (pi instanceof InternalDataDisplayPlugInV2) {
          ((InternalDataDisplayPlugInV2)pi).setDataAccessContext(ddacx_, uics_);
        }     
        dataDisplay_.add(pi);
      }
    }
    
    Iterator<LinkPlugInDirective> ldrit = linkDirectives_.iterator();
    while (ldrit.hasNext()) {
      LinkPlugInDirective pid = ldrit.next();
      DataDisplayPlugIn pi = (DataDisplayPlugIn)pid.buildPlugIn();
      if (pi != null) {
        if (pi instanceof InternalDataDisplayPlugInV2) {
          ((InternalDataDisplayPlugInV2)pi).setDataAccessContext(ddacx_, uics_);
        }     
        linkDataDisplay_.add(pi);
      }
    }
    
    Iterator<SimulatorPlugInDirective> spdit = simDirectives_.iterator();
    while (spdit.hasNext()) {
      SimulatorPlugInDirective pid = spdit.next();
      SimulatorPlugIn pi = (SimulatorPlugIn)pid.buildPlugIn();
      if (pi != null) {
        engines_.add(pi);
      }
    }
    
    Iterator<ModelBuilderPlugInDirective> mbdit = mbDirectives_.iterator();
    while (mbdit.hasNext()) {
      ModelBuilderPlugInDirective pid = mbdit.next();
      ModelBuilderPlugIn pi = (ModelBuilderPlugIn)pid.buildPlugIn();
      if (pi != null) {
        pi.setDataAccessContext(ddacx_);   
        builders_.add(pi);
        ddacx_.getRMan().addBundleForPlugin(pi.getPluginResources());
      }
    }
    return (true);
  }

  /***************************************************************************
  ** 
  ** Get simulator plugin
  */
  
  public SimulatorPlugIn getSimulatorPlugin(int engineIndex) {
    return (engines_.get(engineIndex));
  } 
  
  /***************************************************************************
  ** 
  ** Get iterator for simulator plugins
  */
  
  public Iterator<SimulatorPlugIn> getEngineIterator() {
	  return engines_.iterator();
  }
  
  /***************************************************************************
  ** 
  ** Get  builder plugin
  */
  
  public ModelBuilderPlugIn getBuilderPlugin(int mbIndex) {
    return (builders_.get(mbIndex));
  } 
  
  /***************************************************************************
  ** 
  ** Get iterator for builder plugins
  */
  
  public Iterator<ModelBuilderPlugIn> getBuilderIterator() {
    return (builders_.iterator());
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Read jars
  */

  private boolean readJarFiles(File plugInDir, int currMax, int currLinkMax, int currSimMax, int currMbMax) {
    try {
      ExtensionFilter filter = new ExtensionFilter(".jar");
      if (plugInDir.isDirectory()) {
        File[] files = plugInDir.listFiles(filter);
        for (int i = 0; i < files.length; i++) {
          JarFile jar = new JarFile(files[i]);
          // Obsolete:
          //Manifest manifest = jar.getManifest();
          //Attributes attrib = manifest.getMainAttributes();
          //String plugin = attrib.getValue("Plugin-Class");
          
          //
          // Legacy Services (obsolete node plugins):
          //
          
          List<String> sl = getServiceList(jar, "org.systemsbiology.biotapestry.plugin.ExternalDataDisplayPlugIn");
          int numSvc = sl.size();
          for (int j = 0; j < numSvc; j++) {
            String plugin = sl.get(j);
            PlugInDirective pid = new PlugInDirective(PlugInDirective.EXTERNAL_DATA_DISPLAY_TAG, 
                                                      plugin, Integer.toString(currMax++), files[i]);
            addDirective(pid);
          }
          sl = getServiceList(jar, "org.systemsbiology.biotapestry.plugin.InternalDataDisplayPlugIn");
          numSvc = sl.size();
          for (int j = 0; j < numSvc; j++) {
            String plugin = sl.get(j);
            PlugInDirective pid = new PlugInDirective(PlugInDirective.INTERNAL_DATA_DISPLAY_TAG, 
                                                      plugin, Integer.toString(currMax++), files[i]); 
            addDirective(pid);
          }
          
          //
          // Modern node plugins:
          //        
          
          sl = getServiceList(jar, "org.systemsbiology.biotapestry.plugin.ExternalNodeDataDisplayPlugIn");
          numSvc = sl.size();
          for (int j = 0; j < numSvc; j++) {
            String plugin = sl.get(j);
            NodePlugInDirective pid = new NodePlugInDirective(NodePlugInDirective.EXTERNAL_NODE_DATA_DISPLAY_TAG, 
                                                      plugin, Integer.toString(currMax++), files[i]);
            addDirective(pid);
          }
          sl = getServiceList(jar, "org.systemsbiology.biotapestry.plugin.InternalNodeDataDisplayPlugIn");
          numSvc = sl.size();
          for (int j = 0; j < numSvc; j++) {
            String plugin = sl.get(j);
            NodePlugInDirective pid = new NodePlugInDirective(NodePlugInDirective.INTERNAL_NODE_DATA_DISPLAY_TAG, 
                                                      plugin, Integer.toString(currMax++), files[i]); 
            addDirective(pid);
          }
  
          //
          // Link services:
          //
          
          sl = getServiceList(jar, "org.systemsbiology.biotapestry.plugin.ExternalLinkDataDisplayPlugIn");
          numSvc = sl.size();
          for (int j = 0; j < numSvc; j++) {
            String plugin = sl.get(j);
            LinkPlugInDirective pid = new LinkPlugInDirective(LinkPlugInDirective.EXTERNAL_LINK_DATA_DISPLAY_TAG, 
                                                              plugin, Integer.toString(currLinkMax++), files[i]); 
            addLinkDirective(pid);
          }
          sl = getServiceList(jar, "org.systemsbiology.biotapestry.plugin.InternalLinkDataDisplayPlugIn");
          numSvc = sl.size();
          for (int j = 0; j < numSvc; j++) {
            String plugin = sl.get(j);
            LinkPlugInDirective pid = new LinkPlugInDirective(LinkPlugInDirective.INTERNAL_LINK_DATA_DISPLAY_TAG, 
                                                              plugin, Integer.toString(currLinkMax++), files[i]); 
            addLinkDirective(pid);
          }
          
          //
          // Simulator services:
          //
          
          sl = getServiceList(jar, "org.systemsbiology.biotapestry.plugin.SimulatorPlugIn");
          numSvc = sl.size();
          for (int j = 0; j < numSvc; j++) {
            String plugin = sl.get(j);
            SimulatorPlugInDirective spid = new SimulatorPlugInDirective(SimulatorPlugInDirective.SIMULATOR_TAG, 
                                                                         plugin, Integer.toString(currSimMax++), files[i]); 
            this.addSimDirective(spid);
          }
            
          //
          // Builder services:
          //
          
          sl = getServiceList(jar, "org.systemsbiology.biotapestry.plugin.ModelBuilderPlugIn");
          numSvc = sl.size();
          if (numSvc > 1) {
            return (false);
          } else if (numSvc == 1) {
            String plugin = sl.get(0);
            ModelBuilderPlugInDirective mbpid = new ModelBuilderPlugInDirective(ModelBuilderPlugInDirective.MODEL_BUILDER_TAG, 
                                                                                plugin, Integer.toString(currMbMax++), files[i]); 
          
            this.addModelBuilderDirective(mbpid);
          }
        }
      }
    } catch (IOException ioex) {
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Get the list of services
  */

  private List<String> getServiceList(JarFile jar, String svcInterface) throws IOException {
    svcInterface = "META-INF/services/" + svcInterface;
    Enumeration<JarEntry> entries = jar.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      String entryName = entry.getName();
      if (entryName.equals(svcInterface)) {
        return (readClassList(jar, entry));
      }
    }
    return (new ArrayList<String>());
  }

  /***************************************************************************
  **
  ** Get the list of classes from an entry
  */

  private List<String> readClassList(JarFile jar, JarEntry entry) throws IOException {
    ArrayList<String> retval = new ArrayList<String>();
    BufferedReader in = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)));
    String newLine;
    while ((newLine = in.readLine()) != null) {
      newLine = newLine.trim();
      if (newLine.equals("") || newLine.startsWith("#")) {
        continue;
      }
      retval.add(newLine);
    }
    in.close();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Read the plugin listing
  */

  private boolean readPlugInListing() {
    URL url = getClass().getResource("/org/systemsbiology/biotapestry/plugin/plugInListing.xml");
    if (url == null) {
      System.err.println("No plugIn directives file found");
      return (false);
    }
    ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
    alist.add(new PlugInDirectiveFactory(this));
    SUParser sup = new SUParser(ddacx_, alist);
    try {
      sup.parse(url);
    } catch (IOException ioe) {
      System.err.println("Could not read plugIn directives file");
      return (false);              
    }
    return (true);
    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** File filter
  */ 
    
  class ExtensionFilter implements FileFilter {
    
    private String suffix_;
    
    ExtensionFilter(String suffix) {
      suffix_ = suffix;
    }
    
    private boolean hasSuffix(String fileName, String suffix) {
      int fnl = fileName.length();
      int sufl = suffix.length();   
      return ((fnl > sufl) && 
              (fileName.toLowerCase().lastIndexOf(suffix.toLowerCase()) == fnl - sufl));
    }  
  
    public boolean accept(File f) {
      String fileName = f.getName();
      return (hasSuffix(fileName, suffix_));
    }
  }
  
}
