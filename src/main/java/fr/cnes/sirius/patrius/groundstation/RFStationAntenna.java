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
 * @history created 03/09/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:514:02/02/2016:Correcting link budget computation : add getFrame()
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.groundstation;

import fr.cnes.sirius.patrius.assembly.models.RFLinkBudgetModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.LinearInterpolator;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents an RF antenna model for a ground station.
 * It is used when calculating the RF link budget.
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment link to the tree of frames
 * 
 * @see RFLinkBudgetModel
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class RFStationAntenna implements PVCoordinatesProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 8013452629683729603L;

    /**
     * Topocentric frame of the ground station.
     */
    private final TopocentricFrame topoFrame;

    /**
     * Factor of merit of the ground antenna (gain / noise temperature) [dB / K].
     */
    private final double meritFactor;

    /**
     * Technological losses by the ground antenna [dB].
     */
    private final double groundLoss;

    /**
     * Factor of ellipticity, used to calculate the polarisation losses of the antenna [dB].
     */
    private final double ellipticityFactor;

    /**
     * Matrix of atmospheric loss (iono+tropo+rain) as function of elevation [rad * dB].
     */
    private final double[][] atmosphericLoss;

    /**
     * Matrix of pointing loss as function of elevation [rad * dB].
     */
    private final double[][] pointingLoss;

    /**
     * The function interpolating the atmospheric loss as function of elevation.
     */
    private final UnivariateFunction atmLossFunction;

    /**
     * The function interpolating the pointing loss as function of elevation.
     */
    private final UnivariateFunction pointLossFunction;

    /**
     * Loss due to the combiner of the antenna [dB].
     */
    private final double combinerLoss;

    /**
     * Constructor of the station antenna model.
     * 
     * @param inTopoFrame
     *        topocentric frame of the ground station.
     * @param inMeritFactor
     *        factor of merit of the ground antenna.
     * @param inGroundLoss
     *        technological losses by the ground antenna.
     * @param inEllipticityFactor
     *        factor of ellipticity.
     * @param inAtmosphericLoss
     *        matrix of atmospheric loss as function of elevation.
     * @param inPointingLoss
     *        matrix of pointing loss as function of elevation.
     * @param inCombinerLoss
     *        loss due to the combiner of the antenna.
     */
    public RFStationAntenna(final TopocentricFrame inTopoFrame, final double inMeritFactor,
        final double inGroundLoss, final double inEllipticityFactor, final double[][] inAtmosphericLoss,
        final double[][] inPointingLoss, final double inCombinerLoss) {
        this.topoFrame = inTopoFrame;
        this.meritFactor = inMeritFactor;
        this.groundLoss = inGroundLoss;
        this.ellipticityFactor = inEllipticityFactor;
        this.atmosphericLoss = inAtmosphericLoss.clone();
        this.pointingLoss = inPointingLoss.clone();
        this.combinerLoss = inCombinerLoss;

        this.atmLossFunction = computeInterpolatingFunction(this.atmosphericLoss);
        this.pointLossFunction = computeInterpolatingFunction(this.pointingLoss);
    }

    /**
     * Computes the loss interpolating function from a matrix of value.
     * 
     * @param lossMatrix
     *        the elevation/loss matrix.
     * @return the loss interpolating function.
     */
    private static UnivariateFunction computeInterpolatingFunction(final double[][] lossMatrix) {
        final UnivariateFunction interFunction;
        // Check lossMatrix length
        if (lossMatrix.length == 1) {
            // only one value:
            interFunction = new UnivariateFunction(){
                /** Serializable UID. */
                private static final long serialVersionUID = 1684270115994373774L;

                /** {@inheritDoc} */
                @Override
                public double value(final double x) {
                    return lossMatrix[0][0];
                }
            };
        } else {
            // more than two values, linear interpolation:
            // orders the matrix in two vectors:
            final double[] elevations = new double[lossMatrix.length];
            final double[] values = new double[lossMatrix.length];
            // Loop on all rows
            for (int row = 0; row < lossMatrix.length; row++) {
                elevations[row] = lossMatrix[row][0];
                values[row] = lossMatrix[row][1];
            }
            interFunction = new LinearInterpolator().interpolate(elevations, values);
        }
        return interFunction;
    }

    /**
     * @return the factor of merit of the ground antenna (gain / noise temperature) [dB / K].
     */
    public final double getMeritFactor() {
        return this.meritFactor;
    }

    /**
     * @return the technological losses by the ground antenna [dB].
     */
    public final double getGroundLoss() {
        return this.groundLoss;
    }

    /**
     * @return the factor of ellipticity, used to calculate the polarisation losses of the antenna [dB].
     */
    public final double getEllipticityFactor() {
        return this.ellipticityFactor;
    }

    /**
     * Gets the atmospheric loss using a spline interpolation. <br>
     * The atmospheric loss is a function of the ground station elevation.
     * 
     * @param elevation
     *        ground station elevation [rad], in the range [-PI/2; PI/2].
     * 
     * @return the atmospheric loss (iono+tropo+rain) for a given elevation [rad * dB].
     */
    public final double getAtmosphericLoss(final double elevation) {
        return computeLoss(elevation, this.atmosphericLoss, this.atmLossFunction);
    }

    /**
     * Gets the pointing loss using a spline interpolation. <br>
     * The pointing loss is a function of the ground station elevation.
     * 
     * @param elevation
     *        ground station elevation [rad], in the range [-PI/2; PI/2].
     * 
     * @return the pointing loss for a given elevation [rad * dB].
     */
    public final double getPointingLoss(final double elevation) {
        return computeLoss(elevation, this.pointingLoss, this.pointLossFunction);
    }

    /**
     * Computes the loss interpolating the elevation/loss matrix.
     * 
     * @param elevation
     *        ground station elevation [rad], in the range [-PI/2; PI/2].
     * @param lossMatrix
     *        the elevation/loss matrix.
     * @param interpolatingFunction
     *        the function interpolating the elevation/loss matrix.
     * @return the loss for a given elevation [rad * dB].
     */
    private static double computeLoss(final double elevation, final double[][] lossMatrix,
                               final UnivariateFunction interpolatingFunction) {
        // checks the input angles:
        if (elevation < -FastMath.PI / 2 || elevation > FastMath.PI / 2) {
            throw new MathIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
        }
        final double loss;
        if (elevation <= lossMatrix[0][0]) {
            // the smallest elevation in the atmospheric losses matrix is not -PI/2:
            loss = lossMatrix[0][1];
        } else if (elevation > lossMatrix[lossMatrix.length - 1][0]) {
            // the biggest elevation in the atmospheric losses matrix is not PI/2:
            loss = lossMatrix[lossMatrix.length - 1][1];
        } else {
            loss = interpolatingFunction.value(elevation);
        }
        return loss;
    }

    /**
     * Returns ground station topocentric frame.
     * 
     * @return ground station topocentric frame.
     */
    public Frame getFrame() {
        return this.topoFrame;
    }

    /**
     * Returns loss due to the combiner of the antenna [dB].
     * 
     * @return the loss due to the combiner of the antenna [dB].
     */
    public final double getCombinerLoss() {
        return this.combinerLoss;
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return this.topoFrame.getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
        return this.topoFrame;
    }
}
