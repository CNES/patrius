/**
 *
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
 *
 * @history created 14/09/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2861:18/05/2021:Optimisation du calcul des derivees partielles de EmpiricalForce 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::FA:133:18/11/2013:Javadoc improved
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:412:04/05/2015:changed partial derivatives formulas
 * VERSION::FA:440:06/06/2015:LOF type in partial derivatives
 * VERSION::FA:500:03/11/2015:New management of frames in acceleration computation
 * VERSION::FA:449:21/12/2015:Changes in attitude handling
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class implements the empirical force. An empirical force is a "pseudo acceleration"
 * with the same frequency of the spacecraft orbital frequency (or a multiple of this frequency).
 * This class allows to model a bunch of empirical forces, such as Hill, Hill2 and bias.
 * <p>
 * Given an orbit and a local frame (usually a LOF frame), and three vectors A, B and C defined in this frame, the
 * acceleration due to a generic empirical force is the following:<br>
 * a<sub>loc</sub> = Acos(n&omega;t) + Bsin(n&omega;t) + C, <br>
 * where n is the harmonic factor and &omega; is the orbital period.
 * </p>
 * <p>
 * As &omega; is usually unknown, cos(n&omega;t) and sin(n&omega;t) are computed from the orbital position of the
 * spacecraft:
 * <ul>
 * <li>when the orbit is highly inclined, the orbital position can be computed from the position 
 * of the ascending node;</li>
 * <li>when the orbit is heliosyncronous, the orbital position can be computed from the projection of the Sun in the
 * orbital plane;</li>
 * </ul>
 * To avoid to be restricted to these two cases, the user is free to choose a vector S that is used to define a
 * reference direction V (V = S x &omega;); this reference direction is used to compute cos(n&omega;t) and
 * sin(n&omega;t):
 * </p>
 * <ul>
 * <li>S =(0, 0, 1) when the reference direction V is the position of the ascending node;</li>
 * <li>S = sun direction in the inertial frame when the orbit is heliosyncronous.</li>
 * </ul>
 * This vector S shall be expressed in the inertial frame associated to the SpacecraftState.
 * 
 * <p>
 * The implementation of this class enables the computation of partial derivatives with respect to the <b>X, Y and
 * Z-components</b> of the <b>A, B and C coefficients</b>.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment attributes are mutable
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 */
@SuppressWarnings("PMD.NullAssignment")
public class EmpiricalForce extends JacobiansParameterizable implements ForceModel, GradientModel {

    /** X-component of the A coefficient. */
    public static final String AX_COEFFICIENT = "AX_COEFFICIENT";
    /** Y-component of the A coefficient. */
    public static final String AY_COEFFICIENT = "AY_COEFFICIENT";
    /** Z-component of the A coefficient. */
    public static final String AZ_COEFFICIENT = "AZ_COEFFICIENT";

    /** X-component of the B coefficient. */
    public static final String BX_COEFFICIENT = "BX_COEFFICIENT";
    /** Y-component of the B coefficient. */
    public static final String BY_COEFFICIENT = "BY_COEFFICIENT";
    /** Z-component of the B coefficient. */
    public static final String BZ_COEFFICIENT = "BZ_COEFFICIENT";

    /** X-component of the C coefficient. */
    public static final String CX_COEFFICIENT = "CX_COEFFICIENT";
    /** Y-component of the C coefficient. */
    public static final String CY_COEFFICIENT = "CY_COEFFICIENT";
    /** Z-component of the C coefficient. */
    public static final String CZ_COEFFICIENT = "CZ_COEFFICIENT";

    /** Serializable UID. */
    private static final long serialVersionUID = 5685047685477772591L;

    /** The harmonic factor. */
    private final int n;

    /**
     * The vector S whose cross product with &omega; is used as a reference direction
     * for the spacecraft orbital position computation. This vector shall be expressed in the SpacecraftState
     * inertial frame.
     */
    private Vector3D s;

    /**
     * The local frame of definition of vectors A, B and C
     * If null, the acceleration is expressed in the satellite frame
     */
    private Frame coeffFrame = null;

    /** The local orbital frame type of definition of vectors A, B and C. */
    private LOFType coeffFrameLOFType = null;

    // Coef A representing the coefficient of cos(wnt) in the acceleration formula.
    /** Parameterizable coef A (representing the coefficient of cos(wnt) in the acceleration formula) along x. */
    private final IParamDiffFunction coeffAx;
    /** Parameterizable coef A (representing the coefficient of cos(wnt) in the acceleration formula) along y. */
    private final IParamDiffFunction coeffAy;
    /** Parameterizable coef A (representing the coefficient of cos(wnt) in the acceleration formula) along z. */
    private final IParamDiffFunction coeffAz;

