/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 * Copyright 2019-2020 CNES
 * Copyright 2011-2014 JOptimizer
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.util;

import junit.framework.TestCase;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;

/**
 * Test class for {@link Utils} class.
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class UtilsTest extends TestCase {
    
    /**
     * Test calculateScaledResidual method when it returns 0 (norm x and norm b are 0)
     */
    public void testcalculateScaledResidual() {
        final double[][] a = new double[][] { { 1, 0, 0 }, { 2, 2, 0 }, { 3, 3, 3 } };
        final double[][] x = new double[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } };
        final double[][] b = new double[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } };
        
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final RealMatrix xMatrix = new BlockRealMatrix(x);
        final RealMatrix bMatrix = new BlockRealMatrix(b);
        
        final double scaledResidual = Utils.calculateScaledResidual(aMatrix, xMatrix, bMatrix);
        assertEquals(0.0, scaledResidual);

        final RealVector xVector = new ArrayRealVector(new double[] {0});
        final RealVector bVector = new ArrayRealVector(new double[] {0});
        final double scaledResidual2 = Utils.calculateScaledResidual(aMatrix, xVector, bVector);
        assertEquals(0.0, scaledResidual2);
    }
    
}
