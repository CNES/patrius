/**
 * Copyright 2011-2017 CNES
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
* VERSION:4.4:FA:FA-2258:04/10/2019:[PATRIUS] ecriture TLE et angles negatif
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.tle;

import java.util.List;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.analysis.MultivariateMatrixFunction;
import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

// CHECKSTYLE: stop CommentRatio check
// Reason: model - Orekit code

/**
 * Abstract class for TLE/Orbit fitting.
 * <p>
 * Two-Line Elements are tightly linked to the SGP4/SDP4 propagation models. They cannot be used with other models and
 * do not represent osculating orbits. When conversion is needed, the model must be considered and conversion must be
 * done by some fitting method on a sufficient time range.
 * </p>
 * <p>
 * This base class factor the common code for such conversions. Different implementations correspond to different
 * fitting algorithms.
 * </p>
 * 
 * @author Rocca
 * @since 6.0
 */
public abstract class AbstractTLEFitter {

    /** 1000. */
    private static final double ONE_THOUSAND = 1000.;

    /** 60. */
    private static final double SIXTY = 60.;

    /** Earth gravity coefficient in m<sup>3</sup>/s<sup>2</sup>. */
    private static final double MU =
        TLEConstants.XKE * TLEConstants.XKE *
                TLEConstants.EARTH_RADIUS * TLEConstants.EARTH_RADIUS * TLEConstants.EARTH_RADIUS *
                (ONE_THOUSAND * ONE_THOUSAND * ONE_THOUSAND) / (SIXTY * SIXTY);

    /** 10000. */
    private static final int TEN_THOUSAND = 10000;

    /** 7 */
    private static final int C_7 = 7;

    /** Satellite number. */
    private final int satelliteNumber;

    /** Classification (U for unclassified). */
    private final char classification;

    /** Launch year (all digits). */
    private final int launchYear;

    /** Launch number. */
    private final int launchNumber;

    /** Launch piece. */
    private final String launchPiece;

    /** Element number. */
    private final int elementNumber;

    /** Revolution number at epoch. */
    private final int revolutionNumberAtEpoch;

    /** Auxiliary outputData: RMS of solution. */
    private double rms;

    /** Spacecraft states samples. */
    private List<SpacecraftState> sample;

    /** TEME frame. */
    private Frame teme;

    /** Desired position tolerance. */
    private double tolerance;

    /** Position use indicator. */
    private boolean onlyPosition;

    /** Function computing residuals. */
    private final ResidualsFunction pvFunction;

    /** Target position and velocities at sample points. */
    private double[] target;

    /** Weight for residuals. */
    private double[] weight;

    /** Fitted Two-Lines Elements. */
    private TLE tle;

    /**
     * Simple constructor.
     * 
     * @param satelliteNumberIn
     *        satellite number
     * @param classificationIn
     *        classification (U for unclassified)
     * @param launchYearIn
     *        launch year (all digits)
     * @param launchNumberIn
     *        launch number
     * @param launchPieceIn
     *        launch piece
     * @param elementNumberIn
     *        element number
     * @param revolutionNumberAtEpochIn
     *        revolution number at epoch
     */
    protected AbstractTLEFitter(final int satelliteNumberIn, final char classificationIn,
                                final int launchYearIn, final int launchNumberIn, final String launchPieceIn,
                                final int elementNumberIn, final int revolutionNumberAtEpochIn) {
        this.satelliteNumber = satelliteNumberIn;
        this.classification = classificationIn;
        this.launchYear = launchYearIn;
        this.launchNumber = launchNumberIn;
        this.launchPiece = launchPieceIn;
        this.elementNumber = elementNumberIn;
        this.revolutionNumberAtEpoch = revolutionNumberAtEpochIn;
        this.pvFunction = new ResidualsFunction();
    }

