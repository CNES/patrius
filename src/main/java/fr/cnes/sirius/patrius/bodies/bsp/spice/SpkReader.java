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
import java.util.Arrays;
import java.util.Set;

import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.util.ArrayUtils;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class contains the methods linked to the reading of SPK files.<br>
 * It is build as an adaptation of a variety of routines from the SPICE library.
 *
 * @author T0281925
 *
 * @since 4.11
 */
public final class SpkReader {

    // Global variables only useful for getStateTelativeToBody method.
    /** Boolean indicating if it is the first time calling this.getStateTelativeToBody. */
    private static boolean first = true;

    /** Counter Array to check updates in the target body. */
    private static CounterArray svctr1;

    /** Saved target body name. */
    private static final String[] SV_TARGET_NAME = new String[1];

    /** Saved target body code. */
    private static final int[] SV_TARGET_ID = new int[1];

    /** Saved target body found boolean. */
    private static final boolean[] FND1 = new boolean[1];

    /** Counter Array to check updates in the observer body. */
    private static CounterArray svctr2;

    /** Saved observer body name. */
    private static final String[] SV_OBS_NAME = new String[1];

    /** Saved observer body code. */
    private static final int[] SV_OBS_ID = new int[1];

    /** Saved observer body found boolean. */
    private static final boolean[] FND2 = new boolean[1];

    // Global vairables useful for getGeometricStateTelativeToBodyFromId method
    /** Boolean indicating if it is the first time calling this.getGeometricStateRelativeToBodyFromID. */
    private static boolean first1 = true;

    /** Counter Array to check updates in the reference frame. */
    private static CounterArray svctr3;

    /** Saved reference frame name. */
    private static final String[] SV_REF = new String[1];

    /** Saved reference frame code. */
    private static final int[] SV_REF_ID = new int[1];

    /** Length of the chain of bodies/states. */
    private static final int CHLEN = 20;

    /** Solar system barycenter code. */
    private static final int SOLAR_SYSTEM_BARYCENTER = 0;

    /** User type CounterArray string. */
    private static final String USER = "USER";

    /**
     * Constructor.
     */
    private SpkReader() {
        // Nothing to do
    }

    /**
     * Find the set of ID codes of all objects in a specified SPK file.
     * <p>
     * This method is based on the SPKOBJ routine of the SPICE library.
     * </p>
     * 
     * @param file
     *        the name of an SPK file
     * @param ids w
     *        An initialized SPICE set data structure. IDS optionally may contain a set of ID codes on input; on output,
     *        the data already present in IDS will be combined with ID code set found for the file SPKFNM. The elements
     *        of sets are unique; hence each ID code in IDS appears only once, even if the SPK file contains multiple
     *        segments for that ID code.
     * @throws IOException
     *         if there is a problem managing the file
     * @throws PatriusException
     *         if there is problem managing or reading the file
     */
    public static void spkObjects(final String file, final Set<Integer> ids) throws PatriusException, IOException {
        // Variables needed for the summary reading
        final int nd = 2;
        final int ni = 6;
        final double[] dc = new double[nd];
        final int[] ic = new int[ni];

        // See whether SpiceKernelManager.getArchAndType thinks we've got an SPK file.
        // If it not the case, it will throw an exception.
        SpiceKernelManager.getArchAndType(new File(file));

        // Open the file for reading
        final int handle = DafHandle.openReadDAF(file);

        // We will examine each segment descriptor in the file, and we'll update our ID code set according to the data
        // found in these descriptors.

        // Start a forward search
        FindArraysDAF.beginForwardSearch(handle);

        // Find the next DAF array
        boolean found = FindArraysDAF.findNextArray();

        while (found) {
            // Fetch and unpack the segment descriptor
            final double[] descriptor = FindArraysDAF.getSummaryOfArray();
            SpiceCommon.unpackSummary(descriptor, nd, ni, dc, ic);
            // Insert the current ID code into the output set.
            // The insertion algorithm will handle duplicates; no special action is required here.
            ids.add(Integer.valueOf(ic[0]));

            found = FindArraysDAF.findNextArray();
        }

        // Release the file
        DafHandle.closeDAF(handle);
    }

