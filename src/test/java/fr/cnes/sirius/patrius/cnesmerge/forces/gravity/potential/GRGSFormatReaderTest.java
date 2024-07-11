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
 * @history creation 02/10/2011
 *
 *
 * HISTORY
 * VERSION::FA1303:15/11/2017: Problem for high order/degree for denormalization
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::FA1303::15/11/2017: Problem for high order/degree for denormalization
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.forces.gravity.potential;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsReader;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Complimentary tests for GRGSFormatReader.
 * Should be merged with {@link fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReaderTest} eventually, EXCEPT
 * maybe for
 * tests using CNES-specific data.
 * 
 * @author cardosop
 * 
 * @version $Id: GRGSFormatReaderTest.java 18020 2017-09-29 12:16:56Z marechal $
 * 
 * @since 1.0
 * 
 * @history created 15/11/2017
 * 
 */
public class GRGSFormatReaderTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle GRGS file format compatible
         * 
         * @featureDescription Orekit can read the gravity potential GRGS file format
         * 
         * @coveredRequirements DV-MOD_180
         */
        GRAVITY_POT_GRGS_FILE_COMPAT
    }

    /** No differences expected at all : epsilon is zero. */
    private final double zeroEpsilon = 0.;

    /**
     * Name for directory data.
     */
    private static final String POTDIR = "potentialCNES";

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_GRGS_FILE_COMPAT}
     * 
     * @testedMethod {@link GRGSFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description Test for GRGSFormatReader, constants line missing error case.
     * 
     * @input file with error
     * 
     * @output OrekitException
     * 
     * @testPassCriteria OrekitException
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws IOException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws PatriusException
     *         expected exception
     */
    @Test(expected = PatriusException.class)
    public final void testCorruptedFile4() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot(POTDIR);
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_corrupted4.dat", true));
        GravityFieldFactory.getPotentialProvider();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_GRGS_FILE_COMPAT}
     * 
     * @testedMethod {@link GRGSFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description Test for GRGSFormatReader, max degree line missing error case.
     * 
     * @input file with error
     * 
     * @output OrekitException
     * 
     * @testPassCriteria OrekitException
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws IOException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws PatriusException
     *         expected exception
     */
    @Test(expected = PatriusException.class)
    public final void testCorruptedFile5() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot(POTDIR);
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_corrupted5.dat", true));
        GravityFieldFactory.getPotentialProvider();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_GRGS_FILE_COMPAT}
     * 
     * @testedMethod {@link GRGSFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description Test for GRGSFormatReader, missing data lines error case.
     * 
     * @input file with error
     * 
     * @output OrekitException
     * 
     * @testPassCriteria OrekitException
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws IOException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws PatriusException
     *         expected exception
     */
    @Test(expected = PatriusException.class)
    public final void testCorruptedFile6() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot(POTDIR);
        // Missing coefficients not allowed for code coverage reasons
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_corrupted6.dat", false));
        GravityFieldFactory.getPotentialProvider();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_GRGS_FILE_COMPAT}
     * 
     * @testedMethod {@link GRGSFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description GRGS based test for code coverage of the abstract class {@link PotentialCoefficientsReader }
     * 
     * @input the Orekit GRGS file
     * 
     * @output a properly loaded GRGSFormatReader instance
     * 
     * @testPassCriteria loading successful and data compliant
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         should not happen
     * @throws IOException
     *         should not happen
     * @throws ParseException
     *         should not happen
     */
    @Test
    public final void testForPotentialCoefficientsReader() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot(POTDIR);
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim5_C1.dat", true));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double[][] C = provider.getC(5, 5, true);

        // Minimal data test, subset from testRegular05c
        final double expectedMu = 0.3986004415E+15;
        Assert.assertEquals(expectedMu, provider.getMu(), this.zeroEpsilon);
        final double expectedAe = 0.6378136460E+07;
        Assert.assertEquals(expectedAe, provider.getAe(), this.zeroEpsilon);
        final double expectedC55 = 0.17481512311600E-06;
        Assert.assertEquals(expectedC55, C[5][5], this.zeroEpsilon);

        // Method calls for coverage of PotentialCoefficientsReader
        // through PotentialCoefficientsProvider
        // get unnormalized S
        final double[][] uS = provider.getS(5, 5, false);
        final double expectedUS55 = -0.16480043354874895E-8;
        Assert.assertEquals(expectedUS55, uS[5][5], this.zeroEpsilon);
        // get unnormalized S AGAIN (for branch coverage)
        final double[][] uS2 = provider.getS(5, 5, false);
        Assert.assertEquals(expectedUS55, uS2[5][5], this.zeroEpsilon);
        // get normalized J
        final double[] normalizedJ = provider.getJ(true, 5);
        // normalizedJ[n] should be -C[n]
        final double expectedJ5 = -.67267595149600E-07;
        Assert.assertEquals(expectedJ5, normalizedJ[5], this.zeroEpsilon);
        // get normalized J AGAIN (for branch coverage)
        final double[] normalizedJ2 = provider.getJ(true, 5);
        Assert.assertEquals(expectedJ5, normalizedJ2[5], this.zeroEpsilon);
        // get unnormalized J
        final double[] unnormalizedJ = provider.getJ(false, 5);
        final double expectedUnJ5 = -0.223101373660754E-6;
        Assert.assertEquals(expectedUnJ5, unnormalizedJ[5], this.zeroEpsilon);
        // get unnormalized J AGAIN (for branch coverage)
        final double[] unnormalizedJ2 = provider.getJ(false, 5);
        Assert.assertEquals(expectedUnJ5, unnormalizedJ2[5], this.zeroEpsilon);

        // Error cases
        // we ask for out of bounds data in getS
        boolean didErr = false;
        try {
            provider.getS(10000, 5, true);
        } catch (final PatriusException e) {
            didErr = true;
        }
        Assert.assertTrue(didErr);
        // we ask for out of bounds data in getS
        didErr = false;
        try {
            provider.getS(5, 10000, true);
        } catch (final PatriusException e) {
            didErr = true;
        }
        Assert.assertTrue(didErr);
        // we ask for out of bounds data in getJ
        didErr = false;
        try {
            provider.getJ(true, 10000);
        } catch (final PatriusException e) {
            didErr = true;
        }
        Assert.assertTrue(didErr);
    }

}
