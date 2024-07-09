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
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
* VERSION:4.4:FA:FA-2251:04/10/2019:[PATRIUS] Propagateurs analytique 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:163:12/03/2014:Introduced a public computeMeanOrbit method
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass issues
 * VERSION::FA:416:12/02/2015:Changed EcksteinHechlerPropagator constructor signature
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:533:26/02/2016:add body frame to Eckstein-Heschler propagator
 * VERSION::FA:568:26/02/2016:add setter on convergence threshold
 * VERSION::FA:665:28/07/2016:forbid non-inertial frames
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:1417:13/03/2018:correct 1 attitude case
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical;

import java.io.Serializable;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.MeanOsculatingElementsProvider;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

//CHECKSTYLE: stop MagicNumber check
//Reason: model - Orekit code

/**
 * This class propagates a {@link fr.cnes.sirius.patrius.propagation.SpacecraftState} using the
 * analytical Eckstein-Hechler model.
 * <p>
 * The Eckstein-Hechler model is suited for near circular orbits (e < 0.1, with poor accuracy between 0.005 and 0.1) and
 * inclination neither equatorial (direct or retrograde) nor critical (direct or retrograde).
 * </p>
 * 
 * @see Orbit
 * @author Guylaine Prat
 */
public class EcksteinHechlerPropagator extends AbstractPropagator implements
    MeanOsculatingElementsProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 1268374325750125229L;

    /** Maximum number of iterations. */
    private static final int MAX_ITER = 100;

    /** Default relative convergence threshold for osculating to mean conversion. */
    private static final double DEFAULT_THRESHOLD = 1E-13;

    /** Relative convergence threshold for osculating to mean conversion. */
    private static double threshold = DEFAULT_THRESHOLD;

    /** Reference radius of the central body attraction model (m). */
    private final double referenceRadius;

    /** Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /** Un-normalized zonal coefficient (about -1.08e-3 for Earth). */
    private final double c20;

    /** Un-normalized zonal coefficient (about +2.53e-6 for Earth). */
    private final double c30;

    /** Un-normalized zonal coefficient (about +1.62e-6 for Earth). */
    private final double c40;

    /** Un-normalized zonal coefficient (about +2.28e-7 for Earth). */
    private final double c50;

    /** Un-normalized zonal coefficient (about -5.41e-7 for Earth). */
    private final double c60;

    /** Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     * polar axis of the body. */
    private final Frame frame;

    /** Propagation parameters : replaces old mean, q, ql, g1 ... g6, cos, 4 x sim parameters */
    private PropagationParameters internalParameters;

    /**
     * Build a propagator from orbit and potential.
     * <p>
     * Mass and attitude provider are set to null value.
     * </p>
     * <p>
     * The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they are related to both the normalized
     * coefficients <span style="text-decoration: overline">C</span><sub>n,0</sub> and the J<sub>n</sub> one as follows:
     * </p>
     * 
     * <pre>
     *   C<sub>n,0</sub> = [(2-&delta;<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>&frac12;</sup>
     *   <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     * </pre>
     * 
     * @param initialOrbit initial orbit
     * @param referenceRadiusIn reference radius of the Earth for the potential model (m)
     * @param muIn central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) used for propagation
     * @param frame
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param c20In un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30In un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40In un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50In un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60In un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @param paramsType parameters type (mean or osculating)
     * @exception PropagationException if the mean parameters cannot be computed or coefficients
     *            frame is not inertial
     * @see fr.cnes.sirius.patrius.utils.Constants
     * @see ParametersType
     */
    public EcksteinHechlerPropagator(final Orbit initialOrbit, final double referenceRadiusIn,
        final double muIn, final Frame frame, final double c20In, final double c30In,
        final double c40In, final double c50In, final double c60In,
        final ParametersType paramsType) throws PropagationException {
        this(initialOrbit, null, null, referenceRadiusIn, muIn, frame, c20In, c30In, c40In, c50In,
            c60In, null, paramsType);
    }

    /**
     * Build a propagator from orbit, mass and potential.
     * <p>
     * Attitude law is set to null value.
     * </p>
     * <p>
     * The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they are related to both the normalized
     * coefficients <span style="text-decoration: overline">C</span><sub>n,0</sub> and the J<sub>n</sub> one as follows:
     * </p>
     * 
     * <pre>
     *   C<sub>n,0</sub> = [(2-&delta;<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>&frac12;</sup>
     *   <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     * </pre>
     * 
     * @param initialOrbit initial orbit
     * @param referenceRadiusIn reference radius of the Earth for the potential model (m)
     * @param muIn central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) used for propagation
     * @param frame
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param c20In un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30In un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40In un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50In un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60In un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @param massProvider spacecraft mass provider
     * @param paramsType parameters type (mean or osculating)
     * @exception PropagationException if the mean parameters cannot be computed or coefficients
     *            frame is not inertial
     */
    public EcksteinHechlerPropagator(final Orbit initialOrbit, final double referenceRadiusIn,
        final double muIn, final Frame frame, final double c20In, final double c30In,
        final double c40In, final double c50In, final double c60In,
        final MassProvider massProvider, final ParametersType paramsType)
        throws PropagationException {
        this(initialOrbit, null, referenceRadiusIn, muIn, frame, c20In, c30In, c40In, c50In, c60In,
            massProvider, paramsType);
    }

    /**
     * Build a propagator from orbit, attitude provider and potential.
     * <p>
     * Mass is set to null value.
     * </p>
     * <p>
     * The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they are related to both the normalized
     * coefficients <span style="text-decoration: overline">C</span><sub>n,0</sub> and the J<sub>n</sub> one as follows:
     * </p>
     * 
     * <pre>
     *   C<sub>n,0</sub> = [(2-&delta;<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>&frac12;</sup>
     *   <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     * </pre>
     * 
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param referenceRadiusIn reference radius of the Earth for the potential model (m)
     * @param muIn central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) used for propagation
     * @param frame
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param c20In un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30In un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40In un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50In un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60In un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @param paramsType parameters type (mean or osculating)
     * @exception PropagationException if the mean parameters cannot be computed or coefficients
     *            frame is not inertial
     */
    public EcksteinHechlerPropagator(final Orbit initialOrbit, final AttitudeProvider attitudeProv,
        final double referenceRadiusIn, final double muIn, final Frame frame,
        final double c20In, final double c30In, final double c40In, final double c50In,
        final double c60In, final ParametersType paramsType) throws PropagationException {
        this(initialOrbit, attitudeProv, referenceRadiusIn, muIn, frame, c20In, c30In, c40In,
            c50In, c60In, null, paramsType);
    }

    /**
     * Build a propagator from orbit, attitude provider for forces and events computation and
     * potential.
     * <p>
     * Mass is set to an unspecified non-null arbitrary value.
     * </p>
     * <p>
     * The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they are related to both the normalized
     * coefficients <span style="text-decoration: overline">C</span><sub>n,0</sub> and the J<sub>n</sub> one as follows:
     * </p>
     * 
     * <pre>
     *   C<sub>n,0</sub> = [(2-&delta;<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>&frac12;</sup>
     *   <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     * </pre>
     * 
     * @param initialOrbit initial orbit
     * @param attitudeProvForces attitude provider for forces computation
     * @param attitudeProvEvents attitude provider for events computation
     * @param referenceRadiusIn reference radius of the Earth for the potential model (m)
     * @param muIn central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) used for propagation
     * @param frame
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param c20In un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30In un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40In un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50In un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60In un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @param paramsType parameters type (mean or osculating)
     * @exception PropagationException if the mean parameters cannot be computed or coefficients
     *            frame is not inertial
     */
    public EcksteinHechlerPropagator(final Orbit initialOrbit,
        final AttitudeProvider attitudeProvForces, final AttitudeProvider attitudeProvEvents,
        final double referenceRadiusIn, final double muIn, final Frame frame,
        final double c20In, final double c30In, final double c40In, final double c50In,
        final double c60In, final ParametersType paramsType) throws PropagationException {
        this(initialOrbit, attitudeProvForces, attitudeProvEvents, referenceRadiusIn, muIn, frame,
            c20In, c30In, c40In, c50In, c60In, null, paramsType);
    }

    /**
     * Build a propagator from orbit, attitude provider, mass and potential.
     * <p>
     * The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they are related to both the normalized
     * coefficients <span style="text-decoration: overline">C</span><sub>n,0</sub> and the J<sub>n</sub> one as follows:
     * </p>
     * 
     * <pre>
     *   C<sub>n,0</sub> = [(2-&delta;<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>&frac12;</sup>
     *   <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     * </pre>
     * 
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param referenceRadiusIn reference radius of the Earth for the potential model (m)
     * @param muIn central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) used for propagation
     * @param frame
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param c20In un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30In un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40In un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50In un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60In un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @param massProvider spacecraft mass provider
     * @param paramsType parameters type (mean or osculating)
     * @exception PropagationException if the mean parameters cannot be computed or coefficients
     *            frame is not inertial
     */
    public EcksteinHechlerPropagator(final Orbit initialOrbit, final AttitudeProvider attitudeProv,
        final double referenceRadiusIn, final double muIn, final Frame frame,
        final double c20In, final double c30In, final double c40In, final double c50In,
        final double c60In, final MassProvider massProvider, final ParametersType paramsType)
        throws PropagationException {

        super(attitudeProv);

        try {
            // store model coefficients
            this.referenceRadius = referenceRadiusIn;
            this.mu = muIn;
            if (!frame.isPseudoInertial()) {
                // If frame is not pseudo-inertial, an exception is thrown
                throw new PropagationException(PatriusMessages.NOT_INERTIAL_FRAME);
            }
            this.frame = frame;
            this.c20 = c20In;
            this.c30 = c30In;
            this.c40 = c40In;
            this.c50 = c50In;
            this.c60 = c60In;

            // Compute and store mean parameters
            this.updateInternalParameters(initialOrbit, paramsType);

            // Initialize propagation frame as initial orbit frame (compulsory for upcoming attitude
            // computation)
            this.setOrbitFrame(initialOrbit.getFrame());

            // Treatment in case attitude provider is null
            Attitude attitude = null;
            if (this.getAttitudeProvider() != null) {
                attitude =
                    attitudeProv.getAttitude(this.getPvProvider(), initialOrbit.getDate(),
                        initialOrbit.getFrame());
            }
            super.resetInitialState(new SpacecraftState(initialOrbit, attitude, massProvider));
            this.addAdditionalStateProvider(massProvider);

        } catch (final PatriusException oe) {
            throw new PropagationException(oe);
        }
    }

    /**
     * Build a propagator from orbit, attitude provider, mass and potential.
     * <p>
     * The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they are related to both the normalized
     * coefficients <span style="text-decoration: overline">C</span><sub>n,0</sub> and the J<sub>n</sub> one as follows:
     * </p>
     * 
     * <pre>
     *   C<sub>n,0</sub> = [(2-&delta;<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>&frac12;</sup>
     *   <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     * </pre>
     * 
     * @param initialOrbit initial orbit
     * @param attitudeProvForces attitude provider for forces computation
     * @param attitudeProvEvents attitude provider for events computation
     * @param referenceRadiusIn reference radius of the Earth for the potential model (m)
     * @param muIn central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) used for propagation
     * @param frame
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param c20In un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30In un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40In un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50In un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60In un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @param massProvider spacecraft mass provider
     * @param paramsType parameters type
     * @exception PropagationException if the mean parameters cannot be computed or coefficients
     *            frame is not inertial
     */
    public EcksteinHechlerPropagator(final Orbit initialOrbit,
        final AttitudeProvider attitudeProvForces, final AttitudeProvider attitudeProvEvents,
        final double referenceRadiusIn, final double muIn, final Frame frame,
        final double c20In, final double c30In, final double c40In, final double c50In,
        final double c60In, final MassProvider massProvider, final ParametersType paramsType)
        throws PropagationException {

        super(attitudeProvForces, attitudeProvEvents);
        try {

            // store model coefficients
            this.referenceRadius = referenceRadiusIn;
            this.mu = muIn;
            if (!frame.isPseudoInertial()) {
                // If frame is not pseudo-inertial, an exception is thrown
                throw new PropagationException(PatriusMessages.NOT_INERTIAL_FRAME);
            }
            this.frame = frame;
            this.c20 = c20In;
            this.c30 = c30In;
            this.c40 = c40In;
            this.c50 = c50In;
            this.c60 = c60In;

            // Compute and store mean parameters
            this.updateInternalParameters(initialOrbit, paramsType);

            // Initialize propagation frame as initial orbit frame (compulsory for upcoming attitude
            // computation)
            this.setOrbitFrame(initialOrbit.getFrame());

            // Treatment in case attitude providers are null
            Attitude attitudeForces = null;
            Attitude attitudeEvents = null;
            if (this.getAttitudeProviderForces() != null) {
                attitudeForces =
                    attitudeProvForces.getAttitude(this.getPvProvider(), initialOrbit.getDate(),
                        initialOrbit.getFrame());
            }
            if (this.getAttitudeProviderEvents() != null) {
                attitudeEvents =
                    attitudeProvEvents.getAttitude(this.getPvProvider(), initialOrbit.getDate(),
                        initialOrbit.getFrame());
            }

            super.resetInitialState(new SpacecraftState(initialOrbit, attitudeForces,
                attitudeEvents, massProvider));
            this.addAdditionalStateProvider(massProvider);
        } catch (final PatriusException oe) {
            throw new PropagationException(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        super.resetInitialState(state);
        // State is osculating
        try {
            this.updateInternalParameters(state.getOrbit(), ParametersType.OSCULATING);
        } catch (final PatriusException e) {
            // Wrap exception
            throw new PropagationException(e);
        }
    }

    /**
     * Update mean internal parameters (in body frame).
     * 
     * @param orbit orbit
     * @param orbitParametersType parameters type
     * @throws PatriusException thrown if mean elements computation failed
     */
    private void updateInternalParameters(final Orbit orbit,
                                          final ParametersType orbitParametersType) throws PatriusException {
        final CircularOrbit circOrbit = new CircularOrbit(orbit);

        if (orbitParametersType.equals(ParametersType.OSCULATING)) {
            // Compute internal parameters
            this.internalParameters = this.computeMeanParametersInternal(circOrbit);
        } else {
            // store directly internal parameters
            this.internalParameters = this.buildInternalParameters(circOrbit);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException {
        try {
            return this.propagateOrbitInternalCircular(date, this.internalParameters,
                ParametersType.OSCULATING, this.getFrame(), this.mu);
        } catch (final PatriusException e) {
            // Wrap exception
            throw new PropagationException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Orbit propagateMeanOrbit(final AbsoluteDate date) throws PatriusException {
        return this.propagateOrbitInternalCircular(date, this.internalParameters, ParametersType.MEAN,
            this.getFrame(), this.mu);
    }

    /** {@inheritDoc} */
    @Override
    public Orbit osc2mean(final Orbit orbit) throws PatriusException {
        final CircularOrbit circOrbit =
            this.computeMeanParametersInternal(new CircularOrbit(orbit)).getMean();
        return orbit.getType().convertType(this.convertFrame(circOrbit, orbit.getFrame()));
    }

    /** {@inheritDoc} */
    @Override
    public Orbit mean2osc(final Orbit orbit) throws PatriusException {
        final PropagationParameters parameters = this.buildInternalParameters(new CircularOrbit(orbit));
        final Orbit circOrbit =
            this.propagateOrbitInternalCircular(orbit.getDate(), parameters,
                ParametersType.OSCULATING, orbit.getFrame(), orbit.getMu());
        return orbit.getType().convertType(circOrbit);
    }

    /**
     * Build internal parameters.
     * 
     * @param orbit an orbit
     * @return internal parameters
     * @throws PatriusException thrown if conversion into body frame failed
     */
    private PropagationParameters buildInternalParameters(final CircularOrbit orbit)
                                                                                    throws PatriusException {

        // Get orbit in body frame
        final CircularOrbit orbitBody = this.convertFrame(orbit, this.frame);

        // Initialization
        final double q = this.referenceRadius / orbitBody.getA();
        double ql = q * q;
        final double g2 = this.c20 * ql;
        ql *= q;
        final double g3 = this.c30 * ql;
        ql *= q;
        final double g4 = this.c40 * ql;
        ql *= q;
        final double g5 = this.c50 * ql;
        ql *= q;
        final double g6 = this.c60 * ql;

        // cos/sin
        final double[] sincosI = MathLib.sinAndCos(orbitBody.getI());
        final double cosI1 = sincosI[1];
        final double sinI1 = sincosI[0];
        final double sinI2 = sinI1 * sinI1;
        final double sinI4 = sinI2 * sinI2;
        final double sinI6 = sinI2 * sinI4;

        // Return result
        //
        return new PropagationParameters(orbitBody, g2, g3, g4, g5, g6, cosI1, sinI1, sinI2, sinI4,
            sinI6);
    }

    /**
     * Compute mean parameters according to the Eckstein-Hechler analytical model. This method has
     * been modified in order not to change any instance varibales, and return instead the computed
     * parameters as a {@link PropagationParameters} object.
     * 
     * @param osculating osculating orbit
     * @return a parameters object
     * @exception PatriusException if orbit goes outside of supported range (too eccentric,
     *            equatorial, critical inclination) or if convergence cannot be reached.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    private PropagationParameters computeMeanParametersInternal(final CircularOrbit osculating)
                                                                                               throws PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Initialization
        final CircularOrbit osculatingOrbit = this.convertFrame(osculating, this.frame);

        // Threshold for each parameter
        final double[] tol = {
            threshold * (1 + MathLib.abs(osculatingOrbit.getA())),
            threshold * (1 + osculatingOrbit.getE()),
            threshold * (1 + osculatingOrbit.getE()), threshold * FastMath.PI,
            threshold * FastMath.PI, threshold * FastMath.PI };

        // Mean parameter initialization to osculating parameters
        CircularOrbit mean = new CircularOrbit(osculatingOrbit);

        // Loop until convergence
        int iter = 0;
        while (iter < MAX_ITER) {

            // Recompute the osculating parameters from the current mean parameters
            final PropagationParameters params = this.buildInternalParameters(mean);
            final CircularOrbit newMean;
            try {
                newMean =
                    this.propagateOrbitInternalCircular(mean.getDate(), params,
                        ParametersType.OSCULATING, mean.getFrame(), mean.getMu());
            } catch (final ArithmeticException e) {
                // Some terms cannot be computed (Double.NaN)
                throw new PropagationException(e,
                    PatriusMessages.UNABLE_TO_COMPUTE_ECKSTEIN_HECHLER_MEAN_PARAMETERS, iter);
            }

            // Parameters residuals
            final double[] delta = {
                newMean.getA() - osculatingOrbit.getA(),
                newMean.getCircularEx() - osculatingOrbit.getCircularEx(),
                newMean.getCircularEy() - osculatingOrbit.getCircularEy(),
                newMean.getI() - osculatingOrbit.getI(),
                MathUtils.normalizeAngle(newMean.getRightAscensionOfAscendingNode()
                    - osculatingOrbit.getRightAscensionOfAscendingNode(), 0.),
                MathUtils.normalizeAngle(newMean.getAlphaM()
                    - osculatingOrbit.getAlphaM(), 0.0) };

            // Update mean parameters
            mean = new CircularOrbit(mean.getA() - delta[0], mean.getCircularEx() - delta[1],
                mean.getCircularEy() - delta[2], mean.getI() - delta[3],
                mean.getRightAscensionOfAscendingNode() - delta[4],
                mean.getAlphaM() - delta[5], PositionAngle.MEAN, mean.getFrame(),
                mean.getDate(), mean.getMu());

            // Check convergence
            boolean interrupt = true;
            for (int i = 0; i < delta.length; i++) {
                interrupt &= MathLib.abs(delta[i]) < tol[i];
            }
            if (interrupt) {
                // sanity checks
                final double e = mean.getE();
                if (e > 0.1) {
                    // if 0.005 < e < 0.1 no error is triggered, but accuracy is poor
                    throw new PropagationException(
                        PatriusMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL, e);
                }

                final double meanI = mean.getI();
                if ((meanI < 0.) || (meanI > FastMath.PI)
                    || (MathLib.abs(MathLib.sin(meanI)) < 1.0e-10)) {
                    throw new PropagationException(PatriusMessages.ALMOST_EQUATORIAL_ORBIT,
                        MathLib.toDegrees(meanI));
                }

                if ((MathLib.abs(meanI - 1.1071487) < 1.0e-3)
                    || (MathLib.abs(meanI - 2.0344439) < 1.0e-3)) {
                    throw new PropagationException(
                        PatriusMessages.ALMOST_CRITICALLY_INCLINED_ORBIT,
                        MathLib.toDegrees(meanI));
                }

                // Return result
                return new PropagationParameters(mean, params.getG2(), params.getG3(),
                    params.getG4(), params.getG5(), params.getG6(), params.getCosI1(),
                    params.getSinI1(), params.getSinI2(), params.getSinI4(), params.getSinI6());
            }
            // Update loop counter
            iter++;
        }

        // Algorithm did not converge
        throw new PropagationException(
            PatriusMessages.UNABLE_TO_COMPUTE_ECKSTEIN_HECHLER_MEAN_PARAMETERS, iter);
    }

    /**
     * Internal propagate method. It propagates the parameters passed as arguments.
     * 
     * @param date target date
     * @param params propagation parameters (with orbit in body frame)
     * @param outputType output parameters type
     * @param propagationFrame propagation frame
     * @param orbitMu orbit mu
     * @return the propagated orbit
     * @throws PatriusException thrown if frame conversion failed
     */
    private CircularOrbit
            propagateOrbitInternalCircular(final AbsoluteDate date,
                                           final PropagationParameters params, final ParametersType outputType,
                                           final Frame propagationFrame, final double orbitMu) throws PatriusException {
        final CircularOrbit orbit = this.propagateOrbitInternal(date, params, outputType);

        // Perform frame transformation since output of method propagateOrbitInternal is in
        // bodyFrame
        return this.convertFrame(orbit, propagationFrame);
    }

    /**
     * Internal propagate method. It propagates the parameters passed as arguments.
     * 
     * @param date target date
     * @param params propagation parameters (with orbit in body frame)
     * @param outputType output parameters type
     * @return the propagated orbit
     * @throws PatriusException thrown if frame conversion failed
     */
    // CHECKSTYLE: stop MethodLength check
    // Reason: Orekit code kept as such
    private CircularOrbit propagateOrbitInternal(final AbsoluteDate date, final PropagationParameters params,
            final ParametersType outputType) throws PatriusException {
        // CHECKSTYLE: resume MethodLength check

        // In this method, orbit is already expressed in body frame

        // However final transformation from body frame to initial frame is not performed in this
        // method since
        // this method does not return orbit objects (transformation is then performed in methods
        // propagateOrbitInternalCartesian and propagateOrbitInternalCircular)
        final CircularOrbit mean = params.getMean();

        final double g2 = params.getG2();
        final double g3 = params.getG3();
        final double g4 = params.getG4();
        final double g5 = params.getG5();
        final double g6 = params.getG6();
        final double cosI1 = params.getCosI1();
        final double sinI1 = params.getSinI1();
        final double sinI2 = params.getSinI2();
        final double sinI4 = params.getSinI4();
        final double sinI6 = params.getSinI6();

        // keplerian evolution
        final double xnot =
            date.durationFrom(mean.getDate()) * MathLib.sqrt(this.mu / mean.getA()) / mean.getA();

        // secular effects

        // eccentricity
        final double rdpom = -0.75 * g2 * (4.0 - 5.0 * sinI2);
        final double rdpomp =
            7.5 * g4 * (1.0 - 31.0 / 8.0 * sinI2 + 49.0 / 16.0 * sinI4) - 13.125 * g6
                * (1.0 - 8.0 * sinI2 + 129.0 / 8.0 * sinI4 - 297.0 / 32.0 * sinI6);
        final double x = (rdpom + rdpomp) * xnot;
        final double[] sincosx = MathLib.sinAndCos(x);
        final double cx = sincosx[1];
        final double sx = sincosx[0];
        double q = 3.0 / (32.0 * rdpom);
        final double eps1 =
            q * g4 * sinI2 * (30.0 - 35.0 * sinI2) - 175.0 * q * g6 * sinI2
                * (1.0 - 3.0 * sinI2 + 2.0625 * sinI4);
        q = 3.0 * sinI1 / (8.0 * rdpom);
        final double eps2 =
            q * g3 * (4.0 - 5.0 * sinI2) - q * g5 * (10.0 - 35.0 * sinI2 + 26.25 * sinI4);
        final double exm =
            mean.getCircularEx() * cx - (1.0 - eps1) * (mean.getCircularEy() - eps2) * sx;
        final double eym =
            (1.0 + eps1) * mean.getCircularEx() * sx + (mean.getCircularEy() - eps2) * cx
                + eps2;

        // inclination
        final double xim = mean.getI();

        // right ascension of ascending node
        q =
            1.50 * g2 - 2.25 * g2 * g2 * (2.5 - 19.0 / 6.0 * sinI2) + 0.9375 * g4
                * (7.0 * sinI2 - 4.0) + 3.28125 * g6 * (2.0 - 9.0 * sinI2 + 8.25 * sinI4);
        final double omm =
            MathUtils.normalizeAngle(
                mean.getRightAscensionOfAscendingNode() + q * cosI1 * xnot, FastMath.PI);

        // latitude argument
        final double rdl = 1.0 - 1.50 * g2 * (3.0 - 4.0 * sinI2);
        q =
            rdl + 2.25 * g2 * g2 * (9.0 - 263.0 / 12.0 * sinI2 + 341.0 / 24.0 * sinI4) + 15.0
                / 16.0 * g4 * (8.0 - 31.0 * sinI2 + 24.5 * sinI4) + 105.0 / 32.0 * g6
                * (-10.0 / 3.0 + 25.0 * sinI2 - 48.75 * sinI4 + 27.5 * sinI6);
        final double xlm = MathUtils.normalizeAngle(mean.getAlphaM() + q * xnot, FastMath.PI);

        if (outputType == ParametersType.MEAN) {
            // Mean parameters
            return new CircularOrbit(mean.getA(), exm, eym, xim, MathUtils.normalizeAngle(omm,
                FastMath.PI), MathUtils.normalizeAngle(xlm, FastMath.PI), PositionAngle.MEAN,
                mean.getFrame(), date, mean.getMu());
        } else {
            // Osculating parameters: add short periods

            // periodical terms
            final double[] sincosxlm = MathLib.sinAndCos(xlm);
            final double cl1 = sincosxlm[1];
            final double sl1 = sincosxlm[0];
            final double cl2 = cl1 * cl1 - sl1 * sl1;
            final double sl2 = cl1 * sl1 + sl1 * cl1;
            final double cl3 = cl2 * cl1 - sl2 * sl1;
            final double sl3 = cl2 * sl1 + sl2 * cl1;
            final double cl4 = cl3 * cl1 - sl3 * sl1;
            final double sl4 = cl3 * sl1 + sl3 * cl1;
            final double cl5 = cl4 * cl1 - sl4 * sl1;
            final double sl5 = cl4 * sl1 + sl4 * cl1;
            final double cl6 = cl5 * cl1 - sl5 * sl1;

            final double qq = -1.5 * g2 / rdl;
            final double qh = 0.375 * (eym - eps2) / rdpom;
            final double ql = 0.375 * exm / (sinI1 * rdpom);

            // semi major axis
            double f =
                (2.0 - 3.5 * sinI2) * exm * cl1 + (2.0 - 2.5 * sinI2) * eym * sl1 + sinI2 * cl2
                    + 3.5 * sinI2 * (exm * cl3 + eym * sl3);
            double rda = qq * f;

            q = 0.75 * g2 * g2 * sinI2;
            f = 7.0 * (2.0 - 3.0 * sinI2) * cl2 + sinI2 * cl4;
            rda += q * f;

            q = -0.75 * g3 * sinI1;
            f = (4.0 - 5.0 * sinI2) * sl1 + 5.0 / 3.0 * sinI2 * sl3;
            rda += q * f;

            q = 0.25 * g4 * sinI2;
            f = (15.0 - 17.5 * sinI2) * cl2 + 4.375 * sinI2 * cl4;
            rda += q * f;

            q = 3.75 * g5 * sinI1;
            f =
                (2.625 * sinI4 - 3.5 * sinI2 + 1.0) * sl1 + 7.0 / 6.0 * sinI2
                    * (1.0 - 1.125 * sinI2) * sl3 + 21.0 / 80.0 * sinI4 * sl5;
            rda += q * f;

            q = 105.0 / 16.0 * g6 * sinI2;
            f =
                (3.0 * sinI2 - 1.0 - 33.0 / 16.0 * sinI4) * cl2 + 0.75 * (1.1 * sinI4 - sinI2)
                    * cl4 - 11.0 / 80.0 * sinI4 * cl6;
            rda += q * f;

            // eccentricity
            f =
                (1.0 - 1.25 * sinI2) * cl1 + 0.5 * (3.0 - 5.0 * sinI2) * exm * cl2
                    + (2.0 - 1.5 * sinI2) * eym * sl2 + 7.0 / 12.0 * sinI2 * cl3 + 17.0
                    / 8.0 * sinI2 * (exm * cl4 + eym * sl4);
            final double rdex = qq * f;

            f =
                (1.0 - 1.75 * sinI2) * sl1 + (1.0 - 3.0 * sinI2) * exm * sl2
                    + (2.0 * sinI2 - 1.5) * eym * cl2 + 7.0 / 12.0 * sinI2 * sl3 + 17.0
                    / 8.0 * sinI2 * (exm * sl4 - eym * cl4);
            final double rdey = qq * f;

            // ascending node
            q = -qq * cosI1;
            f = 3.5 * exm * sl1 - 2.5 * eym * cl1 - 0.5 * sl2 + 7.0 / 6.0 * (eym * cl3 - exm * sl3);
            double rdom = q * f;

            f = g3 * cosI1 * (4.0 - 15.0 * sinI2);
            rdom += ql * f;

            f = 2.5 * g5 * cosI1 * (4.0 - 42.0 * sinI2 + 52.5 * sinI4);
            rdom -= ql * f;

            // inclination
            q = 0.5 * qq * sinI1 * cosI1;
            f = eym * sl1 - exm * cl1 + cl2 + 7.0 / 3.0 * (exm * cl3 + eym * sl3);
            double rdxi = q * f;

            f = g3 * cosI1 * (4.0 - 5.0 * sinI2);
            rdxi -= qh * f;

            f = 2.5 * g5 * cosI1 * (4.0 - 14.0 * sinI2 + 10.5 * sinI4);
            rdxi += qh * f;

            // latitude argument
            f =
                (7.0 - 77.0 / 8.0 * sinI2) * exm * sl1 + (55.0 / 8.0 * sinI2 - 7.50) * eym
                    * cl1 + (1.25 * sinI2 - 0.5) * sl2 + (77.0 / 24.0 * sinI2 - 7.0 / 6.0)
                    * (exm * sl3 - eym * cl3);
            double rdxl = qq * f;

            f = g3 * (53.0 * sinI2 - 4.0 - 57.5 * sinI4);
            rdxl += ql * f;

            f = 2.5 * g5 * (4.0 - 96.0 * sinI2 + 269.5 * sinI4 - 183.75 * sinI6);
            rdxl += ql * f;

            // osculating parameters
            return new CircularOrbit(mean.getA() * (1.0 + rda), exm + rdex, eym + rdey,
                xim + rdxi, MathUtils.normalizeAngle(omm + rdom, FastMath.PI),
                MathUtils.normalizeAngle(xlm + rdxl, FastMath.PI), PositionAngle.MEAN,
                mean.getFrame(), date, mean.getMu());
        }
    }

    /**
     * Setter for osculating to mean conversion relative convergence threshold. Default value for
     * this threshold is 1E-13.
     * 
     * @param newThreshold new threshold to set
     */
    public static void setThreshold(final double newThreshold) {
        threshold = newThreshold;
    }

    /**
     * Convert provided orbit in output frame.
     * 
     * @param orbit orbit
     * @param outputFrame output frame
     * @return converted orbit in output frame
     * @throws PatriusException thrown if conversion failed
     */
    private CircularOrbit convertFrame(final CircularOrbit orbit, final Frame outputFrame)
                                                                                          throws PatriusException {
        CircularOrbit res = orbit;
        if (!orbit.getFrame().equals(outputFrame)) {
            // Specific frame transformation to avoid accounting for Earth rotation
            final Transform t = orbit.getFrame().getTransformTo(outputFrame, orbit.getDate());
            final Vector3D pos = t.transformVector(orbit.getPVCoordinates().getPosition());
            final Vector3D vel = t.transformVector(orbit.getPVCoordinates().getVelocity());
            final PVCoordinates pv = new PVCoordinates(pos, vel);

            res = new CircularOrbit(pv, outputFrame, orbit.getDate(), orbit.getMu());
        }
        return res;
    }

    /**
     * This internal class replaces all the old instance variables. Its allows defining an
     * externally useable computeMean method.
     */
    private static class PropagationParameters implements Serializable {

        /** Serial UID. */
        private static final long serialVersionUID = 4769081337411727324L;

        /** Mean parameters at the initial date in body frame. */
        private final CircularOrbit mean;

        /** Preprocessed values. */
        private final double g2;
        /** Preprocessed values. */
        private final double g3;
        /** Preprocessed values. */
        private final double g4;
        /** Preprocessed values. */
        private final double g5;
        /** Preprocessed values. */
        private final double g6;
        /** Preprocessed values. */
        private final double cosI1;
        /** Preprocessed values. */
        private final double sinI1;
        /** Preprocessed values. */
        private final double sinI2;
        /** Preprocessed values. */
        private final double sinI4;
        /** Preprocessed values. */
        private final double sinI6;

        /**
         * Constructor.
         * 
         * @param meanIn orbit in body frame
         * @param g2In propagation parameter
         * @param g3In propagation parameter
         * @param g4In propagation parameter
         * @param g5In propagation parameter
         * @param g6In propagation parameter
         * @param cosI1In propagation parameter
         * @param sinI1In propagation parameter
         * @param sinI2In propagation parameter
         * @param sinI4In propagation parameter
         * @param sinI6In propagation parameter
         */
        public PropagationParameters(final CircularOrbit meanIn, final double g2In,
            final double g3In, final double g4In, final double g5In, final double g6In,
            final double cosI1In, final double sinI1In, final double sinI2In,
            final double sinI4In, final double sinI6In) {
            super();
            this.mean = meanIn;
            this.g2 = g2In;
            this.g3 = g3In;
            this.g4 = g4In;
            this.g5 = g5In;
            this.g6 = g6In;
            this.cosI1 = cosI1In;
            this.sinI1 = sinI1In;
            this.sinI2 = sinI2In;
            this.sinI4 = sinI4In;
            this.sinI6 = sinI6In;
        }

        /**
         * @return mean orbit
         */
        public CircularOrbit getMean() {
            return this.mean;
        }

        /**
         * @return propagation parameter
         */
        public double getG2() {
            return this.g2;
        }

        /**
         * @return propagation parameter
         */
        public double getG3() {
            return this.g3;
        }

        /**
         * @return propagation parameter
         */
        public double getG4() {
            return this.g4;
        }

        /**
         * @return propagation parameter
         */
        public double getG5() {
            return this.g5;
        }

        /**
         * @return propagation parameter
         */
        public double getG6() {
            return this.g6;
        }

        /**
         * @return propagation parameter
         */
        public double getCosI1() {
            return this.cosI1;
        }

        /**
         * @return propagation parameter
         */
        public double getSinI1() {
            return this.sinI1;
        }

        /**
         * @return propagation parameter
         */
        public double getSinI2() {
            return this.sinI2;
        }

        /**
         * @return propagation parameter
         */
        public double getSinI4() {
            return this.sinI4;
        }

        /**
         * @return propagation parameter
         */
        public double getSinI6() {
            return this.sinI6;
        }
    }

    // CHECKSTYLE: resume MagicNumber check
}
