/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

/**
 * Handler mapping for OWS services. 
 * <p>
 * This handler mapping extends a set of mappings to allow for a request to specifying a 
 * local workspace. Consider the following mappings:</p>
 * <pre>
 *   &lt;property name="mappings"&gt;
 *      &lt;props&gt;
 *              &lt;prop key="/wfs"&gt;dispatcher&lt;/prop&gt;
 *              &lt;prop key="/wfs/*"&gt;dispatcher&lt;/prop&gt;
 *      &lt;/props&gt;
 *    &lt;/property&gt;
 * </pre>
 * <p>This handler will allow the above mappings to match "workspace prefixed" requests such as:</p>
 * <pre>
 *   /topp/wfs?...
 *   /nurc/wfs?...
 * </pre>
 * <p>
 * Where "topp" and "nurc" are workspaces.
 *  </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class OWSHandlerMapping extends SimpleUrlHandlerMapping {

    Catalog catalog;
    
    public OWSHandlerMapping(Catalog catalog) {
        this.catalog = catalog;
    }
    
    @Override
    protected Object lookupHandler(String urlPath, HttpServletRequest request) throws Exception {
        Object h = super.lookupHandler(urlPath, request);
        if (h == null /*&& AdvancedDispatch.isSet(getApplicationContext())*/) {
            //check for a workspace being specified in the request and strip it off
            int i = urlPath.startsWith("/") ? 1 : 0;
            int j = urlPath.indexOf("/", i);
            if (j > i) {
                String first = urlPath.substring(i, j);
                String last = urlPath.substring(j);
                
                if (catalog.getWorkspaceByName(first) != null) {
                    String wsName = first;
                    //check for a layer being specified as well
                    j = last.indexOf("/", 1);
                    if (j != -1) {
                        first = last.substring(1, j);
                        if (catalog.getLayerByName(first) != null) {
                            //found, strip off layer and allow call to fall through
                            last = last.substring(j);
                        } else if(catalog.getLayerGroupByName(wsName, first) != null) {
                            //found, strip off layer and allow call to fall through
                            last = last.substring(j);
                        }
                    }
                    
                    h = super.lookupHandler(last, request);
                } else if(catalog.getLayerGroupByName(first) != null) {
                    LayerGroupInfo lg = catalog.getLayerGroupByName(first);
                    h = super.lookupHandler(last, request);
                }
                
            }
        }
        
        return h;
    }
}
