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
 */
/*
 *
 * HISTORY
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
* VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS/COLOSUS] Mise en conformite code avec nouveau standard codage DYNVOL
 * VERSION::DM:1798:10/12/2018:Creation AlternateEquinoctialParameters
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
import fr.cnes.sirius.patrius.orbits.orbitalparameters.AlternateEquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.IOrbitalParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles alternate equinoctial orbital parameters, which can support both circular and
 * equatorial orbits.
 * <p>
 * The parameters used internally are the alternate equinoctial elements (see {@link AlternateEquinoctialParameters} for
 * more information.
 * </p>
 * <p>
 * The instance <code>AlternateEquinoctialOrbit</code> is guaranteed to be immutable.
 * </p>
 * 
 * @see Orbit
 * @see KeplerianOrbit
 * @see CircularOrbit
 * @see CartesianOrbit
 * @author Crepaldi Stefano
 */
public final class AlternateEquinoctialOrbit extends Orbit  {

    /** Serializable UID. */
    private static final long serialVersionUID = -2000712440570076839L;
    
    /** Root int for hash code. */
    private static final int ROOTINT = 352;
    
    /** -2. */
    private static final double MINUS_TWO = -2.;

    /** Orbital parameters. */
    private final AlternateEquinoctialParameters parameters;

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
    public AlternateEquinoctialOrbit(final IOrbitalParameters parametersIn, final Frame frame,
        final AbsoluteDate date) {
        super(frame, date, parametersIn.getMu());
        this.parameters = parametersIn.getAlternateEquinoctialParameters();
    }

    /**
     * Creates a new instance.
     * 
     * @param n
     *        mean motion (1/s)
     * @param ex
     *        e cos(&omega; + &Omega;), first component of eccentricity vector
     * @param ey
     *        e sin(&omega; + &Omega;), second component of eccentricity vector
     * @param hx
     *        tan(i/2) cos(&Omega;), first component of inclination vector
     * @param hy
     *        tan(i/2) sin(&Omega;), second component of inclination vector
     * @param l
     *        (M or E or v) + &omega; + &Omega;, mean, eccentric or true longitude argument (rad)
     * @param type
     *        type of longitude argument
     * @param frame
     *        the frame in which the parameters are defined
     * @param date
     *        date of the orbital parameters
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException
     *            if eccentricity is equal to 1 or larger
     */
    public AlternateEquinoctialOrbit(final double n, final double ex, final double ey,
        final double hx, final double hy, final double l, final PositionAngle type,
        final Frame frame, final AbsoluteDate date, final double mu) {
        super(frame, date, mu);
        this.parameters = new AlternateEquinoctialParameters(n, ex, ey, hx, hy, l, type, mu);
    }

    /**
     * Constructor from cartesian parameters.
     * 
     * @param pvCoordinates
     *        the position end velocity
     * @param frame
     *        the frame in which are defined the {@link PVCoordinates}
     * @param date
     *        date of the orbital parameters
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException
     *            if eccentricity is equal to 1 or larger
     */
    public AlternateEquinoctialOrbit(final PVCoordinates pvCoordinates, final Frame frame,
        final AbsoluteDate date, final double mu) {
        super(pvCoordinates, frame, date, mu);
        this.parameters = new CartesianParameters(pvCoordinates, mu)
            .getAlternateEquinoctialParameters();
    }

