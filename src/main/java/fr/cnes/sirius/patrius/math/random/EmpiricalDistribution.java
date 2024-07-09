/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.random;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.distribution.AbstractRealDistribution;
import fr.cnes.sirius.patrius.math.distribution.NormalDistribution;
import fr.cnes.sirius.patrius.math.distribution.RealDistribution;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.exception.ZeroException;
import fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * Represents an <a href="http://http://en.wikipedia.org/wiki/Empirical_distribution_function"> empirical probability
 * distribution</a> -- a probability distribution derived from observed data without making any assumptions about the
 * functional form of the population distribution that the data come from.
 * </p>
 * 
 * <p>
 * An <code>EmpiricalDistribution</code> maintains data structures, called <i>distribution digests</i>, that describe
 * empirical distributions and support the following operations:
 * <ul>
 * <li>loading the distribution from a file of observed data values</li>
 * <li>dividing the input data into "bin ranges" and reporting bin frequency counts (data for histogram)</li>
 * <li>reporting univariate statistics describing the full set of data values as well as the observations within each
 * bin</li>
 * <li>generating random values from the distribution</li>
 * </ul>
 * Applications can use <code>EmpiricalDistribution</code> to build grouped frequency histograms representing the input
 * data or to generate random values "like" those in the input file -- i.e., the values generated will follow the
 * distribution of the values in the file.
 * </p>
 * 
 * <p>
 * The implementation uses what amounts to the <a
 * href="http://nedwww.ipac.caltech.edu/level5/March02/Silverman/Silver2_6.html"> Variable Kernel Method</a> with
 * Gaussian smoothing:
 * <p>
 * <strong>Digesting the input file</strong>
 * <ol>
 * <li>Pass the file once to compute min and max.</li>
 * <li>Divide the range from min-max into <code>binCount</code> "bins."</li>
 * <li>Pass the data file again, computing bin counts and univariate statistics (mean, std dev.)
 *  for each of the bins</li>
 * <li>Divide the interval (0,1) into subintervals associated with the bins, with the length of a bin's subinterval
 * proportional to its count.</li>
 * </ol>
 * <strong>Generating random values from the distribution</strong>
 * <ol>
 * <li>Generate a uniformly distributed value in (0,1)</li>
 * <li>Select the subinterval to which the value belongs.
 * <li>Generate a random Gaussian value with mean = mean of the associated bin and std dev = std dev of
 *  associated bin.</li>
 * </ol>
 * </p>
 * 
 * <p>
 * EmpiricalDistribution implements the {@link RealDistribution} interface as follows. Given x within the range of
 * values in the dataset, let B be the bin containing x and let K be the within-bin kernel for B. Let P(B-) be the sum
 * of the probabilities of the bins below B and let K(B) be the mass of B under K (i.e., the integral of the kernel
 * density over B). Then set P(X < x) = P(B-) + P(B) * K(x) / K(B) where K(x) is the kernel distribution evaluated at x.
 * This results in a cdf that matches the grouped frequency distribution at the bin endpoints and interpolates within
 * bins using within-bin kernels.
 * </p>
 * 
 * <strong>USAGE NOTES:</strong>
 * <ul>
 * <li>The <code>binCount</code> is set by default to 1000. A good rule of thumb is to set the bin count to
 * approximately the length of the input file divided by 10.</li>
 * <li>The input file <i>must</i> be a plain text file containing one valid numeric entry per line.</li>
 * </ul>
 * </p>
 * 
 * @version $Id: EmpiricalDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.NullAssignment")
public class EmpiricalDistribution extends AbstractRealDistribution {

    /** Default bin count */
    public static final int DEFAULT_BIN_COUNT = 1000;

    /** Character set for file input */
    private static final String FILE_CHARSET = "US-ASCII";

    /** Serializable version identifier */
    private static final long serialVersionUID = 5729073523949762654L;

    /** List of SummaryStatistics objects characterizing the bins */
    private final List<SummaryStatistics> binStats;

    /** Sample statistics */
    private SummaryStatistics sampleStats = null;

    /** Max loaded value */
    private double max = Double.NEGATIVE_INFINITY;

    /** Min loaded value */
    private double min = Double.POSITIVE_INFINITY;

