package org.uberfire.client.workbench.panels.impl;

import java.util.Set;

import junit.framework.TestCase;
import org.junit.Test;
import org.uberfire.client.mvp.BookmarkableUrlHelper;
import org.uberfire.client.workbench.docks.UberfireDock;
import org.uberfire.client.workbench.docks.UberfireDockPosition;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;

/**
 * Created by matteo on 24/05/17.
 */
public class BookmarkableUrlHelperTest extends TestCase {

    @Test
    public void testRegisterOpen() {
        PlaceRequest req1 = new DefaultPlaceRequest("screen1");
        PlaceRequest req2 = new DefaultPlaceRequest("screen2");
        PlaceRequest req3 = new DefaultPlaceRequest("screen3");
        PlaceRequest req4 = new DefaultPlaceRequest("screen4");
        final String perspective = "perspective";
        String url = "";

        url = BookmarkableUrlHelper.registerOpenedScreen(url,
                                                         req1);
        assertEquals(req1.getFullIdentifier(),
                     url);
        url = BookmarkableUrlHelper.registerOpenedScreen(url,
                                                         req2);
        assertEquals("screen1,screen2",
                     url);

        // add the perspective, want to test screen not belonging to the current perspective
        url = perspective.concat(BookmarkableUrlHelper.PERSPECTIVE_SEP)
                .concat(url);
        url = BookmarkableUrlHelper.registerOpenedScreen(url,
                                                         req3);

        assertEquals("perspective|screen1,screen2$screen3",
                     url);

        url = BookmarkableUrlHelper.registerOpenedScreen(url,
                                                         req4);
        assertEquals("perspective|screen1,screen2$screen3,screen4",
                     url);
    }

    @Test
    public void testRegisterClose() {
        String url = "perspective|screen1,screen2$screen3,screen4";

        // close screens not belonging to the current perspective
        url = BookmarkableUrlHelper.registerClose(url,
                                                  "screen3");
        assertEquals("perspective|screen1,screen2$screen4",
                     url);
        url = BookmarkableUrlHelper.registerClose(url,
                                                  "screen4");
        assertEquals("perspective|screen1,screen2",
                     url);

        // close screens belonging to the current perspective

        url = BookmarkableUrlHelper.registerClose(url,
                                                  "screen1");
        assertEquals("perspective|~screen1,screen2",
                     url);

        url = BookmarkableUrlHelper.registerClose(url,
                                                  "screen2");
        assertEquals("perspective|~screen1,~screen2",
                     url);
    }

    @Test
    public void testGetPerspectiveFromPlace() {
        final String perspectiveName = "eccePerspective";
        final String bookmarkableUrl = perspectiveName
                .concat("|~screen1,~screen2");
        final PlaceRequest req = new DefaultPlaceRequest(bookmarkableUrl);

        PlaceRequest place = BookmarkableUrlHelper.getPerspectiveFromPlace(req);

        assertNotNull(place);
        assertNotSame(req,
                      place);
        assertEquals(perspectiveName,
                     place.getFullIdentifier());
    }

    @Test
    public void testGetPerspectiveFromPlaceWithParams() {
        final String perspectiveName = "eccePerspective";
        final String bookmarkableUrl = perspectiveName
                .concat("|~screen1,~screen2");
        final PlaceRequest req = new DefaultPlaceRequest(bookmarkableUrl);

        req.addParameter("param", "value");
        PlaceRequest place = BookmarkableUrlHelper.getPerspectiveFromPlace(req);

        assertNotNull(place);
        assertNotSame(req,
                      place);
        StringBuilder expected = new StringBuilder(perspectiveName);
        expected.append("?param=value");
        assertEquals(expected.toString(),
                     place.getFullIdentifier());
    }

    @Test
    public void testIsPerspectiveScreen() {
        final String url = "perspective|screen1,screen2$screen3,screen4";

        assertTrue(BookmarkableUrlHelper.isPerspectiveScreen(url,
                                                             "screen1"));
        assertTrue(BookmarkableUrlHelper.isPerspectiveScreen(url,
                                                             "screen2"));
        assertFalse(BookmarkableUrlHelper.isPerspectiveScreen(url,
                                                              "screen3"));
        assertFalse(BookmarkableUrlHelper.isPerspectiveScreen(url,
                                                              "screen4"));
    }

    @Test
    public void testIsPerspectiveInUrl() {
        final String url1 = "perspective|screen1,screen2$screen3,screen4";
        final String url2 = "screen1,screen2";
        final String url3 = "perspective|screen1,screen2$screen3,screen4";

        assertTrue(BookmarkableUrlHelper.isPerspectiveInUrl(url1));
        assertFalse(BookmarkableUrlHelper.isPerspectiveInUrl(url2));
        assertTrue(BookmarkableUrlHelper.isPerspectiveInUrl(url3));
    }

