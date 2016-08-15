/*
 * CKFinder
 * ========
 * http://cksource.com/ckfinder
 * Copyright (C) 2007-2015, CKSource - Frederico Knabben. All rights reserved.
 *
 * The software, this file and its contents are subject to the CKFinder
 * License. Please read the license.txt file before using, installing, copying,
 * modifying or distribute this file or part of its contents. The contents of
 * this file is part of the Source Code of CKFinder.
 */
package com.github.zhanhb.ckfinder.connector.handlers.command;

import com.github.zhanhb.ckfinder.connector.configuration.Constants;
import com.github.zhanhb.ckfinder.connector.configuration.IConfiguration;
import com.github.zhanhb.ckfinder.connector.data.FilePostParam;
import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.MoveFilesArguments;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

/**
 * Class to handle <code>MoveFiles</code> command.
 */
@Slf4j
public class MoveFilesCommand extends XMLCommand<MoveFilesArguments> implements IPostCommand {

  public MoveFilesCommand() {
    super(MoveFilesArguments::new);
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Element rootElement) {
    if (hasErrors()) {
      Element errorsNode = getArguments().getDocument().createElement("Errors");
      addErrors(errorsNode);
      rootElement.appendChild(errorsNode);
    }

    if (getArguments().isAddMoveNode()) {
      createMoveFielsNode(rootElement);
    }
  }

  /**
   * creates move file XML node.
   *
   * @param rootElement XML root element.
   */
  private void createMoveFielsNode(Element rootElement) {
    Element element = getArguments().getDocument().createElement("MoveFiles");
    element.setAttribute("moved", String.valueOf(getArguments().getFilesMoved()));
    element.setAttribute("movedTotal",
            String.valueOf(getArguments().getMovedAll() + getArguments().getFilesMoved()));
    rootElement.appendChild(element);
  }

  @Override
  protected int getDataForXml() {
    if (!isTypeExists(getArguments().getType())) {
      getArguments().setType(null);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE;
    }

    if (!getConfiguration().getAccessControl().checkFolderACL(getArguments().getType(),
            getArguments().getCurrentFolder(),
            getArguments().getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_RENAME
            | AccessControl.CKFINDER_CONNECTOR_ACL_FILE_DELETE
            | AccessControl.CKFINDER_CONNECTOR_ACL_FILE_UPLOAD)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }

    try {
      return moveFiles();
    } catch (Exception e) {
      log.error("", e);
    }
    //this code should never be reached
    return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNKNOWN;

  }

  /**
   * move files.
   *
   * @return error code.
   */
  private int moveFiles() {
    getArguments().setFilesMoved(0);
    getArguments().setAddMoveNode(false);
    for (FilePostParam file : getArguments().getFiles()) {

      if (!FileUtils.isFileNameInvalid(file.getName())) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }
      if (Pattern.compile(Constants.INVALID_PATH_REGEX).matcher(
              file.getFolder()).find()) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }

