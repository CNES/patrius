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
 * @history created 12/11/2015
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:484:12/11/2015: Creation.
 * VERSION::FA:685:16/03/2017:Add the order for Hermite interpolation
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexClosedOpen;
import fr.cnes.sirius.patrius.math.utils.ISearchIndex;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * 
 * <p>
 * This abstract class shall be extended to provides a PVCoordinates provider based on manipulation of PVCoordinates
 * ephemeris. The method of the implemented interface
 * {@link fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider} is not implemented here and have to be
 * implemented in extending classes to provide a position velocity for a given date.
 * </p>
 * 
 * @concurrency not thread-safe
 * @concurrency.comment internal mutable attributes
 * 
 * @author chabaudp
 * 
 * @version $Id: AbstractBoundedPVProvider.java 17625 2017-05-19 12:06:56Z bignon $
 * 
 * @since 3.1
 * 
 */
public abstract class AbstractBoundedPVProvider implements PVCoordinatesProvider {

    /** "Greater than" sign. */
    private static final String GREATER_THAN = ">= ";
    /** Position velocity coordinates table */
    protected final PVCoordinates[] tPVCoord;
    /** Lagrange/Hermite polynomial order */
    protected final int polyOrder;
    /** Dates table */
    protected final AbsoluteDate[] tDate;
    /** Class to find for a given date the nearest date index in dates table */
    private final ISearchIndex searchIndex;
    /** Frame of position velocity coordinates expression */
    private final Frame pvFrame;
    /** Reference date */
    private final AbsoluteDate dateRef;
    /** Previous date index used to update interpolator to compute pv coordinate */
    private int previousIndex;

