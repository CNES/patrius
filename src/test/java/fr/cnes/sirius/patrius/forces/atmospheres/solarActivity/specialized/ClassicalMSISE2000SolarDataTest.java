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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:131:28/10/2013:Renamed class
 * VERSION::DM:131:12/11/2013:Fixed bug in ap ref dates
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ACSOLFormatReader;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataFactory;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataReader;
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
 * Test class for {@link ClassicalMSISE2000SolarData}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: ClassicalMSISE2000SolarDataTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class ClassicalMSISE2000SolarDataTest {

    /** threshold */
    private static final double EPS = Precision.DOUBLE_COMPARISON_EPSILON;
    /**
     * container
     */
    private ClassicalMSISE2000SolarData msis2000Data;
    /**
     * utc scale
     */
    private UTCScale utc;
    /**
     * data container
     */
    private SolarActivityDataReader data;

    /**
     * Features description.
     */
    public enum features {
        /**
         * @featureTitle MSISE2000 solar activity
         * 
         * @featureDescription here we test the methods of the MSISE2000 solar activity toolbox
         * 
         * @coveredRequirements DV-MOD_261
         */
        MSISE2000_SOLAR_ACTIVITY
    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#MSISE2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link ClassicalMSISE2000SolarData#getMeanFlux(AbsoluteDate)}.
     * 
     * @description make sure the correct mean flux value are return by these method
     * 
     * @input date
     * 
     * @output mean flux
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

        Assert.assertEquals(89.5753086419753000, this.msis2000Data.getMeanFlux(date), EPS);
    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#MSISE2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link ClassicalMSISE2000SolarData#getInstantFlux(AbsoluteDate)}.
     * 
     * @description make sure the correct instant flux value are return by these method
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
    public void testInstantFlux() throws PatriusException {

        final DateTimeComponents currentDateTime = new DateTimeComponents(new DateTimeComponents(
            DateComponents.FIFTIES_EPOCH, TimeComponents.H00), 17237 * Constants.JULIAN_DAY + 72000);
        final AbsoluteDate date = new AbsoluteDate(currentDateTime, this.utc);

        Assert.assertEquals(74.3, this.msis2000Data.getInstantFlux(date), EPS);
    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#MSISE2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link ClassicalMSISE2000SolarData#getApValues(AbsoluteDate)}.
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
    public void testAp() throws PatriusException {

        final DateTimeComponents currentDateTime = new DateTimeComponents(new DateTimeComponents(
            DateComponents.FIFTIES_EPOCH, TimeComponents.H00), 21021 * Constants.JULIAN_DAY + 73800);
        final AbsoluteDate date = new AbsoluteDate(currentDateTime, this.utc);

        final double[] exp = this.msis2000Data.getApValues(date);

        Assert.assertArrayEquals(new double[] { 109 / 48., 2, 3, 2, 3, 323 / 48., 63 / 4. },
            exp, EPS);
    }

    /**
     * @throws PatriusException
     *         if no solar data at date
     * @testType UT
     * 
     * @testedFeature {@link features#MSISE2000_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link ClassicalMSISE2000SolarData#getMinDate()}.
     * @testedMethod {@link ClassicalMSISE2000SolarData#getMaxDate()}.
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

        Assert.assertTrue(this.data.getMinDate().equals(this.msis2000Data.getMinDate()));
        Assert.assertTrue(this.data.getMaxDate().equals(this.msis2000Data.getMaxDate()));

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
        this.data = (SolarActivityDataReader) SolarActivityDataFactory.getSolarActivityDataProvider();

        this.msis2000Data = new ClassicalMSISE2000SolarData(this.data);
    }
}
