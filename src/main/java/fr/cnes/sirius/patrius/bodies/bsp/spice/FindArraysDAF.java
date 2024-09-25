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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class for the search of arrays inside a DAF file.<br>
 * The main function of these methods is to allow the contents of any DAF to be examined on an array-by-array basis.
 * 
 * <p>
 * Conceptually, the arrays in a DAF form a doubly linked list, which can be searched in either of two directions:
 * forward or backward. It is possible to search multiple DAFs simultaneously.
 * </p>
 * <p>
 * Based on various routines of the daffa.for file in the SPICE library.
 * </p>
 * 
 * @author T0281925
 * 
 * @since 4.11
 */
public final class FindArraysDAF {

    /** Boolean indicating if it is the first time calling any of the methods for initialization purposes. */
    private static boolean first = true;

    /** List of handles of the opened DAFs. */
    private static List<Integer> openSetDAF;

    /** List containing the DAF state for each DAF file. */
    private static List<DafState> statePool = new LinkedList<>();

    /** Read Access method. */
    private static final String READACCESS = "READ";

    /** Last word (8 bytes) in a record. */
    private static final int LASTWORD = 127;

    /**
     * Constructor.
     */
    private FindArraysDAF() {
        // Nothing to do
    }

    /**
     * Begin forward search for arrays in a DAF.<br>
     * Based on DAFBFS from the SPICE library.
     * 
     * @param handle
     *        Handle of file to be searched
     * @throws PatriusException
     *         if there is a problem with the SPICE algorithm
     * @throws IOException
     *         if there is a problem reading the first record of the file
     */
    public static void beginForwardSearch(final int handle) throws PatriusException {

        // Check out the file handle before going any further.
        DafHandle.checkHandleAccess(handle, READACCESS);

        // Initialize the state table pool, if this hasn't been done yet.
        // Also initialize the cell used to obtain the set of handles of open DAFs.
        if (first) {
            openSetDAF = new ArrayList<>(SpiceCommon.FILE_TABLE_SIZE);
            first = false;
        }

        // See whether we already have an entry for this DAF in the state table. Find the previous node if possible.
        Iterator<DafState> it = statePool.iterator();
        boolean found = false;
        DafState state = new DafState();
        DafState prev;
        while (it.hasNext() && !found) {
            state = it.next();
            if (state.getHandle() == handle) {
                found = true;
            } else {
                prev = state;
            }
        }

        // At this point, either found is false, or state points to a state table entry describing the DAF indicated by
        // HANDLE.
        // In the latter case, PREV is the predecessor of state.
        if (found) {
            // We already have a dossier on this DAF. We already have the information on the summary format, but we must
            // re-set our summary record pointers and our name record availability flag.
            //
            // Rather than doing the update here, we do it outside of this IF block. That way, the update gets done in
            // just one place.
            // This just makes life easier: if the collection of state variables is changed, there are fewer places to
            // forget to make the required code changes.
            //
            // Move the node for this DAF to the head of the active list, if it is not already there:
            if (!((LinkedList<DafState>) statePool).getFirst().equals(state)) {
                statePool.remove(state);
                ((LinkedList<DafState>) statePool).addFirst(state);
            }
        } else {
            // We don't yet have any information on this DAF. Make a new state table entry for the DAF. We may need to
            // make room for the new information by freeing space allocated to DAFs that are no longer open.

            // All entries in the list that contains a handle not opened is erased.
            openSetDAF = DafHandle.getHandleList();
            it = statePool.iterator();
            while (it.hasNext()) {
                if (!openSetDAF.contains(Integer.valueOf(it.next().getHandle()))) {
                    it.remove();
                }
            }

            // Add a state to the head of the list
            state = new DafState();
            ((LinkedList<DafState>) statePool).addFirst(state);
        }

        // At this point, either the first element in the stateTable contains the information about the current DAF, or
        // we are going to add it.

        // Read the file record and first summary record. Do not read the corresponding name record until necessary. In
        // most searches, names are of no interest.
        final FileRecordDAF fr = new FileRecordDAF(handle);
        final boolean[] fnd = new boolean[1];
        state.setLastSummaryRecord(ReadDoublePrecisionDAF.getContentSummaryRecord(handle, fr.getForward(), 0, LASTWORD,
            fnd));

        if (!fnd[0]) {
            final String dafName = DafHandle.handleToFilenameDAF(state.getHandle());
            throw new PatriusException(PatriusMessages.PDB_DESC_RECORD_FAILED, fr.getForward(), dafName);
        }

        // Set up the state information for this file. Note that we don't have a name record yet, and we have no current
        // array- yet.
        state.setHandle(handle);
        state.setRecnoCurrSummary(fr.getForward());
        state.setBuffered(false);

        // The arrays are returned in forward order within each summary record.
        state.setIndexCurrSummary(0);
    }