    /**
     * Instantiation of AbstractBoundedPVProvider attributes.
     * 
     * @param tabPV
     *        position velocity coordinates table
     *        (table is not copied and so internal class state can be modified from outside)
     * @param order
     *        Lagrange/Hermite interpolation order. It must be even.
     * @param frame
     *        coordinates expression frame
     * @param tabDate
     *        table of dates for each position velocity
     *        (table is not copied and so internal class state can be modified from outside)
     * @param algo
     *        class to find the nearest date index from a given date in the date table
     *        (If null, algo will be, by default, a BinarySearchIndexOpenClosed
     *        based on a table of duration since the first date of the dates table)
     * @exception IllegalArgumentException
     *            if :
     *            - tabPV and tabDate have not the same size
     *            - parameters are not consistent : order is higher than the length of PV coordinates table
     *            - order is not even
     * @since 3.1
     */
    public AbstractBoundedPVProvider(final PVCoordinates[] tabPV, final int order, final Frame frame,
        final AbsoluteDate[] tabDate, final ISearchIndex algo) {

        this.checkConsistency(tabPV, tabDate);

        this.tPVCoord = tabPV;
        // Check if enough points are available regarding the order
        final int lengthPV = this.tPVCoord.length;
        if (lengthPV < order) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.INVALID_ARRAY_LENGTH, GREATER_THAN + order, lengthPV);
            // Interpolation order has to be even
        } else if (order % 2 != 0) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.ODD_INTERPOLATION_ORDER);
        }

        this.polyOrder = order;
        this.tDate = tabDate;
        this.pvFrame = frame;
        this.dateRef = tabDate[0];
        this.previousIndex = Integer.MIN_VALUE;

        if (algo == null) {
            final double[] tabIndex = new double[tabDate.length];
            for (int i = 0; i < tabDate.length; i++) {
                tabIndex[i] = tabDate[i].durationFrom(tabDate[0]);
            }
            this.searchIndex = new BinarySearchIndexClosedOpen(tabIndex);
        } else {
            this.searchIndex = algo;
        }

    }

    /**
     * Creates an instance of AbstractBoundedPVProvider from a SpacecraftState table
     * 
     * @param tabState
     *        SpacecraftState table
     * @param order
     *        Lagrange/Hermite interpolation order. It must be even.
     * @param algo
     *        class to find the nearest date index from a given date
     *        (If null, algo will be BinarySearchIndexOpenClosed by default
     *        based on a table of duration since the first date of the dates table)
     * @exception IllegalArgumentException
     *            if :
     *            - spacecraftState table should contains elements, and if tabacc not
     *            null should be of the same size.
     *            - tabPV and tabDate have not the same size
     *            - parameters are not consistent : order is higher than the length of PV coordinates table
     *            - order is not even
     *            Exception thrown also if order is not even.
     * @since 3.1
     */
    public AbstractBoundedPVProvider(final SpacecraftState[] tabState,
        final int order, final ISearchIndex algo) {
        final int length = tabState.length;
        if (length > 0) {
            this.tPVCoord = new PVCoordinates[length];
            this.tDate = new AbsoluteDate[length];
            this.pvFrame = tabState[0].getFrame();
            this.dateRef = tabState[0].getDate();
            this.previousIndex = Integer.MIN_VALUE;
            for (int i = 0; i < length; i++) {
                this.tPVCoord[i] = tabState[i].getPVCoordinates();
                this.tDate[i] = tabState[i].getDate();
            }
            this.checkConsistency(this.tPVCoord, this.tDate);

            // Check if enough points are available regarding the order
            final int lengthPV = this.tPVCoord.length;
            if (lengthPV < order) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.INVALID_ARRAY_LENGTH, GREATER_THAN + order, lengthPV);
                // Interpolation order has to be even
            } else if (order % 2 != 0) {
                throw PatriusException.createIllegalArgumentException(PatriusMessages.ODD_INTERPOLATION_ORDER);
            }

            this.polyOrder = order;
            if (algo == null) {
                final double[] tabIndex = new double[this.tDate.length];
                for (int i = 0; i < this.tDate.length; i++) {
                    tabIndex[i] = this.tDate[i].durationFrom(this.tDate[0]);
                }
                this.searchIndex = new BinarySearchIndexClosedOpen(tabIndex);
            } else {
                this.searchIndex = algo;
            }
        } else {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.INVALID_ARRAY_LENGTH, ">0", length);
        }
    }

    /**
     * Get the reference date
     * 
     * @return dateRef
     * @since 3.1
     */
    public AbsoluteDate getDateRef() {
        return this.dateRef;
    }

    /**
     * Get the reference frame
     * 
     * @return pvFrame
     * @since 3.1
     */
    public Frame getFrame() {
        return this.pvFrame;
    }

    /**
     * Get the optimize indice search algorithm
     * 
     * @return searchIndex
     * @since 3.1
     */
    public ISearchIndex getSearchIndex() {
        return this.searchIndex;
    }

    /**
     * Get the previous search index
     * 
     * @return previousIndex
     * @since 3.1
     */
    public int getPreviousIndex() {
        return this.previousIndex;
    }

    /**
     * Set the previous search index
     * 
     * @param index
     *        previous search index
     * @since 3.1
     */
    protected void setPreviousIndex(final int index) {
        this.previousIndex = index;
    }

    /**
     * Checks if interpolation is valid : meaning if 0<= index +1 -interpOrder/2 or index + interpOrder/2 <=
     * maximalIndex
     * 
     * @param index
     *        : the closest index from a given date.
     * @return i0
     *         : i0 = index + 1 - order / 2. is the first interpolation point.
     * @throws PatriusException
     *         if the index does not allow enough point to do the interpolation.
     */
    public int indexValidity(final int index) throws PatriusException {

        final int maximalIndex = this.tDate.length - 1;
        final int i0 = index + 1 - this.polyOrder / 2;
        final int in = index + this.polyOrder / 2;
        if (i0 < 0 || in > maximalIndex) {
            throw new PatriusException(PatriusMessages.NOT_ENOUGH_INTERPOLATION_POINTS);
        }

        return i0;
    }

    /**
     * Return the lower date authorized to call getPVCoordinates.
     * 
     * @return minimum ephemeris date
     */
    public AbsoluteDate getMinDate() {
        return this.tDate[(this.polyOrder / 2) - 1];
    }

    /**
     * Return the higher date authorized to call getPVCoordinates.
     * 
     * @return maximum ephemeris date
     */
    public AbsoluteDate getMaxDate() {
        return this.tDate[this.tDate.length - (this.polyOrder / 2)];
    }

    /**
     * Check consistency of constructor parameters :
     * tabPV and tabDate size shall be equals
     * 
     * @param tabPV
     *        position velocity table
     * @param tabDate
     *        dates table
     * @since 3.1
     */
    private void checkConsistency(final PVCoordinates[] tabPV,
                                  final AbsoluteDate[] tabDate) {

        final int lengthPV = tabPV.length;
        final int lengthDate = tabDate.length;

        if (lengthDate != lengthPV) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.INVALID_ARRAY_LENGTH, lengthDate, lengthPV);
        }

    }

    /**
     * Returns PV Coordinates on bounds if duration is on bounds.
     * 
     * @param date
     *        duration
     * @return PV Coordinates on bounds if duration is on bounds, null otherwise.
     */
    protected PVCoordinates checkBounds(final AbsoluteDate date) {

        PVCoordinates res = null;

        // Special case: date is exactly on validity interval bounds
        // This case is handled specifically since none of interval search convention is able to handle it
        final int n = this.polyOrder / 2 - 1;

        // Lower bound
        if (date.durationFrom(this.getMinDate()) == 0) {
            res = this.tPVCoord[n];
        }

        // Upper bound
        if (date.durationFrom(this.getMaxDate()) == 0) {
            res = this.tPVCoord[this.tPVCoord.length - 1 - n];
        }

        return res;
    }

}
