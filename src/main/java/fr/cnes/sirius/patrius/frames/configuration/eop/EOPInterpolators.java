/**
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
 * @history creation 28/06/2012
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

/**
 * This enumerate lists available interpolators for EOP data.
 * 
 * @author Anton Julie
 */
public enum EOPInterpolators {
    /**
     * Fourth order Lagrange polynomial interpolator.
     */
    LAGRANGE4,
    /**
     * Linear interpolator.
     */
    LINEAR;
}