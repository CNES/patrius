/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history Created 07/11/2012
 *
 * HISTORY
 * VERSION:4.11:FA:FA-3316:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un potentiel variable
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.variations.coefficients;

import org.junit.Test;
import org.testng.Assert;

import fr.cnes.sirius.patrius.forces.gravity.variations.coefficients.VariablePotentialCoefficientsSet.PeriodicComputationMethod;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class
 * 
 * @see VariablePotentialCoefficientsSet
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: VariablePotentialCoefficientsSetTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */
public class VariablePotentialCoefficientsSetTest {

    /** threshold */
    private final double eps = Precision.EPSILON;

    /** Features description. */
    public enum features {

        /**
         * @featureTitle variable coefficients set
         * 
         * @featureDescription test the variable coefficients
         * 
         * @coveredRequirements DV-MOD_190, DV-MOD_220, DV-MOD_230
         */
        VARIABLE_COEFFICIENTS
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS}
     * 
     * @testedMethod {@link VariablePotentialCoefficientsSet#VariablePotentialCoefficientsSet(int, int, double, double, double[], double[])}
     * 
     * @description tests the constructor with faulty params
     * 
     * @input set parameters
     * 
     * @output exceptions
     * 
     * @testPassCriteria expected exceptions are thrown
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testVariablePotentialCoefficientsSet() {
        new VariablePotentialCoefficientsSet(2, 2, 1, 2, new double[] { 1, 2, 3, 4, 5 }, new double[] { 8, 9, 10, 11,
            12 });
        try {
            new VariablePotentialCoefficientsSet(2, -1, 1, 2, new double[] { 1, 2, 3, 4, 5, 6 }, new double[] { 7, 8,
                9, 10, 11, 12 });
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }
        try {
            new VariablePotentialCoefficientsSet(2, 2, 1, 2, new double[] { 1, 2, 3, 4, 5, 6 }, new double[] { 7, 8, 9,
                10, 11 });
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS}
     * 
     * @testedMethod {@link VariablePotentialCoefficientsSet#getCoefC()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getCoefS()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getCoefCDrift()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getCoefCSin1A()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getCoefCCos1A()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getCoefCSin2A()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getCoefCCos2A()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getCoefSDrift()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getCoefSSin1A()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getCoefSCos1A()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getCoefSSin2A()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getCoefSCos2A()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getDegree()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getOrder()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getPeriodicComputationMethod()}
     * 
     * @description tests the getters of the coefficients set
     * 
     * @input a VariablePotentialCoefficientsSet
     * 
     * @output the different constants
     * 
     * @testPassCriteria actual values are the same as the expected ones. threshold is 1e-16
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testGetters() {
        final VariablePotentialCoefficientsSet set = new VariablePotentialCoefficientsSet(2, 3, 1, 2, new double[] { 1,
            2, 3, 4, 5 }, new double[] { 8, 9, 10, 11, 12 });

        Assert.assertEquals(2, set.getDegree());
        Assert.assertEquals(3, set.getOrder());
        Assert.assertEquals(0, (1 - set.getCoefC()), this.eps);
        Assert.assertEquals(0, (2 - set.getCoefS()), this.eps);

        // get c coefficient corrections
        Assert.assertEquals(1, set.getCoefCDrift(), this.eps);
        Assert.assertEquals(2, set.getCoefCSin1A(), this.eps);
        Assert.assertEquals(3, set.getCoefCCos1A(), this.eps);
        Assert.assertEquals(4, set.getCoefCSin2A(), this.eps);
        Assert.assertEquals(5, set.getCoefCCos2A(), this.eps);
        // get s coefficient corrections
        Assert.assertEquals(8, set.getCoefSDrift(), this.eps);
        Assert.assertEquals(9, set.getCoefSSin1A(), this.eps);
        Assert.assertEquals(10, set.getCoefSCos1A(), this.eps);
        Assert.assertEquals(11, set.getCoefSSin2A(), this.eps);
        Assert.assertEquals(12, set.getCoefSCos2A(), this.eps);
        VariablePotentialCoefficientsSet.setPeriodicComputationMethod(PeriodicComputationMethod.HOMOGENEOUS);
        Assert.assertTrue(VariablePotentialCoefficientsSet.getPeriodicComputationMethod().equals(
            PeriodicComputationMethod.HOMOGENEOUS));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS}
     * 
     * @testedMethod {@link VariablePotentialCoefficientsSet#computeCDriftComponent(double)}
     * @testedMethod {@link VariablePotentialCoefficientsSet#computeSDriftComponent(double)}
     * @testedMethod {@link VariablePotentialCoefficientsSet#computeCPeriodicComponent(double, double, double, double)}
     * @testedMethod {@link VariablePotentialCoefficientsSet#computeSPeriodicComponent(double, double, double, double))}
     * @testedMethod {@link VariablePotentialCoefficientsSet#computeDriftFunction(AbsoluteDate, AbsoluteDate)}
     * @testedMethod {@link VariablePotentialCoefficientsSet#setPeriodicComputationMethod(PeriodicComputationMethod)}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getPeriodicComputationMethod()}
     * 
     * @description tests the compute method of the coefficients set
     * 
     * @input a VariablePotentialCoefficientsSet
     * 
     * @output the different constants
     * 
     * @testPassCriteria actual values are the same as the expected ones. threshold is 1e-16
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testCompute() throws PatriusException {
        final VariablePotentialCoefficientsSet set = new VariablePotentialCoefficientsSet(2, 3, 1, 2, new double[] { 1,
            2, 3, 4, 5 }, new double[] { 8, 9, 10, 11, 12 });
        // Test compute drift component
        Assert.assertEquals(2, set.computeCDriftComponent(2.0), this.eps);
        Assert.assertEquals(24, set.computeSDriftComponent(3.0), this.eps);
        Assert.assertEquals(14.0, set.computeCPeriodicComponent(1, 1, 1, 1));
        Assert.assertEquals(42.0, set.computeSPeriodicComponent(1, 1, 1, 1));
        final AbsoluteDate date = new AbsoluteDate(2023, 1, 1, 6, 0, 0.);
        final AbsoluteDate refDate = new AbsoluteDate(2022, 1, 1, 0, 0, 0.);
        Assert.assertEquals(1, VariablePotentialCoefficientsSet.computeDriftFunction(date, refDate), 0);
        VariablePotentialCoefficientsSet.setPeriodicComputationMethod(PeriodicComputationMethod.LEAP_YEAR);
        Assert.assertTrue(VariablePotentialCoefficientsSet.getPeriodicComputationMethod().equals(
            PeriodicComputationMethod.LEAP_YEAR));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS}
     * 
     * @testedMethod {@link VariablePotentialCoefficientsSet#testcomputePeriodicFunctions(AbsoluteDate)}
     * @testedMethod {@link VariablePotentialCoefficientsSet#isLeapYear(integer)}
     * 
     * @description tests the compute method of the coefficients set
     * 
     * @input a VariablePotentialCoefficientsSet
     * 
     * @output the different constants
     * 
     * @testPassCriteria actual values are the same as the expected ones. threshold is 1e-16
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testcomputePeriodicFunctions() throws PatriusException {
        final VariablePotentialCoefficientsSet set = new VariablePotentialCoefficientsSet(2, 3, 1, 2, new double[] { 1,
            2, 3, 4, 5 }, new double[] { 8, 9, 10, 11, 12 });
        // Test HOMEGENEOUS
        VariablePotentialCoefficientsSet.setPeriodicComputationMethod(PeriodicComputationMethod.HOMOGENEOUS);
        final AbsoluteDate date = new AbsoluteDate(2006, 1, 1, 6, 0, 0., TimeScalesFactory.getTAI());
        final double[] sincos = VariablePotentialCoefficientsSet.computePeriodicFunctions(date);
        Assert.assertEquals(0, sincos[0], 1E-15);
        Assert.assertEquals(1, sincos[1], 1E-15);
        Assert.assertEquals(0, sincos[2], 1E-15);
        Assert.assertEquals(1, sincos[3], 1E-15);

        // Test LEAP_YEAR
        VariablePotentialCoefficientsSet.setPeriodicComputationMethod(PeriodicComputationMethod.LEAP_YEAR);
        final AbsoluteDate dateLeapYear = new AbsoluteDate(2016, 7, 2, TimeScalesFactory.getTAI());
        final double[] sincosLeapYear = VariablePotentialCoefficientsSet.computePeriodicFunctions(dateLeapYear);
        Assert.assertEquals(0, sincosLeapYear[0], 1E-15);
        Assert.assertEquals(-1, sincosLeapYear[1], 1E-15);
        Assert.assertEquals(0, sincosLeapYear[2], 1E-15);
        Assert.assertEquals(1, sincosLeapYear[3], 1E-15);
    }

    /**
     * Covers branch isLeapYear(year) is false in computeElapsedPeriodic (LEAP_YEAR)
     * 
     * @testedMethod {@link PeriodicComputationMethod#computeElapsedPeriodic(AbsoluteDate)} (LEAP_YEAR branch)
     */
    @Test
    public void testComputePeriodicFunctionsLeapComputationNoLeap() {
        // Test LEAP_YEAR computation for non leap year
        VariablePotentialCoefficientsSet.setPeriodicComputationMethod(PeriodicComputationMethod.LEAP_YEAR);
        final AbsoluteDate dateLeapYear = new AbsoluteDate(2015, 7, 2, TimeScalesFactory.getTAI());
        // July 2nd 2015 is the 182nd day of 2015. It is not a leap year so it has 365 days.
        final double[] sincosLeapYear = VariablePotentialCoefficientsSet.computePeriodicFunctions(dateLeapYear);
        // The angle is pi *(364/365)
        final double angle = Math.PI * (364.0 / 365.0);
        final double twoAngle = 2.0 * angle;
        Assert.assertEquals(Math.sin(angle), sincosLeapYear[0], 1E-15);
        Assert.assertEquals(Math.cos(angle), sincosLeapYear[1], 1E-15);
        Assert.assertEquals(Math.sin(twoAngle), sincosLeapYear[2], 1E-15);
        Assert.assertEquals(Math.cos(twoAngle), sincosLeapYear[3], 1E-15);
    }

    /**
     * Covers condition (year % HUNDRED) != 0) in isLeapYear . The year 1900 was not a leap year.
     */
    @Test
    public void testcomputePeriodicFunctionsNotLeapYear() {

        // Test LEAP_YEAR
        VariablePotentialCoefficientsSet.setPeriodicComputationMethod(PeriodicComputationMethod.LEAP_YEAR);
        final AbsoluteDate dateLeapYear = new AbsoluteDate(1900, 7, 2, TimeScalesFactory.getTAI());
        final double[] sincosLeapYear = VariablePotentialCoefficientsSet.computePeriodicFunctions(dateLeapYear);
        // The angle is pi *(364/365)
        final double angle = Math.PI * (364.0 / 365.0);
        final double twoAngle = 2.0 * angle;
        Assert.assertEquals(Math.sin(angle), sincosLeapYear[0], 1E-15);
        Assert.assertEquals(Math.cos(angle), sincosLeapYear[1], 1E-15);
        Assert.assertEquals(Math.sin(twoAngle), sincosLeapYear[2], 1E-15);
        Assert.assertEquals(Math.cos(twoAngle), sincosLeapYear[3], 1E-15);
    }

    /**
     * Covers condition (year % FOUR_HUNDRED) == 0) in isLeapYear . The year 2000 was a leap year.
     */
    @Test
    public void testcomputePeriodicFunctionsLeapYearMult400() {

        // Test LEAP_YEAR
        VariablePotentialCoefficientsSet.setPeriodicComputationMethod(PeriodicComputationMethod.LEAP_YEAR);
        final AbsoluteDate dateLeapYear = new AbsoluteDate(2000, 7, 2, TimeScalesFactory.getTAI());
        final double[] sincosLeapYear = VariablePotentialCoefficientsSet.computePeriodicFunctions(dateLeapYear);
        Assert.assertEquals(0, sincosLeapYear[0], 1E-15);
        Assert.assertEquals(-1, sincosLeapYear[1], 1E-15);
        Assert.assertEquals(0, sincosLeapYear[2], 1E-15);
        Assert.assertEquals(1, sincosLeapYear[3], 1E-15);
    }

}