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
 * @history Created on 18/07/2013
 *
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:25/07/2013:Created the Drag wrench model
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.wrenches;

import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents a drag wrench model. It requires a spacecraft capable
 * of computing the wrench caused by drag forces.
 * 
 * @see DragWrenchSensitive
 * 
 * @author Rami Houdroge
 * @version $Id$
 * @since 2.1
 * 
 * @concurrency not thread-safe
 * @concurrency.comment class uses internal mutable attributes and frames
 */
public class DragWrench implements WrenchModel {

    /** Atmospheric model. */
    private final Atmosphere atm;

    /** Spacecraft. */
    private final DragWrenchSensitive spc;

    /**
     * Simple constructor.
     * 
     * @param atmosphere
     *        atmospheric model
     * @param spacecraft
     *        the object physical and geometrical information
     */
    public DragWrench(final Atmosphere atmosphere, final DragWrenchSensitive spacecraft) {
        this.atm = atmosphere;
        this.spc = spacecraft;
    }

    /** {@inheritDoc} */
    @Override
    public Wrench computeWrench(final SpacecraftState s) throws PatriusException {
        final AbsoluteDate date = s.getDate();
        final Frame frame = s.getFrame();
        final Vector3D position = s.getPVCoordinates().getPosition();

        final double rho = this.atm.getDensity(date, position, frame);

        final Vector3D vAtm = this.atm.getVelocity(date, position, frame);
        final Vector3D relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        // Addition of calculated acceleration to adder
        return this.spc.dragWrench(s, rho, relativeVelocity);
    }

    /** {@inheritDoc} */
    @Override
    public Wrench computeWrench(final SpacecraftState s, final Vector3D origin,
                                final Frame frame) throws PatriusException {
        final AbsoluteDate date = s.getDate();
        final Frame velFrame = s.getFrame();
        final Vector3D position = s.getPVCoordinates().getPosition();

        final double rho = this.atm.getDensity(date, position, velFrame);

        final Vector3D vAtm = this.atm.getVelocity(date, position, velFrame);
        final Vector3D relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        // Addition of calculated acceleration to adder
        return this.spc.dragWrench(s, rho, relativeVelocity, origin, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeTorque(final SpacecraftState s) throws PatriusException {
        return this.computeWrench(s).getTorque();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeTorque(final SpacecraftState s, final Vector3D origin,
                                  final Frame frame) throws PatriusException {
        return this.computeWrench(s, origin, frame).getTorque();
    }
}
