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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.URL;
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
    private String historyUrl = "";

    public final static String PERSPECTIVE_SEP = "|";
    public final static String SCREEN_SEP = ",";
    public final static String OTHER_SCREEN_SEP = "$";
    public final static String NEGATION_PREFIX = "~";
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

    /*
    // Useful for the corner cases  (eg. editor with a screen opened in it)
    private void onWorkbenchPartOnFocus(@Observes PlaceGainFocusEvent event) {
        GWT.log("--PlaceGainFocusEvent--");
        GWT.log(">>> " + event.getPlace().getFullIdentifier());
    }
    private void onWorkbenchPartOnFocus(@Observes SelectPlaceEvent event) {
        GWT.log("--SelectPlaceEvent--");
        GWT.log(">>> " + event.getPlace().getFullIdentifier());
    }
    */

    /**
     * Visible for testing.
     */
    Logger log() {
        return log;
    }

    /**
     * Visible for testing.
     */
    public String getHistoryUrl() {
        return historyUrl;
    }

    /**
     * Visible for testing.
     */
    public String getAddressBarUrl() {
        return "";
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
                historyUrl = historyUrl.substring(0,
                                                  MAX_NAV_URL_SIZE);
            }
        }
        return historyUrl;
    }

    /**
     * Check whether the string is valid
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

    private boolean isOtherScreenInUrl(String url) {
        return (isNotBlank(url) && (url.indexOf(OTHER_SCREEN_SEP) > 0));
    }

    private boolean isPerspectiveInUrl() { // FIXME don't think this method overload is really needed
        return isPerspectiveInUrl(historyUrl);
    }

    /**
     * Check whether the screen belongs to the currently opened perspective
     * @param screen
     * @return
     */
    private boolean isPerspectiveScreen(String screen) {
        return (isNotBlank(screen) && ((-1 == historyUrl.indexOf(OTHER_SCREEN_SEP))
                || (historyUrl.indexOf(OTHER_SCREEN_SEP) > historyUrl.indexOf(screen))));
    }

    /**
     * Given a screen name, this method extracts the corresponding token in the
     * URL, that is the screen name with optional parameters and markers
     * @param screen
     * @return
     */
    private String getUrlToken(String screen) {
        int st = isPerspectiveInUrl() ? (historyUrl.indexOf(PERSPECTIVE_SEP) + 1) : 0;
        String screensList = historyUrl.replace(OTHER_SCREEN_SEP,
                                                SCREEN_SEP)
                .substring(st,
                           historyUrl.length());

        String tokens[] = screensList.split(SCREEN_SEP);
        Optional<String> token = Arrays.asList(tokens).stream()
                .filter(s -> s.contains(screen))
                .findFirst();

        return token.orElse(screen);
    }

    /**
     * Handle URL - delete a closed screen
     * @param tag
     */
    private void removeScreenFromUrl(String tag) {
        final boolean isPerspective = isPerspectiveScreen(tag);
        final String sep = isPerspective ? PERSPECTIVE_SEP : OTHER_SCREEN_SEP;
        final String negation = "~".concat(tag);
        final String first = sep.concat(tag); // |screen or $screen
        final String firstComma = first.concat(SCREEN_SEP); // |screen, or $screen,
        final String comma = tag.concat(SCREEN_SEP); // screen,

        // check screen already closed
        if (historyUrl.indexOf(negation) != -1) {
            return;
        }
        if (isPerspective) {
            historyUrl = historyUrl.replace(tag,
                                            negation);
        } else {
            // check for SEP + screen + ","
            if (historyUrl.contains(firstComma)) {
                historyUrl = historyUrl.replace(firstComma,
                                                sep);
            } else if (historyUrl.contains(first)) {
                historyUrl = historyUrl.replace(first,
                                                "");
            } else if (historyUrl.contains(comma)) {
                historyUrl = historyUrl.replace(comma,
                                                "");
            } else {
                historyUrl = historyUrl.replace(tag,
                                                "");
            }
        }
    }

    /**
     * Handle URL - add a newly opened screen
     * @param tag
     */
    private void addScreenToUrl(String tag) {
        String negatedScreen = NEGATION_PREFIX.concat(tag);

        if (isNotBlank(tag) && isNotBlank(historyUrl) &&
                historyUrl.length() + tag.length() >= MAX_NAV_URL_SIZE) {
            GWT.log("ignoring screen '" + tag + "' to avoid a lengthy URL");
            return;
        }

        if (historyUrl.indexOf(negatedScreen) != -1) {
            historyUrl = historyUrl.replace(negatedScreen,
                                            tag);
        } else if (historyUrl.indexOf(tag) > 0) {
            // do nothing the screen is already present
        } else if (!isPerspectiveInUrl()) {
            // must add the screen in the group of the current perspective (which is not yet loaded)
            if (isNotBlank(historyUrl)) {
                historyUrl = historyUrl.concat(SCREEN_SEP).concat(tag);
            } else {
                historyUrl = tag;
            }
        } else {
            // this is a screen outside the current perspective
            if (historyUrl.indexOf(OTHER_SCREEN_SEP) == -1) {
                // add the '$' if needed
                historyUrl = historyUrl.concat(OTHER_SCREEN_SEP).concat(tag);
            }
            // otherwise append the screen after the last element
            else {
                historyUrl = historyUrl.concat(SCREEN_SEP).concat(tag);
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

//        GWT.log(" ~~ adding token ~~ " + activity.getResourceType().getName() + ":" + id + " docks " + historyUrl.toString());
//        if (place.getPath() != null) {
//            GWT.log("    >>> path: " + place.getPath());
//        }

        if (activity.isType(ActivityResourceType.PERSPECTIVE.name())) {
            if (!isPerspectiveInUrl()) {
                historyUrl = id.concat(PERSPECTIVE_SEP).concat(historyUrl);
            }
        } else if (activity.isType(ActivityResourceType.SCREEN.name())) {
            // add the dock marker if needed
            id = isDock ? DOCK_PREFIX.concat(place.getFullIdentifier())
                    : place.getFullIdentifier();
            // add screen to the historyUrl
            addScreenToUrl(id);
        }
        onPlaceChange(place);
    }

    /**
     * Get the perspective id from the URL.
     * @param place\
     * @return
     */
    public PlaceRequest getPerspectiveFromUrl(PlaceRequest place) {
        String url = URL.decode(place.getFullIdentifier());
        if (isPerspectiveInUrl(url)) {
            String perspectiveName = url.substring(0,
                                                   url.indexOf(PERSPECTIVE_SEP));
//            GWT.log(">>> perspective in the ADDRESS BAR: " + perspectiveName);

            // FIXME CREATE A SMALL FACTORY - THE OBJECT MUST BE OF THE SAME TYPE
            DefaultPlaceRequest copy = new DefaultPlaceRequest(perspectiveName);

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

    private List<String> getScreensFromUrl(final String request, final Boolean opened) {
        List result = new ArrayList<>();

        if (isNotBlank(request)) {

            // FIXME make it robust! this is for controlled testing only
            String url = request.substring(request.indexOf(PERSPECTIVE_SEP) + 1);
            String[] screens = url.split(SCREEN_SEP);

            for (String screen : screens) {
                if (((!opened) && screen.startsWith(NEGATION_PREFIX))
                        || (opened && !screen.startsWith(NEGATION_PREFIX))) {
                    if (!opened) {
                        result.add(screen.substring(1));
                    }
                    else {
                        result.add(screen);
                    }
                }
            }
        }
        return result;
    }

    public List<String> getClosedScreenFromUrl(final PlaceRequest req) {
        final String url = URL.decode(req.getFullIdentifier());

        return getScreensFromUrl(url, false);
    }

    public List<String> getOpenedScreenFromUrl(final PlaceRequest req) {
        final String url = URL.decode(req.getFullIdentifier());

        return getScreensFromUrl(url, true);
    }

    public void registerClose(Activity activity,
                              PlaceRequest place,
                              boolean isDock) {
        final String id = isDock ? DOCK_PREFIX.concat(place.getIdentifier())
                : place.getIdentifier();

        if (activity.isType(ActivityResourceType.SCREEN.name())) {
            final String token = getUrlToken(id);

            removeScreenFromUrl(token);
        }
        // update historyUrl
        onPlaceChange(place);
    }

    public void flush() {
//        GWT.log("~~ flush ~~");
        historyUrl = "";
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
