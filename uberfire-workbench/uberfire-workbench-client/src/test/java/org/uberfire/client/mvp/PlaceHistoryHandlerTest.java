package org.uberfire.client.mvp;

import java.util.Map;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.ActivityResourceType;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by matteo on 28/04/17.
 */
//@RunWith(MockitoJUnitRunner.class)
@RunWith(GwtMockitoTestRunner.class)
public class PlaceHistoryHandlerTest {

    private final WorkbenchScreenActivity screenActivity = mock(WorkbenchScreenActivity.class);
    private final WorkbenchScreenActivity perspectiveActivity = mock(WorkbenchScreenActivity.class);

    @Mock
    private PlaceRequestHistoryMapper mapper;

    @Spy
    PlaceHistoryHandler placeHistoryHandler;

    @Before
    public void setup() {

        when(screenActivity.isDynamic()).thenReturn(false);
        when(screenActivity.isType(ActivityResourceType.SCREEN.name())).thenReturn(true);
        when(screenActivity.onMayClose()).thenReturn(true);
        when(screenActivity.preferredWidth()).thenReturn(26);
        when(screenActivity.preferredHeight()).thenReturn(77);

        when(perspectiveActivity.isDynamic()).thenReturn(false);
        when(perspectiveActivity.isDefault()).thenReturn(true);
        when(perspectiveActivity.isType(ActivityResourceType.PERSPECTIVE.name())).thenReturn(true);
        when(perspectiveActivity.onMayClose()).thenReturn(true);
        when(perspectiveActivity.preferredWidth()).thenReturn(26);
        when(perspectiveActivity.preferredHeight()).thenReturn(77);
    }

    @Test
    public void testPerspectiveFromUrlSimple() {
        PlaceRequest req = new DefaultPlaceRequest("perspective");

        PlaceRequest place = placeHistoryHandler.getPerspectiveFromUrl(req);
        assertNotNull(place);
        assertEquals("perspective",
                     place.getIdentifier());
        assertSame(place,
                   req);
    }


    @Test
    public void testPerspectiveFromUrlWithHistory() {
        final String REQUEST = "perspective|secreenOne,~screenTwo$screenThree";

        PlaceRequest req = new DefaultPlaceRequest(REQUEST);

        PlaceRequest place = placeHistoryHandler.getPerspectiveFromUrl(req);
        assertNotNull(place);
        assertEquals("perspective",
                     place.getIdentifier());
        assertNotSame(place,
                      req);
    }


    @Test
    public void testPerspectiveFromUrlWithHistoryExt() {
        // FIXME this smells a lot
        class HistoryWrapper extends PlaceHistoryHandler
        {
            @Override
            public PlaceRequest getPerspectiveFromUrl(PlaceRequest place) {
                String url = place.getFullIdentifier();

                String perspectiveName = url.substring(0,
                                                       url.indexOf(PERSPECTIVE_SEP));

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
        }

        HistoryWrapper wrapper = spy(new HistoryWrapper());
        final String REQUEST = "perspective|secreenOne,~screenTwo$screenThree";

        PlaceRequest req = new DefaultPlaceRequest(REQUEST);

        PlaceRequest place = wrapper.getPerspectiveFromUrl(req);
        assertNotNull(place);
        assertEquals("perspective",
                     place.getIdentifier());
        assertNotSame(place,
                      req);
    }

    @Test
    public void testRegisterExistingURL() {
        final String REQUEST = "perspective|secreenOne,~screenTwo$screenThree";
        final PlaceRequest req = new DefaultPlaceRequest(REQUEST);

        placeHistoryHandler.registerOpen(screenActivity,
                                         req,
                                         false);
        assertEquals(REQUEST,
                     placeHistoryHandler.getHistoryUrl());
    }

    @Test
    public void testRegisterBuild() {
        final String SCREEN1_ID = "screen1";
        final String SCREEN2_ID = "screen2";
        final String PERSPECTIVE_ID = "perspective";
        final PlaceRequest screen1 = new DefaultPlaceRequest(SCREEN1_ID);
        final PlaceRequest screen2 = new DefaultPlaceRequest(SCREEN2_ID);
        final PlaceRequest perspective = new DefaultPlaceRequest(PERSPECTIVE_ID);

        placeHistoryHandler.registerOpen(screenActivity,
                                         screen1,
                                         false);
        assertEquals(SCREEN1_ID,
                     placeHistoryHandler.getHistoryUrl());

        final String SCREENS_OPEN_LIST = SCREEN1_ID.concat(",").concat(SCREEN2_ID);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen2,
                                         false);
        assertEquals(SCREENS_OPEN_LIST,
                     placeHistoryHandler.getHistoryUrl());

        final String PERSPECTIVE_STRING = PERSPECTIVE_ID.concat("|").concat(SCREENS_OPEN_LIST);
        placeHistoryHandler.registerOpen(perspectiveActivity,
                                         perspective,
                                         false);
        assertEquals(PERSPECTIVE_STRING,
                     placeHistoryHandler.getHistoryUrl());
    }