    /**
     * Return the state (position and velocity) of a target body relative to an observing body. Never corrected for
     * light time (planetary aberration) or stellar aberration.
     * <p>
     * This is based on the SPKEZR routine of the SPICE library.
     * </p>
     * 
     * @param target
     *        The name of a target body. Optionally, you may supply the integer ID code for the object as an integer
     *        string. For example both 'MOON' and '301' are legitimate strings that indicate the moon is the target
     *        body.<br>
     *        The target and observer define a state vector whose position component points from the observer to the
     *        target.
     * @param epoch
     *        The ephemeris time, expressed as seconds past J2000 TDB, at which the state of the target body relative to
     *        the observer is to be computed. EPOCH refers to time at the observer's location.
     * @param ref
     *        The name of the reference frame relative to which the output state vector should be expressed. This may be
     *        any frame supported by the SPICE system.
     * @param obs
     *        The name of an observing body. Optionally, you may supply the ID code of the object as an integer string.
     *        For example, both 'EARTH' and '399' are legitimate strings to supply to indicate the observer is Earth.
     * @param lightTime
     *        The one-way light time between the observer and target in seconds.
     * @return a Cartesian state vector representing the position and velocity of the target body relative to the
     *         specified observer. It is expressed with respect to the reference frame specified by REF. The first three
     *         components of STATE represent the x-, y- and z-components of the target's position; the last three
     *         components form the corresponding velocity vector.<br>
     *         The position component of STATE points from the observer's location at EPOCH to the location of the
     *         target.<br>
     *         The velocity component of STATE is the derivative with respect to time of the position component of
     *         STATE.<br>
     *         Units are always km and km/sec.
     * @throws PatriusException
     *         if there is a counter, frame retrieving or state calculation problem
     * @throws IOException
     *         if there is a problem while calculating the state.
     */
    @SuppressWarnings("PMD.PrematureDeclaration")
    // Reason: false positive
    public static double[] getStateRelativeToBody(final String target, final double epoch, final String ref,
                                                  final String obs, final double[] lightTime) throws PatriusException {
        // Initialization
        if (first) {
            // Initialize counters
            svctr1 = new CounterArray(USER);
            svctr2 = new CounterArray(USER);

            first = false;
        }

        // Starting from translation of target name to its code
        final boolean[] found = new boolean[1];
        final int targetID = SpiceBody.bodyString2CodeBypass(svctr1, SV_TARGET_NAME, SV_TARGET_ID, FND1, target, found);
        if (!found[0]) {
            throw new PatriusException(PatriusMessages.PDB_EPHEMERIS_OBJ_NOT_RECON, target);
        }

        // Now do the same for the observer
        final int obsId = SpiceBody.bodyString2CodeBypass(svctr2, SV_OBS_NAME, SV_OBS_ID, FND2, obs, found);
        if (!found[0]) {
            throw new PatriusException(PatriusMessages.PDB_EPHEMERIS_OBJ_NOT_RECON, obs);
        }

        // After all translations are done, we can call SPKEZ (that can only call getGeometricStateRelativeToBodyFromID
        // in this version of SPICE).
        return getGeometricStateRelativeToBodyFromID(targetID, epoch, ref, obsId, lightTime);
    }

