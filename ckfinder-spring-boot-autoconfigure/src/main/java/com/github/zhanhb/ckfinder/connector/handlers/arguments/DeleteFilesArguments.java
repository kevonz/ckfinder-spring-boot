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
public class DeleteFilesArguments extends XMLArguments {

  private List<FilePostParam> files;
  private int filesDeleted;
  private boolean addDeleteNode;

  public void filesDeletedPlus() {
    filesDeleted++;
  }

}