    /**
     * Find the TLE elements that minimize the mean square error for a sample of {@link SpacecraftState states}.
     * 
     * @param states
     *        spacecraft states sample to fit
     * @param positionTolerance
     *        desired position tolerance
     * @param positionOnly
     *        if true, consider only position data otherwise both position and
     *        velocity are used
     * @param withBStar
     *        if true, the B* coefficient must be evaluated too, otherwise
     *        it will be forced to 0
     * @return fitted TLE
     * @exception PatriusException
     *            if TLE cannot be computed
     * @exception MaxCountExceededException
     *            if maximal number of iterations is exceeded
     * @see #getTLE()
     * @see #getRMS()
     */
    public TLE toTLE(final List<SpacecraftState> states, final double positionTolerance,
                     final boolean positionOnly, final boolean withBStar) throws PatriusException {

        this.teme = FramesFactory.getTEME();
        this.setSample(states);
        this.tolerance = positionTolerance;
        this.onlyPosition = positionOnly;

        // very rough first guess using osculating parameters of first sample point
        final double[] initial = new double[withBStar ? C_7 : 6];
        final PVCoordinates pv = states.get(0).getPVCoordinates(FramesFactory.getTEME());
        initial[0] = pv.getPosition().getX();
        initial[1] = pv.getPosition().getY();
        initial[2] = pv.getPosition().getZ();
        initial[3] = pv.getVelocity().getX();
        initial[4] = pv.getVelocity().getY();
        initial[5] = pv.getVelocity().getZ();

        // warm-up iterations, using only a few points
        this.setSample(states.subList(0, this.onlyPosition ? 2 : 1));
        final double[] intermediate = this.fit(initial);

        // final search using all points
        this.setSample(states);
        final double[] result = this.fit(intermediate);

        this.rms = this.getRMS(result);
        this.tle = this.getTLE(result);
        return this.tle;

    }

    /**
     * Get the fitted Two-Lines Elements.
     * 
     * @return fitted Two-Lines Elements
     * @see #toTLE(List, double, boolean, boolean)
     */
    public TLE getTLE() {
        return this.tle;
    }

    /**
     * Get Root Mean Square of the fitting.
     * 
     * @return rms
     * @see #toTLE(List, double, boolean, boolean)
     */
    public double getRMS() {
        return this.rms;
    }

    /**
     * Find the TLE elements that minimize the mean square error for a sample of {@link SpacecraftState states}.
     * 
     * @param initial
     *        initial estimation parameters (position, velocity and B* if estimated)
     * @return fitted parameters
     * @exception PatriusException
     *            if TLE cannot be computed
     * @exception MaxCountExceededException
     *            if maximal number of iterations is exceeded
     */
    protected abstract double[] fit(double[] initial) throws PatriusException;

    /**
     * Get the TLE for a given position/velocity/B* parameters set.
     * 
     * @param parameters
     *        position/velocity/B* parameters set
     * @return TLE
     * @throws PatriusException thrown if inclination is negative
     */
    protected TLE getTLE(final double[] parameters) throws PatriusException {
        final KeplerianOrbit orb =
            new KeplerianOrbit(new PVCoordinates(new Vector3D(parameters[0], parameters[1], parameters[2]),
                new Vector3D(parameters[3], parameters[4], parameters[5])),
                this.teme, this.sample.get(0).getDate(), MU);
        return new TLE(this.satelliteNumber, this.classification, this.launchYear, this.launchNumber, this.launchPiece,
            TLE.DEFAULT, this.elementNumber, this.sample.get(0).getDate(),
            orb.getKeplerianMeanMotion(), 0.0, 0.0,
            orb.getE(), MathUtils.normalizeAngle(orb.getI(), FastMath.PI),
            MathUtils.normalizeAngle(orb.getPerigeeArgument(), FastMath.PI),
            MathUtils.normalizeAngle(orb.getRightAscensionOfAscendingNode(), FastMath.PI),
            MathUtils.normalizeAngle(orb.getMeanAnomaly(), FastMath.PI),
            this.revolutionNumberAtEpoch, (parameters.length == C_7) ? parameters[6] / TEN_THOUSAND : 0.0);

    }

    /**
     * Get the position/velocity target at sample points.
     * 
     * @return position/velocity target at sample points
     */
    protected double[] getTarget() {
        return this.target.clone();
    }

    /**
     * Get the weights for residuals.
     * 
     * @return weights for residuals
     */
    protected double[] getWeight() {
        return this.weight.clone();
    }

    /**
     * Get the residuals for a given position/velocity/B* parameters set.
     * 
     * @param parameters
     *        position/velocity/B* parameters set
     * @return residuals
     * @see #getRMS(double[])
     * @exception PatriusException
     *            if position/velocity cannot be computed at some date
     */
    protected double[] getResiduals(final double[] parameters) throws PatriusException {
        try {
            final double[] residuals = this.pvFunction.value(parameters);
            for (int i = 0; i < residuals.length; ++i) {
                residuals[i] = this.target[i] - residuals[i];
            }
            return residuals;
        } catch (final PatriusExceptionWrapper oew) {
            throw oew.getException();
        }
    }

    /**
     * Get the RMS for a given position/velocity/B* parameters set.
     * 
     * @param parameters
     *        position/velocity/B* parameters set
     * @return RMS
     * @see #getResiduals(double[])
     * @exception PatriusException
     *            if position/velocity cannot be computed at some date
     */
    protected double getRMS(final double[] parameters) throws PatriusException {

        final double[] residuals = this.getResiduals(parameters);
        double sum2 = 0;
        for (final double residual : residuals) {
            sum2 += residual * residual;
        }

        return MathLib.sqrt(sum2 / residuals.length);

    }