    /**
     * Begin a backward search for arrays in a DAF.<br>
     * This method is based on the DAFBBS routine from the SPICE library.
     * 
     * @param handle
     *        Handle of DAF to be searched
     * @throws PatriusException
     *         if there is a problem finding the file or reading it
     * @throws IOException
     *         if there is a problem opening the random access file
     */
    public static void beginBackwardSearch(final int handle) throws PatriusException {
        // Check out the file handle before going any further.
        DafHandle.checkHandleAccess(handle, READACCESS);

        // Initialize the state table pool, if this hasn't been done yet.
        // Also initialize the cell used to obtain the set of handles of open DAFs.
        if (first) {
            openSetDAF = new ArrayList<>(SpiceCommon.FILE_TABLE_SIZE);
            first = false;
        }

        // See whether we already have an entry for this DAF in the state table. Find the previous node if possible.
        Iterator<DafState> it = statePool.iterator();
        boolean found = false;
        DafState state = new DafState();
        DafState prev;
        while (it.hasNext() && !found) {
            state = it.next();
            if (state.getHandle() == handle) {
                found = true;
            } else {
                prev = state;
            }
        }

        // At this point, either found is false, or state points to a state table entry describing the DAF indicated by
        // HANDLE.
        // In the latter case, PREV is the predecessor of state.
        if (found) {
            // We already have a dossier on this DAF. We already have the information on the summary format, but we must
            // re-set our summary record pointers and our name record availability flag.
            //
            // Rather than doing the update here, we do it outside of this IF block. That way, the update gets done in
            // just one place.
            // This just makes life easier: if the collection of state variables is changed, there are fewer places to
            // forget to make the required code changes.
            //
            // Move the node for this DAF to the head of the active list, if it is not already there:
            if (!((LinkedList<DafState>) statePool).getFirst().equals(state)) {
                statePool.remove(state);
                ((LinkedList<DafState>) statePool).addFirst(state);
            }
        } else {
            // We don't yet have any information on this DAF. Make a new state table entry for the DAF. We may need to
            // make room for the new information by freeing space allocated to DAFs that are no longer open.

            // All entries in the list that contains a handle not opened is erased.
            openSetDAF = DafHandle.getHandleList();
            it = statePool.iterator();
            while (it.hasNext()) {
                if (!openSetDAF.contains(Integer.valueOf(it.next().getHandle()))) {
                    it.remove();
                }
            }

            // Add a state to the head of the list
            state = new DafState();
            ((LinkedList<DafState>) statePool).addFirst(state);
        }

        // At this point, either the first element in the stateTable contains the information about the current DAF, or
        // we are going to add it.

        // Read the file record and first summary record. Do not read the corresponding name record until necessary. In
        // most searches, names are of no interest.
        final FileRecordDAF fr = new FileRecordDAF(handle);
        final boolean[] fnd = new boolean[1];
        state.setLastSummaryRecord(ReadDoublePrecisionDAF.getContentSummaryRecord(handle, fr.getBackward(), 0,
            LASTWORD, fnd));

        if (!fnd[0]) {
            final String dafName = DafHandle.handleToFilenameDAF(state.getHandle());
            throw new PatriusException(PatriusMessages.PDB_DESC_RECORD_FAILED, fr.getForward(), dafName);
        }

        // Set up the state information for this file. Note that we don't have a name record yet, and we have no current
        // array yet.
        state.setHandle(handle);
        state.setRecnoCurrSummary(fr.getBackward());
        state.setBuffered(false);

        // The arrays are returned in backward order within each summary record.
        state.setIndexCurrSummary(state.getnSummariesCurrSummaryRecord() + 1); // FIXME check if it is +1
    }

