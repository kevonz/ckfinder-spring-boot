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

import com.github.zhanhb.ckfinder.connector.configuration.IConfiguration;
import com.google.gson.GsonBuilder;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author zhanhb
 */
@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(CKFinderProperties.class)
@SuppressWarnings("PublicInnerClass")
public class CKFinderAutoConfiguration {

  @Autowired
  private CKFinderProperties properties;

  @PostConstruct
  public void init() {
    System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(properties));
  }

  @ConditionalOnMissingBean(IConfiguration.class)
  public static class DefaultConfigurationConfiguration {

    @Autowired
    private CKFinderProperties properties;

    @PostConstruct
    public void init() {
      System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(properties));
    }

    @Bean
    public com.github.zhanhb.ckfinder.connector.configuration.Configuration configuration(ApplicationContext context) throws Exception {
      return new com.github.zhanhb.ckfinder.connector.configuration.Configuration(context, "/tt");
    }

  }

}
