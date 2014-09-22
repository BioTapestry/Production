/*
**    Copyright (C) 2003-2009 Institute for Systems Biology 
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

import java.util.ArrayList;
import java.util.List;

/****************************************************************************
**
** Utility class for handling line break operations
*/

public class LineBreaker {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** When a name is trimmed for submission, the break def must be fixed
  ** accordingly
  */
  
  public static String fixBreaksForTrim(String breakDef, String untrimmed) {
    if (breakDef == null) {
      return (null);
    }   

    String trimmed = untrimmed.trim();
    int tl = trimmed.length();
    int utl = untrimmed.length();
    if (tl == utl) {
      return (breakDef);
    }
    
    //
    // Find trim indices
    //
    
    int frontIndex = untrimmed.indexOf(trimmed);
    int backIndex = frontIndex + tl;
    
    //
    // Breaks in the front trim region are dropped
    // Breaks after that are shifted right
    // Breaks in the back trim are dropped.
    //
    
    List breaks = MultiLineRenderSupport.parseBreaks(breakDef);
    int count = 0;
    while (count < breaks.size()) {
      Integer brk = (Integer)breaks.get(count);
      int brkVal = brk.intValue();
      if ((brkVal <= frontIndex) || (brkVal > backIndex)) {
        breaks.remove(count);
      } else {
        breaks.set(count, new Integer(brkVal - frontIndex));
        count++;
      }
    }
    return (MultiLineRenderSupport.buildBreaks(breaks));
  }

  /***************************************************************************
  **
  ** Figure out how to shift line breaks around
  */
  
  public static String resolveNameChange(String breakDef, LineBreakChangeSteps steps) {
    int numSteps = steps.stepChanges.size();
    if ((breakDef == null) || (numSteps == 0)) {
      return (breakDef);
    }
    
    List breaks = MultiLineRenderSupport.parseBreaks(breakDef);
    
    for (int i = 0; i < numSteps; i++) {
      ChangeStep cs = (ChangeStep)steps.stepChanges.get(i);
      //
      // First insertion into field is ignored as a modifying step:
      if ((i == 0) && (cs.oldString == null) && 
          (cs.type == ChangeStep.INSERT) && (cs.length == cs.newString.length())) {
        continue;
      }

      switch (cs.type) {
        case ChangeStep.INSERT:
          calcInsert(breaks, cs);
          break;
        case ChangeStep.DELETE:
          calcDelete(breaks, cs);
          break;
        case ChangeStep.REPLACE:
          calcReplace(breaks, cs);
          break;
        default:
          throw new IllegalStateException();
      }
    }
    return (MultiLineRenderSupport.buildBreaks(breaks));
  } 
  
  /***************************************************************************
  **
  ** Do break mods for insert (Push all the breaks past the end of the insertion over
  ** to the right)
  */
  
