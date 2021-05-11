package uk.me.steev.smdhashes;

import java.math.BigInteger;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class Utils {
  public static String byteArrayToString(byte[] bytes, boolean addSpaces, boolean addNewLines) {
    if (null == bytes)
      return "";

    BigInteger bigInteger = new BigInteger(1, bytes);
    String result = String.format("%0" + (bytes.length << 1) + "x", bigInteger);
    if (addSpaces) {
      String newString = "";
      char[] charArray = result.toCharArray();
      for (int i = 0; i < charArray.length; i++) {
        newString += charArray[i];
        if (i % 2 == 1 && i > 0)
          newString += ' ';
        if (i % 32 == 31 && i < charArray.length - 1)
          newString = newString + "\r\n";
      }
      result = newString;
    }
    return result;
  }

  public static String humanReadableByteCountBin(long bytes) {
    long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
    if (absB < 1024) {
      return bytes + " B";
    }
    long value = absB;
    CharacterIterator ci = new StringCharacterIterator("KMGTPE");
    for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
      value >>= 10;
      ci.next();
    }
    value *= Long.signum(bytes);
    return String.format("%.1f %ciB", value / 1024.0, ci.current());
  }
}
