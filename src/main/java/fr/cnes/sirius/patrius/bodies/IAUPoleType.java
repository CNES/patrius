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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;


/**
 * IAU pole type: this enumeration lists all possible IAU pole effects (constant, secular, harmonics)
 * used to convert from ICRF body centered frame to true equator/rotating body centered frame.
 * This class is used for defining effects alone. They cannot however be used alone for transformation computation.
 * For transformation computation, effects are used cumulatively through {@link GlobalIAUPoleType} class.
 */
public enum IAUPoleType {
    
    /** Constant part only (transformation between ICRF and inertially-oriented frame). */
    CONSTANT,
    
    /** Secular effects (transformation between inertially-oriented and mean equator/rotating frame). */
    SECULAR,
    
    /** Harmonic effects (transformation between mean equator/rotating and true equator/rotating frame). */
    HARMONICS;
}
