/**
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3223:03/11/2022:[PATRIUS] Frame implements PVCoordinatesProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import fr.cnes.sirius.patrius.math.stat.descriptive.DescriptiveStatistics;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class allows to perform a benchmark of a runnable function.
 *
 * @author veuillh
 */
public class TimeIt {

    /** Default number of runs. */
    public static final int DEFAULT_NB_RUNS = 7;

    /** Default warmup factor. */
    public static final long DEFAULT_WARMUP_FACTOR = 10L;

    /** Nano-seconds exponent limit. */
    private static final double NANO_SEC_EXPONENT_LIMIT = -6.;

    /** Seconds to nano-seconds factor. */
    private static final double SEC_TO_NANO_SEC = 1e9;

    /** Micro-seconds exponent limit. */
    private static final double MICRO_SEC_EXPONENT_LIMIT = -3.;

    /** Seconds to micro-seconds factor. */
    private static final double SEC_TO_MICRO_SEC = 1e6;

    /** Seconds to milli-seconds factor. */
    private static final double SEC_TO_MILLI_SEC = 1e3;

    /** Ten. */
    private static final double TEN = 10.;

    /** Computation times array. */
    private final double[] timesInLoop;

    /** Number of loops to perform. */
    private final long nbLoops;

    /** Number of runs to perform. */
    private final int nbRuns;

    /**
     * Arithmetic mean of the computation time it takes to perform the function to benchmark (evaluated on a batch of
     * "nbLoops" runs)
     */
    private final double mean;

    /**
     * Standard deviation of the computation time it takes to perform the function to benchmark (evaluated on a batch of
     * "nbLoops" runs)
     */
    private final double std;

    /** Minimum time it takes to perform the function to benchmark (evaluated on a batch of "nbLoops" runs). */
    private final double min;

    /** Maximum time it takes to perform the function to benchmark (evaluated on a batch of "nbLoops" runs). */
    private final double max;

    /**
     * Simple constructor that defines automatically the number of loops that should be performed.
     *
     * <p>
     * It chooses the number of loops so that each of the 7 runs will take around 1 second.
     * </p>
     *
     * @param benchmarkFunction
     *        The function to benchmark
     */
    public TimeIt(final Runnable benchmarkFunction) {
        // No warmup since it was already done by the loopEstimator
        this(benchmarkFunction, loopsEstimator(benchmarkFunction), DEFAULT_NB_RUNS, false);
    }

    /**
     * Main constructor.
     *
     * <p>
     * The function to benchmark will be evaluated {@code nbLoops * nbRuns} times.<br>
     * If the function needs to warmup, it is previously run {@code nbLoops / 10} times which are not taken in account
     * for the statistics.
     * </p>
     *
     * @param benchmarkFunction
     *        The function to benchmark
     * @param nbLoops
     *        The number of loops to be performed
     * @param nbRuns
     *        The number of runs to be performed (used for the statistics)
     * @param warmup
     *        {@code true} if the function needs to warmup, {@code false} otherwise
     */
    public TimeIt(final Runnable benchmarkFunction, final long nbLoops, final int nbRuns, final boolean warmup) {
        this(benchmarkFunction, nbLoops, nbRuns, warmup ? MathLib.max(nbLoops / DEFAULT_WARMUP_FACTOR, 1) : 0L);
    }

    /**
     * Main constructor.
     *
     * <p>
     * The function to benchmark will be evaluated {@code nbLoops * nbRuns} times.<br>
     * If the function needs to warmup, it is previously run {@code nbWarmupLoops} times which are not taken in account
     * for the statistics.
     * </p>
     *
     * @param benchmarkFunction
     *        The function to benchmark
     * @param nbLoops
     *        The number of loops to be performed
     * @param nbRuns
     *        The number of runs to be performed (used for the statistics)
     * @param nbWarmupLoops
     *        The number of loops to be performed for warmup the function (should be {@code > 0} to enable to warmup)
     */
    @SuppressWarnings("PMD.DoNotCallGarbageCollectionExplicitly")
    // Reason: Benchmark function
    public TimeIt(final Runnable benchmarkFunction, final long nbLoops, final int nbRuns, final long nbWarmupLoops) {
        this.timesInLoop = new double[nbRuns];
        this.nbLoops = nbLoops;
        this.nbRuns = nbRuns;

        // Warmup phase
        if (nbWarmupLoops > 0l) {
            for (long j = MathLib.max(1l, nbWarmupLoops); j > 0; j--) {
                benchmarkFunction.run();
            }
        }

        // Garbage collection before the time computation so that the garbage collection of previous runs does not
        // influence the following runs
        System.gc();

        // Actual time computation
        long start;
        long end;
        final double factor = SEC_TO_NANO_SEC * nbLoops;
        for (int i = 0; i < nbRuns; i++) {
            start = System.nanoTime();
            for (long j = nbLoops; j > 0L; j--) {
                benchmarkFunction.run();

            }
            end = System.nanoTime();
            this.timesInLoop[i] = (end - start) / factor;
        }

        final DescriptiveStatistics stats = new DescriptiveStatistics(this.timesInLoop);
        this.mean = stats.getMean();
        this.std = stats.getStandardDeviation();
        this.min = stats.getMin();
        this.max = stats.getMax();
    }

