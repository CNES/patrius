/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive;

import fr.cnes.sirius.patrius.math.linear.RealMatrix;

/**
 * Implementation of {@link fr.cnes.sirius.patrius.math.stat.descriptive.MultivariateSummaryStatistics} that
 * is safe to use in a multithreaded environment. Multiple threads can safely
 * operate on a single instance without causing runtime exceptions due to race
 * conditions. In effect, this implementation makes modification and access
 * methods atomic operations for a single instance. That is to say, as one
 * thread is computing a statistic from the instance, no other thread can modify
 * the instance nor compute another statistic.
 * 
 * @since 1.2
 * @version $Id: SynchronizedMultivariateSummaryStatistics.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class SynchronizedMultivariateSummaryStatistics
    extends MultivariateSummaryStatistics {

    /** Serialization UID */
    private static final long serialVersionUID = 7099834153347155363L;

    /**
     * Construct a SynchronizedMultivariateSummaryStatistics instance
     * 
     * @param k
     *        dimension of the data
     * @param isCovarianceBiasCorrected
     *        if true, the unbiased sample
     *        covariance is computed, otherwise the biased population covariance
     *        is computed
     */
    public SynchronizedMultivariateSummaryStatistics(final int k, final boolean isCovarianceBiasCorrected) {
        super(k, isCovarianceBiasCorrected);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void addValue(final double[] value) {
        super.addValue(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int getDimension() {
        return super.getDimension();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long getN() {
        return super.getN();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized double[] getSum() {
        return super.getSum();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized double[] getSumSq() {
        return super.getSumSq();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized double[] getSumLog() {
        return super.getSumLog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized double[] getMean() {
        return super.getMean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized double[] getStandardDeviation() {
        return super.getStandardDeviation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized RealMatrix getCovariance() {
        return super.getCovariance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized double[] getMax() {
        return super.getMax();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized double[] getMin() {
        return super.getMin();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized double[] getGeometricMean() {
        return super.getGeometricMean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String toString() {
        return super.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void clear() {
        super.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean equals(final Object object) {
        return super.equals(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int hashCode() {
        return super.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized StorelessUnivariateStatistic[] getSumImpl() {
        return super.getSumImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setSumImpl(final StorelessUnivariateStatistic[] sumImpl) {
        super.setSumImpl(sumImpl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized StorelessUnivariateStatistic[] getSumsqImpl() {
        return super.getSumsqImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setSumsqImpl(final StorelessUnivariateStatistic[] sumsqImpl) {
        super.setSumsqImpl(sumsqImpl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized StorelessUnivariateStatistic[] getMinImpl() {
        return super.getMinImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setMinImpl(final StorelessUnivariateStatistic[] minImpl) {
        super.setMinImpl(minImpl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized StorelessUnivariateStatistic[] getMaxImpl() {
        return super.getMaxImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setMaxImpl(final StorelessUnivariateStatistic[] maxImpl) {
        super.setMaxImpl(maxImpl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized StorelessUnivariateStatistic[] getSumLogImpl() {
        return super.getSumLogImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setSumLogImpl(final StorelessUnivariateStatistic[] sumLogImpl) {
        super.setSumLogImpl(sumLogImpl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized StorelessUnivariateStatistic[] getGeoMeanImpl() {
        return super.getGeoMeanImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setGeoMeanImpl(final StorelessUnivariateStatistic[] geoMeanImpl) {
        super.setGeoMeanImpl(geoMeanImpl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized StorelessUnivariateStatistic[] getMeanImpl() {
        return super.getMeanImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setMeanImpl(final StorelessUnivariateStatistic[] meanImpl) {
        super.setMeanImpl(meanImpl);
    }
}
