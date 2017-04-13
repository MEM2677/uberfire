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
package org.uberfire.client.mvp;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.security.Resource;
import org.uberfire.workbench.model.ActivityResourceType;

@Dependent
public class PlaceHistoryHandler {

    private static final Logger log = Logger.getLogger(PlaceHistoryHandler.class.getName());
    private final Historian historian;
    @Inject
    private PlaceRequestHistoryMapper mapper;
    private PlaceManager placeManager;
    private PlaceRequest defaultPlaceRequest = PlaceRequest.NOWHERE;
    private Map<Activity, PlaceRequest> objects = new HashMap<>();
    private Map<PlaceRequest, Boolean> perspective = new HashMap<>();

    /**
     * Create a new PlaceHistoryHandler.
     */
    public PlaceHistoryHandler() {
        this.historian = GWT.create(DefaultHistorian.class);
    }

    /**
     * Handle the current history token. Typically called at application start,
     * to ensure bookmark launches work.
     */
    public void handleCurrentHistory() {
        handleHistoryToken(historian.getToken());
    }

    /**
     * Initialize this place history handler.
     * @return a registration object to de-register the handler
     */
    public HandlerRegistration register(final PlaceManager placeManager,
                                        final EventBus eventBus,
                                        final PlaceRequest defaultPlaceRequest) {
        this.placeManager = placeManager;
        this.defaultPlaceRequest = defaultPlaceRequest;
        /*
         * final HandlerRegistration placeReg =
         * eventBus.addHandler(PlaceChangeEvent.TYPE, new
         * PlaceChangeEvent.Handler() { public void
         * onPlaceChange(PlaceChangeEvent event) { Place newPlace =
         * event.getNewPlace();
         * historian.newItem(tokenForPlace(newPlaceRequest), false); } });
         */

        final HandlerRegistration historyReg =
                historian.addValueChangeHandler(new ValueChangeHandler<String>() {
                    @Override
                    public void onValueChange(ValueChangeEvent<String> event) {
                        String token = event.getValue();
                        handleHistoryToken(token);
                    }
                });

        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                PlaceHistoryHandler.this.defaultPlaceRequest = DefaultPlaceRequest.NOWHERE;
                PlaceHistoryHandler.this.placeManager = null;
                //placeReg.removeHandler();
                historyReg.removeHandler();
            }
        };
    }

    public void onPlaceChange(final PlaceRequest placeRequest) {
        if (placeRequest.isUpdateLocationBarAllowed()) {
            historian.newItem(tokenForPlace(placeRequest),
                              false);
        }
    }

    /**
     * Visible for testing.
     */
    Logger log() {
        return log;
    }

    private void handleHistoryToken(String token) {

        PlaceRequest newPlaceRequest = null;

        if ("".equals(token)) {
            newPlaceRequest = defaultPlaceRequest;
        }

        if (newPlaceRequest == null) {
            newPlaceRequest = mapper.getPlaceRequest(token);
        }

        if (newPlaceRequest == null) {
            log().warning("Unrecognized history token: " + token);
            newPlaceRequest = defaultPlaceRequest;
        }

        placeManager.goTo(newPlaceRequest);
    }

    private boolean isNotBlank(String str) {
        return (null != str && !"".equals(str.trim()));
    }

    /**
     * URL schema   perspective#screen-1,screen-2#editor-path1,editor-path2
     * @param newPlaceRequest
     * @return
     */
    private String tokenForPlace(final PlaceRequest newPlaceRequest) {
        StringJoiner sj = new StringJoiner("");
        if (defaultPlaceRequest.equals(newPlaceRequest)) {
            return "";
        }
        String perspective = objects.entrySet().stream()
                .filter(map -> map.getKey().isType(ActivityResourceType.PERSPECTIVE.name()))
                .map(map -> map.getValue().getIdentifier())
                .collect(Collectors.joining());
        String screens = objects.entrySet().stream()
                .filter(map -> map.getKey().isType(ActivityResourceType.SCREEN.name()))
                .map(map -> map.getValue().getIdentifier())
                .collect(Collectors.joining(","));
        String closed = this.perspective.entrySet()
                .stream().filter(map -> !map.getValue())
                .map(map -> "~".concat(map.getKey().getIdentifier()))
                .collect(Collectors.joining(","));
        String editors = objects.entrySet().stream()
                .filter(map -> map.getKey().isType(ActivityResourceType.EDITOR.name()))
                .map(map -> map.getValue().getPath().toURI())
                .collect(Collectors.joining(","));
        // we won't modify URL unless we have the perspective name
        if (isNotBlank(perspective)) {
            sj.add(perspective);
            // append screen and paths if either of them is not null
            if (isNotBlank(editors) || isNotBlank(screens)) {
                // the placeholder for the 'screen' list appear if editor is not null
                if (isNotBlank(screens) || isNotBlank(editors)) {
                    sj.add("#" + screens);
                    if (isNotBlank(closed))
                    {
                        sj.add(",");
                    }
                    sj.add(closed);
                    // finally
                    if (isNotBlank(editors)) {
                        sj.add("#" + editors);
                    }
                }
            }
        }
        GWT.log("URL -> " + sj.toString());
        return sj.toString();
    }

    public void register(Activity activity,
                         PlaceRequest place) {
        GWT.log(" ~~ adding token ~~ " + activity.getResourceType().getName() + ":" + place.getIdentifier());
        for (Resource res : activity.getDependencies()) {
            GWT.log("    ### depends ~~ " + res.getResourceType().getName());
        }
        if (place.getPath() != null) {
            GWT.log("    >>> path: " + place.getPath());
        }
        objects.put(activity,
                    place);
        onPlaceChange(place);
    }

    // This gets called only when opening recursively all the screens of a perspective
    public void register(PlaceRequest place) {
        perspective.put(place,
                        true);
    }

    public void registerClose(Activity activity,
                              PlaceRequest place) {
        GWT.log(" ~~ removing token ~~ " + activity.getResourceType().getName() + ":" + place.getIdentifier());
        if (place.getPath() != null) {
            GWT.log("    >>> path: " + place.getPath());
        }
//        objects.forEach((key, value) -> {
//            GWT.log("   (have key " + key.getResourceType().getName() + ":" + value.getIdentifier() + ")");
//        });
        // remove matching screen (screens don't have a path)
        objects.entrySet().removeIf(
                entry -> {
                    boolean nameMatch = entry.getValue().getIdentifier().equals(place.getIdentifier());

                    if (nameMatch
                            && perspective.containsKey(entry.getValue())) {
                        GWT.log("~~ removing perspective screen ~~");
                        perspective.put(entry.getValue(),
                                        false);
                    }
                    // want to get rid of the activity with matching path
//                    if (isPlacePath) {
//                        return (entry.getValue().getIdentifier().equals(place.getIdentifier())
//                                && null != entry.getValue().getPath()
//                                && entry.getValue().getPath().equals(place.getPath()));
//                    }
                    return (nameMatch);
                }
        );
        onPlaceChange(place);
    }

    public void flush() {
//        GWT.log(" ~~ flushing history line ~~");
        objects.clear();
        perspective.clear();
    }

    /**
     * Optional delegate in charge of History related events. Provides nice
     * isolation for unit testing, and allows pre- or post-processing of tokens.
     * Methods correspond to the like named methods on {@link History}.
     */
    public interface Historian {

        /**
         * Adds a {@link com.google.gwt.event.logical.shared.ValueChangeEvent}
         * handler to be informed of changes to the browser's history stack.
         * @param valueChangeHandler the handler
         * @return the registration used to remove this value change handler
         */
        com.google.gwt.event.shared.HandlerRegistration addValueChangeHandler(final ValueChangeHandler<String> valueChangeHandler);

        /**
         * @return the current history token.
         */
        String getToken();

        /**
         * Adds a new browser history entry. Calling this method will cause
         * {@link ValueChangeHandler#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)}
         * to be called as well.
         */
        void newItem(final String token,
                     final boolean issueEvent);
    }

    /**
     * Default implementation of {@link Historian}, based on {@link History}.
     */
    public static class DefaultHistorian
            implements
            Historian {

        @Override
        public com.google.gwt.event.shared.HandlerRegistration addValueChangeHandler(final ValueChangeHandler<String> valueChangeHandler) {
            return History.addValueChangeHandler(valueChangeHandler);
        }

        @Override
        public String getToken() {
            return History.getToken();
        }

        @Override
        public void newItem(String token,
                            boolean issueEvent) {

            History.newItem(token,
                            issueEvent);
        }
    }
}
