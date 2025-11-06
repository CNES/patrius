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
 * @history created 25/09/2015
 *
 * HISTORY
 * VERSION:4.14:OPENFD-:22/08/2024:
 * VERSION:4.14:OPENFD-141:22/08/2024: Isolation des algorithmes de somme et produit precis
 * VERSION:4.14:OPENFD-129:22/08/2024: [PATRIUS] Interpolation de trajectoire avec la methode de Lagrange
 * VERSION:4.13.1:FA:FA-199:17/01/2024:[PATRIUS] Utilisation du dernier point utilisable dans EphemerisPvHermite
 * VERSION:4.13:FA:FA-140:08/12/2023:[PATRIUS] Imprecision numerique dans EphemerisPvLagrange et EphemerisPvHermite
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:484:25/09/2015: Creation to replace LagrangeEphemeris.
 * VERSION::FA:685:16/03/2017:Add the order for Hermite interpolation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexOpenClosed;
import fr.cnes.sirius.patrius.math.utils.ISearchIndex;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class extends {@link fr.cnes.sirius.patrius.orbits.pvcoordinates.AbstractBoundedPVProvider} which implements
 * {@link fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider} and so provides a position velocity for a
 * given date. The provided position velocity is based on a Lagrange interpolation in a given position velocity
 * ephemeris. Tabulated entries are chronologically classified.
 * </p>
 * <p>
 * The interpolation extracts points from the ephemeris depending on the polynome order and the date to interpolate.
 * Points extraction is based on an implementation of the ISearchIndex interface. This implementation should be based on
 * a table of duration created from the date table with the duration = 0 at the first index.
 * </p>
 * 
 * @concurrency not thread-safe
 * @concurrency.comment internal mutable attributes
 * 
 * @author chabaudp
 * 
 * @version $Id: EphemerisPvLagrange.java 17625 2017-05-19 12:06:56Z bignon $
 * 
 * @since 3.1
 * 
 */
public class EphemerisPvLagrange extends AbstractEphemerisPvHermiteLagrange {

    /** Serializable UID. */
    private static final long serialVersionUID = -6755794029652850329L;

    /**
     * Creates an instance of EphemerisPvLagrange
     * 
     * @param tabPV
     *        position velocity coordinates table
     * @param order
     *        interpolation order
     * @param frame
     *        coordinates expression frame
     * @param tabDate
     *        table of dates for each position velocity
     * @param algo
     *        class to find the nearest date index from a given date in the date table.
     *        (If null, algo will be {@link BinarySearchIndexOpenClosed} by default
     *        based on a table of duration since the first date of the dates table)
     * 
     * @throws IllegalArgumentException
     *         if parameters are not consistent,
     *         see {@link AbstractBoundedPVProvider}.
     */
    public EphemerisPvLagrange(final PVCoordinates[] tabPV, final int order,
        final Frame frame, final AbsoluteDate[] tabDate, final ISearchIndex algo) {
        super(tabPV, order, frame, tabDate, algo);
    }

    /**
     * Creates an instance of EphemerisPvLagrange from a spacecraftstate list
     * 
     * @param tabState
     *        Spacecraftstate list
     * @param order
     *        lagrange polynome order
     * @param algo
     *        class to find the nearest date index from a given date in the date table
     *        (If null, algo will be {@link BinarySearchIndexOpenClosed} by default
     *        based on a table of duration since the first date of the dates table)
     * 
     * @throws IllegalArgumentException
     *         if parameters are not consistent,
     *         see {@link AbstractBoundedPVProvider}.
     */
    public EphemerisPvLagrange(final SpacecraftState[] tabState, final int order, final ISearchIndex algo) {
        super(tabState, order, algo);
    }

    /**
     * {@inheritDoc}
     * <br>
     * Note: Frame can be null : by default the frame of expression is the frame used at instantiation
     * (which is the frame of the first spacecraft state when instantiation is done from a table of spacecraft states).
     */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return super.getPVCoordinates(date, frame, null, false);
    }
}
