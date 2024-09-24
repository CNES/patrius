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
 * Search index algorithm with previous index computation storage. This allows, in case of multiple calls
 * in a close neighborhood (stencil with 4 points) to optimize the search by memorizing the last index found
 * and search around it (+/- 2 points).
 * 
 * @concurrency immutable
 * 
 * @author Sophie Laurens
 * @version $Id: RecordSegmentSearchIndex.java 17583 2017-05-10 13:05:10Z bignon $
 * @since 2.3.1
 * 
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class RecordSegmentSearchIndex extends AbstractSearchIndex {

     /** Serializable UID. */
    private static final long serialVersionUID = 371842648992526687L;

    /** Plus infinity. */
    private static final double PLUS_INF = Double.POSITIVE_INFINITY;
    /** Plus infinity. */
    private static final double MINUS_INF = Double.NEGATIVE_INFINITY;

    /** Search index algorithm. Can be any implementation of ISearchIndex. */
    private final ISearchIndex searchAlgorithm;
    /** Index for computation. */
    private int currentIndex;
    /** Equals false if the number of points is less than 2, because there is no need for this algorithm. */
    private boolean isRecordActive = true;
    /** xim1 = tab[i-1]. */
    private double xim1;
    /** xi = tab[i]. */
    private double xi;
    /** xip1 = tab[i+1]. */
    private double xip1;
    /** xip2 = tab[i+2]. */
    private double xip2;

    /**
     * Constructor of search index class with memorization of the last found index in order to optimize
     * the search and find the index quickly if it belongs to a close neighborhood of the last one (+/- 2 points).
     * 
     * @param searchAlgorithm2
     *        : the choice of algorithm is made directly through an instance of
     *        a class that implements ISearchIndex
     */
    public RecordSegmentSearchIndex(final ISearchIndex searchAlgorithm2) {
        super(searchAlgorithm2.getTab());

        this.searchAlgorithm = searchAlgorithm2;
        this.convention = this.searchAlgorithm.getConvention();
        final int numberOfPoints = this.tab.length;

        // if the number of points is less than 2, no need for this algorithm
        if (numberOfPoints <= 2) {
            this.isRecordActive = false;
        } else {
            this.isRecordActive = true;
            // initialization
            this.currentIndex = SearchIndexLibrary.midPoint(0, numberOfPoints - 1);
            // updates indices for stencil (4 points, i-1, i, i+1, i+2)
            this.updateStencil();
        }
    }

    /**
     * The search algorithm is based on a four-point stencil.
     * To take into account the boundary condition, we have : </br>
     * - if i = - 1 (point outside domain by lower bound), xi-1 = xi = - inf </br>
     * - if i = 0 (lower bound), xi-1 = - inf </br>
     * - if i = nMax - 1, xi+2 = + inf </br>
     * - if i = nMax, xi+2 = xi+1 = + inf
     * 
     * @since 2.3.1
     */
    public void updateStencil() {

        // get current index
        final int i = this.currentIndex;
        // get the number of values
        final int nMax = this.tab.length - 1;

        // if the mid Point is outside the domain, by lower bound, only xi+1 and xi+2 have a value in tab
        if (i == -1) {
            this.xim1 = MINUS_INF;
            this.xi = MINUS_INF;
            this.xip1 = this.tab[i + 1];
            this.xip2 = this.tab[i + 2];
        } else if (i == 0) {
            // if the mid Point is the lower bound, only xi-1 is set to -inf
            this.xim1 = MINUS_INF;
            this.xi = this.tab[i];
            this.xip1 = this.tab[i + 1];
            this.xip2 = this.tab[i + 2];
        } else if (i == nMax - 1) {
            // for the last - 1 element of tab, only xi+2 is set to +inf
            this.xim1 = this.tab[i - 1];
            this.xi = this.tab[i];
            this.xip1 = this.tab[i + 1];
            this.xip2 = PLUS_INF;
        } else if (i == nMax) {
            // for the last element of tab, only xi-1 et xi have a value. The other points are set to +inf
            this.xim1 = this.tab[i - 1];
            this.xi = this.tab[i];
            this.xip1 = PLUS_INF;
            this.xip2 = PLUS_INF;
        } else {
            // regular stencil
            this.xim1 = this.tab[i - 1];
            this.xi = this.tab[i];
            this.xip1 = this.tab[i + 1];
            this.xip2 = this.tab[i + 2];
        }
    }

    /**
     * Returns the index of x in a tab depending on the convention used. </br>
     * 
     * Therefore no exception is returned even if x does not belong to [tab[0], tab[numberOfPoints - 1] ] </br>
     * - if x = tab[i], the integer returned is i
     * - if tab[i] < x < tab[i + 1], the integer returned is i
     * - if x < tab[0], the integer returned is -1
     * - if x >= tab[numberOfPoints-1], the integer returned is numberOfPoints-1
     * 
     * @param x
     *        : the value to search.
     * @return the corresponding index, see the above conditions.
     */
    public int getIndexClosedOpen(final double x) {
        // initialization, index = i if xi <= x < xi+1
        int indexRetour = this.currentIndex;

        // we cut the 4-points stencil into half, delimited by xi
        // first part : strictly inferior to xi
        if (x < this.xi) {
            // if xi-1 <= x < xi, index = i - 1
            if (x >= this.xim1) {
                indexRetour = this.currentIndex - 1;
            } else {
                // if x < xi-1, index is searched in [0, i-1[
                indexRetour = this.searchAlgorithm.getIndex(x, 0, this.currentIndex - 1);
            }
        } else if (x >= this.xip1) {
            // if xi <= x < xi+1, then index = i, which is already the case
            // second part : superior to xi+1

            // if xi+1 <= x < xi+2, index = i + 1
            if (x < this.xip2) {
                indexRetour = this.currentIndex + 1;
            } else {
                // if x => xi+2, index is searched in [i+2, n]
                indexRetour = this.searchAlgorithm.getIndex(x, this.currentIndex + 2, this.tab.length - 1);
            }
        }
        return indexRetour;
    }

    /**
     * Returns the index of x in a tab depending on the convention used. </br>
     * 
     * Therefore no exception is returned even if x does not belong to [tab[0], tab[numberOfPoints - 1] ] </br>
     * - if x = tab[i], the integer returned is i - 1 </br>
     * - if tab[i] < x < tab[i + 1], the integer returned is i </br>
     * - if x <= tab[0], the integer returned is -1 </br>
     * - if x > tab[numberOfPoints-1], the integer returned is numberOfPoints-1
     * 
     * @param x
     *        : the value to search.
     * @return the corresponding index, see the above conditions.
     */
    public int getIndexOpenClosed(final double x) {
        // initialization, index = i if xi < x <= xi+1
        int indexRetour = this.currentIndex;

        // we cut the 4-points stencil into half, delimited by xi
        // first part : inferior to xi
        if (x <= this.xi) {
            // if xi-1 < x <= xi, index = i - 1
            if (x > this.xim1) {
                indexRetour = this.currentIndex - 1;
            } else {
                // if x < xi-1, index is searched in [0, i-1[
                indexRetour = this.searchAlgorithm.getIndex(x, 0, this.currentIndex - 1);
            }
        } else if (x > this.xip1) {
            // if xi < x <= xi+1, then index = i, which is already the case
            // second part : strictly superior to xi+1

            // if xi+1 < x <= xi+2, index = i + 1
            if (x <= this.xip2) {
                indexRetour = this.currentIndex + 1;
            } else {
                // if x > xi+2, index is searched in [i+2, n]
                indexRetour = this.searchAlgorithm.getIndex(x, this.currentIndex + 2, this.tab.length - 1);
            }
        }
        return indexRetour;
    }

    /** {@inheritDoc} */
    @Override
    public int getIndex(final double x) {

        // if not enough points for the memorization to be necessary, do a classic index search
        if (!this.isRecordActive) {
            return this.searchAlgorithm.getIndex(x, 0, this.tab.length - 1);
        }
        // set current index depending on the convention
        switch (this.convention) {
            case CLOSED_OPEN:
                this.currentIndex = this.getIndexClosedOpen(x);
                break;
            case OPEN_CLOSED:
                this.currentIndex = this.getIndexOpenClosed(x);
                break;
            default:
                break;
        }
        // updates indices for stencil (4 points, i-1, i, i+1, i+2)
        this.updateStencil();
        return this.currentIndex;
    }

    /** {@inheritDoc} */
    @Override
    public int getIndex(final double x, final int min, final int max) {
        // initialization
        this.currentIndex = SearchIndexLibrary.midPoint(min, max);
        // updates indices for stencil (4 points, i-1, i, i+1, i+2)
        this.updateStencil();
        return this.getIndex(x);
    }

}
