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
 */
package fr.cnes.sirius.patrius.utils.serializablefunction;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Extension of the {@link Function} interface to specify that these implementations must be serializable.
 *
 * @param <T>
 *        The type of the input to the function
 * @param <R>
 *        The type of the result of the function
 *
 * @author veuillh
 * 
 * @since 4.13
 */
@FunctionalInterface
public interface SerializableFunction<T, R> extends Serializable, Function<T, R> {
    // Nothing to do
}
