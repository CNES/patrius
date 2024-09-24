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
 * @history creation 30/05/2012
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Ajout de conditions meteorologiques variables dans les modeles de troposphere
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: update due to the refactoring of the tropospheric model classes
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1783:12/11/2018:Global improvement of azimuth elevation field
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
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.groundstation.GeometricStationAntenna;
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
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.ApparentRadiusProvider;
import fr.cnes.sirius.patrius.propagation.events.ConstantRadiusProvider;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.signalpropagation.AngularCorrection;
import fr.cnes.sirius.patrius.signalpropagation.ConstantMeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditions;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.AzoulayModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test class for mutual station to spacecraft visibility detector
 * </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class StationToSatMutualVisibilityTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle mutual station to spacecraft visibility
         * 
         * @featureDescription Detector for the mutual station to spacecraft visibility : the
         *                     spacecraft's sensor sees the station while its apparent position from
         *                     the station is in its mask.
         * 
         * @coveredRequirements DV-VISI_10, DV-VISI_20, DV-VISI_30, DV-VISI_40, DV-EVT_160,
         *                      DV-EVT_150
         */
        MUTUAL_STATION_VISIBILITY
    }

    /** Epsilon for dates comparison. */
    private final double datesComparisonEpsilon = 1.0e-3;

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

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MUTUAL_STATION_VISIBILITY}
     * 
     * @testedMethod {@link StationToSatMutualVisibilityDetector#g(SpacecraftState)}
     * @testedMethod {@link StationToSatMutualVisibilityDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Test of the "mutual station to spacecraft visibility" detector during a
     *              keplerian orbit.
     * 
     * @input A sensor model, a SpacecraftState, a station sensor model
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
    public void mutualVisibilityTest() throws PatriusException {
        Utils.clear();

        // Orbit and propagator initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame EME2000Frame = FramesFactory.getEME2000();

        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY);
        final double a = 7500000.0;
        final Orbit tISSOrbit = new KeplerianOrbit(a, 0.0, MathUtils.HALF_PI, 0.0, 0.0, 0.0,
            PositionAngle.TRUE, EME2000Frame, date, Utils.mu);
        Propagator propagator = new KeplerianPropagator(tISSOrbit, attitudeProv);
        final double period = 2.0 * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);

        // station frame creation
        final double r = 6000000.0;
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(r, 0.0, EME2000Frame);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), MathLib.toRadians(0),
            MathLib.toRadians(180), 0., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "Gstation");

        // station sensor model
        final String nameStationField = "circularStationField";
        final double trueElevationToDetect = 8.6393797973719300E-01;
        final double measuredElevationToDetect = 2.4688155038034136E-04 + trueElevationToDetect;
        final CircularField stationField = new CircularField(nameStationField, MathUtils.HALF_PI
                - measuredElevationToDetect, Vector3D.PLUS_K);
        final GeometricStationAntenna station = new GeometricStationAntenna(topoFrame, stationField);

        // building the assembly
        final String mainBody = "mainBody";
        final String secondPart = "secondPart";
        final String solarPanel = "solarPanel";
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensors
        // main part field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.PLUS_I;
        final IFieldOfView mainField = new CircularField(name, FastMath.PI / 4.0,
            mainFieldDirection);

        // Radius provider
        final ApparentRadiusProvider radius = new ConstantRadiusProvider(0.0);

        // main part sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        sensorProperty.setMainTarget(station, radius);

        // second part field
        final String name2 = "circularField2";
        final Vector3D mainFieldDirection2 = Vector3D.PLUS_I;
        final IFieldOfView mainField2 = new CircularField(name2, FastMath.PI / 10.0,
            mainFieldDirection2);

        // second part sensor property creation
        final SensorProperty sensorProperty2 = new SensorProperty(mainFieldDirection2);
        sensorProperty2.setMainFieldOfView(mainField2);
        sensorProperty2.setMainTarget(station, radius);

        // solar panel geometry property creation
        final Vector3D solarPanelCenter = new Vector3D(0., 0., 20.);
        final SolidShape solarPanelShape = new Plate(solarPanelCenter, Vector3D.PLUS_K,
            Vector3D.PLUS_J, 40., 20.);
        final GeometricProperty solarPanelGeom = new GeometricProperty(solarPanelShape);

        // assembly building
        try {
            // add main part
            builder.addMainPart(mainBody);

            // add second part
            builder.addPart(secondPart, mainBody, Transform.IDENTITY);

            // add sensors
            builder.addProperty(sensorProperty, mainBody);
            builder.addProperty(sensorProperty2, secondPart);

            // add solar panel
            builder.addPart(solarPanel, mainBody, Vector3D.PLUS_I, Rotation.IDENTITY);
            builder.addProperty(solarPanelGeom, solarPanel);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame = new UpdatableFrame(EME2000Frame, Transform.IDENTITY,
                "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // spacecraft sensor model
        final SensorModel mainPartSensorModel = new SensorModel(assembly, mainBody);
        final SensorModel secondPartSensorModel = new SensorModel(assembly, secondPart);

        // tropospheric correction
        final AngularCorrection correctionModel = new AzoulayModel(this.meteoConditionsProvider, this.altitude);

        // FIRST TEST
        // ============
        // the spacecraft leaves the station's field before the station leaves its field

        // event detector
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final EventDetector detectorMainPart = new StationToSatMutualVisibilityDetector(
            mainPartSensorModel, station, correctionModel, false, maxCheck, threshold);
        propagator.addEventDetector(detectorMainPart);

        // physical angle from earth center to detect (the stop date is when the g function is
        // decreasing)
        double angleFromEarthCenter = FastMath.PI - (trueElevationToDetect + FastMath.PI / 2.)
                - MathLib.asin(r * MathLib.sin(trueElevationToDetect + FastMath.PI / 2.) / a);

        // associated date from propagation beginning
        double timeDetected = period * (angleFromEarthCenter / (2. * FastMath.PI) + 1. / 2.);

        // test
        SpacecraftState endState = propagator.propagate(date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetected, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // SECOND TEST
        // =============
        // the spacecraft leaves the station's field after the station leaves its field

        // propagation reset
        propagator = new KeplerianPropagator(tISSOrbit, attitudeProv);

        // detector
        final EventDetector detectorSecondPart = new StationToSatMutualVisibilityDetector(
            secondPartSensorModel, station, correctionModel, false, maxCheck, threshold);
        propagator.addEventDetector(detectorSecondPart);

        // physical angle from earth center to detect (the stop date is when the g function is
        // decreasing)
        angleFromEarthCenter = FastMath.PI - (9. * FastMath.PI / 10.)
                - MathLib.asin(r * MathLib.sin(9. * FastMath.PI / 10.) / a);

        // associated date from propagation beginning
        timeDetected = period * (angleFromEarthCenter / (2. * FastMath.PI) + 1. / 2.);

        // test
        endState = propagator.propagate(date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetected, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        // THRID TEST
        // ============
        // A masking occurs when the spacecraft reaches the zenith of the station
        // masking panel adding
        final String[] maskingParts = { solarPanel };
        mainPartSensorModel.addOwnMaskingParts(maskingParts);

        // propagation reset
        propagator = new KeplerianPropagator(tISSOrbit, attitudeProv);

        // detector
        final EventDetector detectorWithMaskng = new StationToSatMutualVisibilityDetector(
            mainPartSensorModel, station, null, true, maxCheck, threshold);
        propagator.addEventDetector(detectorWithMaskng);

        // associated date from propagation beginning
        timeDetected = period / 2.;

        // test
        endState = propagator.propagate(date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetected, endState.getDate().durationFrom(date),
            this.datesComparisonEpsilon);

        try {
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
            new StationToSatMutualVisibilityDetector(
                mainPartSensorModel, topoFrame, masque, correctionModel, maxCheck, threshold);
        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MUTUAL_STATION_VISIBILITY}
     * 
     * @testedMethod {@link StationToSatMutualVisibilityDetector#StationToSatMutualVisibilityDetector}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: sensorModel, station, correctionModel, boolean withMasking,
     *        the max check value and the threshold value, the CONTINUE Action and the STOP Action.
     * 
     * @output a {@link StationToSatMutualVisibilityDetector}
     * 
     * @testPassCriteria the {@link StationToSatMutualVisibilityDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testConstructor() throws PatriusException {
        Utils.clear();

        // Orbit and propagator initialization
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

        // station sensor model
        final String nameStationField = "circularStationField";
        final double trueElevationToDetect = 8.6393797973719300E-01;
        final double measuredElevationToDetect = 2.4688155038034136E-04 + trueElevationToDetect;
        final CircularField stationField = new CircularField(nameStationField, MathUtils.HALF_PI
                - measuredElevationToDetect, Vector3D.PLUS_K);
        final GeometricStationAntenna station = new GeometricStationAntenna(topoFrame, stationField);

        // building the assembly
        final String mainBody = "mainBody";
        final String secondPart = "secondPart";
        final String solarPanel = "solarPanel";
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensors
        // main part field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.PLUS_I;
        final IFieldOfView mainField = new CircularField(name, FastMath.PI / 4.0,
            mainFieldDirection);

        // Radius provider
        final ApparentRadiusProvider radius = new ConstantRadiusProvider(0.0);

        // main part sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        sensorProperty.setMainTarget(station, radius);

        // second part field
        final String name2 = "circularField2";
        final Vector3D mainFieldDirection2 = Vector3D.PLUS_I;
        final IFieldOfView mainField2 = new CircularField(name2, FastMath.PI / 10.0,
            mainFieldDirection2);

        // second part sensor property creation
        final SensorProperty sensorProperty2 = new SensorProperty(mainFieldDirection2);
        sensorProperty2.setMainFieldOfView(mainField2);
        sensorProperty2.setMainTarget(station, radius);

        // solar panel geometry property creation
        final Vector3D solarPanelCenter = new Vector3D(0., 0., 20.);
        final SolidShape solarPanelShape = new Plate(solarPanelCenter, Vector3D.PLUS_K,
            Vector3D.PLUS_J, 40., 20.);
        final GeometricProperty solarPanelGeom = new GeometricProperty(solarPanelShape);

        // assembly building
        try {
            // add main part
            builder.addMainPart(mainBody);

            // add second part
            builder.addPart(secondPart, mainBody, Transform.IDENTITY);

            // add sensors
            builder.addProperty(sensorProperty, mainBody);
            builder.addProperty(sensorProperty2, secondPart);

            // add solar panel
            builder.addPart(solarPanel, mainBody, Vector3D.PLUS_I, Rotation.IDENTITY);
            builder.addProperty(solarPanelGeom, solarPanel);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame = new UpdatableFrame(EME2000Frame, Transform.IDENTITY,
                "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // spacecraft sensor model
        final SensorModel mainPartSensorModel = new SensorModel(assembly, mainBody);
        new SensorModel(assembly, secondPart);

        // tropospheric correction
        final AngularCorrection correctionModel = new AzoulayModel(this.meteoConditionsProvider, this.altitude);

        // FIRST TEST
        // ============
        // the spacecraft leaves the station's field before the station leaves its field

        // event detector
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final StationToSatMutualVisibilityDetector detectorMainPart = new StationToSatMutualVisibilityDetector(
            mainPartSensorModel, station, correctionModel, false, maxCheck, threshold,
            Action.CONTINUE, Action.STOP);
        // Test getters
        Assert.assertEquals(mainPartSensorModel, detectorMainPart.getSensor());
        Assert.assertEquals(correctionModel, detectorMainPart.getCorrection());

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

        // detector
        final StationToSatMutualVisibilityDetector detector = new StationToSatMutualVisibilityDetector(
            mainPartSensorModel, topoFrame, masque, correctionModel, maxCheck, threshold,
            Action.CONTINUE, Action.STOP);
        // Test getters
        Assert.assertEquals(mainPartSensorModel, detector.getSensor());
        Assert.assertEquals(mainPartSensorModel.getAssembly(), detector.getAssembly());

        final StationToSatMutualVisibilityDetector detectorCopy = (StationToSatMutualVisibilityDetector) detector
            .copy();
        Assert.assertEquals(detector.getMaxCheckInterval(), detectorCopy.getMaxCheckInterval(), 0);

    }
}