    // Coef B representing the coefficient of sin(wnt) in the acceleration formula.
    /** Parameterizable coef B (representing the coefficient of sin(wnt) in the acceleration formula) along x. */
    private final IParamDiffFunction coeffBx;
    /** Parameterizable coef B (representing the coefficient of sin(wnt) in the acceleration formula) along y. */
    private final IParamDiffFunction coeffBy;
    /** Parameterizable coef B (representing the coefficient of sin(wnt) in the acceleration formula) along z. */
    private final IParamDiffFunction coeffBz;

    // Coef C representing the constant term in the acceleration formula. */
    /** Parameterizable coef C (representing the constant term in the acceleration formula) along x. */
    private final IParamDiffFunction coeffCx;
    /** Parameterizable coef C (representing the constant term in the acceleration formula) along y. */
    private final IParamDiffFunction coeffCy;
    /** Parameterizable coef C (representing the constant term in the acceleration formula) along z. */
    private final IParamDiffFunction coeffCz;

    /**
     * Simple constructor for an empiric force, assigning a generic frame to the A, B, C coefficients
     * frame. <br>
     * It sets the parameters used for the acceleration computation:
     * a<sub>loc</sub> = Acos(n&omega;t) + Bsin(n&omega;t) + C.<br>
     * WARNING: If the frame of definition of vectors A,B and C is a local orbital frame (LOF), this constructor must
     * NOT be used (acceleration computation would fail);
     * The {@link EmpiricalForce#EmpiricalForce(int, Vector3D, Vector3D, Vector3D, Vector3D, LOFType)} constructor
     * should be used instead to avoid problems during propagation.<br>
     * 
     * @param harmonicFactor
     *        the harmonic factor n.
     * @param vectorS
     *        the direction whose cross product with &omega; is used as a reference direction to
     *        compute the cos(n&omega;t) and sin(n&omega;t) terms.
     * @param a
     *        the vector representing the A coefficient (expressed in the local frame).
     * @param b
     *        the vector representing the B coefficient (expressed in the local frame).
     * @param c
     *        the vector representing the C coefficient (expressed in the local frame).
     * @param coeffsFrame
     *        the frame of definition of vectors A, B and C.
     */
    public EmpiricalForce(final int harmonicFactor, final Vector3D vectorS, final Vector3D a,
                          final Vector3D b, final Vector3D c, final Frame coeffsFrame) {
        this(harmonicFactor, vectorS, new Parameter(AX_COEFFICIENT, a.getX()),
                new Parameter(AY_COEFFICIENT, a.getY()),
                new Parameter(AZ_COEFFICIENT, a.getZ()),
                new Parameter(BX_COEFFICIENT, b.getX()),
                new Parameter(BY_COEFFICIENT, b.getY()),
                new Parameter(BZ_COEFFICIENT, b.getZ()),
                new Parameter(CX_COEFFICIENT, c.getX()),
                new Parameter(CY_COEFFICIENT, c.getY()),
                new Parameter(CZ_COEFFICIENT, c.getZ()), coeffsFrame);
    }

    /**
     * Simple constructor for an empiric force.
     * This constructor MUST be used if the frame of definition of vectors A,B and C is a local orbital frame (LOF).
     * 
     * @param harmonicFactor
     *        the harmonic factor n.
     * @param vectorS
     *        the direction whose cross product with &omega; is used as a reference direction to
     *        compute the cos(n&omega;t) and sin(n&omega;t) terms.
     * @param a
     *        the vector representing the A coefficient (expressed in the local frame).
     * @param b
     *        the vector representing the B coefficient (expressed in the local frame).
     * @param c
     *        the vector representing the C coefficient (expressed in the local frame).
     * @param coeffsFrameLOFType
     *        the frame of definition of vectors A, B and C.
     */
    public EmpiricalForce(final int harmonicFactor, final Vector3D vectorS, final Vector3D a,
                          final Vector3D b, final Vector3D c, final LOFType coeffsFrameLOFType) {
        this(harmonicFactor, vectorS, new Parameter(AX_COEFFICIENT, a.getX()),
                new Parameter(AY_COEFFICIENT, a.getY()),
                new Parameter(AZ_COEFFICIENT, a.getZ()),
                new Parameter(BX_COEFFICIENT, b.getX()),
                new Parameter(BY_COEFFICIENT, b.getY()),
                new Parameter(BZ_COEFFICIENT, b.getZ()),
                new Parameter(CX_COEFFICIENT, c.getX()),
                new Parameter(CY_COEFFICIENT, c.getY()),
                new Parameter(CZ_COEFFICIENT, c.getZ()), coeffsFrameLOFType);
    }

