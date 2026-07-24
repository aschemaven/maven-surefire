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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.project.MavenProject;

/**
 * Internal dispatch seam between Maven versions: implementations translate a
 * Maven-version-specific API into surefire's own model, so the mojo code stays
 * version-agnostic. Discovered via {@link java.util.ServiceLoader}; an implementation
 * compiled for a newer Maven/Java baseline (e.g. {@code surefire-maven4}) is only ever
 * loaded after {@link Maven4DispatchedPaths#tryResolve} verified the running Maven
 * provides that baseline.
 */
public interface Maven4DispatchProvider {
    /**
     * Resolves the dispatched test-runtime class-path/module-path separation.
     *
     * @param session the Maven 3 view of the session
     * @param project the project under test
     * @param logger the console logger for debug output
     * @return the dispatched split, or null if this provider cannot serve the running Maven
     * @throws Exception on resolution failures — callers fall back to plugin-side resolution
     */
    Maven4DispatchedPaths resolve(MavenSession session, MavenProject project, ConsoleLogger logger) throws Exception;
}