    @Test
    public void testUrlContainsExtraPerspectiveScreen() {
        final String url1 = "perspective|screen1,screen2$screen3,screen4";
        final String url2 = "screen1,screen2";
        final String url3 = "perspective|screen1,screen2$screen3,screen4";

        assertTrue(BookmarkableUrlHelper.urlContainsExtraPerspectiveScreen(url1));
        assertFalse(BookmarkableUrlHelper.urlContainsExtraPerspectiveScreen(url2));
        assertTrue(BookmarkableUrlHelper.urlContainsExtraPerspectiveScreen(url3));
    }

    @Test
    public void testGetUrlInToken() {
        final String url1 = "perspective|#screen1,§screen2$#screen3,!screen4";
        final String url2 = "!screen1,#screen2";

        assertEquals("!screen1",
                     BookmarkableUrlHelper.getUrlToken(url2,
                                                       "screen1"));
        assertEquals("#screen2",
                     BookmarkableUrlHelper.getUrlToken(url2,
                                                       "screen2"));

        assertEquals("§screen2",
                     BookmarkableUrlHelper.getUrlToken(url1,
                                                       "screen2"));
        assertEquals("#screen1",
                     BookmarkableUrlHelper.getUrlToken(url1,
                                                       "screen1"));
        assertEquals("#screen3",
                     BookmarkableUrlHelper.getUrlToken(url1,
                                                       "screen3"));
        assertEquals("!screen4",
                     BookmarkableUrlHelper.getUrlToken(url1,
                                                       "screen4"));
    }

    @Test
    public void testGetScreensFromPlace() {
        final String url = "perspective|~screen1,screen2$!screen3,screen4";
        final String url2 = "UFWidgets|PagedTableScreen[ESimpleDockScreen,!WSimpleDockScreen,ESimpleDockScreen,]";
        final PlaceRequest place = new DefaultPlaceRequest(url);
        final PlaceRequest place2 = new DefaultPlaceRequest(url2);

        Set<String> set = BookmarkableUrlHelper.getScreensFromPlace(place);
        assertNotNull(set);
        assertFalse(set.isEmpty());

        assertEquals(4,
                     set.size());
        assertTrue(set.contains("~screen1"));
        assertTrue(set.contains("screen2"));
        assertTrue(set.contains("!screen3"));
        assertTrue(set.contains("screen4"));

        set = BookmarkableUrlHelper.getScreensFromPlace(place2);
        assertNotNull(set);

        assertFalse(set.isEmpty());
        assertEquals(1,
                     set.size());
        assertTrue(set.contains("PagedTableScreen"));
    }

