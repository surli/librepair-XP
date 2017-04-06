package org.geoserver.rest;

import freemarker.template.ObjectWrapper;
import org.geoserver.rest.catalog.CatalogController;
import org.geoserver.config.*;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Settings controller
 *
 * Provides access to global settings, local settings, and contact info
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/settings")
public class SettingsController extends GeoServerController {

    @Autowired
    public SettingsController(GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
    public RestWrapper<GeoServerInfo> getGlobalSettings() {
        return wrapObject(geoServer.getGlobal(), GeoServerInfo.class);
    }

    @PutMapping(consumes = {
            MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public void setGlobalSettings(@RequestBody GeoServerInfo geoServerInfo) {
        GeoServerInfo original = geoServer.getGlobal();
        OwsUtils.copy(geoServerInfo, original, GeoServerInfo.class);
        geoServer.save(original);
    }

    @GetMapping(value = "/contact", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
    public RestWrapper<ContactInfo> getContact() {
        if (geoServer.getSettings().getContact() == null) {
            throw new ResourceNotFoundException("No contact information available");
        }
        return wrapObject(geoServer.getGlobal().getSettings().getContact(), ContactInfo.class);
    }

    @PutMapping(value = "/contact", consumes = {
            MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public void setContact(@RequestBody ContactInfo contactInfo) {
        GeoServerInfo geoServerInfo = geoServer.getGlobal();
        ContactInfo original = geoServerInfo.getSettings().getContact();
        OwsUtils.copy(contactInfo, original, ContactInfo.class);
        geoServer.save(geoServerInfo);
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ContactInfo.class.isAssignableFrom(methodParameter.getParameterType()) ||
                GeoServerInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<>(clazz, Arrays.asList(JAIInfo.class, CoverageAccessInfo.class));
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setHideFeatureTypeAttributes();
        persister.getXStream().alias("contact", ContactInfo.class);
    }

}
