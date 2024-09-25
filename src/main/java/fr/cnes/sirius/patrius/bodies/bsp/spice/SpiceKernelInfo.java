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
 * Class created to contain the information relative to SPICE kernel loaded for {@link SpiceKernelManager}.
 * 
 * @author T0281925
 *
 * @since 4.11
 */
public final class SpiceKernelInfo {
    /** Filename. */
    private final String file;

    /** File type. */
    private final String type;

    /**
     * If the file is loaded directly, its source will be zero. If it is loaded as the result of meta-information in a
     * text kernel, the index of the source file in FILES will be stored.
     */
    private final int source;
    /** Handle associated to the file. */
    private int handle;

    /**
     * Constructor.
     * 
     * @param file
     *        File name
     * @param type
     *        File type
     * @param source
     *        File source
     * @param handle
     *        File associated handle
     * @throws IllegalArgumentException
     *         if {@code file} or {@code type} is {@code null}
     */
    public SpiceKernelInfo(final String file, final String type, final int source, final int handle) {
        // Check for null inputs
        if (file == null) {
            throw new IllegalArgumentException();
        }
        if (type == null) {
            throw new IllegalArgumentException();
        }

        this.file = file;
        this.type = type;
        this.source = source;
        this.handle = handle;
    }

    /**
     * Getter for the file name.
     * 
     * @return the file name
     */
    public String getFile() {
        return this.file;
    }

    /**
     * Getter for the file type.
     * 
     * @return the file type
     */
    public String getType() {
        return this.type;
    }

    /**
     * Getter for the file source.
     * 
     * @return the file source
     */
    public int getSource() {
        return this.source;
    }

    /**
     * Getter for the handle associated to the file.
     * 
     * @return the handle associated to the file
     */
    public int getHandle() {
        return this.handle;
    }

    /**
     * Setter for the handle associated to the file.
     * 
     * @param h
     *        the new handle associated to the file
     */
    public void setHandle(final int h) {
        this.handle = h;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.file, this.type, this.source, this.handle);
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
            final SpiceKernelInfo other = (SpiceKernelInfo) obj;
            isEqual = Objects.equals(this.file, other.file)
                    && Objects.equals(this.type, other.type)
                    && Objects.equals(this.source, other.source)
                    && Objects.equals(this.handle, other.handle);
        }

        return isEqual;
    }
}
