/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history creation 20/04/2012
 * 
 * HISTORY
* VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
* VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:512:08/02/2016:Corrected the target angular radius computation
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.BasicPVCoordinatesProvider;
import fr.cnes.sirius.patrius.assembly.properties.SensorProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.events.sensor.SensorVisibilityDetector;
import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.ConstantRadiusProvider;
import fr.cnes.sirius.patrius.propagation.events.LocalRadiusProvider;
import fr.cnes.sirius.patrius.propagation.events.VariableRadiusProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description
 *              <p>
 *              TEst class for the sensor model
 *              </p>
 * 
 * @see SensorModel
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class SensorModelTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle sensor model
         * 
         * @featureDescription sensor model for an assembly
         * 
         * @coveredRequirements DV-VEHICULE_180, DV-VEHICULE_190, DV-VEHICULE_200, DV-VEHICULE_210,
         *                      DV-VEHICULE_220, DV-VEHICULE_230, DV-VEHICULE_240, DV-VEHICULE_290, DV-VISI_70,
         *                      DV-VISI_90, DV-VISI_50
         */
        SENSOR_MODEL
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * Main part's name
     */
    private final String mainBody = "mainBody";
    /**
     * 2nd part's name
     */
    private final String part2 = "part2";

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SENSOR_MODEL}
     * 
     * @testedMethod {@link SensorModel#isMainTargetInField(AbsoluteDate)}
     * @testedMethod {@link SensorModel#noInhibition(AbsoluteDate)}
     * @testedMethod {@link SensorModel#visibilityOk(AbsoluteDate)}
     * @testedMethod {@link SensorModel#getTargetCenterFOVAngle(AbsoluteDate)}
     * 
     * @description Creation of a sensor model and test of the methods associated to the fields of view
     * 
     * @input An assembly containing a part with a sensor property
     * 
     * @output booleans (visibility or not), angular distances
     * 
     * @testPassCriteria the main visibility is "true", the inhibitions "false", the angular distance
     *                   is PI/8
     * 
     * @referenceVersion 1.2
     * @nonRegressionVersion 1.2
     * @throws PatriusException
     */
    @Test
    public final void fieldsOfViewTest() throws PatriusException {

        // Constant radius provider
        final ConstantRadiusProvider zeroRadius = new ConstantRadiusProvider(0.0);

        // frame and date
        final UpdatableFrame mainFrame = new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY,
            "paperback writer");
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // main field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.PLUS_K;
        final CircularField mainField = new CircularField(name, FastMath.PI / 8.0, mainFieldDirection);

        // main target
        final Vector3D mainTargetPos = new Vector3D(-2.0, -1.0, 45.0);
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(mainTargetPos, Vector3D.ZERO);
        final PVCoordinatesProvider mainTarget = new BasicPVCoordinatesProvider(mainTargetCoordinates, mainFrame);
        final double mainTargetRadius = 0.0;

        // inhibition fields
        final Vector3D inhibitDirection1 = Vector3D.PLUS_I;
        final Vector3D inhibitDirection2 = Vector3D.PLUS_J;
        final CircularField[] inhibitFields = new CircularField[2];
        inhibitFields[0] = new CircularField(name, FastMath.PI / 8.0, inhibitDirection1);
        inhibitFields[1] = new CircularField(name, FastMath.PI / 8.0, inhibitDirection2);

        // inhibition targets
        final Vector3D inhibitionTargetPos1 = new Vector3D(50.0, 0.0, 0.0);
        final PVCoordinates inhibitionTargetCoordinates1 = new PVCoordinates(inhibitionTargetPos1, Vector3D.ZERO);
        final Vector3D inhibitionTargetPos2 = new Vector3D(0.0, 0.0, 0.0);
        final PVCoordinates inhibitionTargetCoordinates2 = new PVCoordinates(inhibitionTargetPos2, Vector3D.ZERO);
        final PVCoordinatesProvider[] inhibitionTargets = new PVCoordinatesProvider[2];
        inhibitionTargets[0] = new BasicPVCoordinatesProvider(inhibitionTargetCoordinates1, mainFrame);
        inhibitionTargets[1] = new BasicPVCoordinatesProvider(inhibitionTargetCoordinates2, mainFrame);
        final LocalRadiusProvider[] inhibitionRadiuses = { zeroRadius, zeroRadius };

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // wrong property creation
        try {
            final SensorProperty sensorProperty1 = new SensorProperty(mainFieldDirection);
            final PVCoordinatesProvider[] wrongInhibitionTargets = new PVCoordinatesProvider[1];
            wrongInhibitionTargets[0] = new BasicPVCoordinatesProvider(inhibitionTargetCoordinates1, mainFrame);
            sensorProperty1.setInhibitionFieldsAndTargets(inhibitFields, wrongInhibitionTargets, inhibitionRadiuses);
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            final SensorProperty sensorProperty1 = new SensorProperty(mainFieldDirection);
            final LocalRadiusProvider[] wrongInhibitionRadiuses = { zeroRadius, zeroRadius };

            sensorProperty1.setInhibitionFieldsAndTargets(inhibitFields, inhibitionTargets, wrongInhibitionRadiuses);
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            final SensorProperty sensorProperty1 = new SensorProperty(mainFieldDirection);
            final LocalRadiusProvider[] wrongInhibitionRadiuses = { zeroRadius, new ConstantRadiusProvider(-1.0) };
            sensorProperty1.setInhibitionFieldsAndTargets(inhibitFields, inhibitionTargets, wrongInhibitionRadiuses);
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            final SensorProperty sensorProperty1 = new SensorProperty(mainFieldDirection);
            sensorProperty1.setMainTarget(mainTarget, new ConstantRadiusProvider(-1.0));
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // main part test
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add another part
            final Vector3D translation = new Vector3D(-2.0, -1.0, -5.0);
            final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);
            builder.addPart(this.part2, this.mainBody, transform1);

            // sensor property creation
            final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
            sensorProperty.setMainFieldOfView(mainField);
            sensorProperty.setMainTarget(mainTarget, new ConstantRadiusProvider(mainTargetRadius));
            sensorProperty.setInhibitionFieldsAndTargets(inhibitFields, inhibitionTargets, inhibitionRadiuses);
            final Vector3D[] refAxis = new Vector3D[3];
            refAxis[0] = Vector3D.MINUS_I;
            refAxis[2] = Vector3D.MINUS_K;
            refAxis[1] = Vector3D.MINUS_J;

            sensorProperty.setReferenceAxis(refAxis);

            builder.addProperty(sensorProperty, this.part2);

            // assembly link to the tree of frames
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        // sensor model
        final SensorModel sensorModel = new SensorModel(assembly, this.part2);

        // visibility tests
        Assert.assertTrue(sensorModel.isMainTargetInField(date));
        Assert.assertTrue(!sensorModel.noInhibition(date));
        Assert.assertTrue(!sensorModel.visibilityOk(date));

        // angular distance test
        Assert.assertEquals(FastMath.PI / 8.0, sensorModel.getTargetCenterFOVAngle(date), this.comparisonEpsilon);

        // sight axis test
        final Vector3D ax = sensorModel.getSightAxis(mainFrame, date);
        Assert.assertEquals(0.0, ax.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, ax.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, ax.getZ(), this.comparisonEpsilon);

        // reference axis test
        final Vector3D[] axis = sensorModel.getRefrenceAxis(mainFrame, date);
        Assert.assertEquals(-1.0, axis[0].getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, axis[0].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, axis[0].getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, axis[1].getX(), this.comparisonEpsilon);
        Assert.assertEquals(-1.0, axis[1].getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, axis[1].getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, axis[2].getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, axis[2].getY(), this.comparisonEpsilon);
        Assert.assertEquals(-1.0, axis[2].getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SENSOR_MODEL}
     * 
     * @testedMethod {@link SensorModel#getTargetDihedralAngles(AbsoluteDate)}
     * @testedMethod {@link SensorModel#getTargetVectorInSensorFrame(AbsoluteDate)}
     * @testedMethod {@link SensorModel#getTargetRefAxisElevation(AbsoluteDate, int)}
     * @testedMethod {@link SensorModel#getTargetRefAxisAngle(AbsoluteDate, int)}
     * @testedMethod {@link SensorModel#getTargetSightAxisElevation(AbsoluteDate)}
     * @testedMethod {@link SensorModel#getTargetSightAxisAngle(AbsoluteDate)}
     * 
     * @description Creation of a sensor model and test of the methods associated to the fields of view
     * 
     * @input An assembly containing a part with a sensor property
     * 
     * @output measures
     * 
     * @testPassCriteria the angular measures values are the expected ones
     * 
     * @referenceVersion 1.2
     * @nonRegressionVersion 1.2
     * @throws PatriusException
     */
    @Test
    public final void measuresTest() throws PatriusException {

        // Constant radius provider
        final ConstantRadiusProvider zeroRadius = new ConstantRadiusProvider(0.0);

        // frame and date
        final UpdatableFrame mainFrame = new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "penny lane");
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D mainFieldDirection = new Vector3D(0.0, 1.0, 1.0);

        // main target
        Vector3D mainTargetPos = new Vector3D(-2.0, -1.0, 45.0);
        PVCoordinates mainTargetCoordinates = new PVCoordinates(mainTargetPos, Vector3D.ZERO);
        PVCoordinatesProvider mainTarget = new BasicPVCoordinatesProvider(mainTargetCoordinates, mainFrame);

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // main part test
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add another part
            final Vector3D translation = new Vector3D(-2.0, -1.0, -5.0);
            final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);
            builder.addPart(this.part2, this.mainBody, transform1);

            // sensor property creation
            final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
            sensorProperty.setMainTarget(mainTarget, zeroRadius);
            final Vector3D[] refAxis = new Vector3D[3];
            refAxis[0] = Vector3D.MINUS_I;
            refAxis[1] = Vector3D.MINUS_J;
            refAxis[2] = Vector3D.MINUS_K;
            sensorProperty.setReferenceAxis(refAxis);

            builder.addProperty(sensorProperty, this.part2);

            // assembly link to the tree of frames
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        // sensor model
        final SensorModel sensorModel = new SensorModel(assembly, this.part2);

        // directing cosine test
        Vector3D targetInFrame = sensorModel.getNormalisedTargetVectorInSensorFrame(date);
        Assert.assertEquals(0.0, targetInFrame.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, targetInFrame.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, targetInFrame.getZ(), this.comparisonEpsilon);

        // dihedral angles test
        final double[] angles = sensorModel.getTargetDihedralAngles(date);
        Assert.assertEquals(FastMath.PI / 2.0, angles[0], this.comparisonEpsilon);
        Assert.assertEquals(0.0, angles[1], this.comparisonEpsilon);
        Assert.assertEquals(0.0, angles[2], this.comparisonEpsilon);

        // vector angles
        Assert.assertEquals(FastMath.PI / 4.0, sensorModel.getTargetSightAxisAngle(date), this.comparisonEpsilon);
        Assert.assertEquals(FastMath.PI / 2.0, sensorModel.getTargetRefAxisAngle(date, 1), this.comparisonEpsilon);
        Assert.assertEquals(FastMath.PI / 2.0, sensorModel.getTargetRefAxisAngle(date, 2), this.comparisonEpsilon);
        Assert.assertEquals(FastMath.PI, sensorModel.getTargetRefAxisAngle(date, 3), this.comparisonEpsilon);

        // elevation angles
        Assert.assertEquals(FastMath.PI / 4.0, sensorModel.getTargetSightAxisElevation(date), this.comparisonEpsilon);
        Assert.assertEquals(0.0, sensorModel.getTargetRefAxisElevation(date, 1), this.comparisonEpsilon);
        Assert.assertEquals(0.0, sensorModel.getTargetRefAxisElevation(date, 2), this.comparisonEpsilon);
        Assert.assertEquals(-FastMath.PI / 2.0, sensorModel.getTargetRefAxisElevation(date, 3), this.comparisonEpsilon);

        // main target setting
        mainTargetPos = new Vector3D(-2.0, 49.0, -5.0);
        mainTargetCoordinates = new PVCoordinates(mainTargetPos, Vector3D.ZERO);
        mainTarget = new BasicPVCoordinatesProvider(mainTargetCoordinates, mainFrame);

        sensorModel.setMainTarget(mainTarget, zeroRadius);

        // directing cosine test
        targetInFrame = sensorModel.getNormalisedTargetVectorInSensorFrame(date);
        Assert.assertEquals(0.0, targetInFrame.getX(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, targetInFrame.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, targetInFrame.getZ(), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SENSOR_MODEL}
     * 
     * @testedMethod {@link SensorModel#getMainTargetAngularRadius(AbsoluteDate)}
     * 
     * @description Creation of a sensor model and test the computation of the angular radius of
     *              the target (Earth) with a low equatorial orbit
     * 
     * @input An assembly containing a part with a sensor property, a low equatorial orbit
     * 
     * @output The angular radius of the target
     * 
     * @testPassCriteria The angle computed should be around the one expected (around PI with such orbit)
     * 
     * @referenceVersion 3.2
     * @nonRegressionVersion 3.2
     * @throws PatriusException
     */
    @Test
    public final void lowOrbitTest1() throws PatriusException {

        // Earth radius
        final double earthRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;

        // Low equatorial orbit and propagator:

        // Attitude law : constant attitude law
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);

        final AbsoluteDate date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());
        final double mu = Constants.WGS84_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit((1 + this.comparisonEpsilon) * earthRadius, .0, .0, 0, 0,
            -FastMath.PI / 2.0, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
        final double t = orbit.getKeplerianPeriod();
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, attitudeProv);

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensor
        // main field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.MINUS_K;
        final CircularField mainField = new CircularField(name, FastMath.PI / 4.0, mainFieldDirection);
        
        // main target : center of the earth !
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);
        final PVCoordinatesProvider mainTarget = new BasicPVCoordinatesProvider(mainTargetCoordinates,
            FramesFactory.getEME2000());

        // sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        sensorProperty.setMainTarget(mainTarget, new ConstantRadiusProvider(earthRadius));

        // assembly building
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add sensor
            builder.addProperty(sensorProperty, this.mainBody);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame =
                new UpdatableFrame(FramesFactory.getEME2000(), Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // sensor model
        final SensorModel sensor = new SensorModel(assembly, this.mainBody);

        // event detector
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final SensorVisibilityDetector detector = new SensorVisibilityDetector(sensor, maxCheck, threshold);

        // add event detector
        propagator.addEventDetector(detector);

        // expected angle
        final double expectedAngle = FastMath.PI;
        final double epsilonAngle = 1.0E-6;

        for (int i = 0; i < t; i++) {
            propagator.propagate(date.shiftedBy(i));
            final double actualAngle = sensor.getMainTargetAngularRadius(date.shiftedBy(i));
            Assert.assertEquals(expectedAngle, 2 * actualAngle, epsilonAngle);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SENSOR_MODEL}
     * 
     * @testedMethod {@link SensorModel#getMainTargetAngularRadius(AbsoluteDate)}
     * 
     * @description Creation of a sensor model and test the computation of the angular radius of
     *              the target (Earth) with a low equatorial orbit.
     *              This test is a duplicate of test {@link #lowOrbitTest1()} with the new implementation of the
     *              {@link SensorModel} taking into account for the main target radius a radius provider and not a
     *              double any more.
     *              Here, a variable radius provider with a null flattening is considered so the results must be exactly
     *              the same.
     * 
     * @input An assembly containing a part with a sensor property, a low equatorial orbit
     * 
     * @output The angular radius of the target
     * 
     * @testPassCriteria The angle computed should be around the one expected (around PI with such orbit) with
     *                   the used variable radius provider at flattening 0.0.
     * 
     * @referenceVersion 3.3
     * @nonRegressionVersion 3.3
     * @throws PatriusException
     */
    @Test
    public final void lowOrbitWithVariableRadiusTest1() throws PatriusException {

        // Earth radius
        final double earthRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;

        // Low equatorial orbit and propagator:

        // Attitude law : constant attitude law
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);

        final AbsoluteDate date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());
        final double mu = Constants.WGS84_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit((1 + this.comparisonEpsilon) * earthRadius, .0, .0, 0, 0,
            -FastMath.PI / 2.0, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
        final double t = orbit.getKeplerianPeriod();
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, attitudeProv);

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensor
        // main field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.MINUS_K;
        final CircularField mainField = new CircularField(name, FastMath.PI / 4.0, mainFieldDirection);

        // main target : center of the earth !
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);
        final PVCoordinatesProvider mainTarget = new BasicPVCoordinatesProvider(mainTargetCoordinates,
            FramesFactory.getEME2000());

        // Variable radius provider with null flattening
        final EllipsoidBodyShape earthNullFlattening = new ExtendedOneAxisEllipsoid(earthRadius,
            0.0,
            FramesFactory.getITRF(), "Earth");
        final LocalRadiusProvider earthRadiusProvider = new VariableRadiusProvider(earthNullFlattening);

        // sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        sensorProperty.setMainTarget(mainTarget, earthRadiusProvider);

        // assembly building
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add sensor
            builder.addProperty(sensorProperty, this.mainBody);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame =
                new UpdatableFrame(FramesFactory.getEME2000(), Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // sensor model
        final SensorModel sensor = new SensorModel(assembly, this.mainBody);

        // event detector
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final SensorVisibilityDetector detector = new SensorVisibilityDetector(sensor, maxCheck, threshold);

        // add event detector
        propagator.addEventDetector(detector);

        // expected angle
        final double expectedAngle = FastMath.PI;
        final double epsilonAngle = 1.0E-6;

        for (int i = 0; i < t; i++) {
            propagator.propagate(date.shiftedBy(i));
            final double actualAngle = sensor.getMainTargetAngularRadius(date.shiftedBy(i));
            Assert.assertEquals(expectedAngle, 2 * actualAngle, epsilonAngle);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SENSOR_MODEL}
     * 
     * @testedMethod {@link SensorModel#getInhibitionTargetAngularRadius(AbsoluteDate, int)}
     * 
     * @description Creation of a sensor model and test the computation of the angular radius of
     *              the target (Earth) with a low equatorial orbit. Same test as {@link #lowOrbitTest1()}, except
     *              that the tested method work with several targets : here, only 1 target is considered.
     * 
     * @input An assembly containing a part with a sensor property, a low equatorial orbit
     * 
     * @output The angular radius of the target
     * 
     * @testPassCriteria The angle computed should be around the one expected (around PI with such orbit)
     * 
     * @referenceVersion 3.2
     * @nonRegressionVersion 3.2
     * @throws PatriusException
     */
    @Test
    public final void lowOrbitTest2() throws PatriusException {

        // Earth radius provider
        final double earthRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final ConstantRadiusProvider earthRadiusProvider = new ConstantRadiusProvider(earthRadius);

        // Low equatorial orbit and propagator:

        // Attitude law : constant attitude law
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);

        final AbsoluteDate date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());
        final double mu = Constants.WGS84_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit((1 + this.comparisonEpsilon) * earthRadius, .0, .0, 0, 0,
            -FastMath.PI / 2.0, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
        final double t = orbit.getKeplerianPeriod();
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, attitudeProv);

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensor
        // main field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.MINUS_K;
        final CircularField[] mainField = new CircularField[1];
        mainField[0] = new CircularField(name, FastMath.PI / 4.0, mainFieldDirection);

        // main target : center of the earth !
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);

        final PVCoordinatesProvider[] mainTarget = new PVCoordinatesProvider[1];
        mainTarget[0] = new BasicPVCoordinatesProvider(mainTargetCoordinates, FramesFactory.getEME2000());
        final LocalRadiusProvider[] inhibitionTargetsRadiuses = { earthRadiusProvider };

        // sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField[0]);
        sensorProperty.setInhibitionFieldsAndTargets(mainField, mainTarget, inhibitionTargetsRadiuses);
        sensorProperty.setMainTarget(mainTarget[0], earthRadiusProvider);

        // assembly building
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add sensor
            builder.addProperty(sensorProperty, this.mainBody);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame =
                new UpdatableFrame(FramesFactory.getEME2000(), Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // sensor model
        final SensorModel sensor = new SensorModel(assembly, this.mainBody);

        // event detector
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final SensorVisibilityDetector detector = new SensorVisibilityDetector(sensor, maxCheck, threshold);

        // add event detector
        propagator.addEventDetector(detector);

        // expected angle
        final double expectedAngle = FastMath.PI;
        final double epsilonAngle = 1.0E-6;

        for (int i = 0; i < t; i++) {
            propagator.propagate(date.shiftedBy(i));
            final double actualAngle = sensor.getInhibitionTargetAngularRadius(date.shiftedBy(i), 1);
            Assert.assertEquals(expectedAngle, 2 * actualAngle, epsilonAngle);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SENSOR_MODEL}
     * 
     * @testedMethod {@link SensorModel#getInhibitionTargetAngularRadius(AbsoluteDate, int)}
     * 
     * @description Creation of a sensor model and test the computation of the angular radius of
     *              the target (Earth) with a low equatorial orbit.
     *              This test is a duplicate of test {@link #lowOrbitTest2()} with the new implementation of the
     *              {@link SensorModel} taking into account for the main target radius a radius provider and not a
     *              double any more.
     *              Here, a variable radius provider with a null flattening is considered so the results must be exactly
     *              the same.
     * 
     *              Same test as {@link #lowOrbitTest1()}, except that the tested method work with several targets :
     *              here, only 1 target is considered.
     * 
     * @input An assembly containing a part with a sensor property, a low equatorial orbit
     * 
     * @output The angular radius of the target
     * 
     * @testPassCriteria The angle computed should be around the one expected (around PI with such orbit) with
     *                   the used variable radius provider at flattening 0.0.
     * 
     * @referenceVersion 3.3
     * @nonRegressionVersion 3.3
     * @throws PatriusException
     */
    @Test
    public final void lowOrbitWithVariableRadiusTest2() throws PatriusException {

        // Earth radius provider : variable provider with null flattening
        final double earthRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final EllipsoidBodyShape earthNullFlattening = new ExtendedOneAxisEllipsoid(earthRadius,
            0.0,
            FramesFactory.getITRF(), "Earth");

        final LocalRadiusProvider earthRadiusProvider = new VariableRadiusProvider(earthNullFlattening);

        // Low equatorial orbit and propagator:

        // Attitude law : constant attitude law
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);

        final AbsoluteDate date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());
        final double mu = Constants.WGS84_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit((1 + this.comparisonEpsilon) * earthRadius, .0, .0, 0, 0,
            -FastMath.PI / 2.0, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
        final double t = orbit.getKeplerianPeriod();
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, attitudeProv);

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensor
        // main field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.MINUS_K;
        final CircularField[] mainField = new CircularField[1];
        mainField[0] = new CircularField(name, FastMath.PI / 4.0, mainFieldDirection);

        // main target : center of the earth !
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);

        final PVCoordinatesProvider[] mainTarget = new PVCoordinatesProvider[1];
        mainTarget[0] = new BasicPVCoordinatesProvider(mainTargetCoordinates, FramesFactory.getEME2000());
        final LocalRadiusProvider[] inhibitionTargetsRadiuses = { earthRadiusProvider };

        // sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField[0]);
        sensorProperty.setInhibitionFieldsAndTargets(mainField, mainTarget, inhibitionTargetsRadiuses);
        sensorProperty.setMainTarget(mainTarget[0], earthRadiusProvider);

        // assembly building
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add sensor
            builder.addProperty(sensorProperty, this.mainBody);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame =
                new UpdatableFrame(FramesFactory.getEME2000(), Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // sensor model
        final SensorModel sensor = new SensorModel(assembly, this.mainBody);

        // event detector
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final SensorVisibilityDetector detector = new SensorVisibilityDetector(sensor, maxCheck, threshold);

        // add event detector
        propagator.addEventDetector(detector);

        // expected angle
        final double expectedAngle = FastMath.PI;
        final double epsilonAngle = 1.0E-6;

        for (int i = 0; i < t; i++) {
            propagator.propagate(date.shiftedBy(i));
            final double actualAngle = sensor.getInhibitionTargetAngularRadius(date.shiftedBy(i), 1);
            Assert.assertEquals(expectedAngle, 2 * actualAngle, epsilonAngle);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SENSOR_MODEL}
     * 
     * @testedMethod {@link SensorModel#noInhibition(AbsoluteDate)}
     * 
     * @description Creation of a sensor model and test if the target (Earth) is in the inhibition
     *              field of the sensor during the propagation of a low equatorial orbit
     * 
     * @input An assembly containing a part with a sensor property, a low equatorial orbit
     * @output True if no inhibition occur at given date, false otherwise
     * 
     * @testPassCriteria The method should return false, since the Earth is always in the field during
     *                   the propagation so there are inhibitions
     * 
     * @referenceVersion 3.2
     * @nonRegressionVersion 3.2
     * @throws PatriusException
     */
    @Test
    public final void NoInhibitionTest() throws PatriusException {

        // Earth radius provider
        final double earthRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final ConstantRadiusProvider earthRadiusProvider = new ConstantRadiusProvider(earthRadius);

        // Low equatorial orbit and propagator:

        // Attitude law : constant attitude law
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);

        final AbsoluteDate date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());
        final double mu = Constants.WGS84_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit((1 + this.comparisonEpsilon) * earthRadius, .0, .0, 0, 0,
            -FastMath.PI / 2.0, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
        final double t = orbit.getKeplerianPeriod();
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, attitudeProv);

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensor
        // main field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.MINUS_K;
        final CircularField[] mainField = new CircularField[1];
        mainField[0] = new CircularField(name, FastMath.PI / 4.0, mainFieldDirection);

        // main target : center of the earth !
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);

        final PVCoordinatesProvider[] mainTarget = new PVCoordinatesProvider[1];
        mainTarget[0] = new BasicPVCoordinatesProvider(mainTargetCoordinates, FramesFactory.getEME2000());
        final LocalRadiusProvider[] inhibitionTargetsRadiuses = { earthRadiusProvider };

        // sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField[0]);
        sensorProperty.setInhibitionFieldsAndTargets(mainField, mainTarget, inhibitionTargetsRadiuses);
        sensorProperty.setMainTarget(mainTarget[0], earthRadiusProvider);

        // assembly building
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add sensor
            builder.addProperty(sensorProperty, this.mainBody);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame =
                new UpdatableFrame(FramesFactory.getEME2000(), Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // sensor model
        final SensorModel sensor = new SensorModel(assembly, this.mainBody);

        // event detector
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final SensorVisibilityDetector detector = new SensorVisibilityDetector(sensor, maxCheck, threshold);

        // add event detector
        propagator.addEventDetector(detector);

        for (int i = 0; i < t; i++) {
            propagator.propagate(date.shiftedBy(i));
            Assert.assertTrue(!sensor.noInhibition(date.shiftedBy(i)));
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SENSOR_MODEL}
     * 
     * @testedMethod {@link SensorModel#noInhibition(AbsoluteDate)}
     * 
     * @description Creation of a sensor model and test if the target (Earth) is in the inhibition
     *              field of the sensor during the propagation of a low equatorial orbit.
     *              This test in a duplicate of {@link NoInhibitionTest()} but a variable radius provider is used to
     *              define
     *              the main target (Earth).
     *              In that case, the target as a null flattening so the results must be exactly the same than the one
     *              obtained with a constant radius (being a double).
     * 
     * @input An assembly containing a part with a sensor property, a low equatorial orbit
     * @output True if no inhibition occur at given date, false otherwise
     * 
     * @testPassCriteria The method should return false, since the Earth is always in the field during
     *                   the propagation so there are inhibitions
     * 
     * @referenceVersion 3.3
     * @nonRegressionVersion 3.3
     * @throws PatriusException
     */
    @Test
    public final void NoInhibitionWithVariableRadiusTest() throws PatriusException {

        // Earth radius provider : variable radius with a null flattening
        final double earthRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final EllipsoidBodyShape earthNullFlattening = new ExtendedOneAxisEllipsoid(earthRadius,
            0.0,
            FramesFactory.getITRF(), "Earth");
        final LocalRadiusProvider earthRadiusProvider = new VariableRadiusProvider(earthNullFlattening);

        // Low equatorial orbit and propagator:

        // Attitude law : constant attitude law
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);

        final AbsoluteDate date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());
        final double mu = Constants.WGS84_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit((1 + this.comparisonEpsilon) * earthRadius, .0, .0, 0, 0,
            -FastMath.PI / 2.0, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
        final double t = orbit.getKeplerianPeriod();
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, attitudeProv);

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensor
        // main field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.MINUS_K;
        final CircularField[] mainField = new CircularField[1];
        mainField[0] = new CircularField(name, FastMath.PI / 4.0, mainFieldDirection);

        // main target : center of the earth !
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);

        final PVCoordinatesProvider[] mainTarget = new PVCoordinatesProvider[1];
        mainTarget[0] = new BasicPVCoordinatesProvider(mainTargetCoordinates, FramesFactory.getEME2000());
        final LocalRadiusProvider[] inhibitionTargetsRadiuses = { earthRadiusProvider };

        // sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField[0]);
        sensorProperty.setInhibitionFieldsAndTargets(mainField, mainTarget, inhibitionTargetsRadiuses);
        sensorProperty.setMainTarget(mainTarget[0], earthRadiusProvider);

        // assembly building
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add sensor
            builder.addProperty(sensorProperty, this.mainBody);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame =
                new UpdatableFrame(FramesFactory.getEME2000(), Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // sensor model
        final SensorModel sensor = new SensorModel(assembly, this.mainBody);

        // event detector
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final SensorVisibilityDetector detector = new SensorVisibilityDetector(sensor, maxCheck, threshold);

        // add event detector
        propagator.addEventDetector(detector);

        for (int i = 0; i < t; i++) {
            propagator.propagate(date.shiftedBy(i));
            Assert.assertTrue(!sensor.noInhibition(date.shiftedBy(i)));
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SENSOR_MODEL}
     * 
     * @testedMethod {@link SensorModel#isMainTargetInField(AbsoluteDate)}
     * 
     * @description Creation of a sensor model and test if the target (Earth) is in the field of
     *              view of the sensor during the propagation of a low equatorial orbit
     * 
     * @input An assembly containing a part with a sensor property, a low equatorial orbit
     * @output True if the target is in the field at given date, false otherwise
     * 
     * @testPassCriteria The method should return true since the Earth is always in the field of
     *                   view during the propagation
     * 
     * @referenceVersion 3.2
     * @nonRegressionVersion 3.2
     * @throws PatriusException
     */
    @Test
    public final void IsInFieldOfViewTest() throws PatriusException {

        // Earth radius
        final double earthRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;

        // Low equatorial orbit and propagator:

        // Attitude law : constant attitude law
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);

        final AbsoluteDate date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());
        final double mu = Constants.WGS84_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit((1 + this.comparisonEpsilon) * earthRadius, .0, .0, 0, 0,
            -FastMath.PI / 2.0, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
        final double t = orbit.getKeplerianPeriod();
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, attitudeProv);

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensor
        // main field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.MINUS_K;
        final CircularField mainField = new CircularField(name, FastMath.PI / 4.0, mainFieldDirection);

        // main target : center of the earth !
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);
        final PVCoordinatesProvider mainTarget = new BasicPVCoordinatesProvider(mainTargetCoordinates,
            FramesFactory.getEME2000());

        // sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        sensorProperty.setMainTarget(mainTarget, new ConstantRadiusProvider(earthRadius));

        // assembly building
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add sensor
            builder.addProperty(sensorProperty, this.mainBody);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame =
                new UpdatableFrame(FramesFactory.getEME2000(), Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // sensor model
        final SensorModel sensor = new SensorModel(assembly, this.mainBody);

        // event detector
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final SensorVisibilityDetector detector = new SensorVisibilityDetector(sensor, maxCheck, threshold);

        // add event detector
        propagator.addEventDetector(detector);

        for (int i = 0; i < t; i++) {
            propagator.propagate(date.shiftedBy(i));
            Assert.assertTrue(sensor.isMainTargetInField(date.shiftedBy(i)));
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SENSOR_MODEL}
     * 
     * @testedMethod {@link SensorModel#isMainTargetInField(AbsoluteDate)}
     * 
     * @description Creation of a sensor model and test if the target (Earth) is in the field of
     *              view of the sensor during the propagation of a low equatorial orbit. This test in a duplicate
     *              of {@link IsInFieldOfViewTest()} but a variable radius provider is used to define the main target
     *              (Earth).
     *              In that case, the target as a null flattening so the results must be exactly the same than the one
     *              obtained with a constant radius (being a double).
     * 
     * @input An assembly containing a part with a sensor property, a low equatorial orbit
     * @output True if the target is in the field at given date, false otherwise
     * 
     * @testPassCriteria The method should return true since the Earth is always in the field of
     *                   view during the propagation
     * 
     * @referenceVersion 3.3
     * @nonRegressionVersion 3.3
     * @throws PatriusException
     */
    @Test
    public final void IsInFieldOfViewWithVariableRadiusTest() throws PatriusException {

        // Earth radius
        final double earthRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;

        // Low equatorial orbit and propagator:

        // Attitude law : constant attitude law
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);

        final AbsoluteDate date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());
        final double mu = Constants.WGS84_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit((1 + this.comparisonEpsilon) * earthRadius, .0, .0, 0, 0,
            -FastMath.PI / 2.0, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
        final double t = orbit.getKeplerianPeriod();
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, attitudeProv);

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensor
        // main field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.MINUS_K;
        final CircularField mainField = new CircularField(name, FastMath.PI / 4.0, mainFieldDirection);

        // main target : center of the earth !
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);
        final PVCoordinatesProvider mainTarget = new BasicPVCoordinatesProvider(mainTargetCoordinates,
            FramesFactory.getEME2000());

        // Variable radius provider with null flattening
        final EllipsoidBodyShape earthNullFlattening = new ExtendedOneAxisEllipsoid(earthRadius,
            0.0,
            FramesFactory.getITRF(), "Earth");
        final LocalRadiusProvider earthRadiusProvider = new VariableRadiusProvider(earthNullFlattening);

        // sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);
        sensorProperty.setMainTarget(mainTarget, earthRadiusProvider);

        // assembly building
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add sensor
            builder.addProperty(sensorProperty, this.mainBody);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame =
                new UpdatableFrame(FramesFactory.getEME2000(), Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly = builder.returnAssembly();

        // sensor model
        final SensorModel sensor = new SensorModel(assembly, this.mainBody);

        // event detector
        final double maxCheck = 10.;
        final double threshold = 10.e-10;
        final SensorVisibilityDetector detector = new SensorVisibilityDetector(sensor, maxCheck, threshold);

        // add event detector
        propagator.addEventDetector(detector);

        for (int i = 0; i < t; i++) {
            propagator.propagate(date.shiftedBy(i));
            Assert.assertTrue(sensor.isMainTargetInField(date.shiftedBy(i)));
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SENSOR_MODEL}
     * 
     * @testedMethod {@link SensorModel#getMainTargetAngularRadius(AbsoluteDate)}
     * 
     * @description Check the computation of the target radius as seen from a polar orbit with variable radius provider.
     *              At the equator and at pole, the expected radius can be computed mathematically. Within pole and
     *              equator, the radius
     *              must lie within radiuses at equator and pole.
     * 
     * @input An assembly containing a part with a sensor property, a low equatorial orbit
     * 
     * @output The angular radius of the target
     * 
     * @testPassCriteria The target radius is as expected (expected value: math, tolerance: 1E-16)
     * 
     * @referenceVersion 3.3
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void variableRadiusTest() throws PatriusException {
        // Equator case
        final double expectedLocalRadius1 = 1000000;
        final double expectedRadius1 = MathLib.asin(expectedLocalRadius1 / 1E15);
        final double actualRadius1 = this.checkTargetAngularRadius(Vector3D.MINUS_I.add(Vector3D.PLUS_J));
        Assert.assertEquals(0., (expectedRadius1 - actualRadius1) / expectedRadius1, 1E-15);
        // Pole case
        final double expectedLocalRadius2 = 500000;
        final double expectedRadius2 = MathLib.asin(expectedLocalRadius2 / 1E15);
        final double actualRadius2 = this.checkTargetAngularRadius(Vector3D.MINUS_I.add(Vector3D.PLUS_K));
        Assert.assertEquals(0., (expectedRadius2 - actualRadius2) / expectedRadius2, 1E-15);
        // 45deg case
        final EllipsoidBodyShape elli = new ExtendedOneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getGCRF(), "Terre");
        final double error = erreurAngleHorizonCible(elli, MathLib.toRadians(45), 0., 250.e3, 845.e3, true);
        Assert.assertEquals(0., error, 6E-6);
    }

    private static
            double
            erreurAngleHorizonCible(final EllipsoidBodyShape elli, final double lat, final double azim,
                                    final double altSat, final double altTarget, final boolean display)
                                                                                                       throws PatriusException {

        // Position du point au sol et Topoc (à longitude 0)
        final GeodeticPoint geoPoint = new GeodeticPoint(lat, 0.0, 0.0);
        final TopocentricFrame topoFrame = new TopocentricFrame(elli, geoPoint, "geop");

        // Droite de direction azim dans horizontale locale (azim Est->Nord)
        final Line groundLine = new Line(Vector3D.ZERO, new Vector3D(azim, 0));

        // Position satellite dans le TopoFrame
        final double req = elli.getEquatorialRadius();
        // Distance au satellite sur la ligne tangente au sol
        final double distSat = MathLib.sqrt(MathLib.pow(req + altSat, 2) - MathLib.pow(req, 2));
        final Vector3D topoSatPos = groundLine.pointAt(distSat);
        // Point visé par le satellite dans topoFrame (sur Z)
        final Vector3D topoTarget = new Vector3D(0, 0, altTarget);

        // Passage en repère de Base
        final Transform TopoToBase = topoFrame.getTransformTo(FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH);
        final Vector3D geop = TopoToBase.transformPosition(Vector3D.ZERO);
        final Vector3D satPos = TopoToBase.transformPosition(topoSatPos);
        final Vector3D target = TopoToBase.transformPosition(topoTarget);
        final Vector3D sat2Target = target.subtract(satPos);
        final Vector3D sat2Geop = geop.subtract(satPos);

        // Angles
        // Angle Horizon-Cible (expected value)
        final double horizElev = Vector3D.angle(sat2Geop, sat2Target);

        // Actual value
        final PVCoordinates pvcTarget = new PVCoordinates(target, Vector3D.PLUS_K.crossProduct(target));
        final KeplerianOrbit orb = new KeplerianOrbit(pvcTarget, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH,
            Constants.EGM96_EARTH_MU);
        final double localRadius = elli.getLocalRadius(satPos, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, orb);
        final double estimApparAngle = MathLib.asin(localRadius / satPos.getNorm());
        final double estimHorizElev = Vector3D.angle(satPos.negate(), sat2Target) - estimApparAngle;
        final double erreurAngleHzCible = estimHorizElev - horizElev;

        // Return relative error
        return erreurAngleHzCible / horizElev;
    }

    private double checkTargetAngularRadius(final Vector3D sightAxis) throws PatriusException {

        // Initialization

        // State (polar orbit)
        final Orbit orbit = new KeplerianOrbit(1E15, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.EGM96_EARTH_MU);
        final Attitude attitude = new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY)
            .getAttitude(orbit);
        final SpacecraftState state = new SpacecraftState(orbit, attitude);

        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart(this.mainBody);
        final SensorProperty sensorProperty = new SensorProperty(sightAxis);
        builder.addProperty(sensorProperty, this.mainBody);
        final Assembly assembly = builder.returnAssembly();

        // Variable radius provider with null flattening
        final EllipsoidBodyShape earthNullFlattening = new ExtendedOneAxisEllipsoid(1000000, 0.5,
            FramesFactory.getGCRF(), "Earth");
        final LocalRadiusProvider earthRadiusProvider = new VariableRadiusProvider(earthNullFlattening);

        // Sensor model
        final PVCoordinates mainTargetCoordinates = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);
        final PVCoordinatesProvider mainTarget = new BasicPVCoordinatesProvider(mainTargetCoordinates,
            FramesFactory.getGCRF());
        sensorProperty.setMainTarget(mainTarget, earthRadiusProvider);
        final CircularField mainField = new CircularField("circularField", FastMath.PI / 4.0, sightAxis);
        sensorProperty.setMainFieldOfView(mainField);

        final SensorModel sensorModel = new SensorModel(assembly, this.mainBody);

        // Computation
        assembly.initMainPartFrame(state);
        final double actual = sensorModel.getMainTargetAngularRadius(AbsoluteDate.J2000_EPOCH);
        return actual;
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }
}
