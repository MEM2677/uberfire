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

import java.util.StringJoiner;
import java.util.logging.Logger;
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
import org.uberfire.workbench.model.ActivityResourceType;

@Dependent
public class PlaceHistoryHandler {

    private static final Logger log = Logger.getLogger(PlaceHistoryHandler.class.getName());
    private final Historian historian;
    @Inject
    private PlaceRequestHistoryMapper mapper;
    private PlaceManager placeManager;
    private PlaceRequest defaultPlaceRequest = PlaceRequest.NOWHERE;
    private String URL;

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

//        GWT.log("URL -> " + URL);
        return URL;
    }

    /**
     * Returns true if anf
     * @return
     */
    private boolean isPerspectiveInUrl() {
        return (isNotBlank(URL) && URL.indexOf("|") > 0);
    }

    private void removeScreenFromUrl(String screen)
    {

    }

    private void addScreenToUrl(String screen)
    {
        String negatedScreen = "~".concat(screen);

        if (URL.indexOf(negatedScreen) != -1)
        {
            URL = URL.replace(negatedScreen, screen);
        }
        else if (URL.indexOf(screen) > 0)
        {
            // do nothing the screen is already present
        }
        else if (!isPerspectiveInUrl())
        {
            // must add the screen in the group of the current perspective (which is not yet loaded)
            if (isNotBlank(URL))
            {
                URL = URL.concat(",").concat(screen);
            }
            else
            {
                URL = screen;
            }
        }
        else
        {
            // this is a screen outside the current perspective
            if (URL.indexOf("$") == -1) {
                // add the '$' if needed
                URL = URL.concat("$").concat(screen);
            }
            // otherwise append the screen after the last element
            else {
                URL = URL.concat(",").concat(screen);
            }
        }
    }

    /**
     * The behaviour is simple: when opening the perspective the perspective itself
     * is the last element to be called with a goTo(), being the screens first.
     * We assume that every screen opened with a goTo() before the perspective is called
     * belongs to the same perspective
     * @param activity
     * @param place
     */
    public void register(Activity activity,
                         PlaceRequest place) {
        String screen = place.getFullIdentifier();

        GWT.log(" ~~ adding token ~~ " + activity.getResourceType().getName() + ":" + place.getIdentifier());
        if (place.getPath() != null) {
            GWT.log("    >>> path: " + place.getPath());
        }
        if (activity.isType(ActivityResourceType.PERSPECTIVE.name())) {
            URL = screen.concat("|").concat(URL);
        } else if (activity.isType(ActivityResourceType.SCREEN.name())) {
//            // if the screen is NOT in the existing URL...
//            if (isPerspectiveInUrl() && URL.indexOf(id) == -1) {
//                // ... if the URL does NOT end with a '$' add it
//                if (URL.indexOf("$") == -1) {
//                    URL = URL.concat("$").concat(id);
//                }
//                // otherwise append the screen after the last element
//                else {
//                    URL = URL.concat(",").concat(id);
//                }
//            } else if (!isPerspectiveInUrl()){
//                // add the screen to the perspective group if it isn't there already
//                if (URL.indexOf(id) == -1)
//                {
//                    if (isNotBlank(URL)) {
//                        URL = URL.concat(",");
//                    }
//                    URL = URL.concat(place.getIdentifier());
//                }
//            }
            addScreenToUrl(screen);
        }
        onPlaceChange(place);
    }

    // This gets called only when opening recursively all the screens of a perspective
    public void register(PlaceRequest place) {
//        if (isNotBlank(URL)) {
//            URL = URL.concat(",");
//        }
//        URL = URL.concat(place.getIdentifier());
    }

    public void registerClose(Activity activity,
                              PlaceRequest place) {
        String id = place.getFullIdentifier();

        GWT.log(" ~~ removing token ~~ " + activity.getResourceType().getName() + ":" + place.getIdentifier());
        if (place.getPath() != null) {
            GWT.log("    >>> path: " + place.getPath());
        }
        if (activity.isType(ActivityResourceType.SCREEN.name())) {
            // SILLY string analysis
            if ((URL.indexOf('$') > 0 && URL.indexOf(id) < URL.indexOf('$'))
                    || URL.indexOf('$') == -1) {
                URL = URL.replace(id,
                                  "~".concat(id));
            } else {
                // simply delete from the URL
                URL = URL.replace(id,
                                  "");
            }
            URL = URL.replace(",,",
                              ",");
            if (URL.endsWith("$")) {
                URL = URL.substring(0,
                                    URL.indexOf('$'));
            }
        } else {
            // suppress CQ warnings
        }
        // update URL
        onPlaceChange(place);
    }

    public void flush() {
//        GWT.log(" ~~ flushing history line ~~");
        URL = "";
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
