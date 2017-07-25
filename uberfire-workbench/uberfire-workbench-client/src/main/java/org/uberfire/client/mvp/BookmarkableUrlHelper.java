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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gwt.core.shared.GWT;
import org.uberfire.client.workbench.docks.UberfireDock;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.mvp.impl.PathPlaceRequest;

/**
 * A bookmarkable URL has the following form:
 * <p>
 * http://url/index.html#FWidgets|$PagedTableScreen[WSimpleDockScreen,],~WiresPropertiesScreen$PagedTableScreen
 * <p>
 * between the '#' and '|' there is the perspective name
 * between the '|' and '$' there is the CSV list of the screens opened when loading the perspective
 * between the '[' and ']'there is the CSV list of the docked Screens
 * after the '$' there is the CSV list of the screens not belonging to the current perspective
 * <p>
 * '~' denotes a closed screen
 * <p>
 * In this unit we have the basic methods used to compose such URLs
 */
public class BookmarkableUrlHelper {

    public final static String PERSPECTIVE_SEP = "|";
    public final static String DOCK_BEGIN_SEP = "[";
    public final static String DOCK_CLOSE_SEP = "]";
    public final static String SEPARATOR = ",";
    public final static String OTHER_SCREEN_SEP = "$";
    public final static String CLOSED_PREFIX = "~";
    public final static String CLOSED_DOCK_PREFIX = "!";
    public final static int MAX_NAV_URL_SIZE = 1900;

    private static boolean isNotBlank(final String str) {
        return (str != null
                && str.trim().length() > 0);
    }

    private static boolean isNotBlank(final PlaceRequest place) {
        return (null != place && isNotBlank(place.getFullIdentifier()));
    }

    /**
     * Add a screen to the bookmarkable URL. If the screen belongs to the currently opened
     * perspective we add it to the list between the '|' and '$', otherwise we add it
     * after the '$'.
     * <p>
     * We add the '|' or the '$' when needed
     * @param bookmarkableUrl
     * @param placeRequest
     * @return
     */
    public static String registerOpenedScreen(String bookmarkableUrl,
                                              final PlaceRequest placeRequest) {
        String screenName = placeRequest.getFullIdentifier();
        String closedScreen = CLOSED_PREFIX.concat(screenName);
        final String currentBookmarkableUrl = bookmarkableUrl;

        if (screenWasClosed(bookmarkableUrl,
                            closedScreen)) {
            bookmarkableUrl = bookmarkableUrl.replace(closedScreen,
                                                      screenName);
        } else if (!isPerspectiveInUrl(bookmarkableUrl)) {
            // must add the screen in the group of the current perspective (which is not yet loaded)
            if (isNotBlank(bookmarkableUrl)) {
                bookmarkableUrl = bookmarkableUrl.concat(SEPARATOR).concat(screenName);
            } else {
                bookmarkableUrl = screenName;
            }
        } else {
            // this is a screen outside the current perspective
            if (!urlContainsExtraPerspectiveScreen(bookmarkableUrl)) {
                bookmarkableUrl = bookmarkableUrl.concat(OTHER_SCREEN_SEP).concat(screenName);
            } else {
                bookmarkableUrl = bookmarkableUrl.concat(SEPARATOR).concat(screenName);
            }
        }
        if (isBiggerThenMaxURLSize(bookmarkableUrl)) {
            return currentBookmarkableUrl;
        }
        return bookmarkableUrl;
    }

    private static boolean screenWasClosed(String bookmarkableUrl,
                                           String closedScreen) {
        return bookmarkableUrl.indexOf(closedScreen) != -1;
    }

    private static boolean isBiggerThenMaxURLSize(String bookmarkableUrl) {
        return isNotBlank(bookmarkableUrl) &&
                bookmarkableUrl.length() >= MAX_NAV_URL_SIZE;
    }

