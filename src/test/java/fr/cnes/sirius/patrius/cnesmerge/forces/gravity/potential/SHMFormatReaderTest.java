/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.cnesmerge.forces.gravity.potential;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.gravity.potential.SHMFormatReader;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Complimentary tests for SHMFormatReader.
 * Should be merged with {@link fr.cnes.sirius.patrius.forces.gravity.potential.SHMFormatReaderTest} eventually, EXCEPT
 * maybe for
 * tests using CNES-specific data.
 * 
 * @author cardosop
 * 
 * @version $Id: SHMFormatReaderTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.0
 * 
 */
public class SHMFormatReaderTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle SHM file format compatible
         * 
         * @featureDescription Orekit can read the gravity potential SHM file format
         * 
         * @coveredRequirements DV-MOD_180
         */
        GRAVITY_POT_SHM_FILE_COMPAT
    }

    /**
     * Name for directory data.
     */
    private static final String POTCNES = "potentialCNES";

    /** No differences expected at all : epsilon is zero. */
    private final double zeroEpsilon = 0.;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_SHM_FILE_COMPAT}
     * 
     * @testedMethod {@link SHMFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description Test for SHMFormatReader, parsing of GRCOEF lines.
     * 
     * @input SMH file
     * 
     * @output data extracted from file
     * 
     * @testPassCriteria data is correct, exactly equal to the loaded data
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
     *         should not happen
     */
    @Test
    public final void testRegular03c2() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot(POTCNES);
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("eigen_cg03c_coef2", true));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double[][] C = provider.getC(5, 5, true);
        ;
        final double[][] S = provider.getS(5, 5, true);

        Assert.assertEquals(0.957201462136E-06, C[3][0], this.zeroEpsilon);
        Assert.assertEquals(0.174786174485E-06, C[5][5], this.zeroEpsilon);
        Assert.assertEquals(0, S[4][0], this.zeroEpsilon);
        Assert.assertEquals(0.308834784975E-06, S[4][4], this.zeroEpsilon);
        Assert.assertEquals(0.3986004415E+15, provider.getMu(), this.zeroEpsilon);
        Assert.assertEquals(0.6378136460E+07, provider.getAe(), this.zeroEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_SHM_FILE_COMPAT}
     * 
     * @testedMethod {@link SHMFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description Test for SHMFormatReader, CNES-generated file. NOTE : uses CNES data, it may not be possible to
     *              opensource it
     * 
     * @input file with GRCOEF lines
     * 
     * @output data extracted from file
     * 
     * @testPassCriteria data is correct, exactly equal to the loaded data
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
     *         should not happen
     */
    @Test
    public final void testGraceFile() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot(POTCNES);
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader(
            "SH_GSM-2_2003055-2005053_0730_GRGS_STAB_0001.txt", true));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double[][] C = provider.getC(5, 5, true);
        ;
        final double[][] S = provider.getS(5, 5, true);

        Assert.assertEquals(0.957217276934E-06, C[3][0], this.zeroEpsilon);
        Assert.assertEquals(0.174807506118E-06, C[5][5], this.zeroEpsilon);
        Assert.assertEquals(0, S[4][0], this.zeroEpsilon);
        Assert.assertEquals(0.308823432906E-06, S[4][4], this.zeroEpsilon);
        Assert.assertEquals(0.3986004415E+15, provider.getMu(), this.zeroEpsilon);
        Assert.assertEquals(0.6378136460E+07, provider.getAe(), this.zeroEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_SHM_FILE_COMPAT}
     * 
     * @testedMethod {@link SHMFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description Test for SHMFormatReader, parse field error case.
     * 
     * @input file with unparseable numbers
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
        Utils.setDataRoot(POTCNES);
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("eigen_corrupted4_coef", false));
        GravityFieldFactory.getPotentialProvider();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_SHM_FILE_COMPAT}
     * 
     * @testedMethod {@link SHMFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description Test for SHMFormatReader, empty file error case.
     *              Test for coverage.
     * 
     * @input empty file
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
        Utils.setDataRoot(POTCNES);
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("eigen_corrupted5_coef", false));
        GravityFieldFactory.getPotentialProvider();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_SHM_FILE_COMPAT}
     * 
     * @testedMethod {@link SHMFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description Test for SHMFormatReader, SHM line missing.
     *              Test for coverage.
     * 
     * @input file with SHM line missing
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
        Utils.setDataRoot(POTCNES);
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("eigen_corrupted6_coef", false));
        GravityFieldFactory.getPotentialProvider();
    }

}
