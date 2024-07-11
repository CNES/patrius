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
 * @history creation 04/04/2017
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:1420:24/11/2017:updateMainPartFrame() speed-up
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class is a cross section property providing the cross section of shapes such as
 * sphere, cylinder or parallelepiped.
 * This cross section is to be used in radiative models for SRP force computation.
 * 
 * @concurrency immutable
 * 
 * @author rodriguest
 * 
 * @version $Id$
 * 
 * @since 3.4
 * 
 */
public class RadiativeCrossSectionProperty implements IPartProperty {

    /** Serial UID. */
    private static final long serialVersionUID = -3125298683136328466L;

    /**
     * Main satellite shape.
     */
    private final CrossSectionProvider mainShape;

    /**
     * Constructor of this property defining the vehicle main shape.
     * 
     * @param shape
     *        the main shape
     */
    public RadiativeCrossSectionProperty(final CrossSectionProvider shape) {
        this.mainShape = shape;
    }

    /**
     * Compute the cross section of main shape using the relative velocity in the
     * part (having the aero property) frame as the direction to provider to the
     * {@link CrossSectionProvider#getCrossSection(Vector3D)}.
     * 
     * @param state
     *        the current state of the spacecraft
     * @param flux
     *        the incoming flux in the state frame
     * @param mainPartFrame
     *        main frame
     * @param partFrame
     *        frame of part owning the property
     * @return the cross section of the main shape.
     * @throws PatriusException
     *         if some frame specific error occurs
     */
    public double getCrossSection(final SpacecraftState state, final Vector3D flux,
                                  final Frame mainPartFrame, final Frame partFrame) throws PatriusException {

        // Return null cross section if flux is null vector
        if (flux.getNorm() == 0.) {
            return 0.;
        }

        final Transform stateToPart = state.getFrame().getTransformTo(partFrame, state.getDate());
        final Vector3D fluxPartFrame = stateToPart.transformVector(flux);
        return this.mainShape.getCrossSection(fluxPartFrame);
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.RADIATIVE_CROSS_SECTION;
    }
}