    /**
     * Select a DAF that already has a search in progress as the one to continue searching.<br>
     * Based on the DAFCS routine of the SPICE library
     * 
     * @param handle
     *        Handle of DAF to continue searching
     * @throws PatriusException
     *         if the access mode is not correct or the DAF associated to the handle is not being searched
     */
    public static void selectDaf(final int handle) throws PatriusException {
        // Validate the DAF's handle before going any further. DAFSIH will signal an error if HANDLE doesn't designate
        // an open DAF.
        DafHandle.checkHandleAccess(handle, READACCESS);

        // See whether we already have an entry for this DAF in the state table. Find the previous node if possible.
        DafState state = new DafState();
        boolean fnd = false;
        final Iterator<DafState> it = statePool.iterator();
        while (it.hasNext() && !fnd) {
            state = it.next();
            if (state.getHandle() == handle) {
                fnd = true;
            }
        }

        // Either FND is false, or STATE is the state in the state table of the DAF specified by HANDLE.

        // You can't continue searching a DAF that you're not already searching.
        if (!fnd) {
            throw new PatriusException(PatriusMessages.PDB_NO_DAF_SEARCHED);
        }

        // Move the node for this DAF to the head of the active list, if it is not already there:
        // - Make the predecessor of P point to the successor of P.
        // - Make P point to the head of the active list.
        // - Make P the active list head node.
        final int index = statePool.indexOf(state);
        if (index != 0) {
            state = statePool.remove(index);
            statePool.add(0, state);
        }
    }

    /**
     * Find the next (forward) array in the current DAF.<br>
     * Based on DAFFNA from the SPICE library.
     * 
     * @return if the next array is found
     * @throws PatriusException
     *         if there is a problem in the SPICE algorithm
     */
    public static boolean findNextArray() throws PatriusException {
        // Make sure the search has been stated in this DAF
        if (statePool.isEmpty()) {
            throw new PatriusException(PatriusMessages.PDB_NO_DAF_SEARCHED);
        }

        // Operate on the last DAF in which a search has been started.
        final DafState state = statePool.get(0);

        // Make sure that the 'current' DAF is still open
        DafHandle.checkHandleAccess(state.getHandle(), READACCESS);

        // Now that we know a search is going on, assume that we will find an array until proven otherwise.
        boolean found = true;

        // Either there are more summaries left in this record, or there aren't. If there are, just incrementing the
        // pointer is sufficient. If there aren't, we have to find the next record and point to the first array there.
        // (If that record is empty, or doesn't exist, then there are simply no more arrays to be found.)
        state.setIndexCurrSummary(state.getIndexCurrSummary() + 1);
        if (state.getIndexCurrSummary() > state.getnSummariesCurrSummaryRecord()) {
            if (state.getRecnoNextSummary() == 0) {
                // there are no more arrays in the list
                found = false;

                // Make sure that the array pointer stays pointing to the position following the end of the list.
                // Otherwise, a call to DAFFPA might fail to find the last array in the list.
                state.setIndexCurrSummary(state.getnSummariesCurrSummaryRecord() + 1);

                // The careful reader may note that we're not updating any of the pointers
                // state.recnoCurrSummary
                // state.recnoNextSummary
                // state.recnoPrevSummary
                // These will not be accessed if there is no current array.
                // If the array pointer is backed up again by a call to DAFFPA, the values we have right now will be
                // correct.
            } else {
                final boolean[] fnd = new boolean[1];
                state.setLastSummaryRecord(ReadDoublePrecisionDAF.getContentSummaryRecord(state.getHandle(),
                    state.getRecnoNextSummary(), 0, LASTWORD, fnd));
                if (!fnd[0]) {
                    final String dafName = DafHandle.handleToFilenameDAF(state.getHandle());
                    throw new PatriusException(PatriusMessages.PDB_DESC_RECORD_FAILED, state.getRecnoNextSummary(),
                        dafName);
                }

                // The name (character) record we've saved no longer applies to the current summary record. However,
                // we've just updated the summary record, so the summary record remains valid.
                state.setBuffered(false);
                state.setRecnoCurrSummary(state.getRecnoNextSummary());
                state.setIndexCurrSummary(1);

                found = state.getnSummariesCurrSummaryRecord() > 0;
            }
        }
        return found;
    }

