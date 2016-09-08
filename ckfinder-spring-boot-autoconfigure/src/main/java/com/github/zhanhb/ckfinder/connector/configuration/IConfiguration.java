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
package com.github.zhanhb.ckfinder.connector.configuration;

import com.github.zhanhb.ckfinder.connector.data.ResourceType;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for configuration.
 */
public interface IConfiguration {

  int DEFAULT_IMG_WIDTH = 500;
  int DEFAULT_IMG_HEIGHT = 400;
  int DEFAULT_THUMB_MAX_WIDTH = 100;
  int DEFAULT_THUMB_MAX_HEIGHT = 100;
  float DEFAULT_IMG_QUALITY = 0.8f;
  String DEFAULT_THUMBS_URL = "_thumbs/";
  String DEFAULT_THUMBS_DIR = "%BASE_DIR%/_thumbs/";
  String DEFAULT_BASE_URL = "/userfiles";

  /**
   * gets user role name sets in config.
   *
   * @return role name
   */
  public String getUserRoleName();

  /**
   * gets resources map types with names as map keys.
   *
   * @return resources map
   */
  public Map<String, ResourceType> getTypes();

  /**
   * returns license key.
   *
   * @return license key
   */
  public String getLicenseKey();

  /**
   * returns license name.
   *
   * @return license name.
   */
  public String getLicenseName();

  /**
   * gets image max width.
   *
   * @return max image height
   */
  public int getImgWidth();

  /**
   * get image max height.
   *
   * @return max image height
   */
  public int getImgHeight();

  /**
   * get image quality.
   *
   * @return image quality
   */
  public float getImgQuality();

  /**
   * check if connector is enabled.
   *
   * @return if connector is enabled
   */
  public boolean isEnabled();

  /**
   * check if thums are enabled.
   *
   * @return true if thums are enabled
   */
  public boolean isThumbsEnabled();

  /**
   * gets url to thumbs dir(path from baseUrl).
   *
   * @return thumbs url
   */
  public String getThumbsUrl();

  /**
   * gets path to thumbs directory.
   *
   * @return thumbs directory
   */
  public String getThumbsDir();

  /**
   * gets path to thumbs directory.
   *
   * @return thumbs directory
   */
  public String getThumbsPath();

  /**
   * gets thumbs quality.
   *
   * @return thumbs quality
   */
  public float getThumbsQuality();

  /**
   * checks if thumbs are accessed direct.
   *
   * @return true if thumbs can be accessed directly
   */
  public boolean isThumbsDirectAccess();

  /**
   * gets max width of thumb.
   *
   * @return max width of thumb
   */
  public int getMaxThumbWidth();

  /**
   * gets max height of thumb.
   *
   * @return max height of thumb
   */
  public int getMaxThumbHeight();

  /**
   * get regex for hidden folders.
   *
   * @return regex for hidden folders
   */
  public List<String> getHiddenFolders();

  /**
   * get regex for hidden files.
   *
   * @return regex for hidden files
   */
  public List<String> getHiddenFiles();

  /**
   * get double extensions configuration.
   *
   * @return configuration value.
   */
  public boolean isCheckDoubleFileExtensions();

  /**
   * flag to check if force ASCII.
   *
   * @return true if force ASCII.
   */
  public boolean isForceAscii();

  /**
   * Checks if disallowed characters in file and folder names are turned on.
   *
   * @return disallowUnsafeCharacters
   */
  public boolean isDisallowUnsafeCharacters();

  /**
   * Returns flag indicating if Cross-site request forgery (CSRF) protection has
   * been enabled.
   *
   * @return {@code boolean} flag indicating if CSRF protection has been
   * enabled.
   */
  public boolean isEnableCsrfProtection();

  /**
   * flag if check image size after resizing image.
   *
   * @return true if check.
   */
  public boolean isCheckSizeAfterScaling();

  /**
   * gets a list of plugins.
   *
   * @return list of plugins.
   */
  public List<String> getPublicPluginNames();

  /**
   * gets events.
   *
   * @return events.
   */
  public Events getEvents();

  /**
   * gets param SecureImageUploads.
   *
   * @return true if is set
   */
  public boolean isSecureImageUploads();

  /**
   * gets html extensions.
   *
   * @return list of html extensions.
   */
  public List<String> getHtmlExtensions();

  /**
   * gets a list of default resource types.
   *
   * @return list of default resource types
   */
  public Set<String> getDefaultResourceTypes();

  /**
   *
   * @return the configuration
   */
  public AccessControl getAccessControl();

}
