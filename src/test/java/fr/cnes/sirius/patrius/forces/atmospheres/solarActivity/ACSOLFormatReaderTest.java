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
 * @history Created 12/07/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:131:28/10/2013:Fixed bug in ap ref dates
 * VERSION::FA:569:02/03/2016:Correction in case of UTC shift
 * VERSION::FA:587:24/01/2017:Generic ACSOL header
 * VERSION::FA:846:20/02/2017: Deleted "static" on cache attributes
 * VERSION::FA:1134:15/11/2017: UTC management robustness
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.SortedMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCScale;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Test class for {@link SolarActivityDataFactory}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: ACSOLFormatReaderTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class ACSOLFormatReaderTest {

    /**
     * Features description.
     */
    public enum features {
        /**
         * @featureTitle NOAA solar activity file reader
         * 
         * @featureDescription here we test the file loaders for ACSOL Solar activity format
         * 
         * @coveredRequirements DV-MOD_261
         */
        SOLAR_ACTIVITY_READERS
    }

    /**
     * Sample data, for testing purposes
     */
    private final double[][] sampleData = new double[][] {
        { 5844, 61200, 82.0, 2, 5, 2, 0, 2, 0, 3, 3 },
        { 5845, 61200, 78.9, 22, 9, 5, 5, 5, 6, 7, 9 },
        { 5846, 61200, 78.5, 15, 5, 2, 2, 3, 6, 6, 6 },
        { 5847, 61200, 80.5, 2, 6, 4, 6, 3, 15, 39, 15 },
        { 5848, 61200, 80.0, 9, 4, 3, 2, 2, 3, 2, 5 },
        { 5849, 61200, 79.7, 4, 0, 0, 0, 2, 2, 2, 6 },
        { 5850, 61200, 80.9, 5, 6, 4, 4, 2, 9, 12, 12 },
        { 5851, 61200, 80.6, 18, 7, 9, 6, 6, 6, 4, 5 },
        { 5852, 61200, 80.1, 15, 0, 2, 0, 3, 9, 7, 27 },
        { 5853, 61200, 79.8, 6, 12, 9, 6, 4, 6, 7, 5 },
        { 5854, 61200, 80.9, 6, 2, 0, 2, 0, 4, 2, 3 } };

    /**
     * The coefficients provider
     */
    private SolarActivityDataProvider sol;

    /**
     * utc scale
     */
    private UTCScale utc;

    /**
     * @throws PatriusException
     *         if no solar activity at date
     * @testType UT
     * 
     * @testedFeature {@link features#SOLAR_ACTIVITY_READERS}
     * 
     * @testedMethod {@link SolarActivityDataReader#getAp(AbsoluteDate)}.
     * 
     * @description make sure the correct coefficients are return by this method
     * 
     * @input date
     * 
     * @output Ap array
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetAp() throws PatriusException {

        AbsoluteDate date;

        for (final double[] data : this.sampleData) {
            final DateTimeComponents currentDateTime = new DateTimeComponents(new DateTimeComponents(
                DateComponents.FIFTIES_EPOCH, TimeComponents.H00), data[0] * Constants.JULIAN_DAY + 20);
            date = new AbsoluteDate(currentDateTime, this.utc);

            Assert.assertEquals(data[3], this.sol.getAp(date), 1e-14);
            Assert.assertEquals(data[4], this.sol.getAp(date.shiftedBy(3 * 3600)), 1e-14);
            Assert.assertEquals(data[5], this.sol.getAp(date.shiftedBy(6 * 3600)), 1e-14);
            Assert.assertEquals(data[6], this.sol.getAp(date.shiftedBy(9 * 3600)), 1e-14);
            Assert.assertEquals(data[7], this.sol.getAp(date.shiftedBy(12 * 3600)), 1e-14);
            Assert.assertEquals(data[8], this.sol.getAp(date.shiftedBy(15 * 3600)), 1e-14);
            Assert.assertEquals(data[9], this.sol.getAp(date.shiftedBy(18 * 3600)), 1e-14);
        }

        try {
            final AbsoluteDate startEnd = new AbsoluteDate(1950, 1, 1, this.utc);
            this.sol.checkApKpValidity(startEnd, startEnd);
            Assert.fail();
        } catch (final PatriusExceptionWrapper e) {
            // expected!
        }

        this.sol.getMaxDate();
    }

    /**
     * @throws PatriusException
     *         if no solar activity at date
     * @testType UT
     * 
     * @testedFeature {@link features#SOLAR_ACTIVITY_READERS}
     * 
     * @testedMethod {@link SolarActivityDataReader#getKp(AbsoluteDate)}.
     * 
     * @description make sure the correct coefficients are return by this method
     * 
     * @input date
     * 
     * @output Kp array
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetKp() throws PatriusException {

        AbsoluteDate date;

        for (final double[] data : this.sampleData) {
            final DateTimeComponents currentDateTime = new DateTimeComponents(new DateTimeComponents(
                DateComponents.FIFTIES_EPOCH, TimeComponents.H00), data[0] * Constants.JULIAN_DAY + 10);
            date = new AbsoluteDate(currentDateTime, this.utc);

            Assert.assertEquals(SolarActivityToolbox.apToKp(data[3]), this.sol.getKp(date), 1e-14);
            Assert.assertEquals(SolarActivityToolbox.apToKp(data[4]), this.sol.getKp(date.shiftedBy(3 * 3600)), 1e-14);
            Assert.assertEquals(SolarActivityToolbox.apToKp(data[5]), this.sol.getKp(date.shiftedBy(6 * 3600)), 1e-14);
            Assert.assertEquals(SolarActivityToolbox.apToKp(data[6]), this.sol.getKp(date.shiftedBy(9 * 3600)), 1e-14);
            Assert.assertEquals(SolarActivityToolbox.apToKp(data[7]), this.sol.getKp(date.shiftedBy(12 * 3600)), 1e-14);
            Assert.assertEquals(SolarActivityToolbox.apToKp(data[8]), this.sol.getKp(date.shiftedBy(15 * 3600)), 1e-14);
            Assert.assertEquals(SolarActivityToolbox.apToKp(data[9]), this.sol.getKp(date.shiftedBy(18 * 3600)), 1e-14);
        }

        try {
            final AbsoluteDate startEnd = new AbsoluteDate(1950, 1, 1, this.utc);
            this.sol.checkApKpValidity(startEnd, startEnd);
            Assert.fail();
        } catch (final PatriusExceptionWrapper e) {
            // expected!
        }
    }

    /**
     * @throws PatriusException
     *         if no solar activity at date
     * @testType UT
     * 
     * @testedFeature {@link features#SOLAR_ACTIVITY_READERS}
     * 
     * @testedMethod {@link SolarActivityDataReader#getInstantFluxValue(AbsoluteDate)}.
     * 
     * @description make sure the correct coefficients are return by this method
     * 
     * @input date
     * 
     * @output Ap array
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetInstantFlux() throws PatriusException {

        AbsoluteDate date;

        for (final double[] data : this.sampleData) {
            final DateTimeComponents currentDateTime = new DateTimeComponents(new DateTimeComponents(
                DateComponents.FIFTIES_EPOCH, TimeComponents.H00), data[0] * Constants.JULIAN_DAY + data[1]);
            date = new AbsoluteDate(currentDateTime, this.utc);
            Assert.assertEquals(data[2], this.sol.getInstantFluxValue(date), 1e-14);
        }

    }

    /**
     * FA-569 FA-1134
     * 
     * @throws PatriusException
     *         if no solar activity at date
     * @throws ParseException
     * @throws IOException
     * @throws FileNotFoundException
     * @testType UT
     * 
     * @testedFeature {@link features#SOLAR_ACTIVITY_READERS}
     * 
     * @testedMethod {@link SolarActivityDataReader#getAp(AbsoluteDate)}.
     * 
     * @description check Ap coefficient is properly retrieved when date match exactly UTC shift
     * 
     * @input date
     * 
     * @output Ap array
     * 
     * @testPassCriteria Ap coefficient must be the expected one
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testUTCShift() throws PatriusException, FileNotFoundException, IOException, ParseException {

        // Initialization - FA 569
        Utils.setDataRoot("solarData");
        final String fichier_AS = System.getProperty(DataProvidersManager.OREKIT_DATA_PATH) + File.separator
            + "ACSOL.act";

        final ACSOLFormatReader solarActivityReader = new ACSOLFormatReader(".*");
        solarActivityReader.loadData(new FileInputStream(fichier_AS), fichier_AS);

        final AbsoluteDate date = new AbsoluteDate("2009-01-01T00:00:33.1", TimeScalesFactory.getTAI());

        // Get solar activity
        final double res = solarActivityReader.getAp(date);

        // Check
        Assert.assertEquals(9, res, 0.);

        // Initialization - FA 1134
        final String fichier_AS_FA1176 = System.getProperty(DataProvidersManager.OREKIT_DATA_PATH) + File.separator
            + "real_activity_ef946";

        final ACSOLFormatReader solarActivityReaderBis = new ACSOLFormatReader(".*");
        solarActivityReaderBis.loadData(new FileInputStream(fichier_AS_FA1176), fichier_AS_FA1176);

        final AbsoluteDate dateTest = new AbsoluteDate("1961-08-02T00:00:01.648", TimeScalesFactory.getTAI());
        // Get solar activity
        final double resTest = solarActivityReader.getAp(dateTest);

        // Check
        Assert.assertEquals(12, resTest, 0.);
    }

    /**
     * FA-587.
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#SOLAR_ACTIVITY_READERS}
     * 
     * @testedMethod {@link ACSOLFormatReader#ACSOLFormatReader()}.
     * 
     * @description check header is properly skipped
     * 
     * @input ACSOL file with long header
     * 
     * @output ACSOLFormatReader object
     * 
     * @testPassCriteria No exception thrown
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testHeader() {

        // Initialization
        Utils.setDataRoot("badData");
        final String fichier_AS = System.getProperty(DataProvidersManager.OREKIT_DATA_PATH) + File.separator
            + "ACSOLLargeHeader.act";

        try {
            final ACSOLFormatReader solarActivityReader = new ACSOLFormatReader(".*");
            solarActivityReader.loadData(new FileInputStream(fichier_AS), fichier_AS);
        } catch (final FileNotFoundException e) {
            Assert.fail();
        } catch (final IOException e) {
            Assert.fail();
        } catch (final ParseException e) {
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.fail();
        }

        // File has been properly read
        Assert.assertTrue(true);
    }

    /**
     * FA-846.
     * 
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @throws FileNotFoundException
     * @testType UT
     * 
     * @testedFeature {@link features#SOLAR_ACTIVITY_READERS}
     * 
     * @testedMethod {@link ACSOLFormatReader#ACSOLFormatReader()}.
     * 
     * @description Using two instances of {@link ACSOLFormatReader} having a different solar activity data file,
     *              check flux, Ap, Kp values obtained are not the same and the one expected : the cache system for
     *              indices
     *              computation must provide for each instance its own cached values.
     * 
     * @input two instances of {@link ACSOLFormatReader}, two solar activity data file being different.
     *        The considered date is 2009-01-01T00:00:33.1, is corresponds to the interval [21 550, 21 551] julian days
     *        in
     *        the data files. The files are different in the following lines :
     * 
     *        1) ACSOL.act
     *        21550 72000 68.9 6 9 12 7 9 6 3 6
     *        21551 72000 69.9 3 3 0 2 3 7 6 12
     * 
     *        2) ACSOL_diff.act
     *        21550 72000 66.9 6 9 12 7 9 6 3 16
     *        21551 72000 60.9 3 3 0 2 3 7 6 22
     * 
     * @output flux, Ap, Kp at given date.
     * 
     * @testPassCriteria Output must be different and the one expected.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testCacheDifferentInput() throws PatriusException, FileNotFoundException, IOException, ParseException {

        // Initialization : two different solar activity data files
        Utils.setDataRoot("solarData");
        final String pathData = System.getProperty(DataProvidersManager.OREKIT_DATA_PATH) + File.separator;
        final String fichier_AS_1 = pathData + "ACSOL.act";
        final String fichier_AS_2 = pathData + "ACSOL_diff.act";

        // Initialize 2 instances
        final ACSOLFormatReader solarActivityReader1 = new ACSOLFormatReader(".*");
        final ACSOLFormatReader solarActivityReader2 = new ACSOLFormatReader(".*");
        solarActivityReader1.loadData(new FileInputStream(fichier_AS_1), fichier_AS_1);
        solarActivityReader2.loadData(new FileInputStream(fichier_AS_2), fichier_AS_2);

        // Check flux : used date matching exactly the flux map dates for the test purpose
        // (expected flux is simply obtained in the file without any computation)
        final AbsoluteDate dateFlux = new AbsoluteDate("2009-01-01T20:00:34.000", TimeScalesFactory.getTAI());
        final double flux = solarActivityReader1.getInstantFluxValue(dateFlux);
        final double fluxDiff = solarActivityReader2.getInstantFluxValue(dateFlux);
        // Perfom comparisons
        Assert.assertEquals(68.9, flux, 0.);
        Assert.assertEquals(66.9, fluxDiff, 0.);

        // Check Ap
        final AbsoluteDate date = new AbsoluteDate("2009-01-01T00:00:33.1", TimeScalesFactory.getTAI());
        final double ap = solarActivityReader1.getAp(date);
        final double apDiff = solarActivityReader2.getAp(date);
        // Perfom comparisons
        Assert.assertEquals(9, ap, 0.);
        Assert.assertEquals(14, apDiff, 0.);

        // Check Kp
        final double kp = solarActivityReader1.getKp(date);
        final double kpDiff = solarActivityReader2.getKp(date);
        // Perfom comparisons
        Assert.assertEquals(SolarActivityToolbox.apToKp(ap), kp, 0.);
        Assert.assertEquals(SolarActivityToolbox.apToKp(apDiff), kpDiff, 0.);

        // Get Ap, Kp between 2 dates as a SortedMap
        final AbsoluteDate date1 = date.shiftedBy(86400.);
        final AbsoluteDate date2 = date1.shiftedBy(86400.);
        final SortedMap<AbsoluteDate, Double[]> apKp = solarActivityReader1.getApKpValues(date1, date2);
        final SortedMap<AbsoluteDate, Double[]> apKpDiff = solarActivityReader2.getApKpValues(date1, date2);

        // Check Ap, Kp values
        assertArraysEquals(new Double[] { 6., SolarActivityToolbox.apToKp(6.) }, apKp.get(date1));
        assertArraysEquals(new Double[] { 12., SolarActivityToolbox.apToKp(12.) }, apKp.get(date2));
        assertArraysEquals(new Double[] { 16., SolarActivityToolbox.apToKp(16.) }, apKpDiff.get(date1));
        assertArraysEquals(new Double[] { 22., SolarActivityToolbox.apToKp(22.) }, apKpDiff.get(date2));

        // Get instant flux between 2 dates
        final AbsoluteDate dateFlux1 = dateFlux.shiftedBy(86400.);
        final AbsoluteDate dateFlux2 = dateFlux1.shiftedBy(86400.);
        final SortedMap<AbsoluteDate, Double> fluxMap = solarActivityReader1.getInstantFluxValues(dateFlux1, dateFlux2);
        final SortedMap<AbsoluteDate, Double> fluxDiffMap = solarActivityReader2.getInstantFluxValues(dateFlux1,
            dateFlux2);

        // Check flux values : the expected flux is the one at dateFlux1 since subMap is
        // [fromKey inclusive, toKey exclusive[
        Assert.assertEquals(69.9, fluxMap.get(dateFlux1), 0.);
        Assert.assertEquals(60.9, fluxDiffMap.get(dateFlux1), 0.);
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

        CNESUtils.clearNewFactoriesAndCallSetDataRoot("badData");
        this.utc = TimeScalesFactory.getUTC();
        try {
            SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader("ACSOL_faulty.act"));
            this.sol = SolarActivityDataFactory.getSolarActivityDataProvider();
            Assert.fail();
        } catch (final PatriusException e) {
            // expected!
        }

        CNESUtils.clearNewFactoriesAndCallSetDataRoot("badData");
        this.utc = TimeScalesFactory.getUTC();
        try {
            SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader("ACSOL_empty.act"));
            this.sol = SolarActivityDataFactory.getSolarActivityDataProvider();
            Assert.fail();
        } catch (final PatriusException e) {
            // expected!
        }

        CNESUtils.clearNewFactoriesAndCallSetDataRoot("atmosphere");
        this.utc = TimeScalesFactory.getUTC();
        // get provider
        SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader("ACSOL.act"));
        this.sol = SolarActivityDataFactory.getSolarActivityDataProvider();

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

    /**
     * Check arrays are equal
     * 
     * @param exp
     *        expected
     * @param act
     *        actual
     */
    public static void assertArraysEquals(final Double[] exp, final Double[] act) {

        Assert.assertEquals(exp.length, act.length);

        for (int k = 0; k < exp.length; k++) {
            Assert.assertEquals(exp[k], act[k]);
        }

    }
}
