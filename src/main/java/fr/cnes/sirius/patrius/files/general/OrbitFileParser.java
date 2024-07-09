/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.files.general;

import java.io.InputStream;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for orbit file parsers.
 * 
 * @author Thomas Neidhart
 */
public interface OrbitFileParser {

    /**
     * Reads an orbit file from the given stream and returns a parsed {@link OrbitFile}.
     * 
     * @param stream
     *        the stream to read from
     * @return a parsed instance of {@link OrbitFile}
     * @exception PatriusException
     *            if the orbit file could not be parsed
     *            successfully from the given stream
     */
    OrbitFile parse(final InputStream stream) throws PatriusException;

    /**
     * Reads the orbit file and returns a parsed {@link OrbitFile}.
     * 
     * @param fileName
     *        the file to read and parse
     * @return a parsed instance of {@link OrbitFile}
     * @exception PatriusException
     *            if the orbit file could not be parsed
     *            successfully from the given file
     */
    OrbitFile parse(final String fileName) throws PatriusException;
}
