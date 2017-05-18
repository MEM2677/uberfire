package org.uberfire.client.mvp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.uberfire.mvp.PlaceRequest;

/**
 * A bookmarkable URL has the following form:
 * <p>
 * http://127.0.0.1:8888/wires.html#WiresScratchPadPerspective|~WiresScratchPadScreen,WiresLayersScreen,~WiresActionsScreen,WiresPaletteScreen,~WiresPropertiesScreen$PagedTableScreen
 * <p>
 * between the '#' and '|' there is the perspective name
 * between the '|' and '$' there is the CSV list of the screens opened when loading the perspective
 * after the '$' there is the CSV list of the screens not belonging to the current perspective
 * <p>
 * '~' denotes a cosed screen
 * '!' indicates a docked screen
 *
 * In this unit we have the basic methods used to compose such URLs
 *
 */
public class BookmarkableUrlHelper {

    private static boolean isNotBlank(final String str) {
        return (str != null
                && str.trim().length() > 0);
    }
    private static boolean isNotBlank(final PlaceRequest place)
    {
        return (null != place && isNotBlank(place.getFullIdentifier()));
    }

    /**
     * Add a screen to the bookmarkable URL. If the screen belongs to the currently opened
     * perspective we add it to the list between the '|' and '$', otherwise we add it
     * after the '$'.
     *
     * We add the '|' or the '$' when needed
     * @param bookmarkableUrl
     * @param screenName
     * @return
     */
    public static String registerOpenedScreen(String bookmarkableUrl,
                                              final String screenName) {
        String closedScreen = CLOSED_PREFIX.concat(screenName);

        // if the length exceeds the max allowed size do nothing
        if (isNotBlank(screenName) && isNotBlank(bookmarkableUrl) &&
                bookmarkableUrl.length() + screenName.length() >= MAX_NAV_URL_SIZE) {
            return bookmarkableUrl;
        }

        // if the screen was closed
        if (bookmarkableUrl.indexOf(closedScreen) != -1) {
            bookmarkableUrl = bookmarkableUrl.replace(closedScreen,
                                                      screenName);
        } else if (bookmarkableUrl.indexOf(screenName) > 0) {
            // do nothing coz the screen is already present
        } else if (!isPerspectiveInUrl(bookmarkableUrl)) {
            // must add the screen in the group of the current perspective (which is not yet loaded)
            if (isNotBlank(bookmarkableUrl)) {
                bookmarkableUrl = bookmarkableUrl.concat(SCREEN_SEP).concat(screenName);
            } else {
                bookmarkableUrl = screenName;
            }
        } else {
            // this is a screen outside the current perspective
            if (!urlContainsExtraPerspectiveScreen(bookmarkableUrl)) {
                // add the '$' if needed
                bookmarkableUrl = bookmarkableUrl.concat(OTHER_SCREEN_SEP).concat(screenName);
            }
            // otherwise append the screen after the last element
            else {
                bookmarkableUrl = bookmarkableUrl.concat(SCREEN_SEP).concat(screenName);
            }
        }
        return bookmarkableUrl;
    }

    /**
     * Update the bookmarkable URL, marking a screen closed. Basically if the screen belongs
     * to the currently opened perspective the we prefix the screen with a '~'; if the
     * screen doesn't belong to the current perspective, that is, after the '$', the it
     * is simply removed.
     *
     * We remove the '$' when needed
     * @param screenName
     */
    public static String registerClosedScreen(String bookmarkableUrl, final String screenName) {
        final boolean isPerspective = isPerspectiveScreen(bookmarkableUrl, screenName);
        final String separator = isPerspective ? PERSPECTIVE_SEP : OTHER_SCREEN_SEP;
        final String closedScreen = CLOSED_PREFIX.concat(screenName);
        final String uniqueScreenAfterDelimiter =
                separator.concat(screenName); // |screen or $screen
        final String firstScreenAfterDelimiter =
                uniqueScreenAfterDelimiter.concat(SCREEN_SEP); // |screen, or $screen,
        final String commaSeparatedScreen =
                screenName.concat(SCREEN_SEP); // screen,

        // check screen already closed
        if (isScreenClosed(bookmarkableUrl, closedScreen)) {
            return bookmarkableUrl;
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
        return bookmarkableUrl;
    }


    /**
     * Check whether the screen belongs to the currently opened perspective
     * @param screen
     * @return
     */
    public static boolean isPerspectiveScreen(final String bookmarkableUrl,
                                        final String screen) {
        return (isNotBlank(screen)
                && (!urlContainsExtraPerspectiveScreen(bookmarkableUrl)
                || (bookmarkableUrl.indexOf(OTHER_SCREEN_SEP) > bookmarkableUrl.indexOf(screen))));
    }

    /**
     * Returns true if the perspective is present in the URL
     * @return
     */
    public static boolean isPerspectiveInUrl(final String url) {
        return (isNotBlank(url) && (url.indexOf(PERSPECTIVE_SEP) > 0));
    }

    /**
     * Check if the URL contains screens not belonging to the current perspective
     * @return
     */
    public static boolean urlContainsExtraPerspectiveScreen(final String bookmarkableUrl) {
        return (bookmarkableUrl.indexOf(OTHER_SCREEN_SEP) != -1);
    }

    /**
     * Return true if the given screen is already closed.
     * @param screen
     * @return
     */
    public static boolean isScreenClosed(final String bookmarkableUrl,
                                   String screen)
    {
        if (!screen.startsWith(CLOSED_PREFIX))
        {
            screen = CLOSED_PREFIX.concat(screen);
        }
        return (bookmarkableUrl.indexOf(screen) != -1);
    }

    /**
     * Given a screen name, this method extracts the corresponding token in the
     * URL, that is the screen name with optional parameters and markers
     * @param screen
     * @return
     */
    public static String getUrlToken(final String bookmarkableUrl,
                               final String screen) {
        int st = isPerspectiveInUrl(bookmarkableUrl) ? (bookmarkableUrl.indexOf(PERSPECTIVE_SEP) + 1) : 0;
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
     * Return all the screens (opened or closed) that is, everything
     * after the perspective declaration
     * @param place
     * @return
     */
    public static Set<String> getScreensFromPlace(final PlaceRequest place) {
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
    public static Set<String> getClosedScreenFromPlace(final PlaceRequest place) {
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
    public static Set<String> getOpenedScreenFromPlace(final PlaceRequest place) {
        Set<String> screens = getScreensFromPlace(place);
        Set<String> result = screens.stream()
                .filter(s -> !s.startsWith(CLOSED_PREFIX))
                .collect(Collectors.toSet());
        return result;
    }


    public final static String PERSPECTIVE_SEP = "|";
    public final static String SCREEN_SEP = ",";
    public final static String OTHER_SCREEN_SEP = "$";
    public final static String CLOSED_PREFIX = "~";
    public final static String DOCK_PREFIX = "!";

    public final static int MAX_NAV_URL_SIZE = 1900;
}
