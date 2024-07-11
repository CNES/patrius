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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:386:19/12/2014: (creation) index mutualisation for ephemeris interpolation
 * VERSION::FA:417:12/02/2015: rename AbstractSearchIndex class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.utils;

/**
 * <p>
 * Abstract class for index search algorithm with a coupled dichotomy-BinarySearch algorithms.
 * </p>
 * <p>
 * <b>IMPORTANT</b>: the tab passed in the constructor has to be sorted by increasing order. Duplicates are allowed. If
 * this tab is not sorting, no exception will be thrown, but the results can be totally wrong.
 * </p>
 * Each implementation of this class defines a convention of SearchIndexIntervalConvention.
 * 
 * @concurrency not thread-safe
 * 
 * @author Sophie Laurens
 * @version $Id: AbstractSearchIndex.java 17583 2017-05-10 13:05:10Z bignon $
 * @since 2.3.1
 * 
 */
public abstract class AbstractSearchIndex implements ISearchIndex {

    /** Serial UID. */
    private static final long serialVersionUID = 4044613988930420114L;

    /** The values in with the search algorithm will be executed. */
    protected double[] tab;
    /** The lower bound of tab to consider for the search index algorithm. By default, iMin = 0. */
    protected int iMin;
    /** The upper bound of tab to consider for the search index algorithm. By default, iMax = tab.length - 1. */
    protected int iMax;
    /** The convention for the definition of intervals. */
    protected SearchIndexIntervalConvention convention;

    /**
     * Default constructor with default CLOSED_OPEN convention.
     * 
     * @precondition : tab is already sorted by increasing order. Duplicates are allowed.
     * 
     * @param tab2
     *        : a double[] array sorted by increasing order. Duplicates are allowed.
     */
    public AbstractSearchIndex(final double[] tab2) {
        this(tab2, SearchIndexIntervalConvention.CLOSED_OPEN);
    }

    /**
     * Constructor with defined interval convention.
     * 
     * @precondition : tab is already sorted by increasing order. Duplicates are allowed.
     * 
     * @param tab2
     *        : a sorted double[]
     * @param searchIndexConvention
     *        interval convention
     */
    public AbstractSearchIndex(final double[] tab2, final SearchIndexIntervalConvention searchIndexConvention) {
        this.tab = tab2;
        this.iMin = 0;
        this.iMax = this.tab.length - 1;
        this.convention = searchIndexConvention;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getTab() {
        return this.tab;
    }

    /** {@inheritDoc} */
    @Override
    public SearchIndexIntervalConvention getConvention() {
        return this.convention;
    }

}
