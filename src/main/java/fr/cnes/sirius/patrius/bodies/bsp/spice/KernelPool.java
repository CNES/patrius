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
 * This class class reproduces the data structure created in pool.for for holding kernel pool variables.<br>
 * This is and adaptation of the structure formed by NMPOOL,PNAME,DATLST,DPPOOL,DPVALS,CHPOOL,CHVALS.
 *
 * @author T0281925
 *
 * @since 4.11
 */
public class KernelPool {

    /** Name of the pool (PNAME). */
    private final String name;

    /** Type of data stored in the pool (DATLST). */
    private final String type;

    /**
     * Constructor of KernelPool.
     * 
     * @param name
     *        name of the pool
     * @param type
     *        type of the pool
     * @throws IllegalArgumentException
     *         if {@code name} or {@code type} is {@code null}
     */
    public KernelPool(final String name, final String type) {
        // Check for null inputs
        if (name == null) {
            throw new IllegalArgumentException();
        }
        if (type == null) {
            throw new IllegalArgumentException();
        }

        this.name = name;
        this.type = type;

        // Here there should be the list to contain data. However, reading SPK files it is impossible to fill them.
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.type);
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
            final KernelPool other = (KernelPool) obj;
            isEqual = Objects.equals(this.name, other.name)
                    && Objects.equals(this.type, other.type);
        }

        return isEqual;
    }
}
