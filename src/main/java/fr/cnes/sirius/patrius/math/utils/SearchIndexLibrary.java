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
 * 
 * @history Created 19/12/2014
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:386:19/12/2014: (creation) index mutualisation for ephemeris interpolation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.utils;

import fr.cnes.sirius.patrius.math.utils.ISearchIndex.SearchIndexIntervalConvention;

/**
 * Index search algorithms based on method BinarySearch, coupled with dichotomy.<br/>
 * Contains two algorithms for both intervals conventions defined by {@link SearchIndexIntervalConvention}.<br/>
 * Also contains utility functions.
 * 
 * @concurrency immutable
 * @author Sophie Laurens
 * @version $Id: SearchIndexLibrary.java 17583 2017-05-10 13:05:10Z bignon $
 * @since 2.3.1
 */
public final class SearchIndexLibrary {
    
    /** -2 */
    private static final int MINUS_TWO = -2;

    /** Private constructor. */
    private SearchIndexLibrary() {
        super();
    }

    /**
     * Returns the index of a middle point of segment [iMin, iMax]
     * 
     * @param iMin
     *        : the lower bound of the interval
     * @param iMax
     *        : the upper bound of the interval
     * 
     * @return ((iMax + iMin) / 2)
     */
    public static int midPoint(final int iMin, final int iMax) {
        // (iMax + iMin) >>> 1 is equivalent to ((iMax + iMin) / 2) if iMax and iMin are positive
        return (iMax + iMin) >>> 1;
    }

    /**
     * Searches the index corresponding to the parameter key inside [tab[iMin], tab[iMax]]
     * with the default convention CLOSED_OPEN, meaning that interval considered are
     * [ tab[i], tab[i+1] [ and therefore, if x = tab[i], the integer returned is i.
     * No exception is thrown if key does not belong to [tab[iMin], tab[iMax]].
     * 
     * @precondition the double[] tab is already sorted in an increasing order. Duplicates are allowed.
     * 
     * @param tab
     *        : the double[] already sorted.
     * @param key
     *        : the double searched inside [tab[iMin], tab[iMax]]
     * @param iMin
     *        : the lower bound of tab considered. If key < tab[iMin], the integer returned is iMin - 1
     * @param iMax
     *        : the lower bound of tab considered. If key >=tab[iMax], the integer returned is iMax.
     *        As a consequence, a method using an index returned by binarySearchClosedOpen should consider two
     *        behavior
     *        if index = iMax. It can be either key = tab[iMax] or key > tab[iMax].
     * 
     * @return index : an integer that belongs to [iMin - 1, iMax]
     */
    public static int binarySearchClosedOpen(final double[] tab, final double key,
                                             final int iMin, final int iMax) {

        // initialization
        int index = MINUS_TWO;
        boolean isOutsideInterval = false;

        // by convention, if key does not belong to [tab[iMin], tab[iMax]], an index is still returned
        if (key < tab[iMin]) {
            // the index returned is minus one the minimal one : no confusion can occured for interpolation
            index = iMin - 1;
            isOutsideInterval = true;
        }
        if (key >= tab[iMax]) {
            // for users, if iMax is returned, it means either that key = tab[iMax] or key > tab[iMax]
            // (and does not belong to the interval allowed in the first place)
            index = iMax;
            isOutsideInterval = true;
        }

        // for quality reason, no more than 3 returns allowed
        if (isOutsideInterval) {
            return index;
        }

        // temporary variables for the dichotomy
        int min = iMin;
        int max = iMax;

        // continue searching while [min,max] is not empty
        while (max - min > 1) {
            // calculate the midpoint for roughly equal partition
            final int mid = midPoint(min, max);
            if (tab[mid] <= key) {
                // change min index to search in the upper subarray
                min = mid;
            } else {
                // change max index to search in the lower subarray
                max = mid;
            }
        }
        return min;
    }

    /**
     * Searches the index corresponding to the parameter key inside [tab[iMin], tab[iMax]]
     * with the convention OPEN_CLOSED, meaning that interval considered are
     * ] tab[i], tab[i+1] ] and therefore, if x = tab[i], the integer returned is i-1.
     * No exception is thrown if key does not belong to [tab[iMin], tab[iMax]].
     * 
     * @precondition the double[] tab is already sorted in an increasing order. Duplicates are allowed.
     * 
     * @param tab
     *        : the double[] already sorted.
     * @param key
     *        : the double searched inside [tab[iMin], tab[iMax]]
     * @param iMin
     *        : the lower bound of tab considered. If key <= tab[iMin], the integer returned is iMin - 1
     * @param iMax
     *        : the lower bound of tab considered. If key > tab[iMax], the integer returned is iMax.
     *        As a consequence, a method using an index returned by binarySearchClosedOpen should consider two
     *        behavior
     *        if index = iMin - 1. It can be either key = tab[iMin] or key < tab[iMin]
     * 
     * @return index : an integer that belongs to [iMin - 1, iMax]
     */
    public static int binarySearchOpenClosed(final double[] tab, final double key,
                                             final int iMin, final int iMax) {

        // initialization
        int index = MINUS_TWO;
        boolean isOutsideInterval = false;

        // by convention, if key does not belong to [tab[iMin], tab[iMax]], an index is still returned
        if (key <= tab[iMin]) {
            // for users, if iMin - 1 is returned, it means either that key = tab[iMin] or key < tab[iMin]
            // (and does not belong to the interval allowed in the first place)
            index = iMin - 1;
            isOutsideInterval = true;
        }
        if (key > tab[iMax]) {
            // the index returned is the maximal one : no confusion can occured for interpolation
            index = iMax;
            isOutsideInterval = true;
        }

        // for quality reason, no more than 3 returns allowed
        if (isOutsideInterval) {
            return index;
        }

        // temporary variables for the dichotomy
        int min = iMin;
        int max = iMax;

        // continue searching while [min,max] is not empty
        while (max - min > 1) {
            // calculate the midpoint for roughly equal partition
            final int mid = midPoint(min, max);
            if (tab[mid] < key) {
                // change min index to search to the upper subarray
                min = mid;
            } else {
                // change max index to search to the lower subarray
                max = mid;
            }
        }
        return min;
    }

}