    /** Grid size */
    private double delta = 0d;

    /** number of bins */
    private final int binCount;

    /** is the distribution loaded? */
    private boolean loaded = false;

    /** upper bounds of subintervals in (0,1) "belonging" to the bins */
    private double[] upperBounds = null;

    /** RandomDataGenerator instance to use in repeated calls to getNext() */
    private final RandomDataGenerator randomData;

    /**
     * Creates a new EmpiricalDistribution with the default bin count.
     */
    public EmpiricalDistribution() {
        this(DEFAULT_BIN_COUNT);
    }

    /**
     * Creates a new EmpiricalDistribution with the specified bin count.
     * 
     * @param binCountIn
     *        number of bins
     */
    public EmpiricalDistribution(final int binCountIn) {
        this(binCountIn, new RandomDataGenerator());
    }

    /**
     * Creates a new EmpiricalDistribution with the specified bin count using the
     * provided {@link RandomGenerator} as the source of random data.
     * 
     * @param binCountIn
     *        number of bins
     * @param generator
     *        random data generator (may be null, resulting in default JDK generator)
     * @since 3.0
     */
    public EmpiricalDistribution(final int binCountIn, final RandomGenerator generator) {
        this(binCountIn, new RandomDataGenerator(generator));
    }

    /**
     * Creates a new EmpiricalDistribution with default bin count using the
     * provided {@link RandomGenerator} as the source of random data.
     * 
     * @param generator
     *        random data generator (may be null, resulting in default JDK generator)
     * @since 3.0
     */
    public EmpiricalDistribution(final RandomGenerator generator) {
        this(DEFAULT_BIN_COUNT, generator);
    }

    /**
     * Private constructor to allow lazy initialisation of the RNG contained
     * in the {@link #randomData} instance variable.
     * 
     * @param binCountIn
     *        number of bins
     * @param randomDataIn
     *        Random data generator.
     */
    private EmpiricalDistribution(final int binCountIn,
        final RandomDataGenerator randomDataIn) {
        super(null);
        this.binCount = binCountIn;
        this.randomData = randomDataIn;
        this.binStats = new ArrayList<SummaryStatistics>();
    }

    /**
     * Computes the empirical distribution from the provided
     * array of numbers.
     * 
     * @param array
     *        the input data array
     * @exception NullArgumentException
     *            if in is null
     */
    public void load(final double[] array) {
        final AbstractDataAdapter da = new ArrayDataAdapter(array);
        try {
            da.computeStats();
            // new adapter for the second pass
            this.fillBinStats(new ArrayDataAdapter(array));
        } catch (final IOException ex) {
            // Can't happen
            throw new MathInternalError(ex);
        }
        this.loaded = true;

    }

