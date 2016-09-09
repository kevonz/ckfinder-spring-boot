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

import com.github.zhanhb.ckfinder.connector.configuration.Constants;
import com.github.zhanhb.ckfinder.connector.configuration.DefaultPathBuilder;
import com.github.zhanhb.ckfinder.connector.configuration.IBasePathBuilder;
import com.github.zhanhb.ckfinder.connector.configuration.IConfiguration;
import com.github.zhanhb.ckfinder.connector.configuration.Plugin;
import com.github.zhanhb.ckfinder.connector.data.AccessControlLevel;
import com.github.zhanhb.ckfinder.connector.data.PluginInfo;
import com.github.zhanhb.ckfinder.connector.data.ResourceType;
import com.github.zhanhb.ckfinder.connector.plugins.FileEditor;
import com.github.zhanhb.ckfinder.connector.plugins.ImageResize;
import com.github.zhanhb.ckfinder.connector.plugins.Watermark;
import com.github.zhanhb.ckfinder.connector.plugins.WatermarkSettings;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 *
 * @author zhanhb
 */
@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(CKFinderProperties.class)
@SuppressWarnings("PublicInnerClass")
public class CKFinderAutoConfiguration {

  @Configuration
  @ConditionalOnMissingBean(IBasePathBuilder.class)
  public static class DefaultBasePathBuilderConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public DefaultPathBuilder defaultPathBuilder() {
      ServletContext servletContext = applicationContext.getBean(ServletContext.class);
      String baseDir = servletContext.getRealPath(IConfiguration.DEFAULT_BASE_URL);
      return DefaultPathBuilder.builder()
              .baseDir(baseDir)
              .baseUrl(IConfiguration.DEFAULT_BASE_URL)
              .build();
    }
  }

  @Configuration
  @ConditionalOnMissingBean(AccessControl.class)
  public static class DefaultAccessControlConfiguration {

    private static int calc(int old, boolean condition, int mask) {
      return condition ? old | mask : old & ~mask;
    }

    @Autowired
    private CKFinderProperties properties;

    @Bean
    public AccessControl defaultAccessControl() {
      AccessControl.Builder accessControlBuilder = AccessControl.builder();
      for (CKFinderProperties.AccessControl accessControl : properties.getAccessControls()) {
        String role = accessControl.getRole();
        String resourceType = accessControl.getResourceType();
        String folder = accessControl.getFolder();
        int mask = 0;
        mask = calc(mask, accessControl.isFileDelete(), AccessControl.CKFINDER_CONNECTOR_ACL_FILE_DELETE);
        mask = calc(mask, accessControl.isFileRename(), AccessControl.CKFINDER_CONNECTOR_ACL_FILE_RENAME);
        mask = calc(mask, accessControl.isFileUpload(), AccessControl.CKFINDER_CONNECTOR_ACL_FILE_UPLOAD);
        mask = calc(mask, accessControl.isFileView(), AccessControl.CKFINDER_CONNECTOR_ACL_FILE_VIEW);
        mask = calc(mask, accessControl.isFolderCreate(), AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_CREATE);
        mask = calc(mask, accessControl.isFolderDelete(), AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_DELETE);
        mask = calc(mask, accessControl.isFolderRename(), AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_RENAME);
        mask = calc(mask, accessControl.isFolderView(), AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_VIEW);

        AccessControlLevel accessControlLevel = AccessControlLevel
                .builder().role(role).resourceType(resourceType).folder(folder)
                .mask(mask).build();
        accessControlBuilder.aclEntry(accessControlLevel);
      }
      return accessControlBuilder.build();
    }

  }

  @Configuration
  @ConditionalOnMissingBean(IConfiguration.class)
  public static class DefaultConfigurationConfiguration {

    @Autowired
    private CKFinderProperties properties;
    @Autowired
    private IBasePathBuilder defaultPathBuilder;
    @Autowired
    private AccessControl defaultAccessControl;
    @Autowired(required = false)
    private final Collection<Plugin> plugins = Collections.emptyList();

    @Bean
    public IConfiguration configuration() throws IOException {
      com.github.zhanhb.ckfinder.connector.configuration.Configuration.Builder builder = com.github.zhanhb.ckfinder.connector.configuration.Configuration.builder();
      if (properties.getEnabled() != null) {
        builder.enabled(properties.getEnabled());
      }
      if (properties.getLicenseKey() != null) {
        builder.licenseKey(properties.getLicenseKey());
      }
      if (properties.getLicenseName() != null) {
        builder.licenseName(properties.getLicenseName());
      }
      if (properties.getImgWidth() != null) {
        builder.imgWidth(properties.getImgWidth());
      }
      if (properties.getImgHeight() != null) {
        builder.imgHeight(properties.getImgHeight());
      }
      if (properties.getImgQuality() != null) {
        builder.imgQuality(properties.getImgQuality());
      }
      if (properties.getDefaultResourceTypes() != null) {
        builder.defaultResourceTypes(Arrays.asList(properties.getDefaultResourceTypes()));
      }
      if (properties.getTypes() != null) {
        setTypes(builder);
      }
      if (properties.getUserRoleSessionVar() != null) {
        builder.userRoleName(properties.getUserRoleSessionVar());
      }
      builder.accessControl(defaultAccessControl);
      setThumbs(builder);
      if (properties.getDisallowUnsafeCharacters() != null) {
        builder.disallowUnsafeCharacters(properties.getDisallowUnsafeCharacters());
      }
      if (properties.getCheckDoubleExtension() != null) {
        builder.checkDoubleFileExtensions(properties.getCheckDoubleExtension());
      }
      if (properties.getCheckSizeAfterScaling() != null) {
        builder.checkSizeAfterScaling(properties.getCheckSizeAfterScaling());
      }
      if (properties.getSecureImageUploads() != null) {
        builder.secureImageUploads(properties.getSecureImageUploads());
      }
      if (properties.getHtmlExtensions() != null) {
        builder.htmlExtensions(Arrays.asList(properties.getHtmlExtensions()));
      }
      if (properties.getForceAscii() != null) {
        builder.forceAscii(properties.getForceAscii());
      }
      if (properties.getEnableCsrfProtection() != null) {
        builder.enableCsrfProtection(properties.getEnableCsrfProtection());
      }
      if (properties.getHideFolders() != null) {
        builder.hiddenFolders(Arrays.asList(properties.getHideFolders()));
      }
      if (properties.getHideFiles() != null) {
        builder.hiddenFiles(Arrays.asList(properties.getHideFiles()));
      }
      builder.eventsFromPlugins(plugins);
      return builder.build();
    }

    private void setTypes(com.github.zhanhb.ckfinder.connector.configuration.Configuration.Builder builder) throws IOException {
      String baseDir = defaultPathBuilder.getBaseDir();
      String baseUrl = defaultPathBuilder.getBaseUrl();
      for (CKFinderProperties.Type type : properties.getTypes()) {
        ResourceType.Builder resourceTypeBuilder = ResourceType.builder();
        final String typeName = type.getName();
        Assert.notNull(typeName, "Resource type name should not be null");
        resourceTypeBuilder.name(typeName);

        if (type.getAllowedExtensions() != null) {
          resourceTypeBuilder.allowedExtensions(type.getAllowedExtensions());
        }
        if (type.getDeniedExtensions() != null) {
          resourceTypeBuilder.deniedExtensions(type.getDeniedExtensions());
        }
        if (type.getMaxSize() != null) {
          resourceTypeBuilder.maxSize(type.getMaxSize());
        }
        String path = !StringUtils.isEmpty(type.getDirectory())
                ? type.getDirectory()
                : Constants.BASE_DIR_PLACEHOLDER + "/" + typeName.toLowerCase() + "/";
        resourceTypeBuilder.path(Files.createDirectories(
                Paths.get(path
                        .replace(Constants.BASE_DIR_PLACEHOLDER, baseDir)))
                .toAbsolutePath().toString());
        String url = (type.getUrl() != null) ? type.getUrl() : Constants.BASE_URL_PLACEHOLDER + "/" + typeName.toLowerCase() + "/";
        resourceTypeBuilder.url(url.replace(Constants.BASE_URL_PLACEHOLDER, baseUrl));

        ResourceType resourceType = resourceTypeBuilder.build();
        builder.type(typeName, resourceType);
      }
    }

    private void setThumbs(com.github.zhanhb.ckfinder.connector.configuration.Configuration.Builder builder) {
      CKFinderProperties.Thumbs thumbs = properties.getThumbs();
      if (thumbs != null) {
        String baseDir = defaultPathBuilder.getBaseDir();
        String baseUrl = defaultPathBuilder.getBaseUrl();
        if (thumbs.getEnabled() != null) {
          builder.thumbsEnabled(thumbs.getEnabled());
        }
        if (thumbs.getDirectory() != null) {
          String path = thumbs.getDirectory().replace(Constants.BASE_DIR_PLACEHOLDER, baseDir);
          builder.thumbsDir(path);
          builder.thumbsPath(path);
        }
        if (thumbs.getDirectAccess() != null) {
          builder.thumbsDirectAccess(thumbs.getDirectAccess());
        }
        if (thumbs.getUrl() != null) {
          builder.thumbsUrl(thumbs.getUrl().replace(Constants.BASE_URL_PLACEHOLDER, baseUrl));
        }
        if (thumbs.getMaxHeight() != null) {
          builder.maxThumbHeight(thumbs.getMaxHeight());
        }
        if (thumbs.getMaxWidth() != null) {
          builder.maxThumbWidth(thumbs.getMaxWidth());
        }
        if (thumbs.getQuality() != null) {
          builder.imgQuality(thumbs.getQuality());
        }
      }
    }

  }

  @Configuration
  @ConditionalOnMissingBean(FileEditor.class)
  @ConditionalOnProperty(prefix = CKFinderProperties.CKFINDER_PREFIX + ".file-editor", name = "enabled", havingValue = "true", matchIfMissing = true)
  public static class DefaultFileEditorConfiguration {

    @Bean
    public FileEditor fileEditor() {
      return new FileEditor();
    }

  }

  @Configuration
  @ConditionalOnMissingBean(ImageResize.class)
  @ConditionalOnProperty(prefix = CKFinderProperties.CKFINDER_PREFIX + ".image-resize", name = "enabled", havingValue = "true", matchIfMissing = true)
  public static class DefaultImageResizeConfiguration {

    @Autowired
    private CKFinderProperties properties;

    @Bean
    public ImageResize imageResize() {
      CKFinderProperties.ImageResize imageResize = properties.getImageResize();
      Properties params = imageResize.getParams();
      PluginInfo.Builder pluginInfoBuilder = PluginInfo.builder();
      if (params != null && !params.isEmpty()) {
        for (Map.Entry<Object, Object> entry : params.entrySet()) {
          String key = (String) entry.getKey();
          String value = (String) entry.getValue();
          pluginInfoBuilder.param(key, value);
        }
      }
      return new ImageResize(pluginInfoBuilder.build().getParams());
    }

  }

  @Configuration
  @ConditionalOnMissingBean(Watermark.class)
  @ConditionalOnProperty(prefix = CKFinderProperties.CKFINDER_PREFIX + ".watermark", name = "enabled", havingValue = "true", matchIfMissing = false)
  public static class DefaultWatermarkConfiguration {

    @Autowired
    private CKFinderProperties properties;
    @Autowired
    private ResourceLoader resourceLoader;

    @Bean
    public Watermark watermark(WatermarkSettings watermarkSettings) {
      CKFinderProperties.Watermark watermark = properties.getWatermark();
      WatermarkSettings.Builder builder = WatermarkSettings.builder();
      if (watermark.getMarginBottom() != null) {
        builder.marginBottom(watermark.getMarginBottom());
      }
      if (watermark.getMarginRight() != null) {
        builder.marginRight(watermark.getMarginRight());
      }
      if (watermark.getQuality() != null) {
        builder.quality(watermark.getQuality());
      }
      if (watermark.getSource() != null) {
        builder.source(resourceLoader.getResource(watermark.getSource()));
      }
      if (watermark.getTransparency() != null) {
        builder.transparency(watermark.getTransparency());
      }
      return new Watermark(builder.build());
    }
  }

}
