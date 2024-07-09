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
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:611:02/08/2016:Creation of the class
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.bodies.GeometricBodyShape;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Implementation for variable radius providers.
 */
public class VariableRadiusProvider implements LocalRadiusProvider {

    /** Serial UID. */
    private static final long serialVersionUID = 6707592469714471270L;

    /** Geometric body shape. */
    private final GeometricBodyShape bodyShape;

    /**
     * Constructor with a body shape capable of calculating apparent radii.
     * 
     * @param shape
     *        body shape
     */
    public VariableRadiusProvider(final GeometricBodyShape shape) {
        this.bodyShape = shape;
    }

    /** {@inheritDoc} */
    @Override
    public double getLocalRadius(final Vector3D position, final Frame frame, final AbsoluteDate date,
                                 final PVCoordinatesProvider occultedBodyIn) {
        try {
            return this.bodyShape.getLocalRadius(position, frame, date, occultedBodyIn);
        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }
    }

    /**
     * Getter for boy shape.
     * 
     * @return body shape
     */
    public GeometricBodyShape getBodyShape() {
        return this.bodyShape;
    }
}
