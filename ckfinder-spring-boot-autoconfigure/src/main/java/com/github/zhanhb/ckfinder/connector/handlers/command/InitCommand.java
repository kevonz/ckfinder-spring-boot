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
import com.github.zhanhb.ckfinder.connector.data.InitCommandEventArgs;
import com.github.zhanhb.ckfinder.connector.data.ResourceType;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.XMLArguments;
import com.github.zhanhb.ckfinder.connector.handlers.response.Connector;
import com.github.zhanhb.ckfinder.connector.handlers.response.ConnectorInfo;
import com.github.zhanhb.ckfinder.connector.handlers.response.ResourceTypes;
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
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

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
   * @param configuration connector configuration
   * @return 0
   */
  @Override
  protected int getDataForXml(XMLArguments arguments, IConfiguration configuration) {
    return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Connector.Builder rootElement, XMLArguments arguments, IConfiguration configuration) {
    if (errorNum == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE) {
      createConnectorData(rootElement, arguments, configuration);
      try {
        createResouceTypesData(rootElement, arguments, configuration);
      } catch (Exception e) {
        log.error("", e);
      }
      createPluginsData(rootElement, arguments, configuration);
    }
  }

  /**
   * Creates connector node in XML.
   *
   * @param rootElement root element in XML
   */
  private void createConnectorData(Connector.Builder rootElement, XMLArguments arguments, IConfiguration configuration) {
    // connector info
    ConnectorInfo.Builder element = ConnectorInfo.builder();
    element.enabled(configuration.isEnabled());
    element.licenseName(getLicenseName(configuration));
    element.licenseKey(createLicenseKey(configuration.getLicenseKey()));
    element.thumbsEnabled(configuration.isThumbsEnabled());
    element.uploadCheckImages(!configuration.isCheckSizeAfterScaling());
    if (configuration.isThumbsEnabled()) {
      element.thumbsUrl(configuration.getThumbsUrl());
      element.thumbsDirectAccess(configuration.isThumbsDirectAccess());
      element.thumbsWidth(configuration.getMaxThumbWidth());
      element.thumbsHeight(configuration.getMaxThumbHeight());
    }
    element.imgWidth(configuration.getImgWidth());
    element.imgHeight(configuration.getImgHeight());
    element.csrfProtection(configuration.isEnableCsrfProtection());
    String plugins = getPlugins(configuration);
    if (plugins.length() > 0) {
      element.plugins(plugins);
    }
    rootElement.connectorInfo(element.build());
  }

  /**
   * gets plugins names.
   *
   * @return plugins names.
   */
  private String getPlugins(IConfiguration configuration) {
    return configuration.getPublicPluginNames();
  }

  /**
   * checks license key.
   *
   * @return license name if key is ok, or empty string if not.
   */
  private String getLicenseName(IConfiguration configuration) {
    if (validateLicenseKey(configuration.getLicenseKey())) {
      int index = Constants.CKFINDER_CHARS.indexOf(configuration.getLicenseKey().charAt(0))
              % LICENSE_CHAR_NR;
      if (index == 1 || index == 4) {
        return configuration.getLicenseName();
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
      StringBuilder sb = new StringBuilder(LICENSE_CHARS.length);
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
  private void createPluginsData(Connector.Builder rootElement, XMLArguments arguments, IConfiguration configuration) {
    if (configuration.getEvents() != null) {
      InitCommandEventArgs args = new InitCommandEventArgs(rootElement);
      configuration.getEvents().runInitCommand(args, configuration);
    }
  }

  /**
   * Creates plugins node in XML.
   *
   * @param rootElement root element in XML
   * @throws Exception when error occurs
   */
  @SuppressWarnings("CollectionWithoutInitialCapacity")
  private void createResouceTypesData(Connector.Builder rootElement, XMLArguments arguments, IConfiguration configuration) throws IOException {
    //resurcetypes
    ResourceTypes.Builder resourceTypes = ResourceTypes.builder();
    Set<String> types;
    if (arguments.getType() != null && !arguments.getType().isEmpty()) {
      types = new LinkedHashSet<>();
      types.add(arguments.getType());
    } else {
      types = getTypes(configuration);
    }

    for (String key : types) {
      ResourceType resourceType = configuration.getTypes().get(key);
      if (((arguments.getType() == null || arguments.getType().equals(key)) && resourceType != null)
              && configuration.getAccessControl().hasPermission(key, "/", arguments.getUserRole(),
                      AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_VIEW)) {

        com.github.zhanhb.ckfinder.connector.handlers.response.ResourceType.Builder childElement = com.github.zhanhb.ckfinder.connector.handlers.response.ResourceType.builder();
        childElement.name(resourceType.getName());
        childElement.acl(configuration.getAccessControl().checkACLForRole(key, "/", arguments.getUserRole()));
        childElement.hash(randomHash(
                resourceType.getPath()));
        childElement.allowedExtensions(resourceType.getAllowedExtensions());
        childElement.deniedExtensions(resourceType.getDeniedExtensions());
        childElement.url(resourceType.getUrl() + "/");
        long maxSize = resourceType.getMaxSize();
        childElement.maxSize(maxSize > 0 ? maxSize : 0);
        boolean hasChildren = FileUtils.hasChildren(configuration.getAccessControl(), "/", Paths.get(PathUtils.escape(resourceType.getPath())), configuration, resourceType.getName(), arguments.getUserRole());
        childElement.hasChildren(hasChildren);
        resourceTypes.resourceType(childElement.build());
      }
    }
    rootElement.resourceTypes(resourceTypes.build());
  }

  /**
   * gets list of types names.
   *
   * @return list of types names.
   */
  private Set<String> getTypes(IConfiguration configuration) {
    if (configuration.getDefaultResourceTypes().size() > 0) {
      return configuration.getDefaultResourceTypes();
    } else {
      return configuration.getTypes().keySet();
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

      StringBuilder hexString = new StringBuilder(messageDigest.length << 1);

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
  protected boolean shouldAddCurrentFolderNode(XMLArguments arguments) {
    return false;
  }

  @Deprecated
  @Override
  String getCurrentFolderParam(HttpServletRequest request) {
    return null;
  }

}
