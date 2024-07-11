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
 * @history created 18/02/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FT:63:19/08/2013: addition of comments for computation of drag at every substeps
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:317:06/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:403:20/10/2015:Improving ergonomics
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.drag;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaGaussContribution;
import fr.cnes.sirius.patrius.stela.forces.Squaring;
import fr.cnes.sirius.patrius.stela.orbits.JacobianConverter;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class representing the atmospheric drag for the Stela GTO extrapolator.
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment not thread-safe due to use of mutable attributes
 * 
 * @see StelaAeroModel
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaAtmosphericDrag extends AbstractStelaGaussContribution {

     /** Serializable UID. */
    private static final long serialVersionUID = 3496586138581780413L;

    /** The drag sensitive spacecraft. */
    private final DragSensitive spacecraft;

    /** The atmosphere. */
    private final Atmosphere atmosphere;

    /** Number of points for Simpson' squaring. */
    private final int squaringPoints;

    /** The Earth radius. */
    private final double earthRadius;

    /** Altitude of the upper atmospheric boundary. */
    private final double atmoThreshold;

    /**
     * Transition matrix computation flag. Set to true if parameters required to get the transition matrix have to be
     * computed.
     */
    private boolean transMatFlag = false;

    /**
     * dGFDE term of the transition matrix for every point of the squaring.<br>
     * First dimension corresponds to the vectorized matrix for every point of the squaring.<br>
     * Second dimension is the index of the squaring point considered.
     */
    private double[][] dGFDESquaring;

    /** Final drag term of the transition matrix. */
    private final double[][] dGFDE = new double[6][6];

    /** Drag recomputeStep */
    private final int dragRecomputeStep;

    /**
     * Constructor.
     * 
     * @param inSpacecraft
     *        the drag sensitive spacecraft.
     * @param inAtmosphere
     *        the atmospheric model.
     * @param inSquaringPoints
     *        number of points for Simpson' Squaring.
     * @param inEarthRadius
     *        the Earth radius.
     * @param inAtmoThreshold
     *        the altitude of the upper atmospheric boundary.
     * @param inDragRecomputeStep
     *        drag will be re-computed every inDragRecomputeStep steps (if value is 0, computed every substep)
     */
    public StelaAtmosphericDrag(final DragSensitive inSpacecraft, final Atmosphere inAtmosphere,
        final int inSquaringPoints, final double inEarthRadius, final double inAtmoThreshold,
        final int inDragRecomputeStep) {
        super();
        this.spacecraft = inSpacecraft;
        this.atmosphere = inAtmosphere;
        this.squaringPoints = inSquaringPoints;
        this.earthRadius = inEarthRadius;
        this.atmoThreshold = inAtmoThreshold;
        this.dragRecomputeStep = inDragRecomputeStep;
    }

    /**
     * @return the dragRecomputeStep
     */
    public int getDragRecomputeStep() {
        return this.dragRecomputeStep;
    }

    /** {@inheritDoc} */
    @Override
    public double[] computePerturbation(final StelaEquinoctialOrbit orbit,
                                        final OrbitNatureConverter converter) throws PatriusException {
        // Initialization
        final double[] result = new double[6];
        final double[][] squaringPV;
        final double[] daDt = new double[this.squaringPoints];
        final double[] dexDt = daDt.clone();
        final double[] deyDt = daDt.clone();
        final double[] dixDt = daDt.clone();
        final double[] diyDt = daDt.clone();
        final double[] dLamEqDt = daDt.clone();

        final double a = orbit.getA();
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        final double eccentricity = MathLib.sqrt(ex * ex + ey * ey);
        final Frame frame = orbit.getFrame();
        final double zp = (a * (1 - eccentricity)) - this.earthRadius;
        final double za = (a * (1 + eccentricity)) - this.earthRadius;
        if (zp <= this.atmoThreshold) {
            // At least one point of the orbit is below the upper atmospheric boundary but a part of the orbit remains
            // above

            // Computation of true and eccentric anomaly bounds of the part of the orbit inside the atmosphere
            final double[] bounds = this.computeAnomalyBounds(a, eccentricity, za);
            // True anomaly (lower bound):
            final double ve = bounds[0];
            // True anomaly (upper bound):
            final double vs = bounds[1];

            // Computation of squaring points (in mean parameters)
            squaringPV = Squaring.computeSquaringPoints(this.squaringPoints, orbit, ve, vs);

            // Drag evaluation for each squaring point (osculating parameters).
            for (int i = 0; i < this.squaringPoints; i++) {
                final AbsoluteDate squaringDate = Squaring.getSquaringJDCNES()[i];
                final StelaEquinoctialOrbit orbitMean = new StelaEquinoctialOrbit(squaringPV[i][0], squaringPV[i][2],
                    squaringPV[i][3], squaringPV[i][4], squaringPV[i][5], squaringPV[i][1], frame, squaringDate,
                    orbit.getMu());

                final StelaEquinoctialOrbit orbitOsc = converter.toOsculating(orbitMean);
                final Vector3D positionOsc = orbitOsc.getPVCoordinates().getPosition();
                final Vector3D velocityOsc = orbitOsc.getPVCoordinates().getVelocity();
                final double adjustCoef = computeAdjustCoef(orbitOsc);
                // Compute the density:
                final double density = this.atmosphere.getDensity(squaringDate, positionOsc, frame);
                // Compute the velocity:
                final Vector3D velocity = this.atmosphere.getVelocity(squaringDate, positionOsc, frame);

                // Attitude is not computed
                final SpacecraftState stateOsc = new SpacecraftState(orbitOsc);
                // Compute the drag in the MOD frame:
                final Vector3D dragMOD = this.spacecraft.dragAcceleration(stateOsc, density,
                    velocity.subtract(velocityOsc));

                // Transform the drag vector in the TNW frame:
                final LocalOrbitalFrame tnw = new LocalOrbitalFrame(frame, LOFType.TNW, orbitOsc, "TNW");
                final Transform transform = tnw.getParent().getTransformTo(tnw, squaringDate);
                final Vector3D dragTNW = transform.transformVector(dragMOD);

                // Computation of drag contribution to Gauss equations (in mean parameters):

                final double[][] gaussEq = computeGaussEquations(orbitMean);

                // Compute the transition matrix:
                if (this.transMatFlag) {
                    final double[] dGFDESquaringCurrent =
                        this.computeDGFDESquaring(orbitMean, orbitOsc, dragMOD, dragTNW,
                            gaussEq, density);
                    for (int j = 0; j < 6 * 6; j++) {
                        this.dGFDESquaring[j][i] = adjustCoef * dGFDESquaringCurrent[j];
                    }
                }

                final double[] instantGaussEq = JavaMathAdapter.matrixVectorMultiply(gaussEq, dragTNW.toArray());
                daDt[i] = adjustCoef * instantGaussEq[0];
                dLamEqDt[i] = adjustCoef * instantGaussEq[1];
                dexDt[i] = adjustCoef * instantGaussEq[2];
                deyDt[i] = adjustCoef * instantGaussEq[3];
                dixDt[i] = adjustCoef * instantGaussEq[4];
                diyDt[i] = adjustCoef * instantGaussEq[5];
            }
            // Simpson's rule
            final double deltavi = (vs - ve) / (this.squaringPoints - 1);
            result[0] = Squaring.simpsonMean(daDt, deltavi);
            result[1] = Squaring.simpsonMean(dLamEqDt, deltavi);
            result[2] = Squaring.simpsonMean(dexDt, deltavi);
            result[3] = Squaring.simpsonMean(deyDt, deltavi);
            result[4] = Squaring.simpsonMean(dixDt, deltavi);
            result[5] = Squaring.simpsonMean(diyDt, deltavi);
            if (this.transMatFlag) {
                this.computeDGFDE(deltavi);
            }
        }
        this.dPert = result;
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit) throws PatriusException {
        // not implemented yet
        return new double[6];
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) throws PatriusException {
        return this.dGFDE;
    }

    /**
     * Computes true anomaly and eccentric anomaly bounds.
     * 
     * @param a
     *        semi-major axis
     * @param e
     *        eccentricity
     * @param za
     *        apogee altitude
     * @return True anomaly and eccentric anomaly bounds.
     */
    protected double[] computeAnomalyBounds(final double a, final double e, final double za) {

        final double[] result = new double[2];
        // True anomaly (lower bound):
        final double ve;
        // True anomaly (upper bound)
        final double vs;
        final double rbarre = this.atmoThreshold + this.earthRadius;

        // Get semi-major axis and eccentricity (type 8)
        if (za > this.atmoThreshold) {
            // Temporary correction
            double alpha = MathLib.divide(a * (1. - (e * e)) / rbarre - 1., e);
            final double epsilon = 1E-12;
            if (alpha > 1. && alpha < 1. + epsilon) {
                alpha = 1.;
            }
            // A part of the orbit is below the upper atmospheric boundary
            vs = MathLib.acos(alpha);
            ve = -vs;
        } else {
            // The orbit is entirely below the upper atmospheric boundary
            ve = 0.;
            vs = 2. * FastMath.PI;
        }
        result[0] = ve;
        result[1] = vs;
        return result;
    }

    /**
     * Compute an coefficient to represent the irregular distribution of mean anomalies.
     * 
     * @param pv
     *        a Stela equinoctial orbit.
     * @return an adjustment coefficient.
     */
    private static double computeAdjustCoef(final StelaEquinoctialOrbit pv) {

        final double ex = pv.getEquinoctialEx();
        final double ey = pv.getEquinoctialEy();
        final double e2 = ex * ex + ey * ey;
        final double e = MathLib.sqrt(e2);
        final double eta = MathLib.sqrt(MathLib.max(0.0, 1.0 - e2));
        final double pomPlusRaan = JavaMathAdapter.mod(MathLib.atan2(ey, ex), 2. * FastMath.PI);
        // Eccentric anomaly
        final double eAnom = pv.kepEq(e, pv.getLM() - pomPlusRaan);
        final double ecosE = e * MathLib.cos(eAnom);
        return MathLib.divide((1.0 - ecosE) * (1.0 - ecosE), eta);
    }

    /**
     * Computation of the temporary d(GF)/dE term of the transition matrix, for the current squaring point.
     * 
     * @param orbitMean
     *        current orbit in mean parameters
     * @param orbitOsc
     *        current orbit in osculating parameters
     * @param dragMOD
     *        current atmospheric drag in the MOD frame
     * @param dragTNW
     *        current atmospheric drag in the TNW frame
     * @param gaussEq
     *        the Gauss equations for the current orbit
     * @param density
     *        the atmospheric density
     * @return the temporary array containing the d(GF)/dE term of the transition matrix for the current squaring point.
     * @throws PatriusException
     *         error when computing partial derivatives
     */
    private double[] computeDGFDESquaring(final StelaEquinoctialOrbit orbitMean, final StelaEquinoctialOrbit orbitOsc,
                                          final Vector3D dragMOD, final Vector3D dragTNW, final double[][] gaussEq,
                                          final double density) throws PatriusException {

        // Attitude is not computed: we use the default attitude law.
        final Attitude attitude = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY)
            .getAttitude(orbitMean);
        final SpacecraftState stateOsc = new SpacecraftState(orbitOsc, attitude);
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        final Vector3D vAtm =
            this.atmosphere.getVelocity(stateOsc.getDate(), stateOsc.getPVCoordinates().getPosition(),
                stateOsc.getFrame());
        final Vector3D relativeVelocity = vAtm.subtract(stateOsc.getPVCoordinates().getVelocity());
        // Call the DragSensitive method to compute partial derivatives with respect to position/velocity:
        this.spacecraft.addDDragAccDState(stateOsc, dAccdPos, dAccdVel, density, dragMOD, relativeVelocity, true, true);
        // The output of this method is partial derivatives in the TNW frame:
        final double[][] dFDECartTNW = new double[6][3];
        dFDECartTNW[0] = dAccdPos[0];
        dFDECartTNW[1] = dAccdPos[1];
        dFDECartTNW[2] = dAccdPos[2];
        dFDECartTNW[3] = dAccdVel[0];
        dFDECartTNW[4] = dAccdVel[1];
        dFDECartTNW[5] = dAccdVel[2];

        // Mean parameters are used for Gauss equations and jacobian matrix computations
        // dF/dE computation
        final double[][] dFDE = JavaMathAdapter.matrixTranspose(JavaMathAdapter.matrixMultiply(
            JavaMathAdapter.matrixTranspose(JacobianConverter.computeEquinoctialToCartesianJacobian(orbitMean)),
            dFDECartTNW));

        // d(GF)/dE = dG/dE * F + G * dF/dE
        double[][] result = JavaMathAdapter.matrixAdd(
            JavaMathAdapter.threeDMatrixVectorMultiply(computeGaussDerivativeEquations(orbitMean), dragTNW.toArray()),
            JavaMathAdapter.matrixTranspose(JavaMathAdapter.matrixMultiply(gaussEq, dFDE)));

        // Transposition (LATER: Keep this)
        result = JavaMathAdapter.matrixTranspose(result);

        // Temporary array containing the d(GF)/dE term of the transition matrix (vectorized form), for the current
        // squaring point.
        final double[] dGFDESquaringCurrent = new double[6 * 6];
        // The result is stored in this vector:
        JavaMathAdapter.matrixToVector(result, dGFDESquaringCurrent, 0);

        return dGFDESquaringCurrent;
    }

    /**
     * Compute the final d(GF)/dE term of the transition matrix, using Simpson's rule for each term.
     * 
     * @param deltavi
     *        interval length in eccentric anomaly
     * @throws PatriusException
     *         error when computing Simpson squaring
     */
    private void computeDGFDE(final double deltavi) throws PatriusException {
        final int dim = 6 * 6;
        final double[] dGFDEVect = new double[dim];

        // dGFDESquaring contains all temporary matrices in a vectorized form
        for (int i = 0; i < dim; i++) {
            dGFDEVect[i] = Squaring.simpsonMean(this.dGFDESquaring[i], deltavi);
        }
        // Final drag term of the transition matrix:
        JavaMathAdapter.vectorToMatrix(dGFDEVect, this.dGFDE);

        // We force d(GF)/dlambdaEq = 0 to avoid numerical noise
        for (int i = 0; i < 6; i++) {
            this.dGFDE[i][1] = 0;
        }
    }

    /**
     * Setter for the switch indicating whether the drag term of the transition matrix has to be computed.
     * 
     * @param transMatrixFlag
     *        flag to set
     */
    public void setTransMatComputationFlag(final boolean transMatrixFlag) {
        this.transMatFlag = transMatrixFlag;
        // Set the dimension of the
        this.dGFDESquaring = new double[6 * 6][this.squaringPoints];
    }
}