    /**
     * Returns the arithmetic mean of the computation time it takes to perform the function to benchmark (evaluated on a
     * batch of {@link #nbLoops N loops}).
     *
     * @return the mean
     */
    public double getMeanTime() {
        return this.mean;
    }

    /**
     * Returns the standard deviation of the computation time it takes to perform the function to benchmark (evaluated
     * on a batch of {@link #nbLoops N loops}).
     *
     * @return the standard deviation
     */
    public double getStandardDeviationTime() {
        return this.std;
    }

    /**
     * Returns the minimum computation time it takes to perform the function to benchmark (evaluated on a batch of
     * {@link #nbLoops N loops}).
     *
     * @return the minimum computation time
     */
    public double getMinTime() {
        return this.min;
    }

    /**
     * Returns the maximum time it takes to perform the function to benchmark (evaluated on a batch of {@link #nbLoops N
     * loops}).
     *
     * @return the maximum computation time
     */
    public double getMaxTime() {
        return this.max;
    }

    /**
     * Returns the computation times it takes to perform the function to benchmark on each batch of "nbLoops" runs.
     *
     * @return the computation times
     */
    public double[] getTimes() {
        return this.timesInLoop.clone();
    }

    /**
     * Returns a string representation of the benchmark evaluation statistical results.
     *
     * @return a string representation of the benchmark evaluation statistical results
     */
    @Override
    public String toString() {

        final long exponent = powerOfTen(this.mean);
        final double factor;
        // Time unit management
        final String unit;
        if (exponent < NANO_SEC_EXPONENT_LIMIT) {
            // Nanosecond
            factor = SEC_TO_NANO_SEC;
            unit = "ns";
        } else if (exponent < MICRO_SEC_EXPONENT_LIMIT) {
            // Microsecond
            factor = SEC_TO_MICRO_SEC;
            unit = "µs";
        } else if (exponent < 0L) {
            // Millisecond
            factor = SEC_TO_MILLI_SEC;
            unit = "ms";
        } else {
            // Second
            factor = 1.;
            unit = "s";
        }
        return String.format("%.2f ± %.2f %s (min=%.2f%s; max=%.2f%s) per loop (%d runs, %d loops each)",
            this.mean * factor, this.std * factor, unit, this.min * factor, unit, this.max * factor, unit, this.nbRuns,
            this.nbLoops);
    }

    /**
     * Estimates very approximately the number of loops per seconds that can be done by the provided function.
     *
     * <p>
     * The estimation takes around 0.2 seconds (unless 1 run takes more than 0.2).
     * </p>
     *
     * @param benchmarkFunction
     *        The function to benchmark
     * @return the number of loops per second that can be done. Returns 1 if a loop takes more than 1 second.
     */
    public static double loopsPerSecondEstimator(final Runnable benchmarkFunction) {

        final long timeEstimationInNs = 200_000_000L; // 0.2 seconds

        long loopSize = 1L;
        long nbLoops = 0L;
        long timeSpent = 0L;

        // Count the number of loops per seconds that can be done by the provided function
        final long start = System.nanoTime();
        while (timeSpent < timeEstimationInNs) {
            for (long i = loopSize; i > 0L; i--) {
                // Run the function
                benchmarkFunction.run();
            }
            timeSpent = System.nanoTime() - start;

            nbLoops += loopSize;
            loopSize *= 2l;
        }
        // Return the number of loops per second that can be done
        return (nbLoops * SEC_TO_NANO_SEC) / (timeSpent);
    }

    /**
     * Internal function to estimate how many loops (expressed as a power of 10) should be perform in 1 second.
     *
     * <p>
     * Maximum 1e9 loops not to overflow integers.
     * </p>
     *
     * @param benchmarkFunction
     *        The function to benchmark
     * @return the power of ten loops to perform in approximately 1 second
     */
    private static long loopsEstimator(final Runnable benchmarkFunction) {
        final double loopsPerSecond = loopsPerSecondEstimator(benchmarkFunction);
        final long exponent = powerOfTen(loopsPerSecond);
        final long loopsEstimation = (long) MathLib.pow(TEN, exponent);
        return loopsEstimation > 0 ? loopsEstimation : 1;
    }

    /**
     * Return the power of ten of the given value.
     *
     * @param d
     *        Value
     * @return the power of ten of the given value
     */
    private static long powerOfTen(final double d) {
        return MathLib.round(MathLib.log10(d));
    }
}
