/**
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
 * VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.7:FA:FA-2902:18/05/2021:Anomalie dans la gestion du JacobiansMapper
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.5:DM:DM-2415:27/05/2020:Gestion des PartialderivativesEquations avec MultiPropagateur 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical.multi;

import java.io.Externalizable;
import java.util.List;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianAttractionModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.numerical.AbstractPartialDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;

/**
 *
 **/
/**
 * Set of {@link AdditionalEquations additional equations} computing the partial derivatives
 * of the state (orbit) with respect to initial state and force models parameters.
 * <p>
 * This set of equations are automatically added to a {@link NumericalPropagator numerical propagator} in order to
 * compute partial derivatives of the orbit along with the orbit itself. This is useful for example in orbit
 * determination applications.
 * </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.5
 */
public class MultiPartialDerivativesEquations extends AbstractPartialDerivativesEquations {

     /** Serializable UID. */
    private static final long serialVersionUID = 3196712568052800053L;

    /** Selected parameters for Jacobian computation. */
    private final MultiNumericalPropagator propagator;

    /** Satellite Id. */
    private final String satId;

    /**
     * Empty constructor for {@link Externalizable} use.
     */
    public MultiPartialDerivativesEquations() {
        this(null, null, null);
    }

    /**
     * Simple constructor.
     * <p>
     * Upon construction, this set of equations is <em>automatically</em> added to the propagator by calling its
     * {@link NumericalPropagator#addAdditionalEquations(AdditionalEquations)} method. So there is no need to call this
     * method explicitly for these equations.
     * </p>
     * 
     * @param nameIn
     *        name of the partial derivatives equations
     * @param propagatorIn
     *        the propagator that will handle the orbit propagation
     * @param satId
     *        satellite ID
     */
    public MultiPartialDerivativesEquations(final String nameIn,
        final MultiNumericalPropagator propagatorIn, final String satId) {
        super(nameIn);
        this.propagator = propagatorIn;
        this.satId = satId;
        if (propagatorIn != null) {
            propagatorIn.addAdditionalEquations(this, satId);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected NewtonianAttractionModel getNewtonianAttraction() {
        return this.propagator.getNewtonianAttractionForceModel(satId);
    }

    /** {@inheritDoc} */
    @Override
    protected List<ForceModel> getForceModels() {
        return this.propagator.getForceModels(satId);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.NullAssignment")
    // REason: null value expected (no frame case)
    protected Frame getFrame() {
        // Get propagation frame if existent
        Frame frame;
        try {
            frame = propagator.getFrame(satId);
        } catch (final IllegalStateException e) {
            // State frame will be used
            frame = null;
        }
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    protected OrbitType getOrbitType() {
        return this.propagator.getOrbitType();
    }

    /** {@inheritDoc} */
    @Override
    protected PositionAngle getPositionAngle() {
        return this.propagator.getPositionAngleType();
    }
}
