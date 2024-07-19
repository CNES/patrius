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
 * @history created 24/05/12
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Ajout de conditions meteorologiques variables dans les modeles de troposphere
 * VERSION:4.10.2:FA:FA-3289:31/01/2023:[PATRIUS] Problemes sur le masquage d une visi avec LIGHT_TIME
 * VERSION:4.10.1:FA:FA-3281:02/12/2022:[PATRIUS] Inversion DOWNLINK/UPLINK dans SensorModel.celestialBodiesMaskingDistance
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3151:10/05/2022:[PATRIUS] Evolution de la casse VisibilityFromStationDetector
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.6.1:DM:DM-2871:15/03/2021:Changement du sens des Azimuts (Annulation de SIRIUS-FT-2558)
 * VERSION:4.6:DM:DM-2586:27/01/2021:[PATRIUS] intersection entre un objet de type «ExtendedOneAxisEllipsoid» et une droite. 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: update due to the refactoring of the tropospheric model classes
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:394:20/03/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.assembly.properties.GeometricProperty;
import fr.cnes.sirius.patrius.assembly.properties.SensorProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeFrame;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.TargetGroundPointing;
import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.sensor.VisibilityFromStationDetector.LinkType;
import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.TranslatedFrame;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.groundstation.GeometricStationAntenna;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.propagation.events.ApparentRadiusProvider;
import fr.cnes.sirius.patrius.propagation.events.ConstantRadiusProvider;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.signalpropagation.AngularCorrection;
import fr.cnes.sirius.patrius.signalpropagation.ConstantMeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditions;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.AzoulayModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Unit tests for {@link VisibilityFromStationDetector}.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class VisibilityFromStationDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Visibility from station
         * 
         * @featureDescription Validate the apparent visibility from station detector with user
         *                     tropospheric correction
         * 
         * @coveredRequirements DV-VISI_10, DV-VISI_20, DV-VISI_30, DV-VISI_40
         */
        VISIBILITY_FROM_STATION_DETECTOR
    }

    /** temperature [K] */
    final double temperature = 20. + 273.16;

    /** pressure [Pa] */
    final double pressure = 102000;

    /** humidity or moisture (percent) */
    final double humidity = 20;

    /** Meteorological conditions provider */
    final MeteorologicalConditions meteoConditions = new MeteorologicalConditions(this.pressure, this.temperature,
        this.humidity);
    final MeteorologicalConditionsProvider meteoConditionsProvider = new ConstantMeteorologicalConditionsProvider(
        this.meteoConditions);

    /** geodetic altitude [m] */
    final double altitude = 150;

    /** Epsilon for dates comparison. */
    private final double datesComparisonEpsilon = 1.0e-3;

    /**
     * @throws PatriusException frame exception
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_FROM_STATION_DETECTOR}
     * 
     * @testedMethod {@link VisibilityFromStationDetector#g(SpacecraftState)}
     * @testedMethod {@link VisibilityFromStationDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Test of the apparent satellite apparent entering in a station's field of view,
     *              with a tropospheric correction
     * 
     * @input a simple polar circular orbit, a station model
     * 
     * @output the detected event's dates
     * 
     * @testPassCriteria the dates are the expected ones
     * 
     * @referenceVersion 1.2
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testApparentSpacecraftVisibilityDetector() throws PatriusException {

        // Orbit initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame EME2000Frame = FramesFactory.getEME2000();

        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY);
        final double a = 7500000.0;
        final Orbit tISSOrbit = new KeplerianOrbit(a, 0.0, MathUtils.HALF_PI, 0.0, 0.0, 0.0,
            PositionAngle.TRUE, EME2000Frame, date, Utils.mu);
        final Propagator propagator = new KeplerianPropagator(tISSOrbit, attitudeProv);
        final double period = 2.0 * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);

        // station frame creation
        final double r = 6000000.0;
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(r, 0.0, EME2000Frame);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), MathLib.toRadians(0),
            MathLib.toRadians(180), 0., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "Gstation");

        // tropospheric correction
        final AngularCorrection correctionModel = new AzoulayModel(this.meteoConditionsProvider, this.altitude);

        // reference angles values from the FDS algorithm
        final double trueElevationToDetect = 8.6393797973719300E-01;
        final double measuredElevationToDetect = 2.4688155038034136E-04 + trueElevationToDetect;

        // station model
        final IFieldOfView field = new CircularField("circular", MathUtils.HALF_PI
                - measuredElevationToDetect, Vector3D.PLUS_K);
        final GeometricStationAntenna stationModel = new GeometricStationAntenna(topoFrame, field);

        final EventDetector detector = new VisibilityFromStationDetector(stationModel,
            correctionModel, 10.0, AbstractDetector.DEFAULT_THRESHOLD, LinkType.DOWNLINK);

        propagator.addEventDetector(detector);

        // physical angle from earth center to detect (the stop date is when the g function is
        // decreasing)
        final double angleFromEarthCenter = FastMath.PI
                - (trueElevationToDetect + FastMath.PI / 2.)
                - MathLib.asin(r * MathLib.sin(trueElevationToDetect + FastMath.PI / 2.) / a);

        // associated date from propagation beginning
        final double timeDetected = period * (angleFromEarthCenter / (2. * FastMath.PI) + 1. / 2.);

        // test
        final SpacecraftState endState = propagator.propagate(date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetected, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);
    }

    /**
     * @throws PatriusException frame exception
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_FROM_STATION_DETECTOR}
     * 
     * @testedMethod {@link VisibilityFromStationDetector#g(SpacecraftState)}
     * @testedMethod {@link VisibilityFromStationDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Test of the apparent satellite apparent entering in a station's field of view,
     *              without any tropospheric correction
     * 
     * @input a simple polar circular orbit, a station model
     * 
     * @output the detected event's dates
     * 
     * @testPassCriteria the dates are the expected ones
     * 
     * @referenceVersion 1.2
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSpacecraftVisibilityDetector() throws PatriusException {
        // Orbit initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame EME2000Frame = FramesFactory.getEME2000();

        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY);
        final double a = 7500000.0;
        final Orbit tISSOrbit = new KeplerianOrbit(a, 0.0, MathUtils.HALF_PI, 0.0, 0.0, 0.0,
            PositionAngle.TRUE, EME2000Frame, date, Utils.mu);
        final Propagator propagator = new KeplerianPropagator(tISSOrbit, attitudeProv);
        final double period = 2.0 * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);

        // station frame creation
        final double r = 6000000.0;
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(r, 0.0, EME2000Frame);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), MathLib.toRadians(0),
            MathLib.toRadians(180), 0., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "Gstation");

        // reference angles values from the FDS algorithm
        final double trueElevationToDetect = 8.6393797973719300E-01;

        // station model
        final IFieldOfView field = new CircularField("circular", MathUtils.HALF_PI
                - trueElevationToDetect, Vector3D.PLUS_K);
        final GeometricStationAntenna stationModel = new GeometricStationAntenna(topoFrame, field);

        // create detector with downlink type of link
        final EventDetector detectorDownlink = new VisibilityFromStationDetector(stationModel, null, 10.0,
            AbstractDetector.DEFAULT_THRESHOLD, LinkType.DOWNLINK);

        propagator.addEventDetector(detectorDownlink);

        // physical angle from earth center to detect (the stop date is when the g function is
        // decreasing)
        final double angleFromEarthCenterDownlink = FastMath.PI
                - (trueElevationToDetect + FastMath.PI / 2.)
                - MathLib.asin(r * MathLib.sin(trueElevationToDetect + FastMath.PI / 2.) / a);

        // associated date from propagation beginning
        final double timeDetectedDownlink = period * (angleFromEarthCenterDownlink / (2. * FastMath.PI) + 1. / 2.);

        // test
        final SpacecraftState endStateDownlink = propagator.propagate(date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetectedDownlink, endStateDownlink.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // create detector with uplink type of link
        final EventDetector detectorUplink = new VisibilityFromStationDetector(stationModel, null, 10.0,
            AbstractDetector.DEFAULT_THRESHOLD, LinkType.UPLINK);

        propagator.clearEventsDetectors();
        propagator.addEventDetector(detectorUplink);

        // physical angle from earth center to detect (the stop date is when the g function is
        // decreasing)
        final double angleFromEarthCenterUplink = FastMath.PI
                - (trueElevationToDetect + FastMath.PI / 2.)
                - MathLib.asin(r * MathLib.sin(trueElevationToDetect + FastMath.PI / 2.) / a);

        // associated date from propagation beginning
        final double timeDetectedUplink = period * (angleFromEarthCenterUplink / (2. * FastMath.PI) + 1. / 2.);

        // test
        final SpacecraftState endStateUplink = propagator.propagate(date.shiftedBy(-10000.0));

        Assert.assertEquals(timeDetectedUplink, endStateUplink.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // create detector with invalid type of link
        final EventDetector detectorInvalidLinkType = new VisibilityFromStationDetector(stationModel, null, 10.0,
            AbstractDetector.DEFAULT_THRESHOLD, null);

        propagator.clearEventsDetectors();
        propagator.addEventDetector(detectorInvalidLinkType);

        try {
            // test
            propagator.propagate(date.shiftedBy(-10000.0));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // the exception has been caught as expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_FROM_STATION_DETECTOR}
     * 
     * @testedMethod {@link VisibilityFromStationDetector#g(SpacecraftState)}
     * @testedMethod {@link VisibilityFromStationDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Test satellite sensor's masking by occulting bodies (between sensor and station). The satellite
     *              orbits in Moon's equatorial plane, and targets a station on the Earth equator (longitude 180).
     *              Three cases are tested: no visibility from Earth station, visibility with masking by the Moon and
     *              visibility without masking by the Moon (the Moon lying behind the satellite in this case).
     *              Intersections calculations are performed to confirm detector's results.
     * 
     * @input circular equatorial orbit around the Moon, a sensor model, a SpacecraftState, a station model
     * 
     * @output detector's g value, intersections computation
     * 
     * @testPassCriteria the g function has the expected sign
     * 
     * @throws PatriusException if a frame problem occurs
     * 
     * @referenceVersion 4.10
     * 
     * @nonRegressionVersion 4.10
     */
    @Test
    public void bodyOccultationTest() throws PatriusException {
        Utils.clear();
        Utils.setDataRoot("regular-dataPBASE");

        // Orbit and Moon initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final Frame EME2000Frame = FramesFactory.getEME2000();
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final Frame moonEme2000 = new TranslatedFrame(EME2000Frame, moon, "Moon_EME2000");
        Orbit orbit = new KeplerianOrbit(Constants.MOON_EQUATORIAL_RADIUS + 250000, 0.0, 0.0, 0.0, 0.0, 0.0,
            PositionAngle.TRUE, moonEme2000, date0, moon.getGM());
        final double orbitPeriod = orbit.getKeplerianPeriod();

        // station frame creation
        final double r = 6000000.0;
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(r, 0.0, EME2000Frame);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), MathLib.toRadians(0),
            MathLib.toRadians(180), 0.1, "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "Gstation");

        // station model
        final IFieldOfView field = new CircularField("station_circular", MathLib.PI / 4.0, Vector3D.PLUS_K);
        final GeometricStationAntenna station = new GeometricStationAntenna(topoFrame, field);

        // attitude law
        final AttitudeProvider attitudeProv = new TargetGroundPointing(point);

        // building the assembly
        final String mainBody = "mainBody";
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensors
        // main part field
        final Vector3D mainFieldDirection = Vector3D.PLUS_K;
        final IFieldOfView mainField = new CircularField("sensor_circular", MathLib.PI / 4.0, mainFieldDirection);

        // Radius provider
        final ApparentRadiusProvider radius = new ConstantRadiusProvider(0.0);

        // main part sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        sensorProperty.setMainTarget(station, radius);

        // assembly building
        try {
            // add main part
            builder.addMainPart(mainBody);

            // add sensor to the main part
            builder.addProperty(sensorProperty, mainBody);

            // assembly INITIAL link to the tree of frames
            final AttitudeFrame attFrame = new AttitudeFrame(orbit, attitudeProv, moonEme2000);
            final UpdatableFrame mainFrame = new UpdatableFrame(attFrame, Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // spacecraft sensor model with the Earth and the Moon as occulting bodies
        final SensorModel sensorModel = new SensorModel(assembly, mainBody);
        sensorModel.addMaskingCelestialBody(moon.getShape());

        // FIRST TEST
        // ============
        // Satellite is visible by the station but the Moon crosses satellite's line of sight.
        // Test spacecraft getter as well

        // Detector
        // create detector with downlink type of link, add the Earth and the Moon as occulting bodies
        // stops when the Moon crosses the line of sight as the station is masked
        final AbstractDetector detectorOccultation = new VisibilityFromStationDetector(station, sensorModel, null,
            true, 50.0, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP, false, false,
            LinkType.DOWNLINK);

        // Test spacecraft getter
        Assert.assertTrue(((VisibilityFromStationDetector) detectorOccultation).getAssembly() == assembly);

        // Propagator with detector
        Propagator propagator = new KeplerianPropagator(orbit, attitudeProv);
        propagator.addEventDetector(detectorOccultation);

        // Test visibility and no masking at the beginning
        // The satellite is in station's FoV
        Assert.assertTrue(station.getFOV().isInTheField(orbit.getPVCoordinates(date0, topoFrame).getPosition()));
        // Satellite's line of sight does not cross the Moon
        Vector3D sightAxis = sensorModel.getSightAxis(moonEme2000, date0);
        Vector3D satPos = orbit.getPVCoordinates(date0, moonEme2000).getPosition();
        Line line = Line.createLine(satPos, sightAxis, satPos);
        BodyPoint[] intersectionsMoon = moon.getShape().getIntersectionPoints(line, moonEme2000, date0);
        // Assert g > 0 (visibility with station, no masking) and no intersection with the Moon
        Assert.assertTrue(detectorOccultation.g(propagator.getInitialState()) > 0);
        Assert.assertTrue(intersectionsMoon.length == 0);

        // Propagate
        SpacecraftState endState = propagator.propagate(date0.shiftedBy(orbitPeriod));
        AbsoluteDate endDate = endState.getDate();

        // The satellite is in station's FoV
        Assert.assertTrue(station.getFOV().isInTheField(orbit.getPVCoordinates(endDate, topoFrame).getPosition()));
        // Satellite's line of sight crosses the Moon
        sightAxis = sensorModel.getSightAxis(moonEme2000, endDate);
        satPos = orbit.getPVCoordinates(endDate, moonEme2000).getPosition();
        line = Line.createLine(satPos, sightAxis, satPos);
        intersectionsMoon = moon.getShape().getIntersectionPoints(line, moonEme2000, endDate);
        // Assert g < 0 (no visibility because of masking) and intersection with the Moon
        Assert.assertTrue(detectorOccultation.g(endState) < 0);
        Assert.assertTrue(intersectionsMoon.length > 0);

        // SECOND TEST
        // =============
        // Additional test case
        // Satellite is visible by the station and the Moon is behind (reverted LoS intersects the Moon)

        // This date is chosen so that the Moon is behind sensor's line of sight
        endDate = new AbsoluteDate("2000-01-01T12:00:00.000");

        // Propagate
        propagator = new KeplerianPropagator(orbit, attitudeProv);
        propagator.addEventDetector(detectorOccultation);
        endState = propagator.propagate(endDate);

        // Check that the state date is the expected date (no event detected)
        Assert.assertEquals(0., endState.getDate().durationFrom(endDate), this.datesComparisonEpsilon);

        // Test visibility and no masking
        // The satellite is in station's FoV
        Assert.assertTrue(station.getFOV().isInTheField(orbit.getPVCoordinates(endDate, topoFrame).getPosition()));
        // Satellite's line of sight does not cross the Moon
        sightAxis = sensorModel.getSightAxis(moonEme2000, endDate);
        satPos = orbit.getPVCoordinates(endDate, moonEme2000).getPosition();
        line = Line.createLine(satPos, sightAxis, satPos);
        intersectionsMoon = moon.getShape().getIntersectionPoints(line, moonEme2000, endDate);
        Assert.assertTrue(detectorOccultation.g(endState) > 0);
        Assert.assertTrue(intersectionsMoon.length == 0);
        // Satellite's reverted line of sight does intersect
        final Line revertedLine = line.revert();
        intersectionsMoon = moon.getShape().getIntersectionPoints(revertedLine, moonEme2000, endDate);
        Assert.assertTrue(intersectionsMoon.length > 0);

        // THIRD TEST
        // ============
        // Satellite is not visible by the station and the Moon crosses the line of sight
        // The g function is negative all along but its magnitude changes because of the Moon
        // A propagation of 1 orbit duration ensures the Moon crosses the LoS without triggering event

        // The orbit is shifted by half a day: no visibility from the station (wrong side)
        orbit = orbit.shiftedBy(Constants.JULIAN_DAY / 2);
        final AbsoluteDate date1 = orbit.getDate();
        endDate = date1.shiftedBy(orbitPeriod);

        final AbstractDetector detectorNoVisibility = new VisibilityFromStationDetector(station, sensorModel, null,
            true, 50.0, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP, false, false,
            LinkType.DOWNLINK);

        // Propagator with detector
        propagator = new KeplerianPropagator(orbit, attitudeProv);
        propagator.addEventDetector(detectorNoVisibility);

        // Test no visibility at beginning
        // The satellite is NOT in station's FoV
        Assert.assertFalse(station.getFOV().isInTheField(orbit.getPVCoordinates(date1, topoFrame).getPosition()));
        Assert.assertTrue(detectorNoVisibility.g(propagator.getInitialState()) < 0);

        // Use a step handler to check that the value of g changes radically when the Moon crosses the LoS
        propagator.setMasterMode(600., new TestStepHandler(detectorNoVisibility, sensorModel, moon.getShape()));

        // Propagate
        endState = propagator.propagate(endDate);

        // Check that the state date is the expected date (no event detected)
        Assert.assertEquals(0., endState.getDate().durationFrom(endDate), this.datesComparisonEpsilon);

        // Test no visibility
        // The satellite is NOT in station's FoV
        Assert.assertFalse(station.getFOV().isInTheField(orbit.getPVCoordinates(endDate, topoFrame).getPosition()));
        Assert.assertTrue(detectorNoVisibility.g(endState) < 0);

        // Satellite's line of sight does not cross the Moon
        sightAxis = sensorModel.getSightAxis(moonEme2000, endDate);
        satPos = orbit.getPVCoordinates(endDate, moonEme2000).getPosition();
        line = Line.createLine(satPos, sightAxis, satPos);
        intersectionsMoon = moon.getShape().getIntersectionPoints(line, moonEme2000, endDate);
        Assert.assertTrue(detectorNoVisibility.g(endState) < 0);
        Assert.assertTrue(intersectionsMoon.length == 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_FROM_STATION_DETECTOR}
     * 
     * @testedMethod {@link VisibilityFromStationDetector#g(SpacecraftState)}
     * @testedMethod {@link VisibilityFromStationDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Check visibility from station with body occultation when light speed is taken into account (downlink
     *              and uplink)
     * 
     * @testPassCriteria dates are coherent (no regression only, FD check has been performed beforehand)
     * 
     * @referenceVersion 4.10.2
     * 
     * @nonRegressionVersion 4.10.2
     */
    @Test
    public void bodyOccultationLightSpeedTest() throws PatriusException {
        Utils.clear();
        Utils.setDataRoot("regular-dataPBASE");

        // Orbit and Moon initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final Frame EME2000Frame = FramesFactory.getEME2000();
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final Frame moonEme2000 = new TranslatedFrame(EME2000Frame, moon, "Moon_EME2000");
        final Orbit orbit = new KeplerianOrbit(Constants.MOON_EQUATORIAL_RADIUS + 250000, 0.0, 0.0, 0.0, 0.0, 0.0,
            PositionAngle.TRUE, moonEme2000, date0, moon.getGM());
        final double orbitPeriod = orbit.getKeplerianPeriod();

        // station frame creation
        final double r = 6000000.0;
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(r, 0.0, EME2000Frame);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), MathLib.toRadians(0),
            MathLib.toRadians(180), 0.1, "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "Gstation");

        // station model
        final IFieldOfView field = new CircularField("station_circular", MathLib.PI / 4.0, Vector3D.PLUS_K);
        final GeometricStationAntenna station = new GeometricStationAntenna(topoFrame, field);

        // attitude law
        final AttitudeProvider attitudeProv = new TargetGroundPointing(point);

        // building the assembly
        final String mainBody = "mainBody";
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensors
        // main part field
        final Vector3D mainFieldDirection = Vector3D.PLUS_K;
        final IFieldOfView mainField = new CircularField("sensor_circular", MathLib.PI / 4.0, mainFieldDirection);

        // Radius provider
        final ApparentRadiusProvider radius = new ConstantRadiusProvider(0.0);

        // main part sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        sensorProperty.setMainTarget(station, radius);

        // assembly building
        try {
            // add main part
            builder.addMainPart(mainBody);

            // add sensor to the main part
            builder.addProperty(sensorProperty, mainBody);

            // assembly INITIAL link to the tree of frames
            final AttitudeFrame attFrame = new AttitudeFrame(orbit, attitudeProv, moonEme2000);
            final UpdatableFrame mainFrame = new UpdatableFrame(attFrame, Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // spacecraft sensor model with the the Moon as occulting bodies
        final SensorModel sensorModel = new SensorModel(assembly, mainBody);
        sensorModel.addMaskingCelestialBody(moon.getShape());

        // Downlink case

        // create detector with downlink type of link, add the Earth and the Moon as occulting bodies
        // stops when the Moon crosses the line of sight as the station is masked
        final VisibilityFromStationDetector detector1 = new VisibilityFromStationDetector(station, sensorModel, null,
            true, 50.0, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP, false, false,
            LinkType.DOWNLINK);
        detector1.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getICRF());

        // Propagator with detector
        final Propagator propagator1 = new KeplerianPropagator(orbit, attitudeProv);
        propagator1.addEventDetector(detector1);

        // Propagate
        final SpacecraftState endState1 = propagator1.propagate(date0.shiftedBy(orbitPeriod));
        final AbsoluteDate endDate1 = endState1.getDate();

        // Non-regression (accuracy: 1ms) after DV check
        Assert.assertTrue(endDate1.durationFrom(new AbsoluteDate("2000-01-01T12:59:26.198")) < 1E-3);

        // Uplink case

        // create detector with downlink type of link, add the Earth and the Moon as occulting bodies
        // stops when the Moon crosses the line of sight as the station is masked
        final VisibilityFromStationDetector detector2 = new VisibilityFromStationDetector(station, sensorModel, null,
            true, 50.0, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP, false, false,
            LinkType.UPLINK);
        detector2.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getICRF());

        // Propagator with detector
        final Propagator propagator2 = new KeplerianPropagator(orbit, attitudeProv);
        propagator2.addEventDetector(detector2);

        // Propagate
        final SpacecraftState endState2 = propagator2.propagate(date0.shiftedBy(orbitPeriod));
        final AbsoluteDate endDate2 = endState2.getDate();

        // Non-regression (accuracy: 1ms) after DV check
        Assert.assertTrue(endDate2.durationFrom(new AbsoluteDate("2000-01-01T12:59:26.211")) < 1E-3);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_FROM_STATION_DETECTOR}
     * 
     * @testedMethod {@link VisibilityFromStationDetector#g(SpacecraftState)}
     * @testedMethod {@link VisibilityFromStationDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Check visibility from station with spacecraft occultation when light speed is taken into account
     *              (downlink and uplink)
     *              This test is the same as bodyOccultationLightSpeedTest, except the occulting body is replaced by a
     *              spacecraft
     * 
     * @testPassCriteria dates are coherent (no regression only, FD check has been performed beforehand)
     * 
     * @referenceVersion 4.10.2
     * 
     * @nonRegressionVersion 4.10.2
     */
    @Test
    public void spacecraftOccultationLightSpeedTest() throws PatriusException {
        Utils.clear();
        Utils.setDataRoot("regular-dataPBASE");

        // Orbit and Moon initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final Frame EME2000Frame = FramesFactory.getEME2000();
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final Frame moonEme2000 = new TranslatedFrame(EME2000Frame, moon, "Moon_EME2000");
        final Orbit orbit = new KeplerianOrbit(Constants.MOON_EQUATORIAL_RADIUS + 250000, 0.0, 0.0, 0.0, 0.0, 0.0,
            PositionAngle.TRUE, moonEme2000, date0, moon.getGM());
        final double orbitPeriod = orbit.getKeplerianPeriod();

        // station frame creation
        final double r = 6000000.0;
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(r, 0.0, EME2000Frame);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), MathLib.toRadians(0),
            MathLib.toRadians(180), 0.1, "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "Gstation");

        // station model
        final IFieldOfView field = new CircularField("station_circular", MathLib.PI / 4.0, Vector3D.PLUS_K);
        final GeometricStationAntenna station = new GeometricStationAntenna(topoFrame, field);

        // attitude law
        final AttitudeProvider attitudeProv = new TargetGroundPointing(point);

        // building the assembly
        final String mainBody = "mainBody";
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensors
        // main part field
        final Vector3D mainFieldDirection = Vector3D.PLUS_K;
        final IFieldOfView mainField = new CircularField("sensor_circular", MathLib.PI / 4.0, mainFieldDirection);

        // Radius provider
        final ApparentRadiusProvider radius = new ConstantRadiusProvider(0.0);

        // main part sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        sensorProperty.setMainTarget(station, radius);

        // assembly building
        try {
            // add main part
            builder.addMainPart(mainBody);

            // add sensor to the main part
            builder.addProperty(sensorProperty, mainBody);

            // assembly INITIAL link to the tree of frames
            final AttitudeFrame attFrame = new AttitudeFrame(orbit, attitudeProv, moonEme2000);
            final UpdatableFrame mainFrame = new UpdatableFrame(attFrame, Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // 2nd assembly building (occulting spacecraft) - Same size as the Moon
        final AssemblyBuilder builder2 = new AssemblyBuilder();
        builder2.addMainPart("MaskingSpacecraftMainPart");
        builder2.addProperty(new GeometricProperty(new Sphere(Vector3D.ZERO, Constants.IERS92_MOON_EQUATORIAL_RADIUS)),
            "MaskingSpacecraftMainPart");
        builder2.initMainPartFrame(new UpdatableFrame(moon.getEME2000(), Transform.IDENTITY, "moonFrame"));
        final Assembly assembly2 = builder2.returnAssembly();

        // spacecraft sensor model with the the Moon as occulting bodies
        final SensorModel sensorModel = new SensorModel(assembly, mainBody);
        final AttitudeProvider moonAttitudeLaw = new ConstantAttitudeLaw(moonEme2000, Rotation.IDENTITY);
        final Orbit orbitMoonInit = new CartesianOrbit(moon.getPVCoordinates(date0, moonEme2000), moonEme2000, date0,
            moon.getGM());
        // Simplest propagator simulating the Moon as a SpacecraftState
        final Propagator propagatorMasking = new KeplerianPropagator(orbitMoonInit){
            /** Serializable UID. */
            private static final long serialVersionUID = -326810234224644590L;

            /** {@inheritDoc} */
            @Override
            public SpacecraftState propagate(final AbsoluteDate target) throws PropagationException {
                try {
                    final Orbit orbitMoon = new CartesianOrbit(moon.getPVCoordinates(target, moonEme2000), moonEme2000,
                        target, moon.getGM());
                    return new SpacecraftState(orbitMoon, moonAttitudeLaw.getAttitude(orbitMoon));
                } catch (final PatriusException e) {
                    throw new PropagationException(e);
                }
            }
        };
        propagatorMasking.setAttitudeProvider(moonAttitudeLaw);
        final SecondarySpacecraft maskingSpacecraft = new SecondarySpacecraft(assembly2, propagatorMasking,
            "MaskingSpacecraft");
        sensorModel.addSecondaryMaskingSpacecraft(maskingSpacecraft, new String[] { "MaskingSpacecraftMainPart" });

        // Downlink case

        // create detector with downlink type of link, add the Earth and the Moon as occulting bodies
        // stops when the Moon crosses the line of sight as the station is masked
        final VisibilityFromStationDetector detector1 = new VisibilityFromStationDetector(station, sensorModel, null,
            true, 50.0, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP, false, false,
            LinkType.DOWNLINK);
        detector1.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getICRF());

        // Propagator with detector
        final Propagator propagator1 = new KeplerianPropagator(orbit, attitudeProv);
        propagator1.addEventDetector(detector1);

        // Propagate
        final SpacecraftState endState1 = propagator1.propagate(date0.shiftedBy(orbitPeriod));
        final AbsoluteDate endDate1 = endState1.getDate();

        // Non-regression (accuracy: 1ms) after DV check
        Assert.assertTrue(endDate1.durationFrom(new AbsoluteDate("2000-01-01T12:59:26.198")) < 1E-3);

        // Uplink case

        // create detector with downlink type of link, add the Earth and the Moon as occulting bodies
        // stops when the Moon crosses the line of sight as the station is masked
        final VisibilityFromStationDetector detector2 = new VisibilityFromStationDetector(station, sensorModel, null,
            true, 50.0, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP, false, false,
            LinkType.UPLINK);
        detector2.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getICRF());

        // Propagator with detector
        final Propagator propagator2 = new KeplerianPropagator(orbit, attitudeProv);
        propagator2.addEventDetector(detector2);

        // Propagate
        final SpacecraftState endState2 = propagator2.propagate(date0.shiftedBy(orbitPeriod));
        final AbsoluteDate endDate2 = endState2.getDate();

        // Non-regression (accuracy: 1ms) after DV check
        Assert.assertTrue(endDate2.durationFrom(new AbsoluteDate("2000-01-01T12:59:26.211")) < 1E-3);
    }

    /**
     * @throws PatriusException frame exception
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_FROM_STATION_DETECTOR}
     * 
     * @testedMethod {@link VisibilityFromStationDetector#g(SpacecraftState)}
     * @testedMethod {@link VisibilityFromStationDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Test of the apparent satellite apparent entering in a station's field of view,
     *              without any tropospheric correction, field defined by an elevation mask. this
     *              test validates that the behaviour with an elevation mask and no tropospheric
     *              correction is the same the one of the orekit's detector.
     * 
     * @input a simple polar circular orbit, an elevation mask
     * 
     * @output the detected event's dates
     * 
     * @testPassCriteria the dates are the expected ones
     * 
     * @referenceVersion 1.2
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGroundMask() throws PatriusException {

        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        final double mu = 3.9860047e14;
        final double ae = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), date, mu);

        final Propagator propagator = new EcksteinHechlerPropagator(orbit, ae, mu,
            orbit.getFrame(), c20, c30, c40, c50, c60, ParametersType.OSCULATING);

        // Earth and frame
        // flattening
        final double f = 1.0 / 298.257223563;
        // terrestrial frame at an arbitrary date
        final Frame ITRF2005 = FramesFactory.getITRF();
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
            MathLib.toRadians(48.833), MathLib.toRadians(2.333), 0., "");
        final TopocentricFrame topo = new TopocentricFrame(point, "Gstation");
        final double[][] masque = { { MathLib.toRadians(0), MathLib.toRadians(5) },
            { MathLib.toRadians(30), MathLib.toRadians(4) },
            { MathLib.toRadians(60), MathLib.toRadians(3) },
            { MathLib.toRadians(90), MathLib.toRadians(2) },
            { MathLib.toRadians(120), MathLib.toRadians(3) },
            { MathLib.toRadians(150), MathLib.toRadians(4) },
            { MathLib.toRadians(180), MathLib.toRadians(5) },
            { MathLib.toRadians(210), MathLib.toRadians(6) },
            { MathLib.toRadians(240), MathLib.toRadians(5) },
            { MathLib.toRadians(270), MathLib.toRadians(4) },
            { MathLib.toRadians(300), MathLib.toRadians(3) },
            { MathLib.toRadians(330), MathLib.toRadians(4) } };
        final VisibilityFromStationDetector detector = new VisibilityFromStationDetector(topo, masque,
            null, 10.0, AbstractDetector.DEFAULT_THRESHOLD, LinkType.DOWNLINK){
            /** Serializable UID. */
            private static final long serialVersionUID = 7515758050410436713L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return increasing ? Action.STOP : Action.CONTINUE;
            }
        };

        final AbsoluteDate startDate = new AbsoluteDate(2003, 9, 15, 20, 0, 0, utc);
        propagator.resetInitialState(propagator.propagate(startDate));
        propagator.addEventDetector(detector);
        final SpacecraftState fs = propagator.propagate(startDate.shiftedBy(Constants.JULIAN_DAY));
        final double elevation = topo.getElevation(fs.getPVCoordinates().getPosition(),
            fs.getFrame(), fs.getDate());
        Assert.assertEquals(0.065, elevation, 2.0e-5);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_FROM_STATION_DETECTOR}
     * 
     * @testedMethod {@link VisibilityFromStationDetector#VisibilityFromStationDetector}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: topoFrame,azimElevMask,correctionModel, the max check value
     *        and the threshold value and the CONTINUE and STOP Action.
     * 
     * @output a {@link VisibilityFromStationDetector}
     * 
     * @testPassCriteria the {@link VisibilityFromStationDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testConstructor() throws PatriusException {
        // Orbit initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame EME2000Frame = FramesFactory.getEME2000();

        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY);
        final double a = 7500000.0;
        final Orbit tISSOrbit = new KeplerianOrbit(a, 0.0, MathUtils.HALF_PI, 0.0, 0.0, 0.0,
            PositionAngle.TRUE, EME2000Frame, date, Utils.mu);
        new KeplerianPropagator(tISSOrbit, attitudeProv);
        MathLib.sqrt(a * a * a / Utils.mu);

        // station frame creation
        final double r = 6000000.0;
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(r, 0.0, EME2000Frame);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), MathLib.toRadians(0),
            MathLib.toRadians(180), 0., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "Gstation");

        // tropospheric correction
        final AngularCorrection correctionModel = new AzoulayModel(this.meteoConditionsProvider, this.altitude);

        // reference angles values from the FDS algorithm
        final double trueElevationToDetect = 8.6393797973719300E-01;
        final double measuredElevationToDetect = 2.4688155038034136E-04 + trueElevationToDetect;

        // station model
        final IFieldOfView field = new CircularField("circular", MathUtils.HALF_PI
                - measuredElevationToDetect, Vector3D.PLUS_K);
        final GeometricStationAntenna stationModel = new GeometricStationAntenna(topoFrame, field);

        // Test constructor VisibilityFromStationDetector(GeometricStationAntenna, AngularCorrection,
        // double, double, Action, Action, LinkType)
        final VisibilityFromStationDetector detector = new VisibilityFromStationDetector(
            stationModel, correctionModel, 10.0, AbstractDetector.DEFAULT_THRESHOLD,
            Action.CONTINUE, Action.STOP, LinkType.DOWNLINK);
        // The constructor did not crash...
        Assert.assertNotNull(detector);

        final TopocentricFrame topo = new TopocentricFrame(point, "Gstation");
        final double[][] masque = { { MathLib.toRadians(0), MathLib.toRadians(5) },
            { MathLib.toRadians(30), MathLib.toRadians(4) },
            { MathLib.toRadians(60), MathLib.toRadians(3) },
            { MathLib.toRadians(90), MathLib.toRadians(2) },
            { MathLib.toRadians(120), MathLib.toRadians(3) },
            { MathLib.toRadians(150), MathLib.toRadians(4) },
            { MathLib.toRadians(180), MathLib.toRadians(5) },
            { MathLib.toRadians(210), MathLib.toRadians(6) },
            { MathLib.toRadians(240), MathLib.toRadians(5) },
            { MathLib.toRadians(270), MathLib.toRadians(4) },
            { MathLib.toRadians(300), MathLib.toRadians(3) },
            { MathLib.toRadians(330), MathLib.toRadians(4) } };

        // Test constructor VisibilityFromStationDetector(TopocentricFrame, double[][], AngularCorrection,
        // double, double, Action, Action, LinkType)
        final VisibilityFromStationDetector detector2 = new VisibilityFromStationDetector(topo, masque,
            null, 10.0, AbstractDetector.DEFAULT_THRESHOLD, Action.RESET_STATE, Action.CONTINUE,
            LinkType.DOWNLINK);

        // The constructor did not crash...
        Assert.assertNotNull(detector2);

        // Test eventOccurred
        Assert.assertEquals(Action.RESET_STATE, detector2.eventOccurred(null, true, true));
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(null, false, true));

        // Test constructor VisibilityFromStationDetector(GeometricStationAntenna, AngularCorrection,
        // int, double, double, Action, boolean, LinkType) and its copy
        final VisibilityFromStationDetector detector3 =
            (VisibilityFromStationDetector) new VisibilityFromStationDetector(
                new GeometricStationAntenna(topo, masque), null, 0, 10.0,
                AbstractDetector.DEFAULT_THRESHOLD, Action.RESET_STATE, false, LinkType.DOWNLINK)
                .copy();
        // Test constructor VisibilityFromStationDetector(GeometricStationAntenna, AngularCorrection,
        // int, double, double, Action, boolean, LinkType) and its copy
        final VisibilityFromStationDetector detector4 =
            (VisibilityFromStationDetector) new VisibilityFromStationDetector(
                new GeometricStationAntenna(topo, masque), null, 1, 10.0,
                AbstractDetector.DEFAULT_THRESHOLD, Action.RESET_STATE, false, LinkType.DOWNLINK)
                .copy();
        // Test constructor VisibilityFromStationDetector(GeometricStationAntenna, AngularCorrection,
        // int, double, double, Action, boolean, LinkType) and its copy
        final VisibilityFromStationDetector detector5 =
            (VisibilityFromStationDetector) new VisibilityFromStationDetector(
                new GeometricStationAntenna(topo, masque), null, 2, 10.0,
                AbstractDetector.DEFAULT_THRESHOLD, Action.RESET_STATE, false, LinkType.DOWNLINK)
                .copy();
        Assert.assertEquals(Action.RESET_STATE, detector3.eventOccurred(null, true, true));
        Assert.assertEquals(Action.RESET_STATE, detector4.eventOccurred(null, false, true));
        Assert.assertEquals(Action.RESET_STATE, detector5.eventOccurred(null, true, true));
        Assert.assertEquals(Action.RESET_STATE, detector5.eventOccurred(null, false, true));

        // Copy
        final VisibilityFromStationDetector detectorCopy = (VisibilityFromStationDetector) detector5
            .copy();
        Assert.assertEquals(detector5.getMaxCheckInterval(), detectorCopy.getMaxCheckInterval(), 0);

        // Test constructor (TopocentricFrame, double[][], AngularCorrection, double, double, LinkType)
        // and its copy
        final VisibilityFromStationDetector detector6 =
            (VisibilityFromStationDetector) new VisibilityFromStationDetector(topo, masque, null, 10.0,
                AbstractDetector.DEFAULT_THRESHOLD, LinkType.UPLINK)
                .copy();
        // Check that the detector has been correctly created
        Assert.assertNotNull(detector6);
        // Check that the type of link has been correctly set
        Assert.assertEquals(LinkType.UPLINK, detector6.getLinkType());
        // Check occurrence of events
        Assert.assertEquals(Action.CONTINUE, detector6.eventOccurred(null, true, true));
        Assert.assertEquals(Action.STOP, detector6.eventOccurred(null, false, true));

        // Test constructor (TopocentricFrame, double[][], AngularCorrection, double, double, Action,
        // Action, boolean, boolean, LinkType) and its copy
        final VisibilityFromStationDetector detector7 =
            (VisibilityFromStationDetector) new VisibilityFromStationDetector(topo, masque, null, 10.0,
                AbstractDetector.DEFAULT_THRESHOLD, Action.RESET_STATE, Action.CONTINUE, false, false,
                LinkType.DOWNLINK).copy();
        // Check that the detector has been correctly created
        Assert.assertNotNull(detector7);
        // Check that the type of link has been correctly set
        Assert.assertEquals(LinkType.DOWNLINK, detector7.getLinkType());
        // Check occurrence of events
        Assert.assertEquals(Action.RESET_STATE, detector7.eventOccurred(null, true, true));
        Assert.assertEquals(Action.CONTINUE, detector7.eventOccurred(null, false, true));

        // Test constructor VisibilityFromStationDetector(GeometricStationAntenna, AngularCorrection,
        // double, double, LinkType) and its copy
        final VisibilityFromStationDetector detector8 =
            (VisibilityFromStationDetector) new VisibilityFromStationDetector(
                new GeometricStationAntenna(topo, masque), null, 10.0,
                AbstractDetector.DEFAULT_THRESHOLD, LinkType.UPLINK).copy();
        // Check that the detector has been correctly created
        Assert.assertNotNull(detector8);
        // Check that the type of link has been correctly set
        Assert.assertEquals(LinkType.UPLINK, detector8.getLinkType());
        // Check occurrence of events
        Assert.assertEquals(Action.CONTINUE, detector8.eventOccurred(null, true, true));
        Assert.assertEquals(Action.STOP, detector8.eventOccurred(null, false, true));

        // Test constructor VisibilityFromStationDetector(GeometricStationAntenna, AngularCorrection,
        // double, double, Action, Action, boolean, boolean, LinkType) and its copy
        final VisibilityFromStationDetector detector9 =
            (VisibilityFromStationDetector) new VisibilityFromStationDetector(
                new GeometricStationAntenna(topo, masque), null, 10.0,
                AbstractDetector.DEFAULT_THRESHOLD, Action.RESET_STATE, Action.CONTINUE, false,
                false, LinkType.DOWNLINK).copy();
        // Check that the detector has been correctly created
        Assert.assertNotNull(detector9);
        // Check that the type of link has been correctly set
        Assert.assertEquals(LinkType.DOWNLINK, detector9.getLinkType());
        // Check occurrence of events
        Assert.assertEquals(Action.RESET_STATE, detector9.eventOccurred(null, true, true));
        Assert.assertEquals(Action.CONTINUE, detector9.eventOccurred(null, false, true));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() throws PatriusException {
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(Constants.CNES_STELA_AE,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getGCRF());
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), MathLib.toRadians(0),
            MathLib.toRadians(180), 0., "");
        // tropospheric correction
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "Gstation");

        // tropospheric correction
        final AngularCorrection correctionModel = new AzoulayModel(this.meteoConditionsProvider, this.altitude);

        // reference angles values from the FDS algorithm
        final double trueElevationToDetect = 8.6393797973719300E-01;
        final double measuredElevationToDetect = 2.4688155038034136E-04 + trueElevationToDetect;

        // station model
        final IFieldOfView field = new CircularField("circular", MathUtils.HALF_PI
                - measuredElevationToDetect, Vector3D.PLUS_K);
        final GeometricStationAntenna stationModel = new GeometricStationAntenna(topoFrame, field);

        final VisibilityFromStationDetector detector = new VisibilityFromStationDetector(
            stationModel, correctionModel, 10.0, AbstractDetector.DEFAULT_THRESHOLD,
            Action.CONTINUE, Action.STOP, LinkType.DOWNLINK);

        Assert.assertEquals(point.getLLHCoordinates().getLatitude(), detector.getStationBodyPoint().getLLHCoordinates()
            .getLatitude(),
            0);
        Assert.assertEquals(earth.getBodyFrame().getName(), detector.getBodyShape().getBodyFrame()
            .getName());
        Assert.assertEquals(Action.CONTINUE, detector.getActionAtEntry());
        Assert.assertEquals(Action.STOP, detector.getActionAtExit());
        Assert.assertEquals(false, detector.isRemoveAtEntry());
        Assert.assertEquals(false, detector.isRemoveAtExit());
        Assert.assertEquals(LinkType.DOWNLINK, detector.getLinkType());
        Assert.assertEquals(correctionModel, detector.getCorrection());
        Assert.assertEquals(stationModel, detector.getStation());

        // Test getNativeFrame
        Assert.assertEquals(stationModel.getTopoFrame(), stationModel.getNativeFrame(null, null));
    }

    /**
     * Step handler used for tests with occultations.
     * 
     * @author astrucma
     */
    private static class TestStepHandler implements PatriusFixedStepHandler {

        /**
         * Default serial UID
         */
        private static final long serialVersionUID = 1L;

        /**
         * Detector
         */
        private final AbstractDetector detector;

        /**
         * Sensor
         */
        private final SensorModel sensor;

        /**
         * The Moon shape
         */
        private final BodyShape moon;

        /**
         * Constructor
         * 
         * @param detector detector
         * @param sensor sensor
         * @param moon the Moon shape
         */
        public TestStepHandler(final AbstractDetector detector, final SensorModel sensor, final BodyShape moon) {
            super();
            this.detector = detector;
            this.sensor = sensor;
            this.moon = moon;
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            // Do nothing
        }

        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast) {

            // Initializations
            final Frame sFrame = currentState.getFrame();
            final AbsoluteDate sDate = currentState.getDate();
            boolean intersectsMoon = false;
            double value = 0;

            try {
                value = this.detector.g(currentState);
                final Vector3D sightAxis = this.sensor.getSightAxis(sFrame, sDate);
                final Vector3D satPos = currentState.getPVCoordinates().getPosition();
                final Line line = Line.createLine(satPos, sightAxis, satPos);
                final BodyPoint[] intersectionsMoon = this.moon.getIntersectionPoints(line, sFrame, sDate);
                intersectsMoon = intersectionsMoon.length != 0;
            } catch (final PatriusException pex) {
                // Do nothing
            }

            // Assert that the g value is greater than the Earth-Moon min distance (~350 000 km) if intersections
            // happen. Otherwise the g value is below pi/2 (absolute value) as it corresponds to an angular distance
            // with station's FoV.
            // The g value is negative in any case as there is no visibility in the context that uses this step handler
            Assert.assertTrue(value < 0);
            Assert.assertTrue(value < -350E6 == intersectsMoon);
            Assert.assertTrue(value > -MathLib.PI / 2 == !intersectsMoon);
        }
    }
}
