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
import com.github.zhanhb.ckfinder.connector.data.XmlAttribute;
import com.github.zhanhb.ckfinder.connector.data.XmlElementData;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.GetFoldersArguments;
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
import org.w3c.dom.Element;

/**
 * Class to handle <code>GetFolders</code> command.
 */
@Slf4j
public class GetFoldersCommand extends XMLCommand<GetFoldersArguments> {

  public GetFoldersCommand() {
    super(GetFoldersArguments::new);
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Element rootElement, GetFoldersArguments arguments) throws IOException {
    if (errorNum == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE) {
      createFoldersData(rootElement, arguments);
    }
  }

  /**
   * gets data for response.
   *
   * @param arguments
   * @return 0 if everything went ok or error code otherwise
   * @throws java.io.IOException
   */
  @Override
  protected int getDataForXml(GetFoldersArguments arguments) throws IOException {
    if (!isTypeExists(arguments, arguments.getType())) {
      arguments.setType(null);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE;
    }

    if (!getConfiguration().getAccessControl().checkFolderACL(arguments.getType(),
            arguments.getCurrentFolder(), arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_VIEW)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }
    if (FileUtils.isDirectoryHidden(arguments.getCurrentFolder(), getConfiguration())) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    Path dir = Paths.get(getConfiguration().getTypes().get(arguments.getType()).getPath()
            + arguments.getCurrentFolder());
    try {
      if (!Files.exists(dir)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND;
      }

      arguments.setDirectories(FileUtils.findChildrensList(dir, true));
    } catch (SecurityException e) {
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
            .filter(dir -> (getConfiguration().getAccessControl().checkFolderACL(arguments.getType(), arguments.getCurrentFolder() + dir, arguments.getUserRole(),
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
  private void createFoldersData(Element rootElement, GetFoldersArguments arguments) throws IOException {
    Element element = arguments.getDocument().createElement("Folders");
    for (String dirPath : arguments.getDirectories()) {
      Path dir = Paths.get(this.getConfiguration().getTypes().get(arguments.getType()).getPath()
              + arguments.getCurrentFolder()
              + dirPath);
      if (Files.exists(dir)) {
        XmlElementData.Builder xmlElementData = XmlElementData.builder().name("Folder");
        xmlElementData.attribute(new XmlAttribute("name", dirPath));

        boolean hasChildren = FileUtils.hasChildren(getConfiguration().getAccessControl(),
                arguments.getCurrentFolder() + dirPath + "/", dir,
                getConfiguration(), arguments.getType(), arguments.getUserRole());
        xmlElementData.attribute(new XmlAttribute("hasChildren",
                String.valueOf(hasChildren)));

        xmlElementData.attribute(new XmlAttribute("acl",
                String.valueOf(getConfiguration().getAccessControl()
                        .checkACLForRole(arguments.getType(),
                        arguments.getCurrentFolder()
                        + dirPath, arguments.getUserRole()))));
        xmlElementData.build().addToDocument(arguments.getDocument(), element);
      }
    }
    rootElement.appendChild(element);
  }

}
