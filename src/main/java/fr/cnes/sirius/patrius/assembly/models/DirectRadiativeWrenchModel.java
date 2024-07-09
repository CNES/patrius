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
 * @history creation 22/07/2012
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * VERSION::DM:85:22/07/2013:Solar radiation wrench
 * VERSION::DM:834:04/04/2017:create vehicle object
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeApplicationPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.wrenches.RadiationWrenchSensitive;
import fr.cnes.sirius.patrius.wrenches.Wrench;

/**
 * This class represents a spacecraft capable
 * of computing the wrench caused by solar radiation pressure.
 * 
 * @author Rami Houdroge
 * @version $Id$
 * @since 2.1
 * 
 * @concurrency not thread-safe
 * @concurrency.comment class uses internal mutable attributes and frames
 * 
 */
public class DirectRadiativeWrenchModel implements RadiationWrenchSensitive {

    /** Serial UID. */
    private static final long serialVersionUID = 7117821510098365571L;
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
    public DirectRadiativeWrenchModel(final Assembly inAssembly) {
        this.checkAssemblyProperties(inAssembly);
        this.assembly = inAssembly;
        this.inertiaModel = new InertiaComputedModel(inAssembly);
    }

    /**
     * This method tests if the required properties exist (RADIATIVE + INERTIA + and RADIATIVE_CROSS_SECTION or
     * FACET_RADIATIVE). At least
     * one part with a mass is required.
     * 
     * @param assemblyIn
     *        The considered vehicle.
     */
    private void checkAssemblyProperties(final Assembly assemblyIn) {

        boolean hasRadProp = false;
        boolean hasMassProp = false;

        for (final IPart part : assemblyIn.getParts().values()) {
            if (part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION)
                ^ part.hasProperty(PropertyType.RADIATIVE_FACET)) {
                if (part.hasProperty(PropertyType.RADIATIVE) &
                    part.hasProperty(PropertyType.RADIATION_APPLICATION_POINT)) {
                    hasRadProp |= true;
                }
            }

            if (part.hasProperty(PropertyType.INERTIA)) {
                hasMassProp |= true;
            }
        }

        if (!hasRadProp || !hasMassProp) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NO_RADIATIVE_MASS_PROPERTIES);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Wrench radiationWrench(final SpacecraftState state, final Vector3D flux) throws PatriusException {

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
        Vector3D appPoint = Vector3D.ZERO;
        Vector3D lever = Vector3D.ZERO;
        Wrench wrench = Wrench.ZERO;
        Wrench totalWrench = new Wrench(centre, Vector3D.ZERO, Vector3D.ZERO);

        for (final IPart part : this.assembly.getParts().values()) {

            partComputedForce = Vector3D.ZERO;
            wrench = Wrench.ZERO;

            // Application point of force expressed in SpacecraftState frame (algebraic)
            if (part.hasProperty(PropertyType.RADIATION_APPLICATION_POINT) &&
                part.hasProperty(PropertyType.RADIATIVE)) {

                // Force applied on the current part, expressed in the SpacecraftState frame
                if (part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION)) {
                    partComputedForce = DirectRadiativeModel.forceOnSphere(state, part, flux, mainPartFrame);
                }
                if (part.hasProperty(PropertyType.RADIATIVE_FACET)) {
                    partComputedForce = DirectRadiativeModel.forceOnFacet(state, part, flux);
                }

                // Express force in the main part frame (incl. orientation)
                partComputedForce = state.getFrame().getTransformTo(mainPartFrame, state.getDate())
                    .transformVector(partComputedForce);

                // lever
                appPoint = ((RadiativeApplicationPoint) part.getProperty(PropertyType.RADIATION_APPLICATION_POINT))
                    .getApplicationPoint();
                appPoint = part.getFrame().getTransformTo(mainPartFrame, state.getDate())
                    .transformPosition(appPoint);
                lever = appPoint.subtract(centre);

                // Computation of resulting torque in SpacecraftState frame
                torque = Vector3D.crossProduct(lever, partComputedForce);
                wrench = new Wrench(centre, partComputedForce, torque);

                // Contribution to total force
                force = force.add(partComputedForce);
                totalWrench = totalWrench.add(wrench);
            }

        }

        // Total wrench is already at mass centre
        return totalWrench;
    }

    /** {@inheritDoc} */
    @Override
    public Wrench radiationWrench(final SpacecraftState state, final Vector3D flux, final Vector3D origin,
                                  final Frame frame) throws PatriusException {

        // wrench in mass centre of spacecraft
        Wrench wrench = this.radiationWrench(state, flux);

        final Transform fr = frame.getTransformTo(this.assembly.getMainPart().getFrame(), state.getDate());
        final Vector3D newOrigin = fr.transformPosition(origin);

        wrench = wrench.displace(newOrigin);

        return fr.transformWrench(wrench);
    }

}
