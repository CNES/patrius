/**
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
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de AbstractEOPHistory
 * VERSION:4.11:FA:FA-3312:22/05/2023:[PATRIUS] TrueInertialFrame pas vraiment pseudo-inertiel
 * VERSION:4.11:DM:DM-3248:22/05/2023:[PATRIUS] Renommage de GeodeticPoint en GeodeticCoordinates
 * VERSION:4.11:DM:DM-40:22/05/2023:[PATRIUS] Gestion derivees par rapport au coefficient k dans les GravityModel
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::FA:280:08/10/2014:propagator modified in order to use the mu of gravitational forces
 * VERSION::DM:284:06/01/2015:New architecture for parameterizable Parameters
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.parameter.Parameter;

/**
 * This class represents a gravitational attraction model.
 * 
 * @author Tiziana Sabatini
 * @version $Id: AttractionModel.java 18082 2017-10-02 16:54:17Z bignon $
 * @since 2.3
 */
public abstract class AbstractGravityModel implements GravityModel {

    /** Parameter name for central attraction coefficient. */
    public static final String MU = "central attraction coefficient";

    /** Serializable UID. */
    private static final long serialVersionUID = 3739655019431902427L;

    /**
     * Central body frame (rotating).
     */
    private final Frame bodyFrame;

    /** Central attraction coefficient parameter. */
    private final Parameter paramMu;

    /**
     * Constructor.
     * 
     * @param bodyFrameIn body frame
     * @param mu gravitational parameter
     */
    public AbstractGravityModel(final Frame bodyFrameIn, final Parameter mu) {
        this.bodyFrame = bodyFrameIn;
        this.paramMu = mu;
    }

    /**
     * Returns the gravitational coefficient as a parameter.
     * 
     * @return the gravitational coefficient as a parameter
     */
    public Parameter getMuParameter() {
        return this.paramMu;
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return this.paramMu.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public void setMu(final double muIn) {
        this.paramMu.setValue(muIn);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getBodyFrame() {
        return this.bodyFrame;
    }

}
