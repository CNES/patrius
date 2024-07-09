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
 * @history creation 16/06/2016
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:600:16/06/2016:add Cook (Cn, Ct) models
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:1177:06/09/2017:add Cook model validation test
 * END-HISTORY
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
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
 * This class implements Cook normal coefficient to a facet.
 * 
 * @concurrency not thread-safe
 * 
 * @author Emmanuel Bignon
 * @since 3.3
 * @version $Id$
 */
public class CnCookModel implements IParamDiffFunction {

    /** serialVersionUID. */
    private static final long serialVersionUID = 4020488548530576523L;

    /** Pi square root. */
    private static final double SQRT_PI = MathLib.sqrt(FastMath.PI);

    /** Atmosphere. */
    private final ExtendedAtmosphere atmosphere;

    /** Facet. */
    private final Facet facet;

    /** Facet frame. */
    private final Frame facetFrame;

    /** Specular reemission coefficient. */
    private final double epsilon;

    /** Wall gas temperature. */
    private final WallGasTemperatureProvider wallGasTemperatureModel;

    /**
     * Constructor with constant wall gas temperature.
     * 
     * @param atmos
     *        atmosphere
     * @param afacet
     *        facet
     * @param afacetFrame
     *        facet frame
     * @param eps
     *        specular reemission coefficient
     * @param wallGasTemp
     *        constant wall gas temperature
     */
    public CnCookModel(final ExtendedAtmosphere atmos, final Facet afacet, final Frame afacetFrame, final double eps,
        final double wallGasTemp) {
        this.atmosphere = atmos;
        this.facet = afacet;
        this.facetFrame = afacetFrame;
        this.epsilon = eps;
        this.wallGasTemperatureModel = new ConstantWallGasTemperature(wallGasTemp);
    }

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
     * @param wallGasTemp
     *        wall gas temperature
     */
    public CnCookModel(final ExtendedAtmosphere atmos, final Facet afacet, final Frame afacetFrame, final double eps,
        final WallGasTemperatureProvider wallGasTemp) {
        this.atmosphere = atmos;
        this.facet = afacet;
        this.facetFrame = afacetFrame;
        this.epsilon = eps;
        this.wallGasTemperatureModel = wallGasTemp;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final SpacecraftState state) {

        try {
            // Atmospheric data
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

            // Temporary variables * Atmosphere
            final double vrelnorm2 = vrel.getNormSq();
            final double r = MathLib.divide(Constants.PERFECT_GAS_CONSTANT, molarMass);

            final double s2 = vrelnorm2 / (2. * r * tAtmo);
            final double s = MathLib.sqrt(s2);
            final double theta = FastMath.PI / 2. - Vector3D.angle(this.facet.getNormal().negate(), vrelFacet);
            final double sintheta = MathLib.sin(theta);
            final double sintheta2 = sintheta * sintheta;
            final double ssintheta = s * sintheta;
            final double s2sintheta2 = s2 * sintheta2;
            final double erfssintheta = 1. + Erf.erf(ssintheta);

            // Absorption part
            final double cnAbs = (ssintheta * MathLib.exp(-s2sintheta2)
                + SQRT_PI * (1. / 2. + s2sintheta2) * erfssintheta) / (s2 * SQRT_PI);

            // Specular part
            final double cnSpec = this.epsilon * cnAbs;

            // Diffuse part
            final double wallGasTemperature = this.wallGasTemperatureModel.getWallGasTemperature(state, vrel, theta);
            final double cnDiff = MathLib.divide((1. - this.epsilon), (2. * s2))
                * MathLib.sqrt(MathLib.divide(wallGasTemperature, tAtmo))
                * (MathLib.exp(-s2sintheta2) + SQRT_PI * ssintheta * erfssintheta);

            // Return sum of all 3 components
            return cnAbs + cnSpec + cnDiff;

        } catch (final PatriusException e) {
            // Computation failed: catch and wrap exception as only Runtime can be sent
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
    public ArrayList<Parameter> getParameters() {
        return new ArrayList<Parameter>();
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsParameter(final Parameter param) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDifferentiableBy(final Parameter p) {
        return false;
    }
}
