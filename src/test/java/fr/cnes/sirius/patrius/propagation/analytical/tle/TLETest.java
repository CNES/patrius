/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.6:DM:DM-2563:27/01/2021:[PATRIUS] Ajout de la matrice de transition J2Secular 
 * VERSION:4.5:FA:FA-2357:27/05/2020:champs TLE mal considere 
* VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
* VERSION:4.4:FA:FA-2296:04/10/2019:[PATRIUS] Meilleure robustesse a la lecture de TLEs
* VERSION:4.4:FA:FA-2258:04/10/2019:[PATRIUS] ecriture TLE et angles negatif
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::FA:1470:19/03/2018:Anomaly TLE reading
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.analytical.tle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test
 */
public class TLETest {

    /**
     * FA-2564.
     * Check that a TLE with a B* equal to 0.1 is properly built.
     */
    @Test
    public void testBStar() throws PatriusException {
        // Initialization
        final double ecc = 0.15; 
        final double inc = FastMath.toRadians(125.); 
        final double paIn = FastMath.toRadians(-10.); 
        final double raanIn = FastMath.toRadians(-105.); 
        final double meanAnomalyIn = FastMath.toRadians(-230.); 
        final AbsoluteDate date = new AbsoluteDate(2019, 01, 01, TimeScalesFactory.getTAI()).shiftedBy(148.72579957 * 86400. + 35.);
        final double coef = 2 * FastMath.PI / 86400.;

         // Build TLE
        final TLE tle1 = new TLE(44300, 'U', 19, 30, "B", 0, 999, date, 2.09023630 * coef, -.00000032 * 2. * coef / 86400.,
                0, ecc, inc, paIn, raanIn, meanAnomalyIn, 5, 0.1); 

        // Check TLE is as expected
        Assert.assertEquals("1 44300U 19030B   19149.72579957 -.00000032  00000-0  10000-0 0  9996", tle1.getLine1());
        Assert.assertEquals("2 44300 125.0000 255.0000 1500000 350.0000 130.0000  2.09023630    51", tle1.getLine2());

        // Build TLE 2
       final TLE tle2 = new TLE(44300, 'U', 19, 30, "B", 0, 999, date, 2.09023630 * coef, -.00000032 * 2. * coef / 86400.,
               0, ecc, inc, paIn, raanIn, meanAnomalyIn, 5, 0.01); 

       // Check TLE is as expected
       Assert.assertEquals("1 44300U 19030B   19149.72579957 -.00000032  00000-0  10000-1 0  9997", tle2.getLine1());
       Assert.assertEquals("2 44300 125.0000 255.0000 1500000 350.0000 130.0000  2.09023630    51", tle2.getLine2());
    }

