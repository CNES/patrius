/**
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.tle;

import fr.cnes.sirius.patrius.math.analysis.MultivariateMatrixFunction;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Orbit converter for Two-Lines Elements using differential algorithm.
 * 
 * @author Rocca
 * @since 6.0
 */
public class DifferentialOrbitConverter extends AbstractTLEFitter {

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
    public DifferentialOrbitConverter(final int maxIterationsIn,
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

        // Get weights
        final double[] w = this.getWeight();
        for (int i = 0; i < w.length; ++i) {
            w[i] = MathLib.sqrt(w[i]);
        }

        // Initialization
        final MultivariateMatrixFunction jacobian = this.getPVFunction().jacobian();
        // clone the initial
        final double[] result = initial.clone();

        // initialize previousRMS at NaN
        double previousRMS = Double.NaN;
        // loop on the maximum number of iterations
        for (int iterations = 0; iterations < this.maxIterations; ++iterations) {
            // Loop until convergence
            final RealMatrix a = new Array2DRowRealMatrix(jacobian.value(result));
            for (int i = 0; i < a.getRowDimension(); i++) {
                for (int j = 0; j < a.getColumnDimension(); j++) {
                    a.multiplyEntry(i, j, w[i]);
                }
            }

            // get the residuals for result
            final double[] residuals = this.getResiduals(result);
            final RealVector y = new ArrayRealVector(residuals.length);
            for (int i = 0; i < y.getDimension(); i++) {
                y.setEntry(i, residuals[i] * w[i]);
            }
            final RealVector dx = new QRDecomposition(a).getSolver().solve(y);
            for (int i = 0; i < result.length; i++) {
                result[i] = result[i] + dx.getEntry(i);
            }

            final double rms = this.getRMS(result);
            if (iterations > 0 && MathLib.abs(rms - previousRMS) <= this.getPositionTolerance()) {
                // Return result
                return result;
            }
            previousRMS = rms;

        }

        // Exception
        // raise an exception if the maximum value is exceeded
        throw new MaxCountExceededException(this.maxIterations);

    }

}
