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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1798:10/12/2018 add getAlternateEquinoctialParameters
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.EquatorialOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop ModifiedControlVariable check
//Reason: Orekit code kept as such

/**
 * This class handles non circular equatorial orbital parameters.
 * <p>
 * The parameters used internally are the following elements:
 * 
 * <pre>
 *     a semi-major axis (m)
 *     e eccentricity
 *     pomega = &omega; + &Omega; , longitude of the periapsis;
 *     ix = 2 sin(i/2) cos(&Omega;), first component of inclination vector
 *     iy = 2 sin(i/2) sin(&Omega;), second component of inclination vector
 *     anomaly (M or E or v);, mean, eccentric or true anomaly (rad)
 * </pre>
 * 
 * where &omega; stands for the Periapsis Argument, &Omega; stands for the Right Ascension of the Ascending Node.
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
 * @version $Id: EquatorialParameters.java 18071 2017-10-02 16:46:39Z bignon $
 */
public class EquatorialParameters extends AbstractOrbitalParameters {

    /** Serializable UID. */
    private static final long serialVersionUID = 4180472353882013018L;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Eccentricity threshold. */
    private static final double E_LIM = 0.1;

    /** Root int for hash code. */
    private static final int ROOTINT = 355;

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

    /** Eccentricity. */
    private final double e;

    /** &omega; + &Omega; (rad). */
    private final double pomega;

    /**
     * first component of inclination vector (rad).
     * ix = 2 sin(i/2) cos(&Omega;).
     */
    private final double ix;

    /**
     * second component of inclination vector (rad).
     * iy = 2 sin(i/2) sin(&Omega;)
     */
    private final double iy;

    /** anomaly (rad). */
    private final double v;
    
    /**
     * Creates a new instance.
     * 
     * @param aIn
     *        semi-major axis (m)
     * @param eIn
     *        eccentricity
     * @param pomegaIn
     *        &omega; + &Omega; (rad)
     * @param ixIn
     *        2 sin(i/2) cos(&Omega;), first component of inclination vector
     * @param iyIn
     *        2 sin(i/2) sin(&Omega;), second component of inclination vector
     * @param anomaly
     *        (M or E or v) = anomaly mean, eccentric or true anomaly (rad)
     * @param type
     *        type of anomaly
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException
     *            if orbit is hyperbolic
     * @exception IllegalArgumentException
     *            if orbit mismatch with conic type
     * @exception IllegalArgumentException
     *            if inclination vector is not valid, meaning ix^2 + iy^2 > 4
     */
    public EquatorialParameters(final double aIn, final double eIn, final double pomegaIn,
        final double ixIn, final double iyIn, final double anomaly, final PositionAngle type,
        final double mu) {
        super(mu);

        if (aIn * (1. - eIn) < 0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE, aIn, eIn);
        }

        this.a = aIn;
        this.e = eIn;
        this.pomega = pomegaIn;
        this.ix = ixIn;
        this.iy = iyIn;

