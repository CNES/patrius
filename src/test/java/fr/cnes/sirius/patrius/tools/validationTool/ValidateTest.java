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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2095:15/05/2019:[PATRIUS] preparation au deploiement sur les depots centraux maven
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.tools.validationTool;

import java.io.IOException;
import java.net.URISyntaxException;

import junit.framework.Assert;

import org.junit.Test;

/**
 * <p>
 * Test class for the Validate class.
 * </p>
 * 
 * @see Validate
 * 
 * @author Philippe Pavero
 * 
 * @version $Id: ValidateTest.java 17915 2017-09-11 12:35:44Z bignon $
 * 
 * @since 1.0
 * 
 */
public class ValidateTest {
    /** Features description. */
    enum features {
        /**
         * @featureTitle basic coverage
         * 
         * @featureDescription ensures that everything in the Validate class is unit tested
         * 
         * @coveredRequirements NA
         */
        BASIC_COVERAGE
    }

    /**
     * @throws IOException
     * @throws URISyntaxException
     * @testType UT
     * 
     * @testedFeature {@link features#BASIC_COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.tools.validationTool.Validate#assertEquals(double, double, double, double, double, java.lang.String)}
     * @testedMethod {@link fr.cnes.sirius.patrius.tools.validationTool.Validate#produceLog()}
     * 
     * @description this test performs 2 asserts and then prints the results in the console.
     * 
     * @input (actual, NonRegExpected, NonRegDelta, externalRefExpected, externalRefDelta) : (0.0, 1.0e-19, 1.0e-14),
     *        (2.1, 2.005, 1.0e-2)
     * 
     * 
     * @output the log in the console
     * 
     * @testPassCriteria the test is ok if no exception is raised
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testAssertEqualsDouble() throws IOException, URISyntaxException {
        /*
         * non regression case
         */
        System.out.println("testAssertEqualsDouble");

        // this should be done in the setup method of a test
        final Validate validate = new Validate(this.getClass());

        // this should be called in test methods
        validate.assertEquals(1.0e-19, 1.0e-16, 1.0e-15, 0.0, 1.0e-12, "non regression case");
        validate.assertEquals(2.01, 2.005, 1.0e-2, 2.007, 1.0e-2, "blabla test");

        /*
         * fail cases
         */
        boolean doesFail = false;
        try {
            validate.assertEquals(1.0e-19, 1.0e-16, 1.0e-20, 0.0, 1.0e-12, "non regression case");
        } catch (final AssertionError e) {
            // test ok
            doesFail = true;
        }
        Assert.assertTrue(doesFail);

        doesFail = false;
        try {
            validate.assertEquals(1.0e-19, 1.0e-16, 1.0e-15, 0.0, 1.0e-20, "non regression case");
        } catch (final AssertionError e) {
            // test ok
            doesFail = true;
        }
        Assert.assertTrue(doesFail);

        // this should be done in the tearDown method of a test
        validate.produceLog();
    }

    /**
     * @throws IOException
     * @throws URISyntaxException
     * @testType UT
     * 
     * @testedFeature {@link features#BASIC_COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.tools.validationTool.Validate#assertEquals(double, double, double, double, double, java.lang.String)}
     * @testedMethod {@link fr.cnes.sirius.patrius.tools.validationTool.Validate#produceLog()}
     * 
     * @description this test performs a array asserts and then prints the results in the console.
     * 
     * @input array
     * 
     * @output the log in the console
     * 
     * @testPassCriteria the test is ok if no exception is raised
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testAssertEqualsArrayDouble() throws IOException, URISyntaxException {
        /*
         * non regression case
         */
        System.out.println("testAssertEqualsArrayDouble");

        // this should be done in the setup method of a test
        final Validate validate = new Validate(this.getClass());

        // initialisation
        final double[] d1 = new double[6];
        final double[] d2 = new double[6];
        int i;

        for (i = 0; i < d1.length; i++) {
            d1[i] = i;
        }

        for (i = 0; i < d2.length; i++) {
            d2[i] = i + 0.001;
        }

        validate.assertEqualsArray(d1, d2, 1.0e-2, d1, 1.0e-1, "array test");
        validate.produceLog("arraytestfile");
    }

    /**
     * @throws IOException ex
     * @testType UT
     * 
     * @testedFeature {@link features#BASIC_COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.tools.validationTool.Validate#assertEqualsWithRelativeTolerance(double, double, double, double, double, String)}
     * 
     * @description this test performs 2 asserts.
     * 
     * @input (actual, NonRegExpected, NonRegDelta, externalRefExpected, externalRefDelta) : (2e-10, 2.1e-11, 1.0e-5,
     *        2.000001e-10, 1.0e-4)
     * 
     * 
     * @output the log in the console
     * 
     * @testPassCriteria the test is ok if no exception is raised
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testAssertEqualsDoubleRelative() throws IOException {
        // this should be done in the setup method of a test
        final Validate validate = new Validate(this.getClass());

        // this should be called in test methods
        validate.assertEqualsWithRelativeTolerance(2.e-10, 2.0000001e-10, 1.0e-6, 2.001e-10, 1.0e-2, "relative test");

        /*
         * fail cases
         */
        boolean doesFail = false;
        try {
            validate.assertEqualsWithRelativeTolerance(2.e-10, 2.0000001e-10, 1.0e-11, 2.001e-10, 1.0e-2, "fail test");
        } catch (final AssertionError e) {
            // test ok
            doesFail = true;
        }
        Assert.assertTrue(doesFail);

        doesFail = false;
        try {
            validate.assertEqualsWithRelativeTolerance(2.e-10, 2.0000001e-10, 1.0e-6, 2.001e-10, 1.0e-20, "fail test");
        } catch (final AssertionError e) {
            // test ok
            doesFail = true;
        }
        Assert.assertTrue(doesFail);
    }

    /**
     * @throws IOException
     * @throws URISyntaxException
     * @testType UT
     * 
     * @testedFeature {@link features#BASIC_COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.tools.validationTool.Validate#assertEquals(double, double, double, double, double, java.lang.String)}
     * @testedMethod {@link fr.cnes.sirius.patrius.tools.validationTool.Validate#produceLog()}
     * 
     * @description this test performs a array asserts and then prints the results in the console.
     * 
     * @input array
     * 
     * @output the log in the console
     * 
     * @testPassCriteria the test is ok if no exception is raised
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testAssertEqualsArrayDoubleRelative() throws IOException, URISyntaxException {
        /*
         * non regression case
         */
        System.out.println("testAssertEqualsArrayDoubleRelative");

        // this should be done in the setup method of a test
        final Validate validate = new Validate(this.getClass());

        // initialisation
        final double[] d1 = new double[6];
        final double[] d2 = new double[6];
        int i;

        for (i = 0; i < d1.length; i++) {
            d1[i] = i;
        }

        for (i = 0; i < d2.length; i++) {
            d2[i] = i + 0.00001;
        }

        validate.assertEqualsArrayWithRelativeTolerance(d1, d2, 2, d2, 2, "array test");
        validate.produceLog("arraytestfile");
    }
}
