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
 * @history creation 16/03/2015
 *
 * HISTORY
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
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class handles circular orbital parameters.
 * 
 * <p>
 * The parameters used internally are the circular elements which can be related to keplerian elements as follows:
 * <ul>
 * <li>a</li>
 * <li>e<sub>x</sub> = e cos(&omega;)</li>
 * <li>e<sub>y</sub> = e sin(&omega;)</li>
 * <li>i</li>
 * <li>&Omega;</li>
 * <li>&alpha;<sub>v</sub> = v + &omega;</li>
 * </ul>
 * where &Omega; stands for the Right Ascension of the Ascending Node and &alpha;<sub>v</sub> stands for the true
 * latitude argument
 * </p>
 * <p>
 * The conversion equations from and to keplerian elements given above hold only when both sides are unambiguously
 * defined, i.e. when orbit is neither equatorial nor circular. When orbit is circular (but not equatorial), the
 * circular parameters are still unambiguously defined whereas some keplerian elements (more precisely &omega; and
 * &Omega;) become ambiguous. When orbit is equatorial, neither the keplerian nor the circular parameters can be defined
 * unambiguously. {@link EquinoctialParameters equinoctial parameters} is the recommended way to represent orbits.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Emmanuel Bignon
 * @since 3.0
 * @version $Id: CircularParameters.java 18071 2017-10-02 16:46:39Z bignon $
 */
public class CircularParameters extends AbstractOrbitalParameters {

    /** Serializable UID. */
    private static final long serialVersionUID = -5285991585368439144L;

    /** Maximum number of iterations for convergence algorithms. */
    private static final double MAX_ITERATIONS = 50;

    /** Threshold for convergence algorithms. */
    private static final double THRESHOLD = 1.0e-12;
    
    /** Root int for hash code. */
    private static final int ROOTINT = 354;

    /** Semi-major axis (m). */
    private final double a;

    /** First component of the circular eccentricity vector. */
    private final double ex;

    /** Second component of the circular eccentricity vector. */
    private final double ey;

    /** Inclination (rad). */
    private final double i;

    /** Right Ascension of Ascending Node (rad). */
    private final double raan;

    /** True latitude argument (rad). */
    private final double alphaV;
    
    /**
     * Creates a new instance.
     * 
     * @param aIn
     *        semi-major axis (m)
     * @param exIn
     *        e cos(&omega;), first component of circular eccentricity vector
     * @param eyIn
     *        e sin(&omega;), second component of circular eccentricity vector
     * @param iIn
     *        inclination (rad)
     * @param raanIn
     *        right ascension of ascending node (&Omega;, rad)
     * @param alpha
     *        an + &omega;, mean, eccentric or true latitude argument (rad)
     * @param type
     *        type of latitude argument
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException
     *            if eccentricity is equal to 1 or larger
     */
    public CircularParameters(final double aIn, final double exIn, final double eyIn,
        final double iIn, final double raanIn, final double alpha, final PositionAngle type,
        final double mu) {
        super(mu);

        if (exIn * exIn + eyIn * eyIn >= 1.0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS, this.getClass().getName());
        }
        this.a = aIn;
        this.ex = exIn;
        this.ey = eyIn;
        this.i = iIn;
        this.raan = raanIn;

        switch (type) {
            case MEAN:
                this.alphaV = this.eccentricToTrue(this.meanToEccentric(alpha));
                break;
            case ECCENTRIC:
                this.alphaV = this.eccentricToTrue(alpha);
                break;
            case TRUE:
                this.alphaV = alpha;
                break;
            default:
                throw PatriusException.createInternalError(null);
        }
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
     * Get the first component of the circular eccentricity vector.
     * 
     * @return ex = e cos(&omega;), first component of the circular eccentricity vector
     */
    public double getCircularEx() {
        return this.ex;
    }