    @Test
    public void TestScreenCloseSimple() {
        final String SCREEN1_ID = "screen1";
        final String SCREEN2_ID = "screen2";
        final PlaceRequest screen1 = new DefaultPlaceRequest(SCREEN1_ID);
        final PlaceRequest screen2 = new DefaultPlaceRequest(SCREEN2_ID);

        placeHistoryHandler.registerOpen(screenActivity,
                                         screen1,
                                         false);
        assertEquals(SCREEN1_ID,
                     placeHistoryHandler.getHistoryUrl());

        final String CLOSED_SCREENS = "~".concat(SCREEN1_ID);
        placeHistoryHandler.registerClose(screenActivity,
                                          screen1,
                                          false);
        assertEquals(CLOSED_SCREENS,
                     placeHistoryHandler.getHistoryUrl());

        String URL = "~".concat(SCREEN1_ID).concat(",").concat(SCREEN2_ID);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen2,
                                         false);
        assertEquals(URL,
                     placeHistoryHandler.getHistoryUrl());

        URL = "~".concat(SCREEN1_ID).concat(",~").concat(SCREEN2_ID);
        placeHistoryHandler.registerClose(screenActivity,
                                          screen2,
                                          false);
        assertEquals(URL,
                     placeHistoryHandler.getHistoryUrl());
    }

    @Test
    @SuppressWarnings(value = "all")
    public void TestScreenCloseSimpleWithArgs() {
        final String SCREEN1_ID = "screen1";
        final String SCREEN2_ID = "screen2";
        final PlaceRequest screen1 = new DefaultPlaceRequest(SCREEN1_ID);
        final PlaceRequest screen2 = new DefaultPlaceRequest(SCREEN2_ID);
        final String PAR1_KEY = "y";
        final String PAR1_VALUE = "x";
        final String PAR2_KEY = "a";
        final String PAR2_VALUE = "b";
        final String PARAM_TAIL =
                getParamListForTest(PAR2_KEY,
                                    PAR2_VALUE,
                                    PAR1_KEY,
                                    PAR1_VALUE);

        screen1.addParameter(PAR1_KEY,
                             PAR1_VALUE);
        screen1.addParameter(PAR2_KEY,
                             PAR2_VALUE);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen1,
                                         false);
        assertEquals(SCREEN1_ID.concat(PARAM_TAIL),
                     placeHistoryHandler.getHistoryUrl());

        final String CLOSED_SCREENS = "~".concat(SCREEN1_ID)
                .concat(PARAM_TAIL);
        placeHistoryHandler.registerClose(screenActivity,
                                          screen1,
                                          false);
        assertEquals(CLOSED_SCREENS,
                     placeHistoryHandler.getHistoryUrl());

        String URL = "~".concat(SCREEN1_ID)
                .concat(PARAM_TAIL)
                .concat(",").concat(SCREEN2_ID);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen2,
                                         false);
        assertEquals(URL,
                     placeHistoryHandler.getHistoryUrl());

        URL = "~".concat(SCREEN1_ID)
                .concat(PARAM_TAIL)
                .concat(",~").concat(SCREEN2_ID);
        placeHistoryHandler.registerClose(screenActivity,
                                          screen2,
                                          false);
        assertEquals(URL,
                     placeHistoryHandler.getHistoryUrl());
    }

    @Test
    public void testOtherScreen() {
        final String SCREEN1_ID = "screen1";
        final String SCREEN2_ID = "screen2";
        final String SCREEN3_ID = "screen3";
        final String PERSPECTIVE_ID = "perspective";
        final PlaceRequest screen1 = new DefaultPlaceRequest(SCREEN1_ID);
        final PlaceRequest screen2 = new DefaultPlaceRequest(SCREEN2_ID);
        final PlaceRequest screen3 = new DefaultPlaceRequest(SCREEN3_ID);
        final PlaceRequest perspective = new DefaultPlaceRequest(PERSPECTIVE_ID);

        placeHistoryHandler.registerOpen(screenActivity,
                                         screen1,
                                         false);
        assertEquals(SCREEN1_ID,
                     placeHistoryHandler.getHistoryUrl());

        final String PERSPECTIVE_STRING = PERSPECTIVE_ID.concat("|").concat(SCREEN1_ID);
        placeHistoryHandler.registerOpen(perspectiveActivity,
                                         perspective,
                                         false);
        assertEquals(PERSPECTIVE_STRING,
                     placeHistoryHandler.getHistoryUrl());

        String EXPECTED_URL = PERSPECTIVE_STRING.concat("$").concat(SCREEN2_ID);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen2,
                                         false);
        assertEquals(EXPECTED_URL,
                     placeHistoryHandler.getHistoryUrl());

        EXPECTED_URL = EXPECTED_URL.concat(",").concat(SCREEN3_ID);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen3,
                                         false);
        assertEquals(EXPECTED_URL,
                     placeHistoryHandler.getHistoryUrl());
    }

    @Test
