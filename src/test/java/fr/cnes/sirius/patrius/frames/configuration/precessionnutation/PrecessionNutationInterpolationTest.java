/**
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
 * VERSION:4.13.5:DM:DM-319:03/07/2024:[PATRIUS] Assurer la compatibilite ascendante de la v4.13
 * VERSION:4.13.2:DM:DM-222:08/03/2024:[PATRIUS] Assurer la compatibilité ascendante
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration.PatriusVersionCompatibility;

/**
 * Test class for {@link PrecessionNutationInterpolation}.
 * 
 * @author Thibaut BONIT
 * 
 * @since 4.13
 */
public class PrecessionNutationInterpolationTest {

    private final double epsN = 5e-12;

    private final double epsNd = 1e-9;

    @Test
    public void testOrigin() {
        // IERS 2010
        final PrecessionNutationModel pnD = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010);
        final PrecessionNutationModel pnN = new PrecessionNutationInterpolation(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010));

        // IERS 2003
        final PrecessionNutationModel pnD3 = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003);
        final PrecessionNutationModel pnN3 = new PrecessionNutationInterpolation(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003));

        // PrecessionNutationInterpolation should wrap the given model
        Assert.assertEquals(pnD.getOrigin(), pnN.getOrigin());
        Assert.assertEquals(pnD3.getOrigin(), pnN3.getOrigin());
    }

    @Test
    public void testIsDirect() {
        // IERS 2010
        final PrecessionNutationModel pnN = new PrecessionNutationInterpolation(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010));

        // IERS 2003
        final PrecessionNutationModel pnN3 = new PrecessionNutationInterpolation(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003));

        // PrecessionNutationInterpolation aren't direct by definition (interpolated)
        Assert.assertFalse(pnN.isDirect());
        Assert.assertFalse(pnN3.isDirect());
    }

    @Test
    public void testCIPInterpolations() {
        
        PatriusConfiguration.setPatriusCompatibilityMode(PatriusVersionCompatibility.NEW_MODELS);

        // IERS 2010
        final PrecessionNutationModel pnD = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010);
        final PrecessionNutationModel pnN = new PrecessionNutationInterpolation(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010));

        // IERS 2003
        final PrecessionNutationModel pnD3 = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003);
        final PrecessionNutationModel pnN3 = new PrecessionNutationInterpolation(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003));

        // Date
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 0, 0, TimeScalesFactory.getTAI());

        for (int i = 0; i < 43200; i += 100) {

            final AbsoluteDate currentDate = date.shiftedBy(i);

            // CIP motion
            final double[] cipD = pnD.getCIPCoordinates(currentDate).getCIPMotion();
            final double[] cipN = pnN.getCIPCoordinates(currentDate).getCIPMotion();

            Assert.assertArrayEquals(cipN, cipD, this.epsN);

            final double[] cipD3 = pnD3.getCIPCoordinates(currentDate).getCIPMotion();
            final double[] cipN3 = pnN3.getCIPCoordinates(currentDate).getCIPMotion();

            Assert.assertArrayEquals(cipN3, cipD3, this.epsN);

            // CIP motion time derivatives
            final double[] cipdD3 = pnD3.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();
            final double[] cipdN3 = pnN3.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();

            Assert.assertArrayEquals(cipdN3, cipdD3, this.epsNd);

            final double[] cipdD = pnD.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();
            final double[] cipdN = pnN.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();

            Assert.assertArrayEquals(cipdN, cipdD, this.epsNd);
        }
    }

    @Test
    public void testCIPInterpolationsCoverage() {

        // This cover test checks the different computation branches in getCIPCoordinates(date)
        // Initialize several PN models with specific configuration
        final PrecessionNutationInterpolation pnN1 = new PrecessionNutationInterpolation(
            new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003), 100, 2, 10000, 3);
        final PrecessionNutationInterpolation pnN2 = new PrecessionNutationInterpolation(
            new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003), 100, 2, 100, 3);
        final PrecessionNutationInterpolation pnN3 = new PrecessionNutationInterpolation(
            new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003), 100, 2, 1000, 3);

        // Date
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 0, 0, TimeScalesFactory.getTAI());

        // Initialize some CIP ephemeris
        final AbsoluteDate firstUsableDate1 = date.shiftedBy(2000);
        final AbsoluteDate lastUsableDate1 = firstUsableDate1.shiftedBy(15000);
        pnN1.initializeCIPEphemeris(firstUsableDate1, lastUsableDate1);

        final AbsoluteDate firstUsableDate2 = date.shiftedBy(100);
        final AbsoluteDate lastUsableDate2 = firstUsableDate2.shiftedBy(1500);
        pnN2.initializeCIPEphemeris(firstUsableDate2, lastUsableDate2);

        final AbsoluteDate firstUsableDate3 = date.shiftedBy(39000);
        final AbsoluteDate lastUsableDate3 = firstUsableDate3.shiftedBy(39900);
        pnN3.initializeCIPEphemeris(firstUsableDate3, lastUsableDate3);

        // Forward loop
        for (int i = 0; i < 20_000; i += 100) {

            final AbsoluteDate currentDate = date.shiftedBy(i);

            // CIP motion
            final double[] cipN1 = pnN1.getCIPCoordinates(currentDate).getCIPMotion();
            final double[] cipN2 = pnN2.getCIPCoordinates(currentDate).getCIPMotion();
            final double[] cipN3 = pnN3.getCIPCoordinates(currentDate).getCIPMotion();

            Assert.assertArrayEquals(cipN1, cipN2, this.epsN);
            Assert.assertArrayEquals(cipN1, cipN3, this.epsN);

            // CIP motion time derivatives
            final double[] cipdN1 = pnN1.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();
            final double[] cipdN2 = pnN2.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();
            final double[] cipdN3 = pnN3.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();

            Assert.assertArrayEquals(cipdN1, cipdN2, this.epsNd);
            Assert.assertArrayEquals(cipdN1, cipdN3, this.epsNd);
        }

        // Backward loop
        for (int i = 40_000; i > 20_000; i -= 100) {

            final AbsoluteDate currentDate = date.shiftedBy(i);

            // CIP motion
            final double[] cipN1 = pnN1.getCIPCoordinates(currentDate).getCIPMotion();
            final double[] cipN2 = pnN2.getCIPCoordinates(currentDate).getCIPMotion();
            final double[] cipN3 = pnN3.getCIPCoordinates(currentDate).getCIPMotion();

            Assert.assertArrayEquals(cipN1, cipN2, this.epsN);
            Assert.assertArrayEquals(cipN1, cipN3, this.epsN);

            // CIP motion time derivatives
            final double[] cipdN1 = pnN1.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();
            final double[] cipdN2 = pnN2.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();
            final double[] cipdN3 = pnN3.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();

            Assert.assertArrayEquals(cipdN1, cipdN2, this.epsNd);
            Assert.assertArrayEquals(cipdN1, cipdN3, this.epsNd);
        }
    }

    @Test
    public void testGetters() {

        // CIP ephemeris not initialized (cipEphemeris = null)
        // IERS 2010
        final PrecessionNutationModel model2010 = new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010);
        final PrecessionNutationInterpolation pn2010 = new PrecessionNutationInterpolation(model2010);

        // IERS 2003
        final PrecessionNutationModel model2003 = new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003);
        final PrecessionNutationInterpolation pn2003 = new PrecessionNutationInterpolation(model2003);

        Assert.assertEquals(PrecessionNutationInterpolation.DEFAULT_INTERP_ORDER, pn2010.getInterpolationOrder());
        Assert.assertEquals(PrecessionNutationInterpolation.DEFAULT_INTERP_ORDER, pn2003.getInterpolationOrder());

        Assert.assertEquals(PrecessionNutationInterpolation.DEFAULT_INTERP_STEP, pn2010.getInterpolationStep());
        Assert.assertEquals(PrecessionNutationInterpolation.DEFAULT_INTERP_STEP, pn2003.getInterpolationStep());

        Assert.assertEquals(PrecessionNutationInterpolation.DEFAULT_EPHEM_MAX_SIZE, pn2010.getEphemerisMaxSize());
        Assert.assertEquals(PrecessionNutationInterpolation.DEFAULT_EPHEM_MAX_SIZE, pn2003.getEphemerisMaxSize());

        Assert.assertEquals(PrecessionNutationInterpolation.DEFAULT_ALLOWED_EXTENSION_BEFORE_EPHEM_RESET,
            pn2010.getAllowedExtensionBeforeEphemerisReset());
        Assert.assertEquals(PrecessionNutationInterpolation.DEFAULT_ALLOWED_EXTENSION_BEFORE_EPHEM_RESET,
            pn2003.getAllowedExtensionBeforeEphemerisReset());

        Assert.assertEquals(model2010, pn2010.getModel());
        Assert.assertEquals(model2003, pn2003.getModel());

        Assert.assertNull(pn2010.getCurrentUsableInterval());
        Assert.assertNull(pn2003.getCurrentUsableInterval());

        Assert.assertEquals(0., pn2010.getEphemerisCacheReusabilityRatio(), 0.);
        Assert.assertEquals(0., pn2003.getEphemerisCacheReusabilityRatio(), 0.);

        Assert.assertEquals(0, pn2010.getEphemerisSize());
        Assert.assertEquals(0, pn2003.getEphemerisSize());

        // CIP ephemeris initialized (cipEphemeris != null)
        final AbsoluteDate firstUsableDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate lastUsableDate = firstUsableDate.shiftedBy(Constants.JULIAN_DAY);
        pn2010.initializeCIPEphemeris(firstUsableDate, lastUsableDate);
        pn2003.initializeCIPEphemeris(firstUsableDate, lastUsableDate);

        final AbsoluteDate expectedFirstUsableDate = new AbsoluteDate("2000-01-01T00:00:00.000");
        final AbsoluteDate expectedLastUsableDate = new AbsoluteDate("2000-01-02T12:00:00.000");
        final AbsoluteDateInterval expectedInterval = new AbsoluteDateInterval(expectedFirstUsableDate,
            expectedLastUsableDate);
        Assert.assertEquals(expectedInterval, pn2010.getCurrentUsableInterval());
        Assert.assertEquals(expectedInterval, pn2003.getCurrentUsableInterval());

        Assert.assertTrue(Double.isNaN(pn2010.getEphemerisCacheReusabilityRatio()));
        Assert.assertTrue(Double.isNaN(pn2003.getEphemerisCacheReusabilityRatio()));

        Assert.assertEquals(6, pn2010.getEphemerisSize());
        Assert.assertEquals(6, pn2003.getEphemerisSize());

        // Non regression on the public static variables values
        Assert.assertEquals(4, PrecessionNutationInterpolation.DEFAULT_INTERP_ORDER);
        Assert.assertEquals(43_200, PrecessionNutationInterpolation.DEFAULT_INTERP_STEP);
        Assert.assertEquals(5_000, PrecessionNutationInterpolation.DEFAULT_EPHEM_MAX_SIZE);
        Assert.assertEquals(60, PrecessionNutationInterpolation.DEFAULT_ALLOWED_EXTENSION_BEFORE_EPHEM_RESET);
    }

    @Test
    public void testExceptions() {

        final PrecessionNutationModel pnD = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010);

        // Try to define a number of interpolation points < 2 (should fail)
        try {
            new PrecessionNutationInterpolation(pnD, 60, 1, 5, 3);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Try to define an odd number of interpolation points (should fail)
        try {
            new PrecessionNutationInterpolation(pnD, 60, 5, 5, 3);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }

        PrecessionNutationInterpolation pnN = new PrecessionNutationInterpolation(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010));
        // Try to use a non direct (already interpolated) model (should fail)
        try {
            new PrecessionNutationInterpolation(pnN, 60, 2, 5, 3);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Try to initialize an ephemeris that exceeds the maximum allowed size (should fail)
        pnN = new PrecessionNutationInterpolation(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010), 60, 2, 5, 3);
        final AbsoluteDate firstUsableDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate lastUsableDate = firstUsableDate.shiftedBy(43200);
        try {
            pnN.initializeCIPEphemeris(firstUsableDate, lastUsableDate);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }
}
