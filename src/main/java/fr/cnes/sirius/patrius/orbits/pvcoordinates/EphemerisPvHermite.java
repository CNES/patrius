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
 * VERSION::DM:484:25/09/2015: Creation to replace HermiteEphemeris.
 * VERSION::FA:685:16/03/2017:Add the order for Hermite interpolation
 * VERSION::FA:1179:01/09/2017:documentation PATRIUSv3.4
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.interpolation.HermiteInterpolator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexOpenClosed;
import fr.cnes.sirius.patrius.math.utils.ISearchIndex;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This class extends {@link fr.cnes.sirius.patrius.orbits.pvcoordinates.AbstractBoundedPVProvider} which implements
 * {@link fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider} and so provides a position velocity for a
 * given date. The provided position velocity is based on a Hermite interpolation in a given position velocity
 * ephemeris.
 * </p>
 * <p>
 * The interpolation extracts position, velocity and eventually acceleration from the ephemeris depending on the number
 * of sample points and the date to interpolate.
 * 
 * Points extraction is based on an implementation of the ISearchIndex interface. This implementation should be based on
 * a table of duration created from the date table with the duration = 0 at the first index.
 * </p>
 * 
 * @concurrency not thread-safe
 * @concurrency.comment internal mutable attributes
 * 
 * @author chabaudp
 * 
 * @version $Id: EphemerisPvHermite.java 17827 2017-09-05 07:45:10Z bignon $
 * 
 * @since 3.1
 * 
 */
public class EphemerisPvHermite extends AbstractBoundedPVProvider {

    /** Default samples number. */
    private static final int DEFAULT_SAMPLES_NUMBER = 2;

    /** Ephemeris Hermite interpolator */
    private HermiteInterpolator interpolatorPV;

    /** Acceleration table */
    private final Vector3D[] tAcc;

    /**
     * Creates an instance of EphemerisPvHermite.
     * 
     * @param tabPV
     *        position velocity coordinates table
     * @param samples
     *        number of samples used for interpolation. It must be even.
     * @param tabAcc
     *        acceleration table (can be null)
     * @param frame
     *        coordinates expression frame
     * @param tabDate
     *        table of dates for each position velocity
     * @param algo
     *        class to find the nearest date index from a given date in the date table
     *        (If null, algo will be, by default, a {@link BinarySearchIndexOpenClosed} based on a table of duration
     *        since the first date of the dates table)
     */
    public EphemerisPvHermite(final PVCoordinates[] tabPV, final int samples, final Vector3D[] tabAcc,
        final Frame frame, final AbsoluteDate[] tabDate, final ISearchIndex algo) {

        super(tabPV, samples, frame, tabDate, algo);
        if ((tabAcc != null) && (tabAcc.length != tabPV.length)) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.INVALID_ARRAY_LENGTH, tabPV.length, tabAcc.length);
        }
        this.tAcc = tabAcc;
    }

    /**
     * Creates an instance of EphemerisPvHermite from a SpacecraftState table
     * 
     * @param tabState
     *        SpacecraftState table
     * @param samples
     *        number of samples used for interpolation. It must be even.
     * @param tabAcc
     *        Acceleration table (can be null)
     * @param algo
     *        class to find the nearest date index from a given date
     *        (If null, algo will be {@link BinarySearchIndexOpenClosed} by default
     *        based on a table of duration since the first date of the dates table)
     * @exception IllegalArgumentException
     *            spacecraftState table should contains elements,
     *            and if tabacc not null should be of the same size
     */
    public EphemerisPvHermite(final SpacecraftState[] tabState, final int samples, final Vector3D[] tabAcc,
        final ISearchIndex algo) {
        super(tabState, samples, algo);
        if ((tabAcc != null) && (tabAcc.length != this.tPVCoord.length)) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.INVALID_ARRAY_LENGTH, this.tPVCoord.length, tabAcc.length);
        }
        this.tAcc = tabAcc;
    }

    /**
     * Creates an instance of EphemerisPvHermite with default number of samples = 2.
     * 
     * @param tabPV
     *        position velocity coordinates table
     * @param tabAcc
     *        acceleration table (can be null)
     * @param frame
     *        coordinates expression frame
     * @param tabDate
     *        table of dates for each position velocity
     * @param algo
     *        class to find the nearest date index from a given date in the date table
     *        (If null, algo will be, by default, a {@link BinarySearchIndexOpenClosed} based on a table of duration
     *        since the first date of the dates table)
     */
    public EphemerisPvHermite(final PVCoordinates[] tabPV, final Vector3D[] tabAcc, final Frame frame,
        final AbsoluteDate[] tabDate, final ISearchIndex algo) {
        this(tabPV, DEFAULT_SAMPLES_NUMBER, tabAcc, frame, tabDate, algo);
    }

    /**
     * Creates an instance of EphemerisPvHermite from a SpacecraftState table
     * with default number of samples = 2.
     * 
     * @param tabState
     *        SpacecraftState table
     * @param tabAcc
     *        Acceleration table (can be null)
     * @param algo
     *        class to find the nearest date index from a given date
     *        (If null, algo will be {@link BinarySearchIndexOpenClosed} by default
     *        based on a table of duration since the first date of the dates table)
     * @exception IllegalArgumentException
     *            spacecraftState table should contains elements,
     *            and if tabacc not null should be of the same size
     */
    public EphemerisPvHermite(final SpacecraftState[] tabState, final Vector3D[] tabAcc,
        final ISearchIndex algo) {
        this(tabState, DEFAULT_SAMPLES_NUMBER, tabAcc, algo);
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        final double duration = date.durationFrom(this.getDateRef());

        // If input date is out of bounds throw illegal argument exception, else proceed
        if ((duration >= 0) && (date.durationFrom(this.getMaxDate()) <= 0)) {

            // Search the index
            final int index = this.getSearchIndex().getIndex(duration);

            // the interpolation is valid only if 0<= index +1 -interpoOrder/2 or index + order/2 <= maximalIndex
            final int i0 = this.indexValidity(index);

            // If the last call was already for a date between the interpolator dates,
            // reuse the last interpolator instance
            if (index != this.getPreviousIndex()) {

                this.interpolatorPV = new HermiteInterpolator();
                this.setPreviousIndex(index);

                // get the PV coordinates and the delta t from startDate
                double deltat;
                double[] pos;
                double[] vit;
                double[] acc;
                for (int i = 0; i < this.polyOrder; i++) {
                    deltat = this.tDate[i0 + i].durationFrom(this.getDateRef());
                    pos = this.tPVCoord[i0 + i].getPosition().toArray();
                    vit = this.tPVCoord[i0 + i].getVelocity().toArray();

                    // If acceleration table is available, compute interpolation using acceleration
                    if (this.tAcc == null) {
                        this.interpolatorPV.addSamplePoint(deltat, pos, vit);
                    } else {
                        acc = this.tAcc[i0 + i].toArray();
                        this.interpolatorPV.addSamplePoint(deltat, pos, vit, acc);
                    }
                }
            }

            // Get the hermite interpolation results
            final Vector3D p = new Vector3D(this.interpolatorPV.value(duration));
            final Vector3D v = new Vector3D(this.interpolatorPV.derivative(duration));

            PVCoordinates interpolPV = new PVCoordinates(p, v);

            // If needed, convert position, velocity to the right frame
            if ((frame != null) && (this.getFrame() != frame)) {
                final Transform t = this.getFrame().getTransformTo(frame, date);
                interpolPV = t.transformPVCoordinates(interpolPV);
            }

            return interpolPV;
        } else {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.DATE_OUTSIDE_INTERVAL);
        }
    }

}
