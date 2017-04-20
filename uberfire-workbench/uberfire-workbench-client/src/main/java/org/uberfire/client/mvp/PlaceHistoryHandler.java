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

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import org.uberfire.client.workbench.events.PlaceGainFocusEvent;
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
    /* This is the 'state' string that will be shown in the address bar */
    private String historyUrl;
    /* this should be an incoming request from the browser */
    private String addressBarUrl;

    public final static String PERSPECTIVE_SEP = "|";
    public final static String SCREEN_SEP = ",";
    public final static String OTHER_SCREEN_SEP = "$";
    public final static String NEGATION_PREFIX = "~";

    public final static int MAX_NAV_URL_SIZE = 1900;

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
    // NOT USED
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

    // seful for the corner cases  (eg. editor with a screen opened in it)
//    private void onWorkbenchPartOnFocus(@Observes PlaceGainFocusEvent event) {
//        GWT.log(">>> " + event.getPlace().getFullIdentifier());
//    }

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

    /**
     * historyUrl schema   perspective#screen-1,screen-2#editor-path1,editor-path2
     * @param newPlaceRequest
     * @return
     */
    private String tokenForPlace(final PlaceRequest newPlaceRequest) {
        StringJoiner sj = new StringJoiner("");
        if (defaultPlaceRequest.equals(newPlaceRequest)) {
            return "";
        }

//        GWT.log("historyUrl -> " + historyUrl);

        // length check - temporary
        if (isNotBlank(historyUrl)
                && historyUrl.length() >= MAX_NAV_URL_SIZE) {
            int idx = historyUrl.lastIndexOf(SCREEN_SEP);

            GWT.log("truncating URL history for safety reasons");
            while (historyUrl.length() >= MAX_NAV_URL_SIZE && idx > 0) {
                idx = historyUrl.lastIndexOf(SCREEN_SEP);
                if (idx != -1) {
                    historyUrl = historyUrl.substring(0,
                                                      idx);
                }
            }
            // paranoid check
            while (historyUrl.length() >= MAX_NAV_URL_SIZE) {
                historyUrl = historyUrl.substring(0, MAX_NAV_URL_SIZE);
            }
        }
        return historyUrl;
    }

    /**
     * Check whether teh string is valid
     * @param str
     * @return
     */
    private boolean isNotBlank(String str) {
        return (null != str && !"".equals(str.trim()));
    }

    /**
     * Returns true if the perspective is present in the URL
     * @return
     */
    private boolean isPerspectiveInUrl(String url) {
        return (isNotBlank(url) && (url.indexOf(PERSPECTIVE_SEP) > 0));
    }

    private boolean isPerspectiveInUrl() { // FIXME don't think this method overload is really needed
        return isPerspectiveInUrl(historyUrl);
    }

    /**
     * Handle URL - delete a closed screen
     * @param screen
     */
    private void removeScreenFromUrl(String screen) {
        // SILLY string analysis
        if ((historyUrl.indexOf('$') > 0 && historyUrl.indexOf(screen) < historyUrl.indexOf('$'))
                || historyUrl.indexOf('$') == -1) {
            historyUrl = historyUrl.replace(screen,
                                            NEGATION_PREFIX.concat(screen));
        } else {
            // simply delete from the historyUrl
            historyUrl = historyUrl.replace(screen,
                                            "");
        }
        // always get rid of double commas
        historyUrl = historyUrl.replace(",,",
                                        SCREEN_SEP);
        if (historyUrl.endsWith(OTHER_SCREEN_SEP)) {
            historyUrl = historyUrl.substring(0,
                                              historyUrl.indexOf('$'));
        }
    }

    /**
     * Handle URL - add a newly opened screen
     * @param screen
     */
    private void addScreenToUrl(String screen) {
        String negatedScreen = NEGATION_PREFIX.concat(screen);

        if (isNotBlank(screen) && isNotBlank(historyUrl) &&
                historyUrl.length() + screen.length() >= MAX_NAV_URL_SIZE)
        {
            GWT.log("ignoring screen '" + screen + "' to avoid a lengthy URL");
            return;
        }

        if (historyUrl.indexOf(negatedScreen) != -1) {
            historyUrl = historyUrl.replace(negatedScreen,
                                            screen);
        } else if (historyUrl.indexOf(screen) > 0) {
            // do nothing the screen is already present
        } else if (!isPerspectiveInUrl()) {
            // must add the screen in the group of the current perspective (which is not yet loaded)
            if (isNotBlank(historyUrl)) {
                historyUrl = historyUrl.concat(SCREEN_SEP).concat(screen);
            } else {
                historyUrl = screen;
            }
        } else {
            // this is a screen outside the current perspective
            if (historyUrl.indexOf(OTHER_SCREEN_SEP) == -1) {
                // add the '$' if needed
                historyUrl = historyUrl.concat(OTHER_SCREEN_SEP).concat(screen);
            }
            // otherwise append the screen after the last element
            else {
                historyUrl = historyUrl.concat(SCREEN_SEP).concat(screen);
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

//        GWT.log(" ~~ adding token ~~ " + activity.getResourceType().getName() + ":" + place.getFullIdentifier());
//        if (place.getPath() != null) {
//            GWT.log("    >>> path: " + place.getPath());
//        }
        if (activity.isType(ActivityResourceType.PERSPECTIVE.name())) {
            if (!isPerspectiveInUrl(addressBarUrl)) {
                historyUrl = screen.concat(PERSPECTIVE_SEP).concat(historyUrl);
            } else {
                if (!isNotBlank(historyUrl)) {
                    historyUrl = addressBarUrl;
                }
            }
        } else if (activity.isType(ActivityResourceType.SCREEN.name())) {
            // add screen to the historyUrl
            addScreenToUrl(screen);
        }
        onPlaceChange(place);
    }

    // This gets called only when opening recursively all the screens of a perspective
    public void register(PlaceRequest place) {
        // FIXME delete this!, not used
    }

    /**
     * Get the perspective id from the URL.
     * @param place
     * @return
     */
    public PlaceRequest getPerspectiveFromUrl(PlaceRequest place) {
        addressBarUrl = place.getFullIdentifier();
        if (isPerspectiveInUrl(addressBarUrl)) {
            String perspectiveName = addressBarUrl.substring(0,
                                                             addressBarUrl.indexOf(PERSPECTIVE_SEP));
//            GWT.log(">>> perspective in the ADDRESS BAR: " + perspectiveName);

            // FIXME CREATE A SMALL FACTORY - THE OBJECT MUST BE OF THE SAME TYPE
            place = new DefaultPlaceRequest(perspectiveName);
        }
        return place;
    }

    private List<String> getScreensFromUrl(Boolean open) {
        List result = new ArrayList<>();
        if (isNotBlank(addressBarUrl)) {

            // FIXME make it robust! this is for controlled testing only
            String url = addressBarUrl.substring(addressBarUrl.indexOf(PERSPECTIVE_SEP));
            String[] screens = url.split(SCREEN_SEP);

            for (String screen : screens) {
                if (((!open) && screen.startsWith(NEGATION_PREFIX))
                        || (open && !screen.startsWith(NEGATION_PREFIX))) {
                    result.add(screen.substring(1));
                }
            }
        }
        return result;
    }

    public List<String> getClosedScreenFromUrl() {
        return getScreensFromUrl(false);
    }

    public List<String> getOpenedScreenFromUrl() {
        return getScreensFromUrl(true);
    }

    public void registerClose(Activity activity,
                              PlaceRequest place) {
        String id = place.getFullIdentifier();

//        GWT.log(" ~~ removing token ~~ " + activity.getResourceType().getName() + ":" + place.getFullIdentifier());
//        if (place.getPath() != null) {
//            GWT.log("    >>> path: " + place.getPath());
//        }
        if (activity.isType(ActivityResourceType.SCREEN.name())) {
            removeScreenFromUrl(id);
        } else {
            // suppress CQ warnings
        }
        // update historyUrl
        onPlaceChange(place);
    }

    public void flush() {
//        GWT.log(" ~~ flushing history line ~~");
        historyUrl = "";
        addressBarUrl = "";
    }

    public String getToken() {
        return (historian.getToken());
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