//    @SuppressWarnings(value = "all")
    public void testCloseAllScreens() {
        final PlaceRequest screen1 = new DefaultPlaceRequest(SCREEN1_ID);
        final PlaceRequest screen2 = new DefaultPlaceRequest(SCREEN2_ID);
        final PlaceRequest screen3 = new DefaultPlaceRequest(SCREEN3_ID);
        final PlaceRequest screen4 = new DefaultPlaceRequest(SCREEN4_ID);

        prepareCompleteUrlWithParamsForTests();

        String EXPECTED_URL = "perspective|screen1,~screen2$screen3?y=x,screen4?y=x";
        placeHistoryHandler.registerClose(screenActivity,
                                          screen2,
                                          false);
        assertEquals(EXPECTED_URL,
                     placeHistoryHandler.getHistoryUrl());

        EXPECTED_URL = "perspective|screen1,~screen2$screen4?y=x";
        placeHistoryHandler.registerClose(screenActivity,
                                          screen3,
                                          false);
        assertEquals(EXPECTED_URL,
                     placeHistoryHandler.getHistoryUrl());

        EXPECTED_URL = "perspective|screen1,~screen2";
        placeHistoryHandler.registerClose(screenActivity,
                                          screen4,
                                          false);
        assertEquals(EXPECTED_URL,
                     placeHistoryHandler.getHistoryUrl());

        EXPECTED_URL = "perspective|~screen1,~screen2";
        placeHistoryHandler.registerClose(screenActivity,
                                          screen1,
                                          false);
        assertEquals(EXPECTED_URL,
                     placeHistoryHandler.getHistoryUrl());
    }

    @Test
    public void TestFlush() {
        prepareCompleteUrlForTests();
        placeHistoryHandler.flush();
        assertNotNull(placeHistoryHandler.getHistoryUrl());
        assertEquals("",
                     placeHistoryHandler.getHistoryUrl());
        assertEquals("",
                     placeHistoryHandler.getAddressBarUrl());
    }

    @Test
    public void testDockOpenClose() {
        final PlaceRequest screen1 = new DefaultPlaceRequest(SCREEN1_ID);
        final PlaceRequest screen2 = new DefaultPlaceRequest(SCREEN2_ID);
        final PlaceRequest screen3 = new DefaultPlaceRequest(SCREEN3_ID);
        final PlaceRequest perspective = new DefaultPlaceRequest(PERSPECTIVE_ID);

        placeHistoryHandler.registerOpen(screenActivity,
                                         screen1,
                                         true);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen2,
                                         false);
        placeHistoryHandler.registerOpen(perspectiveActivity,
                                         perspective,
                                         false);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen3,
                                         true);

        String EXPECTED_URL =
                "perspective|!screen1,screen2$!screen3";
        assertEquals(EXPECTED_URL,
                     placeHistoryHandler.getHistoryUrl());

        placeHistoryHandler.registerClose(screenActivity,
                                          screen1,
                                          true);
        EXPECTED_URL =
                "perspective|~!screen1,screen2$!screen3";
        assertEquals(EXPECTED_URL,
                     placeHistoryHandler.getHistoryUrl());

        placeHistoryHandler.registerClose(screenActivity,
                                          screen3,
                                          false);
        EXPECTED_URL =
                "perspective|~!screen1,screen2";
        assertEquals(EXPECTED_URL,
                     placeHistoryHandler.getHistoryUrl());
    }

    @Test
    public void testUrlLimit() {
        int cnt = 0;
        int length = 0;

        do {
            final PlaceRequest screen = new DefaultPlaceRequest("screen".concat(String.valueOf(cnt++)));

            placeHistoryHandler.registerOpen(screenActivity,
                                             screen,
                                             false);

            if (length == placeHistoryHandler.getHistoryUrl().length())
            {
                break;
            }
            length = placeHistoryHandler.getHistoryUrl().length();
        } while ((placeHistoryHandler.getHistoryUrl().length()
                < placeHistoryHandler.MAX_NAV_URL_SIZE + 100));
        assertNotNull(placeHistoryHandler.getHistoryUrl());
        assertFalse(placeHistoryHandler.getHistoryUrl().length()
                            > placeHistoryHandler.MAX_NAV_URL_SIZE);
    }

    /**
     * Prepare an URL -> perspective|screen1,screen2$screen3,screen4
     */
    private void prepareCompleteUrlForTests() {
        final PlaceRequest screen1 = new DefaultPlaceRequest(SCREEN1_ID);
        final PlaceRequest screen2 = new DefaultPlaceRequest(SCREEN2_ID);
        final PlaceRequest screen3 = new DefaultPlaceRequest(SCREEN3_ID);
        final PlaceRequest screen4 = new DefaultPlaceRequest(SCREEN4_ID);
        final PlaceRequest perspective = new DefaultPlaceRequest(PERSPECTIVE_ID);
        final String expectedUrl = "perspective|screen1,screen2$screen3,screen4";

        placeHistoryHandler.registerOpen(screenActivity,
                                         screen1,
                                         false);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen2,
                                         false);
        placeHistoryHandler.registerOpen(perspectiveActivity,
                                         perspective,
                                         false);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen3,
                                         false);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen4,
                                         false);
        assertEquals(expectedUrl,
                     placeHistoryHandler.getHistoryUrl());
    }

    /**
     * Prepare an URL -> perspective|screen1,screen2$screen3?y=x,screen4?y=x
     */
