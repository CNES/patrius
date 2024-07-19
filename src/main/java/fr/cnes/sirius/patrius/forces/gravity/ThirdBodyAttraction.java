/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le
 * propagateur
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de
 * AbstractEOPHistory
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.11:FA:FA-3312:22/05/2023:[PATRIUS] TrueInertialFrame pas vraiment pseudo-inertiel
 * VERSION:4.11:DM:DM-40:22/05/2023:[PATRIUS] Gestion derivees par rapport au coefficient k dans les GravityModel
 * VERSION:4.11:DM:DM-17:22/05/2023:[PATRIUS] Detecteur de distance a la surface d'un corps celeste
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8.1:FA:FA-2947:07/12/2021:[PATRIUS] Problemes lies au coefficient multiplicatif des forces grav.
 * VERSION:4.8:FA:FA-2947:15/11/2021:[PATRIUS] Problemes lies au coefficient multiplicatif des forces gravitationnelles 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:DM:DM-2685:18/05/2021:Prise en compte d un modele de gravite complexe pour un troisieme corps
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:767:24/04/2018: Creation of ForceModelsData to collect the models data for a force computation
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Third body attraction force model.
 * 
 * <p>
 * The implementation of this class enables the computation of partial derivatives by finite differences with respect to
 * the <b>central attraction coefficient</b>.
 * </p>
 * 
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
@SuppressWarnings("PMD.NullAssignment")
// Reason: code clarity and simplicity
public class ThirdBodyAttraction extends AbstractBodyAttraction {

    /** Serializable UID. */
    private static final long serialVersionUID = -1703641239448217284L;

    /**
     * Simple constructor.
     * Partial derivative computation is set to true by default.
     * 
     * @param gravityModelIn
     *        the gravitational attraction model to consider
     * @throws NullArgumentException
     *         if {@code gravityModelIn} is {@code null}
     */
    public ThirdBodyAttraction(final GravityModel gravityModelIn) {
        this(gravityModelIn, true);
    }

    /**
     * <p>
     * Simple constructor.
     * </p>
     *
     * @param gravityModelIn
     *        the gravitational attraction model to consider
     * @param computePD
     *        true if partial derivatives have to be computed
     * @throws NullArgumentException
     *         if {@code gravityModelIn} is {@code null}
     */
    public ThirdBodyAttraction(final GravityModel gravityModelIn, final boolean computePD) {
        super(gravityModelIn, computePD, 1.);
    }

    /**
     * <p>
     * Simple constructor. Only central term of third body is considered.
     * </p>
     *
     * @param gravityModelIn
     *        the gravitational attraction model to consider
     * @param computePD
     *        true if partial derivatives have to be computed
     * @param k multiplicative coefficient
     * @throws NullArgumentException
     *         if {@code gravityModelIn} is {@code null}
     */
    public ThirdBodyAttraction(final GravityModel gravityModelIn, final boolean computePD, final Parameter k) {
        super(gravityModelIn, computePD, k);
    }

    /** {@inheritDoc}. */
    @Override
    protected Vector3D computeAcceleration(final Vector3D positionInBodyFrame, final AbsoluteDate date,
                                        final Transform scFrameToBodyFrame) throws PatriusException {
        // Define the central term acceleration
        Vector3D gamma = null;
        // Retrieve the gravity model
        final GravityModel gravityModel = this.getGravityModel();
        // Define the main body position in the body frame
        final Vector3D mainBodyPositionInBodyFrame = scFrameToBodyFrame.transformPosition(Vector3D.ZERO);
        if (gravityModel instanceof AbstractHarmonicGravityModel) {
            // Define the gravity model as an AbstractHarmonicGravityModel
            final AbstractHarmonicGravityModel harmonicGravityModel = (AbstractHarmonicGravityModel) gravityModel;

            // Separate the central gamma from the non-central (harmonic) gamma for numerical precision

            // Compute the spacecraft central gamma
            final Vector3D scCentralGamma = harmonicGravityModel.computeCentralTermAcceleration(positionInBodyFrame);
            // Compute the main body central gamma
            final Vector3D mainBodyCentralGamma = harmonicGravityModel
                .computeCentralTermAcceleration(mainBodyPositionInBodyFrame);
            // Compute the difference between the spacecraft central gamma and the main body central gamma
            final Vector3D diffCentralGamma = scCentralGamma.subtract(mainBodyCentralGamma);

            // Compute the spacecraft non-central (harmonic) gamma
            final Vector3D scNonCentralGamma = harmonicGravityModel.computeNonCentralTermsAcceleration(
                positionInBodyFrame, date);
            // Compute the main body non-central (harmonic) gamma
            final Vector3D mainBodyNonCentralGamma = harmonicGravityModel.computeNonCentralTermsAcceleration(
                mainBodyPositionInBodyFrame, date);
            // Compute the difference between the spacecraft non-central (harmonic) gamma and the main body non-central
            // (harmonic) gamma
            final Vector3D diffNonCentralGamma = scNonCentralGamma.subtract(mainBodyNonCentralGamma);

            // Compute the gamma as the sum between diffCentralGamma (the difference between the spacecraft central
            // gamma and the main body central gamma) and diffNonCentralGamma (the difference between the spacecraft
            // non-central (harmonic) gamma and the main body non-central (harmonic) gamma)
            gamma = diffCentralGamma.add(diffNonCentralGamma);

        } else {
            // Compute the spacecraft gamma
            final Vector3D scGamma = gravityModel.computeAcceleration(positionInBodyFrame, date);
            // Compute the main body gamma
            final Vector3D mainBodyGamma = gravityModel.computeAcceleration(mainBodyPositionInBodyFrame, date);
            // Compute the gamma as the sum between the spacecraft gamma and the main body gamma
            gamma = scGamma.subtract(mainBodyGamma);
        }

        return gamma.scalarMultiply(getMultiplicativeFactor());
    }

    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start,
            final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }
}
