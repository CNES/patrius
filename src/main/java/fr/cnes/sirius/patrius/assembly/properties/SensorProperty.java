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
 * @history creation 19/04/2012
  * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.events.LocalRadiusProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents a generic sensor property for a part of the assembly. This sensor is defined by a
 * sight axis and some optional features : a target, some fields of view and inhibition.
 * 
 * @concurrency not thread-safe
 * 
 * @see IPartProperty
 * @see IFieldOfView
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public final class SensorProperty implements IPartProperty {

    /** Serial UID. */
    private static final long serialVersionUID = -2961041854664923116L;

    /** sight axis (in the part's frame) */
    private final Vector3D inSightAxis;

    /** the main target position */
    private PVCoordinatesProvider mainTarget;

    /** the radius of the main target */
    private LocalRadiusProvider mainTargetRadius;

    /** main field of view */
    private IFieldOfView mainField;

    /** inhibition fields, empty by default. */
    private IFieldOfView[] inhibitionFields = new IFieldOfView[0];

    /** inhibition targets, empty by default. */
    private PVCoordinatesProvider[] inhibitionTargets = new PVCoordinatesProvider[0];

    /** the radiuses of the inhibition targets */
    private LocalRadiusProvider[] inhibitionTargetsRadiuses = new LocalRadiusProvider[0];

    /** the reference axis, empty by default. */
    private Vector3D[] referenceAxis = new Vector3D[0];

    /**
     * Constructor of the generic sensor property
     * 
     * @param sightAxis
     *        the main sight axis of this sensor.
     */
    public SensorProperty(final Vector3D sightAxis) {
        this.inSightAxis = sightAxis.normalize();
    }

    /**
     * Sets the main field of view of this sensor
     * 
     * @param field
     *        the new main field of view
     */
    public void setMainFieldOfView(final IFieldOfView field) {
        this.mainField = field;
    }

    /**
     * Sets the arrays of inhibition fields and the associated targets : the two array must
     * have the same length.
     * 
     * @param fields
     *        the inhibition fields
     * @param targets
     *        the targets associated to those fields
     * @param targetRadiuses
     *        the radiuses of the target objects
     */
    public void setInhibitionFieldsAndTargets(final IFieldOfView[] fields, final PVCoordinatesProvider[] targets,
                                              final LocalRadiusProvider[] targetRadiuses) {
        if (fields.length != targets.length || targetRadiuses.length != fields.length) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_BAD_LENGTH);
        }

        this.inhibitionTargetsRadiuses = targetRadiuses.clone();
        this.inhibitionFields = fields.clone();
        this.inhibitionTargets = targets.clone();
    }

    /**
     * Sets the reference axis
     * 
     * @param refAxis
     *        the axis array
     */
    public void setReferenceAxis(final Vector3D[] refAxis) {
        this.referenceAxis = new Vector3D[refAxis.length];
        for (int i = 0; i < refAxis.length; i++) {
            this.referenceAxis[i] = refAxis[i].normalize();
        }
    }

    /**
     * @return the main sight axis (in the part's frame)
     */
    public Vector3D getInSightAxis() {
        return this.inSightAxis;
    }

    /**
     * @return the main field of view (in the part's frame)
     */
    public IFieldOfView getMainField() {
        return this.mainField;
    }

    /**
     * @return the inhibition fields array (in the part's frame)
     */
    public IFieldOfView[] getInhibitionFields() {
        return this.inhibitionFields.clone();
    }

    /**
     * @return the inhibition targets array
     */
    public PVCoordinatesProvider[] getInhibitionTargets() {
        return this.inhibitionTargets.clone();
    }

    /**
     * @return the inhibition targets radiuses
     */
    public LocalRadiusProvider[] getInhibitionTargetsRadiuses() {
        return this.inhibitionTargetsRadiuses.clone();
    }

    /**
     * @return the reference axis array (in the part's frame)
     */
    public Vector3D[] getReferenceAxis() {
        return this.referenceAxis.clone();
    }

    /**
     * Sets the main target of the sensor
     * 
     * @param target
     *        the new main target center
     * @param radius
     *        the target's radius (set 0.0 for to create a simple point target)
     */
    public void setMainTarget(final PVCoordinatesProvider target, final LocalRadiusProvider radius) {
        this.mainTarget = target;
        this.mainTargetRadius = radius;
    }

    /**
     * @return the main target center
     */
    public PVCoordinatesProvider getMainTarget() {
        return this.mainTarget;
    }

    /**
     * @return the main target radius
     */
    public LocalRadiusProvider getMainTargetRadius() {
        return this.mainTargetRadius;
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.SENSOR;
    }

}
