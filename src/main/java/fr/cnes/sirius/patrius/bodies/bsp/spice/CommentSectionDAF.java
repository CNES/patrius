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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class dedicated to the reading of the comment section of binary DAF files.
 * 
 * A binary DAF contains an area which is reserved for storing annotations or descriptive textual information describing
 * the data contained in a file. This area is referred to as the ''comment area'' of the file. The comment area of a DAF
 * is a line oriented medium for storing textual information. The comment area preserves any leading or embedded white
 * space in the line(s) of text which are stored, so that the appearance of the of information will be unchanged when it
 * is retrieved (extracted) at some other time.
 * 
 * The class is inspired by the dafec.for file from the SPICE library.
 * 
 * @author T0281925
 * 
 * @since 4.11
 */
public final class CommentSectionDAF {

    /** Decimal value for the DAF comment area end-of-comment (EOC) marker. */
    private static final int INTEOC = 4;

    /** Decimal value for the DAF comment area end-of-line (EOL) marker. */
    private static final int INTEOL = 0;

    /** Boolean indicating if it is the first time calling any method in the class for initialization purposes. */
    private static boolean first = true;

    /** Number of files from which we have already extracted comments. */
    private static int nfiles;

    /** Handle of the last file we extracted comments from. */
    private static int lsthan;

    /** Characters indicating the end of line and the end of the comment section. */
    private static char eocmrk, eolmrk;

    /**
     * The file table declarations for keeping track of which files are currently in the process of having comments
     * extracted.
     */
    private static List<Integer> filchr;

    /** Number of comment characters for each file in the process of having comments extracted. */
    private static List<Integer> filcnt;

    /** List of handles of files in the process of having comments extracted. */
    private static List<Integer> filhan;

    /** List of the position in the record for each file in the process of having comments extracted. */
    private static List<Integer> lstpos;

    /** List of the current record for each file in the process of having comments extracted. */
    private static List<Integer> lstrec;

    /** String containing the content of the last comment line read. */
    private static String crecrd = SpiceCommon.EMPTY;

    /**
     * Constructor
     */
    private CommentSectionDAF() {
        // Nothing to do
    }

    /**
     * Private method for initialization purposes.
     */
    private static void init() {
        first = false;
        nfiles = 0;
        lsthan = 0;
        eocmrk = (char) INTEOC;
        eolmrk = (char) INTEOL;

        filchr = new ArrayList<>(SpiceCommon.FILE_TABLE_SIZE);
        filcnt = new ArrayList<>(SpiceCommon.FILE_TABLE_SIZE);
        filhan = new ArrayList<>(SpiceCommon.FILE_TABLE_SIZE);
        lstpos = new ArrayList<>(SpiceCommon.FILE_TABLE_SIZE);
        lstrec = new ArrayList<>(SpiceCommon.FILE_TABLE_SIZE);
    }

