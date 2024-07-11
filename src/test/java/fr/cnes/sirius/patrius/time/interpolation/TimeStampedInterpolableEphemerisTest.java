/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.time.interpolation;

import java.util.Arrays;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.random.GaussianRandomGenerator;
import fr.cnes.sirius.patrius.math.random.JDKRandomGenerator;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.time.interpolation.TimeStampedInterpolableEphemeris.SearchMethod;

/**
 * Unit test class for the {@link TimeStampedInterpolableEphemeris} class.
 * 
 * @author bonitt
 */
public class TimeStampedInterpolableEphemerisTest {

    /** Initialized simple interpolation function. */
    private static TestInterpolationFunctionBuilder simpleBuilder;

    /** Initialized samples. */
    private static AbsoluteDate[] samples;

    // TODO discuter de l'ajout d'un test multi-thread (le interpolation function builder risque de ne pas bien
    // fonctionner en multi)

    /**
     * @description Builds a new instance and tests the basic getters.
     *
     * @testedMethod {@link TimeStampedInterpolableEphemeris#TimeStampedInterpolableEphemeris(TimeStamped[], int, TimeStampedInterpolationFunctionBuilder, boolean)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#TimeStampedInterpolableEphemeris(TimeStamped[], int, TimeStampedInterpolationFunctionBuilder, boolean, boolean, boolean, int)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getSamples(boolean)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getSampleSize()}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#isAcceptOutOfOptimalRange()}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getFirstDate()}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getFirstSample()}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getLastDate()}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getLastSample()}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getFloorIndex(AbsoluteDate)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getCeilingIndex(AbsoluteDate)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getFloorSample(AbsoluteDate)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getCeilingSample(AbsoluteDate)}
     *
     * @testPassCriteria The instance is build without error and the basic getters return the expected data.
     */
    @Test
    public void testConstructor() {

        TimeStampedInterpolableEphemeris<AbsoluteDate, AbsoluteDate> interpolableEphem =
            new TimeStampedInterpolableEphemeris<>(samples, 4, simpleBuilder, true);

        Assert.assertEquals(samples.length, interpolableEphem.getSampleSize());
        // Samples without copy shouldn't be the same instance
        Assert.assertFalse(interpolableEphem.getSamples(false).equals(samples));
        Assert.assertTrue(interpolableEphem.isAcceptOutOfOptimalRange()); // Expect true by default

        Assert.assertEquals(samples[0], interpolableEphem.getFirstDate());
        Assert.assertEquals(samples[0], interpolableEphem.getFirstSample());
        Assert.assertEquals(samples[samples.length - 1], interpolableEphem.getLastDate());
        Assert.assertEquals(samples[samples.length - 1], interpolableEphem.getLastSample());

        Assert.assertEquals(-1, interpolableEphem.getFloorIndex(samples[0].shiftedBy(-1.)));
        Assert.assertEquals(0, interpolableEphem.getFloorIndex(samples[0])); // Test the quick escape
        Assert.assertEquals(0, interpolableEphem.getFloorIndex(samples[0].shiftedBy(1.)));
        Assert.assertEquals(5, interpolableEphem.getFloorIndex(samples[5]));
        Assert.assertEquals(3, interpolableEphem.getFloorIndex(samples[3].shiftedBy(1.)));
        Assert.assertEquals(samples.length - 2,
            interpolableEphem.getFloorIndex(samples[samples.length - 1].shiftedBy(-1.)));
        Assert.assertEquals(samples.length - 1,
            interpolableEphem.getFloorIndex(samples[samples.length - 1].shiftedBy(1.)));

        Assert.assertEquals(0, interpolableEphem.getCeilingIndex(samples[0].shiftedBy(-1.)));
        Assert.assertEquals(1, interpolableEphem.getCeilingIndex(samples[0].shiftedBy(1.)));
        Assert.assertEquals(3, interpolableEphem.getCeilingIndex(samples[3]));
        Assert.assertEquals(4, interpolableEphem.getCeilingIndex(samples[3].shiftedBy(1.)));
        Assert.assertEquals(samples.length - 1,
            interpolableEphem.getCeilingIndex(samples[samples.length - 1].shiftedBy(-1.)));
        Assert.assertEquals(samples.length - 1, interpolableEphem.getCeilingIndex(samples[samples.length - 1]));
        Assert.assertEquals(-1, interpolableEphem.getCeilingIndex(samples[samples.length - 1].shiftedBy(1.)));

        Assert.assertEquals(samples[0], interpolableEphem.getFloorSample(samples[0].shiftedBy(1.)));
        Assert.assertEquals(samples[3], interpolableEphem.getFloorSample(samples[3].shiftedBy(1.)));
        Assert.assertEquals(samples[samples.length - 2],
            interpolableEphem.getFloorSample(samples[samples.length - 1].shiftedBy(-1.)));
        Assert.assertEquals(samples[samples.length - 1],
            interpolableEphem.getFloorSample(samples[samples.length - 1].shiftedBy(1.)));

        Assert.assertEquals(samples[0], interpolableEphem.getCeilingSample(samples[0].shiftedBy(-1.)));
        Assert.assertEquals(samples[1], interpolableEphem.getCeilingSample(samples[0].shiftedBy(1.)));
        Assert.assertEquals(samples[4], interpolableEphem.getCeilingSample(samples[3].shiftedBy(1.)));
        Assert.assertEquals(samples[samples.length - 1],
            interpolableEphem.getCeilingSample(samples[samples.length - 1].shiftedBy(-1.)));

        interpolableEphem = new TimeStampedInterpolableEphemeris<>(samples, 4, simpleBuilder, false, false, false, 3);

        // Samples without copy should be the same instance
        Assert.assertTrue(interpolableEphem.getSamples(false).equals(samples));
        // Samples without copy shouldn't be the same instance
        Assert.assertFalse(interpolableEphem.getSamples(true).equals(samples));
        Assert.assertFalse(interpolableEphem.isAcceptOutOfOptimalRange());
    }

