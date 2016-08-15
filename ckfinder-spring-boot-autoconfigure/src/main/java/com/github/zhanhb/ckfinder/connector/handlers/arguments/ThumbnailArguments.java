package com.github.zhanhb.ckfinder.connector.handlers.arguments;

import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author zhanhb
 */
@Getter
@Setter
public class ThumbnailArguments extends Arguments {

  /**
   * File name.
   */
  private String fileName;
  /**
   * Thumbnail file.
   */
  private Path thumbFile;
  /**
   * Field holding If-None-Match header value.
   */
  private String ifNoneMatch;
  /**
   * Field holding If-Modified-Since header value.
   */
  private long ifModifiedSince;
  /**
   * Full path to the thumbnail.
   */
  private String fullCurrentPath;

}
