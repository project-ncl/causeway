/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.BaseUnits;

@ApplicationScoped
public class Meters {

    public static final String METRICS_IMPORT = "causeway.import.build";
    public static final String METRICS_UNTAG = "causeway.untag.build";

    private static final String METRICS_PUSHED_FILE_TO_BREW_KEY = "pushed-file-to-brew";
    private static final String METRICS_LOGS_NUMBER_KEY = METRICS_PUSHED_FILE_TO_BREW_KEY + ".logs.number";
    private static final String METRICS_LOGS_SIZE_KEY = METRICS_PUSHED_FILE_TO_BREW_KEY + ".logs.size";
    private static final String METRICS_ARTIFACTS_NUMBER_KEY = METRICS_PUSHED_FILE_TO_BREW_KEY + ".artifacts.number";
    private static final String METRICS_ARTIFACTS_SIZE_KEY = METRICS_PUSHED_FILE_TO_BREW_KEY + ".artifacts.size";

    private final DistributionSummary artifactsSize;
    private final DistributionSummary artifactsNumber;
    private final DistributionSummary logsSize;
    private final DistributionSummary logsNumber;

    @Inject
    public Meters(MeterRegistry registry) {
        artifactsSize = DistributionSummary.builder(METRICS_ARTIFACTS_SIZE_KEY)
                .baseUnit(BaseUnits.BYTES)
                .description("Size of uploaded artifacts")
                .register(registry);
        artifactsNumber = DistributionSummary.builder(METRICS_ARTIFACTS_NUMBER_KEY)
                .baseUnit(BaseUnits.FILES)
                .description("Number of uploaded artifacts")
                .register(registry);

        logsSize = DistributionSummary.builder(METRICS_LOGS_SIZE_KEY)
                .baseUnit(BaseUnits.BYTES)
                .description("Size of uploaded logs")
                .register(registry);
        logsNumber = DistributionSummary.builder(METRICS_LOGS_NUMBER_KEY)
                .baseUnit(BaseUnits.FILES)
                .description("Number of uploaded logs")
                .register(registry);
    }

    public void recordLogsNumber(long number) {
        logsNumber.record(number);
    }

    public void recordLogsSize(long size) {
        logsSize.record(size);
    }

    public void recordArtifactsNumber(long number) {
        artifactsNumber.record(number);
    }

    public void recordArtifactsSize(long size) {
        artifactsSize.record(size);
    }
}