    /**
     * @description Evaluate that the interpolation method uses the good inferior and superior index (interpolation on
     *              exact match dates).
     *
     * @testedMethod {@link TimeStampedInterpolableEphemeris#TimeStampedInterpolableEphemeris(TimeStamped[], int, TimeStampedInterpolationFunctionBuilder, boolean)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#TimeStampedInterpolableEphemeris(TimeStamped[], int, TimeStampedInterpolationFunctionBuilder, boolean, boolean, boolean, int)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getSearchMethod()}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#setSearchMethod(SearchMethod)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#interpolate(AbsoluteDate)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getCacheReusabilityRatio()}
     * @testedMethod {@link SearchMethod#DICHOTOMY}
     * @testedMethod {@link SearchMethod#PROPORTIONAL}
     *
     * @testPassCriteria The expected inferior and superior index are used by the interpolation method.
     */
    @Test
    public void testInterpolateExactMatch() {

        // Expected index checked manually for the order 2 (independently of the search method)
        final int[][] expectedIndexInfSup2 =
            new int[][] { { 0, 2 }, { 0, 2 }, { 2, 4 }, { 2, 4 }, { 4, 6 }, { 4, 6 }, { 6, 8 }, { 6, 8 }, { 8, 10 },
                { 8, 10 } };
        // Expected index checked manually for the order 4 (independently of the search method)
        final int[][] expectedIndexInfSup4 =
            new int[][] { { 0, 4 }, { 0, 4 }, { 1, 5 }, { 1, 5 }, { 3, 7 }, { 3, 7 }, { 5, 9 }, { 5, 9 }, { 6, 10 },
                { 6, 10 } };

        evaluateInterpolateExactMatch(SearchMethod.PROPORTIONAL, 2, expectedIndexInfSup2);
        evaluateInterpolateExactMatch(SearchMethod.DICHOTOMY, 2, expectedIndexInfSup2);

        evaluateInterpolateExactMatch(SearchMethod.PROPORTIONAL, 4, expectedIndexInfSup4);
        evaluateInterpolateExactMatch(SearchMethod.DICHOTOMY, 4, expectedIndexInfSup4);
    }

