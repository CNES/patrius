/**
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
 * HISTORY
* VERSION:4.7:DM:DM-2909:18/05/2021:Ajout des methodes getIntersectionPoint, getClosestPoint(Vector2D) et getAlpha()
* VERSION:4.3:DM:DM-2082:15/05/2019:Modifications mineures d'api
* VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.twod;

import java.text.NumberFormat;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Vector2D class test.
 * 
 * @author tournebizej
 * 
 * @version $Id: Vector2DTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.2
 * 
 */
public class Vector2DTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Vector 2D
         * 
         * @featureDescription test the vector 2D class constructors
         * 
         * @coveredRequirements DV-MATHS_120, DV-MATHS_130, DV-MATHS_140, DV-MATHS_150, DV-MATHS_160
         */
        VECTOR2D_CONSTRUCTORS,

        /**
         * @featureTitle Vector 2D
         * 
         * @featureDescription test the vector 2D class and methods
         * 
         * @coveredRequirements DV-MATHS_120, DV-MATHS_130, DV-MATHS_140, DV-MATHS_150, DV-MATHS_160
         */
        VECTOR2D_METHODS

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VECTOR2D_CONSTRUCTORS}
     * 
     * @testedMethod {@link Vector2D#Vector2D(double[])}
     * @testedMethod {@link Vector2D#Vector2D(double, double)}
     * @testedMethod {@link Vector2D#Vector2D(double, Vector2D)}
     * @testedMethod {@link Vector2D#Vector2D(double, Vector2D, double, Vector2D)}
     * @testedMethod {@link Vector2D#Vector2D(double, Vector2D, double, Vector2D, double, Vector2D)}
     * @testedMethod {@link Vector2D#Vector2D(double, Vector2D, double, Vector2D, double, Vector2D, double, Vector2D)}
     * 
     * @description Test Vector2D constructors
     * 
     * @input parameters
     * 
     * @output vector3D
     * 
     * @testPassCriteria No exception is raised for nominal cases, a DimensionMismatchException is raised for degraded
     *                   cases. We check the returned elements with the ones given at the construction with an epsilon
     *                   of 1e-14 which takes into account the machine error only.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testConstructors() {
        final double[] t2 = { 0, 1 };
        final double[] t3 = { 0, 1, 3 };
        final Vector2D v1 = new Vector2D(0, 1);
        final Vector2D v2 = new Vector2D(1, 2);

        final Vector2D v1t1 = new Vector2D(t2);
        Assert.assertEquals(v1.getX(), v1t1.getX(), 0.);
        Assert.assertEquals(v1.getY(), v1t1.getY(), 0.);

        try {
            new Vector2D(t3);
            Assert.fail("expecting DimensionMismatchException");
        } catch (final DimensionMismatchException ex) {
            // expected
        }

        Assert.assertEquals(2., new Vector2D(2., v2).getX(), 0.);
        Assert.assertEquals(4., new Vector2D(2., v2).getY(), 0.);

        final Vector2D l2 = new Vector2D(2., v1, 3., v2);
        Assert.assertEquals(3., l2.getX(), 0.);
        Assert.assertEquals(8., l2.getY(), 0.);

        final Vector2D l3 = new Vector2D(2., v1, 3., v2, 10., l2);
        Assert.assertEquals(33., l3.getX(), 0.);
        Assert.assertEquals(88., l3.getY(), 0.);

        final Vector2D l4 = new Vector2D(2., v1, 3., v2, 10., l2, 100., l3);
        Assert.assertEquals(3333., l4.getX(), 0.);
        Assert.assertEquals(8888., l4.getY(), 0.);

        // creation from a RealVector test
        final double[] dataRes = l4.getRealVector().toArray();
        Assert.assertEquals(3333., dataRes[0], 0.);
        Assert.assertEquals(8888., dataRes[1], 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VECTOR2D_METHODS}
     * 
     * @testedMethod {@link Vector2D#add(fr.cnes.sirius.patrius.math.geometry.Vector)}
     * @testedMethod {@link Vector2D#add(double, fr.cnes.sirius.patrius.math.geometry.Vector)}
     * @testedMethod {@link Vector2D#distance(fr.cnes.sirius.patrius.math.geometry.Vector)}
     * @testedMethod {@link Vector2D#distance1(fr.cnes.sirius.patrius.math.geometry.Vector)}
     * @testedMethod {@link Vector2D#distanceInf(fr.cnes.sirius.patrius.math.geometry.Vector)}
     * @testedMethod {@link Vector2D#distanceSq(fr.cnes.sirius.patrius.math.geometry.Vector)}
     * @testedMethod {@link Vector2D#dotProduct(fr.cnes.sirius.patrius.math.geometry.Vector)}
     * @testedMethod {@link Vector2D#equals(Object)}
     * @testedMethod {@link Vector2D#getNorm()}
     * @testedMethod {@link Vector2D#getNorm1()}
     * @testedMethod {@link Vector2D#getNormInf()}
     * @testedMethod {@link Vector2D#getNormSq()}
     * @testedMethod {@link Vector2D#getSpace()}
     * @testedMethod {@link Vector2D#getX()}
     * @testedMethod {@link Vector2D#getY()}
     * @testedMethod {@link Vector2D#getZero()}
     * @testedMethod {@link Vector2D#hashCode()}
     * @testedMethod {@link Vector2D#isInfinite()}
     * @testedMethod {@link Vector2D#isNaN()}
     * @testedMethod {@link Vector2D#negate()}
     * @testedMethod {@link Vector2D#normalize()}
     * @testedMethod {@link Vector2D#scalarMultiply(double)}
     * @testedMethod {@link Vector2D#subtract(fr.cnes.sirius.patrius.math.geometry.Vector)}
     * @testedMethod {@link Vector2D#subtract(double, fr.cnes.sirius.patrius.math.geometry.Vector)}
     * @testedMethod {@link Vector2D#toString()}
     * @testedMethod {@link Vector2D#toString(NumberFormat)}
     * @testedMethod {@link Vector2D#getRealVector()}
     * 
     * @description Test Vector2D methods
     * 
     * @input parameters
     * 
     * @output vector3D
     * 
     * @testPassCriteria No exception is raised for nominal cases, an MathArithmeticException is raised for degraded
     *                   cases. We check the returned elements with the ones given at the construction with an epsilon
     *                   of 1e-14 which takes into account the machine error only.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testMethods() {
        final double[] t1 = { 0., 3. };
        final Vector2D v1 = new Vector2D(0., 3.);
        Assert.assertEquals(t1[0], v1.toArray()[0], 0.);
        Assert.assertEquals(t1[1], v1.toArray()[1], 0.);

        final Euclidean2D e = (Euclidean2D) v1.getSpace();
        Assert.assertEquals(2, e.getDimension());
        Assert.assertEquals(1, e.getSubSpace().getDimension());

        Assert.assertEquals(new Vector2D(0, 0), v1.getZero());
        Assert.assertEquals(3., v1.getNorm1(), 0.);
        Assert.assertEquals(9., v1.getNormSq(), 0.);
        Assert.assertEquals(3., v1.getNorm(), 0.);
        Assert.assertEquals(3., v1.getNormInf(), 0.);

        final Vector2D v2 = new Vector2D(1, 2);
        Assert.assertEquals(1., v1.add(v2).getX(), 0.);
        Assert.assertEquals(5., v1.add(v2).getY(), 0.);
        Assert.assertEquals(10., v1.add(10., v2).getX(), 0.);
        Assert.assertEquals(203., v1.add(100., v2).getY(), 0.);
        Assert.assertEquals(-1., v1.subtract(v2).getX(), 0.);
        Assert.assertEquals(1., v1.subtract(v2).getY(), 0.);
        Assert.assertEquals(-17., v1.subtract(10., v2).getY(), 0.);

        Assert.assertEquals(0., v1.normalize().getX(), 0.);
        Assert.assertEquals(1., v1.normalize().getY(), 0.);

        try {
            final Vector2D v0 = new Vector2D(0, 0);
            v0.normalize();
            Assert.fail("expecting MathArithmeticException");
        } catch (final MathArithmeticException ex) {
            // expected
        }

        Assert.assertEquals(-1., v2.negate().getX(), 0.);
        Assert.assertEquals(-2., v2.negate().getY(), 0.);

        Assert.assertEquals(10., v2.scalarMultiply(10.).getX(), 0.);
        Assert.assertEquals(20., v2.scalarMultiply(10.).getY(), 0.);

        Assert.assertFalse(v2.isNaN());
        Vector2D v3 = new Vector2D(Double.POSITIVE_INFINITY, 2.);
        Assert.assertTrue(v3.isInfinite());
        v3 = new Vector2D(Double.NEGATIVE_INFINITY, 2.);
        Assert.assertTrue(v3.isInfinite());

        Assert.assertEquals(Double.POSITIVE_INFINITY, v2.distance1(v3), 0.);
        Assert.assertEquals(2., v1.distance1(v2), 0.);
        Assert.assertEquals(MathLib.sqrt(2.), v1.distance(v2), 0.);
        Assert.assertEquals(1., v1.distanceInf(v2), 0.);
        Assert.assertEquals(2., v1.distanceSq(v2), 0.);
        Assert.assertEquals(6., v1.dotProduct(v2), 0.);
        Assert.assertEquals(v1.subtract(v2).getNorm(), Vector2D.distance(v1, v2), 0.);
        Assert.assertEquals(1., Vector2D.distanceInf(v1, v2), 0.);
        Assert.assertEquals(2., Vector2D.distanceSq(v1, v2), 0.);

        v3 = new Vector2D(Double.NaN, 2.);
        Assert.assertFalse(v1.equals(v3));
        Assert.assertTrue(v1.equals(v1));
        Assert.assertFalse(v1.equals(e));

        Assert.assertEquals(1.015021568E9, v2.hashCode(), 0.);
        Assert.assertEquals(542., v3.hashCode(), 0.);

        Assert.assertEquals("{(NaN); 2}", v3.toString());
        final NumberFormat nf = NumberFormat.getInstance(Locale.FRENCH);
        Assert.assertEquals("{0; 3}", v1.toString(nf));

        final Vector2D v4 = new Vector2D(new double[] { 1., 2. });
        final double[] dataRes = v4.getRealVector().toArray();
        Assert.assertEquals(1., dataRes[0], 0.);
        Assert.assertEquals(2., dataRes[1], 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VECTOR2D_METHODS}
     * 
     * @testedMethod {@link Vector2D#getAlpha()}
     * 
     * @description Test computation of azimuth (alpha)
     * 
     * @testPassCriteria azimtuh is as expected for vectors i, j and i + j.
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testGetAlpha() {
        // Test i and j vectors
        final Vector2D PLUS_I = new Vector2D(1, 0);
        final Vector2D PLUS_J = new Vector2D(0, 1);
        Assert.assertEquals(0, PLUS_I.getAlpha(), 1.0e-10);
        Assert.assertEquals(FastMath.PI / 2, PLUS_J.getAlpha(), 1.0e-10);
        Assert.assertEquals(FastMath.PI / 4, PLUS_I.add(PLUS_J).getAlpha(), 1.0e-10);
    }
}
