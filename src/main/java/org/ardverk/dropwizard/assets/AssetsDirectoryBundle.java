/*
 * Copyright 2013 Roger Kapsi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.ardverk.dropwizard.assets;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.dropwizard.Bundle;
import com.yammer.dropwizard.assets.AssetServlet;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

/**
 * A bundle for serving static asset files from a directory.
 * 
 * @see AssetsBundle
 */
public class AssetsDirectoryBundle implements Bundle {

  private static final Logger LOG = LoggerFactory.getLogger(AssetsDirectoryBundle.class);
  
  /**
   * Dropwizard's default location for assets. See {@link AssetsBundle} and {@link AssetServlet}.
   * 
   * NOTE: This is a relative path to the working directory.
   */
  public static final Path DEFAULT_ROOT_PATH = Paths.get("src/main/resources/assets");
  
  /**
   * Dropwizard's default URI path.
   */
  public static final String DEFAULT_URI_PATH = "/assets";
  
  /**
   * Dropwizard's default index file.
   */
  public static final String DEFAULT_FILE = "index.htm";
  
  private final Path rootPath;
  
  private final String uriPath;
  
  private final String defaultFile;
  
  public AssetsDirectoryBundle() {
    this(DEFAULT_ROOT_PATH, DEFAULT_URI_PATH, DEFAULT_FILE);
  }
  
  public AssetsDirectoryBundle(String uriPath) {
    this(DEFAULT_ROOT_PATH, uriPath, DEFAULT_FILE);
  }
  
  public AssetsDirectoryBundle(Path rootPath, String uriPath) {
    this(rootPath, uriPath, DEFAULT_FILE);
  }

  public AssetsDirectoryBundle(Path rootPath, String uriPath, String defaultFile) {
    this.rootPath = rootPath;
    this.uriPath = normalize(uriPath);
    this.defaultFile = defaultFile;
  }
  
  @Override
  public void initialize(Bootstrap<?> bootstrap) {
  }

  @Override
  public void run(Environment environment) {
    
    if (LOG.isInfoEnabled()) {
      LOG.info("Mapping {}* to {}", uriPath, rootPath);
    }
    
    AssetsDirectory directory = new AssetsDirectory(rootPath, uriPath, defaultFile);
    environment.addServlet(new AssetsDirectoryServlet(directory), uriPath + "*");
  }
  
  private static String normalize(String uriPath) {
    StringBuilder sb = new StringBuilder("/");
    
    String[] tokens = uriPath.split("/");
    
    for (String token : tokens) {
      if (!token.isEmpty()) {
        sb.append(token).append("/");
      }
    }
    
    return sb.toString();
  }
}
