/*
 * #%L
 * GwtMaterial
 * %%
 * Copyright (C) 2015 - 2016 GwtMaterialDesign
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package gwt.material.design.client.ui;

import com.google.gwt.dom.client.Document;
import gwt.material.design.client.base.MaterialWidget;

//@formatter:off

/**
 * A normal panel that has some material features like : shadow and ripple effect
 * <h3>UiBinder Usage:</h3>
 * <pre>
 * {@code <m:MaterialPanel backgroundColor="WHITE" waves="BLUE" shadow="3"/>}
 * </pre>
 *
 * @author kevzlou7979
 * @author Ben Dol
 * @see <a href="http://gwtmaterialdesign.github.io/gwt-material-demo/#shadow">Material Panels</a>
 */
public class MaterialPanel extends MaterialWidget {

    public MaterialPanel() {
        super(Document.get().createDivElement());
    }

    public MaterialPanel(String... initialClass) {
        super(Document.get().createDivElement(), initialClass);
    }
}
