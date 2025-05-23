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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math;

import java.util.Random;
import java.util.concurrent.Callable;

import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Simple benchmarking utilities.
 */
public class PerfTestUtils {
    /** Nanoseconds to milliseconds conversion factor ({@value} ). */
    public static final double NANO_TO_MILLI = 1e-6;
    /** Default number of code repeat per timed block. */
    private static final int DEFAULT_REPEAT_CHUNK = 1000;
    /** Default number of code repeats for computing the average run time. */
    private static final int DEFAULT_REPEAT_STAT = 10000;
    /** RNG. */
    private static Random rng = new Random();

    /**
     * Timing.
     * 
     * @param repeatChunk
     *        Each timing measurement will done done for that
     *        number of repeats of the code.
     * @param repeatStat
     *        Timing will be averaged over that number of runs.
     * @param runGC
     *        Call {@code System.gc()} between each timed block. When
     *        set to {@code true}, the test will run much slower.
     * @param methods
     *        Codes being timed.
     * @return for each of the given {@code methods}, a {@link StatisticalSummary} of the average times (in
     *         milliseconds)
     *         taken by a single call to the {@code call} method (i.e. the time
     *         taken by each timed block divided by {@code repeatChunk}).
     */
    public static StatisticalSummary[] time(final int repeatChunk,
                                            final int repeatStat,
                                            final boolean runGC,
                                            final Callable<Double>... methods) {
        final double[][][] times = timesAndResults(repeatChunk,
            repeatStat,
            runGC,
            methods);

        final int len = methods.length;
        final StatisticalSummary[] stats = new StatisticalSummary[len];
        for (int j = 0; j < len; j++) {
            final SummaryStatistics s = new SummaryStatistics();
            for (int k = 0; k < repeatStat; k++) {
                s.addValue(times[j][k][0]);
            }
            stats[j] = s.getSummary();
        }

        return stats;
    }

    /**
     * Timing.
     * 
     * @param repeatChunk
     *        Each timing measurement will done done for that
     *        number of repeats of the code.
     * @param repeatStat
     *        Timing will be averaged over that number of runs.
     * @param runGC
     *        Call {@code System.gc()} between each timed block. When
     *        set to {@code true}, the test will run much slower.
     * @param methods
     *        Codes being timed.
     * @return for each of the given {@code methods} (first dimension), and
     *         each of the {@code repeatStat} runs (second dimension):
     *         <ul>
     *         <li>
     *         the average time (in milliseconds) taken by a single call to the {@code call} method (i.e. the time taken
     *         by each timed block divided by {@code repeatChunk})</li>
     *         <li>
     *         the result returned by the {@code call} method.</li>
     *         </ul>
     */
    public static double[][][] timesAndResults(final int repeatChunk,
                                               final int repeatStat,
                                               final boolean runGC,
                                               final Callable<Double>... methods) {
        final int numMethods = methods.length;
        final double[][][] timesAndResults = new double[numMethods][repeatStat][2];

        try {
            for (int k = 0; k < repeatStat; k++) {
                for (int j = 0; j < numMethods; j++) {
                    if (runGC) {
                        // Try to perform GC outside the timed block.
                        System.gc();
                    }

                    final Callable<Double> r = methods[j];
                    final double[] result = new double[repeatChunk];

                    // Timed block.
                    final long start = System.nanoTime();
                    for (int i = 0; i < repeatChunk; i++) {
                        result[i] = r.call();
                    }
                    final long stop = System.nanoTime();

                    // Collect run time.
                    timesAndResults[j][k][0] = (stop - start) * NANO_TO_MILLI;
                    // Keep track of a randomly selected result.
                    timesAndResults[j][k][1] = result[rng.nextInt(repeatChunk)];
                }
            }
        } catch (final Exception e) {
            // Abort benchmarking if codes throw exceptions.
            throw new MathIllegalStateException(PatriusMessages.SIMPLE_MESSAGE, e.getMessage());
        }

        final double normFactor = 1d / repeatChunk;
        for (int j = 0; j < numMethods; j++) {
            for (int k = 0; k < repeatStat; k++) {
                timesAndResults[j][k][0] *= normFactor;
            }
        }

        return timesAndResults;
    }

