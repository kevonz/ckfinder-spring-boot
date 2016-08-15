package com.github.zhanhb.ckfinder.connector.handlers.arguments;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author zhanhb
 */
@Getter
@Setter
public class GetFoldersArguments extends XMLArguments {

  /**
   * list of subdirectories in directory.
   */
  private List<String> directories;

}
