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
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class handles the equinoctial orbital parameters used in Stela;
 * it has been created because the equinoctial parameters associated to Stela
 * differ from the {@link EquinoctialParameters} parameters.
 * <p>
 * The parameters used internally are the equinoctial elements which can be related to keplerian elements as follows:
 * 
 * <pre>
 *     a
 *     ex = e cos(&omega; + &Omega;)
 *     ey = e sin(&omega; + &Omega;)
 *     ix = sin(i/2) cos(&Omega;)
 *     iy = sin(i/2) sin(&Omega;)
 *     lM = M + &omega; + &Omega;
 * </pre>
 * 
 * where &omega; stands for the Perigee Argument and &Omega; stands for the Right Ascension of the Ascending Node.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Emmanuel Bignon
 * @since 3.0
 * @version $Id: StelaEquinoctialParameters.java 18071 2017-10-02 16:46:39Z bignon $
 */
public class StelaEquinoctialParameters extends AbstractOrbitalParameters {

    /** Inclination upper limit. */
    public static final double I_LIM = MathLib.toRadians(179.5);

    /** Sinus of half limit inclination in type 8. */
    public static final double SIN_I_LIM = MathLib.sin(I_LIM / 2.0);
    
    /** Root int for hash code. */
    private static final int ROOTINT = 356;

    /** Serializable UID. */
    private static final long serialVersionUID = -3632727570913759537L;

    /** Record of last RAAN used in T2 to T1 conversion (to save computation time). */
    private static double raansvg = 0.0;
    /** Record of last cosine of RAAN computed in T2 to T1 conversion (to save computation time). */
    private static double cosRaansvg = 1.0;
    /** Record of last sine of RAAN computed in T2 to T1 conversion (to save computation time). */
    private static double sinRaansvg = 0.0;

    /** Record of last perigee argument used in T2 to T1 conversion (to save computation time). */
    private static double wsvg = 0.0;
    /**
     * Record of last cosine of perigee argument computed in T2 to T1 conversion (to save
     * computation time).
     */
    private static double coswsvg = 1.0;
    /**
     * Record of last sine of perigee argument computed in T2 to T1 conversion (to save computation
     * time).
     */
    private static double sinwsvg = 0.0;

    /** Record of last inclination used in T2 to T1 conversion (to save computation time). */
    private static double inclsvg = 0.0;
    /**
     * Record of last cosine of inclination computed in T2 to T1 conversion (to save computation
     * time).
     */
    private static double cosInclsvg = 1.0;
    /**
     * Record of last sine of inclination computed in T2 to T1 conversion (to save computation
     * time).
     */
    private static double sinInclsvg = 0.0;

    /** Semi-major axis (m). */
    private final double a;

    /** First component of the eccentricity vector. */
    private final double ex;

    /** Second component of the eccentricity vector. */
    private final double ey;

    /** First component of the inclination vector. */
    private final double ix;

    /** Second component of the inclination vector. */
    private final double iy;

