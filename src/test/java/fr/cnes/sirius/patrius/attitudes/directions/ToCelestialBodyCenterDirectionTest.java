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
 * @history creation 06/12/2011
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
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
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the directions described by a celestial body : the celestial body's center is the target
 *              point.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ToCelestialBodyCenterDirectionTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 */
public class ToCelestialBodyCenterDirectionTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Direction "to the center of a celestial body"
         * 
         * @featureDescription Direction from a given origin point described by a PVCoordinatesProvider
         *                     to the center of a celestial body (CelestialBody)
         * 
         * @coveredRequirements DV-GEOMETRIE_160, DV-GEOMETRIE_170, DV-GEOMETRIE_190, DV-ATT_380
         */
        TO_CELECTIAL_BODY_CENTER_DIRECTION
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(ToCelestialBodyCenterDirectionTest.class.getSimpleName(),
            "To celestial body center direction");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#TO_CELECTIAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link ToCelestialBodyCenterDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by a celetial body
     *              (the celestial body's center is the target point),
     *              and getting of the vector associated to a given origin expressed in a frame, at a date.
     * 
     * @input the origin created as basic PVCoordinatesProvider, the celestial body
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the returned vector is the correct one from the origin to the celestial body's center,
     *                   when expressed in the wanted frame. The 1.0e-14 epsilon is the simple double comparison
     *                   epsilon, used because
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
            final Frame earthFrame = earth.getInertiallyOrientedFrame();

            // another frame...
            final Vector3D translationVect = new Vector3D(10.0, 10.0, 10.0);
            final Transform outTransform = new Transform(AbsoluteDate.J2000_EPOCH, translationVect);
            final Frame outputFrame = new Frame(earthFrame, outTransform, "outFram");
            ;

            // origin creation from the earth frame
            final Vector3D originPos = new Vector3D(1.635732, -8.654534, 5.6721);
            final Vector3D originVel = new Vector3D(7.6874231, 654.687534, -17.721);
            final PVCoordinates originPV = new PVCoordinates(originPos, originVel);
            final BasicPVCoordinatesProvider inOrigin = new BasicPVCoordinatesProvider(originPV, earthFrame);

            // direction creation
            final ToCelestialBodyCenterDirection direction = new ToCelestialBodyCenterDirection(earth);

            // test
            // the date has no meaning here
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

            final Vector3D result = direction.getVector(inOrigin, date, outputFrame);

            // expected position
            final Vector3D expectedPos = outTransform.transformVector(originPos).scalarMultiply(-1);

            Assert.assertEquals(expectedPos.getX(), result.getX(), this.comparisonEpsilon);
            Assert.assertEquals(expectedPos.getY(), result.getY(), this.comparisonEpsilon);
            Assert.assertEquals(expectedPos.getZ(), result.getZ(), this.comparisonEpsilon);

            Report.printToReport("Direction", expectedPos, result);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#TO_CELECTIAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link ToCelestialBodyCenterDirection#getTargetPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by
     *              a celetial body (the celestial body's center is the target point),
     *              and getting of the target (center of the body) PV coordinates
     *              expressed in a frame, at a date.
     * 
     * @input the origin created as basic PVCoordinatesProvider, the celestial body
     * 
     * @output PVCoordinates
     * 
     * @testPassCriteria the returned target coordinates are equal to zero when transformed in the
     *                   celestial body's local frame. The 1.0e-14 epsilon is the simple double comparison epsilon, used
     *                   because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetTarget() {

        try {
            // frames creation
            // creation of the earth as CelestialBody
            final CelestialBody earth = CelestialBodyFactory.getEarth();
            final Frame earthFrame = earth.getInertiallyOrientedFrame();

            // another one
            final Vector3D translationVect = new Vector3D(10.0, 10.0, 10.0);
            final Transform outTransform = new Transform(AbsoluteDate.J2000_EPOCH, translationVect);
            final Frame outputFrame = new Frame(earthFrame, outTransform, "outFrame");

            // direction creation
            final ToCelestialBodyCenterDirection direction = new ToCelestialBodyCenterDirection(earth);

            // test
            // the date has no meaning here
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

            final PVCoordinates outOrigin = direction.getTargetPVCoordinates(date, outputFrame);
            final Vector3D resultPos = outOrigin.getPosition();
            final Vector3D resultVel = outOrigin.getVelocity();

            // expected coordinates
            final Vector3D expectedPos = outTransform.transformPosition(Vector3D.ZERO);
            final Vector3D expectedVel = outTransform.transformVector(Vector3D.ZERO);

            Assert.assertEquals(expectedPos.getX(), resultPos.getX(), this.comparisonEpsilon);
            Assert.assertEquals(expectedPos.getY(), resultPos.getY(), this.comparisonEpsilon);
            Assert.assertEquals(expectedPos.getZ(), resultPos.getZ(), this.comparisonEpsilon);

            Assert.assertEquals(expectedVel.getX(), resultVel.getX(), this.comparisonEpsilon);
            Assert.assertEquals(expectedVel.getY(), resultVel.getY(), this.comparisonEpsilon);
            Assert.assertEquals(expectedVel.getZ(), resultVel.getZ(), this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#TO_CELECTIAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link ToCelestialBodyCenterDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described
     *              a celetial body (the celestial body's center is the target point),
     *              and getting of the line containing a given origin and the associateds vector.
     * 
     * @input the origin created as basic PVCoordinatesProvider, the celestial body
     * 
     * @output Line
     * 
     * @testPassCriteria the returned Line contains the origin and the (0,0,0) point of the
     *                   celestial body's local frame.
     *                   An exception is returned if those points are identical.
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
            final Frame earthFrame = earth.getInertiallyOrientedFrame();

            // another one
            final Vector3D translationVect = new Vector3D(10.0, 10.0, 10.0);
            final Transform outTransform = new Transform(AbsoluteDate.J2000_EPOCH, translationVect);
            final Frame outputFrame = new Frame(earthFrame, outTransform, "outputFrame");
            ;

            // origin creation from the earth frame
            Vector3D originPos = new Vector3D(1.0, 0.0, 0.0);
            final Vector3D originVel = new Vector3D(7.6874231, 654.687534, -17.721);
            PVCoordinates originPV = new PVCoordinates(originPos, originVel);
            BasicPVCoordinatesProvider inOrigin = new BasicPVCoordinatesProvider(originPV, earthFrame);

            // direction creation
            ToCelestialBodyCenterDirection direction = new ToCelestialBodyCenterDirection(earth);

            // test
            // the date has no meaning here
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

            // line creation
            final Line line = direction.getLine(inOrigin, date, outputFrame);
            // expected points
            final Vector3D expectedCenter = outTransform.transformPosition(originPos);
            final Vector3D expectedOrigin = outTransform.transformPosition(Vector3D.ZERO);
            // test of the points
            Assert.assertTrue(line.contains(expectedCenter));
            Assert.assertTrue(line.contains(expectedOrigin));

            // test with the origin equal to (0,0,0)
            originPos = Vector3D.ZERO;
            originPV = new PVCoordinates(originPos, originVel);
            inOrigin = new BasicPVCoordinatesProvider(originPV, earthFrame);

            // direction creation
            direction = new ToCelestialBodyCenterDirection(earth);

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
     * Set up
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
    }
}