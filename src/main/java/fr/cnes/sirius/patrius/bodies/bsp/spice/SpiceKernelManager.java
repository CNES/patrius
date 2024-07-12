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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class goal is to manage the loading and unloading of SPICE kernels from
 * other PATRIUS methods.
 * 
 * It is based on the keeper.for file in the SPICE library
 * 
 * @author T0281925
 *
 */
public final class SpiceKernelManager {

    /**
     * Boolean indicating if it is the first time calling any method of the class
     */
    private static boolean first = true;
    /**
     * Number of a priori known kernel variables
     */
    private static final int NKNOWN = 3;
    /**
     * A priori known kernel variables
     */
    private static String[] known = {"KERNELS_TO_LOAD", "PATH_SYMBOLS", "PATH_VALUES"};
    /**
     * Max. number of files that can be loaded
     */
    private static final int MAXFILES = 5300;
    /**
     * List of kernel loaded
     */
    private static List<SpiceKernelInfo> loadedKernel;
    /**
     * Name of the class (makes the role of agent name for pool)
     */
    private static final String CLASS_NAME = "loadSpiceKernel";

    /**
     * Constructor
     */
    private SpiceKernelManager() {
        // Nothing to do
    }

    /**
     * Load one or more SPICE kernels into a program.
     * Based on the FURNSH routine of the SPICE library
     * 
     * @param file SPICE kernel file (for now, only binary DAF files are allowed).
     * @throws PatriusException if there is any file loading or pool problem
     * @throws IOException if there is any file loading problem
     */
    public static void loadSpiceKernel(final String file) throws PatriusException, IOException {

        boolean update;

        if (first) {
            first = false;
            loadedKernel = new ArrayList<SpiceKernelInfo>(MAXFILES);

            PoolSpice.setWatch(CLASS_NAME, NKNOWN, known);
            update = PoolSpice.checkUpdates(CLASS_NAME);
        }

        // We don't want external interactions with the kernel pool to
        // have any affect on loadSpiceKernel's watch so we check the watcher
        // here prior to the call to ZZLDKER.
        update = PoolSpice.checkUpdates(CLASS_NAME);

        final int myhand = loadFile(file);

        loadedKernel.add(new SpiceKernelInfo(file, SpiceCommon.SPK, 0, myhand));

        update = PoolSpice.checkUpdates(CLASS_NAME);

        if (!update) {
            // Nothing to do. None of the control variables were set in file
            return;
        } else {
            throw new PatriusException(PatriusMessages.PDB_IMPOSSIBLE_TO_GET_HERE_SPICE);        
        }
        // The original code continues. However, for the read of SPK files, 
        // the continuation is unnecessary.
    }

    // KTOTAL
    /**
     * Return the number of kernels of a specified type that are
     * currently loaded via the loadSpiceKernel interface.
     * Based on the KTOTAL routine from the SPICE library
     * 
     * @param type a kernel type (actually only SPK is allowed. add other types)
     * @return he number of kernels of type TYPE
     */
    public static int totalNumberOfKernel(final String type) {
        if (loadedKernel == null) {
            return 0;
        }
        
        int nTotal = 0;
        final Iterator<SpiceKernelInfo> it = loadedKernel.iterator();
        while (it.hasNext()) {
            if (it.next().getType().equals(type)) {
                nTotal++;
            }
        }

        return nTotal;
    }

    /**
     * Clear the SpiceKernelManager subsystem: unload all kernels, clear the kernel
     * pool, and re-initialize the subsystem. Existing watches on kernel
     * variables are retained.
     * Based on the KCLEAR routine of the SPICE library
     * 
     * @throws PatriusException if there is a problem unloading a file
     */
    public static void clearAllKernels() throws PatriusException {
        if (loadedKernel == null) {
            return;
        }
        // Unloading all kernels is actually much less work than
        // unloading just a few of them. We unload all of the
        // binary kernels via the "unload" routines for their
        // respective subsystems, then clear the kernel pool.
        final Iterator<SpiceKernelInfo> it = loadedKernel.iterator();
        while (it.hasNext()) {
            final SpiceKernelInfo ski = it.next();
            if (ski.getType().equals(SpiceCommon.SPK)) {
                SpkFile.unloadSpkFile(ski.getHandle());
            }
        }

        PoolSpice.clpool();

        // Although it's not strictly necessary, we initialize
        // SpiceKernelManager's database arrays. This step may occasionally
        // be helpful for debugging.
        loadedKernel.clear();

        // Calling CLPOOL doesn't remove watches, but it does send a message
        // to each agent indicating that its variables have been touched.
        // Clear this indication by calling PoolSpice.checkUpdates. (This is done for
        // safety; the current implementation of loadSpiceKernel doesn't require it.)
        PoolSpice.checkUpdates(CLASS_NAME);
    }