    /**
     * Find the previous (backward) array in the current DAF.<br>
     * This method is based on the DAFFPA routine from the SPICE library.
     * 
     * @return a boolean indicating if an array was found
     * @throws PatriusException
     *         if there is a problem accessing/reading the current file
     */
    public static boolean findPreviousArray() throws PatriusException {
        // Make sure the search has been stated in this DAF
        if (statePool.isEmpty()) {
            throw new PatriusException(PatriusMessages.PDB_NO_DAF_SEARCHED);
        }

        // Operate on the last DAF in which a search has been started.
        final DafState state = statePool.get(0);

        // Make sure that the 'current' DAF is still open
        DafHandle.checkHandleAccess(state.getHandle(), READACCESS);

        // Now that we know a search is going on, assume that we will find an array until proven otherwise.
        boolean found = true;

        // Either there are more summaries left in this record, or there aren't. If there are, just decrementing the
        // pointer is sufficient. If there aren't, we have to find the previous record and point to the last array
        // there. (If that record is empty, or doesn't exist, then there are simply no more arrays to be found.)
        state.setIndexCurrSummary(state.getIndexCurrSummary() - 1);
        if (state.getIndexCurrSummary() <= 0) {
            if (state.getRecnoPrevSummary() == 0) {
                // There is no predecessor of the current array in the list.
                found = false;

                // Make sure that the array pointer stays pointing to the position preceding the front of the list.
                // Otherwise, a call to DAFFNA might fail to find the first array in the list.
                state.setIndexCurrSummary(0);

                // The careful reader may note that we're not updating any of the pointers
                // state.recnoCurrSummary
                // state.recnoNextSummary
                // state.recnoPrevSummary
                // These will not be accessed if there is no current array.
                // If the array pointer is moved forward again by a call to DAFFNA, the values we have right now will be
                // correct.
            } else {
                final boolean[] fnd = new boolean[1];
                state.setLastSummaryRecord(ReadDoublePrecisionDAF.getContentSummaryRecord(state.getHandle(),
                    state.getRecnoPrevSummary(), 0, LASTWORD, fnd));
                if (!fnd[0]) {
                    final String dafName = DafHandle.handleToFilenameDAF(state.getHandle());
                    throw new PatriusException(PatriusMessages.PDB_DESC_RECORD_FAILED, state.getRecnoNextSummary(),
                        dafName);
                }

                // The name (character) record we've saved no longer applies to the current summary record. However,
                // we've just updated the summary record, so the summary record remains valid.
                state.setBuffered(false);
                state.setRecnoCurrSummary(state.getRecnoPrevSummary());
                state.setIndexCurrSummary(state.getnSummariesCurrSummaryRecord());

                found = state.getnSummariesCurrSummaryRecord() > 0;
            }
        }
        return found;
    }

    /**
     * Return (get) the summary for the current array in the current DAF.<br>
     * Based on DAFGS from the SPICE library
     * 
     * @return double precision array with the array summary content
     * @throws PatriusException
     *         if there is a problem while reading the file
     */
    public static double[] getSummaryOfArray() throws PatriusException {
        // Make sure the search has been stated in this DAF
        if (statePool.isEmpty()) {
            throw new PatriusException(PatriusMessages.PDB_NO_DAF_SEARCHED);
        }

        // Operate on the last DAF in which a search has been started.
        final DafState state = ((LinkedList<DafState>) statePool).getFirst();

        // Make sure that the 'current' DAF is still open
        DafHandle.checkHandleAccess(state.getHandle(), READACCESS);

        // Check the current pointer position to make sure that it's in bounds. If there is no current array, then we
        // cannot return a summary. This situation occurs if DAFFNA was called when the current array was the last, or
        // if DAFFPA was called when the current array was the first.
        if (state.getIndexCurrSummary() == 0) {
            throw new PatriusException(PatriusMessages.PDB_FIRST_IS_NEXT);
        } else if (state.getIndexCurrSummary() > state.getnSummariesCurrSummaryRecord()) {
            throw new PatriusException(PatriusMessages.PDB_LAST_IS_PREV);
        }

        // The location of the summary depends on the current pointer position.
        final int[] ndni = DafHandle.getSummaryFormatDAF(state.getHandle());

        final int sumsiz = ndni[0] + (ndni[1] + 1) / 2;
        final int offset = 3 + (state.getIndexCurrSummary() - 1) * sumsiz;

        return Arrays.copyOfRange(state.getLastSummaryRecord(), offset, offset + sumsiz);
    }

