/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 *
 * Copyright 2011-2017 CNES
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
 * VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour gerer un if/else dans Ellipsoid.getPointLocation()
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Value object representing the results of a univariate statistical summary.
 * 
 * @version $Id: StatisticalSummaryValues.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class StatisticalSummaryValues implements Serializable,
    StatisticalSummary {

    /** Serialization id */
    private static final long serialVersionUID = -5108854841843722536L;

    /** The sample mean */
    private final double mean;

    /** The sample variance */
    private final double variance;

    /** The number of observations in the sample */
    private final long n;

    /** The maximum value */
    private final double max;

    /** The minimum value */
    private final double min;

    /** The sum of the sample values */
    private final double sum;

    /**
     * Constructor
     * 
     * @param meanIn
     *        the sample mean
     * @param varianceIn
     *        the sample variance
     * @param nIn
     *        the number of observations in the sample
     * @param maxIn
     *        the maximum value
     * @param minIn
     *        the minimum value
     * @param sumIn
     *        the sum of the values
     */
    public StatisticalSummaryValues(final double meanIn, final double varianceIn, final long nIn,
        final double maxIn, final double minIn, final double sumIn) {
        super();
        this.mean = meanIn;
        this.variance = varianceIn;
        this.n = nIn;
        this.max = maxIn;
        this.min = minIn;
        this.sum = sumIn;
    }

    /**
     * @return Returns the max.
     */
    @Override
    public double getMax() {
        return this.max;
    }

    /**
     * @return Returns the mean.
     */
    @Override
    public double getMean() {
        return this.mean;
    }

    /**
     * @return Returns the min.
     */
    @Override
    public double getMin() {
        return this.min;
    }

    /**
     * @return Returns the number of values.
     */
    @Override
    public long getN() {
        return this.n;
    }

    /**
     * @return Returns the sum.
     */
    @Override
    public double getSum() {
        return this.sum;
    }

    /**
     * @return Returns the standard deviation
     */
    @Override
    public double getStandardDeviation() {
        final double res;
        if (this.variance >= 0. && !Double.isNaN(this.variance)) {
            res = MathLib.sqrt(this.variance);
        } else {
            res = Double.NaN;
        }
        return res;
    }

    /**
     * @return Returns the variance.
     */
    @Override
    public double getVariance() {
        return this.variance;
    }

    /**
     * Returns true iff <code>object</code> is a <code>StatisticalSummaryValues</code> instance and all statistics have
     * the same values as this.
     * 
     * @param object
     *        the object to test equality against.
     * @return true if object equals this
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof StatisticalSummaryValues)) {
            return false;
        }
        final StatisticalSummaryValues stat = (StatisticalSummaryValues) object;
        return Precision.equalsIncludingNaN(stat.getMax(), this.getMax()) &&
            Precision.equalsIncludingNaN(stat.getMean(), this.getMean()) &&
            Precision.equalsIncludingNaN(stat.getMin(), this.getMin()) &&
            Precision.equalsIncludingNaN(stat.getN(), this.getN()) &&
            Precision.equalsIncludingNaN(stat.getSum(), this.getSum()) &&
            Precision.equalsIncludingNaN(stat.getVariance(), this.getVariance());
    }

    /**
     * Returns hash code based on values of statistics
     * 
     * @return hash code
     */
    @Override
    public int hashCode() {
        final int number = 31;
        int result = number + MathUtils.hash(this.getMax());
        result = result * number + MathUtils.hash(this.getMean());
        result = result * number + MathUtils.hash(this.getMin());
        result = result * number + MathUtils.hash(this.getN());
        result = result * number + MathUtils.hash(this.getSum());
        result = result * number + MathUtils.hash(this.getVariance());
        return result;
    }

    /**
     * Generates a text report displaying values of statistics.
     * Each statistic is displayed on a separate line.
     * 
     * @return String with line feeds displaying statistics
     */
    @Override
    public String toString() {
        // Initialization
        final StringBuffer outBuffer = new StringBuffer();
        final String endl = "\n";
        outBuffer.append("StatisticalSummaryValues:").append(endl);
        outBuffer.append("n: ").append(this.getN()).append(endl);
        outBuffer.append("min: ").append(this.getMin()).append(endl);
        outBuffer.append("max: ").append(this.getMax()).append(endl);
        outBuffer.append("mean: ").append(this.getMean()).append(endl);
        outBuffer.append("std dev: ").append(this.getStandardDeviation())
            .append(endl);
        outBuffer.append("variance: ").append(this.getVariance()).append(endl);
        outBuffer.append("sum: ").append(this.getSum()).append(endl);
        // Return result
        //
        return outBuffer.toString();
    }

}
