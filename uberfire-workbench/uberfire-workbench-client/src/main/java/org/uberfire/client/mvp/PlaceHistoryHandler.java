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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private String bookmarkableUrl = "";

    public final static String PERSPECTIVE_SEP = "|";
    public final static String SCREEN_SEP = ",";
    public final static String OTHER_SCREEN_SEP = "$";
    public final static String CLOSED_PREFIX = "~";
    public final static String DOCK_PREFIX = "!";

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
    public HandlerRegistration registerOpen(final PlaceManager placeManager,
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


    Logger log() {
        return log;
    }


    public String getBookmarkableUrl() {
        return bookmarkableUrl;
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
     * bookmarkableUrl schema   perspective#screen-1,screen-2#editor-path1,editor-path2
     * @param newPlaceRequest
     * @return
     */
    private String tokenForPlace(final PlaceRequest newPlaceRequest) {
        if (defaultPlaceRequest.equals(newPlaceRequest)) {
            return "";
        }
        return bookmarkableUrl;
    }

    /**
     * Check whether the string is valid
     * @param str
     * @return
     */
    private boolean isNotBlank(final String str) {
        return (null != str && !"".equals(str.trim()));
    }

    private boolean isNotBlank(final PlaceRequest place)
    {
        return (null != place && isNotBlank(place.getFullIdentifier()));
    }

    /**
     * Returns true if the perspective is present in the URL
     * @return
     */
    private boolean isPerspectiveInUrl(String url) {
        return (isNotBlank(url) && (url.indexOf(PERSPECTIVE_SEP) > 0));
    }

//    private boolean isOtherScreenInUrl(String url) {
//        return (isNotBlank(url) && (url.indexOf(OTHER_SCREEN_SEP) > 0));
//    }

    private boolean isPerspectiveInUrl() { // FIXME don't think this method overload is really needed
        return isPerspectiveInUrl(bookmarkableUrl);
    }

    /**
     * Check if the URL contains screens not belonging to the current perspective
     * @return
     */
    private boolean urlContainsExtraPerspectiveScreen()
    {
        return (bookmarkableUrl.indexOf(OTHER_SCREEN_SEP) != -1);
    }

    /**
     * Check whether the screen belongs to the currently opened perspective
     * @param screen
     * @return
     */
    private boolean isPerspectiveScreen(final String screen) {
        return (isNotBlank(screen)
                && (!urlContainsExtraPerspectiveScreen()
                    || (bookmarkableUrl.indexOf(OTHER_SCREEN_SEP) > bookmarkableUrl.indexOf(screen))));
    }

    /**
     * Given a screen name, this method extracts the corresponding token in the
     * URL, that is the screen name with optional parameters and markers
     * @param screen
     * @return
     */
    private String getUrlToken(final String screen) {
        int st = isPerspectiveInUrl() ? (bookmarkableUrl.indexOf(PERSPECTIVE_SEP) + 1) : 0;
        String screensList = bookmarkableUrl.replace(OTHER_SCREEN_SEP,
                                                     SCREEN_SEP)
                .substring(st,
                           bookmarkableUrl.length());

        String tokens[] = screensList.split(SCREEN_SEP);
        Optional<String> token = Arrays.asList(tokens).stream()
                .filter(s -> s.contains(screen))
                .findFirst();

        return token.orElse(screen);
    }

    /**
     * Return true if the given screen is already closed.
     * @param screen
     * @return
     */
    private boolean isScreenClosed(String screen)
    {
        if (!screen.startsWith(CLOSED_PREFIX))
        {
            screen = CLOSED_PREFIX.concat(screen);
        }
        return (bookmarkableUrl.indexOf(screen) != -1);
    }

    /**
     * Handle URL - delete a closed screen
     * @param screenName
     */
    private void markScreenClosedInUrl(final String screenName) {
        final boolean isPerspective = isPerspectiveScreen(screenName);
        final String separator = isPerspective ? PERSPECTIVE_SEP : OTHER_SCREEN_SEP;
        final String closedScreen = CLOSED_PREFIX.concat(screenName);
        final String uniqueScreenAfterDelimiter =
                separator.concat(screenName); // |screen or $screen
        final String firstScreenAfterDelimiter =
                uniqueScreenAfterDelimiter.concat(SCREEN_SEP); // |screen, or $screen,
        final String commaSeparatedScreen =
                screenName.concat(SCREEN_SEP); // screen,

        // check screen already closed
        if (isScreenClosed(closedScreen)) {
            return;
        }
        if (isPerspective) {
            bookmarkableUrl = bookmarkableUrl.replace(screenName,
                                                      closedScreen);
        } else {
            // check for SEP + screen + ","
            if (bookmarkableUrl.contains(firstScreenAfterDelimiter)) {
                bookmarkableUrl = bookmarkableUrl.replace(firstScreenAfterDelimiter,
                                                          separator);
            } else if (bookmarkableUrl.contains(uniqueScreenAfterDelimiter)) {
                bookmarkableUrl = bookmarkableUrl.replace(uniqueScreenAfterDelimiter,
                                                          "");
            } else if (bookmarkableUrl.contains(commaSeparatedScreen)) {
                bookmarkableUrl = bookmarkableUrl.replace(commaSeparatedScreen,
                                                          "");
            } else {
                bookmarkableUrl = bookmarkableUrl.replace(screenName,
                                                          "");
            }
        }
    }

    /**
     * Handle URL - add a newly opened screen
     * @param screenName
     */
    private void markScreenOpenInUrl(String screenName) {
        String closedScreen = CLOSED_PREFIX.concat(screenName);

        // if the length exceeds the max allowed size do nothing
        if (isNotBlank(screenName) && isNotBlank(bookmarkableUrl) &&
                bookmarkableUrl.length() + screenName.length() >= MAX_NAV_URL_SIZE) {
            return;
        }

        // if the screen was closed
        if (bookmarkableUrl.indexOf(closedScreen) != -1) {
            bookmarkableUrl = bookmarkableUrl.replace(closedScreen,
                                                      screenName);
        } else if (bookmarkableUrl.indexOf(screenName) > 0) {
            // do nothing coz the screen is already present
        } else if (!isPerspectiveInUrl()) {
            // must add the screen in the group of the current perspective (which is not yet loaded)
            if (isNotBlank(bookmarkableUrl)) {
                bookmarkableUrl = bookmarkableUrl.concat(SCREEN_SEP).concat(screenName);
            } else {
                bookmarkableUrl = screenName;
            }
        } else {
            // this is a screen outside the current perspective
            if (!urlContainsExtraPerspectiveScreen()) {
                // add the '$' if needed
                bookmarkableUrl = bookmarkableUrl.concat(OTHER_SCREEN_SEP).concat(screenName);
            }
            // otherwise append the screen after the last element
            else {
                bookmarkableUrl = bookmarkableUrl.concat(SCREEN_SEP).concat(screenName);
            }
        }
    }

    /**
     * register opened screen of perspective
     * @param activity
     * @param place
     * @param isDock
     */
    public void registerOpen(Activity activity,
                             PlaceRequest place,
                             boolean isDock) {
        String id = place.getFullIdentifier();

//        GWT.log(" ~~ adding token ~~ " + activity.getResourceType().getName() + ":" + id + " docks " + bookmarkableUrl.toString());
//        if (place.getPath() != null) {
//            GWT.log("    >>> path: " + place.getPath());
//        }

        if (activity.isType(ActivityResourceType.PERSPECTIVE.name())) {
            if (!isPerspectiveInUrl()) {
                bookmarkableUrl = id.concat(PERSPECTIVE_SEP).concat(bookmarkableUrl);
            }
        } else if (activity.isType(ActivityResourceType.SCREEN.name())) {
            // add the dock marker if needed
            id = isDock ? DOCK_PREFIX.concat(place.getFullIdentifier())
                    : place.getFullIdentifier();
            // add screen to the bookmarkableUrl
            markScreenOpenInUrl(id);
        }
        onPlaceChange(place);
    }

    /**
     * Get the perspective id from the URL.
     * @param place\
     * @return
     */
    public PlaceRequest getPerspectiveFromPlace(PlaceRequest place) {
        String url = place.getFullIdentifier();

        if (isPerspectiveInUrl(url)) {
            String perspectiveName = url.substring(0,
                                                   url.indexOf(PERSPECTIVE_SEP));
            PlaceRequest copy = place.clone();
            copy.setIdentifier(perspectiveName);
            // copy arguments
            if (!place.getParameters().isEmpty()) {
                for (Map.Entry<String, String> elem : place.getParameters().entrySet()) {
                    copy.addParameter(elem.getKey(),
                                      elem.getValue());
                }
            }
            return copy;
        }
        return place;
    }

    /**
     * Return all the screens (opened or closed) that is, everything
     * after the perspective declaration
     * @param place
     * @return
     */
    public Set<String> getScreensFromPlace(final PlaceRequest place) {
        String url;

        if (!isNotBlank(place)) {
            return new HashSet<String>();
        }
        // get everything after the perspective
        if (isPerspectiveInUrl(place.getFullIdentifier())) {
            String request = place.getFullIdentifier();

            url = request.substring(request.indexOf(PERSPECTIVE_SEP) + 1);
        }
        else {
            url = place.getFullIdentifier();
        }
        // replace the '$' with a comma ','
        url = url.replace(OTHER_SCREEN_SEP, SCREEN_SEP);
        String[] token = url.split(SCREEN_SEP);
        return new HashSet<>(Arrays.asList(token));
    }

    /**
     * Get the opened screens in the given place request
     * @param place
     * @return
     */
    public Set<String> getClosedScreenFromPlace(final PlaceRequest place) {
        Set<String> screens = getScreensFromPlace(place);
        Set<String> result = screens.stream()
                .filter(s -> s.startsWith(CLOSED_PREFIX))
                .collect(Collectors.toSet());
        return result;
    }

    /**
     * Get the opened screens in the given place request
     * @param place
     * @return
     */
    public Set<String> getOpenedScreenFromPlace(final PlaceRequest place) {
        Set<String> screens = getScreensFromPlace(place);
        Set<String> result = screens.stream()
                .filter(s -> !s.startsWith(CLOSED_PREFIX))
                .collect(Collectors.toSet());
        return result;
    }

    public void registerClose(Activity activity,
                              PlaceRequest place,
                              boolean isDock) {

        GWT.log("~~ close: " + place.getIdentifier());

        final String id = isDock ? DOCK_PREFIX.concat(place.getIdentifier())
                : place.getIdentifier();

        if (activity.isType(ActivityResourceType.SCREEN.name())) {
            final String token = getUrlToken(id);

            markScreenClosedInUrl(token);
        }
        // update bookmarkableUrl
        onPlaceChange(place);
    }

    public void flush() {
        bookmarkableUrl = "";
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
