/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.propagation.events;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.events.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.CodingEventDetector;
import fr.cnes.sirius.patrius.events.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.ExtremaGenericDetector.ExtremumType;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link ExtremaGenericDetector}.
 * 
 * @author nardia
 * 
 * @since 4.9
 * 
 */
public class ExtremaGenericDetectorTest {

    private static final double SUN_RADIUS = 696000000.;
    private static final double EARTH_RADIUS = 6400000.;
    private AbsoluteDate iniDate;
    private SpacecraftState initialState;
    private NumericalPropagator propagator;

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        FramesFactory.clear();
        final double mu = 3.9860047e14;
        final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        this.iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), this.iniDate, mu);
        this.initialState = new SpacecraftState(orbit);
        final double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6
        };
        final double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7
        };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        this.propagator = new NumericalPropagator(integrator);
        this.propagator.setInitialState(this.initialState);
    }

    @After
    public void tearDown() {
        this.iniDate = null;
        this.initialState = null;
        this.propagator = null;
    }

    /**
     * @throws PatriusException propagation exceptions
     * @testType UT
     * 
     * @testedMethod {@link ExtremaGenericDetector#ExtremaGenericOrbitDetector(D, ExtremumType)}
     * @testedMethod {@link ExtremaGenericDetector#ExtremaGenericOrbitDetector(D, ExtremumType, double)}
     * @testedMethod {@link ExtremaGenericDetector#ExtremaGenericOrbitDetector(D, ExtremumType, double, double)}
     * @testedMethod {@link ExtremaGenericDetector#ExtremaGenericOrbitDetector(D, ExtremumType, double, double, double)}
     * @testedMethod {@link ExtremaGenericDetector#ExtremaGenericOrbitDetector(D, ExtremumType, double, double, double, Action, Action)}
     * @testedMethod {@link ExtremaGenericDetector#ExtremaGenericOrbitDetector(D, ExtremumType, double, double, double, Action, Action, boolean, boolean)}
     * @testedMethod {@link ExtremaGenericDetector#getMaxIterationCount()}
     * @testedMethod {@link ExtremaGenericDetector#shouldBeRemoved()}
     * @testedMethod {@link ExtremaGenericDetector#getSlopeSelection()}
     * @testedMethod {@link ExtremaGenericDetector#getMaxCheckInterval()}
     * @testedMethod {@link ExtremaGenericDetector#getThreshold()}
     * 
     * @testPassCriteria the constructors shall create the ExtremaGenericOrbitDetector as expected and the getters shall
     *                   return the corresponding objects as expected
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public void testConstructorsAndGetters() throws PatriusException {
        // Create an EventDetector
        final EclipseDetector extOriEveDetector = new EclipseDetector(CelestialBodyFactory.getSun(), SUN_RADIUS,
            CelestialBodyFactory.getEarth(), EARTH_RADIUS, 1e-12, 100, 1E-6);
        // Create an ExtremaGenericOrbitDetector with the ExtremaGenericOrbitDetector(D, ExtremumType) constructor
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector1 = new ExtremaGenericDetector<>(
            extOriEveDetector, ExtremumType.MIN);
        // Check that the ExtremaGenericOrbitDetector has been correctly built and that the getters work properly
        Assert.assertNotNull(extGenOrbDetector1);
        Assert.assertEquals(extGenOrbDetector1.getMaxIterationCount(), 100);
        Assert.assertEquals(extGenOrbDetector1.shouldBeRemoved(), false);
        Assert.assertEquals(extGenOrbDetector1.getSlopeSelection(), 0);
        Assert.assertEquals(extGenOrbDetector1.getMaxCheckInterval(), ExtremaGenericDetector.DEFAULT_MAXCHECK);
        Assert.assertEquals(extGenOrbDetector1.getThreshold(), AbstractDetector.DEFAULT_THRESHOLD);

        // Create an ExtremaGenericOrbitDetector with the ExtremaGenericOrbitDetector(D, ExtremumType, double)
        // constructor
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector2 = new ExtremaGenericDetector<>(
            extOriEveDetector, ExtremumType.MAX, 60.);
        // Check that the ExtremaGenericOrbitDetector has been correctly built and that the getters work properly
        Assert.assertNotNull(extGenOrbDetector2);
        Assert.assertEquals(extGenOrbDetector2.getMaxIterationCount(), 100);
        Assert.assertEquals(extGenOrbDetector2.shouldBeRemoved(), false);
        Assert.assertEquals(extGenOrbDetector2.getSlopeSelection(), 1);
        Assert.assertEquals(extGenOrbDetector2.getMaxCheckInterval(), ExtremaGenericDetector.DEFAULT_MAXCHECK);
        Assert.assertEquals(extGenOrbDetector2.getThreshold(), AbstractDetector.DEFAULT_THRESHOLD);

        // Create an ExtremaGenericOrbitDetector with the ExtremaGenericOrbitDetector(D, ExtremumType, double, double)
        // constructor
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector3 = new ExtremaGenericDetector<>(
            extOriEveDetector, ExtremumType.MIN_MAX, 60., 240.);
        // Check that the ExtremaGenericOrbitDetector has been correctly built and that the getters work properly
        Assert.assertNotNull(extGenOrbDetector3);
        Assert.assertEquals(extGenOrbDetector3.getMaxIterationCount(), 100);
        Assert.assertEquals(extGenOrbDetector3.shouldBeRemoved(), false);
        Assert.assertEquals(extGenOrbDetector3.getSlopeSelection(), 2);
        Assert.assertEquals(extGenOrbDetector3.getMaxCheckInterval(), 240.);
        Assert.assertEquals(extGenOrbDetector3.getThreshold(), AbstractDetector.DEFAULT_THRESHOLD);

        // Create an ExtremaGenericOrbitDetector with the ExtremaGenericOrbitDetector(D, ExtremumType, double, double,
        // double) constructor
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector4 = new ExtremaGenericDetector<>(
            extOriEveDetector, ExtremumType.MIN, 60., 240., 1.0E-2);
        // Check that the ExtremaGenericOrbitDetector has been correctly built and that the getters work properly
        Assert.assertNotNull(extGenOrbDetector4);
        Assert.assertEquals(extGenOrbDetector4.getMaxIterationCount(), 100);
        Assert.assertEquals(extGenOrbDetector4.shouldBeRemoved(), false);
        Assert.assertEquals(extGenOrbDetector4.getSlopeSelection(), 0);
        Assert.assertEquals(extGenOrbDetector4.getMaxCheckInterval(), 240.);
        Assert.assertEquals(extGenOrbDetector4.getThreshold(), 1.0E-2);

        // Create an ExtremaGenericOrbitDetector with the ExtremaGenericOrbitDetector(D, ExtremumType, double, double,
        // double, Action, Action) constructor
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector5 = new ExtremaGenericDetector<>(
            extOriEveDetector, ExtremumType.MAX, 60., 240., 1.0E-2, Action.STOP, Action.STOP);
        // Check that the ExtremaGenericOrbitDetector has been correctly built and that the getters work properly
        Assert.assertNotNull(extGenOrbDetector5);
        Assert.assertEquals(extGenOrbDetector5.getMaxIterationCount(), 100);
        Assert.assertEquals(extGenOrbDetector5.shouldBeRemoved(), false);
        Assert.assertEquals(extGenOrbDetector5.getSlopeSelection(), 1);
        Assert.assertEquals(extGenOrbDetector5.getMaxCheckInterval(), 240.);
        Assert.assertEquals(extGenOrbDetector5.getThreshold(), 1.0E-2);

        // Create an ExtremaGenericOrbitDetector with the ExtremaGenericOrbitDetector(D, ExtremumType, double, double,
        // double, Action, Action, boolean, boolean) constructor
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector6 = new ExtremaGenericDetector<>(
            extOriEveDetector, ExtremumType.MIN_MAX, 60., 240., 1.0E-2, Action.STOP, Action.STOP, true, true);
        // Check that the ExtremaGenericOrbitDetector has been correctly built and that the getters work properly
        Assert.assertNotNull(extGenOrbDetector6);
        Assert.assertEquals(extGenOrbDetector6.getMaxIterationCount(), 100);
        Assert.assertEquals(extGenOrbDetector6.shouldBeRemoved(), false);
        Assert.assertEquals(extGenOrbDetector6.getSlopeSelection(), 2);
        Assert.assertEquals(extGenOrbDetector6.getMaxCheckInterval(), 240.);
        Assert.assertEquals(extGenOrbDetector6.getThreshold(), 1.0E-2);
    }

    /**
     * @throws PatriusException propagation exceptions
     * @testType UT
     * 
     * @testedMethod {@link ExtremaGenericDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link ExtremaGenericDetector#g(SpacecraftState)}
     * 
     * @testPassCriteria the eventOccured method shall return the Action as expected and the g method shall return the g
     *                   value as expected
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public void testEventOccurredAndG() throws PatriusException {
        // Some orbit data for the tests
        // (Real ISS data!)
        final double ix = 2156444.05;
        final double iy = 3611777.68;
        final double iz = -5316875.46;
        final double ivx = -6579.446110;
        final double ivy = 3916.478783;
        final double ivz = 8.876119;
        final AbsoluteDate iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());
        final double mu = CelestialBodyFactory.getEarth().getGM();
        final Vector3D issPos = new Vector3D(ix, iy, iz);
        final Vector3D issVit = new Vector3D(ivx, ivy, ivz);
        final PVCoordinates pvCoordinates = new PVCoordinates(issPos, issVit);
        final Frame frame = FramesFactory.getEME2000();
        final CartesianOrbit tISSOrbit = new CartesianOrbit(pvCoordinates, frame, iniDate, mu);
        final SpacecraftState tISSSpState = new SpacecraftState(tISSOrbit);
        // Create an EventDetector
        final EclipseDetector extOriEveDetector = new EclipseDetector(CelestialBodyFactory.getSun(), SUN_RADIUS,
            CelestialBodyFactory.getEarth(), EARTH_RADIUS, 1e-12, 100, 1E-6);

        // Create an ExtremaGenericOrbitDetector with MAX as extremum type and the DEFAULT_HALF_SPIN_COMPUTATION_STEP as
        // half spin computation step
        final double halfSpinCompStep1 = ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP;
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector1 = new ExtremaGenericDetector<>(
            extOriEveDetector, ExtremumType.MAX, halfSpinCompStep1,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
            Action.CONTINUE, Action.STOP, true, false);
        // Distance is decreasing and integration direction is forward
        Action event1 = extGenOrbDetector1.eventOccurred(tISSSpState, false, true);
        Assert.assertEquals(Action.STOP, event1);
        Assert.assertFalse(extGenOrbDetector1.shouldBeRemoved());
        // Distance is increasing and integration direction is forward
        event1 = extGenOrbDetector1.eventOccurred(tISSSpState, true, true);
        Assert.assertNull(null);
        Assert.assertFalse(extGenOrbDetector1.shouldBeRemoved());
        // Distance is decreasing and integration direction is backward
        event1 = extGenOrbDetector1.eventOccurred(tISSSpState, false, false);
        Assert.assertEquals(Action.STOP, event1);
        Assert.assertFalse(extGenOrbDetector1.shouldBeRemoved());
        // Distance is increasing and integration direction is backward
        event1 = extGenOrbDetector1.eventOccurred(tISSSpState, true, false);
        Assert.assertEquals(Action.STOP, event1);
        Assert.assertFalse(extGenOrbDetector1.shouldBeRemoved());
        // Check the g value
        Assert.assertEquals(
            (extOriEveDetector.g(tISSSpState.shiftedBy(halfSpinCompStep1)) - extOriEveDetector.g(
                    tISSSpState.shiftedBy(-halfSpinCompStep1))) / (2 * halfSpinCompStep1),
            extGenOrbDetector1.g(tISSSpState), 0.);

        // Create an ExtremaGenericOrbitDetector with MIN as extremum type and 1.0E-3 as half spin computation step
        final double halfSpinCompStep2 = 1.0E-3;
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector2 = new ExtremaGenericDetector<>(
            extOriEveDetector, ExtremumType.MIN, halfSpinCompStep2, ExtremaGenericDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.STOP, true, false);
        // Distance is decreasing and integration direction is forward
        Action event2 = extGenOrbDetector2.eventOccurred(tISSSpState, false, true);
        Assert.assertEquals(Action.CONTINUE, event2);
        Assert.assertTrue(extGenOrbDetector2.shouldBeRemoved());
        // Distance is increasing and integration direction is forward
        event2 = extGenOrbDetector2.eventOccurred(tISSSpState, true, true);
        Assert.assertEquals(Action.CONTINUE, event2);
        Assert.assertTrue(extGenOrbDetector2.shouldBeRemoved());
        // Distance is decreasing and integration direction is backward
        event2 = extGenOrbDetector2.eventOccurred(tISSSpState, false, false);
        Assert.assertEquals(Action.CONTINUE, event2);
        Assert.assertTrue(extGenOrbDetector2.shouldBeRemoved());
        // Distance is increasing and integration direction is backward
        event2 = extGenOrbDetector2.eventOccurred(tISSSpState, true, false);
        Assert.assertEquals(Action.CONTINUE, event2);
        Assert.assertTrue(extGenOrbDetector2.shouldBeRemoved());
        // Check the g value
        Assert.assertEquals(
                (extOriEveDetector.g(tISSSpState.shiftedBy(halfSpinCompStep2)) - extOriEveDetector.g(
                        tISSSpState.shiftedBy(-halfSpinCompStep2))) / (2 * halfSpinCompStep2),
            extGenOrbDetector2.g(tISSSpState), 0.);

        // Create an ExtremaGenericOrbitDetector with MIN_MAX as extremum type and 2.0E-6 as half spin computation step
        final double halfSpinCompStep3 = 2.0E-6;
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector3 = new ExtremaGenericDetector<>(
            extOriEveDetector, ExtremumType.MIN_MAX, halfSpinCompStep3,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
            Action.CONTINUE, Action.STOP, true, false);
        // Distance is decreasing and integration direction is forward
        Action event3 = extGenOrbDetector3.eventOccurred(tISSSpState, false, true);
        Assert.assertEquals(Action.STOP, event3);
        Assert.assertFalse(extGenOrbDetector3.shouldBeRemoved());
        // Distance is increasing and integration direction is forward
        event3 = extGenOrbDetector3.eventOccurred(tISSSpState, true, true);
        Assert.assertEquals(Action.CONTINUE, event3);
        Assert.assertTrue(extGenOrbDetector3.shouldBeRemoved());
        // Distance is decreasing and integration direction is backward
        event3 = extGenOrbDetector3.eventOccurred(tISSSpState, false, false);
        Assert.assertEquals(Action.CONTINUE, event3);
        Assert.assertTrue(extGenOrbDetector3.shouldBeRemoved());
        // Distance is increasing and integration direction is backward
        event3 = extGenOrbDetector3.eventOccurred(tISSSpState, true, false);
        Assert.assertEquals(Action.STOP, event3);
        Assert.assertFalse(extGenOrbDetector3.shouldBeRemoved());
        // Check the g value
        Assert.assertEquals(
                (extOriEveDetector.g(tISSSpState.shiftedBy(halfSpinCompStep3)) - extOriEveDetector.g(
                        tISSSpState.shiftedBy(-halfSpinCompStep3))) / (2 * halfSpinCompStep3),
            extGenOrbDetector3.g(tISSSpState), 0.);
    }

    /**
     * @throws PatriusException propagation exceptions
     * @testType UT
     * 
     * @testedMethod {@link ExtremaGenericDetector#copy()}
     * 
     * @testPassCriteria the copy method shall return the ExtremaGenericOrbitDetector as expected
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public void testCopy() throws PatriusException {
        // Create an EventDetector
        final EclipseDetector extOriEveDetector = new EclipseDetector(CelestialBodyFactory.getSun(), SUN_RADIUS,
            CelestialBodyFactory.getEarth(), EARTH_RADIUS, 1e-12, 100, 1E-6);
        // Create an ExtremaGenericOrbitDetector
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector = new ExtremaGenericDetector<>(
            extOriEveDetector, ExtremumType.MIN_MAX, 60., 240., 1.0E-2, Action.STOP, Action.STOP, true, true);
        // Copy the ExtremaGenericOrbitDetector
        final ExtremaGenericDetector<EventDetector> extGenOrbDetectorCopy = extGenOrbDetector
            .copy();
        // Check that the copy of the ExtremaGenericOrbitDetector has been correctly built
        Assert.assertNotNull(extGenOrbDetectorCopy);
        Assert.assertEquals(extGenOrbDetectorCopy.getMaxIterationCount(), extGenOrbDetector.getMaxIterationCount());
        Assert.assertEquals(extGenOrbDetectorCopy.shouldBeRemoved(), extGenOrbDetector.shouldBeRemoved());
        Assert.assertEquals(extGenOrbDetectorCopy.getSlopeSelection(), extGenOrbDetector.getSlopeSelection());
        Assert.assertEquals(extGenOrbDetectorCopy.getMaxCheckInterval(), extGenOrbDetector.getMaxCheckInterval());
        Assert.assertEquals(extGenOrbDetectorCopy.getThreshold(), extGenOrbDetector.getThreshold());
    }

    /**
     * @throws PatriusException propagation exceptions
     * @testType UT
     * 
     * @testedMethod {@link ExtremaGenericDetector.ExtremumType#getSlopeSelection()}
     * 
     * @testPassCriteria the getSlopeSelection method shall return the slope selection as expected
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public void testGetSlopeSelection() throws PatriusException {
        Assert.assertEquals(0, ExtremumType.MIN.getSlopeSelection());
        Assert.assertEquals(1, ExtremumType.MAX.getSlopeSelection());
        Assert.assertEquals(2, ExtremumType.MIN_MAX.getSlopeSelection());
    }

    /**
     * @throws PatriusException propagation exceptions
     * @testType UT
     * 
     * @testedMethod {@link ExtremaGenericDetector.ExtremumType#find(int)}
     * 
     * @testPassCriteria the find method shall return the ExtremumType as expected
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public void testFind() throws PatriusException {
        Assert.assertEquals(ExtremumType.MIN, ExtremumType.find(0));
        Assert.assertEquals(ExtremumType.MAX, ExtremumType.find(1));
        Assert.assertEquals(ExtremumType.MIN_MAX, ExtremumType.find(2));
    }

    /**
     * @throws PatriusException propagation exceptions
     * @testType UT
     * 
     * @testedMethod {@link ExtremaGenericDetector#logExtremaEventsOverTimeInterval(CodedEventsLogger, Propagator, AbsoluteDateInterval)}
     * 
     * @testPassCriteria the logExtremaEventsOverTimeInterval method shall return the spacecraft state and the detected
     *                   events as expected
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public void testLogExtremaEvents() throws PatriusException {
        // Create an EventDetector
        final EclipseDetector extOriEveDetector = new EclipseDetector(CelestialBodyFactory.getSun(), SUN_RADIUS,
            CelestialBodyFactory.getEarth(), EARTH_RADIUS, 1e-12, 100, 1E-6);
        // Create an ExtremaGenericOrbitDetector
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector = new ExtremaGenericDetector<>(
            extOriEveDetector, ExtremumType.MIN_MAX, 60., 240., 1.0E-2, Action.STOP, Action.STOP, true, true);
        // Create a coding event detector
        final CodingEventDetector extremaCoder = new GenericCodingEventDetector(extGenOrbDetector, "MIN", "MAX", false,
            "EXTREMUM");
        // Create the reference coded events logger
        final CodedEventsLogger refEventsLogger = new CodedEventsLogger();
        // Let's the logger monitor the detector
        final EventDetector monitoredDetector = refEventsLogger.monitorDetector(extremaCoder);
        // Add the detector to the propagator
        this.propagator.addEventDetector(monitoredDetector);
        // Create the date interval
        final AbsoluteDate lowerData = this.iniDate.shiftedBy(-120);
        final AbsoluteDate upperData = this.iniDate.shiftedBy(120);
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(lowerData, upperData);
        // Propagate the spacecraft state
        final SpacecraftState spacecraftState = this.propagator.propagate(
            lowerData.shiftedBy(-2 * extGenOrbDetector.getMaxCheckInterval()),
            upperData.shiftedBy(2 * extGenOrbDetector.getMaxCheckInterval()));
        // Clear the events detectors
        this.propagator.clearEventsDetectors();
        // Create the result coded events logger
        final CodedEventsLogger resEventsLogger = new CodedEventsLogger();
        // Check that the spacecraft states are the same
        Assert.assertEquals(
            extGenOrbDetector.logExtremaEventsOverTimeInterval(resEventsLogger, this.propagator, interval)
            .getPVCoordinates(), spacecraftState.getPVCoordinates());
        // Check that the logged events are of the same size
        Assert.assertEquals(resEventsLogger.getCodedEventsList().getList().size(), refEventsLogger
            .getCodedEventsList().getList().size());
        // Check that the logged events are the same (we use first() because there is only 1 event)
        Assert.assertEquals(
            resEventsLogger.getLoggedCodedEventSet().first().getCodedEvent().getCode()
                .compareTo(refEventsLogger.getLoggedCodedEventSet().first().getCodedEvent().getCode()), 0);
        Assert.assertEquals(
            resEventsLogger.getLoggedCodedEventSet().first().getCodedEvent().getComment()
                .compareTo(refEventsLogger.getLoggedCodedEventSet().first().getCodedEvent().getComment()), 0);
        Assert.assertTrue(
            resEventsLogger.getLoggedCodedEventSet().first().getCodedEvent().getDate()
                .equals(refEventsLogger.getLoggedCodedEventSet().first().getCodedEvent().getDate(), 1.0E-10));
    }
}
