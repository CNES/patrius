/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.AdapterPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Analytical model for small maneuvers.
 * <p>
 * The aim of this model is to compute quickly the effect at date t<sub>1</sub> of a small maneuver performed at an
 * earlier date t<sub>0</sub>. Both the direct effect of the maneuver and the Jacobian of this effect with respect to
 * maneuver parameters are available.
 * </p>
 * <p>
 * These effect are computed analytically using two Jacobian matrices:
 * <ol>
 * <li>J<sub>0</sub>: Jacobian of Keplerian or equinoctial elements with respect to cartesian parameters at date
 * t<sub>0</sub></li> allows to compute maneuver effect as a change in orbital elements at maneuver date t<sub>0</sub>,
 * <li>J<sub>1/0</sub>: Jacobian of Keplerian or equinoctial elements at date t<sub>1</sub> with respect to Keplerian or
 * equinoctial elements at date t<sub>0</sub></li> allows to propagate the change in orbital elements to final date
 * t<sub>1</sub>.
 * </ol>
 * </p>
 * <p>
 * The second Jacobian, J<sub>1/0</sub>, is computed using a simple Keplerian model, i.e. it is the identity except for
 * the mean motion row which also includes an off-diagonal element due to semi-major axis change.
 * </p>
 * <p>
 * The orbital elements change at date t<sub>1</sub> can be added to orbital elements extracted from state, and the
 * final elements taking account the changes are then converted back to appropriate type, which may be different from
 * Keplerian or equinoctial elements.
 * </p>
 * <p>
 * Note that this model takes <em>only</em> Keplerian effects into account. This means that using only this class to
 * compute an inclination maneuver in Low Earth Orbit will <em>not</em> change ascending node drift rate despite
 * inclination has changed (the same would be true for a semi-major axis change of course). In order to take this drift
 * into account, an instance of {@link fr.cnes.sirius.patrius.propagation.analytical.J2DifferentialEffect
 * J2DifferentialEffect} must be used together with an instance of this class.
 * </p>
 * 
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class SmallManeuverAnalyticalModel
    implements AdapterPropagator.DifferentialEffect, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 5046690115016896090L;

    /** Transition eccentricity between equinoctial and keplerian parameters. */
    private static final double E_EQUI_KEP = 0.9;

    /** Small DT. */
    private static final double DT = 1.0e-5;

    /** State at maneuver date (before maneuver occurrence). */
    private final SpacecraftState state0;

    /** Inertial velocity increment. */
    private final Vector3D inertialDV;

    /** Mass change ratio. */
    private final double massRatio;

    /** Type of orbit used for internal Jacobians. */
    private final OrbitType type;

    /** Initial Keplerian (or equinoctial) Jacobian with respect to maneuver. */
    private final double[][] j0;

    /** Time derivative of the initial Keplerian (or equinoctial) Jacobian with respect to maneuver. */
    private double[][] j0Dot;

    /** Mean anomaly change factor. */
    private final double ksi;

    /** Name of the part firing. */
    private final String partName;

    /**
     * Build a maneuver defined in spacecraft frame.
     * 
     * @param state0In
     *        state at maneuver date, <em>before</em> the maneuver
     *        is performed
     * @param dV
     *        velocity increment in spacecraft frame
     * @param isp
     *        engine specific impulse (s)
     * @param partNameIn
     *        the part of the mass provider whose mass diminishes
     * @exception PatriusException
     *            if spacecraft frame cannot be transformed
     */
    public SmallManeuverAnalyticalModel(final SpacecraftState state0In,
        final Vector3D dV, final double isp, final String partNameIn) throws PatriusException {
        this(state0In, state0In.getFrame(),
            state0In.getAttitude().getRotation().applyTo(dV),
            isp, partNameIn);
    }

    /**
     * Build a maneuver defined in user-specified frame.
     * 
     * @param state0In
     *        state at maneuver date, <em>before</em> the maneuver
     *        is performed
     * @param frame
     *        frame in which velocity increment is defined
     * @param dV
     *        velocity increment in specified frame
     * @param isp
     *        engine specific impulse (s)
     * @param partNameIn
     *        the part of the mass provider whose mass diminishes
     * @exception PatriusException
     *            if velocity increment frame cannot be transformed
     */
    public SmallManeuverAnalyticalModel(final SpacecraftState state0In, final Frame frame,
        final Vector3D dV, final double isp, final String partNameIn) throws PatriusException {

        this.state0 = state0In;
        this.massRatio = MathLib.exp(-dV.getNorm() / (Constants.G0_STANDARD_GRAVITY * isp));
        this.partName = partNameIn;
        // use equinoctial orbit type if possible, Keplerian if nearly hyperbolic orbits
        this.type = (state0In.getE() < E_EQUI_KEP) ? OrbitType.EQUINOCTIAL : OrbitType.KEPLERIAN;

        // compute initial Jacobian
        final double[][] fullJacobian = new double[6][6];
        this.j0 = new double[6][3];
        final Orbit orbit0 = this.type.convertType(state0In.getOrbit());
        orbit0.getJacobianWrtCartesian(PositionAngle.MEAN, fullJacobian);
        for (int i = 0; i < this.j0.length; ++i) {
            System.arraycopy(fullJacobian[i], 3, this.j0[i], 0, 3);
        }

        // use lazy evaluation for j0Dot, as it is used only when Jacobians are evaluated
        this.j0Dot = null;

        // compute maneuver effect on Keplerian (or equinoctial) elements
        this.inertialDV = frame.getTransformTo(state0In.getFrame(), state0In.getDate()).transformVector(dV);

        // compute mean anomaly change: dM(t1) = dM(t0) + ksi * da * (t1 - t0)
        final double mu = state0In.getMu();
        final double a = state0In.getA();
        this.ksi = - ( 3. / 2. * MathLib.sqrt(mu / a) / (a * a));

    }

    /**
     * Get the date of the maneuver.
     * 
     * @return date of the maneuver
     */
    public AbsoluteDate getDate() {
        return this.state0.getDate();
    }

    /**
     * Get the inertial velocity increment of the maneuver.
     * 
     * @return velocity increment in a state-dependent inertial frame
     * @see #getInertialFrame()
     */
    public Vector3D getInertialDV() {
        return this.inertialDV;
    }

    /**
     * Get the inertial frame in which the velocity increment is defined.
     * 
     * @return inertial frame in which the velocity increment is defined
     * @see #getInertialDV()
     */
    public Frame getInertialFrame() {
        return this.state0.getFrame();
    }

    /**
     * Compute the effect of the maneuver on an orbit.
     * 
     * @param orbit1
     *        original orbit at t<sub>1</sub>, without maneuver
     * @return orbit at t<sub>1</sub>, taking the maneuver
     *         into account if t<sub>1</sub> &gt; t<sub>0</sub>
     * @see #apply(SpacecraftState)
     * @see #getJacobian(Orbit, PositionAngle, double[][])
     */
    public Orbit apply(final Orbit orbit1) {

        if (orbit1.getDate().compareTo(this.state0.getDate()) <= 0) {
            // the maneuver has not occurred yet, don't change anything
            return orbit1;
        }

        return this.updateOrbit(orbit1);

    }

    /**
     * Compute the effect of the maneuver on a spacecraft state.
     * 
     * @param state1
     *        original spacecraft state at t<sub>1</sub>,
     *        without maneuver
     * @return spacecraft state at t<sub>1</sub>, taking the maneuver
     *         into account if t<sub>1</sub> &gt; t<sub>0</sub>
     * @throws PatriusException
     *         if no attitude information is defined
     * @see #apply(Orbit)
     * @see #getJacobian(Orbit, PositionAngle, double[][])
     */
    @Override
    public SpacecraftState apply(final SpacecraftState state1) throws PatriusException {

        if (state1.getDate().compareTo(this.state0.getDate()) <= 0) {
            // the maneuver has not occurred yet, don't change anything
            return state1;
        }
        final Map<String, double[]> addStates = state1.getAdditionalStates();
        final String name = SpacecraftState.MASS + this.partName;
        addStates.put(name, new double[] { this.updateMass(addStates.get(name)[0]) });

        return new SpacecraftState(this.updateOrbit(state1.getOrbit()), state1.getAttitudeForces(),
            state1.getAttitudeEvents(), addStates);

    }

    /**
     * Compute the effect of the maneuver on an orbit.
     * 
     * @param orbit1
     *        original orbit at t<sub>1</sub>, without maneuver
     * @return orbit at t<sub>1</sub>, always taking the maneuver into account
     */
    private Orbit updateOrbit(final Orbit orbit1) {

        // compute maneuver effect
        final double dt = orbit1.getDate().durationFrom(this.state0.getDate());
        final double x = this.inertialDV.getX();
        final double y = this.inertialDV.getY();
        final double z = this.inertialDV.getZ();
        final double[] delta = new double[6];
        for (int i = 0; i < delta.length; ++i) {
            delta[i] = this.j0[i][0] * x + this.j0[i][1] * y + this.j0[i][2] * z;
        }
        delta[5] += this.ksi * delta[0] * dt;

        // convert current orbital state to Keplerian or equinoctial elements
        final double[] parameters = new double[6];
        this.type.mapOrbitToArray(this.type.convertType(orbit1), PositionAngle.MEAN, parameters);
        for (int i = 0; i < delta.length; ++i) {
            parameters[i] += delta[i];
        }

        // build updated orbit as Keplerian or equinoctial elements
        final Orbit o = this.type.mapArrayToOrbit(parameters, PositionAngle.MEAN,
            orbit1.getDate(), orbit1.getMu(),
            orbit1.getFrame());

        // convert to required type
        return orbit1.getType().convertType(o);

    }

    /**
     * Compute the Jacobian of the orbit with respect to maneuver parameters.
     * <p>
     * The Jacobian matrix is a 6x4 matrix. Element jacobian[i][j] corresponds to the partial derivative of orbital
     * parameter i with respect to maneuver parameter j. The rows order is the same order as used in
     * {@link Orbit#getJacobianWrtCartesian(PositionAngle, double[][]) Orbit.getJacobianWrtCartesian} method. Columns
     * (0, 1, 2) correspond to the velocity increment coordinates (&Delta;V<sub>x</sub>, &Delta;V<sub>y</sub>,
     * &Delta;V<sub>z</sub>) in the inertial frame returned by {@link #getInertialFrame()}, and column 3 corresponds to
     * the maneuver date t<sub>0</sub>.
     * </p>
     * 
     * @param orbit1
     *        original orbit at t<sub>1</sub>, without maneuver
     * @param positionAngle
     *        type of the position angle to use
     * @param jacobian
     *        placeholder 6x4 (or larger) matrix to be filled with the Jacobian, if matrix
     *        is larger than 6x4, only the 6x4 upper left corner will be modified
     * @see #apply(Orbit)
     * @exception PatriusException
     *            if time derivative of the initial Jacobian cannot be computed
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    public void getJacobian(final Orbit orbit1, final PositionAngle positionAngle,
                            final double[][] jacobian) throws PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Basic check
        // Get maneuver duration
        final double dt = orbit1.getDate().durationFrom(this.state0.getDate());
        if (dt < 0) {
            // the maneuver has not occurred yet, Jacobian is null
            for (int i = 0; i < 6; ++i) {
                Arrays.fill(jacobian[i], 0, 4, 0.0);
            }
            // Immdediate return
            return;
        }

        // derivatives of Keplerian/equinoctial elements with respect to velocity increment
        final double x = this.inertialDV.getX();
        final double y = this.inertialDV.getY();
        final double z = this.inertialDV.getZ();
        for (int i = 0; i < 6; ++i) {
            System.arraycopy(this.j0[i], 0, jacobian[i], 0, 3);
        }
        for (int j = 0; j < 3; ++j) {
            jacobian[5][j] += this.ksi * dt * this.j0[0][j];
        }

        // derivatives of Keplerian/equinoctial elements with respect to date
        this.evaluateJ0Dot();
        for (int i = 0; i < 6; ++i) {
            jacobian[i][3] = this.j0Dot[i][0] * x + this.j0Dot[i][1] * y + this.j0Dot[i][2] * z;
        }
        final double da = this.j0[0][0] * x + this.j0[0][1] * y + this.j0[0][2] * z;
        jacobian[5][3] += this.ksi * (jacobian[0][3] * dt - da);

        // Check if conversion needed
        if (orbit1.getType() != this.type || positionAngle != PositionAngle.MEAN) {

            // convert to derivatives of cartesian parameters
            final double[][] j2 = new double[6][6];
            final double[][] pvJacobian = new double[6][4];
            final Orbit updated = this.updateOrbit(orbit1);
            this.type.convertType(updated).getJacobianWrtParameters(PositionAngle.MEAN, j2);
            // Loop on all parameters
            for (int i = 0; i < 6; ++i) {
                for (int j = 0; j < 4; ++j) {
                    pvJacobian[i][j] = j2[i][0] * jacobian[0][j] + j2[i][1] * jacobian[1][j] +
                        j2[i][2] * jacobian[2][j] + j2[i][3] * jacobian[3][j] +
                        j2[i][4] * jacobian[4][j] + j2[i][5] * jacobian[5][j];
                }
            }

            // convert to derivatives of specified parameters
            final double[][] j3 = new double[6][6];
            updated.getJacobianWrtCartesian(positionAngle, j3);
            // Loop on all parameters
            for (int j = 0; j < 4; ++j) {
                for (int i = 0; i < 6; ++i) {
                    jacobian[i][j] = j3[i][0] * pvJacobian[0][j] + j3[i][1] * pvJacobian[1][j] +
                        j3[i][2] * pvJacobian[2][j] + j3[i][3] * pvJacobian[3][j] +
                        j3[i][4] * pvJacobian[4][j] + j3[i][5] * pvJacobian[5][j];
                }
            }
        }
    }

    /**
     * Lazy evaluation of the initial Jacobian time derivative.
     * 
     * @exception PatriusException
     *            if initial orbit cannot be shifted
     */
    private void evaluateJ0Dot() throws PatriusException {

        if (this.j0Dot == null) {
            // Initialization
            this.j0Dot = new double[6][3];
            final double dt = DT / this.state0.getKeplerianMeanMotion();
            final Orbit orbit = this.type.convertType(this.state0.getOrbit());

            // compute shifted Jacobians
            final double[][] j0m1 = new double[6][6];
            orbit.shiftedBy(-1 * dt).getJacobianWrtCartesian(PositionAngle.MEAN, j0m1);
            final double[][] j0p1 = new double[6][6];
            orbit.shiftedBy(+1 * dt).getJacobianWrtCartesian(PositionAngle.MEAN, j0p1);

            // evaluate derivative by finite differences
            for (int i = 0; i < this.j0Dot.length; ++i) {
                final double[] m1Row = j0m1[i];
                final double[] p1Row = j0p1[i];
                final double[] j0DotRow = this.j0Dot[i];
                for (int j = 0; j < 3; ++j) {
                    j0DotRow[j] = (p1Row[j + 3] - m1Row[j + 3]) / (2 * dt);
                }
            }
        }

        // No result to return
        // Class variable is updated
    }

    /**
     * Update a spacecraft mass due to maneuver.
     * 
     * @param mass
     *        masse before maneuver
     * @return mass after maneuver
     */
    public double updateMass(final double mass) {
        return this.massRatio * mass;
    }
}