        final double squareNormI = ixIn * ixIn + iyIn * iyIn;
        if (squareNormI > 4) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.WRONG_INVERSE_TRIGONOMETRIC_FUNCTION_ARGUMENT);
        }

        final double tmpV;
        switch (type) {
            case MEAN:
                if (aIn < 0) {
                    throw PatriusException.createIllegalArgumentException(
                        PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                        EquatorialOrbit.class.getSimpleName());
                }
                tmpV = this.ellipticEccentricToTrue(this.meanToEllipticEccentric(anomaly));
                break;
            case ECCENTRIC:
                if (aIn < 0) {
                    throw PatriusException.createIllegalArgumentException(
                        PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                        EquatorialOrbit.class.getSimpleName());
                }
                tmpV = this.ellipticEccentricToTrue(anomaly);
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
     * Get the first component of the inclination vector.
     * ix = 2 sin(i/2) cos(&Omega;)
     * 
     * @return first component of the inclination vector.
     */
    public double getIx() {
        return this.ix;
    }

    /**
     * Get the second component of the inclination vector.
     * iy = 2 sin(i/2) sin(&Omega;)
     * 
     * @return second component of the inclination vector.
     */
    public double getIy() {
        return this.iy;
    }

    /**
     * Get the longitude of the periapsis (&omega; + &Omega;).
     * 
     * @return longitude of the periapsis (rad)
     */
    public double getPomega() {
        return this.pomega;
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
     * Get the mean anomaly.
     * 
     * @return mean anomaly (rad)
     */
    public double getMeanAnomaly() {

        if (this.a < 0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                EquatorialOrbit.class.getSimpleName());
        }

        // elliptic case
        final double mE = this.getEccentricAnomaly();
        return mE - this.e * MathLib.sin(mE);
    }

    /**
     * Get the eccentric anomaly.
     * 
     * @return eccentric anomaly (rad)
     */
    public double getEccentricAnomaly() {
        if (this.a < 0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                EquatorialOrbit.class.getSimpleName());
        }

        // elliptic case
        final double beta = this.e / (1 + MathLib.sqrt(MathLib.max(0.0, (1 - this.e) * (1 + this.e))));
        final double[] sincos = MathLib.sinAndCos(v);
        final double sin = sincos[0];
        final double cos = sincos[1];
        return this.v - 2 * MathLib.atan(beta * sin / (1 + beta * cos));
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
        // Avoid cancellation for increased accuracy
        final boolean noCancellationRisk = (e1 + mE * mE / 6) >= E_LIM;

        // perform two iterations, each consisting of one Halley step and one Newton-Raphson step
        for (int j = 0; j < 2; ++j) {
            // Initialization
            final double f;
            double fd;
            final double[] sincos = MathLib.sinAndCos(mE);
            final double sin = sincos[0];
            final double cos = sincos[1];
            final double fdd = this.e * sin;
            final double fddd = this.e * cos;
            if (noCancellationRisk) {
                // Standard case
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
        // return E - e sin(E)
        return x;
    }

    /** {@inheritDoc} */
    @Override
    public CartesianParameters getCartesianParameters() {
        // Initialization
        final double hx = this.ix * HALF;
        final double hy = this.iy * HALF;
        final double anome = this.getEccentricAnomaly();
        final double[] sincoso = MathLib.sinAndCos(pomega);
        final double sino = sincoso[0];
        final double coso = sincoso[1];
        final double[] sincose = MathLib.sinAndCos(anome);
        final double sine = sincose[0];
        final double cose = sincose[1];

        // Temporary variables
        final double raca = MathLib.sqrt(this.getMu() / this.a);
        final double race = MathLib.sqrt(MathLib.max(0.0, 1. - this.e * this.e));
        final double unsec = 1. / (1. - this.e * cose);
        final double h1 = cose - this.e;
        final double h2 = race * sine;
        final double hx2 = hx * hx;
        final double hy2 = hy * hy;
        final double rach = MathLib.sqrt(MathLib.max(0.0, 1. - hx2 - hy2));
        final Vector3D f = new Vector3D(1. - 2. * hy2, 2. * hx * hy, -2. * rach * hy);
        final Vector3D g = new Vector3D(f.getY(), 1. - 2. * hx2, 2. * rach * hx);
        final double f1 = h1 * coso - h2 * sino;
        final double f2 = h1 * sino + h2 * coso;

        // Optimisation for computation speed-up
        final double dh1dan = -sine;
        final double dh2dan = race * cose;
        final double df1dan = dh1dan * coso - dh2dan * sino;
        final double df2dan = dh1dan * sino + dh2dan * coso;
        final double g1 = unsec * df1dan;
        final double g2 = unsec * df2dan;

        // Compute position
        final Vector3D rau = f.scalarMultiply(f1).add(g.scalarMultiply(f2));
        final Vector3D position = new Vector3D(this.a, rau);

        // Compute velocity
        final Vector3D velocity = (f.scalarMultiply(g1).add(g.scalarMultiply(g2)))
            .scalarMultiply(raca);

        // Compute acceleration
        final double r2 = position.getNormSq();
        final Vector3D acceleration = new Vector3D(-this.getMu() / (r2 * MathLib.sqrt(r2)), position);

        // Return result
        //
        return new CartesianParameters(position, velocity, acceleration, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public KeplerianParameters getKeplerianParameters() {
        final double squareNormI = this.ix * this.ix + this.iy * this.iy;
        final double value = HALF * MathLib.sqrt(squareNormI);
        final double i = 2. * MathLib.asin(MathLib.min(1.0, value));
        final double cosiOver2 = HALF / MathLib.cos(i / 2.);
        final double hx = this.ix * cosiOver2;
        final double hy = this.iy * cosiOver2;
        final double raan = MathLib.atan2(hy, hx);
        final double pa = this.pomega - raan;
        return new KeplerianParameters(this.a, this.e, i, pa, raan, this.v, PositionAngle.TRUE, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public CircularParameters getCircularParameters() {
        // I
        final double squareNormI = this.ix * this.ix + this.iy * this.iy;
        final double value = HALF * MathLib.sqrt(squareNormI);
        final double i = 2. * MathLib.asin(MathLib.min(1.0, value));
        // RAAN
        final double cosiOver2 = HALF / MathLib.cos(i / 2.);
        final double hx = this.ix * cosiOver2;
        final double hy = this.iy * cosiOver2;
        final double raan = MathLib.atan2(hy, hx);
        // Ex, Ey
        final double[] sincospa = MathLib.sinAndCos(pomega);
        final double sinpa = sincospa[0];
        final double cospa = sincospa[1];
        final double[] sincosRaan = MathLib.sinAndCos(raan);
        final double sinRaan = sincosRaan[0];
        final double cosRaan = sincosRaan[1];
        final double ex = this.e * cospa;
        final double ey = this.e * sinpa;
        final double cirqex = ex * cosRaan + ey * sinRaan;
        final double cirqey = ey * cosRaan - ex * sinRaan;
        final double alphaV = this.pomega + this.v - raan;
        // Return result
        return new CircularParameters(this.a, cirqex, cirqey, i, raan, alphaV, PositionAngle.TRUE,
            this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public EquatorialParameters getEquatorialParameters() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialParameters getEquinoctialParameters() {
        // I
        final double squareNormI = this.ix * this.ix + this.iy * this.iy;
        final double value = HALF * MathLib.sqrt(squareNormI);
        final double i = 2. * MathLib.asin(MathLib.min(1.0, value));
        final double cosiOver2 = HALF / MathLib.cos(i / 2.);
        // Hx/Hy
        final double hx = this.ix * cosiOver2;
        final double hy = this.iy * cosiOver2;
        final double[] sincos = MathLib.sinAndCos(pomega);
        final double sin = sincos[0];
        final double cos = sincos[1];
        // Ex/Ey
        final double ex = this.e * cos;
        final double ey = this.e * sin;
        // Longitude argument
        final double lv = this.pomega + this.v;
        return new EquinoctialParameters(this.a, ex, ey, hx, hy, lv, PositionAngle.TRUE, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public ApsisAltitudeParameters getApsisAltitudeParameters(final double ae) {
        return this.getKeplerianParameters().getApsisAltitudeParameters(ae);
    }

    /** {@inheritDoc} */
    @Override
    public ApsisRadiusParameters getApsisRadiusParameters() {
        return this.getKeplerianParameters().getApsisRadiusParameters();
    }

    /** {@inheritDoc} */
    @Override
    public ReentryParameters getReentryParameters(final double ae, final double f) {
        return this.getCartesianParameters().getReentryParameters(ae, f);
    }

    /** {@inheritDoc} */
    @Override
    public StelaEquinoctialParameters getStelaEquinoctialParameters() {
        final double[] sincos = MathLib.sinAndCos(pomega);
        final double sin = sincos[0];
        final double cos = sincos[1];
        final double ex = this.e * cos;
        final double ey = this.e * sin;
        final double lM = this.pomega + this.getMeanAnomaly();
        return new StelaEquinoctialParameters(this.a, ex, ey, this.ix / 2., this.iy / 2., lM, this.getMu(), true);
    }

    /** {@inheritDoc} */
    @Override
    public AlternateEquinoctialParameters getAlternateEquinoctialParameters() {
        return this.getEquinoctialParameters().getAlternateEquinoctialParameters();
    }

    /**
     * Returns a string representation of this non circular equatorial
     * orbital parameters object.
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return new StringBuffer().append("equatorial parameters: ").append('{').append("a: ")
            .append(this.a).append("; e: ").append(this.e).append("; pomega: ")
            .append(MathLib.toDegrees(this.pomega)).append("; ix: ").append(this.ix).append("; iy: ")
            .append(this.iy).append("; v: ").append(MathLib.toDegrees(this.v)).append(";}").toString();
    }

    // CHECKSTYLE: resume ModifiedControlVariable check
        
    /** {@inheritDoc} */
    @Override
	public boolean equals(final Object object) {
		// parameters : date, frame, type, mu,
		//              a, e, pomega, ix, iy, v
        boolean isEqual = true;
		
        if (object == this) {
            // first fast check
            isEqual = true; 
        } else if (object instanceof EquatorialParameters) {
            // cast object to compare parameters
            final EquatorialParameters other = (EquatorialParameters) object;
            isEqual &= (this.getMu() == other.getMu());
        	
        	// Equatorial parameters
            isEqual &= (this.getA() == other.getA());
            isEqual &= (this.getE() == other.getE());
            isEqual &= (this.getPomega() == other.getPomega());
            isEqual &= (this.getIx() == other.getIx());
            isEqual &= (this.getIy() == other.getIy());
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
        result = effMult * result + MathUtils.hash(this.getPomega());
        result = effMult * result + MathUtils.hash(this.getIx());
        result = effMult * result + MathUtils.hash(this.getIy());
        result = effMult * result + MathUtils.hash(this.getTrueAnomaly());
 
        return result; 
    }
}