    /**
     * This method will read the comments from the comment area of a binary DAF, placing them into a line buffer. If the
     * line buffer is not large enough to hold the entire comment area, the portion read will be returned to the caller,
     * and the DONE flag will be set to .FALSE. This allows the comment area to be read in ``chunks,'' a buffer at a
     * time. After all of the comment lines have been read, the DONE flag will be set to .TRUE.
     * 
     * This method can be used to ``simultaneously'' extract comments from the comment areas of multiple binary DAFs.
     * 
     * This method is based on the DAFEC routine from the SPICE library.
     * 
     * @param handle
     *        Handle of binary DAF opened with read access.
     * @param nLines
     *        Maximum size, in lines, of BUFFER.
     * @param lineSize
     *        Number of characters in a line (normally 1000)
     * @param nRead
     *        (output) Number of extracted comment lines.
     * @param buffer
     *        (output) Buffer where extracted comment lines are placed.
     * @param done
     *        (output) Indicates whether all comments have been extracted.
     * @throws IOException
     *         in case there is a problem opening or reading the file
     * @throws PatriusException
     *         in case there is a problem regarding the SPICE algorithm
     */
    //CHECKSTYLE: stop MethodLength check
    //CHECKSTYLE: stop CyclomaticComplexity check
    //Reason: Spice code kept as such
    public static void readComments(final int handle, final int nLines, final int lineSize, final int[] nRead,
                                    final String[] buffer, final boolean[] done) throws IOException, PatriusException {
        //CHECKSTYLE: resume MethodLength check
        //CHECKSTYLE: resume CyclomaticComplexity check

        // Initialize
        if (first) {
            init();
        }
        // Clean the buffer array
        Arrays.fill(buffer, SpiceCommon.EMPTY);
        // Verify that the DAF attached to HANDLE is opened for reading by calling the routine to signal an invalid
        // access mode on a handle.
        DafHandle.checkHandleAccess(handle, "READ");

        if (nLines <= 0) {
            throw new IllegalArgumentException();
        }

        // Convert the DAF handle to its corresponding File object for reading the comment records.
        final File file = DafHandleManager.getFile(handle);
        final RandomAccessFile readFile = new RandomAccessFile(file, "r");

        // Declare some local variables
        int recno; // record number
        int curpos; // current position
        int nchars; // number of characters read
        final int ncomc; // number of characters in the comment section

        // If we have extracted comments from at least one file and we didn't finish, check to see if HANDLE is in the
        // file table.
        int index = -1; // index of the current file in the handle list
        if (nfiles > 0) {
            index = filhan.indexOf(Integer.valueOf(handle));
        }

        // Check to see if we found HANDLE in the file handle table.
        if (index >= 0) {
            // Set the record number and the starting position accordingly, i.e., where we left off when we last read
            // from that file.
            recno = lstrec.get(index);
            curpos = lstpos.get(index);
            nchars = filchr.get(index);
            ncomc = filcnt.get(index);
        } else {
            ncomc = getNumberOfCharsInCommentSection(handle, readFile);

            // If the number of comment characters, NCOMC, is equal to zero, then we have no comments to read, so set
            // the number of comments to zero, set DONE to .TRUE., check out, and return.
            if (ncomc == 0) {
                nRead[0] = 0;
                done[0] = true;
                return;
            }

            // Otherwise, set the initial position in the comment area.
            recno = 2;
            curpos = 0;
            nchars = 0;
        }

        // Begin reading the comment area into the buffer.
        if (handle != lsthan) {
            // If the current DAF handle is not the same as the handle on the last call, then we need to read in the
            // appropriate record from the DAF comment area. Otherwise the record was saved and so we don't need to read
            // it in.
            readFile.seek(DafReaderTools.nRecord2nByte(recno));
            crecrd = DafReaderTools.readString(readFile, SpiceCommon.MAX_CHAR_RECORD);
        }

        // Initialize the BUFFER line counter, I, and the line position counter, J
        int j = 0;

        // Start filling up the BUFFER.
        int numcom = 0;
        done[0] = false;

        StringBuffer strBuffer;
        for (int i = 0; i < nLines; i++) {
            boolean eol = false;
            strBuffer = new StringBuffer(buffer[i]);
            while (!eol) {
                nchars += 1;
                final char ch = crecrd.charAt(curpos);

                // If we found end of line, fill the buffer with white spaces
                if (ch == INTEOL) {
                    eol = true;
                    if (j < lineSize) {
                        for (int k = j; k < lineSize; k++) {
                            strBuffer.append(' ');
                        }
                        j++;
                    }
                    // if not, add the char to the buffer
                } else {
                    if (j < lineSize) {
                        strBuffer.append(ch);
                        j++;
                    } else {
                        throw new PatriusException(PatriusMessages.PDB_COMMENT_BUFFER_TOO_SHORT, lineSize);
                    }
                }

                // If we have reached the end of the current comment record, read in the next one and reset the current
                // position.
                // Otherwise, just increment the current position.
                if (curpos == SpiceCommon.MAX_CHAR_RECORD - 1) {
                    recno++;
                    readFile.seek(DafReaderTools.nRecord2nByte(recno));
                    crecrd = DafReaderTools.readString(readFile, SpiceCommon.MAX_CHAR_RECORD);
                    curpos = 0;
                } else {
                    curpos++;
                }

                // Check to make sure that it is safe to continue, i.e., that the number of comment characters we have
                // processed has not exceeded the number of comment characters in the comment area of the DAF file. This
                // should never happen.
                if (nchars > ncomc) {
                    throw new PatriusException(PatriusMessages.PDB_COUNT_CHARS_EXCEEDS);
                }
            }

            // We have just completed a comment line, so we save the comment number, increment the buffer line counter,
            // I, and reset the buffer line position counter, J.
            numcom = i + 1;
            buffer[i] = new String(strBuffer);
            j = 0;

            // Check for the end of the comments.
            if (nchars == ncomc) {
                // If we have reached the end of the comments, signaled by having processed all of the comment
                // characters, NCOMC, then we are done. So, set DONE to .TRUE. and remove the entry for this file from
                // the file table.
                lsthan = 0;
                cleanLists(index);
                done[0] = true;
                break;
            }
        }

        // Set the number of comment lines in the buffer
        nRead[0] = numcom;

        // At this point, we have either filled the buffer or we have finished reading in the comment area. Find out
        // what has happened and act accordingly.
        if (!done[0]) {
            saveInfo(nchars, ncomc, handle, recno, curpos, index);
        }
    }

