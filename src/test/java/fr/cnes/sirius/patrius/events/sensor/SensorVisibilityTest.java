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
 * @history creation 23/04/2012
 *
 * HISTORY
- * VERSION:4.13.1:FA:FA-177:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3168:10/05/2022:[PATRIUS] Ajout de la classe FixedPVCoordinatesProvider 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
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
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.assembly.properties.GeometricProperty;
import fr.cnes.sirius.patrius.assembly.properties.SensorProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.ApparentRadiusProvider;
import fr.cnes.sirius.patrius.bodies.ConstantRadiusProvider;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.DatationChoice;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.events.detectors.SensorInhibitionDetector;
import fr.cnes.sirius.patrius.events.detectors.SensorVisibilityDetector;
import fr.cnes.sirius.patrius.events.detectors.TargetInFieldOfViewDetector;
import fr.cnes.sirius.patrius.events.utils.SignalPropagationWrapperDetector;
import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plate;
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
 * @description <p>
 *              Test class for the sensor's visibility detectors (InhibitionDetector, TargetInFieldOfViewDetector,
 *              VisibilityDetector).
 *              </p>
 * 
 * @see SensorInhibitionDetector
 * @see SensorVisibilityDetector
 * @see TargetInFieldOfViewDetector
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class SensorVisibilityTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle sensor's visibility
         * 
         * @featureDescription Three events detectors are tested : main target in field of view,
         *                     inhibition, and total visibility.
         * 
         * @coveredRequirements DV-VEHICULE_230, DV-VEHICULE_240, DV-VEHICULE_250, DV-VEHICULE_270,
         *                      DV-EVT_170, DV-VISI_40, DV-VISI_80, DV-EVT_160, DV-VISI_20
         */
        VISIBILITY
    }

    /** Epsilon for dates comparison. */
    private double datesComparisonEpsilon;

    private Assembly assembly;
    private String mainBody;

    /**
     * Setup configuration frame, orekit data, and all common parameter of the tests
     * 
     * @throws PatriusException should not happen
     */
    @Before
    public void setup() throws PatriusException {
        // Initialise configuration without EOP to avoid using external data file
        Utils.clear();

        this.datesComparisonEpsilon = 1.0e-3;
        final Frame EME2000Frame = FramesFactory.getEME2000();

        // building the assembly
        this.mainBody = "mainBody";
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensor
        // main field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.PLUS_K;
        final CircularField mainField = new CircularField(name, FastMath.PI / 4.0, mainFieldDirection);

        // main target : center of the earth !
        final Vector3D mainTargetPos = Vector3D.ZERO;
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(mainTargetPos, Vector3D.ZERO);
        final PVCoordinatesProvider mainTarget = new ConstantPVCoordinatesProvider(mainTargetCoordinates, EME2000Frame);

        // inhibition fields
        final CircularField[] inhibitFields = new CircularField[2];
        inhibitFields[0] = new CircularField("inhibitionField1", FastMath.PI / 8.0, Vector3D.PLUS_I);
        inhibitFields[1] = new CircularField("inhibitionField2", FastMath.PI / 4.0, Vector3D.MINUS_I);

        // inhibition targets
        final PVCoordinates inhibitionTargetCoordinates1 = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);
        final Vector3D inhibitionTargetPos2 = new Vector3D(-50., 0., -7000050.);
        final PVCoordinates inhibitionTargetCoordinates2 = new PVCoordinates(inhibitionTargetPos2, Vector3D.ZERO);
        final PVCoordinatesProvider[] inhibitionTargets = new PVCoordinatesProvider[2];
        inhibitionTargets[0] = new ConstantPVCoordinatesProvider(inhibitionTargetCoordinates1, EME2000Frame);
        inhibitionTargets[1] = new ConstantPVCoordinatesProvider(inhibitionTargetCoordinates2, EME2000Frame);

        // Radius provider
        final ApparentRadiusProvider radius = new ConstantRadiusProvider(0.0);

        // sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        sensorProperty.setMainTarget(mainTarget, radius);
        final ApparentRadiusProvider[] inhibitionTargetsRadiuses = { radius, radius };
        sensorProperty.setInhibitionFieldsAndTargets(inhibitFields, inhibitionTargets, inhibitionTargetsRadiuses);

        // assembly building
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add sensor
            builder.addProperty(sensorProperty, this.mainBody);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame = new UpdatableFrame(EME2000Frame, Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        this.assembly = builder.returnAssembly();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY}
     * 
     * @testedMethod {@link TargetInFieldOfViewDetector#g(SpacecraftState)}
     * @testedMethod {@link SensorVisibilityDetector#g(SpacecraftState)}
     * @testedMethod {@link SensorInhibitionDetector#g(SpacecraftState)}
     * @testedMethod {@link TargetInFieldOfViewDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link SensorVisibilityDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link SensorInhibitionDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Test of the "target in field of view detector", "inhibition detector" and
     *              "visibility" detector during a keplerian orbit.
     * 
     * @input A sensor model, a SpacecraftState
     * 
     * @output dates of the detected events are the expected ones.
     * 
     * @testPassCriteria the g function has the expected sign
     * @throws PatriusException if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void sensorVisibilityTest() throws PatriusException {

        // frame and date
        final Frame EME2000Frame = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // events detectors
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final TargetInFieldOfViewDetector targetInField = new TargetInFieldOfViewDetector(this.assembly, this.mainBody,
            maxCheck, threshold);
        final SensorInhibitionDetector inhibition = new SensorInhibitionDetector(this.assembly, this.mainBody,
            maxCheck, threshold);
        final SensorVisibilityDetector visibility = new SensorVisibilityDetector(this.assembly, this.mainBody,
            maxCheck, threshold);

        // propagator
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        final double a = 7000000.0;
        final Orbit initialOrbit = new KeplerianOrbit(a, 0., MathUtils.HALF_PI, 0.0, 0.0,
            0.0, PositionAngle.TRUE, EME2000Frame, date, Utils.mu);
        Propagator propagator = new KeplerianPropagator(initialOrbit, attitudeProv);

        final double period = 2.0 * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);

        // TARGET IN FIELD TEST
        // =====================
        propagator.addEventDetector(targetInField);

        SpacecraftState endState = propagator.propagate(date.shiftedBy(10000.));
        Assert.assertEquals(7.0 / 8.0 * period, endState.getDate().durationFrom(date), this.datesComparisonEpsilon);

        // INHIBITION TEST
        // ================
        propagator = new KeplerianPropagator(initialOrbit, attitudeProv);
        propagator.clearEventsDetectors();
        propagator.addEventDetector(inhibition);

        endState = propagator.propagate(date.shiftedBy(10000.));
        Assert.assertEquals(9.0 / 16.0 * period, endState.getDate().durationFrom(date), this.datesComparisonEpsilon);

        // concerned inhibition field number check
        Assert.assertEquals(2, inhibition.getInhibitionNumber());

        // VISIBILITY TEST
        // ==================
        propagator.clearEventsDetectors();
        propagator.addEventDetector(visibility);
        endState = propagator.propagate(date.shiftedBy(10000.0));
        Assert.assertEquals(3.0 / 4.0 * period, endState.getDate().durationFrom(date), this.datesComparisonEpsilon);

        // concerned inhibition field number check : number 2
        Assert.assertEquals(2, visibility.getInhibitionNumber());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY}
     * 
     * @testedMethod {@link TargetInFieldOfViewDetector#g(SpacecraftState)}
     * @testedMethod {@link SensorVisibilityDetector#g(SpacecraftState)}
     * @testedMethod {@link SensorInhibitionDetector#g(SpacecraftState)}
     * @testedMethod {@link TargetInFieldOfViewDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link SensorVisibilityDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link SensorInhibitionDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Test of the "target in field of view detector" and "visibility" detector during
     *              a keplerian orbit, with spherical targets (not null radiuses)
     * 
     * @input A sensor model, a SpacecraftState, spherical targets
     * 
     * @output dates of the detected events are the expected ones.
     * 
     * @testPassCriteria the g function has the expected sign
     * @throws PatriusException if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void sphericalTargetTest() throws PatriusException {

        // frame and date
        final Frame EME2000Frame = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensor
        // main field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.PLUS_K;
        final CircularField mainField = new CircularField(name, FastMath.PI / 4.0, mainFieldDirection);

        // main target : center of the earth !
        final Vector3D mainTargetPos = Vector3D.ZERO;
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(mainTargetPos, Vector3D.ZERO);
        final PVCoordinatesProvider mainTarget = new ConstantPVCoordinatesProvider(mainTargetCoordinates, EME2000Frame);

        // inhibition field
        final CircularField[] inhibitFields = new CircularField[1];
        inhibitFields[0] = new CircularField("inhibitionField1", FastMath.PI / 4.0, Vector3D.MINUS_I);

        // inhibition target
        final double inhibitionTargetRadius = 2000.0;
        // Radius provider
        final ApparentRadiusProvider inhibitionRadius = new ConstantRadiusProvider(inhibitionTargetRadius);

        final Vector3D inhibitionTargetPos = new Vector3D(-7000000.0 + inhibitionTargetRadius
                / MathLib.sqrt(2.0), 0.0, -14000000.0 - inhibitionTargetRadius / MathLib.sqrt(2.0));
        final PVCoordinates inhibitionTargetCoordinates1 = new PVCoordinates(inhibitionTargetPos, Vector3D.ZERO);
        final PVCoordinatesProvider[] inhibitionTargets = new PVCoordinatesProvider[1];
        inhibitionTargets[0] = new ConstantPVCoordinatesProvider(inhibitionTargetCoordinates1, EME2000Frame);

        // sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        final double mainTargetRadius = 600.0;
        // Radius provider
        final ApparentRadiusProvider targetRadius = new ConstantRadiusProvider(mainTargetRadius);
        sensorProperty.setMainTarget(mainTarget, targetRadius);
        final ApparentRadiusProvider[] inhibitionTargetsRadiuses = { inhibitionRadius };
        sensorProperty.setInhibitionFieldsAndTargets(inhibitFields, inhibitionTargets, inhibitionTargetsRadiuses);

        // assembly building
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add sensor
            builder.addProperty(sensorProperty, this.mainBody);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame = new UpdatableFrame(EME2000Frame, Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // events detectors
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final EventDetector targetInField = new TargetInFieldOfViewDetector(assembly, this.mainBody, maxCheck,
            threshold);
        final EventDetector visibility = new SensorVisibilityDetector(assembly, this.mainBody, maxCheck, threshold);

        // propagator
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        final double a = 7000000.;
        final Orbit initialOrbit = new KeplerianOrbit(a, 0.0, MathUtils.HALF_PI, 0., 0., 0.0,
            PositionAngle.TRUE, EME2000Frame, date, Utils.mu);
        Propagator propagator = new KeplerianPropagator(initialOrbit, attitudeProv);

        final double period = 2.0 * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);

        // TARGET IN FIELD TEST
        // =====================
        propagator.addEventDetector(targetInField);

        SpacecraftState endState = propagator.propagate(date.shiftedBy(10000.0));
        final double plusAngle = MathLib.asin(mainTargetRadius / a);
        final double plusTime = plusAngle * MathLib.sqrt(a * a * a / Utils.mu);
        Assert.assertEquals(7.0 / 8.0 * period + plusTime, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // VISIBILITY TEST
        // ==================
        propagator = new KeplerianPropagator(initialOrbit, attitudeProv);
        propagator.clearEventsDetectors();
        propagator.addEventDetector(visibility);
        endState = propagator.propagate(date.shiftedBy(10000.0));
        Assert.assertEquals(3.0 / 4.0 * period, endState.getDate().durationFrom(date), this.datesComparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY}
     * 
     * @testedMethod {@link SensorVisibilityDetector#g(SpacecraftState)}
     * @testedMethod {@link SensorVisibilityDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Test of the "visibility" detector during a keplerian orbit, with maskings. The
     *              visibility ends when the masking begins.
     * 
     * @input A sensor model, a SpacecraftState, a target and a masking object (solar panel)
     * 
     * @output dates of the detected events are the expected ones.
     * 
     * @testPassCriteria the g function has the expected sign : the visibility stops when the solar
     *                   panel cuts the sensor-earth center line (at T/4).
     * @throws PatriusException if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void maskingTest() throws PatriusException {

        // frame and date
        final Frame EME2000Frame = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // propagator
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        final double a = 7000000.;
        final Orbit initialOrbit = new KeplerianOrbit(a, 0.0, MathUtils.HALF_PI, 0., 0., 0., PositionAngle.TRUE,
            EME2000Frame, date, Utils.mu);
        Propagator propagator = new KeplerianPropagator(initialOrbit, attitudeProv);

        final double period = 2. * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);

        // building the assembly
        final String mainBody = "mainBody";
        final String solarPanel = "solarPanel";
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensor
        // main field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.MINUS_K;
        final CircularField mainField = new CircularField(name, FastMath.PI / 4., mainFieldDirection);

        // main target : center of the earth !
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);
        final PVCoordinatesProvider mainTarget = new ConstantPVCoordinatesProvider(mainTargetCoordinates, EME2000Frame);

        // sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        sensorProperty.setMainTarget(mainTarget, new ConstantRadiusProvider(0.0));

        // solar panel geometry property creation
        final Vector3D solarPanelCenter = new Vector3D(20., 0., 0.);
        final SolidShape solarPanelShape = new Plate(solarPanelCenter, Vector3D.PLUS_I, Vector3D.PLUS_J, 40., 20.);
        final GeometricProperty solarPanelGeom = new GeometricProperty(solarPanelShape);

        // assembly building
        try {
            // add main part
            builder.addMainPart(mainBody);

            // add sensor
            builder.addProperty(sensorProperty, mainBody);

            // add solar panel
            builder.addPart(solarPanel, mainBody, Vector3D.MINUS_K, Rotation.IDENTITY);
            builder.addProperty(solarPanelGeom, solarPanel);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame = new UpdatableFrame(EME2000Frame, Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // FIRST TEST
        // ===========
        // the solar panel masks

        // sensor model
        final SensorModel sensor = new SensorModel(assembly, mainBody);

        // masking panel adding
        final String[] maskingParts = { solarPanel };
        sensor.addOwnMaskingParts(maskingParts);

        // event detector
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final SensorVisibilityDetector detector = new SensorVisibilityDetector(sensor, maxCheck, threshold);

        propagator.addEventDetector(detector);

        SpacecraftState endState = propagator.propagate(date.shiftedBy(10000.0));
        Assert.assertEquals(1. / 4. * period, endState.getDate().durationFrom(date), this.datesComparisonEpsilon);

        // masking object name check
        Assert.assertEquals("main spacecraft", detector.getMaskingObjectName());
        Assert.assertEquals("solarPanel", detector.getMaskingPartName());

        // SECOND TEST
        // ============
        // A little asteroid strangely passes between the satellite and the earth...
        propagator = new KeplerianPropagator(initialOrbit, attitudeProv);

        // sensor model
        final SensorModel sensor2 = new SensorModel(assembly, mainBody);

        // stopped asteroid in the zenith of the north pole (it happens rarely)
        final Transform asteroidPos = new Transform(AbsoluteDate.J2000_EPOCH, new Vector3D(-10000, 0., 6500000.0));
        final CelestialBodyFrame asteroidFrame = new CelestialBodyFrame(EME2000Frame, asteroidPos, "asteroid frame",
            null);
        final EllipsoidBodyShape asteroid = new OneAxisEllipsoid(10000., 0., asteroidFrame, "mysterious asteroid");

        sensor2.addMaskingCelestialBody(asteroid);

        final SensorVisibilityDetector detector2 = new SensorVisibilityDetector(sensor2, maxCheck, threshold);

        propagator.addEventDetector(detector2);

        endState = propagator.propagate(date.shiftedBy(10000.));
        Assert.assertEquals(1. / 4. * period, endState.getDate().durationFrom(date), this.datesComparisonEpsilon);

        // masking object name check
        Assert.assertEquals("mysterious asteroid", detector2.getMaskingObjectName());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY}
     * 
     * @testedMethod {@link SensorVisibilityDetector#SensorVisibilityDetector}
     * @testedMethod {@link SensorInhibitionDetector#SensorInhibitionDetector}
     * @testedMethod {@link TargetInFieldOfViewDetector#TargetInFieldOfViewDetector}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: sensor model, the max check value and the threshold value and
     *        the STOP Action.
     * 
     * @output a {@link SensorVisibilityDetector}
     * @output a {@link SensorInhibitionDetector}
     * @output a {@link TargetInFieldOfViewDetector}
     * 
     * @testPassCriteria the {@link SensorVisibilityDetector} is successfully created
     * @testPassCriteria the {@link SensorInhibitionDetector} is successfully created
     * @testPassCriteria the {@link TargetInFieldOfViewDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testConstructor() throws PatriusException {

        // frame and date
        final Frame EME2000Frame = FramesFactory.getEME2000();
        // building the assembly
        final String mainBody = "mainBody";
        final AssemblyBuilder builder = new AssemblyBuilder();
        // sensor
        // main field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.PLUS_K;
        final CircularField mainField = new CircularField(name, FastMath.PI / 4., mainFieldDirection);
        // main target : center of the earth !
        final Vector3D mainTargetPos = Vector3D.ZERO;
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(mainTargetPos, Vector3D.ZERO);
        final PVCoordinatesProvider mainTarget = new ConstantPVCoordinatesProvider(mainTargetCoordinates, EME2000Frame);
        // inhibition field
        final CircularField[] inhibitFields = new CircularField[1];
        inhibitFields[0] = new CircularField("inhibitionField1", FastMath.PI / 4., Vector3D.MINUS_I);
        // inhibition target
        final double inhibitionTargetRadius = 2000.;
        // inhibition target radius provider
        final ApparentRadiusProvider inhibitionRadius = new ConstantRadiusProvider(inhibitionTargetRadius);

        final Vector3D inhibitionTargetPos = new Vector3D(-7000000.0 + inhibitionTargetRadius / MathLib.sqrt(2.), 0.,
            -14000000. - inhibitionTargetRadius / MathLib.sqrt(2.));
        final PVCoordinates inhibitionTargetCoordinates1 = new PVCoordinates(inhibitionTargetPos, Vector3D.ZERO);
        final PVCoordinatesProvider[] inhibitionTargets = new PVCoordinatesProvider[1];
        inhibitionTargets[0] = new ConstantPVCoordinatesProvider(inhibitionTargetCoordinates1, EME2000Frame);
        // sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        final double mainTargetRadius = 600.;
        // Target radius provider
        final ApparentRadiusProvider targetRadius = new ConstantRadiusProvider(mainTargetRadius);

        sensorProperty.setMainTarget(mainTarget, targetRadius);
        final ApparentRadiusProvider[] inhibitionTargetsRadiuses = { inhibitionRadius };
        sensorProperty.setInhibitionFieldsAndTargets(inhibitFields, inhibitionTargets, inhibitionTargetsRadiuses);
        // assembly building
        try {
            // add main part
            builder.addMainPart(mainBody);

            // add sensor
            builder.addProperty(sensorProperty, mainBody);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame = new UpdatableFrame(EME2000Frame, Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // events detectors
        final double maxCheck = 10.;
        final double threshold = 10.e-10;

        final SensorInhibitionDetector inhibition = (SensorInhibitionDetector) new SensorInhibitionDetector(assembly,
            mainBody, maxCheck, threshold, Action.CONTINUE, Action.STOP).copy();
        // test getters
        Assert.assertEquals(assembly, inhibition.getAssembly());

        final SensorVisibilityDetector visibility = new SensorVisibilityDetector(assembly, mainBody, maxCheck,
            threshold, Action.CONTINUE, Action.STOP);
        // test getters
        Assert.assertEquals(assembly, visibility.getAssembly());

        final TargetInFieldOfViewDetector targetInField = (TargetInFieldOfViewDetector) new TargetInFieldOfViewDetector(
            assembly, mainBody, maxCheck, threshold, Action.CONTINUE, Action.STOP).copy();
        // test getters
        Assert.assertEquals(assembly, targetInField.getAssembly());

        // sensor model
        final SensorModel sensor = new SensorModel(assembly, mainBody);

        final SensorInhibitionDetector inhibition2 = new SensorInhibitionDetector(sensor, maxCheck, threshold);
        // test getters
        Assert.assertEquals(sensor, inhibition2.getSensor());

        final SensorVisibilityDetector visibility2 = new SensorVisibilityDetector(sensor, maxCheck, threshold);
        // test getters
        Assert.assertEquals(sensor, visibility2.getSensor());

        final TargetInFieldOfViewDetector targetInField2 = new TargetInFieldOfViewDetector(sensor, maxCheck, threshold);
        // test getters
        Assert.assertEquals(sensor, targetInField2.getSensor());

        // Copy
        final SensorVisibilityDetector detectorCopy = (SensorVisibilityDetector) visibility2.copy();
        Assert.assertEquals(visibility2.getMaskingObjectName(), detectorCopy.getMaskingObjectName());
    }

    /**
     * @description Test the {@link SensorInhibitionDetector} event detector wrap feature in
     *              {@link SignalPropagationWrapperDetector}
     * 
     * @output the emitter & receiver dates
     * 
     * @testPassCriteria The results as expected and the expected exceptions.
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testSensorInhibitionSignalPropagationWrapperDetector() throws PatriusException {

        // frame and date
        final Frame EME2000Frame = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Build an event detector
        final SensorInhibitionDetector eventDetector = new SensorInhibitionDetector(this.assembly, this.mainBody,
            10., 1e-9, Action.CONTINUE, Action.CONTINUE);

        // Wrap this event detector
        final SignalPropagationWrapperDetector wrapper = new SignalPropagationWrapperDetector(eventDetector);

        // Add them in the propagator
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        final Orbit initialOrbit = new KeplerianOrbit(7000000., 0., MathUtils.HALF_PI, 0., 0., 0., PositionAngle.TRUE,
            EME2000Frame, date, Utils.mu);
        final Propagator propagator = new KeplerianPropagator(initialOrbit, attitudeProv);
        propagator.addEventDetector(wrapper);

        // Try to propagate 1h (so an event should have occurred): should fail as the wrapper calls the getEmitter
        // method which is not supported by this detector
        try {
            propagator.propagate(date.shiftedBy(3600.));
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Try to call the getEmitter- method: should fail as it is not supported by this detector
        try {
            eventDetector.getEmitter(null);
            Assert.fail();
        } catch (final UnsupportedOperationException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Evaluate the others AbstractSignalPropagationDetector's abstract methods implementation
        Assert.assertEquals(initialOrbit, eventDetector.getReceiver(propagator.getInitialState()));
        Assert.assertEquals(DatationChoice.RECEIVER, eventDetector.getDatationChoice());
    }

    /**
     * @description Test the {@link SensorVisibilityDetector} event detector wrap feature in
     *              {@link SignalPropagationWrapperDetector}
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
    public void testSensorVisibilitySignalPropagationWrapperDetector() throws PatriusException {

        // frame and date
        final Frame EME2000Frame = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Build two identical event detectors (the first in INSTANTANEOUS, the second in LIGHT_SPEED)
        final SensorVisibilityDetector eventDetector1 = new SensorVisibilityDetector(this.assembly, this.mainBody,
            10., 1e-9, Action.CONTINUE, Action.CONTINUE);
        final SensorVisibilityDetector eventDetector2 = (SensorVisibilityDetector) eventDetector1.copy();
        eventDetector2.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getGCRF());

        // Wrap these event detectors
        final SignalPropagationWrapperDetector wrapper1 = new SignalPropagationWrapperDetector(eventDetector1);
        final SignalPropagationWrapperDetector wrapper2 = new SignalPropagationWrapperDetector(eventDetector2);

        // Add them in the propagator, then propagate
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        final Orbit initialOrbit = new KeplerianOrbit(7000000., 0., MathUtils.HALF_PI, 0., 0., 0., PositionAngle.TRUE,
            EME2000Frame, date, Utils.mu);
        final Propagator propagator = new KeplerianPropagator(initialOrbit, attitudeProv);
        propagator.addEventDetector(wrapper1);
        propagator.addEventDetector(wrapper2);
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(2 * 3600.));

        // Evaluate the first event detector wrapper (INSTANTANEOUS) (emitter dates should be equal to receiver dates)
        Assert.assertEquals(2, wrapper1.getNBOccurredEvents());
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T13:00:10.639"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T13:00:10.639"), 1e-3));
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:12:19.203"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:12:19.203"), 1e-3));

        // Evaluate the second event detector wrapper (LIGHT_SPEED) (emitter dates should be before receiver dates)
        Assert.assertEquals(2, wrapper2.getNBOccurredEvents());
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T13:00:10.616"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T13:00:10.639"), 1e-3));
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:12:19.180"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:12:19.203"), 1e-3));

        // Evaluate the AbstractSignalPropagationDetector's abstract methods implementation
        Assert.assertEquals(eventDetector1.getSensor().getMainTarget(), eventDetector1.getEmitter(null));
        Assert.assertEquals(finalState.getOrbit(), eventDetector1.getReceiver(finalState));
        Assert.assertEquals(DatationChoice.RECEIVER, eventDetector1.getDatationChoice());
    }

    /**
     * @description Test the {@link TargetInFieldOfViewDetector} event detector wrap feature in
     *              {@link SignalPropagationWrapperDetector}
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
    public void testTargetInFieldOfViewSignalPropagationWrapperDetector() throws PatriusException {

        // frame and date
        final Frame EME2000Frame = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Build two identical event detectors (the first in INSTANTANEOUS, the second in LIGHT_SPEED)
        final TargetInFieldOfViewDetector eventDetector1 = new TargetInFieldOfViewDetector(this.assembly,
            this.mainBody, 10., 1e-9, Action.CONTINUE, Action.CONTINUE);
        final TargetInFieldOfViewDetector eventDetector2 = (TargetInFieldOfViewDetector) eventDetector1.copy();
        eventDetector2.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getGCRF());

        // Wrap these event detectors
        final SignalPropagationWrapperDetector wrapper1 = new SignalPropagationWrapperDetector(eventDetector1);
        final SignalPropagationWrapperDetector wrapper2 = new SignalPropagationWrapperDetector(eventDetector2);

        // Add them in the propagator, then propagate
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        final Orbit initialOrbit = new KeplerianOrbit(7000000., 0., MathUtils.HALF_PI, 0., 0., 0., PositionAngle.TRUE,
            EME2000Frame, date, Utils.mu);
        final Propagator propagator = new KeplerianPropagator(initialOrbit, attitudeProv);
        propagator.addEventDetector(wrapper1);
        propagator.addEventDetector(wrapper2);
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(2 * 3600.));

        // Evaluate the first event detector wrapper (INSTANTANEOUS) (emitter dates should be equal to receiver dates)
        Assert.assertEquals(2, wrapper1.getNBOccurredEvents());
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T13:00:10.639"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T13:00:10.639"), 1e-3));
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:24:27.768"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:24:27.768"), 1e-3));

        // Evaluate the second event detector wrapper (LIGHT_SPEED) (emitter dates should be before receiver dates)
        Assert.assertEquals(2, wrapper2.getNBOccurredEvents());
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T13:00:10.616"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T13:00:10.639"), 1e-3));
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:24:27.745"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:24:27.768"), 1e-3));

        // Evaluate the AbstractSignalPropagationDetector's abstract methods implementation
        Assert.assertEquals(eventDetector1.getSensor().getMainTarget(), eventDetector1.getEmitter(null));
        Assert.assertEquals(finalState.getOrbit(), eventDetector1.getReceiver(finalState));
        Assert.assertEquals(DatationChoice.RECEIVER, eventDetector1.getDatationChoice());
    }
}