    /**
     * Private constructor to avoid duplicating code.
     * 
     * @param harmonicFactor
     *        the harmonic factor n.
     * @param vectorS
     *        the direction whose cross product with &omega; is used as a reference direction to
     *        compute the cos(n&omega;t) and sin(n&omega;t) terms.
     * @param ax
     *        ax
     * @param ay
     *        ay
     * @param az
     *        az
     * @param bx
     *        bx
     * @param by
     *        by
     * @param bz
     *        bz
     * @param cx
     *        cx
     * @param cy
     *        cy
     * @param cz
     *        cz
     */
    private EmpiricalForce(final int harmonicFactor, final Vector3D vectorS,
                           final Parameter ax, final Parameter ay, final Parameter az,
                           final Parameter bx, final Parameter by, final Parameter bz,
                           final Parameter cx, final Parameter cy, final Parameter cz) {
        super();
        this.addJacobiansParameter(ax, ay, az, bx, by, bz, cx, cy, cz);
        this.enrichParameterDescriptors();
        this.n = harmonicFactor;
        this.s = vectorS;
        if (!vectorS.equals(Vector3D.ZERO)) {
            this.s = this.s.normalize();
        }
        this.coeffAx = new ConstantFunction(ax);
        this.coeffAy = new ConstantFunction(ay);
        this.coeffAz = new ConstantFunction(az);
        this.coeffBx = new ConstantFunction(bx);
        this.coeffBy = new ConstantFunction(by);
        this.coeffBz = new ConstantFunction(bz);
        this.coeffCx = new ConstantFunction(cx);
        this.coeffCy = new ConstantFunction(cy);
        this.coeffCz = new ConstantFunction(cz);
    }

    /**
     * Simple constructor for an empiric force using {@link Parameter} for A; B and C coef
     * with a given LOF frame.
     * 
     * @param harmonicFactor
     *        the harmonic factor
     * @param vectorS
     *        the direction vector whose cross product with &omega; is used as a reference direction
     *        to compute the cos(n&omega;t) and sin(n&omega;t) terms.
     * @param ax
     *        Parameter for A coefficient along x (ax)
     * @param ay
     *        Parameter for A coefficient along Y (ay)
     * @param az
     *        Parameter for A coefficient along Z (az)
     * @param bx
     *        Parameter for B coefficient along x (bx)
     * @param by
     *        Parameter for B coefficient along Y (by)
     * @param bz
     *        Parameter for B coefficient along Z (bz)
     * @param cx
     *        Parameter for C coefficient along x (cx)
     * @param cy
     *        Parameter for C coefficient along Y (cy)
     * @param cz
     *        Parameter for C coefficient along Z (cz)
     * @param coeffsFrameLOFType
     *        the frame of definition of vectors A, B and C
     */
    public EmpiricalForce(final int harmonicFactor, final Vector3D vectorS,
                          final Parameter ax, final Parameter ay, final Parameter az,
                          final Parameter bx, final Parameter by, final Parameter bz,
                          final Parameter cx, final Parameter cy, final Parameter cz,
                          final LOFType coeffsFrameLOFType) {
        this(harmonicFactor, vectorS, ax, ay, az, bx, by, bz, cx, cy, cz);
        this.coeffFrame = null;
        this.coeffFrameLOFType = coeffsFrameLOFType;
    }

    /**
     * Simple constructor for an empiric force using {@link Parameter} for A; B and C coef
     * with a given frame.
     * WARNING : If the frame of definition of vectors A,B and C is a local orbital frame (LOF),
     * this constructor must NOT be used, acceleration computation would fail.
     * 
     * @param harmonicFactor
     *        the harmonic factor n.
     * @param vectorS
     *        the direction whose cross product with &omega; is used as a reference direction to
     *        compute the cos(n&omega;t) and sin(n&omega;t) terms.
     * @param ax
     *        Parameter for A coefficient along x.
     * @param ay
     *        Parameter for A coefficient along Y.
     * @param az
     *        Parameter for A coefficient along Z.
     * @param bx
     *        Parameter for B coefficient along x.
     * @param by
     *        Parameter for B coefficient along Y.
     * @param bz
     *        Parameter for B coefficient along Z.
     * @param cx
     *        Parameter for C coefficient along x.
     * @param cy
     *        Parameter for C coefficient along Y.
     * @param cz
     *        Parameter for C coefficient along Z.
     * @param coeffsFrame
     *        the frame of vectors A, B and C.
     */
    public EmpiricalForce(final int harmonicFactor, final Vector3D vectorS,
                          final Parameter ax, final Parameter ay, final Parameter az,
                          final Parameter bx, final Parameter by, final Parameter bz,
                          final Parameter cx, final Parameter cy, final Parameter cz,
                          final Frame coeffsFrame) {
        this(harmonicFactor, vectorS, ax, ay, az, bx, by, bz, cx, cy, cz);
        this.coeffFrame = coeffsFrame;
        this.coeffFrameLOFType = null;
    }

