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

import com.github.zhanhb.ckfinder.connector.configuration.DefaultPathBuilder;
import com.github.zhanhb.ckfinder.connector.configuration.IBasePathBuilder;
import com.github.zhanhb.ckfinder.connector.configuration.IConfiguration;
import com.github.zhanhb.ckfinder.connector.configuration.XmlConfigurationParser;
import javax.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 *
 * @author zhanhb
 */
@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(CKFinderProperties.class)
@SuppressWarnings("PublicInnerClass")
public class CKFinderAutoConfiguration {

  @ConditionalOnMissingBean(IBasePathBuilder.class)
  public static class DefaultBasePathBuilderConfiguration {

    @Autowired
    private CKFinderProperties properties;

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public DefaultPathBuilder pathBuilder() {
      ServletContext servletContext = applicationContext.getBean(ServletContext.class);
      String baseDir = servletContext.getRealPath(IConfiguration.DEFAULT_BASE_URL);
      return DefaultPathBuilder.builder()
              .baseDir(baseDir)
              .baseUrl(IConfiguration.DEFAULT_BASE_URL)
              .build();
    }
  }

  @ConditionalOnMissingBean(IConfiguration.class)
  public static class DefaultConfigurationConfiguration {

    @Autowired
    private CKFinderProperties properties;

    @Bean
    public IConfiguration configuration(ResourceLoader resourceLoader, IBasePathBuilder basePathBuilder) throws Exception {
      return XmlConfigurationParser.INSTANCE.parse(resourceLoader, basePathBuilder, "/WEB-INF/config.xml");
    }

  }

}
