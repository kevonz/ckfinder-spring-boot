package com.github.zhanhb.ckfinder.connector.handlers.arguments;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author zhanhb
 */
@Getter
@Setter
public class ImageResizeArguments extends XMLArguments {

  /**
   * file name
   */
  private String fileName;
  private String newFileName;
  private String overwrite;
  private Integer width;
  private Integer height;
  private boolean wrongReqSizesParams;
  private Map<String, String> sizesFromReq;
  private HttpServletRequest request;

}
