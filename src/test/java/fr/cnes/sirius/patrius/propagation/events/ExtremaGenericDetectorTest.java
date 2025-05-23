/**
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-39:08/12/2023:[PATRIUS] Generalisation de DotProductDetector et ExtremaDotProductDetector
 * VERSION:4.11:DM:DM-3232:22/05/2023:[PATRIUS] Detection d'extrema dans la classe ExtremaGenericDetector
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.propagation.events;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.BasicPVCoordinatesProvider;
import fr.cnes.sirius.patrius.attitudes.directions.GenericTargetDirection;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.DotProductDetector;
import fr.cnes.sirius.patrius.events.detectors.EclipseDetector;
import fr.cnes.sirius.patrius.events.detectors.ExtremaGenericDetector;
import fr.cnes.sirius.patrius.events.detectors.ExtremaGenericDetector.ExtremumType;
import fr.cnes.sirius.patrius.events.postprocessing.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.postprocessing.CodingEventDetector;
import fr.cnes.sirius.patrius.events.postprocessing.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
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
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.clear();
        final double mu = 3.9860047e14;
        final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        this.iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity), FramesFactory.getEME2000(),
                this.iniDate, mu);
        this.initialState = new SpacecraftState(orbit);
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, absTolerance,
                relTolerance);
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
     * @testPassCriteria the constructors shall create the ExtremaGenericOrbitDetector as expected
     *                   and the getters shall
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
        // Create an ExtremaGenericOrbitDetector with the ExtremaGenericOrbitDetector(D,
        // ExtremumType) constructor
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector1 = new ExtremaGenericDetector<>(
                extOriEveDetector, ExtremumType.MIN);
        // Check that the ExtremaGenericOrbitDetector has been correctly built and that the getters
        // work properly
        Assert.assertNotNull(extGenOrbDetector1);
        Assert.assertEquals(extGenOrbDetector1.getMaxIterationCount(), 100);
        Assert.assertEquals(extGenOrbDetector1.shouldBeRemoved(), false);
        Assert.assertEquals(extGenOrbDetector1.getSlopeSelection(), 0);
        Assert.assertEquals(extGenOrbDetector1.getMaxCheckInterval(), ExtremaGenericDetector.DEFAULT_MAXCHECK,
                Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(extGenOrbDetector1.getThreshold(), AbstractDetector.DEFAULT_THRESHOLD,
                Precision.DOUBLE_COMPARISON_EPSILON);

        // Create an ExtremaGenericOrbitDetector with the ExtremaGenericOrbitDetector(D,
        // ExtremumType, double) constructor
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector2 = new ExtremaGenericDetector<>(
                extOriEveDetector, ExtremumType.MAX, 60.);
        // Check that the ExtremaGenericOrbitDetector has been correctly built and that the getters
        // work properly
        Assert.assertNotNull(extGenOrbDetector2);
        Assert.assertEquals(extGenOrbDetector2.getMaxIterationCount(), 100);
        Assert.assertEquals(extGenOrbDetector2.shouldBeRemoved(), false);
        Assert.assertEquals(extGenOrbDetector2.getSlopeSelection(), 1);
        Assert.assertEquals(extGenOrbDetector2.getMaxCheckInterval(), ExtremaGenericDetector.DEFAULT_MAXCHECK,
                Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(extGenOrbDetector2.getThreshold(), AbstractDetector.DEFAULT_THRESHOLD,
                Precision.DOUBLE_COMPARISON_EPSILON);

        // Create an ExtremaGenericOrbitDetector with the ExtremaGenericOrbitDetector(D,
        // ExtremumType, double, double) constructor
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector3 = new ExtremaGenericDetector<>(
                extOriEveDetector, ExtremumType.MIN_MAX, 60., 240.);
        // Check that the ExtremaGenericOrbitDetector has been correctly built and that the getters
        // work properly
        Assert.assertNotNull(extGenOrbDetector3);
        Assert.assertEquals(extGenOrbDetector3.getMaxIterationCount(), 100);
        Assert.assertEquals(extGenOrbDetector3.shouldBeRemoved(), false);
        Assert.assertEquals(extGenOrbDetector3.getSlopeSelection(), 2);
        Assert.assertEquals(extGenOrbDetector3.getMaxCheckInterval(), 240., Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(extGenOrbDetector3.getThreshold(), AbstractDetector.DEFAULT_THRESHOLD,
                Precision.DOUBLE_COMPARISON_EPSILON);

        // Create an ExtremaGenericOrbitDetector with the ExtremaGenericOrbitDetector(D,
        // ExtremumType, double, double,double) constructor
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector4 = new ExtremaGenericDetector<>(
                extOriEveDetector, ExtremumType.MIN, 60., 240., 1.0E-2);
        // Check that the ExtremaGenericOrbitDetector has been correctly built and that the getters
        // work properly
        Assert.assertNotNull(extGenOrbDetector4);
        Assert.assertEquals(extGenOrbDetector4.getMaxIterationCount(), 100);
        Assert.assertEquals(extGenOrbDetector4.shouldBeRemoved(), false);
        Assert.assertEquals(extGenOrbDetector4.getSlopeSelection(), 0);
        Assert.assertEquals(extGenOrbDetector4.getMaxCheckInterval(), 240., Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(extGenOrbDetector4.getThreshold(), 1.0E-2, Precision.DOUBLE_COMPARISON_EPSILON);

        // Create an ExtremaGenericOrbitDetector with the ExtremaGenericOrbitDetector(D,
        // ExtremumType, double, double,double, Action, Action) constructor
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector5 = new ExtremaGenericDetector<>(
                extOriEveDetector, ExtremumType.MAX, 60., 240., 1.0E-2, Action.STOP, Action.STOP);
        // Check that the ExtremaGenericOrbitDetector has been correctly built and that the getters
        // work properly
        Assert.assertNotNull(extGenOrbDetector5);
        Assert.assertEquals(extGenOrbDetector5.getMaxIterationCount(), 100);
        Assert.assertEquals(extGenOrbDetector5.shouldBeRemoved(), false);
        Assert.assertEquals(extGenOrbDetector5.getSlopeSelection(), 1);
        Assert.assertEquals(extGenOrbDetector5.getMaxCheckInterval(), 240., Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(extGenOrbDetector5.getThreshold(), 1.0E-2, Precision.DOUBLE_COMPARISON_EPSILON);

        // Create an ExtremaGenericOrbitDetector with the ExtremaGenericOrbitDetector(D,
        // ExtremumType, double, double,double, Action, Action, boolean, boolean) constructor
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector6 = new ExtremaGenericDetector<>(
                extOriEveDetector, ExtremumType.MIN_MAX, 60., 240., 1.0E-2, Action.STOP, Action.STOP, true, true);
        // Check that the ExtremaGenericOrbitDetector has been correctly built and that the getters
        // work properly
        Assert.assertNotNull(extGenOrbDetector6);
        Assert.assertEquals(extGenOrbDetector6.getMaxIterationCount(), 100);
        Assert.assertEquals(extGenOrbDetector6.shouldBeRemoved(), false);
        Assert.assertEquals(extGenOrbDetector6.getSlopeSelection(), 2);
        Assert.assertEquals(extGenOrbDetector6.getMaxCheckInterval(), 240., Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(extGenOrbDetector6.getThreshold(), 1.0E-2, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @throws PatriusException propagation exceptions
     * @testType UT
     *
     * @testedMethod {@link ExtremaGenericDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link ExtremaGenericDetector#g(SpacecraftState)}
     *
     * @testPassCriteria the eventOccured method shall return the Action as expected and the g
     *                   method shall return the g
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

        // Create an ExtremaGenericOrbitDetector with MAX as extremum type and the
        // DEFAULT_HALF_SPIN_COMPUTATION_STEP as half spin computation step
        final double halfSpinCompStep1 = ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP;
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector1 = new ExtremaGenericDetector<>(
                extOriEveDetector, ExtremumType.MAX, halfSpinCompStep1, ExtremaGenericDetector.DEFAULT_MAXCHECK,
                AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.STOP, true, false);
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
                (extOriEveDetector.g(tISSSpState.shiftedBy(halfSpinCompStep1)) - extOriEveDetector.g(tISSSpState
                        .shiftedBy(-halfSpinCompStep1))) / (2 * halfSpinCompStep1), extGenOrbDetector1.g(tISSSpState),
                0.);

        // Create an ExtremaGenericOrbitDetector with MIN as extremum type and 1.0E-3 as half spin
        // computation step
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
                (extOriEveDetector.g(tISSSpState.shiftedBy(halfSpinCompStep2)) - extOriEveDetector.g(tISSSpState
                        .shiftedBy(-halfSpinCompStep2))) / (2 * halfSpinCompStep2), extGenOrbDetector2.g(tISSSpState),
                0.);

        // Create an ExtremaGenericOrbitDetector with MIN_MAX as extremum type and 2.0E-6 as half
        // spin computation step
        final double halfSpinCompStep3 = 2.0E-6;
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector3 = new ExtremaGenericDetector<>(
                extOriEveDetector, ExtremumType.MIN_MAX, halfSpinCompStep3, ExtremaGenericDetector.DEFAULT_MAXCHECK,
                AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.STOP, true, false);
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
                (extOriEveDetector.g(tISSSpState.shiftedBy(halfSpinCompStep3)) - extOriEveDetector.g(tISSSpState
                        .shiftedBy(-halfSpinCompStep3))) / (2 * halfSpinCompStep3), extGenOrbDetector3.g(tISSSpState),
                0.);
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
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector = new ExtremaGenericDetector<>(extOriEveDetector,
                ExtremumType.MIN_MAX, 60., 240., 1.0E-2, Action.STOP, Action.STOP, true, true);
        // Copy the ExtremaGenericOrbitDetector
        final ExtremaGenericDetector<EventDetector> extGenOrbDetectorCopy = extGenOrbDetector.copy();
        // Check that the copy of the ExtremaGenericOrbitDetector has been correctly built
        Assert.assertNotNull(extGenOrbDetectorCopy);
        Assert.assertEquals(extGenOrbDetectorCopy.getMaxIterationCount(), extGenOrbDetector.getMaxIterationCount());
        Assert.assertEquals(extGenOrbDetectorCopy.shouldBeRemoved(), extGenOrbDetector.shouldBeRemoved());
        Assert.assertEquals(extGenOrbDetectorCopy.getSlopeSelection(), extGenOrbDetector.getSlopeSelection());
        Assert.assertEquals(extGenOrbDetectorCopy.getMaxCheckInterval(), extGenOrbDetector.getMaxCheckInterval(),
                Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(extGenOrbDetectorCopy.getThreshold(), extGenOrbDetector.getThreshold(),
                Precision.DOUBLE_COMPARISON_EPSILON);
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
     * @testPassCriteria the logExtremaEventsOverTimeInterval method shall return the spacecraft
     *                   state and the detected
     *                   events as expected
     *
     * @referenceVersion 4.9
     *
     * @nonRegressionVersion 4.9
     */
    @Test
    public void testLogExtremaEvents() throws PatriusException {
        this.propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.propagator
            .getInitialState().getMu())));
        // Create an EventDetector
        final EclipseDetector extOriEveDetector = new EclipseDetector(CelestialBodyFactory.getSun(), SUN_RADIUS,
                CelestialBodyFactory.getEarth(), EARTH_RADIUS, 1e-12, 100, 1E-6);
        // Create an ExtremaGenericOrbitDetector
        final ExtremaGenericDetector<EventDetector> extGenOrbDetector = new ExtremaGenericDetector<>(extOriEveDetector,
                ExtremumType.MIN_MAX, 60., 240., 1.0E-2, Action.STOP, Action.STOP, true, true);
        // Create a coding event detector
        final CodingEventDetector extremaCoder = new GenericCodingEventDetector(extGenOrbDetector, "MIN", "MAX");
        // Create the reference coded events logger
        final CodedEventsLogger refEventsLogger = new CodedEventsLogger();
        // Let's the logger monitor the detector
        final EventDetector monitoredDetector = refEventsLogger.monitorDetector(extremaCoder);
        // Add the detector to the propagator
        this.propagator.addEventDetector(monitoredDetector);
        // Create the date interval
        final AbsoluteDate lowerData = this.iniDate.shiftedBy(-600);
        final AbsoluteDate upperData = this.iniDate.shiftedBy(600);
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(lowerData, upperData);
        // Propagate the spacecraft state
        final SpacecraftState spacecraftState = this.propagator.propagate(lowerData, upperData);
        // Clear the events detectors
        this.propagator.clearEventsDetectors();
        // Create the result coded events logger
        final CodedEventsLogger resEventsLogger = new CodedEventsLogger();
        // Check that the spacecraft states are the same
        final PVCoordinates pvCoord1 = extGenOrbDetector.logExtremaEventsOverTimeInterval(resEventsLogger,
            this.propagator, interval).getPVCoordinates();
        final PVCoordinates pvCoord2 = spacecraftState.getPVCoordinates();
        final Vector3D pos1 = pvCoord1.getPosition();
        final Vector3D vel1 = pvCoord1.getVelocity();
        final Vector3D acc1 = pvCoord1.getAcceleration();
        final Vector3D pos2 = pvCoord2.getPosition();
        final Vector3D vel2 = pvCoord2.getVelocity();
        final Vector3D acc2 = pvCoord2.getAcceleration();
        final double tolerance = 2E-8;
        Assert.assertEquals(pos1.getX(), pos2.getX(), tolerance);
        Assert.assertEquals(pos1.getY(), pos2.getY(), tolerance);
        Assert.assertEquals(pos1.getZ(), pos2.getZ(), tolerance);
        Assert.assertEquals(vel1.getX(), vel2.getX(), tolerance);
        Assert.assertEquals(vel1.getY(), vel2.getY(), tolerance);
        Assert.assertEquals(vel1.getZ(), vel2.getZ(), tolerance);
        Assert.assertEquals(acc1.getX(), acc2.getX(), tolerance);
        Assert.assertEquals(acc1.getY(), acc2.getY(), tolerance);
        Assert.assertEquals(acc1.getZ(), acc2.getZ(), tolerance);
        // Check that the logged events are of the same size
        Assert.assertEquals(resEventsLogger.getCodedEventsList().getList().size(), refEventsLogger.getCodedEventsList()
                .getList().size());
        // Check that the logged events are the same (we use first() because there is only 1 event)
        Assert.assertEquals(
                resEventsLogger.getLoggedCodedEventSet().first().getCodedEvent().getCode()
                        .compareTo(refEventsLogger.getLoggedCodedEventSet().first().getCodedEvent().getCode()), 0);
        Assert.assertEquals(
                resEventsLogger.getLoggedCodedEventSet().first().getCodedEvent().getComment()
                        .compareTo(refEventsLogger.getLoggedCodedEventSet().first().getCodedEvent().getComment()), 0);
        Assert.assertTrue(resEventsLogger.getLoggedCodedEventSet().first().getCodedEvent().getDate()
                .equals(refEventsLogger.getLoggedCodedEventSet().first().getCodedEvent().getDate(), 1.0E-10));
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link ExtremaGenericDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     *
     * @description tests that the position of the satellite when the event occurs is correct
     *
     * @input an extrema dot product detector, a keplerian propagator, and a target vector
     *
     * @testPassCriteria the distance between the end state (when the event occurs) and the
     *                   reference position is lower than 1e-5
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testDotProductKeplerianPropagator() throws PatriusException {

        // propagator
        final Frame eme2000Frame = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double a = 7000000.0;

        final Orbit initialOrbit = new KeplerianOrbit(a, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.TRUE, eme2000Frame,
            date, Utils.mu);

        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);

        propagator.resetInitialState(initialState);
        // Propagate over a half orbit period :
        final double time = 5 * initialOrbit.getKeplerianPeriod();

        // Target PV: (5,0,0),(0,0,0) in EME2000
        PVCoordinatesProvider pvCoordTarget = new BasicPVCoordinatesProvider(new PVCoordinates(new Vector3D(0, 5, 0),
            new Vector3D(0, 0, 0)), eme2000Frame);

        // Testing the detector for different target positions/extremum type/normalizing

        // detector creation with Target position (0,5,0), searching minimum
        DotProductDetector dotProductPassageDetector = new DotProductDetector(pvCoordTarget, true,
                false, 0, eme2000Frame, 0);
        EventDetector detector = new ExtremaGenericDetector<DotProductDetector>(
                dotProductPassageDetector, ExtremumType.MIN, 0.5, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
                Action.STOP, Action.STOP);
        propagator = new KeplerianPropagator(initialOrbit);
        propagator.addEventDetector(detector);
        SpacecraftState endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, new Vector3D(0.0, -a, 0.0).distance(endState.getPVCoordinates().getPosition()), 1e-5);

        // detector creation with Target position (0,5,0), searching maximum
        dotProductPassageDetector = new DotProductDetector(pvCoordTarget, false, true, 0, eme2000Frame, 1);
        detector = new ExtremaGenericDetector<DotProductDetector>(dotProductPassageDetector, ExtremumType.MAX,
                0.5, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP);
        propagator = new KeplerianPropagator(initialOrbit);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, new Vector3D(0.0, a, 0.0).distance(endState.getPVCoordinates().getPosition()), 1e-5);

        // detector creation with Target position (0,5,0), searching minimum or maximum
        dotProductPassageDetector = new DotProductDetector(pvCoordTarget, true, true, 0, eme2000Frame, 2);
        detector = new ExtremaGenericDetector<DotProductDetector>(dotProductPassageDetector,
                ExtremumType.MIN_MAX, 0.5, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP);
        propagator = new KeplerianPropagator(initialOrbit);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, new Vector3D(0.0, a, 0.0).distance(endState.getPVCoordinates().getPosition()), 1e-5);

        // detector tests with different PV of target
        pvCoordTarget = new BasicPVCoordinatesProvider(
            new PVCoordinates(new Vector3D(-15, 0, 0), new Vector3D(0, 0, 0)), eme2000Frame);
        dotProductPassageDetector = new DotProductDetector(pvCoordTarget, false, false, 0, eme2000Frame, 2);
        detector = new ExtremaGenericDetector<DotProductDetector>(dotProductPassageDetector,
                ExtremumType.MIN_MAX, 0.5, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP);
        propagator = new KeplerianPropagator(initialOrbit);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, new Vector3D(a, 0.0, 0.0).distance(endState.getPVCoordinates().getPosition()), 1e-5);

        // detector creation
        pvCoordTarget = new BasicPVCoordinatesProvider(
            new PVCoordinates(new Vector3D(15, 15, 0), new Vector3D(0, 0, 0)), eme2000Frame);
        dotProductPassageDetector = new DotProductDetector(pvCoordTarget, false, false, 0, eme2000Frame, 2);
        detector = new ExtremaGenericDetector<DotProductDetector>(dotProductPassageDetector,
                ExtremumType.MIN_MAX, 0.5, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP);
        propagator = new KeplerianPropagator(initialOrbit);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));
        Assert.assertEquals(0.0, new Vector3D(a * MathLib.cos(MathLib.PI / 4), a * MathLib.sin(MathLib.PI / 4), 0.0)
            .distance(endState.getPVCoordinates().getPosition()), 1e-5);

        // detector creation with no frame specified
        pvCoordTarget = new BasicPVCoordinatesProvider(
            new PVCoordinates(new Vector3D(15, 0, 0), new Vector3D(0, 0, 0)), eme2000Frame);
        dotProductPassageDetector = new DotProductDetector(pvCoordTarget, false, false, 0, null, 2);
        detector = new ExtremaGenericDetector<DotProductDetector>(dotProductPassageDetector,
                ExtremumType.MIN_MAX, 0.5, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP);
        propagator = new KeplerianPropagator(initialOrbit);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));
        Assert.assertEquals(0.0, new Vector3D(a, 0, 0.0)
            .distance(endState.getPVCoordinates().getPosition()), 1e-5);
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link ExtremaGenericDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     *
     * @description tests the well fonctionning of extrema dot product detector when using different constructors
     *
     * @input an extrema dot product detector, a keplerian propagator, and a target vector
     *
     * @testPassCriteria the distance between the end state (when the event occurs) and the
     *                   reference position is lower than 1e-5
     *
     * @referenceVersion 4.13
     *
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testDotProductSeveralConstructors() throws PatriusException {

        // propagator
        final Frame eme2000Frame = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double a = 7000000.0;

        final Orbit initialOrbit = new KeplerianOrbit(a, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.TRUE, eme2000Frame,
            date, Utils.mu);

        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);

        propagator.resetInitialState(initialState);
        // Propagate over a half orbit period :
        final double time = 5 * initialOrbit.getKeplerianPeriod();

        // Target PV: (5,0,0),(0,0,0) in EME2000
        PVCoordinatesProvider pvCoordTarget = new BasicPVCoordinatesProvider(new PVCoordinates(new Vector3D(0, 5, 0),
            new Vector3D(0, 0, 0)), eme2000Frame);

        // Testing the detector for different target positions/extremum type/normalizing

        // detector creation with Target position (0,5,0), searching minimum
        DotProductDetector dotProductPassageDetector = new DotProductDetector(new GenericTargetDirection(pvCoordTarget), true,
                false, 0, eme2000Frame, 2);
        ExtremaGenericDetector<DotProductDetector> detector = new ExtremaGenericDetector<DotProductDetector>(
                dotProductPassageDetector, ExtremumType.MIN, 0.5, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
                Action.STOP, Action.STOP);
        propagator.addEventDetector(detector);
        SpacecraftState endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, detector.g(endState), Precision.DOUBLE_COMPARISON_EPSILON);

        // detector creation with Target position (0,5,0), searching maximum
        dotProductPassageDetector = new DotProductDetector(pvCoordTarget, true,
                true, 0, eme2000Frame, 1);
        detector = new ExtremaGenericDetector<DotProductDetector>(
                dotProductPassageDetector, ExtremumType.MAX, 0.5, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
                Action.STOP, Action.STOP);
        propagator.clearEventsDetectors();
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, detector.g(endState), Precision.DOUBLE_COMPARISON_EPSILON);

        // detector creation with Target position (0,5,0), searching minimum or maximum
        dotProductPassageDetector = new DotProductDetector(pvCoordTarget, true,
                true, 0, eme2000Frame, 2);
        detector = new ExtremaGenericDetector<DotProductDetector>(
                dotProductPassageDetector, ExtremumType.MIN_MAX, 0.5, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
                Action.STOP, Action.STOP);
        propagator.clearEventsDetectors();
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, detector.g(endState), Precision.DOUBLE_COMPARISON_EPSILON);

        // detector tests with different PV of target and reference direction
        pvCoordTarget = new BasicPVCoordinatesProvider(
            new PVCoordinates(new Vector3D(-15, 0, 0), new Vector3D(0, 0, 0)), eme2000Frame);
        dotProductPassageDetector = new DotProductDetector(pvCoordTarget, false,
                false, 0, eme2000Frame, 2);
        detector = new ExtremaGenericDetector<DotProductDetector>(
                dotProductPassageDetector, ExtremumType.MIN_MAX, 0.5, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD);
        propagator.clearEventsDetectors();
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, new Vector3D(a, 0.0, 0.0).distance(endState.getPVCoordinates().getPosition()), 1e-5);

        // Now retest with the reference direction
        final PVCoordinatesProvider pvCoordRef = new BasicPVCoordinatesProvider(new PVCoordinates(new Vector3D(a, 0.0,
            0.0), new Vector3D(0, 0, 0)), eme2000Frame);
        dotProductPassageDetector = new DotProductDetector(new GenericTargetDirection(pvCoordRef),
                new GenericTargetDirection(pvCoordTarget), false,
                false, 0, eme2000Frame, 2, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
                Action.STOP, Action.STOP);
        detector = new ExtremaGenericDetector<DotProductDetector>(
                dotProductPassageDetector, ExtremumType.MIN_MAX, 0.5, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
                Action.STOP, Action.STOP);
        propagator.addEventDetector(detector);
        endState = propagator.propagate(date.shiftedBy(time));

        Assert.assertEquals(0.0, new Vector3D(a, 0.0, 0.0).distance(endState.getPVCoordinates().getPosition()), 1e-5);
    }
}
