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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.variations.coefficients;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for VariableGravityFieldFactory.
 * 
 * @author houdroger
 * 
 * @version $Id: VariableGravityFieldFactoryTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.0
 * 
 */
public class VariableGravityFieldFactoryTest {

    private final double eps = Precision.EPSILON;

    /** Features description. */
    public enum features {

        /**
         * @featureTitle variable coefficients factory
         * 
         * @featureDescription test the variable coefficients factory methods
         * 
         * @coveredRequirements DV-MOD_190, DV-MOD_220, DV-MOD_230
         */
        VARIABLE_COEFFICIENTS_FACTORY
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS_FACTORY}
     * 
     * @testedMethod {@link VariableGravityFieldFactory#getVariablePotentialProvider()}
     * @testedMethod {@link VariableGravityFieldFactory#clearVariablePotentialCoefficientsReaders()}
     * @testedMethod {@link VariableGravityFieldFactory#addVariablePotentialCoefficientsReader(VariablePotentialCoefficientsReader)}
     * @testedMethod {@link VariableGravityFieldFactory#addDefaultVariablePotentialCoefficientsReaders()}
     * 
     * @description test inner class comparisons
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria expected order
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetVariablePotentialProvider() throws IOException, ParseException, PatriusException {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot("variablePotential");
        final VariablePotentialCoefficientsProvider p1 = VariableGravityFieldFactory.getVariablePotentialProvider();

        VariableGravityFieldFactory.clearVariablePotentialCoefficientsReaders();

        VariableGravityFieldFactory.addVariablePotentialCoefficientsReader(new GRGSRL02FormatReader(
            VariableGravityFieldFactory.GRGSRL02_FILENAME));
        VariablePotentialCoefficientsProvider p2 = VariableGravityFieldFactory.getVariablePotentialProvider();

        VariableGravityFieldFactory.clearVariablePotentialCoefficientsReaders();

        p2 = VariableGravityFieldFactory.getVariablePotentialProvider();

        for (final Entry<Integer, Map<Integer, VariablePotentialCoefficientsSet>> map : p1.getData().entrySet()) {
            final Map<Integer, VariablePotentialCoefficientsSet> other = p2.getData().get(map.getKey());

            for (final Entry<Integer, VariablePotentialCoefficientsSet> submapEntry : map.getValue().entrySet()) {
                final VariablePotentialCoefficientsSet otherSubmapEntry = other.get(submapEntry.getKey());

                Assert.assertEquals(submapEntry.getValue().getC(), otherSubmapEntry.getC(), this.eps);
                Assert.assertEquals(submapEntry.getValue().getS(), otherSubmapEntry.getS(), this.eps);
                Assert.assertEquals(submapEntry.getValue().getDegree(), otherSubmapEntry.getDegree(), this.eps);
                Assert.assertEquals(submapEntry.getValue().getOrder(), otherSubmapEntry.getOrder(), this.eps);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS_FACTORY}
     * 
     * @testedMethod {@link VariableGravityFieldFactory#getVariablePotentialProvider()}
     * @testedMethod {@link VariableGravityFieldFactory#clearVariablePotentialCoefficientsReaders()}
     * 
     * @description test the clear variable potential provider method
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria the inner potential provider in null after deleting it
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testNoData() throws IOException, ParseException, PatriusException {
        try {
            CNESUtils.clearNewFactoriesAndCallSetDataRoot("badData");
            VariableGravityFieldFactory.getVariablePotentialProvider();
            Assert.fail();
        } catch (final Exception e) {
            // expected!
        }
    }

}
