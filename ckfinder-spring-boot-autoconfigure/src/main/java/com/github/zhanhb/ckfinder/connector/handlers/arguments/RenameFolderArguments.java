package com.github.zhanhb.ckfinder.connector.handlers.arguments;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author zhanhb
 */
@Getter
@Setter
public class RenameFolderArguments extends XMLArguments {

  private String newFolderName;
  private String newFolderPath;

}
