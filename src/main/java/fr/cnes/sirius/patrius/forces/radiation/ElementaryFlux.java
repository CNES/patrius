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
 * @history creation 24/05/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:205:14/03/2014:Corrected jdoc
 * VERSION::FA:318:05/11/2014:anomalies correction for class RediffusedFlux
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * Elementary flux
 * 
 * @concurrency immutable
 * @concurrency.comment immutable class
 * 
 * @author ClaudeD
 * 
 * @version $Id: ElementaryFlux.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class ElementaryFlux {

    /** direction */
    private final Vector3D dirFlux;
    /** albedo (N/M²) */
    private final double albedoPressure;
    /** infrared (N/M²) */
    private final double infraRedPressure;

    /**
     * Constructor of elementary pressure
     * 
     * @param edirFlux
     *        direction flux
     * @param eAlbedoPressure
     *        albedo pressure (N/M²)
     * @param eInfraRedPressure
     *        infrared pressure (N/M²)
     * 
     * @since 1.2
     */
    public ElementaryFlux(final Vector3D edirFlux, final double eAlbedoPressure, final double eInfraRedPressure) {
        this.dirFlux = edirFlux;
        this.albedoPressure = eAlbedoPressure;
        this.infraRedPressure = eInfraRedPressure;
    }

    /**
     * get the direction flux
     * 
     * @return direction flux
     * 
     * @since 1.2
     */
    public Vector3D getDirFlux() {
        final double dirFluxX = this.dirFlux.getX();
        final double dirFluxY = this.dirFlux.getY();
        final double dirFluxZ = this.dirFlux.getZ();
        return new Vector3D(dirFluxX, dirFluxY, dirFluxZ);
    }

    /**
     * get the albedo pressure (N/M²)
     * 
     * @return albedo pressure
     * 
     * @since 1.2
     */
    public double getAlbedoPressure() {
        return this.albedoPressure;
    }

    /**
     * get the infrared pressure (N/M²)
     * 
     * @return infrared pressure
     * 
     * @since 1.2
     */
    public double getInfraRedPressure() {
        return this.infraRedPressure;
    }
}
