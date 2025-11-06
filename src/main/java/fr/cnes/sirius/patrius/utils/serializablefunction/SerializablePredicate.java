/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.14:OPENFD-194:22/08/2024: Creation de l'interface SerializablePredicate<T>
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils.serializablefunction;

import java.io.Serializable;
import java.util.function.Predicate;

/**
 * Extension of the {@link Predicate} to specify that these implementations must be serializable.
 *
 * 
 * @param <T>
 *        Type of the input to the predicate
 * @since 4.14
 */
@FunctionalInterface
public interface SerializablePredicate<T> extends Serializable, Predicate<T> {
    // Nothing to do
}
