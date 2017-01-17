package com.github.zhanhb.ckfinder.connector.handlers.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 *
 * @author zhanhb
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "Connector")
public class Connector {

  @XmlAttribute(name = "resourceType")
  private String resourceType;

  @XmlElementRef
  private Error error;

  @XmlElementRef
  private ConnectorInfo connectorInfo;

  @XmlElementRef
  private ResourceTypes resourceTypes;

  @XmlElementRef
  private PluginsInfos pluginsInfos;

  @XmlElementRef
  private Errors errors;

  @XmlElementRef
  private CurrentFolder currentFolder;

  @XmlElementRef
  private DeleteFiles deleteFiles;

  @XmlElementRef
  private NewFolder newFolder;

  @XmlElementRef
  private CopyFiles copyFiles;

  @XmlElementRef
  private ImageInfo imageInfo;

  @XmlElementRef
  private Folders folders;

  @XmlElementRef
  private Files files;

  @XmlElementRef
  private RenamedFile renamedFile;

  @XmlElementRef
  private RenamedFolder renamedFolder;

  @XmlElementRef
  private MoveFiles moveFiles;

}
