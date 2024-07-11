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
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff.cowell;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Integration support for Cowell integrator.
 *
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
class Support implements Externalizable {

    /** Delta wrt acceleration. */
    @SuppressWarnings("PMD.DefaultPackage")
    // Reason: code clarity
    protected double[][] deltaAcc;

    /** List of steps in the support. */
    private double[] steps;

    /** Previous support step (step off the support when shifting support forward). */
    private double previousStep;

    /** Support size. */
    private int k;

    /** Support previous size. */
    private int lk;

    /** Intermediate array: psi. */
    private double[] psi;

    /** Intermediate array: psi. */
    private double[] psin;

    /** Intermediate array: psi. */
    private double[] psinm1;

    /** Intermediate array: alpha. */
    private double[] alpha;

    /** Intermediate array: beta. */
    private double[] beta;

    /** Intermediate array: g. */
    private double[][] g;

    /** Intermediate array: g'. */
    private double[][] gp;

    /** Ratio between current step size and support last step. */
    private double ratio;

    /** Integration order. */
    private int order;

    /**
     * Empty constructor for {@link Externalizable} methods use.
     */
    public Support() {
        this(0);
    }

    /**
     * Constructor.
     * @param order integrator order
     */
    public Support(final int order) {
        this.order = order;
        k = 1;
        steps = new double[order + 1];
        psi = new double[order + 1];
        psin = new double[order];
        psinm1 = new double[order];
        alpha = new double[order + 1];
        beta = new double[order + 1];
        g = new double[order + 3][order + 2];
        gp = new double[order + 3][order + 2];
        // Later instantiated: at this point, size is unknown
        deltaAcc = new double[order + 2][];
    }

    /**
     * Copy constructor.
     * @param support support to copy
     */
    public Support(final Support support) {
        this.order = support.order;
        this.previousStep = support.previousStep;
        this.k = support.k;
        this.lk = support.lk;
        this.ratio = support.ratio;
        steps = support.steps.clone();
        psi = support.psi.clone();
        psin = support.psin.clone();
        psinm1 = support.psinm1.clone();
        alpha = support.alpha.clone();
        beta = support.beta.clone();
        g = new double[support.g.length][support.g[0].length];
        for (int i = 0; i < g.length; i++) {
            g[i] = support.g[i].clone();
        }
        gp = new double[support.gp.length][support.gp[0].length];
        for (int i = 0; i < gp.length; i++) {
            gp[i] = support.gp[i].clone();
        }
        deltaAcc = new double[support.deltaAcc.length][support.deltaAcc[0].length];
        for (int i = 0; i < deltaAcc.length; i++) {
            deltaAcc[i] = support.deltaAcc[i].clone();
        }
    }

    /**
     * Returns the support size.
     * @return the support size
     */
    protected int getSize() {
        return k;
    }
    
    /**
     * Returns the support previous size.
     * @return the support previous size
     */
    protected int getPreviousSize() {
        return lk;
    }

    /**
     * Returns the array of support steps.
     * @return the array of support steps
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    // Reason: Efficiency
    protected double[] getSteps() {
        return steps;
    }
   
    /**
     * Initialize support.
     * @param stepsize step size
     */
    protected void init(final double stepsize) {
        steps[1] = stepsize;
    }

    /**
     * Shift support one step backward.
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    // False positive
    protected void shiftBackward() {
        for (int m = k; m >= 2; m--) {
            steps[m] = steps[m - 1];
        }
        steps[1] = previousStep;
    }

    /**
     * Update support and pre-compute all intermediate arrays.
     * @param stepsize step size
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    // Reason: false positive
    protected void shiftForward(final double stepsize) {
        // Shift steps
        if (lk == order) {
            // Support already has its maximum size: shift it forward
            previousStep = steps[1];
            for (int i = 1; i <= k - 1; i++) {
                steps[i] = steps[i + 1];
            }
        }

        // Set current step of support
        steps[k] = stepsize;

        // Compute ratio of current step size to last
        ratio = stepsize / steps[k - 1];

        // Precompute arrays Psi - Alpha - Beta - G and G'
        computePsi(stepsize);
        computeAlpha(stepsize);
        computeBeta();
        computeG();
    }

    /**
     * Initialize delta acceleration.
     * @param acc acceleration
     */
    protected void initDeltaAcc(final double[] acc) {
        deltaAcc = new double[order + 2][acc.length];
        deltaAcc[1] = acc.clone();
    }

