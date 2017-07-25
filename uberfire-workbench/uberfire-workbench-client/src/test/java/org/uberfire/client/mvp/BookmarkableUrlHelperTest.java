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

import java.lang.annotation.Annotation;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;

import com.google.gwt.http.client.URL;
import com.google.gwtmockito.GwtMockitoTestRunner;
import junit.framework.TestCase;
import org.jboss.errai.ioc.client.QualifierUtil;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ioc.client.container.SyncBeanManagerImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.backend.vfs.Path;
import org.uberfire.backend.vfs.PathFactory;
import org.uberfire.backend.vfs.impl.ObservablePathImpl;
import org.uberfire.client.util.MockIOCBeanDef;
import org.uberfire.client.workbench.docks.UberfireDock;
import org.uberfire.client.workbench.docks.UberfireDockPosition;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.mvp.impl.PathPlaceRequest;

@RunWith(GwtMockitoTestRunner.class)
public class BookmarkableUrlHelperTest extends TestCase {

    @BeforeClass
    public static void setupBeans() {
        ((SyncBeanManagerImpl) IOC.getBeanManager()).reset();

        IOC.getBeanManager().registerBean(new MockIOCBeanDef<ObservablePath, ObservablePathImpl>(new ObservablePathImpl(),
                                                                                                 ObservablePath.class,
                                                                                                 Dependent.class,
                                                                                                 new HashSet<Annotation>(Arrays.asList(QualifierUtil.DEFAULT_QUALIFIERS)),
                                                                                                 null,
                                                                                                 true));
    }

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