    /**
     * Do a first reading of the comment section to determine the number of chars and records in it.
     * 
     * @param handle
     *        handle associated to the file
     * @param readFile
     *        the current file
     * @return integer array containing the total number of chars and records that contains the comments section.
     * @throws IOException
     *         if there is a problem reading the file
     * @throws PatriusException
     *         if the character that determines the end of the section is not found
     */
    private static int getNumberOfCharsInCommentSection(final int handle, final RandomAccessFile readFile)
        throws IOException, PatriusException {
        // We have not yet read any comments from this file, so start at the start. To get to the first comment record,
        // we need to skip the file record. We also need to count the number of comment characters.
        //
        // Read the file record from the DAF attached to HANDLE. We will get back some stuff that we do not use.
        final FileRecordDAF fr = new FileRecordDAF(handle);

        // Declarations
        int ncomc = 0;
        boolean empty; // indicates if the comment section is empty
        boolean found; // indicates if the end of comment section character is found
        int recno; // record number
        int eocpos = 0; // position of the end of line character

        // Compute the number of comment records and the number of comment characters. In order to perform these
        // calculations, we assume that we have a valid comment area in the DAF attached to HANDLE.
        int ncomr = fr.getForward() - 2;

        if (ncomr > 0) {
            // The starting record number is the number of comment records
            // + 1 where the 1 skips the file record.
            empty = true;
            found = false;

            while ((ncomr > 0) && (!found) && empty) {
                recno = ncomr + 1;

                // Let's get to the correct position
                readFile.seek(DafReaderTools.nRecord2nByte(recno));

                crecrd = DafReaderTools.readString(readFile, SpiceCommon.MAX_CHAR_RECORD);

                // Scan the comment record looking for the end of the comments marker
                eocpos = crecrd.indexOf(INTEOC);

                if (eocpos >= 0) {
                    found = true;
                } else {
                    final int nelpos = SpiceCommon.indexOfNoChar(crecrd, eolmrk);

                    if (nelpos >= 0) {
                        empty = false;
                    } else {
                        ncomr -= 1;
                    }
                }
            }

            // If we do not find the end of comments marker and the comment area is not empty, then it is an error
            if (!found && !empty) {
                throw new PatriusException(PatriusMessages.PDB_EOC_NOT_FOUND);
            } else if (found) {
                ncomc = SpiceCommon.MAX_CHAR_RECORD * (ncomr - 1) + eocpos;
            } else if (empty) {
                ncomc = 0;
            }
        } else {
            ncomc = 0;
        }
        return ncomc;
    }

    /**
     * Save all the information in case we didn't finish reading the comment section of the current file.
     * 
     * @param nchars
     *        number of characters read
     * @param ncomc
     *        number of characters in the comment section
     * @param handle
     *        handle associated with the current file
     * @param recno
     *        record number
     * @param curpos
     *        current position
     * @param index
     *        index in the list where the information is stored
     * @throws PatriusException
     *         if the number of files is already the maximum.
     */
    private static void saveInfo(final int nchars, final int ncomc, final int handle, final int recno,
                                 final int curpos, final int index) throws PatriusException {
        // If we are not done, then we have filled the buffer, so save everything that needs to be saved in the file
        // table before exiting.
        if (index < 0) {
            // This was the first time that the comment area of this file has been read, so add it to the file table and
            // save all of its information if there is room in the file table.
            if (nfiles >= SpiceCommon.FILE_TABLE_SIZE) {
                throw new PatriusException(PatriusMessages.PDB_TOO_MANY_FILES);
            }

            nfiles += 1;
            filchr.add(Integer.valueOf(nchars));
            filcnt.add(Integer.valueOf(ncomc));
            filhan.add(Integer.valueOf(handle));
            lstrec.add(Integer.valueOf(recno));
            lstpos.add(Integer.valueOf(curpos));
            lsthan = handle;
        } else {
            // The comment area of this file is already in the file table, so just update its information.
            filchr.set(index, Integer.valueOf(nchars));
            lstrec.set(index, Integer.valueOf(recno));
            lstpos.set(index, Integer.valueOf(curpos));
            lsthan = handle;
        }
    }

    /**
     * Remove an element from the lists if it is necessary
     * 
     * @param index
     *        the index concerned in the lists. If it is -1, nothing to remove
     */
    private static void cleanLists(final int index) {
        // -1 <= INDEX < NFILES, and we only want to remove things from the file table if:
        //
        // The file we are currently reading from is in the file table, INDEX >= 0, which implies NFILES > 0.
        //
        // So, if INDEX >= 0, we know that there are files in the file table, and that we are currently reading from one
        // of them.
        if (index >= 0) {
            filchr.remove(index);
            filcnt.remove(index);
            filhan.remove(index);
            lstrec.remove(index);
            lstpos.remove(index);
            nfiles--;
        }
    }
}
