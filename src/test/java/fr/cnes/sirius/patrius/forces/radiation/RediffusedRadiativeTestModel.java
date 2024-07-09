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
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:01/04/2014:Changed partial derivatives API
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * RediffusedRadiativeTestModel class test.
 */
public class RediffusedRadiativeTestModel extends Parameterizable implements RediffusedRadiationSensitive {
    /** Parameter name for K0 albedo global coefficient. */
    public static final String K0ALBEDO_COEFFICIENT = "K0 albedo coefficient";
    /** Parameter name for K0 infrared global coefficient. */
    public static final String K0IR_COEFFICIENT = "K0 infrared coefficient";
    /** Parameter name for absorption coefficient. */
    public static final String ABSORPTION_COEFFICIENT = "absorption coefficient";
    /** Parameter name for specular coefficient. */
    public static final String SPECULAR_COEFFICIENT = "specular coefficient";
    /** Parameter name for diffusion coefficient. */
    public static final String DIFFUSION_COEFFICIENT = "diffusion coefficient";

    /**
     * albedo
     */
    private final boolean albFlag;
    /**
     * ir
     */
    private final boolean irFlag;

    protected Parameter k0Albedo = new Parameter(K0ALBEDO_COEFFICIENT, 0.);
    protected Parameter k0Ir = new Parameter(K0IR_COEFFICIENT, 0.);
    protected Parameter ka = new Parameter(ABSORPTION_COEFFICIENT, 0.);
    protected Parameter ks = new Parameter(SPECULAR_COEFFICIENT, 0.);
    protected Parameter kd = new Parameter(DIFFUSION_COEFFICIENT, 0.);

    /**
     * Constructor
     */
    public RediffusedRadiativeTestModel(final boolean albedo, final boolean ir, final double i, final double j) {
        super();
        this.addAllParameters(this.k0Albedo, this.k0Ir, this.ka, this.kd, this.ks);
        this.albFlag = albedo;
        this.irFlag = ir;
    }

    /**
     * test method
     * 
     * @see fr.cnes.sirius.patrius.forces.radiation.RediffusedRadiationSensitive#rediffusedRadiationPressureAcceleration(fr.cnes.sirius.patrius.propagation.SpacecraftState)
     */
    @Override
    public
            Vector3D
            rediffusedRadiationPressureAcceleration(final SpacecraftState state, final ElementaryFlux f)
                                                                                                        throws PatriusException {
        return new Vector3D(1.e-10, 1.e-10, 1.e-10);
    }

    /**
     * test method
     * 
     * @see fr.cnes.sirius.patrius.forces.radiation.RediffusedRadiationSensitive#addDAccDStateRediffusedRadiativePressure(fr.cnes.sirius.patrius.propagation.SpacecraftState,
     *      double[][], double[][], double[])
     */
    @Override
    public void addDAccDStateRediffusedRadiativePressure(final SpacecraftState s, final double[][] dAccdPos,
                                                         final double[][] dAccdVel) throws PatriusException {
        dAccdPos[0][0] = 1.;
        dAccdPos[0][1] = 2.;
        dAccdPos[0][2] = 3.;
        dAccdPos[1][0] = 4.;
        dAccdPos[1][1] = 5.;
        dAccdPos[1][2] = 6.;
        dAccdPos[2][0] = 7.;
        dAccdPos[2][1] = 8.;
        dAccdPos[2][2] = 9.;
    }

    /**
     * test method
     * 
     * @see fr.cnes.sirius.patrius.forces.radiation.RediffusedRadiationSensitive#addDAccDParamRediffusedRadiativePressure(fr.cnes.sirius.patrius.propagation.SpacecraftState,
     *      java.lang.String, double[])
     */
    @Override
    public void addDAccDParamRediffusedRadiativePressure(final SpacecraftState s, final Parameter param,
                                                         final double[] dAccdParam)
                                                                                   throws PatriusException {
        if (ABSORPTION_COEFFICIENT.endsWith(param.getName())) {
            dAccdParam[0] = 1.;
            dAccdParam[1] = 2.;
            dAccdParam[2] = 3.;
        } else {
            dAccdParam[0] = 0.;
            dAccdParam[1] = 0.;
            dAccdParam[2] = 0.;
        }
    }

    @Override
    public boolean getFlagAlbedo() {
        // TODO Auto-generated method stub
        return this.albFlag;
    }

    @Override
    public boolean getFlagIr() {
        // TODO Auto-generated method stub
        return this.irFlag;
    }

    @Override
    public void initDerivatives() {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getJacobianParameters() {
        // return all parameters
        return this.getParameters();
    }

    @Override
    public Parameter getK0Albedo() {
        return this.k0Albedo;
    }

    @Override
    public Parameter getK0Ir() {
        return this.k0Ir;
    }

    @Override
    public Assembly getAssembly() {
        return null;
    }

}