    /**
     * This interface allows you to unload binary kernels.
     * 
     * The usual usage of loadSpiceKernel is to load each file needed by your
     * program exactly one time. However, it is possible to load a
     * kernel more than one time. The effect of unloading a kernel that has 
     * been loaded more than once is to "undo" the last loading of the kernel.
     * 
     * This method is based on the UNLOAD method from the SPICE library
     * 
     * @param file path of the file to be unload from the system
     * @throws IOException if there is any problem unloading or loading a file
     * @throws PatriusException if there is any problem unloading or loading a file
     */
    public static void unloadKernel(final String file) throws IOException, PatriusException {
        // First locate the file we need to unload, we search backward
        // through the list of loaded files so that we unload in the right
        // order.
        boolean gotit = false;
        final ListIterator<SpiceKernelInfo> it = loadedKernel.listIterator(loadedKernel.size());
        SpiceKernelInfo ski = new SpiceKernelInfo(SpiceCommon.EMPTY, SpiceCommon.EMPTY, 0, 0);
        while (it.hasPrevious() && !gotit) {
            ski = it.previous();
            if (ski.getFile().equals(file)) {
                gotit = true;
            }
        }

        // If we didn't locate the requested file, there is nothing to do.
        if (!gotit) {
            return;
        }

        final boolean single;
        if (SpiceCommon.SPK.equals(ski.getType())) {
            // Count the occurrences of the file in the database.
            // Stop if we reach two occurrences.
            int nmult = 0;
            
            final Iterator<SpiceKernelInfo> it2 = loadedKernel.iterator();
            SpiceKernelInfo ski2;
            while (it2.hasNext() && nmult < 2) {
                ski2 = it2.next();
                if (ski.getHandle() == ski2.getHandle()) {
                    // To be safe, make sure we're not looking at
                    // a text kernel with a random, matching handle
                    // value.
                    if (!ski2.getType().equals("TEXT") && !ski2.getType().equals("META")) {
                        nmult++;
                    }
                }
            }
            single = nmult == 1;

            if (single) {
                SpkFile.unloadSpkFile(ski.getHandle());
            }
        }

        // Remove the kernel found in the first iterator from our local database.
        final int idx = loadedKernel.indexOf(ski);
        it.remove();

        // Update any source pointers affected by the deletion of the Ith
        // database entry.       
        while (it.hasNext()) {
            ski = it.next();
            if (ski.getSource() > idx){
                // This pointer is affected by the deletion of the Ith
                // database entry.
                throw new PatriusException(PatriusMessages.PDB_IMPOSSIBLE_TO_GET_HERE_SPICE);
            }
        }

        // If any SPK files were unloaded, we need to reload everything
        // to establish the right priority sequence for segments.
        final Iterator<SpiceKernelInfo> it3 = loadedKernel.iterator();
        SpiceKernelInfo ski3;
        while (it3.hasNext()) {
            ski3 = it3.next();
            if (ski3.getType().equals(SpiceCommon.SPK)) {
                ski3.setHandle(SpkFile.loadSpkFile(ski3.getFile()));
            }
        }
    }

    /**
     * Determine the architecture and type of a file and load
     * the file into the appropriate SPICE subsystem
     * (For the moment, only DAF SPK files are allowed)
     * 
     * Based on the ZZLDKER routine of the SPICE library
     * 
     * @param pathToFile The name of a file to be loaded.
     * @return The handle associated with the loaded kernel.
     * @throws PatriusException if there is a problem finding the file, determining its arch/type or loading it
     * @throws IOException if there is a problem determining its arch/type or loading it
     */
    private static int loadFile(final String pathToFile) throws PatriusException, IOException {

        final File file = new File(pathToFile);

        if (!(file.exists() && file.isFile())) {
            throw new PatriusException(PatriusMessages.UNABLE_TO_FIND_FILE, pathToFile);
        }

        getArchAndType(file);

        return SpkFile.loadSpkFile(pathToFile);

    }

