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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.tle;

import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointVectorValuePair;
import fr.cnes.sirius.patrius.math.optim.SimpleVectorValueChecker;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunctionJacobian;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Target;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Weight;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian.LevenbergMarquardtOptimizer;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Orbit converter for Two-Lines Elements using differential algorithm.
 * 
 * @author Rocca
 * @since 6.0
 */
public class LevenbergMarquardtOrbitConverter extends AbstractTLEFitter {

    /** Maximum number of iterations for fitting. */
    private final int maxIterations;

    /**
     * Simple constructor.
     * 
     * @param maxIterationsIn
     *        maximum number of iterations for fitting
     * @param satelliteNumber
     *        satellite number
     * @param classification
     *        classification (U for unclassified)
     * @param launchYear
     *        launch year (all digits)
     * @param launchNumber
     *        launch number
     * @param launchPiece
     *        launch piece
     * @param elementNumber
     *        element number
     * @param revolutionNumberAtEpoch
     *        revolution number at epoch
     */
    public LevenbergMarquardtOrbitConverter(final int maxIterationsIn,
        final int satelliteNumber, final char classification,
        final int launchYear, final int launchNumber, final String launchPiece,
        final int elementNumber, final int revolutionNumberAtEpoch) {
        super(satelliteNumber, classification,
            launchYear, launchNumber, launchPiece, elementNumber, revolutionNumberAtEpoch);
        this.maxIterations = maxIterationsIn;
    }

    /** {@inheritDoc} */
    @Override
    protected double[] fit(final double[] initial) throws PatriusException {

        final ConvergenceChecker<PointVectorValuePair> checker =
            new SimpleVectorValueChecker(-1.0, this.getPositionTolerance());
        final LevenbergMarquardtOptimizer optimizer =
            new LevenbergMarquardtOptimizer(checker);
        final PointVectorValuePair optimum =
            optimizer.optimize(
                new MaxEval(this.maxIterations),
                new ModelFunction(this.getPVFunction()),
                new ModelFunctionJacobian(this.getPVFunction().jacobian()),
                new Target(this.getTarget()),
                new Weight(this.getWeight()),
                new InitialGuess(initial));

        return optimum.getPointRef();

    }

}
