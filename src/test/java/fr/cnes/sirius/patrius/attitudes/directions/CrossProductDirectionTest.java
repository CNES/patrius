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
 * @history creation 09/04/2015
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:404:09/04/2015:creation direction : cross product of two directions
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
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class CrossProductDirectionTest {
    /** Features description. */
    public enum features {

        /**
         * @featureTitle Cross Product direction
         * 
         * @featureDescription cross product of two directions
         * 
         * @coveredRequirements DV-GEOMETRIE_160, DV-GEOMETRIE_170, DV-GEOMETRIE_190
         */
        CROSS_PRODUCT_DIRECTION
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(TabulatedAttitudeTest.class.getSimpleName(), "Cross product direction");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CROSS_PRODUCT_DIRECTION}
     * 
     * @testedMethod {@link CrossProductDirectionTest#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction who is the cross product of two directions
     * 
     * @input two directions
     * 
     * @output cross product direction
     * 
     * @testPassCriteria the returned vector is the correct cross product of the two directions vectors.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testGetVector() {

        Report.printMethodHeader("testGetVector", "Get direction vector", "Math", this.comparisonEpsilon,
            ComparisonType.ABSOLUTE);

        try {

            // the date has no meaning here
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
            // creation of the earth as CelestialBody
            final CelestialBody moon = CelestialBodyFactory.getMoon();
            // creation of the sun as CelestialBody
            final CelestialBody sun = CelestialBodyFactory.getSun();

            // Two directions
            final IDirection d1 = new GenericTargetDirection(moon);
            final IDirection d2 = new GenericTargetDirection(sun);

            final Vector3D vector1 = d1.getVector(null, date, FramesFactory.getGCRF());
            final Vector3D vector2 = d2.getVector(null, date, FramesFactory.getGCRF());

            // Cross Product direction
            final IDirection direction = new CrossProductDirection(d1, d2);

            // expected vector
            final Vector3D expected = Vector3D.crossProduct(vector1, vector2);

            // result Vector
            final Vector3D result = direction.getVector(null, date, FramesFactory.getGCRF());

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
     * @testedFeature {@link features#CROSS_PRODUCT_DIRECTION}
     * 
     * @testedMethod {@link CrossProductDirectionTest#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction who is the cross product of two directions
     *              and getting of the line containing the origin (center of the body) and vector.
     * 
     * @input two directions
     * 
     * @output Line
     * 
     * @testPassCriteria the returned Line contains ????????????????????????
     *                   The 1.0e-14 epsilon is the simple double comparison epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testGetLine() {

        try {
            // the date has no meaning here
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
            // the date has no meaning here
            final Vector3D vector1 = new Vector3D(1., 0., 0.);
            final Vector3D vector2 = new Vector3D(0., 1., 0.);
            // Two directions
            final IDirection d1 = new ConstantVectorDirection(vector1, FramesFactory.getGCRF());
            final IDirection d2 = new ConstantVectorDirection(vector2, FramesFactory.getGCRF());

            // Cross Product direction
            final IDirection direction = new CrossProductDirection(d1, d2);

            // expected directing vector
            final Vector3D directingVect = direction.getVector(null, date, FramesFactory.getGCRF());

            // Line 1 (origin: zero)

            // result line creation with coordinates = null
            final Line line = direction.getLine(null, date, FramesFactory.getGCRF());

            // test of the points
            final Vector3D resultPos = new Vector3D(0., 0., 1.);
            Assert.assertTrue(line.contains(resultPos));
            Assert.assertTrue(line.contains(resultPos.add(directingVect)));

            // Line 2: (origin not zero)

            // result line creation with coordinates != ZERO
            final Vector3D origin = Vector3D.PLUS_I;
            final Line line2 = direction.getLine(new BasicPVCoordinatesProvider(
                new PVCoordinates(origin, Vector3D.ZERO), FramesFactory.getGCRF()), date, FramesFactory.getGCRF());

            // test of the points
            Assert.assertTrue(line2.contains(origin));

            // is directed by the right axis
            Assert.assertTrue(line2.contains(origin.add(directingVect)));

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
