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
import com.github.zhanhb.ckfinder.connector.data.InitCommandEventArgs;
import com.github.zhanhb.ckfinder.connector.data.ResourceType;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.XMLArguments;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.github.zhanhb.ckfinder.connector.utils.PathUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

/**
 * Class to handle <code>Init</code> command.
 */
@Slf4j
public class InitCommand extends XMLCommand<XMLArguments> {

  /**
   * chars taken to license key.
   */
  private static final int[] LICENSE_CHARS = {11, 0, 8, 12, 26, 2, 3, 25, 1};
  private static final int LICENSE_CHAR_NR = 5;
  private static final int LICENSE_KEY_LENGTH = 34;

  public InitCommand() {
    super(XMLArguments::new);
  }

  /**
   * method from super class - not used in this command.
   *
   * @param arguments
   * @return 0
   */
  @Override
  protected int getDataForXml(XMLArguments arguments) {
    return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Element rootElement, XMLArguments arguments) {
    if (errorNum == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE) {
      createConnectorData(rootElement, arguments);
      try {
        createResouceTypesData(rootElement, arguments);
      } catch (Exception e) {
        log.error("", e);
      }
      createPluginsData(rootElement, arguments);
    }
  }

  /**
   * Creates connector node in XML.
   *
   * @param rootElement root element in XML
   */
  private void createConnectorData(Element rootElement, XMLArguments arguments) {
    // connector info
    Element element = arguments.getDocument().createElement("ConnectorInfo");
    element.setAttribute("enabled", String.valueOf(getConfiguration().isEnabled()));
    element.setAttribute("s", getLicenseName());
    element.setAttribute("c", createLicenseKey(getConfiguration().getLicenseKey()));
    element.setAttribute("thumbsEnabled", String.valueOf(getConfiguration().isThumbsEnabled()));
    element.setAttribute("uploadCheckImages", getConfiguration().isCheckSizeAfterScaling() ? "false" : "true");
    if (getConfiguration().isThumbsEnabled()) {
      element.setAttribute("thumbsUrl", getConfiguration().getThumbsUrl());
      element.setAttribute("thumbsDirectAccess", String.valueOf(getConfiguration().isThumbsDirectAccess()));
      element.setAttribute("thumbsWidth", String.valueOf(getConfiguration().getMaxThumbWidth()));
      element.setAttribute("thumbsHeight", String.valueOf(getConfiguration().getMaxThumbHeight()));
    }
    element.setAttribute("imgWidth", String.valueOf(getConfiguration().getImgWidth()));
    element.setAttribute("imgHeight", String.valueOf(getConfiguration().getImgHeight()));
    if (getConfiguration().getPlugins().size() > 0) {
      element.setAttribute("plugins", getPlugins());
    }
    rootElement.appendChild(element);
  }

  /**
   * gets plugins names.
   *
   * @return plugins names.
   */
  private String getPlugins() {
    return getConfiguration().getPlugins().stream()
            .filter(item -> !item.isInternal())
            .map(item -> item.getName())
            .collect(Collectors.joining(","));
  }

  /**
   * checks license key.
   *
   * @return license name if key is ok, or empty string if not.
   */
  private String getLicenseName() {
    if (validateLicenseKey(getConfiguration().getLicenseKey())) {
      int index = Constants.CKFINDER_CHARS.indexOf(getConfiguration().getLicenseKey().charAt(0))
              % LICENSE_CHAR_NR;
      if (index == 1 || index == 4) {
        return getConfiguration().getLicenseName();
      }
    }
    return "";
  }

  /**
   * Creates license key from key in configuration.
   *
   * @param licenseKey license key from configuration
   * @return hashed license key
   */
  private String createLicenseKey(String licenseKey) {
    if (validateLicenseKey(licenseKey)) {
      @SuppressWarnings("StringBufferWithoutInitialCapacity")
      StringBuilder sb = new StringBuilder();
      for (int i : LICENSE_CHARS) {
        sb.append(licenseKey.charAt(i));
      }
      return sb.toString();
    }
    return "";
  }

  /**
   * validates license key length.
   *
   * @param licenseKey config license key
   * @return true if has correct length
   */
  private boolean validateLicenseKey(String licenseKey) {
    return licenseKey != null && licenseKey.length() == LICENSE_KEY_LENGTH;
  }

