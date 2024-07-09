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
 * @history creation 13/09/2016
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:599:13/09/2016: new tabulated aero model
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * Drag coefficient container.
 * Drag coefficient is split in 4 parts:
 * <ul>
 * <li>Absorption part.</li>
 * <li>Specular part.</li>
 * <li>Diffuse part (front).</li>
 * <li>Diffuse part (rear).</li>
 * </ul>
 * <p>
 * Drag coefficient must be expressed in satellite frame.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id$
 * 
 * @since 3.3
 */
public final class DragCoefficient {

    /** Absorption part in satellite frame. */
    private final Vector3D scAbs;

    /** Specular part in satellite frame. */
    private final Vector3D scSpec;

    /** Diffuse part (front) in satellite frame. */
    private final Vector3D scDiffAv;

    /** Diffuse part (rear) in satellite frame. */
    private final Vector3D scDiffAr;

    /**
     * Constructor.
     * 
     * @param scAbsIn
     *        absorption part in satellite frame
     * @param scSpecIn
     *        specular part in satellite frame
     * @param scDiffAvIn
     *        diffuse part (front) in satellite frame
     * @param scDiffArIn
     *        diffuse part (rear) in satellite frame
     */
    public DragCoefficient(final Vector3D scAbsIn, final Vector3D scSpecIn, final Vector3D scDiffAvIn,
        final Vector3D scDiffArIn) {
        this.scAbs = scAbsIn;
        this.scSpec = scSpecIn;
        this.scDiffAv = scDiffAvIn;
        this.scDiffAr = scDiffArIn;
    }

    /**
     * Get the absorption part in satellite frame.
     * 
     * @return the absorption part in satellite frame
     */
    public Vector3D getScAbs() {
        return this.scAbs;
    }

    /**
     * Get the specular part in satellite frame.
     * 
     * @return the specular part in satellite frame
     */
    public Vector3D getScSpec() {
        return this.scSpec;
    }

    /**
     * Get the diffuse part (front) in satellite frame.
     * 
     * @return the diffuse part (front) in satellite frame
     */
    public Vector3D getScDiffAv() {
        return this.scDiffAv;
    }

    /**
     * Get the diffuse part (rear) in satellite frame.
     * 
     * @return the diffuse part (rear) in satellite frame
     */
    public Vector3D getScDiffAr() {
        return this.scDiffAr;
    }
}