//    @SuppressWarnings(value = "all")
    private void prepareCompleteUrlWithParamsForTests() {
        final PlaceRequest screen1 = new DefaultPlaceRequest(SCREEN1_ID);
        final PlaceRequest screen2 = new DefaultPlaceRequest(SCREEN2_ID);
        final PlaceRequest screen3 = new DefaultPlaceRequest(SCREEN3_ID);
        final PlaceRequest screen4 = new DefaultPlaceRequest(SCREEN4_ID);
        final PlaceRequest perspective = new DefaultPlaceRequest(PERSPECTIVE_ID);
        final String PAR_KEY = "y";
        final String PAR_VALUE = "x";
        final String PARAM_TAIL =
                getParamListForTest(PAR_KEY,
                                    PAR_VALUE);
        final String expectedUrl = "perspective|screen1,screen2$screen3"
                + PARAM_TAIL + ",screen4" + PARAM_TAIL;

        screen3.addParameter(PAR_KEY,
                             PAR_VALUE);
        screen4.addParameter(PAR_KEY,
                             PAR_VALUE);

        placeHistoryHandler.registerOpen(screenActivity,
                                         screen1,
                                         false);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen2,
                                         false);
        placeHistoryHandler.registerOpen(perspectiveActivity,
                                         perspective,
                                         false);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen3,
                                         false);
        placeHistoryHandler.registerOpen(screenActivity,
                                         screen4,
                                         false);
        assertEquals(expectedUrl,
                     placeHistoryHandler.getHistoryUrl());
    }

    /**
     * Return the list of parameters needed for test
     * @param txt
     * @return
     */
    private String getParamListForTest(String... txt) {
        boolean isQM = true;
        StringBuilder param = new StringBuilder();

        for (int i = 0; i < txt.length; i++) {
            if ((i == 0)
                    || (i % 2 == 0)) {
                if (isQM) {
                    param.append("?");
                    isQM = false;
                } else {
                    param.append("&");
                }
                param.append(txt[i]);
            } else {
                param.append("=");
                param.append(txt[i]);
            }
        }
        return param.toString();
    }

    final static String SCREEN1_ID = "screen1";
    final static String SCREEN2_ID = "screen2";
    final static String SCREEN3_ID = "screen3";
    final static String SCREEN4_ID = "screen4";
    final static String PERSPECTIVE_ID = "perspective";
}
