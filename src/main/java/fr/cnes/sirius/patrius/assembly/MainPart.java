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
 * @history creation 8/02/2012
 *
 * HISTORY
* VERSION:4.4:DM:DM-2148:04/10/2019:[PATRIUS] Creations de parties mobiles dans un Assembly
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * VERSION::FA:1192:30/08/2017:update parts frame
 * VERSION::FA:1313:13/11/2017:correct Assembly HashMap
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.OrphanFrame;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * -Class to manage the assembly's main part.
 * <p>
 * -Contains the assembly's main frame and the main part's properties.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment The use of a frame linked to the tree of frames
 *                      makes this class not thread-safe.
 * 
 * @see IPart
 * 
 * @author Thomas Trapier
 * 
 * @since 1.1
 */
public class MainPart extends AbstractPart {

    /** Serial UID. */
    private static final long serialVersionUID = 1602189073138163247L;

    /** Main part frame, updated on request. */
    private transient UpdatableFrame frame;

    /** Flag to know if the assembly is linked to another tree of frames. */
    private boolean isLinkedToOrekitTreeFlag;

    /**
     * Constructor.
     * @param name the name of the main part
     */
    public MainPart(final String name) {
        super(name, null);

        // Create an orphan frame (not linked to the OREKIT frames tree)
        final Frame orphanFrame = OrphanFrame.getNewOrphanFrame(name + "OrphanFrame");
        this.frame = new UpdatableFrame(orphanFrame, Transform.IDENTITY, name + "Frame");
        this.isLinkedToOrekitTreeFlag = false;
    }

    /** {@inheritDoc} */
    @Override
    public final Frame getFrame() {
        return frame;
    }

    /**
     * This method implements no action for the main part.
     * {@inheritDoc}
     */
    @Override
    public void updateFrame(final AbsoluteDate date) {
        // No action
    }

    /** {@inheritDoc} */
    @Override
    public final void updateFrame(final Transform transform) {
        if (isLinkedToOrekitTreeFlag) {
            frame.setTransform(transform);
        } else {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_MAIN_FRAME_HAS_NO_PARENT);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void updateFrame(final SpacecraftState s) throws PatriusException {
        updateFrame(s.getDate());
    }

    /**
     * 
     * Method to modify the frame of the main part.
     * @param frame the new frame of the main part
     */
    public final void setFrame(final UpdatableFrame frame) {
        this.frame = frame;
        isLinkedToOrekitTreeFlag = true;
    }

    /**
     * Returns true if the part is linked to PATRIUS tree of frames.
     * @return true if the assembly is linked to another tree of frames.
     */
    public final boolean isLinkedToOrekitTree() {
        return isLinkedToOrekitTreeFlag;
    }
}