    /** Mean longitude argument (rad). */
    private final double lM;

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
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param needCorrection
     *        true if the inclination correction has to be done
     * @exception IllegalArgumentException
     *            if eccentricity is equal to 1 or larger
     */
    public StelaEquinoctialParameters(final double aIn, final double exIn, final double eyIn,
        final double ixIn, final double iyIn, final double lMIn, final double mu,
        final boolean needCorrection) {
        super(mu);

        if (exIn * exIn + eyIn * eyIn >= 1.0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS, this.getClass().getName());
        }
        // handle cases where periapsis is below earth surface
        if ((aIn * (1 - MathLib.sqrt(exIn * exIn + eyIn * eyIn)) - Constants.EGM96_EARTH_EQUATORIAL_RADIUS) < 0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS, this.getClass().getName());
        }

        double[] res = { ixIn, iyIn };
        if (needCorrection) {
            res = this.correctInclination(ixIn, iyIn);
        }

        this.a = aIn;
        this.ex = exIn;
        this.ey = eyIn;
        this.ix = res[0];
        this.iy = res[1];
        this.lM = lMIn;
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
     * Get the first component of the eccentricity vector.
     * 
     * @return e cos(&omega; + &Omega;), first component of the eccentricity vector
     */
    public double getEquinoctialEx() {
        return this.ex;
    }

    /**
     * Get the second component of the eccentricity vector.
     * 
     * @return e sin(&omega; + &Omega;), second component of the eccentricity vector
     */
    public double getEquinoctialEy() {
        return this.ey;
    }

    /**
     * Get the first component of the inclination vector.
     * 
     * @return sin(i/2) cos(&Omega;), first component of the inclination vector
     */
    public double getIx() {
        return this.ix;
    }

    /**
     * Get the second component of the inclination vector.
     * 
     * @return sin(i/2) sin(&Omega;), second component of the inclination vector
     */
    public double getIy() {
        return this.iy;
    }

    /**
     * Get the mean longitude argument.
     * 
     * @return M + &omega; + &Omega; mean longitude argument (rad)
     */
    public double getLM() {
        return this.lM;
    }

    /**
     * Inclination correction because of inclination singularity in StelaEquinoctial parameters
     * around 180deg.
     * 
     * @param ixIn
     *        first component of inclination vector
     * @param iyIn
     *        second component of inclination vector
     * @return corrected inclination components
     */
    public double[] correctInclination(final double ixIn, final double iyIn) {
        // Inclination correction (singularity around 180deg)
        // Inclination is clamped to 179.5deg
        final double[] result = { ixIn, iyIn };
        double sinIOver2 = MathLib.sqrt(ixIn * ixIn + iyIn * iyIn);
        // "if block" needed only to avoid null inclination
        // but for efficiency reasons, this code should be avoided as much as possible, so the
        // condition
        if (sinIOver2 > SIN_I_LIM) {
            final double cosGom = ixIn / sinIOver2;
            final double sinGom = iyIn / sinIOver2;
            sinIOver2 = SIN_I_LIM;
            // Then inclination is turned into type 8
            result[0] = sinIOver2 * cosGom;
            result[1] = sinIOver2 * sinGom;
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public CartesianParameters getCartesianParameters() {

        // Get keplerian parameters
        final KeplerianParameters kep = this.getStelaKeplerianParameters();
        final double e = kep.getE();
        final double i = kep.getI();
        final double raan = kep.getRightAscensionOfAscendingNode();
        final double pa = kep.getPerigeeArgument();
        final double v = kep.getTrueAnomaly();

        // Preliminary note : all formulas come from the reference Space Mechanics book
        // "Trajectoires Spatiales" O. ZARROUATI ISBN 2.85428.166.7 ed. 01/1987 Annexe A1.3

        // Intermediate computations
        // Compute eccentric anomaly
        final double beta = e / (1 + MathLib.sqrt(MathLib.max(0.0, (1 - e) * (1 + e))));
        final double[] sincosv = MathLib.sinAndCos(v);
        final double sinv = sincosv[0];
        final double cosv = sincosv[1];
        final double eccA = v - 2 * MathLib.atan(beta * sinv / (1 + beta * cosv));
        final double[] sincosEcc = MathLib.sinAndCos(eccA);
        final double sinE = sincosEcc[0];
        final double cosE = sincosEcc[1];

        // Cache in order to speed-up computation
        // Compute cos(Raan) and sin(Raan)
        final double cosRaan;
        final double sinRaan;
        if (raan == raansvg) {
            cosRaan = cosRaansvg;
            sinRaan = sinRaansvg;
        } else {
            final double[] sincosRaan = MathLib.sinAndCos(raan);
            sinRaan = sincosRaan[0];
            cosRaan = sincosRaan[1];
            cosRaansvg = cosRaan;
            sinRaansvg = sinRaan;
            raansvg = raan;
        }

        // Compute cos(w) and sin(w)
        final double cosW;
        final double sinW;
        if (pa == wsvg) {
            cosW = coswsvg;
            sinW = sinwsvg;
        } else {
            cosW = MathLib.cos(pa);
            sinW = MathLib.sin(pa);
            coswsvg = cosW;
            sinwsvg = sinW;
            wsvg = pa;
        }

        // Compute cos(incl) and sin(incl)
        final double cosI;
        final double sinI;
        if (i == inclsvg) {
            cosI = cosInclsvg;
            sinI = sinInclsvg;
        } else {
            cosI = MathLib.cos(i);
            sinI = MathLib.sin(i);
            cosInclsvg = cosI;
            sinInclsvg = sinI;
            inclsvg = i;
        }

        // Intermediate variables
        final double sqrtOneMinusE2 = MathLib.sqrt(MathLib.max(0.0, 1. - e * e));
        final double cposp = cosE - e;
        final double cposq = sinE * sqrtOneMinusE2;

        final double oneMinusecosE = 1. - e * cosE;
        final double cvelp = -sinE / oneMinusecosE;
        final double cvelq = cosE * sqrtOneMinusE2 / oneMinusecosE;

        final double sqrtMuOverA = MathLib.sqrt(this.getMu() / this.a);

        final Vector3D p = new Vector3D((cosRaan * cosW) - (cosI * sinW * sinRaan),
            (sinRaan * cosW) + (cosI * sinW * cosRaan), sinI * sinW);
        final Vector3D q = new Vector3D(-(cosRaan * sinW) - (cosI * cosW * sinRaan),
            -(sinRaan * sinW) + (cosI * cosW * cosRaan), sinI * cosW);
        // compute final position and velocity
        final Vector3D pos = p.scalarMultiply(cposp).add(q.scalarMultiply(cposq)).scalarMultiply(this.a);
        final Vector3D vel = p.scalarMultiply(cvelp).add(q.scalarMultiply(cvelq))
            .scalarMultiply(sqrtMuOverA);

        // acceleration
        final double r2 = pos.getNormSq();
        final Vector3D acceleration = new Vector3D(-this.getMu() / (r2 * MathLib.sqrt(r2)), pos);

        // Return result
        return new CartesianParameters(pos, vel, acceleration, this.getMu());
    }

    /** {@inheritDoc} */
    /**
     * Convert to Stela keplerian parameters. The only difference with usual keplerian elements
     * is to return modulated angles in the range [0;2Pi[.
     * 
     * @return Stela keplerian parameters
     */
    private KeplerianParameters getStelaKeplerianParameters() {
        final double e = MathLib.sqrt(this.ex * this.ex + this.ey * this.ey);
        final double value = MathLib.sqrt(this.ix * this.ix + this.iy * this.iy);
        final double i = 2. * MathLib.asin(MathLib.min(1.0, value));
        final double raan = this.mod(MathLib.atan2(this.iy, this.ix), 2. * FastMath.PI);
        final double pa = this.mod(MathLib.atan2(this.ey, this.ex) - raan, 2. * FastMath.PI);
        final double m = this.mod(this.lM - pa - raan, 2. * FastMath.PI);
        return new KeplerianParameters(this.a, e, i, pa, raan, m, PositionAngle.MEAN, this.getMu());
    }

    /**
     * Computes "x" modulo "mod".
     * 
     * @param x
     *        value to modulate
     * @param mod
     *        modulo (for instance &pi;)
     * @return "x" modulo "mod"
     */
    private double mod(final double x, final double mod) {
        return ((x % mod) + mod) % mod;
    }

    /** {@inheritDoc} */
    @Override
    public KeplerianParameters getKeplerianParameters() {
        final double e = MathLib.sqrt(this.ex * this.ex + this.ey * this.ey);
        final double value = MathLib.sqrt(this.ix * this.ix + this.iy * this.iy);
        final double i = 2. * MathLib.asin(MathLib.min(1.0, value));
        final double raan = MathLib.atan2(this.iy, this.ix);
        final double pa = MathLib.atan2(this.ey, this.ex) - raan;
        final double m = this.lM - pa - raan;
        return new KeplerianParameters(this.a, e, i, pa, raan, m, PositionAngle.MEAN, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public CircularParameters getCircularParameters() {
        final double value = MathLib.sqrt(this.ix * this.ix + this.iy * this.iy);
        final double i = 2. * MathLib.asin(MathLib.min(1.0, value));
        final double raan = MathLib.atan2(this.iy, this.ix);
        final double[] sincosRaan = MathLib.sinAndCos(raan);
        final double sinRaan = sincosRaan[0];
        final double cosRaan = sincosRaan[1];
        final double cirqex = this.ex * cosRaan + this.ey * sinRaan;
        final double cirqey = this.ey * cosRaan - this.ex * sinRaan;
        final double alphaM = this.lM - raan;
        return new CircularParameters(this.a, cirqex, cirqey, i, raan, alphaM, PositionAngle.MEAN,
            this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public EquatorialParameters getEquatorialParameters() {
        final double e = MathLib.sqrt(this.ex * this.ex + this.ey * this.ey);
        final double pomega = MathLib.atan2(this.ey, this.ex);
        final double m = this.lM - pomega;
        return new EquatorialParameters(this.a, e, pomega, 2. * this.ix, 2. * this.iy, m, PositionAngle.MEAN,
            this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialParameters getEquinoctialParameters() {
        final double value = MathLib.sqrt(this.ix * this.ix + this.iy * this.iy);
        final double iOver2 = MathLib.asin(MathLib.min(1.0, value));
        final double raan = MathLib.atan2(this.iy, this.ix);
        final double[] sincosRaan = MathLib.sinAndCos(raan);
        final double sinRaan = sincosRaan[0];
        final double cosRaan = sincosRaan[1];
        final double hx = MathLib.tan(iOver2) * cosRaan;
        final double hy = MathLib.tan(iOver2) * sinRaan;
        return new EquinoctialParameters(this.a, this.ex, this.ey, hx, hy, this.lM, PositionAngle.MEAN, this.getMu());
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
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public AlternateEquinoctialParameters getAlternateEquinoctialParameters() {
        return this.getEquinoctialParameters().getAlternateEquinoctialParameters();
    }

    /**
     * Returns a string representation of this orbit parameters object.
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return new StringBuffer().append("Stela equinoctial parameters: ").append('{')
            .append("a: ").append(this.a).append("; ex: ").append(this.ex).append("; ey: ").append(this.ey)
            .append("; ix: ").append(this.ix).append("; iy: ").append(this.iy).append("; lM: ")
            .append(MathLib.toDegrees(this.lM)).append(";}").toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        // parameters : date, frame, type, mu,
        //              a, ex, ey, hx, hy, lv
        boolean isEqual = true;
        
        if (object == this) { 
            // first fast check
            isEqual = true; 
        } else if (object instanceof StelaEquinoctialParameters) {
            // cast object to compare parameters
            final StelaEquinoctialParameters other = (StelaEquinoctialParameters) object;
            
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
        result = effMult * result + MathUtils.hash(this.getEquinoctialEx());
        result = effMult * result + MathUtils.hash(this.getEquinoctialEy());
        result = effMult * result + MathUtils.hash(this.getIx());
        result = effMult * result + MathUtils.hash(this.getIy());
        result = effMult * result + MathUtils.hash(this.getLM());
 
        return result; 
    }
}
