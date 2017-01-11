package com.github.zhanhb.ckfinder.connector.handlers.arguments;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author zhanhb
 */
@Getter
@Setter
public abstract class Arguments {

  private String userRole;
  private String currentFolder;
  private String type;

}
