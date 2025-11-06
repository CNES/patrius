/**
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
 * 
 * @history creation 21/01/2013
 *
 * HISTORY
 * VERSION:4.15:OPENFD-221:21/11/2024:[STELA-PATRIUS] Interpolateur STELA précis
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:592:07/04/2016: Javadoc improvement
 * VERSION::DM:684:27/03/2018:add 2nd order RK6 interpolator
 * VERSION::FA:1774:22/10/2018: Javadoc correction
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.FastMath;

/**
 * A 6th order Runge-Kutta Integrators
 * <p>
 * Implementation of a sixth order Runge-Kutta integrator for STELA.
 * <p>
 * Butcher array :
 * 
 * <pre>
 *     0  |     0        0        0        0        0        0        0
 *    1/3 |    1/3       0        0        0        0        0        0
 *    2/3 |     0       2/3       0        0        0        0        0
 *    1/3 |    1/12     1/3     -1/12      0        0        0        0
 *    5/6 |   25/48   -55/24    35/48    15/8       0        0        0
 *    1/6 |    3/20   -11/24    -1/8      1/2      1/10      0        0
 *     1  | -261/260   33/13    43/156 -118/39    32/195   80/39      0
 *        |----------------------------------------------------------------
 *        |   13/200     0      11/40    11/40     4/25     4/25    13/200
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * <b>Warning:</b> This interpolator currently performs a 2nd order interpolation issued from article <i>Dense output
 * for strong stability preserving Runge–Kutta methods, D. Ketcheson, 2016</i>. <br/>
 * Accuracy is however below 1m for standard timestep.
 * </p>
 * 
 * @see RungeKuttaIntegrator
 * 
 * @concurrency not thread-safe
 * 
 * @author Cedric Dental
 * 
 * @version 1.3
 * 
 * @since 1.3
 * 
 */
// CHECKSTYLE: stop MagicNumber check
public class RungeKutta6Integrator extends RungeKuttaIntegrator {

    /** serialVersionUID */
    private static final long serialVersionUID = 7063326024096291802L;

    /** Internal weights Butcher array. */
    private static final double[][] STATIC_A = { { 1. / 3. }, { 0., 2. / 3. }, { 1. / 12., 1. / 3., -1. / 12. },
        { 25. / 48., -55. / 24., 35. / 48., 15. / 8. }, { 3. / 20., -11. / 24., -1. / 8., 1. / 2., 1. / 10. },
        { -261. / 260., 33. / 13., 43. / 156., -118. / 39., 32. / 195., 80. / 39. } };
    
    /** Full internal weights Butcher array (RK6). */
    private static final double[][] STATIC_A_FULL = {
        { 0.0 }, { 1. / 3., 0.0 }, { 0., 2. / 3., 0.0 },
        { 1. / 12., 1. / 3., -1. / 12., 0.0 },
        { 25. / 48., -55. / 24., 35. / 48., 15. / 8., 0.0 },
        { 3. / 20., -11. / 24., -1. / 8., 1. / 2., 1. / 10., 0.0 },
        { -261. / 260., 33. / 13., 43. / 156., -118. / 39., 32. / 195., 80. / 39., 0.0 } };

    /** Propagation weights Butcher array. */
    private static final double[] STATIC_B = { 13. / 200., 0., 11. / 40., 11. / 40., 4. / 25., 4. / 25., 13. / 200. };

    /** Time steps Butcher array. */
    private static final double[] STATIC_C = { 1. / 3., 2. / 3., 1. / 3., 5. / 6., 1. / 6., 1.0 };
    /**
     * Simple constructor.
     * Build a sixth-order Runge-Kutta integrator with the given
     * step.
     * 
     * @param step
     *        integration step
     */
    public RungeKutta6Integrator(final double step) {
        super("Runge-Kutta 6", STATIC_C, STATIC_A, STATIC_B,
            buildRK6StepInterpolator(STATIC_A_FULL, STATIC_B.length, 6), step);
    }
    
