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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
* VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS/COLOSUS] Mise en conformite code avec nouveau standard codage DYNVOL
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
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CircularParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.IOrbitalParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles circular orbital parameters.
 * <p>
 * The parameters used internally are the circular elements (see {@link CircularParameters} for more information.
 * </p>
 * <p>
 * The instance <code>CircularOrbit</code> is guaranteed to be immutable.
 * </p>
 * 
 * @see Orbit
 * @see KeplerianOrbit
 * @see CartesianOrbit
 * @see EquinoctialOrbit
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */

public final class CircularOrbit extends Orbit {

    /** Serializable UID. */
    private static final long serialVersionUID = 5565190329070485158L;
    
    /** Root int for hash code. */
    private static final int ROOTINT = 354;

    /** Orbital parameters. */
    private final CircularParameters parameters;

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
    public CircularOrbit(final IOrbitalParameters parametersIn, final Frame frame,
        final AbsoluteDate date) {
        super(frame, date, parametersIn.getMu());
        this.parameters = parametersIn.getCircularParameters();
    }

    /**
     * Creates a new instance.
     * 
     * @param a
     *        semi-major axis (m)
     * @param ex
     *        e cos(&omega;), first component of circular eccentricity vector
     * @param ey
     *        e sin(&omega;), second component of circular eccentricity vector
     * @param i
     *        inclination (rad)
     * @param raan
     *        right ascension of ascending node (&Omega;, rad)
     * @param alpha
     *        an + &omega;, mean, eccentric or true latitude argument (rad)
     * @param type
     *        type of latitude argument
     * @param frame
     *        the frame in which are defined the parameters
     * @param date
     *        date of the orbital parameters
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException
     *            if eccentricity is equal to 1 or larger
     */
    public CircularOrbit(final double a, final double ex, final double ey, final double i,
        final double raan, final double alpha, final PositionAngle type, final Frame frame,
        final AbsoluteDate date, final double mu) {
        super(frame, date, mu);
        this.parameters = new CircularParameters(a, ex, ey, i, raan, alpha, type, mu);
    }

    /**
     * Constructor from cartesian parameters.
     * 
     * @param pvCoordinates
     *        the {@link PVCoordinates} in inertial frame
     * @param frame
     *        the frame in which are defined the {@link PVCoordinates}
     * @param date
     *        date of the orbital parameters
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public CircularOrbit(final PVCoordinates pvCoordinates, final Frame frame,
        final AbsoluteDate date, final double mu) {
        super(pvCoordinates, frame, date, mu);
        this.parameters = new CartesianParameters(pvCoordinates, mu).getCircularParameters();
    }

    /**
     * Constructor from any kind of orbital parameters.
     * 
     * @param op
     *        orbital parameters to copy
     */
    public CircularOrbit(final Orbit op) {
        super(op.getFrame(), op.getDate(), op.getMu());
        this.parameters = op.getParameters().getCircularParameters();
    }

    /** {@inheritDoc} */
    @Override
    public IOrbitalParameters getParameters() {
        return this.parameters;
    }

    /**
     * Getter for underlying circular parameters.
     * 
     * @return circular parameters
     */
    public CircularParameters getCircularParameters() {
        return this.parameters;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getType() {
        return OrbitType.CIRCULAR;
    }

    /** {@inheritDoc} */
    @Override
    public double getA() {
        return this.parameters.getA();
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
     * Get the first component of the circular eccentricity vector.
     * 
     * @return ex = e cos(&omega;), first component of the circular eccentricity vector
     */
    public double getCircularEx() {
        return this.parameters.getCircularEx();
    }

    /**
     * Get the second component of the circular eccentricity vector.
     * 
     * @return ey = e sin(&omega;), second component of the circular eccentricity vector
     */
    public double getCircularEy() {
        return this.parameters.getCircularEy();
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
     * Get the true latitude argument.
     * 
     * @return v + &omega; true latitude argument (rad)
     */
    public double getAlphaV() {
        return this.parameters.getAlphaV();
    }

    /**
     * Get the latitude argument.
     * 
     * @param type
     *        type of the angle
     * @return latitude argument (rad)
     */
    public double getAlpha(final PositionAngle type) {
        return this.parameters.getAlpha(type);
    }

    /**
     * Get the eccentric latitude argument.
     * 
     * @return E + &omega; eccentric latitude argument (rad)
     */
    public double getAlphaE() {
        return this.parameters.getAlphaE();
    }

    /**
     * Get the mean latitude argument.
     * 
     * @return M + &omega; mean latitude argument (rad)
     */
    public double getAlphaM() {
        return this.parameters.getAlphaM();
    }

    /** {@inheritDoc} */
    @Override
    public double getE() {
        return this.parameters.getKeplerianParameters().getE();
    }

    /** {@inheritDoc} */
    @Override
    public double getI() {
        return this.parameters.getI();
    }

    /**
     * Get the right ascension of the ascending node.
     * 
     * @return right ascension of the ascending node (rad)
     */
    public double getRightAscensionOfAscendingNode() {
        return this.parameters.getRightAscensionOfAscendingNode();
    }

    /** {@inheritDoc} */
    @Override
    public double getLv() {
        return this.parameters.getAlphaV() + this.parameters.getRightAscensionOfAscendingNode();
    }

    /** {@inheritDoc} */
    @Override
    public double getLE() {
        return this.parameters.getAlphaE() + this.parameters.getRightAscensionOfAscendingNode();
    }

    /** {@inheritDoc} */
    @Override
    public double getLM() {
        return this.parameters.getAlphaM() + this.parameters.getRightAscensionOfAscendingNode();
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
    protected CircularOrbit orbitShiftedBy(final double dt) {
        return new CircularOrbit(this.parameters.getA(), this.parameters.getCircularEx(),
            this.parameters.getCircularEy(), this.parameters.getI(),
            this.parameters.getRightAscensionOfAscendingNode(), this.getAlphaM()
                + this.getKeplerianMeanMotion() * dt, PositionAngle.MEAN, this.getFrame(), this.getDate()
                .shiftedBy(dt), this.getMu());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation on circular elements, without
     * derivatives (which means the interpolation falls back to Lagrange interpolation only).
     * </p>
     */
    @Override
    public CircularOrbit interpolate(final AbsoluteDate date, final Collection<Orbit> sample) {

        // set up an interpolator
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // add sample points
        AbsoluteDate previousDate = null;
        double previousRAAN = Double.NaN;
        double previousAlphaM = Double.NaN;
        // loop on all sample points
        for (final Orbit orbit : sample) {
            final CircularOrbit circ = (CircularOrbit) OrbitType.CIRCULAR.convertType(orbit);
            final double continuousRAAN;
            final double continuousAlphaM;
            if (previousDate == null) {
                // first sample
                continuousRAAN = circ.getRightAscensionOfAscendingNode();
                continuousAlphaM = circ.getAlphaM();
            } else {
                final double dt = circ.getDate().durationFrom(previousDate);
                final double keplerAM = previousAlphaM + circ.getKeplerianMeanMotion() * dt;
                continuousRAAN = MathUtils.normalizeAngle(circ.getRightAscensionOfAscendingNode(),
                    previousRAAN);
                continuousAlphaM = MathUtils.normalizeAngle(circ.getAlphaM(), keplerAM);
            }
            previousDate = circ.getDate();
            previousRAAN = continuousRAAN;
            previousAlphaM = continuousAlphaM;
            interpolator.addSamplePoint(
                circ.getDate().durationFrom(date),
                new double[] { circ.getA(), circ.getCircularEx(), circ.getCircularEy(),
                    circ.getI(), continuousRAAN, continuousAlphaM });
        }

        // interpolate
        final double[] interpolated = interpolator.value(0);

        // build a new interpolated instance
        return new CircularOrbit(interpolated[0], interpolated[1], interpolated[2],
            interpolated[3], interpolated[4], interpolated[5], PositionAngle.MEAN, this.getFrame(),
            date, this.getMu());

    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop MethodLength check
    // Reason: Orekit code kept as such
    @Override
    protected double[][] computeJacobianMeanWrtCartesian() {
        // CHECKSTYLE: resume MethodLength check

        // Initialization
        final double[][] jacobian = new double[6][6];
        final double a = this.parameters.getA();
        final double ex = this.parameters.getCircularEx();
        final double ey = this.parameters.getCircularEy();
        final double i = this.parameters.getI();
        final double raan = this.parameters.getRightAscensionOfAscendingNode();

        // compute various intermediate parameters
        final PVCoordinates pvc = this.getPVCoordinates();
        final Vector3D position = pvc.getPosition();
        final Vector3D velocity = pvc.getVelocity();

        // Position
        // Velocity
        final double x = position.getX();
        final double y = position.getY();
        final double z = position.getZ();
        final double vx = velocity.getX();
        final double vy = velocity.getY();
        final double vz = velocity.getZ();

        // Other variables
        final double pv = Vector3D.dotProduct(position, velocity);
        final double r2 = position.getNormSq();
        final double r = MathLib.sqrt(r2);
        final double v2 = velocity.getNormSq();

        final double mu = this.getMu();
        final double oOsqrtMuA = 1 / MathLib.sqrt(mu * a);
        final double rOa = r / a;
        final double aOr = a / r;
        final double aOr2 = a / r2;
        final double a2 = a * a;

        final double ex2 = ex * ex;
        final double ey2 = ey * ey;
        final double e2 = ex2 + ey2;
        final double epsilon = MathLib.sqrt(MathLib.max(0.0, 1 - e2));
        final double beta = 1 / (1 + epsilon);

        final double eCosE = 1 - rOa;
        final double eSinE = pv * oOsqrtMuA;

        // Optimisation
        final double[] sincosI = MathLib.sinAndCos(i);
        final double cosI = sincosI[1];
        final double sinI = sincosI[0];
        final double[] sincosRAAN = MathLib.sinAndCos(raan);
        final double cosRaan = sincosRAAN[1];
        final double sinRaan = sincosRAAN[0];

        // da
        fillHalfRow(2 * aOr * aOr2, position, jacobian[0], 0);
        fillHalfRow(2 * a2 / mu, velocity, jacobian[0], 3);

        // differentials of the normalized momentum
        final Vector3D danP = new Vector3D(v2, position, -pv, velocity);
        final Vector3D danV = new Vector3D(r2, velocity, -pv, position);
        final double recip = 1 / pvc.getMomentum().getNorm();
        final double recip2 = recip * recip;
        final Vector3D dwXP = new Vector3D(recip, new Vector3D(0, vz, -vy), -recip2 * sinRaan
            * sinI, danP);
        final Vector3D dwYP = new Vector3D(recip, new Vector3D(-vz, 0, vx),
            recip2 * cosRaan * sinI, danP);
        final Vector3D dwZP = new Vector3D(recip, new Vector3D(vy, -vx, 0), -recip2 * cosI, danP);
        final Vector3D dwXV = new Vector3D(recip, new Vector3D(0, -z, y), -recip2 * sinRaan * sinI,
            danV);
        final Vector3D dwYV = new Vector3D(recip, new Vector3D(z, 0, -x), recip2 * cosRaan * sinI,
            danV);
        final Vector3D dwZV = new Vector3D(recip, new Vector3D(-y, x, 0), -recip2 * cosI, danV);

        // di
        fillHalfRow(sinRaan * cosI, dwXP, -cosRaan * cosI, dwYP, -sinI, dwZP, jacobian[3], 0);
        fillHalfRow(sinRaan * cosI, dwXV, -cosRaan * cosI, dwYV, -sinI, dwZV, jacobian[3], 3);

        // dRaan
        fillHalfRow(sinRaan / sinI, dwYP, cosRaan / sinI, dwXP, jacobian[4], 0);
        fillHalfRow(sinRaan / sinI, dwYV, cosRaan / sinI, dwXV, jacobian[4], 3);

        // orbital frame: (p, q, w) p along ascending node, w along momentum
        // the coordinates of the spacecraft in this frame are: (u, v, 0)
        final double u = x * cosRaan + y * sinRaan;
        final double cv = -x * sinRaan + y * cosRaan;
        final double v = cv * cosI + z * sinI;

        // du
        final Vector3D duP = new Vector3D(cv * cosRaan / sinI, dwXP, cv * sinRaan / sinI, dwYP, 1,
            new Vector3D(cosRaan, sinRaan, 0));
        final Vector3D duV = new Vector3D(cv * cosRaan / sinI, dwXV, cv * sinRaan / sinI, dwYV);

        // dv
        final Vector3D dvP = new Vector3D(-u * cosRaan * cosI / sinI + sinRaan * z, dwXP, -u
            * sinRaan * cosI / sinI - cosRaan * z, dwYP, cv, dwZP, 1, new Vector3D(-sinRaan
            * cosI, cosRaan * cosI, sinI));
        final Vector3D dvV = new Vector3D(-u * cosRaan * cosI / sinI + sinRaan * z, dwXV, -u
            * sinRaan * cosI / sinI - cosRaan * z, dwYV, cv, dwZV);

        final Vector3D dc1P = new Vector3D(aOr2 * (2 * eSinE * eSinE + 1 - eCosE) / r2, position,
            -2 * aOr2 * eSinE * oOsqrtMuA, velocity);
        final Vector3D dc1V = new Vector3D(-2 * aOr2 * eSinE * oOsqrtMuA, position, 2 / mu,
            velocity);
        final Vector3D dc2P = new Vector3D(aOr2 * eSinE * (eSinE * eSinE - (1 - e2))
            / (r2 * epsilon), position, aOr2 * (1 - e2 - eSinE * eSinE) * oOsqrtMuA / epsilon,
            velocity);
        final Vector3D dc2V = new Vector3D(aOr2 * (1 - e2 - eSinE * eSinE) * oOsqrtMuA / epsilon,
            position, eSinE / (mu * epsilon), velocity);

        final double cof1 = aOr2 * (eCosE - e2);
        final double cof2 = aOr2 * epsilon * eSinE;
        final Vector3D dexP = new Vector3D(u, dc1P, v, dc2P, cof1, duP, cof2, dvP);
        final Vector3D dexV = new Vector3D(u, dc1V, v, dc2V, cof1, duV, cof2, dvV);
        final Vector3D deyP = new Vector3D(v, dc1P, -u, dc2P, cof1, dvP, -cof2, duP);
        final Vector3D deyV = new Vector3D(v, dc1V, -u, dc2V, cof1, dvV, -cof2, duV);
        // Fill Jacobian
        fillHalfRow(1, dexP, jacobian[1], 0);
        fillHalfRow(1, dexV, jacobian[1], 3);
        fillHalfRow(1, deyP, jacobian[2], 0);
        fillHalfRow(1, deyV, jacobian[2], 3);

        // Other intermediate varibles
        final double cle = u / a + ex - eSinE * beta * ey;
        final double sle = v / a + ey + eSinE * beta * ex;
        final double m1 = beta * eCosE;
        final double m2 = 1 - m1 * eCosE;
        final double m3 = (u * ey - v * ex) + eSinE * beta * (u * ex + v * ey);
        final double m4 = -sle + cle * eSinE * beta;
        final double m5 = cle + sle * eSinE * beta;
        // Fill 2 last lines
        fillHalfRow((2 * m3 / r + aOr * eSinE + m1 * eSinE * (1 + m1 - (1 + aOr) * m2) / epsilon)
            / r2, position, (m1 * m2 / epsilon - 1) * oOsqrtMuA, velocity, m4, dexP, m5, deyP,
            -sle / a, duP, cle / a, dvP, jacobian[5], 0);
        fillHalfRow((m1 * m2 / epsilon - 1) * oOsqrtMuA, position, (2 * m3 + eSinE * a + m1 * eSinE
            * r * (eCosE * beta * 2 - aOr * m2) / epsilon)
            / mu, velocity, m4, dexV, m5, deyV, -sle / a, duV, cle / a, dvV, jacobian[5], 3);

        // Return result
        //
        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianEccentricWrtCartesian() {

        // Initialization
        final double ex = this.parameters.getCircularEx();
        final double ey = this.parameters.getCircularEy();

        // start by computing the Jacobian with mean angle
        final double[][] jacobian = this.computeJacobianMeanWrtCartesian();

        // Differentiating the Kepler equation aM = aE - ex sin aE + ey cos aE leads to:
        // daM = (1 - ex cos aE - ey sin aE) dE - sin aE dex + cos aE dey
        // which is inverted and rewritten as:
        // daE = a/r daM + sin aE a/r dex - cos aE a/r dey
        final double alphaE = this.getAlphaE();
        final double[] sincos = MathLib.sinAndCos(alphaE);
        final double cosAe = sincos[1];
        final double sinAe = sincos[0];
        final double aOr = 1 / (1 - ex * cosAe - ey * sinAe);

        // update longitude row
        final double[] rowEx = jacobian[1];
        final double[] rowEy = jacobian[2];
        final double[] rowL = jacobian[5];
        for (int j = 0; j < 6; ++j) {
            rowL[j] = aOr * (rowL[j] + sinAe * rowEx[j] - cosAe * rowEy[j]);
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianTrueWrtCartesian() {

        // Initialization
        final double ex = this.parameters.getCircularEx();
        final double ey = this.parameters.getCircularEy();

        // start by computing the Jacobian with eccentric angle
        final double[][] jacobian = this.computeJacobianEccentricWrtCartesian();

        // Differentiating the eccentric latitude equation
        // tan((aV - aE)/2) = [ex sin aE - ey cos aE] / [sqrt(1-ex^2-ey^2) + 1 - ex cos aE - ey sin
        // aE]
        // leads to
        // cT (daV - daE) = cE daE + cX dex + cY dey
        // with
        // cT = [d^2 + (ex sin aE - ey cos aE)^2] / 2
        // d = 1 + sqrt(1-ex^2-ey^2) - ex cos aE - ey sin aE
        // cE = (ex cos aE + ey sin aE) (sqrt(1-ex^2-ey^2) + 1) - ex^2 - ey^2
        // cX = sin aE (sqrt(1-ex^2-ey^2) + 1) - ey + ex (ex sin aE - ey cos aE) / sqrt(1-ex^2-ey^2)
        // cY = -cos aE (sqrt(1-ex^2-ey^2) + 1) + ex + ey (ex sin aE - ey cos aE) /
        // sqrt(1-ex^2-ey^2)
        // which can be solved to find the differential of the true latitude
        // daV = (cT + cE) / cT daE + cX / cT deX + cY / cT deX
        final double alphaE = this.getAlphaE();
        final double[] sincos = MathLib.sinAndCos(alphaE);
        final double cosAe = sincos[1];
        final double sinAe = sincos[0];
        final double eSinE = ex * sinAe - ey * cosAe;
        final double ecosE = ex * cosAe + ey * sinAe;
        final double e2 = ex * ex + ey * ey;
        final double epsilon = MathLib.sqrt(MathLib.max(0.0, 1 - e2));
        final double onePeps = 1 + epsilon;
        final double d = onePeps - ecosE;
        final double cT = (d * d + eSinE * eSinE) / 2;
        final double cE = ecosE * onePeps - e2;
        final double cX = ex * eSinE / epsilon - ey + sinAe * onePeps;
        final double cY = ey * eSinE / epsilon + ex - cosAe * onePeps;
        final double factorLe = (cT + cE) / cT;
        final double factorEx = cX / cT;
        final double factorEy = cY / cT;

        // update latitude row
        final double[] rowEx = jacobian[1];
        final double[] rowEy = jacobian[2];
        final double[] rowA = jacobian[5];
        for (int j = 0; j < 6; ++j) {
            rowA[j] = factorLe * rowA[j] + factorEx * rowEx[j] + factorEy * rowEy[j];
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected void orbitAddKeplerContribution(final PositionAngle type, final double gm,
                                              final double[] pDot) {

        // Initialization
        final double a = this.parameters.getA();
        final double ex = this.parameters.getCircularEx();
        final double ey = this.parameters.getCircularEy();
        final double alphaV = this.parameters.getAlphaV();

        final double[] sincos = MathLib.sinAndCos(alphaV);
        final double cosAv = sincos[1];
        final double sinAv = sincos[0];

        // Intermediate computation
        final double oMe2 = 1 - ex * ex - ey * ey;
        final double n = MathLib.sqrt(gm / a) / a;
        final double ksi = 1 + ex * cosAv + ey * sinAv;
        // Depends on anomaly type
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
		//              a, ex, ey, i, Right Ascension of the Ascending Node, True latitude argument
        boolean isEqual = true;
		
        if (object == this) { 
            // first fast check
            isEqual = true; 
        } else if (object instanceof CircularOrbit) {
            // cast object to compare parameters
            final CircularOrbit other = (CircularOrbit) object;
        	
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