    /**
     * Constructor from any kind of orbital parameters.
     * 
     * @param op
     *        orbital parameters to copy
     */
    public AlternateEquinoctialOrbit(final Orbit op) {
        super(op.getFrame(), op.getDate(), op.getMu());
        this.parameters = op.getParameters().getAlternateEquinoctialParameters();
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
    public AlternateEquinoctialParameters getAlternateEquinoctialParameters() {
        return this.parameters;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getType() {
        return OrbitType.ALTERNATE_EQUINOCTIAL;
    }

    /**
     * Get the mean motion.
     * 
     * @return mean motion (1/s)
     */
    @Override
    public double getN() {
        return this.parameters.getN();
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

    /**
     * Get the first component of the inclination vector.
     * 
     * @return tan(i/2) cos(&Omega;), first component of the inclination vector
     */
    @Override
    public double getHx() {
        return this.parameters.getHx();
    }

    /**
     * Get the second component of the inclination vector.
     * 
     * @return tan(i/2) sin(&Omega;), second component of the inclination vector
     */
    @Override
    public double getHy() {
        return this.parameters.getHy();
    }

    /**
     * Get the longitude argument.
     * 
     * @param type
     *        type of the angle
     * @return longitude argument (rad)
     */
    public double getL(final PositionAngle type) {
        return this.parameters.getL(type);
    }

    /**
     * Get the true longitude argument.
     * 
     * @return v + &omega; + &Omega; true longitude argument (rad)
     */
    @Override
    public double getLv() {
        return this.parameters.getLv();
    }

    /**
     * Get the eccentric longitude argument.
     * 
     * @return E + &omega; + &Omega; eccentric longitude argument (rad)
     */
    @Override
    public double getLE() {
        return this.parameters.getLE();
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

    /**
     * Get the semi-major axis.
     * 
     * Note that the semi-major axis is considered negative for hyperbolic orbits.
     * 
     * @return semi-major axis (m)
     */
    @Override
    public double getA() {
        return this.parameters.getKeplerianParameters().getA();
    }

    /**
     * Get the eccentricity.
     * 
     * @return eccentricity
     */
    @Override
    public double getE() {
        return this.parameters.getKeplerianParameters().getE();
    }

    /**
     * Get the inclination.
     * 
     * @return inclination (rad)
     */
    @Override
    public double getI() {
        return this.parameters.getKeplerianParameters().getI();
    }

    /** {@inheritDoc} */
    @Override
    protected PVCoordinates initPVCoordinates() {
        return this.parameters.getCartesianParameters().getPVCoordinates();
    }

    /** {@inheritDoc} */
    @Override
    protected AlternateEquinoctialOrbit orbitShiftedBy(final double dt) {
        return new AlternateEquinoctialOrbit(this.parameters.getN(), this.parameters.getEquinoctialEx(),
            this.parameters.getEquinoctialEy(), this.parameters.getHx(), this.parameters.getHy(), this.getLM()
                + this.getKeplerianMeanMotion() * dt, PositionAngle.MEAN, this.getFrame(), this.getDate()
                .shiftedBy(dt), this.getMu());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation on equinoctial elements, without
     * derivatives (which means the interpolation falls back to Lagrange interpolation only).
     * </p>
     */
    @Override
    public AlternateEquinoctialOrbit interpolate(final AbsoluteDate date,
                                                 final Collection<Orbit> sample) {

        // set up an interpolator
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // add sample points
        AbsoluteDate previousDate = null;
        double previousLm = Double.NaN;
        for (final Orbit orbit : sample) {
            final AlternateEquinoctialOrbit equi = (AlternateEquinoctialOrbit)
                OrbitType.ALTERNATE_EQUINOCTIAL.convertType(orbit);
            final double continuousLm;
            if (previousDate == null) {
                continuousLm = equi.getLM();
            } else {
                final double dt = equi.getDate().durationFrom(previousDate);
                final double keplerLm = previousLm + equi.getKeplerianMeanMotion() * dt;
                continuousLm = MathUtils.normalizeAngle(equi.getLM(), keplerLm);
            }
            previousDate = equi.getDate();
            previousLm = continuousLm;
            interpolator.addSamplePoint(equi.getDate().durationFrom(date),
                new double[] { equi.getN(), equi.getEquinoctialEx(), equi.getEquinoctialEy(),
                    equi.getHx(), equi.getHy(), continuousLm });
        }

        // interpolate
        final double[] interpolated = interpolator.value(0);

        // build a new interpolated instance
        return new AlternateEquinoctialOrbit(interpolated[0], interpolated[1], interpolated[2],
            interpolated[3], interpolated[4], interpolated[5], PositionAngle.MEAN, this.getFrame(),
            date, this.getMu());

    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianMeanWrtCartesian() {

        // Initialization
        final double[][] jacobian = new double[6][6];

        // Parameters
        final double a = this.parameters.getKeplerianParameters().getA();
        final double ex = this.parameters.getEquinoctialEx();
        final double ey = this.parameters.getEquinoctialEy();
        final double hx = this.parameters.getHx();
        final double hy = this.parameters.getHy();

        // compute various intermediate parameters
        final Vector3D position = this.getPVCoordinates().getPosition();
        final Vector3D velocity = this.getPVCoordinates().getVelocity();
        final double r2 = position.getNormSq();
        final double r = MathLib.sqrt(r2);
        final double r3 = r * r2;

        final double mu = this.getMu();
        final double sqrtMuA = MathLib.sqrt(a * mu);
        final double a2 = a * a;

        final double e2 = ex * ex + ey * ey;
        final double oMe2 = 1 - e2;
        final double epsilon = MathLib.sqrt(MathLib.max(0.0, oMe2));
        final double beta = 1 / (1 + epsilon);
        final double ratio = epsilon * beta;

        final double hx2 = hx * hx;
        final double hy2 = hy * hy;
        final double hxhy = hx * hy;

        final double c0 = -1.5 * MathLib.sqrt(this.getMu() / a) / (a * a);

        // precomputing equinoctial frame unit vectors (f,g,w)
        final Vector3D f = new Vector3D(1 - hy2 + hx2, 2 * hxhy, MINUS_TWO * hy).normalize();
        final Vector3D g = new Vector3D(2 * hxhy, 1 + hy2 - hx2, 2 * hx).normalize();
        final Vector3D w = Vector3D.crossProduct(position, velocity).normalize();

        // coordinates of the spacecraft in the equinoctial frame
        final double x = Vector3D.dotProduct(position, f);
        final double y = Vector3D.dotProduct(position, g);
        final double xDot = Vector3D.dotProduct(velocity, f);
        final double yDot = Vector3D.dotProduct(velocity, g);

        // drDot / dEx = dXDot / dEx * f + dYDot / dEx * g
        final double c1 = a / (sqrtMuA * epsilon);
        final double c2 = a * sqrtMuA * beta / r3;
        final double c3 = sqrtMuA / (r3 * epsilon);
        final Vector3D drDotSdEx = new Vector3D(c1 * xDot * yDot - c2 * ey * x - c3 * x * y, f, -c1
            * xDot * xDot - c2 * ey * y + c3 * x * x, g);

        // drDot / dEy = dXDot / dEy * f + dYDot / dEy * g
        final Vector3D drDotSdEy = new Vector3D(c1 * yDot * yDot + c2 * ex * x - c3 * y * y, f, -c1
            * xDot * yDot + c2 * ex * y + c3 * x * y, g);

        // da
        final Vector3D vectorAR = new Vector3D(2 * c0 * a2 / r3, position);
        final Vector3D vectorARDot = new Vector3D(2 * c0 * a2 / mu, velocity);
        fillHalfRow(1, vectorAR, jacobian[0], 0);
        fillHalfRow(1, vectorARDot, jacobian[0], 3);

        // dEx
        final double d1 = -a * ratio / r3;
        final double d2 = (hy * xDot - hx * yDot) / (sqrtMuA * epsilon);
        final double d3 = (hx * y - hy * x) / sqrtMuA;
        final Vector3D vectorExRDot = new Vector3D((2 * x * yDot - xDot * y) / mu, g, -y * yDot
            / mu, f, -ey * d3 / epsilon, w);
        fillHalfRow(ex * d1, position, -ey * d2, w, epsilon / sqrtMuA, drDotSdEy, jacobian[1], 0);
        fillHalfRow(1, vectorExRDot, jacobian[1], 3);

        // dEy
        final Vector3D vectorEyRDot = new Vector3D((2 * xDot * y - x * yDot) / mu, f, -x * xDot
            / mu, g, ex * d3 / epsilon, w);
        fillHalfRow(ey * d1, position, ex * d2, w, -epsilon / sqrtMuA, drDotSdEx, jacobian[2], 0);
        fillHalfRow(1, vectorEyRDot, jacobian[2], 3);

        // dHx
        final double h = (1 + hx2 + hy2) / (2 * sqrtMuA * epsilon);
        fillHalfRow(-h * xDot, w, jacobian[3], 0);
        fillHalfRow(h * x, w, jacobian[3], 3);

        // dHy
        fillHalfRow(-h * yDot, w, jacobian[4], 0);
        fillHalfRow(h * y, w, jacobian[4], 3);

        // dLambdaM
        final double l = -ratio / sqrtMuA;
        fillHalfRow(-1 / sqrtMuA, velocity, d2, w, l * ex, drDotSdEx, l * ey, drDotSdEy,
            jacobian[5], 0);
        fillHalfRow(MINUS_TWO / sqrtMuA, position, ex * beta, vectorEyRDot, -ey * beta, vectorExRDot, d3,
            w, jacobian[5], 3);

        // Return result
        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianEccentricWrtCartesian() {

        // start by computing the Jacobian with mean angle
        final double[][] jacobian = this.computeJacobianMeanWrtCartesian();

        final double ex = this.parameters.getEquinoctialEx();
        final double ey = this.parameters.getEquinoctialEy();

        // Differentiating the Kepler equation lM = lE - ex sin lE + ey cos lE leads to:
        // dlM = (1 - ex cos lE - ey sin lE) dE - sin lE dex + cos lE dey
        // which is inverted and rewritten as:
        // dlE = a/r dlM + sin lE a/r dex - cos lE a/r dey
        final double le = this.getLE();
        final double[] sincos = MathLib.sinAndCos(le);
        final double sinLe = sincos[0];
        final double cosLe = sincos[1];
        final double aOr = 1 / (1 - ex * cosLe - ey * sinLe);

        // update longitude row
        final double[] rowEx = jacobian[1];
        final double[] rowEy = jacobian[2];
        final double[] rowL = jacobian[5];
        for (int j = 0; j < 6; ++j) {
            rowL[j] = aOr * (rowL[j] + sinLe * rowEx[j] - cosLe * rowEy[j]);
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianTrueWrtCartesian() {

        // start by computing the Jacobian with eccentric angle
        final double[][] jacobian = this.computeJacobianEccentricWrtCartesian();

        final double ex = this.parameters.getEquinoctialEx();
        final double ey = this.parameters.getEquinoctialEy();

        // Differentiating the eccentric longitude equation
        // tan((lV - lE)/2) = [ex sin lE - ey cos lE] / [sqrt(1-ex^2-ey^2) +
        // 1 - ex cos lE - ey sin lE]
        // leads to
        // cT (dlV - dlE) = cE dlE + cX dex + cY dey
        // with
        // cT = [d^2 + (ex sin lE - ey cos lE)^2] / 2
        // d = 1 + sqrt(1-ex^2-ey^2) - ex cos lE - ey sin lE
        // cE = (ex cos lE + ey sin lE) (sqrt(1-ex^2-ey^2) + 1) - ex^2 - ey^2
        // cX = sin lE (sqrt(1-ex^2-ey^2) + 1) - ey + ex (ex sin lE - ey cos lE) /
        // sqrt(1-ex^2-ey^2)
        // cY = -cos lE (sqrt(1-ex^2-ey^2) + 1) + ex + ey (ex sin lE - ey cos lE) /
        // sqrt(1-ex^2-ey^2)
        // which can be solved to find the differential of the true longitude
        // dlV = (cT + cE) / cT dlE + cX / cT deX + cY / cT deX
        final double le = this.getLE();
        final double[] sincos = MathLib.sinAndCos(le);
        final double sinLe = sincos[0];
        final double cosLe = sincos[1];
        final double eSinE = ex * sinLe - ey * cosLe;
        final double ecosE = ex * cosLe + ey * sinLe;
        final double e2 = ex * ex + ey * ey;
        final double epsilon = MathLib.sqrt(1 - e2);
        final double onePeps = 1 + epsilon;
        final double d = onePeps - ecosE;
        final double cT = (d * d + eSinE * eSinE) / 2;
        final double cE = ecosE * onePeps - e2;
        final double cX = ex * eSinE / epsilon - ey + sinLe * onePeps;
        final double cY = ey * eSinE / epsilon + ex - cosLe * onePeps;
        final double factorLe = (cT + cE) / cT;
        final double factorEx = cX / cT;
        final double factorEy = cY / cT;

        // update longitude row
        final double[] rowEx = jacobian[1];
        final double[] rowEy = jacobian[2];
        final double[] rowL = jacobian[5];
        for (int j = 0; j < 6; ++j) {
            rowL[j] = factorLe * rowL[j] + factorEx * rowEx[j] + factorEy * rowEy[j];
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    public void getJacobianWrtParameters(final PositionAngle type, final double[][] jacobian) {

        // array to cache the Jacobian
        final double[][] cachedJacobian;
        synchronized (this) {
            switch (type) {
                case MEAN:
                    if (this.getJacobianWrtParametersMean() == null) {
                        // first call, we need to compute the jacobian and cache it
                        this.setJacobianWrtParametersMean(this.createInverseJacobian(type));
                    }
                    cachedJacobian = this.getJacobianWrtParametersMean();
                    break;
                case ECCENTRIC:
                    if (this.getJacobianWrtParametersEccentric() == null) {
                        // first call, we need to compute the jacobian and cache it
                        this.setJacobianWrtParametersEccentric(this.createInverseJacobian(type));
                    }
                    cachedJacobian = this.getJacobianWrtParametersEccentric();
                    break;
                case TRUE:
                    if (this.getJacobianWrtParametersTrue() == null) {
                        // first call, we need to compute the jacobian and cache it
                        this.setJacobianWrtParametersTrue(this.computeJacobianCartesianWrtTrue());
                    }
                    cachedJacobian = this.getJacobianWrtParametersTrue();
                    break;
                default:
                    // Exception unknown type
                    throw PatriusException.createInternalError(null);
            }
        }

        // fill the user-provided array
        for (int i = 0; i < cachedJacobian.length; ++i) {
            System.arraycopy(cachedJacobian[i], 0, jacobian[i], 0, cachedJacobian[i].length);
        }

    }

    /**
     * Compute the Jacobian of the Cartesian parameters with respect to the orbital parameters with
     * true angle.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of Cartesian coordinate i with respect to the parameter j of the
     * orbit. This means each column correspond to one orbital parameter whereas rows 0 to 5 correspond to the Cartesian
     * coordinates x, y, z, xDot, yDot and zDot.
     * </p>
     * 
     * @return 6x6 Jacobian matrix
     * @see #computeJacobianMeanWrtCartesian()
     * @see #computeJacobianEccentricWrtCartesian()
     */
    // CHECKSTYLE: stop CommentRatio check
    // CHECKSTYLE: stop MethodLength check
    // Reason: Orekit code kept as such
    protected double[][] computeJacobianCartesianWrtTrue() {
        // CHECKSTYLE: resume CommentRatio check
        // CHECKSTYLE: resume MethodLength check

        // Initialization
        final double[][] jacobian = new double[6][6];

        final double a = this.parameters.getKeplerianParameters().getA();
        final double ex = this.parameters.getEquinoctialEx();
        final double ey = this.parameters.getEquinoctialEy();
        final double hx = this.parameters.getHx();
        final double hy = this.parameters.getHy();
        final double lv = this.parameters.getLv();

        // compute various intermediate parameters
        final Vector3D position = this.getPVCoordinates().getPosition();
        final Vector3D velocity = this.getPVCoordinates().getVelocity();

        // constants
        final double[] sincos = MathLib.sinAndCos(lv);
        final double sinL = sincos[0];
        final double cosL = sincos[1];

        final double onePExCoslPEySinl = 1 + ex * cosL + ey * sinL;
        final double oneMEx2MEy2 = 1 - ex * ex - ey * ey;
        final double sqrtOneMEx2MEy2 = MathLib.sqrt(MathLib.max(0.0, oneMEx2MEy2));

        final double exSinLPExEy = ex * sinL + ex * ey;
        final double eyCosLPExEy = ey * cosL + ex * ey;
        final double onePExCoLMEy2 = 1. + ex * cosL - ey * ey;
        final double onePEySinLMEx2 = 1. + ey * sinL - ex * ex;
        final double exSinLMEyCosL = ex * sinL - ey * cosL;

        final double onePHx2PHy2 = 1 + hx * hx + hy * hy;
        final double onePHx2MHy2 = 1 + hx * hx - hy * hy;
        final double oneMHx2PHy2 = 1 - hx * hx + hy * hy;
        final double twoHxHy = 2. * hx * hy;
        final double nA = MathLib.sqrt(this.getMu() / a);

        final double twoHxCosL = 2. * hx * cosL;
        final double twoHyCosL = 2. * hy * cosL;
        final double twoHySinL = 2. * hy * sinL;
        final double twoHxSinL = 2. * hx * sinL;

        final double cosLPEx = cosL + ex;
        final double sinLPEy = sinL + ey;

        final double hy2 = hy * hy;
        final double hx2 = hx * hx;

        final double p1 = onePHx2MHy2 * cosL + twoHxHy * sinL;
        final double p2 = oneMHx2PHy2 * sinL + twoHxHy * cosL;
        final double p3 = hx * sinL - hy * cosL;

        final double v1 = -onePHx2MHy2 * sinLPEy + twoHxHy * cosLPEx;
        final double v2 = oneMHx2PHy2 * cosLPEx - twoHxHy * sinLPEy;
        final double v3 = hx * cosLPEx + hy * sinLPEy;

        // da/dn
        final double n = nA / a;
        final double dadn = -2. / (3. * n) * MathLib.pow(this.getMu() / (n * n), 1. / 3.);

        // first column d./dn
        jacobian[0][0] = position.getX() / a * dadn;
        jacobian[1][0] = position.getY() / a * dadn;
        jacobian[2][0] = position.getZ() / a * dadn;
        jacobian[3][0] = -velocity.getX() / a / 2. * dadn;
        jacobian[4][0] = -velocity.getY() / a / 2. * dadn;
        jacobian[5][0] = -velocity.getZ() / a / 2. * dadn;

        final double coefB1 = nA / (onePHx2PHy2 * sqrtOneMEx2MEy2 * oneMEx2MEy2);

        // second column d./dex
        final double coefDEx = a * (-2.0 * ex * onePExCoslPEySinl - cosL * oneMEx2MEy2)
            / (onePHx2PHy2 * onePExCoslPEySinl * onePExCoslPEySinl);
        jacobian[0][1] = coefDEx * p1;
        jacobian[1][1] = coefDEx * p2;
        jacobian[2][1] = 2.0 * coefDEx * p3;
        jacobian[3][1] = coefB1 * (-onePHx2MHy2 * exSinLPExEy + twoHxHy * onePExCoLMEy2);
        jacobian[4][1] = coefB1 * (oneMHx2PHy2 * onePExCoLMEy2 - twoHxHy * exSinLPExEy);
        jacobian[5][1] = 2.0 * coefB1 * (hx * onePExCoLMEy2 + hy * exSinLPExEy);

        // third colmun d./dey
        final double coefDEy = a * (-2.0 * ey * onePExCoslPEySinl - sinL * oneMEx2MEy2)
            / (onePHx2PHy2 * onePExCoslPEySinl * onePExCoslPEySinl);
        jacobian[0][2] = coefDEy * p1;
        jacobian[1][2] = coefDEy * p2;
        jacobian[2][2] = 2.0 * coefDEy * p3;
        jacobian[3][2] = coefB1 * (-onePHx2MHy2 * onePEySinLMEx2 + twoHxHy * eyCosLPExEy);
        jacobian[4][2] = coefB1 * (oneMHx2PHy2 * eyCosLPExEy - twoHxHy * onePEySinLMEx2);
        jacobian[5][2] = 2.0 * coefB1 * (hx * eyCosLPExEy + hy * onePEySinLMEx2);

        final double coefB2 = nA / (onePHx2PHy2 * onePHx2PHy2 * sqrtOneMEx2MEy2);
        final double coefDHxy = a * oneMEx2MEy2 / (onePHx2PHy2 * onePHx2PHy2 * onePExCoslPEySinl);

        // fourth column d./dhx
        jacobian[0][3] = coefDHxy * (twoHxCosL * 2.0 * hy2 + twoHySinL * oneMHx2PHy2);
        jacobian[1][3] = coefDHxy * (-twoHxSinL * 2.0 * (1.0 + hy2) + twoHyCosL * oneMHx2PHy2);
        jacobian[2][3] = 2.0 * coefDHxy * (twoHxHy * cosL + oneMHx2PHy2 * sinL);
        jacobian[3][3] = coefB2
            * (MINUS_TWO * hx * v1 + onePHx2PHy2 * (MINUS_TWO * hx * sinLPEy + 2. * hy * cosLPEx));
        jacobian[4][3] = coefB2
            * (MINUS_TWO * hx * v2 + onePHx2PHy2 * (MINUS_TWO * hx * cosLPEx - 2. * hy * sinLPEy));
        jacobian[5][3] = 2.0 * coefB2 * (MINUS_TWO * hx * v3 + onePHx2PHy2 * cosLPEx);

        // fifth column d./dhy
        jacobian[0][4] = coefDHxy * (-twoHyCosL * 2.0 * (1.0 + hx2) + twoHxSinL * onePHx2MHy2);
        jacobian[1][4] = coefDHxy * (twoHySinL * 2.0 * hx2 + twoHxCosL * onePHx2MHy2);
        jacobian[2][4] = 2.0 * coefDHxy * (-twoHxHy * sinL - onePHx2MHy2 * cosL);
        jacobian[3][4] = coefB2
            * (MINUS_TWO * hy * v1 + onePHx2PHy2 * (2. * hy * sinLPEy + 2. * hx * cosLPEx));
        jacobian[4][4] = coefB2
            * (MINUS_TWO * hy * v2 + onePHx2PHy2 * (2. * hy * cosLPEx - 2. * hx * sinLPEy));
        jacobian[5][4] = 2.0 * coefB2 * (MINUS_TWO * hy * v3 + onePHx2PHy2 * sinLPEy);

        // sixth column d./dl
        final double coefDHl = a * oneMEx2MEy2
            / (onePHx2PHy2 * onePExCoslPEySinl * onePExCoslPEySinl);
        final double coefB3 = nA / (onePHx2PHy2 * sqrtOneMEx2MEy2);
        jacobian[0][5] = coefDHl
            * (exSinLMEyCosL * p1 + onePExCoslPEySinl * (-onePHx2MHy2 * sinL
                + twoHxHy * cosL));
        jacobian[1][5] = coefDHl
            * (exSinLMEyCosL * p2 + onePExCoslPEySinl * (oneMHx2PHy2 * cosL
                - twoHxHy * sinL));
        jacobian[2][5] = 2.0 * coefDHl
            * (exSinLMEyCosL * p3 + onePExCoslPEySinl * (hx * cosL + hy * sinL));
        jacobian[3][5] = -coefB3 * (onePHx2MHy2 * cosL + twoHxHy * sinL);
        jacobian[4][5] = -coefB3 * (oneMHx2PHy2 * sinL + twoHxHy * cosL);
        jacobian[5][5] = 2.0 * coefB3 * (-hx * sinL + hy * cosL);

        // Return results
        return jacobian;
    }

    /** {@inheritDoc} */
    @Override
    protected void orbitAddKeplerContribution(final PositionAngle type, final double gm,
                                              final double[] pDot) {

        // Parameters
        final double a = this.parameters.getKeplerianParameters().getA();
        final double ex = this.parameters.getEquinoctialEx();
        final double ey = this.parameters.getEquinoctialEy();
        final double lv = this.parameters.getLv();

        final double oMe2 = 1 - ex * ex - ey * ey;
        final double n = MathLib.sqrt(gm / a) / a;
        final double[] sincos = MathLib.sinAndCos(lv);
        final double sinLv = sincos[0];
        final double cosLv = sincos[1];
        final double ksi = 1 + ex * cosLv + ey * sinLv;
        // Result depends on anomaly type
        //
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
		//              n, ex, ey, hx, hy, lM
        boolean isEqual = true;
		
        if (object == this) { 
            // first fast check
            isEqual = true; 
        } else if (object instanceof AlternateEquinoctialOrbit) {
            // cast object to check parameters
            final AlternateEquinoctialOrbit other = (AlternateEquinoctialOrbit) object;
        	
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
