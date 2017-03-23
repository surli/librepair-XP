/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.onelogin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.geoserver.security.filter.AuthenticationCachingFilter;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Authentication Filter that add to {@link #GeoServerPreAuthenticatedUserNameFilter} the feature of {@link #GeoServerCompositeFilter} to nested
 * {@link Filter} objects
 * 
 * @author Xandros
 */
public abstract class GeoServerPreAuthenticatedCompositeUserNameFilter
        extends GeoServerPreAuthenticatedUserNameFilter {

    public final static String CACHE_KEY_ATTRIBUTE = "_geoserver_security_cache_key";

    public final static String CACHE_KEY_IDLE_SECS = "_geoserver_security_cache_key_idle_secs";

    public final static String CACHE_KEY_LIVE_SECS = "_geoserver_security_cache_key_live_secs";

    protected class NestedFilterChain implements FilterChain {
        private final FilterChain originalChain;

        private int currentPosition = 0;

        private NestedFilterChain(FilterChain chain) {
            this.originalChain = chain;
        }

        public void doFilter(final ServletRequest request, final ServletResponse response)
                throws IOException, ServletException {

            // try cache
            if (GeoServerPreAuthenticatedCompositeUserNameFilter.this instanceof AuthenticationCachingFilter
                    && currentPosition == 0) {
                String cacheKey = authenticateFromCache(
                        (AuthenticationCachingFilter) GeoServerPreAuthenticatedCompositeUserNameFilter.this,
                        (HttpServletRequest) request);
                if (cacheKey != null)
                    request.setAttribute(CACHE_KEY_ATTRIBUTE, cacheKey);
            }

            if (nestedFilters == null || currentPosition == nestedFilters.size()) {
                Authentication postAuthentication = SecurityContextHolder.getContext()
                        .getAuthentication();
                String cacheKey = (String) request.getAttribute(CACHE_KEY_ATTRIBUTE);
                if (postAuthentication != null && cacheKey != null) {
                    Integer idleSecs = (Integer) request.getAttribute(CACHE_KEY_IDLE_SECS);
                    Integer liveSecs = (Integer) request.getAttribute(CACHE_KEY_LIVE_SECS);

                    getSecurityManager().getAuthenticationCache().put(getName(), cacheKey,
                            postAuthentication, idleSecs, liveSecs);
                }
                // clean up request attributes in any case,
                request.setAttribute(CACHE_KEY_ATTRIBUTE, null);
                request.setAttribute(CACHE_KEY_IDLE_SECS, null);
                request.setAttribute(CACHE_KEY_LIVE_SECS, null);
                // GeoServerPreAuthenticatedCompositeUserNameFilter.super.doFilter(request, response, originalChain);
                originalChain.doFilter(request, response);
            } else {
                currentPosition++;
                Filter nextFilter = nestedFilters.get(currentPosition - 1);
                nextFilter.doFilter(request, response, this);
            }
        }

    }

    protected List<Filter> nestedFilters = new ArrayList<Filter>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (nestedFilters == null || nestedFilters.size() == 0) {
            chain.doFilter(request, response);
            return;
        }

        NestedFilterChain nestedChain = new NestedFilterChain(chain);
        nestedChain.doFilter(request, response);

    }

    public List<Filter> getNestedFilters() {
        return nestedFilters;
    }

    public void setNestedFilters(List<Filter> nestedFilters) {
        this.nestedFilters = nestedFilters;
    }

}
