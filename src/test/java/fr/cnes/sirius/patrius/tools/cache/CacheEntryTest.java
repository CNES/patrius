/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.tools.cache;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;

/**
 * Unit test class for the {@link CacheEntry} class.
 * 
 * @author bonitt
 */
public class CacheEntryTest {

    /**
     * @description Builds a new instance and tests the basic getters.
     *
     * @testedMethod {@link CacheEntry#CacheEntry(Object, Object)}
     * @testedMethod {@link CacheEntry#getKey()}
     * @testedMethod {@link CacheEntry#getValue()}
     *
     * @testPassCriteria The instance is build without error and the basic getters return the expected data.
     */
    @Test
    public void testConstructor() {
        final CacheEntry<Integer, Integer> cacheEntry = new CacheEntry<>(1, 2);
        Assert.assertTrue(cacheEntry.getKey().equals(1));
        Assert.assertTrue(cacheEntry.getValue().equals(2));
    }

    /**
     * @description Check the String representation method behavior.
     *
     * @testedMethod {@link CacheEntry#toString()}
     *
     * @testPassCriteria The cache entry String representation contains the expected information.
     */
    @Test
    public void testToString() {
        final CacheEntry<Integer, Integer> cacheEntry = new CacheEntry<>(1, 2);
        final String expectedText = "1 : 2";
        Assert.assertEquals(expectedText, cacheEntry.toString());
    }

    /**
     * @description Try to build an angular actors container with initialization errors.
     *
     * @testedMethod {@link CacheEntry#CacheEntry(Object, Object)}
     *
     * @testPassCriteria The exceptions are returned as expected.
     */
    @Test
    public void testInitializationError() {
        // Try to build a cache entry with a null input
        try {
            new CacheEntry<Integer, Integer>(null, 1);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new CacheEntry<Integer, Integer>(1, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }
}
