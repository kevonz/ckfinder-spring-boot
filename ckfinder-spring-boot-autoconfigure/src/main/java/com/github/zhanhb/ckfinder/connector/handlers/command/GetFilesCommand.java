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
import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.GetFilesArguments;
import com.github.zhanhb.ckfinder.connector.handlers.response.Connector;
import com.github.zhanhb.ckfinder.connector.handlers.response.File;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.github.zhanhb.ckfinder.connector.utils.ImageUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to handle <code>GetFiles</code> command.
 */
@Slf4j
public class GetFilesCommand extends XMLCommand<GetFilesArguments> {

  /**
   * number of bytes in kilobyte.
   */
  private static final float BYTES = 1024f;

  public GetFilesCommand() {
    super(GetFilesArguments::new);
  }

  /**
   * initializing parameters for command handler.
   *
   * @param arguments
   * @param request request
   * @param configuration connector configuration
   * @throws ConnectorException when error occurs
   */
  @Override
  protected void initParams(GetFilesArguments arguments, HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(arguments, request, configuration);

    arguments.setShowThumbs(request.getParameter("showThumbs"));
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Connector.Builder rootElement, GetFilesArguments arguments, IConfiguration configuration) {
    if (errorNum == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE) {
      createFilesData(rootElement, arguments, configuration);
    }
  }

  /**
   * gets data to XML response.
   *
   * @param arguments
   * @return 0 if ok, otherwise error code
   */
  @Override
  protected int getDataForXml(GetFilesArguments arguments, IConfiguration configuration) {
    try {
      checkTypeExists(arguments.getType(), configuration);
    } catch (ConnectorException ex) {
      arguments.setType(null);
      return ex.getErrorCode();
    }

    arguments.setFullCurrentPath(Paths.get(configuration.getTypes().get(arguments.getType()).getPath(),
            arguments.getCurrentFolder()).toString());

    if (!configuration.getAccessControl().hasPermission(arguments.getType(),
            arguments.getCurrentFolder(), arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_VIEW)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }

    Path dir = Paths.get(arguments.getFullCurrentPath());
    try {
      if (!Files.exists(dir)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND;
      }
      arguments.setFiles(FileUtils.findChildrensList(dir, false));
    } catch (IOException | SecurityException e) {
      log.error("", e);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
    }
    filterListByHiddenAndNotAllowed(arguments, configuration);
    Collections.sort(arguments.getFiles());
    return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
  }

  /**
   *
   *
   */
  private void filterListByHiddenAndNotAllowed(GetFilesArguments arguments, IConfiguration configuration) {
    List<String> tmpFiles = arguments.getFiles().stream()
            .filter(file -> (FileUtils.checkFileExtension(file, configuration.getTypes().get(arguments.getType())) == 0
            && !FileUtils.isFileHidden(file, configuration)))
            .collect(Collectors.toList());

    arguments.getFiles().clear();
    arguments.getFiles().addAll(tmpFiles);

  }

  /**
   * creates files data node in response XML.
   *
   * @param rootElement root element from XML.
   */
  private void createFilesData(Connector.Builder rootElement, GetFilesArguments arguments, IConfiguration configuration) {
    com.github.zhanhb.ckfinder.connector.handlers.response.Files.Builder files = com.github.zhanhb.ckfinder.connector.handlers.response.Files.builder();
    for (String filePath : arguments.getFiles()) {
      Path file = Paths.get(arguments.getFullCurrentPath(), filePath);
      if (Files.exists(file)) {
        try {
          File.Builder builder = File.builder()
                  .name(filePath)
                  .date(FileUtils.parseLastModifDate(file))
                  .size(getSize(file));
          if (ImageUtils.isImageExtension(file) && isAddThumbsAttr(arguments, configuration)) {
            String attr = createThumbAttr(file, arguments, configuration);
            if (!attr.isEmpty()) {
              builder.thumb(attr);
            }
          }
          files.file(builder.build());
        } catch (IOException ex) {
        }
      }
    }
    rootElement.files(files.build());
  }

  /**
   * gets thumb attribute value.
   *
   * @param file file to check if has thumb.
   * @return thumb attribute values
   */
  private String createThumbAttr(Path file, GetFilesArguments arguments, IConfiguration configuration) {
    Path thumbFile = Paths.get(configuration.getThumbsPath(),
            arguments.getType(), arguments.getCurrentFolder(),
            file.getFileName().toString());
    if (Files.exists(thumbFile)) {
      return file.getFileName().toString();
    } else if (isShowThumbs(arguments)) {
      return "?".concat(file.getFileName().toString());
    } else {
      return "";
    }
  }

  /**
   * get file size.
   *
   * @param file file
   * @return file size
   */
  private String getSize(Path file) throws IOException {
    long size = Files.size(file);
    if (size > 0 && size < BYTES) {
      return "1";
    } else {
      return String.valueOf(Math.round(size / BYTES));
    }
  }

  /**
   * Check if show thumbs or not (add attr to file node with thumb file name).
   *
   * @return true if show thumbs
   */
  private boolean isAddThumbsAttr(GetFilesArguments arguments, IConfiguration configuration) {
    return configuration.isThumbsEnabled()
            && (configuration.isThumbsDirectAccess()
            || isShowThumbs(arguments));
  }

  /**
   * checks show thumb request attribute.
   *
   * @return true if is set.
   */
  private boolean isShowThumbs(GetFilesArguments arguments) {
    return (arguments.getShowThumbs() != null && arguments.getShowThumbs().equals("1"));
  }

}