    /**
     * Evaluate that the interpolation method uses the good inferior and superior index (interpolation on exact match
     * dates).
     * 
     * @param searchMethod
     *        Search method to evaluate
     * @param order
     *        Order
     * @param expectedIndexInfSup
     *        Reference inferior and superior index array
     */
    private static void evaluateInterpolateExactMatch(final SearchMethod searchMethod, final int order,
                                                      final int[][] expectedIndexInfSup) {

        // Build the interpolable ephemeris
        final TimeStampedInterpolableEphemeris<AbsoluteDate, AbsoluteDate> interpolableEphem =
            new TimeStampedInterpolableEphemeris<>(samples, order, simpleBuilder, true);
        interpolableEphem.setSearchMethod(searchMethod);

        simpleBuilder.clear(); // Reset the builder

        // Interpolate each date and check that the expected indexInf & indexSup are found
        for (int i = 0; i < samples.length; i++) {
            // Only called to update the indexInf, indexSup and the counter in the simpleBuilder
            final AbsoluteDate out = interpolableEphem.interpolate(samples[i]);

            final int expectedIndexInf = expectedIndexInfSup[i][0];
            Assert.assertEquals(samples[expectedIndexInf], out);
            Assert.assertEquals(expectedIndexInf, simpleBuilder.getIndexInf());
            Assert.assertEquals(expectedIndexInfSup[i][1], simpleBuilder.getIndexSup());
        }
        Assert.assertEquals(5, simpleBuilder.getCounter());
        Assert.assertEquals(0.5, interpolableEphem.getCacheReusabilityRatio(), 0.);

        // Interpolate each sample a second time and check that the counter hasn't increased, meaning the cache is fully
        // reused
        for (int i = 0; i < samples.length; i++) {
            final AbsoluteDate out = interpolableEphem.interpolate(samples[i]);

            Assert.assertEquals(samples[expectedIndexInfSup[i][0]], out);
        }
        Assert.assertEquals(5, simpleBuilder.getCounter()); // No increase as the cache should have been fully reused
        Assert.assertEquals(0.75, interpolableEphem.getCacheReusabilityRatio(), 0.);
    }

    /**
     * @description Evaluate that the interpolation method uses the good inferior and superior index (interpolation
     *              between two samples).
     *
     * @testedMethod {@link TimeStampedInterpolableEphemeris#TimeStampedInterpolableEphemeris(TimeStamped[], int, TimeStampedInterpolationFunctionBuilder, boolean)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#TimeStampedInterpolableEphemeris(TimeStamped[], int, TimeStampedInterpolationFunctionBuilder, boolean, boolean, boolean, int)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getSearchMethod()}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#setSearchMethod(SearchMethod)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#interpolate(AbsoluteDate)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getCacheReusabilityRatio()}
     * @testedMethod {@link SearchMethod#DICHOTOMY}
     * @testedMethod {@link SearchMethod#PROPORTIONAL}
     *
     * @testPassCriteria The expected inferior and superior index are used by the interpolation method.
     */
    @Test
    public void testInterpolateBetweenTwoSamples() {

        // Expected index checked manually for the order 2 (independently of the search method)
        final int[][] expectedIndexInfSup2 =
            new int[][] { { 0, 2 }, { 0, 2 }, { 0, 2 }, { 0, 2 }, { 1, 3 }, { 1, 3 }, { 1, 3 }, { 2, 4 }, { 2, 4 },
                { 2, 4 }, { 3, 5 },
                { 3, 5 }, { 3, 5 }, { 4, 6 }, { 4, 6 }, { 4, 6 }, { 5, 7 }, { 5, 7 }, { 5, 7 }, { 6, 8 }, { 6, 8 },
                { 6, 8 }, { 7, 9 },
                { 7, 9 }, { 7, 9 }, { 8, 10 }, { 8, 10 }, { 8, 10 } };

        // Expected index checked manually for the order 4 (independently of the search method)
        final int[][] expectedIndexInfSup4 =
            new int[][] { { 0, 4 }, { 0, 4 }, { 0, 4 }, { 0, 4 }, { 0, 4 }, { 0, 4 }, { 0, 4 }, { 1, 5 }, { 1, 5 },
                { 1, 5 }, { 2, 6 },
                { 2, 6 }, { 2, 6 }, { 3, 7 }, { 3, 7 }, { 3, 7 }, { 4, 8 }, { 4, 8 }, { 4, 8 }, { 5, 9 }, { 5, 9 },
                { 5, 9 }, { 6, 10 },
                { 6, 10 }, { 6, 10 }, { 6, 10 }, { 6, 10 }, { 6, 10 } };

        evaluateInterpolateBetweenTwoSamples(SearchMethod.PROPORTIONAL, 2, expectedIndexInfSup2);
        evaluateInterpolateBetweenTwoSamples(SearchMethod.DICHOTOMY, 2, expectedIndexInfSup2);

        evaluateInterpolateBetweenTwoSamples(SearchMethod.PROPORTIONAL, 4, expectedIndexInfSup4);
        evaluateInterpolateBetweenTwoSamples(SearchMethod.DICHOTOMY, 4, expectedIndexInfSup4);
    }

