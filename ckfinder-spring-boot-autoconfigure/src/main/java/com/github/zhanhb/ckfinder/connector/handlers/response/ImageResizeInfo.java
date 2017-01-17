package com.github.zhanhb.ckfinder.connector.handlers.response;

import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Singular;

/**
 *
 * @see com.github.zhanhb.ckfinder.connector.plugins.ImageResize
 * @author zhanhb
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "imageresize")
public class ImageResizeInfo {

  @Singular
  @XmlAnyAttribute
  private Map<QName, String> attributes;

  @SuppressWarnings("PublicInnerClass")
  public static class Builder {

    public Builder attr(String key, String value) {
      return attribute(QName.valueOf(key), value);
    }

  }

}
