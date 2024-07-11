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
 * @history creation 01/10/2014
 *
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:241:01/10/2014:created AbstractTides class
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::FA:464:24/06/2015:Analytical computation of the partial derivatives
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.forces.gravity.GravityToolbox;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.polynomials.HelmholtzPolynomial;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.ParameterUtils;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * Common handling of {@link ForceModel} methods for tides models.
 * </p>
 * <p>
 * This abstract class allows to provide easily the full set of {@link ForceModel} methods to tides models. Only one
 * method must be implemented by derived classes: {@link #updateCoefficientsCandS}.
 * 
 * @concurrency not thread-safe
 * @concurrency.comment not thread safe because of the method updateCoefficientsCandS().
 * 
 * @author Charlotte Maggiorani
 * 
 * @version $Id: AbstractTides.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 2.3
 * 
 */
public abstract class AbstractTides extends JacobiansParameterizable implements ForceModel, GradientModel,
    PotentialTimeVariations {

    /** Parameter name for central attraction coefficient. */
    public static final String MU = "central attraction coefficient";

    /** Parameter name for equatorial radius. */
    public static final String RADIUS = "equatorial radius";

    /** Serializable UID. */
    private static final long serialVersionUID = 1383292841711855839L;

    /** Central attraction coefficient parameter. */
    protected Parameter paramMu = null;

    /** Equatorial radius parameter. */
    protected Parameter paramAe = null;

    /** First normalized potential tesseral coefficients array. */
    protected double[][] coefficientsC;

    /** Second normalized potential tesseral coefficients array. */
    protected double[][] coefficientsS;

    /** First normalized potential tesseral coefficients array for partial derivatives computation. */
    protected double[][] coefficientsCPD;

    /** Second normalized potential tesseral coefficients array for partial derivatives computation. */
    protected double[][] coefficientsSPD;

    /** Frame for the central body. */
    protected final Frame bodyFrame;

    /** Degree. */
    private final int l;

    /** Order. */
    private final int m;

    /** Helmholtz polynomials */
    private final HelmholtzPolynomial helm;

    /** Denormalized coefficients for partial derivatives. */
    private double[][] denCPD;

    /** Denormalized coefficients for partial derivatives. */
    private double[][] denSPD;

    /**
     * Build a new instance.
     * 
     * @param centralBodyFrame
     *        rotating central body frame
     * @param equatorialRadius
     *        equatorial radius
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param degree
     *        degree
     * @param order
     *        order
     */
    protected AbstractTides(final Frame centralBodyFrame, final double equatorialRadius, final double mu,
        final int degree, final int order) {
        this(centralBodyFrame, equatorialRadius, mu, degree, order, degree, order);
    }

    /**
     * Build a new instance.
     * 
     * @param centralBodyFrame
     *        rotating central body frame
     * @param equatorialRadius
     *        equatorial radius
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param degree
     *        degree for acceleration computation
     * @param order
     *        order for acceleration computation
     * @param degreePD
     *        degree for partial derivatives computation
     * @param orderPD
     *        order for partial derivatives computation
     */
    protected AbstractTides(final Frame centralBodyFrame, final double equatorialRadius, final double mu,
        final int degree, final int order, final int degreePD, final int orderPD) {
        this(centralBodyFrame, new Parameter(RADIUS, equatorialRadius), new Parameter(MU, mu), degree, order,
            degreePD, orderPD);
    }

    /**
     * Build a new instance using {@link Parameter}.
     * 
     * @param centralBodyFrame
     *        rotating central body frame
     * @param equatorialRadius
     *        equatorial radius parameter
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) parameter
     * @param degree
     *        degree
     * @param order
     *        order
     */
    protected AbstractTides(final Frame centralBodyFrame, final Parameter equatorialRadius, final Parameter mu,
        final int degree, final int order) {
        this(centralBodyFrame, equatorialRadius, mu, degree, order, degree, order);
    }

    /**
     * Build a new instance using {@link Parameter}.
     * 
     * @param centralBodyFrame
     *        rotating central body frame
     * @param equatorialRadius
     *        equatorial radius parameter
     * @param mu
     *        central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) parameter
     * @param degree
     *        degree for acceleration computation
     * @param order
     *        order for acceleration computation
     * @param degreePD
     *        degree for partial derivatives computation
     * @param orderPD
     *        order for partial derivatives computation
     */
    protected AbstractTides(final Frame centralBodyFrame, final Parameter equatorialRadius, final Parameter mu,
        final int degree, final int order, final int degreePD, final int orderPD) {
        super(mu, equatorialRadius);
        ParameterUtils.addFieldToParameters(getParameters(), StandardFieldDescriptors.FORCE_MODEL, this.getClass());
        this.paramMu = mu;
        this.paramAe = equatorialRadius;
        this.bodyFrame = centralBodyFrame;
        this.l = degree;
        this.m = order;
        this.coefficientsC = new double[degree][order];
        this.coefficientsS = new double[degree][order];
        this.coefficientsCPD = new double[degreePD][orderPD];
        this.coefficientsSPD = new double[degreePD][orderPD];
        this.helm = new HelmholtzPolynomial(this.l - 1, this.m - 1);
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder) throws PatriusException {
        // compute acceleration in inertial frame
        final Vector3D accInInert = this.computeAcceleration(s);
        adder.addXYZAcceleration(accInInert.getX(), accInInert.getY(), accInInert.getZ());
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {

        // get the position in body frame
        final Transform bodyToInertial = this.bodyFrame.getTransformTo(s.getFrame(), s.getDate());
        final Vector3D posInBody = bodyToInertial.getInverse().transformVector(s.getPVCoordinates().getPosition());
        final PVCoordinates pv = new PVCoordinates(posInBody, Vector3D.ZERO);
        Vector3D gamma = this.computeAcceleration(pv, this.bodyFrame, s.getDate());

        // compute acceleration in inertial frame
        gamma = bodyToInertial.transformVector(gamma);

        return gamma;
    }

    /**
     * <p>
     * Method to compute the acceleration, from Balmino algorithm (see BalminoAttractionModel class). This method has
     * been implemented in order to validate the force model only. The reason is that for the validation context, we do
     * not want to set up an instance of the SpacecraftState object to avoid the inertial frame of the spacecraft orbit.
     * </p>
     * 
     * <p>
     * (see Feature #34 on https://www.orekit.org/forge/issues/34)
     * </p>
     * 
     * <p>
     * Out of the validation context, one must use the method Vector3D computeAcceleration(final SpacecraftState s)
     * </p>
     * 
     * @param pv
     *        PV coordinates of the spacecraft
     * @param frame
     *        frame in which the acceleration is computed
     * @param date
     *        date
     * @throws PatriusException
     *         if an Orekit error occurs
     * 
     * @return acceleration vector
     * 
     */
    public Vector3D computeAcceleration(final PVCoordinates pv, final Frame frame,
                                        final AbsoluteDate date) throws PatriusException {

        this.updateCoefficientsCandS(date);
        return GravityToolbox.computeBalminoAcceleration(pv, this.coefficientsC,
            this.coefficientsS, this.paramMu.getValue(), this.paramAe.getValue(), this.l - 1, this.m - 1, this.helm);
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientVelocity() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public abstract void updateCoefficientsCandS(final AbsoluteDate date) throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public abstract void updateCoefficientsCandSPD(final AbsoluteDate date) throws PatriusException;

    /** {@inheritDoc}. */
    @Override
    public final void addDAccDState(final SpacecraftState s,
                                    final double[][] dAccdPos, final double[][] dAccdVel) throws PatriusException {

        if (this.computeGradientPosition()) {
            // coefficients update and preparation for the partial derivatives
            // computation
            this.updateCoefficientsCandSPD(s.getDate());
            final int degree = this.coefficientsCPD.length;
            // denormalize the C and S normalized coefficients:
            final double[][] tempC = GravityToolbox.deNormalize(this.coefficientsCPD);
            final double[][] tempS = GravityToolbox.deNormalize(this.coefficientsSPD);
            // invert the arrays (optimization for later "line per line"
            // seeking)
            this.denCPD = new double[this.coefficientsCPD[degree - 1].length][this.coefficientsCPD.length];
            this.denSPD = new double[this.coefficientsSPD[degree - 1].length][this.coefficientsSPD.length];
            for (int i = 0; i < degree; i++) {
                final double[] cT = tempC[i];
                final double[] sT = tempS[i];
                for (int j = 0; j < cT.length; j++) {
                    this.denCPD[j][i] = cT[j];
                    this.denSPD[j][i] = sT[j];
                }
            }

            // get the position in body frame.
            final Transform fromBodyFrame = this.bodyFrame.getTransformTo(s.getFrame(),
                s.getDate());
            final Transform toBodyFrame = fromBodyFrame.getInverse();
            final PVCoordinates pvSat = toBodyFrame.transformPVCoordinates(s
                .getPVCoordinates());
            final double[][] dAdP = GravityToolbox
                .computeDAccDPos(pvSat, s.getDate(), this.paramAe.getValue(),
                    this.paramMu.getValue(), this.denCPD, this.denSPD);
            Vector3D dx = new Vector3D(dAdP[0][0], dAdP[1][0], dAdP[2][0]);
            Vector3D dy = new Vector3D(dAdP[0][1], dAdP[1][1], dAdP[2][1]);
            Vector3D dz = new Vector3D(dAdP[0][2], dAdP[1][2], dAdP[2][2]);
            // compute acceleration in inertial frame.
            dx = fromBodyFrame.transformVector(dx);
            dy = fromBodyFrame.transformVector(dy);
            dz = fromBodyFrame.transformVector(dz);

            double[][] derfinal = { { dx.getX(), dy.getX(), dz.getX() }, { dx.getY(), dy.getY(), dz.getY() },
                { dx.getZ(), dy.getZ(), dz.getZ() } };

            // jacobian matrix to express dPos in GCRF instead of body frame.
            final double[][] jac = new double[6][6];
            toBodyFrame.getJacobian(jac);

            // keep the useful part (3x3 for position).
            final double[][] useful = new double[3][3];
            for (int i = 0; i < useful.length; i++) {
                for (int j = 0; j < useful[i].length; j++) {
                    useful[i][j] = jac[i][j];
                }
            }

            // matrices of partial derivatives and jacobian.
            final Array2DRowRealMatrix dAdPMatrix = new Array2DRowRealMatrix(derfinal, false);
            final Array2DRowRealMatrix jacMatrix = new Array2DRowRealMatrix(useful, false);

            // multiplication
            final Array2DRowRealMatrix transformedMatrix = dAdPMatrix.multiply(jacMatrix);
            derfinal = transformedMatrix.getData(false);

            // the only non-null contribution for this force is dAcc/dPos.
            dAccdPos[0][0] += derfinal[0][0];
            dAccdPos[0][1] += derfinal[0][1];
            dAccdPos[0][2] += derfinal[0][2];
            dAccdPos[1][0] += derfinal[1][0];
            dAccdPos[1][1] += derfinal[1][1];
            dAccdPos[1][2] += derfinal[1][2];
            dAccdPos[2][0] += derfinal[2][0];
            dAccdPos[2][1] += derfinal[2][1];
            dAccdPos[2][2] += derfinal[2][2];
        }
    }

    /**
     * {@inheritDoc}.
     * No parameter is supported by this force model
     */
    @Override
    public void addDAccDParam(final SpacecraftState s, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {
        // Exception thrown systematically
        throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
    }

}