    /**
     * Evaluate that the interpolation method uses the good inferior and superior index (interpolation between two
     * samples).
     * 
     * @param searchMethod
     *        Search method to evaluate
     * @param order
     *        Order
     * @param expectedIndexInfSup
     *        Reference inferior and superior index array
     */
    private static void evaluateInterpolateBetweenTwoSamples(final SearchMethod searchMethod, final int order,
                                                             final int[][] expectedIndexInfSup) {

        // Build the interpolable ephemeris (cache limit increased to cover all the interpolations)
        final TimeStampedInterpolableEphemeris<AbsoluteDate, AbsoluteDate> interpolableEphem =
            new TimeStampedInterpolableEphemeris<>(samples, order, simpleBuilder, true, true, true, 9);
        interpolableEphem.setSearchMethod(searchMethod);

        simpleBuilder.clear(); // Reset the builder

        final int subSize = 3;
        final double duration = samples[1].durationFrom(samples[0]); // samples are proprotionnal
        final double step = duration / subSize;

        // Interpolate between each samples couple and check that the expected indexInf & indexSup are found
        for (int i = 0; i < samples.length; i++) {
            for (int j = 0; j < subSize; j++) {
                if (i == samples.length - 1 && j > 0) { // Don't evaluate the sub-samples after the last sample
                    break;
                }
                // Only called to update the indexInf, indexSup and the counter in the simpleBuilder
                final AbsoluteDate out = interpolableEphem.interpolate(samples[i].shiftedBy(step * j));

                final int expectedIndexInf = expectedIndexInfSup[i * subSize + j][0];
                Assert.assertEquals(samples[expectedIndexInf], out);
                Assert.assertEquals(expectedIndexInf, simpleBuilder.getIndexInf());
                Assert.assertEquals(expectedIndexInfSup[i * subSize + j][1], simpleBuilder.getIndexSup());
            }
        }
        Assert.assertEquals(9, simpleBuilder.getCounter());
        Assert.assertEquals(0.6785714285714286, interpolableEphem.getCacheReusabilityRatio(), 0.);

        // Interpolate between each samples couple a second time and check that the counter hasn't increased,
        // meaning the cache is fully reused
        // Note: loop on reverse order as the cache is stored in FIFO order to evaluate the output date on the
        // appropriate interval
        for (int i = samples.length - 1; i >= 0; i--) {
            for (int j = subSize - 1; j >= 0; j--) {
                if (i != samples.length - 1 || j == 0) { // Don't evaluate the sub-samples after the last sample
                    // System.out.println(samples[i].shiftedBy(step * j));
                    final AbsoluteDate out = interpolableEphem.interpolate(samples[i].shiftedBy(step * j));

                    final AbsoluteDate dateInf = samples[expectedIndexInfSup[i * subSize + j][0]];
                    final AbsoluteDateInterval expectedInterval;
                    if (i == samples.length - 1) {
                        expectedInterval = new AbsoluteDateInterval(dateInf, dateInf); // Upper interval
                    } else {
                        expectedInterval = new AbsoluteDateInterval(dateInf, samples[expectedIndexInfSup[i * subSize
                                + j + 1][0]]);
                    }
                    Assert.assertTrue(expectedInterval.contains(out));
                }
            }
        }
        Assert.assertEquals(9, simpleBuilder.getCounter()); // No increase as the cache should be been fully reused
        Assert.assertEquals(0.8392857142857143, interpolableEphem.getCacheReusabilityRatio(), 0.);
    }

