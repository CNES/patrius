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
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

/**
 * This class is created to define a structure to stock inertial frames, their base
 * frame and the rotation to obtain it from its base.
 * It is based on the lists in the CHGIRF.for of the SPICE library. 
 * 
 * Each frame is defined in terms of another frame, except for
 * the root frame, which is defined in terms of itself. For now,
 * the root frame is the standard IAU reference frame, J2000,
 * defined by the Earth mean equator and dynamical equinox of
 * Julian year 2000.
 *
 * Each definition consists of a series of rotations, each
 * through some angle (in arc seconds) and about some axis.
 * The rotations are listed in the opposite order in which
 * they are to be performed, so as to correspond more closely
 * to conventional notation. For example, the definition
 *
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
 * 
 * @author T0281925
 *
 */
final class InertialFrames {

    /**
     * Name of the frame
     */
    private final String name;
    /**
     * Base frame in terms of which "name" is defined
     */
    private final String base;
    /**
     * Rotation that gets from base to name.
     */
    private final String defs;
    
    /**
     * Constructor 
     * @param name Name of the frame
     * @param base Name of the frame in terms of which it is defined
     * @param defs String containing the rotations and axis that takes a vector 
     *             defined in "base" to "name".
     */
    public InertialFrames(final String name, final String base, final String defs) {
        if (name == null) {
            throw new IllegalArgumentException();
        } else {
            this.name = name;
        }
        
        if (base == null) {
            throw new IllegalArgumentException();
        } else {
            this.base = base;
        }
        
        if (defs == null) {
            throw new IllegalArgumentException();
        } else {
            this.defs = defs;
        }     
    }
    
    /**
     * Constructor 
     * @param name Name of the frame
     */
    public InertialFrames(final String name) {
        if (name == null) {
            throw new IllegalArgumentException();
        } else {
            this.name = name;
        }
        this.base = "";
        this.defs = "";
    }

    /**
     * @return the base
     */
    public String getBase() {
        return base;
    }

    /**
     * @return the defs
     */
    public String getDefs() {
        return defs;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        return result;
    }

    /** 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        // Check if it is the same object
        if (this == obj) {
            return true;
        }
        // Check it is not null
        if (obj == null) {
            return false;
        }
        // Check if it is an instance of the concerned class
        if (!(obj instanceof InertialFrames)) {
            return false;
        }
        final InertialFrames other = (InertialFrames) obj;
        // Check only the name, the other attributes are not important here.
        return name.equals(other.name);
    }
  
}
