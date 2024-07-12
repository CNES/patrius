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
 * Class created to contain the information relative to SPICE kernel loaded
 * for {@link SpiceKernelManager}
 * @author T0281925
 *
 */
public final class SpiceKernelInfo {
    /**
     * Filename
     */
    private final String file;
    /**
     * File type
     */
    private final String type;
    /**
     * If the file is loaded directly, its source
     * will be zero.  If it is loaded as the result of meta-information
     * in a text kernel, the index of the source file in FILES will
     * be stored.
     */
    private final int source;
    /**
     * Handle associated to the file
     */
    private int handle;
    
    /**
     * Constructor
     * @param file File name
     * @param type File type
     * @param source File source
     * @param handle File associated handle
     */
    public SpiceKernelInfo(final String file, 
                           final String type, 
                           final int source, 
                           final int handle) {
        if (file == null) {
            throw new IllegalArgumentException();
        } else {
            this.file = file;
        }
        
        if (type == null) {
            throw new IllegalArgumentException();
        } else {
            this.type = type;
        }
                
        this.source = source;
        this.handle = handle;
    }

    /**
     * Get the file name
     * @return file name
     */
    public String getFile() {
        return file;
    }

    /**
     * Get the file type
     * @return file type
     */
    public String getType() {
        return type;
    }

    /**
     * Get the file source
     * @return file source
     */
    public int getSource() {
        return source;
    }

    /**
     * Get the handle associated to the file
     * @return the handle associated to the file
     */
    public int getHandle() {
        return handle;
    }   
    
    /**
     * Set the handle associated to the file
     * @param h the new handle associated to the file
     */
    public void setHandle(final int h) {
        handle = h;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + file.hashCode();
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        // Check obj is appropriate for a comparison
        if (this == obj) {
            return true;
        } else  if (!(obj instanceof SpiceKernelInfo)) {
            return false;
        }
        // Instantiate the SpiceKernelInfo object
        final SpiceKernelInfo other = (SpiceKernelInfo) obj;
        return file.equals(other.file);
    }
    
    
}