    /**
     * Timing and report (to standard output) the average time and standard
     * deviation of a single call.
     * The timing is performed by calling the {@link #time(int,int,boolean,Callable[]) time} method.
     * 
     * @param title
     *        Title of the test (for the report).
     * @param repeatChunk
     *        Each timing measurement will done done for that
     *        number of repeats of the code.
     * @param repeatStat
     *        Timing will be averaged over that number of runs.
     * @param runGC
     *        Call {@code System.gc()} between each timed block. When
     *        set to {@code true}, the test will run much slower.
     * @param methods
     *        Codes being timed.
     * @return for each of the given {@code methods}, a statistics of the
     *         average times (in milliseconds) taken by a single call to the {@code call} method (i.e. the time taken by
     *         each timed block divided
     *         by {@code repeatChunk}).
     */
    public static StatisticalSummary[] timeAndReport(final String title,
                                                     final int repeatChunk,
                                                     final int repeatStat,
                                                     final boolean runGC,
                                                     final RunTest... methods) {
        // Header format.
        final String hFormat = "%s (calls per timed block: %d, timed blocks: %d, time unit: ms)";

        // Width of the longest name.
        int nameLength = 0;
        for (final RunTest m : methods) {
            final int len = m.getName().length();
            if (len > nameLength) {
                nameLength = len;
            }
        }
        final String nameLengthFormat = "%" + nameLength + "s";

        // Column format.
        final String cFormat = nameLengthFormat + " %14s %14s %10s %10s %15s";
        // Result format.
        final String format = nameLengthFormat + " %.8e %.8e %.4e %.4e % .8e";

        System.out.println(String.format(hFormat,
            title,
            repeatChunk,
            repeatStat));
        System.out.println(String.format(cFormat,
            "name",
            "time/call",
            "std error",
            "total time",
            "ratio",
            "difference"));
        final StatisticalSummary[] time = time(repeatChunk,
            repeatStat,
            runGC,
            methods);
        final double refSum = time[0].getSum() * repeatChunk;
        for (int i = 0, max = time.length; i < max; i++) {
            final StatisticalSummary s = time[i];
            final double sum = s.getSum() * repeatChunk;
            System.out.println(String.format(format,
                methods[i].getName(),
                s.getMean(),
                s.getStandardDeviation(),
                sum,
                sum / refSum,
                sum - refSum));
        }

        return time;
    }

    /**
     * Timing and report (to standard output).
     * This method calls {@link #timeAndReport(String,int,int,boolean,RunTest[])
     * timeAndReport(title, 1000, 10000, false, methods)}.
     * 
     * @param title
     *        Title of the test (for the report).
     * @param methods
     *        Codes being timed.
     * @return for each of the given {@code methods}, a statistics of the
     *         average times (in milliseconds) taken by a single call to the {@code call} method (i.e. the time taken by
     *         each timed block divided
     *         by {@code repeatChunk}).
     */
    public static StatisticalSummary[] timeAndReport(final String title,
                                                     final RunTest... methods) {
        return timeAndReport(title,
            DEFAULT_REPEAT_CHUNK,
            DEFAULT_REPEAT_STAT,
            false,
            methods);
    }

    /**
     * Utility class for storing a test label.
     */
    public static abstract class RunTest implements Callable<Double> {
        private final String name;

        /**
         * @param name
         *        Test name.
         */
        public RunTest(final String name) {
            this.name = name;
        }

        /**
         * @return the name of this test.
         */
        public String getName() {
            return this.name;
        }

        /** {@inheritDoc} */
        @Override
        public abstract Double call() throws Exception;
    }
}
