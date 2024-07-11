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
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.7:DM:DM-2684:18/05/2021:Création d'une classe UserIAUPole, à l'image de la classe UserCelestialBody
 * VERSION:4.7:DM:DM-2888:18/05/2021:ajout des derivee des angles alpha,delta,w &amp;#224; la classe UserIAUPole
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.Serializable;
import java.util.List;

/**
 * IAU pole coefficients for one elements (pole or prime meridian angle).
 *
 * @author Emmanuel Bignon
 * 
 * @since 4.7
 */
public class IAUPoleCoefficients1D implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = 3128575010313177304L;

    /** List of atomic IAU pole functions. */
    private final List<IAUPoleFunction> functions;

    /**
     * Constructor.
     * @param functions list of IAU pole atomic functions
     */
    public IAUPoleCoefficients1D(final List<IAUPoleFunction> functions) {
        this.functions = functions;
    }

    /**
     * Returns the IAU pole functions.
     * @return the IAU pole functions
     */
    public List<IAUPoleFunction> getFunctions() {
        return functions;
    }
}
