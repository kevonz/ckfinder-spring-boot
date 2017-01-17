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
 * @see com.github.zhanhb.ckfinder.connector.handlers.command.InitCommand
 * @author zhanhb
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "ConnectorInfo")
public class ConnectorInfo implements ConnectorElement {

  @XmlAttribute(name = "c")
  private String licenseKey;
  @XmlAttribute(name = "csrfProtection")
  private boolean csrfProtection;
  @XmlAttribute(name = "enabled")
  private boolean enabled;
  @XmlAttribute(name = "imgHeight")
  private int imgHeight;
  @XmlAttribute(name = "imgWidth")
  private int imgWidth;
  @XmlAttribute(name = "plugins")
  private String plugins;
  @XmlAttribute(name = "s")
  private String licenseName;
  @XmlAttribute(name = "thumbsDirectAccess")
  private boolean thumbsDirectAccess;
  @XmlAttribute(name = "thumbsEnabled")
  private boolean thumbsEnabled;
  @XmlAttribute(name = "thumbsHeight")
  private Integer thumbsHeight;
  @XmlAttribute(name = "thumbsUrl")
  private String thumbsUrl;
  @XmlAttribute(name = "thumbsWidth")
  private Integer thumbsWidth;
  @XmlAttribute(name = "uploadCheckImages")
  private boolean uploadCheckImages;

}
