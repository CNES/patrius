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
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.io.IOException;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class containing high level methods for the reading of several parts
 * of a DAF binary file.
 * 
 * Based on several routines of the SPICE library.
 * @author T0281925
 *
 */
public final class DafReader {

    /**
     * Constructor
     */
    private DafReader() {
        // Nothing to do
    }

    /**
     * Read the contents of the file record of a DAF.
     * The file record of a DAF is the only record that contains
     * any global information about the file. 
     * 
     * Based on the DAFRFR routine from the SPICE library
     * 
     * @param handle Handle of an open DAF file.
     * @param nd (output) Number of double precision components in summaries.
     * @param ni (output) Number of integer components in summaries.
     * @param ifname (output) Internal file name.
     * @param fward (output) Forward list pointer.
     * @param bward (output) Backward list pointer.
     * @param free (output) Free address pointer.
     * @throws PatriusException if the file is not open or there is a problem while reading the first record
     * @throws IOException if there is a problem tempting to read the first record
     */
    public static void readFileRecord(final int handle,
            final int[] nd,
            final int[] ni,
            final String[] ifname,
            final int[] fward,
            final int[] bward,
            final int[] free) throws PatriusException, IOException {

        // Check to be sure that HANDLE is attached to a file that is open
        // with read access. If the call fails, check out and return.
        DafHandle.checkHandleAccess(handle, "READ");

        // Retrieve all but the internal file name directly from the
        // file record. Read the internal file name into a temporary
        // string, to be sure of the length. Check FOUND.
        final FileRecordDAF fr = new FileRecordDAF(handle);

        if (fr.isFound()) {
            nd[0] = fr.getnDouble();
            ni[0] = fr.getnInt();
            ifname[0] = fr.getInternalFilename();
            fward[0] = fr.getForward();
            bward[0] = fr.getBackward();
            free[0] = fr.getFree();
        } else {
            throw new PatriusException(PatriusMessages.PDB_UNABLE_TO_LOCATE_FILE, handle);
        }
    }

    /** 
     * Public method for the lecture of comments in a DAF file.
     * @param handle Handle of binary DAF opened with read access.
     * @param nLines Maximum size, in lines, of BUFFER.
     * @param lineSize Number of characters in a line (normally 1000)
     * @param nRead (output) Number of extracted comment lines.
     * @param buffer (output) Buffer where extracted comment lines are placed.
     * @param done (output) Indicates whether all comments have been extracted.
     * @throws IOException retrieves the errors from CommentSectionDAF.readComments
     * @throws PatriusException retrieves the errors from CommentSectionDAF.readComments
     */
    public static void readComments(final int handle,
            final int nLines,
            final int lineSize,
            final int[] nRead,
            final String[] buffer,
            final boolean[] done) throws IOException, PatriusException {

        CommentSectionDAF.readComments(handle, nLines, lineSize, nRead, buffer, done);
    }

    /** 
     * Read the double precision data bounded by two addresses within a DAF.
     * Based on the DAFGDA routine from the SPICE library
     * @param handle Handle of a DAF.
     * @param begin Initial address within file.
     * @param end Final address within file.
     * @return Data contained between begin and end.
     * @throws PatriusException retrieves the errors from ReadDoublePrecisionDAF.readContentDPRecord
     */
    public static double[] readDataDaf(final int handle,
            final int begin,
            final int end) throws PatriusException {        
        // Check addresses
        if (begin < 0) {
            throw new IllegalArgumentException();
        } else if (begin > end) {
            throw new IllegalArgumentException();
        }
        // First and last word in a record
        final int firstWord = 1;
        final int lastWord = 128;

        // Convert raw addresses to record/word representations.
        final int[] beginRecord = new int[1];
        final int[] endRecord = new int[1];

        final int[] beginWord = new int[1];
        final int[] endWord = new int[1];

        DafReaderTools.address2RecordWord(begin, beginRecord, beginWord);
        DafReaderTools.address2RecordWord(end, endRecord, endWord);

        // Get as many records as needed. Return the last part of the
        // first record, the first part of the last record, and all of
        // every record in between. Any record not found is assumed to
        // be filled with zeros.
        int next = 0;
        final boolean[] found = new boolean[1];
        int first;
        int last;
        int count;

        //Define array where data will be stored
        final double[] data = new double[end - begin + 1];
        // Do the reading
        for (int record = beginRecord[0]; record <= endRecord[0]; record++) {
            // Determine the first and last word to read in the current record
            if (beginRecord[0] == endRecord[0]) {
                // If only one record is to be read, the first and final word are correct
                first = beginWord[0];
                last = endWord[0];
            } else if (record == beginRecord[0]) {
                // If this is the first record, we read from the indicated first word to 
                // read until the end
                first = beginWord[0];
                last = lastWord;
            } else if (record == endRecord[0]) {
                // If this is the last record, we read from the beginning of the record 
                // until the indicated as last word to be read
                first = firstWord;
                last = endWord[0];
            } else {
                // If this is an intermediate record, read it fully
                first = firstWord;
                last = lastWord;
            }
            count = last - first + 1;
            // Auxiliary array to store the words to be read in the current record
            final double[] aux = ReadDoublePrecisionDAF.readContentDPRecord(handle, record, first, last, found);

            // If the record was found, retrieve the data from the aux to the data array
            if (found[0]) {
                for (int i = next; i < next + count; i++) {
                    data[i] = aux[i - next];
                }
            }
            next += (last - first + 1);
        }
        return data;
    }
}