    /**
     * Simple constructor for an empiric force using parameterizable functions for A; B and C coef
     * with a given LOF frame.
     * 
     * @param harmonicFactor
     *        the harmonic factor n.
     * @param vectorS
     *        the direction whose cross product with &omega; is used as a reference direction to
     *        compute the cos(n&omega;t) and sin(n&omega;t) terms.
     * @param ax
     *        Parameterizable function for A coefficient along x (ax)
     * @param ay
     *        Parameterizable function for A coefficient along Y (ay)
     * @param az
     *        Parameterizable function for A coefficient along Z (az)
     * @param bx
     *        Parameterizable function for B coefficient along x (bx)
     * @param by
     *        Parameterizable function for B coefficient along Y (by)
     * @param bz
     *        Parameterizable function for B coefficient along Z (bz)
     * @param cx
     *        Parameterizable function for C coefficient along x (cx)
     * @param cy
     *        Parameterizable function for C coefficient along Y (cy)
     * @param cz
     *        Parameterizable function for C coefficient along Z (cz)
     * @param coeffsFrameLOFType
     *        the frame of definition of vectors A, B and C.
     */
    public EmpiricalForce(final int harmonicFactor, final Vector3D vectorS,
                          final IParamDiffFunction ax, final IParamDiffFunction ay, final IParamDiffFunction az,
                          final IParamDiffFunction bx, final IParamDiffFunction by, final IParamDiffFunction bz,
                          final IParamDiffFunction cx, final IParamDiffFunction cy, final IParamDiffFunction cz,
                          final LOFType coeffsFrameLOFType) {
        super();
        this.addJacobiansParameter(ax.getParameters());
        this.addJacobiansParameter(ay.getParameters());
        this.addJacobiansParameter(az.getParameters());
        this.addJacobiansParameter(bx.getParameters());
        this.addJacobiansParameter(by.getParameters());
        this.addJacobiansParameter(bz.getParameters());
        this.addJacobiansParameter(cx.getParameters());
        this.addJacobiansParameter(cy.getParameters());
        this.addJacobiansParameter(cz.getParameters());
        this.enrichParameterDescriptors();
        this.n = harmonicFactor;
        this.s = vectorS;
        if (!vectorS.equals(Vector3D.ZERO)) {
            this.s = this.s.normalize();
        }

        this.coeffFrame = null;
        this.coeffFrameLOFType = coeffsFrameLOFType;

        this.coeffAx = ax;
        this.coeffAy = ay;
        this.coeffAz = az;
        this.coeffBx = bx;
        this.coeffBy = by;
        this.coeffBz = bz;
        this.coeffCx = cx;
        this.coeffCy = cy;
        this.coeffCz = cz;
    }

