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
 

package org.systemsbiology.biotapestry.app;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.ViewExporter;
import org.systemsbiology.biotapestry.ui.dialogs.SIFImportChoicesDialogFactory;
import org.systemsbiology.biotapestry.util.DirectoryNamedOutputStreamSource;
import org.systemsbiology.biotapestry.util.ModelNodeIDPair;
import org.systemsbiology.biotapestry.util.NodeRegionModelNameTuple;
import org.systemsbiology.biotapestry.util.WebPublisher;

/****************************************************************************
**
** Used to test and demo the in-process usage of the ImageGeneratorApplication
*/

public class ImageGeneratorAppTestWrapper {
   
  public static void main(String argv[]) {
    //main1(argv);
    main2(argv);
  }
  
  public static void main1(String argv[]) {
      
    InputStream is = null;
    DirectoryNamedOutputStreamSource noss = null;
       
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
   
    try {
      is = new FileInputStream(argv[0]);
      File targetDir = new File(argv[1]);
      noss = new DirectoryNamedOutputStreamSource(targetDir);
    } catch (IOException ioex) {
      System.err.println("IO Failure");
      System.exit(1);
    }   
        
    ImageGeneratorApplication iga1 = null;
    try {      
      iga1 = new ImageGeneratorApplication(ImageGeneratorApplication.CSV_INPUT, is, null,
                                           ImageGeneratorApplication.BTP_OUTPUT, baos,
                                           SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT,
                                           new Integer(NetOverlayProperties.RELAYOUT_NO_CHANGE),                                        
                                           new Boolean(true));
      iga1.process();
    } catch (ImageGeneratorApplication.GeneratorException gex) {
      System.err.println("Failure: " + gex.getMessage() + " exception: " + gex.getWrappedException());
      System.exit(1);
    }      
  
    System.out.println("-----------------Model Map------------------");
    Map<String, String> modelIDMap = iga1.getModelIDMap();
    Iterator<String> kit = modelIDMap.keySet().iterator();
    while (kit.hasNext()) {
      String csvModelName = kit.next();
      String modelInternalID = modelIDMap.get(csvModelName);
      System.out.println("  CSV Model: " + csvModelName + " -> InternalID: " + modelInternalID);
    }

    System.out.println("-----------------Node  Map------------------");
    Map<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap = iga1.getNodeIDMap();
    Iterator<NodeRegionModelNameTuple> nkit = nodeIDMap.keySet().iterator();
    while (nkit.hasNext()) {
      NodeRegionModelNameTuple nrmTuple = nkit.next();
      ModelNodeIDPair internalID = nodeIDMap.get(nrmTuple);
      String csvModelName = nrmTuple.getModelName();
      String csvRegionName = nrmTuple.getRegionName();  // null for top model
      String csvNodeName = nrmTuple.getNodeName();
      String modelID = internalID.getModelID();
      String nodeID = internalID.getNodeID();
      System.out.println("  CSV Model/Region/Node: " + csvModelName + "/" + csvRegionName + "/" + csvNodeName + 
                         " -> Internal Model/Node: " + modelID + "/" + nodeID);
    }
        
    HashSet<WebPublisher.ModelScale> keysToPublish = new HashSet<WebPublisher.ModelScale>();
    String mod1 = modelIDMap.get("ENDOMESODERM");
    keysToPublish.add(new WebPublisher.ModelScale(mod1, WebPublisher.ModelScale.SMALL));
    String mod2 = modelIDMap.get("ENDO");
    keysToPublish.add(new WebPublisher.ModelScale(mod2, WebPublisher.ModelScale.SMALL));
 
    
    byte[] btpBytes = baos.toByteArray();
    InputStream bais = new ByteArrayInputStream(btpBytes);
    ImageGeneratorApplication iga2 = null;
    try {      
      iga2 = new ImageGeneratorApplication(ImageGeneratorApplication.BTP_INPUT, bais, null, noss, keysToPublish, null, null, null);
      iga2.process();
    } catch (ImageGeneratorApplication.GeneratorException gex) {
      System.err.println("Failure: " + gex.getMessage() + " exception: " + gex.getWrappedException());
      System.exit(1);
    }         

    System.out.println("-----------------Intersection  Map------------------");
    Map intersectMap = iga2.getIntersectionMap();
    Iterator ikit = intersectMap.keySet().iterator();
    while (ikit.hasNext()) {
      WebPublisher.ModelScale mScale = (WebPublisher.ModelScale)ikit.next();
      String modelID = mScale.getModelID();
      String scale = WebPublisher.ModelScale.getSizeTag(mScale.getSize());
      String fileName = mScale.getFileName();
      System.out.println("----Model/Scale: " + modelID + "/" + scale + " (filename: " + fileName + ")");     
      ViewExporter.BoundsMaps bMaps = (ViewExporter.BoundsMaps)intersectMap.get(mScale);      
      Iterator bkit = bMaps.nodeBounds.keySet().iterator();
      while (bkit.hasNext()) {
        String nodeID = (String)bkit.next();
        Rectangle rect = (Rectangle)bMaps.nodeBounds.get(nodeID);
        System.out.println("  NodeID: " + nodeID + " -> Rectangle: " + rect);
      }     
    }      
    return;
  }
  
