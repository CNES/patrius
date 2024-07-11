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
package fr.cnes.sirius.patrius.math.random;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Collection;

import fr.cnes.sirius.patrius.math.distribution.BetaDistribution;
import fr.cnes.sirius.patrius.math.distribution.BinomialDistribution;
import fr.cnes.sirius.patrius.math.distribution.CauchyDistribution;
import fr.cnes.sirius.patrius.math.distribution.ChiSquaredDistribution;
import fr.cnes.sirius.patrius.math.distribution.ExponentialDistribution;
import fr.cnes.sirius.patrius.math.distribution.FDistribution;
import fr.cnes.sirius.patrius.math.distribution.GammaDistribution;
import fr.cnes.sirius.patrius.math.distribution.HypergeometricDistribution;
import fr.cnes.sirius.patrius.math.distribution.PascalDistribution;
import fr.cnes.sirius.patrius.math.distribution.PoissonDistribution;
import fr.cnes.sirius.patrius.math.distribution.TDistribution;
import fr.cnes.sirius.patrius.math.distribution.WeibullDistribution;
import fr.cnes.sirius.patrius.math.distribution.ZipfDistribution;
import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.exception.NotANumberException;
import fr.cnes.sirius.patrius.math.exception.NotFiniteNumberException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Uses a {@link RandomGenerator} instance to generate non-secure data and a {@link java.security.SecureRandom} instance
 * to provide data for the <code>nextSecureXxx</code> methods. If no <code>RandomGenerator</code> is provided in the
 * constructor, the default is
 * to use a {@link Well19937c} generator. To plug in a different
 * implementation, either implement <code>RandomGenerator</code> directly or
 * extend {@link AbstractRandomGenerator}.
 * <p>
 * Supports reseeding the underlying pseudo-random number generator (PRNG). The <code>SecurityProvider</code> and
 * <code>Algorithm</code> used by the <code>SecureRandom</code> instance can also be reset.
 * </p>
 * <p>
 * For details on the default PRNGs, see {@link java.util.Random} and {@link java.security.SecureRandom}.
 * </p>
 * <p>
 * <strong>Usage Notes</strong>:
 * <ul>
 * <li>
 * Instance variables are used to maintain <code>RandomGenerator</code> and <code>SecureRandom</code> instances used in
 * data generation. Therefore, to generate a random sequence of values or strings, you should use just
 * <strong>one</strong> <code>RandomDataImpl</code> instance repeatedly.</li>
 * <li>
 * The "secure" methods are *much* slower. These should be used only when a cryptographically secure random sequence is
 * required. A secure random sequence is a sequence of pseudo-random values which, in addition to being well-dispersed
 * (so no subsequence of values is an any more likely than other subsequence of the the same length), also has the
 * additional property that knowledge of values generated up to any point in the sequence does not make it any easier to
 * predict subsequent values.</li>
 * <li>
 * When a new <code>RandomDataImpl</code> is created, the underlying random number generators are <strong>not</strong>
 * initialized. If you do not explicitly seed the default non-secure generator, it is seeded with the current time in
 * milliseconds plus the system identity hash code on first use. The same holds for the secure generator. If you provide
 * a <code>RandomGenerator</code> to the constructor, however, this generator is not reseeded by the constructor nor is
 * it reseeded on first use.</li>
 * <li>
 * The <code>reSeed</code> and <code>reSeedSecure</code> methods delegate to the corresponding methods on the underlying
 * <code>RandomGenerator</code> and <code>SecureRandom</code> instances. Therefore, <code>reSeed(long)</code> fully
 * resets the initial state of the non-secure random number generator (so that reseeding with a specific value always
 * results in the same subsequent random sequence); whereas reSeedSecure(long) does <strong>not</strong> reinitialize
 * the secure random number generator (so secure sequences started with calls to reseedSecure(long) won't be identical).
 * </li>
 * <li>
 * This implementation is not synchronized. The underlying <code>RandomGenerator</code> or <code>SecureRandom</code>
 * instances are not protected by synchronization and are not guaranteed to be thread-safe. Therefore, if an instance of
 * this class is concurrently utilized by multiple threads, it is the responsibility of client code to synchronize
 * access to seeding and data generation methods.</li>
 * </ul>
 * </p>
 * 
 * @since 3.1
 * @version $Id: RandomDataGenerator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class RandomDataGenerator implements Serializable {

    /** 40. */
    private static final int FOURTY = 40;

    /** Generator length. */
    private static final int GENERATOR_LENGTH = 40;

    /** Half byte (128). */
    private static final int HALF_BYTE = 128;

    /** Serializable version identifier */
    private static final long serialVersionUID = -626730818244969716L;

    /** Zero. */
    private static final String ZERO = "0";

    /** underlying random number generator */
    private RandomGenerator rand = null;

    /** underlying secure random number generator */
    private SecureRandom secRand = null;

    /**
     * Construct a RandomDataGenerator, using a default random generator as the source
     * of randomness.
     * 
     * <p>
     * The default generator is a {@link Well19937c} seeded with
     * {@code System.currentTimeMillis() + System.identityHashCode(this))}. The generator is initialized and seeded on
     * first use.
     * </p>
     */
    public RandomDataGenerator() {
        // Nothing to do
    }

    /**
     * Construct a RandomDataGenerator using the supplied {@link RandomGenerator} as
     * the source of (non-secure) random data.
     * 
     * @param randIn
     *        the source of (non-secure) random data
     *        (may be null, resulting in the default generator)
     */
    public RandomDataGenerator(final RandomGenerator randIn) {
        this.rand = randIn;
    }

    /**
     * <p>
     * <strong>Algorithm Description:</strong> hex strings are generated using a 2-step process.
     * <ol>
     * <li>{@code len / 2 + 1} binary bytes are generated using the underlying Random</li>
     * <li>Each binary byte is translated into 2 hex digits</li>
     * </ol>
     * </p>
     * 
     * @param len
     *        the desired string length.
     * @return the random string.
     * @throws NotStrictlyPositiveException
     *         if {@code len <= 0}.
     */
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    public String nextHexString(final int len) {
        if (len <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.LENGTH, len);
        }

        // Get a random number generator
        final RandomGenerator ran = this.getRan();

        // Initialize output buffer
        final StringBuilder outBuffer = new StringBuilder();

        // Get int(len/2)+1 random bytes
        final byte[] randomBytes = new byte[(len / 2) + 1];
        ran.nextBytes(randomBytes);

        // Convert each byte to 2 hex digits
        for (final byte randomByte : randomBytes) {
            final Integer c = Integer.valueOf(randomByte);

            /*
             * Add 128 to byte value to make interval 0-255 before doing hex
             * conversion. This guarantees <= 2 hex digits from toHexString()
             * toHexString would otherwise add 2^32 to negative arguments.
             */
            String hex = Integer.toHexString(c.intValue() + HALF_BYTE);

            // Make sure we add 2 hex digits for each byte
            if (hex.length() == 1) {
                hex = ZERO + hex;
            }
            outBuffer.append(hex);
        }
        return outBuffer.toString().substring(0, len);
    }

    /**
     * Returns random int in [lower, upper] (uniform distribution).
     * 
     * @param lower
     *        lower bound
     * @param upper
     *        upper bound
     * @return random int in [lower, upper]
     * @throws NumberIsTooLargeException
     *         thrown if lower >= upper
     */
    public int nextInt(final int lower, final int upper) {
        if (lower >= upper) {
            throw new NumberIsTooLargeException(PatriusMessages.LOWER_BOUND_NOT_BELOW_UPPER_BOUND,
                lower, upper, false);
        }
        final double r = this.getRan().nextDouble();
        final double scaled = r * upper + (1.0 - r) * lower + r;
        return (int) MathLib.floor(scaled);
    }

    /**
     * Returns random long in [lower, upper] (uniform distribution).
     * 
     * @param lower
     *        lower bound
     * @param upper
     *        upper bound
     * @return random long in [lower, upper]
     * @throws NumberIsTooLargeException
     *         thrown if lower >= upper
     */
    public long nextLong(final long lower, final long upper) {
        if (lower >= upper) {
            throw new NumberIsTooLargeException(PatriusMessages.LOWER_BOUND_NOT_BELOW_UPPER_BOUND,
                lower, upper, false);
        }
        final double r = this.getRan().nextDouble();
        final double scaled = r * upper + (1.0 - r) * lower + r;
        return (long) MathLib.floor(scaled);
    }

    /**
     * <p>
     * <strong>Algorithm Description:</strong> hex strings are generated in 40-byte segments using a 3-step process.
     * <ol>
     * <li>
     * 20 random bytes are generated using the underlying <code>SecureRandom</code>.</li>
     * <li>
     * SHA-1 hash is applied to yield a 20-byte binary digest.</li>
     * <li>
     * Each byte of the binary digest is converted to 2 hex digits.</li>
     * </ol>
     * </p>
     * 
     * @param len
     *        String length
     * @throws NotStrictlyPositiveException
     *         if {@code len <= 0}
     * @return random String
     */
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    public String nextSecureHexString(final int len) {
        if (len <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.LENGTH, len);
        }

        // Setup Digest provider
        MessageDigest alg = null;
        try {
            alg = MessageDigest.getInstance("SHA-1");
        } catch (final NoSuchAlgorithmException ex) {
            // this should never happen
            throw new MathInternalError(ex);
        }
        alg.reset();

        // Get SecureRandom
        final SecureRandom secRan = this.getSecRan();

        // Compute number of iterations required (40 bytes each)
        final int numIter = (len / FOURTY) + 1;

        final StringBuilder outBuffer = new StringBuilder();
        for (int iter = 1; iter < numIter + 1; iter++) {
            final byte[] randomBytes = new byte[GENERATOR_LENGTH];
            secRan.nextBytes(randomBytes);
            alg.update(randomBytes);

            // Compute hash -- will create 20-byte binary hash
            final byte[] hash = alg.digest();

            // Loop over the hash, converting each byte to 2 hex digits
            for (final byte element : hash) {
                final Integer c = Integer.valueOf(element);

                /*
                 * Add 128 to byte value to make interval 0-255 This guarantees
                 * <= 2 hex digits from toHexString() toHexString would
                 * otherwise add 2^32 to negative arguments
                 */
                String hex = Integer.toHexString(c.intValue() + HALF_BYTE);

                // Keep strings uniform length -- guarantees 40 bytes
                if (hex.length() == 1) {
                    hex = ZERO + hex;
                }
                outBuffer.append(hex);
            }
        }
        return outBuffer.toString().substring(0, len);
    }

    /**
     * Returns random int in [lower, upper] (uniform distribution). Algorithm is secured.
     * 
     * @param lower
     *        lower bound
     * @param upper
     *        upper bound
     * @return random int in [lower, upper]
     * @throws NumberIsTooLargeException
     *         thrown if lower >= upper
     * @see <code>SecureRandom</code>
     */
    public int nextSecureInt(final int lower, final int upper) {
        if (lower >= upper) {
            throw new NumberIsTooLargeException(PatriusMessages.LOWER_BOUND_NOT_BELOW_UPPER_BOUND,
                lower, upper, false);
        }
        final SecureRandom sec = this.getSecRan();
        final double r = sec.nextDouble();
        final double scaled = r * upper + (1.0 - r) * lower + r;
        return (int) MathLib.floor(scaled);
    }

    /**
     * Returns random int in [lower, upper] (uniform distribution). Algorithm is secured.
     * 
     * @param lower
     *        lower bound
     * @param upper
     *        upper bound
     * @return random int in [lower, upper]
     * @throws NumberIsTooLargeException
     *         thrown if lower >= upper
     * @see <code>SecureRandom</code>
     */
    public long nextSecureLong(final long lower, final long upper) {
        if (lower >= upper) {
            throw new NumberIsTooLargeException(PatriusMessages.LOWER_BOUND_NOT_BELOW_UPPER_BOUND,
                lower, upper, false);
        }
        final SecureRandom sec = this.getSecRan();
        final double r = sec.nextDouble();
        final double scaled = r * upper + (1.0 - r) * lower + r;
        return (long) MathLib.floor(scaled);
    }

    /**
     * Returns random long following Poisson distribution.
     * 
     * @param mean
     *        mean
     * @return random long following Poisson distribution
     * 
     *         <p>
     *         <strong>Algorithm Description</strong>:
     *         <ul>
     *         <li>For small means, uses simulation of a Poisson process using Uniform deviates, as described <a
     *         href="http://irmi.epfl.ch/cmos/Pmmi/interactive/rng7.htm"> here.</a> The Poisson process (and hence value
     *         returned) is bounded by 1000 * mean.</li>
     * 
     *         <li>For large means, uses the rejection algorithm described in <br/>
     *         Devroye, Luc. (1981).<i>The Computer Generation of Poisson Random Variables</i>
     *         <strong>Computing</strong> vol. 26 pp. 197-207.</li>
     *         </ul>
     *         </p>
     * @throws NotStrictlyPositiveException
     *         if {@code len <= 0}
     */
    public long nextPoisson(final double mean) {
        return new PoissonDistribution(this.getRan(), mean,
            PoissonDistribution.DEFAULT_EPSILON,
            PoissonDistribution.DEFAULT_MAX_ITERATIONS).sample();
    }

    /**
     * Returns random double following Gaussian distribution.
     * 
     * @param mu
     *        center
     * @param sigma
     *        standard deviation
     * @return random double following Gaussian distribution (mu, sigma)
     * @throws NotStrictlyPositiveException
     *         thrown if sigma <= 0
     */
    public double nextGaussian(final double mu, final double sigma) {
        if (sigma <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.STANDARD_DEVIATION, sigma);
        }
        return sigma * this.getRan().nextGaussian() + mu;
    }

    /**
     * Returns random long following Exponential distribution.
     * 
     * @param mean
     *        mean
     * @return random long following Exponential distribution
     * 
     *         <p>
     *         <strong>Algorithm Description</strong>: Uses the Algorithm SA (Ahrens) from p. 876 in: [1]: Ahrens, J. H.
     *         and Dieter, U. (1972). Computer methods for sampling from the exponential and normal distributions.
     *         Communications of the ACM, 15, 873-882.
     *         </p>
     * @throws NotStrictlyPositiveException
     *         thrown if mean <= 0
     */
    public double nextExponential(final double mean) {
        return new ExponentialDistribution(this.getRan(), mean,
            ExponentialDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY).sample();
    }

    /**
     * <p>
     * Generates a random value from the {@link fr.cnes.sirius.patrius.math.distribution.GammaDistribution Gamma
     * Distribution}.
     * </p>
     * 
     * <p>
     * This implementation uses the following algorithms:
     * </p>
     * 
     * <p>
     * For 0 < shape < 1: <br/>
     * Ahrens, J. H. and Dieter, U., <i>Computer methods for sampling from gamma, beta, Poisson and binomial
     * distributions.</i> Computing, 12, 223-246, 1974.
     * </p>
     * 
     * <p>
     * For shape >= 1: <br/>
     * Marsaglia and Tsang, <i>A Simple Method for Generating Gamma Variables.</i> ACM Transactions on Mathematical
     * Software, Volume 26 Issue 3, September, 2000.
     * </p>
     * 
     * @param shape
     *        the median of the Gamma distribution
     * @param scale
     *        the scale parameter of the Gamma distribution
     * @return random value sampled from the Gamma(shape, scale) distribution
     * @throws NotStrictlyPositiveException
     *         if {@code shape <= 0} or {@code scale <= 0}.
     */
    public double nextGamma(final double shape, final double scale) {
        return new GammaDistribution(this.getRan(), shape, scale,
            GammaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY).sample();
    }

    /**
     * Generates a random value from the {@link HypergeometricDistribution Hypergeometric Distribution}.
     * 
     * @param populationSize
     *        the population size of the Hypergeometric distribution
     * @param numberOfSuccesses
     *        number of successes in the population of the Hypergeometric distribution
     * @param sampleSize
     *        the sample size of the Hypergeometric distribution
     * @return random value sampled from the Hypergeometric(numberOfSuccesses, sampleSize) distribution
     * @throws NumberIsTooLargeException
     *         if {@code numberOfSuccesses > populationSize},
     *         or {@code sampleSize > populationSize}.
     * @throws NotStrictlyPositiveException
     *         if {@code populationSize <= 0}.
     * @throws NotPositiveException
     *         if {@code numberOfSuccesses < 0}.
     */
    public int nextHypergeometric(final int populationSize, final int numberOfSuccesses, final int sampleSize) {
        return new HypergeometricDistribution(this.getRan(), populationSize,
            numberOfSuccesses, sampleSize).sample();
    }

    /**
     * Generates a random value from the {@link PascalDistribution Pascal Distribution}.
     * 
     * @param r
     *        the number of successes of the Pascal distribution
     * @param p
     *        the probability of success of the Pascal distribution
     * @return random value sampled from the Pascal(r, p) distribution
     * @throws NotStrictlyPositiveException
     *         if the number of successes is not positive
     * @throws OutOfRangeException
     *         if the probability of success is not in the
     *         range {@code [0, 1]}.
     */
    public int nextPascal(final int r, final double p) {
        return new PascalDistribution(this.getRan(), r, p).sample();
    }

    /**
     * Generates a random value from the {@link TDistribution T Distribution}.
     * 
     * @param df
     *        the degrees of freedom of the T distribution
     * @return random value from the T(df) distribution
     * @throws NotStrictlyPositiveException
     *         if {@code df <= 0}
     */
    public double nextT(final double df) {
        return new TDistribution(this.getRan(), df,
            TDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY).sample();
    }

    /**
     * Generates a random value from the {@link WeibullDistribution Weibull Distribution}.
     * 
     * @param shape
     *        the shape parameter of the Weibull distribution
     * @param scale
     *        the scale parameter of the Weibull distribution
     * @return random value sampled from the Weibull(shape, size) distribution
     * @throws NotStrictlyPositiveException
     *         if {@code shape <= 0} or {@code scale <= 0}.
     */
    public double nextWeibull(final double shape, final double scale) {
        return new WeibullDistribution(this.getRan(), shape, scale,
            WeibullDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY).sample();
    }

    /**
     * Generates a random value from the {@link ZipfDistribution Zipf Distribution}.
     * 
     * @param numberOfElements
     *        the number of elements of the ZipfDistribution
     * @param exponent
     *        the exponent of the ZipfDistribution
     * @return random value sampled from the Zipf(numberOfElements, exponent) distribution
     * @exception NotStrictlyPositiveException
     *            if {@code numberOfElements <= 0} or {@code exponent <= 0}.
     */
    public int nextZipf(final int numberOfElements, final double exponent) {
        return new ZipfDistribution(this.getRan(), numberOfElements, exponent).sample();
    }

    /**
     * Generates a random value from the {@link BetaDistribution Beta Distribution}.
     * 
     * @param alpha
     *        first distribution shape parameter
     * @param beta
     *        second distribution shape parameter
     * @return random value sampled from the beta(alpha, beta) distribution
     */
    public double nextBeta(final double alpha, final double beta) {
        return new BetaDistribution(this.getRan(), alpha, beta,
            BetaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY).sample();
    }

    /**
     * Generates a random value from the {@link BinomialDistribution Binomial Distribution}.
     * 
     * @param numberOfTrials
     *        number of trials of the Binomial distribution
     * @param probabilityOfSuccess
     *        probability of success of the Binomial distribution
     * @return random value sampled from the Binomial(numberOfTrials, probabilityOfSuccess) distribution
     */
    public int nextBinomial(final int numberOfTrials, final double probabilityOfSuccess) {
        return new BinomialDistribution(this.getRan(), numberOfTrials, probabilityOfSuccess).sample();
    }

    /**
     * Generates a random value from the {@link CauchyDistribution Cauchy Distribution}.
     * 
     * @param median
     *        the median of the Cauchy distribution
     * @param scale
     *        the scale parameter of the Cauchy distribution
     * @return random value sampled from the Cauchy(median, scale) distribution
     */
    public double nextCauchy(final double median, final double scale) {
        return new CauchyDistribution(this.getRan(), median, scale,
            CauchyDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY).sample();
    }

    /**
     * Generates a random value from the {@link ChiSquaredDistribution ChiSquare Distribution}.
     * 
     * @param df
     *        the degrees of freedom of the ChiSquare distribution
     * @return random value sampled from the ChiSquare(df) distribution
     */
    public double nextChiSquare(final double df) {
        return new ChiSquaredDistribution(this.getRan(), df,
            ChiSquaredDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY).sample();
    }

    /**
     * Generates a random value from the {@link FDistribution F Distribution}.
     * 
     * @param numeratorDf
     *        the numerator degrees of freedom of the F distribution
     * @param denominatorDf
     *        the denominator degrees of freedom of the F distribution
     * @return random value sampled from the F(numeratorDf, denominatorDf) distribution
     * @throws NotStrictlyPositiveException
     *         if {@code numeratorDf <= 0} or {@code denominatorDf <= 0}.
     */
    public double nextF(final double numeratorDf, final double denominatorDf) {
        return new FDistribution(this.getRan(), numeratorDf, denominatorDf,
            FDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY).sample();
    }

    /**
     * Returns random double following Uniform distribution.
     * 
     * @param lower
     *        lower distribution bound
     * @param upper
     *        upper distribution bound
     * @return random double following Uniform distribution [lower, upper]
     * 
     *         <p>
     *         <strong>Algorithm Description</strong>: scales the output of Random.nextDouble(), but rejects 0 values
     *         (i.e., will generate another random double if Random.nextDouble() returns 0). This is necessary to
     *         provide a symmetric output interval (both endpoints excluded).
     *         </p>
     * @throws NumberIsTooLargeException
     *         if {@code lower >= upper}
     * @throws NotFiniteNumberException
     *         if one of the bounds is infinite
     * @throws NotANumberException
     *         if one of the bounds is NaN
     */
    public double nextUniform(final double lower, final double upper) {
        return this.nextUniform(lower, upper, false);
    }

    /**
     * Returns random double following Uniform distribution.
     * 
     * @param lower
     *        lower distribution bound
     * @param upper
     *        upper distribution bound
     * @param lowerInclusive
     *        true if lower bound is included
     * @return random double following Uniform distribution [lower, upper]
     * 
     *         <p>
     *         <strong>Algorithm Description</strong>: if the lower bound is excluded, scales the output of
     *         Random.nextDouble(), but rejects 0 values (i.e., will generate another random double if
     *         Random.nextDouble() returns 0). This is necessary to provide a symmetric output interval (both endpoints
     *         excluded).
     *         </p>
     * 
     * @throws NumberIsTooLargeException
     *         if {@code lower >= upper}
     * @throws NotFiniteNumberException
     *         if one of the bounds is infinite
     * @throws NotANumberException
     *         if one of the bounds is NaN
     */
    public double nextUniform(final double lower, final double upper, final boolean lowerInclusive) {

        if (lower >= upper) {
            // Exception
            throw new NumberIsTooLargeException(PatriusMessages.LOWER_BOUND_NOT_BELOW_UPPER_BOUND,
                lower, upper, false);
        }

        if (Double.isInfinite(lower)) {
            // Exception
            throw new NotFiniteNumberException(PatriusMessages.INFINITE_BOUND, lower);
        }
        if (Double.isInfinite(upper)) {
            // Exception
            throw new NotFiniteNumberException(PatriusMessages.INFINITE_BOUND, upper);
        }

        if (Double.isNaN(lower) || Double.isNaN(upper)) {
            // Exception
            throw new NotANumberException();
        }

        final RandomGenerator generator = this.getRan();

        // ensure nextDouble() isn't 0.0
        double u = generator.nextDouble();
        while (!lowerInclusive && u <= 0.0) {
            u = generator.nextDouble();
        }

        return u * upper + (1.0 - u) * lower;
    }

    /**
     * Returns random permutation.
     * 
     * @param n
     *        1st permutation parameter
     * @param k
     *        2nd permutation parameter
     * @return random permutation "k among n"
     * 
     *         <p>
     *         Uses a 2-cycle permutation shuffle. The shuffling process is described <a
     *         href="http://www.maths.abdn.ac.uk/~igc/tch/mx4002/notes/node83.html"> here</a>.
     *         </p>
     * @throws NumberIsTooLargeException
     *         if {@code k > n}.
     * @throws NotStrictlyPositiveException
     *         if {@code k <= 0}.
     */
    public int[] nextPermutation(final int n, final int k) {
        if (k > n) {
            // Exception
            throw new NumberIsTooLargeException(PatriusMessages.PERMUTATION_EXCEEDS_N,
                k, n, true);
        }
        if (k <= 0) {
            // Exception
            throw new NotStrictlyPositiveException(PatriusMessages.PERMUTATION_SIZE,
                k);
        }

        final int[] index = this.getNatural(n);
        // Shuffle index list past index n-k
        this.shuffle(index, n - k);
        final int[] result = new int[k];
        // create array from the last k elements of index
        for (int i = 0; i < k; i++) {
            result[i] = index[n - i - 1];
        }

        return result;
    }

    /**
     * Returns random sample in collection.
     * 
     * @param c
     *        collection
     * @param k
     *        number of samples
     * @return k random samples in collection
     * 
     *         <p>
     *         <strong>Algorithm Description</strong>: Uses a 2-cycle permutation shuffle to generate a random
     *         permutation of <code>c.size()</code> and then returns the elements whose indexes correspond to the
     *         elements of the generated permutation. This technique is described, and proven to generate random samples
     *         <a href="http://www.maths.abdn.ac.uk/~igc/tch/mx4002/notes/node83.html"> here</a>
     *         </p>
     * @throws NumberIsTooLargeException
     *         thrown if k > size of c
     * @throws NotStrictlyPositiveException
     *         thrown if k <= 0
     */
    public Object[] nextSample(final Collection<?> c,
                               final int k) {

        // get size of the collection
        final int len = c.size();
        if (k > len) {
            // Exception
            throw new NumberIsTooLargeException(PatriusMessages.SAMPLE_SIZE_EXCEEDS_COLLECTION_SIZE,
                k, len, true);
        }
        if (k <= 0) {
            // Exception
            throw new NotStrictlyPositiveException(PatriusMessages.NUMBER_OF_SAMPLES, k);
        }

        // Select k random objects from collection
        final Object[] objects = c.toArray();
        final int[] index = this.nextPermutation(len, k);
        final Object[] result = new Object[k];
        for (int i = 0; i < k; i++) {
            result[i] = objects[index[i]];
        }
        return result;
    }

    /**
     * Reseeds the random number generator with the supplied seed.
     * <p>
     * Will create and initialize if null.
     * </p>
     * 
     * @param seed
     *        the seed value to use
     */
    public void reSeed(final long seed) {
        this.getRan().setSeed(seed);
    }

    /**
     * Reseeds the secure random number generator with the current time in
     * milliseconds.
     * <p>
     * Will create and initialize if null.
     * </p>
     */
    public void reSeedSecure() {
        this.getSecRan().setSeed(System.currentTimeMillis());
    }

    /**
     * Reseeds the secure random number generator with the supplied seed.
     * <p>
     * Will create and initialize if null.
     * </p>
     * 
     * @param seed
     *        the seed value to use
     */
    public void reSeedSecure(final long seed) {
        this.getSecRan().setSeed(seed);
    }

    /**
     * Reseeds the random number generator with {@code System.currentTimeMillis() + System.identityHashCode(this))}.
     */
    public void reSeed() {
        this.getRan().setSeed(System.currentTimeMillis() + System.identityHashCode(this));
    }

    /**
     * Sets the PRNG algorithm for the underlying SecureRandom instance using
     * the Security Provider API. The Security Provider API is defined in <a
     * href =
     * "http://java.sun.com/j2se/1.3/docs/guide/security/CryptoSpec.html#AppA">
     * Java Cryptography Architecture API Specification & Reference.</a>
     * <p>
     * <strong>USAGE NOTE:</strong> This method carries <i>significant</i> overhead and may take several seconds to
     * execute.
     * </p>
     * 
     * @param algorithm
     *        the name of the PRNG algorithm
     * @param provider
     *        the name of the provider
     * @throws NoSuchAlgorithmException
     *         if the specified algorithm is not available
     * @throws NoSuchProviderException
     *         if the specified provider is not installed
     */
    public void setSecureAlgorithm(final String algorithm,
                                   final String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        this.secRand = SecureRandom.getInstance(algorithm, provider);
    }

    /**
     * Returns the RandomGenerator used to generate non-secure random data.
     * <p>
     * Creates and initializes a default generator if null. Uses a {@link Well19937c} generator with
     * {@code System.currentTimeMillis() + System.identityHashCode(this))} as the default seed.
     * </p>
     * 
     * @return the Random used to generate random data
     */
    public RandomGenerator getRan() {
        if (this.rand == null) {
            this.initRan();
        }
        return this.rand;
    }

    /**
     * Sets the default generator to a {@link Well19937c} generator seeded with
     * {@code System.currentTimeMillis() + System.identityHashCode(this))}.
     */
    private void initRan() {
        this.rand = new Well19937c(System.currentTimeMillis() + System.identityHashCode(this));
    }

    /**
     * Returns the SecureRandom used to generate secure random data.
     * <p>
     * Creates and initializes if null. Uses {@code System.currentTimeMillis() + System.identityHashCode(this)} as the
     * default seed.
     * </p>
     * 
     * @return the SecureRandom used to generate secure random data
     */
    private SecureRandom getSecRan() {
        if (this.secRand == null) {
            this.secRand = new SecureRandom();
            this.secRand.setSeed(System.currentTimeMillis() + System.identityHashCode(this));
        }
        return this.secRand;
    }

    /**
     * Uses a 2-cycle permutation shuffle to randomly re-order the last elements
     * of list.
     * 
     * @param list
     *        list to be shuffled
     * @param end
     *        element past which shuffling begins
     */
    private void shuffle(final int[] list, final int end) {
        int target = 0;
        // loop on the elements of list past index end
        for (int i = list.length - 1; i >= end; i--) {
            if (i == 0) {
                target = 0;
            } else {
                // NumberIsTooLargeException cannot occur
                target = this.nextInt(0, i);
            }
            // switch the values between indices i and target
            final int temp = list[target];
            list[target] = list[i];
            list[i] = temp;
        }
    }

    /**
     * Returns an array representing n.
     * 
     * @param n
     *        the natural number to represent
     * @return array with entries = elements of n
     */
    private int[] getNatural(final int n) {
        final int[] natural = new int[n];
        for (int i = 0; i < n; i++) {
            natural[i] = i;
        }
        return natural;
    }
}
