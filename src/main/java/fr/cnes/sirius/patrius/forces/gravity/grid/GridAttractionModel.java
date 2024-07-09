/**
 * Copyright 2021-2021 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
* VERSION:4.7:DM:DM-2861:18/05/2021:Optimisation du calcul des derivees partielles de EmpiricalForce 
* VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
* VERSION:4.7:DM:DM-2687:18/05/2021:Traitement de modèles de gravité, autres que les harmoniques sphériques
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.grid;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.forces.gravity.AttractionModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.analysis.TrivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.TrivariateGridInterpolator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Computation of central body attraction with a grid attraction model: attraction acceleration is given by
 * {@link GridAttractionProvider} which provides for a set of coordinates the value of acceleration. Interpolation
 * is performed within grid points using a {@link TrivariateGridInterpolator}. Computed acceleration excludes the
 * central attraction force like the other {@link AttractionModel}.
 * If requested point is out of grid boundaries, a 2nd model (back-up model) is used for computing attraction force.
 * <p>
 * Potential is also available using method {@link #computePotential(SpacecraftState)}.
 * </p>
 * <p>
 * Partial derivatives are not available.
 * </p>
 * 
 * @author Emmanuel Bignon
 *
 * @since 4.7
 */
public class GridAttractionModel extends JacobiansParameterizable implements ForceModel, GradientModel, 
    AttractionModel {

    /** Serial UID. */
    private static final long serialVersionUID = -1119667674179789800L;

    /** Attraction data. */
    private final AttractionData data;

    /** Back-up model used if requested point out of boundaries. */
    private final ForceModel backupModel;

    /** Body-centered frame in which grid and accelerations are expressed. */
    private final Frame bodyFrame;
    
    /** Interpolating function for acceleration. */
    private final TrivariateFunction[] fA;
    
    /** Interpolating function for potential. */
    private final TrivariateFunction fPotential;

    /** Multiplicative coefficient. */
    private double k;

    /**
     * Constructor.
     * @param attractionProvider attraction data provider
     * @param interpolator 3D interpolator using for interpolating acceleration and potential
     * @param backupModel back-up model used if requested point out of boundaries. Beware that this model should not
     *        include multiplicative coefficient
     * @param bodyFrame body-centered frame in which grid and accelerations are expressed.
     *        Frame shall be centered on body center of mass, not on grid system
     */
    public GridAttractionModel(final GridAttractionProvider attractionProvider,
            final TrivariateGridInterpolator interpolator,
            final ForceModel backupModel,
            final Frame bodyFrame) {
        super();
        // No need to call enrichParameterDescriptors() as there is no parameter to update
        // Attributes
        this.data = attractionProvider.getData();
        this.backupModel = backupModel;
        this.bodyFrame = bodyFrame;
        this.k = 1.;
        // Build interpolating functions
        final double[] xArray = this.data.getGrid().getXArray();
        final double[] yArray = this.data.getGrid().getYArray();
        final double[] zArray = this.data.getGrid().getZArray();
        final TrivariateFunction fAx = interpolator.interpolate(xArray, yArray, zArray, this.data
                .getGrid().getAccXArray());
        final TrivariateFunction fAy = interpolator.interpolate(xArray, yArray, zArray, this.data
                .getGrid().getAccYArray());
        final TrivariateFunction fAz = interpolator.interpolate(xArray, yArray, zArray, this.data
                .getGrid().getAccZArray());
        this.fA = new TrivariateFunction[] { fAx, fAy, fAz };
        this.fPotential = interpolator.interpolate(xArray, yArray, zArray, this.data.getGrid().getPotentialArray());
    }

    /** {@inheritDoc}. */
    @Override
    public void addContribution(final SpacecraftState state,
            final TimeDerivativesEquations adder) throws PatriusException {
        final Vector3D acceleration = this.computeAcceleration(state);
        adder.addXYZAcceleration(acceleration.getX(), acceleration.getY(), acceleration.getZ());
    }

    /** {@inheritDoc}. */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState state) throws PatriusException {
        // Get position in body frame / grid system
        final Vector3D pos = state.getPVCoordinates(this.bodyFrame).getPosition();
        // Convert to grid system
        final double[] coords = this.data.getGrid().getCoordinates(pos);
        
        // Check if position is within grid
        final boolean isWithin = this.data.getGrid().isInsideGrid(pos);
        
        // Compute acceleration
        if (isWithin) {
            // Interpolation
            final double ax = this.fA[0].value(coords[0], coords[1], coords[2]);
            final double ay = this.fA[1].value(coords[0], coords[1], coords[2]);
            final double az = this.fA[2].value(coords[0], coords[1], coords[2]);
            final Vector3D gamma = new Vector3D(ax, ay, az);
            // Acceleration in inertial frame including Newtonian attraction
            final Vector3D acc = this.bodyFrame.getTransformTo(state.getFrame(), state.getDate())
                    .transformVector(gamma);
            // Take into account multiplicative coefficient
            return acc.scalarMultiply(this.k);
        } else {
            // Out of grid boundaries: use back-up model
            return this.backupModel.computeAcceleration(state).scalarMultiply(this.k);
        }
    }

    /**
     * Compute the potential due to the body attraction.
     * <p>If state position is out of grid boundaries, potential is approximated to central body potential.</p>
     * @param state spacecraft state.
     * @return the potential due to the body attraction
     * @throws PatriusException thrown if position is out of grid boundaries
     */
    public double computePotential(final SpacecraftState state) throws PatriusException {
        // Get position in body frame / grid system
        final Vector3D pos = state.getPVCoordinates(this.bodyFrame).getPosition();
        // Convert to grid system
        final double[] coords = this.data.getGrid().getCoordinates(pos);

        // Check if position is within grid
        final boolean isWithin = this.data.getGrid().isInsideGrid(pos);
        
        // Compute acceleration
        if (isWithin) {
            // Interpolation
            return this.fPotential.value(coords[0], coords[1], coords[2]) * this.k;
        } else {
            // Out of grid boundaries: approximation to central attraction
            return this.data.getGM() / pos.getNorm() * this.k;
        }
    }

    /** {@inheritDoc}. */
    @Override
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /** {@inheritDoc}. */
    @Override
    public boolean computeGradientPosition() {
        return false;
    }

    /** {@inheritDoc}. */
    @Override
    public boolean computeGradientVelocity() {
        return false;
    }

    /** {@inheritDoc}. */
    @Override
    public final void addDAccDState(final SpacecraftState state,
            final double[][] dAccdPos,
            final double[][] dAccdVel) throws PatriusException {
        // Nothing to do
        // Partial derivatives not available
    }

    /**
     * {@inheritDoc}. No parameter is supported by this force model.
     */
    @Override
    public void addDAccDParam(final SpacecraftState state,
            final Parameter param,
            final double[] dAccdParam) throws PatriusException {
        throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
    }

    /** {@inheritDoc}. */
    @Override
    public double getMu() {
        return this.data.getGM();
    }

    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start,
            final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public double getMultiplicativeFactor() {
        return this.k;
    }

    /** {@inheritDoc} */
    @Override
    public void setMultiplicativeFactor(final double coefficient) {
        this.k = coefficient;
    }
}
