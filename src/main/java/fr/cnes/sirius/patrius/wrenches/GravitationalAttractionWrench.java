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
 * VERSION::DM:85:09/09/2013:Created the attraction wrench model
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.wrenches;

import fr.cnes.sirius.patrius.assembly.models.IInertiaModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents a gravitational attraction wrench
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread-safe if the inertia model is thread-safe
 * 
 * @author Rami Houdroge
 * @version 2.1
 * @since $$
 */
public class GravitationalAttractionWrench implements WrenchModel {

    /**
     * Inertia model
     */
    private final IInertiaModel model;

    /**
     * Gravitational parameter
     */
    private final double muu;

    /**
     * Create a wrench model from an inertia model and a gravitational parameter
     * 
     * @param spacecraft
     *        inertia model
     * @param mu
     *        gravitational parameter
     */
    public GravitationalAttractionWrench(final IInertiaModel spacecraft, final double mu) {
        this.model = spacecraft;
        this.muu = mu;
    }

    /** {@inheritDoc} */
    @Override
    public Wrench computeWrench(final SpacecraftState s) throws PatriusException {

        // Unit vector
        final Vector3D unit = s.getPVCoordinates().getPosition().normalize();

        // gravitational attraction
        final double tmtr3 = MathLib.divide(3 * this.muu, MathLib.pow(s.getPVCoordinates().getPosition().getNorm(), 3));
        final Vector3D force = unit.scalarMultiply(tmtr3);

        // inertia model
        final Vector3D inertia = this.model.getInertiaMatrix(s.getFrame(), s.getDate()).multiply(unit);

        return new Wrench(this.model.getMassCenter(s.getFrame(), s.getDate()), force, force.crossProduct(inertia));
    }

    /** {@inheritDoc} */
    @Override
    public Wrench computeWrench(final SpacecraftState s, final Vector3D origin,
                                final Frame frame) throws PatriusException {

        // Unit vector
        final Vector3D unit = s.getPVCoordinates().getPosition().normalize();

        // gravitational attraction
        final double tmtr3 = MathLib.divide(3 * this.muu, MathLib.pow(s.getPVCoordinates().getPosition().getNorm(), 3));
        final Vector3D force = unit.scalarMultiply(tmtr3);

        // inertia model
        final Vector3D inertia = this.model.getInertiaMatrix(frame, s.getDate(), origin).multiply(unit);

        return new Wrench(origin, force, force.crossProduct(inertia));
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
