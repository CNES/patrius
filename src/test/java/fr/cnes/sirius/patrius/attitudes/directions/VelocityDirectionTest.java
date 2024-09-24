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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
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

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
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
 *              Tests for the velocity direction.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: VelocityDirectionTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 */
public class VelocityDirectionTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Velocity direction
         * 
         * @featureDescription described by the expression of a given origin's velocity in a reference frame, projected
         *                     in another one.
         * 
         * @coveredRequirements DV-GEOMETRIE_160, DV-GEOMETRIE_170, DV-GEOMETRIE_180, DV-GEOMETRIE_190, DV-ATT_390
         */
        VELOCITY_DIRECTION
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(VelocityDirectionTest.class.getSimpleName(), "Velocity direction");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VELOCITY_DIRECTION}
     * 
     * @testedMethod {@link VelocityDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a velocity direction, and expression of the vector associated to an origin point in
     *              a frame, at a date.
     * 
     * @input the origin created as basic PVCoordinatesProvider
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the returned vector is the correct velocity vector of the origin. The 1.0e-14 epsilon is the
     *                   simple double comparison epsilon, used because the computations involve here no mechanics
     *                   algorithms.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetVector() {

        Report.printMethodHeader("testGetVector", "Get direction vector", "Math", this.comparisonEpsilon,
            ComparisonType.ABSOLUTE);

        // origin creation
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D originPos = new Vector3D(1.635732, -8.654534, 5.6721);
        final Vector3D originVel = new Vector3D(7.6874231, 654.687534, -17.721);
        final PVCoordinates originPV = new PVCoordinates(originPos, originVel);
        final BasicPVCoordinatesProvider inOrigin = new BasicPVCoordinatesProvider(originPV, frame);

        // direction creation
        final VelocityDirection direction = new VelocityDirection(frame);

        // test
        // the frame and date have no meaning here
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        try {
            final Vector3D result = direction.getVector(inOrigin, date, frame);

            Assert.assertEquals(originVel.getX(), result.getX(), this.comparisonEpsilon);
            Assert.assertEquals(originVel.getY(), result.getY(), this.comparisonEpsilon);
            Assert.assertEquals(originVel.getZ(), result.getZ(), this.comparisonEpsilon);

            Report.printToReport("Direction", originVel, result);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VELOCITY_DIRECTION}
     * 
     * @testedMethod {@link VelocityDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a velocity direction, and getting of the line containing a given origin point, and
     *              the associated vector.
     * 
     * @input the origin created as basic PVCoordinatesProvider
     * 
     * @output Line
     * 
     * @testPassCriteria the returned Line contains the origin and the (0,0,0) point. An exception is returned if those
     *                   points are identical. The 1.0e-14 epsilon is the simple double comparison epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetLine() {

        // origin creation
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D originPos = new Vector3D(1.635732, -8.654534, 5.6721);
        Vector3D originVel = new Vector3D(7.6874231, 654.687534, -17.721);
        PVCoordinates originPV = new PVCoordinates(originPos, originVel);
        BasicPVCoordinatesProvider inOrigin = new BasicPVCoordinatesProvider(originPV, frame);

        // direction creation
        VelocityDirection direction = new VelocityDirection(frame);

        // test
        // the frame and date have no meaning here
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        try {
            // line creation
            final Line line = direction.getLine(inOrigin, date, frame);
            // test of the points
            Assert.assertTrue(line.contains(originPos));
            Assert.assertTrue(line.contains(originPos.add(originVel)));

        } catch (final PatriusException e) {
            Assert.fail();
        }

        // test with the velocity equal to (0,0,0)
        originVel = Vector3D.ZERO;
        originPV = new PVCoordinates(originPos, originVel);
        inOrigin = new BasicPVCoordinatesProvider(originPV, frame);

        // direction creation
        direction = new VelocityDirection(frame);

        try {
            direction.getLine(inOrigin, date, frame);

            // An exception must be thrown !
            Assert.fail();

        } catch (final PatriusException e) {
            // expected
        }
    }

    /**
     * @throws PatriusException
     *         orekit exception
     * @testType UT
     * 
     * @testedFeature {@link features#VELOCITY_DIRECTION}
     * 
     * @testedMethod {@link VelocityDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * @testedMethod {@link VelocityDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description test the methods on a special case i.e. when the frame is the satellite frame and thus when the pv
     *              coordinates are zero.
     * 
     * @input a satellite frame
     * 
     * @output Vector3D and Line
     * 
     * @testPassCriteria the obtained vector should be the velocity vector with respect to eme2000 projected on the
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
        final Frame eme2000 = FramesFactory.getEME2000();
        // satellite frame
        final Frame satellite = new LocalOrbitalFrame(FramesFactory.getEME2000(), LOFType.TNW, propagator, "LOF");

        // velocity direction with respect to eme2000 projected on the satellite frame
        final IDirection dir = new VelocityDirection(eme2000);
        final AbsoluteDate targetDate = date.shiftedBy(84600.);
        final Vector3D v1 = dir.getVector(null, targetDate, satellite);

        // previous velocity obtained with the function getPVCoordinates(Frame) on a SpacecraftState;
        final Vector3D velocity = propagator.propagate(targetDate).getPVCoordinates(eme2000).getVelocity();
        final Transform eme2000ToSatellite = eme2000.getTransformTo(satellite, targetDate);
        final Vector3D v2 = eme2000ToSatellite.transformVector(velocity);

        // the computed vectors should be equal
        Assert.assertEquals(v2.getX(), v1.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(v2.getY(), v1.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(v2.getZ(), v1.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        // same kind of test on the method getLine(...)
        final Line l1 = dir.getLine(null, targetDate, satellite);
        final Line l2 = new Line(v2, Vector3D.ZERO);

        Assert.assertFalse(l1.isSimilarTo(l2));
    }
}
