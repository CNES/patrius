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
 * @history creation 18/16/2012
 *
 * HISTORY
 * VERSION:4.13.1:FA:FA-177:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3168:10/05/2022:[PATRIUS] Ajout de la classe FixedPVCoordinatesProvider 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:457:09/11/2015: Move extendedOneAxisEllipsoid from patrius to orekit addons
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.SecondarySpacecraft;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.assembly.properties.GeometricProperty;
import fr.cnes.sirius.patrius.assembly.properties.SensorProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.ConstantRadiusProvider;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.DatationChoice;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.events.detectors.MaskingDetector;
import fr.cnes.sirius.patrius.events.utils.SignalPropagationWrapperDetector;
import fr.cnes.sirius.patrius.fieldsofview.OmnidirectionalField;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Parallelepiped;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RightCircularCone;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RightCircularCylinder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SolidShape;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test class for the sensor masking event detector.
 * </p>
 * 
 * @see MaskingDetector
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class MaskingDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle sensor masking event detector
         * 
         * @featureDescription Detector for the sensor masking by a part of its own spacecraft, of
         *                     another spacecraft or by a celestial body.
         * 
         * @coveredRequirements DV-EVT_150, DV-EVT_160, DV-VISI_20, DV-VISI_40
         */
        VISIBILITY_MASKING
    }

    /** main part name */
    private static final String mainBody = "mainBody";

    /** fist masking part name */
    private static final String maskingPart1 = "masking part1";

    /** fist masking part name */
    private static final String maskingPart2 = "masking part2";

    /** the orbital period */
    private static double period;

    /** the assembly */
    private static Assembly assembly;

    /** initial orbit */
    private static Orbit initialOrbit;

    /** initial date */
    private static final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

    /** Epsilon for dates comparison. */
    private final double datesComparisonEpsilon = 1.0e-3;

    /** inertial attitude */
    private final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(
        FramesFactory.getEME2000(), Rotation.IDENTITY);

    /**
     * Creates the main spacecraft, with its sensor part and two potentially masking parts.
     * 
     * @throws PatriusException should not happen here
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        Utils.clear();
        // propagator
        final double a = 7000000.0;
        initialOrbit = new KeplerianOrbit(a, 0.0, MathUtils.HALF_PI, 0.0, 0.0, 0.0,
            PositionAngle.TRUE, FramesFactory.getEME2000(), date, Utils.mu);

        period = 2.0 * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensor
        // main field
        final String name = "omniField";
        final Vector3D mainFieldDirection = Vector3D.PLUS_K;
        final OmnidirectionalField mainField = new OmnidirectionalField(name);

        // main target : center of the earth !
        final Vector3D mainTargetPos = Vector3D.ZERO;
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(mainTargetPos, Vector3D.ZERO);
        final PVCoordinatesProvider mainTarget = new ConstantPVCoordinatesProvider(
            mainTargetCoordinates, FramesFactory.getEME2000());

        // sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        sensorProperty.setMainTarget(mainTarget, new ConstantRadiusProvider(0.0));

        // masking part geometry property creation 1
        final Vector3D maskingPartCenter1 = new Vector3D(-10., 0., -20.);
        final SolidShape maskingPartShape1 = new RightCircularCylinder(maskingPartCenter1,
            Vector3D.PLUS_J, 10., 20.);
        final GeometricProperty maskingPartGeom1 = new GeometricProperty(maskingPartShape1);

        // masking part geometry property creation 2
        final Vector3D maskingPartCenter2 = new Vector3D(20., 0., -10.);
        final SolidShape maskingPartShape2 = new Parallelepiped(maskingPartCenter2,
            Vector3D.PLUS_I, Vector3D.PLUS_J, 5., 20., 20.);
        final GeometricProperty maskingPartGeom2 = new GeometricProperty(maskingPartShape2);

        // assembly building
        try {
            // add main part
            builder.addMainPart(mainBody);

            // add sensor
            builder.addProperty(sensorProperty, mainBody);

            // add masking part 1
            builder.addPart(maskingPart1, mainBody, Vector3D.MINUS_K, Rotation.IDENTITY);
            builder.addProperty(maskingPartGeom1, maskingPart1);

            // add masking part 2
            builder.addPart(maskingPart2, mainBody, Vector3D.PLUS_I, Rotation.IDENTITY);
            builder.addProperty(maskingPartGeom2, maskingPart2);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame = new UpdatableFrame(FramesFactory.getEME2000(), Transform.IDENTITY,
                "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        assembly = builder.returnAssembly();
    }

    /**
     * @description Test this event detector wrap feature in {@link SignalPropagationWrapperDetector}
     * 
     * @input this event detector in INSTANTANEOUS & LIGHT_SPEED
     * 
     * @output the emitter & receiver dates
     * 
     * @testPassCriteria The results containers as expected (non regression)
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testSignalPropagationWrapperDetector() throws PatriusException {

        // Build two identical event detectors (the first in INSTANTANEOUS, the second in LIGHT_SPEED)
        final SensorModel sensor = new SensorModel(assembly, mainBody);
        final String[] maskingParts = { maskingPart1, maskingPart2 };
        sensor.addOwnMaskingParts(maskingParts);
        final MaskingDetector eventDetector1 = new MaskingDetector(sensor, 10., 1e-9, Action.CONTINUE, Action.CONTINUE);
        final MaskingDetector eventDetector2 = (MaskingDetector) eventDetector1.copy();
        eventDetector2.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getGCRF());

        // Wrap these event detectors
        final SignalPropagationWrapperDetector wrapper1 = new SignalPropagationWrapperDetector(eventDetector1);
        final SignalPropagationWrapperDetector wrapper2 = new SignalPropagationWrapperDetector(eventDetector2);

        // Add them in the propagator, then propagate
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, this.attitudeProv);
        propagator.addEventDetector(wrapper1);
        propagator.addEventDetector(wrapper2);
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(30 * 60.));

        // Evaluate the first event detector wrapper (INSTANTANEOUS) (emitter dates should be equal to receiver dates)
        Assert.assertEquals(2, wrapper1.getNBOccurredEvents());
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T12:10:00.426"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T12:10:00.426"), 1e-3));
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T12:23:44.945"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T12:23:44.945"), 1e-3));

        // Evaluate the second event detector wrapper (LIGHT_SPEED) (emitter dates should be before receiver dates)
        Assert.assertEquals(2, wrapper2.getNBOccurredEvents());
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T12:10:00.402"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T12:10:00.426"), 1e-3));
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T12:23:44.922"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T12:23:44.945"), 1e-3));

        // Evaluate the AbstractSignalPropagationDetector's abstract methods implementation
        Assert.assertEquals(sensor.getMainTarget(), eventDetector1.getEmitter(null));
        Assert.assertEquals(finalState.getOrbit(), eventDetector1.getReceiver(finalState));
        Assert.assertEquals(DatationChoice.RECEIVER, eventDetector1.getDatationChoice());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_MASKING}
     * 
     * @testedMethod {@link MaskingDetector#g(SpacecraftState)}
     * @testedMethod {@link MaskingDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Test of the visibility masking by the same spacecraft's parts
     * 
     * @input a spacecraft, its orbit, a sensor and some parts geometries
     * 
     * @output dates of the detected events
     * 
     * @testPassCriteria the g function has the expected sign : dates of the detected events are the
     *                   expected ones.
     * @throws PatriusException if a frame problem occurs
     */
    @Test
    public void ownPartsMaskingTestOtherConstructor() throws PatriusException {
        final Propagator propagator = new KeplerianPropagator(initialOrbit, this.attitudeProv);
        final Propagator propagatorNew = new KeplerianPropagator(initialOrbit, this.attitudeProv);

        // sensor model
        final SensorModel sensor = new SensorModel(assembly, mainBody);

        // event detector
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final MaskingDetector detector = new MaskingDetector(sensor, maxCheck, threshold);
        final MaskingDetector detectorNew1 = new MaskingDetector(
            new SensorModel(assembly, mainBody), maxCheck, threshold);
        final MaskingDetector detectorNew2 = new MaskingDetector(assembly, mainBody, maxCheck,
            threshold);

        propagator.addEventDetector(detectorNew1);
        propagator.addEventDetector(detectorNew2);
        propagatorNew.addEventDetector(detector);

        // masking by the first part
        final SpacecraftState endState = propagator.propagate(date.shiftedBy(10000.0));
        final SpacecraftState endStateNew = propagatorNew.propagate(date.shiftedBy(10000.0));
        Assert.assertEquals(endState.getDate().durationFrom(date), endStateNew.getDate()
            .durationFrom(date), this.datesComparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_MASKING}
     * 
     * @testedMethod {@link MaskingDetector#g(SpacecraftState)}
     * @testedMethod {@link MaskingDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Test of the visibility masking by the same spacecraft's parts
     * 
     * @input a spacecraft, its orbit, a sensor and some parts geometries
     * 
     * @output dates of the detected events
     * 
     * @testPassCriteria the g function has the expected sign : dates of the detected events are the
     *                   expected ones.
     * @throws PatriusException if a frame problem occurs
     */
    @Test
    public void ownPartsMaskingTest() throws PatriusException {
        final Propagator propagator = new KeplerianPropagator(initialOrbit, this.attitudeProv);

        // sensor model
        final SensorModel sensor = new SensorModel(assembly, mainBody);

        // masking panel adding
        final String[] maskingParts = { maskingPart1, maskingPart2 };
        sensor.addOwnMaskingParts(maskingParts);

        // event detector
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final MaskingDetector detector = new MaskingDetector(sensor, maxCheck, threshold);

        propagator.addEventDetector(detector);

        // masking by the first part
        SpacecraftState endState = propagator.propagate(date.shiftedBy(10000.0));
        Assert.assertEquals(1.0 / 4.0 * period, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // masking by the second part
        endState = propagator.propagate(date.shiftedBy(10000.0));
        Assert.assertEquals(1.0 / 2.0 * period, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // masking again by the first part : the second part is ignored when behind the sensor
        endState = propagator.propagate(date.shiftedBy(10000.0));
        Assert.assertEquals(5.0 / 4.0 * period, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // masking object name check
        Assert.assertEquals("main spacecraft", detector.getMaskingObjectName());
        Assert.assertEquals("masking part1", detector.getMaskingPartName());

        // wrong sensor description : the main part has no geometry
        try {
            final String[] maskingParts2 = { mainBody };
            sensor.addOwnMaskingParts(maskingParts2);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_MASKING}
     * 
     * @testedMethod {@link MaskingDetector#g(SpacecraftState)}
     * @testedMethod {@link MaskingDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Test of the visibility masking by another spacecraft's parts
     * 
     * @input a main spacecraft, its orbit and sensor, and another spacecraft and its geometry parts
     * 
     * @output dates of the detected events
     * 
     * @testPassCriteria the g function has the expected sign : dates of the detected events are the
     *                   expected ones.
     * @throws PatriusException if a frame problem occurs
     */
    @Test
    public void secondarySpacecraftMaskingTest() throws PatriusException {

        // secondary spacecraft
        final AssemblyBuilder builder = new AssemblyBuilder();
        final String mainBody2 = "mainBody2";
        final String maskingPart = "maskingPart";
        final String notMaskingPart = "notMaskingPart";

        // secondary spacecraft shape : huge E.T conic mothership
        final Vector3D center = new Vector3D(3000., 0., -20.);
        final SolidShape maskingShape = new RightCircularCone(center, Vector3D.MINUS_K,
            FastMath.PI / 4., 3000.);
        final GeometricProperty maskingProp = new GeometricProperty(maskingShape);

        // the same shape, but not masking (the sensor is between it and the earth)
        final Vector3D centerNotMasking = new Vector3D(3000., 0., -20.);
        final SolidShape notMaskingShape = new RightCircularCone(centerNotMasking, Vector3D.PLUS_K,
            FastMath.PI / 4., 3000.);
        final GeometricProperty notMaskingProp = new GeometricProperty(notMaskingShape);

        // assembly building
        try {
            // add main part
            builder.addMainPart(mainBody2);

            // masking part
            builder.addPart(maskingPart, mainBody2, Vector3D.MINUS_K, Rotation.IDENTITY);
            builder.addProperty(maskingProp, maskingPart);

            // not masking part
            builder.addPart(notMaskingPart, mainBody2, Vector3D.PLUS_K, Rotation.IDENTITY);
            builder.addProperty(notMaskingProp, notMaskingPart);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame = new UpdatableFrame(FramesFactory.getEME2000(), Transform.IDENTITY,
                "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly2 = builder.returnAssembly();

        // secondary spacecraft
        final double a = 7000000.0;
        final Orbit secondaryOrbit = new KeplerianOrbit(a, 0.0, MathUtils.HALF_PI, 0.0,
            FastMath.PI, 0.0, PositionAngle.TRUE, FramesFactory.getEME2000(), date, Utils.mu);
        final Propagator propagator2 = new KeplerianPropagator(secondaryOrbit, this.attitudeProv);
        final SecondarySpacecraft secondSpc = new SecondarySpacecraft(assembly2, propagator2,
            "secondary spacecraft");

        // main spacecraft propagator
        final Propagator propagator = new KeplerianPropagator(initialOrbit, this.attitudeProv);

        // TEST 1
        // sensor model
        final SensorModel sensor = new SensorModel(assembly, mainBody);

        // maskings
        final String[] secondarySpcMaskingParts = { maskingPart };
        sensor.addSecondaryMaskingSpacecraft(secondSpc, secondarySpcMaskingParts);

        // event detector
        final double maxCheck = 0.5;
        final double threshold = 10.e-10;
        final MaskingDetector detector = new MaskingDetector(sensor, maxCheck, threshold);
        propagator.addEventDetector(detector);

        // test
        SpacecraftState endState = propagator.propagate(date.shiftedBy(10000.0));
        Assert.assertEquals(1.0 / 4.0 * period, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // the secondary spacecraft masks not when behind the earth : the second masking happens
        // a complete orbit later
        endState = propagator.propagate(date.shiftedBy(10000.0));
        Assert.assertEquals(5.0 / 4.0 * period, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // masking object name check
        Assert.assertEquals("secondary spacecraft", detector.getMaskingObjectName());
        Assert.assertEquals("maskingPart", detector.getMaskingPartName());

        // wrong sensor description : the main part has no geometry
        try {
            final String[] maskingParts2 = { mainBody2 };
            sensor.addSecondaryMaskingSpacecraft(secondSpc, maskingParts2);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        // TEST 2
        // sensor model
        final SensorModel sensor2 = new SensorModel(assembly, mainBody);

        // maskings
        final String[] secondarySpcMaskingParts2 = { notMaskingPart };
        sensor2.addSecondaryMaskingSpacecraft(secondSpc, secondarySpcMaskingParts2);

        // event detector
        final MaskingDetector detector2 = new MaskingDetector(sensor2, maxCheck, threshold);
        propagator.addEventDetector(detector2);

        // test : the shape never masks the sensor
        endState = propagator.propagate(date.shiftedBy(10000.0));
        Assert.assertEquals(10000., endState.getDate().durationFrom(date), this.datesComparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_MASKING}
     * 
     * @testedMethod {@link MaskingDetector#g(SpacecraftState)}
     * @testedMethod {@link MaskingDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Test of the visibility masking by a celestial body
     * 
     * @input a main spacecraft, its orbit and sensor, and a celestial body
     * 
     * @output dates of the detected events
     * 
     * @testPassCriteria the g function has the expected sign : dates of the detected events are the
     *                   expected ones.
     * @throws PatriusException if a frame problem occurs
     */
    @Test
    public void celestialBodyMaskingTest() throws PatriusException {

        // the moon...
        final Transform moonPos = new Transform(AbsoluteDate.J2000_EPOCH, new Vector3D(384403000.,
            -1737000., 0.));
        final CelestialBodyFrame moonFrame = new CelestialBodyFrame(FramesFactory.getEME2000(), moonPos, "moon frame", null);
        final EllipsoidBodyShape moon = new OneAxisEllipsoid(1737000., 0., moonFrame,
            "moon");

        // spacecraft orbit : behind the moon !
        final double a = 400000000.0;
        final Orbit spacecraftOrbit = new KeplerianOrbit(a, 0., 0., 0., 3. * FastMath.PI / 2., 0.,
            PositionAngle.TRUE, FramesFactory.getEME2000(), date, Utils.mu);
        final double spacecraftPeriod = 2.0 * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);

        // spacecraft propagator
        Propagator propagator = new KeplerianPropagator(spacecraftOrbit, this.attitudeProv);

        // sensor model
        final SensorModel sensor = new SensorModel(assembly, mainBody);
        sensor.addMaskingCelestialBody(moon);

        // event detector
        final double maxCheck = 1000.;
        final double threshold = 10.e-10;
        final MaskingDetector detector = new MaskingDetector(sensor, maxCheck, threshold);
        propagator.addEventDetector(detector);

        // test
        SpacecraftState endState = propagator.propagate(date.shiftedBy(1000000.0));
        Assert.assertEquals(1.0 / 4.0 * spacecraftPeriod, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // masking object name check
        Assert.assertEquals("moon", detector.getMaskingObjectName());

        // the moon masks not when behind the earth : the second maskng happens
        // a complete orbit later
        endState = propagator.propagate(date.shiftedBy(10000000.0));
        Assert.assertEquals(5.0 / 4.0 * spacecraftPeriod, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // Second test : the spacecraft passes between the earth and the moon : no masking
        // spacecraft orbit
        final double a2 = 4000000.0;
        final Orbit spacecraftOrbit2 = new KeplerianOrbit(a2, 0., 0., 0., 3. * FastMath.PI / 2.,
            0., PositionAngle.TRUE, FramesFactory.getEME2000(), date, Utils.mu);

        // propagator reset
        propagator = new KeplerianPropagator(spacecraftOrbit2, this.attitudeProv);
        propagator.addEventDetector(detector);

        // test (sur la même durée...)
        endState = propagator.propagate(date.shiftedBy(10000000.0));
        Assert.assertEquals(10000000., endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_MASKING}
     * 
     * @testedMethod {@link MaskingDetector#MaskingDetector(SensorModel, double, double, Action) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: the sensor model, the max check value and the threshold value
     *        and the STOP Action.
     * 
     * @output a {@link MaskingDetector}
     * 
     * @testPassCriteria the {@link MaskingDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testConstructor() throws PatriusException {

        final SensorModel sensor = new SensorModel(assembly, mainBody);
        final MaskingDetector detector = new MaskingDetector(sensor, 10., 10.e-10, Action.CONTINUE,
            Action.STOP);
        // Test getters
        Assert.assertEquals(sensor, detector.getSensor());
        Assert.assertEquals(assembly, detector.getAssembly());

        final MaskingDetector detectorCopy = (MaskingDetector) detector.copy();
        Assert.assertEquals(detector.getMaxCheckInterval(), detectorCopy.getMaxCheckInterval(), 0);
    }
}
