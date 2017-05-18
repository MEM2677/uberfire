/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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

package org.uberfire.client.workbench.docks;

import java.util.Map;

/**
 * Uberfire Dock Support
 */
public interface UberfireDocks {

    void configure(Map<String, String> configurations);

    void add(UberfireDock... docks);

    void remove(UberfireDock... docks);

    void expand(UberfireDock dock);

    void disable(UberfireDockPosition position,
                 String perspectiveName);

    void enable(UberfireDockPosition position,
                String perspectiveName);
    boolean isScreenDockedInPerspective(String perspective,
                                        String screen);

    UberfireDock getDockedScreenInPerspective(String perspective,
                                              String screen);
}