    /**
     * Update the bookmarkable URL, marking a screen or editor closed. Basically if the screen belongs
     * to the currently opened perspective the we prefix the screen with a '~'; if the
     * screen doesn't belong to the current perspective, that is, after the '$', the it
     * is simply removed.
     * <p>
     * We remove the '$' when needed
     * @param screenName
     */
    public static String registerClose(String bookmarkableUrl,
                                       final String screenName) {
        final boolean isPerspective = isPerspectiveScreen(bookmarkableUrl,
                                                          screenName);
        final String separator = isPerspective ? PERSPECTIVE_SEP : OTHER_SCREEN_SEP;
        final String closedScreen = CLOSED_PREFIX.concat(screenName);
        final String uniqueScreenAfterDelimiter =
                separator.concat(screenName); // |screen or $screen
        final String firstScreenAfterDelimiter =
                uniqueScreenAfterDelimiter.concat(SEPARATOR); // |screen, or $screen,
        final String commaSeparatedScreen =
                screenName.concat(SEPARATOR); // screen,

        if (isScreenClosed(bookmarkableUrl,
                           closedScreen)) {
            return bookmarkableUrl;
        }
        if (isPerspective) {
            bookmarkableUrl = bookmarkableUrl.replace(screenName,
                                                      closedScreen);
        } else {
            if (bookmarkableUrl.contains(firstScreenAfterDelimiter)) {
                bookmarkableUrl = bookmarkableUrl.replace(firstScreenAfterDelimiter,
                                                          separator);
            } else if (bookmarkableUrl.contains(uniqueScreenAfterDelimiter)) {
                bookmarkableUrl = bookmarkableUrl.replace(uniqueScreenAfterDelimiter,
                                                          "");
            } else if (bookmarkableUrl.contains(commaSeparatedScreen)) {
                bookmarkableUrl = bookmarkableUrl.replace(commaSeparatedScreen,
                                                          "");
            }
        }
        return bookmarkableUrl;
    }

    /**
     * Get the perspective from a bookmarkable URL
     * @param url
     * @return
     */
    public static PlaceRequest getPerspectiveFromUrl(final String url) {
        PlaceRequest place = null;

        if (isNotBlank(url)) {
            if (isPerspectiveInUrl(url)) {
                // standard case, full bookmarkable URL
                String perspectiveName = url.substring(0,
                                                       url.indexOf(PERSPECTIVE_SEP));
                place = new DefaultPlaceRequest(perspectiveName);
            } else if (isValidScreen(url)) {
                // just in case there is ONLY one screen id in the URL
                place = new DefaultPlaceRequest(url);
            }
        }
        return place;
    }

    /**
     * Check whether the screen belongs to the currently opened perspective
     * @param screen
     * @return
     */
    public static boolean isPerspectiveScreen(final String bookmarkableUrl,
                                              final String screen) {
        return (isNotBlank(screen)
                && isNotBlank(bookmarkableUrl)
                && (!urlContainsExtraPerspectiveScreen(bookmarkableUrl)
                || (bookmarkableUrl.indexOf(OTHER_SCREEN_SEP) > bookmarkableUrl.indexOf(screen))));
    }

    /**
     * Returns true if the perspective is present in the URL
     * @return
     */
    public static boolean isPerspectiveInUrl(final String url) {
        return (isNotBlank(url) && (url.indexOf(PERSPECTIVE_SEP) != -1));
    }

    /**
     * Check if the URL contains screens not belonging to the current perspective
     * @return
     */
    public static boolean urlContainsExtraPerspectiveScreen(final String bookmarkableUrl) {
        return (bookmarkableUrl.indexOf(OTHER_SCREEN_SEP) != -1);
    }

    /**
     * Given a screen name, this method extracts the corresponding token in the
     * URL, that is the screen name with optional parameters and markers
     * @param screen
     * @return
     */
    public static String getUrlToken(final String bookmarkableUrl,
                                     final String screen) {

        int st = isPerspectiveInUrl(bookmarkableUrl) ?
                (bookmarkableUrl.indexOf(PERSPECTIVE_SEP) + 1) : 0;
        String screensList = bookmarkableUrl.replace(OTHER_SCREEN_SEP,
                                                     SEPARATOR)
                .substring(st,
                           bookmarkableUrl.length());

        String tokens[] = screensList.split(SEPARATOR);
        Optional<String> token = Arrays.stream(tokens)
                .filter(s -> s.contains(screen))
                .findFirst();

        return token.orElse(screen);
    }

    /**
     * Return the docked screens in the URL
     * @param url
     * @return
     */
    public static Set<String> getDockedScreensFromUrl(final String url) {
        int start;
        int end;
        String docks;

        if (!isNotBlank(url)) {
            return new HashSet<>();
        }
        start = url.indexOf(DOCK_BEGIN_SEP) + 1;
        end = url.indexOf(DOCK_CLOSE_SEP) - 1;

        if (start > 0) {
            docks = url.substring(start,
                                  end);
            String[] token = docks.split(SEPARATOR);
            return new HashSet<>(Arrays.asList(token));
        }
        return new HashSet<>();
    }

