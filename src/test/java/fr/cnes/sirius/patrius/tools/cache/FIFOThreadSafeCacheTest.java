/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.tools.cache;

import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;

/**
 * Unit test class for the {@link FIFOThreadSafeCache} class.
 * 
 * @author veuillh, bonitt
 */
public class FIFOThreadSafeCacheTest {

    /**
     * @description Builds a new instance and tests the basic getters.
     *
     * @testedMethod {@link FIFOThreadSafeCache#FIFOThreadSafeCache()}
     * @testedMethod {@link FIFOThreadSafeCache#FIFOThreadSafeCache(int)}
     * @testedMethod {@link FIFOThreadSafeCache#getMaxSize()}
     * @testedMethod {@link FIFOThreadSafeCache#getReusabilityRatio()}
     * @testedMethod {@link FIFOThreadSafeCache#clear()}
     *
     * @testPassCriteria The instance is build without error and the basic getters return the expected data.
     */
    @Test
    public void testConstructor() {

        FIFOThreadSafeCache<Integer, Integer> cache = new FIFOThreadSafeCache<>(); // Default size = 8
        Assert.assertEquals(8, cache.getMaxSize());

        unitaryRun(10, 1, 0, cache);
        Assert.assertEquals(0., cache.getReusabilityRatio(), 0.);  // Cache repeat = 1 (no repeat) -> Reusability ratio should be 0%

        cache = new FIFOThreadSafeCache<>(3);
        Assert.assertEquals(3, cache.getMaxSize());

        unitaryRun(10, 10, 0, cache);
        Assert.assertEquals(0.9, cache.getReusabilityRatio(), 0.);  // Cache repeat = 10 -> Reusability ratio should be 90%

        cache.clear();
        Assert.assertTrue(Double.isNaN(cache.getReusabilityRatio()));

        // Evaluate the cache disabled is well supported
        cache = new FIFOThreadSafeCache<>(0);
        unitaryRun(10, 10, 0, cache);
    }

    /**
     * @description Evaluate the cache access with a monothread analysis.
     *
     * @testedMethod {@link FIFOThreadSafeCache#FIFOThreadSafeCache()}
     * @testedMethod {@link FIFOThreadSafeCache#computeIfAbsent(Object, Supplier)}
     * @testedMethod {@link FIFOThreadSafeCache#computeIf(java.util.function.Predicate, Supplier)}
     * 
     * @testPassCriteria The cache gives access to the expected values in a monothread analysis.
     */
    @Test
    public void testMonoThreadFIFOThreadSafeCache() {
        final FIFOThreadSafeCache<Integer, Integer> fifoCache = new FIFOThreadSafeCache<>();
        // Parameters of the run
        final int nbKeys = 100;
        final int cacheRepeat = 10;
        final int nbOps = 1_000;

        try {
            unitaryRun(nbKeys, cacheRepeat, nbOps, fifoCache);
            // No exception: the test passed
            Assert.assertTrue(true);
        } catch (final Exception e) {
            System.out.println(e);
            // An exception occurred: fail
            Assert.assertTrue(false);
        }
    }

    /**
     * @description Evaluate the cache access with a multi-threads analysis.
     *
     * @testedMethod {@link FIFOThreadSafeCache#FIFOThreadSafeCache()}
     * @testedMethod {@link FIFOThreadSafeCache#computeIfAbsent(Object, Supplier)}
     * @testedMethod {@link FIFOThreadSafeCache#computeIf(java.util.function.Predicate, Supplier)}
     *
     * @testPassCriteria The cache gives access to the expected values in a multi-threads analysis.
     */
    @Test
    public void testMultiThreadFIFOThreadSafeCache() {
        final FIFOThreadSafeCache<Integer, Integer> fifoCache = new FIFOThreadSafeCache<>();
        // Parameters of the run
        final int nbKeys = 100;
        final int cacheRepeat = 10;
        final int nbOps = 1_000;

        try {
            // Light entry computation
            runParallel(nbKeys, cacheRepeat, nbOps, fifoCache, 10);
            // Heavier entry computation
            runParallel(nbKeys, cacheRepeat, 10_000 * nbOps, fifoCache, 10);
            // No exception: the test passed
            Assert.assertTrue(true);
        } catch (final Exception e) {
            // An exception occurred: fail
            Assert.assertTrue(false);
        }
    }