    /**
     * Computes the empirical distribution using data read from a URL.
     * 
     * <p>
     * The input file <i>must</i> be an ASCII text file containing one valid numeric entry per line.
     * </p>
     * 
     * @param url
     *        url of the input file
     * 
     * @throws IOException
     *         if an IO error occurs
     * @throws NullArgumentException
     *         if url is null
     * @throws ZeroException
     *         if URL contains no data
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void load(final URL url) throws IOException {
        // Check not null
        MathUtils.checkNotNull(url);
        
        // Initialization
        final Charset charset = Charset.forName(FILE_CHARSET);
        BufferedReader in =
            new BufferedReader(new InputStreamReader(url.openStream(), charset));
        try {
            final AbstractDataAdapter da = new StreamDataAdapter(in);
            da.computeStats();
            if (this.sampleStats.getN() == 0) {
                throw new ZeroException(PatriusMessages.URL_CONTAINS_NO_DATA, url);
            }
            // new adapter for the second pass
            in = new BufferedReader(new InputStreamReader(url.openStream(), charset));
            this.fillBinStats(new StreamDataAdapter(in));
            this.loaded = true;
        } finally {
            try {
                in.close();
            } catch (final IOException ex) {
                // ignore
            }
        }
    }

    /**
     * Computes the empirical distribution from the input file.
     * 
     * <p>
     * The input file <i>must</i> be an ASCII text file containing one valid numeric entry per line.
     * </p>
     * 
     * @param file
     *        the input file
     * @throws IOException
     *         if an IO error occurs
     * @throws NullArgumentException
     *         if file is null
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void load(final File file) throws IOException {
        // CHeck not null
        MathUtils.checkNotNull(file);
        // Initialization
        final Charset charset = Charset.forName(FILE_CHARSET);
        InputStream is = new FileInputStream(file);
        BufferedReader in = new BufferedReader(new InputStreamReader(is, charset));
        try {
            final AbstractDataAdapter da = new StreamDataAdapter(in);
            da.computeStats();
            // new adapter for second pass
            is = new FileInputStream(file);
            in = new BufferedReader(new InputStreamReader(is, charset));
            this.fillBinStats(new StreamDataAdapter(in));
            this.loaded = true;
        } finally {
            try {
                in.close();
            } catch (final IOException ex) {
                // ignore
            }
        }
    }

    /**
     * Fills binStats array (second pass through data file).
     * 
     * @param da
     *        object providing access to the data
     * @throws IOException
     *         if an IO error occurs
     */
    private void fillBinStats(final AbstractDataAdapter da) throws IOException {
        // Set up grid
        this.min = this.sampleStats.getMin();
        this.max = this.sampleStats.getMax();
        this.delta = (this.max - this.min) / (Double.valueOf(this.binCount)).doubleValue();

        // Initialize binStats ArrayList
        if (!this.binStats.isEmpty()) {
            this.binStats.clear();
        }
        for (int i = 0; i < this.binCount; i++) {
            final SummaryStatistics stats = new SummaryStatistics();
            this.binStats.add(i, stats);
        }

        // Filling data in binStats Array
        da.computeBinStats();

        // Assign upperBounds based on bin counts
        this.upperBounds = new double[this.binCount];
        this.upperBounds[0] =
            ((double) this.binStats.get(0).getN()) / (double) this.sampleStats.getN();
        for (int i = 1; i < this.binCount - 1; i++) {
            this.upperBounds[i] = this.upperBounds[i - 1] +
                ((double) this.binStats.get(i).getN()) / (double) this.sampleStats.getN();
        }
        this.upperBounds[this.binCount - 1] = 1.0d;
    }

    /**
     * Returns the index of the bin to which the given value belongs
     * 
     * @param value
     *        the value whose bin we are trying to find
     * @return the index of the bin containing the value
     */
    private int findBin(final double value) {
        return MathLib.min(
            MathLib.max((int) MathLib.ceil((value - this.min) / this.delta) - 1, 0),
            this.binCount - 1);
    }

    /**
     * Generates a random value from this distribution.
     * <strong>Preconditions:</strong>
     * <ul>
     * <li>the distribution must be loaded before invoking this method</li>
     * </ul>
     * 
     * @return the random value.
     * @throws MathIllegalStateException
     *         if the distribution has not been loaded
     */
    public double getNextValue() {

        if (!this.loaded) {
            throw new MathIllegalStateException(PatriusMessages.DISTRIBUTION_NOT_LOADED);
        }

        // Start with a uniformly distributed random number in (0,1)
        final double x = this.randomData.nextUniform(0, 1);

        // Use this to select the bin and generate a Gaussian within the bin
        for (int i = 0; i < this.binCount; i++) {
            if (x <= this.upperBounds[i]) {
                final SummaryStatistics stats = this.binStats.get(i);
                if (stats.getN() > 0) {
                    if (stats.getStandardDeviation() > 0) {
                        // more than one obs
                        return this.randomData.nextGaussian(stats.getMean(),
                            stats.getStandardDeviation());
                    } else {
                        // only one obs in bin
                        return stats.getMean();
                    }
                }
            }
        }
        throw new MathIllegalStateException(PatriusMessages.NO_BIN_SELECTED);
    }

    /**
     * Returns a {@link StatisticalSummary} describing this distribution.
     * <strong>Preconditions:</strong>
     * <ul>
     * <li>the distribution must be loaded before invoking this method</li>
     * </ul>
     * 
     * @return the sample statistics
     * @throws IllegalStateException
     *         if the distribution has not been loaded
     */
    public StatisticalSummary getSampleStats() {
        return this.sampleStats;
    }

    /**
     * Returns the number of bins.
     * 
     * @return the number of bins.
     */
    public int getBinCount() {
        return this.binCount;
    }

