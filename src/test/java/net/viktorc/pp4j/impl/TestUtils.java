/*
 * Copyright 2017 Viktor Csomor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.viktorc.pp4j.impl;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import net.viktorc.pp4j.api.ProcessManager;
import net.viktorc.pp4j.api.ProcessManagerFactory;

/**
 * A utility class for testing.
 *
 * @author Viktor Csomor
 */
class TestUtils {

  static final String TEST_TITLE_FORMAT = "%nTest %d%n-------------------------------------------------%n";

  private static final String EXECUTABLE_PATH;
  private static final String LIBRARY_PATH;
  static {
    String basePath = "net/viktorc/pp4j/impl/";
    String osName = System.getProperty("os.name").toLowerCase();
    if ("mac os x".equals(osName)) {
      EXECUTABLE_PATH = basePath + "osx/testwrapper";
      LIBRARY_PATH = basePath + "osx/libtest.so";
    } else if (osName.contains("win")) {
      EXECUTABLE_PATH = basePath + "win/testwrapper.exe";
      LIBRARY_PATH = basePath + "win/test.dll";
    } else {
      EXECUTABLE_PATH = basePath + "linux/testwrapper";
      LIBRARY_PATH = basePath + "linux/libtest.so";
    }
  }

  /**
   * All static.
   */
  private TestUtils() {
  }

  /**
   * It returns a file resource given the file path.
   *
   * @param path The file path.
   * @return The file or <code>null</code> if the path is invalid.
   */
  private static File getFileResource(String path) {
    try {
      URL resourceUrl = ClassLoader.getSystemClassLoader().getResource(path);
      if (resourceUrl != null) {
        URI resourceUri = resourceUrl.toURI();
        File file = new File(resourceUri.getPath());
        if (file.setExecutable(true)) {
          return file;
        }
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  /**
   * Returns a {@link java.io.File} instance representing the test executable.
   *
   * @return A <code>File</code> pointing to the test executable.
   */
  static File getExecutable() {
    return getFileResource(EXECUTABLE_PATH);
  }

  /**
   * Returns a {@link java.io.File} instance representing the test library.
   *
   * @return A <code>File</code> pointing to the test library.
   */
  static File getLibrary() {
    return getFileResource(LIBRARY_PATH);
  }

  /**
   * Returns a test {@link net.viktorc.pp4j.api.ProcessManagerFactory} instance.
   *
   * @return A test <code>ProcessManagerFactory</code> instance.
   */
  static ProcessManagerFactory createTestProcessManagerFactory() {
    return new TestProcessManagerFactory();
  }

  /**
   * A simple test process manager factory for starting process managers for the test program.
   *
   * @author Viktor Csomor
   */
  private static class TestProcessManagerFactory implements ProcessManagerFactory {

    ProcessBuilder builder;

    /**
     * Constructs an instance for creating process managers.
     */
    TestProcessManagerFactory() {
      builder = new ProcessBuilder(getExecutable().getAbsolutePath());
    }

    @Override
    public ProcessManager newProcessManager() {
      return new SimpleProcessManager(builder) {

        @Override
        public boolean startsUpInstantly() {
          return false;
        }

        @Override
        public boolean isStartedUp(String outputLine, boolean error) {
          return !error && "hi".equals(outputLine);
        }
      };
    }

  }

}
