/**
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
 *
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.SensorProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Unit test for {@link ExtremaSightAxisDetector}
 * 
 * @author chabaudp
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class ExtremaSightAxisDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validation of SightAxisTargetBodyDetector
         * 
         * @featureDescription Validate the detector of extrema in the distance between a target on
         *                     the central body and the intersection point of the sight axis of a
         *                     part
         * 
         * @coveredRequirements DV-EVT_130
         */
        SIGHTAXIS_TARGET_EXTREMA
    }

    /** EPSILON E-2 s because we are in unit test and not validating precision */
    private static final double EPSILON_DURATION = 1E-2;

    /** different targets for the different test cases */
    private Map<String, PVCoordinatesProvider> targetMap;

    /** A sight view axis to use directly from origin of attitude frame or from a sensor frame */
    private Vector3D sightViewAxis;

    /** different orbit type for the different test cases */
    private Map<String, KeplerianOrbit> orbitMap;

    /** an orbit */
    private KeplerianOrbit orbitRetro;

    /** attitude */
    private AttitudeProvider attitude;

    /** initial date */
    private AbsoluteDate initDate;

    /** central body shape */
    private BodyShape earth;

    /** a vehicle */
    private Assembly vehicle;

    /**
     * Setup configuration frame, orekit data, and all common parameter of the tests
     * 
     * @throws PatriusException should not happen
     */
    @Before
    public final void setup() throws PatriusException {
        // Initialise configuration without EOP to avoid using external data file
        Utils.setDataRoot("bent");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Initialise earth shape
        this.earth = new OneAxisEllipsoid(6000000.0, 0.0, FramesFactory.getTIRF());

        // Define a list of targets
        final PVCoordinatesProvider target = new TopocentricFrame(this.earth,
            new GeodeticPoint(0.0, 0.0, 0.0), "target");
        this.targetMap = new HashMap<String, PVCoordinatesProvider>();
        this.targetMap.put("onBodyZero", target);

        // Define a list of orbit to test : one retrograde, one prograde, one gto
        this.initDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());

        // Orbit retrograde
        this.orbitRetro = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(98.0), 0, 0,
            FastMath.PI / 2, PositionAngle.TRUE, FramesFactory.getGCRF(), this.initDate,
            Constants.EGM96_EARTH_MU);

        // Orbit prograde
        final KeplerianOrbit orbitPrograde = new KeplerianOrbit(8000000, 0.001,
            MathLib.toRadians(60.0), 0, 0, FastMath.PI / 2, PositionAngle.TRUE,
            FramesFactory.getGCRF(), this.initDate, Constants.EGM96_EARTH_MU);

        // Orbit GTO
        final KeplerianOrbit orbitGto = new KeplerianOrbit(30000e3, 0.657,
            MathLib.toRadians(10.0), MathLib.toRadians(199.0), 0.0, 0.0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), this.initDate, Constants.EGM96_EARTH_MU);

        this.orbitMap = new HashMap<String, KeplerianOrbit>();
        this.orbitMap.put("Retro", this.orbitRetro);
        this.orbitMap.put("Pro", orbitPrograde);
        this.orbitMap.put("Gto", orbitGto);

        // Simple attitude provider : Pointing body frame origin
        this.attitude = new BodyCenterPointing(this.earth.getBodyFrame());

        // define a vehicle with a sensor having a sight view axis
        // define the translation to place the payload frame
        final Vector3D payloadTranslation = Vector3D.PLUS_I.scalarMultiply(3);
        final Rotation payloadRotation = new Rotation(Vector3D.PLUS_I, FastMath.PI / 2);

        // A sight view axis to use directly from origin of attitude frame or from a sensor frame
        this.sightViewAxis = Vector3D.PLUS_I;
        // sensor property creation with sightViewAxis
        final SensorProperty sensorProperty = new SensorProperty(this.sightViewAxis);

        // Vehicle building
        final AssemblyBuilder builder = new AssemblyBuilder();

        try {
            // add main part
            builder.addMainPart("mainBody");
            builder.addPart("payload", "mainBody", payloadTranslation, payloadRotation);
            builder.addPart("thruster", "mainBody", Vector3D.MINUS_I.scalarMultiply(3),
                Rotation.IDENTITY);

            // add sensor
            builder.addProperty(sensorProperty, "payload");

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame = new UpdatableFrame(FramesFactory.getGCRF(),
                Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        this.vehicle = builder.returnAssembly();

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SIGHTAXIS_TARGET_EXTREMA}
     * 
     * @testedMethod {@link ExtremaSightAxisDetector#g(SpacecraftState)}
     * 
     * @description Test case : orbit retrograde, attitude center body pointing, keplerian
     *              propagator, sight axis (1,0,0) in attitude frame, target geopoint(0,0,0) on the
     *              central body. The value detected have been verified to be local minimum between
     *              sight axis and target.
     * 
     * @input a Vector3D sight axis
     * 
     * @output dates of the detected events are the expected ones.
     * 
     * @testPassCriteria difference with the reference duration less than {@link EPSILON_DURATION}.
     *                   Epsilon Justification : unitary test doesn't valid precise detection and
     *                   susceptible to move slightly with evolution of the products.
     * 
     * @throws PatriusException
     */

    @Test
    public final void testG() throws PatriusException {

        // Test case : orbit retro, target on body geopoint zero
        final KeplerianOrbit orbit = this.orbitMap.get("Retro");
        final PVCoordinatesProvider target = this.targetMap.get("onBodyZero");

        // results : date and angle when event is detected
        final ArrayList<double[]> results = new ArrayList<double[]>();

        // reference results
        final double[] referenceResults = { 130.62969417744887, 6495.918899782327,
            11939.26838263744, 18514.00589758429 };

        final double period = orbit.getKeplerianPeriod();

        // propagator
        final Propagator propagator = new KeplerianPropagator(orbit, this.attitude);

        // Step handler to track angle evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(target, this.sightViewAxis, this.initDate);

        propagator.setMasterMode(10, angleTracking);

        // build the detector and the logger
        final EventsLogger logger = new EventsLogger();
        final ExtremaSightAxisDetector curentDetector = new ExtremaSightAxisDetector(
            ExtremaSightAxisDetector.MIN, target, this.sightViewAxis){
            /** serial UID */
            private static final long serialVersionUID = 4259523968924693381L;

            /** Overrided method eventOccured to continue */
            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                return Action.CONTINUE;
            }
        };

        propagator.addEventDetector(logger.monitorDetector(curentDetector));

        // Propagation
        propagator.propagate(this.initDate.shiftedBy(3 * period));

        // Get information about all detected events
        for (final LoggedEvent event : logger.getLoggedEvents()) {

            // Detected state
            final SpacecraftState sstate = event.getState();

            // combination of the position and the attitude
            final Transform rotation = new Transform(AbsoluteDate.J2000_EPOCH, sstate.getAttitude()
                .getOrientation());
            final Transform translation = new Transform(AbsoluteDate.J2000_EPOCH, sstate.getOrbit()
                .getPVCoordinates().negate());
            final Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, translation,
                rotation);

            // Angle between the sight axis and the target
            final double angle = Vector3D.angle(
                transform.transformPVCoordinates(
                    target.getPVCoordinates(sstate.getDate(), sstate.getFrame()))
                    .getPosition(), this.sightViewAxis);

            final double[] currentResult = { sstate.getDate().durationFrom(this.initDate), angle };

            results.add(currentResult);

        }

        // Unitary validation based on SVS value
        int i = 0;
        for (final double reference : referenceResults) {
            if (i > results.size() - 1) {
                Assert.fail("Doesn't detect enough event");
            }
            Assert.assertEquals(reference, results.get(i)[0], EPSILON_DURATION);
            i++;
        }
        if (i < results.size()) {
            Assert.fail("Detects too many events");
        }

        // Write results from step handler and detector and logger to plot in scilab for SVS
        // ResultsFileWriter.writeResultsToPlot("sensor", "angleTracking.txt",
        // angleTracking.results);
        // ResultsFileWriter.writeResultsToPlot("sensor", "switchingFunction.txt",
        // curentDetector.results);
        // ResultsFileWriter.writeResultsToPlot("sensor", "detectedEvents.txt", results);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SIGHTAXIS_TARGET_EXTREMA}
     * 
     * @testedMethod {@link ExtremaSightAxisDetector#g(SpacecraftState)}
     * 
     * @description Test case : orbit prograde, attitude center body pointing, numerical propagator,
     *              sight axis of a sensor in an assembly, target on a GTO orbit. The value detected
     *              have been verified to be local maximum of angle between sight axis and target.
     * 
     * @input an assembly and a part name
     * 
     * @output dates of the detected events are the expected ones.
     * 
     * @testPassCriteria difference with the reference duration less than {@link EPSILON_DURATION}.
     *                   Epsilon Justification : unitary test doesn't valid precise detection and
     *                   susceptible to move slightly with evolution of the products.
     * 
     * @throws PatriusException
     */

    @Test
    public final void testGwithAssembly() throws PatriusException {

        // results : date and angle when event is detected
        final ArrayList<double[]> results = new ArrayList<double[]>();

        // reference results
        final double[] referenceResults = { 4833.652875180301, 10690.967819403138,
            13536.125648322095, 18277.871833244022, 21267.120689811858 };

        // Test case : orbit retrograde, target based on orbit gto
        final KeplerianOrbit orbit = this.orbitMap.get("Pro");
        final PVCoordinatesProvider target = this.orbitMap.get("Gto");

        final double period = orbit.getKeplerianPeriod();

        // Numerical propagator
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60.,
            absTOL, relTOL);
        final Propagator propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(new SpacecraftState(orbit));
        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY));

        // Step handler to track angle evolution during propagation
        final MyStepHandler angleTracking = new MyStepHandler(target, this.vehicle, "payload", this.initDate);
        propagator.setMasterMode(10, angleTracking);

        // build the different detector
        final EventsLogger logger = new EventsLogger();
        final ExtremaSightAxisDetector curentDetector = new ExtremaSightAxisDetector(
            ExtremaSightAxisDetector.MAX, target, this.vehicle, "payload",
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
            Action.CONTINUE);

        propagator.addEventDetector(logger.monitorDetector(curentDetector));

        // Propagation
        propagator.propagate(this.initDate.shiftedBy(3 * period));

        // Get information about all detected events
        for (final LoggedEvent event : logger.getLoggedEvents()) {

            // Update spacecraft frame with detected state
            final SpacecraftState sstate = event.getState();
            this.vehicle.initMainPartFrame(sstate);

            // get the sensor : usually the payload
            final IPart payload = this.vehicle.getPart("payload");

            // get the payload frame, sensor properties and sightAxis in this frame
            final Frame payloadFrame = payload.getFrame();
            final SensorProperty payloadSensorProperty = (SensorProperty) (payload
                .getProperty(PropertyType.SENSOR));
            final Vector3D sensorSightViewAxis = payloadSensorProperty.getInSightAxis();

            // Angle between the sight axis and the target
            final double angle = Vector3D.angle(
                target.getPVCoordinates(sstate.getDate(), payloadFrame).getPosition(),
                sensorSightViewAxis);

            final double[] currentResult = { sstate.getDate().durationFrom(this.initDate), angle };

            results.add(currentResult);

        }

        // Unitary validation based on SVS value
        int i = 0;
        for (final double reference : referenceResults) {
            if (i > results.size() - 1) {
                Assert.fail("Doesn't detect enough event");
            }
            Assert.assertEquals(reference, results.get(i)[0], EPSILON_DURATION);
            i++;
        }
        if (i < results.size()) {
            Assert.fail("Detects too many events");
        }

        // Write results from step handler and detector and logger to plot in scilab for SVS
        // ResultsFileWriter.writeResultsToPlot("sensor", "angTrackWithAssembly.txt",
        // angleTracking.results);
        // ResultsFileWriter.writeResultsToPlot("sensor", "switchFuncAssembly.txt",
        // curentDetector.results);
        // ResultsFileWriter.writeResultsToPlot("sensor", "detectedEventsAssembly.txt", results);

        /*
         * Test for coverage
         */
        final ExtremaSightAxisDetector detector1 = new ExtremaSightAxisDetector(
            ExtremaSightAxisDetector.MIN, target, this.vehicle, "payload",
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
            Action.RESET_DERIVATIVES);
        Assert.assertEquals(ExtremaSightAxisDetector.MIN, detector1.getSlopeSelection());
        final SpacecraftState s = new SpacecraftState(this.orbitRetro);
        Assert.assertEquals(Action.RESET_DERIVATIVES, detector1.eventOccurred(s, true, true));
        Assert.assertEquals(Action.RESET_DERIVATIVES, detector1.eventOccurred(s, true, false));
        Assert.assertEquals(Action.RESET_DERIVATIVES, detector1.eventOccurred(s, false, true));
        Assert.assertEquals(Action.RESET_DERIVATIVES, detector1.eventOccurred(s, false, false));

        final ExtremaSightAxisDetector detector2 = new ExtremaSightAxisDetector(
            ExtremaSightAxisDetector.MIN_MAX, target, this.vehicle, "payload",
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
            Action.CONTINUE);
        Assert.assertEquals(ExtremaSightAxisDetector.MIN_MAX, detector2.getSlopeSelection());
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(s, true, true));
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(s, true, false));
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(s, false, true));
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(s, false, false));

        final ExtremaSightAxisDetector detector3 = new ExtremaSightAxisDetector(
            ExtremaSightAxisDetector.MAX, target, this.vehicle, "payload");
        Assert.assertEquals(ExtremaSightAxisDetector.MAX, detector3.getSlopeSelection());
        Assert.assertEquals(Action.STOP, detector3.eventOccurred(s, true, true));
        Assert.assertEquals(Action.STOP, detector3.eventOccurred(s, true, false));
        Assert.assertEquals(Action.STOP, detector3.eventOccurred(s, false, true));
        Assert.assertEquals(Action.STOP, detector3.eventOccurred(s, false, false));

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SIGHTAXIS_TARGET_EXTREMA}
     * 
     * @testedMethod {@link ExtremaSightAxisDetector#g(SpacecraftState)}
     * @testedMethod {@link ExtremaSightAxisDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Using the first test case without overriding eventOccured method. The spacecraft
     *              state should be at the first date of this test.
     * 
     * @input a Vector3D sight axis
     * 
     * @output spacecraft state at the first detected event date
     * 
     * @testPassCriteria difference with the reference duration less than {@link EPSILON_DURATION}.
     *                   Epsilon Justification : unitary test doesn't valid precise detection and
     *                   susceptible to move slightly with evolution of the products.
     * 
     * @throws PatriusException should not happens
     */

    @Test
    public final void testEventOccured() throws PatriusException {

        // Test case : orbit retro, target on body geopoint zero
        final KeplerianOrbit orbit = this.orbitMap.get("Retro");
        final PVCoordinatesProvider target = this.targetMap.get("onBodyZero");

        new ArrayList<double[]>();

        // reference results
        final double reference = 130.62969417744887;
        final double period = orbit.getKeplerianPeriod();

        // final date
        final AbsoluteDate finalDate = this.initDate.shiftedBy(3 * period);

        // propagator
        Propagator propagator = new KeplerianPropagator(orbit, this.attitude);

        ExtremaSightAxisDetector curentDetector = new ExtremaSightAxisDetector(
            ExtremaSightAxisDetector.MIN, target, this.sightViewAxis);

        propagator.addEventDetector(curentDetector);

        // Propagation
        SpacecraftState sstate = propagator.propagate(finalDate);

        Assert.assertEquals(reference, sstate.getDate().durationFrom(this.initDate), EPSILON_DURATION);

        /*
         * Same test with MIN MAX detector
         */

        // propagator
        propagator = new KeplerianPropagator(orbit, this.attitude);

        curentDetector = new ExtremaSightAxisDetector(ExtremaSightAxisDetector.MIN_MAX, target,
            this.sightViewAxis);

        propagator.addEventDetector(curentDetector);

        // Propagation
        sstate = propagator.propagate(finalDate);

        Assert.assertEquals(reference, sstate.getDate().durationFrom(this.initDate), EPSILON_DURATION);

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#SIGHTAXIS_TARGET_EXTREMA}
     * 
     * @testedMethod {@link ExtremaSightAxisDetector#ExtremaSightAxisDetector(PVCoordinatesProvider, Vector3D, double, double, Action, Action)}
     * @testedMethod {@link ExtremaSightAxisDetector#ExtremaSightAxisDetector(int, PVCoordinatesProvider, Vector3D, double, double, Action)}
     * @description Test new constructors with actions + Test getters
     * 
     * @input a detector with different actions performed for local min and max detection
     * 
     * @output action returned by eventOccurred
     * 
     * @testPassCriteria The expected action is returned by eventOccurred
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testConstructorsWithActions() throws PatriusException {
        final PVCoordinatesProvider target = this.targetMap.get("onBodyZero");
        ExtremaSightAxisDetector detector = new ExtremaSightAxisDetector(target, this.sightViewAxis,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
            Action.RESET_DERIVATIVES, Action.CONTINUE);
        final SpacecraftState s = new SpacecraftState(this.orbitRetro);
        Assert.assertEquals(Action.RESET_DERIVATIVES, detector.eventOccurred(s, true, true));
        Assert.assertEquals(Action.CONTINUE, detector.eventOccurred(s, true, false));
        Assert.assertEquals(Action.CONTINUE, detector.eventOccurred(s, false, true));
        Assert.assertEquals(Action.RESET_DERIVATIVES, detector.eventOccurred(s, false, false));

        /*
         * Test getters
         */
        Assert.assertEquals(this.sightViewAxis, detector.getSightAxis());

        detector = new ExtremaSightAxisDetector(target, this.vehicle, "payload",
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
            Action.RESET_DERIVATIVES, Action.CONTINUE);
        Assert.assertEquals(Action.RESET_DERIVATIVES, detector.eventOccurred(s, true, true));
        Assert.assertEquals(Action.CONTINUE, detector.eventOccurred(s, true, false));
        Assert.assertEquals(Action.CONTINUE, detector.eventOccurred(s, false, true));
        Assert.assertEquals(Action.RESET_DERIVATIVES, detector.eventOccurred(s, false, false));

        /*
         * Test getters
         */
        Assert.assertEquals(target.hashCode(), detector.getTargetPoint().hashCode());
        Assert.assertEquals("payload", detector.getSensorName());
        Assert.assertEquals(this.vehicle.hashCode(), detector.getVehicle().hashCode());

        /*
         * Test Constructor
         */
        detector = new ExtremaSightAxisDetector(ExtremaSightAxisDetector.MAX, target,
            this.sightViewAxis, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.RESET_DERIVATIVES);
        Assert.assertEquals(ExtremaSightAxisDetector.MAX, detector.getSlopeSelection());
        Assert.assertEquals(Action.RESET_DERIVATIVES, detector.eventOccurred(s, true, true));
        Assert.assertEquals(Action.RESET_DERIVATIVES, detector.eventOccurred(s, true, false));
        Assert.assertEquals(Action.RESET_DERIVATIVES, detector.eventOccurred(s, false, true));
        Assert.assertEquals(Action.RESET_DERIVATIVES, detector.eventOccurred(s, false, false));

        final ExtremaSightAxisDetector detectorCopy = (ExtremaSightAxisDetector) detector.copy();
        Assert.assertEquals(detector.getThreshold(), detectorCopy.getThreshold(), 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SIGHTAXIS_TARGET_EXTREMA}
     * 
     * @testedMethod {@link ExtremaSightAxisDetector#ExtremaSightAxisDetector(int, PVCoordinatesProvider, Vector3D, double, double)}
     * 
     * @description Test the exception when building the detector with a zero sight axis vector 3D.
     * 
     * @input zero sight axis vector3D
     * 
     * @output expected exception
     * 
     * @testPassCriteria expected exception
     * 
     * @throws IllegalArgumentException because the sight axis is zero
     */

    @Test(expected = IllegalArgumentException.class)
    public final void testIllegalArgumentException() {
        new ExtremaSightAxisDetector(
            ExtremaSightAxisDetector.MIN, this.targetMap.get("onBodyZero"), Vector3D.ZERO);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SIGHTAXIS_TARGET_EXTREMA}
     * 
     * @testedMethod {@link ExtremaSightAxisDetector#ExtremaSightAxisDetector(PVCoordinatesProvider, Vector3D, double, double, Action, Action)}
     * 
     * @description Test the exception when building the detector with a zero sight axis vector 3D.
     * 
     * @input zero sight axis vector3D
     * 
     * @output expected exception
     * 
     * @testPassCriteria expected exception
     * 
     * @throws IllegalArgumentException because the sight axis is zero
     */

    @Test(expected = IllegalArgumentException.class)
    public final void testIllegalArgumentException2() {
        new ExtremaSightAxisDetector(
            this.targetMap.get("onBodyZero"), Vector3D.ZERO, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.RESET_DERIVATIVES, Action.CONTINUE);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SIGHTAXIS_TARGET_EXTREMA}
     * 
     * @testedMethod {@link ExtremaSightAxisDetector#ExtremaSightAxisDetector(int, PVCoordinatesProvider, Assembly, String, double, double)}
     * 
     * @description Test the exception message when building the detector with a part that doesn't
     *              have SENSOR property.
     * 
     * @input assembly and part name "thruster"
     * 
     * @output expected exception
     * 
     * @testPassCriteria expected exception message
     * 
     * @throws IllegalArgumentException because the thruster doesn't have SENSOR property
     */

    @Test
    public final void testNewMessageException() {
        try {
            new ExtremaSightAxisDetector(
                ExtremaSightAxisDetector.MAX, this.targetMap.get("onBodyZero"), this.vehicle, "thruster");
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("The part must have a SENSOR property", e.getMessage());
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SIGHTAXIS_TARGET_EXTREMA}
     * 
     * @testedMethod {@link ExtremaSightAxisDetector#ExtremaSightAxisDetector(int, PVCoordinatesProvider, Assembly, String, double, double, Action)}
     * 
     * @description Test the exception message when building the detector with a part that doesn't
     *              have SENSOR property.
     * 
     * @input assembly and part name "thruster"
     * 
     * @output expected exception
     * 
     * @testPassCriteria expected exception message
     * 
     * @throws IllegalArgumentException because the thruster doesn't have SENSOR property
     */

    @Test
    public final void testNewMessageException2() {
        try {
            new ExtremaSightAxisDetector(
                this.targetMap.get("onBodyZero"), this.vehicle, "thruster",
                AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD,
                Action.CONTINUE, Action.CONTINUE);
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("The part must have a SENSOR property", e.getMessage());
        }
    }

    /**
     * 
     * Implementation of a step handler to track the evolution of the physical value to track
     * 
     */
    class MyStepHandler implements PatriusFixedStepHandler {

        /** serial UID */
        private static final long serialVersionUID = 5643736397373186L;

        /** results */
        public ArrayList<double[]> results;

        /** target */
        private final PVCoordinatesProvider target;

        /** sight axis */
        private Vector3D sightViewAxis;

        /** initial date */
        private final AbsoluteDate fromDate;

        /** Used if the user define an assembly to modelize spacecraft */
        private Assembly vehicle;

        /**
         * Used if the user define an assembly containing a part with sensor property to modelize
         * spacecraft
         */
        private String sensorName;

        /**
         * simple constructor without assembly
         * 
         * @param localTarget target
         * @param localSightAxis sightAxis
         * @param date initialDate of propagation
         */
        public MyStepHandler(final PVCoordinatesProvider localTarget,
            final Vector3D localSightAxis, final AbsoluteDate date) {
            this.target = localTarget;
            this.sightViewAxis = localSightAxis;
            this.results = new ArrayList<double[]>();
            this.fromDate = date;
        }

        /**
         * constructor with assembly
         * 
         * @param localTarget target
         * @param localVehicle vehicle containing a part with sensor property
         * @param date initialDate of propagation
         */
        public MyStepHandler(final PVCoordinatesProvider localTarget, final Assembly localVehicle,
            final String partName, final AbsoluteDate date) {
            this.target = localTarget;
            this.vehicle = localVehicle;
            this.sensorName = partName;
            this.results = new ArrayList<double[]>();
            this.fromDate = date;
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {

        }

        @Override
        public void handleStep(final SpacecraftState s, final boolean isLast) throws PropagationException {

            try {
                // the two vector defining the angle to track
                final PVCoordinates p1;
                final PVCoordinates p3;

                if (this.vehicle != null) {
                    this.vehicle.initMainPartFrame(s);
                    // get the sensor : usually the payload
                    final IPart payload = this.vehicle.getPart(this.sensorName);
                    // get the payload frame, sensor properties and sightAxis in this frame
                    final Frame payloadFrame = payload.getFrame();
                    final SensorProperty payloadSensorProperty = (SensorProperty) (payload
                        .getProperty(PropertyType.SENSOR));
                    final Vector3D sensorSightViewAxis = payloadSensorProperty.getInSightAxis();

                    // p1 : target PVCoordinates in payload frame
                    p1 = this.target.getPVCoordinates(s.getDate(), payloadFrame);

                    // p3 : sight axis fixed in this frame
                    p3 = new PVCoordinates(sensorSightViewAxis, Vector3D.ZERO);

                } else {

                    // Transformation from spacecraftState orbit expression frame to orbital local
                    // attitude frame
                    // notice that the transform date is only informative and useless
                    final Transform rotation = new Transform(s.getDate(), s.getAttitude()
                        .getOrientation());
                    final Transform translation = new Transform(s.getDate(), s.getOrbit()
                        .getPVCoordinates().negate());
                    final Transform transform = new Transform(s.getDate(), translation, rotation);

                    // p1 : target PVCoordinates in orbital local attitude frame
                    p1 = transform.transformPVCoordinates(this.target.getPVCoordinates(s.getDate(),
                        s.getFrame()));

                    // p3 : SightView Axis in spacecraft frame position is Axis, velocity is null
                    p3 = new PVCoordinates(this.sightViewAxis, Vector3D.ZERO);

                }

                // Angle between p1 and p3
                final double angle = Vector3D.angle(p1.getPosition(), p3.getPosition());

                final double[] currentResult = { s.getDate().durationFrom(this.fromDate), angle };

                this.results.add(currentResult);

            } catch (final PatriusException e) {
                e.printStackTrace();
            }
        }

    }
}
