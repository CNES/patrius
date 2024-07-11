/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::DM:90:03/10/2013:moved main computation method to GravityToolbox
 * VERSION::FA:228:26/03/2014:Corrected partial derivatives computation
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:280:08/10/2014:propagator modified in order to use the mu of gravitational forces
 * VERSION::FA:372:27/11/2014:Newtonian attraction bug
 * VERSION::FA:423:17/11/2015: improve computation times
 * VERSION::DM:505:19/08/2015:corrected addDAccDParam exception
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:1267:09/03/2018: Addition of getters for C and CS tables
 * VERSION::DM:1489:07/06/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
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
 * This class represents the gravitational field of a celestial body.
 * <p>
 * The algorithm implemented in this class has been designed by Leland E. Cunningham (Lockheed Missiles and Space
 * Company, Sunnyvale and Astronomy Department University of California, Berkeley) in his 1969 paper:
 * <em>On the computation of the spherical harmonic
 * terms needed during the numerical integration of the orbital motion
 * of an artificial satellite</em> (Celestial Mechanics 2, 1970).
 * </p>
 * 
 * <p>
 * The implementation of this class enables the computation of partial derivatives by finite differences with respect to
 * the <b>central attraction coefficient</b>.
 * </p>
 * 
 * <p>
 * Warning: using a 0x0 Earth potential model is equivalent to a simple Newtonian attraction. However computation times
 * will be much slower since this case is not particularized and hence conversion from body frame (often ITRF) to
 * integration frame is necessary.
 * </p>
 * 
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 */
@SuppressWarnings("PMD.NullAssignment")
public class CunninghamAttractionModel extends AbstractAttractionModel {

    /** Parameter name for equatorial radius. */
    public static final String RADIUS = "equatorial radius";

    /** Serializable UID. */
    private static final long serialVersionUID = 759122284106467933L;

    /** First normalized potential tesseral coefficients array. */
    private final double[][] c;
    /** Second normalized potential tesseral coefficients array. */
    private final double[][] s;
    /**
     * First normalized potential tesseral coefficients array for acceleration partial derivatives
     * with respect to state computation.
     */
    private final double[][] cPD;
    /**
     * Second normalized potential tesseral coefficients array for acceleration partial derivatives
     * with respect to state computation.
     */
    private final double[][] sPD;

    /** Central attraction coefficient parameter. */
    private Parameter paramMu = null;

    /** Equatorial radius parameter. */
    private Parameter paramAe = null;

    /** Multiplicative coefficient. */
    private double k;

    /** Degree of potential. */
    private final int degree;
    /** Order of potential. */
    private final int order;

    /**
     * Creates a new instance.
     * 
     * @param centralBodyFrame rotating body frame
     * @param equatorialRadius reference equatorial radius of the potential
     * @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param cIn un-normalized coefficients array (cosine part)
     * @param sIn un-normalized coefficients array (sine part)
     * 
     * @exception IllegalArgumentException if coefficients array do not match
     */
    public CunninghamAttractionModel(final Frame centralBodyFrame, final double equatorialRadius,
        final double mu, final double[][] cIn, final double[][] sIn) {
        this(centralBodyFrame, equatorialRadius, mu, cIn, sIn, cIn.length < 1 ? 0 : cIn.length - 1,
            cIn.length < 1 ? 0 : cIn[cIn.length - 1].length - 1);
    }

    /**
     * Creates a new instance.
     * 
     * @param centralBodyFrame rotating body frame
     * @param equatorialRadius reference equatorial radius of the potential
     * @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param cIn un-normalized coefficients array (cosine part)
     * @param sIn un-normalized coefficients array (sine part)
     * @param degreePD degree for partial derivatives
     * @param orderPD order for partial derivatives
     * @exception IllegalArgumentException if coefficients array do not match or degree and/or order
     *            for partial derivatives is higher than degree and/or order for acceleration
     */
    public CunninghamAttractionModel(final Frame centralBodyFrame, final double equatorialRadius,
        final double mu, final double[][] cIn, final double[][] sIn, final int degreePD,
        final int orderPD) {
        // storing Mu and equatorial radius value in the map of parameters
        this(centralBodyFrame, new Parameter(RADIUS, equatorialRadius), new Parameter(MU, mu), cIn,
            sIn, degreePD, orderPD);
    }

    /**
     * Creates a new instance using {@link Parameter}.
     * 
     * @param centralBodyFrame rotating body frame
     * @param equatorialRadius reference equatorial radius of the potential
     * @param mu parameter representing central body attraction coefficient
     *        (m<sup>3</sup>/s<sup>2</sup>)
     * @param cIn parameter representing un-normalized coefficients array (cosine part)
     * @param sIn un-normalized coefficients array (sine part)
     * @exception IllegalArgumentException if coefficients array do not match
     */
    public CunninghamAttractionModel(final Frame centralBodyFrame,
        final Parameter equatorialRadius, final Parameter mu, final double[][] cIn,
        final double[][] sIn) {
        this(centralBodyFrame, equatorialRadius, mu, cIn, sIn, cIn.length < 1 ? 0 : cIn.length - 1,
            cIn.length < 1 ? 0 : cIn[cIn.length - 1].length - 1);
    }