    @Test
    public void testGetClosedScreenFromPlace() {
        final String url = "perspective|~screen1,screen2$~screen3,screen4";
        final String url2 = "UFWidgets|PagedTableScreen[ESimpleDockScreen,!WSimpleDockScreen,ESimpleDockScreen,]";
        final PlaceRequest place = new DefaultPlaceRequest(url);
        final PlaceRequest place2 = new DefaultPlaceRequest(url2);

        Set<String> set = BookmarkableUrlHelper.getClosedScreenFromPlace(place);
        assertNotNull(set);
        assertFalse(set.isEmpty());
        assertEquals(2,
                     set.size());
        assertTrue(set.contains("~screen1"));
        assertTrue(set.contains("~screen3"));

        set = BookmarkableUrlHelper.getClosedScreenFromPlace(place2);
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testGetOpenedScreenFromPlace() {
        final String url = "perspective|~screen1,screen2$~screen3,screen4";
        final String url2 = "UFWidgets|PagedTableScreen[ESimpleDockScreen,!WSimpleDockScreen,ESimpleDockScreen,]";
        final PlaceRequest place = new DefaultPlaceRequest(url);
        final PlaceRequest place2 = new DefaultPlaceRequest(url2);

        Set<String> set = BookmarkableUrlHelper.getOpenedScreenFromPlace(place);
        assertNotNull(set);
        assertFalse(set.isEmpty());
        assertTrue(set.contains("screen2"));
        assertTrue(set.contains("screen4"));

        set = BookmarkableUrlHelper.getOpenedScreenFromPlace(place2);
        assertNotNull(set);
        assertFalse(set.isEmpty());
        assertTrue(set.contains("PagedTableScreen"));
    }

    public void testGDockedScreensFromPlace() {
        final String url = "perspective|~screen1,screen2$~screen3,screen4";
        final String url2 = "UFWidgets|PagedTableScreen[ESimpleDockScreen,!WSimpleDockScreen,ESimpleDockScreen,]";
        final PlaceRequest place = new DefaultPlaceRequest(url);
        final PlaceRequest place2 = new DefaultPlaceRequest(url2);

        Set<String> set = BookmarkableUrlHelper.getDockedScreensFromPlace(place);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        set = BookmarkableUrlHelper.getDockedScreensFromPlace(place2);
        assertNotNull(set);
        assertFalse(set.isEmpty());
        assertTrue(set.contains("ESimpleDockScreen"));
        assertTrue(set.contains("!WSimpleDockScreen"));
    }

    public void testIsScreenClosed() {
        final String url = "perspective|~screen1,screen2$~screen3,screen4";
        final String url2 = "UFWidgets|PagedTableScreen[ESimpleDockScreen,!WSimpleDockScreen,ESimpleDockScreen,]";

        assertTrue(BookmarkableUrlHelper.isScreenClosed(
                url,
                "screen1"));
        assertTrue(BookmarkableUrlHelper.isScreenClosed(
                url,
                "screen3"));
        assertFalse(BookmarkableUrlHelper.isScreenClosed(
                url,
                "screen2"));
        assertFalse(BookmarkableUrlHelper.isScreenClosed(
                url,
                "screen4"));

        // docked screens are ignored
        assertFalse(BookmarkableUrlHelper.isScreenClosed(
                url2,
                "PagedTableScreen"));
        assertFalse(BookmarkableUrlHelper.isScreenClosed(
                url2,
                "ESimpleDockScreen"));
        assertFalse(BookmarkableUrlHelper.isScreenClosed(
                url2,
                "ESimpleDockScreen"));
        assertFalse(BookmarkableUrlHelper.isScreenClosed(
                url2,
                "!WSimpleDockScreen"));
    }

    public void testRegisterOpenedPerspective() {
        final String screens = "screen1,~screen2";
        final String perspective = "perspective";
        final PlaceRequest place = new DefaultPlaceRequest(perspective);
        String url = screens;

        url = BookmarkableUrlHelper.registerOpenedPerspective(url,
                                                              place);

        assertEquals(perspective.concat(BookmarkableUrlHelper.PERSPECTIVE_SEP).concat(screens),
                     url);
    }

    public void testRegisterOpenDock() {
        final String screens = "screen1";
        final String dockName = "dockedScreen";
        final String perspectiveName = "perspectiveName";
        String url = perspectiveName
                .concat(BookmarkableUrlHelper.PERSPECTIVE_SEP)
                .concat(screens);
        final UberfireDock dock1 = createUberfireDockForTest(dockName,
                                                             perspectiveName);
        final UberfireDock dock2 = createUberfireDockForTest(dockName.concat("New"),
                                                             perspectiveName);

        url = BookmarkableUrlHelper.registerOpenedDock(url,
                                                       dock1);
        assertEquals(perspectiveName
                             .concat(BookmarkableUrlHelper.PERSPECTIVE_SEP)
                             .concat("screen1[WdockedScreen,]"),
                     url);
        url = BookmarkableUrlHelper.registerOpenedDock(url,
                                                       dock2);
        assertEquals(perspectiveName
                             .concat(BookmarkableUrlHelper.PERSPECTIVE_SEP)
                             .concat("screen1[WdockedScreen,WdockedScreenNew,]"),
                     url);

    }


    public void testRegisterClosedDock() {
        final String dockName1 = "dockedScreen1";
        final String dockName2 = "dockedScreen2";
        String perspectiveName = "perspective";
        String url = "perspectiveName|screen[W" + dockName1 + ",W" + dockName2 + ",]";
        UberfireDock dock1 = createUberfireDockForTest(dockName1,
                                                      perspectiveName);
        UberfireDock dock2 = createUberfireDockForTest(dockName2,
                                                      perspectiveName);

        url = BookmarkableUrlHelper.registerClosedDock(url, dock1);
        assertNotNull(url);
        assertTrue(url.contains("!W" + dockName1));

        url = BookmarkableUrlHelper.registerClosedDock(url, dock2);
        assertNotNull(url);
        assertTrue(url.contains("!W" + dockName2));

    }

    /**
     * Get a dock for the test
     * @param dockName
     * @param perspectiveName
     * @return
     */
    private UberfireDock createUberfireDockForTest(String dockName, String perspectiveName) {
        final PlaceRequest req = new DefaultPlaceRequest(dockName);
        UberfireDock dock = new UberfireDock(
                UberfireDockPosition.WEST,
                "iconType",
                req,
                perspectiveName);

        return dock;
    }
}
