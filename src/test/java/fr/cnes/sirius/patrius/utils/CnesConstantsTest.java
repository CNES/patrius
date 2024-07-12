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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * CNES added constants class test.
 * 
 * @author Sylvain VRESK
 * @version $Id: ConstantsTest.java 17926 2017-09-11 13:54:24Z bignon $
 * @since 1.0
 */
public class CnesConstantsTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Constants tests
         * 
         * @featureDescription verification of the CNES constants
         * 
         * @coveredRequirements DV-MOD_380, DV-MOD_390, DV-MOD_410
         */
        CONSTANTS
    }

    /** Smallest positive number such that 1 - EPSILON is not numerically equal to 1. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANTS}
     * 
     * @testedMethod {@link Constants#toString()}
     * 
     * @description Constants tests
     * 
     * @input Constants data
     * 
     * @output constants
     * 
     * @testPassCriteria equality with an epsilon of 1e-16 due to machine errors only.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testConstantsValues() {
        final double delta = this.machineEpsilon;
        final double G_CODATA = 6.67384E-11;
        final double S_UA = 149597870000.0;
        assertEquals(MathLib.asin(2. / MathLib.sqrt(5.)), Constants.CRITICAL_PROGRADE_INCLINATION, delta);
        assertEquals(FastMath.PI - MathLib.asin(2. / MathLib.sqrt(5.)),
            Constants.CRITICAL_RETROGRADE_INCLINATION, delta);
        assertEquals(G_CODATA, Constants.GRAVITATIONAL_CONSTANT, delta);
        assertEquals(S_UA, Constants.SEIDELMANN_UA, delta);
        // IERS 92 theory data ...
        final double iers92_c = 299792458.;
        final double iers92_ua = 150000000000.;
        final double iers92_earth_mu = 398600443104792.;
        final double iers92_earth_Re = 6378140.;
        final double iers92_earth_f = 0.0033536;
        final double iers92_earth_w_dot = 7.29211537319376E-05;
        final double iers92_earth_j2 = 0.00108263;
        final double iers92_sun_mu = 1.32712443255261E+20;
        final double iers92_sun_Re = 650000000.;
        final double iers92_sun_f = 0.;
        final double iers92_sun_w_dot = -1.13855098302775E-05;
        final double iers92_moon_mu = 4902798867501.21;
        final double iers92_moon_Re = 1737500.;
        final double iers92_moon_f = 0.;
        final double iers92_moon_w_dot = 2.86532965763744E-06;
        assertEquals(iers92_c, Constants.IERS92_LIGHT_VELOCITY, delta);
        assertEquals(iers92_ua, Constants.IERS92_UA, delta);
        assertEquals(iers92_earth_mu, Constants.IERS92_EARTH_GRAVITATIONAL_PARAMETER, delta);
        assertEquals(iers92_earth_Re, Constants.IERS92_EARTH_EQUATORIAL_RADIUS, delta);
        assertEquals(iers92_earth_f, Constants.IERS92_EARTH_FLATTENING, delta);
        assertEquals(iers92_earth_w_dot, Constants.IERS92_EARTH_ROTATION_RATE, delta);
        assertEquals(iers92_earth_j2, Constants.IERS92_EARTH_J2, delta);
        assertEquals(iers92_sun_mu, Constants.IERS92_SUN_GRAVITATIONAL_PARAMETER, delta);
        assertEquals(iers92_sun_Re, Constants.IERS92_SUN_EQUATORIAL_RADIUS, delta);
        assertEquals(iers92_sun_f, Constants.IERS92_SUN_FLATTENING, delta);
        assertEquals(iers92_sun_w_dot, Constants.IERS92_SUN_ROTATION_RATE, delta);
        assertEquals(iers92_moon_mu, Constants.IERS92_MOON_GRAVITATIONAL_PARAMETER, delta);
        assertEquals(iers92_moon_Re, Constants.IERS92_MOON_EQUATORIAL_RADIUS, delta);
        assertEquals(iers92_moon_f, Constants.IERS92_MOON_FLATTENING, delta);
        assertEquals(iers92_moon_w_dot, Constants.IERS92_MOON_ROTATION_RATE, delta);
        // UAI1994 theory data ...
        final double uai94_c = 299792458.;
        final double uai94_ua = 149597871475.;
        final double uai94_prec = 5028.83;
        final double uai94_obl = 84381.412;
        final double uai94_G = 6.67259E-11;
        final double uai94_earth_mu = 398600441500000.;
        final double uai94_earth_Re = 6378136.55;
        final double uai94_earth_f = 0.00335281318;
        final double uai94_earth_w_dot = 7.292115E-05;
        final double uai94_earth_j2 = 0.00108263;
        final double uai94_sun_mu = 1.32712440018E+20;
        final double uai94_sun_Re = 650000000.;
        final double uai94_sun_f = 0.;
        final double uai94_sun_w_dot = -1.13855098302775E-05;
        final double uai94_moon_mu = 4902801000000.;
        final double uai94_moon_Re = 1737500.;
        final double uai94_moon_f = 0.;
        final double uai94_moon_w_dot = 2.86532965763744E-06;
        assertEquals(uai94_c, Constants.UAI1994_LIGHT_VELOCITY, delta);
        assertEquals(uai94_ua, Constants.UAI1994_UA, delta);
        assertEquals(uai94_prec, Constants.UAI1994_PRECESSION_RATE, delta);
        assertEquals(uai94_obl, Constants.UAI1994_OBLIQUITY, delta);
        assertEquals(uai94_G, Constants.UAI1994_GRAVITATIONAL_CONSTANT, delta);
        assertEquals(uai94_earth_mu, Constants.UAI1994_EARTH_GRAVITATIONAL_PARAMETER, delta);
        assertEquals(uai94_earth_Re, Constants.UAI1994_EARTH_EQUATORIAL_RADIUS, delta);
        assertEquals(uai94_earth_f, Constants.UAI1994_EARTH_FLATTENING, delta);
        assertEquals(uai94_earth_w_dot, Constants.UAI1994_EARTH_ROTATION_RATE, delta);
        assertEquals(uai94_earth_j2, Constants.UAI1994_EARTH_J2, delta);
        assertEquals(uai94_sun_mu, Constants.UAI1994_SUN_GRAVITATIONAL_PARAMETER, delta);
        assertEquals(uai94_sun_Re, Constants.UAI1994_SUN_EQUATORIAL_RADIUS, delta);
        assertEquals(uai94_sun_f, Constants.UAI1994_SUN_FLATTENING, delta);
        assertEquals(uai94_sun_w_dot, Constants.UAI1994_SUN_ROTATION_RATE, delta);
        assertEquals(uai94_moon_mu, Constants.UAI1994_MOON_GRAVITATIONAL_PARAMETER, delta);
        assertEquals(uai94_moon_Re, Constants.UAI1994_MOON_EQUATORIAL_RADIUS, delta);
        assertEquals(uai94_moon_f, Constants.UAI1994_MOON_FLATTENING, delta);
        assertEquals(uai94_moon_w_dot, Constants.UAI1994_MOON_ROTATION_RATE, delta);
    }
}
