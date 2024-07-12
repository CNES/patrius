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
 * 
 * @history creation 23/05/2018
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:23/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.aerocoeffs;

import java.util.ArrayList;
import java.util.Arrays;

import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.math.analysis.interpolation.BiLinearIntervalsFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexClosedOpen;
import fr.cnes.sirius.patrius.math.utils.RecordSegmentSearchIndex;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Aerodynamic coefficient function of the spacecraft angle of attack and Mach number. Aerodynamic
 * coefficient is then retrieved by bilinear interpolation of an <<angle of attack, Mach number>,
 * aerodynamic coefficient> array.
 * 
 * @author Marie Capot
 * 
 * @version $Id$
 * 
 * @since 4.1
 * 
 */
public class AeroCoeffByAoAAndMach implements AerodynamicCoefficient {

    /** Serializable UID. */
    private static final long serialVersionUID = 3606508187192326359L;

    /** Function. */
    private final BiLinearIntervalsFunction function;

    /** Angle of attack variable array. */
    private final double[] aoAArray;

    /** Mach number variable array. */
    private final double[] machArray;

    /** Y variable array. */
    private final double[][] dataArray;

    /** Earth shape. */
    private final EllipsoidBodyShape earthShape;

    /** Atmosphere. */
    private final Atmosphere atmosphere;

    /**
     * Constructor.
     * 
     * @param anglesOfAttack array of angles of attack (rad)
     * @param machNumbers array of mach numbers
     * @param coeffs array of arrays of aerodynamic coefficients
     * @param atmosphere atmosphere model
     * @param earthShapeIn earth shape
     */
    public AeroCoeffByAoAAndMach(final double[] anglesOfAttack, final double[] machNumbers,
        final double[][] coeffs, final Atmosphere atmosphere,
        final EllipsoidBodyShape earthShapeIn) {

        this.aoAArray = anglesOfAttack;
        this.machArray = machNumbers;
        this.dataArray = coeffs;
        this.earthShape = earthShapeIn;
        this.atmosphere = atmosphere;

        // Coefficients as a function of Mach and AoA
        final RecordSegmentSearchIndex searchAlgorithmAbs = new RecordSegmentSearchIndex(
            new BinarySearchIndexClosedOpen(anglesOfAttack));
        final RecordSegmentSearchIndex searchAlgorithmOrd = new RecordSegmentSearchIndex(
            new BinarySearchIndexClosedOpen(machNumbers));
        this.function = new BiLinearIntervalsFunction(searchAlgorithmAbs, searchAlgorithmOrd,
            coeffs);
    }

    /** {@inheritDoc} */
    @Override
    public double value(final SpacecraftState state) {
        double value = 0;
        try {
            final double mach = AeroCoeffByMach.machFromSpacecraftState(state, this.atmosphere);
            final double angleOfAttack = AeroCoeffByAoA.angleOfAttackFromSpacecraftState(state,
                this.earthShape);
            value = this.function.value(angleOfAttack, mach);
        } catch (final PatriusException e) {
            // If the x variable cannot be computed, as this function
            // cannot throw an exception, it returns infinite.
            value = Double.POSITIVE_INFINITY;
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ArrayList<Parameter> getParameters() {
        // No supported parameters
        return new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public boolean supportsParameter(final Parameter param) {
        // No supported parameters
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public double derivativeValue(final Parameter p, final SpacecraftState s) {
        // Non supported derivatives
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public boolean isDifferentiableBy(final Parameter p) {
        // Non supported differences
        return false;
    }

    /**
     * Getter for the Angles of Attack array.
     * 
     * @return the Angles of Attack array
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getAoAArray() {
        return this.aoAArray;
    }

    /**
     * Getter for the Mach numbers array.
     * 
     * @return the Mach numbers array
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getMachArray() {
        return this.machArray;
    }

    /**
     * Getter for the aerodynamic coefficients array.
     * 
     * @return the aerodynamic coefficients array
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] getAerodynamicCoefficientsArray() {
        return this.dataArray;
    }

    /**
     * Getter for the function.
     * 
     * @return the function
     */
    public BiLinearIntervalsFunction getFunction() {
        return this.function;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s:[%naoAArray=%s, %nmachArray=%s, %ndataArray=%s%n]", this.getClass()
            .getSimpleName(), Arrays.toString(this.getAoAArray()), Arrays.toString(this.getMachArray()),
            Arrays.deepToString(this.getAerodynamicCoefficientsArray()));
    }

    /** {@inheritDoc} */
    @Override
    public AerodynamicCoefficientType getType() {
        return AerodynamicCoefficientType.MACH_AND_AOA;
    }
}
