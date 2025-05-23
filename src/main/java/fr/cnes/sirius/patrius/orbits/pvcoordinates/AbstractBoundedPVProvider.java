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
 * @history created 12/11/2015
 *
 * HISTORY
 * VERSION:4.13:FA:FA-114:08/12/2023:[PATRIUS] Message d'erreur incomplet
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
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
 * This abstract class shall be extended to provides a PVCoordinates provider based on manipulation of PVCoordinates
 * ephemeris. The method of the implemented interface
 * {@link fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider} is not implemented here and have to be
 * implemented in extending classes to provide a position velocity for a given date.
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

    /** Serializable UID. */
    private static final long serialVersionUID = -8320247409028718135L;

    /** "Greater than" sign. */
    private static final String GREATER_THAN = ">= ";

    /** Position velocity coordinates table. */
    protected final PVCoordinates[] tPVCoord;

    /** Lagrange/Hermite polynomial order. */
    protected final int polyOrder;

    /** Dates table. */
    protected final AbsoluteDate[] tDate;

    /** Class to find for a given date the nearest date index in dates table. */
    private final ISearchIndex searchIndex;

    /** Frame of position velocity coordinates expression. */
    private final Frame pvFrame;

    /** Reference date. */
    private final AbsoluteDate dateRef;

    /** Previous date index used to update interpolator to compute pv coordinate. */
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
     * @throws IllegalArgumentException
     *         if tabPV and tabDate have not the same size or<br>
     *         if parameters are not consistent : order is higher than the length of PV coordinates table or<br>
     *         if order is not even
     */
    public AbstractBoundedPVProvider(final PVCoordinates[] tabPV, final int order, final Frame frame,
                                     final AbsoluteDate[] tabDate, final ISearchIndex algo) {

        checkConsistency(tabPV, tabDate);

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
     * Creates an instance of AbstractBoundedPVProvider from a SpacecraftState table.
     * 
     * @param tabState
     *        SpacecraftState table
     * @param order
     *        Lagrange/Hermite interpolation order. It must be even.
     * @param algo
     *        class to find the nearest date index from a given date
     *        (If null, algo will be BinarySearchIndexOpenClosed by default
     *        based on a table of duration since the first date of the dates table)
     * @throws IllegalArgumentException
     *         if spacecraftState table should contains elements, and if tabacc not null should be of the same size<br>
     *         if tabPV and tabDate have not the same size or<br>
     *         if parameters are not consistent : order is higher than the length of PV coordinates table or<br>
     *         if order is not even or<br>
     *         if order is not even.
     */
    public AbstractBoundedPVProvider(final SpacecraftState[] tabState, final int order, final ISearchIndex algo) {
        final int length = tabState.length;
        if (length <= 0) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INVALID_ARRAY_LENGTH, ">0", length);
        }

        this.tPVCoord = new PVCoordinates[length];
        this.tDate = new AbsoluteDate[length];
        this.pvFrame = tabState[0].getFrame();
        this.dateRef = tabState[0].getDate();
        this.previousIndex = Integer.MIN_VALUE;
        for (int i = 0; i < length; i++) {
            this.tPVCoord[i] = tabState[i].getPVCoordinates();
            this.tDate[i] = tabState[i].getDate();
        }
        checkConsistency(this.tPVCoord, this.tDate);

        // Check if enough points are available regarding the order
        final int lengthPV = this.tPVCoord.length;
        if (lengthPV < order) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.INVALID_ARRAY_LENGTH, GREATER_THAN + order, lengthPV);
        } else if (order % 2 != 0) {
            // Interpolation order has to be even
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
    }

    /**
     * Getter for the reference date.
     * 
     * @return dateRef
     */
    public AbsoluteDate getDateRef() {
        return this.dateRef;
    }

    /**
     * Getter for the reference frame.
     * 
     * @return pvFrame
     */
    public Frame getFrame() {
        return this.pvFrame;
    }

    /**
     * Getter for the optimize index search algorithm.
     * 
     * @return searchIndex
     */
    public ISearchIndex getSearchIndex() {
        return this.searchIndex;
    }

    /**
     * Getter for the previous search index.
     * 
     * @return previousIndex
     */
    public int getPreviousIndex() {
        return this.previousIndex;
    }

    /**
     * Setter for the previous search index
     * 
     * @param index
     *        previous search index
     */
    protected void setPreviousIndex(final int index) {
        this.previousIndex = index;
    }

    /**
     * Checks if interpolation is valid : meaning if {@code 0<= index +1 -interpOrder/2} or
     * {@code index + interpOrder/2 <= maximalIndex}.
     * 
     * @param index
     *        the closest index from a given date
     * @return the first interpolation point {@code i0}, such as {@code i0 = index + 1 - order / 2}
     * @throws PatriusException
     *         if the index does not allow enough points to do the interpolation
     */
    public int indexValidity(final int index) throws PatriusException {

        // factor reused several times
        final int k = this.polyOrder / 2;

        // Check if there is enough points on the lower bound
        final int i0 = index + 1 - k;
        if (i0 < 0) {
            throw new PatriusException(PatriusMessages.NOT_ENOUGH_INTERPOLATION_POINTS,
                index + 1 + k, this.polyOrder);
        }
        // Check if there is enough points on the upper bound
        final int maximalIndex = this.tDate.length - 1;
        final int in = index + k;
        if (in > maximalIndex) {
            throw new PatriusException(PatriusMessages.NOT_ENOUGH_INTERPOLATION_POINTS,
                maximalIndex - index + k, this.polyOrder);
        }

        return i0;
    }

    /**
     * Return the lower date authorized to call {@link #getPVCoordinates(AbsoluteDate, Frame) getPVCoordinates}.
     * 
     * @return minimum ephemeris date
     */
    public AbsoluteDate getMinDate() {
        return this.tDate[(this.polyOrder / 2) - 1];
    }

    /**
     * Return the higher date authorized to call {@link #getPVCoordinates(AbsoluteDate, Frame) getPVCoordinates}.
     * 
     * @return maximum ephemeris date
     */
    public AbsoluteDate getMaxDate() {
        return this.tDate[this.tDate.length - (this.polyOrder / 2)];
    }

    /**
     * Check consistency of constructor parameters : {@code tabPV} and {@code tabDate} size shall be equals.
     * 
     * @param tabPV
     *        position velocity table
     * @param tabDate
     *        dates table
     */
    private static void checkConsistency(final PVCoordinates[] tabPV, final AbsoluteDate[] tabDate) {

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
     * @return PV Coordinates on bounds if duration is on bounds, null otherwise
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

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
        return this.pvFrame;
    }
}
