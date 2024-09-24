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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class is used to store the file record of a DAF in a object.<br
 * It is based on the SPICE library.
 * 
 * @author T0281925
 * 
 * @since 4.11
 */
public final class FileRecordDAF {

    /** The number of doubles in a summary. */
    private int nDouble;

    /** The number integers in a summary. */
    private int nInt;

    /** The record number of the first summary record. */
    private int forward;

    /** The record number of the last summary record. */
    private int backward;

    /** The address of the first free record. */
    private int free;

    /** Internal name of the file. */
    private String internalFilename;

    /** Indicates if the file was found or not. */
    private final boolean found;

    /**
     * Constructor.
     * <p>
     * It performs the reading of the file record of a DAF.<br>
     * Based on the ZZDAFGFR routine from the SPICE library.
     * </p>
     * 
     * @param handle
     *        handle of the file to be read
     * @throws IOException
     *         if there is a problem in the reading of the file
     * @throws PatriusException
     *         if the file is not found or the binary format is not supported
     */
    public FileRecordDAF(final int handle) throws PatriusException {
        // Retrieve information regarding the file from the handle manager.
        // The value of IARCH is not a concern, since this is a DAF routine all values passed into handle manager entry
        // points will have 'DAF' as their architecture arguments.
        this.found = DafHandleManager.getFound(handle);
        if (!this.found) {
            throw new PatriusException(PatriusMessages.PDB_UNABLE_TO_LOCATE_FILE, handle);
        }

        final File file = DafHandleManager.getFile(handle);
        final String bff = DafHandleManager.getBinaryFileFormatFromHandle(handle);

        final RandomAccessFile readFile;
        try {
            readFile = new RandomAccessFile(file, "r");

            final int fileTypeLenght = 8;
            final int intFileNameLegnth = 60;

            if (bff.equals(SpiceCommon.BINARY_FORMAT)) {
                // In the native case, just read the components of the file record from the file.
                DafReaderTools.readString(readFile, fileTypeLenght);
                this.nDouble = readFile.readInt();
                this.nInt = readFile.readInt();
                this.internalFilename = DafReaderTools.readString(readFile, intFileNameLegnth);
                this.forward = readFile.readInt();
                this.backward = readFile.readInt();
                this.free = readFile.readInt();
            } else if ("LTL-IEEE".equals(bff)) {
                DafReaderTools.readString(readFile, fileTypeLenght);
                this.nDouble = Integer.reverseBytes(readFile.readInt());
                this.nInt = Integer.reverseBytes(readFile.readInt());
                this.internalFilename = DafReaderTools.readString(readFile, intFileNameLegnth);
                this.forward = Integer.reverseBytes(readFile.readInt());
                this.backward = Integer.reverseBytes(readFile.readInt());
                this.free = Integer.reverseBytes(readFile.readInt());
            } else {
                readFile.close();
                throw new PatriusException(PatriusMessages.PDB_BFF_NOT_SUPPORTED, bff);
            }

        } catch (final FileNotFoundException e) {
            throw new PatriusException(PatriusMessages.PDB_FILE_NOT_FOUND, e);
        } catch (final IOException e) {
            throw new PatriusException(PatriusMessages.PDB_FILE_CANT_BE_READ, e);
        }
    }

    /**
     * Getter for the number of double precision components in summaries.
     * 
     * @return the number of double precision components in summaries
     */
    public int getnDouble() {
        return this.nDouble;
    }

    /**
     * Getter for the number of integer components in a summary.
     * 
     * @return the number of integer components in a summary
     */
    public int getnInt() {
        return this.nInt;
    }

    /**
     * Getter for the first summary record.
     * 
     * @return the first summary record
     */
    public int getForward() {
        return this.forward;
    }

    /**
     * Getter for the last summary record.
     * 
     * @return the last summary record
     */
    public int getBackward() {
        return this.backward;
    }

    /**
     * Getter for the first free address in the file.
     * 
     * @return the first free address in the file
     */
    public int getFree() {
        return this.free;
    }

    /**
     * Getter for the internal file name.
     * 
     * @return the internal file name
     */
    public String getInternalFilename() {
        return this.internalFilename;
    }

    /**
     * Getter for a boolean indicating if a file associated to the handle was found or not.
     * 
     * @return a boolean indicating if a file associated to the handle was found or not
     */
    public boolean isFound() {
        return this.found;
    }
}
