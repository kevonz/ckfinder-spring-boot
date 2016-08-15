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
public class DownloadFileArguments extends Arguments {

  /**
   * File to download.
   */
  private Path file;
  /**
   * filename request param.
   */
  private String fileName;
  private String newFileName;

}
