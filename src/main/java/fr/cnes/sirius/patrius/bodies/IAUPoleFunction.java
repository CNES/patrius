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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel lorsque SpacecraftState en ITRF
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;

/**
 * IAU pole function: this class is used to define an atomic element of IAU pole computation.
 *
 * @author Emmanuel Bignon
 * 
 * @since 4.9
 */
public class IAUPoleFunction implements Serializable {

    /**
     * IAU time dependency (days or centuries).
     */
    public enum IAUTimeDependency {
        /** Days: IAU pole function days-dependent. */
        DAYS,
        
        /** Centuries: IAU pole function centuries-dependent. */
        CENTURIES;
    }
    
     /** Serializable UID. */
    private static final long serialVersionUID = 3128575010313177304L;

    /** IAU pole type. */
    private final IAUPoleFunctionType type;

    /** IAU pole function. */
    private final UnivariateDifferentiableFunction function;

    /** IAU time dependency (days or centuries). */
    private final IAUTimeDependency timeDependency;

    /**
     * Constructor.
     * @param iauPoleType IAU pole type
     * @param function IAU pole function
     * @param iauTimeDependency IAU time dependency (days or centuries)
     */
    public IAUPoleFunction(final IAUPoleFunctionType iauPoleType,
            final UnivariateDifferentiableFunction function,
            final IAUTimeDependency iauTimeDependency) {
        this.type = iauPoleType;
        this.function = function;
        this.timeDependency = iauTimeDependency;
    }

    /**
     * Returns the IAU pole type.
     * @return the IAU pole type
     */
    public IAUPoleFunctionType getType() {
        return type;
    }

    /**
     * Returns the IAU pole function.
     * @return the IAU pole function
     */
    public UnivariateDifferentiableFunction getFunction() {
        return function;
    }

    /**
     * Returns the IAU time dependency (days or centuries).
     * @return the IAU time dependency (days or centuries)
     */
    public IAUTimeDependency getTimeDependency() {
        return timeDependency;
    }
}
