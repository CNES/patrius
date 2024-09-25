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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class is intended to provide low-level services for the creation, updating, and reading of direct access files
 * utilized by the DAF and DAS systems within SPICE.<br>
 * This class based on the zzddhman.for file in the SPICE library.
 * 
 * @author T0281925
 * 
 * @since 4.11
 */
public final class DafHandleManager {

    /** Boolean indicating whether it is the first time calling a method from the class for initialization purposes. */
    private static boolean first = true;

    /**
     * NEXT stores the next handle to be used for file access. It is incremented just before entries in the file table
     * are made. It begins as zero valued.
     */
    private static int next = 0;

    /** List of access method considered in the SPICE library. */
    private static final String[] METHOD = { "READ", "WRITE", "SCRTCH", "NEW" };

    /** Binary files architectures considered in the SPICE library. */
    private static String[] architecture = { "DAF", "DAS" };

    /** List containing the binary file formats considered in the SPICE library. */
    private static List<String> binaryFileFormat = new ArrayList<>();

    /*
     * The file table consists of a set of arrays which serve as 'columns' of the table. The sets of elements having the
     * same index in the arrays form the 'rows' of the table. Each column contains a particular type of information;
     * each row contains all of the information pertaining to a particular file.
     */

    /** List containing the handles of of each file opened. */
    private static List<Integer> fileTableHandle = new ArrayList<>();

    /** List containing the binary format of each file opened. */
    private static List<String> fileTableBFF = new ArrayList<>();

    /** List containing the architecture of each file opened. */
    private static List<String> fileTableArch = new ArrayList<>();

    /** List containing the File object associated to each file opened. */
    private static List<File> fileList = new ArrayList<>();

    /**
     * Constructor.
     */
    private DafHandleManager() {
        // Nothing to do
    }

    /**
     * Initialize DafHandleManager data.<br>
     * Based on ZZDDHINI routine from the SPICE library.
     */
    private static void init() {
        // Binary file format
        binaryFileFormat.add("BIG-IEEE");
        binaryFileFormat.add("LTL-IEEE");
        binaryFileFormat.add("VAX-GFLT");
        binaryFileFormat.add("VAX-DFLT");

        first = false;
    }

    /**
     * Load a new direct access file.<br>
     * This method is based on the ZZDDHOPN routine in the SPICE library
     * 
     * @param fname
     *        Name of file to be loaded
     * @param accessMethod
     *        Access method used to load the file
     * @param archi
     *        Expected architecture of the file to load
     * @return handle assigned to file
     * @throws IOException
     *         if there is a problem with the file
     * @throws PatriusException
     *         if there is a problem with the SPICE algorithm (including access method)
     */
    public static int openFile(final String fname, final String accessMethod, final String archi)
        throws IOException, PatriusException {

        if (first) {
            init();
        }

        // ZZPLTCHK vu que c'est JAVA, runtime toujouts pareil, rien a check. Il check binary file format.

        // Check if access method is correct
        if (!accessMethod.equals(DafHandleManager.METHOD[0])) {
            throw new PatriusException(PatriusMessages.PDB_WRONG_ACCESS_METHOD, accessMethod);
        }

        // Check if file architecture is correct.
        if (!archi.equals(architecture[0])) {
            throw new PatriusException(PatriusMessages.PDB_WRONG_ARCHITECTURE, archi);
        }

        // Check for a non-blank file name.
        if (fname.isEmpty()) {
            throw new FileNotFoundException();
        }

        // Initialize the value of HANDLE to 0. In the event an error is signaled this invalid value will be returned to
        // the caller for safety.
        int handle = 0;

        // Check to see if the file associated with fname is already in the file table.
        final boolean found = checkFileList(fname);
        if (!found) {
            // The file does not exist yet in the lists. We add it.
            next += 1;
            handle = next;
            fileTableHandle.add(handle);
            fileTableArch.add(archi.toUpperCase(Locale.US));
            final File file = new File(fname);
            fileList.add(file);

            final String bff = getBinaryFileFormat(file, archi.toUpperCase(Locale.US));
            fileTableBFF.add(bff);
        } else {
            handle = getHandle(fname);
        }

        // Check the number of elements must be the same for all three.
        if ((fileList.size() != fileTableHandle.size()) || (fileTableHandle.size() != fileTableBFF.size())
                || (fileTableHandle.size() != fileTableArch.size())) {
            throw new PatriusException(PatriusMessages.PDB_LIST_COMPLETION_FAILED);
        }

        return handle;
    }

    /**
     * Close the file associated with HANDLE.<br>
     * Based on the ZZDDHCLS routine from the SPICE library.
     * 
     * @param handle
     *        File handle associated with the file to close
     * @param arch
     *        Expected architecture of the handle to close
     * @throws PatriusException
     *         if the architecture does not correspond to the one saved
     */
    public static void closeFile(final int handle, final String arch) throws PatriusException {
        if (first) {
            init();
        }
        // Find the file in the handle table.
        final int findex = fileTableHandle.indexOf(Integer.valueOf(handle));
        // Check to see whether we found the handle or not.
        if (findex < 0) {
            return;
        }

        // Before actually closing the file, check the input architecture matches that listed in the file table for this
        // handle. This is to prevent one architecture's code from stepping on another's.
        if (!fileTableArch.get(findex).equals(arch.toUpperCase(Locale.US))) {
            throw new PatriusException(PatriusMessages.PDB_WRONG_ARCHITECTURE, arch.toUpperCase(Locale.US));
        }

        // Remove the elements in the position findex
        fileTableBFF.remove(findex);
        fileList.remove(findex);
        fileTableHandle.remove(findex);
        fileTableArch.remove(findex);
    }

