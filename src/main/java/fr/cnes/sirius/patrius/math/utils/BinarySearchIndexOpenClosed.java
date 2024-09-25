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
 * VERSION::FA:417:12/02/2015: rename AbstractSearchIndex class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.utils;

/**
 * <p>
 * Searches index in a double[] with a coupled dichotomy-BinarySearch algorithms.
 * </p>
 * <p>
 * <b>IMPORTANT</b>: the tab passed in the constructor has to be sorted by increasing order. Duplicates are allowed. If
 * this tab is not sorted, no exception will be thrown, but the results can be totally wrong.
 * </p>
 * The interval convention is set to {@link ISearchIndex.SearchIndexIntervalConvention#OPEN_CLOSED}.
 * 
 * @concurrency immutable
 * 
 * @author Sophie Laurens
 * @version $Id: BinarySearchIndexOpenClosed.java 17583 2017-05-10 13:05:10Z bignon $
 * @since 2.3.1
 * 
 */
public class BinarySearchIndexOpenClosed extends AbstractSearchIndex {

     /** Serializable UID. */
    private static final long serialVersionUID = 6029830130855923200L;

    /**
     * Constructor.
     * 
     * @param tab2
     *        : a double[] array sorted by increasing order. Duplicates are allowed.
     */
    public BinarySearchIndexOpenClosed(final double[] tab2) {
        super(tab2, SearchIndexIntervalConvention.OPEN_CLOSED);
    }

    /**
     * Returns the index of x in a tab with a dichotomy / BinarySearch algorithm under
     * the convention {@link ISearchIndex.SearchIndexIntervalConvention#OPEN_CLOSED}.
     * 
     * @param x
     *        : the value to search.
     * @return index of value that belongs to [0, numberOfPoints-1] that fits the above conditions.
     */
    @Override
    public int getIndex(final double x) {
        return SearchIndexLibrary.binarySearchOpenClosed(this.tab, x, this.iMin, this.iMax);
    }

    /**
     * Returns the index of x in a tab with a dichotomy / BinarySearch algorithm under
     * the convention {@link ISearchIndex.SearchIndexIntervalConvention#OPEN_CLOSED}.
     * 
     * @param x
     *        : the value to search.
     * @param iMin2
     *        : defines the lower index bound of the tab for the search.
     * @param iMax2
     *        : defines the upper index bound of the tab for the search.
     * @return index of value that belongs to [iMin2, iMax2] that fits the above conditions.
     */
    @Override
    public int getIndex(final double x, final int iMin2, final int iMax2) {
        return SearchIndexLibrary.binarySearchOpenClosed(this.tab, x, iMin2, iMax2);
    }

}