        // test with screen closed (we compose the URL)
        final String closedScreen = "closedScreen";
        final PlaceRequest closed = new DefaultPlaceRequest(closedScreen);
        url = "perspective|"
                .concat(BookmarkableUrlHelper.CLOSED_PREFIX)
                .concat(closedScreen).concat(",openScreen$externalScreen");
        url = BookmarkableUrlHelper.registerOpenedScreen(url,
                                                         closed);
        String expected = "perspective|"
                .concat(closedScreen).concat(",openScreen$externalScreen");
        assertEquals(expected,
                     url);
        // compose a big URL
        StringBuilder bigUrl = new StringBuilder(perspective);
        while (bigUrl.length() < BookmarkableUrlHelper.MAX_NAV_URL_SIZE) {
            bigUrl.append(",screen");
        }
        url = BookmarkableUrlHelper.registerOpenedScreen(bigUrl.toString(),
                                                         req1);
        assertNotNull(url);
        assertEquals(bigUrl.toString(),
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

        // screen already closed
        url = "perspective|screen1,~screen2$screen3";
        url = BookmarkableUrlHelper.registerClose(url,
                                                  "screen2");
        assertEquals("perspective|screen1,~screen2$screen3",
                     url);

        url = "perspective|screen1$screen2";
        url = BookmarkableUrlHelper.registerClose(url,
                                                  "screen2");
        assertEquals("perspective|screen1",
                     url);

        url = "perspective|screen1$screen2,screen3";
        url = BookmarkableUrlHelper.registerClose(url,
                                                  "screen2");
        assertEquals("perspective|screen1$screen3",
                     url);

        url = "perspective|screen1$screen2,screen3,screen4";
        url = BookmarkableUrlHelper.registerClose(url,
                                                  "screen3");
        assertEquals("perspective|screen1$screen2,screen4",
                     url);
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
        assertFalse(BookmarkableUrlHelper.isPerspectiveScreen(null,
                                                              "screen2"));
        assertFalse(BookmarkableUrlHelper.isPerspectiveScreen("",
                                                              "screen2"));
        assertFalse(BookmarkableUrlHelper.isPerspectiveScreen(url,
                                                              null));
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
    public void testGetScreensFromUrl() {
        final String url1 = "perspective|~screen1,screen2$!screen3,screen4";
        final String url2 = "UFWidgets|PagedTableScreen[ESimpleDockScreen,!WSimpleDockScreen,ESimpleDockScreen,]";
        final String url3 = "PagedTableScreen[ESimpleDockScreen,!WSimpleDockScreen,ESimpleDockScreen,]";
        final String url4 = "perspective|[!WSimpleDockedScreen,]";

        Set<String> set = BookmarkableUrlHelper.getScreensFromUrl(url1);
        assertNotNull(set);
        assertFalse(set.isEmpty());

        assertEquals(4,
                     set.size());
        assertTrue(set.contains("~screen1"));
        assertTrue(set.contains("screen2"));
        assertTrue(set.contains("!screen3"));
        assertTrue(set.contains("screen4"));

        set = BookmarkableUrlHelper.getScreensFromUrl(url2);
        assertNotNull(set);

        assertFalse(set.isEmpty());
        assertEquals(1,
                     set.size());
        assertTrue(set.contains("PagedTableScreen"));

        set = BookmarkableUrlHelper.getScreensFromUrl(null);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        // test with bookmarkable URL with no perspective
        set = BookmarkableUrlHelper.getScreensFromUrl(url3);
        assertNotNull(set);
        assertFalse(set.isEmpty());
        assertTrue(set.contains("PagedTableScreen"));

        // test URL with no screens
        set = BookmarkableUrlHelper.getScreensFromUrl(url4);
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testGDockedScreensFromPlace() {
        final String url1 = "perspective|~screen1,screen2$~screen3,screen4";
        final String url2 = "UFWidgets|PagedTableScreen[ESimpleDockScreen,!WSimpleDockScreen,ESimpleDockScreen,]";

        Set<String> set = BookmarkableUrlHelper.getDockedScreensFromUrl(url1);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        set = BookmarkableUrlHelper.getDockedScreensFromUrl(url2);
        assertNotNull(set);
        assertFalse(set.isEmpty());
        assertEquals(2,
                     set.size());
        assertTrue(set.contains("ESimpleDockScreen"));
        assertTrue(set.contains("!WSimpleDockScreen"));

        // test with invalid URL
        set = BookmarkableUrlHelper.getDockedScreensFromUrl(null);
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testGDockedScreensFromPlaceString() {
        final String url = "perspective|~screen1,screen2$~screen3,screen4";
        final String url2 = "UFWidgets|PagedTableScreen[ESimpleDockScreen,!WSimpleDockScreen,EAnotherDockScreen,]";

        Set<String> set = BookmarkableUrlHelper.getDockedScreensFromUrl(url);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        set = BookmarkableUrlHelper.getDockedScreensFromUrl(url2);
        assertNotNull(set);
        assertFalse(set.isEmpty());
        assertEquals(3,
                     set.size());
        assertTrue(set.contains("ESimpleDockScreen"));
        assertTrue(set.contains("!WSimpleDockScreen"));
        assertTrue(set.contains("EAnotherDockScreen"));

        // test with invalid URL
        set = BookmarkableUrlHelper.getDockedScreensFromUrl(null);
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
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

    @Test
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

    @Test
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

        // test with a closed dock
        url = perspectiveName
                .concat(BookmarkableUrlHelper.PERSPECTIVE_SEP)
                .concat("screen1[!WdockedScreen,]");
        String expected = perspectiveName
                .concat(BookmarkableUrlHelper.PERSPECTIVE_SEP)
                .concat("screen1[WdockedScreen,]");
        url = BookmarkableUrlHelper.registerOpenedDock(url,
                                                       dock1);
        assertEquals(expected,
                     url);

        // test with invalid dock and URL
        expected = BookmarkableUrlHelper.registerOpenedDock(url,
                                                            null);
        assertNotNull(expected);
        assertEquals(expected,
                     url);

        url = "  ";
        expected = BookmarkableUrlHelper.registerOpenedDock(url,
                                                            null);
        assertNotNull(expected);
        assertEquals(expected,
                     url);
    }

    @Test
    public void testDoubleRegisterOpenDock() {
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

        // open the same docked screen twice
        url = BookmarkableUrlHelper.registerOpenedDock(url,
                                                       dock1);
        url = BookmarkableUrlHelper.registerOpenedDock(url,
                                                       dock1);
        assertEquals(perspectiveName
                             .concat(BookmarkableUrlHelper.PERSPECTIVE_SEP)
                             .concat("screen1[WdockedScreen,]"),
                     url);
        // open the another docked screen twice
        url = BookmarkableUrlHelper.registerOpenedDock(url,
                                                       dock2);
        url = BookmarkableUrlHelper.registerOpenedDock(url,
                                                       dock2);
        assertEquals(perspectiveName
                             .concat(BookmarkableUrlHelper.PERSPECTIVE_SEP)
                             .concat("screen1[WdockedScreen,WdockedScreenNew,]"),
                     url);
    }

    @Test
    public void testRegisterClosedDock() {
        final String dockName1 = "dockedScreen1";
        final String dockName2 = "dockedScreen2";
        String perspectiveName = "perspective";
        String url = "perspectiveName|screen[W" + dockName1 + ",W" + dockName2 + ",]";
        UberfireDock dock1 = createUberfireDockForTest(dockName1,
                                                       perspectiveName);
        UberfireDock dock2 = createUberfireDockForTest(dockName2,
                                                       perspectiveName);

        url = BookmarkableUrlHelper.registerClosedDock(url,
                                                       dock1);
        assertNotNull(url);
        assertTrue(url.contains("!W" + dockName1));

        url = BookmarkableUrlHelper.registerClosedDock(url,
                                                       dock2);
        assertNotNull(url);
        assertTrue(url.contains("!W" + dockName2));
    }

    @Test
    public void testDoubleRegisterClosedDock() {
        final String dockName1 = "dockedScreen1";
        final String dockName2 = "dockedScreen2";
        String perspectiveName = "perspective";
        String url = "perspectiveName|screen[W" + dockName1 + ",W" + dockName2 + ",]";
        UberfireDock dock1 = createUberfireDockForTest(dockName1,
                                                       perspectiveName);
        UberfireDock dock2 = createUberfireDockForTest(dockName2,
                                                       perspectiveName);

        // close the same dock twice
        url = BookmarkableUrlHelper.registerClosedDock(url,
                                                       dock1);
        url = BookmarkableUrlHelper.registerClosedDock(url,
                                                       dock1);
        assertNotNull(url);
        assertTrue(url.contains("!W" + dockName1));
        // close another dock twice
        url = BookmarkableUrlHelper.registerClosedDock(url,
                                                       dock2);
        url = BookmarkableUrlHelper.registerClosedDock(url,
                                                       dock2);
        assertNotNull(url);
        assertTrue(url.contains("!W" + dockName2));

        // test with invalid dock and URl
        String expected = "anyBookmarkableUrl";
        url = BookmarkableUrlHelper.registerClosedDock(expected,
                                                       null);
        assertNotNull(url);
        assertEquals(expected,
                     url);

        expected = "    "; // empty string for URL
        url = BookmarkableUrlHelper.registerClosedDock(expected,
                                                       dock2);
        assertNotNull(url);
        assertEquals(expected,
                     url);
    }

    @Test
    public void testOpenADockWithEmptyBookmark() {
        UberfireDock dock1 = createUberfireDockForTest("dock",
                                                       "perspective");
        String url = BookmarkableUrlHelper.registerOpenedDock("",
                                                              dock1);
        assertEquals("[Wdock,]",
                     url);
    }

    @Test
    public void testRegisterCloseEditor() {
        final Path path = PathFactory.newPath("file",
                                              "default://master@repo/path/to/file");
        final PlaceRequest ppr = new PathPlaceRequest(path);

        ppr.setIdentifier("Perspective Editor");
        final String perspectiveClosedUrl = "PlugInAuthoringPerspective|[WPlugins Explorer,]$";
        final String perspectiveOpenUrl = perspectiveClosedUrl.concat(ppr.getFullIdentifier());

        String url = BookmarkableUrlHelper
                .registerCloseEditor(perspectiveOpenUrl,
                                     ppr);

        assertEquals(perspectiveClosedUrl,
                     url);

        // invoke with invalid field type
        final PlaceRequest dpr = new DefaultPlaceRequest("default://master@repo/path/to/file");

        url = BookmarkableUrlHelper
                .registerCloseEditor(perspectiveOpenUrl,
                                     dpr);
        assertEquals(perspectiveOpenUrl,
                     url);
    }

    @Test
    public void testRegisterCloseEditorWithScreens() {
        final Path path = PathFactory.newPath("file",
                                              "default://master@repo/path/to/file");
        final PlaceRequest ppr = new PathPlaceRequest(path);

        ppr.setIdentifier("Perspective Editor");
        final String perspectiveClosedUrl = "PlugInAuthoringPerspective|[WPlugins Explorer,]$";
        final String perspectiveOpenUrl = perspectiveClosedUrl.concat(ppr.getFullIdentifier()).concat(",screen1");

        String url = BookmarkableUrlHelper
                .registerCloseEditor(perspectiveOpenUrl,
                                     ppr);
        final String expectedUrl = "PlugInAuthoringPerspective|[WPlugins Explorer,]$screen1";
        assertEquals(expectedUrl,
                     url);

        // invoke with NULL
        url = BookmarkableUrlHelper
                .registerCloseEditor(expectedUrl,
                                     null);
        assertEquals(expectedUrl,
                     url);
    }

    @Test
    public void testRegisterOpenEditorWithScreens() {
        final String PATH = "default://master@repo/path/to/file";
        final String FILE = "generic_file";
        final Path path = PathFactory.newPath(FILE,
                                              PATH);
        final PlaceRequest ppr = new PathPlaceRequest(path);

        ppr.setIdentifier("Perspective Editor");
        String bookmarkableUrl = "PlugInAuthoringPerspective|[WPlugins Explorer,]";
        bookmarkableUrl = BookmarkableUrlHelper.registerOpenedScreen(bookmarkableUrl,
                                                                     ppr);
        assertEquals("PlugInAuthoringPerspective|[WPlugins Explorer,]$Perspective Editor?path_uri=default%3A%2F%2Fmaster%40repo%2Fpath%2Fto%2Ffile&file_name=generic_file&has_version_support=false",
                     bookmarkableUrl);
        // add the same editor again
        bookmarkableUrl = BookmarkableUrlHelper.registerOpenedEditor(bookmarkableUrl,
                                                                     (PathPlaceRequest) ppr);
        Map<String, Map<String, String>> editors =
                BookmarkableUrlHelper.getOpenedEditorsFromUrl(bookmarkableUrl);
        assertNotNull(editors);
        assertFalse(editors.isEmpty());
        assertEquals(1,
                     editors.size());
        assertTrue(editors.containsKey("default%3A%2F%2Fmaster%40repo%2Fpath%2Fto%2Ffile"));
        Map<String, String> arguments = editors.get("default%3A%2F%2Fmaster%40repo%2Fpath%2Fto%2Ffile");
        assertNotNull(arguments);
        assertFalse(arguments.isEmpty());
        assertEquals(1,
                     arguments.size());
        assertTrue(arguments.containsKey(PathPlaceRequest.FILE_NAME_MARKER));
        assertEquals(FILE,
                     arguments.get(PathPlaceRequest.FILE_NAME_MARKER));
    }

    @Test
    public void testGetOpenedEditorsFromUrl() {
        final String bookmarkableUrl = "PlugInAuthoringPerspective|[!WPlugins Explorer,]$Perspective Editor?path_uri=default://master@plugins/BBBB/perspective_layout.plugin&file_name=perspective_layout.plugin&has_version_support=false&name==BBBB,Editor PlugIn Editor?path_uri=default://master@plugins/AAAA/editor.plugin&file_name=editor.plugin&has_version_support=true&name==AAAA";

        Map<String, Map<String, String>> editors
                = BookmarkableUrlHelper.getOpenedEditorsFromUrl(bookmarkableUrl);
        assertNotNull(editors);
        assertFalse(editors.isEmpty());
        assertEquals(2,
                     editors.size());

        for (Map.Entry<String, Map<String, String>> editor : editors.entrySet()) {
            String key = editor.getKey();
            Map<String, String> arguments = editor.getValue();

            assertTrue(
                    key.equals("default://master@plugins/BBBB/perspective_layout.plugin")
                            || key.equals("default://master@plugins/AAAA/editor.plugin")
            );
            assertNotNull(arguments);
            assertFalse(arguments.isEmpty());
            assertEquals(2,
                         arguments.size());
            assertTrue(arguments.containsKey(PathPlaceRequest.FILE_NAME_MARKER));

            assertTrue(
                    arguments.get(PathPlaceRequest.FILE_NAME_MARKER)
                            .equals("editor.plugin")
                            || arguments.get(PathPlaceRequest.FILE_NAME_MARKER)
                            .equals("perspective_layout.plugin")
            );
            assertTrue(
                    arguments.get("name")
                            .equals("AAAA")
                            || arguments.get("name")
                            .equals("BBBB")
            );
        }
    }

    @Test
    public void testIsValidScreen() {
        final String s1 = "screen1";
        final String s2 = "Editor PlugIn Editor?path_uri=default://master@plugins/AAAA/editor.plugin&file_name=editor.plugin&has_version_support=true&name==AAAA";
        final String s3 = "";
        final String s4 = null;

        assertTrue(BookmarkableUrlHelper.isValidScreen(s1));
        assertFalse(BookmarkableUrlHelper.isValidScreen(s2));
        assertFalse(BookmarkableUrlHelper.isValidScreen(s3));
        assertFalse(BookmarkableUrlHelper.isValidScreen(s4));
    }

    @Test
    public void testGetPerspectiveFromUrl() {
        final String u1 = "PlugInAuthoringPerspective|[!WPlugins Explorer,]$Screen PlugIn Editor?path_uri=default://master@plugins/CCCC/screen.plugin&file_name=screen.plugin&has_version_support=false&name==CCCC,Perspective Editor?path_uri=default://master@plugins/AAA/perspective_layout.plugin&file_name=perspective_layout.plugin&has_version_support=false&name==AAA,Editor PlugIn Editor?path_uri=default://master@plugins/BBBB/editor.plugin&file_name=editor.plugin&has_version_support=false&name==BBBB";
        final String u3 = "   ";
        final String u4 = "perspective";
        final String u5 = "anotherPerspective|screen1,~screen2";

        PlaceRequest p1 = BookmarkableUrlHelper.getPerspectiveFromUrl(u1);
        PlaceRequest p2 = BookmarkableUrlHelper.getPerspectiveFromUrl(null);
        PlaceRequest p3 = BookmarkableUrlHelper.getPerspectiveFromUrl(u3);
        PlaceRequest p4 = BookmarkableUrlHelper.getPerspectiveFromUrl(u4);
        PlaceRequest p5 = BookmarkableUrlHelper.getPerspectiveFromUrl(u5);
        assertNotNull(p1);
        assertEquals("PlugInAuthoringPerspective",
                     p1.getFullIdentifier());
        assertNull(p2);
        assertNull(p3);
        assertNotNull(p4);
        assertEquals("perspective",
                     p4.getFullIdentifier());
        assertNotNull(p5);
        assertEquals("anotherPerspective",
                     p5.getFullIdentifier());
    }

    /**
     * Get a dock for the test
     * @param dockName
     * @param perspectiveName
     * @return
     */
    private UberfireDock createUberfireDockForTest(String dockName,
                                                   String perspectiveName) {
        final PlaceRequest req = new DefaultPlaceRequest(dockName);
        UberfireDock dock = new UberfireDock(
                UberfireDockPosition.WEST,
                "iconType",
                req,
                perspectiveName);

        return dock;
    }
}
