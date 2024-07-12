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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time.interpolation;

import java.io.Serializable;
import java.util.function.Function;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;

/**
 * Interface representing a class that can build an interpolation function from an array of Time stamped samples.
 *
 * @param <IN>
 *        The type of the samples to be interpolated
 * @param <OUT>
 *        The type of the interpolation result.<br>
 *        For generality sake, it can be different from IN (for example, we can interpolate a sub-data of IN).
 * @author veuillh
 */
@FunctionalInterface
public interface TimeStampedInterpolationFunctionBuilder<IN extends TimeStamped, OUT> extends Serializable {

    /**
     * Builds an interpolation function with the provided samples within the provided indexes.
     * 
     * @param samples
     *        The array of time stamped data to be interpolated. The samples can be considered strictly increasingly
     *        sorted
     * @param indexInf
     *        The inferior index (included) to be considered to build the interpolation function
     * @param indexSup
     *        The superior index (excluded) to be considered to build the interpolation function
     * @return the interpolation function
     */
    Function<AbsoluteDate, ? extends OUT> buildInterpolationFunction(IN[] samples, int indexInf, int indexSup);
}