    /**
     * @description Check the String representation method behavior.
     *
     * @testedMethod {@link FIFOThreadSafeCache#toString()}
     *
     * @testPassCriteria The cache String representation contains the expected information.
     */
    @Test
    public void testToString() {

        final FIFOThreadSafeCache<Integer, Integer> cache8 = new FIFOThreadSafeCache<>(); // Default size = 8
        final FIFOThreadSafeCache<Integer, Integer> cache3 = new FIFOThreadSafeCache<>(3);

        unitaryRun(10, 1, 0, cache8);
        unitaryRun(10, 2, 0, cache3);

        final String expectedText8 = "==============================\n" +
                "|     FIFOThreadSafeCache    |\n" +
                "==============================\n" +
                "|              Key  | Value  |\n" +
                "==============================\n" +
                "|                 9 |      9 |\n" +
                "|                 8 |      8 |\n" +
                "|                 7 |      7 |\n" +
                "|                 6 |      6 |\n" +
                "|                 5 |      5 |\n" +
                "|                 4 |      4 |\n" +
                "|                 3 |      3 |\n" +
                "|                 2 |      2 |\n" +
                "==============================\n" +
                "| Reusability ratio |  0.00% |\n" +
                "==============================\n";
        Assert.assertEquals(expectedText8, cache8.toString());

        final String expectedText3 = "==============================\n" +
                "|     FIFOThreadSafeCache    |\n" +
                "==============================\n" +
                "|              Key  | Value  |\n" +
                "==============================\n" +
                "|                 9 |      9 |\n" +
                "|                 8 |      8 |\n" +
                "|                 7 |      7 |\n" +
                "==============================\n" +
                "| Reusability ratio | 50.00% |\n" +
                "==============================\n";
        Assert.assertEquals(expectedText3, cache3.toString());
    }

    /**
     * @description Evaluate the cache serialization / deserialization process.
     *
     * @testPassCriteria The cache can be serialized (only its "listMaxSize" parameter should be deserialized).
     */
    @Test
    public void testSerialization() {

        // Initialize the cache and fill it
        final FIFOThreadSafeCache<Integer, Integer> cache = new FIFOThreadSafeCache<>(7);
        unitaryRun(10, 1, 0, cache);

        // Serialize (only its "listMaxSize" parameter should be serialized) and deserialize the cache, then evaluate it
        final FIFOThreadSafeCache<Integer, Integer> deserializedCache = TestUtils.serializeAndRecover(cache);

        // Check the structure is empty (not serialized) through the toString representation
        final String expectedText = "==============================\n" +
                "|     FIFOThreadSafeCache    |\n" +
                "==============================\n" +
                "|              Key  | Value  |\n" +
                "==============================\n" +
                "==============================\n" +
                "| Reusability ratio |   NaN% |\n" +
                "==============================\n";
        Assert.assertEquals(expectedText, deserializedCache.toString());

        // Check the nbCallComputeIf and the nbCallAddEntry parameters are null (0) (not serialized) through the getReusabilityRatio method
        Assert.assertTrue(Double.isNaN(deserializedCache.getReusabilityRatio()));

        // Check the listMaxSize parameter is well serialized through the getMaxSize method
        Assert.assertEquals(7, deserializedCache.getMaxSize());

        unitaryRun(10, 1, 0, deserializedCache); // Fill the deserialized cache the same way to original cache was filled

        // Evaluate the two caches
        Assert.assertEquals(cache.toString(), deserializedCache.toString());
        Assert.assertEquals(cache.getReusabilityRatio(), deserializedCache.getReusabilityRatio(), 0.);
    }

