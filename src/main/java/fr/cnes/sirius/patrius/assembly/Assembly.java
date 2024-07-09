/**
 * 
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * @history creation 9/02/2012
 * 
 * HISTORY
 * VERSION:4.5:FA:FA-2467:27/05/2020:Amelioration des performances dans la classe Assembly
* VERSION:4.4:DM:DM-2148:04/10/2019:[PATRIUS] Creations de parties mobiles dans un Assembly
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:231:06/10/2014:bad updating of the assembly's tree of frames
 * VERSION::FA:232:21/11/2014:FT232 included in FT231
 * VERSION::DM:289:27/08/2014:Add exception to SpacececraftState.getAttitude()
 * VERSION::FA:367:21/11/2014:Add header for FT232
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:669:26/08/2016:remove exception from updateMainPartFrame method
 * VERSION::FA:1192:30/08/2017:update parts frame
 * VERSION::FA:1313:13/11/2017:correct Assembly HashMap
 * VERSION::DM:1420:24/11/2017:updateMainPartFrame() speed-up
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * 
 * This class describes an assembly by all its sub parts.
 * <p>
 * This assembly shall be created and modified using the associated builder. Then user can access to each part by its
 * name and get its properties. This assembly does not include the physical models : models shall be created in
 * separated classes using this one.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment The use of a frame linked to the tree of frames in each of the parts makes this class not
 *                      thread-safe.
 * 
 * @see AssemblyBuilder
 * @see MainPart
 * @see Part
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class Assembly implements Serializable {

    /**
     * String Frame
     */
    public static final String FRAME = "Frame";

    /** serial ID */
    private static final long serialVersionUID = 2142102502513430662L;

    /**
     * Main part of the assembly.
     */
    private MainPart mainPart;

    /**
     * The other parts.
     */
    private final Map<String, IPart> parts;

    /**
     * Boolean indicating if the assembly has mobile parts. This boolean is used to speed-up computation times
     * (if the assembly does not have mobile parts, parts transformation are not updated).
     */
    private boolean hasMobileParts;

    /**
     * Last spacecraft state used to update main part frame.
     * This a cache used to speed up computations
     */
    private SpacecraftState stateCache;

    /**
     * Assembly simple constructor. Shall be accessed only from the builder.
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    protected Assembly() {
        // creation of the parts list.
        this.parts = new LinkedHashMap<String, IPart>();
        this.hasMobileParts = false;
        this.stateCache = null;
    }

    /**
     * Adds a part to the assembly.
     * 
     * @param part
     *        the part to add to the assembly
     * @throws IllegalArgumentException
     *         if a part with this name already exists or if the main part has not been created yet.
     */
    public final void addPart(final IPart part) {
        this.mainPartCheck();
        final String partName = part.getName();
        if (partName.equals(mainPart.getName()) || parts.containsKey(partName)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_PART_NAME_EXISTS);
        }
        parts.put(partName, part);

        // Check if this part is mobile
        if (part instanceof MobilePart) {
            // Mobile part
            this.hasMobileParts = true;
        }
    }

    /**
     * Adds the main part to the assembly.
     * 
     * @param part
     *        the main part to add
     * @throws IllegalArgumentException
     *         if the main part is already created
     */
    public final void addMainPart(final MainPart part) {
        if (this.mainPart == null) {
            this.mainPart = part;
        } else {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ALREADY_A_MAIN_PART);
        }

    }

    /**
     * Removes a part from the assembly. All the children parts of the one removed will be removed too : they won't
     * have no parent frame.
     * <p>
     * boolean {@link #hasMobileParts} is not updated for simplicity (this situation is not common and is still
     * perfectly valid).
     * </p>
     * 
     * @param partName
     *        the name of the part to remove
     * @throws IllegalArgumentException
     *         if no part has this name
     */
    public final void removePart(final String partName) {

        // checks if the main part exists
        this.mainPartCheck();

        // if the part is the main one, all are removed
        if (partName.equals(this.mainPart.getName())) {
            this.mainPart = null;
            this.parts.clear();
        } else {
            // check of the part name
            this.partNameCheck(partName);

            final Set<String> partToRemove = new HashSet<String>();

            // get the list of children to remove
            for (final Entry<String, IPart> entry : this.parts.entrySet()) {
                final String otherPartName = entry.getKey();
                if (this.parts.get(otherPartName).getParent() != null
                    && this.parts.get(otherPartName).getParent().getName().equals(partName)) {
                    partToRemove.add(otherPartName);
                }
            }

            // properly remove each of the children
            for (final String string : partToRemove) {
                this.removePart(string);
            }

            // finally remove the part itself
            this.parts.remove(partName);
        }
    }

    /**
     * Returns the part whose name is specified.
     * 
     * @param name
     *        the name of the part
     * @return the part whose the name is specified
     * @throws IllegalArgumentException
     *         if no part has this name
     */
    public final IPart getPart(final String name) {

        // checks if the main part exists
        this.mainPartCheck();

        // if the part is the main
        if (name.equals(this.mainPart.getName())) {
            return this.mainPart;
        }

        // check of the part otherwise
        this.partNameCheck(name);

        return this.parts.get(name);
    }

    /**
     * @return the main part
     */
    public final MainPart getMainPart() {
        this.mainPartCheck();
        return this.mainPart;
    }

    /**
     * 
     * Returns all the parts of the assembly.
     * 
     * @return the assembly parts
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public final Map<String, IPart> getParts() {

        // checks if the main part exists
        this.mainPartCheck();

        final Map<String, IPart> listOfParts = new LinkedHashMap<String, IPart>();

        // add the main part
        listOfParts.put(this.mainPart.getName(), this.mainPart);

        // add the other parts
        listOfParts.putAll(this.parts);

        return listOfParts;
    }

    /**
     * Checks if the main part exists.
     */
    private void mainPartCheck() {
        if (this.mainPart == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NO_MAIN_PART);
        }
    }

    /**
     * Checks if a part with this name exists, and throws an exception if not.
     * 
     * @param partName
     *        the part name to be checked.
     * @throws IllegalArgumentException
     *         if no part has this name
     */
    private void partNameCheck(final String partName) {
        if (!this.parts.containsKey(partName)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_PART_DONT_EXIST);
        }
    }

    /**
     * Gets the list of the name of the parts in the assembly (the main part is excluded).
     * 
     * @return the list of the names of the parts
     */
    public final Set<String> getAllPartsNames() {
        return this.getParts().keySet();
    }

    /**
     * Update the frame of the parts with provided state (including date).
     * <p>Note: date and state are redundant. Either date or state is null.
     * This behavior is OK since it is an internal method.</p>
     * @param date date
     * @param state state
     * @throws PatriusException thrown if a part frame update failed
     */
    private void updateAllPartFrames(final AbsoluteDate date, final SpacecraftState state) throws PatriusException {

        // Initializations
        int levelToChange = 1;
        boolean stillPartsToChange = true;

        // Loop on all the parts' levels
        while (stillPartsToChange) {
            stillPartsToChange = false;

            // Loop on all the parts of this level
            for (final Entry<String, IPart> entry : this.parts.entrySet()) {
                final IPart part = entry.getValue();
                if (part.getPartLevel() == levelToChange) {
                    // Update of the frames
                    if (state != null) {
                        part.updateFrame(state);
                    } else {
                        part.updateFrame(date);
                    }
                    stillPartsToChange = true;
                }
            }
            levelToChange++;
        }
    }

    /**
     * Initialize the main part's frame using a {@link UpdatableFrame} as an input argument.
     * This allows the user to link the entire assembly frame tree to the PATRIUS frames tree
     * (if argument {@code frame}) is in the PATRIUS Frames tree (see {@link FramesFactory}).
     * This method asks for an UpdatableFrame that will later allow to redefine its transformation.
     * 
     * <p>Warning: the assembly should contain no mobile parts, otherwise call 
     * {@link #initMainPartFrame(SpacecraftState)}</p>
     *
     * @param frame
     *        the new main part's frame
     * @throws PatriusException thrown if the assembly contains mobiles parts
     */
    public final void initMainPartFrame(final UpdatableFrame frame) throws PatriusException {
        this.mainPartCheck();

        // main part modification
        this.mainPart.setFrame(frame);

        // all children parts are updated with the new tree of frames
        this.updateAllPartFrames(null, null);
    }

    /**
     * Initialize the main part's frame using a {@link SpacecraftState} as an input argument.
     * The main part's parent frame is then the SpacecraftState's definition frame.
     * This allows the user to link the entire assembly frame tree to the PATRIUS frames tree.
     * 
     * @param state
     *        the SpacecraftState defining the position and orientation of the main part
     *        in its parent frame
     * @throws PatriusException
     *         if no attitude is defined
     */
    public final void initMainPartFrame(final SpacecraftState state) throws PatriusException {
        this.mainPartCheck();

        // Computation of the transformation
        final Transform transform = this.computeTransform(state);
        final String mainFrameName = this.mainPart.getName() + FRAME;
        // Creation of the new main part frame
        final UpdatableFrame newMainFrame = new UpdatableFrame(state.getFrame(), transform, mainFrameName);
        // Set the main part's frame
        this.mainPart.setFrame(newMainFrame);

        // All children parts are updated with the new tree of frames
        this.updateAllPartFrames(null, state);
    }

    /**
     * Updates the main part frame's transformation to its parent frame using a {@link Transform} as an input argument.
     * <p>
     * It is assumed that user has already defined a {@link UpdatableFrame} for the main part (using
     * {@link #initMainPartFrame(UpdatableFrame)} or {@link #initMainPartFrame(SpacecraftState)}). If that is not the
     * case, an {@link IllegalArgumentException} is thrown.
     * </p>
     * <p>Warning: the assembly should contain no mobile parts, otherwise call 
     * {@link #updateMainPartFrame(SpacecraftState)}</p>
     * 
     * @param transform
     *        the Transform
     * @throws PatriusException
     *         if a problem occurs during frames transformations
     * @throws IllegalArgumentException
     *         if the main frame has no parent yet
     */
    public final void updateMainPartFrame(final Transform transform) throws PatriusException {
        this.mainPartCheck();
        final UpdatableFrame mainPartFrame = (UpdatableFrame) this.mainPart.getFrame();

        if (this.mainPart.isLinkedToOrekitTree()) {
            mainPartFrame.setTransform(transform);

            if (this.hasMobileParts) {
                // Update if 
                this.updateAllPartFrames(transform.getDate(), null);
            }
        } else {
            // Problem if the main frame has no parent yet
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_MAIN_FRAME_HAS_NO_PARENT);
        }

    }

    /**
     * Updates the main part frame's transformation to its parent frame using a {@link Transform} as an input argument.
     * 
     * <p>
     * It is assumed that user has already defined a {@link UpdatableFrame} for the main part (using
     * {@link #initMainPartFrame(UpdatableFrame)} or {@link #initMainPartFrame(SpacecraftState)}). If that is not the
     * case, an {@link IllegalArgumentException} is thrown.
     * </p>
     * 
     * @param state
     *        the SpacecraftState
     * @throws PatriusException
     *         if a problem occurs during frames transformations
     * @throws IllegalArgumentException
     *         if the main frame has no parent yet
     */
    public final void updateMainPartFrame(final SpacecraftState state) throws PatriusException {

        if (!this.needUpdate(state)) {
            // Check if state has changed
            return;
        }

        this.mainPartCheck();
        final UpdatableFrame mainPartFrame = (UpdatableFrame) this.mainPart.getFrame();

        if (this.mainPart.isLinkedToOrekitTree()) {

            if (mainPartFrame.getParent().equals(state.getFrame())) {
                // Common case: state frame is integration frame
                mainPartFrame.setTransform(this.computeTransform(state));
            } else {
                // Generic case

                // Transformation from main part parent frame to state frame
                final Transform t1 = mainPartFrame.getParent().getTransformTo(state.getFrame(), state.getDate());

                // Transformation from state frame to satellite frame
                final Transform t2 = this.computeTransform(state);

                // Computation of the global transformation
                final Transform transform = new Transform(state.getDate(), t1, t2);
                mainPartFrame.setTransform(transform);
            }

            if (this.hasMobileParts) {
                this.updateAllPartFrames(null, state);
            }
        } else {
            // Problem if the main frame has no parent yet
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_MAIN_FRAME_HAS_NO_PARENT);
        }
    }

    /**
     * @param state spacecraft state
     * @return true if state has changed since last call
     */
    private boolean needUpdate(final SpacecraftState state) {

        // Initialization
        boolean res = true;

        if (this.stateCache != null && !state.equals(stateCache)) {
            // Check dates
            final boolean sameDate = state.getDate().durationFrom(this.stateCache.getDate()) == 0;
            if (sameDate) {
                // Dates are identical, check Frame
                final boolean sameFrame = state.getFrame().getName().equals(this.stateCache.getFrame().getName());

                if (sameFrame) {
                    // Frames are identical, check PV
                    final PVCoordinates pvState = state.getPVCoordinates();
                    final PVCoordinates pvCache = this.stateCache.getPVCoordinates();
                    final Vector3D posState = pvState.getPosition();
                    final Vector3D posCache = pvCache.getPosition();
                    final Vector3D velState = pvState.getVelocity();
                    final Vector3D velCache = pvCache.getVelocity();
                    final boolean samePos = posState.equals(posCache);
                    final boolean sameVel = velState.equals(velCache);
                    if (samePos && sameVel) {
                        res = false;
                    }
                }
            }
        }

        this.stateCache = state;
        return res;
    }

    /**
     * Computes a {@link Transform} from the rotation and translation contained in a {@link SpacecraftState}.
     * 
     * @param state
     *        the SpacecraftState
     * @return the created Transform object
     * @throws PatriusException
     *         if no attitude is defined
     */
    private Transform computeTransform(final SpacecraftState state) throws PatriusException {

        // getting the rotation and rotation
        final Transform rotation = new Transform(state.getDate(), state.getAttitude().getOrientation());
        final Transform translation = new Transform(state.getDate(), state.getOrbit().getPVCoordinates());
        // Transform creation
        return new Transform(state.getDate(), translation, rotation);
    }

}
