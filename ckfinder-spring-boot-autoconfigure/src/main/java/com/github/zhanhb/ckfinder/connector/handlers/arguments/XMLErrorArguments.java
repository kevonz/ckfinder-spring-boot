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
public class XMLErrorArguments extends XMLArguments {

  /**
   * exception to handle.
   */
  private ConnectorException connectorException;

}