    /**
     * Get the second component of the circular eccentricity vector.
     * 
     * @return ey = e sin(&omega;), second component of the circular eccentricity vector
     */
    public double getCircularEy() {
        return this.ey;
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
     * Get the right ascension of the ascending node.
     * 
     * @return right ascension of the ascending node (rad)
     */
    public double getRightAscensionOfAscendingNode() {
        return this.raan;
    }

    /**
     * Get the latitude argument.
     * 
     * @param type
     *        type of the angle
     * @return latitude argument (rad)
     */
    public double getAlpha(final PositionAngle type) {
        return (type == PositionAngle.MEAN) ? this.getAlphaM() : ((type == PositionAngle.ECCENTRIC)
            ? this.getAlphaE() : this.getAlphaV());
    }

    /**
     * Get the true latitude argument.
     * 
     * @return v + &omega; true latitude argument (rad)
     */
    public double getAlphaV() {
        return this.alphaV;
    }

    /**
     * Get the eccentric latitude argument.
     * 
     * @return E + &omega; eccentric latitude argument (rad)
     */
    public double getAlphaE() {
        final double epsilon = MathLib.sqrt(MathLib.max(0.0, 1 - this.ex * this.ex - this.ey * this.ey));
        final double[] sincos = MathLib.sinAndCos(alphaV);
        final double sinAlphaV = sincos[0];
        final double cosAlphaV = sincos[1];
        return this.alphaV
            + 2
            * MathLib.atan((this.ey * cosAlphaV - this.ex * sinAlphaV)
                / (epsilon + 1 + this.ex * cosAlphaV + this.ey * sinAlphaV));
    }

    /**
     * Get the mean latitude argument.
     * 
     * @return M + &omega; mean latitude argument (rad)
     */
    public double getAlphaM() {
        final double alphaE = this.getAlphaE();
        final double[] sincos = MathLib.sinAndCos(alphaE);
        final double sinAlphaE = sincos[0];
        final double cosAlphaE = sincos[1];
        return alphaE - this.ex * sinAlphaE + this.ey * cosAlphaE;
    }

    /**
     * Computes the true latitude argument from the eccentric latitude argument.
     * 
     * @param alphaE
     *        = E + &omega; eccentric latitude argument (rad)
     * @return the true latitude argument.
     */
    private double eccentricToTrue(final double alphaE) {
        final double epsilon = MathLib.sqrt(MathLib.max(0.0, 1 - this.ex * this.ex - this.ey * this.ey));
        final double[] sincos = MathLib.sinAndCos(alphaE);
        final double sinAlphaE = sincos[0];
        final double cosAlphaE = sincos[1];
        return alphaE
            + 2
            * MathLib.atan((this.ex * sinAlphaE - this.ey * cosAlphaE)
                / (epsilon + 1 - this.ex * cosAlphaE - this.ey * sinAlphaE));
    }

    /**
     * Computes the eccentric latitude argument from the mean latitude argument.
     * 
     * @param alphaM
     *        = M + &omega; mean latitude argument (rad)
     * @return the eccentric latitude argument.
     */
    private double meanToEccentric(final double alphaM) {
        // Generalization of Kepler equation to circular parameters
        // with alphaE = PA + E and
        // alphaM = PA + M = alphaE - ex.sin(alphaE) + ey.cos(alphaE)
        double alphaE = alphaM;
        double shift = 0.0;
        double alphaEMalphaM = 0.0;
        double[] sincos = MathLib.sinAndCos(alphaE);
        double sinAlphaE = sincos[0];
        double cosAlphaE = sincos[1];
        // Loop until convergence
        int iter = 0;
        do {
            final double f2 = this.ex * sinAlphaE - this.ey * cosAlphaE;
            final double f1 = 1.0 - this.ex * cosAlphaE - this.ey * sinAlphaE;
            final double f0 = alphaEMalphaM - f2;

            final double f12 = 2.0 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            alphaEMalphaM -= shift;
            alphaE = alphaM + alphaEMalphaM;
            sincos = MathLib.sinAndCos(alphaE);
            sinAlphaE = sincos[0];
            cosAlphaE = sincos[1];

        } while ((++iter < MAX_ITERATIONS) && (MathLib.abs(shift) > THRESHOLD));

        // Return result
        return alphaE;

    }

    /** {@inheritDoc} */
    @Override
    public CartesianParameters getCartesianParameters() {
        // Raan
        final double[] sincos = MathLib.sinAndCos(raan);
        final double sinRaan = sincos[0];
        final double cosRaan = sincos[1];

        // get equinoctial parameters
        final double equEx = this.ex * cosRaan - this.ey * sinRaan;
        final double equEy = this.ey * cosRaan + this.ex * sinRaan;
        final double hx = cosRaan * MathLib.tan(this.i / 2);
        final double hy = sinRaan * MathLib.tan(this.i / 2);
        final double lE = this.getAlphaE() + this.raan;

        // inclination-related intermediate parameters
        final double hx2 = hx * hx;
        final double hy2 = hy * hy;
        final double factH = 1. / (1 + hx2 + hy2);

        // reference axes defining the orbital plane
        final double ux = (1 + hx2 - hy2) * factH;
        final double uy = 2 * hx * hy * factH;
        final double uz = -2 * hy * factH;

        final double vx = uy;
        final double vy = (1 - hx2 + hy2) * factH;
        final double vz = 2 * hx * factH;

        // eccentricity-related intermediate parameters
        final double exey = equEx * equEy;
        final double ex2 = equEx * equEx;
        final double ey2 = equEy * equEy;
        final double e2 = ex2 + ey2;
        final double eta = 1 + MathLib.sqrt(MathLib.max(0.0, 1 - e2));
        final double beta = 1. / eta;

        // eccentric latitude argument
        final double[] sincosLE = MathLib.sinAndCos(lE);
        final double sLe = sincosLE[0];
        final double cLe = sincosLE[1];
        final double exCeyS = equEx * cLe + equEy * sLe;

        // coordinates of position, velocity and acceleration in the orbital plane
        final double x = this.a * ((1 - beta * ey2) * cLe + beta * exey * sLe - equEx);
        final double y = this.a * ((1 - beta * ex2) * sLe + beta * exey * cLe - equEy);

        final double factor = MathLib.sqrt(this.getMu() / this.a) / (1 - exCeyS);
        final double xdot = factor * (-sLe + beta * equEy * exCeyS);
        final double ydot = factor * (cLe - beta * equEx * exCeyS);

        // Pos/Vel/Acc
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
        final double pa = MathLib.atan2(this.ey, this.ex);
        final double e = this.ex / MathLib.cos(pa);
        return new KeplerianParameters(this.a, e, this.i, pa, this.raan, this.alphaV - pa, PositionAngle.TRUE,
            this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public CircularParameters getCircularParameters() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public EquatorialParameters getEquatorialParameters() {
        final double e = MathLib.sqrt(this.ex * this.ex + this.ey * this.ey);
        final double ix = 2. * MathLib.sin(this.i / 2.) * MathLib.cos(this.raan);
        final double iy = 2. * MathLib.sin(this.i / 2.) * MathLib.sin(this.raan);
        final double pa = MathLib.atan2(this.ey, this.ex);
        final double pomega = pa + this.raan;
        final double anomaly = this.raan + this.alphaV - pomega;
        return new EquatorialParameters(this.a, e, pomega, ix, iy, anomaly, PositionAngle.TRUE, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialParameters getEquinoctialParameters() {
        final double[] sincos = MathLib.sinAndCos(raan);
        final double sinRaan = sincos[0];
        final double cosRaan = sincos[1];
        final double exequi = this.ex * cosRaan - this.ey * sinRaan;
        final double eyequi = this.ey * cosRaan + this.ex * sinRaan;
        final double hx = cosRaan * MathLib.tan(this.i / 2);
        final double hy = sinRaan * MathLib.tan(this.i / 2);
        final double lv = this.alphaV + this.raan;
        return new EquinoctialParameters(this.a, exequi, eyequi, hx, hy, lv, PositionAngle.TRUE, this.getMu());
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
        final double[] sincos = MathLib.sinAndCos(raan);
        final double sinRaan = sincos[0];
        final double cosRaan = sincos[1];
        final double exequi = this.ex * cosRaan - this.ey * sinRaan;
        final double eyequi = this.ey * cosRaan + this.ex * sinRaan;
        final double ix = cosRaan * MathLib.sin(this.i / 2);
        final double iy = sinRaan * MathLib.sin(this.i / 2);
        final double lM = this.getAlphaM() + this.raan;
        return new StelaEquinoctialParameters(this.a, exequi, eyequi, ix, iy, lM, this.getMu(), true);
    }

    /** {@inheritDoc} */
    @Override
    public AlternateEquinoctialParameters getAlternateEquinoctialParameters() {
        return this.getEquinoctialParameters().getAlternateEquinoctialParameters();
    }

    /**
     * Returns a string representation of this Orbit object.
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return new StringBuffer().append("circular parameters: ").append('{').append("a: ")
            .append(this.a).append(", ex: ").append(this.ex).append(", ey: ").append(this.ey).append(", i: ")
            .append(MathLib.toDegrees(this.i)).append(", raan: ").append(MathLib.toDegrees(this.raan))
            .append(", alphaV: ").append(MathLib.toDegrees(this.alphaV)).append(";}").toString();
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
        } else if (object instanceof CircularParameters) {
            // cast object to compare parameters
            final CircularParameters other = (CircularParameters) object;
        	
            isEqual &= (this.getMu() == other.getMu());
        	
        	// Circular parameters
            isEqual &= (this.getA() == other.getA());
            isEqual &= (this.getCircularEx() == other.getCircularEx());
            isEqual &= (this.getCircularEy() == other.getCircularEy());
            isEqual &= (this.getI() == other.getI());
            isEqual &= (this.getRightAscensionOfAscendingNode() 
        			== other.getRightAscensionOfAscendingNode());
            isEqual &= (this.getAlphaV() == other.getAlphaV());
        	
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
        result = effMult * result + MathUtils.hash(this.getCircularEx());
        result = effMult * result + MathUtils.hash(this.getCircularEy());
        result = effMult * result + MathUtils.hash(this.getI());
        result = effMult * result + MathUtils.hash(this.getRightAscensionOfAscendingNode());
        result = effMult * result + MathUtils.hash(this.getAlphaV());
 
        return result; 
    }
}