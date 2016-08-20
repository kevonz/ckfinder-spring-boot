package com.github.zhanhb.ckfinder.connector.handlers.command;

import com.github.zhanhb.ckfinder.connector.utils.FileUtils;

/**
 *
 * @author zhanhb
 */
@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
class ContentDisposition {

  static String getContentDisposition(String type, String filename) {
    if (filename == null || filename.length() == 0) {
      return type;
    } else if (isToken(filename)) { // already a token
      return type + "; filename=" + filename;
    } else {
      String encoded = FileUtils.encodeContentDisposition(filename);
      return type + "; filename=\"" + encoded + "\"; filename*=utf-8''" + encoded;
    }
  }

  private static boolean isToken(String filename) {
    if (filename == null || filename.length() == 0) {
      return false;
    }
    for (int i = 0, len = filename.length(); i < len; ++i) {
      char ch = filename.charAt(i);
      if (ch >= 0x7F || ch < ' ') { // CHAR predicate
        return false;
      }
      switch (ch) {
        // token separators
        // @see https://tools.ietf.org/html/rfc2616#section-2.2
        case '(':
        case ')':
        case '<':
        case '>':
        case '@':
        case ',':
        case ';':
        case ':':
        case '\\':
        case '"':
        case '/':
        case '[':
        case ']':
        case '?':
        case '=':
        case '{':
        case '}':
        case ' ':
        case '\t':
        // should percent be a valid token???
        // here we are different from the rfc
        case '%':
          return false;
      }
    }
    return true;
  }

}
