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
 * @history creation 16/06/2016
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:600:16/06/2016:add Cook (Cn, Ct) models
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:1177:06/09/2017:add Cook model validation test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.cook;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.forces.atmospheres.AtmosphereData;
import fr.cnes.sirius.patrius.forces.atmospheres.ExtendedAtmosphere;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.special.Erf;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * This class implements Cook tangential coefficient to a facet.
 * 
 * @concurrency not thread-safe
 * 
 * @author Emmanuel Bignon
 * @since 3.3
 * @version $Id$
 */
public class CtCookModel implements IParamDiffFunction {

    /** serialVersionUID. */
    private static final long serialVersionUID = 4020488548530576523L;

    /** Atmosphere. */
    private final ExtendedAtmosphere atmosphere;

    /** Facet. */
    private final Facet facet;

    /** Facet frame. */
    private final Frame facetFrame;

    /** Specular reemission coefficient. */
    private final double epsilon;

    /**
     * Constructor.
     * 
     * @param atmos
     *        atmosphere
     * @param afacet
     *        facet. Facet should be oriented such that facet normal vector is outside the spacecraft.
     *        Thus the facet will create drag only if facing the wind
     * @param afacetFrame
     *        facet frame
     * @param eps
     *        specular reemission coefficient
     */
    public CtCookModel(final ExtendedAtmosphere atmos, final Facet afacet, final Frame afacetFrame, final double eps) {
        this.atmosphere = atmos;
        this.facet = afacet;
        this.facetFrame = afacetFrame;
        this.epsilon = eps;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final SpacecraftState state) {

        try {
            // Atmospheric data computation
            final Vector3D pos = state.getPVCoordinates().getPosition();
            final Vector3D vrel = this.atmosphere.getVelocity(state.getDate(), pos, state.getFrame())
                .subtract(state.getPVCoordinates().getVelocity());
            final AtmosphereData data = this.atmosphere.getData(state.getDate(), pos, state.getFrame());
            final double molarMass = data.getMeanAtomicMass() * Constants.AVOGADRO_CONSTANT
                * AtmosphereData.HYDROGEN_MASS;
            final double tAtmo = data.getLocalTemperature();

            // Convert vrel in facet frame
            final Transform t = state.getFrame().getTransformTo(this.facetFrame, state.getDate());
            final Vector3D vrelFacet = t.transformVector(vrel);

            // Temporary variables
            final double vrelnorm2 = vrel.getNormSq();
            final double r = MathLib.divide(Constants.PERFECT_GAS_CONSTANT, molarMass);

            final double s2 = vrelnorm2 / (2. * r * tAtmo);
            final double s = MathLib.sqrt(s2);
            final double theta = FastMath.PI / 2. - Vector3D.angle(this.facet.getNormal().negate(), vrelFacet);
            final double[] sincos = MathLib.sinAndCos(theta);
            final double sintheta = sincos[0];
            final double costheta = sincos[1];
            final double sintheta2 = sintheta * sintheta;
            final double ssintheta = s * sintheta;
            final double s2sintheta2 = s2 * sintheta2;
            final double sqrtPi = MathLib.sqrt(FastMath.PI);
            final double erfssintheta = 1. + Erf.erf(ssintheta);

            // Absorption part
            final double ctAbs = costheta * (MathLib.exp(-s2sintheta2) + sqrtPi * ssintheta * erfssintheta)
                / (s * sqrtPi);

            // Specular part
            final double ctSpec = -this.epsilon * ctAbs;

            // Diffuse part
            final double ctDiff = 0.;

            // Return sum of all 3 components
            return ctAbs + ctSpec + ctDiff;

        } catch (final PatriusException e) {
            // Computation failed: catch and wrap exception as only runtime can be sent
            throw new PatriusExceptionWrapper(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double derivativeValue(final Parameter p, final SpacecraftState state) {
        return 0.;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsParameter(final Parameter param) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getParameters() {
        return new ArrayList<Parameter>();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDifferentiableBy(final Parameter p) {
        return false;
    }
}
