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

import java.nio.ByteBuffer;
import java.util.Arrays;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class containing constants and auxiliary methods for the rest of the SPICE related classes.
 * 
 * @author T0281925
 *
 * @since 4.11
 */
public final class SpiceCommon {

    /** Size of counter arrays. */
    public static final int CTRSIZ = 2;

    /** Size of the file tables. */
    public static final int FILE_TABLE_SIZE = 5000;

    /** Java binary format. */
    public static final String BINARY_FORMAT = "BIG-IEEE";

    /** Number of bytes in a record. */
    public static final int RECORD_LENGTH = 1024;

    /** Max number of characters in a record. */
    public static final int MAX_CHAR_RECORD = 1000;

    /** String to define an unknown architecture or file type. */
    public static final String UNKNOWN = "?";

    /** Empty string for comparisons. */
    public static final String EMPTY = "";

    /** String literal for DAF architecture. */
    public static final String DAF = "DAF";

    /** String literal for SPK file type. */
    public static final String SPK = "SPK";

    /** Number of bytes in a double precision number. */
    public static final int BYTES_DOUBLE = 8;

    /**
     * Constructor.
     */
    private SpiceCommon() {
        // Nothing to do
    }

    /**
     * Extract the architecture and type of a SPICE binary kernel file from a file ID word.
     * <p>
     * Based on IDW2AT routine from the SPICE library
     * </p>
     * 
     * @param idword
     *        the ID word from a SPICE binary kernel file
     * @return a 2 component array containing the architecture and the file type identified from the idword
     * @throws PatriusException
     *         if the architecture is not DAF TODO change if more architectures are allowed
     */
    public static String[] idword2architype(final String idword) throws PatriusException {
        final String[] archType = new String[2];
        String arch = UNKNOWN;
        String type = UNKNOWN;

        // Check to see if we got a blank string for the ID word. If we did, set the architecture and type to unknown.
        if (EMPTY.equals(idword)) {
            arch = UNKNOWN;
            type = UNKNOWN;

            archType[0] = arch;
            archType[1] = type;

            return archType;
        }

        // Look for a '/' in the string. If we can't find it, we don't recognize the architecture or the type, so set
        // the architecture and type to unknown.
        final int slash = idword.indexOf("/");

        if (slash < 0) {
            arch = UNKNOWN;
            type = UNKNOWN;

            archType[0] = arch;
            archType[1] = type;

            return archType;
        }

        // The part before the slash is the architecture or the word 'NAIF' in older files and the part after the slash
        // is the type of file or the architecture in older files.
        final String part1 = idword.substring(0, slash);
        final String part2 = idword.substring(slash + 1);

        // Let's now do some testing to try and figure out what's going on.
        //
        // First we look for the information in the ID word format:
        //
        // <architecture>/<type>,
        //
        // then we look for the things that begin with the word 'NAIF'
        if (DAF.equals(part1)) {
            arch = DAF;
            // We have a DAF file, so set the architecture and type.
            if (!EMPTY.equals(part2)) {
                type = part2.toString();
            } else {
                type = UNKNOWN;
            }

            archType[0] = arch;
            archType[1] = type;

            return archType;
        } else if ("NAIF".equals(part1)) {
            // We have a DAF (or NIP, these are equivalent) or DAS file, identified by the value of PART2, but we have
            // no idea what the type is, unless the file is a DAS file, in which case it is a pre-release EK file, since
            // these are the only DAS files which used the 'NAIF/DAS' ID word.

            // First, we determine the architecture from PART2, then if it is DAF or NIP, we give up on the type. As
            // mentioned above, if PART2 contains DAS, we know a priori the type of the file.
            if (DAF.equals(part2) || "NIP".equals(part2)) {
                arch = DAF;
                type = UNKNOWN;
            } else {
                throw new PatriusException(PatriusMessages.PDB_WRONG_ARCHITECTURE, part2);
            }
            archType[0] = arch;
            archType[1] = type;

            return archType;

        } else {
            archType[0] = arch;
            archType[1] = type;
            return archType;
        }
    }