    /**
     * Check that a TLE with negative orbital values is handled properly:
     * <ul>Inclination is set within [0, 180°]</ul>
     * <ul>Perigee argument, RAAN and anomaly are set within [0, 360°]</ul>
     */
    @Test
    public void testNegativeAngle() throws PatriusException {
        // Initialization
        final double ecc = 0.15; 
        final double inc = FastMath.toRadians(125.); 
        final double paIn = FastMath.toRadians(-10.); 
        final double raanIn = FastMath.toRadians(-105.); 
        final double meanAnomalyIn = FastMath.toRadians(-230.); 
        final AbsoluteDate date = new AbsoluteDate(2019, 01, 01, TimeScalesFactory.getTAI()).shiftedBy(148.72579957 * 86400. + 35.);
        final double coef = 2 * FastMath.PI / 86400.;

         // Build TLE
        final TLE tle = new TLE(44300, 'U', 19, 30, "B", 0, 999, date, 2.09023630 * coef, -.00000032 * 2. * coef / 86400.,
                0, ecc, inc, paIn, raanIn, meanAnomalyIn, 5, 0); 
        
        // Check TLE is as expected
        Assert.assertEquals("1 44300U 19030B   19149.72579957 -.00000032  00000-0  00000-0 0  9995", tle.getLine1());
        Assert.assertEquals("2 44300 125.0000 255.0000 1500000 350.0000 130.0000  2.09023630    51", tle.getLine2());

        // Build TLE
        try {
            new TLE(44300, 'U', 19, 30, "B", 0, 999, date, 2.09023630 * coef, -.00000032 * 2. * coef / 86400.,
                    0, ecc, -inc, paIn, raanIn, meanAnomalyIn, 5, 0);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testTLEFormat() throws PatriusException {
        String line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        String line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";

        Assert.assertTrue(TLE.isFormatOK(line1, line2));

        final TLE tle = new TLE(line1, line2);
        Assert.assertEquals(27421, tle.getSatelliteNumber(), 0);
        Assert.assertEquals(2002, tle.getLaunchYear());
        Assert.assertEquals(21, tle.getLaunchNumber());
        Assert.assertEquals("A", tle.getLaunchPiece());
        Assert.assertEquals(-0.0089879, tle.getBStar(), 0);
        Assert.assertEquals(0, tle.getEphemerisType());
        Assert.assertEquals(98.749, MathLib.toDegrees(tle.getI()), 1e-10);
        Assert.assertEquals(199.5121, MathLib.toDegrees(tle.getRaan()), 1e-10);
        Assert.assertEquals(0.0001333, tle.getE(), 1e-10);
        Assert.assertEquals(133.9522, MathLib.toDegrees(tle.getPerigeeArgument()), 1e-10);
        Assert.assertEquals(226.1918, MathLib.toDegrees(tle.getMeanAnomaly()), 1e-10);
        Assert.assertEquals(14.26113993, tle.getMeanMotion() * Constants.JULIAN_DAY/ (2 * FastMath.PI), 0);
        Assert.assertEquals(tle.getRevolutionNumberAtEpoch(), 6, 0);
        Assert.assertEquals(tle.getElementNumber(), 2, 0);

        line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14*26113993    62";
        Assert.assertFalse(TLE.isFormatOK(line1, line2));

        line1 = "1 27421 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
        Assert.assertFalse(TLE.isFormatOK(line1, line2));

        line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 10006113993    62";
        Assert.assertFalse(TLE.isFormatOK(line1, line2));

        line1 = "1 27421U 2002021A   02124.48976499 -.00021470  00000-0 -89879 2 0    20";
        line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
        Assert.assertFalse(TLE.isFormatOK(line1, line2));

    }

    /**
     * Check that 7th char can be any character.
     */
    @Test
    public void testFT2357() throws PatriusException {
        // Test 7th character
        final String line1 = "1 37820O 11053A   18027.60500207  .00000000  00000-0  13127-3 2  0000";
        final String line2 = "2 37820  42.7489 247.2715 0020157 257.2217  23.6918 16.01003387000003";
        final TLE tle = new TLE(line1, line2);
        Assert.assertTrue(TLE.isFormatOK(line1, line2));
    }

    @Test
    public void testFT1470() throws PatriusException {

        String line1 = "1 37820U 11053A   18027.60500207  .00000000  00000-0  13127-3 2  0000";
        String line2 = "2 37820  42.7489 247.2715 0020157 257.2217  23.6918 16.01003387000003";
        TLE tle = new TLE(line1, line2);
        Assert.assertTrue(TLE.isFormatOK(line1, line2));

        line1 = "1 37820U 2011053A 18027.60500207 0.00000000  00000-0  13127-3 2  0002";
        line2 = "2 37820  42.7489 247.2715 0020157 257.2217  23.6918 16.01003387000003";
        TLE tle2 = new TLE(line1, line2);
        Assert.assertTrue(TLE.isFormatOK(line1, line2));

        assertEqualsTLE(tle, tle2);

        line1 = "1 37820U 02053A   18027.60500207  .02000000  00000-0  13127-3 2  0002";
        line2 = "2 37820  42.7489 247.2715 0020157 257.2217  23.6918 16.01003387000003";
        tle = new TLE(line1, line2);
        Assert.assertTrue(TLE.isFormatOK(line1, line2));

        line1 = "1 37820U 2002053A 18027.60500207 0.02000000  00000-0  13127-3 2  0004";
        line2 = "2 37820  42.7489 247.2715 0020157 257.2217  23.6918 16.01003387000003";
        tle2 = new TLE(line1, line2);
        Assert.assertTrue(TLE.isFormatOK(line1, line2));

        assertEqualsTLE(tle, tle2);

        line1 = "1 37820U 00053A   18027.60500207  .02000003  00000-0  13127-1 2  0023";
        line2 = "2 37820  42.7489 247.2715 0020157 257.2217  23.6918 16.01003387000003";
        tle = new TLE(line1, line2);
        Assert.assertTrue(TLE.isFormatOK(line1, line2));

        line1 = "1 37820U 2000053A 18027.60500207 0.02000003  00000-0  13127-1 2  0025";
        line2 = "2 37820  42.7489 247.2715 0020157 257.2217  23.6918 16.01003387000003";
        tle2 = new TLE(line1, line2);
        Assert.assertTrue(TLE.isFormatOK(line1, line2));

        assertEqualsTLE(tle, tle2);

        line1 = "1 37820U 200053AB 18027.60500207 0.02000003  00000-0  13127-1 2  0025";
        line2 = "2 37820  42.7489 247.2715 0020157 257.2217  23.6918 16.01003387000003";
        tle2 = new TLE(line1, line2);
        Assert.assertTrue(TLE.isFormatOK(line1, line2));
    }

    /**
     * Check that tested TLE are accepted.
     */
    @Test
    public void testFT2296() throws PatriusException {

        // Case 1
        String line1 = "1 01100U 65012  C 65061.91463922  .09143209 +1064303 +00000-0 0  9994";
        String line2 = "2 01100 064.8523 269.0466 0179321 047.9362 313.6205 16.03366401001370";
        Assert.assertTrue(TLE.isFormatOK(line1, line2));

        // Case 2 (wrong format: there is one space more than expected at pos 11 of line 1"
        line1 = "1 10588U 0         03073.88889961 -.00000016 +00000-0 +00000-0 0 9990";
        line2 = "2 10588 074.0116 178.9324 0039084 201.7630 245.3510 12.48494445145432";
        try {
            new TLE(line1, line2);
            Assert.fail();
        } catch (final NumberFormatException e) {
            // Expected
            Assert.assertTrue(true);
        }
        Assert.assertFalse(TLE.isFormatOK(line1, line2));

        // Corrected TLE
        line1 = "1 10588U 0        03073.88889961 -.00000016 +00000-0 +00000-0 0  9990";
        line2 = "2 10588 074.0116 178.9324 0039084 201.7630 245.3510 12.48494445145432";
        Assert.assertTrue(TLE.isFormatOK(line1, line2));
    }
    
    @Test
    public void testSymmetry() throws PatriusException {
        checkSymmetry("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        checkSymmetry("1 31928U 98067BA  08269.84884916  .00114257  17652-4  13615-3 0  4412",
                "2 31928  51.6257 175.4142 0001703  41.9031 318.2112 16.08175249 68368");
    }

    private static void checkSymmetry(final String line1, final String line2)
            throws PatriusException {
        final TLE tleRef = new TLE(line1, line2);
        final TLE tle = new TLE(tleRef.getSatelliteNumber(), tleRef.getClassification(),
                tleRef.getLaunchYear(), tleRef.getLaunchNumber(), tleRef.getLaunchPiece(),
                tleRef.getEphemerisType(), tleRef.getElementNumber(), tleRef.getDate(),
                tleRef.getMeanMotion(), tleRef.getMeanMotionFirstDerivative(),
                tleRef.getMeanMotionSecondDerivative(), tleRef.getE(), tleRef.getI(),
                tleRef.getPerigeeArgument(), tleRef.getRaan(), tleRef.getMeanAnomaly(),
                tleRef.getRevolutionNumberAtEpoch(), tleRef.getBStar());
        Assert.assertEquals(line1, tle.getLine1());
        Assert.assertEquals(line2, tle.getLine2());
    }

    @Test
    public void testBug74() throws PatriusException {
        checkSymmetry("1 00001U 00001A   12026.45833333 2.94600864  39565-9  16165-7 1    12",
                "2 00001 627.0796  94.4522 0000000 264.9662   0.4817  0.00000000    12");
    }

    @Test
    public void testBug77() throws PatriusException {
        checkSymmetry("1 05555U 71086J   12026.96078249 -.00000004  10000-8  01234-9 0  9081",
                "2 05555  74.0161 228.9750 0075476 328.9888  30.6709 12.26882470804545");
    }

    @Test(expected = PatriusException.class)
    public void testDifferentSatNumbers() throws PatriusException {
        new TLE("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                "2 27422  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
    }

    @Test
    public void testChecksumOK() throws PatriusException {
        TLE.isFormatOK("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
    }

    @Test(expected = PatriusException.class)
    public void testWrongChecksum1() throws PatriusException {
        TLE.isFormatOK("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    21",
                "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
    }

    @Test(expected = PatriusException.class)
    public void testWrongChecksum2() throws PatriusException {
        TLE.isFormatOK("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    61");
    }

    @Test
    public void testToString() throws PatriusException {
        final TLE tle = new TLE(
                "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    21",
                "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        final String expectedString = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    21"
                + System.getProperty("line.separator")
                + "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
        Assert.assertEquals(tle.toString(), expectedString);
    }

    @Test
    public void testToString2() throws PatriusException {
        final TLE tle = new TLE(
                "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    21",
                "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        final String expectedString = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    21"
                + System.getProperty("line.separator")
                + "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
        Assert.assertEquals(tle.toString(), expectedString);
    }

    @Test
    public void testSatCodeCompliance() throws IOException, PatriusException {
        BufferedReader rEntry = null;
        BufferedReader rResults = null;

        final InputStream inEntry = TLETest.class
                .getResourceAsStream("/tle/extrapolationTest-data/SatCode-entry");
        rEntry = new BufferedReader(new InputStreamReader(inEntry));

        try {
            final InputStream inResults = TLETest.class
                    .getResourceAsStream("/tle/extrapolationTest-data/SatCode-results");
            rResults = new BufferedReader(new InputStreamReader(inResults));

            try {
                double cumulated = 0; // sum of all differences between test
                // cases and OREKIT results
                final boolean stop = false;

                String rline = rResults.readLine();

                while (!stop) {
                    if (rline == null) {
                        break;
                    }

                    final String[] title = rline.split(" ");

                    if (title[0].matches("r")) {

                        String eline;
                        int count = 0;
                        final String[] header = new String[4];
                        for (eline = rEntry.readLine(); (eline != null) && (eline.charAt(0) == '#'); eline = rEntry
                                .readLine()) {
                            header[count++] = eline;
                        }
                        final String line1 = eline;
                        final String line2 = rEntry.readLine();
                        Assert.assertTrue(TLE.isFormatOK(line1, line2));

                        final TLE tle = new TLE(line1, line2);

                        final int satNum = Integer.parseInt(title[1]);
                        Assert.assertTrue(satNum == tle.getSatelliteNumber());
                        final TLEPropagator ex = TLEPropagator.selectExtrapolator(tle);

                        for (rline = rResults.readLine(); (rline != null)
                                && (rline.charAt(0) != 'r'); rline = rResults.readLine()) {
                            final String[] data = rline.split(" ");
                            final double minFromStart = Double.parseDouble(data[0]);
                            final double pX = 1000 * Double.parseDouble(data[1]);
                            final double pY = 1000 * Double.parseDouble(data[2]);
                            final double pZ = 1000 * Double.parseDouble(data[3]);
                            final double vX = 1000 * Double.parseDouble(data[4]);
                            final double vY = 1000 * Double.parseDouble(data[5]);
                            final double vZ = 1000 * Double.parseDouble(data[6]);
                            final Vector3D testPos = new Vector3D(pX, pY, pZ);
                            final Vector3D testVel = new Vector3D(vX, vY, vZ);

                            final AbsoluteDate date = tle.getDate().shiftedBy(minFromStart * 60);
                            final PVCoordinates results = ex.getPVCoordinates(date);
                            final double normDifPos = testPos.subtract(results.getPosition())
                                    .getNorm();
                            final double normDifVel = testVel.subtract(results.getVelocity())
                                    .getNorm();

                            cumulated += normDifPos;

                            Assert.assertEquals(0, normDifPos, 2e-3);

                            Assert.assertEquals(0, normDifVel, 1e-5);

                        }
                    }
                }
                Assert.assertEquals(0, cumulated, 0.026);
            } finally {
                if (rResults != null) {
                    rResults.close();
                }
            }
        } finally {
            if (rEntry != null) {
                rEntry.close();
            }
        }
    }

    @Test
    public void testZeroInclination() throws PatriusException {
        final TLE tle = new TLE(
                "1 26451U 00043A   10130.13784012 -.00000276  00000-0  10000-3 0  3866",
                "2 26451 000.0000 266.1044 0001893 160.7642 152.5985 01.00271160 35865");
        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        final PVCoordinates pv = propagator.propagate(tle.getDate().shiftedBy(100))
                .getPVCoordinates();
        Assert.assertEquals(42171546.979560345, pv.getPosition().getNorm(), 1.0e-3);
        Assert.assertEquals(3074.1890089357994, pv.getVelocity().getNorm(), 1.0e-6);
    }

    @Test
    public void testSerialization() {

        try {
            // build several TLE
            final TLE tle1 = new TLE(
                    "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    21",
                    "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");

            final TLE tle2 = new TLE(
                    "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
                    "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");

            final TLE tle3 = new TLE(
                    "1 31928U 98067BA  08269.84884916  .00114257  17652-4  13615-3 0  4412",
                    "2 31928  51.6257 175.4142 0001703  41.9031 318.2112 16.08175249 68368");

            final TLE tle4 = new TLE(
                    "1 00001U 00001A   12026.45833333 2.94600864  39565-9  16165-7 1    12",
                    "2 00001 627.0796 454.4522 0000000 624.9662   0.4817  0.00000000    12");

            final TLE tle5 = new TLE(
                    "1 05555U 71086J   12026.96078249 -.00000004  00001-9  01234-9 0  9082",
                    "2 05555  74.0161 228.9750 0075476 328.9888  30.6709 12.26882470804545");

            final TLE tle6 = new TLE(
                    "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    21",
                    "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");

            final TLE tle7 = new TLE(
                    "1 26451U 00043A   10130.13784012 -.00000276  00000-0  10000-3 0  3866",
                    "2 26451 000.0000 266.1044 0001893 160.7642 152.5985 01.00271160 35865");

            final TLE[] TLEs = { tle1, tle2, tle3, tle4, tle5, tle6, tle7 };

            // serialization and de-serialization and test equality
            for (final TLE tle : TLEs) {
                final TLE tlebis = TestUtils.serializeAndRecover(tle);
                assertEqualsTLE(tle, tlebis);
                Assert.assertTrue(tle.equals(tlebis));
                Assert.assertEquals(tle.hashCode(), tlebis.hashCode());

            }

        } catch (final PatriusException e) {
            Assert.fail();
        }

    }

    @Test
    public void testEquals() {

        TLE tle1;
        try {
            tle1 = new TLE(
                    "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    21",
                    "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
            
            TLE tle2 = new TLE("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    21",
                    "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");

            Assert.assertTrue(tle1.equals(tle1));
            Assert.assertTrue(tle1.equals(tle2));
            Assert.assertEquals(tle1.hashCode(), tle2.hashCode());

            tle2 = new TLE("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    51",
                    "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");

            Assert.assertFalse(tle1.equals(tle2));
            Assert.assertFalse(tle1.hashCode() == tle2.hashCode());
            Assert.assertFalse(tle1.equals(null));
            
        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    private static void assertEqualsTLE(final TLE tle1, final TLE tle2) {
        Assert.assertEquals(tle1.getSatelliteNumber(), tle2.getSatelliteNumber());
        Assert.assertEquals(tle1.getClassification(), tle2.getClassification());
        Assert.assertEquals(tle1.getLaunchYear(), tle2.getLaunchYear());
        Assert.assertEquals(tle1.getLaunchNumber(), tle2.getLaunchNumber());
        Assert.assertEquals(tle1.getEphemerisType(), tle2.getEphemerisType());
        Assert.assertEquals(tle1.getElementNumber(), tle2.getElementNumber());
        Assert.assertEquals(tle1.getMeanMotion(), tle2.getMeanMotion(), 0);
        Assert.assertEquals(tle1.getMeanMotionFirstDerivative(),
                tle2.getMeanMotionFirstDerivative(), 0);
        Assert.assertEquals(tle1.getMeanMotionSecondDerivative(),
                tle2.getMeanMotionSecondDerivative(), 0);
        Assert.assertEquals(tle1.getE(), tle2.getE(), 0);
        Assert.assertEquals(tle1.getDate(), tle2.getDate());
        Assert.assertEquals(tle1.getI(), tle2.getI(), 0);
        Assert.assertEquals(tle1.getPerigeeArgument(), tle2.getPerigeeArgument(), 0);
        Assert.assertEquals(tle1.getRaan(), tle2.getRaan(), 0);
        Assert.assertEquals(tle1.getMeanAnomaly(), tle2.getMeanAnomaly(), 0);
        Assert.assertEquals(tle1.getRevolutionNumberAtEpoch(), tle2.getRevolutionNumberAtEpoch());
        Assert.assertEquals(tle1.getBStar(), tle2.getBStar(), 0);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
