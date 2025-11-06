package fr.cnes.sirius.patrius.events.sensor;

/** HISTORY
 * VERSION:4.16:OPENFD-550:25/04/2025:[PATRIUS] Detecteur de masquage par un corps celeste
 * END-HISTORY
 */
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.assembly.properties.SensorProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.ConstantRadiusProvider;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.events.detectors.LineMaskingDetector;
import fr.cnes.sirius.patrius.events.detectors.LinkTypeHandler.SignalPropagationRole;
import fr.cnes.sirius.patrius.events.detectors.SatToSatMutualVisibilityDetector;
import fr.cnes.sirius.patrius.events.detectors.SatToSatMutualVisibilityDetector.SatToSatLinkType;
import fr.cnes.sirius.patrius.events.detectors.StationToSatMutualVisibilityDetector;
import fr.cnes.sirius.patrius.events.detectors.VisibilityFromStationDetector.LinkType;
import fr.cnes.sirius.patrius.events.utils.SignalPropagationWrapperDetector;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.fieldsofview.OmnidirectionalField;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.groundstation.GeometricStationAntenna;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.EquatorialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Test class for {@link LineMaskingDetector}.
 */
public class LineMaskingDetectorTest {

    /** Initial date. */
    private AbsoluteDate iniDate;

    /** Called before each test. */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");

