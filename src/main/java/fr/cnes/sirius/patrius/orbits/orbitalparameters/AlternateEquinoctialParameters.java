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
 * @history creation 10/12/2018
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1798:10/12/2018 Creation Class
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class handles alternate equinoctial orbital parameters, which can support both
 * circular and equatorial orbits.
 * <p>
 * The parameters used internally are the alternate equinoctial elements which can be related to keplerian elements as
 * follows:
 * 
 * <pre>
 *     n
 *     ex = e cos(&omega; + &Omega;)
 *     ey = e sin(&omega; + &Omega;)
 *     hx = tan(i/2) cos(&Omega;)
 *     hy = tan(i/2) sin(&Omega;)
 *     lM = M + &omega; + &Omega;
 * </pre>
 * 
 * where &omega; stands for the Perigee Argument and &Omega; stands for the Right Ascension of the Ascending Node.
 * </p>
 * <p>
 * Alternate equinoctial parameters are derived from equinoctial parameter (see {@link EquinoctialParameters} for more
 * information) and they are particularly interesting for uncertainty propagation.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Stefano Crepaldi
 * @since 3.0
 * @version $Id: EquinoctialParameters.java 18071 2017-10-02 16:46:39Z bignon $
 */
public class AlternateEquinoctialParameters extends AbstractOrbitalParameters {

    /** Serializable UID. */
    private static final long serialVersionUID = 4685153744711684793L;

    /** Maximum number of iterations for convergence algorithms. */
    private static final double MAX_ITERATIONS = 50;

    /** Threshold for convergence algorithms. */
    private static final double THRESHOLD = 1.0e-12;
    
    /** Root int for hash code. */
    private static final int ROOTINT = 352;

    /** Mean motion (1/s). */
    private final double n;

    /** First component of the eccentricity vector. */
    private final double ex;

    /** Second component of the eccentricity vector. */
    private final double ey;

    /** First component of the inclination vector. */
    private final double hx;

    /** Second component of the inclination vector. */
    private final double hy;

    /** Mean longitude argument (rad). */
    private final double lM;

    /**
     * Creates a new instance.
     * 
     * @param nIn
     *        mean motion (1/s)
     * @param exIn
     *        e cos(&omega; + &Omega;), first component of eccentricity vector
     * @param eyIn
     *        e sin(&omega; + &Omega;), second component of eccentricity vector
     * @param hxIn
     *        tan(i/2) cos(&Omega;), first component of inclination vector
     * @param hyIn
     *        tan(i/2) sin(&Omega;), second component of inclination vector
     * @param l
     *        (M or E or v) + &omega; + &Omega;, mean, eccentric or true longitude argument (rad)
     * @param type
     *        type of longitude argument
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException
     *            if eccentricity is equal to 1 or larger
     */
    public AlternateEquinoctialParameters(final double nIn, final double exIn, final double eyIn,
        final double hxIn, final double hyIn,
        final double l, final PositionAngle type, final double mu) {
        super(mu);

        if (exIn * exIn + eyIn * eyIn >= 1.0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS, this.getClass().getName());
        }
        this.n = nIn;
        this.ex = exIn;
        this.ey = eyIn;
        this.hx = hxIn;
        this.hy = hyIn;