    /**
     * Return all the screens (opened or closed) that is, everything
     * after the perspective declaration
     * @param url
     * @return
     */
    public static Set<String> getScreensFromUrl(String url) {
        HashSet<String> result = new HashSet<>();
        String docks;
        int start;
        int end;

        if (!isNotBlank(url)) {
            return new HashSet<>();
        }
        // get everything after the perspective
        if (isPerspectiveInUrl(url)) {
            url = url.substring(url.indexOf(PERSPECTIVE_SEP) + 1);
        }
        // get the docks and the screens list
        start = url.indexOf(DOCK_BEGIN_SEP);
        end = url.indexOf(DOCK_CLOSE_SEP) + 1;
        if (start > -1) {
            docks = url.substring(start,
                                  end);
            url = url.replace(docks,
                              "");
        }
        if (null != url
                && !url.trim().equals("")) {
            // replace the '$' with a comma ','
            url = url.replace(OTHER_SCREEN_SEP,
                              SEPARATOR);
            String[] token = url.split(SEPARATOR);
            result = new HashSet<>(Arrays.asList(token));
        }
        return result;
    }

    /**
     * Return true if the given screen is already closed.
     * @param screen
     * @return
     * @note docked screens are ignored
     */
    public static boolean isScreenClosed(final String bookmarkableUrl,
                                         String screen) {
        if (!screen.startsWith(CLOSED_PREFIX)) {
            screen = CLOSED_PREFIX.concat(screen);
        }
        return (bookmarkableUrl.indexOf(screen) != -1);
    }

    public static String registerOpenedPerspective(String currentBookmarkableURLStatus,
                                                   PlaceRequest place) {
        return place.getFullIdentifier().concat(PERSPECTIVE_SEP).concat(currentBookmarkableURLStatus);
    }

    private static String getDockId(UberfireDock targetDock) {
        return targetDock.getDockPosition().getShortName()
                + targetDock.getPlaceRequest().getFullIdentifier() + SEPARATOR;
    }

    /**
     * @param currentBookmarkableURLStatus
     * @param targetDock
     * @return
     */
    public static String registerOpenedDock(String currentBookmarkableURLStatus,
                                            UberfireDock targetDock) {
        if (targetDock == null) {
            return currentBookmarkableURLStatus;
        }
        final String id = getDockId(targetDock);
        final String closed = CLOSED_DOCK_PREFIX.concat(id);

        if (currentBookmarkableURLStatus.contains(DOCK_CLOSE_SEP)) {
            String result = null;

            if (!currentBookmarkableURLStatus.contains(id)) {
                // the screen is not in the URL, insert in last position
                result = currentBookmarkableURLStatus.replace(DOCK_CLOSE_SEP,
                                                              (id + DOCK_CLOSE_SEP));
            } else if (currentBookmarkableURLStatus.contains(closed)) {
                // the screen is closed
                result = currentBookmarkableURLStatus.replace(closed,
                                                              id);
            } else {
                // screen already in URL
                result = currentBookmarkableURLStatus;
            }
            return result;
        } else {
            return currentBookmarkableURLStatus + DOCK_BEGIN_SEP + (getDockId(targetDock) + DOCK_CLOSE_SEP);
        }
    }

    /**
     * @param currentBookmarkableURLStatus
     * @param targetDock
     * @return
     */
    public static String registerClosedDock(String currentBookmarkableURLStatus,
                                            UberfireDock targetDock) {
        if (!isNotBlank(currentBookmarkableURLStatus)
                || null == targetDock) {
            return currentBookmarkableURLStatus;
        }
        final String id = getDockId(targetDock);
        final String closed = CLOSED_DOCK_PREFIX.concat(id);
        if (!currentBookmarkableURLStatus.contains(closed)) {
            return currentBookmarkableURLStatus.replace(id,
                                                        CLOSED_DOCK_PREFIX.concat(id));
        }
        return currentBookmarkableURLStatus;
    }

    /**
     * Remove the editor reference from the URL
     * @param currentBookmarkableURLStatus
     * @param place
     * @return
     */
    public static String registerCloseEditor(final String currentBookmarkableURLStatus,
                                             final PlaceRequest place) {
        if (place != null
                && place instanceof PathPlaceRequest) {
            final String[] fullIdentifier = {place.getFullIdentifier()};

            ((PathPlaceRequest) place).getParameters().entrySet()
                    .forEach(c -> {
                        final String kv = c.getKey()
                                .concat("=")
                                .concat(c.getValue());
                        final String nkv = c.getKey()
                                .concat("==")
                                .concat(c.getValue());
                        fullIdentifier[0] =
                                fullIdentifier[0].replace(kv,
                                                          nkv);
                    });
            final String path = fullIdentifier[0];
            final String pathWithSep = path.concat(SEPARATOR);

            if (currentBookmarkableURLStatus.contains(pathWithSep)) {
                return currentBookmarkableURLStatus.replace(pathWithSep,
                                                            "");
            }
            return currentBookmarkableURLStatus.replace(path,
                                                        "");
        }
        return currentBookmarkableURLStatus;
    }