    /**
     * Returns a List of {@link SummaryStatistics} instances containing
     * statistics describing the values in each of the bins. The list is
     * indexed on the bin number.
     * 
     * @return List of bin statistics.
     */
    public List<SummaryStatistics> getBinStats() {
        return this.binStats;
    }

    /**
     * <p>
     * Returns a fresh copy of the array of upper bounds for the bins. Bins are: <br/>
     * [min,upperBounds[0]],(upperBounds[0],upperBounds[1]],..., (upperBounds[binCount-2], upperBounds[binCount-1] =
     * max].
     * </p>
     * 
     * <p>
     * Note: In versions 1.0-2.0 of commons-math, this method incorrectly returned the array of probability generator
     * upper bounds now returned by {@link #getGeneratorUpperBounds()}.
     * </p>
     * 
     * @return array of bin upper bounds
     * @since 2.1
     */
    public double[] getUpperBounds() {
        final double[] binUpperBounds = new double[this.binCount];
        for (int i = 0; i < this.binCount - 1; i++) {
            binUpperBounds[i] = this.min + this.delta * (i + 1);
        }
        binUpperBounds[this.binCount - 1] = this.max;
        return binUpperBounds;
    }

    /**
     * <p>
     * Returns a fresh copy of the array of upper bounds of the subintervals of [0,1] used in generating data from the
     * empirical distribution. Subintervals correspond to bins with lengths proportional to bin counts.
     * </p>
     * 
     * <p>
     * In versions 1.0-2.0 of commons-math, this array was (incorrectly) returned by {@link #getUpperBounds()}.
     * </p>
     * 
     * @since 2.1
     * @return array of upper bounds of subintervals used in data generation
     */
    public double[] getGeneratorUpperBounds() {
        final int len = this.upperBounds.length;
        final double[] out = new double[len];
        System.arraycopy(this.upperBounds, 0, out, 0, len);
        return out;
    }

    /**
     * Property indicating whether or not the distribution has been loaded.
     * 
     * @return true if the distribution has been loaded
     */
    public boolean isLoaded() {
        return this.loaded;
    }

    /**
     * Reseeds the random number generator used by {@link #getNextValue()}.
     * 
     * @param seed
     *        random generator seed
     * @since 3.0
     */
    public void reSeed(final long seed) {
        this.randomData.reSeed(seed);
    }