    /**
     * Indicate the file name associated to a handle.<br>
     * Inspired by ZZDDHNFO routine in the SPICE library.
     * 
     * @param handle
     *        File handle assigned to file of interest
     * @return name of the file associated with HANDLE
     */
    public static String getFilename(final int handle) {
        final int findex = fileTableHandle.indexOf(Integer.valueOf(handle));

        if (findex >= 0) {
            return fileList.get(findex).getPath();
        }
        return "";
    }

    /**
     * Indicate if the handle is found in the list.<br>
     * Inspired by ZZDDHNFO routine in the SPICE library.
     * 
     * @param handle
     *        File handle assigned to file of interest
     * @return boolean that indicates if handle was found
     */
    public static boolean getFound(final int handle) {
        return fileTableHandle.contains(Integer.valueOf(handle));
    }

    /**
     * Indicate the binary file format of the file associated to handle.<br>
     * Inspired by ZZDDHNFO routine in the SPICE library.
     * 
     * @param handle
     *        File handle assigned to file of interest
     * @return file's binary file format
     */
    public static String getBinaryFileFormatFromHandle(final int handle) {
        return fileTableBFF.get(fileTableHandle.indexOf(handle));
    }

    /**
     * Indicate the architecture of the file associated to handle.<br>
     * Inspired by ZZDDHNFO routine in the SPICE library.
     * 
     * @param handle
     *        File handle assigned to file of interest
     * @return file's architecture
     */
    public static String getArchitectureFromHandle(final int handle) {
        return fileTableArch.get(fileTableHandle.indexOf(handle));
    }

    /**
     * Retrieve handle associated with filename.
     * 
     * @param fname
     *        Name of a file previously loaded
     * @return the corresponding file handle (0 if it was not found)
     * @throws PatriusException
     *         if the file is not found in the list
     */
    public static int getHandle(final String fname) throws PatriusException {
        if (first) {
            init();
        }

        if (checkFileList(fname)) {
            return fileTableHandle.get(fileList.indexOf(new File(fname)));
        }
        return 0;
    }

    /**
     * Return the File object associated with a handle.<br>
     * Inspired by ZZDDHHLU routine from the SPICE library.
     * 
     * @param handle
     *        Handle associated with the file of interest
     * @return the corresponding File object
     */
    public static File getFile(final int handle) {
        return fileList.get(fileTableHandle.indexOf(Integer.valueOf(handle)));
    }

    /**
     * Convert filename to a handle.<br>
     * Inspired by the ZZDDHF2H routine in the SPICE library.
     * 
     * @param pathToFile
     *        Name of the file to convert to a handle
     * @return boolean indicating if FNAME's HANDLE was found
     * @throws PatriusException
     *         if the file does not exist or it is unreadable
     */
    private static boolean checkFileList(final String pathToFile) throws PatriusException {
        boolean exists = false;

        // Check if the file exists
        final File file = new File(pathToFile);
        exists = file.exists() && file.isFile();

        if (!exists) {
            throw new PatriusException(PatriusMessages.UNABLE_TO_FIND_FILE, pathToFile);
        }

        // If it exists, check if it is in the file list.
        if (!fileList.contains(file)) {
            // Check if we can read
            if (!file.canRead()) {
                throw new PatriusException(PatriusMessages.PDB_FILE_CANT_BE_READ);
            }
            return false;
        }
        return true;
    }

    /**
     * Look for the binary file format indicated in the first file record.<br>
     * Based on the ZZDDHPPF routine in the SPICE library.
     * 
     * @param file
     *        File object attached to the binary file
     * @param arch
     *        File architecture
     * @return binary file format
     * @throws IOException
     *         if there is a problem reading the file
     * @throws PatriusException
     *         if the ftp check fails
     */
    private static String getBinaryFileFormat(final File file, final String arch) throws IOException, PatriusException {

        if (first) {
            init();
        }
        // FTP location boundaries
        final int ftpBegin = 499;
        final int ftpFin = 999;

        // Create random access file:
        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        final String firstRecord = DafReaderTools.readString(raf, 1000);

        final String idword = firstRecord.substring(0, 8);
        // As we always expect DAF/SPK or Exception, we don't need the values returned. TODO extent to other types
        SpiceCommon.idword2architype(idword);

        final boolean ftpchk = SpiceCommon.ftpCheck(firstRecord.substring(ftpBegin, ftpFin));

        if (ftpchk) {
            throw new PatriusException(PatriusMessages.PDB_FTP_FAILED);
        }

        // Now this search is redundant, but the presence of the FTPLFT string in the latter half of the file record is
        // fairly conclusive evidence that this is a "new" binary, and we can expect to locate the binary file format
        // identification string.

        final int ftppos = firstRecord.substring(ftpBegin, ftpFin).indexOf("FTPSTR");

        // Check to see if we found FTPLFT. If so extract the binary file format ID word from the file record.
        if (ftppos >= 0) {
            // Extract BFFIDW from firstRecord. We assume it is a DAF file.
            final String bffidw = firstRecord.substring(88, 88 + 8);

            // See if we can find BFFIDW int the STRBFF list.
            if (!binaryFileFormat.contains(bffidw)) {
                throw new PatriusException(PatriusMessages.PDB_BFF_NOT_SUPPORTED, bffidw);
            }
            return bffidw;
        }

        return "";
    }
}
