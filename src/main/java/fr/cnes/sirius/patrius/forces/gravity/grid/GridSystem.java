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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.grid;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;


/**
 * Grid system. A grid system is a 3D grid which is used for attraction models defined by a grid.
 * Grid contains 3D acceleration as well as force potential.
 * <p>
 * This class is to be used in conjunction with {@link GridGravityModel} for attraction force defined by a grid.
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.7
 */
public interface GridSystem extends Serializable {

    /**
     * Returns true if provided position is within grid, false otherwise.
     * @param position position
     * @return true if provided position is within grid, false otherwise
     */
    boolean isInsideGrid(final Vector3D position);

    /**
     * Returns coordinates in grid system for provided position.
     * @param position position
     * @return coordinates in grid system
     */
    double[] getCoordinates(final Vector3D position);
    
    /**
     * Returns first abscissa data array.
     * @return first abscissa data array
     */
    double[] getXArray();

    /**
     * Returns second abscissa data array.
     * @return second abscissa data array
     */
    double[] getYArray();

    /**
     * Returns third abscissa data array.
     * @return third abscissa data array
     */
    double[] getZArray();

    /**
     * Returns X acceleration data array (values along ordinates).
     * @return X acceleration data array (values along ordinates)
     */
    double[][][] getAccXArray();

    /**
     * Returns Y acceleration data array (values along ordinates).
     * @return Y acceleration data array (values along ordinates)
     */
    double[][][] getAccYArray();

    /**
     * Returns Z acceleration data array (values along ordinates).
     * @return Z acceleration data array (values along ordinates)
     */
    double[][][] getAccZArray();

    /**
     * Returns potential data array (values along ordinates).
     * @return potential data array (values along ordinates)
     */
    double[][][] getPotentialArray();
}
