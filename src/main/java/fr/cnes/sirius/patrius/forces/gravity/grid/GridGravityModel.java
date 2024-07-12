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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de AbstractEOPHistory
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.11:DM:DM-40:22/05/2023:[PATRIUS] Gestion derivees par rapport au coefficient k dans les GravityModel
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:DM:DM-2861:18/05/2021:Optimisation du calcul des derivees partielles de EmpiricalForce 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.7:DM:DM-2687:18/05/2021:Traitement de modèles de gravité, autres que les harmoniques sphériques
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.grid;

import fr.cnes.sirius.patrius.forces.gravity.AbstractGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.TrivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.TrivariateGridInterpolator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Computation of central body attraction with a grid attraction model: attraction acceleration is given by
 * {@link GridAttractionProvider} which provides for a set of coordinates the value of acceleration. Interpolation
 * is performed within grid points using a {@link TrivariateGridInterpolator}. Computed acceleration excludes the
 * central attraction force like the other {@link AbstractGravityModel}.
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
public class GridGravityModel extends AbstractGravityModel {

    /** Serializable UID. */
    private static final long serialVersionUID = -1119667674179789800L;

    /** Attraction data. */
    private final AttractionData data;

    /** Back-up model used if requested point out of boundaries. */
    private final GravityModel backupModel;
    
    /** Interpolating function for acceleration. */
    private final TrivariateFunction[] fA;
    
    /** Interpolating function for potential. */
    private final TrivariateFunction fPotential;

    /**
     * Constructor.
     * 
     * @param attractionProvider attraction data provider
     * @param interpolator 3D interpolator using for interpolating acceleration and potential
     * @param backupModel back-up model used if requested point out of boundaries. Beware that this model should not
     *        include multiplicative coefficient
     * @param bodyFrameIn body-centered frame in which grid and accelerations are expressed.
     *        Frame shall be centered on body center of mass, not on grid system
     */
    public GridGravityModel(final GridAttractionProvider attractionProvider,
                            final TrivariateGridInterpolator interpolator, final GravityModel backupModel,
                            final Frame bodyFrameIn) {
        super(bodyFrameIn, attractionProvider.getData().getMuParameter());
        // Attributes
        this.data = attractionProvider.getData();
        this.backupModel = backupModel;
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
    public Vector3D computeAcceleration(final Vector3D positionInBodyFrame, final AbsoluteDate date)
        throws PatriusException {
        // Convert to grid system
        final double[] coords = this.data.getGrid().getCoordinates(positionInBodyFrame);

        // Check if position is within grid
        final boolean isWithin = this.data.getGrid().isInsideGrid(positionInBodyFrame);

        // Compute acceleration
        final Vector3D acc;
        if (isWithin) {
            // Interpolation
            final double ax = this.fA[0].value(coords[0], coords[1], coords[2]);
            final double ay = this.fA[1].value(coords[0], coords[1], coords[2]);
            final double az = this.fA[2].value(coords[0], coords[1], coords[2]);
            acc = new Vector3D(ax, ay, az);
        } else {
            // Out of grid boundaries: use back-up model
            // Compute acceleration
            acc = this.backupModel.computeAcceleration(positionInBodyFrame, date);
        }

        return acc;
    }

    /**
     * Compute the potential due to the body attraction.
     * <p>
     * If state position is out of grid boundaries, potential is approximated to central body potential.
     * </p>
     * 
     * @param pos position of the spacecraft
     * @param frame frame in which the position of the spacecraft is given
     * @param date date
     * @return the potential due to the body attraction
     * @throws PatriusException thrown if position is out of grid boundaries
     */
    public double computePotential(final Vector3D pos, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Get position in body frame / grid system
        final Transform t = frame.getTransformTo(this.getBodyFrame(), date);
        final Vector3D position = t.transformPosition(pos);
        // Convert to grid system
        final double[] coords = this.data.getGrid().getCoordinates(position);

        // Check if position is within grid
        final boolean isWithin = this.data.getGrid().isInsideGrid(position);
        
        // Compute acceleration
        if (isWithin) {
            // Interpolation
            return this.fPotential.value(coords[0], coords[1], coords[2]);
        } else {
            // Out of grid boundaries: approximation to central attraction
            return this.data.getGM() / position.getNorm();
        }
    }

    /** {@inheritDoc}. */
    @Override
    public final double[][] computeDAccDPos(final Vector3D positionInBodyFrame, final AbsoluteDate date)
        throws PatriusException {
        // Unavailable jacobian for the grid gravity model
        throw new PatriusException(PatriusMessages.UNAVAILABLE_JACOBIAN_FOR_GRID_MODEL);
    }

    /** {@inheritDoc}. */
    @Override
    public double getMu() {
        return this.data.getGM();
    }

    /** {@inheritDoc}. */
    @Override
    public void setMu(final double muIn) {
        this.data.setGM(muIn);
    }
}
