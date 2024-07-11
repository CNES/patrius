/**
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
 * @history creation 16/03/2015
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1798:10/12/2018:Add getAlternateEquinoctialParameters
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop ModifiedControlVariable check
//Reason: Orekit code kept as such

/**
 * This class handles traditional keplerian orbital parameters.
 * <p>
 * The parameters used internally are the classical keplerian elements:
 * 
 * <pre>
 *     a
 *     e
 *     i
 *     &omega;
 *     &Omega;
 *     v
 * </pre>
 * 
 * where &omega; stands for the Perigee Argument, &Omega; stands for the Right Ascension of the Ascending Node and v
 * stands for the true anomaly.
 * </p>
 * <p>
 * The eccentricity must be greater than or equal to zero.
 * </p>
 * <p>
 * This class supports hyperbolic orbits, using the convention that semi major axis is negative for such orbits (and of
 * course eccentricity is greater than 1).
 * </p>
 * <p>
 * When orbit is either equatorial or circular, some keplerian elements (more precisely &omega; and &Omega;) become
 * ambiguous so this class should not be used for such orbits. For this reason, {@link EquinoctialParameters equinoctial
 * parameters} is the recommended way to represent orbits.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Emmanuel Bignon
 * @since 3.0
 * @version $Id: KeplerianParameters.java 18071 2017-10-02 16:46:39Z bignon $
 */
public class KeplerianParameters extends AbstractOrbitalParameters {

    /** Serializable UID. */
    private static final long serialVersionUID = -2848224139796971882L;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Eccentricity threshold. */
    private static final double E_LIM = 0.1;

    /** Maximum number of iterations for convergence algorithms. */
    private static final double MAX_ITERATIONS = 50;

    /** Threshold for convergence algorithms. */
    private static final double THRESHOLD = 1.0e-12;

    /** Threshold for equatorial retrograde orbits. */
    private static final double THRESHOLD_RETROGRADE = 1.0e-10;
    
    /** Root int for hash code. */
    private static final int ROOTINT = 351;

    /** First coefficient to compute Kepler equation solver starter. */
    private static final double A;

    /** Second coefficient to compute Kepler equation solver starter. */
    private static final double B;

    static {
        final double k1 = 3 * FastMath.PI + 2;
        final double k2 = FastMath.PI - 1;
        final double k3 = 6 * FastMath.PI - 1;
        A = 3 * k2 * k2 / k1;
        B = k3 * k3 / (6 * k1);
    }

    /** Semi-major axis (m). */
    private final double a;

    /** Eccentricity. (e >= 0) */
    private final double e;

    /** Inclination (rad). */
    private final double i;

    /** Perigee Argument (rad). */
    private final double pa;

    /** Right Ascension of Ascending Node (rad). */
    private final double raan;

    /** True anomaly (rad). */
    private final double v;

    /**
     * Creates a new instance.
     * 
     * @param aIn
     *        semi-major axis (m), negative for hyperbolic orbits
     * @param eIn
     *        eccentricity (e >= 0)
     * @param iIn
     *        inclination (rad)
     * @param paIn
     *        perigee argument (&omega;, rad)
     * @param raanIn
     *        right ascension of ascending node (&Omega;, rad)
     * @param anomaly
     *        mean, eccentric or true anomaly (rad)
     * @param type
     *        type of anomaly
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException
     *            a and e don't match for hyperbolic orbits,
     *            or v is out of range for hyperbolic orbits
     */
    public KeplerianParameters(final double aIn, final double eIn, final double iIn,
        final double paIn, final double raanIn, final double anomaly, final PositionAngle type,
        final double mu) {
        super(mu);

        if (aIn * (1 - eIn) < 0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE, aIn, eIn);
        }

        this.a = aIn;
        this.e = eIn;
        this.i = iIn;
        this.pa = paIn;
        this.raan = raanIn;

