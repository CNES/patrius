/**
 * 
 * Copyright 2011-2022 CNES
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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2148:04/10/2019:[PATRIUS] Creations de parties mobiles dans un Assembly
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformStateProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Mobile part of an assembly.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.4
 */
@SuppressWarnings("PMD.NullAssignment")
public class MobilePart extends AbstractPart {

    /** Serial UID. */
    private static final long serialVersionUID = -2590897456764747009L;

    /** Part frame, updated on request. */
    private transient UpdatableFrame frame;

    /** Transform between own frame and parent frame. */
    private final TransformStateProvider transformProvider;

    /**
     * Constructor.
     * @param name the name of the part
     * @param parentPart the parent part
     * @param transformProvider transformation between the part's frame and the parent's one
     */
    public MobilePart(final String name, final IPart parentPart, final TransformStateProvider transformProvider) {
        super(name, parentPart);
        this.transformProvider = transformProvider;
        // Frame cannot be set at this point, since not spacecraft state has been yet provided
        this.frame = null;
    }

    /** {@inheritDoc} */
    @Override
    public final void updateFrame(final AbsoluteDate date) {
        throw PatriusException.createIllegalArgumentException(PatriusMessages.MOBILE_PART_FRAME_UPDATE);
    }

    /** {@inheritDoc} */
    @Override
    public final void updateFrame(final Transform t) throws PatriusException {
        throw PatriusException.createIllegalArgumentException(PatriusMessages.MOBILE_PART_FRAME_UPDATE);
    }

    /** {@inheritDoc} */
    @Override
    public final void updateFrame(final SpacecraftState s) throws PatriusException {
        final Transform t = transformProvider.getTransform(s);
        this.frame = new UpdatableFrame(getParent().getFrame(), t, getName() + Assembly.FRAME);
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
    public final TransformStateProvider getTransformProvider() {
        return transformProvider;
    }
}
