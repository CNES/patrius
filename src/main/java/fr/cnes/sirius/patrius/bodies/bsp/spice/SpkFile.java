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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MethodLength check
//CHECKSTYLE: stop CyclomaticComplexity check
//CHECKSTYLE: stop NestedBlockDepth check
//Reason: Spice code kept as such

/**
 * Load and unload files for use by the readers. Buffer segments for readers.
 * <p>
 * Before a file can be read by the S/P-kernel readers, it must be loaded by loadSpkFile, which among other things,
 * loads the file into the DAF system.
 * </p>
 * <p>
 * Up to MAX_FILES files may be loaded for use simultaneously, and a file only has to be loaded once to become a
 * potential search target for any number of subsequent reads.
 * </p>
 * <p>
 * Once an SPK file has been loaded, it is assigned a file handle, which is used to keep track of the file internally,
 * and which is used by the calling program to refer to the file in all subsequent calls to SPK routines.
 * </p>
 * <p>
 * A file may be removed from the list of files for potential searching by unloading it via a call to unloadSpkFile.
 * </p>
 * <p>
 * SPKSFS performs the search for segments within a file for the S/P-kernel readers. It searches through last-loaded
 * files first. Within a single file, it searches through last-inserted segments first, thus assuming that
 * "newest data is best".
 * </p>
 * <p>
 * Information on loaded files is used by SPKSFS to manage a buffer of saved segment descriptors and identifiers to
 * speed up access time without having to necessarily perform file reads.
 * <p>
 * This class is based on the SPKBSR.for file of the SPICE library.
 * </p>
 * 
 * @author T0281925
 *
 * @since 4.11
 */
public final class SpkFile {

    /** Number of double precision components in a summary. */
    public static final int ND = 2;

    /** Number of integer components in a summary. */
    public static final int NI = 6;

    /** The maximum number of bodies whose segments can be buffered by SPKSFS. */
    private static final int MAX_BODIES = 10000;

    /**
     * The maximum number of ephemeris files that can be loaded by loadSpkFile at any given time for use by the readers.
     */
    private static final int MAX_FILES = 5000;

    /** NEXT is incremented whenever a new file is loaded to give the file number of the file. */
    private static int next;

    /**
     * Doubly linked list containing for each body, the segment list that corresponds to the body among other
     * informations.
     */
    private static List<SpkBody> segmentTablePool = new LinkedList<>();

    /** Contains the handle of each file that has been loaded for use with the SPK readers. */
    private static List<Integer> fileTableHandle = new ArrayList<>(MAX_FILES);

    /**
     * Contains the file number of each file that has been loaded for use with the SPK readers. File numbers begin at
     * one, and are incremented until they reach a value of INTMAX() - 1, at which point they are mapped to the range
     * 1:NFT, where NFT is the number of loaded SPK files.<br>
     * (A file number is similar to a file handle, but it is assigned and used exclusively by this module. The purpose
     * of file numbers is to keep track of the order in which files are loaded and the order in which they are
     * searched.)
     */
    private static List<Integer> fileTableNumFile = new ArrayList<>(MAX_FILES);

    /**
     * Constructor.
     */
    private SpkFile() {
        // Nothing to do
    }

