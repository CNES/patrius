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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.ZeroException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Generates values for use in simulation applications.
 * <p>
 * How values are generated is determined by the <code>mode</code> property.
 * </p>
 * <p>
 * Supported <code>mode</code> values are:
 * <ul>
 * <li>DIGEST_MODE -- uses an empirical distribution</li>
 * <li>REPLAY_MODE -- replays data from <code>valuesFileURL</code></li>
 * <li>UNIFORM_MODE -- generates uniformly distributed random values with mean = <code>mu</code></li>
 * <li>EXPONENTIAL_MODE -- generates exponentially distributed random values with mean = <code>mu</code></li>
 * <li>GAUSSIAN_MODE -- generates Gaussian distributed random values with mean = <code>mu</code> and standard deviation
 * = <code>sigma</code></li>
 * <li>CONSTANT_MODE -- returns <code>mu</code> every time.</li>
 * </ul>
 * </p>
 * 
 * @version $Id: ValueServer.java 18108 2017-10-04 06:45:27Z bignon $
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class ValueServer {

    /** Use empirical distribution. */
    public static final int DIGEST_MODE = 0;

    /** Replay data from valuesFilePath. */
    public static final int REPLAY_MODE = 1;

    /** Uniform random deviates with mean = &mu;. */
    public static final int UNIFORM_MODE = 2;

    /** Exponential random deviates with mean = &mu;. */
    public static final int EXPONENTIAL_MODE = 3;

    /** Gaussian random deviates with mean = &mu;, std dev = &sigma;. */
    public static final int GAUSSIAN_MODE = 4;

    /** Always return mu */
    public static final int CONSTANT_MODE = 5;

    /** mode determines how values are generated. */
    private int mode = 5;

    /** URI to raw data values. */
    private URL valuesFileURL = null;

    /** Mean for use with non-data-driven modes. */
    private double mu = 0.0;

    /** Standard deviation for use with GAUSSIAN_MODE. */
    private double sigma = 0.0;

    /** Empirical probability distribution for use with DIGEST_MODE. */
    private EmpiricalDistribution empiricalDistribution = null;

    /** File pointer for REPLAY_MODE. */
    private BufferedReader filePointer = null;

    /** Random generator to use for random data generation. */
    private final RandomDataGenerator randomDataGenerator;

    // Data generation modes ======================================

    /** Creates new ValueServer */
    public ValueServer() {
        this.randomDataGenerator = new RandomDataGenerator();
    }

    /**
     * Construct a ValueServer instance using a RandomGenerator as its source
     * of random data.
     * 
     * @since 3.1
     * @param generator
     *        source of random data
     */
    public ValueServer(final RandomGenerator generator) {
        this.randomDataGenerator = new RandomDataGenerator(generator);
    }

    /**
     * Returns the next generated value, generated according
     * to the mode value (see MODE constants).
     * 
     * @return generated value
     * @throws IOException
     *         in REPLAY_MODE if a file I/O error occurs
     * @throws MathIllegalStateException
     *         if mode is not recognized
     * @throws MathIllegalArgumentException
     *         if the underlying random generator thwrows one
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public double getNext() throws IOException {
        // CHECKSTYLE: resume ReturnCount check
        switch (this.mode) {
            case DIGEST_MODE:
                return this.getNextDigest();
            case REPLAY_MODE:
                return this.getNextReplay();
            case UNIFORM_MODE:
                return this.getNextUniform();
            case EXPONENTIAL_MODE:
                return this.getNextExponential();
            case GAUSSIAN_MODE:
                return this.getNextGaussian();
            case CONSTANT_MODE:
                return this.mu;
            default:
                throw new MathIllegalStateException(
                    PatriusMessages.UNKNOWN_MODE,
                    this.mode,
                    "DIGEST_MODE", DIGEST_MODE, "REPLAY_MODE", REPLAY_MODE,
                    "UNIFORM_MODE", UNIFORM_MODE, "EXPONENTIAL_MODE", EXPONENTIAL_MODE,
                    "GAUSSIAN_MODE", GAUSSIAN_MODE, "CONSTANT_MODE", CONSTANT_MODE);
        }
    }

    /**
     * Fills the input array with values generated using getNext() repeatedly.
     * 
     * @param values
     *        array to be filled
     * @throws IOException
     *         in REPLAY_MODE if a file I/O error occurs
     * @throws MathIllegalStateException
     *         if mode is not recognized
     * @throws MathIllegalArgumentException
     *         if the underlying random generator thwrows one
     */
    public void fill(final double[] values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            values[i] = this.getNext();
        }
    }

    /**
     * Returns an array of length <code>length</code> with values generated
     * using getNext() repeatedly.
     * 
     * @param length
     *        length of output array
     * @return array of generated values
     * @throws IOException
     *         in REPLAY_MODE if a file I/O error occurs
     * @throws MathIllegalStateException
     *         if mode is not recognized
     * @throws MathIllegalArgumentException
     *         if the underlying random generator thwrows one
     */
    public double[] fill(final int length) throws IOException {
        final double[] out = new double[length];
        for (int i = 0; i < length; i++) {
            out[i] = this.getNext();
        }
        return out;
    }

    /**
     * Computes the empirical distribution using values from the file
     * in <code>valuesFileURL</code>, using the default number of bins.
     * <p>
     * <code>valuesFileURL</code> must exist and be readable by *this at runtime.
     * </p>
     * <p>
     * This method must be called before using <code>getNext()</code> with <code>mode = DIGEST_MODE</code>
     * </p>
     * 
     * @throws IOException
     *         if an I/O error occurs reading the input file
     * @throws NullArgumentException
     *         if the {@code valuesFileURL} has not been set
     * @throws ZeroException
     *         if URL contains no data
     */
    public void computeDistribution() throws IOException {
        this.computeDistribution(EmpiricalDistribution.DEFAULT_BIN_COUNT);
    }

    /**
     * Computes the empirical distribution using values from the file
     * in <code>valuesFileURL</code> and <code>binCount</code> bins.
     * <p>
     * <code>valuesFileURL</code> must exist and be readable by this process at runtime.
     * </p>
     * <p>
     * This method must be called before using <code>getNext()</code> with <code>mode = DIGEST_MODE</code>
     * </p>
     * 
     * @param binCount
     *        the number of bins used in computing the empirical
     *        distribution
     * @throws NullArgumentException
     *         if the {@code valuesFileURL} has not been set
     * @throws IOException
     *         if an error occurs reading the input file
     * @throws ZeroException
     *         if URL contains no data
     */
    public void computeDistribution(final int binCount) throws IOException {
        this.empiricalDistribution = new EmpiricalDistribution(binCount, this.randomDataGenerator.getRan());
        this.empiricalDistribution.load(this.valuesFileURL);
        this.mu = this.empiricalDistribution.getSampleStats().getMean();
        this.sigma = this.empiricalDistribution.getSampleStats().getStandardDeviation();
    }

    /**
     * Returns the data generation mode. See {@link ValueServer the class javadoc} for description of the valid values
     * of this property.
     * 
     * @return Value of property mode.
     */
    public int getMode() {
        return this.mode;
    }

    /**
     * Sets the data generation mode.
     * 
     * @param modeIn
     *        New value of the data generation mode.
     */
    public void setMode(final int modeIn) {
        this.mode = modeIn;
    }

    /**
     * Returns the URL for the file used to build the empirical distribution
     * when using {@link #DIGEST_MODE}.
     * 
     * @return Values file URL.
     */
    public URL getValuesFileURL() {
        return this.valuesFileURL;
    }

    /**
     * Sets the {@link #getValuesFileURL() values file URL} using a string
     * URL representation.
     * 
     * @param url
     *        String representation for new valuesFileURL.
     * @throws MalformedURLException
     *         if url is not well formed
     */
    public void setValuesFileURL(final String url) throws MalformedURLException {
        this.valuesFileURL = new URL(url);
    }

    /**
     * Sets the the {@link #getValuesFileURL() values file URL}.
     * 
     * <p>
     * The values file <i>must</i> be an ASCII text file containing one valid numeric entry per line.
     * </p>
     * 
     * @param url
     *        URL of the values file.
     */
    public void setValuesFileURL(final URL url) {
        this.valuesFileURL = url;
    }

    /**
     * Returns the {@link EmpiricalDistribution} used when operating in {@value #DIGEST_MODE}.
     * 
     * @return EmpircalDistribution built by {@link #computeDistribution()}
     */
    public EmpiricalDistribution getEmpiricalDistribution() {
        return this.empiricalDistribution;
    }

    /**
     * Resets REPLAY_MODE file pointer to the beginning of the <code>valuesFileURL</code>.
     * 
     * @throws IOException
     *         if an error occurs opening the file
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void resetReplayFile() throws IOException {
        if (this.filePointer != null) {
            try {
                this.filePointer.close();
                this.filePointer = null;
            } catch (final IOException ex) {
                // ignore
            }
        }
        this.filePointer = new BufferedReader(new InputStreamReader(this.valuesFileURL.openStream(), "UTF-8"));
    }

    /**
     * Closes {@code valuesFileURL} after use in REPLAY_MODE.
     * 
     * @throws IOException
     *         if an error occurs closing the file
     */
    public void closeReplayFile() throws IOException {
        if (this.filePointer != null) {
            this.filePointer.close();
            this.filePointer = null;
        }
    }

    /**
     * Returns the mean used when operating in {@link #GAUSSIAN_MODE}, {@link #EXPONENTIAL_MODE} or
     * {@link #UNIFORM_MODE}. When operating in {@link #CONSTANT_MODE}, this is the constant
     * value always returned. Calling {@link #computeDistribution()} sets this value to the
     * overall mean of the values in the {@link #getValuesFileURL() values file}.
     * 
     * @return Mean used in data generation.
     */
    public double getMu() {
        return this.mu;
    }

    /**
     * Sets the {@link #getMu() mean} used in data generation. Note that calling this method
     * after {@link #computeDistribution()} has been called will have no effect on data
     * generated in {@link #DIGEST_MODE}.
     * 
     * @param muIn
     *        new Mean value.
     */
    public void setMu(final double muIn) {
        this.mu = muIn;
    }

    /**
     * Returns the standard deviation used when operating in {@link #GAUSSIAN_MODE}.
     * Calling {@link #computeDistribution()} sets this value to the overall standard
     * deviation of the values in the {@link #getValuesFileURL() values file}. This
     * property has no effect when the data generation mode is not {@link #GAUSSIAN_MODE}.
     * 
     * @return Standard deviation used when operating in {@link #GAUSSIAN_MODE}.
     */
    public double getSigma() {
        return this.sigma;
    }

    /**
     * Sets the {@link #getSigma() standard deviation} used in {@link #GAUSSIAN_MODE}.
     * 
     * @param sigmaIn
     *        New standard deviation.
     */
    public void setSigma(final double sigmaIn) {
        this.sigma = sigmaIn;
    }

    /**
     * Reseeds the random data generator.
     * 
     * @param seed
     *        Value with which to reseed the {@link RandomDataGenerator} used to generate random data.
     */
    public void reSeed(final long seed) {
        this.randomDataGenerator.reSeed(seed);
    }

    // ------------- private methods ---------------------------------

    /**
     * Gets a random value in DIGEST_MODE.
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>Before this method is called, <code>computeDistribution()</code> must have completed successfully; otherwise
     * an <code>IllegalStateException</code> will be thrown</li>
     * </ul>
     * </p>
     * 
     * @return next random value from the empirical distribution digest
     * @throws MathIllegalStateException
     *         if digest has not been initialized
     */
    private double getNextDigest() {
        if ((this.empiricalDistribution == null) ||
            (this.empiricalDistribution.getBinStats().size() == 0)) {
            throw new MathIllegalStateException(PatriusMessages.DIGEST_NOT_INITIALIZED);
        }
        return this.empiricalDistribution.getNextValue();
    }

    /**
     * Gets next sequential value from the <code>valuesFileURL</code>.
     * <p>
     * Throws an IOException if the read fails.
     * </p>
     * <p>
     * This method will open the <code>valuesFileURL</code> if there is no replay file open.
     * </p>
     * <p>
     * The <code>valuesFileURL</code> will be closed and reopened to wrap around from EOF to BOF if EOF is encountered.
     * EOFException (which is a kind of IOException) may still be thrown if the <code>valuesFileURL</code> is empty.
     * </p>
     * 
     * @return next value from the replay file
     * @throws IOException
     *         if there is a problem reading from the file
     * @throws MathIllegalStateException
     *         if URL contains no data
     * @throws NumberFormatException
     *         if an invalid numeric string is
     *         encountered in the file
     */
    private double getNextReplay() throws IOException {
        if (this.filePointer == null) {
            this.resetReplayFile();
        }
        String str = this.filePointer.readLine();
        if (str == null) {
            // we have probably reached end of file, wrap around from EOF to BOF
            this.closeReplayFile();
            this.resetReplayFile();
            str = this.filePointer.readLine();
            if (str == null) {
                throw new MathIllegalStateException(PatriusMessages.URL_CONTAINS_NO_DATA,
                    this.valuesFileURL);
            }
        }
        return Double.valueOf(str);
    }

    /**
     * Gets a uniformly distributed random value with mean = mu.
     * 
     * @return random uniform value
     * @throws MathIllegalArgumentException
     *         if the underlying random generator thwrows one
     */
    private double getNextUniform() {
        return this.randomDataGenerator.nextUniform(0, 2 * this.mu);
    }

    /**
     * Gets an exponentially distributed random value with mean = mu.
     * 
     * @return random exponential value
     * @throws MathIllegalArgumentException
     *         if the underlying random generator thwrows one
     */
    private double getNextExponential() {
        return this.randomDataGenerator.nextExponential(this.mu);
    }

    /**
     * Gets a Gaussian distributed random value with mean = mu
     * and standard deviation = sigma.
     * 
     * @return random Gaussian value
     * @throws MathIllegalArgumentException
     *         if the underlying random generator thwrows one
     */
    private double getNextGaussian() {
        return this.randomDataGenerator.nextGaussian(this.mu, this.sigma);
    }

}