    /**
     * @description Evaluate how quick the different {@link SearchMethod search methods} perform to converge to the
     *              appropriate interpolation index.<br>
     *              This test aims to make sure there is no regression on the search methods performances and it also
     *              allows to compare new search methods performances to the others ones.
     *
     * @testedMethod {@link TimeStampedInterpolableEphemeris#interpolate(AbsoluteDate)}
     * @testedMethod {@link SearchMethod#DICHOTOMY}
     * @testedMethod {@link SearchMethod#PROPORTIONAL}
     *
     * @testPassCriteria The expected counters are obtained by each case.<br>
     *                   The lower the counter is, the quicker the search method can converge to the appropriate
     *                   interpolation index.
     */
    @Test
    public void testSearchMethodsPerformances() {

        // Build the samples to evaluate for interpolation
        final int size = 50;
        final TestAbsoluteDate[] samplesProportional = new TestAbsoluteDate[size];
        final TestAbsoluteDate[] samplesNonProportional = new TestAbsoluteDate[size];
        final TestAbsoluteDate[] samplesRandom = new TestAbsoluteDate[size];

        final RandomGenerator generator = new JDKRandomGenerator();
        generator.setSeed(17399225432l);
        final GaussianRandomGenerator gaussianRandomGenerator = new GaussianRandomGenerator(generator);

        for (int i = 0; i < size; i++) {
            samplesProportional[i] = new TestAbsoluteDate(AbsoluteDate.J2000_EPOCH.shiftedBy(60. * i));
            // y = a * x (proportional)
            samplesNonProportional[i] = new TestAbsoluteDate(AbsoluteDate.J2000_EPOCH.shiftedBy(60. * i * i));
            // y = a * x^2 (non-prop)
            samplesRandom[i] = // (non-proportional, random shift)
            new TestAbsoluteDate(AbsoluteDate.J2000_EPOCH.shiftedBy(60. * i
                    + gaussianRandomGenerator.nextNormalizedDouble()));
        }

        // Build the expected counters arrays (non regression)
        final int[] expectedCounters1 = new int[] { 1, 2, 3, 2, 3, 2, 2, 2, 3, 2, 2, 2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, };
        final int[] expectedCounters2 = new int[] { 1, 6, 7, 5, 6, 7, 4, 6, 7, 5, 6, 7, 3, 6, 7, 5, 6, 7, 4, 6, 7, 5,
            6, 7, 2, 6, 7, 5, 6, 7, 4, 6, 7, 5, 6, 7, 3, 6, 7, 5, 6, 7, 4, 6, 7, 5, 7, 6, 7, 1, };
        final int[] expectedCounters3 = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 9, 9, 8, 8, 8, 7, 7, 7,
            7, 7, 7, 7, 7, 6, 6, 6, 6, 6, 5, 5, 5, 5, 5, 5, 5, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 1, };
        final int[] expectedCounters4 = new int[] { 1, 6, 7, 5, 6, 7, 4, 6, 7, 5, 6, 7, 3, 6, 7, 5, 6, 7, 4, 6, 7, 5,
            6, 7, 2, 6, 7, 5, 6, 7, 4, 6, 7, 5, 6, 7, 3, 6, 7, 5, 6, 7, 4, 6, 7, 5, 7, 6, 7, 1, };
        final int[] expectedCounters5 = new int[] { 1, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            3, 2, 3, 3, 3, 3, 3, 3, 2, 3, 3, 2, 2, 3, 2, 3, 2, 3, 3, 3, 2, 2, 3, 2, 2, 2, 2, 1, };
        final int[] expectedCounters6 = new int[] { 1, 6, 7, 5, 6, 7, 4, 6, 7, 5, 6, 7, 3, 6, 7, 5, 6, 7, 4, 6, 7, 5,
            6, 7, 2, 6, 7, 5, 6, 7, 4, 6, 7, 5, 6, 7, 3, 6, 7, 5, 6, 7, 4, 6, 7, 5, 7, 6, 7, 1, };