    /**
     * Return (get) the name for the current array in the current DAF.<br>
     * Based on DAFGN of the SPICE library.
     * 
     * @return the name for the current array in the current DAF
     * @throws IOException
     *         if there is problem while reading a character record
     * @throws PatriusException
     *         if there is a problem in regarding the SPICE algorithm
     */
    public static String getNameOfArray() throws PatriusException {
        // Make sure the search has been stated in this DAF
        if (statePool.isEmpty()) {
            throw new PatriusException(PatriusMessages.PDB_NO_DAF_SEARCHED);
        }

        // Operate on the last DAF in which a search has been started.
        final DafState state = ((LinkedList<DafState>) statePool).getFirst();

        // Make sure that the 'current' DAF is still open
        DafHandle.checkHandleAccess(state.getHandle(), READACCESS);

        // Check the current pointer position to make sure that it's in bounds. If there is no current array, then we
        // cannot return a summary. This situation occurs if DAFFNA was called when the current array was the last, or
        // if DAFFPA was called when the current array was the first.
        if (state.getIndexCurrSummary() == 0) {
            throw new PatriusException(PatriusMessages.PDB_FIRST_IS_NEXT);
        } else if (state.getIndexCurrSummary() > state.getnSummariesCurrSummaryRecord()) {
            throw new PatriusException(PatriusMessages.PDB_LAST_IS_PREV);
        }

        // Read the name record for this summary record, if we don't have it already.
        if (!state.isBuffered()) {
            state.setLastNameRecord(readCharacterRecordDaf(state.getHandle(), state.getRecnoCurrSummary() + 1));
            state.setBuffered(true);
        }

        // The location of the name depends on the current pointer position.
        final int[] ndni = DafHandle.getSummaryFormatDAF(state.getHandle());

        final int sumsiz = ndni[0] + (ndni[1] + 1) / 2;
        final int namsiz = sumsiz * 8;
        final int offset = (state.getIndexCurrSummary() - 1) * namsiz;

        return state.getLastNameRecord().substring(offset, offset + namsiz);
    }

    /**
     * Read the contents of a character record from a DAF.<br>
     * Based on DAFRCR from the SPICE library.
     * 
     * @param handle
     *        Handle of DAF
     * @param record
     *        Record number of character record
     * @return the Character record
     * @throws IOException
     *         if there is a problem reading the file
     * @throws PatriusException
     *         if the access method is not correct
     */
    public static String readCharacterRecordDaf(final int handle, final int record) throws PatriusException {
        // Check to be sure that HANDLE is attached to a file that is open with read access
        DafHandle.checkHandleAccess(handle, READACCESS);

        // Retreive the file for this handle
        final File file = DafHandleManager.getFile(handle);
        // Initialization
        final RandomAccessFile readFile;
        try {
            readFile = new RandomAccessFile(file, "r");
            readFile.seek(DafReaderTools.nRecord2nByte(record));
            return DafReaderTools.readString(readFile, SpiceCommon.MAX_CHAR_RECORD);
        } catch (final FileNotFoundException e) {
            // File not found
            throw new PatriusException(PatriusMessages.PDB_FILE_NOT_FOUND, e);
        } catch (final IOException e) {
            throw new PatriusException(PatriusMessages.PDB_FILE_CANT_BE_READ, e);
        }
    }
}
