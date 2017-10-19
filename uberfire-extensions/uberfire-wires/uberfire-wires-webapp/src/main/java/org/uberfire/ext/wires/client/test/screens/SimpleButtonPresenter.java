package org.uberfire.ext.wires.client.test.screens;

import com.google.gwt.core.client.GWT;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.mvp.UberElement;
import org.uberfire.lifecycle.OnOpen;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;


@ApplicationScoped
@WorkbenchScreen(identifier = "SimpleButtonPresenter")
public class SimpleButtonPresenter {

    @Inject
    PlaceManager placeManager;
    @Inject
    private View view;

    @OnOpen
    public void onOpen() {
        GWT.log("ON OPEN");
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return "Simple Button Presenter";
    }

    @WorkbenchPartView
    public UberElement<SimpleButtonPresenter> getView() {
        return view;
    }

    public interface View extends UberElement<SimpleButtonPresenter> {
        void setupWidget(SimpleButtonPresenter presenter);
    }
}