    /**
     * Prepend a double equal sign '==' in front of the URL of a PathPlaceRequest and
     * related arguments
     * @param bookmarkableUrl
     * @param placeRequest
     * @return
     */
    public static String registerOpenedEditor(String bookmarkableUrl,
                                              PathPlaceRequest placeRequest) {
        if (placeRequest == null) {
            return bookmarkableUrl;
        }
        final String originalPathRequest = placeRequest.getFullIdentifier();
        String effectiveRequest = originalPathRequest;
        for (String arg : placeRequest.getParameters().keySet()) {
            final String value = placeRequest.getParameter(arg,
                                                           "");
            final String kv = arg.concat("=").concat(value);
            final String nkv = arg.concat("==").concat(value);

            effectiveRequest = effectiveRequest.replace(kv,
                                                        nkv);
        }
        return bookmarkableUrl.replace(originalPathRequest,
                                       effectiveRequest);
    }

    /**
     * Given a token it copies from the beginning to the last comma or
     * ampersend
     * @param token
     * @return
     */
    private static String extractUntilLastDelimiter(final String token) {
        if (token != null
                && (token.indexOf(',') != -1
                || token.indexOf('&') != -1)) {
            int comma = token.lastIndexOf(',');
            int ampersend = token.lastIndexOf('&');
            if (comma != -1
                    && ampersend != -1) {
                return token.substring(0,
                                       comma > ampersend ? comma : ampersend);
            }
        }
        return token;
    }

    /**
     * Given the token of a PathPlaceRequest extract the URI and the related parameters
     * @param token
     * @param map
     */
    private static void preparePathPlaceRequestInvocation(final String token,
                                                          Map<String, Map<String, String>> map) {
        Map<String, String> arguments = new HashMap<String, String>();

        // take the URI (everything from the '==' to the first '&')
        final String uri = token.indexOf('&') != -1 ?
                token.substring(1,
                                token.indexOf('&')) : token.substring(1,
                                                                      (token.length() - 1));
        String[] args = token.split("&");
        Arrays.stream(args).forEach(arg -> {
            if (arg.contains("==")) {
                String[] kv = arg.split("==");

                arguments.put(kv[0],
                              kv[1]);
            }
            if (arg.contains(PathPlaceRequest.FILE_NAME_MARKER)) {
                String[] kv = arg.split("=");

                arguments.put(kv[0],
                              kv[1]);
            }
        });
        map.put(uri,
                arguments);
    }

    /**
     * Get the map of the PathPlaceRequest in a bookmarkable URL with the
     * associated parameters
     * @param bookmarkableUrl
     * @return
     */
    public static Map<String, Map<String, String>> getOpenedEditorsFromUrl(final String bookmarkableUrl) {
        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();

        String[] paths = bookmarkableUrl.split(PathPlaceRequest.PATH_URI_MARKER);
        for (String path : paths) {
            if (path.contains("=")) {
                // note: splitting by 'path_uri' requires  truncate to the last comma
                preparePathPlaceRequestInvocation(
                        extractUntilLastDelimiter(path),
                        result);
            }
        }
        return result;
    }

    /**
     * Make sure that the screen we are about to open is a valid screen name
     * Marker for 'Closed' are allowed
     * @param screen
     * @return
     */
    public static boolean isValidScreen(final String screen) {
        return (null != screen
                && !screen.trim().equals("")
                && !screen.contains(PathPlaceRequest.PATH_URI_MARKER)
                && !screen.contains("=")
                && !screen.contains(BookmarkableUrlHelper.SEPARATOR)
                && !screen.contains("&")
                && !screen.contains(BookmarkableUrlHelper.DOCK_BEGIN_SEP)
                && !screen.contains(BookmarkableUrlHelper.DOCK_CLOSE_SEP)
                && !screen.contains(BookmarkableUrlHelper.OTHER_SCREEN_SEP)
                && !screen.contains(BookmarkableUrlHelper.PERSPECTIVE_SEP)
        );
    }
}
