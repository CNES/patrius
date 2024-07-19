/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3155:10/05/2022:[PATRIUS] Ajout d'une methode public contains (AbsoluteDate) a la classe AbsoluteDateInterval
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.utils.legs;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;

/*
 * This class verifies the behavior of the class {@link Leg}.
 */
public class LegTest {

    /**
     * <p>
     * Create different legs, intervals and dates to check whether a specific interval contains a specific date.
     * </p>
     * Asserts:
     * <ul>
     * <li>Check that the date is considered to be contained in the interval when the date is inside the interval</li>
     * <li>Check that the date is considered to be contained in the interval when the date is outside the interval</li>
     * <li>Check that the date is considered to be contained in the interval when the date coincides with the lower
     * interval point, whose type is closed</li>
     * <li>Check that the date is considered to be contained in the interval when the date coincides with the upper
     * interval point, whose type is closed</li>
     * <li>Check that the date is not considered to be contained in the interval when the date coincides with the lower
     * interval point, whose type is open</li>
     * <li>Check that the date is not considered to be contained in the interval when the date coincides with the upper
     * interval point, whose type is open</li>
     * </ul>
     * 
     * @testedMethod {@link Legs#contains(AbsoluteDate)}
     */
    @Test
    public void testContains() {
        // Check that the date is considered to be contained in the interval when the date is inside the interval
        final String lowerEndpoint = "2009-05-01T00:00:00.000";
        final String upperEndpoint = "2009-05-01T00:01:00.000";
        AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, new AbsoluteDate(
            lowerEndpoint, TimeScalesFactory.getTAI()), new AbsoluteDate(upperEndpoint, TimeScalesFactory.getTAI()),
            IntervalEndpointType.CLOSED);
        AbsoluteDate date = new AbsoluteDate("2009-05-01T00:00:30.000", TimeScalesFactory.getTAI());
        Leg leg = new LinearLeg(interval, 1.0, 1.0);
        Assert.assertTrue(leg.contains(date));
        // Check that the date is considered to be contained in the interval when the date is outside the interval
        date = new AbsoluteDate("2009-05-01T00:01:30.000", TimeScalesFactory.getTAI());
        Assert.assertFalse(leg.contains(date));
        // Check that the date is considered to be contained in the interval when the date coincides with the lower
        // interval point, whose type is closed
        date = new AbsoluteDate(lowerEndpoint, TimeScalesFactory.getTAI());
        Assert.assertTrue(leg.contains(date));
        // Check that the date is considered to be contained in the interval when the date coincides with the upper
        // interval point, whose type is closed
        date = new AbsoluteDate(upperEndpoint, TimeScalesFactory.getTAI());
        Assert.assertTrue(leg.contains(date));
        // Check that the date is not considered to be contained in the interval when the date coincides with the lower
        // interval point, whose type is open
        interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, new AbsoluteDate(lowerEndpoint,
            TimeScalesFactory.getTAI()), new AbsoluteDate(upperEndpoint, TimeScalesFactory.getTAI()),
            IntervalEndpointType.CLOSED);
        leg = new LinearLeg(interval, 1.0, 1.0);
        date = new AbsoluteDate(lowerEndpoint, TimeScalesFactory.getTAI());
        Assert.assertFalse(leg.contains(date));
        // Check that the date is not considered to be contained in the interval when the date coincides with the upper
        // interval point, whose type is open
        interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, new AbsoluteDate(lowerEndpoint,
            TimeScalesFactory.getTAI()), new AbsoluteDate(upperEndpoint, TimeScalesFactory.getTAI()),
            IntervalEndpointType.OPEN);
        leg = new LinearLeg(interval, 1.0, 1.0);
        date = new AbsoluteDate(upperEndpoint, TimeScalesFactory.getTAI());
        Assert.assertFalse(leg.contains(date));
    }

    /**
     * A simple linear a.x + b leg.
     */
    private class LinearLeg implements Leg {

        /** a. */
        private final double a;

        /** b. */
        private final double b;

        /** Time interval. */
        private final AbsoluteDateInterval timeInterval;

        /**
         * Constructor
         * 
         * @param timeInterval time interval of the profile
         * @param a of (slope of linear function)
         * @param b of (0-value of linear function)
         */
        public LinearLeg(final AbsoluteDateInterval timeInterval,
                         final double a, final double b) {
            this.a = a;
            this.b = b;
            this.timeInterval = timeInterval;
        }

        @Override
        public AbsoluteDate getDate() {
            return timeInterval.getLowerData();
        }

        @Override
        public AbsoluteDateInterval getTimeInterval() {
            return timeInterval;
        }

        @Override
        public LinearLeg copy(final AbsoluteDateInterval newInterval) {
            return new LinearLeg(newInterval, a, b
                    + (newInterval.getLowerData().durationFrom(getTimeInterval().getLowerData())) * a);
        }
    }
}
