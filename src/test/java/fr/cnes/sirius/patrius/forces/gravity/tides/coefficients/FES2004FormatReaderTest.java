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
 * @history Created 12/07/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides.coefficients;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link OceanTidesCoefficientsFactory}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: FES2004FormatReaderTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class FES2004FormatReaderTest {

    /**
     * Features description.
     */
    public enum features {
        /**
         * @featureTitle ocean tides file reader
         * 
         * @featureDescription here we test the file loaders for Ocean Tides Coefficient files
         * 
         * @coveredRequirements DV-MOD_210
         */
        OCEAN_TIDES_READERS
    }

    /**
     * Sample data, for testing purposes
     */
    private final double[][] sampleData = new double[][] {
        { 56.554, 46.0, 44.0, 5.7E-5, -2.1E-5, 7.4E-5, -1.1E-5, 1.0E-4, 109.795, 1.0E-4, 98.431 },
        { 56.554, 47.0, 44.0, 1.0E-5, -3.0E-5, 6.0E-6, 3.7E-5, 0.0, 161.074, 0.0, 9.583 },
        { 57.555, 50.0, 49.0, -1.99E-4, -1.49E-4, -2.54E-4, 9.7E-5, 2.0E-4, 233.13, 3.0E-4, 291.022 },
        { 57.555, 50.0, 50.0, -2.29E-4, -8.58E-4, -3.68E-4, 8.13E-4, 9.0E-4, 194.927, 9.0E-4, 335.614 },
        { 65.455, 45.0, 24.0, 4.38E-4, -2.89E-4, 3.82E-4, 0.001648, 5.0E-4, 123.388, 0.0017, 13.042 },
        { 65.455, 46.0, 24.0, -0.001244, -7.92E-4, -0.001291, 6.76E-4, 0.0015, 237.512, 0.0015, 297.64 }, };

    /**
     * The coefficients provider
     */
    private OceanTidesCoefficientsProvider fes;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#OCEAN_TIDES_READERS}
     * 
     * @testedMethod {@link OceanTidesCoefficientsReader#getCpmSpm(double, int, int)}.
     * 
     * @description make sure the correct coefficients are return by this method
     * 
     * @input Doodson number, ordre and degree
     * 
     * @output C± and S± coefficients
     * 
     * @testPassCriteria the different corrections must be the expected ones (references provided from the same file).
     *                   Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetCpmSpm() {

        for (final double[] data : this.sampleData) {
            Assert.assertArrayEquals(new double[] { data[4], data[6], data[3], data[5] },
                this.fes.getCpmSpm(data[0], (int) data[1], (int) data[2]), 1e-14);
        }

        try {
            this.fes.getCpmEpm(20, 1, 1);
            Assert.fail();
        } catch (final NullPointerException e) {
            // expected!
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#OCEAN_TIDES_READERS}
     * 
     * @testedMethod {@link OceanTidesCoefficientsReader#getCpmEpm(double, int, int)}.
     * 
     * @description make sure the correct coefficients are return by this method
     * 
     * @input Doodson number, ordre and degree
     * 
     * @output C± and ε± coefficients
     * 
     * @testPassCriteria the different corrections must be the expected ones (references provided from the same file).
     *                   Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetCpmEpm() {
        for (final double[] data : this.sampleData) {
            Assert.assertArrayEquals(new double[] { data[7], data[9], data[8], data[10] },
                this.fes.getCpmEpm(data[0], (int) data[1], (int) data[2]), 1e-14);

        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#OCEAN_TIDES_READERS}
     * 
     * @testedMethod {@link OceanTidesCoefficientsReader#getDoodsonNumbers()}
     * 
     * @description make sure the reader has all the Doodson numbers in the file
     * 
     * @input none
     * 
     * @output Doodson numbers in file
     * 
     * @testPassCriteria the numbers must be the expected ones (references provided from the same file).
     *                   Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void getDoodsonNumbersTest() {
        Assert.assertArrayEquals(new double[] { 55.565, 55.575, 56.554, 57.555, 65.455, 75.555, 85.455, 93.555,
            135.655, 145.555, 163.555, 165.555, 235.755, 245.655, 255.555, 273.555, 275.555 },
            this.fes.getDoodsonNumbers(), 1e-14);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#OCEAN_TIDES_READERS}
     * 
     * @testedMethod {@link OceanTidesCoefficientsReader#getMaxDegree(double)}
     * 
     * @description make sure the reader has all the orders and degrees for a given Doodson number
     * 
     * @input Doodson number
     * 
     * @output Maximum degree
     * 
     * @testPassCriteria the max degree must be the expected ones (references provided from the same file).
     *                   Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void getDegreeTest() {

        int actual = this.fes.getMaxDegree(55.565, 0);
        int exp = 2;
        Assert.assertEquals(exp, actual);

        actual = this.fes.getMaxDegree(56.554, 0);
        exp = 50;
        Assert.assertEquals(exp, actual);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#OCEAN_TIDES_READERS}
     * 
     * @testedMethod {@link OceanTidesCoefficientsReader#getMaxOrder(double, int)}
     * 
     * @description make sure the reader has all the ordersfor a given Doodson number
     * 
     * @input Doodson number and degree
     * 
     * @output Maximum order
     * 
     * @testPassCriteria the max ordermust be the expected ones (references provided from the same file).
     *                   Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void getOrderTest() {

        int actual = this.fes.getMaxOrder(55.565);
        int exp = 0;
        Assert.assertEquals(exp, actual);

        actual = this.fes.getMaxOrder(56.554);
        exp = 50;
        Assert.assertEquals(exp, actual);

    }

    /**
     * set up
     * 
     * @throws PatriusException
     *         if fails
     * @throws ParseException
     *         if parse fails
     * @throws IOException
     *         if file error
     */
    @Before
    public void setUp() throws IOException, ParseException, PatriusException {

        // data location
        final String s = File.separator;
        final String home = "src" + s + "test" + s + "resources";
        String root = home + s + "bad-end-data";
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, root);

        // clear loaded data
        DataProvidersManager.getInstance().clearLoadedDataNames();
        DataProvidersManager.getInstance().clearProviders();
        OceanTidesCoefficientsFactory.clearOceanTidesCoefficientsReaders();

        // faulty file (for coverage)
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader(
            OceanTidesCoefficientsFactory.FES2004_FILENAME + "_faulty"));
        try {
            this.fes = OceanTidesCoefficientsFactory.getCoefficientsProvider();
            Assert.fail();
        } catch (final PatriusException e) {
            // expected!
        }

        // faulty file (for coverage)
        DataProvidersManager.getInstance().clearLoadedDataNames();
        DataProvidersManager.getInstance().clearProviders();
        OceanTidesCoefficientsFactory.clearOceanTidesCoefficientsReaders();
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader(
            OceanTidesCoefficientsFactory.FES2004_FILENAME + "_faulty2"));
        try {
            this.fes = OceanTidesCoefficientsFactory.getCoefficientsProvider();
            Assert.fail();
        } catch (final PatriusException e) {
            // expected!
        }

        // data location
        root = home + s + "oceanTides";
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, root);

        // give a specific loader to the factory
        DataProvidersManager.getInstance().clearLoadedDataNames();
        DataProvidersManager.getInstance().clearProviders();
        OceanTidesCoefficientsFactory.clearOceanTidesCoefficientsReaders();
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader(
            OceanTidesCoefficientsFactory.FES2004_FILENAME));

        // get provider
        this.fes = OceanTidesCoefficientsFactory.getCoefficientsProvider();
    }

    /**
     * Check arrays are equal
     * 
     * @param exp
     *        expected
     * @param act
     *        actual
     */
    public static void assertMultArraysEquals(final int[][] exp, final int[][] act) {

        Assert.assertEquals(exp.length, act.length);

        for (int k = 0; k < exp.length; k++) {
            Assert.assertArrayEquals(exp[k], act[k]);
        }

    }
}
