package com.github.zhanhb.ckfinder.connector.handlers.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
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
@XmlRootElement(name = "ResourceType")
public class ResourceType {

  @XmlAttribute(name = "acl")
  private int acl;
  @XmlAttribute(name = "allowedExtensions")
  private String allowedExtensions;
  @XmlAttribute(name = "deniedExtensions")
  private String deniedExtensions;
  @XmlAttribute(name = "hasChildren")
  private boolean hasChildren;
  @XmlAttribute(name = "hash")
  private String hash;
  @XmlAttribute(name = "maxSize")
  private long maxSize;
  @XmlAttribute(name = "name")
  private String name;
  @XmlAttribute(name = "url")
  private String url;

}
