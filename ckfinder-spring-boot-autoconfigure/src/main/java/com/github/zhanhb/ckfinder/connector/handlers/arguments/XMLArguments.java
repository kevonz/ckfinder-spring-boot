package com.github.zhanhb.ckfinder.connector.handlers.arguments;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.w3c.dom.Document;

/**
 *
 * @author zhanhb
 */
@Getter
@Setter
public class XMLArguments extends Arguments {

  private Document document;

  /**
   *
   * errors list.
   */
  private final List<ErrorNode> errorList = new ArrayList<>(4);

}
