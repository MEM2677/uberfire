/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uberfire.java.nio.fs.jgit.util.commands;

/**
 * TODO: update me
 */
public class PathUtil {

    public static String normalize(final String path) {

        if (path.equals("/")) {
            return "";
        }

        final boolean startsWith = path.startsWith("/");
        final boolean endsWith = path.endsWith("/");
        if (startsWith && endsWith) {
            return path.substring(1,
                                  path.length() - 1);
        }
        if (startsWith) {
            return path.substring(1);
        }
        if (endsWith) {
            return path.substring(0,
                                  path.length() - 1);
        }
        return path;
    }
}
