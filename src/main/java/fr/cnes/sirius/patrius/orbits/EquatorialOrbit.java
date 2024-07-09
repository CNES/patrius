/**
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
 * @history created 09/11/12
 *
 * HISTORY
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
* VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS/COLOSUS] Mise en conformite code avec nouveau standard codage DYNVOL
 * VERSION::FA:273:10/10/2013:Minor code problems
 * VERSION::FA:323:05/11/2014: anomalies of class EquatorialOrbit
 * VERSION::FA:411:10/02/2015:javadoc
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:426:30/10/2015: allow orbit instantiation in non inertial frame
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::FA:1281:30/08/2017: Javadoc correction
 * VERSION::DM:1798:10/12/2018: Add getN() after AlternateEquinoctialParameters creation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits;

import java.util.Collection;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.analysis.interpolation.HermiteInterpolator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.EquatorialParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.IOrbitalParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class handles non circular equatorial orbital parameters.
 * <p>
 * The parameters used internally are the equatorial elements (see {@link EquatorialParameters} for more information.
 * </p>
 * <p>
 * The instance <code>EquatorialOrbit</code> is guaranteed to be immutable.
 * </p>
 * 
 * @see Orbit
 * @see CircularOrbit
 * @see CartesianOrbit
 * @see EquinoctialOrbit
 * @concurrency immutable
 * @author Denis Claude
 * @version $Id: EquatorialOrbit.java 18082 2017-10-02 16:54:17Z bignon $
 * @since 1.3
 */
public final class EquatorialOrbit extends Orbit {

    /** 0.5. */
    private static final double HALF = 0.5;

    /** 0.25. */
    private static final double QUARTER = 0.25;
    
    /** 7 */
    private static final int SEVEN = 7;
    
    /** 8 */
    private static final int EIGHT = 8;

    /** Serializable UID. */
    private static final long serialVersionUID = 7593919633854535287L;
    
    /** Root int for hash code. */
    private static final int ROOTINT = 355;

    /** Orbital parameters. */
    private final EquatorialParameters parameters;

    /**
     * Creates a new instance.
     * 
     * @param parametersIn
     *        orbital parameters
     * @param frame
     *        the frame in which the parameters are defined
     *        (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date
     *        date of the orbital parameters
     */
    public EquatorialOrbit(final IOrbitalParameters parametersIn, final Frame frame,
        final AbsoluteDate date) {
        super(frame, date, parametersIn.getMu());
        this.parameters = parametersIn.getEquatorialParameters();
    }

    /**
     * Creates a new instance.
     * 
     * @param a
     *        semi-major axis (m)
     * @param e
     *        eccentricity
     * @param pomega
     *        &omega; + &Omega; (rad)
     * @param ix
     *        2 sin(i/2) cos(&Omega;), first component of inclination vector
     * @param iy
     *        2 sin(i/2) sin(&Omega;), second component of inclination vector
     * @param anomaly
     *        (M or E or v) = anomaly mean, eccentric or true anomaly (rad)
     * @param type
     *        type of anomaly
     * @param frame
     *        the frame in which the parameters are defined
     * @param date
     *        date of the orbital parameters
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException
     *            if orbit is hyperbolic
     * @exception IllegalArgumentException
     *            if orbit mismatch with conic type
     * @exception IllegalArgumentException
     *            if inclination vector is not valid, meaning ix^2 + iy^2 > 4
     */
    public EquatorialOrbit(final double a, final double e, final double pomega, final double ix,
        final double iy, final double anomaly, final PositionAngle type, final Frame frame,
        final AbsoluteDate date, final double mu) {
        super(frame, date, mu);
        this.parameters = new EquatorialParameters(a, e, pomega, ix, iy, anomaly, type, mu);
    }

