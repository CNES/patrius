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
 * @history creation 23/07/2012
 *
 * HISTORY
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:23/07/2013:Drag wrench
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:662:26/08/2016:Computation times speed-up
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.AeroApplicationPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.wrenches.DragWrenchSensitive;
import fr.cnes.sirius.patrius.wrenches.Wrench;

/**
 * This class represents a {@link DragWrenchSensitive} assembly model.
 * 
 * @concurrency not thread-safe
 * @concurrency.comment class uses internal mutable attributes and frames
 * 
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $Id $
 */
public class AeroWrenchModel implements DragWrenchSensitive {

    /**
     * The considered vehicle on which the model is based on.
     */
    private final Assembly assembly;

    /**
     * Inertia model
     */
    private final InertiaComputedModel inertiaModel;

    /**
     * Radiative model (the acceleration is computed from all the sub parts of the vehicle).
     * 
     * @param inAssembly
     *        The considered vehicle.
     */
    public AeroWrenchModel(final Assembly inAssembly) {
        this.checkAssemblyProperties(inAssembly);
        this.assembly = inAssembly;
        this.inertiaModel = new InertiaComputedModel(this.assembly);
    }

    /**
     * 
     * This method tests if the required properties exist (AERO + INERTIA + AERO_CROSS_SECTION | AERO_RADIATIVE).
     * At least one part with a mass is required.
     * 
     * @param assemblyIn
     *        The considered vehicle.
     */
    private void checkAssemblyProperties(final Assembly assemblyIn) {

        boolean hasRadProp = false;
        boolean hasMassProp = false;

        for (final IPart part : assemblyIn.getParts().values()) {
            if (part.hasProperty(PropertyType.AERO_FACET) ^ part.hasProperty(PropertyType.AERO_CROSS_SECTION)) {
                if (part.hasProperty(PropertyType.AERO_APPLICATION_POINT)) {
                    hasRadProp |= true;
                }

            }

            if (part.hasProperty(PropertyType.INERTIA)) {
                hasMassProp |= true;
            }
        }

        if (!hasRadProp || !hasMassProp) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NO_AERO_MASS_PROPERTIES);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Wrench dragWrench(final SpacecraftState state, final double density,
                             final Vector3D relativeVelocity) throws PatriusException {

        // Update PV
        this.assembly.updateMainPartFrame(state);

        // Main satellite part frame
        final Frame mainPartFrame = this.assembly.getMainPart().getFrame();

        // center of mass
        final Vector3D centre = this.inertiaModel.getMassCenter(mainPartFrame, state.getDate());

        // Containers
        Vector3D partComputedForce;
        Vector3D force = Vector3D.ZERO;
        Vector3D torque = Vector3D.ZERO;
        Vector3D lever = Vector3D.ZERO;
        Vector3D appPoint = Vector3D.ZERO;
        Wrench wrench = Wrench.ZERO;
        Wrench totalWrench = new Wrench(centre, Vector3D.ZERO, Vector3D.ZERO);

        for (final IPart part : this.assembly.getParts().values()) {

            // Force applied on the current part, expressed in spacecraft state frame
            partComputedForce = Vector3D.ZERO;
            wrench = Wrench.ZERO;

            // Application point of force expressed in SpacecraftState frame (algebraic)
            if (part.hasProperty(PropertyType.AERO_APPLICATION_POINT)) {

                // compute the force for the current shape
                if (part.hasProperty(PropertyType.AERO_CROSS_SECTION)) {
                    partComputedForce = AeroModel.forceOnSphere(state, part, density, relativeVelocity, mainPartFrame);
                }

                // compute the force for the current facet
                if (part.hasProperty(PropertyType.AERO_FACET)) {
                    partComputedForce = AeroModel.forceOnFacet(state, part, this.assembly, density, relativeVelocity);
                }

                // Application point of force expressed in SpacecraftState frame (algebraic)
                appPoint = ((AeroApplicationPoint) part.getProperty(PropertyType.AERO_APPLICATION_POINT))
                    .getApplicationPoint();
                appPoint = part.getFrame().getTransformTo(mainPartFrame, state.getDate()).transformPosition(appPoint);

                lever = appPoint.subtract(centre);

                // Computation of resulting torque in SpacecraftState frame
                torque = Vector3D.crossProduct(lever, partComputedForce);
                wrench = new Wrench(centre, partComputedForce, torque);

                // Contribution to total force
                force = force.add(partComputedForce);
                totalWrench = totalWrench.add(wrench);
            }
        }

        // Total wrench is already at mass centre in main part frame
        return totalWrench;

    }

    /** {@inheritDoc} */
    @Override
    public Wrench dragWrench(final SpacecraftState state, final double density, final Vector3D relativeVelocity,
                             final Vector3D origin, final Frame frame) throws PatriusException {
        // wrench in mass centre of spacecraft
        Wrench wrench = this.dragWrench(state, density, relativeVelocity);

        final Transform fr = frame.getTransformTo(this.assembly.getMainPart().getFrame(), state.getDate());
        final Vector3D newOrigin = fr.transformPosition(origin);

        wrench = wrench.displace(newOrigin);

        return fr.transformWrench(wrench);
    }

}
