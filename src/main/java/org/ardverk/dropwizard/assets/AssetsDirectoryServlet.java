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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.Buffer;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.yammer.dropwizard.assets.AssetServlet;

/**
 * @see AssetServlet
 */
class AssetsDirectoryServlet extends HttpServlet {
  
  private static final long serialVersionUID = -6282260875091451501L;

  private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.HTML_UTF_8;
  
  private final AssetsDirectory directory;
  
  private final MimeTypes mimeTypes;
  
  private final Charset charset;
  
  public AssetsDirectoryServlet(AssetsDirectory directory) {
    this(directory, new MimeTypes(), Charsets.UTF_8);
  }
  
  public AssetsDirectoryServlet(AssetsDirectory directory, MimeTypes mimeTypes, Charset charset) {
    this.directory = directory;
    this.mimeTypes = mimeTypes;
    this.charset = charset;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String requestURI = request.getRequestURI();
    
    try {
      AssetsDirectory.Entry entry = directory.getFileEntry(requestURI);
      
      if (isCurrent(request, entry)) {
        response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        return;
      }
      
      response.setDateHeader(HttpHeaders.LAST_MODIFIED, entry.getLastModified());
      response.setHeader(HttpHeaders.ETAG, entry.getETag());
      
      MediaType mediaType = DEFAULT_MEDIA_TYPE;

      Buffer mimeType = mimeTypes.getMimeByExtension(requestURI);
      if (mimeType != null) {
        try {
          mediaType = MediaType.parse(mimeType.toString());
          if (charset != null && mediaType.is(MediaType.ANY_TEXT_TYPE)) {
              mediaType = mediaType.withCharset(charset);
          }
        } catch (IllegalArgumentException ignore) {}
      }

      response.setContentType(mediaType.type() + "/" + mediaType.subtype());

      if (mediaType.charset().isPresent()) {
        response.setCharacterEncoding(mediaType.charset().get().toString());
      }
      
      long contentLength = entry.length();
      if (contentLength >= 0L && contentLength < Integer.MAX_VALUE) {
        response.setContentLength((int)contentLength);
      }
      
      OutputStream out = response.getOutputStream();
      entry.writeTo(out);
      out.flush();
      
    } catch (FileNotFoundException err) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }
  
  /**
   * Returns {@code true} if the client has an up-to-date version of the asset.
   */
  private static boolean isCurrent(HttpServletRequest request, AssetsDirectory.Entry entry) {
    String etag = entry.getETag();
    long lastModified = entry.getLastModified();
    
    return etag.equals(request.getHeader(HttpHeaders.IF_NONE_MATCH)) 
        || (request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE) >= lastModified);
  }
}