  /**
   * Creates plugins node in XML.
   *
   * @param rootElement root element in XML
   */
  private void createPluginsData(Element rootElement, XMLArguments arguments) {
    Element element = arguments.getDocument().createElement("PluginsInfo");
    rootElement.appendChild(element);
    if (getConfiguration().getEvents() != null) {
      InitCommandEventArgs args = new InitCommandEventArgs(arguments.getDocument(), rootElement);
      getConfiguration().getEvents().runInitCommand(args, getConfiguration());
    }
  }

  /**
   * Creates plugins node in XML.
   *
   * @param rootElement root element in XML
   * @throws Exception when error occurs
   */
  @SuppressWarnings("CollectionWithoutInitialCapacity")
  private void createResouceTypesData(Element rootElement, XMLArguments arguments) throws IOException {
    //resurcetypes
    Element element = arguments.getDocument().createElement("ResourceTypes");
    rootElement.appendChild(element);

    Set<String> types;
    if (arguments.getType() != null && !arguments.getType().isEmpty()) {
      types = new LinkedHashSet<>();
      types.add(arguments.getType());
    } else {
      types = getTypes();
    }

    for (String key : types) {
      ResourceType resourceType = getConfiguration().getTypes().get(key);
      if (((arguments.getType() == null || arguments.getType().equals(key)) && resourceType != null)
              && getConfiguration().getAccessControl().hasPermission(key, "/", arguments.getUserRole(),
                      AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_VIEW)) {

        Element childElement = arguments.getDocument().
                createElement("ResourceType");
        childElement.setAttribute("name", resourceType.getName());
        childElement.setAttribute("acl", String.valueOf(getConfiguration().getAccessControl().checkACLForRole(key, "/", arguments.getUserRole())));
        childElement.setAttribute("hash", randomHash(
                resourceType.getPath()));
        childElement.setAttribute("allowedExtensions",
                resourceType.getAllowedExtensions());
        childElement.setAttribute("deniedExtensions",
                resourceType.getDeniedExtensions());
        childElement.setAttribute("url", resourceType.getUrl() + "/");
        long maxSize = resourceType.getMaxSize();
        childElement.setAttribute("maxSize", maxSize > 0 ? Long.toString(maxSize) : "0");
        boolean hasChildren = FileUtils.hasChildren(getConfiguration().getAccessControl(), "/", Paths.get(PathUtils.escape(resourceType.getPath())), getConfiguration(), resourceType.getName(), arguments.getUserRole());
        childElement.setAttribute("hasChildren", String.valueOf(hasChildren));
        element.appendChild(childElement);
      }
    }
  }

  /**
   * gets list of types names.
   *
   * @return list of types names.
   */
  private Set<String> getTypes() {
    if (getConfiguration().getDefaultResourceTypes().size() > 0) {
      return getConfiguration().getDefaultResourceTypes();
    } else {
      return getConfiguration().getTypes().keySet();
    }
  }

  /**
   * Gets hash for folders in XML response to avoid cached responses.
   *
   * @param folder folder
   * @return hash value
   */
  private String randomHash(String folder) {
    try {
      MessageDigest algorithm = MessageDigest.getInstance("SHA-256");
      byte[] messageDigest = algorithm.digest(folder.getBytes("UTF8"));

      @SuppressWarnings("StringBufferWithoutInitialCapacity")
      StringBuilder hexString = new StringBuilder();

      for (int i = 0; i < messageDigest.length; i++) {
        hexString.append(Integer.toString((messageDigest[i] & 0xff) + 0x100, 16).substring(1));
      }
      return hexString.substring(0, 15);
    } catch (NoSuchAlgorithmException e) {
      log.error("", e);
      return "";
    } catch (UnsupportedEncodingException ex) {
      throw new AssertionError(ex);
    }
  }

  @Override
  protected boolean mustAddCurrentFolderNode(XMLArguments arguments) {
    return false;
  }

  @Override
  protected void setCurrentFolderParam(HttpServletRequest request, XMLArguments arguments) {
    arguments.setCurrentFolder(null);
  }

}
