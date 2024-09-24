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
 * @history creation 01/12/2011
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2648:27/01/2021:[PATRIUS] Definir une direction a partir du moment cinetique d'une trajectoire orbitale
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:393:16/03/2015:Constant Attitude Laws
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the direction "momentum" (normal to trajectory plane), the associated vector being the normed
 *              cross product of the position and the velocity
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: MomentumDirectionTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 */
public class MomentumDirectionTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle momentum direction
         * 
         * @featureDescription defined by the celestial body,
         *                     the vector associated to an origin PVCoordinatesProvider being the normed cross product
         *                     of the position and the velocity
         * 
         * @coveredRequirements DV-GEOMETRIE_160, DV-GEOMETRIE_170,
         *                      DV-GEOMETRIE_180, DV-GEOMETRIE_190, DV-ATT_390
         */
        MOMENTUM_DIRECTION
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(MomentumDirectionTest.class.getSimpleName(), "Momentum direction");
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MOMENTUM_DIRECTION}
     * 
     * @testedMethod {@link MomentumDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by a celestial body
     *              only, and getting of the vector normal to the trajectory
     *              plane of a PVCoordinates provider around this body,
     *              expressed in a frame, at a date.
     * 
     * @input the origin created as basic PVCoordinatesProvider
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the returned vector is the correct one, orthogonal to both position and
     *                   velocity. If the position and velocity are parallel, an exception is thrown.
     *                   The 1.0e-14 epsilon is the simple double comparison epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetVector() {

        Report.printMethodHeader("testGetVector", "Get direction vector", "Math", this.comparisonEpsilon,
            ComparisonType.ABSOLUTE);

        try {
            // frames creation
            // creation of the earth as CelestialBody
            final CelestialBody earth = CelestialBodyFactory.getEarth();
            final Frame earthFrame = earth.getInertialFrame(IAUPoleModelType.CONSTANT);

            // another one
            final Vector3D translationVect = new Vector3D(10.0, 10.0, 10.0);
            final Transform outTransform = new Transform(AbsoluteDate.J2000_EPOCH, translationVect);
            final Frame outputFrame = new Frame(earthFrame, outTransform, "outputFrame");
            ;

            // origin creation from the earth frame
            final Vector3D originPos = new Vector3D(1.635732, -8.654534, 5.6721);
            Vector3D originVel = new Vector3D(7.6874231, 654.687534, -17.721);
            PVCoordinates originPV = new PVCoordinates(originPos, originVel);
            BasicPVCoordinatesProvider inOrigin = new BasicPVCoordinatesProvider(originPV, earthFrame);

            // direction creation
            MomentumDirection direction = new MomentumDirection(earth);

            // test
            // the date has no meaning here
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

            final Vector3D result = direction.getVector(inOrigin, date, outputFrame);

            // the transformation doesn't change this vector
            final Vector3D expected = Vector3D.crossProduct(originPos, originVel).normalize();

            Assert.assertEquals(expected.getX(), result.getX(), this.comparisonEpsilon);
            Assert.assertEquals(expected.getY(), result.getY(), this.comparisonEpsilon);
            Assert.assertEquals(expected.getZ(), result.getZ(), this.comparisonEpsilon);

            Report.printToReport("Direction", expected, result);

            // test with a velocity parallel to the position vector
            originVel = originPos;
            originPV = new PVCoordinates(originPos, originVel);
            inOrigin = new BasicPVCoordinatesProvider(originPV, earthFrame);

            // direction creation
            direction = new MomentumDirection(earth);

            try {
                direction.getVector(inOrigin, date, outputFrame);

                // an exception must be thrown !
                Assert.fail();

            } catch (final PatriusException e) {
                // expected !
            }

        } catch (final PatriusException e) {
            Assert.fail();
        }

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MOMENTUM_DIRECTION}
     * 
     * @testedMethod {@link MomentumDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by a {@link PVCoordinatesProvider} only, and getting of the
     *              vector normal to the trajectory
     *              plane
     * 
     * @testPassCriteria the returned vector is the correct one, orthogonal to both position and
     *                   velocity in input frame.
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testGetVectorPVCoordinateProvider() throws PatriusException {

        // An orbit
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(),
            AbsoluteDate.J2000_EPOCH, Constants.EGM96_EARTH_MU);

        // Direction creation
        final MomentumDirection direction = new MomentumDirection(orbit);

        // Compute direction
        final Frame outputFrame = FramesFactory.getEME2000();
        final Vector3D dir = direction.getVector(null, orbit.getDate(), outputFrame);

        // Check
        final PVCoordinates pv = orbit.getPVCoordinates(outputFrame);
        Assert.assertEquals(0., dir.distance(pv.getMomentum().normalize()), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MOMENTUM_DIRECTION}
     * 
     * @testedMethod {@link MomentumDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by a celestial body
     *              only, and getting of the line containing the given origin and the
     *              normal to the trajectory plane vector, in another frame at a date.
     * 
     * @input the origin created as basic PVCoordinatesProvider
     * 
     * @output Line
     * 
     * @testPassCriteria the returned Line contains the origin and the (normal to the
     *                   trajectory plane vector. If the position and velocity are parallel, an exception is thrown.
     *                   The 1.0e-14 epsilon is the simple double comparison epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetLine() {

        try {

            // frames creation
            // creation of the earth as CelestialBody
            final CelestialBody earth = CelestialBodyFactory.getEarth();
            final Frame earthFrame = earth.getInertialFrame(IAUPoleModelType.CONSTANT);

            // another one
            final Vector3D translationVect = new Vector3D(10.0, 10.0, 10.0);
            final Transform outTransform = new Transform(AbsoluteDate.J2000_EPOCH, translationVect);
            final Frame outputFrame = new Frame(earthFrame, outTransform, "outFram");
            ;

            // origin creation from the earth frame
            Vector3D originPos = new Vector3D(1.635732, -8.654534, 5.6721);
            final Vector3D originVel = new Vector3D(7.6874231, 654.687534, -17.721);
            PVCoordinates originPV = new PVCoordinates(originPos, originVel);
            BasicPVCoordinatesProvider inOrigin = new BasicPVCoordinatesProvider(originPV, earthFrame);

            // direction creation
            MomentumDirection direction = new MomentumDirection(earth);

            // test
            // the date has no meaning here
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

            // line creation
            final Line line = direction.getLine(inOrigin, date, outputFrame);
            // test of the points
            final PVCoordinates outExpected = outTransform.transformPVCoordinates(originPV);
            final Vector3D expectedPos = outExpected.getPosition();
            final Vector3D outOfPlanePoint = expectedPos.add(direction.getVector(inOrigin, date, outputFrame));
            Assert.assertTrue(line.contains(expectedPos));
            Assert.assertTrue(line.contains(outOfPlanePoint));

            // test with a velocity parallel to the position vector
            originPos = originVel;
            originPV = new PVCoordinates(originPos, originVel);
            inOrigin = new BasicPVCoordinatesProvider(originPV, earthFrame);

            // direction creation
            direction = new MomentumDirection(earth);

            try {
                direction.getLine(inOrigin, date, outputFrame);

                // An exception must be thrown !
                Assert.fail();

            } catch (final PatriusException e) {
                // expected
            }

        } catch (final PatriusException e) {
            Assert.fail();
        }

    }

    /**
     * @throws PatriusException
     *         orekit exception
     * @testType UT
     * 
     * @testedFeature {@link features#MOMENTUM_DIRECTION}
     * 
     * @testedMethod {@link MomentumDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * @testedMethod {@link MomentumDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description test the methods on a special case i.e. when the frame is the satellite frame and thus when the pv
     *              coordinates are zero.
     * 
     * @input a satellite frame
     * 
     * @output Vector3D and Line
     * 
     * @testPassCriteria the obtained vector should be the momentum vector with respect to eme2000 projected onto the
     *                   satellite frame and the line should be the associated one
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSpecialCase() throws PatriusException {
        // date
        final AbsoluteDate date = new AbsoluteDate(2012, 5, 17, 11, 21, 0, TimeScalesFactory.getTAI());
        // orbit
        final Orbit orbit = new KeplerianOrbit(7500000, 0.01, 0.2, 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
        final Propagator propagator = new KeplerianPropagator(orbit);
        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        // eme2000 frame
        final Frame bodyFrame = CelestialBodyFactory.getEarth().getInertialFrame(IAUPoleModelType.CONSTANT);
        // satellite frame
        final Frame satellite = new LocalOrbitalFrame(FramesFactory.getEME2000(), LOFType.TNW, propagator, "LOF");

        // momentum direction with respect to eme2000 projected on the satellite frame
        final IDirection dir = new MomentumDirection(CelestialBodyFactory.getEarth());
        final AbsoluteDate targetDate = date.shiftedBy(84600.);
        final Vector3D m1 = dir.getVector(null, targetDate, satellite);

        // previous momentum obtained with the function getPVCoordinates(Frame) on a SpacecraftState;
        final Vector3D momentum = propagator.propagate(targetDate).getPVCoordinates(bodyFrame).getMomentum();
        final Transform eme2000ToSatellite = bodyFrame.getTransformTo(satellite, targetDate);
        final Vector3D m2 = eme2000ToSatellite.transformVector(momentum).normalize();

        // the computed vectors should be equal
        Assert.assertEquals(m2.getX(), m1.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(m2.getY(), m1.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(m2.getZ(), m1.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        // same kind of test on the method getLine(...)
        final Line l1 = dir.getLine(null, targetDate, satellite);
        final Line l2 = new Line(m2, Vector3D.ZERO);

        Assert.assertFalse(l1.isSimilarTo(l2));
    }

    /**
     * Set up
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
