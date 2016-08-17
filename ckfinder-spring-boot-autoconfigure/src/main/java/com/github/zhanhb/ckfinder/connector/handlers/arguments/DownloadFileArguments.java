package com.github.zhanhb.ckfinder.connector.handlers.arguments;

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
   * filename request param.
   */
  private String fileName;

}
