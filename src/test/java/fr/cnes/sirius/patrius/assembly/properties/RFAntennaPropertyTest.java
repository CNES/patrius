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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for the RFAntennaProperty class.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class RFAntennaPropertyTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle RF antenna property
         * 
         * @featureDescription RF antenna properties validation
         * 
         * @coveredRequirements DV-VEHICULE_300, DV-VEHICULE_310, DV-VEHICULE_320
         */
        RF_ANTENNA_PROPERTY
    }

    /**
     * The RF antenna property.
     */
    static RFAntennaProperty property;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RF_ANTENNA_PROPERTY}
     * 
     * @testedMethod {@link RFAntennaProperty#RFAntennaProperty(double, double[], double[], double[][], double[][], double, double, double, double)}
     * 
     * @description test the construction of the RF antenna property
     * 
     * @input the RF antenna property constructor parameters
     * 
     * @output the thrown exceptions
     * 
     * @testPassCriteria the construction using not proper parameters should throw exceptions.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testAntennaPropertyConstruction() {
        // Test 1 - two values of polar angles:
        double[] xx = new double[] { 0.3, 5.9 };
        double[] yy = new double[] { 0.0, FastMath.PI / 2., FastMath.PI };
        double[][] zz = new double[][] { { 7., 6., 5. }, { 5., 4., 3. } };
        // creates the RF antenna property
        boolean rez = false;
        try {
            property = new RFAntennaProperty(10, xx, yy, zz, zz, 0.1, 0.1, 45E06, 8E09);
        } catch (final MathIllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        // Test 2 - two values of azimuthal angles:
        xx = new double[] { 1.2, 0.8, 3.0 };
        yy = new double[] { 0.0, 2. * FastMath.PI };
        zz = new double[][] { { 7., 6. }, { 5., 4. }, { 1., 6. } };
        // creates the RF antenna property
        rez = false;
        try {
            property = new RFAntennaProperty(10, xx, yy, zz, zz, 0.1, 0.1, 45E06, 8E09);
        } catch (final MathIllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        // Test 3 - polar angle number of points and matrix row dimension do not correspond:
        xx = new double[] { 1.2, 0.8, 3.0, 1.4 };
        yy = new double[] { 0.0, FastMath.PI, 2. * FastMath.PI };
        zz = new double[][] { { 7., 6., 1. }, { 5., 4., .1 }, { 1., 6., 3. } };
        double[][] zzp = new double[][] { { 7., 6., 1. }, { 5., 4., .1 }, { 1., 6., 3. }, { 1., 6., 3. } };
        // creates the RF antenna property
        rez = false;
        try {
            property = new RFAntennaProperty(10, xx, yy, zz, zzp, 0.1, 0.1, 45E06, 8E09);
        } catch (final DimensionMismatchException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        rez = false;
        try {
            property = new RFAntennaProperty(10, xx, yy, zzp, zz, 0.1, 0.1, 45E06, 8E09);
        } catch (final DimensionMismatchException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        // Test 4 - azimuthal angle number of points and matrix column dimension do not correspond:
        xx = new double[] { 1.2, 0.8, 3.0 };
        yy = new double[] { 0.0, FastMath.PI, 2. * FastMath.PI };
        zz = new double[][] { { 7., 6., 1. }, { 5., 4., 1., 9. }, { 1., 6., 3. } };
        zzp = new double[][] { { 7., 6., 1. }, { 5., 4., 1. }, { 1., 6., 3. } };
        // creates the RF antenna property
        rez = false;
        try {
            property = new RFAntennaProperty(10, xx, yy, zz, zzp, 0.1, 0.1, 45E06, 8E09);
        } catch (final DimensionMismatchException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        // creates the RF antenna property
        rez = false;
        try {
            property = new RFAntennaProperty(10, xx, yy, zzp, zz, 0.1, 0.1, 45E06, 8E09);
        } catch (final DimensionMismatchException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        // Test 5 - polar angle array not valid:
        xx = new double[] { -2.0, 0.8, 4.0 };
        yy = new double[] { 0.0, FastMath.PI, 2. * FastMath.PI };
        zz = new double[][] { { 7., 6., 1. }, { 5., 4., 1. }, { 1., 6., 3. } };
        // creates the RF antenna property
        rez = false;
        try {
            property = new RFAntennaProperty(10, xx, yy, zz, zz, 0.1, 0.1, 45E06, 8E09);
        } catch (final MathIllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        // Test 6 - azimuthal angle array not valid:
        xx = new double[] { 0., 0.8, 3.0 };
        yy = new double[] { -0.5, FastMath.PI, 3. * FastMath.PI };
        zz = new double[][] { { 7., 6., 1. }, { 5., 4., 1. }, { 1., 6., 3. } };
        // creates the RF antenna property
        rez = false;
        try {
            property = new RFAntennaProperty(10, xx, yy, zz, zz, 0.1, 0.1, 45E06, 8E09);
        } catch (final MathIllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RF_ANTENNA_PROPERTY}
     * 
     * @testedMethod {@link RFAntennaProperty#getOutputPower()}
     * @testedMethod {@link RFAntennaProperty#getTechnoLoss()}
     * @testedMethod {@link RFAntennaProperty#getCircuitLoss()}
     * @testedMethod {@link RFAntennaProperty#getBitRate()}
     * @testedMethod {@link RFAntennaProperty#getFrequency()}
     * @testedMethod {@link RFAntennaProperty#getType()}
     * 
     * @description test the getters of the RF antenna property
     * 
     * @input the RF antenna property
     * 
     * @output the values
     * 
     * @testPassCriteria the values must be the expected ones.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testAntennaPropertyGetters() {
        // tests the getters:
        Assert.assertEquals(13.4, property.getOutputPower(), 0.0);
        Assert.assertEquals(0.1, property.getTechnoLoss(), 0.0);
        Assert.assertEquals(2.0, property.getCircuitLoss(), 0.0);
        Assert.assertEquals(45.5E6, property.getBitRate(), 0.0);
        Assert.assertEquals(8.253E9, property.getFrequency(), 0.0);
        Assert.assertEquals(PropertyType.RF, property.getType());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RF_ANTENNA_PROPERTY}
     * 
     * @testedMethod {@link RFAntennaProperty#getGain(double, double)}
     * @testedMethod {@link RFAntennaProperty#getEllipticity(double, double)}
     * 
     * @description test the interpolation of the antenna gain and the ellipticity factor
     *              diagrams using simple test cases
     * 
     * @input the RF antenna property
     * 
     * @output the interpolated gain and ellipticity values
     * 
     * @testPassCriteria the interpolated values must be the expected ones.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testAntennaGainAndEllipticityInterpolation() {
        // test 1 - nine values for the gain and ellipticity:
        Assert.assertEquals(7., property.getGain(0., 0.), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(5., property.getGain(FastMath.PI / 2., 0.0), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(6., property.getGain(0.0, FastMath.PI), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(5.0, property.getGain(FastMath.PI / 4., FastMath.PI), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(4.5, property.getGain(FastMath.PI / 4., 17. / 12. * FastMath.PI),
            Precision.DOUBLE_COMPARISON_EPSILON);
        // try to use an invalid input value:
        boolean rez = false;
        try {
            property.getGain(2. * FastMath.PI, FastMath.PI);
        } catch (final MathIllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        rez = false;
        try {
            property.getEllipticity(2. * FastMath.PI, FastMath.PI);
        } catch (final MathIllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        // test 2 - only one value for the gain and ellipticity:
        double[] xx = new double[] { 0.2 };
        double[] yy = new double[] { 0.0 };
        double[][] gain = new double[][] { { 5.287 } };
        double[][] ellip = new double[][] { { 0.784 } };
        // creates the RF antenna property
        property = new RFAntennaProperty(13.4, xx, yy, gain, ellip, 0.1, 2.0, 45.5E06, 8.253E09);
        Assert.assertEquals(5.287, property.getGain(0., 0.), 0.0);
        Assert.assertEquals(5.287, property.getGain(0.98, 1.46), 0.0);
        Assert.assertEquals(0.784, property.getEllipticity(3.0, FastMath.PI), 0.0);
        Assert.assertEquals(0.784, property.getEllipticity(0.2, 0), 0.0);

        // test 3 - one value for the polar angle, three values for the azimuthal angle:
        xx = new double[] { 0.2 };
        yy = new double[] { 0.0, 0.5, 1.0 };
        gain = new double[][] { { 0., 5., 10. } };
        ellip = new double[][] { { 1., 6., 11. } };
        // creates the RF antenna property
        property = new RFAntennaProperty(13.4, xx, yy, gain, ellip, 0.1, 2.0, 45.5E06, 8.253E09);
        Assert.assertEquals(5., property.getGain(0., 0.5), 0.0);
        Assert.assertEquals(7., property.getGain(0.6538, 0.7), 0.0);
        Assert.assertEquals(11., property.getEllipticity(3.0, 1.5), 0.0);
        Assert.assertEquals(8., property.getEllipticity(0.2, 0.7), 0.0);

        // test 3 - one value for the azimuthal angle, three values for the polar angle:
        xx = new double[] { 0.2, 0.4, 0.6 };
        yy = new double[] { 0.0 };
        gain = new double[][] { { 2. }, { 4. }, { 6. } };
        ellip = new double[][] { { 0. }, { 5. }, { 10. } };
        // creates the RF antenna property
        property = new RFAntennaProperty(13.4, xx, yy, gain, ellip, 0.1, 2.0, 45.5E06, 8.253E09);
        Assert.assertEquals(2., property.getGain(0.2, 0.0), 0.0);
        Assert.assertEquals(3., property.getGain(0.3, 2.3), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(10., property.getEllipticity(1.0, 0.7), 0.0);
    }

    /**
     * Setup for all unit tests in the class.
     * It provides an RF antenna property.
     * 
     * @throws PatriusException
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        // sets the gain and ellipticity diagram values:
        final double[] xx = new double[] { 0.0, FastMath.PI / 2., FastMath.PI };
        final double[] yy = new double[] { FastMath.PI / 6., FastMath.PI, 11. * FastMath.PI / 6. };
        final double[][] zzg = new double[][] { { 7., 6., 5. }, { 5., 4., 3. }, { 3., 2., 1. } };
        final double[][] zze = new double[][] { { 10., 9., 8. }, { 8., 7., 6. }, { 6., 5., 4. } };
        // creates the RF antenna property
        property = new RFAntennaProperty(13.4, xx, yy, zzg, zze, 0.1, 2.0, 45.5E06, 8.253E09);
    }
}