        switch (type) {
            case MEAN:
                this.lM = l;
                break;
            case ECCENTRIC:
                this.lM = this.eccentricToMean(l);
                break;
            case TRUE:
                this.lM = this.eccentricToMean(this.trueToEccentric(l));
                break;
            default:
                throw PatriusException.createInternalError(null);
        }

    }

    /**
     * Get the mean motion.
     * 
     * @return mean motion (1/s)
     */
    public double getN() {
        return this.n;
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
     * @return tan(i/2) cos(&Omega;), first component of the inclination vector
     */
    public double getHx() {
        return this.hx;
    }

    /**
     * Get the second component of the inclination vector.
     * 
     * @return tan(i/2) sin(&Omega;), second component of the inclination vector
     */
    public double getHy() {
        return this.hy;
    }

    /**
     * Get the longitude argument.
     * 
     * @param type
     *        type of the angle
     * @return longitude argument (rad)
     */
    public double getL(final PositionAngle type) {
        return (type == PositionAngle.MEAN) ? this.getLM() :
            ((type == PositionAngle.ECCENTRIC) ? this.getLE() :
                this.getLv());
    }

    /**
     * Get the mean longitude argument.
     * 
     * @return M + &omega; + &Omega; true longitude argument (rad)
     */
    public double getLM() {
        return this.lM;
    }

    /**
     * Get the true longitude argument.
     * 
     * @return v + &omega; + &Omega; true longitude argument (rad)
     */
    public double getLv() {
        final double lE = this.getLE();
        final double epsilon = MathLib.sqrt(MathLib.max(0.0, 1 - this.ex * this.ex - this.ey * this.ey));
        final double[] sincosLE = MathLib.sinAndCos(lE);
        final double sinLE = sincosLE[0];
        final double cosLE = sincosLE[1];
        final double num = this.ex * sinLE - this.ey * cosLE;
        final double den = epsilon + 1 - this.ex * cosLE - this.ey * sinLE;
        return lE + 2 * MathLib.atan(num / den);
    }

    /**
     * Get the eccentric longitude argument.
     * 
     * @return E + &omega; + &Omega; eccentric longitude argument (rad)
     */
    public double getLE() {
        // Generalization of Kepler equation to equinoctial parameters
        // with lE = PA + RAAN + E and
        // lM = PA + RAAN + M = lE - ex.sin(lE) + ey.cos(lE)
        double lE = this.lM;
        double shift = 0.0;
        double lEmlM = 0.0;
        double[] sincosLE = MathLib.sinAndCos(lE);
        double sinLE = sincosLE[0];
        double cosLE = sincosLE[1];
        int iter = 0;
        do {
            final double f2 = this.ex * sinLE - this.ey * cosLE;
            final double f1 = 1.0 - this.ex * cosLE - this.ey * sinLE;
            final double f0 = lEmlM - f2;

            final double f12 = 2.0 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            lEmlM -= shift;
            lE = this.lM + lEmlM;
            sincosLE = MathLib.sinAndCos(lE);
            sinLE = sincosLE[0];
            cosLE = sincosLE[1];

        } while ((++iter < MAX_ITERATIONS) && (MathLib.abs(shift) > THRESHOLD));

        // Return result
        return lE;

    }

    /**
     * Computes the eccentric longitude argument from the true longitude argument.
     * 
     * @param lv
     *        = v + &omega; + &Omega; true longitude argument (rad)
     * @return the eccentric longitude argument
     */
    private double trueToEccentric(final double lv) {
        final double epsilon = MathLib.sqrt(MathLib.max(0.0, 1 - this.ex * this.ex - this.ey * this.ey));
        final double[] sincosLv = MathLib.sinAndCos(lv);
        final double sinLv = sincosLv[0];
        final double cosLv = sincosLv[1];
        final double num = this.ey * cosLv - this.ex * sinLv;
        final double den = epsilon + 1 + this.ex * cosLv + this.ey * sinLv;
        return lv + 2 * MathLib.atan(num / den);
    }

    /**
     * Computes the mean longitude argument from the eccentric longitude argument.
     * 
     * @param lE
     *        = E + &omega; + &Omega; eccentric longitude argument (rad)
     * @return the mean longitude argument
     */
    private double eccentricToMean(final double lE) {
        final double[] sincosLe = MathLib.sinAndCos(lE);
        final double sinLe = sincosLe[0];
        final double cosLe = sincosLe[1];
        return lE - this.ex * sinLe + this.ey *cosLe;
    }

    /** {@inheritDoc} */
    @Override
    public CartesianParameters getCartesianParameters() {

        // get equinoctial parameters
        final double a = MathLib.cbrt(this.getMu() / (this.n * this.n));
        final double lE = this.getLE();

        // inclination-related intermediate parameters
        final double hx2 = this.hx * this.hx;
        final double hy2 = this.hy * this.hy;
        final double factH = 1. / (1 + hx2 + hy2);

        // reference axes defining the orbital plane
        final double ux = (1 + hx2 - hy2) * factH;
        final double uy = 2 * this.hx * this.hy * factH;
        final double uz = -2 * this.hy * factH;

        final double vx = uy;
        final double vy = (1 - hx2 + hy2) * factH;
        final double vz = 2 * this.hx * factH;

        // eccentricity-related intermediate parameters
        final double exey = this.ex * this.ey;
        final double ex2 = this.ex * this.ex;
        final double ey2 = this.ey * this.ey;
        final double e2 = ex2 + ey2;
        final double eta = 1 + MathLib.sqrt(MathLib.max(0.0, 1 - e2));
        final double beta = 1. / eta;

        // eccentric longitude argument
        final double[] sincosLe = MathLib.sinAndCos(lE);
        final double cLe = sincosLe[1];
        final double sLe = sincosLe[0];
        final double exCeyS = this.ex * cLe + this.ey * sLe;

        // coordinates of position and velocity in the orbital plane
        final double x = a * ((1 - beta * ey2) * cLe + beta * exey * sLe - this.ex);
        final double y = a * ((1 - beta * ex2) * sLe + beta * exey * cLe - this.ey);

        final double factor = MathLib.sqrt(this.getMu() / a) / (1 - exCeyS);
        final double xdot = factor * (-sLe + beta * this.ey * exCeyS);
        final double ydot = factor * (cLe - beta * this.ex * exCeyS);

        final Vector3D position = new Vector3D(x * ux + y * vx, x * uy + y * vy, x * uz + y * vz);
        final Vector3D velocity = new Vector3D(xdot * ux + ydot * vx, xdot * uy + ydot * vy, xdot
            * uz + ydot * vz);
        final double r2 = position.getNormSq();
        final Vector3D acceleration = new Vector3D(-this.getMu() / (r2 * MathLib.sqrt(r2)), position);

        // Return result
        return new CartesianParameters(position, velocity, acceleration, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public KeplerianParameters getKeplerianParameters() {
        final double a = MathLib.cbrt(this.getMu() / (this.n * this.n));
        final double e = MathLib.sqrt(this.ex * this.ex + this.ey * this.ey);
        final double i = 2. * MathLib.atan(MathLib.sqrt(this.hx * this.hx + this.hy * this.hy));
        final double raan = MathLib.atan2(this.hy, this.hx);
        final double pa = MathLib.atan2(this.ey, this.ex) - raan;
        final double lv = this.getLv();
        final double v = lv - pa - raan;
        return new KeplerianParameters(a, e, i, pa, raan, v, PositionAngle.TRUE, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public CircularParameters getCircularParameters() {
        // a
        final double a = MathLib.cbrt(this.getMu() / (this.n * this.n));
        // i
        final double i = 2 * MathLib.atan(MathLib.sqrt(this.hx * this.hx + this.hy * this.hy));
        // Raan
        final double raan = MathLib.atan2(this.hy, this.hx);
        final double[] sincosLe = MathLib.sinAndCos(raan);
        final double sinRaan = sincosLe[0];
        final double cosRaan = sincosLe[1];
        final double cirqex = this.ex * cosRaan + this.ey * sinRaan;
        final double cirqey = this.ey * cosRaan - this.ex * sinRaan;
        // Latitude argument
        final double lv = this.getLv();
        final double alphaV = lv - raan;
        return new CircularParameters(a, cirqex, cirqey, i, raan, alphaV, PositionAngle.TRUE,
            this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public EquatorialParameters getEquatorialParameters() {
        // a
        final double a = MathLib.cbrt(this.getMu() / (this.n * this.n));
        // Raan
        final double i = 2 * MathLib.atan(MathLib.sqrt(this.hx * this.hx + this.hy * this.hy));
        final double raan = MathLib.atan2(this.hy, this.hx);
        final double siniOver2 = 2. * MathLib.sin(i / 2.);
        final double[] sincosRaan = MathLib.sinAndCos(raan);
        final double sinRaan = sincosRaan[0];
        final double cosRaan = sincosRaan[1];
        // Ix/Iy
        final double ix = siniOver2 * cosRaan;
        final double iy = siniOver2 * sinRaan;
        // e
        final double e = MathLib.sqrt(this.ex * this.ex + this.ey * this.ey);
        // pOmega
        final double pomega = MathLib.atan2(this.ey, this.ex);
        return new EquatorialParameters(a, e, pomega, ix, iy, this.getLv() - pomega,
            PositionAngle.TRUE, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public AlternateEquinoctialParameters getAlternateEquinoctialParameters() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialParameters getEquinoctialParameters() {
        final double a = MathLib.cbrt(this.getMu() / (this.n * this.n));
        final double lv = this.getLv();
        return new EquinoctialParameters(a, this.ex, this.ey, this.hx, this.hy, lv, PositionAngle.TRUE, this.getMu());
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
        final double a = MathLib.cbrt(this.getMu() / (this.n * this.n));
        final double iOver2 = MathLib.atan(MathLib.sqrt(this.hx * this.hx + this.hy * this.hy));
        final double sinIOver2 = MathLib.sin(iOver2);
        final double raan = MathLib.atan2(this.hy, this.hx);
        final double[] sincosRaan = MathLib.sinAndCos(raan);
        final double sinRaan = sincosRaan[0];
        final double cosRaan = sincosRaan[1];
        final double ix = sinIOver2 * cosRaan;
        final double iy = sinIOver2 * sinRaan;
        return new StelaEquinoctialParameters(a, this.ex, this.ey, ix, iy, this.getLM(), this.getMu(), true);
    }

    /**
     * Returns a string representation of this orbit parameters object.
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return new StringBuffer().append("equinoctial parameters: ").append('{').
            append("n: ").append(this.n).
            append("; ex: ").append(this.ex).append("; ey: ").append(this.ey).
            append("; hx: ").append(this.hx).append("; hy: ").append(this.hy).
            append("; lM: ").append(MathLib.toDegrees(this.lM)).
            append(";}").toString();
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
        } else if (object instanceof AlternateEquinoctialParameters) {
            // cast object to check parameters
            final AlternateEquinoctialParameters other = (AlternateEquinoctialParameters) object;
            
            //isEqual &= (this.getDate().equals(other.getDate()));
            //isEqual &= (this.getFrame().equals(other.getFrame()));
            isEqual &= (this.getMu() == other.getMu());
            
        	// Alternate Equinoctial parameters
            isEqual &= (this.getN() == other.getN());
            isEqual &= (this.getEquinoctialEx() == other.getEquinoctialEx());
            isEqual &= (this.getEquinoctialEy() == other.getEquinoctialEy());
            isEqual &= (this.getHx() == other.getHx());
            isEqual &= (this.getHy() == other.getHy());
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
        
        //result = effMult * result + this.getDate().hashCode();
        //result = effMult * result + this.getFrame().hashCode();
        
        
        result = effMult * result + MathUtils.hash(this.getMu());
        result = effMult * result + MathUtils.hash(this.getN());
        result = effMult * result + MathUtils.hash(this.getEquinoctialEx());
        result = effMult * result + MathUtils.hash(this.getEquinoctialEy());
        result = effMult * result + MathUtils.hash(this.getHx());
        result = effMult * result + MathUtils.hash(this.getHy());
        result = effMult * result + MathUtils.hash(this.getLM());
 
        return result; 
    }
    
}