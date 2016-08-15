package com.github.zhanhb.ckfinder.connector.handlers.arguments;

import com.github.zhanhb.ckfinder.connector.data.FilePostParam;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author zhanhb
 */
@Getter
@Setter
public class CopyFilesArguments extends XMLArguments {

  private List<FilePostParam> files;
  private int filesCopied;
  private int copiedAll;
  private boolean addCopyNode;

  public void filesCopiedPlus() {
    filesCopied++;
  }

}