  private static void calcInsert(List breaks, ChangeStep cs) {
    int numBr = breaks.size();
    for (int j = 0; j < numBr; j++) {
      Integer brk = (Integer)breaks.get(j);
      int brkVal = brk.intValue();
      if (brkVal < cs.index) {
        continue;
      } else {
        breaks.set(j, new Integer(brkVal + cs.length));
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Do break mods for delete.  All breaks, if any, in the deleted fragment are 
  ** combined into a single break.  Breaks after are moved left. Breaks that
  ** end up at the very end are dropped.
  */
  
  private static void calcDelete(List breaks, ChangeStep cs) {
    int numBr = breaks.size();
    int deathMin = cs.index;
    int deathMax = cs.index + cs.length;
    boolean ghostAdded = false;
    int count = 0;
    while (count < breaks.size()) {
      Integer brk = (Integer)breaks.get(count);
      int brkVal = brk.intValue();
      if (brkVal < deathMin) {
        count++;
        continue;
      } else if ((brkVal >= deathMin) && (brkVal < deathMax)) {
        if (!ghostAdded) {
          breaks.set(count, new Integer(deathMin));
          ghostAdded = true;
          count++;
        } else {
          breaks.remove(count);
        }
      } else {
        breaks.set(count, new Integer(brkVal - cs.length));
        count++;
      }
    }
    return;
  }  

  /***************************************************************************
  **
  ** Do break mods for replace.
  ** All breaks past the right end of the replacement are moved left or right
  ** as needed.  Within the replacement block, for each existing break, going left to right,
  ** try to match in new fragment (2-character RE) until we run out or hit the end of the fragment.
  ** Lots of much better ways to do this...
  **
  */
  
  private static void calcReplace(List breaks, ChangeStep cs) {
    List res = fragToRe(breaks, cs.index, cs.length, cs.fragment1);
    List newbrks = bestBreakFits(res, cs.step2Length, cs.fragment2);
    newbrks = bestBreakFitsSecondPass(res, newbrks, cs.step2Length, cs.fragment2);
  
    int numBr = breaks.size();
    int deathMin = cs.index;
    int deathMax = cs.index + cs.length;
    int fragDiff = cs.step2Length - cs.length;
    int newCount = 0;
    int count = 0;
    while (count < breaks.size()) {
      Integer brk = (Integer)breaks.get(count);
      int brkVal = brk.intValue();
      if (brkVal < deathMin) {
        count++;
        continue;
      } else if ((brkVal >= deathMin) && (brkVal < deathMax)) {
        Integer newBreak = (Integer)newbrks.get(newCount);
        int newBreakVal = newBreak.intValue();
        if (newBreakVal != -1) {
          breaks.set(count, new Integer(deathMin + newBreakVal));
          count++;
          newCount++;
        } else {
          breaks.remove(count);
          newCount++;
        }
      } else {
        breaks.set(count, new Integer(brkVal + fragDiff));
        count++;
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Given a dicarded text fragment, find the 2-char strings bounding
  ** existing breaks:
  */
  
  private static List fragToRe(List breaks, int fragStart, int fragLen, String fragment) {
    ArrayList retval = new ArrayList();
    int numBr = breaks.size();
    int fragEnd = fragStart + fragLen;
    for (int j = 0; j < numBr; j++) {
      Integer brk = (Integer)breaks.get(j);
      int brkVal = brk.intValue();
      if ((brkVal < fragStart) || (brkVal > fragEnd)) {
        continue;
      }
      int fragIndx = brkVal - fragStart;
      char[] addMe = new char[2];
      if (fragIndx == 0) {
        addMe[0] = 0;
        addMe[1] = fragment.charAt(0);
      } else if (fragIndx == fragLen) {
        addMe[0] = fragment.charAt(fragLen - 1);
        addMe[1] = 0;
      } else {
        addMe[0] = fragment.charAt(fragIndx - 1);
        addMe[1] = fragment.charAt(fragIndx);
      }
      retval.add(addMe);
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Find "best" fit (hah!) of old breaks into a new fragment
  ** existing breaks:
  */
  
  private static List bestBreakFits(List res, int fragLen, String newFragment) {
    ArrayList retval = new ArrayList();
    int numRe = res.size();
    int currIndex = 0;
    for (int currRe = 0; currRe < numRe; currRe++) {
      char[] re = (char[])res.get(currRe);
      // We will retain breaks at the start and end no matter what:
      if (re[0] == 0) {
        retval.add(new Integer(0));
        continue;
      }
      if (re[1] == 0) {
        retval.add(new Integer(fragLen - 1));
        continue;
      }
      //
      // Try to fit the next RE.  If it fails completely, tag it as such and try the
      // next one.  Once we are done, go back and try to fit the missed tags that have
      // a space on one side or the other.  If that fails, we toss it out.
      //
      boolean didMatch = false;
      for (int i = currIndex; i < fragLen - 1; i++) {
        if ((newFragment.charAt(i) == re[0]) && (newFragment.charAt(i + 1) == re[1])) {
          retval.add(new Integer(i + 1));
          currIndex = i + 1;
          didMatch = true;
          break;
        }
      }
      if (!didMatch) {
        retval.add(new Integer(-100 - currRe));
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get the next non-zero break to the right of the current break
  */
  
  private static int getNextBreak(List breaks, int fragLen, int currBr) {
    int numBr = breaks.size();
    for (int i = currBr + 1; i < numBr; i++) {
      Integer brk = (Integer)breaks.get(i);
      int brkVal = brk.intValue();
      if (brkVal >= 0) {
        return (brkVal);
      }
    }
    return (fragLen - 1);
  } 
  
  /***************************************************************************
  **
  ** Tries to fit breaks at whitespace to whitespace in candidate sub-fragments
  */
  
  private static List bestBreakFitsSecondPass(List res, List breaks, int fragLen, String newFragment) {
    ArrayList retval = new ArrayList();
    int numBr = breaks.size();
    int lastBrk = 0;
    for (int i = 0; i < numBr; i++) {
      Integer brk = (Integer)breaks.get(i);
      int brkVal = brk.intValue();
      if (brkVal >= 0) {
        retval.add(brk);
        lastBrk = brkVal;
        continue;
      }
      //
      // Negative value.  Look for a match in the piece bounded by the two breaks
      //
      int nextBrk = getNextBreak(breaks, fragLen, i);
      String fragToChk = newFragment.substring(lastBrk, nextBrk);
      int currRe = (-brkVal) - 100; 
      char[] re = (char[])res.get(currRe);
      boolean hasPreSpace = (re[0] == ' ');
      boolean hasPostSpace = (re[1] == ' ');
      if (hasPreSpace || hasPostSpace) {
        int useThis = fragToChk.indexOf(' ');
        if (useThis != -1) {
          lastBrk = useThis + lastBrk;
          if (hasPreSpace) lastBrk++;
          retval.add(new Integer(lastBrk));
          lastBrk++;
          continue;
        } else {
          retval.add(new Integer(-1));
        }
      } else {
        retval.add(new Integer(-1));
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Fixup line breaks to clean them up.
  */
  
  public static String massageLineBreaks(String breakDef, String baseName) {
    if (breakDef == null) {
      return (null);
    }

    List breaks = MultiLineRenderSupport.parseBreaks(breakDef);
    int numBrk = breaks.size();
    int baseLen = baseName.length();

    ArrayList retval = new ArrayList();
    int lastBrk = -1;
    for (int i = 0; i < numBrk; i++) {
      Integer brk = (Integer)breaks.get(i);
      int brkVal = brk.intValue();
      // Don't want newlines tacked on the end:
      if (brkVal >= baseLen) {
        continue;
      }
      // Don't want to allow multiple newlines in a row!
      if ((lastBrk != 1) && (brkVal == lastBrk)) {
        continue;
      }
      // Want to force cases of <newline><blank> to <blank><newline>:
    //  String preStr = (brkVal == 0) ? null : baseName.substring(brkVal - 1, brkVal);
   //   String postStr = (brkVal == baseLen - 1) ? null : baseName.substring(brkVal, brkVal + 1);      
    //  if ((postStr != null) && postStr.equals(" ")) {
     //   offs++;
     //   preStr = getText(offs - 1, 1);
     //   postStr = (offs == currLen) ? null : getText(offs, 1);
    //  }
      retval.add(brk);
      lastBrk = brkVal;
    }
    return (MultiLineRenderSupport.buildBreaks(retval));
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Used to transmit how to modify a line break definition
  **
  */
  
  public static class LineBreakChangeSteps {
    public ArrayList stepChanges;
    
    public LineBreakChangeSteps() {
      stepChanges = new ArrayList();
    }
    
    public void addChange(ChangeStep change) {
      //
      // If the change coming in is an insertion with the same offset as a
      // deletion in the last slot, make the last guy on the list a REPLACE instead.  
      //
      int numCh = stepChanges.size();
      if (numCh > 0) {
        ChangeStep lastChange = (ChangeStep)stepChanges.get(numCh - 1);
        if ((lastChange.type == ChangeStep.DELETE) &&
            (change.type == ChangeStep.INSERT) &&
            (lastChange.index == change.index)) {
          lastChange.type = ChangeStep.REPLACE;
 
          lastChange.newString = change.newString;
          lastChange.step2Length = change.length;         
          lastChange.fragment2 = change.fragment1;
        } else {
          stepChanges.add(change);          
        }
      } else {
        stepChanges.add(change);
      }
      return;
    }
   
    public void clearChanges() {
      stepChanges.clear();
      return;
    }
    
    public String toString() {
      return ("LineBreakChangeSteps: " + stepChanges.toString());
    }
    
    public LineBreakChangeSteps lastStepsOnly() {
      LineBreakChangeSteps retval = new LineBreakChangeSteps();
      int myLen = stepChanges.size();
      if (myLen > 0) {
        retval.addChange((ChangeStep)this.stepChanges.get(myLen - 1));
      }    
      return (retval);
    }
  }  
  
  /***************************************************************************
  **
  ** Used to transmit how to modify a line break definition
  **
  */
  
  public static class ChangeStep {
    
    public static final int DELETE = 0;
    public static final int INSERT = 1;
    public static final int REPLACE = 2;
  
    public String oldString;
    public String newString;
    public int index;
    public int length;
    public int step2Length;
    public int type;
    public String fragment1;
    public String fragment2;
               
    public ChangeStep(int type, int index, int length, String newString, String oldString) {
      this.oldString = oldString;
      this.newString = newString;
      this.index = index;
      this.length = length;
      this.type = type;
      if (this.type == INSERT) {
        this.fragment1 = newString.substring(index, index + length);
      } else if (this.type == DELETE) {
        this.fragment1 = (oldString == null) ? "" : oldString.substring(index, index + length);
      } else {
        throw new IllegalArgumentException();
      }
    }
    
    public String toString() {
      return ("ChangeStep: " + oldString + " " + newString + " " + index + " " + 
              length + " " + step2Length + " " + type + " " + fragment1 + " " + fragment2);
    }
    
    public ChangeStep mergeToReplacement(ChangeStep secondStep) {
      return (null);
    }
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // TEST METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Test frame
  */

  public static void main(String[] argv) {
    try {
      String str3 = "This \nis a test str\ning";
      String insertLoc = "test";
      String frag = "foo";     
      insertTest(str3, insertLoc, frag);     
      str3 = "This \nis a testy\ntesty\ntesty str\ning";
      String delete = "testytestytesty ";
      deleteTest(str3, delete);
      
      str3 = "This \nis a testytestytesty str\ning \ntoo";
      delete = "testytestytesty ";
      deleteTest(str3, delete);    
      
      str3 = "This \nis a testytestytesty str\ning \ntoo";
      delete = "too";
      deleteTest(str3, delete);
      
      str3 = "This \nis a testytestytesty str\ning \ntoo";
      delete = "testytestytesty";
      String replace = "zippy";
      replaceTest(str3, delete, replace);
 
      str3 = "This \nis a testy\ntesty\ntesty str\ning \ntoo";
      delete = "testytestytesty";
      replace = "testytestytesty";
      replaceTest(str3, delete, replace);
      
      str3 = "This \nis a testy\ntesty\ntesty str\ning \ntoo";
      delete = "testytestytesty";
      replace = "tastytastytasty";
      replaceTest(str3, delete, replace);  
      
      str3 = "This \nis a \ntestytestytesty \nstr\ning \ntoo";
      delete = "testytestytesty";
      replace ="xxxxxxxxxxxxxxx";
      replaceTest(str3, delete, replace);
      
      str3 = "This \nis a \ntestytestytesty str\ning \ntoo";
      delete = "testytestytesty";
      replace ="xxxxxxxxxxxxxxx";
      replaceTest(str3, delete, replace);
      
      str3 = "This \nis a te\nsty \ntesty\n test\ny str\ning \ntoo";
      delete = "testy testy testy";
      replace ="xesxx xxxxx xxxty";
      replaceTest(str3, delete, replace);         
          
    } catch (Exception ioex) {
      System.err.println(ioex);
      ioex.printStackTrace();
    }
    return;
  }  
  
  private static void insertTest(String str3, String insertLoc, String frag) {     
    LineBreakChangeSteps steps = new LineBreakChangeSteps();
    String str1 = MultiLineRenderSupport.stripBreaks(str3);
    ChangeStep cs1 = new ChangeStep(ChangeStep.INSERT, 0, str1.length(), str1, null);
    steps.addChange(cs1);
    
    int index1 = str1.indexOf(insertLoc);
    String str2 = str1.substring(0, index1) + frag + str1.substring(index1);
    ChangeStep cs2 = new ChangeStep(ChangeStep.INSERT, index1, frag.length(), str2, str1);
    steps.addChange(cs2);
    showIt(str3, str2, steps);
    return;
  }    
  
  private static void deleteTest(String str3, String delete) {     
    LineBreakChangeSteps steps = new LineBreakChangeSteps();
    String str1 = MultiLineRenderSupport.stripBreaks(str3);
    ChangeStep cs1 = new ChangeStep(ChangeStep.INSERT, 0, str1.length(), str1, null);
    steps.addChange(cs1);
    
    int index1 = str1.indexOf(delete);
    String str2 = str1.substring(0, index1) + str1.substring(index1 + delete.length());
    ChangeStep cs2 = new ChangeStep(ChangeStep.DELETE, index1, delete.length(), str2, str1);
    steps.addChange(cs2); 
    showIt(str3, str2, steps);
    return;
  }      
  
  private static void replaceTest(String str3, String delete, String replace) {     
    LineBreakChangeSteps steps = new LineBreakChangeSteps();
    String str1 = MultiLineRenderSupport.stripBreaks(str3);
    ChangeStep cs1 = new ChangeStep(ChangeStep.INSERT, 0, str1.length(), str1, null);
    steps.addChange(cs1);
    
    int index1 = str1.indexOf(delete);
    String str2 = str1.substring(0, index1) + str1.substring(index1 + delete.length());
    ChangeStep cs2 = new ChangeStep(ChangeStep.DELETE, index1, delete.length(), str2, str1);
    steps.addChange(cs2);
    String str4 = str2.substring(0, index1) + replace + str2.substring(index1);    
    ChangeStep cs3 = new ChangeStep(ChangeStep.INSERT, index1, replace.length(), str4, str2);
    steps.addChange(cs3);
    showIt(str3, str4, steps);
    return;
  } 
  
  private static void showIt(String str3, String str4, LineBreakChangeSteps steps) {     
    System.out.println("postOp: " + str4);
    String breaks = MultiLineRenderSupport.genBreaks(str3);
    System.out.println("old breaks " + breaks);
    String newBrks = resolveNameChange(breaks, steps);
    System.out.println("new breaks " + newBrks);
    System.out.println("was>" + str3 + "<");
    String broken = MultiLineRenderSupport.applyLineBreaks(str4, newBrks);
    System.out.println("now>" + broken + "<");
    System.out.println("-----------------------------------------------");
    return;
  }         
}  