    /**
     * Determine the architecture and type of SPICE kernels.
     * Based on the GETFAT routine of the SPICE library.
     * @param file The name of a file to be examined.
     * @throws PatriusException if there is a problem managing the file or the type is not SPK 
     *         extend to other types
     * @throws IOException if there is a problem managing the file (open/read/close)
     */
    static void getArchAndType(final File file) throws PatriusException, IOException {
        // Check if we find the file already stored
        int handle = DafHandleManager.getHandle(file.getPath());

        final boolean found = handle == 0 ? false : true;
        String arch = SpiceCommon.EMPTY;
        
        if (found) {
            arch = DafHandleManager.getArchitectureFromHandle(handle);
        } else {
            if (!(file.exists() && file.isFile())) {
                throw new PatriusException(PatriusMessages.UNABLE_TO_FIND_FILE, file.getPath());
            }
            // If the file is not already open (probably the case that
            // happens most frequently) we try opening it for direct access
            // and see if we can locate the idword.
        }
        
        // Initialize the temporary storage variables that we use.
        String idword = SpiceCommon.EMPTY;        
        final RandomAccessFile readFile;

        readFile = new RandomAccessFile(file, "r");

        // We opened the file successfully, so let's try to read from the file.
        final String firstRecord = DafReaderTools.readString(readFile, SpiceCommon.RECORD_LENGTH);

        // Close the file (if we opened it here), as we do not need it to be open any more.
        readFile.close();

        // We will now replace any non printing ASCII characters with blanks.
        final int firstAscii = 32;
        final int lastAscii = 126;
        final char[] ch = firstRecord.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            if (ch[i] < firstAscii || ch[i] > lastAscii) {
                ch[i] = ' ';
            }
        }
        idword = new String(Arrays.copyOfRange(ch, 0, SpiceCommon.BYTES_DOUBLE));

        final String[] archtyp = SpiceCommon.idword2architype(idword);
        arch = archtyp[0];
        if (!arch.trim().equals(SpiceCommon.DAF)) {
            throw new PatriusException(PatriusMessages.PDB_WRONG_ARCHITECTURE, arch);
        }
        String type = archtyp[1];

        // If the architecture is DAF and the type is unknown, '?', then we
        // have either an SPK file, a CK file, or something we don't
        // understand. Let's check it out.
        if (type.trim().equals(SpiceCommon.UNKNOWN)) {
            // We have a DAF file and we do not know what the type is. This
            // situation can occur for older SPK and CK files, before the ID
            // word was used to store type information.
            //
            // We use Bill's (WLT'S) magic heuristics to determine the type
            // of the file.
            //
            // Open the file and pass the handle to the private routine
            // that deals with the dirty work.
            handle = DafHandle.openReadDAF(file.getPath());
            type = ckOrSpk(handle);
            DafHandle.closeDAF(handle);
        }

        if (!type.trim().equals(SpiceCommon.SPK)) {
            throw new PatriusException(PatriusMessages.PDB_WRONG_TYPE, type);
        }
    }

    /**
     * This routine determines whether or not a DAF file attached to
     * the supplied handle is an SPK, CK or unknown file.
     * @param handle the handle of a DAF file open for read access.
     * @return the type of the DAF file (SPK,CK or ?)
     * @throws PatriusException if there is a problem reading the file
     * @throws IOException if there is a problem managing the file
     */
    private static String ckOrSpk(final int handle) throws PatriusException, IOException {

        
     
     
        // These parameters give the number of integer and double precision
        // components of the descriptor for SPK and CK files.
        final int nd = 2;
        final int ni = 6;

        // Make sure the values of ND and NI associated with this file have the correct values.
        final int[] thisndni = DafHandle.getSummaryFormatDAF(handle);

        if (thisndni[0] != nd || thisndni[1] != ni) {
            return SpiceCommon.UNKNOWN;
        }

        // We've got the correct values for ND and NI, examine the descriptor
        // for the first array.
        FindArraysDAF.beginForwardSearch(handle);
        final boolean found = FindArraysDAF.findNextArray();

        // If we don't find any segments, we don't have a clue about the file type.
        if (!found) {
            return SpiceCommon.UNKNOWN;
        }

        // Unpack the summary record.
        final double[] sum = FindArraysDAF.getSummaryOfArray();
        final double[] dc = new double[nd];
        final int[] ic = new int[ni];
        SpiceCommon.unpackSummary(sum, nd, ni, dc, ic);
  
        // The following parameters point to the various slots in the
        // integer portion of the DAF descriptor where the values are
        // located.
        final int bodid = 0;
        final int cenid = bodid + 1;
        final int spkfrm = cenid + 1;
        final int spktyp = spkfrm + 1;
        // Look at the slot where the angular velocity flag would
        // be located if this is a CK file.
        final String cktyp = "CK";
        final int angvel = ic[spktyp];
        

        // Test 1. The value of ANGVEL may do the trick
        // right at the start.
        if (angvel == 0) {
            return cktyp;
        } else if (angvel > 1) {
            return SpiceCommon.SPK;
        }

        // Test 2. If this is an SPK file, it has a type 01 segment.
        // See if this is something orbiting the solar system
        // barycenter.
        throw new PatriusException(PatriusMessages.PDB_IMPOSSIBLE_TO_GET_HERE_SPICE);
        // At this moment only type 2 and type 3 SPK files are allowed
    }
}