    /**
     * Build a Runge-Kutta step interpolator of order interpolatorOrder,
     * based on the integrator data.
     * Note that the required configuration may have no solution.
     * @param a Runge Kutta A butcher array
     * @param stagesNumber integrator stages number
     * @param interpolatorOrder interpolator order
     * @return Runge-Kutta step interpolator of required order
     */
    public static RungeKutta6StepInterpolator buildRK6StepInterpolator(final double[][] a,
            final int stagesNumber, final int interpolatorOrder) {
        
     // Build combinations for required interpolator order
        final List<Tree[]> trees = buildCombinations(interpolatorOrder);
        // Compute number of trees for this combination
        final int treeNumber = card(trees);
        
        // Build Phi
        final double[][] phi = new double[treeNumber][stagesNumber];
        int row = 0;
        for (int i = 0; i < trees.size(); i++) {
            for (int j = 0; j < trees.get(i).length; j++) {
                for (int k = 0; k < phi[0].length; k++) {
                    phi[row][k] = phi(trees.get(i)[j], k, a);
                }
                row++;
            }
        }
        final BlockRealMatrix phiMatrix = new BlockRealMatrix(phi);

        // Build Gamma
        final double[][] gamma = new double[treeNumber][interpolatorOrder];
        row = 0;
        for (int i = 0; i < trees.size(); i++) {
            final int order = i;
            for (int j = 0; j < trees.get(i).length; j++) {
                final Tree tree = trees.get(i)[j];
                // Gamma is directly stored as 1 / gamma
                gamma[row][order] = 1. / tree.gamma;
                row++;
            }
        }
        final BlockRealMatrix gammaMatrix = new BlockRealMatrix(gamma);

        // Compute polynomial terms (matrix inversion)
        // Built matrix if not singular is well-conditioned
        final RealMatrix bMatrix = new QRDecomposition(phiMatrix).getSolver().solve(gammaMatrix);
        
        // Build interpolator
        return new RungeKutta6StepInterpolator(bMatrix.getData());
        
    }
    
