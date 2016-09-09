/*
 * Copyright 2016 ZJNU ACM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.zhanhb.ckfinder.connector.autoconfigure;

import java.util.Properties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * @author zhanhb
 */
@Getter
@Setter
@ConfigurationProperties(prefix = CKFinderProperties.CKFINDER_PREFIX)
@SuppressWarnings({"PublicInnerClass", "ReturnOfCollectionOrArrayField", "AssignmentToCollectionOrArrayFieldFromParameter"})
public class CKFinderProperties {

  public static final String CKFINDER_PREFIX = "ckfinder";

  private Boolean enabled;
  private String baseDir;
  private String baseUrl;
  private String licenseKey;
  private String licenseName;
  private Integer imgWidth;
  private Integer imgHeight;
  private Float imgQuality;
  private String[] defaultResourceTypes = {};
  private Type[] types = {};
  private String userRoleSessionVar;
  private AccessControl[] accessControls = {};
  private Thumbs thumbs = new Thumbs();
  private Boolean disallowUnsafeCharacters;
  private Boolean checkDoubleExtension;
  private Boolean checkSizeAfterScaling;
  private Boolean secureImageUploads;
  private String[] htmlExtensions = {};
  private Boolean forceAscii;
  private Boolean enableCsrfProtection;
  private String[] hideFolders = {};
  private String[] hideFiles = {};
  private Watermark watermark = new Watermark();
  private ImageResize imageResize = new ImageResize();

  @Getter
  @Setter
  public static class Type {

    private String name;
    private String url;
    private String directory;
    private Integer maxSize;
    private String allowedExtensions;
    private String deniedExtensions;

  }

  @Getter
  @Setter
  public static class Thumbs {

    private Boolean enabled;
    private String url;
    private String directory;
    private Boolean directAccess;
    private Integer maxHeight;
    private Integer maxWidth;
    private Float quality;

  }

  @Getter
  @Setter
  public static class AccessControl {

    private String role;
    private String resourceType;
    private String folder;
    private boolean folderView;
    private boolean folderCreate;
    private boolean folderRename;
    private boolean folderDelete;
    private boolean fileView;
    private boolean fileUpload;
    private boolean fileRename;
    private boolean fileDelete;

  }

  @Getter
  @Setter
  public static class ImageResize {

    private Boolean enabled;
    private Properties params;

  }

  @Getter
  @Setter
  public static class Watermark {

    private Boolean enabled;
    private String source;
    private Float transparency;
    private Float quality;
    private Integer marginBottom;
    private Integer marginRight;

  }

}
