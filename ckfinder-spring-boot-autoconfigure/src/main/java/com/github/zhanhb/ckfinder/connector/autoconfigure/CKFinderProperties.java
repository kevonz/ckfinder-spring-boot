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

import java.util.List;
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
  private String baseURL;
  private String licenseKey;
  private String licenseName;
  private Integer imgWidth;
  private Integer imgHeight;
  private Float imgQuality;
  private String defaultResourceTypes;
  private List<Type> types;
  private Thumbs thumbs = new Thumbs();
  private Boolean disallowUnsafeCharacters;
  private Boolean checkDoubleExtension;
  private Boolean checkSizeAfterScaling;
  private Boolean secureImageUploads;
  private String htmlExtensions;
  private Boolean forceASCII;
  private Boolean enableCsrfProtection;
  private String hideFolders;
  private String hideFiles;
  private String basePathBuilder;

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

}