    /**
     * Build all necessary tree combinations.
     * @param interpolatorOrder interpolator order
     * @return all necessary tree combinations
     */
    private static List<Tree[]> buildCombinations(final int interpolatorOrder) {
        // Currently until order 6
        final List<Tree[]> res = new ArrayList<Tree[]>();
        
        // First order
        if (interpolatorOrder >= 1) {
            // Taken from Shampine book
            final Tree[] trees1 = { new Tree(null, 1, 1) };
            res.add(trees1);
        }

        // Second order
        if (interpolatorOrder >= 2) {
            // Taken from Shampine book
            final int[][] indices21 = { { 0, 1} };
            final Tree[] trees2 = { new Tree(indices21, 2, 2) };
            res.add(trees2);
        }

        // Third order
        if (interpolatorOrder >= 3) {
            // Taken from Shampine book
            final int[][] indices31 = { { 0, 1}, { 0, 2} };
            final int[][] indices32 = { { 0, 1}, { 1, 2} };
            final Tree[] trees3 = { new Tree(indices31, 3, 3), new Tree(indices32, 6, 3) };
            res.add(trees3);
        }

        // Fourth order
        if (interpolatorOrder >= 4) {
            // Taken from Shampine book
            final int[][] indices41 = { { 0, 1}, { 0, 2}, { 0, 3} };
            final int[][] indices42 = { { 0, 1}, { 0, 2}, { 2, 3} };
            final int[][] indices43 = { { 0, 1}, { 1, 2}, { 1, 3} };
            final int[][] indices44 = { { 0, 1}, { 1, 2}, { 2, 3} };
            final Tree[] trees4 = { new Tree(indices41, 4, 4), new Tree(indices42, 8, 4),
                new Tree(indices43, 12, 4), new Tree(indices44, 24, 4) };
            res.add(trees4);
        }

        // Fifth order
        if (interpolatorOrder >= 5) {
            // Taken from Shampine book
            final int[][] indices51 = { { 0, 1}, { 0, 2}, { 0, 3}, { 0, 4} };
            final int[][] indices52 = { { 0, 1}, { 0, 2}, { 0, 3}, { 3, 4} };
            final int[][] indices53 = { { 0, 1}, { 0, 2}, { 2, 3}, { 2, 4} };
            final int[][] indices54 = { { 0, 1}, { 0, 2}, { 2, 3}, { 3, 4} };
            final int[][] indices55 = { { 0, 1}, { 1, 2}, { 0, 3}, { 3, 4} };
            final int[][] indices56 = { { 0, 1}, { 1, 2}, { 1, 3}, { 1, 4} };
            final int[][] indices57 = { { 0, 1}, { 1, 2}, { 1, 3}, { 3, 4} };
            final int[][] indices58 = { { 0, 1}, { 1, 2}, { 2, 3}, { 2, 4} };
            final int[][] indices59 = { { 0, 1}, { 1, 2}, { 2, 3}, { 3, 4} };
            final Tree[] trees5 = { new Tree(indices51, 5, 5), new Tree(indices52, 10, 5),
                new Tree(indices53, 15, 5), new Tree(indices54, 30, 5),
                new Tree(indices55, 20, 5), new Tree(indices56, 20, 5),
                new Tree(indices57, 40, 5), new Tree(indices58, 60, 5),
                new Tree(indices59, 120, 5) };
            res.add(trees5);
        }

        // Sixth order
        // Built by hand
        if (interpolatorOrder >= 6) {
            // 5 roots legs
            final int[][] indices61 = { { 0, 1}, { 0, 2}, { 0, 3}, { 0, 4}, { 0, 5} };
            // 4 roots legs
            final int[][] indices62 = { { 0, 1}, { 0, 2}, { 0, 3}, { 0, 4}, { 4, 5} };
            // 3 roots legs
            final int[][] indices63 = { { 0, 1}, { 0, 2}, { 0, 3}, { 3, 4}, { 4, 5} };
            final int[][] indices64 = { { 0, 1}, { 0, 2}, { 0, 3}, { 3, 4}, { 3, 5} };
            final int[][] indices65 = { { 0, 1}, { 0, 2}, { 0, 3}, { 2, 4}, { 3, 5} };
            // 2 roots legs
            final int[][] indices66 = { { 0, 1}, { 0, 2}, { 2, 3}, { 3, 4}, { 4, 5} };
            final int[][] indices67 = { { 0, 1}, { 0, 2}, { 2, 3}, { 2, 4}, { 2, 5} };
            final int[][] indices68 = { { 0, 1}, { 1, 2}, { 0, 3}, { 3, 4}, { 4, 5} };
            final int[][] indices69 = { { 0, 1}, { 0, 2}, { 2, 3}, { 2, 4}, { 4, 5} };
            final int[][] indices610 = { { 0, 1}, { 0, 2}, { 2, 3}, { 3, 4}, { 3, 5} };
            final int[][] indices611 = { { 0, 1}, { 1, 2}, { 0, 3}, { 3, 4}, { 3, 5} };

            // 1 root legs
            final int[][] indices612 = { { 0, 1}, { 1, 2}, { 1, 3}, { 1, 4}, { 1, 5} };
            final int[][] indices613 = { { 0, 1}, { 1, 2}, { 1, 3}, { 1, 4}, { 4, 5} };
            final int[][] indices614 = { { 0, 1}, { 1, 2}, { 1, 3}, { 3, 4}, { 3, 5} };
            final int[][] indices615 = { { 0, 1}, { 1, 2}, { 1, 3}, { 3, 4}, { 4, 5} };
            final int[][] indices616 = { { 0, 1}, { 1, 2}, { 2, 3}, { 1, 4}, { 4, 5} };
            final int[][] indices617 = { { 0, 1}, { 1, 2}, { 2, 3}, { 2, 4}, { 2, 5} };
            final int[][] indices618 = { { 0, 1}, { 1, 2}, { 2, 3}, { 2, 4}, { 4, 5} };
            final int[][] indices619 = { { 0, 1}, { 1, 2}, { 2, 3}, { 3, 4}, { 3, 5} };
            final int[][] indices620 = { { 0, 1}, { 1, 2}, { 2, 3}, { 3, 4}, { 4, 5} };

            final Tree[] trees6 = {
                new Tree(indices61, 6, 6), new Tree(indices62, 12, 6),
                new Tree(indices63, 36, 6), new Tree(indices64, 18, 6),
                new Tree(indices65, 24, 6), new Tree(indices66, 120, 6),
                new Tree(indices67, 24, 6), new Tree(indices68, 72, 6),
                new Tree(indices69, 48, 6), new Tree(indices610, 72, 6),
                new Tree(indices611, 36, 6), new Tree(indices612, 30, 6),
                new Tree(indices613, 60, 6), new Tree(indices614, 90, 6),
                new Tree(indices615, 180, 6), new Tree(indices616, 120, 6),
                new Tree(indices617, 120, 6), new Tree(indices618, 240, 6),
                new Tree(indices619, 360, 6), new Tree(indices620, 720, 6) };
            res.add(trees6);
        }

        // Return result
        return res;
    }
    
