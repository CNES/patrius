/**
 *
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
 * @history Created 19/12/2014
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:386:19/12/2014: (creation) index mutualisation for ephemeris interpolation
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::FA:417:12/02/2015:suppressed input tab clone
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.utils;

import java.io.Serializable;

/**
 * <p>
 * Interfaces for optimized indices search algorithm.
 * </p>
 * 
 * <p>
 * Each class that implements this interface will have at least two constructors : - one with a double[] as a parameter
 * and an enumerate SearchIndexIntervalConvention - one with a double[] as a parameter. In that case, the enumerate
 * SearchIndexIntervalConvention is set to CLOSED_OPEN by default.
 * </p>
 * 
 * <p>
 * <b>IMPORTANT</b>: the input double[] should already be sorted by increasing order. If this input tab is not sorted,
 * no exception will be thrown, but the results can be totally wrong. Duplicates are allowed.
 * </p>
 * 
 * <p>
 * The input double[] should not be modified by the interface implementations.
 * </p>
 * 
 * <p>
 * The enumerate {@link SearchIndexIntervalConvention} defines the implementing of the method getIndex that should
 * respect the following convention :
 * </p>
 * CLOSED_OPEN (default case)
 * <ul>
 * <li>if tab[i] <= x < tab[i + 1], the integer returned is i</li>
 * <li>if x < tab[0], the integer returned is -1</li>
 * <li>if x >= tab[numberOfPoints-1], the integer returned is numberOfPoints-1</li>
 * </ul>
 * OPEN_CLOSED
 * <ul>
 * <li>if tab[i] < x <= tab[i + 1], the integer returned is i</li>
 * <li>if x <= tab[0], the integer returned is -1</li>
 * <li>if x > tab[numberOfPoints-1], the integer returned is numberOfPoints-1</li>
 * </ul>
 * 
 * @author Sophie Laurens
 * @version $Id: ISearchIndex.java 17583 2017-05-10 13:05:10Z bignon $
 * @since 2.3.1
 */
public interface ISearchIndex extends Serializable {

    /**
     * Describes the shape of an interval.
     */
    public enum SearchIndexIntervalConvention {
        /**
         * (default case)
         * <ul>
         * <li>if tab[i] <= x < tab[i + 1], the integer returned is i</li>
         * <li>if x < tab[0], the integer returned is -1</li>
         * <li>if x >= tab[numberOfPoints-1], the integer returned is numberOfPoints-1</li>
         * </ul>
         */
        CLOSED_OPEN,
        /**
         * <ul>
         * <li>if tab[i] < x <= tab[i + 1], the integer returned is i</li>
         * <li>if x <= tab[0], the integer returned is -1</li>
         * <li>if x > tab[numberOfPoints-1], the integer returned is numberOfPoints-1</li>
         * </ul>
         */
        OPEN_CLOSED;
    }

    /**
     * Returns the index of x in a tab depending on the convention used. </br>
     * 
     * Therefore no exception is returned even if x does not belong to [tab[0], tab[numberOfPoints - 1] ] </br>
     * 
     * @precondition : tab is already sorted by increasing order. Duplicates are allowed.
     * 
     * @param x
     *        : the value to search.
     * @return the corresponding index, see the above conditions.
     */
    int getIndex(final double x);

    /**
     * Returns the index of x in the extracted tab [tab[iMin], tab[iMax]]
     * depending on the convention used.
     * 
     * @precondition : tab is already sorted by increasing order. Duplicates are allowed.
     * 
     * @param x
     *        : the value to search.
     * @param iMin
     *        : defines the lower bound of the tab for the search.
     * @param iMax
     *        : defines the upper bound of the tab for the search.
     * @return the corresponding index, see the above conditions.
     */
    int getIndex(final double x, final int iMin, final int iMax);

    /**
     * Returns the convention that can be applied to the interval during the search index algorithm.
     * Describes the boundaries of each interval defined by tab.
     * 
     * @return CLOSED_OPEN if intervals are [tab[i], tab[i+1][ or OPEN_CLOSED for ]tab[i], tab[i+1]].
     */
    SearchIndexIntervalConvention getConvention();

    /**
     * Returns the array of values.
     * 
     * @return tab
     */
    double[] getTab();
}
