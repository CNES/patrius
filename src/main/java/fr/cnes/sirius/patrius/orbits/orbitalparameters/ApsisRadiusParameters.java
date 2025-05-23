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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:1798:10/12/2018 add getAlternateEquinoctialParameters
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.PositionAngle;

/**
 * This class handles periapsis/apoapsis parameters.
 * 
 * <p>
 * The parameters used internally are the apsis elements which can be related to keplerian elements as follows:
 * <ul>
 * <li>periapsis = a (1 - e)</li>
 * <li>apoapsis = a (1 + e)</li>
 * <li>i</li>
 * <li>&omega;</li>
 * <li>&Omega;</li>
 * <li>v</li>
 * </ul>
 * where &Omega; stands for the Right Ascension of the Ascending Node and v stands for true anomaly
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Emmanuel Bignon
 * @since 3.0
 * @version $Id: ApsisRadiusParameters.java 18071 2017-10-02 16:46:39Z bignon $
 */
public class ApsisRadiusParameters extends AbstractOrbitalParameters {

    /** Serializable UID. */
    private static final long serialVersionUID = -4701713076176839228L;
    
    /** Periapsis (m). */
    private final double periapsis;

    /** Apoapsis (m). */
    private final double apoapsis;

    /** Keplerian parameters. */
    private final KeplerianParameters kepParameters;
    
    /**
     * Creates a new instance.
     * 
     * @param periapsisIn
     *        periapsis distance (m)
     * @param apoapsisIn
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
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public ApsisRadiusParameters(final double periapsisIn, final double apoapsisIn, final double i,
        final double pa, final double raan, final double anomaly, final PositionAngle type,
        final double mu) {
        super(mu);
        this.periapsis = periapsisIn;
        this.apoapsis = apoapsisIn;
        final double sum = apoapsisIn + periapsisIn;
        final double a = sum / 2.;
        final double e = (apoapsisIn - periapsisIn) / sum;
        this.kepParameters = new KeplerianParameters(a, e, i, pa, raan, anomaly, type, mu);
    }

    /**
     * Get the periapsis.
     * 
     * @return periapsis (m)
     */
    public double getPeriapsis() {
        return this.periapsis;
    }

    /**
     * Get the apoapsis.
     * 
     * @return apoapsis (m)
     */
    public double getApoapsis() {
        return this.apoapsis;
    }

    /**
     * Get the inclination.
     * 
     * @return inclination (rad)
     */
    public double getI() {
        return this.kepParameters.getI();
    }

    /**
     * Get the perigee argument.
     * 
     * @return perigee argument (rad)
     */
    public double getPerigeeArgument() {
        return this.kepParameters.getPerigeeArgument();
    }

    /**
     * Get the right ascension of the ascending node.
     * 
     * @return right ascension of the ascending node (rad)
     */
    public double getRightAscensionOfAscendingNode() {
        return this.kepParameters.getRightAscensionOfAscendingNode();
    }

    /**
     * Get the anomaly.
     * 
     * @param type
     *        type of the angle
     * @return anomaly (rad)
     */
    public double getAnomaly(final PositionAngle type) {
        return this.kepParameters.getAnomaly(type);
    }

    /** {@inheritDoc} */
    @Override
    public CartesianParameters getCartesianParameters() {
        return this.kepParameters.getCartesianParameters();
    }

    /** {@inheritDoc} */
    @Override
    public KeplerianParameters getKeplerianParameters() {
        return this.kepParameters;
    }

    /** {@inheritDoc} */
    @Override
    public CircularParameters getCircularParameters() {
        return this.kepParameters.getCircularParameters();
    }

    /** {@inheritDoc} */
    @Override
    public EquatorialParameters getEquatorialParameters() {
        return this.kepParameters.getEquatorialParameters();
    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialParameters getEquinoctialParameters() {
        return this.kepParameters.getEquinoctialParameters();
    }

    /** {@inheritDoc} */
    @Override
    public ApsisAltitudeParameters getApsisAltitudeParameters(final double ae) {
        return new ApsisAltitudeParameters(this.periapsis - ae, this.apoapsis - ae, this.kepParameters.getI(),
            this.kepParameters.getPerigeeArgument(),
            this.kepParameters.getRightAscensionOfAscendingNode(), this.kepParameters.getTrueAnomaly(),
            PositionAngle.TRUE, this.getMu(), ae);
    }

    /** {@inheritDoc} */
    @Override
    public ApsisRadiusParameters getApsisRadiusParameters() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ReentryParameters getReentryParameters(final double ae, final double f) {
        return this.kepParameters.getReentryParameters(ae, f);
    }

    /** {@inheritDoc} */
    @Override
    public StelaEquinoctialParameters getStelaEquinoctialParameters() {
        return this.kepParameters.getStelaEquinoctialParameters();
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
        return new StringBuffer().append("apsis parameters: ").append('{').append("periapsis: ")
            .append(this.periapsis).append(", apoapsis: ").append(this.apoapsis).append(", i: ")
            .append(MathLib.toDegrees(this.getI())).append(", pa: ")
            .append(MathLib.toDegrees(this.getPerigeeArgument())).append(", raan: ")
            .append(MathLib.toDegrees(this.getRightAscensionOfAscendingNode())).append(", v: ")
            .append(MathLib.toDegrees(this.getAnomaly(PositionAngle.TRUE))).append(";}").toString();
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
        } else if (object instanceof ApsisRadiusParameters) {
            final ApsisRadiusParameters other = (ApsisRadiusParameters) object;
            isEqual = kepParameters.equals(other.kepParameters);
        } else {
            isEqual = false;
        }
		
        return isEqual;
    }
    
    /** {@inheritDoc} */
    @Override
	public int hashCode() {
        return kepParameters.hashCode();
    }		
}