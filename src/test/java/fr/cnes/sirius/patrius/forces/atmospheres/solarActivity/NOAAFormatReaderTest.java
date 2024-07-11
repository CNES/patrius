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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:03/11/2014: coverage
 * VERSION::FA:384:31/03/2015:Optimization of solar activity data provider
 * VERSION::FA:569:02/03/2016:Correction in case of UTC shift
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
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
 * @version $Id: NOAAFormatReaderTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class NOAAFormatReaderTest {

    /**
     * Features description.
     */
    public enum features {
        /**
         * @featureTitle NOAA solar activity file reader
         * 
         * @featureDescription here we test the file loaders for NOAA Solar activity format
         * 
         * @coveredRequirements DV-MOD_261
         */
        SOLAR_ACTIVITY_READERS
    }

    /**
     * The coefficients provider
     */
    private SolarActivityDataProvider sol;

    /** utc scale */
    private UTCScale utc;

    /**
     * @throws PatriusException
     *         if no solar activity at date
     * @testType UT
     * 
     * @testedFeature {@link features#SOLAR_ACTIVITY_READERS}
     * 
     * @testedMethod {@link NOAAFormatReader#getAp(AbsoluteDate)}.
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

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1999, 333), TimeComponents.H00, this.utc)
            .shiftedBy(10);
        Assert.assertEquals(9, this.sol.getAp(date), 1e-14);
        Assert.assertEquals(5, this.sol.getAp(date.shiftedBy(3 * 3600)), 1e-14);
        Assert.assertEquals(3, this.sol.getAp(date.shiftedBy(6 * 3600)), 1e-14);
        Assert.assertEquals(6, this.sol.getAp(date.shiftedBy(9 * 3600)), 1e-14);
        Assert.assertEquals(4, this.sol.getAp(date.shiftedBy(12 * 3600)), 1e-14);
        Assert.assertEquals(0, this.sol.getAp(date.shiftedBy(15 * 3600)), 1e-14);
        Assert.assertEquals(2, this.sol.getAp(date.shiftedBy(18 * 3600)), 1e-14);
        Assert.assertEquals(5, this.sol.getAp(date.shiftedBy(21 * 3600)), 1e-14);
    }

    /**
     * @throws PatriusException
     *         if no solar activity at date
     * @testType UT
     * 
     * @testedFeature {@link features#SOLAR_ACTIVITY_READERS}
     * 
     * @testedMethod {@link NOAAFormatReader#getInstantFluxValue(AbsoluteDate)}.
     * 
     * @description make sure the correct coefficients are return by this method
     * 
     * @input date
     * 
     * @output Ap array
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 2.4
     * 
     * @nonRegressionVersion 2.4
     */
    @Test
    public void testGetInstantFlux() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1998, 110), TimeComponents.H00, this.utc);

        Assert.assertTrue(this.sol.getInstantFluxValues(date.shiftedBy(-10), date.shiftedBy(10)).containsValue(98.));
        Assert.assertEquals(1, this.sol.getInstantFluxValues(date.shiftedBy(-10), date.shiftedBy(10)).size());

        // Test the exceptions of the getInstantFluxValues of the SolarActivityDataReader class:
        boolean rez = false;
        try {
            this.sol.checkFluxValidity(date.shiftedBy(-10), date.shiftedBy(1E10));
        } catch (final PatriusExceptionWrapper e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        rez = false;
        try {
            this.sol.checkFluxValidity(date.shiftedBy(-1E10), date.shiftedBy(10));
        } catch (final PatriusExceptionWrapper e) {
            rez = true;
        }
        Assert.assertTrue(rez);
    }

    /**
     * @throws PatriusException
     *         if no solar activity at date
     * @testType UT
     * 
     * @testedFeature {@link features#SOLAR_ACTIVITY_READERS}
     * 
     * @testedMethod {@link NOAAFormatReader#getInstantFluxValues(AbsoluteDate, AbsoluteDate)}.
     * @testedMethod {@link NOAAFormatReader#getInstantFluxValue(AbsoluteDate)}.
     * @testedMethod {@link NOAAFormatReader#getAp(AbsoluteDate)}.
     * @testedMethod {@link NOAAFormatReader#getKp(AbsoluteDate)}.
     * 
     * @description check the cache system
     * 
     * @input date
     * 
     * @output flux, Ap and Kp data
     * 
     * @testPassCriteria the different coefficients must be exactly the expected ones.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testCacheSystem() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1998, 110), TimeComponents.H00, this.utc);

        // Check flux map
        final SortedMap<AbsoluteDate, Double> fluxs1 = this.sol.getInstantFluxValues(
            date.shiftedBy(-Constants.JULIAN_DAY * 5), date.shiftedBy(Constants.JULIAN_DAY * 5));
        final SortedMap<AbsoluteDate, Double> fluxs2 = this.sol.getInstantFluxValues(
            date.shiftedBy(-Constants.JULIAN_DAY * 5 - 3600), date.shiftedBy(Constants.JULIAN_DAY * 5 + 3600));

        final Iterator<Entry<AbsoluteDate, Double>> it1 = fluxs1.entrySet().iterator();
        final Iterator<Entry<AbsoluteDate, Double>> it2 = fluxs2.entrySet().iterator();

        while (it1.hasNext()) {
            final Entry<AbsoluteDate, Double> next1 = it1.next();
            final Entry<AbsoluteDate, Double> next2 = it2.next();

            Assert.assertEquals(next1.getKey().durationFrom(next2.getKey()), 0, 0);
            Assert.assertEquals(next1.getValue(), next2.getValue(), 0);
        }

        // Check flux
        final double flux1 = this.sol.getInstantFluxValue(date.shiftedBy(3600 * 4));
        final double flux2 = this.sol.getInstantFluxValue(date.shiftedBy(3600 * 8));
        Assert.assertEquals(flux1, 97, 0);
        Assert.assertEquals(flux2, 96, 0);

        // Check Ap
        final double ap1 = this.sol.getAp(date.shiftedBy(3600 * 3));
        final double ap2 = this.sol.getAp(date.shiftedBy(3600 * 6));
        Assert.assertEquals(ap1, 9, 0);
        Assert.assertEquals(ap2, 12, 0);

        // Check Kp
        final double kp1 = this.sol.getKp(date.shiftedBy(3600 * 3));
        final double kp2 = this.sol.getKp(date.shiftedBy(3600 * 6));
        Assert.assertEquals(kp1, 2. + 1. / 3., 0);
        Assert.assertEquals(kp2, 2. + 2. / 3., 0);
    }

    /**
     * FA-569
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

        // Initialization
        Utils.setDataRoot("atmosphere");
        final String fichier_AS = System.getProperty(DataProvidersManager.OREKIT_DATA_PATH) + File.separator
            + "NOAA_ap_97-05.dat.txt";

        final NOAAFormatReader solarActivityReader = new NOAAFormatReader(".*");
        solarActivityReader.loadData(new FileInputStream(fichier_AS), fichier_AS);

        final AbsoluteDate date = new AbsoluteDate("1999-01-01T00:00:31.1", TimeScalesFactory.getTAI());

        // Get solar activity
        final double res = solarActivityReader.getAp(date);

        // Check
        Assert.assertEquals(2, res, 0.);
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
            SolarActivityDataFactory.addSolarActivityDataReader(new NOAAFormatReader("NOAA_ap_97-05_faulty.dat.txt"));
            this.sol = SolarActivityDataFactory.getSolarActivityDataProvider();
            Assert.fail();
        } catch (final PatriusException e) {
            // expected!
        }

        CNESUtils.clearNewFactoriesAndCallSetDataRoot("badData");
        try {
            SolarActivityDataFactory.addSolarActivityDataReader(new NOAAFormatReader("NOAA_ap_97-05_empty.dat.txt"));
            this.sol = SolarActivityDataFactory.getSolarActivityDataProvider();
            Assert.fail();
        } catch (final PatriusException e) {
            // expected!
        }

        CNESUtils.clearNewFactoriesAndCallSetDataRoot("atmosphere");
        this.utc = TimeScalesFactory.getUTC();
        // get provider
        SolarActivityDataFactory.addSolarActivityDataReader(new NOAAFormatReader("NOAA_ap_97-05.dat.txt"));
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
}
