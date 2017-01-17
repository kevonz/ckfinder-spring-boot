package com.github.zhanhb.ckfinder.connector.handlers.arguments;

import com.github.zhanhb.ckfinder.connector.handlers.response.Connector;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author zhanhb
 */
@Getter
@Setter
public class XMLArguments extends Arguments {

  private Connector.Builder connector;

  /**
   *
   * errors list.
   */
  private final List<ErrorNode> errorList = new ArrayList<>(4);

}
