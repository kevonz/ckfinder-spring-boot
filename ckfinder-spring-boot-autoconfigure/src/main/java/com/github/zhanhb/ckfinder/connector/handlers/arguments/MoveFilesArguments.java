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
public class MoveFilesArguments extends XMLArguments {

  private List<FilePostParam> files;
  private int filesMoved;
  private int movedAll;
  private boolean addMoveNode;

  public void filesMovedPlus() {
    filesMoved++;
  }

}