    /**
     * Creates a new instance using {@link Parameter}.
     * 
     * @param centralBodyFrame rotating body frame
     * @param equatorialRadius reference equatorial radius of the potential
     * @param mu parameter representing central body attraction coefficient
     *        (m<sup>3</sup>/s<sup>2</sup>)
     * @param cIn parameter representing un-normalized coefficients array (cosine part)
     * @param sIn un-normalized coefficients array (sine part)
     * @param degreePD degree for partial derivatives
     * @param orderPD order for partial derivatives
     * @exception IllegalArgumentException if coefficients array do not match or degree and/or order
     *            for partial derivatives is higher than degree and/or order for acceleration
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    public CunninghamAttractionModel(final Frame centralBodyFrame,
        final Parameter equatorialRadius, final Parameter mu, final double[][] cIn,
        final double[][] sIn, final int degreePD, final int orderPD) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // storing Mu and equatorial radius value in the map of parameters
        super(mu, equatorialRadius);
        ParameterUtils.addFieldToParameters(getParameters(), StandardFieldDescriptors.FORCE_MODEL, this.getClass());
        this.paramMu = mu;
        this.paramAe = equatorialRadius;
        this.bodyFrame = centralBodyFrame;
        this.k = 1.;

        if (cIn.length < 1) {
            // C size is 0, the degree is zero:
            this.c = new double[1][1];
            this.s = new double[1][1];
            this.degree = 0;
            this.order = 0;
        } else {
            this.degree = cIn.length - 1;
            this.order = cIn[this.degree].length - 1;

            // check the C and S matrix dimension is the same, otherwise throw an exception:
            if ((cIn.length != sIn.length)
                || (cIn[cIn.length - 1].length != sIn[sIn.length - 1].length)) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.POTENTIAL_ARRAYS_SIZES_MISMATCH, cIn.length,
                    cIn[this.degree].length, sIn.length, sIn[this.degree].length);
            }

            // invert the arrays (optimization for later "line per line" seeking)
            this.c = new double[cIn[this.degree].length][cIn.length];
            this.s = new double[sIn[this.degree].length][sIn.length];

            for (int i = 0; i <= this.degree; i++) {
                final double[] cT = cIn[i];
                final double[] sT = sIn[i];
                for (int j = 0; j < cT.length; j++) {
                    this.c[j][i] = cT[j];
                    this.s[j][i] = sT[j];
                }
            }
        }

        // C[0][0] = 0 in order not to compute keplerian evolution (managed by propagator):
        this.c[0][0] = 0.0;

        // Build arrays for partial derivatives
        // Check that input values for degreePD and orderPD are positive
        if (degreePD < 0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.NEGATIVE_DEGREE_FOR_PARTIAL_DERIVATIVES_COMPUTATION, degreePD);
        }

        if (orderPD < 0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.NEGATIVE_ORDER_FOR_PARTIAL_DERIVATIVES_COMPUTATION, orderPD);
        }

        if (degreePD > 0 || orderPD > 0) {

            // Check degree and order for partial derivatives are lower of equal to those for
            // acceleration
            if (degreePD > this.degree) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD_PD, this.degree, degreePD);
            }
            if (orderPD > this.order) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD_PD, this.order, orderPD);
            }

            this.cPD = new double[orderPD + 1][degreePD + 1];
            this.sPD = new double[orderPD + 1][degreePD + 1];
            for (int i = 0; i <= orderPD; i++) {
                final double[] cT = this.c[i];
                final double[] sT = this.s[i];
                for (int j = 0; j <= degreePD; j++) {
                    this.cPD[i][j] = cT[j];
                    this.sPD[i][j] = sT[j];
                }
            }

        } else {
            this.cPD = null;
            this.sPD = null;
        }
    }

    /** {@inheritDoc}. */
    @Override
    public void
            addContribution(final SpacecraftState state, final TimeDerivativesEquations adder)
                                                                                              throws PatriusException {
        // compute acceleration in inertial frame
        final Vector3D acceleration = this.computeAcceleration(state);
        adder.addXYZAcceleration(acceleration.getX(), acceleration.getY(), acceleration.getZ());
    }

    /** {@inheritDoc}. */
    @Override
    public final EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /** {@inheritDoc}. */
    @Override
    public boolean computeGradientPosition() {
        return this.cPD != null && this.sPD != null;
    }

    /** {@inheritDoc}. */
    @Override
    public boolean computeGradientVelocity() {
        return false;
    }