    /**
     * Load an ephemeris file for use by the readers. Return that file's handle, to be used by other SPK routines to
     * refer to the file.
     * <p>
     * This method is based on the SPKLEF routine of the SPICE library.
     * </p>
     * 
     * @param file
     *        Name of the file to be loaded
     * @return the loaded file's handle
     * @throws IOException
     *         if there is problem opening the file
     * @throws PatriusException
     *         if there is a problem opening or closing the file
     */
    public static int loadSpkFile(final String file) throws IOException, PatriusException {
        // Any time we load a file, there is a possibility that the re-use intervals are invalid because they're been
        // superseded by higher-priority data. Since we're not going to examine the loaded file, simply indicate that
        // all of the re-use intervals are invalid.

        for (int i = 0; i < segmentTablePool.size(); i++) {
            segmentTablePool.get(i).setCheckPrevious(false);
        }

        // Nothing works unless at least one file has been loaded, so this is as good a place as any to initialize the
        // segment table pool. We want to avoid unnecessary initializations, so we only initialize the list when no
        // files are loaded. It's quite possible to have files loaded and an empty body table, so we don't want to
        // re-initialize just because there are no body table entries.

        // if (fileTableHandle.size() == 0){
        // segmentTablePool = new LinkedList<SpkBody>();
        // }

        // To load a new file, first try to open it for reading
        final int handle = DafHandle.openReadDAF(file);

        // Determine if the file is already in the table.
        final int findex = fileTableHandle.indexOf(Integer.valueOf(handle));

        if (findex >= 0) {
            // The last call we made to DAFOPR added another DAF link to the SPK file. Remove this link.
            DafHandle.closeDAF(handle);

            // Remove the file from the file table and remove its segments from the segment table. If the segment list
            // for a body becomes empty, remove that body from the body table.
            fileTableHandle.remove(findex);
            fileTableNumFile.remove(findex);

            final Iterator<SpkBody> it = segmentTablePool.iterator();
            while (it.hasNext()) {
                final SpkBody bd = it.next();
                final LinkedList<SpkSegment> segment = (LinkedList<SpkSegment>) bd.getSegmentTable();

                final Iterator<SpkSegment> its = segment.iterator();
                while (its.hasNext()) {
                    if (its.next().getHandle() == handle) {
                        // The segment came from the file we're unloading.
                        // Delete the node from the segment list for body I;
                        its.remove();
                    }
                }
                // If the list for this body is now empty, shorten the current table by one: delete the body.
                if (segment.isEmpty()) {
                    it.remove();
                }
            }
        }

        // Determine the next file number. Note that later code assumes that the file number can be incremented by 1, so
        // we can't allow the file number to reach INTMAX().
        if (next < Integer.MAX_VALUE - 1) {
            next++;
        } else {
            throw new PatriusException(PatriusMessages.PDB_TOO_MANY_FILES);
        }

        fileTableHandle.add(Integer.valueOf(handle));
        fileTableNumFile.add(Integer.valueOf(next));

        return handle;
    }

    /**
     * Unload an ephemeris file so that it will no longer be searched by the readers.
     * <p>
     * Based on the SPKUEF routine of the SPICE library
     * </p>
     * 
     * @param handle
     *        Handle of file to be unloaded
     * @throws PatriusException
     *         if there is problem closing the file
     */
    public static void unloadSpkFile(final int handle) throws PatriusException {
        // All of the stored segments from the file must be removed from the segment table (by returning the
        // corresponding nodes to the segment table pool.)
        //
        // Don't do anything if the given handle is not in the file table.
        final int findex = fileTableHandle.indexOf(Integer.valueOf(handle));

        if (findex < 0) {
            return;
        }

        // First get rid of the entry in the file table. Close the file before wiping out the handle.
        DafHandle.closeDAF(fileTableHandle.get(findex).intValue());

        fileTableHandle.remove(findex);
        fileTableNumFile.remove(findex);

        // Check each body list individually. Note that the first node on each list, having no predecessor, must be
        // handled specially.
        final Iterator<SpkBody> it = segmentTablePool.iterator();
        SpkBody sb;
        while (it.hasNext()) {
            sb = it.next();
            final Iterator<SpkSegment> its = sb.getSegmentTable().iterator();
            while (its.hasNext()) {
                if (its.next().getHandle() == handle) {
                    its.remove();
                }
            }

            if (sb.getSegmentTable().size() == 0) {
                it.remove();
            }
            if (sb.isCheckPrevious() && (sb.getPreviousHandle() == handle)) {
                sb.setCheckPrevious(false);
            }
        }
    }

