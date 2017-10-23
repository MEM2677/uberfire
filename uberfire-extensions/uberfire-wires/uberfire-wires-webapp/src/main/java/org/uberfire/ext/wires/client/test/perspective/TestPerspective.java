package org.uberfire.ext.wires.client.test.perspective;

import org.jboss.errai.common.client.dom.Div;
import org.jboss.errai.ui.client.local.api.IsElement;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.uberfire.client.annotations.WorkbenchPanel;
import org.uberfire.client.annotations.WorkbenchPerspective;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * Created by matteo on 19/10/17.
 */
@Templated("TestPerspective.html")
@Dependent
@WorkbenchPerspective(identifier = "TestPerspective")
public class TestPerspective implements IsElement {

    @Inject
    @DataField
    @WorkbenchPanel(parts = "SimpleButtonPresenter")
    Div simpleButtonPresenter;


}