    /**
     * Simple constructor for an empiric force using parameterizable functions for A; B and C coef
     * with a given frame.
     * WARNING : If the frame of definition of vectors A,B and C is a local orbital frame (LOF),
     * this constructor must NOT be used, acceleration computation would fail.
     * 
     * @param harmonicFactor
     *        the harmonic factor n.
     * @param vectorS
     *        the direction whose cross product with &omega; is used as a reference direction to
     *        compute the cos(n&omega;t) and sin(n&omega;t) terms.
     * @param ax
     *        Parameterizable function for A coefficient along x.
     * @param ay
     *        Parameterizable function for A coefficient along Y.
     * @param az
     *        Parameterizable function for A coefficient along Z.
     * @param bx
     *        Parameterizable function for B coefficient along x.
     * @param by
     *        Parameterizable function for B coefficient along Y.
     * @param bz
     *        Parameterizable function for B coefficient along Z.
     * @param cx
     *        Parameterizable function for C coefficient along x.
     * @param cy
     *        Parameterizable function for C coefficient along Y.
     * @param cz
     *        Parameterizable function for C coefficient along Z.
     * @param coeffsFrame
     *        the frame of vectors A, B and C.
     */
    public EmpiricalForce(final int harmonicFactor, final Vector3D vectorS,
                          final IParamDiffFunction ax, final IParamDiffFunction ay, final IParamDiffFunction az,
                          final IParamDiffFunction bx, final IParamDiffFunction by, final IParamDiffFunction bz,
                          final IParamDiffFunction cx, final IParamDiffFunction cy, final IParamDiffFunction cz,
                          final Frame coeffsFrame) {
        super();
        this.addJacobiansParameter(ax.getParameters());
        this.addJacobiansParameter(ay.getParameters());
        this.addJacobiansParameter(az.getParameters());
        this.addJacobiansParameter(bx.getParameters());
        this.addJacobiansParameter(by.getParameters());
        this.addJacobiansParameter(bz.getParameters());
        this.addJacobiansParameter(cx.getParameters());
        this.addJacobiansParameter(cy.getParameters());
        this.addJacobiansParameter(cz.getParameters());
        this.enrichParameterDescriptors();
        this.n = harmonicFactor;
        this.s = vectorS;
        if (!vectorS.equals(Vector3D.ZERO)) {
            this.s = this.s.normalize();
        }
        this.coeffAx = ax;
        this.coeffAy = ay;
        this.coeffAz = az;
        this.coeffBx = bx;
        this.coeffBy = by;
        this.coeffBz = bz;
        this.coeffCx = cx;
        this.coeffCy = cy;
        this.coeffCz = cz;

        this.coeffFrame = coeffsFrame;
        this.coeffFrameLOFType = null;
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState state,
                                final TimeDerivativesEquations adder) throws PatriusException {
        // compute acceleration in inertial frame
        final Vector3D accInInert = this.computeAcceleration(state);
        adder.addXYZAcceleration(accInInert.getX(), accInInert.getY(), accInInert.getZ());
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState state) throws PatriusException {
        // gets the cos(nwt) and the sin(nwt) coefficients:
        final double[] cosSin = this.computeCosSin(state.getPVCoordinates(), this.s);
        final double cosnwt = cosSin[0];
        final double sinnwt = cosSin[1];
        // computes the acceleration in the local frame:
        final Vector3D accLocal = this.getCoeffA(state).scalarMultiply(cosnwt).add(
            this.getCoeffB(state).scalarMultiply(sinnwt)).add(this.getCoeffC(state));
        // transform the acceleration from the local frame to the inertial frame:
        final Vector3D res;
        if (this.coeffFrame == null) {
            if (this.coeffFrameLOFType == null) {
                // Force is expressed in satellite frame
                // Convert it into inertial frame
                res = state.getAttitude().getRotation().applyTo(accLocal);
            } else {
                // the A, B, C definition frame is a local orbital frame; build the current LOF frame from this orbit:
                final Orbit currentOrbit = state.getOrbit();
                final LocalOrbitalFrame lof = new LocalOrbitalFrame(currentOrbit.getFrame(), this.coeffFrameLOFType,
                    currentOrbit, "lof");
                final Transform t = lof.getTransformTo(state.getFrame(), state.getDate());
                res = t.transformVector(accLocal);
            }
        } else {
            // the A, B, C definition frame is not a local orbital frame:
            final Transform t = this.coeffFrame.getTransformTo(state.getFrame(), state.getDate());
            res = t.transformVector(accLocal);
        }
        return res;
    }

    /**
     * Return a vector coeffA build with values of (coeffAx, coeffAy, coeffAz)
     * 
     * @param state
     *        the current state
     * @return Vector3D representing the A coefficient
     * 
     * @since 2.3
     */
    private Vector3D getCoeffA(final SpacecraftState state) {
        // returned value
        return new Vector3D(this.coeffAx.value(state), this.coeffAy.value(state), this.coeffAz.value(state));
    }

    /**
     * Return a vector coeffB build with values of (coeffBx, coeffBy, coeffBz)
     * 
     * @param state
     *        the current state
     * @return Vector3D representing the A coefficient
     * 
     * @since 2.3
     */
    private Vector3D getCoeffB(final SpacecraftState state) {
        // returned value
        return new Vector3D(this.coeffBx.value(state), this.coeffBy.value(state), this.coeffBz.value(state));
    }

    /**
     * Return a vector coeffC build with values of (coeffCx, coeffCy, coeffCz)
     * 
     * @param state
     *        the current state
     * @return Vector3D representing the C coefficient
     * 
     * @since 2.3
     */
    private Vector3D getCoeffC(final SpacecraftState state) {
        // returned value
        return new Vector3D(this.coeffCx.value(state), this.coeffCy.value(state), this.coeffCz.value(state));
    }

    /**
     * <p>
     * Method to compute the acceleration. This method has been implemented in order to validate the force model only.
     * The reason is that for the validation context, we want to pass to this method some specific values for the local
     * frame and the direction, which depends on external variables (the PV reference ephemeris).
     * </p>
     * Out of the validation context, one must use the method Vector3D computeAcceleration(final SpacecraftState s)
     * </p>
     * 
     * @param pv
     *        PV coordinates of the spacecraft for the validation.
     * @param localFrameValidation
     *        the local frame to use for the validation.
     * @param vectorS
     *        the vector S.
     * @param frame
     *        the frame in which the PV coordinates are given.
     * @param state
     *        the spacecraft state.
     * @throws PatriusException
     *         if an Orekit error occurs
     * @return acceleration the computed acceleration
     * 
     */
    public Vector3D
        computeAcceleration(final PVCoordinates pv, final LocalOrbitalFrame localFrameValidation,
                            final Vector3D vectorS, final Frame frame, final SpacecraftState state)
            throws PatriusException {
        // gets the cos(nwt) and the sin(nwt) coefficients:
        final double[] cosSin = this.computeCosSin(pv, vectorS);
        final double cosnwt = cosSin[0];
        final double sinnwt = cosSin[1];
        // computes the acceleration in the local frame:
        final Vector3D accLocal = this.getCoeffA(state).scalarMultiply(cosnwt).add(
            this.getCoeffB(state).scalarMultiply(sinnwt)).add(this.getCoeffC(state));
        // transform the acceleration from the local frame to the inertial frame:
        final Transform t = localFrameValidation.getTransformTo(frame, state.getDate());
        return t.transformVector(accLocal);
    }

