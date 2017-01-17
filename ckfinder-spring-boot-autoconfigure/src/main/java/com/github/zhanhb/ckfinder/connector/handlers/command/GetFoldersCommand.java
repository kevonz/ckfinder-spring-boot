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
import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.GetFoldersArguments;
import com.github.zhanhb.ckfinder.connector.handlers.response.Connector;
import com.github.zhanhb.ckfinder.connector.handlers.response.Folder;
import com.github.zhanhb.ckfinder.connector.handlers.response.Folders;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to handle <code>GetFolders</code> command.
 */
@Slf4j
public class GetFoldersCommand extends XMLCommand<GetFoldersArguments> {

  public GetFoldersCommand() {
    super(GetFoldersArguments::new);
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Connector.Builder rootElement, GetFoldersArguments arguments) {
    if (errorNum == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE) {
      createFoldersData(rootElement, arguments);
    }
  }

  /**
   * gets data for response.
   *
   * @param arguments
   * @return 0 if everything went ok or error code otherwise
   */
  @Override
  protected int getDataForXml(GetFoldersArguments arguments) {
    try {
      checkTypeExists(arguments.getType());
    } catch (ConnectorException ex) {
      arguments.setType(null);
      return ex.getErrorCode();
    }

    if (!getConfiguration().getAccessControl().hasPermission(arguments.getType(),
            arguments.getCurrentFolder(), arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_VIEW)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }
    if (FileUtils.isDirectoryHidden(arguments.getCurrentFolder(), getConfiguration())) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    Path dir = Paths.get(getConfiguration().getTypes().get(arguments.getType()).getPath(),
            arguments.getCurrentFolder());
    try {
      if (!Files.exists(dir)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND;
      }

      arguments.setDirectories(FileUtils.findChildrensList(dir, true));
    } catch (IOException | SecurityException e) {
      log.error("", e);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
    }
    filterListByHiddenAndNotAllowed(arguments);
    Collections.sort(arguments.getDirectories());
    return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
  }

  /**
   * filters list and check if every element is not hidden and have correct ACL.
   */
  private void filterListByHiddenAndNotAllowed(GetFoldersArguments arguments) {
    List<String> tmpDirs = arguments.getDirectories().stream()
            .filter(dir -> (getConfiguration().getAccessControl().hasPermission(arguments.getType(), arguments.getCurrentFolder() + dir, arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_VIEW)
            && !FileUtils.isDirectoryHidden(dir, getConfiguration())))
            .collect(Collectors.toList());

    arguments.getDirectories().clear();
    arguments.getDirectories().addAll(tmpDirs);

  }

  /**
   * creates folder data node in XML document.
   *
   * @param rootElement root element in XML document
   */
  private void createFoldersData(Connector.Builder rootElement, GetFoldersArguments arguments) {
    Folders.Builder folders = Folders.builder();
    for (String dirPath : arguments.getDirectories()) {
      Path dir = Paths.get(this.getConfiguration().getTypes().get(arguments.getType()).getPath(),
              arguments.getCurrentFolder(), dirPath);
      if (Files.exists(dir)) {
        boolean hasChildren = FileUtils.hasChildren(getConfiguration().getAccessControl(),
                arguments.getCurrentFolder() + dirPath + "/", dir,
                getConfiguration(), arguments.getType(), arguments.getUserRole());
        folders.folder(Folder.builder()
                .name(dirPath)
                .hasChildren(hasChildren)
                .acl(
                        getConfiguration().getAccessControl()
                                .checkACLForRole(arguments.getType(),
                                        arguments.getCurrentFolder()
                                        + dirPath, arguments.getUserRole())).build());
      }
    }
    rootElement.folders(folders.build());
  }

}