    /**
     * @description Try to build an angular actors container with initialization errors.
     *
     * @testedMethod {@link FIFOThreadSafeCache#FIFOThreadSafeCache(int)}
     * @testedMethod {@link FIFOThreadSafeCache#computeIfAbsent(Object, Supplier)}
     * @testedMethod {@link FIFOThreadSafeCache#computeIf(java.util.function.Predicate, Supplier)}
     *
     * @testPassCriteria The exceptions are returned as expected.
     */
    @Test
    public void testInitializationError() {

        // Try to build a cache with a negative size
        try {
            new FIFOThreadSafeCache<Integer, Integer>(-1);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to use a supplier which generate a null entry
        final Supplier<CacheEntry<Integer, Integer>> nullSupplier = () -> {
            return null;
        };
        final FIFOThreadSafeCache<Integer, Integer> cache = new FIFOThreadSafeCache<>();

        try {
            cache.computeIfAbsent(1, nullSupplier);
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            cache.computeIf((cacheEntry) -> cacheEntry.getKey().equals(1), nullSupplier);
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * The number of {@link #unitaryRun(int, int, int, FIFOThreadSafeCache) unitary runs} that should be done in parallel.
     * 
     * @param nbKeys
     *        Number of computation with a different key
     * @param cacheRepeat
     *        Number of computation with a given key.
     * @param nbOps
     *        The number of operations done for an isolated computation
     * @param cache
     *        The cache to be tested
     * @param nbUnitaryRun
     *        The number of unitary runs. The total number of computation is nbKeys * cacheRepeat * nbUnitaryRun
     * @throws IllegalStateException
     *         if the value returned by the cache is not the one expected (the test fails)
     */
    private static void runParallel(final int nbKeys, final int cacheRepeat, final int nbOps,
                                    final FIFOThreadSafeCache<Integer, Integer> cache,
                                    final int nbUnitaryRun) {
        final IntStream intStream = IntStream.rangeClosed(1, nbUnitaryRun);
        intStream.parallel().forEach(i -> unitaryRun(nbKeys, cacheRepeat, nbOps, cache));
    }

    /**
     * Represents a run with an important amount of computations.<br>
     * The number of computation repeat can be parameterized to check the efficiency of the cache.
     * 
     * @param nbKeys
     *        Number of computation with a different key
     * @param cacheRepeat
     *        Number of computation with the same key
     * @param nbOps
     *        The number of operations done for an isolated computation
     * @param cache
     *        The cache
     * @throws IllegalStateException
     *         if the value returned by the cache is not the one expected (the test fails)
     */
    private static void unitaryRun(final int nbKeys, final int cacheRepeat, final int nbOps,
                                   final FIFOThreadSafeCache<Integer, Integer> cache) {
        for (int i = 0; i < nbKeys; i++) {
            for (int j = 0; j < cacheRepeat; j++) {
                final CacheEntry<Integer, Integer> entry = cache.computeIfAbsent(i, buildSupplier(cache, i, nbOps));
                if (entry.getValue() != i) {
                    System.out.println(cache);
                    throw new IllegalStateException("Wrong cache value: " + entry.getValue() + "(cache value), " + i + " (expected)");
                }
            }
        }
    }

    /**
     * Builds a supplier that emulates the necessary computation to build a cache value.<br>
     * The complexity of the supplier can be adjusted with the number of operations.
     * 
     * @param customCache
     *        The cache
     * @param i
     *        The integer that represents the key and the value
     * @param nbOps
     *        The number of operations necessary to emulate the computation of the value
     * @return the supplier for the entry of the cache
     */
    private static Supplier<CacheEntry<Integer, Integer>> buildSupplier(final FIFOThreadSafeCache<Integer, Integer> customCache,
                                                                        final int i, final int nbOps) {
        return () -> {
            @SuppressWarnings("unused")
            int sum = 0;
            for (long j = nbOps; j > 0; j--) {
                sum += j;
            }
            return new CacheEntry<>(i, i);
        };
    }
}
