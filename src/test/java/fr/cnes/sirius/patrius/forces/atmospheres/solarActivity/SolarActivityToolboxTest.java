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
 * HISTORY
 * VERSION:4.9:FA:FA-3126:10/05/2022:[PATRIUS] Imports obsoletes suite a suppression de reflexion Java dans Patrius 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity;

import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link SolarActivityToolbox}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: SolarActivityToolboxTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class SolarActivityToolboxTest {

    /** Threshold */
    private static final double EPS = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * Features description.
     */
    public enum features {
        /**
         * @featureTitle Solar activity toolbox
         * 
         * @featureDescription here we test the methods of the solar activity toolbox
         * 
         * @coveredRequirements DV-MOD_261
         */
        TOOLBOX
    }

    /**
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#TOOLBOX}
     * 
     * @testedMethod {@link SolarActivityToolbox#apToKp(double)}.
     * @testedMethod {@link SolarActivityToolbox#apToKp(double[])}.
     * @testedMethod {@link SolarActivityToolbox#kpToAp(double)}.
     * @testedMethod {@link SolarActivityToolbox#kpToAp(double[])}.
     * @testedMethod {@link SolarActivityToolbox#checkApSanity(double)}.
     * @testedMethod {@link SolarActivityToolbox#checkKpSanity(double)}.
     * 
     * @description make sure the correct coefficients are return by these method
     * 
     * @input solar data
     * 
     * @output Ap array
     * 
     * @testPassCriteria @testPassCriteria the different coefficients must be the expected ones. Threshold of 1e-14 is
     *                   used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testConversion() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
                                PatriusException {

        try {
            SolarActivityToolbox.kpToAp(-1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }

        try {
            SolarActivityToolbox.kpToAp(9.1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }
        try {
            SolarActivityToolbox.apToKp(-1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }
        try {
            SolarActivityToolbox.kpToAp(401);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }

        Assert.assertEquals(179, SolarActivityToolbox.kpToAp(7 + 2. / 3.), EPS);
        Assert.assertEquals(27, SolarActivityToolbox.kpToAp(4), EPS);
        Assert.assertEquals(28.41188717875467518, SolarActivityToolbox.kpToAp(4.1), EPS);

        Assert.assertEquals(7 + 2. / 3., SolarActivityToolbox.apToKp(179), EPS);
        Assert.assertEquals(4, SolarActivityToolbox.apToKp(27), EPS);
        Assert.assertEquals(4.1, SolarActivityToolbox.apToKp(28.41188717875467518), EPS);

        Assert.assertEquals(0, SolarActivityToolbox.apToKp(0), EPS);
        Assert.assertEquals(9, SolarActivityToolbox.apToKp(400), EPS);
        Assert.assertEquals(0, SolarActivityToolbox.kpToAp(0), EPS);
        Assert.assertEquals(400, SolarActivityToolbox.kpToAp(9), EPS);

        Assert.assertArrayEquals(new double[] { 0, 400, 27, 28.41188717875467518 },
            SolarActivityToolbox.kpToAp(new double[] { 0, 9, 4, 4.1 }), EPS);

        Assert.assertArrayEquals(new double[] { 0, 9, 4, 4.1 },
            SolarActivityToolbox.apToKp(new double[] { 0, 400, 27, 28.41188717875467518 }), EPS);

        final AbsoluteDate date = new AbsoluteDate();
        final SolarActivityDataProvider provider = new SolarActivityDataProvider(){
            boolean flag = true;

            @Override
            public AbsoluteDate getMinDate() {
                return null;
            }

            @Override
            public AbsoluteDate getMaxDate() {
                return null;
            }

            @Override
            public double getKp(final AbsoluteDate date) throws PatriusException {
                return 0;
            }

            @Override
            public SortedMap<AbsoluteDate, Double>
                    getInstantFluxValues(final AbsoluteDate date1, final AbsoluteDate date2)
                                                                                            throws PatriusException {
                final SortedMap<AbsoluteDate, Double> map = new TreeMap<AbsoluteDate, Double>();
                map.put(date, 0.0);
                return map;
            }

            @Override
            public double getInstantFluxValue(final AbsoluteDate date) throws PatriusException {
                return 0;
            }

            @Override
            public AbsoluteDate getFluxMinDate() {
                if (this.flag) {
                    this.flag = false;
                    return date.shiftedBy(300.0);
                } else {
                    return date;
                }
            }

            @Override
            public AbsoluteDate getFluxMaxDate() {
                return date.shiftedBy(500.0);
            }

            @Override
            public SortedMap<AbsoluteDate, Double[]>
                    getApKpValues(final AbsoluteDate date1, final AbsoluteDate date2)
                                                                                     throws PatriusException {
                final SortedMap<AbsoluteDate, Double[]> map = new TreeMap<AbsoluteDate, Double[]>();
                map.put(date, new Double[] { 0.0, 0.1 });
                return map;
            }

            @Override
            public AbsoluteDate getApKpMinDate() {
                return null;
            }

            @Override
            public AbsoluteDate getApKpMaxDate() {
                return null;
            }

            @Override
            public double getAp(final AbsoluteDate date) throws PatriusException {
                return 0;
            }

            @Override
            public double getStepApKp() throws PatriusException {
                return 3 * 3600.;
            }

            @Override
            public double getStepF107() throws PatriusException {
                return 86400.;
            }
        };
        // coverage of the getMeanAp exception:
        boolean rez = false;
        try {
            SolarActivityToolbox.getMeanAp(date, date, provider);
        } catch (final IllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        // coverage of the getMeanFlux exceptions:
        // exception 1:
        rez = false;
        try {
            SolarActivityToolbox.getMeanFlux(date.shiftedBy(200.0), date, provider);
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        // exception 2:
        rez = false;
        try {
            SolarActivityToolbox.getMeanFlux(date, date.shiftedBy(200.0), provider);
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        // exception 2:
        rez = false;
        try {
            SolarActivityToolbox.getMeanFlux(date.shiftedBy(-200.0), date, provider);
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        // exception 3:
        rez = false;
        try {
            SolarActivityToolbox.getMeanFlux(date, date, provider);
        } catch (final IllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        // coverage of the getAverageFlux exception 1:
        rez = false;
        try {
            SolarActivityToolbox.getAverageFlux(date.shiftedBy(200.0), date, provider);
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        // exception 2:
        rez = false;
        try {
            SolarActivityToolbox.getAverageFlux(date, date.shiftedBy(600.0), provider);
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        // exception 2:
        rez = false;
        try {
            SolarActivityToolbox.getAverageFlux(date.shiftedBy(-400.0), date, provider);
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

    }

    /**
     * @throws PatriusException
     *         if no solar data
     * @testType UT
     * 
     * @testedFeature {@link features#TOOLBOX}
     * 
     * @testedMethod {@link SolarActivityToolbox#getMeanFlux(AbsoluteDate, int, SolarActivityDataProvider)}.
     * 
     * @description make sure the correct coefficients are return by these method
     * 
     * @input date, days, solar data
     * 
     * @output mean flux
     * 
     * @testPassCriteria the different coefficients must be the expected ones (references provided from the same file).
     *                   Threshold of 1e-14 is used.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testMean() throws PatriusException {

        CNESUtils.clearNewFactoriesAndCallSetDataRoot("atmosphere");
        final TimeScale utc = TimeScalesFactory.getTT();
        SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader(
            SolarActivityDataFactory.ACSOL_FILENAME));

        final SolarActivityDataProvider data = SolarActivityDataFactory.getSolarActivityDataProvider();
        final DateTimeComponents currentDateTime = new DateTimeComponents(new DateTimeComponents(
            DateComponents.FIFTIES_EPOCH, TimeComponents.H00), 5912 * 86400.0 + 61200);

        final AbsoluteDate date = new AbsoluteDate(currentDateTime, utc);
        Assert.assertEquals(89.5753086419753000, SolarActivityToolbox.getAverageFlux(date.shiftedBy(-81 / 2. * 86400.),
            date.shiftedBy(81 / 2. * 86400.), data), EPS);

        try {
            SolarActivityToolbox.getAverageFlux(data.getFluxMinDate().shiftedBy(-5000), date, data);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected!
        }
    }

}
