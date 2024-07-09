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
 * @history creation 15/10/2015
 *
 * HISTORY
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:457:18/11/2015:added Glint pointing direction
 * VERSION::FA:559:26/02/2016:minor corrections
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:650:22/07/2016: ellipsoid corrections
 * VERSION::DM:1415:23/03/2018: add getter Glint position
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.solver.BisectionSolver;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class test for GlintApproximatePointingDirection.
 * 
 * @author rodriguest
 * @version $Id: GlintApproximatePointingDirectionTest.java 17910 2017-09-11 11:58:16Z bignon $
 * @since 3.1
 */

public class GlintApproximatePointingDirectionTest {

    private EllipsoidBodyShape earthShape;
    private CelestialBody sun;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle GLINT
         * 
         * @featureDescription All methods and constructors of the glint pointing direction
         * 
         */
        GLINT,
        /**
         * @featureTitle SINGULARITIES
         * 
         * @featureDescription Test the exception during eclipse and singularity when earth center, satellite, sun are
         *                     aligned
         * 
         */
        SINGULARITIES

    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report
            .printClassHeader(GlintApproximatePointingDirectionTest.class.getSimpleName(), "Glint pointing direction");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#GLINT}
     * 
     * @testedMethod {@link GlintApproximatePointingDirection#getTargetPVCoordinates(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Validation of the coordinates for computed Glint point.
     *              We build a fictive fixed sun in earth frame at (0, 1UA, 0) and a fictive fixed vehicle in earth
     *              frame at (8000 km, 0, 0)
     * 
     * @input BasicPVCoordinatesProvider sun fixed in earth frame at (0, 1UA, 0)
     * @input BasicPVCoordinatesProvider vehicle fixed in earth frame at (8000, 0, 0)
     * @input EllipsoidBodyShape earth with flatten = 0.001
     * 
     * @output The PV coordinates of Glint point
     * 
     * @testPassCriteria The angular separation (n,GSun) and (n,Gsat) must be the same, n being the normal at point G
     *                   which is colinear to the radius,
     *                   The altitude of Glint is 0m and Glint is in the plane (Satellite, Earth center, Sun)
     * 
     * @referenceVersion 3.1
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void getTargetPVCoordinatesITRFTest() throws PatriusException {

        // Initialization
        final EllipsoidBodyShape earth = new ExtendedOneAxisEllipsoid(
            Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            0.001, FramesFactory.getITRF(), "Earth");

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D satPos = new Vector3D(8000E3, 0, 0);
        final PVCoordinates satPV = new PVCoordinates(satPos, Vector3D.ZERO);
        final Vector3D sunPos = new Vector3D(0, Constants.JPL_SSD_ASTRONOMICAL_UNIT, 0.);
        final PVCoordinates sunPV = new PVCoordinates(sunPos, Vector3D.ZERO);
        final BasicPVCoordinatesProvider satPVProvider = new BasicPVCoordinatesProvider(satPV, earth.getBodyFrame());
        final BasicPVCoordinatesProvider sunPVProvider = new BasicPVCoordinatesProvider(sunPV, earth.getBodyFrame());

        // Glint Point coordinates
        final BisectionSolver solver = new BisectionSolver(1.0E-15, 1.0E-15);
        final GlintApproximatePointingDirection glint = new GlintApproximatePointingDirection(earth, sunPVProvider,
            solver);
        final Vector3D glintPos = this.getTargetPVCoordinates(satPVProvider, date, earth.getBodyFrame(), earth, glint,
            sunPVProvider);
        final Vector3D glintPosDirectly = glint.getGlintVectorPosition(satPVProvider, date, earth.getBodyFrame());

        // ============================== CHECK ==============================
        // //

        // Check Glint position is on Earth surface
        Assert.assertEquals(0., earth.transform(glintPos, FramesFactory.getITRF(), date).getAltitude()
            / satPV.getPosition().getNorm(), 2E-16);
        // Check Glint position is on Earth surface
        Assert.assertEquals(0., earth.transform(glintPosDirectly, FramesFactory.getITRF(), date).getAltitude()
            / satPV.getPosition().getNorm(), 2E-16);
        // Check angle (must be equal)
        final Vector3D gSat = satPos.subtract(glintPos);
        final Vector3D gSun = sunPV.getPosition().subtract(glintPos);
        Assert.assertEquals(Vector3D.angle(glintPos, gSat), Vector3D.angle(glintPos, gSun), 1E-10);

        // Check Glint position is aligned with satellite, Earth center and Sun (coplanarity test)
        final double[][] A = { { satPos.getX(), sunPos.getX() }, { satPos.getY(), sunPos.getY() }, };
        final double[] B = { glintPos.getX(), glintPos.getY() };
        final double[] X = new QRDecomposition(new BlockRealMatrix(A)).getSolver().solve(new ArrayRealVector(B))
            .toArray();
        Assert.assertEquals(X[0] * satPos.getZ() + X[1] * sunPos.getZ(), glintPos.getZ(), 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#GLINT}
     * 
     * @testedMethod {@link GlintApproximatePointingDirection#getTargetPVCoordinates(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Validation of the coordinates for computed Glint point.
     * 
     * @input JPL Sun ephemeris
     * @input Spacecraft in EME2000 frame
     * @input EllipsoidBodyShape real earth
     * 
     * @output The PV coordinates of Glint point
     * 
     * @testPassCriteria The angular separation (n,GSun) and (n,Gsat) must be the same, n being the normal at point G
     *                   which is colinear to the radius,
     *                   The altitude of Glint is 0m and Glint is in the plane (Satellite, Earth center, Sun)
     * 
     * @referenceVersion 3.1
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void getTargetPVCoordinatesEME2000Test() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.MODIFIED_JULIAN_EPOCH
            .shiftedBy(57467 * Constants.JULIAN_DAY + 48941.15241788);
        final Vector3D spacecraftPos = new Vector3D(-2912.4990063408, -105.77182409557, -6457.8615136281)
            .scalarMultiply(1e3);
        final PVCoordinates spacecraftPVCoord = new PVCoordinates(spacecraftPos, Vector3D.ZERO);
        final PVCoordinates sunPVCoord = this.sun.getPVCoordinates(date, FramesFactory.getEME2000());
        final Vector3D sunPos = sunPVCoord.getPosition();
        final BasicPVCoordinatesProvider spacecraftPVProvider = new BasicPVCoordinatesProvider(spacecraftPVCoord,
            FramesFactory.getEME2000());

        // Glint pointing direction
        final BisectionSolver solver = new BisectionSolver(1.0E-15, 1.0E-15);
        final GlintApproximatePointingDirection glint =
            new GlintApproximatePointingDirection(this.earthShape, this.sun, solver);

        // Glint Point coordinates in J2000
        final Vector3D glintPos = this.getTargetPVCoordinates(spacecraftPVProvider, date, FramesFactory.getEME2000(),
            this.earthShape, glint, this.sun);

        // ============================== CHECK ============================== //

        // Check Glint position is on Earth surface
        final GeodeticPoint geodeticGlint = this.earthShape.transform(glintPos, FramesFactory.getEME2000(), date);
        final double glintAltitude = geodeticGlint.getAltitude();
        Assert.assertEquals(glintAltitude / glintPos.getNorm(), 0., 1E-14);

        // Check angle (must be equal)
        final Vector3D zenith = FramesFactory.getITRF().getTransformTo(FramesFactory.getEME2000(), date)
            .transformVector(geodeticGlint.getZenith());
        final Vector3D gSat = spacecraftPos.subtract(glintPos);
        final Vector3D gSun = sunPVCoord.getPosition().subtract(glintPos);
        final double angleSat = Vector3D.angle(zenith, gSat);
        final double angleSun = Vector3D.angle(zenith, gSun);
        Assert.assertEquals((angleSat - angleSun) / angleSat, 0., 1E-14);

        // Check Glint position is aligned with satellite, Earth center and Sun
        final double[][] A = { { spacecraftPos.getX(), sunPos.getX() }, { spacecraftPos.getY(), sunPos.getY() } };
        final double[] B = { glintPos.getX(), glintPos.getY() };
        final double[] X = new QRDecomposition(new BlockRealMatrix(A)).getSolver().solve(new ArrayRealVector(B))
            .toArray();

        final double act = X[0] * spacecraftPos.getZ() + X[1] * sunPos.getZ();
        final double exp = glintPos.getZ();
        Assert.assertEquals((exp - act) / exp, 0., 1E-14);

    }

    /**
     * @throws PatriusException
     *         should not happen
     * @testType UT
     * 
     * @testedFeature {@link features#GLINT}
     * 
     * @testedMethod {@link GlintApproximatePointingDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Check that the line contains glint point and spacecraft position
     * 
     * @input Spacecraft in EME2000 frame
     * @input EllipsoidBodyShape real earth
     * 
     * @output line containing spacecraft and glint point
     * 
     * @testPassCriteria the line contains glint point and spacecraft position
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void getLineTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.MODIFIED_JULIAN_EPOCH
            .shiftedBy(57467 * Constants.JULIAN_DAY + 48941.15241788);
        final Vector3D spacecraftPos = new Vector3D(-2912.4990063408, -105.77182409557, -6457.8615136281)
            .scalarMultiply(1e3);
        final PVCoordinates spacecraftPVCoord = new PVCoordinates(spacecraftPos, Vector3D.ZERO);
        final BasicPVCoordinatesProvider spacecraftPVProvider = new BasicPVCoordinatesProvider(spacecraftPVCoord,
            FramesFactory.getEME2000());

        // Glint pointing direction
        final BisectionSolver solver = new BisectionSolver(1.0E-15, 1.0E-15);
        final GlintApproximatePointingDirection glint =
            new GlintApproximatePointingDirection(this.earthShape, this.sun, solver);

        // Glint Point coordinates in body frame
        final Vector3D glintPos =
            this.getTargetPVCoordinates(spacecraftPVProvider, date, this.earthShape.getBodyFrame(),
                this.earthShape, glint, this.sun);
        // Glint Line in body frame
        final Line glintLine = glint.getLine(spacecraftPVProvider, date, this.earthShape.getBodyFrame());
        // Glint Line in EME2000
        final Line glintLine2000 = glint.getLine(spacecraftPVProvider, date, FramesFactory.getEME2000());

        // ============================== CHECK ============================== //

        // Check Lines contains Glint position
        Assert.assertEquals(0., glintLine.distance(glintPos) / glintPos.getNorm(), 1E-14);
        // Check Lines contains spacecraft position
        Assert.assertEquals(0., glintLine2000.distance(spacecraftPos) / spacecraftPos.getNorm(), 1E-14);

    }

    /**
     * @throws PatriusException
     *         should not happen
     * @testType UT
     * 
     * @testedFeature {@link features#GLINT}
     * 
     * @testedMethod {@link GlintApproximatePointingDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Check that the vector is glint minus sat position
     * 
     * @input Spacecraft in EME2000 frame
     * @input EllipsoidBodyShape real earth
     * 
     * @output vector spacecraft position to glint point
     * 
     * @testPassCriteria the vector is glint minus sat position
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void getVectorTest() throws PatriusException {

        Report.printMethodHeader("getVectorTest", "Get direction vector", "Math", 1E-14, ComparisonType.ABSOLUTE);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.MODIFIED_JULIAN_EPOCH
            .shiftedBy(57467 * Constants.JULIAN_DAY + 48941.15241788);
        final Vector3D spacecraftPos = new Vector3D(-2912.4990063408, -105.77182409557, -6457.8615136281)
            .scalarMultiply(1e3);
        final PVCoordinates spacecraftPVCoord = new PVCoordinates(spacecraftPos, Vector3D.ZERO);
        final BasicPVCoordinatesProvider spacecraftPVProvider = new BasicPVCoordinatesProvider(spacecraftPVCoord,
            FramesFactory.getEME2000());

        // Glint pointing direction
        final BisectionSolver solver = new BisectionSolver(1.0E-15, 1.0E-15);
        final GlintApproximatePointingDirection glint =
            new GlintApproximatePointingDirection(this.earthShape, this.sun, solver);

        // Glint Point coordinates in body frame
        final Vector3D glintPos =
            this.getTargetPVCoordinates(spacecraftPVProvider, date, this.earthShape.getBodyFrame(),
                this.earthShape, glint, this.sun);
        final Vector3D glintPointPosNew =
            glint.getGlintVectorPosition(spacecraftPVProvider, date, this.earthShape.getBodyFrame());

        Assert.assertEquals(0, glintPos.distance(glintPointPosNew), 0);

        // Glint Vector in body frame (null by default return in body frame)
        final Vector3D glintVector = glint.getVector(spacecraftPVProvider, date, this.earthShape.getBodyFrame());

        // Glint Vector in EME2000
        final Vector3D glintVector2000 = glint.getVector(spacecraftPVProvider, date, FramesFactory.getEME2000());

        // transform from body frame to eme2000
        final Transform bodyFrametoEme2000 =
            this.earthShape.getBodyFrame().getTransformTo(FramesFactory.getEME2000(), date);

        // ============================== CHECK ============================== //

        // Check vector in EME2000
        Vector3D expected = ((bodyFrametoEme2000.transformPosition(glintPos)).subtract(spacecraftPos)).normalize();
        Assert.assertEquals(0., expected.distance(glintVector2000), 1E-14);

        Report.printToReport("Direction", expected, glintVector2000);

        // Check vector in body frame
        expected = (glintPos.subtract(bodyFrametoEme2000.getInverse().transformPosition(spacecraftPos))).normalize();
        Assert.assertEquals(0., expected.distance(glintVector), 1E-14);

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#GLINT}
     * 
     * @testedMethod {@link GlintApproximatePointingDirection#GlintApproximatePointingDirection(EllipsoidBodyShape, PVCoordinatesProvider)}
     * 
     * @description Validation of the constructor with default solver. Compare the result with a solver having the same
     *              precision than the default one
     * 
     * @input solver with 1E-6
     * @input JPL Sun ephemeris
     * @input Spacecraft in EME2000 frame
     * @input EllipsoidBodyShape real earth
     * 
     * @output The PV coordinates of Glint point
     * 
     * @testPassCriteria Result target point with each constructor have a distance lower than 1E-14.
     * 
     * @referenceVersion 3.1
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void glintApproximatePointingDirectionTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.MODIFIED_JULIAN_EPOCH
            .shiftedBy(57467 * Constants.JULIAN_DAY + 48941.15241788);
        final Vector3D spacecraftPos = new Vector3D(-2912.4990063408, -105.77182409557, -6457.8615136281)
            .scalarMultiply(1e3);
        final PVCoordinates spacecraftPVCoord = new PVCoordinates(spacecraftPos, Vector3D.ZERO);
        final BasicPVCoordinatesProvider spacecraftPVProvider = new BasicPVCoordinatesProvider(spacecraftPVCoord,
            FramesFactory.getEME2000());

        // Glint pointing direction
        final BisectionSolver solver = new BisectionSolver(1.0E-6, 1.0E-6);
        final GlintApproximatePointingDirection glint =
            new GlintApproximatePointingDirection(this.earthShape, this.sun, solver);

        // Glint Point coordinates in J2000
        final Vector3D glintPos = this.getTargetPVCoordinates(spacecraftPVProvider, date, FramesFactory.getEME2000(),
            this.earthShape, glint, this.sun);

        // Glint pointing direction default solver
        final GlintApproximatePointingDirection defaultGlint =
            new GlintApproximatePointingDirection(this.earthShape, this.sun);
        // Glint Point coordinates in J2000
        final Vector3D defaultGlintPos =
            this.getTargetPVCoordinates(spacecraftPVProvider, date, FramesFactory.getEME2000(),
                this.earthShape, defaultGlint, this.sun);

        // ============================== CHECK ============================== //
        // check glint position computed with each constructor have a distance lower than 1E-14
        Assert.assertEquals(0., defaultGlintPos.distance(glintPos) / defaultGlintPos.getNorm(), 1E-14);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#SINGULARITIES}
     * 
     * @testedMethod {@link GlintApproximatePointingDirection#getTargetPVCoordinates(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Get the glint point coordinates when earth center, satellite, and sun are aligned
     * 
     * @input Spacecraft positon
     * 
     * 
     * @output Orekit exception
     * 
     * @testPassCriteria Expected orekit exception
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = PatriusException.class)
    public final void testException() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.MODIFIED_JULIAN_EPOCH
            .shiftedBy(59294 * Constants.JULIAN_DAY + 51630.755992315);
        final Vector3D spacecraftPos = new Vector3D(-3248.8992404739, -336.30694440079, -6229.8518028014)
            .scalarMultiply(1e3);
        final PVCoordinates spacecraftPVCoord = new PVCoordinates(spacecraftPos, Vector3D.ZERO);
        final BasicPVCoordinatesProvider spacecraftPVProvider = new BasicPVCoordinatesProvider(spacecraftPVCoord,
            FramesFactory.getEME2000());

        // Glint pointing direction default solver
        final GlintApproximatePointingDirection glint =
            new GlintApproximatePointingDirection(this.earthShape, this.sun);
        // Glint Point coordinates in J2000
        glint.getVector(spacecraftPVProvider, date, FramesFactory.getEME2000());
    }

    /**
     * @throws OrekitException
     * @testType UT
     * 
     * @testedFeature {@link features#SINGULARITIES}
     * 
     * @testedMethod {@link GlintApproximatePointingDirection#getTargetPVCoordinates(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Get the glint point coordinates during an eclipse.
     * 
     * @input BasicPVCoordinatesProvider sun fixed in earth frame at (0, 1UA, 0)
     * @input BasicPVCoordinatesProvider vehicle fixed in earth frame at (8000, 0, 0)
     * @input EllipsoidBodyShape earth with flatten = 0.001
     * 
     * @output Glint target point
     * 
     * @testPassCriteria Glint is also aligned with sun, spacecraft and earth center
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    // @Test
    // public final void testAlign() throws OrekitException {
    // // Initialization
    // final EllipsoidBodyShape earth = new EllipsoidBodyShape(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
    // 0.001, FramesFactory.getITRF(), "Earth");
    //
    // final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
    // final Vector3D satPos = new Vector3D(0, 8000E3, 0);
    // final PVCoordinates satPV = new PVCoordinates(satPos, Vector3D.ZERO);
    // final Vector3D sunPos = new Vector3D(0., Constants.JPL_SSD_ASTRONOMICAL_UNIT, 0.);
    // final PVCoordinates sunPV = new PVCoordinates(sunPos, Vector3D.ZERO);
    // final BasicPVCoordinatesProvider satPVProvider = new BasicPVCoordinatesProvider(satPV, earth.getBodyFrame());
    // final BasicPVCoordinatesProvider sunPVProvider = new BasicPVCoordinatesProvider(sunPV, earth.getBodyFrame());

    // Glint Point coordinates
    // final BisectionSolver solver = new BisectionSolver(1.0E-15, 1.0E-15);
    // final GlintApproximatePointingDirection glint = new GlintApproximatePointingDirection(earth, sunPVProvider,
    // solver);
    // final PVCoordinates glintCoord = glint.getTargetPVCoordinates(satPVProvider, date, earth.getBodyFrame());
    //
    // // ============================== CHECK ============================== //
    // Assert.assertTrue(glintCoord.getPosition().crossProduct(satPos).equals(Vector3D.ZERO));

    // final EclipseDetector eclipse = new EclipseDetector(sunPVProvider, Constants.SUN_RADIUS, earthShape, 0,
    // AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD);
    // EquinoctialOrbit orbit = new EquinoctialOrbit(8000E3, 0, 0, 0, 0, 0, PositionAngle.TRUE, earth.getBodyFrame(),
    // date, Constants.WGS84_EARTH_MU);
    // PVCoordinates pvCoord = orbit.getPVCoordinates();
    // PVCoordinates pvCoordModif = new PVCoordinates(satPos, pvCoord.getVelocity());
    // SpacecraftState s = new SpacecraftState(new CartesianOrbit(pvCoordModif, earth.getBodyFrame(), date,
    // Constants.WGS84_EARTH_MU));
    // eclipse.g(s);
    // }

    /**
     * Method to compute Glint point coordinates.
     * 
     * @param pvCoord
     *        the spacecraft PV coordinates provider
     * @param date
     *        the date to get the glint coordinates
     * @param frame
     *        the frame to express the glint coordinates by default (if null) result is given in body frame
     * @param bodyShape
     *        body shape
     * @param direction
     *        Glint direction
     * @param sunPV
     *        Sun PV provider
     * @return the PV coordinates of the Glint at the input date and frame
     * @throws PatriusException
     */
    private Vector3D getTargetPVCoordinates(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                                            final Frame frame, final BodyShape bodyShape,
                                            final GlintApproximatePointingDirection direction,
                                            final PVCoordinatesProvider sunPV) throws PatriusException {

        // Ideal method to get Glint position but unecessary conversion lead to degraded results (1E-10 instead 1E-14 in
        // some case)
        // A more direct method is used

        // final Vector3D close = pvCoord.getPVCoordinates(date, bodyShape.getBodyFrame()).getPosition();
        // final Line line = direction.getLine(pvCoord, date, bodyShape.getBodyFrame());
        // final GeodeticPoint geodeticPoint = bodyShape.getIntersectionPoint(line, close, bodyShape.getBodyFrame(),
        // date);
        // Vector3D glint = bodyShape.transform(geodeticPoint);
        final Vector3D pos = pvCoord.getPVCoordinates(date, bodyShape.getBodyFrame()).getPosition();
        final Vector3D sunPos = sunPV.getPVCoordinates(date, bodyShape.getBodyFrame()).getPosition();
        Vector3D glint = direction.getGlintPosition(pos, sunPos, date);

        // Convert glint point coordinates in input frame
        if (frame != null && bodyShape.getBodyFrame() != frame) {
            final Transform bodyFrametoOutputFrame = bodyShape.getBodyFrame().getTransformTo(frame, date);
            glint = bodyFrametoOutputFrame.transformPosition(glint);
        }

        return glint;
    }

    /**
     * Set up
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final JPLEphemeridesLoader loaderSun = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SUN);
        this.sun = loaderSun.loadCelestialBody(CelestialBodyFactory.SUN);

        this.earthShape = new ExtendedOneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING,
            FramesFactory.getITRF(), "Earth");

    }

}
