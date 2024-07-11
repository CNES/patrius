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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.4:FA:FA-2251:04/10/2019:[PATRIUS] Propagateurs analytique
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
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
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
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

/**
 * Abstract Lyddane propagator.
 * This class contains common algorithms to all Lyddane propagators.
 * 
 * @author Emmanuel Bignon
 * @since 3.2
 * @version $Id: AbstractLyddanePropagator.java 17582 2017-05-10 12:58:16Z bignon $
 */

public abstract class AbstractLyddanePropagator extends AbstractPropagator implements MeanOsculatingElementsProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -1868667581720973969L;

    /** Default convergence threshold for osculating to mean/secular algorithm. */
    private static final double DEFAULT_THRESHOLD = 1E-14;
    
    /** 7. */
    private static final double SEVEN = 7.;
    /** 8. */
    private static final double EIGHT = 8.;
    /** 9. */
    private static final double NINE = 9.;
    /** 10. */
    private static final double TEN = 10.;

    /**
     * Relative convergence threshold for osculating to mean/secular algorithm.
     * Default threshold is 1E-14.
     */
    private static double threshold = DEFAULT_THRESHOLD;

    /** Secular orbit. */
    protected Orbit secularOrbitIn;

    /** Reference radius of the central body attraction model (m). */
    private final double referenceRadius;

    /** Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /** Un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth). */
    private final double c20;

    /** Un-normalized 3rd zonal coefficient (about +2.53e-6 for Earth). */
    private final double c30;

    /** Un-normalized 4th zonal coefficient (about +1.62e-6 for Earth). */
    private final double c40;

    /** Un-normalized 5th zonal coefficient (about +2.28e-7 for Earth). */
    private final double c50;

    /** Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     * polar axis of the body. */
    private final Frame frame;

    /**
     * Lyddane parameters types.
     * 
     * @author Emmanuel Bignon
     * @since 3.2
     * @version $Id: AbstractLyddanePropagator.java 17582 2017-05-10 12:58:16Z bignon $
     */
    public enum LyddaneParametersType {

        /** Secular elements. */
        SECULAR,

        /** Mean elements. */
        MEAN,

        /** Osculating elements. */
        OSCULATING;
    }

    /**
     * Generic constructor.
     * 
     * @param secularOrbit
     *        initial secular orbit
     * @param referenceRadiusIn
     *        reference radius of the central body attraction model (m)
     * @param muIn
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20In
     *        un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth)
     * @param c30In
     *        un-normalized 3rd zonal coefficient (about +2.53e-6 for Earth)
     * @param c40In
     *        un-normalized 4th zonal coefficient (about +1.62e-6 for Earth)
     * @param c50In
     *        un-normalized 5th zonal coefficient (about +2.28e-7 for Earth)
     * @param frameIn
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param parametersTypeIn
     *        initial orbit parameters type (mean or osculating)
     * @param attitudeProvForces
     *        attitude provider for force computation
     * @param attitudeProvEvents
     *        attitude provider for events computation
     * @param massProvider
     *        mass provider
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    protected AbstractLyddanePropagator(final Orbit secularOrbit, final double referenceRadiusIn, final double muIn,
        final double c20In, final double c30In, final double c40In, final double c50In,
        final Frame frameIn, final ParametersType parametersTypeIn, final AttitudeProvider attitudeProvForces,
        final AttitudeProvider attitudeProvEvents, final MassProvider massProvider) throws PatriusException {
        super(attitudeProvForces, attitudeProvEvents);
        this.referenceRadius = referenceRadiusIn;
        this.mu = muIn;
        this.c20 = c20In;
        this.c30 = c30In;
        this.c40 = c40In;
        this.c50 = c50In;
        this.secularOrbitIn = secularOrbit;
        if (!frameIn.isPseudoInertial()) {
            // If frame is not pseudo-inertial, an exception is thrown
            throw new PatriusException(PatriusMessages.NOT_INERTIAL_FRAME);
        }
        this.frame = frameIn;

        // Initialize propagation frame as initial orbit frame (compulsory for upcoming attitude computation)
        this.setOrbitFrame(secularOrbit.getFrame());

        // Build initial state
        Attitude attitudeForces = null;
        Attitude attitudeEvents = null;
        if (this.getAttitudeProviderForces() != null) {
            attitudeForces = attitudeProvForces.getAttitude(this.getPvProvider(), secularOrbit.getDate(),
                secularOrbit.getFrame());
        }
        if (this.getAttitudeProviderEvents() != null) {
            attitudeEvents = attitudeProvEvents.getAttitude(this.getPvProvider(), secularOrbit.getDate(),
                secularOrbit.getFrame());
        }

        super.resetInitialState(new SpacecraftState(secularOrbit, attitudeForces, attitudeEvents, massProvider));
        this.addAdditionalStateProvider(massProvider);
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        super.resetInitialState(state);
        // PropagateOrbit() always returns osculating parameters
        try {
            this.secularOrbitIn = this.computeSecular(state.getOrbit(), LyddaneParametersType.OSCULATING);
        } catch (final PatriusException e) {
            // Wrap exception
            throw new PropagationException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException {
        return this.propagateOrbit(date, this.secularOrbitIn, this.getFrame(), LyddaneParametersType.OSCULATING);
    }

    /** {@inheritDoc} */
    @Override
    public Orbit propagateMeanOrbit(final AbsoluteDate date) throws PatriusException {
        return this.propagateOrbit(date, this.secularOrbitIn, this.getFrame(), LyddaneParametersType.MEAN);
    }

    /**
     * Propagate orbit to provided date.
     * 
     * @param date
     *        a date
     * @param secularOrbit
     *        a secular orbit in body frame
     * @param outputFrame
     *        output frame
     * @param returnType
     *        return type : SECULAR, MEAN or OSCULTATING
     * @return propagated orbit in output frame to provided date
     * @throws PropagationException
     *         if some parameters are out of bounds
     */
    // CHECKSTYLE: stop MethodLength check
    protected Orbit propagateOrbit(final AbsoluteDate date, final Orbit secularOrbit,
            final Frame outputFrame, final LyddaneParametersType returnType) throws PropagationException {
        // CHECKSTYLE: resume MethodLength check

        try {
            // Get Keplerian mean orbit
            //
            final KeplerianOrbit kep = new KeplerianOrbit(secularOrbit);
            final double aSecu = kep.getA();
            final double eSecu = kep.getE();
            final double iSecu = kep.getI();
            final double paSecu = kep.getPerigeeArgument();
            final double raanSecu = kep.getRightAscensionOfAscendingNode();
            final double mSecu = kep.getMeanAnomaly();

            // Propagation duration
            final double duration = date.durationFrom(secularOrbit.getDate());

            // Sanity checks
            sanityCheck(eSecu, iSecu);

            // Temporary variables for computation speed-up
            //
            final double q = MathLib.divide(this.referenceRadius, aSecu);
            final double q2 = q * q;
            final double q3 = q2 * q;
            final double q4 = q3 * q;

            final double g2 = -this.c20 * q2 / 2.;

            final double e2 = eSecu * eSecu;

            final double tt1 = MathLib.sqrt(MathLib.max(0.0, 1 - e2));
            final double tt1p2 = tt1 * tt1;
            final double tt1p4 = tt1p2 * tt1p2;
            final double tt1p8 = tt1p4 * tt1p4;

            final double h2 = MathLib.divide(g2, tt1p4);
            final double h4 = MathLib.divide(this.c40 * q4 * (3. / 8.), tt1p8);

            final double ant = MathLib.sqrt(this.mu / (aSecu * aSecu * aSecu)) * duration;

            final double c1 = MathLib.cos(iSecu);
            final double c1p2 = c1 * c1;
            final double c1p3 = c1 * c1p2;
            final double c1p4 = c1 * c1p3;
            final double dc = 1. - 5. * c1p2;

            // Constants
            //
            final double c12 = 12.;
            final double c15 = 15.;
            final double c16 = 16.;
            final double c21 = 21.;
            final double c24 = 24.;
            final double c25 = 25.;
            final double c30p = 30.;
            final double c35 = 35.;
            final double c36 = 36.;
            final double c45 = 45.;
            final double c90 = 90.;
            final double c96 = 96.;
            final double c105 = 105.;
            final double c126 = 126.;
            final double c144 = 144.;
            final double c189 = 189.;
            final double c192 = 192.;
            final double c270 = 270.;
            final double c360 = 360.;
            final double c385 = 385.;
            final double c5Over16 = 5. / c16;

            // ========================== SECULAR ELEMENTS ========================== //
            //
            // Effects of J2, J2^2, J4

            // Secular effects on argp, raan et mean anomaly
            //
            // argument of perigee: po1
            double h22 = -c35 + c24 * tt1 + c25 * tt1p2 + (c90 - c192 * tt1 - c126 * tt1p2) * c1p2
                + (c385 + c360 * tt1 + c45 * tt1p2) * c1p4;
            double h41 = c21 - NINE * tt1p2 + (-c270 + c126 * tt1p2) * c1p2 + (c385 - c189 * tt1p2) * c1p4;
            final double po1 = paSecu + ant * (3. / 2. * h2 * (-dc + MathLib.divide(h2 * h22, c16))
                + c5Over16 * h4 * h41);

            // raan: go1
            h22 = (c12 * tt1 + NINE * tt1p2 - 5.) * c1 + (-c35 - c36 * tt1 - 5. * tt1p2) * c1p3;
            h41 = (5. - 3. * tt1p2) * c1 * (3. - SEVEN * c1p2);
            final double go1 = raanSecu + ant * (3. * h2 * (-c1 + h2 * h22 / 8.) + 5. * h4 * h41 / 4.);

            // mean anomaly: am1
            h22 = -c15 + c16 * tt1 + c25 * tt1p2 + (c30p - c96 * tt1 - c90 * tt1p2) * c1p2 + (c105 + c144 * tt1
                + c25 * tt1p2) * c1p4;
            h41 = (3. - c30p * c1p2 + c35 * c1p4);
            double am1 = mSecu + ant * (1. + 3. / 2. * h2 * (tt1 * (-1. + 3. * c1p2) + MathLib.divide(h2 * tt1
                * h22, c16)) + MathLib.divide(c15 * h4 * tt1 * e2 * h41, c16));
            am1 = MathUtils.normalizeAngle(am1, FastMath.PI);

            // Return secular elements
            if (returnType.equals(LyddaneParametersType.SECULAR)) {
                final KeplerianOrbit kepOrbit = new KeplerianOrbit(aSecu, eSecu, iSecu, po1, go1, am1,
                    PositionAngle.MEAN, kep.getFrame(), date, this.mu);
                return convertFrame(kepOrbit, outputFrame);
            }

            // ========================== MEAN ELEMENTS ========================== //
            //

            // Temporary variables for computation speed-up: trigonometric values of pa, raan et m
            final double[] sincospo1 = MathLib.sinAndCos(po1);
            final double sinpo1 = sincospo1[0];
            final double cospo1 = sincospo1[1];
            // Decomposition for optimization of trigonometric values of pa
            final double sinpo13 = sinpo1 * sinpo1 * sinpo1;
            final double cospo12 = cospo1 * cospo1;
            final double cospo13 = cospo12 * cospo1;
            final double sin2po1 = 2 * cospo1 * sinpo1;
            final double sin3po1 = 3 * sinpo1 - 4 * sinpo13;
            final double cos2po1 = 2 * cospo1 * cospo1 - 1;
            final double cos3po1 = 4 * cospo13 - 3 * cospo1;
            // Other trigonometric function: RAAN
            final double[] sincosgo1 = MathLib.sinAndCos(go1);
            final double singo1 = sincosgo1[0];
            final double cosgo1 = sincosgo1[1];
            // Other trigonometric function: m
            final double[] sincosam1 = MathLib.sinAndCos(am1);
            final double sinam1 = sincosam1[0];
            final double cosam1 = sincosam1[1];

            final double s1 = MathLib.sin(iSecu);
            final double s1p2 = s1 * s1;

            final double r1 = 4. + 3. * e2;
            final double r2 = 4. + 9. * e2;
            final double c1p6 = c1p3 * c1p3;
            final double dc2 = dc * dc;
            final double q5 = q4 * q;
            final double tt1p6 = tt1p4 * tt1p2;
            final double tt1p10 = tt1p6 * tt1p4;
            final double r4 = MathLib.divide(e2 * c1p6, dc2);
            final double h3 = MathLib.divide(this.c30 * q3, tt1p6);
            final double h5 = MathLib.divide(this.c50 * q5, tt1p10);

            final double[] sincosdai = MathLib.sinAndCos(iSecu / 2.);
            final double sdai = sincosdai[0];
            final double cdai = sincosdai[1];

            // long period terms in J2, J3, J4, J5

            final double c40p = 40.;
            final double cc = MathLib.divide(2. * c1p2, dc);
            final double q0 = MathLib.divide(c16 * c1p2, dc) + MathLib.divide(c40p * c1p4, dc2);

            final double c32 = 32.;
            final double c35Over32 = c35 / c32;
            double a = (1. - 5. * cc) * s1 * h2 / 2. - MathLib.divide(5. * (1. - cc) * s1 * h4, 3. * h2);
            double b = MathLib.divide(tt1p2 * h3 + c5Over16 * r1 * (3. * (1. - cc) * s1p2 - 2.) * tt1p2 * h5, h2);
            double c = MathLib.divide(c35Over32 * h5 * ((1. - 2. * cc) * s1p2), 3. * h2);

            double dai2 = eSecu * (-a * cos2po1 + eSecu * c * sin3po1);
            // delta2 e (1)
            final double dex2 = (-tt1p2 * dai2 + b * sinpo1) * s1 / 4.;
            // delta i2 (2)
            dai2 = (dai2 - MathLib.divide(b * sinpo1, tt1p2)) * (eSecu * c1) / 4.;

            final double tt1p3 = tt1p2 * tt1;
            double edm2 = -h3 - c5Over16 * r2 * h5 * (3. * (1 - cc) * s1p2 - 2.);
            edm2 = MathLib.divide(edm2 * cospo1, h2) + eSecu * a * sin2po1;
            edm2 = edm2 + e2 * c * cos3po1;
            edm2 = edm2 * tt1p3 * s1 / 4.;

            final double c11 = 11.;
            final double c35Over16 = c35 / c16;
            final double c5Over8 = 5. / 8.;
            a = (eSecu * s1) * (-(c11 + 5. * q0) * h2 / 2. + MathLib.divide(5. * (3. + q0) * h4, 3. * h2)) * sin2po1;
            b = MathLib.divide((h3 + (c5Over8 * r1 * ((3. * (1 - cc) * s1p2 - 2.) / 2.
                + 3. * s1p2 * (3. + q0))) * h5) * cospo1, h2);
            c = e2 * (MathLib.divide(c35Over16 * (((1. - 2. * cc) * s1p2) / 2. + s1p2 * (5. + 2. * q0)) * h5,
                    NINE * h2)) * cos3po1;

            // sin i d gom 2
            final double sidg2 = (eSecu * c1) * (a + b - c) / 4.;

            a = MathLib.divide(c1, 1. + c1);
            b = MathLib.divide(tt1, 1. + tt1);
            c = tt1 + MathLib.divide(1., 1. + tt1);

            final double c200 = 200.;
            final double som1 = ((-(c + 5. / 2.) * (1. - 5. * cc) * s1p2
                - c1 * (c11 + 5. * q0) - c11 * c1p2) + 2.) * e2 + c200 * r4;
            final double som2 = (((c + 5. / 2.) * (1 - cc) * s1p2 + c1 * (3. + q0) + 3. * c1p2) - 2.) * e2 - c40p * r4;
            final double som3 = (a + c) * (eSecu * s1);
            final double c20p = 20.;
            double som4 = (a + tt1 * b) * r1;
            som4 = som4 + e2 * (NINE + 6. * c) + c20p;
            som4 = som4 * (3 * (1 - cc) * s1p2 - 2) + 6 * r1 * c1 * (1 - c1) * (3 + q0);
            som4 = (eSecu * s1) * som4;
            final double som5 = ((-((1. - 2. * cc) * s1p2) * (a + 3. * c + 2.) / 2.)
                - c1 * (1. - c1) * (5. + 2. * q0)) * (eSecu * s1) * e2;

            // Eccentricity (edm = e''dl, ede = e'' + de)
            double edm = edm2;
            double ede = eSecu + dex2;

            // pom including secular + long periods
            final double c64 = 64.;
            final double c35Over64 = c35 / c64;
            final double c5Over64 = 5. / c64;
            double pgm2 = (h2 * som1 + TEN * h4 * som2 / (3. * h2)) * sin2po1 / EIGHT;
            pgm2 += MathLib.divide((h3 * som3 / 4. + c5Over64 * h5 * som4) * cospo1, h2);
            pgm2 += MathLib.divide(c35Over64 * h5 * som5 * cos3po1, NINE * h2);
            pgm2 += po1 + go1 + am1;

            // gom including LP
            double sidi = sdai + dai2 * cdai / 2.;
            double sidg = sidg2 / (2. * cdai);
            double go2 = MathLib.atan2(sidi * singo1 + sidg * cosgo1, sidi * cosgo1 - sidg * singo1);
            go2 = MathUtils.normalizeAngle(go2, FastMath.PI);

            // Mean anomaly including LP
            double am2 = MathLib.atan2(ede * sinam1 + edm * cosam1, ede * cosam1 - edm * sinam1);
            am2 = MathUtils.normalizeAngle(am2, FastMath.PI);

            // Mean elements
            final double aMean = aSecu;
            final double eMean = MathLib.sqrt(ede * ede + edm * edm);
            final double value = MathLib.sqrt(sidi * sidi + sidg * sidg);
            final double iMean = 2. * MathLib.asin(MathLib.min(1.0, value));
            final double paMean = MathUtils.normalizeAngle(pgm2 - go2 - am2, paSecu);
            final double raanMean = MathUtils.normalizeAngle(go2, raanSecu);
            final double mMean = MathUtils.normalizeAngle(am2, mSecu);

            // Return Mean parameters
            if (returnType.equals(LyddaneParametersType.MEAN)) {
                final KeplerianOrbit kepOrbit = new KeplerianOrbit(aMean, eMean, iMean, paMean, raanMean, mMean,
                    PositionAngle.MEAN, kep.getFrame(), date, this.mu);
                return convertFrame(kepOrbit, outputFrame);
            }

            // ========================== OSCULATING ELEMENTS ========================== //
            //

            // Short periods (J2)
            am2 = am1;
            // will be modified if e > 0.05 (=eccel)
            double ecc2 = eSecu;
            double po2 = po1;
            final double mkt12 = eSecu;

            // Eccentricity threshold
            final double eccel = 0.05;

            // special conditions for eccentricity
            if (eSecu > eccel) {
                final double ede2 = mkt12 + dex2;
                double x = ede2 * cosam1 - edm2 * sinam1;
                double y = ede2 * sinam1 + edm2 * cosam1;
                am2 = MathLib.atan2(y, x);
                am2 = MathUtils.normalizeAngle(am2, FastMath.PI);

                ecc2 = MathLib.sqrt(x * x + y * y);
                final double aux = sdai + dai2 * cdai / 2.;
                final double auxi = MathLib.divide(sidg2, cdai) / 2.;
                x = aux * cosgo1 - auxi * singo1;
                y = aux * singo1 + auxi * cosgo1;

                final double gom2 = MathUtils.normalizeAngle(MathLib.atan2(y, x), FastMath.PI);
                po2 = pgm2 - gom2 - am2;
            }

            // re create Keplerian parameters with ecc2 and am2 to use the getEccentricAnomaly() method
            final double e = (new KeplerianParameters(aMean, ecc2, iMean, paMean, raanMean, am2,
                PositionAngle.MEAN, this.mu)).getEccentricAnomaly();

            final double[] sincose = MathLib.sinAndCos(e);
            final double sine = sincose[0];
            final double cose = sincose[1];
            double v = MathLib.atan2(MathLib.sqrt(MathLib.max(0.0, 1 - ecc2 * ecc2)) * sine,
                    cose - ecc2);
            v = MathUtils.normalizeAngle(v, FastMath.PI);

            final double[] sincosv = MathLib.sinAndCos(v);
            final double sinv = sincosv[0];
            final double cosv = sincosv[1];
            final double[] sincospo2 = MathLib.sinAndCos(po2);
            final double cospo = sincospo2[1];
            final double sinpo = sincospo2[0];
            final double cospo2 = cospo * cospo;
            final double cospo2m1 = 2 * cospo2 - 1;
            final double[] sincospov = MathLib.sinAndCos(po2 + v);
            final double cospov = sincospov[1];
            final double sinpov = sincospov[0];
            final double cospov2 = cospov * cospov;
            final double cosv3 = cosv * cosv * cosv;
            final double sinv3 = sinv * sinv * sinv;
            final double cos2pv = cospo2m1 * cosv - 2 * cospo * sinpo * sinv;
            final double sin2pv = 2 * cospo * sinpo * cosv + cospo2m1 * sinv;

            final double cos2p2v = 2 * cospov2 - 1;
            final double sin2p2v = 2 * cospov * sinpov;
            final double sin2p3v = 2 * cospo * sinpo * (4 * cosv3 - 3 * cosv) + (3 * sinv - 4 * sinv3) * cospo2m1;
            final double xc = (3. * (1. + eSecu * cosv) + e2 * cosv * cosv) * cosv;

            final double dax3 = MathLib.divide(aSecu * (eSecu * (-1. + 3. * c1p2) * (xc + eSecu * c)
                + (1. + eSecu * xc) * (3. * s1p2 * cos2p2v)) * g2, tt1p6);

            final double de1 = MathLib.divide((-1. + 3. * c1p2) * (xc + eSecu * c) + (3. * s1p2 * cos2p2v)
                * (xc + eSecu), tt1p6);
            final double de2 = MathLib.cos(2. * po2 + 3. * v) + 3. * cos2pv;

            final double dex3 = tt1p2 * (de1 * g2 - de2 * s1p2 * h2) / 2.;
            final double dai3 = c1 * s1 * (eSecu * de2 + 3. * cos2p2v) / 2. * h2;

            final double a1 = 1. + eSecu * cosv;
            final double a2 = MathLib.divide(a1 * (1. + a1), tt1p2);
            final double d1 = 2. * (3. * c1p2 - 1.) * (1. + a2) * sinv
                + s1p2 * (3. * (1. - a2) * sin2pv + (3. * a2 + 1.) * sin2p3v);

            final double d21 = 6. * (v + eSecu * sinv - am2);
            final double d22 = 3. * sin2p2v + eSecu * (3. * sin2pv + sin2p3v);

            final double a3 = 5 * c1p2 - 2. * c1 - 1.;
            final double dpgm3 = (eSecu * b * tt1 * d1 + a3 * d21 + (2. - a3) * d22) * h2 / 4.;

            // The second term is in fact "edm3"
            // edm = edm2 + - 0.25 * (tt1.^3) .* d1 .* h2;
            // The second term is in fact "sidg3"; cl = FastMath.cos i0 sl = sin i0
            // sidg = sidg2 + 0.5 * c1 .* s1 .* (d22-d21) .* h2;

            // Re-written compared to original
            final double edm3 = -tt1p3 * d1 * h2 / 4.;
            edm = edm2 + edm3;

            ede = eSecu + dex2 + dex3;

            double am3 = MathLib.atan2(ede * sinam1 + edm * cosam1, ede * cosam1 - edm * sinam1);
            am3 = MathUtils.normalizeAngle(am3, FastMath.PI);

            // sdai = FastMath.sin(i0/2); cdai = FastMath.cos(i0/2);
            sidi = sdai + (dai2 + dai3) * cdai / 2.;
            final double sidg3 = c1 * s1 * (d22 - d21) * h2 / 2.;
            sidg = MathLib.divide((sidg2 + sidg3), cdai) / 2.;

            double go3 = MathLib.atan2(sidi * singo1 + sidg * cosgo1, sidi * cosgo1 - sidg * singo1);
            go3 = MathUtils.normalizeAngle(go3, FastMath.PI);

            // Osculating elements
            // Normalize angles around secular elements
            final double aOsc = aSecu + dax3;
            final double eOsc = MathLib.sqrt(ede * ede + edm * edm);
            final double temp = MathLib.sqrt(sidi * sidi + sidg * sidg);
            final double iOsc = 2. * MathLib.asin(MathLib.min(1.0, temp));
            final double paOsc = MathUtils.normalizeAngle(pgm2 + dpgm3 - go3 - am3, paSecu);
            final double raanOsc = MathUtils.normalizeAngle(go3, raanSecu);
            final double mOsc = MathUtils.normalizeAngle(am3, mSecu);

            // Return osculating elements
            Orbit kepOrbit = new KeplerianOrbit(aOsc, eOsc, iOsc, paOsc, raanOsc, mOsc,
                PositionAngle.MEAN, kep.getFrame(), date, this.mu);
            kepOrbit = convertFrame(kepOrbit, outputFrame);
            return secularOrbit.getType().convertType(kepOrbit);

        } catch (final PatriusException e) {
            // Propagation failed
            throw new PropagationException(e);
        }
    }

    /**
     * Compute secular orbit in body frame from provided orbit.
     * <p>
     * An iterative algorithm is used. {@link #threshold} parameter is used to check convergence of iterations.
     * </p>
     * 
     * @param orbit
     *        an orbit
     * @param fromType
     *        input type
     * @return secular orbit in body frame
     * @throws PatriusException
     *         thrown if conversion to body frame failed
     */
    protected Orbit computeSecular(final Orbit orbit,
                                   final LyddaneParametersType fromType) throws PatriusException {

        // Initialization
        final Orbit osculating = convertFrame(orbit, this.frame);

        // Relative tolerance: threshold for each equinoctial parameter
        final double[] tol = {
            threshold * (1. + MathLib.abs(osculating.getA())),
            threshold * (1. + MathLib.abs(osculating.getEquinoctialEx())),
            threshold * (1. + MathLib.abs(osculating.getEquinoctialEy())),
            threshold * (1. + MathLib.abs(osculating.getHx())),
            threshold * (1. + MathLib.abs(osculating.getHy())),
            threshold * FastMath.PI
        };

        // max number of iterations and current number
        int iter = 0;
        final double maxIter = 100;

        // Mean parameter initialization to osculating parameters
        EquinoctialOrbit mean = new EquinoctialOrbit(osculating);

        // Loop until convergence
        while (iter < maxIter) {

            // Compute f
            final Orbit newMean = new EquinoctialOrbit(this.propagateOrbit(osculating.getDate(), mean,
                osculating.getFrame(), fromType));

            // Parameters residuals
            final double[] delta = {
                newMean.getA() - osculating.getA(),
                newMean.getEquinoctialEx() - osculating.getEquinoctialEx(),
                newMean.getEquinoctialEy() - osculating.getEquinoctialEy(),
                newMean.getHx() - osculating.getHx(),
                newMean.getHy() - osculating.getHy(),
                newMean.getLv() - osculating.getLv()
            };

            // Update mean parameters
            mean = new EquinoctialOrbit(mean.getA() - delta[0],
                mean.getEquinoctialEx() - delta[1],
                mean.getEquinoctialEy() - delta[2],
                mean.getHx() - delta[3],
                mean.getHy() - delta[4],
                mean.getLv() - delta[5],
                PositionAngle.TRUE, mean.getFrame(), mean.getDate(), this.mu);

            // Check convergence
            boolean interrupt = true;
            for (int i = 0; i < delta.length; i++) {
                interrupt &= MathLib.abs(delta[i]) < tol[i];
            }
            if (interrupt) {
                return orbit.getType().convertType(mean);
            }
            // Update loop counter
            iter++;
        }

        // Algorithm did not converge
        throw new PropagationException(PatriusMessages.UNABLE_TO_COMPUTE_LYDDANE_MEAN_PARAMETERS, maxIter);
    }

    /**
     * Sanity check.
     * 
     * @param eSecu
     *        secular eccentricity
     * @param iSecu
     *        secular inclination
     * @throws PropagationException
     *         thrown if eccentricity or inclination out of model validity boundaries
     */
    private static void sanityCheck(final double eSecu, final double iSecu) throws PropagationException {
        // Check eccentricity
        final double eLim = 0.9;
        if (eSecu > eLim) {
            // if 0.005 < e < 0.1 no error is triggered, but accuracy is poor
            throw new PropagationException(PatriusMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL, eSecu);
        }

        // Check inclination
        final double value = 2. / MathLib.sqrt(5);
        final double iCrit1 = MathLib.asin(MathLib.min(1.0, value));
        final double iCrit2 = FastMath.PI - iCrit1;
        final double eps = 4.0e-5;
        if ((MathLib.abs(iSecu - iCrit1) < eps) || (MathLib.abs(iSecu - iCrit2) < eps)
            || iSecu >= FastMath.PI - 2. * eps) {
            throw new PropagationException(PatriusMessages.ALMOST_CRITICALLY_INCLINED_ORBIT, MathLib.toDegrees(iSecu));
        }
    }

    /**
     * Setter for relative convergence threshold for osculating to mean algorithm.
     * 
     * @param newThreshold
     *        new relative threshold
     */
    public void setThreshold(final double newThreshold) {
        threshold = newThreshold;
    }

    /**
     * Update for secular Orbit.
     * 
     * @param secularOrbit
     *        secular orbit
     * @throws PatriusException
     *         thrown if conversion failed
     */
    protected void updateSecularOrbit(final Orbit secularOrbit) throws PatriusException {
        this.secularOrbitIn = convertFrame(secularOrbit, this.frame);
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
    protected Orbit convertFrame(final Orbit orbit, final Frame outputFrame) throws PatriusException {
        Orbit res = orbit;
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