    // Distribution methods ---------------------------

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public double probability(final double x) {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Returns the kernel density normalized so that its integral over each bin equals the bin mass.
     * </p>
     * 
     * <p>
     * Algorithm description:
     * <ol>
     * <li>Find the bin B that x belongs to.</li>
     * <li>Compute K(B) = the mass of B with respect to the within-bin kernel (i.e., the integral of the kernel density
     * over B).</li>
     * <li>Return k(x) * P(B) / K(B), where k is the within-bin kernel density and P(B) is the mass of B.</li>
     * </ol>
     * </p>
     * 
     * @since 3.1
     */
    @Override
    public double density(final double x) {
        if (x < this.min || x > this.max) {
            return 0d;
        }
        final int binIndex = this.findBin(x);
        final RealDistribution kernel = this.getKernel(this.binStats.get(binIndex));
        return kernel.density(x) * this.pBi(binIndex) / this.kBi(binIndex);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Algorithm description:
     * <ol>
     * <li>Find the bin B that x belongs to.</li>
     * <li>Compute P(B) = the mass of B and P(B-) = the combined mass of the bins below B.</li>
     * <li>Compute K(B) = the probability mass of B with respect to the within-bin kernel and K(B-) = the kernel
     * distribution evaluated at the lower endpoint of B</li>
     * <li>Return P(B-) + P(B) * [K(x) - K(B-)] / K(B) where K(x) is the within-bin kernel distribution function
     * evaluated at x.</li>
     * </ol>
     * </p>
     * 
     * @since 3.1
     */
    @Override
    public double cumulativeProbability(final double x) {
        // Initialize result variable
        final double res;
        if (x < this.min) {
            // Lower bound
            res = 0d;
        } else if (x >= this.max) {
            // Upper bound
            res = 1d;
        } else {
            // Generic case
            final int binIndex = this.findBin(x);
            final double pBminus = this.pBminus(binIndex);
            final double pB = this.pBi(binIndex);
            final double[] binBounds = this.getUpperBounds();
            final double kB = this.kBi(binIndex);
            final double lower = binIndex == 0 ? this.min : binBounds[binIndex - 1];
            final RealDistribution kernel = this.wbk(x);
            final double withinBinCum =
                (kernel.cumulativeProbability(x) - kernel.cumulativeProbability(lower)) / kB;
            // res = P(B-) + P(B) * [K(x) - K(B-)] / K(B)
            res = pBminus + pB * withinBinCum;
        }
        return res;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Algorithm description:
     * <ol>
     * <li>Find the smallest i such that the sum of the masses of the bins through i is at least p.</li>
     * <li>
     * Let K be the within-bin kernel distribution for bin i.</br> Let K(B) be the mass of B under K. <br/>
     * Let K(B-) be K evaluated at the lower endpoint of B (the combined mass of the bins below B under K).<br/>
     * Let P(B) be the probability of bin i.<br/>
     * Let P(B-) be the sum of the bin masses below bin i. <br/>
     * Let pCrit = p - P(B-)<br/>
     * <li>Return the inverse of K evaluated at <br/>
     * K(B-) + pCrit * K(B) / P(B)</li>
     * </ol>
     * </p>
     * 
     * @since 3.1
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public double inverseCumulativeProbability(final double p) {
        // CHECKSTYLE: resume ReturnCount check
        if (p < 0.0 || p > 1.0) {
            // Exception
            throw new OutOfRangeException(p, 0, 1);
        }

        if (p == 0.0) {
            // Exception
            return this.getSupportLowerBound();
        }

        if (p == 1.0) {
            // Exception
            return this.getSupportUpperBound();
        }

        // Find index matching probability
        int i = 0;
        while (this.cumBinP(i) < p) {
            i++;
        }

        final double[] binBounds = this.getUpperBounds();
        final double lower = i == 0 ? this.min : binBounds[i - 1];
        final double pBminus = this.pBminus(i);
        final double pCrit = p - pBminus;
        if (pCrit <= 0) {
            return lower;
        }
        // Return result
        final RealDistribution kernel = this.getKernel(this.binStats.get(i));
        final double kB = this.kBi(i);
        final double kBminus = kernel.cumulativeProbability(lower);
        final double pB = this.pBi(i);
        return kernel.inverseCumulativeProbability(kBminus + pCrit * kB / pB);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public double getNumericalMean() {
        return this.sampleStats.getMean();
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public double getNumericalVariance() {
        return this.sampleStats.getVariance();
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public double getSupportLowerBound() {
        return this.min;
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public double getSupportUpperBound() {
        return this.max;
    }

    /**
     * Returns true if support contains lower bound.
     * 
     * @return true
     * @since 3.1
     */
    public boolean isSupportLowerBoundInclusive() {
        return true;
    }

    /**
     * Returns true if support contains upper bound.
     * 
     * @return true
     * @since 3.1
     */
    public boolean isSupportUpperBoundInclusive() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public boolean isSupportConnected() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public double sample() {
        return this.getNextValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public void reseedRandomGenerator(final long seed) {
        this.randomData.reSeed(seed);
    }

    /**
     * The probability of bin i.
     * 
     * @param i
     *        the index of the bin
     * @return the probability that selection begins in bin i
     */
    private double pBi(final int i) {
        return i == 0 ? this.upperBounds[0] :
            this.upperBounds[i] - this.upperBounds[i - 1];
    }

    /**
     * The combined probability of the bins up to but not including bin i.
     * 
     * @param i
     *        the index of the bin
     * @return the probability that selection begins in a bin below bin i.
     */
    private double pBminus(final int i) {
        return i == 0 ? 0 : this.upperBounds[i - 1];
    }

    /**
     * Mass of bin i under the within-bin kernel of the bin.
     * 
     * @param i
     *        index of the bin
     * @return the difference in the within-bin kernel cdf between the
     *         upper and lower endpoints of bin i
     */
    private double kBi(final int i) {
        final double[] binBounds = this.getUpperBounds();
        final RealDistribution kernel = this.getKernel(this.binStats.get(i));
        return i == 0 ? kernel.probability(this.min, binBounds[0]) :
            kernel.probability(binBounds[i - 1], binBounds[i]);
    }

    /**
     * The within-bin kernel of the bin that x belongs to.
     * 
     * @param x
     *        the value to locate within a bin
     * @return the within-bin kernel of the bin containing x
     */
    private RealDistribution wbk(final double x) {
        final int binIndex = this.findBin(x);
        return this.getKernel(this.binStats.get(binIndex));
    }

    /**
     * The combined probability of the bins up to and including binIndex.
     * 
     * @param binIndex
     *        maximum bin index
     * @return sum of the probabilities of bins through binIndex
     */
    private double cumBinP(final int binIndex) {
        return this.upperBounds[binIndex];
    }

    /**
     * The within-bin smoothing kernel.
     * 
     * @param bStats
     *        summary statistics for the bin
     * @return within-bin kernel parameterized by bStats
     */
    private RealDistribution getKernel(final SummaryStatistics bStats) {
        // For now, hard-code Gaussian (only kernel supported)
        return new NormalDistribution(
            bStats.getMean(), bStats.getStandardDeviation());
    }

    /**
     * Provides methods for computing <code>sampleStats</code> and <code>beanStats</code> abstracting the source of
     * data.
     */
    private abstract class AbstractDataAdapter {

        /**
         * Compute bin stats.
         * 
         * @throws IOException
         *         if an error occurs computing bin stats
         */
        public abstract void computeBinStats() throws IOException;

        /**
         * Compute sample statistics.
         * 
         * @throws IOException
         *         if an error occurs computing sample stats
         */
        public abstract void computeStats() throws IOException;

    }

    /**
     * <code>DataAdapter</code> for data provided through some input stream
     */
    private class StreamDataAdapter extends AbstractDataAdapter {

        /** Input stream providing access to the data */
        private BufferedReader inputStream;

        /**
         * Create a StreamDataAdapter from a BufferedReader
         * 
         * @param in
         *        BufferedReader input stream
         */
        public StreamDataAdapter(final BufferedReader in) {
            super();
            this.inputStream = in;
        }

        /** {@inheritDoc} */
        @Override
        public void computeBinStats() throws IOException {
            String str = this.inputStream.readLine();
            double val = 0.0d;
            while (str != null) {
                val = Double.parseDouble(str);
                final SummaryStatistics stats =
                    EmpiricalDistribution.this.binStats.get(EmpiricalDistribution.this.findBin(val));
                stats.addValue(val);
                str = this.inputStream.readLine();
            }

            this.inputStream.close();
            this.inputStream = null;
        }

        /** {@inheritDoc} */
        @Override
        public void computeStats() throws IOException {
            String str = this.inputStream.readLine();
            double val = 0.0;
            EmpiricalDistribution.this.sampleStats = new SummaryStatistics();
            while (str != null) {
                val = Double.valueOf(str);
                EmpiricalDistribution.this.sampleStats.addValue(val);
                str = this.inputStream.readLine();
            }
            this.inputStream.close();
            this.inputStream = null;
        }
    }

    /**
     * <code>DataAdapter</code> for data provided as array of doubles.
     */
    private class ArrayDataAdapter extends AbstractDataAdapter {

        /** Array of input data values */
        private final double[] inputArray;

        /**
         * Construct an ArrayDataAdapter from a double[] array
         * 
         * @param in
         *        double[] array holding the data
         * @throws NullArgumentException
         *         if in is null
         */
        public ArrayDataAdapter(final double[] in) {
            super();
            MathUtils.checkNotNull(in);
            this.inputArray = in;
        }

        /** {@inheritDoc} */
        @Override
        public void computeStats() throws IOException {
            EmpiricalDistribution.this.sampleStats = new SummaryStatistics();
            for (final double element : this.inputArray) {
                EmpiricalDistribution.this.sampleStats.addValue(element);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void computeBinStats() throws IOException {
            for (final double element : this.inputArray) {
                final SummaryStatistics stats =
                    EmpiricalDistribution.this.binStats.get(EmpiricalDistribution.this.findBin(element));
                stats.addValue(element);
            }
        }
    }
}
