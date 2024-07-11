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
 * @history creation 02/12/2011
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
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
import fr.cnes.sirius.patrius.attitudes.TabulatedAttitudeTest;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the directions describing the poles' axis of a celestial body.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: CelestialBodyPolesAxisDirectionTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 */
public class CelestialBodyPolesAxisDirectionTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Position direction
         * 
         * @featureDescription described describing the poles' axis of a celestial body.
         * 
         * @coveredRequirements DV-GEOMETRIE_160, DV-GEOMETRIE_170,
         *                      DV-GEOMETRIE_190, DV-ATT_380
         */
        CELESTIAL_BODY_POLES_AXIS_DIRECTION
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(TabulatedAttitudeTest.class.getSimpleName(), "Celestial body poles direction");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CELESTIAL_BODY_POLES_AXIS_DIRECTION}
     * 
     * @testedMethod {@link CelestialBodyPolesAxisDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction describing the poles' axis of a celestial body,
     *              and getting of the vector position expressed
     *              in another frame, at a date.
     * 
     * @input the celestial body and the output frame
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the returned vector is the correct one directed by the
     *                   Z axis of the celestial body. The 1.0e-14 epsilon is the simple double comparison epsilon, used
     *                   because
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
            final Frame earthFrame = earth.getTrueRotatingFrame();

            // another frame...
            final Vector3D translationVect = new Vector3D(10.0, 10.0, 10.0);
            final Transform outTransform = new Transform(AbsoluteDate.J2000_EPOCH, translationVect);
            final Frame outputFrame = new Frame(earthFrame, outTransform, "outFram");
            ;

            // direction creation
            final CelestialBodyPolesAxisDirection direction = new CelestialBodyPolesAxisDirection(earth);

            // expected vector
            final Vector3D expected = outTransform.transformVector(Vector3D.PLUS_K);

            // test
            // the date has no meaning here
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

            final Vector3D result = direction.getVector(null, date, outputFrame);

            Assert.assertEquals(expected.getX(), result.getX(), this.comparisonEpsilon);
            Assert.assertEquals(expected.getY(), result.getY(), this.comparisonEpsilon);
            Assert.assertEquals(expected.getZ(), result.getZ(), this.comparisonEpsilon);

            Report.printToReport("Direction", expected, result);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CELESTIAL_BODY_POLES_AXIS_DIRECTION}
     * 
     * @testedMethod {@link CelestialBodyPolesAxisDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction describing the poles' axis of a celestial body,
     *              and getting of the line containing the origin (center of the body) and
     *              vector.
     * 
     * @input the celestial body and the output frame
     * 
     * @output Line
     * 
     * @testPassCriteria the returned Line contains the center
     *                   of the body and is directed by the Z vector of its oriented frame.
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
            final Frame earthFrame = earth.getTrueRotatingFrame();

            // another frame...
            final Vector3D translationVect = new Vector3D(10.0, 10.0, 10.0);
            final Transform outTransform = new Transform(AbsoluteDate.J2000_EPOCH, translationVect);
            final Frame outputFrame = new Frame(earthFrame, outTransform, "outFram");
            ;

            // direction creation
            final CelestialBodyPolesAxisDirection direction = new CelestialBodyPolesAxisDirection(earth);

            // the date has no meaning here
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

            // expected directing vector
            final Vector3D directingVect = outTransform.transformVector(Vector3D.PLUS_K);

            // origin of the expected line
            final Vector3D resultPos = earth.getPVCoordinates(date, outputFrame).getPosition();

            // result line creation
            final Line line = direction.getLine(null, date, outputFrame);

            // test of the points :
            // contains the center
            Assert.assertTrue(line.contains(resultPos));

            // is directed by the right axis
            Assert.assertTrue(line.contains(resultPos.add(directingVect)));

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
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