    /** {@inheritDoc}. */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState state) throws PatriusException {
        // get the position in body frame
        final Transform fromBodyFrame = this.bodyFrame.getTransformTo(state.getFrame(), state.getDate());
        final Transform toBodyFrame = fromBodyFrame.getInverse();
        final Vector3D relative = toBodyFrame.transformPosition(state.getPVCoordinates()
            .getPosition());
        final PVCoordinates pv = new PVCoordinates(relative, Vector3D.ZERO);
        Vector3D gamma = GravityToolbox.computeCunninghamAcceleration(pv, this.paramAe.getValue(), this.c, this.s,
            this.degree, this.order, this.paramMu.getValue());
        // compute acceleration in inertial frame
        gamma = fromBodyFrame.transformVector(gamma);
        return gamma.scalarMultiply(this.k);
    }

    /**
     * <p>
     * Method to compute the acceleration. This method has been implemented in order to validate the force model only.
     * The reason is that for the validation context, we do not want to set up an instance of the SpacecraftState object
     * to avoid the inertial frame of the spacecraft orbit.
     * </p>
     * 
     * <p>
     * (see Story #V82 and Feature #34 on https://www.orekit.org/forge/issues/34)
     * </p>
     * 
     * <p>
     * Out of the validation context, one must use the method Vector3D computeAcceleration(final SpacecraftState s)
     * </p>
     * 
     * <p>
     * Fixes bug #97: Numerical stability errors for high order gravity field. (see
     * https://www.orekit.org/forge/issues/97) The error and was due to the acceleration being computed as r^n * (cn *
     * gradC + sn * gradS). As order n increased r^n increased up to exceed floating point limits whereas the gradient
     * decreased. In fact r^n * grad was always a normal floating point number despite both terms were not. The fix
     * involved changing the loops so the gradients did include the r^n part.
     * </p>
     * 
     * @param pv PV coordinates of the spacecraft
     * @param date date
     * 
     * @return acceleration vector
     * 
     */
    public Vector3D computeAcceleration(final PVCoordinates pv, final AbsoluteDate date) {
        return GravityToolbox.computeCunninghamAcceleration(pv, this.paramAe.getValue(), this.c, this.s, this.degree,
            this.order, this.paramMu.getValue()).scalarMultiply(this.k);
    }

    /** {@inheritDoc}. */
    @Override
    public final void addDAccDState(final SpacecraftState state, final double[][] dAccdPos,
                                    final double[][] dAccdVel) throws PatriusException {

        if (this.computeGradientPosition()) {
            // transformations to and from body frame
            final Transform fromBodyFrame = this.bodyFrame.getTransformTo(state.getFrame(),
                state.getDate());
            final Transform toBodyFrame = fromBodyFrame.getInverse();

            // pvs in body frame
            final PVCoordinates pvSat = toBodyFrame
                .transformPVCoordinates(state.getPVCoordinates());

            // partial derivatives in body frame
            final double[][] dAdP = GravityToolbox.computeDAccDPos(pvSat, state.getDate(),
                this.paramAe.getValue(), this.paramMu.getValue(), this.cPD, this.sPD);

            // conversion to spacecraft state frame
            Vector3D dx = new Vector3D(dAdP[0][0], dAdP[1][0], dAdP[2][0]);
            Vector3D dy = new Vector3D(dAdP[0][1], dAdP[1][1], dAdP[2][1]);
            Vector3D dz = new Vector3D(dAdP[0][2], dAdP[1][2], dAdP[2][2]);
            // compute acceleration in inertial frame
            dx = fromBodyFrame.transformVector(dx);
            dy = fromBodyFrame.transformVector(dy);
            dz = fromBodyFrame.transformVector(dz);

            double[][] derfinal = { { dx.getX(), dy.getX(), dz.getX() },
                { dx.getY(), dy.getY(), dz.getY() }, { dx.getZ(), dy.getZ(), dz.getZ() } };

            // jacobian matrix to express dPos in GCRF instead of body frame
            final double[][] jac = new double[6][6];
            toBodyFrame.getJacobian(jac);

            // keep the useful part (3x3 for position)
            final double[][] useful = new double[3][3];
            for (int i = 0; i < useful.length; i++) {
                for (int j = 0; j < useful[i].length; j++) {
                    useful[i][j] = jac[i][j];
                }
            }

            // matrices of partial derivatives and jacobian
            final Array2DRowRealMatrix dAdPMatrix = new Array2DRowRealMatrix(derfinal);
            final Array2DRowRealMatrix jacMatrix = new Array2DRowRealMatrix(useful);

            // multiplication
            final Array2DRowRealMatrix transformedMatrix = dAdPMatrix.multiply(jacMatrix);
            derfinal = transformedMatrix.scalarMultiply(this.k).getData(false);

            // the only non-null contribution for this force is dAcc/dPos
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
     * {@inheritDoc} <br>
     * No parameter is supported by this force model.
     */
    @Override
    public void addDAccDParam(final SpacecraftState state, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {
        throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public double getMu() {
        return this.paramMu.getValue();
    }

    /**
     * @return the normalized C coefficients.
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] getC() {
        return this.c;
    }

    /**
     * @return the normalized S coefficients.
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] getS() {
        return this.s;
    }

    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public double getMultiplicativeFactor() {
        return this.k;
    }

    /** {@inheritDoc} */
    @Override
    public void setMultiplicativeFactor(final double coefficient) {
        this.k = coefficient;
    }

    /** {@inheritDoc} */
    @Override
    public void setMu(final double muIn) {
        this.paramMu.setValue(muIn);
    }
}
