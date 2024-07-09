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
 * @history created 25/09/2015
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:484:25/09/2015: Creation to replace LagrangeEphemeris.
 * VERSION::FA:685:16/03/2017:Add the order for Hermite interpolation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunctionLagrangeForm;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
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
public class EphemerisPvLagrange extends AbstractBoundedPVProvider {

    /** Ephemeris Lagrange interpolator */
    private PolynomialFunctionLagrangeForm interpolatorPV;

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
     * Frame can be null : by default the frame of expression is the frame used at instantiation
     * (which is the frame of the first spacecraft state when instantiation is done from a table of spacecraft states).
     * 
     * @param date
     *        date of interpolation
     * @param frame
     *        frame of coordinates expression. (can be null)
     * @throws PatriusException
     *         if date of interpolation is too near from min and max input dates
     *         compare to Lagrange order
     * @return PVcoordinates at interpolation date in the chosen frame
     */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        // Duration from reference to search index
        final double duration = date.durationFrom(this.getDateRef());

        // Check if date is exactly on validity interval bounds, in that case returns boundary state
        PVCoordinates interpolPV = this.checkBounds(date);
        if (interpolPV == null) {
            // get the nearest index for this duration
            final int index = this.getSearchIndex().getIndex(duration);
            // the interpolation is valid only if 0<= index +1 -interpoOrder/2 or index + order/2 <= maximalIndex
            final int i0 = this.indexValidity(index);

            // checks if this index has already been considered and stores for future computations
            if (index != this.getPreviousIndex()) {
                this.setPreviousIndex(index);

                // computes the abscissa values (t) and the y values which will be used to compute the Lagrange
                // coefficients
                final double[] tDateExtract = new double[this.polyOrder];
                final double[][] tPVExtract = new double[SpacecraftState.ORBIT_DIMENSION][this.polyOrder];

                // gets the PV coordinates and the delta t from startDate
                for (int i = 0; i < this.polyOrder; i++) {
                    tDateExtract[i] = this.tDate[i0 + i].durationFrom(this.getDateRef());
                    final PVCoordinates currentPV = this.tPVCoord[i0 + i];
                    tPVExtract[0][i] = currentPV.getPosition().getX();
                    tPVExtract[1][i] = currentPV.getPosition().getY();
                    tPVExtract[2][i] = currentPV.getPosition().getZ();
                    tPVExtract[3][i] = currentPV.getVelocity().getX();
                    tPVExtract[4][i] = currentPV.getVelocity().getY();
                    tPVExtract[5][i] = currentPV.getVelocity().getZ();
                }
                this.interpolatorPV = new PolynomialFunctionLagrangeForm(tDateExtract, tPVExtract);
            }

            // computes the interpolated orbit
            final double[] y = new double[SpacecraftState.ORBIT_DIMENSION];

            for (int i = 0; i < SpacecraftState.ORBIT_DIMENSION; i++) {
                y[i] = this.interpolatorPV.valueIndex(i, date.durationFrom(this.getDateRef()));
            }

            // Get the lagrange interpolation results
            final Vector3D p = new Vector3D(y[0], y[1], y[2]);
            final Vector3D v = new Vector3D(y[3], y[4], y[5]);

            interpolPV = new PVCoordinates(p, v);
        }

        // If needed, convert position, velocity to the right frame
        if ((frame != null) && (this.getFrame() != frame)) {
            final Transform t = this.getFrame().getTransformTo(frame, date);
            interpolPV = t.transformPVCoordinates(interpolPV);
        }

        return interpolPV;
    }
}
