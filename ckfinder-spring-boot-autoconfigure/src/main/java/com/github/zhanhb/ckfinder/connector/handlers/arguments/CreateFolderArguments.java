package com.github.zhanhb.ckfinder.connector.handlers.arguments;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author zhanhb
 */
@Getter
@Setter
public class CreateFolderArguments extends XMLArguments {

  /**
   * new folder name request param.
   */
  private String newFolderName;

}
