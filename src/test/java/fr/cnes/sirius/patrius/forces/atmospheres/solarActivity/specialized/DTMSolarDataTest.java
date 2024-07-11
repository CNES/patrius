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
 * @history Created 20/08/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:131:28/10/2013:Fixed bug in ap ref dates
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ACSOLFormatReader;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataFactory;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityToolbox;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCScale;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link DTMSolarData}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: DTM2000SolarDataTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class DTMSolarDataTest {

    /** threshold */
    private static final double EPS = Precision.DOUBLE_COMPARISON_EPSILON;
    /**
     * container
     */
    private DTMSolarData dtmData;
    /**
     * utc scale
     */
    private UTCScale utc;
    /**
     * container
     */
    private SolarActivityDataProvider data;

    /**
     * Features description.
     */
    public enum features {
        /**
         * @featureTitle DTM2000 solar activity
         * 
         * @featureDescription here we test the methods of the DTM2000solar activity toolbox
         * 
         * @coveredRequirements DV-MOD_261
         */
        DTM2000_SOLAR_ACTIVITY
    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#DTM2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link DTMSolarData#getMeanFlux(AbsoluteDate)}.
     * 
     * @description make sure the correct flux value are return by these method
     * 
     * @input date
     * 
     * @output mean flux in sfu
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testMeanFlux() throws PatriusException {

        final DateTimeComponents currentDateTime = new DateTimeComponents(new DateTimeComponents(
            DateComponents.FIFTIES_EPOCH, TimeComponents.H00), 5912 * Constants.JULIAN_DAY + 61200);
        final AbsoluteDate date = new AbsoluteDate(currentDateTime, this.utc);

        Assert.assertEquals(89.5753086419753000, this.dtmData.getMeanFlux(date), EPS);
    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#DTM2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link DTMSolarData#getInstantFlux(AbsoluteDate)}.
     * 
     * @description make sure the correct flux value are return by these method
     * 
     * @input date
     * 
     * @output mean flux in sfu
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testInstantFlux() throws PatriusException {

        final DateTimeComponents currentDateTime = new DateTimeComponents(new DateTimeComponents(
            DateComponents.FIFTIES_EPOCH, TimeComponents.H00), 17237 * Constants.JULIAN_DAY + 72000);
        final AbsoluteDate date = new AbsoluteDate(currentDateTime, this.utc);

        Assert.assertEquals(74.3, this.dtmData.getInstantFlux(date), EPS);
    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#DTM2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link DTMSolarData#getThreeHourlyKP(AbsoluteDate)}.
     * 
     * @description make sure the correct ap value are return by these method
     * 
     * @input date
     * 
     * @output ap
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void test3hKpAp() throws PatriusException {

        final DateTimeComponents currentDateTime = new DateTimeComponents(new DateTimeComponents(
            DateComponents.FIFTIES_EPOCH, TimeComponents.H00), 21021 * Constants.JULIAN_DAY + 30000);
        final AbsoluteDate date = new AbsoluteDate(currentDateTime, this.utc);

        Assert.assertEquals(SolarActivityToolbox.apToKp(3.), this.dtmData.getThreeHourlyKP(date), EPS);
    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#DTM2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link DTMSolarData#get24HoursKp(AbsoluteDate)}.
     * 
     * @description make sure the correct ap value are return by these method
     * 
     * @input date
     * 
     * @output ap
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void test24hKp() throws PatriusException {

        final DateTimeComponents currentDateTime = new DateTimeComponents(new DateTimeComponents(
            DateComponents.FIFTIES_EPOCH, TimeComponents.H00), 17087 * Constants.JULIAN_DAY + 72000);
        final AbsoluteDate date = new AbsoluteDate(currentDateTime, this.utc);

        Assert.assertEquals(SolarActivityToolbox.apToKp(12), this.dtmData.get24HoursKp(date), EPS);
    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#DTM2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link DTMSolarData#getMinDate()}.
     * @testedMethod {@link DTMSolarData#getMaxDate()}.
     * 
     * @description make sure the min date is returned by these method
     * 
     * @input
     * 
     * @output mind ate
     * 
     * @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testDates() throws PatriusException {

        Assert.assertTrue(this.data.getMinDate().equals(this.dtmData.getMinDate()));
        Assert.assertTrue(this.data.getMaxDate().equals(this.dtmData.getMaxDate()));

    }

    /**
     * set up test case
     * 
     * @throws PatriusException
     *         if no data loaded
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("atmosphere");
        this.utc = TimeScalesFactory.getUTC();
        SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader(
            SolarActivityDataFactory.ACSOL_FILENAME));
        this.data = SolarActivityDataFactory.getSolarActivityDataProvider();

        this.dtmData = new DTMSolarData(this.data);
    }
}