    /**
     * Compute phi.
     * @param tree labeled tree
     * @param j column
     * @param a integrator A butcher array
     * @return phi<sub>j</sub>(t(i)), t(i) being t for corresponding i
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    private static double phi(final Tree tree, final int j, final double[][] a) {
        // Loop on all terms (k, l, m, etc.)
        double sum = 0;
        final int nbTerms = (int) FastMath.pow(a.length, tree.order - 1);
        for (int i = 0; i < nbTerms; i++) {
            // Build indices list (j not included)
            final int[] indices = new int[tree.order - 1];
            int remain = i;
            for (int k = 0; k < indices.length; k++) {
                indices[k] = remain % a.length;
                remain /= a.length;
            }
            
            try {
                // Compute current term
                double term = 1;
                for (int k = 0; k < tree.order - 1; k++) {
                    final int[] path = tree.indices[k];
                    final int x;
                    if (path[0] == 0) {
                        x = j;
                    } else {
                        x = indices[path[0] - 1];
                    }
                    final int y;
                    if (path[1] == 0) {
                        y = j;
                    } else {
                        y = indices[path[1] - 1];
                    }
                    term *= a[x][y];
                }
                // Sum
                sum += term;
            } catch (final ArrayIndexOutOfBoundsException e) {
                // Nothing to do
                // Term not computable, then not added
            }
        }
        return sum;
    }
    
    /**
     * Computes cardinal of label trees up to integrator order.
     * @param trees list of trees
     * @return total cardinal of label trees up to integrator order. 
     */
    private static int card(final List<Tree[]> trees) {
        int res = 0;
        for (int i = 0; i < trees.size(); i++) {
            res += trees.get(i).length;
        }
        return res;
    }
    
    /**
     * Labeled tree as defined in Shampine book.
     */
    @SuppressWarnings("PMD.ShortClassName")
    private static class Tree {
        
        /** Indices. */
        private final int[][] indices;
        
        /** Gamma value. */
        private final double gamma;
        
        /** Tree order. */
        private final int order;
       
        /**
         * Constructor.
         * @param indices indicies
         * @param gamma gamma
         * @param order order
         */
        public Tree(final int[][] indices, final int gamma, final int order) {
            this.indices = indices;
            this.gamma = gamma;
            this.order = order;
        }
    }

    // CHECKSTYLE: resume MagicNumber check
}