    /**
     * Update delta acceleration.
     * @param acc acceleration
     */
    protected void updateDeltaAcc(final double[] acc) {
        for (int i = 0; i < acc.length; i++) {
            deltaAcc[2][i] = acc[i] - deltaAcc[1][i];
            deltaAcc[1][i] = acc[i];
        }
    }
    
    /**
     * Compute new differences.
     * @param acc acceleration
     * @param newdiff new differences, updated by the method
     */
    protected void computeDiff(final double[] acc,
            final double[][] newdiff) {
        for (int i = 0; i < acc.length; i++) {
            newdiff[1][i] = acc[i];
            for (int m = 2; m <= k + 1; m++) {
                newdiff[m][i] = newdiff[m - 1][i] - deltaAcc[m - 1][i];
            }
        }
    }

    /**
     * Update support indices.
     */
    protected void updateIndices() {
        lk = k;
        k = MathLib.min(k + 1, order);
    }
    
    /**
     * Performs prediction step.
     * @param y0 initial state vector
     * @param yDot0 initial state vector derivative
     * @param y current state vector before/after prediction
     * @param yDot current state vector before/after prediction
     * @param previousY previous state vector
     */
    protected void prediction(final double[] y0,
            final double[] yDot0,
            final double[] y,
            final double[] yDot,
            final double[] previousY) {
        // Step size
        final double stepsize = steps[k];
        final double stepsize2 = stepsize * stepsize;
        // Loop on all y components
        for (int i = 0; i < y0.length; i++) {
            double sum2 = 0.;
            double sum1 = 0.;
            for (int m = 1; m <= k; m++) {
                // Calculate phi* = phi * beta
                deltaAcc[m][i] *= beta[m];
                sum2 += (g[2][m] + ratio * gp[2][m]) * deltaAcc[m][i];
                sum1 += g[1][m] * deltaAcc[m][i];
            }
            // Update y and yDot
            y[i] = y0[i] + ratio * (y0[i] - previousY[i]) + stepsize2 * sum2;
            yDot[i] = yDot0[i] + stepsize * sum1;
        }
    }
    
    /**
     * Performs correction step.
     * @param y current state before/after correction
     * @param yDot current state vector derivative before/after correction
     * @param acc acceleration
     * @return new differences
     */
    protected double[][] correction(final double[] y,
            final double[] yDot,
            final double[] acc) {
        final double stepsize = steps[k];
        final double stepsize2 = stepsize * stepsize;
        final double[][] newdiff = new double[order + 2][y.length];
        computeDiff(acc, newdiff);
        for (int i = 0; i < y.length; i++) {
            y[i] += stepsize2 * (g[2][k + 1] + ratio * gp[2][k + 1])
                    * newdiff[k + 1][i];
            yDot[i] += stepsize * g[1][k + 1] * newdiff[k + 1][i];
        }
        return newdiff;
    }
    
    /**
     * Compute error coefficient for y part.
     * @return error coefficient for y part
     */
    protected double computeErrorCoefficientD() {
        return MathLib.abs(steps[k] * steps[k] * (g[2][k + 1] - g[2][k] + ratio * (gp[2][k + 1] - gp[2][k])));
    }
    
    /**
     * Compute error coefficient for yDot part.
     * @return error coefficient for yDot part
     */
    protected double computeErrorCoefficientS() {
        return MathLib.abs(steps[k] * (g[1][k + 1] - g[1][k]));
    }

    /**
     * Compute sigma.
     * @return sigma
     */
    protected double computeSigma() {
        double sigma = 1.0;
        for (int i = 2; i <= order + 1; i++) {
            sigma *= (i - 1) * alpha[i - 1];
        }
        return sigma;
    }
    
    /**
     * Compute lambda.
     * @param j index
     * @return lambda
     */
    protected double computeLambda(final int j) {
        return g[2][j] + gp[2][j];
    }
    