        // Initial date
        this.iniDate = new AbsoluteDate(2005, 9, 7, TimeScalesFactory.getUTC());
    }

    /**
     * @throws PatriusException
     *
     * @testType UT
     * @description This tests verifies that the constructors in LineMaskingDetector correctly initialize the objects
     *
     * @input input parameters for the object LineMaskingDetector (not relevant)
     *
     * @output the LineMaskingDetector object (class getters)
     * @testPassCriteria the object is correctly constructed
     *
     * @referenceVersion 4.16
     * @nonregressionVersion 4.16
     */
    @Test
    public void testConstructors() throws PatriusException {

        final List<BodyShape> maskingBodies = new ArrayList<>();
        maskingBodies.add(CelestialBodyFactory.getEarth().getShape());
        final LineMaskingDetector detector = new LineMaskingDetector(CelestialBodyFactory.getEarth(),
            SignalPropagationRole.EMITTER, CelestialBodyFactory.getMoon(), maskingBodies);

        Assert.assertEquals(CelestialBodyFactory.getEarth(), detector.getMainElement());
        Assert.assertEquals(maskingBodies, detector.getMaskingBodies());
    }

    /**
     * @throws PatriusException
     *
     * @testType UT
     * @description This tests verifies that the copy method in LineMaskingDetector correctly creates a copy of the
     *              input detector
     *
     * @input input parameters to create an object LineMaskingDetector (dummies, not important but different from the
     *        default ones)
     *
     * @output the values from the different getters
     * @testPassCriteria the copy matches the original detector. A modification to the original does not impact the
     *                   copy.
     *
     * @referenceVersion 4.16
     * @nonregressionVersion 4.16
     */
    @Test
    public void testCopy() throws PatriusException {

        // Create detector
        final List<BodyShape> maskingBodies = new ArrayList<>();
        maskingBodies.add(CelestialBodyFactory.getEarth().getShape());

        final LineMaskingDetector detector = new LineMaskingDetector(CelestialBodyFactory.getEarth(),
            SignalPropagationRole.EMITTER, CelestialBodyFactory.getMoon(), maskingBodies, EventDetector.INCREASING,
            10.0, 1e-5, Action.STOP, Action.STOP, true, true);

        // Copy detector
        final LineMaskingDetector copy = detector.copy();

        // Compare equals
        Assert.assertEquals(detector.getMainElement(), copy.getMainElement());
        Assert.assertEquals(detector.getMaskingBodies(), copy.getMaskingBodies());
        Assert.assertEquals(detector.getLinkTypeHandler().getMainRole(), copy.getLinkTypeHandler().getMainRole());
        Assert.assertEquals(detector.getLinkTypeHandler().getOtherElement(),
            copy.getLinkTypeHandler().getOtherElement());
        Assert.assertEquals(detector.getSlopeSelection(), copy.getSlopeSelection(), 0.);
        Assert.assertEquals(detector.getMaxCheckInterval(), copy.getMaxCheckInterval(), 0.);
        Assert.assertEquals(detector.getEpsilonSignalPropagation(), copy.getEpsilonSignalPropagation(), 0.);
        Assert.assertEquals(detector.getActionAtEntry(), copy.getActionAtEntry());
        Assert.assertEquals(detector.getActionAtEntry(), copy.getActionAtEntry());
        Assert.assertEquals(detector.isRemoveAtEntry(), copy.isRemoveAtEntry());
        Assert.assertEquals(detector.isRemoveAtExit(), copy.isRemoveAtExit());

        // Modify copy
        copy.setMaxCheckInterval(1.0);
        copy.setEpsilonSignalPropagation(20.0);

        // Compare not equals
        Assert.assertNotEquals(detector.getMaxCheckInterval(), copy.getMaxCheckInterval());
        Assert.assertNotEquals(detector.getEpsilonSignalPropagation(), copy.getEpsilonSignalPropagation());
    }

    /**
     * @throws PatriusException
     *
     * @testType UT
     * @description This tests verifies that an error is raised if the list of masking objects is empty
     *
     * @input input parameters for the object LineMaskingDetector (not relevant)
     *
     * @output the expected exception
     * @testPassCriteria the expected exception is thrown
     *
     * @referenceVersion 4.16
     * @nonregressionVersion 4.16
     */
    @Test
    public void testEmptyMaskingObjectsException() throws PatriusException {
        try {
            new LineMaskingDetector(CelestialBodyFactory.getEarth(), SignalPropagationRole.EMITTER,
                CelestialBodyFactory.getMoon(), new ArrayList<>());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertEquals(PatriusMessages.EMPTY_MASKING_BODIES_LIST.getSourceString(), e.getMessage());
        }
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * @description This tests verifies the masking events detected using the SatToSatMutualVisibilityDetector
     *              (no masking satellite parts) correspond to those obtained using the new LineMaskingDetector for two
     *              satellites.<br>
     *              The masking body is the Earth.
     *              <p>
     *              Two assemblies corresponding to the two satellites are created. The FOV is 180º for both.<br>
     *              Satellite 1 is in a much higher orbit than sat2 (10x the SMA). So sat2 turns much faster, hence the
     *              Earth must at some point be masking the line of sight.
     *              </p>
     * 
     * @input input parameters for the objects LineMaskingDetector and SatToSatMutualVisibilityDetector
     *
     * @output the detected masking events for both detectors
     * @testPassCriteria the detected masking events match
     *
     * @referenceVersion 4.16
     * @nonregressionVersion 4.16
     */
    @Test
    public void testSatToSatAgainstLineMasking() throws PatriusException {

        // Build bodies
        final String mainBody1 = "mainBody1";
        final Assembly body1 = buildAssembly(mainBody1);

        final String mainBody2 = "mainBody2";
        final Assembly body2 = buildAssembly(mainBody2);

        // Create sensor models
        final SensorModel primarySpacecraftSensorModel = new SensorModel(body1, mainBody1);
        final SensorModel secondarySpacecraftSensorModel = new SensorModel(body2, mainBody2);

        // targets settings
        secondarySpacecraftSensorModel.setMainTarget(primarySpacecraftSensorModel, new ConstantRadiusProvider(10.));
        primarySpacecraftSensorModel.setMainTarget(secondarySpacecraftSensorModel, new ConstantRadiusProvider(10.));

        // Add masking bodies
        primarySpacecraftSensorModel.addMaskingCelestialBody(CelestialBodyFactory.getEarth().getShape());

        // Create propagators
        final CelestialBodyFrame referenceFrame = FramesFactory.getGCRF();
        final double sma = Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS + 770e3;
        final Orbit orbit1 = new EquatorialOrbit(10. * sma, 0, 0, 0, 0, 0, PositionAngle.TRUE, referenceFrame,
            this.iniDate, Constants.WGS84_EARTH_MU);

        // Orbit for second satellite
        final Orbit orbit2 = new EquatorialOrbit(sma, 0, 0, 0, 0, MathUtils.HALF_PI, PositionAngle.TRUE, referenceFrame,
            this.iniDate, Constants.WGS84_EARTH_MU);

        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);

        final double maxCheck = 1.;
        final double threshold = 1e-8;

        final List<BodyShape> maskingBodies = new ArrayList<>();
        maskingBodies.add(CelestialBodyFactory.getEarth().getShape());

        final double period = MathUtils.TWO_PI * MathLib.sqrt(sma * sma * sma / Constants.WGS84_EARTH_MU);
        final AbsoluteDate endDate = this.iniDate.shiftedBy(2. * period);

        // SatToSatMutualVisibilityDetector always use INCREASING_DECREASING slope, so we can't evaluate all modes

        /*
         * Non-null main element + Keplerian propagators
         */

        // #Case 1: SatToSatLinkType.MAIN_TO_SECONDARY / SignalPropagationRole.EMITTER / INSTANTANEOUS
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.MAIN_TO_SECONDARY, SignalPropagationRole.EMITTER,
            PropagationDelayType.INSTANTANEOUS, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, false, true);

        // #Case 2: SatToSatLinkType.MAIN_TO_SECONDARY / SignalPropagationRole.EMITTER / LIGHT_SPEED
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.MAIN_TO_SECONDARY, SignalPropagationRole.EMITTER,
            PropagationDelayType.LIGHT_SPEED, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, false, true);

        // #Case 3: SatToSatLinkType.SECONDARY_TO_MAIN / SignalPropagationRole.RECEIVER / INSTANTANEOUS
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.SECONDARY_TO_MAIN, SignalPropagationRole.RECEIVER,
            PropagationDelayType.INSTANTANEOUS, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, false, true);

        // #Case 4: SatToSatLinkType.SECONDARY_TO_MAIN / SignalPropagationRole.RECEIVER / LIGHT_SPEED
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.SECONDARY_TO_MAIN, SignalPropagationRole.RECEIVER,
            PropagationDelayType.LIGHT_SPEED, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, false, true);

        /*
         * Null main element + Keplerian propagators
         */

        // #Case 5: SatToSatLinkType.MAIN_TO_SECONDARY / SignalPropagationRole.EMITTER / INSTANTANEOUS
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.MAIN_TO_SECONDARY, SignalPropagationRole.EMITTER,
            PropagationDelayType.INSTANTANEOUS, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, true, true);

        // #Case 6: SatToSatLinkType.MAIN_TO_SECONDARY / SignalPropagationRole.EMITTER / LIGHT_SPEED
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.MAIN_TO_SECONDARY, SignalPropagationRole.EMITTER,
            PropagationDelayType.LIGHT_SPEED, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, true, true);

        // #Case 7: SatToSatLinkType.SECONDARY_TO_MAIN / SignalPropagationRole.RECEIVER / INSTANTANEOUS
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.SECONDARY_TO_MAIN, SignalPropagationRole.RECEIVER,
            PropagationDelayType.INSTANTANEOUS, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, true, true);

        // #Case 8: SatToSatLinkType.SECONDARY_TO_MAIN / SignalPropagationRole.RECEIVER / LIGHT_SPEED
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.SECONDARY_TO_MAIN, SignalPropagationRole.RECEIVER,
            PropagationDelayType.LIGHT_SPEED, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, true, true);

        /*
         * Non-null main element + Numerical propagators
         */

        // #Case 9: SatToSatLinkType.MAIN_TO_SECONDARY / SignalPropagationRole.EMITTER / INSTANTANEOUS
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.MAIN_TO_SECONDARY, SignalPropagationRole.EMITTER,
            PropagationDelayType.INSTANTANEOUS, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, false, false);

        // #Case 10: SatToSatLinkType.MAIN_TO_SECONDARY / SignalPropagationRole.EMITTER / LIGHT_SPEED
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.MAIN_TO_SECONDARY, SignalPropagationRole.EMITTER,
            PropagationDelayType.LIGHT_SPEED, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, false, false);

        // #Case 11: SatToSatLinkType.SECONDARY_TO_MAIN / SignalPropagationRole.RECEIVER / INSTANTANEOUS
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.SECONDARY_TO_MAIN, SignalPropagationRole.RECEIVER,
            PropagationDelayType.INSTANTANEOUS, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, false, false);

        // #Case 12: SatToSatLinkType.SECONDARY_TO_MAIN / SignalPropagationRole.RECEIVER / LIGHT_SPEED
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.SECONDARY_TO_MAIN, SignalPropagationRole.RECEIVER,
            PropagationDelayType.LIGHT_SPEED, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, false, false);

        /*
         * Null main element + Numerical propagators
         */

        // #Case 13: SatToSatLinkType.MAIN_TO_SECONDARY / SignalPropagationRole.EMITTER / INSTANTANEOUS
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.MAIN_TO_SECONDARY, SignalPropagationRole.EMITTER,
            PropagationDelayType.INSTANTANEOUS, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, true, false);

        // #Case 14: SatToSatLinkType.MAIN_TO_SECONDARY / SignalPropagationRole.EMITTER / LIGHT_SPEED
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.MAIN_TO_SECONDARY, SignalPropagationRole.EMITTER,
            PropagationDelayType.LIGHT_SPEED, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, true, false);

        // #Case 15: SatToSatLinkType.SECONDARY_TO_MAIN / SignalPropagationRole.RECEIVER / INSTANTANEOUS
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.SECONDARY_TO_MAIN, SignalPropagationRole.RECEIVER,
            PropagationDelayType.INSTANTANEOUS, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, true, false);

        // #Case 16: SatToSatLinkType.SECONDARY_TO_MAIN / SignalPropagationRole.RECEIVER / LIGHT_SPEED
        LineMaskingDetectorTest.evaluateSatToSat(SatToSatLinkType.SECONDARY_TO_MAIN, SignalPropagationRole.RECEIVER,
            PropagationDelayType.LIGHT_SPEED, orbit1, orbit2, attitudeProv, primarySpacecraftSensorModel,
            secondarySpacecraftSensorModel, maxCheck, threshold, referenceFrame, maskingBodies, endDate, true, false);

        // Cover the isDirectionOcculted method
        final LineMaskingDetector detector =
            new LineMaskingDetector(orbit1, SignalPropagationRole.EMITTER, orbit2, maskingBodies);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit1, attitudeProv);

        // First expected occultation at 2005-09-07T00:06:46.720
        Assert.assertFalse(detector.isDirectionOcculted(propagator, new AbsoluteDate("2005-09-07T00:06:46.719")));
        Assert.assertTrue(detector.isDirectionOcculted(propagator, new AbsoluteDate("2005-09-07T00:06:46.720")));
        Assert.assertTrue(detector.isDirectionOcculted(propagator, new AbsoluteDate("2005-09-07T00:06:46.721")));
    }

    /**
     * Code mutualization.
     * 
     * @param linkType
     * @param role
     * @param propagationDelayType
     * @param orbit1
     * @param orbit2
     * @param attitudeProv
     * @param primarySpacecraftSensorModel
     * @param secondarySpacecraftSensorModel
     * @param maxCheck
     * @param threshold
     * @param referenceFrame
     * @param maskingBodies
     * @param endDate
     * @param nullMainElem
     * @param keplerian
     * @throws PatriusException
     */
    private static void evaluateSatToSat(final SatToSatLinkType linkType, final SignalPropagationRole role,
                                         final PropagationDelayType propagationDelayType,
                                         final Orbit orbit1, final Orbit orbit2, final AttitudeProvider attitudeProv,
                                         final SensorModel primarySpacecraftSensorModel,
                                         final SensorModel secondarySpacecraftSensorModel,
                                         final double maxCheck, final double threshold,
                                         final CelestialBodyFrame referenceFrame,
                                         final List<BodyShape> maskingBodies, final AbsoluteDate endDate,
                                         final boolean nullMainElem, final boolean keplerian)
        throws PatriusException {

        final Propagator mainPropagator;
        final Propagator secondaryPropagator;
        if (keplerian) {
            // Build the 2 propagators as KeplerianPropagator
            mainPropagator = new KeplerianPropagator(orbit1, attitudeProv);
            secondaryPropagator = new KeplerianPropagator(orbit2, attitudeProv);
        } else {
            // Build the 2 propagators as NumericalPropagator
            final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
            final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
            final AdaptiveStepsizeIntegrator integrator1 = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
            final AdaptiveStepsizeIntegrator integrator2 = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);

            mainPropagator = new NumericalPropagator(integrator1);
            mainPropagator.resetInitialState(new SpacecraftState(orbit1, attitudeProv.getAttitude(orbit1)));
            ((NumericalPropagator) mainPropagator).addForceModel(
                new DirectBodyAttraction(new NewtonianGravityModel(mainPropagator.getInitialState().getMu())));
            mainPropagator.setAttitudeProvider(attitudeProv);

            secondaryPropagator = new NumericalPropagator(integrator2);
            secondaryPropagator.resetInitialState(new SpacecraftState(orbit2, attitudeProv.getAttitude(orbit2)));
            ((NumericalPropagator) secondaryPropagator).addForceModel(
                new DirectBodyAttraction(new NewtonianGravityModel(secondaryPropagator.getInitialState().getMu())));
            secondaryPropagator.setAttitudeProvider(attitudeProv);
        }

        // Define if the mainElement object should be described by orbit1 or null (to evaluate both cases)
        final PVCoordinatesProvider mainElement;
        if (nullMainElem) {
            mainElement = null;
        } else {
            mainElement = orbit1;
        }

        // Build SatToSatMutualVisibility detector
        final SatToSatMutualVisibilityDetector sat2satDetector = new SatToSatMutualVisibilityDetector(
            primarySpacecraftSensorModel, secondarySpacecraftSensorModel, secondaryPropagator, true, maxCheck,
            threshold, Action.CONTINUE, Action.CONTINUE, false, false, linkType);
        sat2satDetector.setPropagationDelayType(propagationDelayType, referenceFrame);

        // Build LineMaskingDetector detector
        final LineMaskingDetector lineMaskDetector = new LineMaskingDetector(mainElement, role, orbit2, maskingBodies,
            maxCheck, threshold, Action.CONTINUE, Action.CONTINUE, false, false);
        lineMaskDetector.setPropagationDelayType(propagationDelayType, referenceFrame);

        // Wrap event detectors
        final SignalPropagationWrapperDetector wrapperSat2Sat = new SignalPropagationWrapperDetector(sat2satDetector);
        final SignalPropagationWrapperDetector wrapperLineMask = new SignalPropagationWrapperDetector(lineMaskDetector);

        // Add them in the propagator, then propagate
        mainPropagator.addEventDetector(wrapperSat2Sat);
        mainPropagator.addEventDetector(wrapperLineMask);

        mainPropagator.propagate(endDate);

        // Check some events have been detected and evaluate the two detectors have detected the same number of events
        final int nbEvents = wrapperSat2Sat.getNBOccurredEvents();
        Assert.assertTrue(nbEvents > 0);
        Assert.assertEquals(nbEvents, wrapperLineMask.getNBOccurredEvents());

        // Extract the events dates (emitter / receiver dates) for both detectors and check they are identical
        final List<AbsoluteDate> sat2SatEmitterDates = wrapperSat2Sat.getEmitterDatesList();
        final List<AbsoluteDate> lineMaskEmitterDates = wrapperLineMask.getEmitterDatesList();

        final List<AbsoluteDate> sat2SatReceiverDates = wrapperSat2Sat.getReceiverDatesList();
        final List<AbsoluteDate> lineMaskReceiverDates = wrapperLineMask.getReceiverDatesList();

        for (int i = 0; i < nbEvents; i++) {
            Assert.assertTrue(sat2SatEmitterDates.get(i).durationFrom(lineMaskEmitterDates.get(i)) < threshold);
            Assert.assertTrue(sat2SatReceiverDates.get(i).durationFrom(lineMaskReceiverDates.get(i)) < threshold);
        }
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * @description This tests verifies the masking events detected using the StationToSatMutualVisibilityDetector
     *              (no masking parts) correspond to those obtained using the new LineMaskingDetector for a satellite
     *              and a station.<br>
     *              The masking body is the Earth.
     *              <p>
     *              The satellite is in LEO orbit. FoV for the satellite and the station 180º. The earth should mask the
     *              satellite.
     *              </p>
     * 
     * @input input parameters for the object LineMaskingDetector and StationToSatMutualVisibilityDetector
     *
     * @output the detected masking events for both detectors
     * @testPassCriteria the detected masking events match
     *
     * @referenceVersion 4.16
     * @nonregressionVersion 4.16
     */
    @Test
    @Ignore // Investigate why this test isn't valid
    public void testStationToSatAgainstLineMasking() throws PatriusException {
        Utils.clear();

        // Build antenna
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF());
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), MathLib.toRadians(10),
            MathLib.toRadians(20), 10., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "Station");
        final IFieldOfView stationField = new OmnidirectionalField("");
        final GeometricStationAntenna station = new GeometricStationAntenna(topoFrame, stationField);

        // Build body
        final String mainBody = "mainBody";
        final Assembly body = buildAssembly(mainBody);
        final SensorModel sensorModel = new SensorModel(body, mainBody);
        sensorModel.setMainTarget(station, new ConstantRadiusProvider(10.));
        sensorModel.addMaskingCelestialBody(CelestialBodyFactory.getEarth().getShape());

        // Create propagator
        final CelestialBodyFrame referenceFrame = FramesFactory.getGCRF();

        final Orbit orbit = new KeplerianOrbit(Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 1000e3, 0, 0, 0, 0, 0,
            PositionAngle.TRUE, referenceFrame, this.iniDate, Constants.WGS84_EARTH_MU);

        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);

        final double maxCheck = 1.;
        final double threshold = 1e-12;

        final List<BodyShape> maskingBodies = new ArrayList<>();
        maskingBodies.add(CelestialBodyFactory.getEarth().getShape());

        final double period = Constants.JULIAN_DAY * 0.25;
        final AbsoluteDate endDate = this.iniDate.shiftedBy(period);

        // StationToSatMutualVisibilityDetector always use INCREASING_DECREASING slope, so we can't evaluate all modes
        // #Case 1: LinkType.DOWNLINK / SignalPropagationRole.EMITTER / INSTANTANEOUS
        LineMaskingDetectorTest.evaluateStationToSat(LinkType.DOWNLINK, SignalPropagationRole.EMITTER,
            PropagationDelayType.INSTANTANEOUS, orbit, attitudeProv, sensorModel, station, maxCheck, threshold,
            referenceFrame, maskingBodies, endDate);

        // #Case 2: LinkType.DOWNLINK / SignalPropagationRole.EMITTER / LIGHT_SPEED
        LineMaskingDetectorTest.evaluateStationToSat(LinkType.DOWNLINK, SignalPropagationRole.EMITTER,
            PropagationDelayType.LIGHT_SPEED, orbit, attitudeProv, sensorModel, station, maxCheck, threshold,
            referenceFrame, maskingBodies, endDate);

        // #Case 3: LinkType.UPLINK / SignalPropagationRole.RECEIVER / INSTANTANEOUS
        LineMaskingDetectorTest.evaluateStationToSat(LinkType.UPLINK, SignalPropagationRole.RECEIVER,
            PropagationDelayType.INSTANTANEOUS, orbit, attitudeProv, sensorModel, station, maxCheck, threshold,
            referenceFrame, maskingBodies, endDate);

        // #Case 4: LinkType.UPLINK / SignalPropagationRole.RECEIVER / LIGHT_SPEED
        LineMaskingDetectorTest.evaluateStationToSat(LinkType.UPLINK, SignalPropagationRole.RECEIVER,
            PropagationDelayType.LIGHT_SPEED, orbit, attitudeProv, sensorModel, station, maxCheck, threshold,
            referenceFrame, maskingBodies, endDate);
    }

    /**
     * Code mutualization.
     * 
     * @param linkType
     * @param role
     * @param propagationDelayType
     * @param orbit
     * @param attitudeProv
     * @param sensorModel
     * @param station
     * @param maxCheck
     * @param threshold
     * @param referenceFrame
     * @param maskingBodies
     * @param endDate
     * @throws PatriusException
     */
    private static void evaluateStationToSat(final LinkType linkType, final SignalPropagationRole role,
                                             final PropagationDelayType propagationDelayType,
                                             final Orbit orbit, final AttitudeProvider attitudeProv,
                                             final SensorModel sensorModel, final GeometricStationAntenna station,
                                             final double maxCheck, final double threshold,
                                             final CelestialBodyFrame referenceFrame,
                                             final List<BodyShape> maskingBodies,
                                             final AbsoluteDate endDate)
        throws PatriusException {

        final Propagator propagator = new KeplerianPropagator(orbit, attitudeProv);

        // Build StationToSatMutualVisibility detector
        final StationToSatMutualVisibilityDetector sta2satDetector =
            new StationToSatMutualVisibilityDetector(sensorModel, station, null, true,
                maxCheck, threshold, Action.CONTINUE, Action.CONTINUE, false, false, linkType);
        sta2satDetector.setPropagationDelayType(propagationDelayType, referenceFrame);

        // Build LineMasking detector
        final LineMaskingDetector lineMaskDetector = new LineMaskingDetector(orbit, role,
            station, maskingBodies, maxCheck, threshold, Action.CONTINUE, Action.CONTINUE, false, false);
        lineMaskDetector.setPropagationDelayType(propagationDelayType, referenceFrame);

        // Wrap event detectors
        final SignalPropagationWrapperDetector wrapperSta2Sat = new SignalPropagationWrapperDetector(sta2satDetector);
        final SignalPropagationWrapperDetector wrapperLineMask = new SignalPropagationWrapperDetector(lineMaskDetector);

        // Add them in the propagator, then propagate
        propagator.addEventDetector(wrapperLineMask);
        propagator.addEventDetector(wrapperSta2Sat);

        propagator.propagate(endDate);

        // Check some events have been detected + evaluate the two detectors have detected the same number of events
        final int nbEvents = wrapperSta2Sat.getNBOccurredEvents();
        Assert.assertTrue(nbEvents > 0);
        Assert.assertEquals(nbEvents, wrapperLineMask.getNBOccurredEvents());

        // Extract the events dates (emitter / receiver dates) for both detectors and check they are identical
        final List<AbsoluteDate> sta2SatEmitterDates = wrapperSta2Sat.getEmitterDatesList();
        final List<AbsoluteDate> lineMaskEmitterDates = wrapperLineMask.getEmitterDatesList();

        final List<AbsoluteDate> sta2SatReceiverDates = wrapperSta2Sat.getReceiverDatesList();
        final List<AbsoluteDate> lineMaskReceiverDates = wrapperLineMask.getReceiverDatesList();

        for (int i = 0; i < nbEvents; i++) {
            // System.out.println(sta2SatEmitterDates.get(i).durationFrom(lineMaskEmitterDates.get(i))
            // + "\t" + sta2SatReceiverDates.get(i).durationFrom(lineMaskReceiverDates.get(i))); // TODO
            Assert.assertTrue(sta2SatEmitterDates.get(i).durationFrom(lineMaskEmitterDates.get(i)) < threshold);
            Assert.assertTrue(sta2SatReceiverDates.get(i).durationFrom(lineMaskReceiverDates.get(i)) < threshold);
        }
    }

    /**
     * Internal method to build the test assembly.
     * 
     * @param bodyName
     *        body name
     * @return the test assembly
     */
    private Assembly buildAssembly(final String bodyName) {

        // building the SECONDARY assembly
        // =======================
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensors
        // main part field
        final Vector3D mainFieldDirection = Vector3D.PLUS_I;
        final IFieldOfView mainField = new OmnidirectionalField("");

        // main part sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);

        // assembly building
        try {
            // add main part
            builder.addMainPart(bodyName);

            // add sensors
            builder.addProperty(sensorProperty, bodyName);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame =
                new UpdatableFrame(FramesFactory.getEME2000(), Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.fail();
        }
        return builder.returnAssembly();
    }
}
