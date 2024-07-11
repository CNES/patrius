/**
 *
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
 * @history 23/01/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:18/07/2013:Created Wrench object
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.wrenches;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Screw;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents a wrench.
 * 
 * @concurrency immutable
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: Wrench.java 18067 2017-10-02 16:44:20Z bignon $
 * 
 * @since 1.3
 * 
 */
public class Wrench {
    
    /** Zero wrench. */
    public static final Wrench ZERO = new Wrench(Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO);
    
    /** 7 */
    private static final int SEVEN = 7;
    /** 8 */
    private static final int EIGHT = 8;
    /** 9 */
    private static final int NINE = 9;

    /** Screw. */
    private final Screw screw;

    /**
     * Create a wrench with given force and torque.
     * 
     * @param origin
     *        origin
     * @param force
     *        force
     * @param torque
     *        torque
     */
    public Wrench(final Vector3D origin, final Vector3D force, final Vector3D torque) {
        this.screw = new Screw(origin, force, torque);
    }

    /**
     * Build a wrench from a screw.
     * 
     * @param inScrew
     *        screw to convert to a wrench
     */
    private Wrench(final Screw inScrew) {
        this.screw = new Screw(inScrew);
    }

    /**
     * <p>
     * Constructor from an array.
     * </p>
     * 
     * <pre>
     * data =
     * {o<sub>x</sub>, o<sub>y</sub>, o<sub>z</sub>,
     * f<sub>x</sub>, f<sub>y</sub>, f<sub>z</sub>,
     * t<sub>x</sub>, t<sub>y</sub>, t<sub>z</sub>}
     * </pre>
     * 
     * where o is the origin, f the force and t the torque.
     * 
     * @param data
     *        array with origin, force and torque
     * @throws PatriusException
     *         if input data array has an incorrect length
     */
    public Wrench(final double[] data) throws PatriusException {
        if (data.length != NINE) {
            throw new PatriusException(PatriusMessages.INVALID_ARRAY_LENGTH, NINE, data.length);
        }
        this.screw = new Screw(new Vector3D(data[0], data[1], data[2]), new Vector3D(data[3], data[4], data[5]),
            new Vector3D(data[6], data[SEVEN], data[EIGHT]));
    }

    /** @return the origin of the torque */
    public Vector3D getOrigin() {
        return this.screw.getOrigin();
    }

    /** @return the force */
    public Vector3D getForce() {
        return this.screw.getTranslation();
    }

    /** @return the torque */
    public Vector3D getTorque() {
        return this.screw.getRotation();
    }

    /**
     * Get the torque expressed in another point.
     * 
     * @param origin
     *        new origin for torque expression
     * @return the torque expressed in another point
     */
    public Vector3D getTorque(final Vector3D origin) {
        return this.screw.displace(origin).getRotation();
    }

    /**
     * Sum of two wrenches.
     * 
     * @param wrench
     *        wrench to add
     * @return sum of wrenches
     */
    public Wrench add(final Wrench wrench) {
        return sum(this, wrench);
    }

    /**
     * Sum of two wrenches.
     * 
     * @param wrench1
     *        first wrench
     * @param wrench2
     *        second wrench
     * @return sum of wrenches
     */
    public static Wrench sum(final Wrench wrench1, final Wrench wrench2) {
        return new Wrench(Screw.sum(wrench1.getScrew(), wrench2.getScrew()));
    }

    /**
     * Displace current wrench.
     * 
     * @param newOrigin
     *        new origin
     * @return displaced wrench
     */
    public Wrench displace(final Vector3D newOrigin) {
        return displace(this, newOrigin);
    }

    /**
     * Displace current wrench.
     * 
     * @param wrench
     *        the wrench to displace
     * @param newOrigin
     *        new origin
     * @return displaced wrench
     */
    public static Wrench displace(final Wrench wrench, final Vector3D newOrigin) {
        return new Wrench(Screw.displace(wrench.getScrew(), newOrigin));
    }

    /**
     * Get the wrapped screw.
     * 
     * @return the screw
     */
    private Screw getScrew() {
        return new Screw(this.screw);
    }

    /**
     * Get a double[] representation of this wrench.
     * 
     * <pre>
     * data = wrench.getWrench();
     * data =
     * {o<sub>x</sub>, o<sub>y</sub>, o<sub>z</sub>,
     * f<sub>x</sub>, f<sub>y</sub>, f<sub>z</sub>,
     * t<sub>x</sub>, t<sub>y</sub>, t<sub>z</sub>}
     * </pre>
     * 
     * where o is the origin, f the force and t the torque.
     * 
     * @return wrench as a double[].
     */
    public double[] getWrench() {
        return new double[] { this.screw.getOrigin().getX(), this.screw.getOrigin().getY(),
            this.screw.getOrigin().getZ(),
            this.screw.getTranslation().getX(), this.screw.getTranslation().getY(), this.screw.getTranslation().getZ(),
            this.screw.getRotation().getX(), this.screw.getRotation().getY(), this.screw.getRotation().getZ() };
    }

    /**
     * Get a String representation for this Wrench.
     * 
     * @return a representation for this wrench
     */
    @Override
    public String toString() {
        // Initialization
        final StringBuilder res = new StringBuilder();
        // Class name
        final String fullClassName = this.getClass().getName();
        final String shortClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        // Constants
        final String open = "{";
        final String close = "}";
        final String comma = ",";
        // Build string
        res.append(shortClassName).append(open);
        res.append("Origin");
        res.append(this.screw.getOrigin().toString());
        res.append(comma);
        res.append("Force");
        res.append(this.screw.getTranslation().toString());
        res.append(comma);
        res.append("Torque");
        res.append(this.screw.getRotation().toString());
        res.append(close);
        // Return result
        return res.toString();
    }

}
