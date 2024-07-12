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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class includes auxiliary methods for the DAF 
 * files reading.
 * 
 * @author T0281925
 *
 */
public final class DafReaderTools {

    /**
     * Constructor
     */
    private DafReaderTools() {
        // Nothing to do
    }

    /**
     * Read a String of a given length from the current pointer
     * in the RandomAccessFile on input.
     * @param file RandomAccessFile already opened from where
     *        the string is read
     * @param length Length of the desired String
     * @return String read
     * @throws IOException if there is a problem while reading the file
     */
    public static String readString(final RandomAccessFile file,
            final int length) throws IOException {
        //Define a byte array where we will read the desired content.
        final byte[] bytes = new byte[length];

        final int l = file.read(bytes, 0, length);
        if (l == length) {
            return new String(bytes, Charset.defaultCharset());
        } else {
            throw new IOException();
        }
        
    }

    /**
     * Transform a record number into the byte address of its beginning.
     * @param nRecord File record we want to access.
     * @return file byte where the record starts
     */
    public static long nRecord2nByte(final int nRecord) {
        if (nRecord >= 1) {
            return (nRecord - 1) * SpiceCommon.RECORD_LENGTH;
        } else {
            throw new MathIllegalArgumentException(PatriusMessages.PDB_ILLEGAL_RECORD, nRecord);
        }
    }

    /**
     * Transform an address into a record number and a word inside the record.
     * There are 128 words in each record, being each 8 bytes (a double precision)
     * @param address byte address we want to transform
     * @param record (output) file record where the address points
     * @param word (output) word inside the record where the address points.
     */
    public static void address2RecordWord(final int address,
            final int[] record,
            final int[] word) {
        if (address < 0) {
            throw new MathIllegalArgumentException(PatriusMessages.PDB_ILLEGAL_ADDRESS, address);
        }
        final int nWords = 128;

        // If address is legal, the computation is straightforward
        record[0] = (address - 1) / nWords + 1;
        word[0] = address - (record[0] - 1) * nWords;
    }

}