    /**
     * Private method to compute the cos(nwt) and sin(nwt) values.
     * 
     * @param pv
     *        the pv coordinates.
     * @param vectorS
     *        the vector S.
     * @return the cos(nwt) and sin(nwt) values as an array of doubles.
     */
    public double[] computeCosSin(final PVCoordinates pv, final Vector3D vectorS) {
        final Vector3D pos = pv.getPosition();
        // momentum (p x v) norm
        final Vector3D w = pv.getMomentum().normalize();
        final Vector3D v = Vector3D.crossProduct(w, vectorS).normalize();
        final Vector3D u = Vector3D.crossProduct(v, w).normalize();
        final double[] out = new double[2];
        // initialize cos(nwt)
        out[0] = Vector3D.dotProduct(pos.normalize(), u);
        // initialize sin(nwt)
        out[1] = Vector3D.dotProduct(pos.normalize(), v);
        if (this.n > 1) {
            // harmonic factor > 1
            final double p11 = out[0];
            final double p12 = -out[1];
            final double p21 = out[1];
            final double p22 = out[0];
            double pn11 = out[0];
            double pn12 = -out[1];
            double pn21 = out[1];
            double pn22 = out[0];
            for (int i = 1; i < this.n; i++) {
                // loop on the harmonic factor number:
                final double pn11n = p11 * pn11 + p12 * pn21;
                final double pn12n = p11 * pn12 + p12 * pn22;
                final double pn21n = p21 * pn11 + p22 * pn21;
                final double pn22n = p21 * pn12 + p22 * pn22;
                pn11 = pn11n;
                pn12 = pn12n;
                pn21 = pn21n;
                pn22 = pn22n;
            }
            // cos(nwt)
            out[0] = pn11;
            // sin(nwt)
            out[1] = pn21;
        }
        return out;
    }

    /**
     * @return the local frame in which the vectors A, B and C are defined.
     */
    public final Frame getLocalFrame() {
        return this.coeffFrame;
    }

