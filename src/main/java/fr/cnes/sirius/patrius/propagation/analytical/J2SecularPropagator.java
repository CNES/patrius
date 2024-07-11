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
 * @history created 15/02/2016
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2563:27/01/2021:[PATRIUS] Ajout de la matrice de transition J2Secular 
 * VERSION:4.6:DM:DM-2556:27/01/2021:PATRIUS] Extension modele lunisolaire dans STELA 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.4:FA:FA-2251:04/10/2019:[PATRIUS] Propagateurs analytique
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::FA:665:28/07/2016:forbid non-inertial frames
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * J2 secular propagator.
 * <p>
 * This propagator is an analytical propagator taking into account only mean secular effects of J2 zonal harmonic.
 * </p>
 * 
 * @author Emmanuel Bignon
 * @since 3.2
 * @version $Id: J2SecularPropagator.java 17582 2017-05-10 12:58:16Z bignon $
 */
public class J2SecularPropagator extends AbstractPropagator {

    /** UID. */
    private static final long serialVersionUID = -2889686029910875957L;

    /** Reference radius of the central body attraction model (m). */
    private final double referenceRadius;

    /** Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /** Un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth). */
    private final double c20;

    /** Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     * polar axis of the body. */
    private final Frame frame;

    /** Initial orbit (stored only to speed-up computation times). */
    private KeplerianOrbit initialOrbit;

    /**
     * Constructor without attitude provider and mass provider.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param referenceRadiusIn
     *        reference radius of the central body attraction model (m)
     * @param muIn
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20In
     *        un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth)
     * @param frameIn
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    public J2SecularPropagator(final Orbit initialOrbit, final double referenceRadiusIn, final double muIn,
        final double c20In, final Frame frameIn) throws PatriusException {
        this(initialOrbit, referenceRadiusIn, muIn, c20In, frameIn, null, null, null);
    }

    /**
     * Constructor without attitude provider.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param referenceRadiusIn
     *        reference radius of the central body attraction model (m)
     * @param muIn
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20In
     *        un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth)
     * @param frameIn
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param massProvider
     *        mass provider
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    public J2SecularPropagator(final Orbit initialOrbit, final double referenceRadiusIn, final double muIn,
        final double c20In, final Frame frameIn, final MassProvider massProvider) throws PatriusException {
        this(initialOrbit, referenceRadiusIn, muIn, c20In, frameIn, null, null, massProvider);
    }

    /**
     * Constructor without mass provider.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param referenceRadiusIn
     *        reference radius of the central body attraction model (m)
     * @param muIn
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20In
     *        un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth)
     * @param frameIn
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param attitudeProvider
     *        attitude provider
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    public J2SecularPropagator(final Orbit initialOrbit, final double referenceRadiusIn, final double muIn,
        final double c20In, final Frame frameIn,
        final AttitudeProvider attitudeProvider) throws PatriusException {
        this(initialOrbit, referenceRadiusIn, muIn, c20In, frameIn, attitudeProvider, attitudeProvider, null);
    }

    /**
     * Constructor without mass provider.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param referenceRadiusIn
     *        reference radius of the central body attraction model (m)
     * @param muIn
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20In
     *        un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth)
     * @param frameIn
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param attitudeProvForces
     *        attitude provider for force computation
     * @param attitudeProvEvents
     *        attitude provider for events computation
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    public J2SecularPropagator(final Orbit initialOrbit, final double referenceRadiusIn, final double muIn,
        final double c20In, final Frame frameIn, final AttitudeProvider attitudeProvForces,
        final AttitudeProvider attitudeProvEvents) throws PatriusException {
        this(initialOrbit, referenceRadiusIn, muIn, c20In, frameIn, attitudeProvForces, attitudeProvEvents,
            null);
    }

    /**
     * Generic constructor.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param referenceRadiusIn
     *        reference radius of the central body attraction model (m)
     * @param muIn
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20In
     *        un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth)
     * @param frameIn
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param attitudeProvider
     *        attitude provider
     * @param massProvider
     *        mass provider
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    public J2SecularPropagator(final Orbit initialOrbit, final double referenceRadiusIn, final double muIn,
        final double c20In, final Frame frameIn, final AttitudeProvider attitudeProvider,
        final MassProvider massProvider) throws PatriusException {
        this(initialOrbit, referenceRadiusIn, muIn, c20In, frameIn, attitudeProvider, attitudeProvider,
            massProvider);
    }

    /**
     * Generic constructor.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param referenceRadiusIn
     *        reference radius of the central body attraction model (m)
     * @param muIn
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20In
     *        un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth)
     * @param frameIn
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param attitudeProvForces
     *        attitude provider for force computation
     * @param attitudeProvEvents
     *        attitude provider for events computation
     * @param massProvider
     *        mass provider
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    public J2SecularPropagator(final Orbit initialOrbit, final double referenceRadiusIn, final double muIn,
        final double c20In, final Frame frameIn, final AttitudeProvider attitudeProvForces,
        final AttitudeProvider attitudeProvEvents, final MassProvider massProvider) throws PatriusException {
        super(attitudeProvForces, attitudeProvEvents);
        this.referenceRadius = referenceRadiusIn;
        this.mu = muIn;
        this.c20 = c20In;
        if (!frameIn.isPseudoInertial()) {
            // If frame is not pseudo-inertial, an exception is thrown
            throw new PatriusException(PatriusMessages.NOT_INERTIAL_FRAME);
        }
        this.frame = frameIn;

        // Build initial state
        Attitude attitudeForces = null;
        Attitude attitudeEvents = null;
        if (this.getAttitudeProviderForces() != null) {
            attitudeForces = attitudeProvForces.getAttitude(this.getPvProvider(), initialOrbit.getDate(),
                initialOrbit.getFrame());
        }
        if (this.getAttitudeProviderEvents() != null) {
            attitudeEvents = attitudeProvEvents.getAttitude(this.getPvProvider(), initialOrbit.getDate(),
                initialOrbit.getFrame());
        }

        this.resetInitialState(new SpacecraftState(initialOrbit, attitudeForces, attitudeEvents, massProvider));
        this.addAdditionalStateProvider(massProvider);
    }

    /** {@inheritDoc} */
    @Override
    public Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException {
        try {
            // Get keplerian elements (directly in right frame)
            // Mean parameters = osculating parameters => No need to check elements type
            final Orbit orbit = this.getInitialState().getOrbit();
            final KeplerianOrbit kep = this.initialOrbit;

            final double a = kep.getA();
            final double e = kep.getE();
            final double i = kep.getI();
            final double pa = kep.getPerigeeArgument();
            final double raan = kep.getRightAscensionOfAscendingNode();
            final double m = kep.getMeanAnomaly();

            // Propagation duration
            final double d = date.durationFrom(kep.getDate());

            // Build updated orbit
            final double[] dParams = computedParams(date);
            KeplerianOrbit kepResult = new KeplerianOrbit(a, e, i, pa + dParams[0] * d, raan + dParams[1] * d,
                m + dParams[2] * d, PositionAngle.MEAN, this.frame, date, this.mu);
            kepResult = this.convertFrame(kepResult, orbit.getFrame());
            return orbit.getType().convertType(kepResult);

        } catch (final PatriusException e) {
            throw new PropagationException(e);
        }
    }