    /**
     * Computes the geometric state, T(t), of the target body and the geometric state, O(t), of the observing body
     * relative to the first common center of motion. Subtracting O(t) from T(t) gives the geometric state of the target
     * body relative to the observer.
     *
     * <pre>
     *    CENTER ----- O(t)
     *        |      /
     *        |     /
     *        |    /
     *        |   /  T(t) - O(t)
     *        |  /
     *       T(t)
     * </pre>
     *
     * The one-way light time, tau, is given by
     *
     * <pre>
     *
     *           | T(t) - O(t) |
     *    tau = -----------------
     *                  c
     * </pre>
     * <p>
     * Based on the SPKGEO routine of the SPICE library.
     * </p>
     * 
     * @param target
     *        The standard NAIF ID code for a target body
     * @param epoch
     *        The epoch (ephemeris time) at which the state of the target body is to be computed
     * @param ref
     *        The name of the reference frame to which the vectors returned by the routine should be rotated
     * @param obs
     *        The standard NAIF ID code for an observing body
     * @param lightTime
     *        (out) The one-way light time in seconds from the observing body to the geometric position of the target
     *        body at the specified epoch
     * @return 6-dimensional vector that contains the geometric position and velocity of the target body, relative to
     *         the observing body, at epoch ET. STATE has six elements: the first three contain the target's position;
     *         the last three contain the target's velocity. These vectors are transformed into the specified reference
     *         frame. Units are always km and km/sec.
     * @throws PatriusException
     *         if there is a problem getting the frames, getting the state or rotating vectors
     * @throws IOException
     *         if there is a problem while reading the file.
     */
    //CHECKSTYLE: stop CyclomaticComplexity check
    //CHECKSTYLE: stop MethodLength check
    //Reason: Spice code kept as such
    @SuppressWarnings("PMD.PrematureDeclaration")
    // Reason: false positive
    private static double[] getGeometricStateRelativeToBodyFromID(final int target, final double epoch,
                                                                  final String ref, final int obs,
                                                                  final double[] lightTime) throws PatriusException {
        //CHECKSTYLE: resume CyclomaticComplexity check
        //CHECKSTYLE: resume MethodLength check

        // Initialization
        if (first1) {
            // Initialize counters
            svctr3 = new CounterArray(USER);

            first1 = false;
        }

        final double[] state = new double[6];

        // We take care of the obvious case first. It TARG and OBS are the same we can just fill in zero.
        if (target == obs) {
            lightTime[0] = 0;

            Arrays.fill(state, 0.0);
            return state;
        }

        // Since the upgrade to use counter bypass SpiceFrame.frameName2IdBypass- became more efficient in looking up
        // frame IDs than SpiceChangeFrame.intertialRefFrameNumber. So the original order of calls
        // "SpiceChangeFrame.intertialRefFrameNumber first, SpiceFrame.frameName2IdBypass second" was switched to
        // "SpiceFrame.frameName2IdBypass first, SpiceChangeFrame.intertialRefFrameNumber second".
        //
        // Note that in the case of SpiceFrame.frameName2IdBypass's failure to resolve name and
        // SpiceChangeFrame.intertialRefFrameNumber's success to do so, the code returned by
        // SpiceChangeFrame.intertialRefFrameNumber for 'DEFAULT' frame is *not* copied to the saved code SVREFI (which
        // would be set to 0 by SpiceFrame.frameName2IdBypass) to make sure that on subsequent calls
        // SpiceFrame.frameName2IdBypass does not do a bypass (as SVREFI always forced look up) and calls
        // SpiceChangeFrame.intertialRefFrameNumber again to reset the 'DEFAULT's frame ID should it change between the
        // calls.

        int refId = SpiceFrame.frameName2IdBypass(svctr3, SV_REF, SV_REF_ID, ref);

        if (refId == 0) {
            refId = SpiceChangeFrame.intertialRefFrameNumber(ref);
        }

        if (refId == 0) {
            throw new PatriusException(PatriusMessages.PDB_FRAME_NOT_RECOGNISED, ref);
        }

        // Fill in CTARG and STARG until no more data is found or until we reach the SSB. If the chain gets too long to
        // fit in CTARG, that is if I equals CHLEN, then overwrite the last elements of CTARG and STARG.

        // Note the check for FAILED in the loop. If SpiceFrame.frameName2IdBypass or getStateRelativeToCenterOfMotion
        // happens to fail during execution, and the current error handling action is to NOT abort, then FOUND may be
        // stuck at TRUE, CTARG(I) will never become zero, and the loop will execute indefinitely.

        // CTARG contains the integer codes of the bodies in the target body chain, beginning with TARG itself and then
        // the successive centers of motion.
        final int[] ctarg = new int[CHLEN];

        // STARG(1,I) is the state of the target body relative to CTARG(I). The id-code of the frame of this state is
        // stored in TFRAME(I).
        final double[][] starg = new double[CHLEN][SpiceChangeFrame.STATE_LENGTH];
        final int[] tframe = new int[CHLEN];

        // First, we construct CTARG and STARG. CTARG(1) is just the target itself, and STARG(1,1) is just a zero
        // vector, that is, the state of the target relative to itself.
        //
        // Then we follow the chain, filling up CTARG and STARG as we go. We use SpkFile.searchSegment to search through
        // loaded files to find the first segment applicable to CTARG(1) and time EPOCH. Then we use
        // getStateRelativeToCenterOfMotion to compute the state of the body CTARG(1) at EPOCH in the segment that was
        // found and get its center and frame of motion (CTARG(2) and TFRAME(2).
        //
        // We repeat the process for CTARG(2) and so on, until there is no data found for some CTARG(I) or until we
        // reach the SOLAR_SYSTEM_BARYCENTER.
        //
        // Next, we find centers and states in a similar manner for the observer. It's a similar construction as
        // described above, but I is always 1. COBS and SOBS are overwritten with each new center and state, beginning
        // at OBS. However, we stop when we encounter a common center of motion, that is when COBS is equal to CTARG(I)
        // for some I.
        //
        // Finally, we compute the desired state of the target relative to the observer by subtracting the state of the
        // observing body relative to the common node from the state of the target body relative to the common node.
        //

        // Construct CTARG and STARG. Begin by assigning the first elements: TARGET and the state of TARGET relative to
        // itself.
        int i = 0;
        ctarg[i] = target;
        boolean found = true;
        Arrays.fill(starg[i], 0);

        final int[] handle = new int[1];
        final double[] descr = new double[SpkBody.SIZEDESC];
        final String[] ident = new String[1];

        while (found &&
                (i < CHLEN - 1) &&
                (ctarg[i] != obs) &&
                (ctarg[i] != SOLAR_SYSTEM_BARYCENTER)) {
            // Find a file and segment that has state data for CTARG(I)
            found = SpkFile.searchSegment(ctarg[i], epoch, handle, descr, ident);

            if (found) {
                // Get the state of CTARG(I) relative to some center of motion. This new center goes in CTARG(I+1) and
                // the state is called STEMP.
                i++;
                final int[] tempFrame = new int[1];
                final int[] tempCenter = new int[1];
                starg[i] = getStateRelativeToCenterOfMotion(handle[0], descr, epoch, tempFrame, tempCenter);
                tframe[i] = tempFrame[0];
                ctarg[i] = tempCenter[0];

                // Here's what we have. STARG is the state of CTARG(I-1)
                // relative to CTARG(I) in reference frame TFRAME(I)
            }
        }

        tframe[0] = tframe[1];

        // If the loop above ended because we ran out of room in the arrays CTARG and STARG, then we continue finding
        // states but we overwrite the last elements of CTARG and STARG.
        //
        // If, as a result, the first common node is overwritten, we'll just have to settle for the last common node.
        // This will cause a small loss of precision, but it's better than other alternatives.

        if (i == CHLEN - 1) {
            while (found &&
                    ctarg[CHLEN - 1] != SOLAR_SYSTEM_BARYCENTER &&
                    ctarg[CHLEN - 1] != obs) {

                // Find a file and segment that has state data for CTARG(CHLEN).
                found = SpkFile.searchSegment(ctarg[CHLEN - 1], epoch, handle, descr, ident);

                if (found) {
                    // Get the state of CTARG(CHLEN) relative to some center of motion. The new center overwrites the
                    // old. The state is called STEMP.
                    final int[] tmpfrm = new int[1];
                    final int[] tempCenter = new int[1];
                    final double[] stemp = getStateRelativeToCenterOfMotion(handle[0],
                        descr, epoch, tmpfrm, tempCenter);
                    ctarg[CHLEN - 1] = tempCenter[0];

                    // Add STEMP to the state of TARG relative to the old center to get the state of TARG relative to
                    // the new center. Overwrite the last element of STARG.
                    double[] vtemp = new double[6];
                    if (tframe[CHLEN - 1] == tmpfrm[0]) {
                        vtemp = Arrays.copyOf(starg[CHLEN - 1], SpiceChangeFrame.STATE_LENGTH);
                    } else if (SpiceFrame.isInertial(tmpfrm[0]) && SpiceFrame.isInertial(tframe[CHLEN - 1])) {
                        final Array2DRowRealMatrix rot = SpiceChangeFrame.frameRotationMatrix(tframe[CHLEN - 1],
                            tmpfrm[0]);
                        System.arraycopy(rot.operate(Arrays.copyOfRange(starg[CHLEN - 1], 0, 3)), 0, vtemp, 0, 3);
                        System.arraycopy(rot.operate(Arrays.copyOfRange(starg[CHLEN - 1], 3, 6)), 0, vtemp, 3, 3);
                    } else {
                        throw new PatriusException(PatriusMessages.PDB_NOT_INERTIAL_FRAME_SPICE, tmpfrm[0],
                            tframe[CHLEN - 1]);
                    }

                    // We add vtemp to stemp to stock it in starg[chlen-1]
                    for (int j = 0; j < SpiceChangeFrame.STATE_LENGTH; j++) {
                        starg[CHLEN - 1][j] = vtemp[j] + stemp[j];
                    }

                    tframe[CHLEN - 1] = tmpfrm[0];
                }
            }
        }

        // COBS and SOBS will contain the centers and states of the observing body. (They are single elements instead of
        // arrays because we only need the current center and state of the observer relative to it.)
        int cobs;
        double[] sobs = new double[SpiceChangeFrame.STATE_LENGTH];

        final int nct = i;

        /**
         * NCT is the number of elements in CTARG, the chain length. We have in hand the following information
         * 
         * <pre>
         *          STARG(1...6,K)  state of body
         *          CTARG(K-1)      relative to body CTARG(K) in the frame
         *          TFRAME(K)
         *         
         *          For K = 2,..., NCT.
         *          CTARG(1) = TARG
         *          STARG(1...6,1) = ( 0, 0, 0, 0, 0, 0 )
         *          TFRAME(1)      = TFRAME(2)
         * </pre>
         */

        // Now follow the observer's chain. Assign the first values for COBS and SOBS.
        cobs = obs;
        Arrays.fill(sobs, 0);

        // CTPOS is the position in CTARG of the common node.
        int ctpos;

        // Perhaps we have a common node already.
        // If so it will be the last node on the list CTARG.
        //
        // We let CTPOS will be the position of the common node in CTARG if one is found. It will be zero if COBS is not
        // found in CTARG
        int cframe = 0;
        if (ctarg[nct] == cobs) {
            ctpos = nct;
            cframe = tframe[ctpos];
        } else {
            ctpos = -1;
        }

        // Repeat the same loop as above, but each time we encounter a new center of motion, check to see if it is a
        // common node. (When CTPOS is not zero, CTARG(CTPOS) is the first common node.)
        //
        // Note that we don't need a centers array nor a states array, just a single center and state is sufficient ---
        // we just keep overwriting them. When the common node is found, we have everything we need in that one center
        // (COBS) and state (SOBS-state of the target relative to COBS).
        found = true;
        boolean nofrm = true;
        int legs = 0;
        double[] stemp = new double[SpiceChangeFrame.STATE_LENGTH];

        while (found && cobs != SOLAR_SYSTEM_BARYCENTER && ctpos == -1) {
            // Find a file and segment that has state data for COBS
            found = SpkFile.searchSegment(cobs, epoch, handle, descr, ident);

            if (found) {
                // Get the state of COBS; call it STEMP. The center of motion of COBS becomes the new COBS.
                final int[] tmpfrm = new int[1];
                final int[] tempCenter = new int[1];

                if (legs == 0) {
                    sobs = getStateRelativeToCenterOfMotion(handle[0], descr, epoch, tmpfrm, tempCenter);
                } else {
                    stemp = getStateRelativeToCenterOfMotion(handle[0], descr, epoch, tmpfrm, tempCenter);
                }

                cobs = tempCenter[0];

                if (nofrm) {
                    nofrm = false;
                    cframe = tmpfrm[0];
                }

                // Add STEMP to the state of OBS relative to the old COBS to get the state of OBS relative to the new
                // COBS.
                final double[] vtemp = new double[6];
                if (cframe == tmpfrm[0]) {
                    // On the first leg of the state of the observer, we don't have to add anything, the state of the
                    // observer is already in SOBS. We only have to add when the number of legs in the observer state is
                    // one or greater.
                    if (legs > 0) {
                        for (int j = 0; j < SpiceChangeFrame.STATE_LENGTH; j++) {
                            sobs[j] += stemp[j];
                        }
                    }
                } else if (SpiceFrame.isInertial(tmpfrm[0]) && SpiceFrame.isInertial(cframe)) {
                    final Array2DRowRealMatrix rot = SpiceChangeFrame.frameRotationMatrix(cframe, tmpfrm[0]);
                    System.arraycopy(rot.operate(Arrays.copyOfRange(sobs, 0, 3)), 0, vtemp, 0, 3);
                    System.arraycopy(rot.operate(Arrays.copyOfRange(sobs, 3, 6)), 0, vtemp, 3, 3);
                    for (int j = 0; j < SpiceChangeFrame.STATE_LENGTH; j++) {
                        sobs[j] = vtemp[j] + stemp[j];
                    }
                    cframe = tmpfrm[0];

                } else {
                    throw new PatriusException(PatriusMessages.PDB_NOT_INERTIAL_FRAME_SPICE, tmpfrm[0], cframe);
                }

                // We now have one more leg of the path for OBS. Set LEGS to reflect this. Then see if the new center is
                // a common node. If not, repeat the loop.
                legs += 1;
                ctpos = ArrayUtils.getArrayIndex(ctarg, cobs);
            }
        }

        // If CTPOS is -1 at this point, it means we have not found a common node though we have searched through all
        // the available data.
        if (ctpos == -1) {
            String tname = SpiceBody.bodyCode2Name(target);
            final String openP = " (";
            final String cloesP = ")";

            if (tname.isEmpty()) {
                tname = String.valueOf(target);
            } else {
                tname = (new StringBuffer()).append(target).append(openP).append(tname).append(cloesP).toString();
            }

            String oname = SpiceBody.bodyCode2Name(obs);

            if (oname.isEmpty()) {
                oname = String.valueOf(obs);
            } else {
                oname = (new StringBuffer()).append(obs).append(openP).append(oname).append(cloesP).toString();
            }

            throw new PatriusException(PatriusMessages.PDB_INSUFFICIENT_DATA_FOR_STATE, tname, oname,
                String.valueOf(epoch));
        }

        /**
         * If CTPOS is not negative, then we have reached a common node, specifically,
         * 
         * <pre>
         * CTARG(CTPOS) = COBS = CENTER
         * </pre>
         * 
         * (in diagram below). The STATE of the target (TARG) relative to the observer (OBS) is just
         * 
         * <pre>
         *             STARG(1,CTPOS) - SOBS.
         * </pre>
         * 
         * <pre>
         *                          SOBS
         *              CENTER ---------------->OBS
         *                 |                  .
         *                 |                .
         *              S  |              .   E
         *              T  |            .   T
         *              A  |          .   A
         *              R  |        .   T
         *              G  |      .   S
         *                 |    .
         *                 |  .
         *                 V L
         *                TARG
         * </pre>
         * 
         * And the light-time between them is just:
         * 
         * <pre>
         *                    | STATE |
         *               LT = ---------
         *                        c
         * </pre>
         */

        // Compute the state of the target relative to CTARG(CTPOS)
        if (ctpos == 0) {
            tframe[0] = cframe;
        }

        for (int j = 1; j <= ctpos - 1; j++) {
            if (tframe[j] == tframe[j + 1]) {
                for (int k = 0; k < SpiceChangeFrame.STATE_LENGTH; k++) {
                    starg[j + 1][k] += starg[j][k];
                }
            } else if (SpiceFrame.isInertial(tframe[j]) && SpiceFrame.isInertial(tframe[j + 1])) {
                final Array2DRowRealMatrix rot = SpiceChangeFrame.frameRotationMatrix(tframe[j], tframe[j + 1]);
                System.arraycopy(rot.operate(Arrays.copyOfRange(starg[j], 0, 3)), 0, stemp, 0, 3);
                System.arraycopy(rot.operate(Arrays.copyOfRange(starg[j + 1], 3, 6)), 0, stemp, 3, 3);
                for (int k = 0; k < SpiceChangeFrame.STATE_LENGTH; k++) {
                    starg[j + 1][k] += stemp[k];
                }
            } else {
                throw new PatriusException(PatriusMessages.PDB_NOT_INERTIAL_FRAME_SPICE, tframe[j], tframe[j + 1]);
            }
        }

        // To avoid unnecessary frame transformations we'll do a bit of extra decision making here. It's a lot
        // faster to make logical checks than it is to compute frame transformations.
        if (tframe[ctpos] == cframe) {
            for (int j = 0; j < SpiceChangeFrame.STATE_LENGTH; j++) {
                state[j] = starg[ctpos][j] - sobs[j];
            }
        } else if (tframe[ctpos] == refId) {
            // If the last frame associated with the target is already in the requested output frame, we convert the
            // state of the observer to that frame and then subtract the state of the observer from the state of the
            // target.
            if (SpiceFrame.isInertial(cframe) && SpiceFrame.isInertial(refId)) {
                final Array2DRowRealMatrix rot = SpiceChangeFrame.frameRotationMatrix(cframe, refId);
                System.arraycopy(rot.operate(Arrays.copyOfRange(sobs, 0, 3)), 0, stemp, 0, 3);
                System.arraycopy(rot.operate(Arrays.copyOfRange(sobs, 3, 6)), 0, stemp, 3, 3);
            } else {
                throw new PatriusException(PatriusMessages.PDB_NOT_INERTIAL_FRAME_SPICE, cframe, refId);
            }

            // We've now transformed SOBS into the requested reference frame. Set CFRAME to reflect this.
            cframe = refId;
            for (int j = 0; j < SpiceChangeFrame.STATE_LENGTH; j++) {
                state[j] = starg[ctpos][j] - stemp[j];
            }
        } else if (SpiceFrame.isInertial(tframe[ctpos]) && SpiceFrame.isInertial(cframe)) {
            // If both frames are inertial we use SpiceChangeFrame.SpiceChangeFrame instead of
            // SpiceChangeFrame.stateTransformationMatrix to get things into a common frame.
            final Array2DRowRealMatrix rot = SpiceChangeFrame.frameRotationMatrix(tframe[ctpos], cframe);
            System.arraycopy(rot.operate(Arrays.copyOfRange(starg[ctpos], 0, 3)), 0, stemp, 0, 3);
            System.arraycopy(rot.operate(Arrays.copyOfRange(starg[ctpos], 3, 6)), 0, stemp, 3, 3);
            for (int j = 0; j < SpiceChangeFrame.STATE_LENGTH; j++) {
                state[j] = stemp[j] - sobs[j];
            }
        } else {
            throw new PatriusException(PatriusMessages.PDB_NOT_INERTIAL_FRAME_SPICE, tframe[ctpos], cframe);
        }

        // Finally, rotate as needed into the requested frame
        if (cframe != refId) {
            if (SpiceFrame.isInertial(cframe) && SpiceFrame.isInertial(refId)) {
                final Array2DRowRealMatrix rot = SpiceChangeFrame.frameRotationMatrix(cframe, refId);
                System.arraycopy(rot.operate(Arrays.copyOfRange(state, 0, 3)), 0, stemp, 0, 3);
                System.arraycopy(rot.operate(Arrays.copyOfRange(state, 3, 6)), 0, stemp, 3, 3);
                System.arraycopy(stemp, 0, state, 0, SpiceChangeFrame.STATE_LENGTH);
            } else {
                throw new PatriusException(PatriusMessages.PDB_NOT_INERTIAL_FRAME_SPICE, cframe, refId);
            }
        }

        lightTime[0] = (new ArrayRealVector(state, 0, 3)).getNorm() / (Constants.SPEED_OF_LIGHT / Constants.KM_TO_M);
        return state;
    }

