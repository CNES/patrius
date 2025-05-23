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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

/**
 * This interface represents objects that have a {@link AbsoluteDate} date attached to them.
 * <p>
 * Classes implementing this interface can be stored chronologically in sorted sets using
 * {@link ChronologicalComparator} as the underlying comparator. An example using for
 * {@link fr.cnes.sirius.patrius.orbits.Orbit
 * Orbit} instances is given here:
 * </p>
 * 
 * <pre>
 *     SortedSet&lt;Orbit> sortedOrbits =
 *         new TreeSet&lt;Orbit>(new ChronologicalComparator());
 *     sortedOrbits.add(orbit1);
 *     sortedOrbits.add(orbit2);
 *     ...
 * </pre>
 * <p>
 * This interface is also the base interface used to {@link fr.cnes.sirius.patrius.time.TimeStampedCache cache} series
 * of time-dependent objects for interpolation in a thread-safe manner.
 * </p>
 * 
 * @see AbsoluteDate
 * @see ChronologicalComparator
 * @see fr.cnes.sirius.patrius.time.TimeStampedCache
 * @author Luc Maisonobe
 */
public interface TimeStamped {

    /**
     * Get the date.
     * 
     * @return date attached to the object
     */
    AbsoluteDate getDate();

}
