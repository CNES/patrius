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
 * HISTORY
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.parameter.Parameter;

/**
 * A basic gravitational attraction model of a point mass defined by an attraction
 * coefficient.
 * 
 * @author Hugo Barrere
 */
public class PointAttractionModel implements AttractionModel, Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = -6773677032841061847L;

    /** Central attraction coefficient parameter. */
    private Parameter paramMu = null;

    /** Multiplicative coefficient. */
    private double k;

    /**
     * Simple constructor.
     * 
     * @param muIn
     *        central attraction coefficient (m^3/s^2)
     */
    public PointAttractionModel(final double muIn) {
        this(new Parameter(MU, muIn));
    }

    /**
     * Simple constructor.
     * 
     * @param muIn
     *        mu parameter storing standard gravitational
     */
    public PointAttractionModel(final Parameter muIn) {
        this.paramMu = muIn;
        this.k = 1.;
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
    public double getMultiplicativeFactor() {
        return this.k;
    }

    /** {@inheritDoc} */
    @Override
    public void setMultiplicativeFactor(final double factor) {
        this.k = factor;
    }
}
