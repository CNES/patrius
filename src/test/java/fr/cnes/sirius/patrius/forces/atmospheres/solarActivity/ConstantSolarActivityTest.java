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
 * @history Created 26/04
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:03/11/2014: coverage
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Test the {@link ConstantSolarActivity} class
 * 
 * @see SolarActivityDataProvider
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: ConstantSolarActivityTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class ConstantSolarActivityTest {

    /**
     * threshold
     */
    private static final double EPS = 1E-14;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle ConstantSolarActivity unit tests
         * 
         * @featureDescription test solar activity providers
         * 
         * @coveredRequirements DV-MOD_261
         */
        CONSTANT_SOLAR_ACTIVITY
    }

    /**
     * container
     */
    private ConstantSolarActivity act;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_SOLAR_ACTIVITY}
     * 
     * @testedMethod {@link ConstantSolarActivity#getAp(AbsoluteDate)}.
     * @testedMethod {@link ConstantSolarActivity#getKp(AbsoluteDate)}.
     * @testedMethod {@link ConstantSolarActivity#getInstantFlux(AbsoluteDate)}.
     * @testedMethod {@link ConstantSolarActivity#getMaxDate()}.
     * @testedMethod {@link ConstantSolarActivity#getMinDate()}.
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
    public void testGetters() {

        Utils.setDataRoot("regular-data");

        this.act = new ConstantSolarActivity(140, 15);
        Assert.assertEquals(140, this.act.getInstantFlux(AbsoluteDate.J2000_EPOCH), EPS);
        Assert.assertEquals(15, this.act.getAp(new AbsoluteDate()), EPS);

        this.act = new ConstantSolarActivity(140, 15, SolarActivityToolbox.apToKp(15));
        Assert.assertEquals(140, this.act.getInstantFlux(AbsoluteDate.J2000_EPOCH), EPS);
        Assert.assertEquals(15, this.act.getAp(new AbsoluteDate()), EPS);
        Assert.assertEquals(SolarActivityToolbox.apToKp(15), this.act.getKp(new AbsoluteDate()), EPS);

        try {
            new ConstantSolarActivity(140, 15, SolarActivityToolbox.apToKp(15) + 1);
        } catch (final IllegalArgumentException e) {
            //
        }

        Assert.assertTrue(this.act.getFluxMinDate().equals(AbsoluteDate.PAST_INFINITY));
        Assert.assertTrue(this.act.getFluxMaxDate().equals(AbsoluteDate.FUTURE_INFINITY));

        Assert.assertTrue(this.act.getApKpMinDate().equals(AbsoluteDate.PAST_INFINITY));
        Assert.assertTrue(this.act.getApKpMaxDate().equals(AbsoluteDate.FUTURE_INFINITY));

        Assert.assertTrue(this.act.getMinDate().equals(AbsoluteDate.PAST_INFINITY));
        Assert.assertTrue(this.act.getMaxDate().equals(AbsoluteDate.FUTURE_INFINITY));
    }

    /**
     * @testType UT
     * @testedFeature {@link features#CONSTANT_SOLAR_ACTIVITY}
     * @testedMethod {@link ConstantSolarActivity#ConstantSolarActivity(double, double, double)}.
     * @description tests the if in the constructor that throws IAE
     * @testPassCriteria IllegalArgumentException
     * @since 2.3
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorException() {

        Utils.setDataRoot("regular-data");
        this.act = new ConstantSolarActivity(140, 15);
        final double kkp2 = 147. + SolarActivityToolbox.apToKp(this.act.getAp(new AbsoluteDate()));
        new ConstantSolarActivity(140, 15, kkp2);
    }

}
