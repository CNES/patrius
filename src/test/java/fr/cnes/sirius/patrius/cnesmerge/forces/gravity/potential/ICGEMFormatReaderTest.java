/**
 * 
 * Copyright 2011-2022 CNES
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.forces.gravity.potential;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.ICGEMFormatReader;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Complimentary tests for ICGEMFormatReader.
 * Should be merged with {@link fr.cnes.sirius.patrius.forces.gravity.potential.ICGEMFormatReaderTest} eventually,
 * EXCEPT maybe for
 * tests using CNES-specific data.
 * 
 * @author cardosop
 * 
 * @version $Id: ICGEMFormatReaderTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.0
 * 
 */
public class ICGEMFormatReaderTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle ICGEM file format compatible
         * 
         * @featureDescription Orekit can read the gravity potential ICGEM file format
         * 
         * @coveredRequirements DV-MOD_180
         */
        GRAVITY_POT_ICGEM_FILE_COMPAT
    }

    /**
     * Name for directory data.
     */
    private static final String POTDIR = "potentialCNES";

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_ICGEM_FILE_COMPAT}
     * 
     * @testedMethod {@link ICGEMFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description Test for ICGEMFormatReader, unsupported product_type error case.
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
    public void testCorruptedFile3() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot(POTDIR);
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_corrupted3_coef", true));
        GravityFieldFactory.getPotentialProvider();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_ICGEM_FILE_COMPAT}
     * 
     * @testedMethod {@link ICGEMFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description Test for ICGEMFormatReader, unsupported normalization error case.
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
    public void testCorruptedFile4() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot(POTDIR);
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_corrupted4_coef", true));
        GravityFieldFactory.getPotentialProvider();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_ICGEM_FILE_COMPAT}
     * 
     * @testedMethod {@link ICGEMFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description Test for ICGEMFormatReader, missing gravity constant and radius error case.
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
    public void testCorruptedFile5() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot(POTDIR);
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_corrupted5_coef", true));
        GravityFieldFactory.getPotentialProvider();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_ICGEM_FILE_COMPAT}
     * 
     * @testedMethod {@link ICGEMFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description Test for ICGEMFormatReader, unparseable fields error case.
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
    public void testCorruptedFile6() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot(POTDIR);
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_corrupted6_coef", true));
        GravityFieldFactory.getPotentialProvider();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_ICGEM_FILE_COMPAT}
     * 
     * @testedMethod {@link ICGEMFormatReader#loadData(java.io.InputStream, String)}
     * 
     * @description Test for ICGEMFormatReader, unparseable lines error case.
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
    public void testCorruptedFile7() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot(POTDIR);
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_corrupted7_coef", true));
        GravityFieldFactory.getPotentialProvider();
    }

}
