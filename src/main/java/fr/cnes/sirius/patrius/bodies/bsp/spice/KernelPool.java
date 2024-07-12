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
 * This class class reproduces the data structure created in pool.for for holding kernel pool variables
 * This is and adaptation of the structure formed by NMPOOL,PNAME,DATLST,DPPOOL,DPVALS,CHPOOL,CHVALS
 * <p>
 *
 * @author
 *
 * @since 4.11
 */
public class KernelPool {

    /**
     * Name of the pool. (PNAME)
     */
    private final String name;

    /**
     * Type of data stored in the pool. (DATLST)
     */
    private final String type;

    /**
     * Constructor of KernelPool
     * @param name of the pool
     * @param type of the pool
     */
    public KernelPool(final String name,
            final String type) {
        if (name == null) {
            throw new IllegalArgumentException();
        } else {
            this.name = name;
        }
        if (type == null) {
            throw new IllegalArgumentException();
        } else {
            this.type = type;
        }

        // Here there should be the list to contain data. However, reading SPK files it is impossible to fill them.
    }

    /**
     * Calculate the hash code for the kernelPool object
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        // Calculate a hashCode combining both string particular methods
        result = prime * result + name.hashCode();
        result = prime * result + type.hashCode();
        return result;
    }

    /**
     * Return whether 2 KernelPool objects are the same or not
     */
    @Override
    public boolean equals(final Object obj) {
        // Some fast checks
        if (this == obj) {
            return true;
        }            
        if (obj == null) {
            return false;
        }           
        if (getClass() != obj.getClass()) {
            return false;
        } 
        // Create a kernelPool object
        final KernelPool other = (KernelPool) obj;
        // Compare the interesting data : 
        // variable name and type of data stored
        return name.equals(other.name) && type.equals(other.type);
    }

}
