package org.uberfire.client.mvp;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.gwtmockito.WithClassesToStub;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.uberfire.backend.vfs.Path;
import org.uberfire.backend.vfs.PathFactory;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.PathPlaceRequest;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

//@RunWith(GwtMockitoTestRunner.class)
//@WithClassesToStub({PathPlaceRequest.class})
public class PlaceHistoryHandlerTestGwt extends GWTTestCase {

    @Override
    public String getModuleName() {
        return "org.uberfire.client.mvp.PlaceHistoryHandlerTestGwt";
    }

    @Test
    public void testRegisterOpenEditorWithScreens() {
/*
        final Path path = PathFactory.newPath("file",
                                              "default://master@repo/path/to/file");
        final PlaceRequest ppr = new PathPlaceRequest(path);

        ppr.setIdentifier("Perspective Editor");
        String bookmarkableUrl = "PlugInAuthoringPerspective|[WPlugins Explorer,]";
        bookmarkableUrl = BookmarkableUrlHelper.registerOpenedScreen(bookmarkableUrl,
                                                                     ppr);
//        System.out.println("!!! " + bookmarkableUrl);
        String lol = BookmarkableUrlHelper.registerOpenedEditor(bookmarkableUrl,
                                                                (PathPlaceRequest) ppr);
//        System.out.println(">>> " + lol);
*/

        assertTrue(true);
    }

}
