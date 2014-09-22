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

import java.util.HashMap;
import java.util.Set;

/***************************************************************************
**
** Class for handling Base64 encoding and decoding tasks
*/
  
public class Base64Util {
   
  private final static char[] encodeChars_;
  private final static HashMap<MutChar, Integer> decodeMap_;
  
  /***************************************************************************
  **
  ** Everybody shares the maps!
  */  
  
  static {
    encodeChars_ = new char[] {'A','B','C','D','E','F','G','H','I','J','K','L','M',
                               'N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
                               'a','b','c','d','e','f','g','h','i','j','k','l','m',
                               'n','o','p','q','r','s','t','u','v','w','x','y','z',  
                               '0','1','2','3','4','5','6','7','8','9','+','/'};
    decodeMap_ = new HashMap<MutChar, Integer>();
    for (int i = 0; i < 64; i++) {
      char keyVal = encodeChars_[i];
      MutChar key = new MutChar(keyVal);
      decodeMap_.put(key, new Integer(i));
    }
  }  

  /***************************************************************************
  **
  ** Encode the bytes to a string
  */  
  
  public String encode(byte[] input) {
    StringBuffer retval = new StringBuffer();
    
    //
    // We need to work with an input array that is a multiple of three:
    //
    
    int diffCount = (input.length) % 3;
    String padString = "";
    if (diffCount != 0) {
      StringBuffer padBuffer = new StringBuffer();
      int padCount = 3 - diffCount;
      byte[] paddedInput = new byte[input.length + padCount];
      System.arraycopy(input, 0, paddedInput, 0, input.length);
      for (int i = 0; i < padCount; i++) {
        paddedInput[input.length + i] = 0;
        padBuffer.append('=');
      }
      input = paddedInput;
      padString = padBuffer.toString();
    }
    int paddedLength = input.length;
    
    //
    // Increment over the input array in three-byte chunks:
    //
    
    for (int i = 0; i < paddedLength; i += 3) {
      
      //
      // Newlines every 76 output characters:
      //
      
      if ((i > 0) && (((i / 3) * 4) % 76 == 0)) {
        retval.append('\n');
      }
      
      //
      // Build a 24-bit value from the three characters.  Then divide
      // that into four 6-bit numbers.  Note that Java bytes are signed, 
      // so we need to mask.  This fix addresses BT-12-17-08:2
      //
      
      int val1 = input[i] & 0xFF;
      int val2 = input[i + 1] & 0xFF;
      int val3 = input[i + 2] & 0xFF;
      int tfbit = (val1 << 16) + (val2 << 8) + val3;
      int sixbit0 = (tfbit >> 18) & 0x3F;
      int sixbit1 = (tfbit >> 12) & 0x3F;
      int sixbit2 = (tfbit >> 6) & 0x3F;
      int sixbit3 = tfbit & 0x3F;
      retval.append(encodeChars_[sixbit0]);
      retval.append(encodeChars_[sixbit1]);      
      retval.append(encodeChars_[sixbit2]);      
      retval.append(encodeChars_[sixbit3]); 
    }
 
    //
    // Now finalize the padding.
    //
    
    int retSize = retval.length();
    retval.delete(retSize - padString.length(), retSize);
    retval.append(padString);
    return (retval.toString());
  }    
  
  /***************************************************************************
  **
  ** Decode the String to bytes
  */
    
  public byte[] decode(String input) {
    if ((input == null) || input.equals("")) {
      return (new byte[0]);
    }
    
    //
    // Change incoming "=" pads to appended 0 pads while dropping
    // any out-of-range characters (esp. newlines):
    //
    
    int inlen = input.length();
    int padCount = (input.charAt(inlen - 1) == '=') ? 1 : 0;
    if ((padCount == 1) && (input.charAt(inlen - 2) == '=')) {
      padCount = 2;
    }
    
    MutChar checkChar = new MutChar();
    Set<MutChar> goodChars = decodeMap_.keySet();
    StringBuffer workbuf = new StringBuffer();
    int lastGood = inlen - padCount;
    for (int i = 0; i < lastGood; i++) {
      char val = input.charAt(i);
      if (goodChars.contains(checkChar.setVal(val))) {
        workbuf.append(val);
      }
    }
    
    if (padCount > 0) {
      workbuf.append((padCount == 1) ? "A" : "AA");
    }
    int paddedLength = workbuf.length();
   
    //
    // Crank thru input 4 at a time.
    //
    
    int targLen = ((paddedLength / 4) * 3) - padCount;
    byte[] target = new byte[targLen];
    int targIndex = 0;
    for (int i = 0; i < paddedLength; i += 4) {
      //
      // Build a 24-bit value from the four 6-bit characters.  Then divide
      // that into three 8-bit numbers:
      //
      
      int val0 = decodeMap_.get(checkChar.setVal(workbuf.charAt(i))).intValue();
      int val1 = decodeMap_.get(checkChar.setVal(workbuf.charAt(i + 1))).intValue();
      int val2 = decodeMap_.get(checkChar.setVal(workbuf.charAt(i + 2))).intValue();
      int val3 = decodeMap_.get(checkChar.setVal(workbuf.charAt(i + 3))).intValue();
            
      int tfbit = (val0 << 18) + (val1 << 12) + (val2 << 6) + val3; 
      target[targIndex++] = (byte)((tfbit >>> 16) & 0xFF);
      // This drops the zero padding:
      if (targIndex < targLen) {
        target[targIndex++] = (byte)((tfbit >>> 8) & 0xFF);
      }
      if (targIndex < targLen) {
        target[targIndex++] = (byte)(tfbit & 0xFF);
      }
    }
    return (target);
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
      simpleTest();
    } catch (Exception ex) {
      System.out.println("Exception " + ex.getMessage());
    }
    return;
  }
        
  /***************************************************************************
  **
  ** Test frame.  TOO simple!
  */

  public static void simpleTest() {
    Base64Util bu = new Base64Util();
    String test = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";    
    try {
      String encoded = bu.encode(test.getBytes("US-ASCII"));
      System.out.print(encoded);
      System.out.println();
      byte[] decoded = bu.decode(encoded);
      for (int i = 0; i < decoded.length; i++) {
        System.out.print((char)decoded[i]);
      }
      System.out.println();
     // byte[] retval = new BASE64Decoder().decodeBuffer(encoded); 
     // for (int i = 0; i < retval.length; i++) {
     //   System.out.print((char)retval[i]);
     // }      
     // System.out.println();
      
    } catch (Exception ex) {
      throw new IllegalStateException();
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
  ** Eliminate lots of object creation
  */

  private static class MutChar {
    private char val_;

    MutChar() {
      this.val_ = (char)0;
    }

    MutChar(char val) {
      this.val_ = val;
    }

    MutChar setVal(char val) {
      this.val_ = val;
      return (this);
    }

    @SuppressWarnings("unused")
    char getVal() {
      return (this.val_);
    }  

    public int hashCode() {
      return (this.val_);
    }

    public boolean equals(Object other) {
      if (this == other) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof MutChar)) {
        return (false);
      }
      MutChar otherMS = (MutChar)other;
      return (this.val_ == otherMS.val_);
    }
  }  
}
  
