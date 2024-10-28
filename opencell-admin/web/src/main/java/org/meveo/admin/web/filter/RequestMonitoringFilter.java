/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */

package org.meveo.admin.web.filter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.meveo.cache.MetricsConfigurationCacheContainerProvider;
import org.meveo.commons.utils.EjbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.inject.Inject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebFilter(urlPatterns = "/*")
public class RequestMonitoringFilter extends HttpFilter {

    private Logger log = LoggerFactory.getLogger(RequestMonitoringFilter.class);

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    MetricsConfigurationCacheContainerProvider metricsConfigCache;

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        long millis = System.currentTimeMillis();
        chain.doFilter(req, res);
        registerMetricsForRequest(req, millis);
    }

    private void registerMetricsForRequest(HttpServletRequest req, long startTime) {
        try {
            if (req.getRequestURI() != null) {
                String uri = req.getRequestURI();
                String contextPath = req.getContextPath();
                String name = uri.replaceAll(contextPath, "");
                if (!Strings.isNullOrEmpty(name)) {
                    String nameWithOutExtension = name.replaceAll(".jsf", "");
                    Map<String, Map<String, String>> params = new HashMap<>();
                    if (metricsConfigCache.containsKey(nameWithOutExtension)) {
                        params = metricsConfigCache.getConfiguration(nameWithOutExtension);
                    } else if (name.startsWith("/pages") && name.endsWith(".jsf") && metricsConfigCache.containsKey("*.jsf")) {
                        params = metricsConfigCache.getConfiguration("*.jsf");
                    }
                    if (params.containsKey(req.getMethod())) {
                        Map<String, String> metrics = params.get(req.getMethod());
                        String metricsType = metrics.get("metrics_type");
                        String unit = metrics.get("metrics_unit");

                        log.debug("Register {} metrics for {} in {}", metricsType, req.getMethod(), name);
                        registerMetricsForMethod(name, metricsType, startTime, unit);
                    }
                } else {
                    log.debug("Name {} in uri {} not found ", name, uri);
                }
            }
        } catch (Exception e) {
            log.error("Error when registering metrics {} ", e.getMessage());
        }
    }

    private void registerMetricsForMethod(String name, String metrics, long startTime, String unit) {
        name = metrics + name.replace("/", ".");

        if ("counter".equalsIgnoreCase(metrics)) {
            meterRegistry.counter(name, "node", EjbUtils.getCurrentClusterNode()).increment();

        } else if ("gauge".equalsIgnoreCase(metrics)) {
            AtomicInteger value = meterRegistry.gauge(name, Tags.of("node", EjbUtils.getCurrentClusterNode()), new AtomicInteger(0));
            value.set(value.get() + 1);

            // Akk migrate me

            // } else if ("histogram".equalsIgnoreCase(metrics)) {

            // Histogram histogram = registry.histogram(name);
            // long count = histogram.getCount();
            // histogram.update(count + 1);

            // } else if ("meter".equalsIgnoreCase(metrics)) {

            // registry.meter(name).mark();

        } else if ("timer".equalsIgnoreCase(metrics)) {
            createTimerMetrics(name, startTime, unit);
        } else {
            log.debug("unknown metrics {} , must from list [counter, gauge, histogram, meter, timer]", metrics);
        }
    }

    private void createTimerMetrics(String name, long startTime, String unit) {

        long end = System.currentTimeMillis();
        long duration = end - startTime;
        Timer timer = meterRegistry.timer(name, "node", EjbUtils.getCurrentClusterNode());
        // .withUnit(unit) Akk migrate me
        timer.record(Duration.ofMillis(duration));
    }
}