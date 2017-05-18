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
     * Needed for testing
     * @param place
     * @return
     */
    public PlaceRequest getPerspectiveFromPlace(final PlaceRequest place)
    {
        return BookmarkableUrlHelper.getPerspectiveFromPlace(place);
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
            if (!BookmarkableUrlHelper.isPerspectiveInUrl(bookmarkableUrl)) {
                bookmarkableUrl = id.concat(PERSPECTIVE_SEP).concat(bookmarkableUrl);
            }
        } else if (activity.isType(ActivityResourceType.SCREEN.name())) {
            // add the dock marker if needed
            id = isDock ? DOCK_PREFIX.concat(place.getFullIdentifier())
                    : place.getFullIdentifier();
            // add screen to the bookmarkableUrl
            bookmarkableUrl =
                    BookmarkableUrlHelper.registerOpenedScreen(bookmarkableUrl, id);
        }
        onPlaceChange(place);
    }

    public void registerClose(Activity activity,
                              PlaceRequest place,
                              boolean isDock) {

        GWT.log("close activity: " + place.getIdentifier());

        final String id = isDock ? DOCK_PREFIX.concat(place.getIdentifier())
                : place.getIdentifier();

        if (activity.isType(ActivityResourceType.SCREEN.name())) {
            final String token = BookmarkableUrlHelper.getUrlToken(bookmarkableUrl,
                                                                   id);

            bookmarkableUrl =
                    BookmarkableUrlHelper.registerClosedScreen(bookmarkableUrl,
                                                                         token);
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
