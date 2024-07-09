/**
 *
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
 *
 * @history created 18/01/2013
 * HISTORY
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
* VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS/COLOSUS] Mise en conformite code avec nouveau standard codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:91:26/07/2013: implements Keplerian values as the center of conversions
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:426:30/10/2015: orbit instantiation in non inertial frame, refractor methods
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1798:10/12/2018: Add getN() after AlternateEquinoctialParameters creation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.orbits;

import java.util.Collection;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.IOrbitalParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.StelaEquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class handles Stela equinoctial orbital parameters, which can support both circular and
 * equatorial orbits.
 * <p>
 * The parameters used internally are the Stela equinoctial elements (see
 * {@link StelaEquinoctialParameters} for more information.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
public final class StelaEquinoctialOrbit extends Orbit {

    /** Serializable UID. */
    private static final long serialVersionUID = 4389291298378313927L;

    /** Root int for hash code. */
    private static final int ROOTINT = 357;

    /** Constant. */
    private static final double CST_1_6 = 1.6;

    /** Orbital parameters. */
    private final StelaEquinoctialParameters parameters;

    /**
     * Creates a new instance.
     * 
     * @param params
     *        orbital parameters
     * @param frame
     *        the frame in which the parameters are defined
     * @param date
     *        date of the orbital parameters
     */
    public StelaEquinoctialOrbit(final IOrbitalParameters params, final Frame frame,
            final AbsoluteDate date) {
        super(frame, date, params.getMu());
        this.parameters = params.getStelaEquinoctialParameters();
    }

    /**
     * Creates a new instance. Inclination is corrected when close to 180°
     * 
     * @param aIn
     *        the semi-major axis (m)
     * @param exIn
     *        the e cos(&omega; + &Omega;), first component of eccentricity vector
     * @param eyIn
     *        the e sin(&omega; + &Omega;), second component of eccentricity vector
     * @param ixIn
     *        the sin(i/2) cos(&Omega;), first component of inclination vector
     * @param iyIn
     *        the sin(i/2) sin(&Omega;), second component of inclination vector
     * @param lMIn
     *        the M + &omega; + &Omega;, mean longitude argument (rad)
     * @param frame
     *        frame in which the parameters are defined
     * @param date
     *        the date of the orbital parameters
     * @param mu
     *        the central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException
     *            if eccentricity is equal to 1 or larger or
     */
    public StelaEquinoctialOrbit(final double aIn, final double exIn, final double eyIn,
            final double ixIn, final double iyIn, final double lMIn, final Frame frame,
            final AbsoluteDate date, final double mu) {
        this(aIn, exIn, eyIn, ixIn, iyIn, lMIn, frame, date, mu, true);
    }

    /**
     * Creates a new instance.
     * 
     * @param aIn
     *        semi-major axis (m)
     * @param exIn
     *        e cos(&omega; + &Omega;), first component of eccentricity vector
     * @param eyIn
     *        e sin(&omega; + &Omega;), second component of eccentricity vector
     * @param ixIn
     *        sin(i/2) cos(&Omega;), first component of inclination vector
     * @param iyIn
     *        sin(i/2) sin(&Omega;), second component of inclination vector
     * @param lMIn
     *        M + &omega; + &Omega;, mean longitude argument (rad)
     * @param frame
     *        the frame in which the parameters are defined
     * @param date
     *        date of the orbital parameters
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param isCorrectedIn
     *        Has the correction when inclination is around 180° to be done
     * @exception IllegalArgumentException
     *            if eccentricity is equal to 1 or larger or
     */
    public StelaEquinoctialOrbit(final double aIn, final double exIn, final double eyIn,
            final double ixIn, final double iyIn, final double lMIn, final Frame frame,
            final AbsoluteDate date, final double mu, final boolean isCorrectedIn) {
        super(frame, date, mu);
        this.parameters = new StelaEquinoctialParameters(aIn, exIn, eyIn, ixIn, iyIn, lMIn, mu,
                isCorrectedIn);
    }

    /**
     * Constructor from cartesian parameters. No correction when i around 180°.
     * 
     * @param pvCoordinates
     *        position and velocity
     * @param frame
     *        the frame in which are defined the {@link PVCoordinates}
     * @param date
     *        date of the orbital parameters
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @throws IllegalArgumentException
     *         if eccentricity is equal to 1 or larger
     */
    public StelaEquinoctialOrbit(final PVCoordinates pvCoordinates, final Frame frame,
            final AbsoluteDate date, final double mu) {
        super(pvCoordinates, frame, date, mu);
        this.parameters = new CartesianParameters(pvCoordinates, mu)
                .getStelaEquinoctialParameters();
    }

    /**
     * Constructor from any kind of orbital parameters with correction when inclination is close to
     * 180°
     * 
     * @param op
     *        orbital parameters to copy
     */
    public StelaEquinoctialOrbit(final Orbit op) {
        super(op.getFrame(), op.getDate(), op.getMu());
        this.parameters = op.getParameters().getStelaEquinoctialParameters();
    }

    /**
     * Constructor from any kind of orbital parameters.
     * 
     * @param op
     *        orbital parameters to copy
     * @param isCorrectedIn
     *        Has the correction when inclination is around 180° to be done
     */
    public StelaEquinoctialOrbit(final Orbit op, final boolean isCorrectedIn) {
        super(op.getFrame(), op.getDate(), op.getMu());
        this.parameters = op.getParameters().getStelaEquinoctialParameters();
    }

    /** {@inheritDoc} */
    @Override
    public IOrbitalParameters getParameters() {
        return this.parameters;
    }

    /**
     * Getter for underlying equinoctial parameters.
     * 
     * @return equinoctial parameters
     */
    public StelaEquinoctialParameters getEquinoctialParameters() {
        return this.parameters;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not implemented yet.
     * </p>
     */
    @Override
    public Orbit interpolate(final AbsoluteDate date, final Collection<Orbit> sample) {
        throw PatriusException.createIllegalArgumentException(PatriusMessages.UNSUPPORTED_METHOD);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not implemented yet.
     * </p>
     */
    @Override
    public OrbitType getType() {
        throw PatriusException.createIllegalArgumentException(PatriusMessages.UNSUPPORTED_METHOD);
    }

    /**
     * Get the semi-major axis.
     * 
     * @return semi-major axis (m)
     */
    @Override
    public double getA() {
        return this.parameters.getA();
    }

    /**
     * Get the first component of the eccentricity vector.
     * 
     * @return e cos(&omega; + &Omega;), first component of the eccentricity vector
     */
    @Override
    public double getEquinoctialEx() {
        return this.parameters.getEquinoctialEx();
    }

    /**
     * Get the second component of the eccentricity vector.
     * 
     * @return e sin(&omega; + &Omega;), second component of the eccentricity vector
     */
    @Override
    public double getEquinoctialEy() {
        return this.parameters.getEquinoctialEy();
    }

    /** {@inheritDoc} */
    @Override
    public double getHx() {
        return this.parameters.getEquinoctialParameters().getHx();
    }

    /** {@inheritDoc} */
    @Override
    public double getHy() {
        return this.parameters.getEquinoctialParameters().getHy();
    }

    /**
     * Get the first component of the inclination vector.
     * 
     * @return sin(i/2) cos(&Omega;), first component of the inclination vector
     */
    public double getIx() {
        return this.parameters.getIx();
    }

    /**
     * Get the second component of the inclination vector.
     * 
     * @return sin(i/2) sin(&Omega;), second component of the inclination vector
     */
    public double getIy() {
        return this.parameters.getIy();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not implemented yet.
     * </p>
     */
    @Override
    public double getLE() {
        throw PatriusException.createIllegalArgumentException(PatriusMessages.UNSUPPORTED_METHOD);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not implemented yet.
     * </p>
     */
    @Override
    public double getLv() {
        throw PatriusException.createIllegalArgumentException(PatriusMessages.UNSUPPORTED_METHOD);
    }

    /**
     * Get the mean longitude argument.
     * 
     * @return M + &omega; + &Omega; mean longitude argument (rad)
     */
    @Override
    public double getLM() {
        return this.parameters.getLM();
    }

    /** {@inheritDoc} */
    @Override
    public double getE() {
        return this.parameters.getKeplerianParameters().getE();
    }

    /** {@inheritDoc} */
    @Override
    public double getI() {
        return this.parameters.getKeplerianParameters().getI();
    }

    /** {@inheritDoc} */
    @Override
    public double getN() {
        return this.parameters.getAlternateEquinoctialParameters().getN();
    }

    /** {@inheritDoc} */
    @Override
    protected PVCoordinates initPVCoordinates() {
        return this.parameters.getCartesianParameters().getPVCoordinates();
    }

    /** {@inheritDoc} */
    @Override
    protected Orbit orbitShiftedBy(final double dt) {
        return new StelaEquinoctialOrbit(this.parameters.getA(),
                this.parameters.getEquinoctialEx(), this.parameters.getEquinoctialEy(),
                this.parameters.getIx(), this.parameters.getIy(), this.getLM()
                        + this.getKeplerianMeanMotion() * dt, this.getFrame(), this.getDate()
                        .shiftedBy(dt), this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate otherDate, final Frame otherFrame)
            throws PatriusException {
        // STELA computation time optimization
        final double dt = otherDate.durationFrom(this.getDate());
        if (dt == 0) {
            return this.getPVCoordinates(otherFrame);
        } else {
            return this.shiftedBy(dt).getPVCoordinates(otherFrame);
        }
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop MethodLength check
    @Override
    protected double[][] computeJacobianMeanWrtCartesian() {
        // CHECKSTYLE: resume MethodLength check

        // Initialization:

        final double a = this.parameters.getA();
        final double ix = this.parameters.getIx();
        final double iy = this.parameters.getIy();
        final double ex = this.parameters.getEquinoctialEx();
        final double ey = this.parameters.getEquinoctialEy();

        final double[][] jacobian = new double[6][6];
        final Vector3D pos = this.getPVCoordinates().getPosition();
        final Vector3D vel = this.getPVCoordinates().getVelocity();
        final double r = pos.getNorm();

        final Vector3D w = Vector3D.crossProduct(pos, vel);
        final double normW = w.getNorm();
        final Vector3D wu = w.scalarMultiply(MathLib.divide(1, normW));

        // Other temporary variables:
        final double n = MathLib.sqrt(MathLib.divide(this.getMu(), MathLib.pow(a, 3)));
        final double na2 = MathLib.sqrt(this.getMu() * a);

        final double iFact = MathLib.sqrt(MathLib.max(0.0, 1 - ix * ix - iy * iy));
        final Vector3D p = new Vector3D(1. - 2. * iy * iy, 2. * ix * iy, - 2. * iy * iFact);
        final Vector3D q = new Vector3D(2. * ix * iy, 1 - 2. * ix * ix, 2. * ix * iFact);

        final double x = Vector3D.dotProduct(pos, p);
        final double y = Vector3D.dotProduct(pos, q);
        final double vx = Vector3D.dotProduct(vel, p);
        final double vy = Vector3D.dotProduct(vel, q);

        final double eta = MathLib.sqrt(MathLib.max(0.0, 1 - ex * ex - ey * ey));
        final double nu = MathLib.divide(1., 1. + eta);

        final double dVXdex = MathLib.divide(vx * vy, n * a * eta)
                - na2
                * MathLib.divide(
                        (MathLib.divide(a * ey * x, 1 + eta) + MathLib.divide(x * y, eta)),
                        MathLib.pow(r, 3));
        final double dVYdex = MathLib.divide(-vx * vx, n * a * eta)
                - na2
                * MathLib.divide(
                        (MathLib.divide(a * ey * y, 1 + eta) - MathLib.divide(x * x, eta)),
                        MathLib.pow(r, 3));
        final double dVXdey = MathLib.divide(vy * vy, n * a * eta)
                + na2
                * MathLib.divide(
                        (MathLib.divide(a * ex * x, 1 + eta) - MathLib.divide(y * y, eta)),
                        MathLib.pow(r, 3));
        final double dVYdey = MathLib.divide(-vx * vy, n * a * eta)
                + na2
                * MathLib.divide(
                        (MathLib.divide(a * ex * y, 1 + eta) + MathLib.divide(x * y, eta)),
                        MathLib.pow(r, 3));

        // Partial derivatives of W(1,:), W(2,:) and W(3,:) with respect to (x,y,z) and (vx,vy,vz)
        final Vector3D dW1dr = new Vector3D(0.0, vel.getZ(), -vel.getY());
        final Vector3D dW2dr = new Vector3D(-vel.getZ(), 0.0, vel.getX());
        final Vector3D dW3dr = new Vector3D(vel.getY(), -vel.getX(), 0.0);

        final Vector3D dW1dv = new Vector3D(0.0, -pos.getZ(), pos.getY());
        final Vector3D dW2dv = new Vector3D(pos.getZ(), 0.0, -pos.getX());
        final Vector3D dW3dv = new Vector3D(-pos.getY(), pos.getX(), 0.0);

        // Partial derivatives of the norm of W with respect to (x,y,z) and (vx,vy,vz)
        final Vector3D dnormWdr = Vector3D.crossProduct(vel, w).scalarMultiply(
                MathLib.divide(1., normW));
        final Vector3D dnormWdv = Vector3D.crossProduct(pos, w).scalarMultiply(
                MathLib.divide(-1., normW));

        // Partial derivatives of Wu(1,:), Wu(2,:) and Wu(3,:) with respect to (x,y,z) and
        // (vx,vy,vz)
        // There is a missing 1/normW factor added at last step (jacobian 4th an 5th lines)
        final Vector3D dWu1dr = dW1dr.subtract(dnormWdr.scalarMultiply(wu.getX()));
        final Vector3D dWu2dr = dW2dr.subtract(dnormWdr.scalarMultiply(wu.getY()));
        final Vector3D dWu3dr = dW3dr.subtract(dnormWdr.scalarMultiply(wu.getZ()));

        final Vector3D dWu1dv = dW1dv.subtract(dnormWdv.scalarMultiply(wu.getX()));
        final Vector3D dWu2dv = dW2dv.subtract(dnormWdv.scalarMultiply(wu.getY()));
        final Vector3D dWu3dv = dW3dv.subtract(dnormWdv.scalarMultiply(wu.getZ()));

        for (int j = 0; j < 3; j++) {
            // d(vx,vy,vz)/dex
            final double dvdex = dVXdex * p.toArray()[j] + dVYdex * q.toArray()[j];
            // d(vx,vy,vz)/dey
            final double dvdey = dVXdey * p.toArray()[j] + dVYdey * q.toArray()[j];

            // 1) da/d(x,y,z) and da/d(vx,vy,vz):
            jacobian[0][j] = MathLib.divide(2. * a * a, MathLib.pow(r, 3)) * pos.toArray()[j];
            jacobian[0][j + 3] = MathLib.divide(2., n * n * a) * vel.toArray()[j];
            // 2) dex/d(x,y,z) and dex/d(vx,vy,vz):
            jacobian[2][j] = MathLib.divide(-a * nu * ex * eta, MathLib.pow(r, 3))
                    * pos.toArray()[j]
                    - MathLib.divide(ey * (iy * vx - ix * vy), na2 * eta * iFact) * wu.toArray()[j]
                    + MathLib.divide(eta, na2) * dvdey;
            jacobian[2][j + 3] = MathLib.divide(1., this.getMu())
                    * ((2 * x * vy - vx * y) * q.toArray()[j] - y * vy * p.toArray()[j])
                    - MathLib.divide(ey * (ix * y - iy * x), na2 * eta * iFact) * wu.toArray()[j];
            // 3) dey/d(x,y,z) and dey/d(vx,vy,vz):
            jacobian[3][j] = MathLib.divide(-a * nu * ey * eta, MathLib.pow(r, 3))
                    * pos.toArray()[j]
                    + MathLib.divide(ex * (iy * vx - ix * vy), na2 * eta * iFact) * wu.toArray()[j]
                    - MathLib.divide(eta, na2) * dvdex;
            jacobian[3][j + 3] = MathLib.divide(1., this.getMu())
                    * ((2 * vx * y - x * vy) * p.toArray()[j] - x * vx * q.toArray()[j])
                    + MathLib.divide(ex * (ix * y - iy * x), na2 * eta * iFact) * wu.toArray()[j];
            // 4) dix/d(x,y,z) and dix/d(vx,vy,vz):
            final double d1 = MathLib.divide(1., (2. * MathLib.sqrt((1. + wu.getZ()) / 2.)));
            jacobian[4][j] = MathLib.divide(
                    (-wu.getY() * (-dWu3dr.toArray()[j] * MathLib.pow(d1, 3)) - dWu2dr.toArray()[j]
                            * d1), normW);
            jacobian[4][j + 3] = MathLib.divide(
                    (-wu.getY() * (-dWu3dv.toArray()[j] * MathLib.pow(d1, 3)) - dWu2dv.toArray()[j]
                            * d1), normW);
            // 5) diy/d(x,y,z) and diy/d(vx,vy,vz):
            jacobian[5][j] = MathLib.divide((wu.getX()
                    * (-dWu3dr.toArray()[j] * MathLib.pow(d1, 3)) + dWu1dr.toArray()[j] * d1),
                    normW);
            jacobian[5][j + 3] = MathLib.divide(
                    (wu.getX() * (-dWu3dv.toArray()[j] * MathLib.pow(d1, 3)) + dWu1dv.toArray()[j]
                            * d1), normW);
            // 6) dL/d(x,y,z) and dL/d(vx,vy,vz):
            jacobian[1][j] = MathLib.divide(-vel.toArray()[j], na2)
                    + MathLib.divide(iy * vx - ix * vy, na2 * eta * iFact) * wu.toArray()[j]
                    - MathLib.divide(nu * eta, na2) * (ey * dvdey + ex * dvdex);
            jacobian[1][j + 3] = -MathLib.divide(2., na2) * pos.toArray()[j] + nu
                    * (ex * jacobian[3][j + 3] - ey * jacobian[2][j + 3])
                    + MathLib.divide(1., (na2 * iFact)) * (ix * y - iy * x) * wu.toArray()[j];
        }
        return jacobian;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not implemented yet.
     * </p>
     */
    @Override
    protected double[][] computeJacobianEccentricWrtCartesian() {
        throw PatriusException.createIllegalArgumentException(PatriusMessages.UNSUPPORTED_METHOD);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not implemented yet.
     * </p>
     */
    @Override
    protected double[][] computeJacobianTrueWrtCartesian() {
        throw PatriusException.createIllegalArgumentException(PatriusMessages.UNSUPPORTED_METHOD);
    }

    /** {@inheritDoc} */
    @Override
    protected void orbitAddKeplerContribution(final PositionAngle type, final double gm,
            final double[] pDot) {
        final double a = this.parameters.getA();
        final double n = MathLib.divide(MathLib.sqrt(MathLib.divide(gm, a)), a);
        if (type.equals(PositionAngle.MEAN)) {
            pDot[1] += n;
        } else {
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.UNSUPPORTED_METHOD);
        }
    }

    /**
     * Substitute for Orekit's <code>OrbitTypes.mapOrbitToArray</code>.
     * Warning, unlike other Orekit Orbits lm is returned in second position
     * 
     * @return an array with the following parameters, in this order :
     *         <ul>
     *         <li>a</li>
     *         <li>lm</li>
     *         <li>ey</li>
     *         <li>ix</li>
     *         <li>iy</li>
     *         <li>ex</li>
     *         </ul>
     */
    public double[] mapOrbitToArray() {
        return new double[] { this.getA(), this.getLM(), this.getEquinoctialEx(),
                this.getEquinoctialEy(), this.getIx(), this.getIy() };
    }

    /**
     * Implementation of a Kepler Equation Solver described by F. Landis Markley.
     * (without Pade correction for e>0,75 and E<45deg)
     * 
     * @param e
     *        eccentricity
     * @param mMod
     *        mean anomaly
     * @return eccentric anomaly in [0;2&pi;]
     */
    public double kepEq(final double e, final double mMod) {
        // Initialization
        double res;
        // perform mean anomaly % 2pi
        double m = mMod % (2 * FastMath.PI);
        if ((m % FastMath.PI) == 0.) {
            // Specific case
            res = m;
        } else {
            // Set anomaly in[-Pi, Pi]
            if (m < -FastMath.PI) {
                m += (2 * FastMath.PI);
            } else if (m > FastMath.PI) {
                m -= (2 * FastMath.PI);
            }
            // precomputations
            final double m2 = m * m;
            final double piSq = FastMath.PI * FastMath.PI;
            final double oneMe = (1. - e);
            final double alpha = (3. * piSq + CST_1_6 * FastMath.PI
                    * (MathLib.divide(FastMath.PI - MathLib.abs(m), 1. + e)))
                    / (piSq - 6.);
            final double d = (3. * oneMe) + (alpha * e);
            final double alphaD = alpha * d;
            final double q = 2. * alphaD * oneMe - m2;
            final double q2 = q * q;
            final double r = (3. * alphaD * (d - oneMe)) * m + (m2 * m);
            final double w = MathLib.pow(MathLib.abs(r) + MathLib.sqrt((q * q2) + (r * r)),
                    (2. / 3.));

            // This resolution of Kepler equation avoids to use an iterative scheme which may not converge
            // Instead are used:
            // - A first order approximation
            // - Followed by a 3rd and higher orders correction
            
            // Kepler's equation first order solution
            final double ecc1 = MathLib.divide(MathLib.divide(2. * r * w, (w * w) + (w * q) + q2)
                    + m, d);
            final double[] sincosEcc = MathLib.sinAndCos(ecc1);
            final double eCosEcc1 = e * sincosEcc[1];
            final double eSinEcc1 = e * sincosEcc[0];
            final double fEcc1 = ecc1 - eSinEcc1 - m;
            final double fDotEcc1 = 1. - eCosEcc1;
            final double fDot2Ecc1 = eSinEcc1;
            final double fDot3Ecc1 = 1. - fDotEcc1;
            final double fDot4Ecc1 = -fDot2Ecc1;

            // Third order Halley and higher-order corrections
            final double half = 1. / 2.;
            final double delta3 = MathLib.divide(-fEcc1,
                    (fDotEcc1 - (MathLib.divide(half * fEcc1 * fDot2Ecc1, fDotEcc1))));
            final double delta4 = MathLib.divide(-fEcc1,
                    (fDotEcc1 + half * delta3 * fDot2Ecc1 + (delta3 * delta3 * fDot3Ecc1 / 6.)));
            final double delta4Sq = delta4 * delta4;
            final double delta5 = MathLib.divide(-fEcc1, (fDotEcc1 + half * delta4 * fDot2Ecc1
                    + (delta4Sq * fDot3Ecc1 / 6.) + (delta4Sq * delta4 * fDot4Ecc1 / 24.)));

            // E = E1 + delta5(E1)
            res = ecc1 + delta5;
        }
        if (res < 0) {
            // if res is negative
            res += (2 * FastMath.PI);
        }
        // return eccentric anomaly in [0;2&pi;]
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        // parameters : date, frame, type, mu,
        // a, ex, ey, ix, iy, lM
        boolean isEqual = true;

        if (object == this) {
            // Strict equality
            isEqual = true;
        } else if (object instanceof StelaEquinoctialOrbit) {
            // Check all parameters
            final StelaEquinoctialOrbit other = (StelaEquinoctialOrbit) object;

            isEqual &= (this.getDate().equals(other.getDate()));
            isEqual &= (this.getFrame().equals(other.getFrame()));
            isEqual &= (this.getMu() == other.getMu());

            // Stela Equinoctial parameters
            isEqual &= (this.getA() == other.getA());
            isEqual &= (this.getEquinoctialEx() == other.getEquinoctialEx());
            isEqual &= (this.getEquinoctialEy() == other.getEquinoctialEy());
            isEqual &= (this.getIx() == other.getIx());
            isEqual &= (this.getIy() == other.getIy());
            isEqual &= (this.getLM() == other.getLM());
        } else {
            isEqual = false;
        }

        // Return result
        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        // A not zero random "root int"
        int result = ROOTINT;
        // An efficient multiplier (JVM optimizes 31 * i as (i << 5) - 1 )
        final int effMult = 31;
        // Good hashcode : it's the same
        // for "equal" orbits, but
        // reasonably sure it's different otherwise.
        result = effMult * result + this.getDate().hashCode();
        result = effMult * result + this.getFrame().hashCode();
        result = effMult * result + MathUtils.hash(this.getMu());
        result = effMult * result + MathUtils.hash(this.getA());
        result = effMult * result + MathUtils.hash(this.getEquinoctialEx());
        result = effMult * result + MathUtils.hash(this.getEquinoctialEy());
        result = effMult * result + MathUtils.hash(this.getIx());
        result = effMult * result + MathUtils.hash(this.getIy());
        result = effMult * result + MathUtils.hash(this.getLM());

        return result;
    }
}
