/*
 * Copyright 2017 JBoss Inc
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

package org.uberfire.ext.wires.client.test.screens;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Event;
import com.google.inject.Inject;
import org.ext.uberfire.social.activities.client.widgets.timeline.regular.model.SocialTimelineWidgetModel;
import org.jboss.errai.common.client.dom.Button;
import org.jboss.errai.common.client.dom.Div;
import org.jboss.errai.ui.client.local.api.IsElement;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.SinkNative;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.uberfire.client.annotations.WorkbenchPartTitle;

import javax.enterprise.context.Dependent;

@Dependent
@Templated
public class SimpleButtonView implements IsElement,
                                           SimpleButtonPresenter.View {

    @Inject
    @DataField("panelContainer")
    Div panelContainer;

    @Inject
    @DataField("targetDiv")
    Div targetDiv;

    @Inject
    @DataField
    Button newEvent;
    private SimpleButtonPresenter presenter;

    @EventHandler("newEvent")
    public void onNewEvent(final ClickEvent clickEvent) {
        presenter.fireEvent(targetDiv);
    }

    @Override
    public void setupWidget(SocialTimelineWidgetModel model) {
        GWT.log("SETUP");
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return "Social Timeline";
    }

    @Override
    public void init(final SimpleButtonPresenter presenter) {
        this.presenter = presenter;
    }
}