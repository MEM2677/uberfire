package org.uberfire.ext.wires.client.test.screens;

import com.google.gwt.core.client.GWT;
import org.jboss.errai.ui.client.local.api.IsElement;

/**
 * Created by matteo on 19/10/17.
 */
public class SimpleButtonView implements IsElement,
            SimpleButtonPresenter.View{



    private SimpleButtonPresenter presenter;

    @Override
    public void setupWidget(SimpleButtonPresenter presenter) {
        GWT.log("SETUP");
    }

    @Override
    public void init(SimpleButtonPresenter presenter) {
        GWT.log("INIT");
        this.presenter = presenter;
    }
}