    /**
     * Compute gamma 1.
     * @param hI interpolation step
     * @return gamma 1
     */
    protected double[] computeGamma1(final double hI) {
        final double[] gamma1 = new double[order + 1];
        gamma1[1] = hI / psi[1];
        for (int i = 2; i <= lk; i++) {
            gamma1[i] = (hI + psi[i - 1]) / psi[i];
        }
        return gamma1;
    }
    
    /**
     * Compute gamma P.
     * @param hI interpolation step
     * @return gamma P
     */
    protected double[] computeGammap(final double hI) {
        final double[] gammap = new double[order + 1];
        gammap[1] = -1.0;
        gammap[2] = 0.0;
        for (int i = 3; i <= lk; i++) {
            gammap[i] = psin[i - 2] / psi[i];
        }
        return gammap;
    }
    
    /**
     * Compute interpolation step / psi[i].
     * @param hI interpolation step
     * @param i index
     * @return interpolation step / psi[i]
     */
    protected double hIOverPsi(final double hI, final int i) {
        return hI / psi[i];
    }
    
    /**
     * Compute intermediate array: psi.
     * @param stepsize step size
     */
    private void computePsi(final double stepsize) {
        // Loop on the support
        psi[1] = stepsize;
        for (int i = 2; i <= k; i++) {
            psi[i] = psi[i - 1] + steps[k + 1 - i];
        }

        // Compute Psi n - 1
        psinm1[0] = 0.;
        for (int i = 1; i <= k - 2; i++) {
            psinm1[i] = psinm1[i - 1] + steps[k - 1 - i];
        }
        // Compute Psi n
        psin[1] = steps[k - 1];
        for (int i = 2; i <= k - 1; i++) {
            psin[i] = psin[i - 1] + steps[k - i];
        }
    }

    /**
     * Compute intermediate array: alpha.
     * @param stepsize step size
     */
    private void computeAlpha(final double stepsize) {
        // Loop on the support
        for (int i = 1; i <= k; i++) {
            alpha[i] = stepsize / psi[i];
        }
    }

    /**
     * Compute intermediate array: beta.
     */
    private void computeBeta() {
        // Loop on the support
        beta[1] = 1.;
        for (int m = 2; m <= k; m++) {
            beta[m] = beta[m - 1] * (psi[m - 1] / psin[m - 1]);
        }
    }

    /**
     * Compute intermediate arrays: g and g'.
     */
    private void computeG() {
        // Powers of ratio for computation speed-up
        final double[] powRatio = new double[k + 3];
        for (int i = 1; i < powRatio.length; i++) {
            powRatio[i] = MathLib.pow(-ratio, -i);
        }
        // Compute g and g'
        // Until order k
        for (int i = 1; i <= k + 1; i++) {
            for (int q = 1; q <= k + 3 - i; q++) {
                if (i == 1) {
                    // i = 1
                    g[q][1] = 1. / q;
                    gp[q][1] = powRatio[q] / q;
                } else if (i == 2) {
                    // i = 2
                    g[q][2] = 1. / (q * (q + 1.));
                    gp[q][2] = powRatio[q + 1] / (q * (q + 1.));
                } else {
                    // Generic case
                    g[q][i] = g[q][i - 1] - alpha[i - 1] * g[q + 1][i - 1];
                    gp[q][i] = gp[q][i - 1] * (psinm1[i - 3] / psi[i - 1]) - alpha[i - 1] * gp[q + 1][i - 1];
                }
            }
        }

        // Orders 10 and above
        for (int j = CowellIntegrator.MAX_STANDARD_ORDER + 1; j <= k; j++) {
            g[2][j + 1] = g[2][j] - alpha[j] * g[3][j];
            // Unclear formula (kmax)
            gp[2][j + 1] = psinm1[j - 2] / psi[j] * gp[2][CowellIntegrator.MAX_STANDARD_ORDER] - alpha[j] * gp[3][j];
        }
    }

