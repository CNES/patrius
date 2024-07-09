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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:1798:10/12/2018:Add getAlternateEquinoctialParameters
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

/**
 * Interface for orbital parameters.
 * 
 * @author Emmanuel Bignon
 * @since 3.0
 * @version $Id: IOrbitalParameters.java 18071 2017-10-02 16:46:39Z bignon $
 */
public interface IOrbitalParameters {

    /**
     * Getter for the central acceleration constant.
     * 
     * @return central acceleration constant
     */
    double getMu();

    /**
     * Convert current orbital parameters into cartesian parameters.
     * 
     * @return current orbital parameters converted into cartesian parameters
     * @see CartesianParameters
     */
    CartesianParameters getCartesianParameters();

    /**
     * Convert current orbital parameters into Keplerian parameters.
     * 
     * @return current orbital parameters converted into Keplerian parameters
     * @see KeplerianParameters
     */
    KeplerianParameters getKeplerianParameters();

    /**
     * Convert current orbital parameters into circular parameters.
     * 
     * @return current orbital parameters converted into circular parameters
     * @see CircularParameters
     */
    CircularParameters getCircularParameters();

    /**
     * Convert current orbital parameters into equatorial parameters.
     * 
     * @return current orbital parameters converted into equatorial parameters
     * @see EquatorialParameters
     */
    EquatorialParameters getEquatorialParameters();

    /**
     * Convert current orbital parameters into equinoctial parameters.
     * 
     * @return current orbital parameters converted into equinoctial parameters
     * @see EquinoctialParameters
     */
    EquinoctialParameters getEquinoctialParameters();

    /**
     * Convert current orbital parameters into alternate equinoctial parameters.
     * 
     * @return current orbital parameters converted into alternate equinoctial parameters
     * @see AlternateEquinoctialParameters
     */
    AlternateEquinoctialParameters getAlternateEquinoctialParameters();

    /**
     * Convert current orbital parameters into apsis (using altitude) parameters.
     * 
     * @param ae
     *        equatorial radius (m)
     * @return current orbital parameters converted into apsis (using altitude) parameters
     * @see ApsisAltitudeParameters
     */
    ApsisAltitudeParameters getApsisAltitudeParameters(final double ae);

    /**
     * Convert current orbital parameters into apsis (using radius) parameters.
     * 
     * @return current orbital parameters converted into apsis (using radius) parameters
     * @see ApsisRadiusParameters
     */
    ApsisRadiusParameters getApsisRadiusParameters();

    /**
     * Convert current orbital parameters into reentry parameters.
     * 
     * @param ae
     *        equatorial radius (m)
     * @param f
     *        flattening (f = (a-b)/a)
     * @return current orbital parameters converted into reentry parameters
     * @see ReentryParameters
     */
    ReentryParameters getReentryParameters(final double ae, final double f);

    /**
     * Convert current orbital parameters into Stela equinoctial parameters.
     * 
     * @return current orbital parameters converted into Stela equinoctial parameters
     * @see StelaEquinoctialParameters
     */
    StelaEquinoctialParameters getStelaEquinoctialParameters();
    
    /**
     * Test for the equality of two orbits.
     * <p>
     * Orbits are considered equals if they have the same type and all their attributes are equals.
     * In particular, the orbits frame are considered equals if they represent the same instance.
     * If they have the same attributes but are not the same instance, the method will return false. 
     * </p>
     * 
     * @param object
     *        Object to test for equality to this
     * @return true if two orbits are equal
     */
    public abstract boolean equals(final Object object); 
    
    /**
     * Get a hashCode for the orbit.
     *
     * @return a hash code value for this object
     */
    public abstract int hashCode();

    // CHECKSTYLE: resume ModifiedControlVariable check
}
