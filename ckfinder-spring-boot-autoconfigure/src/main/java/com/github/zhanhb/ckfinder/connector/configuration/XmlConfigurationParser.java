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
package com.github.zhanhb.ckfinder.connector.configuration;

import com.github.zhanhb.ckfinder.connector.data.AccessControlLevel;
import com.github.zhanhb.ckfinder.connector.data.PluginInfo;
import com.github.zhanhb.ckfinder.connector.data.ResourceType;
import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
import com.github.zhanhb.ckfinder.connector.plugins.FileEditor;
import com.github.zhanhb.ckfinder.connector.plugins.ImageResize;
import com.github.zhanhb.ckfinder.connector.plugins.Watermark;
import com.github.zhanhb.ckfinder.connector.plugins.WatermarkSettings;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.github.zhanhb.ckfinder.connector.utils.PathUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static com.github.zhanhb.ckfinder.connector.configuration.IConfiguration.DEFAULT_IMG_HEIGHT;
import static com.github.zhanhb.ckfinder.connector.configuration.IConfiguration.DEFAULT_IMG_QUALITY;
import static com.github.zhanhb.ckfinder.connector.configuration.IConfiguration.DEFAULT_IMG_WIDTH;
import static com.github.zhanhb.ckfinder.connector.configuration.IConfiguration.DEFAULT_THUMB_MAX_WIDTH;
import static com.github.zhanhb.ckfinder.connector.plugins.WatermarkSettings.MARGIN_BOTTOM;
import static com.github.zhanhb.ckfinder.connector.plugins.WatermarkSettings.MARGIN_RIGHT;
import static com.github.zhanhb.ckfinder.connector.plugins.WatermarkSettings.QUALITY;
import static com.github.zhanhb.ckfinder.connector.plugins.WatermarkSettings.SOURCE;
import static com.github.zhanhb.ckfinder.connector.plugins.WatermarkSettings.TRANSPARENCY;

/**
 * Class loads configuration from XML file.
 *
 * @author zhanhb
 */
@Slf4j
public enum XmlConfigurationParser {
  INSTANCE;

  /**
   * bytes in KB.
   */
  private static final int BYTES = 1024;

  private static final int MAX_QUALITY = 100;
  private static final float MAX_QUALITY_FLOAT = 100f;

  /**
   *
   * @param resourceLoader
   * @param basePathBuilder
   * @param xmlFilePath
   * @return
   * @throws java.lang.Exception
   */
  public Configuration parse(ResourceLoader resourceLoader,
          IBasePathBuilder basePathBuilder, String xmlFilePath)
          throws Exception {
    Configuration.Builder builder = Configuration.builder();
    String baseFolder = getBaseFolder(basePathBuilder);
    init(builder, resourceLoader, xmlFilePath, baseFolder, basePathBuilder);
    return builder.build();
  }

