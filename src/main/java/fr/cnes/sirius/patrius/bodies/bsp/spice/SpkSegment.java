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

import java.util.Arrays;
import java.util.Objects;

/**
 * Class allowing to instantiate Spk Segments for the list in {@link SpkBody}.<br>
 * The information stored here is the associated handle, description and identifier.
 * <p>
 * This class is based on the structure defined in SPKBSR.for in the SPICE library.
 * </p>
 *
 * @author T0281925
 *
 * @since 4.11
 */
public class SpkSegment {

    /** Handle associated to the segment. */
    private final int handle;

    /** Descriptor associated to the segment. */
    private final double[] description;

    /** Identifier associated to the segment. */
    private final String id;

    /**
     * Constructor.
     * 
     * @param handle
     *        Handle associated to the segment
     * @param description
     *        Descriptor associated to the segment
     * @param id
     *        Identifier associated to the segment
     * @throws IllegalArgumentException
     *         if {@code id} is {@code null}
     */
    public SpkSegment(final int handle, final double[] description, final String id) {
        // Check for null input
        if (id == null) {
            throw new IllegalArgumentException();
        }

        this.handle = handle;
        this.description = Arrays.copyOf(description, SpkBody.SIZEDESC);
        this.id = id;
    }

    /**
     * Getter for the handle associated to the segment.
     * 
     * @return the handle associated to the segment
     */
    public int getHandle() {
        return this.handle;
    }

    /**
     * // Check for null input the descriptor of the segment.
     * 
     * @return the descriptor of the segment
     */
    public double[] getDescription() {
        return Arrays.copyOf(this.description, SpkBody.SIZEDESC);
    }

    /**
     * // Check for null input the identifier of the segment.
     * 
     * @return the identifier of the segment
     */
    public String getId() {
        return this.id;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.description, this.handle, this.id);
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
            final SpkSegment other = (SpkSegment) obj;
            isEqual = Arrays.equals(this.description, other.description)
                    && Objects.equals(this.handle, other.handle)
                    && Objects.equals(this.id, other.id);
        }

        return isEqual;
    }
}
