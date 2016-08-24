package com.github.zhanhb.ckfinder.connector.handlers.arguments;

import javax.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author zhanhb
 */
@Getter
@Setter
public class SaveFileArguments extends XMLArguments {

  private String fileName;
  private String fileContent;
  private HttpServletRequest request;

}
