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

import java.util.Arrays;

/**
 * Class allowing to instantiate Spk Segments for the list in {@link SpkBody}
 * The information stored here is the associated handle, description and identifier.
 * 
 * This class is based on the structure defined in SPKBSR.for in the SPICE library.
 * @author T0281925
 *
 */
public class SpkSegment {

    /**
     * Handle associated to the segment
     */
    private final int handle;
    /**
     * Descriptor associated to the segment
     */
    private final double[] description;
    /**
     * Identifier associated to the segment
     */
    private String id;

    /**
     * Constructor
     * @param handle Handle associated to the segment
     * @param description Descriptor associated to the segment
     * @param id Identifier associated to the segment
     */
    public SpkSegment(final int handle,
            final double[] description,
            final String id) {
        this.handle = handle;
        this.description = Arrays.copyOf(description,SpkBody.SIZEDESC);
        if (id == null) {
            throw new IllegalArgumentException();
        } else {
            this.id = id;
        }
    }

    /**
     * Get the handle associated to the segment
     * @return the handle associated to the segment
     */
    public int getHandle() {
        return handle;
    }

    /**
     * Get the descriptor of the segment
     * @return the descriptor of the segment
     */
    public double[] getDescription() {
        return Arrays.copyOf(description, SpkBody.SIZEDESC);
    }

    /**
     * Get the identifier of the segment
     * @return the identifier of the segment
     */
    public String getId() {
        return id;
    }


    /** 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(description);
        result = prime * result + handle;
        result = prime * result + id.hashCode();
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        // Check reference
        if (this == obj) {
            return true;
        }
        // Check not null
        if (obj == null) {
            return false;
        }
        // Check if it is an instance of the teh class
        if (!(obj instanceof SpkSegment)) {
            return false;
        }
        // Instantiate
        final SpkSegment other = (SpkSegment) obj;
        // Check description handle id
        return Arrays.equals(description, other.description) && (handle == other.handle) && id.equals(other.id);
    }

}