    /**
     * Check a character string that may contain the FTP validation string for FTP based errors.
     * <p>
     * Based on the ZZFTPCHK routine from the SPICE library
     * </p>
     * 
     * @param ftp
     *        String that may contain the FTP validation string
     * @return boolean indicating if FTP corruption occurred
     */
    public static boolean ftpCheck(final String ftp) {
        boolean err = false;
        // Do the work of zzftpstr : initialize
        final String leftbkt = "FTPSTR";
        final String rightbkt = "ENDFTP";
        final char delim = ':';
        final char[] memchr = { delim, 13, delim, 10, delim, 13, 10, delim, 13, 0, delim };// TODO ,129,':',16,206,':'};
        final String memstr = new String(memchr);

        // Extract the FTP validation string from the block of text that was passed into the routine via the argument
        // STRING. Note, if the bracketed substring in the text block STRING is larger than the FILSTR string size,
        // ZZRBRKST will truncate the data that does not fit. This loss of data is not an issue, since in this case we
        // may only validate the part of the substring near the head, for which we have enough room in FILSTR.

        // ZZRBRKST
        // Search for the limits at the end of the string
        final int begin = ftp.lastIndexOf(leftbkt);
        final int end = ftp.indexOf(rightbkt, begin);
        String substr = EMPTY;
        boolean isThere = false;
        if (begin >= 0 && end >= 0) {
            // if both exists
            if (end == begin + leftbkt.length()) {
                // if both exists but they are adjacent, we found, but nothing inside
                isThere = true;
            } else {
                // if they are not adjacent, substr is filled.
                isThere = true;
                substr = ftp.substring(begin + leftbkt.length(), end);
            }
        } else {
            isThere = begin + end >= -1;
        }

        // Now check ISTHER to see if either LFTBKT or RGTBKT was present in the block of text from the file. If both
        // are absent, then we must assume that this text is from a pre-FTP validation file, and as such do not return
        // any indication of an error.

        if (!isThere) {
            err = false;

            // If one of the brackets is present, then we may proceed with validation. First check to see if the length
            // is 0. If it is, then at least one of the brackets was present, but ZZRBRKST was unable to extract a
            // properly bracketed substring. This is an error.
        } else if (substr.length() == 0) {
            err = true;
            // Now we make it to this ELSE statement only if ISTHER is TRUE, and LENGTH is a positive number. Compare
            // the contents of FILSTR and MEMSTR.
        } else {
            // First determine if the data from the file is a subset of what is stored in memory.
            final int fsmidx = memstr.indexOf(substr);

            // In the event that FSMIDX is non-negative, we know that substr is a substring of MEMSTR, and thus we have
            // validated all the test clusters from the file.
            if (fsmidx >= 0) {
                err = false;

                // If FSMIDX is negative, then we do not yet know whether or not the file is valid. Now it may be the
                // case that this file contains a newer FTP validation string than this version of the toolkit is aware.
                // Check to see whether what's in memory is a substring of what's in substr.
            } else {
                final int msfidx = substr.indexOf(memstr);

                // If this comes back as zero, then we definitely have an FTP error. Set FTPERR appropriately.
                err = msfidx < 0;
            }
        }

        return err;
    }

    /**
     * Find the first occurrence in a string of a character NOT being the char on input.
     * <p>
     * Based on the NCPOS routine in the SPICE library
     * </p>
     * 
     * @param s
     *        Any character string
     * @param ch
     *        a character
     * @return the function returns the index of the first character of STR that is not the character in input. If no
     *         such character is found, the function returns -1
     */
    public static int indexOfNoChar(final String s, final char ch) {
        int idx = 0;

        // Retrieve the first position of ch in the string
        int index = s.indexOf(ch, idx);
        if (index == 0) {
            // If it is the first character in the string, look for the next ocurrence
            while (index == idx && idx < s.length() - 1) {
                idx = index + 1;
                index = s.indexOf(ch, idx);
            }
            if (idx == s.length() - 1) {
                // If we get to the end, all the characters where ch
                return -1;
            }
            // We found the first not to be ch
            return idx;
        }
        // If the first is not ch, it what we search
        return 0;
    }

    /**
     * Unpack an array summary into its double precision and integer components.
     * <p>
     * Based on the DAFUS routine of the SPICE library
     * </p>
     * 
     * @param sum
     *        Array summary
     * @param nd
     *        Number of double precision components
     * @param ni
     *        Number of integer components
     * @param dc
     *        (out) Double precision components
     * @param ic
     *        (out) Integer components
     */
    public static void unpackSummary(final double[] sum, final int nd, final int ni, final double[] dc,
                                     final int[] ic) {
        // Determine the number of double precision components
        final int n = MathLib.min(125, MathLib.max(0, nd));
        // Retrieve the double precision components from sum
        System.arraycopy(sum, 0, dc, 0, n);

        // Determine the number of integer components
        final int m = MathLib.min(250 - 2 * n, MathLib.max(0, ni));

        int j = 0;
        // Determine the last component in sum to be read
        final int end = n + (m - 1) / 2 + 1;
        // Retrieve the integers from sum
        for (int i = n; i < end; i++) {
            final byte[] bytes = ByteBuffer.allocate(BYTES_DOUBLE).putDouble(sum[i]).array();
            // First 4 bytes are an integer
            ic[j] = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0, 4)).getInt();
            j++;
            // Second 4 bytes are an integer
            ic[j] = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 4, BYTES_DOUBLE)).getInt();
            j++;
        }
    }
}
