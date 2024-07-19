/**
 * Copyright 2023-2023 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.util.Objects;

/**
 * This class is created to define a structure to stock inertial frames, their base frame and the rotation to obtain it
 * from its base.<br>
 * It is based on the lists in the CHGIRF.for of the SPICE library.
 * 
 * <p>
 * Each frame is defined in terms of another frame, except for the root frame, which is defined in terms of itself. For
 * now, the root frame is the standard IAU reference frame, J2000, defined by the Earth mean equator and dynamical
 * equinox of Julian year 2000.
 * </p>
 * <p>
 * Each definition consists of a series of rotations, each through some angle (in arc seconds) and about some axis. The
 * rotations are listed in the opposite order in which they are to be performed, so as to correspond more closely to
 * conventional notation. For example, the definition
 *
 * <pre
 *    NAME   = 'F2'
 *    BASES  = 'F1'
 *    DEFS   = '22.34  3   31.21  2   0.449  1'
 * 
 * means that a vector in frame F1 is converted to the equivalent
 * vector in frame F2 by applying the following rotation:
 * 
 *    -                                            -
 *    v    = ( [ 22.34 ]  [ 31.21 ]  [ 0.449 ]  )  v
 *     F2               3          2          1     F1
 * 
 * where the notation
 * 
 *    [ theta ]
 *             a
 * 
 * means ``rotate through angle theta about axis a.''
 * </pre>
 * 
 * </p>
 * 
 * @author T0281925
 * 
 * @since 4.11
 */
final class InertialFrames {

    /** Name of the frame. */
    private final String name;

    /** Base frame in terms of which "name" is defined. */
    private final String base;

    /** Rotation that gets from base to name. */
    private final String defs;

    /**
     * Constructor.
     * 
     * @param name
     *        Name of the frame
     * @param base
     *        Name of the frame in terms of which it is defined
     * @param defs
     *        String containing the rotations and axis that takes a vector defined in "base" to "name"
     * @throws IllegalArgumentException
     *         if {@code name}, {@code base} or {@code defs} is {@code null}
     */
    public InertialFrames(final String name, final String base, final String defs) {
        // Check for null inputs
        if (name == null) {
            throw new IllegalArgumentException();
        }
        if (base == null) {
            throw new IllegalArgumentException();
        }
        if (defs == null) {
            throw new IllegalArgumentException();
        }

        this.name = name;
        this.base = base;
        this.defs = defs;
    }

    /**
     * Constructor.
     * 
     * @param name
     *        Name of the frame
     * @throws IllegalArgumentException
     *         if {@code name} is {@code null}
     */
    public InertialFrames(final String name) {
        // Check for null input
        if (name == null) {
            throw new IllegalArgumentException();
        }

        this.name = name;
        this.base = "";
        this.defs = "";
    }

    /**
     * Getter for the base frame in terms of which "name" is defined.
     * 
     * @return the base frame in terms of which "name" is defined
     */
    public String getBase() {
        return this.base;
    }

    /**
     * Getter for the rotation that gets from base to name.
     * 
     * @return the rotation that gets from base to name
     */
    public String getDefs() {
        return this.defs;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        // Check the object could be a Counter array
        boolean isEqual = false;

        if (obj == this) {
            // Identity
            isEqual = true;
        } else if ((obj != null) && (obj.getClass() == this.getClass())) {
            // Check only the name, the other attributes are not important here.
            final InertialFrames other = (InertialFrames) obj;
            isEqual = Objects.equals(this.name, other.name);
        }

        return isEqual;
    }
}
