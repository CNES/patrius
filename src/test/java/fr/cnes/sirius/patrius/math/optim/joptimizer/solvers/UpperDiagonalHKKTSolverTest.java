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

package fr.cnes.sirius.patrius.math.optim.joptimizer.solvers;

import junit.framework.TestCase;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link UpperDiagonalHKKTSolver}.
 * 
 * @author bdalfoferrer
 */
public class UpperDiagonalHKKTSolverTest extends TestCase {

    /**
     * Test solveAugmentedKKT
     * with null matrix A -> it throws an exception
     * 
     * @throws IllegalArgumentException
     */
    public void testNullMatrixA() throws PatriusException {
        final UpperDiagonalHKKTSolver solver = new UpperDiagonalHKKTSolver(1);
        
        try{
            solver.solveAugmentedKKT();
        }catch (IllegalStateException e) {
            assertTrue(true);//ok, A is null
            return;
        }
        fail();
    }
}
