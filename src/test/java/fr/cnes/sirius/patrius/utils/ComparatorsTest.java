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
 * @history Creation 22/07/11
 */
package fr.cnes.sirius.patrius.utils;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.Comparators;

/**
 * @description test class for the doubles comparisons
 * 
 * @author Thomas TRAPIER
 * 
 * @version $Id$
 * 
 * @since 1.0
 * 
 */
public class ComparatorsTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Double comparisons using epsilons.
         * 
         * @featureDescription Classical methods to compare doubles using an epsilon, given or with a default value.
         * 
         * @coveredRequirements DV-MATHS_30
         */
        DOUBLES_COMPARISONS

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DOUBLES_COMPARISONS}
     * 
     * @testedMethod {@link Comparators#equals(double, double)}
     * 
     * @description Nominal cases for doubles equality test.
     * 
     * @input Double x. A real value, quasi 0.0, infinity and a NaN are tested.
     * @input Double y. Four values are tested for a nominal "x" : greater, lower, 0.0 and quasi equal (found equal by
     *        the method using a default epsilon).
     * 
     * @output Boolean set to true if the doubles are equal, false otherwise.
     * 
     * @testPassCriteria The boolean returned is always the one expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testEqualDoubleDouble() {
        // nominal cases : two real doubles
        Assert.assertEquals(Comparators.equals(1.987654687654968, 1.98765468766), false);
        Assert.assertEquals(Comparators.equals(1.987654687654968, 1.98765468764), false);
        Assert.assertEquals(Comparators.equals(1.987654687654968, 0.0), false);
        Assert.assertEquals(Comparators.equals(1.00000000000000001e-300, 0.0), false);
        Assert.assertEquals(Comparators.equals(1.0000000000001e-300, 1.0e-300), false);
        Assert.assertEquals(Comparators.equals(1.00000000000001e-300, 1.0e-300), true);
        Assert.assertEquals(Comparators.equals(1.987654687654968, 1.987654687654972), true);
        // tests with an "infinity" or "NaN" value
        Assert.assertEquals(Comparators.equals(Double.NEGATIVE_INFINITY, 1.987654687654972), false);
        Assert.assertEquals(Comparators.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), true);
        Assert.assertEquals(Comparators.equals(Double.NaN, 1.987654687654972), false);
        Assert.assertEquals(Comparators.equals(Double.NaN, Double.NaN), false);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DOUBLES_COMPARISONS}
     * 
     * @testedMethod {@link Comparators#lowerOrEqual(double, double)}
     * 
     * @description Nominal cases for doubles lower or equal test.
     * 
     * @input Double x. A real value, infinity and a NaN are tested
     * @input Double y. The following values are tested for a nominal "x" : greater, lower, quasi equal (found equal by
     *        the method using a default epsilon), NaN and infinity
     * 
     * @output Boolean set to true if the x <= y, false otherwise.
     * 
     * @testPassCriteria The boolean returned is always the one expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testLowerOrEqualDoubleDouble() {
        // nominal cases : two real doubles
        Assert.assertEquals(Comparators.lowerOrEqual(1.987654687654968, 1.98765468766), true);
        Assert.assertEquals(Comparators.lowerOrEqual(1.987654687654968, 1.98765468764), false);
        Assert.assertEquals(Comparators.lowerOrEqual(1.987654687654968, 1.987654687654972), true);
        // tests with an "infinity" value
        Assert.assertEquals(Comparators.lowerOrEqual(Double.NEGATIVE_INFINITY, 1.987654687654972), true);
        Assert.assertEquals(Comparators.lowerOrEqual(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), true);
        Assert.assertEquals(Comparators.lowerOrEqual(1.987654687654972, Double.NEGATIVE_INFINITY), false);
        Assert.assertEquals(Comparators.lowerOrEqual(1.987654687654972, Double.POSITIVE_INFINITY), true);
        Assert.assertEquals(Comparators.lowerOrEqual(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), true);
        Assert.assertEquals(Comparators.lowerOrEqual(Double.POSITIVE_INFINITY, 1.987654687654972), false);
        // tests with a "NaN" value
        Assert.assertEquals(Comparators.lowerOrEqual(Double.NaN, 1.987654687654972), false);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DOUBLES_COMPARISONS}
     * 
     * @testedMethod {@link Comparators#lowerOrEqual(double, double)}
     * 
     * @description Nominal cases for doubles greater or equal test.
     * 
     * @input Double x. A real value, infinity and a NaN are tested.
     * @input Double y. The following values are tested for a nominal "x" : greater, lower, quasi equal (found equal by
     *        the method using a default epsilon), NaN and infinity.
     * 
     * @output Boolean set to true if the x >= y, false otherwise.
     * 
     * @testPassCriteria The boolean returned is always the one expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGreaterOrEqualDoubleDouble() {
        // nominal cases : two real doubles
        Assert.assertEquals(Comparators.greaterOrEqual(1.987654687654968, 1.98765468766), false);
        Assert.assertEquals(Comparators.greaterOrEqual(1.987654687654968, 1.98765468764), true);
        Assert.assertEquals(Comparators.greaterOrEqual(1.987654687654968, 1.987654687654972), true);
        // tests with an "infinity" value
        Assert.assertEquals(Comparators.greaterOrEqual(Double.NEGATIVE_INFINITY, 1.987654687654972), false);
        Assert.assertEquals(Comparators.greaterOrEqual(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), true);
        Assert.assertEquals(Comparators.greaterOrEqual(1.987654687654972, Double.NEGATIVE_INFINITY), true);
        Assert.assertEquals(Comparators.greaterOrEqual(1.987654687654972, Double.POSITIVE_INFINITY), false);
        Assert.assertEquals(Comparators.greaterOrEqual(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), true);
        Assert.assertEquals(Comparators.greaterOrEqual(Double.POSITIVE_INFINITY, 1.987654687654972), true);
        // tests with a "NaN" value
        Assert.assertEquals(Comparators.greaterOrEqual(Double.NaN, 1.987654687654972), false);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DOUBLES_COMPARISONS}
     * 
     * @testedMethod {@link Comparators#lowerOrEqual(double, double)}
     * 
     * @description Nominal cases for doubles lower strict test.
     * 
     * @input Double x. A real value, infinity and a NaN are tested.
     * @input Double y. The following values are tested for a nominal "x" : greater, lower, quasi equal (found equal by
     *        the method using a default epsilon), NaN and infinite.
     * 
     * @output Boolean set to true if the x < y, false otherwise.
     * 
     * @testPassCriteria The boolean returned is always the one expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testLowerStrictDoubleDouble() {
        // nominal cases : two real doubles
        Assert.assertEquals(Comparators.lowerStrict(1.987654687654968, 1.98765468766), true);
        Assert.assertEquals(Comparators.lowerStrict(1.987654687654968, 1.98765468764), false);
        Assert.assertEquals(Comparators.lowerStrict(1.987654687654968, 1.987654687654972), false);
        // tests with an "infinity" value
        Assert.assertEquals(Comparators.lowerStrict(Double.NEGATIVE_INFINITY, 1.987654687654972), true);
        Assert.assertEquals(Comparators.lowerStrict(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), false);
        Assert.assertEquals(Comparators.lowerStrict(1.987654687654972, Double.NEGATIVE_INFINITY), false);
        Assert.assertEquals(Comparators.lowerStrict(1.987654687654972, Double.POSITIVE_INFINITY), true);
        Assert.assertEquals(Comparators.lowerStrict(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), false);
        Assert.assertEquals(Comparators.lowerStrict(Double.POSITIVE_INFINITY, 1.987654687654972), false);
        // tests with a "NaN" value
        Assert.assertEquals(Comparators.lowerStrict(Double.NaN, 1.987654687654972), false);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DOUBLES_COMPARISONS}
     * 
     * @testedMethod {@link Comparators#lowerOrEqual(double, double)}
     * 
     * @description Nominal cases for doubles greater strict test.
     * 
     * @input Double x. A real value, infinity and a NaN are tested.
     * @input Double y. The following values are tested for a nominal "x" : greater, lower, quasi equal (found equal by
     *        the method using a default epsilon), NaN and infinite.
     * 
     * @output Boolean set to true if the x > y, false otherwise.
     * 
     * @testPassCriteria The boolean returned is always the one expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGreaterStrictDoubleDouble() {
        // nominal cases : two real doubles
        Assert.assertEquals(Comparators.greaterStrict(1.987654687654968, 1.98765468766), false);
        Assert.assertEquals(Comparators.greaterStrict(1.987654687654968, 1.98765468764), true);
        Assert.assertEquals(Comparators.greaterStrict(1.987654687654968, 1.987654687654972), false);
        // tests with an "infinity" value
        Assert.assertEquals(Comparators.greaterStrict(Double.NEGATIVE_INFINITY, 1.987654687654972), false);
        Assert.assertEquals(Comparators.greaterStrict(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), false);
        Assert.assertEquals(Comparators.greaterStrict(1.987654687654972, Double.NEGATIVE_INFINITY), true);
        Assert.assertEquals(Comparators.greaterStrict(1.987654687654972, Double.POSITIVE_INFINITY), false);
        Assert.assertEquals(Comparators.greaterStrict(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), false);
        Assert.assertEquals(Comparators.greaterStrict(Double.POSITIVE_INFINITY, 1.987654687654972), true);
        // tests with a "NaN" value
        Assert.assertEquals(Comparators.greaterStrict(Double.NaN, 1.987654687654972), false);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DOUBLES_COMPARISONS}
     * 
     * @testedMethod {@link Comparators#equals(double, double)}
     * 
     * @description Nominal cases for doubles equality test.
     * 
     * @input Double x. A real value, quasi 0.0, infinity and a NaN are tested.
     * @input Double y. Four values are tested for a nominal "x" : greater, lower, 0.0 and quasi equal (found equal by
     *        the method using a default epsilon).
     * @input Double eps. Absolute tolerance.
     * 
     * @output Boolean set to true if the doubles are equal, false otherwise.
     * 
     * @testPassCriteria The boolean returned is always the one expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testEqualDoubleDoubleDouble() {
        final double eps = 1.0e-14;
        // nominal cases : two real doubles
        Assert.assertEquals(Comparators.equals(1.987654687654968, 1.98765468766, eps), false);
        Assert.assertEquals(Comparators.equals(1.987654687654968, 1.98765468764, eps), false);
        Assert.assertEquals(Comparators.equals(1.987654687654968, 0.0, eps), false);
        Assert.assertEquals(Comparators.equals(1.0e-14, 0.0, eps), false);
        Assert.assertEquals(Comparators.equals(1.00000000000000001e-300, 1.0e-300, eps), true);
        Assert.assertEquals(Comparators.equals(1.987654687654968, 1.987654687654972, eps), true);
        // tests with an "infinity" or "NaN" value
        Assert.assertEquals(Comparators.equals(Double.NEGATIVE_INFINITY, 1.987654687654972, eps), false);
        Assert.assertEquals(Comparators.equals(Double.NaN, 1.987654687654972, eps), false);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DOUBLES_COMPARISONS}
     * 
     * @testedMethod {@link Comparators#lowerOrEqual(double, double)}
     * 
     * @description Nominal cases for doubles lower or equal test.
     * 
     * @input Double x. A real value, infinity and a NaN are tested
     * @input Double y. The following values are tested for a nominal "x" : greater, lower, quasi equal (found equal by
     *        the method using a default epsilon), NaN and infinity
     * @input Double eps. Absolute tolerance.
     * 
     * @output Boolean set to true if the x <= y, false otherwise.
     * 
     * @testPassCriteria The boolean returned is always the one expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testLowerOrEqualDoubleDoubleDouble() {
        final double eps = 1.0e-14;
        // nominal cases : two real doubles
        Assert.assertEquals(Comparators.lowerOrEqual(1.987654687654968, 1.98765468766, eps), true);
        Assert.assertEquals(Comparators.lowerOrEqual(1.987654687654968, 1.98765468764, eps), false);
        Assert.assertEquals(Comparators.lowerOrEqual(1.987654687654968, 1.987654687654972, eps), true);
        // tests with an "infinity" value
        Assert.assertEquals(Comparators.lowerOrEqual(Double.NEGATIVE_INFINITY, 1.987654687654972, eps), true);
        Assert.assertEquals(Comparators.lowerOrEqual(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, eps), true);
        Assert.assertEquals(Comparators.lowerOrEqual(1.987654687654972, Double.NEGATIVE_INFINITY, eps), false);
        Assert.assertEquals(Comparators.lowerOrEqual(1.987654687654972, Double.POSITIVE_INFINITY, eps), true);
        Assert.assertEquals(Comparators.lowerOrEqual(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, eps), true);
        Assert.assertEquals(Comparators.lowerOrEqual(Double.POSITIVE_INFINITY, 1.987654687654972, eps), false);
        // tests with a "NaN" value
        Assert.assertEquals(Comparators.lowerOrEqual(Double.NaN, 1.987654687654972, eps), false);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DOUBLES_COMPARISONS}
     * 
     * @testedMethod {@link Comparators#lowerOrEqual(double, double)}
     * 
     * @description Nominal cases for doubles greater or equal test.
     * 
     * @input Double x. A real value, infinity and a NaN are tested.
     * @input Double y. The following values are tested for a nominal "x" : greater, lower, quasi equal (found equal by
     *        the method using a default epsilon), NaN and infinity.
     * @input Double eps. Absolute tolerance.
     * 
     * @output Boolean set to true if the x >= y, false otherwise.
     * 
     * @testPassCriteria The boolean returned is always the one expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGreaterOrEqualDoubleDoubleDouble() {
        final double eps = 1.0e-14;
        // nominal cases : two real doubles
        Assert.assertEquals(Comparators.greaterOrEqual(1.987654687654968, 1.98765468766, eps), false);
        Assert.assertEquals(Comparators.greaterOrEqual(1.987654687654968, 1.98765468764, eps), true);
        Assert.assertEquals(Comparators.greaterOrEqual(1.987654687654968, 1.987654687654972, eps), true);
        // tests with an "infinity" value
        Assert.assertEquals(Comparators.greaterOrEqual(Double.NEGATIVE_INFINITY, 1.987654687654972, eps), false);
        Assert.assertEquals(Comparators.greaterOrEqual(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, eps), true);
        Assert.assertEquals(Comparators.greaterOrEqual(1.987654687654972, Double.NEGATIVE_INFINITY, eps), true);
        Assert.assertEquals(Comparators.greaterOrEqual(1.987654687654972, Double.POSITIVE_INFINITY, eps), false);
        Assert.assertEquals(Comparators.greaterOrEqual(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, eps), true);
        Assert.assertEquals(Comparators.greaterOrEqual(Double.POSITIVE_INFINITY, 1.987654687654972, eps), true);
        // tests with a "NaN" value
        Assert.assertEquals(Comparators.greaterOrEqual(Double.NaN, 1.987654687654972, eps), false);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DOUBLES_COMPARISONS}
     * 
     * @testedMethod {@link Comparators#lowerOrEqual(double, double)}
     * 
     * @description Nominal cases for doubles lower strict test.
     * 
     * @input Double x. A real value, infinity and a NaN are tested.
     * @input Double y. The following values are tested for a nominal "x" : greater, lower, quasi equal (found equal by
     *        the method using a default epsilon), NaN and infinite.
     * @input Double eps. Absolute tolerance.
     * 
     * @output Boolean set to true if the x < y, false otherwise.
     * 
     * @testPassCriteria The boolean returned is always the one expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testLowerStrictDoubleDoubleDouble() {
        final double eps = 1.0e-14;
        // nominal cases : two real doubles
        Assert.assertEquals(Comparators.lowerStrict(1.987654687654968, 1.98765468766, eps), true);
        Assert.assertEquals(Comparators.lowerStrict(1.987654687654968, 1.98765468764, eps), false);
        Assert.assertEquals(Comparators.lowerStrict(1.987654687654968, 1.987654687654972, eps), false);
        // tests with an "infinity" value
        Assert.assertEquals(Comparators.lowerStrict(Double.NEGATIVE_INFINITY, 1.987654687654972, eps), true);
        Assert.assertEquals(Comparators.lowerStrict(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, eps), false);
        Assert.assertEquals(Comparators.lowerStrict(1.987654687654972, Double.NEGATIVE_INFINITY, eps), false);
        Assert.assertEquals(Comparators.lowerStrict(1.987654687654972, Double.POSITIVE_INFINITY, eps), true);
        Assert.assertEquals(Comparators.lowerStrict(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, eps), false);
        Assert.assertEquals(Comparators.lowerStrict(Double.POSITIVE_INFINITY, 1.987654687654972, eps), false);
        // tests with a "NaN" value
        Assert.assertEquals(Comparators.lowerStrict(Double.NaN, 1.987654687654972, eps), false);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DOUBLES_COMPARISONS}
     * 
     * @testedMethod {@link Comparators#lowerOrEqual(double, double)}
     * 
     * @description Nominal cases for doubles greater strict test.
     * 
     * @input Double x. A real value, infinity and a NaN are tested.
     * @input Double y. The following values are tested for a nominal "x" : greater, lower, quasi equal (found equal by
     *        the method using a default epsilon), NaN and infinite.
     * @input Double eps. Absolute tolerance.
     * 
     * @output Boolean set to true if the x > y, false otherwise.
     * 
     * @testPassCriteria The boolean returned is always the one expected.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGreaterStrictDoubleDoubleDouble() {
        final double eps = 1.0e-14;
        // nominal cases : two real doubles
        Assert.assertEquals(Comparators.greaterStrict(1.987654687654968, 1.98765468766, eps), false);
        Assert.assertEquals(Comparators.greaterStrict(1.987654687654968, 1.98765468764, eps), true);
        Assert.assertEquals(Comparators.greaterStrict(1.987654687654968, 1.987654687654972, eps), false);
        Assert.assertEquals(Comparators.greaterStrict(1.987654687654972, 1.987654687654968, eps), false);

        // tests with an "infinity" value
        Assert.assertEquals(Comparators.greaterStrict(Double.NEGATIVE_INFINITY, 1.987654687654972, eps), false);
        Assert.assertEquals(Comparators.greaterStrict(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, eps), false);
        Assert.assertEquals(Comparators.greaterStrict(1.987654687654972, Double.NEGATIVE_INFINITY, eps), true);
        Assert.assertEquals(Comparators.greaterStrict(1.987654687654972, Double.POSITIVE_INFINITY, eps), false);
        Assert.assertEquals(Comparators.greaterStrict(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, eps), false);
        Assert.assertEquals(Comparators.greaterStrict(Double.POSITIVE_INFINITY, 1.987654687654972, eps), true);
        // tests with a "NaN" value
        Assert.assertEquals(Comparators.greaterStrict(Double.NaN, 1.987654687654972, eps), false);
    }
}
