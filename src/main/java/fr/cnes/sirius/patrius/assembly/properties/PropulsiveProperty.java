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
 * @history creation 05/04/2017
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import java.io.Serializable;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.math.analysis.IDependentVariable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;

/**
 * Propulsive property : gathers all thrust properties.
 * 
 * @concurrency immutable
 * 
 * @author rodriguest
 * 
 * @version $Id$
 * 
 * @since 3.4
 * 
 */
public final class PropulsiveProperty implements IPartProperty, Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = 2572703355137590010L;

    /** Thrust parameter name. */
    private static final String THRUST = "thrust";

    /** Isp parameter name. */
    private static final String ISP = "Isp";

    /** Thrust force (N) as parameter. */
    private final Parameter thrust;

    /** Engine specific impulse (s) as parameter. */
    private final Parameter isp;

    /** Thrust force as function (N). */
    private final IDependentVariable<SpacecraftState> thrustFunction;

    /** Engine specific impulse as function (s). */
    private final IDependentVariable<SpacecraftState> ispFunction;

    /** Part name. */
    private String partName;

    /**
     * Constructor for the property with thrust and isp as {@link Parameter}.
     * 
     * @param inThrust thrust
     * @param inIsp isp
     */
    public PropulsiveProperty(final Parameter inThrust, final Parameter inIsp) {
        // Name latter defined
        this.partName = "";

        // Create thrust and isp as parameters
        this.thrust = inThrust;
        this.isp = inIsp;

        // Create thrust and isp as IDependentVariable<SpacecraftState>
        this.thrustFunction = new IDependentVariable<SpacecraftState>(){
            /** {@inheritDoc} */
            @Override
            public double value(final SpacecraftState s) {
                return inThrust.getValue();
            }
        };

        this.ispFunction = new IDependentVariable<SpacecraftState>(){
            /** {@inheritDoc} */
            @Override
            public double value(final SpacecraftState s) {
                return inIsp.getValue();
            }
        };
    }

    /**
     * Constructor for the property with constant value for thrust and isp.
     * 
     * @param inThrust thrust
     * @param inIsp isp
     */
    public PropulsiveProperty(final double inThrust, final double inIsp) {
        this(new Parameter(THRUST, inThrust), new Parameter(ISP, inIsp));
    }

    /**
     * Constructor for the property with thrust and isp as {@link IDependentVariable}.
     * 
     * @param inThrust thrust
     * @param inIsp isp
     */
    public PropulsiveProperty(final IDependentVariable<SpacecraftState> inThrust,
        final IDependentVariable<SpacecraftState> inIsp) {
        // Name latter defined
        this.partName = "";

        this.thrustFunction = inThrust;
        this.ispFunction = inIsp;

        // Thrust and isp are functions so we don't know parameters value a priori
        // (depend on Spacecraft)
        this.isp = new Parameter(ISP, Double.NaN);
        this.thrust = new Parameter(THRUST, Double.NaN);
    }

    /**
     * Copy constructor.
     * 
     * @param propulsivePropertyIn propulsive property
     */
    public PropulsiveProperty(final PropulsiveProperty propulsivePropertyIn) {
        this.isp = propulsivePropertyIn.isp;
        this.thrust = propulsivePropertyIn.thrust;
        this.ispFunction = propulsivePropertyIn.ispFunction;
        this.thrustFunction = propulsivePropertyIn.thrustFunction;
        this.partName = propulsivePropertyIn.partName;
    }

    /**
     * Setter for the part name owning the property. <b>Warning</b>: this setter should not be used.
     * It is used internally in PATRIUS.
     * 
     * @param nameIn the part name owning the property
     */
    public void setPartName(final String nameIn) {
        this.partName = nameIn;
    }

    /**
     * Getter for the part name owning the property.
     * 
     * @return the part name owning the property
     */
    public String getPartName() {
        return this.partName;
    }

    /**
     * Getter for the thrust as an {@link IDependentVariable} object.
     * 
     * @return the thrust
     */
    public IDependentVariable<SpacecraftState> getThrust() {
        return this.thrustFunction;
    }

    /**
     * Getter for the isp as an {@link IDependentVariable} object.
     * 
     * @return the isp
     */
    public IDependentVariable<SpacecraftState> getIsp() {
        return this.ispFunction;
    }

    /**
     * Getter for the isp as an {@link Parameter} object.
     * <p>
     * Will return NaN if ISP has been defined as variable.
     * </p>
     * 
     * @return the isp
     */
    public Parameter getIspParam() {
        return this.isp;
    }

    /**
     * Getter for the thrust force as an {@link Parameter} object.
     * <p>
     * Will return NaN if thrust has been defined as variable.
     * </p>
     * 
     * @return the thrust force
     */
    public Parameter getThrustParam() {
        return this.thrust;
    }

    /**
     * Getter for thrust force (N) as function of input {@link SpacecraftState}.
     * 
     * @param state the spacecraft state
     * @return the thrust (N)
     */
    public double getThrust(final SpacecraftState state) {
        return this.thrustFunction.value(state);
    }

    /**
     * Getter for isp (s) as function of input {@link SpacecraftState}.
     * 
     * @param state the spacecraft state
     * @return the isp (s)
     */
    public double getIsp(final SpacecraftState state) {
        return this.ispFunction.value(state);
    }

    /** {@inheritDoc} */
    @Override
    public PropertyType getType() {
        return PropertyType.PROPULSIVE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final String format = "PropulsiveProperty: part name=%s, isp=%s, thrust=%s";
        final String result;
        if (this.isp == null) {
            result = String.format(format, this.partName,
                this.ispFunction, this.thrustFunction);
        } else {
            result = String.format(format, this.partName,
                this.isp.getValue(), this.thrust.getValue());
        }
        return result;
    }
}