        // Evaluate the different cases
        evaluateSearchMethod(samplesProportional, SearchMethod.PROPORTIONAL, expectedCounters1);
        evaluateSearchMethod(samplesProportional, SearchMethod.DICHOTOMY, expectedCounters2);

        evaluateSearchMethod(samplesNonProportional, SearchMethod.PROPORTIONAL, expectedCounters3);
        evaluateSearchMethod(samplesNonProportional, SearchMethod.DICHOTOMY, expectedCounters4);

        evaluateSearchMethod(samplesRandom, SearchMethod.PROPORTIONAL, expectedCounters5);
        evaluateSearchMethod(samplesRandom, SearchMethod.DICHOTOMY, expectedCounters6);
    }

    /**
     * Search method performances evaluation.
     * <p>
     * An interpolable ephemeris is built with the given samples and the given search method. Then, the samples are
     * interpolated on each match (no sub-intervals are interpolated).<br>
     * We count each time the {@link AbsoluteDate#getDate() getDate()} method is called (the lower it is, the quicker
     * the search method converge to the appropriate index).<br>
     * The results are compared to the reference one.
     * </p>
     * 
     * @param samplesToEvaluate
     *        Samples (can be proportional, non-proportional, etc to evaluate the search method performances on
     *        different repartitions)
     * @param searchMethod
     *        Search method to evaluate
     * @param expectedCounters
     *        Reference counters
     */
    private static void evaluateSearchMethod(final TestAbsoluteDate[] samplesToEvaluate,
                                             final SearchMethod searchMethod,
                                             final int[] expectedCounters) {

        final int size = samplesToEvaluate.length;
        if (size != expectedCounters.length) {
            Assert.fail(); // Dimension error with the inputs
        }

        // Build the interpolable ephemeris with specified samples and search methods (order 2, no cache)
        final TimeStampedInterpolableEphemeris<AbsoluteDate, AbsoluteDate> interpolableEphem =
            new TimeStampedInterpolableEphemeris<>(samplesToEvaluate, 2, simpleBuilder, true, true, true, 0);
        interpolableEphem.setSearchMethod(searchMethod);

        // Reset the builder and the counter
        simpleBuilder.clear();
        TestAbsoluteDate.clear();

        final int[] counters = new int[size];
        for (int i = 0; i < size; i++) {
            interpolableEphem.interpolate(samplesToEvaluate[i]);
            counters[i] = TestAbsoluteDate.getCounter();
            TestAbsoluteDate.clear();
        }

        Assert.assertTrue(Arrays.equals(expectedCounters, counters));

        // Reset the builder and the counter (for safety)
        simpleBuilder.clear();
        TestAbsoluteDate.clear();
    }

    /**
     * @description Evaluate the interpolable ephemeris serialization / deserialization process.
     *
     * @testPassCriteria The interpolable ephemeris can be serialized / deserialized.
     */
    @Test
    public void testSerialization() {

        // Build the interpolable ephemeris with some samples
        final TimeStampedInterpolableEphemeris<AbsoluteDate, AbsoluteDate> interpolableEphem =
            new TimeStampedInterpolableEphemeris<>(samples, 4, simpleBuilder, true, false, false, 7);
        interpolableEphem.setSearchMethod(SearchMethod.DICHOTOMY); // Not default value

        // Serialize and deserialize the interpolable ephemeris, then evaluate it
        final TimeStampedInterpolableEphemeris<AbsoluteDate, AbsoluteDate> deserializedInterpolableEphem =
            TestUtils.serializeAndRecover(interpolableEphem);

        // The samples and the search method should be the same
        Assert.assertEquals(interpolableEphem.getSampleSize(), deserializedInterpolableEphem.getSampleSize());
        Assert.assertTrue(Arrays.equals(interpolableEphem.getSamples(false),
            deserializedInterpolableEphem.getSamples(false)));
        Assert.assertTrue(deserializedInterpolableEphem.isAcceptOutOfOptimalRange());
        Assert.assertEquals(interpolableEphem.getSearchMethod(), deserializedInterpolableEphem.getSearchMethod());

        // Evaluate the interpolation is the same too
        for (int i = 0; i < samples.length; i++) {
            final AbsoluteDate expectedOut = interpolableEphem.interpolate(samples[i]);
            final AbsoluteDate out = deserializedInterpolableEphem.interpolate(samples[i]);
            Assert.assertEquals(expectedOut, out);
        }
        Assert.assertEquals(interpolableEphem.getCacheReusabilityRatio(),
            deserializedInterpolableEphem.getCacheReusabilityRatio(), 0.);
        simpleBuilder.clear(); // Reset the builder
    }

    /**
     * @description Try to build an angular actors container with initialization errors.
     *
     * @testedMethod {@link TimeStampedInterpolableEphemeris#TimeStampedInterpolableEphemeris(TimeStamped[], int, TimeStampedInterpolationFunctionBuilder, boolean)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#TimeStampedInterpolableEphemeris(TimeStamped[], int, TimeStampedInterpolationFunctionBuilder, boolean, boolean, boolean, int)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#setSearchMethod(SearchMethod)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getFloorSample(AbsoluteDate)}
     * @testedMethod {@link TimeStampedInterpolableEphemeris#getCeilingSample(AbsoluteDate)}
     *
     * @testPassCriteria The exceptions are returned as expected.
     */
    @Test
    public void testInitializationError() {

        TimeStampedInterpolableEphemeris<AbsoluteDate, AbsoluteDate> interpolableEphem =
            new TimeStampedInterpolableEphemeris<>(samples, 4, simpleBuilder, true);

        // Try to initialize an interpolable ephemeris with null input
        try {
            new TimeStampedInterpolableEphemeris<>(null, 4, simpleBuilder, true);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new TimeStampedInterpolableEphemeris<>(samples, 4, null, true);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to use unsorted samples
        final AbsoluteDate[] unsortedSamples = new AbsoluteDate[samples.length];
        for (int i = 0; i < unsortedSamples.length; i++) {
            unsortedSamples[i] = AbsoluteDate.J2000_EPOCH.shiftedBy(-60. * i);
        }

        try {
            new TimeStampedInterpolableEphemeris<>(unsortedSamples, 4, simpleBuilder, true);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected as the checkStrictlySorted boolean is enabled by default by this constructor
            Assert.assertTrue(true);
        }
        try {
            new TimeStampedInterpolableEphemeris<>(unsortedSamples, 4, simpleBuilder, true, true, false, 3);
            Assert.assertTrue(true);
        } catch (final IllegalArgumentException e) {
            // not expected as the checkStrictlySorted boolean is disabled by this constructor
            Assert.fail();
        }

        // Try to use an odd number order or an order lower than 2
        try {
            new TimeStampedInterpolableEphemeris<>(samples, 3, simpleBuilder, true);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new TimeStampedInterpolableEphemeris<>(samples, 0, simpleBuilder, true);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to use a samples array with a length lower than the order
        try {
            new TimeStampedInterpolableEphemeris<>(samples, samples.length + 2, simpleBuilder, true);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to use a negative cache size
        try {
            new TimeStampedInterpolableEphemeris<>(samples, 4, simpleBuilder, true, true, true, -1);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to set a search method with null input
        try {
            interpolableEphem.setSearchMethod(null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to get the floor sample of a date before the first sample
        try {
            interpolableEphem.getFloorSample(samples[0].shiftedBy(-1.));
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to get the ceiling sample of a date after the last sample
        try {
            interpolableEphem.getCeilingSample(samples[samples.length - 1].shiftedBy(1.));
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to interpolate a date outside the samples
        try {
            interpolableEphem.interpolate(samples[0].shiftedBy(-1.));
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to interpolate a date too close to the bounds wrt to the order with acceptOutOfRange = false
        interpolableEphem = new TimeStampedInterpolableEphemeris<>(samples, 4, simpleBuilder, false);

        try {
            interpolableEphem.interpolate(samples[0].shiftedBy(1.));
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            interpolableEphem.interpolate(samples[samples.length - 1].shiftedBy(-1.));
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /** The following code is executed once before all the tests : global parameters initialization. */
    @BeforeClass
    public static void setupClass() {

        simpleBuilder = new TestInterpolationFunctionBuilder();

        final int size = 10;
        samples = new AbsoluteDate[size];
        for (int i = 0; i < size; i++) {
            samples[i] = AbsoluteDate.J2000_EPOCH.shiftedBy(60. * i);
        }
    }

    /**
     * Dummy date only used for tests (increase a counter each time the {@link AbsoluteDate#getDate() getDate()} method
     * is called.
     */
    private static class TestAbsoluteDate extends AbsoluteDate {

        /** Serializable UID. */
        private static final long serialVersionUID = -8105617138681984337L;

        /** Counter. */
        private static int counter;

        public TestAbsoluteDate(final AbsoluteDate date) {
            super(date.getEpoch(), date.getOffset());
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            counter++;
            return this;
        }

        /**
         * Reset the parameters.
         */
        public static void clear() {
            counter = 0;
        }

        /**
         * Getter for the counter.
         *
         * @return the counter
         */
        public static int getCounter() {
            return counter;
        }
    }

    /** Dummy interpolation function builder only used for tests. */
    private static class TestInterpolationFunctionBuilder
        implements TimeStampedInterpolationFunctionBuilder<AbsoluteDate, AbsoluteDate> {

        /** Serializable UID. */
        private static final long serialVersionUID = 8644386371452900551L;

        /** Inferior index. */
        private int indexInf;

        /** Superior index. */
        private int indexSup;

        /** Counter. */
        private int counter;

        /**
         * {@inheritDoc}
         * 
         * <p>
         * The aim of this dummy function is mainly to store the inferior index, the superior index and to update the
         * counter.<br>
         * These values can then be checked compared to the expected ones.
         * </p>
         */
        @Override
        public Function<AbsoluteDate, ? extends AbsoluteDate> buildInterpolationFunction(final AbsoluteDate[] samples,
                                                                                         final int indexInf,
                                                                                         final int indexSup) {
            this.indexInf = indexInf;
            this.indexSup = indexSup;
            this.counter++;
            return (date) -> samples[indexInf];
        }

        /**
         * Reset the parameters.
         */
        public void clear() {
            this.indexInf = 0;
            this.indexSup = 0;
            this.counter = 0;
        }

        /**
         * Getter for the inferior index.
         *
         * @return the inferior index
         */
        public int getIndexInf() {
            return this.indexInf;
        }

        /**
         * Getter for the superior index.
         *
         * @return the superior index
         */
        public int getIndexSup() {
            return this.indexSup;
        }

        /**
         * Getter for the counter.
         *
         * @return the counter
         */
        public int getCounter() {
            return this.counter;
        }
    }
}