    //CHECKSTYLE: stop CommentRatio check
    //CHECKSTYLE: stop CyclomaticComplexity check
    //Reason: repetitive code with generic rules

    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput oo) throws IOException {
        // Store all data in ObjectOutput
        // For arrays, store size first
        // Data is stored in the same order they are read in readExternal method
        // For arrays, length is stored first
        oo.writeInt(order);
        oo.writeInt(steps.length);
        for (int i = 0; i < steps.length; ++i) {
            oo.writeDouble(steps[i]);
        }
        oo.writeDouble(previousStep);
        oo.writeInt(k);
        oo.writeInt(lk);
        oo.writeInt(psi.length);
        for (int i = 0; i < psi.length; ++i) {
            oo.writeDouble(psi[i]);
        }
        oo.writeInt(psin.length);
        for (int i = 0; i < psin.length; ++i) {
            oo.writeDouble(psin[i]);
        }
        oo.writeInt(psinm1.length);
        for (int i = 0; i < psinm1.length; ++i) {
            oo.writeDouble(psinm1[i]);
        }
        oo.writeInt(alpha.length);
        for (int i = 0; i < alpha.length; ++i) {
            oo.writeDouble(alpha[i]);
        }
        oo.writeInt(beta.length);
        for (int i = 0; i < beta.length; ++i) {
            oo.writeDouble(beta[i]);
        }
        oo.writeInt(g.length);
        oo.writeInt(g[0].length);
        for (int i = 0; i < g.length; ++i) {
            for (int j = 0; j < g[i].length; ++j) {
                oo.writeDouble(g[i][j]);
            }
        }
        oo.writeInt(gp.length);
        oo.writeInt(gp[0].length);
        for (int i = 0; i < gp.length; ++i) {
            for (int j = 0; j < gp[i].length; ++j) {
                oo.writeDouble(gp[i][j]);
            }
        }
        oo.writeDouble(ratio);
        oo.writeInt(deltaAcc.length);
        oo.writeInt(deltaAcc[0].length);
        for (int i = 0; i < deltaAcc.length; ++i) {
            for (int j = 0; j < deltaAcc[i].length; ++j) {
                oo.writeDouble(deltaAcc[i][j]);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput oi) throws IOException, ClassNotFoundException {
        // Read all data in ObjectInput
        // For arrays, read size first
        // Data is read in the same order they are stored in writeExternal method
        // For arrays, length is read first
        order = oi.readInt();
        steps = new double[oi.readInt()];
        for (int i = 0; i < steps.length; i++) {
            steps[i] = oi.readDouble();
        }
        previousStep = oi.readDouble();
        k = oi.readInt();
        lk = oi.readInt();
        psi = new double[oi.readInt()];
        for (int i = 0; i < psi.length; i++) {
            psi[i] = oi.readDouble();
        }
        psin = new double[oi.readInt()];
        for (int i = 0; i < psin.length; i++) {
            psin[i] = oi.readDouble();
        }
        psinm1 = new double[oi.readInt()];
        for (int i = 0; i < psinm1.length; i++) {
            psinm1[i] = oi.readDouble();
        }
        alpha = new double[oi.readInt()];
        for (int i = 0; i < alpha.length; i++) {
            alpha[i] = oi.readDouble();
        }
        beta = new double[oi.readInt()];
        for (int i = 0; i < beta.length; i++) {
            beta[i] = oi.readDouble();
        }
        g = new double[oi.readInt()][oi.readInt()];
        for (int i = 0; i < g.length; i++) {
            for (int j = 0; j < g[i].length; j++) {
                g[i][j] = oi.readDouble();
            }
        }
        gp = new double[oi.readInt()][oi.readInt()];
        for (int i = 0; i < gp.length; i++) {
            for (int j = 0; j < gp[i].length; j++) {
                gp[i][j] = oi.readDouble();
            }
        }
        ratio = oi.readDouble();
        deltaAcc = new double[oi.readInt()][oi.readInt()];
        for (int i = 0; i < deltaAcc.length; i++) {
            for (int j = 0; j < deltaAcc[i].length; j++) {
                deltaAcc[i][j] = oi.readDouble();
            }
        }
    }

    //CHECKSTYLE: resume CommentRatio check
    //CHECKSTYLE: resume CyclomaticComplexity check
}