    /**
     * Constructor from cartesian parameters.
     * 
     * @param pvCoordinates
     *        the PVCoordinates of the satellite
     * @param frame
     *        the frame in which are defined the {@link PVCoordinates}
     * @param date
     *        date of the orbital parameters
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException
     *            if orbit is hyperbolic
     */
    public EquatorialOrbit(final PVCoordinates pvCoordinates, final Frame frame,
        final AbsoluteDate date, final double mu) {
        super(pvCoordinates, frame, date, mu);
        this.parameters = new CartesianParameters(pvCoordinates, mu).getEquatorialParameters();
    }

    /**
     * Constructor from any kind of orbital parameters.
     * 
     * @param op
     *        orbital parameters to copy
     */
    public EquatorialOrbit(final Orbit op) {
        super(op.getPVCoordinates(), op.getFrame(), op.getDate(), op.getMu());
        this.parameters = op.getParameters().getEquatorialParameters();
    }

    /** {@inheritDoc} */
    @Override
    public IOrbitalParameters getParameters() {
        return this.parameters;
    }

    /**
     * Getter for underlying equatorial parameters.
     * 
     * @return equatorial parameters
     */
    public EquatorialParameters getEquatorialParameters() {
        return this.parameters;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getType() {
        return OrbitType.EQUATORIAL;
    }

    /** {@inheritDoc} */
    @Override
    public double getA() {
        return this.parameters.getA();
    }

    /** {@inheritDoc} */
    @Override
    public double getE() {
        return this.parameters.getE();
    }

    /** {@inheritDoc} */
    @Override
    public double getI() {
        return this.parameters.getKeplerianParameters().getI();
    }

    /**
     * Get the anomaly.
     * 
     * @param type
     *        type of the angle
     * @return anomaly (rad)
     */
    public double getAnomaly(final PositionAngle type) {
        return this.parameters.getAnomaly(type);
    }

    /**
     * Get the longitude of the periapsis (&omega; + &Omega;).
     * 
     * @return longitude of the periapsis (rad)
     */
    public double getPomega() {
        return this.parameters.getPomega();
    }

    /**
     * Get the true anomaly.
     * 
     * @return true anomaly (rad)
     */
    public double getTrueAnomaly() {
        return this.parameters.getTrueAnomaly();
    }

    /**
     * Get the eccentric anomaly.
     * 
     * @return eccentric anomaly (rad)
     */
    public double getEccentricAnomaly() {
        return this.parameters.getEccentricAnomaly();

    }

    /**
     * Get the mean anomaly.
     * 
     * @return mean anomaly (rad)
     */
    public double getMeanAnomaly() {
        return this.parameters.getMeanAnomaly();

    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialEx() {
        return this.parameters.getEquinoctialParameters().getEquinoctialEx();
    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialEy() {
        return this.parameters.getEquinoctialParameters().getEquinoctialEy();
    }

    /**
     * Get the first component of the inclination vector.
     * ix = 2 sin(i/2) cos(&Omega;)
     * 
     * @return first component of the inclination vector.
     */
    public double getIx() {
        return this.parameters.getIx();
    }

    /**
     * Get the second component of the inclination vector.
     * iy = 2 sin(i/2) sin(&Omega;)
     * 
     * @return second component of the inclination vector.
     */
    public double getIy() {
        return this.parameters.getIy();
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

    /** {@inheritDoc} */
    @Override
    public double getLv() {
        return this.parameters.getEquinoctialParameters().getLv();
    }

    /** {@inheritDoc} */
    @Override
    public double getLE() {
        return this.parameters.getEquinoctialParameters().getLE();
    }

    /** {@inheritDoc} */
    @Override
    public double getLM() {
        return this.parameters.getEquinoctialParameters().getLM();
    }

    /**
     * Get the mean motion.
     * 
     * @return mean motion (1/s)
     */
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
    protected EquatorialOrbit orbitShiftedBy(final double dt) {
        return new EquatorialOrbit(this.parameters.getA(), this.parameters.getE(), this.parameters.getPomega(),
            this.parameters.getIx(), this.parameters.getIy(), this.getMeanAnomaly() + this.getKeplerianMeanMotion()
                * dt, PositionAngle.MEAN, this.getFrame(), this.getDate().shiftedBy(dt), this.getMu());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation on Keplerian elements, without
     * derivatives (which means the interpolation falls back to Lagrange interpolation only).
     * </p>
     */
    @Override
    public EquatorialOrbit interpolate(final AbsoluteDate date, final Collection<Orbit> sample) {

        // set up an interpolator
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // add sample points
        AbsoluteDate previousDate = null;
        double previousPOMEGA = Double.NaN;
        double previousM = Double.NaN;
        // loop on all sample points
        for (final Orbit orbit : sample) {
            final EquatorialOrbit equa = (EquatorialOrbit) OrbitType.EQUATORIAL.convertType(orbit);
            final double continuousPOMEGA;
            final double continuousM;
            if (previousDate == null) {
                // first sample
                continuousPOMEGA = equa.getPomega();
                continuousM = equa.getMeanAnomaly();
            } else {
                final double dt = equa.getDate().durationFrom(previousDate);
                final double keplerM = previousM + equa.getKeplerianMeanMotion() * dt;
                continuousPOMEGA = MathUtils.normalizeAngle(equa.getPomega(), previousPOMEGA);
                continuousM = MathUtils.normalizeAngle(equa.getMeanAnomaly(), keplerM);
            }
            previousDate = equa.getDate();
            previousPOMEGA = continuousPOMEGA;
            previousM = continuousM;
            interpolator.addSamplePoint(
                equa.getDate().durationFrom(date),
                new double[] { equa.getA(), equa.getE(), continuousPOMEGA, equa.getIx(),
                    equa.getIy(), continuousM });
        }

        // interpolate
        final double[] interpolated = interpolator.value(0);

        // build a new interpolated instance
        return new EquatorialOrbit(interpolated[0], interpolated[1], interpolated[2],
            interpolated[3], interpolated[4], interpolated[5], PositionAngle.MEAN, this.getFrame(),
            date, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianMeanWrtCartesian() {
        if (this.parameters.getA() > 0) {
            final double[][] internalJacobian = this.computeJacobian();
            final double[][] jacobian = new double[6][6];
            System.arraycopy(internalJacobian, 0, jacobian, 0, 6);
            return jacobian;
        } else {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                EquatorialOrbit.class.getSimpleName());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianEccentricWrtCartesian() {
        // check semi-major axis
        if (this.parameters.getA() > 0) {
            // compute Jacobian
            final double[][] internalJacobian = this.computeJacobian();
            final double[][] jacobian = new double[6][6];
            System.arraycopy(internalJacobian, 0, jacobian, 0, 5);
            jacobian[5] = internalJacobian[6];

            return jacobian;
        } else {
            // Exception
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                EquatorialOrbit.class.getSimpleName());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianTrueWrtCartesian() {
        // check semi-major axis
        if (this.parameters.getA() > 0) {
            // compute Jacobian
            final double[][] internalJacobian = this.computeJacobian();
            final double[][] jacobian = new double[6][6];
            System.arraycopy(internalJacobian, 0, jacobian, 0, 5);
            jacobian[5] = internalJacobian[SEVEN];

            return jacobian;
        } else {
            // Exception
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                EquatorialOrbit.class.getSimpleName());
        }
    }

    /**
     * Compute jacobians.
     * jacobian[0] = da,
     * jacobian[1] = de,
     * jacobian[2] = dpomega,
     * jacobian[3] = dhx,
     * jacobian[4] = dhy,
     * jacobian[5] = dM,
     * jacobian[6] = dE,
     * jacobian[7] = dv
     * 
     * @return jacobian jacobians
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CommentRatio check
    // Reason: Orekit code kept as such
    private double[][] computeJacobian() {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CommentRatio check

        final double[][] jacobian = new double[EIGHT][6];

        // compute various intermediate parameters
        final PVCoordinates pvc = this.getPVCoordinates();
        final Vector3D position = pvc.getPosition();
        final Vector3D velocity = pvc.getVelocity();
        final Vector3D rrxv = pvc.getMomentum();
        final double rxv = rrxv.getNorm();
        final double rxv2 = rxv * rxv;
        final double r = position.getNorm();

        final double[][] drxv = new double[3][6];
        drxv[2][0] = velocity.getY();
        drxv[2][1] = -velocity.getX();
        drxv[2][2] = 0.0;
        drxv[2][3] = -position.getY();
        drxv[2][4] = position.getX();
        drxv[2][5] = 0.0;

        drxv[0][0] = 0.0;
        drxv[0][1] = velocity.getZ();
        drxv[0][2] = -velocity.getY();
        drxv[0][3] = 0.0;
        drxv[0][5] = position.getY();
        drxv[0][4] = -position.getZ();

        drxv[1][0] = -velocity.getZ();
        drxv[1][1] = 0.0;
        drxv[1][2] = velocity.getX();
        drxv[1][3] = position.getZ();
        drxv[1][4] = 0.0;
        drxv[1][5] = -position.getX();

        final Vector3D vv = rrxv.scalarMultiply(1. / rxv);
        final double rach2 = 0.5 * (1. + vv.getZ());
        final double rach = MathLib.sqrt(MathLib.max(0.0, rach2));
        final double cofh = 1. / rach;
        final double hx = -cofh * vv.getY();
        final double hy = cofh * vv.getX();

        final double a = this.parameters.getA();
        final double e = this.parameters.getE();

        // da
        final double a2 = 2. * a * a;
        double cof1 = a2 / MathLib.pow(r, 3);
        double cof2 = a2 / this.getMu();

        final Vector3D dadx = position.scalarMultiply(cof1);
        final Vector3D dadvx = velocity.scalarMultiply(cof2);
        fillHalfRow(1., dadx, jacobian[0], 0);
        fillHalfRow(1., dadvx, jacobian[0], 3);

        final double vel = velocity.getNorm();
        final double v2 = vel * vel;
        cof1 = v2 / (this.getMu() * r);
        cof2 = 2. * r / this.getMu();
        final Vector3D f1p = new Vector3D(cof1, position);
        final Vector3D f1v = new Vector3D(cof2, velocity);

        final double rscal = Vector3D.dotProduct(position, velocity);
        final double rscala = HALF * rscal / a;
        final double rmua = MathLib.sqrt(this.getMu() * a);
        final Vector3D f2p = velocity.subtract(dadx.scalarMultiply(rscala)).scalarMultiply(
            1. / rmua);
        final Vector3D f2v = position.subtract(dadvx.scalarMultiply(rscala)).scalarMultiply(
            1. / rmua);

        // de
        final double ecose = r * v2 / this.getMu() - 1.;
        final double esine = rscal / rmua;
        final double cose = ecose / e;
        final double sine = esine / e;
        final Vector3D dedx = f1p.scalarMultiply(cose).add(sine, f2p);
        final Vector3D dedvx = f1v.scalarMultiply(cose).add(sine, f2v);

        fillHalfRow(1., dedx, jacobian[1], 0);
        fillHalfRow(1., dedvx, jacobian[1], 3);

        // dE
        final Vector3D danedx = (f1p.scalarMultiply(-sine).add(cose, f2p)).scalarMultiply(1. / e);
        final Vector3D danedvx = (f1v.scalarMultiply(-sine).add(cose, f2v)).scalarMultiply(1. / e);
        fillHalfRow(1., danedx, jacobian[6], 0);
        fillHalfRow(1., danedvx, jacobian[6], 3);

        // dhx , dhy
        final double[] dan = new double[6];
        for (int i = 0; i < dan.length; i++) {
            final Vector3D tt = new Vector3D(drxv[0][i], drxv[1][i], drxv[2][i]);
            dan[i] = Vector3D.dotProduct(rrxv, tt) / rxv2;
        }

        final double[][] dv = new double[3][6];
        final double[] vvArray = vv.toArray();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 6; j++) {
                dv[i][j] = drxv[i][j] / rxv - vvArray[i] * dan[j];
            }
        }

        final double unme2 = 1. - e * e;

        final double dcdv3 = -QUARTER * cofh / rach2;
        final Vector3D dchp = new Vector3D(dcdv3, new Vector3D(dv[2][0], dv[2][1], dv[2][2]));
        final Vector3D dchv = new Vector3D(dcdv3, new Vector3D(dv[2][3], dv[2][4], dv[2][5]));
        final Vector3D dhxdxp = dchp.scalarMultiply(-vv.getY()).subtract(cofh,
            new Vector3D(dv[1][0], dv[1][1], dv[1][2]));
        final Vector3D dhxdxv = dchv.scalarMultiply(-vv.getY()).subtract(cofh,
            new Vector3D(dv[1][3], dv[1][4], dv[1][5]));
        final Vector3D dhydxp = dchp.scalarMultiply(vv.getX()).add(cofh,
            new Vector3D(dv[0][0], dv[0][1], dv[0][2]));
        final Vector3D dhydxv = dchv.scalarMultiply(vv.getX()).add(cofh,
            new Vector3D(dv[0][3], dv[0][4], dv[0][5]));
        fillHalfRow(1., dhxdxp, jacobian[3], 0);
        fillHalfRow(1., dhxdxv, jacobian[3], 3);
        fillHalfRow(1., dhydxp, jacobian[4], 0);
        fillHalfRow(1., dhydxv, jacobian[4], 3);

        // dpomega
        final double race = MathLib.sqrt(MathLib.max(0.0, unme2));
        final double drace = -e / race;
        final Vector3D fp = new Vector3D(1. - HALF * hy * hy, HALF * hx * hy, -rach * hy);
        final Vector3D fv = Vector3D.ZERO;
        final Vector3D gp = new Vector3D(fp.getY(), 1. - HALF * hx * hx, rach * hx);
        final Vector3D gv = Vector3D.ZERO;

        final double dd = cose - e;
        final Vector3D dal1p = dadx.scalarMultiply(dd).add(a,
            danedx.scalarMultiply(-sine).subtract(dedx));
        final Vector3D dal1v = dadvx.scalarMultiply(dd).add(a,
            danedvx.scalarMultiply(-sine).subtract(dedvx));
        final Vector3D dal2p = dadx.scalarMultiply(race * sine).add(a,
            dedx.scalarMultiply(drace * sine).add(race * cose, danedx));
        final Vector3D dal2v = dadvx.scalarMultiply(race * sine).add(a,
            dedvx.scalarMultiply(drace * sine).add(race * cose, danedvx));

        final Vector3D hdhp = (dhxdxp.scalarMultiply(hx).add(hy, dhydxp)).scalarMultiply(1. / 4.);
        final Vector3D hdhv = (dhxdxv.scalarMultiply(hx).add(hy, dhydxv)).scalarMultiply(1. / 4.);

        final Vector3D k1p = new Vector3D(hx, dhydxp, hy, dhxdxp);
        final Vector3D k2p = new Vector3D(rach, dhydxp, -hy / rach, hdhp);
        final Vector3D k1v = new Vector3D(hx, dhydxv, hy, dhxdxv);
        final Vector3D k2v = new Vector3D(rach, dhydxv, -hy / rach, hdhv);
        final Vector3D dsfp = dhydxp.scalarMultiply(-hy * position.getX())
            .add(position.getY() / 2., k1p).subtract(position.getZ(), k2p).add(fp);
        final Vector3D dsfv = dhydxv.scalarMultiply(-hy * position.getX())
            .add(position.getY() / 2., k1v).subtract(position.getZ(), k2v).add(fv);
        final Vector3D k3p = new Vector3D(rach, dhxdxp, -hx / rach, hdhp);
        final Vector3D k3v = new Vector3D(rach, dhxdxv, -hx / rach, hdhv);
        final Vector3D dsgp = k1p.scalarMultiply(position.getX() / 2.)
            .add(-hx * position.getY(), dhxdxp).add(position.getZ(), k3p).add(gp);
        final Vector3D dsgv = k1v.scalarMultiply(position.getX() / 2.)
            .add(-hx * position.getY(), dhxdxv).add(position.getZ(), k3v).add(gv);
        final double scalf = Vector3D.dotProduct(position, fp);
        final double scalg = Vector3D.dotProduct(position, gp);
        final double al2 = a * race * sine;
        final double al1 = a * dd;
        final Vector3D domdxp = dsgp.scalarMultiply(scalf).subtract(scalg, dsfp).add(al2, dal1p)
            .subtract(al1, dal2p).scalarMultiply(1. / (r * r));
        final Vector3D domdxv = dsgv.scalarMultiply(scalf).subtract(scalg, dsfv).add(al2, dal1v)
            .subtract(al1, dal2v).scalarMultiply(1. / (r * r));
        fillHalfRow(1., domdxp, jacobian[2], 0);
        fillHalfRow(1., domdxv, jacobian[2], 3);

        // DM
        final double coef = 1. - ecose;
        fillHalfRow(coef, danedx, -sine, dedx, jacobian[5], 0);
        fillHalfRow(coef, danedvx, -sine, dedvx, jacobian[5], 3);

        // dv
        final double epsilon = MathLib.sqrt(1 - e * e);
        final double eccentricAnomaly = this.getEccentricAnomaly();
        final double[] sincos = MathLib.sinAndCos(eccentricAnomaly);
        final double cosE = sincos[1];
        final double sinE = sincos[0];
        final double aOr = 1 / (1 - e * cosE);
        final double eFactor = sinE * aOr / epsilon;
        final double[] eRow = jacobian[1];
        final double[] anomalyRow = jacobian[6];
        for (int j = 0; j < anomalyRow.length; ++j) {
            jacobian[SEVEN][j] = epsilon * aOr * anomalyRow[j] + eFactor * eRow[j];
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected void orbitAddKeplerContribution(final PositionAngle type, final double gm,
                                              final double[] pDot) {
        // Keplerian parameters
        final double a = this.parameters.getA();
        final double e = this.parameters.getE();
        final double v = this.parameters.getTrueAnomaly();

        // Intermediate computation
        final double absA = MathLib.abs(a);
        final double n = MathLib.sqrt(gm / absA) / absA;
        final double oMe2 = MathLib.abs(1 - e * e);
        final double ksi = 1 + e * MathLib.cos(v);
        // Result depends on anomaly type
        switch (type) {
            case MEAN:
                pDot[5] += n;
                break;
            case ECCENTRIC:
                pDot[5] += n * ksi / oMe2;
                break;
            case TRUE:
                pDot[5] += n * ksi * ksi / (oMe2 * MathLib.sqrt(oMe2));
                break;
            default:
                // Cannot happen
                throw PatriusException.createInternalError(null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.parameters.toString();
    }
    
    /** {@inheritDoc} */
    @Override
	public boolean equals(final Object object) {
		// parameters : date, frame, type, mu,
		//              a, e, pomega, ix, iy, v
        boolean isEqual = true;
		
        if (object == this) { 
            // first fast check
            isEqual = true; 
        } else if (object instanceof EquatorialOrbit) {
            // cast object to compare parameters
            final EquatorialOrbit other = (EquatorialOrbit) object;
        	
            isEqual &= (this.getDate().equals(other.getDate()));
            isEqual &= (this.getFrame().equals(other.getFrame()));
            isEqual &= (this.parameters.equals(other.parameters));
        	
        } else {
            isEqual = false;
        }
		
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
        result = effMult * result + this.parameters.hashCode();
        
        return result; 
    }
}