    /**
     * Get the function computing position/velocity at sample points.
     * 
     * @return function computing position/velocity at sample points
     */
    protected ResidualsFunction getPVFunction() {
        return this.pvFunction;
    }

    /**
     * Set the states sample.
     * 
     * @param sampleIn
     *        spacecraft states sample
     * @exception PatriusException
     *            if position/velocity cannot be extracted from sample
     */
    private void setSample(final List<SpacecraftState> sampleIn) throws PatriusException {

        // velocity weight relative to position
        final PVCoordinates pv0 = sampleIn.get(0).getPVCoordinates(this.teme);
        final double r2 = pv0.getPosition().getNormSq();
        final double v = pv0.getVelocity().getNorm();
        final double vWeight = v * r2 / MU;

        this.sample = sampleIn;

        if (this.onlyPosition) {
            this.target = new double[sampleIn.size() * 3];
            this.weight = new double[sampleIn.size() * 3];
        } else {
            this.target = new double[sampleIn.size() * 6];
            this.weight = new double[sampleIn.size() * 6];
        }

        int k = 0;
        for (int i = 0; i < sampleIn.size(); i++) {

            final PVCoordinates pv = sampleIn.get(i).getPVCoordinates(FramesFactory.getTEME());

            // position
            this.target[k] = pv.getPosition().getX();
            this.weight[k++] = 1;
            this.target[k] = pv.getPosition().getY();
            this.weight[k++] = 1;
            this.target[k] = pv.getPosition().getZ();
            this.weight[k++] = 1;

            // velocity
            if (!this.onlyPosition) {
                this.target[k] = pv.getVelocity().getX();
                this.weight[k++] = vWeight;
                this.target[k] = pv.getVelocity().getY();
                this.weight[k++] = vWeight;
                this.target[k] = pv.getVelocity().getZ();
                this.weight[k++] = vWeight;
            }

        }

    }

    /**
     * Get the desired position tolerance.
     * 
     * @return position tolerance
     */
    protected double getPositionTolerance() {
        return this.tolerance;
    }

    /** Internal class for computing position/velocity at sample points. */
    protected class ResidualsFunction implements MultivariateVectorFunction {

        /** {@inheritDoc} */
        @Override
        public double[] value(final double[] arg) {
            try {

                final TLEPropagator propagator = TLEPropagator.selectExtrapolator(AbstractTLEFitter.this.getTLE(arg));
                final double[] eval = new double[AbstractTLEFitter.this.target.length];
                int k = 0;
                for (int j = 0; j < AbstractTLEFitter.this.sample.size(); j++) {
                    final PVCoordinates pv =
                        propagator.getPVCoordinates(AbstractTLEFitter.this.sample.get(j).getDate());
                    eval[k++] = pv.getPosition().getX();
                    eval[k++] = pv.getPosition().getY();
                    eval[k++] = pv.getPosition().getZ();
                    if (!AbstractTLEFitter.this.onlyPosition) {
                        eval[k++] = pv.getVelocity().getX();
                        eval[k++] = pv.getVelocity().getY();
                        eval[k++] = pv.getVelocity().getZ();
                    }
                }

                return eval;

            } catch (final PatriusException ex) {
                throw new PatriusExceptionWrapper(ex);
            }
        }

        /**
         * Compute Jacobian.
         * 
         * @return jacobian
         */
        public MultivariateMatrixFunction jacobian() {
            return new MultivariateMatrixFunction(){

                /** {@inheritDoc} */
                @Override
                public double[][] value(final double[] arg) {
                    final double[][] jacob = new double[AbstractTLEFitter.this.target.length][arg.length];
                    final double[] eval = ResidualsFunction.this.value(arg);
                    final double[] arg1 = new double[arg.length];
                    double increment = 0;
                    for (int kappa = 0; kappa < arg.length; kappa++) {
                        System.arraycopy(arg, 0, arg1, 0, arg.length);
                        increment = MathLib.sqrt(Precision.EPSILON) * MathLib.abs(arg[kappa]);
                        if (increment <= Precision.SAFE_MIN) {
                            increment = MathLib.sqrt(Precision.EPSILON);
                        }
                        arg1[kappa] += increment;
                        final double[] eval1 = ResidualsFunction.this.value(arg1);
                        for (int t = 0; t < eval.length; t++) {
                            jacob[t][kappa] = (eval1[t] - eval[t]) / increment;
                        }
                    }

                    return jacob;
                }

            };
        }
    }

    // CHECKSTYLE: resume CommentRatio check
}