  public static void main2(String argv[]) {
      
    InputStream isb = null;
    InputStream isc = null;
    DirectoryNamedOutputStreamSource noss = null;
       
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
   
    try {
      isb = new FileInputStream(argv[0]);
      isc = new FileInputStream(argv[1]);
      File targetDir = new File(argv[2]);
      noss = new DirectoryNamedOutputStreamSource(targetDir);
    } catch (IOException ioex) {
      System.err.println("IO Failure");
      System.exit(1);
    }   
        
    ImageGeneratorApplication iga1 = null;
    try {      
      iga1 = new ImageGeneratorApplication(ImageGeneratorApplication.BTP_PRE_INPUT_CSV_PRUNED, isb, isc,
                                           ImageGeneratorApplication.BTP_OUTPUT, baos, 
                                           SIFImportChoicesDialogFactory.LayoutModes.INCREMENTAL,
                                           new Integer(NetOverlayProperties.RELAYOUT_NO_CHANGE),     
                                           new Boolean(true));
      iga1.process();
    } catch (ImageGeneratorApplication.GeneratorException gex) {
      System.err.println("Failure: " + gex.getMessage() + " exception: " + gex.getWrappedException());
      System.exit(1);
    }      
  
    System.out.println("-----------------Model Map------------------");
    Map<String, String> modelIDMap = iga1.getModelIDMap();
    Iterator<String> kit = modelIDMap.keySet().iterator();
    while (kit.hasNext()) {
      String csvModelName = kit.next();
      String modelInternalID = modelIDMap.get(csvModelName);
      System.out.println("  CSV Model: " + csvModelName + " -> InternalID: " + modelInternalID);
    }

    System.out.println("-----------------Node  Map------------------");
    Map<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap = iga1.getNodeIDMap();
    Iterator<NodeRegionModelNameTuple> nkit = nodeIDMap.keySet().iterator();
    while (nkit.hasNext()) {
      NodeRegionModelNameTuple nrmTuple = nkit.next();
      ModelNodeIDPair internalID = nodeIDMap.get(nrmTuple);
      String csvModelName = nrmTuple.getModelName();
      String csvRegionName = nrmTuple.getRegionName();  // null for top model
      String csvNodeName = nrmTuple.getNodeName();
      String modelID = internalID.getModelID();
      String nodeID = internalID.getNodeID();
      System.out.println("  CSV Model/Region/Node: " + csvModelName + "/" + csvRegionName + "/" + csvNodeName + 
                         " -> Internal Model/Node: " + modelID + "/" + nodeID);
    }
        
    HashSet<WebPublisher.ModelScale> keysToPublish = new HashSet<WebPublisher.ModelScale>();
    String mod1 = (String)modelIDMap.get("ENDOMESODERM");
    keysToPublish.add(new WebPublisher.ModelScale(mod1, WebPublisher.ModelScale.SMALL));
    String mod2 = (String)modelIDMap.get("ENDO");
    keysToPublish.add(new WebPublisher.ModelScale(mod2, WebPublisher.ModelScale.SMALL));
 
    
    byte[] btpBytes = baos.toByteArray();
    InputStream bais = new ByteArrayInputStream(btpBytes);
    ImageGeneratorApplication iga2 = null;
    try {      
      iga2 = new ImageGeneratorApplication(ImageGeneratorApplication.BTP_INPUT, bais, null, 
                                           noss, keysToPublish, null, null, null);
      iga2.process();
    } catch (ImageGeneratorApplication.GeneratorException gex) {
      System.err.println("Failure: " + gex.getMessage() + " exception: " + gex.getWrappedException());
      System.exit(1);
    }         

    System.out.println("-----------------Intersection  Map------------------");
    Map intersectMap = iga2.getIntersectionMap();
    Iterator<WebPublisher.ModelScale> ikit = intersectMap.keySet().iterator();
    while (ikit.hasNext()) {
      WebPublisher.ModelScale mScale = ikit.next();
      String modelID = mScale.getModelID();
      String scale = WebPublisher.ModelScale.getSizeTag(mScale.getSize());
      String fileName = mScale.getFileName();
      System.out.println("----Model/Scale: " + modelID + "/" + scale + " (filename: " + fileName + ")");     
      ViewExporter.BoundsMaps bMaps = (ViewExporter.BoundsMaps)intersectMap.get(mScale);      
      Iterator bkit = bMaps.nodeBounds.keySet().iterator();
      while (bkit.hasNext()) {
        String nodeID = (String)bkit.next();
        Rectangle rect = (Rectangle)bMaps.nodeBounds.get(nodeID);
        System.out.println("  NodeID: " + nodeID + " -> Rectangle: " + rect);
      }     
    }      
    return;
  }
}