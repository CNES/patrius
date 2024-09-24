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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2801:18/05/2021:Suppression des classes et methodes depreciees suite au refactoring des slews
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.List;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 *
 **/
/**
 * <p>
 * This class represents a tabulated slew.
 * </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.5
 */
public final class TabulatedSlew extends TabulatedAttitude implements Slew {

    /** Serializable UID. */
    private static final long serialVersionUID = 6595548109504426959L;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "TABULATED_SLEW";

    /**
     * Constructor with default number N of points used for interpolation.
     * 
     * @param inAttitudes
     *        the list of attitudes. WARNING : these attitudes must be ordered.
     * @exception PatriusException
     *            if the number of point is too small for interpolating
     */
    public TabulatedSlew(final List<Attitude> inAttitudes) throws PatriusException {
        super(inAttitudes, DEFAULT_NATURE);
    }

    /**
     * Constructor with number of points used for interpolation
     * 
     * @param inAttitudes
     *        the list of attitudes. WARNING : these attitudes must be ordered.
     * @param nbInterpolationPoints
     *        number of points used for interpolation
     * @exception PatriusException
     *            if the number of point is too small for interpolating
     */
    public TabulatedSlew(final List<Attitude> inAttitudes,
        final int nbInterpolationPoints) throws PatriusException {
        super(inAttitudes, nbInterpolationPoints, DEFAULT_NATURE);
    }

    /**
     * Constructor with default number N of points used for interpolation.
     * 
     * @param inAttitudes the list of attitudes. WARNING : these attitudes must be ordered.
     * @param natureIn leg nature
     * @exception PatriusException if the number of point is too small for interpolating
     */
    public TabulatedSlew(final List<Attitude> inAttitudes, final String natureIn)
        throws PatriusException {
        super(inAttitudes, natureIn);
    }

    /**
     * Constructor with number of points used for interpolation
     * 
     * @param inAttitudes the list of attitudes. WARNING : these attitudes must be ordered.
     * @param nbInterpolationPoints number of points used for interpolation
     * @param natureIn leg nature
     * @exception PatriusException if the number of point is too small for interpolating
     */
    public TabulatedSlew(final List<Attitude> inAttitudes, final int nbInterpolationPoints,
        final String natureIn) throws PatriusException {
        super(inAttitudes, nbInterpolationPoints, natureIn);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        return getAttitude(null, date, frame);
    }

    /** {@inheritDoc}
     * <p>
     * pvProvider is unused since slew has been computed beforehand.
     * </p>
     * */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProvider, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {
        // Delegate to tabulated attitude
        Attitude res = super.getAttitude(null, date, frame);
        // Handle spin derivatives
        if (isSpinDerivativesComputation()) {
            res = new Attitude(date, frame, res.getRotation(), res.getSpin(), this.getSpinDerivatives(date, frame));
        }
        return res;
    }

    /**
     * Get spin derivatives.
     * 
     * @param date
     *        the date to compute derivative of spin
     * @param frame
     *        reference frame from which derivative of spin is computed
     * @return the spin derivative
     * @throws PatriusException
     *         if spin derivative cannot be computed
     */
    public Vector3D getSpinDerivatives(final AbsoluteDate date, final Frame frame) throws PatriusException {
        try {
            return this.getSpinFunction(frame, date).nthDerivative(1).getVector3D(date);
        } catch (final IllegalArgumentException e) {
            // Date at bounds: use forward/backward finite differences
            final Vector3D res;
            if (date.durationFrom(this.getTimeInterval().getLowerData()) < AbstractVector3DFunction.DEFAULT_STEP) {
                // Lower bound
                final Vector3D spin1 = super.getAttitude(null, date, frame).getSpin();
                final Vector3D spin2 =
                        super.getAttitude(null, date.shiftedBy(AbstractVector3DFunction.DEFAULT_STEP),
                        frame).getSpin();
                res = spin2.subtract(spin1).scalarMultiply(AbstractVector3DFunction.DEFAULT_STEP);
            } else {
                // Upper bound
                final Vector3D spin1 =
                        super.getAttitude(null, date.shiftedBy(-AbstractVector3DFunction.DEFAULT_STEP),
                        frame).getSpin();
                final Vector3D spin2 = super.getAttitude(null, date, frame).getSpin();
                res = spin2.subtract(spin1).scalarMultiply(AbstractVector3DFunction.DEFAULT_STEP);
            }
            return res;
        }
    }

    /**
     * Get spin function.
     * 
     * @param frame
     *        reference frame from which spin function of date is computed
     * @param zeroAbscissa
     *        the date for which x=0 for spin function of date
     * @return spin function of date relative
     */
    private Vector3DFunction getSpinFunction(final Frame frame, final AbsoluteDate zeroAbscissa) {
        return new AbstractVector3DFunction(zeroAbscissa){
            /** {@inheritDoc} */
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                return getAttitudeNoSpinDerivative(date, frame).getSpin();
            }
        };
    }

    /**
     * Compute the attitude without the specific spin derivative.
     * 
     * @param date
     *        : current date
     * @param frame
     *        : reference frame from which attitude is computed
     * @return attitude : attitude at the specified date
     * @throws PatriusException
     *         thrown if failed
     */
    private Attitude getAttitudeNoSpinDerivative(final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        return super.getAttitude(null, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public TabulatedSlew copy(final AbsoluteDateInterval newIntervalOfValidity) {
        final TabulatedAttitude tabulatedAttitude = super.copy(newIntervalOfValidity);
        try {
            final TabulatedSlew result = new TabulatedSlew(tabulatedAttitude.getAttitudes(),
                    tabulatedAttitude.getInterpolationOrder(), tabulatedAttitude.getNature());
            result.setSpinDerivativesComputation(isSpinDerivativesComputation());
            result.setAngularDerivativesFilter(getAngularDerivativeFilter());
            return result;
        } catch (final PatriusException e) {
            // Cannot not happen
            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
        }
    }
}
