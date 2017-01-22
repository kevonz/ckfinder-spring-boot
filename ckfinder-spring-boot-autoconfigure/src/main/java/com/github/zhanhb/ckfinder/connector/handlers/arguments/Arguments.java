package com.github.zhanhb.ckfinder.connector.handlers.arguments;

import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
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

  public void throwException(int code) throws ConnectorException {
    throw new ConnectorException(currentFolder, type, code);
  }

}
