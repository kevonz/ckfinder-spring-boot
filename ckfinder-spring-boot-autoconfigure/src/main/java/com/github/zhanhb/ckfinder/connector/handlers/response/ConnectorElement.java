package com.github.zhanhb.ckfinder.connector.handlers.response;

/**
 *
 * @author zhanhb
 */
public interface ConnectorElement {

  public static ConnectorInfo.Builder connectorInfo() {
    return ConnectorInfo.builder();
  }

  public static CopyFiles.Builder copyFiles() {
    return CopyFiles.builder();
  }

  public static CurrentFolder.Builder currentFolder() {
    return CurrentFolder.builder();
  }

  public static DeleteFiles.Builder deleteFiles() {
    return DeleteFiles.builder();
  }

  public static Error.Builder error() {
    return Error.builder();
  }

  public static ImageInfo.Builder imageInfo() {
    return ImageInfo.builder();
  }

  public static MoveFiles.Builder moveFiles() {
    return MoveFiles.builder();
  }

  public static NewFolder.Builder newFolder() {
    return NewFolder.builder();
  }

  public static RenamedFile.Builder renamedFile() {
    return RenamedFile.builder();
  }

  public static RenamedFolder.Builder renamedFolder() {
    return RenamedFolder.builder();
  }

}
