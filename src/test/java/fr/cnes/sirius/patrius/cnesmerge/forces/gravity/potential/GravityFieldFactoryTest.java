/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
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
 */
package fr.cnes.sirius.patrius.cnesmerge.forces.gravity.potential;

import java.io.IOException;
import java.text.ParseException;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for <code>GravityFieldFactory</code>.
 * 
 * @author cardosop
 * 
 * @version $Id: GravityFieldFactoryTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.0
 * 
 */
public class GravityFieldFactoryTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Gravity potential factory
         * 
         * @featureDescription Orekit has a factory for reading gravity potential files
         * 
         * @coveredRequirements DV-MOD_180
         */
        GRAVITY_POT_FACTORY,
        /**
         * @featureTitle EGM file format compatible
         * 
         * @featureDescription Orekit can read the gravity potential EGM file format
         * 
         * @coveredRequirements DV-MOD_180
         */
        GRAVITY_POT_EGM_FILE_COMPAT
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_EGM_FILE_COMPAT}
     * 
     * @testedMethod {@link GravityFieldFactory#addDefaultPotentialCoefficientsReaders()}
     * 
     * @description unit test for the default factory initialization
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria call to <code>addDefaultPotentialCoefficientsReaders</code> succeeds
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws IOException
     *         should not happen
     */
    @Test
    public final void testAddDefaultPotentialCoefficientsReaders() throws IOException, ParseException, PatriusException {
        // removes any existing reader, so that we can recreate them
        GravityFieldFactory.clearPotentialCoefficientsReaders();
        // sets a default data root
        Utils.setDataRoot("potentialCNES/shm-format");
        // creates the default potential readers
        GravityFieldFactory.addDefaultPotentialCoefficientsReaders();
        // checks that at least one of the default providers work
        final PotentialCoefficientsProvider pcp = GravityFieldFactory.getPotentialProvider();
        Assert.assertNotNull(pcp);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITY_POT_FACTORY}
     * 
     * @testedMethod {@link GravityFieldFactory#getPotentialProvider()}
     * 
     * @description unit test for error case : call of <code>getPotentialProvider</code> with no data available
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria call to <code>getPotentialProvider</code> fails
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         should happen
     * @throws ParseException
     *         should not happen
     * @throws IOException
     *         should not happen
     */
    @Test(expected = PatriusException.class)
    public final void testErrorCase() throws IOException, ParseException, PatriusException {
        // removes any existing reader, so that we can recreate them
        GravityFieldFactory.clearPotentialCoefficientsReaders();
        // sets a wrong data root, with no gravity potential data
        Utils.setDataRoot("empty-data");
        GravityFieldFactory.getPotentialProvider();
    }

}