  /**
   * Initializes configuration from XML file.
   *
   * @throws Exception when error occurs.
   */
  private void init(Configuration.Builder builder, ResourceLoader resourceLoader,
          String xmlFilePath, String baseFolder, IBasePathBuilder basePathBuilder)
          throws ConnectorException, IOException, ParserConfigurationException, SAXException {
    Resource resource = getFullConfigPath(resourceLoader, xmlFilePath);
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc;
    try (InputStream stream = resource.getInputStream()) {
      doc = db.parse(stream);
    }
    doc.normalize();
    Node node = doc.getFirstChild();
    if (node != null) {
      NodeList nodeList = node.getChildNodes();
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node childNode = nodeList.item(i);
        switch (childNode.getNodeName()) {
          case "enabled":
            builder.enabled(Boolean.parseBoolean(nullNodeToString(childNode)));
            break;
          case "licenseName":
            builder.licenseName(nullNodeToString(childNode));
            break;
          case "licenseKey":
            builder.licenseKey(nullNodeToString(childNode));
            break;
          case "imgWidth":
            String width = nullNodeToString(childNode);
            width = width.replaceAll("\\D", "");
            try {
              builder.imgWidth(Integer.parseInt(width));
            } catch (NumberFormatException e) {
              builder.imgWidth(DEFAULT_IMG_WIDTH);
            }
            break;
          case "imgQuality":
            String quality = nullNodeToString(childNode);
            quality = quality.replaceAll("\\D", "");
            builder.imgQuality(adjustQuality(quality));
            break;
          case "imgHeight":
            String height = nullNodeToString(childNode);
            height = height.replaceAll("\\D", "");
            try {
              builder.imgHeight(Integer.parseInt(height));
            } catch (NumberFormatException e) {
              builder.imgHeight(DEFAULT_IMG_HEIGHT);
            }
            break;
          case "thumbs":
            setThumbs(builder, childNode.getChildNodes(), baseFolder, basePathBuilder);
            break;
          case "accessControls":
            setACLs(builder, childNode.getChildNodes());
            break;
          case "hideFolders":
            setHiddenFolders(builder, childNode.getChildNodes());
            break;
          case "hideFiles":
            setHiddenFiles(builder, childNode.getChildNodes());
            break;
          case "checkDoubleExtension":
            builder.checkDoubleFileExtensions(Boolean.parseBoolean(nullNodeToString(childNode)));
            break;
          case "disallowUnsafeCharacters":
            builder.disallowUnsafeCharacters(Boolean.parseBoolean(nullNodeToString(childNode)));
            break;
          case "forceASCII":
            builder.forceAscii(Boolean.parseBoolean(nullNodeToString(childNode)));
            break;
          case "checkSizeAfterScaling":
            builder.checkSizeAfterScaling(Boolean.parseBoolean(nullNodeToString(childNode)));
            break;
          case "htmlExtensions":
            String htmlExt = nullNodeToString(childNode);
            StringTokenizer scanner = new StringTokenizer(htmlExt, ",");
            while (scanner.hasMoreTokens()) {
              String val = scanner.nextToken();
              if (val != null && !val.isEmpty()) {
                builder.htmlExtension(val.trim().toLowerCase());
              }
            }
            break;
          case "secureImageUploads":
            builder.secureImageUploads(Boolean.parseBoolean(nullNodeToString(childNode)));
            break;
          case "uriEncoding":
            break;
          case "userRoleSessionVar":
            builder.userRoleName(nullNodeToString(childNode));
            break;
          case "defaultResourceTypes":
            String value = nullNodeToString(childNode);
            StringTokenizer sc = new StringTokenizer(value, ",");
            while (sc.hasMoreTokens()) {
              builder.defaultResourceType(sc.nextToken());
            }
            break;
          case "plugins":
            setPlugins(builder, childNode, resourceLoader);
            break;
          case "enableCsrfProtection":
            builder.enableCsrfProtection(Boolean.parseBoolean(nullNodeToString(childNode)));
            break;
          default:
            break;
        }
      }
    }
    setTypes(builder, doc, basePathBuilder);
  }

  /**
   * Returns XML node contents or empty String instead of null if XML node is
   * empty.
   */
  private String nullNodeToString(Node childNode) {
    String textContent = childNode.getTextContent();
    return textContent == null ? "" : textContent.trim();
  }

  /**
   * Gets absolute path to XML configuration file.
   *
   * @return absolute path to XML configuration file
   * @throws ConnectorException when absolute path cannot be obtained.
   */
  private Resource getFullConfigPath(ResourceLoader resourceLoader, String xmlFilePath) throws ConnectorException {
    Resource resource = resourceLoader.getResource(xmlFilePath);
    if (!resource.exists()) {
      throw new ConnectorException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND,
              "Configuration file could not be found under specified location.");
    }
    return resource;
  }

  /**
   * Adjusts image quality.
   *
   * @param imgQuality Image quality
   * @return Adjusted image quality
   */
  private float adjustQuality(String imgQuality) {
    float helper;
    try {
      helper = Math.abs(Float.parseFloat(imgQuality));
    } catch (NumberFormatException e) {
      return DEFAULT_IMG_QUALITY;
    }
    if (helper == 0 || helper == 1) {
      return helper;
    } else if (helper > 0 && helper < 1) {
      helper = (Math.round(helper * MAX_QUALITY_FLOAT) / MAX_QUALITY_FLOAT);
    } else if (helper > 1 && helper <= MAX_QUALITY) {
      helper = (Math.round(helper) / MAX_QUALITY_FLOAT);
    } else {
      helper = DEFAULT_IMG_QUALITY;
    }
    return helper;
  }

  /**
   * Sets hidden files list defined in XML configuration.
   *
   * @param childNodes list of files nodes.
   */
  private void setHiddenFiles(Configuration.Builder builder, NodeList childNodes) {
    for (int i = 0, j = childNodes.getLength(); i < j; i++) {
      Node node = childNodes.item(i);
      if (node.getNodeName().equals("file")) {
        String val = nullNodeToString(node);
        if (!val.isEmpty()) {
          builder.hiddenFile(val.trim());
        }
      }
    }
  }

  /**
   * Sets hidden folders list defined in XML configuration.
   *
   * @param childNodes list of folder nodes.
   */
  private void setHiddenFolders(Configuration.Builder builder, NodeList childNodes) {
    for (int i = 0, j = childNodes.getLength(); i < j; i++) {
      Node node = childNodes.item(i);
      if (node.getNodeName().equals("folder")) {
        String val = nullNodeToString(node);
        if (!val.isEmpty()) {
          builder.hiddenFolder(val.trim());
        }
      }
    }
  }

  /**
   * Sets ACL configuration as a list of access control levels.
   *
   * @param childNodes nodes with ACL configuration.
   */
  private void setACLs(Configuration.Builder builder, NodeList childNodes) {
    AccessControl.Builder accessControlBuilder = AccessControl.builder();
    for (int i = 0, j = childNodes.getLength(); i < j; i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals("accessControl")) {
        AccessControlLevel acl = getACLFromNode(childNode);
        if (acl != null) {
          accessControlBuilder.aclEntry(acl);
        }
      }
    }
    builder.accessControl(accessControlBuilder.build());
  }

  /**
   * Gets single ACL configuration from XML node.
   *
   * @param childNode XML accessControl node.
   * @return access control level object.
   */
  private AccessControlLevel getACLFromNode(Node childNode) {
    String role = null;
    String resourceType = null;
    String folder = null;
    int mask = 0;
    for (int i = 0, j = childNode.getChildNodes().getLength(); i < j; i++) {
      Node childChildNode = childNode.getChildNodes().item(i);
      String nodeName = childChildNode.getNodeName();
      int index = 0;
      boolean bool = false;
      switch (nodeName) {
        case "role":
          role = nullNodeToString(childChildNode);
          break;
        case "resourceType":
          resourceType = nullNodeToString(childChildNode);
          break;
        case "folder":
          folder = nullNodeToString(childChildNode);
          break;
        case "folderView":
          bool = Boolean.parseBoolean(nullNodeToString(childChildNode));
          index = AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_VIEW;
          break;
        case "folderCreate":
          bool = Boolean.parseBoolean(nullNodeToString(childChildNode));
          index = AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_CREATE;
          break;
        case "folderRename":
          bool = Boolean.parseBoolean(nullNodeToString(childChildNode));
          index = AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_RENAME;
          break;
        case "folderDelete":
          bool = Boolean.parseBoolean(nullNodeToString(childChildNode));
          index = AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_DELETE;
          break;
        case "fileView":
          bool = Boolean.parseBoolean(nullNodeToString(childChildNode));
          index = AccessControl.CKFINDER_CONNECTOR_ACL_FILE_VIEW;
          break;
        case "fileUpload":
          bool = Boolean.parseBoolean(nullNodeToString(childChildNode));
          index = AccessControl.CKFINDER_CONNECTOR_ACL_FILE_UPLOAD;
          break;
        case "fileRename":
          bool = Boolean.parseBoolean(nullNodeToString(childChildNode));
          index = AccessControl.CKFINDER_CONNECTOR_ACL_FILE_RENAME;
          break;
        case "fileDelete":
          bool = Boolean.parseBoolean(nullNodeToString(childChildNode));
          index = AccessControl.CKFINDER_CONNECTOR_ACL_FILE_DELETE;
          break;
      }
      if (index != 0) {
        if (bool) {
          mask |= index;
        } else {
          mask &= ~index;
        }
      }
    }

    if (resourceType == null || role == null) {
      return null;
    }

    if (folder == null || folder.isEmpty()) {
      folder = "/";
    }
    return AccessControlLevel.builder()
            .folder(folder)
            .resourceType(resourceType)
            .role(role)
            .mask(mask)
            .build();
  }

  /**
   * creates thumb configuration from XML.
   *
   * @param childNodes list of thumb XML nodes
   */
  private void setThumbs(Configuration.Builder builder, NodeList childNodes, String baseFolder, IBasePathBuilder basePathBuilder) throws ConnectorException, IOException {
    for (int i = 0, j = childNodes.getLength(); i < j; i++) {
      Node childNode = childNodes.item(i);
      switch (childNode.getNodeName()) {
        case "enabled":
          builder.thumbsEnabled(Boolean.parseBoolean(nullNodeToString(childNode)));
          break;
        case "url":
          builder.thumbsUrl(PathUtils.escape(nullNodeToString(childNode).replace(Constants.BASE_URL_PLACEHOLDER,
                  basePathBuilder.getBaseUrl())));
          break;
        case "directory":
          String thumbsDir = nullNodeToString(childNode);
          Path file = Paths.get(thumbsDir.replace(Constants.BASE_DIR_PLACEHOLDER, baseFolder));
          if (file == null) {
            throw new ConnectorException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND,
                    "Thumbs directory could not be created using specified path.");
          }
          log.debug("{}", file);
          Files.createDirectories(file);
          builder.thumbsPath(file.toAbsolutePath().toString());

          break;
        case "directAccess":
          builder.thumbsDirectAccess(Boolean.parseBoolean(nullNodeToString(childNode)));
          break;
        case "maxHeight":
          String width = nullNodeToString(childNode);
          width = width.replaceAll("\\D", "");
          try {
            builder.maxThumbHeight(Integer.valueOf(width));
          } catch (NumberFormatException e) {
            builder.maxThumbHeight(DEFAULT_THUMB_MAX_WIDTH);
          }
          break;
        case "maxWidth":
          width = nullNodeToString(childNode);
          width = width.replaceAll("\\D", "");
          try {
            builder.maxThumbWidth(Integer.valueOf(width));
          } catch (NumberFormatException e) {
            builder.maxThumbWidth(DEFAULT_IMG_WIDTH);
          }
          break;
        case "quality":
          String quality = nullNodeToString(childNode);
          quality = quality.replaceAll("\\D", "");
          builder.thumbsQuality(adjustQuality(quality));
      }
    }
  }

  /**
   * Creates resource types configuration from XML configuration file (from XML
   * element 'types').
   *
   * @param doc XML document.
   */
  private void setTypes(Configuration.Builder builder, Document doc, IBasePathBuilder basePathBuilder)
          throws IOException, ConnectorException {
    NodeList list = doc.getElementsByTagName("type");

    for (int i = 0, j = list.getLength(); i < j; i++) {
      Element element = (Element) list.item(i);
      String name = element.getAttribute("name");
      if (name != null && !name.isEmpty()) {
        ResourceType resourceType = createTypeFromXml(name, element.getChildNodes(), basePathBuilder);
        builder.type(name, resourceType);
      }
    }
  }

  /**
   * Creates single resource type configuration from XML configuration file
   * (from XML element 'type').
   *
   * @param typeName name of type.
   * @param childNodes type XML child nodes.
   * @return resource type
   */
  private ResourceType createTypeFromXml(String typeName,
          NodeList childNodes, IBasePathBuilder basePathBuilder) throws IOException, ConnectorException {
    ResourceType.Builder builder = ResourceType.builder().name(typeName);
    String path = Constants.BASE_DIR_PLACEHOLDER + "/" + typeName.toLowerCase() + "/";
    String url = Constants.BASE_URL_PLACEHOLDER + "/" + typeName.toLowerCase() + "/";

    for (int i = 0, j = childNodes.getLength(); i < j; i++) {
      Node childNode = childNodes.item(i);
      switch (childNode.getNodeName()) {
        case "url":
          url = nullNodeToString(childNode);
          break;
        case "directory":
          path = nullNodeToString(childNode);
          break;
        case "maxSize":
          long maxSize = 0;
          try {
            parseMaxSize(nullNodeToString(childNode));
          } catch (NumberFormatException ex) {
          }
          builder.maxSize(maxSize);
          break;
        case "allowedExtensions":
          builder.allowedExtensions(nullNodeToString(childNode));
          break;
        case "deniedExtensions":
          builder.deniedExtensions(nullNodeToString(childNode));
      }
    }
    url = url.replace(Constants.BASE_URL_PLACEHOLDER,
            basePathBuilder.getBaseUrl());
    url = PathUtils.escape(url);
    url = PathUtils.removeSlashFromEnd(url);

    path = path.replace(Constants.BASE_DIR_PLACEHOLDER, getBaseFolder(basePathBuilder));
    path = PathUtils.escape(path);
    path = PathUtils.removeSlashFromEnd(path);

    if (path == null || path.isEmpty()) {
      throw new IllegalStateException("baseFolder is empty");
    }
    Path p = Paths.get(path);
    if (p == null) {
      throw new ConnectorException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND,
              "Resource directory could not be created using specified path.");
    }

    FileUtils.createPath(p, false);

    return builder.url(url).path(p.toAbsolutePath().toString()).build();
  }

  /**
   * parses max size value from config (ex. 16M to number of bytes).
   *
   * @return number of bytes in max size.
   */
  private long parseMaxSize(String maxSize) {
    char lastChar = Character.toLowerCase(maxSize.charAt(maxSize.length() - 1));
    int a = 1, off = 1;
    switch (lastChar) {
      case 'k':
        a = BYTES;
        break;
      case 'm':
        a = BYTES * BYTES;
        break;
      case 'g':
        a = BYTES * BYTES * BYTES;
        break;
      default:
        off = 0;
        break;
    }
    long value = Long.parseLong(maxSize.substring(0, maxSize.length() - off));
    return value * a;
  }

  /**
   * Sets plugins list from XML configuration file.
   *
   * @param childNode child of XML node 'plugins'.
   */
  private void setPlugins(Configuration.Builder builder, Node childNode, ResourceLoader resourceLoader) {
    NodeList nodeList = childNode.getChildNodes();
    List<Plugin> plugins = new ArrayList<>(4);
    for (int i = 0, j = nodeList.getLength(); i < j; i++) {
      Node childChildNode = nodeList.item(i);
      if ("plugin".equals(childChildNode.getNodeName())) {
        PluginInfo pluginInfo = createPluginFromNode(childChildNode);
        String name = pluginInfo.getName();
        if (name != null) {
          Plugin plugin;
          switch (name) {
            case "imageresize":
              plugin = new ImageResize(pluginInfo.getParams());
              break;
            case "watermark":
              WatermarkSettings watermarkSettings = checkPluginInfo(pluginInfo, resourceLoader);
              plugin = new Watermark(watermarkSettings);
              break;
            case "fileeditor":
              plugin = new FileEditor();
              break;
            default:
              continue;
          }
          plugins.add(plugin);
        }
      }
    }
    builder.eventsFromPlugins(plugins);
  }

  private WatermarkSettings checkPluginInfo(PluginInfo pluginInfo, ResourceLoader resourceLoader) {
    WatermarkSettings.Builder settings = WatermarkSettings.builder();
    for (Map.Entry<String, String> entry : pluginInfo.getParams().entrySet()) {
      final String name = entry.getKey();
      final String value = entry.getValue();
      switch (name) {
        case SOURCE:
          settings.source(resourceLoader.getResource(value));
          break;
        case TRANSPARENCY:
          settings.transparency(Float.parseFloat(value));
          break;
        case QUALITY:
          final int parseInt = Integer.parseInt(value);
          final int name1 = parseInt % 101;
          final float name2 = name1 / 100f;
          settings.quality(name2);
          break;
        case MARGIN_BOTTOM:
          settings.marginBottom(Integer.parseInt(value));
          break;
        case MARGIN_RIGHT:
          settings.marginRight(Integer.parseInt(value));
      }
    }
    return settings.build();
  }

  /**
   * Creates plugin data from configuration file.
   *
   * @param element XML plugin node.
   * @return PluginInfo data
   */
  private PluginInfo createPluginFromNode(Node element) {
    PluginInfo.Builder builder = PluginInfo.builder();
    NodeList list = element.getChildNodes();
    for (int i = 0, l = list.getLength(); i < l; i++) {
      Node childElem = list.item(i);
      String nodeName = childElem.getNodeName();
      String textContent = nullNodeToString(childElem);
      switch (nodeName) {
        case "name":
          builder.name(textContent);
          break;
        case "params":
          NodeList paramLlist = childElem.getChildNodes();
          for (int j = 0, m = paramLlist.getLength(); j < m; j++) {
            Node node = paramLlist.item(j);
            if ("param".equals(node.getNodeName())) {
              NamedNodeMap map = node.getAttributes();
              String name = null;
              String value = null;
              for (int k = 0, o = map.getLength(); k < o; k++) {
                Node item = map.item(k);
                String nodeName1 = item.getNodeName();
                if ("name".equals(nodeName1)) {
                  name = nullNodeToString(item);
                } else if ("value".equals(nodeName1)) {
                  value = nullNodeToString(item);
                }
              }
              builder.param(name, value);
            }
          }
      }
    }
    return builder.build();
  }

  /**
   * Gets the path to base dir from configuration Crates the base dir folder if
   * it doesn't exists.
   *
   * @param basePathBuilder the basePathBuilder
   * @return path to base dir from conf
   * @throws IOException when error during creating folder occurs
   */
  private String getBaseFolder(IBasePathBuilder basePathBuilder) throws IOException {
    String baseFolder = basePathBuilder.getBaseDir();
    Path baseDir = Paths.get(baseFolder);
    if (!Files.exists(baseDir)) {
      FileUtils.createPath(baseDir, false);
    }
    return PathUtils.addSlashToEnd(baseFolder);
  }

}
