/**
 * Copyright 2021-2021 CNES
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
 * HISTORY
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.grid;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;


/**
 * Attraction data: 3D acceleration and potential for all grid points. Grid point can be expressed in any coordinates
 * system defined by interface {@link GridSystem} (currently either in 3D coordinates or spherical coordinates).
 * <p>
 * This class is to be used in conjunction with {@link GridAttractionModel} for attraction force defined by a grid.
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.7
 */
public class AttractionData implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = 1676011337644908737L;

    /** Gravitational constant of body. */
    private final double gm;

    /** Center of mass. */
    private final Vector3D centerOfMass;
    
    /** Grid system. */
    private final GridSystem grid;

    /** Attraction data points. */
    private final AttractionDataPoint[] data;

    /**
     * Constructor.
     * @param gm gravitational constant of body
     * @param centerOfMass center of mass
     * @param grid grid system
     * @param data attraction data points
     */
    public AttractionData(final double gm,
            final Vector3D centerOfMass,
            final GridSystem grid,
            final AttractionDataPoint[] data) {
        this.gm = gm;
        this.centerOfMass = centerOfMass;
        this.grid = grid;
        this.data = data;
    }

    /**
     * Returns the gravitational constant of the body.
     * @return the gravitational constant of the body
     */
    public double getGM() {
        return gm;
    }

    /**
     * Returns the center of mass position of the body.
     * @return the center of mass position of the body
     */
    public Vector3D getCenterOfMass() {
        return centerOfMass;
    }

    /**
     * Returns the grid system.
     * @return the grid system
     */
    public GridSystem getGrid() {
        return grid;
    }

    /**
     * Returns the attraction data points.
     * @return the attraction data points
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    // Reason: performances
    public AttractionDataPoint[] getData() {
        return data;
    }
}