    /**
     * Compute [dpom/dt, dgom/dt, dM/dt] for given date.
     * @param date a date
     * @return [dpom/dt, dgom/dt, dM/dt] for given date
     */
    private double[] computedParams(final AbsoluteDate date) {
        // Get keplerian elements (directly in right frame)
        // Mean parameters = osculating parameters => No need to check elements type
        final KeplerianOrbit kep = this.initialOrbit;

        final double a = kep.getA();
        final double e = kep.getE();
        final double i = kep.getI();
        final double n = kep.getKeplerianMeanMotion();

        // Temporary variables
        final double rOverA = MathLib.divide(this.referenceRadius, a);
        final double rOverA2 = rOverA * rOverA;
        final double[] sincosi = MathLib.sinAndCos(i);
        final double sini = sincosi[0];
        final double cosi = sincosi[1];
        final double sini2 = sini * sini;
        final double oneMinuse2 = 1. - e * e;
        final double term = rOverA2 * n * MathLib.divide(-this.c20, (oneMinuse2 * oneMinuse2));

        // Compute elements derivatives
        final double dpomdt = 3. / 4. * term * (4 - 5 * sini2);
        final double dgomdt = -3. / 2. * term * cosi;
        final double dMdt = n + 3. / 4. * term * (2 - 3 * sini2) * MathLib.sqrt(MathLib.max(0.0, oneMinuse2));
        // Return result
        return new double[] { dpomdt, dgomdt, dMdt };
    }

