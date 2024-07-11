/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 * 
 * HISTORY
* VERSION:4.9:DM:DM-3151:10/05/2022:[PATRIUS] Evolution de la casse VisibilityFromStationDetector
* VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
* VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:FA:FA-2955:15/11/2021:[PATRIUS] Tests manquants pour certains evenements avec prise en compte du temps de propagation 
* VERSION:4.8:FA:FA-2956:15/11/2021:[PATRIUS] Temps de propagation non implemente pour certains evenements 
 * VERSION:4.5:DM:DM-2367:27/05/2020:Configuration de changement de repère simplifiee 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 * 
 */
package fr.cnes.sirius.patrius.propagation.events;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.RFLinkBudgetModel;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.assembly.properties.RFAntennaProperty;
import fr.cnes.sirius.patrius.assembly.properties.SensorProperty;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.GeometricBodyShape;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.events.CentralBodyMaskCircularFOVDetector;
import fr.cnes.sirius.patrius.events.sensor.ExtremaSightAxisDetector;
import fr.cnes.sirius.patrius.events.sensor.MaskingDetector;
import fr.cnes.sirius.patrius.events.sensor.RFVisibilityDetector;
import fr.cnes.sirius.patrius.events.sensor.SensorInhibitionDetector;
import fr.cnes.sirius.patrius.events.sensor.SensorVisibilityDetector;
import fr.cnes.sirius.patrius.events.sensor.StationToSatMutualVisibilityDetector;
import fr.cnes.sirius.patrius.events.sensor.TargetInFieldOfViewDetector;
import fr.cnes.sirius.patrius.events.sensor.VisibilityFromStationDetector;
import fr.cnes.sirius.patrius.events.sensor.VisibilityFromStationDetector.LinkType;
import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.groundstation.GeometricStationAntenna;
import fr.cnes.sirius.patrius.groundstation.RFStationAntenna;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class DetectorTest {

    private double mu;

    @Test
    public void testBasicScheduling() throws PatriusException {

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new CircularOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), date, this.mu);

        final Propagator propagator = new KeplerianPropagator(orbit);
        final double stepSize = 60.0;
        final OutOfOrderChecker detector = new OutOfOrderChecker(date.shiftedBy(5.25 * stepSize), stepSize);
        propagator.addEventDetector(detector);
        propagator.setMasterMode(stepSize, detector);
        propagator.propagate(date.shiftedBy(10 * stepSize));
        Assert.assertTrue(detector.outOfOrderCallDetected());

    }

    private static class OutOfOrderChecker extends DateDetector implements PatriusFixedStepHandler {

        private static final long serialVersionUID = 26319257020496654L;
        private AbsoluteDate triggerDate;
        private boolean outOfOrderCallDetected;
        private final double stepSize;

        public OutOfOrderChecker(final AbsoluteDate target, final double stepSize) {
            super(target);
            this.triggerDate = null;
            this.outOfOrderCallDetected = false;
            this.stepSize = stepSize;
        }

        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
            this.triggerDate = s.getDate();
            return Action.CONTINUE;
        }

        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast) {
            // step handling and event occurrences may be out of order up to one step
            // with variable steps, and two steps with fixed steps (due to the delay
            // induced by StepNormalizer)
            if (this.triggerDate != null) {
                final double dt = currentState.getDate().durationFrom(this.triggerDate);
                if (dt < 0) {
                    this.outOfOrderCallDetected = true;
                    Assert.assertTrue(MathLib.abs(dt) < (2 * this.stepSize));
                }
            }
        }

        public boolean outOfOrderCallDetected() {
            return this.outOfOrderCallDetected;
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        this.mu = Constants.EIGEN5C_EARTH_MU;
    }

    /**
     * @description check for all detectors that detection is performed at the correct date when considering signal travels at
     * light speed and is not instantaneous.
     * In order to compute the reference mathematically, the configuration is simplified to maximum:
     * <ul>
     * <li>Earth is not oblate</li>
     * <li>Station is on the equator, looking upward, with a small field of view</li>
     * <li>Satellite is on a circular equatorial orbit, with an Earth pointing law and a small FOV sensor along K vector</li>
     * </ul>
     * 
     * @testPassCriteria the detection delay is as expected for each detector (approximative reference mathematically computed,
     * except for SensorInhibitionDetector, RFVisibilityDetector, where a simple non-regression is performed since reference cannot be mathematically computed)
     * SatToSatMutualVisibilityDetector is tested in class SatToSatMutualVisibilityTest

     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public final void testDetectionPropagationDelay() throws PatriusException {
        Report.printClassHeader(DetectorTest.class.getSimpleName(), "Detectors");
        Report.printMethodHeader("testDetectionPropagationDelay", "Delay computation", "Math", 1E-2, ComparisonType.ABSOLUTE);

        // Initialization
        FramesFactory.setConfiguration(FramesConfigurationFactory.getSimpleConfiguration(false));
        final GeometricBodyShape earth = new ExtendedOneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                0, FramesFactory.getTIRF(), "");
        // Station such that the satellite passes exactly over the station at anomaly = Pi / 2
        final TopocentricFrame topo = new TopocentricFrame(earth, new GeodeticPoint(0, FastMath.PI / 2., 0), "");

        // Assembly
        final Assembly assembly = buildAssembly();
        final Assembly assembly2 = buildAssembly();

        // Sun and "Delayed Sun". Delayed Sun is such that a ray leaving sun1 at light speed should arrive on the spacecraft at
        // same date than a instantaneous ray of sun2
        final CelestialBody sun1 = CelestialBodyFactory.getSun();
        final CelestialBody sun2 = new MeeusSun() {
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return CelestialBodyFactory.getSun().getPVCoordinates(date.shiftedBy(-490.6931414907628), frame);
            }
        };

        // Station and sensors
        final double[][] mask = { {0, FastMath.PI / 2 - 0.5 }, {2. * MathLib.PI, FastMath.PI / 2 - 0.5 }};
        final GeometricStationAntenna station = new GeometricStationAntenna(topo, new CircularField("", 0.01, Vector3D.PLUS_K));
        final SensorModel sensor = new SensorModel(assembly, "Main");
        sensor.addMaskingCelestialBody(new ExtendedOneAxisEllipsoid(700E6, 0, sun1.getInertialEquatorFrame(), "Sun1"));
        sensor.setMainTarget(topo, new ConstantRadiusProvider(1.));
        final SensorModel sensor2 = new SensorModel(assembly2, "Main");
        sensor2.setMainTarget(topo, new ConstantRadiusProvider(1.));
        sensor2.addMaskingCelestialBody(new ExtendedOneAxisEllipsoid(700E6, 0, sun2.getInertialEquatorFrame(), "Sun2"));

        // Station
        final double[][] atmopshericLoss = {
            { 0.0E+00, 0.677E+01 },
            { 0.5E+01, 0.109E+01 },
            { 0.1E+02, 0.500E+00 },
            { 0.2E+02, 0.230E+00 },
            { 0.3E+02, 0.150E+00 },
            { 0.4E+02, 0.120E+00 },
            { 0.5E+02, 0.100E+00 },
            { 0.6E+02, 0.800E-01 },
            { 0.7E+02, 0.800E-01 },
            { 0.9E+02, 0.700E-01 },
        };
        final double[][] pointingLoss = {
            { 0.0E+00, 0 },
            { 0.1E+02, 0 },
            { 0.2E+02, 0 },
            { 0.3E+02, 0 },
            { 0.4E+02, 0 },
            { 0.5E+02, 0 },
            { 0.6E+02, 0 },
            { 0.7E+02, 0 },
            { 0.9E+02, 0 },
        };
        final RFStationAntenna groundAntenna = new RFStationAntenna(topo, 11.5, 2.5, 3., atmopshericLoss, pointingLoss,
                0.);
        final RFLinkBudgetModel linkBudgetModel = new RFLinkBudgetModel(groundAntenna, assembly, "Main");

        // Sun - related detectors
        
        // Beta angle detector
        AbstractDetector d1 = new BetaAngleDetector(-0.402, 10, 1E-12, Action.STOP, false, sun1);
        AbstractDetector d2 = new BetaAngleDetector(-0.402, 10, 1E-12, Action.STOP, false, sun2);
        checkPropagation(d1, d2);

        // Local time angle detector
        d1 = new LocalTimeAngleDetector(-0.23, 10, 1E-12, Action.STOP, false, sun1);
        d2 = new LocalTimeAngleDetector(-0.23, 10, 1E-12, Action.STOP, false, sun2);
        checkPropagation(d1, d2);

        // Solar time angle detector
        d1 = new SolarTimeAngleDetector(-0.23, sun1, 10, 1E-12, Action.STOP, false);
        d2 = new SolarTimeAngleDetector(-0.23, sun2, 10, 1E-12, Action.STOP, false);
        checkPropagation(d1, d2);

        // Nadir solar incidence detector
        d1 = new NadirSolarIncidenceDetector(1., earth, 10, 1E-12, Action.STOP, false, sun1);
        d2 = new NadirSolarIncidenceDetector(1., earth, 10, 1E-12, Action.STOP, false, sun2);
        checkPropagation(d1, d2);

        // Central body mask circular FOV detector
        d1 = new CentralBodyMaskCircularFOVDetector(sun1, 700000E3, earth, true, Vector3D.PLUS_I, MathLib.PI / 4., 10, 1E-12);
        d2 = new CentralBodyMaskCircularFOVDetector(sun2, 700000E3, earth, true, Vector3D.PLUS_I, MathLib.PI / 4., 10, 1E-12);
        checkPropagation(d1, d2);

        // Eclipse detector
        d1 = new EclipseDetector(sun1, 700000E3, earth, 0, 10, 1E-12);
        d2 = new EclipseDetector(sun2, 700000E3, earth, 0, 10, 1E-12);
        checkPropagation(d1, d2);
        
        // Extrema sight axis detector
        d1 = new ExtremaSightAxisDetector(0, sun1, Vector3D.PLUS_K);
        d2 = new ExtremaSightAxisDetector(0, sun2, Vector3D.PLUS_K);
        checkPropagation(d1, d2);

        // Masking detector
        d1 = new MaskingDetector(sensor, 10, 1E-12);
        d2 = new MaskingDetector(sensor2, 10, 1E-12);
        checkPropagation(d1, d2);

        // Station to sat - related link
        // Distance at visibility is about 622km, which means a signal propagation delay of about 2ms
        // Delay is due to station emission delay, station velocity in GCRF is 465m/s. In 2ms, station moves about 1m
        // 1m is covered by satellite in about 1.5E-4s which correspond to satellite initial date shift for comparison
        final double exp1 = -1.5E-4;

        // Circular FOV detector
        d1 = new CircularFieldOfViewDetector(topo, Vector3D.PLUS_K, 0.01, 1, 1E-12);
        checkPropagationWithStation(d1, exp1);

        // Dihedral FOV detector
        d1 = new DihedralFieldOfViewDetector(topo, Vector3D.PLUS_K, Vector3D.PLUS_I, 0.01,
                Vector3D.PLUS_J, 0.01, 1, 1E-12);
        checkPropagationWithStation(d1, exp1);

        // Sensor visibility detector
        d1 = new SensorVisibilityDetector(sensor, 1, 1E-12);
        checkPropagationWithStation(d1, exp1);

        // Target in field of view detector
        d1 = new TargetInFieldOfViewDetector(sensor, 1, 1E-12);
        checkPropagationWithStation(d1, exp1);

        // Station to satellite mutual visibility detector
        d1 = new StationToSatMutualVisibilityDetector(sensor, station, null, false, 1, 1E-12);
        checkPropagationWithStation(d1, exp1);

        // Sat to station - related link
        // Distance at visibility is about 622km, which means a signal propagation delay of about 2ms
        // Delay is due to station emission delay, station velocity in GCRF is 465m/s. In 2ms, station covers about 1m
        // 1m is covered by satellite in about 1.5E-4s which correspond to satellite initial date shift for comparison
        // Delay is inverted compared to station - satellite link
        final double exp2 = 1.5E-4;

        // Visibility from station detector
        d1 = new VisibilityFromStationDetector(station, null, 1, 1E-12, LinkType.DOWNLINK);
        checkPropagationWithStation(d1, exp2);

        // Apparent elevation (from station) detector
        d1 = new ApparentElevationDetector(MathLib.PI / 2. - 0.01, topo, 1, 1E-12);
        checkPropagationWithStation(d1, exp2);

        // Ground mask elevation detector
        d1 = new GroundMaskElevationDetector(mask, topo);
        checkPropagationWithStation(d1, exp2);

        // Others - Non-regression only

        // RF visibility detector
        d1 = new RFVisibilityDetector(linkBudgetModel, 1., 1, 1E-12);
        checkPropagationWithStation(d1, 1.25E-3);

        // Sensor inhibition detector
        d1 = new SensorInhibitionDetector(sensor, 100, 1E-6);
        d2 = new SensorInhibitionDetector(sensor2, 100, 1E-6);
        checkPropagation(d1, d2);
    }

    /**
     * Build an assembly.
     * @return assembly
     * @throws PatriusException thrown if build failed
     */
    private Assembly buildAssembly() throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        
        // Sensor property
        final SensorProperty sensorProperty = new SensorProperty(Vector3D.PLUS_K);
        sensorProperty.setMainFieldOfView(new CircularField("", MathLib.PI / 1000., Vector3D.PLUS_K));
        sensorProperty.setInhibitionFieldsAndTargets(
                new IFieldOfView[] { new CircularField("", MathLib.PI / 1000., Vector3D.PLUS_K)},
                new PVCoordinatesProvider[] { new MeeusSun(MODEL.BOARD) },
                new LocalRadiusProvider[] { new ConstantRadiusProvider(5000E3)});
        builder.addProperty(sensorProperty, "Main");

        // RF properties
        final double outputPower = 8.;
        final double technoLoss = 0.1;
        final double circuitLoss = 4.1;
        final double bitRate = 838861.;
        final double frequency = 2215920000.;
        final double[] inPatternPolarAngle = new double[10];
        final double[] inPatternAzimuth = new double[37];
        final double[][] inGainPattern = new double[10][37];
        final double[][] inEllipticityFactor = new double[10][37];
        for (int i = 0; i < inPatternPolarAngle.length; i++) {
            inPatternPolarAngle[i] = MathLib.toRadians(i * 10.);
        }
        for (int i = 0; i < inPatternAzimuth.length; i++) {
            inPatternAzimuth[i] = MathLib.toRadians(i * 10.);
        }
        for (int i = 0; i < inEllipticityFactor.length; i++) {
            for (int j = 0; j < inEllipticityFactor[i].length; j++) {
                inGainPattern[i][j] = i * j / 1000;
                inEllipticityFactor[i][j] = i * j / 1000;
            }
        }
        builder.addProperty(new RFAntennaProperty(outputPower, inPatternPolarAngle, inPatternAzimuth, inGainPattern,
            inEllipticityFactor, technoLoss, circuitLoss, bitRate, frequency), "Main");

        builder.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, ""));
        return builder.returnAssembly();
    }

    /**
     * Build an assembly.
     * @return assembly
     * @throws PatriusException thrown if build failed
     */
    private Assembly buildAssembly2() throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        
        final CelestialBody sun2 = new MeeusSun() {
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return CelestialBodyFactory.getSun().getPVCoordinates(date.shiftedBy(-490.6931414907628), frame);
            }
        };

        // Sensor property
        final SensorProperty sensorProperty = new SensorProperty(Vector3D.PLUS_K);
        sensorProperty.setMainFieldOfView(new CircularField("", MathLib.PI / 1000., Vector3D.PLUS_K));
        sensorProperty.setInhibitionFieldsAndTargets(
                new IFieldOfView[] { new CircularField("", MathLib.PI / 1000., Vector3D.PLUS_K)},
                new PVCoordinatesProvider[] { sun2 },
                new LocalRadiusProvider[] { new ConstantRadiusProvider(5000E3)});
        builder.addProperty(sensorProperty, "Main");

        // RF properties
        final double outputPower = 8.;
        final double technoLoss = 0.1;
        final double circuitLoss = 4.1;
        final double bitRate = 838861.;
        final double frequency = 2215920000.;
        final double[] inPatternPolarAngle = new double[10];
        final double[] inPatternAzimuth = new double[37];
        final double[][] inGainPattern = new double[10][37];
        final double[][] inEllipticityFactor = new double[10][37];
        for (int i = 0; i < inPatternPolarAngle.length; i++) {
            inPatternPolarAngle[i] = MathLib.toRadians(i * 10.);
        }
        for (int i = 0; i < inPatternAzimuth.length; i++) {
            inPatternAzimuth[i] = MathLib.toRadians(i * 10.);
        }
        for (int i = 0; i < inEllipticityFactor.length; i++) {
            for (int j = 0; j < inEllipticityFactor[i].length; j++) {
                inGainPattern[i][j] = i * j / 1000;
                inEllipticityFactor[i][j] = i * j / 1000;
            }
        }
        builder.addProperty(new RFAntennaProperty(outputPower, inPatternPolarAngle, inPatternAzimuth, inGainPattern,
            inEllipticityFactor, technoLoss, circuitLoss, bitRate, frequency), "Main");

        builder.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, ""));
        return builder.returnAssembly();
    }

    /**
     * Check detection with delay is detected at expected date compared to an detection with an instantaneous signal.
     * @param detector1 detector to use for light speed propagation
     * @param detector2 detector to use for instantaneous propagation (emitter/receiver is delayed by distance / Light speed)
     * @throws PatriusException thrown if failed
     */
    private void checkPropagation(final AbstractDetector detector1, final AbstractDetector detector2) throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        final AbsoluteDate finalDate = initialDate.shiftedBy(Constants.JULIAN_DAY);

        propagator.setAttitudeProvider(new BodyCenterPointing());
        
        // Propagation, stops at first event detection
        // Light speed
        propagator.addEventDetector(detector1);
        detector1.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getGCRF());
        final AbsoluteDate finalDateLightSpeed = propagator.propagate(initialDate, finalDate).getDate();
        // Instantaneous
        propagator.clearEventsDetectors();
        propagator.addEventDetector(detector2);
        detector2.setPropagationDelayType(PropagationDelayType.INSTANTANEOUS, FramesFactory.getGCRF());
        final AbsoluteDate finalDateInstantaneous = propagator.propagate(initialDate, finalDate).getDate();

        // Check (error should be close to 0)
        final double error = finalDateLightSpeed.durationFrom(finalDateInstantaneous);
        final double epsilon = 2E-2;
        Report.printToReport(detector1.getClass().getSimpleName(), 0, error);
        Assert.assertEquals(0, error, epsilon);
    }

    /**
     * Check detection with delay is detected at expected date compared to an detection with an instantaneous signal.
     * @param detector detector to use
     * @param expectedDelay expected detection delay
     * @throws PatriusException thrown if failed
     */
    private void checkPropagationWithStation(final AbstractDetector detector, final double expectedDelay) throws PatriusException {

        // Propagation, stops at first event detection
        // Light speed
        final AbsoluteDate finalDateLightSpeed = propagate(detector, PropagationDelayType.LIGHT_SPEED, 0);
        // Instantaneous
        final AbsoluteDate finalDateInstantaneous = propagate(detector, PropagationDelayType.INSTANTANEOUS, expectedDelay);

        // Check (error should be close to 0)
        final double error = finalDateLightSpeed.durationFrom(finalDateInstantaneous);
        final double epsilon = 2E-5;
        Report.printToReport(detector.getClass().getSimpleName(), 0, error);
        Assert.assertEquals(0, error, epsilon);
    }

    /**
     * Check detection with delay is detected at expected date compared to an detection with an instantaneous signal.
     * @param detector detector to use
     * @param delayType delay type
     * @param dt initial date shift
     * @throws PatriusException thrown if failed
     */
    private AbsoluteDate propagate(final AbstractDetector detector, final PropagationDelayType delayType,
            final double dt) throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        final AbsoluteDate finalDate = initialDate.shiftedBy(Constants.JULIAN_DAY);

        propagator.setAttitudeProvider(new BodyCenterPointing());
        
        // Propagation, stops at first event detection
        propagator.addEventDetector(detector);
        detector.setPropagationDelayType(delayType, FramesFactory.getGCRF());
        return propagator.propagate(initialDate, finalDate).getDate();
    }
}
