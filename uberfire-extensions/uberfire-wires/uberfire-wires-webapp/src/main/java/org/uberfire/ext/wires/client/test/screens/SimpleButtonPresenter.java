/*
 * Copyright 2015 JBoss, by Red Hat, Inc
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
import org.ext.uberfire.social.activities.client.widgets.timeline.regular.model.SocialTimelineWidgetModel;
import org.jboss.errai.security.shared.api.identity.User;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.mvp.UberElement;
import org.uberfire.ext.wires.shared.social.ShowcaseSocialUserEvent;
import org.uberfire.lifecycle.OnOpen;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import org.jboss.errai.common.client.dom.HTMLElement;
import org.uberfire.mvp.impl.DefaultPlaceRequest;

@ApplicationScoped
@WorkbenchScreen(identifier = "SimpleButtonPresenter")
public class SimpleButtonPresenter {

    @Inject
    PlaceManager placeManager;
    @Inject
    private View view;

    @Inject
    private User loggedUser;
    @Inject
    private Event<ShowcaseSocialUserEvent> event;

    @PostConstruct
    public void init() {
        GWT.log("INIT");
    }

    @OnOpen
    public void onOpen() {
        GWT.log("OPEN");
    }

    public void fireEvent(HTMLElement targetDiv) {
        doGoTo(targetDiv);
    }

    public void doGoTo(HTMLElement a) {
        placeManager.goTo(new DefaultPlaceRequest("SimpleTimelinePresenter"),
                a);
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return "Social TimelineScreen";
    }

    @WorkbenchPartView
    public UberElement<SimpleButtonPresenter> getView() {
        return view;
    }

    public interface View extends UberElement<SimpleButtonPresenter> {

        void setupWidget(SocialTimelineWidgetModel model);
    }
}