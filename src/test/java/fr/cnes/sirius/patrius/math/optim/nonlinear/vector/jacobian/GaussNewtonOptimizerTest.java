/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian;

import java.io.IOException;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.SimpleVectorValueChecker;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Target;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Weight;

/**
 * <p>
 * Some of the unit tests are re-implementations of the MINPACK <a
 * href="http://www.netlib.org/minpack/ex/file17">file17</a> and <a
 * href="http://www.netlib.org/minpack/ex/file22">file22</a> test files. The redistribution policy for MINPACK is
 * available <a href="http://www.netlib.org/minpack/disclaimer">here</a>, for convenience, it is reproduced below.
 * </p>
 * 
 * <table border="0" width="80%" cellpadding="10" align="center" bgcolor="#E0E0E0">
 * <tr>
 * <td>
 * Minpack Copyright Notice (1999) University of Chicago. All rights reserved</td>
 * </tr>
 * <tr>
 * <td>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * <ol>
 * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.</li>
 * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
 * <li>The end-user documentation included with the redistribution, if any, must include the following acknowledgment:
 * <code>This product includes software developed by the University of
 *           Chicago, as Operator of Argonne National Laboratory.</code> Alternately, this acknowledgment may appear in
 * the software itself, if and wherever such third-party acknowledgments normally appear.</li>
 * <li><strong>WARRANTY DISCLAIMER. THE SOFTWARE IS SUPPLIED "AS IS" WITHOUT WARRANTY OF ANY KIND. THE COPYRIGHT HOLDER,
 * THE UNITED STATES, THE UNITED STATES DEPARTMENT OF ENERGY, AND THEIR EMPLOYEES: (1) DISCLAIM ANY WARRANTIES, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * TITLE OR NON-INFRINGEMENT, (2) DO NOT ASSUME ANY LEGAL LIABILITY OR RESPONSIBILITY FOR THE ACCURACY, COMPLETENESS, OR
 * USEFULNESS OF THE SOFTWARE, (3) DO NOT REPRESENT THAT USE OF THE SOFTWARE WOULD NOT INFRINGE PRIVATELY OWNED RIGHTS,
 * (4) DO NOT WARRANT THAT THE SOFTWARE WILL FUNCTION UNINTERRUPTED, THAT IT IS ERROR-FREE OR THAT ANY ERRORS WILL BE
 * CORRECTED.</strong></li>
 * <li><strong>LIMITATION OF LIABILITY. IN NO EVENT WILL THE COPYRIGHT HOLDER, THE UNITED STATES, THE UNITED STATES
 * DEPARTMENT OF ENERGY, OR THEIR EMPLOYEES: BE LIABLE FOR ANY INDIRECT, INCIDENTAL, CONSEQUENTIAL, SPECIAL OR PUNITIVE
 * DAMAGES OF ANY KIND OR NATURE, INCLUDING BUT NOT LIMITED TO LOSS OF PROFITS OR LOSS OF DATA, FOR ANY REASON
 * WHATSOEVER, WHETHER SUCH LIABILITY IS ASSERTED ON THE BASIS OF CONTRACT, TORT (INCLUDING NEGLIGENCE OR STRICT
 * LIABILITY), OR OTHERWISE, EVEN IF ANY OF SAID PARTIES HAS BEEN WARNED OF THE POSSIBILITY OF SUCH LOSS OR
 * DAMAGES.</strong></li>
 * <ol></td>
 * </tr>
 * </table>
 * 
 * @author Argonne National Laboratory. MINPACK project. March 1980 (original fortran minpack tests)
 * @author Burton S. Garbow (original fortran minpack tests)
 * @author Kenneth E. Hillstrom (original fortran minpack tests)
 * @author Jorge J. More (original fortran minpack tests)
 * @author Luc Maisonobe (non-minpack tests and minpack tests Java translation)
 */
public class GaussNewtonOptimizerTest
    extends AbstractLeastSquaresOptimizerAbstractTest {

    @Override
    public AbstractLeastSquaresOptimizer createOptimizer() {
        return new GaussNewtonOptimizer(new SimpleVectorValueChecker(1.0e-6, 1.0e-6));
    }

    @Override
    @Test(expected = ConvergenceException.class)
    public void testMoreEstimatedParametersSimple() {
        /*
         * Exception is expected with this optimizer
         */
        super.testMoreEstimatedParametersSimple();
    }

    @Override
    @Test(expected = ConvergenceException.class)
    public void testMoreEstimatedParametersUnsorted() {
        /*
         * Exception is expected with this optimizer
         */
        super.testMoreEstimatedParametersUnsorted();
    }

    @Test(expected = TooManyEvaluationsException.class)
    public void testMaxEvaluations() throws Exception {
        final CircleVectorial circle = new CircleVectorial();
        circle.addPoint(30.0, 68.0);
        circle.addPoint(50.0, -6.0);
        circle.addPoint(110.0, -20.0);
        circle.addPoint(35.0, 15.0);
        circle.addPoint(45.0, 97.0);

        final GaussNewtonOptimizer optimizer = new GaussNewtonOptimizer(new SimpleVectorValueChecker(1e-30, 1e-30));

        optimizer.optimize(new MaxEval(100),
            circle.getModelFunction(),
            circle.getModelFunctionJacobian(),
            new Target(new double[] { 0, 0, 0, 0, 0 }),
            new Weight(new double[] { 1, 1, 1, 1, 1 }),
            new InitialGuess(new double[] { 98.680, 47.345 }));
    }

    @Override
    @Test(expected = ConvergenceException.class)
    public void testCircleFittingBadInit() {
        /*
         * This test does not converge with this optimizer.
         */
        super.testCircleFittingBadInit();
    }

    @Override
    @Test(expected = ConvergenceException.class)
    public void testHahn1()
                           throws IOException {
        super.testHahn1();
    }
}
