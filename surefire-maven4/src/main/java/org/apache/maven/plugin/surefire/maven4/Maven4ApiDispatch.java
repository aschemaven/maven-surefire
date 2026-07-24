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
package org.apache.maven.plugin.surefire.maven4;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.surefire.Maven4DispatchProvider;
import org.apache.maven.plugin.surefire.Maven4DispatchedPaths;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.project.MavenProject;

/**
 * Maven 4 implementation of the dispatch seam. Mirrors what maven-compiler-plugin 4.x
 * does for compilation: ask Maven core's {@link DependencyResolver} for the test-runtime
 * paths and consume the class-path/module-path separation decided by core (driven by the
 * per-dependency {@code <type>} declaration).
 * <p>
 * Compiled for Java 17 against the Maven 4 API — this class is only ever loaded when
 * the entry guard verified the running Maven exposes that API.
 */
public class Maven4ApiDispatch implements Maven4DispatchProvider {
    @Override
    public Maven4DispatchedPaths resolve(MavenSession mavenSession, MavenProject mavenProject, ConsoleLogger logger) {
        Session session = mavenSession.getSession();
        Project project = session.getProjects().stream()
                .filter(candidate -> candidate.getGroupId().equals(mavenProject.getGroupId())
                        && candidate.getArtifactId().equals(mavenProject.getArtifactId())
                        && candidate.getVersion().equals(mavenProject.getVersion()))
                .findFirst()
                .orElse(null);
        if (project == null) {
            logger.debug("Maven 4 path dispatch: project " + mavenProject.getId() + " not found in session");
            return null;
        }
        DependencyResolver resolver = session.getService(DependencyResolver.class);
        DependencyResolverResult result = resolver.resolve(DependencyResolverRequest.builder()
                .session(session)
                .project(project)
                .requestType(DependencyResolverRequest.RequestType.RESOLVE)
                .pathScope(PathScope.TEST_RUNTIME)
                .pathTypeFilter(t -> JavaPathType.CLASSES.equals(t) || JavaPathType.MODULES.equals(t))
                .build());
        Map<PathType, List<Path>> dispatched = result.getDispatchedPaths();
        List<String> classpath = toStrings(dispatched.get(JavaPathType.CLASSES));
        List<String> modulepath = toStrings(dispatched.get(JavaPathType.MODULES));
        logger.debug("Maven 4 dispatched test class-path: " + classpath);
        logger.debug("Maven 4 dispatched test module-path: " + modulepath);
        return new Maven4DispatchedPaths(classpath, modulepath);
    }

    private static List<String> toStrings(List<Path> paths) {
        List<String> strings = new ArrayList<>();
        if (paths != null) {
            for (Path path : paths) {
                strings.add(path.toAbsolutePath().toString());
            }
        }
        return strings;
    }
}