    /**
     * Search through loaded SPK files to find the highest-priority segment applicable to the body and time specified
     * and buffer searched segments in the process, to attempt to avoid re-reading files.
     * <p>
     * Based on the SPKSFS routine from the SPICE library
     * </p>
     * 
     * @param body
     *        Body ID
     * @param et
     *        Ephemeris time
     * @param handle
     *        Handle of file containing the applicable segment
     * @param descr
     *        Descriptor of the applicable segment
     * @param ident
     *        Identifier of the applicable segment
     * @return a boolean indicating whether or not a segment was found
     * @throws PatriusException
     *         if there is a problem while searching in the file
     * @throws IOException
     *         if there is a problem while beginning a segment search
     */
    public static boolean searchSegment(final int body, final double et, final int[] handle, final double[] descr,
                                        final String[] ident) throws PatriusException {

        // Buffering segments involves maintaining three tables: the file table, the body table, and the segment table.
        // The routine is broken down into various tasks, described below, which perform these manipulations. A
        // description of the components of each table is provided in the declarations section of SPKBSR.

        // There must be at least ONE file loaded
        if (fileTableHandle.isEmpty()) {
            throw new PatriusException(PatriusMessages.PDB_ANY_FILE_LOADED);
        }

        // In the following loop, we will try to simplify things by doing exactly one thing on each pass through the
        // loop. After each pass, the status of the loop (STATUS) will be adjusted to reflect the next thing that needs
        // to be done. Occasionally, the current task will have to be interrupted until another task can be carried out.
        // (For example, when collecting new segments, an interrupt might place a segment at the front or end of the
        // current body list; when placing the segment on the list, a second interrupt might free up room in the segment
        // table in order to allow the addition to proceed.) In this case, the current task will be saved and restored
        // after the more urgent task has been completed.
        //
        // The loop can terminate in only one of two ways (unless an error occurs). First, if an applicable segment is
        // found in the segment table, the handle, descriptor, and identifier for the segment are returned immediately.
        // Second, if the table does not contain an applicable segment, and if no files remain to be searched, the loop
        // terminates normally, and no data are returned.
        //
        // The individual tasks are described below.
        //
        final String newBody = "NEW BODY";
        //
        // This indicates that the specified body has no segments stored for it at all. It must be added to the body
        // table. (This is followed immediately by an OLD FILES search, in which every file loaded is considered an old
        // file.)
        //
        final String unknown = "?";
        //
        // This indicates that the next task is not immediately apparent: if new files exist, they should be searched;
        // otherwise the list should be checked.
        //

        // Is the body already in the body table? This determines what the first task should be.
        String status = "";

        SpkBody currentBody = new SpkBody(body);

        final int bindex = segmentTablePool.indexOf(currentBody);

        if (bindex < 0) {
            status = newBody;
        } else {
            currentBody = segmentTablePool.get(bindex);
            // Much of the time, the segment used to satisfy the previous request for a given body will also satisfy the
            // current request for data for that body. Check whether this is the case.
            if (currentBody.isCheckPrevious()) {
                // The previous segment found for the current body is a viable candidate for the current request. See
                // whether the input ET value falls into the re-use interval for this body: the time interval for which
                // the previously returned segment for this body provides the highest-priority coverage.
                //
                // We treat the re-use interval as topologically open because one or both endpoints may belong to
                // higher-priority segments.
                if ((et > currentBody.getLowerBound()) && (et < currentBody.getUpperBound())) {
                    // The request time is covered by the segment found on the previous request for data for the current
                    // body, and this interval is not masked by any higher-priority segments. The previous segment for
                    // this body satisfies the request.
                    handle[0] = currentBody.getPreviousHandle();
                    ident[0] = currentBody.getPreviousSegmentId();
                    System.arraycopy(currentBody.getPreviousDescriptor(), 0, descr, 0, SpkBody.SIZEDESC);
                    return true;
                }

                // Adjust the expense here. If the expense of the list contains a component due to the cost of finding
                // the unbuffered segment providing data for re-use, subtract that component from the expense.
                currentBody.setExpense(currentBody.getExpense() - currentBody.getReuseExpense());
                currentBody.setReuseExpense(0);

                // The re-use interval becomes invalid if it didn't satisfy the request. The validity flag gets re-set
                // below.
                //
                // At this point, the previous segment is not a candidate to satisfy the request---at least not until
                // we've verified that
                // - The previous segment is still available
                // - The previous segment hasn't been superseded by a more recently loaded segment
                currentBody.setCheckPrevious(false);
            }

            // If the segment list for this body is empty, make sure the expense is reset to 0.
            if (currentBody.getSegmentTable().isEmpty()) {
                currentBody.setExpense(0);
            }

            status = unknown;
        }

        // The rest of the possible tasks are :
        final String newFiles = "NEW FILES";

        // This indicates that at least one new file has been added since the last time the segment list for the
        // specified body was searched. Find the oldest of these new files, and begin a NEW SEGMENTS search in forward
        // order for segments to add to the front of the list.
        final String newSegments = "NEW SEGMENTS";

        // Continue a NEW FILES search, adding segments for the specified body to the front of the list.
        final String oldFiles = "OLD FILES";

        // This indicates that although the list has been searched and found to contain no applicable segment, some of
        // the older files remain to be searched. Find the newest of these old files, and begin an OLD SEGMENTS search
        // in backward order.
        final String oldSegments = "OLD SEGMENTS";

        // Continue an OLD FILES search, adding segments for the specified body to the end of the list.
        final String checkList = "CHECK LIST";

        // This indicates that the list is ready to be searched, either because no new files have been added, or because
        // segments from a new file or an old file have recently been added.
        //
        // The list is never checked until all new files have been searched.
        //
        // If an applicable segment is found, it is returned.
        //
        // "MAKE ROOM" (Interrupt)
        //
        // This indicates that one of the bodies must be removed, along with its stored segments, to make room for
        // another body or segment. The body (other than the one being searched for) with the smallest expense is
        // selected for this honor.
        //
        final String addToFront = "ADD TO FRONT"; // (Interrupt)

        // This indicates that a segment has been found (during the course of a NEW FILES search) and must be added to
        // the front of the list.
        final String addToEnd = "ADD TO END"; // (Interrupt)

        // This indicates that a segment has been found (during the course of an OLD FILES search) and must be added to
        // the end of the list.
        final String suspend = "SUSPEND";

        // This indicates that the current task (DOING) should be interrupted until a more urgent task (URGENT) can be
        // carried out. The current task is placed on a stack for safekeeping.
        final String resume = "RESUME";

        // This indicates that the most recently interrupted task should be resumed immediately.
        final String hopeless = "HOPELESS";
        // Other variables
        String doing = "";
        String urgent = "";
        int cost = 0;
        int findex = 0;
        // The stack of suspended tasks is empty
        int top = 0;
        final String[] stack = new String[2];

        // Real search of the desired segment
        while (!hopeless.equals(status)) {
            // If new files have been added, they have to be searched.
            // Otherwise, we can go right to the list of stored segments.
            if (unknown.equals(status)) {
                // There are two ways to get to this point.
                //
                // 1) Status may have been set to '?' prior to the loop DO WHILE ( STATUS .NE. HOPELESS ).
                //
                // 2) Status was set to '?' by the NEW SEGMENTS block of code as the result of finishing the read of a
                // new file.
                if (currentBody.getHighestFile() < fileTableNumFile.get(fileTableNumFile.size() - 1).intValue()) {
                    status = newFiles;
                } else {
                    status = checkList;
                }
            } else if (newBody.equals(status)) {
                // New bodies are added to the end of the body table. If the table is full, one of the current occupants
                // must be removed to make room for the new one.
                //
                // Setting LFS to one more than the highest current file number means the OLD FILES SEARCH that follows
                // will begin with the last-loaded file.
                //
                // There is one way to get here:
                //
                // 1) The variable STATUS was set to NEW BODY prior to the loop DO WHILE ( STATUS .NE. HOPELESS ).
                //
                // Find the cheapest slot in the body table to store the initial information about this body.
                //
                // NOTE: This used to be handled by the MAKE ROOM section.
                // However, trying to handle this special case there was just more trouble than it was worth.
                //
                // NOTE: In Java , the ArrayList cannot be full, we will just add the new body to the table
                currentBody = new SpkBody(body, 0, fileTableNumFile.get(fileTableNumFile.size() - 1).intValue(),
                    fileTableNumFile.get(fileTableNumFile.size() - 1).intValue() + 1, Double.MIN_VALUE,
                    Double.MAX_VALUE, new double[5], "", 0, false, 0);

                segmentTablePool.add(currentBody);

                // Now search the loaded SPK files for segments relating to this body. We start with the last-loaded
                // files and work backwards.
                status = oldFiles;
            } else if (newFiles.equals(status)) {
                // When new files exist, they should be searched in forward order, beginning with the oldest new file
                // not yet searched.
                // All new files must be searched before the list can be checked, to ensure that the best (newest)
                // segments are being used.
                //
                // Begin a forward search, and prepare to look for individual segments from the file.
                //
                // The only way to get here is to have STATUS set to the value NEW FILES in the STATUS .EQ. '?' block of
                // the IF structure.
                //
                // Find the next file to search; set FINDEX to the corresponding file table entry.
                findex = 0;
                while (currentBody.getHighestFile() >= fileTableNumFile.get(findex).intValue()) {
                    findex++;
                }

                currentBody.setHighestFile(fileTableNumFile.get(findex).intValue());

                FindArraysDAF.beginForwardSearch(fileTableHandle.get(findex).intValue());

                status = newSegments;

                // The cost of the list contributed by th new file is zero so far
                cost = 0;
            } else if (newSegments.equals(status)) {
                // New files are searched in forward order. Segments, when found, are inserted at the front of the list.
                // Invisible segments (alpha > omega) are ignored.
                //
                // Each segment examined, whether applicable or not, adds to the expense of the list.
                //
                // The only way to get here is from the NEW FILES block of the IF structure.
                final boolean fnd = FindArraysDAF.findNextArray();

                if (!fnd) {
                    // We're out of segments in the current file. Decide whether we need to examine another new file, or
                    // whether we're ready to check the list.
                    status = unknown;
                    currentBody.setExpense(currentBody.getExpense() + cost);
                } else {
                    System.arraycopy(FindArraysDAF.getSummaryOfArray(), 0, descr, 0, SpkBody.SIZEDESC);
                    final double[] dcd = new double[ND];
                    final int[] icd = new int[NI];
                    SpiceCommon.unpackSummary(descr, ND, NI, dcd, icd);

                    if ((icd[0] == body) && (dcd[0] <= dcd[1])) {
                        doing = newSegments;
                        urgent = addToFront;
                        status = suspend;
                    }

                    cost++;
                }

                // If we haven't reset the status, we'll return for another 'NEW SEGMENTS' pass

            } else if (oldFiles.equals(status)) {
                // When old files must be searched (because the segments in the list are inadequate), they should be
                // searched in backward order, beginning with the newest old file not yet searched. The segment list
                // will be re-checked after each file is searched. If a match is found, the search terminates, so some
                // old files may not be searched.
                //
                // Search from the end, and prepare to look for individual segments from the file.
                //
                // You can get to this block in two ways.
                //
                // 1) We can have a NEW BODY
                //
                // 2) We have checked the current list (CHECK LIST) for this body, didn't find an applicable segment and
                // have some files left that have not been searched.
                findex = fileTableNumFile.size() - 1;
                while (currentBody.getLowestFile() <= fileTableNumFile.get(findex).intValue()) {
                    findex--;
                }

                FindArraysDAF.beginBackwardSearch(fileTableHandle.get(findex).intValue());

                status = oldSegments;

                // The next thing we'll do is search through all the segments of this file for those that applicable to
                // this body.
                // The cost of the list contributed by the current file is zero so far.
                cost = 0;
            } else if (oldSegments.equals(status)) {
                // Old files are searched in backward order. Segments, when found, are inserted at the end of the list.
                // Invisible segments (alpha > omega) are ignored.
                //
                // Each segment examined, whether applicable or not, adds to the expense of the list.
                //
                // There is only one way to get here---from the block 'OLD FILES'. Note we do not add to the expense of
                // the list for this body until we've completely searched this file.
                final boolean fnd = FindArraysDAF.findPreviousArray();

                if (!fnd) {
                    // We've been through all of the segments in this file.
                    // Change the lowest file searched indicator for this body to be the current file, and go check the
                    // current list.
                    currentBody.setLowestFile(fileTableNumFile.get(findex).intValue());
                    currentBody.setExpense(currentBody.getExpense() + cost);
                    status = checkList;
                } else {
                    System.arraycopy(FindArraysDAF.getSummaryOfArray(), 0, descr, 0, SpkBody.SIZEDESC);
                    final double[] dcd = new double[ND];
                    final int[] icd = new int[NI];
                    SpiceCommon.unpackSummary(descr, ND, NI, dcd, icd);

                    if ((icd[0] == body) && (dcd[0] <= dcd[1])) {
                        doing = oldSegments;
                        urgent = addToEnd;
                        status = suspend;
                    }

                    cost++;
                }
                // If we haven't reset the status, we'll return for another 'OLD SEGMENTS' pass.
            } else if (checkList.equals(status)) {
                // Okay, all the new files (and maybe an old file or two) have been searched. Time to look at the list
                // of segments stored for the body to see if one applicable to the specified epoch is hiding in there.
                // If so, return it. If not, try another old file. If there are no more old files, give up the ghost.
                //
                // There are two ways to get to this point.
                //
                // 1) From the '?' block.
                // 2) From the 'OLD SEGMENTS' block.
                //
                // For every segment examined, initialize the re-use interval associated with the current body.
                currentBody.setLowerBound(Double.MIN_VALUE);
                currentBody.setUpperBound(Double.MAX_VALUE);
                final Iterator<SpkSegment> sgmntList = currentBody.getSegmentTable().iterator();

                while (sgmntList.hasNext()) {
                    final SpkSegment sgmnt = sgmntList.next();
                    if (et > sgmnt.getDescription()[1]) {
                        // ET is to the right of the coverage interval of this segment
                        currentBody.setLowerBound(MathLib.max(currentBody.getLowerBound(),
                            sgmnt.getDescription()[1]));
                    } else if (et < sgmnt.getDescription()[0]) {
                        // ET is to the left of the coverage interval of this segment
                        currentBody.setUpperBound(MathLib.min(currentBody.getUpperBound(),
                            sgmnt.getDescription()[0]));
                    } else {
                        // The segment coverage interval includes ET
                        System.arraycopy(sgmnt.getDescription(), 0, descr, 0, SpkBody.SIZEDESC);
                        ident[0] = sgmnt.getId();
                        handle[0] = sgmnt.getHandle();

                        // Set the re-use interval for the current body
                        currentBody.setLowerBound(MathLib.max(currentBody.getLowerBound(), sgmnt.getDescription()[0]));
                        currentBody.setUpperBound(MathLib.min(currentBody.getUpperBound(), sgmnt.getDescription()[1]));

                        // Save the returned output items, in case this segment may satisfy the next request
                        currentBody.setPreviousHandle(handle[0]);
                        currentBody.setPreviousSegmentId(ident[0]);
                        currentBody.setPreviousDescriptor(descr);
                        currentBody.setCheckPrevious(true);

                        return true;
                    }
                }

                // If we're still here we didn't have information for this body in the segment list.
                //
                // If there are more files, search them.
                // Otherwise, things are hopeless, set the status that way.
                if (currentBody.getLowestFile() > fileTableNumFile.get(0).intValue()) {
                    status = oldFiles;
                } else {
                    status = hopeless;
                }
                // The status "MAKE ROOM" is not included because it is not necessary in Java
            } else if (addToFront.equals(status)) {
                // The current segment information should be linked in at the head of the segment list for the current
                // body, and the pertinent body table entry should point to the new head of the list.
                //
                // The only way to get here is from the block NEW SEGMENTS after suspending that task.
                final SpkSegment spks = new SpkSegment(fileTableHandle.get(findex).intValue(),
                    Arrays.copyOf(descr, SpkBody.SIZEDESC),
                    FindArraysDAF.getNameOfArray());
                ((LinkedList<SpkSegment>) currentBody.getSegmentTable()).addFirst(spks);
                status = resume;
            } else if (addToEnd.equals(status)) {
                // The current segment information should be linked in at the tail of the segment list for the current
                // body.
                //
                // The only way to get to this task is from the OLD SEGMENTS block after suspending that task.
                final SpkSegment spks = new SpkSegment(fileTableHandle.get(findex).intValue(), Arrays.copyOf(descr,
                    SpkBody.SIZEDESC), FindArraysDAF.getNameOfArray());
                ((LinkedList<SpkSegment>) currentBody.getSegmentTable()).addLast(spks);
                status = resume;
                // SEARCH W/O BUFF is not do because the only way to get here is the MAKE ROOM status (not done)
            } else if (suspend.equals(status)) {
                // When a task is suspended, the current activity is placed on a stack, to be restored later. Two levels
                // are provided, since some interrupts can be interrupted by others.
                top++;
                stack[top] = doing;
                status = urgent;
            } else if (resume.equals(status)) {
                // Pop the status stack
                status = stack[top];
                top--;
            }
        }
        // If we didn't find a segment, don't attempt to use saved outputs from a previous call. BINDEX will always be
        // set at this point. Also clear the re-use interval's expense.
        if (bindex >= 0) {
            currentBody.setCheckPrevious(false);
            currentBody.setReuseExpense(0);
        }

        return false;
    }

    // CHECKSTYLE: resume MethodLength check
    // CHECKSTYLE: resume CyclomaticComplexity check
    // CHECKSTYLE: resume NestedBlockDepth check
}
