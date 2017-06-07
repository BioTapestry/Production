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


package org.systemsbiology.biotapestry.util;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;

/****************************************************************************
**
** Helpers for getting a file to use
*/

public class FilePreparer {
  
  private boolean isHeadless_;
  private JFrame topWindow_;
  private Class<?> prefClass_;
  private UIComponentSource uics_;
  private DataAccessContext dacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** For standard file checks
  */

  private static final boolean FILE_MUST_EXIST_DONT_CARE = false;
  private static final boolean FILE_MUST_EXIST           = true;
  
  private static final boolean FILE_CAN_CREATE_DONT_CARE = false;
  private static final boolean FILE_CAN_CREATE           = true;
  
  private static final boolean FILE_DONT_CHECK_OVERWRITE = false;
  private static final boolean FILE_CHECK_OVERWRITE      = true;
  
  private static final boolean FILE_MUST_BE_FILE         = false;
  private static final boolean FILE_MUST_BE_DIRECTORY    = true;  
          
  private static final boolean FILE_CAN_WRITE_DONT_CARE  = false;
  private static final boolean FILE_CAN_WRITE            = true;
  
  private static final boolean FILE_CAN_READ_DONT_CARE   = false;
  private static final boolean FILE_CAN_READ             = true;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  //////////////////////////////////////////////////////////////////////////// 
    
