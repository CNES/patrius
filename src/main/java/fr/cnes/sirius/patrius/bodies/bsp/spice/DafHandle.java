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
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class is and adaptation of one part of the 
 * dafah.for file of the SPICE library
 * 
 * This class has as objective to manage DAF files by their 
 * handle. This includes opening, closing and retrieving 
 * information about the summary.
 * 
 * Several files may be opened for use simultaneously. (This makes
 * it convenient to combine data from several files to produce a
 * single result.) As each DAF is opened, it is assigned a file
 * handle, which is used to keep track of the file internally, and
 * which is used by the calling program to refer to the file in all
 * subsequent calls to DAF methods.
 * 
 * Currently DAF files can only be opened for read purposes. Writing
 * in a DAF or creating a DAF file is not implemented.
 * 
 * @author T0281925
 *
 */
public final class DafHandle {     
    /**
     * List containing the handles of the files opened
     */
    private static List<Integer> fileHandleList = new ArrayList<Integer>(SpiceCommon.FILE_TABLE_SIZE);  
    /**
     * List containing the handles in the file table
     */
    private static List<Integer> fileTableHandle = new ArrayList<Integer>(SpiceCommon.FILE_TABLE_SIZE);  
    /**
     * List containing the number of doubles in a summary in the file table
     */
    private static List<Integer> fileTableNDouble = new ArrayList<Integer>(SpiceCommon.FILE_TABLE_SIZE);
    /**
     * List containing the number of integers in a summary in the file table
     */
    private static List<Integer> fileTableNInteger = new ArrayList<Integer>(SpiceCommon.FILE_TABLE_SIZE);
    /**
     * List containing the number of links created for a file (number of times it has been opened
     * and not closed) in the file table
     */
    private static List<Integer> fileTableLink = new ArrayList<Integer>(SpiceCommon.FILE_TABLE_SIZE);
    /**
     * File architecture
     */
    private static final String ARCHI = "DAF";    
    /**
     * Access method
     */
    private static final String READACCESS = "READ";
    /**
     * Constructor
     */
    private DafHandle() {
        // Nothing to do
    }

    /**
     * Open a DAF for subsequent read requests.
     * Based on DAFOPR from the SPICE library
     * 
     * @param filename Name of DAF to be opened.
     * @return Handle assigned to DAF.
     * @throws IOException in case there is a problem opening, reading or closing the file
     * @throws PatriusException in case there is a problem related with the SPICE algorithm
     */
    public static int openReadDAF(final String filename) throws IOException, PatriusException {

        // Attempt to open the file; perform any appropriate checks.
        final int handle = DafHandleManager.openFile(filename, READACCESS, ARCHI);

        // See if this file is already present in the file table. If it
        // is simply increment its link count by one, check out and
        // return.
        final int findex = fileTableHandle.indexOf(Integer.valueOf(handle));

        if (findex >= 0) {
            fileTableLink.set(findex, Integer.valueOf(fileTableLink.get(findex).intValue() + 1));
            return handle;
        }

        // Extract the file record
        final FileRecordDAF fileRecord = new FileRecordDAF(handle);
        if (!fileRecord.isFound()) {
            DafHandleManager.closeFile(handle, ARCHI);
            throw new PatriusException(PatriusMessages.PDB_UNABLE_TO_LOCATE_FILE, handle);
        }
        // Retrieve ND and NI from the file record.
        final int nd = fileRecord.getnDouble();
        final int ni = fileRecord.getnInt();

        // At this point, we know that we have a valid DAF file, and we're
        // set up to read from it, so ...
        // Update the file table to include information about our newly
        // opened DAF.
        fileTableHandle.add(Integer.valueOf(handle));
        fileTableNDouble.add(Integer.valueOf(nd));
        fileTableNInteger.add(Integer.valueOf(ni));
        fileTableLink.add(Integer.valueOf(1));

        // Insert the new handle into our handle set.
        fileHandleList.add(Integer.valueOf(handle));

        return handle;
    }

