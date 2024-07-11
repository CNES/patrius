/**
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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well1024a;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStampedCache;
import fr.cnes.sirius.patrius.time.TimeStampedGenerator;
import fr.cnes.sirius.patrius.utils.exception.TimeStampedCacheException;

public class TimeStampedCacheTest {

    @Test
    public void testSingleCall() throws TimeStampedCacheException {
        final TimeStampedCache<AbsoluteDate> cache = this.createCache(10, 3600.0, 13);
        final List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
        list.add(AbsoluteDate.GALILEO_EPOCH);
        Assert.assertEquals(1, this.checkDatesSingleThread(list, cache));
        Assert.assertEquals(4, cache.getGenerateCalls());
        Assert.assertEquals(0, cache.getSlotsEvictions());
        Assert.assertEquals(10, cache.getMaxSlots());
        Assert.assertEquals(Constants.JULIAN_DAY, cache.getNewSlotQuantumGap(), 1.0e-10);
        Assert.assertEquals(Constants.JULIAN_YEAR, cache.getMaxSpan(), 1.0e-10);
    }

    @Test
    public void testPastInfinityRange() throws TimeStampedCacheException {
        final TimeStampedCache<AbsoluteDate> cache =
            new TimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                new Generator(AbsoluteDate.PAST_INFINITY,
                    AbsoluteDate.J2000_EPOCH,
                    10.0), AbsoluteDate.class);
        final List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
        list.add(AbsoluteDate.GALILEO_EPOCH);
        list.add(AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        list.add(AbsoluteDate.JULIAN_EPOCH);
        Assert.assertEquals(3, this.checkDatesSingleThread(list, cache));
        try {
            cache.getNeighbors(AbsoluteDate.J2000_EPOCH.shiftedBy(100.0));
            Assert.fail("expected TimeStampedCacheException");
        } catch (final TimeStampedCacheException tce) {
            // expected behavior
        } catch (final Exception e) {
            Assert.fail("wrong exception caught");
        }
    }

    @Test
    public void testFutureInfinityRange() throws TimeStampedCacheException {
        final TimeStampedCache<AbsoluteDate> cache =
            new TimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                new Generator(AbsoluteDate.MODIFIED_JULIAN_EPOCH,
                    AbsoluteDate.FUTURE_INFINITY, 10.0),
                AbsoluteDate.class);
        final List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
        list.add(AbsoluteDate.J2000_EPOCH);
        list.add(AbsoluteDate.GALILEO_EPOCH);
        Assert.assertEquals(2, this.checkDatesSingleThread(list, cache));
        try {
            cache.getNeighbors(AbsoluteDate.JULIAN_EPOCH);
            Assert.fail("expected TimeStampedCacheException");
        } catch (final TimeStampedCacheException tce) {
            // expected behavior
        } catch (final Exception e) {
            Assert.fail("wrong exception caught");
        }
    }

    @Test
    public void testInfinityRange() throws TimeStampedCacheException {
        final TimeStampedCache<AbsoluteDate> cache =
            new TimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                new Generator(AbsoluteDate.PAST_INFINITY,
                    AbsoluteDate.FUTURE_INFINITY,
                    10.0), AbsoluteDate.class);
        final List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
        list.add(AbsoluteDate.J2000_EPOCH.shiftedBy(+4.6e12));
        list.add(AbsoluteDate.J2000_EPOCH.shiftedBy(-4.6e12));
        list.add(AbsoluteDate.JULIAN_EPOCH);
        list.add(AbsoluteDate.J2000_EPOCH);
        list.add(AbsoluteDate.GALILEO_EPOCH);
        Assert.assertEquals(5, this.checkDatesSingleThread(list, cache));
    }

    @Test
    public void testRegularCalls() throws TimeStampedCacheException {
        final TimeStampedCache<AbsoluteDate> cache = this.createCache(2, 3600, 13);
        Assert.assertEquals(2000, this.testMultipleSingleThread(cache, new SequentialMode(), 2));
        Assert.assertEquals(56, cache.getGenerateCalls());
        Assert.assertEquals(0, cache.getSlotsEvictions());
    }

    @Test
    public void testAlternateCallsGoodConfiguration() throws TimeStampedCacheException {
        final TimeStampedCache<AbsoluteDate> cache = this.createCache(2, 3600, 13);
        Assert.assertEquals(2000, this.testMultipleSingleThread(cache, new AlternateMode(), 2));
        Assert.assertEquals(56, cache.getGenerateCalls());
        Assert.assertEquals(0, cache.getSlotsEvictions());
    }

    @Test
    public void testAlternateCallsBadConfiguration() throws TimeStampedCacheException {
        final TimeStampedCache<AbsoluteDate> cache = this.createCache(1, 3600, 13);
        Assert.assertEquals(2000, this.testMultipleSingleThread(cache, new AlternateMode(), 2));
        Assert.assertEquals(8000, cache.getGenerateCalls());
        Assert.assertEquals(1999, cache.getSlotsEvictions());
    }

    @Test
    public void testRandomCallsGoodConfiguration() throws TimeStampedCacheException {
        final TimeStampedCache<AbsoluteDate> cache = this.createCache(30, 3600, 13);
        Assert.assertEquals(5000, this.testMultipleSingleThread(cache, new RandomMode(64394632125212l), 5));
        Assert.assertTrue(cache.getGenerateCalls() < 250);
        Assert.assertEquals(0, cache.getSlotsEvictions());
    }

    @Test
    public void testRandomCallsBadConfiguration() throws TimeStampedCacheException {
        final TimeStampedCache<AbsoluteDate> cache = this.createCache(3, 3600, 13);
        Assert.assertEquals(5000, this.testMultipleSingleThread(cache, new RandomMode(64394632125212l), 5));
        Assert.assertTrue(cache.getGenerateCalls() > 400);
        Assert.assertTrue(cache.getSlotsEvictions() > 300);
    }

    @Test
    public void testMultithreadedGoodConfiguration() throws TimeStampedCacheException {
        final TimeStampedCache<AbsoluteDate> cache = this.createCache(50, 3600, 13);
        final int n = this.testMultipleMultiThread(cache, new AlternateMode(), 50, 30);
        Assert.assertTrue("this test may fail randomly due to multi-threading non-determinism" +
            " (n = " + n + ", calls = " + cache.getGenerateCalls() +
            ", ratio = " + (n / cache.getGenerateCalls()) + ")",
            cache.getGenerateCalls() < n / 20);
        Assert.assertTrue("this test may fail randomly due to multi-threading non-determinism" +
            " (n = " + n + ", evictions = " + cache.getSlotsEvictions() +
            (cache.getSlotsEvictions() == 0 ? "" : (", ratio = " + (n / cache.getSlotsEvictions()))) + ")",
            cache.getSlotsEvictions() < n / 1000);
    }

    @Test
    public void testMultithreadedBadConfiguration() throws TimeStampedCacheException {
        final TimeStampedCache<AbsoluteDate> cache = this.createCache(3, 3600, 13);
        final int n = this.testMultipleMultiThread(cache, new AlternateMode(), 50, 100);
        Assert.assertTrue("this test may fail randomly due to multi-threading non-determinism" +
            " (n = " + n + ", calls = " + cache.getGenerateCalls() +
            ", ratio = " + (n / cache.getGenerateCalls()) + ")",
            cache.getGenerateCalls() > n / 15);
        Assert.assertTrue("this test may fail randomly due to multi-threading non-determinism" +
            " (n = " + n + ", evictions = " + cache.getSlotsEvictions() +
            ", ratio = " + (n / cache.getSlotsEvictions()) + ")",
            cache.getSlotsEvictions() > n / 60);
    }

    @Test
    public void testSmallShift() throws TimeStampedCacheException {
        final double hour = 3600;
        final TimeStampedCache<AbsoluteDate> cache = this.createCache(10, hour, 13);
        Assert.assertEquals(0, cache.getSlots());
        Assert.assertEquals(0, cache.getEntries());
        final AbsoluteDate start = AbsoluteDate.GALILEO_EPOCH;
        cache.getNeighbors(start);
        Assert.assertEquals(1, cache.getSlots());
        Assert.assertEquals(18, cache.getEntries());
        Assert.assertEquals(4, cache.getGenerateCalls());
        Assert.assertEquals(-11 * hour, cache.getEarliest().durationFrom(start), 1.0e-10);
        Assert.assertEquals(+6 * hour, cache.getLatest().durationFrom(start), 1.0e-10);
        cache.getNeighbors(start.shiftedBy(-3 * 3600));
        Assert.assertEquals(1, cache.getSlots());
        Assert.assertEquals(18, cache.getEntries());
        Assert.assertEquals(4, cache.getGenerateCalls());
        Assert.assertEquals(-11 * hour, cache.getEarliest().durationFrom(start), 1.0e-10);
        Assert.assertEquals(+6 * hour, cache.getLatest().durationFrom(start), 1.0e-10);
        cache.getNeighbors(start.shiftedBy(7 * 3600));
        Assert.assertEquals(1, cache.getSlots());
        Assert.assertEquals(25, cache.getEntries());
        Assert.assertEquals(5, cache.getGenerateCalls());
        Assert.assertEquals(-11 * hour, cache.getEarliest().durationFrom(start), 1.0e-10);
        Assert.assertEquals(+13 * hour, cache.getLatest().durationFrom(start), 1.0e-10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEnoughSlots() {
        this.createCache(0, 3600.0, 13);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEnoughNeighbors() {
        this.createCache(10, 3600.0, 1);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoEarliestEntry() {
        this.createCache(10, 3600.0, 3).getEarliest();
    }

    @Test(expected = IllegalStateException.class)
    public void testNoLatestEntry() {
        this.createCache(10, 3600.0, 3).getLatest();
    }

    @Test(expected = TimeStampedCacheException.class)
    public void testNoGeneratedData() throws TimeStampedCacheException {
        final TimeStampedGenerator<AbsoluteDate> nullGenerator =
            new TimeStampedGenerator<AbsoluteDate>(){
                @Override
                public List<AbsoluteDate> generate(final AbsoluteDate existing,
                                                   final AbsoluteDate date) {
                    return new ArrayList<AbsoluteDate>();
                }
            };
        new TimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
            nullGenerator, AbsoluteDate.class).getNeighbors(AbsoluteDate.J2000_EPOCH);
    }

    @Test(expected = TimeStampedCacheException.class)
    public void testNoDataBefore() throws TimeStampedCacheException {
        final TimeStampedGenerator<AbsoluteDate> nullGenerator =
            new TimeStampedGenerator<AbsoluteDate>(){
                @Override
                public List<AbsoluteDate> generate(final AbsoluteDate existing,
                                                   final AbsoluteDate date) {
                    return Arrays.asList(AbsoluteDate.J2000_EPOCH);
                }
            };
        new TimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
            nullGenerator, AbsoluteDate.class).getNeighbors(AbsoluteDate.J2000_EPOCH.shiftedBy(-10));
    }

    @Test(expected = TimeStampedCacheException.class)
    public void testNoDataAfter() throws TimeStampedCacheException {
        final TimeStampedGenerator<AbsoluteDate> nullGenerator =
            new TimeStampedGenerator<AbsoluteDate>(){
                @Override
                public List<AbsoluteDate> generate(final AbsoluteDate existing,
                                                   final AbsoluteDate date) {
                    return Arrays.asList(AbsoluteDate.J2000_EPOCH);
                }
            };
        new TimeStampedCache<AbsoluteDate>(2, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
            nullGenerator, AbsoluteDate.class).getNeighbors(AbsoluteDate.J2000_EPOCH.shiftedBy(+10));
    }

    @Test(expected = TimeStampedCacheException.class)
    public void testUnsortedEntries() throws TimeStampedCacheException {
        final TimeStampedGenerator<AbsoluteDate> reversedGenerator =
            new TimeStampedGenerator<AbsoluteDate>(){
                /** {@inheritDoc} */
                @Override
                public List<AbsoluteDate> generate(final AbsoluteDate existing, final AbsoluteDate date) {
                    final List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
                    list.add(date);
                    list.add(date.shiftedBy(-10.0));
                    return list;
                }
            };

        new TimeStampedCache<AbsoluteDate>(3, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
            reversedGenerator, AbsoluteDate.class).getNeighbors(AbsoluteDate.J2000_EPOCH);

    }

    @Test
    public void testDuplicatingGenerator() throws TimeStampedCacheException {

        final double step = 3600.0;

        final TimeStampedGenerator<AbsoluteDate> duplicatingGenerator =
            new TimeStampedGenerator<AbsoluteDate>(){

                /** {@inheritDoc} */
                @Override
                public List<AbsoluteDate> generate(final AbsoluteDate existing, final AbsoluteDate date) {
                    final List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
                    if (existing == null) {
                        list.add(date);
                    } else {
                        if (date.compareTo(existing) > 0) {
                            AbsoluteDate t = existing.shiftedBy(-10 * step);
                            do {
                                t = t.shiftedBy(step);
                                list.add(list.size(), t);
                            } while (t.compareTo(date) <= 0);
                        } else {
                            AbsoluteDate t = existing.shiftedBy(10 * step);
                            do {
                                t = t.shiftedBy(-step);
                                list.add(0, t);
                            } while (t.compareTo(date) >= 0);
                        }
                    }
                    return list;
                }

            };

        final TimeStampedCache<AbsoluteDate> cache =
            new TimeStampedCache<AbsoluteDate>(5, 10, Constants.JULIAN_YEAR, Constants.JULIAN_DAY,
                duplicatingGenerator, AbsoluteDate.class);

        final AbsoluteDate start = AbsoluteDate.GALILEO_EPOCH;
        final AbsoluteDate[] firstSet = cache.getNeighbors(start);
        Assert.assertEquals(5, firstSet.length);
        Assert.assertEquals(4, cache.getGenerateCalls());
        Assert.assertEquals(8, cache.getEntries());
        for (int i = 1; i < firstSet.length; ++i) {
            Assert.assertEquals(step, firstSet[i].durationFrom(firstSet[i - 1]), 1.0e-10);
        }

        final AbsoluteDate[] secondSet = cache.getNeighbors(cache.getLatest().shiftedBy(10 * step));
        Assert.assertEquals(5, secondSet.length);
        Assert.assertEquals(7, cache.getGenerateCalls());
        Assert.assertEquals(20, cache.getEntries());
        for (int i = 1; i < secondSet.length; ++i) {
            Assert.assertEquals(step, firstSet[i].durationFrom(firstSet[i - 1]), 1.0e-10);
        }

    }

    private
            int
            testMultipleSingleThread(final TimeStampedCache<AbsoluteDate> cache, final Mode mode, final int slots)
                                                                                                                  throws TimeStampedCacheException {
        final double step = ((Generator) cache.getGenerator()).getStep();
        final AbsoluteDate[] base = new AbsoluteDate[slots];
        base[0] = AbsoluteDate.GALILEO_EPOCH;
        for (int i = 1; i < base.length; ++i) {
            base[i] = base[i - 1].shiftedBy(10 * Constants.JULIAN_DAY);
        }
        return this.checkDatesSingleThread(mode.generateDates(base, 25 * step, 0.025 * step), cache);
    }

    private int testMultipleMultiThread(final TimeStampedCache<AbsoluteDate> cache, final Mode mode,
                                        final int slots, final int threadPoolSize)
                                                                                  throws TimeStampedCacheException {
        final double step = ((Generator) cache.getGenerator()).getStep();
        final AbsoluteDate[] base = new AbsoluteDate[slots];
        base[0] = AbsoluteDate.GALILEO_EPOCH;
        for (int i = 1; i < base.length; ++i) {
            base[i] = base[i - 1].shiftedBy(10 * Constants.JULIAN_DAY);
        }
        return this.checkDatesMultiThread(mode.generateDates(base, 25 * step, 0.025 * step), cache, threadPoolSize);
    }

    private TimeStampedCache<AbsoluteDate> createCache(final int maxSlots, final double step, final int neighborsSize) {
        final Generator generator =
            new Generator(AbsoluteDate.J2000_EPOCH.shiftedBy(-Constants.JULIAN_CENTURY),
                AbsoluteDate.J2000_EPOCH.shiftedBy(+Constants.JULIAN_CENTURY),
                step);
        return new TimeStampedCache<AbsoluteDate>(neighborsSize, maxSlots, Constants.JULIAN_YEAR,
            Constants.JULIAN_DAY, generator, AbsoluteDate.class);
    }

    private int checkDatesSingleThread(final List<AbsoluteDate> centralDates,
                                       final TimeStampedCache<AbsoluteDate> cache)
                                                                                  throws TimeStampedCacheException {

        final int n = cache.getNeighborsSize();
        final double step = ((Generator) cache.getGenerator()).getStep();

        for (final AbsoluteDate central : centralDates) {
            final AbsoluteDate[] neighbors = cache.getNeighbors(central);
            Assert.assertEquals(n, neighbors.length);
            for (final AbsoluteDate date : neighbors) {
                Assert.assertTrue(date.durationFrom(central) >= -(n + 1) * step);
                Assert.assertTrue(date.durationFrom(central) <= n * step);
            }
        }

        return centralDates.size();

    }

    private int checkDatesMultiThread(final List<AbsoluteDate> centralDates,
                                      final TimeStampedCache<AbsoluteDate> cache,
                                      final int threadPoolSize)
                                                               throws TimeStampedCacheException {

        final int n = cache.getNeighborsSize();
        final double step = ((Generator) cache.getGenerator()).getStep();
        final AtomicReference<AbsoluteDate[]> failedDates = new AtomicReference<AbsoluteDate[]>();
        final AtomicReference<TimeStampedCacheException> caught = new AtomicReference<TimeStampedCacheException>();
        final ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        for (final AbsoluteDate central : centralDates) {
            executorService.execute(new Runnable(){
                @Override
                public void run() {
                    try {
                        final AbsoluteDate[] neighbors = cache.getNeighbors(central);
                        Assert.assertEquals(n, neighbors.length);
                        for (final AbsoluteDate date : neighbors) {
                            if (date.durationFrom(central) < -(n + 1) * step ||
                                date.durationFrom(central) > n * step) {
                                final AbsoluteDate[] dates = new AbsoluteDate[n + 1];
                                dates[0] = central;
                                System.arraycopy(neighbors, 0, dates, 1, n);
                                failedDates.set(dates);
                            }
                        }
                    } catch (final TimeStampedCacheException tce) {
                        caught.set(tce);
                    }
                }
            });
        }

        try {
            executorService.shutdown();
            executorService.awaitTermination(3, TimeUnit.SECONDS);
        } catch (final InterruptedException ie) {
            Assert.fail(ie.getLocalizedMessage());
        }

        if (caught.get() != null) {
            throw caught.get();
        }

        if (failedDates.get() != null) {
            final AbsoluteDate[] dates = failedDates.get();
            final StringBuilder builder = new StringBuilder();
            final String eol = System.getProperty("line.separator");
            builder.append("central = ").append(dates[0]).append(eol);
            builder.append("step = ").append(step).append(eol);
            builder.append("neighbors =").append(eol);
            for (int i = 1; i < dates.length; ++i) {
                builder.append("    ").append(dates[i]).append(eol);
            }
            Assert.fail(builder.toString());
        }

        return centralDates.size();

    }

    private static class Generator implements TimeStampedGenerator<AbsoluteDate> {

        private final AbsoluteDate earliest;
        private final AbsoluteDate latest;
        private final double step;

        public Generator(final AbsoluteDate earliest, final AbsoluteDate latest, final double step) {
            this.earliest = earliest;
            this.latest = latest;
            this.step = step;
        }

        public double getStep() {
            return this.step;
        }

        @Override
        public List<AbsoluteDate> generate(final AbsoluteDate existing, final AbsoluteDate date) {
            final List<AbsoluteDate> dates = new ArrayList<AbsoluteDate>();
            if (existing == null) {
                dates.add(date);
            } else if (date.compareTo(existing) >= 0) {
                AbsoluteDate previous = existing;
                while (date.compareTo(previous) > 0) {
                    previous = previous.shiftedBy(this.step);
                    if (previous.compareTo(this.earliest) >= 0 && previous.compareTo(this.latest) <= 0) {
                        dates.add(dates.size(), previous);
                    }
                }
            } else {
                AbsoluteDate previous = existing;
                while (date.compareTo(previous) < 0) {
                    previous = previous.shiftedBy(-this.step);
                    if (previous.compareTo(this.earliest) >= 0 && previous.compareTo(this.latest) <= 0) {
                        dates.add(0, previous);
                    }
                }
            }
            return dates;
        }

    }

    private interface Mode {
        public List<AbsoluteDate> generateDates(AbsoluteDate[] base, double duration, double step);
    }

    private class SequentialMode implements Mode {

        @Override
        public List<AbsoluteDate> generateDates(final AbsoluteDate[] base, final double duration, final double step) {
            final List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
            for (final AbsoluteDate initial : base) {
                for (double dt = 0; dt < duration; dt += step) {
                    list.add(initial.shiftedBy(dt));
                }
            }
            return list;
        }

    }

    private class AlternateMode implements Mode {

        @Override
        public List<AbsoluteDate> generateDates(final AbsoluteDate[] base, final double duration, final double step) {
            final List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
            for (double dt = 0; dt < duration; dt += step) {
                for (final AbsoluteDate initial : base) {
                    list.add(initial.shiftedBy(dt));
                }
            }
            return list;
        }

    }

    private class RandomMode implements Mode {

        private final RandomGenerator random;

        public RandomMode(final long seed) {
            this.random = new Well1024a(seed);
        }

        @Override
        public List<AbsoluteDate> generateDates(final AbsoluteDate[] base, final double duration, final double step) {
            final List<AbsoluteDate> list = new ArrayList<AbsoluteDate>();
            for (int i = 0; i < base.length * duration / step; ++i) {
                final int j = this.random.nextInt(base.length);
                final double dt = this.random.nextDouble() * duration;
                list.add(base[j].shiftedBy(dt));
            }
            return list;
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
