/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history creation 8/02/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2148:04/10/2019:[PATRIUS] Creations de parties mobiles dans un Assembly
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:231:06/10/2014:bad updating of the assembly's tree of frames
 * VERSION::FA:1192:30/08/2017:update parts frame
 * VERSION::FA:1313:13/11/2017:correct Assembly HashMap
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * -Class to manage the assembly's part (the main part is excluded).
 * </p>
 * <p>
 * -Contains the part's local frame and properties.
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
@SuppressWarnings("PMD.ShortClassName")
public class Part extends AbstractPart {

    /** Part frame. */
    private UpdatableFrame frame;

    /** Transform between own frame and parent frame. */
    private Transform transform;

    /**
     * Constructor.
     * @param name the name of the part
     * @param parentPart the parent part
     * @param transform transformation between the part's frame and the parent's one
     */
    public Part(final String name, final IPart parentPart, final Transform transform) {
        super(name, parentPart);
        this.transform = transform;
        this.frame = new UpdatableFrame(getParent().getFrame(), transform, getName() + Assembly.FRAME);
    }

    /** {@inheritDoc} */
    @Override
    public final void updateFrame(final AbsoluteDate date) {
        Transform t = transform;
        if (date != null) {
            t = transform.shiftedBy(date.durationFrom(transform.getDate()));
        }
        frame = new UpdatableFrame(getParent().getFrame(), t, getName() + Assembly.FRAME);
    }

    /** {@inheritDoc} */
    @Override
    public final void updateFrame(final SpacecraftState s) {
        updateFrame(s.getDate());
    }

    /** {@inheritDoc} */
    @Override
    public final void updateFrame(final Transform t) throws PatriusException {
        frame.setTransform(t);
        transform = t;
    }

    /** {@inheritDoc} */
    @Override
    public final UpdatableFrame getFrame() {
        return frame;
    }

    /**
     * Returns the transform linking the part to its parent part.
     * @return the transform linking the part to its parent part
     */
    public final Transform getTransform() {
        return transform;
    }
}