        final double tmpV;
        switch (type) {
            case MEAN:
                tmpV = (aIn < 0) ? this.hyperbolicEccentricToTrue(this.meanToHyperbolicEccentric(anomaly))
                    : this.ellipticEccentricToTrue(this.meanToEllipticEccentric(anomaly));
                break;
            case ECCENTRIC:
                tmpV = (aIn < 0) ? this.hyperbolicEccentricToTrue(anomaly)
                    : this.ellipticEccentricToTrue(anomaly);
                break;
            case TRUE:
                tmpV = anomaly;
                break;
            default:
                // this should never happen
                throw PatriusException.createInternalError(null);
        }

        // check true anomaly range
        if (1 + eIn * MathLib.cos(tmpV) <= 0) {
            final double vMax = MathLib.acos(MathLib.min(1.0, MathLib.max(-1.0, -1 / eIn)));
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.ORBIT_ANOMALY_OUT_OF_HYPERBOLIC_RANGE, tmpV, eIn, -vMax, vMax);
        }
        this.v = tmpV;

    }

    /**
     * Get the semi-major axis.
     * 
     * @return semi-major axis (m)
     */
    public double getA() {
        return this.a;
    }

    /**
     * Get the eccentricity.
     * 
     * @return eccentricity
     */
    public double getE() {
        return this.e;
    }

    /**
     * Get the inclination.
     * 
     * @return inclination (rad)
     */
    public double getI() {
        return this.i;
    }

    /**
     * Get the perigee argument.
     * 
     * @return perigee argument (rad)
     */
    public double getPerigeeArgument() {
        return this.pa;
    }

    /**
     * Get the right ascension of the ascending node.
     * 
     * @return right ascension of the ascending node (rad)
     */
    public double getRightAscensionOfAscendingNode() {
        return this.raan;
    }

    /**
     * Get the anomaly.
     * 
     * @param type
     *        type of the angle
     * @return anomaly (rad)
     */
    public double getAnomaly(final PositionAngle type) {
        return (type == PositionAngle.MEAN) ? this.getMeanAnomaly() : ((type == PositionAngle.ECCENTRIC)
            ? this.getEccentricAnomaly() : this.getTrueAnomaly());
    }

    /**
     * Get the true anomaly.
     * 
     * @return true anomaly (rad)
     */
    public double getTrueAnomaly() {
        return this.v;
    }

    /**
     * Get the eccentric anomaly.
     * 
     * @return eccentric anomaly (rad)
     */
    public double getEccentricAnomaly() {
        final double[] sincos = MathLib.sinAndCos(v);
        final double sinv = sincos[0];
        final double cosv = sincos[1];

        if (this.a < 0) {
            // hyperbolic case
            final double sinhH = MathLib.sqrt(MathLib.max(0.0, this.e * this.e - 1)) * sinv
                / (1 + this.e * cosv);
            return MathLib.asinh(sinhH);
        }

        // elliptic case
        final double beta = this.e / (1 + MathLib.sqrt(MathLib.max(0.0, (1 - this.e) * (1 + this.e))));
        return this.v - 2 * MathLib.atan(beta * sinv / (1 + beta * cosv));

    }

    /**
     * Get the mean anomaly.
     * 
     * @return mean anomaly (rad)
     */
    public double getMeanAnomaly() {

        if (this.a < 0) {
            // hyperbolic case
            final double h = this.getEccentricAnomaly();
            return this.e * MathLib.sinh(h) - h;
        }

        // elliptic case
        final double mE = this.getEccentricAnomaly();
        return mE - this.e * MathLib.sin(mE);

    }

    /**
     * Computes the true anomaly from the elliptic eccentric anomaly.
     * 
     * @param anomE
     *        eccentric anomaly (rad)
     * @return v the true anomaly
     */
    private double ellipticEccentricToTrue(final double anomE) {
        final double beta = this.e / (1 + MathLib.sqrt(MathLib.max(0.0, (1 - this.e) * (1 + this.e))));
        final double[] sincos = MathLib.sinAndCos(anomE);
        final double sin = sincos[0];
        final double cos = sincos[1];
        return anomE + 2 * MathLib.atan(beta * sin / (1 - beta * cos));
    }

    /**
     * Computes the true anomaly from the hyperbolic eccentric anomaly.
     * 
     * @param h
     *        hyperbolic eccentric anomaly (rad)
     * @return v the true anomaly
     */
    private double hyperbolicEccentricToTrue(final double h) {
        return 2 * MathLib.atan(MathLib.sqrt(MathLib.max(0.0, (this.e + 1) / (this.e - 1)))
            * MathLib.tanh(h / 2));
    }

    /**
     * Computes the elliptic eccentric anomaly from the mean anomaly.
     * <p>
     * The algorithm used here for solving Kepler equation has been published in:
     * "Procedures for  solving Kepler's Equation", A. W. Odell and R. H. Gooding, Celestial Mechanics 38 (1986) 307-334
     * </p>
     * 
     * @param m
     *        mean anomaly (rad)
     * @return v the true anomaly
     */
    private double meanToEllipticEccentric(final double m) {

        // reduce M to [-PI PI) interval
        final double reducedM = MathUtils.normalizeAngle(m, 0.0);

        // compute start value according to A. W. Odell and R. H. Gooding S12 starter
        // 3 different cases
        double mE;
        if (MathLib.abs(reducedM) < 1.0 / 6.0) {
            mE = reducedM + this.e * (MathLib.cbrt(6 * reducedM) - reducedM);
        } else {
            if (reducedM < 0) {
                final double w = FastMath.PI + reducedM;
                mE = reducedM + this.e * (A * w / (B - w) - FastMath.PI - reducedM);
            } else {
                final double w = FastMath.PI - reducedM;
                mE = reducedM + this.e * (FastMath.PI - A * w / (B - w) - reducedM);
            }
        }

        final double e1 = 1 - this.e;
        // Check to avoid cancellation and improve accuracy
        final boolean noCancellationRisk = (e1 + mE * mE / 6) >= E_LIM;

        // perform two iterations, each consisting of one Halley step and one Newton-Raphson step
        for (int j = 0; j < 2; ++j) {
            // Initialization
            final double f;
            double fd;
            final double[] sincos = MathLib.sinAndCos(mE);
            final double sinE = sincos[0];
            final double cosE = sincos[1];
            final double fdd = this.e * sinE;
            final double fddd = this.e * cosE;
            if (noCancellationRisk) {
                f = (mE - fdd) - reducedM;
                fd = 1 - fddd;
            } else {
                f = this.eMeSinE(mE) - reducedM;
                final double s = MathLib.sin(HALF * mE);
                fd = e1 + 2 * this.e * s * s;
            }
            final double dee = f * fd / (HALF * f * fdd - fd * fd);

            // update eccentric anomaly, using expressions that limit underflow problems
            final double w = fd + HALF * dee * (fdd + dee * fddd / 3);
            fd += dee * (fdd + dee * fddd / 2.);
            mE -= (f - dee * (fd - w)) / fd;
        }

        // expand the result back to original range
        mE += m - reducedM;

        // Return result
        return mE;
    }

    /**
     * Accurate computation of E - e sin(E).
     * <p>
     * This method is used when E is close to 0 and e close to 1, i.e. near the perigee of almost parabolic orbits
     * </p>
     * 
     * @param anomE
     *        eccentric anomaly
     * @return E - e sin(E)
     */
    private double eMeSinE(final double anomE) {
        double x = (1 - this.e) * MathLib.sin(anomE);
        final double mE2 = -anomE * anomE;
        double term = anomE;
        double d = 0;
        // the inequality test below IS intentional and should NOT be replaced by a check with a
        // small tolerance
        for (double x0 = Double.NaN; x != x0;) {
            d += 2;
            term *= mE2 / (d * (d + 1));
            x0 = x;
            x = x - term;
        }
        // Return E - e sin(E)
        return x;
    }

    /**
     * Computes the hyperbolic eccentric anomaly from the mean anomaly.
     * <p>
     * The algorithm used here for solving hyperbolic Kepler equation is a naive initialization and classical Halley
     * method for iterations.
     * </p>
     * 
     * @param m
     *        mean anomaly (rad)
     * @return v the true anomaly
     */
    private double meanToHyperbolicEccentric(final double m) {

        // resolution of hyperbolic Kepler equation for keplerian parameters
        double h = -m;
        double shift = 0.0;
        double hpM = 0.0;
        int iter = 0;
        do {
            // Iteration until convergence
            final double[] sinhcosh = MathLib.sinhAndCosh(h);
            final double sinh = sinhcosh[0];
            final double cosh = sinhcosh[1];
            final double f2 = this.e * sinh;
            final double f1 = this.e * cosh - 1;
            final double f0 = f2 - hpM;

            final double f12 = 2 * f1;
            // Update loop variables
            shift = f0 * f12 / (f1 * f12 - f0 * f2);
            hpM -= shift;
            h = hpM - m;

        } while ((++iter < MAX_ITERATIONS) && (MathLib.abs(shift) > THRESHOLD));

        // Return result
        return h;
    }

    /** {@inheritDoc} */
    @Override
    public CartesianParameters getCartesianParameters() {

        // preliminary variables
        final double[] sincosRaan = MathLib.sinAndCos(raan);
        final double sinRaan = sincosRaan[0];
        final double cosRaan = sincosRaan[1];
        final double[] sincosPa = MathLib.sinAndCos(pa);
        final double sinPa = sincosPa[0];
        final double cosPa = sincosPa[1];
        final double[] sincosI = MathLib.sinAndCos(i);
        final double sinI = sincosI[0];
        final double cosI = sincosI[1];

        final double crcp = cosRaan * cosPa;
        final double crsp = cosRaan * sinPa;
        final double srcp = sinRaan * cosPa;
        final double srsp = sinRaan * sinPa;

        // reference axes defining the orbital plane
        final Vector3D p = new Vector3D(crcp - cosI * srsp, srcp + cosI * crsp, sinI * sinPa);
        final Vector3D q = new Vector3D(-crsp - cosI * srcp, -srsp + cosI * crcp, sinI * cosPa);

        // PV
        final PVCoordinates res = (this.a > 0) ? this.initPVCoordinatesElliptical(p, q)
            : this.initPVCoordinatesHyperbolic(p, q);
        // Return result
        return new CartesianParameters(res, this.getMu());
    }

    /**
     * Initialize the position/velocity coordinates, elliptic case.
     * 
     * @param p
     *        unit vector in the orbital plane pointing towards perigee
     * @param q
     *        unit vector in the orbital plane in quadrature with q
     * @return computed position/velocity coordinates
     */
    private PVCoordinates initPVCoordinatesElliptical(final Vector3D p, final Vector3D q) {

        // elliptic eccentric anomaly
        final double uME2 = (1 - this.e) * (1 + this.e);
        final double s1Me2 = MathLib.sqrt(MathLib.max(0.0, uME2));
        final double mE = this.getEccentricAnomaly();
        final double[] sincos = MathLib.sinAndCos(mE);
        final double sinE = sincos[0];
        final double cosE = sincos[1];

        // coordinates of position, velocity and acceleration in the orbital plane
        final double x = this.a * (cosE - this.e);
        final double y = this.a * sinE * s1Me2;
        final double factor = MathLib.sqrt(this.getMu() / this.a) / (1 - this.e * cosE);
        final double xDot = -sinE * factor;
        final double yDot = cosE * s1Me2 * factor;

        // Compute P, V, A
        final Vector3D position = new Vector3D(x, p, y, q);
        final double r2 = x * x + y * y;
        final Vector3D velocity = new Vector3D(xDot, p, yDot, q);
        final Vector3D acceleration = new Vector3D(-this.getMu() / (r2 * MathLib.sqrt(r2)), position);

        // Return result
        return new PVCoordinates(position, velocity, acceleration);
    }

    /**
     * Initialize the position/velocity coordinates, hyperbolic case.
     * 
     * @param p
     *        unit vector in the orbital plane pointing towards perigee
     * @param q
     *        unit vector in the orbital plane in quadrature with q
     * @return computed position/velocity coordinates
     */
    private PVCoordinates initPVCoordinatesHyperbolic(final Vector3D p, final Vector3D q) {

        // compute position, velocity and acceleration factors
        final double[] sincos = MathLib.sinAndCos(v);
        final double sinV = sincos[0];
        final double cosV = sincos[1];
        final double f = this.a * (1 - this.e * this.e);
        final double posFactor = f / (1 + this.e * cosV);
        final double velFactor = MathLib.sqrt(this.getMu() / f);

        final Vector3D position = new Vector3D(posFactor * cosV, p, posFactor * sinV, q);
        final Vector3D velocity = new Vector3D(-velFactor * sinV, p, velFactor * (this.e + cosV), q);
        final Vector3D acceleration = new Vector3D(-this.getMu() / (posFactor * posFactor * posFactor),
            position);

        return new PVCoordinates(position, velocity, acceleration);
    }

    /** {@inheritDoc} */
    @Override
    public KeplerianParameters getKeplerianParameters() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public CircularParameters getCircularParameters() {
        final double[] sincosPa = MathLib.sinAndCos(pa);
        final double sinPa = sincosPa[0];
        final double cosPa = sincosPa[1];
        final double ex = this.e * cosPa;
        final double ey = this.e * sinPa;
        final double alphaV = this.v + this.pa;
        return new CircularParameters(this.a, ex, ey, this.i, this.raan, alphaV, PositionAngle.TRUE, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public EquatorialParameters getEquatorialParameters() {
        final double pomega = this.raan + this.pa;
        final double siniOver2 = 2. * MathLib.sin(this.i / 2.);
        final double[] sincosRaan = MathLib.sinAndCos(raan);
        final double sinRaan = sincosRaan[0];
        final double cosRaan = sincosRaan[1];
        final double ix = siniOver2 * cosRaan;
        final double iy = siniOver2 * sinRaan;
        return new EquatorialParameters(this.a, this.e, pomega, ix, iy, this.v, PositionAngle.TRUE, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialParameters getEquinoctialParameters() {
        // Ex / Ey
        final double[] sincosRaanPa = MathLib.sinAndCos(this.raan + this.pa);
        final double sinRaanPa = sincosRaanPa[0];
        final double cosRaanPa = sincosRaanPa[1];
        final double ex = this.e * cosRaanPa;
        final double ey = this.e * sinRaanPa;

        final double hx;
        final double hy;
        // Check for equatorial retrograde orbit
        if (MathLib.abs(this.i - FastMath.PI) < THRESHOLD_RETROGRADE) {
            hx = Double.NaN;
            hy = Double.NaN;
        } else {
            // Regular orbit
            final double taniOver2 = MathLib.tan(this.i / 2.);
            final double[] sincosRaan = MathLib.sinAndCos(raan);
            final double sinRaan = sincosRaan[0];
            final double cosRaan = sincosRaan[1];
            hx = taniOver2 * cosRaan;
            hy = taniOver2 * sinRaan;
        }

        // Longitude argument
        final double lv = this.v + this.raan + this.pa;

        // Return result
        return new EquinoctialParameters(this.a, ex, ey, hx, hy, lv, PositionAngle.TRUE, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public ApsisAltitudeParameters getApsisAltitudeParameters(final double ae) {
        final double rp = this.a * (1. - this.e) - ae;
        final double ra = this.a * (1. + this.e) - ae;
        return new ApsisAltitudeParameters(rp, ra, this.i, this.pa, this.raan, this.v, PositionAngle.TRUE,
            this.getMu(),
            ae);
    }

    /** {@inheritDoc} */
    @Override
    public ApsisRadiusParameters getApsisRadiusParameters() {
        final double rp = this.a * (1. - this.e);
        final double ra = this.a * (1. + this.e);
        return new ApsisRadiusParameters(rp, ra, this.i, this.pa, this.raan, this.v, PositionAngle.TRUE, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public ReentryParameters getReentryParameters(final double ae, final double f) {
        return this.getCartesianParameters().getReentryParameters(ae, f);
    }

    /** {@inheritDoc} */
    @Override
    public StelaEquinoctialParameters getStelaEquinoctialParameters() {
        // Raan + Pa
        final double[] sincosRaanPa = MathLib.sinAndCos(this.raan + this.pa);
        final double sinRaanPa = sincosRaanPa[0];
        final double cosRaanPa = sincosRaanPa[1];
        // Ex/Ey
        final double ex = this.e * cosRaanPa;
        final double ey = this.e * sinRaanPa;
        final double siniOver2 = MathLib.sin(this.i / 2.);
        // Raan
        final double[] sincosRaan = MathLib.sinAndCos(raan);
        final double sinRaan = sincosRaan[0];
        final double cosRaan = sincosRaan[1];
        // Ix/Iy
        final double ix = siniOver2 * cosRaan;
        final double iy = siniOver2 * sinRaan;
        // Longitude argument
        final double lm = this.getMeanAnomaly() + this.raan + this.pa;
        return new StelaEquinoctialParameters(this.a, ex, ey, ix, iy, lm, this.getMu(), true);
    }

    /** {@inheritDoc} */
    @Override
    public AlternateEquinoctialParameters getAlternateEquinoctialParameters() {
        return this.getEquinoctialParameters().getAlternateEquinoctialParameters();
    }

    /**
     * Returns a string representation of this keplerian parameters object.
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return new StringBuffer().append("keplerian parameters: ").append('{').append("a: ")
            .append(this.a).append("; e: ").append(this.e).append("; i: ").append(MathLib.toDegrees(this.i))
            .append("; pa: ").append(MathLib.toDegrees(this.pa)).append("; raan: ")
            .append(MathLib.toDegrees(this.raan)).append("; v: ").append(MathLib.toDegrees(this.v))
            .append(";}").toString();
    }

    // CHECKSTYLE: resume ModifiedControlVariable check
    
    /** {@inheritDoc} */
    @Override
	public boolean equals(final Object object) {
		// parameters : date, frame, type, mu,
		//              a, e, i, Right Ascension of the Ascending Node, Perigee Argument,
		//              true anomaly
        boolean isEqual = true;
		
        if (object == this) { 
            // first fast check
            isEqual = true; 
        } else if (object instanceof KeplerianParameters) {
            // cast object to compare parameters
            final KeplerianParameters other = (KeplerianParameters) object;
        	
            
            isEqual &= (this.getMu() == other.getMu());
        	
        	// Keplerian parameters
            isEqual &= (this.getA() == other.getA());
            isEqual &= (this.getE() == other.getE());
            isEqual &= (this.getI() == other.getI());
            isEqual &= (this.getRightAscensionOfAscendingNode() 
        			== other.getRightAscensionOfAscendingNode());
            isEqual &= (this.getPerigeeArgument() == other.getPerigeeArgument());
            isEqual &= (this.getTrueAnomaly() == other.getTrueAnomaly());
        	
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
        result = effMult * result + MathUtils.hash(this.getMu());
        result = effMult * result + MathUtils.hash(this.getA());
        result = effMult * result + MathUtils.hash(this.getE());
        result = effMult * result + MathUtils.hash(this.getI());
        result = effMult * result + MathUtils.hash(this.getRightAscensionOfAscendingNode());
        result = effMult * result + MathUtils.hash(this.getPerigeeArgument());
        result = effMult * result + MathUtils.hash(this.getTrueAnomaly());
 
        return result; 
    }
}