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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;

class AssetsDirectory {
  
  private static final boolean USE_CACHE = true;
  
  private final Map<String, Entry> cache;
  
  private final Path directory;
  
  private final String uriPath;
  
  private final String defaultFile;
  
  public AssetsDirectory(Path directory, String uriPath, String defaultFile) {
    this (directory, uriPath, defaultFile, 1024);
  }
  
  public AssetsDirectory(Path directory, String uriPath, String defaultFile, final int maxSize) {
    this.directory = directory;
    this.uriPath = uriPath;
    this.defaultFile = defaultFile;
    
    this.cache = new LinkedHashMap<String, Entry>() {
      
      private static final long serialVersionUID = 1L;

      @Override
      protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
        return size() >= maxSize;
      }
    };
  }
  
  /**
   * Returns an {@link Entry} or throws a {@link FileNotFoundException}.
   */
  public Entry getFileEntry(String requestURI) throws FileNotFoundException, IOException {
    Entry asset = lookup(requestURI);
    if (asset != null) {
      return asset;
    }
    
    return load(requestURI);
  }
  
  /**
   * Lookups an {@link Entry} and returns it.
   */
  private Entry lookup(String requestURI) throws IOException {
    if (!USE_CACHE) {
      return null;
    }
    
    synchronized (cache) {
      Entry entry = cache.get(requestURI);
      
      if (entry == null) {
        return null;
      }
      
      if (entry.isExpired()) {
        cache.remove(requestURI);
        return null;
      }
      
      return entry;
    }
  }
  
  /**
   * Loads an {@link Entry} from the underlying filesystem.
   */
  private Entry load(String requestURI) throws IOException {
    Path file = getFilePath(requestURI);
    
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      try (DigestOutputStream dos = new DigestOutputStream(NopOutputStream.NULL, md)) {
        Files.copy(file, dos);
      }
      
      FileTime lastModified = Files.getLastModifiedTime(file);
      String etag = "\"" + Hex.encodeHexString(md.digest()) + "\"";
      Entry entry = new Entry(file, lastModified, etag);
      
      if (USE_CACHE) {
        synchronized (cache) {
          cache.put(requestURI, entry);
        }
      }
      
      return entry;
    } catch (NoSuchAlgorithmException err) {
      throw new IOException("NoSuchAlgorithmException", err);
    }
  }
  
  private Path getFilePath(String requestURI) throws IOException {
    // The 'requestURI' is not in the 'uriPath'
    if (!requestURI.startsWith(uriPath)) {
      throw new IOException(requestURI);
    }
    
    String requestPath = requestURI.substring(uriPath.length());
    Path file = directory.resolve(requestPath);
    
    if (Files.isDirectory(file) && defaultFile != null) {
      file = file.resolve(defaultFile);
    }
    
    // The 'requestURI' points to a directory and there 
    // is no default file.
    if (!Files.exists(file)) {
      throw new FileNotFoundException(requestURI);
    }
    
    return file;
  }
  
  /**
   * An entry in the {@link AssetsDirectory}.
   */
  public static class Entry {
    
    private final Path file;
    
    private final FileTime lastModified;
    
    private final String etag;

    private Entry(Path file, FileTime lastModified, String etag) {
      this.file = file;
      this.lastModified = lastModified;
      this.etag= etag;
    }
    
    /**
     * Returns {@code true} if the {@link Entry} has changed.
     */
    public boolean isExpired() throws IOException {
      return !lastModified.equals(Files.getLastModifiedTime(file));
    }
    
    /**
     * Returns the underlying file.
     */
    public Path getFile() {
      return file;
    }
    
    /**
     * Returns the file's size in bytes.
     */
    public long length() throws IOException {
      return Files.size(file);
    }
    
    /**
     * Returns the file's last modified time.
     */
    public long getLastModified() {
      return lastModified.toMillis();
    }
    
    /**
     * Returns the file's ETag.
     */
    public String getETag() {
      return etag;
    }
    
    public void writeTo(OutputStream out) throws IOException {
      Files.copy(file, out);
    }
  }
  
  /**
   * An {@link OutputStream} that does nothing. 
   */
  private static class NopOutputStream extends OutputStream {

    public static final NopOutputStream NULL = new NopOutputStream();
    
    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void write(byte[] value, int offset, int length) {
    }

    @Override
    public void write(byte[] value) {
    }

    @Override
    public void write(int value) {
    }
  }
}