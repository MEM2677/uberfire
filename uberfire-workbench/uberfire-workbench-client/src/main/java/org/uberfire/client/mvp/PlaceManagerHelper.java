package org.uberfire.client.mvp;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import org.jboss.errai.ioc.client.container.SyncBeanDef;
import org.jboss.errai.ioc.client.container.SyncBeanManager;
import org.uberfire.client.workbench.docks.UberfireDock;
import org.uberfire.client.workbench.docks.UberfireDockPosition;
import org.uberfire.client.workbench.docks.UberfireDocks;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.PathPlaceRequest;

@ApplicationScoped
public class PlaceManagerHelper {

    @Inject
    private PerspectiveManager perspectiveManager;
    @Inject
    private PlaceHistoryHandler placeHistoryHandler;
    @Inject
    private SyncBeanManager iocManager;
    // do NOT @Inject this!
    private UberfireDocks uberfireDocks;

    public PlaceHistoryHandler getPlaceHistoryHandler() {
        return placeHistoryHandler;
    }

    public PerspectiveManager getPerspectiveManager() {
        return perspectiveManager;
    }

    public UberfireDocks getUberfireDocks() {
        return uberfireDocks;
    }

    public SyncBeanManager getIocManager() {
        return iocManager;
    }

    @PostConstruct
    public void setup() {
        // get the UberfireDocks bean (if any)
        if (null != getIocManager()) {
            SyncBeanDef<UberfireDocks> dockBean = getIocManager().lookupBean(UberfireDocks.class);

            if (null != dockBean) {
                this.uberfireDocks = dockBean.getInstance();
            }
        }
    }

    /**
     * Take all the appropriate steps to track an opened screen, whatever the type
     * @param activity
     * @param place
     */
    public void onOpen(final Activity activity,
                       final PlaceRequest place) {
        getPlaceHistoryHandler().registerOpen(activity,
                                              place);
        // ...handle the case of a dock (if the current place is docked)
        handleDockOpen(place);
    }

    private void handleDockOpen(final PlaceRequest place) {
        PerspectiveActivity perspectiveActivity =
                getPerspectiveManager().getCurrentPerspective();

        if (null != getUberfireDocks()
                && null != place) {
            UberfireDock dock = getUberfireDocks()
                    .getDockedScreenInPerspective(perspectiveActivity.getIdentifier(),
                                                  place.getIdentifier());
            if (null != dock) {
                getPlaceHistoryHandler().registerOpenDock(dock);
            }
        }
    }

    /**
     * Take all the appropriate steps to track a closed screen, whatever the type
     * @param activity
     * @param place
     */
    public void onClose(final Activity activity,
                        final PlaceRequest place) {
        // always register closed screen...
        getPlaceHistoryHandler().registerClose(activity,
                                               place);
        // ...handle the case of a dock (if the current place is docked)
        handleDockClose(place);
        // ...handle the case of a PathPlaceRequest (if place is a PathPlaceRequest)
        handlePathClose(place);
    }

    private void handleDockClose(final PlaceRequest place) {
        PerspectiveActivity perspectiveActivity =
                getPerspectiveManager().getCurrentPerspective();

        if (null != getUberfireDocks()
                && null != perspectiveActivity
                && null != place) {
            UberfireDock dock = getUberfireDocks()
                    .getDockedScreenInPerspective(perspectiveActivity.getIdentifier(),
                                                  place.getIdentifier());
            if (null != dock) {
                getPlaceHistoryHandler().registerCloseDock(dock);
            }
        }
    }

    private void handlePathClose(final PlaceRequest place) {

        if (null == place) {
            return;
        }

        if (place instanceof PathPlaceRequest) {
            getPlaceHistoryHandler().registerClosedEditor(place);
        }
    }

    /**
     * Reset the current bookmarkable URL
     */
    public void flush() {
        placeHistoryHandler.flush();
    }


    /**
     * Show (expand) or hide (collapse) the desired dock in the current perspective
     * @param dockName
     */
    public void toggleDock(final String dockName) {
        final Activity currentActivity =
                getPerspectiveManager().getCurrentPerspective();
        final String positionString =
                String.valueOf(dockName.charAt(0));
//        final UberfireDockPosition position =
//                UberfireDockPosition.decode(positionString);
        final String dockId =
                dockName.substring(1);

        if (null != currentActivity
                && null != currentActivity.getIdentifier()) {
            String perspectiveName = currentActivity.getIdentifier();
            UberfireDock dock = uberfireDocks.getDockedScreenInPerspective(perspectiveName,
                                                                           dockId);
            if (null != dock) {
                    GWT.log("CLOSING " + dockId);
//                    uberfireDocks.expand(dock);
//                uberfireDocks.collapse(dock);
            }
        }
    }


}
