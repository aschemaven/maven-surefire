/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.surefire;

import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.project.MavenProject;

import static java.util.Collections.unmodifiableList;

/**
 * Class-path/module-path split as dispatched by Maven 4 core: instead of the plugin
 * deciding per element (plexus-java), Maven 4 separates the test-runtime paths itself,
 * driven by the per-dependency {@code <type>} declaration ({@code modular-jar},
 * {@code classpath-jar}, or the Maven 3 heuristic for the default type). This guarantees
 * tests run with the same separation the compiler used.
 * <p>
 * This class is the entry guard of the version dispatch: it detects the Maven 4 session
 * bridge reflectively and only then looks up a {@link Maven4DispatchProvider} — the
 * provider implementation ships in the {@code surefire-maven4} module, is compiled for
 * the Maven 4 baseline (Java 17) and is never loaded under Maven 3.
 */
public final class Maven4DispatchedPaths {
    private final List<String> classpathElements;
    private final List<String> modulepathElements;

    public Maven4DispatchedPaths(List<String> classpathElements, List<String> modulepathElements) {
        this.classpathElements = unmodifiableList(classpathElements);
        this.modulepathElements = unmodifiableList(modulepathElements);
    }

    public List<String> getClasspathElements() {
        return classpathElements;
    }

    public List<String> getModulepathElements() {
        return modulepathElements;
    }

    /**
     * Resolves the dispatched test-runtime paths if the running Maven exposes the
     * Maven 4 API and a dispatch provider is on the plugin class path.
     *
     * @param session the Maven 3 view of the session
     * @param project the project under test
     * @param logger the console logger for debug output
     * @return the dispatched split, or null when running under Maven 3, without a
     *         provider, or when the dispatch fails — callers fall back to the
     *         plugin-side resolution
     */
    static Maven4DispatchedPaths tryResolve(MavenSession session, MavenProject project, ConsoleLogger logger) {
        try {
            MavenSession.class.getMethod("getSession");
        } catch (NoSuchMethodException e) {
            // Maven 3 core: no Maven 4 API session bridge — never load any provider
            return null;
        }
        try {
            for (Maven4DispatchProvider provider :
                    ServiceLoader.load(Maven4DispatchProvider.class, Maven4DispatchedPaths.class.getClassLoader())) {
                Maven4DispatchedPaths dispatched = provider.resolve(session, project, logger);
                if (dispatched != null) {
                    return dispatched;
                }
            }
            logger.debug("Maven 4 path dispatch: no provider available");
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            logger.debug("Maven 4 path dispatch unavailable, falling back to plugin-side resolution: " + e);
        }
        return null;
    }
}
