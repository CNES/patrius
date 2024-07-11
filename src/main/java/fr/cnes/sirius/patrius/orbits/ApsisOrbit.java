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
 * @history created 09/10/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS/COLOSUS] Mise en conformite code avec nouveau standard codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:426:30/10/2015: allow orbit instantiation in non inertial frame
 * VERSION::FA:1281:30/08/2017: Javadoc correction
 * VERSION::DM:1798:10/12/2018: Add getN() after AlternateEquinoctialParameters creation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits;

import java.util.Collection;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.ApsisRadiusParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.IOrbitalParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class handles periapsis/apoapsis parameters.
 * <p>
 * The parameters used internally are the periapsis/apoapsis elements (see {@link ApsisRadiusParameters} for more
 * information.
 * </p>
 * <p>
 * The instance <code>ApsisOrbit</code> is guaranteed to be immutable.
 * </p>
 * 
 * @see Orbit
 * @see KeplerianOrbit
 * @see CartesianOrbit
 * @see EquinoctialOrbit
 * @concurrency immutable
 * @author Denis Claude
 * @version $Id: ApsisOrbit.java 18082 2017-10-02 16:54:17Z bignon $
 * @since 1.3
 */

public final class ApsisOrbit extends Orbit {

    /** Serializable UID. */
    private static final long serialVersionUID = 5565190329070785158L;
    
    /** Root int for hash code. */
    private static final int ROOTINT = 353;

    /** Orbital parameters. */
    private final ApsisRadiusParameters parameters;

    /**
     * Creates a new instance.
     * 
     * @param parametersIn
     *        orbital parameters
     * @param frame
     *        the frame in which the parameters are defined
     * @param date
     *        date of the orbital parameters
     */
    public ApsisOrbit(final IOrbitalParameters parametersIn, final Frame frame,
        final AbsoluteDate date) {
        super(frame, date, parametersIn.getMu());
        this.parameters = parametersIn.getApsisRadiusParameters();
    }

    /**
     * Creates a new instance.
     * 
     * @param peri
     *        periapsis distance (m)
     * @param apo
     *        apoapsis distance (m)
     * @param i
     *        inclination (rad)
     * @param pa
     *        perigee argument (&omega;, rad)
     * @param raan
     *        right ascension of ascending node (&Omega;, rad)
     * @param anomaly
     *        mean, eccentric or true anomaly (rad).
     * @param type
     *        type of anomaly
     * @param frame
     *        the frame in which are defined the parameters
     * @param date
     *        date of the orbital parameters
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public ApsisOrbit(final double peri, final double apo, final double i, final double pa,
        final double raan, final double anomaly, final PositionAngle type, final Frame frame,
        final AbsoluteDate date, final double mu) {
        super(frame, date, mu);
        this.parameters = new ApsisRadiusParameters(peri, apo, i, pa, raan, anomaly, type, mu);
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
    public ApsisOrbit(final PVCoordinates pvCoordinates, final Frame frame,
        final AbsoluteDate date, final double mu) {
        super(pvCoordinates, frame, date, mu);
        this.parameters = new CartesianParameters(pvCoordinates, mu).getApsisRadiusParameters();
    }

    /**
     * Constructor from any kind of orbital parameters.
     * 
     * @param op
     *        orbital parameters to copy
     */
    public ApsisOrbit(final Orbit op) {
        super(op.getPVCoordinates(), op.getFrame(), op.getDate(), op.getMu());
        this.parameters = op.getParameters().getApsisRadiusParameters();
    }

    /** {@inheritDoc} */
    @Override
    public IOrbitalParameters getParameters() {
        return this.parameters;
    }

    /**
     * Getter for underlying apsis parameters.
     * 
     * @return apsis parameters
     */
    public ApsisRadiusParameters getApsisParameters() {
        return this.parameters;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getType() {
        return OrbitType.APSIS;
    }

    /**
     * Get the periapsis.
     * 
     * @return periapsis (m)
     */
    public double getPeriapsis() {
        return this.parameters.getPeriapsis();
    }

    /**
     * Get the apoapsis.
     * 
     * @return apoapsis (m)
     */
    public double getApoapsis() {
        return this.parameters.getApoapsis();
    }

    /** {@inheritDoc} */
    @Override
    public double getA() {
        return this.parameters.getKeplerianParameters().getA();
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
     * Get the anomaly.
     * 
     * @param type
     *        type of the angle
     * @return anomaly (rad)
     */
    public double getAnomaly(final PositionAngle type) {
        return this.parameters.getAnomaly(type);
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
     * Get the perigee argument.
     * 
     * @return perigee argument (rad)
     */
    public double getPerigeeArgument() {
        return this.parameters.getPerigeeArgument();
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
     * {@inheritDoc}
     * 
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
    protected ApsisOrbit orbitShiftedBy(final double dt) {
        return new ApsisOrbit(this.parameters.getPeriapsis(), this.parameters.getApoapsis(),
            this.parameters.getI(), this.parameters.getPerigeeArgument(),
            this.parameters.getRightAscensionOfAscendingNode(), this.getAnomaly(PositionAngle.MEAN)
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
    public ApsisOrbit interpolate(final AbsoluteDate date, final Collection<Orbit> sample) {

        final KeplerianOrbit kepOrbit = new KeplerianOrbit(this.parameters.getKeplerianParameters(),
            this.getFrame(), this.getDate());
        final KeplerianOrbit interpolated = kepOrbit.interpolate(date, sample);

        // build a new interpolated instance
        return new ApsisOrbit(interpolated.getPVCoordinates(), this.getFrame(), date, this.getMu());
    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianMeanWrtCartesian() {
        // Get Keplerian parameters and Jacobian
        final KeplerianOrbit kepOrbit = new KeplerianOrbit(this.parameters.getKeplerianParameters(),
            this.getFrame(), this.getDate());
        final double[][] jacobian = kepOrbit.computeJacobianMeanWrtCartesian();

        // Update Jacobian for Periapsis and Apoapsis

        // dPeriapsis
        final Vector3D vectorA = new Vector3D(jacobian[0][0], jacobian[0][1], jacobian[0][2]);
        final Vector3D vectorE = new Vector3D(jacobian[1][0], jacobian[1][1], jacobian[1][2]);
        fillHalfRow(1. - this.getE(), vectorA, -this.getA(), vectorE, jacobian[0], 0);
        final Vector3D vectorVA = new Vector3D(jacobian[0][3], jacobian[0][4], jacobian[0][5]);
        final Vector3D vectorVE = new Vector3D(jacobian[1][3], jacobian[1][4], jacobian[1][5]);
        fillHalfRow(1. - this.getE(), vectorVA, -this.getA(), vectorVE, jacobian[0], 3);

        // dApoapsis
        fillHalfRow(1. + this.getE(), vectorA, this.getA(), vectorE, jacobian[1], 0);
        fillHalfRow(1. + this.getE(), vectorVA, this.getA(), vectorVE, jacobian[1], 3);

        // Return result
        return jacobian;
    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianEccentricWrtCartesian() {
        // Get Keplerian parameters and Jacobian
        final KeplerianOrbit kepOrbit = new KeplerianOrbit(this.parameters.getKeplerianParameters(),
            this.getFrame(), this.getDate());
        final double[][] jacobian = kepOrbit.computeJacobianEccentricWrtCartesian();

        // dPeriapsis
        final Vector3D vectorA = new Vector3D(jacobian[0][0], jacobian[0][1], jacobian[0][2]);
        final Vector3D vectorE = new Vector3D(jacobian[1][0], jacobian[1][1], jacobian[1][2]);
        fillHalfRow(1. - this.getE(), vectorA, -this.getA(), vectorE, jacobian[0], 0);
        final Vector3D vectorVA = new Vector3D(jacobian[0][3], jacobian[0][4], jacobian[0][5]);
        final Vector3D vectorVE = new Vector3D(jacobian[1][3], jacobian[1][4], jacobian[1][5]);
        fillHalfRow(1. - this.getE(), vectorVA, -this.getA(), vectorVE, jacobian[0], 3);

        // dApoapsis
        fillHalfRow(1. + this.getE(), vectorA, this.getA(), vectorE, jacobian[1], 0);
        fillHalfRow(1. + this.getE(), vectorVA, this.getA(), vectorVE, jacobian[1], 3);

        // Return result
        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianTrueWrtCartesian() {
        // Get Keplerian parameters and Jacobian
        final KeplerianOrbit kepOrbit = new KeplerianOrbit(this.parameters.getKeplerianParameters(),
            this.getFrame(), this.getDate());
        final double[][] jacobian = kepOrbit.computeJacobianTrueWrtCartesian();

        // dPeriapsis
        final Vector3D vectorA = new Vector3D(jacobian[0][0], jacobian[0][1], jacobian[0][2]);
        final Vector3D vectorE = new Vector3D(jacobian[1][0], jacobian[1][1], jacobian[1][2]);
        fillHalfRow(1. - this.getE(), vectorA, -this.getA(), vectorE, jacobian[0], 0);
        final Vector3D vectorVA = new Vector3D(jacobian[0][3], jacobian[0][4], jacobian[0][5]);
        final Vector3D vectorVE = new Vector3D(jacobian[1][3], jacobian[1][4], jacobian[1][5]);
        fillHalfRow(1. - this.getE(), vectorVA, -this.getA(), vectorVE, jacobian[0], 3);

        // dApoapsis
        fillHalfRow(1. + this.getE(), vectorA, this.getA(), vectorE, jacobian[1], 0);
        fillHalfRow(1. + this.getE(), vectorVA, this.getA(), vectorVE, jacobian[1], 3);

        // Return result
        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected void orbitAddKeplerContribution(final PositionAngle type, final double gm,
                                              final double[] pDot) {
        final KeplerianOrbit kepOrbit = new KeplerianOrbit(this.parameters.getKeplerianParameters(),
            this.getFrame(), this.getDate());
        kepOrbit.addKeplerContribution(type, gm, pDot);
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
		//              periapsis, apoapsis, i, Right Ascension of the Ascending Node,
		//              perigee argument, anomaly
        boolean isEqual = true;
		
        if (object == this) { 
            isEqual = true; 
        } else if (object instanceof ApsisOrbit) {
            final ApsisOrbit other = (ApsisOrbit) object;
        	
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