    /**
     * Compute transition matrix for given date.
     * @param date a date
     * @return transition matrix for given date
     * @throws PropagationException thrown if frame conversion failed or initial state could not be retrieved
     */
    //CHECKSTYLE: stop MagicNumber check
    // Reason: code lisibility
    public RealMatrix getTransitionMatrix(final AbsoluteDate date) throws PropagationException {
        try {
            // Get keplerian elements (directly in right frame)
            // Mean parameters = osculating parameters => No need to check elements type
            final Orbit orbit = this.getInitialState().getOrbit();
            final KeplerianOrbit kep = this.initialOrbit;

            final double a = kep.getA();
            final double e = kep.getE();
            final double i = kep.getI();
            final double pa = kep.getPerigeeArgument();
            final double raan = kep.getRightAscensionOfAscendingNode();
            final double n = kep.getKeplerianMeanMotion();
            final double m = kep.getMeanAnomaly();

            // Propagation duration
            final double d = date.durationFrom(kep.getDate());

            // Temporary variables
            final double rOverA = MathLib.divide(this.referenceRadius, a);
            final double rOverA2 = rOverA * rOverA;
            final double oneMinuse2 = 1. - e * e;
            final double coef = rOverA2 * n * c20 / (oneMinuse2 * oneMinuse2) * d;
            final double sin2i = MathLib.sin(2. * i);

            // Get [dpom/dt, dgom/dt, dM/dt]
            final double[] dParams = computedParams(date);
            final double dpomdt = dParams[0];
            final double dgomdt = dParams[1];
            final double dMdt = dParams[2];

            // Compute final orbit
            // Do not call propagateOrbit in order to avoid recompute previous variables
            final KeplerianOrbit finalOrbit = new KeplerianOrbit(a, e, i, pa + dpomdt * d, raan + dgomdt * d, m
                    + dMdt * d, PositionAngle.MEAN, this.frame, date, this.mu);

            // Compute transition matrix
            final RealMatrix matrix = MatrixUtils.createRealIdentityMatrix(6);

            // ddpom/da
            matrix.setEntry(3, 0, -7. / 2. * dpomdt / a * d);
            // ddgom/da
            matrix.setEntry(4, 0, -7. / 2. * dgomdt / a * d);
            // ddM/da
            matrix.setEntry(5, 0, (-7. / 2. * (dMdt - n) / a - 3. / 2. * n / a) * d);

            // ddpom/de
            matrix.setEntry(3, 1, 4. * e / oneMinuse2 * dpomdt * d);
            // ddgom/de
            matrix.setEntry(4, 1, 4. * e / oneMinuse2 * dgomdt * d);
            // ddM/de
            matrix.setEntry(5, 1, 3. * e / oneMinuse2 * (dMdt - n) * d);

            // ddpom/di
            matrix.setEntry(3, 2, 15. / 4. * coef * sin2i);
            // ddgom/di
            matrix.setEntry(4, 2, -3. / 2. * coef * MathLib.sin(i));
            // ddM/di
            matrix.setEntry(5, 2, 9. / 4. * coef * sin2i * MathLib.sqrt(oneMinuse2));
            
            // Convert to propagation frame if necessary
            final RealMatrix res;
            if (finalOrbit.getFrame() != orbit.getFrame()) {
                // Convert to cartesian coordinates
                RealMatrix jacobT0 = orbit.getJacobian(OrbitType.KEPLERIAN, OrbitType.CARTESIAN);
                RealMatrix jacobT = finalOrbit.getJacobian(OrbitType.CARTESIAN, OrbitType.KEPLERIAN);
                final RealMatrix stmCartesian = jacobT.multiply(matrix.multiply(jacobT0));

                // Compute jacobian to output frame
                final RealMatrix jacobian = finalOrbit.getFrame().getTransformJacobian(orbit.getFrame(),
                        finalOrbit.getDate());
                final RealMatrix stmOutputFrame = jacobian.multiply(stmCartesian).multiply(jacobian.getInverse());

                // Convert to initial orbit elements types
                jacobT0 = orbit.getJacobian(OrbitType.CARTESIAN, orbit.getType());
                jacobT = finalOrbit.getJacobian(orbit.getType(), OrbitType.CARTESIAN);
                res = jacobT.multiply(stmOutputFrame.multiply(jacobT0));
            } else {
                // No frame conversion
                if (OrbitType.KEPLERIAN.equals(orbit.getType())) {
                    // No types conversion
                    res = matrix;
                } else {
                    // Convert to initial orbit elements types
                    final RealMatrix jacobT0 = orbit.getJacobian(OrbitType.KEPLERIAN, orbit.getType());
                    final RealMatrix jacobT = finalOrbit.getJacobian(orbit.getType(), OrbitType.KEPLERIAN);
                    res = jacobT.multiply(matrix).multiply(jacobT0);
                }
            }
            return res;

        } catch (final PatriusException e) {
            throw new PropagationException(e);
        }
    }
    //CHECKSTYLE: resume MagicNumber check

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        super.resetInitialState(state);

        // Store orbit in body frame and Keplerian elements for future computation speed-up
        try {
            final Orbit orbit = state.getOrbit();
            this.initialOrbit = this.convertFrame(new KeplerianOrbit(orbit), this.frame);
        } catch (final PatriusException e) {
            // Wrap exception
            throw new PropagationException(e);
        }
    }

    /**
     * Convert provided orbit in output frame.
     * 
     * @param orbit
     *        orbit
     * @param outputFrame
     *        output frame
     * @return converted orbit in output frame
     * @throws PatriusException
     *         thrown if conversion failed
     */
    private KeplerianOrbit convertFrame(final KeplerianOrbit orbit, final Frame outputFrame) throws PatriusException {
        KeplerianOrbit res = orbit;
        if (!orbit.getFrame().equals(outputFrame)) {
            // Specific frame transformation to avoid accounting for Earth rotation
            final Transform t = orbit.getFrame().getTransformTo(outputFrame, orbit.getDate());
            final Vector3D pos = t.transformVector(orbit.getPVCoordinates().getPosition());
            final Vector3D vel = t.transformVector(orbit.getPVCoordinates().getVelocity());
            final PVCoordinates pv = new PVCoordinates(pos, vel);

            res = new KeplerianOrbit(pv, outputFrame, orbit.getDate(), orbit.getMu());
        }
        return res;
    }
}