  public FilePreparer(UIComponentSource uics, DataAccessContext dacx, JFrame topWindow, Class<?> prefClass) {
    uics_ = uics;
    dacx_ = dacx;
    isHeadless_ = uics_.isHeadless();
    topWindow_ = topWindow;
    prefClass_ = prefClass;  
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////     
  
   /***************************************************************************
   **
   ** Bundle up file writing error message
   */ 
        
   public FileInputResultClosure getFileInputError(IOException ioex) { 
     return (new FileInputResultClosure(dacx_, isHeadless_, ioex, topWindow_));
   }

  /***************************************************************************
   **
   ** Displays file writing error message
   */ 
        
   public void displayFileOutputError() { 
     ResourceManager rMan = dacx_.getRMan();
     JOptionPane.showMessageDialog(topWindow_, 
                                   rMan.getString("fileWrite.errorMessage"), 
                                   rMan.getString("fileWrite.errorTitle"),
                                   JOptionPane.ERROR_MESSAGE);
     return;
   }

  /***************************************************************************
  **
  ** Command
  */ 
  
  public File getExistingImportFile(String pref, FileFilter filter) {
   
    String filename = getPreference(pref);
    
    File file = null;
    while (file == null) {
      JFileChooser chooser = new JFileChooser();                 
      chooser.addChoosableFileFilter(filter);
      chooser.setFileFilter(filter);
      if (filename != null) {
        File startDir = new File(filename);
        if (startDir.exists()) {
          chooser.setCurrentDirectory(startDir);  
        }
      }
      int option = chooser.showOpenDialog(topWindow_);
      if (option != JFileChooser.APPROVE_OPTION) {
        return (null);
      }
      file = chooser.getSelectedFile();
      if (file != null) {
        if (!standardFileChecks(file, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE, 
                                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ)) {
          file = null;
          continue; 
        }
      }
    }
    return (file);
  }
  
  /***************************************************************************
   **
   ** Command
   */ 
   
   public File getExistingImportFile(String pref, List<FileFilter> filters) {
    
     String filename = getPreference(pref);
     
     File file = null;
     while (file == null) {
       JFileChooser chooser = new JFileChooser();
       for (FileFilter ff: filters) {
         chooser.addChoosableFileFilter(ff);
       }
       if (!filters.isEmpty()) {
         chooser.setFileFilter(filters.get(0));
       }
       if (filename != null) {
         File startDir = new File(filename);
         if (startDir.exists()) {
           chooser.setCurrentDirectory(startDir);  
         }
       }
       int option = chooser.showOpenDialog(topWindow_);
       if (option != JFileChooser.APPROVE_OPTION) {
         return (null);
       }
       file = chooser.getSelectedFile();
       // SAW THIS VARIATION ON THE "LoadDirectory" CASE.  EXITS IF FILE IS NULL!
       //if (file == null) {
        // return (null);
       //}
              
       if (file != null) {
         if (!standardFileChecks(file, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE, 
                                       FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                       FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ)) {
           file = null;
           continue; 
         }
       }
     }
     return (file);
   }
   
  /***************************************************************************
  **
  ** Command
  */ 
   
  public boolean checkExistingImportFile(File toCheck) {  
    if (toCheck == null) {
      return (false);
    }
    return(standardFileChecks(toCheck, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE, 
                                       FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                       FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ));
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  public File getExistingWritableDirectory(String pref) {
     
    String filename = getPreference(pref);
    File targetDir = null;

    while (targetDir == null) {
      JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      if (filename != null) {
        File startDir = new File(filename);
        if (startDir.exists()) {
          //
          // We are asking for a directory.  On Linux/Windows, setting 
          // the current directory makes that directory selectable.  On
          // Mac (10.4, others?), it shows us that directory, but it
          // is not selectable unless we pop up to the parent.  So set
          // the selected files instead.  On Mac, this still only manages
          // to set the directory to the parentString pref, List<FileFilter> filters, and the user needs to
          // select the actual directory still.  Also, the first time,
          // if the Mac user needs to create the directory, they are
          // dumped INTO that directory, and need to back out to select
          // it!  At least now (2.0.2), we catch that something is not
          // a directory instead of crashing.
          //
          chooser.setSelectedFile(startDir);  
        }
      }

      int option = chooser.showSaveDialog(topWindow_);
      if (option != JFileChooser.APPROVE_OPTION) {
        return (null);
      }
      targetDir = chooser.getSelectedFile();
      if (targetDir == null) {
        continue;
      }
      if (!standardFileChecks(targetDir, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE, 
                                         FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_DIRECTORY, 
                                         FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE)) {
        targetDir = null;
        continue; 
      }
    }
    return (targetDir);
  }
   
   /***************************************************************************
   **
   ** Command
   */ 
   
   public boolean checkExistingWritableDirectory(File targetDir) {  
     if (targetDir == null) {
       return (false);
     }
     return(standardFileChecks(targetDir, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE, 
                                          FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_DIRECTORY, 
                                          FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE));
   }
  
   
  /***************************************************************************
  **
  ** Create a file is needed, append suffix if not specified
  */ 
   
  public File getOrCreateWritableFileWithSuffix(String pref, List<FileFilter> filters, 
                                                List<String> suffixes, String prefSuffix) {      
    String filename = getPreference(pref);

    File file = null;
    while (file == null) {
      JFileChooser chooser = new JFileChooser(); 
      for (FileFilter ff: filters) {
        chooser.addChoosableFileFilter(ff);
      }
      if (!filters.isEmpty()) {
        chooser.setFileFilter(filters.get(0));
      }
      if (filename != null) {
        File startDir = new File(filename);
        if (startDir.exists()) {
          chooser.setCurrentDirectory(startDir);  
        }
      }

      int option = chooser.showSaveDialog(topWindow_);
      if (option != JFileChooser.APPROVE_OPTION) {
        return (null);    
      }
      file = chooser.getSelectedFile();
      if (file == null) {
        continue;
      }
      if (!file.exists()) {        
        if (!FileExtensionFilters.hasASuffix(file.getName(), "." , suffixes)) { 
          file = new File(file.getAbsolutePath() + "." + prefSuffix);
        }
      }
      if (!standardFileChecks(file, FILE_MUST_EXIST_DONT_CARE, FILE_CAN_CREATE, 
                                    FILE_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                    FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE)) {
        file = null;
        continue; 
      }
    }
    return (file);
  }
   
  /***************************************************************************
  **
  ** Command
  */ 
   
  public boolean checkWriteFile(File toCheck, boolean checkOverwrite) {  
    if (toCheck == null) {
      return (false);
    }
    boolean checkOver = (checkOverwrite) ? FILE_CHECK_OVERWRITE : FILE_DONT_CHECK_OVERWRITE;    
    return (standardFileChecks(toCheck, FILE_MUST_EXIST_DONT_CARE, FILE_CAN_CREATE, 
                                        checkOver, FILE_MUST_BE_FILE, 
                                        FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE));
  }
  
  /***************************************************************************
  **
  ** Preferences are stored by package. 
  */ 
     
  public void setPreference(String key, String val) {
    if (isHeadless_) {
      return;
    }
    Preferences prefs = Preferences.userNodeForPackage(prefClass_);
    prefs.put(key, val);
    return;
  } 
 
  /***************************************************************************
  **
  ** Error message generation 
  */
  
  public SimpleUserFeedback generateSUFForReadError(IOException ioex) { 
    ResourceManager rMan = dacx_.getRMan();
    String title = rMan.getString("fileRead.errorTitle");
    SimpleUserFeedback.JOP jtype = SimpleUserFeedback.JOP.ERROR;
    String message;
     
    if ((ioex == null) || (ioex.getMessage() == null) || (ioex.getMessage().trim().equals(""))) {
      message = rMan.getString("fileRead.errorMessage"); 
    } else {
      String errMsg = ioex.getMessage().trim();
      String format = rMan.getString("fileRead.inputErrorMessageForIOEx");
      message = MessageFormat.format(format, new Object[] {errMsg});
    }   
    return (new SimpleUserFeedback(jtype, message, title));
  }   
  
  /***************************************************************************
  **
  ** Error message generation 
  */
  
  @SuppressWarnings("unused")
  public SimpleUserFeedback generateSUFForWriteError(IOException ioex) { 
    ResourceManager rMan = dacx_.getRMan();
    return (new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, 
                                   rMan.getString("fileWrite.errorMessage"), 
                                   rMan.getString("fileWrite.errorTitle")));
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////     
    
  /***************************************************************************
  **
  ** Preferences are stored by package.
  */ 
    
  private String getPreference(String key) {
    if (isHeadless_) {
      return (null);
    }
    Preferences prefs = Preferences.userNodeForPackage(prefClass_);    
    String retval = prefs.get(key, null);
    return (retval);
  }   
   
  /***************************************************************************
  **
  ** Do standard file checks and warnings
  */
 
  private boolean standardFileChecks(File target, boolean mustExist, boolean canCreate,
                                     boolean checkOverwrite, boolean mustBeDirectory, 
                                     boolean canWrite, boolean canRead) {
    ResourceManager rMan = dacx_.getRMan();
    boolean doesExist = target.exists();
  
    if (mustExist) {
      if (!doesExist) {
        String noFileFormat = rMan.getString("fileChecks.noFileFormat");
        String noFileMsg = MessageFormat.format(noFileFormat, new Object[] {target.getName()});
        if (isHeadless_) {
          System.err.println(noFileMsg);          
        } else {
          JOptionPane.showMessageDialog(topWindow_, noFileMsg,
                                        rMan.getString("fileChecks.noFileTitle"),
                                        JOptionPane.ERROR_MESSAGE);
        }
        return (false);
      }
    }
    if (mustBeDirectory) {
      if (doesExist && !target.isDirectory()) {
        String notADirectoryFormat = rMan.getString("fileChecks.notADirectoryFormat");
        String notADirectoryMsg = MessageFormat.format(notADirectoryFormat, new Object[] {target.getName()});
        if (isHeadless_) {
          System.err.println(notADirectoryMsg);          
        } else {
          JOptionPane.showMessageDialog(topWindow_, notADirectoryMsg,
                                        rMan.getString("fileChecks.notADirectoryTitle"),
                                        JOptionPane.ERROR_MESSAGE);
        }
        return (false);
      }
    } else { // gotta be a file
      if (doesExist && !target.isFile()) {
        String notAFileFormat = rMan.getString("fileChecks.notAFileFormat");
        String notAFileMsg = MessageFormat.format(notAFileFormat, new Object[] {target.getName()});
        if (isHeadless_) {
          System.err.println(notAFileMsg);          
        } else {
          JOptionPane.showMessageDialog(topWindow_, notAFileMsg,
                                        rMan.getString("fileChecks.notAFileTitle"),
                                        JOptionPane.ERROR_MESSAGE);
        }
        return (false);
      }
    }

    if (!doesExist && canCreate) {
      if (mustBeDirectory) {
        throw new IllegalArgumentException();
      }
      boolean couldNotCreate = false;
      try {
        if (!target.createNewFile()) {
          couldNotCreate = true;
        }
      } catch (IOException ioex) {
        couldNotCreate = true;   
      }
      if (couldNotCreate) {
        String noCreateFormat = rMan.getString("fileChecks.noCreateFormat");
        String noCreateMsg = MessageFormat.format(noCreateFormat, new Object[] {target.getName()});
        if (isHeadless_) {
          System.err.println(noCreateMsg);          
        } else {
          JOptionPane.showMessageDialog(topWindow_, noCreateMsg,
                                        rMan.getString("fileChecks.noCreateTitle"),
                                        JOptionPane.ERROR_MESSAGE);
        }
        return (false);
      }
    }
    
    boolean didExist = doesExist;
    doesExist = target.exists();    
    
    if (canWrite) {
      if (doesExist && !target.canWrite()) {
        String noWriteFormat = rMan.getString("fileChecks.noWriteFormat");
        String noWriteMsg = MessageFormat.format(noWriteFormat, new Object[] {target.getName()});
        if (isHeadless_) {
          System.err.println(noWriteMsg);          
        } else {
          JOptionPane.showMessageDialog(topWindow_, noWriteMsg,
                                        rMan.getString("fileChecks.noWriteTitle"),
                                        JOptionPane.ERROR_MESSAGE);
        }
        return (false);
      }
    }
    if (canRead) {
      if (doesExist && !target.canRead()) {
        String noReadFormat = rMan.getString("fileChecks.noReadFormat");
        String noReadMsg = MessageFormat.format(noReadFormat, new Object[] {target.getName()});
        if (isHeadless_) {
          System.err.println(noReadMsg);          
        } else {
          JOptionPane.showMessageDialog(topWindow_, noReadMsg,
                                        rMan.getString("fileChecks.noReadTitle"),
                                        JOptionPane.ERROR_MESSAGE);
        }
        return (false);
      }
    }
    
    if (didExist && checkOverwrite && !isHeadless_) {  // note we care about DID exist (before creation)
      String overFormat = rMan.getString("fileChecks.doOverwriteFormat");
      String overMsg = MessageFormat.format(overFormat, new Object[] {target.getName()});
      int overwrite =
        JOptionPane.showConfirmDialog(topWindow_, overMsg,
                                      rMan.getString("fileChecks.doOverwriteTitle"),
                                      JOptionPane.YES_NO_OPTION);        
      if (overwrite != JOptionPane.YES_OPTION) {
        return (false);
      }
    }
    return (true);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES 
  //
  ////////////////////////////////////////////////////////////////////////////     
 
  public static class FileInputResultClosure  {   
    private DataAccessContext dacx_; 
    private boolean isHeadless_;
    private IOException ioex_;
    private JFrame topWindow_;
    private boolean success_;
    private boolean passSuccess_;
    
    public FileInputResultClosure() {
      success_ = true;
      passSuccess_ = true;
    }
    
    public FileInputResultClosure(DataAccessContext dacx, boolean isHeadless, IOException ioex, JFrame topWindow) {
      dacx_ = dacx;
      isHeadless_ = isHeadless;
      ioex_ = ioex;
      topWindow_ = topWindow;
      success_ = false;
      passSuccess_ = true;
    }

    /***************************************************************************
    **
    ** Set password success
    */ 
         
    public void setPasswordStatus(boolean success) { 
      passSuccess_ = success;
      return;
    }
    
    /***************************************************************************
    **
    ** Was input OK
    */ 
         
    public boolean wasSuccessful() { 
      return (success_);
    }
    
    /***************************************************************************
    **
    ** Was password OK
    */ 
         
    public boolean wasPasswordSuccess() { 
      return (passSuccess_);
    }
       
    /***************************************************************************
    **
    ** Displays file reading/writing error message
    */ 
         
    public void displayFileInputError() { 
      ResourceManager rMan = dacx_.getRMan();
      
      if ((ioex_ == null) || (ioex_.getMessage() == null) || (ioex_.getMessage().trim().equals(""))) {
        if (!isHeadless_) {
          JOptionPane.showMessageDialog(topWindow_, 
                                        rMan.getString("fileRead.errorMessage"), 
                                        rMan.getString("fileRead.errorTitle"),
                                        JOptionPane.ERROR_MESSAGE);
        } else {
          System.err.println(rMan.getString("fileRead.errorMessage"));
        }
        return;
      }
      String errMsg = ioex_.getMessage().trim();
      String format = rMan.getString("fileRead.inputErrorMessageForIOEx");
      String outMsg = MessageFormat.format(format, new Object[] {errMsg}); 
      if (!isHeadless_) {
        JOptionPane.showMessageDialog(topWindow_, outMsg, 
                                      rMan.getString("fileRead.errorTitle"),
                                      JOptionPane.ERROR_MESSAGE);
      } else {
        System.err.println(outMsg);
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Displays file reading/writing error message
    */ 
         
    public SimpleUserFeedback getSUF() { 
      ResourceManager rMan = dacx_.getRMan();
      
      if ((ioex_ == null) || (ioex_.getMessage() == null) || (ioex_.getMessage().trim().equals(""))) {
        if (isHeadless_ && !dacx_.isForWeb()) {
          System.err.println(rMan.getString("fileRead.errorMessage"));
        }
        return (new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR,
                                       rMan.getString("fileRead.errorMessage"), 
                                       rMan.getString("fileRead.errorTitle")));
      }
      String errMsg = ioex_.getMessage().trim();
      String format = rMan.getString("fileRead.inputErrorMessageForIOEx");
      String outMsg = MessageFormat.format(format, new Object[] {errMsg});
      if (isHeadless_ && !dacx_.isForWeb()) {
        System.err.println(rMan.getString(outMsg));
      }
      return (new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR,
                                     outMsg,
                                     rMan.getString("fileRead.errorTitle")));
    }
  }
}
