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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.exception;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Test for {@link MathIllegalStateException}, for code coverage only.
 * 
 * @version $Id: MathIllegalStateExceptionTest.java 17909 2017-09-11 11:57:36Z bignon $
 * @since 1.3
 */
public class MathIllegalStateExceptionTest {
    /** Features description. */
    public enum features {
        /**
         * @featureTitle Code coverage
         * 
         * @featureDescription Code coverage
         * 
         * @coveredRequirements none
         */
        CODE_COVERAGE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CODE_COVERAGE}
     * 
     * @testedMethod misc
     * 
     * @description Code coverage
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria misc
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testConstructor() {
        final MathIllegalStateException e = new MathIllegalStateException(new MathArithmeticException(),
            PatriusMessages.ILLEGAL_STATE);
        Assert.assertNotNull(e);
    }
}