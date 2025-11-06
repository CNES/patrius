package fr.cnes.sirius.patrius.propagation.events;

/** HISTORY
 * VERSION:4.16:OPENFD-468:25/04/2025:[PATRIUS] Renommer toutes les mentions du GeodeticPoint
 * VERSION:4.16:OPENFD-442:25/04/2025:[PATRIUS] Calcul des eclipses d'un corps celeste
 * END-HISTORY
 */
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.EventDatationType;
import fr.cnes.sirius.patrius.events.detectors.BodyInEclipseDetector;
import fr.cnes.sirius.patrius.events.detectors.BodyInEclipseDetector.BodyInEclipseModelEnum;
import fr.cnes.sirius.patrius.events.postprocessing.EventsLogger;
import fr.cnes.sirius.patrius.events.postprocessing.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.EquatorialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class BodyInEclipseDetectorTest {

    private final double sunRadius = 696342000.;
    private final double earthRadius = 6378000.;
    private final double moonRadius = 1737000.;
    private List<BodyInEclipseDetector> detectors;

    private static TimeScale UTC;

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        UTC = TimeScalesFactory.getUTC();
        this.detectors = new ArrayList<>();
    }

    /**
     * @throws PatriusException
     *
     * @testType UT
     * @description This tests verifies that the constructors in BodyInEclipseDetector
     *              correctly initialize the objects
     *
     * @input input parameters for the object BodyInEclipseDetector (not relevant)
     *
     * @output the BodyInEclipseDetector object
     * @testPassCriteria the object is correctly constructed
     *
     * @referenceVersion 4.16
     * @nonregressionVersion 4.16
     */
    @Test
    public void testConstructors() throws PatriusException {

        // Create detector object using first constructor

        final boolean totalEclipse = true;
        final boolean bodyFullyIn = true;
        final int slopeSelection = 0;

        BodyInEclipseDetector detector = new BodyInEclipseDetector(
            CelestialBodyFactory.getMoon(), this.moonRadius,
            CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth().getShape(),
            totalEclipse, bodyFullyIn, BodyInEclipseModelEnum.EXACT_MODEL,
            slopeSelection, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD);

        // Check that the constructor did not crash... so the object was constructed correctly
        Assert.assertNotNull(detector);

        // Create detector object with second constructor
        detector = new BodyInEclipseDetector(
            CelestialBodyFactory.getMoon(), this.moonRadius,
            CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius,
            totalEclipse, bodyFullyIn, BodyInEclipseModelEnum.EXACT_MODEL,
            slopeSelection, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD);

        // The constructor did not crash... so the object was constructed correctly
        Assert.assertNotNull(detector);

    }

    /**
     * @throws PatriusException
     *
     * @testType UT
     * @description This tests verifies that the getters in BodyInEclipseDetector
     *              correctly return the values
     *
     * @input input parameters to create an object BodyInEclipseDetector
     *        (dummies, not important)
     *
     * @output the values from the different getters
     * @testPassCriteria the returned values match the input ones
     *
     * @referenceVersion 4.16
     * @nonregressionVersion 4.16
     */
    @Test
    public void testGetters() throws PatriusException {

        // Create detector object
        final boolean totalEclipse = true;
        final boolean bodyFullyIn = true;
        final int slopeSelection = 0;

        final BodyInEclipseDetector detector = new BodyInEclipseDetector(
            CelestialBodyFactory.getMoon(), this.moonRadius,
            CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth().getShape(),
            totalEclipse, bodyFullyIn, BodyInEclipseModelEnum.EXACT_MODEL,
            slopeSelection, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
            Action.CONTINUE, Action.CONTINUE, false, false);

        // Test getters
        Assert.assertEquals(detector.isTotalEclipse(), totalEclipse);

    }

    /**
     * @throws PatriusException
     *
     * @testType UT
     * @description This tests verifies that the copy method in
     *              BodyInEclipseDetector correctly creates a copy of the
     *              input detector
     *
     * @input input parameters to create an object BodyInEclipseDetector
     *        (dummies, not important)
     *
     * @output the values from the different getters
     * @testPassCriteria the copy matches the original detector. A modification to the original does not impact the copy
     *
     * @referenceVersion 4.16
     * @nonregressionVersion 4.16
     */
    @Test
    public void testCopy() throws PatriusException {

        // Create detector object
        final boolean totalEclipse = true;
        final boolean bodyFullyIn = true;
        final int slopeSelection = 0;

        final BodyInEclipseDetector detectorVariable = new BodyInEclipseDetector(
            CelestialBodyFactory.getMoon(), this.moonRadius,
            CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth().getShape(),
            totalEclipse, bodyFullyIn, BodyInEclipseModelEnum.EXACT_MODEL,
            slopeSelection, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
            Action.CONTINUE, Action.CONTINUE, false, false);

        final BodyInEclipseDetector detectorVariableCopy = detectorVariable.copy();

        // Create detector object with second constructor
        final BodyInEclipseDetector detectorConstant = new BodyInEclipseDetector(
            CelestialBodyFactory.getMoon(), this.moonRadius,
            CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius,
            totalEclipse, bodyFullyIn, BodyInEclipseModelEnum.EXACT_MODEL,
            slopeSelection, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
            Action.CONTINUE, Action.CONTINUE, false, false);

        final BodyInEclipseDetector detectorConstantCopy = detectorConstant.copy();

        // Do verifications
        verifyCopy(detectorVariable, detectorVariableCopy);
        verifyCopy(detectorConstant, detectorConstantCopy);

    }

    /**
     * @throws PatriusException
     *
     * @testType UT
     * @description This test compute the times of the 2025-09-07 Moon eclipse. The target is the Earth, the occulted
     *              body is the SUN and the occulting body is the MOON. Since we know that on the 2025-09-07 the moon is
     *              in Earth's shadow, approx 14 days later (1/2 moon's period around the Earth) the MOON should be
     *              between the sun and the Earth.
     *
     *              This tests uses this fact to compute the events when the target body is much bigger than the
     *              occulting body (EARTH >> MOON). Since for this case, when computing the interest point
     *              (BodyInEclipseDetector.getInterestPoint()), we return the intersection point
     *              because the nominal algorithm fails.
     *
     * @input input parameters to create an object BodyInEclipseDetector and the specific dates and propagation
     *        configuration to replicate the moon eclipse
     *
     * @output the eclipse events
     * @testPassCriteria 2 eclipse events are recorded and they match the reference
     *
     * @referenceVersion 4.16
     * @nonregressionVersion 4.16
     */
    @Test
    public void testEarthInMoonEclipse() throws PatriusException {

        final List<AbsoluteDate> eventDates =
                runTestInEclipse(BodyInEclipseModelEnum.EXACT_MODEL, CelestialBodyFactory.getEarth(), this.earthRadius,
                    CelestialBodyFactory.getSun(), this.sunRadius,
                    CelestialBodyFactory.getMoon(), this.moonRadius);

        // Reference results
        final AbsoluteDate refDate1 = new AbsoluteDate("2025-09-21T17:30:27.8261810964904726", UTC);
        final AbsoluteDate refDate2 = new AbsoluteDate("2025-09-21T21:54:53.7602609391324222", UTC);

        // This threshold of Precision.DOUBLE_COMPARISON_EPSILON allows to create the non regression test :

        // Compare with reference (note: no reference available for events index 2 and 5 "Moon fully in penumbra")
        Assert.assertEquals(0., eventDates.get(0).durationFrom(refDate1), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., eventDates.get(1).durationFrom(refDate2), Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @throws PatriusException
     *
     * @testType UT
     * @description This test compute the times of the 2025-09-07 Moon eclipse, and compare results with a reference
     *              (IMCCE). The reference values have been modified to the results from the calculations (after having
     *              verified the correctness of the results) so that the test can be used as a non regression test.
     *
     *              This tests uses the EXACT_MODEL to calculate the events.
     *
     * @input input parameters to create an object BodyInEclipseDetector and the specific dates and propagation
     *        configuration to replicate the moon eclipse
     *
     *
     * @output the dates of the different events
     * @testPassCriteria the dates of the events match the reference
     *
     * @referenceVersion 4.16
     * @nonregressionVersion 4.16
     */
    @Test
    public void testMoonInEclipse() throws PatriusException {

        final List<AbsoluteDate> eventDates =
                runTestInEclipse(BodyInEclipseModelEnum.EXACT_MODEL, CelestialBodyFactory.getMoon(), this.moonRadius,
                    CelestialBodyFactory.getSun(), this.sunRadius, CelestialBodyFactory.getEarth(), this.earthRadius);

        // Reference results, obtained on website https://ssp.imcce.fr/forms/lunar-eclipses/2025-09-07
        // Date (UTC)
        // Entrée dans la pénombre (P1) 2025-09-07T15:28:25
        // Entrée dans l’ombre (O1) 2025-09-07T16:27:06
        // Début de la totalité (T1) 2025-09-07T17:30:45
        // Maximum de l’éclipse (M) 2025-09-07T18:11:49
        // Fin de la totalité (T2) 2025-09-07T18:52:54
        // Sortie de l’ombre (O2) 2025-09-07T19:56:34
        // Sortie de la pénombre (P2) 2025-09-07T20:55:08
        // The references are modified to the actual values to create a non regression test (no tolerances)
        final AbsoluteDate p1Ref = new AbsoluteDate("2025-09-07T15:30:06.8430658612487605", UTC);
        final AbsoluteDate o1Ref = new AbsoluteDate("2025-09-07T16:28:51.982797959310119", UTC);
        final AbsoluteDate t1Ref = new AbsoluteDate("2025-09-07T17:32:40.8787113809084985", UTC);
        final AbsoluteDate t2Ref = new AbsoluteDate("2025-09-07T18:52:12.6531864894204773", UTC);
        final AbsoluteDate o2Ref = new AbsoluteDate("2025-09-07T19:56:02.3099898574582767", UTC);
        final AbsoluteDate p2Ref = new AbsoluteDate("2025-09-07T20:54:40.548970132964314", UTC);

        // This threshold of Precision.DOUBLE_COMPARISON_EPSILON allows to create the non regression test :

        // Compare with reference (note: no reference available for events index 2 and 5 "Moon fully in penumbra")
        Assert.assertEquals(0., eventDates.get(0).durationFrom(p1Ref), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., eventDates.get(1).durationFrom(o1Ref), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., eventDates.get(3).durationFrom(t1Ref), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., eventDates.get(4).durationFrom(t2Ref), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., eventDates.get(6).durationFrom(o2Ref), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., eventDates.get(7).durationFrom(p2Ref), Precision.DOUBLE_COMPARISON_EPSILON);

        // Verify the isInEclipse method
        final boolean[] bodyFullInArray = {true, true, false, false, true, true, false, false}; // configuration of detectors
        int i = 0;
        for (final BodyInEclipseDetector detector : this.detectors) {

            final boolean totalEclipse = detector.isTotalEclipse();
            final boolean bodyFullIn = bodyFullInArray[i];

            if (totalEclipse && bodyFullIn) {
                Assert.assertFalse(detector.isInEclipse(p1Ref));
                Assert.assertFalse(detector.isInEclipse(o1Ref));
                Assert.assertTrue(detector.isInEclipse(t1Ref));
                Assert.assertFalse(detector.isInEclipse(t2Ref));
                Assert.assertFalse(detector.isInEclipse(o2Ref));
                Assert.assertFalse(detector.isInEclipse(p2Ref));
            } else if (totalEclipse && !bodyFullIn) {
                Assert.assertFalse(detector.isInEclipse(p1Ref));
                Assert.assertTrue(detector.isInEclipse(o1Ref));
                Assert.assertTrue(detector.isInEclipse(t1Ref));
                Assert.assertTrue(detector.isInEclipse(t2Ref));
                Assert.assertFalse(detector.isInEclipse(o2Ref));
                Assert.assertFalse(detector.isInEclipse(p2Ref));
            } else if (!totalEclipse && bodyFullIn) {
                Assert.assertFalse(detector.isInEclipse(p1Ref));
                Assert.assertFalse(detector.isInEclipse(o1Ref));
                Assert.assertTrue(detector.isInEclipse(t1Ref));
                Assert.assertTrue(detector.isInEclipse(t2Ref));
                Assert.assertFalse(detector.isInEclipse(o2Ref));
                Assert.assertFalse(detector.isInEclipse(p2Ref));
            }else {
                Assert.assertTrue(detector.isInEclipse(p1Ref));
                Assert.assertTrue(detector.isInEclipse(o1Ref));
                Assert.assertTrue(detector.isInEclipse(t1Ref));
                Assert.assertTrue(detector.isInEclipse(t2Ref));
                Assert.assertTrue(detector.isInEclipse(o2Ref));
                Assert.assertFalse(detector.isInEclipse(p2Ref));
            }
            i++;
        }
    }

    /**
     * @throws PatriusException
     *
     * @testType UT
     * @description This test compute the times of the 2025-09-07 Moon eclipse, and compare results with a reference
     *              (IMCCE). The reference values have been modified to the results from the calculations (after having
     *              verified the correctness of the results) so that the test can be used as a non regression test.
     *
     *              This tests uses the APPROX_MODEL to calculate the events.
     *
     * @input input parameters to create an object BodyInEclipseDetector and the specific dates and propagation
     *        configuration to replicate the moon eclipse
     *
     *
     * @output the dates of the different events
     * @testPassCriteria the dates of the events match the reference
     *
     * @referenceVersion 4.16
     * @nonregressionVersion 4.16
     */
    @Test
    public void testMoonInEclipseApprox() throws PatriusException {

        final List<AbsoluteDate> eventDates =
                runTestInEclipse(BodyInEclipseModelEnum.APPROX_MODEL, CelestialBodyFactory.getMoon(), this.moonRadius,
                    CelestialBodyFactory.getSun(), this.sunRadius, CelestialBodyFactory.getEarth(), this.earthRadius);

        // Reference results, obtained on website https://ssp.imcce.fr/forms/lunar-eclipses/2025-09-07
        // Date (UTC)
        // Entrée dans la pénombre (P1) 2025-09-07T15:28:25
        // Entrée dans l’ombre (O1) 2025-09-07T16:27:06
        // Début de la totalité (T1) 2025-09-07T17:30:45
        // Maximum de l’éclipse (M) 2025-09-07T18:11:49
        // Fin de la totalité (T2) 2025-09-07T18:52:54
        // Sortie de l’ombre (O2) 2025-09-07T19:56:34
        // Sortie de la pénombre (P2) 2025-09-07T20:55:08
        // The references are modified to the actual values to create a non regression test (no tolerances)
        final AbsoluteDate p1Ref = new AbsoluteDate("2025-09-07T15:30:06.8619266382956994", UTC);
        final AbsoluteDate o1Ref = new AbsoluteDate("2025-09-07T16:28:52.0020808889748877846", UTC);
        final AbsoluteDate t1Ref = new AbsoluteDate("2025-09-07T17:32:40.8556542309306678", UTC);
        final AbsoluteDate t2Ref = new AbsoluteDate("2025-09-07T18:52:12.6762491546105593", UTC);
        final AbsoluteDate o2Ref = new AbsoluteDate("2025-09-07T19:56:02.29070237481209915", UTC);
        final AbsoluteDate p2Ref = new AbsoluteDate("2025-09-07T20:54:40.53011927154148", UTC);

        // This threshold of Precision.DOUBLE_COMPARISON_EPSILON allows to create the non regression test :

        // Compare with reference (note: no reference available for events index 2 and 5 "Moon fully in penumbra")
        Assert.assertEquals(0., eventDates.get(0).durationFrom(p1Ref), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., eventDates.get(1).durationFrom(o1Ref), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., eventDates.get(3).durationFrom(t1Ref), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., eventDates.get(4).durationFrom(t2Ref), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., eventDates.get(6).durationFrom(o2Ref), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., eventDates.get(7).durationFrom(p2Ref), Precision.DOUBLE_COMPARISON_EPSILON);
    }

    private List<AbsoluteDate> runTestInEclipse(final BodyInEclipseModelEnum model, final CelestialBody target,
                                                final double targetRadius, final CelestialBody occulted,
                                                final double occultedRadius, final CelestialBody occulting,
                                                final double occultingRadius)
                                                        throws PatriusException {

        final EventsLogger eventsLogger = new EventsLogger();

        // Initialize the propagator
        final AbsoluteDate iniDate = new AbsoluteDate(2025, 9, 7, UTC);
        final Propagator propagator = initPropagator(iniDate);

        // Create all the 8 detectors
        for (final boolean totalEclipse : new Boolean[] { true, false }) {
            for (final boolean bodyFullyIn : new Boolean[] { true, false }) {
                for (final int slopeSelection : new Integer[] { BodyInEclipseDetector.ENTRY,
                    BodyInEclipseDetector.EXIT }) {

                    final BodyInEclipseDetector detector = new BodyInEclipseDetector(
                        target, targetRadius, occulted, occultedRadius, occulting, occultingRadius,
                        totalEclipse, bodyFullyIn, model,
                        slopeSelection, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
                        Action.CONTINUE, Action.CONTINUE, false, false);

                    propagator.addEventDetector(eventsLogger.monitorDetector(detector));
                    this.detectors.add(detector);
                }
            }
        }

        // Propagate for 15 day to detect eclipse events
        // 1 day necesary for the testMoonInEclipse* tests
        // 1/2 moon's orbit later necessary for testEarthInMoonEclipse
        propagator.propagate(iniDate.shiftedBy(15 * 86400));

        // Loop over computed events
        final List<AbsoluteDate> eventDates = new ArrayList<>();
        for (final LoggedEvent loggedEvent : eventsLogger.getLoggedEvents()) {
            // Retrieve event date
            final AbsoluteDate eventDate = loggedEvent.getEventDate(EventDatationType.RECEIVER);
            eventDates.add(eventDate);
        }

        return eventDates;
    }

    private Propagator initPropagator(final AbsoluteDate iniDate) throws PatriusException {
        // Note: The orbit has no importance for this detector
        final CelestialBodyFrame referenceFrame = FramesFactory.getGCRF();
        final Orbit orbit = new EquatorialOrbit(2 * 2250e3, 0, 0, 0, 0, 0, PositionAngle.TRUE, referenceFrame,
            iniDate, Constants.WGS84_EARTH_MU);
        return new KeplerianPropagator(orbit);
    }

    private void verifyCopy(final BodyInEclipseDetector detector, final BodyInEclipseDetector detectorCopy) {

        // Check values for Variable
        Assert.assertEquals(detector.getOccultedRadius(), detectorCopy.getOccultedRadius(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(detector.getMaxIterationCount(), detectorCopy.getMaxIterationCount(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(detector.getMaxCheckInterval(), detectorCopy.getMaxCheckInterval(),
            Precision.DOUBLE_COMPARISON_EPSILON);

        // Change some values
        detectorCopy.setMaxIter(0);
        detectorCopy.setMaxCheckInterval(100.0);

        // Check values are not the same
        Assert.assertNotEquals(detector.getMaxIterationCount(), detectorCopy.getMaxIterationCount(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertNotEquals(detector.getMaxCheckInterval(), detectorCopy.getMaxCheckInterval(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

}
