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
package fr.cnes.sirius.patrius.math.optim.joptimizer.algebra;

import junit.framework.TestCase;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.CholeskyDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.optim.joptimizer.util.Utils;
import fr.cnes.sirius.patrius.math.stat.descriptive.DescriptiveStatistics;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * tests Commons-Math CholeskyDecomposition
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class CholeskyDecompositionTest extends TestCase {

    /**
     * good decomposition.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testDecomposition1() throws PatriusException {
        final RealMatrix p1 = new Array2DRowRealMatrix(new double[][] {
                { 8.08073550734687, 1.59028724315583 }, { 1.59028724315583, 0.3250861184011492 } });
        final CholeskyDecomposition cFact1 = new CholeskyDecomposition(p1);
        // check L.LT-Q=0
        final RealMatrix p1Inv = cFact1.getL().multiply(cFact1.getLT());
        final double norm1 = p1Inv.subtract(p1).getNorm();
        assertTrue(norm1 < 1.E-12);
    }

    /**
     * poor decomposition.
     * rescaling can help in doing it better
     * 
     * @throws PatriusException if an error occurs
     */
    public void testDecomposition2() throws PatriusException {
        final RealMatrix p1 = new Array2DRowRealMatrix(new double[][] {
                { 8.185301256666552E9, 1.5977225251367908E9 },
                { 1.5977225251367908E9, 3.118660129093004E8 } });
        final CholeskyDecomposition cFact1 = new CholeskyDecomposition(p1);
        // check L.LT-Q=0
        final double norm1 = cFact1.getL().multiply(cFact1.getLT()).subtract(p1).getNorm();
        assertTrue(norm1 < 1.E-5);

        // poor precision, try to make it better

        // geometric eigenvalues mean
        final DescriptiveStatistics ds = new DescriptiveStatistics(new double[] { 8.5E9, 0.00572 });
        final RealMatrix p2 = p1.scalarMultiply(1. / ds.getGeometricMean());
        final CholeskyDecomposition cFact2 = new CholeskyDecomposition(p2);
        // check L.LT-Q=0
        final double norm2 = cFact2.getL().multiply(cFact2.getLT()).subtract(p2).getNorm();
        assertTrue(norm2 < Utils.getDoubleMachineEpsilon());
    }
}
