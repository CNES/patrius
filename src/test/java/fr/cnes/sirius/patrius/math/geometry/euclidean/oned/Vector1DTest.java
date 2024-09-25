/**
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2082:15/05/2019:Modifications mineures d'api
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.oned;

import java.text.NumberFormat;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathUnsupportedOperationException;

/**
 * Vector1D class test.
 * 
 * @author tournebizej
 * 
 * @version $Id: Vector1DTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.2
 * 
 */
public class Vector1DTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Code coverage
         * 
         * @featureDescription Code coverage
         * 
         * @coveredRequirements none
         */
        CODE_COVERAGE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CODE_COVERAGE}
     * 
     * @testedMethod misc
     * 
     * @description Code coverage
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria misc
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testConstructors() {
        final Vector1D v1 = new Vector1D(1);
        final Vector1D v2 = new Vector1D(5);

        Assert.assertEquals(1, v1.getX(), 0.);

        Assert.assertEquals(10., new Vector1D(2, v2).getX(), 0.);

        final Vector1D l2 = new Vector1D(2., v1, 3., v2);
        Assert.assertEquals(17., l2.getX(), 0.);

        final Vector1D l3 = new Vector1D(2., v1, 3., v2, 4, v2);
        Assert.assertEquals(37., l3.getX(), 0.);

        final Vector1D l4 = new Vector1D(2., v1, 3., v2, 10., l2, 100., l3);
        Assert.assertEquals(3887., l4.getX(), 0.);

        // creation from a RealVector test
        final double[] dataRes = l4.getRealVector().toArray();
        Assert.assertEquals(3887., dataRes[0], 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CODE_COVERAGE}
     * 
     * @testedMethod misc
     * 
     * @description Code coverage
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria misc
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testMethods() {
        final Vector1D v1 = new Vector1D(3.);

        Assert.assertEquals(new Vector1D(0), v1.getZero());
        Assert.assertEquals(3., v1.getNorm1(), 0.);
        Assert.assertEquals(9., v1.getNormSq(), 0.);
        Assert.assertEquals(3., v1.getNorm(), 0.);
        Assert.assertEquals(3., v1.getNormInf(), 0.);

        final Euclidean1D e = (Euclidean1D) v1.getSpace();
        Assert.assertEquals(1, e.getDimension());
        try {
            e.getSubSpace().getDimension();
            Assert.fail("expecting MathUnsupportedOperationException");
        } catch (final MathUnsupportedOperationException ex) {
            // expected
        }

        final Vector1D v2 = new Vector1D(2);
        Assert.assertEquals(5., v1.add(v2).getX(), 0.);
        Assert.assertEquals(23., v1.add(10., v2).getX(), 0.);
        Assert.assertEquals(1., v1.subtract(v2).getX(), 0.);
        Assert.assertEquals(-17., v1.subtract(10., v2).getX(), 0.);

        Assert.assertEquals(1., v1.normalize().getX(), 0.);

        try {
            final Vector1D v0 = new Vector1D(0);
            v0.normalize();
            Assert.fail("expecting MathArithmeticException");
        } catch (final MathArithmeticException ex) {
            // expected
        }

        Assert.assertEquals(-2., v2.negate().getX(), 0.);

        Assert.assertEquals(20., v2.scalarMultiply(10.).getX(), 0.);

        Assert.assertFalse(v2.isNaN());
        Vector1D v3 = new Vector1D(Double.POSITIVE_INFINITY);
        Assert.assertTrue(v3.isInfinite());
        v3 = new Vector1D(Double.NEGATIVE_INFINITY);
        Assert.assertTrue(v3.isInfinite());

        Assert.assertEquals(Double.POSITIVE_INFINITY, v2.distance1(v3), 0.);
        Assert.assertEquals(1., v1.distance1(v2), 0.);
        Assert.assertEquals(1., v1.distance(v2), 0.);
        Assert.assertEquals(1., v1.distanceInf(v2), 0.);
        Assert.assertEquals(1., v1.distanceSq(v2), 0.);
        Assert.assertEquals(6., v1.dotProduct(v2), 0.);
        Assert.assertEquals(v1.subtract(v2).getNorm(), Vector1D.distance(v1, v2), 0.);
        Assert.assertEquals(1., Vector1D.distanceInf(v1, v2), 0.);
        Assert.assertEquals(1., Vector1D.distanceSq(v1, v2), 0.);

        v3 = new Vector1D(Double.NaN);
        Assert.assertFalse(v1.equals(v3));
        Assert.assertTrue(v1.equals(v1));

        Assert.assertEquals(1.073741824E9, v2.hashCode(), 0.);
        Assert.assertEquals(7785., v3.hashCode(), 0.);

        Assert.assertEquals("{(NaN)}", v3.toString());
        final NumberFormat nf = NumberFormat.getInstance(Locale.FRENCH);
        Assert.assertEquals("{3}", v1.toString(nf));

        final Vector1D v4 = new Vector1D(1.);
        final double[] dataRes = v4.getRealVector().toArray();
        Assert.assertEquals(1., dataRes[0], 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CODE_COVERAGE}
     * 
     * @testedMethod misc
     * 
     * @description Code coverage
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria misc
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testEquals() {
        // for code coverage
        final Vector1D any = new Vector1D(0.5);
        Assert.assertFalse(any.equals("bogus"));
    }

}