    /**
     * @return the direction vector S.
     */
    public final Vector3D getVectorS() {
        return this.s;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDState(final SpacecraftState state, final double[][] dAccdPos,
                              final double[][] dAccdVel) throws PatriusException {
        // does nothing
    }

    /**
     * {@inheritDoc} <br>
     * {@link EmpiricalForce#AX_COEFFICIENT} --> derivatives with respect to the X-component of the A coefficient <br>
     * {@link EmpiricalForce#AY_COEFFICIENT} --> derivatives with respect to the Y-component of the A coefficient <br>
     * {@link EmpiricalForce#AZ_COEFFICIENT} --> derivatives with respect to the Z-component of the A coefficient <br>
     * {@link EmpiricalForce#BX_COEFFICIENT} --> derivatives with respect to the X-component of the B coefficient <br>
     * {@link EmpiricalForce#BY_COEFFICIENT} --> derivatives with respect to the Y-component of the B coefficient <br>
     * {@link EmpiricalForce#BZ_COEFFICIENT} --> derivatives with respect to the Z-component of the B coefficient <br>
     * {@link EmpiricalForce#CX_COEFFICIENT} --> derivatives with respect to the X-component of the C coefficient <br>
     * {@link EmpiricalForce#CY_COEFFICIENT} --> derivatives with respect to the Y-component of the C coefficient <br>
     * {@link EmpiricalForce#CZ_COEFFICIENT} --> derivatives with respect to the Z-component of the C coefficient <br>
     */
    @Override
    public void addDAccDParam(final SpacecraftState state, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {
        // if the parameter is not handled (not supported = not stored in the parameters map) :
        // re throwing the exception
        if (!this.supportsJacobianParameter(param)) {
            throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
        }

        // get the transformation from the local frame to the inertial frame:
        // transform matrix
        final double[][] t;
        if (this.coeffFrame == null) {
            if (this.coeffFrameLOFType == null) {
                // Force is expressed in satellite frame
                // Convert it into inertial frame
                // Check if an attitude is defined first
                if (state.getAttitude() == null) {
                    throw new PatriusException(PatriusMessages.NO_ATTITUDE_DEFINED);
                } else {
                    t = state.getAttitude().getRotation().getMatrix();
                }
            } else {
                // the A, B, C definition frame is a local orbital frame; build the current LOF frame from this orbit:
                final Orbit currentOrbit = state.getOrbit();
                final LocalOrbitalFrame lof = new LocalOrbitalFrame(currentOrbit.getFrame(), this.coeffFrameLOFType,
                    currentOrbit, "lofPartialDer");
                final Transform transform = state.getFrame().getTransformTo(lof, state.getDate());
                t = transform.getRotation().getMatrix();
            }
        } else {
            // the A, B, C definition frame is not a local orbital frame:
            final Transform transform = state.getFrame().getTransformTo(this.coeffFrame, state.getDate());
            t = transform.getRotation().getMatrix();
        }
        // transform matrix
        final double[] cosSin = this.computeCosSin(state.getPVCoordinates(), this.s);

        // Computes the contribution of the partial derivatives of acceleration
        // with respect to final parameter on the x-component
        this.addDACcDparamOnX(param, state, t, cosSin, dAccdParam);

        // Computes the contribution of the partial derivatives of acceleration
        // with respect to final parameter on the y-component
        this.addDAccDparamOnY(param, state, t, cosSin, dAccdParam);

        // Computes the contribution of the partial derivatives of acceleration
        // with respect to final parameter on the z-component
        this.addDAccDparamOnZ(param, state, t, cosSin, dAccdParam);

    }

    /**
     * This method is used to gather treatments for computing contribution of Ax,
     * Bx, and Cx for partial derivatives of acceleration with respect to the parameter along x
     * 
     * @param param
     *        : name parameter
     * @param state
     *        : current state
     * @param cf
     *        : matrix of the transformation from the local frame to the inertial frame
     * @param cosSin
     *        : array of double 0 : cos(nwt) 1 : sin(nwt)
     * @param dAccdParam
     *        : vector updated with Ax, Bx and Cx contributions along the axe x
     * 
     * @since 2.3
     */
    private void addDACcDparamOnX(final Parameter param, final SpacecraftState state,
                                  final double[][] cf, final double[] cosSin, final double[] dAccdParam) {

        // Computing Ax contribution
        if (this.coeffAx.supportsParameter(param)) {
            final double derAx = cosSin[0] * this.coeffAx.derivativeValue(param, state);
            // partial derivatives of accelerationX
            dAccdParam[0] += cf[0][0] * derAx;
            // partial derivatives of accelerationY:
            dAccdParam[1] += cf[1][0] * derAx;
            // partial derivatives of accelerationZ:
            dAccdParam[2] += cf[2][0] * derAx;

        }

        // Computing Bx contribution
        if (this.coeffBx.supportsParameter(param)) {
            final double derBx = cosSin[1] * this.coeffBx.derivativeValue(param, state);
            // partial derivatives of acceleration
            dAccdParam[0] += cf[0][0] * derBx;
            // partial derivatives of accelerationY:
            dAccdParam[1] += cf[1][0] * derBx;
            // partial derivatives of accelerationZ:
            dAccdParam[2] += cf[2][0] * derBx;
        }

        // Computing Cx contribution
        if (this.coeffCx.supportsParameter(param)) {
            final double derCx = this.coeffCx.derivativeValue(param, state);
            // partial derivatives of acceleration
            dAccdParam[0] += cf[0][0] * derCx;
            // partial derivatives of accelerationY:
            dAccdParam[1] += cf[1][0] * derCx;
            // partial derivatives of accelerationZ:
            dAccdParam[2] += cf[2][0] * derCx;
        }
    }

    /**
     * This method is used to gather treatments for computing contribution of
     * Ay, By, and Cy for partial derivatives of acceleration with respect to the parameter along y
     * 
     * @param param
     *        : name parameter
     * @param state
     *        : current state
     * @param cf
     *        : matrix of the transformation from the local frame to the inertial frame
     * @param cosSin
     *        : array of double 0 : cos(nwt) 1 : sin(nwt)
     * @param dAccdParam
     *        : vector updated with Ay, By and Cy contributions along the axe y
     * 
     * @since 2.3
     */
    private void addDAccDparamOnY(final Parameter param, final SpacecraftState state,
                                  final double[][] cf, final double[] cosSin, final double[] dAccdParam) {

        // Computing Ay contribution
        if (this.coeffAy.supportsParameter(param)) {
            final double derAy = cosSin[0] * this.coeffAy.derivativeValue(param, state);
            // partial derivatives of accelerationX:
            dAccdParam[0] += cf[0][1] * derAy;
            // partial derivatives of acceleration with respect to the parameter along y
            dAccdParam[1] += cf[1][1] * derAy;
            // partial derivatives of accelerationZ:
            dAccdParam[2] += cf[2][1] * derAy;
        }

        // Computing By contribution
        if (this.coeffBy.supportsParameter(param)) {
            final double derBy = cosSin[1] * this.coeffBy.derivativeValue(param, state);
            // partial derivatives of accelerationX:
            dAccdParam[0] += cf[0][1] * derBy;
            // partial derivatives of acceleration
            dAccdParam[1] += cf[1][1] * derBy;
            // partial derivatives of accelerationZ:
            dAccdParam[2] += cf[2][1] * derBy;
        }

        // Computing Cy contribution
        if (this.coeffCy.supportsParameter(param)) {
            final double derCy = this.coeffCy.derivativeValue(param, state);
            // partial derivatives of accelerationX:
            dAccdParam[0] += cf[0][1] * derCy;
            // partial derivatives of acceleration
            dAccdParam[1] += cf[1][1] * derCy;
            // partial derivatives of accelerationZ:
            dAccdParam[2] += cf[2][1] * derCy;
        }

    }

    /**
     * This method is used to gather treatments for computing contribution of
     * Az, Bz, and Cz for partial derivatives of acceleration with respect to the parameter along z
     * 
     * @param param
     *        : name parameter
     * @param state
     *        : current state
     * @param cf
     *        : matrix of the transformation from the local frame to the inertial frame
     * @param cosSin
     *        : array of double 0 : cos(nwt) 1 : sin(nwt)
     * @param dAccdParam
     *        : vector updated with Az, Bz and Cz contributions along the axe z
     * 
     * @since 2.3
     */
    private void addDAccDparamOnZ(final Parameter param, final SpacecraftState state,
                                  final double[][] cf, final double[] cosSin, final double[] dAccdParam) {

        // Computing Ay contribution
        if (this.coeffAz.supportsParameter(param)) {
            final double derAz = cosSin[0] * this.coeffAz.derivativeValue(param, state);
            // partial derivatives of accelerationX:
            dAccdParam[0] += cf[0][2] * derAz;
            // partial derivatives of accelerationY:
            dAccdParam[1] += cf[1][2] * derAz;
            // partial derivatives of acceleration
            dAccdParam[2] += cf[2][2] * derAz;
        }

        // Computing Bz contribution
        if (this.coeffBz.supportsParameter(param)) {
            final double derBz = cosSin[1] * this.coeffBz.derivativeValue(param, state);
            // partial derivatives of accelerationX:
            dAccdParam[0] += cf[0][2] * derBz;
            // partial derivatives of accelerationY:
            dAccdParam[1] += cf[1][2] * derBz;
            // partial derivatives of acceleration
            dAccdParam[2] += cf[2][2] * derBz;
        }

        // Computing Cz contribution
        if (this.coeffCz.supportsParameter(param)) {
            final double derCz = this.coeffCz.derivativeValue(param, state);
            // partial derivatives of accelerationX:
            dAccdParam[0] += cf[0][2] * derCz;
            // partial derivatives of accelerationY:
            dAccdParam[1] += cf[1][2] * derCz;
            // partial derivatives of acceleration
            dAccdParam[2] += cf[2][2] * derCz;
        }
    }

    /**
     * Get the parametereziable function for the Ax coefficient.
     * 
     * @return the parametereziable function for the Ax coefficient
     */
    public IParamDiffFunction getAx() {
        return this.coeffAx;
    }

    /**
     * Get the parametereziable function for the Ay coefficient.
     * 
     * @return the parametereziable function for the Ay coefficient
     */
    public IParamDiffFunction getAy() {
        return this.coeffAy;
    }

    /**
     * Get the parametereziable function for the Az coefficient.
     * 
     * @return the parametereziable function for the Az coefficient
     */
    public IParamDiffFunction getAz() {
        return this.coeffAz;
    }

    /**
     * Get the parametereziable function for the Bx coefficient.
     * 
     * @return the parametereziable function for the Bx coefficient
     */
    public IParamDiffFunction getBx() {
        return this.coeffBx;
    }

    /**
     * Get the parametereziable function for the By coefficient.
     * 
     * @return the parametereziable function for the By coefficient
     */
    public IParamDiffFunction getBy() {
        return this.coeffBy;
    }

    /**
     * Get the parametereziable function for the Bz coefficient.
     * 
     * @return the parametereziable function for the Bz coefficient
     */
    public IParamDiffFunction getBz() {
        return this.coeffBz;
    }

    /**
     * Get the parametereziable function for the Ax coefficient.
     * 
     * @return the parametereziable function for the Ax coefficient
     */
    public IParamDiffFunction getCx() {
        return this.coeffCx;
    }

    /**
     * Get the parametereziable function for the Cy coefficient.
     * 
     * @return the parametereziable function for the Cy coefficient
     */
    public IParamDiffFunction getCy() {
        return this.coeffCy;
    }

    /**
     * Get the parametereziable function for the Cz coefficient.
     * 
     * @return the parametereziable function for the Cz coefficient
     */
    public IParamDiffFunction getCz() {
        return this.coeffCz;
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientVelocity() {
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }
}
