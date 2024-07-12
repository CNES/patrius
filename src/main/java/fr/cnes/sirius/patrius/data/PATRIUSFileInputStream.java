/**
 * Copyright 2011-2022 CNES
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
package fr.cnes.sirius.patrius.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Extension of {@link FileInputStream} with file name storage.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.11.1
 */
public class PATRIUSFileInputStream extends FileInputStream {

    /** File associated to stream. */
    private final File file;
    
    /**
     * Constructor.
     * @param file file
     * @throws FileNotFoundException thrown if file not found
     */
    public PATRIUSFileInputStream(final File file) throws FileNotFoundException {
        super(file);
        this.file = file;
    }
    
    /**
     * Returns the file associated to the stream.
     * @return the file associated to the stream
     */
    public File getFile() {
        return file;
    }
}