      if (getConfiguration().getTypes().get(file.getType()) == null) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }

      if (file.getFolder() == null || file.getFolder().isEmpty()) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }
      if (FileUtils.checkFileExtension(file.getName(),
              this.getConfiguration().getTypes().get(getArguments().getType())) == 1) {
        appendErrorNodeChild(
                Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_EXTENSION,
                file.getName(), file.getFolder(), file.getType());
        continue;
      }

      if (!getArguments().getType().equals(file.getType())) {
        if (FileUtils.checkFileExtension(file.getName(),
                this.getConfiguration().getTypes().get(file.getType())) == 1) {
          appendErrorNodeChild(
                  Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_EXTENSION,
                  file.getName(), file.getFolder(), file.getType());
          continue;
        }
      }

      if (FileUtils.isFileHidden(file.getName(), this.getConfiguration())) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }

      if (FileUtils.isDirectoryHidden(file.getFolder(), this.getConfiguration())) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }

      if (!getConfiguration().getAccessControl().checkFolderACL(file.getType(), file.getFolder(),
              getArguments().getUserRole(),
              AccessControl.CKFINDER_CONNECTOR_ACL_FILE_VIEW)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
      }
      Path sourceFile = Paths.get(getConfiguration().getTypes().get(file.getType()).getPath()
              + file.getFolder(), file.getName());
      Path destFile = Paths.get(getConfiguration().getTypes().get(getArguments().getType()).getPath()
              + getArguments().getCurrentFolder(), file.getName());

      Path sourceThumb = Paths.get(getConfiguration().getThumbsPath(), file.getType()
              + file.getFolder() + file.getName());
      try {
        if (!Files.exists(sourceFile) || !Files.isRegularFile(sourceFile)) {
          appendErrorNodeChild(
                  Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND,
                  file.getName(), file.getFolder(), file.getType());
          continue;
        }
        if (!getArguments().getType().equals(file.getType())) {
          long maxSize = getConfiguration().getTypes().get(getArguments().getType()).getMaxSize();
          if (maxSize != 0 && maxSize < Files.size(sourceFile)) {
            appendErrorNodeChild(
                    Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_TOO_BIG,
                    file.getName(), file.getFolder(), file.getType());
            continue;
          }
        }
        if (sourceFile.equals(destFile)) {
          appendErrorNodeChild(
                  Constants.Errors.CKFINDER_CONNECTOR_ERROR_SOURCE_AND_TARGET_PATH_EQUAL,
                  file.getName(), file.getFolder(), file.getType());
        } else if (Files.exists(destFile)) {
          if (file.getOptions() != null
                  && file.getOptions().contains("overwrite")) {
            if (!handleOverwrite(sourceFile, destFile)) {
              appendErrorNodeChild(
                      Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED,
                      file.getName(), file.getFolder(),
                      file.getType());
            } else {
              getArguments().filesMovedPlus();
              FileUtils.delete(sourceThumb);
            }
          } else if (file.getOptions() != null
                  && file.getOptions().contains("autorename")) {
            if (!handleAutoRename(sourceFile, destFile)) {
              appendErrorNodeChild(
                      Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED,
                      file.getName(), file.getFolder(),
                      file.getType());
            } else {
              getArguments().filesMovedPlus();
              FileUtils.delete(sourceThumb);
            }
          } else {
            appendErrorNodeChild(
                    Constants.Errors.CKFINDER_CONNECTOR_ERROR_ALREADY_EXIST,
                    file.getName(), file.getFolder(), file.getType());
          }
        } else if (FileUtils.copyFromSourceToDestFile(sourceFile, destFile,
                true, getConfiguration())) {
          getArguments().filesMovedPlus();
          moveThumb(file);
        }
      } catch (SecurityException | IOException e) {
        log.error("", e);
        appendErrorNodeChild(
                Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED,
                file.getName(), file.getFolder(), file.getType());
      }

    }
    getArguments().setAddMoveNode(true);
    if (hasErrors()) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_MOVE_FAILED;
    } else {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
    }
  }

  /**
   * Handles autorename option.
   *
   * @param sourceFile source file to move from.
   * @param destFile destination file to move to.
   * @return true if moved correctly
   * @throws IOException when ioerror occurs
   */
  private boolean handleAutoRename(Path sourceFile, Path destFile)
          throws IOException {
    int counter = 1;
    Path newDestFile;
    String fileNameWithoutExtension = FileUtils.getFileNameWithoutExtension(destFile.getFileName().toString(), false);
    while (true) {
      String newFileName = fileNameWithoutExtension
              + "(" + counter + ")."
              + FileUtils.getFileExtension(destFile.getFileName().toString(), false);
      newDestFile = destFile.getParent().resolve(newFileName);
      if (!Files.exists(newDestFile)) {
        // can't be in one if=, because when error in
        // copy file occurs then it will be infinity loop
        return (FileUtils.copyFromSourceToDestFile(sourceFile,
                newDestFile, true, getConfiguration()));
      } else {
        counter++;
      }
    }
  }

  /**
   * Handles overwrite option.
   *
   * @param sourceFile source file to move from.
   * @param destFile destination file to move to.
   * @return true if moved correctly
   * @throws IOException when ioerror occurs
   */
  private boolean handleOverwrite(Path sourceFile, Path destFile)
          throws IOException {
    return FileUtils.delete(destFile)
            && FileUtils.copyFromSourceToDestFile(sourceFile, destFile,
                    true, getConfiguration());
  }

  /**
   * move thumb file.
   *
   * @param file file to move.
   * @throws IOException when ioerror occurs
   */
  private void moveThumb(FilePostParam file) throws IOException {
    Path sourceThumbFile = Paths.get(getConfiguration().getThumbsPath(),
            file.getType()
            + file.getFolder() + file.getName());
    Path destThumbFile = Paths.get(getConfiguration().getThumbsPath(),
            getArguments().getType()
            + getArguments().getCurrentFolder()
            + file.getName()
    );

    FileUtils.copyFromSourceToDestFile(sourceThumbFile, destThumbFile,
            true, getConfiguration());

  }

  @Override
  @SuppressWarnings("CollectionWithoutInitialCapacity")
  protected void initParams(HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(request, configuration);
    getArguments().setFiles(new ArrayList<>());
    getArguments().setMovedAll(request.getParameter("moved") != null ? Integer.parseInt(request.getParameter("moved")) : 0);
    getFilesListFromRequest(request);
  }

  /**
   * get file list to copy from request.
   *
   * @param request request
   */
  private void getFilesListFromRequest(HttpServletRequest request) {
    int i = 0;
    while (true) {
      String paramName = "files[" + i + "][name]";
      String name = request.getParameter(paramName);
      if (name != null) {
        String folder = request.getParameter("files[" + i + "][folder]");
        String options = request.getParameter("files[" + i + "][options]");
        String type = request.getParameter("files[" + i + "][type]");
        getArguments().getFiles().add(FilePostParam.builder().name(name).folder(folder).options(options).type(type).build());
      } else {
        break;
      }
      i++;
    }
  }

}