    /**
     * Return, for a specified SPK segment and time, the state (position and velocity) of the segment's target body
     * relative to its center of motion.
     * <p>
     * This method is based on the SPKPVN of the SPICE library. <:p>
     * 
     * @param handle
     *        File handle
     * @param descr
     *        Segment descriptor
     * @param epoch
     *        Target epoch
     * @param ref
     *        (out) Target reference frame
     * @param center
     *        (out) Center of state
     * @return a 6-array containing position and velocity
     * @throws PatriusException
     *         if there is a problem reading or evaluating the segment
     */
    public static double[] getStateRelativeToCenterOfMotion(final int handle, final double[] descr, final double epoch,
                                                            final int[] ref, final int[] center)
        throws PatriusException {
        final double[] record;
        final double[] dc = new double[SpkFile.ND];
        final int[] ic = new int[SpkFile.NI];
        final int type;
        final double[] state;

        // Unpacking the segment descriptor will tell us the center, reference frame, and data type for this segment.

        SpiceCommon.unpackSummary(descr, SpkFile.ND, SpkFile.NI, dc, ic);

        center[0] = ic[1];
        ref[0] = ic[2];
        type = ic[3];

        // Each data type has a pair of routines to read and evaluate records for that data type. These routines are the
        // only ones that actually look inside the segments.
        //
        // We will only treat type 2 and type 3 here.
        switch (type) {
            case 2:
                record = SpkRecord.readType2(handle, descr, epoch);
                state = SpkRecord.evaluateType2(epoch, record);
                break;
            case 3:
                record = SpkRecord.readType3(handle, descr, epoch);
                state = SpkRecord.evaluateType3(epoch, record);
                break;
            default:
                throw new PatriusException(PatriusMessages.PDB_SPK_TYPE_NOT_SUPPORTED, type);
        }

        return state;
    }
}
