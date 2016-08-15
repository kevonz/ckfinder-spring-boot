package com.github.zhanhb.ckfinder.connector.handlers.arguments;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author zhanhb
 */
@Getter
@Setter
public class RenameFileArguments extends XMLArguments {

  private String fileName;
  private String newFileName;
  private boolean renamed;
  private boolean addRenameNode;

}