    /**
     * Close the DAF associated with a given handle.
     * Based on DAFCLS from the SPICE library.
     * 
     * @param handle Handle of DAF to be closed.
     * @throws PatriusException in case there is a problem in the HandleManager closing the file
     */
    public static void closeDAF(final int handle) throws PatriusException {
        // Is this file even open? If so, decrement the number of links
        // to the file. If the number of links drops to zero, physically
        // close the file and remove it from the file buffer.

        // If the file is not open: no harm, no foul.

        final int findex = fileTableHandle.indexOf(Integer.valueOf(handle));

        if (findex >= 0) {
            fileTableLink.set(findex, Integer.valueOf(fileTableLink.get(findex).intValue() - 1));

            if (fileTableLink.get(findex).intValue() == 0) {
                DafHandleManager.closeFile(handle, ARCHI);

                fileTableHandle.remove(findex);
                fileTableNDouble.remove(findex);
                fileTableNInteger.remove(findex);
                fileTableLink.remove(findex);

                // Delete the handle from our handle set.
                fileHandleList.remove(Integer.valueOf(handle));
            }
        }
    }

    /**
     * Return the summary format associated with a handle.
     * Based on DAFHSF from the SPICE library.
     * 
     * @param handle Handle of a DAF file.
     * @return int array containing the number of double precision 
     *         (first element) and integer (second element) components
     *         in summaries
     * @throws PatriusException in case the file wasn't opened beforehand
     */
    public static int[] getSummaryFormatDAF(final int handle) throws PatriusException {
        final int[] ndni = new int[2];

        final int findex = fileTableHandle.indexOf(Integer.valueOf(handle));

        if (findex >= 0) {
            ndni[0] = fileTableNDouble.get(findex).intValue();
            ndni[1] = fileTableNInteger.get(findex).intValue();
        } else {
            throw new PatriusException(PatriusMessages.PDB_DAF_NOT_OPENED, handle);
        }

        return ndni;
    }

    /**
     * Return the name of the file associated with a handle.
     * Based on DAFHFN from the SPICE library.
     * 
     * @param handle Handle of a DAF file.
     * @return Corresponding file name.
     * @throws PatriusException in case the filename in the HandleManager is empty.
     */
    public static String handleToFilenameDAF(final int handle) throws PatriusException {
        final String fname = DafHandleManager.getFilename(handle);

        if (fname.isEmpty()) {
            throw new PatriusException(PatriusMessages.PDB_DAF_NOT_OPENED, handle);
        }

        return fname;
    }
    
    /**
     * Check if a file is already loaded in the DAF Handle structure.
     * 
     * @param file Path to the file we are interested in
     * @param handle (out) contains the handle associated to the file if it was loaded.
     * @return a boolean indicating if the file has already been been loaded.
     * @throws PatriusException If there is a problem retrieving the information.
     */
    public static boolean isLoaded(final String file, final int[] handle) throws PatriusException {
        handle[0] = DafHandleManager.getHandle(file);
        return handle[0] != 0;
    }

    /**
     * Return a SPICE set containing the handles of all currently
     * open DAFS.
     * Based on DAFHOF from the SPICE library.
     * 
     * @return the list of handles associated to opened DAF files 
     */
    public static List<Integer> getHandleList() {
        return fileHandleList;
    }

    /**
     * Signal an error if a DAF file handle does not designate a DAF
     * that is open for a specified type of access.
     * Based on DAFSIH from the SPICE library.
     * 
     * @param handle HANDLE to be validated.
     * @param access String indicating access type.
     * @throws PatriusException in case the access is not correct or the handle not found in the list
     */
    public static void checkHandleAccess(final int handle,
            final String access) throws PatriusException {
        // Make sure we recognize the access type specified by the caller.
        if (!access.equalsIgnoreCase(READACCESS)) {
            throw new PatriusException(PatriusMessages.PDB_WRONG_ACCESS_METHOD);
        }

        final boolean found = DafHandleManager.getFound(handle);

        if (!found || !fileHandleList.contains(Integer.valueOf(handle))) {
            throw new PatriusException(PatriusMessages.PDB_UNABLE_TO_LOCATE_FILE, handle);
        }
    }
}
